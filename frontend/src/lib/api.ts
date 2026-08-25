import axios from "axios"
import type { ApiResponse, AuthUser } from "@/types/api"

const ACCESS_KEY = "loanpro.accessToken"
const REFRESH_KEY = "loanpro.refreshToken"
const USER_KEY = "loanpro.user"

const API_BASE = (import.meta.env.VITE_API_URL as string | undefined)?.replace(/\/$/, "") || "/api/v1"

export const api = axios.create({
  baseURL: API_BASE,
})

function isAnonymousAuthUrl(url?: string) {
  if (!url) return false
  const path = url.split("?")[0]
  return (
    path.endsWith("/auth/login") ||
    path.endsWith("/auth/register") ||
    path.endsWith("/auth/refresh") ||
    path.endsWith("/auth/logout")
  )
}

api.interceptors.request.use((config) => {
  if (isAnonymousAuthUrl(config.url)) {
    return config
  }
  const token = localStorage.getItem(ACCESS_KEY)
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

let refreshing: Promise<string | null> | null = null

api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const original = error.config
    if (
      error.response?.status === 401 &&
      original &&
      !original._retry &&
      !isAnonymousAuthUrl(original.url) &&
      localStorage.getItem(REFRESH_KEY)
    ) {
      original._retry = true
      refreshing ??= refreshAccessToken()
      const token = await refreshing
      refreshing = null
      if (token) {
        original.headers.Authorization = `Bearer ${token}`
        return api(original)
      }
      clearSession()
    }
    return Promise.reject(error)
  },
)

export function persistSession(accessToken: string, refreshToken: string, user: AuthUser) {
  localStorage.setItem(ACCESS_KEY, accessToken)
  localStorage.setItem(REFRESH_KEY, refreshToken)
  localStorage.setItem(USER_KEY, JSON.stringify(user))
}

export function clearSession() {
  localStorage.removeItem(ACCESS_KEY)
  localStorage.removeItem(REFRESH_KEY)
  localStorage.removeItem(USER_KEY)
}

export function persistUser(user: AuthUser) {
  localStorage.setItem(USER_KEY, JSON.stringify(user))
}

export function getStoredUser(): AuthUser | null {
  const raw = localStorage.getItem(USER_KEY)
  return raw ? (JSON.parse(raw) as AuthUser) : null
}

export async function revokeRemoteSession() {
  try {
    await api.post("/auth/logout-all")
  } catch {
    const refreshToken = localStorage.getItem(REFRESH_KEY)
    if (refreshToken) {
      await axios.post(`${API_BASE}/auth/logout`, { refreshToken }).catch(() => undefined)
    }
  }
}

async function refreshAccessToken() {
  const refreshToken = localStorage.getItem(REFRESH_KEY)
  if (!refreshToken) return null
  const { data } = await axios.post<ApiResponse<{ accessToken: string; refreshToken: string; user: AuthUser }>>(
    `${API_BASE}/auth/refresh`,
    { refreshToken },
  )
  persistSession(data.data.accessToken, data.data.refreshToken, data.data.user)
  return data.data.accessToken
}

export async function unwrap<T>(promise: Promise<{ data: ApiResponse<T> }>) {
  const { data } = await promise
  return data.data
}

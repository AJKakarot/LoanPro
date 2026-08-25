import { createContext, useContext, useMemo, useState, type ReactNode } from "react"
import { clearSession, getStoredUser, persistSession, persistUser, revokeRemoteSession } from "@/lib/api"
import type { AuthUser, Role } from "@/types/api"

type AuthContextValue = {
  user: AuthUser | null
  login: (accessToken: string, refreshToken: string, user: AuthUser) => void
  logout: (options?: { remote?: boolean }) => Promise<void>
  updateUser: (user: AuthUser) => void
  hasRole: (role: Role) => boolean
}

const AuthContext = createContext<AuthContextValue | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(() => getStoredUser())

  const value = useMemo<AuthContextValue>(
    () => ({
      user,
      login: (accessToken, refreshToken, next) => {
        persistSession(accessToken, refreshToken, next)
        setUser(next)
      },
      logout: async (options) => {
        if (options?.remote !== false) {
          await revokeRemoteSession().catch(() => undefined)
        }
        clearSession()
        setUser(null)
      },
      updateUser: (next) => {
        persistUser(next)
        setUser(next)
      },
      hasRole: (role) => Boolean(user?.roles.includes(role)),
    }),
    [user],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error("useAuth must be used within AuthProvider")
  return ctx
}

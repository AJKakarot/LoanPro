import { clsx, type ClassValue } from "clsx"
import { twMerge } from "tailwind-merge"

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs))
}

export function formatCurrency(value?: number | string | null) {
  const amount = Number(value ?? 0)
  return new Intl.NumberFormat("en-IN", {
    style: "currency",
    currency: "INR",
    maximumFractionDigits: 0,
  }).format(amount)
}

export function formatDate(value?: string | null) {
  if (!value) return "—"
  return new Intl.DateTimeFormat("en-GB", {
    day: "2-digit",
    month: "short",
    year: "numeric",
  }).format(new Date(value))
}

export function statusLabel(status: string) {
  return status.replaceAll("_", " ").toLowerCase().replace(/\b\w/g, (c) => c.toUpperCase())
}

export function primaryRole(roles: string[] = []) {
  if (roles.includes("ADMIN")) return "ADMIN"
  if (roles.includes("CHECKER")) return "CHECKER"
  if (roles.includes("MAKER")) return "MAKER"
  return "CUSTOMER"
}

export function roleLabel(roles: string[] = []) {
  const labels: Record<string, string> = {
    CUSTOMER: "Customer",
    MAKER: "Maker",
    CHECKER: "Checker",
    ADMIN: "Admin",
  }
  return labels[primaryRole(roles)] ?? "User"
}

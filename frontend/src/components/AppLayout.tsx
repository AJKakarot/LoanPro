import { useState } from "react"
import {
  Bell,
  ClipboardCheck,
  FileText,
  LayoutDashboard,
  LogOut,
  Menu,
  Shield,
  Users,
  Wallet,
  X,
} from "lucide-react"
import { Link, NavLink, Outlet, useNavigate } from "react-router-dom"
import { Button } from "@/components/ui/button"
import { useAuth } from "@/lib/auth"
import { useQuery } from "@tanstack/react-query"
import { api, unwrap } from "@/lib/api"
import { roleLabel } from "@/lib/utils"

const links = [
  { to: "/app", label: "Dashboard", icon: LayoutDashboard, roles: ["CUSTOMER", "MAKER", "CHECKER", "ADMIN"] },
  { to: "/app/apply", label: "Apply for Loan", icon: Wallet, roles: ["CUSTOMER"] },
  { to: "/app/applications", label: "My Applications", icon: FileText, roles: ["CUSTOMER"] },
  { to: "/app/maker", label: "Maker Queue", icon: ClipboardCheck, roles: ["MAKER"] },
  { to: "/app/checker", label: "Checker Queue", icon: Shield, roles: ["CHECKER"] },
  { to: "/app/admin/users", label: "Users", icon: Users, roles: ["ADMIN"] },
  { to: "/app/admin/products", label: "Loan Products", icon: Wallet, roles: ["ADMIN"] },
  { to: "/app/admin/applications", label: "All Applications", icon: FileText, roles: ["ADMIN"] },
  { to: "/app/admin/audit", label: "Audit Logs", icon: Shield, roles: ["ADMIN"] },
  { to: "/app/profile", label: "Profile", icon: Users, roles: ["CUSTOMER", "MAKER", "CHECKER", "ADMIN"] },
  { to: "/app/notifications", label: "Notifications", icon: Bell, roles: ["CUSTOMER", "MAKER", "CHECKER", "ADMIN"] },
]

export function AppLayout() {
  const { user, logout, hasRole } = useAuth()
  const navigate = useNavigate()
  const [menuOpen, setMenuOpen] = useState(false)
  const unread = useQuery({
    queryKey: ["unread"],
    queryFn: () => unwrap<{ count: number }>(api.get("/notifications/unread-count")),
  })
  const role = roleLabel(user?.roles)
  const visibleLinks = links.filter((link) => link.roles.some((item) => hasRole(item as never)))

  async function signOut() {
    setMenuOpen(false)
    await logout()
    navigate("/login")
  }

  function Nav() {
    return (
      <>
        <Link to="/app" className="mb-8 font-serif text-2xl" onClick={() => setMenuOpen(false)}>
          LoanPro
        </Link>
        <nav className="flex flex-1 flex-col gap-1 overflow-y-auto">
          {visibleLinks.map((link) => (
            <NavLink
              key={link.to}
              to={link.to}
              end={link.to === "/app"}
              onClick={() => setMenuOpen(false)}
              className={({ isActive }) =>
                `flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm ${isActive ? "bg-primary/20 font-medium" : "text-muted-foreground hover:bg-muted"}`
              }
            >
              <link.icon className="h-4 w-4 shrink-0" />
              <span className="truncate">{link.label}</span>
              {link.to === "/app/notifications" && unread.data?.count ? (
                <span className="ml-auto rounded-full bg-primary px-2 text-xs">{unread.data.count}</span>
              ) : null}
            </NavLink>
          ))}
        </nav>
        <Button variant="outline" className="mt-4 w-full" onClick={signOut}>
          <LogOut className="h-4 w-4" />
          Sign out
        </Button>
      </>
    )
  }

  return (
    <div className="min-h-svh overflow-x-hidden bg-background">
      <aside className="fixed inset-y-0 left-0 z-30 hidden w-64 border-r border-border bg-white p-5 md:flex md:flex-col">
        <Nav />
      </aside>
      {menuOpen ? (
        <div className="fixed inset-0 z-40 md:hidden">
          <button type="button" className="absolute inset-0 bg-black/40" aria-label="Close menu" onClick={() => setMenuOpen(false)} />
          <aside className="relative flex h-full w-[min(18rem,85vw)] flex-col border-r border-border bg-white p-5 pt-12 shadow-xl">
            <button type="button" className="absolute right-4 top-4 text-muted-foreground" onClick={() => setMenuOpen(false)} aria-label="Close">
              <X className="h-5 w-5" />
            </button>
            <Nav />
          </aside>
        </div>
      ) : null}
      <div className="md:pl-64">
        <header className="sticky top-0 z-20 flex items-center gap-3 border-b border-border bg-white/90 px-4 py-3 backdrop-blur md:px-8">
          <Button variant="ghost" size="icon" className="shrink-0 md:hidden" onClick={() => setMenuOpen(true)} aria-label="Open menu">
            <Menu className="h-5 w-5" />
          </Button>
          <Link to="/app" className="font-serif text-xl md:hidden">
            LoanPro
          </Link>
          <Link to="/app/profile" className="ml-auto flex min-w-0 items-center gap-3 rounded-2xl px-2 py-1 hover:bg-muted">
            <div className="hidden min-w-0 text-right sm:block">
              <p className="truncate font-medium">
                {user?.firstName} {user?.lastName}
              </p>
              <p className="truncate text-xs text-muted-foreground">
                {role} · {user?.email}
              </p>
            </div>
            <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-primary font-medium">
              {user?.firstName?.[0]}
              {user?.lastName?.[0]}
            </div>
          </Link>
        </header>
        <main className="px-4 py-6 md:px-8">
          <Outlet />
        </main>
      </div>
    </div>
  )
}

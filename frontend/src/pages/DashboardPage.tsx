import { useQuery } from "@tanstack/react-query"
import { Link } from "react-router-dom"
import { StatusBadge } from "@/components/StatusBadge"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Skeleton } from "@/components/ui/skeleton"
import { api, unwrap } from "@/lib/api"
import { useAuth } from "@/lib/auth"
import { formatCurrency, formatDate } from "@/lib/utils"
import type { DashboardStats } from "@/types/api"

function Stat({ label, value, highlight = false }: { label: string; value: string; highlight?: boolean }) {
  return (
    <div className={`rounded-2xl p-5 ${highlight ? "bg-primary text-black" : "bg-muted"}`}>
      <p className="text-sm opacity-70">{label}</p>
      <p className="mt-2 font-serif text-2xl sm:text-3xl">{value}</p>
    </div>
  )
}

export function DashboardPage() {
  const { hasRole } = useAuth()
  const stats = useQuery({
    queryKey: ["dashboard"],
    queryFn: () => unwrap<DashboardStats>(api.get("/dashboard")),
  })

  if (stats.isLoading) {
    return <div className="grid gap-4 md:grid-cols-4">{Array.from({ length: 4 }).map((_, i) => <Skeleton key={i} className="h-28" />)}</div>
  }

  const data = stats.data
  if (!data) return <p>Unable to load dashboard.</p>

  const title = hasRole("ADMIN")
    ? "System overview"
    : hasRole("CHECKER")
      ? "Checker review"
      : hasRole("MAKER")
        ? "Maker review"
        : "Loan applications"
  const subtitle = hasRole("CUSTOMER")
    ? "Create an application, upload documents, and track status through maker and checker review."
    : hasRole("MAKER")
      ? "Verify customer information, documents and financials before sending files to checker."
      : hasRole("CHECKER")
        ? "Review maker-verified applications and record approve, reject or return decisions."
        : "Manage users, products, applications and the audit trail."

  return (
    <div className="space-y-8">
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1 className="font-serif text-3xl sm:text-4xl md:text-5xl">{title}</h1>
          <p className="mt-2 max-w-2xl text-muted-foreground">{subtitle}</p>
        </div>
        {hasRole("CUSTOMER") ? (
          <Button asChild>
            <Link to="/app/apply">Apply for a loan</Link>
          </Button>
        ) : null}
      </div>

      <div className="grid grid-cols-2 gap-3 sm:gap-4 md:grid-cols-4">
        <Stat label="Total applications" value={String(data.totalApplications)} />
        <Stat label="Pending" value={String(data.pendingApplications)} />
        <Stat label="NV3 book" value={formatCurrency(data.approvedLoanAmount)} highlight />
        <Stat label="Approved" value={String(data.approved)} />
      </div>

      <div className="grid gap-6 lg:grid-cols-[1.4fr_0.8fr]">
        <Card>
          <CardHeader>
            <CardTitle>Recent applications</CardTitle>
          </CardHeader>
          <CardContent className="space-y-3">
            {data.recentApplications.length === 0 ? (
              <p className="text-sm text-muted-foreground">No applications yet.</p>
            ) : (
              data.recentApplications.map((item) => (
                <div key={item.id} className="flex min-w-0 flex-col gap-3 rounded-2xl border border-border p-4 sm:flex-row sm:items-center sm:justify-between">
                  <div className="min-w-0">
                    <p className="font-mono text-sm">{item.applicationNumber}</p>
                    <p className="text-sm text-muted-foreground">{item.customerName} · {formatCurrency(item.requestedAmount)}</p>
                  </div>
                  <div className="flex flex-wrap items-center gap-3">
                    <StatusBadge status={item.status} />
                    <span className="hidden text-sm text-muted-foreground md:inline">{formatDate(item.createdAt)}</span>
                    <Button asChild size="sm">
                      <Link to={`/app/applications/${item.id}`}>View Details</Link>
                    </Button>
                  </div>
                </div>
              ))
            )}
          </CardContent>
        </Card>
        <div className="space-y-4">
          <div className="grid grid-cols-2 gap-3">
            <Stat label="Maker review" value={String(data.makerReview)} />
            <Stat label="Checker review" value={String(data.checkerReview)} />
            <Stat label="Rejected" value={String(data.rejected)} />
            <Stat label="Book size" value={formatCurrency(data.totalLoanAmount)} highlight />
          </div>
        </div>
      </div>
    </div>
  )
}

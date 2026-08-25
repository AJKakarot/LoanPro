import { useQuery } from "@tanstack/react-query"
import { Link, useSearchParams } from "react-router-dom"
import { StatusBadge } from "@/components/StatusBadge"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { api, unwrap } from "@/lib/api"
import { formatCurrency, formatDate } from "@/lib/utils"
import type { ApplicationStatus, ApplicationSummary, PageResponse } from "@/types/api"

const tabs: Array<{ label: string; value: string }> = [
  { label: "All", value: "" },
  { label: "Approved", value: "APPROVED" },
  { label: "Checker queue", value: "CHECKER_REVIEW" },
  { label: "Rejected", value: "REJECTED" },
]

export function ApplicationsPage({
  endpoint = "/applications/me",
  title = "My Applications",
}: {
  endpoint?: string
  title?: string
}) {
  const [params, setParams] = useSearchParams()
  const status = params.get("status") ?? ""
  const search = params.get("search") ?? ""
  const page = Number(params.get("page") ?? 0)

  const query = useQuery({
    queryKey: ["applications", endpoint, status, search, page],
    queryFn: () =>
      unwrap<PageResponse<ApplicationSummary>>(
        api.get(endpoint, { params: { status: status || undefined, search: search || undefined, page, size: 8, sort: "createdAt,desc" } }),
      ),
  })

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-end justify-between gap-4">
        <h1 className="font-serif text-3xl sm:text-4xl">{title}</h1>
        <Input
          className="w-full sm:max-w-xs"
          placeholder="Search application number"
          defaultValue={search}
          onKeyDown={(e) => {
            if (e.key === "Enter") {
              params.set("search", (e.target as HTMLInputElement).value)
              params.set("page", "0")
              setParams(params)
            }
          }}
        />
      </div>
      <div className="flex flex-wrap gap-2">
        {tabs.map((tab) => (
          <button
            key={tab.label}
            className={`rounded-full px-4 py-2 text-sm ${status === tab.value ? "bg-white shadow-sm" : "bg-muted text-muted-foreground"}`}
            onClick={() => {
              if (tab.value) params.set("status", tab.value)
              else params.delete("status")
              params.set("page", "0")
              setParams(params)
            }}
          >
            {tab.label}
          </button>
        ))}
      </div>
      <div className="space-y-3">
        {query.data?.content.length === 0 ? (
          <p className="rounded-2xl bg-white p-8 text-muted-foreground">No applications found.</p>
        ) : (
          query.data?.content.map((item) => (
            <div key={item.id} className="flex flex-col gap-3 rounded-2xl bg-white p-4 shadow-sm sm:flex-row sm:flex-wrap sm:items-center sm:justify-between">
              <div>
                <p className="font-mono text-sm">{item.applicationNumber}</p>
                <p>{item.customerName}</p>
              </div>
              <StatusBadge status={item.status as ApplicationStatus} />
              <p className="text-sm">{formatCurrency(item.requestedAmount)}</p>
              <p className="text-sm text-muted-foreground">{formatDate(item.createdAt)}</p>
              <Button asChild size="sm">
                <Link to={`/app/applications/${item.id}`}>View Details</Link>
              </Button>
            </div>
          ))
        )}
      </div>
      <div className="flex justify-end gap-2">
        <Button variant="outline" disabled={page === 0} onClick={() => { params.set("page", String(page - 1)); setParams(params) }}>
          Previous
        </Button>
        <Button variant="outline" disabled={(query.data?.page ?? 0) + 1 >= (query.data?.totalPages ?? 1)} onClick={() => { params.set("page", String(page + 1)); setParams(params) }}>
          Next
        </Button>
      </div>
    </div>
  )
}

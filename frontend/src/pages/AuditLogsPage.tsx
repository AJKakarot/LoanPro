import { useQuery } from "@tanstack/react-query"
import { api, unwrap } from "@/lib/api"
import { formatDate } from "@/lib/utils"
import type { PageResponse } from "@/types/api"

type AuditRow = {
  id: string
  userEmail?: string
  action: string
  applicationNumber?: string
  oldStatus?: string
  newStatus?: string
  remarks?: string
  timestamp: string
}

export function AuditLogsPage() {
  const logs = useQuery({
    queryKey: ["audit"],
    queryFn: () => unwrap<PageResponse<AuditRow>>(api.get("/admin/audit-logs", { params: { size: 30, sort: "createdAt,desc" } })),
  })

  return (
    <div className="space-y-4">
      <h1 className="font-serif text-4xl">Audit logs</h1>
      {logs.data?.content.map((row) => (
        <div key={row.id} className="rounded-2xl bg-white p-4 text-sm">
          <p className="font-medium">{row.action}</p>
          <p className="text-muted-foreground">
            {row.userEmail} · {row.applicationNumber ?? "—"} · {row.oldStatus ?? "—"} → {row.newStatus ?? "—"}
          </p>
          <p>{row.remarks}</p>
          <p className="text-xs">{formatDate(row.timestamp)}</p>
        </div>
      ))}
    </div>
  )
}

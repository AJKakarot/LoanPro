import { cn, statusLabel } from "@/lib/utils"
import type { ApplicationStatus } from "@/types/api"

const styles: Record<ApplicationStatus, string> = {
  APPROVED: "bg-emerald-50 text-emerald-800",
  REJECTED: "bg-red-50 text-red-700",
  DRAFT: "bg-slate-100 text-slate-700",
  SUBMITTED: "bg-amber-50 text-amber-800",
  MAKER_REVIEW: "bg-amber-50 text-amber-800",
  INFO_REQUESTED: "bg-orange-50 text-orange-800",
  MAKER_VERIFIED: "bg-yellow-50 text-yellow-800",
  CHECKER_REVIEW: "bg-violet-50 text-violet-800",
  RETURNED_TO_MAKER: "bg-orange-50 text-orange-800",
}

export function StatusBadge({ status }: { status: ApplicationStatus }) {
  return (
    <span className={cn("inline-flex rounded-full px-2.5 py-1 text-xs font-medium", styles[status])}>
      {statusLabel(status)}
    </span>
  )
}

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { Button } from "@/components/ui/button"
import { api, unwrap } from "@/lib/api"
import { formatDate } from "@/lib/utils"
import type { NotificationItem, PageResponse } from "@/types/api"

export function NotificationsPage() {
  const qc = useQueryClient()
  const list = useQuery({
    queryKey: ["notifications"],
    queryFn: () => unwrap<PageResponse<NotificationItem>>(api.get("/notifications")),
  })
  const readAll = useMutation({
    mutationFn: () => unwrap(api.post("/notifications/read-all")),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["notifications"] })
      qc.invalidateQueries({ queryKey: ["unread"] })
    },
  })

  return (
    <div className="space-y-4">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <h1 className="font-serif text-3xl sm:text-4xl">Notifications</h1>
        <Button variant="outline" className="w-full sm:w-auto" onClick={() => readAll.mutate()}>Mark all read</Button>
      </div>
      {list.data?.content.length === 0 ? <p className="text-muted-foreground">No notifications yet.</p> : null}
      {list.data?.content.map((item) => (
        <div key={item.id} className={`rounded-2xl bg-white p-4 ${item.read ? "opacity-70" : ""}`}>
          <p className="font-medium">{item.title}</p>
          <p className="text-sm text-muted-foreground">{item.message}</p>
          <p className="mt-1 text-xs">{formatDate(item.createdAt)}</p>
        </div>
      ))}
    </div>
  )
}

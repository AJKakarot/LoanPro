import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { toast } from "sonner"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { api, unwrap } from "@/lib/api"
import type { PageResponse } from "@/types/api"

type UserRow = {
  id: string
  email: string
  firstName: string
  lastName: string
  status: string
  roles: string[]
}

export function AdminUsersPage() {
  const qc = useQueryClient()
  const users = useQuery({
    queryKey: ["admin-users"],
    queryFn: () => unwrap<PageResponse<UserRow>>(api.get("/admin/users", { params: { size: 20 } })),
  })
  const create = useMutation({
    mutationFn: (body: Record<string, unknown>) => unwrap(api.post("/admin/users", body)),
    onSuccess: () => {
      toast.success("User created")
      qc.invalidateQueries({ queryKey: ["admin-users"] })
    },
    onError: (e: unknown) => toast.error((e as { response?: { data?: { message?: string } } }).response?.data?.message ?? "Create failed"),
  })

  return (
    <div className="grid gap-6 lg:grid-cols-[1.2fr_0.8fr]">
      <Card>
        <CardHeader><CardTitle>User management</CardTitle></CardHeader>
        <CardContent className="space-y-3">
          {users.data?.content.map((user) => (
            <div key={user.id} className="flex flex-col gap-2 rounded-xl border p-3 text-sm sm:flex-row sm:items-center sm:justify-between">
              <div>
                <p>{user.firstName} {user.lastName}</p>
                <p className="text-muted-foreground">{user.email} · {user.roles.join(", ")}</p>
              </div>
              <span>{user.status}</span>
            </div>
          ))}
        </CardContent>
      </Card>
      <Card>
        <CardHeader><CardTitle>Create staff or customer</CardTitle></CardHeader>
        <CardContent>
          <form
            className="space-y-3"
            onSubmit={(e) => {
              e.preventDefault()
              const form = new FormData(e.currentTarget)
              create.mutate({
                firstName: form.get("firstName"),
                lastName: form.get("lastName"),
                email: form.get("email"),
                phone: form.get("phone"),
                password: form.get("password"),
                roles: [form.get("role")],
                status: "ACTIVE",
              })
            }}
          >
            <Input name="firstName" placeholder="First name" required />
            <Input name="lastName" placeholder="Last name" required />
            <Input name="email" type="email" placeholder="Email" required />
            <Input name="phone" placeholder="Phone" />
            <Input name="password" type="password" placeholder="Password" required />
            <select name="role" className="h-11 w-full rounded-xl border px-3">
              <option>MAKER</option>
              <option>CHECKER</option>
              <option>ADMIN</option>
              <option>CUSTOMER</option>
            </select>
            <Button disabled={create.isPending}>Create user</Button>
          </form>
        </CardContent>
      </Card>
    </div>
  )
}

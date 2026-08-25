import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { useNavigate } from "react-router-dom"
import { toast } from "sonner"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { api, unwrap } from "@/lib/api"
import { useAuth } from "@/lib/auth"
import { roleLabel } from "@/lib/utils"
import type { AuthUser } from "@/types/api"

export function ProfilePage() {
  const { user, hasRole, updateUser, logout } = useAuth()
  const navigate = useNavigate()
  const qc = useQueryClient()
  const isCustomer = hasRole("CUSTOMER")
  const profile = useQuery({
    queryKey: ["profile"],
    enabled: isCustomer,
    queryFn: () => unwrap<Record<string, string | number>>(api.get("/profile")),
  })

  const saveAccount = useMutation({
    mutationFn: (body: Record<string, unknown>) => unwrap<AuthUser>(api.put("/auth/me", body)),
    onSuccess: (next) => {
      updateUser({
        id: next.id,
        email: next.email,
        firstName: next.firstName,
        lastName: next.lastName,
        phone: next.phone,
        status: String(next.status),
        roles: next.roles,
      })
      toast.success("Profile updated")
    },
    onError: (e: unknown) => {
      toast.error((e as { response?: { data?: { message?: string } } }).response?.data?.message ?? "Update failed")
    },
  })

  const saveCustomer = useMutation({
    mutationFn: (body: Record<string, unknown>) => unwrap(api.put("/profile", body)),
    onSuccess: () => {
      toast.success("Customer details saved")
      qc.invalidateQueries({ queryKey: ["profile"] })
    },
  })

  const changePassword = useMutation({
    mutationFn: (body: Record<string, string>) => unwrap(api.post("/auth/change-password", body)),
    onSuccess: async () => {
      toast.success("Password changed. Please sign in again.")
      await logout({ remote: false })
      navigate("/login")
    },
    onError: (e: unknown) => {
      toast.error((e as { response?: { data?: { message?: string } } }).response?.data?.message ?? "Password change failed")
    },
  })

  if (isCustomer && !profile.data) return <p>Loading profile...</p>
  const data = profile.data

  return (
    <div className="space-y-6">
      <div>
        <h1 className="font-serif text-3xl sm:text-4xl">Profile</h1>
        <p className="mt-1 text-muted-foreground">{roleLabel(user?.roles)} account</p>
      </div>
      <Card className="max-w-3xl">
        <CardHeader>
          <CardTitle>Account</CardTitle>
        </CardHeader>
        <CardContent>
          <form
            className="grid gap-4 md:grid-cols-2"
            onSubmit={(e) => {
              e.preventDefault()
              const form = new FormData(e.currentTarget)
              saveAccount.mutate({
                firstName: form.get("firstName"),
                lastName: form.get("lastName"),
                phone: form.get("phone"),
              })
            }}
          >
            <div className="space-y-2">
              <Label>First name</Label>
              <Input name="firstName" defaultValue={user?.firstName} required />
            </div>
            <div className="space-y-2">
              <Label>Last name</Label>
              <Input name="lastName" defaultValue={user?.lastName} required />
            </div>
            <div className="space-y-2">
              <Label>Email</Label>
              <Input value={user?.email} disabled />
            </div>
            <div className="space-y-2">
              <Label>Phone</Label>
              <Input name="phone" defaultValue={user?.phone ?? ""} />
            </div>
            <div className="space-y-2">
              <Label>Role</Label>
              <Input value={roleLabel(user?.roles)} disabled />
            </div>
            <div className="md:col-span-2">
              <Button disabled={saveAccount.isPending}>Save account</Button>
            </div>
          </form>
        </CardContent>
      </Card>

      <Card className="max-w-3xl">
        <CardHeader>
          <CardTitle>Change password</CardTitle>
        </CardHeader>
        <CardContent>
          <form
            className="grid gap-4 md:grid-cols-2"
            onSubmit={(e) => {
              e.preventDefault()
              const form = new FormData(e.currentTarget)
              const newPassword = String(form.get("newPassword") ?? "")
              const confirmPassword = String(form.get("confirmPassword") ?? "")
              if (newPassword !== confirmPassword) {
                toast.error("New passwords do not match")
                return
              }
              changePassword.mutate({
                currentPassword: String(form.get("currentPassword") ?? ""),
                newPassword,
              })
            }}
          >
            <div className="space-y-2 md:col-span-2">
              <Label>Current password</Label>
              <Input name="currentPassword" type="password" required autoComplete="current-password" />
            </div>
            <div className="space-y-2">
              <Label>New password</Label>
              <Input name="newPassword" type="password" required minLength={8} autoComplete="new-password" />
            </div>
            <div className="space-y-2">
              <Label>Confirm new password</Label>
              <Input name="confirmPassword" type="password" required minLength={8} autoComplete="new-password" />
            </div>
            <p className="text-sm text-muted-foreground md:col-span-2">
              Use at least 8 characters with upper, lower, and a digit. All sessions will be signed out.
            </p>
            <div className="md:col-span-2">
              <Button disabled={changePassword.isPending}>Update password</Button>
            </div>
          </form>
        </CardContent>
      </Card>

      {isCustomer && data ? (
        <Card className="max-w-3xl">
          <CardHeader>
            <CardTitle>Customer details</CardTitle>
          </CardHeader>
          <CardContent>
            <form
              className="grid gap-4 md:grid-cols-2"
              onSubmit={(e) => {
                e.preventDefault()
                const form = new FormData(e.currentTarget)
                saveCustomer.mutate({
                  dateOfBirth: form.get("dateOfBirth") || null,
                  gender: form.get("gender"),
                  nationalId: form.get("nationalId"),
                  addressLine: form.get("addressLine"),
                  city: form.get("city"),
                  state: form.get("state"),
                  postalCode: form.get("postalCode"),
                  employmentType: form.get("employmentType"),
                  employerName: form.get("employerName"),
                  designation: form.get("designation"),
                  yearsEmployed: Number(form.get("yearsEmployed") || 0),
                  monthlyIncome: Number(form.get("monthlyIncome") || 0),
                  otherIncome: Number(form.get("otherIncome") || 0),
                  existingEmis: Number(form.get("existingEmis") || 0),
                  monthlyExpenses: Number(form.get("monthlyExpenses") || 0),
                })
              }}
            >
              <div className="space-y-2">
                <Label>Date of birth</Label>
                <Input type="date" name="dateOfBirth" defaultValue={String(data.dateOfBirth ?? "")} />
              </div>
              <div className="space-y-2">
                <Label>National ID</Label>
                <Input name="nationalId" defaultValue={String(data.nationalId ?? "")} />
              </div>
              <div className="space-y-2 md:col-span-2">
                <Label>Address</Label>
                <Input name="addressLine" defaultValue={String(data.addressLine ?? "")} />
              </div>
              <div className="space-y-2">
                <Label>City</Label>
                <Input name="city" defaultValue={String(data.city ?? "")} />
              </div>
              <div className="space-y-2">
                <Label>State</Label>
                <Input name="state" defaultValue={String(data.state ?? "")} />
              </div>
              <div className="space-y-2">
                <Label>Employment type</Label>
                <select name="employmentType" defaultValue={String(data.employmentType ?? "SALARIED")} className="h-11 w-full rounded-xl border px-3">
                  <option>SALARIED</option>
                  <option>SELF_EMPLOYED</option>
                  <option>BUSINESS</option>
                  <option>UNEMPLOYED</option>
                </select>
              </div>
              <div className="space-y-2">
                <Label>Employer</Label>
                <Input name="employerName" defaultValue={String(data.employerName ?? "")} />
              </div>
              <div className="space-y-2">
                <Label>Monthly income</Label>
                <Input type="number" name="monthlyIncome" defaultValue={String(data.monthlyIncome ?? "")} />
              </div>
              <div className="space-y-2">
                <Label>Existing EMIs</Label>
                <Input type="number" name="existingEmis" defaultValue={String(data.existingEmis ?? "")} />
              </div>
              <div className="md:col-span-2">
                <Button disabled={saveCustomer.isPending}>Save customer details</Button>
              </div>
            </form>
          </CardContent>
        </Card>
      ) : null}
    </div>
  )
}

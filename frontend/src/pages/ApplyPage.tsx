import { useMutation, useQuery } from "@tanstack/react-query"
import { useState } from "react"
import { useNavigate } from "react-router-dom"
import { toast } from "sonner"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Textarea } from "@/components/ui/textarea"
import { api, unwrap } from "@/lib/api"
import type { ApplicationDetail, LoanProduct } from "@/types/api"

export function ApplyPage() {
  const navigate = useNavigate()
  const products = useQuery({
    queryKey: ["products"],
    queryFn: () => unwrap<LoanProduct[]>(api.get("/loan-products")),
  })
  const profile = useQuery({
    queryKey: ["profile"],
    queryFn: () => unwrap<Record<string, unknown>>(api.get("/profile")),
  })
  const [form, setForm] = useState({
    loanProductId: "",
    requestedAmount: "250000",
    tenureMonths: "24",
    purpose: "Home renovation and working capital",
  })
  const create = useMutation({
    mutationFn: () =>
      unwrap<ApplicationDetail>(
        api.post("/applications", {
          ...form,
          requestedAmount: Number(form.requestedAmount),
          tenureMonths: Number(form.tenureMonths),
          fullName: `${profile.data?.firstName ?? ""} ${profile.data?.lastName ?? ""}`.trim(),
          email: profile.data?.email,
          phone: profile.data?.phone,
          dateOfBirth: profile.data?.dateOfBirth,
          gender: profile.data?.gender,
          nationalId: profile.data?.nationalId,
          addressLine: profile.data?.addressLine,
          city: profile.data?.city,
          state: profile.data?.state,
          postalCode: profile.data?.postalCode,
          employmentType: profile.data?.employmentType,
          employerName: profile.data?.employerName,
          designation: profile.data?.designation,
          yearsEmployed: profile.data?.yearsEmployed,
          monthlyIncome: profile.data?.monthlyIncome,
          otherIncome: profile.data?.otherIncome,
          existingEmis: profile.data?.existingEmis,
          monthlyExpenses: profile.data?.monthlyExpenses,
        }),
      ),
    onSuccess: (app) => {
      toast.success("Draft application created")
      navigate(`/app/applications/${app.id}`)
    },
    onError: (err: unknown) => {
      toast.error((err as { response?: { data?: { message?: string } } }).response?.data?.message ?? "Unable to create application")
    },
  })

  return (
    <Card className="max-w-3xl">
      <CardHeader>
        <CardTitle>Apply for Loan</CardTitle>
      </CardHeader>
      <CardContent>
        <form
          className="grid gap-4"
          onSubmit={(e) => {
            e.preventDefault()
            create.mutate()
          }}
        >
          <div className="space-y-2">
            <Label>Loan product</Label>
            <select
              required
              className="h-11 w-full rounded-xl border border-input bg-white px-3"
              value={form.loanProductId}
              onChange={(e) => setForm({ ...form, loanProductId: e.target.value })}
            >
              <option value="">Select a product</option>
              {products.data?.map((product) => (
                <option key={product.id} value={product.id}>
                  {product.name} · {product.interestRate}%
                </option>
              ))}
            </select>
          </div>
          <div className="grid gap-4 md:grid-cols-2">
            <div className="space-y-2">
              <Label>Amount</Label>
              <Input type="number" value={form.requestedAmount} onChange={(e) => setForm({ ...form, requestedAmount: e.target.value })} />
            </div>
            <div className="space-y-2">
              <Label>Tenure (months)</Label>
              <Input type="number" value={form.tenureMonths} onChange={(e) => setForm({ ...form, tenureMonths: e.target.value })} />
            </div>
          </div>
          <div className="space-y-2">
            <Label>Purpose</Label>
            <Textarea value={form.purpose} onChange={(e) => setForm({ ...form, purpose: e.target.value })} />
          </div>
          <Button disabled={create.isPending}>{create.isPending ? "Saving..." : "Create draft"}</Button>
        </form>
      </CardContent>
    </Card>
  )
}

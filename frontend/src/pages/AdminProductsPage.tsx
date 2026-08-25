import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { toast } from "sonner"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { api, unwrap } from "@/lib/api"
import type { LoanProduct } from "@/types/api"

export function AdminProductsPage() {
  const qc = useQueryClient()
  const products = useQuery({
    queryKey: ["admin-products"],
    queryFn: () => unwrap<LoanProduct[]>(api.get("/admin/loan-products")),
  })
  const create = useMutation({
    mutationFn: (body: Record<string, unknown>) => unwrap(api.post("/admin/loan-products", body)),
    onSuccess: () => {
      toast.success("Product saved")
      qc.invalidateQueries({ queryKey: ["admin-products"] })
    },
    onError: (e: unknown) => toast.error((e as { response?: { data?: { message?: string } } }).response?.data?.message ?? "Save failed"),
  })

  return (
    <div className="grid gap-6 lg:grid-cols-2">
      <Card>
        <CardHeader><CardTitle>Loan products</CardTitle></CardHeader>
        <CardContent className="space-y-3">
          {products.data?.map((product) => (
            <div key={product.id} className="rounded-xl border p-3">
              <p className="font-medium">{product.name}</p>
              <p className="text-sm text-muted-foreground">{product.code} · {product.interestRate}% · {product.active ? "Active" : "Inactive"}</p>
            </div>
          ))}
        </CardContent>
      </Card>
      <Card>
        <CardHeader><CardTitle>Add product</CardTitle></CardHeader>
        <CardContent>
          <form
            className="grid gap-3"
            onSubmit={(e) => {
              e.preventDefault()
              const form = new FormData(e.currentTarget)
              create.mutate({
                code: form.get("code"),
                name: form.get("name"),
                description: form.get("description"),
                minAmount: Number(form.get("minAmount")),
                maxAmount: Number(form.get("maxAmount")),
                minTenureMonths: Number(form.get("minTenureMonths")),
                maxTenureMonths: Number(form.get("maxTenureMonths")),
                interestRate: Number(form.get("interestRate")),
                processingFeePercent: Number(form.get("processingFeePercent")),
                requiredDocuments: "IDENTITY,INCOME_PROOF",
                active: true,
              })
            }}
          >
            <Input name="code" placeholder="Code" required />
            <Input name="name" placeholder="Name" required />
            <Input name="description" placeholder="Description" />
            <Input name="minAmount" type="number" placeholder="Min amount" required />
            <Input name="maxAmount" type="number" placeholder="Max amount" required />
            <Input name="minTenureMonths" type="number" placeholder="Min tenure" required />
            <Input name="maxTenureMonths" type="number" placeholder="Max tenure" required />
            <Input name="interestRate" type="number" step="0.001" placeholder="Interest rate" required />
            <Input name="processingFeePercent" type="number" step="0.001" placeholder="Fee %" required />
            <Button>Create product</Button>
          </form>
        </CardContent>
      </Card>
    </div>
  )
}

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { useState } from "react"
import { useParams } from "react-router-dom"
import { toast } from "sonner"
import { StatusBadge } from "@/components/StatusBadge"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Dialog, DialogContent, DialogHeader, DialogTitle } from "@/components/ui/dialog"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Textarea } from "@/components/ui/textarea"
import { api, unwrap } from "@/lib/api"
import { useAuth } from "@/lib/auth"
import { formatCurrency, formatDate } from "@/lib/utils"
import type {
  AiAnalysis,
  ApplicationDetail,
  Eligibility,
  LoanDocument,
} from "@/types/api"

type HistoryItem = { id: string; fromStatus?: string; toStatus: string; changedBy: string; remarks?: string; timestamp: string }
type MakerReview = {
  id: string
  makerName: string
  customerInfoVerified: boolean
  documentsVerified: boolean
  financialsVerified: boolean
  remarks?: string
  missingInformation?: string
  createdAt: string
}

export function ApplicationDetailPage() {
  const { id = "" } = useParams()
  const { hasRole } = useAuth()
  const qc = useQueryClient()
  const [remarks, setRemarks] = useState("")
  const [reason, setReason] = useState("")
  const [rejectOpen, setRejectOpen] = useState(false)
  const [docType, setDocType] = useState("IDENTITY")

  const app = useQuery({
    queryKey: ["application", id],
    queryFn: () => unwrap<ApplicationDetail>(api.get(`/applications/${id}`)),
  })
  const docs = useQuery({
    queryKey: ["documents", id],
    queryFn: () => unwrap<LoanDocument[]>(api.get(`/applications/${id}/documents`)),
  })
  const history = useQuery({
    queryKey: ["history", id],
    queryFn: () => unwrap<HistoryItem[]>(api.get(`/applications/${id}/history`)),
  })
  const eligibility = useQuery({
    queryKey: ["eligibility", id],
    enabled: hasRole("CHECKER") || hasRole("MAKER") || hasRole("ADMIN"),
    queryFn: () => unwrap<Eligibility>(api.get(`/applications/${id}/eligibility`)),
  })
  const aiAnalysis = useQuery({
    queryKey: ["ai-analysis", id],
    enabled: hasRole("CHECKER") || hasRole("MAKER") || hasRole("ADMIN"),
    retry: false,
    queryFn: () => unwrap<AiAnalysis>(api.get(`/applications/${id}/ai-analysis`)),
  })
  const makerReviews = useQuery({
    queryKey: ["maker-reviews", id],
    enabled: hasRole("MAKER") || hasRole("CHECKER") || hasRole("ADMIN"),
    queryFn: () => unwrap<MakerReview[]>(api.get(`/applications/${id}/maker-reviews`)),
  })

  const refresh = () => {
    qc.invalidateQueries({ queryKey: ["application", id] })
    qc.invalidateQueries({ queryKey: ["documents", id] })
    qc.invalidateQueries({ queryKey: ["history", id] })
    qc.invalidateQueries({ queryKey: ["dashboard"] })
  }

  const submit = useMutation({
    mutationFn: () => unwrap(api.post(`/applications/${id}/submit`)),
    onSuccess: () => { toast.success("Application submitted"); refresh() },
    onError: (e: unknown) => toast.error((e as { response?: { data?: { message?: string } } }).response?.data?.message ?? "Submit failed"),
  })
  const claim = useMutation({
    mutationFn: () => unwrap(api.post(`/maker/applications/${id}/claim`)),
    onSuccess: () => { toast.success("Claimed for review"); refresh() },
    onError: (e: unknown) => toast.error((e as { response?: { data?: { message?: string } } }).response?.data?.message ?? "Claim failed"),
  })
  const verify = useMutation({
    mutationFn: () => unwrap(api.post(`/maker/applications/${id}/verify`, {
      customerInfoVerified: true,
      documentsVerified: true,
      financialsVerified: true,
      remarks,
    })),
    onSuccess: () => toast.success("Maker verification saved"),
  })
  const sendChecker = useMutation({
    mutationFn: () => unwrap(api.post(`/maker/applications/${id}/send-to-checker`, { remarks: remarks || "Verified and forwarded" })),
    onSuccess: () => { toast.success("Sent to checker"); refresh() },
    onError: (e: unknown) => toast.error((e as { response?: { data?: { message?: string } } }).response?.data?.message ?? "Cannot send"),
  })
  const requestInfo = useMutation({
    mutationFn: () => unwrap(api.post(`/maker/applications/${id}/request-info`, { remarks, missingInformation: remarks })),
    onSuccess: () => { toast.success("Information requested"); refresh() },
  })
  const approve = useMutation({
    mutationFn: () => unwrap(api.post(`/checker/applications/${id}/approve`, { remarks })),
    onSuccess: () => { toast.success("Approved"); refresh() },
    onError: (e: unknown) => toast.error((e as { response?: { data?: { message?: string } } }).response?.data?.message ?? "Approve failed"),
  })
  const reject = useMutation({
    mutationFn: () => unwrap(api.post(`/checker/applications/${id}/reject`, { reason, remarks })),
    onSuccess: () => { toast.success("Rejected"); setRejectOpen(false); refresh() },
    onError: (e: unknown) => toast.error((e as { response?: { data?: { message?: string } } }).response?.data?.message ?? "Reject failed"),
  })
  const returnMaker = useMutation({
    mutationFn: () => unwrap(api.post(`/checker/applications/${id}/return`, { remarks: remarks || "Returned for rework" })),
    onSuccess: () => { toast.success("Returned to maker"); refresh() },
  })

  async function upload(file: File) {
    const body = new FormData()
    body.append("file", file)
    try {
      await api.post(`/applications/${id}/documents`, body, { params: { documentType: docType } })
      toast.success("Document uploaded")
      qc.invalidateQueries({ queryKey: ["documents", id] })
    } catch (e: unknown) {
      toast.error((e as { response?: { data?: { message?: string } } }).response?.data?.message ?? "Upload failed")
    }
  }

  async function verifyDoc(docId: string, verified: boolean) {
    await api.post(`/maker/applications/${id}/documents/${docId}/verify`, { verified, remarks })
    toast.success(verified ? "Document verified" : "Document rejected")
    qc.invalidateQueries({ queryKey: ["documents", id] })
  }

  const data = app.data
  if (!data) return <p>Loading application...</p>
  const canEdit = hasRole("CUSTOMER") && (data.status === "DRAFT" || data.status === "INFO_REQUESTED")
  const makerCanAct = ["SUBMITTED", "MAKER_REVIEW", "RETURNED_TO_MAKER", "INFO_REQUESTED"].includes(data.status)
  const checkerCanDecide = data.status === "CHECKER_REVIEW" || data.status === "MAKER_VERIFIED"

  return (
    <div className="grid gap-6 lg:grid-cols-[1.3fr_0.7fr]">
      <div className="space-y-6">
        <div className="flex flex-wrap items-start justify-between gap-3">
          <div>
            <p className="font-mono text-sm text-muted-foreground">{data.applicationNumber}</p>
            <h1 className="break-words font-serif text-2xl sm:text-3xl lg:text-4xl">{data.fullName ?? data.customerName}</h1>
          </div>
          <StatusBadge status={data.status} />
        </div>
        <Card>
          <CardHeader><CardTitle>Application snapshot</CardTitle></CardHeader>
          <CardContent className="grid gap-3 text-sm md:grid-cols-2">
            <p>Product: {data.loanProductName}</p>
            <p>Amount: {formatCurrency(data.requestedAmount)}</p>
            <p>Tenure: {data.tenureMonths} months</p>
            <p>Rate: {data.interestRate}%</p>
            <p>Income: {formatCurrency(data.monthlyIncome)}</p>
            <p>Existing EMIs: {formatCurrency(data.existingEmis)}</p>
            <p>National ID: {data.nationalId ?? "—"}</p>
            <p>Submitted: {formatDate(data.submittedAt)}</p>
            <p className="md:col-span-2">Purpose: {data.purpose}</p>
            <p className="md:col-span-2 text-muted-foreground">
              {data.addressLine}, {data.city}, {data.state} {data.postalCode}
            </p>
          </CardContent>
        </Card>
        <Card>
          <CardHeader><CardTitle>Supporting documents</CardTitle></CardHeader>
          <CardContent className="space-y-4">
            {canEdit ? (
              <div className="flex flex-wrap gap-2">
                <select className="h-10 rounded-full border px-3" value={docType} onChange={(e) => setDocType(e.target.value)}>
                  {["IDENTITY", "ADDRESS_PROOF", "INCOME_PROOF", "BANK_STATEMENT", "PHOTO"].map((t) => (
                    <option key={t}>{t}</option>
                  ))}
                </select>
                <Input className="w-full min-w-0" type="file" accept=".pdf,image/*" onChange={(e) => e.target.files?.[0] && upload(e.target.files[0])} />
              </div>
            ) : null}
            {docs.data?.length === 0 ? <p className="text-sm text-muted-foreground">No documents uploaded.</p> : null}
            {docs.data?.map((doc) => (
              <div key={doc.id} className="flex min-w-0 flex-col gap-3 rounded-xl bg-muted/60 p-3 sm:flex-row sm:flex-wrap sm:items-center sm:justify-between">
                <div className="min-w-0">
                  <p className="break-all underline">{doc.originalFileName}</p>
                  <p className="text-xs text-muted-foreground">{doc.documentType} · {doc.verificationStatus}</p>
                </div>
                <div className="flex flex-wrap gap-2">
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={async () => {
                      const res = await api.get(`/documents/${doc.id}/download`, { responseType: "blob" })
                      const url = URL.createObjectURL(res.data)
                      const a = document.createElement("a")
                      a.href = url
                      a.download = doc.originalFileName
                      a.click()
                      URL.revokeObjectURL(url)
                    }}
                  >
                    Download
                  </Button>
                  {hasRole("MAKER") ? (
                    <>
                      <Button size="sm" onClick={() => verifyDoc(doc.id, true)}>Verify</Button>
                      <Button variant="destructive" size="sm" onClick={() => verifyDoc(doc.id, false)}>Reject</Button>
                    </>
                  ) : null}
                </div>
              </div>
            ))}
          </CardContent>
        </Card>
        <Card>
          <CardHeader><CardTitle>Application history</CardTitle></CardHeader>
          <CardContent className="space-y-3">
            {history.data?.map((item) => (
              <div key={item.id} className="border-b border-border pb-2 text-sm">
                <p>{item.fromStatus ?? "—"} → {item.toStatus}</p>
                <p className="break-words text-muted-foreground">{item.changedBy} · {formatDate(item.timestamp)} · {item.remarks}</p>
              </div>
            ))}
          </CardContent>
        </Card>
      </div>
      <div className="space-y-4">
        {eligibility.data ? (
          <div className="grid grid-cols-2 gap-3">
            <div className="rounded-2xl bg-muted p-4"><p className="text-xs">Fees</p><p className="font-serif text-2xl">{eligibility.data.feesPercent}%</p></div>
            <div className="rounded-2xl bg-muted p-4"><p className="text-xs">DTI</p><p className="font-serif text-2xl">{(eligibility.data.debtToIncomeRatio * 100).toFixed(2)}%</p></div>
            <div className="rounded-2xl bg-muted p-4"><p className="text-xs">Rate</p><p className="font-serif text-2xl">{eligibility.data.interestRate}%</p></div>
            <div className="rounded-2xl bg-primary p-4"><p className="text-xs">NV3 Score</p><p className="font-serif text-2xl">{eligibility.data.nv3Score}</p></div>
          </div>
        ) : null}
        {(hasRole("MAKER") || hasRole("CHECKER") || hasRole("ADMIN")) ? (
          <Card>
            <CardHeader>
              <CardTitle>AI verification summary</CardTitle>
            </CardHeader>
            <CardContent className="space-y-3 text-sm">
              {aiAnalysis.isLoading ? <p className="text-muted-foreground">Running AI analysis...</p> : null}
              {aiAnalysis.isError ? (
                <p className="text-muted-foreground">AI analysis is unavailable. Continue the maker-checker review manually.</p>
              ) : null}
              {aiAnalysis.data && !aiAnalysis.data.available ? (
                <p className="text-muted-foreground">{aiAnalysis.data.message ?? "AI analysis is unavailable."}</p>
              ) : null}
              {aiAnalysis.data?.available ? (
                <>
                  <p>Document status: <strong>{aiAnalysis.data.documentStatus}</strong></p>
                  <p>Eligibility: <strong>{aiAnalysis.data.eligibilityAssessment}</strong></p>
                  <p>Risk: <strong>{aiAnalysis.data.riskLevel}</strong></p>
                  {aiAnalysis.data.keyIssues.length > 0 ? (
                    <div>
                      <p className="font-medium">Issues</p>
                      <ul className="list-disc pl-5 text-muted-foreground">
                        {aiAnalysis.data.keyIssues.map((item) => <li key={item}>{item}</li>)}
                      </ul>
                    </div>
                  ) : null}
                  {aiAnalysis.data.recommendedManualChecks.length > 0 ? (
                    <div>
                      <p className="font-medium">Manual checks</p>
                      <ul className="list-disc pl-5 text-muted-foreground">
                        {aiAnalysis.data.recommendedManualChecks.map((item) => <li key={item}>{item}</li>)}
                      </ul>
                    </div>
                  ) : null}
                  <p className="text-muted-foreground">{aiAnalysis.data.summary}</p>
                </>
              ) : null}
              <p className="rounded-xl bg-muted p-3 text-xs text-muted-foreground">
                AI is advisory only. Maker and checker remain responsible for the final decision.
              </p>
            </CardContent>
          </Card>
        ) : null}
        {hasRole("CUSTOMER") && canEdit ? (
          <Button className="w-full" onClick={() => submit.mutate()} disabled={submit.isPending}>Submit application</Button>
        ) : null}
        {hasRole("MAKER") && makerCanAct ? (
          <Card>
            <CardHeader><CardTitle>Maker actions</CardTitle></CardHeader>
            <CardContent className="space-y-3">
              <Textarea placeholder="Remarks" value={remarks} onChange={(e) => setRemarks(e.target.value)} />
              <Button className="w-full" variant="outline" onClick={() => claim.mutate()}>Claim / start review</Button>
              <Button className="w-full" variant="outline" onClick={() => verify.mutate()}>Save verification</Button>
              <Button className="w-full" variant="outline" onClick={() => requestInfo.mutate()}>Request missing information</Button>
              <Button className="w-full" onClick={() => sendChecker.mutate()}>Send to checker</Button>
            </CardContent>
          </Card>
        ) : null}
        {hasRole("CHECKER") ? (
          checkerCanDecide ? (
            <Card>
              <CardHeader><CardTitle>Checker decision</CardTitle></CardHeader>
              <CardContent className="space-y-3">
                <p className="text-sm text-muted-foreground">{eligibility.data?.summary}</p>
                <Textarea placeholder="Remarks" value={remarks} onChange={(e) => setRemarks(e.target.value)} />
                <Button className="w-full" onClick={() => approve.mutate()}>Approve</Button>
                <Button className="w-full" variant="outline" onClick={() => returnMaker.mutate()}>Return to maker</Button>
                <Button className="w-full" variant="destructive" onClick={() => setRejectOpen(true)}>Reject</Button>
              </CardContent>
            </Card>
          ) : (
            <Card>
              <CardHeader><CardTitle>Checker decision</CardTitle></CardHeader>
              <CardContent className="text-sm text-muted-foreground">
                This file is still {data.status.replaceAll("_", " ").toLowerCase()}.
                Approve or reject is available only after a maker verifies it and sends it to checker.
              </CardContent>
            </Card>
          )
        ) : null}
        {makerReviews.data && makerReviews.data.length > 0 ? (
          <Card>
            <CardHeader><CardTitle>Maker remarks</CardTitle></CardHeader>
            <CardContent className="space-y-2 text-sm">
              {makerReviews.data.map((review) => (
                <p key={review.id}>{review.makerName}: {review.remarks || "Verified"}</p>
              ))}
            </CardContent>
          </Card>
        ) : null}
      </div>
      <Dialog open={rejectOpen} onOpenChange={setRejectOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Reject application</DialogTitle>
          </DialogHeader>
          <div className="space-y-3">
            <Label>Mandatory reason</Label>
            <Textarea value={reason} onChange={(e) => setReason(e.target.value)} />
            <Button className="w-full" variant="destructive" onClick={() => reject.mutate()}>Confirm rejection</Button>
          </div>
        </DialogContent>
      </Dialog>
    </div>
  )
}

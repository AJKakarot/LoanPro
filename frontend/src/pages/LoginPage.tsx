import { useState } from "react"
import { Link, useNavigate } from "react-router-dom"
import { toast } from "sonner"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { api, unwrap } from "@/lib/api"
import { useAuth } from "@/lib/auth"
import type { AuthUser } from "@/types/api"

export function LoginPage() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const [email, setEmail] = useState("customer@loanpro.com")
  const [password, setPassword] = useState("Customer@12345")
  const [loading, setLoading] = useState(false)

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault()
    setLoading(true)
    try {
      const data = await unwrap<{ accessToken: string; refreshToken: string; user: AuthUser }>(
        api.post("/auth/login", { email, password }),
      )
      login(data.accessToken, data.refreshToken, data.user)
      toast.success("Welcome back")
      navigate("/app")
    } catch (err: unknown) {
      const message = (err as { response?: { data?: { message?: string } } }).response?.data?.message
      toast.error(message ?? "Unable to sign in")
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="grid min-h-svh lg:grid-cols-2">
      <div className="hidden flex-col justify-between bg-[#111] p-10 text-white lg:flex">
        <p className="font-serif text-3xl">LoanPro</p>
        <div>
          <h1 className="max-w-md font-serif text-5xl leading-tight">Credit decisions with maker-checker control.</h1>
          <p className="mt-4 max-w-sm text-white/70">
            Apply, verify documents, and approve loans with a complete audit trail.
          </p>
        </div>
        <p className="text-sm text-white/50">Demo: customer@loanpro.com / Customer@12345</p>
      </div>
      <div className="flex items-center justify-center p-4 sm:p-6">
        <Card className="w-full max-w-md">
          <CardHeader>
            <p className="font-serif text-2xl lg:hidden">LoanPro</p>
            <CardTitle>Sign in</CardTitle>
          </CardHeader>
          <CardContent>
            <form className="space-y-4" onSubmit={onSubmit}>
              <div className="space-y-2">
                <Label>Email</Label>
                <Input value={email} onChange={(e) => setEmail(e.target.value)} type="email" required />
              </div>
              <div className="space-y-2">
                <Label>Password</Label>
                <Input value={password} onChange={(e) => setPassword(e.target.value)} type="password" required />
              </div>
              <Button className="w-full" disabled={loading}>
                {loading ? "Signing in..." : "Continue"}
              </Button>
              <p className="text-center text-sm text-muted-foreground">
                New customer?{" "}
                <Link className="underline" to="/register">
                  Create an account
                </Link>
              </p>
            </form>
          </CardContent>
        </Card>
      </div>
    </div>
  )
}

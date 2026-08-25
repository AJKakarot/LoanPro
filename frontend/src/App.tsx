import { QueryClient, QueryClientProvider } from "@tanstack/react-query"
import { BrowserRouter, Navigate, Route, Routes } from "react-router-dom"
import { Toaster } from "sonner"
import { AppLayout } from "@/components/AppLayout"
import { ProtectedRoute } from "@/components/ProtectedRoute"
import { AuthProvider } from "@/lib/auth"
import { AdminApplicationsPage, CheckerQueuePage, MakerQueuePage } from "@/pages/QueuePages"
import { AdminProductsPage } from "@/pages/AdminProductsPage"
import { AdminUsersPage } from "@/pages/AdminUsersPage"
import { ApplicationDetailPage } from "@/pages/ApplicationDetailPage"
import { ApplicationsPage } from "@/pages/ApplicationsPage"
import { ApplyPage } from "@/pages/ApplyPage"
import { AuditLogsPage } from "@/pages/AuditLogsPage"
import { DashboardPage } from "@/pages/DashboardPage"
import { LoginPage } from "@/pages/LoginPage"
import { NotificationsPage } from "@/pages/NotificationsPage"
import { ProfilePage } from "@/pages/ProfilePage"
import { RegisterPage } from "@/pages/RegisterPage"

const queryClient = new QueryClient({
  defaultOptions: { queries: { retry: 1, refetchOnWindowFocus: false } },
})

export default function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <AuthProvider>
        <BrowserRouter>
          <Routes>
            <Route path="/login" element={<LoginPage />} />
            <Route path="/register" element={<RegisterPage />} />
            <Route
              path="/app"
              element={
                <ProtectedRoute>
                  <AppLayout />
                </ProtectedRoute>
              }
            >
              <Route index element={<DashboardPage />} />
              <Route path="apply" element={<ProtectedRoute roles={["CUSTOMER"]}><ApplyPage /></ProtectedRoute>} />
              <Route path="applications" element={<ProtectedRoute roles={["CUSTOMER"]}><ApplicationsPage /></ProtectedRoute>} />
              <Route path="applications/:id" element={<ApplicationDetailPage />} />
              <Route path="profile" element={<ProfilePage />} />
              <Route path="notifications" element={<NotificationsPage />} />
              <Route path="maker" element={<ProtectedRoute roles={["MAKER"]}><MakerQueuePage /></ProtectedRoute>} />
              <Route path="checker" element={<ProtectedRoute roles={["CHECKER"]}><CheckerQueuePage /></ProtectedRoute>} />
              <Route path="admin/users" element={<ProtectedRoute roles={["ADMIN"]}><AdminUsersPage /></ProtectedRoute>} />
              <Route path="admin/products" element={<ProtectedRoute roles={["ADMIN"]}><AdminProductsPage /></ProtectedRoute>} />
              <Route path="admin/applications" element={<ProtectedRoute roles={["ADMIN"]}><AdminApplicationsPage /></ProtectedRoute>} />
              <Route path="admin/audit" element={<ProtectedRoute roles={["ADMIN"]}><AuditLogsPage /></ProtectedRoute>} />
            </Route>
            <Route path="*" element={<Navigate to="/login" replace />} />
          </Routes>
        </BrowserRouter>
        <Toaster richColors position="top-right" />
      </AuthProvider>
    </QueryClientProvider>
  )
}

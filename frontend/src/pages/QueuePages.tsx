import { ApplicationsPage } from "@/pages/ApplicationsPage"

export function MakerQueuePage() {
  return <ApplicationsPage endpoint="/maker/applications" title="Pending Applications" />
}

export function CheckerQueuePage() {
  return <ApplicationsPage endpoint="/checker/applications" title="Verification Queue" />
}

export function AdminApplicationsPage() {
  return <ApplicationsPage endpoint="/admin/applications" title="All Applications" />
}

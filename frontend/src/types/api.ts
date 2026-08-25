export type Role = "CUSTOMER" | "MAKER" | "CHECKER" | "ADMIN"

export type AuthUser = {
  id: string
  email: string
  firstName: string
  lastName: string
  phone?: string
  status: string
  roles: Role[]
}

export type PageResponse<T> = {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export type ApiResponse<T> = {
  success: boolean
  message: string
  data: T
}

export type ApplicationStatus =
  | "DRAFT"
  | "SUBMITTED"
  | "MAKER_REVIEW"
  | "INFO_REQUESTED"
  | "MAKER_VERIFIED"
  | "CHECKER_REVIEW"
  | "RETURNED_TO_MAKER"
  | "APPROVED"
  | "REJECTED"

export type ApplicationSummary = {
  id: string
  applicationNumber: string
  customerId: string
  customerName: string
  customerEmail: string
  loanProductId: string
  loanProductName: string
  requestedAmount: number
  tenureMonths: number
  interestRate: number
  purpose: string
  status: ApplicationStatus
  submittedAt?: string
  createdAt: string
  updatedAt: string
}

export type ApplicationDetail = ApplicationSummary & {
  loanProductCode: string
  processingFeePercent: number
  fullName?: string
  dateOfBirth?: string
  gender?: string
  nationalId?: string
  phone?: string
  email?: string
  addressLine?: string
  city?: string
  state?: string
  postalCode?: string
  employmentType?: string
  employerName?: string
  designation?: string
  yearsEmployed?: number
  monthlyIncome?: number
  otherIncome?: number
  existingEmis?: number
  monthlyExpenses?: number
  assignedMakerId?: string
  assignedMakerName?: string
  assignedCheckerId?: string
  assignedCheckerName?: string
  decidedAt?: string
}

export type LoanProduct = {
  id: string
  code: string
  name: string
  description?: string
  minAmount: number
  maxAmount: number
  minTenureMonths: number
  maxTenureMonths: number
  interestRate: number
  processingFeePercent: number
  requiredDocuments: string
  active: boolean
}

export type DashboardStats = {
  totalApplications: number
  pendingApplications: number
  makerReview: number
  checkerReview: number
  approved: number
  rejected: number
  totalLoanAmount: number
  approvedLoanAmount: number
  recentApplications: ApplicationSummary[]
}

export type Eligibility = {
  estimatedEmi: number
  debtToIncomeRatio: number
  nv3Score: number
  riskBand: string
  eligible: boolean
  summary: string
  processingFee: number
  feesPercent: number
  interestRate: number
}

export type AiAnalysis = {
  available: boolean
  message?: string
  documentStatus?: string
  eligibilityAssessment?: string
  riskLevel?: string
  keyIssues: string[]
  recommendedManualChecks: string[]
  summary?: string
  disclaimer?: string
}

export type LoanDocument = {
  id: string
  documentType: string
  originalFileName: string
  contentType: string
  fileSize: number
  verificationStatus: "PENDING" | "VERIFIED" | "REJECTED"
  verificationRemarks?: string
  uploadedBy: string
  createdAt: string
}

export type NotificationItem = {
  id: string
  title: string
  message: string
  type: string
  read: boolean
  applicationId?: string
  createdAt: string
}

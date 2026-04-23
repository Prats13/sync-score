import { apiFetch } from "./client"
import type {
  ArchitectureProfileResponse,
  ArchitectureScanResponse,
  ReviewCaseResponse,
} from "@/lib/types"

export const architectureApi = {
  getArchitectureProfile: (agencyId: string) =>
    apiFetch<ArchitectureProfileResponse>(`/api/v2/profile/${agencyId}/architecture`, { noAuth: true }),

  triggerScan: () =>
    apiFetch<ArchitectureScanResponse>(`/api/v2/scans/trigger`, { method: "POST" }),

  adminListOpenReviewCases: () =>
    apiFetch<ReviewCaseResponse[]>(`/api/v2/admin/review-cases`, { method: "GET" }),

  adminResolveReviewCase: (caseId: string, action: "APPROVE" | "DISMISS", note?: string) =>
    apiFetch<ReviewCaseResponse>(`/api/v2/admin/review-cases/${caseId}/resolve`, {
      method: "POST",
      body: JSON.stringify({ action, note: note ?? "" }),
    }),
}


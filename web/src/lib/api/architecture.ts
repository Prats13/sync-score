import { apiFetch } from "./client"
import type {
  ArchitectureProfileResponse,
  ArchitectureScanResponse,
  ScanDetailResponse,
  ReviewCaseResponse,
} from "@/lib/types"

export const architectureApi = {
  getArchitectureProfile: (agencyId: string) =>
    apiFetch<ArchitectureProfileResponse>(`/api/v2/profile/${agencyId}/architecture`, { noAuth: true }),

  triggerScan: (source?: string, exclusions?: string[], customExclusions?: string) =>
    apiFetch<ArchitectureScanResponse>(`/api/v2/scans/trigger`, {
      method: "POST",
      body: source ? JSON.stringify({ source, exclusions: exclusions ?? [], customExclusions: customExclusions ?? "" }) : undefined,
    }),

  getScan: (scanId: string) =>
    apiFetch<ArchitectureScanResponse>(`/api/v2/scans/${scanId}`),

  getScanDetail: (scanId: string) =>
    apiFetch<ScanDetailResponse>(`/api/v2/scans/${scanId}/detail`),

  listScans: () =>
    apiFetch<ArchitectureScanResponse[]>(`/api/v2/scans`),

  adminListOpenReviewCases: () =>
    apiFetch<ReviewCaseResponse[]>(`/api/v2/admin/review-cases`, { method: "GET" }),

  adminResolveReviewCase: (caseId: string, action: "APPROVE" | "DISMISS", note?: string) =>
    apiFetch<ReviewCaseResponse>(`/api/v2/admin/review-cases/${caseId}/resolve`, {
      method: "POST",
      body: JSON.stringify({ action, note: note ?? "" }),
    }),
}


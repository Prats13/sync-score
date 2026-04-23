import { apiFetch } from "./client"
import type { VerificationResponse, GithubVerificationRequest, PasteVerificationRequest } from "@/lib/types"

export const verificationApi = {
  github: (data: GithubVerificationRequest) =>
    apiFetch<VerificationResponse>("/api/v1/verifications/github", {
      method: "POST",
      body: JSON.stringify(data),
    }),

  paste: (data: PasteVerificationRequest) =>
    apiFetch<VerificationResponse>("/api/v1/verifications/paste", {
      method: "POST",
      body: JSON.stringify(data),
    }),

  rescan: (agencyId: string) =>
    apiFetch<VerificationResponse>(`/api/v1/verifications/${agencyId}/rescan`, {
      method: "POST",
    }),
}

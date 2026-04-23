import { apiFetch } from "./client"
import type { AgencyResponse, AgencyUpsertRequest, AgencyDashboardResponse } from "@/lib/types"

export const agencyApi = {
  me: () =>
    apiFetch<AgencyResponse>("/api/v1/agencies/me"),

  upsert: (data: AgencyUpsertRequest) =>
    apiFetch<AgencyResponse>("/api/v1/agencies", {
      method: "POST",
      body: JSON.stringify(data),
    }),

  dashboard: (agencyId: string) =>
    apiFetch<AgencyDashboardResponse>(`/api/v1/agencies/${agencyId}/dashboard`),
}

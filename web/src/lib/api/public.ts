import { apiFetch } from "./client"
import type { BrowseAgency, PublicAgencyProfile, SyncTier } from "@/lib/types"

export const publicApi = {
  browse: (tier?: SyncTier) => {
    const params = tier ? `?tier=${tier}` : ""
    return apiFetch<BrowseAgency[]>(`/api/v1/browse${params}`, { noAuth: true })
  },

  agencyProfile: (slug: string) =>
    apiFetch<PublicAgencyProfile>(`/api/v1/public/agencies/${slug}`, { noAuth: true }),
}

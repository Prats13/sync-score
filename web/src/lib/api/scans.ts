import { apiFetch } from "./client"
import type { ScanStatusResponse } from "@/lib/types"

export const scansApi = {
  get: (scanId: string) =>
    apiFetch<ScanStatusResponse>(`/api/v1/scans/${scanId}`),
}

// ── Enums ──────────────────────────────────────────────────────────────────

export type SyncTier = "WRAPPER" | "BUILDER" | "EXPERT"
export type EvidenceGrade = "VERIFIED" | "DECLARED" | "INFERRED" | "NOT_TESTED"
export type ScanStatus = "QUEUED" | "RUNNING" | "SUCCEEDED" | "FAILED"
export type TriggerType = "GITHUB_SCAN" | "PASTE_SCAN" | "RESCAN"
export type VerificationLabel = "GITHUB_VERIFIED" | "SELF_REPORTED" | "UNVERIFIED"
export type SubmissionStatus = "PENDING" | "ACTIVE" | "FAILED"

// ── V2 (Architecture) ──────────────────────────────────────────────────────

export type ArchConfidence = "LOW" | "MEDIUM" | "HIGH"
export type ArchStatus = "VERIFIED" | "UNDER_REVIEW" | "EVIDENCE_MISMATCH" | "FRESHNESS_LOW"

export interface ArchitectureProfileResponse {
  agencyId: string
  confidence: ArchConfidence | null
  archStatus: ArchStatus | null
  evidenceSource: string
  hasOpenReviewCase: boolean
  lastVerifiedAt: string | null
}

export interface ArchitectureScanResponse {
  id: string
  agencyId: string
  status: ScanStatus
  confidence: ArchConfidence | null
  archStatus: ArchStatus | null
  evidenceSource: string
  rulesetVersion: string
  createdAt: string
  finishedAt: string | null
}

export interface ReviewCaseResponse {
  id: string
  agencyId: string
  architectureScanId: string
  status: string
  triggerReason: string
  triggerDetailsJson: unknown
  resolvedBy: string | null
  resolutionNote: string | null
  createdAt: string
  resolvedAt: string | null
}

// ── Auth ───────────────────────────────────────────────────────────────────

export interface TokenPair {
  accessToken: string
  refreshToken: string
}

export interface SignupTokenResponse {
  signupToken: string
}

export interface MeResponse {
  id: string
  username: string
  email: string | null
}

// Auth request bodies
export interface EmailStartRequest { email: string }
export interface EmailVerifyOtpRequest { email: string; otp: string }
export interface SignupCompleteRequest { username: string; password: string }
export interface LoginRequest { username: string; password: string }
export interface GoogleAuthRequest { idToken: string }

// ── Agency ─────────────────────────────────────────────────────────────────

export interface AgencyResponse {
  agencyId: string
  name: string
  niche: string | null
  websiteUrl: string | null
  description: string | null
  bookingUrl: string | null
  githubUsername: string | null
  isPublic: boolean
  publicSlug: string | null
  rescanCount: number
  rescanLimit: number
  repoScanLimit: number
  createdAt: string
  updatedAt: string
}

export interface AgencyUpsertRequest {
  name: string
  niche?: string
  websiteUrl?: string
  description?: string
  bookingUrl?: string
  githubUsername?: string
  isPublic?: boolean
}

// ── Dashboard ──────────────────────────────────────────────────────────────

export interface ScanSummary {
  scanId: string
  triggerType: TriggerType
  status: ScanStatus
  verificationLabel: VerificationLabel
  createdAt: string
}

export interface LatestScore {
  totalScore: number | null
  tier: SyncTier | null
  verificationLabel: VerificationLabel
  scanId: string
  rulesetVersion: string | null
  createdAt: string
}

export interface AgencyDashboardResponse {
  agency: AgencyResponse
  latestScore: LatestScore | null
  scans: ScanSummary[]
}

// ── Verification ───────────────────────────────────────────────────────────

export interface GithubVerificationRequest { githubUsername: string }
export interface PasteVerificationRequest { content: string; manifestType?: string }
export interface VerificationResponse { scanId: string }

// ── Scans ──────────────────────────────────────────────────────────────────

/** UI helper type — not directly from the API */
export interface CategorySubtotal {
  category: string
  points: number
  cap: number
}

export interface StackChipData {
  packageName: string
  category: string
  pointsAwarded: number
}

/** Matches the backend ScanStatusResponse.Score record */
export interface ScanScore {
  totalScore: number
  tier: SyncTier
  /** Map<String,Integer> serialised as { "Orchestration": 12, ... } */
  categorySubtotals: Record<string, number>
}

/** Matches the backend ScanStatusResponse record exactly */
export interface ScanStatusResponse {
  scanId: string
  agencyId: string
  triggerType: TriggerType
  status: ScanStatus
  errorMessage: string | null
  rulesetVersion: string | null
  verificationLabel: VerificationLabel
  evidenceItemIds: unknown
  startedAt: string | null
  finishedAt: string | null
  createdAt: string
  score: ScanScore | null
  repositories: Array<{
    repositoryScanId: string
    repoFullName: string
    repoUrl: string
    defaultBranch: string
    status: string
    manifestsFound: unknown
  }>
  detectedPackages: Array<{
    packageName: string
    category: string
    pointsAwarded: number
    manifestType: string | null
    manifestPath: string | null
    repositoryScanId: string
  }>
}

// ── Category caps (from syncscore_v1.json ruleset) ─────────────────────────

export const CATEGORY_CAPS: Record<string, number> = {
  "Orchestration":  30,
  "RAG & Retrieval": 20,
  "Memory & State": 15,
  "Guardrails":     15,
  "Observability":  15,
  "Base SDK":        5,
}

/** Convert API's Record<string,number> into the UI CategorySubtotal[] */
export function toCategorySubtotals(raw: Record<string, number> | null | undefined): CategorySubtotal[] {
  if (!raw) return []
  return Object.entries(CATEGORY_CAPS).map(([category, cap]) => ({
    category,
    points: raw[category] ?? 0,
    cap,
  }))
}

// ── Public / Browse ────────────────────────────────────────────────────────

export interface BrowseAgency {
  slug: string
  agencyId: string
  agencyName: string
  niche: string | null
  websiteUrl: string | null
  bookingUrl: string | null
  totalScore: number | null
  tier: SyncTier | null
  verificationLabel: VerificationLabel
}

export interface PublicAgencyProfile {
  agencyId: string
  slug: string
  verificationLabel: VerificationLabel
  agency: {
    name: string
    niche: string | null
    websiteUrl: string | null
    description: string | null
    bookingUrl: string | null
  }
  score: {
    totalScore: number | null
    tier: SyncTier | null
    rulesetVersion: string | null
    /** Map<String,Integer> from backend: { "Orchestration": 12, ... } */
    categorySubtotals: Record<string, number> | null
  }
  stack: StackChipData[]
}

// ── API Error ──────────────────────────────────────────────────────────────

export interface ApiError {
  status: number
  message: string
}

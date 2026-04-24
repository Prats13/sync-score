import { cn } from "@/lib/utils"
import type { ArchConfidence, ArchStatus, VerificationLabel } from "@/lib/types"
import { ScoreBadge } from "@/components/ui/score-badge"
import { TrustLabel } from "@/components/ui/trust-label"

const EVIDENCE_SOURCE_LABELS: Record<string, string> = {
  GITHUB_PUBLIC:        "Public GitHub",
  CONFIDENTIAL_GITHUB:  "Confidential GitHub",
  CONFIDENTIAL_SESSION: "Confidential Session",
  MIXED_EVIDENCE:       "Mixed Evidence",
  PASTE:                "Paste Upload",
}

function chipForArch(confidence: ArchConfidence | null | undefined) {
  if (!confidence) return null
  if (confidence === "HIGH") return { label: "ARCH HIGH", className: "border-[#2ECC71] text-verified" }
  if (confidence === "MEDIUM") return { label: "ARCH MED", className: "border-amber-300 text-amber-700" }
  return { label: "ARCH LOW", className: "border-red-300 text-red-700" }
}

function chipForStatus(status: ArchStatus | null | undefined) {
  if (!status || status === "VERIFIED") return null
  if (status === "UNDER_REVIEW") return { label: "UNDER REVIEW", className: "border-amber-300 text-amber-800 bg-amber-50" }
  if (status === "EVIDENCE_MISMATCH") return { label: "MISMATCH", className: "border-red-300 text-red-800 bg-red-50" }
  return { label: "FRESHNESS LOW", className: "border-hairline-strong text-muted bg-surface-1" }
}

export function UnifiedTrustCard({
  score,
  tier,
  verificationLabel,
  archConfidence,
  archStatus,
  evidenceSource,
  freshnessLabel,
  className,
}: {
  score: number | null | undefined
  tier: "WRAPPER" | "BUILDER" | "EXPERT" | null | undefined
  verificationLabel: VerificationLabel
  archConfidence: ArchConfidence | null | undefined
  archStatus: ArchStatus | null | undefined
  evidenceSource?: string | null
  freshnessLabel?: string | null
  className?: string
}) {
  const archChip = chipForArch(archConfidence)
  const statusChip = chipForStatus(archStatus)

  return (
    <div className={cn("rounded-[23px] border-2 border-hairline-strong bg-surface-inset p-6", className)}>
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <p className="text-xs font-semibold uppercase tracking-widest text-muted">
            SyncScore verification
          </p>
          <div className="mt-2 flex items-center gap-3">
            {tier && <ScoreBadge tier={tier} score={score ?? undefined} />}
            {archChip && (
              <span className={cn("rounded-full border-2 px-3 py-1 text-xs font-semibold", archChip.className)}>
                {archChip.label}
              </span>
            )}
            {statusChip && (
              <span className={cn("rounded-full border px-3 py-1 text-xs font-semibold", statusChip.className)}>
                {statusChip.label}
              </span>
            )}
          </div>
        </div>

        <div className="flex flex-col items-end gap-2">
          <TrustLabel label={verificationLabel} />
          <div className="flex flex-wrap justify-end gap-2 text-[10px] font-medium text-muted">
            {evidenceSource && (
              <span className="rounded-full border border-hairline-strong bg-surface-1 px-2 py-1">
                {EVIDENCE_SOURCE_LABELS[evidenceSource] ?? evidenceSource.replace(/_/g, " ")}
              </span>
            )}
            {freshnessLabel && (
              <span className="rounded-full border border-hairline-strong bg-surface-1 px-2 py-1">
                {freshnessLabel}
              </span>
            )}
          </div>
        </div>
      </div>
    </div>
  )
}


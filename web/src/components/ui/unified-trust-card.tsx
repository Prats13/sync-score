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
  if (!confidence) return { label: "ARCH —", className: "border-[#D7D3CB] text-[#6B6B6B]" }
  if (confidence === "HIGH") return { label: "ARCH HIGH", className: "border-[#2ECC71] text-[#279455]" }
  if (confidence === "MEDIUM") return { label: "ARCH MED", className: "border-amber-300 text-amber-700" }
  return { label: "ARCH LOW", className: "border-red-300 text-red-700" }
}

function chipForStatus(status: ArchStatus | null | undefined) {
  if (!status || status === "VERIFIED") return null
  if (status === "UNDER_REVIEW") return { label: "UNDER REVIEW", className: "border-amber-300 text-amber-800 bg-amber-50" }
  if (status === "EVIDENCE_MISMATCH") return { label: "MISMATCH", className: "border-red-300 text-red-800 bg-red-50" }
  return { label: "FRESHNESS LOW", className: "border-[#D7D3CB] text-[#6B6B6B] bg-white" }
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
    <div className={cn("rounded-[23px] border-2 border-[#D7D3CB] bg-[#F6F6F3] p-6", className)}>
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <p className="text-xs font-semibold uppercase tracking-widest text-[#6B6B6B]">
            SyncScore verification
          </p>
          <div className="mt-2 flex items-center gap-3">
            {tier && <ScoreBadge tier={tier} score={score ?? undefined} />}
            <span className={cn("rounded-full border-2 px-3 py-1 text-xs font-semibold", archChip.className)}>
              {archChip.label}
            </span>
            {statusChip && (
              <span className={cn("rounded-full border px-3 py-1 text-xs font-semibold", statusChip.className)}>
                {statusChip.label}
              </span>
            )}
          </div>
        </div>

        <div className="flex flex-col items-end gap-2">
          <TrustLabel label={verificationLabel} />
          <div className="flex flex-wrap justify-end gap-2 text-[10px] font-medium text-[#6B6B6B]">
            {evidenceSource && (
              <span className="rounded-full border border-[#D7D3CB] bg-white px-2 py-1">
                {EVIDENCE_SOURCE_LABELS[evidenceSource] ?? evidenceSource.replace(/_/g, " ")}
              </span>
            )}
            {freshnessLabel && (
              <span className="rounded-full border border-[#D7D3CB] bg-white px-2 py-1">
                {freshnessLabel}
              </span>
            )}
          </div>
        </div>
      </div>
    </div>
  )
}


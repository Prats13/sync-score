import type { EvidenceGrade } from "@/lib/types"

const CONFIG: Record<EvidenceGrade, { label: string; bg: string; text: string; dot: string }> = {
  VERIFIED:   { label: "Verified",   bg: "bg-verified-bg", text: "text-verified", dot: "bg-verified" },
  DECLARED:   { label: "Declared",   bg: "bg-declared-bg", text: "text-declared", dot: "bg-[#F59E0B]" },
  INFERRED:   { label: "Inferred",   bg: "bg-inferred-bg", text: "text-inferred", dot: "bg-[#6366F1]" },
  NOT_TESTED: { label: "Not tested", bg: "bg-surface-inset", text: "text-muted", dot: "bg-hairline-strong" },
}

interface Props {
  grade: EvidenceGrade
  className?: string
}

export function EvidenceBadge({ grade, className = "" }: Props) {
  const cfg = CONFIG[grade]
  return (
    <span
      className={`inline-flex items-center gap-1.5 rounded-full px-2.5 py-0.5 text-xs font-medium ${cfg.bg} ${cfg.text} ${className}`}
    >
      <span className={`h-1.5 w-1.5 rounded-full ${cfg.dot}`} />
      {cfg.label}
    </span>
  )
}

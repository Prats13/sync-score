import type { EvidenceGrade } from "@/lib/types"

const CONFIG: Record<EvidenceGrade, { label: string; bg: string; text: string; dot: string }> = {
  VERIFIED:   { label: "Verified",   bg: "bg-[#EBFFF2]", text: "text-[#279455]", dot: "bg-[#2ECC71]" },
  DECLARED:   { label: "Declared",   bg: "bg-[#FFF8E1]", text: "text-[#B45309]", dot: "bg-[#F59E0B]" },
  INFERRED:   { label: "Inferred",   bg: "bg-[#EEF2FF]", text: "text-[#4338CA]", dot: "bg-[#6366F1]" },
  NOT_TESTED: { label: "Not tested", bg: "bg-[#F6F6F3]", text: "text-[#6B6B6B]", dot: "bg-[#D7D3CB]" },
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

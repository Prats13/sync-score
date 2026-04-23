import { cva, type VariantProps } from "class-variance-authority"
import { cn } from "@/lib/utils"
import type { SyncTier } from "@/lib/types"

const scoreBadgeVariants = cva(
  "inline-flex items-center gap-1.5 rounded-full px-3 py-1 text-xs font-semibold uppercase tracking-widest border",
  {
    variants: {
      tier: {
        WRAPPER: "bg-gray-100 text-gray-700 border-gray-200",
        BUILDER: "bg-blue-50 text-blue-700 border-blue-200",
        EXPERT: "bg-[#EBFFF2] text-[#279455] border-[#2ECC71]",
      },
    },
    defaultVariants: {
      tier: "WRAPPER",
    },
  },
)

interface ScoreBadgeProps extends VariantProps<typeof scoreBadgeVariants> {
  tier: SyncTier
  score?: number | null
  className?: string
}

const TIER_LABELS: Record<SyncTier, string> = {
  WRAPPER: "Wrapper",
  BUILDER: "Builder",
  EXPERT: "Expert",
}

export function ScoreBadge({ tier, score, className }: ScoreBadgeProps) {
  return (
    <span className={cn(scoreBadgeVariants({ tier }), className)}>
      {score != null && (
        <span className="font-bold tabular-nums">{score}</span>
      )}
      {TIER_LABELS[tier]}
    </span>
  )
}

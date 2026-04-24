import { cva, type VariantProps } from "class-variance-authority"
import { cn } from "@/lib/utils"
import type { VerificationLabel } from "@/lib/types"

const trustLabelVariants = cva(
  "inline-flex items-center gap-1.5 rounded-full px-3 py-1 text-xs font-semibold uppercase tracking-widest border",
  {
    variants: {
      label: {
        GITHUB_VERIFIED: "bg-verified-bg text-verified border-[#2ECC71]",
        SELF_REPORTED: "bg-gray-50 text-gray-500 border-gray-200",
        UNVERIFIED: "bg-surface-inset text-muted border-hairline-strong",
      },
    },
    defaultVariants: {
      label: "UNVERIFIED",
    },
  },
)

const LABEL_TEXT: Record<VerificationLabel, string> = {
  GITHUB_VERIFIED: "SyncScore Verified",
  SELF_REPORTED: "Self-Reported",
  UNVERIFIED: "Non-Verified",
}

const LABEL_DOT: Record<VerificationLabel, string> = {
  GITHUB_VERIFIED: "bg-verified",
  SELF_REPORTED: "bg-gray-400",
  UNVERIFIED: "bg-gray-300",
}

interface TrustLabelProps extends VariantProps<typeof trustLabelVariants> {
  label: VerificationLabel
  className?: string
}

export function TrustLabel({ label, className }: TrustLabelProps) {
  return (
    <span className={cn(trustLabelVariants({ label }), className)}>
      <span className={cn("h-1.5 w-1.5 rounded-full", LABEL_DOT[label])} />
      {LABEL_TEXT[label]}
    </span>
  )
}

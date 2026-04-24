import Link from "next/link"
import { ScoreBadge } from "./score-badge"
import { TrustLabel } from "./trust-label"
import { cn } from "@/lib/utils"
import type { BrowseAgency } from "@/lib/types"

interface AgentCardProps {
  agency: BrowseAgency
  className?: string
}

export function AgentCard({ agency, className }: AgentCardProps) {
  return (
    <Link
      href={`/agents/${agency.slug}`}
      className={cn(
        "group block rounded-[23px] border-2 border-hairline-strong bg-surface-inset p-6",
        "transition-all duration-200 hover:-translate-y-1 hover:border-trust-border hover:shadow-md",
        className,
      )}
    >
      <div className="flex items-start justify-between gap-3">
        <div className="min-w-0 flex-1">
          <h3 className="truncate text-base font-semibold text-ink group-hover:underline">
            {agency.agencyName}
          </h3>
          {agency.niche && (
            <p className="mt-0.5 truncate text-sm text-muted">{agency.niche}</p>
          )}
        </div>
        {agency.tier && (
          <ScoreBadge tier={agency.tier} score={agency.totalScore} />
        )}
      </div>

      <div className="mt-4 flex items-center gap-2">
        <TrustLabel label={agency.verificationLabel} />
      </div>

      {agency.websiteUrl && (
        <p className="mt-3 truncate text-xs text-muted">
          {agency.websiteUrl.replace(/^https?:\/\//, "")}
        </p>
      )}
    </Link>
  )
}

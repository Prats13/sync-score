"use client"

import { useState, useEffect } from "react"
import { publicApi } from "@/lib/api/public"
import { architectureApi } from "@/lib/api/architecture"
import { AgentCard } from "@/components/ui/agent-card"
import type { ArchConfidence, ArchStatus, BrowseAgency, SyncTier } from "@/lib/types"

const TIERS: Array<{ label: string; value: SyncTier | undefined }> = [
  { label: "All", value: undefined },
  { label: "Wrapper", value: "WRAPPER" },
  { label: "Builder", value: "BUILDER" },
  { label: "Expert", value: "EXPERT" },
]

const ARCH_CONFIDENCE: Array<{ label: string; value: ArchConfidence | undefined }> = [
  { label: "Any arch", value: undefined },
  { label: "High", value: "HIGH" },
  { label: "Medium", value: "MEDIUM" },
  { label: "Low", value: "LOW" },
]

const ARCH_STATUS: Array<{ label: string; value: ArchStatus | undefined }> = [
  { label: "Any status", value: undefined },
  { label: "Verified", value: "VERIFIED" },
  { label: "Under review", value: "UNDER_REVIEW" },
  { label: "Mismatch", value: "EVIDENCE_MISMATCH" },
  { label: "Freshness low", value: "FRESHNESS_LOW" },
]

export default function BrowsePage() {
  const [activeTier, setActiveTier] = useState<SyncTier | undefined>(undefined)
  const [activeArchConfidence, setActiveArchConfidence] = useState<ArchConfidence | undefined>(undefined)
  const [activeArchStatus, setActiveArchStatus] = useState<ArchStatus | undefined>(undefined)
  const [agencies, setAgencies] = useState<BrowseAgency[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [archIndex, setArchIndex] = useState<Record<string, { confidence: ArchConfidence | null; archStatus: ArchStatus | null }>>({})

  useEffect(() => {
    setIsLoading(true)
    publicApi
      .browse(activeTier)
      .then((rows) => {
        setAgencies(rows)
        return rows
      })
      .then(async (rows) => {
        const results = await Promise.allSettled(
          rows.map(async (a) => {
            const ap = await architectureApi.getArchitectureProfile(a.agencyId)
            return { agencyId: a.agencyId, confidence: ap.confidence, archStatus: ap.archStatus }
          }),
        )
        const next: Record<string, { confidence: ArchConfidence | null; archStatus: ArchStatus | null }> = {}
        for (const r of results) {
          if (r.status === "fulfilled") {
            next[r.value.agencyId] = { confidence: r.value.confidence, archStatus: r.value.archStatus }
          }
        }
        setArchIndex(next)
      })
      .finally(() => setIsLoading(false))
  }, [activeTier])

  const filtered = agencies.filter((a) => {
    const arch = archIndex[a.agencyId]
    if (activeArchConfidence && arch?.confidence !== activeArchConfidence) return false
    if (activeArchStatus && arch?.archStatus !== activeArchStatus) return false
    return true
  })

  return (
    <div className="mx-auto max-w-6xl px-6 py-12">
      <div className="mb-10">
        <h1
          className="text-4xl text-[#000000]"
          style={{ fontFamily: "var(--font-dm-serif-display)" }}
        >
          Browse verified agents
        </h1>
        <p className="mt-2 text-[#6B6B6B]">
          Discover AI agent builders who have verified their technical stack with SyncScore.
        </p>
      </div>

      {/* Tier filter */}
      <div className="mb-8 flex gap-2 flex-wrap">
        {TIERS.map(({ label, value }) => (
          <button
            key={label}
            onClick={() => setActiveTier(value)}
            className={[
              "rounded-full border-2 px-5 py-1.5 text-sm font-medium transition-all",
              activeTier === value
                ? "border-[#10100F] bg-[#10100F] text-white"
                : "border-[#D7D3CB] text-[#6B6B6B] hover:border-[#10100F] hover:text-[#000000]",
            ].join(" ")}
          >
            {label}
          </button>
        ))}
      </div>

      {/* Architecture filters */}
      <div className="mb-8 flex flex-wrap gap-2">
        {ARCH_CONFIDENCE.map(({ label, value }) => (
          <button
            key={label}
            onClick={() => setActiveArchConfidence(value)}
            className={[
              "rounded-full border-2 px-4 py-1.5 text-sm font-medium transition-all",
              activeArchConfidence === value
                ? "border-[#10100F] bg-[#10100F] text-white"
                : "border-[#D7D3CB] text-[#6B6B6B] hover:border-[#10100F] hover:text-[#000000]",
            ].join(" ")}
          >
            {label}
          </button>
        ))}
        {ARCH_STATUS.map(({ label, value }) => (
          <button
            key={label}
            onClick={() => setActiveArchStatus(value)}
            className={[
              "rounded-full border-2 px-4 py-1.5 text-sm font-medium transition-all",
              activeArchStatus === value
                ? "border-[#10100F] bg-[#10100F] text-white"
                : "border-[#D7D3CB] text-[#6B6B6B] hover:border-[#10100F] hover:text-[#000000]",
            ].join(" ")}
          >
            {label}
          </button>
        ))}
      </div>

      {/* Grid */}
      {isLoading ? (
        <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
          {Array.from({ length: 6 }).map((_, i) => (
            <div
              key={i}
              className="h-40 animate-pulse rounded-[23px] bg-[#F6F6F3]"
            />
          ))}
        </div>
      ) : filtered.length === 0 ? (
        <div className="py-24 text-center text-[#6B6B6B]">
          No verified agents found.
        </div>
      ) : (
        <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
          {filtered.map((a) => (
            <AgentCard key={a.agencyId} agency={a} />
          ))}
        </div>
      )}
    </div>
  )
}

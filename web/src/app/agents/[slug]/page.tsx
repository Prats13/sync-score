import { notFound } from "next/navigation"
import { publicApi } from "@/lib/api/public"
import { architectureApi } from "@/lib/api/architecture"
import { CategoryBar } from "@/components/ui/category-bar"
import { StackChip } from "@/components/ui/stack-chip"
import { UnifiedTrustCard } from "@/components/ui/unified-trust-card"
import { toCategorySubtotals, type CategorySubtotal } from "@/lib/types"

interface Props {
  params: Promise<{ slug: string }>
}

function deriveStrengthsAndConsiderations(categories: CategorySubtotal[]) {
  const strengths = categories.filter((c) => c.cap > 0 && c.points / c.cap >= 0.8)
  const considerations = categories.filter((c) => c.cap > 0 && c.points === 0)
  return { strengths, considerations }
}

export default async function PublicAgencyProfilePage({ params }: Props) {
  const { slug } = await params

  let profile
  try {
    profile = await publicApi.agencyProfile(slug)
  } catch {
    notFound()
  }

  const { agency, score, stack, verificationLabel } = profile

  let archProfile = null
  try {
    archProfile = await architectureApi.getArchitectureProfile(profile.agencyId)
  } catch {
    // No V2 scan yet (or temporarily unavailable) — render without it.
  }

  const categories = toCategorySubtotals(score.categorySubtotals as Record<string, number> | null)
  const { strengths, considerations } = deriveStrengthsAndConsiderations(categories)

  return (
    <div className="mx-auto max-w-4xl px-6 py-12">
      {/* ── Header ────────────────────────────────────────────────────── */}
      <div className="rounded-[23px] border-2 border-[#D7D3CB] bg-[#F6F6F3] p-8 mb-6">
        <div className="flex flex-wrap items-start justify-between gap-4">
          <div>
            <h1
              className="text-3xl text-[#000000]"
              style={{ fontFamily: "var(--font-dm-serif-display)" }}
            >
              {agency.name}
            </h1>
            {agency.niche && (
              <p className="mt-1 text-[#6B6B6B]">{agency.niche}</p>
            )}
          </div>
        </div>

        {agency.description && (
          <p className="mt-5 text-sm leading-relaxed text-[#6B6B6B]">{agency.description}</p>
        )}

        <div className="mt-6">
          <UnifiedTrustCard
            score={score.totalScore}
            tier={score.tier}
            verificationLabel={verificationLabel}
            archConfidence={archProfile?.confidence}
            archStatus={archProfile?.archStatus}
            evidenceSource={archProfile?.evidenceSource ?? null}
            freshnessLabel={archProfile?.lastVerifiedAt ? `Verified ${new Date(archProfile.lastVerifiedAt).toLocaleDateString("en-US")}` : null}
          />
        </div>

        <div className="mt-6 flex flex-wrap gap-3">
          {agency.websiteUrl && (
            <a
              href={agency.websiteUrl}
              target="_blank"
              rel="noopener noreferrer"
              className="rounded-full border-2 border-[#D7D3CB] px-5 py-1.5 text-sm font-medium text-[#000000] transition-colors hover:border-[#10100F]"
            >
              Website ↗
            </a>
          )}
          {agency.bookingUrl && (
            <a
              href={agency.bookingUrl}
              target="_blank"
              rel="noopener noreferrer"
              className="rounded-full bg-[#10100F] px-5 py-1.5 text-sm font-medium text-white transition-opacity hover:opacity-80"
            >
              Book a call
            </a>
          )}
        </div>
      </div>

      <div className="grid gap-6 lg:grid-cols-2">
        {/* ── Score breakdown ───────────────────────────────────────── */}
        {categories.length > 0 && (
          <div className="rounded-[23px] border-2 border-[#D7D3CB] bg-[#F6F6F3] p-6">
            <h2 className="mb-5 text-lg font-semibold text-[#000000]">Score breakdown</h2>
            <div className="space-y-4">
              {categories.map((c) => (
                <CategoryBar
                  key={c.category}
                  label={c.category}
                  points={c.points}
                  cap={c.cap}
                />
              ))}
            </div>
            {score.totalScore != null && (
              <div className="mt-6 flex items-center justify-between border-t-2 border-[#D7D3CB] pt-4">
                <span className="text-sm font-medium text-[#6B6B6B]">Total score</span>
                <span className="text-2xl font-bold tabular-nums text-[#000000]">
                  {score.totalScore}
                </span>
              </div>
            )}
          </div>
        )}

        {/* ── Strengths & Considerations ───────────────────────────── */}
        <div className="space-y-4">
          {strengths.length > 0 && (
            <div className="rounded-[23px] border-2 border-[#2ECC71] bg-[#EBFFF2] p-6">
              <h2 className="mb-3 text-base font-semibold text-[#279455]">Strengths</h2>
              <ul className="space-y-1.5">
                {strengths.map((s) => (
                  <li key={s.category} className="flex items-center gap-2 text-sm text-[#000000]">
                    <span className="h-1.5 w-1.5 rounded-full bg-[#2ECC71]" />
                    {s.category} ({s.points}/{s.cap} pts)
                  </li>
                ))}
              </ul>
            </div>
          )}

          {considerations.length > 0 && (
            <div className="rounded-[23px] border-2 border-[#D7D3CB] bg-[#F6F6F3] p-6">
              <h2 className="mb-3 text-base font-semibold text-[#6B6B6B]">Considerations</h2>
              <ul className="space-y-1.5">
                {considerations.map((c) => (
                  <li key={c.category} className="flex items-center gap-2 text-sm text-[#6B6B6B]">
                    <span className="h-1.5 w-1.5 rounded-full bg-[#D7D3CB]" />
                    No {c.category} layer detected
                  </li>
                ))}
              </ul>
            </div>
          )}
        </div>
      </div>

      {/* ── Detected stack ────────────────────────────────────────────── */}
      {stack.length > 0 && (
        <div className="mt-6 rounded-[23px] border-2 border-[#D7D3CB] bg-[#F6F6F3] p-6">
          <h2 className="mb-4 text-lg font-semibold text-[#000000]">Detected stack</h2>
          <div className="flex flex-wrap gap-2">
            {stack.map((pkg) => (
              <StackChip
                key={pkg.packageName}
                packageName={pkg.packageName}
                category={pkg.category}
                pointsAwarded={pkg.pointsAwarded}
              />
            ))}
          </div>
          {score.rulesetVersion && (
            <p className="mt-4 text-xs text-[#6B6B6B]">
              Ruleset: {score.rulesetVersion}
            </p>
          )}
        </div>
      )}
    </div>
  )
}

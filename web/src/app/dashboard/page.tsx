"use client"

import { useEffect, useState } from "react"
import Link from "next/link"
import { useRouter } from "next/navigation"
import { useAuth } from "@/lib/hooks/use-auth"
import { useAgency } from "@/lib/hooks/use-agency"
import { agencyApi } from "@/lib/api/agency"
import { verificationApi } from "@/lib/api/verification"
import { architectureApi } from "@/lib/api/architecture"
import { ApiError } from "@/lib/api/client"
import { ScoreBadge } from "@/components/ui/score-badge"
import { TrustLabel } from "@/components/ui/trust-label"
import { CompleteProfileModal } from "@/components/ui/complete-profile-modal"
import { Button } from "@/components/ui/button"
import { UnifiedTrustCard } from "@/components/ui/unified-trust-card"
import type { ArchitectureProfileResponse } from "@/lib/types"

function formatDate(iso: string) {
  return new Date(iso).toLocaleDateString("en-US", {
    month: "short",
    day: "numeric",
    year: "numeric",
  })
}

export default function DashboardPage() {
  const router = useRouter()
  const { isAuthenticated, isLoading: authLoading } = useAuth()
  const { agency, dashboard, notFound, isLoading, refresh } = useAgency()
  const [rescanError, setRescanError] = useState<string | null>(null)
  const [isRescanning, setIsRescanning] = useState(false)
  const [archProfile, setArchProfile] = useState<ArchitectureProfileResponse | null>(null)

  // Redirect unauthenticated users
  useEffect(() => {
    if (!authLoading && !isAuthenticated) {
      router.push("/auth/login")
    }
  }, [authLoading, isAuthenticated, router])

  useEffect(() => {
    if (!agency) return
    architectureApi
      .getArchitectureProfile(agency.agencyId)
      .then(setArchProfile)
      .catch(() => setArchProfile(null))
  }, [agency?.agencyId])

  const handleRescan = async () => {
    if (!agency) return
    setRescanError(null)
    setIsRescanning(true)
    try {
      const { scanId } = await verificationApi.rescan(agency.agencyId)
      router.push(`/dashboard/scan/${scanId}`)
    } catch (err) {
      if (err instanceof ApiError && err.status === 429) {
        setRescanError("You've reached your rescan limit. Upgrade for more scans.")
      } else {
        setRescanError(err instanceof Error ? err.message : "Rescan failed.")
      }
      setIsRescanning(false)
    }
  }

  const handlePublish = async () => {
    if (!agency) return
    try {
      await agencyApi.upsert({ name: agency.name, isPublic: true })
      await refresh()
    } catch {
      // silently fail — user can retry
    }
  }

  if (authLoading || isLoading) {
    return (
      <div className="mx-auto max-w-4xl px-6 py-12">
        <div className="space-y-4">
          {Array.from({ length: 3 }).map((_, i) => (
            <div key={i} className="h-24 animate-pulse rounded-[23px] bg-[#F6F6F3]" />
          ))}
        </div>
      </div>
    )
  }

  return (
    <>
      <CompleteProfileModal
        open={notFound}
        onComplete={() => refresh()}
      />

      <div className="mx-auto max-w-4xl px-6 py-12">
        <div className="mb-8 flex flex-wrap items-start justify-between gap-4">
          <div>
            <h1
              className="text-3xl text-[#000000]"
              style={{ fontFamily: "var(--font-dm-serif-display)" }}
            >
              {agency?.name ?? "Your Dashboard"}
            </h1>
            {agency?.niche && (
              <p className="mt-1 text-sm text-[#6B6B6B]">{agency.niche}</p>
            )}
          </div>
          <div className="flex gap-2">
            <Button asChild size="sm" variant="outline">
              <Link href="/dashboard/verify">
                {dashboard?.latestScore ? "Re-verify" : "Get verified"}
              </Link>
            </Button>
            {agency && !agency.isPublic && dashboard?.latestScore && (
              <Button
                size="sm"
                className="rounded-full bg-[#10100F] text-white hover:bg-[#10100F]/80"
                onClick={handlePublish}
              >
                Publish proof
              </Button>
            )}
            {agency?.isPublic && agency.publicSlug && (
              <Button asChild size="sm" variant="outline">
                <Link href={`/agents/${agency.publicSlug}`} target="_blank">
                  View public profile ↗
                </Link>
              </Button>
            )}
          </div>
        </div>

        {/* Latest score card */}
        {dashboard?.latestScore ? (
          <div className="mb-6 rounded-[23px] border-2 border-[#D7D3CB] bg-[#F6F6F3] p-6">
            <div className="flex flex-wrap items-center justify-between gap-4">
              <div>
                <p className="text-xs font-semibold uppercase tracking-widest text-[#6B6B6B]">
                  Latest score
                </p>
                {dashboard.latestScore.tier && dashboard.latestScore.totalScore != null ? (
                  <div className="mt-2 flex items-center gap-3">
                    <span className="text-4xl font-bold tabular-nums text-[#000000]">
                      {dashboard.latestScore.totalScore}
                    </span>
                    <ScoreBadge tier={dashboard.latestScore.tier} />
                  </div>
                ) : (
                  <p className="mt-2 text-sm text-[#6B6B6B]">Scan in progress…</p>
                )}
              </div>
              <div className="flex flex-col items-end gap-2">
                <TrustLabel label={dashboard.latestScore.verificationLabel} />
                <p className="text-xs text-[#6B6B6B]">
                  {formatDate(dashboard.latestScore.createdAt)}
                </p>
                {dashboard.latestScore.rulesetVersion && (
                  <p className="text-xs text-[#6B6B6B]">
                    {dashboard.latestScore.rulesetVersion}
                  </p>
                )}
              </div>
            </div>

            <div className="mt-5">
              <UnifiedTrustCard
                score={dashboard.latestScore.totalScore}
                tier={dashboard.latestScore.tier}
                verificationLabel={dashboard.latestScore.verificationLabel}
                archConfidence={archProfile?.confidence}
                archStatus={archProfile?.archStatus}
                evidenceSource={archProfile?.evidenceSource ?? null}
                freshnessLabel={archProfile?.lastVerifiedAt ? `Verified ${formatDate(archProfile.lastVerifiedAt)}` : null}
              />
            </div>

            {rescanError && (
              <div className="mt-4 rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-600">
                {rescanError}
              </div>
            )}

            <div className="mt-4 flex gap-2">
              <Button
                size="sm"
                variant="outline"
                disabled={isRescanning}
                onClick={handleRescan}
              >
                {isRescanning ? "Starting rescan…" : "Re-run scan"}
              </Button>
              <Button asChild size="sm" variant="ghost">
                <Link href={`/dashboard/scan/${dashboard.latestScore.scanId}`}>
                  View full report
                </Link>
              </Button>
            </div>
          </div>
        ) : (
          !notFound && (
            <div className="mb-6 rounded-[23px] border-2 border-dashed border-[#D7D3CB] bg-[#F6F6F3] p-8 text-center">
              <p className="mb-4 text-[#6B6B6B]">
                You haven&apos;t run a verification scan yet.
              </p>
              <Button asChild className="rounded-full bg-[#10100F] text-white hover:bg-[#10100F]/80">
                <Link href="/dashboard/verify">Get verified</Link>
              </Button>
            </div>
          )
        )}

        {/* Scan history */}
        {dashboard && dashboard.scans.length > 0 && (
          <div className="rounded-[23px] border-2 border-[#D7D3CB] bg-[#F6F6F3] p-6">
            <h2 className="mb-4 font-semibold text-[#000000]">Scan history</h2>
            <div className="space-y-2">
              {dashboard.scans.map((s) => (
                <Link
                  key={s.scanId}
                  href={`/dashboard/scan/${s.scanId}`}
                  className="flex items-center justify-between rounded-xl border-2 border-[#D7D3CB] bg-white px-4 py-3 transition-colors hover:border-[#10100F]"
                >
                  <div className="flex items-center gap-3">
                    <span
                      className={[
                        "h-2 w-2 rounded-full",
                        s.status === "SUCCEEDED"
                          ? "bg-[#2ECC71]"
                          : s.status === "FAILED"
                          ? "bg-red-400"
                          : "bg-amber-400",
                      ].join(" ")}
                    />
                    <span className="text-sm font-medium text-[#000000]">
                      {s.triggerType.replace("_", " ").toLowerCase()}
                    </span>
                    <TrustLabel label={s.verificationLabel} className="text-[10px]" />
                  </div>
                  <span className="text-xs text-[#6B6B6B]">{formatDate(s.createdAt)}</span>
                </Link>
              ))}
            </div>
          </div>
        )}

        {/* Rescan counter */}
        {agency && (
          <p className="mt-4 text-xs text-[#6B6B6B]">
            Rescans used: {agency.rescanCount} / {agency.rescanLimit}
          </p>
        )}
      </div>
    </>
  )
}

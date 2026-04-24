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
import type { ArchitectureProfileResponse, ArchitectureScanResponse, ScanDetailResponse } from "@/lib/types"

function formatDate(iso: string) {
  return new Date(iso).toLocaleDateString("en-US", {
    month: "short",
    day: "numeric",
    year: "numeric",
  })
}

const TRIGGER_LABELS: Record<string, string> = {
  GITHUB_SCAN:  "GitHub scan",
  PASTE_SCAN:   "Paste upload",
  RESCAN:       "Rescan",
}

const ARCH_STATUS_LABELS: Record<string, { text: string; color: string }> = {
  VERIFIED:          { text: "Architecture Verified",  color: "text-verified" },
  UNDER_REVIEW:      { text: "Under Review",           color: "text-declared" },
  EVIDENCE_MISMATCH: { text: "Evidence Mismatch",      color: "text-mismatch"   },
  FRESHNESS_LOW:     { text: "Freshness Low",          color: "text-muted" },
}

const ARCH_CONFIDENCE_LABELS: Record<string, string> = {
  HIGH:   "High",
  MEDIUM: "Medium",
  LOW:    "Low",
}

const EVIDENCE_SOURCE_LABELS: Record<string, string> = {
  GITHUB_PUBLIC:        "Public GitHub",
  CONFIDENTIAL_GITHUB:  "Confidential GitHub",
  CONFIDENTIAL_SESSION: "Confidential Session",
  MIXED_EVIDENCE:       "Mixed Evidence",
  PASTE:                "Paste Upload",
}

function ArchScanHistory({ scans }: { scans: ArchitectureScanResponse[] }) {
  const [expandedId, setExpandedId] = useState<string | null>(null)
  const [details, setDetails] = useState<Record<string, ScanDetailResponse>>({})
  const [loading, setLoading] = useState<string | null>(null)

  const toggle = async (scanId: string) => {
    if (expandedId === scanId) { setExpandedId(null); return }
    setExpandedId(scanId)
    if (!details[scanId]) {
      setLoading(scanId)
      try {
        const d = await architectureApi.getScanDetail(scanId)
        setDetails((prev) => ({ ...prev, [scanId]: d }))
      } catch { /* silently skip */ }
      finally { setLoading(null) }
    }
  }

  return (
    <div className="mt-6 card-base p-6">
      <h2 className="mb-4 text-[16px] font-semibold text-ink">Architecture scan history</h2>
      <div className="space-y-2">
        {scans.map((s) => {
          const statusMeta = ARCH_STATUS_LABELS[s.archStatus ?? ""]
          const isDone = s.status === "SUCCEEDED"
          const isFailed = s.status === "FAILED"
          const isExpanded = expandedId === s.id
          const detail = details[s.id]

          return (
            <div key={s.id} className="rounded-[16px] border border-hairline-strong bg-surface-1 overflow-hidden">
              <button
                onClick={() => isDone && toggle(s.id)}
                className={[
                  "w-full flex items-center justify-between px-4 py-3.5 text-left transition-colors",
                  isDone ? "hover:bg-surface-inset cursor-pointer" : "cursor-default",
                ].join(" ")}
              >
                <div className="flex items-center gap-3">
                  <span className={[
                    "h-2.5 w-2.5 shrink-0 rounded-full border border-surface-1",
                    isDone ? "bg-verified" : isFailed ? "bg-mismatch" : "bg-declared",
                  ].join(" ")} />
                  <div className="text-left">
                    <p className="text-[13px] font-medium text-ink">
                      {EVIDENCE_SOURCE_LABELS[s.evidenceSource] ?? s.evidenceSource?.replace(/_/g, " ") ?? "Scan"}
                    </p>
                    {isDone && s.confidence && (
                      <p className={["text-[11px] font-medium", statusMeta?.color ?? "text-muted"].join(" ")}>
                        {ARCH_CONFIDENCE_LABELS[s.confidence] ?? s.confidence} confidence
                        {s.archStatus ? ` · ${statusMeta?.text ?? s.archStatus.replace(/_/g, " ")}` : ""}
                      </p>
                    )}
                    {isFailed && <p className="text-[11px] text-mismatch">Failed</p>}
                    {!isDone && !isFailed && <p className="text-[11px] text-muted">Processing…</p>}
                  </div>
                </div>
                <div className="flex items-center gap-3">
                  {isDone && s.llmScore != null && (
                    <span className="rounded-full bg-surface-inset border border-hairline-strong px-2.5 py-0.5 text-[12px] font-semibold tabular-nums text-ink">
                      {s.llmScore}/100
                    </span>
                  )}
                  <span className="text-[12px] text-muted font-mono">{formatDate(s.createdAt)}</span>
                  {isDone && (
                    <span className="text-[11px] text-muted">{isExpanded ? "▲" : "▼"}</span>
                  )}
                </div>
              </button>

              {isExpanded && (
                <div className="border-t border-hairline-strong px-4 pb-4 pt-3">
                  {loading === s.id && (
                    <div className="space-y-2">
                      {[1, 2].map((i) => (
                        <div key={i} className="h-10 animate-pulse rounded-xl bg-surface-inset" />
                      ))}
                    </div>
                  )}
                  {detail && (
                    <>
                      {s.llmScore != null && (
                        <div className="mb-4 rounded-xl border border-hairline-strong bg-surface-inset px-3 py-2.5">
                          <div className="flex items-center gap-2 mb-1">
                            <span className="text-[10px] font-semibold uppercase tracking-widest text-muted">AI Assessment</span>
                            <span className="text-[13px] font-bold text-ink">{s.llmScore}/100</span>
                          </div>
                          {s.llmReasoning && (
                            <p className="text-[12px] text-muted leading-relaxed">{s.llmReasoning}</p>
                          )}
                        </div>
                      )}
                      <p className="mb-2 text-[10px] font-semibold uppercase tracking-widest text-muted">
                        Repos scanned ({detail.repos.length})
                      </p>
                      <div className="space-y-2">
                        {detail.repos.map((repo) => (
                          <div key={repo.repoFullName} className="rounded-xl border border-hairline-strong bg-surface-inset px-3 py-2.5">
                            <p className="text-[13px] font-medium text-ink">{repo.repoFullName}</p>
                            <div className="mt-1 flex flex-wrap gap-x-4 gap-y-0.5 text-[11px] text-muted">
                              <span>{repo.commits30d} commits (30d)</span>
                              <span>{repo.commits90d} commits (90d)</span>
                              <span>{repo.contributorCount} contributor{repo.contributorCount !== 1 ? "s" : ""}</span>
                              <span>depth {repo.maxFolderDepth}</span>
                              <span>{repo.serviceCount} service{repo.serviceCount !== 1 ? "s" : ""}</span>
                              <span>{repo.sourceFileCount} source files</span>
                              <span>{repo.repoAgeMonths}mo old</span>
                            </div>
                          </div>
                        ))}
                        {detail.repos.length === 0 && (
                          <p className="text-[12px] text-muted">No repos recorded for this scan.</p>
                        )}
                      </div>
                    </>
                  )}
                </div>
              )}
            </div>
          )
        })}
      </div>
    </div>
  )
}

export default function DashboardPage() {
  const router = useRouter()
  const { isAuthenticated, user, isLoading: authLoading } = useAuth()
  const { agency, dashboard, notFound, isLoading, refresh } = useAgency()
  const [rescanError, setRescanError] = useState<string | null>(null)
  const [isRescanning, setIsRescanning] = useState(false)
  const [archProfile, setArchProfile] = useState<ArchitectureProfileResponse | null>(null)
  const [archScans, setArchScans] = useState<ArchitectureScanResponse[]>([])

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
    architectureApi
      .listScans()
      .then(setArchScans)
      .catch(() => setArchScans([]))
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
            <div key={i} className="h-24 animate-pulse rounded-[22px] bg-surface-inset" />
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
            <h1 className="text-4xl text-ink font-display tracking-tight">
              {agency?.name ?? "Your Dashboard"}
            </h1>
            {agency?.niche && (
              <p className="mt-1 text-[15px] font-medium text-muted">{agency.niche}</p>
            )}
          </div>
          <div className="flex flex-wrap gap-2">
            <Button asChild size="sm" variant="outline" className="rounded-full border-hairline-strong hover:border-trust-border text-[13px]">
              <Link href="/dashboard/verify-architecture">
                Architecture session
              </Link>
            </Button>
            <Button asChild size="sm" variant="outline" className="rounded-full border-hairline-strong hover:border-trust-border text-[13px]">
              <Link href="/dashboard/verify">
                {dashboard?.latestScore ? "Re-verify" : "Get verified"}
              </Link>
            </Button>
            {agency && !agency.isPublic && dashboard?.latestScore && (
              <Button
                size="sm"
                className="rounded-full bg-trust text-bg hover:opacity-80 transition-opacity text-[13px] border border-trust-border"
                onClick={handlePublish}
              >
                Publish proof
              </Button>
            )}
            {agency?.isPublic && agency.publicSlug && (
              <Button asChild size="sm" variant="outline" className="rounded-full border-hairline-strong hover:border-trust-border text-[13px]">
                <Link href={`/agents/${agency.publicSlug}`} target="_blank">
                  View public profile ↗
                </Link>
              </Button>
            )}
          </div>
        </div>

        {/* Latest score card */}
        {dashboard?.latestScore ? (
          <div className="mb-6 card-base p-6">
            <div className="flex flex-wrap items-center justify-between gap-4">
              <div>
                <p className="text-[10px] font-mono font-semibold uppercase tracking-widest text-muted">
                  Latest score
                </p>
                {dashboard.latestScore.tier && dashboard.latestScore.totalScore != null ? (
                  <div className="mt-2 flex items-center gap-3">
                    <span className="text-[40px] font-bold tabular-nums text-ink font-display">
                      {dashboard.latestScore.totalScore}
                    </span>
                    <ScoreBadge tier={dashboard.latestScore.tier} />
                  </div>
                ) : (
                  <p className="mt-2 text-[13px] text-muted">Scan in progress…</p>
                )}
              </div>
              <div className="flex flex-col items-end gap-2">
                <TrustLabel label={dashboard.latestScore.verificationLabel} />
                <p className="text-xs text-muted font-medium">
                  {formatDate(dashboard.latestScore.createdAt)}
                </p>
                {dashboard.latestScore.rulesetVersion && (
                  <p className="text-xs text-muted font-mono">
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
              <div className="mt-4 rounded-xl border border-mismatch-bg bg-mismatch-bg/30 px-3 py-2 text-[13px] text-mismatch">
                {rescanError}
              </div>
            )}

            <div className="mt-5 flex gap-2 pt-4 border-t border-hairline-strong">
              <Button
                size="sm"
                variant="outline"
                className="rounded-full text-[13px] border-hairline-strong hover:border-trust-border"
                disabled={isRescanning}
                onClick={handleRescan}
              >
                {isRescanning ? "Starting rescan…" : "Re-run scan"}
              </Button>
              <Button asChild size="sm" variant="ghost" className="rounded-full text-[13px] text-muted hover:text-ink">
                <Link href={`/dashboard/scan/${dashboard.latestScore.scanId}`}>
                  View full report
                </Link>
              </Button>
            </div>
          </div>
        ) : (
          !notFound && (
            <div className="mb-6 rounded-[22px] border-2 border-dashed border-hairline-strong bg-surface-inset p-8 text-center flex flex-col items-center">
              <p className="mb-4 text-muted text-[15px]">
                You haven&apos;t run a verification scan yet.
              </p>
              <Button asChild className="rounded-full bg-trust text-bg hover:opacity-80 px-6">
                <Link href="/dashboard/verify">Get verified</Link>
              </Button>
            </div>
          )
        )}

        {/* ── Anti-gaming alert ─────────────────────────────────────────── */}
        {archProfile?.hasOpenReviewCase && (
          <div className="mb-6 flex items-start gap-3 rounded-[18px] border border-declared bg-declared-bg p-5 shadow-sm">
            <span className="mt-0.5 text-lg text-declared">⚑</span>
            <div>
              <p className="text-[14px] font-semibold text-declared">Architecture review in progress</p>
              <p className="mt-1 text-[13px] text-declared/80 leading-relaxed">
                A recent architecture change has been flagged for review. Your score remains visible
                but the Architecture Verified badge is paused until the review resolves. This is
                a routine integrity check — no action required from you.
              </p>
            </div>
          </div>
        )}

        {/* ── Architecture monitoring ───────────────────────────────────── */}
        {archProfile ? (
          <div className="mb-6 card-base p-6">
            <div className="mb-4 flex items-center justify-between">
              <h2 className="text-[16px] font-semibold text-ink">Architecture monitoring</h2>
              <Link
                href="/dashboard/verify-architecture"
                className="badge hover:border-trust-border hover:text-ink transition-colors"
              >
                New session →
              </Link>
            </div>
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
              <div className="rounded-[16px] border border-hairline-strong bg-surface-1 p-4 text-center ss-grid-bg-sm relative overflow-hidden">
                <div className="relative z-10">
                  <p className="text-[10px] font-semibold uppercase tracking-widest text-muted font-mono bg-surface-1 inline-block px-2">Confidence</p>
                  <p className={[
                    "mt-2 text-[15px] font-bold",
                    archProfile.confidence === "HIGH" ? "text-verified"
                    : archProfile.confidence === "MEDIUM" ? "text-declared"
                    : "text-muted",
                  ].join(" ")}>
                    {archProfile.confidence ? ARCH_CONFIDENCE_LABELS[archProfile.confidence] ?? archProfile.confidence : "—"}
                  </p>
                </div>
              </div>
              <div className="rounded-[16px] border border-hairline-strong bg-surface-1 p-4 text-center ss-grid-bg-sm relative overflow-hidden">
                <div className="relative z-10">
                  <p className="text-[10px] font-semibold uppercase tracking-widest text-muted font-mono bg-surface-1 inline-block px-2">Status</p>
                  <p className={[
                    "mt-2 text-[14px] font-bold",
                    ARCH_STATUS_LABELS[archProfile.archStatus ?? ""]?.color ?? "text-declared",
                  ].join(" ")}>
                    {archProfile.archStatus ? (ARCH_STATUS_LABELS[archProfile.archStatus]?.text ?? archProfile.archStatus.replace(/_/g, " ")) : "—"}
                  </p>
                </div>
              </div>
              <div className="rounded-[16px] border border-hairline-strong bg-surface-1 p-4 text-center ss-grid-bg-sm relative overflow-hidden">
                <div className="relative z-10">
                  <p className="text-[10px] font-semibold uppercase tracking-widest text-muted font-mono bg-surface-1 inline-block px-2">Source</p>
                  <p className="mt-2 text-[13px] font-bold text-ink">
                    {archProfile.evidenceSource
                      ? (EVIDENCE_SOURCE_LABELS[archProfile.evidenceSource] ?? archProfile.evidenceSource.replace(/_/g, " "))
                      : "—"}
                  </p>
                </div>
              </div>
            </div>
            {archProfile.lastVerifiedAt && (
              <p className="mt-4 text-[12px] text-muted text-center">
                Last verified {formatDate(archProfile.lastVerifiedAt)} · Automated rescans keep this fresh
              </p>
            )}
          </div>
        ) : (
          !notFound && (
            <div className="mb-6 card-base border-dashed bg-surface-inset p-6 bg-none border-2">
              <div className="flex flex-wrap items-center justify-between gap-4">
                <div>
                  <p className="text-[15px] font-semibold text-ink">Unlock Architecture Verified</p>
                  <p className="mt-1 text-[13px] text-muted">
                    Connect your systems for an architecture-level trust signal that goes beyond
                    manifest scanning.
                  </p>
                </div>
                <Button asChild size="sm" className="rounded-full bg-trust text-bg hover:opacity-80 transition-opacity">
                  <Link href="/dashboard/verify-architecture">Start session →</Link>
                </Button>
              </div>
            </div>
          )
        )}

        {/* Scan history */}
        {dashboard && dashboard.scans.length > 0 && (
          <div className="card-base p-6">
            <h2 className="mb-4 text-[16px] font-semibold text-ink">Verification timeline</h2>
            <div className="space-y-3">
              {dashboard.scans.map((s) => (
                <Link
                  key={s.scanId}
                  href={`/dashboard/scan/${s.scanId}`}
                  className="flex items-center justify-between rounded-[16px] border border-hairline-strong bg-surface-1 px-4 py-3.5 transition-colors hover:border-trust-border group"
                >
                  <div className="flex items-center gap-3">
                    <span
                      className={[
                        "h-2.5 w-2.5 rounded-full border border-surface-1",
                        s.status === "SUCCEEDED"
                          ? "bg-verified"
                          : s.status === "FAILED"
                          ? "bg-mismatch"
                          : "bg-declared",
                      ].join(" ")}
                    />
                    <span className="text-[14px] font-medium text-ink group-hover:underline decoration-hairline-strong underline-offset-4">
                      {TRIGGER_LABELS[s.triggerType] ?? s.triggerType.replace(/_/g, " ").toLowerCase()}
                    </span>
                    <TrustLabel label={s.verificationLabel} className="text-[9px] px-1.5 py-0.5 rounded-sm" />
                  </div>
                  <span className="text-[12px] text-muted font-mono">{formatDate(s.createdAt)}</span>
                </Link>
              ))}
            </div>
          </div>
        )}

        {/* Architecture scan history */}
        {archScans.length > 0 && (
          <ArchScanHistory scans={archScans} />
        )}

        {/* Rescan counter */}
        {agency && (
          <p className="mt-6 text-center text-[12px] text-muted font-mono">
            Rescans used: {agency.rescanCount} / {agency.rescanLimit}
          </p>
        )}
      </div>
    </>
  )
}

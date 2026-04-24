"use client"

import { use, useEffect, useRef, useState } from "react"
import Link from "next/link"
import { useRouter } from "next/navigation"
import { useAuth } from "@/lib/hooks/use-auth"
import { useScanPoller } from "@/lib/hooks/use-scan-poller"
import { agencyApi } from "@/lib/api/agency"
import { ScoreBadge } from "@/components/ui/score-badge"
import { TrustLabel } from "@/components/ui/trust-label"
import { CategoryBar } from "@/components/ui/category-bar"
import { StackChip } from "@/components/ui/stack-chip"
import { Button } from "@/components/ui/button"
import { toCategorySubtotals, type CategorySubtotal } from "@/lib/types"

// ── Terminal animation ─────────────────────────────────────────────────────

const SCAN_LINES = [
  "Cloning repositories…",
  "Parsing package manifests…",
  "Resolving dependency graph…",
  "Matching against SyncScore ruleset v1…",
  "Evaluating Orchestration layer…",
  "Evaluating RAG & Retrieval layer…",
  "Evaluating Memory & State layer…",
  "Evaluating Guardrails layer…",
  "Evaluating Observability layer…",
  "Evaluating Base SDK layer…",
  "Computing category scores…",
  "Applying score caps…",
  "Determining tier…",
  "Finalizing report…",
]

interface TerminalPackage { packageName: string; category: string; pointsAwarded: number }

function useTerminalLines(isRunning: boolean, detectedPackages: TerminalPackage[]) {
  const [lines, setLines] = useState<string[]>(["$ syncscore scan --mode=auto"])
  const indexRef = useRef(0)

  useEffect(() => {
    if (!isRunning) return
    indexRef.current = 0
    setLines(["$ syncscore scan --mode=auto"])
  }, [isRunning])

  useEffect(() => {
    if (!isRunning) return

    const interval = setInterval(() => {
      const i = indexRef.current
      if (i < SCAN_LINES.length) {
        setLines((prev) => [...prev, `  ${SCAN_LINES[i]}`])
        indexRef.current++
      } else if (detectedPackages.length > 0) {
        const pkgIdx = i - SCAN_LINES.length
        if (pkgIdx < detectedPackages.length) {
          const pkg = detectedPackages[pkgIdx]
          setLines((prev) => [
            ...prev,
            `  ✓ ${pkg.packageName} [${pkg.category}] +${pkg.pointsAwarded}pts`,
          ])
          indexRef.current++
        }
      }
    }, 220)

    return () => clearInterval(interval)
  }, [isRunning, detectedPackages])

  return lines
}

function deriveInsights(categories: CategorySubtotal[]) {
  const strengths = categories.filter((c) => c.cap > 0 && c.points / c.cap >= 0.8)
  const considerations = categories.filter((c) => c.cap > 0 && c.points === 0)
  return { strengths, considerations }
}

// ── Page ──────────────────────────────────────────────────────────────────

interface Props {
  params: Promise<{ scanId: string }>
}

export default function ScanResultPage({ params }: Props) {
  const { scanId } = use(params)
  const router = useRouter()
  const { isAuthenticated, isLoading: authLoading } = useAuth()
  const { scan, isLoading, isTerminal, error } = useScanPoller(scanId)
  const [published, setPublished] = useState(false)
  const [publishing, setPublishing] = useState(false)
  const terminalRef = useRef<HTMLDivElement>(null)

  const isRunning = !isTerminal
  const packages = scan?.detectedPackages ?? []
  const terminalLines = useTerminalLines(isRunning, packages)

  useEffect(() => {
    if (!authLoading && !isAuthenticated) {
      router.push("/auth/login")
    }
  }, [authLoading, isAuthenticated, router])

  useEffect(() => {
    if (terminalRef.current) {
      terminalRef.current.scrollTop = terminalRef.current.scrollHeight
    }
  }, [terminalLines])

  const handlePublish = async () => {
    setPublishing(true)
    try {
      const agency = await agencyApi.me()
      await agencyApi.upsert({ name: agency.name, isPublic: true })
      setPublished(true)
    } finally {
      setPublishing(false)
    }
  }

  if (authLoading || (isLoading && !scan)) {
    return (
      <div className="mx-auto max-w-4xl px-6 py-12">
        <div className="h-64 animate-pulse rounded-[23px] bg-surface-inset" />
      </div>
    )
  }

  if (error) {
    return (
      <div className="mx-auto max-w-4xl px-6 py-12 text-center">
        <p className="mb-4 text-red-500">{error}</p>
        <Button asChild variant="outline">
          <Link href="/dashboard">Back to dashboard</Link>
        </Button>
      </div>
    )
  }

  // ── Terminal view (QUEUED / RUNNING / still polling) ──────────────────
  if (!isTerminal) {
    return (
      <div className="mx-auto max-w-4xl px-6 py-12">
        <div className="mb-4 flex items-center gap-3">
          <div className="flex gap-1">
            <span className="h-3 w-3 rounded-full bg-red-400" />
            <span className="h-3 w-3 rounded-full bg-amber-400" />
            <span className="h-3 w-3 rounded-full bg-verified" />
          </div>
          <span className="text-xs text-muted">SyncScore Scanner</span>
          <div className="ml-auto flex items-center gap-2">
            <span className="h-2 w-2 animate-pulse rounded-full bg-amber-400" />
            <span className="text-xs text-muted">
              {scan?.status === "QUEUED" ? "Queued" : "Running"}
            </span>
          </div>
        </div>
        <div
          ref={terminalRef}
          className="h-80 overflow-y-auto rounded-[23px] border-2 border-trust-border bg-trust p-6 font-mono text-sm text-bg"
        >
          {terminalLines.map((line, i) => (
            <div key={i} className={line.includes("✓") ? "text-verified" : ""}>
              {line}
            </div>
          ))}
          <div className="mt-1 inline-block h-4 w-2 animate-pulse bg-bg" />
        </div>
        <p className="mt-4 text-center text-sm text-muted">
          Scanning your stack… this usually takes under 30 seconds
        </p>
      </div>
    )
  }

  // ── Failed view ────────────────────────────────────────────────────────
  if (scan?.status === "FAILED") {
    return (
      <div className="mx-auto max-w-4xl px-6 py-12 text-center">
        <div className="mx-auto max-w-sm rounded-[23px] border-2 border-red-200 bg-red-50 p-8">
          <p className="mb-2 text-lg font-semibold text-red-600">Scan failed</p>
          <p className="mb-6 text-sm text-red-500">
            {scan.errorMessage ?? "An unexpected error occurred during the scan."}
          </p>
          <div className="flex justify-center gap-3">
            <Button asChild variant="outline">
              <Link href="/dashboard/verify">Try again</Link>
            </Button>
            <Button asChild variant="ghost">
              <Link href="/dashboard">Dashboard</Link>
            </Button>
          </div>
        </div>
      </div>
    )
  }

  // ── Success view ───────────────────────────────────────────────────────
  const score = scan?.score
  const categories = toCategorySubtotals(score?.categorySubtotals)
  const { strengths, considerations } = deriveInsights(categories)

  return (
    <div className="mx-auto max-w-4xl px-6 py-12">
      {/* Header */}
      <div className="mb-6 rounded-[23px] border-2 border-hairline-strong bg-surface-inset p-8">
        <div className="flex flex-wrap items-start justify-between gap-4">
          <div>
            <p className="text-xs font-semibold uppercase tracking-widest text-muted">
              Verification complete
            </p>
            {score && (
              <div className="mt-2 flex items-center gap-3">
                <span className="text-5xl font-bold tabular-nums text-ink">
                  {score.totalScore}
                </span>
                <ScoreBadge tier={score.tier} />
              </div>
            )}
          </div>
          <div className="flex flex-col items-end gap-2">
            <TrustLabel label={scan?.verificationLabel ?? "UNVERIFIED"} />
            {scan?.rulesetVersion && (
              <p className="text-xs text-muted">{scan.rulesetVersion}</p>
            )}
          </div>
        </div>

        <div className="mt-6 flex flex-wrap gap-3">
          {!published ? (
            <Button
              className="rounded-full bg-trust text-bg hover:bg-trust/80"
              onClick={handlePublish}
              disabled={publishing}
            >
              {publishing ? "Publishing…" : "Publish proof"}
            </Button>
          ) : (
            <div className="flex items-center gap-2 rounded-full border-2 border-[#2ECC71] bg-verified-bg px-4 py-1.5">
              <span className="h-1.5 w-1.5 rounded-full bg-verified" />
              <span className="text-sm font-medium text-verified">Profile published</span>
            </div>
          )}
          <Button asChild variant="outline">
            <Link href="/dashboard">Back to dashboard</Link>
          </Button>
        </div>
      </div>

      <div className="grid gap-6 lg:grid-cols-2">
        {/* Score breakdown */}
        {categories.length > 0 && (
          <div className="rounded-[23px] border-2 border-hairline-strong bg-surface-inset p-6">
            <h2 className="mb-5 text-lg font-semibold text-ink">Score breakdown</h2>
            <div className="space-y-4">
              {categories.map((c) => (
                <CategoryBar key={c.category} label={c.category} points={c.points} cap={c.cap} />
              ))}
            </div>
            <div className="mt-6 flex items-center justify-between border-t-2 border-hairline-strong pt-4">
              <span className="text-sm font-medium text-muted">Total score</span>
              <span className="text-2xl font-bold tabular-nums text-ink">
                {score?.totalScore ?? 0}
              </span>
            </div>
          </div>
        )}

        {/* Strengths & Considerations */}
        <div className="space-y-4">
          {strengths.length > 0 && (
            <div className="rounded-[23px] border-2 border-[#2ECC71] bg-verified-bg p-6">
              <h2 className="mb-3 text-base font-semibold text-verified">Strengths</h2>
              <ul className="space-y-1.5">
                {strengths.map((s) => (
                  <li key={s.category} className="flex items-center gap-2 text-sm text-ink">
                    <span className="h-1.5 w-1.5 rounded-full bg-verified" />
                    {s.category} ({s.points}/{s.cap} pts)
                  </li>
                ))}
              </ul>
            </div>
          )}
          {considerations.length > 0 && (
            <div className="rounded-[23px] border-2 border-hairline-strong bg-surface-inset p-6">
              <h2 className="mb-3 text-base font-semibold text-muted">Considerations</h2>
              <ul className="space-y-1.5">
                {considerations.map((c) => (
                  <li key={c.category} className="flex items-center gap-2 text-sm text-muted">
                    <span className="h-1.5 w-1.5 rounded-full bg-hairline-strong" />
                    No {c.category} layer detected
                  </li>
                ))}
              </ul>
            </div>
          )}
        </div>
      </div>

      {/* Detected stack */}
      {packages.length > 0 && (
        <div className="mt-6 rounded-[23px] border-2 border-hairline-strong bg-surface-inset p-6">
          <h2 className="mb-4 text-lg font-semibold text-ink">Detected stack</h2>
          <div className="flex flex-wrap gap-2">
            {packages.map((pkg) => (
              <StackChip
                key={pkg.packageName}
                packageName={pkg.packageName}
                category={pkg.category}
                pointsAwarded={pkg.pointsAwarded}
              />
            ))}
          </div>
        </div>
      )}
    </div>
  )
}

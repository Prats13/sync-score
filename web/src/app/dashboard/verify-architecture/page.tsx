"use client"

import { useState, useEffect, useRef } from "react"
import { useRouter } from "next/navigation"
import Link from "next/link"
import { useAuth } from "@/lib/hooks/use-auth"
import { useAgency } from "@/lib/hooks/use-agency"
import { architectureApi } from "@/lib/api/architecture"
import { Button } from "@/components/ui/button"
import { CompleteProfileModal } from "@/components/ui/complete-profile-modal"
import type { ArchitectureScanResponse, ScanDetailResponse } from "@/lib/types"

const STEPS = [
  "Source",
  "Exclusions",
  "Preview",
  "Extracting",
  "Review",
  "Result",
]

const SOURCES = [
  {
    id: "github",
    icon: "⬡",
    title: "Private GitHub / GitLab / Bitbucket",
    desc: "Connect a private repo for the strongest structural evidence.",
    recommended: true,
  },
  {
    id: "agent",
    icon: "⚙",
    title: "Internal architecture agent",
    desc: "Point SyncScore at an internal agent endpoint that describes your system.",
    recommended: false,
  },
  {
    id: "docs",
    icon: "📄",
    title: "Internal docs bundle",
    desc: "Upload architecture docs, ADRs, or README exports.",
    recommended: false,
  },
  {
    id: "export",
    icon: "📦",
    title: "Structured architecture export",
    desc: "Provide a JSON/YAML manifest of your services, tools, and integrations.",
    recommended: false,
  },
  {
    id: "interview",
    icon: "💬",
    title: "Guided architecture interview",
    desc: "Answer structured questions. Lowest evidence strength but zero exposure.",
    recommended: false,
  },
]

const EXCLUSION_DEFAULTS = [
  { id: "secrets", label: "Secrets & credentials", checked: true },
  { id: "pii", label: "Customer records & PII", checked: true },
  { id: "prompts", label: "Prompt text", checked: true },
  { id: "biz_rules", label: "Proprietary business rules", checked: true },
  { id: "raw_content", label: "Retain raw content after extraction", checked: true },
]

const EXTRACTION_LINES = [
  "Connecting to source…",
  "Reading repository metadata…",
  "Scanning folder structure…",
  "Resolving service inventory…",
  "Detecting orchestration patterns…",
  "Checking retrieval & memory signals…",
  "Evaluating guardrail evidence…",
  "Analysing observability signals…",
  "Cross-referencing manifest claims…",
  "Normalising architecture evidence…",
  "Applying exclusion policies…",
  "Finalising verification record…",
]

const EVIDENCE_SOURCE_LABELS: Record<string, string> = {
  GITHUB_PUBLIC:        "Public GitHub",
  CONFIDENTIAL_GITHUB:  "Confidential GitHub",
  CONFIDENTIAL_SESSION: "Confidential Session",
  MIXED_EVIDENCE:       "Mixed Evidence",
  PASTE:                "Paste Upload",
}

function StepIndicator({ current }: { current: number }) {
  return (
    <div className="flex items-center gap-0">
      {STEPS.map((label, i) => {
        const idx = i + 1
        const done = idx < current
        const active = idx === current
        return (
          <div key={label} className="flex items-center">
            <div className="flex flex-col items-center gap-1">
              <div
                className={[
                  "flex h-7 w-7 items-center justify-center rounded-full text-xs font-semibold transition-all",
                  done
                    ? "bg-verified text-bg"
                    : active
                      ? "bg-trust text-bg"
                      : "border-2 border-hairline-strong bg-surface-1 text-muted",
                ].join(" ")}
              >
                {done ? "✓" : idx}
              </div>
              <span className={`hidden text-[10px] sm:block ${active ? "font-semibold text-ink" : "text-muted"}`}>
                {label}
              </span>
            </div>
            {i < STEPS.length - 1 && (
              <div className={`mb-4 h-px w-8 sm:w-12 ${done ? "bg-verified" : "bg-hairline-strong"}`} />
            )}
          </div>
        )
      })}
    </div>
  )
}

export default function VerifyArchitecturePage() {
  const router = useRouter()
  const { isAuthenticated, isLoading: authLoading } = useAuth()
  const { notFound, isLoading: agencyLoading, refresh } = useAgency()
  const [step, setStep] = useState(1)
  const [source, setSource] = useState<string | null>(null)
  const [exclusions, setExclusions] = useState(EXCLUSION_DEFAULTS)
  const [customExclusions, setCustomExclusions] = useState("")
  const [extractionLine, setExtractionLine] = useState(0)
  const [scanResult, setScanResult] = useState<ArchitectureScanResponse | null>(null)
  const [scanDetail, setScanDetail] = useState<ScanDetailResponse | null>(null)
  const [error, setError] = useState<string | null>(null)
  const extractionRef = useRef<ReturnType<typeof setInterval> | null>(null)

  useEffect(() => {
    if (!authLoading && !isAuthenticated) router.push("/auth/login")
  }, [authLoading, isAuthenticated, router])

  useEffect(() => {
    if (step === 4) startExtraction()
    return () => { if (extractionRef.current) clearInterval(extractionRef.current) }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [step])

  const pollUntilDone = async (scanId: string): Promise<ArchitectureScanResponse> => {
    const POLL_INTERVAL_MS = 2000
    const MAX_WAIT_MS = 60_000
    const deadline = Date.now() + MAX_WAIT_MS
    while (Date.now() < deadline) {
      await new Promise((r) => setTimeout(r, POLL_INTERVAL_MS))
      const scan = await architectureApi.getScan(scanId)
      if (scan.status === "SUCCEEDED" || scan.status === "FAILED") return scan
    }
    throw new Error("Scan timed out — please try again.")
  }

  const startExtraction = async () => {
    setExtractionLine(0)
    extractionRef.current = setInterval(() => {
      setExtractionLine((l) => {
        if (l >= EXTRACTION_LINES.length - 1) {
          clearInterval(extractionRef.current!)
          return l
        }
        return l + 1
      })
    }, 600)

    try {
      const checkedExclusions = exclusions.filter((e) => e.checked).map((e) => e.id)
      const triggered = await architectureApi.triggerScan(source ?? undefined, checkedExclusions, customExclusions)

      const animationMs = EXTRACTION_LINES.length * 600 + 400
      const [finalResult] = await Promise.all([
        pollUntilDone(triggered.id),
        new Promise((r) => setTimeout(r, animationMs)),
      ])

      const detail = await architectureApi.getScanDetail(triggered.id)
      setScanResult(finalResult)
      setScanDetail(detail)
      setStep(5)
    } catch (e) {
      setError(e instanceof Error ? e.message : "Scan failed. Please try again.")
      if (extractionRef.current) clearInterval(extractionRef.current)
    }
  }

  const toggleExclusion = (id: string) => {
    setExclusions((prev) =>
      prev.map((e) => (e.id === id ? { ...e, checked: !e.checked } : e))
    )
  }

  if (authLoading || agencyLoading) {
    return (
      <div className="mx-auto max-w-2xl px-6 py-12">
        <div className="h-64 animate-pulse rounded-[23px] bg-surface-inset" />
      </div>
    )
  }

  return (
    <>
      <CompleteProfileModal
        open={notFound}
        onComplete={() => refresh()}
      />

      <div className="mx-auto max-w-2xl px-6 py-12">
        {/* Header */}
        <div className="mb-8 text-center">
          <p className="mb-2 text-xs font-semibold uppercase tracking-widest text-muted">
            Architecture Verification
          </p>
          <h1
            className="text-3xl text-ink"
            style={{ fontFamily: "var(--font-display)" }}
          >
            Confidential Architecture Session
          </h1>
          <p className="mt-2 text-sm text-muted">
            Connect your systems, set exclusions, and receive an architecture-level trust signal
            without exposing confidential implementation details.
          </p>
        </div>

        {/* Stepper */}
        <div className="mb-8 flex justify-center">
          <StepIndicator current={step} />
        </div>

        {/* Error banner */}
        {error && (
          <div className="mb-6 rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
            {error}
            <button className="ml-3 font-medium underline" onClick={() => { setError(null); setStep(3) }}>
              Go back
            </button>
          </div>
        )}

        {/* ── Step 1: Source Selection ── */}
        {step === 1 && (
          <div className="rounded-[23px] border-2 border-hairline-strong bg-surface-inset p-8">
            <h2 className="mb-1 text-xl font-semibold text-ink">Choose a source</h2>
            <p className="mb-6 text-sm text-muted">
              SyncScore collects more data with less effort through connectors — choose the source
              that best fits your setup.
            </p>
            <div className="space-y-3">
              {SOURCES.map((s) => (
                <button
                  key={s.id}
                  onClick={() => setSource(s.id)}
                  className={[
                    "w-full rounded-xl border-2 p-4 text-left transition-all",
                    source === s.id
                      ? "border-trust-border bg-surface-1"
                      : "border-hairline-strong bg-surface-1 hover:border-trust-border/30",
                  ].join(" ")}
                >
                  <div className="flex items-start justify-between gap-3">
                    <div className="flex items-start gap-3">
                      <span className="mt-0.5 text-xl">{s.icon}</span>
                      <div>
                        <p className="text-sm font-medium text-ink">{s.title}</p>
                        <p className="mt-0.5 text-xs text-muted">{s.desc}</p>
                      </div>
                    </div>
                    <div className="flex shrink-0 flex-col items-end gap-1">
                      {s.recommended && (
                        <span className="rounded-full bg-verified-bg px-2 py-0.5 text-[10px] font-semibold text-verified">
                          Recommended
                        </span>
                      )}
                      <div
                        className={[
                          "h-4 w-4 rounded-full border-2 transition-all",
                          source === s.id
                            ? "border-trust-border bg-trust"
                            : "border-hairline-strong",
                        ].join(" ")}
                      />
                    </div>
                  </div>
                </button>
              ))}
            </div>
            <div className="mt-6 flex justify-end">
              <Button
                disabled={!source}
                onClick={() => setStep(2)}
                className="rounded-full bg-trust text-bg hover:bg-trust/80"
              >
                Continue →
              </Button>
            </div>
          </div>
        )}

        {/* ── Step 2: Exclusions ── */}
        {step === 2 && (
          <div className="rounded-[23px] border-2 border-hairline-strong bg-surface-inset p-8">
            <h2 className="mb-1 text-xl font-semibold text-ink">Set exclusions</h2>
            <p className="mb-6 text-sm text-muted">
              Choose what SyncScore must not access or retain. Defaults are pre-selected — most users
              accept them without changes.
            </p>
            <div className="space-y-3">
              {exclusions.map((ex) => (
                <label key={ex.id} className="flex cursor-pointer items-center gap-3 rounded-xl border-2 border-hairline-strong bg-surface-1 px-4 py-3 transition-colors hover:border-trust-border/30">
                  <input
                    type="checkbox"
                    checked={ex.checked}
                    onChange={() => toggleExclusion(ex.id)}
                    className="h-4 w-4 rounded accent-trust"
                  />
                  <span className="text-sm text-ink">Exclude {ex.label}</span>
                </label>
              ))}
            </div>
            <div className="mt-4">
              <label className="mb-1 block text-xs font-medium text-muted">
                Custom paths, services, or domains to exclude (optional)
              </label>
              <textarea
                value={customExclusions}
                onChange={(e) => setCustomExclusions(e.target.value)}
                placeholder="e.g. /internal/billing, payment-service, *.pem"
                rows={3}
                className="w-full rounded-xl border-2 border-hairline-strong bg-surface-1 px-3 py-2 text-sm text-ink outline-none focus:border-trust-border"
              />
            </div>
            <div className="mt-6 flex justify-between">
              <Button variant="outline" onClick={() => setStep(1)}>← Back</Button>
              <Button onClick={() => setStep(3)} className="rounded-full bg-trust text-bg hover:bg-trust/80">
                Continue →
              </Button>
            </div>
          </div>
        )}

        {/* ── Step 3: Access Preview ── */}
        {step === 3 && (
          <div className="rounded-[23px] border-2 border-hairline-strong bg-surface-inset p-8">
            <h2 className="mb-1 text-xl font-semibold text-ink">Access preview</h2>
            <p className="mb-6 text-sm text-muted">
              Here is exactly what SyncScore will do during this session. Review before granting
              access.
            </p>
            <div className="space-y-2">
              {[
                { allow: true, text: "Read repository metadata and folder structure" },
                { allow: true, text: "Summarise service inventory and detected integrations" },
                { allow: true, text: "Extract capability signals from approved content categories" },
                { allow: true, text: "Retain only normalised verification outputs — not raw content" },
                { allow: false, text: "Access paths or data classes you excluded" },
                { allow: false, text: "Store raw source files, credentials, or prompt content" },
                { allow: false, text: "Share evidence with third parties" },
              ].map(({ allow, text }, i) => (
                <div key={i} className="flex items-center gap-3 rounded-xl border-2 border-hairline-strong bg-surface-1 px-4 py-3">
                  <span className={allow ? "text-verified" : "text-red-400"}>
                    {allow ? "✓" : "✗"}
                  </span>
                  <span className="text-sm text-ink">{text}</span>
                </div>
              ))}
            </div>
            <div className="mt-5 rounded-xl border-2 border-hairline-strong bg-surface-1 p-4">
              <p className="text-xs font-semibold uppercase tracking-widest text-muted">
                Session summary
              </p>
              <div className="mt-2 flex flex-wrap gap-x-6 gap-y-1 text-sm">
                <span className="text-muted">Source: <strong className="text-ink">{SOURCES.find((s) => s.id === source)?.title}</strong></span>
                <span className="text-muted">Exclusions: <strong className="text-ink">{exclusions.filter((e) => e.checked).length} active</strong></span>
              </div>
            </div>
            <div className="mt-6 flex justify-between">
              <Button variant="outline" onClick={() => setStep(2)}>← Back</Button>
              <Button onClick={() => setStep(4)} className="rounded-full bg-trust text-bg hover:bg-trust/80">
                Start session →
              </Button>
            </div>
          </div>
        )}

        {/* ── Step 4: Extraction ── */}
        {step === 4 && (
          <div className="rounded-[23px] border-2 border-hairline-strong bg-trust p-8">
            <div className="mb-4 flex items-center gap-3">
              <span className="relative flex h-2.5 w-2.5">
                <span className="absolute inline-flex h-full w-full animate-ping rounded-full bg-verified opacity-75" />
                <span className="relative inline-flex h-2.5 w-2.5 rounded-full bg-verified" />
              </span>
              <p className="text-xs font-semibold uppercase tracking-widest text-muted">
                Extracting architecture evidence
              </p>
            </div>
            <div
              className="min-h-[220px] rounded-xl bg-surface-inset p-5 font-mono text-sm"
              style={{ fontFamily: "JetBrains Mono, monospace" }}
            >
              {EXTRACTION_LINES.slice(0, extractionLine + 1).map((line, i) => (
                <div key={i} className={i === extractionLine ? "text-verified" : "text-muted"}>
                  <span className="text-muted/60 mr-2 select-none">&gt;</span>
                  {line}
                  {i === extractionLine && (
                    <span className="ml-1 animate-pulse">▋</span>
                  )}
                </div>
              ))}
            </div>
            {error && (
              <div className="mt-4 rounded-lg border border-red-400/30 bg-red-900/20 px-4 py-3 text-sm text-red-300">
                {error}
              </div>
            )}
          </div>
        )}

        {/* ── Step 5: Evidence Review ── */}
        {step === 5 && scanResult && scanDetail && (
          <div className="rounded-[23px] border-2 border-hairline-strong bg-surface-inset p-8">
            <h2 className="mb-1 text-xl font-semibold text-ink">Review evidence</h2>
            <p className="mb-6 text-sm text-muted">
              SyncScore scanned {scanDetail.repos.length} repo{scanDetail.repos.length !== 1 ? "s" : ""} and
              produced {scanDetail.signals.length} structural signals. Review before finalising.
            </p>

            {/* Repos scanned */}
            <p className="mb-2 text-xs font-semibold uppercase tracking-widest text-muted">Repos scanned</p>
            <div className="mb-5 space-y-2">
              {scanDetail.repos.map((repo) => (
                <div key={repo.repoFullName} className="rounded-xl border-2 border-hairline-strong bg-surface-1 p-4">
                  <div className="flex items-start justify-between gap-4">
                    <p className="text-sm font-semibold text-ink">{repo.repoFullName}</p>
                    <span className="shrink-0 rounded-full bg-verified-bg px-2.5 py-0.5 text-xs font-medium text-verified">
                      ● Scanned
                    </span>
                  </div>
                  <div className="mt-2 flex flex-wrap gap-x-5 gap-y-1 text-xs text-muted">
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
              {scanDetail.repos.length === 0 && (
                <div className="rounded-xl border-2 border-hairline-strong bg-surface-1 px-4 py-3 text-sm text-muted">
                  No repos found for this GitHub account.
                </div>
              )}
            </div>

            {/* Signals */}
            <p className="mb-2 text-xs font-semibold uppercase tracking-widest text-muted">Scoring signals</p>
            <div className="mb-5 rounded-xl border-2 border-hairline-strong bg-surface-1 divide-y divide-hairline-strong">
              {scanDetail.signals.map((sig) => {
                const pct = sig.confidenceContribution != null ? Math.round(sig.confidenceContribution) : null
                return (
                  <div key={sig.signalType} className="flex items-center justify-between px-4 py-3">
                    <div>
                      <p className="text-xs font-medium text-ink">
                        {sig.signalType.replace(/_/g, " ").toLowerCase().replace(/^\w/, (c) => c.toUpperCase())}
                      </p>
                      {sig.valueLabel && <p className="text-xs text-muted">{sig.valueLabel}</p>}
                    </div>
                    {pct != null && (
                      <div className="flex items-center gap-2">
                        <div className="h-1.5 w-16 overflow-hidden rounded-full bg-hairline-strong">
                          <div
                            className="h-full rounded-full bg-verified"
                            style={{ width: `${Math.min(100, Math.max(0, pct))}%` }}
                          />
                        </div>
                        <span className="w-8 text-right text-xs text-muted">{pct}%</span>
                      </div>
                    )}
                  </div>
                )
              })}
            </div>

            {/* Exclusions */}
            <div className="mb-5 rounded-xl border-2 border-hairline-strong bg-surface-1 p-4">
              <div className="flex items-center justify-between">
                <p className="text-sm font-semibold text-ink">Redacted / excluded</p>
                <span className="rounded-full bg-surface-inset px-2.5 py-0.5 text-xs font-medium text-muted">● Excluded</span>
              </div>
              <p className="mt-1 text-xs text-muted">{customExclusions || "Secrets, PII, prompt content, raw source"}</p>
            </div>

            <div className="rounded-xl border-2 border-hairline-strong bg-surface-1 p-4 text-sm">
              <p className="font-semibold text-ink">Public visibility</p>
              <p className="mt-1 text-muted">
                Your public profile will show architecture confidence, verification status, and a
                capability summary. Raw source evidence and excluded content remain private.
              </p>
            </div>

            <div className="mt-6 flex justify-between">
              <Button variant="outline" onClick={() => setStep(3)}>← Back</Button>
              <Button onClick={() => setStep(6)} className="rounded-full bg-trust text-bg hover:bg-trust/80">
                Approve & finalise →
              </Button>
            </div>
          </div>
        )}

        {/* ── Step 6: Result ── */}
        {step === 6 && (
          <div className="rounded-[23px] border-2 border-[#2ECC71] bg-verified-bg p-8 text-center">
            <div className="mx-auto mb-5 flex h-16 w-16 items-center justify-center rounded-full bg-verified text-3xl text-bg">
              ✓
            </div>
            <h2 className="mb-2 text-2xl font-semibold text-ink" style={{ fontFamily: "var(--font-display)" }}>
              Architecture Verified by SyncScore
            </h2>
            <p className="mb-6 text-sm text-muted">
              Your architecture evidence has been processed and your trust signal is live.
            </p>

            {scanResult?.llmScore != null && (
              <div className="mb-6 rounded-2xl border-2 border-[#2ECC71]/40 bg-surface-1 px-6 py-5 text-left">
                <div className="flex items-center gap-4">
                  <div className="flex-1">
                    <p className="text-xs font-semibold uppercase tracking-widest text-muted mb-1">SyncScore AI Assessment</p>
                    <p className="text-[13px] text-muted leading-relaxed">{scanResult.llmReasoning ?? ""}</p>
                  </div>
                  <div className="shrink-0 text-right">
                    <p className="text-4xl font-bold text-ink tabular-nums">{scanResult.llmScore}</p>
                    <p className="text-xs text-muted">/100</p>
                  </div>
                </div>
              </div>
            )}

            {scanResult && (
              <div className="mb-6 grid grid-cols-3 gap-4 text-left">
                <div className="rounded-xl border-2 border-hairline-strong bg-surface-1 p-4">
                  <p className="text-xs font-semibold uppercase tracking-widest text-muted">Status</p>
                  <p className="mt-1 text-sm font-semibold text-ink">
                    {(scanResult.archStatus ?? "PROCESSING").replace("_", " ")}
                  </p>
                </div>
                <div className="rounded-xl border-2 border-hairline-strong bg-surface-1 p-4">
                  <p className="text-xs font-semibold uppercase tracking-widest text-muted">Confidence</p>
                  <p className="mt-1 text-sm font-semibold text-ink">
                    {scanResult.confidence ?? "Pending"}
                  </p>
                </div>
                <div className="rounded-xl border-2 border-hairline-strong bg-surface-1 p-4">
                  <p className="text-xs font-semibold uppercase tracking-widest text-muted">Source</p>
                  <p className="mt-1 text-sm font-semibold text-ink">
                    {scanResult.evidenceSource
                      ? (EVIDENCE_SOURCE_LABELS[scanResult.evidenceSource] ?? scanResult.evidenceSource.replace(/_/g, " "))
                      : "Mixed"}
                  </p>
                </div>
              </div>
            )}

            <div className="flex justify-center gap-3">
              <Button asChild variant="outline">
                <Link href="/dashboard">Go to dashboard</Link>
              </Button>
              <Button asChild className="rounded-full bg-trust text-bg hover:bg-trust/80">
                <Link href="/dashboard">View public profile ↗</Link>
              </Button>
            </div>
          </div>
        )}
      </div>
    </>
  )
}

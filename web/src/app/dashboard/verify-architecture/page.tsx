"use client"

import { useState, useEffect, useRef } from "react"
import { useRouter } from "next/navigation"
import Link from "next/link"
import { useAuth } from "@/lib/hooks/use-auth"
import { architectureApi } from "@/lib/api/architecture"
import { Button } from "@/components/ui/button"
import type { ArchitectureScanResponse } from "@/lib/types"

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
                    ? "bg-[#2ECC71] text-white"
                    : active
                    ? "bg-[#10100F] text-white"
                    : "border-2 border-[#D7D3CB] bg-white text-[#6B6B6B]",
                ].join(" ")}
              >
                {done ? "✓" : idx}
              </div>
              <span className={`hidden text-[10px] sm:block ${active ? "font-semibold text-[#000000]" : "text-[#6B6B6B]"}`}>
                {label}
              </span>
            </div>
            {i < STEPS.length - 1 && (
              <div className={`mb-4 h-px w-8 sm:w-12 ${done ? "bg-[#2ECC71]" : "bg-[#D7D3CB]"}`} />
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
  const [step, setStep] = useState(1)
  const [source, setSource] = useState<string | null>(null)
  const [exclusions, setExclusions] = useState(EXCLUSION_DEFAULTS)
  const [customExclusions, setCustomExclusions] = useState("")
  const [extractionLine, setExtractionLine] = useState(0)
  const [scanResult, setScanResult] = useState<ArchitectureScanResponse | null>(null)
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
      const result = await architectureApi.triggerScan()
      setScanResult(result)
      setTimeout(() => setStep(5), EXTRACTION_LINES.length * 600 + 400)
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

  if (authLoading) {
    return (
      <div className="mx-auto max-w-2xl px-6 py-12">
        <div className="h-64 animate-pulse rounded-[23px] bg-[#F6F6F3]" />
      </div>
    )
  }

  return (
    <div className="mx-auto max-w-2xl px-6 py-12">
      {/* Header */}
      <div className="mb-8 text-center">
        <p className="mb-2 text-xs font-semibold uppercase tracking-widest text-[#6B6B6B]">
          Architecture Verification
        </p>
        <h1
          className="text-3xl text-[#000000]"
          style={{ fontFamily: "var(--font-dm-serif-display)" }}
        >
          Confidential Architecture Session
        </h1>
        <p className="mt-2 text-sm text-[#6B6B6B]">
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
        <div className="rounded-[23px] border-2 border-[#D7D3CB] bg-[#F6F6F3] p-8">
          <h2 className="mb-1 text-xl font-semibold text-[#000000]">Choose a source</h2>
          <p className="mb-6 text-sm text-[#6B6B6B]">
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
                    ? "border-[#10100F] bg-white"
                    : "border-[#D7D3CB] bg-white hover:border-[#10100F]/30",
                ].join(" ")}
              >
                <div className="flex items-start justify-between gap-3">
                  <div className="flex items-start gap-3">
                    <span className="mt-0.5 text-xl">{s.icon}</span>
                    <div>
                      <p className="text-sm font-medium text-[#000000]">{s.title}</p>
                      <p className="mt-0.5 text-xs text-[#6B6B6B]">{s.desc}</p>
                    </div>
                  </div>
                  <div className="flex shrink-0 flex-col items-end gap-1">
                    {s.recommended && (
                      <span className="rounded-full bg-[#EBFFF2] px-2 py-0.5 text-[10px] font-semibold text-[#279455]">
                        Recommended
                      </span>
                    )}
                    <div
                      className={[
                        "h-4 w-4 rounded-full border-2 transition-all",
                        source === s.id
                          ? "border-[#10100F] bg-[#10100F]"
                          : "border-[#D7D3CB]",
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
              className="rounded-full bg-[#10100F] text-white hover:bg-[#10100F]/80"
            >
              Continue →
            </Button>
          </div>
        </div>
      )}

      {/* ── Step 2: Exclusions ── */}
      {step === 2 && (
        <div className="rounded-[23px] border-2 border-[#D7D3CB] bg-[#F6F6F3] p-8">
          <h2 className="mb-1 text-xl font-semibold text-[#000000]">Set exclusions</h2>
          <p className="mb-6 text-sm text-[#6B6B6B]">
            Choose what SyncScore must not access or retain. Defaults are pre-selected — most users
            accept them without changes.
          </p>
          <div className="space-y-3">
            {exclusions.map((ex) => (
              <label key={ex.id} className="flex cursor-pointer items-center gap-3 rounded-xl border-2 border-[#D7D3CB] bg-white px-4 py-3 transition-colors hover:border-[#10100F]/30">
                <input
                  type="checkbox"
                  checked={ex.checked}
                  onChange={() => toggleExclusion(ex.id)}
                  className="h-4 w-4 rounded accent-[#10100F]"
                />
                <span className="text-sm text-[#000000]">Exclude {ex.label}</span>
              </label>
            ))}
          </div>
          <div className="mt-4">
            <label className="mb-1 block text-xs font-medium text-[#6B6B6B]">
              Custom paths, services, or domains to exclude (optional)
            </label>
            <textarea
              value={customExclusions}
              onChange={(e) => setCustomExclusions(e.target.value)}
              placeholder="e.g. /internal/billing, payment-service, *.pem"
              rows={3}
              className="w-full rounded-xl border-2 border-[#D7D3CB] bg-white px-3 py-2 text-sm text-[#000000] outline-none focus:border-[#10100F]"
            />
          </div>
          <div className="mt-6 flex justify-between">
            <Button variant="outline" onClick={() => setStep(1)}>← Back</Button>
            <Button onClick={() => setStep(3)} className="rounded-full bg-[#10100F] text-white hover:bg-[#10100F]/80">
              Continue →
            </Button>
          </div>
        </div>
      )}

      {/* ── Step 3: Access Preview ── */}
      {step === 3 && (
        <div className="rounded-[23px] border-2 border-[#D7D3CB] bg-[#F6F6F3] p-8">
          <h2 className="mb-1 text-xl font-semibold text-[#000000]">Access preview</h2>
          <p className="mb-6 text-sm text-[#6B6B6B]">
            Here is exactly what SyncScore will do during this session. Review before granting
            access.
          </p>
          <div className="space-y-2">
            {[
              { allow: true,  text: "Read repository metadata and folder structure" },
              { allow: true,  text: "Summarise service inventory and detected integrations" },
              { allow: true,  text: "Extract capability signals from approved content categories" },
              { allow: true,  text: "Retain only normalised verification outputs — not raw content" },
              { allow: false, text: "Access paths or data classes you excluded" },
              { allow: false, text: "Store raw source files, credentials, or prompt content" },
              { allow: false, text: "Share evidence with third parties" },
            ].map(({ allow, text }, i) => (
              <div key={i} className="flex items-center gap-3 rounded-xl border-2 border-[#D7D3CB] bg-white px-4 py-3">
                <span className={allow ? "text-[#2ECC71]" : "text-red-400"}>
                  {allow ? "✓" : "✗"}
                </span>
                <span className="text-sm text-[#000000]">{text}</span>
              </div>
            ))}
          </div>
          <div className="mt-5 rounded-xl border-2 border-[#D7D3CB] bg-white p-4">
            <p className="text-xs font-semibold uppercase tracking-widest text-[#6B6B6B]">
              Session summary
            </p>
            <div className="mt-2 flex flex-wrap gap-x-6 gap-y-1 text-sm">
              <span className="text-[#6B6B6B]">Source: <strong className="text-[#000000]">{SOURCES.find((s) => s.id === source)?.title}</strong></span>
              <span className="text-[#6B6B6B]">Exclusions: <strong className="text-[#000000]">{exclusions.filter((e) => e.checked).length} active</strong></span>
            </div>
          </div>
          <div className="mt-6 flex justify-between">
            <Button variant="outline" onClick={() => setStep(2)}>← Back</Button>
            <Button onClick={() => setStep(4)} className="rounded-full bg-[#10100F] text-white hover:bg-[#10100F]/80">
              Start session →
            </Button>
          </div>
        </div>
      )}

      {/* ── Step 4: Extraction ── */}
      {step === 4 && (
        <div className="rounded-[23px] border-2 border-[#D7D3CB] bg-[#10100F] p-8">
          <div className="mb-4 flex items-center gap-3">
            <span className="relative flex h-2.5 w-2.5">
              <span className="absolute inline-flex h-full w-full animate-ping rounded-full bg-[#2ECC71] opacity-75" />
              <span className="relative inline-flex h-2.5 w-2.5 rounded-full bg-[#2ECC71]" />
            </span>
            <p className="text-xs font-semibold uppercase tracking-widest text-[#6B6B6B]">
              Extracting architecture evidence
            </p>
          </div>
          <div
            className="min-h-[220px] rounded-xl bg-black/30 p-5 font-mono text-sm"
            style={{ fontFamily: "JetBrains Mono, monospace" }}
          >
            {EXTRACTION_LINES.slice(0, extractionLine + 1).map((line, i) => (
              <div key={i} className={i === extractionLine ? "text-[#2ECC71]" : "text-[#6B6B6B]"}>
                <span className="text-[#6B6B6B]/60 mr-2 select-none">&gt;</span>
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
      {step === 5 && scanResult && (
        <div className="rounded-[23px] border-2 border-[#D7D3CB] bg-[#F6F6F3] p-8">
          <h2 className="mb-1 text-xl font-semibold text-[#000000]">Review evidence</h2>
          <p className="mb-6 text-sm text-[#6B6B6B]">
            SyncScore has extracted and graded architecture evidence. Review what will be visible
            on your public profile before finalising.
          </p>

          <div className="space-y-3">
            {[
              { label: "Verified evidence", desc: "Confirmed through source cross-checks", grade: "VERIFIED", items: ["Service inventory detected", "Dependency manifest analysed", "Folder structure reviewed"] },
              { label: "Declared evidence", desc: "Provided by you — not independently confirmed", grade: "DECLARED", items: ["Architecture descriptions from docs", "Integration claims from export"] },
              { label: "Inferred evidence", desc: "Derived from surrounding patterns", grade: "INFERRED", items: ["Likely orchestration layer from manifest context"] },
              { label: "Redacted / excluded", desc: "Removed per your exclusion policy", grade: null, items: [customExclusions || "Secrets, PII, prompt content, raw source"] },
            ].map(({ label, desc, grade, items }) => (
              <div key={label} className="rounded-xl border-2 border-[#D7D3CB] bg-white p-4">
                <div className="flex items-center justify-between">
                  <p className="text-sm font-semibold text-[#000000]">{label}</p>
                  {grade === "VERIFIED" && <span className="rounded-full bg-[#EBFFF2] px-2.5 py-0.5 text-xs font-medium text-[#279455]">● Verified</span>}
                  {grade === "DECLARED" && <span className="rounded-full bg-[#FFF8E1] px-2.5 py-0.5 text-xs font-medium text-[#B45309]">● Declared</span>}
                  {grade === "INFERRED" && <span className="rounded-full bg-[#EEF2FF] px-2.5 py-0.5 text-xs font-medium text-[#4338CA]">● Inferred</span>}
                  {!grade && <span className="rounded-full bg-[#F6F6F3] px-2.5 py-0.5 text-xs font-medium text-[#6B6B6B]">● Excluded</span>}
                </div>
                <p className="mt-0.5 text-xs text-[#6B6B6B]">{desc}</p>
                <ul className="mt-2 space-y-1">
                  {items.map((item) => (
                    <li key={item} className="flex items-center gap-2 text-xs text-[#6B6B6B]">
                      <span className="h-1 w-1 shrink-0 rounded-full bg-[#D7D3CB]" />
                      {item}
                    </li>
                  ))}
                </ul>
              </div>
            ))}
          </div>

          <div className="mt-5 rounded-xl border-2 border-[#D7D3CB] bg-white p-4 text-sm">
            <p className="font-semibold text-[#000000]">Public visibility</p>
            <p className="mt-1 text-[#6B6B6B]">
              Your public profile will show architecture confidence, verification status, and a
              capability summary. Raw source evidence and excluded content remain private.
            </p>
          </div>

          <div className="mt-6 flex justify-between">
            <Button variant="outline" onClick={() => setStep(3)}>← Back</Button>
            <Button onClick={() => setStep(6)} className="rounded-full bg-[#10100F] text-white hover:bg-[#10100F]/80">
              Approve & finalise →
            </Button>
          </div>
        </div>
      )}

      {/* ── Step 6: Result ── */}
      {step === 6 && (
        <div className="rounded-[23px] border-2 border-[#2ECC71] bg-[#EBFFF2] p-8 text-center">
          <div className="mx-auto mb-5 flex h-16 w-16 items-center justify-center rounded-full bg-[#2ECC71] text-3xl text-white">
            ✓
          </div>
          <h2 className="mb-2 text-2xl font-semibold text-[#000000]" style={{ fontFamily: "var(--font-dm-serif-display)" }}>
            Architecture Verified by SyncScore
          </h2>
          <p className="mb-6 text-sm text-[#6B6B6B]">
            Your architecture evidence has been processed and your trust signal is live.
          </p>

          {scanResult && (
            <div className="mb-6 grid grid-cols-3 gap-4 text-left">
              <div className="rounded-xl border-2 border-[#D7D3CB] bg-white p-4">
                <p className="text-xs font-semibold uppercase tracking-widest text-[#6B6B6B]">Status</p>
                <p className="mt-1 text-sm font-semibold text-[#000000]">
                  {(scanResult.archStatus ?? "PROCESSING").replace("_", " ")}
                </p>
              </div>
              <div className="rounded-xl border-2 border-[#D7D3CB] bg-white p-4">
                <p className="text-xs font-semibold uppercase tracking-widest text-[#6B6B6B]">Confidence</p>
                <p className="mt-1 text-sm font-semibold text-[#000000]">
                  {scanResult.confidence ?? "Pending"}
                </p>
              </div>
              <div className="rounded-xl border-2 border-[#D7D3CB] bg-white p-4">
                <p className="text-xs font-semibold uppercase tracking-widest text-[#6B6B6B]">Source</p>
                <p className="mt-1 text-sm font-semibold text-[#000000]">
                  {scanResult.evidenceSource || "Mixed"}
                </p>
              </div>
            </div>
          )}

          <div className="flex justify-center gap-3">
            <Button asChild variant="outline">
              <Link href="/dashboard">Go to dashboard</Link>
            </Button>
            <Button asChild className="rounded-full bg-[#10100F] text-white hover:bg-[#10100F]/80">
              <Link href="/dashboard">View public profile ↗</Link>
            </Button>
          </div>
        </div>
      )}
    </div>
  )
}

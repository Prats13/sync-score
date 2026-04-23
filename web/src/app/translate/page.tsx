"use client"

import { useState } from "react"
import Link from "next/link"
import { publicApi } from "@/lib/api/public"
import { Button } from "@/components/ui/button"
import { ScoreBadge } from "@/components/ui/score-badge"
import type { BrowseAgency, SyncTier } from "@/lib/types"

const STEPS = ["Problem", "Industry", "Capabilities", "Tier", "Budget"]

const INDUSTRIES = [
  "Fintech", "Legal", "Healthcare", "E-commerce", "SaaS", "Logistics",
  "Real Estate", "Education", "Media", "Government", "Other",
]

const CAPABILITY_OPTIONS = [
  { id: "rag", label: "Retrieval & Knowledge Base", category: "RAG & Retrieval" },
  { id: "orchestration", label: "Multi-step Workflows", category: "Orchestration" },
  { id: "memory", label: "Conversation Memory", category: "Memory & State" },
  { id: "guardrails", label: "Safety & Content Controls", category: "Guardrails" },
  { id: "observability", label: "Monitoring & Auditing", category: "Observability" },
]

const TIER_OPTIONS: { value: SyncTier | "ANY"; label: string; desc: string }[] = [
  { value: "ANY", label: "Any tier", desc: "Show all verified agencies regardless of maturity." },
  { value: "EXPERT", label: "Expert", desc: "Production-grade architecture with full stack evidence." },
  { value: "BUILDER", label: "Builder", desc: "Solid working systems — a good fit for most projects." },
  { value: "WRAPPER", label: "Wrapper", desc: "Lighter integrations; great for simpler use cases." },
]

const BUDGET_OPTIONS = [
  { id: "early", label: "< $5k / month", score: 0 },
  { id: "mid", label: "$5k – $25k / month", score: 1 },
  { id: "scale", label: "$25k+ / month", score: 2 },
  { id: "undecided", label: "Not decided yet", score: 0 },
]

interface IntakeState {
  problem: string
  industry: string
  capabilities: string[]
  tier: SyncTier | "ANY"
  budget: string
}

interface RankedAgency extends BrowseAgency {
  fitScore: number
  fitReasons: string[]
}

function computeFit(agency: BrowseAgency, intake: IntakeState): RankedAgency {
  let fitScore = 0
  const fitReasons: string[] = []

  if (agency.totalScore) {
    fitScore += Math.min(agency.totalScore / 2, 30)
  }

  if (intake.tier !== "ANY" && agency.tier === intake.tier) {
    fitScore += 25
    fitReasons.push(`Matches your preferred tier (${agency.tier})`)
  } else if (intake.tier === "ANY") {
    fitScore += 15
  }

  if (agency.verificationLabel === "GITHUB_VERIFIED") {
    fitScore += 15
    fitReasons.push("GitHub verified")
  } else if (agency.verificationLabel === "SELF_REPORTED") {
    fitScore += 5
  }

  if (intake.industry && agency.niche) {
    const nicheLC = agency.niche.toLowerCase()
    const industryLC = intake.industry.toLowerCase()
    if (nicheLC.includes(industryLC) || industryLC.includes(nicheLC)) {
      fitScore += 20
      fitReasons.push(`Specialises in ${intake.industry}`)
    }
  }

  if (fitReasons.length === 0 && fitScore > 30) {
    fitReasons.push("Strong overall verification score")
  }

  return { ...agency, fitScore: Math.min(Math.round(fitScore), 100), fitReasons }
}

function StepIndicator({ current, total }: { current: number; total: number }) {
  return (
    <div className="flex items-center gap-0">
      {STEPS.map((label, i) => {
        const idx = i + 1
        const done = idx < current
        const active = idx === current
        return (
          <div key={label} className="flex items-center">
            <div className="flex flex-col items-center gap-1">
              <div className={[
                "flex h-6 w-6 items-center justify-center rounded-full text-xs font-semibold",
                done ? "bg-[#2ECC71] text-white"
                : active ? "bg-[#10100F] text-white"
                : "border-2 border-[#D7D3CB] bg-white text-[#6B6B6B]",
              ].join(" ")}>
                {done ? "✓" : idx}
              </div>
              <span className={`hidden text-[10px] sm:block ${active ? "font-semibold text-[#000000]" : "text-[#6B6B6B]"}`}>
                {label}
              </span>
            </div>
            {i < total - 1 && (
              <div className={`mb-4 h-px w-8 ${done ? "bg-[#2ECC71]" : "bg-[#D7D3CB]"}`} />
            )}
          </div>
        )
      })}
    </div>
  )
}

export default function TranslatePage() {
  const [step, setStep] = useState(1)
  const [intake, setIntake] = useState<IntakeState>({
    problem: "",
    industry: "",
    capabilities: [],
    tier: "ANY",
    budget: "",
  })
  const [results, setResults] = useState<RankedAgency[]>([])
  const [isLoading, setIsLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const toggleCap = (id: string) => {
    setIntake((prev) => ({
      ...prev,
      capabilities: prev.capabilities.includes(id)
        ? prev.capabilities.filter((c) => c !== id)
        : [...prev.capabilities, id],
    }))
  }

  const runMatch = async () => {
    setIsLoading(true)
    setError(null)
    try {
      const agencies = await publicApi.browse(intake.tier !== "ANY" ? intake.tier : undefined)
      const ranked = agencies
        .map((a) => computeFit(a, intake))
        .sort((a, b) => b.fitScore - a.fitScore)
      setResults(ranked)
      setStep(6)
    } catch (e) {
      setError(e instanceof Error ? e.message : "Failed to fetch agencies")
    } finally {
      setIsLoading(false)
    }
  }

  return (
    <div className="mx-auto max-w-2xl px-6 py-12">
      <div className="mb-8 text-center">
        <p className="mb-2 text-xs font-semibold uppercase tracking-widest text-[#6B6B6B]">
          Smarter Translator
        </p>
        <h1 className="text-3xl text-[#000000]" style={{ fontFamily: "var(--font-dm-serif-display)" }}>
          Find the right AI agency
        </h1>
        <p className="mt-2 text-sm text-[#6B6B6B]">
          Tell us about your project and we&apos;ll rank verified agencies by fit — not just score.
        </p>
      </div>

      {step < 6 && (
        <div className="mb-8 flex justify-center">
          <StepIndicator current={step} total={STEPS.length} />
        </div>
      )}

      {error && (
        <div className="mb-4 rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-600">
          {error}
        </div>
      )}

      {/* Step 1: Problem */}
      {step === 1 && (
        <div className="rounded-[23px] border-2 border-[#D7D3CB] bg-[#F6F6F3] p-8">
          <h2 className="mb-1 text-xl font-semibold text-[#000000]">What are you trying to solve?</h2>
          <p className="mb-5 text-sm text-[#6B6B6B]">
            Describe the business problem or workflow you want to automate with AI.
          </p>
          <textarea
            value={intake.problem}
            onChange={(e) => setIntake({ ...intake, problem: e.target.value })}
            placeholder="e.g. We need an AI agent to handle customer support tickets and escalate complex cases to humans…"
            rows={5}
            className="w-full rounded-xl border-2 border-[#D7D3CB] bg-white px-4 py-3 text-sm text-[#000000] outline-none focus:border-[#10100F]"
          />
          <div className="mt-5 flex justify-end">
            <Button
              disabled={intake.problem.trim().length < 10}
              onClick={() => setStep(2)}
              className="rounded-full bg-[#10100F] text-white hover:bg-[#10100F]/80"
            >
              Continue →
            </Button>
          </div>
        </div>
      )}

      {/* Step 2: Industry */}
      {step === 2 && (
        <div className="rounded-[23px] border-2 border-[#D7D3CB] bg-[#F6F6F3] p-8">
          <h2 className="mb-1 text-xl font-semibold text-[#000000]">What&apos;s your industry?</h2>
          <p className="mb-5 text-sm text-[#6B6B6B]">
            We&apos;ll prioritise agencies with relevant domain experience.
          </p>
          <div className="flex flex-wrap gap-2">
            {INDUSTRIES.map((ind) => (
              <button
                key={ind}
                onClick={() => setIntake({ ...intake, industry: ind })}
                className={[
                  "rounded-full border-2 px-4 py-1.5 text-sm font-medium transition-all",
                  intake.industry === ind
                    ? "border-[#10100F] bg-[#10100F] text-white"
                    : "border-[#D7D3CB] bg-white text-[#000000] hover:border-[#10100F]/40",
                ].join(" ")}
              >
                {ind}
              </button>
            ))}
          </div>
          <div className="mt-6 flex justify-between">
            <Button variant="outline" onClick={() => setStep(1)}>← Back</Button>
            <Button
              disabled={!intake.industry}
              onClick={() => setStep(3)}
              className="rounded-full bg-[#10100F] text-white hover:bg-[#10100F]/80"
            >
              Continue →
            </Button>
          </div>
        </div>
      )}

      {/* Step 3: Capabilities */}
      {step === 3 && (
        <div className="rounded-[23px] border-2 border-[#D7D3CB] bg-[#F6F6F3] p-8">
          <h2 className="mb-1 text-xl font-semibold text-[#000000]">What capabilities matter most?</h2>
          <p className="mb-5 text-sm text-[#6B6B6B]">Select all that apply — we&apos;ll weight verified evidence in these areas.</p>
          <div className="space-y-2">
            {CAPABILITY_OPTIONS.map((cap) => (
              <button
                key={cap.id}
                onClick={() => toggleCap(cap.id)}
                className={[
                  "w-full rounded-xl border-2 px-4 py-3 text-left text-sm transition-all",
                  intake.capabilities.includes(cap.id)
                    ? "border-[#10100F] bg-white font-medium text-[#000000]"
                    : "border-[#D7D3CB] bg-white text-[#6B6B6B] hover:border-[#10100F]/30",
                ].join(" ")}
              >
                <div className="flex items-center justify-between">
                  <span>{cap.label}</span>
                  {intake.capabilities.includes(cap.id) && <span className="text-[#2ECC71]">✓</span>}
                </div>
              </button>
            ))}
          </div>
          <div className="mt-6 flex justify-between">
            <Button variant="outline" onClick={() => setStep(2)}>← Back</Button>
            <Button
              onClick={() => setStep(4)}
              className="rounded-full bg-[#10100F] text-white hover:bg-[#10100F]/80"
            >
              Continue →
            </Button>
          </div>
        </div>
      )}

      {/* Step 4: Tier */}
      {step === 4 && (
        <div className="rounded-[23px] border-2 border-[#D7D3CB] bg-[#F6F6F3] p-8">
          <h2 className="mb-1 text-xl font-semibold text-[#000000]">What maturity level do you need?</h2>
          <p className="mb-5 text-sm text-[#6B6B6B]">SyncScore tiers reflect verified architecture depth.</p>
          <div className="space-y-3">
            {TIER_OPTIONS.map((t) => (
              <button
                key={t.value}
                onClick={() => setIntake({ ...intake, tier: t.value as SyncTier | "ANY" })}
                className={[
                  "w-full rounded-xl border-2 p-4 text-left transition-all",
                  intake.tier === t.value
                    ? "border-[#10100F] bg-white"
                    : "border-[#D7D3CB] bg-white hover:border-[#10100F]/30",
                ].join(" ")}
              >
                <div className="flex items-center justify-between">
                  <div>
                    <p className="text-sm font-semibold text-[#000000]">{t.label}</p>
                    <p className="mt-0.5 text-xs text-[#6B6B6B]">{t.desc}</p>
                  </div>
                  {intake.tier === t.value && <span className="text-[#2ECC71]">✓</span>}
                </div>
              </button>
            ))}
          </div>
          <div className="mt-6 flex justify-between">
            <Button variant="outline" onClick={() => setStep(3)}>← Back</Button>
            <Button
              onClick={() => setStep(5)}
              className="rounded-full bg-[#10100F] text-white hover:bg-[#10100F]/80"
            >
              Continue →
            </Button>
          </div>
        </div>
      )}

      {/* Step 5: Budget */}
      {step === 5 && (
        <div className="rounded-[23px] border-2 border-[#D7D3CB] bg-[#F6F6F3] p-8">
          <h2 className="mb-1 text-xl font-semibold text-[#000000]">What&apos;s your monthly budget?</h2>
          <p className="mb-5 text-sm text-[#6B6B6B]">This helps surface agencies that are the right fit without over-engineering.</p>
          <div className="space-y-3">
            {BUDGET_OPTIONS.map((b) => (
              <button
                key={b.id}
                onClick={() => setIntake({ ...intake, budget: b.id })}
                className={[
                  "w-full rounded-xl border-2 px-4 py-3 text-left text-sm transition-all",
                  intake.budget === b.id
                    ? "border-[#10100F] bg-white font-medium text-[#000000]"
                    : "border-[#D7D3CB] bg-white text-[#6B6B6B] hover:border-[#10100F]/30",
                ].join(" ")}
              >
                <div className="flex items-center justify-between">
                  <span>{b.label}</span>
                  {intake.budget === b.id && <span className="text-[#2ECC71]">✓</span>}
                </div>
              </button>
            ))}
          </div>
          <div className="mt-6 flex justify-between">
            <Button variant="outline" onClick={() => setStep(4)}>← Back</Button>
            <Button
              disabled={!intake.budget || isLoading}
              onClick={runMatch}
              className="rounded-full bg-[#10100F] text-white hover:bg-[#10100F]/80"
            >
              {isLoading ? "Finding matches…" : "Find matches →"}
            </Button>
          </div>
        </div>
      )}

      {/* Step 6: Results */}
      {step === 6 && (
        <div>
          <div className="mb-6 flex items-center justify-between">
            <div>
              <h2 className="text-xl font-semibold text-[#000000]">
                {results.length} match{results.length !== 1 ? "es" : ""} found
              </h2>
              <p className="mt-0.5 text-sm text-[#6B6B6B]">
                Ranked by fit with your project — not just overall score.
              </p>
            </div>
            <Button variant="outline" size="sm" onClick={() => setStep(1)}>
              Start over
            </Button>
          </div>

          {results.length === 0 ? (
            <div className="rounded-[23px] border-2 border-[#D7D3CB] bg-[#F6F6F3] p-8 text-center text-[#6B6B6B]">
              No verified agencies match your current criteria. Try broadening your tier or capability filters.
            </div>
          ) : (
            <div className="space-y-4">
              {results.map((agency, i) => (
                <Link
                  key={agency.agencyId}
                  href={`/agents/${agency.slug}`}
                  className="block rounded-[23px] border-2 border-[#D7D3CB] bg-[#F6F6F3] p-5 transition-all hover:border-[#10100F]"
                >
                  <div className="flex items-start justify-between gap-4">
                    <div className="min-w-0">
                      <div className="flex items-center gap-2">
                        {i === 0 && (
                          <span className="rounded-full bg-[#EBFFF2] px-2 py-0.5 text-[10px] font-semibold text-[#279455]">
                            #1 match
                          </span>
                        )}
                        <h3 className="text-base font-semibold text-[#000000]">{agency.agencyName}</h3>
                      </div>
                      {agency.niche && (
                        <p className="mt-0.5 text-sm text-[#6B6B6B]">{agency.niche}</p>
                      )}
                      {agency.fitReasons.length > 0 && (
                        <div className="mt-2 flex flex-wrap gap-1.5">
                          {agency.fitReasons.map((r) => (
                            <span key={r} className="rounded-full border border-[#D7D3CB] bg-white px-2 py-0.5 text-[10px] text-[#6B6B6B]">
                              {r}
                            </span>
                          ))}
                        </div>
                      )}
                    </div>
                    <div className="flex shrink-0 flex-col items-end gap-2">
                      <div className="flex items-center gap-1.5">
                        <div className="h-1.5 w-16 overflow-hidden rounded-full bg-[#D7D3CB]">
                          <div
                            className="h-full rounded-full bg-[#10100F]"
                            style={{ width: `${agency.fitScore}%` }}
                          />
                        </div>
                        <span className="text-xs font-semibold tabular-nums text-[#000000]">
                          {agency.fitScore}%
                        </span>
                      </div>
                      {agency.tier && <ScoreBadge tier={agency.tier} />}
                    </div>
                  </div>
                </Link>
              ))}
            </div>
          )}
        </div>
      )}
    </div>
  )
}

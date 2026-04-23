import { notFound } from "next/navigation"
import Link from "next/link"
import { publicApi } from "@/lib/api/public"
import { architectureApi } from "@/lib/api/architecture"
import { EvidenceBadge } from "@/components/ui/evidence-badge"
import { toCategorySubtotals, type EvidenceGrade } from "@/lib/types"

interface Props {
  params: Promise<{ slug: string }>
}

function gradeFromScore(points: number, cap: number): EvidenceGrade {
  if (cap === 0) return "NOT_TESTED"
  const r = points / cap
  if (r >= 0.8) return "VERIFIED"
  if (r > 0) return "DECLARED"
  return "NOT_TESTED"
}

const PLAIN_ENGLISH: Record<string, { strength: string; gap: string; question: string }> = {
  "Orchestration": {
    strength: "Multi-step agent workflows and task routing are present and detectable.",
    gap: "Orchestration layer is not clearly evident — agent may handle only single-step tasks.",
    question: "How does your system handle multi-step reasoning or task delegation between agents?",
  },
  "RAG & Retrieval": {
    strength: "Retrieval-augmented generation is in use — responses are grounded in indexed knowledge.",
    gap: "No retrieval layer detected — responses likely rely on model knowledge alone.",
    question: "What knowledge bases or vector stores does your agent query at runtime?",
  },
  "Memory & State": {
    strength: "Conversation memory and persistent state are implemented.",
    gap: "Memory architecture is absent or undetectable — agent may not carry context across sessions.",
    question: "How does your agent retain context between sessions or interactions?",
  },
  "Guardrails": {
    strength: "Input/output validation and safety controls are detectable.",
    gap: "No guardrail tooling detected — unclear how harmful or off-topic outputs are handled.",
    question: "What controls prevent your agent from generating harmful or off-topic responses?",
  },
  "Observability": {
    strength: "Tracing and monitoring tooling is in place — the system is auditable.",
    gap: "No observability tooling detected — production failures may be hard to diagnose.",
    question: "How do you trace, log, and monitor agent behaviour in production?",
  },
  "Base SDK": {
    strength: "Core LLM SDK integration is confirmed.",
    gap: "LLM integration is unverified.",
    question: "Which LLM providers and API clients does your system depend on?",
  },
}

const RISK_THRESHOLDS = {
  noGuardrails: (pts: number) => pts === 0,
  noObservability: (pts: number) => pts === 0,
  lowOverall: (score: number) => score < 30,
}

export default async function BuyerReportPage({ params }: Props) {
  const { slug } = await params

  let profile
  try {
    profile = await publicApi.agencyProfile(slug)
  } catch {
    notFound()
  }

  let archProfile = null
  try {
    archProfile = await architectureApi.getArchitectureProfile(profile.agencyId)
  } catch {
    // Not available — render without
  }

  const { agency, score, stack } = profile
  const categories = toCategorySubtotals(score.categorySubtotals as Record<string, number> | null)

  const guardrails = categories.find((c) => c.category === "Guardrails")
  const observability = categories.find((c) => c.category === "Observability")
  const riskFlags: string[] = []
  if (guardrails && RISK_THRESHOLDS.noGuardrails(guardrails.points))
    riskFlags.push("No guardrail layer detected — verify safety controls in discovery.")
  if (observability && RISK_THRESHOLDS.noObservability(observability.points))
    riskFlags.push("No observability tooling found — production debugging visibility is unclear.")
  if (score.totalScore != null && RISK_THRESHOLDS.lowOverall(score.totalScore))
    riskFlags.push("Overall stack score is low — this may indicate a lightweight or early-stage build.")

  return (
    <div className="mx-auto max-w-3xl px-6 py-12">
      {/* Back link */}
      <Link
        href={`/agents/${slug}`}
        className="mb-6 inline-flex items-center gap-1 text-sm text-[#6B6B6B] hover:text-[#000000]"
      >
        ← Back to profile
      </Link>

      {/* Header */}
      <div className="mb-6 rounded-[23px] border-2 border-[#D7D3CB] bg-[#F6F6F3] p-8">
        <p className="mb-2 text-xs font-semibold uppercase tracking-widest text-[#6B6B6B]">
          Buyer architecture report
        </p>
        <h1
          className="text-3xl text-[#000000]"
          style={{ fontFamily: "var(--font-dm-serif-display)" }}
        >
          {agency.name}
        </h1>
        {agency.niche && <p className="mt-1 text-[#6B6B6B]">{agency.niche}</p>}

        <div className="mt-4 flex flex-wrap gap-3">
          {archProfile?.archStatus && (
            <span className={[
              "rounded-full px-3 py-1 text-xs font-semibold",
              archProfile.archStatus === "VERIFIED"
                ? "bg-[#EBFFF2] text-[#279455]"
                : archProfile.archStatus === "EVIDENCE_MISMATCH"
                ? "bg-red-50 text-red-600"
                : "bg-amber-50 text-amber-700",
            ].join(" ")}>
              {archProfile.archStatus.replace("_", " ")}
            </span>
          )}
          {archProfile?.confidence && (
            <span className="rounded-full border-2 border-[#D7D3CB] px-3 py-1 text-xs font-medium text-[#6B6B6B]">
              {archProfile.confidence} confidence
            </span>
          )}
          {score.totalScore != null && (
            <span className="rounded-full border-2 border-[#D7D3CB] px-3 py-1 text-xs font-medium text-[#6B6B6B]">
              Stack score {score.totalScore}
            </span>
          )}
        </div>
      </div>

      {/* Risk flags */}
      {riskFlags.length > 0 && (
        <div className="mb-6 rounded-[23px] border-2 border-red-200 bg-red-50 p-5">
          <h2 className="mb-3 text-sm font-semibold text-red-700">⚠ Risk flags</h2>
          <ul className="space-y-2">
            {riskFlags.map((flag) => (
              <li key={flag} className="flex items-start gap-2 text-sm text-red-700">
                <span className="mt-0.5 shrink-0">•</span>
                {flag}
              </li>
            ))}
          </ul>
        </div>
      )}

      {/* Capability summary */}
      {categories.length > 0 && (
        <div className="mb-6 rounded-[23px] border-2 border-[#D7D3CB] bg-[#F6F6F3] p-6">
          <h2 className="mb-4 text-lg font-semibold text-[#000000]">Capability summary</h2>
          <div className="space-y-4">
            {categories.map((c) => {
              const grade = gradeFromScore(c.points, c.cap)
              const info = PLAIN_ENGLISH[c.category]
              const isStrong = grade === "VERIFIED"
              return (
                <div key={c.category} className="rounded-xl border-2 border-[#D7D3CB] bg-white p-4">
                  <div className="flex items-start justify-between gap-3">
                    <p className="text-sm font-semibold text-[#000000]">{c.category}</p>
                    <EvidenceBadge grade={grade} className="shrink-0" />
                  </div>
                  {info && (
                    <p className="mt-2 text-sm text-[#6B6B6B]">
                      {isStrong ? info.strength : info.gap}
                    </p>
                  )}
                  <div className="mt-3 flex items-center gap-2">
                    <div className="h-1.5 flex-1 overflow-hidden rounded-full bg-[#D7D3CB]">
                      <div
                        className={[
                          "h-full rounded-full",
                          grade === "VERIFIED"
                            ? "bg-[#2ECC71]"
                            : grade === "DECLARED"
                            ? "bg-[#F59E0B]"
                            : "bg-[#D7D3CB]",
                        ].join(" ")}
                        style={{ width: `${c.cap > 0 ? Math.round((c.points / c.cap) * 100) : 0}%` }}
                      />
                    </div>
                    <span className="text-xs text-[#6B6B6B]">{c.points}/{c.cap}</span>
                  </div>
                </div>
              )
            })}
          </div>
        </div>
      )}

      {/* Questions to ask */}
      <div className="mb-6 rounded-[23px] border-2 border-[#D7D3CB] bg-[#F6F6F3] p-6">
        <h2 className="mb-4 text-lg font-semibold text-[#000000]">Questions to ask before signing</h2>
        <div className="space-y-3">
          {categories
            .filter((c) => PLAIN_ENGLISH[c.category])
            .map((c, i) => (
              <div key={c.category} className="flex items-start gap-3 rounded-xl border-2 border-[#D7D3CB] bg-white p-4">
                <span className="mt-0.5 text-sm font-semibold text-[#6B6B6B]">{String(i + 1).padStart(2, "0")}</span>
                <p className="text-sm text-[#000000]">{PLAIN_ENGLISH[c.category]!.question}</p>
              </div>
            ))}
          <div className="flex items-start gap-3 rounded-xl border-2 border-[#D7D3CB] bg-white p-4">
            <span className="mt-0.5 text-sm font-semibold text-[#6B6B6B]">{String(categories.length + 1).padStart(2, "0")}</span>
            <p className="text-sm text-[#000000]">
              Can you provide a reference from a client who has deployed this agent in production?
            </p>
          </div>
        </div>
      </div>

      {/* Detected packages summary */}
      {stack.length > 0 && (
        <div className="mb-6 rounded-[23px] border-2 border-[#D7D3CB] bg-[#F6F6F3] p-6">
          <h2 className="mb-3 text-lg font-semibold text-[#000000]">Evidence sources</h2>
          <p className="mb-3 text-sm text-[#6B6B6B]">
            {stack.length} package{stack.length !== 1 ? "s" : ""} detected in manifest analysis — these are the verified tools powering this agent.
          </p>
          <div className="flex flex-wrap gap-2">
            {stack.map((pkg) => (
              <span
                key={pkg.packageName}
                className="rounded-full border-2 border-[#D7D3CB] bg-white px-3 py-1 text-xs font-medium text-[#000000]"
              >
                {pkg.packageName}
              </span>
            ))}
          </div>
        </div>
      )}

      {/* Footer disclaimer */}
      <p className="text-xs text-[#6B6B6B]">
        This report is based on manifest analysis and available architecture signals.
        SyncScore V2 verifies architecture evidence — not live system behaviour or performance.
        For behavioral proof, ask the agency for a demonstration or pilot engagement.
      </p>
    </div>
  )
}

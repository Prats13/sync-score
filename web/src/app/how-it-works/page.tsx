import Link from "next/link"

const STEPS = [
  {
    step: "01",
    title: "Connect your stack",
    body: "Sign up as a builder, complete your agency profile, and either connect your GitHub username or paste a package manifest (package.json, requirements.txt, etc.).",
  },
  {
    step: "02",
    title: "We scan your dependencies",
    body: "Our scanner checks every package against the SyncScore V1 ruleset — six categories: Orchestration, RAG, Memory, Guardrails, Observability, and Base SDK.",
  },
  {
    step: "03",
    title: "Get your score and tier",
    body: "Packages earn points up to a per-category cap. Your total score determines your tier: Wrapper (0–29), Builder (30–64), or Expert (65–100).",
  },
  {
    step: "04",
    title: "Publish proof",
    body: "Toggle your profile public to receive a shareable URL. Businesses browsing SyncScore can view your score, stack, and contact you directly.",
  },
]

export default function HowItWorksPage() {
  return (
    <div className="mx-auto max-w-4xl px-6 py-20">
      <h1
        className="mb-6 text-5xl text-ink"
        style={{ fontFamily: "var(--font-display)" }}
      >
        How SyncScore works
      </h1>
      <p className="mb-14 max-w-2xl text-lg leading-relaxed text-muted">
        A transparent, reproducible way to verify what&apos;s actually inside an AI agent.
      </p>

      <div className="space-y-6">
        {STEPS.map(({ step, title, body }) => (
          <div key={step} className="flex gap-6 rounded-[23px] border-2 border-hairline-strong bg-surface-inset p-6">
            <div className="shrink-0 text-2xl font-bold tabular-nums text-hairline-strong">
              {step}
            </div>
            <div>
              <h3 className="mb-1.5 font-semibold text-ink">{title}</h3>
              <p className="text-sm leading-relaxed text-muted">{body}</p>
            </div>
          </div>
        ))}
      </div>

      <div className="mt-12 flex gap-4 flex-wrap">
        <Link
          href="/auth/signup"
          className="inline-flex h-11 items-center rounded-full bg-trust px-8 text-sm font-medium text-bg hover:opacity-80 transition-opacity"
        >
          Get verified
        </Link>
        <Link
          href="/browse"
          className="inline-flex h-11 items-center rounded-full border-2 border-hairline-strong px-8 text-sm font-medium text-ink hover:border-trust-border transition-colors"
        >
          Browse agents
        </Link>
      </div>
    </div>
  )
}

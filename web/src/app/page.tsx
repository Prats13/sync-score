import Link from "next/link"
import { publicApi } from "@/lib/api/public"
import { AgentCard } from "@/components/ui/agent-card"

async function getFeaturedAgencies() {
  try {
    const agencies = await publicApi.browse()
    return agencies.slice(0, 3)
  } catch {
    return []
  }
}

export default async function HomePage() {
  const featured = await getFeaturedAgencies()

  return (
    <div className="bg-[#F7F6F2]">
      {/* ── Hero ──────────────────────────────────────────────────────── */}
      <section className="mx-auto max-w-6xl px-6 pb-24 pt-20 text-center">
        <h1
          className="mx-auto max-w-4xl text-5xl leading-tight text-[#000000] sm:text-6xl"
          style={{ fontFamily: "var(--font-dm-serif-display)" }}
        >
          Setting the standard for AI agent trust
        </h1>
        <p className="mx-auto mt-6 max-w-2xl text-lg font-medium leading-relaxed tracking-tight text-[#6B6B6B] sm:text-xl">
          As AI agents enter core business operations, technical verification becomes
          non-negotiable. SyncScore audits and certifies AI agent architecture, so businesses
          deploy proof, not promises.
        </p>
        <div className="mt-10 flex flex-col items-center justify-center gap-3 sm:flex-row">
          <Link
            href="/auth/signup"
            className="inline-flex h-11 items-center rounded-full bg-[#10100F] px-8 text-sm font-medium text-white transition-opacity hover:opacity-80"
          >
            Get Started
          </Link>
          <Link
            href="/browse"
            className="inline-flex h-11 items-center rounded-full border-2 border-[#D7D3CB] px-8 text-sm font-medium text-[#000000] transition-colors hover:border-[#10100F]"
          >
            Browse verified agents
          </Link>
        </div>
      </section>

      {/* ── How it works ──────────────────────────────────────────────── */}
      <section className="border-y-2 border-[#D7D3CB] bg-[#F6F6F3] py-20">
        <div className="mx-auto max-w-6xl px-6">
          <h2
            className="mb-12 text-center text-3xl text-[#000000] sm:text-4xl"
            style={{ fontFamily: "var(--font-dm-serif-text)" }}
          >
            How SyncScore works
          </h2>
          <div className="grid gap-8 sm:grid-cols-3">
            {[
              {
                step: "01",
                title: "Verify your stack",
                body: "Connect your GitHub or paste your package manifest. We scan your agent's dependencies against our technical ruleset.",
              },
              {
                step: "02",
                title: "Get your score",
                body: "We evaluate your stack across six categories: Orchestration, RAG, Memory, Guardrails, Observability, and Base SDK.",
              },
              {
                step: "03",
                title: "Publish proof",
                body: "Share your verified score card with clients and platforms. A public profile shows exactly what your agent is built on.",
              },
            ].map(({ step, title, body }) => (
              <div key={step} className="rounded-[23px] border-2 border-[#D7D3CB] bg-white p-8">
                <div className="mb-4 text-xs font-semibold uppercase tracking-widest text-[#6B6B6B]">
                  {step}
                </div>
                <h3 className="mb-3 text-lg font-semibold text-[#000000]">{title}</h3>
                <p className="text-sm leading-relaxed text-[#6B6B6B]">{body}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* ── Featured agencies ─────────────────────────────────────────── */}
      {featured.length > 0 && (
        <section className="mx-auto max-w-6xl px-6 py-20">
          <div className="mb-8 flex items-center justify-between">
            <h2
              className="text-2xl text-[#000000] sm:text-3xl"
              style={{ fontFamily: "var(--font-dm-serif-text)" }}
            >
              Top verified agents
            </h2>
            <Link
              href="/browse"
              className="text-sm font-medium text-[#6B6B6B] underline-offset-2 hover:underline"
            >
              View all →
            </Link>
          </div>
          <div className="grid gap-5 sm:grid-cols-3">
            {featured.map((a) => (
              <AgentCard key={a.agencyId} agency={a} />
            ))}
          </div>
        </section>
      )}

      {/* ── Split CTA ─────────────────────────────────────────────────── */}
      <section className="border-t-2 border-[#D7D3CB] bg-[#10100F] py-20 text-center">
        <h2
          className="mb-8 text-3xl text-white sm:text-4xl"
          style={{ fontFamily: "var(--font-dm-serif-display)" }}
        >
          Who are you?
        </h2>
        <div className="mx-auto flex max-w-sm flex-col gap-4 sm:max-w-lg sm:flex-row">
          <Link
            href="/for-business"
            className="flex-1 rounded-full border-2 border-white/30 px-8 py-3.5 text-sm font-medium text-white transition-colors hover:bg-white/10"
          >
            I&apos;m a Business
          </Link>
          <Link
            href="/auth/signup"
            className="flex-1 rounded-full bg-white px-8 py-3.5 text-sm font-medium text-[#10100F] transition-opacity hover:opacity-90"
          >
            I&apos;m a Builder
          </Link>
        </div>
      </section>
    </div>
  )
}

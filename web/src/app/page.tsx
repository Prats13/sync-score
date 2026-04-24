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
    <div className="bg-bg transition-colors duration-300">
      {/* ── Hero ──────────────────────────────────────────────────────── */}
      <section className="mx-auto max-w-6xl px-6 pb-24 pt-20 text-center relative overflow-hidden">
        <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-[800px] h-[800px] bg-verified/5 rounded-full blur-3xl -z-10 pointer-events-none"></div>
        <h1 className="mx-auto max-w-4xl text-5xl leading-[1.02] tracking-[-0.015em] text-ink sm:text-6xl font-display">
          Setting the standard for <span className="text-verified">AI agent trust</span>
        </h1>
        <p className="mx-auto mt-6 max-w-2xl text-[16px] leading-[1.65] text-muted">
          As AI agents enter core business operations, technical verification becomes
          non-negotiable. SyncScore audits and certifies AI agent architecture, so businesses
          deploy proof, not promises.
        </p>
        <div className="mt-10 flex flex-col items-center justify-center gap-3 sm:flex-row">
          <Link
            href="/auth/signup"
            className="inline-flex h-11 items-center rounded-full bg-trust px-8 text-[13px] font-medium text-bg transition-opacity hover:opacity-80"
          >
            Get Started
          </Link>
          <Link
            href="/browse"
            className="inline-flex h-11 items-center rounded-full border border-hairline-strong px-8 text-[13px] font-medium text-ink transition-all hover:border-trust-border bg-surface-2 shadow-sm hover:shadow-md"
          >
            Browse verified agents
          </Link>
        </div>
      </section>

      {/* ── Stats equivalent (from v2 hero concept) ───────────────────── */}
      <div className="border-y border-trust-border/10 py-10 bg-trust-bg/50">
        <div className="mx-auto flex max-w-3xl flex-wrap justify-center gap-8 sm:gap-24 px-6">
           <div className="text-center"><div className="text-4xl font-display text-ink leading-none">6</div><div className="mt-2 text-[10px] font-mono tracking-widest uppercase text-trust">Trust Layers</div></div>
           <div className="text-center"><div className="text-4xl font-display flex items-baseline justify-center gap-1 leading-none text-verified">✓ <span className="text-ink">V2</span></div><div className="mt-2 text-[10px] font-mono tracking-widest uppercase text-trust">Architecture</div></div>
           <div className="text-center"><div className="text-4xl font-display text-ink leading-none">100%</div><div className="mt-2 text-[10px] font-mono tracking-widest uppercase text-trust">Confidential</div></div>
        </div>
      </div>

      {/* ── How it works ──────────────────────────────────────────────── */}
      <section className="bg-surface-inset py-24">
        <div className="mx-auto max-w-6xl px-6">
          <div className="mb-12 flex justify-center">
            <span className="inline-flex items-center gap-2 font-mono text-[11px] tracking-[0.14em] uppercase text-trust font-bold">
              <span className="w-2.5 h-2.5 rounded-full bg-verified animate-pulse shadow-[0_0_8px_rgba(46,204,113,0.6)]"></span>
              How SyncScore works
            </span>
          </div>
          
          <div className="grid gap-6 sm:grid-cols-3">
            {[
              {
                step: "01",
                title: "Verify your stack",
                body: "Connect your GitHub securely. We run a scoped architectural scan against our standard technical ruleset.",
              },
              {
                step: "02",
                title: "Get your score",
                body: "Earn architecture-level Verified badges and confidence ratings mapped to our six evaluation categories.",
              },
              {
                step: "03",
                title: "Publish proof",
                body: "Share a sleek, buyer-friendly report that explains your tech stack capabilities in plain English.",
              },
            ].map(({ step, title, body }) => (
              <div key={step} className="card-base p-8 flex flex-col items-start text-left border-t-4 border-t-verified/80">
                <div className="mb-4 text-[10px] font-mono font-bold uppercase tracking-widest text-verified/80 bg-verified/10 px-2 py-1 rounded">
                  {step}
                </div>
                <h3 className="mb-2 text-[17px] font-bold text-ink">{title}</h3>
                <p className="text-[14px] leading-[1.65] text-muted">{body}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* ── Featured agencies ─────────────────────────────────────────── */}
      {featured.length > 0 && (
        <section className="mx-auto max-w-6xl px-6 py-24">
          <div className="mb-8 flex items-center justify-between">
            <h2 className="text-3xl text-ink font-display">
              Top verified agents
            </h2>
            <Link
              href="/browse"
              className="text-[13px] font-medium text-muted hover:text-ink transition-colors"
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
      <section className="border-t border-hairline-strong bg-surface-2 py-24 text-center">
        <h2 className="mb-8 text-4xl text-ink font-display">
          Who are you?
        </h2>
        <div className="mx-auto flex max-w-sm flex-col gap-4 sm:max-w-lg sm:flex-row">
          <Link
            href="/for-business"
            className="flex-1 rounded-full border border-hairline-strong px-8 py-3.5 text-[13px] font-medium text-ink transition-colors hover:border-trust-border bg-surface-1 shadow-sm"
          >
            I&apos;m a Business
          </Link>
          <Link
            href="/auth/signup"
            className="flex-1 rounded-full bg-trust px-8 py-3.5 text-[13px] font-medium text-bg transition-opacity hover:opacity-90 shadow-sm"
          >
            I&apos;m a Builder
          </Link>
        </div>
      </section>
    </div>
  )
}

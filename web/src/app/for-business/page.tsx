import Link from "next/link"

export default function ForBusinessPage() {
  return (
    <div className="mx-auto max-w-4xl px-6 py-20">
      <h1
        className="mb-6 text-5xl text-[#000000]"
        style={{ fontFamily: "var(--font-dm-serif-display)" }}
      >
        Build with verified AI agents
      </h1>
      <p className="mb-12 max-w-2xl text-lg leading-relaxed text-[#6B6B6B]">
        SyncScore lets you filter and evaluate AI agent builders by their verified technical stack —
        before you sign a contract. No more taking an agency&apos;s word for it.
      </p>

      <div className="grid gap-6 sm:grid-cols-2">
        {[
          {
            title: "See exactly what's inside",
            body: "Browse agencies by tier and view the specific packages they use for orchestration, memory, guardrails, and more.",
          },
          {
            title: "Trust the verification",
            body: "GitHub-verified scores are scanned directly from source code — not self-reported. Look for the SYNCSCORE VERIFIED badge.",
          },
          {
            title: "Filter by category strength",
            body: "Need an agent with guardrails? Filter to Expert-tier builders and check their category scores before reaching out.",
          },
          {
            title: "Reduce vendor risk",
            body: "Technical transparency lowers the risk of deploying agents that rely on untested, closed, or unsupported libraries.",
          },
        ].map(({ title, body }) => (
          <div key={title} className="rounded-[23px] border-2 border-[#D7D3CB] bg-[#F6F6F3] p-6">
            <h3 className="mb-2 font-semibold text-[#000000]">{title}</h3>
            <p className="text-sm leading-relaxed text-[#6B6B6B]">{body}</p>
          </div>
        ))}
      </div>

      <div className="mt-12">
        <Link
          href="/browse"
          className="inline-flex h-11 items-center rounded-full bg-[#10100F] px-8 text-sm font-medium text-white hover:opacity-80 transition-opacity"
        >
          Browse verified agents
        </Link>
      </div>
    </div>
  )
}

"use client"

import { useEffect, useState } from "react"
import { architectureApi } from "@/lib/api/architecture"
import type { ReviewCaseResponse } from "@/lib/types"
import { Button } from "@/components/ui/button"

function formatDate(iso: string) {
  return new Date(iso).toLocaleDateString("en-US", {
    month: "short",
    day: "numeric",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  })
}

function TriggerDetails({ details }: { details: unknown }) {
  if (!details || typeof details !== "object") return null
  const d = details as Record<string, unknown>
  return (
    <dl className="space-y-1.5 text-xs">
      {Object.entries(d).map(([k, v]) => (
        <div key={k} className="flex gap-2">
          <dt className="w-36 shrink-0 font-medium text-[#6B6B6B]">{k.replace(/_/g, " ")}</dt>
          <dd className="text-[#000000]">{String(v)}</dd>
        </div>
      ))}
    </dl>
  )
}

const STRUCTURAL_SIGNALS = [
  "Score increased by more than 20 pts in a single scan",
  "High-tier libraries added without commit activity changes",
  "Observability claims not supported by visible evidence",
  "Manifest edited within 1 hr of rescan trigger",
]

export default function AdminReviewCasesPage() {
  const [cases, setCases] = useState<ReviewCaseResponse[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [selected, setSelected] = useState<ReviewCaseResponse | null>(null)
  const [note, setNote] = useState("")
  const [resolving, setResolving] = useState(false)

  const refresh = async () => {
    setIsLoading(true)
    setError(null)
    try {
      const rows = await architectureApi.adminListOpenReviewCases()
      setCases(rows)
      if (selected) {
        const updated = rows.find((r) => r.id === selected.id)
        setSelected(updated ?? null)
      }
    } catch (e) {
      setError(e instanceof Error ? e.message : "Failed to load review cases")
    } finally {
      setIsLoading(false)
    }
  }

  useEffect(() => { refresh() }, [])

  const resolve = async (action: "APPROVE" | "DISMISS") => {
    if (!selected) return
    setResolving(true)
    try {
      await architectureApi.adminResolveReviewCase(selected.id, action, note)
      setNote("")
      setSelected(null)
      await refresh()
    } catch (e) {
      setError(e instanceof Error ? e.message : "Resolve failed")
    } finally {
      setResolving(false)
    }
  }

  return (
    <div className="mx-auto max-w-6xl px-6 py-12">
      {/* Header */}
      <div className="mb-6 flex items-start justify-between gap-4">
        <div>
          <h1 className="text-3xl text-[#000000]" style={{ fontFamily: "var(--font-dm-serif-display)" }}>
            Review cases
          </h1>
          <p className="mt-1 text-sm text-[#6B6B6B]">
            Anti-gaming flags requiring an admin decision. Select a case to review the detail.
          </p>
        </div>
        <Button size="sm" variant="outline" onClick={refresh} disabled={isLoading}>
          {isLoading ? "Refreshing…" : "Refresh"}
        </Button>
      </div>

      {error && (
        <div className="mb-4 rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-600">
          {error}
        </div>
      )}

      <div className="flex gap-5 items-start">
        {/* ── Queue panel ── */}
        <div className="w-72 shrink-0 space-y-2">
          <p className="mb-2 text-xs font-semibold uppercase tracking-widest text-[#6B6B6B]">
            Open ({cases.length})
          </p>
          {isLoading ? (
            Array.from({ length: 3 }).map((_, i) => (
              <div key={i} className="h-20 animate-pulse rounded-[23px] bg-[#F6F6F3]" />
            ))
          ) : cases.length === 0 ? (
            <div className="rounded-[23px] border-2 border-[#D7D3CB] bg-[#F6F6F3] p-6 text-center text-sm text-[#6B6B6B]">
              No open cases
            </div>
          ) : (
            cases.map((c) => (
              <button
                key={c.id}
                onClick={() => { setSelected(c); setNote("") }}
                className={[
                  "w-full rounded-[23px] border-2 p-4 text-left transition-all",
                  selected?.id === c.id
                    ? "border-[#10100F] bg-white"
                    : "border-[#D7D3CB] bg-[#F6F6F3] hover:border-[#10100F]/40",
                ].join(" ")}
              >
                <div className="flex items-start justify-between gap-2">
                  <span className="inline-block rounded-full bg-amber-100 px-2 py-0.5 text-[10px] font-semibold uppercase text-amber-700">
                    ⚑ Flag
                  </span>
                  <span className="text-[10px] text-[#6B6B6B]">{formatDate(c.createdAt)}</span>
                </div>
                <p className="mt-2 text-sm font-medium text-[#000000] line-clamp-2">{c.triggerReason}</p>
                <p className="mt-1 text-[10px] text-[#6B6B6B]">Agency {c.agencyId.slice(0, 8)}…</p>
              </button>
            ))
          )}
        </div>

        {/* ── Detail panel ── */}
        <div className="flex-1 min-w-0">
          {!selected ? (
            <div className="rounded-[23px] border-2 border-dashed border-[#D7D3CB] bg-[#F6F6F3] p-12 text-center text-[#6B6B6B]">
              Select a case from the queue to review details
            </div>
          ) : (
            <div className="space-y-4">
              {/* Case header */}
              <div className="rounded-[23px] border-2 border-amber-200 bg-amber-50 p-5">
                <div className="flex items-start justify-between gap-3">
                  <div>
                    <span className="text-xs font-semibold uppercase tracking-widest text-amber-700">
                      ⚑ Anti-gaming flag
                    </span>
                    <p className="mt-1 text-lg font-semibold text-[#000000]">{selected.triggerReason}</p>
                  </div>
                  <span className="shrink-0 text-xs text-[#6B6B6B]">{formatDate(selected.createdAt)}</span>
                </div>
                <div className="mt-3 flex flex-wrap gap-x-6 gap-y-1 text-xs text-[#6B6B6B]">
                  <span>Agency: <strong className="text-[#000000]">{selected.agencyId}</strong></span>
                  <span>Scan: <strong className="text-[#000000]">{selected.architectureScanId.slice(0, 12)}…</strong></span>
                  <span>Status: <strong className="text-[#000000]">{selected.status}</strong></span>
                </div>
              </div>

              {/* Score jump / trigger details */}
              {selected.triggerDetailsJson && (
                <div className="rounded-[23px] border-2 border-[#D7D3CB] bg-[#F6F6F3] p-5">
                  <h3 className="mb-3 text-sm font-semibold text-[#000000]">Score jump analysis</h3>
                  <TriggerDetails details={selected.triggerDetailsJson} />
                </div>
              )}

              {/* Structural signals */}
              <div className="rounded-[23px] border-2 border-[#D7D3CB] bg-[#F6F6F3] p-5">
                <h3 className="mb-3 text-sm font-semibold text-[#000000]">Structural signals</h3>
                <div className="space-y-2">
                  {STRUCTURAL_SIGNALS.map((sig) => (
                    <div key={sig} className="flex items-start gap-2 text-xs text-[#6B6B6B]">
                      <span className="mt-0.5 text-amber-500">△</span>
                      {sig}
                    </div>
                  ))}
                </div>
              </div>

              {/* Audit trail */}
              <div className="rounded-[23px] border-2 border-[#D7D3CB] bg-[#F6F6F3] p-5">
                <h3 className="mb-3 text-sm font-semibold text-[#000000]">Audit trail</h3>
                <div className="space-y-2 text-xs text-[#6B6B6B]">
                  <div className="flex items-center gap-3">
                    <span className="h-1.5 w-1.5 rounded-full bg-[#2ECC71]" />
                    Case opened · {formatDate(selected.createdAt)}
                  </div>
                  <div className="flex items-center gap-3">
                    <span className="h-1.5 w-1.5 rounded-full bg-amber-400" />
                    Architecture scan triggered · {selected.architectureScanId.slice(0, 8)}…
                  </div>
                  {selected.resolvedAt && (
                    <div className="flex items-center gap-3">
                      <span className="h-1.5 w-1.5 rounded-full bg-[#6B6B6B]" />
                      Resolved by {selected.resolvedBy ?? "admin"} · {formatDate(selected.resolvedAt)}
                      {selected.resolutionNote && ` — "${selected.resolutionNote}"`}
                    </div>
                  )}
                </div>
              </div>

              {/* Reviewer actions */}
              <div className="rounded-[23px] border-2 border-[#D7D3CB] bg-white p-5">
                <h3 className="mb-3 text-sm font-semibold text-[#000000]">Reviewer decision</h3>
                <label className="mb-1 block text-xs font-medium text-[#6B6B6B]">
                  Resolution note (optional)
                </label>
                <textarea
                  value={note}
                  onChange={(e) => setNote(e.target.value)}
                  placeholder="Explain the decision for the audit trail…"
                  rows={3}
                  className="mb-4 w-full rounded-xl border-2 border-[#D7D3CB] bg-[#F6F6F3] px-3 py-2 text-sm text-[#000000] outline-none focus:border-[#10100F]"
                />
                <div className="flex gap-3">
                  <Button
                    className="rounded-full bg-[#10100F] text-white hover:bg-[#10100F]/80"
                    disabled={resolving}
                    onClick={() => resolve("APPROVE")}
                  >
                    {resolving ? "Saving…" : "Approve — badge restored"}
                  </Button>
                  <Button
                    variant="outline"
                    disabled={resolving}
                    onClick={() => resolve("DISMISS")}
                  >
                    Dismiss flag
                  </Button>
                </div>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  )
}

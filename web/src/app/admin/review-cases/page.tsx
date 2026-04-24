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
          <dt className="w-36 shrink-0 font-medium text-muted">{k.replace(/_/g, " ")}</dt>
          <dd className="text-ink">{v !== null && v !== undefined ? String(v) : "—"}</dd>
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

const TRIGGER_REASON_LABELS: Record<string, string> = {
  SCORE_JUMP_LARGE:              "Score jumped more than 20 pts",
  HIGH_TIER_NO_COMMIT_ACTIVITY:  "High-tier libs added, no commit activity",
  OBSERVABILITY_NO_EVIDENCE:     "Observability claimed but no evidence",
  RAPID_MANIFEST_EDIT:           "Manifest edited just before rescan",
  ANTI_GAMING_FLAG:              "Anti-gaming heuristic triggered",
  MANUAL_FLAG:                   "Manually flagged by admin",
}

const CASE_STATUS_META: Record<string, { label: string; chip: string }> = {
  OPEN:      { label: "Open",     chip: "bg-amber-100 text-amber-700" },
  APPROVED:  { label: "Approved", chip: "bg-verified-bg text-verified" },
  DISMISSED: { label: "Dismissed",chip: "bg-surface-inset text-muted" },
}

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
          <h1 className="text-3xl text-ink" style={{ fontFamily: "var(--font-display)" }}>
            Review cases
          </h1>
          <p className="mt-1 text-sm text-muted">
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
          <p className="mb-2 text-xs font-semibold uppercase tracking-widest text-muted">
            Open ({cases.length})
          </p>
          {isLoading ? (
            Array.from({ length: 3 }).map((_, i) => (
              <div key={i} className="h-20 animate-pulse rounded-[23px] bg-surface-inset" />
            ))
          ) : cases.length === 0 ? (
            <div className="rounded-[23px] border-2 border-hairline-strong bg-surface-inset p-6 text-center text-sm text-muted">
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
                    ? "border-trust-border bg-surface-1"
                    : "border-hairline-strong bg-surface-inset hover:border-trust-border/40",
                ].join(" ")}
              >
                <div className="flex items-start justify-between gap-2">
                  <span className={[
                    "inline-block rounded-full px-2 py-0.5 text-[10px] font-semibold uppercase",
                    CASE_STATUS_META[c.status]?.chip ?? "bg-amber-100 text-amber-700",
                  ].join(" ")}>
                    {CASE_STATUS_META[c.status]?.label ?? c.status}
                  </span>
                  <span className="text-[10px] text-muted">{formatDate(c.createdAt)}</span>
                </div>
                <p className="mt-2 text-sm font-medium text-ink line-clamp-2">
                  {TRIGGER_REASON_LABELS[c.triggerReason] ?? c.triggerReason.replace(/_/g, " ")}
                </p>
                <p className="mt-1 text-[10px] text-muted">Agency {c.agencyId.slice(0, 8)}…</p>
              </button>
            ))
          )}
        </div>

        {/* ── Detail panel ── */}
        <div className="flex-1 min-w-0">
          {!selected ? (
            <div className="rounded-[23px] border-2 border-dashed border-hairline-strong bg-surface-inset p-12 text-center text-muted">
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
                    <p className="mt-1 text-lg font-semibold text-ink">
                      {TRIGGER_REASON_LABELS[selected.triggerReason] ?? selected.triggerReason.replace(/_/g, " ")}
                    </p>
                  </div>
                  <span className="shrink-0 text-xs text-muted">{formatDate(selected.createdAt)}</span>
                </div>
                <div className="mt-3 flex flex-wrap gap-x-6 gap-y-1 text-xs text-muted">
                  <span>Agency: <strong className="text-ink">{selected.agencyId}</strong></span>
                  <span>Scan: <strong className="text-ink">{selected.architectureScanId.slice(0, 12)}…</strong></span>
                  <span>Status: <strong className="text-ink">{selected.status}</strong></span>
                </div>
              </div>

              {/* Score jump / trigger details */}
              {Boolean(selected.triggerDetailsJson) && (
                <div className="rounded-[23px] border-2 border-hairline-strong bg-surface-inset p-5">
                  <h3 className="mb-3 text-sm font-semibold text-ink">Score jump analysis</h3>
                  <TriggerDetails details={selected.triggerDetailsJson} />
                </div>
              )}

              {/* Structural signals */}
              <div className="rounded-[23px] border-2 border-hairline-strong bg-surface-inset p-5">
                <h3 className="mb-3 text-sm font-semibold text-ink">Structural signals</h3>
                <div className="space-y-2">
                  {STRUCTURAL_SIGNALS.map((sig) => (
                    <div key={sig} className="flex items-start gap-2 text-xs text-muted">
                      <span className="mt-0.5 text-amber-500">△</span>
                      {sig}
                    </div>
                  ))}
                </div>
              </div>

              {/* Audit trail */}
              <div className="rounded-[23px] border-2 border-hairline-strong bg-surface-inset p-5">
                <h3 className="mb-3 text-sm font-semibold text-ink">Audit trail</h3>
                <div className="space-y-2 text-xs text-muted">
                  <div className="flex items-center gap-3">
                    <span className="h-1.5 w-1.5 rounded-full bg-verified" />
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
              <div className="rounded-[23px] border-2 border-hairline-strong bg-surface-1 p-5">
                <h3 className="mb-3 text-sm font-semibold text-ink">Reviewer decision</h3>
                <label className="mb-1 block text-xs font-medium text-muted">
                  Resolution note (optional)
                </label>
                <textarea
                  value={note}
                  onChange={(e) => setNote(e.target.value)}
                  placeholder="Explain the decision for the audit trail…"
                  rows={3}
                  className="mb-4 w-full rounded-xl border-2 border-hairline-strong bg-surface-inset px-3 py-2 text-sm text-ink outline-none focus:border-trust-border"
                />
                <div className="flex gap-3">
                  <Button
                    className="rounded-full bg-trust text-bg hover:bg-trust/80"
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

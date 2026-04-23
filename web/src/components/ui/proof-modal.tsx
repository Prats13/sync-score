"use client"

import { useEffect, useRef } from "react"
import type { PublicAgencyProfile, EvidenceGrade } from "@/lib/types"
import { toCategorySubtotals } from "@/lib/types"
import { EvidenceBadge } from "./evidence-badge"

// ── helpers ───────────────────────────────────────────────────────────────

function gradeFromScore(points: number, cap: number): EvidenceGrade {
  if (cap === 0) return "NOT_TESTED"
  const r = points / cap
  if (r >= 0.8) return "VERIFIED"
  if (r > 0) return "DECLARED"
  return "NOT_TESTED"
}

function formatDate(iso: string) {
  return new Date(iso).toLocaleDateString("en-US", {
    month: "long",
    day: "numeric",
    year: "numeric",
  })
}

// ── ProofModal ─────────────────────────────────────────────────────────────

interface ProofModalProps {
  open: boolean
  onClose: () => void
  profile: PublicAgencyProfile
  archProfile?: {
    confidence: string | null
    archStatus: string | null
    evidenceSource: string
    lastVerifiedAt: string | null
  } | null
  lastScanDate?: string | null
}

const ARCH_STATUS_LABELS: Record<string, { label: string; badge: string }> = {
  VERIFIED:          { label: "Architecture Verified",  badge: "bg-[#EBFFF2] text-[#279455] border-[#2ECC71]" },
  UNDER_REVIEW:      { label: "Under Review",           badge: "bg-amber-50 text-amber-700 border-amber-300" },
  EVIDENCE_MISMATCH: { label: "Evidence Mismatch",      badge: "bg-red-50 text-red-600 border-red-300" },
  FRESHNESS_LOW:     { label: "Freshness Low",          badge: "bg-[#F6F6F3] text-[#6B6B6B] border-[#D7D3CB]" },
}

const CONFIDENCE_LABELS: Record<string, { label: string; color: string }> = {
  HIGH:   { label: "High confidence",   color: "text-[#279455]" },
  MEDIUM: { label: "Medium confidence", color: "text-amber-600" },
  LOW:    { label: "Low confidence",    color: "text-red-500" },
}

const SOURCE_LABELS: Record<string, string> = {
  GITHUB_PUBLIC:          "Public GitHub",
  CONFIDENTIAL_GITHUB:    "Confidential GitHub",
  CONFIDENTIAL_SESSION:   "Confidential Session",
  MIXED_EVIDENCE:         "Mixed Evidence",
  PASTE:                  "Paste Upload",
}

export function ProofModal({ open, onClose, profile, archProfile, lastScanDate }: ProofModalProps) {
  const overlayRef = useRef<HTMLDivElement>(null)

  // Close on Escape
  useEffect(() => {
    if (!open) return
    const handler = (e: KeyboardEvent) => { if (e.key === "Escape") onClose() }
    document.addEventListener("keydown", handler)
    return () => document.removeEventListener("keydown", handler)
  }, [open, onClose])

  // Lock body scroll
  useEffect(() => {
    if (open) {
      document.body.style.overflow = "hidden"
    } else {
      document.body.style.overflow = ""
    }
    return () => { document.body.style.overflow = "" }
  }, [open])

  if (!open) return null

  const { agency, score, stack, verificationLabel } = profile
  const categories = toCategorySubtotals(score.categorySubtotals as Record<string, number> | null)
  const verifiedCount = categories.filter((c) => gradeFromScore(c.points, c.cap) === "VERIFIED").length
  const archMeta = archProfile?.archStatus ? ARCH_STATUS_LABELS[archProfile.archStatus] : null
  const confMeta = archProfile?.confidence ? CONFIDENCE_LABELS[archProfile.confidence] : null
  const sourceLabel = archProfile?.evidenceSource
    ? (SOURCE_LABELS[archProfile.evidenceSource] ?? archProfile.evidenceSource.replace(/_/g, " "))
    : null

  return (
    <>
      {/* Overlay */}
      <div
        ref={overlayRef}
        className="fixed inset-0 z-50 bg-black/40 backdrop-blur-sm"
        onClick={(e) => { if (e.target === overlayRef.current) onClose() }}
        aria-label="Close proof modal"
      />

      {/* Panel */}
      <div
        role="dialog"
        aria-modal="true"
        aria-label={`Verification proof for ${agency.name}`}
        className="fixed inset-x-0 bottom-0 z-50 max-h-[90vh] overflow-y-auto rounded-t-[32px] bg-white shadow-2xl md:inset-auto md:left-1/2 md:top-1/2 md:-translate-x-1/2 md:-translate-y-1/2 md:rounded-[23px] md:max-h-[85vh] md:w-full md:max-w-2xl"
        style={{ animation: "slideUp 0.25s ease" }}
      >
        {/* Drag handle (mobile) */}
        <div className="flex justify-center pt-3 pb-1 md:hidden">
          <div className="h-1 w-10 rounded-full bg-[#D7D3CB]" />
        </div>

        <div className="p-6 md:p-8">
          {/* Header */}
          <div className="flex items-start justify-between gap-4 mb-6">
            <div>
              <p className="text-xs font-semibold uppercase tracking-widest text-[#6B6B6B] mb-1">
                Verification proof
              </p>
              <h2
                className="text-2xl text-[#000000]"
                style={{ fontFamily: "var(--font-dm-serif-display)" }}
              >
                {agency.name}
              </h2>
              {agency.niche && (
                <p className="mt-0.5 text-sm text-[#6B6B6B]">{agency.niche}</p>
              )}
            </div>
            <button
              onClick={onClose}
              className="shrink-0 flex h-8 w-8 items-center justify-center rounded-full border-2 border-[#D7D3CB] text-[#6B6B6B] transition-colors hover:border-[#10100F] hover:text-[#000000]"
              aria-label="Close"
            >
              ✕
            </button>
          </div>

          {/* Stack score summary */}
          <div className="mb-5 rounded-[23px] border-2 border-[#D7D3CB] bg-[#F6F6F3] p-5">
            <div className="flex items-center justify-between gap-4">
              <div>
                <p className="text-xs font-semibold uppercase tracking-widest text-[#6B6B6B]">Stack score</p>
                <div className="mt-1.5 flex items-center gap-3">
                  {score.totalScore != null && (
                    <span className="text-3xl font-bold tabular-nums text-[#000000]">
                      {score.totalScore}
                    </span>
                  )}
                  {score.tier && (
                    <span className={[
                      "inline-block rounded-full px-3 py-1 text-xs font-bold tier-" + score.tier,
                    ].join(" ")}>
                      {score.tier}
                    </span>
                  )}
                </div>
              </div>
              <div className="text-right">
                <span className={[
                  "inline-flex items-center gap-1.5 rounded-full border px-3 py-1 text-xs font-semibold",
                  verificationLabel === "GITHUB_VERIFIED"
                    ? "border-[#2ECC71] bg-[#EBFFF2] text-[#279455]"
                    : verificationLabel === "SELF_REPORTED"
                    ? "border-gray-200 bg-gray-50 text-gray-500"
                    : "border-[#D7D3CB] bg-[#F6F6F3] text-[#6B6B6B]",
                ].join(" ")}>
                  <span className={[
                    "h-1.5 w-1.5 rounded-full",
                    verificationLabel === "GITHUB_VERIFIED" ? "bg-[#2ECC71]"
                    : verificationLabel === "SELF_REPORTED" ? "bg-gray-400"
                    : "bg-gray-300",
                  ].join(" ")} />
                  {verificationLabel === "GITHUB_VERIFIED" ? "GitHub Verified"
                    : verificationLabel === "SELF_REPORTED" ? "Self-Reported"
                    : "Unverified"}
                </span>
                {lastScanDate && (
                  <p className="mt-1 text-[10px] text-[#6B6B6B]">
                    Scanned {formatDate(lastScanDate)}
                  </p>
                )}
              </div>
            </div>
          </div>

          {/* Architecture layer (V2) */}
          {archMeta && (
            <div className="mb-5 rounded-[23px] border-2 border-[#D7D3CB] bg-[#F6F6F3] p-5">
              <p className="text-xs font-semibold uppercase tracking-widest text-[#6B6B6B] mb-3">Architecture</p>
              <div className="flex flex-wrap gap-2">
                <span className={[
                  "rounded-full border px-3 py-1 text-xs font-semibold",
                  archMeta.badge,
                ].join(" ")}>
                  {archMeta.label}
                </span>
                {confMeta && (
                  <span className={["text-xs font-medium", confMeta.color].join(" ")}>
                    {confMeta.label}
                  </span>
                )}
                {sourceLabel && (
                  <span className="rounded-full border border-[#D7D3CB] bg-white px-3 py-1 text-[10px] text-[#6B6B6B]">
                    {sourceLabel}
                  </span>
                )}
                {archProfile?.lastVerifiedAt && (
                  <span className="rounded-full border border-[#D7D3CB] bg-white px-3 py-1 text-[10px] text-[#6B6B6B]">
                    Verified {formatDate(archProfile.lastVerifiedAt)}
                  </span>
                )}
              </div>
            </div>
          )}

          {/* Category evidence grid */}
          {categories.length > 0 && (
            <div className="mb-5">
              <p className="mb-3 text-xs font-semibold uppercase tracking-widest text-[#6B6B6B]">
                Evidence — {verifiedCount} of {categories.length} capabilities verified
              </p>
              <div className="grid gap-2 sm:grid-cols-2">
                {categories.map((c) => {
                  const grade = gradeFromScore(c.points, c.cap)
                  return (
                    <div
                      key={c.category}
                      className="flex items-center justify-between rounded-xl border-2 border-[#D7D3CB] bg-white p-3"
                    >
                      <div className="min-w-0">
                        <p className="text-xs font-semibold text-[#000000]">{c.category}</p>
                        <p className="text-[10px] text-[#6B6B6B]">{c.points}/{c.cap} pts</p>
                      </div>
                      <EvidenceBadge grade={grade} />
                    </div>
                  )
                })}
              </div>
            </div>
          )}

          {/* Detected packages */}
          {stack.length > 0 && (
            <div className="mb-5">
              <p className="mb-3 text-xs font-semibold uppercase tracking-widest text-[#6B6B6B]">
                Detected packages ({stack.length})
              </p>
              <div className="flex flex-wrap gap-1.5">
                {stack.map((pkg) => (
                  <span
                    key={pkg.packageName}
                    className="rounded-full border border-[#D7D3CB] bg-[#F6F6F3] px-2.5 py-1 text-[10px] font-medium text-[#000000]"
                  >
                    {pkg.packageName}
                  </span>
                ))}
              </div>
            </div>
          )}

          {/* Disclaimer */}
          <p className="text-[10px] text-[#6B6B6B] border-t border-[#D7D3CB] pt-4">
            This proof is based on manifest analysis and available architecture signals.
            SyncScore V2 verifies architecture evidence — not live system behaviour or performance.
            Ruleset: {score.rulesetVersion ?? "syncscore_v1"}
          </p>
        </div>
      </div>

      <style jsx global>{`
        @keyframes slideUp {
          from { transform: translateY(20px); opacity: 0; }
          to   { transform: translateY(0);    opacity: 1; }
        }
        @media (min-width: 768px) {
          @keyframes slideUp {
            from { transform: translate(-50%, calc(-50% + 16px)); opacity: 0; }
            to   { transform: translate(-50%, -50%);               opacity: 1; }
          }
        }
      `}</style>
    </>
  )
}

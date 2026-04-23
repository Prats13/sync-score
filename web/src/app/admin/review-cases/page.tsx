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
  })
}

export default function AdminReviewCasesPage() {
  const [cases, setCases] = useState<ReviewCaseResponse[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const refresh = async () => {
    setIsLoading(true)
    setError(null)
    try {
      const rows = await architectureApi.adminListOpenReviewCases()
      setCases(rows)
    } catch (e) {
      setError(e instanceof Error ? e.message : "Failed to load review cases")
    } finally {
      setIsLoading(false)
    }
  }

  useEffect(() => {
    refresh()
  }, [])

  const resolve = async (caseId: string, action: "APPROVE" | "DISMISS") => {
    try {
      await architectureApi.adminResolveReviewCase(caseId, action, "")
      await refresh()
    } catch (e) {
      setError(e instanceof Error ? e.message : "Resolve failed")
    }
  }

  return (
    <div className="mx-auto max-w-4xl px-6 py-12">
      <div className="mb-8 flex items-start justify-between gap-4">
        <div>
          <h1 className="text-3xl text-[#000000]" style={{ fontFamily: "var(--font-dm-serif-display)" }}>
            Review cases
          </h1>
          <p className="mt-1 text-sm text-[#6B6B6B]">
            Anti-gaming flags that require an admin decision.
          </p>
        </div>
        <Button size="sm" variant="outline" onClick={refresh} disabled={isLoading}>
          {isLoading ? "Refreshing…" : "Refresh"}
        </Button>
      </div>

      {error && (
        <div className="mb-6 rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-600">
          {error}
        </div>
      )}

      {isLoading ? (
        <div className="space-y-3">
          {Array.from({ length: 3 }).map((_, i) => (
            <div key={i} className="h-20 animate-pulse rounded-[23px] bg-[#F6F6F3]" />
          ))}
        </div>
      ) : cases.length === 0 ? (
        <div className="rounded-[23px] border-2 border-[#D7D3CB] bg-[#F6F6F3] p-8 text-center text-[#6B6B6B]">
          No open review cases.
        </div>
      ) : (
        <div className="space-y-3">
          {cases.map((c) => (
            <div key={c.id} className="rounded-[23px] border-2 border-[#D7D3CB] bg-[#F6F6F3] p-5">
              <div className="flex flex-wrap items-start justify-between gap-3">
                <div className="min-w-0">
                  <p className="text-sm font-semibold text-[#000000]">{c.triggerReason}</p>
                  <p className="mt-1 text-xs text-[#6B6B6B]">
                    Agency: {c.agencyId} · Created: {formatDate(c.createdAt)}
                  </p>
                </div>
                <div className="flex gap-2">
                  <Button size="sm" className="rounded-full bg-[#10100F] text-white hover:bg-[#10100F]/80" onClick={() => resolve(c.id, "APPROVE")}>
                    Approve
                  </Button>
                  <Button size="sm" variant="outline" onClick={() => resolve(c.id, "DISMISS")}>
                    Dismiss
                  </Button>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}


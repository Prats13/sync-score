"use client"

import { useState, useEffect } from "react"
import { agencyApi } from "@/lib/api/agency"
import { ApiError } from "@/lib/api/client"
import type { AgencyResponse, AgencyDashboardResponse } from "@/lib/types"

export function useAgency() {
  const [agency, setAgency] = useState<AgencyResponse | null>(null)
  const [dashboard, setDashboard] = useState<AgencyDashboardResponse | null>(null)
  const [notFound, setNotFound] = useState(false)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const refresh = async () => {
    setIsLoading(true)
    setError(null)
    try {
      const a = await agencyApi.me()
      setAgency(a)
      setNotFound(false)
      const d = await agencyApi.dashboard(a.agencyId)
      setDashboard(d)
    } catch (err) {
      if (err instanceof ApiError && err.status === 404) {
        setNotFound(true)
      } else {
        setError(err instanceof Error ? err.message : "Failed to load agency")
      }
    } finally {
      setIsLoading(false)
    }
  }

  useEffect(() => {
    refresh()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  return { agency, dashboard, notFound, isLoading, error, refresh }
}

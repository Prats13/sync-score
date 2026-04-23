"use client"

import { useState, useEffect, useRef } from "react"
import { scansApi } from "@/lib/api/scans"
import type { ScanStatusResponse } from "@/lib/types"

const TERMINAL_STATUSES = ["SUCCEEDED", "FAILED"] as const

export function useScanPoller(scanId: string | null) {
  const [scan, setScan] = useState<ScanStatusResponse | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const intervalRef = useRef<ReturnType<typeof setInterval> | null>(null)

  useEffect(() => {
    if (!scanId) return

    const poll = async () => {
      try {
        const data = await scansApi.get(scanId)
        setScan(data)
        setIsLoading(false)

        if (TERMINAL_STATUSES.includes(data.status as (typeof TERMINAL_STATUSES)[number])) {
          if (intervalRef.current) {
            clearInterval(intervalRef.current)
            intervalRef.current = null
          }
        }
      } catch (err) {
        setError(err instanceof Error ? err.message : "Failed to load scan")
        setIsLoading(false)
        if (intervalRef.current) {
          clearInterval(intervalRef.current)
          intervalRef.current = null
        }
      }
    }

    poll()
    intervalRef.current = setInterval(poll, 2000)

    return () => {
      if (intervalRef.current) {
        clearInterval(intervalRef.current)
        intervalRef.current = null
      }
    }
  }, [scanId])

  const isTerminal =
    scan !== null &&
    TERMINAL_STATUSES.includes(scan.status as (typeof TERMINAL_STATUSES)[number])

  return { scan, isLoading, isTerminal, error }
}

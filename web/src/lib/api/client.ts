import { TokenPair } from "@/lib/types"

// Empty string → requests go through Next.js rewrites (proxied to localhost:8080)
// Set NEXT_PUBLIC_API_URL in production to point at the real backend
const BASE_URL = process.env.NEXT_PUBLIC_API_URL ?? ""

// Stored in module-level so the refresh lock is shared
let isRefreshing = false
let refreshQueue: Array<(token: string) => void> = []

function getTokens(): { accessToken: string | null; refreshToken: string | null } {
  if (typeof window === "undefined") return { accessToken: null, refreshToken: null }
  return {
    accessToken: localStorage.getItem("accessToken"),
    refreshToken: localStorage.getItem("refreshToken"),
  }
}

function setTokens(pair: TokenPair) {
  localStorage.setItem("accessToken", pair.accessToken)
  localStorage.setItem("refreshToken", pair.refreshToken)
}

function clearTokens() {
  localStorage.removeItem("accessToken")
  localStorage.removeItem("refreshToken")
}

async function attemptRefresh(): Promise<string | null> {
  const { refreshToken } = getTokens()
  if (!refreshToken) return null
  try {
    const res = await fetch(`${BASE_URL}/auth/token/refresh`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ refreshToken }),
    })
    if (!res.ok) return null
    const pair: TokenPair = await res.json()
    setTokens(pair)
    return pair.accessToken
  } catch {
    return null
  }
}

export class ApiError extends Error {
  constructor(
    public readonly status: number,
    message: string,
    public readonly data?: Record<string, unknown>,
  ) {
    super(message)
    this.name = "ApiError"
  }
}

export async function apiFetch<T>(
  path: string,
  options: RequestInit & { signupToken?: string; noAuth?: boolean } = {},
): Promise<T> {
  const { signupToken, noAuth, ...fetchOptions } = options
  const { accessToken } = getTokens()

  const headers: Record<string, string> = {
    "Content-Type": "application/json",
    ...(fetchOptions.headers as Record<string, string> | undefined),
  }

  if (signupToken) {
    headers["Authorization"] = `Bearer ${signupToken}`
  } else if (!noAuth && accessToken) {
    headers["Authorization"] = `Bearer ${accessToken}`
  }

  const url = path.startsWith("http") ? path : `${BASE_URL}${path}`
  let res = await fetch(url, { ...fetchOptions, headers })

  // Auto-refresh on 401
  if (res.status === 401 && !noAuth && !signupToken) {
    if (!isRefreshing) {
      isRefreshing = true
      const newToken = await attemptRefresh()
      isRefreshing = false

      if (!newToken) {
        clearTokens()
        if (typeof window !== "undefined") {
          window.location.href = "/auth/login"
        }
        throw new ApiError(401, "Session expired")
      }

      // Drain the queue
      refreshQueue.forEach((cb) => cb(newToken))
      refreshQueue = []

      // Retry with new token
      headers["Authorization"] = `Bearer ${newToken}`
      res = await fetch(url, { ...fetchOptions, headers })
    } else {
      // Another call is already refreshing — queue and wait
      const newToken = await new Promise<string>((resolve) => {
        refreshQueue.push(resolve)
      })
      headers["Authorization"] = `Bearer ${newToken}`
      res = await fetch(url, { ...fetchOptions, headers })
    }
  }

  if (!res.ok) {
    let message = `HTTP ${res.status}`
    let data: Record<string, unknown> | undefined
    try {
      data = await res.json()
      message = (data?.message ?? data?.error ?? message) as string
    } catch {
      // ignore JSON parse failure
    }
    throw new ApiError(res.status, message, data)
  }

  // 204 No Content or empty body (Spring void endpoints return 200 with no body)
  if (res.status === 204) return undefined as T
  const text = await res.text()
  if (!text) return undefined as T
  return JSON.parse(text) as T
}

export { getTokens, setTokens, clearTokens }

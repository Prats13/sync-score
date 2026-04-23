"use client"

import React, { createContext, useContext, useEffect, useState, useCallback } from "react"
import { setTokens, clearTokens, getTokens } from "./api/client"
import { authApi } from "./api/auth"
import type { TokenPair, MeResponse } from "./types"

interface AuthState {
  user: MeResponse | null
  accessToken: string | null
  isAuthenticated: boolean
  isLoading: boolean
}

interface AuthContextValue extends AuthState {
  login: (pair: TokenPair) => Promise<void>
  logout: () => Promise<void>
}

const AuthContext = createContext<AuthContextValue | null>(null)

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [state, setState] = useState<AuthState>({
    user: null,
    accessToken: null,
    isAuthenticated: false,
    isLoading: true,
  })

  // Hydrate from localStorage on mount
  useEffect(() => {
    const { accessToken } = getTokens()
    if (accessToken) {
      authApi
        .me()
        .then((user) => {
          setState({
            user,
            accessToken,
            isAuthenticated: true,
            isLoading: false,
          })
        })
        .catch(() => {
          // Token invalid / expired — clear and remain logged out
          clearTokens()
          setState({ user: null, accessToken: null, isAuthenticated: false, isLoading: false })
        })
    } else {
      setState((s) => ({ ...s, isLoading: false }))
    }
  }, [])

  const login = useCallback(async (pair: TokenPair) => {
    setTokens(pair)
    const user = await authApi.me()
    setState({
      user,
      accessToken: pair.accessToken,
      isAuthenticated: true,
      isLoading: false,
    })
  }, [])

  const logout = useCallback(async () => {
    try {
      await authApi.logout()
    } catch {
      // Best-effort
    }
    clearTokens()
    setState({ user: null, accessToken: null, isAuthenticated: false, isLoading: false })
    window.location.href = "/auth/login"
  }, [])

  return (
    <AuthContext.Provider value={{ ...state, login, logout }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error("useAuth must be used within AuthProvider")
  return ctx
}

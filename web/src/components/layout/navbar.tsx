"use client"

import Link from "next/link"
import { useAuth } from "@/lib/hooks/use-auth"
import { useState, useEffect } from "react"
import { Button } from "@/components/ui/button"

export function Navbar() {
  const { isAuthenticated, user, logout, isLoading } = useAuth()
  const [theme, setTheme] = useState("light")

  useEffect(() => {
    const saved = localStorage.getItem("ss-theme") || "light"
    setTheme(saved)
    document.documentElement.setAttribute("data-theme", saved)
  }, [])

  const toggleTheme = () => {
    const next = theme === "light" ? "dark" : "light"
    setTheme(next)
    localStorage.setItem("ss-theme", next)
    document.documentElement.setAttribute("data-theme", next)
  }

  const moonPath = <path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"/>
  const sunPath = (
    <>
      <circle cx="12" cy="12" r="4"/>
      <path d="M12 2v2M12 20v2M4.9 4.9l1.4 1.4M17.7 17.7l1.4 1.4M2 12h2M20 12h2M4.9 19.1l1.4-1.4M17.7 6.3l1.4-1.4"/>
    </>
  )

  return (
    <header className="sticky top-0 z-50 flex items-center justify-between gap-4 px-8 py-3.5 border-b border-hairline-strong bg-bg/80 backdrop-blur-md backdrop-saturate-150">
      <div className="flex items-center gap-6">
        <Link href="/" className="flex items-center gap-2.5 text-[15px] font-semibold tracking-[-0.01em] text-ink">
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none">
            <circle cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="1.25" opacity="0.3"/>
            <circle cx="12" cy="12" r="6" stroke="currentColor" strokeWidth="1.25" opacity="0.55"/>
            <circle cx="12" cy="12" r="2.2" fill="currentColor"/>
          </svg>
          SyncScore
          <span className="font-mono text-[10px] px-1.5 py-0.5 rounded-full border border-trust-border text-trust-text bg-trust-bg ml-1">
            V2
          </span>
        </Link>
        
        <nav className="hidden items-center gap-5 text-[13px] font-medium text-muted sm:flex ml-4">
          <Link href="/browse" className="transition-colors hover:text-ink">
            Browse
          </Link>
          <Link href="/for-business" className="transition-colors hover:text-ink">
            For business
          </Link>
        </nav>
      </div>

      <div className="flex items-center gap-3">
        {isLoading ? (
          <div className="h-8 w-20 animate-pulse rounded-md bg-hairline-strong" />
        ) : isAuthenticated ? (
          <>
            <span className="hidden text-xs text-muted sm:block px-2">{user?.username}</span>
            <Link href="/dashboard" className="text-[13px] font-medium transition-colors hover:text-trust">Dashboard</Link>
            <Link href="/admin/review-cases" className="text-[13px] font-medium text-muted hover:text-trust ml-2">Admin</Link>
            <button
              onClick={logout}
              className="text-[13px] font-medium text-muted hover:text-trust ml-2 px-2"
            >
              Log out
            </button>
          </>
        ) : (
          <>
            <Link href="/auth/login" className="text-[13px] font-medium text-muted hover:text-trust transition-colors">Log in</Link>
            <Link href="/auth/signup" className="text-[13px] font-medium bg-trust text-bg px-4 py-1.5 rounded-full hover:opacity-80 transition-opacity ml-1">
              Test Agent
            </Link>
          </>
        )}
        
        <div className="w-[1px] h-4 bg-hairline-strong mx-1" />

        <button 
          onClick={toggleTheme} 
          className="w-8 h-8 rounded-full border border-hairline-strong bg-surface-2 flex items-center justify-center text-ink transition-all hover:border-trust-border"
          aria-label="Toggle theme"
        >
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.6">
            {theme === "light" ? moonPath : sunPath}
          </svg>
        </button>
      </div>
    </header>
  )
}

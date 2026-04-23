"use client"

import Link from "next/link"
import { useAuth } from "@/lib/hooks/use-auth"
import { Button } from "@/components/ui/button"

export function Navbar() {
  const { isAuthenticated, user, logout, isLoading } = useAuth()

  return (
    <header className="sticky top-0 z-50 w-full border-b border-[#D7D3CB] bg-[#F7F6F2]/80 backdrop-blur-sm">
      <div className="mx-auto flex h-14 max-w-6xl items-center justify-between px-6">
        {/* Logo */}
        <Link href="/" className="text-base font-semibold tracking-tight text-[#000000]">
          SyncScore
        </Link>

        {/* Nav links */}
        <nav className="hidden items-center gap-6 text-sm text-[#6B6B6B] sm:flex">
          <Link href="/browse" className="transition-colors hover:text-[#000000]">
            Browse
          </Link>
          <Link href="/translate" className="transition-colors hover:text-[#000000]">
            Find an agency
          </Link>
          <Link href="/how-it-works" className="transition-colors hover:text-[#000000]">
            How it works
          </Link>
          <Link href="/for-business" className="transition-colors hover:text-[#000000]">
            For business
          </Link>
        </nav>

        {/* Auth actions */}
        <div className="flex items-center gap-2">
          {isLoading ? (
            <div className="h-8 w-20 animate-pulse rounded-md bg-[#D7D3CB]" />
          ) : isAuthenticated ? (
            <>
              <span className="hidden text-sm text-[#6B6B6B] sm:block">{user?.username}</span>
              <Button asChild size="sm" variant="outline">
                <Link href="/dashboard">Dashboard</Link>
              </Button>
              <Button asChild size="sm" variant="ghost">
                <Link href="/admin/review-cases" className="text-[#6B6B6B] hover:text-[#000000]">
                  Admin
                </Link>
              </Button>
              <Button
                size="sm"
                variant="ghost"
                onClick={logout}
                className="text-[#6B6B6B] hover:text-[#000000]"
              >
                Log out
              </Button>
            </>
          ) : (
            <>
              <Button asChild size="sm" variant="ghost">
                <Link href="/auth/login">Log in</Link>
              </Button>
              <Button
                asChild
                size="sm"
                className="rounded-full bg-[#10100F] text-white hover:bg-[#10100F]/80"
              >
                <Link href="/auth/signup">Get Started</Link>
              </Button>
            </>
          )}
        </div>
      </div>
    </header>
  )
}

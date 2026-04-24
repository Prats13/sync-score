import Link from "next/link"

export function Footer() {
  return (
    <footer className="border-t border-hairline-strong bg-bg">
      <div className="mx-auto flex max-w-6xl items-center justify-between px-6 py-8 text-sm text-muted">
        <span>© {new Date().getFullYear()} SyncScore</span>
        <nav className="flex gap-6">
          <Link href="/browse" className="hover:text-ink">Browse</Link>
          <Link href="/how-it-works" className="hover:text-ink">How it works</Link>
          <Link href="/for-business" className="hover:text-ink">For business</Link>
        </nav>
      </div>
    </footer>
  )
}

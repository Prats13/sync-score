import Link from "next/link"

export function Footer() {
  return (
    <footer className="border-t border-[#D7D3CB] bg-[#F7F6F2]">
      <div className="mx-auto flex max-w-6xl items-center justify-between px-6 py-8 text-sm text-[#6B6B6B]">
        <span>© {new Date().getFullYear()} SyncScore</span>
        <nav className="flex gap-6">
          <Link href="/browse" className="hover:text-[#000000]">Browse</Link>
          <Link href="/how-it-works" className="hover:text-[#000000]">How it works</Link>
          <Link href="/for-business" className="hover:text-[#000000]">For business</Link>
        </nav>
      </div>
    </footer>
  )
}

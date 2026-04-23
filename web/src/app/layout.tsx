import type { Metadata } from "next"
import { Inter, DM_Serif_Text, DM_Serif_Display } from "next/font/google"
import "./globals.css"
import { AuthProvider } from "@/lib/auth-context"
import { Navbar } from "@/components/layout/navbar"
import { Footer } from "@/components/layout/footer"

const inter = Inter({
  variable: "--font-inter",
  subsets: ["latin"],
  display: "swap",
})

const dmSerifText = DM_Serif_Text({
  variable: "--font-dm-serif-text",
  weight: "400",
  subsets: ["latin"],
  display: "swap",
})

const dmSerifDisplay = DM_Serif_Display({
  variable: "--font-dm-serif-display",
  weight: "400",
  subsets: ["latin"],
  display: "swap",
})

export const metadata: Metadata = {
  title: "SyncScore — Setting the standard for AI agent trust",
  description:
    "SyncScore audits and certifies AI agent architecture, so businesses deploy proof, not promises.",
}

export default function RootLayout({
  children,
}: {
  children: React.ReactNode
}) {
  return (
    <html
      lang="en"
      className={`${inter.variable} ${dmSerifText.variable} ${dmSerifDisplay.variable} h-full`}
    >
      <body className="flex min-h-full flex-col bg-[#F7F6F2] font-[family-name:var(--font-inter)] antialiased">
        <AuthProvider>
          <Navbar />
          <main className="flex-1">{children}</main>
          <Footer />
        </AuthProvider>
      </body>
    </html>
  )
}

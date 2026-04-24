"use client"

import { useEffect, useState } from "react"
import { useForm } from "react-hook-form"
import { zodResolver } from "@hookform/resolvers/zod"
import { z } from "zod"
import { useRouter } from "next/navigation"
import { useAuth } from "@/lib/hooks/use-auth"
import { useAgency } from "@/lib/hooks/use-agency"
import { verificationApi } from "@/lib/api/verification"
import { ApiError } from "@/lib/api/client"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { TrustLabel } from "@/components/ui/trust-label"
import { CompleteProfileModal } from "@/components/ui/complete-profile-modal"

const githubSchema = z.object({
  githubUsername: z.string().min(1, "GitHub username is required"),
})

const pasteSchema = z.object({
  content: z.string().min(10, "Paste your package manifest content"),
  manifestType: z.string().optional(),
})

type GithubForm = z.infer<typeof githubSchema>
type PasteForm = z.infer<typeof pasteSchema>

const MANIFEST_TYPES = [
  { value: "package.json", label: "package.json (Node.js)" },
  { value: "requirements.txt", label: "requirements.txt (Python)" },
  { value: "pyproject.toml", label: "pyproject.toml (Python)" },
  { value: "Cargo.toml", label: "Cargo.toml (Rust)" },
  { value: "go.mod", label: "go.mod (Go)" },
]

export default function VerifyPage() {
  const router = useRouter()
  const { isAuthenticated, isLoading: authLoading } = useAuth()
  const { notFound, isLoading: agencyLoading, refresh } = useAgency()

  useEffect(() => {
    if (!authLoading && !isAuthenticated) {
      router.push("/auth/login")
    }
  }, [authLoading, isAuthenticated, router])

  const [githubError, setGithubError] = useState<string | null>(null)
  const [pasteError, setPasteError] = useState<string | null>(null)

  const githubForm = useForm<GithubForm>({ resolver: zodResolver(githubSchema) })
  const pasteForm = useForm<PasteForm>({ resolver: zodResolver(pasteSchema) })

  const onGithubSubmit = async (data: GithubForm) => {
    setGithubError(null)
    try {
      const { scanId } = await verificationApi.github(data)
      router.push(`/dashboard/scan/${scanId}`)
    } catch (err) {
      setGithubError(err instanceof ApiError ? err.message : "Failed to start scan.")
    }
  }

  const onPasteSubmit = async (data: PasteForm) => {
    setPasteError(null)
    try {
      const { scanId } = await verificationApi.paste(data)
      router.push(`/dashboard/scan/${scanId}`)
    } catch (err) {
      setPasteError(err instanceof ApiError ? err.message : "Failed to start scan.")
    }
  }

  if (authLoading || agencyLoading) {
    return <div className="mx-auto max-w-2xl px-6 py-12" />
  }

  return (
    <>
      <CompleteProfileModal
        open={notFound}
        onComplete={() => refresh()}
      />
      
      <div className="mx-auto max-w-2xl px-6 py-12">
        <div className="mb-10">
          <h1
            className="text-3xl text-ink"
            style={{ fontFamily: "var(--font-display)" }}
          >
            Verify your stack
          </h1>
          <p className="mt-2 text-muted">
            Choose how you want to submit your agent&apos;s dependencies for verification.
          </p>
        </div>

        <Tabs defaultValue="github">
          <TabsList className="mb-6 border-2 border-hairline-strong bg-surface-inset">
            <TabsTrigger value="github" className="data-[state=active]:bg-surface-1">
              <span className="flex items-center gap-2">
                GitHub scan
                <TrustLabel label="GITHUB_VERIFIED" className="text-[10px]" />
              </span>
            </TabsTrigger>
            <TabsTrigger value="paste" className="data-[state=active]:bg-surface-1">
              <span className="flex items-center gap-2">
                Paste manifest
                <TrustLabel label="SELF_REPORTED" className="text-[10px]" />
              </span>
            </TabsTrigger>
          </TabsList>

          {/* GitHub tab */}
          <TabsContent value="github">
            <div className="rounded-[23px] border-2 border-hairline-strong bg-surface-inset p-8">
              <p className="mb-6 text-sm leading-relaxed text-muted">
                Enter your GitHub username. We&apos;ll scan your public repositories for
                package manifests and evaluate your stack. This results in a{" "}
                <span className="font-medium text-verified">GITHUB VERIFIED</span> badge.
              </p>
              <form onSubmit={githubForm.handleSubmit(onGithubSubmit)} className="space-y-4">
                <div className="space-y-1.5">
                  <Label htmlFor="githubUsername">GitHub username</Label>
                  <div className="flex items-center gap-2">
                    <span className="text-muted">github.com/</span>
                    <Input
                      id="githubUsername"
                      placeholder="your-username"
                      {...githubForm.register("githubUsername")}
                      className="border-hairline-strong bg-surface-1"
                    />
                  </div>
                  {githubForm.formState.errors.githubUsername && (
                    <p className="text-xs text-red-500">
                      {githubForm.formState.errors.githubUsername.message}
                    </p>
                  )}
                </div>
                {githubError && (
                  <div className="rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-600">
                    {githubError}
                  </div>
                )}
                <Button
                  type="submit"
                  disabled={githubForm.formState.isSubmitting}
                  className="w-full rounded-full bg-trust text-bg hover:bg-trust/80"
                >
                  {githubForm.formState.isSubmitting ? "Starting scan…" : "Scan GitHub"}
                </Button>
              </form>
            </div>
          </TabsContent>

          {/* Paste tab */}
          <TabsContent value="paste">
            <div className="rounded-[23px] border-2 border-hairline-strong bg-surface-inset p-8">
              <p className="mb-6 text-sm leading-relaxed text-muted">
                Paste your package manifest directly. This results in a{" "}
                <span className="font-medium text-muted">SELF-REPORTED</span> label — useful
                for testing or private repos.
              </p>
              <form onSubmit={pasteForm.handleSubmit(onPasteSubmit)} className="space-y-4">
                <div className="space-y-1.5">
                  <Label htmlFor="manifestType">Manifest type</Label>
                  <select
                    id="manifestType"
                    {...pasteForm.register("manifestType")}
                    className="w-full rounded-md border-2 border-hairline-strong bg-surface-1 px-3 py-2 text-sm text-ink focus:outline-none focus:ring-2 focus:ring-trust"
                  >
                    {MANIFEST_TYPES.map((t) => (
                      <option key={t.value} value={t.value}>
                        {t.label}
                      </option>
                    ))}
                  </select>
                </div>
                <div className="space-y-1.5">
                  <Label htmlFor="content">Manifest content</Label>
                  <textarea
                    id="content"
                    rows={12}
                    placeholder={'{\n  "dependencies": {\n    "langchain": "^0.3.0"\n  }\n}'}
                    {...pasteForm.register("content")}
                    className="w-full rounded-md border-2 border-hairline-strong bg-surface-1 px-3 py-2 font-mono text-xs text-ink focus:outline-none focus:ring-2 focus:ring-trust"
                  />
                  {pasteForm.formState.errors.content && (
                    <p className="text-xs text-red-500">{pasteForm.formState.errors.content.message}</p>
                  )}
                </div>
                {pasteError && (
                  <div className="rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-600">
                    {pasteError}
                  </div>
                )}
                <Button
                  type="submit"
                  disabled={pasteForm.formState.isSubmitting}
                  className="w-full rounded-full bg-trust text-bg hover:bg-trust/80"
                >
                  {pasteForm.formState.isSubmitting ? "Starting scan…" : "Analyze manifest"}
                </Button>
              </form>
            </div>
          </TabsContent>
        </Tabs>
      </div>
    </>
  )
}

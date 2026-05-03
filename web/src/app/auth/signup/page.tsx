"use client"

import { Suspense } from "react"
import { useState, useEffect } from "react"
import { useForm } from "react-hook-form"
import { zodResolver } from "@hookform/resolvers/zod"
import { z } from "zod"
import Link from "next/link"
import { useRouter, useSearchParams } from "next/navigation"
import { GoogleLogin } from "@react-oauth/google"
import { authApi } from "@/lib/api/auth"
import { ApiError } from "@/lib/api/client"
import { useAuth } from "@/lib/hooks/use-auth"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"

// ── Step schemas ──────────────────────────────────────────────────────────

const emailSchema = z.object({
  email: z.string().email("Enter a valid email"),
})

const otpSchema = z.object({
  otp: z.string().length(6, "Enter the 6-digit code"),
})

const completeSchema = z.object({
  username: z.string().min(3, "Minimum 3 characters"),
  password: z.string().min(8, "Minimum 8 characters"),
})

type EmailForm = z.infer<typeof emailSchema>
type OtpForm = z.infer<typeof otpSchema>
type CompleteForm = z.infer<typeof completeSchema>

type Step = "email" | "otp" | "complete"

export default function SignupPage() {
  return (
    <Suspense>
      <SignupContent />
    </Suspense>
  )
}

function SignupContent() {
  const router = useRouter()
  const searchParams = useSearchParams()
  const { login } = useAuth()

  const [step, setStep] = useState<Step>("email")
  const [email, setEmail] = useState("")
  const [signupToken, setSignupToken] = useState("")
  const [error, setError] = useState<string | null>(null)

  // If redirected from Google login with an incomplete signup token, jump to complete
  useEffect(() => {
    const token = searchParams.get("token")
    if (token) {
      setSignupToken(token)
      setStep("complete")
    }
  }, [searchParams])

  // ── Step 1: Email ────────────────────────────────────────────────────────
  const emailForm = useForm<EmailForm>({ resolver: zodResolver(emailSchema) })
  const onEmailSubmit = async (data: EmailForm) => {
    setError(null)
    try {
      await authApi.emailStart({ email: data.email })
      setEmail(data.email)
      setStep("otp")
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to send OTP.")
    }
  }

  // ── Step 2: OTP ──────────────────────────────────────────────────────────
  const otpForm = useForm<OtpForm>({ resolver: zodResolver(otpSchema) })
  const onOtpSubmit = async (data: OtpForm) => {
    setError(null)
    try {
      const res = await authApi.emailVerifyOtp({ email, otp: data.otp })
      setSignupToken(res.signupToken)
      setStep("complete")
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Invalid or expired code.")
    }
  }

  // ── Step 3: Complete ─────────────────────────────────────────────────────
  const completeForm = useForm<CompleteForm>({ resolver: zodResolver(completeSchema) })
  const onCompleteSubmit = async (data: CompleteForm) => {
    setError(null)
    try {
      const pair = await authApi.signupComplete(data, signupToken)
      await login(pair)
      router.push("/dashboard/verify")
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to complete signup.")
    }
  }

  // ── Google signup ─────────────────────────────────────────────────────────
  const onGoogleSuccess = async (credential: string) => {
    setError(null)
    try {
      const res = await authApi.signupGoogle({ idToken: credential })
      if ("signupToken" in res) {
        // Profile incomplete — go to username/password step
        setSignupToken(res.signupToken)
        setStep("complete")
      } else {
        // Already a complete account (shouldn't normally happen via /signup/google)
        await login(res)
        router.push("/dashboard")
      }
    } catch (err) {
      if (err instanceof ApiError && err.status === 409) {
        setError("This Google account is already registered. Please log in instead.")
      } else {
        setError(err instanceof ApiError ? err.message : "Google sign-in failed.")
      }
    }
  }

  // ── Step indicator ────────────────────────────────────────────────────────
  const steps = ["Email", "Verify", "Profile"]
  const currentIdx = step === "email" ? 0 : step === "otp" ? 1 : 2

  return (
    <div className="flex min-h-[70vh] items-center justify-center px-6 py-12">
      <div className="w-full max-w-sm">
        <div className="mb-8 text-center">
          <h1 className="text-4xl text-ink font-display">
            Create your account
          </h1>
          {/* Step pills */}
          <div className="mt-5 flex items-center justify-center gap-2">
            {steps.map((s, i) => (
              <div key={s} className="flex items-center gap-2">
                <span
                  className={[
                    "flex h-6 w-6 items-center justify-center rounded-full text-[11px] font-mono",
                    i <= currentIdx
                      ? "bg-trust text-bg"
                      : "border border-hairline-strong text-muted",
                  ].join(" ")}
                >
                  {i + 1}
                </span>
                <span className={`text-[12px] font-medium ${i === currentIdx ? "text-ink" : "text-muted"}`}>
                  {s}
                </span>
                {i < steps.length - 1 && (
                  <span className="mx-1 h-px w-6 bg-hairline-strong" />
                )}
              </div>
            ))}
          </div>
        </div>

        <div className="card-base p-8">
          {/* Step 1: Email (with Google option) */}
          {step === "email" && (
            <>
              <div className="flex justify-center">
                <GoogleLogin
                  onSuccess={(res) => res.credential && onGoogleSuccess(res.credential)}
                  onError={() => setError("Google sign-in failed.")}
                  theme="filled_black"
                  shape="pill"
                  text="signup_with"
                />
              </div>

              <div className="my-5 flex items-center gap-3">
                <span className="flex-1 h-px bg-hairline-strong" />
                <span className="text-[11px] text-muted">or</span>
                <span className="flex-1 h-px bg-hairline-strong" />
              </div>

              <form onSubmit={emailForm.handleSubmit(onEmailSubmit)} className="space-y-4">
                <div className="space-y-1.5">
                  <Label htmlFor="email" className="text-ink">Email address</Label>
                  <Input
                    id="email"
                    type="email"
                    autoComplete="email"
                    {...emailForm.register("email")}
                    className="border-hairline-strong bg-surface-1 text-ink focus-visible:ring-trust"
                  />
                  {emailForm.formState.errors.email && (
                    <p className="text-xs text-mismatch">{emailForm.formState.errors.email.message}</p>
                  )}
                </div>
                {error && (
                  <div className="rounded-lg border border-mismatch-bg bg-mismatch-bg/30 px-3 py-2 text-sm text-mismatch">
                    {error}
                  </div>
                )}
                <Button
                  type="submit"
                  disabled={emailForm.formState.isSubmitting}
                  className="w-full mt-2 rounded-full bg-trust text-bg hover:opacity-80 transition-opacity"
                >
                  {emailForm.formState.isSubmitting ? "Sending…" : "Send verification code"}
                </Button>
              </form>
            </>
          )}

          {/* Step 2: OTP */}
          {step === "otp" && (
            <form onSubmit={otpForm.handleSubmit(onOtpSubmit)} className="space-y-4">
              <p className="text-[13px] text-muted">
                We sent a 6-digit code to <span className="font-medium text-ink">{email}</span>
              </p>
              <div className="space-y-1.5 pt-2">
                <Label htmlFor="otp" className="text-ink">Verification code</Label>
                <Input
                  id="otp"
                  maxLength={6}
                  autoComplete="one-time-code"
                  inputMode="numeric"
                  {...otpForm.register("otp")}
                  className="border-hairline-strong bg-surface-1 text-ink text-center text-2xl tracking-[0.5em] font-mono h-12"
                />
                {otpForm.formState.errors.otp && (
                  <p className="text-xs text-mismatch">{otpForm.formState.errors.otp.message}</p>
                )}
              </div>
              {error && (
                <div className="rounded-lg border border-mismatch-bg bg-mismatch-bg/30 px-3 py-2 text-sm text-mismatch">
                  {error}
                </div>
              )}
              <Button
                type="submit"
                disabled={otpForm.formState.isSubmitting}
                className="w-full mt-2 rounded-full bg-trust text-bg hover:opacity-80 transition-opacity"
              >
                {otpForm.formState.isSubmitting ? "Verifying…" : "Verify code"}
              </Button>
              <button
                type="button"
                className="w-full mt-2 text-center text-[12px] text-muted underline-offset-2 hover:underline hover:text-ink transition-colors"
                onClick={() => setStep("email")}
              >
                Change email
              </button>
            </form>
          )}

          {/* Step 3: Complete */}
          {step === "complete" && (
            <form onSubmit={completeForm.handleSubmit(onCompleteSubmit)} className="space-y-4">
              <p className="text-[13px] text-muted">Choose a username and password to finish.</p>
              <div className="space-y-1.5 pt-2">
                <Label htmlFor="username" className="text-ink">Username</Label>
                <Input
                  id="username"
                  autoComplete="username"
                  {...completeForm.register("username")}
                  className="border-hairline-strong bg-surface-1 text-ink focus-visible:ring-trust"
                />
                {completeForm.formState.errors.username && (
                  <p className="text-xs text-mismatch">
                    {completeForm.formState.errors.username.message}
                  </p>
                )}
              </div>
              <div className="space-y-1.5">
                <Label htmlFor="pw" className="text-ink">Password</Label>
                <Input
                  id="pw"
                  type="password"
                  autoComplete="new-password"
                  {...completeForm.register("password")}
                  className="border-hairline-strong bg-surface-1 text-ink focus-visible:ring-trust"
                />
                {completeForm.formState.errors.password && (
                  <p className="text-xs text-mismatch">
                    {completeForm.formState.errors.password.message}
                  </p>
                )}
              </div>
              {error && (
                <div className="rounded-lg border border-mismatch-bg bg-mismatch-bg/30 px-3 py-2 text-sm text-mismatch">
                  {error}
                </div>
              )}
              <Button
                type="submit"
                disabled={completeForm.formState.isSubmitting}
                className="w-full mt-2 rounded-full bg-trust text-bg hover:opacity-80 transition-opacity"
              >
                {completeForm.formState.isSubmitting ? "Creating account…" : "Create account"}
              </Button>
            </form>
          )}
        </div>

        <p className="mt-6 text-center text-[13px] text-muted">
          Already have an account?{" "}
          <Link href="/auth/login" className="font-medium text-ink underline-offset-2 hover:underline">
            Log in
          </Link>
        </p>
      </div>
    </div>
  )
}
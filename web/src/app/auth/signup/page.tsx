"use client"

import { useState } from "react"
import { useForm } from "react-hook-form"
import { zodResolver } from "@hookform/resolvers/zod"
import { z } from "zod"
import Link from "next/link"
import { useRouter } from "next/navigation"
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
  const router = useRouter()
  const { login } = useAuth()

  const [step, setStep] = useState<Step>("email")
  const [email, setEmail] = useState("")
  const [signupToken, setSignupToken] = useState("")
  const [error, setError] = useState<string | null>(null)

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

  // ── Step indicator ────────────────────────────────────────────────────────
  const steps = ["Email", "Verify", "Profile"]
  const currentIdx = step === "email" ? 0 : step === "otp" ? 1 : 2

  return (
    <div className="flex min-h-[70vh] items-center justify-center px-6 py-12">
      <div className="w-full max-w-sm">
        <div className="mb-8 text-center">
          <h1
            className="text-3xl text-[#000000]"
            style={{ fontFamily: "var(--font-dm-serif-display)" }}
          >
            Create your account
          </h1>
          {/* Step pills */}
          <div className="mt-5 flex items-center justify-center gap-2">
            {steps.map((s, i) => (
              <div key={s} className="flex items-center gap-2">
                <span
                  className={[
                    "flex h-6 w-6 items-center justify-center rounded-full text-xs font-semibold",
                    i <= currentIdx
                      ? "bg-[#10100F] text-white"
                      : "border-2 border-[#D7D3CB] text-[#6B6B6B]",
                  ].join(" ")}
                >
                  {i + 1}
                </span>
                <span className={`text-xs ${i === currentIdx ? "font-medium text-[#000000]" : "text-[#6B6B6B]"}`}>
                  {s}
                </span>
                {i < steps.length - 1 && (
                  <span className="mx-1 h-px w-6 bg-[#D7D3CB]" />
                )}
              </div>
            ))}
          </div>
        </div>

        <div className="rounded-[23px] border-2 border-[#D7D3CB] bg-[#F6F6F3] p-8">
          {/* Step 1: Email */}
          {step === "email" && (
            <form onSubmit={emailForm.handleSubmit(onEmailSubmit)} className="space-y-4">
              <div className="space-y-1.5">
                <Label htmlFor="email">Email address</Label>
                <Input
                  id="email"
                  type="email"
                  autoComplete="email"
                  {...emailForm.register("email")}
                  className="border-[#D7D3CB] bg-white"
                />
                {emailForm.formState.errors.email && (
                  <p className="text-xs text-red-500">{emailForm.formState.errors.email.message}</p>
                )}
              </div>
              {error && (
                <div className="rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-600">
                  {error}
                </div>
              )}
              <Button
                type="submit"
                disabled={emailForm.formState.isSubmitting}
                className="w-full rounded-full bg-[#10100F] text-white hover:bg-[#10100F]/80"
              >
                {emailForm.formState.isSubmitting ? "Sending…" : "Send verification code"}
              </Button>
            </form>
          )}

          {/* Step 2: OTP */}
          {step === "otp" && (
            <form onSubmit={otpForm.handleSubmit(onOtpSubmit)} className="space-y-4">
              <p className="text-sm text-[#6B6B6B]">
                We sent a 6-digit code to <span className="font-medium text-[#000000]">{email}</span>
              </p>
              <div className="space-y-1.5">
                <Label htmlFor="otp">Verification code</Label>
                <Input
                  id="otp"
                  maxLength={6}
                  autoComplete="one-time-code"
                  inputMode="numeric"
                  {...otpForm.register("otp")}
                  className="border-[#D7D3CB] bg-white text-center text-2xl tracking-[0.5em]"
                />
                {otpForm.formState.errors.otp && (
                  <p className="text-xs text-red-500">{otpForm.formState.errors.otp.message}</p>
                )}
              </div>
              {error && (
                <div className="rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-600">
                  {error}
                </div>
              )}
              <Button
                type="submit"
                disabled={otpForm.formState.isSubmitting}
                className="w-full rounded-full bg-[#10100F] text-white hover:bg-[#10100F]/80"
              >
                {otpForm.formState.isSubmitting ? "Verifying…" : "Verify code"}
              </Button>
              <button
                type="button"
                className="w-full text-center text-xs text-[#6B6B6B] underline-offset-2 hover:underline"
                onClick={() => setStep("email")}
              >
                Change email
              </button>
            </form>
          )}

          {/* Step 3: Complete */}
          {step === "complete" && (
            <form onSubmit={completeForm.handleSubmit(onCompleteSubmit)} className="space-y-4">
              <p className="text-sm text-[#6B6B6B]">Choose a username and password to finish.</p>
              <div className="space-y-1.5">
                <Label htmlFor="username">Username</Label>
                <Input
                  id="username"
                  autoComplete="username"
                  {...completeForm.register("username")}
                  className="border-[#D7D3CB] bg-white"
                />
                {completeForm.formState.errors.username && (
                  <p className="text-xs text-red-500">
                    {completeForm.formState.errors.username.message}
                  </p>
                )}
              </div>
              <div className="space-y-1.5">
                <Label htmlFor="pw">Password</Label>
                <Input
                  id="pw"
                  type="password"
                  autoComplete="new-password"
                  {...completeForm.register("password")}
                  className="border-[#D7D3CB] bg-white"
                />
                {completeForm.formState.errors.password && (
                  <p className="text-xs text-red-500">
                    {completeForm.formState.errors.password.message}
                  </p>
                )}
              </div>
              {error && (
                <div className="rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-600">
                  {error}
                </div>
              )}
              <Button
                type="submit"
                disabled={completeForm.formState.isSubmitting}
                className="w-full rounded-full bg-[#10100F] text-white hover:bg-[#10100F]/80"
              >
                {completeForm.formState.isSubmitting ? "Creating account…" : "Create account"}
              </Button>
            </form>
          )}
        </div>

        <p className="mt-6 text-center text-sm text-[#6B6B6B]">
          Already have an account?{" "}
          <Link href="/auth/login" className="font-medium text-[#000000] underline-offset-2 hover:underline">
            Log in
          </Link>
        </p>
      </div>
    </div>
  )
}

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

const schema = z.object({
  username: z.string().min(1, "Username is required"),
  password: z.string().min(1, "Password is required"),
})

type FormData = z.infer<typeof schema>

export default function LoginPage() {
  const router = useRouter()
  const { login } = useAuth()
  const [error, setError] = useState<string | null>(null)

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<FormData>({ resolver: zodResolver(schema) })

  const onSubmit = async (data: FormData) => {
    setError(null)
    try {
      const pair = await authApi.login(data)
      await login(pair)
      router.push("/dashboard")
    } catch (err) {
      if (err instanceof ApiError) {
        if (err.status === 401) {
          setError("Invalid username or password.")
        } else {
          setError(err.message)
        }
      } else {
        setError("Something went wrong. Please try again.")
      }
    }
  }

  return (
    <div className="flex min-h-[70vh] items-center justify-center px-6">
      <div className="w-full max-w-sm">
        <div className="mb-8 text-center">
          <h1 className="text-4xl text-ink font-display">
            Welcome back
          </h1>
          <p className="mt-2 text-[14px] text-muted">Log in to your SyncScore account</p>
        </div>

        <div className="card-base p-8">
          <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
            <div className="space-y-1.5">
              <Label htmlFor="username" className="text-ink">Username</Label>
              <Input
                id="username"
                autoComplete="username"
                {...register("username")}
                className="border-hairline-strong bg-surface-1 text-ink focus-visible:ring-trust"
              />
              {errors.username && (
                <p className="text-xs text-mismatch">{errors.username.message}</p>
              )}
            </div>

            <div className="space-y-1.5">
              <Label htmlFor="password" className="text-ink">Password</Label>
              <Input
                id="password"
                type="password"
                autoComplete="current-password"
                {...register("password")}
                className="border-hairline-strong bg-surface-1 text-ink focus-visible:ring-trust"
              />
              {errors.password && (
                <p className="text-xs text-mismatch">{errors.password.message}</p>
              )}
            </div>

            {error && (
              <div className="rounded-lg border border-mismatch-bg bg-mismatch-bg/30 px-3 py-2 text-sm text-mismatch">
                {error}
              </div>
            )}

            <Button
              type="submit"
              disabled={isSubmitting}
              className="w-full rounded-full bg-trust text-bg hover:opacity-80 mt-2"
            >
              {isSubmitting ? "Logging in…" : "Log in"}
            </Button>
          </form>
        </div>

        <p className="mt-6 text-center text-[13px] text-muted">
          Don&apos;t have an account?{" "}
          <Link href="/auth/signup" className="font-medium text-ink underline-offset-2 hover:underline">
            Sign up
          </Link>
        </p>
      </div>
    </div>
  )
}

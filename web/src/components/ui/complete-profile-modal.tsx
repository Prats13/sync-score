"use client"

import { useForm } from "react-hook-form"
import { zodResolver } from "@hookform/resolvers/zod"
import { z } from "zod"
import { useState } from "react"
import { agencyApi } from "@/lib/api/agency"
import { ApiError } from "@/lib/api/client"
import { Button } from "./button"
import { Input } from "./input"
import { Label } from "./label"
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
} from "./dialog"

const schema = z.object({
  name: z.string().min(1, "Agency name is required"),
  niche: z.string().optional(),
  websiteUrl: z.string().url("Enter a valid URL").optional().or(z.literal("")),
  description: z.string().max(500).optional(),
  bookingUrl: z.string().url("Enter a valid URL").optional().or(z.literal("")),
})

type FormData = z.infer<typeof schema>

interface Props {
  open: boolean
  onComplete: () => void
}

export function CompleteProfileModal({ open, onComplete }: Props) {
  const [error, setError] = useState<string | null>(null)

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<FormData>({ resolver: zodResolver(schema) })

  const onSubmit = async (data: FormData) => {
    setError(null)
    try {
      await agencyApi.upsert({
        name: data.name,
        niche: data.niche || undefined,
        websiteUrl: data.websiteUrl || undefined,
        description: data.description || undefined,
        bookingUrl: data.bookingUrl || undefined,
      })
      onComplete()
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to save profile.")
    }
  }

  return (
    <Dialog open={open}>
      <DialogContent className="max-w-md border-2 border-[#D7D3CB] bg-[#F7F6F2]">
        <DialogHeader>
          <DialogTitle style={{ fontFamily: "var(--font-dm-serif-text)" }}>
            Complete your profile
          </DialogTitle>
          <DialogDescription className="text-[#6B6B6B]">
            Tell us about your agency to get started.
          </DialogDescription>
        </DialogHeader>

        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          <div className="space-y-1.5">
            <Label htmlFor="name">Agency name *</Label>
            <Input id="name" {...register("name")} className="border-[#D7D3CB] bg-white" />
            {errors.name && <p className="text-xs text-red-500">{errors.name.message}</p>}
          </div>

          <div className="space-y-1.5">
            <Label htmlFor="niche">Niche / specialty</Label>
            <Input
              id="niche"
              placeholder="e.g. AI agents for legal teams"
              {...register("niche")}
              className="border-[#D7D3CB] bg-white"
            />
          </div>

          <div className="space-y-1.5">
            <Label htmlFor="websiteUrl">Website</Label>
            <Input
              id="websiteUrl"
              placeholder="https://..."
              {...register("websiteUrl")}
              className="border-[#D7D3CB] bg-white"
            />
            {errors.websiteUrl && (
              <p className="text-xs text-red-500">{errors.websiteUrl.message}</p>
            )}
          </div>

          <div className="space-y-1.5">
            <Label htmlFor="bookingUrl">Booking / contact URL</Label>
            <Input
              id="bookingUrl"
              placeholder="https://cal.com/..."
              {...register("bookingUrl")}
              className="border-[#D7D3CB] bg-white"
            />
            {errors.bookingUrl && (
              <p className="text-xs text-red-500">{errors.bookingUrl.message}</p>
            )}
          </div>

          {error && (
            <div className="rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-600">
              {error}
            </div>
          )}

          <Button
            type="submit"
            disabled={isSubmitting}
            className="w-full rounded-full bg-[#10100F] text-white hover:bg-[#10100F]/80"
          >
            {isSubmitting ? "Saving…" : "Save and continue"}
          </Button>
        </form>
      </DialogContent>
    </Dialog>
  )
}

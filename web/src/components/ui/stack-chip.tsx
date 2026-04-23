import { cn } from "@/lib/utils"

const CATEGORY_CHIP_COLORS: Record<string, string> = {
  "Orchestration":   "border-blue-200 text-blue-700",
  "RAG & Retrieval": "border-purple-200 text-purple-700",
  "Memory & State":  "border-amber-200 text-amber-700",
  "Guardrails":      "border-red-200 text-red-700",
  "Observability":   "border-teal-200 text-teal-700",
  "Base SDK":        "border-gray-200 text-gray-600",
}

interface StackChipProps {
  packageName: string
  category: string
  pointsAwarded: number
  className?: string
}

export function StackChip({ packageName, category, pointsAwarded, className }: StackChipProps) {
  const colorClass = CATEGORY_CHIP_COLORS[category] ?? "border-gray-200 text-gray-600"

  return (
    <span
      className={cn(
        "inline-flex items-center gap-1 rounded-lg border px-2.5 py-1 text-xs font-medium",
        "bg-white transition-all duration-150 hover:-translate-y-0.5 hover:shadow-sm",
        colorClass,
        className,
      )}
      title={`${category} · +${pointsAwarded}pts`}
    >
      {packageName}
      {pointsAwarded > 0 && (
        <span className="opacity-60">+{pointsAwarded}</span>
      )}
    </span>
  )
}

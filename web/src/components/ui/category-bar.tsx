import { cn } from "@/lib/utils"

interface CategoryBarProps {
  label: string
  points: number
  cap: number
  className?: string
}

const CATEGORY_COLORS: Record<string, string> = {
  "Orchestration":   "bg-blue-500",
  "RAG & Retrieval": "bg-purple-500",
  "Memory & State":  "bg-amber-500",
  "Guardrails":      "bg-red-500",
  "Observability":   "bg-teal-500",
  "Base SDK":        "bg-gray-500",
}

export function CategoryBar({ label, points, cap, className }: CategoryBarProps) {
  const pct = cap > 0 ? Math.min((points / cap) * 100, 100) : 0
  const color = CATEGORY_COLORS[label] ?? "bg-gray-400"

  return (
    <div className={cn("space-y-1.5", className)}>
      <div className="flex items-center justify-between text-sm">
        <span className="font-medium text-ink">{label}</span>
        <span className="tabular-nums text-muted">
          {points}
          <span className="text-xs">/{cap}</span>
        </span>
      </div>
      <div className="h-2 rounded-full bg-hairline-strong">
        <div
          className={cn("h-2 rounded-full transition-all duration-500", color)}
          style={{ width: `${pct}%` }}
        />
      </div>
    </div>
  )
}

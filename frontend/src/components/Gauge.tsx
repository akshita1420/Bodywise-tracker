"use client";

import { fmt } from "@/lib/dates";

/** Arc gauge: calories left = budget + exercise − food. */
export default function Gauge({ budget, exercise, food }: { budget: number; exercise: number; food: number }) {
  const allowed = budget + exercise;
  const left = allowed - food;
  const over = left < 0;
  const pct = allowed > 0 ? Math.min(1, food / allowed) : 0;

  // 240° arc from 150° to 390°.
  const r = 88;
  const cx = 110;
  const cy = 104;
  const start = (150 * Math.PI) / 180;
  const end = (390 * Math.PI) / 180;
  const pt = (a: number) => `${cx + r * Math.cos(a)} ${cy + r * Math.sin(a)}`;
  const d = `M ${pt(start)} A ${r} ${r} 0 1 1 ${pt(end)}`;
  const len = r * (end - start);

  return (
    <div className="flex flex-col items-center">
      <svg viewBox="0 0 220 180" className="w-full max-w-[300px]" role="img" aria-label={`${fmt(Math.abs(left), 1)} calories ${over ? "over" : "left"}`}>
        <path d={d} fill="none" stroke="var(--color-line)" strokeWidth="13" strokeLinecap="round" />
        <path
          d={d}
          fill="none"
          stroke={over ? "var(--color-bad)" : "var(--color-ok)"}
          strokeWidth="13"
          strokeLinecap="round"
          strokeDasharray={len}
          strokeDashoffset={len * (1 - pct)}
          style={{ transition: "stroke-dashoffset 1s cubic-bezier(0.2,0.7,0.2,1), stroke 0.3s", filter: `drop-shadow(0 0 6px ${over ? "#f8514955" : "#3fb95055"})` }}
        />
        <text x={cx} y={cy - 2} textAnchor="middle" className="fill-ink" style={{ fontSize: 34, fontWeight: 700 }}>
          {fmt(Math.abs(left), 1)}
        </text>
        <text x={cx} y={cy + 20} textAnchor="middle" className={over ? "fill-bad" : "fill-muted"} style={{ fontSize: 11 }}>
          {over ? "calories over" : "calories left"}
        </text>
      </svg>
      <div className="-mt-6 text-center text-[13px] tabular-nums text-muted">
        <span className="text-ink-2">{fmt(budget)}</span> budget <span className="text-info">+ {fmt(exercise)}</span> exercise{" "}
        <span className="text-mustard">− {fmt(food, 1)}</span> food
      </div>
    </div>
  );
}

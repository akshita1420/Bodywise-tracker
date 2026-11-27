"use client";

import { useState } from "react";
import { monthGrid, parse } from "@/lib/dates";
import type { HabitRow, WeekPoint } from "@/lib/types";
import { Pixel } from "./Pixel";

const DOW = ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"];

/**
 * Month calendar. Rows are Monday–Sunday weeks; Sunday is weigh-in day and shows the weekly change.
 * Each day shows its weight and little pixel icons for the habits hit that day.
 */
export default function Calendar({
  year,
  month,
  today,
  selected,
  weights,
  weeks,
  habits,
  onSelect,
  onMonth,
}: {
  year: number;
  month: number;
  today: string;
  selected: string;
  weights: Record<string, number>;
  weeks: Record<string, WeekPoint>;
  habits: Record<string, HabitRow>;
  onSelect: (d: string) => void;
  onMonth: (delta: number) => void;
}) {
  const days = monthGrid(year, month);
  const monthName = new Date(year, month, 1).toLocaleDateString("en-IN", { month: "long" });
  const [dir, setDir] = useState<1 | -1>(1);
  const go = (d: number) => {
    setDir(d > 0 ? 1 : -1);
    onMonth(d);
  };

  return (
    <div className="card overflow-hidden p-4 sm:p-5">
      <div className="mb-4 flex items-center gap-3">
        <span className="grid h-10 w-10 place-items-center rounded-lg border-2 border-line-2 bg-panel-2 text-mustard">
          <Pixel name="calendar" size={24} />
        </span>
        <div key={`${year}-${month}`} className="anim-fade">
          <div className="font-pixel text-[24px] leading-none text-ink">{monthName}</div>
          <div className="text-[13px] text-muted">{year}</div>
        </div>
        <div className="ml-auto flex gap-1.5">
          <button className="btn-ghost h-9 w-9 p-0" onClick={() => go(-1)} aria-label="Previous month">
            ◀
          </button>
          <button className="btn-ghost h-9 w-9 p-0" onClick={() => go(1)} aria-label="Next month">
            ▶
          </button>
        </div>
      </div>

      <div className="mb-1.5 grid grid-cols-7 gap-1.5">
        {DOW.map((d) => (
          <div key={d} className={`flex items-center justify-center gap-1 font-pixel text-[14px] ${d === "Sun" ? "text-mustard" : "text-muted"}`}>
            {d === "Sun" && <Pixel name="scale" size={13} />}
            {d}
          </div>
        ))}
      </div>

      <div key={`${year}-${month}`} className={`grid grid-cols-7 gap-1.5 ${dir > 0 ? "anim-slide-l" : "anim-slide-r"}`}>
        {days.map((d, i) => {
          const inMonth = parse(d).getMonth() === month;
          const isSunday = i % 7 === 6;
          const w = weights[d];
          const wk = isSunday ? weeks[d] : undefined;
          const h = habits[d];
          const isSel = d === selected;
          const isToday = d === today;
          const future = d > today;
          return (
            <button
              key={d}
              onClick={() => onSelect(d)}
              title={h ? habitTitle(h) : undefined}
              className={`group relative flex min-h-[4.2rem] flex-col items-start rounded-lg border-2 p-1.5 text-left transition duration-150 hover:-translate-y-0.5 sm:min-h-[5.2rem] sm:p-2 ${
                isSel
                  ? "border-mustard bg-mustard/10 shadow-[3px_3px_0_0_rgb(0_0_0/0.55)]"
                  : isSunday
                    ? "border-mustard/20 bg-mustard/[0.05] hover:border-mustard/60"
                    : "border-line bg-bg/50 hover:border-line-2 hover:bg-panel-2"
              } ${inMonth ? "" : "opacity-25"}`}
            >
              <span
                className={`grid h-6 min-w-6 place-items-center rounded px-1 font-pixel text-[15px] leading-none ${
                  isToday ? "bg-mustard text-bg" : future ? "text-muted" : "text-ink"
                }`}
              >
                {parse(d).getDate()}
              </span>

              {wk && wk.change !== null && (
                <span
                  className={`absolute right-1 top-1 rounded px-1 text-[11.5px] font-bold tabular-nums ${
                    wk.change < 0 ? "bg-ok/20 text-ok" : wk.change > 0 ? "bg-bad/20 text-bad" : "bg-line text-muted"
                  }`}
                  title="Change vs. previous Sunday"
                >
                  {wk.change > 0 ? "▲" : wk.change < 0 ? "▼" : "•"}
                  {Math.abs(wk.change)}
                </span>
              )}

              <div className="mt-auto w-full">
                {h && (h.waterOk || h.gymOk || h.stepsOk || h.ateOut) && (
                  <div className="mb-0.5 flex flex-wrap gap-0.5">
                    {h.waterOk && <Pixel name="drop" size={13} />}
                    {h.gymOk && <Pixel name="dumbbell" size={13} />}
                    {h.stepsOk && <Pixel name="shoe" size={13} />}
                    {h.ateOut && (
                      <span className="text-bad">
                        <Pixel name="forkknife" size={13} />
                      </span>
                    )}
                  </div>
                )}
                {w !== undefined && <span className="text-[12px] font-semibold tabular-nums text-ink-2 sm:text-[13px]">{w}kg</span>}
              </div>
            </button>
          );
        })}
      </div>

      <div className="mt-3 flex flex-wrap items-center gap-x-4 gap-y-1 text-[12.5px] text-muted">
        <span className="flex items-center gap-1"><Pixel name="drop" size={13} /> water</span>
        <span className="flex items-center gap-1"><Pixel name="dumbbell" size={13} /> gym</span>
        <span className="flex items-center gap-1"><Pixel name="shoe" size={13} /> steps</span>
        <span className="flex items-center gap-1 text-bad"><Pixel name="forkknife" size={13} /> <span className="text-muted">ate out</span></span>
        <span className="flex items-center gap-1"><span className="font-bold text-ok">▼</span> weekly change (sun)</span>
      </div>
    </div>
  );
}

function habitTitle(h: HabitRow) {
  const parts = [];
  if (h.waterLiters != null) parts.push(`water ${h.waterLiters}L`);
  if (h.gymMinutes != null) parts.push(`gym ${h.gymMinutes} min`);
  if (h.steps != null) parts.push(`${h.steps} steps`);
  if (h.ateOut) parts.push("ate out");
  return parts.join(" · ");
}

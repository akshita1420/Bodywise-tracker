"use client";

import { useCallback, useEffect, useState } from "react";
import { Pixel } from "@/components/Pixel";
import SugarModal from "@/components/SugarModal";
import { PageHead, Spinner, Tick } from "@/components/ui";
import { api } from "@/lib/api";
import { fmt, pretty, todayIso } from "@/lib/dates";
import type { HabitRow, HabitsResponse } from "@/lib/types";

type Field = "waterLiters" | "gymMinutes" | "steps";

export default function HabitsPage() {
  const [data, setData] = useState<HabitsResponse | null>(null);
  const [today, setToday] = useState<string | null>(null);

  const load = useCallback(async () => {
    setData(await api<HabitsResponse>("/habits"));
  }, []);

  useEffect(() => {
    setToday(todayIso());
    load();
  }, [load]);

  if (!data || !today) return <Spinner />;

  const n = data.rows.length;
  const waterDays = data.rows.filter((r) => r.waterOk).length;
  const gymDays = data.rows.filter((r) => r.gymOk).length;
  const stepDays = data.rows.filter((r) => r.stepsOk).length;
  const outDays = data.rows.filter((r) => r.ateOut).length;
  const sugarFreeDays = data.rows.filter((r) => r.noSugar).length;
  const streak = (() => {
    let s = 0;
    for (const r of data.rows) {
      if (r.waterOk && r.gymOk && r.stepsOk) s++;
      else if (r.date === today) continue; // today may still be in progress
      else break;
    }
    return s;
  })();

  return (
    <div>
      <PageHead icon="heart"
        cmd="bodywise habits --since start"
        title="Habits"
        sub={`Started ${pretty(data.startDate, { day: "numeric", month: "long", year: "numeric" })}. Every new day is added on top. Click a cell to log it.`}
      />

      <div className="mb-5 grid grid-cols-2 gap-3 md:grid-cols-3 xl:grid-cols-6">
        <Summary label={`water ${fmt(data.waterTargetL, 2)}L`} n={waterDays} of={n} i={0} />
        <Summary label="gym" n={gymDays} of={n} i={1} />
        <Summary label={`${fmt(data.stepsTarget / 1000)}k steps`} n={stepDays} of={n} i={2} />
        <Summary label="no sugar" n={sugarFreeDays} of={n} i={3} tone="ok" />
        <Summary label="ate out" n={outDays} of={n} i={4} tone="bad" />
        <Summary label="streak (all 3)" n={streak} of={null} i={5} tone="ok" />
      </div>

      <div className="mb-3 flex flex-wrap items-center gap-4 text-[13px] text-muted">
        <span className="flex items-center gap-1.5"><Tick ok size="sm" /> target hit</span>
        <span className="flex items-center gap-1.5"><Tick ok={false} size="sm" /> not yet</span>
        <span className="flex items-center gap-1.5"><Tick ok variant="ateOut" size="sm" /> ate outside food</span>
        <span className="text-muted/80">· click any tick to undo it, or an empty one to mark it done</span>
      </div>

      <div className="card overflow-x-auto">
        <table className="w-full min-w-[620px] border-collapse text-[14px]">
          <thead>
            <tr className="border-b border-line-2 text-left text-[12.5px] uppercase tracking-[0.12em] text-muted">
              <th className={TH}>date</th>
              <th className={`${TH} ${COL}`}><span className="flex items-center gap-2 text-sky"><Pixel name="drop" size={20} />water · {fmt(data.waterTargetL, 2)}L</span></th>
              <th className={`${TH} ${COL}`}><span className="flex items-center gap-2 text-lav"><Pixel name="dumbbell" size={20} />gym</span></th>
              <th className={`${TH} ${COL}`}><span className="flex items-center gap-2 text-pink"><Pixel name="shoe" size={20} />steps · {fmt(data.stepsTarget)}</span></th>
              <th className={`${TH} ${COL}`}><span className="flex items-center gap-2 text-pink"><Pixel name="lollipop" size={20} />sugar</span></th>
              <th className={`${TH} ${COL}`}><span className="flex items-center gap-2 text-peach"><Pixel name="forkknife" size={20} />food</span></th>
            </tr>
          </thead>
          <tbody>
            {data.rows.map((r, i) => (
              <Row key={r.date} row={r} i={i} isToday={r.date === today} waterTarget={data.waterTargetL} stepsTarget={data.stepsTarget} onSaved={load} />
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}

const TH = "px-3 py-3 font-pixel text-[15px] font-normal normal-case tracking-normal";
/** Vertical line between the habit columns. */
const COL = "border-l border-line-2";

function Summary({ label, n, of, i, tone }: { label: string; n: number; of: number | null; i: number; tone?: "ok" | "bad" }) {
  return (
    <div className="card anim-in px-4 py-3" style={{ animationDelay: `${i * 50}ms` }}>
      <div className="label">{label}</div>
      <div className={`mt-1 text-[22px] font-bold tabular-nums ${tone === "ok" ? "text-ok" : tone === "bad" ? "text-bad" : "text-ink"}`}>
        {n}
        <span className="text-[13.5px] font-normal text-muted">{of !== null ? ` / ${of}d` : " days"}</span>
      </div>
    </div>
  );
}

function Row({
  row,
  i,
  isToday,
  waterTarget,
  stepsTarget,
  onSaved,
}: {
  row: HabitRow;
  i: number;
  isToday: boolean;
  waterTarget: number;
  stepsTarget: number;
  onSaved: () => void;
}) {
  const out = row.ateOut;
  const [sugarOpen, setSugarOpen] = useState(false);
  return (
    <tr className={`anim-in border-b border-line last:border-0 ${isToday ? "bg-mustard/[0.05]" : ""}`} style={{ animationDelay: `${Math.min(i, 20) * 25}ms` }}>
      <td className="whitespace-nowrap px-3 py-2.5">
        <span className={isToday ? "font-bold text-ink" : "text-ink-2"}>{pretty(row.date)}</span>
        {isToday && <span className="ml-2 rounded bg-mustard px-1.5 py-0.5 text-[11.5px] font-bold text-bg">today</span>}
      </td>
      <Cell date={row.date} field="waterLiters" value={row.waterLiters} ok={row.waterOk} fill={waterTarget} unit="L" step="0.25" quick={[0.5, 0.25]} onSaved={onSaved} />
      <Cell date={row.date} field="gymMinutes" value={row.gymMinutes} ok={row.gymOk} unit="min" step="5" onSaved={onSaved} />
      <Cell date={row.date} field="steps" value={row.steps} ok={row.stepsOk} fill={stepsTarget} unit="" step="500" onSaved={onSaved} />
      <td className={`px-3 py-2 ${COL}`}>
        <button
          className="flex max-w-56 items-center gap-2 rounded-md px-1.5 py-1 text-left transition hover:bg-panel-2"
          title="Sugar-free, or what sweets you had"
          onClick={() => setSugarOpen(true)}
        >
          {row.sugarKcal > 0 ? (
            <>
              <span className="grid h-7 w-7 shrink-0 place-items-center rounded-full border-2 border-bad bg-bad/15">
                <Pixel name="lollipop" size={16} />
              </span>
              <span className="min-w-0">
                <span className="block truncate text-[13.5px] text-ink-2">{row.sugarItems.join(", ")}</span>
                <span className="block text-[12.5px] text-bad">{row.sugarKcal} kcal</span>
              </span>
            </>
          ) : (
            <>
              <Tick ok={row.noSugar} />
              <span className={row.noSugar ? "text-ink-2" : "text-muted/60"}>{row.noSugar ? "sugar-free" : "log"}</span>
            </>
          )}
        </button>
        {sugarOpen && <SugarModal date={row.date} onClose={() => setSugarOpen(false)} onSaved={onSaved} />}
      </td>
      <td className={`px-3 py-2 ${COL}`}>
        <button
          className="flex items-center gap-2 rounded-md px-1.5 py-1 transition hover:bg-panel-2"
          title={out ? "Click to undo: ate out" : "Click if you ate outside food"}
          onClick={async () => {
            await api(`/days/${row.date}/log`, "PATCH", { ateOut: !out });
            onSaved();
          }}
        >
          {out ? (
            <>
              <Tick ok variant="ateOut" />
              <span className="text-bad">ate out</span>
            </>
          ) : row.planFollowed ? (
            <>
              <Tick ok />
              <span className="text-ink-2">on plan</span>
            </>
          ) : (
            <>
              <Tick ok={false} />
              <span className="text-muted/70">{row.planFollowed === false ? "own diet" : "home food"}</span>
            </>
          )}
        </button>
      </td>
    </tr>
  );
}

function Cell({
  date,
  field,
  value,
  ok,
  fill,
  unit,
  step,
  quick,
  onSaved,
}: {
  date: string;
  field: Field;
  value: number | null;
  ok: boolean;
  /** value set when an empty tick is clicked (the target); without it the input opens instead */
  fill?: number;
  unit: string;
  step: string;
  quick?: number[];
  onSaved: () => void;
}) {
  const [editing, setEditing] = useState(false);
  const [v, setV] = useState("");

  async function save(next: number | null) {
    setEditing(false);
    if (next === value) return;
    await api(`/days/${date}/log`, "PATCH", { [field]: next });
    onSaved();
  }

  return (
    <td className={`px-3 py-2 ${COL}`}>
      <div className="flex flex-wrap items-center gap-x-2 gap-y-1">
        <button
          className="rounded-full transition hover:scale-110 active:scale-95"
          title={ok ? "Click to undo (clears the value)" : fill != null ? "Click to mark the target as done" : "Click to enter"}
          onClick={() => {
            if (ok) save(null);
            else if (fill != null) save(fill);
            else {
              setV(value != null ? String(value) : "");
              setEditing(true);
            }
          }}
        >
          <Tick ok={ok} />
        </button>
        {editing ? (
          <input
            autoFocus
            className="input w-24 px-2 py-1 tabular-nums"
            type="number"
            min="0"
            step={step}
            value={v}
            onChange={(e) => setV(e.target.value)}
            onBlur={() => save(v.trim() === "" ? null : Number(v))}
            onKeyDown={(e) => {
              if (e.key === "Enter") (e.target as HTMLInputElement).blur();
              if (e.key === "Escape") setEditing(false);
            }}
          />
        ) : (
          <button
            className="min-w-12 rounded px-1.5 py-1 text-left tabular-nums text-ink-2 transition hover:bg-panel-2 hover:text-ink"
            onClick={() => {
              setV(value != null ? String(value) : "");
              setEditing(true);
            }}
          >
            {value != null ? `${fmt(value, 2)} ${unit}` : <span className="text-muted/50">add</span>}
          </button>
        )}
        {quick && !editing && (
          <div className="flex gap-1">
            {quick.map((q) => (
              <button
                key={q}
                className="rounded border border-info/30 px-1.5 py-0.5 text-[12px] text-info transition hover:bg-info/10"
                onClick={() => save(Math.round(((value ?? 0) + q) * 100) / 100)}
              >
                +{q}
              </button>
            ))}
          </div>
        )}
      </div>
    </td>
  );
}

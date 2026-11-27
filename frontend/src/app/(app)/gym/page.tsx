"use client";

import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { Pixel } from "@/components/Pixel";
import { PageHead, PanelTitle, Spinner } from "@/components/ui";
import { api } from "@/lib/api";
import { addDays, fmt, pretty, todayIso } from "@/lib/dates";
import type { Exercise, GymDay } from "@/lib/types";

type Intensity = "LIGHT" | "MODERATE" | "HARD";
const FACTOR: Record<Intensity, number> = { LIGHT: 0.8, MODERATE: 1, HARD: 1.2 };

/** Same formula as the server: sets × (reps × 4 s + 90 s rest). */
const strengthMinutes = (sets: number, reps: number) => (sets * (reps * 4 + 90)) / 60;
const kcalOf = (met: number, kg: number, minutes: number) => Math.round(Math.max(0, met - 1) * kg * (minutes / 60));

export default function GymPage() {
  const [date, setDate] = useState<string | null>(null);
  const [day, setDay] = useState<GymDay | null>(null);
  const [catalog, setCatalog] = useState<Exercise[]>([]);

  useEffect(() => {
    setDate(todayIso());
    api<Exercise[]>("/exercises").then(setCatalog).catch(() => {});
  }, []);

  const load = useCallback(async () => {
    if (!date) return;
    setDay(await api<GymDay>(`/days/${date}/gym`));
  }, [date]);

  useEffect(() => {
    load();
  }, [load]);

  if (!date || !day) return <Spinner />;
  const isToday = date === todayIso();

  return (
    <div>
      <PageHead
        icon="dumbbell"
        cmd="bodywise gym --log"
        title={isToday ? "Gym" : `Gym · ${pretty(date)}`}
        sub="Log each exercise: sets × reps for weights, minutes for machines and classes. Calories burned are estimated and added to that day's food budget."
      >
        <div className="flex items-center gap-1.5">
          <button className="btn-ghost h-9 w-9 p-0" onClick={() => setDate(addDays(date, -1))} aria-label="Previous day">◀</button>
          {!isToday && (
            <button className="btn-ghost px-3 py-1.5" onClick={() => setDate(todayIso())}>
              today
            </button>
          )}
          <button className="btn-ghost h-9 w-9 p-0" onClick={() => setDate(addDays(date, 1))} aria-label="Next day">▶</button>
        </div>
      </PageHead>

      <div className="grid gap-5 lg:grid-cols-[1.1fr_1fr]">
        <AddExercise date={date} day={day} catalog={catalog} onAdded={load} />

        <section className="card anim-in p-5" style={{ animationDelay: "60ms" }}>
          <PanelTitle icon="flame" right={<span className="text-[13px] text-muted">{pretty(date)}</span>}>
            session
          </PanelTitle>
          <div className="mb-4 grid grid-cols-3 gap-2 text-center">
            <Big label="exercises" value={String(day.entries.length)} />
            <Big label="minutes" value={fmt(day.totalMinutes, 0)} />
            <Big label="kcal burned" value={fmt(day.totalKcal)} tone="text-peach" />
          </div>
          {day.entries.length === 0 ? (
            <div className="flex flex-col items-center gap-2 py-8 text-center text-muted">
              <Pixel name="dumbbell" size={40} />
              <span>nothing logged yet</span>
            </div>
          ) : (
            <ul className="divide-y-2 divide-line">
              {day.entries.map((e, i) => (
                <li key={e.id} className="anim-in flex items-center gap-3 py-2.5" style={{ animationDelay: `${i * 30}ms` }}>
                  <span className={e.kind === "STRENGTH" ? "text-lav" : "text-sky"}>
                    <Pixel name={e.kind === "STRENGTH" ? "dumbbell" : "shoe"} size={22} />
                  </span>
                  <div className="min-w-0 flex-1">
                    <div className="truncate font-semibold text-ink">{e.name}</div>
                    <div className="text-[13px] text-muted">
                      {e.kind === "STRENGTH"
                        ? `${e.sets} × ${e.reps}${e.weightKg ? ` @ ${fmt(e.weightKg, 1)} kg` : ""} · ≈${fmt(e.minutes, 1)} min`
                        : `${fmt(e.minutes, 1)} min`}{" "}
                      · {e.intensity.toLowerCase()} · MET {e.met}
                    </div>
                  </div>
                  <span className="font-bold tabular-nums text-peach">{e.kcal} kcal</span>
                  <button
                    className="grid h-7 w-7 place-items-center rounded-full border-2 border-line-2 text-muted transition hover:border-bad hover:text-bad"
                    onClick={async () => {
                      await api(`/gym/${e.id}`, "DELETE");
                      load();
                    }}
                    aria-label="Remove"
                  >
                    ×
                  </button>
                </li>
              ))}
            </ul>
          )}
          <p className="mt-4 rounded-md border-2 border-line bg-bg/50 px-3 py-2 text-[13px] text-muted">
            Body weight used: <b className="text-ink-2">{fmt(day.bodyWeightKg, 1)} kg</b>{" "}
            {day.weightSource === "LOGGED" ? "(your latest weigh-in)" : day.weightSource === "GOAL" ? "(goal weight; log your weight on Today)" : "(65 kg default; log your weight on Today)"}.
          </p>
        </section>
      </div>

      <section className="card anim-in mt-5 p-5" style={{ animationDelay: "120ms" }}>
        <PanelTitle icon="check">how calories burned are calculated</PanelTitle>
        <div className="space-y-2 text-[14px] text-ink-2">
          <div className="rounded-md border-2 border-line bg-bg/60 px-3 py-2 tabular-nums">
            kcal = (MET − 1) × body weight (kg) × hours
          </div>
          <p className="text-muted">
            <b className="text-ink-2">MET</b> is how hard an activity is compared with sitting still (1 MET). Values come from the Compendium of Physical
            Activities, e.g. biceps curls 3.5, squats 6, elliptical 5, incline walk 6, running 9.8. Light × 0.8, hard × 1.2.
            The <b className="text-ink-2">− 1</b> removes what you&apos;d burn at rest anyway, since that&apos;s already in your maintenance calories.
          </p>
          <p className="text-muted">
            <b className="text-ink-2">Sets × reps</b> are turned into time: about 4 seconds per rep plus 90 seconds rest after each set (3 × 12 ≈ 7 min).
            If you rested more or less, type the real minutes.
          </p>
          <p className="text-muted">These are estimates (±20–30%). Watches and machine displays often read higher.</p>
        </div>
      </section>
    </div>
  );
}

function Big({ label, value, tone = "text-ink" }: { label: string; value: string; tone?: string }) {
  return (
    <div className="rounded-lg border-2 border-line bg-bg/40 py-2">
      <div className={`font-pixel text-[26px] leading-tight ${tone}`}>{value}</div>
      <div className="text-[12.5px] text-muted">{label}</div>
    </div>
  );
}

function AddExercise({ date, day, catalog, onAdded }: { date: string; day: GymDay; catalog: Exercise[]; onAdded: () => void }) {
  const [name, setName] = useState("");
  const [open, setOpen] = useState(false);
  const [kind, setKind] = useState<"STRENGTH" | "TIME">("STRENGTH");
  const [sets, setSets] = useState("3");
  const [reps, setReps] = useState("12");
  const [weight, setWeight] = useState("");
  const [minutes, setMinutes] = useState("");
  const [intensity, setIntensity] = useState<Intensity>("MODERATE");
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const box = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const close = (e: MouseEvent) => box.current && !box.current.contains(e.target as Node) && setOpen(false);
    document.addEventListener("mousedown", close);
    return () => document.removeEventListener("mousedown", close);
  }, []);

  const [group, setGroup] = useState("All");
  const groups = useMemo(() => ["All", ...Array.from(new Set(catalog.map((c) => c.group)))], [catalog]);
  const match = catalog.find((c) => c.name.toLowerCase() === name.trim().toLowerCase());
  /** Typing searches every group (each word must appear); otherwise show the chosen group. */
  const suggestions = useMemo(() => {
    const words = name.trim().toLowerCase().split(/\s+/).filter(Boolean);
    return catalog.filter((c) => {
      const hay = `${c.name} ${c.group} ${c.tip}`.toLowerCase();
      if (words.length) return words.every((w) => hay.includes(w));
      return group === "All" || c.group === group;
    });
  }, [catalog, name, group]);

  const met = (match?.met ?? 5) * FACTOR[intensity];
  const mins = kind === "STRENGTH" ? (Number(minutes) > 0 ? Number(minutes) : strengthMinutes(Number(sets) || 0, Number(reps) || 0)) : Number(minutes) || 0;
  const preview = kcalOf(met, day.bodyWeightKg, mins);

  async function add() {
    setError(null);
    if (!name.trim()) return setError("Pick or type an exercise");
    if (kind === "TIME" && !(Number(minutes) > 0)) return setError("How many minutes?");
    if (kind === "STRENGTH" && (!(Number(sets) > 0) || !(Number(reps) > 0))) return setError("Enter sets and reps");
    setBusy(true);
    try {
      await api(`/days/${date}/gym`, "POST", {
        name: name.trim(),
        kind,
        sets: kind === "STRENGTH" ? Number(sets) : null,
        reps: kind === "STRENGTH" ? Number(reps) : null,
        weightKg: kind === "STRENGTH" && weight ? Number(weight) : null,
        minutes: Number(minutes) > 0 ? Number(minutes) : null,
        intensity,
      });
      setName("");
      setMinutes("");
      setWeight("");
      onAdded();
    } catch (e) {
      setError(e instanceof Error ? e.message : "Could not add");
    } finally {
      setBusy(false);
    }
  }

  return (
    <section className="card anim-in relative z-10 p-5">
      <PanelTitle icon="dumbbell">add exercise</PanelTitle>

      <div className="mb-3 flex flex-wrap gap-1.5">
        {groups.map((g) => (
          <button
            key={g}
            type="button"
            onClick={() => {
              setGroup(g);
              setName("");
              setOpen(true);
            }}
            className={`rounded-full border-2 px-2.5 py-0.5 text-[12.5px] transition ${
              group === g ? "border-mustard bg-mustard/15 text-mustard" : "border-line text-muted hover:border-line-2 hover:text-ink"
            }`}
          >
            {g}
          </button>
        ))}
      </div>

      <div ref={box} className="relative">
        <span className="label">exercise</span>
        <input
          className="input mt-1.5 w-full"
          placeholder="search, or pick a group above (e.g. incline press, leg curl, rowing)"
          value={name}
          onFocus={() => setOpen(true)}
          onChange={(e) => {
            setName(e.target.value);
            setOpen(true);
            const m = catalog.find((c) => c.name.toLowerCase() === e.target.value.trim().toLowerCase());
            if (m) setKind(m.kind);
          }}
        />
        {open && suggestions.length > 0 && (
          <ul className="anim-fade absolute z-30 mt-1 max-h-80 w-full overflow-auto rounded-lg border-2 border-line-2 bg-panel-2 py-1 shadow-[4px_4px_0_0_rgb(0_0_0/0.5)]">
            {suggestions.map((c, i) => (
              <li key={c.name}>
                {(i === 0 || suggestions[i - 1].group !== c.group) && (
                  <div className="sticky top-0 bg-panel-2 px-3 pb-1 pt-2 font-pixel text-[14px] text-mustard">{c.group}</div>
                )}
                <button
                  type="button"
                  className="flex w-full items-start gap-2 px-3 py-1.5 text-left transition hover:bg-line"
                  onClick={() => {
                    setName(c.name);
                    setKind(c.kind);
                    setOpen(false);
                  }}
                >
                  <span className={`mt-0.5 ${c.kind === "STRENGTH" ? "text-lav" : "text-sky"}`}>
                    <Pixel name={c.kind === "STRENGTH" ? "dumbbell" : "shoe"} size={16} />
                  </span>
                  <span className="min-w-0 flex-1">
                    <span className="block text-ink">{c.name}</span>
                    <span className="block truncate text-[12px] text-muted">{c.tip}</span>
                  </span>
                  <span className="shrink-0 text-[12px] text-muted">{c.kind === "STRENGTH" ? "sets × reps" : "minutes"} · MET {c.met}</span>
                </button>
              </li>
            ))}
          </ul>
        )}
        {open && suggestions.length === 0 && name.trim() && (
          <div className="absolute z-30 mt-1 w-full rounded-lg border-2 border-line-2 bg-panel-2 px-3 py-2 text-[13px] text-muted">
            Not in the list. You can still add it (MET 5, moderate, is assumed).
          </div>
        )}
      </div>

      {match && (
        <p className="anim-fade mt-2 text-[13px] text-muted">
          <span className="text-mustard">{match.group}</span> · {match.tip}
        </p>
      )}

      <div className="mt-4 grid grid-cols-2 gap-1 rounded-lg border-2 border-line bg-bg p-1">
        {(["STRENGTH", "TIME"] as const).map((k) => (
          <button
            key={k}
            type="button"
            onClick={() => setKind(k)}
            className={`flex items-center justify-center gap-2 rounded-md py-1.5 transition ${kind === k ? "bg-mustard font-bold text-bg" : "text-muted hover:text-ink"}`}
          >
            <Pixel name={k === "STRENGTH" ? "dumbbell" : "shoe"} size={16} />
            {k === "STRENGTH" ? "sets × reps" : "time (machine / class)"}
          </button>
        ))}
      </div>

      {kind === "STRENGTH" ? (
        <div className="mt-4 grid grid-cols-2 gap-3 sm:grid-cols-4">
          <Num label="sets" value={sets} onChange={setSets} />
          <Num label="reps" value={reps} onChange={setReps} />
          <Num label="weight kg" value={weight} onChange={setWeight} placeholder="optional" />
          <Num label="minutes" value={minutes} onChange={setMinutes} placeholder={`≈${fmt(strengthMinutes(Number(sets) || 0, Number(reps) || 0), 1)}`} />
        </div>
      ) : (
        <div className="mt-4 grid grid-cols-2 gap-3">
          <Num label="minutes" value={minutes} onChange={setMinutes} placeholder="e.g. 20" />
        </div>
      )}

      <div className="mt-4">
        <span className="label">intensity</span>
        <div className="mt-1.5 grid grid-cols-3 gap-1.5">
          {(["LIGHT", "MODERATE", "HARD"] as const).map((x) => (
            <button
              key={x}
              type="button"
              onClick={() => setIntensity(x)}
              className={`rounded-md border-2 py-1.5 text-[14px] transition ${
                intensity === x ? "border-mustard bg-mustard/15 text-mustard" : "border-line bg-bg/40 text-muted hover:text-ink"
              }`}
            >
              {x.toLowerCase()}
            </button>
          ))}
        </div>
      </div>

      <div className="mt-5 flex flex-wrap items-center gap-3 rounded-lg border-2 border-dashed border-line-2 px-3 py-2.5">
        <Pixel name="flame" size={22} />
        <div className="flex-1 text-[13.5px] text-muted">
          ≈ <b className="text-[18px] text-peach">{preview} kcal</b> · ({fmt(met, 1)} − 1) × {fmt(day.bodyWeightKg, 1)} kg × {fmt(mins, 1)} min
          {!match && name.trim() && <span className="block text-mustard">not in the list, so MET 5 (moderate) is assumed</span>}
        </div>
        <button className="btn-primary" onClick={add} disabled={busy}>
          + add
        </button>
      </div>
      {error && <p className="mt-2 text-[13.5px] text-bad">{error}</p>}
    </section>
  );
}

function Num({ label, value, onChange, placeholder }: { label: string; value: string; onChange: (v: string) => void; placeholder?: string }) {
  return (
    <label className="block">
      <span className="label">{label}</span>
      <input className="input mt-1.5 w-full tabular-nums" type="number" min="0" step="any" value={value} placeholder={placeholder} onChange={(e) => onChange(e.target.value)} />
    </label>
  );
}

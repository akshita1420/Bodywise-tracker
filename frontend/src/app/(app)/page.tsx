"use client";

import Link from "next/link";
import { useCallback, useEffect, useMemo, useState } from "react";
import CalcExplain from "@/components/CalcExplain";
import Calendar from "@/components/Calendar";
import { Mascot, Pixel } from "@/components/Pixel";
import Portal from "@/components/Portal";
import Gauge from "@/components/Gauge";
import { MealCard, MealModal } from "@/components/Meals";
import { MacroBar, PageHead, PanelTitle, Spinner, Tick } from "@/components/ui";
import { api } from "@/lib/api";
import { addDays, fmt, monthGrid, parse, pretty, todayIso } from "@/lib/dates";
import type { Card, DaySummary, HabitRow, HabitsResponse, MealType, Profile, WeekPoint, WeightPoint } from "@/lib/types";

export default function TodayPage() {
  const [today, setToday] = useState<string | null>(null);
  const [selected, setSelected] = useState<string | null>(null);
  const [view, setView] = useState<{ y: number; m: number } | null>(null);
  const [profile, setProfile] = useState<Profile | null>(null);
  const [weights, setWeights] = useState<Record<string, number>>({});
  const [weeks, setWeeks] = useState<WeekPoint[]>([]);
  const [habits, setHabits] = useState<Record<string, HabitRow>>({});
  const [day, setDay] = useState<DaySummary | null>(null);
  const [cards, setCards] = useState<Card[]>([]);
  const [openMeal, setOpenMeal] = useState<MealType | null>(null);
  const [explain, setExplain] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Dates come from the browser clock, so they are set after mount.
  useEffect(() => {
    const t = todayIso();
    const now = new Date();
    setToday(t);
    setSelected(t);
    setView({ y: now.getFullYear(), m: now.getMonth() });
    api<Card[]>("/cards").then(setCards).catch(() => {});
  }, []);

  const loadProfile = useCallback(() => api<Profile>("/profile").then(setProfile).catch(() => {}), []);
  useEffect(() => {
    loadProfile();
  }, [loadProfile]);

  const loadWeights = useCallback(async () => {
    if (!view) return;
    const grid = monthGrid(view.y, view.m);
    const [pts, wk, hb] = await Promise.all([
      api<WeightPoint[]>(`/weights?from=${grid[0]}&to=${grid[41]}`),
      api<WeekPoint[]>(`/weights/weekly?weeks=12`),
      api<HabitsResponse>(`/habits?limit=400`),
    ]);
    setHabits(Object.fromEntries(hb.rows.map((r) => [r.date, r])));
    setWeights(Object.fromEntries(pts.map((p) => [p.date, p.weightKg])));
    setWeeks(wk);
  }, [view]);

  const loadDay = useCallback(async () => {
    if (!selected) return;
    try {
      setDay(await api<DaySummary>(`/days/${selected}`));
      setError(null);
    } catch (e) {
      setError(e instanceof Error ? e.message : "Could not load the day");
    }
  }, [selected]);

  useEffect(() => {
    loadWeights().catch(() => {});
  }, [loadWeights]);
  useEffect(() => {
    loadDay();
  }, [loadDay]);

  const weekMap = useMemo(() => Object.fromEntries(weeks.map((w) => [w.sunday, w])), [weeks]);
  const lastWeek = useMemo(() => [...weeks].reverse().find((w) => w.change !== null), [weeks]);

  async function saveLog(patch: Record<string, number | boolean | null>) {
    if (!selected) return;
    await api(`/days/${selected}/log`, "PATCH", patch);
    await Promise.all([loadDay(), loadWeights(), loadProfile()]);
  }

  async function answer(followed: boolean) {
    if (!selected || !day) return;
    const filled = day.meals.reduce((n, m) => n + m.items.length, 0);
    if (followed && filled && !confirm("This replaces the planned meals in your diary with the flash card. Continue?")) return;
    await api(`/days/${selected}/plan/answer`, "POST", { followed });
    await loadDay();
  }

  if (!today || !selected || !view) return <Spinner />;

  const t = day?.targets;
  const isFuture = selected > today;
  const meal = openMeal && day ? day.meals.find((m) => m.mealType === openMeal) : null;

  return (
    <div className="space-y-6">
      <PageHead icon="sun" cmd="bodywise today" title={selected === today ? "Today" : pretty(selected, { weekday: "long", day: "numeric", month: "long" })}>
        <div className="flex items-center gap-1.5">
          <button className="btn-ghost px-2.5 py-1.5" onClick={() => setSelected(addDays(selected, -1))} aria-label="Previous day">
            ‹
          </button>
          {selected !== today && (
            <button
              className="btn-ghost px-3 py-1.5 text-[13px]"
              onClick={() => {
                setSelected(today);
                const n = new Date();
                setView({ y: n.getFullYear(), m: n.getMonth() });
              }}
            >
              today
            </button>
          )}
          <button className="btn-ghost px-2.5 py-1.5" onClick={() => setSelected(addDays(selected, 1))} aria-label="Next day">
            ›
          </button>
        </div>
      </PageHead>

      {/* body stats */}
      <div className="grid grid-cols-2 gap-3 md:grid-cols-4">
        <Stat i={0} label="height" value={profile?.heightCm ? `${fmt(profile.heightCm)} cm` : "—"} />
        <Stat
          i={1}
          label="age"
          value={profile?.age != null ? `${profile.age} yrs` : "—"}
          hint={profile?.birthday ? `🎂 ${pretty(profile.birthday, { day: "numeric", month: "long" })}` : undefined}
        />
        <Stat
          i={2}
          label="weight"
          value={profile?.currentWeightKg ? `${fmt(profile.currentWeightKg, 1)} kg` : "—"}
          hint={profile?.goalWeightKg ? `goal ${fmt(profile.goalWeightKg, 1)} kg` : undefined}
        />
        <Stat
          i={3}
          label="sun → sun"
          value={lastWeek?.change != null ? `${lastWeek.change > 0 ? "+" : ""}${lastWeek.change} kg` : "—"}
          tone={lastWeek?.change != null ? (lastWeek.change < 0 ? "good" : lastWeek.change > 0 ? "bad" : undefined) : undefined}
          hint={lastWeek ? `week to ${pretty(lastWeek.sunday, { day: "numeric", month: "short" })}` : "weigh in on sundays"}
        />
      </div>
      {day && (
        <div className="anim-in">
          <Mascot size={52} say={catSays(day)} />
        </div>
      )}
      {profile && (!profile.birthday || !profile.heightCm || !profile.goalWeightKg) && (
        <p className="anim-in rounded-lg border border-mustard/30 bg-mustard/[0.06] px-4 py-3 text-[14px] text-ink-2">
          <span className="text-mustard">warn:</span> add birthday, height and goal weight in{" "}
          <Link href="/profile" className="text-mustard underline">./profile</Link> so the calorie-deficit plan fits you.
        </p>
      )}

      <div className="grid gap-5 lg:grid-cols-[1.35fr_1fr]">
        <Calendar
          year={view.y}
          month={view.m}
          today={today}
          selected={selected}
          weights={weights}
          weeks={weekMap}
          habits={habits}
          onSelect={setSelected}
          onMonth={(d) => {
            const nd = new Date(view.y, view.m + d, 1);
            setView({ y: nd.getFullYear(), m: nd.getMonth() });
          }}
        />

        {/* calories */}
        <div className="card p-5">
          <PanelTitle icon="flame" right={t && <span className="text-[12.5px] text-muted">{t.mode === "LOSE" ? "deficit plan" : t.mode === "GAIN" ? "gain plan" : "maintain"}</span>}>
            nutrition
          </PanelTitle>
          {!day || !t ? (
            <Spinner />
          ) : (
            <>
              <Gauge budget={t.calories} exercise={day.gymBurnKcal} food={day.totals.kcal} />
              <div className="mt-5 space-y-3">
                <MacroBar icon="drumstick" label="Protein" value={day.totals.protein} target={t.proteinG} color="var(--color-prot)" showTick />
                <MacroBar icon="leaf" label="Fiber" value={day.totals.fiber} target={t.fiberG} color="var(--color-fib)" showTick />
                <MacroBar icon="wheat" label="Carbs" value={day.totals.carbs} target={t.carbsG} color="var(--color-carb)" />
                <MacroBar icon="drop" swap={{ s: "#ffa66b" }} label="Fat" value={day.totals.fat} target={t.fatG} color="var(--color-fat)" />
              </div>
              <div className="mt-4 flex flex-wrap gap-x-4 gap-y-2 border-t border-line pt-3 text-[13.5px] text-ink-2">
                <span className="flex items-center gap-1.5"><Tick ok={day.proteinReached} size="sm" /> protein</span>
                <span className="flex items-center gap-1.5"><Tick ok={day.fiberReached} size="sm" /> fiber</span>
                <span className="flex items-center gap-1.5"><Tick ok={day.caloriesWithin} size="sm" /> in budget</span>
              </div>
              {t.weightSource !== "LOGGED" && (
                <p className="mt-3 text-[13px] text-mustard">⚠ no weight logged yet, so these use {t.weightSource === "GOAL" ? "your goal weight" : "65 kg"}. Enter today&apos;s weight below.</p>
              )}
              <button className="mt-3 w-full rounded-md border border-dashed border-line-2 py-2 text-[13.5px] text-muted transition hover:border-mustard hover:text-mustard" onClick={() => setExplain(true)}>
                ? how is this calculated
              </button>
            </>
          )}
        </div>
      </div>

      {error && <p className="rounded-md border border-bad/40 bg-bad/10 px-3 py-2 text-[14px] text-bad">{error}</p>}

      {day && (
        <>
          {/* daily entry + plan */}
          <div className="grid gap-5 lg:grid-cols-[1fr_1.35fr]">
            <div className="card p-5">
              <PanelTitle icon="scale">daily entry</PanelTitle>
              <div className="grid grid-cols-2 gap-4">
                <NumberField
                  key={`w-${selected}-${day.log.weightKg}`}
                  label={parse(selected).getDay() === 0 ? "weight · sunday ✱" : "weight"}
                  unit="kg"
                  step="0.1"
                  initial={day.log.weightKg}
                  onSave={(v) => saveLog({ weightKg: v })}
                />
                {day.log.gymKcal != null ? (
                  <div className="block">
                    <span className="label">gym</span>
                    <Link href="/gym" className="mt-1.5 flex items-center gap-2 rounded-md border-2 border-line-2 bg-bg px-3 py-2 transition hover:border-mustard">
                      <Pixel name="dumbbell" size={18} />
                      <span className="flex-1 tabular-nums text-ink">{day.log.gymMinutes} min</span>
                      <span className="text-[13px] text-peach">{day.log.gymKcal} kcal ›</span>
                    </Link>
                  </div>
                ) : (
                  <NumberField
                    key={`g-${selected}-${day.log.gymMinutes}`}
                    label="gym"
                    unit="min"
                    step="5"
                    initial={day.log.gymMinutes}
                    onSave={(v) => saveLog({ gymMinutes: v })}
                  />
                )}
              </div>
              <p className="mt-2 text-[12.5px] text-muted">
                {day.log.gymKcal != null ? <>from your <Link href="/gym" className="text-mustard underline">gym log</Link>: {day.gymBurnKcal} kcal added to today&apos;s budget</> : day.gymBurnKcal > 0 ? <>≈ {day.gymBurnKcal} kcal from {day.log.gymMinutes} gym minutes. <Link href="/gym" className="text-mustard underline">Log exercises</Link> for a better estimate</> : <>log sets &amp; reps on the <Link href="/gym" className="text-mustard underline">gym page</Link>, or just type minutes</>}
              </p>
              <label className="mt-4 flex cursor-pointer items-center gap-3 rounded-lg border border-line bg-bg/40 px-3 py-2.5 transition hover:border-mustard/50">
                <input
                  type="checkbox"
                  className="h-4 w-4 accent-[var(--color-mustard)]"
                  checked={!!day.log.ateOut}
                  onChange={(e) => saveLog({ ateOut: e.target.checked })}
                />
                <span className="flex-1 text-ink-2">I ate out today</span>
                {day.log.ateOut && <Tick ok variant="ateOut" size="sm" />}
              </label>
            </div>

            <div className={`card p-5 ${day.plan ? "border-mustard/30" : ""}`}>
              <PanelTitle
                right={
                  day.plan &&
                  cards.length > 1 && (
                    <div className="flex items-center gap-1.5">
                      <select
                        className="input max-w-44 py-1 text-[13px]"
                        value={day.plan.id}
                        onChange={async (e) => {
                          await api(`/days/${selected}/plan`, "PUT", { cardId: Number(e.target.value) });
                          loadDay();
                        }}
                      >
                        {cards.map((c) => (
                          <option key={c.id} value={c.id}>{c.title}</option>
                        ))}
                      </select>
                      <button
                        className="btn-ghost px-2 py-1 text-[13px]"
                        title="Pick another card at random"
                        onClick={async () => {
                          await api(`/days/${selected}/plan/shuffle`, "POST");
                          loadDay();
                        }}
                      >
                        ⤮
                      </button>
                    </div>
                  )
                }
              >
                flash card plan
              </PanelTitle>
              {!day.plan ? (
                <p className="text-[14px] text-muted">
                  No flash cards yet. <Link className="text-mustard underline" href="/cards">Make your first day card</Link>; one is picked for every day.
                </p>
              ) : (
                <>
                  <div className="text-[19px] font-bold text-ink">{day.plan.title}</div>
                  <div className="mt-0.5 text-[13.5px] text-muted">
                    {fmt(day.meals.reduce((s, m) => s + (m.plan?.totals.kcal ?? 0), 0))} kcal planned across{" "}
                    {day.meals.filter((m) => m.plan && m.plan.items.length).length} meals
                  </div>
                  <div className="mt-4 rounded-lg border border-line bg-bg/40 p-3">
                    {day.log.planFollowed === null ? (
                      <>
                        <p className="text-ink">{isFuture ? "> this day's card is ready." : "> did you follow the flash card plan?"}</p>
                        <p className="mt-0.5 text-[13.5px] text-muted">yes → every meal is filled from the card · no → type your own meals below</p>
                      </>
                    ) : day.log.planFollowed ? (
                      <p className="text-ok">✓ followed the card <span className="text-muted">· edit any meal below</span></p>
                    ) : (
                      <p className="text-ink-2">✎ entering your own diet <span className="text-muted">· use “I ate exactly this” in a meal you did follow</span></p>
                    )}
                    <div className="mt-3 flex flex-wrap gap-2">
                      <button className="btn-primary" onClick={() => answer(true)}>
                        {day.log.planFollowed ? "re-apply card" : "yes, followed it"}
                      </button>
                      {day.log.planFollowed !== false && (
                        <button className="btn-ghost" onClick={() => answer(false)}>
                          no, I&apos;ll enter mine
                        </button>
                      )}
                    </div>
                  </div>
                </>
              )}
            </div>
          </div>

          {/* meals */}
          <div>
            <PanelTitle icon="forkknife" right={<span className="text-[12.5px] text-muted">{fmt(day.totals.kcal, 1)} kcal eaten</span>}>your meals</PanelTitle>
            <div className="grid gap-3 md:grid-cols-2">
              {day.meals.map((m, i) => (
                <MealCard key={`${selected}-${m.mealType}`} meal={m} index={i} onOpen={() => setOpenMeal(m.mealType)} />
              ))}
            </div>
          </div>
        </>
      )}

      {meal && selected && <MealModal key={meal.mealType} meal={meal} date={selected} onClose={() => setOpenMeal(null)} onChange={loadDay} />}

      {explain && day && (
        <Portal>
          <div className="fixed inset-0 z-50 flex items-center justify-center p-0 sm:p-6" role="dialog" aria-modal="true">
            <div className="anim-fade absolute inset-0 bg-black/70 backdrop-blur-[2px]" onClick={() => setExplain(false)} />
            <div className="anim-in relative flex max-h-full w-full max-w-2xl flex-col border border-line-2 bg-panel shadow-2xl sm:max-h-[90vh] sm:rounded-2xl">
              <div className="flex items-center gap-3 border-b border-line px-6 py-4">
                <div className="flex-1">
                  <div className="text-[12.5px] text-muted">$ bodywise explain --date {selected}</div>
                  <div className="font-bold text-ink">How your numbers are calculated</div>
                </div>
                <button className="text-[22px] text-muted hover:text-ink" onClick={() => setExplain(false)} aria-label="Close">
                  ×
                </button>
              </div>
              <div className="overflow-y-auto px-6 py-5">
                <CalcExplain day={day} />
              </div>
            </div>
          </div>
        </Portal>
      )}
    </div>
  );
}

/** What the cat says on Today, based on how the day is going. */
function catSays(day: DaySummary): string {
  const t = day.targets;
  if (t.weightSource !== "LOGGED") return "psst… log your weight below!";
  if (day.log.ateOut) return "ate out today? no stress, log what you can";
  if (day.totals.kcal === 0) return "what are we eating today?";
  if (day.remainingKcal < 0) return `${Math.round(-day.remainingKcal)} kcal over… a walk helps!`;
  if (day.proteinReached && day.fiberReached) return "protein + fiber done. yay!";
  if (!day.proteinReached && day.remainingKcal < 400) return "add some protein in the next meal";
  return `${Math.round(day.remainingKcal)} kcal left. you got this!`;
}

function Stat({ label, value, hint, tone, i }: { label: string; value: string; hint?: string; tone?: "good" | "bad"; i: number }) {
  return (
    <div className="card anim-in px-4 py-3" style={{ animationDelay: `${i * 60}ms` }}>
      <div className="label">{label}</div>
      <div className={`mt-1 text-[22px] font-bold tabular-nums ${tone === "good" ? "text-ok" : tone === "bad" ? "text-bad" : "text-ink"}`}>{value}</div>
      {hint && <div className="mt-0.5 text-[12.5px] text-muted">{hint}</div>}
    </div>
  );
}

function NumberField({
  label,
  unit,
  step,
  initial,
  onSave,
}: {
  label: string;
  unit: string;
  step: string;
  initial: number | null;
  onSave: (v: number | null) => Promise<void>;
}) {
  const [v, setV] = useState(initial != null ? String(initial) : "");
  const [state, setState] = useState<"idle" | "saving" | "error">("idle");
  const dirty = (initial != null ? String(initial) : "") !== v;

  async function save() {
    if (!dirty) return;
    setState("saving");
    try {
      await onSave(v.trim() === "" ? null : Number(v));
      setState("idle");
    } catch {
      setState("error");
    }
  }

  return (
    <label className="block">
      <span className="label">{label}</span>
      <div className="mt-1.5 flex items-center gap-2">
        <input
          className="input w-full tabular-nums"
          type="number"
          step={step}
          min="0"
          placeholder="—"
          value={v}
          onChange={(e) => setV(e.target.value)}
          onBlur={save}
          onKeyDown={(e) => e.key === "Enter" && save()}
        />
        <span className="text-[13.5px] text-muted">{unit}</span>
      </div>
      {state === "saving" && <span className="caret text-[12.5px] text-muted">saving</span>}
      {state === "error" && <span className="text-[12.5px] text-bad">not saved</span>}
    </label>
  );
}

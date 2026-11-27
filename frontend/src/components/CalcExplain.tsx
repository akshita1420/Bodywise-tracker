"use client";

import Link from "next/link";
import { fmt, pretty } from "@/lib/dates";
import type { DaySummary } from "@/lib/types";

const ACTIVITY: Record<string, string> = {
  SEDENTARY: "sedentary (desk job)",
  LIGHT: "light",
  MODERATE: "moderate",
  ACTIVE: "active job",
};

/** Step-by-step: how today's calorie and macro targets were worked out, with the user's own numbers. */
export default function CalcExplain({ day }: { day: DaySummary }) {
  const t = day.targets;
  const male = t.sex === "MALE";
  const weightNote =
    t.weightSource === "LOGGED"
      ? `your weigh-in on ${pretty(t.weightDate!, { day: "numeric", month: "short" })}`
      : t.weightSource === "GOAL"
        ? "no weight logged yet, so your goal weight is used. Log your weight for accurate numbers"
        : "no weight or goal set, so 65 kg is assumed. Log your weight!";

  return (
    <div className="space-y-4 text-[14px] leading-relaxed">
      <Step n={1} title="Your inputs">
        <Row k="weight" v={`${fmt(t.weightKg, 1)} kg`} note={weightNote} warn={t.weightSource !== "LOGGED"} />
        <Row k="height" v={`${fmt(t.heightCm)} cm`} note={t.heightIsDefault ? "not set, 160 cm assumed" : "from profile"} warn={t.heightIsDefault} />
        <Row k="age" v={`${t.age} yrs`} note={t.ageIsDefault ? "birthday not set, 30 assumed" : "from your birthday"} warn={t.ageIsDefault} />
        <Row k="sex" v={male ? "male" : "female"} note="changes the formula by a fixed amount" />
        <Row k="goal" v={t.goalWeightKg ? `${fmt(t.goalWeightKg, 1)} kg` : "not set"} note="decides lose / maintain / gain" warn={!t.goalWeightKg} />
      </Step>

      <Step n={2} title="BMR: calories your body burns at complete rest">
        <Formula>
          10 × {fmt(t.weightKg, 1)} + 6.25 × {fmt(t.heightCm)} − 5 × {t.age} {male ? "+ 5" : "− 161"} = <b className="text-ink">{fmt(t.bmr)} kcal</b>
        </Formula>
        <p className="text-muted">The Mifflin-St Jeor equation, the most widely used and accurate formula for adults.</p>
      </Step>

      <Step n={3} title="Maintenance: what keeps your weight the same">
        <Formula>
          {fmt(t.bmr)} × {t.activityFactor} <span className="text-muted">({ACTIVITY[t.activityLevel] ?? t.activityLevel})</span> = <b className="text-ink">{fmt(t.maintenance)} kcal</b>
        </Formula>
        <p className="text-muted">
          Activity here means your normal day <i>outside</i> the gym. Gym is counted separately each day, so it isn&apos;t counted twice.
        </p>
      </Step>

      <Step n={4} title="Daily budget for your goal">
        {t.mode === "LOSE" ? (
          <>
            <Formula>
              {fmt(t.maintenance)} − {fmt(t.deficitChosen)} deficit = {fmt(t.maintenance - t.deficitChosen)} kcal
              {t.floorApplied && (
                <>
                  {" "}→ raised to the <b className="text-mustard">{fmt(t.floorKcal)} kcal</b> safe minimum
                </>
              )}
            </Formula>
            <p className="text-muted">
              Budget <b className="text-ink">{fmt(t.calories)} kcal</b>, a real deficit of <b className="text-ink">{fmt(t.deficitActual)} kcal/day</b>. 1 kg of fat ≈ 7,700 kcal, so that is about{" "}
              <b className="text-ok">{fmt(Math.abs(t.weeklyChangeKg), 2)} kg a week</b>
              {t.weeksToGoal != null && (
                <>
                  , reaching your goal in about <b className="text-ink">{fmt(t.weeksToGoal, 0)} weeks</b>
                </>
              )}
              .
            </p>
            {t.floorApplied && (
              <p className="rounded-md border border-mustard/40 bg-mustard/[0.07] px-3 py-2 text-mustard">
                Your maintenance is low enough that a {fmt(t.deficitChosen)} kcal deficit would drop below {fmt(t.floorKcal)} kcal, the usual minimum without medical supervision.
                The gym is the healthy way to widen the gap: its calories are added to your budget.
              </p>
            )}
          </>
        ) : t.mode === "GAIN" ? (
          <Formula>
            {fmt(t.maintenance)} + 300 surplus = <b className="text-ink">{fmt(t.calories)} kcal</b>
          </Formula>
        ) : (
          <p className="text-muted">
            {t.goalWeightKg ? "You're within 0.5 kg of your goal, so the budget is your maintenance." : "No goal weight set, so the budget is your maintenance."}{" "}
            <b className="text-ink">{fmt(t.calories)} kcal</b>
          </p>
        )}
        <p className="text-muted">
          Change the deficit in <Link href="/profile" className="text-mustard underline">./profile</Link> by picking how fast you want to lose.
        </p>
      </Step>

      <Step n={5} title="Gym: added to today's budget">
        {day.log.gymMinutes ? (
          <Formula>
            ({fmt(day.gymMet, 1)} MET − 1) × {fmt(t.weightKg, 1)} kg × {fmt(day.log.gymMinutes / 60, 2)} h = <b className="text-info">{fmt(day.gymBurnKcal)} kcal</b>
          </Formula>
        ) : (
          <p className="text-muted">No gym logged today.</p>
        )}
        <p className="text-muted">
          MET is workout intensity (light 3.5 · moderate 5 · hard 7, set in profile). 1 is subtracted because your resting burn is already in maintenance.
          These are estimates; trackers often over-count, so some people eat back only half.
        </p>
      </Step>

      <Step n={6} title="Macros">
        <Row k="protein" v={`${fmt(t.proteinG)} g`} note={`1.6 g × ${fmt(t.proteinBasisKg, 1)} kg ${t.mode === "LOSE" ? "(goal weight, protects muscle while losing)" : "(body weight)"}`} />
        <Row k="fat" v={`${fmt(t.fatG)} g`} note={`25% of ${fmt(t.calories)} kcal ÷ 9 kcal per g`} />
        <Row k="carbs" v={`${fmt(t.carbsG)} g`} note="the calories left after protein (4 kcal/g) and fat, ÷ 4" />
        <Row k="fiber" v={`${fmt(t.fiberG)} g`} note="14 g per 1000 kcal, at least 25 g" />
      </Step>

      <Step n={7} title="Food calories">
        <p className="text-muted">
          Each food has values for one unit (1 roti, 1 bowl of 150 g, 100 g…), multiplied by the quantity you enter. Built-in values are averages for home-style Indian food.
          Foods you define yourself use your numbers.
        </p>
      </Step>
    </div>
  );
}

function Step({ n, title, children }: { n: number; title: string; children: React.ReactNode }) {
  return (
    <div className="flex gap-3">
      <span className="grid h-6 w-6 shrink-0 place-items-center rounded-full border border-mustard/50 text-[12.5px] font-bold text-mustard">{n}</span>
      <div className="min-w-0 flex-1 space-y-1.5">
        <div className="font-semibold text-ink">{title}</div>
        {children}
      </div>
    </div>
  );
}

function Formula({ children }: { children: React.ReactNode }) {
  return <div className="overflow-x-auto rounded-md border border-line bg-bg/70 px-3 py-2 tabular-nums text-ink-2">{children}</div>;
}

function Row({ k, v, note, warn }: { k: string; v: string; note: string; warn?: boolean }) {
  return (
    <div className="flex flex-wrap gap-x-3">
      <span className="w-16 text-muted">{k}</span>
      <span className="w-20 tabular-nums text-ink">{v}</span>
      <span className={`flex-1 ${warn ? "text-mustard" : "text-muted"}`}>{note}</span>
    </div>
  );
}

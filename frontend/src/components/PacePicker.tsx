"use client";

import { fmt } from "@/lib/dates";

/** 1 kg of body fat ≈ 7700 kcal → kcal per day for a weekly loss = kg × 7700 / 7. */
const PACES = [
  { kg: 0.25, label: "gentle", note: "easy to stick to, almost no hunger" },
  { kg: 0.5, label: "steady", note: "recommended for most people" },
  { kg: 0.75, label: "fast", note: "harder; keep protein high" },
  { kg: 1, label: "aggressive", note: "only with a lot to lose; tiring" },
];
export const kcalForPace = (kg: number) => Math.round((kg * 7700) / 7);

const FACTORS: Record<string, number> = { SEDENTARY: 1.2, LIGHT: 1.375, MODERATE: 1.55, ACTIVE: 1.725 };

function ageFrom(birthday: string) {
  if (!birthday) return null;
  const b = new Date(birthday);
  const now = new Date();
  let a = now.getFullYear() - b.getFullYear();
  if (now.getMonth() < b.getMonth() || (now.getMonth() === b.getMonth() && now.getDate() < b.getDate())) a--;
  return a;
}

/**
 * Pick how fast to lose instead of typing a deficit. Shows, live, what that means with the
 * profile values on screen: maintenance, daily budget, kg per week and weeks to goal.
 */
export default function PacePicker({
  deficit,
  onChange,
  currentWeightKg,
  goalWeightKg,
  heightCm,
  birthday,
  sex,
  activityLevel,
}: {
  deficit: number;
  onChange: (kcal: number) => void;
  currentWeightKg: number | null;
  goalWeightKg: number | null;
  heightCm: number | null;
  birthday: string;
  sex: string;
  activityLevel: string;
}) {
  const weight = currentWeightKg ?? goalWeightKg;
  const age = ageFrom(birthday);
  const ready = weight != null && heightCm != null && age != null;
  const maintenance = ready ? Math.round((10 * weight! + 6.25 * heightCm! - 5 * age! + (sex === "MALE" ? 5 : -161)) * (FACTORS[activityLevel] ?? 1.2)) : null;
  const floor = sex === "MALE" ? 1500 : 1200;
  const losing = weight != null && goalWeightKg != null && goalWeightKg < weight - 0.5;

  const budget = maintenance != null ? Math.max(maintenance - deficit, floor) : null;
  const realDeficit = maintenance != null && budget != null ? maintenance - budget : deficit;
  const weekly = (realDeficit * 7) / 7700;
  const weeks = losing && weekly > 0 ? (weight! - goalWeightKg!) / weekly : null;
  const custom = !PACES.some((p) => kcalForPace(p.kg) === deficit);

  return (
    <div className="space-y-3">
      <p className="text-[13.5px] text-muted">
        Your deficit is how many calories you eat <i>below</i> maintenance each day. 1 kg of fat ≈ 7,700 kcal, so pick how fast you want to go and the deficit follows.
      </p>
      <div className="grid gap-2 sm:grid-cols-2">
        {PACES.map((p) => {
          const k = kcalForPace(p.kg);
          const active = deficit === k;
          return (
            <button
              type="button"
              key={p.kg}
              onClick={() => onChange(k)}
              className={`rounded-lg border px-3 py-2.5 text-left transition ${
                active ? "border-mustard bg-mustard/10 shadow-[0_0_20px_-8px_var(--color-mustard)]" : "border-line-2 bg-bg/40 hover:border-mustard/50"
              }`}
            >
              <div className="flex items-baseline gap-2">
                <span className={`font-bold ${active ? "text-mustard" : "text-ink"}`}>−{p.kg} kg/week</span>
                <span className="text-[12.5px] text-muted">{p.label}</span>
                <span className="ml-auto text-[13.5px] tabular-nums text-ink-2">−{k} kcal/day</span>
              </div>
              <div className="mt-0.5 text-[12.5px] text-muted">{p.note}</div>
            </button>
          );
        })}
      </div>
      <label className="flex items-center gap-2 text-[13.5px] text-muted">
        or custom
        <input
          className={`input w-24 px-2 py-1 tabular-nums ${custom ? "border-mustard" : ""}`}
          type="number"
          min="0"
          step="25"
          value={deficit}
          onChange={(e) => onChange(Number(e.target.value) || 0)}
        />
        kcal/day
      </label>

      <div className="rounded-lg border border-line bg-bg/60 p-3 text-[14px]">
        {!ready ? (
          <p className="text-mustard">Fill in birthday, height, goal and log a weight on Today to see your numbers.</p>
        ) : !losing ? (
          <p className="text-ink-2">
            Your goal isn&apos;t below your current weight, so no deficit is applied: your budget is maintenance, <b className="text-ink">{fmt(maintenance)} kcal</b>.
          </p>
        ) : (
          <div className="space-y-1 tabular-nums">
            <div className="flex justify-between"><span className="text-muted">maintenance</span><span className="text-ink">{fmt(maintenance)} kcal</span></div>
            <div className="flex justify-between"><span className="text-muted">− deficit</span><span className="text-ink">{fmt(deficit)} kcal</span></div>
            <div className="flex justify-between border-t border-line pt-1">
              <span className="text-muted">daily budget</span>
              <span className="font-bold text-mustard">{fmt(budget)} kcal</span>
            </div>
            {maintenance! - deficit < floor && (
              <p className="text-mustard">
                ⚠ that would go below {floor} kcal, the safe minimum, so the budget is held at {floor} and your real deficit is {fmt(realDeficit)} kcal. Gym calories add on top.
              </p>
            )}
            <p className="pt-1 text-ink-2">
              ≈ <b className="text-ok">{fmt(weekly, 2)} kg/week</b> → {fmt(weight, 1)} kg to {fmt(goalWeightKg, 1)} kg in about{" "}
              <b className="text-ink">{fmt(weeks, 0)} weeks</b>
              {weeks != null && <span className="text-muted"> (~{fmt(weeks / 4.35, 1)} months)</span>}
            </p>
            <p className="text-[12.5px] text-muted">based on {currentWeightKg != null ? "your latest logged weight" : "your goal weight (log a weight for accuracy)"}; gym days burn extra on top</p>
          </div>
        )}
      </div>
    </div>
  );
}

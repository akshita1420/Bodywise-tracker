"use client";

import { fmt } from "@/lib/dates";
import { Pixel } from "./Pixel";

/**
 * A pixel face that reacts to the day's sugar, with light-hearted but real effects.
 *
 *  walk minutes  = kcal ÷ ((4.3 MET − 1) × kg ÷ 60)      brisk walking burns about that
 *  sugar tsp     ≈ carbs ÷ 4 g                           upper bound: not all carbs in a sweet are sugar
 *  if daily      = kcal × 30 ÷ 7700 kg a month            1 kg of body fat ≈ 7700 kcal
 */
export function sugarFacts(kcal: number, carbs: number, weightKg: number) {
  const walk = Math.round(kcal / (((4.3 - 1) * weightKg) / 60));
  const tsp = Math.round(carbs / 4);
  const kgMonth = (kcal * 30) / 7700;

  if (kcal <= 0) {
    return {
      face: "faceHappy" as const,
      mood: "glowing",
      color: "text-ok",
      lines: [
        "no sugar today ✨ steady energy, no 4 pm crash",
        "fewer sugar cravings tomorrow (they really do drop after a sugar-free day or two)",
        "skin bonus: less sugar = less of the collagen damage (glycation) that builds up over months",
      ],
    };
  }
  if (kcal < 200) {
    return {
      face: "faceHappy" as const,
      mood: "tiny treat",
      color: "text-mustard",
      lines: [
        `small treat 🍪 ≈ ${walk} min of brisk walking burns it off`,
        "no visible change to face or body from this. enjoy it!",
        tsp > 0 ? `about ${tsp} tsp of sugar-ish carbs` : "",
      ].filter(Boolean),
    };
  }
  if (kcal < 500) {
    return {
      face: "facePuffy" as const,
      mood: "puffy-face risk",
      color: "text-peach",
      lines: [
        `sugar spike → energy dip in about 1–2 hours`,
        "sugar + salt make the body hold water → slightly puffier cheeks & eyes tomorrow morning (gone in a day or two)",
        `≈ ${walk} min of brisk walking to burn ${fmt(kcal)} kcal`,
        `if this were every day: ≈ +${fmt(kgMonth, 1)} kg a month`,
      ],
    };
  }
  return {
    face: "faceSleepy" as const,
    mood: "sugar crash",
    color: "text-bad",
    lines: [
      "big sugar day 🍰 → crash + sleepiness later, stronger cravings tomorrow",
      "puffy face and eyes next morning are likely (water retention, temporary)",
      `if this were every day: ≈ +${fmt(kgMonth, 1)} kg a month → a rounder face and softer jawline over time`,
      `≈ ${walk} min of brisk walking to burn ${fmt(kcal)} kcal`,
      tsp > 0 ? `about ${tsp} tsp of sugar-ish carbs (the daily limit advised by WHO is about 6 tsp of added sugar)` : "",
    ].filter(Boolean),
  };
}

export default function SugarBuddy({ kcal, carbs, weightKg, compact = false }: { kcal: number; carbs: number; weightKg: number; compact?: boolean }) {
  const f = sugarFacts(kcal, carbs, weightKg);
  return (
    <div className="flex items-start gap-3">
      <div className="pixel-bob shrink-0 text-center">
        <Pixel name={f.face} size={compact ? 44 : 64} title={f.mood} />
        {f.face === "faceSleepy" && <div className="font-pixel text-[13px] text-lav">z z z</div>}
        {f.face === "faceHappy" && kcal <= 0 && <div className="font-pixel text-[13px] text-mustard">✦ ✧ ✦</div>}
      </div>
      <div className="min-w-0 flex-1">
        <div className={`font-pixel text-[18px] ${f.color}`}>
          {f.mood}
          {kcal > 0 && <span className="ml-2 text-[15px] text-muted">{fmt(kcal)} kcal of sweets</span>}
        </div>
        <ul className="mt-1 space-y-1 text-[13.5px] text-ink-2">
          {(compact ? f.lines.slice(0, 2) : f.lines).map((l) => (
            <li key={l} className="flex gap-1.5">
              <span className="text-muted">›</span>
              <span>{l}</span>
            </li>
          ))}
        </ul>
        {!compact && <p className="mt-2 text-[12px] text-muted">fun estimates, not medical advice · one day never changes your face; habits over weeks do</p>}
      </div>
    </div>
  );
}

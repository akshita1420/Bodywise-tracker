"use client";

import { useEffect, useState } from "react";
import { api } from "@/lib/api";
import { fmt, pretty } from "@/lib/dates";
import type { DaySummary, Item, Profile } from "@/lib/types";
import { Pixel } from "./Pixel";
import FoodTextarea from "./FoodTextarea";
import Portal from "./Portal";
import SugarBuddy from "./SugarBuddy";
import { ParsedPreview, parsedKcal, useParsed } from "./TextEntry";

/**
 * Sugar for one day: mark it sugar-free, or type the sweets you had (brownie, 2 cookies, 1 gulab jamun…).
 * Sweets are saved in the day's "Sweets & sugar" slot, so their calories count in the day's total.
 */
export default function SugarModal({ date, onClose, onSaved }: { date: string; onClose: () => void; onSaved: () => void }) {
  const [items, setItems] = useState<Item[]>([]);
  const [noSugar, setNoSugar] = useState<boolean | null>(null);
  const [weight, setWeight] = useState(65);
  const [text, setText] = useState("");
  const [refresh, setRefresh] = useState(0);
  const { items: parsed, busy } = useParsed(text, refresh);
  const [error, setError] = useState<string | null>(null);

  async function load() {
    const d = await api<DaySummary>(`/days/${date}`);
    setItems(d.meals.find((m) => m.mealType === "SWEETS")?.items ?? []);
    setNoSugar(d.log.noSugar);
    setWeight(d.targets.weightKg);
  }

  useEffect(() => {
    load();
    api<Profile>("/profile").then((p) => p.currentWeightKg && setWeight(p.currentWeightKg)).catch(() => {});
    const onKey = (e: KeyboardEvent) => e.key === "Escape" && onClose();
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [date]);

  async function run(fn: () => Promise<unknown>) {
    setError(null);
    try {
      await fn();
      await load();
      onSaved();
    } catch (e) {
      setError(e instanceof Error ? e.message : "Something went wrong");
    }
  }

  const kcal = items.reduce((s, i) => s + i.kcal * i.quantity, 0);
  const carbs = items.reduce((s, i) => s + i.carbs * i.quantity, 0);
  const ok = parsed.filter((p) => p.matched);
  const previewKcal = kcal + parsedKcal(parsed);
  const previewCarbs = carbs + ok.reduce((s, p) => s + p.carbs * p.quantity, 0);

  return (
    <Portal>
      <div className="fixed inset-0 z-50 flex items-end justify-center sm:items-center" role="dialog" aria-modal="true">
        <div className="anim-fade absolute inset-0 bg-black/70 backdrop-blur-[2px]" onClick={onClose} />
        <div className="anim-in relative flex max-h-[92vh] w-full max-w-xl flex-col rounded-t-2xl border-2 border-line-2 bg-panel shadow-[6px_6px_0_0_rgb(0_0_0/0.55)] sm:rounded-2xl">
          <div className="flex items-center gap-3 border-b-2 border-line px-5 py-4">
            <span className="grid h-12 w-12 place-items-center rounded-lg border-2 border-bad/40 bg-bad/10">
              <Pixel name="lollipop" size={30} />
            </span>
            <div className="flex-1">
              <div className="font-pixel text-[22px] leading-tight text-pink">sugar check</div>
              <div className="text-[13px] text-muted">{pretty(date, { weekday: "long", day: "numeric", month: "short" })}</div>
            </div>
            <button className="text-[22px] text-muted hover:text-ink" onClick={onClose} aria-label="Close">
              ×
            </button>
          </div>

          <div className="flex-1 space-y-5 overflow-y-auto px-5 py-4">
            {/* the reacting pixel face */}
            <div className="rounded-xl border-2 border-line bg-bg/50 p-4">
              <SugarBuddy kcal={text.trim() ? previewKcal : kcal} carbs={text.trim() ? previewCarbs : carbs} weightKg={weight} />
            </div>

            {/* sugar-free */}
            <div className="flex flex-wrap items-center gap-2">
              <button
                className={noSugar && items.length === 0 ? "btn-primary" : "btn-ghost"}
                onClick={() =>
                  run(async () => {
                    if (items.length && !confirm(`Remove the ${items.length} sweet(s) logged for this day?`)) return;
                    if (items.length) await api(`/days/${date}/meals/SWEETS`, "DELETE");
                    await api(`/days/${date}/log`, "PATCH", { noSugar: true });
                  })
                }
              >
                ✓ no sugar this day
              </button>
              {noSugar !== null && (
                <button className="text-[13px] text-muted hover:text-ink" onClick={() => run(() => api(`/days/${date}/log`, "PATCH", { noSugar: null }))}>
                  reset
                </button>
              )}
            </div>

            {/* logged sweets */}
            {items.length > 0 && (
              <div>
                <div className="label mb-1">sweets you had</div>
                <ul className="divide-y-2 divide-line rounded-lg border-2 border-line">
                  {items.map((it) => (
                    <li key={it.id} className="flex items-center gap-3 px-3 py-2">
                      <Pixel name="lollipop" size={18} />
                      <span className="flex-1 text-ink">
                        {it.name} <span className="text-[13px] text-muted">· {fmt(it.quantity, 2)} × {it.unit}</span>
                      </span>
                      <span className="tabular-nums text-peach">{fmt(it.kcal * it.quantity)} kcal</span>
                      <button
                        className="grid h-6 w-6 place-items-center rounded-full border-2 border-line-2 text-[12px] text-muted hover:border-bad hover:text-bad"
                        onClick={() => run(() => api(`/entries/${it.id}`, "DELETE"))}
                        aria-label="Remove"
                      >
                        ×
                      </button>
                    </li>
                  ))}
                </ul>
                <div className="mt-1 text-right text-[13px] text-muted">
                  total <b className="text-peach">{fmt(kcal)} kcal</b> · {fmt(carbs, 1)} g carbs
                </div>
              </div>
            )}

            {/* add sweets */}
            <div>
              <label className="label">had something sweet? type it</label>
              <div className="mt-1.5">
                <FoodTextarea placeholder="e.g. 1 brownie, 2 chocolate chip cookies, 1 gulab jamun, kulfi" value={text} onChange={setText} />
              </div>
              <ParsedPreview items={parsed} busy={busy} onDefined={() => setRefresh((n) => n + 1)} />
              <button
                className="btn-primary mt-2"
                disabled={ok.length === 0}
                onClick={() =>
                  run(async () => {
                    await api(`/days/${date}/entries/bulk`, "POST", {
                      mealType: "SWEETS",
                      items: ok.map((p) => ({ foodId: p.foodId, quantity: p.quantity })),
                    });
                    await api(`/days/${date}/log`, "PATCH", { noSugar: false });
                    setText("");
                  })
                }
              >
                + add {ok.length || ""} sweet{ok.length === 1 ? "" : "s"} {ok.length > 0 && `· ${fmt(parsedKcal(parsed))} kcal`}
              </button>
              <p className="mt-2 text-[12.5px] text-muted">These count in that day&apos;s calories (they show as a &quot;Sweets &amp; sugar&quot; card on Today).</p>
            </div>
            {error && <p className="text-[13.5px] text-bad">{error}</p>}
          </div>
        </div>
      </div>
    </Portal>
  );
}

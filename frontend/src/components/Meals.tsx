"use client";

import { useEffect, useState } from "react";
import { api } from "@/lib/api";
import { fmt, pretty } from "@/lib/dates";
import type { Item, Meal } from "@/lib/types";
import { MEALS } from "@/lib/types";
import { Pixel } from "./Pixel";
import Portal from "./Portal";
import { ParsedPreview, parsedKcal, useParsed } from "./TextEntry";
import { MacroNumbers, MacroStack } from "./ui";

const timeOf = (iso: string | null) =>
  iso ? new Date(iso).toLocaleTimeString("en-IN", { hour: "2-digit", minute: "2-digit", hour12: false }) : null;

/** One meal in the list: kcal, name, items summary, ✓ when logged. Click to open. */
export function MealCard({ meal, index, onOpen }: { meal: Meal; index: number; onOpen: () => void }) {
  const info = MEALS.find((m) => m.type === meal.mealType)!;
  const logged = meal.items.length > 0;
  const names = logged ? meal.items.map((i) => i.name).join(", ") : meal.plan?.text || "nothing planned";
  const first = meal.items.find((i) => i.createdAt)?.createdAt ?? null;

  return (
    <button
      onClick={onOpen}
      style={{ animationDelay: `${index * 45}ms` }}
      className={`anim-in pixel-wiggle group w-full rounded-xl border-2 p-4 text-left shadow-[4px_4px_0_0_rgb(0_0_0/0.5)] transition hover:-translate-y-0.5 active:translate-x-[2px] active:translate-y-[2px] active:shadow-none ${
        logged ? "border-ok/40 bg-ok/[0.06] hover:border-ok/70" : "border-line-2 bg-panel hover:border-mustard/60"
      }`}
    >
      <div className="flex items-start gap-3">
        <span
          className="grid h-12 w-12 shrink-0 place-items-center rounded-lg border-2"
          style={{ borderColor: `${info.color}66`, background: `${info.color}1a` }}
        >
          <Pixel name={info.sprite} size={30} swap={info.swap} />
        </span>
        <div className="min-w-0 flex-1">
          <div className="flex items-baseline gap-2">
            {logged && <span className="anim-pop text-ok">✓</span>}
            <span className="text-[17px] font-bold tabular-nums text-ink">{fmt(meal.totals.kcal, 1)}</span>
            <span className="text-[12.5px] text-muted">kcal</span>
            <span className="truncate font-pixel text-[17px]" style={{ color: info.color }}>{info.label}</span>
          </div>
          <p className={`mt-1 line-clamp-2 text-[13.5px] ${logged ? "text-ink-2" : "text-muted"}`}>
            {!logged && meal.plan?.text && <span className="text-mustard/80">plan › </span>}
            {names}
          </p>
        </div>
        <span
          className={`grid h-8 w-8 shrink-0 place-items-center rounded-full border text-[19px] transition group-hover:scale-110 ${
            logged ? "border-ok/40 text-ok" : "border-mustard/40 text-mustard"
          }`}
        >
          {logged ? "✎" : "+"}
        </span>
      </div>
      <div className="mt-2 flex items-center gap-3 text-[12.5px] text-muted">
        <span className="pl-[60px]">⏰ {info.time}</span>
        {logged && <span>{meal.items.length} item{meal.items.length === 1 ? "" : "s"}</span>}
        {logged && first && <span className="ml-auto">logged {timeOf(first)}</span>}
        {!logged && meal.plan && meal.plan.items.length > 0 && (
          <span className="ml-auto text-mustard/80">plan {fmt(meal.plan.totals.kcal)} kcal</span>
        )}
      </div>
    </button>
  );
}

/** The popup for one meal: totals, items with editable quantities, add by typing, use the plan. */
export function MealModal({ meal, date, onClose, onChange }: { meal: Meal; date: string; onClose: () => void; onChange: () => Promise<void> }) {
  const info = MEALS.find((m) => m.type === meal.mealType)!;
  const [adding, setAdding] = useState(meal.items.length === 0);
  const [text, setText] = useState("");
  const [refresh, setRefresh] = useState(0);
  const { items: parsed, busy } = useParsed(text, refresh);
  const [error, setError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    const onKey = (e: KeyboardEvent) => e.key === "Escape" && onClose();
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [onClose]);

  async function run(fn: () => Promise<unknown>) {
    setError(null);
    try {
      await fn();
      await onChange();
    } catch (e) {
      setError(e instanceof Error ? e.message : "Something went wrong");
    }
  }

  async function addTyped() {
    const ok = parsed.filter((p) => p.matched);
    if (ok.length === 0) return;
    setSaving(true);
    await run(() =>
      api(`/days/${date}/entries/bulk`, "POST", {
        mealType: meal.mealType,
        items: ok.map((p) => ({ foodId: p.foodId, quantity: p.quantity })),
      }),
    );
    setSaving(false);
    setText("");
    setAdding(false);
  }

  const plan = meal.plan;
  const unknown = parsed.filter((p) => !p.matched).length;

  return (
    <Portal>
    <div className="fixed inset-0 z-50 flex items-end justify-center sm:items-center" role="dialog" aria-modal="true">
      <div className="anim-fade absolute inset-0 bg-black/70 backdrop-blur-[2px]" onClick={onClose} />
      <div className="anim-in relative flex max-h-[92vh] w-full max-w-xl flex-col rounded-t-2xl border border-line-2 bg-panel shadow-2xl sm:rounded-2xl">
        {/* header */}
        <div className="flex items-center gap-3 border-b border-line px-5 py-4">
          <span className="grid h-12 w-12 place-items-center rounded-lg border-2" style={{ borderColor: `${info.color}66`, background: `${info.color}1a` }}>
            <Pixel name={info.sprite} size={30} swap={info.swap} />
          </span>
          <div className="flex-1">
            <div className="font-pixel text-[22px] leading-tight" style={{ color: info.color }}>{info.label}</div>
            <div className="text-[12.5px] text-muted">{pretty(date, { weekday: "long", day: "numeric", month: "short" })}</div>
          </div>
          <button className="text-[22px] text-muted hover:text-ink" onClick={onClose} aria-label="Close">
            ×
          </button>
        </div>

        <div className="flex-1 overflow-y-auto px-5 py-4">
          {/* totals */}
          <div className="flex items-center gap-5">
            <div>
              <div className="text-4xl font-bold tabular-nums text-ink">{fmt(meal.totals.kcal, 1)}</div>
              <div className="text-[12.5px] text-muted">kcal</div>
            </div>
            <div className="flex-1">
              <MacroNumbers m={meal.totals} size="sm" />
              <div className="mt-2">
                <MacroStack {...meal.totals} />
              </div>
            </div>
          </div>

          {/* items */}
          <div className="mt-5 flex items-center justify-between">
            <span className="font-bold text-ink">
              {meal.items.length} item{meal.items.length === 1 ? "" : "s"}
            </span>
            {meal.items.length > 0 && (
              <button
                className="text-[13.5px] text-muted hover:text-bad"
                onClick={() => confirm(`Clear all items from ${info.label}?`) && run(() => api(`/days/${date}/meals/${meal.mealType}`, "DELETE"))}
              >
                clear all
              </button>
            )}
          </div>
          <ul className="mt-2 divide-y divide-line">
            {meal.items.map((it, i) => (
              <ItemRow key={`${it.id}-${it.quantity}`} item={it} index={i} run={run} />
            ))}
          </ul>

          {/* plan for this meal */}
          {plan && plan.items.length > 0 && (
            <div className="mt-4 rounded-lg border border-mustard/30 bg-mustard/[0.06] p-3">
              <div className="flex items-center gap-2 text-[13.5px]">
                <span className="text-mustard">▸ today&apos;s card says</span>
                <span className="ml-auto tabular-nums text-muted">{fmt(plan.totals.kcal)} kcal</span>
              </div>
              <p className="mt-1 text-[14px] text-ink-2">{plan.text}</p>
              <button
                className="btn-ghost mt-2 px-2.5 py-1 text-[13px]"
                onClick={() => {
                  if (meal.items.length && !confirm(`Replace the ${meal.items.length} item(s) with the plan?`)) return;
                  run(() => api(`/days/${date}/plan/follow`, "POST", { mealType: meal.mealType }));
                }}
              >
                ✓ I ate exactly this
              </button>
            </div>
          )}

          {/* type what you ate */}
          {adding && (
            <div className="anim-in mt-4">
              <label className="label">type what you ate</label>
              <textarea
                autoFocus
                rows={2}
                className="input mt-1.5 w-full resize-y leading-relaxed"
                placeholder="e.g. 2 jowar roti, 1 bowl lauki sabzi, 100 g low fat curd"
                value={text}
                onChange={(e) => setText(e.target.value)}
                onKeyDown={(e) => {
                  if (e.key === "Enter" && (e.ctrlKey || e.metaKey)) addTyped();
                }}
              />
              <ParsedPreview items={parsed} busy={busy} onDefined={() => setRefresh((n) => n + 1)} />
              <div className="mt-2 flex items-center gap-2">
                <button className="btn-primary px-3 py-1.5 text-[13px]" disabled={saving || parsed.every((p) => !p.matched)} onClick={addTyped}>
                  add {parsed.filter((p) => p.matched).length || ""} item{parsed.filter((p) => p.matched).length === 1 ? "" : "s"}
                  {parsed.length > 0 && ` · ${fmt(parsedKcal(parsed))} kcal`}
                </button>
                {unknown > 0 && <span className="text-[12.5px] text-bad">{unknown} unknown, define or skip</span>}
                <span className="ml-auto hidden text-[12.5px] text-muted sm:inline">ctrl+enter</span>
              </div>
            </div>
          )}
          {error && <p className="mt-3 text-[13.5px] text-bad">{error}</p>}
        </div>

        {/* footer */}
        <div className="flex gap-3 border-t border-line px-5 py-4">
          <button className="btn-ghost flex-1 rounded-full py-2.5" onClick={() => setAdding(true)}>
            + add item
          </button>
          <button className="btn-primary flex-[1.6] rounded-full py-2.5 font-bold" onClick={onClose}>
            ✓ log meal
          </button>
        </div>
      </div>
    </div>
    </Portal>
  );
}

function ItemRow({ item, index, run }: { item: Item; index: number; run: (fn: () => Promise<unknown>) => Promise<void> }) {
  const [editing, setEditing] = useState(false);
  const [qty, setQty] = useState(String(item.quantity));

  function commit() {
    setEditing(false);
    const n = Number(qty);
    if (!n || n <= 0 || n === item.quantity) {
      setQty(String(item.quantity));
      return;
    }
    run(() => api(`/entries/${item.id}`, "PUT", { quantity: n }));
  }

  return (
    <li className="anim-in flex items-center gap-3 py-2.5" style={{ animationDelay: `${index * 30}ms` }}>
      <span className="grid h-9 w-9 shrink-0 place-items-center rounded-lg bg-panel-2 text-[12.5px] font-bold uppercase text-mustard">
        {item.name.slice(0, 2)}
      </span>
      <div className="min-w-0 flex-1">
        <div className="truncate text-ink">
          {item.name}
          {item.fromPlan && <span className="ml-1.5 text-[11.5px] text-mustard">plan</span>}
        </div>
        <div className="text-[12.5px] tabular-nums text-muted">
          {fmt(item.kcal * item.quantity, 1)} kcal · P {fmt(item.protein * item.quantity, 1)} · C {fmt(item.carbs * item.quantity, 1)} · F{" "}
          {fmt(item.fat * item.quantity, 1)}
        </div>
      </div>
      {editing ? (
        <input
          autoFocus
          className="input w-20 rounded-full px-2 py-1 text-center text-[13.5px]"
          type="number"
          step="0.5"
          min="0"
          value={qty}
          onChange={(e) => setQty(e.target.value)}
          onBlur={commit}
          onKeyDown={(e) => e.key === "Enter" && commit()}
        />
      ) : (
        <button
          className="shrink-0 rounded-full border border-line-2 px-2.5 py-1 text-[12.5px] tabular-nums text-ink-2 transition hover:border-mustard hover:text-mustard"
          onClick={() => setEditing(true)}
          title="Change quantity"
        >
          {fmt(item.quantity, 2)} {item.unit} ✎
        </button>
      )}
      <button
        className="grid h-6 w-6 shrink-0 place-items-center rounded-full border border-line-2 text-[12.5px] text-muted transition hover:border-bad hover:text-bad"
        onClick={() => run(() => api(`/entries/${item.id}`, "DELETE"))}
        aria-label="Remove"
      >
        ×
      </button>
    </li>
  );
}

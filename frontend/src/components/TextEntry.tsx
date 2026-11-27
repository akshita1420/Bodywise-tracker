"use client";

import { useEffect, useRef, useState } from "react";
import { api } from "@/lib/api";
import { fmt } from "@/lib/dates";
import type { ParsedItem } from "@/lib/types";

/** Parses text on the server (debounced) and returns the lines. */
export function useParsed(text: string, refreshKey = 0) {
  const [items, setItems] = useState<ParsedItem[]>([]);
  const [busy, setBusy] = useState(false);
  const seq = useRef(0);

  useEffect(() => {
    const mine = ++seq.current;
    if (!text.trim()) {
      setItems([]);
      return;
    }
    setBusy(true);
    const t = setTimeout(() => {
      api<ParsedItem[]>("/parse", "POST", { text })
        .then((r) => {
          if (mine === seq.current) setItems(r);
        })
        .catch(() => {})
        .finally(() => {
          if (mine === seq.current) setBusy(false);
        });
    }, 280);
    return () => clearTimeout(t);
  }, [text, refreshKey]);

  return { items, busy };
}

export const parsedKcal = (items: ParsedItem[]) => items.reduce((s, i) => s + i.kcal * i.quantity, 0);

/**
 * The parsed lines under a text box: ✓ known, ≈ closest guess, ✗ unknown (with a form to define it).
 * onDefined is called after a new food is saved so the caller can re-parse.
 */
export function ParsedPreview({ items, busy, onDefined }: { items: ParsedItem[]; busy: boolean; onDefined: () => void }) {
  const [defining, setDefining] = useState<string | null>(null);
  if (items.length === 0 && !busy) return null;
  const total = parsedKcal(items);
  return (
    <div className="mt-2 space-y-1 rounded-md border border-line bg-bg/60 p-2 text-[13.5px]">
      {busy && items.length === 0 && <div className="caret text-muted">parsing</div>}
      {items.map((it, i) => (
        <div key={`${it.raw}-${i}`} className="anim-fade">
          <div className="flex items-start gap-2">
            <span className={`w-3 shrink-0 text-center ${!it.matched ? "text-bad" : it.approximate ? "text-mustard" : "text-ok"}`}>
              {!it.matched ? "✗" : it.approximate ? "≈" : "✓"}
            </span>
            <div className="min-w-0 flex-1">
              {it.matched ? (
                <>
                  <span className="text-ink">{it.name}</span>
                  <span className="text-muted"> · {fmt(it.quantity, 2)} × {it.unit}</span>
                  {it.approximate && <span className="text-mustard"> · closest match for “{it.query}”</span>}
                  {it.note && <span className="block text-mustard">{it.note}</span>}
                </>
              ) : (
                <>
                  <span className="text-ink">{it.query || it.raw}</span>
                  <span className="text-bad"> · unknown food</span>
                </>
              )}
            </div>
            {it.matched && <span className="shrink-0 tabular-nums text-ink-2">{fmt(it.kcal * it.quantity)} kcal</span>}
            {(!it.matched || it.approximate) && (
              <button
                type="button"
                className="shrink-0 text-mustard hover:underline"
                onClick={() => setDefining(defining === it.raw ? null : it.raw)}
              >
                {it.matched ? "use my own" : "define"}
              </button>
            )}
          </div>
          {defining === it.raw && (
            <DefineFood
              name={it.query || it.raw}
              onDone={() => {
                setDefining(null);
                onDefined();
              }}
              onCancel={() => setDefining(null)}
            />
          )}
        </div>
      ))}
      {items.length > 0 && (
        <div className="flex justify-between border-t border-line pt-1 text-muted">
          <span>
            {items.length} item{items.length === 1 ? "" : "s"}
            {items.some((i) => !i.matched) && <span className="text-bad"> · unknown items count as 0 kcal until defined</span>}
          </span>
          <span className="tabular-nums text-ink">{fmt(total)} kcal</span>
        </div>
      )}
    </div>
  );
}

/** Save a food the parser didn't know (values for ONE unit). Next time the same words are recognised. */
export function DefineFood({ name, onDone, onCancel }: { name: string; onDone: () => void; onCancel: () => void }) {
  const [f, setF] = useState({ name, unit: "serving", kcal: "", protein: "", carbs: "", fat: "", fiber: "", ingredients: "" });
  const [err, setErr] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  async function save() {
    if (!f.name.trim()) return setErr("Name it");
    if (f.kcal === "") return setErr("Enter calories for one unit");
    setBusy(true);
    try {
      await api("/foods", "POST", {
        name: f.name.trim(),
        unit: f.unit.trim() || "serving",
        kcal: Number(f.kcal) || 0,
        protein: Number(f.protein) || 0,
        carbs: Number(f.carbs) || 0,
        fat: Number(f.fat) || 0,
        fiber: Number(f.fiber) || 0,
        ingredients: f.ingredients.trim() || null,
      });
      onDone();
    } catch (e) {
      setErr(e instanceof Error ? e.message : "Could not save");
      setBusy(false);
    }
  }

  return (
    <div className="anim-in mb-1 ml-5 mt-1.5 rounded-md border border-mustard/40 bg-mustard/5 p-2">
      <div className="mb-1.5 text-[12.5px] text-mustard">new food · values for ONE unit (check the pack or a nutrition site)</div>
      <div className="flex flex-wrap items-center gap-1.5">
        <input className="input min-w-36 flex-1 px-2 py-1" value={f.name} onChange={(e) => setF({ ...f, name: e.target.value })} placeholder="name" />
        <input className="input w-28 px-2 py-1" value={f.unit} onChange={(e) => setF({ ...f, unit: e.target.value })} placeholder="unit e.g. piece" />
        {(["kcal", "protein", "carbs", "fat", "fiber"] as const).map((k) => (
          <label key={k} className="flex items-center gap-1 text-[12.5px] text-muted">
            {k === "kcal" ? "kcal" : k.slice(0, 4)}
            <input
              className="input w-14 px-1.5 py-1 text-center"
              type="number"
              min="0"
              step="0.1"
              value={f[k]}
              onChange={(e) => setF({ ...f, [k]: e.target.value })}
            />
          </label>
        ))}
      </div>
      <input
        className="input mt-1.5 w-full px-2 py-1"
        value={f.ingredients}
        onChange={(e) => setF({ ...f, ingredients: e.target.value })}
        placeholder="ingredients for the grocery list (optional), e.g. kuttu atta, potato, ghee"
      />
      <div className="mt-2 flex items-center gap-2">
        <button type="button" className="btn-primary px-2.5 py-1 text-[13px]" onClick={save} disabled={busy}>
          save food
        </button>
        <button type="button" className="text-[13px] text-muted hover:text-ink" onClick={onCancel}>
          cancel
        </button>
        {err && <span className="text-[13px] text-bad">{err}</span>}
      </div>
    </div>
  );
}

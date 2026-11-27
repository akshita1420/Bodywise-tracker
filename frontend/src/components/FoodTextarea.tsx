"use client";

import { useEffect, useRef, useState } from "react";
import { api } from "@/lib/api";

type FoodHit = { id: number; name: string; unit: string; kcal: number; custom: boolean };

/** Leading "2", "1.5 bowl", "100 g of"… before the food name in the item being typed. */
const LEAD = /^\s*((?:\d+(?:\.\d+)?|\d+\/\d+|½|¼|¾|half|one|two|three|four|five)\s*(?:x\s*)?(?:(?:glass(?:es)?|cups?|bowls?|katoris?|plates?|pieces?|pcs?|tbsps?|tsps?|grams?|gms?|gm|g|ml|slices?|scoops?|servings?)\b\.?)?\s*(?:of\s+)?)?/i;

/**
 * A textarea for typing foods separated by commas, with suggestions for the item being typed:
 * "2 cori" suggests "Coriander seed water" and fills in "2 Coriander seed water".
 * ↑/↓ to pick, Tab or Enter to accept, Esc to close.
 */
export default function FoodTextarea({
  value,
  onChange,
  placeholder,
  rows = 2,
  autoFocus,
  onSubmit,
}: {
  value: string;
  onChange: (v: string) => void;
  placeholder?: string;
  rows?: number;
  autoFocus?: boolean;
  /** Ctrl/Cmd + Enter */
  onSubmit?: () => void;
}) {
  const [hits, setHits] = useState<FoodHit[]>([]);
  const [active, setActive] = useState(0);
  const [focused, setFocused] = useState(false);
  const [dismissed, setDismissed] = useState("");
  const ref = useRef<HTMLTextAreaElement>(null);

  // The item being typed = text after the last separator.
  const cut = Math.max(value.lastIndexOf(","), value.lastIndexOf(";"), value.lastIndexOf("\n"), value.lastIndexOf("+"), value.lastIndexOf("&"));
  const fragment = value.slice(cut + 1);
  const lead = fragment.match(LEAD)?.[0] ?? "";
  const query = fragment.slice(lead.length).trim();

  useEffect(() => {
    if (!focused || query.length < 2 || query === dismissed) {
      setHits([]);
      return;
    }
    const t = setTimeout(() => {
      api<FoodHit[]>(`/foods?q=${encodeURIComponent(query)}`)
        .then((r) => {
          // Nothing to suggest if what's typed is already exactly a food name.
          const exact = r.some((f) => f.name.toLowerCase() === query.toLowerCase());
          setHits(exact ? [] : r.slice(0, 6));
          setActive(0);
        })
        .catch(() => setHits([]));
    }, 150);
    return () => clearTimeout(t);
  }, [query, focused, dismissed]);

  function accept(f: FoodHit) {
    const before = value.slice(0, cut + 1);
    const space = cut >= 0 && !/\s$/.test(before) ? " " : "";
    const next = `${before}${space}${lead.trimStart()}${f.name}`;
    onChange(next);
    setHits([]);
    setDismissed(f.name);
    requestAnimationFrame(() => {
      ref.current?.focus();
      ref.current?.setSelectionRange(next.length, next.length);
    });
  }

  const open = hits.length > 0;

  return (
    <div className="relative">
      <textarea
        ref={ref}
        rows={rows}
        autoFocus={autoFocus}
        className="input w-full resize-y leading-relaxed"
        placeholder={placeholder}
        value={value}
        onFocus={() => setFocused(true)}
        onBlur={() => setTimeout(() => setFocused(false), 150)}
        onChange={(e) => onChange(e.target.value)}
        onKeyDown={(e) => {
          if (e.key === "Enter" && (e.ctrlKey || e.metaKey)) {
            e.preventDefault();
            onSubmit?.();
            return;
          }
          if (!open) return;
          if (e.key === "ArrowDown") {
            e.preventDefault();
            setActive((a) => (a + 1) % hits.length);
          } else if (e.key === "ArrowUp") {
            e.preventDefault();
            setActive((a) => (a - 1 + hits.length) % hits.length);
          } else if (e.key === "Tab" || e.key === "Enter") {
            e.preventDefault();
            accept(hits[active]);
          } else if (e.key === "Escape") {
            e.stopPropagation();
            setDismissed(query);
            setHits([]);
          }
        }}
      />
      {open && (
        <ul className="anim-fade absolute left-0 right-0 z-40 mt-1 overflow-hidden rounded-lg border-2 border-line-2 bg-panel-2 py-1 shadow-[4px_4px_0_0_rgb(0_0_0/0.5)]">
          {hits.map((f, i) => (
            <li key={f.id}>
              <button
                type="button"
                onMouseDown={(e) => e.preventDefault()}
                onClick={() => accept(f)}
                onMouseEnter={() => setActive(i)}
                className={`flex w-full items-center gap-2 px-3 py-1.5 text-left text-[14px] ${i === active ? "bg-mustard/15 text-ink" : "text-ink-2"}`}
              >
                <span className="flex-1">
                  {f.name} <span className="text-[12.5px] text-muted">· {f.unit}</span>
                  {f.custom && <span className="ml-1.5 text-[11.5px] text-mustard">mine</span>}
                </span>
                <span className="text-[12.5px] tabular-nums text-muted">{Math.round(f.kcal)} kcal</span>
              </button>
            </li>
          ))}
          <li className="px-3 pt-1 text-[11.5px] text-muted">↑↓ choose · tab / enter to fill in · esc to close</li>
        </ul>
      )}
    </div>
  );
}

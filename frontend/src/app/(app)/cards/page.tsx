"use client";

import { useEffect, useState } from "react";
import { Pixel } from "@/components/Pixel";
import FoodTextarea from "@/components/FoodTextarea";
import Portal from "@/components/Portal";
import { ParsedPreview, parsedKcal, useParsed } from "@/components/TextEntry";
import { MacroNumbers, MacroStack, PageHead, Spinner } from "@/components/ui";
import { api } from "@/lib/api";
import { fmt, todayIso } from "@/lib/dates";
import type { Card, MealType } from "@/lib/types";
import { MEALS } from "@/lib/types";

type Draft = { id?: number; title: string; notes: string; meals: Record<MealType, string> };

const emptyMeals = () => Object.fromEntries(MEALS.map((m) => [m.type, ""])) as Record<MealType, string>;

export default function CardsPage() {
  const [cards, setCards] = useState<Card[] | null>(null);
  const [draft, setDraft] = useState<Draft | null>(null);
  const [todayCard, setTodayCard] = useState<number | null>(null);
  const [msg, setMsg] = useState<string | null>(null);

  const load = async () => {
    setCards(await api<Card[]>("/cards"));
    const d = await api<{ plan: { id: number } | null }>(`/days/${todayIso()}`);
    setTodayCard(d.plan?.id ?? null);
  };
  useEffect(() => {
    load();
  }, []);

  if (!cards) return <Spinner />;

  const edit = (c: Card) =>
    setDraft({
      id: c.id,
      title: c.title,
      notes: c.notes ?? "",
      meals: { ...emptyMeals(), ...Object.fromEntries(c.meals.map((m) => [m.mealType, m.text])) },
    });

  return (
    <div>
      <PageHead icon="card"
        cmd="bodywise cards --list"
        title="Flash cards"
        sub="Each card is a whole day of eating, from early morning to post dinner. Type each meal in plain words and the calories are worked out. One card is picked at random as each day's default plan."
      >
        <button className="btn-primary" onClick={() => setDraft({ title: "", notes: "", meals: emptyMeals() })}>
          + new day card
        </button>
      </PageHead>

      {msg && <p className="anim-in mb-4 text-[14px] text-ok">{msg}</p>}
      {cards.length === 0 && (
        <div className="card p-10 text-center text-muted">
          <div className="caret">no cards yet</div>
          <p className="mt-2 text-[14px]">Make one card with everything you eat in a day, then add more for variety.</p>
        </div>
      )}

      <div className="grid gap-5 xl:grid-cols-2">
        {cards.map((c, i) => (
          <article key={c.id} className="card anim-in flex flex-col overflow-hidden" style={{ animationDelay: `${i * 60}ms` }}>
            <header className="border-b border-line p-5">
              <div className="flex items-start gap-3">
                <div className="min-w-0 flex-1">
                  <div className="flex flex-wrap items-center gap-2">
                    <h2 className="text-[19px] font-bold text-ink">{c.title}</h2>
                    {todayCard === c.id && <span className="chip border-mustard/50 text-mustard">today&apos;s plan</span>}
                    {c.unmatched > 0 && <span className="chip border-bad/50 text-bad">{c.unmatched} unknown</span>}
                  </div>
                  {c.notes && <p className="mt-1 text-[13.5px] text-muted">{c.notes}</p>}
                </div>
                <div className="text-right">
                  <div className="text-[28px] font-bold tabular-nums text-mustard">{fmt(c.totals.kcal)}</div>
                  <div className="text-[12.5px] text-muted">kcal / day</div>
                </div>
              </div>
              <div className="mt-4">
                <MacroNumbers m={c.totals} size="sm" />
                <div className="mt-2">
                  <MacroStack {...c.totals} />
                </div>
              </div>
            </header>

            <ol className="flex-1 divide-y divide-line">
              {MEALS.map((info) => {
                const m = c.meals.find((x) => x.mealType === info.type);
                const empty = !m || !m.text;
                return (
                  <li key={info.type} className="flex gap-3 px-5 py-2.5">
                    <span className="flex w-40 shrink-0 items-center gap-2 font-pixel text-[15px]" style={{ color: info.color }}>
                      <Pixel name={info.sprite} size={20} swap={info.swap} /> {info.label}
                    </span>
                    <span className={`min-w-0 flex-1 text-[14px] ${empty ? "text-muted/60" : "text-ink-2"}`}>
                      {empty ? "—" : m!.text}
                      {m?.items.some((x) => !x.matched) && <span className="text-bad"> · has unknown items</span>}
                    </span>
                    <span className="shrink-0 text-[13.5px] tabular-nums text-ink-2">{m && m.totals.kcal > 0 ? fmt(m.totals.kcal) : ""}</span>
                  </li>
                );
              })}
            </ol>

            <footer className="flex flex-wrap gap-2 border-t border-line bg-bg/30 px-5 py-3">
              <button className="btn-ghost px-2.5 py-1 text-[13px]" onClick={() => edit(c)}>
                ✎ edit
              </button>
              <button
                className="btn-ghost px-2.5 py-1 text-[13px]"
                disabled={todayCard === c.id}
                onClick={async () => {
                  await api(`/days/${todayIso()}/plan`, "PUT", { cardId: c.id });
                  setTodayCard(c.id);
                  setMsg(`“${c.title}” is today's plan now.`);
                }}
              >
                ▸ use today
              </button>
              <button
                className="btn-ghost px-2.5 py-1 text-[13px]"
                onClick={async () => {
                  await api(`/cards/${c.id}/duplicate`, "POST");
                  load();
                }}
              >
                ⧉ duplicate
              </button>
              <button
                className="btn-danger ml-auto px-2.5 py-1 text-[13px]"
                onClick={async () => {
                  if (!confirm(`Delete “${c.title}”?`)) return;
                  await api(`/cards/${c.id}`, "DELETE");
                  load();
                }}
              >
                delete
              </button>
            </footer>
          </article>
        ))}
      </div>

      {draft && (
        <CardEditor
          draft={draft}
          onClose={() => setDraft(null)}
          onSaved={() => {
            setDraft(null);
            load();
          }}
        />
      )}
    </div>
  );
}

function CardEditor({ draft, onClose, onSaved }: { draft: Draft; onClose: () => void; onSaved: () => void }) {
  const [d, setD] = useState<Draft>(draft);
  const [kcal, setKcal] = useState<Record<string, number>>({});
  const [unknown, setUnknown] = useState<Record<string, number>>({});
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    const onKey = (e: KeyboardEvent) => e.key === "Escape" && onClose();
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [onClose]);

  const total = Object.values(kcal).reduce((a, b) => a + b, 0);
  const unknownTotal = Object.values(unknown).reduce((a, b) => a + b, 0);

  async function save() {
    if (!d.title.trim()) return setError("Give the card a title");
    setBusy(true);
    setError(null);
    const body = { title: d.title, notes: d.notes || null, meals: d.meals };
    try {
      if (d.id) await api(`/cards/${d.id}`, "PUT", body);
      else await api("/cards", "POST", body);
      onSaved();
    } catch (e) {
      setError(e instanceof Error ? e.message : "Could not save");
      setBusy(false);
    }
  }

  return (
    <Portal>
    <div className="fixed inset-0 z-50 flex items-center justify-center p-0 sm:p-6" role="dialog" aria-modal="true">
      <div className="anim-fade absolute inset-0 bg-black/70 backdrop-blur-[2px]" onClick={onClose} />
      <div className="anim-in relative flex h-full w-full max-w-5xl flex-col border border-line-2 bg-panel shadow-2xl sm:h-[92vh] sm:rounded-2xl">
        <div className="flex items-center gap-3 border-b border-line px-6 py-4">
          <div className="flex-1">
            <div className="text-[12.5px] text-muted">$ bodywise cards {d.id ? `--edit ${d.id}` : "--new"}</div>
            <div className="font-bold text-ink">{d.id ? "Edit day card" : "New day card"}</div>
          </div>
          <button className="text-[22px] text-muted hover:text-ink" onClick={onClose} aria-label="Close">
            ×
          </button>
        </div>

        <div className="flex-1 space-y-5 overflow-y-auto px-6 py-5">
          <div className="grid gap-3 sm:grid-cols-2">
            <label className="block">
              <span className="label">title</span>
              <input className="input mt-1.5 w-full" value={d.title} placeholder="e.g. High-protein weekday" onChange={(e) => setD({ ...d, title: e.target.value })} />
            </label>
            <label className="block">
              <span className="label">notes</span>
              <input className="input mt-1.5 w-full" value={d.notes} placeholder="optional" onChange={(e) => setD({ ...d, notes: e.target.value })} />
            </label>
          </div>
          <p className="rounded-md border border-line bg-bg/50 px-3 py-2 text-[13.5px] text-muted">
            Write each meal the way you&apos;d say it, separated by commas:{" "}
            <span className="text-ink-2">2 jowar roti, 1 bowl lauki sabzi, 100 g low fat curd</span>. ✓ known · ≈ closest guess · ✗ unknown (define it once and it&apos;s remembered).
          </p>

          <div className="grid gap-x-6 gap-y-5 lg:grid-cols-2">
          {MEALS.map((m) => (
            <MealText
              key={m.type}
              meal={m}
              time={m.time}
              value={d.meals[m.type]}
              onChange={(v) => setD((prev) => ({ ...prev, meals: { ...prev.meals, [m.type]: v } }))}
              onTotals={(k, u) => {
                setKcal((prev) => (prev[m.type] === k ? prev : { ...prev, [m.type]: k }));
                setUnknown((prev) => (prev[m.type] === u ? prev : { ...prev, [m.type]: u }));
              }}
            />
          ))}
          </div>
        </div>

        <div className="flex items-center gap-3 border-t border-line px-6 py-4">
          <div className="flex-1">
            <div className="text-[22px] font-bold tabular-nums text-mustard">{fmt(total)} kcal</div>
            <div className="text-[12.5px] text-muted">{unknownTotal > 0 ? <span className="text-bad">{unknownTotal} unknown item(s) not counted</span> : "whole day"}</div>
          </div>
          {error && <span className="text-[13.5px] text-bad">{error}</span>}
          <button className="btn-ghost" onClick={onClose}>
            cancel
          </button>
          <button className="btn-primary" onClick={save} disabled={busy}>
            {busy ? "saving…" : "save card"}
          </button>
        </div>
      </div>
    </div>
    </Portal>
  );
}

function MealText({
  meal,
  time,
  value,
  onChange,
  onTotals,
}: {
  meal: (typeof MEALS)[number];
  time: string;
  value: string;
  onChange: (v: string) => void;
  onTotals: (kcal: number, unknown: number) => void;
}) {
  const [refresh, setRefresh] = useState(0);
  const { items, busy } = useParsed(value, refresh);
  const k = parsedKcal(items);
  const u = items.filter((i) => !i.matched).length;

  useEffect(() => {
    onTotals(k, u);
  }, [k, u, onTotals]);

  return (
    <div>
      <div className="mb-1.5 flex items-center gap-2">
        <Pixel name={meal.sprite} size={22} swap={meal.swap} />
        <span className="font-pixel text-[18px]" style={{ color: meal.color }}>{meal.label}</span>
        <span className="text-[12.5px] text-muted">{time}</span>
        <span className="ml-auto text-[13.5px] tabular-nums text-ink-2">{k > 0 ? `${fmt(k)} kcal` : ""}</span>
      </div>
      <FoodTextarea placeholder="start typing, e.g. 2 jowar roti, 1 bowl dal" value={value} onChange={onChange} />
      <ParsedPreview items={items} busy={busy} onDefined={() => setRefresh((n) => n + 1)} />
    </div>
  );
}

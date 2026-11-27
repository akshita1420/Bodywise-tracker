"use client";

import { useCallback, useEffect, useState } from "react";
import { PageHead, PanelTitle, Spinner } from "@/components/ui";
import { api } from "@/lib/api";
import { addDays, pretty, todayIso } from "@/lib/dates";
import Link from "next/link";
import type { Card, GList, GroceryView } from "@/lib/types";
import { MEALS } from "@/lib/types";
import { Pixel } from "@/components/Pixel";

export default function GroceryPage() {
  const [date, setDate] = useState<string | null>(null);
  const [data, setData] = useState<GroceryView | null>(null);
  const [newList, setNewList] = useState("");
  const [sendMsg, setSendMsg] = useState<string | null>(null);

  useEffect(() => setDate(addDays(todayIso(), 1)), []);

  const [cards, setCards] = useState<Card[]>([]);
  const [planId, setPlanId] = useState<number | null>(null);

  const load = useCallback(async () => {
    if (!date) return;
    // The grocery view picks the day's card if none is set yet; then read which one it is.
    const [g, c] = await Promise.all([api<GroceryView>(`/grocery?date=${date}`), api<Card[]>("/cards")]);
    const d = await api<{ plan: { id: number } | null }>(`/days/${date}`);
    setData(g);
    setCards(c);
    setPlanId(d.plan?.id ?? null);
  }, [date]);

  useEffect(() => {
    load();
  }, [load]);

  if (!data || !date) return <Spinner />;
  const isTomorrow = date === addDays(todayIso(), 1);

  return (
    <div>
      <PageHead icon="cart" cmd="bodywise grocery --for tomorrow" title="Grocery" sub="Built from the flash card picked for the day you're shopping for, plus your own lists.">
        <div className="flex items-center gap-1.5">
          <button className="btn-ghost px-2.5 py-1.5" onClick={() => setDate(addDays(date, -1))}>‹</button>
          <span className="px-1 text-[14px] text-ink-2">
            {isTomorrow ? "tomorrow · " : ""}
            {pretty(date)}
          </span>
          <button className="btn-ghost px-2.5 py-1.5" onClick={() => setDate(addDays(date, 1))}>›</button>
        </div>
      </PageHead>

      {/* email reminder */}
      <div className="card anim-in mb-5 flex flex-wrap items-center gap-4 p-4">
        <span className="grid h-10 w-10 place-items-center rounded-lg bg-mustard/15 text-mustard">✉</span>
        <div className="min-w-60 flex-1 text-[14px]">
          <p className="text-ink">
            cron <span className="text-mustard">0 0 19 * * *</span> · daily at 7:00 pm
          </p>
          <p className="text-muted">
            next day&apos;s list → <span className="text-ink-2">{data.reminderEmail}</span>
            {!data.mailConfigured && <span className="text-mustard"> · email not configured on the server yet (see README), so it&apos;s only logged</span>}
          </p>
          {sendMsg && <p className="anim-fade mt-1 text-ink-2">› {sendMsg}</p>}
        </div>
        <button
          className="btn-ghost"
          onClick={async () => {
            setSendMsg("sending…");
            try {
              const r = await api<{ sent: boolean; message: string }>("/grocery/send-now", "POST");
              setSendMsg(r.message);
            } catch (e) {
              setSendMsg(e instanceof Error ? e.message : "failed");
            }
          }}
        >
          send now
        </button>
      </div>

      <div className="grid gap-5 lg:grid-cols-3">
        <section className="card anim-in p-5 lg:col-span-2 lg:row-span-2">
          <PanelTitle icon="cart">grocery list</PanelTitle>
          {cards.length === 0 ? (
            <p className="rounded-md border border-mustard/30 bg-mustard/[0.06] px-3 py-3 text-[14px] text-ink-2">
              No flash cards yet. This list is built from the flash card set as the plan for {pretty(date)}.{" "}
              <Link href="/cards" className="text-mustard underline">Make a day card</Link> and its ingredients appear here.
            </p>
          ) : (
            <label className="-mt-1 mb-3 block">
              <span className="text-[13px] text-muted">default flash card for {pretty(date)}</span>
              <select
                className="input mt-1 w-full py-1.5 text-[14px]"
                value={planId ?? ""}
                onChange={async (e) => {
                  await api(`/days/${date}/plan`, "PUT", { cardId: Number(e.target.value) });
                  load();
                }}
              >
                {cards.map((c) => (
                  <option key={c.id} value={c.id}>{c.title}</option>
                ))}
              </select>
            </label>
          )}
          {cards.length > 0 && data.dishes.length === 0 && <p className="text-[14px] text-muted">This card has no foods yet.</p>}
          <p className="mb-4 text-[13px] text-muted">
            What to buy for each dish. An ingredient appears only under the first dish that needs it; later dishes just mention it.
          </p>
          <div className="space-y-5">
            {MEALS.filter((m) => data.dishes.some((d) => d.mealType === m.type)).map((m) => (
              <div key={m.type}>
                <div className="mb-2 flex items-center gap-2 border-b-2 border-line pb-1.5">
                  <Pixel name={m.sprite} size={22} swap={m.swap} />
                  <span className="font-pixel text-[18px]" style={{ color: m.color }}>{m.label}</span>
                </div>
                <div className="space-y-3 pl-1">
                  {data.dishes
                    .filter((d) => d.mealType === m.type)
                    .map((d, i) => (
                      <div key={`${d.dish}-${i}`}>
                        <div className="text-[15px] font-semibold text-ink">
                          {d.dish}
                          {d.amount && <span className="ml-1.5 text-[13px] font-normal text-muted">· {d.amount}</span>}
                        </div>
                        {d.ingredients.length > 0 && (
                          <ul className="mt-1 grid gap-x-4 gap-y-0.5 sm:grid-cols-2">
                            {d.ingredients.map((g) => (
                              <li key={g.key}>
                                <label className="flex cursor-pointer items-center gap-2 rounded px-1 py-0.5 text-[14px] transition hover:bg-panel-2">
                                  <input
                                    type="checkbox"
                                    className="h-4 w-4 accent-[var(--color-mustard)]"
                                    checked={g.purchased}
                                    onChange={async (e) => {
                                      await api("/grocery/plan-check", "POST", { date, key: g.key, purchased: e.target.checked });
                                      load();
                                    }}
                                  />
                                  <span className={g.purchased ? "text-muted line-through" : "text-ink-2"}>{g.name}</span>
                                </label>
                              </li>
                            ))}
                          </ul>
                        )}
                        {d.alreadyListed.length > 0 && (
                          <p className="mt-0.5 pl-1 text-[12.5px] text-muted/80">
                            {d.ingredients.length === 0 ? "everything is listed above: " : "also uses (listed above): "}
                            {d.alreadyListed.join(", ")}
                          </p>
                        )}
                        {d.ingredients.length === 0 && d.alreadyListed.length === 0 && (
                          <p className="mt-0.5 pl-1 text-[12.5px] text-muted/80">nothing to buy</p>
                        )}
                      </div>
                    ))}
                </div>
              </div>
            ))}
          </div>
        </section>

        <section className="card anim-in p-5" style={{ animationDelay: "60ms" }}>
          <PanelTitle icon="card">my grocery list</PanelTitle>
          <div className="space-y-5">
            {data.lists.map((l) => (
              <MyList key={l.id} list={l} onChange={load} />
            ))}
            {data.lists.length === 0 && <p className="text-[14px] text-muted">Make a list for staples, monthly shopping, anything.</p>}
          </div>
          <form
            className="mt-4 flex gap-2"
            onSubmit={async (e) => {
              e.preventDefault();
              if (!newList.trim()) return;
              await api("/grocery/lists", "POST", { name: newList });
              setNewList("");
              load();
            }}
          >
            <input className="input min-w-0 flex-1" placeholder="new list name" value={newList} onChange={(e) => setNewList(e.target.value)} />
            <button className="btn-primary">+ list</button>
          </form>
        </section>

        <section className="card anim-in border-mustard/30 p-5" style={{ animationDelay: "120ms" }}>
          <PanelTitle icon="check" right={<span className="text-[12.5px] text-muted">{data.toBuy.length} left</span>}>to be purchased</PanelTitle>
          {data.toBuy.length === 0 ? (
            <p className="anim-pop rounded-md border border-ok/30 bg-ok/10 px-3 py-4 text-center text-[14px] text-ok">✓ all set for {pretty(date)}</p>
          ) : (
            <ul className="space-y-1.5 text-[14px]">
              {data.toBuy.map((t, i) => (
                <li key={i} className="flex justify-between gap-2">
                  <span className="text-ink">
                    <span className="text-mustard">›</span> {t.name}
                    {t.quantity && <span className="text-muted"> · {t.quantity}</span>}
                  </span>
                  <span className="shrink-0 text-right text-[12.5px] text-muted">{t.source}</span>
                </li>
              ))}
            </ul>
          )}
          <p className="mt-4 text-[12.5px] text-muted">This is exactly what the 7 pm email contains.</p>
        </section>
      </div>
    </div>
  );
}

function MyList({ list, onChange }: { list: GList; onChange: () => void }) {
  const [name, setName] = useState("");
  const [qty, setQty] = useState("");

  return (
    <div>
      <div className="mb-1.5 flex items-center justify-between">
        <h3 className="font-semibold text-ink">{list.name}</h3>
        <button
          className="text-[12.5px] text-muted hover:text-bad"
          onClick={async () => {
            if (!confirm(`Delete the list “${list.name}”?`)) return;
            await api(`/grocery/lists/${list.id}`, "DELETE");
            onChange();
          }}
        >
          rm
        </button>
      </div>
      <ul className="space-y-1">
        {list.items.map((i) => (
          <li key={i.id} className="group flex items-center gap-2 text-[14px]">
            <input
              type="checkbox"
              className="h-4 w-4 accent-[var(--color-mustard)]"
              checked={i.purchased}
              onChange={async (e) => {
                await api(`/grocery/items/${i.id}`, "PATCH", { purchased: e.target.checked });
                onChange();
              }}
            />
            <span className={`flex-1 ${i.purchased ? "text-muted line-through" : "text-ink"}`}>
              {i.name}
              {i.quantity && <span className="text-muted"> · {i.quantity}</span>}
            </span>
            <button
              className="text-muted opacity-0 transition hover:text-bad group-hover:opacity-100"
              onClick={async () => {
                await api(`/grocery/items/${i.id}`, "DELETE");
                onChange();
              }}
            >
              ×
            </button>
          </li>
        ))}
      </ul>
      <form
        className="mt-2 flex gap-1.5"
        onSubmit={async (e) => {
          e.preventDefault();
          if (!name.trim()) return;
          await api(`/grocery/lists/${list.id}/items`, "POST", { name, quantity: qty || null });
          setName("");
          setQty("");
          onChange();
        }}
      >
        <input className="input min-w-0 flex-1 py-1.5" placeholder="item" value={name} onChange={(e) => setName(e.target.value)} />
        <input className="input w-20 py-1.5" placeholder="qty" value={qty} onChange={(e) => setQty(e.target.value)} />
        <button className="btn-ghost py-1.5">add</button>
      </form>
    </div>
  );
}

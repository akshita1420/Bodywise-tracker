"use client";

import { useEffect, useRef, useState } from "react";
import { iso, monthGrid, parse } from "@/lib/dates";
import { Pixel } from "./Pixel";

const DOW = ["Mo", "Tu", "We", "Th", "Fr", "Sa", "Su"];
const MONTHS = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"];

type View = "days" | "months" | "years";

/**
 * Themed date picker. Click the title to zoom out: days → months → years (12 at a time),
 * so a birthday years back is three clicks. value / onChange use "YYYY-MM-DD".
 */
export default function DatePicker({ value, onChange }: { value: string; onChange: (v: string) => void }) {
  const [open, setOpen] = useState(false);
  const start = value ? parse(value) : new Date(2000, 0, 1);
  const [y, setY] = useState(start.getFullYear());
  const [m, setM] = useState(start.getMonth());
  const [view, setView] = useState<View>("days");
  const [anim, setAnim] = useState("anim-fade");
  const box = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!open) return;
    const close = (e: MouseEvent) => box.current && !box.current.contains(e.target as Node) && setOpen(false);
    const esc = (e: KeyboardEvent) => e.key === "Escape" && setOpen(false);
    document.addEventListener("mousedown", close);
    document.addEventListener("keydown", esc);
    return () => {
      document.removeEventListener("mousedown", close);
      document.removeEventListener("keydown", esc);
    };
  }, [open]);

  const decadeStart = Math.floor(y / 12) * 12;

  function step(delta: number) {
    setAnim(delta > 0 ? "anim-slide-l" : "anim-slide-r");
    if (view === "days") {
      const d = new Date(y, m + delta, 1);
      setY(d.getFullYear());
      setM(d.getMonth());
    } else if (view === "months") setY(y + delta);
    else setY(y + delta * 12);
  }

  function zoom(to: View) {
    setAnim("anim-pop");
    setView(to);
  }

  const title = view === "days" ? `${MONTHS[m]} ${y}` : view === "months" ? `${y}` : `${decadeStart} – ${decadeStart + 11}`;
  const todayIso = iso(new Date());
  const sel = value ? parse(value) : null;

  return (
    <div ref={box} className="relative">
      <button
        type="button"
        className="input flex w-full items-center gap-2 text-left"
        onClick={() => {
          if (!open && value) {
            const v = parse(value);
            setY(v.getFullYear());
            setM(v.getMonth());
          }
          setView("days");
          setOpen(!open);
        }}
      >
        <span className="text-mustard">
          <Pixel name="calendar" size={18} />
        </span>
        <span className={`flex-1 ${value ? "text-ink" : "text-muted/70"}`}>
          {value ? parse(value).toLocaleDateString("en-IN", { day: "numeric", month: "long", year: "numeric" }) : "pick a date"}
        </span>
        <span className="text-mustard">{open ? "▴" : "▾"}</span>
      </button>

      {open && (
        <div className="anim-in absolute left-0 top-full z-40 mt-2 w-[19rem] rounded-xl border-2 border-line-2 bg-panel-2 p-3 shadow-[5px_5px_0_0_rgb(0_0_0/0.55)]">
          <div className="mb-3 flex items-center gap-1.5">
            <button type="button" className="btn-ghost h-8 w-8 p-0" onClick={() => step(-1)} aria-label="Back">
              ◀
            </button>
            <button
              type="button"
              className="flex-1 rounded-md py-1 font-pixel text-[18px] text-ink transition hover:bg-line hover:text-mustard"
              onClick={() => zoom(view === "days" ? "months" : "years")}
              title={view === "years" ? undefined : "zoom out"}
            >
              {title} {view !== "years" && <span className="text-[13px] text-muted">▴</span>}
            </button>
            <button type="button" className="btn-ghost h-8 w-8 p-0" onClick={() => step(1)} aria-label="Forward">
              ▶
            </button>
          </div>

          {view === "days" && (
            <>
              <div className="grid grid-cols-7 text-center font-pixel text-[13px] text-muted">
                {DOW.map((d) => (
                  <div key={d} className={`py-1 ${d === "Su" ? "text-mustard" : ""}`}>{d}</div>
                ))}
              </div>
              <div key={`d-${y}-${m}`} className={`grid grid-cols-7 gap-1 ${anim}`}>
                {monthGrid(y, m).map((d) => {
                  const inMonth = parse(d).getMonth() === m;
                  const isSel = d === value;
                  const isToday = d === todayIso;
                  return (
                    <button
                      type="button"
                      key={d}
                      onClick={() => {
                        onChange(d);
                        setOpen(false);
                      }}
                      className={`h-8 rounded-md border-2 text-[13.5px] tabular-nums transition ${
                        isSel
                          ? "border-bg bg-mustard font-bold text-bg shadow-[2px_2px_0_0_rgb(0_0_0/0.6)]"
                          : isToday
                            ? "border-mustard/60 text-mustard hover:bg-mustard/10"
                            : inMonth
                              ? "border-transparent text-ink-2 hover:border-line-2 hover:bg-line hover:text-ink"
                              : "border-transparent text-muted/35 hover:bg-line"
                      }`}
                    >
                      {parse(d).getDate()}
                    </button>
                  );
                })}
              </div>
            </>
          )}

          {view === "months" && (
            <div key={`m-${y}`} className={`grid grid-cols-3 gap-1.5 ${anim}`}>
              {MONTHS.map((name, i) => {
                const isSel = sel && sel.getFullYear() === y && sel.getMonth() === i;
                return (
                  <button
                    type="button"
                    key={name}
                    onClick={() => {
                      setM(i);
                      zoom("days");
                    }}
                    className={`h-11 rounded-md border-2 font-pixel text-[16px] transition ${
                      isSel ? "border-bg bg-mustard text-bg" : "border-line bg-bg/40 text-ink-2 hover:border-mustard/60 hover:text-ink"
                    }`}
                  >
                    {name}
                  </button>
                );
              })}
            </div>
          )}

          {view === "years" && (
            <div key={`y-${decadeStart}`} className={`grid grid-cols-3 gap-1.5 ${anim}`}>
              {Array.from({ length: 12 }, (_, i) => decadeStart + i).map((yr) => {
                const isSel = sel && sel.getFullYear() === yr;
                return (
                  <button
                    type="button"
                    key={yr}
                    onClick={() => {
                      setY(yr);
                      zoom("months");
                    }}
                    className={`h-11 rounded-md border-2 font-pixel text-[16px] transition ${
                      isSel ? "border-bg bg-mustard text-bg" : "border-line bg-bg/40 text-ink-2 hover:border-mustard/60 hover:text-ink"
                    }`}
                  >
                    {yr}
                  </button>
                );
              })}
            </div>
          )}

          <div className="mt-3 flex items-center justify-between border-t-2 border-line pt-2 text-[13px]">
            <button type="button" className="text-muted hover:text-bad" onClick={() => { onChange(""); setOpen(false); }}>
              clear
            </button>
            <span className="text-[12px] text-muted">tap the title to zoom out</span>
            <button type="button" className="text-muted hover:text-ink" onClick={() => setOpen(false)}>
              close
            </button>
          </div>
        </div>
      )}
    </div>
  );
}

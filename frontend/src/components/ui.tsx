"use client";

import { Pixel } from "./Pixel";
import type { SpriteName } from "./pixelArt";

/**
 * ok      → green filled tick
 * ateOut  → red fork-and-knife mark (a day of eating outside food)
 * none    → empty dashed circle
 */
export function Tick({
  ok,
  variant,
  size = "md",
  title,
}: {
  ok: boolean;
  variant?: "ok" | "ateOut";
  size?: "sm" | "md";
  title?: string;
}) {
  const s = size === "sm" ? "h-5 w-5 text-[12.5px]" : "h-7 w-7 text-[14.5px]";
  if (variant === "ateOut") {
    return (
      <span
        title={title ?? "Ate out"}
        className={`anim-pop inline-grid shrink-0 place-items-center rounded-full border-2 border-bad bg-bad/15 text-bad ${s}`}
      >
        {/* fork and knife */}
        <svg viewBox="0 0 24 24" className="h-[62%] w-[62%]" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
          <path d="M7 3v8M4.5 3v5a2.5 2.5 0 0 0 5 0V3M7 11v10" />
          <path d="M17 21V3c-2.2 1.2-3 3.8-3 7v3h3" />
        </svg>
      </span>
    );
  }
  return ok ? (
    <span title={title} className={`anim-pop inline-grid shrink-0 place-items-center rounded-full bg-ok font-bold text-bg ${s}`}>
      ✓
    </span>
  ) : (
    <span title={title} className={`inline-grid shrink-0 place-items-center rounded-full border-2 border-dashed border-line-2 ${s}`} />
  );
}

export function MacroBar({
  label,
  value,
  target,
  color,
  icon,
  swap,
  showTick = false,
}: {
  label: string;
  value: number;
  target: number;
  color: string;
  icon?: SpriteName;
  swap?: Record<string, string>;
  showTick?: boolean;
}) {
  const pct = target > 0 ? Math.min(100, (value / target) * 100) : 0;
  const reached = value >= target;
  return (
    <div>
      <div className="mb-1.5 flex items-center justify-between text-[14px]">
        <span className="flex items-center gap-2 text-ink-2">
          {icon && <Pixel name={icon} size={18} swap={swap} />}
          {label}
          {showTick && <Tick ok={reached} size="sm" />}
        </span>
        <span className="tabular-nums text-muted">
          <b className="text-ink">{Math.round(value)}</b>/{Math.round(target)}g
        </span>
      </div>
      {/* chunky segmented bar, like an old game's health bar */}
      <div className="flex h-2.5 gap-[2px] rounded-sm border border-line-2 bg-bg p-[2px]">
        {Array.from({ length: 20 }, (_, i) => (
          <div
            key={i}
            className="flex-1 rounded-[1px] transition-colors duration-500"
            style={{ background: i < Math.round(pct / 5) ? color : "var(--color-line)", transitionDelay: `${i * 18}ms` }}
          />
        ))}
      </div>
    </div>
  );
}

export function Spinner({ label = "loading" }: { label?: string }) {
  return (
    <div className="mx-auto max-w-xs py-20 text-center">
      <div className="caret text-[15px] text-muted">{label}</div>
      <div className="loading-bar mt-3 h-0.5 rounded bg-line" />
    </div>
  );
}

/** Page header: "$ command" line, pixel-font title with a sprite. */
export function PageHead({
  cmd,
  title,
  sub,
  icon,
  children,
}: {
  cmd: string;
  title: string;
  sub?: string;
  icon?: SpriteName;
  children?: React.ReactNode;
}) {
  return (
    <div className="mb-7 flex flex-wrap items-end justify-between gap-4">
      <div className="flex items-start gap-4">
        {icon && (
          <span className="pixel-wiggle grid h-14 w-14 shrink-0 place-items-center rounded-xl border-2 border-line-2 bg-panel-2 text-mustard shadow-[3px_3px_0_0_rgb(0_0_0/0.5)]">
            <Pixel name={icon} size={36} />
          </span>
        )}
        <div>
          <div className="text-[13.5px] text-muted">
            <span className="text-ok">➜</span> <span className="text-sky">~</span> $ {cmd}
          </div>
          <h1 className="mt-0.5 font-pixel text-[34px] leading-tight text-ink">{title}</h1>
          {sub && <p className="mt-1 max-w-2xl text-[14px] text-muted">{sub}</p>}
        </div>
      </div>
      {children}
    </div>
  );
}

/** Panel title: pixel font, optional sprite, comment-style prefix. */
export function PanelTitle({ children, right, icon }: { children: React.ReactNode; right?: React.ReactNode; icon?: SpriteName }) {
  return (
    <div className="mb-4 flex items-center gap-2">
      {icon && <Pixel name={icon} size={20} className="text-mustard" />}
      <h3 className="font-pixel text-[18px] tracking-wide text-mustard">
        <span className="text-muted">{"// "}</span>
        {children}
      </h3>
      {right && <div className="ml-auto">{right}</div>}
    </div>
  );
}

/** Coloured stacked bar of carbs / protein / fat / fibre, like the summary bar in food apps. */
export function MacroStack({ carbs, protein, fat, fiber }: { carbs: number; protein: number; fat: number; fiber: number }) {
  const total = carbs + protein + fat + fiber || 1;
  const parts = [
    { v: carbs, c: "var(--color-carb)" },
    { v: protein, c: "var(--color-prot)" },
    { v: fat, c: "var(--color-fat)" },
    { v: fiber, c: "var(--color-fib)" },
  ];
  return (
    <div className="flex h-1.5 gap-0.5 overflow-hidden rounded-full bg-line">
      {parts.map((p, i) => (
        <div key={i} className="bar-fill h-full" style={{ width: `${(p.v / total) * 100}%`, background: p.c }} />
      ))}
    </div>
  );
}

export function MacroNumbers({ m, size = "md" }: { m: { carbs: number; protein: number; fat: number; fiber: number }; size?: "sm" | "md" }) {
  const items: { k: string; v: number; c: string; icon: SpriteName; swap?: Record<string, string> }[] = [
    { k: "Carbs", v: m.carbs, c: "text-carb", icon: "wheat" },
    { k: "Protein", v: m.protein, c: "text-prot", icon: "drumstick" },
    { k: "Fat", v: m.fat, c: "text-fat", icon: "drop", swap: { s: "#ffa66b" } },
    { k: "Fiber", v: m.fiber, c: "text-fib", icon: "leaf" },
  ];
  return (
    <div className="grid grid-cols-4 divide-x divide-line">
      {items.map((x) => (
        <div key={x.k} className="px-2 text-center first:pl-0 last:pr-0">
          <div className={`font-bold tabular-nums text-ink ${size === "sm" ? "text-[15px]" : "text-[18px]"}`}>
            {Math.round(x.v * 10) / 10}
            <span className="text-[12.5px] font-normal text-muted">g</span>
          </div>
          <div className={`mt-0.5 flex items-center justify-center gap-1 text-[13px] ${x.c}`}>
            <Pixel name={x.icon} size={14} swap={x.swap} />
            {x.k}
          </div>
        </div>
      ))}
    </div>
  );
}

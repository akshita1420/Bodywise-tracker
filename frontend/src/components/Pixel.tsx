"use client";

import { useEffect, useMemo, useState } from "react";
import { PALETTE, SPRITES, type SpriteName } from "./pixelArt";

/** Draws a sprite as crisp SVG squares. `swap` recolours palette letters, e.g. { s: "#ffa66b" }. */
export function Pixel({
  name,
  size = 20,
  className = "",
  swap,
  title,
}: {
  name: SpriteName;
  size?: number;
  className?: string;
  swap?: Record<string, string>;
  title?: string;
}) {
  const rows = SPRITES[name];
  const w = rows[0].length;
  const rects = useMemo(() => {
    const out: { x: number; y: number; w: number; fill: string }[] = [];
    rows.forEach((row, y) => {
      let x = 0;
      while (x < row.length) {
        const ch = row[x];
        if (ch === ".") {
          x++;
          continue;
        }
        let run = 1;
        while (x + run < row.length && row[x + run] === ch) run++;
        const fill = ch === "#" ? "currentColor" : (swap?.[ch] ?? PALETTE[ch]);
        out.push({ x, y, w: run, fill });
        x += run;
      }
    });
    return out;
  }, [rows, swap]);

  return (
    <svg
      viewBox={`0 0 ${w} ${rows.length}`}
      width={size}
      height={size}
      shapeRendering="crispEdges"
      className={`inline-block shrink-0 ${className}`}
      role={title ? "img" : undefined}
      aria-hidden={title ? undefined : true}
      aria-label={title}
    >
      {rects.map((r, i) => (
        <rect key={i} x={r.x} y={r.y} width={r.w} height={1} fill={r.fill} />
      ))}
    </svg>
  );
}

const TIPS = [
  "sip some water!",
  "protein first, then carbs",
  "a walk counts as steps",
  "you're doing great",
  "log it, don't judge it",
  "veggies = fibre = happy gut",
  "sundays are weigh-in days",
];

/** The bodywise cat: bobs, blinks, and can say something. */
export function Mascot({ size = 48, say, tips = false }: { size?: number; say?: string; tips?: boolean }) {
  const [blink, setBlink] = useState(false);
  const [tip, setTip] = useState(0);

  useEffect(() => {
    const id = setInterval(() => {
      setBlink(true);
      setTimeout(() => setBlink(false), 160);
    }, 3200);
    return () => clearInterval(id);
  }, []);

  useEffect(() => {
    if (!tips) return;
    const id = setInterval(() => setTip((t) => (t + 1) % TIPS.length), 7000);
    return () => clearInterval(id);
  }, [tips]);

  const text = say ?? (tips ? TIPS[tip] : undefined);
  return (
    <div className="flex items-end gap-2">
      <div className="pixel-bob">
        <Pixel name={blink ? "catBlink" : "cat"} size={size} title="bodywise cat" />
      </div>
      {text && (
        <div key={text} className="anim-pop relative mb-3 rounded-md border-2 border-cream bg-panel-2 px-2 py-1 font-pixel text-[14.5px] leading-tight text-ink">
          {text}
          <span className="absolute -left-[7px] bottom-1.5 h-2.5 w-2.5 rotate-45 border-b-2 border-l-2 border-cream bg-panel-2" />
        </div>
      )}
    </div>
  );
}

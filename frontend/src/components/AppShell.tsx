"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { api, getToken, setToken } from "@/lib/api";
import type { Profile } from "@/lib/types";
import { Mascot, Pixel } from "./Pixel";
import type { SpriteName } from "./pixelArt";

const NAV: { href: string; label: string; key: string; icon: SpriteName; tint: string }[] = [
  { href: "/", label: "today", key: "1", icon: "sun", tint: "text-mustard" },
  { href: "/cards", label: "flash-cards", key: "2", icon: "card", tint: "text-lav" },
  { href: "/gym", label: "gym", key: "3", icon: "dumbbell", tint: "text-sky" },
  { href: "/habits", label: "habits", key: "4", icon: "heart", tint: "text-pink" },
  { href: "/grocery", label: "grocery", key: "5", icon: "cart", tint: "text-mint" },
  { href: "/profile", label: "profile", key: "6", icon: "user", tint: "text-peach" },
];

export default function AppShell({ children }: { children: React.ReactNode }) {
  const router = useRouter();
  const pathname = usePathname();
  const [open, setOpen] = useState(false);
  const [profile, setProfile] = useState<Profile | null>(null);
  const [online, setOnline] = useState<boolean | null>(null);
  const [clock, setClock] = useState("");

  useEffect(() => {
    if (!getToken()) {
      router.replace("/login");
      return;
    }
    const ping = () =>
      api<Profile>("/profile")
        .then((p) => {
          setProfile(p);
          setOnline(true);
        })
        .catch(() => setOnline(false));
    ping();
    const id = setInterval(ping, 60_000);
    return () => clearInterval(id);
  }, [router]);

  useEffect(() => {
    const tick = () =>
      setClock(new Date().toLocaleString("en-IN", { weekday: "short", day: "2-digit", month: "short", hour: "2-digit", minute: "2-digit", hour12: false }));
    tick();
    const id = setInterval(tick, 15_000);
    return () => clearInterval(id);
  }, []);

  // Keyboard shortcuts: g then 1-5 is overkill; plain Alt+1..5 jumps between pages.
  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if (!e.altKey) return;
      const n = NAV.find((x) => x.key === e.key);
      if (n) {
        e.preventDefault();
        router.push(n.href);
      }
    };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [router]);

  const current = NAV.find((n) => (n.href === "/" ? pathname === "/" : pathname.startsWith(n.href)));

  const sidebar = (
    <div className="flex h-full flex-col">
      <Link href="/" className="block px-5 pb-3 pt-6" onClick={() => setOpen(false)}>
        <Logo />
        <div className="mt-1.5 text-[12.5px] text-muted">body · food · habits</div>
      </Link>
      <div className="px-4 pb-4">
        <Mascot size={44} tips />
      </div>

      <nav className="flex-1 space-y-0.5 px-3">
        <div className="label px-2 pb-2">{"// navigate"}</div>
        {NAV.map((n, i) => {
          const active = current?.href === n.href;
          return (
            <Link
              key={n.href}
              href={n.href}
              onClick={() => setOpen(false)}
              style={{ animationDelay: `${i * 40}ms` }}
              className={`anim-in pixel-wiggle group relative flex items-center gap-3 rounded-md border-2 px-3 py-2 text-[15px] transition ${
                active ? "border-mustard/50 bg-mustard/10 text-mustard" : "border-transparent text-ink-2 hover:bg-panel-2 hover:text-ink"
              }`}
            >
              <span className={n.tint}>
                <Pixel name={n.icon} size={22} />
              </span>
              <span className="flex-1">
                <span className="text-muted">./</span>
                {n.label}
              </span>
              <kbd className="hidden text-[11.5px] text-muted/70 lg:inline">⌥{n.key}</kbd>
            </Link>
          );
        })}
      </nav>

      <div className="space-y-3 border-t border-line px-5 py-4 text-[12.5px]">
        <div className="text-muted">{clock}</div>
        {profile && (
          <div className="truncate text-ink-2" title={profile.email}>
            <span className="text-muted">user@</span>
            {profile.name || profile.email.split("@")[0]}
          </div>
        )}
        <button
          className="text-muted transition hover:text-bad"
          onClick={() => {
            setToken(null);
            router.replace("/login");
          }}
        >
          $ logout
        </button>
      </div>
    </div>
  );

  return (
    <div className="min-h-screen md:pl-64">
      {/* desktop sidebar */}
      <aside className="fixed inset-y-0 left-0 z-30 hidden w-64 border-r-2 border-line-2 bg-panel/95 backdrop-blur md:block">{sidebar}</aside>

      {/* mobile top bar + drawer */}
      <header className="sticky top-0 z-30 flex items-center gap-3 border-b border-line bg-panel/95 px-4 py-3 backdrop-blur md:hidden">
        <button className="btn-ghost px-2.5 py-1.5" onClick={() => setOpen(true)} aria-label="Open menu">
          ☰
        </button>
        <Logo small />
        <span className="ml-auto text-[13px] text-muted">./{current?.label}</span>
      </header>
      {online === false && (
        <div className="anim-in sticky top-0 z-20 border-b-2 border-bad/50 bg-bad/15 px-4 py-2 text-center text-[13.5px] text-bad">
          Can&apos;t reach the Bodywise server, so changes won&apos;t save. Start the backend (see README) and refresh.
        </div>
      )}
      {open && (
        <div className="fixed inset-0 z-40 md:hidden">
          <div className="anim-fade absolute inset-0 bg-black/60" onClick={() => setOpen(false)} />
          <aside className="anim-sidebar absolute inset-y-0 left-0 w-72 border-r-2 border-line-2 bg-panel">{sidebar}</aside>
        </div>
      )}

      <main key={pathname} className="anim-in mx-auto max-w-6xl px-4 py-6 sm:px-6 lg:py-8">
        {children}
      </main>
    </div>
  );
}

/** Pink pixel logo: a leaf slanting like "/", plus the wordmark (same mark as the favicon). */
export function Logo({ small = false }: { small?: boolean }) {
  return (
    <span className="flex items-center gap-2">
      <span className="pixel-bob">
        <Pixel name="logoLeaf" size={small ? 26 : 34} title="bodywise" />
      </span>
      <span className={`font-pixel leading-none text-pink ${small ? "text-[20px]" : "text-[27px]"}`}>bodywise</span>
    </span>
  );
}

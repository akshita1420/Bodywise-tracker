"use client";

import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { ApiError, api, getToken, setToken } from "@/lib/api";
import type { Profile } from "@/lib/types";
import { Logo } from "@/components/AppShell";
import { Mascot } from "@/components/Pixel";

const EMAIL_KEY = "bw_last_email";

export default function LoginPage() {
  const router = useRouter();
  const [mode, setMode] = useState<"login" | "signup">("login");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [show, setShow] = useState(false);
  const [name, setName] = useState("");
  const [error, setError] = useState<{ msg: string; noAccount?: boolean } | null>(null);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    if (getToken()) {
      router.replace("/");
      return;
    }
    try {
      const last = localStorage.getItem(EMAIL_KEY);
      if (last) setEmail(last);
    } catch {
      /* ignore */
    }
  }, [router]);

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    setBusy(true);
    setError(null);
    try {
      const res = await api<{ token: string; profile: Profile }>(
        `/auth/${mode}`,
        "POST",
        mode === "signup" ? { email: email.trim(), password, name } : { email: email.trim(), password },
      );
      setToken(res.token);
      try {
        localStorage.setItem(EMAIL_KEY, email.trim());
      } catch {
        /* ignore */
      }
      router.replace(mode === "signup" ? "/profile?welcome=1" : "/");
    } catch (err) {
      const msg = err instanceof Error ? err.message : "Something went wrong";
      setError({ msg, noAccount: err instanceof ApiError && err.status === 404 && mode === "login" });
      setBusy(false);
    }
  }

  return (
    <div className="grid min-h-screen place-items-center px-4 py-10">
      <div className="anim-in w-full max-w-md">
        <div className="mb-6 text-center">
          <div className="mb-3 flex justify-center">
            <Mascot size={64} say={mode === "login" ? "welcome back!" : "hi! nice to meet you"} />
          </div>
          <div className="inline-block">
            <Logo />
          </div>
          <p className="mt-1 text-[13.5px] text-muted">weight · meals · habits · groceries</p>
        </div>

        <div className="card overflow-hidden shadow-[0_0_60px_-20px_var(--color-mustard)]">
          {/* fake terminal title bar */}
          <div className="flex items-center gap-1.5 border-b border-line bg-panel-2 px-4 py-2.5">
            <span className="h-2.5 w-2.5 rounded-full bg-bad/80" />
            <span className="h-2.5 w-2.5 rounded-full bg-mustard/80" />
            <span className="h-2.5 w-2.5 rounded-full bg-ok/80" />
            <span className="ml-3 text-[12.5px] text-muted">bodywise — {mode === "login" ? "auth login" : "auth signup"}</span>
          </div>

          <div className="p-6">
            <div className="mb-5 grid grid-cols-2 rounded-md border border-line bg-bg p-1 text-[14px]">
              {(["login", "signup"] as const).map((m) => (
                <button
                  key={m}
                  type="button"
                  onClick={() => {
                    setMode(m);
                    setError(null);
                  }}
                  className={`rounded py-1.5 transition ${mode === m ? "bg-mustard font-bold text-bg" : "text-muted hover:text-ink"}`}
                >
                  {m === "login" ? "sign in" : "create account"}
                </button>
              ))}
            </div>

            <form onSubmit={submit} className="space-y-4">
              {mode === "signup" && (
                <Field label="name">
                  <input className="input w-full" value={name} autoComplete="name" onChange={(e) => setName(e.target.value)} />
                </Field>
              )}
              <Field label="email">
                <input
                  className="input w-full"
                  type="email"
                  required
                  autoComplete="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                />
              </Field>
              <Field label="password">
                <div className="relative">
                  <input
                    className="input w-full pr-14"
                    type={show ? "text" : "password"}
                    required
                    minLength={6}
                    autoComplete={mode === "login" ? "current-password" : "new-password"}
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                  />
                  <button
                    type="button"
                    className="absolute right-2 top-1/2 -translate-y-1/2 text-[12.5px] text-muted hover:text-mustard"
                    onClick={() => setShow(!show)}
                  >
                    {show ? "hide" : "show"}
                  </button>
                </div>
                {mode === "signup" && <span className="mt-1 block text-[12.5px] text-muted">at least 6 characters</span>}
              </Field>

              {error && (
                <div className="anim-in rounded-md border border-bad/40 bg-bad/10 px-3 py-2 text-[14px] text-bad">
                  <span className="font-bold">error:</span> {error.msg}
                  {error.noAccount && (
                    <button
                      type="button"
                      className="mt-1 block text-mustard underline"
                      onClick={() => {
                        setMode("signup");
                        setError(null);
                      }}
                    >
                      → create an account with this email
                    </button>
                  )}
                </div>
              )}

              <button className="btn-primary w-full py-2.5 font-bold" disabled={busy}>
                {busy ? <span className="caret">working</span> : mode === "login" ? "$ sign in ⏎" : "$ create account ⏎"}
              </button>
            </form>
          </div>
        </div>
        <p className="mt-4 text-center text-[12.5px] text-muted">you stay signed in on this device for 30 days</p>
      </div>
    </div>
  );
}

function Field({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <label className="block">
      <span className="label">{label}</span>
      <div className="mt-1.5">{children}</div>
    </label>
  );
}

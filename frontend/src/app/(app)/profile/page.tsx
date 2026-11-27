"use client";

import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import DatePicker from "@/components/DatePicker";
import PacePicker from "@/components/PacePicker";
import { PageHead, PanelTitle, Spinner } from "@/components/ui";
import { api } from "@/lib/api";
import type { Profile } from "@/lib/types";

type Form = Record<string, string | boolean>;

const toForm = (p: Profile): Form => ({
  name: p.name ?? "",
  birthday: p.birthday ?? "",
  heightCm: p.heightCm?.toString() ?? "",
  sex: p.sex,
  goalWeightKg: p.goalWeightKg?.toString() ?? "",
  activityLevel: p.activityLevel,
  deficitKcal: p.deficitKcal?.toString() ?? "500",
  gymMet: p.gymMet?.toString() ?? "5",
  waterTargetL: p.waterTargetL?.toString() ?? "3.45",
  stepsTarget: p.stepsTarget?.toString() ?? "10000",
  reminderEmail: p.reminderEmail ?? p.email,
  remindersEnabled: p.remindersEnabled,
});

export default function ProfilePage() {
  const router = useRouter();
  const [profile, setProfile] = useState<Profile | null>(null);
  const [f, setF] = useState<Form | null>(null);
  const [welcome, setWelcome] = useState(false);
  const [msg, setMsg] = useState<string | null>(null);

  useEffect(() => {
    setWelcome(new URLSearchParams(window.location.search).has("welcome"));
    api<Profile>("/profile").then((p) => {
      setProfile(p);
      setF(toForm(p));
    });
  }, []);

  if (!f || !profile) return <Spinner />;

  const set = (k: string) => (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) =>
    setF({ ...f, [k]: e.target.type === "checkbox" ? (e.target as HTMLInputElement).checked : e.target.value });
  const num = (k: string) => (f[k] === "" ? null : Number(f[k]));

  async function save(e: React.FormEvent) {
    e.preventDefault();
    setMsg(null);
    try {
      const p = await api<Profile>("/profile", "PUT", {
        name: f!.name || null,
        birthday: f!.birthday || null,
        heightCm: num("heightCm"),
        sex: f!.sex,
        goalWeightKg: num("goalWeightKg"),
        activityLevel: f!.activityLevel,
        deficitKcal: num("deficitKcal"),
        gymMet: num("gymMet"),
        waterTargetL: num("waterTargetL"),
        stepsTarget: num("stepsTarget"),
        reminderEmail: f!.reminderEmail || null,
        remindersEnabled: f!.remindersEnabled,
      });
      setProfile(p);
      setF(toForm(p));
      if (welcome) router.push("/");
      else setMsg("Saved ✓");
    } catch (err) {
      setMsg(err instanceof Error ? err.message : "Could not save");
    }
  }

  return (
    <form onSubmit={save} className="mx-auto max-w-2xl space-y-5">
      <PageHead icon="user"
        cmd={welcome ? "bodywise init" : "bodywise config --edit"}
        title={welcome ? "Welcome! Let's set you up" : "Profile"}
        sub="These drive your calorie-deficit plan. Age updates by itself every birthday. Log your weight on the Today page."
      />

      {/* relative z-20: the birthday picker pops out over the sections below */}
      <section className="card anim-in relative z-20 grid gap-4 p-5 sm:grid-cols-2">
        <div className="sm:col-span-2 -mb-1">
          <PanelTitle>body &amp; goal</PanelTitle>
        </div>
        <Field label="Name"><input className="input w-full" value={f.name as string} onChange={set("name")} /></Field>
        <Field label={`Birthday${profile.age != null ? ` · ${profile.age} yrs` : ""}`}>
          <DatePicker value={f.birthday as string} onChange={(d) => setF({ ...f, birthday: d })} />
        </Field>
        <Field label="Height (cm)"><input className="input w-full" type="number" step="0.5" value={f.heightCm as string} onChange={set("heightCm")} /></Field>
        <Field label="Sex (for the BMR formula)">
          <select className="input w-full" value={f.sex as string} onChange={set("sex")}>
            <option value="FEMALE">Female</option>
            <option value="MALE">Male</option>
          </select>
        </Field>
        <Field label="Goal weight (kg)"><input className="input w-full" type="number" step="0.1" value={f.goalWeightKg as string} onChange={set("goalWeightKg")} /></Field>
        <Field label="Activity outside the gym">
          <select className="input w-full" value={f.activityLevel as string} onChange={set("activityLevel")}>
            <option value="SEDENTARY">Sedentary (desk job)</option>
            <option value="LIGHT">Light (on your feet some)</option>
            <option value="MODERATE">Moderate</option>
            <option value="ACTIVE">Active job</option>
          </select>
        </Field>
        <Field label="Gym intensity (MET)">
          <select className="input w-full" value={f.gymMet as string} onChange={set("gymMet")}>
            <option value="3.5">Light, 3.5 (stretching, easy weights)</option>
            <option value="5">Moderate, 5 (weights, steady cardio)</option>
            <option value="7">Hard, 7 (HIIT, running)</option>
          </select>
        </Field>
      </section>

      <section className="card anim-in p-5" style={{ animationDelay: "40ms" }}>
        <PanelTitle>how fast to lose</PanelTitle>
        <PacePicker
          deficit={Number(f.deficitKcal) || 0}
          onChange={(k) => setF({ ...f, deficitKcal: String(k) })}
          currentWeightKg={profile.currentWeightKg}
          goalWeightKg={num("goalWeightKg")}
          heightCm={num("heightCm")}
          birthday={f.birthday as string}
          sex={f.sex as string}
          activityLevel={f.activityLevel as string}
        />
      </section>

      <section className="card anim-in grid gap-4 p-5 sm:grid-cols-2" style={{ animationDelay: "80ms" }}>
        <div className="sm:col-span-2 -mb-1">
          <PanelTitle>targets &amp; reminders</PanelTitle>
        </div>
        <Field label="Water target (L)"><input className="input w-full" type="number" step="0.05" value={f.waterTargetL as string} onChange={set("waterTargetL")} /></Field>
        <Field label="Steps target"><input className="input w-full" type="number" step="500" value={f.stepsTarget as string} onChange={set("stepsTarget")} /></Field>
        <Field label="Reminder email (7 pm grocery list)">
          <input className="input w-full" type="email" value={f.reminderEmail as string} onChange={set("reminderEmail")} />
        </Field>
        <label className="flex items-center gap-2 self-end pb-2 text-ink-2">
          <input type="checkbox" className="h-4 w-4 accent-[var(--color-mustard)]" checked={f.remindersEnabled as boolean} onChange={set("remindersEnabled")} />
          send me the 7 pm grocery email
        </label>
      </section>

      <div className="flex items-center gap-3">
        <button className="btn-primary">{welcome ? "save & continue →" : "save"}</button>
        {msg && <span className="anim-fade text-[14px] text-ok">{msg}</span>}
      </div>
    </form>
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

import AppShell from "@/components/AppShell";

// Signed-in pages load their data in the browser (the token lives in localStorage),
// so they are opted out of Next's instant-navigation validation.
export const instant = false;

export default function AppLayout({ children }: LayoutProps<"/">) {
  return <AppShell>{children}</AppShell>;
}

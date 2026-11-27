import type { Metadata } from "next";
import { JetBrains_Mono, Pixelify_Sans } from "next/font/google";
import "./globals.css";

const mono = JetBrains_Mono({
  variable: "--font-mono-main",
  subsets: ["latin"],
});

const pixel = Pixelify_Sans({
  variable: "--font-pixel-main",
  subsets: ["latin"],
});

export const metadata: Metadata = {
  title: "Bodywise",
  description: "Weight, meals, habits and groceries in one place",
};

export default function RootLayout({ children }: LayoutProps<"/">) {
  return (
    <html lang="en" className={`${mono.variable} ${pixel.variable} h-full antialiased`}>
      <body className="bg-grid min-h-full font-mono text-[15px]">{children}</body>
    </html>
  );
}

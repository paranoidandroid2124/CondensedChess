import type { Metadata } from "next";
import "./globals.css";
import Navigation from "@/components/Navigation";

export const metadata: Metadata = {
  title: "CondensedChess | Chess Review Like a Book",
  description: "Analyze your games with Practicality Score, Study Score, and book-style narrative powered by engine + LLM"
};

export default function RootLayout({
  children
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="en">
      <body className="bg-ink text-mist font-body">
        <div className="bg-hero-gradient min-h-screen">
          <div className="grid-accent absolute inset-0 pointer-events-none opacity-50" />
          <Navigation />
          <main className="relative z-10">{children}</main>
        </div>
      </body>
    </html>
  );
}

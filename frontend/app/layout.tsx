import type { Metadata } from "next";
import { Merriweather } from "next/font/google";
import "./globals.css";
import React from "react";
import "chessground/assets/chessground.base.css";
import "chessground/assets/chessground.brown.css";
import Navigation from "@/components/Navigation";
import ClientProviders from "@/components/ClientProviders";

const merriweather = Merriweather({
  weight: ["300", "400", "700", "900"],
  subsets: ["latin"],
  variable: "--font-merriweather",
});

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
      <body className={`${merriweather.className} bg-ink text-mist font-body`}>
        <div className="bg-hero-gradient min-h-screen">
          <div className="grid-accent absolute inset-0 pointer-events-none opacity-50" />
          <ClientProviders>
            <Navigation />
            <main className="relative z-10">{children}</main>
          </ClientProviders>
        </div>
      </body>
    </html>
  );
}

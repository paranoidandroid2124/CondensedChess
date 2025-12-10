import type { Metadata } from "next";
import "./globals.css";
import React from "react";
import "chessground/assets/chessground.base.css";
import "chessground/assets/chessground.brown.css";
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
      <head>
        <link href="https://fonts.googleapis.com/css2?family=Merriweather:ital,wght@0,300;0,400;0,700;0,900;1,300;1,400;1,700&display=swap" rel="stylesheet" />
      </head>
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

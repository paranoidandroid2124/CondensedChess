import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "Chess Review | scalachess",
  description: "Upload PGNs, get human-like review summaries and critical moments."
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
          <main className="relative z-10">{children}</main>
        </div>
      </body>
    </html>
  );
}

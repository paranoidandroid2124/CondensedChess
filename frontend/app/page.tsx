import Link from "next/link";
import dynamic from "next/dynamic";

const UploadCard = dynamic(() => import("../components/UploadCard"), { ssr: false });

const features = [
  {
    title: "Deep Review",
    desc: "Engine+LLM pipeline highlights critical moments, PVs, and concept shifts.",
    badge: "ΔWin% + concepts",
    href: "/review/sample"
  },
  {
    title: "Live/Quick",
    desc: "Shallow Stockfish pass for fast feedback during play or streaming.",
    badge: "Fast",
    href: "/review/sample"
  },
  {
    title: "Patterns & Training",
    desc: "Aggregate recent games to find style tags and recommend drills.",
    badge: "Soon",
    href: "/review/sample"
  }
];

const steps = [
  { title: "Upload PGN", desc: "One or many PGNs from lichess/chess.com." },
  { title: "Analyze", desc: "We compute FEN timeline, MultiPV, concepts, critical nodes." },
  { title: "Review", desc: "Board, eval graph, critical branches, LLM summaries ready to share." }
];

export default function Home() {
  return (
    <div className="relative px-6 py-10 sm:px-12 lg:px-16">
      <div className="mx-auto flex max-w-6xl flex-col gap-10">
        <section className="flex flex-col gap-8 rounded-3xl bg-gradient-to-r from-[rgba(91,141,239,0.14)] via-[rgba(61,214,183,0.12)] to-[rgba(139,92,246,0.1)] p-[1px] shadow-glow">
          <div className="glass-card relative overflow-hidden rounded-[28px] p-10 sm:p-14">
            <div className="absolute inset-0 bg-[radial-gradient(circle_at_20%_20%,rgba(139,92,246,0.08),transparent_35%),radial-gradient(circle_at_80%_0%,rgba(61,214,183,0.12),transparent_40%)]" />
            <div className="relative z-10 flex flex-col gap-6 sm:flex-row sm:items-center sm:justify-between">
              <div className="max-w-2xl space-y-4">
                <p className="inline-flex items-center rounded-full bg-white/10 px-3 py-1 text-xs font-semibold uppercase tracking-[0.2em] text-mist">
                  Human-like Review
                </p>
                <h1 className="font-display text-3xl leading-tight sm:text-4xl lg:text-5xl">
                  One-click PGN → coach-level review
                </h1>
                <p className="text-lg text-white/70">
                  Engine + LLM auto-analyze key branches, style, and training points with a clean dashboard.
                </p>
                <div className="flex flex-col gap-3 sm:flex-row sm:items-center">
                  <Link
                    href="#upload"
                    className="inline-flex items-center justify-center rounded-full bg-accent-teal px-5 py-3 text-sm font-semibold text-ink shadow-glow transition hover:scale-[1.01]"
                  >
                    Upload PGN & analyze
                  </Link>
                  <Link
                    href="/review/sample"
                    className="inline-flex items-center justify-center rounded-full border border-white/15 px-5 py-3 text-sm font-semibold text-white/80 transition hover:border-white/35"
                  >
                    View sample review
                  </Link>
                </div>
              </div>
              <div className="glass-card relative w-full max-w-md rounded-2xl border border-white/10 p-6">
                <div className="flex items-center justify-between text-xs text-white/70">
                  <span>Sample Insights</span>
                  <span className="rounded-full bg-white/10 px-2 py-1 text-[11px] font-semibold uppercase tracking-wide">
                    ΔWin% + Concepts
                  </span>
                </div>
                <div className="mt-4 space-y-3 text-sm">
                  <div className="flex justify-between rounded-xl bg-white/5 px-3 py-2">
                    <span className="text-white/80">Critical nodes</span>
                    <span className="font-semibold text-accent-teal">5</span>
                  </div>
                  <div className="flex justify-between rounded-xl bg-white/5 px-3 py-2">
                    <span className="text-white/80">Dynamic spike</span>
                    <span className="font-semibold text-accent-blue">+0.32</span>
                  </div>
                  <div className="flex justify-between rounded-xl bg-white/5 px-3 py-2">
                    <span className="text-white/80">Blunder risk</span>
                    <span className="font-semibold text-[#fda4af]">high</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </section>

        <section className="grid gap-5 md:grid-cols-3">
          {features.map((f) => (
            <Link
              key={f.title}
              href={f.href}
              className="glass-card relative flex flex-col gap-2 rounded-2xl p-6 transition hover:border-white/20 hover:shadow-lg hover:shadow-accent-blue/10"
            >
              <span className="text-xs font-semibold uppercase tracking-[0.16em] text-white/60">
                {f.badge}
              </span>
              <h3 className="font-display text-xl text-white">{f.title}</h3>
              <p className="text-sm text-white/70">{f.desc}</p>
            </Link>
          ))}
        </section>

        <section className="glass-card rounded-3xl p-8">
          <div className="flex flex-col gap-6 md:flex-row md:items-center md:justify-between">
            <div>
              <p className="text-xs uppercase tracking-[0.2em] text-white/60">
                How it works
              </p>
              <h2 className="font-display text-2xl text-white">Pipeline</h2>
            </div>
            <div className="flex flex-wrap gap-2 text-xs">
              <span className="rounded-full bg-white/10 px-3 py-1">Stockfish</span>
              <span className="rounded-full bg-white/10 px-3 py-1">Concept scores</span>
              <span className="rounded-full bg-white/10 px-3 py-1">LLM summary</span>
            </div>
          </div>
          <div className="mt-6 grid gap-4 md:grid-cols-3">
            {steps.map((s, idx) => (
              <div key={s.title} className="rounded-2xl border border-white/10 p-4">
                <div className="mb-3 flex h-10 w-10 items-center justify-center rounded-xl bg-white/10 text-sm font-semibold text-accent-teal">
                  {idx + 1}
                </div>
                <h4 className="text-lg font-semibold text-white">{s.title}</h4>
                <p className="text-sm text-white/70">{s.desc}</p>
              </div>
            ))}
          </div>
        </section>

        <section id="upload">
          <UploadCard />
        </section>
      </div>
    </div>
  );
}

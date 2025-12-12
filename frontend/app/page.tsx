import HeroSection from "../components/landing/HeroSection";
import HowItWorks from "../components/landing/HowItWorks";
import FeatureHighlights from "../components/landing/FeatureHighlights";
import ComparisonTable from "../components/landing/ComparisonTable";
import FAQ from "../components/landing/FAQ";
import Link from "next/link";
import Footer from "../components/landing/Footer";

export default function Home() {
  return (
    <div className="relative px-6 py-10 sm:px-12 lg:px-16">
      <div className="mx-auto flex max-w-6xl flex-col gap-16">
        {/* Hero Section */}
        <HeroSection />

        {/* How It Works */}
        <HowItWorks />

        {/* Feature Highlights */}
        <FeatureHighlights />

        {/* Comparison Table */}
        <ComparisonTable />

        {/* FAQ */}
        <FAQ />

        {/* Pricing Teaser */}
        <section className="glass-card rounded-3xl p-8 sm:p-10 text-center">
          <p className="text-xs uppercase tracking-[0.2em] text-white/60">Pricing</p>
          <h2 className="font-display text-3xl text-white mt-2">Simple, Transparent Pricing</h2>
          <p className="mt-3 text-sm text-white/70">
            <span className="rounded-full bg-accent-teal/20 px-2 py-0.5 text-xs text-accent-teal font-semibold">BETA</span>
            {" "} Currently in Public Beta. All Pro features are <strong>Free</strong> for a limited time.
          </p>
          <div className="mt-6 inline-grid grid-cols-2 gap-4 text-sm max-w-md mx-auto">
            <div className="rounded-xl border border-accent-teal bg-accent-teal/10 p-4">
              <p className="font-semibold text-accent-teal">Beta Access</p>
              <p className="mt-2 text-xs text-white/60">Full Access (Free)</p>
            </div>
            <div className="rounded-xl border border-white/10 bg-white/5 p-4 opacity-60">
              <p className="font-semibold text-white">Pro</p>
              <p className="mt-2 text-xs text-white/60">Coming Soon</p>
            </div>
          </div>
          <Link
            href="/pricing"
            className="mt-6 inline-block text-sm text-accent-teal hover:underline"
          >
            Learn more →
          </Link>
        </section>

        {/* Footer CTA */}
        <section className="text-center py-8">
          <h3 className="font-display text-2xl text-white">Ready to level up your chess study?</h3>
          <p className="mt-2 text-sm text-white/70">Upload your first game and see the difference.</p>
          <Link
            href="/app/dashboard"
            className="mt-6 inline-flex items-center justify-center rounded-full bg-accent-teal px-8 py-4 text-sm font-semibold text-ink shadow-glow transition hover:scale-[1.02]"
          >
            Get Started Free
          </Link>
        </section>
      </div>

      <Footer />
    </div>
  );
}

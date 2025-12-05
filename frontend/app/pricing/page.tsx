import Link from "next/link";

export default function PricingPage() {
    const plans = [
        {
            name: "Free",
            price: "$0",
            period: "forever",
            features: [
                "5 games per month",
                "Standard analysis depth",
                "Practicality & Study Score",
                "Basic LLM summaries",
                "Public share links",
            ],
            cta: "Get Started",
            href: "/app/dashboard",
            highlighted: false,
        },
        {
            name: "Pro",
            price: "$9",
            period: "/month",
            features: [
                "Unlimited game analysis",
                "Deep engine analysis",
                "Full narrative generation",
                "50+ Semantic Tags",
                "Opening Explorer integration",
                "CSV/PGN export",
                "Priority support",
            ],
            cta: "Start Pro",
            href: "/app/dashboard",
            highlighted: true,
        },
        {
            name: "Coach",
            price: "$29",
            period: "/month",
            features: [
                "All Pro features",
                "Team/student account management",
                "Group analysis dashboard",
                "Custom training recommendations",
                "White-label options",
                "Dedicated support",
            ],
            cta: "Contact Us",
            href: "/app/dashboard",
            highlighted: false,
        },
    ];

    return (
        <div className="relative px-6 py-10 sm:px-12 lg:px-16">
            <div className="mx-auto max-w-6xl">
                {/* Header */}
                <div className="mb-12 text-center">
                    <Link href="/" className="inline-block text-sm text-accent-teal hover:underline mb-4">
                        ← Back to Home
                    </Link>
                    <h1 className="font-display text-4xl sm:text-5xl text-white mt-4">Pricing</h1>
                    <p className="mt-4 text-lg text-white/70 max-w-2xl mx-auto">
                        Simple, transparent pricing. Start free, upgrade when you need more.
                    </p>
                </div>

                {/* Pricing Cards */}
                <div className="grid gap-6 md:grid-cols-3">
                    {plans.map((plan) => (
                        <div
                            key={plan.name}
                            className={`glass-card rounded-3xl p-8 ${plan.highlighted ? "border-accent-teal bg-accent-teal/5" : ""
                                }`}
                        >
                            {plan.highlighted && (
                                <span className="inline-block rounded-full bg-accent-teal px-3 py-1 text-xs font-semibold text-ink mb-4">
                                    Most Popular
                                </span>
                            )}
                            <h2 className="font-display text-2xl text-white">{plan.name}</h2>
                            <div className="mt-4 flex items-baseline gap-1">
                                <span className="text-4xl font-bold text-white">{plan.price}</span>
                                <span className="text-sm text-white/60">{plan.period}</span>
                            </div>
                            <ul className="mt-6 space-y-3 text-sm text-white/80">
                                {plan.features.map((feature) => (
                                    <li key={feature} className="flex gap-2">
                                        <span className="text-accent-teal">✓</span>
                                        <span>{feature}</span>
                                    </li>
                                ))}
                            </ul>
                            <Link
                                href={plan.href}
                                className={`mt-8 block text-center rounded-full px-6 py-3 text-sm font-semibold transition hover:scale-[1.02] ${plan.highlighted
                                        ? "bg-accent-teal text-ink shadow-glow"
                                        : "border border-white/20 text-white hover:border-white/40"
                                    }`}
                            >
                                {plan.cta}
                            </Link>
                        </div>
                    ))}
                </div>

                {/* FAQ Section */}
                <div className="mt-16 glass-card rounded-3xl p-8 sm:p-10">
                    <h2 className="font-display text-2xl text-white mb-6">Pricing FAQ</h2>
                    <div className="space-y-4 text-sm">
                        <div>
                            <p className="font-semibold text-white">Q: Is the free plan sufficient?</p>
                            <p className="mt-1 text-white/70">
                                A: If you analyze 1-2 games per week, the free plan is enough. For deeper analysis or unlimited games, we recommend Pro.
                            </p>
                        </div>
                        <div>
                            <p className="font-semibold text-white">Q: Can I cancel anytime?</p>
                            <p className="mt-1 text-white/70">
                                A: Yes, cancel anytime with no penalties.
                            </p>
                        </div>
                        <div>
                            <p className="font-semibold text-white">Q: Who is the Coach plan for?</p>
                            <p className="mt-1 text-white/70">
                                A: For coaches teaching students or clubs requiring team-level analysis.
                            </p>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
}

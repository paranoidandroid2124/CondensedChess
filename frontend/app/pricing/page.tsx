import Link from "next/link";
import Footer from "../../components/landing/Footer";

export default function PricingPage() {
    const plans = [
        {
            name: "Beta Access",
            price: "$0",
            period: "during beta",
            description: "Full access to all Pro features while we polish the experience.",
            features: [
                "Unlimited game analysis",
                "Deep engine analysis",
                "Practicality & Study Scores",
                "Full narrative generation",
                "50+ Semantic Tags",
                "Opening Explorer integration",
                "Priority support",
            ],
            cta: "Start Analyzing Now",
            href: "/app/dashboard",
            highlighted: true,
            status: "active"
        },
        {
            name: "Pro",
            price: "TBA",
            period: "/month",
            description: "The official release plan. Coming after beta concludes.",
            features: [
                "Everything in Beta",
                "Cloud Storage for 10,000+ games",
                "Team management features",
                "Advanced PDF exports",
                "Personalized training drills",
            ],
            cta: "Coming Soon",
            href: "#",
            highlighted: false,
            status: "disabled"
        }
    ];

    return (
        <div className="relative flex flex-col min-h-screen">
            <div className="flex-grow px-6 py-10 sm:px-12 lg:px-16">
                <div className="mx-auto max-w-6xl">
                    {/* Header */}
                    <div className="mb-12 text-center">
                        <Link href="/" className="inline-block text-sm text-accent-teal hover:underline mb-4">
                            ← Back to Home
                        </Link>
                        <h1 className="font-display text-4xl sm:text-5xl text-white mt-4">Pricing</h1>
                        <p className="mt-4 text-lg text-white/70 max-w-2xl mx-auto">
                            We are currently in <span className="text-accent-teal font-semibold">Public Beta</span>.
                            <br />
                            Enjoy full access to all features for free.
                        </p>
                    </div>

                    {/* Pricing Cards */}
                    <div className="grid gap-8 md:grid-cols-2 max-w-4xl mx-auto">
                        {plans.map((plan) => (
                            <div
                                key={plan.name}
                                className={`glass-card flex flex-col rounded-3xl p-8 transition-all ${plan.highlighted
                                    ? "border-accent-teal bg-accent-teal/5 shadow-glow scale-[1.02]"
                                    : "border-white/10 bg-white/5 opacity-80 hover:opacity-100"
                                    }`}
                            >
                                {plan.highlighted && (
                                    <span className="self-start rounded-full bg-accent-teal px-3 py-1 text-xs font-semibold text-ink mb-4">
                                        Current Status
                                    </span>
                                )}
                                <h2 className="font-display text-3xl text-white">{plan.name}</h2>
                                <p className="mt-2 text-sm text-white/60 min-h-[40px]">{plan.description}</p>

                                <div className="mt-6 flex items-baseline gap-1">
                                    <span className="text-4xl font-bold text-white">{plan.price}</span>
                                    <span className="text-sm text-white/60">{plan.period}</span>
                                </div>

                                <ul className="mt-8 space-y-4 text-sm text-white/80 flex-grow">
                                    {plan.features.map((feature) => (
                                        <li key={feature} className="flex gap-3">
                                            <span className={`text-lg ${plan.status === 'disabled' ? 'text-white/30' : 'text-accent-teal'}`}>✓</span>
                                            <span className={plan.status === 'disabled' ? 'text-white/50' : ''}>{feature}</span>
                                        </li>
                                    ))}
                                </ul>

                                <div className="mt-8">
                                    {plan.status === 'active' ? (
                                        <Link
                                            href={plan.href}
                                            className="block w-full text-center rounded-full bg-accent-teal px-6 py-4 text-sm font-semibold text-ink shadow-glow transition hover:scale-[1.02]"
                                        >
                                            {plan.cta}
                                        </Link>
                                    ) : (
                                        <button
                                            disabled
                                            className="block w-full text-center rounded-full border border-white/10 bg-white/5 px-6 py-4 text-sm font-semibold text-white/40 cursor-not-allowed"
                                        >
                                            {plan.cta}
                                        </button>
                                    )}
                                </div>
                            </div>
                        ))}
                    </div>

                    {/* FAQ Section */}
                    <div className="mt-20 glass-card rounded-3xl p-8 sm:p-10 max-w-4xl mx-auto">
                        <h2 className="font-display text-2xl text-white mb-6">Beta FAQ</h2>
                        <div className="space-y-6 text-sm">
                            <div>
                                <p className="font-semibold text-white">Q: Is it really free?</p>
                                <p className="mt-2 text-white/70 leading-relaxed">
                                    A: Yes! During the beta period, we want as much feedback as possible. You get full access to the engine analysis, narrative generation, and scoring systems without any credit card required.
                                </p>
                            </div>
                            <div>
                                <p className="font-semibold text-white">Q: What happens when beta ends?</p>
                                <p className="mt-2 text-white/70 leading-relaxed">
                                    A: We will give plenty of notice before introducing paid plans. Your game data will remain yours, and early beta testers may receive special founder pricing.
                                </p>
                            </div>
                            <div>
                                <p className="font-semibold text-white">Q: How can I provide feedback?</p>
                                <p className="mt-2 text-white/70 leading-relaxed">
                                    A: We&apos;d love to hear from you! Please reach out to use via the support email.
                                </p>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
            <Footer />
        </div>
    );
}

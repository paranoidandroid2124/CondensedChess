import FAQ from "../../components/landing/FAQ";
import Link from "next/link";

export default function FAQPage() {
    return (
        <div className="relative px-6 py-10 sm:px-12 lg:px-16">
            <div className="mx-auto max-w-4xl">
                {/* Header */}
                <div className="mb-12 text-center">
                    <Link href="/" className="inline-block text-sm text-accent-teal hover:underline mb-4">
                        ← Back to Home
                    </Link>
                    <h1 className="font-display text-4xl sm:text-5xl text-white mt-4">FAQ</h1>
                    <p className="mt-4 text-lg text-white/70">
                        Answers to common questions.
                    </p>
                </div>

                {/* FAQ Component */}
                <FAQ />

                {/* Additional Support */}
                <div className="mt-12 glass-card rounded-3xl p-8 text-center">
                    <h2 className="font-display text-2xl text-white">Still have questions?</h2>
                    <p className="mt-2 text-sm text-white/70">
                        Feel free to reach out anytime.
                    </p>
                    <a
                        href="mailto:support@chesstory.com"
                        className="mt-6 inline-flex items-center justify-center rounded-full border border-white/20 px-6 py-3 text-sm font-semibold text-white transition hover:border-white/40"
                    >
                        Contact Support
                    </a>
                </div>
            </div>
        </div>
    );
}

"use client";

import { useState } from "react";

export default function FAQ() {
    const [openIndex, setOpenIndex] = useState<number | null>(null);

    const faqs = [
        {
            q: "How is this different from other chess analysis tools?",
            a: "Most chess platforms excel at finding mistakes. Chesstory goes further: we tell you *which* mistakes to study, how dangerous a move is in practice (Practicality Score), and organize everything like a chess textbook with narrative commentary.",
        },
        {
            q: "How does this compare to raw engine analysis?",
            a: "Engines calculate the best moves. We take engine results and add Practicality Score (tolerance for mistakes), Study Score (learning value), Semantic Tags (position characteristics), and LLM-generated human-friendly explanations.",
        },
        {
            q: "Where is my game data stored?",
            a: "Currently stored in local browser storage or on our server. When we introduce authentication, your data will be securely managed in a user-specific database. Data is used only for learning purposes and never shared with third parties.",
        },
        {
            q: "How deep is the analysis?",
            a: "We use professional-strength engine analysis for all positions. Critical moments get even deeper analysis with more variations. The LLM explains the strategic context for each moment. Pro plans offer the deepest analysis available.",
        },
        {
            q: "Who is this for?",
            a: "Players rated 1200-2400 benefit the most. Beginners can start with foundational tactics, and advanced players can study subtle strategic moments. Especially recommended if you're unsure 'what to study next.'",
        },
    ];

    return (
        <section className="glass-card rounded-3xl p-8 sm:p-10">
            <div className="mb-8">
                <p className="text-xs uppercase tracking-[0.2em] text-white/60">FAQ</p>
                <h2 className="font-display text-3xl text-white">Frequently Asked Questions</h2>
            </div>
            <div className="space-y-3">
                {faqs.map((faq, idx) => (
                    <div key={idx} className="rounded-xl border border-white/10 bg-white/5 overflow-hidden">
                        <button
                            onClick={() => setOpenIndex(openIndex === idx ? null : idx)}
                            className="flex w-full items-center justify-between p-4 text-left transition hover:bg-white/10"
                        >
                            <span className="font-semibold text-white pr-4">{faq.q}</span>
                            <span className="text-accent-teal text-xl transition-transform" style={{
                                transform: openIndex === idx ? "rotate(45deg)" : "rotate(0deg)"
                            }}>
                                +
                            </span>
                        </button>
                        {openIndex === idx && (
                            <div className="border-t border-white/10 p-4 text-sm leading-relaxed text-white/70">
                                {faq.a}
                            </div>
                        )}
                    </div>
                ))}
            </div>
        </section>
    );
}

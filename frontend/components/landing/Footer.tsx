import Link from "next/link";
import Image from "next/image";

export default function Footer() {
    return (
        <footer className="mt-20 border-t border-white/10 bg-slate-950 py-12">
            <div className="mx-auto max-w-6xl px-6 sm:px-12 lg:px-16">
                <div className="flex flex-col gap-8 md:flex-row md:items-center md:justify-between">
                    <div className="space-y-4">
                        <Link href="/" className="flex items-center gap-2">
                            <Image
                                src="/logo.png"
                                alt="Chesstory Logo"
                                width={32}
                                height={32}
                                className="h-8 w-8"
                            />
                            <span className="font-display text-xl font-bold text-white">Chesstory</span>
                        </Link>
                        <p className="text-sm text-white/50 max-w-xs">
                            Turn your chess games into a story.
                            <br />
                            Powered by Stockfish & LLMs.
                        </p>
                    </div>

                    <div className="flex flex-wrap gap-8 text-sm text-white/70">
                        <Link href="/" className="hover:text-accent-teal transition-colors">Home</Link>
                        <Link href="/app/dashboard" className="hover:text-accent-teal transition-colors">Dashboard</Link>
                        <Link href="/pricing" className="hover:text-accent-teal transition-colors">Pricing</Link>
                        <Link href="/legal" className="hover:text-accent-teal transition-colors">Legal & Credits</Link>
                    </div>
                </div>

                <div className="mt-12 flex flex-col gap-4 border-t border-white/5 pt-8 text-xs text-white/30 md:flex-row md:justify-between">
                    <p>&copy; {new Date().getFullYear()} Chesstory. All rights reserved.</p>
                    <p>
                        Chess analysis powered by <a href="https://stockfishchess.org" target="_blank" rel="noopener noreferrer" className="hover:text-white/50 underline">Stockfish</a>.
                        Board UI by <a href="https://lichess.org" target="_blank" rel="noopener noreferrer" className="hover:text-white/50 underline">Lichess</a> (Chessground).
                    </p>
                </div>
            </div>
        </footer>
    );
}

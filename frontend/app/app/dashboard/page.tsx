import Link from "next/link";
import dynamic from "next/dynamic";

const UploadCard = dynamic(() => import("../../../components/UploadCard"), { ssr: false });

export default function DashboardPage() {
    // Mock data - in real app, this would come from API/database
    const stats = {
        gamesThisWeek: 12,
        studyPoints: 34,
    };

    const topStudyPoints = [
        {
            id: "game1",
            move: 15,
            notation: "15.Nd5!",
            studyScore: 9.1,
            game: "vs. Player123",
            description: "Tactical breakthrough in Reversed Sicilian",
            tags: ["Tactical Blow", "Initiative Gain"],
        },
        {
            id: "game2",
            move: 23,
            notation: "23...Qg5?",
            studyScore: 8.9,
            game: "vs. Player456",
            description: "King safety crisis - premature attack",
            tags: ["King Safety Crisis", "Blunder"],
        },
        {
            id: "game1",
            move: 9,
            notation: "9...d5?!",
            studyScore: 8.4,
            game: "vs. Player123",
            description: "Positional mistake in pawn structure",
            tags: ["Positional Mistake", "Pawn Structure"],
        },
        {
            id: "game3",
            move: 28,
            notation: "28.Rxe6",
            studyScore: 7.8,
            game: "vs. Player789",
            description: "Exchange sacrifice for compensation",
            tags: ["Sacrifice", "Dynamic Compensation"],
        },
        {
            id: "game2",
            move: 12,
            notation: "12.Bg5",
            studyScore: 7.5,
            game: "vs. Player456",
            description: "Plan transition to attacking play",
            tags: ["Plan Transition", "Attacking Setup"],
        },
    ];

    const recentGames = [
        {
            id: "game1",
            white: "You",
            black: "Player123",
            result: "1-0",
            opening: "Reversed Sicilian",
            date: "2 hours ago",
        },
        {
            id: "game2",
            white: "Player456",
            black: "You",
            result: "0-1",
            opening: "Ruy Lopez",
            date: "1 day ago",
        },
        {
            id: "game3",
            white: "You",
            black: "Player789",
            result: "1/2-1/2",
            opening: "Queen's Gambit",
            date: "2 days ago",
        },
    ];

    return (
        <div className="relative px-6 py-10 sm:px-12 lg:px-16">
            <div className="mx-auto max-w-7xl">
                {/* Header */}
                <div className="mb-8">
                    <h1 className="font-display text-3xl sm:text-4xl text-white">Dashboard</h1>
                    <p className="mt-2 text-sm text-white/70">
                        Your personalized chess study hub
                    </p>
                </div>

                {/* Stats Cards */}
                <div className="grid gap-4 md:grid-cols-2 mb-8">
                    <div className="glass-card rounded-2xl p-6">
                        <p className="text-xs uppercase tracking-[0.2em] text-white/60">This Week</p>
                        <p className="mt-2 text-3xl font-bold text-white">{stats.gamesThisWeek}</p>
                        <p className="mt-1 text-sm text-white/70">Games Analyzed</p>
                    </div>
                    <div className="glass-card rounded-2xl p-6">
                        <p className="text-xs uppercase tracking-[0.2em] text-white/60">Study Queue</p>
                        <p className="mt-2 text-3xl font-bold text-accent-teal">{stats.studyPoints}</p>
                        <p className="mt-1 text-sm text-white/70">Recommended Study Points</p>
                    </div>
                </div>

                <div className="grid gap-8 lg:grid-cols-3">
                    {/* Main: Top Study Points */}
                    <div className="lg:col-span-2 space-y-4">
                        <div className="flex items-center justify-between">
                            <h2 className="font-display text-2xl text-white">Top Study Points</h2>
                            <span className="text-xs text-white/60">Sorted by Study Score</span>
                        </div>
                        <div className="space-y-3">
                            {topStudyPoints.map((point, idx) => (
                                <Link
                                    key={idx}
                                    href={`/review/${point.id}?move=${point.move}`}
                                    className="glass-card group block rounded-2xl p-5 transition hover:border-white/25 hover:bg-white/5"
                                >
                                    <div className="flex items-start justify-between gap-4">
                                        <div className="flex-1">
                                            <div className="flex items-center gap-3 mb-2">
                                                <span className="font-mono text-lg font-semibold text-accent-teal">
                                                    {point.notation}
                                                </span>
                                                <span className="rounded-full bg-accent-blue/20 px-2.5 py-0.5 text-xs font-semibold text-accent-blue">
                                                    Study {point.studyScore}
                                                </span>
                                            </div>
                                            <p className="text-sm text-white/80">{point.description}</p>
                                            <p className="mt-1 text-xs text-white/60">{point.game}</p>
                                            <div className="mt-2 flex flex-wrap gap-1.5">
                                                {point.tags.map((tag) => (
                                                    <span key={tag} className="rounded-md bg-white/10 px-2 py-0.5 text-xs text-white/70">
                                                        {tag}
                                                    </span>
                                                ))}
                                            </div>
                                        </div>
                                        <span className="text-white/40 transition group-hover:text-accent-teal">→</span>
                                    </div>
                                </Link>
                            ))}
                        </div>
                    </div>

                    {/* Sidebar: Recent Games + Upload */}
                    <div className="space-y-6">
                        {/* Upload Section */}
                        <div>
                            <h2 className="font-display text-xl text-white mb-4">Analyze New Game</h2>
                            <UploadCard />
                        </div>

                        {/* Recent Games */}
                        <div>
                            <h2 className="font-display text-xl text-white mb-4">Recent Games</h2>
                            <div className="space-y-3">
                                {recentGames.map((game) => (
                                    <Link
                                        key={game.id}
                                        href={`/review/${game.id}`}
                                        className="glass-card block rounded-xl p-4 transition hover:border-white/25"
                                    >
                                        <div className="flex items-center justify-between mb-2">
                                            <span className="text-sm font-semibold text-white">
                                                {game.white} vs {game.black}
                                            </span>
                                            <span className="text-xs font-semibold text-white/80">{game.result}</span>
                                        </div>
                                        <p className="text-xs text-white/60">{game.opening}</p>
                                        <p className="mt-1 text-xs text-white/50">{game.date}</p>
                                    </Link>
                                ))}
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
}

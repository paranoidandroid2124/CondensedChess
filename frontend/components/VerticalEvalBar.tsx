import { formatEvaluation } from "../lib/eval";

type VerticalEvalBarProps = {
    evalPercent?: number; // 0 (Black winning) to 100 (White winning), 50 is equal
    cp?: number;
    mate?: number;
    orientation?: "white" | "black";
    conceptMarkers?: Array<{
        yPercent: number; // 0 to 100
        color: string;
        label: string;
    }>;
};

export function VerticalEvalBar({
    evalPercent = 50,
    cp,
    mate,
    orientation = "white",
    conceptMarkers = []
}: VerticalEvalBarProps) {
    // If Mate is present, override height to 100% (White wins) or 0% (Black wins)
    // Note: mate > 0 means White mates, mate < 0 means Black mates (standard convention)
    const effectivePercent = mate !== undefined
        ? (mate > 0 ? 100 : 0)
        : evalPercent;

    // Clamp between 5% and 95% to always show a bit of the losing side color
    const clampedEval = Math.max(5, Math.min(95, effectivePercent));

    // If orientation is black, we flip the visual representation if needed, 
    // but usually evalPercent is "White's winning chance". 
    // Standard: White is top (100%), Black is bottom (0%).
    // If flipped board (Black at bottom), usually we still keep White at top for consistency,
    // or flip it? Lichess keeps White at top always for the bar.
    // Let's stick to White = Top (100%), Black = Bottom (0%).

    const heightPercent = clampedEval;

    // Determine text to show
    const evalText = formatEvaluation(cp, mate);

    return (
        <div className="relative h-full w-6 select-none overflow-hidden rounded bg-white/10">
            {/* Background is Black's winning color (dark grey/black) */}
            <div className="absolute inset-0 bg-[#403d39]" />

            {/* White's winning color bar (white/light grey) */}
            <div
                className="absolute top-0 w-full bg-[#f0f0f0] transition-all duration-500 ease-in-out"
                style={{ height: `${heightPercent}%` }}
            />

            {/* Middle marker */}
            <div className="absolute top-1/2 left-0 h-[1px] w-full bg-black/20" />

            {/* Evaluation Text Label */}
            <div className={`absolute left-0 w-full text-center text-[10px] font-bold ${heightPercent > 50 ? 'bottom-1 text-[#403d39]' : 'top-1 text-[#f0f0f0]'}`}>
                {evalText}
            </div>

            {/* Concept Markers */}
            {conceptMarkers.map((marker, idx) => (
                <div
                    key={idx}
                    className="absolute right-0 h-2 w-2 translate-x-1/2 rounded-full border border-black/20 shadow-sm"
                    style={{
                        top: `${100 - marker.yPercent}%`,
                        backgroundColor: marker.color
                    }}
                    title={marker.label}
                />
            ))}
        </div>
    );
}

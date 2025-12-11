import type { Concepts, Review } from "../../types/review";
import { ConceptCards } from "../ConceptCards";
import { EvalSparkline } from "./EvalSparkline";
import { formatSanHuman } from "../../lib/review-format";
import { displayTag, phaseOf } from "../../lib/review-tags";
import { CollapsibleSection } from "../common/CollapsibleSection";
import type { TimelineNode } from "../../types/review";
import { ConceptTrendChart } from "./ConceptTrendChart";

export function SummaryPanel({
  opening,
  openingStats,
  oppositeColorBishops,
  concepts,
  conceptSpikes,
  showAdvanced,
  summaryText,
  openingSummary,
  bookExitComment,
  openingTrend,
  onSelectPly,
  timeline,
  currentPly
}: {
  opening?: Review["opening"];
  openingStats?: Review["openingStats"];
  oppositeColorBishops?: boolean;
  concepts?: Concepts;
  conceptSpikes?: Array<{ ply: number; concept: string; delta: number; label: string }>;
  showAdvanced: boolean;
  summaryText?: string;
  openingSummary?: string;
  bookExitComment?: string;
  openingTrend?: string;
  onSelectPly?: (ply: number) => void;
  timeline?: TimelineNode[];
  currentPly?: number;
}) {
  const chartData = timeline?.map(node => ({
    ply: node.ply,
    dynamic: node.concepts?.dynamic,
    kingSafety: node.concepts?.kingSafety,
    tacticalDepth: node.concepts?.tacticalDepth,
    comfortable: node.concepts?.comfortable
  })) ?? [];
  return (
    <div className="glass-card rounded-3xl p-5 animate-fade-in">
      <div className="flex items-center gap-3">
        <div className="h-9 w-9 rounded-xl bg-accent-teal/20 text-accent-teal grid place-items-center font-semibold">
          Σ
        </div>
        <div>
          <p className="text-xs uppercase tracking-[0.2em] text-content-secondary">Summary</p>
          <h3 className="text-lg font-semibold text-content-primary">Game overview</h3>
        </div>
      </div>

      {summaryText ? (
        <p className="mt-3 text-sm text-content-secondary">{summaryText}</p>
      ) : (
        <p className="mt-3 text-xs text-content-tertiary">No summary provided by server.</p>
      )}

      {/* Concept Trend Chart */}
      {chartData.length > 0 && (
        <div className="mt-4 rounded-2xl border border-surface-highlight bg-surface-elevated p-4">
          <p className="mb-2 text-xs font-semibold text-content-secondary">Concept Trends</p>
          <ConceptTrendChart
            data={chartData}
            currentPly={currentPly}
            height={140}
          />
        </div>
      )}


      < div className="mt-4 grid gap-3 sm:grid-cols-2">
        <div className="rounded-2xl border border-surface-highlight bg-surface-elevated p-3">
          <p className="text-xs text-content-secondary">Opening</p>
          <p className="text-sm font-semibold text-content-primary">{opening?.name ?? "Unknown"}</p>
          {opening?.eco ? <p className="text-xs text-content-secondary">{opening.eco}</p> : null}
        </div>
        <div className="rounded-2xl border border-surface-highlight bg-surface-elevated p-3">
          <p className="text-xs text-content-secondary">Opening stats</p>
          <p className="text-sm font-semibold text-content-primary">
            {openingStats?.games ? `${openingStats.games} games` : "N/A"}{" "}
            {openingStats?.winWhite != null ? `W ${openingStats.winWhite}%` : ""}{" "}
            {openingStats?.draw != null ? `D ${openingStats.draw}%` : ""}{" "}
            {openingStats?.winBlack != null ? `B ${openingStats.winBlack}%` : ""}
          </p>
        </div>
      </div>

      {
        openingSummary ? (
          <div className="mt-3 rounded-2xl border border-surface-highlight bg-surface-elevated p-3 text-sm text-content-secondary">{openingSummary}</div>
        ) : null
      }
      {
        bookExitComment ? (
          <div className="mt-2 rounded-2xl border border-surface-highlight bg-surface-elevated p-3 text-xs text-content-secondary">{bookExitComment}</div>
        ) : null
      }
      {
        openingTrend ? (
          <div className="mt-2 rounded-2xl border border-surface-highlight bg-surface-elevated p-3 text-xs text-content-secondary">{openingTrend}</div>
        ) : null
      }

      {
        conceptSpikes && conceptSpikes.length ? (
          <CollapsibleSection title="Concept spikes" defaultOpen className="mt-4">
            <div className="space-y-2 text-sm text-content-secondary">
              {conceptSpikes.map((s) => (
                <button
                  key={s.ply}
                  onClick={() => onSelectPly?.(s.ply)}
                  className="flex w-full items-center justify-between rounded-xl border border-surface-highlight bg-surface-elevated px-3 py-2 text-left hover:border-accent-teal/50 transition-colors duration-200"
                >
                  <span>
                    <span className="font-semibold text-accent-teal">{s.concept}</span>{" "}
                    <span className="text-content-secondary">at ply {s.ply} ({s.label})</span>
                  </span>
                  <span className="text-xs text-content-tertiary">+{s.delta.toFixed(2)}</span>
                </button>
              ))}
            </div>
          </CollapsibleSection>
        ) : null
      }

      {
        concepts ? (
          <div className="mt-4">
            <ConceptCards concepts={concepts} />
          </div>
        ) : null
      }

      {
        oppositeColorBishops ? (
          <div className="mt-3 rounded-2xl border border-surface-highlight bg-surface-elevated p-3 text-xs text-content-secondary">
            Opposite-colored bishops detected — expect drawing chances.
          </div>
        ) : null
      }
    </div >
  );
}

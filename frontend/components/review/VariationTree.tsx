import React from "react";
import type { ReviewTreeNode } from "../../types/review";
import { formatPvList, formatSanHuman } from "../../lib/review-format";

interface VariationTreeProps {
  root?: ReviewTreeNode;
  onSelect: (ply: number) => void;
  selected?: number;
}

export function VariationTree({ root, onSelect, selected }: VariationTreeProps) {
  if (!root) return null;

  const renderNode = (node: ReviewTreeNode, depth: number, isMainline: boolean): JSX.Element => {
    const mainlineChildren = node.children.filter((c) => c.judgement !== "variation");
    const variations = node.children.filter((c) => c.judgement === "variation");
    const isSelected = selected === node.ply;
    return (
      <div key={`${node.ply}-${node.uci}-${depth}`} className="mb-1">
        <button
          onClick={() => onSelect(node.ply)}
          className={`group flex w-full items-center gap-2 rounded-lg px-2 py-1 text-left ${
            isSelected ? "bg-white/10 ring-1 ring-accent-teal/60" : "hover:bg-white/5"
          }`}
        >
          <span className="text-[10px] text-white/50" style={{ width: 34, visibility: isMainline ? "visible" : "hidden" }}>
            {node.ply}.
          </span>
          <div className={`flex flex-1 items-center gap-2 ${isMainline ? "font-semibold text-white" : "text-white/80"}`}>
            <span className="text-white">{formatSanHuman(node.san)}</span>
            {node.glyph ? <span className="text-xs text-white/60">{node.glyph}</span> : null}
            {node.comment ? (
              <span className="text-[11px] text-white/60 whitespace-normal leading-snug break-words">{node.comment}</span>
            ) : null}
          </div>
        </button>
        {variations.length ? (
          <div className="ml-4 space-y-1 border-l border-white/10 pl-3">
            {variations.map((v, idx) => (
              <div key={`${v.ply}-var-${idx}`} className="flex items-start gap-2">
                <span className="text-[11px] text-white/50">↳</span>
                <button
                  onClick={() => onSelect(v.ply)}
                  className={`flex flex-col rounded-lg px-2 py-1 text-left ${
                    selected === v.ply ? "bg-white/10 ring-1 ring-accent-teal/50" : "hover:bg-white/5"
                  }`}
                >
                  <div className="flex items-center gap-2 text-sm text-white/80">
                    <span className="text-white">{formatSanHuman(v.san)}</span>
                    {v.glyph ? <span className="text-xs text-white/60">{v.glyph}</span> : null}
                  </div>
                  {v.pv?.length ? <div className="text-[11px] text-white/50">{formatPvList(v.pv)}</div> : null}
                  {v.comment ? <div className="text-[11px] text-white/60">{v.comment}</div> : null}
                </button>
              </div>
            ))}
          </div>
        ) : null}
        {mainlineChildren.length
          ? mainlineChildren.map((c) => (
              <div key={`${c.ply}-main`} className="ml-2 border-l border-white/5 pl-2">
                {renderNode(c, depth + 1, true)}
              </div>
            ))
          : null}
      </div>
    );
  };

  return (
    <div className="glass-card rounded-2xl p-4">
      <div className="mb-2 flex items-center justify-between">
        <h3 className="text-sm font-semibold text-white/80">Tree</h3>
        <span className="text-xs text-white/60">mainline + variations</span>
      </div>
      <div className="min-h-[320px] max-h-[70vh] lg:max-h-[calc(100vh-240px)] overflow-y-auto pr-2">
        {renderNode(root, 0, true)}
      </div>
    </div>
  );
}

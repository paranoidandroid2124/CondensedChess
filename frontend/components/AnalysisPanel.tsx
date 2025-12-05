import React, { useState } from "react";

export type TabId = "opening" | "moves" | "tree" | "study" | "concepts";

type TabConfig = { id: TabId; label: string; icon?: string };

type AnalysisPanelProps = {
  children: React.ReactNode;
  tabs?: TabConfig[];
  activeTab?: TabId;
  onTabChange?: (tab: TabId) => void;
};

export function AnalysisPanel({ children, tabs, activeTab: controlledTab, onTabChange }: AnalysisPanelProps) {
  const [localTab, setLocalTab] = useState<TabId>("concepts");
  const activeTab = controlledTab ?? localTab;
  const setActiveTab = onTabChange ?? setLocalTab;

  const tabList: TabConfig[] =
    tabs ??
    [
      { id: "opening", label: "Opening", icon: "📖" },
      { id: "moves", label: "Moves", icon: "↔️" },
      { id: "tree", label: "Tree", icon: "🌿" },
      { id: "study", label: "Study", icon: "🎓" },
      { id: "concepts", label: "Concepts", icon: "💡" }
    ];

  return (
    <div className="flex h-full flex-col overflow-hidden rounded-2xl border border-white/10 bg-white/5">
      <div className="flex flex-wrap border-b border-white/10 bg-white/5">
        {tabList.map((tab) => (
          <button
            key={tab.id}
            onClick={() => setActiveTab(tab.id)}
            className={`
              flex-1 px-3 py-3 text-xs font-medium transition-colors
              ${activeTab === tab.id
                ? "border-b-2 border-accent-teal text-white bg-white/5"
                : "text-white/60 hover:bg-white/5 hover:text-white/80"}
            `}
          >
            {tab.icon ? <span className="mr-1.5">{tab.icon}</span> : null}
            {tab.label}
          </button>
        ))}
      </div>

      <div className="flex-1 overflow-y-auto p-4">
        <div className="space-y-4">{children}</div>
      </div>
    </div>
  );
}

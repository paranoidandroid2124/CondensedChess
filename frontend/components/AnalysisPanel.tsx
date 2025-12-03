import React, { useState } from "react";

type TabId = "engine" | "opening" | "study" | "concepts";

type AnalysisPanelProps = {
    children: React.ReactNode; // We can pass the existing SummaryPanel/CriticalList as children for now
    activeTab?: TabId;
    onTabChange?: (tab: TabId) => void;
};

export function AnalysisPanel({ children, activeTab: controlledTab, onTabChange }: AnalysisPanelProps) {
    const [localTab, setLocalTab] = useState<TabId>("engine");
    const activeTab = controlledTab ?? localTab;
    const setActiveTab = onTabChange ?? setLocalTab;

    const tabs: { id: TabId; label: string; icon: string }[] = [
        { id: "engine", label: "Engine", icon: "🔍" },
        { id: "opening", label: "Opening", icon: "📖" },
        { id: "study", label: "Study", icon: "🎓" },
        { id: "concepts", label: "Concepts", icon: "💡" },
    ];

    return (
        <div className="flex h-full flex-col overflow-hidden rounded-2xl border border-white/10 bg-white/5">
            {/* Tab Header */}
            <div className="flex border-b border-white/10 bg-white/5">
                {tabs.map((tab) => (
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
                        <span className="mr-1.5">{tab.icon}</span>
                        {tab.label}
                    </button>
                ))}
            </div>

            {/* Tab Content Area */}
            <div className="flex-1 overflow-y-auto p-4">
                {/* For Phase 1, we just render children (Summary/CriticalList) regardless of tab, 
            or we can conditionally render if we had separate content ready. 
            The plan says "Initially will just house the existing SummaryPanel and CriticalList content within tabs."
            So let's just render children for now, and maybe add a placeholder for empty tabs.
        */}
                <div className="space-y-4">
                    {children}
                </div>
            </div>
        </div>
    );
}

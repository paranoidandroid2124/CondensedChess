'use client';

import React, { useMemo } from 'react';

interface ConceptData {
    ply: number;
    dynamic?: number;
    kingSafety?: number;
    tacticalDepth?: number;
    comfortable?: number;
}

interface ConceptTrendChartProps {
    data: ConceptData[];
    currentPly?: number;
    height?: number;
}

const CONCEPT_COLORS = {
    dynamic: '#ff6b6b',
    kingSafety: '#feca57',
    tacticalDepth: '#48dbfb',
    comfortable: '#1dd1a1',
};

const CONCEPT_LABELS = {
    dynamic: 'Dynamic',
    kingSafety: 'King Danger',
    tacticalDepth: 'Tactical Depth',
    comfortable: 'Comfort',
};



const concepts = ['dynamic', 'kingSafety', 'tacticalDepth', 'comfortable'] as const;

export function ConceptTrendChart({
    data,
    currentPly,
    height = 120
}: ConceptTrendChartProps) {

    const chartData = useMemo(() => {
        if (data.length === 0) return null;

        const width = 100; // percentage
        const padding = { top: 10, bottom: 20, left: 5, right: 5 };
        const chartHeight = height - padding.top - padding.bottom;

        const maxPly = Math.max(...data.map(d => d.ply));
        const xScale = (ply: number) => (ply / maxPly) * (width - padding.left - padding.right) + padding.left;
        const yScale = (val: number) => chartHeight - (val * chartHeight) + padding.top;

        const paths: Record<string, string> = {};

        concepts.forEach(concept => {
            const points = data
                .filter(d => d[concept] !== undefined)
                .map(d => `${xScale(d.ply)}%,${yScale(d[concept] ?? 0)}`);

            if (points.length > 1) {
                paths[concept] = `M ${points.join(' L ')}`;
            }
        });

        return { paths, xScale, yScale, maxPly };
    }, [data, height]);

    if (!chartData || data.length < 2) {
        return (
            <div style={{
                height,
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                color: 'var(--muted)',
                fontSize: '0.8rem'
            }}>
                분석 데이터 부족
            </div>
        );
    }

    return (
        <div className="concept-trend-chart" style={{ height, position: 'relative' }}>
            <svg
                width="100%"
                height={height}
                style={{ overflow: 'visible' }}
            >
                {/* Grid lines */}
                {[0, 0.25, 0.5, 0.75, 1].map(val => (
                    <line
                        key={val}
                        x1="5%"
                        x2="95%"
                        y1={height - 20 - val * (height - 30)}
                        y2={height - 20 - val * (height - 30)}
                        stroke="var(--border, #333)"
                        strokeWidth="0.5"
                        strokeDasharray="2,2"
                    />
                ))}

                {/* Concept lines */}
                {concepts.map(concept => (
                    chartData.paths[concept] && (
                        <path
                            key={concept}
                            d={chartData.paths[concept]}
                            fill="none"
                            stroke={CONCEPT_COLORS[concept]}
                            strokeWidth="2"
                            strokeLinecap="round"
                            strokeLinejoin="round"
                        />
                    )
                ))}

                {/* Current ply marker */}
                {currentPly && (
                    <line
                        x1={`${chartData.xScale(currentPly)}%`}
                        x2={`${chartData.xScale(currentPly)}%`}
                        y1="10"
                        y2={height - 20}
                        stroke="var(--primary, #fff)"
                        strokeWidth="1"
                        strokeDasharray="3,3"
                    />
                )}
            </svg>

            {/* Legend */}
            <div style={{
                display: 'flex',
                justifyContent: 'center',
                gap: '12px',
                fontSize: '0.65rem',
                marginTop: '4px'
            }}>
                {concepts.map(concept => (
                    <span key={concept} style={{ display: 'flex', alignItems: 'center', gap: '4px' }}>
                        <span style={{
                            width: '8px',
                            height: '8px',
                            backgroundColor: CONCEPT_COLORS[concept],
                            borderRadius: '50%'
                        }} />
                        {CONCEPT_LABELS[concept]}
                    </span>
                ))}
            </div>
        </div>
    );
}

export default ConceptTrendChart;

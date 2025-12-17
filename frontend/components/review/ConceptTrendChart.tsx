'use client';

import React, { useMemo } from 'react';

interface ConceptData {
    ply: number;
    dynamic?: number;
    kingSafety?: number;
    imbalanced?: number;
    colorComplex?: number;
    space?: number;
    blunderRisk?: number;
}

interface ConceptTrendChartProps {
    data: ConceptData[];
    currentPly?: number;
    height?: number;
}

const CONCEPT_COLORS: Record<string, string> = {
    dynamic: '#ff9f43',       // Orange
    kingSafety: '#ff6b6b',    // Reddish
    imbalanced: '#a55eea',    // Purple
    colorComplex: '#4b7bec',  // Blue
    space: '#26de81',         // Green
    blunderRisk: '#ee5253',   // Red
};

const CONCEPT_LABELS: Record<string, string> = {
    dynamic: 'Dynamic',
    kingSafety: 'King Danger',
    imbalanced: 'Imbalance',
    colorComplex: 'Color Complex',
    space: 'Space',
    blunderRisk: 'Blunder Risk',
};

const concepts = [
    'dynamic',
    'kingSafety',
    'imbalanced',
    'colorComplex',
    'space',
    'blunderRisk'
] as const;

export function ConceptTrendChart({
    data,
    currentPly,
    height = 120
}: ConceptTrendChartProps) {

    const chartData = useMemo(() => {
        if (data.length === 0) return null;

        // Logical coordinate system: 0-100 for both X and Y
        // This allows us to rely on SVG scaling via viewBox
        const logicalWidth = 100;
        const logicalHeight = 100;

        // Vertical padding within the logical space (10 units top, 10 units bottom)
        const vPadding = 10;
        const drawHeight = logicalHeight - (vPadding * 2);

        const maxPly = Math.max(...data.map(d => d.ply));

        // Map Ply to 0..100
        // We use full width for X axis
        const xScale = (ply: number) => (ply / Math.max(1, maxPly)) * logicalWidth;

        // Map Value 0..1 to Y (inverted: 1 is top, 0 is bottom)
        // Value 1 -> vPadding
        // Value 0 -> logicalHeight - vPadding
        const yScale = (val: number) => (logicalHeight - vPadding) - (val * drawHeight);

        const paths: Record<string, string> = {};

        concepts.forEach(concept => {
            const points = data
                .filter(d => d[concept] !== undefined)
                .map(d => {
                    const x = xScale(d.ply);
                    const y = yScale(Math.max(0, Math.min(1, d[concept] ?? 0)));
                    return `${x.toFixed(2)},${y.toFixed(2)}`;
                });

            if (points.length > 1) {
                paths[concept] = `M ${points.join(' L ')}`;
            }
        });

        return { paths, xScale, yScale, maxPly, logicalHeight, logicalWidth };
    }, [data]);

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
                Not enough data
            </div>
        );
    }

    return (
        <div className="concept-trend-chart" style={{ height, position: 'relative' }}>
            <svg
                width="100%"
                height="100%"
                viewBox={`0 0 ${chartData.logicalWidth} ${chartData.logicalHeight}`}
                preserveAspectRatio="none"
                style={{ overflow: 'visible' }}
            >
                {/* Grid lines (0, 0.25, 0.5, 0.75, 1.0) */}
                {[0, 0.25, 0.5, 0.75, 1].map(val => (
                    <line
                        key={val}
                        x1="0"
                        x2="100"
                        y1={chartData.yScale(val)}
                        y2={chartData.yScale(val)}
                        stroke="var(--border, #444)"
                        strokeWidth="0.5"
                        strokeDasharray="2,2"
                        vectorEffect="non-scaling-stroke"
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
                            vectorEffect="non-scaling-stroke"
                        />
                    )
                ))}

                {/* Current ply marker */}
                {currentPly !== undefined && (
                    <line
                        x1={chartData.xScale(currentPly)}
                        x2={chartData.xScale(currentPly)}
                        y1="0"
                        y2="100"
                        stroke="var(--primary, #fff)"
                        strokeWidth="1"
                        strokeDasharray="3,3"
                        vectorEffect="non-scaling-stroke"
                    />
                )}
            </svg>

            {/* Legend */}
            <div style={{
                display: 'flex',
                justifyContent: 'center',
                gap: '12px',
                fontSize: '0.65rem',
                marginTop: '-10px',
                position: 'relative',
                zIndex: 10
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

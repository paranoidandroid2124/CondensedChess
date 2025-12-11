'use client';

import React from 'react';

// Tag descriptions for tooltips (English for US/UK audience)
const TAG_DESCRIPTIONS: Record<string, string> = {
    // King Safety
    'white_king_safety_crisis': 'White king under serious attack',
    'black_king_safety_crisis': 'Black king under serious attack',
    'white_king_exposed': 'White king lacks pawn shelter',
    'black_king_exposed': 'Black king lacks pawn shelter',

    // Tactical
    'tactical_complexity': 'Tactically complex position',
    'high_blunder_risk': 'High risk of making a mistake',
    'tactical_miss': 'Missed tactical opportunity',

    // Positional
    'white_comfortable_position': 'White has multiple good options',
    'black_comfortable_position': 'Black has multiple good options',
    'white_unpleasant_position': 'White must find precise moves',
    'black_unpleasant_position': 'Black must find precise moves',
    'material_imbalance': 'Material imbalance on the board',

    // Structure
    'white_space_advantage': 'White controls more space',
    'black_space_advantage': 'Black controls more space',
    'white_active_rooks': 'White rooks are active',
    'black_active_rooks': 'Black rooks are active',
    'white_bishop_pair_advantage': 'White has the bishop pair',
    'black_bishop_pair_advantage': 'Black has the bishop pair',

    // Game Phase
    'endgame_transition': 'Transitioning to endgame',
    'fortress_structure': 'Fortress structure forming',
    'tactical_to_positional': 'Shift from tactics to positional play',

    // Mistakes
    'greedy': 'Greedy move—took material but paid a price',
    'passive_move': 'Passive move',
    'ignored_threat': 'Ignored an opponent threat',
    'premature_pawn_push': 'Premature pawn advance',

    // Opening
    'novelty': 'Novelty—deviation from known theory',
    'book_move': 'Book move',
};

interface TagBadgeProps {
    tag: string;
    className?: string;
}

export function TagBadge({ tag, className = '' }: TagBadgeProps) {
    const description = TAG_DESCRIPTIONS[tag] || formatTagName(tag);
    const displayName = formatTagName(tag);

    return (
        <span
            className={`tag-badge ${className}`}
            title={description}
            style={{
                display: 'inline-block',
                padding: '2px 6px',
                fontSize: '0.7rem',
                backgroundColor: getTagColor(tag),
                color: '#fff',
                borderRadius: '4px',
                cursor: 'help',
                marginRight: '4px',
                marginBottom: '2px',
            }}
        >
            {displayName}
        </span>
    );
}

function formatTagName(tag: string): string {
    return tag
        .replace(/_/g, ' ')
        .replace(/white |black /gi, '')
        .split(' ')
        .map(word => word.charAt(0).toUpperCase() + word.slice(1))
        .join(' ');
}

function getTagColor(tag: string): string {
    if (tag.includes('crisis') || tag.includes('exposed') || tag.includes('miss')) {
        return '#dc3545'; // danger red
    }
    if (tag.includes('comfortable') || tag.includes('advantage') || tag.includes('active')) {
        return '#28a745'; // success green
    }
    if (tag.includes('tactical') || tag.includes('blunder')) {
        return '#fd7e14'; // warning orange
    }
    if (tag.includes('endgame') || tag.includes('fortress') || tag.includes('transition')) {
        return '#6c757d'; // neutral gray
    }
    return '#6c757d'; // default gray
}

export default TagBadge;

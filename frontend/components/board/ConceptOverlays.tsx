import { DrawShape } from 'chessground/draw';
import { Color } from 'chessground/types';

// Logic to map Semantic Tags to Visual Shapes on the board
// This allows the frontend to visualize "Space Advantage" or "Weak Square"

export const getConceptShapes = (tags: string[], orientation: Color): DrawShape[] => {
    const shapes: DrawShape[] = [];

    tags.forEach(tag => {
        // 1. Weak Squares / Targets
        if (tag.includes("WeakF7")) {
            shapes.push({ orig: 'f7', brush: 'red' });
            // If we knew the attacker, we could draw arrow. For now, simple highlight.
        }
        if (tag.includes("WeakF2")) {
            shapes.push({ orig: 'f2', brush: 'red' });
        }

        // 2. Central Control
        if (tag.includes("SpaceAdvantage") || tag.includes("ControlCenter")) {
            // Highlight center based on side?
            // "SpaceAdvantageWhite" -> d4, e4, c4? 
            if (tag.includes("White")) {
                shapes.push({ orig: 'd4', brush: 'blue' });
                shapes.push({ orig: 'e4', brush: 'blue' });
            } else if (tag.includes("Black")) {
                shapes.push({ orig: 'd5', brush: 'blue' });
                shapes.push({ orig: 'e5', brush: 'blue' });
            }
        }

        // 3. Piece Activity
        if (tag.includes("BadBishop")) {
            // Determining *which* bishop is bad requires board state (FEN) + logic.
            // Since we only have the tag string here, we can't easily know the square.
            // Ideally, the Backend tag should be "BadBishopOnC8".
            // If not, we skip or use a generic "Badge" in the MoveList instead of board overlay.
            // Board overlays usually need coordinate data from backend or heavy FE logic.
        }

        // 4. Files
        if (tag.includes("OpenCFile")) {
            // Draw arrow along C file?
        }
    });

    return shapes;
};

function splitWords(text: string): string[] {
  return text
    .split(/\s+/)
    .map(t => t.trim())
    .filter(Boolean);
}

function humanizeToken(raw: string): string {
  return splitWords(raw.replace(/[_-]+/g, ' ').replace(/([a-z])([A-Z])/g, '$1 $2'))
    .map(word => (word.length <= 2 ? word.toUpperCase() : word.charAt(0).toUpperCase() + word.slice(1)))
    .join(' ');
}

function rewriteSurfaceLabels(text: string): string {
  return text
    .replace(/\bStrategic Focus\b/gi, 'Key theme')
    .replace(/\bStrategic Priority\b/gi, 'Key theme')
    .replace(/\bOpening Branch Point\b/gi, 'Opening Branch')
    .replace(/\bPreparing ([A-H])[- ](?:pawn\s+)?break Break\b/gi, (_, file: string) => `Preparing the ${file.toLowerCase()}-break`)
    .replace(/\b([A-H])[- ](?:pawn\s+)?break Break\b/gi, (_, file: string) => `${file.toLowerCase()}-break`)
    .replace(/\bPreparing ([A-H])[- ](?:pawn\s+)?break\b/gi, (_, file: string) => `Preparing the ${file.toLowerCase()}-break`)
    .replace(/\b([A-H])[- ](?:pawn\s+)?break\b/gi, (_, file: string) => `${file.toLowerCase()}-break`)
    .replace(/\bOpening Development And Center Control\b/gi, 'Development and central control')
    .replace(/\bPiece Activation\b/gi, 'Improving piece placement')
    .replace(/\bExploiting Space Advantage\b/gi, 'Using the space advantage')
    .replace(/\bExchanging For Favorable Simplification\b/gi, 'Simplifying with favorable exchanges')
    .replace(/\bSimplification Into Endgame\b/gi, 'Simplifying toward an endgame')
    .replace(/\bImmediate Tactical Gain Counterplay\b/gi, 'Immediate counterplay')
    .replace(/\bAttacking Fixed Pawn\b/gi, 'Attacking a fixed pawn')
    .replace(/\bRook Pawn March To Gain Flank Space\b/gi, 'Gaining flank space with a rook pawn')
    .replace(/\bKingside Pawn Storm\b/gi, 'kingside pawn storm');
}

export function cleanNarrativeSurfaceLabel(raw: string): string {
  const cleaned = rewriteSurfaceLabels(
    humanizeToken(
      (raw || '')
        .replace(/\*\*/g, '')
        .replace(/`+/g, ' ')
        .replace(/\bPlayed\s*PV\b/gi, 'Played Line')
        .replace(/\bPlayable\s*By\s*PV\b/gi, 'Reliable Continuation'),
    ),
  )
    .replace(/\bPlayed Line\b/gi, 'Played line')
    .replace(/\bReliable Continuation\b/gi, 'Reliable continuation')
    .replace(/\b(\w+)\s+\1\b/gi, '$1')
    .replace(/\s{2,}/g, ' ')
    .trim();
  return cleaned || raw;
}

import type { NarrativeSignalDigest } from './signalTypes';

export type PlayerFacingSupportOptions = {
  compensationContext?: boolean;
};

// Mirrors the backend PlayerFacingSupportPolicy contract.
const supportMetaPatterns = [
  /\bplan fit\b/i,
  /\bstrategic stack\b/i,
  /\branked stack\b/i,
  /\blatent plan\b/i,
  /\bnominal evaluation\b/i,
  /\bpractical conversion\b/i,
  /\bconversion window\b/i,
  /\bforgiveness\b/i,
  /\b\d+cp\b/i,
  /\bpending probes?\b/i,
  /\bprobe evidence\b/i,
  /\bauthoring evidence\b/i,
  /\bcoordination improvement\b/i,
  /\bplan activation lane\b/i,
  /\brather than drifting into\b/i,
  /\bkeeps the play focused on\b/i,
  /\bwithin normal bounds\b/i,
  /\bdominant thesis\b/i,
  /\bso the plan cannot drift\b/i,
  /\bstill looks playable in the engine line\b/i,
  /\bneeds stronger support beyond that line\b/i,
  /\bmore confirmation is still needed\b/i,
  /\bstill needs more concrete support\b/i,
  /\bthe idea still needs concrete support\b/i,
  /\bthe plan still revolves around\b/i,
  /\ba useful route is\b/i,
  /\bthe next useful target is\b/i,
  /\[pv coupled\]/i,
];

const supportAbstractPattern = /\b(initiative|counterplay|compensation|conversion|pressure|attack|plan|campaign|objective|execution)\b/i;
const supportConcretePatterns = [
  /\b[a-h][1-8]\b/i,
  /\b[a-h]-file\b/i,
  /\b(queenside|kingside|central|open)\s+files?\b/i,
  /\b(queenside|kingside|central|fixed)\s+targets?\b/i,
  /\b(light|dark)-squared\b/i,
  /\b(exchange|trade|recapture|pawn break|break|castling|castle|back-rank)\b/i,
  /(?:^|\s)(?:\d+\.(?:\.\.)?\s*)?(?:O-O(?:-O)?|[KQRBN]?[a-h]?[1-8]?x?[a-h][1-8](?:=[QRBN])?[+#]?)/,
  /\.\.\.(?:O-O(?:-O)?|[KQRBN]?[a-h]?[1-8]?x?[a-h][1-8](?:=[QRBN])?[+#]?)/,
  /\b(king|queen|rook|bishop|knight|pawn)s?\b/i,
];

const compensationAbstractRowPatterns = [
  /^Improving piece placement$/i,
  /^Development and central control$/i,
  /^Immediate counterplay$/i,
  /^Attacking a fixed pawn$/i,
  /^Using the space advantage$/i,
  /^Simplifying with favorable exchanges$/i,
  /^Simplifying toward an endgame$/i,
  /^Kingside pawn storm$/i,
  /^Gaining flank space with a rook pawn$/i,
];

const compensationRelevantPattern =
  /\b(material can wait|winning the material back|compensation|initiative|pressure|attack|open lines?|open files?|queenside files?|central files?|queenside targets?|central targets?|fixed targets?|defenders?|extra pawn)\b/i;
const compensationStrongAnchorPatterns = [
  /\b[a-h][1-8]\b/i,
  /\b[a-h]-file\b/i,
  /\bpressure (?:on|against|along)\b/i,
  /\b(?:queen|rook|bishop|knight|king|pawn)s?\b.*\b(?:queenside|central|open)\s+files?\b/i,
  /\b(?:queen|rook|bishop|knight|king|pawn)s?\b.*\b(?:can head for|heads? for|to)\s+[a-h][1-8]\b/i,
  /\bdefenders?\b.*\bking\b/i,
  /\bextra pawn\b.*\bactive\b/i,
  /\b[a-h]-break\b/i,
  /(?:^|\s)(?:\d+\.(?:\.\.)?\s*)?(?:O-O(?:-O)?|[KQRBN]?[a-h]?[1-8]?x?[a-h][1-8](?:=[QRBN])?[+#]?)/,
];

function splitWords(text: string): string[] {
  return text
    .split(/\s+/)
    .map(t => t.trim())
    .filter(Boolean);
}

export function humanizeToken(raw: string): string {
  return splitWords(raw.replace(/[_-]+/g, ' ').replace(/([a-z])([A-Z])/g, '$1 $2'))
    .map(word => (word.length <= 2 ? word.toUpperCase() : word.charAt(0).toUpperCase() + word.slice(1)))
    .join(' ');
}

function rewriteSurfaceLabels(text: string): string {
  return text
    .replace(/\bOpening Branch Point\b/gi, 'Opening Branch')
    .replace(/\bPreparing ([A-H]) Break Break\b/gi, (_, file: string) => `Preparing the ${file.toLowerCase()}-break`)
    .replace(/\b([A-H]) Break Break\b/gi, (_, file: string) => `${file.toLowerCase()}-break`)
    .replace(/\bPreparing ([A-H]) Break\b/gi, (_, file: string) => `Preparing the ${file.toLowerCase()}-break`)
    .replace(/\b([A-H]) Break\b/gi, (_, file: string) => `${file.toLowerCase()}-break`)
    .replace(/\bOpening Development And Center Control\b/gi, 'Development and central control')
    .replace(/\bPiece Activation\b/gi, 'Improving piece placement')
    .replace(/\bExploiting Space Advantage\b/gi, 'Using the space advantage')
    .replace(/\bExchanging For Favorable Simplification\b/gi, 'Simplifying with favorable exchanges')
    .replace(/\bSimplification Into Endgame\b/gi, 'Simplifying toward an endgame')
    .replace(/\bImmediate Tactical Gain Counterplay\b/gi, 'Immediate counterplay')
    .replace(/\bAttacking Fixed Pawn\b/gi, 'Attacking a fixed pawn')
    .replace(/\bRook Pawn March To Gain Flank Space\b/gi, 'Gaining flank space with a rook pawn')
    .replace(/\bKingside Pawn Storm\b/gi, 'Kingside pawn storm');
}

export function cleanNarrativeSurfaceLabel(raw: string): string {
  const cleaned = rewriteSurfaceLabels(
    humanizeToken(
      (raw || '')
      .replace(/\*\*/g, '')
      .replace(/`+/g, ' ')
      .replace(/\bPlayed\s*PV\b/gi, 'Played Line')
      .replace(/\bPlayable\s*By\s*PV\b/gi, 'Engine Backed Continuation'),
    ),
  )
    .replace(/\bPlayed Line\b/gi, 'Played line')
    .replace(/\bEngine Backed Continuation\b/gi, 'Engine-backed continuation')
    .replace(/\b(\w+)\s+\1\b/gi, '$1')
    .replace(/\s{2,}/g, ' ')
    .trim();
  return cleaned || raw;
}

export function cleanNarrativeProseText(raw: string): string {
  return (raw || '')
    .replace(/\*\*/g, '')
    .replace(/__+/g, '')
    .replace(/`+/g, ' ')
    .replace(/\bPlayed\s*PV\b/gi, 'played line')
    .replace(/\bPlayable\s*By\s*PV\b/gi, 'engine-backed continuation')
    .replace(/\b(?:theme|support|seed|proposal|subplan):([a-z0-9_]+)\b/gi, (_, label: string) => humanizeToken(label))
    .replace(/\bply\s+(\d+)\b/gi, (_, ply: string) => `move ${Math.max(1, Math.ceil(Number(ply) / 2))}`)
    .replace(/\bcoordination improvement\b/gi, 'better piece coordination')
    .replace(/\bplan activation lane\b/gi, 'the main plan')
    .replace(/\ba cleaner bishop circuit\b/gi, 'a better bishop route')
    .replace(/\bpiece activation before the break\b/gi, 'a better square before the break')
    .replace(/\bking-attack build-up around ([^.]+?)\b/gi, 'pressure toward $1')
    .replace(/\bfavorable trade or transformation around ([^.]+?)\b/gi, 'exchanges around $1')
    .replace(/\bspace gain or restriction around ([^.]+?)\b/gi, 'space around $1')
    .replace(/\bline occupation around ([^.]+?)\b/gi, 'pressure along $1')
    .replace(/\bimmediate tactical gain Counterplay\b/gi, 'tactical counterplay')
    .replace(/\bfocus on ([a-h][1-8](?:,\s*[a-h][1-8])*)\b/gi, 'pressure on $1')
    .replace(/\bin the foreground via\b/gi, 'with')
    .replace(/\bso the plan cannot drift\b/gi, 'so the idea stays clear')
    .replace(/\bmore confirmation is still needed\b/gi, '')
    .replace(/\bThe plan still revolves around\b/gi, 'The key idea is')
    .replace(/\bA useful route is\b/gi, 'A likely follow-up is')
    .replace(/\bThe next useful target is\b/gi, 'A concrete target is')
    .replace(/\bUnknown and its (?:fluid|locked|symmetric|semi-open|open) center\b/gi, 'the current structure')
    .replace(/\bUnknown\b/gi, 'the current structure')
    .replace(/\s*\((?:the idea still needs concrete support|still needs more concrete support)\)/gi, '')
    .replace(/\s+\((?:contested|build)\)/gi, '')
    .replace(/\s{2,}/g, ' ')
    .replace(/\.{4,}/g, '...')
    .trim();
}

export function cleanStrategicNoteText(raw: string): string {
  const lines = cleanNarrativeProseText(raw)
    .split(/\r?\n+/)
    .map(line => line.trim())
    .filter(Boolean)
    .filter(line =>
      !supportMetaPatterns.some(pattern => pattern.test(line)) &&
      !/^execution\s*\/\s*objective\s*\/\s*focus:/i.test(line) &&
      !/^active\s*\(/i.test(line),
    );
  return lines.join(' ').replace(/\s{2,}/g, ' ').trim();
}

export function hasCompensationContext(signalDigest: NarrativeSignalDigest | null | undefined): boolean {
  const digest = signalDigest || null;
  return !!(
    digest?.compensation ||
    typeof digest?.investedMaterial === 'number' ||
    (digest?.compensationVectors?.length || 0)
  );
}

export function buildPlayerFacingSupportOptions(
  signalDigest: NarrativeSignalDigest | null | undefined,
): PlayerFacingSupportOptions {
  return { compensationContext: hasCompensationContext(signalDigest) };
}

function keepCompensationSupportText(text: string): boolean {
  if (compensationAbstractRowPatterns.some(pattern => pattern.test(text))) return false;
  if (!compensationRelevantPattern.test(text)) return true;
  return compensationStrongAnchorPatterns.some(pattern => pattern.test(text));
}

function allowPlayerFacingSupportText(text: string): boolean {
  return !supportAbstractPattern.test(text) || supportConcretePatterns.some(pattern => pattern.test(text));
}

export function rewritePlayerFacingSupportText(
  raw: string | null | undefined,
  opts: PlayerFacingSupportOptions = {},
): string | null {
  const cleaned = cleanNarrativeProseText(raw || '')
    .replace(/\bplan fit\b/gi, 'structure logic')
    .replace(/\bcuts out counterplay\b/gi, 'stops the opponent\'s next active idea')
    .replace(/\bcounterplay suppression\b/gi, 'stopping the opponent\'s next active idea')
    .replace(/\bpractical conversion\b/gi, 'over-the-board follow-up')
    .replace(/\bconversion window\b/gi, 'favorable exchange window')
    .replace(/\bmaking ([^.]+?) available\b/gi, 'preparing $1')
    .replace(/\s{2,}/g, ' ')
    .trim();

  if (!cleaned) return null;
  if (/still needs (?:more )?concrete support/i.test(cleaned)) return null;
  if (/^the follow-up is to stay inside .*themes without losing the position's balance\.?$/i.test(cleaned)) return null;
  if (/^the move keeps development going without changing the basic balance\.?$/i.test(cleaned)) return null;
  if (opts.compensationContext && !keepCompensationSupportText(cleaned)) return null;
  if (supportMetaPatterns.some(pattern => pattern.test(cleaned))) return null;
  if (!allowPlayerFacingSupportText(cleaned)) return null;
  return cleaned;
}

export function filterPlayerFacingValues(
  values: Array<string | null | undefined>,
  opts: PlayerFacingSupportOptions = {},
): string[] {
  return values
    .map(value => rewritePlayerFacingSupportText(value, opts))
    .filter(Boolean) as string[];
}

export function filterPlayerFacingRows(
  rows: Array<[string, string] | null>,
  opts: PlayerFacingSupportOptions = {},
): [string, string][] {
  return rows
    .map(row => {
      if (!row) return null;
      const cleaned = rewritePlayerFacingSupportText(row[1], opts);
      return cleaned ? [row[0], cleaned] as [string, string] : null;
    })
    .filter(Boolean) as [string, string][];
}

export function summarizeReviewMomentProse(raw: string, openingPhase = false): string {
  const cleaned = cleanNarrativeProseText(raw);
  if (!cleaned) return cleaned;

  const sentences =
    cleaned
      .match(/[^.!?]+(?:[.!?]+|$)/g)
      ?.map(sentence => sentence.trim())
      .filter(Boolean) || [cleaned];

  const maxSentences = openingPhase ? 2 : 3;
  return sentences.slice(0, maxSentences).join(' ').trim();
}

export function formatEvidenceStatus(status: string): string {
  switch (status.trim().toLowerCase()) {
    case 'resolved':
      return 'Resolved';
    case 'pending':
      return 'Pending';
    case 'question_only':
      return 'Heuristic';
    default:
      return humanizeToken(status);
  }
}

export function formatEvidenceScore(evalCp?: number | null, mate?: number | null): string {
  if (typeof mate === 'number') return `mate ${mate}`;
  if (typeof evalCp === 'number') return `${evalCp >= 0 ? '+' : ''}${evalCp}cp`;
  return '';
}

export function formatDeploymentSummary(signalDigest: NarrativeSignalDigest): string | null {
  if (!signalDigest.deploymentPiece || !signalDigest.deploymentPurpose) return null;
  const route = (signalDigest.deploymentRoute || []).filter(Boolean);
  const surfaceMode = signalDigest.deploymentSurfaceMode || 'hidden';
  if (surfaceMode === 'hidden') return null;
  const destination = route[route.length - 1] || '';
  const lead =
    surfaceMode === 'exact'
      ? `${signalDigest.deploymentPiece} via ${route.join('-')}`
      : `${signalDigest.deploymentPiece} toward ${destination || 'the target square'}`;
  const purpose = rewritePlayerFacingSupportText(signalDigest.deploymentPurpose) || '';
  const contribution = rewritePlayerFacingSupportText(signalDigest.deploymentContribution?.trim());
  const purposeSurface =
    purpose && /^(supports?|prepares?|opens?|keeps?|pressures?|stops?|covers?|guards?)/i.test(purpose)
      ? purpose
      : purpose
        ? `to support ${purpose}`
        : '';
  return rewritePlayerFacingSupportText([lead, purposeSurface, contribution].filter(Boolean).join(' · '));
}

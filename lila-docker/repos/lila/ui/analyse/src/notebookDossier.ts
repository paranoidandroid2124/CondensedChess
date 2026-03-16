export type EvidenceStrength = 'weak' | 'medium' | 'strong';

export type EvidenceSummaryV1 = {
  strength: EvidenceStrength;
  supportingGames: number;
  totalSampledGames: number;
  occurrenceCount?: number;
  clusterSharePct?: number;
};

export type GameEvidenceRefV1 = {
  id: string;
  provider: 'chesscom' | 'lichess' | 'internal';
  externalGameId?: string;
  sourceUrl?: string;
  white: string;
  black: string;
  result: string;
  opening?: string;
  role: 'anchor' | 'support';
};

export type OverviewCardKind =
  | 'opening_identity'
  | 'recurring_leak'
  | 'repair_priority'
  | 'pressure_point'
  | 'steering_target';

export type OverviewCardV1 = {
  id: string;
  kind: OverviewCardKind;
  title: string;
  headline: string;
  summary: string;
  priority: 'low' | 'medium' | 'high';
  confidence: EvidenceStrength;
  evidence: EvidenceSummaryV1;
  tags?: string[];
  linkedSectionId?: string;
  linkedCardId?: string;
};

export type OverviewSurfaceV1 = {
  title: 'Overview';
  dek: string;
  cards: OverviewCardV1[];
};

export type MoveRefV1 = {
  san?: string;
  uci?: string;
  note?: string;
};

export type AnchorPositionCardV1 = {
  cardKind: 'anchor_position';
  id: string;
  clusterId: string;
  title: string;
  lens: 'self_repair' | 'opponent_pressure';
  claim: string;
  explanation: string;
  fen: string;
  moveContext: {
    ply: number;
    moveNumber: number;
    sideToMove: 'white' | 'black';
    lastMoveSan?: string;
  };
  boardFocus?: {
    squares?: string[];
    arrows?: Array<{ from: string; to: string; label?: string }>;
  };
  strategicTags: string[];
  questionPrompt: string;
  recommendedPlan: {
    label: string;
    summary: string;
    candidateMoves?: MoveRefV1[];
  };
  antiPattern?: {
    label: string;
    summary: string;
    playedMoveSan?: string;
    estimatedCostCp?: number | null;
  };
  evidence: EvidenceSummaryV1 & {
    supportingGameIds: string[];
  };
  exemplarGameId?: string;
};

export type OpeningMapCardV1 = {
  cardKind: 'opening_map';
  id: string;
  title: string;
  side: 'white' | 'black' | 'mixed';
  openingFamily: string;
  structureLabels: string[];
  story: string;
  evidence: EvidenceSummaryV1 & { supportingGameIds: string[] };
};

export type ExemplarGameCardV1 = {
  cardKind: 'exemplar_game';
  id: string;
  title: string;
  game: GameEvidenceRefV1;
  whyItMatters: string;
  narrative: string;
  turningPointPly?: number;
  linkedAnchorPositionIds: string[];
  takeaway: string;
};

export type ActionItemCardV1 = {
  cardKind: 'action_item';
  id: string;
  title: string;
  instruction: string;
  successMarker: string;
  linkedAnchorPositionIds?: string[];
};

export type ChecklistCardV1 = {
  cardKind: 'checklist';
  id: string;
  title: string;
  items: Array<{
    id: string;
    label: string;
    reason?: string;
    priority: 'low' | 'medium' | 'high';
  }>;
};

export type SectionCardV1 =
  | OpeningMapCardV1
  | AnchorPositionCardV1
  | ExemplarGameCardV1
  | ActionItemCardV1
  | ChecklistCardV1;

export type NotebookSectionKind =
  | 'opening_map'
  | 'pattern_cluster'
  | 'steering_plan'
  | 'exemplar_games'
  | 'action_page'
  | 'pre_game_checklist';

export type NotebookSectionV1 = {
  id: string;
  kind: NotebookSectionKind;
  title: string;
  summary: string;
  rank: number;
  status: 'ready' | 'partial';
  evidence?: EvidenceSummaryV1;
  cards: SectionCardV1[];
};

export type NotebookDossierProductKind = 'my_account_intelligence_lite' | 'opponent_prep';

export type NotebookDossierV1 = {
  schema: 'chesstory.notebook.dossier.v1';
  dossierId: string;
  productKind: NotebookDossierProductKind;
  subject: {
    role: 'self' | 'opponent';
    provider: 'chesscom' | 'lichess';
    username: string;
    displayName: string;
  };
  source: {
    requestedGameLimit: number;
    sampledGameCount: number;
    eligibleGameCount: number;
    publicOnly: true;
    includeTimeControl: false;
    generatedAt: string;
    cache: { hit: boolean; ttlSec: number; ageSec?: number };
  };
  status: 'ready' | 'partial';
  headline: string;
  summary: string;
  overview: OverviewSurfaceV1;
  sections: NotebookSectionV1[];
  appendix?: {
    sampledGames: GameEvidenceRefV1[];
    warnings?: string[];
  };
};

export const NOTEBOOK_DOSSIER_SCHEMA_V1 = 'chesstory.notebook.dossier.v1';

const overviewKindOrder: Record<NotebookDossierProductKind, OverviewCardKind[]> = {
  my_account_intelligence_lite: ['opening_identity', 'recurring_leak', 'repair_priority'],
  opponent_prep: ['opening_identity', 'pressure_point', 'steering_target'],
};

const subjectRoleByProduct: Record<NotebookDossierProductKind, NotebookDossierV1['subject']['role']> = {
  my_account_intelligence_lite: 'self',
  opponent_prep: 'opponent',
};

function isObject(value: unknown): value is Record<string, unknown> {
  return !!value && typeof value === 'object' && !Array.isArray(value);
}

function isString(value: unknown): value is string {
  return typeof value === 'string';
}

function isNumber(value: unknown): value is number {
  return typeof value === 'number' && Number.isFinite(value);
}

function isBoolean(value: unknown): value is boolean {
  return typeof value === 'boolean';
}

function isStringArray(value: unknown): value is string[] {
  return Array.isArray(value) && value.every(isString);
}

function isEvidenceStrength(value: unknown): value is EvidenceStrength {
  return value === 'weak' || value === 'medium' || value === 'strong';
}

function isPriority(value: unknown): value is 'low' | 'medium' | 'high' {
  return value === 'low' || value === 'medium' || value === 'high';
}

function isProvider(value: unknown): value is GameEvidenceRefV1['provider'] {
  return value === 'chesscom' || value === 'lichess' || value === 'internal';
}

function isSubjectProvider(value: unknown): value is NotebookDossierV1['subject']['provider'] {
  return value === 'chesscom' || value === 'lichess';
}

function isOverviewCardKind(value: unknown): value is OverviewCardKind {
  return (
    value === 'opening_identity' ||
    value === 'recurring_leak' ||
    value === 'repair_priority' ||
    value === 'pressure_point' ||
    value === 'steering_target'
  );
}

function isSectionKind(value: unknown): value is NotebookSectionKind {
  return (
    value === 'opening_map' ||
    value === 'pattern_cluster' ||
    value === 'steering_plan' ||
    value === 'exemplar_games' ||
    value === 'action_page' ||
    value === 'pre_game_checklist'
  );
}

function isCardKind(value: unknown): value is SectionCardV1['cardKind'] {
  return (
    value === 'opening_map' ||
    value === 'anchor_position' ||
    value === 'exemplar_game' ||
    value === 'action_item' ||
    value === 'checklist'
  );
}

function isEvidenceSummary(value: unknown): value is EvidenceSummaryV1 {
  if (!isObject(value)) return false;
  if (!isEvidenceStrength(value.strength)) return false;
  if (!isNumber(value.supportingGames) || value.supportingGames < 0) return false;
  if (!isNumber(value.totalSampledGames) || value.totalSampledGames < 0) return false;
  if (value.occurrenceCount !== undefined && (!isNumber(value.occurrenceCount) || value.occurrenceCount < 0))
    return false;
  if (value.clusterSharePct !== undefined && !isNumber(value.clusterSharePct)) return false;
  return true;
}

function hasSupportingGameIds(value: unknown): value is { supportingGameIds: string[] } {
  return isObject(value) && isStringArray(value.supportingGameIds);
}

function isGameEvidenceRef(value: unknown): value is GameEvidenceRefV1 {
  if (!isObject(value)) return false;
  return (
    isString(value.id) &&
    isProvider(value.provider) &&
    (value.externalGameId === undefined || isString(value.externalGameId)) &&
    (value.sourceUrl === undefined || isString(value.sourceUrl)) &&
    isString(value.white) &&
    isString(value.black) &&
    isString(value.result) &&
    (value.opening === undefined || isString(value.opening)) &&
    (value.role === 'anchor' || value.role === 'support')
  );
}

function isMoveRef(value: unknown): value is MoveRefV1 {
  if (!isObject(value)) return false;
  return (
    (value.san === undefined || isString(value.san)) &&
    (value.uci === undefined || isString(value.uci)) &&
    (value.note === undefined || isString(value.note))
  );
}

function isBoardFocus(value: unknown): value is NonNullable<AnchorPositionCardV1['boardFocus']> {
  if (!isObject(value)) return false;
  if (value.squares !== undefined && !isStringArray(value.squares)) return false;
  if (value.arrows !== undefined) {
    if (
      !Array.isArray(value.arrows) ||
      !value.arrows.every(
        arrow => isObject(arrow) && isString(arrow.from) && isString(arrow.to) && (arrow.label === undefined || isString(arrow.label)),
      )
    )
      return false;
  }
  return true;
}

function isAnchorPositionCard(value: unknown): value is AnchorPositionCardV1 {
  if (!isObject(value)) return false;
  if (value.cardKind !== 'anchor_position') return false;
  if (!isString(value.id) || !isString(value.clusterId) || !isString(value.title)) return false;
  if (value.lens !== 'self_repair' && value.lens !== 'opponent_pressure') return false;
  if (!isString(value.claim) || !isString(value.explanation) || !isString(value.fen)) return false;
  if (!isObject(value.moveContext)) return false;
  if (
    !isNumber(value.moveContext.ply) ||
    !isNumber(value.moveContext.moveNumber) ||
    (value.moveContext.sideToMove !== 'white' && value.moveContext.sideToMove !== 'black') ||
    (value.moveContext.lastMoveSan !== undefined && !isString(value.moveContext.lastMoveSan))
  )
    return false;
  if (value.boardFocus !== undefined && !isBoardFocus(value.boardFocus)) return false;
  if (!isStringArray(value.strategicTags) || !isString(value.questionPrompt)) return false;
  if (!isObject(value.recommendedPlan)) return false;
  if (
    !isString(value.recommendedPlan.label) ||
    !isString(value.recommendedPlan.summary) ||
    (value.recommendedPlan.candidateMoves !== undefined &&
      (!Array.isArray(value.recommendedPlan.candidateMoves) || !value.recommendedPlan.candidateMoves.every(isMoveRef)))
  )
    return false;
  if (value.antiPattern !== undefined) {
    if (
      !isObject(value.antiPattern) ||
      !isString(value.antiPattern.label) ||
      !isString(value.antiPattern.summary) ||
      (value.antiPattern.playedMoveSan !== undefined && !isString(value.antiPattern.playedMoveSan)) ||
      (value.antiPattern.estimatedCostCp !== undefined &&
        value.antiPattern.estimatedCostCp !== null &&
        !isNumber(value.antiPattern.estimatedCostCp))
    )
      return false;
  }
  if (!isObject(value.evidence) || !isEvidenceSummary(value.evidence) || !hasSupportingGameIds(value.evidence))
    return false;
  return value.exemplarGameId === undefined || isString(value.exemplarGameId);
}

function isOpeningMapCard(value: unknown): value is OpeningMapCardV1 {
  if (!isObject(value)) return false;
  return (
    value.cardKind === 'opening_map' &&
    isString(value.id) &&
    isString(value.title) &&
    (value.side === 'white' || value.side === 'black' || value.side === 'mixed') &&
    isString(value.openingFamily) &&
    isStringArray(value.structureLabels) &&
    isString(value.story) &&
    isObject(value.evidence) &&
    isEvidenceSummary(value.evidence) &&
    hasSupportingGameIds(value.evidence)
  );
}

function isExemplarGameCard(value: unknown): value is ExemplarGameCardV1 {
  if (!isObject(value)) return false;
  return (
    value.cardKind === 'exemplar_game' &&
    isString(value.id) &&
    isString(value.title) &&
    isGameEvidenceRef(value.game) &&
    isString(value.whyItMatters) &&
    isString(value.narrative) &&
    (value.turningPointPly === undefined || isNumber(value.turningPointPly)) &&
    isStringArray(value.linkedAnchorPositionIds) &&
    isString(value.takeaway)
  );
}

function isActionItemCard(value: unknown): value is ActionItemCardV1 {
  if (!isObject(value)) return false;
  return (
    value.cardKind === 'action_item' &&
    isString(value.id) &&
    isString(value.title) &&
    isString(value.instruction) &&
    isString(value.successMarker) &&
    (value.linkedAnchorPositionIds === undefined || isStringArray(value.linkedAnchorPositionIds))
  );
}

function isChecklistCard(value: unknown): value is ChecklistCardV1 {
  if (!isObject(value)) return false;
  return (
    value.cardKind === 'checklist' &&
    isString(value.id) &&
    isString(value.title) &&
    Array.isArray(value.items) &&
    value.items.every(
      item =>
        isObject(item) &&
        isString(item.id) &&
        isString(item.label) &&
        (item.reason === undefined || isString(item.reason)) &&
        isPriority(item.priority),
    )
  );
}

function isSectionCard(value: unknown): value is SectionCardV1 {
  if (!isObject(value) || !isCardKind(value.cardKind)) return false;
  switch (value.cardKind) {
    case 'opening_map':
      return isOpeningMapCard(value);
    case 'anchor_position':
      return isAnchorPositionCard(value);
    case 'exemplar_game':
      return isExemplarGameCard(value);
    case 'action_item':
      return isActionItemCard(value);
    case 'checklist':
      return isChecklistCard(value);
  }
}

function collectSectionCount(sections: NotebookSectionV1[], kind: NotebookSectionKind): number {
  return sections.filter(section => section.kind === kind).length;
}

function validateSectionComposition(
  dossier: NotebookDossierV1,
  section: NotebookSectionV1,
  errors: string[],
  anchorIds: Set<string>,
): void {
  const cards = section.cards;
  const cardKinds = cards.map(card => card.cardKind);
  const anchorCount = cardKinds.filter(kind => kind === 'anchor_position').length;
  const actionCount = cardKinds.filter(kind => kind === 'action_item').length;
  switch (section.kind) {
    case 'opening_map':
      if (cards.length < 1 || cards.length > 2 || !cards.every(card => card.cardKind === 'opening_map'))
        errors.push(`Section ${section.id} must contain 1-2 opening map cards.`);
      break;
    case 'pattern_cluster':
      if (!cards.every(card => card.cardKind === 'anchor_position' || card.cardKind === 'action_item'))
        errors.push(`Pattern cluster ${section.id} can only contain anchor and action cards.`);
      if (section.status === 'ready' && anchorCount !== 1)
        errors.push(`Ready pattern cluster ${section.id} must contain exactly one anchor card.`);
      if (section.status === 'partial' && anchorCount > 1)
        errors.push(`Partial pattern cluster ${section.id} can contain at most one anchor card.`);
      if (actionCount > 2) errors.push(`Pattern cluster ${section.id} can contain at most two action cards.`);
      break;
    case 'steering_plan':
      if (cards.length < 2 || cards.length > 4 || !cards.every(card => card.cardKind === 'action_item'))
        errors.push(`Steering plan ${section.id} must contain 2-4 action cards.`);
      break;
    case 'exemplar_games':
      if (cards.length < 1 || cards.length > 2 || !cards.every(card => card.cardKind === 'exemplar_game'))
        errors.push(`Exemplar games ${section.id} must contain 1-2 exemplar game cards.`);
      break;
    case 'action_page':
      if (cards.length < 2 || cards.length > 4 || !cards.every(card => card.cardKind === 'action_item'))
        errors.push(`Action page ${section.id} must contain 2-4 action cards.`);
      break;
    case 'pre_game_checklist':
      if (cards.length !== 1 || cards[0]?.cardKind !== 'checklist')
        errors.push(`Pre-game checklist ${section.id} must contain exactly one checklist card.`);
      break;
  }

  for (const card of cards) {
    if (card.cardKind === 'anchor_position') {
      anchorIds.add(card.id);
      const expectedLens = dossier.productKind === 'my_account_intelligence_lite' ? 'self_repair' : 'opponent_pressure';
      if (card.lens !== expectedLens)
        errors.push(`Anchor card ${card.id} has lens ${card.lens}, expected ${expectedLens}.`);
    }
  }
}

function validateProductDefaults(dossier: NotebookDossierV1, errors: string[]): void {
  const sections = dossier.sections;
  const openingCount = collectSectionCount(sections, 'opening_map');
  const patternCount = collectSectionCount(sections, 'pattern_cluster');
  const exemplarCount = collectSectionCount(sections, 'exemplar_games');
  const actionCount = collectSectionCount(sections, 'action_page');
  const steeringCount = collectSectionCount(sections, 'steering_plan');
  const checklistCount = collectSectionCount(sections, 'pre_game_checklist');

  if (openingCount !== 1) errors.push('Notebook dossier must contain exactly one opening_map section.');
  if (patternCount < 2 || patternCount > 3) errors.push('Notebook dossier must contain 2-3 pattern_cluster sections.');

  if (dossier.productKind === 'my_account_intelligence_lite') {
    if (dossier.subject.role !== 'self') errors.push('My Account dossiers must use subject.role=self.');
    if (exemplarCount !== 1) errors.push('My Account dossiers must contain exactly one exemplar_games section.');
    if (actionCount !== 1) errors.push('My Account dossiers must contain exactly one action_page section.');
    if (steeringCount !== 0) errors.push('My Account dossiers cannot contain steering_plan sections.');
    if (checklistCount !== 0) errors.push('My Account dossiers cannot contain pre_game_checklist sections.');
  } else {
    if (dossier.subject.role !== 'opponent') errors.push('Opponent Prep dossiers must use subject.role=opponent.');
    if (steeringCount !== 1) errors.push('Opponent Prep dossiers must contain exactly one steering_plan section.');
    if (checklistCount !== 1) errors.push('Opponent Prep dossiers must contain exactly one pre_game_checklist section.');
    if (actionCount !== 0) errors.push('Opponent Prep dossiers cannot contain action_page sections.');
    if (exemplarCount > 1) errors.push('Opponent Prep dossiers can contain at most one exemplar_games section.');
  }
}

export function validateNotebookDossier(raw: unknown): string[] {
  const errors: string[] = [];
  if (!isObject(raw)) return ['Notebook dossier must be an object.'];
  if (raw.schema !== NOTEBOOK_DOSSIER_SCHEMA_V1) errors.push(`schema must equal ${NOTEBOOK_DOSSIER_SCHEMA_V1}.`);
  if (!isString(raw.dossierId)) errors.push('dossierId must be a string.');
  if (raw.productKind !== 'my_account_intelligence_lite' && raw.productKind !== 'opponent_prep')
    errors.push('productKind must be my_account_intelligence_lite or opponent_prep.');
  if (!isObject(raw.subject)) errors.push('subject must be an object.');
  if (!isObject(raw.source)) errors.push('source must be an object.');
  if (raw.status !== 'ready' && raw.status !== 'partial') errors.push('status must be ready or partial.');
  if (!isString(raw.headline)) errors.push('headline must be a string.');
  if (!isString(raw.summary)) errors.push('summary must be a string.');
  if (!isObject(raw.overview)) errors.push('overview must be an object.');
  if (!Array.isArray(raw.sections)) errors.push('sections must be an array.');
  if (errors.length) return errors;

  const dossier = raw as NotebookDossierV1;

  if (!isSubjectProvider(dossier.subject.provider)) errors.push('subject.provider must be chesscom or lichess.');
  if (!isString(dossier.subject.username)) errors.push('subject.username must be a string.');
  if (!isString(dossier.subject.displayName)) errors.push('subject.displayName must be a string.');
  if (dossier.subject.role !== 'self' && dossier.subject.role !== 'opponent')
    errors.push('subject.role must be self or opponent.');

  if (!isNumber(dossier.source.requestedGameLimit) || dossier.source.requestedGameLimit < 1)
    errors.push('source.requestedGameLimit must be a positive number.');
  if (!isNumber(dossier.source.sampledGameCount) || dossier.source.sampledGameCount < 0)
    errors.push('source.sampledGameCount must be a non-negative number.');
  if (!isNumber(dossier.source.eligibleGameCount) || dossier.source.eligibleGameCount < 0)
    errors.push('source.eligibleGameCount must be a non-negative number.');
  if (dossier.source.publicOnly !== true) errors.push('source.publicOnly must be true.');
  if (dossier.source.includeTimeControl !== false) errors.push('source.includeTimeControl must be false.');
  if (!isString(dossier.source.generatedAt) || Number.isNaN(Date.parse(dossier.source.generatedAt)))
    errors.push('source.generatedAt must be a valid ISO-8601 string.');
  if (!isObject(dossier.source.cache)) errors.push('source.cache must be an object.');
  else {
    if (!isBoolean(dossier.source.cache.hit)) errors.push('source.cache.hit must be a boolean.');
    if (!isNumber(dossier.source.cache.ttlSec) || dossier.source.cache.ttlSec < 0)
      errors.push('source.cache.ttlSec must be a non-negative number.');
    if (dossier.source.cache.ageSec !== undefined && (!isNumber(dossier.source.cache.ageSec) || dossier.source.cache.ageSec < 0))
      errors.push('source.cache.ageSec must be a non-negative number when present.');
  }

  if (dossier.source.sampledGameCount < 8 && dossier.status !== 'partial')
    errors.push('Dossiers with fewer than 8 sampled games must be partial.');

  if (dossier.overview.title !== 'Overview') errors.push('overview.title must equal Overview.');
  if (!isString(dossier.overview.dek)) errors.push('overview.dek must be a string.');
  if (!Array.isArray(dossier.overview.cards)) errors.push('overview.cards must be an array.');
  else {
    if (dossier.overview.cards.length !== 3) errors.push('overview.cards must contain exactly 3 cards.');
    dossier.overview.cards.forEach((card, index) => {
      if (!isObject(card)) {
        errors.push(`overview.cards[${index}] must be an object.`);
        return;
      }
      if (!isString(card.id)) errors.push(`overview card ${index} must have a string id.`);
      if (!isOverviewCardKind(card.kind)) errors.push(`overview card ${index} has an invalid kind.`);
      if (!isString(card.title) || !isString(card.headline) || !isString(card.summary))
        errors.push(`overview card ${index} must include title, headline, and summary strings.`);
      if (!isPriority(card.priority)) errors.push(`overview card ${index} has an invalid priority.`);
      if (!isEvidenceStrength(card.confidence)) errors.push(`overview card ${index} has an invalid confidence.`);
      if (!isEvidenceSummary(card.evidence)) errors.push(`overview card ${index} evidence is invalid.`);
      if (card.tags !== undefined && !isStringArray(card.tags)) errors.push(`overview card ${index} tags must be a string array.`);
      if (card.linkedSectionId !== undefined && !isString(card.linkedSectionId))
        errors.push(`overview card ${index} linkedSectionId must be a string.`);
      if (card.linkedCardId !== undefined && !isString(card.linkedCardId))
        errors.push(`overview card ${index} linkedCardId must be a string.`);
    });
    const expectedOrder = overviewKindOrder[dossier.productKind];
    for (const [index, expectedKind] of expectedOrder.entries()) {
      if (dossier.overview.cards[index]?.kind !== expectedKind)
        errors.push(`overview.cards[${index}] must use kind ${expectedKind}.`);
    }
  }

  const sectionIds = new Set<string>();
  const sectionRanks = new Set<number>();
  const cardIds = new Set<string>();
  const anchorIds = new Set<string>();
  const sampledGameIds = new Set<string>();
  const referencedGameIds = new Set<string>();
  let previousRank = -Infinity;

  if (dossier.appendix !== undefined) {
    if (!isObject(dossier.appendix)) errors.push('appendix must be an object.');
    else {
      if (!Array.isArray(dossier.appendix.sampledGames) || !dossier.appendix.sampledGames.every(isGameEvidenceRef))
        errors.push('appendix.sampledGames must be an array of game references.');
      else dossier.appendix.sampledGames.forEach(game => sampledGameIds.add(game.id));
      if (dossier.appendix.warnings !== undefined && !isStringArray(dossier.appendix.warnings))
        errors.push('appendix.warnings must be a string array when present.');
    }
  }

  dossier.sections.forEach((section, sectionIndex) => {
    if (!isObject(section)) {
      errors.push(`sections[${sectionIndex}] must be an object.`);
      return;
    }
    if (!isString(section.id)) errors.push(`sections[${sectionIndex}].id must be a string.`);
    else if (sectionIds.has(section.id)) errors.push(`Duplicate section id ${section.id}.`);
    else sectionIds.add(section.id);

    if (!isSectionKind(section.kind)) errors.push(`Section ${section.id || sectionIndex} has an invalid kind.`);
    if (!isString(section.title) || !isString(section.summary))
      errors.push(`Section ${section.id || sectionIndex} must include title and summary strings.`);
    if (!isNumber(section.rank)) errors.push(`Section ${section.id || sectionIndex} rank must be numeric.`);
    else {
      if (sectionRanks.has(section.rank)) errors.push(`Duplicate section rank ${section.rank}.`);
      if (section.rank <= previousRank) errors.push(`Section ranks must be strictly increasing. Problem at ${section.id || sectionIndex}.`);
      sectionRanks.add(section.rank);
      previousRank = section.rank;
    }
    if (section.status !== 'ready' && section.status !== 'partial')
      errors.push(`Section ${section.id || sectionIndex} has an invalid status.`);
    if (section.evidence !== undefined && !isEvidenceSummary(section.evidence))
      errors.push(`Section ${section.id || sectionIndex} evidence is invalid.`);
    if (!Array.isArray(section.cards) || !section.cards.every(isSectionCard))
      errors.push(`Section ${section.id || sectionIndex} cards are invalid.`);
    else {
      section.cards.forEach(card => {
        if (cardIds.has(card.id)) errors.push(`Duplicate card id ${card.id}.`);
        else cardIds.add(card.id);
        switch (card.cardKind) {
          case 'opening_map':
            card.evidence.supportingGameIds.forEach(id => referencedGameIds.add(id));
            break;
          case 'anchor_position':
            card.evidence.supportingGameIds.forEach(id => referencedGameIds.add(id));
            if (card.exemplarGameId) referencedGameIds.add(card.exemplarGameId);
            break;
          case 'exemplar_game':
            referencedGameIds.add(card.game.id);
            break;
        }
      });
      validateSectionComposition(dossier, section as NotebookSectionV1, errors, anchorIds);
    }
  });

  validateProductDefaults(dossier, errors);

  dossier.overview.cards.forEach((card, index) => {
    if (!isObject(card)) return;
    if (card.linkedSectionId && !sectionIds.has(card.linkedSectionId))
      errors.push(`Overview card ${card.id || index} links to missing section ${card.linkedSectionId}.`);
    if (card.linkedCardId && !anchorIds.has(card.linkedCardId))
      errors.push(`Overview card ${card.id || index} links to missing anchor card ${card.linkedCardId}.`);
  });

  dossier.sections.forEach((section, index) => {
    if (!isObject(section) || !Array.isArray(section.cards)) return;
    section.cards.forEach(card => {
      if (!isSectionCard(card)) return;
      if (card.cardKind === 'exemplar_game') {
        card.linkedAnchorPositionIds.forEach(anchorId => {
          if (!anchorIds.has(anchorId))
            errors.push(`Exemplar card ${card.id} links to missing anchor card ${anchorId}.`);
        });
      }
      if (card.cardKind === 'action_item') {
        card.linkedAnchorPositionIds?.forEach(anchorId => {
          if (!anchorIds.has(anchorId))
            errors.push(`Action item ${card.id} links to missing anchor card ${anchorId}.`);
        });
      }
    });
    if (!isString(section.id) && index >= 0) {
      errors.push(`Section ${index} is missing a valid id.`);
    }
  });

  if (referencedGameIds.size > 0 && sampledGameIds.size === 0)
    errors.push('appendix.sampledGames must be present when cards reference game ids.');
  referencedGameIds.forEach(gameId => {
    if (!sampledGameIds.has(gameId)) errors.push(`Referenced game id ${gameId} is missing from appendix.sampledGames.`);
  });

  return errors;
}

export function parseNotebookDossier(raw: unknown): NotebookDossierV1 | null {
  return validateNotebookDossier(raw).length ? null : (raw as NotebookDossierV1);
}

export function notebookDossierProductLabel(kind: NotebookDossierProductKind): string {
  return kind === 'my_account_intelligence_lite' ? 'My Account Intelligence Lite' : 'Opponent Prep';
}

export function notebookDossierOverviewKindLabel(kind: OverviewCardKind): string {
  switch (kind) {
    case 'opening_identity':
      return 'Opening identity';
    case 'recurring_leak':
      return 'Recurring leak';
    case 'repair_priority':
      return 'Repair priority';
    case 'pressure_point':
      return 'Pressure point';
    case 'steering_target':
      return 'Steering target';
  }
}

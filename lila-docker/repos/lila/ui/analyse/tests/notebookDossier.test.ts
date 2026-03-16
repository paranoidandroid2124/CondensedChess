import { describe, test } from 'node:test';
import assert from 'node:assert/strict';
import {
  NOTEBOOK_DOSSIER_SCHEMA_V1,
  parseNotebookDossier,
  validateNotebookDossier,
  type NotebookDossierV1,
} from '../src/notebookDossier';

type DossierOverrides = Omit<Partial<NotebookDossierV1>, 'source'> & {
  source?: Partial<NotebookDossierV1['source']>;
};

describe('notebook dossier schema', () => {
  test('valid my account dossier passes validation', () => {
    const dossier = makeMyAccountDossier();

    assert.deepEqual(validateNotebookDossier(dossier), []);
    assert.equal(parseNotebookDossier(dossier)?.productKind, 'my_account_intelligence_lite');
  });

  test('valid opponent prep dossier passes validation', () => {
    const dossier = makeOpponentPrepDossier();

    assert.deepEqual(validateNotebookDossier(dossier), []);
    assert.equal(parseNotebookDossier(dossier)?.productKind, 'opponent_prep');
  });

  test('wrong overview order fails validation', () => {
    const dossier = makeMyAccountDossier();
    [dossier.overview.cards[1], dossier.overview.cards[2]] = [dossier.overview.cards[2], dossier.overview.cards[1]];

    const errors = validateNotebookDossier(dossier);

    assert.match(errors.join('\n'), /overview\.cards\[1\] must use kind recurring_leak/i);
  });

  test('overview linkedCardId must resolve to an anchor card', () => {
    const dossier = makeMyAccountDossier();
    dossier.overview.cards[0]!.linkedCardId = 'act-a1';

    const errors = validateNotebookDossier(dossier);

    assert.match(errors.join('\n'), /links to missing anchor card act-a1/i);
  });

  test('partial dossier can keep pattern clusters without anchor cards', () => {
    const dossier = makeOpponentPrepDossier({
      status: 'partial',
      source: {
        sampledGameCount: 6,
      },
    });
    const firstPattern = dossier.sections.find(section => section.id === 'prep-pattern-1');
    if (!firstPattern) throw new Error('missing pattern section');
    firstPattern.status = 'partial';
    firstPattern.cards = [
      {
        cardKind: 'action_item',
        id: 'prep-pattern-1-action',
        title: 'Delay the release',
        instruction: 'Keep the center closed until the minor pieces are committed.',
        successMarker: 'Opponent has to choose a structure before finishing development.',
      },
    ];
    const steeringPlan = dossier.sections.find(section => section.id === 'prep-steering');
    if (!steeringPlan) throw new Error('missing steering section');
    steeringPlan.cards = steeringPlan.cards.map(card =>
      card.cardKind === 'action_item' && card.id === 'prep-plan-1'
        ? { ...card, linkedAnchorPositionIds: undefined }
        : card,
    );
    dossier.overview.cards[0]!.linkedCardId = undefined;
    dossier.overview.cards[1]!.linkedCardId = undefined;

    assert.deepEqual(validateNotebookDossier(dossier), []);
  });

  test('referenced game ids must exist in appendix sampled games', () => {
    const dossier = makeMyAccountDossier();
    dossier.appendix!.sampledGames = dossier.appendix!.sampledGames.filter(game => game.id !== 'g2');

    const errors = validateNotebookDossier(dossier);

    assert.match(errors.join('\n'), /Referenced game id g2 is missing from appendix\.sampledGames/i);
  });
});

function makeMyAccountDossier(overrides: DossierOverrides = {}): NotebookDossierV1 {
  const base: NotebookDossierV1 = {
    schema: NOTEBOOK_DOSSIER_SCHEMA_V1,
    dossierId: 'acct-ych24',
    productKind: 'my_account_intelligence_lite',
    subject: {
      role: 'self',
      provider: 'chesscom',
      username: 'ych24',
      displayName: 'ych24',
    },
    source: {
      requestedGameLimit: 40,
      sampledGameCount: 12,
      eligibleGameCount: 18,
      publicOnly: true,
      includeTimeControl: false,
      generatedAt: '2026-03-16T12:30:00.000Z',
      cache: { hit: false, ttlSec: 86400, ageSec: 0 },
    },
    status: 'ready',
    headline: 'The opening is stable, but the first structural release keeps blurring the plan.',
    summary: 'This notebook highlights recurring repair spots from recent public games.',
    overview: {
      title: 'Overview',
      dek: 'A concise map of the recurring patterns from recent public games.',
      cards: [
        {
          id: 'ov-open',
          kind: 'opening_identity',
          title: 'Opening identity',
          headline: 'Queen pawn structures appear most often.',
          summary: 'The games repeatedly reach closed or semi-closed center structures.',
          priority: 'medium',
          confidence: 'strong',
          evidence: { strength: 'strong', supportingGames: 8, totalSampledGames: 12, occurrenceCount: 8, clusterSharePct: 66.7 },
          tags: ['d4', 'closed center'],
          linkedSectionId: 'opening-map',
          linkedCardId: 'anchor-a1',
        },
        {
          id: 'ov-leak',
          kind: 'recurring_leak',
          title: 'Recurring leak',
          headline: 'The first structural release often hands over the initiative.',
          summary: 'A repeated pattern appears right after central tension is resolved.',
          priority: 'high',
          confidence: 'strong',
          evidence: { strength: 'strong', supportingGames: 5, totalSampledGames: 12, occurrenceCount: 5, clusterSharePct: 41.7 },
          linkedSectionId: 'pattern-a',
          linkedCardId: 'anchor-a1',
        },
        {
          id: 'ov-repair',
          kind: 'repair_priority',
          title: 'Repair priority',
          headline: 'Fix the handoff from equal structure to active piece plan.',
          summary: 'Two sections isolate the earliest preventable decision points.',
          priority: 'high',
          confidence: 'medium',
          evidence: { strength: 'medium', supportingGames: 4, totalSampledGames: 12, occurrenceCount: 4, clusterSharePct: 33.3 },
          linkedSectionId: 'pattern-b',
          linkedCardId: 'anchor-b1',
        },
      ],
    },
    sections: [
      {
        id: 'opening-map',
        kind: 'opening_map',
        title: 'Opening map',
        summary: 'The recurring families and structures from the recent sample.',
        rank: 1,
        status: 'ready',
        cards: [
          {
            cardKind: 'opening_map',
            id: 'opening-card',
            title: 'Closed center map',
            side: 'mixed',
            openingFamily: 'Queen Pawn Games',
            structureLabels: ['closed center', 'queenside tension'],
            story: 'The sample most often lands in slow middlegames where timing matters more than forcing lines.',
            evidence: { strength: 'strong', supportingGames: 8, totalSampledGames: 12, occurrenceCount: 8, clusterSharePct: 66.7, supportingGameIds: ['g1', 'g2', 'g3'] },
          },
        ],
      },
      {
        id: 'pattern-a',
        kind: 'pattern_cluster',
        title: 'Plan fades after the center opens',
        summary: 'The first release of central tension often hands over the initiative.',
        rank: 2,
        status: 'ready',
        evidence: { strength: 'strong', supportingGames: 5, totalSampledGames: 12, occurrenceCount: 5, clusterSharePct: 41.7 },
        cards: [
          {
            cardKind: 'anchor_position',
            id: 'anchor-a1',
            clusterId: 'cluster-a',
            title: 'Do not release before the heavy pieces know their files',
            lens: 'self_repair',
            claim: 'The structure is still playable, but the next release must serve a clear file plan.',
            explanation: 'The repeated loss comes from resolving the center before deciding which rook and knight gain activity.',
            fen: 'r2q1rk1/ppp2ppp/2n1bn2/3p4/3P4/2P1PN2/PP3PPP/R1BQ1RK1 w - - 0 12',
            moveContext: { ply: 24, moveNumber: 12, sideToMove: 'white', lastMoveSan: '...Be6' },
            boardFocus: {
              squares: ['c1', 'd4', 'e5'],
              arrows: [{ from: 'f1', to: 'e1', label: 'file first' }],
            },
            strategicTags: ['center tension', 'file control'],
            questionPrompt: 'Which file should be claimed before the center opens?',
            recommendedPlan: {
              label: 'Prepare the file handoff first',
              summary: 'Place the rook and improve the knight before exchanging the center.',
              candidateMoves: [{ san: 'Re1' }, { san: 'Nbd2', note: 'keep c4 available later' }],
            },
            antiPattern: {
              label: 'Release too early',
              summary: 'The immediate exchange solves the tension for the opponent.',
              playedMoveSan: 'dxe5',
              estimatedCostCp: 64,
            },
            evidence: {
              strength: 'strong',
              supportingGames: 5,
              totalSampledGames: 12,
              occurrenceCount: 5,
              clusterSharePct: 41.7,
              supportingGameIds: ['g1', 'g2'],
            },
            exemplarGameId: 'g1',
          },
          {
            cardKind: 'action_item',
            id: 'act-a1',
            title: 'Delay the release',
            instruction: 'Before exchanging in the center, decide which rook and knight improve afterward.',
            successMarker: 'You can name the open file and the worst piece before the exchange.',
            linkedAnchorPositionIds: ['anchor-a1'],
          },
        ],
      },
      {
        id: 'pattern-b',
        kind: 'pattern_cluster',
        title: 'Queenside gains vanish without a second wave',
        summary: 'The first space gain often stalls when the follow-up route is missing.',
        rank: 3,
        status: 'ready',
        evidence: { strength: 'medium', supportingGames: 4, totalSampledGames: 12, occurrenceCount: 4, clusterSharePct: 33.3 },
        cards: [
          {
            cardKind: 'anchor_position',
            id: 'anchor-b1',
            clusterId: 'cluster-b',
            title: 'After gaining queenside space, bring the next piece immediately',
            lens: 'self_repair',
            claim: 'The space edge only matters if the next piece arrives before the counterplay starts.',
            explanation: 'The recurring issue is not the pawn thrust itself, but the missing follow-up coordination.',
            fen: '2rq1rk1/pp3ppp/2n1bn2/2pp4/2P5/1PN1PN2/PB2QPPP/2RR2K1 w - - 0 16',
            moveContext: { ply: 32, moveNumber: 16, sideToMove: 'white', lastMoveSan: '...d5' },
            strategicTags: ['queenside space', 'second wave'],
            questionPrompt: 'Which piece must join the queenside before Black untangles?',
            recommendedPlan: {
              label: 'Route the knight before expanding again',
              summary: 'Improve the nearest minor piece so the pawn space turns into a real bind.',
              candidateMoves: [{ san: 'Na4' }, { san: 'Rc2', note: 'keep lateral defense ready' }],
            },
            evidence: {
              strength: 'medium',
              supportingGames: 4,
              totalSampledGames: 12,
              occurrenceCount: 4,
              clusterSharePct: 33.3,
              supportingGameIds: ['g2', 'g3'],
            },
            exemplarGameId: 'g2',
          },
          {
            cardKind: 'action_item',
            id: 'act-b1',
            title: 'Name the second wave',
            instruction: 'Every queenside advance should be followed by a piece route, not a second pawn push.',
            successMarker: 'The next improving piece is identified before committing another pawn move.',
            linkedAnchorPositionIds: ['anchor-b1'],
          },
        ],
      },
      {
        id: 'examples',
        kind: 'exemplar_games',
        title: 'Representative games',
        summary: 'Two anchor examples show how the same strategic leak repeats.',
        rank: 4,
        status: 'ready',
        cards: [
          {
            cardKind: 'exemplar_game',
            id: 'game-a',
            title: 'Space edge disappears after the release',
            game: sampleGame('g1'),
            whyItMatters: 'This is the cleanest example of the main recurring leak.',
            narrative: 'The opening is playable, but the first structural release gives Black the easier play.',
            turningPointPly: 24,
            linkedAnchorPositionIds: ['anchor-a1'],
            takeaway: 'Delay the exchange until the file plan is already named.',
          },
        ],
      },
      {
        id: 'actions',
        kind: 'action_page',
        title: 'Next ten games',
        summary: 'Concrete repair targets for the next review cycle.',
        rank: 5,
        status: 'ready',
        cards: [
          {
            cardKind: 'action_item',
            id: 'action-1',
            title: 'Keep the center closed longer',
            instruction: 'Do not open the center before the rook file is chosen.',
            successMarker: 'The file plan is stated before the first exchange.',
            linkedAnchorPositionIds: ['anchor-a1'],
          },
          {
            cardKind: 'action_item',
            id: 'action-2',
            title: 'Pair every space gain with a route',
            instruction: 'When a flank pawn advances, identify the piece that follows it.',
            successMarker: 'A piece route is written down before the second pawn move.',
            linkedAnchorPositionIds: ['anchor-b1'],
          },
        ],
      },
    ],
    appendix: {
      sampledGames: [sampleGame('g1'), sampleGame('g2'), sampleGame('g3')],
      warnings: ['Public games only.'],
    },
  };

  return {
    ...base,
    ...overrides,
    source: {
      ...base.source,
      ...overrides.source,
    },
  };
}

function makeOpponentPrepDossier(overrides: DossierOverrides = {}): NotebookDossierV1 {
  const base: NotebookDossierV1 = {
    schema: NOTEBOOK_DOSSIER_SCHEMA_V1,
    dossierId: 'prep-ych24',
    productKind: 'opponent_prep',
    subject: {
      role: 'opponent',
      provider: 'lichess',
      username: 'ych24',
      displayName: 'ych24',
    },
    source: {
      requestedGameLimit: 32,
      sampledGameCount: 14,
      eligibleGameCount: 20,
      publicOnly: true,
      includeTimeControl: false,
      generatedAt: '2026-03-16T12:45:00.000Z',
      cache: { hit: true, ttlSec: 604800, ageSec: 1200 },
    },
    status: 'ready',
    headline: 'The target is most uncomfortable when equal structures are kept unresolved for one move longer.',
    summary: 'This notebook focuses on where to steer the game rather than raw results.',
    overview: {
      title: 'Overview',
      dek: 'Steer the game toward recurring discomfort points.',
      cards: [
        {
          id: 'prep-open',
          kind: 'opening_identity',
          title: 'Opening identity',
          headline: 'Semi-closed queen pawn structures dominate the sample.',
          summary: 'The target returns to the same structural family from both colors.',
          priority: 'medium',
          confidence: 'strong',
          evidence: { strength: 'strong', supportingGames: 9, totalSampledGames: 14, occurrenceCount: 9, clusterSharePct: 64.3 },
          linkedSectionId: 'prep-opening-map',
          linkedCardId: 'prep-anchor-a1',
        },
        {
          id: 'prep-pressure',
          kind: 'pressure_point',
          title: 'Pressure point',
          headline: 'Delayed structural choices create the most discomfort.',
          summary: 'Keep the center unresolved long enough to force an imprecise release.',
          priority: 'high',
          confidence: 'strong',
          evidence: { strength: 'strong', supportingGames: 6, totalSampledGames: 14, occurrenceCount: 6, clusterSharePct: 42.9 },
          linkedSectionId: 'prep-pattern-1',
          linkedCardId: 'prep-anchor-a1',
        },
        {
          id: 'prep-steer',
          kind: 'steering_target',
          title: 'Steering target',
          headline: 'Aim for a queenside bind before the target fully coordinates.',
          summary: 'The target often mishandles the second wave after conceding space.',
          priority: 'high',
          confidence: 'medium',
          evidence: { strength: 'medium', supportingGames: 4, totalSampledGames: 14, occurrenceCount: 4, clusterSharePct: 28.6 },
          linkedSectionId: 'prep-pattern-2',
          linkedCardId: 'prep-anchor-b1',
        },
      ],
    },
    sections: [
      {
        id: 'prep-opening-map',
        kind: 'opening_map',
        title: 'Opening map',
        summary: 'The most common families and resulting structures.',
        rank: 1,
        status: 'ready',
        cards: [
          {
            cardKind: 'opening_map',
            id: 'prep-opening-card',
            title: 'Semi-closed center map',
            side: 'mixed',
            openingFamily: 'Queen Pawn Games',
            structureLabels: ['semi-closed center', 'queenside space'],
            story: 'The target rarely explodes the game early and instead drifts into equal middlegames.',
            evidence: { strength: 'strong', supportingGames: 9, totalSampledGames: 14, occurrenceCount: 9, clusterSharePct: 64.3, supportingGameIds: ['g1', 'g2', 'g4'] },
          },
        ],
      },
      {
        id: 'prep-pattern-1',
        kind: 'pattern_cluster',
        title: 'Hold the tension longer',
        summary: 'The target often releases central tension before the pieces are coordinated.',
        rank: 2,
        status: 'ready',
        evidence: { strength: 'strong', supportingGames: 6, totalSampledGames: 14, occurrenceCount: 6, clusterSharePct: 42.9 },
        cards: [
          {
            cardKind: 'anchor_position',
            id: 'prep-anchor-a1',
            clusterId: 'prep-cluster-a',
            title: 'One extra waiting move often forces the wrong release',
            lens: 'opponent_pressure',
            claim: 'The target becomes less precise when equal central tension remains unresolved.',
            explanation: 'The best way to press is to keep multiple files undecided and force a structural commitment.',
            fen: 'r1bq1rk1/pp3ppp/2n1pn2/2bp4/2P5/1PN1PN2/PB2QPPP/2RR2K1 w - - 0 12',
            moveContext: { ply: 24, moveNumber: 12, sideToMove: 'white', lastMoveSan: '...Bd6' },
            strategicTags: ['hold tension', 'force release'],
            questionPrompt: 'Which improving move keeps the most structural questions open?',
            recommendedPlan: {
              label: 'Improve before exchanging',
              summary: 'Refuse to clarify the center until the opponent reveals the releasing move.',
              candidateMoves: [{ san: 'h3' }, { san: 'a3', note: 'reserve b4 later' }],
            },
            antiPattern: {
              label: 'Immediate simplification',
              summary: 'Resolving the center right away makes the target more comfortable.',
              playedMoveSan: 'cxd5',
              estimatedCostCp: 43,
            },
            evidence: {
              strength: 'strong',
              supportingGames: 6,
              totalSampledGames: 14,
              occurrenceCount: 6,
              clusterSharePct: 42.9,
              supportingGameIds: ['g1', 'g4'],
            },
            exemplarGameId: 'g4',
          },
          {
            cardKind: 'action_item',
            id: 'prep-act-a1',
            title: 'Keep the release ambiguous',
            instruction: 'Choose an improving move that preserves multiple structural outcomes.',
            successMarker: 'The opponent must decide the first exchange, not you.',
            linkedAnchorPositionIds: ['prep-anchor-a1'],
          },
        ],
      },
      {
        id: 'prep-pattern-2',
        kind: 'pattern_cluster',
        title: 'Queenside space invites a late coordination error',
        summary: 'The target often misplaces the next piece after conceding space.',
        rank: 3,
        status: 'ready',
        evidence: { strength: 'medium', supportingGames: 4, totalSampledGames: 14, occurrenceCount: 4, clusterSharePct: 28.6 },
        cards: [
          {
            cardKind: 'anchor_position',
            id: 'prep-anchor-b1',
            clusterId: 'prep-cluster-b',
            title: 'Space first, then reroute before tactics',
            lens: 'opponent_pressure',
            claim: 'The target often loses the thread when the bind requires one quiet rerouting move.',
            explanation: 'This is the cleanest way to convert a positional edge into an actionable plan.',
            fen: '2rq1rk1/pp3ppp/2n1bn2/2pp4/2P5/1PN1PN2/PB2QPPP/2RR2K1 w - - 0 16',
            moveContext: { ply: 32, moveNumber: 16, sideToMove: 'white', lastMoveSan: '...d5' },
            strategicTags: ['space clamp', 'reroute'],
            questionPrompt: 'Which quiet route keeps the bind alive for one more cycle?',
            recommendedPlan: {
              label: 'Reroute before forcing',
              summary: 'Improve the nearest piece and only then start concrete queenside play.',
              candidateMoves: [{ san: 'Na4' }, { san: 'Rc2' }],
            },
            evidence: {
              strength: 'medium',
              supportingGames: 4,
              totalSampledGames: 14,
              occurrenceCount: 4,
              clusterSharePct: 28.6,
              supportingGameIds: ['g2', 'g4'],
            },
            exemplarGameId: 'g2',
          },
        ],
      },
      {
        id: 'prep-steering',
        kind: 'steering_plan',
        title: 'Where to steer the game',
        summary: 'Use the recurring pressure points to guide move selection.',
        rank: 4,
        status: 'ready',
        cards: [
          {
            cardKind: 'action_item',
            id: 'prep-plan-1',
            title: 'Delay central clarification',
            instruction: 'Preserve equal central tension if it keeps more than one file unresolved.',
            successMarker: 'The target makes the first structural commitment.',
            linkedAnchorPositionIds: ['prep-anchor-a1'],
          },
          {
            cardKind: 'action_item',
            id: 'prep-plan-2',
            title: 'Pair space with rerouting',
            instruction: 'After gaining space, improve the nearest piece before starting forcing play.',
            successMarker: 'The bind remains intact for one extra cycle.',
            linkedAnchorPositionIds: ['prep-anchor-b1'],
          },
        ],
      },
      {
        id: 'prep-checklist',
        kind: 'pre_game_checklist',
        title: 'Pre-game checklist',
        summary: 'Short reminders before the round starts.',
        rank: 5,
        status: 'ready',
        cards: [
          {
            cardKind: 'checklist',
            id: 'prep-checklist-card',
            title: 'Round checklist',
            items: [
              { id: 'c1', label: 'Do not resolve the center first.', priority: 'high' },
              { id: 'c2', label: 'Aim for a queenside bind, then reroute.', priority: 'medium' },
            ],
          },
        ],
      },
    ],
    appendix: {
      sampledGames: [sampleGame('g1'), sampleGame('g2'), sampleGame('g4')],
    },
  };

  return {
    ...base,
    ...overrides,
    source: {
      ...base.source,
      ...overrides.source,
    },
  };
}

function sampleGame(id: string) {
  return {
    id,
    provider: 'chesscom' as const,
    white: id === 'g4' ? 'opponent' : 'ych24',
    black: id === 'g4' ? 'ych24' : 'opponent',
    result: id === 'g2' ? '0-1' : '1-0',
    opening: 'Queen Pawn Game',
    role: 'support' as const,
  };
}

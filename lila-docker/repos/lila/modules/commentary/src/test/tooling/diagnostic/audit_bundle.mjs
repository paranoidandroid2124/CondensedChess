#!/usr/bin/env node
import fs from 'node:fs';
import path from 'node:path';

const repoRoot = process.cwd();
const base = path.join(repoRoot, 'tmp/commentary-diagnostic');
const outDir = path.join(base, 'audit-bundle', 'materialized300-enriched');

const files = {
  projectionLedger: path.join(base, 'king-attack-projection-impact/materialized300-enriched/king-attack-projection-probe-admitted-fen-ledger.jsonl'),
  s02Audit: path.join(base, 'king-attack-projection-impact/materialized300-enriched/s02-full-audit.md'),
  residualLedger: path.join(base, 'attack-scaffold-impact/materialized300-enriched/attack-scaffold-current-residual-ledger.jsonl'),
  droppedLedger: path.join(base, 'attack-scaffold-impact/materialized300-enriched/attack-scaffold-dropped-source-ledger.jsonl'),
  serialSlices: path.join(base, 'lower-layer/materialized300-enriched/serial-audit-plan/serial-audit-slices.jsonl'),
  serialDir: path.join(base, 'lower-layer/materialized300-enriched/serial-audit-plan'),
  highRiskSummary: path.join(base, 'lower-layer/materialized300-enriched/high-risk-tactical-audit/high-risk-tactical-audit-summary.json'),
};

const S02_VERDICTS = new Map([
  ['1r2r1k1/3p1pp1/7p/4q3/PP1R4/8/1Q3PPP/5RK1 b - - 0 29', 'reject_low_significance'],
  ['2b2rk1/p3r1pn/1p1p4/n1pP3p/2P1p2q/2P1B2P/P1BNQPP1/2R1R1K1 b - - 5 25', 'support_only'],
  ['3r1rk1/pp2ppbp/1qn3p1/3R1b2/Q7/2P3P1/PP2PPBP/R1B1N1K1 b - - 2 13', 'reject_wrong_theater'],
  ['3r2k1/5ppp/1bp1q1n1/4p1Pb/4P3/1BP3QP/1P1N1P2/2B1R1K1 b - - 10 25', 'support_only'],
  ['5rk1/pn5n/1p1p2p1/2pPrb1p/P1P1p2q/1NP1B2P/3Q1PP1/2RBR1K1 b - - 2 29', 'public_ready_candidate'],
  ['5rk1/pp2n1p1/2q1p2p/2p2r2/6N1/2PP3P/P1P1Q1P1/R3R1K1 b - - 5 21', 'support_only'],
  ['r1b2rk1/b1RB4/p2p4/n3p1qp/6p1/2PP2B1/3N1PP1/3Q1RK1 b - - 1 25', 'support_only_bidirectional'],
  ['r1b3k1/bpp3q1/P1np4/1B2prPp/6p1/2PP2B1/3N1PP1/R2Q1RK1 b - - 0 21', 'support_only_promising'],
  ['r1br2k1/1pq2ppp/p1n1pn2/2b5/P1B1P3/2N2N1P/1P2QPP1/R1B1R1K1 b - - 2 13', 'reject_opening_shape'],
  ['r1r3k1/pp2pp1p/3pb1p1/2q5/2P1P3/1PNQ4/P4PPP/R3R1K1 b - - 4 17', 'reject_wrong_theater'],
  ['r2r2k1/p6p/2p1Ppp1/3b1P2/3B3q/8/1Q3RPP/4R1K1 b - - 3 25', 'reject_low_significance'],
  ['r3r1k1/b1p1npp1/p2p1q1p/P2P4/2N1P3/3Q1N2/5PPP/2R2RK1 b - - 0 21', 'support_only'],
  ['r4rk1/p4ppp/bqp1pn2/8/4P3/2Q3PP/PP1N1PB1/R2R2K1 b - - 3 17', 'reject_geometry_only'],
]);

const PROJECTION_EXTRA_VERDICTS = new Map([
  ['2r3k1/2r2p1p/3p1pp1/p2b1P2/3P3q/1P1Q1P2/P2B2R1/5RK1 b - - 1 29', 'support_only_second_rank_pressure'],
  ['5rk1/1p2n1pp/2n5/1p1b4/r5P1/P3P3/1P1BB2N/2R2RK1 b - - 2 21', 'reject_queenless_activity'],
  ['6k1/5pp1/b6p/4p3/1N4n1/1P2P1P1/3r1PBP/2R3K1 b - - 0 29', 'reject_queenless_second_rank_activity'],
  ['r4rk1/1pqnnp2/2p3pp/p2p1b2/N2P4/3BPN1P/PPQ2PP1/1RR3K1 b - - 7 17', 'reject_opening_development_geometry'],
]);

function readJsonl(file) {
  if (!fs.existsSync(file)) return [];
  return fs.readFileSync(file, 'utf8')
    .split(/\r?\n/)
    .filter(Boolean)
    .map(line => JSON.parse(line));
}

function writeJsonl(file, rows) {
  fs.mkdirSync(path.dirname(file), { recursive: true });
  fs.writeFileSync(file, rows.map(row => JSON.stringify(row)).join('\n') + (rows.length ? '\n' : ''), 'utf8');
}

function writeJson(file, value) {
  fs.mkdirSync(path.dirname(file), { recursive: true });
  fs.writeFileSync(file, JSON.stringify(value, null, 2) + '\n', 'utf8');
}

function writeText(file, value) {
  fs.mkdirSync(path.dirname(file), { recursive: true });
  fs.writeFileSync(file, value, 'utf8');
}

function readJson(file) {
  if (!fs.existsSync(file)) return {};
  return JSON.parse(fs.readFileSync(file, 'utf8'));
}

function countBy(values) {
  const counts = {};
  for (const value of values) counts[value] = (counts[value] || 0) + 1;
  return Object.fromEntries(Object.entries(counts).sort(([a], [b]) => a.localeCompare(b)));
}

function firstPresent(row, keys) {
  for (const key of keys) {
    if (row[key] !== undefined && row[key] !== null) return row[key];
  }
  return undefined;
}

function projectionRows(rows) {
  return rows.map(row => {
    const bands = row.admittedBands || [];
    const verdict =
      PROJECTION_EXTRA_VERDICTS.get(row.currentFen) ||
      (bands.includes('S02') ? S02_VERDICTS.get(row.currentFen) || 's02_unclassified' : 'pending_direct_audit');
    return {
      auditGroup: 'A_projection_probe',
      currentFen: row.currentFen,
      rowCount: row.rowCount,
      admittedBands: bands,
      carrierBands: row.carrierBands || [],
      sideToMove: row.sideToMove,
      fullmoveNumber: row.fullmoveNumber,
      actualStatuses: row.actualStatuses || [],
      manualVerdict: verdict,
      boundaryRisk: projectionRisk(bands, verdict),
    };
  });
}

function projectionRisk(bands, verdict) {
  if (verdict.startsWith('reject')) return 'public_claim_must_reject';
  if (verdict.startsWith('support_only')) return 'support_only_requires_certification';
  if (verdict === 'public_ready_candidate') return 'candidate_requires_exact_wording_and_certification';
  if (bands.includes('S03')) return 'diagonal_geometry_may_be_overadmitted';
  if (bands.includes('S01')) return 'storm_route_may_be_overadmitted';
  return 'pending_direct_audit';
}

function residualBucket(row) {
  const smell = row.smellSupportIds || [];
  const carriers = row.carrierFragmentIds || [];
  const colors = row.attackScaffoldColors || [];
  if (!smell.length) return 'non_smell_control';
  if (colors.length > 1) return 'dual_owner_high_risk';
  if (smell.includes('pinned_piece') && carriers.length === 1 && carriers[0] === 'diagonal_lane_only') {
    return 'pinned_diagonal_only_primary_risk';
  }
  if ((smell.includes('xray_target') || smell.includes('loose_piece')) && carriers.includes('diagonal_lane_only')) {
    return 'xray_or_loose_diagonal_binding_risk';
  }
  if (carriers.includes('rook_on_open_file_state') || carriers.includes('file_lane_state')) {
    return 'file_or_rook_carrier_control';
  }
  if (smell.includes('xray_target') && carriers.includes('diagonal_lane_only')) return 'diagonal_xray_geometry_risk';
  if (smell.includes('pinned_piece')) return 'pinned_support_needs_same_theater_audit';
  if (smell.includes('loose_piece')) return 'loose_support_needs_capture_binding';
  return 'smell_supported_general';
}

function residualRows(rows) {
  return rows.map(row => ({
    auditGroup: 'B_attack_scaffold_residual',
    currentFen: row.currentFen,
    rowCount: row.rowCount,
    rowIds: row.rowIds || [],
    sideToMove: row.sideToMove,
    fullmoveNumber: row.fullmoveNumber,
    currentStatus: row.currentStatus,
    attackScaffoldColors: row.attackScaffoldColors || [],
    supportFragmentIds: row.supportFragmentIds || [],
    carrierFragmentIds: row.carrierFragmentIds || [],
    smellSupportIds: row.smellSupportIds || [],
    auditBucket: residualBucket(row),
    ownerStratum: ownerStratum(row),
    fanoutStratum: fanoutStratum(row),
    boundaryQuestion: residualQuestion(residualBucket(row)),
  }));
}

function ownerStratum(row) {
  const colors = row.attackScaffoldColors || [];
  if (colors.length > 1) return 'black_to_move_both_scaffold_colors';
  if (colors.includes('white')) return 'black_to_move_white_scaffold';
  if (colors.includes('black')) return 'black_to_move_black_scaffold';
  return 'black_to_move_no_scaffold_color';
}

function fanoutStratum(row) {
  const ids = row.rowIds || [];
  if (ids.length > 1) return 'duplicate_fen_fanout';
  if ((row.rowCount || 0) >= 17) return 'schema_fanout_high';
  return 'schema_fanout_normal';
}

function residualQuestion(bucket) {
  switch (bucket) {
    case 'dual_owner_high_risk': return 'which side owns the public attack claim, if any';
    case 'pinned_diagonal_only_primary_risk': return 'does the pinned piece bind to the diagonal carrier line or only share a broad theater';
    case 'xray_or_loose_diagonal_binding_risk': return 'is xray/loose bound to the actual king-theater carrier or only long geometry';
    case 'file_or_rook_carrier_control': return 'does explicit file/rook carrier reduce smell-only inflation';
    case 'diagonal_xray_geometry_risk': return 'is xray bound to the actual king-theater carrier or only long geometry';
    case 'pinned_support_needs_same_theater_audit': return 'is the pinned piece a relevant defender on the same king route';
    case 'loose_support_needs_capture_binding': return 'is the loose piece immediately capturable or merely loose';
    case 'non_smell_control': return 'does non-tactical scaffold pass without smell inflation';
    default: return 'does the support fragment bind to a concrete carrier and target';
  }
}

function droppedStratum(row) {
  const move = row.fullmoveNumber || 0;
  if ((row.extractionErrors || []).length) return 'input_invalid';
  if (move <= 13) return 'opening_or_home_geometry';
  if (move <= 20) return 'early_middlegame_payload_missing';
  if (move <= 25) return 'middlegame_payload_missing';
  if (move <= 30) return 'late_middlegame_payload_missing';
  return 'late_or_unclassified_payload_missing';
}

function droppedSample(rows) {
  const buckets = new Map();
  for (const row of rows) {
    const bucket = droppedStratum(row);
    if (!buckets.has(bucket)) buckets.set(bucket, []);
    buckets.get(bucket).push(row);
  }
  const sample = [];
  for (const [bucket, bucketRows] of [...buckets.entries()].sort(([a], [b]) => a.localeCompare(b))) {
    for (const row of bucketRows.slice(0, 12)) {
      sample.push({
        auditGroup: 'C_attack_scaffold_dropped',
        currentFen: row.currentFen,
        rowCount: row.rowCount,
        rowIds: row.rowIds || [],
        sideToMove: row.sideToMove,
        fullmoveNumber: row.fullmoveNumber,
        auditBucket: 'source_payload_missing_quarantine',
        phaseStratum: bucket,
        boundaryQuestion: 'was the source label overbroad, current runtime too strict, or source payload insufficient',
      });
    }
  }
  return sample;
}

function overlapRows(dropped, residual, projection) {
  const droppedByFen = new Map(dropped.map(row => [row.currentFen, row]));
  const residualByFen = new Map(residual.map(row => [row.currentFen, row]));
  const projectionByFen = new Map(projection.map(row => [row.currentFen, row]));
  const fens = new Set([...droppedByFen.keys(), ...residualByFen.keys(), ...projectionByFen.keys()]);
  const rows = [];
  for (const fen of [...fens].sort()) {
    const memberships = [];
    if (droppedByFen.has(fen)) memberships.push('source_dropped');
    if (residualByFen.has(fen)) memberships.push('current_residual');
    if (projectionByFen.has(fen)) memberships.push('projection_probe');
    if (memberships.length < 2) continue;
    rows.push({
      auditGroup: 'D_dropped_downstream_overlap',
      currentFen: fen,
      memberships,
      risk: memberships.includes('source_dropped') && memberships.some(m => m !== 'source_dropped')
        ? 'dropped_candidate_survives_downstream'
        : 'downstream_cross_path_duplicate',
      droppedBucket: droppedByFen.has(fen) ? 'source_payload_missing_quarantine' : null,
      droppedPhaseStratum: droppedByFen.has(fen) ? droppedStratum(droppedByFen.get(fen)) : null,
      residualBucket: residualByFen.has(fen) ? residualBucket(residualByFen.get(fen)) : null,
      projectionBands: projectionByFen.has(fen) ? projectionByFen.get(fen).admittedBands || [] : [],
    });
  }
  return rows;
}

function transitionSummary() {
  const slices = readJsonl(files.serialSlices);
  const preferredOrder = new Map([
    ['source-pgn-verification', 0],
    ['pre-existing-anti-case-guard', 1],
    ['loose-touched-anchor-capture-audit', 2],
    ['loose-non-touch-created-causality-audit', 3],
    ['pinned-created-pin-geometry-audit', 5],
    ['xray-created-geometry-audit', 6],
    ['trapped-overloaded-followup', 7],
  ]);
  const rows = slices.map(slice => ({
    auditGroup: 'E_transition_created_or_followup',
    sourceOrder: slice.order,
    recommendedOrder: preferredOrder.has(slice.actionId) ? preferredOrder.get(slice.actionId) : slice.order,
    actionId: slice.actionId,
    candidateTransitions: slice.candidateTransitions,
    sourceVerifiedEligibleTransitions: slice.sourceVerifiedEligibleTransitions || 0,
    sourceBlockedTransitions: slice.sourceBlockedTransitions || 0,
    outputArtifact: slice.outputArtifact,
    completionGate: slice.completionGate || [],
    nextHandoff: slice.nextHandoff,
    boundaryRisk: transitionRisk(slice.actionId),
  }));
  rows.push({
    auditGroup: 'E_transition_created_or_followup',
    sourceOrder: null,
    recommendedOrder: 4,
    actionId: 'line-geometry-normalization-prepass',
    candidateTransitions: 578,
    sourceVerifiedEligibleTransitions: 0,
    sourceBlockedTransitions: 578,
    outputArtifact: 'line-geometry-normalization-prepass.jsonl',
    completionGate: [
      'slider identity present',
      'blocker identity present',
      'target or pinned anchor identity present',
      'same before/after line normalized for pin and xray',
    ],
    nextHandoff: 'pinned-created-pin-geometry-audit',
    boundaryRisk: 'shared pin/xray geometry must be normalized before public move-causal claims',
  });
  return rows.sort((a, b) => a.recommendedOrder - b.recommendedOrder);
}

function transitionRisk(actionId) {
  if (actionId === 'source-pgn-verification') return 'all transition claims blocked until exact source replay is verified';
  if (actionId.includes('loose')) return 'loose smell must separate immediate capture from move-causal creation';
  if (actionId.includes('pinned')) return 'pin must prove pinner pinned-piece anchor and move-created line';
  if (actionId.includes('xray')) return 'xray must reject broad line geometry without slider blocker target payload';
  if (actionId.includes('pre-existing')) return 'pre-existing facts must remain anti-cases not public move explanations';
  return 'requires dedicated root-specific audit runner';
}

function pickSamples(rows, bucketKey, perBucket) {
  const buckets = new Map();
  for (const row of rows) {
    const bucket = row[bucketKey] || 'unknown';
    if (!buckets.has(bucket)) buckets.set(bucket, []);
    buckets.get(bucket).push(row);
  }
  const result = [];
  for (const [bucket, bucketRows] of [...buckets.entries()].sort(([a], [b]) => a.localeCompare(b))) {
    result.push(...bucketRows.slice(0, perBucket));
  }
  return result;
}

const projection = projectionRows(readJsonl(files.projectionLedger));
const residual = residualRows(readJsonl(files.residualLedger));
const droppedRaw = readJsonl(files.droppedLedger);
const dropped = droppedSample(droppedRaw);
const overlap = overlapRows(droppedRaw, readJsonl(files.residualLedger), readJsonl(files.projectionLedger));
const transitions = transitionSummary();

const projectionS01S03 = projection.filter(row => row.admittedBands.some(band => band === 'S01' || band === 'S03'));
const projectionS01S03Fens = projectionS01S03.map(row => row.currentFen);
const residualSamples = pickSamples(residual, 'auditBucket', 8);
const highRiskSummary = readJson(files.highRiskSummary);

const summary = {
  projection: {
    uniqueFen: projection.length,
    byBand: countBy(projection.flatMap(row => row.admittedBands)),
    byManualVerdict: countBy(projection.map(row => row.manualVerdict)),
    s01s03UniqueFen: projectionS01S03.length,
    s01s03PendingDirectAuditUniqueFen: projectionS01S03.filter(row => row.manualVerdict === 'pending_direct_audit').length,
  },
  residual: {
    uniqueFen: residual.length,
    smellSupportedUniqueFen: residual.filter(row => row.currentStatus === 'current_residual_smell_supported').length,
    byBucket: countBy(residual.map(row => row.auditBucket)),
    byOwnerStratum: countBy(residual.map(row => row.ownerStratum)),
    byFanoutStratum: countBy(residual.map(row => row.fanoutStratum)),
    sampleRows: residualSamples.length,
  },
  dropped: {
    uniqueFen: droppedRaw.length,
    sampleRows: dropped.length,
    sampleByBucket: countBy(dropped.map(row => row.auditBucket)),
    sampleByPhaseStratum: countBy(dropped.map(row => row.phaseStratum)),
  },
  overlap: {
    uniqueFen: overlap.length,
    byRisk: countBy(overlap.map(row => row.risk)),
  },
  transition: {
    slices: transitions.length,
    totalCandidateTransitions: transitions.reduce((sum, row) => sum + (row.candidateTransitions || 0), 0),
    totalSourceVerifiedEligibleTransitions: transitions.reduce((sum, row) => sum + (row.sourceVerifiedEligibleTransitions || 0), 0),
    totalSourceBlockedTransitions: transitions.reduce((sum, row) => sum + (row.sourceBlockedTransitions || 0), 0),
    byAction: Object.fromEntries(transitions.map(row => [row.actionId, row.candidateTransitions])),
    sourceVerifiedEligibleByAction: Object.fromEntries(transitions.map(row => [row.actionId, row.sourceVerifiedEligibleTransitions])),
    sourceBlockedByAction: Object.fromEntries(transitions.map(row => [row.actionId, row.sourceBlockedTransitions])),
    highRiskUniqueTransitions: highRiskSummary.uniqueTransitions,
    highRiskUniqueTransitionKeys: highRiskSummary.uniqueTransitionKeys,
    highRiskMultiSchemaTransitionKeys: highRiskSummary.multiSchemaTransitionKeys,
    highRiskRowAmplification: highRiskSummary.rowAmplification,
    highRiskSourceStates: highRiskSummary.countsBySourceVerificationState,
    highRiskCountsByDisposition: highRiskSummary.countsByAuditDisposition,
  },
};

const report = [
  '# Commentary False-Positive Audit Bundle',
  '',
  'This bundle is diagnostic-only. It converts the A-G backlog into exact-board or exact-transition ledgers, not public claim acceptance.',
  '',
  '## A. Projection Probe',
  `- Unique FEN: ${summary.projection.uniqueFen}`,
  `- Manual/provisional verdicts: ${JSON.stringify(summary.projection.byManualVerdict)}`,
  `- S01/S03 unique FEN: ${summary.projection.s01s03UniqueFen}`,
  `- S01/S03 still pending direct audit: ${summary.projection.s01s03PendingDirectAuditUniqueFen}`,
  '',
  '## B. AttackScaffold Current Residual',
  `- Unique FEN: ${summary.residual.uniqueFen}`,
  `- Smell-supported unique FEN: ${summary.residual.smellSupportedUniqueFen}`,
  `- Buckets: ${JSON.stringify(summary.residual.byBucket)}`,
  `- Owner strata: ${JSON.stringify(summary.residual.byOwnerStratum)}`,
  `- Fanout strata: ${JSON.stringify(summary.residual.byFanoutStratum)}`,
  '',
  '## C. Source AttackScaffold Dropped',
  `- Unique FEN: ${summary.dropped.uniqueFen}`,
  `- Stratified sample rows: ${summary.dropped.sampleRows}`,
  `- Sample buckets: ${JSON.stringify(summary.dropped.sampleByBucket)}`,
  `- Phase strata: ${JSON.stringify(summary.dropped.sampleByPhaseStratum)}`,
  '',
  '## D. Dropped/Downstream Overlap',
  `- Unique FEN with cross-membership: ${summary.overlap.uniqueFen}`,
  `- Risks: ${JSON.stringify(summary.overlap.byRisk)}`,
  '',
  '## E-G. Transition And Tactical Follow-Up',
  `- Serial slices: ${summary.transition.slices}`,
  `- Total candidate transitions across slices: ${summary.transition.totalCandidateTransitions}`,
  `- Source-verified eligible transitions across slices: ${summary.transition.totalSourceVerifiedEligibleTransitions}`,
  `- Source-blocked transitions across slices: ${summary.transition.totalSourceBlockedTransitions}`,
  `- Counts by action: ${JSON.stringify(summary.transition.byAction)}`,
  `- High-risk schema-transition audit rows: ${summary.transition.highRiskUniqueTransitions}`,
  `- High-risk unique transition keys: ${summary.transition.highRiskUniqueTransitionKeys}`,
  `- High-risk multi-schema transition keys: ${summary.transition.highRiskMultiSchemaTransitionKeys}`,
  `- High-risk source states: ${JSON.stringify(summary.transition.highRiskSourceStates)}`,
  `- High-risk row amplification: ${summary.transition.highRiskRowAmplification}`,
  '',
  '## Boundary Direction',
  '- Projection: geometry-only and support-only rows must not become public Sxx claims without same-target route binding plus certification.',
  '- AttackScaffold residual: smell support must prove same-theater carrier binding, not just xray/pin/loose presence.',
  '- Dropped rows: current-contract drop is not false-positive proof until source payload is reconciled.',
  '- Transition-created rows: all move-causal tactical claims stay blocked until source PGN replay and before/current identity are verified.',
  '',
].join('\n');

fs.mkdirSync(outDir, { recursive: true });
writeJson(path.join(outDir, 'audit-bundle-summary.json'), summary);
writeJsonl(path.join(outDir, 'A-projection-probe-ledger.jsonl'), projection);
writeJsonl(path.join(outDir, 'A-projection-s01-s03-direct-audit-input.jsonl'), projectionS01S03);
writeJson(path.join(outDir, 'A-projection-s01-s03-stockfish-input.json'), { fens: projectionS01S03Fens, depth: 12, multiPv: 3 });
writeJsonl(path.join(outDir, 'B-attack-scaffold-residual-buckets.jsonl'), residual);
writeJsonl(path.join(outDir, 'B-attack-scaffold-residual-stratified-sample.jsonl'), residualSamples);
writeJsonl(path.join(outDir, 'C-attack-scaffold-dropped-stratified-sample.jsonl'), dropped);
writeJsonl(path.join(outDir, 'D-dropped-downstream-overlap-ledger.jsonl'), overlap);
writeJsonl(path.join(outDir, 'E-transition-created-backlog-summary.jsonl'), transitions);
writeText(path.join(outDir, 'audit-bundle-report.md'), report);

console.log(JSON.stringify(summary, null, 2));

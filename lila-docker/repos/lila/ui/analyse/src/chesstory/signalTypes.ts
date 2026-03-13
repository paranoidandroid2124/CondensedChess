import type { DecisionComparisonDigestLike } from '../decisionComparison';

export type StrategicIdeaKind =
  | 'pawn_break'
  | 'space_gain_or_restriction'
  | 'target_fixing'
  | 'line_occupation'
  | 'outpost_creation_or_occupation'
  | 'minor_piece_imbalance_exploitation'
  | 'prophylaxis'
  | 'king_attack_build_up'
  | 'favorable_trade_or_transformation'
  | 'counterplay_suppression';

export type StrategicIdeaGroup =
  | 'structural_change'
  | 'piece_and_line_management'
  | 'interaction_and_transformation';

export type DecisionComparisonDigest = DecisionComparisonDigestLike;

export type NarrativeSignalDigest = {
  opening?: string;
  strategicStack?: string[];
  latentPlan?: string;
  latentReason?: string;
  decisionComparison?: DecisionComparisonDigest;
  authoringEvidence?: string;
  practicalVerdict?: string;
  practicalFactors?: string[];
  compensation?: string;
  compensationVectors?: string[];
  investedMaterial?: number;
  structuralCue?: string;
  structureProfile?: string;
  centerState?: string;
  alignmentBand?: string;
  alignmentReasons?: string[];
  deploymentOwnerSide?: string;
  deploymentPiece?: string;
  deploymentRoute?: string[];
  deploymentPurpose?: string;
  deploymentContribution?: string;
  deploymentStrategicFit?: number;
  deploymentTacticalSafety?: number;
  deploymentSurfaceConfidence?: number;
  deploymentSurfaceMode?: 'exact' | 'toward' | 'hidden';
  prophylaxisPlan?: string;
  prophylaxisThreat?: string;
  counterplayScoreDrop?: number;
  dominantIdeaKind?: StrategicIdeaKind;
  dominantIdeaGroup?: StrategicIdeaGroup;
  dominantIdeaReadiness?: 'ready' | 'build' | 'premature' | 'blocked';
  dominantIdeaFocus?: string;
  secondaryIdeaKind?: StrategicIdeaKind;
  secondaryIdeaGroup?: StrategicIdeaGroup;
  secondaryIdeaFocus?: string;
  decision?: string;
  strategicFlow?: string;
  opponentPlan?: string;
  preservedSignals?: string[];
};

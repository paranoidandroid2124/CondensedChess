package lila.llm.analysis

import chess.File
import lila.llm.model.PlanId
import lila.llm.model.authoring.*

/**
 * Seed Library (SSOT, code-side).
 *
 * This is intentionally "book-biased": it enumerates idea-space items that authors
 * routinely mention even when they do not appear in PV/MultiPV.
 *
 * Validation is delegated to:
 * - LatentPlanSeeder (static → position-specific LatentPlanInfo)
 * - ProbeDetector (free-tempo/legal branches + refutation)
 * - BookStyleRenderer (conditional rendering rules)
 */
object LatentSeedLibrary:

  val all: List[LatentSeed] =
    List(
      // ==========================================================
      // 1) Pawn Levers & Storms
      // ==========================================================
      LatentSeed(
        id = "PawnStorm_Kingside",
        family = SeedFamily.Pawn,
        mapsToPlan = Some(PlanId.PawnStorm),
        candidateMoves = List(
          MovePattern.PawnAdvance(File.G),
          MovePattern.PawnAdvance(File.H),
          MovePattern.PawnAdvance(File.F)
        ),
        preconditions = List(
          WeightedPrecondition(Precondition.KingPosition(SideRef.Them, Flank.Kingside), required = true, weight = 2.0),
          WeightedPrecondition(Precondition.CenterStateIs(CenterState.Locked), required = false, weight = 1.3),
          WeightedPrecondition(Precondition.SpaceAdvantage(Flank.Kingside, min = 1), required = false, weight = 1.0),
          WeightedPrecondition(Precondition.NoImmediateDefensiveTask(maxThreatCp = 150), required = true, weight = 2.0)
        ),
        typicalCounters = List(
          CounterPattern.PawnPushBlock(File.H),
          CounterPattern.CentralStrike
        ),
        evidencePolicy = EvidencePolicy(
          useTempoInjection = true,
          freeTempoVerify = Some(FreeTempoVerify(minBranches = 2, maxBranches = 4, seedMustAppearWithinPlies = 1)),
          immediateViability = Some(ImmediateViability(maxMoverLossCp = 70)),
          refutationCheck = Some(RefutationCheck(counters = List(CounterPattern.PawnPushBlock(File.H), CounterPattern.CentralStrike)))
        ),
        narrative = NarrativeTemplate(
          template =
            "If {them} is slow and does not challenge the position, {us} can start a kingside pawn storm with {seed}, aiming to open lines against the king."
        )
      ),
      LatentSeed(
        id = "Attack_The_Hook",
        family = SeedFamily.Pawn,
        mapsToPlan = Some(PlanId.KingsideAttack),
        candidateMoves = Nil, // dynamic via SeedMoveGenerator
        preconditions = List(
          WeightedPrecondition(Precondition.PawnStructureIs(StructureType.FianchettoShell), required = false, weight = 1.0), // Helpful but not strictly required if hook exists otherwise
          WeightedPrecondition(Precondition.NoImmediateDefensiveTask(maxThreatCp = 150), required = true, weight = 1.5)
        ),
        evidencePolicy = EvidencePolicy(
          useTempoInjection = true,
          freeTempoVerify = Some(FreeTempoVerify(minBranches = 2)),
          immediateViability = Some(ImmediateViability(maxMoverLossCp = 100))
        ),
        narrative = NarrativeTemplate(
          template =
            "With a 'hook' present in the pawn structure, {us} can launch a direct attack ({seed}) to tear open the kingside."
        )
      ),
      LatentSeed(
        id = "MinorityAttack_Queenside",
        family = SeedFamily.Pawn,
        mapsToPlan = Some(PlanId.MinorityAttack),
        candidateMoves = List(MovePattern.PawnAdvance(File.B)),
        preconditions = List(
          WeightedPrecondition(Precondition.PawnStructureIs(StructureType.CarlsbadLike), required = true, weight = 2.0),
          WeightedPrecondition(Precondition.NoImmediateDefensiveTask(maxThreatCp = 150), required = true, weight = 1.5)
        ),
        typicalCounters = List(CounterPattern.Counterplay(Flank.Kingside), CounterPattern.CentralStrike),
        narrative = NarrativeTemplate(
          template =
            "With a Carlsbad-like structure, {us} can consider a queenside minority attack (often starting with {seed}) to create a long-term weakness."
        )
      ),
      LatentSeed(
        id = "CanOpener_h_Pawn",
        family = SeedFamily.Pawn,
        mapsToPlan = Some(PlanId.KingsideAttack),
        candidateMoves = List(MovePattern.PawnAdvance(File.H)),
        preconditions = List(
          WeightedPrecondition(Precondition.PawnStructureIs(StructureType.FianchettoShell), required = true, weight = 2.0),
          WeightedPrecondition(Precondition.KingPosition(SideRef.Them, Flank.Kingside), required = true, weight = 2.0),
          WeightedPrecondition(Precondition.NoImmediateDefensiveTask(maxThreatCp = 150), required = true, weight = 1.5)
        ),
        typicalCounters = List(CounterPattern.PawnPushBlock(File.H), CounterPattern.CentralStrike),
        narrative = NarrativeTemplate(
          template =
            "Against a fianchetto shell, {us} may use the h-pawn as a 'can opener' ({seed}) to attack the hook and open lines in front of the king."
        )
      ),
      LatentSeed(
        id = "CentralBreak_D",
        family = SeedFamily.Pawn,
        mapsToPlan = Some(PlanId.PawnBreakPreparation),
        candidateMoves = List(MovePattern.PawnAdvance(File.D)),
        preconditions = List(
          WeightedPrecondition(Precondition.CenterStateIs(CenterState.Fluid), required = true, weight = 1.5),
          WeightedPrecondition(Precondition.NoImmediateDefensiveTask(maxThreatCp = 150), required = true, weight = 1.0)
        ),
        typicalCounters = List(CounterPattern.CentralStrike),
        narrative = NarrativeTemplate(
          template =
            "A central break with {seed} is a thematic way to seize space and open lines, provided the pieces are ready to support it."
        )
      ),
      LatentSeed(
        id = "CentralBreak_E",
        family = SeedFamily.Pawn,
        mapsToPlan = Some(PlanId.PawnBreakPreparation),
        candidateMoves = List(MovePattern.PawnAdvance(File.E)),
        preconditions = List(
          WeightedPrecondition(Precondition.CenterStateIs(CenterState.Fluid), required = true, weight = 1.5),
          WeightedPrecondition(Precondition.NoImmediateDefensiveTask(maxThreatCp = 150), required = true, weight = 1.0)
        ),
        typicalCounters = List(CounterPattern.CentralStrike),
        narrative = NarrativeTemplate(
          template =
            "A central break with {seed} is often the most direct way to change the character of the position and activate the pieces."
        )
      ),

      // ==========================================================
      // 2) Piece Maneuvers & Optimization
      // ==========================================================
      LatentSeed(
        id = "KnightOutpost_Route",
        family = SeedFamily.Piece,
        mapsToPlan = Some(PlanId.MinorPieceManeuver),
        candidateMoves = Nil, // position-specific (computed by seeder)
        preconditions = List(
          WeightedPrecondition(Precondition.NoImmediateDefensiveTask(maxThreatCp = 150), required = true, weight = 1.2)
        ),
        narrative = NarrativeTemplate(
          template =
            "{us} can improve a knight toward a stable outpost ({seed}), aiming for long-term domination on key squares."
        )
      ),
      LatentSeed(
        id = "RookLift_Kingside",
        family = SeedFamily.Piece,
        mapsToPlan = Some(PlanId.RookActivation),
        candidateMoves = Nil, // position-specific (computed by seeder)
        preconditions = List(
          WeightedPrecondition(Precondition.KingPosition(SideRef.Them, Flank.Kingside), required = false, weight = 1.2),
          WeightedPrecondition(Precondition.CenterStateIs(CenterState.Locked), required = false, weight = 1.2)
        ),
        narrative = NarrativeTemplate(
          template =
            "A rook lift ({seed}) is a practical way to bring heavy pieces into the attack when the first rank is congested."
        )
      ),
      LatentSeed(
        id = "BadBishop_Reroute",
        family = SeedFamily.Piece,
        mapsToPlan = Some(PlanId.PieceActivation),
        candidateMoves = Nil, // position-specific (computed by seeder)
        narrative = NarrativeTemplate(
          template =
            "{us} may need to reroute a passive bishop ({seed}) to a more active diagonal to avoid being stuck behind the pawn chain."
        )
      ),
      LatentSeed(
        id = "Battery_Formation",
        family = SeedFamily.Piece,
        mapsToPlan = None,
        candidateMoves = Nil, // position-specific (computed by seeder)
        narrative = NarrativeTemplate(
          template =
            "{us} can consider forming a battery ({seed}) to concentrate force on a critical file or diagonal before breaking through."
        )
      ),

      // ==========================================================
      // 3) Structural Transformation
      // ==========================================================
      LatentSeed(
        id = "CreatePassedPawn",
        family = SeedFamily.Structure,
        mapsToPlan = Some(PlanId.PassedPawnCreation),
        candidateMoves = Nil,
        narrative = NarrativeTemplate(
          template =
            "{us} can aim to create a passed pawn ({seed}) by using the majority and forcing favorable exchanges."
        )
      ),
      LatentSeed(
        id = "CreateIQP",
        family = SeedFamily.Structure,
        mapsToPlan = Some(PlanId.WeakPawnAttack),
        candidateMoves = Nil,
        narrative = NarrativeTemplate(
          template =
            "{us} may try to induce an isolated pawn ({seed}) and then blockade and attack it as a long-term weakness."
        )
      ),
      LatentSeed(
        id = "FixBackwardPawn",
        family = SeedFamily.Structure,
        mapsToPlan = Some(PlanId.WeakPawnAttack),
        candidateMoves = Nil,
        narrative = NarrativeTemplate(
          template =
            "A common technical plan is to fix a backward pawn ({seed}) so it cannot advance, turning it into a stable target."
        )
      ),

      // ==========================================================
      // 4) Prophylaxis & Safety
      // ==========================================================
      LatentSeed(
        id = "Prophylaxis_Luft",
        family = SeedFamily.Prophylaxis,
        mapsToPlan = Some(PlanId.Prophylaxis),
        candidateMoves = List(MovePattern.PawnAdvance(File.H), MovePattern.PawnAdvance(File.G)),
        preconditions = List(
          WeightedPrecondition(Precondition.NoImmediateDefensiveTask(maxThreatCp = 250), required = false, weight = 0.8)
        ),
        narrative = NarrativeTemplate(
          template =
            "{us} may want to create luft with {seed}, avoiding back-rank accidents before starting more active operations."
        )
      ),
      LatentSeed(
        id = "Restrict_OpponentPiece",
        family = SeedFamily.Prophylaxis,
        mapsToPlan = Some(PlanId.Prophylaxis),
        candidateMoves = Nil,
        narrative = NarrativeTemplate(
          template =
            "{us} can play prophylactically ({seed}) to restrict the opponent’s most active piece and cut down counterplay."
        )
      ),
      LatentSeed(
        id = "KingSafety_Run",
        family = SeedFamily.Prophylaxis,
        mapsToPlan = Some(PlanId.DefensiveConsolidation),
        candidateMoves = Nil,
        narrative = NarrativeTemplate(
          template =
            "If the center is about to open, {us} may need a king evacuation ({seed}) to avoid being caught in the crossfire."
        )
      ),

      // ==========================================================
      // 5) Exchange Decisions
      // ==========================================================
      LatentSeed(
        id = "Trade_BadBishop",
        family = SeedFamily.Exchange,
        mapsToPlan = Some(PlanId.Exchange),
        candidateMoves = Nil,
        narrative = NarrativeTemplate(
          template =
            "{us} can consider exchanging a bad piece ({seed}) to improve the long-term structure and reduce defensive tasks."
        )
      ),
      LatentSeed(
        id = "Trade_Queens_Defensive",
        family = SeedFamily.Exchange,
        mapsToPlan = Some(PlanId.Simplification),
        candidateMoves = Nil, // dynamic
        preconditions = List(
          // Logic: Us under pressure?
           WeightedPrecondition(Precondition.NoImmediateDefensiveTask(maxThreatCp = 300), required = true, weight = 1.0)
        ),
        evidencePolicy = EvidencePolicy(
           useTempoInjection = true,
           // Must be viable immediately as a defense
           immediateViability = Some(ImmediateViability(maxMoverLossCp = 60))
        ),
        narrative = NarrativeTemplate(
          template =
            "Facing an aggressive setup, {us} can offer a queen trade ({seed}) to neutralize the attack and secure the king."
        )
      ),
      LatentSeed(
        id = "Simplify_To_Endgame",
        family = SeedFamily.Exchange,
        mapsToPlan = Some(PlanId.Simplification),
        candidateMoves = Nil,
        narrative = NarrativeTemplate(
          template =
            "With a stable advantage, {us} can simplify ({seed}) to reduce counterplay and reach a technically winning endgame."
        )
      ),

      // ==========================================================
      // 6) Dynamic & Tactical Preparation
      // ==========================================================
      LatentSeed(
        id = "Prepare_Overload",
        family = SeedFamily.TacticalPrep,
        mapsToPlan = None,
        candidateMoves = Nil,
        narrative = NarrativeTemplate(
          template =
            "{us} can increase the pressure ({seed}) to overload a key defender before the decisive break."
        )
      ),
      LatentSeed(
        id = "Prepare_Clearance",
        family = SeedFamily.TacticalPrep,
        mapsToPlan = None,
        candidateMoves = Nil,
        narrative = NarrativeTemplate(
          template =
            "{us} can prepare a clearance idea ({seed}) to open lines or squares for a piece to jump into the attack."
        )
      )
    )

  def byId(id: String): Option[LatentSeed] =
    all.find(_.id == id)


package lila.llm.analysis

import _root_.chess.{ Bishop, Board, Color, File, Knight, Pawn, Queen, Rank, Role, Rook, Square }

import lila.llm.analysis.L3.{ ThreatAnalysis, ThreatKind }
import lila.llm.*
import lila.llm.model.{ Motif, PlanId, PlanMatch }
import lila.llm.model.strategic.{ PositionalTag, PreventedPlan, TheoreticalOutcomeHint, WeakComplex }
import lila.llm.model.structure.{ CenterState, StructureId }

private[llm] object StrategicIdeaSelector:

  private final case class StrategicIdeaEvidence(
      ownerSide: String,
      kind: String,
      group: String,
      readiness: String,
      source: String,
      confidence: Double,
      focusSquares: List[String] = Nil,
      focusFiles: List[String] = Nil,
      focusDiagonals: List[String] = Nil,
      focusZone: Option[String] = None,
      beneficiaryPieces: List[String] = Nil,
      factIds: List[String] = Nil
  ):
    def signature: String = s"$ownerSide|$kind"

  private final case class Candidate(
      ownerSide: String,
      kind: String,
      group: String,
      readiness: String,
      focusSquares: List[String] = Nil,
      focusFiles: List[String] = Nil,
      focusDiagonals: List[String] = Nil,
      focusZone: Option[String] = None,
      beneficiaryPieces: List[String] = Nil,
      score: Double,
      evidenceRefs: List[String] = Nil
  )

  def enrich(pack: StrategyPack): StrategyPack =
    enrich(pack, StrategicIdeaSemanticContext.empty(pack.sideToMove))

  def enrich(pack: StrategyPack, semantic: StrategicIdeaSemanticContext): StrategyPack =
    val ideas = select(pack, semantic)
    if ideas.isEmpty then pack
    else
      val enrichedDigest = enrichDigest(pack.signalDigest, ideas)
      val enrichedFocus = enrichLongTermFocus(pack.longTermFocus, ideas, pack.directionalTargets)
      val enrichedEvidence = enrichEvidence(pack.evidence, ideas, pack.directionalTargets)
      pack.copy(
        strategicIdeas = ideas,
        longTermFocus = enrichedFocus,
        evidence = enrichedEvidence,
        signalDigest = enrichedDigest
      )

  def select(pack: StrategyPack): List[StrategyIdeaSignal] =
    select(pack, StrategicIdeaSemanticContext.empty(pack.sideToMove))

  def select(pack: StrategyPack, semantic: StrategicIdeaSemanticContext): List[StrategyIdeaSignal] =
    val evidence = collectTypedEvidence(pack, semantic)
    if evidence.isEmpty then Nil
    else
      val merged = mergeEvidence(evidence)
      val dominant = merged.head
      val secondary =
        merged.drop(1).find(candidate =>
          candidate.group != dominant.group &&
            math.abs(dominant.score - candidate.score) <= 0.12
        )
      List(Some(dominant), secondary).flatten.zipWithIndex.map { case (candidate, idx) =>
        StrategyIdeaSignal(
          ideaId = s"idea_${idx + 1}",
          ownerSide = candidate.ownerSide,
          kind = candidate.kind,
          group = candidate.group,
          readiness = candidate.readiness,
          focusSquares = candidate.focusSquares,
          focusFiles = candidate.focusFiles,
          focusDiagonals = candidate.focusDiagonals,
          focusZone = candidate.focusZone,
          beneficiaryPieces = candidate.beneficiaryPieces.distinct,
          confidence = candidate.score.min(0.98),
          evidenceRefs = candidate.evidenceRefs.distinct.take(6)
        )
      }

  def humanizedKind(kind: String): String =
    kind match
      case StrategicIdeaKind.PawnBreak                       => "pawn break"
      case StrategicIdeaKind.SpaceGainOrRestriction          => "space gain or restriction"
      case StrategicIdeaKind.TargetFixing                    => "target fixing"
      case StrategicIdeaKind.LineOccupation                  => "line occupation"
      case StrategicIdeaKind.OutpostCreationOrOccupation     => "outpost creation or occupation"
      case StrategicIdeaKind.MinorPieceImbalanceExploitation => "minor-piece imbalance exploitation"
      case StrategicIdeaKind.Prophylaxis                     => "prophylaxis"
      case StrategicIdeaKind.KingAttackBuildUp               => "king-attack build-up"
      case StrategicIdeaKind.FavorableTradeOrTransformation  => "favorable trade or transformation"
      case StrategicIdeaKind.CounterplaySuppression          => "counterplay suppression"
      case other                                             => other.replace('_', ' ')

  def focusSummary(signal: StrategyIdeaSignal): String =
    focusSummary(
      focusSquares = signal.focusSquares,
      focusFiles = signal.focusFiles,
      focusDiagonals = signal.focusDiagonals,
      focusZone = signal.focusZone
    )

  private def collectTypedEvidence(
      pack: StrategyPack,
      semantic: StrategicIdeaSemanticContext
  ): List[StrategicIdeaEvidence] =
    // Runtime selection is limited to the typed sources registered in
    // StrategicIdeaSourceRegistry.authoritative / derivedTyped.
    val side = pack.sideToMove
    (
      collectPawnBreakEvidence(side, semantic) ++
        collectSpaceEvidence(side, semantic) ++
        collectTargetFixingEvidence(side, pack, semantic) ++
        collectLineOccupationEvidence(side, pack, semantic) ++
        collectOutpostEvidence(side, pack, semantic) ++
        collectMinorPieceImbalanceEvidence(side, semantic) ++
        collectProphylaxisEvidence(side, semantic) ++
        collectKingAttackEvidence(side, pack, semantic) ++
        collectFavorableTradeEvidence(side, pack, semantic) ++
        collectCounterplaySuppressionEvidence(side, semantic)
    ).sortBy(ev => (-ev.confidence, ev.kind, ev.source))

  private def collectPawnBreakEvidence(
      side: String,
      semantic: StrategicIdeaSemanticContext
  ): List[StrategicIdeaEvidence] =
    val analysisBreakReady =
      semantic.pawnAnalysis.toList.flatMap { analysis =>
        val file = analysis.breakFile.flatMap(normalizeFileToken)
        val focusSquares = normalizeSquareKeys(analysis.tensionSquares)
        Option.when(analysis.pawnBreakReady && (file.nonEmpty || focusSquares.nonEmpty)) {
          evidence(
            ownerSide = side,
            kind = StrategicIdeaKind.PawnBreak,
            readiness = StrategicIdeaReadiness.Ready,
            source = "pawn_analysis_break_ready",
            confidence = 0.84 + breakImpactBonusFromInt(analysis.breakImpact),
            focusSquares = focusSquares.take(3),
            focusFiles = file.toList,
            focusZone = file.flatMap(zoneFromFileToken).orElse(zoneFromSquareKeys(focusSquares)),
            factIds =
              List("pawn_analysis_break_ready") ++
                Option.when(analysis.advanceOrCapture)("advance_or_capture").toList ++
                Option.when(analysis.counterBreak)("counter_break_race").toList ++
                Option.when(focusSquares.nonEmpty)("tension_squares").toList
          )
        }
      }

    val analysisTension =
      semantic.pawnAnalysis.toList.flatMap { analysis =>
        val focusSquares = normalizeSquareKeys(analysis.tensionSquares)
        Option.when(analysis.advanceOrCapture && focusSquares.nonEmpty) {
          evidence(
            ownerSide = side,
            kind = StrategicIdeaKind.PawnBreak,
            readiness = if analysis.pawnBreakReady then StrategicIdeaReadiness.Ready else StrategicIdeaReadiness.Build,
            source = "pawn_analysis_tension",
            confidence = 0.78 + Option.when(semantic.structureProfile.exists(_.centerState == CenterState.Locked))(0.02)
              .getOrElse(0.0),
            focusSquares = focusSquares.take(3),
            focusZone = zoneFromSquareKeys(focusSquares),
            factIds =
              List("pawn_analysis_tension", "advance_or_capture") ++
                Option.when(analysis.counterBreak)("counter_break_race").toList
          )
        }
      }

    val analysisBreakRace =
      semantic.pawnAnalysis.toList.flatMap { analysis =>
        val file = analysis.breakFile.flatMap(normalizeFileToken)
        Option.when(analysis.counterBreak && file.nonEmpty) {
          evidence(
            ownerSide = side,
            kind = StrategicIdeaKind.PawnBreak,
            readiness = if analysis.pawnBreakReady then StrategicIdeaReadiness.Build else StrategicIdeaReadiness.Premature,
            source = "pawn_analysis_break_race",
            confidence = 0.72,
            focusFiles = file.toList,
            focusZone = file.flatMap(zoneFromFileToken),
            factIds = List("counter_break_race") ++ file.toList.map(v => s"break_file_$v")
          )
        }
      }

    val base =
      semantic.pawnPlay.toList.flatMap { pawnPlay =>
        Option.when(pawnPlay.breakReady && pawnPlay.breakFile.exists(_.trim.nonEmpty) && pawnPlay.breakImpact != "Low") {
          val file = normalizeFileToken(pawnPlay.breakFile.get)
          evidence(
            ownerSide = side,
            kind = StrategicIdeaKind.PawnBreak,
            readiness = StrategicIdeaReadiness.Ready,
            source = "pawn_play_break_ready",
            confidence = 0.84 + breakImpactBonus(pawnPlay.breakImpact),
            focusFiles = file.toList,
            focusZone = file.flatMap(zoneFromFileToken),
            factIds = List("pawn_play_break_ready") ++ file.toList.map(v => s"break_file_$v")
          )
        }
      }

    val tension =
      for
        pawnPlay <- semantic.pawnPlay.toList
        file <- pawnPlay.breakFile.flatMap(normalizeFileToken).toList
        features <- semantic.positionFeatures.toList
        if features.centralSpace.pawnTensionCount > 0 || features.centralSpace.lockedCenter
      yield
        evidence(
          ownerSide = side,
          kind = StrategicIdeaKind.PawnBreak,
          readiness = if pawnPlay.breakReady then StrategicIdeaReadiness.Ready else StrategicIdeaReadiness.Build,
          source = "central_break_tension",
          confidence = 0.74 + (if features.centralSpace.lockedCenter then 0.04 else 0.0),
          focusFiles = List(file),
          focusZone = Some("center"),
          factIds =
            List("central_break_tension") ++
              Option.when(features.centralSpace.lockedCenter)("locked_center").toList
        )

    val fileOpening =
      for
        pawnPlay <- semantic.pawnPlay.toList
        file <- pawnPlay.breakFile.flatMap(normalizeFileToken).flatMap(fileFromToken).toList
        board <- semantic.board.toList
        if fileHasBothColorsPawns(board, file)
      yield
        evidence(
          ownerSide = side,
          kind = StrategicIdeaKind.PawnBreak,
          readiness = if pawnPlay.breakReady then StrategicIdeaReadiness.Ready else StrategicIdeaReadiness.Build,
          source = "file_opening_consequence",
          confidence = 0.72,
          focusFiles = List(fileToken(file)),
          focusZone = zoneFromFileToken(fileToken(file)),
          factIds = List("file_opening_consequence", s"contested_file_${fileToken(file)}")
        )

    val planBridge =
      topPlansFor(side, semantic).flatMap { plan =>
        Option.when(
          plan.plan.id == PlanId.PawnBreakPreparation || plan.plan.id == PlanId.CentralBreakthrough
        ) {
          evidence(
            ownerSide = side,
            kind = StrategicIdeaKind.PawnBreak,
            readiness = StrategicIdeaReadiness.Build,
            source = "plan_match_break_preparation",
            confidence = 0.76 + math.min(0.08, plan.score * 0.10),
            focusFiles =
              semantic.pawnAnalysis.flatMap(_.breakFile.flatMap(normalizeFileToken)).toList ++
                semantic.pawnPlay.flatMap(_.breakFile.flatMap(normalizeFileToken)).toList,
            factIds = List("plan_match_break_preparation", s"plan_${plan.plan.id.toString.toLowerCase}")
          )
        }
      }

    val frenchCounterBreak =
      Option.when(structureIs(semantic, StructureId.FrenchAdvanceChain) && side == "black") {
        evidence(
          ownerSide = side,
          kind = StrategicIdeaKind.PawnBreak,
          readiness = StrategicIdeaReadiness.Build,
          source = "french_counterbreak_profile",
          confidence = 0.88,
          focusFiles = List("f"),
          focusZone = Some("center"),
          factIds = List("structure_french_advance_chain", "french_counterbreak_profile", "french_f6_break")
        )
      }.toList

    val frenchF6Break =
      Option.when(
        structureIs(semantic, StructureId.FrenchAdvanceChain) &&
          side == "black" &&
          semantic.board.exists(board =>
            pawnAt(board, Color.Black, Square.F7) &&
              pawnAt(board, Color.Black, Square.E6) &&
              pawnAt(board, Color.Black, Square.D5) &&
              pawnAt(board, Color.White, Square.E5)
          )
      ) {
        evidence(
          ownerSide = side,
          kind = StrategicIdeaKind.PawnBreak,
          readiness = StrategicIdeaReadiness.Build,
          source = "french_f6_break_seed",
          confidence = 0.92,
          focusFiles = List("f"),
          focusSquares = List("e5", "f6"),
          focusZone = Some("center"),
          factIds = List("french_f6_break_seed", "white_e5_chain", "black_f7_break_pawn")
        )
      }.toList

    analysisBreakReady ++ analysisTension ++ analysisBreakRace ++ base ++ tension ++ fileOpening ++ planBridge ++
      frenchCounterBreak ++ frenchF6Break

  private def collectSpaceEvidence(
      side: String,
      semantic: StrategicIdeaSemanticContext
  ): List[StrategicIdeaEvidence] =
    val tagEvidence =
      semantic.positionalFeatures.collect {
        case PositionalTag.SpaceAdvantage(color) if matchesSide(color, side) =>
          evidence(
            ownerSide = side,
            kind = StrategicIdeaKind.SpaceGainOrRestriction,
            readiness = StrategicIdeaReadiness.Ready,
            source = "space_advantage_tag",
            confidence = 0.84,
            focusZone = Some("center"),
            factIds = List("tag_space_advantage")
          )
      }

    val clampEvidence =
      semantic.strategicState
        .filter(colorComplexClampFor(side, _))
        .map { _ =>
          evidence(
            ownerSide = side,
            kind = StrategicIdeaKind.SpaceGainOrRestriction,
            readiness = StrategicIdeaReadiness.Build,
            source = "color_complex_clamp",
            confidence = 0.80,
            focusZone = Some("center"),
            factIds = List("state_color_complex_clamp")
          )
        }
        .toList

    val centralSpaceEvidence =
      semantic.positionFeatures
        .flatMap { features =>
          Option.when(spaceDiffFor(side, features) >= 2) {
            evidence(
              ownerSide = side,
              kind = StrategicIdeaKind.SpaceGainOrRestriction,
              readiness = StrategicIdeaReadiness.Ready,
              source = "central_space_edge",
              confidence = 0.74 + math.min(0.08, (spaceDiffFor(side, features) - 2) * 0.02),
              focusZone = Some("center"),
              factIds = List("central_space_edge")
            )
          }
        }
        .toList

    val mobilityRestriction =
      semantic.positionFeatures
        .flatMap { features =>
          val enemyLow = lowMobilityPiecesFor(opponentSide(side), features)
          val ours = lowMobilityPiecesFor(side, features)
          Option.when(enemyLow > ours) {
            evidence(
              ownerSide = side,
              kind = StrategicIdeaKind.SpaceGainOrRestriction,
              readiness = StrategicIdeaReadiness.Build,
              source = "mobility_restriction",
              confidence = 0.68 + math.min(0.06, (enemyLow - ours) * 0.02),
              focusZone = Some("center"),
              factIds = List("mobility_restriction")
            )
          }
        }
        .toList

    val lockedCenterBind =
      semantic.structureProfile.toList.flatMap { profile =>
        semantic.positionFeatures.flatMap { features =>
          Option.when(
            profile.centerState == CenterState.Locked &&
              (spaceDiffFor(side, features) > 0 || semantic.strategicState.exists(colorComplexClampFor(side, _)))
          ) {
            evidence(
              ownerSide = side,
              kind = StrategicIdeaKind.SpaceGainOrRestriction,
              readiness = StrategicIdeaReadiness.Build,
              source = "locked_center_bind",
              confidence = 0.70,
              focusZone = Some("center"),
              factIds = List("structure_locked_center")
            )
          }
        }
      }

    val alignmentSpaceRace =
      Option.when(hasAlignmentReason(semantic, "SPACE_RACE")) {
        evidence(
          ownerSide = side,
          kind = StrategicIdeaKind.SpaceGainOrRestriction,
          readiness = StrategicIdeaReadiness.Build,
          source = "plan_alignment_space_race",
          confidence = 0.68,
          focusZone = Some("center"),
          factIds = List("alignment_space_race")
        )
      }.toList

    val planBridge =
      topPlansFor(side, semantic).flatMap { plan =>
        Option.when(plan.plan.id == PlanId.SpaceAdvantage || plan.plan.id == PlanId.CentralControl) {
          evidence(
            ownerSide = side,
            kind = StrategicIdeaKind.SpaceGainOrRestriction,
            readiness = StrategicIdeaReadiness.Build,
            source = "plan_match_space_advantage",
            confidence = 0.78 + math.min(0.06, plan.score * 0.08),
            focusZone = Some("center"),
            factIds = List("plan_match_space_advantage", s"plan_${plan.plan.id.toString.toLowerCase}")
          )
        }
      }

    val maroczyProfile =
      Option.when(structureIs(semantic, StructureId.MaroczyBind) && side == "white") {
        evidence(
          ownerSide = side,
          kind = StrategicIdeaKind.SpaceGainOrRestriction,
          readiness = StrategicIdeaReadiness.Ready,
          source = "maroczy_bind_profile",
          confidence = 0.86,
          focusZone = Some("center"),
          factIds = List("structure_maroczy_bind")
        )
      }.toList

    val iqpSpaceBridge =
      Option.when(structureIs(semantic, StructureId.IQPWhite) && side == "white") {
        evidence(
          ownerSide = side,
          kind = StrategicIdeaKind.SpaceGainOrRestriction,
          readiness = StrategicIdeaReadiness.Build,
          source = "iqp_space_bridge",
          confidence = 0.84,
          focusZone = Some("center"),
          factIds = List("structure_iqp_white")
        )
      }.toList

    val iqpCentralPresence =
      Option.when(
        structureIs(semantic, StructureId.IQPWhite) &&
          side == "white" &&
          semantic.board.exists(board => pawnAt(board, Color.White, Square.D4))
      ) {
        evidence(
          ownerSide = side,
          kind = StrategicIdeaKind.SpaceGainOrRestriction,
          readiness = StrategicIdeaReadiness.Build,
          source = "iqp_central_presence",
          confidence = 0.82,
          focusSquares = List("d4"),
          focusZone = Some("center"),
          factIds = List("structure_iqp_white", "iqp_central_presence")
        )
      }.toList

    tagEvidence ++ clampEvidence ++ centralSpaceEvidence ++ mobilityRestriction ++ lockedCenterBind ++ alignmentSpaceRace ++ planBridge ++ maroczyProfile ++ iqpSpaceBridge ++ iqpCentralPresence

  private def collectTargetFixingEvidence(
      side: String,
      pack: StrategyPack,
      semantic: StrategicIdeaSemanticContext
  ): List[StrategicIdeaEvidence] =
    val enemyWeakSquares =
      semantic.positionalFeatures.collect {
        case PositionalTag.WeakSquare(square, owner) if !matchesSide(owner, side) => square.key
      }.toSet

    val weakSquareEvidence =
      enemyWeakSquares.toList.map { square =>
        evidence(
          ownerSide = side,
          kind = StrategicIdeaKind.TargetFixing,
          readiness = StrategicIdeaReadiness.Ready,
          source = "enemy_weak_square",
          confidence = 0.74,
          focusSquares = List(square),
          factIds = List(s"enemy_weak_square_$square")
        )
      }

    val colorComplexEvidence =
      semantic.positionalFeatures.collect {
        case PositionalTag.ColorComplexWeakness(owner, squareColor, squares) if !matchesSide(owner, side) =>
          evidence(
            ownerSide = side,
            kind = StrategicIdeaKind.TargetFixing,
            readiness = StrategicIdeaReadiness.Build,
            source = "color_complex_weakness",
            confidence = 0.70,
            focusSquares = squares.map(_.key).take(3),
            focusZone = Some(s"$squareColor squares"),
            factIds = List("enemy_color_complex_weakness", s"color_complex_$squareColor")
          )
      }

    val minorityAttackEvidence =
      semantic.positionalFeatures.collect {
        case PositionalTag.MinorityAttack(color, flank) if matchesSide(color, side) =>
          evidence(
            ownerSide = side,
            kind = StrategicIdeaKind.TargetFixing,
            readiness = StrategicIdeaReadiness.Build,
            source = "minority_attack_fixation",
            confidence = 0.70,
            focusZone = Some(flank),
            factIds = List(s"minority_attack_$flank")
          )
      }

    val weakComplexEvidence =
      semantic.structuralWeaknesses
        .filter { weakness =>
          !matchesSide(weakness.color, side) &&
          weakness.squares.nonEmpty &&
          (
            normalizeFactToken(weakness.cause) != "holes" ||
              weakness.isOutpost ||
              weakness.squares.map(_.key).exists(enemyWeakSquares.contains)
          )
        }
        .map { weakness =>
          val cause = normalizeFactToken(weakness.cause)
          evidence(
            ownerSide = side,
            kind = StrategicIdeaKind.TargetFixing,
            readiness = StrategicIdeaReadiness.Build,
            source = "weak_complex_fixation",
            confidence = weakComplexConfidence(weakness),
            focusSquares = weakness.squares.map(_.key).distinct.take(3),
            focusZone = zoneFromSquares(weakness.squares),
              factIds =
              List("weak_complex_fixation", s"weak_complex_$cause") ++
                Option.when(weakness.isOutpost)("weak_complex_outpost").toList
          )
        }

    val planBridge =
      topPlansFor(side, semantic).flatMap { plan =>
        Option.when(
          plan.plan.id == PlanId.WeakPawnAttack ||
            plan.plan.id == PlanId.MinorityAttack ||
            plan.plan.id == PlanId.Blockade
        ) {
          evidence(
            ownerSide = side,
            kind = StrategicIdeaKind.TargetFixing,
            readiness = StrategicIdeaReadiness.Build,
            source = "plan_match_target_fixing",
            confidence = 0.78 + math.min(0.06, plan.score * 0.08),
            focusSquares = enemyWeakSquares.toList.take(2),
            factIds = List("plan_match_target_fixing", s"plan_${plan.plan.id.toString.toLowerCase}")
          )
        }
      }

    val directionalFixation =
      pack.directionalTargets
        .filter(_.ownerSide == side)
        .flatMap { target =>
          Option.when(enemyWeakSquares.contains(target.targetSquare)) {
            evidence(
              ownerSide = side,
              kind = StrategicIdeaKind.TargetFixing,
              readiness = ideaReadinessFromDirectionalTarget(target.readiness),
              source = "directional_target_fixation",
              confidence = 0.64 + readinessBonus(target.readiness),
              focusSquares = List(target.targetSquare),
              beneficiaryPieces = List(target.piece),
              factIds = List("directional_target_fixation", s"directional_target_${target.targetSquare}")
            )
          }
        }

    val carlsbadFixation =
      Option.when(
        structureIs(semantic, StructureId.Carlsbad) &&
          semantic.positionalFeatures.exists {
            case PositionalTag.MinorityAttack(color, _) => matchesSide(color, side)
            case _                                      => false
          } &&
          enemyWeakSquares.nonEmpty
      ) {
        evidence(
          ownerSide = side,
          kind = StrategicIdeaKind.TargetFixing,
          readiness = StrategicIdeaReadiness.Build,
          source = "carlsbad_fixation_profile",
          confidence = 0.90,
          focusSquares = enemyWeakSquares.toList.take(3),
          factIds = List("structure_carlsbad", "carlsbad_fixation_profile", "minority_attack_fixation")
        )
      }.toList

    weakSquareEvidence ++ colorComplexEvidence ++ minorityAttackEvidence ++ weakComplexEvidence ++ planBridge ++ directionalFixation ++ carlsbadFixation

  private def collectLineOccupationEvidence(
      side: String,
      pack: StrategyPack,
      semantic: StrategicIdeaSemanticContext
  ): List[StrategicIdeaEvidence] =
    val openFileEvidence =
      semantic.positionalFeatures.collect {
        case PositionalTag.OpenFile(file, color) if matchesSide(color, side) =>
          evidence(
            ownerSide = side,
            kind = StrategicIdeaKind.LineOccupation,
            readiness = StrategicIdeaReadiness.Ready,
            source = "open_file_control",
            confidence = 0.82,
            focusFiles = List(fileToken(file)),
            beneficiaryPieces = List("R", "Q"),
            factIds = List(s"open_file_${fileToken(file)}")
          )
      }

    val doubledRooks =
      semantic.positionalFeatures.collect {
        case PositionalTag.DoubledRooks(file, color) if matchesSide(color, side) =>
          evidence(
            ownerSide = side,
            kind = StrategicIdeaKind.LineOccupation,
            readiness = StrategicIdeaReadiness.Ready,
            source = "doubled_rooks",
            confidence = 0.78,
            focusFiles = List(fileToken(file)),
            beneficiaryPieces = List("R"),
            factIds = List(s"doubled_rooks_${fileToken(file)}")
          )
      }

    val connectedRooks =
      semantic.positionalFeatures.collect {
        case PositionalTag.ConnectedRooks(color) if matchesSide(color, side) =>
          evidence(
            ownerSide = side,
            kind = StrategicIdeaKind.LineOccupation,
            readiness = StrategicIdeaReadiness.Build,
            source = "connected_rooks",
            confidence = 0.70,
            beneficiaryPieces = List("R"),
            factIds = List("connected_rooks")
          )
      }

    val rookOnSeventh =
      semantic.positionalFeatures.collect {
        case PositionalTag.RookOnSeventh(color) if matchesSide(color, side) =>
          evidence(
            ownerSide = side,
            kind = StrategicIdeaKind.LineOccupation,
            readiness = StrategicIdeaReadiness.Ready,
            source = "rook_on_seventh",
            confidence = 0.76,
            focusZone = Some("back rank"),
            beneficiaryPieces = List("R"),
            factIds = List("rook_on_seventh")
          )
      }

    val occupiedLineEvidence =
      semantic.board.toList.flatMap { board =>
        val color = sideColor(side)
        val occupiedSquares =
          board.byPiece(color, Rook).map(_ -> "R").toList ++
            board.byPiece(color, Queen).map(_ -> "Q").toList

        occupiedSquares.flatMap { case (square, piece) =>
          val open = isOpenFile(board, square.file)
          val semiOpen = isSemiOpenFileFor(board, square.file, color)
          val seventh = isSeventhRankFor(side, square)
          Option.when(open || semiOpen || seventh) {
            evidence(
              ownerSide = side,
              kind = StrategicIdeaKind.LineOccupation,
              readiness = StrategicIdeaReadiness.Ready,
              source = "occupied_line_control",
              confidence =
                (if piece == "R" then 0.84 else 0.78) +
                  (if open then 0.04 else if semiOpen then 0.02 else 0.0) +
                  (if seventh then 0.02 else 0.0),
              focusSquares = List(square.key),
              focusFiles = Option.when(open || semiOpen)(List(fileToken(square.file))).getOrElse(Nil),
              focusZone = if seventh then Some("back rank") else zoneFromFileToken(fileToken(square.file)),
              beneficiaryPieces = List(piece),
              factIds =
                List(
                  Some("occupied_line_control"),
                  Some(s"occupied_${piece.toLowerCase}_${square.key}"),
                  Option.when(open)(s"open_file_${fileToken(square.file)}"),
                  Option.when(semiOpen)(s"semi_open_file_${fileToken(square.file)}"),
                  Option.when(seventh)("occupied_seventh_rank")
                ).flatten
            )
          }
        }
      }

    val routeEvidence =
      pack.pieceRoutes
        .filter(route => route.ownerSide == side && route.surfaceMode != RouteSurfaceMode.Hidden && isMajorPiece(route.piece))
        .flatMap { route =>
          route.route.lastOption.flatMap(squareFromKey).flatMap { endpoint =>
            lineAccessFacts(side, endpoint, semantic).map { case (focusFiles, focusZone, factIds) =>
              evidence(
                ownerSide = side,
                kind = StrategicIdeaKind.LineOccupation,
                readiness = ideaReadinessFromRoute(route.surfaceMode),
                source = "route_line_access",
                confidence = 0.70 + route.surfaceConfidence * 0.12,
                focusSquares = List(endpoint.key),
                focusFiles = focusFiles,
                focusZone = focusZone,
                beneficiaryPieces = List(route.piece),
                factIds = factIds ++ List(s"route_surface_${route.surfaceMode.toLowerCase}")
              )
            }
          }
        }

    val directionalEvidence =
      pack.directionalTargets
        .filter(target => target.ownerSide == side && isMajorPiece(target.piece))
        .flatMap { target =>
          squareFromKey(target.targetSquare).flatMap { endpoint =>
            lineAccessFacts(side, endpoint, semantic).map { case (focusFiles, focusZone, factIds) =>
              evidence(
                ownerSide = side,
                kind = StrategicIdeaKind.LineOccupation,
                readiness = ideaReadinessFromDirectionalTarget(target.readiness),
                source = "directional_line_access",
                confidence = 0.66 + readinessBonus(target.readiness),
                focusSquares = List(endpoint.key),
                focusFiles = focusFiles,
                focusZone = focusZone,
                beneficiaryPieces = List(target.piece),
                factIds = factIds ++ List("directional_line_access")
              )
            }
          }
        }

    val featureSupport =
      semantic.positionFeatures
        .flatMap { features =>
          val hasMajorAccess =
            pack.pieceRoutes.exists(route =>
              route.ownerSide == side &&
                route.surfaceMode != RouteSurfaceMode.Hidden &&
                isMajorPiece(route.piece)
            ) ||
              pack.directionalTargets.exists(target =>
                target.ownerSide == side &&
                  isMajorPiece(target.piece)
              )

          Option.when(hasMajorAccess && (semiOpenFilesFor(side, features) > 0 || openFilesCount(features) > 0)) {
            evidence(
              ownerSide = side,
              kind = StrategicIdeaKind.LineOccupation,
              readiness = StrategicIdeaReadiness.Build,
              source = "line_control_features",
              confidence = 0.66,
              beneficiaryPieces = List("R", "Q"),
              factIds = List("line_control_features")
            )
          }
        }
        .toList

    val planBridge =
      topPlansFor(side, semantic).flatMap { plan =>
        Option.when(plan.plan.id == PlanId.FileControl || plan.plan.id == PlanId.RookActivation) {
          evidence(
            ownerSide = side,
            kind = StrategicIdeaKind.LineOccupation,
            readiness = StrategicIdeaReadiness.Build,
            source = "plan_match_line_occupation",
            confidence = 0.80 + math.min(0.06, plan.score * 0.08),
            beneficiaryPieces = List("R", "Q"),
            factIds = List("plan_match_line_occupation", s"plan_${plan.plan.id.toString.toLowerCase}")
          )
        }
      }

    openFileEvidence ++ doubledRooks ++ connectedRooks ++ rookOnSeventh ++ occupiedLineEvidence ++ routeEvidence ++ directionalEvidence ++ featureSupport ++ planBridge

  private def collectOutpostEvidence(
      side: String,
      pack: StrategyPack,
      semantic: StrategicIdeaSemanticContext
  ): List[StrategicIdeaEvidence] =
    val tagEvidence =
      semantic.positionalFeatures.collect {
        case PositionalTag.Outpost(square, color) if matchesSide(color, side) =>
          evidence(
            ownerSide = side,
            kind = StrategicIdeaKind.OutpostCreationOrOccupation,
            readiness = StrategicIdeaReadiness.Ready,
            source = "outpost_tag",
            confidence = 0.84,
            focusSquares = List(square.key),
            beneficiaryPieces = List("N", "B"),
            factIds = List(s"outpost_${square.key}")
          )
      }

    val strongKnightEvidence =
      semantic.positionalFeatures.collect {
        case PositionalTag.StrongKnight(square, color) if matchesSide(color, side) =>
          evidence(
            ownerSide = side,
            kind = StrategicIdeaKind.OutpostCreationOrOccupation,
            readiness = StrategicIdeaReadiness.Ready,
            source = "strong_knight",
            confidence = 0.80,
            focusSquares = List(square.key),
            beneficiaryPieces = List("N"),
            factIds = List(s"strong_knight_${square.key}")
          )
      }

    val entrenchedSupport =
      semantic.strategicState
        .filter(entrenchedPiecesFor(side, _) > 0)
        .map { state =>
          evidence(
            ownerSide = side,
            kind = StrategicIdeaKind.OutpostCreationOrOccupation,
            readiness = StrategicIdeaReadiness.Build,
            source = "entrenched_piece_state",
            confidence = 0.68 + math.min(0.08, entrenchedPiecesFor(side, state) * 0.02),
            beneficiaryPieces = List("N", "B"),
            factIds = List("entrenched_piece_state")
          )
        }
        .toList

    val routeEvidence =
      pack.pieceRoutes
        .filter(route => route.ownerSide == side && route.surfaceMode != RouteSurfaceMode.Hidden && isMinorPiece(route.piece))
        .flatMap { route =>
          route.route.lastOption.flatMap(squareFromKey).filter(isOutpostSquareFor(side, _, semantic)).map { endpoint =>
            evidence(
              ownerSide = side,
              kind = StrategicIdeaKind.OutpostCreationOrOccupation,
              readiness = ideaReadinessFromRoute(route.surfaceMode),
              source = "route_outpost_access",
              confidence = 0.64 + route.surfaceConfidence * 0.08,
              focusSquares = List(endpoint.key),
              beneficiaryPieces = List(route.piece),
              factIds = List("route_outpost_access", s"route_surface_${route.surfaceMode.toLowerCase}")
            )
          }
        }

    val directionalEvidence =
      pack.directionalTargets
        .filter(target => target.ownerSide == side && isMinorPiece(target.piece))
        .flatMap { target =>
          squareFromKey(target.targetSquare).filter(isOutpostSquareFor(side, _, semantic)).map { endpoint =>
            evidence(
              ownerSide = side,
              kind = StrategicIdeaKind.OutpostCreationOrOccupation,
              readiness = ideaReadinessFromDirectionalTarget(target.readiness),
              source = "directional_outpost_access",
              confidence = 0.66 + readinessBonus(target.readiness),
              focusSquares = List(endpoint.key),
              beneficiaryPieces = List(target.piece),
              factIds = List("directional_outpost_access")
            )
          }
        }

    tagEvidence ++ strongKnightEvidence ++ entrenchedSupport ++ routeEvidence ++ directionalEvidence

  private def collectMinorPieceImbalanceEvidence(
      side: String,
      semantic: StrategicIdeaSemanticContext
  ): List[StrategicIdeaEvidence] =
    val bishopPair =
      semantic.positionalFeatures.collect {
        case PositionalTag.BishopPairAdvantage(color) if matchesSide(color, side) =>
          evidence(
            ownerSide = side,
            kind = StrategicIdeaKind.MinorPieceImbalanceExploitation,
            readiness = StrategicIdeaReadiness.Ready,
            source = "bishop_pair_advantage",
            confidence = 0.82,
            beneficiaryPieces = List("B"),
            factIds = List("bishop_pair_advantage")
          )
      }

    val enemyBadBishop =
      semantic.positionalFeatures.collect {
        case PositionalTag.BadBishop(color) if !matchesSide(color, side) =>
          evidence(
            ownerSide = side,
            kind = StrategicIdeaKind.MinorPieceImbalanceExploitation,
            readiness = StrategicIdeaReadiness.Build,
            source = "enemy_bad_bishop",
            confidence = 0.80,
            beneficiaryPieces = List("N", "B"),
            factIds = List("enemy_bad_bishop")
          )
      }

    val goodBishop =
      semantic.positionalFeatures.collect {
        case PositionalTag.GoodBishop(color) if matchesSide(color, side) =>
          evidence(
            ownerSide = side,
            kind = StrategicIdeaKind.MinorPieceImbalanceExploitation,
            readiness = StrategicIdeaReadiness.Build,
            source = "good_bishop",
            confidence = 0.74,
            beneficiaryPieces = List("B"),
            factIds = List("good_bishop")
          )
      }

    val oppositeColorBishops =
      semantic.positionalFeatures.collect {
        case PositionalTag.OppositeColorBishops =>
          evidence(
            ownerSide = side,
            kind = StrategicIdeaKind.MinorPieceImbalanceExploitation,
            readiness = StrategicIdeaReadiness.Build,
            source = "opposite_color_bishops",
            confidence = 0.68,
            beneficiaryPieces = List("B"),
            factIds = List("opposite_color_bishops")
          )
      }

    val strongKnightBridge =
      for
        square <- semantic.positionalFeatures.collect {
          case PositionalTag.StrongKnight(sq, color) if matchesSide(color, side) => sq.key
        }
        if enemyBadBishop.nonEmpty
      yield
        evidence(
          ownerSide = side,
          kind = StrategicIdeaKind.MinorPieceImbalanceExploitation,
          readiness = StrategicIdeaReadiness.Ready,
          source = "strong_knight_vs_bad_bishop",
          confidence = 0.78,
          focusSquares = List(square),
          beneficiaryPieces = List("N"),
          factIds = List("strong_knight_vs_bad_bishop", s"strong_knight_$square")
        )

    val activityBridge =
      semantic.board.toList.flatMap { board =>
        semantic.pieceActivity.flatMap { activity =>
          Option.when(activity.isBadBishop && board.colorAt(activity.square).exists(color => !matchesSide(color, side))) {
            evidence(
              ownerSide = side,
              kind = StrategicIdeaKind.MinorPieceImbalanceExploitation,
              readiness = StrategicIdeaReadiness.Build,
              source = "piece_activity_bad_bishop",
              confidence = 0.74,
              focusSquares = List(activity.square.key),
              beneficiaryPieces = List("N", "B"),
              factIds = List("piece_activity_bad_bishop", s"enemy_bad_bishop_${activity.square.key}")
            )
          }
        }
      }

    val countBasedImbalance =
      semantic.positionFeatures
        .flatMap { features =>
          val bishopEdge = bishopCountFor(side, features) - bishopCountFor(opponentSide(side), features)
          val knightEdge = knightCountFor(side, features) - knightCountFor(opponentSide(side), features)
          val facts =
            List(
              Option.when(bishopPairFor(side, features))("bishop_pair_count_edge"),
              Option.when(goodBishop.nonEmpty && bishopEdge > 0)("good_bishop_count_edge"),
              Option.when(enemyBadBishop.nonEmpty && knightEdge >= 0 && strongKnightBridge.nonEmpty)("knight_vs_bishop_count_edge")
            ).flatten
          Option.when(facts.nonEmpty) {
            evidence(
              ownerSide = side,
              kind = StrategicIdeaKind.MinorPieceImbalanceExploitation,
              readiness = StrategicIdeaReadiness.Build,
              source = "minor_piece_count_imbalance",
              confidence = 0.72 + math.min(0.06, facts.size * 0.02),
              beneficiaryPieces =
                List(
                  Option.when(bishopEdge > 0 || bishopPairFor(side, features))("B"),
                  Option.when(knightEdge >= 0 && enemyBadBishop.nonEmpty)("N")
                ).flatten,
              factIds = List("minor_piece_count_imbalance") ++ facts
            )
          }
        }
        .toList

    val frenchProfileBridge =
      Option.when(
        structureIs(semantic, StructureId.FrenchAdvanceChain) &&
          side == "white" &&
          (enemyBadBishop.nonEmpty || goodBishop.nonEmpty || strongKnightBridge.nonEmpty)
      ) {
        evidence(
          ownerSide = side,
          kind = StrategicIdeaKind.MinorPieceImbalanceExploitation,
          readiness = StrategicIdeaReadiness.Build,
          source = "french_minor_piece_profile",
          confidence = 0.80,
          beneficiaryPieces = List("N", "B"),
          factIds = List("structure_french_advance_chain", "french_minor_piece_profile")
        )
      }.toList

    bishopPair ++ enemyBadBishop ++ goodBishop ++ oppositeColorBishops ++ strongKnightBridge ++ activityBridge ++ countBasedImbalance ++ frenchProfileBridge

  private def collectProphylaxisEvidence(
      side: String,
      semantic: StrategicIdeaSemanticContext
  ): List[StrategicIdeaEvidence] =
    val preventedEvidence =
      semantic.preventedPlans.flatMap { prevented =>
        Option.when(isPreventiveWithoutCounterplaySuppression(prevented)) {
          evidence(
            ownerSide = side,
            kind = StrategicIdeaKind.Prophylaxis,
            readiness = StrategicIdeaReadiness.Ready,
            source = "prevented_plan",
            confidence = 0.78 + prophylaxisThreatBonus(prevented),
            focusSquares = prevented.deniedSquares.map(_.key).take(3),
            focusFiles = prevented.breakNeutralized.toList.flatMap(normalizeFileToken),
            factIds =
              List("prevented_plan") ++
                Option.when(prevented.preventedThreatType.isDefined)("prevented_threat").toList ++
                Option.when(prevented.mobilityDelta < 0)("prevented_mobility").toList ++
                Option.when(prevented.deniedResourceClass.contains("forcing_threat"))("denied_forcing_threat").toList
          )
        }
      }

    val threatBridge =
      semantic.threatsToUs.toList.flatMap { threats =>
        Option.when(isThreatDrivenProphylaxis(threats)) {
          evidence(
            ownerSide = side,
            kind = StrategicIdeaKind.Prophylaxis,
            readiness = StrategicIdeaReadiness.Build,
            source = "threat_analysis_prophylaxis",
            confidence = 0.76 + threatDefenseBonus(threats),
            focusSquares = threatSquares(threats),
            focusZone = threatFocusZone(threats),
            factIds =
              List("threat_analysis_prophylaxis") ++
                Option.when(threats.prophylaxisNeeded)("prophylaxis_needed").toList ++
                Option.when(threats.defense.prophylaxisNeeded)("defensive_prophylaxis").toList ++
                Option.when(threats.resourceAvailable)("defensive_resources_available").toList
          )
        }
      }

    val counterBreakWatch =
      semantic.opponentPawnAnalysis.toList.flatMap { analysis =>
        val file = analysis.breakFile.flatMap(normalizeFileToken)
        Option.when(analysis.counterBreak && semantic.threatsToUs.exists(isThreatDrivenProphylaxis)) {
          evidence(
            ownerSide = side,
            kind = StrategicIdeaKind.Prophylaxis,
            readiness = StrategicIdeaReadiness.Build,
            source = "opponent_counterbreak_watch",
            confidence = 0.70,
            focusFiles = file.toList,
            focusZone = file.flatMap(zoneFromFileToken),
            factIds = List("opponent_counter_break_watch", "opponent_counter_break")
          )
        }
      }

    val planBridge =
      topPlansFor(side, semantic).flatMap { plan =>
        Option.when(
          plan.plan.id == PlanId.Prophylaxis || plan.plan.id == PlanId.DefensiveConsolidation
        ) {
          evidence(
            ownerSide = side,
            kind = StrategicIdeaKind.Prophylaxis,
            readiness = StrategicIdeaReadiness.Build,
            source = "plan_match_prophylaxis",
            confidence = 0.80 + math.min(0.06, plan.score * 0.08),
            focusFiles =
              Option.when(hasQueensideClampWatch(side, semantic))("b").toList ++
                Option.when(hasBishopPinWatch(side, semantic))("g").toList,
            factIds = List("plan_match_prophylaxis", s"plan_${plan.plan.id.toString.toLowerCase}")
          )
        }
      }

    val bishopPinWatch =
      Option.when(hasBishopPinWatch(side, semantic)) {
        evidence(
          ownerSide = side,
          kind = StrategicIdeaKind.Prophylaxis,
          readiness = StrategicIdeaReadiness.Build,
          source = "bishop_pin_watch",
          confidence = 0.84,
          focusSquares = if side == "white" then List("g4") else List("g5"),
          focusZone = Some("kingside"),
          factIds = List("bishop_pin_watch")
        )
      }.toList

    val queensideClampWatch =
      Option.when(hasQueensideClampWatch(side, semantic)) {
        evidence(
          ownerSide = side,
          kind = StrategicIdeaKind.Prophylaxis,
          readiness = StrategicIdeaReadiness.Build,
          source = "queenside_counterbreak_watch",
          confidence = 0.90,
          focusFiles = List("b"),
          focusZone = Some("queenside"),
          factIds = List("queenside_counterbreak_watch")
        )
      }.toList

    preventedEvidence ++ threatBridge ++ counterBreakWatch ++ planBridge ++ bishopPinWatch ++ queensideClampWatch

  private def collectKingAttackEvidence(
      side: String,
      pack: StrategyPack,
      semantic: StrategicIdeaSemanticContext
  ): List[StrategicIdeaEvidence] =
    val enemyKingZone = semantic.board.flatMap(board => board.kingPosOf(sideColor(opponentSide(side)))).flatMap(zoneFromSquare)

    val mateNet =
      semantic.positionalFeatures.collect {
        case PositionalTag.MateNet(color) if matchesSide(color, side) =>
          evidence(
            ownerSide = side,
            kind = StrategicIdeaKind.KingAttackBuildUp,
            readiness = StrategicIdeaReadiness.Ready,
            source = "mate_net",
            confidence = 0.88,
            focusZone = enemyKingZone,
            factIds = List("mate_net")
          )
      }

    val stuckCenter =
      semantic.positionalFeatures.collect {
        case PositionalTag.KingStuckCenter(color) if !matchesSide(color, side) =>
          evidence(
            ownerSide = side,
            kind = StrategicIdeaKind.KingAttackBuildUp,
            readiness = StrategicIdeaReadiness.Build,
            source = "enemy_king_stuck_center",
            confidence = 0.80,
            focusZone = enemyKingZone.orElse(Some("center")),
            factIds = List("enemy_king_stuck_center")
          )
      }

    val weakBackRank =
      semantic.positionalFeatures.collect {
        case PositionalTag.WeakBackRank(color) if !matchesSide(color, side) =>
          evidence(
            ownerSide = side,
            kind = StrategicIdeaKind.KingAttackBuildUp,
            readiness = StrategicIdeaReadiness.Build,
            source = "enemy_weak_back_rank",
            confidence = 0.74,
            focusZone = enemyKingZone,
            factIds = List("enemy_weak_back_rank")
          )
      }

    val kingRingPressure =
      semantic.positionFeatures
        .flatMap { features =>
          val attackers = attackersCountFor(side, features)
          val ring = enemyKingRingAttackedFor(side, features)
          val exposed = enemyKingExposedFilesFor(side, features)
          Option.when(attackers >= 2 && (ring >= 2 || exposed > 0)) {
            evidence(
              ownerSide = side,
              kind = StrategicIdeaKind.KingAttackBuildUp,
              readiness = StrategicIdeaReadiness.Build,
              source = "king_ring_pressure",
              confidence = 0.78 + math.min(0.08, ring * 0.02),
              focusZone = enemyKingZone,
              factIds = List("king_ring_pressure") ++ Option.when(exposed > 0)("king_exposed_files").toList
            )
          }
        }
        .toList

    val flankPawns =
      semantic.strategicState.toList.flatMap { state =>
        val facts =
          List(
            Option.when(hookCreationChanceFor(side, state))("hook_creation_chance"),
            Option.when(rookPawnMarchReadyFor(side, state))("rook_pawn_march_ready")
          ).flatten
        Option.when(facts.nonEmpty) {
          evidence(
            ownerSide = side,
            kind = StrategicIdeaKind.KingAttackBuildUp,
            readiness = StrategicIdeaReadiness.Build,
            source = "flank_pawn_pressure",
            confidence = 0.74 + (facts.size * 0.03),
            focusZone = enemyKingZone,
            factIds = facts
          )
        }
      }

    val attackingThreats =
      semantic.threatsToThem.toList.flatMap { threats =>
        Option.when(isKingAttackThreatProfile(threats, side, semantic)) {
          evidence(
            ownerSide = side,
            kind = StrategicIdeaKind.KingAttackBuildUp,
            readiness = if threats.immediateThreat then StrategicIdeaReadiness.Ready else StrategicIdeaReadiness.Build,
            source = "attacking_threat_analysis",
            confidence = 0.78 + Option.when(threats.primaryDriver == "mate_threat")(0.06).getOrElse(0.0),
            focusSquares = threatSquares(threats),
            focusZone = enemyKingZone.orElse(threatFocusZone(threats)),
            factIds =
              List("attacking_threat_analysis") ++
                Option.when(threats.primaryDriver == "mate_threat")("mate_threat").toList ++
                Option.when(threats.threats.exists(_.kind == ThreatKind.Mate))("mate_threat_kind").toList
          )
        }
      }

    val motifPressure =
      semantic.motifs.flatMap {
        case Motif.RookLift(file, _, _, color, _, _) if matchesSide(color, side) =>
          Some(
            evidence(
              ownerSide = side,
              kind = StrategicIdeaKind.KingAttackBuildUp,
              readiness = StrategicIdeaReadiness.Build,
              source = "motif_rook_lift",
              confidence = 0.78,
              focusFiles = List(fileToken(file)),
              focusZone = enemyKingZone.orElse(zoneFromFileToken(fileToken(file))),
              beneficiaryPieces = List("R"),
              factIds = List("motif_rook_lift")
            )
          )
        case Motif.Battery(front, back, axis, color, _, _, frontSq, backSq)
            if matchesSide(color, side) &&
              (axis == Motif.BatteryAxis.File || axis == Motif.BatteryAxis.Diagonal) =>
          val squares = (frontSq.toList ++ backSq.toList).distinct
          Some(
            evidence(
              ownerSide = side,
              kind = StrategicIdeaKind.KingAttackBuildUp,
              readiness = StrategicIdeaReadiness.Build,
              source = "motif_battery",
              confidence = 0.74,
              focusSquares = squares.map(_.key).take(2),
              focusZone = enemyKingZone.orElse(zoneFromSquares(squares)),
              beneficiaryPieces = List(roleToken(front), roleToken(back)),
              factIds = List("motif_battery", s"battery_axis_${axis.toString.toLowerCase}")
            )
          )
        case Motif.PieceLift(piece, _, _, color, _, _) if matchesSide(color, side) =>
          Some(
            evidence(
              ownerSide = side,
              kind = StrategicIdeaKind.KingAttackBuildUp,
              readiness = StrategicIdeaReadiness.Build,
              source = "motif_piece_lift",
              confidence = 0.72,
              focusZone = enemyKingZone,
              beneficiaryPieces = List(roleToken(piece)),
              factIds = List("motif_piece_lift")
            )
          )
        case Motif.Check(piece, targetSquare, checkType, color, _, _) if matchesSide(color, side) =>
          Some(
            evidence(
              ownerSide = side,
              kind = StrategicIdeaKind.KingAttackBuildUp,
              readiness = StrategicIdeaReadiness.Ready,
              source = "motif_check_pressure",
              confidence = 0.68 + checkTypeBonus(checkType),
              focusSquares = List(targetSquare.key),
              focusZone = enemyKingZone.orElse(zoneFromSquare(targetSquare)),
              beneficiaryPieces = List(roleToken(piece)),
              factIds = List("motif_check_pressure", s"check_type_${checkType.toString.toLowerCase}")
            )
          )
        case _ => None
      }

    val routePressure =
      pack.pieceRoutes
        .filter(route => route.ownerSide == side && route.surfaceMode != RouteSurfaceMode.Hidden)
        .flatMap { route =>
          route.route.lastOption.flatMap(squareFromKey).filter(isNearEnemyKing(side, _, semantic)).map { endpoint =>
            evidence(
              ownerSide = side,
              kind = StrategicIdeaKind.KingAttackBuildUp,
              readiness = ideaReadinessFromRoute(route.surfaceMode),
              source = "route_attack_lane",
              confidence = 0.70 + route.surfaceConfidence * 0.10,
              focusSquares = List(endpoint.key),
              focusZone = enemyKingZone,
              beneficiaryPieces = List(route.piece),
              factIds = List("route_attack_lane", s"route_surface_${route.surfaceMode.toLowerCase}")
            )
          }
        }

    val directionalPressure =
      pack.directionalTargets
        .filter(_.ownerSide == side)
        .flatMap { target =>
          squareFromKey(target.targetSquare).filter(isNearEnemyKing(side, _, semantic)).map { endpoint =>
            evidence(
              ownerSide = side,
              kind = StrategicIdeaKind.KingAttackBuildUp,
              readiness = ideaReadinessFromDirectionalTarget(target.readiness),
              source = "directional_attack_lane",
              confidence = 0.68 + readinessBonus(target.readiness),
              focusSquares = List(endpoint.key),
              focusZone = enemyKingZone,
              beneficiaryPieces = List(target.piece),
              factIds = List("directional_attack_lane")
            )
          }
        }

    val planBridge =
      topPlansFor(side, semantic).flatMap { plan =>
        Option.when(plan.plan.id == PlanId.KingsideAttack || plan.plan.id == PlanId.PawnStorm) {
          evidence(
            ownerSide = side,
            kind = StrategicIdeaKind.KingAttackBuildUp,
            readiness = StrategicIdeaReadiness.Build,
            source = "plan_match_king_attack",
            confidence = 0.82 + math.min(0.06, plan.score * 0.08),
            focusZone = enemyKingZone,
            factIds = List("plan_match_king_attack", s"plan_${plan.plan.id.toString.toLowerCase}")
          )
        }
      }

    val oppositeSideStorm =
      Option.when(hasOppositeSideStormAttack(side, semantic)) {
        evidence(
          ownerSide = side,
          kind = StrategicIdeaKind.KingAttackBuildUp,
          readiness = StrategicIdeaReadiness.Build,
          source = "opposite_side_storm",
          confidence = 0.84,
          focusZone = enemyKingZone,
          factIds = List("opposite_side_storm")
        )
      }.toList

    val fianchettoAssault =
      Option.when(
        structureIs(semantic, StructureId.FianchettoShell) &&
          hasOppositeSideStormAttack(side, semantic) &&
          semantic.board.exists(board =>
            board.kingPosOf(sideColor(side)).exists(king => king.file.value <= File.C.value || king.file.value >= File.F.value)
          )
      ) {
        evidence(
          ownerSide = side,
          kind = StrategicIdeaKind.KingAttackBuildUp,
          readiness = StrategicIdeaReadiness.Build,
          source = "fianchetto_assault_profile",
          confidence = 0.90,
          focusZone = enemyKingZone,
          factIds = List("structure_fianchetto_shell", "fianchetto_assault_profile", "opposite_side_storm")
        )
      }.toList

    mateNet ++ stuckCenter ++ weakBackRank ++ kingRingPressure ++ flankPawns ++ attackingThreats ++ motifPressure ++ routePressure ++ directionalPressure ++ planBridge ++ oppositeSideStorm ++ fianchettoAssault

  private def collectFavorableTradeEvidence(
      side: String,
      pack: StrategyPack,
      semantic: StrategicIdeaSemanticContext
  ): List[StrategicIdeaEvidence] =
    val removingTheDefender =
      semantic.positionalFeatures.collect {
        case PositionalTag.RemovingTheDefender(target, color) if matchesSide(color, side) =>
          evidence(
            ownerSide = side,
            kind = StrategicIdeaKind.FavorableTradeOrTransformation,
            readiness = StrategicIdeaReadiness.Ready,
            source = "removing_the_defender",
            confidence = 0.84,
            beneficiaryPieces = List(roleToken(target)),
            factIds = List("removing_the_defender", s"removing_defender_${target.name.toLowerCase}")
          )
      }

    val winningEndgameTransition =
      semantic.endgameFeatures
        .filter(_.theoreticalOutcomeHint == TheoreticalOutcomeHint.Win)
        .flatMap { feature =>
          semantic.positionFeatures.flatMap { features =>
            Option.when(materialEdgeFor(side, features) >= 100 || semantic.phase == "endgame") {
              evidence(
                ownerSide = side,
                kind = StrategicIdeaKind.FavorableTradeOrTransformation,
                readiness = StrategicIdeaReadiness.Ready,
                source = "winning_endgame_transition",
                confidence = 0.80,
                focusSquares = feature.keySquaresControlled.map(_.key).take(3),
                factIds = List("winning_endgame_transition")
              )
            }
          }
        }
        .toList

    val classificationWindow =
      semantic.classification.toList.flatMap { classification =>
        semantic.positionFeatures.flatMap { features =>
          val evalEdge = materialEdgeFor(side, features)
          val facts =
            List(
              Option.when(classification.simplifyBias.shouldSimplify)("simplify_window"),
              Option.when(classification.taskMode.isConvertMode)("convert_mode"),
              Option.when(hasAlignmentReason(semantic, "TRANSFORMATION"))("alignment_transformation")
            ).flatten
          Option.when(facts.nonEmpty && (evalEdge >= 80 || classification.taskMode.isConvertMode)) {
            evidence(
              ownerSide = side,
              kind = StrategicIdeaKind.FavorableTradeOrTransformation,
              readiness = if evalEdge >= 120 then StrategicIdeaReadiness.Ready else StrategicIdeaReadiness.Build,
              source = "classification_transformation_window",
              confidence = 0.76 + Option.when(classification.taskMode.isConvertMode)(0.04).getOrElse(0.0),
              factIds = List("classification_transformation_window") ++ facts
            )
          }
        }
      }

    val exchangeAvailabilityBridge =
      semantic.classification.toList.flatMap { classification =>
        Option.when(
          classification.simplifyBias.exchangeAvailable &&
            structureIs(semantic, StructureId.IQPBlack) &&
            side == "white"
        ) {
          evidence(
            ownerSide = side,
            kind = StrategicIdeaKind.FavorableTradeOrTransformation,
            readiness = StrategicIdeaReadiness.Build,
            source = "exchange_availability_bridge",
            confidence = 0.90,
            factIds =
              List(
                Some("exchange_availability_bridge"),
                Some("structure_iqp_black")
              ).flatten
          )
        }
      }

    val moveRefEvidence =
      pack.pieceMoveRefs
        .filter(ref => ref.ownerSide == side && ref.tacticalTheme.contains("capture_or_exchange"))
        .flatMap { ref =>
          Option.when(favorableTradeContext(side, ref, semantic, removingTheDefender.nonEmpty || winningEndgameTransition.nonEmpty)) {
            evidence(
              ownerSide = side,
              kind = StrategicIdeaKind.FavorableTradeOrTransformation,
              readiness =
                if winningEndgameTransition.nonEmpty || removingTheDefender.nonEmpty then StrategicIdeaReadiness.Ready
                else StrategicIdeaReadiness.Build,
              source = "capture_exchange_transformation",
              confidence = 0.70 + moveRefSupportBonus(side, ref, semantic),
              focusSquares = List(ref.target),
              beneficiaryPieces = List(ref.piece),
              factIds = List("capture_or_exchange") ++ ref.evidence.filter(_.startsWith("target_"))
            )
          }
        }

    val planBridge =
      topPlansFor(side, semantic).flatMap { plan =>
        Option.when(
          (
            plan.plan.id == PlanId.Exchange ||
              plan.plan.id == PlanId.Simplification ||
              plan.plan.id == PlanId.QueenTrade
          ) &&
            (
              removingTheDefender.nonEmpty ||
                winningEndgameTransition.nonEmpty ||
                classificationWindow.nonEmpty ||
                exchangeAvailabilityBridge.nonEmpty ||
                structureIs(semantic, StructureId.IQPBlack)
            )
        ) {
          evidence(
            ownerSide = side,
            kind = StrategicIdeaKind.FavorableTradeOrTransformation,
            readiness = StrategicIdeaReadiness.Build,
            source = "plan_match_transformation",
            confidence = 0.74 + math.min(0.06, plan.score * 0.08),
            factIds = List("plan_match_transformation", s"plan_${plan.plan.id.toString.toLowerCase}")
          )
        }
      }

    val iqpSimplification =
      Option.when(structureIs(semantic, StructureId.IQPBlack) && side == "white") {
        evidence(
          ownerSide = side,
          kind = StrategicIdeaKind.FavorableTradeOrTransformation,
          readiness = StrategicIdeaReadiness.Build,
          source = "iqp_simplification_profile",
          confidence = 0.92,
          factIds = List("structure_iqp_black", "iqp_simplification_profile")
        )
      }.toList

    removingTheDefender ++ winningEndgameTransition ++ classificationWindow ++ exchangeAvailabilityBridge ++ moveRefEvidence ++ planBridge ++ iqpSimplification

  private def collectCounterplaySuppressionEvidence(
      side: String,
      semantic: StrategicIdeaSemanticContext
  ): List[StrategicIdeaEvidence] =
    val preventedEvidence =
      semantic.preventedPlans.flatMap { prevented =>
        Option.when(isCounterplaySuppression(prevented)) {
          evidence(
            ownerSide = side,
            kind = StrategicIdeaKind.CounterplaySuppression,
            readiness = StrategicIdeaReadiness.Ready,
            source = "counterplay_suppression",
            confidence = 0.82 + counterplaySuppressionBonus(prevented),
            focusSquares = prevented.deniedSquares.map(_.key).take(3),
            focusFiles = prevented.breakNeutralized.toList.flatMap(normalizeFileToken),
            factIds =
              List("counterplay_suppression") ++
                Option.when(prevented.breakNeutralized.isDefined)("break_neutralized").toList ++
                Option.when(prevented.counterplayScoreDrop >= 100)("counterplay_score_drop").toList ++
                Option.when(prevented.deniedResourceClass.contains("break"))("denied_break_resource").toList
          )
        }
      }

    val counterBreakBridge =
      semantic.opponentPawnAnalysis.toList.flatMap { analysis =>
        val file = analysis.breakFile.flatMap(normalizeFileToken)
        Option.when(analysis.counterBreak && semantic.preventedPlans.exists(preventsCounterBreak(_, analysis))) {
          evidence(
            ownerSide = side,
            kind = StrategicIdeaKind.CounterplaySuppression,
            readiness = StrategicIdeaReadiness.Ready,
            source = "opponent_counterbreak_denial",
            confidence = 0.80 + Option.when(analysis.pawnBreakReady)(0.04).getOrElse(0.0),
            focusSquares = semantic.preventedPlans.flatMap(_.deniedSquares.map(_.key)).distinct.take(3),
            focusFiles = file.toList,
            focusZone = file.flatMap(zoneFromFileToken),
            factIds = List("opponent_counterbreak_denial", "opponent_counter_break")
          )
        }
      }

    val threatBridge =
      semantic.threatsToUs.toList.flatMap { threats =>
        Option.when(isThreatDrivenCounterplaySuppression(threats, semantic.opponentPawnAnalysis, semantic.preventedPlans)) {
          evidence(
            ownerSide = side,
            kind = StrategicIdeaKind.CounterplaySuppression,
            readiness = StrategicIdeaReadiness.Ready,
            source = "threat_analysis_counterplay",
            confidence = 0.78 + threatSuppressionBonus(threats),
            focusSquares = threatSquares(threats),
            focusZone = threatFocusZone(threats),
            factIds =
              List("threat_analysis_counterplay") ++
                Option.when(threats.strategicThreat)("strategic_threat").toList ++
                Option.when(threats.maxLossIfIgnored >= 180)("high_counterplay_cost").toList
          )
        }
      }

    val structureBridge =
      List(
        Option.when(structureIs(semantic, StructureId.Hedgehog) && side == "white") {
          evidence(
            ownerSide = side,
            kind = StrategicIdeaKind.CounterplaySuppression,
            readiness = StrategicIdeaReadiness.Build,
            source = "hedgehog_containment_profile",
            confidence = 0.88,
            focusZone = Some("queenside"),
            factIds = List("structure_hedgehog", "hedgehog_containment_profile")
          )
        },
        Option.when(
          structureIs(semantic, StructureId.Hedgehog) &&
            side == "white" &&
            semantic.board.exists(board =>
              pawnAt(board, Color.White, Square.C4) &&
                pawnAt(board, Color.Black, Square.A6) &&
                pawnAt(board, Color.Black, Square.B6) &&
                pawnAt(board, Color.Black, Square.D6)
            )
        ) {
          evidence(
            ownerSide = side,
            kind = StrategicIdeaKind.CounterplaySuppression,
            readiness = StrategicIdeaReadiness.Build,
            source = "hedgehog_break_denial_geometry",
            confidence = 0.92,
            focusFiles = List("b", "d"),
            focusZone = Some("queenside"),
            factIds = List("structure_hedgehog", "hedgehog_break_denial_geometry")
          )
        },
        Option.when(
          structureIs(semantic, StructureId.MaroczyBind) &&
            side == "white" &&
            (clampForSide(side, semantic) || mobilityClampForSide(side, semantic))
        ) {
          evidence(
            ownerSide = side,
            kind = StrategicIdeaKind.CounterplaySuppression,
            readiness = StrategicIdeaReadiness.Build,
            source = "maroczy_counterplay_suppression",
            confidence = 0.82,
            focusZone = Some("center"),
            factIds = List("structure_maroczy_bind", "maroczy_counterplay_suppression")
          )
        }
        ,
        Option.when(
          structureIs(semantic, StructureId.MaroczyBind) &&
            side == "white" &&
            semantic.board.exists(board =>
              pawnAt(board, Color.White, Square.C4) &&
                pawnAt(board, Color.White, Square.E4) &&
                pawnAt(board, Color.Black, Square.C6) &&
                pawnAt(board, Color.Black, Square.D6)
            )
        ) {
          evidence(
            ownerSide = side,
            kind = StrategicIdeaKind.CounterplaySuppression,
            readiness = StrategicIdeaReadiness.Build,
            source = "maroczy_break_denial_geometry",
            confidence = 0.88,
            focusFiles = List("c", "d"),
            focusZone = Some("center"),
            factIds = List("structure_maroczy_bind", "maroczy_break_denial_geometry")
          )
        }
      ).flatten

    val planBridge =
      topPlansFor(side, semantic).flatMap { plan =>
        Option.when(
          structureIs(semantic, StructureId.Hedgehog) &&
            (plan.plan.id == PlanId.Prophylaxis || plan.plan.id == PlanId.SpaceAdvantage)
        ) {
          evidence(
            ownerSide = side,
            kind = StrategicIdeaKind.CounterplaySuppression,
            readiness = StrategicIdeaReadiness.Build,
            source = "plan_match_counterplay_suppression",
            confidence = 0.80 + math.min(0.04, plan.score * 0.06),
            factIds = List("plan_match_counterplay_suppression", s"plan_${plan.plan.id.toString.toLowerCase}")
          )
        }
      }

    preventedEvidence ++ counterBreakBridge ++ threatBridge ++ structureBridge ++ planBridge

  private def mergeEvidence(evidence: List[StrategicIdeaEvidence]): List[Candidate] =
    evidence
      .groupBy(_.signature)
      .values
      .map { grouped =>
        val best = grouped.maxBy(_.confidence)
        val stackCap =
          best.kind match
            case StrategicIdeaKind.TargetFixing                   => 0.10
            case StrategicIdeaKind.FavorableTradeOrTransformation => 0.12
            case _                                                => 0.18
        val stackIncrement =
          best.kind match
            case StrategicIdeaKind.TargetFixing                   => 0.03
            case StrategicIdeaKind.FavorableTradeOrTransformation => 0.04
            case _                                                => 0.05
        val score = (best.confidence + math.min(stackCap, (grouped.size - 1) * stackIncrement)).min(0.98)
        Candidate(
          ownerSide = best.ownerSide,
          kind = best.kind,
          group = best.group,
          readiness = mergeReadiness(grouped.map(_.readiness)),
          focusSquares = grouped.flatMap(_.focusSquares).distinct.take(4),
          focusFiles = grouped.flatMap(_.focusFiles).distinct.take(2),
          focusDiagonals = grouped.flatMap(_.focusDiagonals).distinct.take(2),
          focusZone = mostCommon(grouped.flatMap(_.focusZone)),
          beneficiaryPieces = grouped.flatMap(_.beneficiaryPieces).distinct.take(4),
          score = score,
          evidenceRefs = (grouped.map(ev => s"source:${ev.source}") ++ grouped.flatMap(_.factIds)).distinct.take(6)
        )
      }
      .toList
      .sortBy(candidate => (-candidate.score, candidate.kind))

  private def enrichDigest(
      digest: Option[NarrativeSignalDigest],
      ideas: List[StrategyIdeaSignal]
  ): Option[NarrativeSignalDigest] =
    val base = digest.getOrElse(NarrativeSignalDigest())
    val dominant = ideas.headOption
    val secondary = ideas.drop(1).headOption
    Option.when(dominant.isDefined || digest.isDefined)(
      base.copy(
        dominantIdeaKind = dominant.map(_.kind),
        dominantIdeaGroup = dominant.map(_.group),
        dominantIdeaReadiness = dominant.map(_.readiness),
        dominantIdeaFocus = dominant.map(focusSummary),
        secondaryIdeaKind = secondary.map(_.kind),
        secondaryIdeaGroup = secondary.map(_.group),
        secondaryIdeaFocus = secondary.map(focusSummary)
      )
    )

  private def enrichLongTermFocus(
      current: List[String],
      ideas: List[StrategyIdeaSignal],
      targets: List[StrategyDirectionalTarget]
  ): List[String] =
    val ideaLines =
      ideas.zipWithIndex.map { case (idea, idx) =>
        val prefix = if idx == 0 then "dominant idea" else "secondary idea"
        s"$prefix: ${humanizedKind(idea.kind)}${withFocusSuffix(focusSummary(idea))}"
      }
    val targetLines =
      targets.take(2).map(target => s"objective: work toward making ${target.targetSquare} available for the ${pieceName(target.piece)}")
    (ideaLines ++ targetLines ++ current).map(_.trim).filter(_.nonEmpty).distinct.take(6)

  private def enrichEvidence(
      current: List[String],
      ideas: List[StrategyIdeaSignal],
      targets: List[StrategyDirectionalTarget]
  ): List[String] =
    val ideaEvidence = ideas.map(idea => s"idea:${idea.kind}:${focusSummary(idea)}")
    val targetEvidence =
      targets.map(target => s"directional_target:${target.ownerSide}:${target.piece}:${target.targetSquare}:${target.readiness}")
    (current ++ ideaEvidence ++ targetEvidence).map(_.trim).filter(_.nonEmpty).distinct.take(12)

  private def evidence(
      ownerSide: String,
      kind: String,
      readiness: String,
      source: String,
      confidence: Double,
      focusSquares: List[String] = Nil,
      focusFiles: List[String] = Nil,
      focusDiagonals: List[String] = Nil,
      focusZone: Option[String] = None,
      beneficiaryPieces: List[String] = Nil,
      factIds: List[String] = Nil
  ): StrategicIdeaEvidence =
    StrategicIdeaEvidence(
      ownerSide = ownerSide,
      kind = kind,
      group = groupForKind(kind),
      readiness = readiness,
      source = source,
      confidence = confidence.max(0.0).min(0.98),
      focusSquares = focusSquares.distinct.filter(_.nonEmpty),
      focusFiles = focusFiles.distinct.filter(_.nonEmpty),
      focusDiagonals = focusDiagonals.distinct.filter(_.nonEmpty),
      focusZone = focusZone.map(_.trim).filter(_.nonEmpty),
      beneficiaryPieces = beneficiaryPieces.distinct.filter(_.nonEmpty),
      factIds = factIds.distinct.filter(_.nonEmpty)
    )

  private def mergeReadiness(readinessValues: Iterable[String]): String =
    if readinessValues.exists(_ == StrategicIdeaReadiness.Ready) then StrategicIdeaReadiness.Ready
    else if readinessValues.exists(_ == StrategicIdeaReadiness.Build) then StrategicIdeaReadiness.Build
    else if readinessValues.exists(_ == StrategicIdeaReadiness.Premature) then StrategicIdeaReadiness.Premature
    else if readinessValues.exists(_ == StrategicIdeaReadiness.Blocked) then StrategicIdeaReadiness.Blocked
    else StrategicIdeaReadiness.Build

  private def ideaReadinessFromDirectionalTarget(readiness: String): String =
    readiness match
      case DirectionalTargetReadiness.Build     => StrategicIdeaReadiness.Build
      case DirectionalTargetReadiness.Contested => StrategicIdeaReadiness.Build
      case DirectionalTargetReadiness.Premature => StrategicIdeaReadiness.Premature
      case DirectionalTargetReadiness.Blocked   => StrategicIdeaReadiness.Blocked
      case _                                    => StrategicIdeaReadiness.Build

  private def ideaReadinessFromRoute(surfaceMode: String): String =
    if surfaceMode == RouteSurfaceMode.Exact then StrategicIdeaReadiness.Ready else StrategicIdeaReadiness.Build

  private def readinessBonus(readiness: String): Double =
    readiness match
      case DirectionalTargetReadiness.Build     => 0.04
      case DirectionalTargetReadiness.Contested => 0.00
      case DirectionalTargetReadiness.Premature => -0.02
      case DirectionalTargetReadiness.Blocked   => -0.05
      case _                                    => 0.0

  private def breakImpactBonus(impact: String): Double =
    impact.trim.toLowerCase match
      case "high"   => 0.08
      case "medium" => 0.04
      case _        => 0.0

  private def breakImpactBonusFromInt(impact: Int): Double =
    if impact >= 180 then 0.08
    else if impact >= 100 then 0.05
    else if impact >= 40 then 0.02
    else 0.0

  private def prophylaxisThreatBonus(prevented: PreventedPlan): Double =
    (if prevented.preventedThreatType.isDefined then 0.04 else 0.0) +
      (if prevented.mobilityDelta < 0 then 0.02 else 0.0) +
      (if prevented.defensiveSufficiency.exists(_ >= 80) then 0.02 else 0.0)

  private def counterplaySuppressionBonus(prevented: PreventedPlan): Double =
    (if prevented.breakNeutralized.isDefined then 0.06 else 0.0) +
      (if prevented.counterplayScoreDrop >= 140 then 0.06 else if prevented.counterplayScoreDrop >= 100 then 0.03 else 0.0) +
      (if prevented.deniedSquares.size >= 2 then 0.03 else 0.0) +
      (if prevented.deniedResourceClass.contains("break") then 0.03 else 0.0) +
      (if prevented.breakNeutralizationStrength.exists(_ >= 80) then 0.03 else 0.0)

  private def moveRefSupportBonus(
      side: String,
      ref: StrategyPieceMoveRef,
      semantic: StrategicIdeaSemanticContext
  ): Double =
    val materialBonus =
      semantic.positionFeatures.fold(0.0)(features =>
        if materialEdgeFor(side, features) >= 150 then 0.08
        else if materialEdgeFor(side, features) >= 80 then 0.04
        else 0.0
      )
    val badBishopBonus =
      if ownBadBishop(side, semantic) && ref.piece == "B" then 0.06 else 0.0
    materialBonus + badBishopBonus

  private def favorableTradeContext(
      side: String,
      ref: StrategyPieceMoveRef,
      semantic: StrategicIdeaSemanticContext,
      hasStructuredTradeSignal: Boolean
  ): Boolean =
    hasStructuredTradeSignal ||
      hasFavorableClassificationWindow(side, semantic) ||
      semantic.positionFeatures.exists(features => materialEdgeFor(side, features) >= 80) ||
      (ownBadBishop(side, semantic) && ref.piece == "B")

  private def isPreventiveWithoutCounterplaySuppression(prevented: PreventedPlan): Boolean =
    (
      prevented.deniedSquares.nonEmpty ||
        prevented.preventedThreatType.isDefined ||
        prevented.mobilityDelta < 0 ||
        prevented.counterplayScoreDrop > 0 ||
        prevented.deniedResourceClass.isDefined ||
        prevented.defensiveSufficiency.exists(_ > 0)
    ) &&
      !isCounterplaySuppression(prevented)

  private def isCounterplaySuppression(prevented: PreventedPlan): Boolean =
    prevented.breakNeutralized.isDefined ||
      prevented.deniedResourceClass.contains("break") ||
      prevented.deniedEntryScope.exists(scope => scope == "file" || scope == "sector") ||
      prevented.breakNeutralizationStrength.exists(_ >= 60) ||
      prevented.counterplayScoreDrop >= 100 ||
      prevented.deniedSquares.size >= 2 ||
      prevented.mobilityDelta <= -2

  private def topPlansFor(side: String, semantic: StrategicIdeaSemanticContext): List[PlanMatch] =
    semantic.plans
      .filter(plan => matchesSide(plan.plan.color, side))
      .sortBy(plan => -plan.score)

  private def structureIs(semantic: StrategicIdeaSemanticContext, structureId: StructureId): Boolean =
    semantic.structureProfile.exists(_.primary == structureId)

  private def clampForSide(side: String, semantic: StrategicIdeaSemanticContext): Boolean =
    semantic.strategicState.exists(colorComplexClampFor(side, _))

  private def mobilityClampForSide(side: String, semantic: StrategicIdeaSemanticContext): Boolean =
    semantic.positionFeatures.exists(features =>
      lowMobilityPiecesFor(opponentSide(side), features) > lowMobilityPiecesFor(side, features) + 1
    )

  private def hasBishopPinWatch(side: String, semantic: StrategicIdeaSemanticContext): Boolean =
    semantic.board.exists { board =>
      if side == "white" then
        hasPiece(board, Color.White, Square.F3, Knight) &&
        hasPiece(board, Color.Black, Square.C8, Bishop) &&
        diagonalClear(board, Square.C8, Square.G4)
      else
        hasPiece(board, Color.Black, Square.F6, Knight) &&
        hasPiece(board, Color.White, Square.C1, Bishop) &&
        diagonalClear(board, Square.C1, Square.G5)
    }

  private def hasQueensideClampWatch(side: String, semantic: StrategicIdeaSemanticContext): Boolean =
    semantic.board.exists { board =>
      side == "white" &&
      pawnAt(board, Color.White, Square.C4) &&
      pawnAt(board, Color.White, Square.D5) &&
      pawnAt(board, Color.White, Square.E4) &&
      pawnAt(board, Color.Black, Square.D6) &&
      pawnAt(board, Color.Black, Square.E5) &&
      pawnAt(board, Color.Black, Square.G6) &&
      pawnAt(board, Color.Black, Square.B7)
    }

  private def hasOppositeSideStormAttack(side: String, semantic: StrategicIdeaSemanticContext): Boolean =
    semantic.board.exists { board =>
      val ourKing = board.kingPosOf(sideColor(side))
      val theirKing = board.kingPosOf(sideColor(opponentSide(side)))
      side match
        case "white" =>
          ourKing.exists(king => king.file.value <= File.C.value) &&
          theirKing.exists(king => king.file.value >= File.G.value) &&
          (
            pawnAt(board, Color.White, Square.H4) ||
              pawnAt(board, Color.White, Square.H5) ||
              pawnAt(board, Color.White, Square.G4) ||
              pawnAt(board, Color.White, Square.G5)
          )
        case _ =>
          ourKing.exists(king => king.file.value >= File.F.value) &&
          theirKing.exists(king => king.file.value <= File.C.value) &&
          (
            pawnAt(board, Color.Black, Square.H5) ||
              pawnAt(board, Color.Black, Square.H4) ||
              pawnAt(board, Color.Black, Square.G5) ||
              pawnAt(board, Color.Black, Square.G4)
          )
    }

  private def lineAccessFacts(
      side: String,
      endpoint: Square,
      semantic: StrategicIdeaSemanticContext
  ): Option[(List[String], Option[String], List[String])] =
    semantic.board.flatMap { board =>
      val color = sideColor(side)
      val open = isOpenFile(board, endpoint.file)
      val semiOpen = isSemiOpenFileFor(board, endpoint.file, color)
      val seventh = isSeventhRankFor(side, endpoint)
      Option.when(open || semiOpen || seventh) {
        val files = Option.when(open || semiOpen)(List(fileToken(endpoint.file))).getOrElse(Nil)
        val facts =
          List(
            Option.when(open)(s"open_file_${fileToken(endpoint.file)}"),
            Option.when(semiOpen)(s"semi_open_file_${fileToken(endpoint.file)}"),
            Option.when(seventh)("seventh_rank_entry")
          ).flatten
        val zone =
          if seventh then Some("back rank")
          else zoneFromFileToken(fileToken(endpoint.file))
        (files, zone, facts)
      }
    }

  private def isOutpostSquareFor(
      side: String,
      square: Square,
      semantic: StrategicIdeaSemanticContext
  ): Boolean =
    semantic.positionalFeatures.exists {
      case PositionalTag.Outpost(tagSquare, color)      => tagSquare == square && matchesSide(color, side)
      case PositionalTag.StrongKnight(tagSquare, color) => tagSquare == square && matchesSide(color, side)
      case _                                            => false
    } || semantic.board.exists(board => isOutpostSquare(board, square, sideColor(side)))

  private def isNearEnemyKing(
      side: String,
      square: Square,
      semantic: StrategicIdeaSemanticContext
  ): Boolean =
    semantic.board.flatMap(_.kingPosOf(sideColor(opponentSide(side)))).exists(enemyKing => chebyshev(square, enemyKing) <= 2)

  private def groupForKind(kind: String): String =
    kind match
      case StrategicIdeaKind.PawnBreak | StrategicIdeaKind.SpaceGainOrRestriction | StrategicIdeaKind.TargetFixing =>
        StrategicIdeaGroup.StructuralChange
      case StrategicIdeaKind.LineOccupation | StrategicIdeaKind.OutpostCreationOrOccupation |
          StrategicIdeaKind.MinorPieceImbalanceExploitation =>
        StrategicIdeaGroup.PieceAndLineManagement
      case _ =>
        StrategicIdeaGroup.InteractionAndTransformation

  private def hasAlignmentReason(semantic: StrategicIdeaSemanticContext, code: String): Boolean =
    semantic.planAlignmentReasonCodes.contains(code)

  private def hasFavorableClassificationWindow(
      side: String,
      semantic: StrategicIdeaSemanticContext
  ): Boolean =
    semantic.classification.exists(classification =>
      semantic.positionFeatures.exists(features =>
        materialEdgeFor(side, features) >= 80 &&
          (classification.simplifyBias.shouldSimplify || classification.taskMode.isConvertMode)
      )
    )

  private def bishopCountFor(side: String, features: PositionFeatures): Int =
    if side == "white" then features.imbalance.whiteBishops else features.imbalance.blackBishops

  private def knightCountFor(side: String, features: PositionFeatures): Int =
    if side == "white" then features.imbalance.whiteKnights else features.imbalance.blackKnights

  private def bishopPairFor(side: String, features: PositionFeatures): Boolean =
    if side == "white" then features.imbalance.whiteBishopPair else features.imbalance.blackBishopPair

  private def normalizeSquareKeys(keys: List[String]): List[String] =
    keys.flatMap(squareFromKey).map(_.key).distinct

  private def hasPiece(board: Board, color: Color, square: Square, role: Role): Boolean =
    board.pieceAt(square).exists(piece => piece.color == color && piece.role == role)

  private def pawnAt(board: Board, color: Color, square: Square): Boolean =
    hasPiece(board, color, square, Pawn)

  private def diagonalClear(board: Board, from: Square, to: Square): Boolean =
    val fileStep = math.signum(to.file.value - from.file.value)
    val rankStep = math.signum(to.rank.value - from.rank.value)
    val fileDiff = (to.file.value - from.file.value).abs
    val rankDiff = (to.rank.value - from.rank.value).abs
    if fileDiff != rankDiff || fileDiff == 0 then false
    else
      (1 until fileDiff).forall { offset =>
        Square
          .at(from.file.value + offset * fileStep, from.rank.value + offset * rankStep)
          .forall(board.pieceAt(_).isEmpty)
      }

  private def zoneFromSquareKeys(keys: List[String]): Option[String] =
    zoneFromSquares(keys.flatMap(squareFromKey))

  private def zoneFromSquares(squares: List[Square]): Option[String] =
    mostCommon(squares.flatMap(zoneFromSquare))

  private def normalizeFactToken(value: String): String =
    Option(value)
      .map(_.trim.toLowerCase.replaceAll("[^a-z0-9]+", "_").stripPrefix("_").stripSuffix("_"))
      .filter(_.nonEmpty)
      .getOrElse("unknown")

  private def weakComplexConfidence(weakness: WeakComplex): Double =
    weakness.cause.trim.toLowerCase match
      case "backward pawn" => 0.76
      case "hanging pawns" => 0.72
      case "doubled pawns" => 0.68
      case "holes"         => if weakness.isOutpost then 0.64 else 0.60
      case _               => 0.66

  private def threatSquares(threats: ThreatAnalysis): List[String] =
    threats.threats.flatMap(_.attackSquares).flatMap(squareFromKey).map(_.key).distinct.take(3)

  private def threatFocusZone(threats: ThreatAnalysis): Option[String] =
    zoneFromSquareKeys(threats.threats.flatMap(_.attackSquares))

  private def threatDefenseBonus(threats: ThreatAnalysis): Double =
    (if threats.resourceAvailable then 0.03 else 0.0) +
      (if threats.maxLossIfIgnored >= 120 then 0.03 else if threats.maxLossIfIgnored >= 60 then 0.01 else 0.0)

  private def threatSuppressionBonus(threats: ThreatAnalysis): Double =
    (if threats.maxLossIfIgnored >= 250 then 0.06 else if threats.maxLossIfIgnored >= 180 then 0.03 else 0.0) +
      (if threats.threats.exists(_.kind == ThreatKind.Mate) then 0.04 else 0.0)

  private def isThreatDrivenProphylaxis(threats: ThreatAnalysis): Boolean =
    threats.prophylaxisNeeded &&
      !threats.immediateThreat &&
      threats.resourceAvailable &&
      !threats.counterThreatBetter &&
      (threats.strategicThreat || threats.defense.prophylaxisNeeded) &&
      threats.maxLossIfIgnored > 0 &&
      threats.maxLossIfIgnored < 250

  private def isThreatDrivenCounterplaySuppression(
      threats: ThreatAnalysis,
      opponentPawnAnalysis: Option[lila.llm.analysis.L3.PawnPlayAnalysis],
      preventedPlans: List[PreventedPlan]
  ): Boolean =
    threats.maxLossIfIgnored >= 120 &&
      !threats.threatIgnorable &&
      (
        threats.strategicThreat ||
          opponentPawnAnalysis.exists(_.counterBreak) ||
          preventedPlans.exists(isCounterplaySuppression)
      )

  private def preventsCounterBreak(
      prevented: PreventedPlan,
      opponentPawn: lila.llm.analysis.L3.PawnPlayAnalysis
  ): Boolean =
    opponentPawn.counterBreak &&
      (
        prevented.breakNeutralized.flatMap(normalizeFileToken) == opponentPawn.breakFile.flatMap(normalizeFileToken) ||
          prevented.deniedResourceClass.contains("break") ||
          prevented.breakNeutralizationStrength.exists(_ >= 60) ||
          prevented.counterplayScoreDrop >= 100
      )

  private def isKingAttackThreatProfile(
      threats: ThreatAnalysis,
      side: String,
      semantic: StrategicIdeaSemanticContext
  ): Boolean =
    threats.threats.exists(_.kind == ThreatKind.Mate) ||
      threats.primaryDriver == "mate_threat" ||
      (
        threats.maxLossIfIgnored >= 180 &&
          threatSquares(threats).flatMap(squareFromKey).exists(isNearEnemyKing(side, _, semantic))
      )

  private def checkTypeBonus(checkType: Motif.CheckType): Double =
    checkType match
      case Motif.CheckType.Mate       => 0.10
      case Motif.CheckType.Double     => 0.06
      case Motif.CheckType.Discovered => 0.04
      case Motif.CheckType.Smothered  => 0.08
      case _                          => 0.02

  private def mostCommon(values: List[String]): Option[String] =
    values.groupBy(identity).toList.sortBy { case (value, grouped) => (-grouped.size, value) }.headOption.map(_._1)

  private def focusSummary(
      focusSquares: List[String],
      focusFiles: List[String],
      focusDiagonals: List[String],
      focusZone: Option[String]
  ): String =
    val parts =
      List(
        Option.when(focusSquares.nonEmpty)(focusSquares.mkString(", ")),
        Option.when(focusFiles.nonEmpty)(focusFiles.map(_ + "-file").mkString(", ")),
        Option.when(focusDiagonals.nonEmpty)(focusDiagonals.mkString(", ")),
        focusZone
      ).flatten
    parts.headOption.getOrElse("the key sector")

  private def withFocusSuffix(focus: String): String =
    Option(focus).map(_.trim).filter(_.nonEmpty).map(v => s" around $v").getOrElse("")

  private def pieceName(code: String): String =
    code match
      case "N" => "knight"
      case "B" => "bishop"
      case "R" => "rook"
      case "Q" => "queen"
      case "K" => "king"
      case "P" => "pawn"
      case _   => "piece"

  private def roleToken(role: Role): String =
    role match
      case Knight => "N"
      case Bishop => "B"
      case Rook   => "R"
      case Queen  => "Q"
      case Pawn   => "P"
      case _      => "K"

  private def normalizeFileToken(value: String): Option[String] =
    Option(value)
      .map(_.trim.toLowerCase)
      .filter(_.nonEmpty)
      .flatMap(raw =>
        raw.headOption
          .filter(ch => ch >= 'a' && ch <= 'h')
          .map(_.toString)
      )

  private def zoneFromFileToken(file: String): Option[String] =
    file.trim.toLowerCase.headOption.flatMap {
      case ch if ch <= 'c'                => Some("queenside")
      case ch if ch >= 'f'                => Some("kingside")
      case ch if ch == 'd' || ch == 'e'   => Some("center")
      case _                              => None
    }

  private def fileFromToken(token: String): Option[File] =
    token.headOption.flatMap(ch => File.all.find(_.char == ch))

  private def fileToken(file: File): String = file.char.toString

  private def squareFromKey(key: String): Option[Square] =
    Square.all.find(_.key == Option(key).map(_.trim.toLowerCase).getOrElse(""))

  private def sideColor(side: String): Color =
    if side == "white" then Color.White else Color.Black

  private def opponentSide(side: String): String =
    if side == "white" then "black" else "white"

  private def matchesSide(color: Color, side: String): Boolean =
    color.white == (side == "white")

  private def materialEdgeFor(side: String, features: PositionFeatures): Int =
    if side == "white" then features.materialPhase.materialDiff else -features.materialPhase.materialDiff

  private def spaceDiffFor(side: String, features: PositionFeatures): Int =
    if side == "white" then features.centralSpace.spaceDiff else -features.centralSpace.spaceDiff

  private def lowMobilityPiecesFor(side: String, features: PositionFeatures): Int =
    if side == "white" then features.activity.whiteLowMobilityPieces else features.activity.blackLowMobilityPieces

  private def openFilesCount(features: PositionFeatures): Int = features.lineControl.openFilesCount

  private def semiOpenFilesFor(side: String, features: PositionFeatures): Int =
    if side == "white" then features.lineControl.whiteSemiOpenFiles else features.lineControl.blackSemiOpenFiles

  private def attackersCountFor(side: String, features: PositionFeatures): Int =
    if side == "white" then features.kingSafety.whiteAttackersCount else features.kingSafety.blackAttackersCount

  private def enemyKingRingAttackedFor(side: String, features: PositionFeatures): Int =
    if side == "white" then features.kingSafety.blackKingRingAttacked else features.kingSafety.whiteKingRingAttacked

  private def enemyKingExposedFilesFor(side: String, features: PositionFeatures): Int =
    if side == "white" then features.kingSafety.blackKingExposedFiles else features.kingSafety.whiteKingExposedFiles

  private def colorComplexClampFor(side: String, state: StrategicStateFeatures): Boolean =
    if side == "white" then state.whiteColorComplexClamp else state.blackColorComplexClamp

  private def hookCreationChanceFor(side: String, state: StrategicStateFeatures): Boolean =
    if side == "white" then state.whiteHookCreationChance else state.blackHookCreationChance

  private def rookPawnMarchReadyFor(side: String, state: StrategicStateFeatures): Boolean =
    if side == "white" then state.whiteRookPawnMarchReady else state.blackRookPawnMarchReady

  private def entrenchedPiecesFor(side: String, state: StrategicStateFeatures): Int =
    if side == "white" then state.whiteEntrenchedPieces else state.blackEntrenchedPieces

  private def ownBadBishop(side: String, semantic: StrategicIdeaSemanticContext): Boolean =
    semantic.positionalFeatures.exists {
      case PositionalTag.BadBishop(color) => matchesSide(color, side)
      case _                              => false
    } || semantic.board.exists { board =>
      semantic.pieceActivity.exists(activity =>
        activity.isBadBishop && board.colorAt(activity.square).exists(color => matchesSide(color, side))
      )
    }

  private def isMajorPiece(piece: String): Boolean =
    piece == "R" || piece == "Q"

  private def isMinorPiece(piece: String): Boolean =
    piece == "N" || piece == "B"

  private def isSeventhRankFor(side: String, square: Square): Boolean =
    if side == "white" then square.rank == Rank.Seventh else square.rank == Rank.Second

  private def isOutpostSquare(board: Board, square: Square, color: Color): Boolean =
    val supportedByPawn = board.attackers(square, color).intersects(board.byPiece(color, Pawn))
    val attackedByEnemyPawn = board.attackers(square, !color).intersects(board.byPiece(!color, Pawn))
    supportedByPawn && !attackedByEnemyPawn

  private def isOpenFile(board: Board, file: File): Boolean =
    (board.pawns & _root_.chess.Bitboard.file(file)).isEmpty

  private def isSemiOpenFileFor(board: Board, file: File, color: Color): Boolean =
    val mask = _root_.chess.Bitboard.file(file)
    val ours = board.pawns & board.byColor(color) & mask
    val theirs = board.pawns & board.byColor(!color) & mask
    ours.isEmpty && theirs.nonEmpty

  private def fileHasBothColorsPawns(board: Board, file: File): Boolean =
    val mask = _root_.chess.Bitboard.file(file)
    (board.pawns & board.white & mask).nonEmpty && (board.pawns & board.black & mask).nonEmpty

  private def chebyshev(a: Square, b: Square): Int =
    math.max((a.file.value - b.file.value).abs, (a.rank.value - b.rank.value).abs)

  private def zoneFromSquare(square: Square): Option[String] =
    if square.file.value <= File.C.value then Some("queenside")
    else if square.file.value >= File.F.value then Some("kingside")
    else Some("center")

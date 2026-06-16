package lila.commentary.analysis

import lila.commentary.*
import lila.commentary.model.{ FactScope, NarrativeContext, TargetSquare }
import lila.commentary.analysis.claim.{ ClaimAuthorityResolver, OpeningFamilyClaimResolver }
import lila.commentary.analysis.semantic.{ RelationObservationCatalog, RelationObservationDescriptor, RelationSurfaceRowKind }
import lila.commentary.analysis.semantic.StrategicObservationIds.{ ProofFamilyId, ProofSourceId }
import lila.commentary.analysis.structure.{ PawnStructureTargets, WeaknessTargetProfile }
import lila.commentary.analysis.PlanEvidenceEvaluator.{ EvaluatedPlan, UserFacingPlanEligibility }
import lila.commentary.analysis.PlanTaxonomy.{ PlanKind, PlanTheme }
import chess.format.{ Fen, Uci }
import chess.variant.Standard

object MoveReviewPlayerPayloadBuilder:

  private val Schema = "chesstory.move_review.player_surface.v2"
  private val MinDecisionSurfaceGapCp = 35
  private val ClearDecisionSurfaceGapCp = 60
  private val ExactComparativeSource = "exact"
  private val MinWeaknessTargetOutcomePlies = 5
  private val SquareToken = """[a-h][1-8]""".r
  private val FileCharPattern = "^[a-h]$".r
  private[analysis] val ChessSquarePattern = "^[a-h][1-8]$".r
  private[analysis] val ChessSquareRangePattern = "^[a-h][1-8]-[a-h][1-8]$".r
  private[analysis] val LabelSquareListPattern = """^\.{0,3}[a-h][1-8](?:,[a-h][1-8])*$""".r
  private[analysis] val LabelSquareRangePattern = """^\.{0,3}[a-h][1-8]-[a-h][1-8]$""".r
  private val ConnectedRooksPattern = """The checked line connects the rooks on the (?:first|second|third|fourth|fifth|sixth|seventh|eighth) rank[.]""".r
  private val TechnicalConversionPattern = """The checked line keeps the conversion route intact after .+[.]""".r
  private val RestrainedPattern = """The checked line keeps .+ restrained[.]""".r
  private val UnavailablePattern = """The checked line keeps .+ unavailable as a counterplay resource[.]""".r
  private val BreakShutPattern = """The checked line keeps the \.{0,3}[a-h][1-8](?:-[a-h][1-8]|(?:,[a-h][1-8])*) break shut while keeping \.{0,3}[a-h][1-8](?:-[a-h][1-8]|(?:,[a-h][1-8])*) unavailable[.]""".r
  private val RouteDenialPattern = """The checked line keeps [a-h][1-8] closed, takes the [a-h]-file away, and cuts off the [a-h][1-8] reroute[.]""".r
  private val BackRankMatePattern = """The checked line ends in back-rank mate on [a-h][1-8] after .+[.]""".r
  private val MateNetPattern = """The checked line ends in mate net on [a-h][1-8] after .+[.]""".r
  private val GreekGiftPattern = """The checked line starts a Greek gift sacrifice with the bishop on [a-h][1-8][.]""".r
  private val BatteryPressurePattern = """The checked line forms a [a-z]+-[a-z]+ battery on the (?:diagonal|file) toward [a-h][1-8][.]""".r
  private val HookCreationPattern = """The checked rook-pawn move creates a flank hook on [a-h][1-8][.]""".r
  private val RookPawnMarchPattern = """The checked line advances the rook pawn to [a-h][1-8] for flank space[.]""".r
  private val RookLiftPattern = """The checked line lifts the rook to [a-h][1-8] as attacking infrastructure[.]""".r

  private val MaxStrategicRelationRows = 4
  private val MaxCompensationRows = 2
  private[commentary] val MoveReviewLedgerLineSources = Set("probe", "decision_compare", "variation", "authoring")
  private[analysis] val PracticalPlanAuthority =
    Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan))

  def decisionComparisonSurface(
      ctx: NarrativeContext,
      refs: Option[MoveReviewRefs]
  ): Option[MoveReviewPlayerDecisionComparison] =
    decisionComparisonSurface(
      comparison = DecisionComparisonBuilder.digest(ctx, refs),
      lineEvidence = LineConsequenceEvaluator.surfaceCandidate(ctx, refs)
    )

  private[commentary] def decisionComparisonSurface(
      inputs: QuestionPlannerInputs,
      localFact: Option[MoveReviewLocalFact.Admission],
      ctx: NarrativeContext,
      refs: Option[MoveReviewRefs]
  ): Option[MoveReviewPlayerDecisionComparison] =
    roleAwareDecisionComparisonSurface(inputs.decisionComparison, localFact)
      .orElse(pieceRelocationDecisionComparisonSurface(inputs.decisionComparison, localFact))
      .orElse(decisionComparisonSurface(ctx, refs))

  private[analysis] def roleAwareDecisionComparisonSurface(
      comparison: Option[DecisionComparison],
      localFact: Option[MoveReviewLocalFact.Admission]
  ): Option[MoveReviewPlayerDecisionComparison] =
    for
      enriched <- comparison
      if roleAwareDecisionSurfaceAdmitted(enriched, localFact)
      branchEvidence <- enriched.roleAwareBranchEvidence
      secondary <- cleanOpt(enriched.comparativeConsequence)
      bestLine = branchEvidence.engineBest
      playedLine = branchEvidence.played
      if bestLine.narrativeReady && playedLine.narrativeReady
    yield MoveReviewPlayerDecisionComparison(
      kicker = "Decision point",
      gapLabel = enriched.cpLossVsChosen.map(decisionGapLabel),
      chosenSan = cleanMove(enriched.chosenMove),
      engineSan = cleanMove(enriched.engineBestMove),
      comparedSan = cleanMove(enriched.comparedMove),
      secondaryText = Some(secondary),
      chosenMatchesBest = enriched.chosenMatchesBest,
      refSans = cleanLineList(bestLine.sanMoves.take(4) ++ playedLine.sanMoves.take(4))
    )

  private[analysis] def decisionComparisonSurface(
      comparison: Option[DecisionComparisonDigest],
      lineEvidence: Option[LineConsequenceEvidence]
  ): Option[MoveReviewPlayerDecisionComparison] =
    for
      digest <- comparison
      evidence <- lineEvidence
      if evidence.surfaceReady
      if hasComparableDecisionShape(digest, evidence)
      if hasDecisionSurfaceReason(digest)
      secondary <- safeDecisionSecondaryText(evidence)
    yield MoveReviewPlayerDecisionComparison(
      kicker = "Decision point",
      gapLabel = digest.cpLossVsChosen.map(decisionGapLabel),
      chosenSan = cleanMove(digest.chosenMove),
      engineSan = cleanMove(digest.engineBestMove),
      comparedSan = cleanMove(digest.comparedMove),
      secondaryText = Some(secondary),
      chosenMatchesBest = digest.chosenMatchesBest,
      refSans = cleanLineList(evidence.sanMoves.take(5))
    )

  private def roleAwareDecisionSurfaceAdmitted(
      comparison: DecisionComparison,
      localFact: Option[MoveReviewLocalFact.Admission]
  ): Boolean =
    comparison.comparativeSource.exists(_.trim == DecisionComparisonComparativeSupport.RoleAwareLineConsequenceSource) &&
      comparison.roleAwareBranchEvidence.nonEmpty &&
      localFact.exists(fact =>
        fact.family == MoveReviewLocalFact.Family.LineConsequence &&
          fact.authority == MoveReviewLocalFact.Authority.AlternativeComparison &&
          fact.producer == MoveReviewLocalFact.Producer.AlternativeComparison &&
          fact.lineBinding == MoveReviewLocalFact.LineBinding.PvCoupled
      )

  private[analysis] def pieceRelocationDecisionComparisonSurface(
      comparison: Option[DecisionComparison],
      localFact: Option[MoveReviewLocalFact.Admission]
  ): Option[MoveReviewPlayerDecisionComparison] =
    for
      enriched <- comparison
      if pieceRelocationDecisionSurfaceAdmitted(enriched, localFact)
      secondary <- cleanOpt(enriched.comparativeConsequence)
    yield MoveReviewPlayerDecisionComparison(
      kicker = "Decision point",
      gapLabel = enriched.cpLossVsChosen.map(decisionGapLabel),
      chosenSan = cleanMove(enriched.chosenMove),
      engineSan = cleanMove(enriched.engineBestMove),
      comparedSan = cleanMove(enriched.comparedMove),
      secondaryText = Some(secondary),
      chosenMatchesBest = enriched.chosenMatchesBest,
      refSans = cleanLineList(enriched.engineBestPv.take(4) ++ enriched.chosenMove.toList)
    )

  private def pieceRelocationDecisionSurfaceAdmitted(
      comparison: DecisionComparison,
      localFact: Option[MoveReviewLocalFact.Admission]
  ): Boolean =
    false

  def build(
      ctx: NarrativeContext,
      moveReviewExplanation: Option[MoveReviewExplanation],
      moveReviewLedger: Option[MoveReviewStrategicLedger],
      refs: Option[MoveReviewRefs],
      evaluatedPlans: List[EvaluatedPlan],
      authoringSurface: AuthoringEvidenceSurface,
      supportedLocalRows: List[MoveReviewPlayerSurfaceRow] = Nil,
      decisionComparisonSurface: Option[MoveReviewPlayerDecisionComparison] = None,
      strategyPack: Option[StrategyPack] = None,
      truthContract: Option[DecisiveTruthContract] = None,
      lineConsequence: Option[LineConsequenceEvidence] = None
  ): MoveReviewPlayerSurface =
    val knownSans = refs.toList.flatMap(_.variations.flatMap(_.moves.map(_.san))).map(normalizeSan).toSet
    val supportBlocked = MoveReviewSurfaceTruthVeto.truthContractSurfaceVeto(truthContract)
    val compensationBlocked = truthContract.exists(contract => !contract.compensationProseAllowed)
    val promotedPlans =
      if supportBlocked then Nil
      else evaluatedPlans.filter(PlanEvidenceEvaluator.isMainAdmittedPlan).sortBy(_.hypothesis.rank)
    val practicalStructureArc =
      if supportBlocked then None else StructurePlanArcBuilder.build(ctx).filter(StructurePlanArcBuilder.proseEligible)
    val practicalRows = if supportBlocked then Nil else practicalPlanRows(ctx, evaluatedPlans, practicalStructureArc)
    val openingRows = if supportBlocked then Nil else openingFamilyRow(ctx).toList
    val compensationRows = if supportBlocked || compensationBlocked then Nil else compensationAdvancedRows(strategyPack)
    val localRows = if supportBlocked then Nil else sanitizeRows(supportedLocalRows, knownSans)
    val relationRows =
      if supportBlocked then Nil
      else strategicRelationRows(strategyPack, MoveReviewSupportedLocalSurfaceRows.relationKindsForRows(localRows))
    val structuralIdeaRows =
      if supportBlocked then Nil
      else
        val structuralIdeas = strategyPack.toList.flatMap(_.strategicIdeas)
        val strategySide = strategyPack.map(_.sideToMove.trim.toLowerCase).filter(_.nonEmpty)
        val visibleRows = practicalRows ++ localRows
        val exactTargetSquaresAlreadyVisible =
          visibleRows.flatMap(exactTargetRowSquare).toSet
        val exactColorComplexesAlreadyVisible =
          visibleRows.flatMap(exactColorComplexRowComplex).toSet
        val exactOutpostSquaresAlreadyVisible =
          visibleRows.flatMap(exactOutpostRowSquare).toSet
        val exactSimplificationAlreadyVisible =
          visibleRows.exists(exactSimplificationRow)
        val exactConversionAlreadyVisible =
          visibleRows.exists(exactTechnicalConversionRow)
        val exactFileEntryFilesAlreadyVisible =
          visibleRows.flatMap(exactFileEntryRowFile).toSet
        val exactSeventhRankEntryRolesAlreadyVisible =
          visibleRows.flatMap(exactSeventhRankEntryRowRole).toSet
        val exactConnectedRooksAlreadyVisible =
          visibleRows.exists(exactConnectedRooksRow)
        val exactDoubledRooksFilesAlreadyVisible =
          visibleRows.flatMap(exactDoubledRooksRowFile).toSet
        val exactBishopPairAlreadyVisible =
          visibleRows.exists(exactBishopPairRow)
        val exactOppositeColorBishopsAlreadyVisible =
          visibleRows.exists(exactOppositeColorBishopsRow)
        val exactCounterplayAlreadyVisible =
          (practicalRows ++ localRows).exists(exactCounterplayRow)
        val prophylaxisAlreadyVisible =
          promotedPlans.exists(isProphylaxisPlan)
        val exactAttackAlreadyVisible =
          (practicalRows ++ localRows).exists(exactAttackRow)
        val exactBatteryPressureAlreadyVisible =
          (practicalRows ++ localRows).exists(exactBatteryPressureRow)
        val exactFlankAttackAlreadyVisible =
          (practicalRows ++ localRows).exists(exactFlankAttackRow)
        val exactRookLiftAlreadyVisible =
          (practicalRows ++ localRows).exists(exactRookLiftRow)
        val practicalTargetSquaresAlreadyVisible =
          evaluatedPlans
            .filter(PlanEvidenceEvaluator.isBoundedPracticalSupportPlan)
            .flatMap { plan =>
              val hints = practicalTargetHints(plan)
              if hints.nonEmpty then hints
              else if isWeaknessPlan(plan) then practicalCurrentTarget(ctx, Set.empty).map(_.targetSquare).toSet
              else Set.empty
            }
            .toSet
        val pressureRows = structuralIdeas
          .filter(_.kind == StrategicIdeaKind.TargetFixing)
          .flatMap { idea =>
            val refs = idea.evidenceRefs.map(_.trim.toLowerCase)
            val targetPressureFact =
              refs.exists(ref => ref == "target_pressure_semantic" || ref.startsWith("target_pressure_"))
            val carlsbadProfile =
              refs.contains("source:carlsbad_fixation_profile") &&
                targetPressureFact &&
                strategySide.forall(side => idea.ownerSide.equalsIgnoreCase(side))
            val minorityPressure =
              refs.contains("source:minority_attack_semantic") &&
                targetPressureFact &&
                strategySide.forall(side => idea.ownerSide.equalsIgnoreCase(side))
            val minoritySupportPressure =
              refs.contains("source:minority_attack_support") &&
                refs.exists(_.startsWith("minority_attack_support_")) &&
                idea.readiness == StrategicIdeaReadiness.Build &&
                strategySide.forall(side => idea.ownerSide.equalsIgnoreCase(side)) &&
                idea.confidence >= 0.56
            val targetSquares =
              idea.focusSquares.flatMap(validSquare).distinct.take(3).filterNot(exactTargetSquaresAlreadyVisible.contains)
            val weaknessPressureTargetSquares =
              targetSquares.filterNot(practicalTargetSquaresAlreadyVisible.contains)
            val pressureFiles = idea.focusFiles.map(_.trim.toLowerCase).filter(FileCharPattern.matches).distinct.take(2)
            val weakSquarePressure =
              refs.contains("source:enemy_weak_square") &&
                weaknessPressureTargetSquares.exists(square => refs.contains(s"enemy_weak_square_$square")) &&
                idea.readiness == StrategicIdeaReadiness.Ready &&
                strategySide.forall(side => idea.ownerSide.equalsIgnoreCase(side)) &&
                idea.confidence >= 0.74
            val weakComplexPressure =
              refs.contains("source:weak_complex_fixation") &&
                refs.exists(ref => ref.startsWith("weak_complex_") && ref != "weak_complex_fixation") &&
                weaknessPressureTargetSquares.nonEmpty &&
                strategySide.forall(side => idea.ownerSide.equalsIgnoreCase(side)) &&
                idea.confidence >= 0.72
            val doubledPawnPressureFile =
              pressureFiles.find(file => refs.contains(s"doubled_pawn_file_$file"))
            val doubledPawnPressure =
              refs.contains("source:doubled_pawn_pressure_motif") &&
                refs.contains("doubled_pawn_pressure_shape") &&
                doubledPawnPressureFile.nonEmpty &&
                idea.readiness == StrategicIdeaReadiness.Build &&
                strategySide.forall(side => idea.ownerSide.equalsIgnoreCase(side)) &&
                idea.confidence >= 0.70
            val compensationTargetPressure =
              refs.contains("source:compensation_target_fixation") &&
                refs.contains("compensation_target_fixation") &&
                refs.contains("material_deficit_compensation") &&
                strategySide.forall(side => idea.ownerSide.equalsIgnoreCase(side)) &&
                idea.confidence >= 0.78
            Option.when(
              (
                (carlsbadProfile || minorityPressure || minoritySupportPressure || weakSquarePressure || weakComplexPressure || compensationTargetPressure) &&
                  targetSquares.nonEmpty
              ) || (doubledPawnPressure && pressureFiles.nonEmpty)
            ) {
              val zone = cleanOpt(idea.focusZone).map(zone => s"${zone.toLowerCase} ").getOrElse("")
              val targetText =
                if weakSquarePressure || weakComplexPressure then weaknessPressureTargetSquares.mkString(", ")
                else targetSquares.mkString(", ")
              val text =
                if carlsbadProfile then s"The current pawn structure points ${zone}pressure toward $targetText."
                else if minorityPressure then s"The pawn-break signal adds ${zone}pressure toward $targetText."
                else if minoritySupportPressure then s"The minority-attack structure points ${zone}pressure toward $targetText."
                else if weakSquarePressure then s"The current weak-square map gives a practical pressure cue around $targetText."
                else if doubledPawnPressure then s"The doubled-pawn structure gives a practical pressure cue on the ${doubledPawnPressureFile.get}-file."
                else if compensationTargetPressure then s"The compensation structure keeps practical ${zone}pressure on $targetText."
                else s"The current pawn weaknesses give a practical pressure cue around $targetText."
              row("Practical pressure", text, tone = Some("practical")).map(_.copy(authority = PracticalPlanAuthority))
            }.flatten
          }
        val spaceRows = structuralIdeas
          .filter(_.kind == StrategicIdeaKind.SpaceGainOrRestriction)
          .flatMap { idea =>
            val refs = idea.evidenceRefs.map(_.trim.toLowerCase)
            val colorComplexTokenZones =
              List(
                Option.when(refs.contains("color_complex_dark"))("dark-square complex"),
                Option.when(refs.contains("color_complex_light"))("light-square complex")
              ).flatten.distinct
            val colorComplexClamp =
              refs.contains("source:color_complex_clamp") &&
                refs.contains("enemy_color_complex_weakness") &&
                colorComplexTokenZones.size == 1
            val focusSquares = idea.focusSquares.flatMap(validSquare).distinct.take(3)
            val focusZone = cleanOpt(idea.focusZone).map(_.toLowerCase)
            val colorComplexZone =
              colorComplexTokenZones match
                case zone :: Nil => Some(zone)
                case _           => None
            val exactColorComplexAlreadyVisible =
              colorComplexZone.exists(exactColorComplexesAlreadyVisible.contains)
            val focusFiles = idea.focusFiles.map(_.trim.toLowerCase).filter(FileCharPattern.matches).distinct
            val pawnChainFactZones =
              refs.flatMap { ref =>
                if ref.startsWith("pawn_chain_") && ref != "pawn_chain_space_shape" then
                  ref.stripPrefix("pawn_chain_").split("_").toList match
                    case base :: tip :: Nil =>
                      List(tip, base).flatMap { file =>
                        if FileCharPattern.matches(file) then
                          file.headOption.flatMap {
                            case ch if ch <= 'c' => Some("queenside")
                            case ch if ch >= 'f' => Some("kingside")
                            case _               => None
                          }
                        else None
                      }.headOption
                    case _ => None
                else None
              }.distinct
            val motifSpaceAdvantage =
              refs.contains("source:space_advantage_motif") &&
                refs.contains("space_advantage_motif_shape") &&
                refs.exists(ref =>
                  ref.stripPrefix("space_pawn_delta_").toIntOption.exists(_ >= 2)
                ) &&
                focusZone.contains("center") &&
                idea.readiness == StrategicIdeaReadiness.Ready &&
                strategySide.forall(side => idea.ownerSide.equalsIgnoreCase(side)) &&
                idea.confidence >= 0.72
            val centralPawnAdvanceFile =
              focusFiles.find(file => Set("c", "d", "e", "f").contains(file) && refs.contains(s"central_pawn_file_$file"))
            val centralPawnAdvance =
              refs.contains("source:central_pawn_advance_motif") &&
                refs.contains("central_pawn_advance_shape") &&
                centralPawnAdvanceFile.nonEmpty &&
                refs.exists(ref => ref.stripPrefix("central_pawn_to_rank_").toIntOption.exists(_ >= 4)) &&
                focusZone.contains("center") &&
                idea.readiness == StrategicIdeaReadiness.Build &&
                strategySide.forall(side => idea.ownerSide.equalsIgnoreCase(side)) &&
                idea.confidence >= 0.70
            val pawnChainSpace =
              refs.contains("source:pawn_chain_space_motif") &&
                refs.contains("pawn_chain_space_shape") &&
                refs.exists(ref => ref.startsWith("pawn_chain_") && ref != "pawn_chain_space_shape") &&
                idea.focusFiles.map(_.trim.toLowerCase).filter(FileCharPattern.matches).distinct.size >= 2 &&
                idea.readiness == StrategicIdeaReadiness.Build &&
                strategySide.forall(side => idea.ownerSide.equalsIgnoreCase(side)) &&
                idea.confidence >= 0.74
            val profileText =
            if colorComplexClamp && focusSquares.size >= 2 && idea.confidence >= 0.78 && colorComplexZone.nonEmpty &&
                strategySide.forall(side => idea.ownerSide.equalsIgnoreCase(side)) &&
                !exactColorComplexAlreadyVisible
              then
                Some(s"The current structure clamps the ${colorComplexZone.get} around ${focusSquares.mkString(", ")}.")
              else if motifSpaceAdvantage then Some("The current motif map gives a practical central-space cue.")
              else if centralPawnAdvance then
                Some(s"The central pawn advance gives a practical ${centralPawnAdvanceFile.get}-file space cue.")
              else if pawnChainSpace then
                val zone =
                  pawnChainFactZones match
                    case zone :: Nil if zone == "kingside" || zone == "queenside" => zone
                    case _                                                        => "flank"
                Some(s"The pawn chain gives a practical ${zone}-space cue.")
              else if refs.contains("source:maroczy_bind_profile") &&
                refs.contains("structure_maroczy_bind") &&
                idea.ownerSide.equalsIgnoreCase("white") &&
                strategySide.forall(side => idea.ownerSide.equalsIgnoreCase(side)) &&
                idea.confidence >= 0.86
              then Some("The current Maroczy bind gives a practical central-space grip.")
              else if refs.contains("source:iqp_central_presence") &&
                refs.contains("structure_iqp_white") &&
                refs.contains("iqp_central_presence_shape") &&
                idea.ownerSide.equalsIgnoreCase("white") &&
                strategySide.forall(side => idea.ownerSide.equalsIgnoreCase(side)) &&
                focusSquares.contains("d4") &&
                idea.confidence >= 0.82
              then Some("The current IQP structure gives a practical central-space cue around d4.")
              else if refs.contains("source:iqp_space_bridge") &&
                refs.contains("structure_iqp_white") &&
                idea.ownerSide.equalsIgnoreCase("white") &&
                focusZone.contains("center") &&
                idea.readiness == StrategicIdeaReadiness.Build &&
                strategySide.forall(side => idea.ownerSide.equalsIgnoreCase(side)) &&
                idea.confidence >= 0.84
              then Some("The current IQP structure gives a practical central-space cue.")
              else if refs.contains("source:locked_center_bind") &&
                refs.contains("structure_locked_center") &&
                focusZone.contains("center") &&
                strategySide.forall(side => idea.ownerSide.equalsIgnoreCase(side)) &&
                idea.confidence >= 0.70
              then Some("The locked center gives a practical central-space bind.")
              else if refs.contains("source:central_space_edge") &&
                refs.contains("central_space_edge_shape") &&
                focusZone.contains("center") &&
                idea.readiness == StrategicIdeaReadiness.Ready &&
                strategySide.forall(side => idea.ownerSide.equalsIgnoreCase(side)) &&
                idea.confidence >= 0.74
              then Some("The current position gives a practical central-space edge.")
              else if refs.contains("source:mobility_restriction") &&
                refs.contains("mobility_restriction_shape") &&
                focusZone.contains("center") &&
                strategySide.forall(side => idea.ownerSide.equalsIgnoreCase(side)) &&
                idea.confidence >= 0.72
              then Some("The current position gives a practical mobility bind.")
              else None
            profileText.flatMap(text =>
              row("Practical space", text, tone = Some("practical")).map(_.copy(authority = PracticalPlanAuthority))
            )
          }
        val breakRows = structuralIdeas
          .filter(_.kind == StrategicIdeaKind.PawnBreak)
          .flatMap { idea =>
            val refs = idea.evidenceRefs.map(_.trim.toLowerCase)
            val typedBreakReady =
              refs.contains("source:pawn_analysis_break_ready") ||
                refs.contains("source:pawn_play_break_ready")
            val file = idea.focusFiles.map(_.trim.toLowerCase).filter(FileCharPattern.matches).distinct.headOption
            val typedBreakReadyFile =
              idea.focusFiles
                .map(_.trim.toLowerCase)
                .filter(FileCharPattern.matches)
                .distinct
                .find(file => refs.contains(s"break_file_$file"))
                .orElse(file)
            val focusZone = cleanOpt(idea.focusZone).map(_.toLowerCase)
            val focusSquares = idea.focusSquares.flatMap(validSquare).distinct
            val frenchF6Seed =
              refs.contains("source:french_f6_break_seed") &&
                refs.contains("french_f6_break_seed_shape") &&
                refs.contains("white_e5_chain") &&
                refs.contains("black_f7_break_pawn") &&
                idea.ownerSide.equalsIgnoreCase("black") &&
                file.contains("f") &&
                focusSquares.contains("e5") &&
                focusSquares.contains("f6") &&
                idea.confidence >= 0.92
            val fileOpeningConsequenceFile =
              idea.focusFiles.map(_.trim.toLowerCase).filter(FileCharPattern.matches).distinct.find(file =>
                refs.contains(s"contested_file_$file")
              )
            val fileOpeningConsequence =
              refs.contains("source:file_opening_consequence") &&
                fileOpeningConsequenceFile.nonEmpty &&
                strategySide.forall(side => idea.ownerSide.equalsIgnoreCase(side)) &&
                idea.confidence >= 0.72
            val centralBreakTensionFiles =
              idea.focusFiles.map(_.trim.toLowerCase).filter(FileCharPattern.matches).distinct.filter(file =>
                Set("c", "d", "e", "f").contains(file)
              )
            val centralBreakTensionFile =
              centralBreakTensionFiles match
                case file :: Nil => Some(file)
                case _           => None
            val centralBreakTension =
              refs.contains("source:central_break_tension") &&
                focusZone.contains("center") &&
                centralBreakTensionFiles.nonEmpty &&
                (refs.contains("locked_center") || idea.readiness == StrategicIdeaReadiness.Ready) &&
                strategySide.forall(side => idea.ownerSide.equalsIgnoreCase(side)) &&
                idea.confidence >= 0.78
            val pawnBreakMotifFile =
              idea.focusFiles.map(_.trim.toLowerCase).filter(FileCharPattern.matches).distinct.find(file =>
                refs.contains(s"break_file_$file")
              )
            val pawnBreakMotif =
              refs.contains("source:pawn_break_motif") &&
                refs.contains("pawn_break_motif_shape") &&
                pawnBreakMotifFile.nonEmpty &&
                idea.readiness == StrategicIdeaReadiness.Build &&
                strategySide.forall(side => idea.ownerSide.equalsIgnoreCase(side)) &&
                idea.confidence >= 0.76
            val breakSurface =
              if typedBreakReady &&
                refs.exists(ref => ref == "pawn_analysis_break_ready_shape" || ref == "pawn_play_break_ready_shape") &&
                idea.readiness == StrategicIdeaReadiness.Ready &&
                idea.confidence >= 0.92 &&
                typedBreakReadyFile.nonEmpty
              then Some("Practical break" -> s"The current pawn structure gives a practical ${typedBreakReadyFile.get}-file break cue.")
              else if fileOpeningConsequence then Some("Practical break" -> s"The current pawn tension gives a practical ${fileOpeningConsequenceFile.get}-file opening cue.")
              else if frenchF6Seed then Some("Practical break" -> "The French Advance chain gives Black a practical ...f6 break cue.")
              else if centralBreakTension then
                centralBreakTensionFile
                  .map(file => "Practical break" -> s"The central tension gives a practical $file-file break cue.")
                  .orElse(Some("Practical break" -> "The central tension gives a practical central-break cue."))
              else if pawnBreakMotif then Some("Practical cue" -> s"The current file contact marks a practical ${pawnBreakMotifFile.get}-file break candidate.")
              else None
            breakSurface.flatMap { case (label, text) =>
              row(label, text, tone = Some("practical")).map(_.copy(authority = PracticalPlanAuthority))
            }
          }
        val restraintRows =
          structuralIdeas
            .filter(_.kind == StrategicIdeaKind.CounterplaySuppression)
            .flatMap { idea =>
            val refs = idea.evidenceRefs.map(_.trim.toLowerCase)
            val files = idea.focusFiles.map(_.trim.toLowerCase).filter(FileCharPattern.matches).distinct
            val singleFocusFile =
              files match
                case file :: Nil => Some(file)
                case _           => None
            val focusSquares = idea.focusSquares.flatMap(validSquare).distinct
            val zone = cleanOpt(idea.focusZone).map(_.toLowerCase)
            val blockadeSquare = focusSquares.find(square => refs.contains(s"blockade_square_$square"))
            val blockadedPawnSquare = focusSquares.find(square => refs.contains(s"blockaded_pawn_$square"))
            val hedgehogGeometry =
              !exactCounterplayAlreadyVisible &&
                refs.contains("source:hedgehog_break_denial_geometry") &&
                refs.contains("hedgehog_break_denial_shape") &&
                refs.contains("structure_hedgehog") &&
                idea.ownerSide.equalsIgnoreCase("white") &&
                files.contains("b") &&
                files.contains("d") &&
                zone.contains("queenside") &&
                idea.confidence >= 0.92
            val maroczyGeometry =
              !exactCounterplayAlreadyVisible &&
                refs.contains("source:maroczy_break_denial_geometry") &&
                refs.contains("maroczy_break_denial_shape") &&
                refs.contains("structure_maroczy_bind") &&
                idea.ownerSide.equalsIgnoreCase("white") &&
                files.contains("c") &&
                files.contains("d") &&
                zone.contains("center") &&
                idea.confidence >= 0.88
            val opponentCounterbreakDenial =
              !exactCounterplayAlreadyVisible &&
                refs.contains("source:opponent_counterbreak_denial") &&
                refs.contains("opponent_counter_break") &&
                files.nonEmpty &&
                idea.readiness == StrategicIdeaReadiness.Ready &&
                strategySide.forall(side => idea.ownerSide.equalsIgnoreCase(side)) &&
                idea.confidence >= 0.80
            val passerBlockade =
              refs.contains("source:passer_blockade_motif") &&
                refs.contains("passer_blockade_shape") &&
                blockadeSquare.nonEmpty &&
                blockadedPawnSquare.nonEmpty &&
                idea.readiness == StrategicIdeaReadiness.Ready &&
                strategySide.forall(side => idea.ownerSide.equalsIgnoreCase(side)) &&
                idea.confidence >= 0.76
            val counterplayBreakDenied =
              !exactCounterplayAlreadyVisible &&
                refs.contains("source:counterplay_suppression") &&
                refs.contains("counterplay_suppression_shape") &&
                refs.contains("counterplay_break_denial") &&
                refs.contains("break_neutralized") &&
                refs.contains("denied_break_resource") &&
                files.nonEmpty &&
                idea.readiness == StrategicIdeaReadiness.Ready &&
                strategySide.forall(side => idea.ownerSide.equalsIgnoreCase(side)) &&
                idea.confidence >= 0.82
            val compensationCounterplayDenied =
              !exactCounterplayAlreadyVisible &&
                refs.contains("source:compensation_counterplay_denial") &&
                refs.contains("material_deficit_compensation") &&
                refs.contains("break_neutralized") &&
                files.nonEmpty &&
                strategySide.forall(side => idea.ownerSide.equalsIgnoreCase(side)) &&
                idea.confidence >= 0.78
            val restraintText =
              if hedgehogGeometry then
                Some("The Hedgehog structure gives White a practical brake on Black's queenside breaks.")
              else if maroczyGeometry then
                Some("The Maroczy bind gives White a practical brake on Black's central breaks.")
              else if opponentCounterbreakDenial then
                singleFocusFile
                  .map(file => s"The current structure gives a practical brake on the opponent's $file-file counterbreak.")
                  .orElse(Some("The current structure gives a practical brake on the opponent's counterbreaks."))
              else if passerBlockade then
                Some(s"The blockade gives a practical brake on the passed pawn on ${blockadedPawnSquare.get}.")
              else if counterplayBreakDenied then
                singleFocusFile
                  .map(file => s"The current structure gives a practical brake on the opponent's $file-file break.")
                  .orElse(Some("The current structure gives a practical brake on the opponent's breaks."))
              else if compensationCounterplayDenied then
                singleFocusFile
                  .map(file => s"The compensation structure gives a practical brake on the opponent's $file-file break.")
                  .orElse(Some("The compensation structure gives a practical brake on the opponent's breaks."))
              else None
            restraintText.flatMap(text =>
              row("Practical restraint", text, tone = Some("practical")).map(_.copy(authority = PracticalPlanAuthority))
            )
            }
        val lineRows =
          structuralIdeas
            .filter(_.kind == StrategicIdeaKind.LineOccupation)
            .flatMap { idea =>
                val refs = idea.evidenceRefs.map(_.trim.toLowerCase)
                val files = idea.focusFiles.map(_.trim.toLowerCase).filter(FileCharPattern.matches).distinct
                val focusSquares = idea.focusSquares.flatMap(validSquare).distinct
                val occupiedRookSquare =
                  focusSquares.find(square => refs.contains(s"occupied_r_$square"))
                val occupiedQueenSquare =
                  focusSquares.find(square => refs.contains(s"occupied_q_$square"))
                val openLineFile =
                  files.find(file => refs.contains(s"open_file_$file"))
                val semiOpenLineFile =
                  files.find(file => refs.contains(s"semi_open_file_$file"))
                val lineFile =
                  openLineFile.orElse(semiOpenLineFile)
                val lineFactFiles =
                  files.filter(file => refs.contains(s"open_file_$file") || refs.contains(s"semi_open_file_$file"))
                val singleLineFactFile =
                  lineFactFiles match
                    case file :: Nil => Some(file)
                    case _           => None
                val doubledRooksFile =
                  files.find(file => refs.contains(s"doubled_rooks_$file"))
                val exactFileEntryAlreadyVisible =
                  singleLineFactFile.exists(exactFileEntryFilesAlreadyVisible.contains)
                val rookFilePost =
                  refs.contains("source:occupied_line_control") &&
                    occupiedRookSquare.nonEmpty &&
                    lineFile.nonEmpty &&
                    idea.beneficiaryPieces.exists(_.trim.equalsIgnoreCase("R")) &&
                    strategySide.forall(side => idea.ownerSide.equalsIgnoreCase(side)) &&
                    idea.confidence >= 0.78
                val queenLinePost =
                  refs.contains("source:occupied_line_control") &&
                    occupiedQueenSquare.nonEmpty &&
                    (lineFile.nonEmpty || refs.contains("occupied_seventh_rank")) &&
                    idea.beneficiaryPieces.exists(_.trim.equalsIgnoreCase("Q")) &&
                    idea.readiness == StrategicIdeaReadiness.Ready &&
                    strategySide.forall(side => idea.ownerSide.equalsIgnoreCase(side)) &&
                    idea.confidence >= 0.72
                val doubledRooksLine =
                  refs.contains("source:doubled_rooks") &&
                    doubledRooksFile.nonEmpty &&
                    idea.beneficiaryPieces.exists(_.trim.equalsIgnoreCase("R")) &&
                    idea.readiness == StrategicIdeaReadiness.Ready &&
                    strategySide.forall(side => idea.ownerSide.equalsIgnoreCase(side)) &&
                    idea.confidence >= 0.74
                val rookOnSeventhLine =
                  refs.contains("source:rook_on_seventh") &&
                    refs.contains("rook_on_seventh_shape") &&
                    idea.beneficiaryPieces.exists(_.trim.equalsIgnoreCase("R")) &&
                    idea.readiness == StrategicIdeaReadiness.Ready &&
                    strategySide.forall(side => idea.ownerSide.equalsIgnoreCase(side)) &&
                    idea.confidence >= 0.72
                val openFileLineCue =
                  refs.contains("source:open_file_control") &&
                    openLineFile.nonEmpty &&
                    idea.beneficiaryPieces.exists(piece =>
                      piece.trim.equalsIgnoreCase("R") || piece.trim.equalsIgnoreCase("Q")
                    ) &&
                    idea.readiness == StrategicIdeaReadiness.Ready &&
                    strategySide.forall(side => idea.ownerSide.equalsIgnoreCase(side)) &&
                    idea.confidence >= 0.80
                val semiOpenFileLineCue =
                  refs.contains("source:semi_open_file_control") &&
                    semiOpenLineFile.nonEmpty &&
                    idea.beneficiaryPieces.exists(piece =>
                      piece.trim.equalsIgnoreCase("R") || piece.trim.equalsIgnoreCase("Q")
                    ) &&
                    idea.readiness == StrategicIdeaReadiness.Ready &&
                    strategySide.forall(side => idea.ownerSide.equalsIgnoreCase(side)) &&
                    idea.confidence >= 0.78
                val connectedRooksLine =
                  refs.contains("source:connected_rooks") &&
                    refs.contains("connected_rooks_shape") &&
                    idea.beneficiaryPieces.exists(_.trim.equalsIgnoreCase("R")) &&
                    (idea.readiness == StrategicIdeaReadiness.Build || idea.readiness == StrategicIdeaReadiness.Ready) &&
                    strategySide.forall(side => idea.ownerSide.equalsIgnoreCase(side)) &&
                    idea.confidence >= 0.64
                val directionalRookLine =
                  refs.contains("source:directional_line_access") &&
                    refs.contains("directional_line_access_shape") &&
                    focusSquares.nonEmpty &&
                    files.exists(file => refs.contains(s"open_file_$file") || refs.contains(s"semi_open_file_$file")) &&
                    idea.beneficiaryPieces.exists(_.trim.equalsIgnoreCase("R")) &&
                    idea.readiness == StrategicIdeaReadiness.Build &&
                    strategySide.forall(side => idea.ownerSide.equalsIgnoreCase(side)) &&
                    idea.confidence >= 0.50
                val routeLineControl =
                  refs.contains("source:line_control_features") &&
                    refs.contains("line_control_shape") &&
                    refs.contains("source:route_line_access") &&
                    refs.contains("route_surface_exact") &&
                    focusSquares.nonEmpty &&
                    files.exists(file => refs.contains(s"open_file_$file") || refs.contains(s"semi_open_file_$file")) &&
                    idea.beneficiaryPieces.exists(_.trim.equalsIgnoreCase("R")) &&
                    idea.readiness == StrategicIdeaReadiness.Ready &&
                    strategySide.forall(side => idea.ownerSide.equalsIgnoreCase(side)) &&
                    idea.confidence >= 0.60
                val compensationOpenLine =
                  refs.contains("source:compensation_open_lines") &&
                    refs.contains("compensation_open_lines_shape") &&
                    refs.contains("material_deficit_compensation") &&
                    files.exists(file => refs.contains(s"open_file_$file") || refs.contains(s"semi_open_file_$file")) &&
                    idea.beneficiaryPieces.exists(piece =>
                      piece.trim.equalsIgnoreCase("R") || piece.trim.equalsIgnoreCase("Q")
                    ) &&
                    idea.readiness == StrategicIdeaReadiness.Build &&
                    strategySide.forall(side => idea.ownerSide.equalsIgnoreCase(side)) &&
                    idea.confidence >= 0.70
                val delayedRecoveryLine =
                  refs.contains("source:delayed_recovery_window") &&
                    refs.contains("delayed_material_recovery") &&
                    refs.contains("development_lead_compensation") &&
                    refs.contains("material_deficit_compensation") &&
                    files.exists(file => refs.contains(s"open_file_$file") || refs.contains(s"semi_open_file_$file")) &&
                    idea.beneficiaryPieces.exists(piece =>
                      piece.trim.equalsIgnoreCase("R") || piece.trim.equalsIgnoreCase("Q")
                    ) &&
                    idea.readiness == StrategicIdeaReadiness.Build &&
                    strategySide.forall(side => idea.ownerSide.equalsIgnoreCase(side)) &&
                    idea.confidence >= 0.74
                val lineText =
                if rookFilePost && !exactFileEntryAlreadyVisible then
                  Some(s"The rook already has a practical ${lineFile.get}-file post.")
                else if queenLinePost && lineFile.nonEmpty && !exactFileEntryAlreadyVisible then
                  Some(s"The queen already has a practical ${lineFile.get}-file post.")
                else if queenLinePost && !exactSeventhRankEntryRolesAlreadyVisible.contains("queen") then
                  Some("The queen on the seventh rank gives a practical line cue.")
                else if doubledRooksLine && !doubledRooksFile.exists(exactDoubledRooksFilesAlreadyVisible.contains) then
                  Some(s"The doubled rooks give a practical ${doubledRooksFile.get}-file line cue.")
                else if rookOnSeventhLine && !exactSeventhRankEntryRolesAlreadyVisible.contains("rook") then
                  Some("The rook on the seventh rank gives a practical line cue.")
                else if openFileLineCue && !exactFileEntryAlreadyVisible then
                  Some(s"The open ${openLineFile.get}-file gives a practical major-piece line cue.")
                else if semiOpenFileLineCue && !exactFileEntryAlreadyVisible then
                  Some(s"The semi-open ${semiOpenLineFile.get}-file gives a practical major-piece line cue.")
                else if connectedRooksLine && !exactConnectedRooksAlreadyVisible then
                  Some("The connected rooks give a practical major-piece coordination cue.")
                else if directionalRookLine && !exactFileEntryAlreadyVisible then
                  singleLineFactFile
                    .map(file => s"The rook is aimed at practical line-play on the $file-file.")
                    .orElse(Some("The rook is aimed at practical line-play."))
                else if routeLineControl && !exactFileEntryAlreadyVisible then
                  singleLineFactFile
                    .map(file => s"The rook route gives a practical $file-file line cue.")
                    .orElse(Some("The rook route gives a practical line cue."))
                else if compensationOpenLine && !exactFileEntryAlreadyVisible then
                  singleLineFactFile
                    .map(file => s"The compensation gives practical line-play on the $file-file.")
                    .orElse(Some("The compensation gives practical line-play."))
                else if delayedRecoveryLine && !exactFileEntryAlreadyVisible then
                  singleLineFactFile
                    .map(file => s"The $file-file pressure gives practical line-play before trying to recover the material.")
                    .orElse(Some("The line pressure gives practical play before trying to recover the material."))
                else None
                lineText.flatMap(text =>
                  row("Practical line", text, tone = Some("practical")).map(_.copy(authority = PracticalPlanAuthority))
                )
            }
        val outpostRows =
          structuralIdeas
            .filter(_.kind == StrategicIdeaKind.OutpostCreationOrOccupation)
            .flatMap { idea =>
              val refs = idea.evidenceRefs.map(_.trim.toLowerCase)
              val focusSquares = idea.focusSquares.flatMap(validSquare).distinct
              val routeOutpostNamedSquare =
                focusSquares match
                  case square :: Nil => Some(square)
                  case _             => None
              val routeOutpostHasUnsuppressedSquare =
                focusSquares.exists(square => !exactOutpostSquaresAlreadyVisible.contains(square))
              val exactRouteOutpost =
                  refs.contains("source:route_outpost_access") &&
                  refs.contains("route_outpost_access_shape") &&
                  refs.contains("route_surface_exact") &&
                  focusSquares.nonEmpty &&
                  routeOutpostHasUnsuppressedSquare &&
                  idea.beneficiaryPieces.exists(piece =>
                    val normalized = piece.trim.toUpperCase
                    normalized == "N" || normalized == "B"
                  ) &&
                  idea.readiness == StrategicIdeaReadiness.Ready &&
                  strategySide.forall(side => idea.ownerSide.equalsIgnoreCase(side)) &&
                  idea.confidence >= 0.66
              val directionalOutpost =
                refs.contains("source:directional_outpost_access") &&
                  refs.contains("directional_outpost_access_shape") &&
                  focusSquares.nonEmpty &&
                  routeOutpostHasUnsuppressedSquare &&
                  idea.beneficiaryPieces.exists(piece =>
                    val normalized = piece.trim.toUpperCase
                    normalized == "N" || normalized == "B"
                  ) &&
                  idea.readiness == StrategicIdeaReadiness.Build &&
                  strategySide.forall(side => idea.ownerSide.equalsIgnoreCase(side)) &&
                  idea.confidence >= 0.64
              val outpostText =
                if exactRouteOutpost then
                  routeOutpostNamedSquare
                    .map(square => s"The minor-piece route points toward a practical outpost cue around $square.")
                    .orElse(Some("The minor-piece route points toward a practical outpost cue."))
                else if directionalOutpost then
                  routeOutpostNamedSquare
                    .map(square => s"The minor piece is aimed at a practical outpost cue around $square.")
                    .orElse(Some("The minor piece is aimed at a practical outpost cue."))
                else None
              outpostText.flatMap(text =>
                row("Practical outpost", text, tone = Some("practical")).map(_.copy(authority = PracticalPlanAuthority))
              )
            }
        val transformationRows =
          if exactSimplificationAlreadyVisible then Nil
          else
            structuralIdeas
              .filter(_.kind == StrategicIdeaKind.FavorableTradeOrTransformation)
              .flatMap { idea =>
                val refs = idea.evidenceRefs.map(_.trim.toLowerCase)
                val focusSquares = idea.focusSquares.flatMap(validSquare).distinct
                val singleFocusSquare =
                  focusSquares match
                    case square :: Nil => Some(square)
                    case _             => None
                val iqpTradeDown =
                  refs.contains("source:iqp_simplification_profile") &&
                    refs.contains("structure_iqp_black") &&
                    (refs.contains("capture_or_exchange") || refs.contains("iqp_trade_down_plan")) &&
                    idea.ownerSide.equalsIgnoreCase("white") &&
                    strategySide.forall(side => idea.ownerSide.equalsIgnoreCase(side)) &&
                    idea.confidence >= 0.78
                val exchangeAvailableIqp =
                  refs.contains("source:exchange_availability_bridge") &&
                    refs.contains("structure_iqp_black") &&
                    idea.ownerSide.equalsIgnoreCase("white") &&
                    idea.readiness == StrategicIdeaReadiness.Build &&
                    strategySide.forall(side => idea.ownerSide.equalsIgnoreCase(side)) &&
                    idea.confidence >= 0.64
                val rookEndgamePatternFacts =
                  List(
                    Option.when(refs.contains("rook_behind_passed_pawn"))("rook-behind-passer structure"),
                    Option.when(refs.contains("king_cut_off"))("king cut-off")
                  ).flatten
                val singleFocusFile =
                  idea.focusFiles.map(_.trim.toLowerCase).filter(file => file.length == 1 && file.head >= 'a' && file.head <= 'h').distinct match
                    case file :: Nil => Some(file)
                    case _           => None
                val singleRookEndgamePatternFact =
                  rookEndgamePatternFacts match
                    case fact :: Nil => Some(fact)
                    case _           => None
                val rookEndgamePattern =
                  refs.contains("source:rook_endgame_pattern") &&
                    refs.contains("rook_endgame_pattern_shape") &&
                    rookEndgamePatternFacts.nonEmpty &&
                    idea.readiness == StrategicIdeaReadiness.Build &&
                    strategySide.forall(side => idea.ownerSide.equalsIgnoreCase(side)) &&
                    idea.confidence >= 0.72 &&
                    !exactConversionAlreadyVisible
                val anchoredOpposition =
                  refs.exists(ref => ref.startsWith("opposition_")) && focusSquares.size >= 2
                val anchoredKingActivity =
                  refs.contains("king_activity_shape") && singleFocusSquare.nonEmpty
                val endgameTechniqueFacts =
                  List(
                    Option.when(refs.contains("opposition_direct") && anchoredOpposition)("direct-opposition cue"),
                    Option.when(refs.contains("opposition_distant") && anchoredOpposition)("distant-opposition cue"),
                    Option.when(refs.contains("opposition_diagonal") && anchoredOpposition)("diagonal-opposition cue"),
                    Option.when(anchoredKingActivity)("active-king cue")
                  ).flatten
                val singleEndgameTechniqueFact =
                  endgameTechniqueFacts match
                    case fact :: Nil => Some(fact)
                    case _           => None
                val endgameTechniqueMotif =
                  refs.contains("source:endgame_technique_motif") &&
                    refs.contains("endgame_technique_shape") &&
                    endgameTechniqueFacts.nonEmpty &&
                    cleanOpt(idea.focusZone).exists(_.equalsIgnoreCase("endgame")) &&
                    idea.readiness == StrategicIdeaReadiness.Build &&
                    strategySide.forall(side => idea.ownerSide.equalsIgnoreCase(side)) &&
                    idea.confidence >= 0.72 &&
                    !exactConversionAlreadyVisible
                val passedPawnConversionMotif =
                  refs.contains("source:passed_pawn_conversion_motif") &&
                    refs.contains("passed_pawn_conversion_shape") &&
                    focusSquares.exists(square => refs.contains(s"passed_pawn_$square")) &&
                    idea.readiness == StrategicIdeaReadiness.Build &&
                    strategySide.forall(side => idea.ownerSide.equalsIgnoreCase(side)) &&
                    idea.confidence >= 0.72 &&
                    !exactConversionAlreadyVisible
                val passedPawnConversionSquare =
                  focusSquares.find(square => refs.contains(s"passed_pawn_$square"))
                val transformationRow =
                  if iqpTradeDown then row("Practical trade", {
                    singleFocusSquare
                      .map(square => s"The IQP structure gives White a practical trade-down cue around $square.")
                      .getOrElse("The IQP structure gives White a practical trade-down cue.")
                  }, tone = Some("practical"))
                  else if exchangeAvailableIqp then
                    row("Practical trade", "The IQP structure gives White a practical exchange-availability cue.", tone = Some("practical"))
                  else if rookEndgamePattern then
                    val text =
                      singleRookEndgamePatternFact
                        .map(fact =>
                          singleFocusFile
                            .map(file => s"The $fact cue is anchored on the $file-file.")
                            .getOrElse(s"The $fact cue is present as endgame support.")
                        )
                        .getOrElse("The rook endgame support stays result-neutral.")
                    row("Endgame cue", text, tone = Some("practical"))
                  else if endgameTechniqueMotif then
                    val text =
                      if anchoredOpposition then
                        singleEndgameTechniqueFact
                          .map(fact => s"The $fact is anchored by the kings on ${focusSquares.take(2).mkString(" and ")}.")
                          .getOrElse("The opposition support stays result-neutral.")
                      else if anchoredKingActivity then
                        singleFocusSquare
                          .map(square => s"The active-king cue is anchored on $square.")
                          .getOrElse("The active-king support stays result-neutral.")
                      else "The endgame support stays result-neutral."
                    row("Endgame cue", text, tone = Some("practical"))
                  else if passedPawnConversionMotif then
                    val text =
                      if refs.contains("pawn_promotion") then
                        passedPawnConversionSquare
                          .map(square => s"The promotion cue is anchored on $square.")
                          .getOrElse("The promotion cue is present as endgame support.")
                      else
                        passedPawnConversionSquare
                          .map(square => s"The passed-pawn cue is anchored on $square.")
                          .getOrElse("The passed-pawn cue is present as endgame support.")
                    row("Endgame cue", text, tone = Some("practical"))
                  else None
                transformationRow.map { row =>
                  if row.label == "Endgame cue" then row
                  else row.copy(authority = PracticalPlanAuthority)
                }
              }
        val minorRows = structuralIdeas
          .filter(_.kind == StrategicIdeaKind.MinorPieceImbalanceExploitation)
          .flatMap { idea =>
            val refs = idea.evidenceRefs.map(_.trim.toLowerCase)
            val focusSquares = idea.focusSquares.flatMap(validSquare).distinct
            val strongKnightSquare = focusSquares.find(square => refs.contains(s"strong_knight_$square"))
            val badBishopSquare = focusSquares.find(square => refs.contains(s"enemy_bad_bishop_$square"))
            val centralizedPieceSquares = focusSquares.filter(square => refs.contains(s"centralized_piece_$square"))
            val singleCentralizedPieceSquare =
              centralizedPieceSquares match
                case square :: Nil => Some(square)
                case _             => None
            val ownerMatchesPack = strategySide.forall(side => idea.ownerSide.equalsIgnoreCase(side))
            val knightBeneficiary = idea.beneficiaryPieces.exists(_.trim.equalsIgnoreCase("N"))
            val minorBeneficiaries =
              idea.beneficiaryPieces.filter(piece => piece.trim.equalsIgnoreCase("N") || piece.trim.equalsIgnoreCase("B")).distinct
            val minorBeneficiary = idea.beneficiaryPieces.find(piece =>
              piece.trim.equalsIgnoreCase("N") || piece.trim.equalsIgnoreCase("B")
            )
            val singleCentralizedMinorBeneficiary =
              minorBeneficiaries match
                case piece :: Nil => Some(piece)
                case _            => None
            val bishopPair =
              !exactBishopPairAlreadyVisible &&
                refs.contains("source:bishop_pair_advantage") &&
                refs.contains("bishop_pair_advantage_shape") &&
                idea.beneficiaryPieces.exists(_.trim.equalsIgnoreCase("B")) &&
                idea.readiness == StrategicIdeaReadiness.Ready &&
                ownerMatchesPack &&
                idea.confidence >= 0.82
            val frenchMinorProfile =
              refs.contains("source:french_minor_piece_profile") &&
                refs.contains("structure_french_advance_chain") &&
                knightBeneficiary &&
                idea.ownerSide.equalsIgnoreCase("white") &&
                ownerMatchesPack
            val frenchKnightVsBishop =
              frenchMinorProfile &&
                refs.contains("source:strong_knight_vs_bad_bishop") &&
                strongKnightSquare.nonEmpty &&
                idea.confidence >= 0.78
            val frenchBadBishopActivity =
              frenchMinorProfile &&
                refs.contains("source:piece_activity_bad_bishop") &&
                badBishopSquare.nonEmpty &&
                idea.confidence >= 0.74
            val strongKnightVsBishop =
              !frenchMinorProfile &&
                refs.contains("source:strong_knight_vs_bad_bishop") &&
                strongKnightSquare.nonEmpty &&
                knightBeneficiary &&
                idea.readiness == StrategicIdeaReadiness.Ready &&
                ownerMatchesPack &&
                idea.confidence >= 0.78
            val knightVsBishopMotif =
              refs.contains("source:knight_vs_bishop_motif") &&
                refs.contains("knight_vs_bishop_motif_shape") &&
                refs.contains("knight_preferred_over_bishop") &&
                knightBeneficiary &&
                idea.readiness == StrategicIdeaReadiness.Build &&
                ownerMatchesPack &&
                idea.confidence >= 0.70
            val badBishopActivity =
              !frenchMinorProfile &&
                refs.contains("source:piece_activity_bad_bishop") &&
                badBishopSquare.nonEmpty &&
                knightBeneficiary &&
                idea.beneficiaryPieces.exists(_.trim.equalsIgnoreCase("B")) &&
                ownerMatchesPack &&
                idea.confidence >= 0.74
            val goodBishopCountEdge =
              !exactBishopPairAlreadyVisible &&
                refs.contains("source:good_bishop") &&
                refs.contains("good_bishop_shape") &&
                refs.contains("source:minor_piece_count_imbalance") &&
                refs.contains("minor_piece_count_imbalance_shape") &&
                refs.contains("good_bishop_count_edge") &&
                idea.beneficiaryPieces.exists(_.trim.equalsIgnoreCase("B")) &&
                ownerMatchesPack &&
                idea.confidence >= 0.74
            val oppositeColorBishops =
              !exactOppositeColorBishopsAlreadyVisible &&
                refs.contains("source:opposite_color_bishops") &&
                refs.contains("opposite_color_bishops_shape") &&
                idea.beneficiaryPieces.exists(_.trim.equalsIgnoreCase("B")) &&
                ownerMatchesPack &&
                idea.confidence >= 0.68
            val pieceCentralization =
              refs.contains("source:piece_centralization_motif") &&
                refs.contains("piece_centralization_shape") &&
                centralizedPieceSquares.nonEmpty &&
                minorBeneficiary.nonEmpty &&
                idea.readiness == StrategicIdeaReadiness.Build &&
                ownerMatchesPack &&
                idea.confidence >= 0.70
            val pieceManeuver =
              refs.contains("source:piece_maneuver_motif") &&
                refs.contains("piece_maneuver_shape") &&
                minorBeneficiary.nonEmpty &&
                idea.readiness == StrategicIdeaReadiness.Build &&
                ownerMatchesPack &&
                idea.confidence >= 0.70
            val enemyBadBishop =
              refs.contains("source:enemy_bad_bishop") &&
                refs.contains("enemy_bad_bishop_shape") &&
                knightBeneficiary &&
                idea.beneficiaryPieces.exists(_.trim.equalsIgnoreCase("B")) &&
                ownerMatchesPack &&
                idea.confidence >= 0.80
            val minorText =
              if frenchKnightVsBishop then Some(
                s"The French pawn chain gives White a practical knight-vs-bishop cue around ${strongKnightSquare.get}."
              )
              else if frenchBadBishopActivity then
                Some(s"The French pawn chain gives White a practical minor-piece cue against the bad bishop on ${badBishopSquare.get}.")
              else if strongKnightVsBishop then
                Some(s"The current minor-piece map gives a practical knight-vs-bishop cue around ${strongKnightSquare.get}.")
              else if knightVsBishopMotif then
                Some("The current minor-piece map gives a practical knight-vs-bishop cue.")
              else if badBishopActivity then
                Some(s"The current minor-piece map gives a practical cue against the bad bishop on ${badBishopSquare.get}.")
              else if bishopPair then
                Some("The current minor-piece map gives a practical bishop-pair cue.")
              else if goodBishopCountEdge then
                Some("The current minor-piece map gives a practical good-bishop cue.")
              else if oppositeColorBishops then
                Some("The current minor-piece map gives a practical opposite-colored-bishops cue.")
              else if pieceCentralization then
                (singleCentralizedMinorBeneficiary, singleCentralizedPieceSquare) match
                  case (Some(piece), Some(square)) =>
                    Some(s"The centralized ${piece.trim.toUpperCase} on $square gives a practical minor-piece cue.")
                  case _ =>
                    Some("The centralized minor pieces give a practical minor-piece cue.")
              else if pieceManeuver then
                Some(s"The ${minorBeneficiary.get.trim.toUpperCase} maneuver gives a practical minor-piece cue.")
              else if enemyBadBishop then
                Some("The current minor-piece map gives a practical cue against the opponent's bad bishop.")
              else None
            minorText.flatMap(text =>
              row("Practical minor", text, tone = Some("practical")).map(_.copy(authority = PracticalPlanAuthority))
            )
          }
        val prophylaxisRows =
          if prophylaxisAlreadyVisible then Nil
          else
            structuralIdeas
              .filter(_.kind == StrategicIdeaKind.Prophylaxis)
              .flatMap { idea =>
                val refs = idea.evidenceRefs.map(_.trim.toLowerCase)
                val focusSquares = idea.focusSquares.flatMap(validSquare).distinct
                val focusFiles = idea.focusFiles.map(_.trim.toLowerCase).filter(FileCharPattern.matches).distinct
                val bishopPinWatch =
                  refs.contains("source:bishop_pin_watch") &&
                    focusSquares.exists(square => square == "g4" || square == "g5") &&
                    strategySide.forall(side => idea.ownerSide.equalsIgnoreCase(side)) &&
                    idea.confidence >= 0.84
                val queensideCounterbreakWatch =
                  refs.contains("source:queenside_counterbreak_watch") &&
                    focusFiles.contains("b") &&
                    strategySide.forall(side => idea.ownerSide.equalsIgnoreCase(side)) &&
                    idea.confidence >= 0.90
                val prophylaxisText =
                  if bishopPinWatch then Some("The current piece layout gives a practical prophylaxis cue against the bishop pin.")
                  else if queensideCounterbreakWatch then Some("The queenside structure gives a practical prophylaxis cue against the ...b5 break.")
                  else None
                prophylaxisText.flatMap(text =>
                  row("Practical prophylaxis", text, tone = Some("practical")).map(_.copy(authority = PracticalPlanAuthority))
                )
              }
        val attackRows =
          structuralIdeas
            .filter(_.kind == StrategicIdeaKind.KingAttackBuildUp)
            .flatMap { idea =>
                val refs = idea.evidenceRefs.map(_.trim.toLowerCase)
                val focusSquares = idea.focusSquares.flatMap(validSquare).distinct
                val focusFiles = idea.focusFiles.map(_.trim.toLowerCase).filter(_.nonEmpty)
                val singleFocusFile =
                  focusFiles match
                    case file :: Nil => Some(file)
                    case _           => None
                val singleFocusSquare =
                  focusSquares match
                    case square :: Nil => Some(square)
                    case _             => None
                val beneficiaryPieces =
                  idea.beneficiaryPieces.map(_.trim).filter(_.nonEmpty).distinct
                val singleBeneficiaryPiece =
                  beneficiaryPieces match
                    case piece :: Nil => Some(piece)
                    case _            => None
                val focusZone = cleanOpt(idea.focusZone).map(_.toLowerCase)
                val broadAttackShell =
                  refs.contains("source:king_ring_pressure") ||
                    refs.contains("source:enemy_weak_back_rank") ||
                    refs.contains("source:compensation_king_window")
                val fianchettoAssault =
                  !exactAttackAlreadyVisible &&
                    refs.contains("source:fianchetto_assault_profile") &&
                    refs.contains("source:opposite_side_storm") &&
                    refs.contains("structure_fianchetto_shell") &&
                    focusZone.contains("kingside") &&
                    idea.readiness == StrategicIdeaReadiness.Build &&
                    strategySide.forall(side => idea.ownerSide.equalsIgnoreCase(side)) &&
                    idea.confidence >= 0.90
                val enemyKingStuckCenter =
                  !exactAttackAlreadyVisible &&
                    refs.contains("source:enemy_king_stuck_center") &&
                    refs.contains("enemy_king_central_exposure") &&
                    focusZone.nonEmpty &&
                    idea.readiness == StrategicIdeaReadiness.Build &&
                    strategySide.forall(side => idea.ownerSide.equalsIgnoreCase(side)) &&
                    idea.confidence >= 0.80
                val flankHookPressure =
                  !exactFlankAttackAlreadyVisible &&
                    refs.contains("source:flank_pawn_pressure") &&
                    refs.contains("hook_creation_chance") &&
                    focusZone.nonEmpty &&
                    idea.readiness == StrategicIdeaReadiness.Build &&
                    strategySide.forall(side => idea.ownerSide.equalsIgnoreCase(side)) &&
                    idea.confidence >= 0.77
                val flankPawnAdvanceFiles =
                  focusFiles.distinct.filter(file =>
                    Set("a", "b", "g", "h").contains(file) &&
                      refs.contains(s"flank_pawn_file_$file")
                  )
                val singleFlankPawnAdvanceFile =
                  flankPawnAdvanceFiles match
                    case file :: Nil => Some(file)
                    case _           => None
                val flankPawnAdvanceZones =
                  flankPawnAdvanceFiles.flatMap {
                    case "a" | "b" => Some("queenside")
                    case "g" | "h" => Some("kingside")
                    case _         => None
                  }.distinct
                val singleFlankPawnAdvanceZone =
                  flankPawnAdvanceZones match
                    case zone :: Nil => Some(zone)
                    case _           => None
                val flankPawnAdvance =
                  !exactFlankAttackAlreadyVisible &&
                    refs.contains("source:flank_pawn_advance_motif") &&
                    refs.contains("flank_pawn_advance_shape") &&
                    flankPawnAdvanceFiles.nonEmpty &&
                    refs.exists(ref => ref.startsWith("flank_pawn_to_rank_") && ref.stripPrefix("flank_pawn_to_rank_").toIntOption.exists(_ >= 4)) &&
                    idea.readiness == StrategicIdeaReadiness.Build &&
                    strategySide.forall(side => idea.ownerSide.equalsIgnoreCase(side)) &&
                    idea.confidence >= 0.70
                val compensationDiagonalBattery =
                  !exactAttackAlreadyVisible &&
                    refs.contains("source:compensation_diagonal_battery") &&
                    refs.contains("compensation_diagonal_battery") &&
                    refs.contains("material_deficit_compensation") &&
                    idea.beneficiaryPieces.exists(_.trim.equalsIgnoreCase("B")) &&
                    idea.beneficiaryPieces.exists(_.trim.equalsIgnoreCase("Q")) &&
                    focusZone.exists(zone => zone == "kingside" || zone == "center" || zone.contains("king")) &&
                    strategySide.forall(side => idea.ownerSide.equalsIgnoreCase(side)) &&
                    idea.confidence >= 0.74
                val compensationDevelopmentLead =
                  !exactAttackAlreadyVisible &&
                    refs.contains("source:compensation_development_lead") &&
                    refs.contains("development_lead_compensation") &&
                    refs.contains("material_deficit_compensation") &&
                    focusZone.exists(zone => zone == "kingside" || zone == "center" || zone.contains("king")) &&
                    idea.readiness == StrategicIdeaReadiness.Build &&
                    strategySide.forall(side => idea.ownerSide.equalsIgnoreCase(side)) &&
                    idea.confidence >= 0.76
                val routeAttackLaneCandidate =
                  !exactAttackAlreadyVisible &&
                    refs.contains("source:route_attack_lane") &&
                    refs.contains("route_attack_lane_shape") &&
                    focusSquares.nonEmpty &&
                    focusZone.nonEmpty &&
                    idea.beneficiaryPieces.exists(_.trim.nonEmpty) &&
                    idea.readiness == StrategicIdeaReadiness.Ready &&
                    strategySide.forall(side => idea.ownerSide.equalsIgnoreCase(side)) &&
                    idea.confidence >= 0.74
                val exactRouteAttackLane =
                  routeAttackLaneCandidate && singleFocusSquare.nonEmpty
                val directionalAttackLaneCandidate =
                  !exactAttackAlreadyVisible &&
                    refs.contains("source:directional_attack_lane") &&
                    refs.contains("directional_attack_lane_shape") &&
                    focusSquares.nonEmpty &&
                    focusZone.nonEmpty &&
                    idea.beneficiaryPieces.exists(_.trim.nonEmpty) &&
                    idea.readiness == StrategicIdeaReadiness.Build &&
                    strategySide.forall(side => idea.ownerSide.equalsIgnoreCase(side)) &&
                    idea.confidence >= 0.72
                val directionalAttackLane =
                  directionalAttackLaneCandidate && singleFocusSquare.nonEmpty
                val ambiguousAttackLane =
                  (routeAttackLaneCandidate || directionalAttackLaneCandidate) && singleFocusSquare.isEmpty
                val motifBatteryAxes =
                  List(
                    Option.when(refs.contains("battery_axis_diagonal"))("diagonal"),
                    Option.when(refs.contains("battery_axis_file"))("file")
                  ).flatten
                val singleMotifBatteryAxis =
                  motifBatteryAxes match
                    case axis :: Nil => Some(axis)
                    case _           => None
                val motifBattery =
                  !exactBatteryPressureAlreadyVisible &&
                    refs.contains("source:motif_battery") &&
                    motifBatteryAxes.nonEmpty &&
                    focusSquares.size >= 2 &&
                    focusZone.nonEmpty &&
                    idea.beneficiaryPieces.count(_.trim.nonEmpty) >= 2 &&
                    idea.readiness == StrategicIdeaReadiness.Build &&
                    strategySide.forall(side => idea.ownerSide.equalsIgnoreCase(side)) &&
                    idea.confidence >= 0.74
                val motifRookLift =
                  !exactRookLiftAlreadyVisible &&
                    refs.contains("source:motif_rook_lift") &&
                    focusFiles.nonEmpty &&
                    focusZone.nonEmpty &&
                    idea.beneficiaryPieces.exists(_.trim.equalsIgnoreCase("R")) &&
                    idea.readiness == StrategicIdeaReadiness.Build &&
                    strategySide.forall(side => idea.ownerSide.equalsIgnoreCase(side)) &&
                    idea.confidence >= 0.78
                val motifPieceLift =
                  !exactAttackAlreadyVisible &&
                    refs.contains("source:motif_piece_lift") &&
                    refs.contains("motif_piece_lift_shape") &&
                    focusZone.nonEmpty &&
                    idea.beneficiaryPieces.exists(_.trim.nonEmpty) &&
                    idea.readiness == StrategicIdeaReadiness.Build &&
                    strategySide.forall(side => idea.ownerSide.equalsIgnoreCase(side)) &&
                    idea.confidence >= 0.72
                val motifCheckPressure =
                  !exactAttackAlreadyVisible &&
                    refs.contains("source:motif_check_pressure") &&
                    (refs.contains("check_type_normal") || refs.contains("check_type_discovered")) &&
                    focusSquares.nonEmpty &&
                    focusZone.nonEmpty &&
                    idea.beneficiaryPieces.exists(_.trim.nonEmpty) &&
                    idea.readiness == StrategicIdeaReadiness.Ready &&
                    strategySide.forall(side => idea.ownerSide.equalsIgnoreCase(side)) &&
                    idea.confidence >= 0.68
                val fianchettoMotif =
                  !exactAttackAlreadyVisible &&
                    refs.contains("source:fianchetto_motif") &&
                    refs.contains("fianchetto_motif_shape") &&
                    refs.exists(ref => ref == "fianchetto_side_kingside" || ref == "fianchetto_side_queenside") &&
                    focusZone.nonEmpty &&
                    idea.beneficiaryPieces.exists(_.trim.equalsIgnoreCase("B")) &&
                    idea.readiness == StrategicIdeaReadiness.Build &&
                    strategySide.forall(side => idea.ownerSide.equalsIgnoreCase(side)) &&
                    idea.confidence >= 0.70
                val attackSurface =
                  if ambiguousAttackLane || broadAttackShell then None
                  else if fianchettoAssault then Some("Practical attack" -> "The fianchetto-shell structure gives a practical opposite-side attack cue.")
                  else if enemyKingStuckCenter then
                    Some("Practical attack" -> "The enemy king's central exposure gives a practical attacking cue.")
                  else if flankHookPressure then
                    Some("Practical attack" -> "The current flank-pawn map gives a practical hook-creation cue.")
                  else if flankPawnAdvance then
                    (singleFlankPawnAdvanceFile, singleFlankPawnAdvanceZone) match
                      case (Some(file), Some(zone)) =>
                        Some("Practical attack" -> s"The $file-pawn advance gives a practical $zone attacking cue.")
                      case (None, Some(zone)) =>
                        Some("Practical attack" -> s"The flank-pawn advances give a practical $zone attacking cue.")
                      case _ =>
                        Some("Practical attack" -> "The flank-pawn advances give a practical attacking cue.")
                  else if compensationDiagonalBattery then
                    Some("Compensation pressure" -> "The material-compensation structure gives practical diagonal-battery pressure.")
                  else if compensationDevelopmentLead then
                    Some("Compensation pressure" -> "The material-compensation structure gives practical development-led pressure.")
                  else if exactRouteAttackLane then
                    singleFocusSquare
                      .map(square => "Practical attack" -> s"The $square route gives a practical attacking lane.")
                  else if directionalAttackLane then
                    singleFocusSquare
                      .map(square => "Practical attack" -> s"The $square target gives a practical attacking lane.")
                  else if motifBattery then
                    singleMotifBatteryAxis
                      .map(axis => "Practical attack" -> s"The current $axis battery gives a practical attacking cue.")
                      .orElse(Some("Practical attack" -> "The current battery gives a practical attacking cue."))
                  else if motifRookLift then
                    singleFocusFile
                      .map(file => "Practical attack" -> s"The rook lift on the $file-file gives a practical attacking cue.")
                      .orElse(Some("Practical attack" -> "The rook lift gives a practical attacking cue."))
                  else if motifPieceLift then
                    singleBeneficiaryPiece
                      .map(piece => "Practical attack" -> s"The ${piece.toUpperCase} lift gives a practical attacking cue.")
                      .orElse(Some("Practical attack" -> "The piece lift gives a practical attacking cue."))
                  else if motifCheckPressure then
                    (singleBeneficiaryPiece, singleFocusSquare) match
                      case (Some(piece), Some(square)) =>
                        Some("Practical attack" -> s"The ${piece.toUpperCase} check on $square gives a practical attacking cue.")
                      case (Some(piece), None) =>
                        Some("Practical attack" -> s"The ${piece.toUpperCase} check motif gives a practical attacking cue.")
                      case (None, Some(square)) =>
                        Some("Practical attack" -> s"The check on $square gives a practical attacking cue.")
                      case _ =>
                        Some("Practical attack" -> "The check motif gives a practical attacking cue.")
                  else if fianchettoMotif then
                    Some("Practical attack" -> "The fianchettoed bishop gives a practical long-diagonal cue.")
                  else None
                attackSurface.flatMap { case (label, text) =>
                  row(label, text, tone = Some("practical")).map(_.copy(authority = PracticalPlanAuthority))
                }
            }
        (pressureRows ++ spaceRows ++ breakRows ++ restraintRows ++ lineRows ++ outpostRows ++ transformationRows ++ minorRows ++ prophylaxisRows ++ attackRows)
          .distinctBy(_.text)
    val explanationRows = moveReviewExplanation.toList.flatMap(explanationSupportRows(_, knownSans))
    val referenceRows = if explanationRows.isEmpty then referenceLineRows(ctx, refs, knownSans, lineConsequence) else Nil
    val summaryRows =
      distinctVisibleRows(
        (
          mainPlanRow(promotedPlans).toList ++
            openingRows ++
            practicalRows ++
            localRows ++
            explanationRows ++
            referenceRows
        )
      )
    MoveReviewPlayerSurface(
      schema = Schema,
      title =
        moveReviewExplanation.flatMap(explanation => cleanOpt(Some(explanation.title)))
          .orElse(ctx.playedSan.flatMap(san => cleanOpt(Some(s"Move review: $san")))),
      summaryRows = summaryRows,
      advancedRows =
        (
          relationRows ++ compensationRows ++ structuralIdeaRows ++
            (if supportBlocked then Nil else advancedRows(ctx, promotedPlans, evaluatedPlans, practicalStructureArc))
        )
          .distinctBy(row => (row.label, row.text, row.authority.flatMap(_.token))),
      decisionComparison = decisionComparisonSurface.map(comparison =>
        sanitizeDecisionComparison(enrichDecisionTargetComparison(ctx, comparison))
      ),
      probeRows = ledgerRows(moveReviewLedger, knownSans),
      authorRows = authorRows(authoringSurface, knownSans)
    )

  private def strategicRelationRows(
      strategyPack: Option[StrategyPack],
      suppressedRelationKinds: Set[String]
  ): List[MoveReviewPlayerSurfaceRow] =
    strategyPack.toList
      .flatMap(_.strategicIdeas)
      .zipWithIndex
      .flatMap { case (idea, index) =>
        strategicRelationDescriptor(idea)
          .filterNot(descriptor => suppressedRelationKinds.contains(descriptor.relationKind))
          .map(descriptor => (descriptor, idea, index))
      }
      .sortBy { case (descriptor, _, index) => (descriptor.surfacePriority, index) }
      .flatMap { case (descriptor, idea, _) => strategicRelationRow(idea, descriptor) }
      .distinctBy(row => (row.authority.flatMap(_.token), row.authority.flatMap(_.target), row.text))
      .take(MaxStrategicRelationRows)

  private def compensationAdvancedRows(strategyPack: Option[StrategyPack]): List[MoveReviewPlayerSurfaceRow] =
    val surface = StrategyPackSurface.from(strategyPack)
    if !compensationRowsEligible(surface) then Nil
    else
      val primary = CompensationDisplayPhrasing.compensationWhyNowText(surface).map("Compensation" -> _)
      val support =
        CompensationDisplayPhrasing
          .dedupeCompensationSupport(
            primary.map(_._2).getOrElse(""),
            CompensationDisplayPhrasing.compensationPersistenceText(surface).toList ++
              CompensationDisplayPhrasing.compensationObjectiveText(surface).toList
          )
          .map("Compensation condition" -> _)
      distinctVisibleRows(
        (primary.toList ++ support).flatMap { case (label, text) =>
          row(label, text, tone = Some("practical")).map(
            _.copy(authority = PracticalPlanAuthority)
          )
        }
      )
        .take(MaxCompensationRows)

  private def compensationRowsEligible(surface: StrategyPackSurface.Snapshot): Boolean =
    StrategyPackSurface.strictCompensationSubtypeLabel(surface).nonEmpty &&
      CompensationDisplayPhrasing.compensationNarrationEligible(surface)

  private def strategicRelationRow(
      idea: StrategyIdeaSignal,
      descriptor: RelationObservationDescriptor
  ): Option[MoveReviewPlayerSurfaceRow] =
    val relation = descriptor.relationKind
    val focus = strategicRelationFocus(idea)
    if focus.isEmpty then None
    else
      val focusText = focus.take(3).mkString(", ")
      val tailFocusText = focus.tail.take(3).mkString(" and ")
      val anchorText = s" around $focusText"
      val lead = strategicRelationLead(descriptor)
      val text =
        descriptor.publicLabel match
          case "defender-trade" if focus.size >= 2 =>
            s"$lead is a ${descriptor.publicLabel} around ${focus.head} through the exchange on ${focus(1)}."
          case "bad-piece liquidation" if focus.size >= 2 =>
            s"$lead is ${descriptor.publicLabel} from ${focus.head} through the exchange on ${focus(1)}."
          case "overload" if focus.size >= 2 =>
            s"$lead is ${descriptor.publicLabel} pressure on ${focus.head} across $tailFocusText."
          case "deflection" if focus.size >= 3 =>
            s"$lead is a ${descriptor.publicLabel} motif on ${focus.head} by attacking ${focus(1)} from ${focus(2)}."
          case "discovered-attack" if focus.size >= 3 =>
            s"$lead is a ${descriptor.publicLabel} from ${focus.head} through ${focus(1)} toward ${focus(2)}."
          case "double-check" if focus.size >= 2 =>
            s"$lead is ${descriptor.publicLabel} pressure on ${focus.head} from $tailFocusText."
          case "back-rank mate" if focus.size >= 2 =>
            s"$lead is a ${descriptor.publicLabel} pattern around ${focus.head} from $tailFocusText."
          case "mate net" if focus.size >= 2 =>
            s"$lead is a ${descriptor.publicLabel} around ${focus.head} from $tailFocusText."
          case "Greek gift" if focus.size >= 2 =>
            s"$lead is a ${descriptor.publicLabel} sacrifice from ${focus.head} toward ${focus(1)}."
          case "stalemate resource" if focus.size >= 2 =>
            s"$lead is a ${descriptor.publicLabel} available around ${focus.head} via ${focus(1)}."
          case "perpetual-check resource" if focus.size >= 2 =>
            s"$lead is a ${descriptor.publicLabel} available around ${focus.head} from $tailFocusText."
          case "fork" if focus.size >= 2 =>
            s"$lead is a ${descriptor.publicLabel} from ${focus.head} across $tailFocusText."
          case "hanging piece" if focus.size >= 2 =>
            s"$lead is ${descriptor.publicLabel} pressure from ${focus.head} on ${focus(1)}."
          case "decoy" if focus.size >= 3 =>
            s"$lead is a ${descriptor.publicLabel} motif on ${focus(1)} that pulls from ${focus(2)}."
          case "trapped-piece" if focus.size >= 2 =>
            s"$lead is ${descriptor.publicLabel} pressure on ${focus(1)} from ${focus.head}."
          case "key-square restriction" if focus.size >= 2 =>
            s"$lead is ${descriptor.publicLabel} on ${focus(1)} from ${focus.head}."
          case "zwischenzug" if focus.size >= 3 =>
            s"$lead is ${descriptor.publicLabel} from ${focus.head} before the recapture on ${focus(1)}."
          case "x-ray" if focus.size >= 3 =>
            s"$lead is ${descriptor.publicLabel} geometry from ${focus.head} through ${focus(1)} toward ${focus(2)}."
          case "clearance" if focus.size >= 3 =>
            s"$lead is ${descriptor.publicLabel} geometry with ${focus(1)} clearing the line from ${focus.head} toward ${focus(2)}."
          case "battery" if focus.size >= 3 =>
            s"$lead is ${descriptor.publicLabel} geometry between ${focus.head} and ${focus(1)} toward ${focus(2)}."
          case "pin" if focus.size >= 3 =>
            s"$lead is ${descriptor.publicLabel} geometry from ${focus.head} through ${focus(1)} toward ${focus(2)}."
          case "skewer" if focus.size >= 3 =>
            s"$lead is ${descriptor.publicLabel} geometry from ${focus.head} through ${focus(1)} toward ${focus(2)}."
          case "interference" if focus.size >= 3 =>
            s"$lead is ${descriptor.publicLabel} geometry with ${focus.head} between ${focus(1)} and ${focus(2)}."
          case _ =>
            descriptor.surfaceRowKind match
              case RelationSurfaceRowKind.DrawResource =>
                s"$lead is ${descriptor.publicLabel} available$anchorText."
              case RelationSurfaceRowKind.MoveOrder =>
                s"$lead is ${descriptor.publicLabel}$anchorText."
              case RelationSurfaceRowKind.MobilityRestriction =>
                s"$lead is ${descriptor.publicLabel}$anchorText."
              case RelationSurfaceRowKind.LineGeometry =>
                s"$lead is ${descriptor.publicLabel} geometry$anchorText."
              case RelationSurfaceRowKind.TacticalRelation =>
                s"$lead is a ${descriptor.publicLabel} motif$anchorText."
      row(
        label = descriptor.surfaceRowLabel,
        text = text,
        tone = Some("relation")
      ).map(
        _.copy(
          authority =
            Some(
              MoveReviewSurfaceAuthority(
                kind = MoveReviewSurfaceAuthority.StrategicRelation,
                token = Some(relation),
                target = relationTargetFromIdea(idea, descriptor, focus)
              )
            )
        )
      )

  private def strategicRelationLead(descriptor: RelationObservationDescriptor): String =
    descriptor.surfaceRowKind match
      case RelationSurfaceRowKind.DrawResource        => "The draw-resource relation"
      case RelationSurfaceRowKind.MoveOrder           => "The move-order relation"
      case RelationSurfaceRowKind.MobilityRestriction => "The mobility relation"
      case RelationSurfaceRowKind.LineGeometry        => "The line relation"
      case RelationSurfaceRowKind.TacticalRelation    => "The tactical relation"

  private def strategicRelationDescriptor(idea: StrategyIdeaSignal) =
    RelationObservationCatalog.descriptorForEvidence(idea.relationKind, idea.evidenceRefs)

  private def strategicRelationFocus(idea: StrategyIdeaSignal): List[String] =
    idea.relationFocusSquares.flatMap(validSquare).distinct

  private def relationTargetFromIdea(
      idea: StrategyIdeaSignal,
      descriptor: RelationObservationDescriptor,
      focus: List[String]
  ): Option[String] =
    idea.relationKind.flatMap(_ => idea.targetSquare)
      .flatMap(validSquare)
      .filter(focus.contains)
      .orElse(descriptor.fallbackTarget(focus))

  private def validSquare(raw: String): Option[String] =
    val cleaned = clean(raw).toLowerCase
    if ChessSquarePattern.matches(cleaned) then Some(cleaned) else None

  private def squareCoords(raw: String): Option[(Int, Int)] =
    validSquare(raw).map(square => (square.charAt(0) - 'a' + 1, square.charAt(1).asDigit))

  private def squareColorOf(raw: String): Option[String] =
    squareCoords(raw).map { case (file, rank) =>
      if (file + rank) % 2 == 0 then "dark" else "light"
    }

  private def roleCanAttackSquare(role: String, from: String, target: String): Boolean =
    (squareCoords(from), squareCoords(target)) match
      case (Some((fromFile, fromRank)), Some((targetFile, targetRank))) =>
        val fileDelta = math.abs(fromFile - targetFile)
        val rankDelta = math.abs(fromRank - targetRank)
        clean(role).toLowerCase match
          case "bishop" => fileDelta == rankDelta && fileDelta > 0
          case "knight" => (fileDelta == 1 && rankDelta == 2) || (fileDelta == 2 && rankDelta == 1)
          case _        => false
      case _ => false

  private def exactFileEntryRowFile(row: MoveReviewPlayerSurfaceRow): Option[String] =
    exactPracticalTargetRowTarget(row, "File entry") { target =>
      row.text == s"The checked line keeps pressure on $target through the ${target.take(1)}-file."
    }.map(_.take(1))

  private def exactSeventhRankEntryRowRole(row: MoveReviewPlayerSurfaceRow): Option[String] =
    val SeventhRankEntryText =
      """The checked line puts the (rook) on the (?:seventh|second) rank at [a-h][1-8][.]""".r
    if row.label != "Seventh-rank entry" then None
    else
      row.authority
        .filter(_.kind == MoveReviewSurfaceAuthority.PracticalPlan)
        .flatMap(_ => SeventhRankEntryText.findFirstMatchIn(row.text))
        .map(_.group(1))

  private def exactConnectedRooksRow(row: MoveReviewPlayerSurfaceRow): Boolean =
    exactPracticalPlanRow(row, "Connected rooks") {
      ConnectedRooksPattern.matches
    }

  private def exactDoubledRooksRowFile(row: MoveReviewPlayerSurfaceRow): Option[String] =
    val DoubledRooksText = """The checked line doubles the rooks on the ([a-h])-file[.]""".r
    if row.label != "Doubled rooks" then None
    else
      row.authority
        .filter(_.kind == MoveReviewSurfaceAuthority.PracticalPlan)
        .flatMap(_ => DoubledRooksText.findFirstMatchIn(row.text))
        .map(_.group(1))

  private def exactBishopPairRow(row: MoveReviewPlayerSurfaceRow): Boolean =
    exactPracticalPlanRow(row, "Bishop pair") {
      _ == "The checked capture keeps the bishop pair on the board."
    }

  private def exactOppositeColorBishopsRow(row: MoveReviewPlayerSurfaceRow): Boolean =
    exactPracticalPlanRow(row, "Opposite-color bishops") {
      _ == "The checked capture leaves opposite-colored bishops on the board."
    }

  private def exactTargetRowSquare(row: MoveReviewPlayerSurfaceRow): Option[String] =
    exactPracticalTargetRowTarget(row, "Fixed target") { target =>
      row.text == s"The checked line keeps $target fixed as the target."
    }.orElse(
      exactPracticalTargetRowTarget(row, "Minority attack") { target =>
        row.text == s"The checked line keeps $target as the minority-attack fixed target."
      }
    )

  private def exactOutpostRowSquare(row: MoveReviewPlayerSurfaceRow): Option[String] =
    exactKnightOutpostRowSquare(row)

  private def exactKnightOutpostRowSquare(row: MoveReviewPlayerSurfaceRow): Option[String] =
    exactPracticalTargetRowTarget(row, "Knight outpost") { target =>
      row.text == s"The checked line puts the knight on the $target outpost." ||
        row.text == s"The checked line puts the knight on the pawn-supported $target outpost square."
    }

  private def exactSimplificationRow(row: MoveReviewPlayerSurfaceRow): Boolean =
    exactPracticalTargetRow(row, "Simplification") { target =>
      row.text == s"The checked line keeps the same local edge after the exchange on $target."
    }

  private def exactTechnicalConversionRow(row: MoveReviewPlayerSurfaceRow): Boolean =
    exactPracticalPlanRow(row, "Technical conversion") { text =>
      text == "The checked line keeps the best defense narrow and the conversion route intact." ||
        TechnicalConversionPattern.matches(text)
    }

  private def exactCounterplayRow(row: MoveReviewPlayerSurfaceRow): Boolean =
    exactCounterplayBreakRow(row) ||
      exactPracticalPlanRow(row, "Counterplay restraint") { text =>
        RestrainedPattern.matches(text) ||
          UnavailablePattern.matches(text)
      } ||
      exactPracticalPlanRow(row, "Break and entry") { text =>
        BreakShutPattern.matches(text)
      } ||
      exactPracticalPlanRow(row, "Route denial") { text =>
        RouteDenialPattern.matches(text)
      }

  private def exactCounterplayBreakRow(row: MoveReviewPlayerSurfaceRow): Boolean =
    row.label == "Counterplay break" &&
      row.authority.exists(authority =>
        authority.kind == MoveReviewSurfaceAuthority.CounterplayBreak &&
          authority.token.exists(token =>
            NeutralizeKeyBreakSurfaceGate.matchesSurfaceText(token, row.text, authority.target)
          )
      )

  private def exactAttackRow(row: MoveReviewPlayerSurfaceRow): Boolean =
    exactPracticalPlanRow(row, "Back-rank mate") { text =>
      BackRankMatePattern.matches(text)
    } ||
      exactPracticalPlanRow(row, "Mate net") { text =>
        MateNetPattern.matches(text)
      } ||
      exactPracticalPlanRow(row, "Greek gift") { text =>
        GreekGiftPattern.matches(text)
      }

  private def exactBatteryPressureRow(row: MoveReviewPlayerSurfaceRow): Boolean =
    exactPracticalPlanRow(row, "Battery pressure") { text =>
      BatteryPressurePattern.matches(text)
    }

  private def exactFlankAttackRow(row: MoveReviewPlayerSurfaceRow): Boolean =
    exactPracticalPlanRow(row, "Hook creation") { text =>
      HookCreationPattern.matches(text)
    } ||
      exactPracticalPlanRow(row, "Rook-pawn march") { text =>
        RookPawnMarchPattern.matches(text)
      }

  private def exactRookLiftRow(row: MoveReviewPlayerSurfaceRow): Boolean =
    exactPracticalPlanRow(row, "Rook lift") { text =>
      RookLiftPattern.matches(text)
    }

  private def exactColorComplexRowComplex(row: MoveReviewPlayerSurfaceRow): Option[String] =
    exactPracticalTargetRowTarget(row, "Color complex") { target =>
      val ColorComplexText =
        s"""The checked line keeps the (bishop|knight) on ([a-h][1-8]) attacking $target in the (dark|light)-square complex[.]""".r
      row.text match
        case ColorComplexText(role, from, complex) =>
          squareColorOf(target).contains(complex) && roleCanAttackSquare(role, from, target)
        case _ => false
    }.flatMap(_ =>
      """in the ((?:dark|light)-square complex)\.""".r
        .findFirstMatchIn(row.text)
        .map(_.group(1).toLowerCase)
    )

  private def exactPracticalPlanRow(
      row: MoveReviewPlayerSurfaceRow,
      label: String
  )(textMatches: String => Boolean): Boolean =
    row.label == label &&
      row.authority.exists(_.kind == MoveReviewSurfaceAuthority.PracticalPlan) &&
      textMatches(row.text)

  private def exactPracticalTargetRow(
      row: MoveReviewPlayerSurfaceRow,
      label: String
  )(textMatches: String => Boolean): Boolean =
    exactPracticalTargetRowTarget(row, label)(textMatches).nonEmpty

  private def exactPracticalTargetRowTarget(
      row: MoveReviewPlayerSurfaceRow,
      label: String
  )(textMatches: String => Boolean): Option[String] =
    if row.label != label then None
    else
      row.authority
        .filter(_.kind == MoveReviewSurfaceAuthority.PracticalPlan)
        .flatMap(_.target)
        .flatMap(validSquare)
        .filter(textMatches)

  private def mainPlanRow(promotedPlans: List[EvaluatedPlan]): Option[MoveReviewPlayerSurfaceRow] =
    val plans =
      promotedPlans
        .flatMap(plan => cleanOpt(Some(plan.hypothesis.planName)))
        .distinct
        .take(2)
    row("Main plans", plans.mkString(" / "))

  private def advancedRows(
      ctx: NarrativeContext,
      promotedPlans: List[EvaluatedPlan],
      evaluatedPlans: List[EvaluatedPlan],
      practicalStructureArc: Option[StructurePlanArc]
  ): List[MoveReviewPlayerSurfaceRow] =
    val planRows =
      promotedPlans.take(2).flatMap(planRowsFromPromotedPlan)
    val promotedRows = distinctVisibleRows(planRows)
    (
      promotedRows ++ practicalAdvancedRows(
        ctx,
        evaluatedPlans,
        promotedPlans,
        practicalStructureArc,
        slots = 8 - promotedRows.size
      )
    ).take(8)

  private def planRowsFromPromotedPlan(plan: EvaluatedPlan): List[MoveReviewPlayerSurfaceRow] =
    val hypothesis = plan.hypothesis
    val promotedRows =
      row("Execution", hypothesis.executionSteps.take(2).mkString(" - ")).toList ++
        row("Objective", hypothesis.preconditions.take(2).mkString(" - ")).toList
    val prophylaxisRows =
      if isProphylaxisPlan(plan) then
        val text =
          cleanOpt(Some(hypothesis.planName))
            .orElse(cleanOpt(hypothesis.failureModes.headOption))
            .getOrElse("")
        row("Prophylaxis", text).toList
      else Nil
    promotedRows ++ prophylaxisRows

  private def practicalPlanRows(
      ctx: NarrativeContext,
      plans: List[EvaluatedPlan],
      structureArc: Option[StructurePlanArc]
  ): List[MoveReviewPlayerSurfaceRow] =
    plans
      .filter(PlanEvidenceEvaluator.isBoundedPracticalSupportPlan)
      .filterNot(_.userFacingEligibility == UserFacingPlanEligibility.PvCoupledOnly)
      .sortBy(_.hypothesis.rank)
      .flatMap(plan => practicalPlanRow(ctx, plan, structureArc))
      .take(2)

  private def openingFamilyRow(ctx: NarrativeContext): Option[MoveReviewPlayerSurfaceRow] =
    for
      opening <- openingName(ctx)
      family <- OpeningFamilyCatalog.default.familiesForOpening(opening).headOption
      decision <- OpeningFamilyClaimResolver.decideOpeningFamilyClaim(
        OpeningFamilyClaimResolver.OpeningFamilyClaim(family.wireKey),
        OpeningFamilyClaimResolver.OpeningFamilyMatchProof(
          opening = Some(opening),
          phase = openingProofPhase(ctx),
          ply = ctx.ply,
          fen = rawOpt(ctx.fen)
        )
      )
      if decision.supportedLocalWithoutTacticalVeto
      surfaceRow <- row(
        label = "Opening family",
        text = s"The opening context is ${family.displayName}.",
        tone = Some("opening")
      )
    yield surfaceRow.copy(
      authority =
        Some(
          MoveReviewSurfaceAuthority(
            kind = MoveReviewSurfaceAuthority.OpeningFamily,
            openingFamily = Some(family.wireKey),
            target = None,
            openingBook = ctx.openingData.flatMap(MoveReviewOpeningBookMetadata.fromReference)
          )
        )
    )

  private def openingName(ctx: NarrativeContext): Option[String] =
    cleanOpt(ctx.openingData.flatMap(_.name))
      .orElse(openingEventName(ctx).flatMap(name => cleanOpt(Some(name))))

  private def openingEventName(ctx: NarrativeContext): Option[String] =
    ctx.openingEvent.collect {
      case lila.commentary.model.OpeningEvent.Intro(_, name, _, _) => name
    }

  private def openingProofPhase(ctx: NarrativeContext): String =
    rawOpt(ctx.phase.current).orElse(rawOpt(ctx.header.phase)).getOrElse("")

  private def practicalPlanRow(
      ctx: NarrativeContext,
      plan: EvaluatedPlan,
      structureArc: Option[StructurePlanArc]
  ): Option[MoveReviewPlayerSurfaceRow] =
    if plan.userFacingEligibility != UserFacingPlanEligibility.StructuralOnly then None
    else cleanOpt(Some(plan.hypothesis.planName)).flatMap { name =>
      val text =
        structureArcForPlan(name, structureArc)
          .map(arc => s"The ${arc.structureLabel} structure gives $name practical support.")
          .orElse {
            val profile = ctx.semantic.flatMap(_.structureProfile)
            val alignment = ctx.semantic.flatMap(_.planAlignment)
            val alignmentBand =
              alignment
                .flatMap(alignment => cleanOpt(Some(alignment.band)))
                .map(_.toLowerCase)
            val alignmentPlanMatched =
              alignment.toList
                .flatMap(alignment => alignment.matchedPlanIds ++ alignment.narrativeIntent.toList)
                .exists(planTokenMatches(name, _))
            profile
              .filter(_.confidence >= 0.70)
              .flatMap(profile =>
                cleanOpt(Some(profile.primary))
                  .filterNot(_.equalsIgnoreCase("Unknown"))
                  .map(structure => profile -> structure)
              )
              .filter(_ => alignmentBand.exists(band => band == "onbook" || band == "playable"))
              .filter(_ => alignmentPlanMatched)
              .map { case (profile, structure) =>
                val center =
                  cleanOpt(Some(profile.centerState))
                    .filterNot(_.equalsIgnoreCase(structure))
                    .map(value => s" with the center ${value.toLowerCase}")
                    .getOrElse("")
                s"The $structure structure$center gives $name practical support."
              }
          }
          .getOrElse(s"The structure gives $name practical support.")
      row(
        label = "Structure support",
        text = text,
        tone = Some("practical")
      )
    }

  private def practicalAdvancedRows(
      ctx: NarrativeContext,
      plans: List[EvaluatedPlan],
      promotedPlans: List[EvaluatedPlan],
      structureArc: Option[StructurePlanArc],
      slots: Int
  ): List[MoveReviewPlayerSurfaceRow] =
    if slots <= 0 then Nil
    else
      distinctVisibleRows(
        plans
          .filter(PlanEvidenceEvaluator.isBoundedPracticalSupportPlan)
          .filterNot(_.userFacingEligibility == UserFacingPlanEligibility.PvCoupledOnly)
          .filterNot(plan => hasPromotedSibling(plan, promotedPlans))
          .sortBy(_.hypothesis.rank)
          .flatMap(plan => practicalAdvancedRowsForPlan(ctx, plan, structureArc))
      )
        .take(slots)

  private def hasPromotedSibling(plan: EvaluatedPlan, promotedPlans: List[EvaluatedPlan]): Boolean =
    promotedPlans.exists(promoted =>
      sameTheme(plan, promoted) || executionOverlapRatio(plan, promoted) >= 0.7
    )

  private def sameTheme(left: EvaluatedPlan, right: EvaluatedPlan): Boolean =
    val leftTheme = cleanToken(left.hypothesis.themeL1)
    val rightTheme = cleanToken(right.hypothesis.themeL1)
    leftTheme.nonEmpty && leftTheme == rightTheme

  private def executionOverlapRatio(practical: EvaluatedPlan, promoted: EvaluatedPlan): Double =
    val practicalAtoms = executionAtoms(practical.hypothesis.executionSteps)
    if practicalAtoms.isEmpty then 0.0
    else
      val promotedAtoms = executionAtoms(promoted.hypothesis.executionSteps).toSet
      practicalAtoms.count(promotedAtoms.contains).toDouble / practicalAtoms.size.toDouble

  private def executionAtoms(steps: List[String]): List[String] =
    steps.flatMap { step =>
      val token = cleanToken(step)
      val squares = SquareToken.findAllIn(token).toList
      if squares.nonEmpty then squares
      else if token.nonEmpty then List(token)
      else Nil
    }.distinct

  private def cleanToken(value: String): String =
    value
      .replaceAll("""([A-Z]+)([A-Z][a-z])""", "$1 $2")
      .replaceAll("""([a-z0-9])([A-Z])""", "$1 $2")
      .toLowerCase
      .replaceAll("""[^a-z0-9]+""", " ")
      .trim

  private def practicalAdvancedRowsForPlan(
      ctx: NarrativeContext,
      plan: EvaluatedPlan,
      structureArc: Option[StructurePlanArc]
  ): List[MoveReviewPlayerSurfaceRow] =
    val hypothesis = plan.hypothesis
    val structureRouteRow =
      if plan.userFacingEligibility == UserFacingPlanEligibility.StructuralOnly then
        structureArcForPlan(hypothesis.planName, structureArc).flatMap { arc =>
          row("Practical route", StructurePlanArcBuilder.supportPrimaryText(arc), tone = Some("practical"))
        }
      else None
    val structureMoveRow =
      if plan.userFacingEligibility == UserFacingPlanEligibility.StructuralOnly then
        structureArcForPlan(hypothesis.planName, structureArc).flatMap { arc =>
          ctx.playedMove
            .flatMap(move => MoveReviewExchangeAnalyzer.boundedReplay(ctx.fen, List(move), maxPlies = 1))
            .flatMap(_.headOption)
            .flatMap { step =>
              val cue = arc.primaryDeployment
              if step.move.orig.key == cue.from && cue.route.headOption.contains(step.move.dest.key) then
                Some("The played move starts that structure route immediately.")
              else if step.move.orig.key == cue.from && cue.route.drop(1).contains(step.move.dest.key) then
                Some("The played move reaches that structure route directly.")
              else None
            }
            .flatMap(text => row("Practical move", text, tone = Some("practical")))
        }
      else None
    val currentBoardPreventsPlan =
      ctx.semantic.flatMap(_.preventedPlans.headOption).exists { plan =>
        plan.sourceScope == FactScope.Now &&
          (plan.counterplayScoreDrop > 0 || plan.mobilityDelta < 0)
      }
    val structureRestraintRow =
      if plan.userFacingEligibility == UserFacingPlanEligibility.StructuralOnly then
        structureArcForPlan(hypothesis.planName, structureArc)
          .filter(_ => currentBoardPreventsPlan)
          .flatMap(_.prophylaxisSupport)
          .flatMap(text => row("Practical restraint", text, tone = Some("practical")))
      else None
    val structureFitRow =
      if plan.userFacingEligibility == UserFacingPlanEligibility.StructuralOnly then
        structureArcForPlan(hypothesis.planName, structureArc)
          .flatMap { arc =>
            arc.alignmentReasons
              .filterNot(_ == "expected plans are present")
              .headOption
          }
          .flatMap(reason => row("Practical fit", s"The structure fit is still partial: $reason.", tone = Some("practical")))
      else None
    val structureTaskRow =
      if plan.userFacingEligibility == UserFacingPlanEligibility.StructuralOnly then
        structureArcForPlan(hypothesis.planName, structureArc)
          .flatMap(_.practicalCoda)
          .flatMap(text => row("Practical support", text, tone = Some("practical")))
      else None
    (
      practicalTargetRow(ctx, plan).toList ++
        structureRouteRow.toList ++
        structureMoveRow.toList ++
        structureRestraintRow.toList ++
        structureFitRow.toList ++
        structureTaskRow.toList
    ).map(_.copy(authority = PracticalPlanAuthority))

  private def structureArcForPlan(
      planName: String,
      structureArc: Option[StructurePlanArc]
  ): Option[StructurePlanArc] =
    structureArc.filter(arc =>
      planTokenMatches(planName, arc.planLabel) ||
        arc.alignmentPlanIds.exists(planTokenMatches(planName, _))
    )

  private def planTokenMatches(left: String, right: String): Boolean =
    val leftToken = cleanToken(left)
    val rightToken = cleanToken(right)
    val leftWords = leftToken.split("\\s+").count(_.nonEmpty)
    val rightWords = rightToken.split("\\s+").count(_.nonEmpty)
    val containmentFloor = 3
    leftToken.nonEmpty && rightToken.nonEmpty &&
      (
        leftToken == rightToken ||
          (leftWords >= containmentFloor && rightWords > leftWords && rightToken.contains(leftToken)) ||
          (rightWords >= containmentFloor && leftWords > rightWords && leftToken.contains(rightToken))
      )

  private def practicalTargetRow(
      ctx: NarrativeContext,
      plan: EvaluatedPlan
  ): Option[MoveReviewPlayerSurfaceRow] =
    val targetHints = practicalTargetHints(plan)
    if isWeaknessPlan(plan) || targetHints.nonEmpty then
      val planName = cleanToken(plan.hypothesis.planName)
      val evidence = plan.hypothesis.evidenceSources.map(cleanToken)
      val fixedTargetHints =
        plan.hypothesis.evidenceSources.flatMap { source =>
          val lower = Option(source).getOrElse("").trim.toLowerCase
          Option
            .when(lower.startsWith("fixed_target:"))(lower.stripPrefix("fixed_target:").trim)
            .filter(ChessSquarePattern.matches)
        }.toSet
      val carlsbadPlan =
        plan.userFacingEligibility == UserFacingPlanEligibility.StructuralOnly &&
          (
            plan.subplanId.contains(PlanKind.MinorityAttackFixation.id) ||
              plan.subplanId.contains(PlanKind.BackwardPawnTargeting.id) ||
              planName.contains("carlsbad") ||
              planName.contains("minority attack") ||
              evidence.exists(token => token.contains("carlsbad") || token.contains("minority attack"))
          )
      val carlsbadContext =
        if plan.userFacingEligibility == UserFacingPlanEligibility.StructuralOnly &&
          (carlsbadPlan || fixedTargetHints.nonEmpty)
        then
          Fen.read(Standard, Fen.Full(ctx.fen)).flatMap { position =>
            PawnStructureTargets
              .carlsbadTargetForBoard(position.board, position.color)
              .map(target => position.color -> target)
              .filter { case (_, target) => carlsbadPlan || fixedTargetHints.contains(target.targetSquare) }
          }.map { case (pressureSide, target) =>
            practicalTargetRowFor(
              s"The Carlsbad-type pawn shape makes ${target.targetSquare} a natural queenside target for ${sideLabel(pressureSide)}'s minority-attack ideas."
            )
          }
        else None
      carlsbadContext
        .orElse(
          practicalCurrentTarget(ctx, targetHints).map { target =>
            val currentStructurePrefix =
              target.structureContext
                .flatMap(context =>
                  cleanToken(context) match
                    case "unknown" | "open center" | "locked center" | "fluid center" | "symmetric center" => None
                    case "iqp white" | "iqp black" => Some("The current IQP structure gives")
                    case "hanging pawns white" | "hanging pawns black" =>
                      Some("The current hanging-pawn structure gives")
                    case token if token.nonEmpty => Some(s"The current ${humanizeToken(context)} structure gives")
                    case _ => None
                )
                .getOrElse("The current structure gives")
            practicalTargetRowFor(
              s"$currentStructurePrefix ${sideLabel(target.weakSide)} a weak ${targetKindLabel(target.kind)} on ${target.targetSquare} to pressure."
            )
          }
        )
        .orElse(
          practicalEndpointTarget(ctx, targetHints).map(target =>
            practicalTargetRowFor(
              s"The checked line leaves ${sideLabel(target.weakSide)} a weak ${targetKindLabel(target.kind)} on ${target.targetSquare} to pressure."
            )
          )
        )
    else None

  private def practicalCurrentTarget(
      ctx: NarrativeContext,
      targetHints: Set[String]
  ): Option[WeaknessTargetProfile] =
    WeaknessTargetProfile
      .fromFenForMover(ctx.fen)
      .filter(target => targetHints.isEmpty || targetHints.contains(target.targetSquare))
      .find(target => practicalTargetSurvivesBestLine(ctx, target))

  private def practicalEndpointTarget(
      ctx: NarrativeContext,
      targetHints: Set[String]
  ): Option[WeaknessTargetProfile] =
    (if targetHints.nonEmpty then ctx.engineEvidence.flatMap(_.best) else None)
      .filter(line => line.moves.nonEmpty && (line.resultingFen.nonEmpty || line.moves.size >= MinWeaknessTargetOutcomePlies))
      .flatMap(line =>
        WeaknessTargetProfile
          .targetsAfterLineFromFen(
            fen = ctx.fen,
            moves = line.moves,
            resultingFen = line.resultingFen
          )
          .filter(target => targetHints.contains(target.targetSquare))
          .headOption
      )

  private def practicalTargetRowFor(
      text: String
  ): MoveReviewPlayerSurfaceRow =
    row("Practical target", text, tone = Some("practical"))
      .getOrElse(MoveReviewPlayerSurfaceRow(label = "Practical target", text = text, tone = Some("practical")))

  private def practicalTargetHints(plan: EvaluatedPlan): Set[String] =
    WeaknessTargetProfile.targetHintSquares(plan.hypothesis.evidenceSources).toSet

  private def isWeaknessPlan(plan: EvaluatedPlan): Boolean =
    plan.themeL1 == PlanTheme.WeaknessFixation.id ||
      plan.subplanId.exists(id =>
        Set(
          PlanKind.StaticWeaknessFixation.id,
          PlanKind.MinorityAttackFixation.id,
          PlanKind.BackwardPawnTargeting.id,
          PlanKind.IQPInducement.id
        ).contains(id)
      )

  private def sideLabel(side: _root_.chess.Color): String =
    if side.white then "White" else "Black"

  private def targetKindLabel(kind: String): String =
    kind match
      case WeaknessTargetProfile.BackwardPawn => "backward pawn"
      case WeaknessTargetProfile.IsolatedPawn => "isolated pawn"
      case WeaknessTargetProfile.IQP          => "isolated queen pawn"
      case WeaknessTargetProfile.DoubledPawn  => "doubled pawn"
      case WeaknessTargetProfile.FixedPawn    => "fixed pawn"
      case _                                  => "pawn"

  private def practicalTargetSurvivesBestLine(
      ctx: NarrativeContext,
      target: WeaknessTargetProfile
  ): Boolean =
    ctx.engineEvidence.flatMap(_.best) match
      case None => true
      case Some(line) if line.moves.isEmpty => true
      case Some(line) =>
        val outcome =
          WeaknessTargetProfile.lineOutcomeFromFen(
            fen = ctx.fen,
            moves = line.moves,
            targetSquare = target.targetSquare,
            resultingFen = line.resultingFen
          )
        outcome.forall(_.status != WeaknessTargetProfile.LiquidatedByDefense)

  private def enrichDecisionTargetComparison(
      ctx: NarrativeContext,
      comparison: MoveReviewPlayerDecisionComparison
  ): MoveReviewPlayerDecisionComparison =
    if comparison.targetComparison.nonEmpty || comparison.chosenMatchesBest then comparison
    else comparison.copy(targetComparison = bestVsChosenTargetComparison(ctx))

  private def bestVsChosenTargetComparison(ctx: NarrativeContext): Option[MoveReviewDecisionTargetComparison] =
    for
      evidence <- ctx.engineEvidence
      bestLine <- evidence.best
      chosenMove <- ctx.playedMove.map(NarrativeUtils.normalizeUciMove).filter(_.nonEmpty)
      if bestLine.moves.headOption.map(NarrativeUtils.normalizeUciMove).exists(_ != chosenMove)
      chosenLine <- evidence.variations.find(line =>
        line.moves.headOption.map(NarrativeUtils.normalizeUciMove).contains(chosenMove)
      )
      bestTarget <- lineTarget(ctx, bestLine)
      chosenTarget <- lineTarget(ctx, chosenLine)
      if bestTarget.targetSquare != chosenTarget.targetSquare || bestTarget.kind != chosenTarget.kind
    yield MoveReviewDecisionTargetComparison(
      chosenTarget = chosenTarget.targetSquare,
      chosenTargetKind = chosenTarget.kind,
      bestTarget = bestTarget.targetSquare,
      bestTargetKind = bestTarget.kind
    )

  private def lineTarget(
      ctx: NarrativeContext,
      line: lila.commentary.model.strategic.VariationLine
  ): Option[WeaknessTargetProfile] =
    WeaknessTargetProfile
      .targetsAfterLineFromFen(
        fen = ctx.fen,
        moves = line.moves,
        resultingFen = line.resultingFen
      )
      .headOption

  private def explanationSupportRows(
      explanation: MoveReviewExplanation,
      knownSans: Set[String]
  ): List[MoveReviewPlayerSurfaceRow] =
    val sanMoves = cleanLineList(explanation.shortLine.toList.flatMap(_.san))
    val learningPoint =
      explanation.pvInterpretation
        .flatMap(interpretation => cleanOpt(Some(interpretation.learningPoint)))
        .map(value => value.replaceAll("""\.+$""", ""))
    if sanMoves.isEmpty then Nil
    else
      val line = s"Short line: ${sanMoves.mkString(" ")}."
      val text = learningPoint.fold(line)(point => s"$point. $line")
      row(
        label = "Checked line",
        text = text,
        refSans = refSans(sanMoves, knownSans),
        tone = Some("line")
      ).toList

  private def referenceLineRows(
      ctx: NarrativeContext,
      refs: Option[MoveReviewRefs],
      knownSans: Set[String],
      lineConsequence: Option[LineConsequenceEvidence]
  ): List[MoveReviewPlayerSurfaceRow] =
    refs.toList
      .flatMap(ref => preferredVariation(ctx, ref, lineConsequence).toList)
      .flatMap { variation =>
        val sanMoves = cleanLineList(variation.moves.map(_.san)).take(5)
        if sanMoves.nonEmpty then
          row(
            label = "Checked line",
            text = s"Short line: ${sanMoves.mkString(" ")}.",
            refSans = refSans(sanMoves, knownSans),
            tone = Some("line")
          )
        else None
      }

  private def preferredVariation(
      ctx: NarrativeContext,
      refs: MoveReviewRefs,
      lineConsequence: Option[LineConsequenceEvidence]
  ): Option[MoveReviewVariationRef] =
    preferredLineConsequenceVariation(ctx, refs, lineConsequence)
      .orElse(refs.variations.find(startsWithReviewedMove(ctx, _)))
      .orElse(refs.variations.headOption)

  private def preferredLineConsequenceVariation(
      ctx: NarrativeContext,
      refs: MoveReviewRefs,
      lineConsequence: Option[LineConsequenceEvidence]
  ): Option[MoveReviewVariationRef] =
    for
      evidence <- lineConsequence
      if evidence.surfaceReady && evidence.kind != LineConsequenceKind.PreviewOnly
      lineId <- evidence.lineId
      playedUci <- ctx.playedMove.map(NarrativeUtils.normalizeUciMove).filter(_.nonEmpty)
      if evidence.uciMoves.headOption.exists(uci => NarrativeUtils.normalizeUciMove(uci) == playedUci)
      variation <- refs.variations.find(_.lineId == lineId)
      if MoveReviewPvLine.validatedLine(ctx.fen, variation, playedUci).nonEmpty
    yield variation

  private def startsWithReviewedMove(
      ctx: NarrativeContext,
      variation: MoveReviewVariationRef
  ): Boolean =
    variation.moves.headOption.exists(move => reviewedMoveMatches(ctx, move))

  private def reviewedMoveMatches(
      ctx: NarrativeContext,
      move: MoveReviewMoveRef
  ): Boolean =
    val playedUci = ctx.playedMove.map(NarrativeUtils.normalizeUciMove).filter(_.nonEmpty)
    val playedSan = ctx.playedSan.map(normalizeMove).filter(_.nonEmpty)
    playedUci.exists(_ == NarrativeUtils.normalizeUciMove(move.uci)) ||
      playedSan.exists(_ == normalizeMove(move.san))

  private def ledgerRows(
      moveReviewLedger: Option[MoveReviewStrategicLedger],
      knownSans: Set[String]
  ): List[MoveReviewPlayerSurfaceRow] =
    moveReviewLedger.toList.flatMap { ledger =>
      (ledger.primaryLine.toList ++ ledger.resourceLine.toList).flatMap { line =>
        val source = clean(line.source)
        val sanMoves = cleanLineList(line.sanMoves)
        if MoveReviewLedgerLineSources.contains(source) && sanMoves.nonEmpty then
          val text =
            cleanOpt(line.note)
              .orElse(cleanOpt(Some(sanMoves.mkString(" "))))
          row(
            label = line.title,
            text = text.getOrElse(""),
            refSans = refSans(sanMoves, knownSans)
          )
        else None
      }
    }

  private def authorRows(
      authoringSurface: AuthoringEvidenceSurface,
      knownSans: Set[String]
  ): List[MoveReviewPlayerAuthorRow] =
    val evidenceRows =
      authoringSurface.evidence.take(2).flatMap(summary => authorEvidenceRow(summary, knownSans))
    if evidenceRows.nonEmpty then evidenceRows
    else authoringSurface.questions.take(2).flatMap(authorQuestionRow)

  private def authorEvidenceRow(
      summary: AuthorEvidenceSummary,
      knownSans: Set[String]
  ): Option[MoveReviewPlayerAuthorRow] =
    val question = cleanOpt(Some(summary.question))
    question.map { q =>
      MoveReviewPlayerAuthorRow(
        title = humanizeToken(summary.questionKind),
        status = clean(summary.status),
        question = q,
        why = cleanOpt(summary.why),
        meta = Nil,
        branches =
          summary.branches.take(2).flatMap { branch =>
            row(
              label = branch.keyMove,
              text = branch.line,
              refSans = refSans(List(branch.keyMove), knownSans)
            )
          }
      )
    }

  private def authorQuestionRow(summary: AuthorQuestionSummary): Option[MoveReviewPlayerAuthorRow] =
    cleanOpt(Some(summary.question)).map { question =>
      MoveReviewPlayerAuthorRow(
        title = humanizeToken(summary.kind),
        status = "question_only",
        question = question,
        why = cleanOpt(summary.why),
        meta = Nil
      )
    }

  private def isProphylaxisPlan(plan: EvaluatedPlan): Boolean =
    plan.themeL1 == PlanTheme.RestrictionProphylaxis.id ||
      plan.subplanId.exists(id =>
        Set(
          PlanKind.ProphylaxisRestraint.id,
          PlanKind.BreakPrevention.id,
          PlanKind.FlankClamp.id,
          PlanKind.CentralSpaceBind.id,
          PlanKind.MobilitySuppression.id,
          PlanKind.KeySquareDenial.id
        ).contains(id)
      )

  private def row(
      label: String,
      text: String,
      refSans: List[String] = Nil,
      tone: Option[String] = None
  ): Option[MoveReviewPlayerSurfaceRow] =
    for
      cleanLabel <- cleanOpt(Some(label))
      cleanText <- cleanOpt(Some(text))
    yield MoveReviewPlayerSurfaceRow(
      label = cleanLabel,
      text = cleanText,
      tone = tone.flatMap(value => cleanOpt(Some(value))),
      source = None,
      refSans = cleanLineList(refSans)
    )

  private[analysis] def distinctVisibleRows(rows: List[MoveReviewPlayerSurfaceRow]): List[MoveReviewPlayerSurfaceRow] =
    rows.distinctBy(row => (row.label, row.text))

  private def sanitizeRows(
      rows: List[MoveReviewPlayerSurfaceRow],
      knownSans: Set[String]
  ): List[MoveReviewPlayerSurfaceRow] =
    rows.flatMap { sourceRow =>
      for
        cleanLabel <- cleanOpt(Some(sourceRow.label))
        cleanText <- cleanOpt(Some(sourceRow.text))
      yield MoveReviewPlayerSurfaceRow(
        label = cleanLabel,
        text = cleanText,
        tone = sourceRow.tone.flatMap(value => cleanOpt(Some(value))),
        source = None,
        refSans = refSans(sourceRow.refSans, knownSans),
        authority = sourceRow.authority
      )
    }

  private def sanitizeDecisionComparison(
      comparison: MoveReviewPlayerDecisionComparison
  ): MoveReviewPlayerDecisionComparison =
    comparison.copy(
      kicker = clean(comparison.kicker),
      gapLabel = cleanOpt(comparison.gapLabel),
      chosenSan = cleanOpt(comparison.chosenSan),
      engineSan = cleanOpt(comparison.engineSan),
      comparedSan = cleanOpt(comparison.comparedSan),
      secondaryText = cleanOpt(comparison.secondaryText)
    )

  private def hasTwoComparableMoves(digest: DecisionComparisonDigest): Boolean =
    (digest.chosenMove.toList ++ digest.engineBestMove.toList ++ digest.comparedMove.toList)
      .flatMap(move => cleanMove(Some(move)))
      .map(normalizeMove)
      .distinct
      .size >= 2

  private def hasComparableDecisionShape(
      digest: DecisionComparisonDigest,
      evidence: LineConsequenceEvidence
  ): Boolean =
    hasTwoComparableMoves(digest) || hasLaterLineDivergence(digest, evidence)

  private def hasLaterLineDivergence(
      digest: DecisionComparisonDigest,
      evidence: LineConsequenceEvidence
  ): Boolean =
    val compared = cleanMove(digest.comparedMove)
    val anchorMoves = (digest.chosenMove.toList ++ digest.engineBestMove.toList).flatMap(move => cleanMove(Some(move)))
    compared.exists { comparedMove =>
      anchorMoves.exists(anchor => normalizeMove(anchor) == normalizeMove(comparedMove)) &&
        evidence.kind != LineConsequenceKind.PreviewOnly &&
        triggerPly(evidence).exists(_ > 1)
    }

  private def hasDecisionSurfaceReason(digest: DecisionComparisonDigest): Boolean =
    digest.cpLossVsChosen.exists(_ >= MinDecisionSurfaceGapCp) ||
      digest.practicalAlternative ||
      (
        digest.comparativeConsequence.exists(_.trim.nonEmpty) &&
          digest.comparativeSource.exists(_.toLowerCase.contains(ExactComparativeSource))
      )

  private def decisionGapLabel(cp: Int): String =
    if cp >= ClearDecisionSurfaceGapCp then s"${cp}cp"
    else s"${cp}cp slight"

  private def triggerPly(evidence: LineConsequenceEvidence): Option[Int] =
    evidence.triggerSan
      .flatMap(trigger =>
        evidence.sanMoves.indexWhere(san => normalizeMove(san) == normalizeMove(trigger)) match
          case -1  => None
          case idx => Some(idx + 1)
      )

  private def safeDecisionSecondaryText(evidence: LineConsequenceEvidence): Option[String] =
    if evidence.kind == LineConsequenceKind.PreviewOnly then None
    else
      val text = clean(evidence.playerSentence).trim
      if text.nonEmpty then Some(text) else None

  private def cleanMove(raw: Option[String]): Option[String] =
    raw.map(clean).map(_.trim).filter(_.nonEmpty)

  private def normalizeMove(raw: String): String =
    raw.replaceAll("""[+#?!]+$""", "").toLowerCase

  private def refSans(sans: List[String], knownSans: Set[String]): List[String] =
    val cleaned = cleanLineList(sans)
    if knownSans.isEmpty then cleaned
    else cleaned.filter(san => knownSans.contains(normalizeSan(san)))

  private def clean(value: String): String =
    UserFacingSignalSanitizer.sanitize(value)

  private[analysis] def cleanOpt(value: Option[String]): Option[String] =
    value.map(clean).map(_.trim).filter(_.nonEmpty)

  private[analysis] def cleanLineList(values: List[String]): List[String] =
    values.flatMap(value => cleanOpt(Some(value)))

  private def rawOpt(value: String): Option[String] =
    Option(value).map(_.trim).filter(_.nonEmpty)

  private def normalizeSan(value: String): String =
    Option(value).getOrElse("").replaceAll("[+#?!]+", "").trim.toLowerCase

  private def humanizeToken(raw: String): String =
    clean(raw.replaceAll("([a-z])([A-Z])", "$1 $2").replace("_", " ").replace("-", " "))
      .split("\\s+")
      .filter(_.nonEmpty)
      .map(part => part.take(1).toUpperCase + part.drop(1).toLowerCase)
      .mkString(" ")

private[commentary] object MoveReviewSurfaceTruthVeto:

  def truthContractSurfaceVeto(truthContract: Option[DecisiveTruthContract]): Boolean =
    truthContract.exists(_.blocksStrategicSupport)

private[commentary] object BreakSurfaceToken:

  def canonical(raw: String): Option[String] =
    val lower = Option(raw).map(_.trim.toLowerCase).getOrElse("")
    if lower.isEmpty ||
        lower.contains("_") ||
        lower.contains(":") ||
        lower.contains("|") ||
        lower.contains(" ") ||
        lower.contains("counterplay") ||
        lower.contains("neutralize")
    then None
    else
      val hasEllipsis = lower.startsWith("...")
      val core = if hasEllipsis then lower.drop(3) else lower
      if MoveReviewPlayerPayloadBuilder.ChessSquarePattern.matches(core) ||
          MoveReviewPlayerPayloadBuilder.ChessSquareRangePattern.matches(core)
      then
        Some(s"${if hasEllipsis then "..." else ""}$core")
      else None

  def canonicalRoute(raw: String): Option[String] =
    canonical(raw).filter(_.stripPrefix("...").contains("-"))

  def singleSquare(token: String): Option[String] =
    canonical(token)
      .map(_.stripPrefix("..."))
      .filter(MoveReviewPlayerPayloadBuilder.ChessSquarePattern.matches)

  def displayRoute(token: String): String =
    token.stripPrefix("...")

private[commentary] object NeutralizeKeyBreakSurfaceGate:

  val MissingNamedBreak = "surface:named_break_missing"
  val PlayedMoveCollision = "surface:played_move_collision"
  val PlayedMoveUnverified = "surface:played_move_unverified"

  final case class Decision(token: Option[String], rejectReason: Option[String]):
    def admitted: Boolean = token.nonEmpty && rejectReason.isEmpty

  def decideForPlanPacket(
      plan: QuestionPlan,
      packet: PlayerFacingClaimPacket,
      ctx: NarrativeContext
  ): Decision =
    val planToken = plan.timingWitness.flatMap(_.namedBreak).flatMap(BreakSurfaceToken.canonical)
    val packetToken = packetBreakToken(packet)
    (planToken, packetToken) match
      case (Some(left), Some(right)) if left == right =>
        decideToken(left, Some(ctx))
      case _ =>
        Decision(None, Some(MissingNamedBreak))

  def decideForPacket(
      packet: PlayerFacingClaimPacket,
      ctx: NarrativeContext
  ): Decision =
    decideForPacket(packet, Some(ctx))

  def decideForPacket(
      packet: PlayerFacingClaimPacket,
      ctx: Option[NarrativeContext]
  ): Decision =
    packetBreakToken(packet)
      .map(token => decideToken(token, ctx))
      .getOrElse(Decision(None, Some(MissingNamedBreak)))

  def surfaceText(token: String): String =
    s"This stops the $token break before it appears."

  def surfaceText(token: String, packet: PlayerFacingClaimPacket): String =
    pinnedBreakPawn(token, packet)
      .map(pawn => pinnedBreakPawnSurfaceText(token, pawn))
      .getOrElse(surfaceText(token))

  def surfaceAuthority(token: String, packet: PlayerFacingClaimPacket): MoveReviewSurfaceAuthority =
    MoveReviewSurfaceAuthority(
      kind = MoveReviewSurfaceAuthority.CounterplayBreak,
      token = Some(token),
      target = pinnedBreakPawn(token, packet)
    )

  def matchesSurfaceText(token: String, text: String, target: Option[String]): Boolean =
    text == surfaceText(token) ||
      target.exists(pawn => text == pinnedBreakPawnSurfaceText(token, pawn))

  private def packetBreakToken(packet: PlayerFacingClaimPacket): Option[String] =
    packet.proofPathWitness.exactSliceProof.collect {
      case PlayerFacingExactSliceProof.CounterplayAxisSuppression(breakToken) => breakToken
    }.flatMap(BreakSurfaceToken.canonical)

  private def pinnedBreakPawn(token: String, packet: PlayerFacingClaimPacket): Option[String] =
    val packetToken = packetBreakToken(packet)
    val expectedToken = BreakSurfaceToken.canonical(token)
    val terms =
      (
        packet.proofPathWitness.ownerSeedTerms ++
          packet.proofPathWitness.structureTransitionTerms
      ).map(normalize).filter(_.nonEmpty)
    Option
      .when(
        packetToken == expectedToken &&
          terms.contains("break_clamp_mechanism:pinned_pawn")
      ) {
        terms
          .collectFirst {
            case term if term.startsWith("pinned_break_pawn:") =>
              term.stripPrefix("pinned_break_pawn:")
          }
          .filter(MoveReviewPlayerPayloadBuilder.ChessSquarePattern.matches)
      }
      .flatten

  private def pinnedBreakPawnSurfaceText(token: String, pinnedPawn: String): String =
    s"This pins the $pinnedPawn pawn, so the $token break is not available."

  private def normalize(raw: String): String =
    Option(raw).map(_.trim.toLowerCase).getOrElse("")

  private def decideToken(token: String, ctx: Option[NarrativeContext]): Decision =
    BreakSurfaceToken.singleSquare(token).orElse(BreakSurfaceToken.canonicalRoute(token).flatMap(routeDestination)) match
      case Some(square) =>
        ctx.flatMap(legalPlayedTargetSquare) match
          case Some(target) if target == square => Decision(None, Some(PlayedMoveCollision))
          case Some(_)                         => Decision(Some(token), None)
          case None                            => Decision(None, Some(PlayedMoveUnverified))
      case None => Decision(Some(token), None)

  private def routeDestination(route: String): Option[String] =
    route.stripPrefix("...").split("-").lastOption.filter(MoveReviewPlayerPayloadBuilder.ChessSquarePattern.matches)

  private def legalPlayedTargetSquare(ctx: NarrativeContext): Option[String] =
    for
      position <- Fen.read(Standard, Fen.Full(ctx.fen))
      uci <- ctx.playedMove.map(NarrativeUtils.normalizeUciMove).filter(MoveReviewExchangeAnalyzer.isUciMove)
      move <- Uci(uci).collect { case move: Uci.Move => move }.flatMap(position.move(_).toOption)
    yield move.dest.key

private[commentary] object CentralBreakTimingSurfaceGate:

  val MissingExactWitness = "surface:central_break_exact_witness_missing"
  val MalformedBreakToken = "surface:central_break_token_malformed"

  final case class Decision(token: Option[String], rejectReason: Option[String]):
    def admitted: Boolean = token.nonEmpty && rejectReason.isEmpty

  def decide(witness: CentralBreakTimingWitness.Witness): Decision =
    val token = Option(witness.breakToken).map(_.trim).filter(_.nonEmpty)
    token match
      case Some(value) =>
        BreakSurfaceToken.canonicalRoute(value) match
          case Some(canonical) => Decision(Some(canonical), None)
          case None            => Decision(None, Some(MalformedBreakToken))
      case None =>
        Decision(None, Some(MissingExactWitness))

  def surfaceText(witness: CentralBreakTimingWitness.Witness, token: String): String =
    if witness.sourceTags.contains("board:played_break") then
      s"On the checked line, this also plays the $token break at this moment."
    else s"On the checked line, this also leaves the $token break available on this branch."

private[commentary] object MoveReviewSupportedLocalSurfaceRows:
  import MoveReviewPlayerPayloadBuilder.PracticalPlanAuthority

  private val MaxSupportedLocalRows = 3
  private val CounterplayBreakLabel = "Counterplay break"
  private val CentralBreakLabel = "Central break"
  private val CentralLiquidationLabel = "Central liquidation"
  private val CentralChallengeLabel = "Central challenge"
  private val OpeningBreakLabel = "Pawn break"
  private val KnightOutpostLabel = "Knight outpost"
  private val OpeningBreakGoalTokens =
    List("attack", "break", "challenge", "chipper", "equalizer", "expansion", "liberator", "liquidator", "release", "storm")
  private val KingCenterSquares =
    List(_root_.chess.Square.D4, _root_.chess.Square.E4, _root_.chess.Square.D5, _root_.chess.Square.E5)
  private val BishopPairLabel = "Bishop pair"
  private val OppositeColorBishopsLabel = "Opposite-color bishops"
  private val PieceImprovementLabel = "Piece improvement"
  private val KingSafetyLabel = "King safety"
  private val KingActivationLabel = "King activation"
  private val TechnicalConversionLabel = "Technical conversion"
  private val RookLiftLabel = "Rook lift"
  private val SeventhRankEntryLabel = "Seventh-rank entry"
  private val RookBehindPasserLabel = "Rook behind passer"
  private val ConnectedRooksLabel = "Connected rooks"
  private val DoubledRooksLabel = "Doubled rooks"
  private val XRayPressureLabel = "X-ray pressure"
  private val DoubleCheckLabel = "Double check"
  private val DeflectionLabel = "Deflection"
  private val DiscoveredAttackLabel = "Discovered attack"
  private val InterferenceLabel = "Interference"
  private val BatteryPressureLabel = "Battery pressure"
  private val PinPressureLabel = "Pin pressure"
  private val DecoyLabel = "Decoy"
  private val TrappedPieceLabel = "Trapped piece"
  private val DominationLabel = "Domination"
  private val ZwischenzugLabel = "Zwischenzug"
  private val PasserBlockadeLabel = "Passer blockade"
  private val ConnectedPassersLabel = "Connected passers"
  private val OutsidePasserLabel = "Outside passer"
  private val PassedPawnAdvanceLabel = "Passed pawn advance"
  private val RouteDenialLabel = "Route denial"
  private val DualAxisBindLabel = "Break and entry"
  private val CounterplayRestraintLabel = "Counterplay restraint"
  private val ColorComplexLabel = "Color complex"
  private val FileEntryLabel = "File entry"
  private val IqpTargetLabel = "IQP target"
  private val SimplificationLabel = "Simplification"
  private val BackRankMateLabel = "Back-rank mate"
  private val MateNetLabel = "Mate net"
  private val SmotheredMateLabel = "Smothered mate"
  private val ArabianMateLabel = "Arabian mate"
  private val BodensMateLabel = "Boden's mate"
  private val AnastasiaMateLabel = "Anastasia's mate"
  private val HookMateLabel = "Hook mate"
  private val CornerMateLabel = "Corner mate"
  private val GreekGiftLabel = "Greek gift"
  private val StalemateResourceLabel = "Stalemate resource"
  private val PerpetualCheckResourceLabel = "Perpetual check"
  private val OverloadedDefenderLabel = "Overloaded defender"
  private val ForkLabel = "Fork"
  private val HangingPieceLabel = "Hanging piece"
  private val SkewerLabel = "Skewer"
  private val DefenderTradeLabel = "Defender trade"
  private val BadPieceTradeLabel = "Bad piece trade"
  private val QueenTradeLabel = "Queen trade"
  private val MatePatternLabels =
    Map(
      "smothered_mate" -> SmotheredMateLabel,
      "arabian_mate" -> ArabianMateLabel,
      "bodens_mate" -> BodensMateLabel,
      "anastasia_mate" -> AnastasiaMateLabel,
      "hook_mate" -> HookMateLabel,
      "corner_mate" -> CornerMateLabel
    )
  private val MatePatternPhrases =
    Map(
      "smothered_mate" -> "smothered mate",
      "arabian_mate" -> "Arabian mate",
      "bodens_mate" -> "Boden's mate",
      "anastasia_mate" -> "Anastasia's mate",
      "hook_mate" -> "hook mate",
      "corner_mate" -> "corner mate"
    )
  private val PracticalRelationKindByLabel =
    Map(
      XRayPressureLabel -> MoveReviewExchangeAnalyzer.RelationKind.XRay,
      DoubleCheckLabel -> MoveReviewExchangeAnalyzer.RelationKind.DoubleCheck,
      DeflectionLabel -> MoveReviewExchangeAnalyzer.RelationKind.Deflection,
      DiscoveredAttackLabel -> MoveReviewExchangeAnalyzer.RelationKind.DiscoveredAttack,
      InterferenceLabel -> MoveReviewExchangeAnalyzer.RelationKind.Interference,
      BatteryPressureLabel -> MoveReviewExchangeAnalyzer.RelationKind.Battery,
      PinPressureLabel -> MoveReviewExchangeAnalyzer.RelationKind.Pin,
      DecoyLabel -> MoveReviewExchangeAnalyzer.RelationKind.Decoy,
      TrappedPieceLabel -> MoveReviewExchangeAnalyzer.RelationKind.TrappedPiece,
      DominationLabel -> MoveReviewExchangeAnalyzer.RelationKind.Domination,
      ZwischenzugLabel -> MoveReviewExchangeAnalyzer.RelationKind.Zwischenzug,
      BackRankMateLabel -> MoveReviewExchangeAnalyzer.RelationKind.BackRankMate,
      MateNetLabel -> MoveReviewExchangeAnalyzer.RelationKind.MateNet,
      SmotheredMateLabel -> MoveReviewExchangeAnalyzer.RelationKind.MateNet,
      ArabianMateLabel -> MoveReviewExchangeAnalyzer.RelationKind.MateNet,
      BodensMateLabel -> MoveReviewExchangeAnalyzer.RelationKind.MateNet,
      AnastasiaMateLabel -> MoveReviewExchangeAnalyzer.RelationKind.MateNet,
      HookMateLabel -> MoveReviewExchangeAnalyzer.RelationKind.MateNet,
      CornerMateLabel -> MoveReviewExchangeAnalyzer.RelationKind.MateNet,
      GreekGiftLabel -> MoveReviewExchangeAnalyzer.RelationKind.GreekGift,
      StalemateResourceLabel -> MoveReviewExchangeAnalyzer.RelationKind.StalemateTrap,
      PerpetualCheckResourceLabel -> MoveReviewExchangeAnalyzer.RelationKind.PerpetualCheck,
      OverloadedDefenderLabel -> MoveReviewExchangeAnalyzer.RelationKind.Overload,
      ForkLabel -> MoveReviewExchangeAnalyzer.RelationKind.Fork,
      HangingPieceLabel -> MoveReviewExchangeAnalyzer.RelationKind.HangingPiece,
      SkewerLabel -> MoveReviewExchangeAnalyzer.RelationKind.Skewer,
      DefenderTradeLabel -> MoveReviewExchangeAnalyzer.RelationKind.DefenderTrade,
      BadPieceTradeLabel -> MoveReviewExchangeAnalyzer.RelationKind.BadPieceLiquidation
    )
  private val IqpInducementFamily =
    ProofFamilyId.fromPlanKind(PlanKind.IQPInducement).map(_.wireKey).getOrElse(PlanKind.IQPInducement.id)
  private val SimplificationWindowFamily =
    ProofFamilyId.fromPlanKind(PlanKind.SimplificationWindow).map(_.wireKey).getOrElse(PlanKind.SimplificationWindow.id)
  private val DefenderTradeFamily = ProofFamilyId.fromPlanKind(PlanKind.DefenderTrade).map(_.wireKey).getOrElse(PlanKind.DefenderTrade.id)
  private val BadPieceLiquidationFamily =
    ProofFamilyId.fromPlanKind(PlanKind.BadPieceLiquidation).map(_.wireKey).getOrElse(PlanKind.BadPieceLiquidation.id)
  private val QueenTradeShieldFamily =
    ProofFamilyId.fromPlanKind(PlanKind.QueenTradeShield).map(_.wireKey).getOrElse(PlanKind.QueenTradeShield.id)
  private val PositionProbeProofSources =
    Set(
      ProofSourceId.ExactTargetFixation.wireKey,
      ProofSourceId.CarlsbadFixedTargetProbe.wireKey,
      ProofSourceId.TargetFocusedCoordinationProbe.wireKey,
      ProofSourceId.ColorComplexSqueezeProbe.wireKey
    )

  private[commentary] def relationKindsForRows(rows: List[MoveReviewPlayerSurfaceRow]): Set[String] =
    rows
      .flatMap { row =>
        row.authority.flatMap {
          case authority if authority.kind == MoveReviewSurfaceAuthority.StrategicRelation =>
            authority.token
          case authority if authority.kind == MoveReviewSurfaceAuthority.PracticalPlan =>
            PracticalRelationKindByLabel.get(row.label)
          case _ =>
            None
        }
      }
      .toSet

  def build(
      ctx: NarrativeContext,
      inputs: QuestionPlannerInputs,
      rankedPlans: RankedQuestionPlans,
      truthContract: Option[DecisiveTruthContract]
  ): List[MoveReviewPlayerSurfaceRow] =
    val planRows =
      (rankedPlans.primary.toList ++ rankedPlans.secondary.toList)
        .flatMap(plan => rowForPlan(ctx, inputs, truthContract, plan))
    val packetRows =
      mainPathClaims(inputs)
        .flatMap(claim => rowForClaim(ctx, inputs, truthContract, claim))
    val packetAndPlanRows =
      MoveReviewPlayerPayloadBuilder.distinctVisibleRows(planRows ++ packetRows)
    val typedSurfaceRows =
      MoveReviewPlayerPayloadBuilder.distinctVisibleRows(
        (
          namedRouteNetworkRow(ctx, inputs, truthContract).toList ++
            dualAxisBindRow(ctx, inputs, truthContract).toList ++
            restrictedDefenseConversionRow(ctx, inputs, truthContract).toList
        )
      )
    val exactRows =
      if packetAndPlanRows.nonEmpty && typedSurfaceRows.nonEmpty then
        MoveReviewPlayerPayloadBuilder.distinctVisibleRows(
          packetAndPlanRows.take(1) ++ typedSurfaceRows.take(MaxSupportedLocalRows - 1)
        )
          .take(MaxSupportedLocalRows)
      else (packetAndPlanRows ++ typedSurfaceRows).take(MaxSupportedLocalRows)
    if exactRows.nonEmpty then exactRows
    else if tacticalPracticalVeto(inputs, truthContract) then Nil
    else
      lazy val topPvReplayCache = PlayedTopPvReplayCache(ctx)
      backRankMatePracticalRow(topPvReplayCache)
        .orElse(mateNetPracticalRow(topPvReplayCache))
        .orElse(greekGiftPracticalRow(topPvReplayCache))
        .orElse(drawResourcePracticalRow(ctx, topPvReplayCache))
        .orElse(doubleCheckPracticalRow(topPvReplayCache))
        .orElse(defenderTradePracticalRow(ctx, topPvReplayCache))
        .orElse(badPieceTradePracticalRow(topPvReplayCache))
        .orElse(queenTradePracticalRow(topPvReplayCache))
        .orElse(zwischenzugPracticalRow(ctx, topPvReplayCache))
        .orElse(trappedPiecePracticalRow(ctx, topPvReplayCache))
        .orElse(dominationPracticalRow(ctx, topPvReplayCache))
        .orElse(forkPracticalRow(topPvReplayCache))
        .orElse(overloadPracticalRow(topPvReplayCache))
        .orElse(decoyPracticalRow(topPvReplayCache))
        .orElse(deflectionPracticalRow(topPvReplayCache))
        .orElse(discoveredAttackPracticalRow(topPvReplayCache))
        .orElse(hangingPiecePracticalRow(topPvReplayCache))
        .orElse(skewerPracticalRow(topPvReplayCache))
        .orElse(xrayPracticalRow(topPvReplayCache))
        .orElse(interferencePracticalRow(topPvReplayCache))
        .orElse(batteryPracticalRow(topPvReplayCache))
        .orElse(pinPracticalRow(topPvReplayCache))
        .orElse(practicalCentralRow(ctx))
        .orElse(openingPracticalGoalRow(ctx, topPvReplayCache))
        .orElse(knightOutpostPracticalRow(topPvReplayCache))
        .orElse(bishopPairPracticalRow(topPvReplayCache))
        .orElse(oppositeColorBishopsPracticalRow(topPvReplayCache))
        .orElse(flankPawnPracticalRow(ctx, topPvReplayCache))
        .orElse(rookLiftPracticalRow(topPvReplayCache))
        .orElse(seventhRankPracticalRow(topPvReplayCache))
        .orElse(rookBehindPasserPracticalRow(topPvReplayCache))
        .orElse(passerBlockadePracticalRow(topPvReplayCache))
        .orElse(fileEntryPracticalRow(topPvReplayCache))
        .orElse(connectedRooksPracticalRow(topPvReplayCache))
        .orElse(doubledRooksPracticalRow(topPvReplayCache))
        .orElse(connectedPassersPracticalRow(topPvReplayCache))
        .orElse(outsidePasserPracticalRow(topPvReplayCache))
        .orElse(passedPawnPracticalRow(topPvReplayCache))
        .orElse(kingActivationPracticalRow(topPvReplayCache))
        .orElse(quietIntentRow(ctx, inputs))
        .toList

  private def practicalCentralRow(ctx: NarrativeContext): Option[MoveReviewPlayerSurfaceRow] =
    CentralBreakTimingWitness.practical(ctx).flatMap { practical =>
      BreakSurfaceToken.canonicalRoute(practical.token).flatMap { token =>
        practical.kind match
          case CentralBreakTimingWitness.PracticalKind.Liquidation =>
            row(
              CentralLiquidationLabel,
              s"The move releases central tension through ${BreakSurfaceToken.displayRoute(token)}.",
              authority = Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.CentralLiquidation, token = Some(token)))
            )
          case CentralBreakTimingWitness.PracticalKind.Challenge =>
            row(
              CentralChallengeLabel,
              s"The move challenges the center through ${BreakSurfaceToken.displayRoute(token)}.",
              authority = Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.CentralChallenge, token = Some(token)))
            )
      }
    }

  private def openingPracticalGoalRow(
      ctx: NarrativeContext,
      replayCache: PlayedTopPvReplayCache
  ): Option[MoveReviewPlayerSurfaceRow] =
    ctx.openingGoalEvaluation.flatMap { goal =>
      openingBreakRoute(ctx, goal, replayCache).flatMap { token =>
        row(
          OpeningBreakLabel,
          openingBreakText(goal, token),
          authority = PracticalPlanAuthority
        )
      }
    }

  private def openingBreakRoute(
      ctx: NarrativeContext,
      goal: OpeningGoals.Evaluation,
      replayCache: PlayedTopPvReplayCache
  ): Option[String] =
    Option
      .when(
        openingGoalPracticalStatus(goal) &&
          goal.confidence >= 0.70 &&
          openingBreakGoalName(goal.goalName) &&
          openingGoalTriggeredByPlayedMove(ctx, goal)
      ) {
        replayCache.playedMove.flatMap { case (_, position, move) =>
          Option
            .when(
              move.piece.role == _root_.chess.Pawn &&
                move.piece.color == position.color &&
                move.dest.file.value >= 1 &&
                move.dest.file.value <= 5
            )(s"${move.orig.key}-${move.dest.key}")
            .flatMap(BreakSurfaceToken.canonicalRoute)
        }
      }
      .flatten

  private def openingGoalTriggeredByPlayedMove(ctx: NarrativeContext, goal: OpeningGoals.Evaluation): Boolean =
    ctx.playedMove
      .map(MoveReviewPvLine.normalizeUci)
      .filter(MoveReviewExchangeAnalyzer.isUciMove)
      .exists(played =>
        OpeningGoals.allGoals.exists(definition =>
          Option(goal.goalName).exists(name => definition.name.equalsIgnoreCase(name.trim)) &&
            definition.triggers(played)
        )
      )

  private def openingGoalPracticalStatus(goal: OpeningGoals.Evaluation): Boolean =
    goal.status == OpeningGoals.Status.Achieved || goal.status == OpeningGoals.Status.Partial

  private def openingBreakText(goal: OpeningGoals.Evaluation, token: String): String =
    val route = BreakSurfaceToken.displayRoute(token)
    goal.status match
      case OpeningGoals.Status.Achieved =>
        s"The checked line plays the $route pawn break."
      case OpeningGoals.Status.Partial =>
        s"The checked line plays the $route pawn break, but ${openingBreakCaution(goal.missingEvidence)} still needs care."
      case _ =>
        s"The $route pawn move remains only an opening cue."

  private def openingBreakCaution(missingEvidence: List[String]): String =
    missingEvidence
      .map(_.trim.toLowerCase)
      .find(_.nonEmpty)
      .getOrElse("the follow-up")

  private def openingBreakGoalName(name: String): Boolean =
    val lower = Option(name).getOrElse("").trim.toLowerCase
    lower.nonEmpty &&
      !lower.contains("preparation") &&
      OpeningBreakGoalTokens.exists(lower.contains)

  private def playedTopPvReplay(
      ctx: NarrativeContext,
      maxPlies: Int
  ): Option[List[MoveReviewExchangeAnalyzer.BoundedReplayStep]] =
    for
      played <- ctx.playedMove.map(MoveReviewPvLine.normalizeUci).filter(MoveReviewExchangeAnalyzer.isUciMove)
      replay <- MoveReviewExchangeAnalyzer
        .boundedTopReplay(ctx.fen, ctx.engineEvidence.toList.flatMap(_.variations), maxPlies = maxPlies)
      if replay.headOption.exists(_.uci == played)
    yield replay

  private final class PlayedTopPvReplayCache(ctx: NarrativeContext):
    private val replays =
      scala.collection.mutable.Map.empty[Int, Option[List[MoveReviewExchangeAnalyzer.BoundedReplayStep]]]

    def replay(maxPlies: Int): Option[List[MoveReviewExchangeAnalyzer.BoundedReplayStep]] =
      replays.getOrElseUpdate(maxPlies, playedTopPvReplay(ctx, maxPlies))

    private lazy val playedStep: Option[MoveReviewExchangeAnalyzer.BoundedReplayStep] =
      replay(maxPlies = 1).flatMap(_.headOption)

    lazy val playedMove: Option[(String, chess.Position, chess.Move)] =
      playedStep.map(step => (step.uci, step.before, step.move))

    lazy val playedMoveAfter: Option[(String, chess.Position, chess.Move, chess.Position)] =
      playedStep.map(step => (step.uci, step.before, step.move, step.after))

    def sanMoves(maxPlies: Int): List[String] =
      sanMovesFor(replay(maxPlies).toList.flatten.map(_.uci))

    def sanMovesFor(uciMoves: List[String]): List[String] =
      NarrativeUtils
        .uciListToSan(ctx.fen, uciMoves.take(5))
        .map(_.trim)
        .filter(_.nonEmpty)

  private def explicitTargetSquares(ctx: NarrativeContext): List[String] =
    ctx.decision.toList
      .flatMap(_.focalPoint.collect { case TargetSquare(key) => key })
      .map(_.trim.toLowerCase)
      .filter(MoveReviewPlayerPayloadBuilder.ChessSquarePattern.matches)
      .distinct

  private def playedTopPvRelationRow(
      replayCache: PlayedTopPvReplayCache,
      maxPlies: Int,
      label: String
  )(
      witnessFromReplay: (List[MoveReviewExchangeAnalyzer.BoundedReplayStep], String) => Option[MoveReviewExchangeAnalyzer.RelationWitness],
      surfaceFromWitness: MoveReviewExchangeAnalyzer.RelationWitness => Option[MoveReviewExchangeAnalyzer.RelationPracticalSurface] =
        witness => MoveReviewExchangeAnalyzer.relationPracticalSurfaceFromWitness(witness),
      labelFromSurface: MoveReviewExchangeAnalyzer.RelationPracticalSurface => Option[String] = _ => None
  ): Option[MoveReviewPlayerSurfaceRow] =
    for
      replay <- replayCache.replay(maxPlies)
      played <- replay.headOption.map(_.uci)
      witness <- witnessFromReplay(replay, played)
      surface <- surfaceFromWitness(witness)
      witnessRefs = replayCache.sanMovesFor(witness.lineMoves)
      refs = if witnessRefs.nonEmpty then witnessRefs else replayCache.sanMoves(maxPlies)
      row <- row(
        labelFromSurface(surface).getOrElse(label),
        surface.text,
        refSans = refs,
        authority = PracticalPlanAuthority
      )
    yield row

  private def knightOutpostPracticalRow(replayCache: PlayedTopPvReplayCache): Option[MoveReviewPlayerSurfaceRow] =
    for
      (_, position, move, after) <- replayCache.playedMoveAfter
      if knightOutpostMove(position, move)
      if after.board.pieceAt(move.dest).exists(piece => piece.color == move.piece.color && piece.role == _root_.chess.Knight)
      if outpostSquare(after.board, move.dest, move.piece.color)
      if topPvKeepsPracticalKnightOutpost(replayCache, move.dest, move.piece.color)
      row <- row(
        KnightOutpostLabel,
        s"The checked line puts the knight on the pawn-supported ${move.dest.key} outpost square.",
        authority =
          Some(
            MoveReviewSurfaceAuthority(
              kind = MoveReviewSurfaceAuthority.PracticalPlan,
              target = Some(move.dest.key)
            )
          )
      )
    yield row

  private def knightOutpostMove(position: chess.Position, move: chess.Move): Boolean =
    !move.captures &&
      move.piece.role == _root_.chess.Knight &&
      move.piece.color == position.color &&
      advancedOutpostSquare(move.dest, move.piece.color)

  private def outpostSquare(board: chess.Board, square: chess.Square, color: chess.Color): Boolean =
    val supportedByPawn =
      board.attackers(square, color).intersects(board.byPiece(color, _root_.chess.Pawn))
    val attackedByEnemyPawn =
      board.attackers(square, !color).intersects(board.byPiece(!color, _root_.chess.Pawn))
    val attackedByEnemyMinor =
      board.attackers(square, !color).intersects(
        board.byPiece(!color, _root_.chess.Knight) | board.byPiece(!color, _root_.chess.Bishop)
      )
    supportedByPawn && !attackedByEnemyPawn && !attackedByEnemyMinor

  private def topPvKeepsPracticalKnightOutpost(
      replayCache: PlayedTopPvReplayCache,
      square: chess.Square,
      color: chess.Color
  ): Boolean =
    replayCache.replay(maxPlies = 3).exists { replay =>
      replay.size >= 2 &&
        replay.headOption.exists(step =>
          step.move.dest == square &&
            step.move.piece.color == color &&
            step.move.piece.role == _root_.chess.Knight
        ) &&
        replay.drop(1).take(2).forall(step =>
          step.after.board
            .pieceAt(square)
            .exists(piece => piece.color == color && piece.role == _root_.chess.Knight)
        )
    }

  private def advancedOutpostSquare(square: chess.Square, color: chess.Color): Boolean =
    if color.white then square.rank.value >= _root_.chess.Rank.Fourth.value
    else square.rank.value <= _root_.chess.Rank.Fifth.value

  private def bishopPairPracticalRow(replayCache: PlayedTopPvReplayCache): Option[MoveReviewPlayerSurfaceRow] =
    for
      (_, position, move, after) <- replayCache.playedMoveAfter
      if bishopPairCaptureMove(position, move)
      if after.board.pieceAt(move.dest).exists(piece => piece.color == move.piece.color && piece.role == _root_.chess.Bishop)
      if bishopPairFor(after.board, move.piece.color) && !bishopPairFor(after.board, !move.piece.color)
      row <- row(
        BishopPairLabel,
        "The checked capture keeps the bishop pair on the board.",
        authority = PracticalPlanAuthority
      )
    yield row

  private def bishopPairCaptureMove(position: chess.Position, move: chess.Move): Boolean =
    move.captures &&
      move.piece.role == _root_.chess.Bishop &&
      move.piece.color == position.color

  private def bishopPairFor(board: chess.Board, color: chess.Color): Boolean =
    board.byPiece(color, _root_.chess.Bishop).count >= 2

  private def oppositeColorBishopsPracticalRow(replayCache: PlayedTopPvReplayCache): Option[MoveReviewPlayerSurfaceRow] =
    for
      (_, position, move, after) <- replayCache.playedMoveAfter
      if materialClarifyingCapture(position, move)
      if oppositeColorBishopsOnly(after.board)
      row <- row(
        OppositeColorBishopsLabel,
        "The checked capture leaves opposite-colored bishops on the board.",
        authority = PracticalPlanAuthority
      )
    yield row

  private def materialClarifyingCapture(position: chess.Position, move: chess.Move): Boolean =
    move.captures && move.piece.color == position.color

  private def oppositeColorBishopsOnly(board: chess.Board): Boolean =
    val whiteBishop = board.byPiece(_root_.chess.Color.White, _root_.chess.Bishop).squares.headOption
    val blackBishop = board.byPiece(_root_.chess.Color.Black, _root_.chess.Bishop).squares.headOption
    board.byPiece(_root_.chess.Color.White, _root_.chess.Bishop).count == 1 &&
      board.byPiece(_root_.chess.Color.Black, _root_.chess.Bishop).count == 1 &&
      board.queens.isEmpty &&
      board.rooks.isEmpty &&
      board.knights.isEmpty &&
      whiteBishop.zip(blackBishop).exists { case (white, black) => white.isLight != black.isLight }

  private def quietIntentRow(
      ctx: NarrativeContext,
      inputs: QuestionPlannerInputs
  ): Option[MoveReviewPlayerSurfaceRow] =
    inputs.quietIntent
      .filter(quietIntentPubliclySupported)
      .flatMap(intent => quietIntentLegalMove(ctx, intent).map(move => intent -> move))
      .flatMap { case (intent, move) =>
        val square = move.dest.key
        val piece = quietRoleLabel(move.piece.role)
        intent.intentClass match
          case QuietMoveIntentClass.PieceImprovement =>
            row(
              PieceImprovementLabel,
              s"The checked move improves the $piece by placing it on $square.",
              authority = PracticalPlanAuthority
            )
          case QuietMoveIntentClass.KingSafety =>
            val text =
              if quietCastleMove(move) then "The checked move castles to improve king safety."
              else s"The checked move brings the king to $square for safety."
            row(KingSafetyLabel, text, authority = PracticalPlanAuthority)
          case QuietMoveIntentClass.TechnicalConversionStep =>
            row(
              PieceImprovementLabel,
              s"The checked move improves the $piece on $square for endgame handling.",
              authority = PracticalPlanAuthority
            )
          case QuietMoveIntentClass.CounterplayRestraint => None
      }

  private def flankPawnPracticalRow(
      ctx: NarrativeContext,
      replayCache: PlayedTopPvReplayCache
  ): Option[MoveReviewPlayerSurfaceRow] =
    for
      (_, _, move, afterPosition) <- replayCache.playedMoveAfter
      if rookPawnAdvance(move)
      before <- PositionAnalyzer.extractStrategicState(ctx.fen)
      after <- PositionAnalyzer.extractStrategicState(Fen.write(afterPosition).value)
      row <- flankPawnRowFor(move, before, after)
    yield row

  private def flankPawnRowFor(
      move: chess.Move,
      before: StrategicStateFeatures,
      after: StrategicStateFeatures
  ): Option[MoveReviewPlayerSurfaceRow] =
    CommentaryIdeaSurface
      .flankPawnPracticalFact(move.piece.color, move.dest, before, after)
      .flatMap(fact => row(fact.label, fact.text, authority = PracticalPlanAuthority))

  private def rookPawnAdvance(move: chess.Move): Boolean =
    !move.captures &&
      move.piece.role == _root_.chess.Pawn &&
      move.orig.file == move.dest.file &&
      (move.dest.file == _root_.chess.File.A || move.dest.file == _root_.chess.File.H)

  private def rookLiftPracticalRow(replayCache: PlayedTopPvReplayCache): Option[MoveReviewPlayerSurfaceRow] =
    for
      (_, position, move) <- replayCache.playedMove
      if rookLiftMove(position, move)
      row <- row(
        RookLiftLabel,
        s"The checked line lifts the rook to ${move.dest.key} as attacking infrastructure.",
        authority = PracticalPlanAuthority
      )
    yield row

  private def rookLiftMove(position: chess.Position, move: chess.Move): Boolean =
    val backRank = if move.piece.color.white then _root_.chess.Rank.First else _root_.chess.Rank.Eighth
    val liftRank = if move.piece.color.white then _root_.chess.Rank.Third else _root_.chess.Rank.Sixth
    !move.captures &&
      move.piece.role == _root_.chess.Rook &&
      move.piece.color == position.color &&
      move.orig.rank == backRank &&
      move.dest.rank == liftRank

  private def seventhRankPracticalRow(replayCache: PlayedTopPvReplayCache): Option[MoveReviewPlayerSurfaceRow] =
    for
      (_, position, move, after) <- replayCache.playedMoveAfter
      if seventhRankEntryMove(position, move)
      if after.board.pieceAt(move.dest).exists(piece => piece.color == move.piece.color && piece.role == _root_.chess.Rook)
      row <- row(
        SeventhRankEntryLabel,
        seventhRankEntryText(move),
        authority = PracticalPlanAuthority
      )
    yield row

  private def seventhRankEntryMove(position: chess.Position, move: chess.Move): Boolean =
    val entryRank = if move.piece.color.white then _root_.chess.Rank.Seventh else _root_.chess.Rank.Second
    !move.captures &&
      move.piece.role == _root_.chess.Rook &&
      move.piece.color == position.color &&
      move.dest.rank == entryRank &&
      move.orig.rank != entryRank

  private def seventhRankEntryText(move: chess.Move): String =
    val rankLabel = if move.piece.color.white then "seventh rank" else "second rank"
    s"The checked line puts the rook on the $rankLabel at ${move.dest.key}."

  private def rookBehindPasserPracticalRow(replayCache: PlayedTopPvReplayCache): Option[MoveReviewPlayerSurfaceRow] =
    for
      (_, position, move, after) <- replayCache.playedMoveAfter
      if rookBehindPasserMove(position, move)
      pawn <- rookBehindPassedPawn(after, move)
      row <- row(
        RookBehindPasserLabel,
        s"The checked line places the rook behind the passed pawn on ${pawn.key}.",
        authority = PracticalPlanAuthority
      )
    yield row

  private def rookBehindPasserMove(position: chess.Position, move: chess.Move): Boolean =
    !move.captures &&
      move.piece.role == _root_.chess.Rook &&
      move.piece.color == position.color

  private def rookBehindPassedPawn(position: chess.Position, move: chess.Move): Option[chess.Square] =
    if position.board.occupied.count <= 12 then
      val color = move.piece.color
      val passers = PositionAnalyzer.passedPawns(color, pawnsFor(position.board, color), pawnsFor(position.board, !color))
      passers
        .filter(_.file == move.dest.file)
        .filter(pawn => if color.white then move.dest.rank.value < pawn.rank.value else move.dest.rank.value > pawn.rank.value)
        .sortBy(pawn => math.abs(pawn.rank.value - move.dest.rank.value))
        .headOption
        .filter(_ => position.board.pieceAt(move.dest).exists(piece => piece.color == color && piece.role == _root_.chess.Rook))
    else None

  private def passerBlockadePracticalRow(replayCache: PlayedTopPvReplayCache): Option[MoveReviewPlayerSurfaceRow] =
    for
      (_, position, move, after) <- replayCache.playedMoveAfter
      if passerBlockadeMove(position, move)
      pawn <- blockadedPassedPawn(after, move)
      row <- row(
        PasserBlockadeLabel,
        s"The checked line blockades the passed pawn on ${pawn.key} with the knight.",
        authority = PracticalPlanAuthority
      )
    yield row

  private def passerBlockadeMove(position: chess.Position, move: chess.Move): Boolean =
    !move.captures &&
      move.piece.role == _root_.chess.Knight &&
      move.piece.color == position.color

  private def blockadedPassedPawn(position: chess.Position, move: chess.Move): Option[chess.Square] =
    val pawnColor = !move.piece.color
    val passers = PositionAnalyzer.passedPawns(pawnColor, pawnsFor(position.board, pawnColor), pawnsFor(position.board, !pawnColor))
    passers
      .filter(pawn => advancedPassedPawn(pawn, pawnColor))
      .find(pawn => passedPawnStopSquare(pawn, pawnColor).contains(move.dest))
      .filter(_ => position.board.pieceAt(move.dest).exists(piece => piece.color == move.piece.color && piece.role == _root_.chess.Knight))

  private def passedPawnStopSquare(square: chess.Square, color: chess.Color): Option[chess.Square] =
    val rank = square.rank.value + (if color.white then 1 else -1)
    _root_.chess.Square.at(square.file.value, rank)

  private def advancedPassedPawn(square: chess.Square, color: chess.Color): Boolean =
    if color.white then square.rank.value >= _root_.chess.Rank.Fourth.value
    else square.rank.value <= _root_.chess.Rank.Fifth.value

  private def fileEntryPracticalRow(replayCache: PlayedTopPvReplayCache): Option[MoveReviewPlayerSurfaceRow] =
    for
      (_, position, move, after) <- replayCache.playedMoveAfter
      if majorFileEntryMove(position, move)
      if after.board.pieceAt(move.dest).exists(piece => piece.color == move.piece.color && piece.role == move.piece.role)
      fileKind <- fileEntryKind(after.board, move)
      row <- row(
        FileEntryLabel,
        s"The checked line places the ${quietRoleLabel(move.piece.role)} on the $fileKind ${move.dest.file.char.toString.toLowerCase}-file.",
        refSans = replayCache.sanMoves(1),
        authority = PracticalPlanAuthority
      )
    yield row

  private def majorFileEntryMove(position: chess.Position, move: chess.Move): Boolean =
    val backRank = if move.piece.color.white then _root_.chess.Rank.First else _root_.chess.Rank.Eighth
    !move.captures &&
      (move.piece.role == _root_.chess.Rook || move.piece.role == _root_.chess.Queen) &&
      move.piece.color == position.color &&
      move.orig.file != move.dest.file &&
      move.dest.rank != backRank

  private def fileEntryKind(board: chess.Board, move: chess.Move): Option[String] =
    if openFile(board, move.dest.file) then Some("open")
    else if semiOpenFileFor(board, move.dest.file, move.piece.color) then Some("semi-open")
    else None

  private def openFile(board: chess.Board, file: chess.File): Boolean =
    (board.pawns & _root_.chess.Bitboard.file(file)).isEmpty

  private def semiOpenFileFor(board: chess.Board, file: chess.File, color: chess.Color): Boolean =
    val mask = _root_.chess.Bitboard.file(file)
    val ours = board.pawns & board.byColor(color) & mask
    val theirs = board.pawns & board.byColor(!color) & mask
    ours.isEmpty && theirs.nonEmpty

  private def xrayPracticalRow(replayCache: PlayedTopPvReplayCache): Option[MoveReviewPlayerSurfaceRow] =
    playedTopPvRelationRow(replayCache, maxPlies = 1, label = XRayPressureLabel)(
      (replay, played) => MoveReviewExchangeAnalyzer.xrayWitness(replay, played)
    )

  private def deflectionPracticalRow(replayCache: PlayedTopPvReplayCache): Option[MoveReviewPlayerSurfaceRow] =
    playedTopPvRelationRow(replayCache, maxPlies = 2, label = DeflectionLabel)(
      (replay, played) => MoveReviewExchangeAnalyzer.deflectionWitness(replay, played)
    )

  private def discoveredAttackPracticalRow(replayCache: PlayedTopPvReplayCache): Option[MoveReviewPlayerSurfaceRow] =
    playedTopPvRelationRow(replayCache, maxPlies = 1, label = DiscoveredAttackLabel)(
      (replay, played) => MoveReviewExchangeAnalyzer.discoveredAttackWitness(replay, played)
    )

  private def backRankMatePracticalRow(replayCache: PlayedTopPvReplayCache): Option[MoveReviewPlayerSurfaceRow] =
    playedTopPvRelationRow(replayCache, maxPlies = 1, label = BackRankMateLabel)(
      (replay, played) => MoveReviewExchangeAnalyzer.backRankMateWitness(replay, played)
    )

  private def mateNetPracticalRow(replayCache: PlayedTopPvReplayCache): Option[MoveReviewPlayerSurfaceRow] =
    playedTopPvRelationRow(
      replayCache,
      maxPlies = 1,
      label = MateNetLabel
    )(
      (replay, played) => MoveReviewExchangeAnalyzer.mateNetWitness(replay, played),
      surfaceFromWitness = witness =>
        MoveReviewExchangeAnalyzer.relationPracticalSurfaceFromWitness(witness, patternId => MatePatternPhrases.get(patternId)),
      labelFromSurface = surface => surface.patternId.flatMap(MatePatternLabels.get)
    )

  private def greekGiftPracticalRow(replayCache: PlayedTopPvReplayCache): Option[MoveReviewPlayerSurfaceRow] =
    playedTopPvRelationRow(replayCache, maxPlies = 8, label = GreekGiftLabel)(
      (replay, played) =>
        MoveReviewExchangeAnalyzer.greekGiftWitness(
          replay.take(1),
          played,
          continuationLines = List(replay.map(_.uci))
        )
    )

  private def drawResourcePracticalRow(
      ctx: NarrativeContext,
      replayCache: PlayedTopPvReplayCache
  ): Option[MoveReviewPlayerSurfaceRow] =
    ctx.engineEvidence.flatMap(_.best).flatMap { topLine =>
      for
        replay <- replayCache.replay(maxPlies = 12)
        played <- replay.headOption.map(_.uci)
        witness <-
          MoveReviewExchangeAnalyzer
            .stalemateTrapWitness(replay, played, engineScoreCp = Some(topLine.scoreCp), engineMate = topLine.mate)
            .orElse(
              MoveReviewExchangeAnalyzer.perpetualCheckWitness(
                replay,
                played,
                engineScoreCp = Some(topLine.scoreCp),
                engineMate = topLine.mate
              )
            )
        surface <- MoveReviewExchangeAnalyzer.relationPracticalSurfaceFromWitness(witness)
        label <-
          witness.kind match
            case MoveReviewExchangeAnalyzer.RelationKind.StalemateTrap  => Some(StalemateResourceLabel)
            case MoveReviewExchangeAnalyzer.RelationKind.PerpetualCheck => Some(PerpetualCheckResourceLabel)
            case _                                                      => None
        row <- row(label, surface.text, authority = PracticalPlanAuthority)
      yield row
    }

  private def doubleCheckPracticalRow(replayCache: PlayedTopPvReplayCache): Option[MoveReviewPlayerSurfaceRow] =
    playedTopPvRelationRow(replayCache, maxPlies = 1, label = DoubleCheckLabel)(
      (replay, played) => MoveReviewExchangeAnalyzer.doubleCheckWitness(replay, played)
    )

  private def defenderTradePracticalRow(
      ctx: NarrativeContext,
      replayCache: PlayedTopPvReplayCache
  ): Option[MoveReviewPlayerSurfaceRow] =
    playedTopPvRelationRow(replayCache, maxPlies = 3, label = DefenderTradeLabel)(
      (replay, played) => MoveReviewExchangeAnalyzer.defenderTradeRelationWitness(replay, played, explicitTargetSquares(ctx))
    )

  private def badPieceTradePracticalRow(replayCache: PlayedTopPvReplayCache): Option[MoveReviewPlayerSurfaceRow] =
    playedTopPvRelationRow(replayCache, maxPlies = 4, label = BadPieceTradeLabel)(
      (replay, played) => MoveReviewExchangeAnalyzer.badPieceLiquidationRelationWitness(replay, played)
    )

  private def queenTradePracticalRow(replayCache: PlayedTopPvReplayCache): Option[MoveReviewPlayerSurfaceRow] =
    for
      replay <- replayCache.replay(maxPlies = 2)
      _ <- MoveReviewExchangeAnalyzer.queenTradeShieldLine(replay)
      row <- row(
        QueenTradeLabel,
        "This exchange moves the game into the queenless branch.",
        authority = PracticalPlanAuthority
      )
    yield row

  private def forkPracticalRow(replayCache: PlayedTopPvReplayCache): Option[MoveReviewPlayerSurfaceRow] =
    playedTopPvRelationRow(replayCache, maxPlies = 1, label = ForkLabel)(
      (replay, played) => MoveReviewExchangeAnalyzer.forkWitness(replay, played)
    )

  private def overloadPracticalRow(replayCache: PlayedTopPvReplayCache): Option[MoveReviewPlayerSurfaceRow] =
    playedTopPvRelationRow(replayCache, maxPlies = 1, label = OverloadedDefenderLabel)(
      (replay, played) => MoveReviewExchangeAnalyzer.overloadWitness(replay, played)
    )

  private def interferencePracticalRow(replayCache: PlayedTopPvReplayCache): Option[MoveReviewPlayerSurfaceRow] =
    playedTopPvRelationRow(replayCache, maxPlies = 1, label = InterferenceLabel)(
      (replay, played) => MoveReviewExchangeAnalyzer.interferenceWitness(replay, played)
    )

  private def hangingPiecePracticalRow(replayCache: PlayedTopPvReplayCache): Option[MoveReviewPlayerSurfaceRow] =
    playedTopPvRelationRow(replayCache, maxPlies = 1, label = HangingPieceLabel)(
      (replay, played) => MoveReviewExchangeAnalyzer.hangingPieceWitness(replay, played)
    )

  private def trappedPiecePracticalRow(
      ctx: NarrativeContext,
      replayCache: PlayedTopPvReplayCache
  ): Option[MoveReviewPlayerSurfaceRow] =
    playedTopPvRelationRow(replayCache, maxPlies = 1, label = TrappedPieceLabel)(
      (replay, played) => MoveReviewExchangeAnalyzer.trappedPieceWitness(replay, played, explicitTargetSquares(ctx))
    )

  private def dominationPracticalRow(
      ctx: NarrativeContext,
      replayCache: PlayedTopPvReplayCache
  ): Option[MoveReviewPlayerSurfaceRow] =
    playedTopPvRelationRow(replayCache, maxPlies = 1, label = DominationLabel)(
      (replay, played) => MoveReviewExchangeAnalyzer.dominationWitness(replay, played, explicitTargetSquares(ctx))
    )

  private def zwischenzugPracticalRow(
      ctx: NarrativeContext,
      replayCache: PlayedTopPvReplayCache
  ): Option[MoveReviewPlayerSurfaceRow] =
    playedTopPvRelationRow(replayCache, maxPlies = 2, label = ZwischenzugLabel)(
      (replay, played) => MoveReviewExchangeAnalyzer.zwischenzugWitness(replay, played, explicitTargetSquares(ctx))
    )

  private def skewerPracticalRow(replayCache: PlayedTopPvReplayCache): Option[MoveReviewPlayerSurfaceRow] =
    playedTopPvRelationRow(replayCache, maxPlies = 1, label = SkewerLabel)(
      (replay, played) => MoveReviewExchangeAnalyzer.skewerWitness(replay, played)
    )

  private def batteryPracticalRow(replayCache: PlayedTopPvReplayCache): Option[MoveReviewPlayerSurfaceRow] =
    playedTopPvRelationRow(replayCache, maxPlies = 1, label = BatteryPressureLabel)(
      (replay, played) => MoveReviewExchangeAnalyzer.batteryWitness(replay, played)
    )

  private def pinPracticalRow(replayCache: PlayedTopPvReplayCache): Option[MoveReviewPlayerSurfaceRow] =
    playedTopPvRelationRow(replayCache, maxPlies = 1, label = PinPressureLabel)(
      (replay, played) => MoveReviewExchangeAnalyzer.pinWitness(replay, played)
    )

  private def decoyPracticalRow(replayCache: PlayedTopPvReplayCache): Option[MoveReviewPlayerSurfaceRow] =
    playedTopPvRelationRow(replayCache, maxPlies = 3, label = DecoyLabel)(
      (replay, played) => MoveReviewExchangeAnalyzer.decoyWitness(replay, played)
    )

  private def connectedRooksPracticalRow(replayCache: PlayedTopPvReplayCache): Option[MoveReviewPlayerSurfaceRow] =
    for
      (_, position, move, after) <- replayCache.playedMoveAfter
      if rookCoordinationMove(position, move)
      rank <- connectedRooksRank(after.board, move.piece.color)
      row <- row(
        ConnectedRooksLabel,
        s"The checked line connects the rooks on the ${rankLabel(rank)} rank.",
        authority = PracticalPlanAuthority
      )
    yield row

  private def doubledRooksPracticalRow(replayCache: PlayedTopPvReplayCache): Option[MoveReviewPlayerSurfaceRow] =
    for
      (_, position, move, after) <- replayCache.playedMoveAfter
      if rookCoordinationMove(position, move)
      file <- doubledRooksFile(after.board, move.piece.color)
      row <- row(
        DoubledRooksLabel,
        s"The checked line doubles the rooks on the ${file.char.toString.toLowerCase}-file.",
        authority = PracticalPlanAuthority
      )
    yield row

  private def rookCoordinationMove(position: chess.Position, move: chess.Move): Boolean =
    !move.captures &&
      move.piece.role == _root_.chess.Rook &&
      move.piece.color == position.color

  private def connectedRooksRank(board: chess.Board, color: chess.Color): Option[chess.Rank] =
    val rooks = board.byPiece(color, _root_.chess.Rook).squares.toList
    if rooks.size == 2 && rooks.head.rank == rooks(1).rank && clearRankBetween(board, rooks.head, rooks(1)) then
      Some(rooks.head.rank)
    else None

  private def doubledRooksFile(board: chess.Board, color: chess.Color): Option[chess.File] =
    val rooks = board.byPiece(color, _root_.chess.Rook).squares.toList
    if rooks.size == 2 && rooks.head.file == rooks(1).file && clearFileBetween(board, rooks.head, rooks(1)) then
      Some(rooks.head.file)
    else None

  private def clearRankBetween(board: chess.Board, first: chess.Square, second: chess.Square): Boolean =
    val minFile = math.min(first.file.value, second.file.value)
    val maxFile = math.max(first.file.value, second.file.value)
    (minFile + 1 until maxFile).forall(file =>
      _root_.chess.Square.at(file, first.rank.value).forall(square => !board.occupied.contains(square))
    )

  private def clearFileBetween(board: chess.Board, first: chess.Square, second: chess.Square): Boolean =
    val minRank = math.min(first.rank.value, second.rank.value)
    val maxRank = math.max(first.rank.value, second.rank.value)
    (minRank + 1 until maxRank).forall(rank =>
      _root_.chess.Square.at(first.file.value, rank).forall(square => !board.occupied.contains(square))
    )

  private def rankLabel(rank: chess.Rank): String =
    rank match
      case _root_.chess.Rank.First   => "first"
      case _root_.chess.Rank.Second  => "second"
      case _root_.chess.Rank.Third   => "third"
      case _root_.chess.Rank.Fourth  => "fourth"
      case _root_.chess.Rank.Fifth   => "fifth"
      case _root_.chess.Rank.Sixth   => "sixth"
      case _root_.chess.Rank.Seventh => "seventh"
      case _root_.chess.Rank.Eighth  => "eighth"

  private def connectedPassersPracticalRow(replayCache: PlayedTopPvReplayCache): Option[MoveReviewPlayerSurfaceRow] =
    for
      (_, position, move, after) <- replayCache.playedMoveAfter
      if endgamePawnAdvanceMove(position, move)
      pair <- connectedPassedPair(after, move.piece.color)
      row <- row(
        ConnectedPassersLabel,
        s"The checked line leaves connected passers on ${pair._1.key} and ${pair._2.key}.",
        authority = PracticalPlanAuthority
      )
    yield row

  private def connectedPassedPair(position: chess.Position, color: chess.Color): Option[(chess.Square, chess.Square)] =
    if position.board.occupied.count <= 12 then
      val passers =
        PositionAnalyzer
          .passedPawns(color, pawnsFor(position.board, color), pawnsFor(position.board, !color))
          .filter(pawn => advancedPassedPawn(pawn, color))
          .sortBy(square => (square.file.value, square.rank.value))
      val enemyPassers =
        PositionAnalyzer.passedPawns(!color, pawnsFor(position.board, !color), pawnsFor(position.board, color))
      if enemyPassers.isEmpty then
        passers
          .combinations(2)
          .collectFirst {
            case List(first, second)
                if fileDistance(first.file, second.file) == 1 &&
                  (first.rank.value - second.rank.value).abs <= 1 =>
              first -> second
          }
      else None
    else None

  private def outsidePasserPracticalRow(replayCache: PlayedTopPvReplayCache): Option[MoveReviewPlayerSurfaceRow] =
    for
      (_, position, move, after) <- replayCache.playedMoveAfter
      if endgamePawnAdvanceMove(position, move)
      passer <- outsidePassedPawn(after, move.piece.color)
      row <- row(
        OutsidePasserLabel,
        s"The checked line leaves an outside passer on ${passer.key}.",
        authority = PracticalPlanAuthority
      )
    yield row

  private def outsidePassedPawn(position: chess.Position, color: chess.Color): Option[chess.Square] =
    if position.board.occupied.count <= 12 then
      val board = position.board
      val passers =
        PositionAnalyzer
          .passedPawns(color, pawnsFor(board, color), pawnsFor(board, !color))
          .filter(pawn => advancedPassedPawn(pawn, color))
          .filter(pawn => pawn.file.value <= 1 || pawn.file.value >= 6)
          .sortBy(pawn => -relativeRank(pawn, color))
      val enemyPassers = PositionAnalyzer.passedPawns(!color, pawnsFor(board, !color), pawnsFor(board, color))
      val ownPawns = pawnsFor(board, color).squares.toList
      if enemyPassers.isEmpty then
        passers.find { passer =>
          ownPawns.exists(pawn => pawn != passer && fileDistance(pawn.file, passer.file) >= 3) &&
            !board.kingPosOf(!color).exists(enemyKingBlocksPasser(_, passer, color))
        }
      else None
    else None

  private def endgamePawnAdvanceMove(position: chess.Position, move: chess.Move): Boolean =
    !move.captures &&
      move.promotion.isEmpty &&
      move.piece.role == _root_.chess.Pawn &&
      move.piece.color == position.color &&
      move.orig.file == move.dest.file &&
      (if move.piece.color.white then move.dest.rank.value > move.orig.rank.value else move.dest.rank.value < move.orig.rank.value)

  private def fileDistance(first: chess.File, second: chess.File): Int =
    (first.value - second.value).abs

  private def relativeRank(square: chess.Square, color: chess.Color): Int =
    if color.white then square.rank.value else 7 - square.rank.value

  private def enemyKingBlocksPasser(enemyKing: chess.Square, passer: chess.Square, color: chess.Color): Boolean =
    enemyKing.file == passer.file &&
      (if color.white then enemyKing.rank.value > passer.rank.value else enemyKing.rank.value < passer.rank.value)

  private def passedPawnPracticalRow(replayCache: PlayedTopPvReplayCache): Option[MoveReviewPlayerSurfaceRow] =
    for
      (_, position, move, after) <- replayCache.playedMoveAfter
      if endgamePawnAdvanceMove(position, move)
      if afterMovePassedPawn(after, move)
      row <- row(
        PassedPawnAdvanceLabel,
        s"The checked line advances the passed pawn to ${move.dest.key}.",
        authority = PracticalPlanAuthority
      )
    yield row

  private def afterMovePassedPawn(position: chess.Position, move: chess.Move): Boolean =
    PositionAnalyzer.passedPawns(move.piece.color, pawnsFor(position.board, move.piece.color), pawnsFor(position.board, !move.piece.color)).contains(move.dest)

  private def pawnsFor(board: chess.Board, color: chess.Color): _root_.chess.Bitboard =
    board.pawns & board.byColor(color)

  private def kingActivationPracticalRow(replayCache: PlayedTopPvReplayCache): Option[MoveReviewPlayerSurfaceRow] =
    for
      (_, position, move, after) <- replayCache.playedMoveAfter
      if kingActivationMove(position, move)
      if after.board.pieceAt(move.dest).exists(piece => piece.color == move.piece.color && piece.role == _root_.chess.King)
      if kingActivityImproves(position.board, after.board, move.piece.color, move.orig, move.dest)
      row <- row(
        KingActivationLabel,
        s"The checked line activates the king on ${move.dest.key} for the endgame.",
        authority = PracticalPlanAuthority
      )
    yield row

  private def kingActivationMove(position: chess.Position, move: chess.Move): Boolean =
    !move.captures &&
      move.castle.isEmpty &&
      move.piece.role == _root_.chess.King &&
      move.piece.color == position.color &&
      position.board.occupied.count <= 10

  private def kingActivityImproves(
      before: chess.Board,
      after: chess.Board,
      color: chess.Color,
      orig: chess.Square,
      dest: chess.Square
  ): Boolean =
    val beforeCenter = kingCenterDistance(orig)
    val afterCenter = kingCenterDistance(dest)
    val beforeMobility = (orig.kingAttacks & ~before.byColor(color)).count
    val afterMobility = (dest.kingAttacks & ~after.byColor(color)).count
    afterCenter < beforeCenter && afterMobility >= beforeMobility

  private def kingCenterDistance(square: chess.Square): Int =
    KingCenterSquares
      .map(center => math.max((square.file.value - center.file.value).abs, (square.rank.value - center.rank.value).abs))
      .min

  private def quietIntentPubliclySupported(intent: QuietMoveIntentClaim): Boolean =
    intent.allowsUserFacing &&
      quietIntentPacketMatchesClass(intent) &&
      intent.packet.scope == PlayerFacingPacketScope.MoveLocal &&
      intent.packet.releaseRisks.isEmpty &&
      intent.packet.suppressionReasons.isEmpty

  private def quietIntentPacketMatchesClass(intent: QuietMoveIntentClaim): Boolean =
    intent.packet.proofFamily == intent.intentClass.proofFamily &&
      intent.packet.proofSource == intent.sourceKind &&
      intent.packet.claimGate.ontologyFamily == intent.intentClass.ontologyFamily

  private def quietIntentLegalMove(ctx: NarrativeContext, intent: QuietMoveIntentClaim): Option[chess.Move] =
    (for
      position <- Fen.read(Standard, Fen.Full(ctx.fen))
      uci <- ctx.playedMove.map(NarrativeUtils.normalizeUciMove).filter(MoveReviewExchangeAnalyzer.isUciMove)
      move <- Uci(uci).collect { case move: Uci.Move => move }.flatMap(position.move(_).toOption)
    yield move).filter(move => quietIntentMoveMatches(intent, move))

  private def quietIntentMoveMatches(intent: QuietMoveIntentClaim, move: chess.Move): Boolean =
    val anchors = quietIntentAnchorSquares(intent)
    val anchorOk = anchors.nonEmpty && anchors.forall(_ == move.dest.key)
    val quietNonCapturePiece =
      !move.captures &&
        move.piece.role != _root_.chess.Pawn
    intent.intentClass match
      case QuietMoveIntentClass.PieceImprovement =>
        anchorOk &&
          quietNonCapturePiece &&
          move.piece.role != _root_.chess.King
      case QuietMoveIntentClass.TechnicalConversionStep =>
        anchorOk && quietNonCapturePiece
      case QuietMoveIntentClass.KingSafety =>
        anchorOk &&
          move.piece.role == _root_.chess.King &&
          (
            intent.sourceKind == "king_move" ||
              (Set("castle_short", "castle_long").contains(intent.sourceKind) && quietCastleMove(move))
          )
      case QuietMoveIntentClass.CounterplayRestraint => false

  private def quietIntentAnchorSquares(intent: QuietMoveIntentClaim): Set[String] =
    (intent.packet.anchorTerms ++ intent.packet.proofPathWitness.ownerSeedTerms)
      .map(_.trim.toLowerCase)
      .filter(MoveReviewPlayerPayloadBuilder.ChessSquarePattern.matches)
      .toSet

  private def quietCastleMove(move: chess.Move): Boolean =
    Set("e1g1", "e1c1", "e8g8", "e8c8").contains(s"${move.orig.key}${move.dest.key}")

  private def quietRoleLabel(role: chess.Role): String =
    role match
      case _root_.chess.King   => "king"
      case _root_.chess.Queen  => "queen"
      case _root_.chess.Rook   => "rook"
      case _root_.chess.Bishop => "bishop"
      case _root_.chess.Knight => "knight"
      case _root_.chess.Pawn   => "pawn"

  private def tacticalPracticalVeto(
      inputs: QuestionPlannerInputs,
      truthContract: Option[DecisiveTruthContract]
  ): Boolean =
    inputs.truthMode == PlayerFacingTruthMode.Tactical ||
      inputs.mainBundle.flatMap(_.mainClaim).exists(_.mode == PlayerFacingTruthMode.Tactical) ||
      MoveReviewSurfaceTruthVeto.truthContractSurfaceVeto(truthContract)

  private def namedRouteNetworkRow(
      ctx: NarrativeContext,
      inputs: QuestionPlannerInputs,
      truthContract: Option[DecisiveTruthContract]
  ): Option[MoveReviewPlayerSurfaceRow] =
    val decision =
      ClaimAuthorityResolver.namedRouteNetworkSurfaceDecision(
        ctx = Some(ctx),
        inputs = inputs,
        truthContract = truthContract
      )
    decision
      .filter(_.supportedLocalWithoutTacticalVeto)
      .flatMap(_ => inputs.namedRouteNetworkSurface.flatMap(routeNetworkRow))

  private def routeNetworkRow(
      network: RouteNetworkBindProof.SurfaceNetwork
  ): Option[MoveReviewPlayerSurfaceRow] =
    row(
      RouteDenialLabel,
      network.routeDenialText("The checked line"),
      authority = PracticalPlanAuthority
    )

  private def dualAxisBindRow(
      ctx: NarrativeContext,
      inputs: QuestionPlannerInputs,
      truthContract: Option[DecisiveTruthContract]
  ): Option[MoveReviewPlayerSurfaceRow] =
    val decision =
      ClaimAuthorityResolver.dualAxisBindSurfaceDecision(
        ctx = Some(ctx),
        inputs = inputs,
        truthContract = truthContract
      )
    decision
      .filter(_.supportedLocalWithoutTacticalVeto)
      .flatMap(_ => inputs.dualAxisBindSurface.flatMap(dualAxisBindSurfaceRow))

  private def dualAxisBindSurfaceRow(
      contract: TwoAxisBindProof.Contract
  ): Option[MoveReviewPlayerSurfaceRow] =
    for
      breakAxis <- contract.primaryAxis.filter(axis => normalizeSurfaceToken(axis.kind) == "break_axis")
      entryAxis <- contract.corroboratingAxes.find(axis => normalizeSurfaceToken(axis.kind) == "entry_axis")
      breakLabel <- surfaceAxisLabel(breakAxis.label)
      entryLabel <- surfaceAxisLabel(entryAxis.label)
      row <- row(
        DualAxisBindLabel,
        s"The checked line keeps the $breakLabel break shut while keeping $entryLabel unavailable.",
        authority = PracticalPlanAuthority
      )
    yield row

  private def restrictedDefenseConversionRow(
      ctx: NarrativeContext,
      inputs: QuestionPlannerInputs,
      truthContract: Option[DecisiveTruthContract]
  ): Option[MoveReviewPlayerSurfaceRow] =
    val decision =
      ClaimAuthorityResolver.restrictedDefenseConversionSurfaceDecision(
        ctx = Some(ctx),
        inputs = inputs,
        truthContract = truthContract
      )
    decision
      .filter(_.supportedLocalWithoutTacticalVeto)
      .flatMap(_ =>
        inputs.restrictedDefenseConversionSurface.flatMap(contract => restrictedDefenseConversionSurfaceRow(ctx, contract))
      )

  private def restrictedDefenseConversionSurfaceRow(
      ctx: NarrativeContext,
      contract: RestrictedDefenseConversionProof.Contract
  ): Option[MoveReviewPlayerSurfaceRow] =
    val replySan =
      for
        played <- ctx.playedMove
        reply <- contract.bestDefenseFound
        afterFen <- MoveReviewPvLine.legalFenAfter(ctx.fen, played)
        san <- NarrativeUtils.uciToSan(afterFen, reply)
        cleanSan <- MoveReviewPlayerPayloadBuilder.cleanOpt(Option(san))
      yield cleanSan
    val text =
      replySan
        .map(san => s"The checked line keeps the conversion route intact after $san.")
        .getOrElse("The checked line keeps the best defense narrow and the conversion route intact.")
    row(TechnicalConversionLabel, text, authority = PracticalPlanAuthority)

  private def rowForPlan(
      ctx: NarrativeContext,
      inputs: QuestionPlannerInputs,
      truthContract: Option[DecisiveTruthContract],
      plan: QuestionPlan
  ): Option[MoveReviewPlayerSurfaceRow] =
    val admission =
      ClaimAuthorityResolver.supportedLocalNeutralizeKeyBreakTimingAdmission(
        ctx = Some(ctx),
        inputs = inputs,
        truthContract = truthContract,
        plan = plan
      )
    admission
      .filter(_.decision.supportedLocalWithoutTacticalVeto)
      .flatMap { admission =>
        NeutralizeKeyBreakSurfaceGate
          .decideForPlanPacket(plan, admission.packet, ctx)
          .token
          .map(_ -> admission.packet)
      }
      .flatMap { case (token, packet) =>
        row(
          CounterplayBreakLabel,
          NeutralizeKeyBreakSurfaceGate.surfaceText(token, packet),
          authority = Some(NeutralizeKeyBreakSurfaceGate.surfaceAuthority(token, packet))
        )
      }

  private def rowForClaim(
      ctx: NarrativeContext,
      inputs: QuestionPlannerInputs,
      truthContract: Option[DecisiveTruthContract],
      claim: MainPathScopedClaim
  ): Option[MoveReviewPlayerSurfaceRow] =
    claim.packet.flatMap { packet =>
      if packet.proofSource == ProofSourceId.CounterplayAxisSuppression.wireKey &&
          packet.proofFamily == ProofFamilyId.NeutralizeKeyBreak.wireKey
      then
        val decision =
          ClaimAuthorityResolver.supportedLocalNeutralizeKeyBreakPacketDecision(
            ctx = Some(ctx),
            inputs = inputs,
            truthContract = truthContract,
            packet = packet
          )
        if decision.supportedLocalWithoutTacticalVeto then
          NeutralizeKeyBreakSurfaceGate
            .decideForPacket(packet, ctx)
            .token
            .flatMap { token =>
              row(
                CounterplayBreakLabel,
                NeutralizeKeyBreakSurfaceGate.surfaceText(token, packet),
                authority = Some(NeutralizeKeyBreakSurfaceGate.surfaceAuthority(token, packet))
              )
            }
        else None
      else if packet.proofSource == CentralBreakTimingWitness.ProofSource &&
          packet.proofFamily == CentralBreakTimingWitness.ProofFamily
      then
        ClaimAuthorityResolver
          .supportedLocalCentralBreakTimingAdmission(
            ctx = Some(ctx),
            inputs = inputs,
            truthContract = truthContract,
            packet = packet
          )
          .filter(_.decision.supportedLocalWithoutTacticalVeto)
          .flatMap { admission =>
            CentralBreakTimingSurfaceGate
              .decide(admission.witness)
              .token
              .map(token =>
                token ->
                  CentralBreakTimingSurfaceGate.surfaceText(admission.witness, token)
              )
          }
          .flatMap { case (token, text) =>
            row(
              CentralBreakLabel,
              text,
              authority = Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.CentralBreak, token = Some(token)))
            )
          }
      else if positionProbePacket(packet)
      then
        admittedPositionProbeRow(ctx, inputs, truthContract, packet)
      else if counterplayRestraintPacket(packet)
      then
        supportedLocalMoveDeltaRow(ctx, inputs, truthContract, packet)(counterplayRestraintRow)
      else if packet.proofSource == ProofSourceId.LocalFileEntryBind.wireKey &&
          packet.proofFamily == ProofFamilyId.HalfOpenFilePressure.wireKey
      then
        supportedLocalMoveDeltaRow(ctx, inputs, truthContract, packet)(localFileEntryRow)
      else if outpostOccupationPacket(packet)
      then
        supportedLocalMoveDeltaRow(ctx, inputs, truthContract, packet)(outpostOccupationRow)
      else if iqpInducementPacket(packet)
      then
        supportedLocalMoveDeltaRow(ctx, inputs, truthContract, packet)(packet => iqpInducementRow(Some(ctx), packet))
      else if simplificationWindowPacket(packet)
      then
        supportedLocalMoveDeltaRow(ctx, inputs, truthContract, packet)(simplificationWindowRow)
      else if exchangeOwnershipPacket(packet)
      then
        supportedLocalMoveDeltaRow(ctx, inputs, truthContract, packet)(exchangeOwnershipRow)
      else None
      }

  private def supportedLocalMoveDeltaRow(
      ctx: NarrativeContext,
      inputs: QuestionPlannerInputs,
      truthContract: Option[DecisiveTruthContract],
      packet: PlayerFacingClaimPacket
  )(
      buildRow: PlayerFacingClaimPacket => Option[MoveReviewPlayerSurfaceRow]
  ): Option[MoveReviewPlayerSurfaceRow] =
    val decision =
      ClaimAuthorityResolver.supportedLocalMoveDeltaPacketDecision(
        ctx = Some(ctx),
        inputs = inputs,
        truthContract = truthContract,
        packet = packet
      )
    if decision.supportedLocalWithoutTacticalVeto then buildRow(packet)
    else None

  private def positionProbePacket(packet: PlayerFacingClaimPacket): Boolean =
    packet.scope == PlayerFacingPacketScope.PositionLocal &&
      PositionProbeProofSources.contains(packet.proofSource)

  private def counterplayRestraintPacket(packet: PlayerFacingClaimPacket): Boolean =
    packet.proofSource == ProofSourceId.ProphylacticMove.wireKey &&
      packet.proofFamily == ProofFamilyId.CounterplayRestraint.wireKey

  private def outpostOccupationPacket(packet: PlayerFacingClaimPacket): Boolean =
    packet.proofSource == PlayerFacingTruthModePolicy.OutpostEntrenchmentProofSource &&
      packet.proofFamily == PlayerFacingTruthModePolicy.OutpostEntrenchmentProofFamily

  private def iqpInducementPacket(packet: PlayerFacingClaimPacket): Boolean =
    packet.proofSource == ProofSourceId.IQPInducementProbe.wireKey &&
      packet.proofFamily == IqpInducementFamily

  private def simplificationWindowPacket(packet: PlayerFacingClaimPacket): Boolean =
    packet.proofSource == SimplificationWindowFamily &&
      packet.proofFamily == SimplificationWindowFamily

  private def matchedExactSliceProof[A](
      packet: PlayerFacingClaimPacket
  )(build: PartialFunction[PlayerFacingExactSliceProof, Option[A]]): Option[A] =
    packet.proofPathWitness.exactSliceProof.flatMap { proof =>
      if build.isDefinedAt(proof) && PlayerFacingExactSliceProofFacts.matchesPacket(packet, proof) then build(proof)
      else None
    }

  private def admittedPositionProbeRow(
      ctx: NarrativeContext,
      inputs: QuestionPlannerInputs,
      truthContract: Option[DecisiveTruthContract],
      packet: PlayerFacingClaimPacket
  ): Option[MoveReviewPlayerSurfaceRow] =
    val decision =
      ClaimAuthorityResolver.decidePositionProbe(
        ctx = Some(ctx),
        inputs = inputs,
        truthContract = truthContract,
        packet = packet
    )
    if decision.admitted && decision.vetoReasons.isEmpty then
      positionProbeRow(
        packet,
        afterPosition = positionAfterPlayedMove(ctx),
        ctx = Some(ctx)
      )
    else None

  private def positionProbeRow(
      packet: PlayerFacingClaimPacket,
      afterPosition: Option[_root_.chess.Position],
      ctx: Option[NarrativeContext] = None,
      lineFactsValidatedColorComplex: Boolean = false
  ): Option[MoveReviewPlayerSurfaceRow] =
    matchedExactSliceProof(packet) {
      case PlayerFacingExactSliceProof.ExactTargetFixation(targetSquare) =>
        val target = targetSquare.toLowerCase
        val targetText =
          pawnDefenderOfTarget(afterPosition, target)
            .map(defender => s"The checked line keeps $target fixed as a pawn target, with the $defender pawn still defending it.")
            .getOrElse(s"The checked line keeps $target fixed as the target.")
        row(
          "Fixed target",
          targetText,
          authority =
            Some(
              MoveReviewSurfaceAuthority(
                kind = MoveReviewSurfaceAuthority.PracticalPlan,
                target = Some(target)
              )
            )
        )
      case PlayerFacingExactSliceProof.CarlsbadFixedTarget(targetSquare, true) =>
        val target = targetSquare.toLowerCase
        row(
          "Minority attack",
          s"The checked line keeps $target as the minority-attack fixed target.",
          authority =
            Some(
              MoveReviewSurfaceAuthority(
                kind = MoveReviewSurfaceAuthority.PracticalPlan,
                target = Some(target)
              )
            )
        )
      case PlayerFacingExactSliceProof.TargetFocusedCoordination(targetSquare, supportFromSquares, _) =>
        val target = targetSquare.toLowerCase
        val support = supportFromSquares.map(_.toLowerCase).distinct.take(2).mkString(" and ")
        row(
          "Target coordination",
          s"The checked line coordinates pressure on $target from $support.",
          authority =
            Some(
              MoveReviewSurfaceAuthority(
                kind = MoveReviewSurfaceAuthority.PracticalPlan,
                target = Some(target)
              )
            )
        )
      case proof @ PlayerFacingExactSliceProof.ColorComplexSqueeze(
            targetSquare,
            squareColor,
            minorPieceRole,
            minorPieceSquare
          ) =>
        val target = targetSquare.toLowerCase
        val complex = squareColor.toLowerCase
        val role = minorPieceRole.toLowerCase
        val from = minorPieceSquare.toLowerCase
        if lineFactsValidatedColorComplex || ColorComplexRuntimeProof.playedMoveOwnsAndPersists(ctx, proof)
        then
          row(
            ColorComplexLabel,
            s"The checked line keeps the $role on $from attacking $target in the $complex-square complex.",
            authority =
              Some(
                MoveReviewSurfaceAuthority(
                  kind = MoveReviewSurfaceAuthority.PracticalPlan,
                  target = Some(target)
                )
              )
          )
        else None
    }

  private[analysis] def positionProbeExactSliceRow(
      packet: PlayerFacingClaimPacket,
      afterPosition: Option[_root_.chess.Position] = None,
      lineFactsValidatedColorComplex: Boolean = false
  ): Option[MoveReviewPlayerSurfaceRow] =
    positionProbeRow(
      packet,
      afterPosition = afterPosition,
      lineFactsValidatedColorComplex = lineFactsValidatedColorComplex
    )

  private def pawnDefenderOfTarget(
      position: Option[_root_.chess.Position],
      target: String
  ): Option[String] =
    for
      boardPosition <- position
      square <- MoveReviewExchangeAnalyzer.squareFromKey(target)
      targetPiece <- boardPosition.board.pieceAt(square)
      if targetPiece.role == _root_.chess.Pawn
      defender <- boardPosition.board
        .attackers(square, targetPiece.color)
        .squares
        .toList
        .sortBy(_.key)
        .find(defenderSquare =>
          boardPosition.board
            .pieceAt(defenderSquare)
            .exists(piece => piece.color == targetPiece.color && piece.role == _root_.chess.Pawn)
        )
    yield defender.key

  private def positionAfterPlayedMove(ctx: NarrativeContext): Option[_root_.chess.Position] =
    for
      played <- ctx.playedMove
      afterFen <- MoveReviewPvLine.legalFenAfter(ctx.fen, played)
      position <- Fen.read(Standard, Fen.Full(afterFen))
    yield position

  private[analysis] def moveLocalExactSliceRow(
      packet: PlayerFacingClaimPacket
  ): Option[MoveReviewPlayerSurfaceRow] =
    if counterplayRestraintPacket(packet) then counterplayRestraintRow(packet)
    else if packet.proofSource == ProofSourceId.LocalFileEntryBind.wireKey &&
        packet.proofFamily == ProofFamilyId.HalfOpenFilePressure.wireKey
    then localFileEntryRow(packet)
    else if outpostOccupationPacket(packet) then outpostOccupationRow(packet)
    else if iqpInducementPacket(packet) then iqpInducementRow(None, packet)
    else if simplificationWindowPacket(packet) then simplificationWindowRow(packet)
    else if exchangeOwnershipPacket(packet) then exchangeOwnershipRow(packet)
    else None

  private def counterplayRestraintRow(
      packet: PlayerFacingClaimPacket
  ): Option[MoveReviewPlayerSurfaceRow] =
    matchedExactSliceProof(packet) {
      case PlayerFacingExactSliceProof.ProphylacticRestraint(resourceToken) =>
        row(
          CounterplayRestraintLabel,
          counterplayRestraintText(resourceToken),
          authority = PracticalPlanAuthority
        )
    }

  private def counterplayRestraintText(resourceToken: String): String =
    val token = resourceToken.trim.toLowerCase
    if token.startsWith("denied_resource:") then
      val resource = token.stripPrefix("denied_resource:")
      s"The checked line keeps ${counterplayResourceLabel(resource)} restrained."
    else s"The checked line keeps $token unavailable as a counterplay resource."

  private def counterplayResourceLabel(resource: String): String =
    resource match
      case "break"                => "the opponent's break"
      case "entry_square"         => "the opponent's entry square"
      case "forcing_threat"       => "the forcing threat"
      case "piece_activity"       => "the opponent's piece activity"
      case "counterplay_route"    => "the counterplay route"
      case "route_node"           => "the route node"
      case "reroute_square"       => "the reroute square"
      case "pressure"             => "the pressure resource"
      case "color_complex_escape" => "the color-complex escape"
      case other                  => other.replace('_', ' ')

  private def iqpInducementRow(
      ctx: Option[NarrativeContext],
      packet: PlayerFacingClaimPacket
  ): Option[MoveReviewPlayerSurfaceRow] =
    matchedExactSliceProof(packet) {
      case PlayerFacingExactSliceProof.IqpInducement(targetSquare, lineMoves) =>
        val target = targetSquare.toLowerCase
        val refSans =
          ctx.toList.flatMap(context =>
            LineConsequenceEvaluator
              .replaySteps(context.fen, lineMoves, preferredSan = Nil)
              .map(_.san)
              .take(6)
          )
        row(
          IqpTargetLabel,
          s"The checked line leaves $target as an isolated pawn target.",
          refSans = refSans,
          authority =
            Some(
              MoveReviewSurfaceAuthority(
                kind = MoveReviewSurfaceAuthority.PracticalPlan,
                target = Some(target)
              )
            )
        )
    }

  private def outpostOccupationRow(
      packet: PlayerFacingClaimPacket
  ): Option[MoveReviewPlayerSurfaceRow] =
    matchedExactSliceProof(packet) {
      case PlayerFacingExactSliceProof.OutpostOccupation(pieceRole, square) =>
        val target = square.toLowerCase
        row(
          KnightOutpostLabel,
          s"The checked line puts the ${pieceRole.toLowerCase} on the $target outpost.",
          authority =
            Some(
              MoveReviewSurfaceAuthority(
                kind = MoveReviewSurfaceAuthority.PracticalPlan,
                target = Some(target)
              )
            )
        )
    }

  private def simplificationWindowRow(
      packet: PlayerFacingClaimPacket
  ): Option[MoveReviewPlayerSurfaceRow] =
    matchedExactSliceProof(packet) {
      case PlayerFacingExactSliceProof.SimplificationWindow(exchangeSquare) =>
        val square = exchangeSquare.toLowerCase
        row(
          SimplificationLabel,
          s"The checked line keeps the same local edge after the exchange on $square.",
          authority =
            Some(
              MoveReviewSurfaceAuthority(
                kind = MoveReviewSurfaceAuthority.PracticalPlan,
                target = Some(square)
              )
            )
        )
    }

  private def exchangeOwnershipPacket(packet: PlayerFacingClaimPacket): Boolean =
    (packet.proofSource == PlayerFacingTruthModePolicy.DefenderTradeProofSource &&
      packet.proofFamily == DefenderTradeFamily) ||
      (packet.proofSource == PlayerFacingTruthModePolicy.BadPieceLiquidationProofSource &&
        packet.proofFamily == BadPieceLiquidationFamily) ||
      (packet.proofSource == PlayerFacingTruthModePolicy.QueenTradeShieldProofSource &&
        packet.proofFamily == QueenTradeShieldFamily)

  private def exchangeOwnershipRow(
      packet: PlayerFacingClaimPacket
  ): Option[MoveReviewPlayerSurfaceRow] =
    if packet.proofSource == PlayerFacingTruthModePolicy.DefenderTradeProofSource &&
        packet.proofFamily == DefenderTradeFamily
    then
      defenderTradeBranch(packet).flatMap { case (defender, exchange, target) =>
        row(
          DefenderTradeLabel,
          s"The checked line trades on $exchange to remove the defender from $defender, loosening $target.",
          authority = Some(
            MoveReviewSurfaceAuthority(
              kind = MoveReviewSurfaceAuthority.StrategicRelation,
              token = Some(MoveReviewExchangeAnalyzer.RelationKind.DefenderTrade),
              target = Some(target)
            )
          )
        )
      }
    else if packet.proofSource == PlayerFacingTruthModePolicy.BadPieceLiquidationProofSource &&
        packet.proofFamily == BadPieceLiquidationFamily
    then
      badPieceLiquidationBranch(packet).flatMap { case (badPiece, exchange) =>
        row(
          BadPieceTradeLabel,
          s"The checked line trades on $exchange to clear the bad piece from $badPiece.",
          authority = Some(
            MoveReviewSurfaceAuthority(
              kind = MoveReviewSurfaceAuthority.StrategicRelation,
              token = Some(MoveReviewExchangeAnalyzer.RelationKind.BadPieceLiquidation),
              target = Some(exchange)
            )
          )
        )
      }
    else if packet.proofSource == PlayerFacingTruthModePolicy.QueenTradeShieldProofSource &&
        packet.proofFamily == QueenTradeShieldFamily
    then queenTradeShieldRow(packet)
    else None

  private def defenderTradeBranch(packet: PlayerFacingClaimPacket): Option[(String, String, String)] =
    matchedExactSliceProof(packet) {
      case PlayerFacingExactSliceProof.DefenderTrade(defender, exchange, target) =>
        Some((defender.trim.toLowerCase, exchange.trim.toLowerCase, target.trim.toLowerCase))
    }

  private def badPieceLiquidationBranch(packet: PlayerFacingClaimPacket): Option[(String, String)] =
    matchedExactSliceProof(packet) {
      case PlayerFacingExactSliceProof.BadPieceLiquidation(badPiece, exchange) =>
        Some((badPiece.trim.toLowerCase, exchange.trim.toLowerCase))
    }

  private def queenTradeShieldRow(
      packet: PlayerFacingClaimPacket
  ): Option[MoveReviewPlayerSurfaceRow] =
    matchedExactSliceProof(packet) {
      case PlayerFacingExactSliceProof.QueenTradeShield(_) =>
        row(
          QueenTradeLabel,
          "This exchange moves the game into the queenless branch.",
          authority = PracticalPlanAuthority
        )
    }

  private def localFileEntryRow(
      packet: PlayerFacingClaimPacket
  ): Option[MoveReviewPlayerSurfaceRow] =
    matchedExactSliceProof(packet) {
      case PlayerFacingExactSliceProof.LocalFileEntryBind(file, entrySquare) =>
        val displayFile = file.toLowerCase.stripSuffix("-file") + "-file"
        val entry = entrySquare.toLowerCase
        row(
          FileEntryLabel,
          s"The checked line keeps pressure on $entry through the $displayFile.",
          authority =
            Some(
              MoveReviewSurfaceAuthority(
                kind = MoveReviewSurfaceAuthority.PracticalPlan,
                target = Some(entry)
              )
            )
        )
    }

  private def mainPathClaims(inputs: QuestionPlannerInputs): List[MainPathScopedClaim] =
    inputs.mainBundle.toList.flatMap { bundle =>
      bundle.mainClaim.toList ++ bundle.lineScopedClaim.toList
    }

  private def row(
      label: String,
      text: String,
      refSans: List[String] = Nil,
      authority: Option[MoveReviewSurfaceAuthority]
  ): Option[MoveReviewPlayerSurfaceRow] =
    for
      cleanLabel <- MoveReviewPlayerPayloadBuilder.cleanOpt(Option(label))
      cleanText <- MoveReviewPlayerPayloadBuilder.cleanOpt(Option(text))
    yield MoveReviewPlayerSurfaceRow(
      label = cleanLabel,
      text = cleanText,
      tone = None,
      source = None,
      refSans = MoveReviewPlayerPayloadBuilder.cleanLineList(refSans),
      authority = authority
    )

  private def surfaceAxisLabel(value: String): Option[String] =
    MoveReviewPlayerPayloadBuilder.cleanOpt(Option(value))
      .map(_.toLowerCase)
      .map(_.replaceAll("""(?i)^neutralized[-_ ]break[: ]""", ""))
      .map(_.replaceAll("\\s+", " "))
      .filter(label =>
        MoveReviewPlayerPayloadBuilder.LabelSquareListPattern.matches(label) ||
          MoveReviewPlayerPayloadBuilder.LabelSquareRangePattern.matches(label)
      )

  private def normalizeSurfaceToken(value: String): String =
    Option(value).getOrElse("").trim.toLowerCase

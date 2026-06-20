package lila.chessjudgment.analysis.assembly

import chess.Color
import chess.format.Fen
import chess.variant.Standard
import lila.chessjudgment.analysis.evaluation.{ EvaluationPerspectivePolicy, PerspectiveMath }
import lila.chessjudgment.analysis.move.{ MoveAnalyzer, MoveMotifNormalizer }
import lila.chessjudgment.analysis.opening.OpeningContextFactNormalizer
import lila.chessjudgment.analysis.plan.{ PlanInteractionContext, PlanMatcher }
import lila.chessjudgment.analysis.position.{ PositionAnalyzer, PositionFeatures }
import lila.chessjudgment.analysis.singlePosition.{ PawnPlayAssessor, PawnPlayDriver, PvLine, ThreatPressureAssessor }
import lila.chessjudgment.analysis.strategic.StrategicFactNormalizer
import lila.chessjudgment.analysis.structure.{
  PawnStructureAssessor,
  PlanAlignmentScorer,
  StructuralDeltaAnalyzer,
  StructuralPlaybook
}
import lila.chessjudgment.analysis.tactical.{ RelationFactNormalizer, TacticalRelationEvidence }
import lila.chessjudgment.analysis.transition.{ TransitionAnalyzer, TransitionFactNormalizer }
import lila.chessjudgment.model.{ CompatibilityAdjustment, Fact, Motif, PlanCategory }
import lila.chessjudgment.model.judgment.*

final case class EvidenceFactAssembly(
    input: NormalizedMoveReviewInput,
    context: JudgmentAssemblyContext
)

object EvidenceFactAssembler:

  private val OpeningRelevanceMaxPly = 20

  def assemble(raw: RawMoveReviewInput): Option[EvidenceFactAssembly] =
    NodeLineTransitionAssembler.assemble(raw).map(enrich)

  def enrich(assembly: NodeLineTransitionAssembly): EvidenceFactAssembly =
    val allocator = JudgmentProvenanceAllocator.forInput(assembly.input)
    val context = assembly.context
    val motifRecords = moveMotifRecords(assembly.input, context, allocator)
    val motifContext = context.withEvidence(motifRecords)
    val baseRecords =
      motifRecords ++ List.concat(
        relationRecords(assembly.input, context, allocator),
        pawnStructureRecords(motifContext, allocator),
        threatPressureRecords(assembly.input, motifContext, allocator),
        structuralDeltaRecords(context, allocator)
      )
    val baseContext = context.withEvidence(baseRecords)
    val strategicRecords = strategicFeatureRecords(baseContext, allocator)
    val strategicContext = baseContext.withEvidence(strategicRecords)
    val planRecords = planPressureRecords(assembly.input, strategicContext, allocator)
    val planContext = strategicContext.withEvidence(planRecords)
    val openingRecords = featureApplicabilityRecords(assembly.input, planContext, allocator)
    EvidenceFactAssembly(assembly.input, planContext.withEvidence(openingRecords))

  private def moveMotifRecords(
      input: NormalizedMoveReviewInput,
      context: JudgmentAssemblyContext,
      allocator: JudgmentProvenanceAllocator
  ): List[EvidenceRecord] =
    val before = context.position(PositionNodeRole.Before).toList
    before.flatMap { root =>
      val transitionRecords =
        List(
          context.playedTransition.map { edge =>
            val line = lineForTransition(context, edge)
            moveMotifRecord(
              id = allocator.evidenceId(s"move-motif:played:${edge.moveUci}"),
              startFen = input.beforeFen,
              position = root.ref,
              moveUci = edge.moveUci,
              moves = List(edge.moveUci),
              line = line,
              scope = EvidenceScope.PlayedTransition,
              parents = transitionParents(context, edge, line)
            )
          },
          context.referenceTransition.map { edge =>
            val line = lineForTransition(context, edge)
            moveMotifRecord(
              id = allocator.evidenceId(s"move-motif:reference:${edge.moveUci}"),
              startFen = input.beforeFen,
              position = root.ref,
              moveUci = edge.moveUci,
              moves = List(edge.moveUci),
              line = line,
              scope = EvidenceScope.ReferenceTransition,
              parents = transitionParents(context, edge, line)
            )
          }
        ).flatten.flatten
      val lineRecords =
        context.lines.flatMap { line =>
          val startPosition = startPositionForLine(input, context, root, line)
          moveMotifRecord(
            id = allocator.evidenceId(s"move-motif:line:${allocator.key(line.role)}:${line.ref.rank}:${line.ref.rootMove}"),
            startFen = startPosition.ref.fen,
            position = startPosition.ref,
            moveUci = line.ref.rootMove,
            moves = line.line.moves,
            line = Some(line),
            scope = line.role.scope,
            parents = lineParents(context, line)
          )
        }
      transitionRecords ++ lineRecords
    }

  private def moveMotifRecord(
      id: String,
      startFen: String,
      position: PositionNodeRef,
      moveUci: String,
      moves: List[String],
      line: Option[CandidateLineNode],
      scope: EvidenceScope,
      parents: List[EvidenceRef]
  ): Option[EvidenceRecord] =
    MoveAnalyzer.tokenizePv(startFen, moves).flatMap { motifs =>
      Option.when(motifs.nonEmpty) {
        MoveMotifNormalizer.fromMotifs(
          id = id,
          moveUci = moveUci,
          motifs = motifs.distinct,
          position = position,
          line = line.map(_.ref),
          scope = scope,
          parents = parents
        )
      }
    }

  private def relationRecords(
      input: NormalizedMoveReviewInput,
      context: JudgmentAssemblyContext,
      allocator: JudgmentProvenanceAllocator
  ): List[EvidenceRecord] =
    val root = context.position(PositionNodeRole.Before).map(_.ref)
    root.toList.flatMap { rootRef =>
      val continuationLines = context.lines.filterNot(_.role == LineNodeRole.Threat).map(_.line.moves)
      val relationTargetHints = relationTargetHintsFromBoardFacts(context)
      val lineBackedRecords = context.lines.filterNot(_.role == LineNodeRole.Threat).flatMap { line =>
        TacticalRelationEvidence
          .boundedReplay(input.beforeFen, line.line.moves, maxPlies = 8)
          .toList
          .flatMap { replay =>
            val witnesses =
              TacticalRelationEvidence
                .relationWitnesses(
                  replay = replay,
                  playedMove = line.ref.rootMove,
                  targetHints = relationTargetHints,
                  continuationLines = continuationLines,
                  engineScoreCp = Some(line.whitePovEvalCp),
                  engineMate = line.mate,
                  drawishWinPercent = Some(PerspectiveMath.winPercentFromWhiteEval(line.whitePovEvalCp, line.mate))
                )
                .distinctBy(witness => (witness.kind, witness.focusSquares, witness.targetSquare, witness.lineMoves))
            witnesses.zipWithIndex.flatMap { case (witness, index) =>
              RelationFactNormalizer.fromWitness(
                id = allocator.evidenceId(s"relation:${allocator.key(line.role)}:${line.ref.rank}:$index:${witness.kind}"),
                witness = witness,
                position = rootRef,
                line = Some(line.ref),
                scope = line.role.scope,
                confidence = EvidenceConfidence.LegalReplayVerified
              ).map { record =>
                record.copy(parents = lineParents(context, line))
              }
            }
          }
      }
      val transitionLocalRecords =
        context.playedTransition.toList
          .filter(edge => lineForTransition(context, edge).isEmpty)
          .flatMap { edge =>
            TacticalRelationEvidence
              .boundedReplay(input.beforeFen, List(edge.moveUci), maxPlies = 1)
              .toList
              .flatMap { replay =>
                val witnesses =
                  TacticalRelationEvidence
                    .relationWitnesses(
                      replay = replay,
                      playedMove = edge.moveUci,
                      targetHints = relationTargetHints,
                      continuationLines = continuationLines,
                      includeDrawResources = false
                    )
                    .distinctBy(witness => (witness.kind, witness.focusSquares, witness.targetSquare, witness.lineMoves))
                witnesses.zipWithIndex.flatMap { case (witness, index) =>
                  RelationFactNormalizer.fromWitness(
                    id = allocator.evidenceId(s"relation:${allocator.key(edge.role)}:transition:$index:${witness.kind}"),
                    witness = witness,
                    position = rootRef,
                    line = None,
                    scope = edge.role.scope,
                    confidence = EvidenceConfidence.LegalReplayVerified
                  ).map { record =>
                    record.copy(parents = transitionParents(context, edge, None))
                  }
                }
              }
          }
      lineBackedRecords ++ transitionLocalRecords
    }

  private def relationTargetHintsFromBoardFacts(context: JudgmentAssemblyContext): List[String] =
    context.evidenceGraph.records.collect {
      case EvidenceRecord(_, payload: BoardFactEvidence, _) =>
        payload.targetHintSquares.map(_.key)
    }.flatten.distinct

  private def pawnStructureRecords(
      context: JudgmentAssemblyContext,
      allocator: JudgmentProvenanceAllocator
  ): List[EvidenceRecord] =
    context.positions.flatMap { node =>
      for
        features <- node.features
        position <- Fen.read(Standard, Fen.Full(node.ref.fen))
      yield
        val side = node.ref.sideToMove.getOrElse(position.color)
        val profile = PawnStructureAssessor.assess(features, position.board)
        val pawnPlay =
          node.assessment.flatMap { assessment =>
            PawnPlayAssessor.analyze(
              features = features,
              motifs = motifsForLineRole(context, LineNodeRole.BestReference),
              positionAssessment = assessment,
              sideToMove = side
            )
          }
        StrategicFactNormalizer.fromPawnStructure(
          id = allocator.evidenceId(s"pawn-structure:${allocator.key(node.role)}"),
          profile = profile,
          alignment = None,
          pawnPlay = pawnPlay,
          position = node.ref,
          scope = node.role.scope,
          parents = evidenceRefs(context, EvidenceLayer.Board, Some(node.ref), None) ++
            evidenceRefs(context, EvidenceLayer.SinglePosition, Some(node.ref), None)
        )
    }

  private def threatPressureRecords(
      input: NormalizedMoveReviewInput,
      context: JudgmentAssemblyContext,
      allocator: JudgmentProvenanceAllocator
  ): List[EvidenceRecord] =
    val beforeRecords = context.position(PositionNodeRole.Before).toList.flatMap { node =>
      node.assessment.toList.flatMap { assessment =>
        node.ref.sideToMove.toList.map { sideUnderPressure =>
          val threats =
            ThreatPressureAssessor.analyze(
              fen = input.beforeFen,
              motifs = motifsForLineRole(context, LineNodeRole.BestReference),
              multiPv = EvaluationPerspectivePolicy.sideToMovePvLines(sideUnderPressure, input.lines.map(_.line)),
              positionAssessment = assessment,
              sideToMove = sideUnderPressure
            )
          StrategicFactNormalizer.fromThreatPressure(
            id = allocator.evidenceId(s"threat-pressure:${allocator.key(sideUnderPressure.name)}:before"),
            sideUnderPressure = sideUnderPressure,
            threats = threats,
            position = node.ref,
            line = input.referenceLine.flatMap(line => context.line(line.role).map(_.ref)),
            scope = EvidenceScope.BeforePosition,
            parents = evidenceRefs(context, EvidenceLayer.Board, Some(node.ref), None) ++
              evidenceRefs(context, EvidenceLayer.SinglePosition, Some(node.ref), None) ++
              context.line(LineNodeRole.BestReference).toList.flatMap(lineParents(context, _))
          )
        }
      }
    }
    val branchRecords =
      input.threatBranches.flatMap { branch =>
        val lineNodes = branch.lines.flatMap(lineNodeForNormalized(context, _))
        for
          node <- context.positions.find(position =>
            position.role == PositionNodeRole.AfterThreat && position.ref.fen == branch.branchFen
          ).toList
          assessment <- node.assessment.toList
          sideUnderPressure <- node.ref.sideToMove.toList
        yield
          val threats =
            ThreatPressureAssessor.analyze(
              fen = branch.branchFen,
              motifs = lineNodes.flatMap(motifsForLineNode(context, _)).distinct,
              multiPv = EvaluationPerspectivePolicy.sideToMovePvLines(sideUnderPressure, branch.lines.map(_.line)),
              positionAssessment = assessment,
              sideToMove = sideUnderPressure
            )
          StrategicFactNormalizer.fromThreatPressure(
            id = allocator.evidenceId(
              s"threat-pressure:${allocator.key(sideUnderPressure.name)}:branch:${allocator.key(branch.sourceProbeId)}:${allocator.key(branch.probedMoveUci)}"
            ),
            sideUnderPressure = sideUnderPressure,
            threats = threats,
            position = node.ref,
            line = lineNodes.headOption.map(_.ref),
            scope = EvidenceScope.ThreatLine,
            parents = evidenceRefs(context, EvidenceLayer.Board, Some(node.ref), None) ++
              evidenceRefs(context, EvidenceLayer.SinglePosition, Some(node.ref), None) ++
              lineNodes.flatMap(lineParents(context, _))
          )
      }
    val afterLineRecords =
      context.transitions.flatMap { edge =>
        for
          line <- lineForTransition(context, edge).toList
          node <- context.positions.find(_.ref == edge.to).toList
          assessment <- node.assessment.toList
          sideUnderPressure <- node.ref.sideToMove.toList
          suffixMoves = line.line.moves.drop(1)
          if suffixMoves.nonEmpty
          continuationPv = PvLine(
            moves = suffixMoves,
            sideRelativeEvalCp = EvaluationPerspectivePolicy.sideToMoveScoreCp(sideUnderPressure, line.whitePovEvalCp),
            mate = EvaluationPerspectivePolicy.sideToMoveMate(sideUnderPressure, line.mate),
            depth = line.depth
          )
          threats = ThreatPressureAssessor.analyze(
            fen = edge.to.fen,
            motifs = motifsForLineRole(context, line.role),
            multiPv = List(continuationPv),
            positionAssessment = assessment,
            sideToMove = sideUnderPressure
          )
          if threats.hasThreat || threats.defenseRequired || threats.prophylaxisNeeded
        yield
          StrategicFactNormalizer.fromThreatPressure(
            id = allocator.evidenceId(s"threat-pressure:${allocator.key(sideUnderPressure.name)}:${allocator.key(edge.role)}:${edge.moveUci}"),
            sideUnderPressure = sideUnderPressure,
            threats = threats,
            position = node.ref,
            line = Some(line.ref),
            scope = line.role.scope,
            parents = List(edge.evidence) ++
              evidenceRefs(context, EvidenceLayer.Board, Some(node.ref), None) ++
              evidenceRefs(context, EvidenceLayer.SinglePosition, Some(node.ref), None) ++
              lineParents(context, line)
          )
      }
    beforeRecords ++ branchRecords ++ afterLineRecords

  private def structuralDeltaRecords(
      context: JudgmentAssemblyContext,
      allocator: JudgmentProvenanceAllocator
  ): List[EvidenceRecord] =
    context.transitions.flatMap { edge =>
      for
        fromPosition <- Fen.read(Standard, Fen.Full(edge.from.fen))
        toPosition <- Fen.read(Standard, Fen.Full(edge.to.fen))
        side <- edge.from.sideToMove.orElse(Some(fromPosition.color))
        (files, targets, createdTensionFrom) = moveStructureInputs(edge.moveUci)
        if files.nonEmpty
        delta <- StructuralDeltaAnalyzer.delta(
          beforeFen = edge.from.fen,
          beforeBoard = fromPosition.board,
          afterFen = edge.to.fen,
          afterBoard = toPosition.board,
          side = side,
          files = files,
          targets = targets,
          createdTensionFrom = createdTensionFrom,
          moveUci = Some(edge.moveUci)
        )
        if delta.hasConsequence
      yield
        TransitionFactNormalizer.fromStructuralDelta(
          id = allocator.evidenceId(s"structural-delta:${allocator.key(edge.role)}:${edge.moveUci}"),
          delta = delta,
          position = edge.from,
          line = lineForTransition(context, edge).map(_.ref),
          scope = edge.role.scope,
          parents = transitionParents(context, edge, lineForTransition(context, edge)) ++
            evidenceRefs(context, EvidenceLayer.Board, Some(edge.to), None)
        )
    }

  private def strategicFeatureRecords(
      context: JudgmentAssemblyContext,
      allocator: JudgmentProvenanceAllocator
  ): List[EvidenceRecord] =
    context.positions.flatMap { node =>
      val boardParents = evidenceRefs(context, EvidenceLayer.Board, Some(node.ref), None)
      val boardFacts = boardFactEvidence(context, node.ref)
      val targetAnchors =
        boardFacts.toList.flatMap(_.boardAnchors.filter(anchor => anchor.kind == BoardAnchorKind.LooseMaterial))
      val targetFixation =
        Option
        .when(targetAnchors.nonEmpty) {
          StrategicFactNormalizer.fromBoardAnchors(
            id = allocator.evidenceId(
              s"strategic:target-fixation:${allocator.positionKey(node.role, node.ref.fen, node.ref.ply)}"
            ),
            kind = StrategicFactKind.TargetFixation,
            anchors = targetAnchors,
            relatedPlans = Nil,
            confidence = 0.78,
            position = node.ref,
            line = None,
            scope = node.role.scope,
            parents = boardParents
          )
        }
        .toList
      val outpostAnchors =
        boardFacts.toList.flatMap(_.boardAnchors.filter(anchor => anchor.kind == BoardAnchorKind.Outpost))
      val outpost =
        Option.when(outpostAnchors.nonEmpty) {
          StrategicFactNormalizer.fromBoardAnchors(
            id = allocator.evidenceId(s"strategic:outpost:${allocator.positionKey(node.role, node.ref.fen, node.ref.ply)}"),
            kind = StrategicFactKind.Outpost,
            anchors = outpostAnchors,
            relatedPlans = Nil,
            confidence = 0.78,
            position = node.ref,
            line = None,
            scope = node.role.scope,
            parents = boardParents
          )
        }.toList
      val endgameFacts = node.facts.collect {
        case fact: Fact.KingActivity            => fact
        case fact: Fact.Opposition              => fact
        case fact: Fact.RuleOfSquare            => fact
        case fact: Fact.TriangulationOpportunity => fact
        case fact: Fact.RookEndgamePattern      => fact
        case fact: Fact.EndgameOutcome          => fact
        case fact: Fact.Zugzwang                => fact
        case fact: Fact.PawnPromotion           => fact
        case fact: Fact.StalemateThreat         => fact
      }
      val endgame =
        Option.when(endgameFacts.nonEmpty) {
          StrategicFactNormalizer.fromFacts(
            id = allocator.evidenceId(s"strategic:endgame:${allocator.positionKey(node.role, node.ref.fen, node.ref.ply)}"),
            kind = StrategicFactKind.Endgame,
            facts = endgameFacts,
            relatedPlans = Nil,
            confidence = 0.82,
            position = node.ref,
            line = None,
            scope = node.role.scope,
            parents = boardParents
          )
        }.toList
      val featureRecords = node.features.toList.flatMap { features =>
        featureStrategicRecords(node, features, allocator, boardParents)
      }
      targetFixation ++ outpost ++ endgame ++ featureRecords
    }

  private def boardFactEvidence(context: JudgmentAssemblyContext, position: PositionNodeRef): Option[BoardFactEvidence] =
    context.evidenceGraph.records.collectFirst {
      case EvidenceRecord(ref, payload: BoardFactEvidence, _) if ref.position == position =>
        payload
    }

  private def featureStrategicRecords(
      node: PositionNode,
      features: PositionFeatures,
      allocator: JudgmentProvenanceAllocator,
      parents: List[EvidenceRef]
  ): List[EvidenceRecord] =
    val side = node.ref.sideToMove.getOrElse(features.sideToMove)
    val nodeKey = allocator.positionKey(node.role, node.ref.fen, node.ref.ply)
    val spaceEdge = sideSpaceEdge(features, side)
    val mobilityEdge = sideMobilityEdge(features, side)
    val opponentLowMobility = lowMobility(features, !side)
    val strategicState = PositionAnalyzer.extractStrategicState(node.ref.fen)
    List(
      Option.when(semiOpenFiles(features, side) > 0 || rookOnSeventh(features, side)) {
        StrategicFactNormalizer.fromFacts(
          id = allocator.evidenceId(s"strategic:file-control:$nodeKey"),
          kind = StrategicFactKind.FileControl,
          facts = Nil,
          relatedPlans = Nil,
          confidence = if rookOnSeventh(features, side) then 0.82 else 0.72,
          position = node.ref,
          line = None,
          scope = node.role.scope,
          parents = parents
        )
      },
      Option.when(spaceEdge >= 2 || features.centralSpace.lockedCenter && spaceEdge > 0) {
        StrategicFactNormalizer.fromFacts(
          id = allocator.evidenceId(s"strategic:space:$nodeKey"),
          kind = StrategicFactKind.Space,
          facts = Nil,
          relatedPlans = Nil,
          confidence = if spaceEdge >= 3 then 0.80 else 0.72,
          position = node.ref,
          line = None,
          scope = node.role.scope,
          parents = parents
        )
      },
      Option.when(mobilityEdge >= 5 || opponentLowMobility >= 2) {
        StrategicFactNormalizer.fromFacts(
          id = allocator.evidenceId(s"strategic:activity:$nodeKey"),
          kind = StrategicFactKind.Activity,
          facts = Nil,
          relatedPlans = Nil,
          confidence = if mobilityEdge >= 8 then 0.78 else 0.70,
          position = node.ref,
          line = None,
          scope = node.role.scope,
          parents = parents
        )
      },
      Option.when(colorComplexClamp(strategicState, side) || opponentLowMobility >= 3 && spaceEdge >= 1) {
        StrategicFactNormalizer.fromFacts(
          id = allocator.evidenceId(s"strategic:counterplay-restraint:$nodeKey"),
          kind = StrategicFactKind.CounterplayRestraint,
          facts = Nil,
          relatedPlans = Nil,
          confidence = 0.76,
          position = node.ref,
          line = None,
          scope = node.role.scope,
          parents = parents
        )
      }
    ).flatten

  private def semiOpenFiles(features: PositionFeatures, side: Color): Int =
    if side.white then features.lineControl.whiteSemiOpenFiles else features.lineControl.blackSemiOpenFiles

  private def rookOnSeventh(features: PositionFeatures, side: Color): Boolean =
    if side.white then features.lineControl.whiteRookOn7th else features.lineControl.blackRookOn7th

  private def sideSpaceEdge(features: PositionFeatures, side: Color): Int =
    if side.white then features.centralSpace.spaceDiff else -features.centralSpace.spaceDiff

  private def sideMobilityEdge(features: PositionFeatures, side: Color): Int =
    if side.white then features.activity.whitePseudoMobility - features.activity.blackPseudoMobility
    else features.activity.blackPseudoMobility - features.activity.whitePseudoMobility

  private def lowMobility(features: PositionFeatures, side: Color): Int =
    if side.white then features.activity.whiteLowMobilityPieces else features.activity.blackLowMobilityPieces

  private def colorComplexClamp(
      state: Option[lila.chessjudgment.analysis.position.StrategicStateFeatures],
      side: Color
  ): Boolean =
    state.exists { s =>
      if side.white then s.whiteColorComplexClamp else s.blackColorComplexClamp
    }

  private def featureApplicabilityRecords(
      input: NormalizedMoveReviewInput,
      context: JudgmentAssemblyContext,
      allocator: JudgmentProvenanceAllocator
  ): List[EvidenceRecord] =
    val root = context.position(PositionNodeRole.Before)
    root.toList.flatMap { rootNode =>
      val openingPhase =
        rootNode.assessment.exists(_.gamePhase.isOpening) ||
          rootNode.features.exists(_.materialPhase.phase == "opening")
      val canAssessOpening =
        openingPhase || input.opening.nonEmpty || input.openingRecognition.nonEmpty
      val signals =
        input.openingSignals ++ List(
          Option.when(openingPhase)(OpeningContextSignal.OpeningPhase)
        ).flatten
      val contextRecord = Option.when(
        signals.nonEmpty ||
          input.opening.nonEmpty ||
          input.openingRecognition.nonEmpty ||
          input.openingThemePrior.nonEmpty ||
          canAssessOpening
      ) {
        OpeningContextFactNormalizer.fromContext(
          id = allocator.evidenceId("opening-context:before"),
          identity = input.opening,
          signals = signals,
          recognition = input.openingRecognition,
          themePrior = input.openingThemePrior,
          position = rootNode.ref,
          line = None,
          scope = EvidenceScope.BeforePosition,
          confidence = openingContextConfidence(input.opening, input.openingRecognition, openingPhase),
          parents = openingContextParents(context, rootNode.ref)
        )
      }
      val anchorRecords = featureAnchorRecords(context, rootNode, allocator)
      val anchors = anchorRecords.collect { case EvidenceRecord(_, FeatureAnchorEvidence(anchor), _) => anchor }
      val applicabilityRecord =
        for
          assessment <- assessApplicability(input, rootNode, anchors)
        yield
          applicabilityAssessmentRecord(
            id = allocator.evidenceId("applicability:before"),
            assessment = assessment,
            position = rootNode.ref,
            line = None,
            scope = EvidenceScope.BeforePosition,
            confidence = applicabilityConfidence(assessment),
            parents = (contextRecord.map(_.ref).toList ++ anchorRecords.map(_.ref)).distinctBy(_.id)
          )
      contextRecord.toList ++ anchorRecords ++ applicabilityRecord.toList
    }

  private def openingContextParents(
      context: JudgmentAssemblyContext,
      position: PositionNodeRef
  ): List[EvidenceRef] =
    val base =
      List(
        EvidenceLayer.Board,
        EvidenceLayer.SinglePosition,
        EvidenceLayer.MoveMotif
      ).flatMap(layer => evidenceRefs(context, layer, Some(position), None))
    val pawnStructure =
      context.evidenceGraph.records.collect {
        case EvidenceRecord(ref, PawnStructureFactEvidence(profile, _, pawnPlay), _)
            if ref.position == position &&
              (profile.primary != lila.chessjudgment.model.structure.StructureId.Unknown ||
                pawnPlay.exists(_.primaryDriver != PawnPlayDriver.Quiet)) =>
          ref
      }
    (base ++ pawnStructure).distinctBy(_.id)

  private def featureAnchorRecords(
      context: JudgmentAssemblyContext,
      node: PositionNode,
      allocator: JudgmentProvenanceAllocator
  ): List[EvidenceRecord] =
    val boardParents =
      evidenceRefs(context, EvidenceLayer.Board, Some(node.ref), None) ++
        evidenceRefs(context, EvidenceLayer.SinglePosition, Some(node.ref), None)
    val featureAnchors =
      node.features.toList.flatMap { features =>
        val side = node.ref.sideToMove.getOrElse(features.sideToMove)
        boardFeatureAnchors(features, side).map(anchor => anchor -> boardParents)
      }
    val evidenceAnchors =
      context.evidenceGraph.records
        .filter(_.ref.position == node.ref)
        .flatMap(evidenceFeatureAnchors)
    (featureAnchors ++ evidenceAnchors)
      .distinctBy { case (anchor, parents) =>
        (anchor.theme, anchor.signal, anchor.sourceLayer, parents.map(_.id).sorted.mkString("|"))
      }
      .map { case (anchor, parents) =>
        featureAnchorRecord(
          id = allocator.evidenceId(
            s"feature-anchor:${allocator.key(anchor.theme)}:${allocator.key(anchor.sourceLayer)}:${allocator.key(anchor.signal)}:${parents.headOption.map(parent => allocator.key(parent.id)).getOrElse("root")}"
          ),
          anchor = anchor,
          position = node.ref,
          line = None,
          scope = EvidenceScope.BeforePosition,
          confidence = if anchor.sourceLayer == EvidenceLayer.Board then EvidenceConfidence.BoardDerived else EvidenceConfidence.Mixed,
          parents = parents.distinctBy(_.id)
        )
      }

  private def openingContextConfidence(
      identity: Option[OpeningIdentity],
      recognition: Option[OpeningRecognition],
      openingPhase: Boolean
  ): EvidenceConfidence =
    if (identity.nonEmpty || recognition.nonEmpty) && openingPhase then EvidenceConfidence.Mixed
    else if openingPhase || identity.nonEmpty || recognition.nonEmpty then EvidenceConfidence.BoardDerived
    else EvidenceConfidence.Heuristic

  private def featureAnchorRecord(
      id: String,
      anchor: FeatureAnchor,
      position: PositionNodeRef,
      line: Option[LineNodeRef],
      scope: EvidenceScope,
      confidence: EvidenceConfidence,
      parents: List[EvidenceRef]
  ): EvidenceRecord =
    val ref =
      EvidenceRef(
        id = id,
        producer = EvidenceProducer.FeatureAnchorProducer,
        layer = EvidenceLayer.FeatureAnchor,
        position = position,
        line = line,
        scope = scope,
        confidence = confidence
      )
    EvidenceRecord(
      ref = ref,
      payload = FeatureAnchorEvidence(anchor),
      parents = parents
    )

  private def applicabilityAssessmentRecord(
      id: String,
      assessment: ApplicabilityAssessment,
      position: PositionNodeRef,
      line: Option[LineNodeRef],
      scope: EvidenceScope,
      confidence: EvidenceConfidence,
      parents: List[EvidenceRef]
  ): EvidenceRecord =
    val ref =
      EvidenceRef(
        id = id,
        producer = EvidenceProducer.ApplicabilityAssessmentProducer,
        layer = EvidenceLayer.ApplicabilityAssessment,
        position = position,
        line = line,
        scope = scope,
        confidence = confidence
      )
    EvidenceRecord(
      ref = ref,
      payload = ApplicabilityAssessmentEvidence(assessment),
      parents = parents
    )

  private def assessApplicability(
      input: NormalizedMoveReviewInput,
      node: PositionNode,
      anchors: List[FeatureAnchor]
  ): Option[ApplicabilityAssessment] =
    val observedThemes = anchors.map(_.theme).distinct
    Option.when(observedThemes.nonEmpty) {
      val priorThemes = input.openingThemePrior.toList.flatMap(_.themes).distinct
      val supported = priorThemes.filter(theme =>
        anchors.exists(anchor => anchor.theme == theme && proofSignalOpeningAnchor(anchor))
      )
      val unverified = priorThemes.filterNot(supported.contains)
      val observedOnly = observedThemes.filterNot(priorThemes.contains)
      val ambiguousRecognition = input.openingRecognition.exists(_.candidates.drop(1).nonEmpty)
      val applicability = featureApplicability(input, node, anchors, supported)
      val status =
        if applicability == FeatureApplicability.Contraindicated then ApplicabilityStatus.Contradicted
        else if priorThemes.isEmpty then ApplicabilityStatus.InternalOnly
        else if ambiguousRecognition && supported.isEmpty then ApplicabilityStatus.Ambiguous
        else if supported.nonEmpty && unverified.nonEmpty then ApplicabilityStatus.PartiallySupported
        else if supported.nonEmpty then ApplicabilityStatus.Supported
        else ApplicabilityStatus.Unverified
      ApplicabilityAssessment(
        applicability = applicability,
        status = status,
        observedThemes = observedThemes,
        supportedThemes = supported,
        unverifiedPriorThemes = unverified,
        observedOnlyThemes = observedOnly
      )
    }

  private def proofSignalOpeningAnchor(anchor: FeatureAnchor): Boolean =
    anchor.sourceLayer != EvidenceLayer.Board

  private def featureApplicability(
      input: NormalizedMoveReviewInput,
      node: PositionNode,
      anchors: List[FeatureAnchor],
      supportedThemes: List[OpeningTheme]
  ): FeatureApplicability =
    val phase = node.assessment.map(_.gamePhase)
    val featurePhase = node.features.map(_.materialPhase.phase)
    val openingPhase = phase.exists(_.isOpening) || featurePhase.contains("opening")
    val openingWindow = input.beforePly <= OpeningRelevanceMaxPly
    val middlegamePhase = phase.exists(_.isMiddlegame) || featurePhase.contains("middlegame")
    val endgamePhase = phase.exists(_.isEndgame) || featurePhase.contains("endgame")
    val openingContext = input.opening.nonEmpty || input.openingRecognition.nonEmpty || input.openingThemePrior.nonEmpty
    val denseMaterial =
      phase.exists(result => result.queensOnBoard || result.minorPiecesCount >= 4) ||
        node.features.exists(features => features.materialPhase.whiteMaterial + features.materialPhase.blackMaterial >= 48)
    val openingSensitiveTheme =
      anchors.exists(anchor =>
        anchor.theme == OpeningTheme.Development ||
          anchor.theme == OpeningTheme.GambitInitiative ||
          anchor.theme == OpeningTheme.KingSafety ||
          anchor.theme == OpeningTheme.CenterControl
      )
    val themePriorSupportedByInternalAnchor = supportedThemes.nonEmpty
    if endgamePhase && !openingWindow then FeatureApplicability.EndgameRelevant
    else if openingPhase && !openingWindow && themePriorSupportedByInternalAnchor then FeatureApplicability.MiddlegameRelevant
    else if endgamePhase && openingSensitiveTheme && !themePriorSupportedByInternalAnchor then FeatureApplicability.Contraindicated
    else if openingWindow && denseMaterial && openingSensitiveTheme && themePriorSupportedByInternalAnchor then
      FeatureApplicability.OpeningRelevant
    else if openingWindow && themePriorSupportedByInternalAnchor then FeatureApplicability.OpeningRelevant
    else if endgamePhase then FeatureApplicability.EndgameRelevant
    else if openingContext || middlegamePhase || anchors.exists(anchor => anchor.sourceLayer == EvidenceLayer.PlanPressure || anchor.sourceLayer == EvidenceLayer.Strategic) then
      FeatureApplicability.MiddlegameRelevant
    else FeatureApplicability.ObservedOnly

  private def applicabilityConfidence(assessment: ApplicabilityAssessment): EvidenceConfidence =
    assessment.status match
      case ApplicabilityStatus.Supported | ApplicabilityStatus.PartiallySupported =>
        EvidenceConfidence.Mixed
      case ApplicabilityStatus.InternalOnly =>
        EvidenceConfidence.BoardDerived
      case ApplicabilityStatus.Unverified | ApplicabilityStatus.Ambiguous |
          ApplicabilityStatus.Contradicted =>
        EvidenceConfidence.Heuristic

  private def boardFeatureAnchors(features: PositionFeatures, side: Color): List[FeatureAnchor] =
    List(
      Option.when(centerControlSignal(features))(
        FeatureAnchor(
          OpeningTheme.CenterControl,
          FeatureAnchorSignal.CenterControlObserved,
          EvidenceLayer.Board,
          0.7
        )
      ),
      Option.when(developmentSignal(features, side))(
        FeatureAnchor(
          OpeningTheme.Development,
          FeatureAnchorSignal.DevelopmentTempoObserved,
          EvidenceLayer.Board,
          0.65
        )
      ),
      Option.when(pawnStructureSignal(features))(
        FeatureAnchor(
          OpeningTheme.PawnStructure,
          FeatureAnchorSignal.PawnStructureObserved,
          EvidenceLayer.Board,
          0.6
        )
      ),
      Option.when(gambitInitiativeSignal(features, side))(
        FeatureAnchor(
          OpeningTheme.GambitInitiative,
          FeatureAnchorSignal.CompensationObserved,
          EvidenceLayer.Board,
          0.75
        )
      ),
      Option.when(kingSafetySignal(features, side))(
        FeatureAnchor(
          OpeningTheme.KingSafety,
          FeatureAnchorSignal.KingSafetyObserved,
          EvidenceLayer.Board,
          0.55
        )
      )
    ).flatten

  private def evidenceFeatureAnchors(record: EvidenceRecord): List[(FeatureAnchor, List[EvidenceRef])] =
    record.payload match
      case PawnStructureFactEvidence(profile, alignment, pawnPlay)
          if profile.primary != lila.chessjudgment.model.structure.StructureId.Unknown ||
            pawnPlay.exists(_.primaryDriver != PawnPlayDriver.Quiet) ||
            alignment.nonEmpty =>
        List(
          FeatureAnchor(
            OpeningTheme.PawnStructure,
            FeatureAnchorSignal.PawnStructureObserved,
            EvidenceLayer.PawnStructure,
            profile.confidence.max(0.6)
          ) -> (record.ref :: record.parents)
        )
      case PlanPressureEvidence(_, activePlans)
          if activePlans.primary.plan.category == PlanCategory.Opening ||
            activePlans.secondary.exists(_.plan.category == PlanCategory.Opening) ||
            activePlans.compatibilityEvents.exists(_.adjustment == CompatibilityAdjustment.OpeningPhase) =>
        List(
          FeatureAnchor(
            OpeningTheme.PlanPressure,
            FeatureAnchorSignal.PlanPressureObserved,
            EvidenceLayer.PlanPressure,
            activePlans.primary.score.max(0.6)
          ) -> (record.ref :: record.parents)
        )
      case StructuralDeltaEvidence(delta) if delta.hasConsequence =>
        val center =
          Option.when(
            delta.createdTension.nonEmpty ||
              delta.resolvedTension.nonEmpty ||
              delta.pawnTensionDelta > 0 ||
              delta.lineUnlockDelta > 0 ||
              delta.centerControlDelta > 0
          )(
            FeatureAnchor(
              OpeningTheme.CenterControl,
              FeatureAnchorSignal.StructuralDeltaObserved,
              EvidenceLayer.StructuralDelta,
              0.65
            )
          )
        val pressure =
          Option.when(delta.createdTargetPressure.nonEmpty || delta.targetPressureGain > delta.targetPressureRelease || delta.targetPressureDelta > 0)(
            FeatureAnchor(
              OpeningTheme.PlanPressure,
              FeatureAnchorSignal.StructuralDeltaObserved,
              EvidenceLayer.StructuralDelta,
              0.66
            )
          )
        val structure =
          Option.when(
              delta.openedFiles.nonEmpty ||
              delta.semiOpenedFiles.nonEmpty ||
              delta.fileAccessDelta > 0 ||
              delta.fileOccupation.nonEmpty ||
              delta.newWeakPawns.nonEmpty ||
              delta.newWeakSquares.nonEmpty
          )(
            FeatureAnchor(
              OpeningTheme.PawnStructure,
              FeatureAnchorSignal.StructuralDeltaObserved,
              EvidenceLayer.StructuralDelta,
              0.65
            )
          )
        val development =
          Option.when(delta.mobilityDelta > 0 || delta.developmentDelta > 0 || delta.lineUnlockDelta > 0)(
            FeatureAnchor(
              OpeningTheme.Development,
              FeatureAnchorSignal.StructuralDeltaObserved,
              EvidenceLayer.StructuralDelta,
              0.62
            )
          )
        val kingSafety =
          Option.when(delta.kingShelterDelta > 0)(
            FeatureAnchor(
              OpeningTheme.KingSafety,
              FeatureAnchorSignal.StructuralDeltaObserved,
              EvidenceLayer.StructuralDelta,
              0.66
            )
          )
        List(center, pressure, structure, development, kingSafety).flatten.map(_ -> (record.ref :: record.parents))
      case payload @ StrategicFactEvidence(kind, _, _, confidence) if payload.hasTypedSupport =>
        val theme =
          kind match
            case StrategicFactKind.Space        => Some(OpeningTheme.CenterControl)
            case StrategicFactKind.Activity     => Some(OpeningTheme.Development)
            case StrategicFactKind.Compensation => Some(OpeningTheme.GambitInitiative)
            case StrategicFactKind.Structure    => Some(OpeningTheme.PawnStructure)
            case StrategicFactKind.PlanPressure => Some(OpeningTheme.PlanPressure)
            case _                              => None
        theme
          .map(openingTheme =>
            FeatureAnchor(
              openingTheme,
              FeatureAnchorSignal.PlanPressureObserved,
              EvidenceLayer.Strategic,
              confidence.max(0.55)
            ) -> (record.ref :: record.parents)
          )
          .toList
      case _ => Nil

  private def centerControlSignal(features: PositionFeatures): Boolean =
    features.centralSpace.whiteCentralPawns + features.centralSpace.blackCentralPawns > 0 ||
      features.centralSpace.whiteCenterControl + features.centralSpace.blackCenterControl > 0 ||
      features.centralSpace.pawnTensionCount > 0 ||
      features.centralSpace.lockedCenter ||
      features.centralSpace.openCenter

  private def developmentSignal(features: PositionFeatures, side: Color): Boolean =
    val sideLag =
      if side.white then features.activity.whiteDevelopmentLag else features.activity.blackDevelopmentLag
    val opponentLag =
      if side.white then features.activity.blackDevelopmentLag else features.activity.whiteDevelopmentLag
    sideLag != opponentLag || sideLag <= 2 || opponentLag <= 2

  private def pawnStructureSignal(features: PositionFeatures): Boolean =
    val pawns = features.pawns
    pawns.whiteIsolatedPawns + pawns.blackIsolatedPawns > 0 ||
      pawns.whiteDoubledPawns + pawns.blackDoubledPawns > 0 ||
      pawns.whitePassedPawns + pawns.blackPassedPawns > 0 ||
      pawns.whiteIQP ||
      pawns.blackIQP ||
      pawns.whiteHangingPawns ||
      pawns.blackHangingPawns ||
      features.centralSpace.pawnTensionCount > 0

  private def gambitInitiativeSignal(features: PositionFeatures, side: Color): Boolean =
    val materialForSide =
      if side.white then features.materialPhase.materialDiff else -features.materialPhase.materialDiff
    materialForSide <= -100 && (centerControlSignal(features) || developmentSignal(features, side))

  private def kingSafetySignal(features: PositionFeatures, side: Color): Boolean =
    if side.white then
      features.kingSafety.whiteCastledSide != "none" ||
        features.kingSafety.whiteCastlingRights != "none" ||
        features.kingSafety.whiteKingExposedFiles > 0
    else
      features.kingSafety.blackCastledSide != "none" ||
        features.kingSafety.blackCastlingRights != "none" ||
        features.kingSafety.blackKingExposedFiles > 0

  private def planPressureRecords(
      input: NormalizedMoveReviewInput,
      context: JudgmentAssemblyContext,
      allocator: JudgmentProvenanceAllocator
  ): List[EvidenceRecord] =
    context.position(PositionNodeRole.Before).toList.flatMap { node =>
      val records = for
        features <- node.features
        assessment <- node.assessment
        side <- node.ref.sideToMove.orElse(input.sideToMove)
        initialPosition <- Fen.read(Standard, Fen.Full(node.ref.fen))
      yield
        val motifs = motifsForLineRole(context, LineNodeRole.BestReference)
        val pawnStructureRecord = pawnStructureRecordFor(context, node.ref)
        val pawnStructure = pawnStructureRecord.map(_._2)
        val threatsToUs = threatPressureEvidence(context, node.ref, side)
        val planContext =
          PlanInteractionContext(
            whitePovEvalCp = input.currentWhitePovEvalCp,
            positionAssessment = Some(assessment),
            pawnAnalysis = pawnStructure.flatMap(_.pawnPlay),
            threatsToUs = threatsToUs.map(_.threats),
            isWhiteToMove = side.white,
            positionKey = Some(node.ref.fen),
            features = Some(features),
            initialPos = Some(initialPosition),
            structureProfile = pawnStructure.map(_.profile),
            planAlignment = pawnStructure.flatMap(_.alignment)
        )
        val scoring = PlanMatcher.matchPlans(motifs, planContext, side)
        val alignment =
          for
            pawn <- pawnStructure
            entry <- StructuralPlaybook.lookup(pawn.profile.primary)
          yield
            PlanAlignmentScorer.score(
              structureProfile = pawn.profile,
              playbookEntry = entry,
              topPlans = scoring.topPlans,
              motifs = motifs,
              pawnAnalysis = pawn.pawnPlay,
              sideToMove = side
            )
        val alignedPlanContext = planContext.copy(planAlignment = alignment)
        PlanMatcher.toActivePlans(scoring.topPlans, scoring.compatibilityEvents).toList.flatMap { activePlans =>
          val planPressure =
            StrategicFactNormalizer.fromPlanPressure(
              id = allocator.evidenceId("plan-pressure:before"),
              scoring = scoring,
              activePlans = activePlans,
              position = node.ref,
              line = context.line(LineNodeRole.BestReference).map(_.ref),
              scope = EvidenceScope.BeforePosition,
              parents = evidenceRefs(context, EvidenceLayer.Board, Some(node.ref), None) ++
                evidenceRefs(context, EvidenceLayer.SinglePosition, Some(node.ref), None) ++
                evidenceRefs(context, EvidenceLayer.PawnStructure, Some(node.ref), None) ++
                evidenceRefs(context, EvidenceLayer.ThreatPressure, Some(node.ref), None) ++
                context.line(LineNodeRole.BestReference).toList.flatMap(lineParents(context, _))
            )
          val alignedPawnStructure =
            for
              (original, _) <- pawnStructureRecord
              pawn <- pawnStructure
              planAlignment <- alignment
            yield
              StrategicFactNormalizer.fromPawnStructure(
                id = original.ref.id,
                profile = pawn.profile,
                alignment = Some(planAlignment),
                pawnPlay = pawn.pawnPlay,
                position = node.ref,
                scope = original.ref.scope,
                parents = (original.parents :+ planPressure.ref).distinctBy(_.id)
              )
          val planTransition =
            TransitionFactNormalizer.fromPlanTransition(
              id = allocator.evidenceId("plan-transition:before"),
              transition = TransitionAnalyzer.analyze(activePlans, None, alignedPlanContext),
              position = node.ref,
              line = context.line(LineNodeRole.BestReference).map(_.ref),
              scope = EvidenceScope.BeforePosition,
              parents = planPressure.ref :: planPressure.parents
            )
          List(planPressure) ++ alignedPawnStructure.toList ++ List(planTransition)
        }
      records.getOrElse(Nil)
    }

  private def motifsForLineRole(context: JudgmentAssemblyContext, role: LineNodeRole): List[Motif] =
    context.line(role).map(_.ref).flatMap { lineRef =>
      context.evidenceGraph.records.collectFirst {
        case EvidenceRecord(ref, MoveMotifEvidence(_, motifs), _) if ref.line.contains(lineRef) => motifs
      }
    }.getOrElse(Nil)

  private def motifsForLineNode(context: JudgmentAssemblyContext, line: CandidateLineNode): List[Motif] =
    context.evidenceGraph.records.collectFirst {
      case EvidenceRecord(ref, MoveMotifEvidence(_, motifs), _) if ref.line.contains(line.ref) => motifs
    }.getOrElse(Nil)

  private def startPositionForLine(
      input: NormalizedMoveReviewInput,
      context: JudgmentAssemblyContext,
      root: PositionNode,
      line: CandidateLineNode
  ): PositionNode =
    if line.role != LineNodeRole.Threat then root
    else
      input.threatBranches
        .find(branch => branch.lines.exists(normalizedLineMatches(_, line)))
        .flatMap(branch =>
          context.positions.find(position =>
            position.role == PositionNodeRole.AfterThreat && position.ref.fen == branch.branchFen
          )
        )
        .getOrElse(root)

  private def lineNodeForNormalized(
      context: JudgmentAssemblyContext,
      normalized: NormalizedCandidateLine
  ): Option[CandidateLineNode] =
    context.lines.find(line => normalizedLineMatches(normalized, line))

  private def normalizedLineMatches(normalized: NormalizedCandidateLine, line: CandidateLineNode): Boolean =
    normalized.role == line.role &&
      normalized.rank == line.ref.rank &&
      normalized.rootMove.contains(line.ref.rootMove)

  private def moveStructureInputs(moveUci: String): (List[Char], List[String], Option[String]) =
    val normalized = MoveReviewInputNormalizer.normalizeUci(moveUci)
    val origin = normalized.take(2)
    val target = normalized.drop(2).take(2)
    val files = List(origin.headOption, target.headOption).flatten.filter(file => file >= 'a' && file <= 'h').distinct
    val targets = Option.when(target.matches("[a-h][1-8]"))(target).toList
    val createdTensionFrom = Option.when(origin.matches("[a-h][1-8]"))(origin)
    (files, targets, createdTensionFrom)

  private def pawnStructureRecordFor(
      context: JudgmentAssemblyContext,
      position: PositionNodeRef
  ): Option[(EvidenceRecord, PawnStructureFactEvidence)] =
    context.evidenceGraph.records.collectFirst {
      case record @ EvidenceRecord(ref, payload: PawnStructureFactEvidence, _) if ref.position == position =>
        record -> payload
    }

  private def threatPressureEvidence(
      context: JudgmentAssemblyContext,
      position: PositionNodeRef,
      sideUnderPressure: Color
  ): Option[ThreatPressureEvidence] =
    context.evidenceGraph.records.collectFirst {
      case EvidenceRecord(ref, payload: ThreatPressureEvidence, _)
          if ref.position == position && payload.sideUnderPressure == sideUnderPressure =>
        payload
    }

  private def transitionParents(
      context: JudgmentAssemblyContext,
      edge: MoveTransitionEdge,
      line: Option[CandidateLineNode]
  ): List[EvidenceRef] =
    List(edge.evidence) ++
      line.toList.flatMap(lineParents(context, _)) ++
      evidenceRefs(context, EvidenceLayer.Board, Some(edge.from), None)

  private def lineParents(
      context: JudgmentAssemblyContext,
      line: CandidateLineNode
  ): List[EvidenceRef] =
    evidenceRefs(context, EvidenceLayer.Line, None, Some(line.ref)) ++
      evidenceRefs(context, EvidenceLayer.Eval, None, Some(line.ref))

  private def evidenceRefs(
      context: JudgmentAssemblyContext,
      layer: EvidenceLayer,
      position: Option[PositionNodeRef],
      line: Option[LineNodeRef]
  ): List[EvidenceRef] =
    context.evidenceGraph.records.collect {
      case record
          if record.payload.layer == layer &&
            position.forall(_ == record.ref.position) &&
            line.forall(record.ref.line.contains) =>
        record.ref
    }

  private def lineForTransition(
      context: JudgmentAssemblyContext,
      edge: MoveTransitionEdge
  ): Option[CandidateLineNode] =
    context.lines.find(line => line.role == edge.role.lineRole && line.ref.rootMove == edge.moveUci)

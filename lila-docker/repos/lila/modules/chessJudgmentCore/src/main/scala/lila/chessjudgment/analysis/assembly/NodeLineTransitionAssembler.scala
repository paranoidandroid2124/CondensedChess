package lila.chessjudgment.analysis.assembly

import chess.format.Fen
import chess.variant.Standard
import lila.chessjudgment.analysis.evaluation.{ EvalFactNormalizer, EvaluationPerspectivePolicy }
import lila.chessjudgment.analysis.line.{ ForcedLineTruth, LineFactNormalizer, PrincipalVariationEvidence }
import lila.chessjudgment.analysis.position.{ FactExtractor, PositionAnalyzer, PositionFactNormalizer }
import lila.chessjudgment.analysis.singlePosition.{ SinglePositionAssessor, SinglePositionFactNormalizer }
import lila.chessjudgment.analysis.transition.TransitionFactNormalizer
import lila.chessjudgment.model.Fact
import lila.chessjudgment.model.strategic.VariationLine
import lila.chessjudgment.model.judgment.*

final case class PositionNodeAssembly(
    node: PositionNode,
    evidence: List[EvidenceRecord]
)

final case class CandidateLineAssembly(
    node: CandidateLineNode,
    evidence: List[EvidenceRecord]
)

final case class TransitionEdgeAssembly(
    edge: MoveTransitionEdge,
    evidence: List[EvidenceRecord]
)

final case class NodeLineTransitionAssembly(
    input: NormalizedMoveReviewInput,
    context: JudgmentAssemblyContext
)

object PositionNodeAssembler:

  def fromFen(
      input: NormalizedMoveReviewInput,
      role: PositionNodeRole,
      fen: String,
      ply: Int,
      allocator: JudgmentProvenanceAllocator,
      scope: EvidenceScope,
      includeAssessment: Boolean,
      assessmentSourceLines: Option[List[VariationLine]] = None,
      assessmentEvalCp: Option[Int] = None
  ): Option[PositionNodeAssembly] =
    Fen.read(Standard, Fen.Full(fen)).map { position =>
      val ref = allocator.positionRef(role, fen, ply, Some(position.color))
      val nodeKey = allocator.positionKey(role, fen, ply)
      val features = PositionAnalyzer.extractFeatures(fen, ply)
      val facts = FactExtractor.extractStaticFacts(position.board, position.color)
      val boardRecord =
        PositionFactNormalizer.fromBoardFacts(
          id = allocator.evidenceId(s"board:$nodeKey"),
          facts = facts,
          features = features,
          position = ref,
          scope = scope
        )
      val assessmentRecord =
        Option
          .when(includeAssessment)(features)
          .flatten
          .map { positionFeatures =>
            val assessmentSide = ref.sideToMove.getOrElse(position.color)
            val assessment =
              SinglePositionAssessor.classify(
                features = positionFeatures,
                multiPv = assessmentLines(input, role, assessmentSide, assessmentSourceLines),
                currentEval = assessmentEval(input, role, assessmentEvalCp)
              )
            SinglePositionFactNormalizer.fromAssessment(
              id = allocator.evidenceId(s"single-position:$nodeKey"),
              assessment = assessment,
              position = ref,
              scope = scope,
              parents = List(boardRecord.ref)
            )
          }
      val records = boardRecord :: assessmentRecord.toList
      val node =
        PositionNodeBuilder.fromAnalysis(
          role = role,
          ref = ref,
          facts = facts,
          features = features,
          assessment = assessmentRecord.collect { case EvidenceRecord(_, SinglePositionEvidence(assessment), _) => assessment },
          evidence = records.map(_.ref)
        )
      PositionNodeAssembly(node, records)
    }

  private def assessmentLines(
      input: NormalizedMoveReviewInput,
      role: PositionNodeRole,
      sideToMove: chess.Color,
      sourceLines: Option[List[VariationLine]]
  ): List[lila.chessjudgment.analysis.singlePosition.PvLine] =
    val lines =
      sourceLines.getOrElse {
        role match
          case PositionNodeRole.Before =>
            input.lines.map(_.line)
          case PositionNodeRole.AfterPlayed =>
            input.playedLine.map(line => line.line.copy(moves = line.line.moves.drop(1))).toList
          case PositionNodeRole.AfterReference =>
            input.referenceLine.map(line => line.line.copy(moves = line.line.moves.drop(1))).toList
          case PositionNodeRole.AfterAlternative =>
            input.lines
              .filter(_.role == LineNodeRole.Alternative)
              .map(line => line.line.copy(moves = line.line.moves.drop(1)))
          case PositionNodeRole.AfterThreat =>
            Nil
      }
    EvaluationPerspectivePolicy.sideToMovePvLines(sideToMove, lines)

  private def assessmentEval(
      input: NormalizedMoveReviewInput,
      role: PositionNodeRole,
      overrideEval: Option[Int]
  ): Int =
    overrideEval.getOrElse {
      role match
        case PositionNodeRole.Before =>
          input.currentEvalCp
        case PositionNodeRole.AfterPlayed =>
          input.playedLine.map(_.line.scoreCp).getOrElse(input.currentEvalCp)
        case PositionNodeRole.AfterReference =>
          input.referenceLine.map(_.line.scoreCp).getOrElse(input.currentEvalCp)
        case PositionNodeRole.AfterAlternative =>
          input.lines.find(_.role == LineNodeRole.Alternative).map(_.line.scoreCp).getOrElse(input.currentEvalCp)
        case PositionNodeRole.AfterThreat =>
          input.currentEvalCp
    }

object CandidateLineAssembler:

  def fromLine(
      line: NormalizedCandidateLine,
      root: PositionNodeRef,
      allocator: JudgmentProvenanceAllocator
  ): CandidateLineAssembly =
    val scope = scopeFor(line.role)
    val ref = allocator.lineRef(line)
    val validatedFacts = replayFacts(line, root)
    val forcedTheme =
      ForcedLineTruth
        .detect(root.fen, ref.rootMove, List(line.line))
        .map(LineFactNormalizer.fromForcedTheme)
    val lineEvidence =
      allocator.evidenceRef(
        suffix = s"line:${allocator.key(line.role)}:${line.rank}",
        producer = EvidenceProducer.LegalLineProducer,
        layer = EvidenceLayer.Line,
        position = root,
        line = Some(ref),
        scope = scope,
        confidence = if validatedFacts.isDefined then EvidenceConfidence.LegalReplayVerified else EvidenceConfidence.EngineBacked
      )
    val node =
      CandidateLineNodeBuilder.fromEngineLine(
        role = line.role,
        ref = ref,
        line = line.line,
        evalCp = line.line.scoreCp,
        mate = line.line.mate,
        depth = line.line.depth,
        evidence = lineEvidence
      )
    val lineRecord =
      validatedFacts
        .map { facts =>
          LineFactNormalizer.fromValidatedLine(
            id = lineEvidence.id,
            lineRef = ref,
            facts = facts,
            position = root,
            scope = scope,
            forcedTheme = forcedTheme
          )
        }
        .getOrElse {
          LineFactNormalizer.fromCandidateLine(
            id = lineEvidence.id,
            line = node,
            position = root,
            scope = scope,
            forcedTheme = forcedTheme
          )
        }
    val evalRecord =
      EvalFactNormalizer.fromCandidateLine(
        id = allocator.evidenceId(s"eval:${allocator.key(line.role)}:${line.rank}"),
        line = node,
        position = root,
        scope = scope,
        parents = List(lineRecord.ref)
      )
    CandidateLineAssembly(node, List(lineRecord, evalRecord))

  private def replayFacts(
      line: NormalizedCandidateLine,
      root: PositionNodeRef
  ): Option[PrincipalVariationEvidence.LineFacts] =
    val refs = scala.collection.mutable.ListBuffer.empty[PrincipalVariationEvidence.LineMoveRef]
    var currentFen = root.fen
    var currentPly = root.ply
    val iterator = line.line.moves.iterator
    var legal = true
    while iterator.hasNext && legal do
      val move = PrincipalVariationEvidence.normalizeUci(iterator.next())
      PrincipalVariationEvidence.legalFenAfter(currentFen, move) match
        case Some(afterFen) =>
          currentPly += 1
          refs += PrincipalVariationEvidence.LineMoveRef(currentPly, move, afterFen)
          currentFen = afterFen
        case None =>
          legal = false
    val replayed = refs.toList
    Option
      .when(legal && replayed.nonEmpty)(PrincipalVariationEvidence.LineVariationRef(replayed))
      .flatMap(PrincipalVariationEvidence.validatedLineFromStart(root.fen, _))
      .flatMap { validated =>
        validated.first.map { first =>
          PrincipalVariationEvidence.LineFacts(
            line = validated.line,
            first = first,
            reply = validated.reply,
            continuation = validated.continuation,
            continuationTail = validated.moves.drop(3).take(3)
          )
        }
      }

  private def scopeFor(role: LineNodeRole): EvidenceScope =
    role match
      case LineNodeRole.Played        => EvidenceScope.PlayedLine
      case LineNodeRole.BestReference => EvidenceScope.BestLine
      case LineNodeRole.Threat        => EvidenceScope.ThreatLine
      case LineNodeRole.Alternative   => EvidenceScope.CandidateLine

object TransitionEdgeAssembler:

  def fromMove(
      role: TransitionEdgeRole,
      from: PositionNode,
      moveUci: String,
      to: PositionNode,
      allocator: JudgmentProvenanceAllocator
  ): TransitionEdgeAssembly =
    val scope = scopeFor(role)
    val transitionEvidence =
      allocator.evidenceRef(
        suffix = s"transition:${allocator.key(role)}:${MoveReviewInputNormalizer.normalizeUci(moveUci)}",
        producer = EvidenceProducer.MoveTransitionProducer,
        layer = EvidenceLayer.MoveTransition,
        position = from.ref,
        line = None,
        scope = scope,
        confidence = EvidenceConfidence.LegalReplayVerified
      )
    val edge =
      MoveTransitionEdgeBuilder.fromMove(
        role = role,
        id = allocator.transitionId(role, moveUci),
        from = from.ref,
        moveUci = MoveReviewInputNormalizer.normalizeUci(moveUci),
        to = to.ref,
        changedFacts = changedFacts(from.facts, to.facts),
        planTransition = None,
        evidence = transitionEvidence
      )
    TransitionEdgeAssembly(edge, List(TransitionFactNormalizer.fromMoveTransition(edge)))

  private def changedFacts(before: List[Fact], after: List[Fact]): List[Fact] =
    after.filterNot(before.contains)

  private def scopeFor(role: TransitionEdgeRole): EvidenceScope =
    role match
      case TransitionEdgeRole.Played    => EvidenceScope.PlayedTransition
      case TransitionEdgeRole.Reference => EvidenceScope.ReferenceTransition
      case TransitionEdgeRole.Alternative | TransitionEdgeRole.Threat =>
        EvidenceScope.AlternativeTransition

object NodeLineTransitionAssembler:

  def assemble(raw: RawMoveReviewInput): Option[NodeLineTransitionAssembly] =
    MoveReviewInputNormalizer.normalize(raw).flatMap { input =>
      val allocator = JudgmentProvenanceAllocator.forInput(input)
      for
        before <- PositionNodeAssembler.fromFen(
          input = input,
          role = PositionNodeRole.Before,
          fen = input.beforeFen,
          ply = input.beforePly,
          allocator = allocator,
          scope = EvidenceScope.BeforePosition,
          includeAssessment = true
        )
        afterPlayed <- PositionNodeAssembler.fromFen(
          input = input,
          role = PositionNodeRole.AfterPlayed,
          fen = input.afterPlayedFen,
          ply = input.beforePly + 1,
          allocator = allocator,
          scope = EvidenceScope.AfterPlayedPosition,
          includeAssessment = true
        )
      yield
        val afterReference =
          input.afterReferenceFen.flatMap { fen =>
            PositionNodeAssembler.fromFen(
              input = input,
              role = PositionNodeRole.AfterReference,
              fen = fen,
              ply = input.beforePly + 1,
              allocator = allocator,
              scope = EvidenceScope.AfterReferencePosition,
              includeAssessment = true
            )
          }
        val afterAlternatives =
          input.lines.filter(_.role == LineNodeRole.Alternative).flatMap { line =>
            line.rootMove
              .flatMap(PrincipalVariationEvidence.legalFenAfter(input.beforeFen, _))
              .flatMap { fen =>
                PositionNodeAssembler.fromFen(
                  input = input,
                  role = PositionNodeRole.AfterAlternative,
                  fen = fen,
                  ply = input.beforePly + 1,
                  allocator = allocator,
                  scope = EvidenceScope.AlternativeTransition,
                  includeAssessment = true,
                  assessmentSourceLines = Some(List(line.line.copy(moves = line.line.moves.drop(1)))),
                  assessmentEvalCp = Some(line.line.scoreCp)
                ).map(line -> _)
              }
          }
        val lines = input.lines.map(CandidateLineAssembler.fromLine(_, before.node.ref, allocator))
        val playedTransition =
          TransitionEdgeAssembler.fromMove(
            role = TransitionEdgeRole.Played,
            from = before.node,
            moveUci = input.playedMoveUci,
            to = afterPlayed.node,
            allocator = allocator
          )
        val referenceTransition =
          for
            referencePosition <- afterReference
            referenceMove <- input.referenceLine.flatMap(_.rootMove)
          yield
            TransitionEdgeAssembler.fromMove(
              role = TransitionEdgeRole.Reference,
              from = before.node,
              moveUci = referenceMove,
              to = referencePosition.node,
              allocator = allocator
            )
        val alternativeTransitions =
          afterAlternatives.flatMap { case (line, position) =>
            line.rootMove.map { move =>
              TransitionEdgeAssembler.fromMove(
                role = TransitionEdgeRole.Alternative,
                from = before.node,
                moveUci = move,
                to = position.node,
                allocator = allocator
              )
            }
          }
        val context =
          (List(before, afterPlayed) ++ afterReference.toList ++ afterAlternatives.map(_._2))
            .foldLeft(JudgmentAssemblyContext.empty()) { (ctx, assembly) =>
              ctx.withPosition(assembly.node).withEvidence(assembly.evidence)
            }
        val withLines =
          lines.foldLeft(context) { (ctx, assembly) =>
            ctx.withLine(assembly.node).withEvidence(assembly.evidence)
          }
        val withTransitions =
          (List(playedTransition) ++ referenceTransition.toList ++ alternativeTransitions).foldLeft(withLines) { (ctx, assembly) =>
            ctx.withTransition(assembly.edge).withEvidence(assembly.evidence)
          }
        NodeLineTransitionAssembly(input, withTransitions)
    }

object EvidenceGraphAssembler:

  def fromRecords(records: List[EvidenceRecord]): TypedEvidenceGraph =
    records.foldLeft(TypedEvidenceGraph.empty)((graph, record) => graph.add(record))

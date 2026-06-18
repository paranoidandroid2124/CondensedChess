package lila.chessjudgment.analysis.assembly

import chess.Color
import chess.format.Fen
import chess.variant.Standard
import lila.chessjudgment.analysis.evaluation.EvaluationPerspectivePolicy
import lila.chessjudgment.analysis.move.{ MoveAnalyzer, MoveMotifNormalizer }
import lila.chessjudgment.analysis.singlePosition.{ PawnPlayAssessor, ThreatPressureAssessor }
import lila.chessjudgment.analysis.strategic.StrategicFactNormalizer
import lila.chessjudgment.analysis.structure.{ PawnStructureAssessor, StructuralDeltaAnalyzer }
import lila.chessjudgment.analysis.tactical.{ RelationFactNormalizer, TacticalRelationEvidence }
import lila.chessjudgment.analysis.transition.TransitionFactNormalizer
import lila.chessjudgment.model.Motif
import lila.chessjudgment.model.judgment.*

final case class EvidenceFactAssembly(
    input: NormalizedMoveReviewInput,
    context: JudgmentAssemblyContext
)

object EvidenceFactAssembler:

  def assemble(raw: RawMoveReviewInput): Option[EvidenceFactAssembly] =
    NodeLineTransitionAssembler.assemble(raw).map(enrich)

  def enrich(assembly: NodeLineTransitionAssembly): EvidenceFactAssembly =
    val allocator = JudgmentProvenanceAllocator.forInput(assembly.input)
    val context = assembly.context
    val records =
      List.concat(
        moveMotifRecords(assembly.input, context, allocator),
        relationRecords(assembly.input, context, allocator),
        pawnStructureRecords(assembly.input, context, allocator),
        threatPressureRecords(assembly.input, context, allocator),
        structuralDeltaRecords(context, allocator)
      )
    EvidenceFactAssembly(assembly.input, context.withEvidence(records))

  private def moveMotifRecords(
      input: NormalizedMoveReviewInput,
      context: JudgmentAssemblyContext,
      allocator: JudgmentProvenanceAllocator
  ): List[EvidenceRecord] =
    val before = context.position(PositionNodeRole.Before).toList
    before.flatMap { root =>
      List(
        context.playedTransition.map { edge =>
          moveMotifRecord(
            id = allocator.evidenceId(s"move-motif:played:${edge.moveUci}"),
            input = input,
            root = root,
            moveUci = edge.moveUci,
            moves = List(edge.moveUci),
            line = context.line(LineNodeRole.Played),
            scope = EvidenceScope.PlayedTransition,
            parents = transitionParents(context, edge.role, context.line(LineNodeRole.Played))
          )
        },
        context.referenceTransition.map { edge =>
          moveMotifRecord(
            id = allocator.evidenceId(s"move-motif:reference:${edge.moveUci}"),
            input = input,
            root = root,
            moveUci = edge.moveUci,
            moves = List(edge.moveUci),
            line = context.line(LineNodeRole.BestReference),
            scope = EvidenceScope.ReferenceTransition,
            parents = transitionParents(context, edge.role, context.line(LineNodeRole.BestReference))
          )
        }
      ).flatten.flatten
    }

  private def moveMotifRecord(
      id: String,
      input: NormalizedMoveReviewInput,
      root: PositionNode,
      moveUci: String,
      moves: List[String],
      line: Option[CandidateLineNode],
      scope: EvidenceScope,
      parents: List[EvidenceRef]
  ): Option[EvidenceRecord] =
    val motifs = MoveAnalyzer.tokenizePv(input.beforeFen, moves).distinct
    Option.when(motifs.nonEmpty) {
      MoveMotifNormalizer.fromMotifs(
        id = id,
        moveUci = moveUci,
        motifs = motifs,
        position = root.ref,
        line = line.map(_.ref),
        scope = scope,
        parents = parents
      )
    }

  private def relationRecords(
      input: NormalizedMoveReviewInput,
      context: JudgmentAssemblyContext,
      allocator: JudgmentProvenanceAllocator
  ): List[EvidenceRecord] =
    val root = context.position(PositionNodeRole.Before).map(_.ref)
    root.toList.flatMap { rootRef =>
      val continuationLines = context.lines.map(_.line.moves)
      context.lines.flatMap { line =>
        TacticalRelationEvidence
          .boundedReplay(input.beforeFen, line.line.moves, maxPlies = 8)
          .toList
          .flatMap { replay =>
            val witnesses =
              TacticalRelationEvidence
                .relationWitnesses(
                  replay = replay,
                  playedMove = line.ref.rootMove,
                  explicitTargets = Nil,
                  continuationLines = continuationLines,
                  engineScoreCp = Some(line.evalCp),
                  engineMate = line.mate
                )
                .distinctBy(witness => (witness.kind, witness.focusSquares, witness.targetSquare, witness.lineMoves))
            witnesses.zipWithIndex.flatMap { case (witness, index) =>
              RelationFactNormalizer.fromWitness(
                id = allocator.evidenceId(s"relation:${allocator.key(line.role)}:${line.ref.rank}:$index:${witness.kind}"),
                witness = witness,
                position = rootRef,
                line = Some(line.ref),
                scope = scopeFor(line.role),
                confidence = EvidenceConfidence.LegalReplayVerified
              ).map { record =>
                record.copy(parents = lineParents(context, line))
              }
            }
          }
      }
    }

  private def pawnStructureRecords(
      input: NormalizedMoveReviewInput,
      context: JudgmentAssemblyContext,
      allocator: JudgmentProvenanceAllocator
  ): List[EvidenceRecord] =
    context.positions.flatMap { node =>
      for
        features <- node.features
        position <- Fen.read(Standard, Fen.Full(node.ref.fen))
      yield
        val side = node.ref.sideToMove.getOrElse(position.color)
        val profile = PawnStructureAssessor.assess(features, position.board, side)
        val pawnPlay =
          node.assessment.map { assessment =>
            PawnPlayAssessor.analyze(
              features = features,
              motifs = motifsForPrimaryLine(input),
              positionAssessment = assessment,
              sideToMove = features.sideToMove
            )
          }
        StrategicFactNormalizer.fromPawnStructure(
          id = allocator.evidenceId(s"pawn-structure:${allocator.key(node.role)}"),
          profile = profile,
          alignment = None,
          pawnPlay = pawnPlay,
          position = node.ref,
          scope = scopeFor(node.role),
          parents = evidenceRefs(context, EvidenceLayer.Board, Some(node.ref), None) ++
            evidenceRefs(context, EvidenceLayer.SinglePosition, Some(node.ref), None)
        )
    }

  private def threatPressureRecords(
      input: NormalizedMoveReviewInput,
      context: JudgmentAssemblyContext,
      allocator: JudgmentProvenanceAllocator
  ): List[EvidenceRecord] =
    context.position(PositionNodeRole.Before).toList.flatMap { node =>
      node.assessment.map { assessment =>
        val sideToMove = node.ref.sideToMove.getOrElse(input.sideToMove.getOrElse(Color.White))
        val threats =
          ThreatPressureAssessor.analyze(
            fen = input.beforeFen,
            motifs = motifsForPrimaryLine(input),
            multiPv = EvaluationPerspectivePolicy.sideToMovePvLines(sideToMove, input.lines.map(_.line)),
            positionAssessment = assessment,
            sideToMove = sideToMove.name
          )
        StrategicFactNormalizer.fromThreatPressure(
          id = allocator.evidenceId("threat-pressure:before"),
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
          createdTensionFrom = createdTensionFrom
        )
        if delta.hasConsequence
      yield
        TransitionFactNormalizer.fromStructuralDelta(
          id = allocator.evidenceId(s"structural-delta:${allocator.key(edge.role)}:${edge.moveUci}"),
          delta = delta,
          position = edge.from,
          line = lineForTransition(context, edge.role).map(_.ref),
          scope = scopeFor(edge.role),
          parents = transitionParents(context, edge.role, lineForTransition(context, edge.role)) ++
            evidenceRefs(context, EvidenceLayer.Board, Some(edge.to), None)
        )
    }

  private def motifsForPrimaryLine(input: NormalizedMoveReviewInput): List[Motif] =
    val moves =
      input.referenceLine
        .orElse(input.playedLine)
        .map(_.line.moves)
        .getOrElse(Nil)
    MoveAnalyzer.tokenizePv(input.beforeFen, moves).distinct

  private def moveStructureInputs(moveUci: String): (List[Char], List[String], Option[String]) =
    val normalized = MoveReviewInputNormalizer.normalizeUci(moveUci)
    val origin = normalized.take(2)
    val target = normalized.drop(2).take(2)
    val files = List(origin.headOption, target.headOption).flatten.filter(file => file >= 'a' && file <= 'h').distinct
    val targets = Option.when(target.matches("[a-h][1-8]"))(target).toList
    val createdTensionFrom = Option.when(origin.matches("[a-h][1-8]"))(origin)
    (files, targets, createdTensionFrom)

  private def transitionParents(
      context: JudgmentAssemblyContext,
      role: TransitionEdgeRole,
      line: Option[CandidateLineNode]
  ): List[EvidenceRef] =
    context.transition(role).toList.map(_.evidence) ++
      line.toList.flatMap(lineParents(context, _)) ++
      context.transition(role).toList.flatMap(edge => evidenceRefs(context, EvidenceLayer.Board, Some(edge.from), None))

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
      role: TransitionEdgeRole
  ): Option[CandidateLineNode] =
    role match
      case TransitionEdgeRole.Played    => context.line(LineNodeRole.Played)
      case TransitionEdgeRole.Reference => context.line(LineNodeRole.BestReference)
      case TransitionEdgeRole.Alternative | TransitionEdgeRole.Threat => context.line(LineNodeRole.Alternative)

  private def scopeFor(role: PositionNodeRole): EvidenceScope =
    role match
      case PositionNodeRole.Before           => EvidenceScope.BeforePosition
      case PositionNodeRole.AfterPlayed      => EvidenceScope.AfterPlayedPosition
      case PositionNodeRole.AfterReference   => EvidenceScope.AfterReferencePosition
      case PositionNodeRole.AfterAlternative => EvidenceScope.AlternativeTransition
      case PositionNodeRole.AfterThreat      => EvidenceScope.ThreatLine

  private def scopeFor(role: LineNodeRole): EvidenceScope =
    role match
      case LineNodeRole.Played        => EvidenceScope.PlayedLine
      case LineNodeRole.BestReference => EvidenceScope.BestLine
      case LineNodeRole.Threat        => EvidenceScope.ThreatLine
      case LineNodeRole.Alternative   => EvidenceScope.CandidateLine

  private def scopeFor(role: TransitionEdgeRole): EvidenceScope =
    role match
      case TransitionEdgeRole.Played    => EvidenceScope.PlayedTransition
      case TransitionEdgeRole.Reference => EvidenceScope.ReferenceTransition
      case TransitionEdgeRole.Alternative | TransitionEdgeRole.Threat =>
        EvidenceScope.AlternativeTransition

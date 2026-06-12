package lila.commentary.analysis

import chess.Square
import chess.format.{ Fen, Uci }
import chess.variant.Standard
import lila.commentary.{ MoveReviewExplanation, MoveReviewRefs, StrategyPack }
import lila.commentary.model.*
import lila.commentary.model.strategic.VariationLine

private[commentary] object MoveReviewExplanationBuilder:

  private[commentary] final case class Result(
      explanation: MoveReviewExplanation,
      localFact: MoveReviewLocalFact.Admission
  )

  def build(
      ctx: NarrativeContext,
      refs: Option[MoveReviewRefs],
      truthContract: Option[DecisiveTruthContract] = None,
      strategyPack: Option[StrategyPack] = None,
      strictLocalFacts: Boolean = false
  ): Option[MoveReviewExplanation] =
    buildWithLocalFact(ctx, refs, truthContract, strategyPack, strictLocalFacts).map(_.explanation)

  private[commentary] def buildWithLocalFact(
      ctx: NarrativeContext,
      refs: Option[MoveReviewRefs],
      truthContract: Option[DecisiveTruthContract] = None,
      strategyPack: Option[StrategyPack] = None,
      strictLocalFacts: Boolean = false
  ): Option[Result] =
    for
      played <- current(ctx)
      standardLineFacts = MoveReviewPvLine.firstCoupled(ctx.fen, played.uci, refs)
      evidence = moveReviewEvidence(ctx, played, refs, strategyPack, truthContract, strictLocalFacts)
      lineFacts = standardLineFacts.orElse(drawResourceLineFacts(ctx.fen, played, refs, evidence))
      descriptor <- CommentaryIdeaSurface.describe(played, evidence, lineFacts, truthContract, strictLocalFacts)
      pvInterpretation = descriptor.pvInterpretation(lineFacts)
      shortLine = MoveReviewPvLine.shortLine(refs, pvInterpretation.flatMap(_.supportedByLineId))
    yield
      Result(
        explanation =
          MoveReviewExplanation(
            title = descriptor.title,
            prose = descriptor.prose,
            qualityLabel = qualityLabel(truthContract),
            reasonTags = descriptor.reasonTags,
            shortLine = shortLine,
            pvInterpretation = pvInterpretation,
            source = descriptor.source,
            factFragments = Option.when(descriptor.factFragments.nonEmpty)(descriptor.factFragments)
          ),
        localFact = descriptor.localFact
      )

  def current(ctx: NarrativeContext): Option[CommentaryIdeaSurface.PlayedMove] =
    for
      uci <- ctx.playedMove.map(MoveReviewPvLine.normalizeUci)
      parsed <- Uci(uci).collect { case move: Uci.Move => move }
      san <- ctx.playedSan.map(_.trim).filter(_.nonEmpty)
      before <- Fen.read(Standard, Fen.Full(ctx.fen))
      move <- before.move(parsed).toOption
      after = move.after
      afterFen = Fen.write(after).value
      movedPiece <- after.board.pieceAt(parsed.dest)
      if movedPiece.color == move.piece.color
    yield
      CommentaryIdeaSurface.PlayedMove(
        uci = uci,
        san = san,
        from = parsed.orig,
        to = parsed.dest,
        piece = move.piece,
        afterFen = afterFen,
        capturedRole = before.board.pieceAt(parsed.dest).filter(_.color != move.piece.color).map(_.role)
      )

  private def moveReviewEvidence(
      ctx: NarrativeContext,
      played: CommentaryIdeaSurface.PlayedMove,
      refs: Option[MoveReviewRefs],
      strategyPack: Option[StrategyPack],
      truthContract: Option[DecisiveTruthContract],
      strictLocalFacts: Boolean
  ): CommentaryIdeaSurface.MoveReviewEvidence =
    val playedCandidate =
      ctx.candidates.find(candidate =>
        candidate.uci.exists(uci => MoveReviewPvLine.normalizeUci(uci) == played.uci) ||
          sanitizeSan(candidate.move) == sanitizeSan(played.san)
      )
    val facts =
      (playedCandidate.toList.flatMap(_.facts) ++ ctx.facts ++ ctx.mainPvFacts ++ ctx.threatLineFacts ++ ctx.counterfactualFacts)
        .distinct
    val motifs =
      playedCandidate.toList.flatMap(_.lineMotifs).distinct
    val openingGoal =
      ctx.openingGoalEvaluation
        .filter(goal => goal.status == OpeningGoals.Status.Achieved || goal.status == OpeningGoals.Status.Partial)
    val lineConsequence =
      lineConsequenceCandidate(ctx, refs, truthContract)
        .filter(_.kind != LineConsequenceKind.PreviewOnly)
        .filter(_.uciMoves.headOption.exists(uci => MoveReviewPvLine.normalizeUci(uci) == played.uci))
    val forcedLineTheme =
      ForcedLineTruth.detect(
        fen = ctx.fen,
        playedUci = played.uci,
        ply = ctx.ply,
        variations = forcedLineTruthVariations(ctx, refs)
      )
    val surface = StrategyPackSurface.from(strategyPack)
    val practicalPositionFacts =
      if truthContract.exists(_.blocksStrategicSupport) then Nil
      else CommentaryIdeaSurface.practicalPositionFacts(played, surface)
    CommentaryIdeaSurface.MoveReviewEvidence(
      facts = facts,
      motifs = motifs,
      openingGoal = openingGoal,
      openingName = ctx.openingData.flatMap(_.name).orElse(openingNameFromEvent(ctx)),
      lineConsequence = lineConsequence,
      postMoveTargetFacts = postMoveTargetFacts(played),
      relationWitnesses = relationWitnesses(ctx, played, facts, motifs, refs),
      strategicDeltas = PlayerFacingTruthModePolicy.mainPathMoveDeltaEvidences(ctx, surface, truthContract),
      phase = Option(ctx.phase.current).filter(_.trim.nonEmpty).getOrElse(ctx.header.phase),
      ply = ctx.ply,
      strictLocalFacts = strictLocalFacts,
      forcedLineTheme = forcedLineTheme,
      practicalPositionFacts = practicalPositionFacts
    )

  private def qualityLabel(truthContract: Option[DecisiveTruthContract]): Option[String] =
    truthContract.map(_.truthClass.toString)

  private def drawResourceLineFacts(
      startFen: String,
      played: CommentaryIdeaSurface.PlayedMove,
      refs: Option[MoveReviewRefs],
      evidence: CommentaryIdeaSurface.MoveReviewEvidence
  ): Option[MoveReviewPvLine.LineFacts] =
    Option
      .when(evidence.relationWitnesses.exists(drawResourceWitnessForPlayedMove(_, played)))(
        MoveReviewPvLine.playedCoupled(startFen, played.uci, refs)
      )
      .flatten

  private def drawResourceWitnessForPlayedMove(
      witness: MoveReviewExchangeAnalyzer.RelationWitness,
      played: CommentaryIdeaSurface.PlayedMove
  ): Boolean =
    (
      witness.kind == MoveReviewExchangeAnalyzer.RelationKind.StalemateTrap ||
        witness.kind == MoveReviewExchangeAnalyzer.RelationKind.PerpetualCheck
    ) &&
      witness.lineMoves.headOption.exists(uci => MoveReviewPvLine.normalizeUci(uci) == played.uci)

  private def openingNameFromEvent(ctx: NarrativeContext): Option[String] =
    ctx.openingEvent.collect { case event: OpeningEvent.Intro => event.name }

  private def lineConsequenceCandidate(
      ctx: NarrativeContext,
      refs: Option[MoveReviewRefs],
      truthContract: Option[DecisiveTruthContract]
  ): Option[LineConsequenceEvidence] =
    if truthContract.exists(_.blocksStrategicSupport) then
      LineConsequenceEvaluator.reviewedMoveSurfaceCandidate(ctx, refs)
        .orElse(LineConsequenceEvaluator.surfaceCandidate(ctx, refs))
    else LineConsequenceEvaluator.surfaceCandidate(ctx, refs)

  private def forcedLineTruthVariations(ctx: NarrativeContext, refs: Option[MoveReviewRefs]): List[VariationLine] =
    (
      ctx.engineEvidence.toList.flatMap(_.variations) ++
        refs.toList.flatMap(_.variations).map(ref =>
          VariationLine(
            moves = ref.moves.map(move => MoveReviewPvLine.normalizeUci(move.uci)).filter(_.nonEmpty),
            scoreCp = ref.scoreCp,
            mate = ref.mate,
            depth = ref.depth
          )
        )
    ).filter(_.moves.nonEmpty).distinct

  private def postMoveTargetFacts(played: CommentaryIdeaSurface.PlayedMove): List[Fact] =
    Fen.read(Standard, Fen.Full(played.afterFen))
      .map(position => FactExtractor.extractStaticFacts(position.board, !played.color))
      .getOrElse(Nil)
      .collect {
        case fact: Fact.TargetPiece if fact.attackers.contains(played.to) => fact
        case fact: Fact.HangingPiece if fact.attackers.contains(played.to) => fact
      }
      .distinct

  private def relationWitnesses(
      ctx: NarrativeContext,
      played: CommentaryIdeaSurface.PlayedMove,
      facts: List[Fact],
      motifs: List[Motif],
      refs: Option[MoveReviewRefs]
  ): List[MoveReviewExchangeAnalyzer.RelationWitness] =
    val variations = ctx.engineEvidence.toList.flatMap(_.variations)
    val refValidatedLines =
      refs.toList
        .flatMap(_.variations)
        .flatMap(variation => MoveReviewPvLine.validatedLine(ctx.fen, variation, played.uci))
    val continuationLines =
      (
        variations.map(MoveReviewExchangeAnalyzer.normalizedLineMoves) ++
          refValidatedLines.map(_.moves.map(move => MoveReviewPvLine.normalizeUci(move.uci)))
      ).filter(_.nonEmpty).distinct
    val explicitTargets = explicitTargetSquares(ctx)
    val tacticalTargets = tacticalRelationTargetSquares(played, facts, motifs)
    val targetSets =
      (List(explicitTargets) ++ Option.when(tacticalTargets.nonEmpty)(tacticalTargets).toList)
        .map(_.distinct)
        .distinct
    def witnessesFrom(
        replays: List[List[MoveReviewExchangeAnalyzer.BoundedReplayStep]],
        replayTargetSets: List[List[String]]
    ): List[MoveReviewExchangeAnalyzer.RelationWitness] =
      replays.flatMap(replay =>
        replayTargetSets.flatMap(targets =>
          MoveReviewExchangeAnalyzer.relationWitnesses(
            replay = replay,
            playedMove = played.uci,
            explicitTargets = targets,
            continuationLines = continuationLines,
            includeDrawResources = false
          )
        )
      )
    val engineReplays =
      MoveReviewExchangeAnalyzer
        .boundedTopReplay(ctx.fen, variations, maxPlies = 6)
        .filter(replay => replay.headOption.exists(step => MoveReviewPvLine.normalizeUci(step.uci) == played.uci))
        .toList
    val refReplays =
      refValidatedLines.flatMap(validated =>
        MoveReviewExchangeAnalyzer.boundedReplay(
          ctx.fen,
          validated.moves.map(_.uci),
          maxPlies = 6
        )
      )
    (
      witnessesFrom(engineReplays.distinct, targetSets) ++
        witnessesFrom(refReplays.distinct, targetSets.filter(_.nonEmpty)) ++
        drawResourceRelationWitnesses(ctx, played, refs)
    )
      .filter(MoveReviewExchangeAnalyzer.relationDetailsValidForKind)
      .distinct

  private def drawResourceRelationWitnesses(
      ctx: NarrativeContext,
      played: CommentaryIdeaSurface.PlayedMove,
      refs: Option[MoveReviewRefs]
  ): List[MoveReviewExchangeAnalyzer.RelationWitness] =
    val variations = ctx.engineEvidence.toList.flatMap(_.variations)
    val engineWitnesses =
      ctx.engineEvidence.flatMap(_.best).toList.flatMap { topLine =>
        MoveReviewExchangeAnalyzer
          .boundedTopReplayPrefix(
            ctx.fen,
            variations,
            minPlies = 1,
            maxPlies = MoveReviewExchangeAnalyzer.DrawResourceRelationReplayMaxPlies
          )
          .filter(replay => replay.headOption.exists(step => MoveReviewPvLine.normalizeUci(step.uci) == played.uci))
          .toList
          .flatMap(replay =>
            drawResourceWitnessesFromReplay(
              replay = replay,
              played = played,
              engineScoreCp = Some(topLine.scoreCp),
              engineMate = topLine.mate
            )
          )
      }
    val refWitnesses =
      refs.toList.flatMap(_.variations).flatMap { variation =>
        MoveReviewPvLine.validatedLine(ctx.fen, variation, played.uci).toList.flatMap { validated =>
          MoveReviewExchangeAnalyzer
            .boundedReplayPrefix(
              ctx.fen,
              validated.moves.map(move => MoveReviewPvLine.normalizeUci(move.uci)),
              minPlies = 1,
              maxPlies = MoveReviewExchangeAnalyzer.DrawResourceRelationReplayMaxPlies
            )
            .toList
            .flatMap(replay =>
              drawResourceWitnessesFromReplay(
                replay = replay,
                played = played,
                engineScoreCp = Some(variation.scoreCp),
                engineMate = variation.mate
              )
            )
        }
      }
    val probeWitnesses =
      ctx.validatedRootProbeResults
        .filter(drawResourceProbeMatchesPlayed(ctx.fen, _, played.uci))
        .flatMap(result =>
          drawResourceProbeReplyLines(result).flatMap(replyLine =>
            MoveReviewExchangeAnalyzer
              .boundedReplayPrefix(
                ctx.fen,
                played.uci :: replyLine,
                minPlies = 1,
                maxPlies = MoveReviewExchangeAnalyzer.DrawResourceRelationReplayMaxPlies
              )
              .toList
              .flatMap(replay =>
                drawResourceWitnessesFromReplay(
                  replay = replay,
                  played = played,
                  engineScoreCp = Some(result.evalCp),
                  engineMate = result.mate
                )
              )
          )
        )
    (engineWitnesses ++ refWitnesses ++ probeWitnesses).distinct

  private def drawResourceWitnessesFromReplay(
      replay: List[MoveReviewExchangeAnalyzer.BoundedReplayStep],
      played: CommentaryIdeaSurface.PlayedMove,
      engineScoreCp: Option[Int],
      engineMate: Option[Int]
  ): List[MoveReviewExchangeAnalyzer.RelationWitness] =
    List(
      MoveReviewExchangeAnalyzer.stalemateTrapWitness(replay, played.uci, engineScoreCp, engineMate),
      MoveReviewExchangeAnalyzer.perpetualCheckWitness(replay, played.uci, engineScoreCp, engineMate)
    ).flatten

  private def drawResourceProbeMatchesPlayed(
      fen: String,
      result: ProbeResult,
      playedUci: String
  ): Boolean =
    ProbeContractValidator.validate(result).isValid &&
      result.fen.map(_.trim).filter(_.nonEmpty).contains(fen.trim) &&
      {
        val normalizedBoundMoves =
          (result.probedMove.toList ++ result.candidateMove.toList)
            .map(MoveReviewPvLine.normalizeUci)
            .filter(_.nonEmpty)
        normalizedBoundMoves.nonEmpty &&
          normalizedBoundMoves.forall(_ == playedUci) &&
          normalizedBoundMoves.forall(MoveReviewExchangeAnalyzer.isUciMove)
      }

  private def drawResourceProbeReplyLines(result: ProbeResult): List[List[String]] =
    (result.bestReplyPv :: result.replyPvs.toList.flatten)
      .filter(_.nonEmpty)
      .flatMap(drawResourceStrictUciLine)
      .distinct

  private def drawResourceStrictUciLine(moves: List[String]): Option[List[String]] =
    val normalized = moves.map(move => Option(move).fold("")(MoveReviewPvLine.normalizeUci))
    Option.when(normalized.nonEmpty && normalized.forall(MoveReviewExchangeAnalyzer.isUciMove))(normalized)

  private def explicitTargetSquares(ctx: NarrativeContext): List[String] =
    ctx.decision.toList
      .flatMap(_.focalPoint.collect { case TargetSquare(key) => key })
      .map(_.trim.toLowerCase)
      .filter(MoveReviewPlayerPayloadBuilder.ChessSquarePattern.matches)
      .distinct

  private def tacticalRelationTargetSquares(
      played: CommentaryIdeaSurface.PlayedMove,
      facts: List[Fact],
      motifs: List[Motif]
  ): List[String] =
    (
      facts.flatMap(tacticalFactTargetSquares(played, _)) ++
        motifs.filter(currentMoveMotif(played, _)).flatMap(tacticalMotifTargetSquares)
    ).map(_.key).filter(MoveReviewPlayerPayloadBuilder.ChessSquarePattern.matches).distinct

  private def tacticalFactTargetSquares(
      played: CommentaryIdeaSurface.PlayedMove,
      fact: Fact
  ): List[Square] =
    fact match
      case fact: Fact.Fork if fact.attacker == played.to => fact.targets.map(_._1)
      case fact: Fact.Pin if fact.attacker == played.to => List(fact.pinned, fact.behind)
      case fact: Fact.Skewer if fact.attacker == played.to => List(fact.front, fact.back)
      case fact: Fact.HangingPiece if fact.attackers.contains(played.to) => List(fact.square)
      case fact: Fact.TargetPiece if fact.attackers.contains(played.to) => List(fact.square)
      case _                       => Nil

  private def tacticalMotifTargetSquares(motif: Motif): List[Square] =
    motif match
      case motif: Motif.Fork             => motif.targetSquares
      case motif: Motif.Pin              => List(motif.pinnedSq, motif.behindSq).flatten
      case motif: Motif.Skewer           => List(motif.frontSq, motif.backSq).flatten
      case motif: Motif.DiscoveredAttack => motif.targetSq.toList
      case motif: Motif.Zwischenzug      => List(motif.expectedRecaptureSquare)
      case motif: Motif.Overloading      => motif.duties
      case motif: Motif.TrappedPiece     => List(motif.trappedSquare)
      case motif: Motif.Domination       => List(motif.square)
      case _                             => Nil

  private def currentMoveMotif(
      played: CommentaryIdeaSurface.PlayedMove,
      motif: Motif
  ): Boolean =
    motif.plyIndex == 0 &&
      motif.color == played.color &&
      motif.move.exists(move => sanitizeSan(move) == sanitizeSan(played.san))

  private def sanitizeSan(san: String): String =
    Option(san).getOrElse("").trim.replaceAll("""[+#?!]+$""", "")

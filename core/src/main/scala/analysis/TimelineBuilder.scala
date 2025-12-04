package chess
package analysis

import AnalyzeDomain.*
import AnalyzeUtils.*
import chess.format.Fen
import chess.opening.Opening

object TimelineBuilder:

  def buildTimeline(
      replay: Replay, 
      client: StockfishClient, 
      config: EngineConfig, 
      opening: Option[Opening.AtPly],
      playerContext: Option[PlayerContext] = None,
      jobId: Option[String] = None
  ): (Vector[PlyOutput], Game) =
    var game = replay.setup
    val entries = Vector.newBuilder[PlyOutput]
    var prevDeltaForOpp: Double = 0.0
    val bookExitPly: Option[Int] = opening.map(_.ply.value + 1)
    val totalMoves = replay.chronoMoves.size
    
    replay.chronoMoves.zipWithIndex.foreach { case (mod, idx) =>
      jobId.foreach(id => AnalysisProgressTracker.update(id, AnalysisStage.ENGINE_EVALUATION, idx.toDouble / totalMoves))
      val player = game.position.color
      val legalCount = game.position.legalMoves.size
      val multiPv = math.min(config.maxMultiPv, math.max(1, legalCount))
      val fenBefore = Fen.write(game.position, game.ply.fullMoveNumber).value
      val evalBeforeShallow = EngineProbe.evalFen(client, fenBefore, config.shallowDepth, multiPv, Some(config.shallowTimeMs))
      val evalBeforeDeep = EngineProbe.evalFen(client, fenBefore, config.deepDepth, multiPv, Some(config.deepTimeMs))
      val winBefore = evalBeforeDeep.lines.headOption.map(_.winPct).getOrElse(50.0)

      val nextGame = mod.applyGame(game)
      val fenAfter = Fen.write(nextGame.position, nextGame.ply.fullMoveNumber).value
      val statusAfter = nextGame.position.status
      val winnerAfter = nextGame.position.winner
      val (winAfterForPlayer, _) =
        statusAfter match
          case Some(Status.Mate) =>
            val sideToMoveWin = if winnerAfter.contains(nextGame.position.color) then 100.0 else 0.0
            val moverWin = if winnerAfter.contains(player) then 100.0 else 0.0
            (moverWin, sideToMoveWin)
          case Some(Status.Stalemate | Status.Draw | Status.VariantEnd) =>
            (50.0, 50.0)
          case _ =>
            val evalAfter = EngineProbe.evalFen(client, fenAfter, config.deepDepth, 1, Some(config.deepTimeMs))
            val side = evalAfter.lines.headOption.map(_.winPct).getOrElse(50.0)
            val mover = if player == nextGame.position.color then side else 100.0 - side
            (mover, side)
      val delta = winAfterForPlayer - winBefore
      val epBefore = clamp01(winBefore / 100.0)
      val epAfter = clamp01(winAfterForPlayer / 100.0)
      val epLoss = math.max(0.0, epBefore - epAfter)

      val baseJudgement =
        statusAfter match
          case Some(Status.Mate) => "mate"
          case Some(Status.Stalemate | Status.Draw | Status.VariantEnd) => "draw"
          case _ =>
            if epLoss <= 0.0 then "best"
            else if epLoss <= 0.05 then "good" // excellent 통합
            else if epLoss <= 0.10 then "inaccuracy"
            else if epLoss <= 0.20 then "mistake"
            else "blunder"

      val materialBefore = material(game.position.board, player)
      val materialAfter = material(nextGame.position.board, player)
      val sacrificed = materialAfter < materialBefore - 1.5 // 말 하나 이상 희생

      val specialBrilliant =
        sacrificed &&
          (baseJudgement == "best" || baseJudgement == "excellent") &&
          epLoss <= 0.02 &&
          epBefore <= 0.7 && epBefore >= 0.3

      val greatMove =
        !specialBrilliant && (baseJudgement == "best" || baseJudgement == "excellent" || baseJudgement == "good") && (
          (epBefore < 0.35 && epAfter >= 0.55) || // 패배권 → 승리권
            (epAfter - epBefore) >= 0.2 // 큰 전환
        )

      val miss =
        (baseJudgement == "inaccuracy" || baseJudgement == "mistake" || baseJudgement == "blunder") &&
          prevDeltaForOpp <= -20 // 직전에 상대가 큰 실수(내 입장에서는 큰 득점)를 했는데 놓침

      val special =
        if specialBrilliant then Some("brilliant")
        else if greatMove then Some("great")
        else None

      val inBook = opening.exists(op => nextGame.ply.value <= op.ply.value)
      val finalJudgement =
        if inBook then "book"
        else baseJudgement

      val moverFeaturesBefore = FeatureExtractor.sideFeatures(game.position, player)
      val oppFeaturesBefore = FeatureExtractor.sideFeatures(game.position, !player)
      val moverFeatures = FeatureExtractor.sideFeatures(nextGame.position, player)
      val oppFeatures = FeatureExtractor.sideFeatures(nextGame.position, !player)
      ConceptScorer.computeBreakAndInfiltration(nextGame.position, nextGame.position.color)
      val conceptScoresBefore = ConceptScorer.score(
        features = moverFeaturesBefore,
        oppFeatures = oppFeaturesBefore,
        evalShallowWin = evalBeforeShallow.lines.headOption.map(_.winPct).getOrElse(winBefore),
        evalDeepWin = evalBeforeDeep.lines.headOption.map(_.winPct).getOrElse(winBefore),
        multiPvWin = evalBeforeDeep.lines.map(_.winPct),
        position = game.position,
        sideToMove = game.position.color
      )
      val conceptScores = ConceptScorer.score(
        features = moverFeatures,
        oppFeatures = oppFeatures,
        evalShallowWin = evalBeforeShallow.lines.headOption.map(_.winPct).getOrElse(winBefore),
        evalDeepWin = evalBeforeDeep.lines.headOption.map(_.winPct).getOrElse(winBefore),
        multiPvWin = evalBeforeDeep.lines.map(_.winPct),
        position = nextGame.position,
        sideToMove = nextGame.position.color
      )
      val concepts = Concepts(
        dynamic = conceptScores.dynamic,
        drawish = conceptScores.drawish,
        imbalanced = conceptScores.imbalanced,
        tacticalDepth = conceptScores.tacticalDepth,
        blunderRisk = conceptScores.blunderRisk,
        pawnStorm = conceptScores.pawnStorm,
        fortress = conceptScores.fortress,
        colorComplex = conceptScores.colorComplex,
        badBishop = conceptScores.badBishop,
        goodKnight = conceptScores.goodKnight,
        rookActivity = conceptScores.rookActivity,
        kingSafety = conceptScores.kingSafety,
        dry = conceptScores.dry,
        comfortable = conceptScores.comfortable,
        unpleasant = conceptScores.unpleasant,
        engineLike = conceptScores.engineLike,
        conversionDifficulty = conceptScores.conversionDifficulty,
        sacrificeQuality = conceptScores.sacrificeQuality,
        alphaZeroStyle = conceptScores.alphaZeroStyle
      )
      val conceptsBefore = Concepts(
        dynamic = conceptScoresBefore.dynamic,
        drawish = conceptScoresBefore.drawish,
        imbalanced = conceptScoresBefore.imbalanced,
        tacticalDepth = conceptScoresBefore.tacticalDepth,
        blunderRisk = conceptScoresBefore.blunderRisk,
        pawnStorm = conceptScoresBefore.pawnStorm,
        fortress = conceptScoresBefore.fortress,
        colorComplex = conceptScoresBefore.colorComplex,
        badBishop = conceptScoresBefore.badBishop,
        goodKnight = conceptScoresBefore.goodKnight,
        rookActivity = conceptScoresBefore.rookActivity,
        kingSafety = conceptScoresBefore.kingSafety,
        dry = conceptScoresBefore.dry,
        comfortable = conceptScoresBefore.comfortable,
        unpleasant = conceptScoresBefore.unpleasant,
        engineLike = conceptScoresBefore.engineLike,
        conversionDifficulty = conceptScoresBefore.conversionDifficulty,
        sacrificeQuality = conceptScoresBefore.sacrificeQuality,
        alphaZeroStyle = conceptScoresBefore.alphaZeroStyle
      )
      val conceptDelta = Concepts(
        dynamic = concepts.dynamic - conceptsBefore.dynamic,
        drawish = concepts.drawish - conceptsBefore.drawish,
        imbalanced = concepts.imbalanced - conceptsBefore.imbalanced,
        tacticalDepth = concepts.tacticalDepth - conceptsBefore.tacticalDepth,
        blunderRisk = concepts.blunderRisk - conceptsBefore.blunderRisk,
        pawnStorm = concepts.pawnStorm - conceptsBefore.pawnStorm,
        fortress = concepts.fortress - conceptsBefore.fortress,
        colorComplex = concepts.colorComplex - conceptsBefore.colorComplex,
        badBishop = concepts.badBishop - conceptsBefore.badBishop,
        goodKnight = concepts.goodKnight - conceptsBefore.goodKnight,
        rookActivity = concepts.rookActivity - conceptsBefore.rookActivity,
        kingSafety = concepts.kingSafety - conceptsBefore.kingSafety,
        dry = concepts.dry - conceptsBefore.dry,
        comfortable = concepts.comfortable - conceptsBefore.comfortable,
        unpleasant = concepts.unpleasant - conceptsBefore.unpleasant,
        engineLike = concepts.engineLike - conceptsBefore.engineLike,
        conversionDifficulty = concepts.conversionDifficulty - conceptsBefore.conversionDifficulty,
        sacrificeQuality = concepts.sacrificeQuality - conceptsBefore.sacrificeQuality,
        alphaZeroStyle = concepts.alphaZeroStyle - conceptsBefore.alphaZeroStyle
      )
      val (phaseScore, phaseLabelRaw) = PhaseClassifier.phaseTransition(conceptDelta, winBefore)
      val phaseThreshold = if bookExitPly.contains(nextGame.ply.value) then 6.0 else 8.0
      val phaseLabel = phaseLabelRaw.filter(_ => phaseScore >= phaseThreshold)
      val bestVsSecondGap = (for
        top <- evalBeforeDeep.lines.headOption
        second <- evalBeforeDeep.lines.drop(1).headOption
      yield (top.winPct - second.winPct).abs)
      val bestVsPlayedGap = evalBeforeDeep.lines.headOption.map(_.winPct - winAfterForPlayer)

      val semanticTags =
        SemanticTagger.tags(
          position = nextGame.position,
          perspective = player,
          ply = nextGame.ply,
          self = moverFeatures,
          opp = oppFeatures,
          concepts = Some(conceptScores),
          winPct = winBefore
        )

      val baseMistakeCategory =
        MistakeClassifier.classify(
          MistakeClassifier.Input(
            ply = nextGame.ply,
            judgement = finalJudgement,
            deltaWinPct = delta,
            san = mod.toSanStr.value,
            player = player,
            inCheckBefore = game.position.check.yes,
            concepts = concepts,
            features = moverFeatures,
            oppFeatures = oppFeatures
          )
        )
      val mistakeCategory = if miss then Some("tactical_miss") else baseMistakeCategory

      val practicality = Some(
        PracticalityScorer.score(
          eval = evalBeforeDeep,
          tacticalDepth = concepts.tacticalDepth,
          sacrificeQuality = concepts.sacrificeQuality,
          tags = semanticTags,
          context = playerContext,
          turn = player
        )
      )

      entries += PlyOutput(
        ply = nextGame.ply,
        turn = player,
        san = mod.toSanStr.value,
        uci = mod.toUci.uci,
        fen = fenAfter,
        fenBefore = fenBefore,
        legalMoves = legalCount,
        features = moverFeatures,
        evalBeforeShallow = evalBeforeShallow,
        evalBeforeDeep = evalBeforeDeep,
        winPctBefore = winBefore,
        winPctAfterForPlayer = winAfterForPlayer,
        deltaWinPct = delta,
        epBefore = epBefore,
        epAfter = epAfter,
        epLoss = epLoss,
        judgement = finalJudgement,
        special = special,
        conceptsBefore = conceptsBefore,
        concepts = concepts,
        conceptDelta = conceptDelta,
        bestVsSecondGap = bestVsSecondGap,
        bestVsPlayedGap = bestVsPlayedGap,
        semanticTags = semanticTags,
        mistakeCategory = mistakeCategory,
        phaseLabel = phaseLabel,
        practicality = practicality
      )
      game = nextGame
      prevDeltaForOpp = delta
    }
    entries.result() -> game

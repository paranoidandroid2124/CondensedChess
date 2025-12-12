package chess
package analysis

import scala.concurrent.{Future, ExecutionContext}
import chess.format.Fen
import chess.opening.Opening
import chess.analysis.FeatureExtractor
import chess.analysis.MoveGenerator
import chess.analysis.ConceptLabeler
import chess.analysis.RoleLabeler
import chess.analysis.AnalysisTypes._
import chess.analysis.AnalysisModel._
import chess.analysis.{PhaseCalculator, PracticalityScorer, ConceptScorer, AnalysisContext}
import chess.MoveOrDrop

object TimelineBuilder:

  // Intermediate data structure
  private case class RawPlyData(
      plyIndex: Int,
      gameBefore: Game,
      gameAfter: Game,
      move: MoveOrDrop,
      evalBeforeShallow: EngineEval,
      evalBeforeDeep: EngineEval,
      evalAfterDeep: Option[EngineEval], // Opponent's perspective after move
      experiments: List[ExperimentResult],
      featuresBefore: FeatureExtractor.SideFeatures,
      featuresAfter: FeatureExtractor.SideFeatures, // Mover's features in new position (for concept scoring)
      oppFeaturesAfter: FeatureExtractor.SideFeatures,
      p4FeaturesBefore: FeatureExtractor.PositionFeatures,
      p4FeaturesAfter: FeatureExtractor.PositionFeatures
      // Note: we might need featuresBefore for ConceptScorer (moverFeaturesBefore)
      , moverFeaturesBefore: FeatureExtractor.SideFeatures,
      oppFeaturesBefore: FeatureExtractor.SideFeatures
  )

  /**
   * Builds the timeline asynchronously.
   * 
   * @param engineInterface Engine evaluation interface (Student role)
   * @param experimentRunner Experiment execution (Coach role)
   */
  def buildTimeline(
      replay: Replay, 
      engineInterface: EngineInterface,  // Changed from EngineService
      experimentRunner: ExperimentRunner,
      config: EngineConfig, 
      opening: Option[Opening.AtPly],
      playerContext: Option[PlayerContext] = None,
      onProgress: (AnalysisStage.AnalysisStage, Double) => Unit = (_, _) => ()
  )(using ec: ExecutionContext): Future[(Vector[PlyOutput], Game)] =
    
    val totalMoves = replay.chronoMoves.size
    
    // 1. Prepare Game States
    var currentGame = replay.setup
    val gameSequence = replay.chronoMoves.zipWithIndex.map { case (mod, idx) =>
      val gameBefore = currentGame
      val gameAfter = mod.applyGame(currentGame)
      currentGame = gameAfter
      (idx, mod, gameBefore, gameAfter)
    }
    val finalGame = currentGame

    // 2. Parallel Analysis Phase (with per-ply error isolation)
    // 2. Parallel Analysis Phase in Batches
    // We process in small batches to avoid overloading the engine pool queue
    // and to provide smoother progress feedback.
    val batchSize = 8
    val batches = gameSequence.grouped(batchSize).toList
    
    // Process batches sequentially, but items within a batch in parallel
    val futureRawData: Future[Vector[RawPlyData]] = batches.foldLeft(Future.successful(Vector.empty[RawPlyData])) { (accFuture, batch) =>
       accFuture.flatMap { acc =>
         Future.sequence(
           batch.map { case (idx, mod, gameBefore, gameAfter) =>
             processPlyParallel(idx, mod, gameBefore, gameAfter, engineInterface, experimentRunner, config, totalMoves, onProgress)
               .recover { case e: Throwable =>
                 // Log error but continue with fallback data
                 // This effectively isolates single ply failures
                 System.err.println(s"[TimelineBuilder] Ply $idx analysis failed: ${e.getMessage}")
                 fallbackRawPlyData(idx, mod, gameBefore, gameAfter)
               }
           }
         ).map(batchResults => acc ++ batchResults)
       }
    }

    // 3. Sequential Enrichment Phase
    futureRawData.map { rawData =>
      var prevDeltaForOpp: Double = 0.0
      val bookExitPly: Option[Int] = opening.map(_.ply.value + 1)
      
      val timeline = rawData.map { d =>
        val (output, delta) = enrichPly(d, prevDeltaForOpp, bookExitPly, opening, playerContext)
        prevDeltaForOpp = delta
        output
      }
      (timeline, finalGame)
    }

  private def processPlyParallel(
      idx: Int,
      mod: MoveOrDrop,
      gameBefore: Game,
      gameAfter: Game,
      engine: EngineInterface,  // Changed from EngineService
      experimentRunner: ExperimentRunner,
      config: EngineConfig,
      totalMoves: Int,
      onProgress: (AnalysisStage.AnalysisStage, Double) => Unit
  )(using ec: ExecutionContext): Future[RawPlyData] =
    val prog = (idx + 1).toDouble / totalMoves

    val player = gameBefore.position.color
    val legalCount = gameBefore.position.legalMoves.size
    val multiPv = math.min(config.maxMultiPv, math.max(1, legalCount))
    val fenBefore = Fen.write(gameBefore.position, gameBefore.ply.fullMoveNumber).value
    val fenAfter = Fen.write(gameAfter.position, gameAfter.ply.fullMoveNumber).value
    
    // Request 1 & 2: Before (using EngineInterface)
    val shallowEvalF = engine.evaluate(fenBefore, config.shallowDepth, multiPv, config.shallowTimeMs)
    val deepEvalF = engine.evaluate(fenBefore, config.deepDepth, multiPv, config.deepTimeMs)
    
    // Request 3: After (for accurate winPct if needed)
    // Only needed if game is not over
    val statusAfter = gameAfter.position.status
    val afterEvalF: Future[Option[EngineEval]] = statusAfter match
      case Some(_) => Future.successful(None)
      case None =>
        engine.evaluate(fenAfter, config.deepDepth, 1, config.deepTimeMs).map(Some(_))

    // Features
    val p4FeaturesBefore = FeatureExtractor.extractPositionFeatures(fenBefore, gameBefore.ply.value)
    val p4FeaturesAfter = FeatureExtractor.extractPositionFeatures(fenAfter, gameAfter.ply.value)
    
    val moverFeaturesBefore = FeatureExtractor.sideFeatures(gameBefore.position, player)
    val oppFeaturesBefore = FeatureExtractor.sideFeatures(gameBefore.position, !player)
    val moverFeaturesAfter = FeatureExtractor.sideFeatures(gameAfter.position, player)
    val oppFeaturesAfter = FeatureExtractor.sideFeatures(gameAfter.position, !player)

    // Candidates & Experiments
    val p4Candidates = MoveGenerator.generateCandidates(gameBefore.position, p4FeaturesBefore)
    val runExperimentsFuture = Future.sequence(
      p4Candidates.map { candidate =>
        val expType = candidate.candidateType match
          case MoveGenerator.CandidateType.TacticalCheck => ExperimentType.TacticalCheck
          case _ => ExperimentType.StructureAnalysis 
        experimentRunner.run(
           expType = expType,
           fen = fenBefore,
           move = Some(candidate.uci),
           depth = 10,
           multiPv = 1,
           forcedMoves = List(candidate.uci)
        ).map(res => res.copy(metadata = res.metadata + ("candidateType" -> candidate.candidateType.toString)))
      }
    )

    for
      shallowEval <- shallowEvalF
      deepEval <- deepEvalF
      afterEvalOpt <- afterEvalF
      experiments <- runExperimentsFuture
    yield
      if (idx % 5 == 0 || idx == totalMoves - 1) 
         onProgress(AnalysisStage.ENGINE_EVALUATION, prog)
 
      RawPlyData(
        plyIndex = idx,
        gameBefore = gameBefore,
        gameAfter = gameAfter,
        move = mod,
        evalBeforeShallow = shallowEval,
        evalBeforeDeep = deepEval,
        evalAfterDeep = afterEvalOpt,
        experiments = experiments,
        featuresBefore = moverFeaturesBefore, // Wait, features field in output is usually 'moverFeaturesAfter' (current state) or 'moverFeaturesBefore'? 
        // Original code: features = moverFeatures (which is nextGame/Around).
        // Let's store all.
        moverFeaturesBefore = moverFeaturesBefore,
        oppFeaturesBefore = oppFeaturesBefore,
        featuresAfter = moverFeaturesAfter,
        oppFeaturesAfter = oppFeaturesAfter,
        p4FeaturesBefore = p4FeaturesBefore,
        p4FeaturesAfter = p4FeaturesAfter
      )

  private def enrichPly(
      d: RawPlyData, 
      prevDeltaForOpp: Double,
      _bookExitPly: Option[Int],
      opening: Option[Opening.AtPly],
      playerContext: Option[PlayerContext]
  ): (PlyOutput, Double) =
    import d._
    
    val player = gameBefore.position.color
    val winBefore = evalBeforeDeep.lines.headOption.map(_.winPct).getOrElse(50.0)
    
    val statusAfter = gameAfter.position.status
    val winnerAfter = gameAfter.position.winner
    
    val (winAfterForPlayer, _, evalAfterOpt) =
      statusAfter match
        case Some(Status.Mate) =>
          val sideWin = if winnerAfter.contains(gameAfter.position.color) then 100.0 else 0.0
          val moverWin = if winnerAfter.contains(player) then 100.0 else 0.0
          (moverWin, sideWin, None)
        case Some(_) => // Draw
          (50.0, 50.0, None)
        case None =>
          // evalAfterDeep is from opponent perspective (side to move)
          val eval = evalAfterDeep.getOrElse(EngineEval(0, Nil)) // Should verify logic if missing
          val side = eval.lines.headOption.map(_.winPct).getOrElse(50.0)
          val mover = if player == gameAfter.position.color then side else 100.0 - side
          (mover, side, Some(eval))

    val delta = winAfterForPlayer - winBefore
    val epBefore = clamp01(winBefore / 100.0)
    val epAfter = clamp01(winAfterForPlayer / 100.0)
    val epLoss = math.max(0.0, epBefore - epAfter)

    val baseJudgement =
      statusAfter match
        case Some(Status.Mate) => "mate"
        case Some(_) => "draw"
        case _ =>
          if epLoss <= 0.0 then "best"
          else if epLoss <= 0.05 then "good"
          else if epLoss <= 0.10 then "inaccuracy"
          else if epLoss <= 0.20 then "mistake"
          else "blunder"

    // Material
    val materialBefore = material(gameBefore.position.board, player)
    val materialAfter = material(gameAfter.position.board, player)
    val sacrificed = materialAfter < materialBefore - 1.5
    val materialDiff = materialAfter - materialBefore

    // Best move material diff
    val bestMovePv = evalBeforeDeep.lines.headOption.map(_.pv)
    val bestMaterialDiff = bestMovePv.flatMap { pv =>
      pv.headOption.flatMap { bestMoveStr =>
        chess.format.Uci(bestMoveStr).flatMap(u => gameBefore.apply(u).toOption).map { case (bg, _) =>
           material(bg.position.board, player) - materialBefore
        }
      }
    }

    val tacticalMotif =
      if materialDiff < -0.5 && bestMaterialDiff.exists(_ >= 0.0) && epLoss > 0.1 then Some("Material Loss")
      else if materialDiff < 0.5 && bestMaterialDiff.exists(_ > 1.0) && epLoss > 0.1 then Some("Missed Tactics")
      else if materialDiff > 0.5 then Some("Material Gain")
      else None

    val specialBrilliant = sacrificed && (baseJudgement == "best" || baseJudgement == "excellent") && epLoss <= 0.02 && epBefore <= 0.7 && epBefore >= 0.3
    val greatMove = !specialBrilliant && (baseJudgement == "best" || baseJudgement == "excellent" || baseJudgement == "good") && ((epBefore < 0.35 && epAfter >= 0.55) || (epAfter - epBefore) >= 0.2)
    val miss = (baseJudgement == "inaccuracy" || baseJudgement == "mistake" || baseJudgement == "blunder") && prevDeltaForOpp <= -20

    val special = if specialBrilliant then Some("brilliant") else if greatMove then Some("great") else None
    
    val inBook = opening.exists(op => gameAfter.ply.value <= op.ply.value)
    val finalJudgement = if inBook then "book" else baseJudgement

    // Concepts
    ConceptScorer.computeBreakAndInfiltration(gameAfter.position, gameAfter.position.color)
    val conceptScoresBefore = ConceptScorer.score(
      features = moverFeaturesBefore,
      oppFeatures = oppFeaturesBefore,
      evalShallowWin = evalBeforeShallow.lines.headOption.map(_.winPct).getOrElse(winBefore),
      evalDeepWin = evalBeforeDeep.lines.headOption.map(_.winPct).getOrElse(winBefore),
      multiPvWin = evalBeforeDeep.lines.map(_.winPct),
      position = gameBefore.position,
      sideToMove = gameBefore.position.color
    )
    val conceptScores = ConceptScorer.score(
      features = featuresAfter,
      oppFeatures = oppFeaturesAfter,
      // FIX: Use evalAfterDeep (the resulting position's eval) instead of evalBefore.
      // Note: evalAfterDeep is from opponent's perspective, so we invert winPct for mover's view.
      evalShallowWin = evalAfterDeep.flatMap(_.lines.headOption).map(l => 100.0 - l.winPct).getOrElse(winAfterForPlayer),
      evalDeepWin = evalAfterDeep.flatMap(_.lines.headOption).map(l => 100.0 - l.winPct).getOrElse(winAfterForPlayer),
      multiPvWin = evalAfterDeep.map(_.lines.map(l => 100.0 - l.winPct)).getOrElse(List(winAfterForPlayer)),
      position = gameAfter.position,
      sideToMove = gameAfter.position.color,
      san = move.toSanStr.value
    )

    // Map to Concepts case class (Helper)
    def toConcepts(s: ConceptScorer.Scores) = Concepts(
      dynamic = s.dynamic, drawish = s.drawish, imbalanced = s.imbalanced, tacticalDepth = s.tacticalDepth,
      blunderRisk = s.blunderRisk, pawnStorm = s.pawnStorm, fortress = s.fortress, colorComplex = s.colorComplex,
      badBishop = s.badBishop, goodKnight = s.goodKnight, rookActivity = s.rookActivity, kingSafety = s.kingSafety,
      dry = s.dry, comfortable = s.comfortable, unpleasant = s.unpleasant, engineLike = s.engineLike,
      conversionDifficulty = s.conversionDifficulty, sacrificeQuality = s.sacrificeQuality, alphaZeroStyle = s.alphaZeroStyle
    )
    val conceptsBefore = toConcepts(conceptScoresBefore)
    val conceptsCurrent = toConcepts(conceptScores)
    
    // Concept Delta
    val conceptDelta = Concepts(
        dynamic = conceptsCurrent.dynamic - conceptsBefore.dynamic,
        drawish = conceptsCurrent.drawish - conceptsBefore.drawish,
        imbalanced = conceptsCurrent.imbalanced - conceptsBefore.imbalanced,
        tacticalDepth = conceptsCurrent.tacticalDepth - conceptsBefore.tacticalDepth,
        blunderRisk = conceptsCurrent.blunderRisk - conceptsBefore.blunderRisk,
        pawnStorm = conceptsCurrent.pawnStorm - conceptsBefore.pawnStorm,
        fortress = conceptsCurrent.fortress - conceptsBefore.fortress,
        colorComplex = conceptsCurrent.colorComplex - conceptsBefore.colorComplex,
        badBishop = conceptsCurrent.badBishop - conceptsBefore.badBishop,
        goodKnight = conceptsCurrent.goodKnight - conceptsBefore.goodKnight,
        rookActivity = conceptsCurrent.rookActivity - conceptsBefore.rookActivity,
        kingSafety = conceptsCurrent.kingSafety - conceptsBefore.kingSafety,
        dry = conceptsCurrent.dry - conceptsBefore.dry,
        comfortable = conceptsCurrent.comfortable - conceptsBefore.comfortable,
        unpleasant = conceptsCurrent.unpleasant - conceptsBefore.unpleasant,
        engineLike = conceptsCurrent.engineLike - conceptsBefore.engineLike,
        conversionDifficulty = conceptsCurrent.conversionDifficulty - conceptsBefore.conversionDifficulty,
        sacrificeQuality = conceptsCurrent.sacrificeQuality - conceptsBefore.sacrificeQuality,
        alphaZeroStyle = conceptsCurrent.alphaZeroStyle - conceptsBefore.alphaZeroStyle
    )

    // Phase 4 Concept Labeler (Moved up)
    val bestEvalVal = evalBeforeDeep.lines.headOption.flatMap(l => l.cp.orElse(l.mate.map(m => if m>0 then 10000-m else -10000+m))).getOrElse(0)
    
    // playedEvalRel: Mover's perspective. evalAfterDeep is Opponent's perspective (side to move), so we negate.
    val playedEvalRel = evalAfterDeep.flatMap(_.lines.headOption).map(l => 
       -(l.cp.getOrElse(l.mate.map(m => if m>0 then 10000-m else -10000+m).getOrElse(0)))
    ).getOrElse(0)

    val positionalTags = ConceptLabeler.labelPositional(
       position = gameAfter.position, perspective = player, ply = gameAfter.ply,
       self = featuresAfter, opp = oppFeaturesAfter, concepts = Some(conceptScores), winPct = winBefore
    )

    val p4Concepts = ConceptLabeler.labelAll(
        featuresBefore = p4FeaturesBefore, featuresAfter = p4FeaturesAfter,
        movePlayedUci = move.toUci.uci, experiments = experiments,
        baselineEval = bestEvalVal, evalAfterPlayed = playedEvalRel, bestEval = bestEvalVal,
        positionalTags = positionalTags
    )

    // Phase Label
    val phaseLabel = p4Concepts.transitionTags.headOption.map(_.toSnakeCase)

    val bestVsSecondGap = evalBeforeDeep.lines.take(2) match
       case top :: second :: _ => Some((top.winPct - second.winPct).abs)
       case _ :: Nil if gameBefore.position.legalMoves.size > 1 => 
          // Requested MultiPV but got only 1 line? Maybe singular move or engine cutoff.
          // Or maybe only 1 sensible move found.
          // Let's assume high gap if engine is certain enough to prune everything else or legal moves is small.
          // But safer is None if we are unsure.
          None 
       case _ => None

    val bestVsPlayedGap = (for
        top <- evalBeforeDeep.lines.headOption
        played <- evalBeforeDeep.lines.find(_.move == move.toUci.uci)
        if top.move != played.move
    yield (top.winPct - played.winPct).abs).orElse(None)

    // Tags & Mistakes
    val semanticTags = p4Concepts.positionalTags.map(_.toSnakeCase)
    
    val baseMistakeCategory = p4Concepts.mistakeTags.headOption.map(_.toSnakeCase)

    val mistakeCategory = if miss then Some("tactical_miss") else baseMistakeCategory

    // Phase Calculation
    val gamePhase = PhaseCalculator.getPhase(
       fen = Fen.write(gameBefore).value, ply = gameAfter.ply.value, semanticTags = semanticTags
    )

    // Practicality
    val evalSpread = evalBeforeDeep.lines.take(3) match
       case lines if lines.size >= 2 => (lines.head.winPct - lines.last.winPct).abs
       case _ => 0.0
    val playerElo = playerContext.flatMap(ctx => if player == chess.White then ctx.whiteElo else ctx.blackElo)
    val practicality = Some(
       PracticalityScorer.scoreFromConcepts(
         labels = p4Concepts, evalSpread = evalSpread, tacticalDepth = conceptsCurrent.tacticalDepth, playerElo = playerElo
       )
    )



    // Phase 4.5 Role Labeler Integration
    val ctx = AnalysisContext(
      move = move.toUci.uci,
      evalBefore = Some(evalBeforeDeep),
      evalAfter = evalAfterOpt,
      bestMoveBefore = evalBeforeDeep.lines.headOption.map(_.move), // This might be sanitized (Option[String])
      bestMoveEval = Some(evalBeforeDeep),
      featuresBefore = p4FeaturesBefore,
      featuresAfter = p4FeaturesAfter,
      phaseBefore = p4FeaturesBefore.materialPhase.phase,
      phaseAfter = p4FeaturesAfter.materialPhase.phase,
      isCapture = move match { case m: chess.Move => m.captures; case _ => false },
      isCheck = gameAfter.position.check.yes,
      isCastle = move match { case m: chess.Move => m.castles; case _ => false }
    )
    val newRoles = RoleLabeler.label(ctx).map(_.toString).toList.sorted

    (PlyOutput(
      ply = gameAfter.ply, turn = player, san = move.toSanStr.value, uci = move.toUci.uci,
      fen = Fen.write(gameAfter).value, fenBefore = Fen.write(gameBefore).value,
      legalMoves = gameBefore.position.legalMoves.size,
      features = featuresAfter,
      evalBeforeShallow = evalBeforeShallow, evalBeforeDeep = evalBeforeDeep,
      winPctBefore = winBefore, winPctAfterForPlayer = winAfterForPlayer,
      deltaWinPct = delta,
      epBefore = epBefore, epAfter = epAfter, epLoss = epLoss,
      judgement = finalJudgement.toString.toLowerCase,
      special = special,
      conceptsBefore = conceptsBefore, concepts = conceptsCurrent, conceptDelta = conceptDelta,
      bestVsSecondGap = bestVsSecondGap, bestVsPlayedGap = bestVsPlayedGap,
      semanticTags = semanticTags, mistakeCategory = mistakeCategory,
      phaseLabel = phaseLabel, phase = gamePhase.toString.toLowerCase,
      practicality = practicality,
      materialDiff = materialDiff, bestMaterialDiff = bestMaterialDiff,
      tacticalMotif = tacticalMotif,
      roles = newRoles,
      conceptLabels = Some(p4Concepts),
      fullFeatures = Some(p4FeaturesAfter), playedEvalCp = Some(playedEvalRel),
      hypotheses = d.experiments.map { exp =>
         val meta = exp.metadata
         val label = meta.getOrElse("candidateType", exp.expType.toString)
         // Calculate hypothetical winPct if eval exists
         val lines = exp.eval.lines
         val winPct = lines.headOption.map(_.winPct).getOrElse(50.0)
         // PV?
         val pv = lines.headOption.map(_.pv).getOrElse(Nil)
         Branch(
           move = exp.move.getOrElse("?"), // If None, it was null-move experiment or general eval
           winPct = winPct,
           pv = pv,
           label = label
         )
      }
    ), delta)

  private def clamp01(d: Double): Double = math.max(0.0, math.min(1.0, d))
  private def material(board: chess.Board, color: chess.Color): Double =
     board.pieces.foldLeft(0.0) { (acc, piece) =>
       if piece.color == color then 
         val v = piece.role match
            case chess.Pawn => 1.0
            case chess.Knight => 3.0
            case chess.Bishop => 3.0
            case chess.Rook => 5.0
            case chess.Queen => 9.0
            case _ => 0.0
         acc + v
       else acc
     }

  /** Fallback RawPlyData for error recovery - creates minimal valid data when engine fails */
  private def fallbackRawPlyData(
      idx: Int, 
      mod: MoveOrDrop, 
      gameBefore: Game, 
      gameAfter: Game
  ): RawPlyData =
    // val player = gameBefore.position.color
    val emptyEval = EngineEval(0, Nil)
    val emptyFeatures = FeatureExtractor.SideFeatures(
      pawnIslands = 0, isolatedPawns = 0, doubledPawns = 0, passedPawns = 0,
      rookOpenFiles = 0, rookSemiOpenFiles = 0, bishopPair = false, kingRingPressure = 0, spaceControl = 0
    )
    val emptyP4Features = FeatureExtractor.extractPositionFeatures(
      Fen.write(gameBefore).value, gameBefore.ply.value
    )
    RawPlyData(
      plyIndex = idx,
      gameBefore = gameBefore,
      gameAfter = gameAfter,
      move = mod,
      evalBeforeShallow = emptyEval,
      evalBeforeDeep = emptyEval,
      evalAfterDeep = None,
      experiments = Nil,
      featuresBefore = emptyFeatures,
      moverFeaturesBefore = emptyFeatures,
      oppFeaturesBefore = emptyFeatures,
      featuresAfter = emptyFeatures,
      oppFeaturesAfter = emptyFeatures,
      p4FeaturesBefore = emptyP4Features,
      p4FeaturesAfter = emptyP4Features
    )

package lila.llm.analysis

import lila.llm.model._
import lila.llm.model.authoring.*
import lila.llm.analysis.L3.{ PvLine, TensionPolicy }
import chess.*
import chess.format.Fen
import lila.llm.analysis.MoveAnalyzer

/**
 * Detects "Ghost Plans" and generates ProbeRequests for the client.
 */
object ProbeDetector:

  private val ScoreThreshold = 0.70
  private val MaxMovesPerPlan = 3
  private val DefaultDepth = 20
  private val MaxProbeRequests = 8

  case class StrategicFrame(
    cause: String,
    consequence: String,
    turningPoint: String,
    severity: String
  )

  case class HypothesisVerificationSignals(
    supportSignals: List[String],
    conflictSignals: List[String],
    consistencyBonus: Double,
    contradictionPenalty: Double,
    longSupportSignals: List[String],
    longConflictSignals: List[String],
    longConfidenceDelta: Double,
    strategicFrame: Option[StrategicFrame]
  )

  private def planEvidenceProbes(
    fen: String,
    playedMove: Option[String],
    candidates: List[CandidateInfo],
    authorQuestions: List[AuthorQuestion],
    baseline: Option[PvLine],
    legalUci: Set[String]
  ): List[ProbeRequest] =
    val playedUci = playedMove.filter(legalUci.contains).getOrElse(return Nil)
    val afterPlayedFen = NarrativeUtils.uciListToFen(fen, List(playedUci))

    def mkProbe(
      id: String,
      probeFen: String,
      moves: List[String],
      purpose: String,
      question: AuthorQuestion,
      multiPv: Int,
      planName: String
    ): ProbeRequest =
      val base = Option.when(probeFen == fen)(baseline).flatten
      ProbeRequest(
        id = id,
        fen = probeFen,
        moves = moves,
        depth = DefaultDepth,
        purpose = Some(purpose),
        questionId = Some(question.id),
        questionKind = Some(question.kind.toString),
        multiPv = Some(multiPv),
        planName = Some(planName),
        baselineMove = base.flatMap(_.moves.headOption),
        baselineEvalCp = base.map(_.evalCp),
        baselineMate = base.flatMap(_.mate),
        baselineDepth = base.map(_.depth).filter(_ > 0)
      )

    val evidence = scala.collection.mutable.ListBuffer.empty[ProbeRequest]

    // Task A (QID, etc): probe the played move itself with reply MultiPV,
    // even when it already appears in root MultiPV, to surface opponent branching.
    authorQuestions
      .find(q => q.kind == AuthorQuestionKind.TensionDecision || q.kind == AuthorQuestionKind.DefensiveTask)
      .foreach { q =>
        val purpose =
          if (q.kind == AuthorQuestionKind.DefensiveTask) "defense_reply_multipv"
          else "reply_multipv"
        evidence += mkProbe(
          id = s"evidence_reply_${q.id}",
          probeFen = fen,
          moves = List(playedUci),
          purpose = purpose,
          question = q,
          multiPv = 3,
          planName = "Evidence: reply MultiPV"
        )
      }

    // Task B (recapture branching): after a capture, probe at least 2 distinct recaptures if possible.
    authorQuestions.find(_.kind == AuthorQuestionKind.StructuralCommitment).foreach { q =>
      val recaptures = recaptureUcis(afterPlayedFen, playedUci).take(3)
      if (recaptures.size >= 2)
        evidence += mkProbe(
          id = s"evidence_recapture_${q.id}",
          probeFen = afterPlayedFen,
          moves = recaptures,
          purpose = "recapture_branches",
          question = q,
          multiPv = 2,
          planName = "Evidence: recapture branches"
        )
    }

    // Keep-tension branching: when the best move keeps tension (often castling),
    // probe the opponent's main structural choices (e.g., ...dxc4 vs ...c5 in QID).
    authorQuestions.find(_.kind == AuthorQuestionKind.PlanClash).foreach { q =>
      val bestUci = candidates.headOption.flatMap(_.uci).filterNot(_ == playedUci)
      bestUci.foreach { uci =>
        val afterBestFen = NarrativeUtils.uciListToFen(fen, List(uci))
        val (capOpt, pushOpt) = MovePredicates.planClashOptions(afterBestFen)
        val opts = List(capOpt, pushOpt).flatten.distinct.take(2)
        if (opts.size >= 2)
          evidence += mkProbe(
            id = s"evidence_keep_${q.id}",
            probeFen = afterBestFen,
            moves = opts,
            purpose = "keep_tension_branches",
            question = q,
            multiPv = 2,
            planName = "Evidence: keep tension branches"
          )
      }
    }

    // Conversion probing: when we are in a conversion window, probe the best move's reply MultiPV
    // so we can describe the defender's main resource and how the advantage persists.
    authorQuestions.find(_.kind == AuthorQuestionKind.ConversionPlan).foreach { q =>
      val bestUci = candidates.headOption.flatMap(_.uci).filter(legalUci.contains)
      bestUci.foreach { uci =>
        evidence += mkProbe(
          id = s"evidence_convert_${q.id}",
          probeFen = fen,
          moves = List(uci),
          purpose = "convert_reply_multipv",
          question = q,
          multiPv = 3,
          planName = "Evidence: convert reply MultiPV"
        )
      }
    }

    // Task D (latent plan): probe "free tempo" legal quiet branches + counters (refutations).
    // The base FEN is AFTER the played move, where the opponent is to move.
    authorQuestions
      .filter(_.kind == AuthorQuestionKind.LatentPlan)
      .flatMap(q => q.latentPlan.map(lp => q -> lp))
      .take(1) // Keep this bounded: one latent plan per move is enough for book-style clarity.
      .foreach { (q, lp) =>
        Fen.read(chess.variant.Standard, Fen.Full(afterPlayedFen)).foreach { afterPos =>
          val legalMoves = afterPos.legalMoves.toList
          
          def isQuiet(mv: Move): Boolean =
            !mv.captures && !mv.after.check.yes

          def slownessScore(mv: Move): Int =
            var s = 0
            if (mv.castle.isDefined) s -= 3
            mv.piece.role match
              case Pawn =>
                mv.orig.file match
                  case File.A | File.H => s += 5
                  case File.B | File.G => s += 3
                  case f if MovePredicates.isCentralFile(f) => s -= 2
                  case _ => s += 0
              case Queen | Rook => s += 2
              case Knight | Bishop =>
                val back = if afterPos.color.white then Rank.First else Rank.Eighth
                if (mv.orig.rank == back) s -= 2 else s += 1
              case _ => s += 0
            s

          val freeTempoCfg = lp.evidencePolicy.freeTempoVerify.getOrElse(FreeTempoVerify())
          val slowMoves =
            legalMoves
              .filter(isQuiet)
              .sortBy(mv => -slownessScore(mv))
              .map(_.toUci.uci)
              .distinct
              .take(freeTempoCfg.maxBranches)

          if (slowMoves.nonEmpty)
            evidence += mkProbe(
              id = s"evidence_free_tempo_${q.id}",
              probeFen = afterPlayedFen,
              moves = slowMoves,
              purpose = "free_tempo_branches",
              question = q,
              multiPv = freeTempoCfg.replyMultiPv,
              planName = s"Evidence: free tempo (${lp.seedId})"
            )

          // 2) Immediate Viability (Why not now?)
          lp.evidencePolicy.immediateViability.foreach { ivCfg =>
             val candidateUcis = lp.candidateMoves.flatMap { pat =>
               // Generating concrete moves from patterns for the CURRENT position (afterPlayedFen)
               // Re-using LatentPlanSeeder logic would be ideal, but here we need simple matching.
               // We assume LatentPlanInfo already carries concrete candidate moves if Seeder did its job.
               // Since AuthorQuestion.LatentPlanInfo is the source, let's use the ones passed in via AuthorQuestion?
               // Actually, AuthorQuestion just holds the Info.
               // For now, let's filter legal moves matching the pattern.
               legalMoves.filter(m => matchesMovePattern(m, pat)).map(_.toUci.uci)
             }.distinct.take(2)

             if (candidateUcis.nonEmpty)
               evidence += mkProbe(
                 id = s"evidence_immediate_${q.id}",
                 probeFen = afterPlayedFen,
                 moves = candidateUcis,
                 purpose = "latent_plan_immediate",
                 question = q,
                 multiPv = ivCfg.replyMultiPv,
                 planName = s"Evidence: immediate viability (${lp.seedId})"
               )
          }

          val refCfg = lp.evidencePolicy.refutationCheck.getOrElse(RefutationCheck())
          val counterPatterns = (refCfg.counters ++ lp.typicalCounters).distinct

          def matchesCounterPattern(mv: Move, p: CounterPattern): Boolean =
            p match
              case CounterPattern.PawnPushBlock(file) =>
                mv.piece.role == Pawn && !mv.captures && mv.orig.file == file
              case CounterPattern.CentralStrike =>
                mv.piece.role == Pawn &&
                  (MovePredicates.isCentralFile(mv.orig.file) || MovePredicates.isCentralFile(mv.dest.file))
              case CounterPattern.PieceControl(role, sq) =>
                mv.piece.role == role && mv.dest == sq
              case CounterPattern.Counterplay(flank) =>
                mv.piece.role == Pawn && !mv.captures && (flank match
                  case Flank.Kingside  => mv.dest.file == File.F || mv.dest.file == File.G || mv.dest.file == File.H
                  case Flank.Queenside => mv.dest.file == File.A || mv.dest.file == File.B || mv.dest.file == File.C
                  case Flank.Center    => MovePredicates.isCentralFile(mv.dest.file)
                )

          // Refutation Candidates: Pattern Matches + Engine Top Replies (Contextual)
          val patternCounters = 
            if (counterPatterns.isEmpty) Nil
            else legalMoves.filter(mv => counterPatterns.exists(p => matchesCounterPattern(mv, p)))

          // We don't have engine top replies for the latent checking position yet (chicken-egg).
          // We rely on pattern matching for the *initial* probe.
          // However, we can add "Active Defense" candidates (checks, captures) as generic refutation candidates.
          val activeDefense = legalMoves.filter(m => m.captures || m.after.check.yes)

          val counterMoves = (patternCounters ++ activeDefense)
            .sortBy(slownessScore) // prefer moves that do something
            .map(_.toUci.uci)
            .distinct
            .take(3)

          if (counterMoves.nonEmpty)
            evidence += mkProbe(
              id = s"evidence_refute_${q.id}",
              probeFen = afterPlayedFen,
              moves = counterMoves,
              purpose = "latent_plan_refutation",
              question = q,
              multiPv = refCfg.replyMultiPv,
              planName = s"Evidence: refutation (${lp.seedId})"
            )
        }
      }

    evidence.toList.distinctBy(_.id)

  private def matchesMovePattern(mv: Move, pat: MovePattern): Boolean =
    pat match
      case MovePattern.PawnAdvance(f) => mv.piece.role == Pawn && mv.orig.file == f
      case MovePattern.PawnLever(f, t) => mv.piece.role == Pawn && mv.orig.file == f && mv.dest.file == t
      case MovePattern.PieceTo(r, s) => mv.piece.role == r && mv.dest == s
      case MovePattern.PieceManeuver(r, t, _) => mv.piece.role == r && mv.dest == t // Simplified match
      case MovePattern.Exchange(r, s) => mv.piece.role == r && mv.dest == s && mv.captures
      case MovePattern.Castle => mv.castle.isDefined
      case MovePattern.BatteryFormation(_, _, _) => true // Hard to match single move to battery, skip filtering

  private def recaptureUcis(afterFen: String, playedUci: String): List[String] =
    val played = chess.format.Uci(playedUci).collect { case m: chess.format.Uci.Move => m }
    (for
      pm <- played
      pos <- Fen.read(chess.variant.Standard, Fen.Full(afterFen))
    yield
      pos.legalMoves.toList
        .filter(mv => mv.captures && mv.dest == pm.dest)
        .map(_.toUci.uci)
        .distinct
    ).getOrElse(Nil)

  /**
   * Detects ghost plans and generates requests.
   */
  def detect(
    ctx: IntegratedContext,
    planScoring: PlanScoringResult,
    multiPv: List[PvLine],
    fen: String,
    playedMove: Option[String] = None,
    candidates: List[CandidateInfo] = Nil,
    authorQuestions: List[AuthorQuestion] = Nil
  ): List[ProbeRequest] = {
    // We must only emit LEGAL UCI moves. If FEN is invalid, fail closed (no probes).
    Fen.read(chess.variant.Standard, Fen.Full(fen)).map { pos =>
      val sideToMove = pos.color
      val ctxSideToMove = if ctx.isWhiteToMove then White else Black
      if (ctxSideToMove != sideToMove) Nil
      else
        val legalMoves = pos.legalMoves.toList
        val legalUci = legalMoves.map(_.toUci.uci).toSet
        val topPvMoves = multiPv.flatMap(_.moves.headOption).toSet
        val baseline = multiPv.headOption

        val evidenceProbes =
          planEvidenceProbes(
            fen = fen,
            playedMove = playedMove,
            candidates = candidates,
            authorQuestions = authorQuestions,
            baseline = baseline,
            legalUci = legalUci
          )

        def mkProbe(
            id: String,
            moves: List[String],
            planName: Option[String]
        ): ProbeRequest =
          ProbeRequest(
            id = id,
            fen = fen,
            moves = moves,
            depth = DefaultDepth,
            planName = planName,
            baselineMove = baseline.flatMap(_.moves.headOption),
            baselineEvalCp = baseline.map(_.evalCp),
            baselineMate = baseline.flatMap(_.mate),
            baselineDepth = baseline.map(_.depth).filter(_ > 0)
          )



        def isEdgePawnInOpening(uci: String): Boolean =
          if (!legalUci.contains(uci)) false
          else
            chess.format.Uci(uci).collect { case m: chess.format.Uci.Move => m }.flatMap(pos.move(_).toOption) match
              case Some(mv) =>
                val isOpening = pos.board.occupied.count >= 28
                isOpening && mv.piece.role == Pawn && (mv.orig.file == File.A || mv.orig.file == File.H) && !mv.captures
              case None => false

        // 0) PV validation: if any PV is illegal/mismatched (partial parse), request a probe for baseline.
        val hasSuspiciousPv =
          multiPv.exists(pv => pv.moves.nonEmpty && MoveAnalyzer.parsePv(fen, pv.moves).size != pv.moves.size)

        val pvRepairProbes =
          if (!hasSuspiciousPv) Nil
          else
            baseline
              .flatMap(_.moves.headOption)
              .filter(legalUci.contains)
              .toList
              .map { mv =>
                mkProbe(
                  id = s"pv_repair_${mv}_${Integer.toHexString(fen.hashCode)}",
                  moves = List(mv),
                  planName = Some("PV validation probe")
                )
              }

        // 0b) Played move probe: if it isn't in top MultiPV, probe it when it is structurally/tactically critical.
        val playedMoveProbe =
          playedMove
            .filter(legalUci.contains)
            .filterNot(topPvMoves.contains)
            .toList
            .flatMap { mv =>
              val should =
                // Central tension: played move resolves tension when policy says maintain
                (ctx.pawnAnalysis.exists(_.tensionPolicy == TensionPolicy.Maintain) && MovePredicates.isTensionReleaseCandidate(pos, mv)) ||
                  // Slow edge pawn moves in opening often need concrete justification/refutation
                  isEdgePawnInOpening(mv) ||
                  // Tactical pressure situations: show best reply line to anchor claims
                  ctx.tacticalThreatToUs || ctx.tacticalThreatToThem

              Option.when(should) {
                mkProbe(
                  id = s"played_${mv}_${Integer.toHexString(fen.hashCode)}",
                  moves = List(mv),
                  planName = Some("Played move probe")
                )
              }
            }

        // 0c) Tension probes: when policy says Maintain, proactively probe 1-2 tension-releasing captures
        // to obtain concrete a1/a2 reply samples (recapture/simplification branches).
        val tensionProbes =
          ctx.pawnAnalysis
            .filter(_.tensionPolicy == TensionPolicy.Maintain)
            .toList
            .flatMap { pa =>
              val tensionSq = pa.tensionSquares.toSet
              val candidates =
                legalMoves
                  .filter(_.captures)
                  .filter(mv =>
                    MovePredicates.isCentralFile(mv.orig.file) ||
                      MovePredicates.isCentralFile(mv.dest.file) ||
                      tensionSq.contains(mv.orig.key) ||
                      tensionSq.contains(mv.dest.key)
                  )
                  .map(_.toUci.uci)
                  .distinct
                  .filterNot(topPvMoves.contains)
                  .take(2)

              Option.when(candidates.nonEmpty) {
                mkProbe(
                  id = s"tension_${Integer.toHexString(fen.hashCode)}",
                  moves = candidates,
                  planName = Some("Tension release test")
                )
              }.toList
            }

        // 1. Plan-based Ghost Probes (Existing)
        val planProbes = planScoring.topPlans.flatMap { pm =>
          if (pm.score < ScoreThreshold) None
          else if (pm.plan.color != sideToMove) None
          else
            val repMoves = representativeMoves(pos, pm, legalMoves).take(MaxMovesPerPlan)
            val isRepresented = repMoves.exists(topPvMoves.contains)

            if (repMoves.isEmpty || isRepresented) None
            else
              Some(ProbeRequest(
                id = stableRequestId(pm.plan, fen),
                fen = fen,
                moves = repMoves,
                depth = DefaultDepth,
                planId = Some(pm.plan.id.toString),
                planName = Some(pm.plan.name),
                planScore = Some(pm.score),
                baselineMove = baseline.flatMap(_.moves.headOption),
                baselineEvalCp = baseline.map(_.evalCp),
                baselineMate = baseline.flatMap(_.mate),
                baselineDepth = baseline.map(_.depth).filter(_ > 0)
              ))
        }

        // 2. Competitive Probes: Similar engine scores but different moves
        val competitiveProbes = multiPv.drop(1).take(2).flatMap { pv =>
          val bestPv = multiPv.head
          val scoreDiff = (pv.evalCp - bestPv.evalCp).abs
          // If second/third best move is close (e.g. < 30cp difference), probe it for "choice" explanation
          if (scoreDiff < 30 && pv.moves.nonEmpty) {
            val move = pv.moves.head
            Some(ProbeRequest(
              id = s"competitive_${move}_${Integer.toHexString(fen.hashCode)}",
              fen = fen,
              moves = List(move),
              depth = DefaultDepth,
              planName = Some(s"Competitive Alternative: $move"),
              baselineMove = bestPv.moves.headOption,
              baselineEvalCp = Some(bestPv.evalCp),
              baselineMate = bestPv.mate,
              baselineDepth = Some(bestPv.depth).filter(_ > 0)
            ))
          } else None
        }

        // 3. Defensive Probes: If top move is passive but an aggressive one is slightly worse
        val defensiveProbes = if (multiPv.size > 1 && baseline.exists(_.evalCp > -50)) {
          val aggressiveMoves = legalMoves.filter(m => m.captures || m.after.check.yes).map(_.toUci.uci)
          multiPv.filter(pv => pv.moves.headOption.exists(aggressiveMoves.contains)).headOption.flatMap { aggPv =>
            val bestPv = multiPv.head
            val cost = (bestPv.evalCp - aggPv.evalCp)
            // If an aggressive move exists that costs 50-150cp, probe it to explain "Why not X?"
            if (cost >= 50 && cost <= 150) {
              val move = aggPv.moves.head
              Some(ProbeRequest(
                id = s"aggressive_why_not_${move}_${Integer.toHexString(fen.hashCode)}",
                fen = fen,
                moves = List(move),
                depth = DefaultDepth,
                planName = Some(s"Aggressive Alternative: $move"),
                baselineMove = bestPv.moves.headOption,
                baselineEvalCp = Some(bestPv.evalCp),
                baselineMate = bestPv.mate,
                baselineDepth = Some(bestPv.depth).filter(_ > 0)
              ))
            } else None
          }
        } else None

        (evidenceProbes ++ pvRepairProbes ++ playedMoveProbe ++ tensionProbes ++ planProbes ++ competitiveProbes ++ defensiveProbes.toList)
          .distinctBy(r => s"${r.fen}|${r.moves.mkString(",")}")
          .take(MaxProbeRequests)
    }.getOrElse(Nil)
  }

  /**
   * Structured probe-derived signals for hypothesis validation/ranking.
   * Reuses existing probe metadata without introducing opening-specific rules.
   */
  def hypothesisVerificationSignals(
    candidate: CandidateInfo,
    probeResults: List[ProbeResult],
    probeRequests: List[ProbeRequest],
    isWhiteToMove: Boolean
  ): HypothesisVerificationSignals =
    val candUciNorm = candidate.uci.map(NarrativeUtils.normalizeUciMove).filter(_.nonEmpty)

    def moveMatches(raw: String): Boolean =
      candUciNorm.exists(u => NarrativeUtils.uciEquivalent(u, raw))

    val matchingRequests =
      candUciNorm.toList.flatMap { _ =>
        probeRequests.filter(req => req.moves.exists(moveMatches))
      }

    val matchingResults =
      probeResults.filter(pr => pr.probedMove.exists(moveMatches))

    val supportSignals = scala.collection.mutable.ListBuffer.empty[String]
    val conflictSignals = scala.collection.mutable.ListBuffer.empty[String]
    val longSupportSignals = scala.collection.mutable.ListBuffer.empty[String]
    val longConflictSignals = scala.collection.mutable.ListBuffer.empty[String]

    if matchingRequests.exists(_.purpose.contains("reply_multipv")) then
      supportSignals += "reply multipv coverage collected"
    if matchingRequests.exists(_.purpose.exists(_.contains("convert"))) then
      supportSignals += "conversion branch checked by probe"
    if matchingRequests.exists(_.purpose.exists(_.contains("tension"))) then
      supportSignals += "tension-release branch was explicitly tested"

    matchingResults.foreach { pr =>
      val moverLoss = if isWhiteToMove then -pr.deltaVsBaseline else pr.deltaVsBaseline
      val collapse = pr.l1Delta.flatMap(_.collapseReason).map(_.trim).filter(_.nonEmpty)
      val collapseLower = collapse.map(_.toLowerCase)
      val future = pr.futureSnapshot
      val hasDelayedProgress =
        future.exists(fs => fs.planPrereqsMet.nonEmpty || fs.resolvedThreatKinds.nonEmpty)
      val hasTrajectoryIntent =
        containsLongSignalKeyword(pr.purpose.getOrElse("")) ||
          pr.keyMotifs.exists(containsLongSignalKeyword)
      val onlyThreatGrowthWithoutProgress =
        future.exists(fs =>
          fs.newThreatKinds.nonEmpty &&
            fs.planPrereqsMet.isEmpty &&
            fs.resolvedThreatKinds.isEmpty
        )
      val collapseHasLongFailure =
        collapseLower.exists { txt =>
          txt.contains("structure") ||
            txt.contains("king safety") ||
            txt.contains("king") ||
            txt.contains("exposed") ||
            txt.contains("pawn")
        }
      if moverLoss <= 25 then
        supportSignals += "probe keeps score near baseline"
      else if moverLoss <= 80 then
        supportSignals += "probe indicates a manageable practical concession"
      else if moverLoss >= 200 then
        val tail = collapse.map(r => s" ($r)").getOrElse("")
        conflictSignals += s"probe refutes the move with a forcing swing$tail"
      else if moverLoss >= 90 then
        val tail = collapse.map(r => s" ($r)").getOrElse("")
        conflictSignals += s"probe shows a clear practical concession$tail"

      if pr.mate.exists(_ < 0) then
        conflictSignals += "probe line allows a mate threat against the mover"
      if pr.mate.exists(_ > 0) then
        supportSignals += "probe line preserves mating pressure"

      if (moverLoss <= 60 && hasDelayedProgress) || hasTrajectoryIntent then
        if hasDelayedProgress then
          longSupportSignals += "long-horizon probe confirms delayed plan prerequisites"
        if hasTrajectoryIntent then
          longSupportSignals += "long-horizon probe samples conversion and trajectory branches"

      if moverLoss >= 90 then
        longConflictSignals += "long-horizon probe shows the delayed plan is too costly"
      if onlyThreatGrowthWithoutProgress then
        longConflictSignals += "long-horizon probe adds threats without meeting plan prerequisites"
      if collapseHasLongFailure then
        val tail = collapse.map(r => s" ($r)").getOrElse("")
        longConflictSignals += s"long-horizon probe indicates structural or king-safety collapse$tail"
    }

    val hasStrongConflict = conflictSignals.exists { s =>
      s.contains("forcing swing") || s.contains("mate threat")
    }
    val longSupport = longSupportSignals.toList.distinct
    val longConflict = longConflictSignals.toList.distinct
    val longConfidenceDelta =
      clampLongConfidenceDelta((longSupport.size * 0.07) - (longConflict.size * 0.09))
    val consistencyBonus =
      (if supportSignals.nonEmpty then 0.12 else 0.0) +
        (if matchingRequests.nonEmpty && matchingResults.nonEmpty then 0.06 else 0.0)
    val contradictionPenalty =
      (if conflictSignals.nonEmpty then 0.10 else 0.0) +
        (if hasStrongConflict then 0.12 else 0.0)

    HypothesisVerificationSignals(
      supportSignals = supportSignals.toList.distinct,
      conflictSignals = conflictSignals.toList.distinct,
      consistencyBonus = consistencyBonus,
      contradictionPenalty = contradictionPenalty,
      longSupportSignals = longSupport,
      longConflictSignals = longConflict,
      longConfidenceDelta = longConfidenceDelta,
      strategicFrame = deriveStrategicFrame(matchingResults, isWhiteToMove)
    )

  private def containsLongSignalKeyword(text: String): Boolean =
    val lower = Option(text).getOrElse("").toLowerCase
    List("convert", "endgame", "trajectory", "coordination").exists(lower.contains)

  private def clampLongConfidenceDelta(v: Double): Double =
    Math.max(-0.27, Math.min(0.21, v))

  private def deriveStrategicFrame(
    matchingResults: List[ProbeResult],
    isWhiteToMove: Boolean
  ): Option[StrategicFrame] =
    if matchingResults.isEmpty then None
    else
      val seeded =
        matchingResults.map { pr =>
          val moverLoss = if isWhiteToMove then -pr.deltaVsBaseline else pr.deltaVsBaseline
          val collapse = pr.l1Delta.flatMap(_.collapseReason).map(_.toLowerCase).getOrElse("")
          val cause =
            if moverLoss <= 25 then "near-baseline"
            else if moverLoss <= 80 then "manageable-concession"
            else "forcing-swing"
          val consequence =
            if pr.mate.exists(_ < 0) || collapse.contains("king") || collapse.contains("exposed") then
              "king-safety-exposure"
            else if collapse.contains("structure") || collapse.contains("pawn") then
              "structural-collapse"
            else if pr.purpose.exists(_.contains("convert")) || pr.keyMotifs.exists(containsLongSignalKeyword) then
              "conversion-cost"
            else "initiative-handoff"
          val turningPoint =
            if moverLoss >= 90 || pr.mate.nonEmpty then "immediate-sequence"
            else if
              pr.purpose.exists(_.contains("convert")) ||
                pr.futureSnapshot.exists(fs => fs.planPrereqsMet.nonEmpty || fs.planBlockersRemoved.nonEmpty)
            then "simplification-transition"
            else "middlegame-regrouping"
          val severity =
            cause match
              case "forcing-swing"         => 3
              case "manageable-concession" => 2
              case _                       => 1
          (cause, consequence, turningPoint, severity)
        }
      seeded.sortBy(t => -t._4).headOption.map { top =>
        val severityLabel =
          top._4 match
            case 3 => "high"
            case 2 => "medium"
            case _ => "low"
        StrategicFrame(
          cause = top._1,
          consequence = top._2,
          turningPoint = top._3,
          severity = severityLabel
        )
      }

  private def stableRequestId(plan: Plan, fen: String): String =
    val slug = plan.name
      .toLowerCase
      .map(c => if (c.isLetterOrDigit) c else '_')
      .mkString
      .replaceAll("_+", "_")
      .stripPrefix("_")
      .stripSuffix("_")
    s"${plan.id}_${slug}_${Integer.toHexString(fen.hashCode)}"

  /**
   * Produces representative LEGAL UCI moves for a plan in the current position.
   *
   * Strategy:
   * 1) Try evidence-driven mapping (motif -> legal move constraints)
   * 2) Fill remaining slots with a small plan-specific heuristic fallback
   */
  private def representativeMoves(
      pos: Position,
      pm: PlanMatch,
      legalMoves: List[Move]
  ): List[String] = {
    val evidenceUci =
      pm.evidence
        .flatMap(e => movesFromMotif(e.motif, pos, legalMoves))
        .map(_.toUci.uci)
        .distinct

    val fallbackUci =
      movesFromPlan(pm.plan, pos, legalMoves)
        .map(_.toUci.uci)
        .distinct
        .sorted
        .filterNot(evidenceUci.contains)

    (evidenceUci ++ fallbackUci).take(MaxMovesPerPlan)
  }

  private def movesFromMotif(motif: Motif, pos: Position, legalMoves: List[Move]): List[Move] = {
    val sideToMove = pos.color
    motif match {
      case Motif.PawnAdvance(file, fromRank, toRank, c, _, _) if c == sideToMove =>
        legalMoves.filter { mv =>
          mv.piece.role == Pawn &&
          mv.orig.file == file &&
          mv.orig.rank.value + 1 == fromRank &&
          mv.dest.rank.value + 1 == toRank
        }

      case Motif.PawnBreak(file, targetFile, c, _, _) if c == sideToMove =>
        legalMoves.filter { mv =>
          mv.piece.role == Pawn &&
          mv.orig.file == file &&
          mv.dest.file == targetFile &&
          mv.captures
        }

      case Motif.PawnPromotion(file, promotedTo, c, _, _) if c == sideToMove =>
        legalMoves.filter { mv =>
          mv.piece.role == Pawn &&
          mv.dest.file == file &&
          mv.promotion.contains(promotedTo)
        }

      case Motif.PassedPawnPush(file, toRank, c, _, _) if c == sideToMove =>
        legalMoves.filter { mv =>
          mv.piece.role == Pawn &&
          mv.dest.file == file &&
          mv.dest.rank.value + 1 == toRank &&
          !mv.captures
        }

      case Motif.RookLift(file, fromRank, toRank, c, _, _) if c == sideToMove =>
        legalMoves.filter { mv =>
          mv.piece.role == Rook &&
          mv.dest.file == file &&
          mv.orig.rank.value + 1 == fromRank &&
          mv.dest.rank.value + 1 == toRank
        }

      case Motif.Fianchetto(_, c, _, _) if c == sideToMove =>
        legalMoves.filter { mv =>
          mv.piece.role == Bishop &&
          (mv.dest == Square.G2 || mv.dest == Square.B2 || mv.dest == Square.G7 || mv.dest == Square.B7)
        }

      case Motif.Centralization(piece, sq, c, _, _) if c == sideToMove =>
        legalMoves.filter(mv => mv.piece.role == piece && mv.dest == sq)

      case Motif.Outpost(piece, sq, c, _, _) if c == sideToMove =>
        legalMoves.filter(mv => mv.piece.role == piece && mv.dest == sq)

      case Motif.OpenFileControl(file, c, _, _) if c == sideToMove =>
        legalMoves.filter { mv =>
          (mv.piece.role == Rook || mv.piece.role == Queen) && mv.dest.file == file
        }

      case Motif.Capture(piece, _, sq, _, c, _, _, _) if c == sideToMove =>
        legalMoves.filter(mv => mv.piece.role == piece && mv.dest == sq && mv.captures)

      case Motif.Check(_, _, _, c, _, _) if c == sideToMove =>
        legalMoves.filter(_.after.check.yes)

      case Motif.Castling(_, c, _, _) if c == sideToMove =>
        legalMoves.filter(_.castle.isDefined)

      case _ => Nil
    }
  }

  private def movesFromPlan(plan: Plan, pos: Position, legalMoves: List[Move]): List[Move] = {
    val sideToMove = pos.color
    plan match {
      case Plan.KingsideAttack(c) if c == sideToMove =>
        legalMoves.filter(mv => mv.piece.role == Pawn && mv.dest.file.isKingside && !mv.captures)

      case Plan.QueensideAttack(c) if c == sideToMove =>
        legalMoves.filter(mv => mv.piece.role == Pawn && mv.dest.file.isQueenside && !mv.captures)

      case Plan.PawnStorm(c, side) if c == sideToMove =>
        val filePred = if (side == "queenside") (_: File).isQueenside else (_: File).isKingside
        legalMoves.filter(mv => mv.piece.role == Pawn && filePred(mv.dest.file) && !mv.captures)

      case Plan.CentralControl(c) if c == sideToMove =>
        val centralSquares = Set(Square.D4, Square.E4, Square.D5, Square.E5)
        legalMoves.filter { mv =>
          (mv.piece.role == Pawn && mv.dest.file.isCentral && !mv.captures) ||
          (centralSquares.contains(mv.dest) && (mv.piece.role == Knight || mv.piece.role == Bishop))
        }

      case Plan.PieceActivation(c) if c == sideToMove =>
        val backRank = if c.white then Rank.First else Rank.Eighth
        legalMoves.filter(mv =>
          (mv.piece.role == Knight || mv.piece.role == Bishop) && mv.orig.rank == backRank
        )

      case Plan.RookActivation(c) if c == sideToMove =>
        legalMoves.filter(mv => mv.piece.role == Rook && !mv.captures)

      case Plan.PassedPawnPush(c) if c == sideToMove =>
        legalMoves.filter(mv => mv.piece.role == Pawn && !mv.captures)

      case Plan.Promotion(c) if c == sideToMove =>
        legalMoves.filter(mv => mv.piece.role == Pawn && mv.promotion.isDefined)

      case Plan.Blockade(c, squareStr) if c == sideToMove =>
        Square.fromKey(squareStr).map { sq =>
          legalMoves.filter(mv => mv.dest == sq)
        }.getOrElse(Nil)

      case Plan.KingActivation(c) if c == sideToMove =>
        legalMoves.filter(mv => mv.piece.role == King && !mv.captures && mv.castle.isEmpty)

      case _ => Nil
    }
  }

  extension (f: File)
    def isKingside: Boolean = f == File.F || f == File.G || f == File.H
    def isQueenside: Boolean = f == File.A || f == File.B || f == File.C
    def isCentral: Boolean = f == File.D || f == File.E

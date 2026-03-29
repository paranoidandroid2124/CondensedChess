package lila.llm.analysis

import lila.llm.model._
import lila.llm.model.authoring.*
import lila.llm.analysis.L3.{ PvLine, TensionPolicy }
import chess.*
import chess.format.Fen
import lila.llm.analysis.MoveAnalyzer
import lila.llm.analysis.ThemeTaxonomy.{ ThemeL1, ThemeResolver, SubplanCatalog, SubplanId }

/**
 * Detects "Ghost Plans" and generates ProbeRequests for the client.
 */
object ProbeDetector:

  private val ScoreThreshold = 0.62
  private val HighConfidenceGhostScore = 0.72
  private val HighConfidenceGlobalScore = 0.65
  private val MaxMovesPerPlan = 3
  private val DefaultDepth = 20
  private val MaxProbeRequests = 8
  private val PurposeBudget: Map[String, Int] = Map(
    "theme_plan_validation" -> 3,
    "route_denial_validation" -> 2,
    "color_complex_squeeze_validation" -> 2,
    "long_term_restraint_validation" -> 2,
    "reply_multipv" -> 2,
    "defense_reply_multipv" -> 2,
    "convert_reply_multipv" -> 2,
    "recapture_branches" -> 1,
    "keep_tension_branches" -> 1,
    "played_move_counterfactual" -> 1,
    "NullMoveThreat" -> 2
  )

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
    val playedUci = playedMove.filter(legalUci.contains).getOrElse("")
    if (playedUci.isEmpty) return Nil
    val afterPlayedFen = NarrativeUtils.uciListToFen(fen, List(playedUci))

    def mkProbe(
      id: String,
      probeFen: String,
      moves: List[String],
      purpose: String,
      question: AuthorQuestion,
      multiPv: Int,
      planName: String,
      seedId: Option[String] = None,
      maxCpLoss: Option[Int] = None
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
        objective = objectiveForPurpose(purpose),
        seedId = seedId,
        requiredSignals = requiredSignalsForPurpose(purpose),
        horizon = horizonForPurpose(purpose),
        maxCpLoss = maxCpLoss,
        baselineMove = base.flatMap(_.moves.headOption),
        baselineEvalCp = base.map(_.evalCp),
        baselineMate = base.flatMap(_.mate),
        baselineDepth = base.map(_.depth).filter(_ > 0)
      )

    val evidence = scala.collection.mutable.ListBuffer.empty[ProbeRequest]

    // Task A (QID, etc): probe the played move itself with reply MultiPV,
    // even when it already appears in root MultiPV, to surface opponent branching.
    authorQuestions.find(_.evidencePurposes.contains("defense_reply_multipv")).foreach { q =>
      evidence += mkProbe(
        id = s"evidence_reply_${q.id}",
        probeFen = fen,
        moves = List(playedUci),
        purpose = "defense_reply_multipv",
        question = q,
        multiPv = 3,
        planName = "Evidence: defense reply MultiPV"
      )
    }
    authorQuestions
      .find(q =>
        q.evidencePurposes.contains("reply_multipv") &&
          !q.evidencePurposes.contains("defense_reply_multipv")
      )
      .foreach { q =>
        evidence += mkProbe(
          id = s"evidence_reply_${q.id}",
          probeFen = fen,
          moves = List(playedUci),
          purpose = "reply_multipv",
          question = q,
          multiPv = 3,
          planName = "Evidence: reply MultiPV"
        )
      }

    // Task B (recapture branching): after a capture, probe at least 2 distinct recaptures if possible.
    authorQuestions.find(_.evidencePurposes.contains("recapture_branches")).foreach { q =>
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
    authorQuestions.find(_.evidencePurposes.contains("keep_tension_branches")).foreach { q =>
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
    authorQuestions.find(_.evidencePurposes.contains("convert_reply_multipv")).foreach { q =>
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

    evidence.toList.distinctBy(_.id)

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
    planHypotheses: List[PlanHypothesis] = Nil,
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
            planName: Option[String],
            purpose: Option[String] = None
        ): ProbeRequest =
          ProbeRequest(
            id = id,
            fen = fen,
            moves = moves,
            depth = DefaultDepth,
            purpose = purpose,
            planName = planName,
            objective = purpose.flatMap(objectiveForPurpose),
            requiredSignals = purpose.map(requiredSignalsForPurpose).getOrElse(Nil),
            horizon = purpose.flatMap(horizonForPurpose),
            baselineMove = baseline.flatMap(_.moves.headOption),
            baselineEvalCp = baseline.map(_.evalCp),
            baselineMate = baseline.flatMap(_.mate),
            baselineDepth = baseline.map(_.depth).filter(_ > 0)
          )

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

        // 0b) Played move probe: if it isn't in top MultiPV, always probe for robust counterfactual recovery.
        val playedMoveProbe =
          playedMove
            .filter(legalUci.contains)
            .filterNot(topPvMoves.contains)
            .toList
            .flatMap { mv =>
              val should = true

              Option.when(should) {
                mkProbe(
                  id = s"played_${mv}_${Integer.toHexString(fen.hashCode)}",
                  moves = List(mv),
                  planName = Some("Played move probe"),
                  purpose = Some("played_move_counterfactual")
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

        // 1. Theme plan validation probes:
        // Probe strategic hypotheses directly; PV is treated as validation support.
        val planProbes =
          val hypothesisDriven =
            planHypotheses
              .groupBy(hypothesisKey)
              .values
              .toList
              .flatMap(_.sortBy(h => -h.score).headOption)
              .sortBy(h => -h.score)
              .flatMap { h =>
                if h.score < ScoreThreshold || !isPlanValidationCandidate(h) then None
                else
                  val contract = themePlanContract(h)
                  val repMoves = representativeMoves(pos, h, legalMoves, planScoring).take(MaxMovesPerPlan)
                  val novelMoves = repMoves.filterNot(topPvMoves.contains)
                  val selectedMoves =
                    if novelMoves.nonEmpty then novelMoves.take(MaxMovesPerPlan)
                    else if h.score >= HighConfidenceGhostScore && repMoves.nonEmpty then repMoves.take(1)
                    else Nil
                  if selectedMoves.isEmpty then None
                  else
                    val labeledPlanName =
                      contract.subplanId
                        .map(sp => s"${h.planName} [subplan:${sp.id}]")
                        .getOrElse(h.planName)
                    Some(
                      ProbeRequest(
                        id = stableRequestId(h, fen),
                        fen = fen,
                        moves = selectedMoves,
                        depth = DefaultDepth,
                        planId = Some(h.planId),
                        planName = Some(labeledPlanName),
                        planScore = Some(h.score),
                        purpose = Some(contract.purpose),
                        objective = contract.objective,
                        requiredSignals = contract.requiredSignals,
                        horizon = contract.horizon,
                        maxCpLoss = Some(110),
                        baselineMove = baseline.flatMap(_.moves.headOption),
                        baselineEvalCp = baseline.map(_.evalCp),
                        baselineMate = baseline.flatMap(_.mate),
                        baselineDepth = baseline.map(_.depth).filter(_ > 0)
                      )
                    )
              }
              .take(3)

          if hypothesisDriven.nonEmpty then hypothesisDriven
          else
            planScoring.topPlans.flatMap { pm =>
              if (pm.plan.color != sideToMove || pm.score < ScoreThreshold || !isPlanValidationCandidate(pm, planScoring)) None
              else
                val contract = themePlanContract(pm)
                val repMoves = representativeMoves(pos, pm, legalMoves).take(MaxMovesPerPlan)
                val novelMoves = repMoves.filterNot(topPvMoves.contains)
                val selectedMoves =
                  if novelMoves.nonEmpty then novelMoves.take(MaxMovesPerPlan)
                  else if pm.score >= HighConfidenceGhostScore && repMoves.nonEmpty then repMoves.take(1)
                  else Nil
                if selectedMoves.isEmpty then None
                else
                  val labeledPlanName =
                    contract.subplanId
                      .map(sp => s"${pm.plan.name} [subplan:${sp.id}]")
                      .getOrElse(pm.plan.name)
                  Some(
                    ProbeRequest(
                      id = stableRequestId(pm.plan, fen),
                      fen = fen,
                      moves = selectedMoves,
                      depth = DefaultDepth,
                      planId = Some(pm.plan.id.toString),
                      planName = Some(labeledPlanName),
                      planScore = Some(pm.score),
                      purpose = Some(contract.purpose),
                      objective = contract.objective,
                      requiredSignals = contract.requiredSignals,
                      horizon = contract.horizon,
                      maxCpLoss = Some(110),
                      baselineMove = baseline.flatMap(_.moves.headOption),
                      baselineEvalCp = baseline.map(_.evalCp),
                      baselineMate = baseline.flatMap(_.mate),
                      baselineDepth = baseline.map(_.depth).filter(_ > 0)
                    )
                  )
            }.take(3)

        // 2. Null-Move (Prophylaxis) Probes: Generate a "What if I pass?" FEN to find true threats.
        // We only do this if there's tension or the baseline eval is somewhat balanced/threatening.
        val nullMoveProbes = if (ctx.tacticalThreatToUs || ctx.tacticalThreatToThem || (baseline.exists(_.evalCp.abs < 200))) {
          val nullFen = lila.llm.analysis.FENUtils.passTurn(fen)
          if (nullFen != fen) {
            Some(ProbeRequest(
              id = s"null_move_${Integer.toHexString(fen.hashCode)}",
              fen = nullFen,
              moves = Nil, // We want the engine's best move FOR THE OPPONENT
              depth = DefaultDepth,
              purpose = Some("NullMoveThreat"),
              planName = Some("Restriction/Prophylaxis Null-Move Check"),
              objective = objectiveForPurpose("NullMoveThreat"),
              requiredSignals = requiredSignalsForPurpose("NullMoveThreat"),
              horizon = horizonForPurpose("NullMoveThreat"),
              baselineMove = baseline.flatMap(_.moves.headOption),
              baselineEvalCp = baseline.map(_.evalCp),
              baselineMate = baseline.flatMap(_.mate),
              baselineDepth = baseline.map(_.depth).filter(_ > 0)
            ))
          } else None
        } else None

        applyPurposeBudget(
          (planProbes ++ evidenceProbes ++ pvRepairProbes ++ playedMoveProbe ++ tensionProbes ++ nullMoveProbes.toList)
            .distinctBy(r => s"${r.fen}|${r.moves.mkString(",")}")
        ).take(MaxProbeRequests)
    }.getOrElse(Nil)
  }

  private def applyPurposeBudget(requests: List[ProbeRequest]): List[ProbeRequest] =
    val counters = scala.collection.mutable.Map.empty[String, Int].withDefaultValue(0)
    requests.filter { req =>
      val key = req.purpose.getOrElse("none")
      val limit = PurposeBudget.getOrElse(key, 2)
      val current = counters(key)
      if current >= limit then false
      else
        counters(key) = current + 1
        true
      }.map(bindProbeRequest)

  private def bindProbeRequest(req: ProbeRequest): ProbeRequest =
    val candidateMove =
      req.candidateMove
        .orElse(req.moves match
          case move :: Nil => Some(move)
          case _           => None
        )
        .map(_.trim)
        .filter(_.nonEmpty)
    val depthFloor =
      req.depthFloor
        .orElse(Option.when(req.depth > 0)(req.depth))
        .filter(_ > 0)
    val variationHash =
      req.variationHash
        .orElse(Some(probeVariationHash(req, candidateMove)))
        .filter(_.trim.nonEmpty)
    val engineConfigFingerprint =
      req.engineConfigFingerprint
        .orElse(Some(defaultEngineFingerprint(req)))
        .filter(_.trim.nonEmpty)
    req.copy(
      candidateMove = candidateMove,
      depthFloor = depthFloor,
      variationHash = variationHash,
      engineConfigFingerprint = engineConfigFingerprint
    )

  private def probeVariationHash(
      req: ProbeRequest,
      candidateMove: Option[String]
  ): String =
    List(
      Option(req.fen).getOrElse("").trim,
      candidateMove.getOrElse(""),
      req.moves.distinct.sorted.mkString(","),
      req.purpose.getOrElse("").trim,
      req.objective.getOrElse("").trim,
      req.seedId.getOrElse("").trim,
      req.requiredSignals.sorted.mkString(",")
    ).mkString("|")

  private def defaultEngineFingerprint(req: ProbeRequest): String =
    s"wasm_stockfish:depth=${req.depth}:multipv=${req.multiPv.getOrElse(1)}"

  private case class ThemePlanContract(
      purpose: String,
      subplanId: Option[SubplanId],
      objective: Option[String],
      requiredSignals: List[String],
      horizon: Option[String]
  )

  private def themePlanContract(pm: PlanMatch): ThemePlanContract =
    val taggedSubplan =
      pm.supports
        .collectFirst { case s if s.startsWith("subplan:") => s.stripPrefix("subplan:").trim }
        .filter(_.nonEmpty)
        .flatMap(SubplanId.fromId)
    val inferredSubplan =
      taggedSubplan
        .orElse(ThemeResolver.subplanFromPlanId(pm.plan.id.toString))
        .orElse(ThemeResolver.subplanFromPlanName(pm.plan.name))
    themePlanContractForSubplan(inferredSubplan)

  private def themePlanContract(h: PlanHypothesis): ThemePlanContract =
    themePlanContractForSubplan(hypothesisSubplanId(h))

  private def themePlanContractForSubplan(
      inferredSubplan: Option[SubplanId]
  ): ThemePlanContract =
    inferredSubplan.flatMap(SubplanCatalog.specs.get) match
      case Some(spec) =>
        val purpose = ThemePlanProbePurpose.contractForSubplan(inferredSubplan)
        val required =
          inferredSubplan
            .map(sp => broadenThemeContractForDefaultSubplan(sp, spec.requiredSignals))
            .getOrElse(spec.requiredSignals.distinct)
        ThemePlanContract(
          purpose = purpose.purpose,
          subplanId = inferredSubplan,
          objective = Some(purpose.objective),
          requiredSignals = ThemePlanProbePurpose.mergedSignals(required, purpose.purpose),
          horizon = Some(purpose.horizon)
        )
      case None =>
        val purpose = ThemePlanProbePurpose.contractForSubplan(inferredSubplan)
        ThemePlanContract(
          purpose = purpose.purpose,
          subplanId = inferredSubplan,
          objective = Some(purpose.objective),
          requiredSignals = purpose.requiredSignals,
          horizon = Some(purpose.horizon)
        )

  private def hypothesisKey(h: PlanHypothesis): String =
    val sub = hypothesisSubplanId(h).map(_.id).getOrElse("")
    s"${h.planId.trim.toLowerCase}|${sub.toLowerCase}"

  private def hypothesisThemeId(h: PlanHypothesis): ThemeL1 =
    ThemeResolver.fromHypothesis(h)

  private def hypothesisSubplanId(h: PlanHypothesis): Option[SubplanId] =
    ThemeResolver.subplanFromHypothesis(h).orElse(h.subplanId.flatMap(SubplanId.fromId))

  private val SignalPriority = List("replyPvs", "keyMotifs", "l1Delta", "futureSnapshot")

  private def broadenThemeContractForDefaultSubplan(
      subplan: SubplanId,
      baseSignals: List[String]
  ): List[String] =
    val isDefault = ThemeResolver.defaultSubplanForTheme(subplan.theme).contains(subplan)
    val merged =
      if isDefault then
        val themeSignals =
          SubplanCatalog
            .byTheme(subplan.theme)
            .flatMap(_._2.requiredSignals)
            .toSet
        baseSignals.toSet ++ themeSignals
      else baseSignals.toSet
    SignalPriority.filter(merged.contains) ++ (merged -- SignalPriority.toSet).toList.sorted

  private def requiredSignalsForPurpose(purpose: String): List[String] =
    ThemePlanProbePurpose.contractForPurpose(purpose).map(_.requiredSignals).getOrElse(
      purpose match
        case "latent_plan_refutation" => List("replyPvs", "keyMotifs", "l1Delta", "futureSnapshot")
        case "latent_plan_immediate"  => List("replyPvs", "l1Delta")
        case "free_tempo_branches"    => List("replyPvs", "futureSnapshot")
        case "reply_multipv" | "defense_reply_multipv" | "convert_reply_multipv" |
            "recapture_branches" | "keep_tension_branches" =>
          List("replyPvs")
        case "played_move_counterfactual" =>
          List("replyPvs", "l1Delta")
        case "NullMoveThreat" =>
          List("replyPvs", "keyMotifs", "l1Delta")
        case _ =>
          Nil
    )

  private def objectiveForPurpose(purpose: String): Option[String] =
    ThemePlanProbePurpose.contractForPurpose(purpose).map(_.objective).orElse(
      purpose match
        case "latent_plan_refutation" => Some("refute_plan")
        case "latent_plan_immediate"  => Some("validate_immediate_viability")
        case "free_tempo_branches"    => Some("validate_latent_plan")
        case "reply_multipv"          => Some("compare_reply_branches")
        case "defense_reply_multipv"  => Some("validate_defensive_resources")
        case "convert_reply_multipv"  => Some("validate_conversion_route")
        case "recapture_branches"     => Some("compare_recapture_structures")
        case "keep_tension_branches"  => Some("compare_tension_branches")
        case "played_move_counterfactual" => Some("counterfactual_probe")
        case "NullMoveThreat"         => Some("validate_restriction_prophylaxis")
        case _                        => None
    )

  private def horizonForPurpose(purpose: String): Option[String] =
    ThemePlanProbePurpose.contractForPurpose(purpose).map(_.horizon).orElse(
      purpose match
        case "latent_plan_refutation" | "free_tempo_branches" => Some("long")
        case "latent_plan_immediate" | "convert_reply_multipv" => Some("medium")
        case "reply_multipv" | "defense_reply_multipv" | "recapture_branches" | "keep_tension_branches" =>
          Some("short")
        case "played_move_counterfactual" | "NullMoveThreat" => Some("short")
        case _ => None
    )

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
    val slug = slugify(plan.name)
    s"${plan.id}_${slug}_${Integer.toHexString(fen.hashCode)}"

  private def stableRequestId(h: PlanHypothesis, fen: String): String =
    val subplanTag = hypothesisSubplanId(h).map(_.id).getOrElse("none")
    val slug = slugify(s"${h.planId}_${h.planName}_$subplanTag")
    s"hyp_${slug}_${Integer.toHexString(fen.hashCode)}"

  private def slugify(raw: String): String =
    raw
      .toLowerCase
      .map(c => if c.isLetterOrDigit then c else '_')
      .mkString
      .replaceAll("_+", "_")
      .stripPrefix("_")
      .stripSuffix("_")

  private def isPlanValidationCandidate(pm: PlanMatch, planScoring: PlanScoringResult): Boolean =
    val strongPlanScore = pm.score >= HighConfidenceGhostScore
    val stableGlobalScore =
      if planScoring.confidence <= 0.0 then true
      else planScoring.confidence >= HighConfidenceGlobalScore
    val hasSupport = pm.evidence.nonEmpty || pm.supports.nonEmpty
    val manageableUncertainty = pm.blockers.size <= 2 && pm.missingPrereqs.size <= 1
    val nearTopScore =
      planScoring.topPlans.headOption.forall(top => pm.score >= (top.score - 0.14))
    hasSupport && manageableUncertainty && (strongPlanScore || stableGlobalScore || nearTopScore)

  private def isPlanValidationCandidate(h: PlanHypothesis): Boolean =
    val hasSupport = h.evidenceSources.nonEmpty || h.executionSteps.nonEmpty || h.preconditions.nonEmpty
    val manageableUncertainty = h.failureModes.size <= 3
    val viable = h.viability.score >= 0.45
    hasSupport && manageableUncertainty && viable

  private def representativeMoves(
      pos: Position,
      h: PlanHypothesis,
      legalMoves: List[Move],
      planScoring: PlanScoringResult
  ): List[String] =
    val fromRulePlans =
      planScoring.topPlans
        .filter(pm => hypothesisMatchesPlanMatch(h, pm))
        .flatMap(pm => representativeMoves(pos, pm, legalMoves))
        .distinct
    val fallback =
      movesFromHypothesis(h, legalMoves)
        .map(_.toUci.uci)
        .distinct
        .filterNot(fromRulePlans.contains)
    (fromRulePlans ++ fallback).distinct.take(MaxMovesPerPlan)

  private def hypothesisMatchesPlanMatch(h: PlanHypothesis, pm: PlanMatch): Boolean =
    val hypPlanId = h.planId.trim.toLowerCase
    val pmPlanId = pm.plan.id.toString.trim.toLowerCase
    val hypTheme = hypothesisThemeId(h)
    val pmTheme = ThemeResolver.fromPlanId(pm.plan.id.toString)
    val hypSubplan = hypothesisSubplanId(h).map(_.id)
    val pmSubplan = themePlanContract(pm).subplanId.map(_.id)
    hypPlanId == pmPlanId ||
      (hypSubplan.nonEmpty && hypSubplan == pmSubplan) ||
      (hypTheme != ThemeL1.Unknown && hypTheme == pmTheme)

  private def movesFromHypothesis(
      h: PlanHypothesis,
      legalMoves: List[Move]
  ): List[Move] =
    val subplanMoves = hypothesisSubplanId(h).toList.flatMap(sp => movesFromSubplan(sp, legalMoves))
    val themeMoves = movesFromTheme(hypothesisThemeId(h), legalMoves)
    (subplanMoves ++ themeMoves).distinct

  private def movesFromSubplan(
      subplan: SubplanId,
      legalMoves: List[Move]
  ): List[Move] =
    subplan match
      case SubplanId.OpeningDevelopment =>
        legalMoves.filter(mv =>
          (!mv.captures && (mv.piece.role == Knight || mv.piece.role == Bishop || mv.piece.role == King)) ||
            (mv.piece.role == Pawn && (mv.dest.file == File.C || mv.dest.file == File.D || mv.dest.file == File.E || mv.dest.file == File.F))
        )
      case SubplanId.ProphylaxisRestraint | SubplanId.BreakPrevention | SubplanId.KeySquareDenial |
          SubplanId.MobilitySuppression =>
        legalMoves.filter(mv => !mv.captures && mv.piece.role != Pawn)
      case SubplanId.OutpostEntrenchment | SubplanId.WorstPieceImprovement =>
        legalMoves.filter(mv =>
          !mv.captures && (mv.piece.role == Knight || mv.piece.role == Bishop)
        )
      case SubplanId.BishopReanchor =>
        legalMoves.filter(mv => mv.piece.role == Bishop && !mv.captures)
      case SubplanId.RookFileTransfer | SubplanId.RookLiftScaffold =>
        legalMoves.filter(mv => mv.piece.role == Rook && !mv.captures)
      case SubplanId.OpenFilePressure =>
        legalMoves.filter(mv =>
          (mv.piece.role == Rook || mv.piece.role == Queen) &&
            (!mv.captures || mv.dest.file == mv.orig.file)
        )
      case SubplanId.FlankClamp | SubplanId.RookPawnMarch | SubplanId.HookCreation | SubplanId.WingBreakTiming |
          SubplanId.MinorityAttackFixation =>
        legalMoves.filter(mv =>
          mv.piece.role == Pawn &&
            !mv.dest.file.isCentral &&
            (!mv.captures || mv.dest.file.isKingside || mv.dest.file.isQueenside)
        )
      case SubplanId.CentralSpaceBind | SubplanId.CentralBreakTiming | SubplanId.TensionMaintenance |
          SubplanId.IQPInducement =>
        legalMoves.filter(mv =>
          mv.piece.role == Pawn &&
            (mv.orig.file.isCentral || mv.dest.file.isCentral || mv.captures)
        )
      case SubplanId.StaticWeaknessFixation | SubplanId.BackwardPawnTargeting =>
        legalMoves.filter(mv => mv.captures || (mv.piece.role == Pawn && !mv.dest.file.isCentral))
      case SubplanId.SimplificationWindow | SubplanId.DefenderTrade | SubplanId.QueenTradeShield |
          SubplanId.SimplificationConversion | SubplanId.InvasionTransition | SubplanId.OppositeBishopsConversion |
          SubplanId.BadPieceLiquidation =>
        legalMoves.filter(mv => mv.captures || mv.piece.role == Rook)
      case SubplanId.PasserConversion | SubplanId.PassedPawnManufacture =>
        legalMoves.filter(mv => mv.piece.role == Pawn && !mv.captures)
      case SubplanId.ForcingTacticalShot | SubplanId.DefenderOverload | SubplanId.ClearanceBreak |
          SubplanId.BatteryPressure =>
        legalMoves.filter(mv => mv.captures || mv.after.check.yes)

  private def movesFromTheme(theme: ThemeL1, legalMoves: List[Move]): List[Move] =
    theme match
      case ThemeL1.OpeningPrinciples =>
        legalMoves.filter(mv =>
          (!mv.captures && (mv.piece.role == Knight || mv.piece.role == Bishop || mv.piece.role == King)) ||
            (mv.piece.role == Pawn && (mv.dest.file == File.C || mv.dest.file == File.D || mv.dest.file == File.E || mv.dest.file == File.F))
        )
      case ThemeL1.RestrictionProphylaxis =>
        legalMoves.filter(mv => !mv.captures && mv.piece.role != Pawn)
      case ThemeL1.PieceRedeployment =>
        legalMoves.filter(mv =>
          !mv.captures && (mv.piece.role == Knight || mv.piece.role == Bishop || mv.piece.role == Rook)
        )
      case ThemeL1.SpaceClamp =>
        legalMoves.filter(mv => mv.piece.role == Pawn && !mv.captures)
      case ThemeL1.WeaknessFixation =>
        legalMoves.filter(mv => mv.captures || (mv.piece.role == Pawn && !mv.dest.file.isCentral))
      case ThemeL1.PawnBreakPreparation =>
        legalMoves.filter(mv => mv.piece.role == Pawn && (mv.orig.file.isCentral || mv.dest.file.isCentral))
      case ThemeL1.FavorableExchange =>
        legalMoves.filter(_.captures)
      case ThemeL1.FlankInfrastructure =>
        legalMoves.filter(mv =>
          (mv.piece.role == Rook && !mv.captures) ||
            (mv.piece.role == Pawn && (mv.dest.file.isKingside || mv.dest.file.isQueenside))
        )
      case ThemeL1.AdvantageTransformation =>
        legalMoves.filter(mv => mv.captures || mv.piece.role == Pawn)
      case ThemeL1.ImmediateTacticalGain =>
        legalMoves.filter(mv => mv.captures || mv.after.check.yes)
      case ThemeL1.Unknown =>
        legalMoves.filter(mv => !mv.captures)

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

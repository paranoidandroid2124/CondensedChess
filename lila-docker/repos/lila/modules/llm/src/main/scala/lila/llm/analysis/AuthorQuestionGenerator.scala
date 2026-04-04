package lila.llm.analysis

import chess.*
import chess.format.Fen
import lila.llm.analysis.L3.TensionPolicy
import lila.llm.model.*
import lila.llm.model.authoring.*

object AuthorQuestionGenerator:

  private val MaxSeededQuestionKinds = 5

  def generate(
    data: ExtendedAnalysisData,
    ctx: IntegratedContext,
    candidates: List[CandidateInfo],
    playedSan: Option[String]
  ): List[AuthorQuestion] =
    val fen = data.fen
    val playedUciOpt = data.prevMove
    if playedUciOpt.isEmpty then return Nil

    val us = if ctx.isWhiteToMove then "White" else "Black"
    val them = if us == "White" then "Black" else "White"

    val playedUci = playedUciOpt.get
    val playedSanStr = playedSan.getOrElse(NarrativeUtils.uciToSanOrFormat(fen, playedUci))
    val topCandidate =
      candidates.headOption.map(_.move)
        .orElse(data.alternatives.headOption.flatMap(_.moves.headOption.map(NarrativeUtils.uciToSanOrFormat(fen, _))))
    val topCandidateUci =
      candidates.headOption.flatMap(_.uci)
        .orElse(data.alternatives.headOption.flatMap(_.moves.headOption))
    val alternativeCandidate =
      candidates.iterator
        .map(_.move)
        .find(candidate => candidate.nonEmpty && candidate != playedSanStr)
        .orElse {
          data.alternatives.iterator
            .flatMap(_.moves.headOption)
            .map(NarrativeUtils.uciToSanOrFormat(fen, _))
            .find(candidate => candidate.nonEmpty && candidate != playedSanStr)
        }
    val playedCandidate =
      candidates.find(_.uci.contains(playedUci)).orElse(candidates.find(_.move == playedSanStr))

    val posOpt = Fen.read(chess.variant.Standard, Fen.Full(fen))

    rankQuestions(
      List(
      buildTrapQuestion(data.ply, fen, playedUci, playedSanStr),
      buildDefensiveTaskQuestion(ctx, us, fen, playedUci),
      buildConversionPlanQuestion(ctx, us, fen, playedUci, playedSanStr),
      buildTensionDecisionQuestion(posOpt, ctx, fen, playedUci, playedSanStr, topCandidate),
      buildPlanClashQuestion(posOpt, ctx, us, fen, playedUci, topCandidateUci, topCandidate),
      buildDecisionComparisonQuestion(playedSanStr, alternativeCandidate, playedCandidate, fen, playedUci),
      buildQuietIntentQuestion(data, ctx, playedSanStr, alternativeCandidate, fen, playedUci),
      buildPositionProbeQuestion(ctx, us, posOpt, fen, playedUci),
      buildTimingWindowQuestion(ctx, us, playedSanStr, fen, playedUci),
      buildMoveChangeQuestion(data, playedSanStr, playedCandidate, fen, playedUci),
      buildRaceWindowQuestion(ctx, us, them, playedSanStr, fen, playedUci),
      buildRecaptureStructureQuestion(them, fen, playedUci, playedSanStr)
      ).flatten
    )

  private def rankQuestions(questions: List[AuthorQuestion]): List[AuthorQuestion] =
    questions
      .sortBy(question => (question.priority, question.kind.toString, question.id))
      .foldLeft(List.empty[AuthorQuestion]) { (acc, question) =>
        if acc.exists(_.kind == question.kind) then acc
        else acc :+ question
      }
      .take(MaxSeededQuestionKinds)

  private def buildTrapQuestion(
    ply: Int,
    fen: String,
    playedUci: String,
    playedSan: String
  ): Option[AuthorQuestion] =
    TruthGate.detect(fen, playedUci, ply).map { theme =>
      val reasonText = s"The move $playedSan triggers the confirmed ${theme.name} sequence."

      AuthorQuestion(
        id = s"tactical_${Integer.toHexString((fen + playedUci).hashCode)}",
        kind = AuthorQuestionKind.WhatMustBeStopped,
        priority = 1,
        question = s"What must be stopped right now after $playedSan?",
        why = Some(reasonText),
        anchors = List(theme.name),
        confidence = ConfidenceLevel.Engine,
        evidencePurposes = List("reply_multipv")
      )
    }

  private def buildDefensiveTaskQuestion(
    ctx: IntegratedContext,
    us: String,
    fen: String,
    playedUci: String
  ): Option[AuthorQuestion] =
    val topThreat =
      ctx.threatsToUs
        .flatMap(_.threats.sortBy(t => (t.turnsToImpact, -t.lossIfIgnoredCp)).headOption)

    val seriousThreat = topThreat.filter(_.severity != lila.llm.analysis.L3.ThreatSeverity.Low)
    if seriousThreat.isEmpty then None
    else
      def toSanMaybe(move: String): String =
        val isUci = move.matches("^[a-h][1-8][a-h][1-8][qrbn]?$")
        if isUci then NarrativeUtils.uciToSanOrFormat(fen, move) else move

      val t = seriousThreat.get
      val threatHint = t.attackSquares.headOption.map(sq => s" (watch $sq)").getOrElse("")
      val defenseHint = t.bestDefense.filter(_.nonEmpty).map(toSanMaybe)

      val q =
        defenseHint match
          case Some(d) => s"What is the defensive task here — can $us meet the threat with $d$threatHint?"
          case None    => s"What is the defensive task here, and how should $us meet the opponent’s threat$threatHint?"

      val why = Some(
        s"There is immediate pressure: ${defensiveTaskConsequence(t.kind.toString, t.turnsToImpact)}"
      )

      Some(
        AuthorQuestion(
          id = s"defense_${Integer.toHexString((fen + playedUci).hashCode)}",
          kind = AuthorQuestionKind.WhatMustBeStopped,
          priority = 1,
          question = q,
          why = why,
          anchors = List("defense", "threat"),
          evidencePurposes = List("defense_reply_multipv")
        )
      )

  private def defensiveTaskConsequence(kind: String, turnsToImpact: Int): String =
    val horizon =
      if turnsToImpact <= 1 then "on the next move"
      else if turnsToImpact <= 2 then "within the next few moves"
      else "in a short sequence"
    kind.toLowerCase match
      case "mate" =>
        s"if ignored, king safety collapses $horizon."
      case "material" =>
        s"if ignored, material can be lost $horizon."
      case "positional" =>
        s"if ignored, the position can become passive and hard to untangle $horizon."
      case _ =>
        s"if ignored, the opponent can seize the initiative $horizon."

  private def buildConversionPlanQuestion(
    ctx: IntegratedContext,
    us: String,
    fen: String,
    playedUci: String,
    playedSan: String
  ): Option[AuthorQuestion] =
    val isConvertMode = ctx.classification.exists(_.taskMode.isConvertMode)
    val isSimplifyWindow = ctx.classification.exists(_.simplifyBias.shouldSimplify)
    val advantage = ctx.evalCp
    val isClearlyBetter = advantage >= 200

    if !isConvertMode && !(isSimplifyWindow && isClearlyBetter) then None
    else
      val why =
        if (isSimplifyWindow)
          Some("This looks like a conversion window: trading down can reduce counterplay and make the advantage easier to realize.")
        else Some("With a clear advantage, the key is not to rush tactics but to convert: restrict counterplay and simplify when it helps.")

      Some(
        AuthorQuestion(
          id = s"convert_${Integer.toHexString((fen + playedUci).hashCode)}",
          kind = AuthorQuestionKind.WhyNow,
          priority = 2,
          question = s"Why is $playedSan the right moment to convert the advantage for $us?",
          why = why,
          anchors = List("conversion", "simplify"),
          evidencePurposes = List("convert_reply_multipv")
        )
      )

  private def buildTensionDecisionQuestion(
    posOpt: Option[Position],
    ctx: IntegratedContext,
    fen: String,
    playedUci: String,
    playedSan: String,
    topCandidate: Option[String]
  ): Option[AuthorQuestion] =
    val shouldMaintain = ctx.pawnAnalysis.exists(_.tensionPolicy == TensionPolicy.Maintain)
    if !shouldMaintain then None
    else
      val isRelease =
        posOpt.exists(pos => MovePredicates.isTensionReleaseCandidate(pos, playedUci))

      Option.when(isRelease) {
        val alt =
          topCandidate
            .filter(a => a.nonEmpty && a != playedSan)
            .map(a => s" or keep it with $a")
            .getOrElse("")

        AuthorQuestion(
          id = s"tension_${Integer.toHexString((fen + playedUci).hashCode)}",
          kind = AuthorQuestionKind.WhyThis,
          priority = 1,
          question = s"Why choose $playedSan here instead of keeping the tension$alt?",
          why = Some("This choice often decides whether the other side can simplify into a comfortable structure."),
          anchors = List("central tension"),
          evidencePurposes = List("keep_tension_branches", "reply_multipv")
        )
      }

  private def buildPlanClashQuestion(
    posOpt: Option[Position],
    ctx: IntegratedContext,
    us: String,
    fen: String,
    playedUci: String,
    bestUciOpt: Option[String],
    bestSanOpt: Option[String]
  ): Option[AuthorQuestion] =
    val shouldMaintain = ctx.pawnAnalysis.exists(_.tensionPolicy == TensionPolicy.Maintain)
    val isRelease = posOpt.exists(pos => MovePredicates.isTensionReleaseCandidate(pos, playedUci))
    if !shouldMaintain || !isRelease then None
    else
      val bestSan = bestSanOpt.getOrElse("")
      val bestUci = bestUciOpt.getOrElse("")
      val isCastle = bestSan == "O-O" || bestSan == "O-O-O"
      if bestUci.isEmpty || !isCastle then None
      else
        val afterBestFen = NarrativeUtils.uciListToFen(fen, List(bestUci))
        val (capUciOpt, pushUciOpt) = MovePredicates.planClashOptions(afterBestFen)

        (for
          capUci <- capUciOpt
          pushUci <- pushUciOpt
        yield
          val capSan = NarrativeUtils.uciToSanOrFormat(afterBestFen, capUci)
          val pushSan = NarrativeUtils.uciToSanOrFormat(afterBestFen, pushUci)

          AuthorQuestion(
            id = s"planclash_${Integer.toHexString((fen + bestUci).hashCode)}",
            kind = AuthorQuestionKind.WhosePlanIsFaster,
            priority = 2,
            question = s"If $us keeps the tension with $bestSan, whose plan is faster: ...$capSan or ...$pushSan?",
            why = Some("Keeping the tension is not about waiting: it forces the opponent to reveal their plan before you clarify the center."),
            anchors = List("central tension", capSan, pushSan),
            evidencePurposes = List("keep_tension_branches")
          )
        )

  private def buildDecisionComparisonQuestion(
    playedSan: String,
    alternativeCandidate: Option[String],
    playedCandidate: Option[CandidateInfo],
    fen: String,
    playedUci: String
  ): Option[AuthorQuestion] =
    val alternative = alternativeCandidate.filter(candidate => candidate.nonEmpty && candidate != playedSan)
    val playedSignal =
      playedCandidate.exists(candidate =>
        candidate.tacticalAlert.exists(_.trim.nonEmpty) ||
          candidate.downstreamTactic.exists(_.trim.nonEmpty) ||
          candidate.whyNot.exists(_.trim.nonEmpty) ||
          candidate.lineSanMoves.nonEmpty
      )

    Option.when(alternative.nonEmpty || playedSignal) {
      val question =
        alternative match
          case Some(other) => s"Why choose $playedSan here instead of $other?"
          case None        => s"Why is $playedSan the move that fits the position here?"
      val why =
        playedCandidate.flatMap(_.downstreamTactic).filter(_.trim.nonEmpty)
          .map(tactic => s"The move appears to be tied to the immediate idea of $tactic.")
          .orElse {
            alternative.map(other =>
              s"The decision is not automatic here because $other also looks natural at first glance."
            )
          }

      AuthorQuestion(
        id = s"compare_${Integer.toHexString((fen + playedUci).hashCode)}",
        kind = AuthorQuestionKind.WhyThis,
        priority = 2,
        question = question,
        why = why,
        anchors = List("decision compare"),
        evidencePurposes = List("reply_multipv")
      )
    }

  private def buildTimingWindowQuestion(
    ctx: IntegratedContext,
    us: String,
    playedSan: String,
    fen: String,
    playedUci: String
  ): Option[AuthorQuestion] =
    val timingPressure =
      ctx.maxThreatLossToUs >= 120 ||
        ctx.maxThreatLossToThem >= 120 ||
        ctx.attackingOpportunityAtRisk ||
        ctx.simplificationReliefPossible

    Option.when(timingPressure) {
      val why =
        if ctx.maxThreatLossToUs >= 120 then
          Some("The position is already time-sensitive because the opponent's threat cannot be ignored for long.")
        else if ctx.maxThreatLossToThem >= 120 then
          Some("The position is time-sensitive because the attacking window may close if the move order drifts.")
        else
          Some(s"The timing matters because both sides' plans are becoming concrete, and $us cannot drift here.")

      AuthorQuestion(
        id = s"timing_${Integer.toHexString((fen + playedUci).hashCode)}",
        kind = AuthorQuestionKind.WhyNow,
        priority = 3,
        question = s"Why does $playedSan have to be timed right now?",
        why = why,
        anchors = List("timing"),
        evidencePurposes = List("reply_multipv")
      )
    }

  private def buildQuietIntentQuestion(
    data: ExtendedAnalysisData,
    ctx: IntegratedContext,
    playedSan: String,
    alternativeCandidate: Option[String],
    fen: String,
    playedUci: String
  ): Option[AuthorQuestion] =
    val quietIntent =
      ctx.planAlignment.flatMap(_.narrativeIntent).filter(_.trim.nonEmpty)
        .orElse(data.planHypotheses.headOption.map(_.planName).filter(_.trim.nonEmpty))
        .orElse(data.plans.headOption.map(_.plan.name).filter(_.trim.nonEmpty))
    val underImmediateThreat =
      ctx.maxThreatLossToUs >= 120 || ctx.maxThreatLossToThem >= 120

    Option.when(quietIntent.nonEmpty && !underImmediateThreat) {
      val alternativeText = alternativeCandidate.filter(_.nonEmpty).map(other => s" better than $other").getOrElse("")
      val why =
        quietIntent.map(intent =>
          s"The move looks like a quiet choice about $intent rather than a forced tactical answer."
        )

      AuthorQuestion(
        id = s"quiet_${Integer.toHexString((fen + playedUci).hashCode)}",
        kind = AuthorQuestionKind.WhyThis,
        priority = 4,
        question = s"Why does $playedSan fit the position$alternativeText?",
        why = why,
        anchors = List("quiet intent"),
        evidencePurposes = List("reply_multipv")
      )
    }

  private def buildPositionProbeQuestion(
    ctx: IntegratedContext,
    us: String,
    posOpt: Option[Position],
    fen: String,
    playedUci: String
  ): Option[AuthorQuestion] =
    PlayerFacingTruthModePolicy.positionProbeQuestionSeed(ctx, posOpt).map { seed =>
      AuthorQuestion(
        id = s"probe_${Integer.toHexString((fen + playedUci).hashCode)}",
        kind = AuthorQuestionKind.WhatMattersHere,
        priority = 1,
        question = s"What matters most here for $us around ${seed.questionFocusText}?",
        why = Some(seed.why),
        anchors = seed.anchors,
        evidencePurposes = List("reply_multipv")
      )
    }

  private def buildMoveChangeQuestion(
    data: ExtendedAnalysisData,
    playedSan: String,
    playedCandidate: Option[CandidateInfo],
    fen: String,
    playedUci: String
  ): Option[AuthorQuestion] =
    val changeSignal =
      data.counterfactual.nonEmpty ||
        data.preventedPlans.nonEmpty ||
        data.endgameTransition.nonEmpty ||
        playedCandidate.exists(candidate =>
          candidate.downstreamTactic.exists(_.trim.nonEmpty) ||
            candidate.lineSanMoves.nonEmpty ||
            candidate.facts.nonEmpty
        )

    Option.when(changeSignal) {
      val why =
        data.counterfactual.map(_ => "The move appears to change the practical balance compared with the missed alternative.")
          .orElse(
            Option.when(data.preventedPlans.nonEmpty)(
              "The move appears to change what the opponent can do next."
            )
          )
          .orElse(
            Option.when(data.endgameTransition.nonEmpty)(
              "The move appears to change which endgame task now defines the position."
            )
          )
          .orElse(
            playedCandidate.flatMap(_.downstreamTactic).filter(_.trim.nonEmpty)
              .map(tactic => s"The move seems to reshape the position around $tactic.")
          )

      AuthorQuestion(
        id = s"change_${Integer.toHexString((fen + playedUci).hashCode)}",
        kind = AuthorQuestionKind.WhatChanged,
        priority = 3,
        question = s"What changed in the position after $playedSan?",
        why = why,
        anchors = List("move change"),
        evidencePurposes = List("reply_multipv")
      )
    }

  private def buildRaceWindowQuestion(
    ctx: IntegratedContext,
    us: String,
    them: String,
    playedSan: String,
    fen: String,
    playedUci: String
  ): Option[AuthorQuestion] =
    val directRace =
      ctx.threatsToUs.exists(_.threats.exists(threat => threat.turnsToImpact <= 3 && threat.lossIfIgnoredCp >= 80)) &&
        ctx.threatsToThem.exists(_.threats.exists(threat => threat.turnsToImpact <= 3 && threat.lossIfIgnoredCp >= 80))
    val activeWindowRace =
      ctx.maxThreatLossToUs >= 60 &&
        ctx.maxThreatLossToThem >= 60 &&
        (
          ctx.attackingOpportunityAtRisk ||
            ctx.simplificationReliefPossible ||
            (
              ctx.threatsToUs.exists(ta => ta.immediateThreat || ta.strategicThreat) &&
                ctx.threatsToThem.exists(ta => ta.immediateThreat || ta.strategicThreat)
            )
        )
    val raceAlive = directRace || activeWindowRace

    Option.when(raceAlive) {
      AuthorQuestion(
        id = s"race_${Integer.toHexString((fen + playedUci).hashCode)}",
        kind = AuthorQuestionKind.WhosePlanIsFaster,
        priority = 3,
        question = s"After $playedSan, whose plan is faster: $us's or $them's?",
        why = Some("Both sides have active ideas, so the race is likely to be decided by timing."),
        anchors = List("race", "timing"),
        evidencePurposes = List("reply_multipv")
      )
    }

  private def buildRecaptureStructureQuestion(
    them: String,
    fen: String,
    playedUci: String,
    playedSan: String
  ): Option[AuthorQuestion] =
    val playedMove = chess.format.Uci(playedUci).collect { case m: chess.format.Uci.Move => m }
    playedMove.flatMap { m =>
      val afterFen = NarrativeUtils.uciListToFen(fen, List(playedUci))
      Fen.read(chess.variant.Standard, Fen.Full(afterFen)).flatMap { afterPos =>
        val recapsUci =
          afterPos.legalMoves.toList
            .filter(mv => mv.captures && mv.dest == m.dest)
            .map(_.toUci.uci)
            .distinct

        val recapsSan =
          recapsUci
            .map(uci => NarrativeUtils.uciToSanOrFormat(afterFen, uci))
            .filter(_.nonEmpty)
            .distinct
            .take(3)

        Option.when(recapsSan.size >= 2) {
          val list = renderEllipsisList(recapsSan, them)
          AuthorQuestion(
            id = s"recapture_${Integer.toHexString((fen + playedUci).hashCode)}",
            kind = AuthorQuestionKind.WhatChanged,
            priority = 3,
            question = s"After $playedSan, what changes depending on how $them recaptures — $list?",
            why = Some("Different recaptures lead to different pawn structures and piece placements."),
            anchors = List("recapture choice", "structure"),
            evidencePurposes = List("recapture_branches")
          )
        }
      }
    }

  private def renderEllipsisList(sans: List[String], side: String): String =
    val rendered =
      if side.equalsIgnoreCase("black") then sans.map(s => if s.startsWith("...") then s else s"...$s")
      else sans.map(s => s.stripPrefix("..."))
    rendered match
      case Nil => ""
      case a :: Nil => a
      case a :: b :: Nil => s"$a or $b"
      case many => many.init.mkString(", ") + s", or ${many.last}"

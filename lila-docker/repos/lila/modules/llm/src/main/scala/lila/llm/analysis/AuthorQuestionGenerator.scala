package lila.llm.analysis

import chess.*
import chess.format.Fen
import lila.llm.analysis.L3.TensionPolicy
import lila.llm.model.*
import lila.llm.model.authoring.*

object AuthorQuestionGenerator:

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
    val topCandidate = candidates.headOption.map(_.move)
    val topCandidateUci = candidates.headOption.flatMap(_.uci)

    val posOpt = Fen.read(chess.variant.Standard, Fen.Full(fen))

    val core =
      List(
      buildTrapQuestion(data.ply, fen, playedUci, playedSanStr),
      buildDefensiveTaskQuestion(ctx, us, fen, playedUci),
      buildConversionPlanQuestion(ctx, us, fen, playedUci, playedSanStr),
      buildTensionDecisionQuestion(posOpt, ctx, us, fen, playedUci, playedSanStr, topCandidate),
      buildPlanClashQuestion(posOpt, ctx, us, them, fen, playedUci, topCandidateUci, topCandidate),
      buildRecaptureStructureQuestion(them, fen, playedUci, playedSanStr)
      ).flatten.sortBy(_.priority).take(3)

    // Latent plans should not dilute forcing tactical moments or urgent defense.
    val allowLatentPlans = core.forall(_.kind != AuthorQuestionKind.TacticalTest) && !ctx.underDefensivePressure

    val latent =
      if !allowLatentPlans then Nil
      else
        LatentPlanSeeder
          .seedForMoveAnnotation(fenBefore = fen, playedUci = playedUci, ply = data.ply, ctx = ctx)
          .take(1)
          .map { lp =>
            // Simple interpolation for the narrative template
            val seedReadable = lp.seedId.replace("_", " ").toLowerCase
            val qText = lp.narrative.template
              .replace("{us}", us)
              .replace("{them}", them)
              .replace("{seed}", seedReadable)
              
            // Fallback if template is empty
            val finalQ = if qText.trim.isEmpty then s"If $them is slow, what long-term plan becomes available for $us?" else qText

            AuthorQuestion(
              id = s"latent_${lp.seedId}_${Integer.toHexString((fen + playedUci).hashCode)}",
              kind = AuthorQuestionKind.LatentPlan,
              priority = 4,
              question = finalQ,
              why = Some("Idea-space candidate: must be validated via legal quiet branches before being stated as a book-style plan."),
              anchors = List(lp.seedId, "given time"),
              confidence = ConfidenceLevel.Heuristic,
              latentPlan = Some(lp)
            )
          }

    core ++ latent

  private def buildTrapQuestion(
    ply: Int,
    fen: String,
    playedUci: String,
    playedSan: String
  ): Option[AuthorQuestion] =
    TruthGate.detect(fen, playedUci, ply).map { theme =>
      val questionText = s"Does this fall into the ${theme.name}?"
      val reasonText = s"The move $playedSan triggers the confirmed ${theme.name} sequence."

      AuthorQuestion(
        id = s"tactical_${Integer.toHexString((fen + playedUci).hashCode)}",
        kind = AuthorQuestionKind.TacticalTest,
        priority = 1,
        question = questionText,
        why = Some(reasonText),
        anchors = List(theme.name),
        confidence = ConfidenceLevel.Engine
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
          kind = AuthorQuestionKind.DefensiveTask,
          priority = 1,
          question = q,
          why = why,
          anchors = List("defense", "threat")
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
          kind = AuthorQuestionKind.ConversionPlan,
          priority = 2,
          question = s"How should $us convert the advantage after $playedSan?",
          why = why,
          anchors = List("conversion", "simplify")
        )
      )

  private def buildTensionDecisionQuestion(
    posOpt: Option[Position],
    ctx: IntegratedContext,
    us: String,
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
          kind = AuthorQuestionKind.TensionDecision,
          priority = 1,
          question = s"Should $us release the central tension with $playedSan$alt?",
          why = Some("This choice often decides whether the other side can simplify into a comfortable structure."),
          anchors = List("central tension")
        )
      }

  private def buildPlanClashQuestion(
    posOpt: Option[Position],
    ctx: IntegratedContext,
    us: String,
    them: String,
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
            kind = AuthorQuestionKind.PlanClash,
            priority = 2,
            question = s"If $us keeps the tension with $bestSan, should $them commit with ...$capSan or ...$pushSan?",
            why = Some("Keeping the tension is not about waiting: it forces the opponent to reveal their plan before you clarify the center."),
            anchors = List("central tension", capSan, pushSan)
          )
        )

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
            kind = AuthorQuestionKind.StructuralCommitment,
            priority = 3,
            question = s"After $playedSan, how should $them recapture — $list?",
            why = Some("Different recaptures lead to different pawn structures and piece placements."),
            anchors = List("recapture choice", "structure")
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

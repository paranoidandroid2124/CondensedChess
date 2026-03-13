package lila.llm.analysis

import lila.llm.*

private[llm] object LegacyStrategicIdeaTextClassifier:

  private final case class LegacyCandidate(
      kind: String,
      score: Double,
      focusSquares: List[String] = Nil,
      focusFiles: List[String] = Nil,
      focusZone: Option[String] = None
  )

  private val SquarePattern = """\b([a-h][1-8])\b""".r
  private val FilePattern = """\b([a-h])-file\b""".r

  def select(pack: StrategyPack): List[StrategyIdeaSignal] =
    val texts =
      collectTexts(pack)
        .flatMap { case (score, text, source) =>
          detectKind(text).map { kind =>
            val normalizedKind =
              if source == "opponent_plan" && kind == StrategicIdeaKind.Prophylaxis then StrategicIdeaKind.CounterplaySuppression
              else kind
            LegacyCandidate(
              kind = normalizedKind,
              score = score,
              focusSquares = extractSquares(text),
              focusFiles = extractFiles(text),
              focusZone = zoneFromText(text)
            )
          }
        }

    texts
      .sortBy(candidate => -candidate.score)
      .headOption
      .toList
      .map { candidate =>
        StrategyIdeaSignal(
          ideaId = "legacy_idea_1",
          ownerSide = pack.sideToMove,
          kind = candidate.kind,
          group = groupForKind(candidate.kind),
          readiness = StrategicIdeaReadiness.Build,
          focusSquares = candidate.focusSquares,
          focusFiles = candidate.focusFiles,
          focusZone = candidate.focusZone,
          confidence = candidate.score.min(0.98),
          evidenceRefs = List("legacy_text_classifier")
        )
      }

  private def collectTexts(pack: StrategyPack): List[(Double, String, String)] =
    val planTexts =
      pack.plans.filter(_.side == pack.sideToMove).zipWithIndex.map { case (plan, idx) =>
        (0.90 - (idx * 0.04), List(Some(plan.planName), plan.priorities.headOption, plan.riskTriggers.headOption).flatten.mkString(" · "), "plan")
      }
    val digestTexts =
      pack.signalDigest.toList.flatMap { digest =>
        List(
          digest.structuralCue.map(v => (0.88, v, "structural_cue")),
          digest.latentPlan.map(v => (0.86, v, "latent_plan")),
          digest.decision.map(v => (0.84, v, "decision")),
          digest.prophylaxisPlan.map(v => (0.89, v, "prophylaxis_plan")),
          digest.prophylaxisThreat.map(v => (0.87, v, "prophylaxis_threat")),
          digest.opponentPlan.map(v => (0.80, v, "opponent_plan")),
          digest.compensation.map(v => (0.82, v, "compensation"))
        ).flatten
      }
    val focusTexts = pack.longTermFocus.take(3).zipWithIndex.map { case (text, idx) => (0.80 - (idx * 0.03), text, "long_term_focus") }
    val routeTexts = pack.pieceRoutes.filter(_.ownerSide == pack.sideToMove).map(route => (0.70, route.purpose, "route_purpose"))
    val targetTexts =
      pack.directionalTargets
        .filter(_.ownerSide == pack.sideToMove)
        .map(target => (0.74, (target.strategicReasons ++ target.prerequisites).mkString(" "), "directional_target"))
    val moveTexts = pack.pieceMoveRefs.filter(_.ownerSide == pack.sideToMove).map(ref => (0.56, ref.idea, "move_ref"))
    (planTexts ++ digestTexts ++ focusTexts ++ routeTexts ++ targetTexts ++ moveTexts)
      .map { case (score, text, source) => (score, text.trim, source) }
      .filter(_._2.nonEmpty)

  private def detectKind(text: String): Option[String] =
    val low = normalize(text)
    Option.when(low.nonEmpty) {
      if hasAny(low, "break", "lever", "pawn storm", "pawn push") then StrategicIdeaKind.PawnBreak
      else if hasAny(low, "space", "restriction", "clamp", "squeeze", "bind") then StrategicIdeaKind.SpaceGainOrRestriction
      else if hasAny(low, "fix", "fixed", "fixing", "freeze", "anchor target", "target square") then StrategicIdeaKind.TargetFixing
      else if hasAny(low, "file", "rank", "line", "lift", "seventh", "occupation", "invasion") then StrategicIdeaKind.LineOccupation
      else if hasAny(low, "outpost", "entrench", "post", "foothold") then StrategicIdeaKind.OutpostCreationOrOccupation
      else if hasAny(low, "bishop pair", "bad bishop", "good knight", "minor piece", "opposite bishops") then
        StrategicIdeaKind.MinorPieceImbalanceExploitation
      else if hasAny(low, "prevent", "stop", "deny", "neutralize", "prophylaxis") then StrategicIdeaKind.Prophylaxis
      else if hasAny(low, "king attack", "kingside attack", "mate", "attack build", "attack", "storm the king") then
        StrategicIdeaKind.KingAttackBuildUp
      else if hasAny(low, "trade", "exchange", "simplify", "transform", "conversion", "convert", "liquidate") then
        StrategicIdeaKind.FavorableTradeOrTransformation
      else if hasAny(low, "counterplay", "shut down", "suppress", "contain", "hold down") then
        StrategicIdeaKind.CounterplaySuppression
      else StrategicIdeaKind.SpaceGainOrRestriction
    }

  private def groupForKind(kind: String): String =
    kind match
      case StrategicIdeaKind.PawnBreak | StrategicIdeaKind.SpaceGainOrRestriction | StrategicIdeaKind.TargetFixing =>
        StrategicIdeaGroup.StructuralChange
      case StrategicIdeaKind.LineOccupation | StrategicIdeaKind.OutpostCreationOrOccupation |
          StrategicIdeaKind.MinorPieceImbalanceExploitation =>
        StrategicIdeaGroup.PieceAndLineManagement
      case _ =>
        StrategicIdeaGroup.InteractionAndTransformation

  private def extractSquares(text: String): List[String] =
    SquarePattern.findAllMatchIn(normalize(text)).map(_.group(1)).toList.distinct.take(3)

  private def extractFiles(text: String): List[String] =
    FilePattern.findAllMatchIn(normalize(text)).map(_.group(1)).toList.distinct.take(2)

  private def zoneFromText(text: String): Option[String] =
    val low = normalize(text)
    if low.contains("kingside") then Some("kingside")
    else if low.contains("queenside") then Some("queenside")
    else if low.contains("center") || low.contains("central") then Some("center")
    else None

  private def normalize(text: String): String =
    Option(text).getOrElse("").trim.toLowerCase

  private def hasAny(text: String, needles: String*): Boolean =
    needles.exists(text.contains)

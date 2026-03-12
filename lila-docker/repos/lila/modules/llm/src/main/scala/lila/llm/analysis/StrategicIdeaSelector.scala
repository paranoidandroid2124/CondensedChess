package lila.llm.analysis

import lila.llm.*

private[llm] object StrategicIdeaSelector:

  private final case class Candidate(
      ownerSide: String,
      kind: String,
      group: String,
      readiness: String,
      focusSquares: List[String] = Nil,
      focusFiles: List[String] = Nil,
      focusDiagonals: List[String] = Nil,
      focusZone: Option[String] = None,
      beneficiaryPieces: List[String] = Nil,
      score: Double,
      evidenceRefs: List[String] = Nil
  ):
    def signature: String =
      List(
        ownerSide,
        kind,
        group,
        focusSquares.mkString(","),
        focusFiles.mkString(","),
        focusDiagonals.mkString(","),
        focusZone.getOrElse(""),
        beneficiaryPieces.mkString(",")
      ).mkString("|")

  private val SquarePattern = """\b([a-h][1-8])\b""".r
  private val FilePattern = """\b([a-h])-file\b""".r
  private val DiagonalPattern = """\b([a-h][1-8]-[a-h][1-8])\b""".r

  def enrich(pack: StrategyPack): StrategyPack =
    val ideas = select(pack)
    if ideas.isEmpty then pack
    else
      val enrichedDigest = enrichDigest(pack.signalDigest, ideas)
      val enrichedFocus = enrichLongTermFocus(pack.longTermFocus, ideas, pack.directionalTargets)
      val enrichedEvidence = enrichEvidence(pack.evidence, ideas, pack.directionalTargets)
      pack.copy(
        strategicIdeas = ideas,
        longTermFocus = enrichedFocus,
        evidence = enrichedEvidence,
        signalDigest = enrichedDigest
      )

  def select(pack: StrategyPack): List[StrategyIdeaSignal] =
    val candidates = collectCandidates(pack)
    if candidates.isEmpty then Nil
    else
      val merged = mergeCandidates(candidates)
      val dominant = merged.head
      val secondary =
        merged.drop(1).find(candidate =>
          candidate.group != dominant.group &&
            math.abs(dominant.score - candidate.score) <= 0.12
        )
      List(Some(dominant), secondary).flatten.zipWithIndex.map { case (candidate, idx) =>
        StrategyIdeaSignal(
          ideaId = s"idea_${idx + 1}",
          ownerSide = candidate.ownerSide,
          kind = candidate.kind,
          group = candidate.group,
          readiness = candidate.readiness,
          focusSquares = candidate.focusSquares,
          focusFiles = candidate.focusFiles,
          focusDiagonals = candidate.focusDiagonals,
          focusZone = candidate.focusZone,
          beneficiaryPieces = candidate.beneficiaryPieces.distinct,
          confidence = candidate.score.min(0.98),
          evidenceRefs = candidate.evidenceRefs.distinct.take(6)
        )
      }

  def humanizedKind(kind: String): String =
    kind match
      case StrategicIdeaKind.PawnBreak                       => "pawn break"
      case StrategicIdeaKind.SpaceGainOrRestriction          => "space gain or restriction"
      case StrategicIdeaKind.TargetFixing                    => "target fixing"
      case StrategicIdeaKind.LineOccupation                  => "line occupation"
      case StrategicIdeaKind.OutpostCreationOrOccupation     => "outpost creation or occupation"
      case StrategicIdeaKind.MinorPieceImbalanceExploitation => "minor-piece imbalance exploitation"
      case StrategicIdeaKind.Prophylaxis                     => "prophylaxis"
      case StrategicIdeaKind.KingAttackBuildUp               => "king-attack build-up"
      case StrategicIdeaKind.FavorableTradeOrTransformation  => "favorable trade or transformation"
      case StrategicIdeaKind.CounterplaySuppression          => "counterplay suppression"
      case other                                             => other.replace('_', ' ')

  def focusSummary(signal: StrategyIdeaSignal): String =
    focusSummary(
      focusSquares = signal.focusSquares,
      focusFiles = signal.focusFiles,
      focusDiagonals = signal.focusDiagonals,
      focusZone = signal.focusZone
    )

  private def collectCandidates(pack: StrategyPack): List[Candidate] =
    val side = pack.sideToMove
    val digest = pack.signalDigest
    val explicit = collectExplicitCandidates(pack, digest, side)
    val directional = collectDirectionalTargets(pack.directionalTargets, side)
    val routes = collectRoutes(pack.pieceRoutes, side)
    val moveRefs = collectMoveRefs(pack.pieceMoveRefs, digest, side)
    (explicit ++ directional ++ routes ++ moveRefs)
      .filter(_.ownerSide == side)
      .sortBy(candidate => -candidate.score)

  private def collectExplicitCandidates(
      pack: StrategyPack,
      digest: Option[NarrativeSignalDigest],
      side: String
  ): List[Candidate] =
    val planCandidates =
      pack.plans
        .filter(_.side == side)
        .take(3)
        .zipWithIndex
        .flatMap { case (plan, idx) =>
          candidateFromText(
            text = List(Some(plan.planName), plan.priorities.headOption, plan.riskTriggers.headOption).flatten.mkString(" · "),
            ownerSide = side,
            readiness = explicitReadiness(pack, side),
            score = 0.92 - (idx * 0.04),
            evidence = List(s"plan:${plan.planName}"),
            pieceHints = piecesFromText(plan.planName)
          )
        }

    val digestCandidates =
      List(
        digest.flatMap(_.structuralCue).map(text => ("structural_cue", text, 0.90)),
        digest.flatMap(_.latentPlan).map(text => ("latent_plan", text, 0.88)),
        digest.flatMap(_.decision).map(text => ("decision", text, 0.87)),
        digest.flatMap(_.prophylaxisPlan).map(text => ("prophylaxis_plan", text, 0.91)),
        digest.flatMap(_.prophylaxisThreat).map(text => ("prophylaxis_threat", text, 0.89)),
        digest.flatMap(_.compensation).map(text => ("compensation", text, 0.86))
      ).flatten.flatMap { case (label, text, score) =>
        candidateFromText(
          text = text,
          ownerSide = side,
          readiness = explicitReadiness(pack, side),
          score = score,
          evidence = List(s"digest:$label"),
          pieceHints = piecesFromText(text)
        )
      }

    val focusCandidates =
      pack.longTermFocus.take(3).zipWithIndex.flatMap { case (text, idx) =>
        candidateFromText(
          text = text,
          ownerSide = side,
          readiness = explicitReadiness(pack, side),
          score = 0.84 - (idx * 0.03),
          evidence = List("long_term_focus"),
          pieceHints = piecesFromText(text)
        )
      }

    planCandidates ++ digestCandidates ++ focusCandidates

  private def collectDirectionalTargets(
      targets: List[StrategyDirectionalTarget],
      side: String
  ): List[Candidate] =
    targets
      .filter(_.ownerSide == side)
      .map { target =>
        val detectedKind = detectDirectionalTargetKind(target)
        Candidate(
          ownerSide = target.ownerSide,
          kind = detectedKind,
          group = groupForKind(detectedKind),
          readiness =
            target.readiness match
              case DirectionalTargetReadiness.Build      => StrategicIdeaReadiness.Build
              case DirectionalTargetReadiness.Contested  => StrategicIdeaReadiness.Build
              case DirectionalTargetReadiness.Premature  => StrategicIdeaReadiness.Premature
              case DirectionalTargetReadiness.Blocked    => StrategicIdeaReadiness.Blocked
              case _                                     => StrategicIdeaReadiness.Build,
          focusSquares = List(target.targetSquare),
          focusZone = zoneFromTexts(target.strategicReasons :+ target.targetSquare),
          beneficiaryPieces = List(target.piece),
          score = 0.78 + readinessScore(target.readiness),
          evidenceRefs = (target.evidence ++ target.strategicReasons.map(reason => s"reason:$reason")).distinct
        )
      }

  private def collectRoutes(routes: List[StrategyPieceRoute], side: String): List[Candidate] =
    routes
      .filter(route => route.ownerSide == side && route.surfaceMode != RouteSurfaceMode.Hidden)
      .map { route =>
        val detectedKind = detectRouteKind(route)
        Candidate(
          ownerSide = route.ownerSide,
          kind = detectedKind,
          group = groupForKind(detectedKind),
          readiness =
            if route.surfaceMode == RouteSurfaceMode.Exact then StrategicIdeaReadiness.Ready
            else StrategicIdeaReadiness.Build,
          focusSquares = route.route.lastOption.toList,
          focusZone = zoneFromTexts(List(route.purpose)),
          beneficiaryPieces = List(route.piece),
          score = 0.64 + (route.surfaceConfidence * 0.18),
          evidenceRefs = (route.evidence :+ s"route:${route.purpose}").distinct
        )
      }

  private def collectMoveRefs(
      moveRefs: List[StrategyPieceMoveRef],
      digest: Option[NarrativeSignalDigest],
      side: String
  ): List[Candidate] =
    val fromMoveRefs =
      moveRefs
        .filter(_.ownerSide == side)
        .map { ref =>
          val detectedKind = detectMoveRefKind(ref)
          Candidate(
            ownerSide = ref.ownerSide,
            kind = detectedKind,
            group = groupForKind(detectedKind),
            readiness = StrategicIdeaReadiness.Premature,
            focusSquares = List(ref.target),
            focusZone = zoneFromTexts(List(ref.idea)),
            beneficiaryPieces = List(ref.piece),
            score = 0.48,
            evidenceRefs = (ref.evidence :+ s"move_ref:${ref.idea}").distinct
          )
        }

    val counterplay =
      List(
        digest.flatMap(_.opponentPlan),
        digest.flatMap(_.counterplayScoreDrop).map(cp => s"counterplay drop $cp"),
        digest.flatMap(_.latentReason).filter(_.toLowerCase.contains("counterplay"))
      ).flatten.headOption.flatMap { text =>
        candidateFromText(
          text = text,
          ownerSide = side,
          readiness = StrategicIdeaReadiness.Build,
          score = 0.50,
          evidence = List("counterplay"),
          pieceHints = Nil
        ).map {
          case value if value.kind == StrategicIdeaKind.Prophylaxis =>
            value.copy(kind = StrategicIdeaKind.CounterplaySuppression, group = StrategicIdeaGroup.InteractionAndTransformation)
          case value => value
        }
      }.toList

    fromMoveRefs ++ counterplay

  private def mergeCandidates(candidates: List[Candidate]): List[Candidate] =
    candidates
      .groupBy(_.signature)
      .values
      .map { grouped =>
        grouped.reduce { (left, right) =>
          if right.score > left.score then
            right.copy(
              focusSquares = (left.focusSquares ++ right.focusSquares).distinct,
              focusFiles = (left.focusFiles ++ right.focusFiles).distinct,
              focusDiagonals = (left.focusDiagonals ++ right.focusDiagonals).distinct,
              beneficiaryPieces = (left.beneficiaryPieces ++ right.beneficiaryPieces).distinct,
              evidenceRefs = (left.evidenceRefs ++ right.evidenceRefs).distinct
            )
          else
            left.copy(
              focusSquares = (left.focusSquares ++ right.focusSquares).distinct,
              focusFiles = (left.focusFiles ++ right.focusFiles).distinct,
              focusDiagonals = (left.focusDiagonals ++ right.focusDiagonals).distinct,
              beneficiaryPieces = (left.beneficiaryPieces ++ right.beneficiaryPieces).distinct,
              evidenceRefs = (left.evidenceRefs ++ right.evidenceRefs).distinct
            )
        }
      }
      .toList
      .sortBy(candidate => -candidate.score)

  private def enrichDigest(
      digest: Option[NarrativeSignalDigest],
      ideas: List[StrategyIdeaSignal]
  ): Option[NarrativeSignalDigest] =
    val base = digest.getOrElse(NarrativeSignalDigest())
    val dominant = ideas.headOption
    val secondary = ideas.drop(1).headOption
    Option.when(dominant.isDefined || digest.isDefined)(
      base.copy(
        dominantIdeaKind = dominant.map(_.kind),
        dominantIdeaGroup = dominant.map(_.group),
        dominantIdeaReadiness = dominant.map(_.readiness),
        dominantIdeaFocus = dominant.map(focusSummary),
        secondaryIdeaKind = secondary.map(_.kind),
        secondaryIdeaGroup = secondary.map(_.group),
        secondaryIdeaFocus = secondary.map(focusSummary)
      )
    )

  private def enrichLongTermFocus(
      current: List[String],
      ideas: List[StrategyIdeaSignal],
      targets: List[StrategyDirectionalTarget]
  ): List[String] =
    val ideaLines =
      ideas.zipWithIndex.map { case (idea, idx) =>
        val prefix = if idx == 0 then "dominant idea" else "secondary idea"
        s"$prefix: ${humanizedKind(idea.kind)}${withFocusSuffix(focusSummary(idea))}"
      }
    val targetLines =
      targets.take(2).map(target => s"objective: work toward making ${target.targetSquare} available for the ${pieceName(target.piece)}")
    (ideaLines ++ targetLines ++ current).map(_.trim).filter(_.nonEmpty).distinct.take(6)

  private def enrichEvidence(
      current: List[String],
      ideas: List[StrategyIdeaSignal],
      targets: List[StrategyDirectionalTarget]
  ): List[String] =
    val ideaEvidence =
      ideas.map(idea => s"idea:${idea.kind}:${focusSummary(idea)}")
    val targetEvidence =
      targets.map(target => s"directional_target:${target.ownerSide}:${target.piece}:${target.targetSquare}:${target.readiness}")
    (current ++ ideaEvidence ++ targetEvidence).map(_.trim).filter(_.nonEmpty).distinct.take(12)

  private def candidateFromText(
      text: String,
      ownerSide: String,
      readiness: String,
      score: Double,
      evidence: List[String],
      pieceHints: List[String]
  ): Option[Candidate] =
    detectKind(text).map { kind =>
      Candidate(
        ownerSide = ownerSide,
        kind = kind,
        group = groupForKind(kind),
        readiness = readiness,
        focusSquares = extractSquares(text),
        focusFiles = extractFiles(text),
        focusDiagonals = extractDiagonals(text),
        focusZone = zoneFromTexts(List(text)),
        beneficiaryPieces = pieceHints,
        score = score,
        evidenceRefs = evidence
      )
    }

  private def explicitReadiness(pack: StrategyPack, side: String): String =
    if pack.pieceRoutes.exists(route => route.ownerSide == side && route.surfaceMode == RouteSurfaceMode.Exact) then StrategicIdeaReadiness.Ready
    else if pack.directionalTargets.exists(target => target.ownerSide == side && target.readiness == DirectionalTargetReadiness.Blocked) then StrategicIdeaReadiness.Blocked
    else if pack.directionalTargets.exists(target => target.ownerSide == side && target.readiness == DirectionalTargetReadiness.Premature) then StrategicIdeaReadiness.Premature
    else StrategicIdeaReadiness.Build

  private def detectDirectionalTargetKind(target: StrategyDirectionalTarget): String =
    val joined = (target.strategicReasons ++ target.prerequisites :+ target.targetSquare).mkString(" ")
    detectKind(joined).getOrElse {
      if target.piece == "R" || target.piece == "Q" then StrategicIdeaKind.LineOccupation
      else if target.piece == "N" || target.piece == "B" then StrategicIdeaKind.OutpostCreationOrOccupation
      else StrategicIdeaKind.TargetFixing
    }

  private def detectRouteKind(route: StrategyPieceRoute): String =
    detectKind(route.purpose).getOrElse {
      if route.piece == "R" || route.piece == "Q" then StrategicIdeaKind.LineOccupation
      else if route.piece == "N" then StrategicIdeaKind.OutpostCreationOrOccupation
      else StrategicIdeaKind.SpaceGainOrRestriction
    }

  private def detectMoveRefKind(ref: StrategyPieceMoveRef): String =
    detectKind(ref.idea).getOrElse(StrategicIdeaKind.FavorableTradeOrTransformation)

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

  private def readinessScore(readiness: String): Double =
    readiness match
      case DirectionalTargetReadiness.Build     => 0.02
      case DirectionalTargetReadiness.Contested => 0.00
      case DirectionalTargetReadiness.Premature => -0.03
      case DirectionalTargetReadiness.Blocked   => -0.06
      case _                                    => 0.0

  private def extractSquares(text: String): List[String] =
    SquarePattern.findAllMatchIn(Option(text).getOrElse("").toLowerCase).map(_.group(1)).toList.distinct.take(3)

  private def extractFiles(text: String): List[String] =
    FilePattern.findAllMatchIn(Option(text).getOrElse("").toLowerCase).map(_.group(1)).toList.distinct.take(2)

  private def extractDiagonals(text: String): List[String] =
    DiagonalPattern.findAllMatchIn(Option(text).getOrElse("").toLowerCase).map(_.group(1)).toList.distinct.take(2)

  private def zoneFromTexts(texts: List[String]): Option[String] =
    val low = texts.map(normalize).mkString(" ")
    if low.contains("kingside") then Some("kingside")
    else if low.contains("queenside") then Some("queenside")
    else if low.contains("center") || low.contains("central") then Some("center")
    else if low.contains("dark square") || low.contains("dark-square") then Some("dark squares")
    else if low.contains("light square") || low.contains("light-square") then Some("light squares")
    else None

  private def focusSummary(
      focusSquares: List[String],
      focusFiles: List[String],
      focusDiagonals: List[String],
      focusZone: Option[String]
  ): String =
    val parts =
      List(
        Option.when(focusSquares.nonEmpty)(focusSquares.mkString(", ")),
        Option.when(focusFiles.nonEmpty)(focusFiles.map(_ + "-file").mkString(", ")),
        Option.when(focusDiagonals.nonEmpty)(focusDiagonals.mkString(", ")),
        focusZone
      ).flatten
    parts.headOption.getOrElse("the key sector")

  private def withFocusSuffix(focus: String): String =
    Option(focus).map(_.trim).filter(_.nonEmpty).map(v => s" around $v").getOrElse("")

  private def piecesFromText(text: String): List[String] =
    val low = normalize(text)
    List(
      Option.when(low.contains("knight"))("N"),
      Option.when(low.contains("bishop"))("B"),
      Option.when(low.contains("rook"))("R"),
      Option.when(low.contains("queen"))("Q"),
      Option.when(low.contains("pawn"))("P"),
      Option.when(low.contains("king"))("K")
    ).flatten

  private def pieceName(code: String): String =
    code match
      case "N" => "knight"
      case "B" => "bishop"
      case "R" => "rook"
      case "Q" => "queen"
      case "K" => "king"
      case _   => "piece"

  private def hasAny(text: String, needles: String*): Boolean =
    needles.exists(text.contains)

  private def normalize(text: String): String =
    Option(text).getOrElse("").trim.toLowerCase

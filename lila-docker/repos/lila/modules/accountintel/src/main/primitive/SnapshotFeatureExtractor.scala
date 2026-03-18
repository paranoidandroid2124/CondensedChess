package lila.accountintel.primitive

import chess.*
import chess.format.Fen
import chess.format.pgn.{ Parser, PgnStr }
import lila.tree.ParseImport

import lila.accountintel.*
import lila.accountintel.AccountIntel.*
import lila.accountintel.opening.CanonicalOpeningBook
import lila.llm.{ MoveEval, PgnAnalysisHelper }
import lila.llm.analysis.{
  CausalCollapseAnalyzer,
  CommentaryEngine,
  GameNarrativeOrchestrator,
  PlanStateTracker
}
import lila.llm.model.CollapseAnalysis
import lila.llm.model.strategic.{ EndgamePatternState, StrategicSalience, VariationLine }

object SnapshotFeatureExtractor:

  def extract(username: String, games: List[ExternalGame]): Either[String, PrimitiveBundle] =
    val parsedGames = games.flatMap(parseGame(_, username))
    Option
      .when(parsedGames.nonEmpty)(parsedGames)
      .toRight("No parseable public games found for this account.")
      .map: parsed =>
        val featureRows = parsed.flatMap(extractRowsForGame)
        PrimitiveBundle(
          parsedGames = parsed,
          featureRows = featureRows,
          sampledGameCount = parsed.size,
          eligibleGameCount = games.size,
          warnings = buildWarnings(parsed.size, games.size)
        )

  private def parseGame(game: ExternalGame, username: String): Option[ParsedGame] =
    ParseImport
      .full(PgnStr(game.pgn))
      .toOption
      .flatMap: imported =>
        val tags = imported.parsed.tags
        val white = tags.names.white.map(_.value.trim).filter(_.nonEmpty).getOrElse(game.white)
        val black = tags.names.black.map(_.value.trim).filter(_.nonEmpty).getOrElse(game.black)
        val subjectColor =
          if white.equalsIgnoreCase(username) then Some(Color.White)
          else if black.equalsIgnoreCase(username) then Some(Color.Black)
          else None
        subjectColor.map: color =>
          val providerOpeningName = tags("Opening").map(_.trim).filter(_.nonEmpty)
          val providerEcoUrl = tags("ECOUrl").map(_.trim).filter(_.nonEmpty)
          val providerEcoCode = tags("ECO").map(_.trim.toUpperCase).filter(_.nonEmpty)
          val opening =
            CanonicalOpeningBook.classify(
              pgn = game.pgn,
              subjectColor = color,
              providerOpeningName = providerOpeningName,
              providerEcoCode = providerEcoCode,
              providerEcoUrl = providerEcoUrl
            )
          ParsedGame(
            external = game.copy(white = white, black = black),
            subjectName = if color.white then white else black,
            subjectColor = color,
            subjectResult = subjectResult(game.result, color),
            openingName = opening.canonicalName,
            openingFamily = opening.family,
            openingBucket = opening.bucket,
            openingRelation = opening.relation,
            canonicalEcoCode = opening.ecoCode,
            providerOpeningName = providerOpeningName,
            providerEcoCode = providerEcoCode,
            providerEcoUrl = providerEcoUrl,
            labels = labelsFor(opening.family, color),
            plyCount = imported.game.ply.value,
            rep = representativePosition(game.pgn, color)
          )

  private def extractRowsForGame(game: ParsedGame): List[SnapshotFeatureRow] =
    val moveEvalsByPly = game.external.moveEvals.map(ev => ev.ply -> ev).toMap
    val events = scanDecisionEvents(game, moveEvalsByPly)
    val rows = buildRows(game, events, moveEvalsByPly)
    val collapseAttached = attachCollapseBacktrace(game, rows, moveEvalsByPly)
    attachHeuristicCollapseBacktrace(game, collapseAttached)

  private def buildRows(
      game: ParsedGame,
      events: List[DecisionEvent],
      moveEvalsByPly: Map[Int, MoveEval]
  ): List[SnapshotFeatureRow] =
    val (rows, _, _) =
      events
        .sortBy(_.ply)
        .foldLeft(
          (List.empty[SnapshotFeatureRow], PlanStateTracker.empty, Option.empty[EndgamePatternState])
        ) { case ((acc, planTracker, prevEgState), event) =>
          val vars = chooseVariations(event, moveEvalsByPly.get(event.ply))
          val analysis =
            CommentaryEngine.assessExtended(
              fen = event.fen,
              variations = vars,
              playedMove = Some(event.playedUci),
              opening = Some(game.openingName),
              phase = None,
              ply = event.ply,
              prevMove = Some(event.playedUci),
              prevPlanContinuity = planTracker.getContinuity(event.subjectColor),
              evalDeltaCp = evalDeltaCp(moveEvalsByPly, event.ply),
              prevEndgameState = prevEgState
            )

          analysis match
            case Some(data) =>
              val nextTracker = planTracker.update(
                movingColor = event.subjectColor,
                ply = event.ply,
                primaryPlan = data.plans.headOption,
                secondaryPlan = data.plans.lift(1),
                sequence = data.planSequence
              )
              val nextEgState = EndgamePatternState.evolve(prevEgState, data.endgameFeatures, event.ply)
              val enriched = data.copy(planContinuity = nextTracker.getContinuity(event.subjectColor))
              (acc :+ toFeatureRow(event, enriched, collapse = None), nextTracker, nextEgState)
            case None =>
              (acc, planTracker, prevEgState)
        }
    rows

  private def attachCollapseBacktrace(
      game: ParsedGame,
      rows: List[SnapshotFeatureRow],
      moveEvalsByPly: Map[Int, MoveEval]
  ): List[SnapshotFeatureRow] =
    if rows.isEmpty || moveEvalsByPly.isEmpty then rows
    else
      val plyDataByPly =
        PgnAnalysisHelper.extractPlyData(game.external.pgn).toOption.getOrElse(Nil).map(p => p.ply -> p).toMap
      val collapses =
        GameNarrativeOrchestrator
          .selectKeyMoments(game.external.moveEvals)
          .flatMap: moment =>
            if moment.momentType == "Blunder" || moment.momentType == "SustainedPressure" then
              for
                plyData <- plyDataByPly.get(moment.ply).toList
                eval <- moveEvalsByPly.get(moment.ply).toList
                analysis <- CommentaryEngine
                  .assessExtended(
                    fen = plyData.fen,
                    variations = eval.getVariations,
                    playedMove = Some(plyData.playedUci),
                    opening = Some(game.openingName),
                    phase = None,
                    ply = plyData.ply,
                    prevMove = Some(plyData.playedUci)
                  )
                  .toList
                collapse <- CausalCollapseAnalyzer
                  .analyze(moment.ply, game.external.moveEvals, analysis)
                  .toList
              yield moment.ply -> collapse
            else Nil

      collapses.foldLeft(rows) { case (acc, (blunderPly, collapse)) =>
        acc.map: row =>
          if
            row.gameId == game.external.gameId &&
              row.ply >= collapse.earliestPreventablePly &&
              row.ply <= blunderPly
          then
            val rootCauseTag = normalizeLabel(collapse.rootCause)
            val collapseBoost =
              if row.quiet then 0.92
              else 0.78
            row.copy(
              labels = (row.labels :+ rootCauseTag).distinct,
              preventabilityScore = row.preventabilityScore.max(collapseBoost),
              branchingScore = row.branchingScore.max(if row.quiet then 0.78 else 0.68),
              earliestPreventablePly = Some(collapse.earliestPreventablePly),
              collapseMomentPly = Some(blunderPly),
              collapseAnalysis = Some(collapse)
            )
          else row
      }

  private case class HeuristicCollapseWindow(
      structureFamily: String,
      earliestPreventablePly: Int,
      collapseMomentPly: Int
  )

  private def attachHeuristicCollapseBacktrace(
      game: ParsedGame,
      rows: List[SnapshotFeatureRow]
  ): List[SnapshotFeatureRow] =
    if rows.size < 2 || game.subjectResult == SubjectResult.Win then rows
    else
      val windows = heuristicCollapseWindows(rows)
      if windows.isEmpty then rows
      else
        windows.foldLeft(rows) { case (acc, window) =>
          acc.map: row =>
            val insideWindow =
              row.gameId == game.external.gameId &&
                row.structureFamily == window.structureFamily &&
                row.ply >= window.earliestPreventablePly &&
                row.ply <= window.collapseMomentPly &&
                row.collapseMomentPly.isEmpty
            if insideWindow then
              val quietBoost =
                if row.ply == window.earliestPreventablePly then 0.82
                else if row.quiet then 0.76
                else 0.68
              val branchingBoost =
                if row.ply == window.collapseMomentPly then 0.82
                else if row.quiet then 0.74
                else 0.66
              row.copy(
                labels = (row.labels :+ "Earlier quiet decision").distinct.take(6),
                preventabilityScore = row.preventabilityScore.max(quietBoost),
                branchingScore = row.branchingScore.max(branchingBoost),
                earliestPreventablePly = Some(window.earliestPreventablePly),
                collapseMomentPly = Some(window.collapseMomentPly)
              )
            else row
        }

  private def heuristicCollapseWindows(rows: List[SnapshotFeatureRow]): List[HeuristicCollapseWindow] =
    val sorted = rows.sortBy(_.ply)
    sorted
      .flatMap: collapseRow =>
        val looksLikeCollapse =
          collapseRow.planAlignmentBand.contains("OffPlan") &&
            collapseRow.preventabilityScore >= 0.60 &&
            collapseRow.branchingScore >= 0.62 &&
            (
              collapseRow.triggerHints.nonEmpty ||
                collapseRow.transitionType.exists(_ != "Continuation") ||
                collapseRow.integratedTension >= 0.60
            )
        if !looksLikeCollapse || collapseRow.collapseMomentPly.isDefined then None
        else
          val earliestFloor = (collapseRow.ply - 6).max(1)
          val candidates =
            sorted.filter: row =>
              row.ply >= earliestFloor &&
                row.ply <= (collapseRow.ply - 2) &&
                row.structureFamily == collapseRow.structureFamily &&
                row.quiet &&
                row.collapseMomentPly.isEmpty &&
                (
                  row.triggerHints.nonEmpty ||
                    row.transitionType.exists(_ != "Continuation") ||
                    row.planAlignmentBand.contains("OffPlan") ||
                    row.planIntent.isDefined ||
                    row.integratedTension >= 0.55
                )
          candidates.headOption.map(anchor =>
            HeuristicCollapseWindow(
              structureFamily = collapseRow.structureFamily,
              earliestPreventablePly = anchor.ply,
              collapseMomentPly = collapseRow.ply
            )
          )
      .distinctBy(window =>
        s"${slug(window.structureFamily)}:${window.earliestPreventablePly}:${window.collapseMomentPly}"
      )

  private def toFeatureRow(
      event: DecisionEvent,
      analysis: lila.llm.model.ExtendedAnalysisData,
      collapse: Option[CollapseAnalysis]
  ): SnapshotFeatureRow =
    val structureFamily = structureFamilyFor(analysis, event.game)
    val labels =
      (
        event.labels ++
          analysis.structureProfile.toList.flatMap(_.evidenceCodes.map(normalizeLabel)) ++
          analysis.planAlignment.toList.flatMap(_.reasonCodes.map(normalizeLabel)) ++
          analysis.planHypotheses.map(_.planName) ++
          analysis.planHypotheses.map(_.themeL1).filterNot(_ == "unknown")
      ).distinct.take(6)
    val explainabilityScore =
      (
        0.34 +
          analysis.structureProfile.fold(0.0)(_ => 0.18) +
          analysis.planAlignment.fold(0.0)(_ => 0.18) +
          analysis.planSequence.fold(0.0)(_ => 0.12) +
          Option.when(analysis.planHypotheses.nonEmpty)(0.12).getOrElse(0.0) +
          Option.when(analysis.planAlignment.flatMap(_.narrativeIntent).isDefined)(0.08).getOrElse(0.0)
      ).min(1.0)
    val preventabilityScore =
      (
        0.24 +
          Option.when(event.quiet)(0.2).getOrElse(0.0) +
          Option.when(analysis.planAlignment.exists(_.band.toString == "OffPlan"))(0.18).getOrElse(0.0) +
          Option
            .when(
              analysis.planSequence.exists(seq =>
                seq.transitionType.toString == "ForcedPivot" || seq.transitionType.toString == "NaturalShift"
              )
            )(0.14)
            .getOrElse(0.0) +
          Option.when(analysis.nature.tension >= 0.55)(0.1).getOrElse(0.0) +
          Option.when(collapse.isDefined)(0.2).getOrElse(0.0)
      ).min(1.0)
    val branchingScore =
      (
        0.26 +
          Option
            .when(event.triggerHints.exists(Set("tension_release", "pawn_structure_mutation")))(0.2)
            .getOrElse(0.0) +
          Option
            .when(
              analysis.planSequence.exists(seq =>
                seq.transitionType.toString == "ForcedPivot" ||
                  seq.transitionType.toString == "NaturalShift" ||
                  seq.transitionType.toString == "Opportunistic"
              )
            )(0.18)
            .getOrElse(0.0) +
          Option.when(analysis.strategicSalience == StrategicSalience.High)(0.16).getOrElse(0.0) +
          Option.when(analysis.planHypotheses.size >= 2)(0.12).getOrElse(0.0) +
          Option.when(collapse.isDefined)(0.14).getOrElse(0.0)
      ).min(1.0)

    SnapshotFeatureRow(
      gameId = event.gameId,
      subjectColor = event.subjectColor,
      openingFamily = event.openingFamily,
      structureFamily = structureFamily,
      labels = labels,
      ply = event.ply,
      fen = event.fen,
      sideToMove = event.sideToMove,
      quiet = event.quiet,
      triggerHints = event.triggerHints,
      playedUci = event.playedUci,
      playedSan = event.playedSan,
      explainabilityScore = explainabilityScore,
      preventabilityScore = preventabilityScore,
      branchingScore = branchingScore,
      transitionType = analysis.planSequence.map(_.transitionType.toString),
      strategicSalienceHigh = analysis.strategicSalience == StrategicSalience.High,
      planAlignmentBand = analysis.planAlignment.map(_.band.toString),
      planIntent = analysis.planAlignment.flatMap(_.narrativeIntent),
      planRisk = analysis.planAlignment.flatMap(_.narrativeRisk),
      hypothesisThemes = analysis.planHypotheses.map(_.themeL1).filterNot(_ == "unknown").distinct,
      integratedTension = analysis.nature.tension,
      earliestPreventablePly = collapse.map(_.earliestPreventablePly),
      collapseMomentPly = none,
      collapseAnalysis = collapse,
      analysis = analysis,
      game = event.game,
      lastSan = event.lastSan
    )

  private def scanDecisionEvents(
      game: ParsedGame,
      moveEvalsByPly: Map[Int, MoveEval]
  ): List[DecisionEvent] =
    Parser.mainline(PgnStr(game.external.pgn)) match
      case Left(_) => Nil
      case Right(parsed) =>
        val rootGame = parsed.toGame
        val replay = Replay.makeReplay(rootGame, parsed.moves)
        replay.replay.chronoMoves.zipWithIndex.flatMap { case (move, idx) =>
          val ply = rootGame.ply.value + idx + 1
          val before = if idx == 0 then rootGame.position else replay.replay.chronoMoves.take(idx).last.after
          if before.color != game.subjectColor then None
          else
            val san = move.toSanStr.toString
            val uci = move.toUci.uci
            val movePiece = movingPiece(before, uci)
            val triggerHints = triggerHintsFor(before, san, uci, movePiece, game.subjectColor, ply)
            Option.when(triggerHints.nonEmpty):
              DecisionEvent(
                gameId = game.external.gameId,
                subjectColor = game.subjectColor,
                openingFamily = game.openingFamily,
                labels = game.labels,
                ply = ply,
                fen = Fen.write(before, Ply(ply - 1).fullMoveNumber).value,
                sideToMove = before.color,
                quiet = !san.contains("+") && !san.contains("#"),
                triggerHints = triggerHints,
                playedSan = san,
                playedUci = uci,
                lastSan = replay.replay.chronoMoves.lift(idx - 1).map(_.toSanStr.toString),
                moveEval = moveEvalsByPly.get(ply),
                game = game
              )
        }

  private def chooseVariations(
      event: DecisionEvent,
      moveEval: Option[MoveEval]
  ): List[VariationLine] =
    moveEval
      .map(_.getVariations)
      .filter(_.nonEmpty)
      .getOrElse:
        List(
          VariationLine(
            moves = List(event.playedUci),
            scoreCp = moveEval.map(_.cp).getOrElse(0),
            mate = moveEval.flatMap(_.mate)
          )
        )

  private def evalDeltaCp(moveEvalsByPly: Map[Int, MoveEval], ply: Int): Option[Int] =
    for
      prev <- moveEvalsByPly.get(ply - 1).map(evalScore)
      curr <- moveEvalsByPly.get(ply).map(evalScore)
    yield curr - prev

  private def evalScore(eval: MoveEval): Int =
    eval.mate.map(m => if m > 0 then 10000 - m else -10000 + m).getOrElse(eval.cp)

  private def movingPiece(position: Position, uci: String): Option[Piece] =
    squareFromUci(uci.take(2)).flatMap(position.board.pieceAt)

  private def triggerHintsFor(
      before: Position,
      san: String,
      uci: String,
      movePiece: Option[Piece],
      mover: Color,
      ply: Int
  ): List[String] =
    val role = movePiece.map(_.role)
    val destination = destinationSquare(uci)
    val capture = san.contains("x")
    val isPawnMove = role.contains(Pawn)
    val isMajorPiece = role.exists(r => r == Rook || r == Queen)
    val centralDestination =
      destination.exists(dest => Set('c', 'd', 'e', 'f').contains(dest.key.headOption.getOrElse(' ')))
    val openOrSemiOpenFile =
      destination.exists(dest => fileOpenOrSemiOpen(before, dest.key.head, mover))
    val hints = scala.collection.mutable.ListBuffer.empty[String]
    if capture && (centralDestination || isPawnMove || ply >= 10) then hints += "tension_release"
    if isPawnMove && (ply >= 8 || capture || centralDestination) then hints += "pawn_structure_mutation"
    if san.startsWith("O-O") then hints += "castling_commitment"
    if capture && role.exists(_ != Pawn) && ply >= 10 then hints += "major_simplification"
    if isMajorPiece && openOrSemiOpenFile then hints += "file_commitment"
    hints.toList.distinct

  private def fileOpenOrSemiOpen(position: Position, file: Char, mover: Color): Boolean =
    val squares = (1 to 8).flatMap(rank => Square.fromKey(s"$file$rank"))
    val ourPawn = squares.exists(sq => position.board.pieceAt(sq).contains(Piece(mover, Pawn)))
    val theirPawn = squares.exists(sq => position.board.pieceAt(sq).contains(Piece(!mover, Pawn)))
    !ourPawn || !theirPawn

  private def squareFromUci(raw: String): Option[Square] =
    Option(raw).filter(_.length == 2).flatMap(Square.fromKey)

  private def destinationSquare(uci: String): Option[Square] =
    if uci.length >= 4 then Square.fromKey(uci.slice(2, 4)) else None

  private def structureFamilyFor(
      analysis: lila.llm.model.ExtendedAnalysisData,
      game: ParsedGame
  ): String =
    analysis.structureProfile
      .map(_.primary.toString)
      .filterNot(_ == "Unknown")
      .map(humanizeSignal)
      .getOrElse(game.openingFamily)

  private def representativePosition(pgn: String, color: Color): Option[RepPos] =
    snapshots(pgn).toOption.flatMap: snaps =>
      val candidates = snaps.filter(_.color == color)
      if candidates.isEmpty then None
      else
        val band = candidates.filter(s => s.ply >= 11 && s.ply <= 28)
        val chosen = (if band.nonEmpty then band else candidates).sortBy(s => math.abs(s.ply - 18)).headOption
        chosen.map(s => RepPos(s, snaps.find(_.ply == s.ply - 1).map(_.san)))

  private def snapshots(pgn: String): Either[String, List[PlySnap]] =
    Parser.mainline(PgnStr(pgn)) match
      case Left(err) => Left(err.value)
      case Right(parsed) =>
        val game = parsed.toGame
        val replay = Replay.makeReplay(game, parsed.moves)
        Right(replay.replay.chronoMoves.zipWithIndex.map { case (move, idx) =>
          val ply = game.ply.value + idx + 1
          val before = if idx == 0 then game.position else replay.replay.chronoMoves.take(idx).last.after
          PlySnap(
            ply = ply,
            fen = Fen.write(before, Ply(ply - 1).fullMoveNumber).value,
            san = move.toSanStr.toString,
            uci = move.toUci.uci,
            color = before.color
          )
        })

  private def subjectResult(result: String, color: Color): SubjectResult =
    result match
      case "1-0" => if color.white then SubjectResult.Win else SubjectResult.Loss
      case "0-1" => if color.black then SubjectResult.Win else SubjectResult.Loss
      case "1/2-1/2" => SubjectResult.Draw
      case _ => SubjectResult.Draw

  private def labelsFor(openingFamily: String, color: Color): List[String] =
    val lower = openingFamily.toLowerCase
    val base =
      if lower.contains("sicilian") then List("asymmetrical center", "open c-file")
      else if lower.contains("queen's gambit") then List("queen-pawn tension", "c-file pressure")
      else if lower.contains("nimzo") then List("central restraint", "bishop pair tension")
      else if lower.contains("king's indian") then List("dark-square pressure", "kingside race")
      else if lower.contains("caro-kann") then List("solid center", "piece rerouting")
      else if lower.contains("french") then List("locked center", "queenside counterplay")
      else if lower.contains("english") then List("flank pressure", "flexible center")
      else if lower.contains("london") then List("solid setup", "kingside expansion")
      else List("central tension", "piece coordination")
    (base :+ s"${colorLabel(color)} structures").distinct.take(3)

  private def buildWarnings(sampled: Int, eligible: Int): List[String] =
    List(
      Option.when(sampled < 8)("Sample size is still thin, so some sections stay provisional."),
      Option.when(eligible > sampled)(
        "Some fetched games were skipped because they could not be parsed cleanly."
      )
    ).flatten

  private def normalizeLabel(raw: String): String =
    humanizeSignal(raw)

  private def humanizeEnum(raw: String): String =
    raw
      .replaceAll("([a-z])([A-Z])", "$1 $2")
      .replaceAll("\\s+", " ")
      .trim
      .split(' ')
      .filter(_.nonEmpty)
      .map(word => s"${word.head.toUpper}${word.drop(1).toLowerCase}")
      .mkString(" ")

  private def humanizeSignal(raw: String): String =
    val compact = raw.replace('_', ' ').replaceAll("\\s+", " ").trim
    val collapsed = compact.replace(" ", "")
    collapsed.toLowerCase match
      case "iqpwhite" => "White IQP"
      case "iqpblack" => "Black IQP"
      case "reqwhiteisolatedd" => "White isolated d-pawn"
      case "reqblackisolatedd" => "Black isolated d-pawn"
      case "reqgpawnandbishopshell" => "Kingside fianchetto shell"
      case "unclassifiedmiddlegame" => "Recent practical structure"
      case "recentpracticalstructure" => "Recent practical structure"
      case "unknown" => "Recent practical structure"
      case _ =>
        val withoutReq = compact.replaceFirst("^Req\\s+", "")
        humanizeEnum(withoutReq)

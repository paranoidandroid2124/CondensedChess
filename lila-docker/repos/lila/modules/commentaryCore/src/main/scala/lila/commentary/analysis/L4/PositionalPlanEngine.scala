package lila.commentary.analysis.L4

import chess.*
import chess.format.Fen
import chess.variant.Standard
import lila.commentary.model.authoring.PlanHypothesis
import lila.commentary.analysis.L3.{ PassedPawnUrgency, PawnPlayAnalysis, PositionClassification, NatureType, TaskModeType }
import lila.commentary.analysis.strategic.{ ActivityAnalyzerImpl, StructureAnalyzerImpl }

private[analysis] object PositionalPlanEngine:

  private val activityAnalyzer = new ActivityAnalyzerImpl()
  private val structureAnalyzer = new StructureAnalyzerImpl()

  def enrich(
      fen: String,
      classification: PositionClassification,
      pawnAnalysis: Option[PawnPlayAnalysis],
      hypothesis: PlanHypothesis
  ): PlanHypothesis =
    val activeColor = fen.split(" ").lift(1).getOrElse("w").toLowerCase
    val isWhite = activeColor == "w"
    val ourColorStr = if isWhite then "White" else "Black"

    val board = Fen.read(Standard, Fen.Full(fen)).map(_.board)

    // 1. Regrouping Piece Selection
    val pieceActivity = board.toList.flatMap { b =>
      activityAnalyzer.analyze(b, chess.Color.White) ++
        activityAnalyzer.analyze(b, chess.Color.Black)
    }

    val ourPiecesActivity = pieceActivity.filter { info =>
      val colorOpt = for {
        b <- board
        piece <- b.pieceAt(info.square)
      } yield piece.color.name
      colorOpt.exists(_.equalsIgnoreCase(ourColorStr))
    }

    val leastActiveOpt = ourPiecesActivity.sortBy(_.mobilityScore).headOption
    val routeSquareOpt = leastActiveOpt.flatMap(_.keyRoutes.headOption).map(_.key)

    // 2. Pawn Break Selection
    val breakFileOpt = pawnAnalysis.flatMap(_.breakFile)

    // 3. Target Selection
    val structuralWeaknesses = board.toList.flatMap { b =>
      structureAnalyzer.analyze(b)
    }

    val opponentWeaknesses = structuralWeaknesses.filter { info =>
      info.color == (if isWhite then chess.Color.Black else chess.Color.White)
    }
    val targetSquare = opponentWeaknesses.flatMap(_.squares.headOption).headOption.map(_.key)
    val actionablePasser =
      pawnAnalysis.exists(pa =>
        pa.primaryDriver == "passed_pawn" ||
          pa.passedPawnUrgency == PassedPawnUrgency.Critical ||
          (pa.passedPawnUrgency == PassedPawnUrgency.Important && pa.pusherSupport && !pa.passerBlockade)
      )

    // Template Selection
    val template = (classification.taskMode.taskMode, classification.nature.natureType) match
      case (TaskModeType.ExplainPlan, NatureType.Static) => "StaticClamp"
      case (TaskModeType.ExplainPlan, _) => "DynamicBreak"
      case (TaskModeType.ExplainConvert, _) => "EndgameConversion"
      case _ => "TacticalRestraint"

    val executionSteps = template match
      case "StaticClamp" =>
        List(
          leastActiveOpt match
            case Some(info) =>
              routeSquareOpt match
                case Some(square) =>
                  s"In this static position, regroup the ${info.piece.name.toLowerCase} on ${info.square.key} to $square to optimize piece placement."
                case None =>
                  s"In this static position, improve the ${info.piece.name.toLowerCase} on ${info.square.key} before assigning it a concrete route."
            case None => "Improve your worst-placed piece before naming a concrete redeployment square."
          ,
          breakFileOpt match
            case Some(file) => s"Restrain opponent counterplay and slowly prepare the pawn push on the $file-file."
            case None => "Keep the pawn structure stable while looking for a concrete central lever."
          ,
          targetSquare
            .map(square => s"Use piece pressure against the static weakness on $square without loosening the structure.")
            .getOrElse("Improve piece placement first; no specific weak square is certified yet.")
        )
      case "DynamicBreak" =>
        List(
          leastActiveOpt match
            case Some(info) =>
              routeSquareOpt match
                case Some(square) =>
                  s"Regroup the ${info.piece.name.toLowerCase} on ${info.square.key} to $square to prepare for open-board activity."
                case None =>
                  s"Improve the ${info.piece.name.toLowerCase} on ${info.square.key} before forcing open-board activity."
            case None => "Coordinate your minor pieces to prepare for central exchanges."
          ,
          breakFileOpt match
            case Some(file) => s"Use the pawn break on the $file-file only when the opened lines are tactically supported."
            case None => "Look for a concrete central tension break before opening lines for heavy pieces."
          ,
          targetSquare
            .map(square => s"Increase pressure on the vulnerable target on $square only if the tactics support it.")
            .getOrElse("Look for a concrete target before turning the dynamic play into a direct attack.")
        )
      case "EndgameConversion" =>
        List(
          (leastActiveOpt, routeSquareOpt) match
            case (Some(info), Some(square)) if actionablePasser =>
              s"Activate your king and regroup the ${info.piece.name.toLowerCase} on ${info.square.key} to $square to support the passed-pawn task."
            case (Some(info), Some(square)) =>
              s"Activate your king and regroup the ${info.piece.name.toLowerCase} on ${info.square.key} to $square to improve the conversion setup."
            case (Some(info), None) if actionablePasser =>
              s"Activate your king and improve the ${info.piece.name.toLowerCase} on ${info.square.key} before tying it to the passed-pawn task."
            case (Some(info), None) =>
              s"Activate your king and improve the ${info.piece.name.toLowerCase} on ${info.square.key} before claiming a conversion route."
            case (None, _) if actionablePasser => "Coordinate the king and remaining pieces around the passed-pawn task."
            case (None, _) => "Coordinate the king and remaining pieces before trying to convert."
          ,
          pawnAnalysis match
            case Some(pa) if actionablePasser && pa.passerBlockade =>
              "First improve support around the blockaded passer rather than assuming it can run."
            case Some(_) if actionablePasser =>
              "Advance the passer only with the current support intact."
            case Some(pa) if pa.pawnBreakReady =>
              pa.breakFile match
                case Some(file) => s"Use the pawn break on the $file-file only if it keeps the conversion path stable."
                case None => "Use the available pawn break only if it keeps the conversion path stable."
            case _ => "Improve king activity and pawn structure before forcing pawn advances."
          ,
          targetSquare
            .map(square => s"Use $square as a structural target only when the line keeps counterplay under control.")
            .getOrElse("Keep the plan technical and verify the next pawn or exchange target before claiming progress.")
        )
      case _ =>
        List(
          (leastActiveOpt, routeSquareOpt) match
            case (Some(info), Some(square)) =>
              s"Secure piece safety by regrouping the ${info.piece.name.toLowerCase} on ${info.square.key} to $square."
            case (Some(info), None) =>
              s"Secure piece safety and improve the ${info.piece.name.toLowerCase} on ${info.square.key} before naming a route."
            case (None, _) => "Regroup your pieces to stable defensive squares."
          ,
          breakFileOpt match
            case Some(file) => s"Avoid premature pawn breaks on the $file-file and maintain a solid defensive structure."
            case None => "Keep the pawn structure stable and avoid opening lines for opponent pieces."
          ,
          targetSquare
            .map(square => s"Guard against tactical threats and use $square as a target only if the line confirms it.")
            .getOrElse("Guard against tactical threats before naming a concrete target.")
        )

    hypothesis.copy(
      executionSteps = executionSteps
    )

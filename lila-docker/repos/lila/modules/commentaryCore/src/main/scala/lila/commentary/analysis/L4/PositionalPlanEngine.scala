package lila.commentary.analysis.L4

import chess.*
import chess.format.Fen
import chess.variant.Standard
import lila.commentary.model.authoring.PlanHypothesis
import lila.commentary.analysis.L3.{ PositionClassification, NatureType, TaskModeType, PawnPlayAnalysis }
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
    val destSquare = leastActiveOpt.flatMap(_.keyRoutes.headOption).map(_.key).getOrElse {
      if isWhite then "d4" else "d5"
    }

    // 2. Pawn Break Selection
    val breakFileOpt = pawnAnalysis.flatMap(_.breakFile)

    // 3. Target Selection
    val structuralWeaknesses = board.toList.flatMap { b =>
      structureAnalyzer.analyze(b)
    }

    val opponentWeaknesses = structuralWeaknesses.filter { info =>
      info.color == (if isWhite then chess.Color.Black else chess.Color.White)
    }
    val targetSquare = opponentWeaknesses.flatMap(_.squares.headOption).headOption.map(_.key).getOrElse {
      if isWhite then "d5" else "d4"
    }

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
            case Some(info) => s"In this static position, regroup the ${info.piece.name.toLowerCase} on ${info.square.key} to $destSquare to optimize piece placement."
            case None => "Regroup your worst-placed piece to a more stable outpost."
          ,
          breakFileOpt match
            case Some(file) => s"Restrain opponent counterplay and slowly prepare the pawn push on the $file-file."
            case None => "Maintain a solid pawn skeleton and slowly build central space pressure."
          ,
          s"Exploit the static weakness on $targetSquare by placing pieces on dominant outpost squares."
        )
      case "DynamicBreak" =>
        List(
          leastActiveOpt match
            case Some(info) => s"Regroup the ${info.piece.name.toLowerCase} on ${info.square.key} to $destSquare to prepare for open-board activity."
            case None => "Coordinate your minor pieces to prepare for central exchanges."
          ,
          breakFileOpt match
            case Some(file) => s"Exert maximum pressure with the pawn break on the $file-file to open lines."
            case None => "Force pawn tension resolution in the center to open lines for heavy pieces."
          ,
          s"Attack the vulnerable target on $targetSquare to create tactical opportunities."
        )
      case "EndgameConversion" =>
        List(
          leastActiveOpt match
            case Some(info) => s"Activate your king and regroup the ${info.piece.name.toLowerCase} on ${info.square.key} to $destSquare to support the passed pawn."
            case None => "Regroup your remaining active forces to support the pawn promotion."
          ,
          breakFileOpt match
            case Some(file) => s"Push the passed pawn on the $file-file toward promotion."
            case None => "Advance your primary passed pawn toward the promotion rank."
          ,
          s"Target the weak pawn on $targetSquare to simplify the game into a winning ending."
        )
      case _ =>
        List(
          leastActiveOpt match
            case Some(info) => s"Secure piece safety by regrouping the ${info.piece.name.toLowerCase} on ${info.square.key} to $destSquare."
            case None => "Regroup your pieces to defensive blockading squares."
          ,
          breakFileOpt match
            case Some(file) => s"Avoid premature pawn breaks on the $file-file and maintain a solid defensive structure."
            case None => "Maintain a closed pawn center and avoid opening lines for opponent pieces."
          ,
          s"Guard against tactical threats and neutralize the opponent's outpost on $targetSquare."
        )

    hypothesis.copy(
      executionSteps = executionSteps
    )


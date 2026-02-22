package lila.llm.analysis.structure

import scala.io.Source
import play.api.libs.json._
import chess.format.Fen
import chess.variant.Standard
import chess.Color
import lila.llm.analysis.PositionAnalyzer
import lila.llm.analysis.structure.PawnStructureClassifier

object GoldsetDebug {
  def main(args: Array[String]): Unit = {
    val file = "modules/llm/src/test/resources/structure_goldset_v1_llm_curated.jsonl"
    val lines = Source.fromFile(file).getLines().toList

    for (line <- lines.take(50)) {
      try {
        val json = Json.parse(line)
        val id = (json \ "id").asOpt[String].getOrElse("?")
        val fen = (json \ "fen").asOpt[String].getOrElse("?")
        val primary = (json \ "primary").asOpt[String].getOrElse("?")
        
        if (primary == "Carlsbad" || primary == "IQPWhite" || primary == "HangingPawnsWhite") {
          val fenFull = Fen.Full(fen)
          val posOpt = Fen.read(Standard, fenFull)
          val boardOpt = posOpt.map(_.board)
          val sideToMove = posOpt.map(_.color).getOrElse(Color.White)
          val featuresOpt = PositionAnalyzer.extractFeatures(fen, 1)
          
          (boardOpt, featuresOpt) match {
            case (Some(board), Some(features)) =>
              val profile = PawnStructureClassifier.classify(features, board, sideToMove)
              println(s"ID: $id")
              println(s"Expected: $primary")
              println(s"Actual: ${profile.primary}")
              println(s"Confidence: ${profile.confidence}")
              println(s"CenterState: ${profile.centerState}")
              println(s"Evidence: ${profile.evidenceCodes.mkString(", ")}")
              
              if (profile.primary.toString != primary) {
                println(s"--- Features Mismatch Debug ---")
                println(s"White IQP: ${features.pawns.whiteIQP}")
                println(s"Black IQP: ${features.pawns.blackIQP}")
                println(s"White Hanging: ${features.pawns.whiteHangingPawns}")
                println(s"Black Hanging: ${features.pawns.blackHangingPawns}")
                
                // Carlsbad debug
                val hasWd4 = board.pieceAt(chess.Square.D4).contains(chess.Piece(Color.White, chess.Pawn))
                val hasBd5 = board.pieceAt(chess.Square.D5).contains(chess.Piece(Color.Black, chess.Pawn))
                val hasBc6 = board.pieceAt(chess.Square.C6).contains(chess.Piece(Color.Black, chess.Pawn))
                val hasBe6 = board.pieceAt(chess.Square.E6).contains(chess.Piece(Color.Black, chess.Pawn))
                val hasBe7 = board.pieceAt(chess.Square.E7).contains(chess.Piece(Color.Black, chess.Pawn))
                val whitePawns = board.byPiece(Color.White, chess.Pawn).squares
                val whiteByFile = whitePawns.groupBy(_.file).view.mapValues(_.size).toMap
                val cFileW = whiteByFile.getOrElse(chess.File.C, 0)
                println(s"Carlsbad features: Wd4=$hasWd4, Bd5=$hasBd5, Bc6=$hasBc6, Be6=$hasBe6, Be7=$hasBe7, cFileW=$cFileW")
              }
              
              println("-" * 40)
            case _ =>
              println(s"Failed to parse FEN: $fen")
          }
        }
      } catch {
        case e: Exception => println(s"Parse error: ${e.getMessage}")
      }
    }
  }
}

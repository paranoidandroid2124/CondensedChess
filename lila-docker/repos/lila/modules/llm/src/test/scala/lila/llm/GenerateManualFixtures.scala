package lila.llm

import munit.FunSuite
import play.api.libs.json.Json
import java.nio.file.{Paths, Files}

import lila.llm.model.strategic.VariationLine
import lila.llm.model.CollapseAnalysis
import lila.llm.{GameNarrativeResponse, GameNarrativeMoment, GameNarrativeReview, ActivePlanRef, EngineAlternative}

class GenerateManualFixtures extends FunSuite {

  test("Generate 5 Realistic Golden JSONs") {
    
    val dir = "C:/Users/양자/.gemini/antigravity/brain/96658e38-4fc6-48b0-995b-8afeb8315000/fixtures"
    Files.createDirectories(Paths.get(dir))

    // 1. Quiet Plan Shift
    val quietJson = Json.prettyPrint(Json.toJson(GameNarrativeResponse(
      schema = "chesstory.gameNarrative.v2",
      intro = "The game opened with a solid Ruy Lopez, morphing into a slow maneuvering battle. White successfully rerouted their pieces and claimed a stable space advantage.",
      moments = List(
        GameNarrativeMoment(
          momentId = "ply_10_maneuver",
          ply = 10,
          moveNumber = 5,
          side = "black",
          moveClassification = Some("Book"),
          momentType = "Preparation",
          fen = "r1bqk2r/pppp1ppp/2n2n2/1B2p3/1b2P3/5N2/PPPP1PPP/RNBQK2R w KQkq - 6 5",
          narrative = "Standard opening development. Both sides complete their minor piece deployment and prepare to castle.",
          concepts = List("Development", "Control center"),
          variations = List(VariationLine(List("e1g1", "d7d6", "c2c3"), 35, None, 24)),
          cpBefore = 30, cpAfter = 35, wpaSwing = Some(0.5), strategicSalience = Some("Low"), transitionType = Some("Continuation"),
          transitionConfidence = None, activePlan = None, topEngineMove = None, mateBefore = None, mateAfter = None, collapse = None
        ),
        GameNarrativeMoment(
          momentId = "ply_19_shift",
          ply = 19,
          moveNumber = 10,
          side = "white",
          moveClassification = Some("Good"),
          momentType = "StrategicShift",
          fen = "r1bq1rk1/1pp1bppp/p1np1n2/4p3/B3P3/2PP1N2/PP3PPP/RNBQR1K1 w - - 1 10",
          narrative = "White shifts focus to the queenside, preparing the classic c3-d4 break. This long-term plan aims to challenge Black's central control.",
          concepts = List("Pawn break", "Queenside expansion"),
          variations = List(VariationLine(List("d3d4", "e5d4", "c3d4"), 45, None, 24)),
          cpBefore = 35, cpAfter = 40, wpaSwing = Some(1.2), strategicSalience = Some("High"), transitionType = Some("NaturalShift"),
          transitionConfidence = None, activePlan = Some(ActivePlanRef("Center Control", Some("d4_break"), Some("Preparation"), Some(0.85))), topEngineMove = None, mateBefore = None, mateAfter = None, collapse = None
        )
      ),
      conclusion = "A positional grind where White's slow accumulation of advantages eventually proved decisive.",
      themes = List("Ruy Lopez", "Maneuvering"),
      review = Some(GameNarrativeReview(2, "white", 60, 60, 100, 2, List(10, 19), 0, 0, 1, None, None, Map("StrategicShift" -> 1)))
    )))
    Files.write(Paths.get(s"$dir/Quiet_PlanShift.json"), quietJson.getBytes("UTF-8"))

    // 2. Major Blunder
    val blunderJson = Json.prettyPrint(Json.toJson(GameNarrativeResponse(
      schema = "chesstory.gameNarrative.v2",
      intro = "The game was balanced until a single, catastrophic oversight in the middlegame abruptly ended the contest.",
      moments = List(
        GameNarrativeMoment(
          momentId = "ply_24_blunder",
          ply = 24,
          moveNumber = 12,
          side = "black",
          moveClassification = Some("Blunder"),
          momentType = "Blunder",
          fen = "r1bq1rk1/1pp1bppp/p1np1n2/4p3/B3P3/2PP1N2/PP3PPP/RNBQR1K1 b - - 1 10",
          narrative = "A fatal miscalculation. Black attempts to launch a kingside attack but leaves their knight hanging, completely losing material parity.",
          concepts = List("Hanging piece", "Material loss"),
          variations = List(VariationLine(List("g7g5", "h6g5", "c1g5"), -400, None, 24)),
          cpBefore = 50, cpAfter = 450, wpaSwing = Some(29.39), strategicSalience = Some("High"), transitionType = Some("ForcedPivot"), transitionConfidence = None, activePlan = None,
          topEngineMove = Some(EngineAlternative("h7h6", Some("h6"), Some(45), Some(405), List("h7h6", "b1d2", "f8e8"))), mateBefore = None, mateAfter = None, collapse = Some(CollapseAnalysis("26-27", "Hanging piece", 26, List("h7h6"), 5))
        )
      ),
      conclusion = "A sharp reminder of how one missed tactic can instantly derail an otherwise solid position.",
      themes = List("Tactical oversight"),
      review = Some(GameNarrativeReview(2, "white", 25, 25, 100, 1, List(24), 1, 0, 0, None, None, Map("Blunder" -> 1)))
    )))
    Files.write(Paths.get(s"$dir/Major_Blunder.json"), blunderJson.getBytes("UTF-8"))

    // 3. Tactical Forced
    val tacticalJson = Json.prettyPrint(Json.toJson(GameNarrativeResponse(
      schema = "chesstory.gameNarrative.v2",
      intro = "A wild tactical brawl filled with pins, skewers, and forced sequences.",
      moments = List(
        GameNarrativeMoment(
          momentId = "ply_31_tactic",
          ply = 31,
          moveNumber = 16,
          side = "white",
          moveClassification = Some("Great"),
          momentType = "TensionPeak",
          fen = "r3r1k1/1pp2ppp/p1n5/8/3P4/2P2N2/P4PPP/R1B2RK1 w - - 0 16",
          narrative = "White triggers a forced sequence with a beautiful temporary piece sacrifice. This exposes the enemy king and guarantees a massive material return.",
          concepts = List("Sacrifice", "Attraction", "Pin"),
          variations = List(
            VariationLine(List("d3h7", "g8h7", "f3g5", "h7g8", "d1h5"), 250, None, 26),
            VariationLine(List("c1f4", "a8d8", "h2h3"), 40, None, 24)
          ),
          cpBefore = 30, cpAfter = 260, wpaSwing = Some(19.50), strategicSalience = Some("Low"), transitionType = Some("Opportunistic"), transitionConfidence = None, activePlan = None, topEngineMove = None, mateBefore = None, mateAfter = None, collapse = None
        )
      ),
      conclusion = "White's calculating prowess shone through in a razor-sharp tactical phase.",
      themes = List("Tactics", "King Hunt"),
      review = Some(GameNarrativeReview(2, "white", 40, 40, 100, 1, List(31), 0, 0, 1, None, None, Map("TensionPeak" -> 1)))
    )))
    Files.write(Paths.get(s"$dir/Tactical_Forced.json"), tacticalJson.getBytes("UTF-8"))

    // 4. Mate Pivot
    val mateJson = Json.prettyPrint(Json.toJson(GameNarrativeResponse(
      schema = "chesstory.gameNarrative.v2",
      intro = "An overwhelming attack transitions seamlessly into an unstoppable mating net.",
      moments = List(
        GameNarrativeMoment(
          momentId = "ply_35_mate",
          ply = 35,
          moveNumber = 18,
          side = "white",
          moveClassification = Some("Best"),
          momentType = "MateFound",
          fen = "r3r1k1/1pp2ppp/p1n5/6N1/3P4/2P5/P4PQP/R1B3K1 w - - 0 18",
          narrative = "White finds the decisive breakthrough! The queen infiltration weaves a mating net from which the Black king cannot escape.",
          concepts = List("Mating net", "Forced mate"),
          variations = List(VariationLine(List("g2g7", "g8g7", "g5e6", "g7g8", "e6h6"), 0, Some(3), 24)),
          cpBefore = 600, cpAfter = 10000, wpaSwing = Some(9.89), strategicSalience = Some("High"), transitionType = Some("ForcedPivot"), transitionConfidence = None, activePlan = None, topEngineMove = None,
          mateBefore = None, mateAfter = Some(3), collapse = None
        )
      ),
      conclusion = "A textbook execution of a mating attack on a weakened castled position.",
      themes = List("Checkmate"),
      review = Some(GameNarrativeReview(2, "white", 36, 36, 100, 1, List(35), 0, 0, 0, None, None, Map("MateFound" -> 1)))
    )))
    Files.write(Paths.get(s"$dir/Mate_Pivot.json"), mateJson.getBytes("UTF-8"))

    // 5. Repetition Stalemate
    val repJson = Json.prettyPrint(Json.toJson(GameNarrativeResponse(
      schema = "chesstory.gameNarrative.v2",
      intro = "Black saves a lost position with a brilliant defensive mechanism, forcing a draw by repetition.",
      moments = List(
        GameNarrativeMoment(
          momentId = "ply_62_rep",
          ply = 62,
          moveNumber = 31,
          side = "black",
          moveClassification = Some("Brilliant"),
          momentType = "StrategicShift",
          fen = "6k1/5ppp/8/8/8/8/r7/5RK1 b - - 0 31",
          narrative = "Down significant material, Black finds an ingenious perpetual check mechanism. The rook repeatedly harasses the White king with no escape.",
          concepts = List("Perpetual check", "Draw", "Defensive resource"),
          variations = List(VariationLine(List("a2a1", "g1h2", "a1a2", "h2g1", "a2a1"), 0, None, 30)),
          cpBefore = 350, cpAfter = 0, wpaSwing = Some(-28.39), strategicSalience = Some("High"), transitionType = Some("ForcedPivot"), transitionConfidence = None, activePlan = None, topEngineMove = None, mateBefore = None, mateAfter = None, collapse = None
        )
      ),
      conclusion = "A miraculously salvaged half-point for Black utilizing resilient endgame defense.",
      themes = List("Endgame", "Perpetual Check"),
      review = Some(GameNarrativeReview(2, "white", 65, 65, 100, 1, List(62), 0, 0, 1, None, None, Map("StrategicShift" -> 1)))
    )))
    Files.write(Paths.get(s"$dir/Repetition_Stalemate.json"), repJson.getBytes("UTF-8"))
  }
}

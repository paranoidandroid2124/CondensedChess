package lila.commentary.analysis.semantic

import chess.{ Color, Square }
import chess.format.Fen
import chess.variant.Standard
import lila.commentary.{ StrategicIdeaKind, StrategyPack }
import lila.commentary.analysis.{ MoveReviewExchangeAnalyzer, StrategicIdeaSelector, StrategicIdeaSemanticContext }
import lila.commentary.model.strategic.VariationLine
import lila.commentary.model.strategic.WeakComplex
import lila.commentary.model.structure.{ CenterState, StructureId, StructureProfile }
import munit.FunSuite

class StrategicSemanticObservationPipelineTest extends FunSuite:

  private def boardFromFen(fen: String) =
    Fen.read(Standard, Fen.Full(fen)).map(_.board).getOrElse(fail(s"invalid FEN: $fen"))

  test("minority producer emits typed selector source plus support-only target pressure facts") {
    val fen = "4k3/pp3ppp/2p5/3p4/1P1P4/4P3/P4PPP/4K3 w - - 0 1"
    val semantic =
      StrategicIdeaSemanticContext(
        sideToMove = "white",
        board = Some(boardFromFen(fen)),
        fen = fen,
        structuralWeaknesses = List(
          WeakComplex(
            color = Color.Black,
            squares = List(Square.C6),
            isOutpost = false,
            cause = "Minority-attack target on c6"
          )
        ),
        structureProfile = Some(
          StructureProfile(
            primary = StructureId.Carlsbad,
            confidence = 0.98,
            alternatives = Nil,
            centerState = CenterState.Locked,
            evidenceCodes = List("structure_carlsbad")
          )
        )
      )

    val observations =
      StrategicSemanticObservationPipeline.collect(StrategyPack(sideToMove = "white"), semantic)

    assert(
      observations.exists(observation =>
        observation.id == StrategicObservationIds.SemanticObservationId.MinorityAttackSemantic &&
          observation.source.contains(StrategicObservationIds.EvidenceSourceId.MinorityAttackSemantic) &&
          observation.wireEvidenceRefs.contains("source:minority_attack_semantic") &&
          observation.wireEvidenceRefs.contains("target_pressure_semantic") &&
          !observation.wireEvidenceRefs.contains("source:target_pressure_semantic")
      ),
      clues(observations.map(_.wireEvidenceRefs))
    )
  }

  test("relation producer emits defender-trade semantic evidence without exchange-square fallback") {
    val fen = "3k1b1r/p2b1ppp/1n3n2/4p3/8/1R4P1/P1QPqPBP/2B2RK1 w - - 0 17"
    val semantic =
      StrategicIdeaSemanticContext(
        sideToMove = "white",
        fen = fen,
        playedMove = Some("c1a3"),
        board = Some(boardFromFen(fen)),
        structuralWeaknesses = List(
          WeakComplex(
            color = Color.Black,
            squares = List(Square.C5),
            isOutpost = false,
            cause = "defended local target"
          )
        ),
        engineVariations = List(VariationLine(moves = List("c1a3", "f8a3", "b3a3", "h8h7"), scoreCp = 44))
      )

    val observations =
      StrategicSemanticObservationPipeline.collect(StrategyPack(sideToMove = "white"), semantic)
    val defenderTrade =
      observations.find(_.id == StrategicObservationIds.SemanticObservationId.DefenderTradeSemantic)

    assertEquals(defenderTrade.flatMap(_.source), Some(StrategicObservationIds.EvidenceSourceId.RemovingTheDefender))
    assertEquals(defenderTrade.map(_.focusSquares), Some(List("c5", "a3")))
    assert(defenderTrade.exists(_.wireEvidenceRefs.contains("defender_trade_semantic")), clues(defenderTrade))
    assert(defenderTrade.exists(_.wireEvidenceRefs.contains("defended_target:c5")), clues(defenderTrade))
    assert(!defenderTrade.exists(_.wireEvidenceRefs.contains("defended_target:a3")), clues(defenderTrade))

    val ideas = StrategicIdeaSelector.select(StrategyPack(sideToMove = "white"), semantic)
    assert(
      ideas.exists(idea =>
        idea.kind == StrategicIdeaKind.FavorableTradeOrTransformation &&
          idea.evidenceRefs.contains("source:removing_the_defender") &&
          idea.evidenceRefs.contains("defender_trade_semantic")
      ),
      clues(ideas)
    )

    val missingPlayedMove =
      StrategicSemanticObservationPipeline.collect(StrategyPack(sideToMove = "white"), semantic.copy(playedMove = None))

    assert(
      missingPlayedMove.forall(_.id != StrategicObservationIds.SemanticObservationId.DefenderTradeSemantic),
      clues(missingPlayedMove)
    )
  }

  test("relation producer requires legal replayed bishop liquidation") {
    val fen = "5b2/4k1pp/8/8/3P4/1R2P3/P4PPP/2B3K1 w - - 0 1"
    val semantic =
      StrategicIdeaSemanticContext(
        sideToMove = "white",
        fen = fen,
        playedMove = Some("c1a3"),
        board = Some(boardFromFen(fen)),
        engineVariations = List(VariationLine(moves = List("c1a3", "e7f7", "a3f8", "f7f8", "h1h2"), scoreCp = 38))
      )

    val observations =
      StrategicSemanticObservationPipeline.collect(StrategyPack(sideToMove = "white"), semantic)
    val liquidation =
      observations.find(_.id == StrategicObservationIds.SemanticObservationId.BadPieceLiquidationSemantic)

    assertEquals(
      liquidation.flatMap(_.source),
      Some(StrategicObservationIds.EvidenceSourceId.CaptureExchangeTransformation)
    )
    assertEquals(liquidation.map(_.focusSquares), Some(List("c1", "f8")))
    assert(liquidation.exists(_.wireEvidenceRefs.contains("bad_piece_liquidation_semantic")), clues(liquidation))
    assert(liquidation.exists(_.wireEvidenceRefs.contains("bad_piece:c1")), clues(liquidation))

    val withoutRecapture =
      semantic.copy(engineVariations = List(VariationLine(moves = List("c1a3", "e7f7", "a3f8", "g7g6"), scoreCp = 38)))
    val rejected =
      StrategicSemanticObservationPipeline.collect(StrategyPack(sideToMove = "white"), withoutRecapture)

    assert(
      rejected.forall(_.id != StrategicObservationIds.SemanticObservationId.BadPieceLiquidationSemantic),
      clues(rejected)
    )

    val missingPlayedMove =
      StrategicSemanticObservationPipeline.collect(StrategyPack(sideToMove = "white"), semantic.copy(playedMove = None))

    assert(
      missingPlayedMove.forall(_.id != StrategicObservationIds.SemanticObservationId.BadPieceLiquidationSemantic),
      clues(missingPlayedMove)
    )
  }

  test("relation producer emits overload relation from the shared replay witness") {
    val fen = "k7/7p/5n2/3p4/8/8/8/3Q2K1 w - - 0 1"
    val semantic =
      StrategicIdeaSemanticContext(
        sideToMove = "white",
        fen = fen,
        playedMove = Some("d1d3"),
        board = Some(boardFromFen(fen)),
        structuralWeaknesses = List(
          WeakComplex(
            color = Color.Black,
            squares = List(Square.D5, Square.H7),
            isOutpost = false,
            cause = "overload targets"
          )
        ),
        engineVariations = List(VariationLine(moves = List("d1d3"), scoreCp = 26))
      )

    val observations =
      StrategicSemanticObservationPipeline.collect(StrategyPack(sideToMove = "white"), semantic)
    val overload =
      observations.find(_.id == StrategicObservationIds.SemanticObservationId.OverloadSemantic)

    assertEquals(overload.flatMap(_.source), Some(StrategicObservationIds.EvidenceSourceId.OverloadRelation))
    assert(overload.exists(_.wireEvidenceRefs.contains("source:overload_relation")), clues(overload))
    assert(overload.exists(_.wireEvidenceRefs.exists(_.contains("defender:f6"))), clues(overload))
    assert(
      StrategicSemanticObservationPipeline
        .collect(StrategyPack(sideToMove = "white"), semantic.copy(playedMove = None))
        .forall(_.id != StrategicObservationIds.SemanticObservationId.OverloadSemantic)
    )

    val ideas =
      StrategicIdeaSelector.select(
        StrategyPack(sideToMove = "white"),
        semantic.copy(board = None, structuralWeaknesses = Nil)
      )

    assert(
      ideas.exists(idea =>
        idea.kind == StrategicIdeaKind.FavorableTradeOrTransformation &&
          idea.evidenceRefs.contains("source:overload_relation") &&
          idea.evidenceRefs.contains("overload_semantic")
      ),
      clues(ideas)
    )

    val doubleCheckFen = "4k3/8/8/8/4N3/8/8/4R1K1 w - - 0 1"
    val doubleCheckSemantic =
      StrategicIdeaSemanticContext(
        sideToMove = "white",
        fen = doubleCheckFen,
        playedMove = Some("e4f6"),
        engineVariations = List(VariationLine(moves = List("e4f6"), scoreCp = 130))
      )
    val doubleCheckObservations =
      StrategicSemanticObservationPipeline.collect(StrategyPack(sideToMove = "white"), doubleCheckSemantic)
    val doubleCheck =
      doubleCheckObservations.find(_.id == StrategicObservationIds.SemanticObservationId.DoubleCheckSemantic)

    assertEquals(doubleCheck.flatMap(_.source), Some(StrategicObservationIds.EvidenceSourceId.DoubleCheckRelation))
    assert(doubleCheck.exists(_.wireEvidenceRefs.contains("source:double_check_relation")), clues(doubleCheck))
    assert(doubleCheck.exists(_.wireEvidenceRefs.contains("king:e8")), clues(doubleCheck))

    val doubleCheckIdeas = StrategicIdeaSelector.select(StrategyPack(sideToMove = "white"), doubleCheckSemantic)
    assert(
      doubleCheckIdeas.exists(idea =>
        idea.kind == StrategicIdeaKind.FavorableTradeOrTransformation &&
          idea.relationKind.contains(MoveReviewExchangeAnalyzer.RelationKind.DoubleCheck) &&
          idea.targetSquare.contains("e8") &&
          idea.evidenceRefs.contains("source:double_check_relation") &&
          idea.evidenceRefs.contains("double_check_semantic")
      ),
      clues(doubleCheckIdeas)
    )

    val backRankMateFen = "6k1/5ppp/8/8/8/8/8/4R1K1 w - - 0 1"
    val backRankMateSemantic =
      StrategicIdeaSemanticContext(
        sideToMove = "white",
        fen = backRankMateFen,
        playedMove = Some("e1e8"),
        engineVariations = List(VariationLine(moves = List("e1e8"), scoreCp = 9999))
      )
    val backRankMateObservations =
      StrategicSemanticObservationPipeline.collect(StrategyPack(sideToMove = "white"), backRankMateSemantic)
    val backRankMate =
      backRankMateObservations.find(_.id == StrategicObservationIds.SemanticObservationId.BackRankMateSemantic)

    assertEquals(backRankMate.flatMap(_.source), Some(StrategicObservationIds.EvidenceSourceId.BackRankMateRelation))
    assert(backRankMate.exists(_.wireEvidenceRefs.contains("source:back_rank_mate_relation")), clues(backRankMate))
    assert(backRankMate.exists(_.wireEvidenceRefs.contains("mating_move:e1e8")), clues(backRankMate))

    val backRankMateIdeas = StrategicIdeaSelector.select(StrategyPack(sideToMove = "white"), backRankMateSemantic)
    assert(
      backRankMateIdeas.exists(idea =>
        idea.kind == StrategicIdeaKind.KingAttackBuildUp &&
          idea.relationKind.contains(MoveReviewExchangeAnalyzer.RelationKind.BackRankMate) &&
          idea.targetSquare.contains("g8") &&
          idea.evidenceRefs.contains("source:back_rank_mate_relation") &&
          idea.evidenceRefs.contains("back_rank_mate_semantic")
      ),
      clues(backRankMateIdeas)
    )

    val mateNetFen = "6rk/6pp/7N/8/8/8/8/6K1 w - - 0 1"
    val mateNetSemantic =
      StrategicIdeaSemanticContext(
        sideToMove = "white",
        fen = mateNetFen,
        playedMove = Some("h6f7"),
        engineVariations = List(VariationLine(moves = List("h6f7"), scoreCp = 9999))
      )
    val mateNetObservations =
      StrategicSemanticObservationPipeline.collect(StrategyPack(sideToMove = "white"), mateNetSemantic)
    val mateNet =
      mateNetObservations.find(_.id == StrategicObservationIds.SemanticObservationId.MateNetSemantic)

    assertEquals(mateNet.flatMap(_.source), Some(StrategicObservationIds.EvidenceSourceId.MateNet))
    assert(mateNet.exists(_.wireEvidenceRefs.contains("source:mate_net")), clues(mateNet))
    assert(mateNet.exists(_.wireEvidenceRefs.contains("pattern:smothered_mate")), clues(mateNet))

    val mateNetIdeas = StrategicIdeaSelector.select(StrategyPack(sideToMove = "white"), mateNetSemantic)
    assert(
      mateNetIdeas.exists(idea =>
        idea.kind == StrategicIdeaKind.KingAttackBuildUp &&
          idea.relationKind.contains(MoveReviewExchangeAnalyzer.RelationKind.MateNet) &&
          idea.targetSquare.contains("h8") &&
          idea.evidenceRefs.contains("source:mate_net") &&
          idea.evidenceRefs.contains("mate_net_semantic")
      ),
      clues(mateNetIdeas)
    )

    val greekGiftFen = "6k1/7p/8/8/8/3B1N2/8/3QK3 w - - 0 1"
    val greekGiftLine = List("d3h7", "g8h7", "f3g5", "h7g8", "d1h5")
    val greekGiftSemantic =
      StrategicIdeaSemanticContext(
        sideToMove = "white",
        fen = greekGiftFen,
        playedMove = Some("d3h7"),
        engineVariations = List(VariationLine(moves = greekGiftLine, scoreCp = 120))
      )
    val greekGiftObservations =
      StrategicSemanticObservationPipeline.collect(StrategyPack(sideToMove = "white"), greekGiftSemantic)
    val greekGift =
      greekGiftObservations.find(_.id == StrategicObservationIds.SemanticObservationId.GreekGiftSemantic)

    assertEquals(greekGift.flatMap(_.source), Some(StrategicObservationIds.EvidenceSourceId.GreekGiftRelation))
    assert(greekGift.exists(_.wireEvidenceRefs.contains("source:greek_gift_relation")), clues(greekGift))
    assert(greekGift.exists(_.wireEvidenceRefs.contains("entry_move:d3h7")), clues(greekGift))

    val greekGiftIdeas = StrategicIdeaSelector.select(StrategyPack(sideToMove = "white"), greekGiftSemantic)
    assert(
      greekGiftIdeas.exists(idea =>
        idea.kind == StrategicIdeaKind.KingAttackBuildUp &&
          idea.relationKind.contains(MoveReviewExchangeAnalyzer.RelationKind.GreekGift) &&
          idea.targetSquare.contains("h7") &&
          idea.evidenceRefs.contains("source:greek_gift_relation") &&
          idea.evidenceRefs.contains("greek_gift_semantic")
      ),
      clues(greekGiftIdeas)
    )

    val forkFen = "k7/4r3/8/8/3N3q/8/8/6K1 w - - 0 1"
    val forkSemantic =
      StrategicIdeaSemanticContext(
        sideToMove = "white",
        fen = forkFen,
        playedMove = Some("d4f5"),
        structuralWeaknesses = List(
          WeakComplex(
            color = Color.Black,
            squares = List(Square.E7, Square.H4),
            isOutpost = false,
            cause = "fork targets"
          )
        ),
        engineVariations = List(VariationLine(moves = List("d4f5"), scoreCp = 90))
      )
    val forkObservations =
      StrategicSemanticObservationPipeline.collect(StrategyPack(sideToMove = "white"), forkSemantic)
    val fork =
      forkObservations.find(_.id == StrategicObservationIds.SemanticObservationId.ForkSemantic)

    assertEquals(fork.flatMap(_.source), Some(StrategicObservationIds.EvidenceSourceId.ForkRelation))
    assert(fork.exists(_.wireEvidenceRefs.contains("source:fork_relation")), clues(fork))
    assert(fork.exists(_.wireEvidenceRefs.contains("target:h4:queen")), clues(fork))

    val forkIdeas = StrategicIdeaSelector.select(StrategyPack(sideToMove = "white"), forkSemantic)
    assert(
      forkIdeas.exists(idea =>
        idea.kind == StrategicIdeaKind.FavorableTradeOrTransformation &&
          idea.evidenceRefs.contains("source:fork_relation") &&
          idea.evidenceRefs.contains("fork_semantic")
      ),
      clues(forkIdeas)
    )

    val hangingFen = "k7/8/8/5b2/2B5/8/8/6K1 w - - 0 1"
    val hangingSemantic =
      StrategicIdeaSemanticContext(
        sideToMove = "white",
        fen = hangingFen,
        playedMove = Some("c4d3"),
        structuralWeaknesses = List(
          WeakComplex(
            color = Color.Black,
            squares = List(Square.F5),
            isOutpost = false,
            cause = "hanging target"
          )
        ),
        engineVariations = List(VariationLine(moves = List("c4d3"), scoreCp = 48))
      )
    val hangingObservations =
      StrategicSemanticObservationPipeline.collect(StrategyPack(sideToMove = "white"), hangingSemantic)
    val hanging =
      hangingObservations.find(_.id == StrategicObservationIds.SemanticObservationId.HangingPieceSemantic)

    assertEquals(hanging.flatMap(_.source), Some(StrategicObservationIds.EvidenceSourceId.HangingPieceRelation))
    assert(hanging.exists(_.wireEvidenceRefs.contains("source:hanging_piece_relation")), clues(hanging))
    assert(hanging.exists(_.wireEvidenceRefs.contains("undefended_target")), clues(hanging))

    val hangingIdeas = StrategicIdeaSelector.select(StrategyPack(sideToMove = "white"), hangingSemantic)
    assert(
      hangingIdeas.exists(idea =>
        idea.kind == StrategicIdeaKind.FavorableTradeOrTransformation &&
          idea.evidenceRefs.contains("source:hanging_piece_relation") &&
          idea.evidenceRefs.contains("hanging_piece_semantic")
      ),
      clues(hangingIdeas)
    )
  }

  test("relation producer carries deflection and discovered-attack geometry into selector signals") {
    val deflectionFen = "3k1b1r/p2b1ppp/1n3n2/4p3/8/1R4P1/P1QPqPBP/2B2RK1 w - - 0 17"
    val deflectionSemantic =
      StrategicIdeaSemanticContext(
        sideToMove = "white",
        fen = deflectionFen,
        playedMove = Some("c1a3"),
        board = Some(boardFromFen(deflectionFen)),
        structuralWeaknesses = List(
          WeakComplex(
            color = Color.Black,
            squares = List(Square.G7),
            isOutpost = false,
            cause = "deflection target"
          )
        ),
        engineVariations = List(VariationLine(moves = List("c1a3", "f8a3"), scoreCp = 32))
      )
    val deflectionObservations =
      StrategicSemanticObservationPipeline.collect(StrategyPack(sideToMove = "white"), deflectionSemantic)
    val deflection =
      deflectionObservations.find(_.id == StrategicObservationIds.SemanticObservationId.DeflectionSemantic)

    assertEquals(deflection.flatMap(_.source), Some(StrategicObservationIds.EvidenceSourceId.DeflectionRelation))
    assertEquals(deflection.flatMap(_.targetSquare), Some("g7"))
    assertEquals(deflection.map(_.focusSquares), Some(List("g7", "f8", "a3")))
    assert(deflection.exists(_.wireEvidenceRefs.contains("source:deflection_relation")), clues(deflection))
    assert(deflection.exists(_.wireEvidenceRefs.contains("defender:f8")), clues(deflection))
    assert(deflection.exists(_.wireEvidenceRefs.contains("attacker:a3")), clues(deflection))

    val deflectionIdeas =
      StrategicIdeaSelector.select(StrategyPack(sideToMove = "white"), deflectionSemantic.copy(board = None))
    assert(
      deflectionIdeas.exists(idea =>
        idea.kind == StrategicIdeaKind.FavorableTradeOrTransformation &&
          idea.relationKind.contains(MoveReviewExchangeAnalyzer.RelationKind.Deflection) &&
          idea.targetSquare.contains("g7") &&
          idea.relationFocusSquares == List("g7", "f8", "a3") &&
          idea.evidenceRefs.contains("source:deflection_relation") &&
          idea.evidenceRefs.contains("deflection_semantic")
      ),
      clues(deflectionIdeas)
    )

    val discoveredFen = "k7/7q/8/8/8/3N4/8/1B4K1 w - - 0 1"
    val discoveredSemantic =
      StrategicIdeaSemanticContext(
        sideToMove = "white",
        fen = discoveredFen,
        playedMove = Some("d3f4"),
        board = Some(boardFromFen(discoveredFen)),
        engineVariations = List(VariationLine(moves = List("d3f4"), scoreCp = 80))
      )
    val discoveredObservations =
      StrategicSemanticObservationPipeline.collect(StrategyPack(sideToMove = "white"), discoveredSemantic)
    val discovered =
      discoveredObservations.find(_.id == StrategicObservationIds.SemanticObservationId.DiscoveredAttackSemantic)

    assertEquals(discovered.flatMap(_.source), Some(StrategicObservationIds.EvidenceSourceId.DiscoveredAttackRelation))
    assertEquals(discovered.flatMap(_.targetSquare), Some("h7"))
    assertEquals(discovered.map(_.focusSquares), Some(List("b1", "d3", "h7")))
    assert(discovered.exists(_.wireEvidenceRefs.contains("source:discovered_attack_relation")), clues(discovered))
    assert(discovered.exists(_.wireEvidenceRefs.contains("attacker:b1")), clues(discovered))
    assert(discovered.exists(_.wireEvidenceRefs.contains("cleared_square:d3")), clues(discovered))

    val discoveredIdeas =
      StrategicIdeaSelector.select(StrategyPack(sideToMove = "white"), discoveredSemantic.copy(board = None))
    assert(
      discoveredIdeas.exists(idea =>
        idea.kind == StrategicIdeaKind.FavorableTradeOrTransformation &&
          idea.relationKind.contains(MoveReviewExchangeAnalyzer.RelationKind.DiscoveredAttack) &&
          idea.targetSquare.contains("h7") &&
          idea.relationFocusSquares == List("b1", "d3", "h7") &&
          idea.evidenceRefs.contains("source:discovered_attack_relation") &&
          idea.evidenceRefs.contains("discovered_attack_semantic")
      ),
      clues(discoveredIdeas)
    )
  }

  test("relation producer emits line-occupation relations through the shared catalog") {
    val xrayFen = "k7/8/6q1/5n2/8/8/8/1B5K w - - 0 1"
    val xraySemantic =
      StrategicIdeaSemanticContext(
        sideToMove = "white",
        fen = xrayFen,
        playedMove = Some("b1e4"),
        engineVariations = List(VariationLine(moves = List("b1e4"), scoreCp = 42))
      )
    val xrayObservations =
      StrategicSemanticObservationPipeline.collect(StrategyPack(sideToMove = "white"), xraySemantic)
    val xray =
      xrayObservations.find(_.id == StrategicObservationIds.SemanticObservationId.XRaySemantic)

    assertEquals(xray.flatMap(_.source), Some(StrategicObservationIds.EvidenceSourceId.XRayRelation))
    assert(xray.exists(_.wireEvidenceRefs.contains("source:xray_relation")), clues(xray))
    assert(xray.exists(_.wireEvidenceRefs.contains("blocker:f5")), clues(xray))

    val ideas = StrategicIdeaSelector.select(StrategyPack(sideToMove = "white"), xraySemantic)
    val xrayIdea =
      ideas.find(idea =>
        idea.kind == StrategicIdeaKind.LineOccupation &&
          idea.evidenceRefs.contains("source:xray_relation") &&
          idea.evidenceRefs.contains("xray_semantic")
      )

    assert(xrayIdea.nonEmpty, clues(ideas))
    assertEquals(xrayIdea.flatMap(_.relationKind), Some(MoveReviewExchangeAnalyzer.RelationKind.XRay), clues(ideas))
    assertEquals(xrayIdea.map(_.evidenceRefs.take(2)), Some(List("source:xray_relation", "xray_semantic")), clues(ideas))

    val xrayTargetMismatch =
      xraySemantic.copy(
        structuralWeaknesses = List(
          WeakComplex(
            color = Color.Black,
            squares = List(Square.H7),
            isOutpost = false,
            cause = "mismatched exact target"
          )
        )
      )
    val mismatchObservations =
      StrategicSemanticObservationPipeline.collect(StrategyPack(sideToMove = "white"), xrayTargetMismatch)

    assert(
      mismatchObservations.forall(_.id != StrategicObservationIds.SemanticObservationId.XRaySemantic),
      clues(mismatchObservations)
    )

    val clearanceFen = "k7/7q/8/8/8/3N4/8/1B4K1 w - - 0 1"
    val clearanceSemantic =
      StrategicIdeaSemanticContext(
        sideToMove = "white",
        fen = clearanceFen,
        playedMove = Some("d3f4"),
        structuralWeaknesses = List(
          WeakComplex(
            color = Color.Black,
            squares = List(Square.H7),
            isOutpost = false,
            cause = "clearance target"
          )
        ),
        engineVariations = List(VariationLine(moves = List("d3f4"), scoreCp = 80))
      )
    val clearanceObservations =
      StrategicSemanticObservationPipeline.collect(StrategyPack(sideToMove = "white"), clearanceSemantic)
    val clearance =
      clearanceObservations.find(_.id == StrategicObservationIds.SemanticObservationId.ClearanceSemantic)

    assertEquals(clearance.flatMap(_.source), Some(StrategicObservationIds.EvidenceSourceId.ClearanceRelation))
    assert(clearance.exists(_.wireEvidenceRefs.contains("source:clearance_relation")), clues(clearance))
    assert(clearance.exists(_.wireEvidenceRefs.contains("beneficiary:b1")), clues(clearance))

    val batteryFen = "k7/7p/8/8/8/8/8/1B1Q2K1 w - - 0 1"
    val batterySemantic =
      StrategicIdeaSemanticContext(
        sideToMove = "white",
        fen = batteryFen,
        playedMove = Some("d1d3"),
        structuralWeaknesses = List(
          WeakComplex(
            color = Color.Black,
            squares = List(Square.H7),
            isOutpost = false,
            cause = "battery target"
          )
        ),
        engineVariations = List(VariationLine(moves = List("d1d3"), scoreCp = 34))
      )
    val batteryObservations =
      StrategicSemanticObservationPipeline.collect(StrategyPack(sideToMove = "white"), batterySemantic)
    val battery =
      batteryObservations.find(_.id == StrategicObservationIds.SemanticObservationId.BatterySemantic)

    assertEquals(battery.flatMap(_.source), Some(StrategicObservationIds.EvidenceSourceId.BatteryRelation))
    assert(battery.exists(_.wireEvidenceRefs.contains("source:battery_relation")), clues(battery))
    assert(battery.exists(_.wireEvidenceRefs.contains("axis:diagonal")), clues(battery))

    val batteryIdeas = StrategicIdeaSelector.select(StrategyPack(sideToMove = "white"), batterySemantic)
    assert(
      batteryIdeas.exists(idea =>
        idea.kind == StrategicIdeaKind.LineOccupation &&
          idea.evidenceRefs.contains("source:battery_relation") &&
          idea.evidenceRefs.contains("battery_semantic")
      ),
      clues(batteryIdeas)
    )

    val pinFen = "4kb2/8/8/8/8/2N5/8/4K3 b - - 0 1"
    val pinSemantic =
      StrategicIdeaSemanticContext(
        sideToMove = "black",
        fen = pinFen,
        playedMove = Some("f8b4"),
        structuralWeaknesses = List(
          WeakComplex(
            color = Color.White,
            squares = List(Square.C3),
            isOutpost = false,
            cause = "pin target"
          )
        ),
        engineVariations = List(VariationLine(moves = List("f8b4"), scoreCp = 52))
      )
    val pinObservations =
      StrategicSemanticObservationPipeline.collect(StrategyPack(sideToMove = "black"), pinSemantic)
    val pin =
      pinObservations.find(_.id == StrategicObservationIds.SemanticObservationId.PinSemantic)

    assertEquals(pin.flatMap(_.source), Some(StrategicObservationIds.EvidenceSourceId.PinRelation))
    assert(pin.exists(_.wireEvidenceRefs.contains("source:pin_relation")), clues(pin))
    assert(pin.exists(_.wireEvidenceRefs.contains("absolute_pin")), clues(pin))

    val pinIdeas = StrategicIdeaSelector.select(StrategyPack(sideToMove = "black"), pinSemantic)
    assert(
      pinIdeas.exists(idea =>
        idea.kind == StrategicIdeaKind.LineOccupation &&
          idea.evidenceRefs.contains("source:pin_relation") &&
          idea.evidenceRefs.contains("pin_semantic")
      ),
      clues(pinIdeas)
    )

    val skewerFen = "r6k/8/8/8/8/8/7K/4Q2R b - - 0 1"
    val skewerSemantic =
      StrategicIdeaSemanticContext(
        sideToMove = "black",
        fen = skewerFen,
        playedMove = Some("a8a1"),
        structuralWeaknesses = List(
          WeakComplex(
            color = Color.White,
            squares = List(Square.E1),
            isOutpost = false,
            cause = "skewer target"
          )
        ),
        engineVariations = List(VariationLine(moves = List("a8a1"), scoreCp = 76))
      )
    val skewerObservations =
      StrategicSemanticObservationPipeline.collect(StrategyPack(sideToMove = "black"), skewerSemantic)
    val skewer =
      skewerObservations.find(_.id == StrategicObservationIds.SemanticObservationId.SkewerSemantic)

    assertEquals(skewer.flatMap(_.source), Some(StrategicObservationIds.EvidenceSourceId.SkewerRelation))
    assert(skewer.exists(_.wireEvidenceRefs.contains("source:skewer_relation")), clues(skewer))
    assert(skewer.exists(_.wireEvidenceRefs.contains("front_role:queen")), clues(skewer))

    val skewerIdeas = StrategicIdeaSelector.select(StrategyPack(sideToMove = "black"), skewerSemantic)
    assert(
      skewerIdeas.exists(idea =>
        idea.kind == StrategicIdeaKind.LineOccupation &&
          idea.evidenceRefs.contains("source:skewer_relation") &&
          idea.evidenceRefs.contains("skewer_semantic")
      ),
      clues(skewerIdeas)
    )

    val interferenceFen = "k2r4/8/8/3q1N2/8/8/8/3Q2K1 w - - 0 1"
    val interferenceSemantic =
      StrategicIdeaSemanticContext(
        sideToMove = "white",
        fen = interferenceFen,
        playedMove = Some("f5d6"),
        structuralWeaknesses = List(
          WeakComplex(
            color = Color.Black,
            squares = List(Square.D5),
            isOutpost = false,
            cause = "interference target"
          )
        ),
        engineVariations = List(VariationLine(moves = List("f5d6"), scoreCp = 48))
      )
    val interferenceObservations =
      StrategicSemanticObservationPipeline.collect(StrategyPack(sideToMove = "white"), interferenceSemantic)
    val interference =
      interferenceObservations.find(_.id == StrategicObservationIds.SemanticObservationId.InterferenceSemantic)

    assertEquals(interference.flatMap(_.source), Some(StrategicObservationIds.EvidenceSourceId.InterferenceRelation))
    assert(interference.exists(_.wireEvidenceRefs.contains("source:interference_relation")), clues(interference))
    assert(interference.exists(_.wireEvidenceRefs.contains("defender:d8")), clues(interference))

    val interferenceIdeas = StrategicIdeaSelector.select(StrategyPack(sideToMove = "white"), interferenceSemantic)
    assert(
      interferenceIdeas.exists(idea =>
        idea.kind == StrategicIdeaKind.LineOccupation &&
          idea.evidenceRefs.contains("source:interference_relation") &&
          idea.evidenceRefs.contains("interference_semantic")
      ),
      clues(interferenceIdeas)
    )
  }

  test("relation producer emits decoy relation from a legal lure-and-win branch") {
    val fen = "k7/8/8/3q4/5N2/8/4B3/3Q2K1 w - - 0 1"
    val semantic =
      StrategicIdeaSemanticContext(
        sideToMove = "white",
        fen = fen,
        playedMove = Some("f4d3"),
        structuralWeaknesses = List(
          WeakComplex(
            color = Color.Black,
            squares = List(Square.D3),
            isOutpost = false,
            cause = "decoy target"
          )
        ),
        engineVariations = List(VariationLine(moves = List("f4d3", "d5d3", "e2d3"), scoreCp = 120))
      )

    val observations =
      StrategicSemanticObservationPipeline.collect(StrategyPack(sideToMove = "white"), semantic)
    val decoy =
      observations.find(_.id == StrategicObservationIds.SemanticObservationId.DecoySemantic)

    assertEquals(decoy.flatMap(_.source), Some(StrategicObservationIds.EvidenceSourceId.DecoyRelation))
    assert(decoy.exists(_.wireEvidenceRefs.contains("source:decoy_relation")), clues(decoy))
    assert(decoy.exists(_.wireEvidenceRefs.contains("lured_from:d5")), clues(decoy))

    val ideas = StrategicIdeaSelector.select(StrategyPack(sideToMove = "white"), semantic)
    assert(
      ideas.exists(idea =>
        idea.kind == StrategicIdeaKind.FavorableTradeOrTransformation &&
          idea.evidenceRefs.contains("source:decoy_relation") &&
          idea.evidenceRefs.contains("decoy_semantic")
      ),
      clues(ideas)
    )
  }

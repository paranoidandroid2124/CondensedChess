package lila.commentary.analysis

import lila.commentary.model.ProbeResult
import lila.commentary.model.strategic.{ PvMove, VariationLine }
import munit.FunSuite
import scala.jdk.CollectionConverters.*

class MoveReviewExchangeAnalyzerTest extends FunSuite:

  test("engine raw PV moves override stale parsed move metadata") {
    val fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
    val line =
      VariationLine(
        moves = List("e2e4"),
        scoreCp = 18,
        parsedMoves = List(
          PvMove("d2d4", "d4", "d2", "d4", "P", isCapture = false, capturedPiece = None, givesCheck = false)
        )
      )

    assertEquals(MoveReviewExchangeAnalyzer.normalizedLineMoves(line), List("e2e4"))

    val replay =
      MoveReviewExchangeAnalyzer
        .boundedTopReplay(fen, List(line), maxPlies = 1)
        .getOrElse(fail("raw engine PV move should replay despite stale parsed metadata"))

    assertEquals(replay.map(_.uci), List("e2e4"))
    assertEquals(replay.head.move.dest.key, "e4")
  }

  test("queen-trade shield requires legal replayed queen capture and king recapture") {
    val fen = "3qk3/8/8/8/8/3Q4/8/6K1 w - - 0 1"
    val replay =
      MoveReviewExchangeAnalyzer
        .boundedTopReplay(fen, List(VariationLine(moves = List("d3d8", "e8d8"), scoreCp = 20)), maxPlies = 2)
        .getOrElse(fail("queen trade line should replay legally"))

    assertEquals(MoveReviewExchangeAnalyzer.queenTradeShieldLine(replay), Some(List("d3d8", "e8d8")))
  }

  test("queen-trade shield rejects legal non-king recaptures and illegal string geometry") {
    val rookRecaptureFen = "k2qr3/8/8/8/8/3Q4/8/6K1 w - - 0 1"
    val rookRecapture =
      MoveReviewExchangeAnalyzer
        .boundedTopReplay(
          rookRecaptureFen,
          List(VariationLine(moves = List("d3d8", "e8d8"), scoreCp = 20)),
          maxPlies = 2
        )
        .getOrElse(fail("rook recapture line should replay legally"))

    assertEquals(MoveReviewExchangeAnalyzer.queenTradeShieldLine(rookRecapture), None)

    val illegalSameDestination =
      MoveReviewExchangeAnalyzer.boundedTopReplay(
        rookRecaptureFen,
        List(VariationLine(moves = List("d3d8", "a8d8"), scoreCp = 20)),
        maxPlies = 2
      )

    assertEquals(illegalSameDestination, None)
  }

  test("immediate exchange square is based on legal capture and recapture") {
    val fen = "k2qr3/8/8/8/8/3Q4/8/6K1 w - - 0 1"
    val replay =
      MoveReviewExchangeAnalyzer
        .boundedTopReplay(fen, List(VariationLine(moves = List("d3d8", "e8d8"), scoreCp = 20)), maxPlies = 2)
        .getOrElse(fail("exchange line should replay legally"))

    assertEquals(MoveReviewExchangeAnalyzer.immediateExchangeSquare(replay), Some("d8"))
  }

  test("defender trade rejects a legal three-ply exchange without a removed defense relation") {
    val fen = "3k1b2/8/8/8/8/nR6/8/2B3K1 w - - 0 1"
    val replay =
      MoveReviewExchangeAnalyzer
        .boundedTopReplay(fen, List(VariationLine(moves = List("c1a3", "f8a3", "b3a3"), scoreCp = 12)), maxPlies = 3)
        .getOrElse(fail("three-ply exchange should replay legally"))

    assertEquals(
      MoveReviewExchangeAnalyzer.defenderTradeBranch(replay, "c1a3", explicitTargets = List("h7")),
      None
    )
  }

  test("defender trade requires a declared or structural target and never falls back to the exchange square") {
    val fen = "3k1b1r/p2b1ppp/1n3n2/4p3/8/1R4P1/P1QPqPBP/2B2RK1 w - - 0 17"
    val replay =
      MoveReviewExchangeAnalyzer
        .boundedTopReplay(fen, List(VariationLine(moves = List("c1a3", "f8a3", "b3a3"), scoreCp = 44)), maxPlies = 3)
        .getOrElse(fail("defender-trade branch should replay legally"))

    assertEquals(
      MoveReviewExchangeAnalyzer.defenderTradeBranch(replay, "c1a3", explicitTargets = Nil),
      None
    )

    val branch =
      MoveReviewExchangeAnalyzer
        .defenderTradeBranch(replay, "c1a3", explicitTargets = List("a3", "c5"))
        .getOrElse(fail("declared c5 target should prove defender trade"))

    assertEquals(branch.defenderSquare, "f8")
    assertEquals(branch.exchangeSquare, "a3")
    assertEquals(branch.targetSquare, "c5")
    assertEquals(branch.lineMoves, List("c1a3", "f8a3", "b3a3"))
    val relation =
      MoveReviewExchangeAnalyzer
        .defenderTradeRelationWitness(replay, "c1a3", explicitTargets = List("a3", "c5"))
        .getOrElse(fail("defender-trade relation witness should carry typed branch details"))
    assertEquals(relation.kind, MoveReviewExchangeAnalyzer.RelationKind.DefenderTrade)
    assertEquals(
      MoveReviewExchangeAnalyzer.defenderTradeBranchFromWitness(relation),
      Some(branch)
    )
    assertEquals(
      MoveReviewExchangeAnalyzer.defenderTradeBranch(replay, "c1a3", explicitTargets = List("a3", "bad")),
      None
    )
    assertEquals(
      MoveReviewExchangeAnalyzer.defenderTradeBranch(replay, "c1a3", explicitTargets = List("c5", "bad")),
      None
    )
  }

  test("defender trade rejects a replacement defender line that preserves the target defense count") {
    val fen = "k7/8/5b2/4b3/8/8/3Q4/1R4K1 w - - 0 1"
    val replay =
      MoveReviewExchangeAnalyzer
        .boundedTopReplay(fen, List(VariationLine(moves = List("b1b2", "e5b2", "d2b2"), scoreCp = 16)), maxPlies = 3)
        .getOrElse(fail("three-ply exchange with a replacement defender should replay legally"))

    assertEquals(
      MoveReviewExchangeAnalyzer.defenderTradeBranch(replay, "b1b2", explicitTargets = List("c3")),
      None
    )
  }

  test("defender trade can use a proven legal prefix without trusting a stale PV tail") {
    val fen = "3k1b1r/p2b1ppp/1n3n2/4p3/8/1R4P1/P1QPqPBP/2B2RK1 w - - 0 17"
    val line = VariationLine(moves = List("c1a3", "f8a3", "b3a3", "h8h7"), scoreCp = 44)

    assertEquals(MoveReviewExchangeAnalyzer.boundedTopReplay(fen, List(line), maxPlies = 4), None)

    val prefix =
      MoveReviewExchangeAnalyzer
        .boundedTopReplayPrefix(fen, List(line), minPlies = 3, maxPlies = 4)
        .getOrElse(fail("first three plies prove the defender trade before the stale tail"))
    val branch =
      MoveReviewExchangeAnalyzer
        .defenderTradeBranch(prefix, "c1a3", explicitTargets = List("c5"))
        .getOrElse(fail("legal prefix should preserve defender-trade proof"))

    assertEquals(prefix.map(_.uci), List("c1a3", "f8a3", "b3a3"))
    assertEquals(branch.targetSquare, "c5")
  }

  test("bad-piece liquidation rejects an open bishop even with same-color central pawns") {
    val fen = "5r2/4k3/8/8/3P4/2P5/8/2B3K1 w - - 0 1"
    val replay =
      MoveReviewExchangeAnalyzer
        .boundedTopReplay(
          fen,
          List(VariationLine(moves = List("c1a3", "e7f7", "a3f8", "f7f8"), scoreCp = 18)),
          maxPlies = 4
        )
        .getOrElse(fail("open-bishop exchange branch should replay legally"))

    assertEquals(MoveReviewExchangeAnalyzer.badPieceLiquidationBranch(replay, "c1a3"), None)
  }

  test("bad-piece liquidation accepts an immediate legal bishop trade") {
    val fen = "3k1b2/8/8/8/3P4/n3P3/8/2B3K1 w - - 0 1"
    val replay =
      MoveReviewExchangeAnalyzer
        .boundedTopReplay(
          fen,
          List(VariationLine(moves = List("c1a3", "f8a3"), scoreCp = 22)),
          maxPlies = 2
        )
        .getOrElse(fail("direct bad-bishop liquidation branch should replay legally"))

    val branch =
      MoveReviewExchangeAnalyzer
        .badPieceLiquidationBranch(replay, "c1a3")
        .getOrElse(fail("direct bishop capture and recapture should prove liquidation"))

    assertEquals(branch.badPieceSquare, "c1")
    assertEquals(branch.exchangeSquare, "a3")
    assertEquals(branch.lineMoves, List("c1a3", "f8a3"))
    val relation =
      MoveReviewExchangeAnalyzer
        .badPieceLiquidationRelationWitness(replay, "c1a3")
        .getOrElse(fail("bad-piece relation witness should carry typed branch details"))
    assertEquals(relation.kind, MoveReviewExchangeAnalyzer.RelationKind.BadPieceLiquidation)
    assertEquals(
      MoveReviewExchangeAnalyzer.badPieceLiquidationBranchFromWitness(relation),
      Some(branch)
    )
  }

  test("bad-piece liquidation can use a direct legal prefix without trusting a stale PV tail") {
    val fen = "3k1b2/8/8/8/3P4/n3P3/8/2B3K1 w - - 0 1"
    val line = VariationLine(moves = List("c1a3", "f8a3", "h1h2"), scoreCp = 22)

    assertEquals(MoveReviewExchangeAnalyzer.boundedTopReplay(fen, List(line), maxPlies = 3), None)

    val prefix =
      MoveReviewExchangeAnalyzer
        .boundedTopReplayPrefix(fen, List(line), minPlies = 2, maxPlies = 3)
        .getOrElse(fail("first two plies prove the direct bishop liquidation before the stale tail"))
    val branch =
      MoveReviewExchangeAnalyzer
        .badPieceLiquidationBranch(prefix, "c1a3")
        .getOrElse(fail("legal prefix should preserve bad-piece-liquidation proof"))

    assertEquals(prefix.map(_.uci), List("c1a3", "f8a3"))
    assertEquals(branch.exchangeSquare, "a3")
  }

  test("runtime consumers use relation branch extraction helpers instead of raw detail keys") {
    val root = java.nio.file.Paths.get("").toAbsolutePath
    val analyzerPath =
      root.resolve("modules/commentaryCore/src/main/scala/lila/commentary/analysis/MoveReviewExchangeAnalyzer.scala")
    val policyPath =
      root.resolve("modules/commentaryCore/src/main/scala/lila/commentary/analysis/PlayerFacingTruthModePolicy.scala")
    val semanticObservationPath =
      root.resolve("modules/commentaryCore/src/main/scala/lila/commentary/analysis/semantic/StrategicSemanticObservation.scala")
    val analyzerText = java.nio.file.Files.readString(analyzerPath)
    val policyText = java.nio.file.Files.readString(policyPath)
    val semanticObservationText = java.nio.file.Files.readString(semanticObservationPath)
    val checkedRuntimeFiles =
      java.nio.file.Files
        .walk(root.resolve("modules/commentaryCore/src/main/scala"))
        .iterator()
        .asScala
        .toList
        .filter(path => path.toString.endsWith(".scala") && path != analyzerPath)
    val runtimeRawAttributeReads =
      checkedRuntimeFiles.flatMap { path =>
        val rel = root.relativize(path).toString
        val text = java.nio.file.Files.readString(path)
        Option.when(text.contains("attributes.get("))(rel)
      }
    val leakedRelationDetailInternals =
      checkedRuntimeFiles.flatMap { path =>
        val rel = root.relativize(path).toString
        val text = java.nio.file.Files.readString(path)
        val rawKeys = List("defender_square", "bad_piece_square").flatMap { key =>
          Option.when(text.contains("\"" + key + "\""))(s"$rel:$key")
        }
        val directDetails =
          Option.when(text.contains("RelationDetails."))(s"$rel:RelationDetails")
        rawKeys ++ directDetails
      }

    assert(!analyzerText.contains("attributes: Map"), clues(analyzerPath))
    assert(!analyzerText.contains("RelationAttribute"), clues(analyzerPath))
    assert(analyzerText.contains("details: RelationDetails"), clues(analyzerPath))
    assertEquals(
      "kind = RelationKind\\.".r.findAllMatchIn(analyzerText).length,
      "details = RelationDetails\\.".r.findAllMatchIn(analyzerText).length
    )
    assertEquals(runtimeRawAttributeReads, Nil)
    assertEquals(leakedRelationDetailInternals, Nil)
    assert(policyText.contains("defenderTradeBranchFromWitness"), clues(policyPath))
    assert(policyText.contains("badPieceLiquidationBranchFromWitness"), clues(policyPath))
    assert(policyText.contains("ownerSeedTermsFromWitness"), clues(policyPath))
    assert(policyText.contains("transitionTermsFromWitness"), clues(policyPath))
    assert(analyzerText.contains("relationProjectionFromWitness(witness)"), clues(analyzerPath))
    assert(analyzerText.contains("projection.focusSquares"), clues(analyzerPath))
    assert(analyzerText.contains("projection.factTerms ++ extras"), clues(analyzerPath))
    assert(!analyzerText.contains("witness.facts ++ extras"), clues(analyzerPath))
    assert(!analyzerText.contains("witness.focusSquares ++"), clues(analyzerPath))
    assert(!analyzerText.contains("witness.targetSquare.toList ++"), clues(analyzerPath))
    assert(!policyText.contains("relation.facts"), clues(policyPath))
    assert(!policyText.contains("relation.focusSquares"), clues(policyPath))
    assert(!policyText.contains("relation.targetSquare"), clues(policyPath))
    assert(semanticObservationText.contains("relationProjectionFromWitness"), clues(semanticObservationPath))
    assert(!semanticObservationText.contains("relationFocusSquaresFromWitness(witness)"), clues(semanticObservationPath))
    assert(!semanticObservationText.contains("relationTargetSquareFromWitness(witness)"), clues(semanticObservationPath))
    assert(!semanticObservationText.contains("relationFactTermsFromWitness(witness)"), clues(semanticObservationPath))
    assert(!semanticObservationText.contains("focusSquares = witness.focusSquares"), clues(semanticObservationPath))
    assert(!semanticObservationText.contains("targetSquare = witness.targetSquare"), clues(semanticObservationPath))
    assert(!semanticObservationText.contains("witness.facts"), clues(semanticObservationPath))
  }

  test("implemented relation projection helpers cover every implemented kind") {
    val root = java.nio.file.Paths.get("").toAbsolutePath
    val analyzerPath =
      root.resolve("modules/commentaryCore/src/main/scala/lila/commentary/analysis/MoveReviewExchangeAnalyzer.scala")
    val analyzerText = java.nio.file.Files.readString(analyzerPath)
    def slice(from: String, until: String): String =
      val start = analyzerText.indexOf(from)
      val end = analyzerText.indexOf(until, start + from.length)
      assert(start >= 0, clues(from, analyzerPath))
      assert(end > start, clues(from, until, analyzerPath))
      analyzerText.substring(start, end)

    val focusProjection =
      slice("def relationFocusSquaresFromWitness", "def relationTargetSquareFromWitness")
    val targetProjection =
      slice("def relationTargetSquareFromWitness", "def relationFactTermsFromWitness")
    val factProjection =
      slice("def relationFactTermsFromWitness", "def ownerSeedTermsFromWitness")
    val implementedKindSourceNames =
      List(
        "DefenderTrade",
        "BadPieceLiquidation",
        "Overload",
        "Deflection",
        "DiscoveredAttack",
        "DoubleCheck",
        "BackRankMate",
        "MateNet",
        "GreekGift",
        "StalemateTrap",
        "PerpetualCheck",
        "Fork",
        "HangingPiece",
        "TrappedPiece",
        "XRay",
        "Clearance",
        "Battery",
        "Pin",
        "Skewer",
        "Interference",
        "Decoy"
      )
    def missingFrom(section: String): List[String] =
      implementedKindSourceNames.filterNot(kind => section.contains(s"RelationKind.$kind"))

    assertEquals(missingFrom(focusProjection), Nil)
    assertEquals(missingFrom(targetProjection), Nil)
    assertEquals(missingFrom(factProjection), Nil)
  }

  test("mismatched typed relation details do not fall back to raw witness fields") {
    val witness =
      MoveReviewExchangeAnalyzer.RelationWitness(
        kind = MoveReviewExchangeAnalyzer.RelationKind.Pin,
        focusSquares = List("a1", "a2", "a3"),
        facts = List("pin_relation_witness", "attacker:a1", "pinned:a2", "behind:a3"),
        lineMoves = List("h1h4", "a7a6"),
        targetSquare = Some("a2"),
        details =
          MoveReviewExchangeAnalyzer.RelationDetails.Skewer(
            attackerSquare = "h1",
            frontSquare = "h4",
            backSquare = "h8",
            targetSquare = "h4",
            attackerRole = "rook",
            frontRole = "queen",
            backRole = "rook"
          )
      )

    assertEquals(MoveReviewExchangeAnalyzer.typedDetailsFromWitness(witness), None)
    assertEquals(MoveReviewExchangeAnalyzer.relationDetailsValidForKind(witness), false)
    assertEquals(MoveReviewExchangeAnalyzer.relationProjectionFromWitness(witness), None)
    assertEquals(MoveReviewExchangeAnalyzer.relationFocusSquaresFromWitness(witness), Nil)
    assertEquals(MoveReviewExchangeAnalyzer.relationTargetSquareFromWitness(witness), None)
    assertEquals(MoveReviewExchangeAnalyzer.relationFactTermsFromWitness(witness), Nil)
    assertEquals(
      MoveReviewExchangeAnalyzer.ownerSeedTermsFromWitness(
        witness = witness,
        planKindId = "pin",
        aliases = List("pin")
      ),
      Nil
    )
    assertEquals(
      MoveReviewExchangeAnalyzer.transitionTermsFromWitness(
        witness = witness,
        extras = List("unsafe_extra")
      ),
      Nil
    )
  }

  test("implemented relation kinds require typed relation details before projection") {
    MoveReviewExchangeAnalyzer.RelationKind.Implemented.foreach { kind =>
      val rawOnly =
        MoveReviewExchangeAnalyzer.RelationWitness(
          kind = kind,
          focusSquares = List("a1", "a2"),
          facts = List(s"${kind}_relation_witness"),
          lineMoves = List("a1a2"),
          targetSquare = Some("a2")
        )

      assertEquals(MoveReviewExchangeAnalyzer.relationDetailsValidForKind(rawOnly), false, clues(kind))
      assertEquals(MoveReviewExchangeAnalyzer.relationProjectionFromWitness(rawOnly), None, clues(kind))
      assertEquals(MoveReviewExchangeAnalyzer.relationFocusSquaresFromWitness(rawOnly), Nil, clues(kind))
      assertEquals(MoveReviewExchangeAnalyzer.relationTargetSquareFromWitness(rawOnly), None, clues(kind))
      assertEquals(MoveReviewExchangeAnalyzer.relationFactTermsFromWitness(rawOnly), Nil, clues(kind))
    }
  }

  test("branch keys are emitted only after legal replay accepts the prefix") {
    val fen = "3qk3/8/8/8/8/3Q4/8/6K1 w - - 0 1"
    val replay =
      MoveReviewExchangeAnalyzer
        .boundedTopReplay(fen, List(VariationLine(moves = List("d3d8", "e8d8"), scoreCp = 20)), maxPlies = 2)
        .getOrElse(fail("legal branch should replay"))

    assertEquals(MoveReviewExchangeAnalyzer.branchKey(replay), Some("d3d8|e8d8"))

    val illegal =
      MoveReviewExchangeAnalyzer.boundedTopReplay(
        fen,
        List(VariationLine(moves = List("d3d8", "a8d8"), scoreCp = 20)),
        maxPlies = 2
      )

    assertEquals(illegal.flatMap(MoveReviewExchangeAnalyzer.branchKey(_)), None)
  }

  test("branch key and branch fact formatting is centralized in the analyzer") {
    val moves = List("D3D8", " e8d8 ", "h2h4")

    assertEquals(MoveReviewExchangeAnalyzer.branchKeyFromMoves(moves), Some("d3d8|e8d8"))
    assertEquals(MoveReviewExchangeAnalyzer.branchKeyFromMoves(moves.take(1)), None)
    assertEquals(MoveReviewExchangeAnalyzer.linePrefixKeyFromMoves(moves.take(1)), Some("d3d8"))
    assertEquals(MoveReviewExchangeAnalyzer.branchFactFromMoves(moves, maxPlies = 3), List("branch:d3d8|e8d8|h2h4"))

    val root = java.nio.file.Paths.get("").toAbsolutePath
    val analyzerPath =
      root.resolve("modules/commentaryCore/src/main/scala/lila/commentary/analysis/MoveReviewExchangeAnalyzer.scala")
    val policyPath =
      root.resolve("modules/commentaryCore/src/main/scala/lila/commentary/analysis/PlayerFacingTruthModePolicy.scala")
    val analyzerText = java.nio.file.Files.readString(analyzerPath)
    val policyText = java.nio.file.Files.readString(policyPath)

    assert(analyzerText.contains("def branchFactFromMoves"), clues(analyzerPath))
    assert(policyText.contains("MoveReviewExchangeAnalyzer.branchKeyFromMoves"), clues(policyPath))
    assert(policyText.contains("MoveReviewExchangeAnalyzer.linePrefixKeyFromMoves"), clues(policyPath))
    assert(!policyText.contains("private def branchKeyFromMoves"), clues(policyPath))
    assert(!policyText.contains("mkString(\"|\")"), clues(policyPath))
  }

  test("probe branch key helpers centralize proof consumer identity formats") {
    val stableByHash =
      ProbeResult(
        id = "stable-hash",
        evalCp = 0,
        bestReplyPv = Nil,
        deltaVsBaseline = 0,
        keyMotifs = Nil,
        variationHash = Some(" Branch-A ")
      )
    val stableByPv =
      ProbeResult(
        id = "stable-pv",
        evalCp = 0,
        bestReplyPv = List("D3D8", " e8d8 ", "not-a-move"),
        deltaVsBaseline = 0,
        keyMotifs = Nil
      )
    val fullLine =
      ProbeResult(
        id = "full-line",
        evalCp = 0,
        bestReplyPv = Nil,
        replyPvs = Some(List(List("Qg4", " h4 "))),
        deltaVsBaseline = 0,
        keyMotifs = Nil
      )
    val firstReply =
      ProbeResult(
        id = "first-reply",
        evalCp = 0,
        bestReplyPv = Nil,
        replyPvs = Some(List(List("Nc6", "d2d4"))),
        probedMove = Some("e2e4"),
        deltaVsBaseline = 0,
        keyMotifs = Nil
      )

    assertEquals(MoveReviewExchangeAnalyzer.probeStableBranchKey(stableByHash), Some("branch-a"))
    assertEquals(MoveReviewExchangeAnalyzer.probeStableBranchKey(stableByPv), Some("d3d8 e8d8"))
    assertEquals(MoveReviewExchangeAnalyzer.probeReplyPrefixKeyFromMoves(List("D3D8"), plies = 2), None)
    assertEquals(MoveReviewExchangeAnalyzer.probeFullReplyLineKey(fullLine), Some("Qg4 h4"))
    assert(MoveReviewExchangeAnalyzer.probeFullReplyLineMatches(fullLine, "Qg4 h4"))
    assertEquals(MoveReviewExchangeAnalyzer.probeFirstReplyOrMoveKey(firstReply), Some("Nc6"))
  }

  test("owner seed terms consume typed relation focus before raw witness focus") {
    val witness =
      MoveReviewExchangeAnalyzer.RelationWitness(
        kind = MoveReviewExchangeAnalyzer.RelationKind.DefenderTrade,
        focusSquares = List("a1"),
        facts = List("defender_trade_branch", "defender:a1", "defended_target:a1"),
        lineMoves = List("c1a3", "f8a3", "b3a3"),
        targetSquare = Some("a1"),
        details =
          MoveReviewExchangeAnalyzer.RelationDetails.DefenderTrade(
            defenderSquare = "f8",
            exchangeSquare = "a3",
            targetSquare = "g7"
          )
      )
    val terms =
      MoveReviewExchangeAnalyzer.ownerSeedTermsFromWitness(
        witness = witness,
        planKindId = "trade_key_defender",
        aliases = List("defender_trade")
      )
    val projection =
      MoveReviewExchangeAnalyzer
        .relationProjectionFromWitness(witness)
        .getOrElse(fail("typed defender-trade witness should project through the analyzer"))

    assertEquals(projection.kind, MoveReviewExchangeAnalyzer.RelationKind.DefenderTrade)
    assertEquals(projection.focusSquares, List("g7", "a3"))
    assertEquals(projection.targetSquare, Some("g7"))
    assert(projection.factTerms.contains("defender:f8"), clues(projection))
    assert(projection.factTerms.contains("defended_target:g7"), clues(projection))
    assert(!projection.factTerms.contains("defender:a1"), clues(projection))
    assert(terms.contains("g7"), clues(terms))
    assert(terms.contains("a3"), clues(terms))
    assert(!terms.contains("a1"), clues(terms))
    assert(terms.contains("defender:f8"), clues(terms))
    assert(!terms.contains("defender:a1"), clues(terms))
    assert(terms.contains("defended_target:g7"), clues(terms))
    assert(!terms.contains("defended_target:a1"), clues(terms))
    assert(terms.contains("defender_trade"), clues(terms))
    assert(terms.contains("trade_key_defender"), clues(terms))
  }

  test("relation witnesses expose overload from replayed attack-defense duties") {
    val fen = "k7/7p/5n2/3p4/8/8/8/3Q2K1 w - - 0 1"
    val replay =
      MoveReviewExchangeAnalyzer
        .boundedTopReplay(fen, List(VariationLine(moves = List("d1d3"), scoreCp = 26)), maxPlies = 1)
        .getOrElse(fail("queen lift should replay legally"))

    val witness =
      MoveReviewExchangeAnalyzer
        .overloadWitness(replay, "d1d3", explicitTargets = List("d5", "h7"))
        .getOrElse(fail("knight f6 should be overloaded across d5 and h7"))

    assertEquals(witness.kind, MoveReviewExchangeAnalyzer.RelationKind.Overload)
    assert(witness.focusSquares.contains("f6"), clues(witness))
    assert(witness.facts.exists(_.contains("d5")) && witness.facts.exists(_.contains("h7")), clues(witness))
    val details =
      MoveReviewExchangeAnalyzer
        .overloadDetailsFromWitness(witness)
        .getOrElse(fail("overload witness should carry typed relation details"))
    assertEquals(details.defenderSquare, "f6")
    assertEquals(details.targetSquares, List("d5", "h7"))
    assertEquals(details.attackerSquare, "d3")
    assertEquals(MoveReviewExchangeAnalyzer.overloadWitness(replay, "d1d3", explicitTargets = List("d5", "bad")), None)
  }

  test("relation witnesses expose deflection only when the attacked defender leaves a defended target") {
    val fen = "3k1b1r/p2b1ppp/1n3n2/4p3/8/1R4P1/P1QPqPBP/2B2RK1 w - - 0 17"
    val replay =
      MoveReviewExchangeAnalyzer
        .boundedTopReplay(fen, List(VariationLine(moves = List("c1a3", "f8a3"), scoreCp = 32)), maxPlies = 2)
        .getOrElse(fail("deflection branch should replay legally"))

    val witness =
      MoveReviewExchangeAnalyzer
        .deflectionWitness(replay, "c1a3", explicitTargets = List("g7"))
        .getOrElse(fail("bishop f8 should be deflected away from g7"))

    assertEquals(witness.kind, MoveReviewExchangeAnalyzer.RelationKind.Deflection)
    assertEquals(witness.focusSquares.take(2), List("g7", "f8"))
    assert(witness.lineMoves == List("c1a3", "f8a3"), clues(witness))
    val details =
      MoveReviewExchangeAnalyzer
        .deflectionDetailsFromWitness(witness)
        .getOrElse(fail("deflection witness should carry typed relation details"))
    assertEquals(details.defenderSquare, "f8")
    assertEquals(details.targetSquare, "g7")
    assertEquals(details.attackerSquare, "a3")

    assertEquals(MoveReviewExchangeAnalyzer.deflectionWitness(replay, "c1a3", explicitTargets = List("c5")), None)
  }

  test("relation witnesses expose discovered attack from a cleared legal ray") {
    val fen = "k7/7q/8/8/8/3N4/8/1B4K1 w - - 0 1"
    val replay =
      MoveReviewExchangeAnalyzer
        .boundedTopReplay(fen, List(VariationLine(moves = List("d3f4"), scoreCp = 80)), maxPlies = 1)
        .getOrElse(fail("knight move should replay legally"))

    val witness =
      MoveReviewExchangeAnalyzer
        .discoveredAttackWitness(replay, "d3f4")
        .getOrElse(fail("bishop b1 should discover an attack on h7"))

    assertEquals(witness.kind, MoveReviewExchangeAnalyzer.RelationKind.DiscoveredAttack)
    assertEquals(witness.focusSquares, List("b1", "d3", "h7"))
    val details =
      MoveReviewExchangeAnalyzer
        .discoveredAttackDetailsFromWitness(witness)
        .getOrElse(fail("discovered attack witness should carry typed relation details"))
    assertEquals(details.attackerSquare, "b1")
    assertEquals(details.clearedSquare, "d3")
    assertEquals(details.targetSquare, "h7")
    assertEquals(details.attackerRole, "bishop")
    assert(MoveReviewExchangeAnalyzer.discoveredAttackWitness(replay, "d3f4", explicitTargets = List("h7")).nonEmpty)
    assertEquals(MoveReviewExchangeAnalyzer.discoveredAttackWitness(replay, "d3f4", explicitTargets = List("a8")), None)
    assertEquals(MoveReviewExchangeAnalyzer.discoveredAttackWitness(replay, "d3e5"), None)
  }

  test("relation witnesses expose double check only from replayed multiple-checker geometry without target fallback") {
    val fen = "4k3/8/8/8/4N3/8/8/4R1K1 w - - 0 1"
    val replay =
      MoveReviewExchangeAnalyzer
        .boundedTopReplay(fen, List(VariationLine(moves = List("e4f6"), scoreCp = 130)), maxPlies = 1)
        .getOrElse(fail("knight double-check move should replay legally"))

    val witness =
      MoveReviewExchangeAnalyzer
        .doubleCheckWitness(replay, "e4f6")
        .getOrElse(fail("knight f6 should create double check on e8"))

    assertEquals(witness.kind, MoveReviewExchangeAnalyzer.RelationKind.DoubleCheck)
    assertEquals(witness.targetSquare, Some("e8"))
    assert(witness.focusSquares.contains("f6"), clues(witness))
    assert(witness.facts.exists(_.startsWith("checkers:")), clues(witness))
    val details =
      MoveReviewExchangeAnalyzer
        .doubleCheckDetailsFromWitness(witness)
        .getOrElse(fail("double-check witness should carry typed king-attack details"))
    assertEquals(details.kingSquare, "e8")
    assertEquals(details.checkerSquares, List("e1", "f6"))
    assertEquals(details.moverSquare, "f6")
    assertEquals(details.moverRole, "knight")
    val staleRaw =
      witness.copy(
        focusSquares = List("a1"),
        facts = List("double_check_relation_witness", "king:a1", "checkers:a1"),
        targetSquare = Some("a1")
      )
    assertEquals(MoveReviewExchangeAnalyzer.relationFocusSquaresFromWitness(staleRaw), List("e8", "e1", "f6"))
    assertEquals(MoveReviewExchangeAnalyzer.relationTargetSquareFromWitness(staleRaw), Some("e8"))
    assert(MoveReviewExchangeAnalyzer.relationFactTermsFromWitness(staleRaw).contains("mover:f6"), clues(staleRaw))
    assert(!MoveReviewExchangeAnalyzer.relationFactTermsFromWitness(staleRaw).contains("king:a1"), clues(staleRaw))
    assertEquals(MoveReviewExchangeAnalyzer.doubleCheckWitness(replay, "e4f6", explicitTargets = List("e8")), None)
    assertEquals(MoveReviewExchangeAnalyzer.doubleCheckWitness(replay, "e4g5"), None)
  }

  test("relation witnesses expose back-rank mate only through replayed mate geometry") {
    val fen = "6k1/5ppp/8/8/8/8/8/4R1K1 w - - 0 1"
    val replay =
      MoveReviewExchangeAnalyzer
        .boundedTopReplay(fen, List(VariationLine(moves = List("e1e8"), scoreCp = 9999)), maxPlies = 1)
        .getOrElse(fail("rook back-rank mate should replay legally"))

    val witness =
      MoveReviewExchangeAnalyzer
        .backRankMateWitness(replay, "e1e8")
        .getOrElse(fail("rook e8 should create back-rank mate on g8"))

    assertEquals(witness.kind, MoveReviewExchangeAnalyzer.RelationKind.BackRankMate)
    assertEquals(witness.targetSquare, Some("g8"))
    assertEquals(witness.focusSquares, List("g8", "e8"))
    assert(witness.facts.contains("mate"), clues(witness))
    assert(witness.facts.contains("mating_move:e1e8"), clues(witness))
    val details =
      MoveReviewExchangeAnalyzer
        .matePatternDetailsFromWitness(witness)
        .getOrElse(fail("back-rank mate witness should carry typed mate details"))
    assertEquals(details.relationKind, MoveReviewExchangeAnalyzer.RelationKind.BackRankMate)
    assertEquals(details.kingSquare, "g8")
    assertEquals(details.checkerSquares, List("e8"))
    assertEquals(details.matingMove, "e1e8")
    val staleRaw =
      witness.copy(
        focusSquares = List("a1"),
        facts = List("back_rank_mate_relation_witness", "king:a1", "mating_move:a1a2"),
        targetSquare = Some("a1")
      )
    assertEquals(MoveReviewExchangeAnalyzer.relationFocusSquaresFromWitness(staleRaw), List("g8", "e8"))
    assertEquals(MoveReviewExchangeAnalyzer.relationTargetSquareFromWitness(staleRaw), Some("g8"))
    assert(MoveReviewExchangeAnalyzer.relationFactTermsFromWitness(staleRaw).contains("mating_move:e1e8"), clues(staleRaw))
    assert(!MoveReviewExchangeAnalyzer.relationFactTermsFromWitness(staleRaw).contains("king:a1"), clues(staleRaw))
    assertEquals(MoveReviewExchangeAnalyzer.backRankMateWitness(replay, "e1e8", explicitTargets = List("g8")), None)
    assertEquals(MoveReviewExchangeAnalyzer.backRankMateWitness(replay, "e1e7"), None)
    assertEquals(MoveReviewExchangeAnalyzer.mateNetWitness(replay, "e1e8"), None)
  }

  test("relation witnesses expose mate net through existing mate-pattern detectors") {
    val fen = "6rk/6pp/7N/8/8/8/8/6K1 w - - 0 1"
    val replay =
      MoveReviewExchangeAnalyzer
        .boundedTopReplay(fen, List(VariationLine(moves = List("h6f7"), scoreCp = 9999)), maxPlies = 1)
        .getOrElse(fail("smothered mate should replay legally"))

    val witness =
      MoveReviewExchangeAnalyzer
        .mateNetWitness(replay, "h6f7")
        .getOrElse(fail("knight f7 should create a smothered mate net on h8"))

    assertEquals(witness.kind, MoveReviewExchangeAnalyzer.RelationKind.MateNet)
    assertEquals(witness.targetSquare, Some("h8"))
    assertEquals(witness.focusSquares, List("h8", "f7"))
    assert(witness.facts.contains("pattern:smothered_mate"), clues(witness))
    assert(witness.facts.contains("mating_move:h6f7"), clues(witness))
    val details =
      MoveReviewExchangeAnalyzer
        .matePatternDetailsFromWitness(witness)
        .getOrElse(fail("mate-net witness should carry typed mate details"))
    assertEquals(details.relationKind, MoveReviewExchangeAnalyzer.RelationKind.MateNet)
    assertEquals(details.kingSquare, "h8")
    assertEquals(details.checkerSquares, List("f7"))
    assertEquals(details.patternId, Some("smothered_mate"))
    val staleRaw =
      witness.copy(
        focusSquares = List("a1"),
        facts = List("mate_net_relation_witness", "pattern:wrong", "king:a1"),
        targetSquare = Some("a1")
      )
    assertEquals(MoveReviewExchangeAnalyzer.relationFocusSquaresFromWitness(staleRaw), List("h8", "f7"))
    assertEquals(MoveReviewExchangeAnalyzer.relationTargetSquareFromWitness(staleRaw), Some("h8"))
    assert(MoveReviewExchangeAnalyzer.relationFactTermsFromWitness(staleRaw).contains("pattern:smothered_mate"), clues(staleRaw))
    assert(!MoveReviewExchangeAnalyzer.relationFactTermsFromWitness(staleRaw).contains("pattern:wrong"), clues(staleRaw))
    assertEquals(MoveReviewExchangeAnalyzer.mateNetWitness(replay, "h6f7", explicitTargets = List("h8")), None)
    assertEquals(MoveReviewExchangeAnalyzer.mateNetWitness(replay, "h6g8"), None)
  }

  test("relation witnesses expose Greek gift through existing detector support") {
    val fen = "6k1/6pp/8/6NQ/8/3B4/8/4K3 w - - 0 1"
    val replay =
      MoveReviewExchangeAnalyzer
        .boundedTopReplay(fen, List(VariationLine(moves = List("d3h7"), scoreCp = 160)), maxPlies = 1)
        .getOrElse(fail("Greek gift entry should replay legally"))

    val witness =
      MoveReviewExchangeAnalyzer
        .greekGiftWitness(replay, "d3h7")
        .getOrElse(fail("Bxh7+ should create Greek gift relation with immediate support"))

    assertEquals(witness.kind, MoveReviewExchangeAnalyzer.RelationKind.GreekGift)
    assertEquals(witness.targetSquare, Some("h7"))
    assertEquals(witness.focusSquares, List("h7"))
    assert(witness.facts.contains("pattern:greek_gift"), clues(witness))
    assert(witness.facts.contains("entry_move:d3h7"), clues(witness))
    val details =
      MoveReviewExchangeAnalyzer
        .greekGiftDetailsFromWitness(witness)
        .getOrElse(fail("Greek gift witness should carry typed sacrifice-entry details"))
    assertEquals(details.bishopSquare, "h7")
    assertEquals(details.targetSquare, "h7")
    assertEquals(details.entryMove, "d3h7")
    assertEquals(details.patternId, "greek_gift")
    val staleRaw =
      witness.copy(
        focusSquares = List("a1"),
        facts = List("greek_gift_relation_witness", "target:a1", "bishop:a1"),
        targetSquare = Some("a1")
      )
    assertEquals(MoveReviewExchangeAnalyzer.relationFocusSquaresFromWitness(staleRaw), List("h7"))
    assertEquals(MoveReviewExchangeAnalyzer.relationTargetSquareFromWitness(staleRaw), Some("h7"))
    assert(MoveReviewExchangeAnalyzer.relationFactTermsFromWitness(staleRaw).contains("entry_move:d3h7"), clues(staleRaw))
    assert(!MoveReviewExchangeAnalyzer.relationFactTermsFromWitness(staleRaw).contains("target:a1"), clues(staleRaw))
    assertEquals(MoveReviewExchangeAnalyzer.greekGiftWitness(replay, "d3h7", explicitTargets = List("h7")), None)
    assertEquals(MoveReviewExchangeAnalyzer.greekGiftWitness(replay, "d3h5"), None)
  }

  test("relation witnesses expose Greek gift when support appears through replayed continuation") {
    val fen = "6k1/7p/8/8/8/3B1N2/8/3QK3 w - - 0 1"
    val line = List("d3h7", "g8h7", "f3g5", "h7g8", "d1h5")
    val replay =
      MoveReviewExchangeAnalyzer
        .boundedTopReplay(fen, List(VariationLine(moves = line, scoreCp = 120)), maxPlies = 1)
        .getOrElse(fail("Greek gift entry should replay legally"))

    val witness =
      MoveReviewExchangeAnalyzer
        .greekGiftWitness(replay, "d3h7", continuationLines = List(line))
        .getOrElse(fail("Bxh7+ should create Greek gift relation from continuation support"))

    assertEquals(witness.kind, MoveReviewExchangeAnalyzer.RelationKind.GreekGift)
    assertEquals(witness.targetSquare, Some("h7"))
    assert(witness.facts.contains("sacrifice_entry"), clues(witness))
    assertEquals(MoveReviewExchangeAnalyzer.greekGiftWitness(replay, "d3h7"), None)
  }

  test("relation witnesses expose zwischenzug only from a replayed check before a bound recapture") {
    val fen = "4k3/8/8/8/3b4/5N2/8/3QK3 w - - 0 1"
    val replay =
      MoveReviewExchangeAnalyzer
        .boundedTopReplay(fen, List(VariationLine(moves = List("d1a4"), scoreCp = 42)), maxPlies = 1)
        .getOrElse(fail("queen in-between check should replay legally"))

    val witness =
      MoveReviewExchangeAnalyzer
        .zwischenzugWitness(replay, "d1a4", explicitTargets = List("d4"))
        .getOrElse(fail("Qa4+ should prove an in-between check before Nxd4"))

    assertEquals(witness.kind, MoveReviewExchangeAnalyzer.RelationKind.Zwischenzug)
    assertEquals(witness.targetSquare, Some("d4"))
    assert(witness.facts.contains("zwischenzug_relation_witness"), clues(witness))
    assert(witness.facts.contains("intermediate_move:d1a4"), clues(witness))
    assert(witness.facts.contains("expected_recapture:d4"), clues(witness))
    assert(witness.facts.contains("threat:check"), clues(witness))
    assert(witness.facts.contains("branch:d1a4"), clues(witness))
    val details =
      MoveReviewExchangeAnalyzer
        .zwischenzugDetailsFromWitness(witness)
        .getOrElse(fail("zwischenzug witness should carry typed move-order details"))
    assertEquals(details.intermediateMove, "d1a4")
    assertEquals(details.expectedRecaptureSquare, "d4")
    assertEquals(details.checkingPieceSquare, "a4")
    assertEquals(details.checkedKingSquare, "e8")

    val staleRaw =
      witness.copy(
        focusSquares = List("a1"),
        facts = List("zwischenzug_relation_witness", "expected_recapture:a1"),
        targetSquare = Some("a1")
      )
    assertEquals(MoveReviewExchangeAnalyzer.relationFocusSquaresFromWitness(staleRaw), List("a4", "d4", "e8"))
    assertEquals(MoveReviewExchangeAnalyzer.relationTargetSquareFromWitness(staleRaw), Some("d4"))

    val relationKinds =
      MoveReviewExchangeAnalyzer
        .relationWitnesses(replay, "d1a4", explicitTargets = List("d4"))
        .map(_.kind)
    assert(relationKinds.contains(MoveReviewExchangeAnalyzer.RelationKind.Zwischenzug), clues(relationKinds))
    assert(!relationKinds.contains(MoveReviewExchangeAnalyzer.RelationKind.Fork), clues(relationKinds))

    assertEquals(MoveReviewExchangeAnalyzer.zwischenzugWitness(replay, "d1a4"), None)
    assertEquals(MoveReviewExchangeAnalyzer.zwischenzugWitness(replay, "d1a4", explicitTargets = List("d4", "bad")), None)

    val directRecapture =
      MoveReviewExchangeAnalyzer
        .boundedTopReplay(fen, List(VariationLine(moves = List("f3d4"), scoreCp = 20)), maxPlies = 1)
        .getOrElse(fail("direct recapture should replay legally"))
    assertEquals(MoveReviewExchangeAnalyzer.zwischenzugWitness(directRecapture, "f3d4", explicitTargets = List("d4")), None)

    val nonCheckingMove =
      MoveReviewExchangeAnalyzer
        .boundedTopReplay(fen, List(VariationLine(moves = List("d1d2"), scoreCp = 12)), maxPlies = 1)
        .getOrElse(fail("quiet queen move should replay legally"))
    assertEquals(MoveReviewExchangeAnalyzer.zwischenzugWitness(nonCheckingMove, "d1d2", explicitTargets = List("d4")), None)
  }

  test("relation witnesses expose stalemate trap only from PV-backed terminal stalemate") {
    val fen = "7k/5K2/8/6Q1/8/8/8/8 w - - 0 1"
    val replay =
      MoveReviewExchangeAnalyzer
        .boundedTopReplay(fen, List(VariationLine(moves = List("g5g6"), scoreCp = 0)), maxPlies = 1)
        .getOrElse(fail("stalemate trap line should replay legally"))

    val witness =
      MoveReviewExchangeAnalyzer
        .stalemateTrapWitness(replay, "g5g6", engineScoreCp = Some(0), engineMate = None)
        .getOrElse(fail("Qg6 should create a PV-backed stalemate resource"))

    assertEquals(witness.kind, MoveReviewExchangeAnalyzer.RelationKind.StalemateTrap)
    assertEquals(witness.targetSquare, Some("h8"))
    assertEquals(witness.focusSquares, List("h8", "g6"))
    assert(witness.facts.contains("terminal:stalemate"), clues(witness))
    assert(witness.facts.contains("draw_resource"), clues(witness))
    assert(witness.facts.contains("draw_score_cp:0"), clues(witness))
    val details =
      MoveReviewExchangeAnalyzer
        .typedDetailsFromWitness(witness)
        .collect { case details: MoveReviewExchangeAnalyzer.RelationDetails.StalemateTrap => details }
        .getOrElse(fail("stalemate trap witness should carry typed draw-resource details"))
    assertEquals(details.stalematedKingSquare, "h8")
    assertEquals(details.resourceSquare, "g6")
    assertEquals(details.entryMove, "g5g6")
    assertEquals(details.terminalMove, "g5g6")

    val projection =
      MoveReviewExchangeAnalyzer
        .relationProjectionFromWitness(witness)
        .getOrElse(fail("PV-backed stalemate trap should project through the relation boundary"))
    assertEquals(projection.kind, MoveReviewExchangeAnalyzer.RelationKind.StalemateTrap)
    assertEquals(projection.focusSquares, List("h8", "g6"))
    assertEquals(projection.targetSquare, Some("h8"))
    assert(projection.factTerms.contains("stalemate_trap_relation_witness"), clues(projection))
    assert(projection.factTerms.contains("terminal:stalemate"), clues(projection))

    assertEquals(MoveReviewExchangeAnalyzer.stalemateTrapWitness(replay, "g5g6", engineScoreCp = None, engineMate = None), None)
    assertEquals(MoveReviewExchangeAnalyzer.stalemateTrapWitness(replay, "g5g6", engineScoreCp = Some(180), engineMate = None), None)
    assertEquals(MoveReviewExchangeAnalyzer.stalemateTrapWitness(replay, "g5g6", engineScoreCp = Some(0), engineMate = Some(1)), None)

    val nonTerminalReplay =
      MoveReviewExchangeAnalyzer
        .boundedTopReplay("7k/5K2/8/8/6Q1/8/8/8 w - - 0 1", List(VariationLine(moves = List("g4g5"), scoreCp = 0)), maxPlies = 1)
        .getOrElse(fail("non-stalemate line should replay legally"))
    assertEquals(
      MoveReviewExchangeAnalyzer.stalemateTrapWitness(nonTerminalReplay, "g4g5", engineScoreCp = Some(0), engineMate = None),
      None
    )
  }

  test("relation witnesses expose perpetual check only from PV-backed repeated checking cycle") {
    val fen = "r4rk1/5ppp/8/8/7q/8/2Q3P1/R5K1 b - - 0 1"
    val line = List("h4e1", "g1h2", "e1h4", "h2g1", "h4e1")
    val replay =
      MoveReviewExchangeAnalyzer
        .boundedTopReplay(fen, List(VariationLine(moves = line, scoreCp = 0)), maxPlies = 5)
        .getOrElse(fail("perpetual-check line should replay legally"))

    val witness =
      MoveReviewExchangeAnalyzer
        .perpetualCheckWitness(replay, "h4e1", engineScoreCp = Some(0), engineMate = None)
        .getOrElse(fail("Qe1+ cycle should create a PV-backed perpetual-check resource"))

    assertEquals(witness.kind, MoveReviewExchangeAnalyzer.RelationKind.PerpetualCheck)
    assertEquals(witness.targetSquare, Some("g1"))
    assertEquals(witness.focusSquares, List("g1", "e1"))
    assert(witness.facts.contains("checking_cycle"), clues(witness))
    assert(witness.facts.contains("draw_resource"), clues(witness))
    assert(witness.facts.exists(_.startsWith("repetition_key:")), clues(witness))
    assert(witness.facts.contains("draw_score_cp:0"), clues(witness))
    val details =
      MoveReviewExchangeAnalyzer
        .typedDetailsFromWitness(witness)
        .collect { case details: MoveReviewExchangeAnalyzer.RelationDetails.PerpetualCheck => details }
        .getOrElse(fail("perpetual-check witness should carry typed cycle details"))
    assertEquals(details.checkedKingSquare, "g1")
    assertEquals(details.checkerSquares, List("e1"))
    assertEquals(details.checkingSide, "black")
    assertEquals(details.entryMove, "h4e1")
    assertEquals(details.cycleStartMove, "h4e1")
    assertEquals(details.cycleReturnMove, "h4e1")
    assert(details.repeatedPositionKey.contains(" w "), clues(details))

    val projection =
      MoveReviewExchangeAnalyzer
        .relationProjectionFromWitness(witness)
        .getOrElse(fail("PV-backed perpetual check should project through the relation boundary"))
    assertEquals(projection.kind, MoveReviewExchangeAnalyzer.RelationKind.PerpetualCheck)
    assertEquals(projection.focusSquares, List("g1", "e1"))
    assertEquals(projection.targetSquare, Some("g1"))
    assert(projection.factTerms.contains("perpetual_check_relation_witness"), clues(projection))
    assert(projection.factTerms.contains("checking_cycle"), clues(projection))

    assertEquals(MoveReviewExchangeAnalyzer.perpetualCheckWitness(replay, "h4e1", engineScoreCp = None, engineMate = None), None)
    assertEquals(MoveReviewExchangeAnalyzer.perpetualCheckWitness(replay, "h4e1", engineScoreCp = Some(160), engineMate = None), None)
    assertEquals(MoveReviewExchangeAnalyzer.perpetualCheckWitness(replay, "h4e1", engineScoreCp = Some(0), engineMate = Some(1)), None)

    val noCycleReplay =
      MoveReviewExchangeAnalyzer
        .boundedTopReplay(fen, List(VariationLine(moves = line.take(4), scoreCp = 0)), maxPlies = 4)
        .getOrElse(fail("short checking line should replay legally"))
    assertEquals(
      MoveReviewExchangeAnalyzer.perpetualCheckWitness(noCycleReplay, "h4e1", engineScoreCp = Some(0), engineMate = None),
      None
    )
  }

  test("relation witnesses expose fork only after a replayed move attacks multiple bound targets") {
    val fen = "k7/4r3/8/8/3N3q/8/8/6K1 w - - 0 1"
    val replay =
      MoveReviewExchangeAnalyzer
        .boundedTopReplay(fen, List(VariationLine(moves = List("d4f5"), scoreCp = 90)), maxPlies = 1)
        .getOrElse(fail("knight fork move should replay legally"))

    val witness =
      MoveReviewExchangeAnalyzer
        .forkWitness(replay, "d4f5", explicitTargets = List("e7", "h4"))
        .getOrElse(fail("knight f5 should fork rook e7 and queen h4"))

    assertEquals(witness.kind, MoveReviewExchangeAnalyzer.RelationKind.Fork)
    assertEquals(witness.focusSquares, List("f5", "h4", "e7"))
    assertEquals(witness.targetSquare, Some("h4"))
    assert(witness.facts.contains("target:h4:queen"), clues(witness))
    val details =
      MoveReviewExchangeAnalyzer
        .forkDetailsFromWitness(witness)
        .getOrElse(fail("fork witness should carry typed material-target details"))
    assertEquals(details.attackerSquare, "f5")
    assertEquals(details.attackerRole, "knight")
    assertEquals(details.targets.map(target => target.square -> target.role), List("h4" -> "queen", "e7" -> "rook"))
    val staleRaw =
      witness.copy(
        focusSquares = List("a1"),
        facts = List("fork_relation_witness", "attacker:a1", "target:a1:queen"),
        targetSquare = Some("a1")
      )
    assertEquals(MoveReviewExchangeAnalyzer.relationFocusSquaresFromWitness(staleRaw), List("f5", "h4", "e7"))
    assertEquals(MoveReviewExchangeAnalyzer.relationTargetSquareFromWitness(staleRaw), Some("h4"))
    assert(MoveReviewExchangeAnalyzer.relationFactTermsFromWitness(staleRaw).contains("target:h4:queen"), clues(staleRaw))
    assert(!MoveReviewExchangeAnalyzer.relationFactTermsFromWitness(staleRaw).contains("attacker:a1"), clues(staleRaw))
    assertEquals(MoveReviewExchangeAnalyzer.forkWitness(replay, "d4f5", explicitTargets = List("e7")), None)
    assertEquals(MoveReviewExchangeAnalyzer.forkWitness(replay, "d4f5", explicitTargets = List("e7", "bad")), None)
    assertEquals(MoveReviewExchangeAnalyzer.forkWitness(replay, "d4e2"), None)
  }

  test("relation witnesses expose hanging piece only when the replayed mover attacks an undefended bound target") {
    val fen = "k7/8/8/5b2/2B5/8/8/6K1 w - - 0 1"
    val replay =
      MoveReviewExchangeAnalyzer
        .boundedTopReplay(fen, List(VariationLine(moves = List("c4d3"), scoreCp = 48)), maxPlies = 1)
        .getOrElse(fail("bishop attack move should replay legally"))

    val witness =
      MoveReviewExchangeAnalyzer
        .hangingPieceWitness(replay, "c4d3", explicitTargets = List("f5"))
        .getOrElse(fail("bishop d3 should attack undefended bishop f5"))

    assertEquals(witness.kind, MoveReviewExchangeAnalyzer.RelationKind.HangingPiece)
    assertEquals(witness.focusSquares, List("d3", "f5"))
    assertEquals(witness.targetSquare, Some("f5"))
    assert(witness.facts.contains("undefended_target"), clues(witness))
    val details =
      MoveReviewExchangeAnalyzer
        .hangingPieceDetailsFromWitness(witness)
        .getOrElse(fail("hanging-piece witness should carry typed material-target details"))
    assertEquals(details.attackerSquare, "d3")
    assertEquals(details.targetSquare, "f5")
    assertEquals(details.attackerRole, "bishop")
    assertEquals(details.targetRole, "bishop")
    val staleRaw =
      witness.copy(
        focusSquares = List("a1"),
        facts = List("hanging_piece_relation_witness", "attacker:a1", "target:a1"),
        targetSquare = Some("a1")
      )
    assertEquals(MoveReviewExchangeAnalyzer.relationFocusSquaresFromWitness(staleRaw), List("d3", "f5"))
    assertEquals(MoveReviewExchangeAnalyzer.relationTargetSquareFromWitness(staleRaw), Some("f5"))
    assert(MoveReviewExchangeAnalyzer.relationFactTermsFromWitness(staleRaw).contains("target_role:bishop"), clues(staleRaw))
    assert(!MoveReviewExchangeAnalyzer.relationFactTermsFromWitness(staleRaw).contains("target:a1"), clues(staleRaw))
    assertEquals(MoveReviewExchangeAnalyzer.hangingPieceWitness(replay, "c4d3", explicitTargets = List("a8")), None)
    assertEquals(MoveReviewExchangeAnalyzer.hangingPieceWitness(replay, "c4d3", explicitTargets = List("f5", "bad")), None)
    assertEquals(MoveReviewExchangeAnalyzer.hangingPieceWitness(replay, "c4e2"), None)
  }

  test("relation witnesses expose trapped piece only when a bound target has no safe legal escape") {
    val fen = "k5pr/7p/8/8/8/2B5/8/6K1 w - - 0 1"
    val replay =
      MoveReviewExchangeAnalyzer
        .boundedTopReplay(fen, List(VariationLine(moves = List("c3g7"), scoreCp = 64)), maxPlies = 1)
        .getOrElse(fail("bishop move should replay legally"))

    val witness =
      MoveReviewExchangeAnalyzer
        .trappedPieceWitness(replay, "c3g7", explicitTargets = List("h8"))
        .getOrElse(fail("bishop g7 should trap the rook on h8"))

    assertEquals(witness.kind, MoveReviewExchangeAnalyzer.RelationKind.TrappedPiece)
    assertEquals(witness.focusSquares, List("g7", "h8"))
    assertEquals(witness.targetSquare, Some("h8"))
    assert(witness.facts.contains("no_safe_escape"), clues(witness))
    val details =
      MoveReviewExchangeAnalyzer
        .trappedPieceDetailsFromWitness(witness)
        .getOrElse(fail("trapped-piece witness should carry typed target details"))
    assertEquals(details.attackerSquare, "g7")
    assertEquals(details.targetSquare, "h8")
    assertEquals(details.attackerRole, "bishop")
    assertEquals(details.targetRole, "rook")
    val staleRaw =
      witness.copy(
        focusSquares = List("a1"),
        facts = List("trapped_piece_relation_witness", "attacker:a1", "target:a1"),
        targetSquare = Some("a1")
      )
    assertEquals(MoveReviewExchangeAnalyzer.relationFocusSquaresFromWitness(staleRaw), List("g7", "h8"))
    assertEquals(MoveReviewExchangeAnalyzer.relationTargetSquareFromWitness(staleRaw), Some("h8"))
    assert(MoveReviewExchangeAnalyzer.relationFactTermsFromWitness(staleRaw).contains("target_role:rook"), clues(staleRaw))
    assert(!MoveReviewExchangeAnalyzer.relationFactTermsFromWitness(staleRaw).contains("target:a1"), clues(staleRaw))

    val relationKinds =
      MoveReviewExchangeAnalyzer
        .relationWitnesses(replay, "c3g7", explicitTargets = List("h8"))
        .map(_.kind)
    assert(relationKinds.contains(MoveReviewExchangeAnalyzer.RelationKind.TrappedPiece), clues(relationKinds))
    assert(!relationKinds.contains(MoveReviewExchangeAnalyzer.RelationKind.HangingPiece), clues(relationKinds))
    assertEquals(MoveReviewExchangeAnalyzer.trappedPieceWitness(replay, "c3g7"), None)
    assertEquals(MoveReviewExchangeAnalyzer.trappedPieceWitness(replay, "c3g7", explicitTargets = List("h8", "bad")), None)

    val escapeFen = "k6r/8/8/8/8/2B5/8/6K1 w - - 0 1"
    val escapeReplay =
      MoveReviewExchangeAnalyzer
        .boundedTopReplay(escapeFen, List(VariationLine(moves = List("c3g7"), scoreCp = 64)), maxPlies = 1)
        .getOrElse(fail("bishop move with rook escape should replay legally"))
    assertEquals(MoveReviewExchangeAnalyzer.trappedPieceWitness(escapeReplay, "c3g7", explicitTargets = List("h8")), None)
  }

  test("relation witnesses expose domination only from target-bound escape-square control") {
    val fen = "n6k/8/B7/1K6/8/8/8/2R5 w - - 0 1"
    val replay =
      MoveReviewExchangeAnalyzer
        .boundedTopReplay(fen, List(VariationLine(moves = List("a6b7"), scoreCp = 42)), maxPlies = 1)
        .getOrElse(fail("bishop domination move should replay legally"))

    val witness =
      MoveReviewExchangeAnalyzer
        .dominationWitness(replay, "a6b7", explicitTargets = List("a8"))
        .getOrElse(fail("bishop b7 should dominate the knight on a8"))

    assertEquals(witness.kind, MoveReviewExchangeAnalyzer.RelationKind.Domination)
    assertEquals(witness.focusSquares, List("b7", "a8", "b6", "c7"))
    assertEquals(witness.targetSquare, Some("a8"))
    assert(witness.facts.contains("escape_square_control"), clues(witness))
    assert(witness.facts.contains("controlled_escapes:b6|c7"), clues(witness))
    val details =
      MoveReviewExchangeAnalyzer
        .dominationDetailsFromWitness(witness)
        .getOrElse(fail("domination witness should carry typed restriction details"))
    assertEquals(details.attackerSquare, "b7")
    assertEquals(details.targetSquare, "a8")
    assertEquals(details.attackerRole, "bishop")
    assertEquals(details.targetRole, "knight")
    assertEquals(details.controlledEscapeSquares, List("b6", "c7"))
    val staleRaw =
      witness.copy(
        focusSquares = List("a1"),
        facts = List("domination_relation_witness", "attacker:a1", "target:a1"),
        targetSquare = Some("a1")
      )
    assertEquals(MoveReviewExchangeAnalyzer.relationFocusSquaresFromWitness(staleRaw), List("b7", "a8", "b6", "c7"))
    assertEquals(MoveReviewExchangeAnalyzer.relationTargetSquareFromWitness(staleRaw), Some("a8"))
    assert(MoveReviewExchangeAnalyzer.relationFactTermsFromWitness(staleRaw).contains("target_role:knight"), clues(staleRaw))
    assert(!MoveReviewExchangeAnalyzer.relationFactTermsFromWitness(staleRaw).contains("target:a1"), clues(staleRaw))

    val relationKinds =
      MoveReviewExchangeAnalyzer
        .relationWitnesses(replay, "a6b7", explicitTargets = List("a8"))
        .map(_.kind)
    assert(relationKinds.contains(MoveReviewExchangeAnalyzer.RelationKind.Domination), clues(relationKinds))
    assert(!relationKinds.contains(MoveReviewExchangeAnalyzer.RelationKind.HangingPiece), clues(relationKinds))
    assertEquals(MoveReviewExchangeAnalyzer.dominationWitness(replay, "a6b7"), None)
    assertEquals(MoveReviewExchangeAnalyzer.dominationWitness(replay, "a6b7", explicitTargets = List("a8", "bad")), None)

    val pinFen = "4kb2/8/8/8/8/2N5/8/4K3 b - - 0 1"
    val pinReplay =
      MoveReviewExchangeAnalyzer
        .boundedTopReplay(pinFen, List(VariationLine(moves = List("f8b4"), scoreCp = 52)), maxPlies = 1)
        .getOrElse(fail("pin move should replay legally"))
    assertEquals(MoveReviewExchangeAnalyzer.dominationWitness(pinReplay, "f8b4", explicitTargets = List("c3")), None)
  }

  test("relation witnesses expose x-ray pressure through an enemy blocker") {
    val fen = "k7/8/6q1/5n2/8/8/8/1B5K w - - 0 1"
    val replay =
      MoveReviewExchangeAnalyzer
        .boundedTopReplay(fen, List(VariationLine(moves = List("b1e4"), scoreCp = 42)), maxPlies = 1)
        .getOrElse(fail("bishop move should replay legally"))

    val witness =
      MoveReviewExchangeAnalyzer
        .xrayWitness(replay, "b1e4", explicitTargets = List("g6"))
        .getOrElse(fail("bishop e4 should x-ray queen g6 through knight f5"))

    assertEquals(witness.kind, MoveReviewExchangeAnalyzer.RelationKind.XRay)
    assertEquals(witness.focusSquares, List("e4", "f5", "g6"))
    assert(witness.facts.contains("blocker:f5"), clues(witness))
    val details =
      MoveReviewExchangeAnalyzer
        .xrayDetailsFromWitness(witness)
        .getOrElse(fail("x-ray witness should carry typed line-relation details"))
    assertEquals(details.attackerSquare, "e4")
    assertEquals(details.blockerSquare, "f5")
    assertEquals(details.targetSquare, "g6")
    assertEquals(details.attackerRole, "bishop")
    assertEquals(details.blockerRole, "knight")
    assertEquals(details.targetRole, "queen")
    val staleRaw =
      witness.copy(
        focusSquares = List("a1"),
        facts = List("xray_relation_witness", "attacker:a1", "blocker:a1", "target:a1"),
        targetSquare = Some("a1")
      )
    assertEquals(MoveReviewExchangeAnalyzer.relationFocusSquaresFromWitness(staleRaw), List("e4", "f5", "g6"))
    assertEquals(MoveReviewExchangeAnalyzer.relationTargetSquareFromWitness(staleRaw), Some("g6"))
    assert(MoveReviewExchangeAnalyzer.relationFactTermsFromWitness(staleRaw).contains("blocker_role:knight"), clues(staleRaw))
    assert(!MoveReviewExchangeAnalyzer.relationFactTermsFromWitness(staleRaw).contains("attacker:a1"), clues(staleRaw))
    assertEquals(MoveReviewExchangeAnalyzer.xrayWitness(replay, "b1e4", explicitTargets = List("h7")), None)
    assertEquals(MoveReviewExchangeAnalyzer.xrayWitness(replay, "b1e4", explicitTargets = List("bad")), None)
  }

  test("relation witnesses expose clearance when a legal move opens a friendly line") {
    val fen = "k7/7q/8/8/8/3N4/8/1B4K1 w - - 0 1"
    val replay =
      MoveReviewExchangeAnalyzer
        .boundedTopReplay(fen, List(VariationLine(moves = List("d3f4"), scoreCp = 80)), maxPlies = 1)
        .getOrElse(fail("knight move should replay legally"))

    val witness =
      MoveReviewExchangeAnalyzer
        .clearanceWitness(replay, "d3f4", explicitTargets = List("h7"))
        .getOrElse(fail("knight should clear bishop b1 against h7"))

    assertEquals(witness.kind, MoveReviewExchangeAnalyzer.RelationKind.Clearance)
    assertEquals(witness.focusSquares, List("b1", "d3", "h7"))
    assert(witness.facts.contains("beneficiary:b1"), clues(witness))
    val details =
      MoveReviewExchangeAnalyzer
        .clearanceDetailsFromWitness(witness)
        .getOrElse(fail("clearance witness should carry typed line-relation details"))
    assertEquals(details.beneficiarySquare, "b1")
    assertEquals(details.clearedSquare, "d3")
    assertEquals(details.targetSquare, "h7")
    assertEquals(details.beneficiaryRole, "bishop")
    assertEquals(details.clearingTo, "f4")
    val staleRaw =
      witness.copy(
        focusSquares = List("a1"),
        facts = List("clearance_relation_witness", "beneficiary:a1", "cleared_square:a1", "target:a1"),
        targetSquare = Some("a1")
      )
    assertEquals(MoveReviewExchangeAnalyzer.relationFocusSquaresFromWitness(staleRaw), List("b1", "d3", "h7"))
    assertEquals(MoveReviewExchangeAnalyzer.relationTargetSquareFromWitness(staleRaw), Some("h7"))
    assert(MoveReviewExchangeAnalyzer.relationFactTermsFromWitness(staleRaw).contains("clearing_to:f4"), clues(staleRaw))
    assert(!MoveReviewExchangeAnalyzer.relationFactTermsFromWitness(staleRaw).contains("beneficiary:a1"), clues(staleRaw))
    assertEquals(MoveReviewExchangeAnalyzer.clearanceWitness(replay, "d3f4", explicitTargets = List("a8")), None)
  }

  test("relation witnesses expose battery only when the aligned pieces have a clear target line") {
    val fen = "k7/7p/8/8/8/8/8/1B1Q2K1 w - - 0 1"
    val replay =
      MoveReviewExchangeAnalyzer
        .boundedTopReplay(fen, List(VariationLine(moves = List("d1d3"), scoreCp = 34)), maxPlies = 1)
        .getOrElse(fail("queen lift should replay legally"))

    val witness =
      MoveReviewExchangeAnalyzer
        .batteryWitness(replay, "d1d3", explicitTargets = List("h7"))
        .getOrElse(fail("queen d3 and bishop b1 should form a diagonal battery on h7"))

    assertEquals(witness.kind, MoveReviewExchangeAnalyzer.RelationKind.Battery)
    assertEquals(witness.focusSquares, List("d3", "b1", "h7"))
    assert(witness.facts.contains("axis:diagonal"), clues(witness))
    val details =
      MoveReviewExchangeAnalyzer
        .batteryDetailsFromWitness(witness)
        .getOrElse(fail("battery witness should carry typed line-relation details"))
    assertEquals(details.frontSquare, "d3")
    assertEquals(details.backSquare, "b1")
    assertEquals(details.targetSquare, "h7")
    assertEquals(details.frontRole, "queen")
    assertEquals(details.backRole, "bishop")
    assertEquals(details.axis, "diagonal")
    val staleRaw =
      witness.copy(
        focusSquares = List("a1"),
        facts = List("battery_relation_witness", "front:a1", "back:a1", "target:a1"),
        targetSquare = Some("a1")
      )
    assertEquals(MoveReviewExchangeAnalyzer.relationFocusSquaresFromWitness(staleRaw), List("d3", "b1", "h7"))
    assertEquals(MoveReviewExchangeAnalyzer.relationTargetSquareFromWitness(staleRaw), Some("h7"))
    assert(MoveReviewExchangeAnalyzer.relationFactTermsFromWitness(staleRaw).contains("axis:diagonal"), clues(staleRaw))
    assert(!MoveReviewExchangeAnalyzer.relationFactTermsFromWitness(staleRaw).contains("front:a1"), clues(staleRaw))
    assertEquals(MoveReviewExchangeAnalyzer.batteryWitness(replay, "d1d3", explicitTargets = List("a8")), None)
  }

  test("relation witnesses expose pin only after a replayed long-range move creates real ray geometry") {
    val fen = "4kb2/8/8/8/8/2N5/8/4K3 b - - 0 1"
    val replay =
      MoveReviewExchangeAnalyzer
        .boundedTopReplay(fen, List(VariationLine(moves = List("f8b4"), scoreCp = 52)), maxPlies = 1)
        .getOrElse(fail("bishop pin move should replay legally"))

    val witness =
      MoveReviewExchangeAnalyzer
        .pinWitness(replay, "f8b4", explicitTargets = List("c3"))
        .getOrElse(fail("bishop b4 should pin knight c3 to king e1"))

    assertEquals(witness.kind, MoveReviewExchangeAnalyzer.RelationKind.Pin)
    assertEquals(witness.focusSquares, List("b4", "c3", "e1"))
    assertEquals(witness.targetSquare, Some("c3"))
    assert(witness.facts.contains("absolute_pin"), clues(witness))
    val details =
      MoveReviewExchangeAnalyzer
        .pinDetailsFromWitness(witness)
        .getOrElse(fail("pin witness should carry typed line-relation details"))
    assertEquals(details.attackerSquare, "b4")
    assertEquals(details.pinnedSquare, "c3")
    assertEquals(details.behindSquare, "e1")
    assertEquals(details.targetSquare, "c3")
    assertEquals(details.attackerRole, "bishop")
    assertEquals(details.pinnedRole, "knight")
    assertEquals(details.behindRole, "king")
    assert(details.absolute, clues(details))
    val staleRaw =
      witness.copy(
        focusSquares = List("a1"),
        facts = List("pin_relation_witness", "attacker:a1", "pinned:a1", "behind:a1"),
        targetSquare = Some("a1")
      )
    assertEquals(MoveReviewExchangeAnalyzer.relationFocusSquaresFromWitness(staleRaw), List("b4", "c3", "e1"))
    assertEquals(MoveReviewExchangeAnalyzer.relationTargetSquareFromWitness(staleRaw), Some("c3"))
    assert(MoveReviewExchangeAnalyzer.relationFactTermsFromWitness(staleRaw).contains("attacker:b4"), clues(staleRaw))
    assert(!MoveReviewExchangeAnalyzer.relationFactTermsFromWitness(staleRaw).contains("attacker:a1"), clues(staleRaw))
    assertEquals(MoveReviewExchangeAnalyzer.pinWitness(replay, "f8b4", explicitTargets = List("a8")), None)
    assertEquals(MoveReviewExchangeAnalyzer.pinWitness(replay, "f8b4", explicitTargets = List("c3", "bad")), None)
    assertEquals(MoveReviewExchangeAnalyzer.pinWitness(replay, "f8e7"), None)
  }

  test("relation witnesses expose skewer only after a replayed long-range move lines up front and back targets") {
    val fen = "r6k/8/8/8/8/8/7K/4Q2R b - - 0 1"
    val replay =
      MoveReviewExchangeAnalyzer
        .boundedTopReplay(fen, List(VariationLine(moves = List("a8a1"), scoreCp = 76)), maxPlies = 1)
        .getOrElse(fail("rook skewer move should replay legally"))

    val witness =
      MoveReviewExchangeAnalyzer
        .skewerWitness(replay, "a8a1", explicitTargets = List("e1"))
        .getOrElse(fail("rook a1 should skewer queen e1 to rook h1"))

    assertEquals(witness.kind, MoveReviewExchangeAnalyzer.RelationKind.Skewer)
    assertEquals(witness.focusSquares, List("a1", "e1", "h1"))
    assertEquals(witness.targetSquare, Some("e1"))
    assert(witness.facts.contains("front_role:queen"), clues(witness))
    val details =
      MoveReviewExchangeAnalyzer
        .skewerDetailsFromWitness(witness)
        .getOrElse(fail("skewer witness should carry typed line-relation details"))
    assertEquals(details.attackerSquare, "a1")
    assertEquals(details.frontSquare, "e1")
    assertEquals(details.backSquare, "h1")
    assertEquals(details.targetSquare, "e1")
    assertEquals(details.attackerRole, "rook")
    assertEquals(details.frontRole, "queen")
    assertEquals(details.backRole, "rook")
    val staleRaw =
      witness.copy(
        focusSquares = List("a8"),
        facts = List("skewer_relation_witness", "attacker:a8", "front:a8", "back:a8"),
        targetSquare = Some("a8")
      )
    assertEquals(MoveReviewExchangeAnalyzer.relationFocusSquaresFromWitness(staleRaw), List("a1", "e1", "h1"))
    assertEquals(MoveReviewExchangeAnalyzer.relationTargetSquareFromWitness(staleRaw), Some("e1"))
    assert(MoveReviewExchangeAnalyzer.relationFactTermsFromWitness(staleRaw).contains("front:e1"), clues(staleRaw))
    assert(!MoveReviewExchangeAnalyzer.relationFactTermsFromWitness(staleRaw).contains("front:a8"), clues(staleRaw))
    assertEquals(MoveReviewExchangeAnalyzer.skewerWitness(replay, "a8a1", explicitTargets = List("a8")), None)
    assertEquals(MoveReviewExchangeAnalyzer.skewerWitness(replay, "a8a1", explicitTargets = List("e1", "bad")), None)
    assertEquals(MoveReviewExchangeAnalyzer.skewerWitness(replay, "a8a2"), None)
  }

  test("relation witnesses expose interference only when a legal move blocks a real defense line") {
    val fen = "k2r4/8/8/3q1N2/8/8/8/3Q2K1 w - - 0 1"
    val replay =
      MoveReviewExchangeAnalyzer
        .boundedTopReplay(fen, List(VariationLine(moves = List("f5d6"), scoreCp = 48)), maxPlies = 1)
        .getOrElse(fail("knight interference move should replay legally"))

    val witness =
      MoveReviewExchangeAnalyzer
        .interferenceWitness(replay, "f5d6", explicitTargets = List("d5"))
        .getOrElse(fail("knight d6 should interfere with rook d8 defending queen d5"))

    assertEquals(witness.kind, MoveReviewExchangeAnalyzer.RelationKind.Interference)
    assertEquals(witness.focusSquares, List("d6", "d8", "d5"))
    assert(witness.facts.contains("defender:d8"), clues(witness))
    val details =
      MoveReviewExchangeAnalyzer
        .interferenceDetailsFromWitness(witness)
        .getOrElse(fail("interference witness should carry typed line-relation details"))
    assertEquals(details.blockerSquare, "d6")
    assertEquals(details.defenderSquare, "d8")
    assertEquals(details.targetSquare, "d5")
    assertEquals(details.blockerRole, "knight")
    assertEquals(details.defenderRole, "rook")
    assertEquals(details.targetRole, "queen")
    val staleRaw =
      witness.copy(
        focusSquares = List("a1"),
        facts = List("interference_relation_witness", "blocker:a1", "defender:a1", "target:a1"),
        targetSquare = Some("a1")
      )
    assertEquals(MoveReviewExchangeAnalyzer.relationFocusSquaresFromWitness(staleRaw), List("d6", "d8", "d5"))
    assertEquals(MoveReviewExchangeAnalyzer.relationTargetSquareFromWitness(staleRaw), Some("d5"))
    assert(MoveReviewExchangeAnalyzer.relationFactTermsFromWitness(staleRaw).contains("defender_role:rook"), clues(staleRaw))
    assert(!MoveReviewExchangeAnalyzer.relationFactTermsFromWitness(staleRaw).contains("defender:a1"), clues(staleRaw))
    assertEquals(MoveReviewExchangeAnalyzer.interferenceWitness(replay, "f5d6", explicitTargets = List("a8")), None)
  }

  test("relation witnesses expose decoy only after the lured piece is legally won") {
    val fen = "k7/8/8/3q4/5N2/8/4B3/3Q2K1 w - - 0 1"
    val replay =
      MoveReviewExchangeAnalyzer
        .boundedTopReplay(fen, List(VariationLine(moves = List("f4d3", "d5d3", "e2d3"), scoreCp = 120)), maxPlies = 3)
        .getOrElse(fail("decoy branch should replay legally"))

    val witness =
      MoveReviewExchangeAnalyzer
        .decoyWitness(replay, "f4d3", explicitTargets = List("d3"))
        .getOrElse(fail("knight d3 should lure and win the queen on d3"))

    assertEquals(witness.kind, MoveReviewExchangeAnalyzer.RelationKind.Decoy)
    assertEquals(witness.focusSquares, List("f4", "d3", "d5"))
    assert(witness.facts.contains("lured_role:queen"), clues(witness))
    val details =
      MoveReviewExchangeAnalyzer
        .decoyDetailsFromWitness(witness)
        .getOrElse(fail("decoy witness should carry typed lure-and-win details"))
    assertEquals(details.baitFromSquare, "f4")
    assertEquals(details.baitSquare, "d3")
    assertEquals(details.luredFromSquare, "d5")
    assertEquals(details.executionFromSquare, "e2")
    assertEquals(details.executionToSquare, "d3")
    assertEquals(details.baitRole, "knight")
    assertEquals(details.luredRole, "queen")
    val staleRaw =
      witness.copy(
        focusSquares = List("a1"),
        facts = List("decoy_relation_witness", "bait:a1", "lured_from:a1", "lured_role:queen"),
        targetSquare = Some("a1")
      )
    assertEquals(MoveReviewExchangeAnalyzer.relationFocusSquaresFromWitness(staleRaw), List("f4", "d3", "d5"))
    assertEquals(MoveReviewExchangeAnalyzer.relationTargetSquareFromWitness(staleRaw), Some("d3"))
    assert(MoveReviewExchangeAnalyzer.relationFactTermsFromWitness(staleRaw).contains("execution:e2-d3"), clues(staleRaw))
    assert(!MoveReviewExchangeAnalyzer.relationFactTermsFromWitness(staleRaw).contains("bait:a1"), clues(staleRaw))
    assertEquals(MoveReviewExchangeAnalyzer.decoyWitness(replay, "f4d3", explicitTargets = List("a8")), None)
  }

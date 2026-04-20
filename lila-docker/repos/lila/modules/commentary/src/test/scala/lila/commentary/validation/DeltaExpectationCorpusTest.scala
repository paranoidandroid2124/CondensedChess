package lila.commentary.validation

import chess.{ Bishop, Color, King, Knight, Queen, Rook, Square }

import lila.commentary.CommentaryCore
import lila.commentary.delta.{ StrategicDelta, StrategicDeltaExtraction, StrategicDeltaExtractor }
import lila.commentary.root.RootExtractor
import lila.commentary.strategic.{ StrategicObjectContext, StrategicObjectExtraction }
import lila.commentary.witness.WitnessValue

class DeltaExpectationCorpusTest extends munit.FunSuite:

  private val rows = DeltaExpectationCorpus.loadAll()
  private val caseTypesByFamily = rows.groupMap(_.family)(_.caseType).view.mapValues(_.toSet).toMap
  private val pressureTargetsByFamily =
    rows.groupBy(_.validatedFamily).view.mapValues(_.map(_.validatedPressureTarget).toSet).toMap

  test("delta runtime corpus carries exact, near_miss, nasty_negative, and move_local_false_witness coverage for both delta rows"):
    DeltaExpectationCorpus.requiredFamilies.foreach: family =>
      val actualCaseTypes = caseTypesByFamily.getOrElse(family, Set.empty)
      assertEquals(
        actualCaseTypes,
        DeltaExpectationCorpus.requiredCaseTypes,
        clues(s"$family case types = ${actualCaseTypes.toVector.sorted.mkString(",")}")
      )

  test("delta runtime corpus carries every frozen pressure-target law for both delta rows"):
    DeltaExpectationCorpus.requiredFamilies.foreach: family =>
      val actualPressureTargets = pressureTargetsByFamily.getOrElse(family, Set.empty)
      val expectedPressureTargets =
        DeltaExpectationCorpus.requiredPressureTargetsByFamily.getOrElse(family, Set.empty)
      assertEquals(
        actualPressureTargets,
        expectedPressureTargets,
        clues(s"$family pressure targets = ${actualPressureTargets.toVector.sorted.mkString(",")}")
      )

  rows.foreach: row =>
    test(s"delta runtime row parses, stays board-coherent, and matches live extraction for ${row.id}"):
      row.validatedCaseType
      row.validatedExpectation
      row.validatedFamily
      row.validatedOwner
      row.validatedScope
      row.validatedDeltaTag
      row.expectedAnchor
      row.validatedPressureTarget
      row.validatedHelpers
      row.validatedCanonicalCorridorPairAfter
      row.validatedPersistentCarrierFamily
      row.validatedPersistentCarrierAnchor
      row.validatedForbiddenRivalFamilies
      row.validatedMove

      RootExtractor.fromFen(row.normalizedFenBefore).fold(
        message => fail(s"Row ${row.id} before-FEN parse failed: $message"),
        identity
      )
      RootExtractor.fromFen(row.normalizedFenAfter).fold(
        message => fail(s"Row ${row.id} after-FEN parse failed: $message"),
        identity
      )

      val before = snapshotFor(row.normalizedFenBefore)
      val after = snapshotFor(row.normalizedFenAfter)
      val runtimeExtraction =
        StrategicDeltaExtractor
          .fromFens(row.normalizedFenBefore, row.validatedMove, row.normalizedFenAfter)
          .fold(message => fail(s"Row ${row.id} runtime extraction failed: $message"), identity)
      val publicExtraction =
        CommentaryCore
          .extractStrategicDeltasFromFens(row.fenBefore, row.playedMove, row.fenAfter)
          .fold(message => fail(s"Row ${row.id} public extraction failed: $message"), identity)

      assertBoardCoherentMoveTransition(row, before.context, after.context)
      assertPressureTargetEvidence(row, before, after)
      assertFrozenAdmissionContract(row, before, after)
      assertForbiddenRivalFamilies(row, before, after)
      assertRuntimeExtraction(row, runtimeExtraction, before.context)
      assertEquals(publicExtraction, runtimeExtraction, clues(s"${row.id} public facade drift"))

      if row.caseType == "exact" then assertEquals(row.expectation, "present")
      else assertEquals(row.expectation, "absent")

  private def assertRuntimeExtraction(
      row: DeltaExpectationCorpus.Row,
      extraction: StrategicDeltaExtraction,
      beforeContext: StrategicObjectContext
  ): Unit =
    val familyDeltas = extraction.deltas.forFamilyId(row.family)
    row.validatedForbiddenRivalFamilies.foreach(rival =>
      assertEquals(
        extraction.deltas.forFamilyId(rival),
        Vector.empty[StrategicDelta],
        clues(s"${row.id} emitted forbidden rival $rival")
      )
    )

    if row.expectation == "present" then
      assertEquals(
        extraction.deltas.all.size,
        1,
        clues(s"${row.id} emitted unexpected extra live deltas")
      )
      assertEquals(familyDeltas.size, 1, clues(s"${row.id} missing expected live ${row.family}"))
      assertRuntimeDeltaMatchesRow(row, familyDeltas.head, beforeContext)
    else
      assertEquals(
        extraction.deltas.all,
        Vector.empty[StrategicDelta],
        clues(s"${row.id} should stay delta-absent at runtime")
      )

  private def assertRuntimeDeltaMatchesRow(
      row: DeltaExpectationCorpus.Row,
      delta: StrategicDelta,
      beforeContext: StrategicObjectContext
  ): Unit =
    assertEquals(delta.familyId.value, row.family)
    assertEquals(delta.scope.key, row.scope)
    assertEquals(delta.deltaTag.value, row.deltaTag)
    assertEquals(delta.anchor, row.expectedAnchor)
    assertEquals(
      delta.color.map(color => if color.white then "white" else "black"),
      Some(row.owner)
    )
    assertEquals(delta.support.supportingTags.toSet, row.validatedHelpers.toSet)

    row.family match
      case "TradeCompressionCorridor" =>
        val corridorPair =
          row.validatedCanonicalCorridorPairAfter.getOrElse(
            fail(s"${row.id} missing canonical corridor pair")
          )
        assertEquals(
          delta.payload.get("capturedRole"),
          beforeContext.pieceAt(row.validatedMove.dest).map(piece => WitnessValue.RoleValue(piece.role))
        )
        assertEquals(
          delta.payload.get("corridorPairSquares"),
          Some(WitnessValue.SquareListValue(Vector(corridorPair._1, corridorPair._2)))
        )
        assertEquals(
          delta.payload.get("captureSquare"),
          Some(WitnessValue.SquareValue(row.validatedMove.dest))
        )
        assertEquals(
          delta.payload.get("corridorKind"),
          Some(WitnessValue.Token(
            if corridorPair._1.file == corridorPair._2.file then "file" else "diagonal"
          ))
        )
        assertEquals(
          delta.support.targetSquares,
          Vector(corridorPair._1, corridorPair._2)
        )
        assert(delta.support.rootIndices.nonEmpty, clues(s"${row.id} corridor support must keep root evidence"))
      case "TradeInvariant" =>
        assertEquals(
          delta.payload.get("persistent_carrier_family"),
          Some(WitnessValue.Token(
            row.validatedPersistentCarrierFamily.getOrElse(
              fail(s"${row.id} missing persistent carrier family")
            )
          ))
        )
        assertEquals(
          delta.payload.get("persistent_carrier_anchor"),
          Some(WitnessValue.Token(
            row.validatedPersistentCarrierAnchor.getOrElse(
              fail(s"${row.id} missing persistent carrier anchor")
            ).key
          ))
        )
        assertEquals(
          delta.payload.get("material_reduction"),
          Some(WitnessValue.Number(1))
        )
        assertEquals(
          delta.support.targetSquares,
          Vector(row.validatedMove.dest)
        )
        assert(delta.support.rootIndices.nonEmpty, clues(s"${row.id} invariant support must keep root evidence"))
      case other =>
        fail(s"Unhandled runtime delta family $other")

  private def assertBoardCoherentMoveTransition(
      row: DeltaExpectationCorpus.Row,
      before: StrategicObjectContext,
      after: StrategicObjectContext
  ): Unit =
    val move = row.validatedMove
    val mover =
      row.owner match
        case "white" => Color.White
        case "black" => Color.Black
        case other => fail(s"Unhandled owner in simple move transition: $other")
    val originPiece =
      before.pieceAt(move.orig).getOrElse(fail(s"${row.id} has no moving piece on ${move.orig.key}"))
    val destinationBefore = before.pieceAt(move.dest)
    val destinationAfter =
      after.pieceAt(move.dest).getOrElse(fail(s"${row.id} has no moved piece on ${move.dest.key}"))

    assertEquals(originPiece.color, mover)
    assertEquals(after.pieceAt(move.orig), None)
    assertEquals(destinationAfter.color, mover)
    assertEquals(destinationAfter.role, move.promotion.getOrElse(originPiece.role))

    Square.all.foreach: square =>
      if square != move.orig && square != move.dest then
        assertEquals(
          before.pieceAt(square),
          after.pieceAt(square),
          clues(s"${row.id} changed unrelated square ${square.key}")
        )

    val captures = destinationBefore.exists(_.color != mover)
    if captures then
      assert(destinationBefore.exists(_.role != King), s"${row.id} must not capture a king")
    else
      assertEquals(destinationBefore, None, clues(s"${row.id} expected a quiet move target"))

    assert(
      pieceMoveShapeIsLegal(before, originPiece.role, mover, move.orig, move.dest, captures),
      s"${row.id} move shape ${move.orig.key}${move.dest.key} is illegal for ${originPiece.role}"
    )

  private def assertFrozenAdmissionContract(
      row: DeltaExpectationCorpus.Row,
      before: DeltaSnapshot,
      after: DeltaSnapshot
  ): Unit =
    val admitted =
      row.family match
        case "TradeCompressionCorridor" => matchesTradeCompressionCorridor(row, before, after)
        case "TradeInvariant" => matchesTradeInvariant(row, before, after)
        case other => fail(s"Unhandled delta family in frozen admission contract: $other")
    assertEquals(
      admitted,
      row.expectation == "present",
      clues(s"${row.id} frozen admission mismatch for ${row.family}")
    )

  private def assertForbiddenRivalFamilies(
      row: DeltaExpectationCorpus.Row,
      before: DeltaSnapshot,
      after: DeltaSnapshot
  ): Unit =
    row.validatedForbiddenRivalFamilies.foreach: rivalFamily =>
      val rivalAdmitted =
        rivalFamily match
          case "TradeCompressionCorridor" => genericTradeCompressionCorridor(before, after)
          case "TradeInvariant" => genericTradeInvariant(row, before, after)
          case other => fail(s"Unhandled forbidden rival family: $other")
      assert(
        !rivalAdmitted,
        s"${row.id} satisfies forbidden rival family $rivalFamily"
      )

  private def pieceMoveShapeIsLegal(
      context: StrategicObjectContext,
      role: chess.Role,
      mover: Color,
      orig: Square,
      dest: Square,
      captures: Boolean
  ): Boolean =
    role match
      case chess.Rook =>
        orig.onSameLine(dest) && clearSliderPath(context, orig, dest)
      case chess.Bishop =>
        orig.onSameDiagonal(dest) && clearSliderPath(context, orig, dest)
      case chess.Queen =>
        (orig.onSameLine(dest) || orig.onSameDiagonal(dest)) &&
          clearSliderPath(context, orig, dest)
      case chess.King =>
        math.abs(orig.file.value - dest.file.value) <= 1 &&
          math.abs(orig.rank.value - dest.rank.value) <= 1
      case chess.Pawn =>
        val rankDelta = dest.rank.value - orig.rank.value
        val fileDelta = math.abs(dest.file.value - orig.file.value)
        val forward = if mover.white then 1 else -1
        if captures then rankDelta == forward && fileDelta == 1
        else rankDelta == forward && fileDelta == 0 && context.pieceAt(dest).isEmpty
      case chess.Knight =>
        val fileDelta = math.abs(orig.file.value - dest.file.value)
        val rankDelta = math.abs(orig.rank.value - dest.rank.value)
        Set((1, 2), (2, 1)).contains((fileDelta, rankDelta))

  private def clearSliderPath(
      context: StrategicObjectContext,
      orig: Square,
      dest: Square
  ): Boolean =
    val fileStep = Integer.compare(dest.file.value, orig.file.value)
    val rankStep = Integer.compare(dest.rank.value, orig.rank.value)
    LazyList
      .iterate((orig.file.value + fileStep, orig.rank.value + rankStep)) {
        case (fileValue, rankValue) => (fileValue + fileStep, rankValue + rankStep)
      }
      .takeWhile { case (fileValue, rankValue) =>
        fileValue != dest.file.value || rankValue != dest.rank.value
      }
      .forall { case (fileValue, rankValue) =>
        Square.all
          .find(square => square.file.value == fileValue && square.rank.value == rankValue)
          .forall(square => context.pieceAt(square).isEmpty)
      }

  private def assertPressureTargetEvidence(
      row: DeltaExpectationCorpus.Row,
      before: DeltaSnapshot,
      after: DeltaSnapshot
  ): Unit =
    val persistentCarrierFamily = row.validatedPersistentCarrierFamily
    val persistentCarrierAnchor = row.validatedPersistentCarrierAnchor
    row.pressureTarget match
      case "capture_creates_compressed_exchange_corridor" =>
        assert(capturesNonKingPiece(row, before.context))
        assertEquals(nonKingNonPawnCount(before.context) - nonKingNonPawnCount(after.context), 1)
        assertDeclaredCorridorPairAfter(row, after.context)
        assert(reciprocalExchangeCorridorPairs(after.context).nonEmpty)
        assert(compressedTradeWindow(after.context))
        assert(
          reciprocalExchangeCorridorPairs(before.context).isEmpty || !compressedTradeWindow(before.context)
        )
      case "liquidation_without_shared_corridor" =>
        assert(capturesNonKingPiece(row, before.context))
        assertEquals(nonKingNonPawnCount(before.context) - nonKingNonPawnCount(after.context), 1)
        assertEquals(reciprocalExchangeCorridorPairs(after.context), Vector.empty)
      case "shared_corridor_without_compressed_window" =>
        assert(capturesNonKingPiece(row, before.context))
        assertEquals(nonKingNonPawnCount(before.context) - nonKingNonPawnCount(after.context), 1)
        assertDeclaredCorridorPairAfter(row, after.context)
        assert(reciprocalExchangeCorridorPairs(after.context).nonEmpty)
        assert(!compressedTradeWindow(after.context))
      case "shared_corridor_without_material_compression" =>
        assert(capturesNonKingPiece(row, before.context))
        assertEquals(nonKingNonPawnCount(before.context) - nonKingNonPawnCount(after.context), 1)
        assertDeclaredCorridorPairAfter(row, after.context)
        assertEquals(queensRemain(after.context), false)
        assert(reciprocalExchangeCorridorPairs(after.context).nonEmpty)
        assert(nonKingNonPawnCount(after.context) > 4)
        assert(!compressedTradeWindow(after.context))
      case "shared_corridor_without_unique_pair" =>
        assert(capturesNonKingPiece(row, before.context))
        assertEquals(nonKingNonPawnCount(before.context) - nonKingNonPawnCount(after.context), 1)
        assertEquals(queensRemain(after.context), false)
        assert(compressedTradeWindow(after.context))
        assert(reciprocalExchangeCorridorPairs(after.context).size > 1)
        assert(!genericTradeCompressionShape(before.context))
      case "corridor_picture_without_trade_reduction" =>
        assert(!capturesNonKingPiece(row, before.context))
        assertDeclaredCorridorPairAfter(row, after.context)
        assert(reciprocalExchangeCorridorPairs(after.context).nonEmpty)
        assertEquals(nonKingNonPawnCount(before.context), nonKingNonPawnCount(after.context))
      case "corridor_already_true_before_trade" =>
        assert(capturesNonKingPiece(row, before.context))
        assertEquals(nonKingNonPawnCount(before.context) - nonKingNonPawnCount(after.context), 1)
        assertDeclaredCorridorPairAfter(row, after.context)
        assert(reciprocalExchangeCorridorPairs(after.context).nonEmpty)
        assert(compressedTradeWindow(after.context))
        assert(genericTradeCompressionShape(before.context))
      case "bounded_capture_preserves_endgame_race" =>
        assert(capturesNonKingPiece(row, before.context))
        assertEquals(nonKingNonPawnCount(before.context) - nonKingNonPawnCount(after.context), 1)
        assert(
          hasAnchoredObject(
            before.extraction,
            persistentCarrierFamily.getOrElse(fail(s"${row.id} missing persistent carrier family")),
            persistentCarrierAnchor.getOrElse(fail(s"${row.id} missing persistent carrier anchor"))
          )
        )
        assert(
          hasAnchoredObject(
            after.extraction,
            persistentCarrierFamily.getOrElse(fail(s"${row.id} missing persistent carrier family")),
            persistentCarrierAnchor.getOrElse(fail(s"${row.id} missing persistent carrier anchor"))
          )
        )
        assert(
          moverCarrierContinuity(
            row,
            before,
            after,
            persistentCarrierFamily.getOrElse(fail(s"${row.id} missing persistent carrier family")),
            persistentCarrierAnchor.getOrElse(fail(s"${row.id} missing persistent carrier anchor"))
          )
        )
      case "trade_without_persistent_carrier" =>
        assert(capturesNonKingPiece(row, before.context))
        assertEquals(nonKingNonPawnCount(before.context) - nonKingNonPawnCount(after.context), 1)
        assert(
          !hasAnchoredObject(
            after.extraction,
            persistentCarrierFamily.getOrElse(fail(s"${row.id} missing persistent carrier family")),
            persistentCarrierAnchor.getOrElse(fail(s"${row.id} missing persistent carrier anchor"))
          )
        )
      case "winning_look_without_invariant_carrier" =>
        assert(capturesNonKingPiece(row, before.context))
        assertEquals(nonKingNonPawnCount(before.context) - nonKingNonPawnCount(after.context), 1)
        assert(
          !hasAnchoredObject(
            before.extraction,
            persistentCarrierFamily.getOrElse(fail(s"${row.id} missing persistent carrier family")),
            persistentCarrierAnchor.getOrElse(fail(s"${row.id} missing persistent carrier anchor"))
          )
        )
        assert(
          !hasAnchoredObject(
            after.extraction,
            persistentCarrierFamily.getOrElse(fail(s"${row.id} missing persistent carrier family")),
            persistentCarrierAnchor.getOrElse(fail(s"${row.id} missing persistent carrier anchor"))
          )
        )
        assert(after.context.activePieceSquares(Color.White, chess.Pawn).exists(_.key == "c7"))
      case "quiet_move_preserves_task_without_trade" =>
        assert(!capturesNonKingPiece(row, before.context))
        assertEquals(nonKingNonPawnCount(before.context), nonKingNonPawnCount(after.context))
        assert(
          hasAnchoredObject(
            before.extraction,
            persistentCarrierFamily.getOrElse(fail(s"${row.id} missing persistent carrier family")),
            persistentCarrierAnchor.getOrElse(fail(s"${row.id} missing persistent carrier anchor"))
          )
        )
        assert(
          hasAnchoredObject(
            after.extraction,
            persistentCarrierFamily.getOrElse(fail(s"${row.id} missing persistent carrier family")),
            persistentCarrierAnchor.getOrElse(fail(s"${row.id} missing persistent carrier anchor"))
          )
        )
      case "pawn_capture_preserves_task_without_bounded_reduction" =>
        assert(capturesNonKingPiece(row, before.context))
        assertEquals(nonKingNonPawnCount(before.context), nonKingNonPawnCount(after.context))
        assert(
          hasAnchoredObject(
            before.extraction,
            persistentCarrierFamily.getOrElse(fail(s"${row.id} missing persistent carrier family")),
            persistentCarrierAnchor.getOrElse(fail(s"${row.id} missing persistent carrier anchor"))
          )
        )
        assert(
          hasAnchoredObject(
            after.extraction,
            persistentCarrierFamily.getOrElse(fail(s"${row.id} missing persistent carrier family")),
            persistentCarrierAnchor.getOrElse(fail(s"${row.id} missing persistent carrier anchor"))
          )
        )
      case "trade_switches_persistent_carrier" =>
        assert(capturesNonKingPiece(row, before.context))
        assertEquals(nonKingNonPawnCount(before.context) - nonKingNonPawnCount(after.context), 1)
        assert(
          hasAnchoredObject(
            before.extraction,
            persistentCarrierFamily.getOrElse(fail(s"${row.id} missing persistent carrier family")),
            persistentCarrierAnchor.getOrElse(fail(s"${row.id} missing persistent carrier anchor"))
          )
        )
        assert(
          hasAnchoredObject(
            after.extraction,
            persistentCarrierFamily.getOrElse(fail(s"${row.id} missing persistent carrier family")),
            persistentCarrierAnchor.getOrElse(fail(s"${row.id} missing persistent carrier anchor"))
          )
        )
        assert(
          !moverCarrierContinuity(
            row,
            before,
            after,
            persistentCarrierFamily.getOrElse(fail(s"${row.id} missing persistent carrier family")),
            persistentCarrierAnchor.getOrElse(fail(s"${row.id} missing persistent carrier anchor"))
          )
        )
      case "trade_creates_after_only_carrier" =>
        assert(capturesNonKingPiece(row, before.context))
        assertEquals(nonKingNonPawnCount(before.context) - nonKingNonPawnCount(after.context), 1)
        assert(
          !hasAnchoredObject(
            before.extraction,
            persistentCarrierFamily.getOrElse(fail(s"${row.id} missing persistent carrier family")),
            persistentCarrierAnchor.getOrElse(fail(s"${row.id} missing persistent carrier anchor"))
          )
        )
        assert(
          hasAnchoredObject(
            after.extraction,
            persistentCarrierFamily.getOrElse(fail(s"${row.id} missing persistent carrier family")),
            persistentCarrierAnchor.getOrElse(fail(s"${row.id} missing persistent carrier anchor"))
          )
        )
      case other =>
        fail(s"Unhandled delta pressureTarget evidence check: $other")

  private def capturesNonKingPiece(
      row: DeltaExpectationCorpus.Row,
      before: StrategicObjectContext
  ): Boolean =
    before.pieceAt(row.validatedMove.dest).exists(_.role != chess.King)

  private def compressedTradeWindow(context: StrategicObjectContext): Boolean =
    !queensRemain(context) && nonKingNonPawnCount(context) <= 4

  private def queensRemain(context: StrategicObjectContext): Boolean =
    context.activePieceSquares(Color.White, Queen).nonEmpty ||
      context.activePieceSquares(Color.Black, Queen).nonEmpty

  private def nonKingNonPawnCount(context: StrategicObjectContext): Int =
    Vector(Knight, Bishop, Rook, Queen)
      .map(role =>
        context.activePieceSquares(Color.White, role).size +
          context.activePieceSquares(Color.Black, role).size
      )
      .sum

  private def reciprocalExchangeCorridorPairs(
      context: StrategicObjectContext
  ): Vector[(Square, Square)] =
    val whiteSquares = attackableNonKingSquares(context, Color.White)
    val blackSquares = attackableNonKingSquares(context, Color.Black)
    whiteSquares
      .flatMap: whiteSquare =>
        blackSquares.collect:
          case blackSquare
              if (whiteSquare.file == blackSquare.file || whiteSquare.onSameDiagonal(blackSquare)) &&
                context.board.attacksSquare(whiteSquare, blackSquare) &&
                context.board.attacksSquare(blackSquare, whiteSquare) =>
                (whiteSquare, blackSquare)
      .distinct
      .sortBy: (whiteSquare, blackSquare) =>
        s"${whiteSquare.key}|${blackSquare.key}"

  private def assertDeclaredCorridorPairAfter(
      row: DeltaExpectationCorpus.Row,
      context: StrategicObjectContext
  ): Unit =
    val declaredPair =
      row.validatedCanonicalCorridorPairAfter.getOrElse(
        fail(s"${row.id} must declare a canonical corridor pair")
      )
    assertEquals(
      reciprocalExchangeCorridorPairs(context),
      Vector(declaredPair),
      clues(s"${row.id} after-board corridor pair mismatch")
    )

  private def matchesTradeCompressionCorridor(
      row: DeltaExpectationCorpus.Row,
      before: DeltaSnapshot,
      after: DeltaSnapshot
  ): Boolean =
    val declaredPair = row.validatedCanonicalCorridorPairAfter
    capturesNonKingPiece(row, before.context) &&
    nonKingNonPawnCount(before.context) - nonKingNonPawnCount(after.context) == 1 &&
    compressedTradeWindow(after.context) &&
    declaredPair.exists(pair => reciprocalExchangeCorridorPairs(after.context) == Vector(pair)) &&
    !genericTradeCompressionShape(before.context)

  private def genericTradeCompressionCorridor(
      before: DeltaSnapshot,
      after: DeltaSnapshot
  ): Boolean =
    nonKingNonPawnCount(before.context) - nonKingNonPawnCount(after.context) == 1 &&
    compressedTradeWindow(after.context) &&
    reciprocalExchangeCorridorPairs(after.context).size == 1 &&
    !genericTradeCompressionShape(before.context)

  private def genericTradeCompressionShape(context: StrategicObjectContext): Boolean =
    compressedTradeWindow(context) && reciprocalExchangeCorridorPairs(context).size == 1

  private def matchesTradeInvariant(
      row: DeltaExpectationCorpus.Row,
      before: DeltaSnapshot,
      after: DeltaSnapshot
  ): Boolean =
    capturesNonKingPiece(row, before.context) &&
    nonKingNonPawnCount(before.context) - nonKingNonPawnCount(after.context) == 1 &&
    hasAnchoredObject(
      before.extraction,
      row.validatedPersistentCarrierFamily.getOrElse(
        fail(s"${row.id} missing persistent carrier family")
      ),
      row.validatedPersistentCarrierAnchor.getOrElse(
        fail(s"${row.id} missing persistent carrier anchor")
      )
    ) &&
    hasAnchoredObject(
      after.extraction,
      row.validatedPersistentCarrierFamily.getOrElse(
        fail(s"${row.id} missing persistent carrier family")
      ),
      row.validatedPersistentCarrierAnchor.getOrElse(
        fail(s"${row.id} missing persistent carrier anchor")
      )
    ) &&
    moverCarrierContinuity(
      row,
      before,
      after,
      row.validatedPersistentCarrierFamily.getOrElse(
        fail(s"${row.id} missing persistent carrier family")
      ),
      row.validatedPersistentCarrierAnchor.getOrElse(
        fail(s"${row.id} missing persistent carrier anchor")
      )
    )

  private def genericTradeInvariant(
      row: DeltaExpectationCorpus.Row,
      before: DeltaSnapshot,
      after: DeltaSnapshot
  ): Boolean =
    nonKingNonPawnCount(before.context) - nonKingNonPawnCount(after.context) == 1 &&
    hasAnchoredObject(
      before.extraction,
      "EndgameRaceScaffold",
      lila.commentary.witness.WitnessAnchor.BoardAnchor
    ) &&
    hasAnchoredObject(
      after.extraction,
      "EndgameRaceScaffold",
      lila.commentary.witness.WitnessAnchor.BoardAnchor
    ) &&
    moverCarrierContinuity(
      row,
      before,
      after,
      "EndgameRaceScaffold",
      lila.commentary.witness.WitnessAnchor.BoardAnchor
    )

  private def attackableNonKingSquares(
      context: StrategicObjectContext,
      color: Color
  ): Vector[Square] =
    Vector(Knight, Bishop, Rook, Queen).flatMap(role => context.activePieceSquares(color, role))

  private def hasAnchoredObject(
      extraction: StrategicObjectExtraction,
      family: String,
      anchor: lila.commentary.witness.WitnessAnchor
  ): Boolean =
    extraction.objects.forFamilyId(family).exists(_.anchor == anchor)

  private def moverCarrierContinuity(
      row: DeltaExpectationCorpus.Row,
      before: DeltaSnapshot,
      after: DeltaSnapshot,
      family: String,
      anchor: lila.commentary.witness.WitnessAnchor
  ): Boolean =
    val mover = ownerColor(row.owner)
    val beforeSquares = clearRunSquares(before.extraction, family, anchor, mover)
    val afterSquares = clearRunSquares(after.extraction, family, anchor, mover)
    stationaryCarrierPersists(beforeSquares, afterSquares) ||
      movedCarrierPersists(row, before.context, mover, beforeSquares, afterSquares)

  private def clearRunSquares(
      extraction: StrategicObjectExtraction,
      family: String,
      anchor: lila.commentary.witness.WitnessAnchor,
      color: Color
  ): Vector[Square] =
    val field = if color.white then "white_clear_run_squares" else "black_clear_run_squares"
    extraction.objects
      .forFamilyId(family)
      .filter(_.anchor == anchor)
      .flatMap(_.payload.get(field).toVector)
      .flatMap:
        case WitnessValue.SquareListValue(values) => values
        case _ => Vector.empty
      .distinct
      .sortBy(_.value)

  private def stationaryCarrierPersists(
      beforeSquares: Vector[Square],
      afterSquares: Vector[Square]
  ): Boolean =
    beforeSquares.exists(afterSquares.contains)

  private def movedCarrierPersists(
      row: DeltaExpectationCorpus.Row,
      before: StrategicObjectContext,
      mover: Color,
      beforeSquares: Vector[Square],
      afterSquares: Vector[Square]
  ): Boolean =
    before.pieceAt(row.validatedMove.orig).exists(piece =>
      piece.color == mover &&
        piece.role == chess.Pawn &&
        beforeSquares.contains(row.validatedMove.orig) &&
        afterSquares.contains(row.validatedMove.dest)
    )

  private def ownerColor(owner: String): Color =
    owner match
      case "white" => Color.White
      case "black" => Color.Black
      case other => fail(s"Unhandled owner color: $other")

  private def snapshotFor(fen: chess.format.Fen.Full): DeltaSnapshot =
    val extraction =
      CommentaryCore.extractStrategicObjects(fen).fold(message => fail(message), identity)
    DeltaSnapshot(
      extraction,
      StrategicObjectContext(
        extraction.rootState,
        extraction.primaryWitnesses,
        extraction.attachedWitnesses
      )
    )

  private final case class DeltaSnapshot(
      extraction: StrategicObjectExtraction,
      context: StrategicObjectContext
  )

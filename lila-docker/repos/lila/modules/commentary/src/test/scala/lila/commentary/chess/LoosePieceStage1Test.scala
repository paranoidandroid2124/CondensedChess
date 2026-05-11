package lila.commentary.chess

import java.nio.file.{ Files, Path, Paths }
import scala.jdk.CollectionConverters.*

class LoosePieceStage1Test extends munit.FunSuite:

  private val looseFen = "4k3/8/8/7b/8/8/3R4/4K3 w - - 0 1"
  private val guardedFen = "4k3/8/8/7b/5n2/8/3R4/4K3 w - - 0 1"
  private val kingTargetFen = "8/8/8/7k/8/8/3R4/4K3 w - - 0 1"
  private val looseMove = Line(Square('d', 2), Square('h', 2))
  private val quietMove = Line(Square('d', 2), Square('a', 2))
  private val illegalMove = Line(Square('d', 2), Square('h', 8))

  test("Stage-1 LoosePieceProof proves exact legal after-board loose attack"):
    val facts = BoardFacts.fromFen(looseFen).toOption.get
    val proof = LoosePieceProof.fromBoardFacts(facts, looseMove)

    assertEquals(proof.complete, true)
    assertEquals(proof.publicClaimAllowed, false)
    assertEquals(proof.attackingSide, Side.White)
    assertEquals(proof.rivalSide, Side.Black)
    assertEquals(proof.attackMove, Some(looseMove))
    assertEquals(proof.originSquare, Some(Square('d', 2)))
    assertEquals(proof.destinationSquare, Some(Square('h', 2)))
    assertEquals(proof.targetPieceAfter.map(_.man), Some(Man.Bishop))
    assertEquals(proof.targetPieceSquareAfter, Some(Square('h', 5)))
    assertEquals(proof.targetRivalOwned, true)
    assertEquals(proof.targetNonKing, true)
    assertEquals(proof.attackingPieceSquareAfter, Some(Square('h', 2)))
    assertEquals(proof.legalMove, true)
    assertEquals(proof.sameBoardProof, true)
    assertEquals(proof.exactAfterBoardReplay, true)
    assertEquals(proof.afterBoardTargetAttackedByMovingSide, true)
    assertEquals(proof.rivalLegalDefendersAfter, Vector.empty)
    assertEquals(proof.rivalSideHasNoLegalDefenderOfTarget, true)
    assertEquals(proof.looseAttackProducedOrRevealedByLegalMove, true)
    assertEquals(proof.missingEvidence, Vector.empty)

  test("Stage-1 LoosePieceProof is complete proof or silence"):
    val facts = BoardFacts.fromFen(looseFen).toOption.get
    val guardedFacts = BoardFacts.fromFen(guardedFen).toOption.get
    val kingTargetFacts = BoardFacts.fromFen(kingTargetFen).toOption.get

    val quietProof = LoosePieceProof.fromBoardFacts(facts, quietMove)
    assertEquals(quietProof.complete, false)
    assertEquals(quietProof.afterBoardTargetAttackedByMovingSide, false)
    assert(quietProof.missingEvidence.exists(_.missing.contains("after-board loose target attack")))

    val guardedProof = LoosePieceProof.fromBoardFacts(guardedFacts, looseMove)
    assertEquals(guardedProof.complete, false)
    assertEquals(guardedProof.rivalSideHasNoLegalDefenderOfTarget, false)
    assertEquals(guardedProof.rivalLegalDefendersAfter.map(_.square), Vector(Square('f', 4)))
    assert(guardedProof.missingEvidence.exists(_.missing.contains("zero legal defenders of target square")))

    val illegalProof = LoosePieceProof.fromBoardFacts(facts, illegalMove)
    assertEquals(illegalProof.complete, false)
    assertEquals(illegalProof.legalMove, false)
    assert(illegalProof.missingEvidence.exists(_.missing.contains("legal move identity")))

    val kingTargetProof = LoosePieceProof.fromBoardFacts(kingTargetFacts, looseMove)
    assertEquals(kingTargetProof.complete, false)
    assertEquals(kingTargetProof.targetNonKing, false)
    assert(kingTargetProof.missingEvidence.exists(_.missing.contains("target piece is not king")))

    val untrustedFacts = BoardFacts.untrusted(
      root = facts.root,
      sideToMove = facts.sideToMove,
      header = facts.header,
      sideLegal = facts.sideLegal,
      rivalLegal = facts.rivalLegal,
      control = facts.control,
      material = facts.material,
      pawns = facts.pawns,
      pieces = facts.pieces
    )
    val untrustedProof = LoosePieceProof.fromBoardFacts(untrustedFacts, looseMove)
    assertEquals(untrustedProof.complete, false)
    assertEquals(untrustedProof.sameBoardProof, false)
    assert(untrustedProof.missingEvidence.exists(_.missing.contains("same-board proof")))

  test("Stage-1 LoosePieceProof remains below Story and downstream speech surface"):
    val runtimeText = scalaSourceText(Paths.get("modules/commentary/src/main/scala/lila/commentary/chess"))

    Vector(
      "LoosePieceStory",
      "wins loose piece"
    ).foreach: forbidden =>
      assert(!runtimeText.contains(forbidden), s"Stage-1 must not open runtime Loose surface: $forbidden")

  private def scalaSourceText(root: Path): String =
    val stream = Files.walk(root)
    try
      stream
        .iterator()
        .asScala
        .filter(path => Files.isRegularFile(path) && path.toString.endsWith(".scala"))
        .map(Files.readString)
        .mkString("\n")
    finally stream.close()

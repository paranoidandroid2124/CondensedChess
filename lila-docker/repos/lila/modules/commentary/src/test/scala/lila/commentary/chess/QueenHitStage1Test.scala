package lila.commentary.chess

import java.nio.file.{ Files, Path, Paths }
import scala.jdk.CollectionConverters.*

class QueenHitStage1Test extends munit.FunSuite:

  private val queenHitFen = "4k3/8/8/7q/8/8/3R4/4K3 w - - 0 1"
  private val noQueenFen = "4k3/8/8/8/8/8/3R4/4K3 w - - 0 1"
  private val queenHitMove = Line(Square('d', 2), Square('h', 2))
  private val quietMove = Line(Square('d', 2), Square('a', 2))
  private val illegalMove = Line(Square('d', 2), Square('h', 8))

  test("Stage-1 QueenHitProof proves only exact legal after-board queen contact"):
    val facts = BoardFacts.fromFen(queenHitFen).toOption.get
    val proof = QueenHitProof.fromBoardFacts(facts, queenHitMove)

    assertEquals(proof.complete, true)
    assertEquals(proof.publicClaimAllowed, false)
    assertEquals(proof.attackingSide, Side.White)
    assertEquals(proof.rivalSide, Side.Black)
    assertEquals(proof.attackMove, Some(queenHitMove))
    assertEquals(proof.originSquare, Some(Square('d', 2)))
    assertEquals(proof.destinationSquare, Some(Square('h', 2)))
    assertEquals(proof.rivalQueenSquareAfter, Some(Square('h', 5)))
    assertEquals(proof.attackingPieceSquareAfter, Some(Square('h', 2)))
    assertEquals(proof.legalMove, true)
    assertEquals(proof.sameBoardProof, true)
    assertEquals(proof.exactAfterBoardReplay, true)
    assertEquals(proof.rivalQueenExistsAfter, true)
    assertEquals(proof.afterBoardQueenAttackedByMovingSide, true)
    assertEquals(proof.queenHitProducedOrRevealedByLegalMove, true)
    assertEquals(proof.missingEvidence, Vector.empty)

  test("Stage-1 QueenHitProof is complete proof or silence"):
    val facts = BoardFacts.fromFen(queenHitFen).toOption.get
    val noQueenFacts = BoardFacts.fromFen(noQueenFen).toOption.get

    val quietProof = QueenHitProof.fromBoardFacts(facts, quietMove)
    assertEquals(quietProof.complete, false)
    assertEquals(quietProof.afterBoardQueenAttackedByMovingSide, false)
    assert(quietProof.missingEvidence.exists(_.missing.contains("after-board queen attack")))

    val illegalProof = QueenHitProof.fromBoardFacts(facts, illegalMove)
    assertEquals(illegalProof.complete, false)
    assertEquals(illegalProof.legalMove, false)
    assert(illegalProof.missingEvidence.exists(_.missing.contains("legal move identity")))

    val noQueenProof = QueenHitProof.fromBoardFacts(noQueenFacts, quietMove)
    assertEquals(noQueenProof.complete, false)
    assertEquals(noQueenProof.rivalQueenExistsAfter, false)
    assert(noQueenProof.missingEvidence.exists(_.missing.contains("rival queen after move")))

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
    val untrustedProof = QueenHitProof.fromBoardFacts(untrustedFacts, queenHitMove)
    assertEquals(untrustedProof.complete, false)
    assertEquals(untrustedProof.sameBoardProof, false)
    assert(untrustedProof.missingEvidence.exists(_.missing.contains("same-board proof")))

  test("Stage-1 QueenHitProof remains below downstream speech surface"):
    val runtimeText = scalaSourceText(Paths.get("modules/commentary/src/main/scala/lila/commentary/chess"))

    Vector(
      "QueenLost",
      "ForbiddenWording.Tempo"
    ).foreach: forbidden =>
      assert(!runtimeText.contains(forbidden), s"Stage-1 must not open runtime public QueenHit surface: $forbidden")

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

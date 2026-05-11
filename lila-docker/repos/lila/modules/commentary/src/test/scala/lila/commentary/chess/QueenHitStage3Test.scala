package lila.commentary.chess

import java.nio.file.{ Files, Path, Paths }
import scala.jdk.CollectionConverters.*

class QueenHitStage3Test extends munit.FunSuite:

  private val queenHitFen = "4k3/8/8/7q/8/8/3R4/4K3 w - - 0 1"
  private val queenHitMove = Line(Square('d', 2), Square('h', 2))

  test("Stage-3 QueenHit negative corpus rejects move board and target false positives"):
    val noQueen = facts("4k3/8/8/8/8/8/3R4/4K3 w - - 0 1")
    val attackedPieceNotQueen = facts("4k3/8/8/7r/8/8/3R4/4K3 w - - 0 1")
    val ownQueenOnly = facts("4k3/8/8/8/7Q/8/3R4/4K3 w - - 0 1")
    val beforeOnly = facts("4k3/8/8/7q/8/8/7R/4K3 w - - 0 1")
    val preExistingBeforeAndAfter = facts("4k3/8/8/7q/8/8/P6R/4K3 w - - 0 1")
    val blockedAfter = facts("4k3/8/8/7q/8/8/8/4K1NR w - - 0 1")

    assertNoQueenHit(noQueen, Line(Square('d', 2), Square('h', 2)), "rival queen absent after move")
    assertNoQueenHit(attackedPieceNotQueen, queenHitMove, "attacked piece is not queen")
    assertNoQueenHit(ownQueenOnly, queenHitMove, "queen belongs to moving side")
    assertNoQueenHit(beforeOnly, Line(Square('h', 2), Square('d', 2)), "queen was attacked only on before-board")
    assertNoQueenHit(preExistingBeforeAndAfter, Line(Square('a', 2), Square('a', 3)), "queen hit existed before the move")
    assertNoQueenHit(blockedAfter, Line(Square('g', 1), Square('h', 3)), "attack line blocked on after-board")

  test("Stage-3 QueenHit requires legal after-board queen attack when pin rules matter"):
    val pinnedAttacker = facts("k3r3/8/8/8/8/8/P3R2q/4K3 w - - 0 1")
    val quietPawnMove = Line(Square('a', 2), Square('a', 3))
    val proof = QueenHitProof.fromBoardFacts(pinnedAttacker, quietPawnMove)

    assertEquals(proof.complete, false)
    assertEquals(proof.afterBoardQueenAttackedByMovingSide, false)
    assert(proof.missingEvidence.exists(_.missing.contains("legal after-board queen attack")))
    assertEquals(TacticQueenHit.write(pinnedAttacker, Some(quietPawnMove)), None)

  test("Stage-3 QueenHit blocks notation engine source proof and sidecar contamination"):
    val board = facts(queenHitFen)
    val story = TacticQueenHit.write(board, Some(queenHitMove)).get
    val notationOnly = story.copy(queenHitProof = None)
    val incompleteStoryProof = story.copy(storyProof = StoryProof.empty)
    val writerless = story.copy(writer = None)
    val contaminated = story.copy(scene = Scene.Material, tactic = None)
    val engineOnly = EngineCheck.fromEvidence(
      sameBoardProof = true,
      checkedMove = Some(queenHitMove),
      engineLine = Some(EngineLine(Vector(queenHitMove))),
      replyLine = Some(EngineLine(Vector(Line(Square('e', 8), Square('e', 7))))),
      evalBefore = Some(EngineEval(20)),
      evalAfter = Some(EngineEval(25)),
      depth = Some(18),
      freshnessPly = Some(0),
      requestedStatus = EngineCheckStatus.Supports
    )

    Vector(notationOnly, incompleteStoryProof, writerless, contaminated).foreach: falsePositive =>
      val verdict = StoryTable.choose(Vector(falsePositive)).head
      assertEquals(verdict.role, Role.Blocked)

    Vector(notationOnly, incompleteStoryProof, writerless).foreach: falsePositive =>
      val verdict = StoryTable.choose(Vector(falsePositive)).head
      assertEquals(ExplanationPlan.fromSelected(verdict), None)

    assertEquals(TacticQueenHit.withEngineCheck(notationOnly, engineOnly), None)
    assertEquals(TacticQueenHit.withEngineCheck(story.copy(routeSan = Some("queen hit")), engineOnly), None)

  test("Stage-3 QueenHit forbidden wording remains unopened in runtime QueenHit path"):
    val queenHitRuntimeText =
      scalaSourceText(Paths.get("modules/commentary/src/main/scala/lila/commentary/chess"))
        .linesIterator
        .filter(line => line.contains("QueenHit") || line.contains("queenHit"))
        .mkString("\n")

    Vector(
      "wins queen",
      "queen is lost",
      "queen trap",
      "tempo",
      "initiative",
      "pressure",
      "decisive",
      "winning",
      "best move",
      "only move",
      "forced move"
    ).foreach: forbidden =>
      assert(!queenHitRuntimeText.toLowerCase.contains(forbidden.toLowerCase), s"QueenHit path must not open: $forbidden")

  private def assertNoQueenHit(facts: BoardFacts, move: Line, label: String): Unit =
    val proof = QueenHitProof.fromBoardFacts(facts, move)
    assertEquals(proof.complete, false, label)
    assertEquals(TacticQueenHit.write(facts, Some(move)), None, label)

  private def facts(fen: String): BoardFacts =
    BoardFacts.fromFen(fen).fold(error => fail(s"invalid Stage-3 FEN: $fen -> $error"), identity)

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

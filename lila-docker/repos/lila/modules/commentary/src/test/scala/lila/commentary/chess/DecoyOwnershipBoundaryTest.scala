package lila.commentary.chess

class DecoyOwnershipBoundaryTest extends munit.FunSuite:

  private final case class MeaningOwner(
      meaning: String,
      proofHome: String,
      storyLabel: String,
      writer: Option[StoryWriter],
      speechKey: Option[String]
  )

  test("Decoy keeps one proof Story writer and speech owner"):
    val story = decoyStory
    val verdict = StoryTable.choose(Vector(story)).head
    val plan = ExplanationPlan.fromSelected(verdict).getOrElse(fail("expected Decoy plan"))
    val rendered = DeterministicRenderer.fromPlan(plan).getOrElse(fail("expected Decoy render"))

    assertEquals(decoyOwner.meaning, "decoys_piece")
    assertEquals(decoyOwner.proofHome, "DecoyProof")
    assertEquals(decoyOwner.storyLabel, "Tactic.Decoy")
    assertEquals(decoyOwner.writer, Some(StoryWriter.TacticDecoy))
    assertEquals(decoyOwner.speechKey, Some("decoys_piece"))

    assertEquals(story.scene, Scene.Tactic)
    assertEquals(story.tactic, Some(Tactic.Decoy))
    assertEquals(story.writer, Some(StoryWriter.TacticDecoy))
    assertEquals(story.decoyProof.map(_.complete), Some(true))
    assertEquals(story.trapProof, None)
    assertEquals(story.decoyProof.flatMap(_.trapFollowUpProof).map(_.complete), Some(true))
    assertEquals(plan.allowedClaim, Some(ExplanationClaim.DecoysPiece))
    assertEquals(rendered.claimKey, "decoys_piece")

  test("Decoy does not duplicate neighbor proof speech or engine ownership"):
    val neighborOwners =
      Vector(
        MeaningOwner("traps_piece", "TrapProof", "Tactic.Trap", Some(StoryWriter.TacticTrap), Some("traps_piece")),
        MeaningOwner("deflects_defender", "DeflectProof", "Tactic.Deflect", Some(StoryWriter.TacticDeflect), Some("deflects_defender")),
        MeaningOwner(
          "removes_defender",
          "RemoveGuardProof",
          "Tactic.RemoveGuard",
          Some(StoryWriter.TacticRemoveGuard),
          Some("removes_defender")
        ),
        MeaningOwner("overloads_defender", "OverloadProof", "Tactic.Overload", Some(StoryWriter.TacticOverload), Some("overloads_defender")),
        MeaningOwner("attacks_queen", "QueenHitProof", "Tactic.QueenHit", Some(StoryWriter.TacticQueenHit), Some("attacks_queen")),
        MeaningOwner("attacks_loose_piece", "LoosePieceProof", "Tactic.Loose", Some(StoryWriter.TacticLoose), Some("attacks_loose_piece")),
        MeaningOwner("can_win_piece", "CaptureResult", "Tactic.Hanging", Some(StoryWriter.TacticHanging), Some("can_win_piece")),
        MeaningOwner("material_balance_changes", "CaptureResult", "Scene.Material", Some(StoryWriter.SceneMaterial), Some("material_balance_changes")),
        MeaningOwner("pins_piece", "PinProof", "Tactic.Pin", Some(StoryWriter.TacticPin), Some("pins_piece")),
        MeaningOwner("skewers_piece_to_piece", "SkewerProof", "Tactic.Skewer", Some(StoryWriter.TacticSkewer), Some("skewers_piece_to_piece")),
        MeaningOwner(
          "reveals_attack_on_piece",
          "LineProof",
          "Tactic.DiscoveredAttack",
          Some(StoryWriter.TacticDiscoveredAttack),
          Some("reveals_attack_on_piece")
        ),
        MeaningOwner("defends_piece", "DefenseProof", "Scene.Defense", Some(StoryWriter.SceneDefense), Some("defends_piece")),
        MeaningOwner("engine_check", "EngineCheck", "none", None, None)
      )

    neighborOwners.foreach: neighbor =>
      assertNotEquals(decoyOwner.meaning, neighbor.meaning, neighbor.meaning)
      assertNotEquals(decoyOwner.proofHome, neighbor.proofHome, neighbor.meaning)
      assertNotEquals(decoyOwner.storyLabel, neighbor.storyLabel, neighbor.meaning)
      assertNotEquals(decoyOwner.writer, neighbor.writer, neighbor.meaning)
      assertNotEquals(decoyOwner.speechKey, neighbor.speechKey, neighbor.meaning)

  test("Decoy closed result force and process names stay out of runtime concepts"):
    val story = decoyStory
    val rendered =
      ExplanationPlan
        .fromSelected(StoryTable.choose(Vector(story)).head)
        .flatMap(DeterministicRenderer.fromPlan)
        .getOrElse(fail("expected Decoy rendered line"))
    val text = rendered.text.toLowerCase

    Vector(
      "wins material",
      "wins piece",
      "forced",
      "only",
      "best",
      "cannot refuse",
      "no escape",
      "no counterplay",
      "trap",
      "removes defender",
      "overload",
      "deflect"
    ).foreach: phrase =>
      assertEquals(text.contains(phrase), false, phrase)

    val runtimeNames =
      Scene.values.map(_.toString).toVector ++
        Tactic.values.map(_.toString).toVector ++
        Plan.values.map(_.toString).toVector ++
        StoryWriter.values.map(_.toString).toVector ++
        ExplanationClaim.values.map(_.key).toVector ++
        ForbiddenWording.values.map(_.key).toVector

    Vector("workflow", "stage", "audit", "gate", "checklist").foreach: word =>
      assertEquals(runtimeNames.exists(_.toLowerCase.contains(word)), false, word)
    assertEquals(runtimeNames.exists(_.toLowerCase.contains("broad_decoy")), false)

  private def decoyOwner: MeaningOwner =
    MeaningOwner(
      meaning = "decoys_piece",
      proofHome = "DecoyProof",
      storyLabel = "Tactic.Decoy",
      writer = Some(StoryWriter.TacticDecoy),
      speechKey = Some(ExplanationClaim.DecoysPiece.key)
    )

  private def decoyStory: Story =
    TacticDecoy
      .write(
        facts,
        Some(route),
        Some(reply),
        Some(namedPieceBeforeReply),
        Some(target),
        Some(completeTrapFollowUp)
      )
      .get

  private def completeTrapFollowUp: TrapProof =
    TrapProof.fromBoardFacts(afterReplyTrapFacts, trapMove)

  private def board(fen: String): BoardFacts =
    BoardFacts.fromFen(fen).fold(error => fail(s"invalid Decoy ownership FEN: $fen -> $error"), identity)

  private val facts = board("4k3/8/1nb5/8/8/8/8/R3B1K1 w - - 0 1")
  private val afterReplyTrapFacts = board("n3k3/8/2b5/8/8/8/5B2/R5K1 w - - 0 1")
  private val route = Line(Square('e', 1), Square('f', 2))
  private val reply = Line(Square('b', 6), Square('a', 8))
  private val namedPieceBeforeReply = Square('b', 6)
  private val target = Square('a', 8)
  private val trapMove = Line(Square('a', 1), Square('a', 7))

package lila.commentary.chess

class PublicMeaningOwnershipIndexTest extends munit.FunSuite:

  private final case class OpenedMeaningOwner(
      publicMeaning: String,
      proofHome: String,
      storyLabel: String,
      writer: StoryWriter,
      speechClaim: ExplanationClaim
  ):
    def speechKey: String = speechClaim.key

  private val openedMeanings =
    Vector(
      OpenedMeaningOwner("Tactic.Hanging", "CaptureResult", "Tactic.Hanging", StoryWriter.TacticHanging, ExplanationClaim.CanWinPiece),
      OpenedMeaningOwner("Tactic.Fork", "MultiTargetProof", "Tactic.Fork", StoryWriter.TacticFork, ExplanationClaim.ForksTwoTargets),
      OpenedMeaningOwner("Scene.Material", "CaptureResult", "Scene.Material", StoryWriter.SceneMaterial, ExplanationClaim.MaterialBalanceChanges),
      OpenedMeaningOwner("Scene.Defense", "DefenseProof", "Scene.Defense", StoryWriter.SceneDefense, ExplanationClaim.DefendsPiece),
      OpenedMeaningOwner("Tactic.DiscoveredAttack", "LineProof", "Tactic.DiscoveredAttack", StoryWriter.TacticDiscoveredAttack, ExplanationClaim.RevealsAttackOnPiece),
      OpenedMeaningOwner("Tactic.Pin", "PinProof", "Tactic.Pin", StoryWriter.TacticPin, ExplanationClaim.PinsPiece),
      OpenedMeaningOwner("Tactic.RemoveGuard", "RemoveGuardProof", "Tactic.RemoveGuard", StoryWriter.TacticRemoveGuard, ExplanationClaim.RemovesDefender),
      OpenedMeaningOwner("Tactic.Overload", "OverloadProof", "Tactic.Overload", StoryWriter.TacticOverload, ExplanationClaim.OverloadsDefender),
      OpenedMeaningOwner("Tactic.Skewer", "SkewerProof", "Tactic.Skewer", StoryWriter.TacticSkewer, ExplanationClaim.SkewersPieceToPiece),
      OpenedMeaningOwner("Tactic.QueenHit", "QueenHitProof", "Tactic.QueenHit", StoryWriter.TacticQueenHit, ExplanationClaim.AttacksQueen),
      OpenedMeaningOwner("Tactic.Loose", "LoosePieceProof", "Tactic.Loose", StoryWriter.TacticLoose, ExplanationClaim.AttacksLoosePiece),
      OpenedMeaningOwner("Tactic.Trap", "TrapProof", "Tactic.Trap", StoryWriter.TacticTrap, ExplanationClaim.TrapsPiece),
      OpenedMeaningOwner("Tactic.Decoy", "DecoyProof", "Tactic.Decoy", StoryWriter.TacticDecoy, ExplanationClaim.DecoysPiece),
      OpenedMeaningOwner("Scene.PawnAdvance", "PawnAdvanceProof", "Scene.PawnAdvance", StoryWriter.ScenePawnAdvance, ExplanationClaim.AdvancesPassedPawn),
      OpenedMeaningOwner("Scene.PawnStop", "PawnStopProof", "Scene.PawnStop", StoryWriter.ScenePawnStop, ExplanationClaim.StopsPassedPawnNextAdvance),
      OpenedMeaningOwner("Scene.PawnBreak", "PawnBreakProof", "Scene.PawnBreak", StoryWriter.ScenePawnBreak, ExplanationClaim.ChallengesPawnDirectly),
      OpenedMeaningOwner("Scene.PawnBlock", "PawnBlockProof", "Scene.PawnBlock", StoryWriter.ScenePawnBlock, ExplanationClaim.BlocksPawn),
      OpenedMeaningOwner("Scene.PromotionThreat", "PromotionThreatProof", "Scene.PromotionThreat", StoryWriter.ScenePromotionThreat, ExplanationClaim.CreatesPromotionThreat),
      OpenedMeaningOwner("Scene.Promotion", "PromotionProof", "Scene.Promotion", StoryWriter.ScenePromotion, ExplanationClaim.PromotesPawn),
      OpenedMeaningOwner("Scene.PawnCapture", "PawnCaptureProof", "Scene.PawnCapture", StoryWriter.ScenePawnCapture, ExplanationClaim.CapturesPawn),
      OpenedMeaningOwner("Scene.PassedPawnCreated", "PassedPawnCreatedProof", "Scene.PassedPawnCreated", StoryWriter.ScenePassedPawnCreated, ExplanationClaim.CreatesPassedPawn),
      OpenedMeaningOwner("Scene.FileOpened", "FileOpenedProof", "Scene.FileOpened", StoryWriter.SceneFileOpened, ExplanationClaim.OpensFile),
      OpenedMeaningOwner("Scene.CheckGiven", "CheckGivenProof", "Scene.CheckGiven", StoryWriter.SceneCheckGiven, ExplanationClaim.GivesCheck),
      OpenedMeaningOwner("Scene.CheckEscaped", "CheckEscapedProof", "Scene.CheckEscaped", StoryWriter.SceneCheckEscaped, ExplanationClaim.EscapesCheck),
      OpenedMeaningOwner("Scene.Checkmate", "CheckmateProof", "Scene.Checkmate", StoryWriter.SceneCheckmate, ExplanationClaim.Checkmates),
      OpenedMeaningOwner("Scene.Stalemate", "StalemateProof", "Scene.Stalemate", StoryWriter.SceneStalemate, ExplanationClaim.Stalemates)
    )

  test("Stage-3 opened public meanings have one owner tuple each"):
    assertEquals(openedMeanings.size, 26)
    openedMeanings.groupBy(_.publicMeaning).foreach: (meaning, rows) =>
      assertEquals(rows.map(_.proofHome).distinct.size, 1, s"$meaning must have one proof home")
      assertEquals(rows.map(_.storyLabel).distinct.size, 1, s"$meaning must have one Story label")
      assertEquals(rows.map(_.writer).distinct.size, 1, s"$meaning must have one writer")
      assertEquals(rows.map(_.speechKey).distinct.size, 1, s"$meaning must have one speech key")
      assertEquals(rows.head.publicMeaning, rows.head.storyLabel, s"$meaning must not have a second public name")

  test("Stage-3 speech keys writers and authority layer names remain unique"):
    assertEquals(openedMeanings.map(_.speechKey).distinct.size, openedMeanings.size)
    assertEquals(openedMeanings.map(_.writer).distinct.size, openedMeanings.size)
    assertEquals(openedMeanings.map(_.proofHome).toSet.intersect(openedMeanings.map(_.storyLabel).toSet), Set.empty[String])
    assertEquals(ExplanationClaim.values.map(_.key).distinct.size, ExplanationClaim.values.size)

  test("Stage-3 public speech keys exclude internal proof ingredients and cause-owned consequence wording"):
    val internalIngredients =
      Set(
        "BoardFacts",
        "EngineCheck",
        "ProofDeficitDiagnostic",
        "DeterministicRenderer",
        "LlmNarrationSmoke",
        "DefenderDuty",
        "DualDefenderDuty",
        "OverloadTest",
        "CannotSatisfyBoth"
      )
    val consequenceKeys =
      Set(
        "cannot_save_both",
        "deflects_defender",
        "decoys_defender",
        "wins_material",
        "wins_piece",
        "wins_queen",
        "only_defender",
        "no_defense"
      )
    val publicKeys = openedMeanings.map(_.speechKey).toSet
    assertEquals(publicKeys.intersect(internalIngredients), Set.empty[String])
    assertEquals(publicKeys.intersect(consequenceKeys), Set.empty[String])

  test("Stage-7 forged writer label and proof combinations stay blocked and silent"):
    val forgedRows =
      Vector(
        "QueenHit proof forged as Loose" ->
          queenHitStory.copy(tactic = Some(Tactic.Loose), writer = Some(StoryWriter.TacticLoose)),
        "Loose proof forged as QueenHit" ->
          looseStory.copy(tactic = Some(Tactic.QueenHit), writer = Some(StoryWriter.TacticQueenHit)),
        "QueenHit proof forged as Material" ->
          queenHitStory.copy(scene = Scene.Material, tactic = None, writer = Some(StoryWriter.SceneMaterial))
      )

    forgedRows.foreach: (label, row) =>
      val verdict = StoryTable.choose(Vector(row)).head
      assertEquals(verdict.role, Role.Blocked, label)
      assertEquals(verdict.leadAllowed, false, label)
      assertEquals(ExplanationPlan.fromSelected(verdict), None, label)
      assertEquals(ExplanationPlan.fromSelected(verdict).flatMap(DeterministicRenderer.fromPlan), None, label)

  test("Stage-7 closed PawnFork rows cannot become Lead or render"):
    val closedPawnFork = forkStory.copy(tactic = Some(Tactic.PawnFork))
    val verdicts = StoryTable.choose(Vector(closedPawnFork))

    assertEquals(verdicts, Vector.empty)
    assertEquals(verdicts.exists(_.role == Role.Lead), false)
    assertEquals(verdicts.flatMap(ExplanationPlan.fromSelected).flatMap(DeterministicRenderer.fromPlan), Vector.empty)

  test("Stage-7 non Lead rows and neighbor-owned stronger LLM wording stay silent"):
    val selected = StoryTable.choose(Vector(queenHitStory)).head
    val plan = ExplanationPlan.fromSelected(selected).get
    val rendered = DeterministicRenderer.fromPlan(plan).get

    Vector(
      "unselected" -> selected.copy(selected = false),
      "support" -> selected.copy(role = Role.Support, leadAllowed = false),
      "context" -> selected.copy(role = Role.Context, leadAllowed = false),
      "blocked" -> selected.copy(role = Role.Blocked, leadAllowed = false)
    ).foreach: (label, row) =>
      assertEquals(ExplanationPlan.fromSelected(row), None, label)
      assertEquals(ExplanationPlan.fromSelected(row).flatMap(DeterministicRenderer.fromPlan), None, label)

    Vector(
      "Rh2 wins the queen.",
      "Rh2 wins material.",
      "Rh2 attacks a hanging piece."
    ).foreach: stronger =>
      assertEquals(LlmNarrationSmoke.check(plan, rendered, stronger).accepted, false, stronger)

  private def queenHitStory: Story =
    TacticQueenHit.write(board("4k3/8/8/7q/8/8/3R4/4K3 w - - 0 1"), Some(Line(Square('d', 2), Square('h', 2)))).get
      .copy(proof = strongProof)

  private def looseStory: Story =
    TacticLoose.write(board("4k3/8/8/7b/8/8/3R4/4K3 w - - 0 1"), Some(Line(Square('d', 2), Square('h', 2)))).get
      .copy(proof = strongProof)

  private def forkStory: Story =
    TacticFork
      .write(
        board("7k/8/3n1n2/8/4P3/8/8/7K w - - 0 1"),
        Some(Line(Square('e', 4), Square('e', 5))),
        Some(Square('d', 6)),
        Some(Square('f', 6))
      )
      .get
      .copy(proof = strongProof)

  private def board(fen: String): BoardFacts =
    BoardFacts.fromFen(fen).fold(error => fail(s"invalid Stage-7 FEN: $fen -> $error"), identity)

  private val strongProof: Proof =
    Proof(
      boardProof = 99,
      lineProof = 99,
      ownerProof = 99,
      anchorProof = 99,
      routeProof = 99,
      persistence = 99,
      immediacy = 99,
      forcing = 99,
      conversionPrize = 0,
      counterplayRisk = 0,
      kingHeat = 0,
      pieceSupport = 99,
      pawnSupport = 0,
      sourceFit = 0,
      novelty = 0,
      clarity = 99
    )

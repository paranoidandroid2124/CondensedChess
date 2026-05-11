package lila.commentary.chess

import java.nio.file.{ Files, Paths }

class AttackTargetInteractionHardeningTest extends munit.FunSuite:

  private val queenHitFen = "4k3/8/8/7q/8/8/3R4/4K3 w - - 0 1"
  private val queenHitMove = Line(Square('d', 2), Square('h', 2))
  private val queenHitOnlyFen = "4k3/8/6b1/7q/8/8/3R4/4K3 w - - 0 1"
  private val looseFen = "4k3/8/8/7b/8/8/3R4/4K3 w - - 0 1"
  private val looseMove = Line(Square('d', 2), Square('h', 2))
  private val materialFen = "4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1"
  private val materialMove = Line(Square('d', 4), Square('e', 5))

  test("ATIH Stage-0 blocks mixed QueenHit and Loose sidecar ownership"):
    val queenHitFacts = board(queenHitFen)
    val queenHit = TacticQueenHit.write(queenHitFacts, Some(queenHitMove)).get.copy(proof = orderingProof)
    val looseProof = TacticLoose.write(queenHitFacts, Some(queenHitMove)).get.loosePieceProof.get
    val contaminated = queenHit.copy(loosePieceProof = Some(looseProof))
    val verdict = StoryTable.choose(Vector(contaminated)).head

    assertEquals(verdict.role, Role.Blocked)
    assertEquals(verdict.leadAllowed, false)
    assertEquals(ExplanationPlan.fromSelected(verdict), None)
    assertEquals(ExplanationPlan.fromSelected(verdict).flatMap(DeterministicRenderer.fromPlan), None)

  test("ATIH Stage-0 keeps attack-only rows out of Hanging and Material speech"):
    val queenHit = TacticQueenHit.write(board(queenHitFen), Some(queenHitMove)).get.copy(proof = orderingProof)
    val loose = TacticLoose.write(board(looseFen), Some(looseMove)).get.copy(proof = orderingProof)
    val attackOnlyRows =
      Vector(
        "QueenHit as Hanging" -> queenHit.copy(tactic = Some(Tactic.Hanging)),
        "QueenHit as Material" -> queenHit.copy(scene = Scene.Material, tactic = None, writer = Some(StoryWriter.SceneMaterial)),
        "Loose as Hanging" -> loose.copy(tactic = Some(Tactic.Hanging)),
        "Loose as Material" -> loose.copy(scene = Scene.Material, tactic = None, writer = Some(StoryWriter.SceneMaterial))
      )

    attackOnlyRows.foreach: (label, row) =>
      val verdict = StoryTable.choose(Vector(row)).head
      assertEquals(verdict.role, Role.Blocked, label)
      assertEquals(verdict.leadAllowed, false, label)
      assertEquals(ExplanationPlan.fromSelected(verdict).flatMap(DeterministicRenderer.fromPlan), None, label)

  test("ATIH Stage-0 preserves existing Material and Hanging homes for actual capture proof"):
    val facts = board(materialFen)
    val material = SceneMaterial.write(facts, materialMove).get.copy(proof = orderingProof)
    val hanging = TacticHanging.write(facts, materialMove).get.copy(proof = orderingProof)

    Vector(material, hanging).foreach: row =>
      val verdict = StoryTable.choose(Vector(row)).head
      assertEquals(verdict.role, Role.Lead)
      assertEquals(verdict.leadAllowed, true)

  test("ATIH Stage-0 detailed authority lives only in StoryInteractionLaw"):
    val law = Files.readString(Paths.get("modules/commentary/docs/StoryInteractionLaw.md"))
    Vector(
      "## ATIH Stage-0 Charter",
      "Attack-Target Interaction Hardening",
      "ATIH-0 opens no new chess meaning.",
      "`QueenHitProof` -> `Tactic.QueenHit` -> `TacticQueenHit` -> `attacks_queen`",
      "`LoosePieceProof` -> `Tactic.Loose` -> `TacticLoose` -> `attacks_loose_piece`",
      "existing Hanging proof path -> `Tactic.Hanging` -> `TacticHanging` -> `can_win_piece`",
      "existing material proof path -> `Scene.Material` -> `SceneMaterial` -> `material_balance_changes`",
      "material gain from attack-only rows",
      "public route `200`",
      "public/user-facing LLM narration"
    ).foreach: marker =>
      assert(law.contains(marker), s"ATIH law must pin: $marker")

    val agents = Files.readString(Paths.get("../../../AGENTS.md"))
    assert(!agents.contains("Attack-Target Interaction Hardening"))

  test("ATIH Stage-1 authority inventory keeps proof label writer and speech key separated"):
    val queenHitProof = QueenHitProof.fromBoardFacts(board(queenHitFen), queenHitMove)
    val queenHit = queenHitStory
    val queenHitPlan = ExplanationPlan.fromSelected(StoryTable.choose(Vector(queenHit)).head).get
    val looseProof = LoosePieceProof.fromBoardFacts(board(looseFen), looseMove)
    val loose = looseStory
    val loosePlan = ExplanationPlan.fromSelected(StoryTable.choose(Vector(loose)).head).get

    assertEquals(queenHitProof.complete, true)
    assertEquals(queenHitProof.publicClaimAllowed, false)
    assertEquals(queenHit.tactic, Some(Tactic.QueenHit))
    assertEquals(queenHit.writer, Some(StoryWriter.TacticQueenHit))
    assertEquals(queenHitPlan.allowedClaim.map(_.key), Some("attacks_queen"))

    assertEquals(looseProof.complete, true)
    assertEquals(looseProof.publicClaimAllowed, false)
    assertEquals(loose.tactic, Some(Tactic.Loose))
    assertEquals(loose.writer, Some(StoryWriter.TacticLoose))
    assertEquals(loosePlan.allowedClaim.map(_.key), Some("attacks_loose_piece"))

    Vector(
      "QueenHit label only" -> queenHit.copy(writer = None, queenHitProof = None),
      "QueenHit writer only" -> queenHit.copy(queenHitProof = None),
      "Loose label only" -> loose.copy(writer = None, loosePieceProof = None),
      "Loose writer only" -> loose.copy(loosePieceProof = None)
    ).foreach: (label, row) =>
      val verdict = StoryTable.choose(Vector(row)).head
      assertEquals(verdict.role, Role.Blocked, label)
      assertEquals(ExplanationPlan.fromSelected(verdict), None, label)

  test("ATIH Stage-1 Hanging and Material remain separate existing meanings"):
    val facts = board(materialFen)
    val hanging = TacticHanging.write(facts, materialMove).get.copy(proof = orderingProof)
    val material = SceneMaterial.write(facts, materialMove).get.copy(proof = orderingProof)
    val hangingPlan = ExplanationPlan.fromSelected(StoryTable.choose(Vector(hanging)).head).get
    val materialPlan = ExplanationPlan.fromSelected(StoryTable.choose(Vector(material)).head).get

    assertEquals(hanging.scene, Scene.Tactic)
    assertEquals(hanging.tactic, Some(Tactic.Hanging))
    assertEquals(hanging.writer, Some(StoryWriter.TacticHanging))
    assertEquals(hangingPlan.allowedClaim.map(_.key), Some("can_win_piece"))

    assertEquals(material.scene, Scene.Material)
    assertEquals(material.tactic, None)
    assertEquals(material.writer, Some(StoryWriter.SceneMaterial))
    assertEquals(materialPlan.allowedClaim.map(_.key), Some("material_balance_changes"))

    assert(hanging.scene != material.scene)
    assert(hanging.tactic != material.tactic)
    assert(hanging.writer != material.writer)
    assert(hangingPlan.allowedClaim.map(_.key) != materialPlan.allowedClaim.map(_.key))

  test("ATIH Stage-1 BoardFacts EngineCheck StoryTable and downstream stages do not create attack target Stories"):
    val facts = board(queenHitFen)
    val engineOnly = EngineCheck.fromStory(
      facts = facts,
      story = None,
      engineLine = Some(EngineLine(Vector(queenHitMove))),
      replyLine = Some(EngineLine(Vector(Line(Square('e', 8), Square('f', 8))))),
      evalBefore = Some(EngineEval(20)),
      evalAfter = Some(EngineEval(20)),
      depth = Some(18),
      freshnessPly = Some(0),
      requestedStatus = EngineCheckStatus.Supports
    )
    val forgedQueenHit = forgedAttackStory(Tactic.QueenHit).copy(engineCheck = Some(engineOnly))
    val forgedLoose = forgedAttackStory(Tactic.Loose).copy(engineCheck = Some(engineOnly))

    assertEquals(StoryTable.choose(Vector.empty), Vector.empty)
    assertEquals(engineOnly.publicClaimAllowed, false)
    assertEquals(engineOnly.storyBound, false)
    assertEquals(engineOnly.status, EngineCheckStatus.Unknown)

    Vector("QueenHit" -> forgedQueenHit, "Loose" -> forgedLoose).foreach: (label, row) =>
      val verdict = StoryTable.choose(Vector(row)).head
      assertEquals(verdict.role, Role.Blocked, label)
      assertEquals(verdict.leadAllowed, false, label)
      assertEquals(ExplanationPlan.fromSelected(verdict), None, label)
      assertEquals(ExplanationPlan.fromSelected(verdict).flatMap(DeterministicRenderer.fromPlan), None, label)

    val queenHit = queenHitStory
    val plan = ExplanationPlan.fromSelected(StoryTable.choose(Vector(queenHit)).head).get
    val malformedPlan = plan.copy(allowedClaim = Some(ExplanationClaim.MaterialBalanceChanges))
    val rendered = DeterministicRenderer.fromPlan(plan).get

    assertEquals(DeterministicRenderer.fromPlan(malformedPlan), None)
    assertEquals(LlmNarrationSmoke.check(plan, rendered, "Rh2 wins the queen.").accepted, false)
    assertEquals(LlmNarrationSmoke.check(plan, rendered, "Rh2 wins material.").accepted, false)

  test("ATIH Stage-1 authority inventory lives only in StoryInteractionLaw"):
    val law = Files.readString(Paths.get("modules/commentary/docs/StoryInteractionLaw.md"))
    Vector(
      "## ATIH Stage-1 Authority Inventory",
      "ATIH-1 opens authority inventory tests only.",
      "QueenHitProof is not Tactic.QueenHit.",
      "Tactic.QueenHit is not TacticQueenHit.",
      "TacticQueenHit is not attacks_queen.",
      "LoosePieceProof is not Tactic.Loose.",
      "Tactic.Loose is not TacticLoose.",
      "TacticLoose is not attacks_loose_piece.",
      "Tactic.Hanging is not Scene.Material.",
      "Scene.Material is not Tactic.Hanging.",
      "BoardFacts cannot create any of these Stories.",
      "EngineCheck cannot create any of these Stories.",
      "StoryTable cannot create any of these Stories.",
      "ExplanationPlan, Renderer, and LLM smoke cannot create, repair, rank, or strengthen these meanings."
    ).foreach: marker =>
      assert(law.contains(marker), s"ATIH-1 law must pin: $marker")

  test("ATIH Stage-2 collision corpus covers QueenHit Loose Hanging and Material fixtures"):
    val queenHitOnlyFacts = board(queenHitOnlyFen)
    val queenHitOnly = TacticQueenHit.write(queenHitOnlyFacts, Some(queenHitMove)).get.copy(proof = orderingProof)
    val looseOnlyFacts = board(looseFen)
    val looseOnly = TacticLoose.write(looseOnlyFacts, Some(looseMove)).get.copy(proof = orderingProof)
    val queenOverlapFacts = board(queenHitFen)
    val queenHitOverlap = TacticQueenHit.write(queenOverlapFacts, Some(queenHitMove)).get.copy(proof = orderingProof)
    val looseOverlap = TacticLoose.write(queenOverlapFacts, Some(queenHitMove)).get.copy(proof = orderingProof)
    val materialFacts = board(materialFen)
    val hangingOnly = TacticHanging.write(materialFacts, materialMove).get.copy(proof = orderingProof)
    val materialOnly = SceneMaterial.write(materialFacts, materialMove).get.copy(proof = orderingProof)

    assertEquals(queenHitOnly.tactic, Some(Tactic.QueenHit))
    assertEquals(looseOnly.tactic, Some(Tactic.Loose))
    assertEquals(TacticLoose.write(queenHitOnlyFacts, Some(queenHitMove)), None, "QueenHit-only must not become Loose")
    assertEquals(SceneMaterial.write(queenHitOnlyFacts, queenHitMove), None, "QueenHit-only has no material result")
    assertEquals(TacticQueenHit.write(looseOnlyFacts, Some(looseMove)), None, "Loose-only must not become QueenHit")
    assertEquals(SceneMaterial.write(looseOnlyFacts, looseMove), None, "Loose-only has no material result")

    val overlapVerdicts = StoryTable.choose(Vector(looseOverlap, queenHitOverlap))
    val queenHitVerdict = overlapVerdicts.find(_.story.tactic.contains(Tactic.QueenHit)).get
    val looseVerdict = overlapVerdicts.find(_.story.tactic.contains(Tactic.Loose)).get
    assertEquals(queenHitVerdict.role, Role.Lead)
    assertEquals(looseVerdict.role == Role.Lead, false)
    assertEquals(ExplanationPlan.fromSelected(queenHitVerdict).flatMap(DeterministicRenderer.fromPlan).map(_.claimKey), Some("attacks_queen"))
    assertEquals(ExplanationPlan.fromSelected(looseVerdict).flatMap(DeterministicRenderer.fromPlan), None)

    assertEquals(TacticQueenHit.write(materialFacts, Some(materialMove)), None, "Hanging-only must not steal QueenHit")
    assertEquals(TacticLoose.write(materialFacts, Some(materialMove)), None, "Hanging-only must not steal Loose")
    assertEquals(ExplanationPlan.fromSelected(StoryTable.choose(Vector(hangingOnly)).head).flatMap(DeterministicRenderer.fromPlan).map(_.claimKey), Some("can_win_piece"))

    assertEquals(TacticQueenHit.write(materialFacts, Some(materialMove)), None, "Material-only does not require attack-only QueenHit")
    assertEquals(TacticLoose.write(materialFacts, Some(materialMove)), None, "Material-only does not require attack-only Loose")
    assertEquals(ExplanationPlan.fromSelected(StoryTable.choose(Vector(materialOnly)).head).flatMap(DeterministicRenderer.fromPlan).map(_.claimKey), Some("material_balance_changes"))

  test("ATIH Stage-2 collision corpus keeps attack-only rows out of Material and Hanging"):
    val queenHitOnlyFacts = board(queenHitOnlyFen)
    val looseOnlyFacts = board(looseFen)
    val materialFacts = board(materialFen)

    assertEquals(SceneMaterial.write(queenHitOnlyFacts, queenHitMove), None, "Attack-only QueenHit must not produce Scene.Material")
    assertEquals(SceneMaterial.write(looseOnlyFacts, looseMove), None, "Attack-only Loose must not produce Scene.Material")
    assertEquals(TacticLoose.write(materialFacts, Some(materialMove)), None, "Material capture must not require Loose")
    assertEquals(LoosePieceProof.fromBoardFacts(materialFacts, materialMove).complete, false, "Material not Loose")
    assertEquals(TacticHanging.write(looseOnlyFacts, looseMove), None, "Loose not Hanging without capture proof")

  test("ATIH Stage-2 QueenHit does not become wins queen traps queen or queen lost"):
    val queenHit = TacticQueenHit.write(board(queenHitFen), Some(queenHitMove)).get.copy(proof = orderingProof)
    val verdict = StoryTable.choose(Vector(queenHit)).head
    val plan = ExplanationPlan.fromSelected(verdict).get
    val rendered = DeterministicRenderer.fromPlan(plan).get
    val publicText =
      Vector(
        rendered.text,
        LlmNarrationSmoke.mockNarrate(plan, rendered).getOrElse("")
      ).mkString("\n").toLowerCase(java.util.Locale.ROOT)

    assertEquals(rendered.claimKey, "attacks_queen")
    Vector("wins queen", "wins the queen", "traps queen", "traps the queen", "queen is lost", "lost queen").foreach: phrase =>
      assert(!publicText.contains(phrase), s"QueenHit collision text must not say: $phrase")

    Vector(
      "Rh2 wins the queen.",
      "Rh2 traps the queen.",
      "The queen is lost after Rh2."
    ).foreach: output =>
      assertEquals(LlmNarrationSmoke.check(plan, rendered, output).accepted, false, output)

  test("ATIH Stage-2 capped and refuted rows stay bounded for each chain"):
    val queenHit = TacticQueenHit.write(board(queenHitFen), Some(queenHitMove)).get.copy(proof = orderingProof)
    val loose = TacticLoose.write(board(looseFen), Some(looseMove)).get.copy(proof = orderingProof)
    val materialFacts = board(materialFen)
    val hanging = TacticHanging.write(materialFacts, materialMove).get.copy(proof = orderingProof)
    val material = SceneMaterial.write(materialFacts, materialMove).get.copy(proof = orderingProof)

    val rows =
      Vector(
        "QueenHit" -> (queenHit, board(queenHitFen), attachQueenHit),
        "Loose" -> (loose, board(looseFen), attachLoose),
        "Hanging" -> (hanging, materialFacts, attachHanging),
        "Material" -> (material, materialFacts, attachMaterial)
      )

    rows.foreach: (label, tuple) =>
      val (story, facts, attach) = tuple
      val capped = attach(story, engineCheck(facts, story, EngineCheckStatus.Caps))
      val refuted = attach(story, engineCheck(facts, story, EngineCheckStatus.Supports, before = 220, after = 20))
      val cappedVerdict = StoryTable.choose(Vector(capped)).head
      val refutedVerdict = StoryTable.choose(Vector(refuted)).head

      assertEquals(cappedVerdict.engineStrengthLimited, true, label)
      assertEquals(ExplanationPlan.fromSelected(cappedVerdict).flatMap(DeterministicRenderer.fromPlan), None, label)
      assertEquals(refutedVerdict.role, Role.Blocked, label)
      assertEquals(ExplanationPlan.fromSelected(refutedVerdict).flatMap(DeterministicRenderer.fromPlan), None, label)

  test("ATIH Stage-2 collision fixture authority lives only in StoryInteractionLaw"):
    val law = Files.readString(Paths.get("modules/commentary/docs/StoryInteractionLaw.md"))
    Vector(
      "## ATIH Stage-2 Collision Fixtures",
      "ATIH-2 opens collision fixture corpus only.",
      "QueenHit-only: legal move attacks rival queen, no material change.",
      "Loose-only: legal move attacks one undefended rival non-king non-queen piece, no material change.",
      "QueenHit plus Loose-looking overlap: rival queen is undefended and attacked.",
      "Hanging-only: existing hanging proof complete, no QueenHit/Loose speech steal.",
      "Material-only: actual material balance changes now, no attack-only claim required.",
      "Attack-only not Material: queen or loose piece attacked but no capture/material result.",
      "Material not Loose: capture/material change where loose-piece proof is incomplete.",
      "Loose not Hanging: target is attacked and undefended, but no bounded hanging capture proof.",
      "QueenHit not wins-queen: queen attacked but not proven lost/trapped.",
      "Capped/refuted rows for each chain.",
      "Preferred: QueenHit Lead, Loose silent or Support with no standalone text.",
      "Attack-only rows cannot produce Scene.Material.",
      "Actual material result remains Scene.Material home."
    ).foreach: marker =>
      assert(law.contains(marker), s"ATIH-2 law must pin: $marker")

  test("ATIH Stage-3 StoryTable ordering is stable across attack-target input order"):
    val queenFacts = board(queenHitFen)
    val materialFacts = board(materialFen)
    val queenHit = TacticQueenHit.write(queenFacts, Some(queenHitMove)).get.copy(proof = orderingProof)
    val loose = TacticLoose.write(queenFacts, Some(queenHitMove)).get.copy(proof = orderingProof)
    val hanging = TacticHanging.write(materialFacts, materialMove).get.copy(proof = orderingProof)
    val material = SceneMaterial.write(materialFacts, materialMove).get.copy(proof = orderingProof)
    val orders = Vector(queenHit, loose, hanging, material).permutations.toVector
    val expected = StoryTable.choose(orders.head).map(verdictSignature)

    orders.foreach: rows =>
      val verdicts = StoryTable.choose(rows)
      assertEquals(verdicts.map(verdictSignature), expected)
      assertEquals(verdicts.count(_.role == Role.Lead), 1)

  test("ATIH Stage-3 same-route collisions keep one deterministic Lead and silent siblings"):
    val queenFacts = board(queenHitFen)
    val queenHit = TacticQueenHit.write(queenFacts, Some(queenHitMove)).get.copy(proof = orderingProof)
    val loose = TacticLoose.write(queenFacts, Some(queenHitMove)).get.copy(proof = orderingProof)
    Vector(Vector(queenHit, loose), Vector(loose, queenHit)).foreach: rows =>
      val verdicts = StoryTable.choose(rows)
      val queenHitVerdict = verdicts.find(_.story.tactic.contains(Tactic.QueenHit)).get
      val looseVerdict = verdicts.find(_.story.tactic.contains(Tactic.Loose)).get
      assertEquals(verdicts.count(_.role == Role.Lead), 1)
      assertEquals(queenHitVerdict.role, Role.Lead)
      assertEquals(standaloneClaimKey(queenHitVerdict), Some("attacks_queen"))
      assertEquals(looseVerdict.role == Role.Lead, false)
      assertNoStandaloneText(looseVerdict, "QueenHit plus Loose-looking overlap keeps Loose silent")

    val materialFacts = board(materialFen)
    val hanging = TacticHanging.write(materialFacts, materialMove).get.copy(proof = orderingProof)
    val material = SceneMaterial.write(materialFacts, materialMove).get.copy(proof = orderingProof)
    val referenceLead = StoryTable.choose(Vector(hanging, material)).find(_.role == Role.Lead).map(verdictSignature)
    Vector(Vector(hanging, material), Vector(material, hanging)).foreach: rows =>
      val verdicts = StoryTable.choose(rows)
      assertEquals(verdicts.count(_.role == Role.Lead), 1)
      assertEquals(verdicts.find(_.role == Role.Lead).map(verdictSignature), referenceLead)
      verdicts.filter(_.role != Role.Lead).foreach(assertNoStandaloneText(_, "Material/Hanging same-route sibling"))

  test("ATIH Stage-3 Support Context Blocked capped and refuted rows have no standalone text"):
    val queenFacts = board(queenHitFen)
    val queenHit = TacticQueenHit.write(queenFacts, Some(queenHitMove)).get.copy(proof = orderingProof)
    val loose = TacticLoose.write(queenFacts, Some(queenHitMove)).get.copy(proof = orderingProof)
    val support = StoryTable.choose(Vector(queenHit, loose)).find(_.story.tactic.contains(Tactic.Loose)).get
    val context = StoryTable.choose(Vector(queenHit.copy(proof = lowProof))).head
    val blocked = StoryTable.choose(Vector(queenHit.copy(storyProof = StoryProof.empty))).head
    val capped = StoryTable.choose(Vector(attachQueenHit(queenHit, engineCheck(queenFacts, queenHit, EngineCheckStatus.Caps)))).head
    val refuted = StoryTable.choose(Vector(attachQueenHit(queenHit, engineCheck(queenFacts, queenHit, EngineCheckStatus.Supports, before = 220, after = 20)))).head

    assertEquals(support.role, Role.Support)
    assertEquals(context.role, Role.Context)
    assertEquals(blocked.role, Role.Blocked)
    assertEquals(capped.engineStrengthLimited, true)
    assertEquals(refuted.role, Role.Blocked)

    Vector(
      "Support" -> support,
      "Context" -> context,
      "Blocked" -> blocked,
      "Capped" -> capped,
      "Refuted" -> refuted
    ).foreach: (label, verdict) =>
      assertNoStandaloneText(verdict, label)

  test("ATIH Stage-3 sibling sidecars raw sources proof failures and EngineCheck do not create Leads"):
    val queenFacts = board(queenHitFen)
    val queenHit = TacticQueenHit.write(queenFacts, Some(queenHitMove)).get.copy(proof = orderingProof)
    val loose = TacticLoose.write(queenFacts, Some(queenHitMove)).get.copy(proof = orderingProof)
    val forgedEngineCheck = engineCheck(queenFacts, queenHit, EngineCheckStatus.Supports)
    val rows =
      Vector(
        "QueenHitProof cannot lend to Loose" -> queenHit.copy(tactic = Some(Tactic.Loose), writer = Some(StoryWriter.TacticLoose)),
        "LoosePieceProof cannot lend to QueenHit" -> loose.copy(tactic = Some(Tactic.QueenHit), writer = Some(StoryWriter.TacticQueenHit)),
        "QueenHit cannot upgrade to Hanging" -> queenHit.copy(tactic = Some(Tactic.Hanging), writer = Some(StoryWriter.TacticHanging)),
        "Loose cannot upgrade to Hanging" -> loose.copy(tactic = Some(Tactic.Hanging), writer = Some(StoryWriter.TacticHanging)),
        "QueenHit cannot upgrade to Material" -> queenHit.copy(scene = Scene.Material, tactic = None, writer = Some(StoryWriter.SceneMaterial)),
        "Loose cannot upgrade to Material" -> loose.copy(scene = Scene.Material, tactic = None, writer = Some(StoryWriter.SceneMaterial)),
        "Raw Source row cannot lead" -> rawSourceStory,
        "ProofFailures cannot lead" -> queenHit.copy(storyProof = StoryProof.empty),
        "EngineCheck cannot lead forged QueenHit" -> forgedAttackStory(Tactic.QueenHit).copy(engineCheck = Some(forgedEngineCheck))
      )

    rows.foreach: (label, row) =>
      val verdict = StoryTable.choose(Vector(row)).head
      assertEquals(verdict.role == Role.Lead, false, label)
      assertNoStandaloneText(verdict, label)

  test("ATIH Stage-3 StoryTable stability authority lives only in StoryInteractionLaw"):
    val law = Files.readString(Paths.get("modules/commentary/docs/StoryInteractionLaw.md"))
    Vector(
      "## ATIH Stage-3 StoryTable Stability",
      "ATIH-3 opens StoryTable ordering hardening only.",
      "input order stability across QueenHit, Loose, Hanging, and Material rows",
      "at most one Lead for the same route when meanings collide",
      "selected Lead is deterministic",
      "Support, Context, and Blocked rows have no standalone text.",
      "capped and refuted rows have no standalone text.",
      "sibling rows do not lend proof sidecars to each other.",
      "StoryTable must not upgrade QueenHit to wins queen.",
      "StoryTable must not upgrade Loose to Hanging.",
      "StoryTable must not upgrade attack-only rows to Material.",
      "StoryTable must not use raw source rows, proofFailures, or EngineCheck to create a Lead."
    ).foreach: marker =>
      assert(law.contains(marker), s"ATIH-3 law must pin: $marker")

  test("ATIH Stage-4 EngineCheck statuses attach only to existing proof-backed Stories"):
    engineRows.foreach: row =>
      val (label, facts, story, attach, claimKey) = row
      val baseVerdict = StoryTable.choose(Vector(story)).head
      val supports = engineCheck(facts, story, EngineCheckStatus.Supports)
      val caps = engineCheck(facts, story, EngineCheckStatus.Caps)
      val refutes = engineCheck(facts, story, EngineCheckStatus.Supports, before = 220, after = 20)

      assertEquals(baseVerdict.role, Role.Lead, label)
      assertEquals(standaloneClaimKey(baseVerdict), Some(claimKey), label)
      assertEquals(supports.storyBound, true, label)
      assertEquals(supports.publicClaimAllowed, false, label)
      assertEquals(caps.storyBound, true, label)
      assertEquals(refutes.storyBound, true, label)
      assertEquals(refutes.status, EngineCheckStatus.Refutes, label)

      val supportedVerdict = StoryTable.choose(Vector(attach(story, supports))).head
      val cappedVerdict = StoryTable.choose(Vector(attach(story, caps))).head
      val refutedVerdict = StoryTable.choose(Vector(attach(story, refutes))).head

      assertEquals(supportedVerdict.role, Role.Lead, label)
      assertEquals(supportedVerdict.engineCheckStatus, Some(EngineCheckStatus.Supports), label)
      assertEquals(standaloneClaimKey(supportedVerdict), Some(claimKey), label)
      assertEquals(cappedVerdict.engineCheckStatus, Some(EngineCheckStatus.Caps), label)
      assertEquals(cappedVerdict.engineStrengthLimited, true, label)
      assertNoStandaloneText(cappedVerdict, s"$label Caps")
      assertEquals(refutedVerdict.role, Role.Blocked, label)
      assertEquals(refutedVerdict.engineCheckStatus, Some(EngineCheckStatus.Refutes), label)
      assertNoStandaloneText(refutedVerdict, s"$label Refutes")

  test("ATIH Stage-4 EngineCheck cannot create QueenHit Loose Hanging or Material"):
    val engineOnly = EngineCheck.fromEvidence(
      sameBoardProof = true,
      checkedMove = Some(queenHitMove),
      engineLine = Some(EngineLine(Vector(queenHitMove))),
      replyLine = Some(EngineLine(Vector(Line(Square('e', 8), Square('f', 8))))),
      evalBefore = Some(EngineEval(20)),
      evalAfter = Some(EngineEval(20)),
      depth = Some(18),
      freshnessPly = Some(0),
      requestedStatus = EngineCheckStatus.Supports
    )
    val forgedMaterial = rawSourceStory.copy(scene = Scene.Material, tactic = None, writer = Some(StoryWriter.SceneMaterial), engineCheck = Some(engineOnly))
    val rows =
      Vector(
        "QueenHit" -> forgedAttackStory(Tactic.QueenHit).copy(writer = Some(StoryWriter.TacticQueenHit), engineCheck = Some(engineOnly)),
        "Loose" -> forgedAttackStory(Tactic.Loose).copy(writer = Some(StoryWriter.TacticLoose), engineCheck = Some(engineOnly)),
        "Hanging" -> forgedAttackStory(Tactic.Hanging).copy(writer = Some(StoryWriter.TacticHanging), engineCheck = Some(engineOnly)),
        "Material" -> forgedMaterial
      )

    assertEquals(engineOnly.storyBound, false)
    assertEquals(engineOnly.publicClaimAllowed, false)
    rows.foreach: (label, row) =>
      val verdict = StoryTable.choose(Vector(row)).head
      assertEquals(verdict.role == Role.Lead, false, label)
      assertNoStandaloneText(verdict, label)

  test("ATIH Stage-4 EngineCheck does not broaden attack-only claims or create engine wording"):
    val queenFacts = board(queenHitFen)
    val looseFacts = board(looseFen)
    val queenHit = TacticQueenHit.write(queenFacts, Some(queenHitMove)).get.copy(proof = orderingProof)
    val loose = TacticLoose.write(looseFacts, Some(looseMove)).get.copy(proof = orderingProof)
    val supportedQueenHit = attachQueenHit(queenHit, engineCheck(queenFacts, queenHit, EngineCheckStatus.Supports))
    val supportedLoose = attachLoose(loose, engineCheck(looseFacts, loose, EngineCheckStatus.Supports))

    Vector("QueenHit" -> supportedQueenHit, "Loose" -> supportedLoose).foreach: (label, story) =>
      val verdict = StoryTable.choose(Vector(story)).head
      assertEquals(verdict.role, Role.Lead, label)
      assertTextExcludes(publicText(verdict), attackOnlyEngineForbiddenPublicText, label)

    val unknown = EngineCheck.fromStory(
      facts = queenFacts,
      story = Some(queenHit),
      engineLine = None,
      replyLine = Some(EngineLine(Vector(Line(Square('e', 8), Square('f', 8))))),
      evalBefore = Some(EngineEval(20)),
      evalAfter = Some(EngineEval(20)),
      depth = Some(18),
      freshnessPly = Some(0),
      requestedStatus = EngineCheckStatus.Unknown
    )
    val unknownVerdict = StoryTable.choose(Vector(queenHit.copy(engineCheck = Some(unknown)))).head
    assertEquals(unknown.status, EngineCheckStatus.Unknown)
    assertEquals(unknown.publicClaimAllowed, false)
    assertEquals(TacticQueenHit.withEngineCheck(queenHit, unknown), None)
    assertEquals(unknownVerdict.engineCheckStatus, Some(EngineCheckStatus.Unknown))
    assertEquals(standaloneClaimKey(unknownVerdict), Some("attacks_queen"))
    assertTextExcludes(publicText(unknownVerdict), engineForbiddenPublicText, "Unknown")

  test("ATIH Stage-4 EngineCheck boundary authority lives only in StoryInteractionLaw"):
    val law = Files.readString(Paths.get("modules/commentary/docs/StoryInteractionLaw.md"))
    Vector(
      "## ATIH Stage-4 EngineCheck Boundary",
      "ATIH-4 opens EngineCheck interaction tests only.",
      "EngineCheck supports, caps, and refutes only already existing proof-backed Stories.",
      "EngineCheck cannot create QueenHit.",
      "EngineCheck cannot create Loose.",
      "EngineCheck cannot create Hanging.",
      "EngineCheck cannot create Material.",
      "Supports creates no new claim.",
      "Caps suppresses or bounds already selected speech.",
      "Refutes blocks the affected Story.",
      "Unknown creates no engine expression.",
      "EngineCheck may cap or refute a claim; it never broadens attack-only meaning into material or tactical certainty."
    ).foreach: marker =>
      assert(law.contains(marker), s"ATIH-4 law must pin: $marker")

  test("ATIH Stage-5 ExplanationPlan lowers only selected uncapped Lead public claims"):
    engineRows.foreach: row =>
      val (label, facts, story, attach, claimKey) = row
      val selected = StoryTable.choose(Vector(story)).head
      val support = selected.copy(role = Role.Support, leadAllowed = false)
      val context = selected.copy(role = Role.Context, leadAllowed = false, rank = 3)
      val unselected = selected.copy(selected = false)
      val capped = StoryTable.choose(Vector(attach(story, engineCheck(facts, story, EngineCheckStatus.Caps)))).head
      val refuted = StoryTable.choose(Vector(attach(story, engineCheck(facts, story, EngineCheckStatus.Supports, before = 220, after = 20)))).head
      val blocked = StoryTable.choose(Vector(story.copy(storyProof = StoryProof.empty))).head

      val plan = ExplanationPlan.fromSelected(selected).get
      assertEquals(selected.role, Role.Lead, label)
      assertEquals(selected.leadAllowed, true, label)
      assertEquals(plan.allowedClaim.map(_.key), Some(claimKey), label)
      assertEquals(DeterministicRenderer.fromPlan(plan).map(_.claimKey), Some(claimKey), label)

      Vector(
        "Support" -> support,
        "Context" -> context,
        "Unselected" -> unselected,
        "Blocked" -> blocked,
        "Capped" -> capped,
        "Refuted" -> refuted
      ).foreach: (state, verdict) =>
        assertNoPublicClaimPlan(verdict, s"$label $state")

  test("ATIH Stage-5 ExplanationPlan rejects sibling claim substitution incomplete sidecars and contamination"):
    engineRows.foreach: row =>
      val (label, _, story, _, claimKey) = row
      val verdict = StoryTable.choose(Vector(story)).head
      val plan = ExplanationPlan.fromSelected(verdict).get
      val rendered = DeterministicRenderer.fromPlan(plan).get

      allowedAtihClaims.filterNot(_.key == claimKey).foreach: siblingClaim =>
        val substituted = plan.copy(allowedClaim = Some(siblingClaim))
        assertEquals(DeterministicRenderer.fromPlan(substituted), None, s"$label cannot render ${siblingClaim.key}")
        assertEquals(LlmNarrationSmoke.mockNarrate(substituted, rendered), None, s"$label cannot narrate ${siblingClaim.key}")

      assertNoPublicClaimPlan(StoryTable.choose(Vector(incompleteSidecar(story))).head, s"$label incomplete sidecar")
      assertNoPublicClaimPlan(StoryTable.choose(Vector(contaminatedSidecar(story))).head, s"$label contaminated sidecar")

  test("ATIH Stage-5 Renderer emits only bounded templates for selected claim keys"):
    engineRows.foreach: row =>
      val (label, _, story, _, claimKey) = row
      val verdict = StoryTable.choose(Vector(story)).head
      val plan = ExplanationPlan.fromSelected(verdict).get
      val rendered = DeterministicRenderer.fromPlan(plan).get

      assertEquals(rendered.claimKey, claimKey, label)
      assertEquals(rendered.text, expectedRendererText(label), label)
      assertEquals(rendered.forbiddenCheckPassed, true, label)
      assertTextExcludes(rendered.text, rendererForbiddenPublicText, label)

    val sourceVerdict = StoryTable.choose(Vector(rawSourceStory)).head
    assertNoPublicClaimPlan(sourceVerdict, "source row")
    assertEquals(DeterministicRenderer.fromPlan(queenHitPlan.copy(allowedClaim = None)), None)

  test("ATIH Stage-5 LLM smoke accepts only bounded rendered fields and rejects added chess facts"):
    engineRows.foreach: row =>
      val (label, _, story, _, claimKey) = row
      val verdict = StoryTable.choose(Vector(story)).head
      val plan = ExplanationPlan.fromSelected(verdict).get
      val rendered = DeterministicRenderer.fromPlan(plan).get
      val prompt = LlmNarrationSmoke.codexCliPrompt(plan, rendered).get

      assert(prompt.contains("Rephrase only. Do not add chess facts."), label)
      assert(prompt.contains(s"claimKey: $claimKey"), label)
      assert(prompt.contains("renderedText:"), label)
      assert(!prompt.toLowerCase(java.util.Locale.ROOT).contains("proofFailures".toLowerCase(java.util.Locale.ROOT)), label)
      assert(!prompt.contains("evalBefore"), label)
      assertEquals(LlmNarrationSmoke.mockNarrate(plan, rendered), Some(rendered.text), label)

      llmEngineAndForceOverclaims.foreach: output =>
        assertEquals(LlmNarrationSmoke.check(plan, rendered, output).accepted, false, s"$label rejects $output")

    Vector(queenHitPlan, loosePlan).foreach: plan =>
      val rendered = DeterministicRenderer.fromPlan(plan).get
      llmAttackTargetOverclaims.foreach: output =>
        assertEquals(LlmNarrationSmoke.check(plan, rendered, output).accepted, false, output)

  test("ATIH Stage-5 downstream boundary authority lives only in StoryInteractionLaw"):
    val law = Files.readString(Paths.get("modules/commentary/docs/StoryInteractionLaw.md"))
    Vector(
      "## ATIH Stage-5 Downstream Boundary",
      "ATIH-5 opens ExplanationPlan, Renderer, and LLM smoke hardening only.",
      "`attacks_queen`",
      "`attacks_loose_piece`",
      "`can_win_piece`",
      "`material_balance_changes`",
      "ExplanationPlan accepts only selected uncapped Lead Verdicts for public claim lowering.",
      "ExplanationPlan rejects Support, Context, Blocked, capped, and refuted rows for public claim lowering.",
      "ExplanationPlan rejects sibling claim-key substitution.",
      "ExplanationPlan rejects incomplete proof sidecars and contaminated rows.",
      "Renderer input is ExplanationPlan only.",
      "Renderer emits only the selected claim key's bounded template.",
      "Renderer cannot inspect raw Story, proofs, BoardFacts, EngineCheck, raw PV, proofFailures, or source rows.",
      "LLM smoke input is only renderedText, claimKey, strength, and forbidden wording.",
      "Rephrase only. Do not add chess facts.",
      "LLM smoke must reject added material, queen-trap, hanging, pressure, tempo, best/only/forced, winning, or engine facts.",
      "proofFailures must not become public text.",
      "raw engine eval/PV must not become public wording.",
      "source rows must not become public claim owners."
    ).foreach: marker =>
      assert(law.contains(marker), s"ATIH-5 law must pin: $marker")

  test("ATIH Stage-6 public routes stay fail closed"):
    val law = Files.readString(Paths.get("modules/commentary/docs/StoryInteractionLaw.md"))
    val routes = Files.readString(Paths.get("conf/routes"))
    val controller = Files.readString(Paths.get("app/controllers/Commentary.scala"))

    Vector(
      "## ATIH Stage-6 Docs / Public Surface",
      "ATIH-6 opens documentation and public-surface audit only.",
      "detailed ATIH authority lives only in `StoryInteractionLaw.md`.",
      "README, SSOT, Architecture, Contract, and Manifest remain summary-only if touched.",
      "`AGENTS.md` remains unchanged unless durable operator rules change.",
      "docs tests must prevent duplicated detailed ATIH law outside `StoryInteractionLaw.md`.",
      "`/api/commentary/render` remains fail-closed.",
      "`/internal/commentary/render-local-probe` remains fail-closed.",
      "no public route `200`.",
      "no production API.",
      "no public/user-facing LLM narration.",
      "no route opens because attack-target hardening exists.",
      "no env switch or local probe returns production-style rendered payload."
    ).foreach: marker =>
      assert(law.contains(marker), s"ATIH-6 law must pin: $marker")

    assert(routes.contains("POST  /api/commentary/render           controllers.Commentary.renderCommentary"))
    assert(
      routes.contains("POST  /internal/commentary/render-local-probe controllers.Commentary.renderLocalProbeCommentary")
    )
    assert(controller.contains("def renderCommentary"))
    assert(controller.contains("def renderLocalProbeCommentary"))
    assert(controller.contains("ServiceUnavailable(unavailable).toFuccess"))
    assert(controller.contains("\"noCommentary\" -> true"))
    assert(controller.contains("\"render\" -> JsNull"))
    assert(!controller.contains("Ok("), "ATIH must not open a route 200")
    Vector(
      "AttackTargetInteraction",
      "QueenHitProof",
      "LoosePieceProof",
      "HangingProof",
      "MaterialProof",
      "BoardFacts",
      "Story(",
      "EngineCheck",
      "ExplanationPlan",
      "DeterministicRenderer",
      "LlmNarrationSmoke",
      "proofFailures"
    ).foreach: raw =>
      assert(!controller.contains(raw), s"Commentary controller must not expose ATIH/raw $raw")

  test("ATIH Stage-7 closeout keeps attack target homes separated"):
    val rows = engineRows.map: row =>
      val (label, _, story, _, claimKey) = row
      label -> (story, StoryTable.choose(Vector(story)).head, claimKey)

    rows.foreach: (label, entry) =>
      val (_, verdict, claimKey) = entry
      val plan = ExplanationPlan.fromSelected(verdict).get
      val rendered = DeterministicRenderer.fromPlan(plan).get
      assertEquals(verdict.role, Role.Lead, label)
      assertEquals(plan.allowedClaim.map(_.key), Some(claimKey), label)
      assertEquals(rendered.claimKey, claimKey, label)
      assertTextExcludes(rendered.text, closeoutForbiddenPublicText, label)

    val queen = rows.find(_._1 == "QueenHit").get._2._1
    assertEquals(queen.tactic, Some(Tactic.QueenHit))
    assertEquals(queen.writer, Some(StoryWriter.TacticQueenHit))
    assert(queen.queenHitProof.nonEmpty)
    assertEquals(queen.loosePieceProof, None)
    assertEquals(queen.captureResult, None)
    assertEquals(standaloneClaimKey(StoryTable.choose(Vector(queen)).head), Some("attacks_queen"))

    val loose = rows.find(_._1 == "Loose").get._2._1
    assertEquals(loose.tactic, Some(Tactic.Loose))
    assertEquals(loose.writer, Some(StoryWriter.TacticLoose))
    assert(loose.loosePieceProof.nonEmpty)
    assertEquals(loose.queenHitProof, None)
    assertEquals(loose.captureResult, None)
    assertEquals(standaloneClaimKey(StoryTable.choose(Vector(loose)).head), Some("attacks_loose_piece"))

    val hanging = rows.find(_._1 == "Hanging").get._2._1
    assertEquals(hanging.tactic, Some(Tactic.Hanging))
    assertEquals(hanging.writer, Some(StoryWriter.TacticHanging))
    assert(hanging.captureResult.nonEmpty)
    assertEquals(hanging.queenHitProof, None)
    assertEquals(hanging.loosePieceProof, None)
    assertEquals(standaloneClaimKey(StoryTable.choose(Vector(hanging)).head), Some("can_win_piece"))

    val material = rows.find(_._1 == "Material").get._2._1
    assertEquals(material.scene, Scene.Material)
    assertEquals(material.writer, Some(StoryWriter.SceneMaterial))
    assert(material.captureResult.nonEmpty)
    assertEquals(material.queenHitProof, None)
    assertEquals(material.loosePieceProof, None)
    assertEquals(standaloneClaimKey(StoryTable.choose(Vector(material)).head), Some("material_balance_changes"))

    assertEquals(SceneMaterial.write(board(queenHitFen), queenHitMove), None)
    assertEquals(SceneMaterial.write(board(looseFen), looseMove), None)
    assertEquals(TacticHanging.write(board(looseFen), looseMove), None)

    val queenHitVerdict = StoryTable.choose(Vector(queen)).head
    assertTextExcludes(publicText(queenHitVerdict), Vector("wins queen", "traps queen", "queen is lost"), "QueenHit")
    assertNoPublicClaimPlan(StoryTable.choose(Vector(rawSourceStory.copy(scene = Scene.Material))).head, "raw source")
    val unboundEngineCheck =
      EngineCheck.fromStory(
        facts = board(queenHitFen),
        story = None,
        engineLine = None,
        replyLine = None,
        evalBefore = None,
        evalAfter = None,
        depth = None,
        freshnessPly = None
      )
    assertEquals(unboundEngineCheck.publicClaimAllowed, false)

  test("ATIH Stage-7 closeout authority lives only in StoryInteractionLaw"):
    val law = Files.readString(Paths.get("modules/commentary/docs/StoryInteractionLaw.md"))
    Vector(
      "## ATIH Stage-7 Closeout / Final Verification",
      "ATIH-7 opens no new chess meaning.",
      "ATIH-7 closes only Attack-Target Interaction Hardening.",
      "QueenHit remains queen-attacked only.",
      "Loose remains undefended non-king piece attacked only.",
      "Hanging remains its existing bounded hanging/can-win-piece meaning only.",
      "Material remains actual material balance change now only.",
      "No proof home, Story label, writer, or speech key is reused as another layer.",
      "No attack-only row creates material.",
      "No Loose row creates Hanging.",
      "No QueenHit row creates wins-queen or trap.",
      "No Material row is created from BoardFacts, EngineCheck, raw source, or renderer text.",
      "wins queen",
      "traps queen",
      "queen is lost",
      "hanging from Loose",
      "wins piece/material from attack-only rows",
      "free piece",
      "en prise",
      "underdefended",
      "overloaded defender",
      "pressure",
      "initiative",
      "tempo",
      "best / only / forced",
      "decisive / winning",
      "public route `200`",
      "production API",
      "public/user-facing LLM narration",
      "Completion standard: ATIH closes when"
    ).foreach: marker =>
      assert(law.contains(marker), s"ATIH-7 law must pin: $marker")

  private def board(fen: String): BoardFacts =
    BoardFacts.fromFen(fen).fold(error => fail(s"invalid ATIH FEN: $fen -> $error"), identity)

  private def queenHitStory: Story =
    TacticQueenHit.write(board(queenHitFen), Some(queenHitMove)).get.copy(proof = orderingProof)

  private def looseStory: Story =
    TacticLoose.write(board(looseFen), Some(looseMove)).get.copy(proof = orderingProof)

  private def forgedAttackStory(tactic: Tactic): Story =
    Story(
      scene = Scene.Tactic,
      tactic = Some(tactic),
      side = Side.White,
      rival = Side.Black,
      target = Some(Square('h', 5)),
      anchor = Some(Square('h', 2)),
      route = Some(queenHitMove),
      routeSan = Some("Rh2"),
      proof = orderingProof,
      storyProof = StoryProof.fromBoardFacts(board(queenHitFen), queenHitMove)
    )

  private def rawSourceStory: Story =
    Story(
      scene = Scene.Source,
      proof = orderingProof,
      side = Side.White,
      rival = Side.Black,
      target = Some(Square('h', 5)),
      anchor = Some(Square('h', 2)),
      route = Some(queenHitMove),
      routeSan = Some("Rh2"),
      storyProof = StoryProof.fromBoardFacts(board(queenHitFen), queenHitMove)
    )

  private def queenHitPlan: ExplanationPlan =
    ExplanationPlan.fromSelected(StoryTable.choose(Vector(queenHitStory)).head).get

  private def loosePlan: ExplanationPlan =
    ExplanationPlan.fromSelected(StoryTable.choose(Vector(looseStory)).head).get

  private def engineRows: Vector[(String, BoardFacts, Story, (Story, EngineCheck) => Story, String)] =
    val queenFacts = board(queenHitFen)
    val looseFacts = board(looseFen)
    val materialFacts = board(materialFen)
    Vector(
      (
        "QueenHit",
        queenFacts,
        TacticQueenHit.write(queenFacts, Some(queenHitMove)).get.copy(proof = orderingProof),
        (story: Story, check: EngineCheck) => attachQueenHit(story, check),
        "attacks_queen"
      ),
      (
        "Loose",
        looseFacts,
        TacticLoose.write(looseFacts, Some(looseMove)).get.copy(proof = orderingProof),
        (story: Story, check: EngineCheck) => attachLoose(story, check),
        "attacks_loose_piece"
      ),
      (
        "Hanging",
        materialFacts,
        TacticHanging.write(materialFacts, materialMove).get.copy(proof = orderingProof),
        (story: Story, check: EngineCheck) => attachHanging(story, check),
        "can_win_piece"
      ),
      (
        "Material",
        materialFacts,
        SceneMaterial.write(materialFacts, materialMove).get.copy(proof = orderingProof),
        (story: Story, check: EngineCheck) => attachMaterial(story, check),
        "material_balance_changes"
      )
    )

  private def incompleteSidecar(story: Story): Story =
    story.writer match
      case Some(StoryWriter.TacticQueenHit) => story.copy(queenHitProof = None)
      case Some(StoryWriter.TacticLoose) => story.copy(loosePieceProof = None)
      case Some(StoryWriter.TacticHanging) => story.copy(captureResult = None)
      case Some(StoryWriter.SceneMaterial) => story.copy(captureResult = None)
      case _ => story

  private def contaminatedSidecar(story: Story): Story =
    val queenFacts = board(queenHitFen)
    val queenProof = TacticQueenHit.write(queenFacts, Some(queenHitMove)).get.queenHitProof
    val looseProof = TacticLoose.write(queenFacts, Some(queenHitMove)).get.loosePieceProof
    story.writer match
      case Some(StoryWriter.TacticQueenHit) => story.copy(loosePieceProof = looseProof)
      case Some(StoryWriter.TacticLoose) => story.copy(queenHitProof = queenProof)
      case Some(StoryWriter.TacticHanging) => story.copy(queenHitProof = queenProof)
      case Some(StoryWriter.SceneMaterial) => story.copy(queenHitProof = queenProof)
      case _ => story

  private def attachQueenHit(story: Story, check: EngineCheck): Story =
    TacticQueenHit.withEngineCheck(story, check).get

  private def attachLoose(story: Story, check: EngineCheck): Story =
    TacticLoose.withEngineCheck(story, check).get

  private def attachHanging(story: Story, check: EngineCheck): Story =
    TacticHanging.withEngineCheck(story, check).get

  private def attachMaterial(story: Story, check: EngineCheck): Story =
    SceneMaterial.withEngineCheck(story, check).get

  private def engineCheck(
      facts: BoardFacts,
      story: Story,
      status: EngineCheckStatus,
      before: Int = 20,
      after: Int = 20
  ): EngineCheck =
    EngineCheck.fromStory(
      facts = facts,
      story = Some(story),
      engineLine = story.route.map(route => EngineLine(Vector(route))),
      replyLine = Some(EngineLine(Vector(Line(Square('e', 8), Square('f', 8))))),
      evalBefore = Some(EngineEval(before)),
      evalAfter = Some(EngineEval(after)),
      depth = Some(18),
      freshnessPly = Some(0),
      requestedStatus = status
    )

  private def standaloneClaimKey(verdict: Verdict): Option[String] =
    ExplanationPlan.fromSelected(verdict).flatMap(DeterministicRenderer.fromPlan).map(_.claimKey)

  private def publicText(verdict: Verdict): String =
    ExplanationPlan.fromSelected(verdict).flatMap(DeterministicRenderer.fromPlan).map(_.text).getOrElse("")

  private def assertNoStandaloneText(verdict: Verdict, label: String): Unit =
    assertEquals(ExplanationPlan.fromSelected(verdict).flatMap(DeterministicRenderer.fromPlan), None, label)

  private def assertNoPublicClaimPlan(verdict: Verdict, label: String): Unit =
    val plan = ExplanationPlan.fromSelected(verdict)
    assertEquals(plan.flatMap(_.allowedClaim).map(_.key), None, label)
    assertEquals(plan.flatMap(DeterministicRenderer.fromPlan), None, label)

  private def assertTextExcludes(text: String, forbidden: Vector[String], label: String): Unit =
    val normalized = text.toLowerCase(java.util.Locale.ROOT)
    forbidden.foreach: phrase =>
      assert(!normalized.contains(phrase), s"$label public text must not contain: $phrase")

  private def verdictSignature(verdict: Verdict) =
    (
      verdict.role,
      verdict.leadAllowed,
      verdict.story.scene,
      verdict.story.tactic,
      verdict.story.writer,
      verdict.story.target,
      verdict.story.anchor,
      verdict.story.route,
      verdict.engineStrengthLimited
    )

  private def lowProof: Proof =
    orderingProof.copy(boardProof = 60, lineProof = 60, ownerProof = 60, anchorProof = 60, routeProof = 60)

  private val engineForbiddenPublicText =
    Vector(
      "engine says",
      "eval",
      "raw pv",
      "best move",
      "only move",
      "forced",
      "pressure",
      "initiative",
      "decisive",
      "winning"
    )

  private val attackOnlyEngineForbiddenPublicText =
    engineForbiddenPublicText ++ Vector(
      "wins queen",
      "wins the queen",
      "wins piece",
      "wins a piece",
      "wins material"
    )

  private val rendererForbiddenPublicText =
    Vector(
      "engine says",
      "eval",
      "raw pv",
      "proof failure",
      "source row",
      "best move",
      "only move",
      "forced",
      "decisive",
      "winning",
      "pressure",
      "initiative"
    )

  private val closeoutForbiddenPublicText =
    rendererForbiddenPublicText ++ Vector(
      "wins queen",
      "traps queen",
      "queen is lost",
      "free piece",
      "en prise",
      "underdefended",
      "overloaded",
      "tempo"
    )

  private val allowedAtihClaims =
    Vector(
      ExplanationClaim.AttacksQueen,
      ExplanationClaim.AttacksLoosePiece,
      ExplanationClaim.CanWinPiece,
      ExplanationClaim.MaterialBalanceChanges
    )

  private def expectedRendererText(label: String): String =
    label match
      case "QueenHit" => "Rh2 attacks the queen on h5."
      case "Loose" => "Rh2 attacks the undefended piece on h5."
      case "Hanging" => "dxe5 wins material against the piece on e5."
      case "Material" => "After dxe5, White comes out ahead in material."
      case _ => fail(s"unexpected ATIH renderer label: $label")

  private val llmEngineAndForceOverclaims =
    Vector(
      "engine says this is best",
      "the eval is +2.0",
      "raw PV confirms it",
      "this is the best move",
      "this is the only move",
      "this is forced",
      "this is winning",
      "this is decisive"
    )

  private val llmAttackTargetOverclaims =
    Vector(
      "Rh2 wins material.",
      "Rh2 traps the queen.",
      "Rh2 creates a hanging piece.",
      "Rh2 creates pressure.",
      "Rh2 gains tempo."
    )

  private def orderingProof: Proof =
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

package lila.commentary.chess

import java.nio.file.{ Files, Paths }

class ProofDeficitDiagnosticsStage4Test extends munit.FunSuite:

  test("Stage-4 diagnostic strings do not appear in rendered text"):
    val blockedVerdict = StoryTable.choose(Vector(materialStory.copy(writer = None))).head
    val diagnostic = blockedVerdict.proofDeficitDiagnostic.get
    val rendered = leadRendered

    assertEquals(blockedVerdict.role, Role.Blocked)
    assertEquals(rendered.text.contains(diagnostic.reason), false)
    assertNoDiagnosticText(rendered.text)
    assertEquals(rendered.text.contains("writer"), false)
    assertEquals(rendered.text.contains("proofCoordinates"), false)

  test("Stage-4 LLM smoke prompt accepts only rendered contract fields and rejects diagnostic wording"):
    val (plan, rendered) = leadPlanAndRendered
    val prompt = LlmNarrationSmoke.codexCliPrompt(plan, rendered).get

    Vector("renderedText:", "claimKey:", "strength:", "forbiddenWording:").foreach: field =>
      assert(prompt.contains(field), field)
    assert(prompt.contains("instruction: Rephrase only. Do not add chess facts."))

    Vector(
      "proofDeficitDiagnostic",
      "ProofDeficitDiagnostic",
      "diagnosticReason",
      "blockedBy",
      "boardFactsPresent",
      "proofCoordinates",
      "missingSidecar",
      "proofFailures",
      "EngineCheck",
      "raw PV",
      "source row"
    ).foreach: forbidden =>
      assertEquals(prompt.contains(forbidden), false, forbidden)

    Vector(
      "Use diagnostic reason in text: this row cannot produce standalone text.",
      "Explain why blocked: blockedBy contains writer and proofCoordinates route.",
      "Tell user missing proof: missingSidecar has StoryProof legal line.",
      "Engine/proof failure wording: EngineCheck Refutes and proofFailures are present.",
      "Raw PV wording: raw PV says d4e5.",
      "Source-row wording: source row ranked this claim."
    ).foreach: output =>
      val result = LlmNarrationSmoke.check(plan, rendered, output)
      assertEquals(result.accepted, false, output)

  test("Stage-4 expression-layer APIs do not consume diagnostic types"):
    val explanationSource =
      Files.readString(Paths.get("modules/commentary/src/main/scala/lila/commentary/chess/ExplanationPlan.scala"))
    val rendererSource =
      Files.readString(Paths.get("modules/commentary/src/main/scala/lila/commentary/chess/DeterministicRenderer.scala"))
    val llmSource =
      Files.readString(Paths.get("modules/commentary/src/main/scala/lila/commentary/chess/LlmNarrationSmoke.scala"))

    Vector(explanationSource, rendererSource, llmSource).foreach: source =>
      assert(!source.contains("ProofDeficitDiagnostic"))
      assert(!source.contains("proofDeficitDiagnostic"))
      assert(!source.contains("proofCoordinates"))
      assert(!source.contains("missingSidecar"))

    val llmMethods = LlmNarrationSmoke.getClass.getDeclaredMethods.map(_.toString).mkString("\n")
    assert(!llmMethods.contains("ProofDeficitDiagnostic"))
    assert(llmMethods.contains("ExplanationPlan"))
    assert(llmMethods.contains("RenderedLine"))

  test("Stage-4 public routes remain tombstones and Verdict.values shape is unchanged"):
    val controller = Files.readString(Paths.get("app/controllers/Commentary.scala"))
    val verdict = StoryTable.choose(Vector(materialStory)).head
    val blocked = StoryTable.choose(Vector(materialStory.copy(writer = None))).head
    val values = blocked.values

    assertEquals(verdict.values.size, Verdict.Size)
    assertEquals(blocked.values.size, Verdict.Size)
    assertEquals(values, blocked.values)
    assertEquals(blocked.proofDeficitDiagnostic.nonEmpty, true)

    Vector(
      "ProofDeficitDiagnostics",
      "ProofDeficitDiagnostic",
      "proofDeficitDiagnostic",
      "proofCoordinates",
      "missingSidecar",
      "blockedBy"
    ).foreach: forbidden =>
      assert(!controller.contains(forbidden), forbidden)

    assert(controller.contains("ServiceUnavailable(unavailable).toFuccess"))
    assert(controller.contains("\"status\" -> \"unavailable\""))
    assert(controller.contains("\"noCommentary\" -> true"))
    assert(controller.contains("\"render\" -> JsNull"))
    assert(!controller.contains("Ok("), "commentary route tombstones must not return 200")

  private val materialFen = "4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1"
  private val materialMove = Line(Square('d', 4), Square('e', 5))

  private def materialStory: Story =
    SceneMaterial.write(board(materialFen), materialMove).get

  private def leadPlanAndRendered: (ExplanationPlan, RenderedLine) =
    val verdict = StoryTable.choose(Vector(materialStory)).head
    val plan = ExplanationPlan.fromSelected(verdict).get
    val rendered = DeterministicRenderer.fromPlan(plan).get
    (plan, rendered)

  private def leadRendered: RenderedLine =
    leadPlanAndRendered._2

  private def board(fen: String): BoardFacts =
    BoardFacts.fromFen(fen).toOption.get

  private def assertNoDiagnosticText(text: String): Unit =
    val normalized = text.toLowerCase(java.util.Locale.ROOT)
    Vector(
      "proof-deficit",
      "diagnostic",
      "blockedby",
      "blocked by",
      "proofcoordinates",
      "proof coordinates",
      "missingsidecar",
      "missing sidecar",
      "proof failure",
      "prooffailures",
      "missing proof",
      "raw pv",
      "source row"
    ).foreach: phrase =>
      assertEquals(normalized.contains(phrase), false, phrase)

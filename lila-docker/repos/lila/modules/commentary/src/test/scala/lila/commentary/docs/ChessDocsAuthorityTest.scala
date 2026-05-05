package lila.commentary.docs

import java.nio.file.{ Files, Paths }

import scala.jdk.CollectionConverters.*

class ChessDocsAuthorityTest extends munit.FunSuite:

  private val docsRoot = Paths.get("modules/commentary/docs")
  private val agentInstructions = Paths.get("../../..", "AGENTS.md")
  private val LiveDocs =
    Vector(
      "BoardFacts.md",
      "BoardMoodCutLaw.md",
      "BoardMoodSplitLaw.md",
      "ChessCommentarySSOT.md",
      "ChessModelArchitecture.md",
      "ChessModelContract.md",
      "ChessResetRationale.md",
      "LegacyPruneManifest.md",
      "README.md",
      "StoryInteractionLaw.md",
      "StoryResurrectionLaw.md"
    )
  private val RetiredRootDocs =
    Vector(
      "CommentaryCoreSSOT.md",
      "SemanticModelArchitecture.md",
      "LegacyArchiveIndex.md",
      "CommentaryFrontendBridgeContract.md"
    )
  private val commentaryBridgeTest = Paths.get("ui/analyse/tests/commentaryBridge.test.ts")
  private val HardPublicOutputBlocker =
    "Missing side, target, anchor, route, rival, required legal line, or same-root proof sidecar is a hard public-output block."

  private val SplitSlots =
    Vector(
      "S031", "S076", "S077", "S079", "S092", "S093", "S095", "S102", "S106", "S107", "S109",
      "S110", "S118", "S122", "S123", "S125", "S126", "S128", "S129", "S130", "S133", "S137",
      "S143", "S144", "S145", "S146", "S149", "S153", "S159", "S163", "S168", "S169", "S170",
      "S171", "S172", "S173", "S179", "S184", "S185", "S186", "S187", "S188", "S189", "S192",
      "S193", "S194", "S195", "S196", "S197", "S198", "S199", "S200", "S202", "S203", "S204",
      "S205", "S206", "S207", "S208", "S209", "S210", "S211", "S212", "S213", "S218"
    )

  private val CutSlots =
    Vector(
      "S013", "S014", "S078", "S094", "S104", "S111", "S120", "S127", "S131", "S142", "S147",
      "S158", "S164", "S174", "S175", "S180", "S190", "S191", "S201", "S214", "S215", "S216",
      "S217", "S219", "S220", "S221", "S222", "S223"
    )

  private val Scenes =
    Vector(
      "Scene.Tactic",
      "Scene.Blunder",
      "Scene.Material",
      "Scene.King",
      "Scene.Defense",
      "Scene.Opening",
      "Scene.Pawns",
      "Scene.Plan",
      "Scene.Pieces",
      "Scene.Space",
      "Scene.Initiative",
      "Scene.Convert",
      "Scene.Endgame",
      "Scene.Counterplay",
      "Scene.Source",
      "Scene.Quiet"
    )

  private val Plans =
    Vector(
      "Plan.Minority",
      "Plan.Majority",
      "Plan.CenterBreak",
      "Plan.FlankBreak",
      "Plan.Storm",
      "Plan.Expansion",
      "Plan.Cramp",
      "Plan.Outpost",
      "Plan.BadPiece",
      "Plan.Reroute",
      "Plan.Bishops",
      "Plan.Blockade",
      "Plan.OpenFile",
      "Plan.Seventh",
      "Plan.ColorBind",
      "Plan.WeakSquare",
      "Plan.Isolani",
      "Plan.BackwardPawn",
      "Plan.HangingPawns",
      "Plan.ChainBase",
      "Plan.PasserMake",
      "Plan.PasserBlock",
      "Plan.Race",
      "Plan.Trade",
      "Plan.Simplify",
      "Plan.KeepPieces",
      "Plan.Overload",
      "Plan.Prophy",
      "Plan.Counterplay",
      "Plan.Initiative",
      "Plan.KingConvert",
      "Plan.Convert"
    )

  private val Tactics =
    Vector(
      "Tactic.Loose",
      "Tactic.Hanging",
      "Tactic.AbsPin",
      "Tactic.RelPin",
      "Tactic.Skewer",
      "Tactic.Xray",
      "Tactic.Fork",
      "Tactic.Discover",
      "Tactic.RemoveGuard",
      "Tactic.Overload",
      "Tactic.BackRank",
      "Tactic.MateNet",
      "Tactic.SafeCheck",
      "Tactic.PawnFork",
      "Tactic.PawnPush",
      "Tactic.Trap",
      "Tactic.QueenHit",
      "Tactic.KingOpen",
      "Tactic.Promote",
      "Tactic.InBetween",
      "Tactic.Clear",
      "Tactic.Decoy",
      "Tactic.Deflect",
      "Tactic.Tempo"
    )

  private val ProofFields =
    Vector(
      "boardProof",
      "lineProof",
      "ownerProof",
      "anchorProof",
      "routeProof",
      "persistence",
      "immediacy",
      "forcing",
      "conversionPrize",
      "counterplayRisk",
      "kingHeat",
      "pieceSupport",
      "pawnSupport",
      "sourceFit",
      "novelty",
      "clarity"
    )

  private def rootDocNames: Vector[String] =
    val stream = Files.list(docsRoot)
    try
      stream
        .iterator()
        .asScala
        .filter(path => Files.isRegularFile(path))
        .map(path => path.getFileName.toString)
        .toVector
        .sorted
    finally stream.close()

  private def liveDocContents: String =
    LiveDocs.map(fileName => Files.readString(docsRoot.resolve(fileName))).mkString("\n")

  private def tableSlots(contents: String): Vector[String] =
    """(?m)^\| (S\d{3}) `""".r.findAllMatchIn(contents).map(_.group(1)).toVector

  private def tableRows(contents: String): Vector[Vector[String]] =
    contents.linesIterator
      .filter(_.startsWith("| S"))
      .map: line =>
        line.stripPrefix("|").stripSuffix("|").split("\\|").toVector.map(_.trim)
      .toVector

  private def bulletSlots(contents: String): Vector[String] =
    """(?m)^- (S\d{3}) `""".r.findAllMatchIn(contents).map(_.group(1)).toVector

  private def namedRows(contents: String, prefix: String): Vector[Vector[String]] =
    contents.linesIterator
      .filter(_.startsWith(s"| $prefix"))
      .map: line =>
        line.stripPrefix("|").stripSuffix("|").split("\\|").toVector.map(_.trim)
      .toVector

  private def assertNoForbiddenLawTerms(contents: String): Unit =
    val forbidden =
      Vector(
        "pipeline",
        "builder",
        "selector",
        "gate",
        "semantic",
        "object",
        "delta",
        "certification",
        "v1",
        "later",
        "future",
        "defer",
        "deferred",
        "tbd",
        "todo",
        "나중에"
    )
    forbidden.foreach: term =>
      if term.exists(_.toInt > 127) then
        assert(!contents.contains(term), s"$term must not appear in law docs")
      else assert(s"(?i)\\b$term\\b".r.findFirstIn(contents).isEmpty, s"$term must not appear in law docs")
    val nonPawnCandidate = """(?i)\bcandidate\b(?!_passer)""".r
    assert(nonPawnCandidate.findFirstIn(contents).isEmpty, "candidate is allowed only as candidate_passer")

  test("chess model branch exposes only live reset docs as root authority"):
    val liveDocs = rootDocNames

    assertEquals(liveDocs, LiveDocs)

    RetiredRootDocs.foreach: fileName =>
      assert(!liveDocs.contains(fileName), s"$fileName must not be listed as root authority")
      assert(!Files.exists(docsRoot.resolve(fileName)), s"$fileName must not remain live")

  test("live authority is the chess model chain"):
    val readme = Files.readString(docsRoot.resolve("README.md"))
    val ssot = Files.readString(docsRoot.resolve("ChessCommentarySSOT.md"))
    val architecture = Files.readString(docsRoot.resolve("ChessModelArchitecture.md"))
    val boardFacts = Files.readString(docsRoot.resolve("BoardFacts.md"))
    val modelContract = Files.readString(docsRoot.resolve("ChessModelContract.md"))
    val rationale = Files.readString(docsRoot.resolve("ChessResetRationale.md"))
    val cutLaw = Files.readString(docsRoot.resolve("BoardMoodCutLaw.md"))
    val splitLaw = Files.readString(docsRoot.resolve("BoardMoodSplitLaw.md"))
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))
    val resurrectionLaw = Files.readString(docsRoot.resolve("StoryResurrectionLaw.md"))
    val manifest = Files.readString(docsRoot.resolve("LegacyPruneManifest.md"))

    assert(readme.contains("Live authority is exactly and exhaustively"))
    assert(readme.contains("mismatch is a no-go state"))
    LiveDocs.foreach: docName =>
      assert(readme.contains(s"`$docName`"), s"README must list $docName as live authority")
      assert(
        manifest.contains(s"`$docName`") || manifest.contains(s"`docs/$docName`"),
        s"LegacyPruneManifest must list $docName as live authority"
      )
    assert(!readme.contains("archived authority"))
    assert(ssot.contains("`BoardMood` -> `Story` -> `StoryTable` -> `Verdict`"))
    assert(ssot.contains("No other path owns current public chess meaning."))
    assert(!ssot.contains("CommentaryPipelineInput"))
    assert(!ssot.contains("candidate generation"))
    assert(!ssot.contains("semantic selector"))
    assert(architecture.contains("HCE-style deterministic chess scorer"))
    assert(architecture.contains("`BoardMood` -> `Story` -> `StoryTable` -> `Verdict`"))
    assert(architecture.contains("`48` bit slots"))
    assert(architecture.contains("Stage 1 - Board Facts"))
    assert(!architecture.contains("Historical selector-shaped scaffolds may remain"))
    assert(!architecture.contains("gate bits"))
    assert(boardFacts.contains("Stage 1 name is `Board Facts`."))
    assert(boardFacts.contains("Board state observes. Story proves."))
    assert(boardFacts.contains("A named board fact is still only an observation."))
    assert(boardFacts.contains("Open file, pin, weak square, loose piece, and pawn lever"))
    assert(boardFacts.contains("No renderer, LLM, public route, template, frontend mock, or API transport may read Board Facts directly as commentary."))
    assert(modelContract.contains("Forbidden in new core model names"))
    assert(modelContract.contains("The model has exactly `32` long-term plan families"))
    assert(modelContract.contains("B00..B45 are packed `RootStateVector` transport words"))
    assert(modelContract.contains("BoardMood carries proof summaries, not public proof authority by itself"))
    assert(modelContract.contains("`BoardFacts` -> `BoardMood.fromFacts` is the runtime input boundary"))
    assert(modelContract.contains("`BoardFacts.fromFen` is the strict root transport entrypoint"))
    assert(modelContract.contains("`BoardFacts.fromPosition` is an internal/test boundary only"))
    assert(modelContract.contains("Runtime BoardFacts factories must not accept caller-supplied root"))
    assert(modelContract.contains("Strict same-board producers record the"))
    assert(modelContract.contains("factory-created `BoardFacts` instance identity as ready"))
    assert(modelContract.contains("parameters do not carry readiness authority"))
    assert(modelContract.contains("Manual `BoardFacts` assembly remains only for contract tests"))
    assert(modelContract.contains("must not expose case-class `copy` or product reconstruction"))
    assert(modelContract.contains("reflective construction and caller-supplied fields must not create readiness"))
    assert(modelContract.contains("`BoardMood.fromPieces` is scaffold-only and not runtime authority"))
    assert(modelContract.contains("BoardFacts required fields"))
    assert(modelContract.contains("Nested BoardFacts facts must be marked `known = true`"))
    assert(modelContract.contains("S015 `position_ready` may be `1` only when all nested facts are known and sane"))
    assert(modelContract.contains("B46 and B47 are legal destination summaries, not proof"))
    assert(modelContract.contains("`BoardMood.fromPacked`, `BoardMood.fromParts`, and `BoardMood.fromRoot`"))
    assert(modelContract.contains("canonicalize all closed scalar slots to `0`"))
    assert(modelContract.contains("`BoardMood` must not expose case-class `copy` or product reconstruction"))
    assert(modelContract.contains("reflective construction, must still canonicalize"))
    assert(modelContract.contains("board_hash_lo` and"))
    assert(modelContract.contains("closed by `BoardMoodSplitLaw.md` or `BoardMoodCutLaw.md`"))
    assert(modelContract.contains("Cut BoardMood meanings may be spoken only by Story under"))
    assert(architecture.contains("## Root Transport"))
    assert(architecture.contains("`BoardFacts.fromFen` is the strict runtime entrypoint into `BoardMood`"))
    assert(modelContract.contains("Binding and proof slots remain zero"))
    assert(cutLaw.contains("These slots are not kept as BoardMood live facts"))
    assert(cutLaw.contains("always `0`/silent"))
    assert(cutLaw.contains("`StoryResurrectionLaw.md`, not BoardMood"))
    assert(splitLaw.contains("The 65 split slots listed here are inactive in live BoardMood"))
    assert(splitLaw.contains("Re-entry is allowed only through the exact smaller BoardMood fact"))
    assert(splitLaw.contains("Public chess speech still belongs to Story only after Story binds side"))
    assert(splitLaw.contains("target, anchor, route, rival, required legal line, and same-root proof"))
    assert(splitLaw.contains("numeric proof scores alone are not enough"))
    assert(resurrectionLaw.contains("BoardMood does not create, score, revive, or hint at the meanings below"))
    assert(resurrectionLaw.contains("Every row inherits mandatory Story binding"))
    assert(resurrectionLaw.contains(HardPublicOutputBlocker.replace(" is a hard public-output block.", " means no public sentence.")))
    assert(interactionLaw.contains("Nonlinear interaction is explicit"))
    assert(interactionLaw.contains("A blocker can cap or silence a Story"))
    assert(interactionLaw.contains("An opposing Tactic or Blunder at public floor blocks Plan lead"))
    assert(manifest.contains("Authority is exactly the live root docs listed in `docs/README.md`"))
    assert(manifest.contains("fail-closed tombstones"))
    assert(!manifest.contains("Deleted legacy areas include the old public facade"))
    RetiredRootDocs.foreach: docName =>
      assert(manifest.contains(docName), s"LegacyPruneManifest must explicitly retire $docName")
    assert(modelContract.contains("The `Candidate` ban applies to new core model, type, and module names."))
    assert(modelContract.contains("schema term `candidate_passer` are allowed"))
    assert(modelContract.contains("candidate passer is a"))
    assert(modelContract.contains("chess pawn-structure term"))
    assert(modelContract.contains("Legacy candidate line selectors, candidate scoring,"))
    assert(rationale.contains("lower-layer success with public commentary readiness"))
    assert(rationale.contains("A fact being extracted is not the same as a fact"))
    assert(rationale.contains("Stockfish HCE-like lesson"))
    assert(rationale.contains("`BoardMood` -> `Story` -> `StoryTable` -> `Verdict`"))
    assert(rationale.contains("Legal destination masks are not proof"))
    assert(rationale.contains("Missing `known && sane` facts must not be hidden behind default zeroes"))
    assert(rationale.contains("Do not reintroduce new core names"))

  test("live docs freeze public path and proof openings"):
    val readme = Files.readString(docsRoot.resolve("README.md"))
    val ssot = Files.readString(docsRoot.resolve("ChessCommentarySSOT.md"))
    val architecture = Files.readString(docsRoot.resolve("ChessModelArchitecture.md"))
    val modelContract = Files.readString(docsRoot.resolve("ChessModelContract.md"))
    val rationale = Files.readString(docsRoot.resolve("ChessResetRationale.md"))
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))
    val manifest = Files.readString(docsRoot.resolve("LegacyPruneManifest.md"))
    val agents = Files.readString(agentInstructions)
    val contents = Vector(readme, ssot, architecture, modelContract, rationale, interactionLaw, manifest).mkString("\n")

    assert(readme.contains("Public route no-go"))
    assert(contents.contains("`/api/commentary/render`"))
    assert(contents.contains("`/internal/commentary/render-local-probe`"))
    assert(contents.contains("registered only as fail-closed tombstones"))
    assert(contents.contains("No `200`"))
    assert(contents.contains("rendered payload"))
    assert(contents.contains("environment switch"))
    assert(readme.contains("`BoardMood` no-go"))
    assert(contents.contains("`48` bits, `256` scalars, and `3,328` total values"))
    assert(contents.contains("Split/cut re-entry requires a named law and same-board producer proof"))
    assert(ssot.contains("`BoardMood` Sxxx expansion or re-entry"))
    assert(rationale.contains("no `Story` proof writers"))
    assert(manifest.contains("default runtime FEN to public"))
    assert(modelContract.contains("At this checkpoint no `BoardMood` Sxxx re-entry or proof writer is admitted"))
    assert(modelContract.contains("No `Story` proof writer is live in this checkpoint"))
    assert(contents.contains("Numeric `Proof` scores may rank blocked/context `Verdict` rows only"))
    assert(contents.contains("cannot set `leadAllowed=true` or produce `Role.Lead`"))
    assert(contents.contains("Runtime proof-sidecar writers for that full tuple do not exist"))
    assert(!contents.contains("does not yet require target and rival everywhere"))
    assert(!contents.contains("only conditionally required"))
    assert(contents.contains("Missing side, target, anchor, route, rival, required legal line, or same-root proof"))
    assert(contents.contains("hard public-output block"))
    assert(contents.contains("Old failing tests proved lower/scaffold/"))
    assert(rationale.contains("They did not prove default runtime"))
    assert(rationale.contains("FEN to public `Verdict`"))
    assert(interactionLaw.contains("| Scene.Opening | context-only |"))
    assert(interactionLaw.contains("no truth override or lead over board-backed Story"))
    assert(modelContract.contains("`Source` and `Opening` never lead over a board-backed story"))
    assert(readme.contains("Stage order no-go"))
    assert(readme.contains("Current implementation scope is Stage 1 Board Facts only"))
    assert(readme.contains("Stages 2-11 are a dependency map"))
    assert(readme.contains("LLM no-go"))
    assert(readme.contains("LLM narration remains"))
    assert(readme.contains("closed and must not judge chess"))
    assert(ssot.contains("proof-first Story kernel"))
    assert(ssot.contains("Engine is the truth oracle."))
    assert(ssot.contains("Backend is the proof and pedagogy system."))
    assert(ssot.contains("LLM is the narrator."))
    assert(ssot.contains("The LLM does not judge chess."))
    assert(ssot.contains("Engine lines, mate/tablebase proof, SEE, and bounded material results are"))
    assert(ssot.contains("truth-oracle evidence for backend proof"))
    assert(ssot.contains("Backend policy owns proof, Story selection,"))
    assert(ssot.contains("arbitration, and pedagogy"))
    assert(ssot.contains("it must not decide, prove, rank, repair, or invent"))
    assert(ssot.contains("`BoardMood` observes."))
    assert(ssot.contains("`Story` proves."))
    assert(ssot.contains("`StoryTable` arbitrates."))
    assert(ssot.contains("`Verdict` speaks."))
    assert(ssot.contains("Renderer only phrases."))
    assert(ssot.contains("feature is not a claim"))
    assert(ssot.contains("claim is not a public `Story`"))
    assert(ssot.contains("public `Story` requires proof-bearing identity"))
    assert(ssot.contains("whether that feature can become a"))
    assert(ssot.contains("Story with side, target, anchor, route, rival, required legal line, and"))
    assert(ssot.contains("same-root proof"))
    assert(ssot.contains("`observation` -> `proof sidecar` -> `Story` -> `Verdict` -> `Explanation IR` -> Renderer"))
    assert(ssot.contains("feature to public claim"))
    assert(ssot.contains("raw engine eval to public truth"))
    assert(ssot.contains("high numeric `Proof` score to `Role.Lead` without sidecar"))
    assert(ssot.contains("If a change makes commentary richer but weakens proof ownership, it is rejected."))
    Vector(
      "Renderer before Story proof sidecar is forbidden.",
      "LLM before Explanation IR is forbidden.",
      "Strategy before tactical/material proof is forbidden.",
      "Pedagogy before causal arbitration is forbidden.",
      "Personalization before stable Story taxonomy is forbidden."
    ).foreach: shortcut =>
      assert(ssot.contains(shortcut), s"SSOT must pin shortcut ban: $shortcut")
      assert(agents.contains(shortcut), s"AGENTS must pin shortcut ban: $shortcut")
    assert(ssot.contains("Pedagogy is backend policy over selected proof-backed `Verdict` data"))
    assert(ssot.contains("Renderer"))
    assert(ssot.contains("LLM layers may not choose instructional emphasis"))
    assert(rationale.contains("The old question was: what tactical or strategic feature is visible"))
    assert(rationale.contains("The new question is: can this feature become a Story with side,"))
    assert(rationale.contains("A tactic motif, plan affordance, source row, or engine number is not a public"))
    assert(rationale.contains("Engine lines, mate/tablebase proof, SEE, and bounded material results are"))
    assert(rationale.contains("Raw engine numbers and engine text"))
    assert(rationale.contains("The LLM is not the intelligence of commentary"))
    assert(architecture.contains("`Board Truth / Primitive Geometry / Story boundary / Verdict boundary`"))
    assert(architecture.contains("Current implementation scope is Stage 1 Board Facts only"))
    assert(architecture.contains("Stages 2-11 below are a"))
    assert(architecture.contains("dependency map for product design"))
    assert(architecture.contains("Only Stage 1 Board Facts is active implementation authority now."))
    assert(architecture.contains("Stages 2-11 are"))
    assert(architecture.contains("dependency-map-only"))
    assert(architecture.contains("`Board Truth` -> `Engine Truth` -> `Primitive Geometry` -> `Tactical/Strategic Story Birth` -> `Engine Validation` -> `Causal Arbitration` -> `Pedagogical Policy` -> `Explanation IR` -> `LLM Narration` -> `Verifier`"))
    assert(!architecture.contains("Tactical/Strategic Hypotheses"))
    Vector(
      "Stage 0 - Closed Kernel",
      "Stage 1 - Board Facts",
      "Stage 2 - Story Proof Sidecar",
      "Stage 3 - First Narrow Positive Story",
      "Stage 4 - Engine Validation",
      "Stage 5 - Causal Arbitration",
      "Stage 6 - Explanation IR",
      "Stage 7 - Deterministic Renderer",
      "Stage 8 - LLM Narration",
      "Stage 9 - Natural-Language Verifier",
      "Stage 10 - Pedagogical Policy",
      "Stage 11 - Personal Learning Loop"
    ).foreach: stage =>
      assert(architecture.contains(stage), s"architecture must pin $stage")
    (1 to 11).foreach: stage =>
      assert(
        architecture.contains(s"Stage $stage depends on Stage ${stage - 1}."),
        s"architecture must pin Stage $stage dependency"
      )
    assert(architecture.contains("Engine lines, mate/tablebase proof, SEE, and bounded material results are"))
    assert(architecture.contains("truth-oracle evidence for backend proof"))
    assert(architecture.contains("Pedagogy is backend policy over selected proof-backed `Verdict` data"))
    assert(architecture.contains("LLM before Explanation IR is forbidden"))
    assert(architecture.contains("Personalization depends on stable Story taxonomy"))
    assert(interactionLaw.contains("Stage 3 opens exactly one narrow proof-backed Story family"))
    assert(interactionLaw.contains("Every blocked Story must report proof deficit"))
    assert(interactionLaw.contains("\"proofCoordinates\":"))
    Vector(
      "\"root\": \"...\"",
      "\"side\": \"...\"",
      "\"target\": \"...\"",
      "\"anchor\": \"...\"",
      "\"route\": \"...\"",
      "\"rival\": \"...\"",
      "\"requiredLegalLine\": \"...\"",
      "\"sameRootProofSidecar\": \"...\""
    ).foreach: coordinate =>
      assert(interactionLaw.contains(coordinate), s"proof-deficit log must include $coordinate")
    assert(interactionLaw.contains("\"missingSidecar\": [\"...\"]"))
    assert(interactionLaw.contains("\"root\": \"current-position-root\""))
    assert(interactionLaw.contains("\"requiredLegalLine\": null"))
    assert(interactionLaw.contains("\"sameRootProofSidecar\": null"))
    assert(interactionLaw.contains("\"missingSidecar\": [\"legal file-entry line\", \"same-root route proof\"]"))
    assert(readme.contains("Forbidden-name no-go"))
    Vector("Semantic", "Candidate", "Certification", "Object", "Delta", "Selector", "Pipeline", "Gate", "ScoreVector").foreach: term =>
      assert(readme.contains(term), s"README must freeze forbidden name $term")
    RetiredRootDocs.foreach: docName =>
      assert(readme.contains(docName), s"README must freeze retired root doc $docName")

  test("agent instructions agree with live authority"):
    assert(Files.exists(agentInstructions), "AGENTS.md must be available from the lila worktree")
    val agents = Files.readString(agentInstructions)
    assert(agents.contains("Live commentary documentation authority is exactly and exhaustively"))
    assert(agents.contains("Any mismatch is a no-go state"))
    LiveDocs.foreach: docName =>
      assert(
        agents.contains(s"modules/commentary/docs/$docName"),
        s"AGENTS.md must list $docName as live authority"
      )
    RetiredRootDocs.foreach: fileName =>
      assert(!agents.contains(s"modules/commentary/docs/$fileName"), s"AGENTS.md must not list $fileName as live authority")
      assert(agents.contains(fileName), s"AGENTS.md must explicitly retire $fileName")
    assert(agents.contains("Public route no-go"))
    assert(agents.contains("No `BoardMood` Sxxx expansion or re-entry"))
    assert(agents.contains("No `Story` proof writers"))
    assert(agents.contains("No renderer opening"))
    assert(agents.contains("Forbidden-name no-go"))
    assert(agents.contains("proof-first chess-story kernel"))
    assert(agents.contains("`BoardMood` observes."))
    assert(agents.contains("feature is not a claim"))
    assert(agents.contains("public `Story` requires proof-bearing identity"))
    assert(agents.contains("Ask whether a feature can become a `Story` with side, target, anchor, route,"))
    assert(agents.contains("`observation` -> `proof sidecar` -> `Story` -> `Verdict` -> `Explanation IR` -> Renderer"))

  test("stage 1 board facts charter keeps observations below public claims"):
    val boardFacts = Files.readString(docsRoot.resolve("BoardFacts.md"))

    assert(boardFacts.contains("Stage 1 name is `Board Facts`."))
    assert(boardFacts.contains("Board state observes. Story proves."))
    assert(boardFacts.contains("Small board facts may be recorded only when they are directly visible from the current board"))
    assert(boardFacts.contains("binds to the same board root"))
    assert(boardFacts.contains("side"))
    assert(boardFacts.contains("piece"))
    assert(boardFacts.contains("square"))
    assert(boardFacts.contains("file"))
    assert(boardFacts.contains("rank"))
    assert(boardFacts.contains("line"))
    assert(boardFacts.contains("legal move"))
    assert(boardFacts.contains("A named board fact is still only an observation."))
    assert(boardFacts.contains("Missing or unproven data stays `0`/silent."))
    assert(boardFacts.contains("Failure logs must say which evidence is missing before any Story can speak."))
    assert(boardFacts.contains("current-board observations through `facts.seen`"))
    assert(boardFacts.contains("Manual or untrusted `BoardFacts` assembly must produce empty `facts.seen`"))
    assert(boardFacts.contains("same-board producer proof"))
    Vector(
      "the knight is free",
      "controls the c-file",
      "the outpost is strategically central",
      "the king is unsafe",
      "this is a good plan",
      "counterplay is stopped"
    ).foreach: claim =>
      assert(boardFacts.contains(claim), s"Board Facts charter must explicitly reject public claim: $claim")
    assert(boardFacts.contains("No renderer, LLM, public route, template, frontend mock, or API transport may read Board Facts directly as commentary."))
    assert(!boardFacts.contains("Stage 1 is commentary"))
    assert(!boardFacts.contains("leadAllowed=true"))

  test("live docs reject legacy candidate selector authority while allowing candidate passer"):
    val contents = liveDocContents
    val forbiddenLiveTerms =
      Vector(
        "CandidateLine",
        "SemanticCandidate",
        "CandidateScoreVector"
      )

    forbiddenLiveTerms.foreach: term =>
      assert(!contents.contains(term), s"$term must not appear in live docs")

    val modelContract = Files.readString(docsRoot.resolve("ChessModelContract.md"))
    assert(modelContract.contains("white_candidate_passer_count"))
    assert(modelContract.contains("black_candidate_passer_count"))
    assert(modelContract.contains("candidate_passer"))
    assert(modelContract.contains("white_break_chance_count"))
    assert(modelContract.contains("black_break_chance_count"))
    assert(modelContract.contains("white_check_threat_count"))
    assert(modelContract.contains("black_check_threat_count"))
    assert(!modelContract.contains("break_candidate_count"))
    assert(!modelContract.contains("check_candidate_count"))

  test("split law maps every split slot to an exact smaller fact"):
    val splitLaw = Files.readString(docsRoot.resolve("BoardMoodSplitLaw.md"))
    val slots = tableSlots(splitLaw)
    val rows = tableRows(splitLaw)

    assertEquals(slots.size, 65)
    assertEquals(slots.distinct.size, 65)
    assertEquals(slots.sorted, SplitSlots.sorted)
    assertEquals(rows.size, 65)
    rows.foreach: row =>
      assertEquals(row.size, 5)
      assert("S\\d{3} `[^`]+`".r.matches(row(0)), s"old slot cell must name one slot: ${row(0)}")
      assert("`[a-z0-9_]+`".r.matches(row(1)), s"fact cell must be one exact fact name: ${row(1)}")
      assert(row(2).nonEmpty, s"derivation rule must not be empty for ${row(0)}")
      assert(row(3).startsWith("No "), s"zero meaning must close the fact: ${row(0)}")
      assert(row(4).startsWith("No "), s"public speech ban must be explicit: ${row(0)}")
    assertEquals(rows.map(_(1)).distinct.size, 65)
    assert(splitLaw.contains("Exact smaller BoardMood fact"))
    assert(splitLaw.contains("Current-board derivation rule"))
    assert(splitLaw.contains("Public speech ban"))
    Vector(
      "piece_role_tally",
      "white_king_ring_attack_map",
      "black_king_ring_attack_map",
      "white_capture_target_map",
      "black_capture_target_map",
      "queenside_minority_lever",
      "candidate_passer_lever",
      "dual_target_defender"
    ).foreach: factName =>
      assert(splitLaw.contains(factName), s"$factName must be named in split law")
    assertNoForbiddenLawTerms(splitLaw)

  test("cut law enumerates every cut slot as silent BoardMood meaning"):
    val cutLaw = Files.readString(docsRoot.resolve("BoardMoodCutLaw.md"))
    val slots = bulletSlots(cutLaw)

    assertEquals(slots.size, 28)
    assertEquals(slots.distinct.size, 28)
    assertEquals(slots.sorted, CutSlots.sorted)
    assert(cutLaw.contains("always `0`/silent"))
    assert(cutLaw.contains("If one of these chess ideas is spoken at all"))
    assert(cutLaw.contains("`StoryResurrectionLaw.md`, not BoardMood"))
    CutSlots.foreach: slot =>
      assert(cutLaw.contains(slot), s"$slot must remain listed in cut law")
    assertNoForbiddenLawTerms(cutLaw)

  test("story resurrection law maps every cut slot to proof-backed Story conditions"):
    val resurrectionLaw = Files.readString(docsRoot.resolve("StoryResurrectionLaw.md"))
    val slots = tableSlots(resurrectionLaw)
    val rows = tableRows(resurrectionLaw)

    assertEquals(slots.size, 28)
    assertEquals(slots.distinct.size, 28)
    assertEquals(slots.sorted, CutSlots.sorted)
    assertEquals(rows.size, 28)
    rows.foreach: row =>
      assertEquals(row.size, 6)
      assert("S\\d{3} `[^`]+`".r.matches(row(0)), s"cut slot cell must name one slot: ${row(0)}")
      assert(row(1).nonEmpty, s"forbidden reason must not be empty for ${row(0)}")
      assert(row(2).nonEmpty, s"resurrection condition must not be empty for ${row(0)}")
      assert(row(3).nonEmpty, s"proof identity must not be empty for ${row(0)}")
      assert(row(4).nonEmpty, s"engine/line rule must not be empty for ${row(0)}")
      assert(row(5).contains("sentence"), s"public sentence limit must be explicit for ${row(0)}")
    assert(resurrectionLaw.contains("No Story resurrection as a chess idea"))
    assert(resurrectionLaw.contains("Every row inherits mandatory Story binding"))
    assert(resurrectionLaw.contains("side, target, anchor, route, rival"))
    assert(resurrectionLaw.contains(HardPublicOutputBlocker.replace(" is a hard public-output block.", " means no public sentence.")))
    assert(resurrectionLaw.contains("A forgeable numeric `Proof` score is not enough."))
    assert(resurrectionLaw.contains("legal checking line, escape-square proof, or engine mate/decisive line"))
    assert(resurrectionLaw.contains("Engine context does not speak by itself"))
    assert(resurrectionLaw.contains("Legal lines must use legal moves from the current position"))
    Vector(
      "S078 `white_mate_net_pressure`",
      "S094 `black_mate_net_pressure`",
      "S215 `plan_trade`",
      "S216 `plan_simplify`",
      "S221 `plan_initiative`",
      "S223 `plan_convert`"
    ).foreach: row =>
      assert(resurrectionLaw.contains(row), s"$row must be governed by resurrection law")
    assertNoForbiddenLawTerms(resurrectionLaw)

  test("story interaction law classifies every upper family and nonlinear rule"):
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))
    val sceneRows = namedRows(interactionLaw, "Scene.")
    val planRows = namedRows(interactionLaw, "Plan.")
    val tacticRows = namedRows(interactionLaw, "Tactic.")
    val proofRows = ProofFields.map(field => namedRows(interactionLaw, field)).flatten
    val ruleRows = namedRows(interactionLaw, "Hard proof blocker") ++
      namedRows(interactionLaw, "Tactical override") ++
      namedRows(interactionLaw, "Same-side tactic priority") ++
      namedRows(interactionLaw, "Source cap") ++
      namedRows(interactionLaw, "Engine cap") ++
      namedRows(interactionLaw, "Board-only cap") ++
      namedRows(interactionLaw, "Blunder override") ++
      namedRows(interactionLaw, "Counterplay cap") ++
      namedRows(interactionLaw, "Quiet fallback") ++
      namedRows(interactionLaw, "Render cap")

    assertEquals(sceneRows.map(_(0)).sorted, Scenes.sorted)
    assertEquals(planRows.map(_(0)).sorted, Plans.sorted)
    assertEquals(tacticRows.map(_(0)).sorted, Tactics.sorted)
    assertEquals(proofRows.map(_(0)).sorted, ProofFields.sorted)
    assertEquals(ruleRows.size, 10)
    val hardProofRows = namedRows(interactionLaw, "Hard proof blocker")
    assertEquals(hardProofRows.size, 1)
    assertEquals(hardProofRows.head, Vector("Hard proof blocker", HardPublicOutputBlocker))
    assert(!interactionLaw.contains("when the corresponding proof claims strength"))
    sceneRows.foreach: row =>
      assertEquals(row.size, 5)
      assert(row(2).nonEmpty && row(3).nonEmpty && row(4).nonEmpty, s"scene row must close support/block/wording: ${row(0)}")
    planRows.foreach: row =>
      assertEquals(row.size, 4)
      assert(row(1).nonEmpty && row(2).nonEmpty && row(3).nonEmpty, s"plan row must close affordance/proof/blockers: ${row(0)}")
    tacticRows.foreach: row =>
      assertEquals(row.size, 4)
      assert(row(2).contains("legal") || row(2).contains("mate"), s"tactic row must require line proof: ${row(0)}")
    proofRows.foreach: row =>
      assertEquals(row.size, 4)
      assert(row(3).nonEmpty, s"proof row must define caps or blockers: ${row(0)}")
    assert(interactionLaw.contains("Opening context with a tactical refutation"))
    assert(interactionLaw.contains("Mate-net shape with legal escape"))
    assert(interactionLaw.contains("Strategic plan with no route"))
    assertNoForbiddenLawTerms(interactionLaw)

  test("agents and active frontend tests reject retired downstream authority"):
    assert(Files.exists(agentInstructions), "AGENTS.md must be available from the lila worktree")
    assert(Files.exists(commentaryBridgeTest), "commentaryBridge.test.ts must remain an active frontend test")

    val agents = Files.readString(agentInstructions)
    assert(!agents.contains("CommentaryOutline"), "AGENTS.md must not grant CommentaryOutline authority")
    assert(!agents.contains("CommentaryPlan"), "AGENTS.md must not grant CommentaryPlan authority")
    assert(agents.contains("selected `Verdict` data only"))

    val bridgeTest = Files.readString(commentaryBridgeTest)
    RetiredRootDocs.foreach: docName =>
      assert(!bridgeTest.contains(docName), s"active commentaryBridge.test.ts must not read retired doc $docName")

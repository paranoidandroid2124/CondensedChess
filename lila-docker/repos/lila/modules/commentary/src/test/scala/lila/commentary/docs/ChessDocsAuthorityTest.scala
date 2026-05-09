package lila.commentary.docs

import java.nio.file.{ Files, Paths }

import scala.jdk.CollectionConverters.*

class ChessDocsAuthorityTest extends munit.FunSuite:

  private val docsRoot = Paths.get("modules/commentary/docs")
  private val agentInstructions = Paths.get("../../..", "AGENTS.md")
  private def agents: String = Files.readString(agentInstructions)
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
  private val moveExplanationTest = Paths.get("ui/analyse/tests/moveExplanation.test.ts")
  private val HardPublicOutputBlocker =
    "Missing side, target, anchor, route, rival, required legal line, or same-root proof sidecar is a hard public-output block."
  private val LineDefenderCloseoutSummary =
    "Line / Defender closeout summaries are non-authoritative: `StoryInteractionLaw.md` owns LNC-0 through LNC-7 and final completion detail, while this document only records that `Tactic.DiscoveredAttack`, `Tactic.Pin`, `Tactic.RemoveGuard`, and `Tactic.Skewer` close as four narrow proof-backed slices with broad line/ray/XRay, pressure, initiative, material-win, public route `200`, production API, and public/user-facing LLM narration surfaces closed."
  private val LineDefenderManifestSummary =
    "Line / Defender Closeout documentation simplification authority lives in `StoryInteractionLaw.md`. Summary documents do not repeat detailed LNC checklists, and legacy broad line/ray/XRay, pressure, initiative, material-win, public route, production API, and public/user-facing LLM paths remain closed."
  private val LineDefenderSummaryForbiddenFragments =
    Vector(
      "Line / Defender Neighborhood Closeout opens no new chess meaning;",
      "LNC-0 confirms",
      "LNC-1 Scope Audit opens",
      "LNC-1 confirms",
      "Closed names are not backlog",
      "LNC-2 Duplication Audit opens",
      "LNC-2 confirms",
      "LNC-3 Authority Audit opens",
      "LNC-3 fixes",
      "LNC-3 forbids",
      "LNC-4 Collision Audit opens",
      "LNC-4 requires",
      "LNC-5 Downstream Boundary Audit opens",
      "LNC-5 forbids",
      "LNC-7 Test Helper / Runtime Boundary Audit opens",
      "LNC Closeout final completion standard:",
      "LNC-4 collision targets:",
      "LNC-4 verification criteria:",
      "LNC-5 downstream authority:",
      "LNC-5 forbidden downstream wording:",
      "LNC-7 runtime boundary:",
      "LNC final verification:"
    )
  private val PawnPromotionCloseoutSummary =
    "Pawn / Promotion closeout summaries are non-authoritative: `StoryInteractionLaw.md` owns PNC-0 through PNC-7 and final completion detail, while this document only records that `Scene.PawnAdvance`, `Scene.PawnStop`, `Scene.PromotionThreat`, and `Scene.Promotion` close as four narrow proof-backed slices; PawnBreak was not opened by PNC and is now governed only by the later PawnBreak-0 charter, while broad PawnTactic, pawn strategy, conversion, winning endgame, tablebase, pawn race, king route, opposition, public route `200`, production API, and public/user-facing LLM narration surfaces remain closed."
  private val PawnPromotionManifestSummary =
    "Pawn / Promotion Closeout documentation simplification authority lives in `StoryInteractionLaw.md`. Summary documents do not repeat detailed PNC checklists; PawnBreak was not opened by PNC and is now governed only by the later PawnBreak-0 charter, while broad PawnTactic, pawn strategy, conversion, winning endgame, tablebase, pawn race, king route, opposition, production API, public route, and public/user-facing LLM paths remain closed."
  private val PawnBreakSummary =
    "PawnBreak-0 summary is non-authoritative: `StoryInteractionLaw.md` owns the Pawn Structure / Break Neighborhood charter; runtime opens only narrow `Scene.PawnBreak` through `PawnBreakProof` for a legal pawn move creating one direct rival-pawn lever on the exact board, with broad PawnTactic, pawn strategy, opens-position, breaks-through, passed-pawn, weakens-structure, wins-space, initiative, pressure, conversion, public route `200`, production API, and public/user-facing LLM narration closed."
  private val PawnCaptureSummary =
    "PawnCapture-0 summary is non-authoritative: `StoryInteractionLaw.md` owns the second Pawn Structure / Break Neighborhood charter; runtime opens only narrow `Scene.PawnCapture` through `PawnCaptureProof` for a legal pawn move capturing one rival pawn on the exact board, with material gain, wins-pawn, passed-pawn, open-file, structure advantage, breakthrough, initiative, pressure, conversion, public route `200`, production API, and public/user-facing LLM narration closed."
  private val PassedPawnCreatedSummary =
    "PassedPawnCreated-0 summary is non-authoritative: `StoryInteractionLaw.md` owns the third Pawn Structure / Break Neighborhood charter; runtime opens only narrow `Scene.PassedPawnCreated` through `PassedPawnCreatedProof` for a legal non-promotion pawn move creating exactly one newly passed pawn on the exact after-board, with promotion threat, actual promotion, unstoppable passer, conversion, winning endgame, pawn race, tablebase, breakthrough, initiative, pressure, public route `200`, production API, and public/user-facing LLM narration closed."
  private val PawnStructureBreakCloseoutSummary =
    "PSBNC-0 summary is non-authoritative: `StoryInteractionLaw.md` owns the Pawn Structure / Break Neighborhood closeout; it opens no new chess meaning and closes only `Scene.PawnBreak`, `Scene.PawnCapture`, and `Scene.PassedPawnCreated` as three narrow proof-backed event slices with separate proof homes and speech keys."
  private val FileOpenedSummary =
    "FileOpened-0 summary is non-authoritative: `StoryInteractionLaw.md` owns the File Opened Neighborhood charter; runtime opens only narrow `Scene.FileOpened` through `FileOpenedProof` for a legal pawn move that leaves its origin file with no pawns on the exact after-board, with rook activity, file control, pressure, initiative, weakness, breakthrough, material gain, passed-pawn creation, pawn-majority change, best/only/forced, public route `200`, production API, and public/user-facing LLM narration closed."
  private val FilePawnStructureCloseoutSummary =
    "FPSNC-0 summary is non-authoritative: `StoryInteractionLaw.md` owns the File / Pawn Structure Neighborhood closeout; it opens no new chess meaning and closes only `Scene.PawnBreak`, `Scene.PawnCapture`, `Scene.PassedPawnCreated`, and `Scene.FileOpened` as four narrow proof-backed event slices with separate proof homes and speech keys; broad pawn-structure and file interpretation remains closed."
  private val FilePawnStructureCloseoutDetailedMarkers =
    Vector(
      "### FPSNC-0 File / Pawn Structure Neighborhood Closeout Charter",
      "FPSNC-0 closes only already-open narrow meanings:",
      "### FPSNC-1 Scope Audit",
      "FPSNC-1 open positive Stories are exactly:",
      "### FPSNC-2 Authority / Duplication Audit",
      "FPSNC-2 duplication audit:",
      "### FPSNC-3 Cross-Slice Collision Fixtures",
      "FPSNC-3 required fixture candidates:",
      "### FPSNC-4 StoryTable Interaction Audit",
      "FPSNC-4 collision targets:",
      "### FPSNC-5 ExplanationPlan Boundary Audit",
      "FPSNC-5 allowed claim keys:",
      "### FPSNC-6 Renderer Boundary Audit",
      "FPSNC-6 allowed bounded templates:",
      "### FPSNC-7 LLM Smoke Boundary Audit",
      "FPSNC-7 forbidden LLM input:",
      "### FPSNC-8 Forbidden Wording Audit",
      "FPSNC-8 forbidden live-authority wording:",
      "### FPSNC-9 Documentation Simplification Audit",
      "FPSNC-9 documentation simplification audit:",
      "FPSNC-9 summary-only documents:",
      "FPSNC-9 forbidden documentation duplication:",
      "### FPSNC-10 Runtime Boundary Audit",
      "FPSNC-10 runtime boundary audit:",
      "FPSNC-10 forbids new runtime authority:",
      "FPSNC-10 allowed closeout changes:",
      "FPSNC-10 runtime source guard:",
      "### FPSNC Final Completion Standard",
      "FPSNC final completion standard:",
      "FPSNC final verification:"
    )
  private val PawnStructureBreakCloseoutDetailedMarkers =
    Vector(
      "### PSBNC-0 Pawn Structure / Break Neighborhood Closeout Charter",
      "PSBNC-0 closes only already-open narrow meanings:",
      "### PSBNC-1 Scope Audit",
      "PSBNC-1 open positive Stories are exactly:",
      "### PSBNC-2 Duplication Audit",
      "PSBNC-2 duplication audit:",
      "### PSBNC-3 StoryTable Interaction Audit",
      "PSBNC-3 target rows:",
      "### PSBNC-4 Downstream Boundary Audit",
      "PSBNC-4 boundary rules:",
      "### PSBNC-5 Forbidden Wording Audit",
      "PSBNC-5 forbidden live-authority wording:",
      "### PSBNC-6 Documentation Simplification Audit",
      "PSBNC-6 documentation simplification audit:",
      "PSBNC-6 summary-only documents:",
      "PSBNC-6 forbidden documentation duplication:"
    )
  private val PawnPromotionSummaryForbiddenFragments =
    Vector(
      "PNC-0 opens only closeout work:",
      "PNC-0 closes only already-open narrow slices:",
      "PNC-0 related proof homes:",
      "PNC-0 opens no:",
      "PNC-0 scope audit:",
      "PNC-0 duplication audit:",
      "PNC-0 authority audit:",
      "PNC-0 collision and downstream audit:",
      "PNC-0 docs simplification:",
      "PNC-0 next-neighborhood handoff:",
      "Completion standard: PNC-0 closes when",
      "PNC-1 confirms:",
      "PNC-1 runtime path audit:",
      "Closed pawn names are not missing features inside this neighborhood.",
      "PNC-2 confirms:",
      "PNC-2 specific duplication checks:",
      "One chess meaning, one proof home, one Story label, one speech key, one live authority document.",
      "PNC-3 layer authority:",
      "PNC-3 forbids:",
      "PNC-3 confirms the authority chain stays:",
      "Completion standard: PNC-3 closes when",
      "PNC-4 collision targets:",
      "PNC-4 runtime checks:",
      "PNC-4 reuses existing runtime tests:",
      "Completion standard: PNC-4 closes when",
      "PNC-5 downstream authority:",
      "PNC-5 non-Lead rows:",
      "PNC-5 forbidden downstream wording:",
      "PNC-5 reuses existing runtime tests:",
      "Completion standard: PNC-5 closes when",
      "PNC-6 documentation simplification:",
      "PNC-6 summary-only documents:",
      "PNC-6 closed-summary wording:",
      "Completion standard: PNC-6 closes when",
      "PNC-7 test helper runtime boundary:",
      "PNC-7 forbids helper promotion:",
      "PNC-7 runtime source guard:",
      "Completion standard: PNC-7 closes when",
      "PNC Closeout final completion standard:",
      "PNC final verification:"
    )

  private def assertLineDefenderCloseoutSummaryDocs(
      readme: String,
      ssot: String,
      architecture: String,
      normalizedModelContract: String,
      legacyManifest: String
  ): Unit =
    Vector(readme, ssot, architecture, normalizedModelContract).foreach: doc =>
      assert(doc.contains(LineDefenderCloseoutSummary))
      LineDefenderSummaryForbiddenFragments.foreach: fragment =>
        assert(
          !doc.contains(fragment),
          s"summary docs must not repeat detailed Line / Defender closeout rule: $fragment"
        )
    assert(legacyManifest.contains(LineDefenderManifestSummary))
    LineDefenderSummaryForbiddenFragments.foreach: fragment =>
      assert(
        !legacyManifest.contains(fragment),
        s"LegacyPruneManifest must not repeat detailed Line / Defender closeout rule: $fragment"
      )

  private def assertPawnPromotionCloseoutSummaryDocs(
      readme: String,
      ssot: String,
      architecture: String,
      normalizedModelContract: String,
      legacyManifest: String
  ): Unit =
    Vector(readme, ssot, architecture, normalizedModelContract).foreach: doc =>
      assert(doc.contains(PawnPromotionCloseoutSummary))
      assert(doc.contains(PawnBreakSummary))
      assert(doc.contains(PawnCaptureSummary))
      assert(doc.contains(PassedPawnCreatedSummary))
      assert(doc.contains(PawnStructureBreakCloseoutSummary))
      assert(doc.contains(FileOpenedSummary))
      assert(doc.contains(FilePawnStructureCloseoutSummary))
      PawnPromotionSummaryForbiddenFragments.foreach: fragment =>
        assert(
          !doc.contains(fragment),
          s"summary docs must not repeat detailed Pawn / Promotion closeout rule: $fragment"
        )
    assert(legacyManifest.contains(PawnPromotionManifestSummary))
    assert(legacyManifest.contains(PawnBreakSummary))
    assert(legacyManifest.contains(PawnCaptureSummary))
    assert(legacyManifest.contains(PassedPawnCreatedSummary))
    assert(legacyManifest.contains(PawnStructureBreakCloseoutSummary))
    assert(legacyManifest.contains(FileOpenedSummary))
    assert(legacyManifest.contains(FilePawnStructureCloseoutSummary))
    PawnPromotionSummaryForbiddenFragments.foreach: fragment =>
      assert(
        !legacyManifest.contains(fragment),
        s"LegacyPruneManifest must not repeat detailed Pawn / Promotion closeout rule: $fragment"
      )

  private def runtimeChessSourceText: String =
    val runtimeRoot = Paths.get("modules/commentary/src/main/scala/lila/commentary/chess")
    val runtimeSourceStream = Files.walk(runtimeRoot)
    try
      runtimeSourceStream
        .iterator()
        .asScala
        .filter(path => Files.isRegularFile(path) && path.toString.endsWith(".scala"))
        .map(Files.readString)
        .mkString("\n")
    finally runtimeSourceStream.close()

  private val SplitSlots =
    Vector(
      "S031",
      "S076",
      "S077",
      "S079",
      "S092",
      "S093",
      "S095",
      "S102",
      "S106",
      "S107",
      "S109",
      "S110",
      "S118",
      "S122",
      "S123",
      "S125",
      "S126",
      "S128",
      "S129",
      "S130",
      "S133",
      "S137",
      "S134",
      "S136",
      "S143",
      "S144",
      "S145",
      "S146",
      "S149",
      "S150",
      "S152",
      "S153",
      "S159",
      "S163",
      "S168",
      "S169",
      "S170",
      "S171",
      "S172",
      "S173",
      "S179",
      "S184",
      "S185",
      "S186",
      "S187",
      "S188",
      "S189",
      "S192",
      "S193",
      "S194",
      "S195",
      "S196",
      "S197",
      "S198",
      "S199",
      "S200",
      "S202",
      "S203",
      "S204",
      "S205",
      "S206",
      "S207",
      "S208",
      "S209",
      "S210",
      "S211",
      "S212",
      "S213",
      "S218"
    )

  private val CutSlots =
    Vector(
      "S013",
      "S014",
      "S072",
      "S078",
      "S088",
      "S094",
      "S104",
      "S111",
      "S120",
      "S127",
      "S131",
      "S142",
      "S147",
      "S158",
      "S164",
      "S174",
      "S175",
      "S180",
      "S190",
      "S191",
      "S201",
      "S214",
      "S215",
      "S216",
      "S217",
      "S219",
      "S220",
      "S221",
      "S222",
      "S223"
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
      "Scene.PawnAdvance",
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
      "Tactic.Pin",
      "Tactic.Skewer",
      "Tactic.Xray",
      "Tactic.Fork",
      "Tactic.DiscoveredAttack",
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

  private val commentaryController = Paths.get("app/controllers/Commentary.scala")
  private val moveExplanationSource = Paths.get("ui/analyse/src/chesstory/moveExplanation.ts")

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
    val allowedFpsncFixtureCandidateText = contents
      .replace("- FileOpened candidate that is only half-open and must stay silent", "")
      .replace("- FileOpened candidate with another pawn still on origin file and must stay silent", "")
    val nonPawnCandidate = """(?i)\bcandidate\b(?!_passer)""".r
    assert(
      nonPawnCandidate.findFirstIn(allowedFpsncFixtureCandidateText).isEmpty,
      "candidate is allowed only as candidate_passer or exact FPSNC-3 FileOpened fixture wording"
    )

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
    assert(ssot.contains("One chess meaning, one home."))
    assert(ssot.contains("One observation family, one owner."))
    assert(ssot.contains("One public claim, one proof path."))
    assert(ssot.contains("Too many small modules and duplicated roles caused authority explosion"))
    assert(ssot.contains("A new type, module, row, or docs-authority name is the last resort"))
    assert(!ssot.contains("CommentaryPipelineInput"))
    assert(!ssot.contains("candidate generation"))
    assert(!ssot.contains("semantic selector"))
    assert(architecture.contains("HCE-style deterministic chess scorer"))
    assert(architecture.contains("`BoardMood` -> `Story` -> `StoryTable` -> `Verdict`"))
    assert(architecture.contains("Before adding a new type, module, row, or observation family"))
    assert(architecture.contains("prefer extending the existing owner over creating a second authority name"))
    assert(architecture.contains("`48` bit slots"))
    assert(architecture.contains("Stage 1 - Board Facts"))
    assert(!architecture.contains("Historical selector-shaped scaffolds may remain"))
    assert(!architecture.contains("gate bits"))
    assert(boardFacts.contains("Stage 1 name is `Board Facts`."))
    assert(boardFacts.contains("Board Facts observes. Story Proof binds. Story may speak only after proof."))
    assert(boardFacts.contains("A named board fact is still only an observation."))
    assert(boardFacts.contains("Open file, pin, weak square, loose piece, and pawn lever"))
    assert(
      boardFacts.contains(
        "No renderer, LLM, public route, template, frontend mock, or API transport may read Board Facts directly as commentary."
      )
    )
    assert(modelContract.contains("Forbidden in new core model names"))
    assert(modelContract.contains("New names are last-resort authority changes"))
    assert(modelContract.contains("ask whether the same chess meaning already has a home"))
    assert(modelContract.contains("ask whether an existing Fact can carry the new field"))
    assert(modelContract.contains("ask whether Story would later have two possible inputs to trust"))
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
    assert(
      modelContract.contains("reflective construction and caller-supplied fields must not create readiness")
    )
    assert(modelContract.contains("`BoardMood.fromPieces` is scaffold-only and not runtime authority"))
    assert(modelContract.contains("BoardFacts required fields"))
    assert(modelContract.contains("Nested BoardFacts facts must be marked `known = true`"))
    Vector(
      "PieceContact",
      "FileFact",
      "LineFact",
      "PawnChallenge",
      "SquareGuardMap",
      "KingSquare",
      "KingRingDefender",
      "ContactCheckObservation",
      "MissingEvidence"
    ).foreach: row =>
      assert(modelContract.contains(s"`$row`"), s"model contract must name BoardFacts row $row")
    Vector(
      "PieceUnderAttack",
      "GuardedPiece",
      "AttackedUnguardedPiece",
      "LoosePieceObservation",
      "OpenFileObservation",
      "SemiOpenFileObservation",
      "RookOpenFileEntry",
      "LineObservation",
      "XRayShape",
      "LineToKing",
      "BlockerNearKing"
    ).foreach: retiredRow =>
      assert(
        !modelContract.contains(s"- `$retiredRow`"),
        s"$retiredRow must not remain a separate admitted BoardFacts row"
      )
    assert(
      modelContract.contains("S015 `position_ready` may be `1` only when all nested facts are known and sane")
    )
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
    assert(splitLaw.contains("The 69 split slots listed here are inactive in live BoardMood"))
    assert(splitLaw.contains("Re-entry is allowed only through the exact smaller BoardMood fact"))
    assert(splitLaw.contains("Public chess speech still belongs to Story only after Story binds side"))
    assert(splitLaw.contains("target, anchor, route, rival, required legal line, and same-root proof"))
    assert(splitLaw.contains("numeric proof scores alone are not enough"))
    assert(
      resurrectionLaw.contains("BoardMood does not create, score, revive, or hint at the meanings below")
    )
    assert(resurrectionLaw.contains("Every row inherits mandatory Story binding"))
    assert(
      resurrectionLaw.contains(
        HardPublicOutputBlocker.replace(" is a hard public-output block.", " means no public sentence.")
      )
    )
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
    val contents =
      Vector(readme, ssot, architecture, modelContract, rationale, interactionLaw, manifest).mkString("\n")
    val normalizedSsot = ssot.replaceAll("\\s+", " ")
    val normalizedInteractionLaw = interactionLaw.replaceAll("\\s+", " ")

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
    assert(
      rationale
        .replaceAll("\\s+", " ")
        .contains(
          "no positive `Story` proof writer beyond `Tactic.Hanging`, the narrow `Tactic.Fork` vertical slice, and the narrow `Scene.Material` writer"
        )
    )
    assert(manifest.contains("default runtime FEN to public"))
    assert(
      modelContract.contains("At this checkpoint no `BoardMood` Sxxx re-entry or proof writer is admitted")
    )
    assert(modelContract.contains("Only the named `Tactic.Hanging` writer, the narrow named `Tactic.Fork`"))
    assert(modelContract.contains("and the narrow named `Scene.Material` writer are live"))
    assert(modelContract.contains("Story owns identity."))
    assert(modelContract.contains("StoryProof owns proof and missing evidence."))
    assert(modelContract.contains("Verdict carries the result."))
    assert(modelContract.contains("StoryProof must not own or duplicate `side`, `target`, `secondaryTarget`"))
    assert(modelContract.contains("`anchor`, `route`, or `rival`."))
    assert(modelContract.contains("`CaptureResult`"))
    assert(modelContract.contains("`EngineEval`"))
    assert(modelContract.contains("`EngineLine`"))
    assert(modelContract.contains("`EngineCheck`"))
    assert(modelContract.contains("`EngineCheckStatus`"))
    assert(modelContract.contains("`StoryWriter`"))
    assert(modelContract.contains("`TacticHanging`"))
    assert(modelContract.contains("Legal line binding is not tactical success proof"))
    assert(
      modelContract.contains(
        "StoryProof is only the minimum evidence form needed before a Story could speak."
      )
    )
    assert(modelContract.contains("`StoryInteractionLaw.md` owns the Stage 3 charter."))
    assert(modelContract.contains("proofFailures are internal diagnostics only."))
    assert(modelContract.contains("proofFailures are not public payload."))
    assert(modelContract.contains("proofFailures are not renderer input."))
    assert(modelContract.contains("proofFailures are not LLM input."))
    assert(modelContract.contains("Missing evidence text must not become user commentary."))
    assert(agents.contains("proofFailures are internal diagnostics only"))
    assert(agents.contains("must not become public JSON, renderer input, or LLM input"))
    val frontendSource = Paths.get("ui/analyse/src")
    val frontendStream = Files.walk(frontendSource)
    val frontendProofFailureReaders =
      try
        frontendStream
          .iterator()
          .asScala
          .filter(path => Files.isRegularFile(path))
          .filter(path => Files.readString(path).contains("proofFailures"))
          .map(_.toString)
          .toVector
          .sorted
      finally frontendStream.close()
    assertEquals(frontendProofFailureReaders, Vector.empty)
    assert(contents.contains("Numeric `Proof` scores may rank blocked/context `Verdict` rows only"))
    assert(contents.contains("cannot set `leadAllowed=true` or produce `Role.Lead`"))
    assert(contents.contains("Runtime `StoryProof` records that full tuple and its missing evidence"))
    assert(!contents.contains("does not yet require target and rival everywhere"))
    assert(!contents.contains("only conditionally required"))
    assert(
      contents.contains("Missing side, target, anchor, route, rival, required legal line, or same-root proof")
    )
    assert(contents.contains("hard public-output block"))
    assert(contents.contains("Old failing tests proved lower/scaffold/"))
    assert(rationale.contains("They did not prove default runtime"))
    assert(rationale.contains("FEN to public `Verdict`"))
    assert(interactionLaw.contains("| Scene.Opening | context-only |"))
    assert(interactionLaw.contains("no truth override or lead over board-backed Story"))
    assert(
      interactionLaw.contains(
        "This diagnostic shape is internal validation, test, and debugging output only."
      )
    )
    assert(
      interactionLaw.contains("It is not API/public JSON, renderer input, LLM input, or user commentary.")
    )
    assert(interactionLaw.contains("it does not provide text that may be spoken."))
    assert(modelContract.contains("`Source` and `Opening` never lead over a board-backed story"))
    val chessFoundationTest =
      Files.readString(
        Paths.get("modules/commentary/src/test/scala/lila/commentary/chess/ChessFoundationTest.scala")
      )
    assert(chessFoundationTest.contains("Verdict proofFailures are internal diagnostics, not public payload"))
    assert(chessFoundationTest.contains("assertEquals(verdict.values, cleared.values)"))
    assert(chessFoundationTest.contains("Stage 2 ordering does not use proofFailures as public sort input"))
    val controllerSource = Files.readString(commentaryController)
    assert(controllerSource.contains("def renderCommentary"))
    assert(controllerSource.contains("def renderLocalProbeCommentary"))
    assert(controllerSource.contains("ServiceUnavailable(unavailable)"))
    assert(controllerSource.contains("\"noCommentary\" -> true"))
    assert(!controllerSource.contains("Ok("))
    val moveExplanation = Files.readString(moveExplanationSource)
    assert(moveExplanation.contains("const emptyState: MoveExplanationState = { kind: 'empty' }"))
    assert(moveExplanation.contains("refresh: () => Promise.resolve()"))
    assert(!moveExplanation.contains("/api/commentary/render"))
    assert(!moveExplanation.contains("/internal/commentary/render-local-probe"))
    assert(!moveExplanation.contains("fetch("))
    assert(readme.contains("Stage order no-go"))
    assert(readme.contains("Line / Ray Slice is a closed baseline."))
    assert(readme.contains("Current implementation scope is Line / Defender Contact Neighborhood."))
    assert(readme.contains("Stage 4 is named `Engine Check`."))
    assert(readme.replaceAll("\\s+", " ").contains("Stages 9-11 remain a dependency map"))
    assert(readme.contains("LLM no-go"))
    assert(readme.contains("LLM narration behavior smoke may rephrase"))
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
    assert(
      ssot.contains(
        "`observation` -> `proof sidecar` -> `Story` -> `Verdict` -> `Explanation IR` -> Renderer -> LLM narration smoke"
      )
    )
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
    assert(
      rationale.contains("A tactic motif, plan affordance, source row, or engine number is not a public")
    )
    assert(rationale.contains("Engine lines, mate/tablebase proof, SEE, and bounded material results are"))
    assert(rationale.contains("Raw engine numbers and engine text"))
    assert(rationale.contains("The LLM is not the intelligence of commentary"))
    assert(
      architecture.contains(
        "`Board Truth / Primitive Geometry / Story boundary / Verdict boundary / Explanation Plan boundary / Deterministic Renderer boundary`"
      )
    )
    assert(architecture.contains("Line / Ray Slice is a closed baseline."))
    assert(architecture.contains("Current implementation scope is Line / Defender Contact Neighborhood."))
    assert(architecture.contains("Stage 4 is named `Engine Check`."))
    assert(architecture.replaceAll("\\s+", " ").contains("Stages 9-11 below"))
    assert(architecture.replaceAll("\\s+", " ").contains("dependency map for product design"))
    assert(
      architecture
        .replaceAll("\\s+", " ")
        .contains(
          "Only Stage 8 Prompt Smoke is active implementation authority"
        )
    )
    assert(architecture.contains("Stages 9-11"))
    assert(architecture.contains("dependency-map-only"))
    assert(
      architecture.contains(
        "`Board Truth` -> `Engine Truth` -> `Primitive Geometry` -> `Tactical/Strategic Story Birth` -> `Engine Check` -> `StoryTable Arbitration` -> `Pedagogical Policy` -> `Explanation IR` -> `LLM Narration` -> `Verifier`"
      )
    )
    assert(!architecture.contains("Tactical/Strategic Hypotheses"))
    Vector(
      "Stage 0 - Closed Kernel",
      "Stage 1 - Board Facts",
      "Stage 2 - Story Proof",
      "Stage 3 - First Narrow Positive Story",
      "Stage 4 - Engine Check",
      "Stage 5 - Story Order",
      "Stage 6 - Explanation Plan (Explanation IR)",
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
    assert(interactionLaw.contains("`CaptureResult` is internal"))
    assert(interactionLaw.contains("side, capturing piece, target piece, legal"))
    assert(interactionLaw.contains("captured value, recapture candidates, material result"))
    assert(interactionLaw.contains("Failed `CaptureResult` rows leave missing evidence and stay silent."))
    assert(interactionLaw.contains("## Stage 4 Charter"))
    assert(interactionLaw.contains("Stage 4 name is `Engine Check`."))
    assert(
      interactionLaw.contains(
        "Story comes first. Engine checks, caps, or refutes. Engine never speaks alone."
      )
    )
    assert(interactionLaw.contains("Stage 4-1 opens only the internal engine evidence shape."))
    assert(
      interactionLaw.contains(
        "Engine eval, engine line, reply line, and checked move data cannot create a Story."
      )
    )
    assert(interactionLaw.contains("Renderer, LLM, and public route `200` remain closed."))
    assert(interactionLaw.contains("Stage 4-2 adds same-board and stale engine guards."))
    assert(
      interactionLaw.contains(
        "Engine evidence must bind to the same board, the same Story route, and the same legal line."
      )
    )
    assert(
      interactionLaw.contains(
        "Different-FEN engine lines, route-mismatched engine lines, stale engine data, depth-missing engine data, eval-only input without a Story, and PV-only input without a Story are diagnostic only."
      )
    )
    assert(interactionLaw.contains("Stage 4-3 attaches EngineCheck only to `Tactic.Hanging`."))
    assert(
      interactionLaw.contains(
        "EngineCheck status starts with exactly `Unknown`, `Supports`, `Caps`, and `Refutes`."
      )
    )
    assert(interactionLaw.contains("`Refutes` blocks the Hanging Story."))
    assert(
      interactionLaw.contains(
        "`Supports` does not mean winning, best move, decisive, PV explanation, or public truth."
      )
    )
    assert(interactionLaw.contains("`Caps` leaves the Story available but forbids strong expression."))
    assert(
      interactionLaw.contains("Stage 4 negative corpus covers local material gain that fails to a larger")
    )
    assert(
      interactionLaw.contains("without `CaptureResult`, engine data without complete StoryProof, and engine")
    )
    assert(interactionLaw.contains("writerless, or proofless engine evidence"))
    assert(interactionLaw.contains("Stage 4 StoryTable integration is conservative."))
    assert(interactionLaw.contains("EngineCheck never creates a Story in StoryTable."))
    assert(interactionLaw.contains("`Caps` records only the internal `engineStrengthLimited`"))
    assert(interactionLaw.contains("Renderer and LLM wording remain"))
    assert(interactionLaw.contains("## Stage 4 Closeout"))
    assert(interactionLaw.contains("One chess meaning, one home. One rule, one live authority."))
    assert(interactionLaw.contains("Stage 4 ends with `Tactic.Hanging` as the only EngineCheck consumer."))
    assert(interactionLaw.contains("`EngineCheck` is not a Story writer and does not create public truth."))
    assert(
      interactionLaw.contains(
        "`StoryInteractionLaw.md` owns this closeout; other live docs may summarize scope only."
      )
    )
    assert(
      interactionLaw.contains(
        "Stage 5 Story Order may receive internal EngineCheck diagnostics from selected"
      )
    )
    assert(
      interactionLaw
        .replaceAll("\\s+", " ")
        .contains(
          "Stage 5 must not open renderer, LLM, public route `200`, `Tactic.Fork`, `Scene.Material`, `Scene.Defense`, Plan, Strategy, engine PV commentary, or best-move explanation."
        )
    )
    assert(interactionLaw.contains("## Stage 5 Charter"))
    assert(interactionLaw.contains("Stage 5 name is `Story Order`."))
    assert(interactionLaw.contains("Stage 5 may be described as `StoryTable Arbitration`"))
    assert(interactionLaw.contains("Core sentence: StoryTable orders. It does not invent."))
    assert(
      interactionLaw.contains("Many Stories may exist. StoryTable chooses roles. No new chess meaning is")
    )
    assert(interactionLaw.contains("Stage 5 first"))
    assert(interactionLaw.contains("scope is limited to the existing `Tactic.Hanging` vertical slice."))
    Vector(
      "- Lead",
      "- Support",
      "- Context",
      "- Blocked",
      "- deterministic ordering",
      "- `Refutes` -> blocked",
      "- `Caps` -> strength-limited diagnostic",
      "- `Supports` -> no new claim",
      "- `Unknown` -> no engine claim"
    ).foreach: allowed =>
      assert(interactionLaw.contains(allowed), s"Stage 5 charter must allow $allowed")
    Vector(
      "- new Story creation",
      "- new positive family",
      "- engine eval as ranking truth",
      "- Board Facts as direct public claim",
      "- `CaptureResult` as public material story",
      "- pedagogical advice",
      "- Explanation IR",
      "- renderer",
      "- LLM",
      "- public route",
      "- `Tactic.Fork`",
      "- `Scene.Material`",
      "- `Scene.Defense`",
      "- Plan / Strategy",
      "- engine PV commentary",
      "- best-move explanation"
    ).foreach: forbidden =>
      assert(interactionLaw.contains(forbidden), s"Stage 5 charter must forbid $forbidden")
    assert(interactionLaw.contains("StoryTable may order only existing Story rows."))
    assert(interactionLaw.contains("A `Refutes` EngineCheck sends the Hanging Story"))
    assert(
      interactionLaw.contains(
        "`StoryInteractionLaw.md` is the single live authority for the Stage 5 charter."
      )
    )
    assert(interactionLaw.contains("Other live documents may summarize Stage 5 scope only."))
    assert(interactionLaw.contains("## Stage 5-1 Hanging Role Rules"))
    assert(interactionLaw.contains("Stage 5-1 goal: assign roles for existing `Tactic.Hanging` Story rows."))
    Vector(
      "complete `Tactic.Hanging` Story, positive `CaptureResult`, and no `Refutes`",
      "exactly one selected Hanging row may be Lead",
      "lower-strength complete Hanging may become Support or Context",
      "`EngineCheck.Refutes` sends Hanging to Blocked",
      "incomplete StoryProof sends Hanging to Blocked",
      "missing `CaptureResult` sends Hanging to Blocked",
      "no writer sends Hanging to Context or Blocked",
      "`EngineCheck.Unknown` creates no engine claim"
    ).foreach: roleRule =>
      assert(interactionLaw.contains(roleRule), s"Stage 5-1 must pin role rule: $roleRule")
    assert(interactionLaw.contains("Support is not yet a public sentence."))
    assert(interactionLaw.contains("Context is not yet a public sentence."))
    assert(interactionLaw.contains("Role assignment does not open renderer or LLM."))
    assert(interactionLaw.contains("## Stage 5-2 Deterministic Ordering"))
    assert(interactionLaw.contains("Stage 5-2 goal: when multiple Story rows exist"))
    Vector(
      "- Story role eligibility",
      "- publicStrength",
      "- scene / tactic identity",
      "- side",
      "- target",
      "- anchor",
      "- route",
      "- writer presence",
      "- blocked status"
    ).foreach: allowed =>
      assert(interactionLaw.contains(allowed), s"Stage 5-2 must allow sort input: $allowed")
    Vector(
      "- raw engine eval",
      "- raw PV text",
      "- proofFailures text",
      "- Board Facts row count",
      "- `CaptureResult` text",
      "- renderer wording"
    ).foreach: forbidden =>
      assert(interactionLaw.contains(forbidden), s"Stage 5-2 must forbid sort input: $forbidden")
    assert(interactionLaw.contains("Input order must not decide Lead."))
    assert(interactionLaw.contains("Equal-strength rows must fall through to"))
    assert(interactionLaw.contains("proofFailures text must not sort public rows."))
    assert(interactionLaw.replaceAll("\\s+", " ").contains("Raw engine eval and raw PV text"))
    assert(interactionLaw.contains("## Stage 5-3 Conflict and Block Rules"))
    assert(
      interactionLaw.contains("Stage 5-3 goal: resolve close blocker relationships for Hanging Story rows")
    )
    Vector(
      "- `EngineCheck.Refutes` blocks Hanging.",
      "- Missing StoryProof blocks Hanging.",
      "- Missing `CaptureResult` blocks Hanging.",
      "- Missing writer blocks Hanging.",
      "- Quiet only if no positive Hanging exists.",
      "- `Scene.Source` and `Scene.Opening` cannot outrank board-backed Hanging."
    ).foreach: allowed =>
      assert(interactionLaw.contains(allowed), s"Stage 5-3 must allow/block only scoped relation: $allowed")
    Vector(
      "- Tactic vs Plan override.",
      "- Blunder override.",
      "- Defense vs Threat relation.",
      "- Counterplay cap beyond existing `EngineCheck.Caps`.",
      "- Strategy suppression."
    ).foreach: notImplemented =>
      assert(interactionLaw.contains(notImplemented), s"Stage 5-3 must keep closed: $notImplemented")
    assert(interactionLaw.contains("Stage 5-3 does not create a Story"))
    assert(interactionLaw.contains("## Stage 5-4 Verdict Diagnostic Boundary"))
    assert(
      interactionLaw.contains(
        "Stage 5-4 goal: keep StoryTable results from being mistaken for renderer or LLM input."
      )
    )
    Vector(
      "- `Verdict.values` shape stays fixed.",
      "- proofFailures do not enter `Verdict.values`.",
      "- EngineCheck diagnostics do not enter `Verdict.values`.",
      "- `engineStrengthLimited` is an internal diagnostic.",
      "- `Verdict` is not public text.",
      "- renderer, LLM, and public route remain closed."
    ).foreach: boundary =>
      assert(interactionLaw.contains(boundary), s"Stage 5-4 must pin diagnostic boundary: $boundary")
    assert(interactionLaw.contains("## Stage 5 Closeout"))
    Vector(
      "Stage 5 closes with Story ordering only.",
      "Explanation IR, renderer, LLM, and pedagogical advice remain closed.",
      "StoryTable creates no chess meaning. It orders existing Stories.",
      "`EngineCheck`, `CaptureResult`, and Board Facts keep their existing homes.",
      "Refuted, incomplete, writerless, captureless, source-only, opening-only, and Quiet fallback rows cannot become Lead over proof-backed Hanging.",
      "No new type, row, or live md authority is introduced by Stage 5 closeout.",
      "Stage 6 handoff receives selected Verdict only.",
      "Stage 6 must not read raw Board Facts, `CaptureResult`, `EngineCheck`, raw engine eval, or raw PV text directly."
    ).foreach: closeoutLine =>
      assert(interactionLaw.contains(closeoutLine), s"Stage 5 closeout must pin: $closeoutLine")
    assert(interactionLaw.contains("## Stage 6 Charter"))
    assert(interactionLaw.contains("Stage 6 name is `Explanation Plan`."))
    assert(interactionLaw.contains("Documents may write `Explanation IR` parenthetically"))
    assert(interactionLaw.contains("Core sentence: Verdict decides. Explanation Plan bounds speech."))
    Vector(
      "Stage 6-0 fixes this charter before renderer or narration work.",
      "The goal is not natural language.",
      "The goal is to receive selected Verdict data and organize claim, evidence, strength, role, support/context relation, and forbidden wording",
      "Explanation Plan must not decide, prove, rank, repair, or invent chess meaning.",
      "Stage 6-0 completion standard: Explanation Plan defines what may be said from the selected Verdict, but it writes no sentence.",
      "`StoryInteractionLaw.md` is the single live authority for the Stage 6 charter.",
      "Other live documents may summarize Stage 6 scope only."
    ).foreach: charterLine =>
      assert(
        normalizedInteractionLaw.contains(charterLine),
        s"StoryInteractionLaw must own Stage 6 charter line: $charterLine"
      )
    Vector(
      "- selected Verdict",
      "- Verdict role",
      "- Verdict strength",
      "- Verdict story identity",
      "- Verdict scene / tactic",
      "- Verdict Lead, Support, Context, or Blocked state"
    ).foreach: allowed =>
      assert(interactionLaw.contains(allowed), s"Stage 6 charter must allow input: $allowed")
    Vector(
      "- raw Board Facts",
      "- raw BoardMood",
      "- root atoms",
      "- `CaptureResult`",
      "- `EngineCheck`",
      "- `EngineEval`",
      "- `EngineLine`",
      "- raw PV",
      "- proofFailures text",
      "- source row",
      "- renderer wording",
      "- LLM wording"
    ).foreach: forbidden =>
      assert(interactionLaw.contains(forbidden), s"Stage 6 charter must forbid input: $forbidden")
    Vector(
      "- deterministic renderer",
      "- LLM narration",
      "- public route `200`",
      "- user-facing prose",
      "- pedagogy",
      "- new Story family",
      "- engine explanation"
    ).foreach: closed =>
      assert(interactionLaw.contains(closed), s"Stage 6 charter must keep closed: $closed")
    val nonCharterStage6Docs =
      Vector(
        "AGENTS.md" -> agents,
        "ChessCommentarySSOT.md" -> ssot,
        "README.md" -> readme,
        "ChessModelArchitecture.md" -> architecture,
        "ChessModelContract.md" -> modelContract,
        "ChessResetRationale.md" -> rationale,
        "LegacyPruneManifest.md" -> manifest
      ).map((name, text) => name -> text.replaceAll("\\s+", " "))
    Vector(
      "Verdict decides. Explanation Plan bounds speech.",
      "Stage 6-0 fixes this charter before renderer or narration work.",
      "The goal is not natural language.",
      "Stage 6-0 completion standard: Explanation Plan defines what may be said from the selected Verdict, but it writes no sentence."
    ).foreach: charterLine =>
      nonCharterStage6Docs.foreach: (name, doc) =>
        assert(
          !doc.contains(charterLine),
          s"$name must not duplicate Stage 6 charter line owned by StoryInteractionLaw: $charterLine"
        )
    Vector(
      "Stage 6-0 opens only the Explanation Plan charter and selected-Verdict speech boundary",
      "Stage 6 is named `Explanation Plan`",
      "selected Verdict data only to bound claim, evidence, strength, role, support/context relation, and forbidden wording",
      "without renderer, LLM, public route, user-facing prose, or pedagogy"
    ).foreach: scopeSummary =>
      assert(
        agents.replaceAll("\\s+", " ").contains(scopeSummary) ||
          ssot.replaceAll("\\s+", " ").contains(scopeSummary) ||
          readme.replaceAll("\\s+", " ").contains(scopeSummary) ||
          modelContract.replaceAll("\\s+", " ").contains(scopeSummary) ||
          manifest.replaceAll("\\s+", " ").contains(scopeSummary),
        s"Stage 6 scope summary must appear in summary docs: $scopeSummary"
      )
    assert(interactionLaw.contains("## Stage 6-1 Explanation Plan Shape"))
    assert(interactionLaw.contains("Stage 6-1 goal: turn one selected Verdict into a small pre-speech plan."))
    assert(interactionLaw.contains("First scope handles exactly one `Tactic.Hanging` Lead Verdict."))
    assert(
      interactionLaw.contains("`allowedClaim` is a structured claim key, not a natural-language sentence.")
    )
    assert(interactionLaw.contains("The first live claim key is `can_win_piece`."))
    assert(interactionLaw.contains("The first live strength key is `bounded`."))
    assert(interactionLaw.contains("support/context links stay empty in the first scope."))
    Vector(
      "- role",
      "- scene",
      "- tactic",
      "- side",
      "- target",
      "- secondaryTarget",
      "- anchor",
      "- route",
      "- allowedClaim",
      "- evidenceLine",
      "- strength",
      "- forbiddenWording",
      "- supportContextLinks"
    ).foreach: field =>
      assert(interactionLaw.contains(field), s"Stage 6-1 must pin ExplanationPlan field: $field")
    Vector(
      "- full sentence generation",
      "- user-facing prose",
      "- `engine says`",
      "- best move",
      "- winning",
      "- decisive",
      "- public eval"
    ).foreach: forbidden =>
      assert(interactionLaw.contains(forbidden), s"Stage 6-1 must keep closed: $forbidden")
    Vector(
      "Stage 6-1 opens only the Explanation Plan shape for one selected `Tactic.Hanging` Lead Verdict",
      "`allowedClaim` stays a structured key such as `can_win_piece`",
      "the first shape carries `bounded` strength and forbidden wording, not public prose"
    ).foreach: scopeSummary =>
      assert(
        agents.replaceAll("\\s+", " ").contains(scopeSummary) ||
          ssot.replaceAll("\\s+", " ").contains(scopeSummary) ||
          readme.replaceAll("\\s+", " ").contains(scopeSummary) ||
          modelContract.replaceAll("\\s+", " ").contains(scopeSummary) ||
          manifest.replaceAll("\\s+", " ").contains(scopeSummary),
        s"Stage 6-1 scope summary must appear in summary docs: $scopeSummary"
      )
    assert(interactionLaw.contains("## Stage 6-2 Tactic.Hanging Allowed Claim Mapping"))
    assert(
      interactionLaw.contains(
        "Stage 6-2 goal: define which claim keys a `Tactic.Hanging` Verdict may lower to."
      )
    )
    Vector(
      "- `can_win_piece`",
      "- `piece_can_be_taken_with_gain`",
      "- `capture_leaves_material_gain`"
    ).foreach: claimKey =>
      assert(interactionLaw.contains(claimKey), s"Stage 6-2 must allow claim key: $claimKey")
    Vector(
      "- `free_piece`",
      "- `blunder`",
      "- `winning_tactic`",
      "- `decisive_tactic`",
      "- `forced_win`",
      "- `best_move`",
      "- `no_counterplay`",
      "- `engine_approved`"
    ).foreach: claimKey =>
      assert(interactionLaw.contains(claimKey), s"Stage 6-2 must forbid claim key: $claimKey")
    Vector(
      "Only uncapped Lead Verdict may carry an allowed claim key.",
      "Support and Context are not standalone claims.",
      "Blocked creates no allowed claim.",
      "`engineStrengthLimited` suppresses allowed claim keys and strengthens forbidden wording."
    ).foreach: boundary =>
      assert(normalizedInteractionLaw.contains(boundary), s"Stage 6-2 must pin claim boundary: $boundary")
    Vector(
      "Stage 6-2 opens only `Tactic.Hanging` allowed claim mapping",
      "Uncapped Lead Verdict only may carry an allowed claim key",
      "Support, Context, Blocked, and engine-capped Verdicts do not create standalone public claims",
      "`engineStrengthLimited` suppresses claim keys and strengthens forbidden wording"
    ).foreach: scopeSummary =>
      assert(
        agents.replaceAll("\\s+", " ").contains(scopeSummary) ||
          ssot.replaceAll("\\s+", " ").contains(scopeSummary) ||
          readme.replaceAll("\\s+", " ").contains(scopeSummary) ||
          modelContract.replaceAll("\\s+", " ").contains(scopeSummary) ||
          manifest.replaceAll("\\s+", " ").contains(scopeSummary),
        s"Stage 6-2 scope summary must appear in summary docs: $scopeSummary"
      )
    assert(interactionLaw.contains("## Stage 6-3 Forbidden Wording Boundary"))
    assert(
      normalizedInteractionLaw.contains(
        "Stage 6-3 goal: Explanation Plan carries forbidden wording that renderer or LLM layers must not say."
      )
    )
    Vector(
      "- `free piece`",
      "- `blunder`",
      "- `winning`",
      "- `decisive`",
      "- `forced`",
      "- `best move`",
      "- `only move`",
      "- `engine says`",
      "- `no counterplay`",
      "- `king unsafe`",
      "- `file control`",
      "- `outpost`",
      "- `strategic key`",
      "- `conversion`",
      "- `mate net`"
    ).foreach: wording =>
      assert(interactionLaw.contains(wording), s"Stage 6-3 must forbid wording: $wording")
    Vector(
      "`Tactic.Hanging` first allowed claim remains bounded material tactic only.",
      "`engineStrengthLimited=true` strengthens the forbidden wording boundary.",
      "`engineStrengthLimited=true` carries no allowed claim key.",
      "Explanation Plan must make forbidden wording clearer than allowed speech."
    ).foreach: boundary =>
      assert(interactionLaw.contains(boundary), s"Stage 6-3 must pin wording boundary: $boundary")
    Vector(
      "Stage 6-3 opens only forbidden wording boundary",
      "Explanation Plan must carry the default forbidden wording set",
      "`Tactic.Hanging` remains bounded material tactic wording only",
      "`engineStrengthLimited` strengthens forbidden wording without carrying a claim"
    ).foreach: scopeSummary =>
      assert(
        agents.replaceAll("\\s+", " ").contains(scopeSummary) ||
          ssot.replaceAll("\\s+", " ").contains(scopeSummary) ||
          readme.replaceAll("\\s+", " ").contains(scopeSummary) ||
          modelContract.replaceAll("\\s+", " ").contains(scopeSummary) ||
          manifest.replaceAll("\\s+", " ").contains(scopeSummary),
        s"Stage 6-3 scope summary must appear in summary docs: $scopeSummary"
      )
    assert(interactionLaw.contains("## Stage 6-4 Support / Context Relation"))
    assert(
      normalizedInteractionLaw.contains(
        "Stage 6-4 goal: carry Support and Context as structure-only relations inside Explanation Plan."
      )
    )
    Vector(
      "- `same_family_lower_rank`",
      "- `alternative_hanging_candidate`",
      "- `capped_same_story`",
      "- `blocked_by_engine_refute`"
    ).foreach: relation =>
      assert(interactionLaw.contains(relation), s"Stage 6-4 must allow relation: $relation")
    Vector(
      "Uncapped Lead only carries an allowed claim.",
      "Support carries relation to Lead only.",
      "Context creates no public claim.",
      "Blocked may enter Explanation Plan only as debug-only relation structure.",
      "proofFailures must not feed Explanation Plan wording or relation text."
    ).foreach: boundary =>
      assert(interactionLaw.contains(boundary), s"Stage 6-4 must pin relation boundary: $boundary")
    Vector(
      "- Support standalone sentence",
      "- Context standalone sentence",
      "- Blocked debug text as user explanation",
      "- proofFailures text as wording"
    ).foreach: forbidden =>
      assert(interactionLaw.contains(forbidden), s"Stage 6-4 must forbid: $forbidden")
    Vector(
      "Stage 6-4 opens only Support and Context relation structure",
      "Uncapped Lead only carries an allowed claim",
      "Support and Context create no standalone public claim",
      "Blocked remains debug-only relation structure",
      "proofFailures do not feed relation wording"
    ).foreach: scopeSummary =>
      assert(
        agents.replaceAll("\\s+", " ").contains(scopeSummary) ||
          ssot.replaceAll("\\s+", " ").contains(scopeSummary) ||
          readme.replaceAll("\\s+", " ").contains(scopeSummary) ||
          modelContract.replaceAll("\\s+", " ").contains(scopeSummary) ||
          manifest.replaceAll("\\s+", " ").contains(scopeSummary),
        s"Stage 6-4 scope summary must appear in summary docs: $scopeSummary"
      )
    assert(interactionLaw.contains("## Stage 6-5 Selected Verdict Only Guard"))
    assert(interactionLaw.contains("Stage 6-5 goal: Explanation Plan receives selected Verdict only."))
    assert(interactionLaw.contains("- selected Verdict only"))
    Vector(
      "- raw BoardFacts",
      "- BoardMood",
      "- root atoms",
      "- MultiTargetProof",
      "- CaptureResult",
      "- EngineCheck",
      "- EngineEval",
      "- EngineLine",
      "- raw PV",
      "- proofFailures text",
      "- unselected Story",
      "- unselected Verdict",
      "- source row"
    ).foreach: forbidden =>
      assert(interactionLaw.contains(forbidden), s"Stage 6-5 must forbid input: $forbidden")
    Vector(
      "Explanation Plan must not expose overloads, constructors, fields, or relation text paths for raw proof material.",
      "It may read only the selected Verdict value and the fields already carried by that Verdict.",
      "Stage 6-5 completion standard: Explanation Plan does not read raw proof material directly.",
      "It creates no chess meaning beyond the selected Verdict."
    ).foreach: boundary =>
      assert(normalizedInteractionLaw.contains(boundary), s"Stage 6-5 must pin selected guard: $boundary")
    Vector(
      "Stage 6-5 opens only the selected Verdict input guard",
      "Explanation Plan accepts selected Verdict only",
      "raw proof material",
      "unselected Story",
      "unselected Verdict",
      "proofFailures wording"
    ).foreach: scopeSummary =>
      assert(
        agents.replaceAll("\\s+", " ").contains(scopeSummary) ||
          ssot.replaceAll("\\s+", " ").contains(scopeSummary) ||
          readme.replaceAll("\\s+", " ").contains(scopeSummary) ||
          modelContract.replaceAll("\\s+", " ").contains(scopeSummary) ||
          manifest.replaceAll("\\s+", " ").contains(scopeSummary),
        s"Stage 6-5 scope summary must appear in summary docs: $scopeSummary"
      )
    assert(interactionLaw.contains("## Stage 6 Closeout"))
    Vector(
      "Stage 6 closes with Explanation Plan only.",
      "Renderer, LLM, public route `200`, user-facing prose, and pedagogy remain closed.",
      "Explanation Plan creates no chess meaning.",
      "StoryTable and Verdict keep selection authority.",
      "`EngineCheck` and `CaptureResult` keep evidence authority.",
      "Blocked, Support, Context, engine-capped, and engine-refuted Verdicts create no allowed claim or public claim.",
      "Stage 7 deterministic renderer may receive Explanation Plan only.",
      "Stage 7 must not read raw Verdict, `EngineCheck`, `CaptureResult`, Board Facts, BoardMood, raw PV, proofFailures text, source rows, or raw engine evidence directly.",
      "One chess meaning, one home.",
      "One rule, one live authority.",
      "Verdict decides. Explanation Plan bounds speech."
    ).foreach: closeoutLine =>
      assert(
        normalizedInteractionLaw.contains(closeoutLine),
        s"Stage 6 closeout must pin: $closeoutLine"
      )
    Vector(
      "Stage 6 closeout confirms Explanation Plan only",
      "Blocked, Support, Context, engine-capped, and engine-refuted Verdicts create no allowed claim",
      "Stage 7 deterministic renderer may receive Explanation Plan only"
    ).foreach: closeoutSummary =>
      assert(
        agents.replaceAll("\\s+", " ").contains(closeoutSummary) ||
          ssot.replaceAll("\\s+", " ").contains(closeoutSummary) ||
          readme.replaceAll("\\s+", " ").contains(closeoutSummary) ||
          modelContract.replaceAll("\\s+", " ").contains(closeoutSummary) ||
          manifest.replaceAll("\\s+", " ").contains(closeoutSummary),
        s"Stage 6 closeout summary must appear in summary docs: $closeoutSummary"
      )
    assert(interactionLaw.contains("## Stage 7-0 Deterministic Renderer Charter"))
    assert(interactionLaw.contains("Stage 7 name is `Deterministic Renderer`."))
    assert(
      interactionLaw.contains("Core sentence: Explanation Plan bounds speech. Renderer only phrases it.")
    )
    Vector(
      "Stage 7-0 fixes what the Deterministic Renderer may open before LLM narration or public route work.",
      "The goal is a deterministic template baseline that turns `ExplanationPlan` into internal text without making chess meaning.",
      "The Deterministic Renderer may phrase only fields already present in the `ExplanationPlan`.",
      "It must not read, recover, repair, or reinterpret raw proof material.",
      "Every deterministic template must pass the forbidden wording boundary before text can leave the renderer boundary.",
      "Stage 7-0 completion standard: Stage 7 charter fixes that Deterministic Renderer creates no chess meaning and phrases Explanation Plan only."
    ).foreach: charterLine =>
      assert(
        normalizedInteractionLaw.contains(charterLine),
        s"Stage 7-0 charter must pin: $charterLine"
      )
    Vector(
      "- `ExplanationPlan` only input",
      "- deterministic template",
      "- `Tactic.Hanging` bounded claim",
      "- forbidden wording check",
      "- no LLM",
      "- no public route"
    ).foreach: allowed =>
      assert(interactionLaw.contains(allowed), s"Stage 7-0 charter must allow: $allowed")
    Vector(
      "- raw Verdict",
      "- Story",
      "- Board Facts",
      "- CaptureResult",
      "- EngineCheck",
      "- EngineEval / EngineLine",
      "- raw PV",
      "- proofFailures text",
      "- source row"
    ).foreach: forbidden =>
      assert(interactionLaw.contains(forbidden), s"Stage 7-0 charter must forbid input: $forbidden")
    Vector(
      "- user-level pedagogy",
      "- best-move wording",
      "- engine-says wording",
      "- winning wording",
      "- decisive wording",
      "- forced wording",
      "- blunder wording",
      "- free-piece wording"
    ).foreach: forbidden =>
      assert(interactionLaw.contains(forbidden), s"Stage 7-0 charter must forbid speech: $forbidden")
    assert(
      normalizedInteractionLaw.contains(
        "LLM narration, public route `200`, pedagogy, new Story families, engine explanation, engine-says wording, best-move explanation, winning, decisive, forced, blunder, and free-piece wording remain closed."
      )
    )
    val nonCharterStage7Docs =
      Vector(
        "AGENTS.md" -> agents,
        "ChessCommentarySSOT.md" -> ssot,
        "README.md" -> readme,
        "ChessModelArchitecture.md" -> architecture,
        "ChessModelContract.md" -> modelContract,
        "ChessResetRationale.md" -> rationale,
        "LegacyPruneManifest.md" -> manifest
      ).map((name, text) => name -> text.replaceAll("\\s+", " "))
    Vector(
      "Explanation Plan bounds speech. Renderer only phrases it.",
      "Stage 7-0 completion standard: Stage 7 charter fixes that Deterministic Renderer creates no chess meaning and phrases Explanation Plan only."
    ).foreach: charterLine =>
      nonCharterStage7Docs.foreach: (name, doc) =>
        assert(
          !doc.contains(charterLine),
          s"$name must not duplicate Stage 7-0 charter line owned by StoryInteractionLaw: $charterLine"
        )
    Vector(
      "Stage 7-0 opens only the Deterministic Renderer charter",
      "`ExplanationPlan` only input",
      "deterministic template",
      "`Tactic.Hanging` bounded claim",
      "forbidden wording check",
      "no LLM",
      "no public route"
    ).foreach: scopeSummary =>
      assert(
        agents.replaceAll("\\s+", " ").contains(scopeSummary) ||
          ssot.replaceAll("\\s+", " ").contains(scopeSummary) ||
          readme.replaceAll("\\s+", " ").contains(scopeSummary) ||
          architecture.replaceAll("\\s+", " ").contains(scopeSummary) ||
          modelContract.replaceAll("\\s+", " ").contains(scopeSummary) ||
          rationale.replaceAll("\\s+", " ").contains(scopeSummary) ||
          manifest.replaceAll("\\s+", " ").contains(scopeSummary),
        s"Stage 7-0 scope summary must appear in summary docs: $scopeSummary"
      )
    assert(interactionLaw.contains("## Stage 7-1 Renderer Input Guard"))
    assert(interactionLaw.contains("Stage 7-1 goal: Renderer receives ExplanationPlan only."))
    Vector(
      "- ExplanationPlan"
    ).foreach: allowed =>
      assert(interactionLaw.contains(allowed), s"Stage 7-1 must allow input: $allowed")
    Vector(
      "- Verdict",
      "- Story",
      "- BoardFacts",
      "- BoardMood",
      "- CaptureResult",
      "- EngineCheck",
      "- EngineEval",
      "- EngineLine",
      "- raw PV",
      "- proofFailures",
      "- source row"
    ).foreach: forbidden =>
      assert(interactionLaw.contains(forbidden), s"Stage 7-1 must forbid input: $forbidden")
    Vector(
      "Renderer must expose no `fromVerdict`, `fromStory`, `fromBoardFacts`, or `fromEngineCheck` path.",
      "Renderer must not create a sentence without an `ExplanationPlan`.",
      "Stage 7-1 completion standard: Renderer does not read proof material directly."
    ).foreach: guardLine =>
      assert(
        normalizedInteractionLaw.contains(guardLine),
        s"Stage 7-1 must pin renderer guard: $guardLine"
      )
    Vector(
      "Player notation boundary:",
      "- SAN formats an already-approved legal move only",
      "- legal `Line` endpoints remain proof binding only",
      "- route SAN is the speech notation carried from Story into ExplanationPlan",
      "- SAN check or mate marks are legal-replay notation only",
      "- SAN does not create Story, Proof, Verdict, ExplanationPlan, or public claims",
      "- Renderer and LLM smoke must not phrase moves as origin-destination routes",
      "- SAN text owns no proof, ranking, or chess meaning by itself"
    ).foreach: notationLine =>
      assert(interactionLaw.contains(notationLine), s"Stage 7 notation boundary must include: $notationLine")
    Vector(
      "Stage 7-1 opens only the Renderer input guard",
      "Renderer receives `ExplanationPlan` only",
      "Renderer exposes no raw Verdict, Story, BoardFacts, BoardMood, CaptureResult, EngineCheck, EngineEval, EngineLine, raw PV, proofFailures, or source-row input"
    ).foreach: scopeSummary =>
      assert(
        agents.replaceAll("\\s+", " ").contains(scopeSummary) ||
          ssot.replaceAll("\\s+", " ").contains(scopeSummary) ||
          readme.replaceAll("\\s+", " ").contains(scopeSummary) ||
          architecture.replaceAll("\\s+", " ").contains(scopeSummary) ||
          modelContract.replaceAll("\\s+", " ").contains(scopeSummary) ||
          rationale.replaceAll("\\s+", " ").contains(scopeSummary) ||
          manifest.replaceAll("\\s+", " ").contains(scopeSummary),
        s"Stage 7-1 scope summary must appear in summary docs: $scopeSummary"
      )
    assert(interactionLaw.contains("## Stage 7-2 Minimal Tactic.Hanging Template"))
    assert(
      interactionLaw.contains(
        "Stage 7-2 goal: turn only the `can_win_piece` claim key into deterministic text."
      )
    )
    Vector(
      "- role is Lead",
      "- allowedClaim is `CanWinPiece`",
      "- strength is `Bounded`",
      "- debugOnly is false",
      "- route exists",
      "- route SAN exists",
      "- target exists",
      "- evidenceLine exists",
      "- forbidden wording set exists"
    ).foreach: condition =>
      assert(interactionLaw.contains(condition), s"Stage 7-2 must require condition: $condition")
    assert(interactionLaw.contains("`dxe5 wins material against the piece on e5.`"))
    Vector(
      "- free piece",
      "- blunder",
      "- winning",
      "- decisive",
      "- forced",
      "- best move",
      "- only move",
      "- engine says",
      "- no counterplay",
      "- king unsafe",
      "- file control",
      "- outpost"
    ).foreach: wording =>
      assert(interactionLaw.contains(wording), s"Stage 7-2 must forbid wording: $wording")
    Vector(
      "Renderer must refuse Support, Context, Blocked, debug-only, missing-route, missing-target, missing-evidenceLine, missing-forbidden-wording, and non-`CanWinPiece` plans.",
      "Stage 7-2 completion standard: the first `Tactic.Hanging` deterministic text does not exceed the ExplanationPlan claim key or evidenceLine."
    ).foreach: templateLine =>
      assert(
        normalizedInteractionLaw.contains(templateLine),
        s"Stage 7-2 must pin template boundary: $templateLine"
      )
    Vector(
      "Stage 7-2 opens only the minimal `Tactic.Hanging` template",
      "`can_win_piece` claim key",
      "does not exceed the ExplanationPlan claim key or evidenceLine"
    ).foreach: scopeSummary =>
      assert(
        agents.replaceAll("\\s+", " ").contains(scopeSummary) ||
          ssot.replaceAll("\\s+", " ").contains(scopeSummary) ||
          readme.replaceAll("\\s+", " ").contains(scopeSummary) ||
          architecture.replaceAll("\\s+", " ").contains(scopeSummary) ||
          modelContract.replaceAll("\\s+", " ").contains(scopeSummary) ||
          rationale.replaceAll("\\s+", " ").contains(scopeSummary) ||
          manifest.replaceAll("\\s+", " ").contains(scopeSummary),
        s"Stage 7-2 scope summary must appear in summary docs: $scopeSummary"
      )
    assert(interactionLaw.contains("## Stage 7-3 Forbidden Wording Enforcement"))
    assert(
      interactionLaw.contains(
        "Stage 7-3 goal: Renderer output must not violate `ExplanationPlan.forbiddenWording`."
      )
    )
    Vector(
      "- free piece",
      "- blunder",
      "- winning as position verdict",
      "- decisive",
      "- forced",
      "- best move",
      "- only move",
      "- engine says",
      "- no counterplay",
      "- king unsafe",
      "- file control",
      "- outpost",
      "- strategic key",
      "- conversion",
      "- mate net"
    ).foreach: wording =>
      assert(interactionLaw.contains(wording), s"Stage 7-3 must enforce forbidden meaning: $wording")
    Vector(
      "Renderer must reject output when any forbidden wording meaning appears in deterministic text.",
      "`win material` wording is allowed only when `allowedClaim` is `CanWinPiece`.",
      "`winning position` remains forbidden.",
      "Engine-strength-limited plans must fail strong wording output.",
      "Plans with no allowedClaim or debugOnly true must produce no output.",
      "Stage 7-3 completion standard: Renderer automatically refuses forbidden wording."
    ).foreach: guardLine =>
      assert(
        normalizedInteractionLaw.contains(guardLine),
        s"Stage 7-3 must pin forbidden wording enforcement: $guardLine"
      )
    Vector(
      "Stage 7-3 opens only forbidden wording enforcement",
      "Renderer output must not violate `ExplanationPlan.forbiddenWording`",
      "`win material` wording is allowed only when `allowedClaim` is `CanWinPiece`"
    ).foreach: scopeSummary =>
      assert(
        agents.replaceAll("\\s+", " ").contains(scopeSummary) ||
          ssot.replaceAll("\\s+", " ").contains(scopeSummary) ||
          readme.replaceAll("\\s+", " ").contains(scopeSummary) ||
          architecture.replaceAll("\\s+", " ").contains(scopeSummary) ||
          modelContract.replaceAll("\\s+", " ").contains(scopeSummary) ||
          rationale.replaceAll("\\s+", " ").contains(scopeSummary) ||
          manifest.replaceAll("\\s+", " ").contains(scopeSummary),
        s"Stage 7-3 scope summary must appear in summary docs: $scopeSummary"
      )
    assert(interactionLaw.contains("## Stage 7-4 No Text for Support / Context / Blocked"))
    assert(
      interactionLaw.contains(
        "Stage 7-4 goal: non-Lead ExplanationPlan rows must not create public text."
      )
    )
    Vector(
      "- Lead with allowedClaim may create text",
      "- Support creates no standalone text",
      "- Context creates no standalone text",
      "- Blocked creates no public text",
      "- debugOnly true creates no public text",
      "- engineStrengthLimited with no allowedClaim creates no public text",
      "- engine-refuted relation creates no public text"
    ).foreach: rule =>
      assert(interactionLaw.contains(rule), s"Stage 7-4 must pin no-text rule: $rule")
    Vector(
      "Renderer must return no text for Support, Context, Blocked, capped no-claim, and engine-refuted relation plans.",
      "Stage 7-4 completion standard: Renderer phrases only Lead plans with an allowed claim."
    ).foreach: noTextLine =>
      assert(
        normalizedInteractionLaw.contains(noTextLine),
        s"Stage 7-4 must pin no-text boundary: $noTextLine"
      )
    Vector(
      "Stage 7-4 opens only the no-standalone-text boundary",
      "Renderer phrases only Lead plans with an allowed claim",
      "Support, Context, Blocked, capped no-claim, and engine-refuted relation plans produce no text"
    ).foreach: scopeSummary =>
      assert(
        agents.replaceAll("\\s+", " ").contains(scopeSummary) ||
          ssot.replaceAll("\\s+", " ").contains(scopeSummary) ||
          readme.replaceAll("\\s+", " ").contains(scopeSummary) ||
          architecture.replaceAll("\\s+", " ").contains(scopeSummary) ||
          modelContract.replaceAll("\\s+", " ").contains(scopeSummary) ||
          rationale.replaceAll("\\s+", " ").contains(scopeSummary) ||
          manifest.replaceAll("\\s+", " ").contains(scopeSummary),
        s"Stage 7-4 scope summary must appear in summary docs: $scopeSummary"
      )
    assert(interactionLaw.contains("## Stage 7-5 Rendered Line Shape"))
    assert(
      interactionLaw.contains(
        "Stage 7-5 goal: deterministic renderer output stays small and verifiable."
      )
    )
    Vector(
      "- text",
      "- claim key",
      "- strength",
      "- forbidden check passed"
    ).foreach: field =>
      assert(interactionLaw.contains(field), s"Stage 7-5 must pin RenderedLine field: $field")
    Vector(
      "- CaptureResult",
      "- EngineCheck",
      "- BoardFacts",
      "- proofFailures",
      "- raw route analysis",
      "- source row"
    ).foreach: forbidden =>
      assert(interactionLaw.contains(forbidden), s"Stage 7-5 must forbid RenderedLine field: $forbidden")
    Vector(
      "`RenderedLine` owns no chess meaning.",
      "`RenderedLine` owns no proof.",
      "`RenderedLine` owns no engine data.",
      "`RenderedLine` is only the expression result of an `ExplanationPlan`.",
      "Stage 7-5 completion standard: RenderedLine is only the expression result of ExplanationPlan."
    ).foreach: shapeLine =>
      assert(
        normalizedInteractionLaw.contains(shapeLine),
        s"Stage 7-5 must pin RenderedLine boundary: $shapeLine"
      )
    Vector(
      "Stage 7-5 opens only the RenderedLine shape",
      "`RenderedLine` owns no chess meaning, proof, or engine data",
      "RenderedLine is only the expression result of ExplanationPlan"
    ).foreach: scopeSummary =>
      assert(
        agents.replaceAll("\\s+", " ").contains(scopeSummary) ||
          ssot.replaceAll("\\s+", " ").contains(scopeSummary) ||
          readme.replaceAll("\\s+", " ").contains(scopeSummary) ||
          architecture.replaceAll("\\s+", " ").contains(scopeSummary) ||
          modelContract.replaceAll("\\s+", " ").contains(scopeSummary) ||
          rationale.replaceAll("\\s+", " ").contains(scopeSummary) ||
          manifest.replaceAll("\\s+", " ").contains(scopeSummary),
        s"Stage 7-5 scope summary must appear in summary docs: $scopeSummary"
      )
    assert(interactionLaw.contains("## Stage 7-6 Renderer Baseline Tests"))
    assert(
      interactionLaw.contains(
        "Stage 7-6 goal: renderer baseline tests prove output is no stronger than ExplanationPlan."
      )
    )
    Vector(
      "- Lead + CanWinPiece + bounded strength renders safe deterministic text",
      "- Support renders no text",
      "- Context renders no text",
      "- Blocked renders no text",
      "- debugOnly renders no text",
      "- no allowedClaim renders no text",
      "- engineStrengthLimited without allowedClaim renders no text",
      "- forbidden wording appearing in output is rejected",
      "- renderer cannot read Verdict directly",
      "- renderer cannot read EngineCheck directly",
      "- renderer cannot mention engine",
      "- renderer cannot say best move, blunder, free piece, decisive, forced, or winning position"
    ).foreach: baseline =>
      assert(interactionLaw.contains(baseline), s"Stage 7-6 must pin baseline test: $baseline")
    Vector(
      "Stage 7-6 opens only renderer baseline tests.",
      "It opens no new renderer wording, no new input, no route, no public route `200`, and no LLM narration.",
      "Stage 7-6 completion standard: Renderer output is no stronger than ExplanationPlan."
    ).foreach: baselineLine =>
      assert(
        normalizedInteractionLaw.contains(baselineLine),
        s"Stage 7-6 must pin baseline boundary: $baselineLine"
      )
    Vector(
      "Stage 7-6 opens only renderer baseline tests",
      "Renderer output is no stronger than ExplanationPlan",
      "no new renderer wording, no new input, no public route `200`, and no LLM narration"
    ).foreach: scopeSummary =>
      assert(
        agents.replaceAll("\\s+", " ").contains(scopeSummary) ||
          ssot.replaceAll("\\s+", " ").contains(scopeSummary) ||
          readme.replaceAll("\\s+", " ").contains(scopeSummary) ||
          architecture.replaceAll("\\s+", " ").contains(scopeSummary) ||
          modelContract.replaceAll("\\s+", " ").contains(scopeSummary) ||
          rationale.replaceAll("\\s+", " ").contains(scopeSummary) ||
          manifest.replaceAll("\\s+", " ").contains(scopeSummary),
        s"Stage 7-6 scope summary must appear in summary docs: $scopeSummary"
      )
    assert(interactionLaw.contains("## Stage 7 Closeout Pass"))
    Vector(
      "Stage 7 closeout goal: audit that deterministic renderer is the only opened Stage 7 surface.",
      "Explanation Plan bounds speech.",
      "Renderer only phrases it.",
      "One rule, one live authority."
    ).foreach: closeoutLine =>
      assert(
        normalizedInteractionLaw.contains(closeoutLine),
        s"Stage 7 closeout must pin core line: $closeoutLine"
      )
    Vector(
      "- only the deterministic renderer is open",
      "- LLM narration remains closed",
      "- public route `200` remains closed",
      "- pedagogy remains closed",
      "- new Story families remain closed"
    ).foreach: scopeAudit =>
      assert(interactionLaw.contains(scopeAudit), s"Stage 7 closeout must pin scope audit: $scopeAudit")
    Vector(
      "- renderer creates no chess meaning",
      "- renderer does not overlap ExplanationPlan ownership",
      "- renderer does not overlap Verdict ownership",
      "- renderer does not overlap StoryTable ownership",
      "- renderer does not overlap EngineCheck ownership",
      "- renderer does not overlap CaptureResult ownership"
    ).foreach: authorityAudit =>
      assert(
        interactionLaw.contains(authorityAudit),
        s"Stage 7 closeout must pin authority audit: $authorityAudit"
      )
    Vector(
      "- Support plans produce no text",
      "- Context plans produce no text",
      "- Blocked plans produce no text",
      "- capped plans produce no text",
      "- refuted plans produce no text",
      "- no-claim plans produce no text"
    ).foreach: negativeAudit =>
      assert(
        interactionLaw.contains(negativeAudit),
        s"Stage 7 closeout must pin negative audit: $negativeAudit"
      )
    Vector(
      "Stage 7 closeout adds no new Story family, row, route, public payload, or markdown authority file.",
      "Renderer rules live in `StoryInteractionLaw.md` only; other documents may summarize scope only.",
      "Stage 8 LLM Narration may receive deterministic text and ExplanationPlan only.",
      "Stage 8 must not read raw Verdict, Story, EngineCheck, CaptureResult, Board Facts, BoardMood, raw PV, proofFailures text, or source rows directly.",
      "Stage 7 closeout completion standard: deterministic renderer is closed as a template baseline, and Stage 8 handoff is bounded to deterministic text plus ExplanationPlan."
    ).foreach: closeoutBoundary =>
      assert(
        normalizedInteractionLaw.contains(closeoutBoundary),
        s"Stage 7 closeout must pin boundary: $closeoutBoundary"
      )
    Vector(
      "Stage 7 Closeout Pass",
      "deterministic renderer is closed as a template baseline",
      "Stage 8 LLM Narration may receive deterministic text and ExplanationPlan only",
      "Stage 8 must not read raw Verdict, Story, EngineCheck, CaptureResult, Board Facts"
    ).foreach: scopeSummary =>
      assert(
        agents.replaceAll("\\s+", " ").contains(scopeSummary) ||
          ssot.replaceAll("\\s+", " ").contains(scopeSummary) ||
          readme.replaceAll("\\s+", " ").contains(scopeSummary) ||
          architecture.replaceAll("\\s+", " ").contains(scopeSummary) ||
          modelContract.replaceAll("\\s+", " ").contains(scopeSummary) ||
          rationale.replaceAll("\\s+", " ").contains(scopeSummary) ||
          manifest.replaceAll("\\s+", " ").contains(scopeSummary),
        s"Stage 7 closeout scope summary must appear in summary docs: $scopeSummary"
      )
    assert(interactionLaw.contains("Stage 7 completion criteria:"))
    Vector(
      "1. Renderer accepts `ExplanationPlan` only.",
      "2. Only Lead `Tactic.Hanging` plans with allowedClaim create deterministic text.",
      "3. Support, Context, Blocked, and debugOnly plans create no text.",
      "4. Forbidden wording boundary violations are rejected.",
      "5. Renderer output is no stronger than ExplanationPlan.",
      "6. Renderer never creates engine, best-move, blunder, decisive, forced, free-piece, or winning-position wording.",
      "7. Public route remains closed.",
      "8. LLM narration remains closed.",
      "9. Stage 8 handoff is limited to ExplanationPlan plus deterministic text.",
      "10. Cleanup pass consolidated duplicate authority and over-documentation."
    ).foreach: criterion =>
      assert(
        normalizedInteractionLaw.contains(criterion),
        s"Stage 7 completion criteria must include: $criterion"
      )
    assert(interactionLaw.contains("## Stage 8 LLM Narration Prompt Smoke"))
    assert(interactionLaw.contains("Stage 8 name is `LLM Narration`."))
    assert(interactionLaw.contains("Core sentence: LLM rephrases. It does not add chess meaning."))
    Vector(
      "Stage 8 opens narration behavior smoke only.",
      "It is not production API validation.",
      "The prompt smoke input should match the production Stage 8 prompt shape as closely as this checkpoint permits.",
      "Stage 8 completion standard: Codex CLI prompt smoke can check narration behavior without opening production API, public route `200`, or new chess meaning."
    ).foreach: stage8Line =>
      assert(
        normalizedInteractionLaw.contains(stage8Line),
        s"Stage 8 must pin smoke boundary: $stage8Line"
      )
    Vector(
      "- 8A Mock narrator",
      "- 8B Codex CLI prompt smoke test",
      "- 8C Production API micro-test remains closed",
      "- 8D Nightly eval remains closed"
    ).foreach: lane =>
      assert(interactionLaw.contains(lane), s"Stage 8 must pin lane: $lane")
    Vector(
      "8A Mock narrator allowed input:",
      "- ExplanationPlan",
      "- RenderedLine",
      "8B Codex CLI prompt smoke allowed input:",
      "- renderedText",
      "- claimKey",
      "- strength",
      "- forbidden wording list",
      "- instruction: \"Rephrase only. Do not add chess facts.\""
    ).foreach: allowed =>
      assert(interactionLaw.contains(allowed), s"Stage 8 must allow input: $allowed")
    Vector(
      "- FEN",
      "- PGN",
      "- engine line",
      "- eval",
      "- CaptureResult",
      "- EngineCheck",
      "- BoardFacts",
      "- raw Verdict",
      "- Story",
      "- BoardMood",
      "- engine eval",
      "- raw PV",
      "- proofFailures",
      "- source row"
    ).foreach: forbidden =>
      assert(interactionLaw.contains(forbidden), s"Stage 8 must forbid input: $forbidden")
    Vector(
      "- new move",
      "- new line",
      "- new tactic",
      "- new plan",
      "- new cause or causal explanation",
      "- new evaluation",
      "- engine mention",
      "- `engine says`",
      "- best move",
      "- forced",
      "- winning",
      "- decisive",
      "- blunder",
      "- free piece",
      "- claim stronger than deterministic text",
      "- chess meaning absent from ExplanationPlan"
    ).foreach: forbidden =>
      assert(interactionLaw.contains(forbidden), s"Stage 8 must forbid output: $forbidden")
    Vector(
      "- same system prompt",
      "- same model / temperature / response format",
      "- stable schema",
      "- forbidden wording checker applied to API output",
      "- cost and latency acceptable",
      "- failure / retry / timeout fail closed"
    ).foreach: apiCheck =>
      assert(
        interactionLaw.contains(apiCheck),
        s"Stage 8 production API closed check must be pinned: $apiCheck"
      )
    Vector(
      "Stage 8 Prompt Smoke",
      "Stage 8 opens only 8A Mock narrator and 8B Codex CLI prompt smoke test",
      "Production API validation remains closed",
      "Stage 8B Codex CLI prompt smoke",
      "forbidden wording checker",
      "Rephrase only. Do not add chess facts."
    ).foreach: scopeSummary =>
      assert(
        agents.replaceAll("\\s+", " ").contains(scopeSummary) ||
          ssot.replaceAll("\\s+", " ").contains(scopeSummary) ||
          readme.replaceAll("\\s+", " ").contains(scopeSummary) ||
          architecture.replaceAll("\\s+", " ").contains(scopeSummary) ||
          modelContract.replaceAll("\\s+", " ").contains(scopeSummary) ||
          rationale.replaceAll("\\s+", " ").contains(scopeSummary) ||
          manifest.replaceAll("\\s+", " ").contains(scopeSummary),
        s"Stage 8 scope summary must appear in summary docs: $scopeSummary"
      )
    Vector(
      "- scope stayed limited to mock narrator plus Codex CLI smoke test.",
      "- production API integration stayed closed.",
      "- raw proof material did not enter narration.",
      "- forbidden wording is rejected.",
      "- new move is rejected.",
      "- new tactic or plan is rejected.",
      "- new cause or evaluation is rejected.",
      "- engine mention is rejected.",
      "- strengthened claim is rejected.",
      "- Stage 8C production API micro-test remains closed"
    ).foreach: closeout =>
      assert(interactionLaw.contains(closeout), s"Stage 8 closeout must include: $closeout")
    Vector(
      "- Deterministic text is the ceiling.",
      "- LLM only polishes below it.",
      "- No raw proof material enters narration."
    ).foreach: closeoutStandard =>
      assert(
        interactionLaw.contains(closeoutStandard),
        s"Stage 8 closeout standard must include: $closeoutStandard"
      )
    assert(modelContract.contains("positive `CaptureResult`"))
    assert(modelContract.contains("EngineCheck is internal evidence only."))
    assert(
      modelContract.contains(
        "It records same-board proof, checked move, engine line, reply line, eval before, eval after, depth or freshness, and missing evidence."
      )
    )
    assert(
      modelContract.contains(
        "EngineCheck does not create a Story, select a Story, rank a Story, write a Verdict, feed a renderer, or feed an LLM."
      )
    )
    assert(
      modelContract.contains(
        "EngineCheck.fromStory binds engine evidence to same-board BoardFacts, an existing Story route, and a same-board legal line."
      )
    )
    assert(
      modelContract.contains("EngineCheckStatus has exactly `Unknown`, `Supports`, `Caps`, and `Refutes`.")
    )
    assert(
      modelContract.contains(
        "Only `Tactic.Hanging`, the narrow `Tactic.Fork` vertical slice, and the narrow `Scene.Material` writer may carry EngineCheck in this checkpoint."
      )
    )
    assert(
      modelContract.contains(
        "Eval collapse after capture or fork route may refute an existing EngineCheck only after same-board Story proof"
      )
    )
    assert(
      modelContract.contains(
        "Eval collapse cannot create a Story, public eval claim, or engine-authored explanation."
      )
    )
    assert(
      modelContract.contains(
        "Verdict carries `engineCheckStatus` and `engineStrengthLimited` as internal diagnostics only."
      )
    )
    assert(
      modelContract.contains(
        "`Verdict.values`, renderer, and LLM inputs must not consume EngineCheck diagnostics."
      )
    )
    assert(modelContract.contains("`StoryInteractionLaw.md` owns the Stage 5 charter."))
    assert(
      modelContract.contains("Stage 5 Story Order baseline is limited to existing `Tactic.Hanging` Story")
    )
    assert(modelContract.contains("The current Fork-6 slice adds only deterministic ordering between"))
    assert(modelContract.contains("existing `Tactic.Hanging` rows and existing narrow `Tactic.Fork` rows"))
    assert(modelContract.contains("Material-3 adds only single-row StoryTable admission for proof-backed"))
    assert(modelContract.contains("Stage 5-1 may mark only the selected Lead row as `leadAllowed`"))
    assert(modelContract.contains("Stage 5-2 ordering may use role eligibility"))
    assert(modelContract.contains("It must not use raw engine eval, raw PV text, proofFailures text"))
    assert(modelContract.contains("Stage 5-3 conflict rules may block Hanging-shaped rows"))
    assert(modelContract.contains("Fork-6 role rules block refuted Fork, incomplete Fork, writerless Fork"))
    assert(
      modelContract.contains("Fork-7 opens only ExplanationPlan mapping for selected narrow `Tactic.Fork`")
    )
    assert(modelContract.contains("Fork allowed claim keys are `forks_two_targets` and"))
    assert(modelContract.contains("`attacks_two_targets`; the first emitted Fork claim key is"))
    assert(modelContract.contains("`forks_two_targets`. Fork plans may carry secondaryTarget"))
    assert(
      modelContract.contains("Support, Context, Blocked, capped, and engine-refuted Fork plans create no")
    )
    assert(
      modelContract
        .replaceAll("\\s+", " ")
        .contains("without opening Plan, Blunder, Defense, extra counterplay, or Strategy relations")
    )
    assert(
      modelContract.contains(
        "Stage 5-4 Verdict Diagnostic Boundary is also owned by `StoryInteractionLaw.md`"
      )
    )
    assert(
      modelContract
        .replaceAll("\\s+", " ")
        .contains("proofFailures, EngineCheck diagnostics, and `engineStrengthLimited` remain internal")
    )
    val nonCharterStage3Docs =
      Vector(
        "AGENTS.md" -> agents,
        "ChessCommentarySSOT.md" -> ssot,
        "README.md" -> readme,
        "ChessModelArchitecture.md" -> architecture,
        "ChessModelContract.md" -> modelContract,
        "ChessResetRationale.md" -> rationale,
        "LegacyPruneManifest.md" -> manifest
      ).map((name, text) => name -> text.replaceAll("\\s+", " "))
    assert(agents.contains("Detailed slice authority lives in `StoryInteractionLaw.md`"))
    assert(agents.contains("add detailed stage rules and closeout criteria only to `StoryInteractionLaw.md`"))
    assert(ssot.contains("`StoryInteractionLaw.md` is the single live authority for the Stage 3 charter."))
    assert(ssot.contains("`StoryInteractionLaw.md` is the single live authority for the Stage 4 charter."))
    assert(ssot.contains("`StoryInteractionLaw.md` is the single live authority for the Stage 5 charter."))
    assert(ssot.contains("`StoryInteractionLaw.md` is the single live authority for the Stage 6 charter."))
    assert(readme.contains("`StoryInteractionLaw.md` owns the Stage 3 charter."))
    assert(readme.contains("`StoryInteractionLaw.md` owns the Stage 4 charter."))
    assert(readme.contains("`StoryInteractionLaw.md` owns the Stage 5 charter."))
    assert(readme.contains("`StoryInteractionLaw.md` owns the Stage 6 charter."))
    assert(architecture.contains("`StoryInteractionLaw.md` owns the Stage 3 charter."))
    assert(architecture.contains("`StoryInteractionLaw.md` owns the Stage 4 charter."))
    assert(architecture.contains("`StoryInteractionLaw.md` owns the Stage 5 charter."))
    assert(modelContract.contains("`StoryInteractionLaw.md` owns the Stage 3 charter."))
    assert(modelContract.contains("`StoryInteractionLaw.md` owns the Stage 4 charter."))
    assert(modelContract.contains("`StoryInteractionLaw.md` owns the Stage 5 charter."))
    assert(modelContract.contains("`StoryInteractionLaw.md` owns the Stage 6 charter."))
    assert(manifest.contains("Stage 3 charter authority lives in `StoryInteractionLaw.md`."))
    assert(manifest.contains("Stage 4 charter authority lives in `StoryInteractionLaw.md`."))
    assert(manifest.contains("Stage 5 charter authority lives in `StoryInteractionLaw.md`."))
    assert(manifest.contains("Stage 6 charter authority lives in `StoryInteractionLaw.md`."))
    assert(ssot.contains("Stage 5-1 Hanging role rules also live in `StoryInteractionLaw.md`"))
    assert(modelContract.contains("Stage 5-1 Hanging Role Rules are also owned by `StoryInteractionLaw.md`"))
    assert(manifest.contains("Stage 5-1 Hanging Role Rules also live in `StoryInteractionLaw.md`"))
    assert(ssot.contains("Stage 5-2 deterministic ordering rules also live in `StoryInteractionLaw.md`"))
    assert(
      modelContract.contains("Stage 5-2 Deterministic Ordering is also owned by `StoryInteractionLaw.md`")
    )
    assert(manifest.contains("Stage 5-2 Deterministic Ordering also lives in `StoryInteractionLaw.md`"))
    assert(ssot.contains("Stage 5-3 conflict and block rules also live in `StoryInteractionLaw.md`"))
    assert(
      modelContract.contains("Stage 5-3 Conflict and Block Rules are also owned by `StoryInteractionLaw.md`")
    )
    assert(manifest.contains("Stage 5-3 Conflict and Block Rules also live in `StoryInteractionLaw.md`"))
    assert(ssot.contains("Stage 5-4 Verdict diagnostic boundary also lives in `StoryInteractionLaw.md`"))
    assert(
      modelContract.contains(
        "Stage 5-4 Verdict Diagnostic Boundary is also owned by `StoryInteractionLaw.md`"
      )
    )
    assert(manifest.contains("Stage 5-4 Verdict Diagnostic Boundary also lives in `StoryInteractionLaw.md`"))
    assert(ssot.contains("Stage 5 closeout also lives in `StoryInteractionLaw.md`"))
    assert(modelContract.contains("Stage 5 closeout is also owned by `StoryInteractionLaw.md`"))
    assert(manifest.contains("Stage 5 closeout also lives in `StoryInteractionLaw.md`"))
    Vector(
      "StoryProof is necessary. A named Story writer gives permission. Family proof gives the reason.",
      "Stage 3 opens exactly one positive Story family at a time.",
      "Complete StoryProof is necessary but not sufficient.",
      "A positive Story requires a named Story writer and family-specific proof.",
      "Opening Tactic.Hanging does not open Fork, Material, Defense, Plan, Strategy, renderer, LLM, or public route."
    ).foreach: charterLine =>
      assert(
        normalizedInteractionLaw.contains(charterLine),
        s"StoryInteractionLaw must own Stage 3 charter line: $charterLine"
      )
      nonCharterStage3Docs.foreach: (name, doc) =>
        assert(
          !doc.contains(charterLine),
          s"$name must not duplicate Stage 3 charter line owned by StoryInteractionLaw: $charterLine"
        )
    assert(agents.contains("Detailed slice authority lives in `StoryInteractionLaw.md`"))
    assert(
      normalizedSsot.contains(
        "Stage 3 opens backend Material proof evidence, the named `Tactic.Hanging` writer, the narrow `Tactic.Fork` proof/writer vertical slice, and the narrow `Scene.Material` writer, while every other positive family, Fork LLM narration, and public route `200` remain closed."
      )
    )
    assert(
      normalizedSsot.contains(
        "Stage 4 opens only `EngineCheck`, `EngineLine`, and `EngineEval` as internal evidence, same-board and stale guards, `Tactic.Hanging` attachment, narrow `Tactic.Fork` attachment, narrow `Scene.Material` EngineCheck reuse, false-positive corpus, and conservative StoryTable diagnostics for existing `Tactic.Hanging`, narrow `Tactic.Fork`, and single proof-backed `Scene.Material` Stories."
      )
    )
    assert(
      normalizedSsot.contains(
        "Stage 4-2 requires engine evidence to bind to the same board, the same Story route, and the same legal line."
      )
    )
    assert(
      normalizedSsot.contains(
        "Stage 4-3 first attaches EngineCheck only to `Tactic.Hanging`; Fork-5 reuses the same sidecar for existing narrow `Tactic.Fork` Stories, with `Unknown`, `Supports`, `Caps`, and `Refutes` as the only statuses."
      )
    )
    Vector(
      "renderer opening outside named ExplanationPlan-only templates",
      "LLM narration",
      "public route `200`",
      "Board Facts direct public claim",
      "Proof score alone as Lead",
      "StoryProof alone as Lead",
      "positive Story families other than `Tactic.Hanging`, the narrow `Tactic.Fork` vertical slice, and the narrow `Scene.Material` writer"
    ).foreach: noGo =>
      assert(
        normalizedInteractionLaw.contains(noGo),
        s"StoryInteractionLaw must pin Stage 3 no-go: $noGo"
      )
    Vector(
      "free piece",
      "blunder",
      "winning",
      "decisive",
      "forced",
      "king unsafe",
      "file control"
    ).foreach: forbiddenWording =>
      assert(
        interactionLaw.contains(forbiddenWording),
        s"StoryInteractionLaw must ban Stage 3 wording: $forbiddenWording"
      )
    assert(normalizedInteractionLaw.contains("Stage 4-1 does not wire StoryTable consumption."))
    val nonCharterStage5Docs =
      Vector(
        "AGENTS.md" -> agents,
        "ChessCommentarySSOT.md" -> ssot,
        "README.md" -> readme,
        "ChessModelArchitecture.md" -> architecture,
        "ChessModelContract.md" -> modelContract,
        "ChessResetRationale.md" -> rationale,
        "LegacyPruneManifest.md" -> manifest
      ).map((name, text) => name -> text.replaceAll("\\s+", " "))
    Vector(
      "StoryTable orders. It does not invent.",
      "Many Stories may exist. StoryTable chooses roles. No new chess meaning is created.",
      "Stage 5 completion standard: when multiple Hanging Story rows exist, StoryTable deterministically decides Lead, Support, Context, and Blocked roles without creating new chess meaning or a new public claim.",
      "Stage 5-1 goal: assign roles for existing `Tactic.Hanging` Story rows.",
      "Support is not yet a public sentence.",
      "Context is not yet a public sentence.",
      "Role assignment does not open renderer or LLM.",
      "Stage 5-2 goal: when multiple Story rows exist, StoryTable always returns the same order for the same rows.",
      "Input order must not decide Lead.",
      "proofFailures text must not sort public rows.",
      "Stage 5-3 goal: resolve close blocker relationships for Hanging Story rows only.",
      "Missing writer blocks Hanging.",
      "Quiet only if no positive Hanging exists.",
      "`Scene.Source` and `Scene.Opening` cannot outrank board-backed Hanging.",
      "Stage 5-4 goal: keep StoryTable results from being mistaken for renderer or LLM input.",
      "`Verdict.values` shape stays fixed.",
      "EngineCheck diagnostics do not enter `Verdict.values`.",
      "`Verdict` is not public text.",
      "Stage 5 closes with Story ordering only.",
      "StoryTable creates no chess meaning. It orders existing Stories.",
      "Stage 6 handoff receives selected Verdict only.",
      "Stage 6 must not read raw Board Facts, `CaptureResult`, `EngineCheck`, raw engine eval, or raw PV text directly."
    ).foreach: charterLine =>
      assert(
        normalizedInteractionLaw.contains(charterLine),
        s"StoryInteractionLaw must own Stage 5 charter line: $charterLine"
      )
      nonCharterStage5Docs.foreach: (name, doc) =>
        assert(
          !doc.contains(charterLine),
          s"$name must not duplicate Stage 5 charter line owned by StoryInteractionLaw: $charterLine"
        )
    Vector(
      "Stage 5 opens only StoryTable role ordering for existing `Tactic.Hanging` Story rows and existing narrow `Tactic.Fork` Story rows",
      "StoryTable may assign roles among existing `Tactic.Hanging` Story rows and existing narrow `Tactic.Fork` Story rows",
      "Stage 5-3 tightens close blockers and context relations",
      "Stage 5-4 keeps Verdict diagnostics out of public numeric values",
      "Stage 5 closeout confirmed Story ordering only"
    ).foreach: scopeSummary =>
      assert(
        normalizedSsot.contains(scopeSummary) ||
          readme.replaceAll("\\s+", " ").contains(scopeSummary) ||
          agents.replaceAll("\\s+", " ").contains(scopeSummary),
        s"Stage 5 scope summary must appear in summary docs: $scopeSummary"
      )
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
    assert(
      interactionLaw.contains("\"missingSidecar\": [\"legal file-entry line\", \"same-root route proof\"]")
    )
    assert(readme.contains("Forbidden-name no-go"))
    Vector(
      "Semantic",
      "Candidate",
      "Certification",
      "Object",
      "Delta",
      "Selector",
      "Pipeline",
      "Gate",
      "ScoreVector"
    ).foreach: term =>
      assert(readme.contains(term), s"README must freeze forbidden name $term")
    RetiredRootDocs.foreach: docName =>
      assert(readme.contains(docName), s"README must freeze retired root doc $docName")

  test("agent instructions agree with live authority"):
    assert(Files.exists(agentInstructions), "AGENTS.md must be available from the lila worktree")
    assert(agents.contains("Live commentary documentation authority is exactly and exhaustively"))
    assert(agents.contains("Any mismatch is a no-go state"))
    LiveDocs.foreach: docName =>
      assert(
        agents.contains(s"modules/commentary/docs/$docName"),
        s"AGENTS.md must list $docName as live authority"
      )
    RetiredRootDocs.foreach: fileName =>
      assert(
        !agents.contains(s"modules/commentary/docs/$fileName"),
        s"AGENTS.md must not list $fileName as live authority"
      )
      assert(agents.contains(fileName), s"AGENTS.md must explicitly retire $fileName")
    assert(agents.contains("Public route no-go"))
    assert(agents.contains("No `BoardMood` Sxxx expansion or re-entry"))
    assert(agents.contains("The active slice and closed baselines are owned by `StoryInteractionLaw.md`."))
    assert(agents.contains("Renderer input is `ExplanationPlan` only."))
    assert(agents.contains("Forbidden-name no-go"))
    assert(agents.contains("proof-first chess-story kernel"))
    assert(agents.contains("Board Facts observes."))
    assert(agents.contains("feature is not a claim"))
    assert(agents.contains("public `Story` requires proof-bearing identity"))
    assert(agents.contains("Authority consolidation is mandatory"))
    assert(agents.contains("`One chess meaning, one home`"))
    assert(agents.contains("`One observation family, one owner`"))
    assert(agents.contains("`One public claim, one proof path`"))
    assert(agents.contains("New names are the last resort"))
    assert(agents.contains("Would a later Story proof know which input to trust?"))
    assert(agents.contains("Player-facing move notation defaults to SAN."))
    assert(agents.contains("SAN formats already-approved legal moves"))
    assert(agents.contains("SAN formats already-approved legal moves only."))
    assert(agents.contains("check or mate marks in SAN do not create"))
    assert(
      agents.contains(
        "`observation` -> `proof sidecar` -> `Story` -> `Verdict` -> `ExplanationPlan` -> Renderer -> LLM smoke"
      )
    )

  test("stage 1 board facts charter keeps observations below public claims"):
    val boardFacts = Files.readString(docsRoot.resolve("BoardFacts.md"))

    assert(boardFacts.contains("Stage 1 name is `Board Facts`."))
    assert(boardFacts.contains("Board Facts observes. Story Proof binds. Story may speak only after proof."))
    assert(
      boardFacts.contains(
        "Small board facts may be recorded only when they are directly visible from the current board"
      )
    )
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
      "Slice 1 - Board Facts index / facts.seen ledger",
      "Slice 2 - Piece facts",
      "Slice 3 - Line facts",
      "Slice 4 - File facts",
      "Slice 5 - Pawn and square facts",
      "Slice 6 - King ring facts and Board Facts closure"
    ).foreach: slice =>
      assert(boardFacts.contains(slice), s"Board Facts charter must pin $slice")
    Vector(
      "free piece",
      "hanging",
      "wins material",
      "can be taken",
      "blunder",
      "tactical target",
      "controls the file",
      "king is unsafe",
      "outpost",
      "pin wins material",
      "x-ray tactic",
      "forced tactic",
      "minority attack",
      "bad structure",
      "permanent weakness",
      "fixed target"
    ).foreach: forbidden =>
      assert(boardFacts.contains(forbidden), s"Board Facts charter must ban $forbidden")
    Vector(
      "`PieceContact` rows",
      "`FileFact` rows",
      "`LineFact` rows",
      "not separate `PieceUnderAttack`",
      "`LoosePieceObservation` rows",
      "not separate `OpenFile`",
      "`FileTargetSquare` rows",
      "not separate `LineObservation`",
      "`BlockerNearKing` rows"
    ).foreach: rowName =>
      assert(
        boardFacts.contains(rowName),
        s"Board Facts charter must document consolidated authority: $rowName"
      )
    Vector(
      "attacked",
      "guarded",
      "attackedUnguarded",
      "unguardedNonPawnNonKing",
      "file state",
      "legal entry moves",
      "ordinary geometry",
      "king-line geometry"
    ).foreach: field =>
      assert(boardFacts.contains(field), s"Board Facts charter must pin consolidated field: $field")
    Vector(
      "does not prove file control",
      "does not prove invasion",
      "does not prove route binding",
      "does not prove plan quality",
      "does not prove an unsafe king",
      "does not prove a mate net",
      "does not prove a pin tactic",
      "does not prove an x-ray tactic"
    ).foreach: ban =>
      assert(boardFacts.contains(ban), s"Board Facts charter must pin public ban: $ban")
    Vector(
      "the knight is free",
      "controls the c-file",
      "the outpost is strategically central",
      "the king is unsafe",
      "this is a good plan",
      "counterplay is stopped"
    ).foreach: claim =>
      assert(boardFacts.contains(claim), s"Board Facts charter must explicitly reject public claim: $claim")
    assert(
      boardFacts.contains(
        "No renderer, LLM, public route, template, frontend mock, or API transport may read Board Facts directly as commentary."
      )
    )
    assert(!boardFacts.contains("Stage 1 is commentary"))
    assert(!boardFacts.contains("leadAllowed=true"))

  test("BoardFacts consolidated row names avoid public-claim vocabulary"):
    val source =
      Files.readString(Paths.get("modules/commentary/src/main/scala/lila/commentary/chess/BoardFacts.scala"))
    val admittedRows = Vector("PieceContact", "FileFact", "LineFact")
    val forbiddenRowVocabulary =
      Vector(
        "Free",
        "Hanging",
        "Wins",
        "MaterialWin",
        "Control",
        "Dominates",
        "Invasion",
        "Decisive",
        "Tactic",
        "Motif",
        "Proof",
        "Claim"
      )

    admittedRows.foreach: row =>
      assert(source.contains(s"final case class $row"), s"BoardFacts source must define $row")
      forbiddenRowVocabulary.foreach: term =>
        assert(!row.contains(term), s"$row must not include public-claim term $term")

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

    assertEquals(slots.size, 69)
    assertEquals(slots.distinct.size, 69)
    assertEquals(slots.sorted, SplitSlots.sorted)
    assertEquals(rows.size, 69)
    rows.foreach: row =>
      assertEquals(row.size, 5)
      assert("S\\d{3} `[^`]+`".r.matches(row(0)), s"old slot cell must name one slot: ${row(0)}")
      assert("`[a-z0-9_]+`".r.matches(row(1)), s"fact cell must be one exact fact name: ${row(1)}")
      assert(row(2).nonEmpty, s"derivation rule must not be empty for ${row(0)}")
      assert(row(3).startsWith("No "), s"zero meaning must close the fact: ${row(0)}")
      assert(row(4).startsWith("No "), s"public speech ban must be explicit: ${row(0)}")
    assertEquals(rows.map(_(1)).distinct.size, 69)
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

    assertEquals(slots.size, 30)
    assertEquals(slots.distinct.size, 30)
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

    assertEquals(slots.size, 30)
    assertEquals(slots.distinct.size, 30)
    assertEquals(slots.sorted, CutSlots.sorted)
    assertEquals(rows.size, 30)
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
    assert(
      resurrectionLaw.contains(
        HardPublicOutputBlocker.replace(" is a hard public-output block.", " means no public sentence.")
      )
    )
    assert(resurrectionLaw.contains("A forgeable numeric `Proof` score is not enough."))
    assert(resurrectionLaw.contains("legal checking line, escape-square proof, or engine mate/decisive line"))
    assert(resurrectionLaw.contains("Engine context does not speak by itself"))
    assert(resurrectionLaw.contains("Legal lines must use legal moves from the current position"))
    Vector(
      "S078 `white_mate_net_pressure`",
      "S072 `white_open_file_exposure`",
      "S088 `black_open_file_exposure`",
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
      assert(
        row(2).nonEmpty && row(3).nonEmpty && row(4).nonEmpty,
        s"scene row must close support/block/wording: ${row(0)}"
      )
    planRows.foreach: row =>
      assertEquals(row.size, 4)
      assert(
        row(1).nonEmpty && row(2).nonEmpty && row(3).nonEmpty,
        s"plan row must close affordance/proof/blockers: ${row(0)}"
      )
    tacticRows.foreach: row =>
      assertEquals(row.size, 4)
      assert(
        row(2).contains("legal") || row(2).contains("mate"),
        s"tactic row must require line proof: ${row(0)}"
      )
    proofRows.foreach: row =>
      assertEquals(row.size, 4)
      assert(row(3).nonEmpty, s"proof row must define caps or blockers: ${row(0)}")
    assert(interactionLaw.contains("Opening context with a tactical refutation"))
    assert(interactionLaw.contains("Mate-net shape with legal escape"))
    assert(interactionLaw.contains("Strategic plan with no route"))
    assertNoForbiddenLawTerms(interactionLaw)

  test("story interaction law carries tactics family width map without opening families"):
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))
    val modelContract = Files.readString(docsRoot.resolve("ChessModelContract.md"))
    val normalizedInteractionLaw = interactionLaw.replaceAll("\\s+", " ")
    val normalizedModelContract = modelContract.replaceAll("\\s+", " ")
    val widthRows =
      interactionLaw.linesIterator
        .filter(line => line.matches("""^\| W\d{2} \|.*"""))
        .map: line =>
          line.stripPrefix("|").stripSuffix("|").split("\\|").toVector.map(_.trim)
        .toVector
    val mappedTactics =
      widthRows.map(row => row(1).stripPrefix("`").stripSuffix("`")).sorted

    assert(interactionLaw.contains("## Tactics Family Width Map"))
    assert(interactionLaw.contains("One tactic name is not one proof system."))
    assert(
      normalizedInteractionLaw.contains(
        "The width map is a proof-home map, not permission to open a new positive family."
      )
    )
    assert(
      interactionLaw.contains(
        "`Tactic.Hanging`, the narrow `Tactic.Fork` vertical slice, the narrow"
      )
    )
    assert(interactionLaw.contains("`Tactic.Pin` writer"))
    assert(interactionLaw.contains("Opening a proof home does not open all tactic names in that home."))
    assert(interactionLaw.contains("## Fork-0 Tactic.Fork Charter"))
    assert(interactionLaw.contains("Fork is a multi-target Story, not a capture Story."))
    assert(interactionLaw.contains("MultiTargetProof gives the reason. TacticFork writer gives permission."))
    assert(interactionLaw.contains("## Fork-1 Geometry Readiness"))
    assert(interactionLaw.contains("post-move attacked target squares"))
    assert(interactionLaw.contains("BoardFacts must not say that a fork works."))
    assert(
      normalizedInteractionLaw.contains(
        "Fork-1 does not create a public Story, Verdict, renderer sentence, LLM narration, public route `200`, or public material claim by itself."
      )
    )
    assert(modelContract.contains("post-move attacked target squares"))
    assert(interactionLaw.contains("## Fork-2 MultiTargetProof"))
    assert(
      interactionLaw.contains(
        "`MultiTargetProof` is the family-specific proof home for the first Fork slice."
      )
    )
    assert(
      interactionLaw.contains("It creates Fork evidence, but it does not directly create a public Story.")
    )
    assert(interactionLaw.contains("target A"))
    assert(interactionLaw.contains("target B"))
    assert(interactionLaw.contains("target value or target class"))
    assert(
      normalizedInteractionLaw.contains(
        "The first Fork proof scope is non-pawn attacker only, preferably knight-shaped, with no pawn fork, no skewer, no queen-hit-only tactic, and no king or mate claim."
      )
    )
    assert(normalizedModelContract.contains("fork square, target A, and target B as proof evidence only"))
    assert(interactionLaw.contains("## Fork-3 TacticFork Writer"))
    assert(
      interactionLaw.contains("`TacticFork` is the named positive writer for the first narrow `Tactic.Fork`")
    )
    assert(interactionLaw.contains("target relation is proven after the move"))
    assert(interactionLaw.contains("EngineCheck does not `Refutes`"))
    assert(
      normalizedInteractionLaw.contains(
        "The first allowed Fork meaning is limited to move attacks two targets and move creates a fork on named targets."
      )
    )
    assert(
      normalizedInteractionLaw.contains(
        "A Fork-looking row with unproven target relation, missing writer, incomplete proof, or EngineCheck Refutes result must not lead."
      )
    )
    assert(
      normalizedModelContract.contains(
        "`StoryWriter.TacticFork` is the named positive Fork writer."
      )
    )
    assert(
      normalizedModelContract.toLowerCase.contains(
        "reply-map entries showing one reply can save both targets block Fork Lead.".toLowerCase
      )
    )
    assert(interactionLaw.contains("## Fork-4 Negative Corpus"))
    assert(interactionLaw.contains("pawn fork trying to enter `Tactic.Fork`"))
    assert(interactionLaw.contains("skewer trying to enter `Tactic.Fork`"))
    assert(interactionLaw.contains("queen-hit-only trying to enter `Tactic.Fork`"))
    assert(interactionLaw.contains("## Fork-5 EngineCheck For Tactic.Fork"))
    assert(interactionLaw.contains("Fork reuses the existing `EngineCheck` sidecar."))
    assert(interactionLaw.contains("There is no `ForkEngineCheck`"))
    assert(interactionLaw.contains("Engine evidence cannot create Fork by itself."))
    assert(
      normalizedInteractionLaw.contains(
        "EngineCheck can support, cap, or refute an existing Fork Story, but engine lines, engine eval, PV-shaped data, missing-depth checks, route-mismatched checks, and engine-only checks cannot create Fork or attach as Fork evidence."
      )
    )
    assert(normalizedModelContract.contains("Fork reuses `EngineCheck`; no `ForkEngineCheck` type exists."))
    assert(interactionLaw.contains("## Fork-6 StoryTable Hanging Vs Fork"))
    assert(
      interactionLaw.contains(
        "Fork-6 opens only StoryTable role ordering for existing `Tactic.Hanging` Story"
      )
    )
    assert(interactionLaw.contains("A refuted Fork, incomplete Fork, writerless Fork, or"))
    assert(interactionLaw.contains("Fork without `MultiTargetProof` becomes `Blocked`."))
    assert(normalizedInteractionLaw.contains("Hanging and Fork may both be eligible for `Lead`"))
    assert(interactionLaw.contains("Support and Context are not sentences."))
    assert(interactionLaw.contains("StoryTable must not create Fork, rank Fork by raw engine eval or raw PV"))
    assert(
      normalizedModelContract.contains(
        "Fork-6 role rules block refuted Fork, incomplete Fork, writerless Fork, and Fork rows without `MultiTargetProof`."
      )
    )
    assert(interactionLaw.contains("## Fork-7 ExplanationPlan For Fork"))
    assert(
      interactionLaw.contains("Fork-7 opens only ExplanationPlan mapping for a selected narrow `Tactic.Fork`")
    )
    assert(interactionLaw.contains("- selected Verdict only"))
    assert(interactionLaw.contains("- MultiTargetProof"))
    assert(interactionLaw.contains("- `forks_two_targets`"))
    assert(interactionLaw.contains("- `attacks_two_targets`"))
    assert(interactionLaw.contains("- `wins_material_by_fork`"))
    assert(interactionLaw.contains("- `wins_queen`"))
    assert(interactionLaw.contains("- `decisive_fork`"))
    assert(interactionLaw.contains("- `forced_win`"))
    assert(interactionLaw.contains("secondaryTarget"))
    assert(interactionLaw.contains("Support, Context, and Blocked Fork Verdicts create no standalone claim"))
    assert(interactionLaw.contains("Fork-7 does not open Fork renderer text"))
    assert(
      interactionLaw.contains(
        "Completion standard: Fork ExplanationPlan creates no meaning stronger than the"
      )
    )
    assert(interactionLaw.contains("## Fork-8 Deterministic Renderer For Fork"))
    assert(interactionLaw.contains("Fork-8 opens only deterministic renderer text for a selected Fork"))
    assert(interactionLaw.contains("- ExplanationPlan only"))
    assert(interactionLaw.contains("`{route} forks the pieces on {targetA} and {targetB}.`"))
    assert(interactionLaw.contains("`targetA` and `targetB` must come from structured `target` and"))
    assert(interactionLaw.contains("- allowed claim is `forks_two_targets`"))
    assert(
      interactionLaw.contains("Support, Context, Blocked, capped, and engine-refuted Fork plans produce no")
    )
    assert(
      interactionLaw.contains("The `attacks_two_targets` claim key remains an internal allowed claim key")
    )
    assert(interactionLaw.contains("Fork-8 itself does not open Fork LLM smoke"))
    assert(
      interactionLaw.contains("Completion standard: Fork renderer text is no stronger than the selected Fork")
    )
    assert(
      normalizedInteractionLaw.contains(
        "Renderer wording opens tactic by tactic only; the current Fork slice opens deterministic Fork renderer text in Fork-8 and Fork LLM smoke in Fork-9."
      )
    )
    assert(
      normalizedModelContract.contains(
        "Fork-8 opens only deterministic renderer text for Fork ExplanationPlan."
      )
    )
    assert(interactionLaw.contains("## Fork-9 LLM Smoke For Fork"))
    assert(
      interactionLaw.contains(
        "Fork-9 opens only LLM smoke for selected Fork ExplanationPlan and RenderedLine."
      )
    )
    Vector(
      "Allowed input:",
      "- ExplanationPlan",
      "- RenderedLine",
      "8B Codex CLI prompt smoke allowed input for Fork:",
      "- renderedText",
      "- claimKey",
      "- strength",
      "- forbidden wording list",
      "- instruction: \"Rephrase only. Do not add chess facts.\""
    ).foreach: allowed =>
      assert(interactionLaw.contains(allowed), s"Fork-9 must allow only smoke input: $allowed")
    Vector(
      "- raw Verdict",
      "- Story",
      "- MultiTargetProof",
      "- EngineCheck",
      "- BoardFacts",
      "- BoardMood",
      "- CaptureResult",
      "- EngineEval",
      "- EngineLine",
      "- engine eval",
      "- raw PV",
      "- proofFailures",
      "- source row"
    ).foreach: forbidden =>
      assert(interactionLaw.contains(forbidden), s"Fork-9 must forbid raw input: $forbidden")
    Vector(
      "- new move",
      "- new line",
      "- new tactic",
      "- new plan",
      "- engine mention",
      "- best move",
      "- forced",
      "- winning",
      "- decisive",
      "- blunder",
      "- wins queen",
      "- wins material",
      "- target piece identity absent from renderedText",
      "- claim stronger than deterministic text"
    ).foreach: forbidden =>
      assert(interactionLaw.contains(forbidden), s"Fork-9 must reject stronger output: $forbidden")
    assert(
      interactionLaw.contains("Completion standard: Fork LLM smoke does not strengthen Fork deterministic")
    )
    assert(
      normalizedModelContract.contains(
        "Fork-9 opens only LLM smoke for selected Fork ExplanationPlan and RenderedLine."
      )
    )
    assert(interactionLaw.contains("## Fork Slice Closeout Pass"))
    Vector(
      "Fork slice closeout goal: audit that the Fork closeout opened only the narrow `Tactic.Fork` vertical slice.",
      "Scope audit:",
      "- opened by Fork closeout: narrow non-pawn `Tactic.Fork` only",
      "- closed: `Tactic.PawnFork`, `Tactic.Skewer`, `Tactic.QueenHit`, `Tactic.Tempo`, `Tactic.InBetween`",
      "- not opened by Fork closeout: `Scene.Material`, `Scene.Defense`, Plan, Strategy",
      "Authority audit:",
      "- MultiTargetProof does not replace CaptureResult.",
      "- MultiTargetProof does not replace StoryProof.",
      "- MultiTargetProof does not replace EngineCheck.",
      "- MultiTargetProof does not replace StoryTable.",
      "Negative corpus audit:",
      "Fork-looking false positives either produce no Story, no Lead, or Blocked.",
      "Cleanup and consolidation:",
      "No new markdown authority file, public row family, public route, production API, or sibling tactic writer opens in Fork closeout.",
      "The proof shape remains reusable for subsequent PawnFork, Skewer, QueenHit, Tempo, or InBetween work only after each family gets its own named writer, negative corpus, EngineCheck rule, StoryTable rule, ExplanationPlan mapping, renderer boundary, and LLM smoke boundary.",
      "Next-stage handoff at Fork closeout:",
      "The next family candidates were `Scene.Material` or `Scene.Defense`.",
      "Fork does not open `Scene.Material` or `Scene.Defense` by implication.",
      "Material-3 separately opens only the narrow named `Scene.Material` writer.",
      "`One tactic name is not one proof system.`",
      "`One proof shape may support multiple tactics.`",
      "`One chess meaning, one home.`"
    ).foreach: closeoutLine =>
      assert(interactionLaw.contains(closeoutLine), s"Fork closeout must pin: $closeoutLine")
    assert(modelContract.contains("Fork slice closeout is also owned by `StoryInteractionLaw.md`"))
    assert(interactionLaw.contains("## Material-0 Scene.Material Charter"))
    Vector(
      "Material is a scene, not a tactic.",
      "`CaptureResult` or `ExchangeResult` is the proof home.",
      "`Scene.Material` is the Story label.",
      "`material_change` is the speech claim.",
      "- simple capture or exchange result",
      "- same-board proof",
      "- legal line",
      "- bounded recapture or exchange check",
      "- known material result",
      "- no tactic label required",
      "- winning",
      "- decisive",
      "- blunder",
      "- conversion",
      "- best move",
      "- forced",
      "- no counterplay",
      "- engine says",
      "- full evaluation claim",
      "Completion standard: `Scene.Material` must not become another name for `Tactic.Hanging` or `CaptureResult`."
    ).foreach: materialCharterLine =>
      assert(interactionLaw.contains(materialCharterLine), s"Material-0 must pin: $materialCharterLine")
    assert(interactionLaw.contains("## Material-1 Proof Home Decision"))
    Vector(
      "Decision: the first `Scene.Material` scope reuses `CaptureResult`.",
      "`CaptureResult is capture proof.`",
      "`ExchangeResult is bounded exchange proof.`",
      "`Scene.Material is not proof.`",
      "Do not create `ExchangeResult` in Material-1.",
      "A new `ExchangeResult` proof home opens only when Material needs a bounded multi-move exchange sequence that `CaptureResult` cannot own without overloading capture meaning.",
      "Completion standard: one material meaning has one proof home."
    ).foreach: materialDecisionLine =>
      assert(interactionLaw.contains(materialDecisionLine), s"Material-1 must pin: $materialDecisionLine")
    assert(interactionLaw.contains("## Material-2 Material / Exchange Proof Shape"))
    Vector(
      "Material-2 opens only the bounded material proof shape.",
      "- side",
      "- legal line",
      "- captured pieces",
      "- recapture candidates",
      "- bounded exchange sequence",
      "- material result",
      "- same-board proof",
      "- missing evidence",
      "- line leaves White up material",
      "- line leaves Black up material",
      "- exchange result is known",
      "- winning position",
      "- decisive advantage",
      "- conversion",
      "- blunder",
      "- best move",
      "- forced line",
      "Completion standard: the proof calculates material result, but it does not create a public Story or sentence."
    ).foreach: materialProofLine =>
      assert(interactionLaw.contains(materialProofLine), s"Material-2 must pin: $materialProofLine")
    assert(interactionLaw.contains("## Material-3 Scene.Material Writer"))
    Vector(
      "Material-3 opens the named `SceneMaterial` writer only.",
      "- scene is `Scene.Material`",
      "- StoryProof is complete",
      "- material proof is complete",
      "- same-board proof is present",
      "- legal line is present",
      "- material result is known",
      "- writer is `StoryWriter.SceneMaterial`",
      "- EngineCheck does not `Refutes`",
      "- this line changes material balance",
      "- this exchange leaves one side ahead in material",
      "- winning",
      "- decisive",
      "- blunder",
      "- conversion",
      "- best move",
      "- forced",
      "- no counterplay",
      "- engine says",
      "Completion standard: one narrow Material Story with proof can enter StoryTable."
    ).foreach: materialWriterLine =>
      assert(interactionLaw.contains(materialWriterLine), s"Material-3 must pin: $materialWriterLine")
    assert(interactionLaw.contains("## Material-4 Material Negative Corpus"))
    Vector(
      "Material-4 opens only the Material negative corpus.",
      "- legal line missing",
      "- same-board proof missing",
      "- capture exists but bounded recapture erases the material result",
      "- exchange result unclear",
      "- target is king",
      "- material result is zero",
      "- EngineCheck `Refutes`",
      "- StoryProof incomplete",
      "- material proof incomplete",
      "- tactic writer tries to speak Material",
      "- Hanging tries to auto-duplicate as Material",
      "- Fork tries to auto-duplicate as Material",
      "- high Proof score only",
      "Completion standard: material-looking rows without bounded material proof become no Lead or Blocked."
    ).foreach: materialNegativeLine =>
      assert(interactionLaw.contains(materialNegativeLine), s"Material-4 must pin: $materialNegativeLine")
    assert(interactionLaw.contains("## Material-5 EngineCheck for Scene.Material"))
    Vector(
      "Material-5 opens only existing `EngineCheck` reuse for `Scene.Material`.",
      "- `Unknown`",
      "- `Supports`",
      "- `Caps`",
      "- `Refutes`",
      "- Material Story already exists",
      "- same-board proof",
      "- same Story route",
      "- same legal line",
      "- fresh or depth evidence",
      "- engine creates Material Story",
      "- engine eval becomes public truth",
      "- PV becomes explanation",
      "- best move explanation",
      "- winning claim",
      "- `MaterialEngineCheck` duplicate type",
      "Completion standard: `EngineCheck` may support, cap, or refute an existing Material Story, but it must not create Material."
    ).foreach: materialEngineLine =>
      assert(interactionLaw.contains(materialEngineLine), s"Material-5 must pin: $materialEngineLine")
    assert(interactionLaw.contains("## Material-6 StoryTable Integration"))
    Vector(
      "Material-6 opens only StoryTable integration for existing Hanging, Fork, and Material rows.",
      "- Lead",
      "- Support",
      "- Context",
      "- Blocked",
      "- deterministic ordering",
      "- Refuted Material becomes Blocked",
      "- incomplete Material becomes Blocked",
      "- writerless Material becomes Blocked",
      "- Material without material proof becomes Blocked",
      "- Hanging, Fork, and Material can compete for Lead",
      "- Material with the same route, target, and material result as positive Hanging orders behind Hanging",
      "- Support and Context are not sentences",
      "- StoryTable creates Material",
      "- raw engine eval ranks Material",
      "- material proof text becomes public",
      "- renderer wording affects order",
      "- Material silently opens conversion or winning",
      "Completion standard: StoryTable deterministically orders the three Story families without creating new chess meaning."
    ).foreach: materialStoryTableLine =>
      assert(interactionLaw.contains(materialStoryTableLine), s"Material-6 must pin: $materialStoryTableLine")
    assert(interactionLaw.contains("## Material-7 ExplanationPlan for Scene.Material"))
    Vector(
      "Material-7 opens only ExplanationPlan mapping for selected `Scene.Material` Verdicts.",
      "Allowed Material-7 input:",
      "- selected Verdict only",
      "Forbidden Material-7 inputs:",
      "- material proof directly",
      "- `CaptureResult`",
      "- `ExchangeResult`",
      "- `EngineCheck`",
      "- `BoardFacts`",
      "- raw PV",
      "- proofFailures",
      "- source row",
      "Allowed Material-7 claim keys:",
      "- `material_balance_changes`",
      "- `line_leaves_material_gain`",
      "- `exchange_leaves_side_ahead`",
      "The first emitted Material claim key is `material_balance_changes`.",
      "Forbidden Material-7 claim keys:",
      "- `winning_position`",
      "- `decisive_advantage`",
      "- `conversion`",
      "- `blunder`",
      "- `best_move`",
      "- `forced_win`",
      "- `no_counterplay`",
      "Support, Context, Blocked, capped, and engine-refuted Material plans create no standalone public claim.",
      "Material-7 does not open Material renderer text",
      "Completion standard: Material ExplanationPlan creates no meaning stronger than the selected Verdict."
    ).foreach: materialPlanLine =>
      assert(interactionLaw.contains(materialPlanLine), s"Material-7 must pin: $materialPlanLine")
    assert(
      normalizedModelContract.contains("Material-0 and Material-1 are owned by `StoryInteractionLaw.md`.")
    )
    assert(
      normalizedModelContract.contains(
        "Material-1 reuses `CaptureResult` for the first simple capture and immediate bounded recapture scope."
      )
    )
    assert(
      normalizedModelContract.contains(
        "Material-2 extends `CaptureResult` with captured pieces and bounded exchange sequence proof fields."
      )
    )
    assert(
      normalizedModelContract.contains(
        "Material-3 opens `StoryWriter.SceneMaterial` as the named Material writer."
      )
    )
    assert(normalizedModelContract.contains("Material-4 adds only the Material negative corpus."))
    assert(normalizedModelContract.contains("Material-5 reuses existing `EngineCheck` for `Scene.Material`."))
    assert(
      normalizedModelContract.contains(
        "Material-6 adds only StoryTable integration for existing Hanging, Fork, and Material rows."
      )
    )
    assert(
      normalizedModelContract.contains(
        "Material-7 opens only ExplanationPlan mapping for selected `Scene.Material` Verdicts."
      )
    )
    assert(
      normalizedModelContract.contains(
        "Material allowed claim keys are `material_balance_changes`, `line_leaves_material_gain`, and `exchange_leaves_side_ahead`; the first emitted Material claim key is `material_balance_changes`."
      )
    )
    assert(interactionLaw.contains("## Material-8 Deterministic Renderer"))
    Vector(
      "Material-8 opens only deterministic renderer text for selected `Scene.Material` ExplanationPlan.",
      "Allowed Material-8 input:",
      "- ExplanationPlan only",
      "First Material-8 templates:",
      "`This line leaves White ahead in material.`",
      "`After {route}, White comes out ahead in material.`",
      "Forbidden Material-8 wording:",
      "- winning",
      "- decisive",
      "- blunder",
      "- forced",
      "- best move",
      "- no counterplay",
      "- engine says",
      "- conversion",
      "- technically winning",
      "Material-8 does not open LLM smoke",
      "Completion standard: Renderer text is no stronger than the Material ExplanationPlan."
    ).foreach: materialRendererLine =>
      assert(interactionLaw.contains(materialRendererLine), s"Material-8 must pin: $materialRendererLine")
    assert(
      normalizedModelContract.contains(
        "Material-8 opens only deterministic renderer text for selected `Scene.Material` ExplanationPlan."
      )
    )
    assert(interactionLaw.contains("## Material-9 LLM Smoke"))
    Vector(
      "Material-9 opens only LLM smoke for selected Material ExplanationPlan and RenderedLine.",
      "Allowed Material-9 input:",
      "- ExplanationPlan",
      "- RenderedLine",
      "8B Material Codex CLI input:",
      "- renderedText",
      "- claimKey",
      "- strength",
      "- forbidden wording",
      "- instruction: Rephrase only. Do not add chess facts.",
      "Forbidden Material-9 input:",
      "- raw Verdict",
      "- Story",
      "- material proof",
      "- CaptureResult",
      "- ExchangeResult",
      "- EngineCheck",
      "- BoardFacts",
      "- engine eval",
      "- raw PV",
      "- proofFailures",
      "Material LLM smoke must reject output that adds:",
      "- new move",
      "- new line",
      "- new tactic",
      "- new plan",
      "- engine mention",
      "- winning, decisive, forced, blunder, or best-move wording",
      "- conversion claim",
      "- stronger claim",
      "Material-9 does not open public/user-facing LLM narration",
      "Completion standard: LLM smoke does not strengthen Material text."
    ).foreach: materialSmokeLine =>
      assert(interactionLaw.contains(materialSmokeLine), s"Material-9 must pin: $materialSmokeLine")
    assert(
      normalizedModelContract.contains(
        "Material-9 opens only LLM smoke for selected Material ExplanationPlan and RenderedLine."
      )
    )
    assert(interactionLaw.contains("## Material Slice Closeout Pass"))
    Vector(
      "Material slice closeout opens no new chess meaning beyond the narrow `Scene.Material` vertical slice.",
      "Scope audit:",
      "- opened: `Scene.Material` only",
      "- still closed: `Scene.Defense`",
      "- still closed: Plan",
      "- still closed: Strategy",
      "- still closed: Conversion",
      "- still closed: Blunder",
      "Authority audit:",
      "- `CaptureResult` owns simple capture and immediate bounded recapture material proof.",
      "- `ExchangeResult` remains unopened and is reserved for bounded multi-move exchange proof outside this slice if needed.",
      "- `StoryProof` owns identity completeness, same-board proof, and legal-line binding.",
      "- `EngineCheck` supports, caps, or refutes only an existing Material Story.",
      "- `StoryTable` orders existing Material rows but creates no Material Story or material proof.",
      "- `Scene.Material` owns the Story label only.",
      "- `material_change` is speech-claim vocabulary; current emitted key is `material_balance_changes`.",
      "Negative corpus audit:",
      "- material-looking false positives produce no Lead or Blocked.",
      "- high Proof score alone remains insufficient.",
      "Cleanup and consolidation audit:",
      "- no `ExchangeResult` type was created in this slice.",
      "- no `MaterialEngineCheck` type was created.",
      "- Material proof text does not become renderer or LLM input.",
      "Shared skeleton audit:",
      "- Material reuses proof home -> Story writer -> EngineCheck -> StoryTable -> ExplanationPlan -> Renderer -> LLM smoke.",
      "- Reuse the skeleton before adding a new one.",
      "- no second Story writer path, EngineCheck type, StoryTable route, ExplanationPlan input, renderer input, or LLM prompt shape was added.",
      "- if `ExchangeResult` opens in its own slice, it must state how bounded multi-move exchange proof differs from `CaptureResult`.",
      "Next-stage handoff:",
      "- next named slice remains `Scene.Defense`.",
      "- Material does not open Defense, Conversion, Winning, Plan, Strategy, or Blunder.",
      "`Material is a scene, not a tactic.`",
      "`Material gain is not winning.`",
      "`One chess meaning, one home.`",
      "Completion standard: Material slice is closed as a narrow bounded material-result Story label."
    ).foreach: materialCloseoutLine =>
      assert(
        interactionLaw.contains(materialCloseoutLine),
        s"Material closeout must pin: $materialCloseoutLine"
      )
    assert(
      normalizedModelContract.contains(
        "Material Slice Closeout confirms `Scene.Material` opened no Defense, Conversion, Winning, Plan, Strategy, or Blunder path."
      )
    )
    assertEquals(widthRows.size, 25)
    assertEquals(mappedTactics, Tactics.sorted)
    widthRows.foreach: row =>
      assertEquals(row.size, 9)
      assert(row(2).nonEmpty, s"width map must name proof shape for ${row(1)}")
      assert(row(3).contains("StoryProof"), s"width map must keep StoryProof reuse visible for ${row(1)}")
      assert(row(5).nonEmpty, s"width map must name false-positive risks for ${row(1)}")
      assert(
        row(6).contains("yes") || row(6).contains("attached"),
        s"width map must name EngineCheck need for ${row(1)}"
      )
    Vector(
      "CaptureProof",
      "TargetProof",
      "LineProof",
      "PinProof",
      "SkewerProof",
      "DefenderProof",
      "KingProof",
      "PromotionProof",
      "MobilityProof"
    ).foreach: proofHome =>
      assert(interactionLaw.contains(s"| $proofHome |"), s"width map must cover $proofHome")

  test("Defense-0 charter pins Scene.Defense as bounded material-loss defense only"):
    val readme = Files.readString(docsRoot.resolve("README.md"))
    val ssot = Files.readString(docsRoot.resolve("ChessCommentarySSOT.md"))
    val architecture = Files.readString(docsRoot.resolve("ChessModelArchitecture.md"))
    val modelContract = Files.readString(docsRoot.resolve("ChessModelContract.md"))
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))
    val normalizedModelContract = modelContract.replaceAll("\\s+", " ")

    assert(interactionLaw.contains("## Defense-0 Scene.Defense Charter"))
    Vector(
      "Defense-0 opens only the charter for the first narrow `Scene.Defense` slice.",
      "First Defense scope:",
      "- attacked piece exists",
      "- opponent has an immediate material threat",
      "- the threat is same-board legal",
      "- the defended move removes, guards, or saves the target",
      "- material loss is prevented in a bounded way",
      "- no claim of best move or only move",
      "`ThreatProof = what must be stopped`",
      "`DefenseProof = how it is stopped`",
      "`Scene.Defense = Story label`",
      "`defends_piece / prevents_material_loss = speech claim`",
      "Forbidden Defense-0 claims:",
      "- only move",
      "- best move",
      "- no counterplay",
      "- refutes the attack",
      "- solves the position",
      "- king safety",
      "- mate defense",
      "- strategic defense",
      "- prophylaxis",
      "- winning",
      "- conversion",
      "Completion standard: `Scene.Defense` must not become another name for a good move or for stopping all counterplay."
    ).foreach: defenseLine =>
      assert(interactionLaw.contains(defenseLine), s"Defense-0 must pin: $defenseLine")

    Vector(readme, ssot, architecture).foreach: doc =>
      assert(doc.contains("Defense-0 opened only the charter for the first narrow `Scene.Defense` slice."))
      assert(doc.contains("Defense requires a threat."))
      assert(doc.contains("ThreatProof proves what must be stopped."))
      assert(doc.contains("DefenseProof proves how the move stops it."))
      assert(doc.contains("Defense is not no-counterplay."))
    assert(normalizedModelContract.contains("Defense-0 is owned by `StoryInteractionLaw.md`."))
    assert(normalizedModelContract.contains("Defense-0 opens only the `Scene.Defense` charter."))
    assert(
      normalizedModelContract.contains(
        "It opens no writer, proof sidecar, StoryTable integration, ExplanationPlan, renderer, LLM smoke, public route `200`, or production API."
      )
    )

  test("Defense-1 ThreatProof pins what Defense must stop without creating Story"):
    val readme = Files.readString(docsRoot.resolve("README.md"))
    val ssot = Files.readString(docsRoot.resolve("ChessCommentarySSOT.md"))
    val architecture = Files.readString(docsRoot.resolve("ChessModelArchitecture.md"))
    val modelContract = Files.readString(docsRoot.resolve("ChessModelContract.md"))
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))
    val normalizedModelContract = modelContract.replaceAll("\\s+", " ")

    assert(interactionLaw.contains("## Defense-1 ThreatProof"))
    Vector(
      "Defense-1 opens only `ThreatProof`.",
      "ThreatProof proves what must be stopped.",
      "ThreatProof does not create a Defense Story.",
      "ThreatProof does not create a public claim.",
      "Required ThreatProof fields:",
      "- rival side",
      "- threatened target",
      "- attacking piece",
      "- legal threat line",
      "- target value",
      "- material loss if unanswered",
      "- same-board proof",
      "- missing evidence",
      "Allowed Defense-1 meanings:",
      "- rival can capture the target",
      "- target is attacked",
      "- capture would cause material loss",
      "- threat is immediate and legal",
      "Forbidden Defense-1 meanings:",
      "- opponent has an attack",
      "- king is unsafe",
      "- no counterplay",
      "- mate threat",
      "- long-term pressure",
      "- strategic threat",
      "- engine says this is a threat",
      "Completion standard: ThreatProof proves what must be stopped, but it does not create a Defense Story or public claim."
    ).foreach: defenseLine =>
      assert(interactionLaw.contains(defenseLine), s"Defense-1 must pin: $defenseLine")

    Vector(readme, ssot, architecture).foreach: doc =>
      assert(doc.contains("Defense-1 opens only `ThreatProof`."))
      assert(doc.contains("ThreatProof proves what must be stopped."))
    assert(normalizedModelContract.contains("Defense-1 is owned by `StoryInteractionLaw.md`."))
    assert(normalizedModelContract.contains("Defense-1 opens only `ThreatProof`."))
    assert(
      normalizedModelContract.contains(
        "ThreatProof proves what must be stopped, but it does not create a Defense Story or public claim."
      )
    )

  test("Defense-2 DefenseProof pins how a specific move stops a ThreatProof"):
    val readme = Files.readString(docsRoot.resolve("README.md"))
    val ssot = Files.readString(docsRoot.resolve("ChessCommentarySSOT.md"))
    val architecture = Files.readString(docsRoot.resolve("ChessModelArchitecture.md"))
    val modelContract = Files.readString(docsRoot.resolve("ChessModelContract.md"))
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))
    val normalizedModelContract = modelContract.replaceAll("\\s+", " ")

    assert(interactionLaw.contains("## Defense-2 DefenseProof"))
    Vector(
      "Defense-2 opens only `DefenseProof`.",
      "DefenseProof proves how a specific move stops a specific ThreatProof.",
      "DefenseProof does not create a Defense Story.",
      "DefenseProof does not create a public claim.",
      "Allowed Defense-2 move types:",
      "1. target moves away",
      "2. target becomes guarded",
      "3. attacker line is blocked or attacker is captured",
      "Required DefenseProof fields:",
      "- defending side",
      "- defense move",
      "- defended target",
      "- original threat",
      "- after-defense target status",
      "- material loss prevented",
      "- same-board proof",
      "- missing evidence",
      "Allowed Defense-2 meanings:",
      "- the target is no longer capturable for gain",
      "- the target is defended after the move",
      "- the attacker's line is blocked",
      "- the attacker is captured",
      "Forbidden Defense-2 meanings:",
      "- solves the position",
      "- refutes the attack",
      "- stops all threats",
      "- only move",
      "- best defense",
      "- no counterplay",
      "- king safety",
      "- mate defense",
      "Completion standard: DefenseProof proves whether a specific threat is stopped, but it does not create a Defense Story or public claim."
    ).foreach: defenseLine =>
      assert(interactionLaw.contains(defenseLine), s"Defense-2 must pin: $defenseLine")

    Vector(readme, ssot, architecture).foreach: doc =>
      assert(doc.contains("Defense-2 opens only `DefenseProof`."))
      assert(doc.contains("DefenseProof proves how a specific move stops a specific ThreatProof."))
    assert(normalizedModelContract.contains("Defense-2 is owned by `StoryInteractionLaw.md`."))
    assert(normalizedModelContract.contains("Defense-2 opens only `DefenseProof`."))
    assert(
      normalizedModelContract.contains(
        "DefenseProof proves whether a specific threat is stopped, but it does not create a Defense Story or public claim."
      )
    )

  test("Defense-3 SceneDefense writer pins the first narrow Defense Story"):
    val readme = Files.readString(docsRoot.resolve("README.md"))
    val ssot = Files.readString(docsRoot.resolve("ChessCommentarySSOT.md"))
    val architecture = Files.readString(docsRoot.resolve("ChessModelArchitecture.md"))
    val modelContract = Files.readString(docsRoot.resolve("ChessModelContract.md"))
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))
    val normalizedModelContract = modelContract.replaceAll("\\s+", " ")

    assert(interactionLaw.contains("## Defense-3 SceneDefense Writer"))
    Vector(
      "Defense-3 opens only the named `SceneDefense` writer for one narrow `Scene.Defense` Story.",
      "Required SceneDefense writer evidence:",
      "- scene = Defense",
      "- StoryProof complete",
      "- ThreatProof complete",
      "- DefenseProof complete",
      "- same-board proof present",
      "- defense move legal",
      "- protected target identified",
      "- material loss prevented",
      "- writer = SceneDefense",
      "- EngineCheck does not Refute",
      "First allowed Defense-3 meanings:",
      "- this move defends the attacked piece",
      "- this move prevents immediate material loss",
      "Forbidden Defense-3 meanings:",
      "- only move",
      "- best move",
      "- refutes attack",
      "- stops counterplay",
      "- solves position",
      "- king safe",
      "- mate stopped",
      "- winning",
      "- decisive",
      "Completion standard: one narrow proof-backed Defense Story can enter StoryTable."
    ).foreach: defenseLine =>
      assert(interactionLaw.contains(defenseLine), s"Defense-3 must pin: $defenseLine")

    Vector(readme, ssot, architecture).foreach: doc =>
      assert(
        doc.contains(
          "Defense-3 opens only the named `SceneDefense` writer for one narrow `Scene.Defense` Story."
        )
      )
      assert(doc.contains("ThreatProof proves what must be stopped."))
      assert(doc.contains("DefenseProof proves how a specific move stops a specific ThreatProof."))
      assert(
        doc.contains(
          "Public route `200`, production API, and public/user-facing LLM narration remain closed."
        )
      )
    assert(normalizedModelContract.contains("Defense-3 is owned by `StoryInteractionLaw.md`."))
    assert(
      normalizedModelContract.contains(
        "Defense-3 opens only the named `SceneDefense` writer for one narrow `Scene.Defense` Story."
      )
    )
    assert(
      normalizedModelContract.contains(
        "A SceneDefense Story requires complete StoryProof, complete ThreatProof, complete DefenseProof, same-board proof, legal defense move, identified protected target, prevented material loss, writer `SceneDefense`, and no refuting EngineCheck."
      )
    )

  test("Defense-4 negative corpus pins defense-looking false positives silent"):
    val readme = Files.readString(docsRoot.resolve("README.md"))
    val ssot = Files.readString(docsRoot.resolve("ChessCommentarySSOT.md"))
    val architecture = Files.readString(docsRoot.resolve("ChessModelArchitecture.md"))
    val modelContract = Files.readString(docsRoot.resolve("ChessModelContract.md"))
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))
    val normalizedModelContract = modelContract.replaceAll("\\s+", " ")

    assert(interactionLaw.contains("## Defense-4 Defense Negative Corpus"))
    Vector(
      "Defense-4 opens only the Defense negative corpus.",
      "Defense-looking false positives must stay silent without complete ThreatProof and complete DefenseProof.",
      "Defense-4 negative cases:",
      "- no actual threat",
      "- threat is illegal",
      "- attacked piece is already adequately defended",
      "- defense move does not affect the target",
      "- defense move guards wrong piece",
      "- defense move still loses material",
      "- defense move allows equivalent recapture",
      "- defense only looks like prophylaxis",
      "- defense is actually a tactic / material gain",
      "- king safety claim tries to enter",
      "- mate defense tries to enter",
      "- only-move claim tries to enter",
      "- StoryProof incomplete",
      "- ThreatProof incomplete",
      "- DefenseProof incomplete",
      "- EngineCheck Refutes",
      "- high Proof score only",
      "Completion standard: defense-looking rows have no Lead or are Blocked unless ThreatProof and DefenseProof are complete."
    ).foreach: defenseLine =>
      assert(interactionLaw.contains(defenseLine), s"Defense-4 must pin: $defenseLine")

    Vector(readme, ssot, architecture).foreach: doc =>
      assert(doc.contains("Defense-4 opens only the Defense negative corpus."))
      assert(
        doc.contains(
          "Defense-looking false positives must stay silent without complete ThreatProof and complete DefenseProof."
        )
      )
      assert(
        doc.contains(
          "Public route `200`, production API, and public/user-facing LLM narration remain closed."
        )
      )
    assert(normalizedModelContract.contains("Defense-4 is owned by `StoryInteractionLaw.md`."))
    assert(normalizedModelContract.contains("Defense-4 opens only the Defense negative corpus."))
    assert(
      normalizedModelContract.contains(
        "Defense-looking rows have no Lead or are Blocked unless ThreatProof and DefenseProof are complete."
      )
    )

  test("Defense-5 EngineCheck reuse pins Defense engine evidence boundary"):
    val readme = Files.readString(docsRoot.resolve("README.md"))
    val ssot = Files.readString(docsRoot.resolve("ChessCommentarySSOT.md"))
    val architecture = Files.readString(docsRoot.resolve("ChessModelArchitecture.md"))
    val modelContract = Files.readString(docsRoot.resolve("ChessModelContract.md"))
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))
    val normalizedModelContract = modelContract.replaceAll("\\s+", " ")
    val chessSources =
      Files
        .walk(docsRoot.getParent.resolve("src/main/scala/lila/commentary/chess"))
        .toList
        .stream
        .filter(path => path.toString.endsWith(".scala"))
        .map(path => Files.readString(path))
        .reduce("", _ + "\n" + _)

    assert(interactionLaw.contains("## Defense-5 EngineCheck for Scene.Defense"))
    Vector(
      "Defense-5 opens only existing EngineCheck reuse for existing `Scene.Defense` Stories.",
      "Allowed Defense-5 EngineCheck statuses:",
      "- Unknown",
      "- Supports",
      "- Caps",
      "- Refutes",
      "Required Defense-5 EngineCheck evidence:",
      "- Defense Story already exists",
      "- same-board proof",
      "- same Story route",
      "- same legal line",
      "- fresh/depth evidence",
      "Forbidden Defense-5 meanings and shortcuts:",
      "- engine creates Defense Story",
      "- engine eval becomes public truth",
      "- PV becomes explanation",
      "- best move explanation",
      "- only move claim",
      "- refutes attack claim",
      "- DefenseEngineCheck duplicate type",
      "Completion standard: EngineCheck may support, cap, or refute an existing Defense Story, but it does not create Defense."
    ).foreach: defenseLine =>
      assert(interactionLaw.contains(defenseLine), s"Defense-5 must pin: $defenseLine")

    Vector(readme, ssot, architecture).foreach: doc =>
      assert(
        doc.contains("Defense-5 opens only existing EngineCheck reuse for existing `Scene.Defense` Stories.")
      )
      assert(
        doc.contains(
          "EngineCheck may support, cap, or refute an existing Defense Story, but it does not create Defense."
        )
      )
      assert(
        doc.contains(
          "Public route `200`, production API, and public/user-facing LLM narration remain closed."
        )
      )
    assert(normalizedModelContract.contains("Defense-5 is owned by `StoryInteractionLaw.md`."))
    assert(
      normalizedModelContract.contains(
        "Defense-5 opens only existing EngineCheck reuse for existing `Scene.Defense` Stories."
      )
    )
    assert(
      normalizedModelContract.contains(
        "EngineCheck may support, cap, or refute an existing Defense Story, but it does not create Defense."
      )
    )
    assert(!chessSources.contains("DefenseEngineCheck"))

  test("Defense-6 StoryTable integration pins four-family deterministic ordering"):
    val readme = Files.readString(docsRoot.resolve("README.md"))
    val ssot = Files.readString(docsRoot.resolve("ChessCommentarySSOT.md"))
    val architecture = Files.readString(docsRoot.resolve("ChessModelArchitecture.md"))
    val modelContract = Files.readString(docsRoot.resolve("ChessModelContract.md"))
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))
    val normalizedModelContract = modelContract.replaceAll("\\s+", " ")

    assert(interactionLaw.contains("## Defense-6 StoryTable Integration"))
    Vector(
      "Defense-6 opens only StoryTable integration for existing Hanging, Fork, Material, and Defense rows.",
      "Allowed Defense-6 roles and behavior:",
      "- Lead",
      "- Support",
      "- Context",
      "- Blocked",
      "- deterministic ordering",
      "Defense-6 StoryTable rules:",
      "- Refuted Defense -> Blocked",
      "- incomplete Defense -> Blocked",
      "- writerless Defense -> Blocked",
      "- Defense without ThreatProof -> Blocked",
      "- Defense without DefenseProof -> Blocked",
      "- Defense can compete for Lead only if it has complete proof",
      "- Support / Context are not sentences",
      "Forbidden Defense-6 shortcuts:",
      "- StoryTable creates Defense",
      "- raw engine eval ranks Defense",
      "- Defense proof text becomes public",
      "- renderer wording affects order",
      "- Defense silently opens only move or no counterplay",
      "Completion standard: StoryTable deterministically orders Hanging, Fork, Material, and Defense without creating new chess meaning."
    ).foreach: defenseLine =>
      assert(interactionLaw.contains(defenseLine), s"Defense-6 must pin: $defenseLine")

    Vector(readme, ssot, architecture).foreach: doc =>
      assert(
        doc.contains(
          "Defense-6 opens only StoryTable integration for existing Hanging, Fork, Material, and Defense rows."
        )
      )
      assert(
        doc.contains(
          "StoryTable deterministically orders Hanging, Fork, Material, and Defense without creating new chess meaning."
        )
      )
      assert(
        doc.contains(
          "The completed Stage 8, Fork-9, Material Slice Closeout, Defense-0, Defense-1, Defense-2, Defense-3, Defense-4, Defense-5, Defense-6, Defense-7, Defense-8, Defense-9, and Defense Slice Closeout scopes remain closed baselines."
        )
      )
    assert(normalizedModelContract.contains("Defense-6 is owned by `StoryInteractionLaw.md`."))
    assert(
      normalizedModelContract.contains(
        "Defense-6 opens only StoryTable integration for existing Hanging, Fork, Material, and Defense rows."
      )
    )
    assert(
      normalizedModelContract.contains(
        "StoryTable deterministically orders Hanging, Fork, Material, and Defense without creating new chess meaning."
      )
    )

  test("Defense-7 ExplanationPlan pins selected Defense Verdict speech boundary"):
    val readme = Files.readString(docsRoot.resolve("README.md"))
    val ssot = Files.readString(docsRoot.resolve("ChessCommentarySSOT.md"))
    val architecture = Files.readString(docsRoot.resolve("ChessModelArchitecture.md"))
    val modelContract = Files.readString(docsRoot.resolve("ChessModelContract.md"))
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))
    val normalizedModelContract = modelContract.replaceAll("\\s+", " ")

    assert(interactionLaw.contains("## Defense-7 ExplanationPlan for Scene.Defense"))
    Vector(
      "Defense-7 opens only ExplanationPlan mapping for selected `Scene.Defense` Verdicts.",
      "Defense-7 allowed input:",
      "- selected Verdict only",
      "Defense-7 forbidden inputs:",
      "- ThreatProof directly",
      "- DefenseProof directly",
      "- EngineCheck",
      "- BoardFacts",
      "- raw PV",
      "- proofFailures",
      "- source row",
      "Defense-7 first allowed claim keys:",
      "- defends_piece",
      "- prevents_material_loss",
      "- protects_target",
      "Defense-7 forbidden claim keys:",
      "- only_move",
      "- best_defense",
      "- refutes_attack",
      "- stops_counterplay",
      "- solves_position",
      "- king_safe",
      "- mate_defense",
      "- no_counterplay",
      "Completion standard: Defense ExplanationPlan creates no meaning stronger than the selected Verdict."
    ).foreach: defenseLine =>
      assert(interactionLaw.contains(defenseLine), s"Defense-7 must pin: $defenseLine")

    Vector(readme, ssot, architecture).foreach: doc =>
      assert(
        doc.contains("Defense-7 opens only ExplanationPlan mapping for selected `Scene.Defense` Verdicts.")
      )
      assert(doc.contains("Defense ExplanationPlan creates no meaning stronger than the selected Verdict."))
      assert(
        doc.contains(
          "The completed Stage 8, Fork-9, Material Slice Closeout, Defense-0, Defense-1, Defense-2, Defense-3, Defense-4, Defense-5, Defense-6, Defense-7, Defense-8, Defense-9, and Defense Slice Closeout scopes remain closed baselines."
        )
      )
    assert(normalizedModelContract.contains("Defense-7 is owned by `StoryInteractionLaw.md`."))
    assert(
      normalizedModelContract.contains(
        "Defense-7 opens only ExplanationPlan mapping for selected `Scene.Defense` Verdicts."
      )
    )
    assert(
      normalizedModelContract.contains(
        "Defense ExplanationPlan creates no meaning stronger than the selected Verdict."
      )
    )

  test("Defense-8 deterministic renderer pins Defense text boundary"):
    val readme = Files.readString(docsRoot.resolve("README.md"))
    val ssot = Files.readString(docsRoot.resolve("ChessCommentarySSOT.md"))
    val architecture = Files.readString(docsRoot.resolve("ChessModelArchitecture.md"))
    val modelContract = Files.readString(docsRoot.resolve("ChessModelContract.md"))
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))
    val normalizedModelContract = modelContract.replaceAll("\\s+", " ")

    assert(interactionLaw.contains("## Defense-8 Deterministic Renderer"))
    Vector(
      "Defense-8 opens only deterministic renderer text for selected Defense ExplanationPlan.",
      "Defense-8 allowed renderer input:",
      "- ExplanationPlan only",
      "Defense-8 first deterministic templates:",
      "- `{route} defends the piece on {target}.`",
      "- `{route} prevents the piece on {target} from being lost immediately.`",
      "Defense-8 forbidden renderer wording:",
      "- only move",
      "- best move",
      "- refutes the attack",
      "- stops all counterplay",
      "- solves the position",
      "- king is safe",
      "- mate is stopped",
      "- winning",
      "- decisive",
      "- forced",
      "Completion standard: Renderer text is no stronger than the Defense ExplanationPlan."
    ).foreach: defenseLine =>
      assert(interactionLaw.contains(defenseLine), s"Defense-8 must pin: $defenseLine")

    Vector(readme, ssot, architecture).foreach: doc =>
      assert(
        doc.contains("Defense-8 opens only deterministic renderer text for selected Defense ExplanationPlan.")
      )
      assert(doc.contains("Renderer text is no stronger than the Defense ExplanationPlan."))
      assert(
        doc.contains(
          "The completed Stage 8, Fork-9, Material Slice Closeout, Defense-0, Defense-1, Defense-2, Defense-3, Defense-4, Defense-5, Defense-6, Defense-7, Defense-8, Defense-9, and Defense Slice Closeout scopes remain closed baselines."
        )
      )
    assert(normalizedModelContract.contains("Defense-8 is owned by `StoryInteractionLaw.md`."))
    assert(
      normalizedModelContract.contains(
        "Defense-8 opens only deterministic renderer text for selected Defense ExplanationPlan."
      )
    )
    assert(normalizedModelContract.contains("Renderer text is no stronger than the Defense ExplanationPlan."))

  test("Defense-9 LLM smoke pins Defense rephrase boundary"):
    val readme = Files.readString(docsRoot.resolve("README.md"))
    val ssot = Files.readString(docsRoot.resolve("ChessCommentarySSOT.md"))
    val architecture = Files.readString(docsRoot.resolve("ChessModelArchitecture.md"))
    val modelContract = Files.readString(docsRoot.resolve("ChessModelContract.md"))
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))
    val normalizedModelContract = modelContract.replaceAll("\\s+", " ")

    assert(interactionLaw.contains("## Defense-9 LLM Smoke"))
    Vector(
      "Defense-9 opens only LLM smoke for selected Defense ExplanationPlan and RenderedLine.",
      "Defense-9 allowed LLM smoke input:",
      "- ExplanationPlan",
      "- RenderedLine",
      "Defense-9 Codex CLI smoke input:",
      "- renderedText",
      "- claimKey",
      "- strength",
      "- forbidden wording",
      "- instruction: Rephrase only. Do not add chess facts.",
      "Defense-9 forbidden LLM smoke inputs:",
      "- raw Verdict",
      "- Story",
      "- ThreatProof",
      "- DefenseProof",
      "- EngineCheck",
      "- BoardFacts",
      "- engine eval",
      "- raw PV",
      "- proofFailures",
      "Defense-9 smoke rejection checks:",
      "- no new move",
      "- no new line",
      "- no new tactic",
      "- no new plan",
      "- no engine mention",
      "- no only move",
      "- no best move",
      "- no no-counterplay",
      "- no king safety",
      "- no mate defense",
      "- no refutes-attack wording",
      "- no stronger claim",
      "Completion standard: LLM smoke does not make Defense text stronger."
    ).foreach: defenseLine =>
      assert(interactionLaw.contains(defenseLine), s"Defense-9 must pin: $defenseLine")

    Vector(readme, ssot, architecture).foreach: doc =>
      assert(
        doc.contains("Defense-9 opens only LLM smoke for selected Defense ExplanationPlan and RenderedLine.")
      )
      assert(doc.contains("LLM smoke does not make Defense text stronger."))
      assert(
        doc.contains(
          "Public route `200`, production API, and public/user-facing LLM narration remain closed."
        )
      )
    assert(normalizedModelContract.contains("Defense-9 is owned by `StoryInteractionLaw.md`."))
    assert(
      normalizedModelContract.contains(
        "Defense-9 opens only LLM smoke for selected Defense ExplanationPlan and RenderedLine."
      )
    )
    assert(normalizedModelContract.contains("LLM smoke does not make Defense text stronger."))

  test("Defense slice closeout pins audit and next-stage handoff"):
    val readme = Files.readString(docsRoot.resolve("README.md"))
    val ssot = Files.readString(docsRoot.resolve("ChessCommentarySSOT.md"))
    val architecture = Files.readString(docsRoot.resolve("ChessModelArchitecture.md"))
    val modelContract = Files.readString(docsRoot.resolve("ChessModelContract.md"))
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))
    val normalizedModelContract = modelContract.replaceAll("\\s+", " ")

    assert(interactionLaw.contains("## Defense Slice Closeout Pass"))
    Vector(
      "Defense Slice Closeout opens no new chess meaning beyond the narrow `Scene.Defense` vertical slice.",
      "Defense closeout scope audit:",
      "- only `Scene.Defense` opened",
      "- king safety remains closed",
      "- mate defense remains closed",
      "- Plan remains closed",
      "- Strategy remains closed",
      "- Counterplay remains closed beyond existing EngineCheck Caps",
      "- Prophylaxis remains closed",
      "Defense closeout authority audit:",
      "- ThreatProof owns what must be stopped",
      "- DefenseProof owns how the move stops it",
      "- StoryProof owns same-board Story identity evidence",
      "- EngineCheck supports, caps, or refutes an existing Defense Story only",
      "- StoryTable arbitrates roles without creating Defense",
      "Defense closeout negative corpus audit: defense-looking false positives stay silent without complete ThreatProof and DefenseProof.",
      "Defense closeout shared skeleton audit: charter, proof home, named writer, negative corpus, EngineCheck reuse, StoryTable integration, ExplanationPlan, deterministic renderer, LLM smoke, and closeout reused the existing vertical-slice skeleton.",
      "Defense closeout cleanup audit: `ThreatProof`, `DefenseProof`, `Scene.Defense`, and `defends_piece` each have one home.",
      "Defense closeout real-game smoke: Fischer-Spassky 1972 game 6 after 6...h6, 7.Bh4 is covered as an attacked-piece defense smoke.",
      "Defense closeout next-stage handoff: next candidates remain line-based tactic or king-forcing tactic; Defense does not open king safety, mate defense, or counterplay.",
      "Defense requires a threat.",
      "Defense is not no-counterplay.",
      "Reuse the skeleton before adding a new one.",
      "One chess meaning, one home.",
      "Completion standard: Defense closes as a narrow proof-backed attacked-piece material-loss defense slice only."
    ).foreach: closeoutLine =>
      assert(interactionLaw.contains(closeoutLine), s"Defense closeout must pin: $closeoutLine")

    Vector(readme, ssot, architecture).foreach: doc =>
      assert(
        doc.contains(
          "Defense Slice Closeout opens no new chess meaning beyond the narrow `Scene.Defense` vertical slice."
        )
      )
      assert(
        doc.contains(
          "Defense closes as a narrow proof-backed attacked-piece material-loss defense slice only."
        )
      )
      assert(
        doc.contains(
          "Public route `200`, production API, and public/user-facing LLM narration remain closed."
        )
      )
    assert(
      normalizedModelContract.contains(
        "Defense Slice Closeout confirms `Scene.Defense` opened no King safety, Mate defense, Plan, Strategy, Counterplay, or Prophylaxis path."
      )
    )
    assert(
      normalizedModelContract.contains(
        "`ThreatProof`, `DefenseProof`, `Scene.Defense`, and `defends_piece` each keep one home."
      )
    )

  test("MIH-0 charter pins Middlegame Interaction Hardening scope"):
    val readme = Files.readString(docsRoot.resolve("README.md"))
    val ssot = Files.readString(docsRoot.resolve("ChessCommentarySSOT.md"))
    val architecture = Files.readString(docsRoot.resolve("ChessModelArchitecture.md"))
    val modelContract = Files.readString(docsRoot.resolve("ChessModelContract.md"))
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))
    val normalizedModelContract = modelContract.replaceAll("\\s+", " ")

    assert(interactionLaw.contains("## Middlegame Interaction Hardening"))
    assert(interactionLaw.contains("### MIH-0 Charter"))
    Vector(
      "Middlegame Interaction Hardening opens no chess meaning. It stress-tests already-open meanings.",
      "MIH-0 opens only interaction hardening among existing Hanging, Fork, Material, and Defense rows.",
      "MIH-0 opens complex middlegame fixture based role stability checks.",
      "MIH-0 checks selected Verdict, ExplanationPlan, and renderer/LLM smoke boundary stability without opening new speech.",
      "MIH-0 may apply only the minimum StoryTable ordering fix if an existing ordering bug is exposed.",
      "Allowed MIH-0 rows:",
      "- Tactic.Hanging",
      "- Tactic.Fork",
      "- Scene.Material",
      "- Scene.Defense",
      "MIH-0 hardening matrix:",
      "- Material vs Defense",
      "- Hanging vs Defense",
      "- Fork vs Defense",
      "- Hanging vs Material",
      "- Hanging vs Fork",
      "- EngineCheck Supports / Caps / Refutes across these rows",
      "MIH-0 forbidden openings:",
      "- Line/Ray",
      "- RemoveGuard",
      "- Pin",
      "- Skewer",
      "- Pawn",
      "- BackRank",
      "- Plan",
      "- Strategy",
      "- Initiative",
      "- Pressure",
      "- public route 200",
      "- production API",
      "- public/user-facing LLM narration",
      "- new Story family",
      "- new proof home",
      "- new renderer wording",
      "Material vs Defense is the first and highest-risk case because `Scene.Defense` prevents immediate material loss while `Scene.Material` describes material balance changing now.",
      "If the move actually wins or changes material now, `Scene.Material` may lead.",
      "If the move prevents an immediate material loss, `Scene.Defense` may lead.",
      "If both are present on the same-board route, same-board outcome decides:",
      "- actual material gain/change outranks Defense as Material",
      "- prevented immediate loss without actual material gain/change remains Defense",
      "- speculative material loss remains Blocked",
      "StoryTable must not create Defense, Material, Hanging, or Fork rows during this hardening.",
      "Raw engine eval and renderer wording must not rank rows.",
      "Support and Context remain relation roles, not sentences."
    ).foreach: hardeningLine =>
      assert(interactionLaw.contains(hardeningLine), s"MIH-0 must pin: $hardeningLine")

    Vector(readme, ssot, architecture).foreach: doc =>
      assert(doc.contains("Middlegame Interaction Hardening is a closed baseline."))
      assert(
        doc.contains(
          "Middlegame Interaction Hardening opens no chess meaning. It stress-tests already-open meanings."
        )
      )
      assert(
        doc.contains(
          "MIH-0 opens only interaction hardening among existing Hanging, Fork, Material, and Defense rows."
        )
      )
      assert(
        doc.contains(
          "Material vs Defense is the first and highest-risk case because `Scene.Defense` prevents immediate material loss while `Scene.Material` describes material balance changing now."
        )
      )
    assert(
      normalizedModelContract.contains(
        "Middlegame Interaction Hardening checks StoryTable role stability, selected Verdict stability, ExplanationPlan stability, and renderer/LLM smoke boundaries among already-open Hanging, Fork, Material, and Defense rows."
      )
    )
    assert(
      normalizedModelContract.contains(
        "Middlegame Interaction Hardening opens no chess meaning. It stress-tests already-open meanings."
      )
    )
    assert(
      normalizedModelContract.contains(
        "Same-board Material vs Defense collisions are resolved by actual material change now over prevented immediate loss, with speculative material loss blocked."
      )
    )

  test("MIH-1 fixture map pins complex middlegame fixture contract"):
    val readme = Files.readString(docsRoot.resolve("README.md"))
    val ssot = Files.readString(docsRoot.resolve("ChessCommentarySSOT.md"))
    val architecture = Files.readString(docsRoot.resolve("ChessModelArchitecture.md"))
    val modelContract = Files.readString(docsRoot.resolve("ChessModelContract.md"))
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))
    val normalizedModelContract = modelContract.replaceAll("\\s+", " ")

    assert(interactionLaw.contains("### MIH-1 Fixture Map"))
    Vector(
      "MIH-1 opens only complex middlegame test fixtures for already-open Hanging, Fork, Material, and Defense rows.",
      "Each MIH-1 fixture must state:",
      "- same-board FEN",
      "- side to move",
      "- legal fixture lines",
      "- expected open rows",
      "- expected blocked rows",
      "- expected Lead / Support / Context / Blocked role",
      "- expected selected Verdict",
      "- forbidden claims",
      "Allowed MIH-1 fixture categories:",
      "- Hanging vs Material",
      "- Hanging vs Fork",
      "- Material vs Defense",
      "- Fork vs Defense",
      "- Material vs Defense on same board",
      "- EngineCheck Supports/Caps/Refutes over existing rows",
      "MIH-1 forbidden fixture shortcuts:",
      "- fixture implies a new Story family",
      "- pressure expectation",
      "- initiative expectation",
      "- best move expectation",
      "- only move expectation",
      "- proofFailures text as expected public output",
      "Completion standard: Fixture Map names board, rows, roles, selected Verdict, and forbidden claims without opening new meaning."
    ).foreach: fixtureLine =>
      assert(interactionLaw.contains(fixtureLine), s"MIH-1 must pin: $fixtureLine")

    Vector(readme, ssot, architecture).foreach: doc =>
      assert(
        doc.contains(
          "MIH-1 opens only complex middlegame test fixtures for already-open Hanging, Fork, Material, and Defense rows."
        )
      )
      assert(
        doc.contains(
          "Completion standard: Fixture Map names board, rows, roles, selected Verdict, and forbidden claims without opening new meaning."
        )
      )
    assert(
      normalizedModelContract.contains(
        "MIH-1 opens only complex middlegame test fixtures for already-open Hanging, Fork, Material, and Defense rows."
      )
    )
    assert(
      normalizedModelContract.contains(
        "Fixture Map names board, rows, roles, selected Verdict, and forbidden claims without opening new meaning."
      )
    )

  test("MIH-2 role stability pins deterministic StoryTable hardening"):
    val readme = Files.readString(docsRoot.resolve("README.md"))
    val ssot = Files.readString(docsRoot.resolve("ChessCommentarySSOT.md"))
    val architecture = Files.readString(docsRoot.resolve("ChessModelArchitecture.md"))
    val modelContract = Files.readString(docsRoot.resolve("ChessModelContract.md"))
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))
    val normalizedModelContract = modelContract.replaceAll("\\s+", " ")

    assert(interactionLaw.contains("### MIH-2 Role Stability"))
    Vector(
      "MIH-2 opens only StoryTable role stability checks over existing Hanging, Fork, Material, and Defense rows.",
      "MIH-2 verifies:",
      "- selected Verdict remains stable across input order changes",
      "- same-board collisions create no duplicate Lead",
      "- incomplete rows cannot Lead",
      "- refuted rows become Blocked",
      "- capped rows create no standalone strong claim",
      "MIH-2 specific checks:",
      "- `Scene.Material` and `Tactic.Hanging` on the same capture route cannot both Lead.",
      "- `Scene.Defense` cannot Lead without an actual ThreatProof.",
      "- `Tactic.Fork` cannot create a Material claim without complete two-target proof.",
      "MIH-2 forbidden openings:",
      "- new Story family",
      "- new proof home",
      "- new renderer wording",
      "- public route 200",
      "- production API",
      "- public/user-facing LLM narration",
      "Completion standard: Role Stability keeps selected Verdict deterministic, prevents duplicate Lead, blocks incomplete or refuted rows, and keeps capped rows from standalone strong claims without opening new meaning."
    ).foreach: roleLine =>
      assert(interactionLaw.contains(roleLine), s"MIH-2 must pin: $roleLine")

    Vector(readme, ssot, architecture).foreach: doc =>
      assert(
        doc.contains(
          "MIH-2 opens only StoryTable role stability checks over existing Hanging, Fork, Material, and Defense rows."
        )
      )
      assert(
        doc.contains(
          "Completion standard: Role Stability keeps selected Verdict deterministic, prevents duplicate Lead, blocks incomplete or refuted rows, and keeps capped rows from standalone strong claims without opening new meaning."
        )
      )
    assert(
      normalizedModelContract.contains(
        "MIH-2 opens only StoryTable role stability checks over existing Hanging, Fork, Material, and Defense rows."
      )
    )
    assert(
      normalizedModelContract.contains(
        "Role Stability keeps selected Verdict deterministic, prevents duplicate Lead, blocks incomplete or refuted rows, and keeps capped rows from standalone strong claims without opening new meaning."
      )
    )

  test("MIH-3 material defense collision pins actual material change priority"):
    val readme = Files.readString(docsRoot.resolve("README.md"))
    val ssot = Files.readString(docsRoot.resolve("ChessCommentarySSOT.md"))
    val architecture = Files.readString(docsRoot.resolve("ChessModelArchitecture.md"))
    val modelContract = Files.readString(docsRoot.resolve("ChessModelContract.md"))
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))
    val normalizedModelContract = modelContract.replaceAll("\\s+", " ")

    assert(interactionLaw.contains("### MIH-3 Material vs Defense Collision"))
    Vector(
      "MIH-3 opens only the Material vs Defense collision rule over existing `Scene.Material` and `Scene.Defense` rows.",
      "MIH-3 rules:",
      "- actual material balance change now gives `Scene.Material` priority",
      "- `Scene.Defense` may speak only when it prevents immediate material loss",
      "- speculative material loss does not open Defense",
      "- same-board Material and Defense rows must distinguish actual material change now from prevented immediate loss",
      "MIH-3 forbidden upgrades:",
      "- Defense as best defense",
      "- Defense as only move",
      "- Defense as refutes attack",
      "- Material as winning",
      "- Material as conversion",
      "- Material as decisive",
      "MIH-3 forbidden openings:",
      "- new Story family",
      "- new proof home",
      "- new renderer wording",
      "- public route 200",
      "- production API",
      "- public/user-facing LLM narration",
      "Completion standard: Material vs Defense collision selects actual material change now over prevented immediate loss, blocks speculative material loss, and keeps both public boundaries bounded."
    ).foreach: collisionLine =>
      assert(interactionLaw.contains(collisionLine), s"MIH-3 must pin: $collisionLine")

    Vector(readme, ssot, architecture).foreach: doc =>
      assert(
        doc.contains(
          "MIH-3 opens only the Material vs Defense collision rule over existing `Scene.Material` and `Scene.Defense` rows."
        )
      )
      assert(
        doc.contains(
          "Completion standard: Material vs Defense collision selects actual material change now over prevented immediate loss, blocks speculative material loss, and keeps both public boundaries bounded."
        )
      )
    assert(
      normalizedModelContract.contains(
        "MIH-3 opens only the Material vs Defense collision rule over existing `Scene.Material` and `Scene.Defense` rows."
      )
    )
    assert(
      normalizedModelContract.contains(
        "Material vs Defense collision selects actual material change now over prevented immediate loss, blocks speculative material loss, and keeps both public boundaries bounded."
      )
    )

  test("MIH-4 engine check interaction pins existing status boundaries"):
    val readme = Files.readString(docsRoot.resolve("README.md"))
    val ssot = Files.readString(docsRoot.resolve("ChessCommentarySSOT.md"))
    val architecture = Files.readString(docsRoot.resolve("ChessModelArchitecture.md"))
    val modelContract = Files.readString(docsRoot.resolve("ChessModelContract.md"))
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))
    val normalizedModelContract = modelContract.replaceAll("\\s+", " ")

    assert(interactionLaw.contains("### MIH-4 EngineCheck Interaction"))
    Vector(
      "MIH-4 opens only existing EngineCheck interaction checks over already-open Hanging, Fork, Material, and Defense rows.",
      "MIH-4 reuses existing `EngineCheck` only.",
      "MIH-4 verifies:",
      "- `Supports` creates no new claim",
      "- `Caps` weakens or suppresses allowed claim",
      "- `Refutes` blocks the checked Story",
      "- `Unknown` creates no engine-related expression",
      "MIH-4 forbidden shortcuts:",
      "- engine eval ordering",
      "- raw PV explanation",
      "- engine says wording",
      "- best-move wording",
      "- eval numbers in public values",
      "MIH-4 forbidden openings:",
      "- new Story family",
      "- new proof home",
      "- new renderer wording",
      "- public route 200",
      "- production API",
      "- public/user-facing LLM narration",
      "Completion standard: EngineCheck Interaction reuses existing EngineCheck statuses, keeps Supports and Unknown non-speaking, suppresses or weakens Caps, blocks Refutes, and prevents engine eval, raw PV, engine-says, best-move, and eval-number public leakage."
    ).foreach: engineLine =>
      assert(interactionLaw.contains(engineLine), s"MIH-4 must pin: $engineLine")

    Vector(readme, ssot, architecture).foreach: doc =>
      assert(
        doc.contains(
          "MIH-4 opens only existing EngineCheck interaction checks over already-open Hanging, Fork, Material, and Defense rows."
        )
      )
      assert(
        doc.contains(
          "Completion standard: EngineCheck Interaction reuses existing EngineCheck statuses, keeps Supports and Unknown non-speaking, suppresses or weakens Caps, blocks Refutes, and prevents engine eval, raw PV, engine-says, best-move, and eval-number public leakage."
        )
      )
    assert(
      normalizedModelContract.contains(
        "MIH-4 opens only existing EngineCheck interaction checks over already-open Hanging, Fork, Material, and Defense rows."
      )
    )
    assert(
      normalizedModelContract.contains(
        "EngineCheck Interaction reuses existing EngineCheck statuses, keeps Supports and Unknown non-speaking, suppresses or weakens Caps, blocks Refutes, and prevents engine eval, raw PV, engine-says, best-move, and eval-number public leakage."
      )
    )

  test("MIH-5 negative corpus pins close false positives silent"):
    val readme = Files.readString(docsRoot.resolve("README.md"))
    val ssot = Files.readString(docsRoot.resolve("ChessCommentarySSOT.md"))
    val architecture = Files.readString(docsRoot.resolve("ChessModelArchitecture.md"))
    val modelContract = Files.readString(docsRoot.resolve("ChessModelContract.md"))
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))
    val normalizedModelContract = modelContract.replaceAll("\\s+", " ")

    assert(interactionLaw.contains("### MIH-5 Negative Corpus"))
    Vector(
      "MIH-5 opens only close false-positive negative corpus tests over already-open Hanging, Fork, Material, Defense, and EngineCheck rows.",
      "Looks plausible is not enough. Complete proof or silence.",
      "MIH-5 must include:",
      "- attacked-looking piece but adequate recapture exists",
      "- fork-looking move but only one real target",
      "- material-looking capture but equal or lost after immediate reply",
      "- defense-looking move but no complete ThreatProof",
      "- defense move guards wrong target",
      "- defense move still leaves material loss",
      "- engine refutes otherwise plausible Story",
      "- same-board proof missing",
      "- route mismatch",
      "- stale or wrong engine line",
      "MIH-5 forbidden openings:",
      "- new Story family",
      "- new proof home",
      "- new renderer wording",
      "- public route 200",
      "- production API",
      "- public/user-facing LLM narration",
      "Completion standard: Negative Corpus keeps close false positives silent unless complete proof exists, and no plausible-looking row may reach selected public output through StoryTable, ExplanationPlan, renderer, or LLM smoke boundaries."
    ).foreach: negativeLine =>
      assert(interactionLaw.contains(negativeLine), s"MIH-5 must pin: $negativeLine")

    Vector(readme, ssot, architecture).foreach: doc =>
      assert(
        doc.contains(
          "MIH-5 opens only close false-positive negative corpus tests over already-open Hanging, Fork, Material, Defense, and EngineCheck rows."
        )
      )
      assert(
        doc.contains(
          "Completion standard: Negative Corpus keeps close false positives silent unless complete proof exists, and no plausible-looking row may reach selected public output through StoryTable, ExplanationPlan, renderer, or LLM smoke boundaries."
        )
      )
    assert(
      normalizedModelContract.contains(
        "MIH-5 opens only close false-positive negative corpus tests over already-open Hanging, Fork, Material, Defense, and EngineCheck rows."
      )
    )
    assert(
      normalizedModelContract.contains(
        "Negative Corpus keeps close false positives silent unless complete proof exists, and no plausible-looking row may reach selected public output through StoryTable, ExplanationPlan, renderer, or LLM smoke boundaries."
      )
    )

  test("MIH-6 downstream boundary smoke pins selected Verdict handoff only"):
    val readme = Files.readString(docsRoot.resolve("README.md"))
    val ssot = Files.readString(docsRoot.resolve("ChessCommentarySSOT.md"))
    val architecture = Files.readString(docsRoot.resolve("ChessModelArchitecture.md"))
    val modelContract = Files.readString(docsRoot.resolve("ChessModelContract.md"))
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))
    val normalizedModelContract = modelContract.replaceAll("\\s+", " ")

    assert(interactionLaw.contains("### MIH-6 Downstream Boundary Smoke"))
    Vector(
      "MIH-6 opens only downstream boundary smoke over selected Verdict, existing ExplanationPlan, existing DeterministicRenderer, and existing LLM smoke.",
      "MIH-6 verifies:",
      "- ExplanationPlan input is selected Verdict only",
      "- Renderer input is ExplanationPlan only",
      "- LLM smoke input is renderedText, claimKey, strength, forbidden wording, and rephrase-only instruction only",
      "- Support, Context, Blocked, capped, and refuted rows create no standalone public text",
      "MIH-6 forbidden openings:",
      "- new renderer template",
      "- new LLM behavior",
      "- raw Story to renderer or LLM",
      "- raw Proof to renderer or LLM",
      "- raw EngineCheck to renderer or LLM",
      "- public route 200",
      "- production API",
      "- public/user-facing LLM narration",
      "Completion standard: Downstream Boundary Smoke passes only selected Lead Verdict data through existing ExplanationPlan, renderer, and LLM smoke boundaries, while non-Lead, capped, and refuted rows stay silent."
    ).foreach: boundaryLine =>
      assert(interactionLaw.contains(boundaryLine), s"MIH-6 must pin: $boundaryLine")

    Vector(readme, ssot, architecture).foreach: doc =>
      assert(
        doc.contains(
          "MIH-6 opens only downstream boundary smoke over selected Verdict, existing ExplanationPlan, existing DeterministicRenderer, and existing LLM smoke."
        )
      )
      assert(
        doc.contains(
          "Completion standard: Downstream Boundary Smoke passes only selected Lead Verdict data through existing ExplanationPlan, renderer, and LLM smoke boundaries, while non-Lead, capped, and refuted rows stay silent."
        )
      )
    assert(
      normalizedModelContract.contains(
        "MIH-6 opens only downstream boundary smoke over selected Verdict, existing ExplanationPlan, existing DeterministicRenderer, and existing LLM smoke."
      )
    )
    assert(
      normalizedModelContract.contains(
        "Downstream Boundary Smoke passes only selected Lead Verdict data through existing ExplanationPlan, renderer, and LLM smoke boundaries, while non-Lead, capped, and refuted rows stay silent."
      )
    )

  test("MIH-7 diagnostics boundary pins diagnostics as non-public meaning"):
    val readme = Files.readString(docsRoot.resolve("README.md"))
    val ssot = Files.readString(docsRoot.resolve("ChessCommentarySSOT.md"))
    val architecture = Files.readString(docsRoot.resolve("ChessModelArchitecture.md"))
    val modelContract = Files.readString(docsRoot.resolve("ChessModelContract.md"))
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))
    val normalizedModelContract = modelContract.replaceAll("\\s+", " ")

    assert(interactionLaw.contains("### MIH-7 Diagnostics Boundary"))
    Vector(
      "MIH-7 opens only diagnostics boundary smoke over already-open Hanging, Fork, Material, Defense, StoryTable, selected Verdict, ExplanationPlan, renderer, and LLM smoke.",
      "MIH-7 verifies:",
      "- proofFailures are internal diagnostic only",
      "- Verdict.values do not include raw proof failure text or engine text",
      "- source row data does not flow directly into ExplanationPlan",
      "- StoryTable debug relation does not become renderer wording",
      "MIH-7 forbidden openings:",
      "- new Story family",
      "- new proof home",
      "- new renderer wording",
      "- new LLM behavior",
      "- raw Story to renderer or LLM",
      "- raw Proof to renderer or LLM",
      "- raw EngineCheck to renderer or LLM",
      "- public route 200",
      "- production API",
      "- public/user-facing LLM narration",
      "Completion standard: Diagnostics Boundary keeps proofFailures, raw proof failure text, engine text, source row data, and StoryTable debug relations out of public meaning, Verdict.values, ExplanationPlan source inputs, renderer wording, and LLM smoke prompts."
    ).foreach: diagnosticsLine =>
      assert(interactionLaw.contains(diagnosticsLine), s"MIH-7 must pin: $diagnosticsLine")

    Vector(readme, ssot, architecture).foreach: doc =>
      assert(
        doc.contains(
          "MIH-7 opens only diagnostics boundary smoke over already-open Hanging, Fork, Material, Defense, StoryTable, selected Verdict, ExplanationPlan, renderer, and LLM smoke."
        )
      )
      assert(
        doc.contains(
          "Completion standard: Diagnostics Boundary keeps proofFailures, raw proof failure text, engine text, source row data, and StoryTable debug relations out of public meaning, Verdict.values, ExplanationPlan source inputs, renderer wording, and LLM smoke prompts."
        )
      )
    assert(
      normalizedModelContract.contains(
        "MIH-7 opens only diagnostics boundary smoke over already-open Hanging, Fork, Material, Defense, StoryTable, selected Verdict, ExplanationPlan, renderer, and LLM smoke."
      )
    )
    assert(
      normalizedModelContract.contains(
        "Diagnostics Boundary keeps proofFailures, raw proof failure text, engine text, source row data, and StoryTable debug relations out of public meaning, Verdict.values, ExplanationPlan source inputs, renderer wording, and LLM smoke prompts."
      )
    )

  test("MIH closeout hard cleanup pins authority cleanup and closed public surfaces"):
    val readme = Files.readString(docsRoot.resolve("README.md"))
    val ssot = Files.readString(docsRoot.resolve("ChessCommentarySSOT.md"))
    val architecture = Files.readString(docsRoot.resolve("ChessModelArchitecture.md"))
    val modelContract = Files.readString(docsRoot.resolve("ChessModelContract.md"))
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))
    val normalizedModelContract = modelContract.replaceAll("\\s+", " ")

    assert(interactionLaw.contains("### MIH Closeout Hard Cleanup Pass"))
    Vector(
      "MIH Closeout opens no chess meaning. It only audits the MIH hardening surface.",
      "MIH Closeout audit checklist:",
      "- no new chess meaning opened",
      "- no new proof home opened",
      "- Hanging, Material, and Defense do not duplicate ownership of the same public meaning",
      "- Story label, proof home, and speech key remain separate",
      "- broad terms remain closed and do not become authority",
      "- detailed MIH rules live in StoryInteractionLaw.md only",
      "- other live docs summarize MIH scope without duplicating rule text",
      "- test helpers remain test-only and do not become runtime authority",
      "- public route 200 remains closed",
      "- production API remains closed",
      "- public/user-facing LLM narration remains closed",
      "MIH Closeout ownership map:",
      "- Tactic.Hanging owns the Story label for the hanging tactic; CaptureResult remains the capture proof home; can_win_piece remains the speech key.",
      "- Scene.Material owns the Story label for current material balance change; CaptureResult remains the simple capture proof home; material_balance_changes remains the first speech key.",
      "- Scene.Defense owns the Story label for preventing immediate material loss; ThreatProof and DefenseProof remain the proof homes; defends_piece remains the first speech key.",
      "Completion standard: MIH closes as interaction hardening only, with no new Story family, no new proof home, no duplicate meaning owner, no broad-term authority, no duplicated live rule authority outside StoryInteractionLaw.md, no promoted test helper, no public route 200, no production API, and no public/user-facing LLM narration."
    ).foreach: closeoutLine =>
      assert(interactionLaw.contains(closeoutLine), s"MIH closeout must pin: $closeoutLine")

    Vector(readme, ssot, architecture).foreach: doc =>
      assert(doc.contains("MIH Closeout opens no chess meaning. It only audits the MIH hardening surface."))
      assert(
        doc.contains(
          "Completion standard: MIH closes as interaction hardening only, with no new Story family, no new proof home, no duplicate meaning owner, no broad-term authority, no duplicated live rule authority outside StoryInteractionLaw.md, no promoted test helper, no public route 200, no production API, and no public/user-facing LLM narration."
        )
      )
      assert(!doc.contains("MIH Closeout audit checklist:"))
      assert(!doc.contains("MIH Closeout ownership map:"))
    assert(
      normalizedModelContract.contains(
        "MIH Closeout opens no chess meaning. It only audits the MIH hardening surface."
      )
    )
    assert(
      normalizedModelContract.contains(
        "MIH closes as interaction hardening only, with no new Story family, no new proof home, no duplicate meaning owner, no broad-term authority, no duplicated live rule authority outside StoryInteractionLaw.md, no promoted test helper, no public route 200, no production API, and no public/user-facing LLM narration."
      )
    )
    assert(!modelContract.contains("MIH Closeout audit checklist:"))
    assert(!modelContract.contains("MIH Closeout ownership map:"))

    val sourceRoot = Paths.get("modules/commentary/src/main/scala/lila/commentary/chess")
    val sourceStream = Files.walk(sourceRoot)
    val productionSource =
      try
        sourceStream
          .iterator()
          .asScala
          .filter(path => Files.isRegularFile(path))
          .map(path => path.getFileName.toString -> Files.readString(path))
          .toVector
      finally sourceStream.close()
    val productionText = productionSource.map(_._2).mkString("\n")
    Vector("MIH", "Middlegame Interaction Hardening", "Hard Cleanup", "Closeout").foreach: testOnlyTerm =>
      assert(
        !productionText.contains(testOnlyTerm),
        s"test-only MIH term reached production source: $testOnlyTerm"
      )
    Vector("ProofHome", "Mih", "Hardening", "Cleanup", "Closeout").foreach: forbiddenFileName =>
      assert(
        !productionSource.exists(_._1.contains(forbiddenFileName)),
        s"unexpected production authority file: $forbiddenFileName"
      )

    val controller = Files.readString(Paths.get("app/controllers/Commentary.scala"))
    assert(controller.contains("ServiceUnavailable(unavailable).toFuccess"))
    assert(controller.contains("\"noCommentary\" -> true"))
    assert(controller.contains("\"render\" -> JsNull"))
    assert(!controller.contains("Ok("))
    assert(!controller.contains("env.mode.isProd"))

  test(
    "Line-0 through Line Closeout pin narrow discovered attack proof writer corpus EngineCheck StoryTable ExplanationPlan renderer LLM smoke and hard cleanup only"
  ):
    val readme = Files.readString(docsRoot.resolve("README.md"))
    val ssot = Files.readString(docsRoot.resolve("ChessCommentarySSOT.md"))
    val architecture = Files.readString(docsRoot.resolve("ChessModelArchitecture.md"))
    val modelContract = Files.readString(docsRoot.resolve("ChessModelContract.md"))
    val boardFacts = Files.readString(docsRoot.resolve("BoardFacts.md"))
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))
    val normalizedModelContract = modelContract.replaceAll("\\s+", " ")

    assert(interactionLaw.contains("## Line / Ray Slice"))
    assert(interactionLaw.contains("### Line-0 Charter"))
    assert(interactionLaw.contains("### Line-1 LineProof"))
    assert(interactionLaw.contains("### Line-2 Tactic.DiscoveredAttack Writer"))
    assert(interactionLaw.contains("### Line-3 Negative Corpus"))
    assert(interactionLaw.contains("### Line-4 EngineCheck Reuse"))
    assert(interactionLaw.contains("### Line-5 StoryTable Integration"))
    assert(interactionLaw.contains("### Line-6 ExplanationPlan"))
    assert(interactionLaw.contains("### Line-7 Deterministic Renderer"))
    assert(interactionLaw.contains("### Line-8 LLM Smoke"))
    assert(interactionLaw.contains("### Line Closeout Hard Cleanup"))
    Vector(
      "Current implementation scope is Line / Ray Slice.",
      "Line-0 opens only the charter for the first narrow line/ray proof slice.",
      "First Line scope: a legal move reveals one slider attack on one non-king material target.",
      "LineFact observes geometry.",
      "LineProof binds the revealed line.",
      "Tactic.DiscoveredAttack may speak only after proof.",
      "Line-0 opens no broad LineTactic, Pin, Skewer, XRay public Story, RemoveGuard, mate threat, king safety, pressure, initiative, best move, forced line, winning, decisive, blunder, public route `200`, or production API.",
      "Line-1 opens only `LineProof` as a narrow proof home.",
      "LineProof proves side, slider piece, blocker or moved piece, revealed target, legal revealing move, line kind, same-board proof, before-move blocked or inactive line, after-move slider attack, and non-king material target.",
      "LineProof is not a public Story.",
      "LineFact is not a public Story.",
      "LineProof must not directly say pin, pressure, attack works, or wins material.",
      "LineProof proof failure text must not become renderer or LLM input.",
      "Line-2 opens only the named `TacticDiscoveredAttack` writer for one narrow `Tactic.DiscoveredAttack` Story.",
      "Tactic.DiscoveredAttack writer conditions:",
      "- complete StoryProof",
      "- complete LineProof",
      "- same-board legal replay",
      "- legal revealing move",
      "- target exists",
      "- after move slider attacks target",
      "- writer = TacticDiscoveredAttack",
      "- EngineCheck does not Refute",
      "Line-2 Story identity:",
      "- tactic = DiscoveredAttack",
      "- side = revealing side",
      "- target = revealed target square",
      "- anchor = moved piece or slider anchor",
      "- route = revealing move",
      "- rival = opposite side",
      "Line-2 forbidden openings:",
      "- Tactic.Pin",
      "- Tactic.Skewer",
      "- Tactic.XRay",
      "- RemoveGuard",
      "- king target speech",
      "Target king remains silent in Line-2.",
      "Line-3 opens only the negative corpus for the narrow `Tactic.DiscoveredAttack` slice.",
      "Line-3 negative corpus must close:",
      "- legal move is absent",
      "- same-board proof is absent",
      "- line is not actually opened",
      "- target is still not attacked after the move",
      "- slider is not a slider",
      "- target is king",
      "- blocker moved but another piece still blocks",
      "- discovered-looking move has no target",
      "- pressure, initiative, or mate wording tries to enter",
      "- Pin, Skewer, or XRay classification tries to enter",
      "Geometry is not enough. Revealed attack proof or silence.",
      "Line-3 opens no new Story family, proof home, renderer wording, LLM smoke, public route `200`, production API, pressure, initiative, mate threat, Pin, Skewer, XRay public Story, or RemoveGuard.",
      "Line-4 opens only existing `EngineCheck` reuse for existing `Tactic.DiscoveredAttack` Stories.",
      "Line-4 EngineCheck rules:",
      "- EngineCheck cannot create DiscoveredAttack",
      "- Supports creates no new claim",
      "- Caps suppresses strong expression",
      "- Refutes blocks the Story",
      "- Unknown creates no engine expression",
      "Line-4 forbidden openings:",
      "- raw eval ordering",
      "- raw PV explanation",
      "- engine says",
      "- best move",
      "- winning tactic",
      "Line-5 opens only StoryTable integration for existing `Tactic.Hanging`, `Tactic.Fork`, `Scene.Material`, `Scene.Defense`, and `Tactic.DiscoveredAttack` rows.",
      "Line-5 verification:",
      "- selected Verdict remains stable when input order changes",
      "- DiscoveredAttack does not own a Material claim",
      "- Hanging and Material on the same target do not both become Lead",
      "- Defense without an actual threat cannot create a claim that blocks DiscoveredAttack",
      "- Fork without two-target proof cannot absorb DiscoveredAttack",
      "Line-5 opens no Material claim for DiscoveredAttack, no Defense claim without ThreatProof, no Fork claim without two-target proof, no renderer wording, no LLM smoke, no public route `200`, and no production API.",
      "Line-6 opens only ExplanationPlan mapping for selected Lead `Tactic.DiscoveredAttack` Verdicts.",
      "Line-6 allowed claim key:",
      "- reveals_attack_on_piece",
      "Line-6 forbidden claim keys:",
      "- wins_material",
      "- pins_piece",
      "- skewers_piece",
      "- creates_pressure",
      "- takes_initiative",
      "- mate_threat",
      "- best_move",
      "- forced",
      "- decisive",
      "Support, Context, Blocked, capped, and refuted DiscoveredAttack rows create no standalone claim.",
      "Line-6 opens no renderer wording, LLM smoke, public route `200`, production API, Material claim, Pin, Skewer, XRay public Story, RemoveGuard, pressure, initiative, mate threat, best-move, forced-line, winning, or decisive claim.",
      "Line-7 opens only deterministic renderer text for selected `Tactic.DiscoveredAttack` ExplanationPlan.",
      "Renderer input is `ExplanationPlan` only.",
      "Line-7 allowed template:",
      "- `{route} reveals an attack on the piece on {target}.`",
      "Line-7 forbidden renderer wording:",
      "- wins material",
      "- winning",
      "- decisive",
      "- best move",
      "- forces",
      "- pins",
      "- skewers",
      "- puts pressure",
      "- creates a mating threat",
      "Line-7 opens no LLM smoke, public route `200`, production API, Material claim, Pin, Skewer, XRay public Story, RemoveGuard, pressure, initiative, mate threat, best-move, forced-line, winning, or decisive claim.",
      "Line-8 opens only LLM smoke for selected DiscoveredAttack ExplanationPlan and RenderedLine.",
      "Line-8 reuses existing 8B prompt smoke only.",
      "Line-8 LLM input:",
      "- renderedText",
      "- claimKey",
      "- strength",
      "- forbidden wording",
      "- Rephrase only. Do not add chess facts.",
      "Line-8 forbidden inputs and additions:",
      "- raw Story",
      "- raw LineProof",
      "- LineFact",
      "- BoardFacts",
      "- EngineCheck",
      "- raw PV",
      "- proofFailures",
      "- new move",
      "- new line",
      "- mate",
      "- pressure",
      "- initiative",
      "- winning claim",
      "Line-8 opens no raw Story, raw LineProof, LineFact, BoardFacts, EngineCheck, raw PV, proofFailures, public/user-facing LLM narration, public route `200`, production API, Material claim, Pin, Skewer, XRay public Story, RemoveGuard, pressure, initiative, mate threat, best-move, forced-line, winning, or decisive claim.",
      "Completion standard: LLM smoke may receive only renderedText, claimKey, strength, forbidden wording, and the instruction `Rephrase only. Do not add chess facts.` for selected DiscoveredAttack RenderedLine; it must reject raw proof/board/engine inputs, new moves, new lines, and mate, pressure, initiative, or winning claims.",
      "Line Closeout opens no new chess meaning. It only audits the Line / Ray Slice hard cleanup surface.",
      "Line Closeout must confirm:",
      "- LineFact observes geometry only.",
      "- LineProof binds the revealed line only.",
      "- Tactic.DiscoveredAttack owns only the proof-backed Story identity.",
      "- `reveals_attack_on_piece` owns only the bounded speech claim key.",
      "- Broad Line, Ray, XRay, Pin, and Skewer are not live public authority for this slice.",
      "- LineProof does not duplicate StoryProof, CaptureResult, MultiTargetProof, ThreatProof, DefenseProof, or EngineCheck.",
      "- Renderer and LLM smoke cannot create wording stronger than the selected DiscoveredAttack ExplanationPlan.",
      "- Detailed Line authority lives only in `StoryInteractionLaw.md`; AGENTS.md, README.md, ChessCommentarySSOT.md, ChessModelArchitecture.md, and ChessModelContract.md may summarize only.",
      "- Public route `200`, production API, and public/user-facing LLM narration remain closed.",
      "Line Closeout opens no broad LineTactic, Ray tactic, XRay public Story, Pin, Skewer, RemoveGuard, Material claim, pressure, initiative, mate threat, best-move, forced-line, winning, decisive, blunder, new proof home, new Story family, renderer wording beyond Line-7, LLM input beyond Line-8, public route `200`, production API, or public/user-facing LLM narration.",
      "Completion standard: Line closes as a narrow proof-backed discovered attack slice only, with LineFact, LineProof, Tactic.DiscoveredAttack, StoryTable, ExplanationPlan, Renderer, and LLM smoke keeping separate authority and no downstream layer speaking beyond selected proof-backed `reveals_attack_on_piece`."
    ).foreach: lineScope =>
      assert(interactionLaw.contains(lineScope), s"Line / Ray scope must pin: $lineScope")

    Vector(readme, ssot, architecture).foreach: doc =>
      assert(doc.contains("Line / Ray Slice is a closed baseline."))
      assert(doc.contains("Line-0 opens only the charter for the first narrow line/ray proof slice."))
      assert(doc.contains("Line-1 opens only `LineProof` as a narrow proof home."))
      assert(
        doc.contains(
          "Line-2 opens only the named `TacticDiscoveredAttack` writer for one narrow `Tactic.DiscoveredAttack` Story."
        )
      )
      assert(
        doc.contains("Line-3 opens only the negative corpus for the narrow `Tactic.DiscoveredAttack` slice.")
      )
      assert(
        doc.contains(
          "Line-4 opens only existing `EngineCheck` reuse for existing `Tactic.DiscoveredAttack` Stories."
        )
      )
      assert(
        doc.contains(
          "Line-5 opens only StoryTable integration for existing `Tactic.Hanging`, `Tactic.Fork`, `Scene.Material`, `Scene.Defense`, and `Tactic.DiscoveredAttack` rows."
        )
      )
      assert(
        doc.contains(
          "Line-6 opens only ExplanationPlan mapping for selected Lead `Tactic.DiscoveredAttack` Verdicts."
        )
      )
      assert(
        doc.contains(
          "Line-7 opens only deterministic renderer text for selected `Tactic.DiscoveredAttack` ExplanationPlan."
        )
      )
      assert(
        doc.contains(
          "Line-8 opens only LLM smoke for selected DiscoveredAttack ExplanationPlan and RenderedLine."
        )
      )
      assert(
        doc.contains(
          "Line Closeout opens no new chess meaning; it audits only LineFact, LineProof, Tactic.DiscoveredAttack, speech key, renderer/LLM, docs authority, and closed public surfaces."
        )
      )
      assert(
        doc.contains(
          "LineFact observes geometry. LineProof binds the revealed line. Tactic.DiscoveredAttack may speak only after proof."
        )
      )
      assert(doc.contains("Geometry is not enough. Revealed attack proof or silence."))
      assert(doc.contains("LineProof is not a public Story. LineFact is not a public Story."))
      assert(
        doc.contains(
          "Public route `200`, production API, and public/user-facing LLM narration remain closed."
        )
      )
    assert(
      boardFacts.contains(
        "LineFact observes geometry; it does not prove `LineProof` and it is not a public Story."
      )
    )
    assert(normalizedModelContract.contains("Line-0 and Line-1 are owned by `StoryInteractionLaw.md`."))
    assert(normalizedModelContract.contains("Line-1 opens only `LineProof` as a narrow proof home."))
    assert(
      normalizedModelContract.contains(
        "LineProof is not a public Story, LineFact is not a public Story, and proof failure text must not become renderer or LLM input."
      )
    )
    assert(
      normalizedModelContract.contains(
        "Line-1 opens no Story writer, StoryTable integration, ExplanationPlan mapping, renderer wording, LLM smoke, public route `200`, production API, pin, skewer, x-ray public Story, RemoveGuard, pressure, initiative, winning, decisive, blunder, best-move, forced-line, mate threat, or king-safety claim."
      )
    )
    assert(
      normalizedModelContract.contains(
        "Line-2 opens only the named `TacticDiscoveredAttack` writer for one narrow `Tactic.DiscoveredAttack` Story."
      )
    )
    assert(
      normalizedModelContract.contains(
        "Line-2 opens no Tactic.Pin, Tactic.Skewer, Tactic.XRay, RemoveGuard, king-target speech, ExplanationPlan mapping, renderer wording, LLM smoke, public route `200`, production API, winning, decisive, blunder, best-move, forced-line, pressure, initiative, mate threat, or king-safety claim."
      )
    )
    assert(
      normalizedModelContract.contains(
        "Line-3 opens only the negative corpus for the narrow `Tactic.DiscoveredAttack` slice."
      )
    )
    assert(normalizedModelContract.contains("Geometry is not enough. Revealed attack proof or silence."))
    assert(
      normalizedModelContract.contains(
        "Line-4 opens only existing `EngineCheck` reuse for existing `Tactic.DiscoveredAttack` Stories."
      )
    )
    assert(
      normalizedModelContract.contains(
        "EngineCheck cannot create DiscoveredAttack, Supports creates no new claim, Caps suppresses strong expression, Refutes blocks the Story, and Unknown creates no engine expression."
      )
    )
    assert(
      normalizedModelContract.contains(
        "Line-5 opens only StoryTable integration for existing `Tactic.Hanging`, `Tactic.Fork`, `Scene.Material`, `Scene.Defense`, and `Tactic.DiscoveredAttack` rows."
      )
    )
    assert(
      normalizedModelContract.contains(
        "DiscoveredAttack does not own a Material claim, Hanging and Material on the same target do not both become Lead, Defense without an actual threat cannot create a claim that blocks DiscoveredAttack, and Fork without two-target proof cannot absorb DiscoveredAttack."
      )
    )
    assert(
      normalizedModelContract.contains(
        "Line-6 opens only ExplanationPlan mapping for selected Lead `Tactic.DiscoveredAttack` Verdicts."
      )
    )
    assert(
      normalizedModelContract.contains(
        "Support, Context, Blocked, capped, and refuted DiscoveredAttack rows create no standalone claim."
      )
    )
    assert(
      normalizedModelContract.contains(
        "Line-7 opens only deterministic renderer text for selected `Tactic.DiscoveredAttack` ExplanationPlan."
      )
    )
    assert(
      normalizedModelContract.contains(
        "Renderer receives `ExplanationPlan` only and may render `{route} reveals an attack on the piece on {target}.`"
      )
    )
    assert(
      normalizedModelContract.contains(
        "It must not render `wins material`, `winning`, `decisive`, `best move`, `forces`, `pins`, `skewers`, `puts pressure`, or `creates a mating threat`."
      )
    )
    assert(
      normalizedModelContract.contains(
        "Line-8 opens only LLM smoke for selected DiscoveredAttack ExplanationPlan and RenderedLine."
      )
    )
    assert(normalizedModelContract.contains("It reuses existing 8B prompt smoke only."))
    assert(
      normalizedModelContract.contains(
        "LLM input is renderedText, claimKey, strength, forbidden wording, and `Rephrase only. Do not add chess facts.`"
      )
    )
    assert(
      normalizedModelContract.contains(
        "It must not read raw Story, raw LineProof, LineFact, BoardFacts, EngineCheck, raw PV, or proofFailures, and it must reject new moves, new lines, mate, pressure, initiative, and winning claims."
      )
    )
    assert(normalizedModelContract.contains("Line Closeout opens no new chess meaning."))
    assert(
      normalizedModelContract.contains(
        "It audits only that LineFact observes geometry, LineProof proves one revealed line, Tactic.DiscoveredAttack owns the Story identity, `reveals_attack_on_piece` owns speech vocabulary, renderer/LLM stay no stronger than the selected ExplanationPlan, detailed Line closeout authority stays in `StoryInteractionLaw.md`, and public route `200`, production API, and public/user-facing LLM narration remain closed."
      )
    )

  test("Pin-0 charter opens only the narrow pinned-to-king line relation"):
    val readme = Files.readString(docsRoot.resolve("README.md"))
    val ssot = Files.readString(docsRoot.resolve("ChessCommentarySSOT.md"))
    val architecture = Files.readString(docsRoot.resolve("ChessModelArchitecture.md"))
    val modelContract = Files.readString(docsRoot.resolve("ChessModelContract.md"))
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))
    val normalizedModelContract = modelContract.replaceAll("\\s+", " ")

    assert(interactionLaw.contains("## Line / Defender Contact Neighborhood"))
    assert(interactionLaw.contains("### Pin-0 Charter"))
    Vector(
      "Current implementation scope is Line / Defender Contact Neighborhood.",
      "Pin-0 opens only the charter for the second narrow line/defender contact vertical slice.",
      "Pin first positive scope is not a broad pin family.",
      "Pin first scope: a legal move creates or reveals a line where one non-king piece is pinned to its king.",
      "LineFact observes geometry.",
      "LineProof binds the line.",
      "PinProof proves the pinned relation.",
      "Tactic.Pin may speak only after proof.",
      "Pin-0 allowed opening:",
      "- narrow `Tactic.Pin`",
      "- king-behind line relation",
      "- one non-king pinned target",
      "- legal move that creates or reveals the pin relation",
      "- bounded pin wording after selected Verdict only",
      "Pin-0 forbidden openings:",
      "- broad LineTactic",
      "- broad AbsPin or RelPin family",
      "- Skewer",
      "- XRay public Story",
      "- RemoveGuard",
      "- DiscoveredAttack expansion",
      "- mate threat",
      "- king safety",
      "- winning material",
      "- decisive tactic",
      "- forced move",
      "- best move",
      "- cannot move wording",
      "- pressure",
      "- initiative",
      "- public route `200`",
      "- production API",
      "- public/user-facing LLM narration",
      "Pin-0 opens no broad LineTactic, broad AbsPin or RelPin family, Skewer, XRay public Story, RemoveGuard, DiscoveredAttack expansion, mate threat, king safety, winning material, decisive tactic, forced move, best move, cannot move wording, pressure, initiative, public route `200`, production API, or public/user-facing LLM narration.",
      "Completion standard: Pin-0 keeps pin work at charter scope and opens no PinProof runtime, no Tactic.Pin writer, no StoryTable integration, no ExplanationPlan mapping, no renderer wording, no LLM smoke, no public route `200`, and no production API."
    ).foreach: pinScope =>
      assert(interactionLaw.contains(pinScope), s"Pin-0 scope must pin: $pinScope")

    Vector(readme, ssot, architecture).foreach: doc =>
      assert(doc.contains("Line / Ray Slice is a closed baseline."))
      assert(doc.contains("Current implementation scope is Line / Defender Contact Neighborhood."))
      assert(
        doc.contains(
          "Pin-0 opens only the charter for the second narrow line/defender contact vertical slice."
        )
      )
      assert(doc.contains("Pin first positive scope is not a broad pin family."))
      assert(
        doc.contains(
          "Pin first scope: a legal move creates or reveals a line where one non-king piece is pinned to its king."
        )
      )
      assert(
        doc.contains(
          "LineFact observes geometry. LineProof binds the line. PinProof proves the pinned relation. Tactic.Pin may speak only after proof."
        )
      )
      assert(
        doc.contains(
          "Pin-0 opens only narrow Tactic.Pin, king-behind line relation, one non-king pinned target, a legal move that creates or reveals the pin relation, and bounded pin wording after selected Verdict only."
        )
      )
      assert(
        doc.contains(
          "Pin-0 opens no broad LineTactic, broad AbsPin or RelPin family, Skewer, XRay public Story, RemoveGuard, DiscoveredAttack expansion, mate threat, king safety, winning material, decisive tactic, forced move, best move, cannot move wording, pressure, initiative, public route `200`, production API, or public/user-facing LLM narration."
        )
      )
      assert(
        doc.contains(
          "Public route `200`, production API, and public/user-facing LLM narration remain closed."
        )
      )
    assert(normalizedModelContract.contains("Pin-0 is owned by `StoryInteractionLaw.md`."))
    assert(
      normalizedModelContract.contains(
        "Pin-0 opens only the charter for the second narrow line/defender contact vertical slice."
      )
    )
    assert(normalizedModelContract.contains("The first positive Pin scope is not a broad pin family."))
    assert(
      normalizedModelContract.contains(
        "The Pin first scope is a legal move that creates or reveals a line where one non-king piece is pinned to its king."
      )
    )
    assert(
      normalizedModelContract.contains(
        "LineFact observes geometry, LineProof binds the line, PinProof proves the pinned relation, and Tactic.Pin may speak only after proof."
      )
    )
    assert(
      normalizedModelContract.contains(
        "Pin-0 opens only narrow Tactic.Pin, king-behind line relation, one non-king pinned target, a legal move that creates or reveals the pin relation, and bounded pin wording after selected Verdict only."
      )
    )
    assert(
      normalizedModelContract.contains(
        "Pin-0 opens no broad LineTactic, broad AbsPin or RelPin family, Skewer, XRay public Story, RemoveGuard, DiscoveredAttack expansion, mate threat, king safety, winning material, decisive tactic, forced move, best move, cannot move wording, pressure, initiative, public route `200`, production API, or public/user-facing LLM narration."
      )
    )

  test("Pin-1 PinProof opens only the narrow pinned relation proof home"):
    val readme = Files.readString(docsRoot.resolve("README.md"))
    val ssot = Files.readString(docsRoot.resolve("ChessCommentarySSOT.md"))
    val architecture = Files.readString(docsRoot.resolve("ChessModelArchitecture.md"))
    val modelContract = Files.readString(docsRoot.resolve("ChessModelContract.md"))
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))
    val normalizedModelContract = modelContract.replaceAll("\\s+", " ")

    assert(interactionLaw.contains("### Pin-1 PinProof"))
    Vector(
      "Pin-1 opens only `PinProof` as a narrow proof home.",
      "PinProof proves side creating the pin, pinned target, pinning slider, king behind target, legal pinning or revealing move, line kind, same-board proof, before/after relation, target non-king, target and king same side, and slider attacks through target toward king after move.",
      "PinProof must prove:",
      "- side creating the pin",
      "- pinned target",
      "- pinning slider",
      "- king behind target",
      "- legal pinning or revealing move",
      "- line kind: file / rank / diagonal",
      "- same-board proof",
      "- before/after relation",
      "- target is non-king",
      "- target and king are same side",
      "- slider attacks through target toward king after move",
      "PinProof is not a public Story.",
      "LineFact is not a public Story.",
      "LineProof is not a public Story.",
      "PinProof must not say material gain, king unsafe, mate, pressure, or initiative.",
      "PinProof proof failure text must not become renderer or LLM input.",
      "Pin-1 forbidden openings:",
      "- Tactic.Pin writer",
      "- StoryTable integration",
      "- ExplanationPlan mapping",
      "- renderer wording",
      "- LLM smoke",
      "- material gain claim",
      "- king unsafe claim",
      "- mate threat",
      "- pressure",
      "- initiative",
      "- public route `200`",
      "- production API",
      "Completion standard: `PinProof` proves only a legal move creating or revealing one pinned-to-king relation over one non-king target, while LineFact, LineProof, PinProof, proof failures, renderer, and LLM boundaries remain non-speaking."
    ).foreach: pinScope =>
      assert(interactionLaw.contains(pinScope), s"Pin-1 scope must pin: $pinScope")

    Vector(readme, ssot, architecture).foreach: doc =>
      assert(doc.contains("Pin-1 opens only `PinProof` as a narrow proof home."))
      assert(
        doc.contains(
          "PinProof proves side creating the pin, pinned target, pinning slider, king behind target, legal pinning or revealing move, line kind, same-board proof, before/after relation, target non-king, target and king same side, and slider attacks through target toward king after move."
        )
      )
      assert(doc.contains("PinProof is not a public Story. LineFact and LineProof are not public Stories."))
      assert(
        doc.contains(
          "PinProof says no material gain, king unsafe, mate, pressure, or initiative; proofFailures stay out of renderer/LLM input."
        )
      )
      assert(
        doc.contains(
          "Public route `200`, production API, and public/user-facing LLM narration remain closed."
        )
      )
    assert(normalizedModelContract.contains("Pin-1 is owned by `StoryInteractionLaw.md`."))
    assert(normalizedModelContract.contains("Pin-1 opens only `PinProof` as a narrow proof home."))
    assert(
      normalizedModelContract.contains(
        "PinProof proves side creating the pin, pinned target, pinning slider, king behind target, legal pinning or revealing move, line kind, same-board proof, before/after relation, target non-king, target and king same side, and slider attacks through target toward king after move."
      )
    )
    assert(
      normalizedModelContract.contains(
        "PinProof is not a public Story, LineFact is not a public Story, and LineProof is not a public Story."
      )
    )
    assert(
      normalizedModelContract.contains(
        "PinProof says no material gain, king unsafe, mate, pressure, or initiative, and proof failure text must not become renderer or LLM input."
      )
    )

  test("Pin-2 TacticPin writer opens only proof-backed Tactic.Pin Story identity"):
    val readme = Files.readString(docsRoot.resolve("README.md"))
    val ssot = Files.readString(docsRoot.resolve("ChessCommentarySSOT.md"))
    val architecture = Files.readString(docsRoot.resolve("ChessModelArchitecture.md"))
    val modelContract = Files.readString(docsRoot.resolve("ChessModelContract.md"))
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))
    val normalizedModelContract = modelContract.replaceAll("\\s+", " ")

    assert(interactionLaw.contains("### Pin-2 Tactic.Pin Writer"))
    Vector(
      "Pin-2 opens only the named `TacticPin` writer for one narrow `Tactic.Pin` Story.",
      "TacticPin writer conditions:",
      "- complete StoryProof",
      "- complete PinProof",
      "- same-board legal replay",
      "- legal pinning or revealing move",
      "- pinned target exists",
      "- pinning slider exists",
      "- king-behind-target relation complete",
      "- writer = TacticPin",
      "- EngineCheck does not Refute",
      "Pin-2 Story identity:",
      "- tactic = Pin",
      "- scene = Tactic",
      "- side = pinning side",
      "- rival = pinned side",
      "- target = pinned target square",
      "- anchor = pinning slider square or moved piece square",
      "- route = pinning/revealing move",
      "Pin-2 opened runtime pieces:",
      "- `Tactic.Pin` tactic identity",
      "- `StoryWriter.TacticPin` writer identity",
      "- `TacticPin.write`",
      "- `TacticPin.withEngineCheck`",
      "- StoryTable admission for complete non-refuted `Tactic.Pin` rows",
      "Pin-2 forbidden openings:",
      "- Material claim",
      "- Defense ownership",
      "- RemoveGuard ownership",
      "- king target speech",
      "- broad AbsPin or RelPin family",
      "- Skewer",
      "- XRay public Story",
      "- DiscoveredAttack expansion",
      "- ExplanationPlan mapping",
      "- renderer wording",
      "- LLM smoke",
      "- public route `200`",
      "- production API",
      "Target king remains silent in Pin-2.",
      "Completion standard: Tactic.Pin may become a Story only through complete StoryProof plus complete PinProof for one legal move creating or revealing one pinned-to-king relation over one non-king target, while Material, Defense, RemoveGuard, king target speech, ExplanationPlan, renderer, LLM, public route `200`, and production API remain closed."
    ).foreach: pinScope =>
      assert(interactionLaw.contains(pinScope), s"Pin-2 scope must pin: $pinScope")

    Vector(readme, ssot, architecture).foreach: doc =>
      assert(doc.contains("Pin-2 opens only the named `TacticPin` writer for one narrow `Tactic.Pin` Story."))
      assert(
        doc.contains(
          "TacticPin requires complete StoryProof, complete PinProof, same-board legal replay, legal pinning or revealing move, pinned target, pinning slider, king-behind-target relation, writer identity, and no EngineCheck Refutes status."
        )
      )
      assert(
        doc.contains(
          "Pin Story identity is tactic Pin, scene Tactic, pinning side, pinned-side rival, pinned target square, pinning slider or moved-piece anchor, and pinning/revealing route."
        )
      )
      assert(
        doc.contains(
          "Pin-2 opens no Material claim, Defense ownership, RemoveGuard ownership, king target speech, broad AbsPin or RelPin family, ExplanationPlan mapping, renderer wording, LLM smoke, public route `200`, production API, or public/user-facing LLM narration."
        )
      )
      assert(
        doc.contains(
          "Public route `200`, production API, and public/user-facing LLM narration remain closed."
        )
      )
    assert(normalizedModelContract.contains("Pin-2 is owned by `StoryInteractionLaw.md`."))
    assert(
      normalizedModelContract.contains(
        "Pin-2 opens only the named `TacticPin` writer for one narrow `Tactic.Pin` Story."
      )
    )
    assert(
      normalizedModelContract.contains(
        "TacticPin requires complete StoryProof, complete PinProof, same-board legal replay, legal pinning or revealing move, pinned target, pinning slider, king-behind-target relation, writer identity, and no EngineCheck Refutes status."
      )
    )
    assert(
      normalizedModelContract.contains(
        "Pin Story identity is tactic Pin, scene Tactic, pinning side, pinned-side rival, pinned target square, pinning slider or moved-piece anchor, and pinning/revealing route."
      )
    )
    assert(
      normalizedModelContract.contains(
        "Pin-2 opens no Material claim, Defense ownership, RemoveGuard ownership, king target speech, broad AbsPin or RelPin family, ExplanationPlan mapping, renderer wording, LLM smoke, public route `200`, production API, or public/user-facing LLM narration."
      )
    )

  test("Pin-3 negative corpus keeps pin-looking false positives silent"):
    val readme = Files.readString(docsRoot.resolve("README.md"))
    val ssot = Files.readString(docsRoot.resolve("ChessCommentarySSOT.md"))
    val architecture = Files.readString(docsRoot.resolve("ChessModelArchitecture.md"))
    val modelContract = Files.readString(docsRoot.resolve("ChessModelContract.md"))
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))
    val normalizedModelContract = modelContract.replaceAll("\\s+", " ")

    assert(interactionLaw.contains("### Pin-3 Negative Corpus"))
    Vector(
      "Pin-3 opens only the negative corpus for the narrow `Tactic.Pin` slice.",
      "A line to a king is not enough. Complete pinned relation or silence.",
      "Pin-3 required silent counterexamples:",
      "- legal move absent",
      "- same-board proof absent",
      "- slider is not a slider",
      "- no king behind target",
      "- target and king are not same side",
      "- line does not continue through target to king",
      "- target is king",
      "- another blocker is between slider and king",
      "- pin-looking geometry but no post-move relation",
      "- discovered attack only",
      "- skewer-looking position classified as Pin",
      "- mate wording",
      "- king safety wording",
      "- pressure wording",
      "Pin-3 forbidden openings:",
      "- new Pin writer behavior",
      "- broad AbsPin or RelPin family",
      "- Skewer",
      "- DiscoveredAttack expansion",
      "- Material claim",
      "- Defense ownership",
      "- RemoveGuard ownership",
      "- ExplanationPlan mapping",
      "- renderer wording",
      "- LLM smoke",
      "- public route `200`",
      "- production API",
      "Completion standard: Pin-looking rows stay silent unless complete StoryProof and complete PinProof prove one legal move creates or reveals one pinned-to-king relation over one non-king target."
    ).foreach: pinScope =>
      assert(interactionLaw.contains(pinScope), s"Pin-3 scope must pin: $pinScope")

    Vector(readme, ssot, architecture).foreach: doc =>
      assert(doc.contains("Pin-3 opens only the negative corpus for the narrow `Tactic.Pin` slice."))
      assert(
        doc.contains(
          "Pin-3 keeps illegal moves, missing same-board proof, non-sliders, missing king-behind-target relation, wrong-side king relation, broken target-to-king line, king targets, extra blockers, stale pin-looking geometry, discovered-only lines, skewer-looking lines, and mate, king-safety, or pressure wording silent."
        )
      )
      assert(doc.contains("A line to a king is not enough. Complete pinned relation or silence."))
      assert(
        doc.contains(
          "Public route `200`, production API, and public/user-facing LLM narration remain closed."
        )
      )
    assert(normalizedModelContract.contains("Pin-3 is owned by `StoryInteractionLaw.md`."))
    assert(
      normalizedModelContract.contains(
        "Pin-3 opens only the negative corpus for the narrow `Tactic.Pin` slice."
      )
    )
    assert(
      normalizedModelContract.contains(
        "Pin-3 keeps illegal moves, missing same-board proof, non-sliders, missing king-behind-target relation, wrong-side king relation, broken target-to-king line, king targets, extra blockers, stale pin-looking geometry, discovered-only lines, skewer-looking lines, and mate, king-safety, or pressure wording silent."
      )
    )
    assert(
      normalizedModelContract.contains("A line to a king is not enough. Complete pinned relation or silence.")
    )

  test("Pin-4 EngineCheck reuse keeps engine evidence internal for Pin"):
    val readme = Files.readString(docsRoot.resolve("README.md"))
    val ssot = Files.readString(docsRoot.resolve("ChessCommentarySSOT.md"))
    val architecture = Files.readString(docsRoot.resolve("ChessModelArchitecture.md"))
    val modelContract = Files.readString(docsRoot.resolve("ChessModelContract.md"))
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))
    val normalizedModelContract = modelContract.replaceAll("\\s+", " ")

    assert(interactionLaw.contains("### Pin-4 EngineCheck Reuse"))
    Vector(
      "Pin-4 opens only existing `EngineCheck` reuse for existing `Tactic.Pin` Stories.",
      "EngineCheck must not create Pin.",
      "`Supports` creates no new Pin claim.",
      "`Caps` suppresses allowed claim or weakens expression to bounded strength when downstream speech opens.",
      "`Refutes` blocks the Pin Story.",
      "`Unknown` creates no engine expression.",
      "Pin-4 forbidden openings:",
      "- engine says",
      "- best move",
      "- only move",
      "- winning tactic",
      "- forced win",
      "- raw PV explanation",
      "- eval number public value",
      "- new EngineCheck type",
      "- Pin from engine evidence",
      "- ExplanationPlan mapping",
      "- renderer wording",
      "- LLM smoke",
      "- public route `200`",
      "- production API",
      "Completion standard: Existing EngineCheck may only support, cap, or refute an already proof-backed `Tactic.Pin` Story; it never creates Pin, never ranks by raw eval or raw PV, and never adds engine wording or stronger tactic wording."
    ).foreach: pinScope =>
      assert(interactionLaw.contains(pinScope), s"Pin-4 scope must pin: $pinScope")

    Vector(readme, ssot, architecture).foreach: doc =>
      assert(doc.contains("Pin-4 opens only existing `EngineCheck` reuse for existing `Tactic.Pin` Stories."))
      assert(
        doc.contains(
          "EngineCheck cannot create Pin; Supports creates no new claim; Caps suppresses allowed claim or keeps downstream speech bounded when opened later; Refutes blocks the Pin Story; Unknown creates no engine expression."
        )
      )
      assert(
        doc.contains(
          "Pin-4 opens no engine-says wording, best-move wording, only-move wording, winning tactic, forced win, raw PV explanation, eval number public value, ExplanationPlan mapping, renderer wording, LLM smoke, public route `200`, production API, or public/user-facing LLM narration."
        )
      )
      assert(
        doc.contains(
          "Public route `200`, production API, and public/user-facing LLM narration remain closed."
        )
      )
    assert(normalizedModelContract.contains("Pin-4 is owned by `StoryInteractionLaw.md`."))
    assert(
      normalizedModelContract.contains(
        "Pin-4 opens only existing `EngineCheck` reuse for existing `Tactic.Pin` Stories."
      )
    )
    assert(
      normalizedModelContract.contains(
        "EngineCheck cannot create Pin; Supports creates no new claim; Caps suppresses allowed claim or keeps downstream speech bounded when opened later; Refutes blocks the Pin Story; Unknown creates no engine expression."
      )
    )
    assert(
      normalizedModelContract.contains(
        "Pin-4 opens no engine-says wording, best-move wording, only-move wording, winning tactic, forced win, raw PV explanation, eval number public value, ExplanationPlan mapping, renderer wording, LLM smoke, public route `200`, production API, or public/user-facing LLM narration."
      )
    )

  test("Pin-5 StoryTable integration keeps Pin claim ownership bounded"):
    val readme = Files.readString(docsRoot.resolve("README.md"))
    val ssot = Files.readString(docsRoot.resolve("ChessCommentarySSOT.md"))
    val architecture = Files.readString(docsRoot.resolve("ChessModelArchitecture.md"))
    val modelContract = Files.readString(docsRoot.resolve("ChessModelContract.md"))
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))
    val normalizedModelContract = modelContract.replaceAll("\\s+", " ")

    assert(interactionLaw.contains("### Pin-5 StoryTable Integration"))
    Vector(
      "Pin-5 opens only StoryTable integration for existing `Tactic.Hanging`, `Tactic.Fork`, `Scene.Material`, `Scene.Defense`, `Tactic.DiscoveredAttack`, and `Tactic.Pin` rows.",
      "Pin-5 StoryTable checks:",
      "- selected Verdict remains stable when input order changes",
      "- Pin does not own Material claim",
      "- Pin does not own king safety claim",
      "- DiscoveredAttack and Pin on the same line do not both become Lead",
      "- actual material change now remains owned by Scene.Material",
      "- Defense creates no defense claim without complete ThreatProof and complete DefenseProof",
      "Pin-5 forbidden openings:",
      "- new Pin proof home",
      "- new Story family",
      "- broad AbsPin or RelPin family",
      "- Material claim from Pin",
      "- king safety claim from Pin",
      "- Defense claim from incomplete Defense rows",
      "- duplicate Lead for same-line DiscoveredAttack and Pin",
      "- ExplanationPlan mapping",
      "- renderer wording",
      "- LLM smoke",
      "- public route `200`",
      "- production API",
      "Completion standard: StoryTable orders existing open rows with Pin deterministically, keeps one selected Lead, and keeps Material, Defense, DiscoveredAttack, and Pin claim homes separate."
    ).foreach: pinScope =>
      assert(interactionLaw.contains(pinScope), s"Pin-5 scope must pin: $pinScope")

    Vector(readme, ssot, architecture).foreach: doc =>
      assert(
        doc.contains(
          "Pin-5 opens only StoryTable integration for existing `Tactic.Hanging`, `Tactic.Fork`, `Scene.Material`, `Scene.Defense`, `Tactic.DiscoveredAttack`, and `Tactic.Pin` rows."
        )
      )
      assert(
        doc.contains(
          "Pin-5 keeps selected Verdict stable across input order, prevents duplicate Lead for same-line DiscoveredAttack and Pin, keeps Pin from owning Material or king-safety claims, keeps actual material change in Scene.Material, and gives Defense no claim without complete ThreatProof and DefenseProof."
        )
      )
      assert(
        doc.contains(
          "Pin-5 opens no new Pin proof home, new Story family, broad AbsPin or RelPin family, Material claim from Pin, king-safety claim from Pin, Defense claim from incomplete Defense rows, ExplanationPlan mapping, renderer wording, LLM smoke, public route `200`, production API, or public/user-facing LLM narration."
        )
      )
      assert(
        doc.contains(
          "Public route `200`, production API, and public/user-facing LLM narration remain closed."
        )
      )
    assert(normalizedModelContract.contains("Pin-5 is owned by `StoryInteractionLaw.md`."))
    assert(
      normalizedModelContract.contains(
        "Pin-5 opens only StoryTable integration for existing `Tactic.Hanging`, `Tactic.Fork`, `Scene.Material`, `Scene.Defense`, `Tactic.DiscoveredAttack`, and `Tactic.Pin` rows."
      )
    )
    assert(
      normalizedModelContract.contains(
        "Pin-5 keeps selected Verdict stable across input order, prevents duplicate Lead for same-line DiscoveredAttack and Pin, keeps Pin from owning Material or king-safety claims, keeps actual material change in Scene.Material, and gives Defense no claim without complete ThreatProof and DefenseProof."
      )
    )
    assert(
      normalizedModelContract.contains(
        "Pin-5 opens no new Pin proof home, new Story family, broad AbsPin or RelPin family, Material claim from Pin, king-safety claim from Pin, Defense claim from incomplete Defense rows, ExplanationPlan mapping, renderer wording, LLM smoke, public route `200`, production API, or public/user-facing LLM narration."
      )
    )

  test("Pin-6 ExplanationPlan opens only bounded pins_piece for selected Pin Lead"):
    val readme = Files.readString(docsRoot.resolve("README.md"))
    val ssot = Files.readString(docsRoot.resolve("ChessCommentarySSOT.md"))
    val architecture = Files.readString(docsRoot.resolve("ChessModelArchitecture.md"))
    val modelContract = Files.readString(docsRoot.resolve("ChessModelContract.md"))
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))
    val normalizedModelContract = modelContract.replaceAll("\\s+", " ")

    assert(interactionLaw.contains("### Pin-6 ExplanationPlan"))
    Vector(
      "Pin-6 opens only ExplanationPlan mapping for selected uncapped Lead `Tactic.Pin` Verdicts.",
      "Pin-6 ExplanationPlan input is selected uncapped Lead Verdict only.",
      "Pin-6 allowed claim key:",
      "- pins_piece",
      "Pin-6 forbidden claim keys:",
      "- wins_material",
      "- king_unsafe",
      "- mate_threat",
      "- best_move",
      "- only_move",
      "- forced",
      "- decisive",
      "- creates_pressure",
      "- takes_initiative",
      "- cannot_move",
      "Support, Context, Blocked, capped, and refuted Pin rows create no standalone claim.",
      "Pin-6 forbidden openings:",
      "- wins_material claim",
      "- king_unsafe claim",
      "- mate_threat claim",
      "- best_move claim",
      "- only_move claim",
      "- forced claim",
      "- decisive claim",
      "- creates_pressure claim",
      "- takes_initiative claim",
      "- cannot_move wording",
      "- renderer wording",
      "- LLM smoke",
      "- public route `200`",
      "- production API",
      "Completion standard: selected uncapped Lead `Tactic.Pin` Verdicts may lower only to bounded `pins_piece`; all non-Lead, capped, refuted, and unselected Pin rows remain without standalone claim."
    ).foreach: pinScope =>
      assert(interactionLaw.contains(pinScope), s"Pin-6 scope must pin: $pinScope")

    Vector(readme, ssot, architecture).foreach: doc =>
      assert(
        doc.contains(
          "Pin-6 opens only ExplanationPlan mapping for selected uncapped Lead `Tactic.Pin` Verdicts."
        )
      )
      assert(
        doc.contains(
          "Pin-6 allows only the `pins_piece` claim key and forbids wins_material, king_unsafe, mate_threat, best_move, only_move, forced, decisive, creates_pressure, takes_initiative, and cannot_move."
        )
      )
      assert(
        doc.contains("Support, Context, Blocked, capped, and refuted Pin rows create no standalone claim.")
      )
      assert(
        doc.contains(
          "Pin-6 opens no renderer wording, LLM smoke, public route `200`, production API, or public/user-facing LLM narration."
        )
      )
      assert(
        doc.contains(
          "Public route `200`, production API, and public/user-facing LLM narration remain closed."
        )
      )
    assert(normalizedModelContract.contains("Pin-6 is owned by `StoryInteractionLaw.md`."))
    assert(
      normalizedModelContract.contains(
        "Pin-6 opens only ExplanationPlan mapping for selected uncapped Lead `Tactic.Pin` Verdicts."
      )
    )
    assert(
      normalizedModelContract.contains(
        "Pin-6 allows only the `pins_piece` claim key and forbids wins_material, king_unsafe, mate_threat, best_move, only_move, forced, decisive, creates_pressure, takes_initiative, and cannot_move."
      )
    )
    assert(
      normalizedModelContract.contains(
        "Support, Context, Blocked, capped, and refuted Pin rows create no standalone claim."
      )
    )
    assert(
      normalizedModelContract.contains(
        "Pin-6 opens no renderer wording, LLM smoke, public route `200`, production API, or public/user-facing LLM narration."
      )
    )

  test("Pin-7 deterministic renderer opens only bounded Pin template"):
    val readme = Files.readString(docsRoot.resolve("README.md"))
    val ssot = Files.readString(docsRoot.resolve("ChessCommentarySSOT.md"))
    val architecture = Files.readString(docsRoot.resolve("ChessModelArchitecture.md"))
    val modelContract = Files.readString(docsRoot.resolve("ChessModelContract.md"))
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))
    val normalizedModelContract = modelContract.replaceAll("\\s+", " ")

    assert(interactionLaw.contains("### Pin-7 Deterministic Renderer"))
    Vector(
      "Pin-7 opens only deterministic renderer text for selected `Tactic.Pin` ExplanationPlan.",
      "Pin-7 renderer input is `ExplanationPlan` only.",
      "Pin-7 allowed renderer template:",
      "- `{route} pins the piece on {target}.`",
      "Pin-7 forbidden renderer wording:",
      "- cannot move",
      "- the king is unsafe",
      "- wins material",
      "- winning",
      "- decisive",
      "- best move",
      "- only move",
      "- forces",
      "- creates pressure",
      "- threatens mate",
      "Pin-7 forbidden openings:",
      "- raw Verdict input",
      "- raw Story input",
      "- PinProof input",
      "- LineFact input",
      "- LineProof input",
      "- EngineCheck input",
      "- proofFailures input",
      "- LLM smoke",
      "- public route `200`",
      "- production API",
      "Completion standard: Renderer phrases only selected bounded `pins_piece` ExplanationPlan data and refuses wording stronger than the Pin-6 claim boundary."
    ).foreach: pinScope =>
      assert(interactionLaw.contains(pinScope), s"Pin-7 scope must pin: $pinScope")

    Vector(readme, ssot, architecture).foreach: doc =>
      assert(
        doc.contains(
          "Pin-7 opens only deterministic renderer text for selected `Tactic.Pin` ExplanationPlan."
        )
      )
      assert(
        doc.contains(
          "Pin-7 renderer input is ExplanationPlan only and may render `{route} pins the piece on {target}.`"
        )
      )
      assert(
        doc.contains(
          "Pin-7 forbids cannot move, the king is unsafe, wins material, winning, decisive, best move, only move, forces, creates pressure, and threatens mate."
        )
      )
      assert(
        doc.contains(
          "Pin-7 opens no raw Verdict, raw Story, PinProof, LineFact, LineProof, EngineCheck, proofFailures, LLM smoke, public route `200`, production API, or public/user-facing LLM narration."
        )
      )
      assert(
        doc.contains(
          "Public route `200`, production API, and public/user-facing LLM narration remain closed."
        )
      )
    assert(normalizedModelContract.contains("Pin-7 is owned by `StoryInteractionLaw.md`."))
    assert(
      normalizedModelContract.contains(
        "Pin-7 opens only deterministic renderer text for selected `Tactic.Pin` ExplanationPlan."
      )
    )
    assert(
      normalizedModelContract.contains(
        "Pin-7 renderer input is ExplanationPlan only and may render `{route} pins the piece on {target}.`"
      )
    )
    assert(
      normalizedModelContract.contains(
        "Pin-7 forbids cannot move, the king is unsafe, wins material, winning, decisive, best move, only move, forces, creates pressure, and threatens mate."
      )
    )
    assert(
      normalizedModelContract.contains(
        "Pin-7 opens no raw Verdict, raw Story, PinProof, LineFact, LineProof, EngineCheck, proofFailures, LLM smoke, public route `200`, production API, or public/user-facing LLM narration."
      )
    )

  test("Pin-8 LLM smoke reuses 8B prompt contract for selected Pin RenderedLine"):
    val readme = Files.readString(docsRoot.resolve("README.md"))
    val ssot = Files.readString(docsRoot.resolve("ChessCommentarySSOT.md"))
    val architecture = Files.readString(docsRoot.resolve("ChessModelArchitecture.md"))
    val modelContract = Files.readString(docsRoot.resolve("ChessModelContract.md"))
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))
    val normalizedModelContract = modelContract.replaceAll("\\s+", " ")

    assert(interactionLaw.contains("### Pin-8 LLM Smoke"))
    Vector(
      "Pin-8 opens only LLM smoke for selected Pin ExplanationPlan and RenderedLine.",
      "Pin-8 reuses only the existing 8B Codex CLI prompt smoke contract.",
      "Pin-8 LLM smoke input:",
      "- renderedText",
      "- claimKey",
      "- strength",
      "- forbidden wording",
      "- `Rephrase only. Do not add chess facts.`",
      "Pin-8 forbidden inputs and additions:",
      "- raw Story",
      "- raw PinProof",
      "- raw LineProof",
      "- BoardFacts",
      "- EngineCheck",
      "- raw PV",
      "- proofFailures",
      "- new move",
      "- new line",
      "- mate claim",
      "- pressure claim",
      "- initiative claim",
      "- winning claim",
      "- cannot-move claim",
      "Pin-8 forbidden openings:",
      "- public/user-facing LLM narration",
      "- public route `200`",
      "- production API",
      "- raw proof repair",
      "- engine explanation",
      "Completion standard: LLM smoke may receive only renderedText, claimKey, strength, forbidden wording, and `Rephrase only. Do not add chess facts.` for selected Pin RenderedLine; it rejects raw proof/board/engine inputs, new moves, new lines, and mate, pressure, initiative, winning, or cannot-move claims."
    ).foreach: pinScope =>
      assert(interactionLaw.contains(pinScope), s"Pin-8 scope must pin: $pinScope")

    Vector(readme, ssot, architecture).foreach: doc =>
      assert(doc.contains("Pin-8 opens only LLM smoke for selected Pin ExplanationPlan and RenderedLine."))
      assert(
        doc.contains(
          "Pin-8 reuses only the existing 8B Codex CLI prompt smoke contract with renderedText, claimKey, strength, forbidden wording, and `Rephrase only. Do not add chess facts.`"
        )
      )
      assert(
        doc.contains(
          "Pin-8 forbids raw Story, raw PinProof, raw LineProof, BoardFacts, EngineCheck, raw PV, proofFailures, new move, new line, mate, pressure, initiative, winning, and cannot-move claims."
        )
      )
      assert(
        doc.contains(
          "Pin-8 opens no public/user-facing LLM narration, public route `200`, production API, raw proof repair, or engine explanation."
        )
      )
      assert(
        doc.contains(
          "Public route `200`, production API, and public/user-facing LLM narration remain closed."
        )
      )
    assert(normalizedModelContract.contains("Pin-8 is owned by `StoryInteractionLaw.md`."))
    assert(
      normalizedModelContract.contains(
        "Pin-8 opens only LLM smoke for selected Pin ExplanationPlan and RenderedLine."
      )
    )
    assert(
      normalizedModelContract.contains(
        "Pin-8 reuses only the existing 8B Codex CLI prompt smoke contract with renderedText, claimKey, strength, forbidden wording, and `Rephrase only. Do not add chess facts.`"
      )
    )
    assert(
      normalizedModelContract.contains(
        "Pin-8 forbids raw Story, raw PinProof, raw LineProof, BoardFacts, EngineCheck, raw PV, proofFailures, new move, new line, mate, pressure, initiative, winning, and cannot-move claims."
      )
    )
    assert(
      normalizedModelContract.contains(
        "Pin-8 opens no public/user-facing LLM narration, public route `200`, production API, raw proof repair, or engine explanation."
      )
    )

  test("Pin Closeout hard cleanup keeps Pin authority separated and public surfaces closed"):
    val readme = Files.readString(docsRoot.resolve("README.md"))
    val ssot = Files.readString(docsRoot.resolve("ChessCommentarySSOT.md"))
    val architecture = Files.readString(docsRoot.resolve("ChessModelArchitecture.md"))
    val modelContract = Files.readString(docsRoot.resolve("ChessModelContract.md"))
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))
    val normalizedModelContract = modelContract.replaceAll("\\s+", " ")

    assert(interactionLaw.contains("### Pin Closeout Hard Cleanup"))
    Vector(
      "Pin Closeout opens no new chess meaning. It only audits the Pin hard cleanup surface.",
      "Pin Closeout must confirm:",
      "- LineFact observes geometry only.",
      "- LineProof binds line evidence only and does not own Pin speech.",
      "- PinProof proves only the pinned-to-king relation.",
      "- Tactic.Pin owns only the proof-backed Story identity.",
      "- `pins_piece` owns only the bounded speech claim key.",
      "- Pin does not own Material, Defense, DiscoveredAttack, Skewer, or RemoveGuard meaning.",
      "- Broad Line, Ray, XRay, and broad Pin family terms are not live public authority for this slice.",
      "- Renderer and LLM smoke cannot create wording stronger than `pins_piece`.",
      "- Test helpers are not promoted into runtime authority.",
      "- Detailed Pin authority lives only in `StoryInteractionLaw.md`; AGENTS.md, README.md, ChessCommentarySSOT.md, ChessModelArchitecture.md, and ChessModelContract.md may summarize only.",
      "- Public route `200`, production API, and public/user-facing LLM narration remain closed.",
      "Pin Closeout opens no broad LineTactic, broad Ray tactic, XRay public Story, broad AbsPin or RelPin family, Skewer, RemoveGuard, Material claim, Defense claim, DiscoveredAttack expansion, pressure, initiative, mate threat, cannot-move wording, best-move, only-move, forced-line, winning, decisive, new proof home, new Story family, renderer wording beyond Pin-7, LLM input beyond Pin-8, public route `200`, production API, or public/user-facing LLM narration.",
      "Completion standard: Pin closes as a narrow proof-backed pinned-to-king slice only, with LineFact, LineProof, PinProof, Tactic.Pin, StoryTable, ExplanationPlan, Renderer, and LLM smoke keeping separate authority and no downstream layer speaking beyond selected proof-backed `pins_piece`."
    ).foreach: pinScope =>
      assert(interactionLaw.contains(pinScope), s"Pin Closeout scope must pin: $pinScope")

    Vector(readme, ssot, architecture).foreach: doc =>
      assert(
        doc.contains(
          "Pin Closeout opens no new chess meaning; it audits only LineFact, LineProof, PinProof, Tactic.Pin, speech key, renderer/LLM, docs authority, test-helper boundary, and closed public surfaces."
        )
      )
      assert(
        doc.contains(
          "Pin Closeout confirms Pin owns no Material, Defense, DiscoveredAttack, Skewer, or RemoveGuard meaning; broad Line, Ray, XRay, and broad Pin family terms are not live authority; renderer/LLM wording stays no stronger than `pins_piece`; public route `200`, production API, and public/user-facing LLM narration remain closed."
        )
      )
      assert(
        !doc.contains("Pin Closeout must confirm:"),
        "summary docs must not duplicate detailed Pin Closeout checklist"
      )
      assert(
        doc.contains(
          "Public route `200`, production API, and public/user-facing LLM narration remain closed."
        )
      )
    assert(normalizedModelContract.contains("Pin Closeout opens no new chess meaning."))
    assert(
      normalizedModelContract.contains(
        "It audits only that LineFact observes geometry, LineProof binds line evidence, PinProof proves one pinned-to-king relation, Tactic.Pin owns the Story identity, `pins_piece` owns speech vocabulary, renderer/LLM stay no stronger than the selected ExplanationPlan, test helpers are not runtime authority, detailed Pin closeout authority stays in `StoryInteractionLaw.md`, and public route `200`, production API, and public/user-facing LLM narration remain closed."
      )
    )
    assert(
      !normalizedModelContract.contains("Pin Closeout must confirm:"),
      "model contract must summarize, not duplicate detailed Pin Closeout checklist"
    )

  test("RemoveGuard-0 charter opens only one defender removed from one target"):
    val readme = Files.readString(docsRoot.resolve("README.md"))
    val ssot = Files.readString(docsRoot.resolve("ChessCommentarySSOT.md"))
    val architecture = Files.readString(docsRoot.resolve("ChessModelArchitecture.md"))
    val modelContract = Files.readString(docsRoot.resolve("ChessModelContract.md"))
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))
    val normalizedModelContract = modelContract.replaceAll("\\s+", " ")

    assert(interactionLaw.contains("### RemoveGuard-0 Charter"))
    Vector(
      "Current implementation scope is Line / Defender Contact Neighborhood.",
      "RemoveGuard-0 opens only the charter for the third narrow line/defender contact vertical slice.",
      "First RemoveGuard positive scope is not a broad remove-the-guard motif.",
      "RemoveGuard first scope: a legal move removes one defender from one non-king material target.",
      "First runtime positive path stays centered on defender capture when possible.",
      "Deflection is allowed only when exact-board proof immediately after the same move shows the defender no longer guards the target.",
      "BoardFacts observes guard relation.",
      "RemoveGuardProof proves the guard was removed.",
      "Tactic.RemoveGuard may speak only after proof.",
      "RemoveGuard-0 allowed opening:",
      "- narrow `Tactic.RemoveGuard`",
      "- one non-king material target",
      "- one defender",
      "- one legal move that removes the defender guard relation",
      "- bounded remove-guard wording after selected Verdict only",
      "RemoveGuard-0 forbidden openings:",
      "- broad deflection tactic",
      "- overloaded defender theory",
      "- discovered attack expansion",
      "- Pin expansion",
      "- Skewer expansion",
      "- XRay expansion",
      "- material win claim",
      "- winning",
      "- decisive",
      "- forced",
      "- best move",
      "- only move",
      "- no defense",
      "- refutes defense",
      "- collapses position",
      "- pressure",
      "- initiative",
      "- public route `200`",
      "- production API",
      "- public/user-facing LLM narration",
      "RemoveGuard-0 opens no broad deflection tactic, overloaded defender theory, discovered attack expansion, Pin/Skewer/XRay expansion, material win claim, winning, decisive, forced, best move, only move, no defense, refutes defense, collapses position, pressure, initiative, public route `200`, production API, or public/user-facing LLM narration.",
      "Completion standard: RemoveGuard-0 keeps remove-guard work at charter scope and opens no RemoveGuardProof runtime, no Tactic.RemoveGuard writer, no StoryTable integration, no ExplanationPlan mapping, no renderer wording, no LLM smoke, no public route `200`, and no production API."
    ).foreach: removeGuardScope =>
      assert(interactionLaw.contains(removeGuardScope), s"RemoveGuard-0 scope must pin: $removeGuardScope")

    Vector(readme, ssot, architecture).foreach: doc =>
      assert(doc.contains("Current implementation scope is Line / Defender Contact Neighborhood."))
      assert(
        doc.contains(
          "RemoveGuard-0 opens only the charter for the third narrow line/defender contact vertical slice."
        )
      )
      assert(doc.contains("First RemoveGuard positive scope is not a broad remove-the-guard motif."))
      assert(
        doc.contains(
          "RemoveGuard first scope: a legal move removes one defender from one non-king material target."
        )
      )
      assert(
        doc.contains(
          "First runtime positive path stays centered on defender capture when possible; deflection is allowed only when exact-board proof immediately after the same move shows the defender no longer guards the target."
        )
      )
      assert(
        doc.contains(
          "BoardFacts observes guard relation. RemoveGuardProof proves the guard was removed. Tactic.RemoveGuard may speak only after proof."
        )
      )
      assert(
        doc.contains(
          "RemoveGuard-0 opens only narrow Tactic.RemoveGuard, one non-king material target, one defender, one legal move that removes the defender guard relation, and bounded remove-guard wording after selected Verdict only."
        )
      )
      assert(
        doc.contains(
          "RemoveGuard-0 opens no broad deflection tactic, overloaded defender theory, discovered attack expansion, Pin/Skewer/XRay expansion, material win claim, winning, decisive, forced, best move, only move, no defense, refutes defense, collapses position, pressure, initiative, public route `200`, production API, or public/user-facing LLM narration."
        )
      )
      assert(
        doc.contains(
          "Public route `200`, production API, and public/user-facing LLM narration remain closed."
        )
      )
    assert(normalizedModelContract.contains("RemoveGuard-0 is owned by `StoryInteractionLaw.md`."))
    assert(
      normalizedModelContract.contains(
        "RemoveGuard-0 opens only the charter for the third narrow line/defender contact vertical slice."
      )
    )
    assert(
      normalizedModelContract.contains(
        "The first positive RemoveGuard scope is not a broad remove-the-guard motif."
      )
    )
    assert(
      normalizedModelContract.contains(
        "The RemoveGuard first scope is a legal move that removes one defender from one non-king material target."
      )
    )
    assert(
      normalizedModelContract.contains(
        "First runtime positive path stays centered on defender capture when possible; deflection is allowed only when exact-board proof immediately after the same move shows the defender no longer guards the target."
      )
    )
    assert(
      normalizedModelContract.contains(
        "BoardFacts observes guard relation, RemoveGuardProof proves the guard was removed, and Tactic.RemoveGuard may speak only after proof."
      )
    )
    assert(
      normalizedModelContract.contains(
        "RemoveGuard-0 opens only narrow Tactic.RemoveGuard, one non-king material target, one defender, one legal move that removes the defender guard relation, and bounded remove-guard wording after selected Verdict only."
      )
    )
    assert(
      normalizedModelContract.contains(
        "RemoveGuard-0 opens no broad deflection tactic, overloaded defender theory, discovered attack expansion, Pin/Skewer/XRay expansion, material win claim, winning, decisive, forced, best move, only move, no defense, refutes defense, collapses position, pressure, initiative, public route `200`, production API, or public/user-facing LLM narration."
      )
    )

  test("RemoveGuard-1 RemoveGuardProof opens only the removed guard relation proof home"):
    val readme = Files.readString(docsRoot.resolve("README.md"))
    val ssot = Files.readString(docsRoot.resolve("ChessCommentarySSOT.md"))
    val architecture = Files.readString(docsRoot.resolve("ChessModelArchitecture.md"))
    val modelContract = Files.readString(docsRoot.resolve("ChessModelContract.md"))
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))
    val normalizedModelContract = modelContract.replaceAll("\\s+", " ")

    assert(interactionLaw.contains("### RemoveGuard-1 RemoveGuardProof"))
    Vector(
      "RemoveGuard-1 opens only `RemoveGuardProof` as a narrow proof home.",
      "RemoveGuardProof must prove:",
      "- side removing the guard",
      "- rival side",
      "- guarded target",
      "- removed defender",
      "- legal remove-guard move",
      "- target is non-king material piece",
      "- defender guarded target before move",
      "- after move defender no longer guards target",
      "- same-board proof",
      "- exact-board after-move relation",
      "RemoveGuard-1 first allowed removal kind:",
      "- DefenderCaptured",
      "RemoveGuard-1 conditional removal kind:",
      "- GuardLineBlocked, only when one legal move blocks a slider defender guard line and exact-board proof shows the defender no longer guards the target",
      "RemoveGuard-1 closed removal kinds:",
      "- opponent-reply deflection",
      "- sacrifice lure",
      "- overloaded defender",
      "- remove guard by long tactic sequence",
      "- defender cannot defend general theory",
      "RemoveGuardProof is not a public Story.",
      "RemoveGuardProof owns no material result.",
      "RemoveGuardProof proof failure text must not become renderer or LLM input.",
      "RemoveGuard-1 forbidden openings:",
      "- Tactic.RemoveGuard writer",
      "- StoryTable integration",
      "- ExplanationPlan mapping",
      "- renderer wording",
      "- LLM smoke",
      "- material win claim",
      "- winning",
      "- decisive",
      "- forced",
      "- best move",
      "- only move",
      "- no defense",
      "- refutes defense",
      "- pressure",
      "- initiative",
      "- public route `200`",
      "- production API",
      "Completion standard: `RemoveGuardProof` proves only that one legal same-board move removes one defender guard relation from one non-king material target, while RemoveGuardProof, proof failures, renderer, LLM, public route `200`, and production API remain non-speaking."
    ).foreach: removeGuardScope =>
      assert(interactionLaw.contains(removeGuardScope), s"RemoveGuard-1 scope must pin: $removeGuardScope")

    Vector(readme, ssot, architecture).foreach: doc =>
      assert(doc.contains("RemoveGuard-1 opens only `RemoveGuardProof` as a narrow proof home."))
      assert(
        doc.contains(
          "RemoveGuardProof proves side removing the guard, rival side, guarded target, removed defender, legal remove-guard move, target non-king material piece, defender guarded target before move, after move defender no longer guards target, same-board proof, and exact-board after-move relation."
        )
      )
      assert(
        doc.contains(
          "RemoveGuard-1 permits DefenderCaptured first and GuardLineBlocked only when one legal move blocks a slider defender guard line and exact-board proof shows the defender no longer guards the target."
        )
      )
      assert(
        doc.contains(
          "RemoveGuard-1 keeps opponent-reply deflection, sacrifice lure, overloaded defender, remove guard by long tactic sequence, and defender-cannot-defend general theory closed."
        )
      )
      assert(
        doc.contains(
          "RemoveGuardProof is not a public Story and owns no material result; proofFailures stay out of renderer/LLM input."
        )
      )
      assert(
        doc.contains(
          "RemoveGuard-1 opens no Tactic.RemoveGuard writer, StoryTable integration, ExplanationPlan mapping, renderer wording, LLM smoke, material win claim, public route `200`, production API, or public/user-facing LLM narration."
        )
      )
      assert(
        doc.contains(
          "Public route `200`, production API, and public/user-facing LLM narration remain closed."
        )
      )
    assert(normalizedModelContract.contains("- `RemoveGuardProof`"))
    assert(normalizedModelContract.contains("- `RemoveGuardRemovalKind`"))
    assert(normalizedModelContract.contains("RemoveGuard-1 is owned by `StoryInteractionLaw.md`."))
    assert(
      normalizedModelContract.contains("RemoveGuard-1 opens only `RemoveGuardProof` as a narrow proof home.")
    )
    assert(
      normalizedModelContract.contains(
        "RemoveGuardProof proves side removing the guard, rival side, guarded target, removed defender, legal remove-guard move, target non-king material piece, defender guarded target before move, after move defender no longer guards target, same-board proof, and exact-board after-move relation."
      )
    )
    assert(
      normalizedModelContract.contains(
        "RemoveGuard-1 permits DefenderCaptured first and GuardLineBlocked only when one legal move blocks a slider defender guard line and exact-board proof shows the defender no longer guards the target."
      )
    )
    assert(
      normalizedModelContract.contains(
        "RemoveGuard-1 keeps opponent-reply deflection, sacrifice lure, overloaded defender, remove guard by long tactic sequence, and defender-cannot-defend general theory closed."
      )
    )
    assert(
      normalizedModelContract.contains(
        "RemoveGuardProof is not a public Story and owns no material result, and proofFailures stay out of renderer/LLM input."
      )
    )
    assert(
      normalizedModelContract.contains(
        "RemoveGuard-1 opens no Tactic.RemoveGuard writer, StoryTable integration, ExplanationPlan mapping, renderer wording, LLM smoke, material win claim, public route `200`, production API, or public/user-facing LLM narration."
      )
    )

  test("RemoveGuard-2 TacticRemoveGuard writer opens only proof-backed Tactic.RemoveGuard Story identity"):
    val readme = Files.readString(docsRoot.resolve("README.md"))
    val ssot = Files.readString(docsRoot.resolve("ChessCommentarySSOT.md"))
    val architecture = Files.readString(docsRoot.resolve("ChessModelArchitecture.md"))
    val modelContract = Files.readString(docsRoot.resolve("ChessModelContract.md"))
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))
    val normalizedModelContract = modelContract.replaceAll("\\s+", " ")

    assert(interactionLaw.contains("### RemoveGuard-2 Tactic.RemoveGuard Writer"))
    Vector(
      "RemoveGuard-2 opens only the named `TacticRemoveGuard` writer for one narrow `Tactic.RemoveGuard` Story.",
      "TacticRemoveGuard writer conditions:",
      "- complete StoryProof",
      "- complete RemoveGuardProof",
      "- same-board legal replay",
      "- legal remove-guard move",
      "- guarded target exists",
      "- removed defender existed and guarded target before move",
      "- defender no longer guards target after move",
      "- writer = TacticRemoveGuard",
      "- EngineCheck does not Refute",
      "RemoveGuard-2 Story identity:",
      "- tactic = RemoveGuard",
      "- scene = Tactic",
      "- side = guard-removing side",
      "- rival = target/defender side",
      "- target = guarded target square",
      "- anchor = removed defender square or moving piece square",
      "- route = remove-guard move",
      "RemoveGuard-2 opened runtime pieces:",
      "- `Tactic.RemoveGuard` tactic identity",
      "- `StoryWriter.TacticRemoveGuard` writer identity",
      "- `TacticRemoveGuard.write`",
      "- `Story.removeGuardProof`",
      "- StoryTable admission for complete non-refuted `Tactic.RemoveGuard` rows",
      "RemoveGuard-2 forbidden openings:",
      "- Scene.Material claim",
      "- Tactic.Hanging replacement",
      "- Defense refutation wording",
      "- material win claim",
      "- winning",
      "- decisive",
      "- forced",
      "- best move",
      "- only move",
      "- no defense",
      "- pressure",
      "- initiative",
      "- ExplanationPlan mapping",
      "- renderer wording",
      "- LLM smoke",
      "- public route `200`",
      "- production API",
      "- public/user-facing LLM narration",
      "Completion standard: `Tactic.RemoveGuard` may become a Story only through complete StoryProof plus complete RemoveGuardProof for one legal move removing one defender guard relation from one non-king material target, while Material, Hanging, Defense-refutation wording, ExplanationPlan, renderer, LLM, public route `200`, and production API remain closed."
    ).foreach: removeGuardScope =>
      assert(interactionLaw.contains(removeGuardScope), s"RemoveGuard-2 scope must pin: $removeGuardScope")

    Vector(readme, ssot, architecture).foreach: doc =>
      assert(
        doc.contains(
          "RemoveGuard-2 opens only the named `TacticRemoveGuard` writer for one narrow `Tactic.RemoveGuard` Story."
        )
      )
      assert(
        doc.contains(
          "TacticRemoveGuard requires complete StoryProof, complete RemoveGuardProof, same-board legal replay, legal remove-guard move, guarded target, removed defender that guarded the target before move, defender no longer guards target after move, writer identity, and no EngineCheck Refutes status."
        )
      )
      assert(
        doc.contains(
          "RemoveGuard Story identity is tactic RemoveGuard, scene Tactic, guard-removing side, target/defender-side rival, guarded target square, removed-defender or moving-piece anchor, and remove-guard route."
        )
      )
      assert(
        doc.contains(
          "RemoveGuard-2 opens no Scene.Material claim, Tactic.Hanging replacement, Defense refutation wording, material win claim, ExplanationPlan mapping, renderer wording, LLM smoke, public route `200`, production API, or public/user-facing LLM narration."
        )
      )
      assert(
        doc.contains(
          "Public route `200`, production API, and public/user-facing LLM narration remain closed."
        )
      )
    assert(normalizedModelContract.contains("- `TacticRemoveGuard`"))
    assert(normalizedModelContract.contains("RemoveGuard-2 is owned by `StoryInteractionLaw.md`."))
    assert(
      normalizedModelContract.contains(
        "RemoveGuard-2 opens only the named `TacticRemoveGuard` writer for one narrow `Tactic.RemoveGuard` Story."
      )
    )
    assert(
      normalizedModelContract.contains(
        "TacticRemoveGuard requires complete StoryProof, complete RemoveGuardProof, same-board legal replay, legal remove-guard move, guarded target, removed defender that guarded the target before move, defender no longer guards target after move, writer identity, and no EngineCheck Refutes status."
      )
    )
    assert(
      normalizedModelContract.contains(
        "RemoveGuard Story identity is tactic RemoveGuard, scene Tactic, guard-removing side, target/defender-side rival, guarded target square, removed-defender or moving-piece anchor, and remove-guard route."
      )
    )
    assert(
      normalizedModelContract.contains(
        "RemoveGuard-2 opens no Scene.Material claim, Tactic.Hanging replacement, Defense refutation wording, material win claim, ExplanationPlan mapping, renderer wording, LLM smoke, public route `200`, production API, or public/user-facing LLM narration."
      )
    )

  test("RemoveGuard-3 negative corpus keeps remove-guard false positives silent"):
    val readme = Files.readString(docsRoot.resolve("README.md"))
    val ssot = Files.readString(docsRoot.resolve("ChessCommentarySSOT.md"))
    val architecture = Files.readString(docsRoot.resolve("ChessModelArchitecture.md"))
    val modelContract = Files.readString(docsRoot.resolve("ChessModelContract.md"))
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))
    val normalizedModelContract = modelContract.replaceAll("\\s+", " ")

    assert(interactionLaw.contains("### RemoveGuard-3 Negative Corpus"))
    Vector(
      "RemoveGuard-3 opens only the negative corpus for the narrow `Tactic.RemoveGuard` slice.",
      "RemoveGuard-3 required silent counterexamples:",
      "- legal move missing",
      "- same-board proof missing",
      "- target is king",
      "- defender did not guard target",
      "- defender still guards target after move",
      "- another defender remains and broad claim is attempted",
      "- direct material gain claim",
      "- Pin misclassified as RemoveGuard",
      "- DiscoveredAttack misclassified as RemoveGuard",
      "- Skewer misclassified as RemoveGuard",
      "- opponent-reply deflection",
      "- overloaded defender claim",
      "- no defense wording",
      "- wins material wording",
      "- best move wording",
      "Removing one guard is not winning material. Complete guard-removal proof or silence.",
      "RemoveGuard-3 forbidden openings:",
      "- new proof home",
      "- new writer",
      "- StoryTable ordering change",
      "- Scene.Material claim",
      "- Tactic.Hanging replacement",
      "- Defense refutation wording",
      "- Pin ownership",
      "- DiscoveredAttack ownership",
      "- Skewer ownership",
      "- overloaded defender theory",
      "- broad deflection tactic",
      "- material win claim",
      "- no defense",
      "- wins material",
      "- best move",
      "- ExplanationPlan mapping",
      "- renderer wording",
      "- LLM smoke",
      "- public route `200`",
      "- production API",
      "- public/user-facing LLM narration",
      "Completion standard: RemoveGuard-3 adds only close false-positive corpus coverage; no plausible-looking row may speak unless complete StoryProof plus complete RemoveGuardProof proves one legal same-board move removes one defender guard relation from one non-king material target."
    ).foreach: removeGuardScope =>
      assert(interactionLaw.contains(removeGuardScope), s"RemoveGuard-3 scope must pin: $removeGuardScope")

    Vector(readme, ssot, architecture).foreach: doc =>
      assert(
        doc.contains(
          "RemoveGuard-3 opens only the negative corpus for the narrow `Tactic.RemoveGuard` slice."
        )
      )
      assert(
        doc.contains(
          "RemoveGuard-3 keeps illegal moves, missing same-board proof, king targets, non-guarding defenders, still-guarding defenders, broad claims from other defenders, material-gain claims, Pin/DiscoveredAttack/Skewer misclassification, opponent-reply deflection, overloaded-defender claims, and no-defense, wins-material, and best-move wording silent."
        )
      )
      assert(
        doc.contains("Removing one guard is not winning material. Complete guard-removal proof or silence.")
      )
      assert(
        doc.contains(
          "RemoveGuard-3 opens no new proof home, new writer, StoryTable ordering change, ExplanationPlan mapping, renderer wording, LLM smoke, public route `200`, production API, or public/user-facing LLM narration."
        )
      )
      assert(
        doc.contains(
          "Public route `200`, production API, and public/user-facing LLM narration remain closed."
        )
      )
    assert(normalizedModelContract.contains("RemoveGuard-3 is owned by `StoryInteractionLaw.md`."))
    assert(
      normalizedModelContract.contains(
        "RemoveGuard-3 opens only the negative corpus for the narrow `Tactic.RemoveGuard` slice."
      )
    )
    assert(
      normalizedModelContract.contains(
        "RemoveGuard-3 keeps illegal moves, missing same-board proof, king targets, non-guarding defenders, still-guarding defenders, broad claims from other defenders, material-gain claims, Pin/DiscoveredAttack/Skewer misclassification, opponent-reply deflection, overloaded-defender claims, and no-defense, wins-material, and best-move wording silent."
      )
    )
    assert(
      normalizedModelContract.contains(
        "Removing one guard is not winning material. Complete guard-removal proof or silence."
      )
    )
    assert(
      normalizedModelContract.contains(
        "RemoveGuard-3 opens no new proof home, new writer, StoryTable ordering change, ExplanationPlan mapping, renderer wording, LLM smoke, public route `200`, production API, or public/user-facing LLM narration."
      )
    )

  test("RemoveGuard-4 EngineCheck reuse pins RemoveGuard engine boundary"):
    val readme = Files.readString(docsRoot.resolve("README.md"))
    val ssot = Files.readString(docsRoot.resolve("ChessCommentarySSOT.md"))
    val architecture = Files.readString(docsRoot.resolve("ChessModelArchitecture.md"))
    val modelContract = Files.readString(docsRoot.resolve("ChessModelContract.md"))
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))
    val normalizedModelContract = modelContract.replaceAll("\\s+", " ")

    assert(interactionLaw.contains("### RemoveGuard-4 EngineCheck Reuse"))
    Vector(
      "RemoveGuard-4 opens only existing `EngineCheck` reuse for existing `Tactic.RemoveGuard` Stories.",
      "RemoveGuard-4 EngineCheck rules:",
      "- EngineCheck cannot create RemoveGuard",
      "- Supports creates no new claim",
      "- Caps suppresses standalone claim or weakens expression to bounded strength when downstream speech opens",
      "- Refutes blocks the RemoveGuard Story",
      "- Unknown creates no engine expression",
      "RemoveGuard-4 forbidden openings:",
      "- engine says",
      "- best move",
      "- only move",
      "- winning tactic",
      "- raw PV explanation",
      "- eval number public value",
      "- new EngineCheck type",
      "- RemoveGuard from engine evidence",
      "- ExplanationPlan mapping",
      "- renderer wording",
      "- LLM smoke",
      "- public route `200`",
      "- production API",
      "- public/user-facing LLM narration",
      "Completion standard: Existing EngineCheck may only support, cap, or refute an already proof-backed `Tactic.RemoveGuard` Story; it never creates RemoveGuard, never ranks by raw eval or raw PV, and never adds engine wording or stronger tactic wording."
    ).foreach: removeGuardScope =>
      assert(interactionLaw.contains(removeGuardScope), s"RemoveGuard-4 scope must pin: $removeGuardScope")

    Vector(readme, ssot, architecture).foreach: doc =>
      assert(
        doc.contains(
          "RemoveGuard-4 opens only existing `EngineCheck` reuse for existing `Tactic.RemoveGuard` Stories."
        )
      )
      assert(
        doc.contains(
          "EngineCheck cannot create RemoveGuard; Supports creates no new claim; Caps suppresses standalone claim or keeps downstream speech bounded when opened later; Refutes blocks the RemoveGuard Story; Unknown creates no engine expression."
        )
      )
      assert(
        doc.contains(
          "RemoveGuard-4 opens no engine-says wording, best-move wording, only-move wording, winning tactic, raw PV explanation, eval number public value, ExplanationPlan mapping, renderer wording, LLM smoke, public route `200`, production API, or public/user-facing LLM narration."
        )
      )
      assert(
        doc.contains(
          "Public route `200`, production API, and public/user-facing LLM narration remain closed."
        )
      )
    assert(normalizedModelContract.contains("RemoveGuard-4 is owned by `StoryInteractionLaw.md`."))
    assert(
      normalizedModelContract.contains(
        "RemoveGuard-4 opens only existing `EngineCheck` reuse for existing `Tactic.RemoveGuard` Stories."
      )
    )
    assert(
      normalizedModelContract.contains(
        "EngineCheck cannot create RemoveGuard; Supports creates no new claim; Caps suppresses standalone claim or keeps downstream speech bounded when opened later; Refutes blocks the RemoveGuard Story; Unknown creates no engine expression."
      )
    )
    assert(
      normalizedModelContract.contains(
        "RemoveGuard-4 opens no engine-says wording, best-move wording, only-move wording, winning tactic, raw PV explanation, eval number public value, ExplanationPlan mapping, renderer wording, LLM smoke, public route `200`, production API, or public/user-facing LLM narration."
      )
    )

  test("RemoveGuard-5 StoryTable integration keeps RemoveGuard claim ownership bounded"):
    val readme = Files.readString(docsRoot.resolve("README.md"))
    val ssot = Files.readString(docsRoot.resolve("ChessCommentarySSOT.md"))
    val architecture = Files.readString(docsRoot.resolve("ChessModelArchitecture.md"))
    val modelContract = Files.readString(docsRoot.resolve("ChessModelContract.md"))
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))
    val normalizedModelContract = modelContract.replaceAll("\\s+", " ")

    assert(interactionLaw.contains("### RemoveGuard-5 StoryTable Integration"))
    Vector(
      "RemoveGuard-5 opens only StoryTable integration for existing `Tactic.Hanging`, `Tactic.Fork`, `Scene.Material`, `Scene.Defense`, `Tactic.DiscoveredAttack`, `Tactic.Pin`, and `Tactic.RemoveGuard` rows.",
      "RemoveGuard-5 StoryTable checks:",
      "- selected Verdict remains stable when input order changes",
      "- RemoveGuard does not own Material claim",
      "- RemoveGuard does not replace Hanging claim",
      "- Defense creates no response claim without complete ThreatProof and complete DefenseProof",
      "- Pin and RemoveGuard on the same defender or line do not both become Lead",
      "- actual material change now remains owned by Scene.Material",
      "RemoveGuard-5 forbidden openings:",
      "- new RemoveGuard proof home",
      "- new Story family",
      "- Material claim from RemoveGuard",
      "- Hanging claim from RemoveGuard",
      "- Defense response from incomplete Defense rows",
      "- duplicate Lead for same-line Pin and RemoveGuard",
      "- ExplanationPlan mapping",
      "- renderer wording",
      "- LLM smoke",
      "- public route `200`",
      "- production API",
      "- public/user-facing LLM narration",
      "Completion standard: StoryTable orders existing open rows with RemoveGuard deterministically, keeps one selected Lead, and keeps Material, Hanging, Defense, Pin, DiscoveredAttack, Fork, and RemoveGuard claim homes separate."
    ).foreach: removeGuardScope =>
      assert(interactionLaw.contains(removeGuardScope), s"RemoveGuard-5 scope must pin: $removeGuardScope")

    Vector(readme, ssot, architecture).foreach: doc =>
      assert(
        doc.contains(
          "RemoveGuard-5 opens only StoryTable integration for existing `Tactic.Hanging`, `Tactic.Fork`, `Scene.Material`, `Scene.Defense`, `Tactic.DiscoveredAttack`, `Tactic.Pin`, and `Tactic.RemoveGuard` rows."
        )
      )
      assert(
        doc.contains(
          "RemoveGuard-5 keeps selected Verdict stable across input order, keeps RemoveGuard from owning Material or Hanging claims, blocks incomplete Defense responses, prevents duplicate Lead for same-line Pin and RemoveGuard, and keeps actual material change now in Scene.Material."
        )
      )
      assert(
        doc.contains(
          "RemoveGuard-5 opens no new proof home, new Story family, Material claim from RemoveGuard, Hanging claim from RemoveGuard, incomplete Defense response claim, ExplanationPlan mapping, renderer wording, LLM smoke, public route `200`, production API, or public/user-facing LLM narration."
        )
      )
      assert(
        doc.contains(
          "Public route `200`, production API, and public/user-facing LLM narration remain closed."
        )
      )
    assert(normalizedModelContract.contains("RemoveGuard-5 is owned by `StoryInteractionLaw.md`."))
    assert(
      normalizedModelContract.contains(
        "RemoveGuard-5 opens only StoryTable integration for existing `Tactic.Hanging`, `Tactic.Fork`, `Scene.Material`, `Scene.Defense`, `Tactic.DiscoveredAttack`, `Tactic.Pin`, and `Tactic.RemoveGuard` rows."
      )
    )
    assert(
      normalizedModelContract.contains(
        "RemoveGuard-5 keeps selected Verdict stable across input order, keeps RemoveGuard from owning Material or Hanging claims, blocks incomplete Defense responses, prevents duplicate Lead for same-line Pin and RemoveGuard, and keeps actual material change now in Scene.Material."
      )
    )
    assert(
      normalizedModelContract.contains(
        "RemoveGuard-5 opens no new proof home, new Story family, Material claim from RemoveGuard, Hanging claim from RemoveGuard, incomplete Defense response claim, ExplanationPlan mapping, renderer wording, LLM smoke, public route `200`, production API, or public/user-facing LLM narration."
      )
    )

  test("RemoveGuard-6 ExplanationPlan opens only bounded removes_defender for selected RemoveGuard Lead"):
    val readme = Files.readString(docsRoot.resolve("README.md"))
    val ssot = Files.readString(docsRoot.resolve("ChessCommentarySSOT.md"))
    val architecture = Files.readString(docsRoot.resolve("ChessModelArchitecture.md"))
    val modelContract = Files.readString(docsRoot.resolve("ChessModelContract.md"))
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))
    val normalizedModelContract = modelContract.replaceAll("\\s+", " ")

    assert(interactionLaw.contains("### RemoveGuard-6 ExplanationPlan"))
    Vector(
      "RemoveGuard-6 opens only ExplanationPlan mapping for selected uncapped Lead `Tactic.RemoveGuard` Verdicts.",
      "RemoveGuard-6 ExplanationPlan input is selected uncapped Lead Verdict only.",
      "RemoveGuard-6 allowed claim key:",
      "- removes_defender",
      "RemoveGuard-6 forbidden claim keys:",
      "- wins_material",
      "- target_is_hanging",
      "- no_defense",
      "- refutes_defense",
      "- best_move",
      "- only_move",
      "- forced",
      "- decisive",
      "- creates_pressure",
      "- takes_initiative",
      "Support, Context, Blocked, capped, and refuted RemoveGuard rows create no standalone claim.",
      "RemoveGuard-6 forbidden openings:",
      "- wins_material claim",
      "- target_is_hanging claim",
      "- no_defense claim",
      "- refutes_defense claim",
      "- best_move claim",
      "- only_move claim",
      "- forced claim",
      "- decisive claim",
      "- creates_pressure claim",
      "- takes_initiative claim",
      "- renderer wording",
      "- LLM smoke",
      "- public route `200`",
      "- production API",
      "- public/user-facing LLM narration",
      "Completion standard: selected uncapped Lead `Tactic.RemoveGuard` Verdicts may lower only to bounded `removes_defender`; all non-Lead, capped, refuted, and unselected RemoveGuard rows remain without standalone claim."
    ).foreach: removeGuardScope =>
      assert(interactionLaw.contains(removeGuardScope), s"RemoveGuard-6 scope must pin: $removeGuardScope")

    Vector(readme, ssot, architecture).foreach: doc =>
      assert(
        doc.contains(
          "RemoveGuard-6 opens only ExplanationPlan mapping for selected uncapped Lead `Tactic.RemoveGuard` Verdicts."
        )
      )
      assert(
        doc.contains(
          "RemoveGuard-6 allows only the `removes_defender` claim key and forbids wins_material, target_is_hanging, no_defense, refutes_defense, best_move, only_move, forced, decisive, creates_pressure, and takes_initiative."
        )
      )
      assert(
        doc.contains(
          "Support, Context, Blocked, capped, and refuted RemoveGuard rows create no standalone claim."
        )
      )
      assert(
        doc.contains(
          "RemoveGuard-6 opens no renderer wording, LLM smoke, public route `200`, production API, or public/user-facing LLM narration."
        )
      )
      assert(
        doc.contains(
          "Public route `200`, production API, and public/user-facing LLM narration remain closed."
        )
      )
    assert(normalizedModelContract.contains("RemoveGuard-6 is owned by `StoryInteractionLaw.md`."))
    assert(
      normalizedModelContract.contains(
        "RemoveGuard-6 opens only ExplanationPlan mapping for selected uncapped Lead `Tactic.RemoveGuard` Verdicts."
      )
    )
    assert(
      normalizedModelContract.contains(
        "RemoveGuard-6 allows only the `removes_defender` claim key and forbids wins_material, target_is_hanging, no_defense, refutes_defense, best_move, only_move, forced, decisive, creates_pressure, and takes_initiative."
      )
    )
    assert(
      normalizedModelContract.contains(
        "Support, Context, Blocked, capped, and refuted RemoveGuard rows create no standalone claim."
      )
    )
    assert(
      normalizedModelContract.contains(
        "RemoveGuard-6 opens no renderer wording, LLM smoke, public route `200`, production API, or public/user-facing LLM narration."
      )
    )

  test("RemoveGuard-7 deterministic renderer opens only bounded RemoveGuard template"):
    val readme = Files.readString(docsRoot.resolve("README.md"))
    val ssot = Files.readString(docsRoot.resolve("ChessCommentarySSOT.md"))
    val architecture = Files.readString(docsRoot.resolve("ChessModelArchitecture.md"))
    val modelContract = Files.readString(docsRoot.resolve("ChessModelContract.md"))
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))
    val normalizedModelContract = modelContract.replaceAll("\\s+", " ")

    assert(interactionLaw.contains("### RemoveGuard-7 Deterministic Renderer"))
    Vector(
      "RemoveGuard-7 opens only deterministic renderer text for selected `Tactic.RemoveGuard` ExplanationPlan.",
      "RemoveGuard-7 renderer input is `ExplanationPlan` only.",
      "RemoveGuard-7 allowed renderer template:",
      "- `{route} removes the defender of the piece on {target}.`",
      "RemoveGuard-7 forbidden renderer wording:",
      "- wins material",
      "- leaves it undefended",
      "- no defender remains",
      "- best move",
      "- only move",
      "- forces",
      "- decisive",
      "- refutes the defense",
      "- creates pressure",
      "RemoveGuard-7 forbidden openings:",
      "- raw Verdict input",
      "- raw Story input",
      "- RemoveGuardProof input",
      "- BoardFacts input",
      "- EngineCheck input",
      "- proofFailures input",
      "- LLM smoke",
      "- public route `200`",
      "- production API",
      "- public/user-facing LLM narration",
      "Completion standard: Renderer phrases only selected bounded `removes_defender` ExplanationPlan data and refuses wording stronger than the RemoveGuard-6 claim boundary."
    ).foreach: removeGuardScope =>
      assert(interactionLaw.contains(removeGuardScope), s"RemoveGuard-7 scope must pin: $removeGuardScope")

    Vector(readme, ssot, architecture).foreach: doc =>
      assert(
        doc.contains(
          "RemoveGuard-7 opens only deterministic renderer text for selected `Tactic.RemoveGuard` ExplanationPlan."
        )
      )
      assert(
        doc.contains(
          "RemoveGuard-7 renderer input is ExplanationPlan only and may render `{route} removes the defender of the piece on {target}.`"
        )
      )
      assert(
        doc.contains(
          "RemoveGuard-7 forbids wins material, leaves it undefended, no defender remains, best move, only move, forces, decisive, refutes the defense, and creates pressure."
        )
      )
      assert(
        doc.contains(
          "RemoveGuard-7 opens no raw Verdict, raw Story, RemoveGuardProof, BoardFacts, EngineCheck, proofFailures, LLM smoke, public route `200`, production API, or public/user-facing LLM narration."
        )
      )
      assert(
        doc.contains(
          "Public route `200`, production API, and public/user-facing LLM narration remain closed."
        )
      )
    assert(normalizedModelContract.contains("RemoveGuard-7 is owned by `StoryInteractionLaw.md`."))
    assert(
      normalizedModelContract.contains(
        "RemoveGuard-7 opens only deterministic renderer text for selected `Tactic.RemoveGuard` ExplanationPlan."
      )
    )
    assert(
      normalizedModelContract.contains(
        "RemoveGuard-7 renderer input is ExplanationPlan only and may render `{route} removes the defender of the piece on {target}.`"
      )
    )
    assert(
      normalizedModelContract.contains(
        "RemoveGuard-7 forbids wins material, leaves it undefended, no defender remains, best move, only move, forces, decisive, refutes the defense, and creates pressure."
      )
    )
    assert(
      normalizedModelContract.contains(
        "RemoveGuard-7 opens no raw Verdict, raw Story, RemoveGuardProof, BoardFacts, EngineCheck, proofFailures, LLM smoke, public route `200`, production API, or public/user-facing LLM narration."
      )
    )

  test("RemoveGuard-8 LLM smoke reuses 8B prompt contract for selected RemoveGuard RenderedLine"):
    val readme = Files.readString(docsRoot.resolve("README.md"))
    val ssot = Files.readString(docsRoot.resolve("ChessCommentarySSOT.md"))
    val architecture = Files.readString(docsRoot.resolve("ChessModelArchitecture.md"))
    val modelContract = Files.readString(docsRoot.resolve("ChessModelContract.md"))
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))
    val normalizedModelContract = modelContract.replaceAll("\\s+", " ")

    assert(interactionLaw.contains("### RemoveGuard-8 LLM Smoke"))
    Vector(
      "RemoveGuard-8 opens only LLM smoke for selected RemoveGuard ExplanationPlan and RenderedLine.",
      "RemoveGuard-8 reuses only the existing 8B Codex CLI prompt smoke contract.",
      "RemoveGuard-8 LLM smoke input:",
      "- renderedText",
      "- claimKey",
      "- strength",
      "- forbidden wording",
      "- `Rephrase only. Do not add chess facts.`",
      "RemoveGuard-8 forbidden inputs and additions:",
      "- raw Story",
      "- raw RemoveGuardProof",
      "- BoardFacts",
      "- EngineCheck",
      "- raw PV",
      "- proofFailures",
      "- new move",
      "- new line",
      "- material win claim",
      "- no-defense claim",
      "- pressure claim",
      "- initiative claim",
      "RemoveGuard-8 forbidden openings:",
      "- public/user-facing LLM narration",
      "- public route `200`",
      "- production API",
      "- raw proof repair",
      "- engine explanation",
      "Completion standard: LLM smoke may receive only renderedText, claimKey, strength, forbidden wording, and `Rephrase only. Do not add chess facts.` for selected RemoveGuard RenderedLine; it rejects raw proof/board/engine inputs, new moves, new lines, and material-win, no-defense, pressure, or initiative claims."
    ).foreach: removeGuardScope =>
      assert(interactionLaw.contains(removeGuardScope), s"RemoveGuard-8 scope must pin: $removeGuardScope")

    Vector(readme, ssot, architecture).foreach: doc =>
      assert(
        doc.contains(
          "RemoveGuard-8 opens only LLM smoke for selected RemoveGuard ExplanationPlan and RenderedLine."
        )
      )
      assert(
        doc.contains(
          "RemoveGuard-8 reuses only the existing 8B Codex CLI prompt smoke contract with renderedText, claimKey, strength, forbidden wording, and `Rephrase only. Do not add chess facts.`"
        )
      )
      assert(
        doc.contains(
          "RemoveGuard-8 forbids raw Story, raw RemoveGuardProof, BoardFacts, EngineCheck, raw PV, proofFailures, new move, new line, material-win, no-defense, pressure, and initiative claims."
        )
      )
      assert(
        doc.contains(
          "RemoveGuard-8 opens no public/user-facing LLM narration, public route `200`, production API, raw proof repair, or engine explanation."
        )
      )
      assert(
        doc.contains(
          "Public route `200`, production API, and public/user-facing LLM narration remain closed."
        )
      )
    assert(normalizedModelContract.contains("RemoveGuard-8 is owned by `StoryInteractionLaw.md`."))
    assert(
      normalizedModelContract.contains(
        "RemoveGuard-8 opens only LLM smoke for selected RemoveGuard ExplanationPlan and RenderedLine."
      )
    )
    assert(
      normalizedModelContract.contains(
        "RemoveGuard-8 reuses only the existing 8B Codex CLI prompt smoke contract with renderedText, claimKey, strength, forbidden wording, and `Rephrase only. Do not add chess facts.`"
      )
    )
    assert(
      normalizedModelContract.contains(
        "RemoveGuard-8 forbids raw Story, raw RemoveGuardProof, BoardFacts, EngineCheck, raw PV, proofFailures, new move, new line, material-win, no-defense, pressure, and initiative claims."
      )
    )
    assert(
      normalizedModelContract.contains(
        "RemoveGuard-8 opens no public/user-facing LLM narration, public route `200`, production API, raw proof repair, or engine explanation."
      )
    )

  test("RemoveGuard Closeout hard cleanup keeps authority separated and public surfaces closed"):
    val readme = Files.readString(docsRoot.resolve("README.md"))
    val ssot = Files.readString(docsRoot.resolve("ChessCommentarySSOT.md"))
    val architecture = Files.readString(docsRoot.resolve("ChessModelArchitecture.md"))
    val modelContract = Files.readString(docsRoot.resolve("ChessModelContract.md"))
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))
    val normalizedModelContract = modelContract.replaceAll("\\s+", " ")

    assert(interactionLaw.contains("### RemoveGuard Closeout Hard Cleanup"))
    Vector(
      "RemoveGuard Closeout opens no new chess meaning. It only audits the RemoveGuard hard cleanup surface.",
      "RemoveGuard Closeout must confirm:",
      "- BoardFacts guard relation observes only same-side guard contact.",
      "- RemoveGuardProof proves only that one defender guard relation was removed from one non-king material target.",
      "- Tactic.RemoveGuard owns only the proof-backed Story identity.",
      "- `removes_defender` owns only the bounded speech claim key.",
      "- RemoveGuard does not own Material, Hanging, Defense, Pin, or DiscoveredAttack meaning.",
      "- Deflection, overload, no-defender, and wins-material terms are not live public authority for this slice.",
      "- Renderer and LLM smoke cannot create wording stronger than `removes_defender`.",
      "- Test helpers are not promoted into runtime authority.",
      "- Detailed RemoveGuard authority lives only in `StoryInteractionLaw.md`; AGENTS.md, README.md, ChessCommentarySSOT.md, ChessModelArchitecture.md, and ChessModelContract.md may summarize only.",
      "- Public route `200`, production API, and public/user-facing LLM narration remain closed.",
      "RemoveGuard Closeout opens no broad deflection tactic, overloaded defender theory, no-defender claim, wins-material claim, Material claim, Hanging claim, Defense claim, Pin expansion, DiscoveredAttack expansion, Skewer, XRay, pressure, initiative, best-move, only-move, forced-line, winning, decisive, new proof home, new Story family, renderer wording beyond RemoveGuard-7, LLM input beyond RemoveGuard-8, public route `200`, production API, or public/user-facing LLM narration.",
      "Completion standard: RemoveGuard closes as a narrow proof-backed guard-removal slice only, with BoardFacts guard relation, RemoveGuardProof, Tactic.RemoveGuard, StoryTable, ExplanationPlan, Renderer, and LLM smoke keeping separate authority and no downstream layer speaking beyond selected proof-backed `removes_defender`."
    ).foreach: removeGuardScope =>
      assert(
        interactionLaw.contains(removeGuardScope),
        s"RemoveGuard Closeout scope must pin: $removeGuardScope"
      )

    assert(interactionLaw.contains("| RemoveGuardProof | `Tactic.RemoveGuard` |"))
    assert(
      !interactionLaw.contains("| DefenderProof | `Tactic.RemoveGuard`"),
      "RemoveGuard must not remain under broad DefenderProof authority"
    )
    assert(interactionLaw.contains("| W14 | `Tactic.RemoveGuard` | RemoveGuardProof plus StoryProof"))
    assert(
      interactionLaw.contains(
        "RemoveGuardProof admitted for narrow `Tactic.RemoveGuard`; broader DefenderProof for overload, deflection, and decoy remains closed."
      )
    )

    Vector(readme, ssot, architecture).foreach: doc =>
      assert(
        doc.contains(
          "RemoveGuard Closeout opens no new chess meaning; it audits only BoardFacts guard relation, RemoveGuardProof, Tactic.RemoveGuard, speech key, renderer/LLM, docs authority, test-helper boundary, and closed public surfaces."
        )
      )
      assert(
        doc.contains(
          "RemoveGuard Closeout confirms RemoveGuard owns no Material, Hanging, Defense, Pin, or DiscoveredAttack meaning; deflection, overload, no-defender, and wins-material terms are not live authority; renderer/LLM wording stays no stronger than `removes_defender`; public route `200`, production API, and public/user-facing LLM narration remain closed."
        )
      )
      assert(
        !doc.contains("RemoveGuard Closeout must confirm:"),
        "summary docs must not duplicate detailed RemoveGuard Closeout checklist"
      )
      assert(
        doc.contains(
          "Public route `200`, production API, and public/user-facing LLM narration remain closed."
        )
      )
    assert(normalizedModelContract.contains("RemoveGuard Closeout opens no new chess meaning."))
    assert(
      normalizedModelContract.contains(
        "It audits only that BoardFacts guard relation observes same-side guard contact, RemoveGuardProof proves one removed guard relation, Tactic.RemoveGuard owns the Story identity, `removes_defender` owns speech vocabulary, RemoveGuard owns no Material, Hanging, Defense, Pin, or DiscoveredAttack meaning, deflection, overload, no-defender, and wins-material terms are not live public authority, renderer/LLM stay no stronger than the selected ExplanationPlan, test helpers are not runtime authority, detailed RemoveGuard closeout authority stays in `StoryInteractionLaw.md`, and public route `200`, production API, and public/user-facing LLM narration remain closed."
      )
    )
    assert(
      !normalizedModelContract.contains("RemoveGuard Closeout must confirm:"),
      "model contract must summarize, not duplicate detailed RemoveGuard Closeout checklist"
    )

  test("LDH-0 charter pins Line Defender Interaction Hardening scope"):
    val readme = Files.readString(docsRoot.resolve("README.md"))
    val ssot = Files.readString(docsRoot.resolve("ChessCommentarySSOT.md"))
    val architecture = Files.readString(docsRoot.resolve("ChessModelArchitecture.md"))
    val modelContract = Files.readString(docsRoot.resolve("ChessModelContract.md"))
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))
    val normalizedModelContract = modelContract.replaceAll("\\s+", " ")

    assert(interactionLaw.contains("## Line / Defender Interaction Hardening"))
    assert(interactionLaw.contains("### LDH-0 Charter"))
    Vector(
      "Line/Defender hardening opens no new Story. It proves that existing line and defender Stories do not steal each other's meaning.",
      "LDH-0 opens only existing Line/Defender rows interaction hardening.",
      "LDH-0 opens only complex same-board fixture checks.",
      "LDH-0 opens only StoryTable role stability checks.",
      "LDH-0 opens only downstream no-overclaim smoke.",
      "LDH-0 may apply only the minimum StoryTable ordering fix if an existing DiscoveredAttack ordering bug is exposed.",
      "Allowed LDH-0 line and defender rows:",
      "- Tactic.DiscoveredAttack",
      "- Tactic.Pin",
      "- Tactic.RemoveGuard",
      "Allowed LDH-0 collision targets:",
      "- Tactic.Hanging",
      "- Tactic.Fork",
      "- Scene.Material",
      "- Scene.Defense",
      "LDH-0 forbidden openings:",
      "- Tactic.Skewer",
      "- Tactic.XRay",
      "- broad LineTactic",
      "- broad deflection",
      "- overloaded defender",
      "- pressure",
      "- initiative",
      "- mate threat",
      "- king safety",
      "- material win claim",
      "- public route 200",
      "- production API",
      "- public/user-facing LLM narration"
    ).foreach: ldhLine =>
      assert(interactionLaw.contains(ldhLine), s"LDH-0 must pin: $ldhLine")

    Vector(readme, ssot, architecture).foreach: doc =>
      assert(doc.contains("Line / Defender Interaction Hardening is a closed baseline."))
      assert(
        doc.contains(
          "Line/Defender hardening opens no new Story. It proves that existing line and defender Stories do not steal each other's meaning."
        )
      )
      assert(
        doc.contains(
          "LDH-0 opens only existing Line/Defender rows interaction hardening, complex same-board fixtures, StoryTable role stability, downstream no-overclaim smoke, and the minimum StoryTable ordering fix if an existing DiscoveredAttack ordering bug is exposed."
        )
      )
      assert(
        doc.contains(
          "LDH-0 opens no Skewer, XRay, broad LineTactic, broad deflection, overloaded defender, pressure, initiative, mate threat, king safety, material win claim, public route `200`, production API, or public/user-facing LLM narration."
        )
      )
      assert(
        !doc.contains("LDH-0 allowed line and defender rows:"),
        "summary docs must not duplicate detailed LDH-0 checklist"
      )
    assert(normalizedModelContract.contains("Line / Defender Interaction Hardening opens no new Story."))
    assert(
      normalizedModelContract.contains(
        "It proves that existing DiscoveredAttack, Pin, and RemoveGuard rows do not steal each other's meaning or the existing Hanging, Fork, Material, and Defense claim homes."
      )
    )
    assert(
      normalizedModelContract.contains(
        "LDH-0 may apply only the minimum StoryTable ordering fix if an existing DiscoveredAttack ordering bug is exposed."
      )
    )

  test("LDH-1 fixture map pins required same-board interaction fixtures"):
    val readme = Files.readString(docsRoot.resolve("README.md"))
    val ssot = Files.readString(docsRoot.resolve("ChessCommentarySSOT.md"))
    val architecture = Files.readString(docsRoot.resolve("ChessModelArchitecture.md"))
    val modelContract = Files.readString(docsRoot.resolve("ChessModelContract.md"))
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))
    val normalizedModelContract = modelContract.replaceAll("\\s+", " ")

    assert(interactionLaw.contains("### LDH-1 Fixture Map"))
    Vector(
      "LDH-1 opens only complex same-board Fixture Map coverage.",
      "Each LDH-1 fixture must state:",
      "- same-board FEN",
      "- side to move",
      "- candidate_passer legal lines",
      "- expected open rows",
      "- expected blocked rows",
      "- expected Lead / Support / Context / Blocked role",
      "- expected selected Verdict",
      "- forbidden claims",
      "Required LDH-1 fixture categories:",
      "- DiscoveredAttack vs Pin",
      "- DiscoveredAttack vs RemoveGuard",
      "- Pin vs RemoveGuard",
      "- DiscoveredAttack + Pin + RemoveGuard same-board",
      "- Line/Defender row vs Material",
      "- Line/Defender row vs Hanging",
      "- Line/Defender row vs Defense",
      "- EngineCheck Supports/Caps/Refutes over existing Line/Defender rows",
      "LDH-1 fixture map forbids:",
      "- expecting a Skewer-looking fixture as positive Skewer",
      "- using `wins material`, `best move`, `pressure`, or `initiative` as expected output",
      "- using proofFailures text as public expected output"
    ).foreach: ldhLine =>
      assert(interactionLaw.contains(ldhLine), s"LDH-1 must pin: $ldhLine")

    Vector(readme, ssot, architecture).foreach: doc =>
      assert(
        doc.contains(
          "LDH-1 opens only the Fixture Map for complex same-board Line/Defender interaction fixtures."
        )
      )
      assert(
        doc.contains(
          "LDH-1 fixtures must state same-board FEN, side to move, candidate legal lines, expected open rows, expected blocked rows, expected Lead/Support/Context/Blocked role, expected selected Verdict, and forbidden claims."
        )
      )
      assert(
        doc.contains(
          "LDH-1 required fixture categories are DiscoveredAttack vs Pin, DiscoveredAttack vs RemoveGuard, Pin vs RemoveGuard, DiscoveredAttack + Pin + RemoveGuard same-board, Line/Defender row vs Material, Line/Defender row vs Hanging, Line/Defender row vs Defense, and EngineCheck Supports/Caps/Refutes over existing Line/Defender rows."
        )
      )
      assert(
        doc.contains(
          "LDH-1 opens no Skewer, XRay, broad LineTactic, broad deflection, overloaded defender, pressure, initiative, mate threat, king safety, material win claim, public route `200`, production API, or public/user-facing LLM narration."
        )
      )
      assert(
        !doc.contains("Each LDH-1 fixture must state:"),
        "summary docs must not duplicate detailed LDH-1 checklist"
      )

    assert(
      normalizedModelContract.contains(
        "LDH-1 opens only the Fixture Map for complex same-board Line/Defender interaction fixtures."
      )
    )
    assert(
      normalizedModelContract.contains(
        "Each fixture states same-board FEN, side to move, candidate legal lines, expected open rows, expected blocked rows, expected Lead, Support, Context, or Blocked role, expected selected Verdict, and forbidden claims."
      )
    )
    assert(
      normalizedModelContract.contains(
        "The required LDH-1 categories are DiscoveredAttack vs Pin, DiscoveredAttack vs RemoveGuard, Pin vs RemoveGuard, DiscoveredAttack + Pin + RemoveGuard same-board, Line/Defender row vs Material, Line/Defender row vs Hanging, Line/Defender row vs Defense, and EngineCheck Supports/Caps/Refutes over existing Line/Defender rows."
      )
    )
    assert(
      normalizedModelContract.contains(
        "LDH-1 does not open Skewer, XRay, broad LineTactic, broad deflection, overloaded defender, pressure, initiative, mate threat, king safety, material win claim, public route `200`, production API, or public/user-facing LLM narration."
      )
    )

  test("LDH-2 role stability pins StoryTable Line Defender interaction checks"):
    val readme = Files.readString(docsRoot.resolve("README.md"))
    val ssot = Files.readString(docsRoot.resolve("ChessCommentarySSOT.md"))
    val architecture = Files.readString(docsRoot.resolve("ChessModelArchitecture.md"))
    val modelContract = Files.readString(docsRoot.resolve("ChessModelContract.md"))
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))
    val normalizedModelContract = modelContract.replaceAll("\\s+", " ")

    assert(interactionLaw.contains("### LDH-2 Role Stability"))
    Vector(
      "LDH-2 opens only StoryTable role stability checks over existing Line/Defender rows.",
      "LDH-2 role stability must verify:",
      "- input order changes must keep the same selected Verdict",
      "- same meaning must not become duplicate Lead",
      "- incomplete rows must not become Lead",
      "- refuted rows must become Blocked",
      "- capped rows must create no standalone claim",
      "LDH-2 must specifically check:",
      "- Pin and DiscoveredAttack on the same line must not both become Lead",
      "- RemoveGuard must not own Pin line relation",
      "- DiscoveredAttack must not own RemoveGuard defender relation",
      "LDH-2 opens no Skewer, XRay, broad LineTactic, broad deflection, overloaded defender, pressure, initiative, mate threat, king safety, material win claim, public route `200`, production API, or public/user-facing LLM narration."
    ).foreach: ldhLine =>
      assert(interactionLaw.contains(ldhLine), s"LDH-2 must pin: $ldhLine")

    Vector(readme, ssot, architecture).foreach: doc =>
      assert(
        doc.contains("LDH-2 opens only StoryTable role stability checks over existing Line/Defender rows.")
      )
      assert(
        doc.contains(
          "LDH-2 verifies input-order stable selected Verdicts, no duplicate Lead for the same meaning, incomplete rows not Lead, refuted rows Blocked, capped rows without standalone claims, and separated ownership for same-line Pin/DiscoveredAttack, Pin/RemoveGuard, and DiscoveredAttack/RemoveGuard collisions."
        )
      )
      assert(
        doc.contains(
          "LDH-2 opens no Skewer, XRay, broad LineTactic, broad deflection, overloaded defender, pressure, initiative, mate threat, king safety, material win claim, public route `200`, production API, or public/user-facing LLM narration."
        )
      )
      assert(
        !doc.contains("LDH-2 role stability must verify:"),
        "summary docs must not duplicate detailed LDH-2 checklist"
      )

    assert(
      normalizedModelContract.contains(
        "LDH-2 opens only StoryTable role stability checks over existing Line/Defender rows."
      )
    )
    assert(
      normalizedModelContract.contains(
        "It verifies input-order stable selected Verdicts, no duplicate Lead for the same meaning, incomplete rows not Lead, refuted rows Blocked, capped rows without standalone claims, and separated ownership for same-line Pin/DiscoveredAttack, Pin/RemoveGuard, and DiscoveredAttack/RemoveGuard collisions."
      )
    )
    assert(
      normalizedModelContract.contains(
        "LDH-2 does not open Skewer, XRay, broad LineTactic, broad deflection, overloaded defender, pressure, initiative, mate threat, king safety, material win claim, public route `200`, production API, or public/user-facing LLM narration."
      )
    )

  test("LDH-3 meaning ownership boundary pins each existing row to its own meaning"):
    val readme = Files.readString(docsRoot.resolve("README.md"))
    val ssot = Files.readString(docsRoot.resolve("ChessCommentarySSOT.md"))
    val architecture = Files.readString(docsRoot.resolve("ChessModelArchitecture.md"))
    val modelContract = Files.readString(docsRoot.resolve("ChessModelContract.md"))
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))
    val normalizedModelContract = modelContract.replaceAll("\\s+", " ")

    assert(interactionLaw.contains("### LDH-3 Meaning Ownership Boundary"))
    Vector(
      "LDH-3 opens only Meaning Ownership Boundary checks over existing Line/Defender and collision rows.",
      "LDH-3 owned meanings:",
      "- Tactic.DiscoveredAttack owns only a legal move reveals one slider attack on one material target.",
      "- Tactic.Pin owns only a non-king piece is pinned to its own king on a line.",
      "- Tactic.RemoveGuard owns only one defender no longer guards one target after a legal move.",
      "- Scene.Material owns only actual material balance change now.",
      "- Tactic.Hanging owns only a capturable target with bounded material gain proof.",
      "- Scene.Defense owns only complete ThreatProof plus DefenseProof prevents immediate material loss.",
      "LDH-3 forbidden ownership leaks:",
      "- RemoveGuard must not say material gain.",
      "- Pin must not say cannot-move or king unsafe.",
      "- DiscoveredAttack must not say wins-material.",
      "- Material must not own line tactic identity.",
      "- Defense must not say it stopped the threat without complete threat proof.",
      "LDH-3 opens no Skewer, XRay, broad LineTactic, broad deflection, overloaded defender, pressure, initiative, mate threat, king safety, material win claim, public route `200`, production API, or public/user-facing LLM narration."
    ).foreach: ldhLine =>
      assert(interactionLaw.contains(ldhLine), s"LDH-3 must pin: $ldhLine")

    Vector(readme, ssot, architecture).foreach: doc =>
      assert(
        doc.contains(
          "LDH-3 opens only Meaning Ownership Boundary checks over existing Line/Defender and collision rows."
        )
      )
      assert(
        doc.contains(
          "LDH-3 fixes each row to its own meaning: DiscoveredAttack reveals one slider attack on one material target, Pin pins one non-king piece to its own king on a line, RemoveGuard removes one defender guard relation from one target, Scene.Material owns actual material balance change now, Tactic.Hanging owns capturable target with bounded material gain proof, and Scene.Defense owns complete ThreatProof plus DefenseProof preventing immediate material loss."
        )
      )
      assert(
        doc.contains(
          "LDH-3 forbids RemoveGuard material-gain claims, Pin cannot-move or king-unsafe claims, DiscoveredAttack wins-material claims, Material line-tactic identity, and Defense stopped-threat wording without complete threat proof."
        )
      )
      assert(
        doc.contains(
          "LDH-3 opens no Skewer, XRay, broad LineTactic, broad deflection, overloaded defender, pressure, initiative, mate threat, king safety, material win claim, public route `200`, production API, or public/user-facing LLM narration."
        )
      )
      assert(
        !doc.contains("LDH-3 owned meanings:"),
        "summary docs must not duplicate detailed LDH-3 checklist"
      )

    assert(
      normalizedModelContract.contains(
        "LDH-3 opens only Meaning Ownership Boundary checks over existing Line/Defender and collision rows."
      )
    )
    assert(
      normalizedModelContract.contains(
        "It fixes each row to its own meaning: DiscoveredAttack reveals one slider attack on one material target, Pin pins one non-king piece to its own king on a line, RemoveGuard removes one defender guard relation from one target, Scene.Material owns actual material balance change now, Tactic.Hanging owns capturable target with bounded material gain proof, and Scene.Defense owns complete ThreatProof plus DefenseProof preventing immediate material loss."
      )
    )
    assert(
      normalizedModelContract.contains(
        "LDH-3 forbids RemoveGuard material-gain claims, Pin cannot-move or king-unsafe claims, DiscoveredAttack wins-material claims, Material line-tactic identity, and Defense stopped-threat wording without complete threat proof."
      )
    )
    assert(
      normalizedModelContract.contains(
        "LDH-3 does not open Skewer, XRay, broad LineTactic, broad deflection, overloaded defender, pressure, initiative, mate threat, king safety, material win claim, public route `200`, production API, or public/user-facing LLM narration."
      )
    )

  test("LDH-4 EngineCheck interaction reuses existing statuses without engine-owned public claims"):
    val readme = Files.readString(docsRoot.resolve("README.md"))
    val ssot = Files.readString(docsRoot.resolve("ChessCommentarySSOT.md"))
    val architecture = Files.readString(docsRoot.resolve("ChessModelArchitecture.md"))
    val modelContract = Files.readString(docsRoot.resolve("ChessModelContract.md"))
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))
    val normalizedModelContract = modelContract.replaceAll("\\s+", " ")

    assert(interactionLaw.contains("### LDH-4 EngineCheck Interaction"))
    Vector(
      "LDH-4 opens only existing EngineCheck interaction checks over existing Line/Defender rows.",
      "LDH-4 must verify:",
      "- Supports must not create a new claim.",
      "- Caps must suppress allowed claim or keep downstream speech bounded.",
      "- Refutes must make the checked Story Blocked.",
      "- Unknown must create no engine-related expression.",
      "LDH-4 forbidden public engine wording:",
      "- engine says",
      "- raw PV explanation",
      "- eval number public value",
      "- best move",
      "- only move",
      "- forced line",
      "LDH-4 opens no new EngineCheck proof home, new Story family, engine-says wording, raw PV explanation, eval number public value, best move, only move, forced line, Skewer, XRay, broad LineTactic, broad deflection, overloaded defender, pressure, initiative, mate threat, king safety, material win claim, public route `200`, production API, or public/user-facing LLM narration."
    ).foreach: ldhLine =>
      assert(interactionLaw.contains(ldhLine), s"LDH-4 must pin: $ldhLine")

    Vector(readme, ssot, architecture).foreach: doc =>
      assert(
        doc.contains(
          "LDH-4 opens only existing EngineCheck interaction checks over existing Line/Defender rows."
        )
      )
      assert(
        doc.contains(
          "LDH-4 verifies Supports creates no new claim, Caps suppresses allowed claim or keeps downstream speech bounded, Refutes blocks the checked Story, and Unknown creates no engine-related expression."
        )
      )
      assert(
        doc.contains(
          "LDH-4 forbids engine-says wording, raw PV explanation, eval number public value, best move, only move, forced line, Skewer, XRay, broad LineTactic, broad deflection, overloaded defender, pressure, initiative, mate threat, king safety, material win claim, public route `200`, production API, or public/user-facing LLM narration."
        )
      )
      assert(!doc.contains("LDH-4 must verify:"), "summary docs must not duplicate detailed LDH-4 checklist")

    assert(
      normalizedModelContract.contains(
        "LDH-4 opens only existing EngineCheck interaction checks over existing Line/Defender rows."
      )
    )
    assert(
      normalizedModelContract.contains(
        "It verifies Supports creates no new claim, Caps suppresses allowed claim or keeps downstream speech bounded, Refutes blocks the checked Story, and Unknown creates no engine-related expression."
      )
    )
    assert(
      normalizedModelContract.contains(
        "LDH-4 forbids engine-says wording, raw PV explanation, eval number public value, best move, only move, forced line, Skewer, XRay, broad LineTactic, broad deflection, overloaded defender, pressure, initiative, mate threat, king safety, material win claim, public route `200`, production API, or public/user-facing LLM narration."
      )
    )
    assert(
      normalizedModelContract.contains(
        "LDH-4 does not open a new EngineCheck proof home, new Story family, engine-owned wording, public route `200`, production API, or public/user-facing LLM narration."
      )
    )

  test("LDH-5 negative corpus keeps close line defender false positives silent"):
    val readme = Files.readString(docsRoot.resolve("README.md"))
    val ssot = Files.readString(docsRoot.resolve("ChessCommentarySSOT.md"))
    val architecture = Files.readString(docsRoot.resolve("ChessModelArchitecture.md"))
    val modelContract = Files.readString(docsRoot.resolve("ChessModelContract.md"))
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))
    val normalizedModelContract = modelContract.replaceAll("\\s+", " ")

    assert(interactionLaw.contains("### LDH-5 Negative Corpus"))
    Vector(
      "LDH-5 opens only close false-positive negative corpus tests over existing Line/Defender rows and already-open collision rows.",
      "LDH-5 close false positives must stay silent:",
      "- line opens but no actual attack",
      "- attack appears but target is king",
      "- pin-looking line but no king behind target",
      "- remove-guard-looking move but defender still guards target",
      "- defender removed but Material or Hanging proof is incomplete",
      "- discovered attack and pin both plausible but one proof is incomplete",
      "- wrong-board or stale same-board proof",
      "- route mismatch",
      "- engine refutes plausible row",
      "- Skewer-looking relation tries to enter before Skewer opens",
      "LDH-5 rule: Looks like a line tactic is not enough. Existing complete proof or silence.",
      "LDH-5 opens no new Story family, proof home, Skewer, XRay, broad LineTactic, broad deflection, overloaded defender, pressure, initiative, mate threat, king safety, material win claim, public route `200`, production API, or public/user-facing LLM narration."
    ).foreach: ldhLine =>
      assert(interactionLaw.contains(ldhLine), s"LDH-5 must pin: $ldhLine")

    Vector(readme, ssot, architecture).foreach: doc =>
      assert(
        doc.contains(
          "LDH-5 opens only close false-positive negative corpus tests over existing Line/Defender rows and already-open collision rows."
        )
      )
      assert(
        doc.contains(
          "LDH-5 keeps line-open-without-attack, king targets, pin-looking lines without king-behind-target, still-guarding defenders, incomplete Material or Hanging proof after defender removal, incomplete DiscoveredAttack or Pin proof, wrong-board or stale same-board proof, route mismatch, engine-refuted plausible rows, and Skewer-looking relations silent."
        )
      )
      assert(
        doc.contains(
          "LDH-5 rule: Looks like a line tactic is not enough. Existing complete proof or silence."
        )
      )
      assert(
        doc.contains(
          "LDH-5 opens no new Story family, proof home, Skewer, XRay, broad LineTactic, broad deflection, overloaded defender, pressure, initiative, mate threat, king safety, material win claim, public route `200`, production API, or public/user-facing LLM narration."
        )
      )
      assert(
        !doc.contains("LDH-5 close false positives must stay silent:"),
        "summary docs must not duplicate detailed LDH-5 checklist"
      )

    assert(
      normalizedModelContract.contains(
        "LDH-5 opens only close false-positive negative corpus tests over existing Line/Defender rows and already-open collision rows."
      )
    )
    assert(
      normalizedModelContract.contains(
        "It keeps line-open-without-attack, king targets, pin-looking lines without king-behind-target, still-guarding defenders, incomplete Material or Hanging proof after defender removal, incomplete DiscoveredAttack or Pin proof, wrong-board or stale same-board proof, route mismatch, engine-refuted plausible rows, and Skewer-looking relations silent."
      )
    )
    assert(
      normalizedModelContract.contains(
        "LDH-5 rule: Looks like a line tactic is not enough. Existing complete proof or silence."
      )
    )
    assert(
      normalizedModelContract.contains(
        "LDH-5 does not open a new Story family, proof home, Skewer, XRay, broad LineTactic, broad deflection, overloaded defender, pressure, initiative, mate threat, king safety, material win claim, public route `200`, production API, or public/user-facing LLM narration."
      )
    )

  test("LDH-6 downstream boundary smoke passes only selected Lead Verdict data"):
    val readme = Files.readString(docsRoot.resolve("README.md"))
    val ssot = Files.readString(docsRoot.resolve("ChessCommentarySSOT.md"))
    val architecture = Files.readString(docsRoot.resolve("ChessModelArchitecture.md"))
    val modelContract = Files.readString(docsRoot.resolve("ChessModelContract.md"))
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))
    val normalizedModelContract = modelContract.replaceAll("\\s+", " ")

    assert(interactionLaw.contains("### LDH-6 Downstream Boundary Smoke"))
    Vector(
      "LDH-6 opens only downstream boundary smoke over selected Lead Verdicts from existing Line/Defender rows.",
      "LDH-6 must verify:",
      "- ExplanationPlan input is selected Verdict only.",
      "- Renderer input is ExplanationPlan only.",
      "- LLM smoke input is renderedText, claimKey, strength, forbidden wording, and the rephrase-only instruction only.",
      "- Support, Context, Blocked, capped, and refuted rows create no standalone text.",
      "LDH-6 forbidden downstream inputs or changes:",
      "- no new renderer template",
      "- no new LLM behavior",
      "- no raw Story, Proof, or EngineCheck reaches renderer or LLM smoke",
      "LDH-6 opens no new Story family, proof home, renderer template, LLM behavior, raw Story, Proof, or EngineCheck downstream path, Skewer, XRay, broad LineTactic, broad deflection, overloaded defender, pressure, initiative, mate threat, king safety, material win claim, public route `200`, production API, or public/user-facing LLM narration."
    ).foreach: ldhLine =>
      assert(interactionLaw.contains(ldhLine), s"LDH-6 must pin: $ldhLine")

    Vector(readme, ssot, architecture).foreach: doc =>
      assert(
        doc.contains(
          "LDH-6 opens only downstream boundary smoke over selected Lead Verdicts from existing Line/Defender rows."
        )
      )
      assert(
        doc.contains(
          "LDH-6 verifies ExplanationPlan receives selected Verdict only, Renderer receives ExplanationPlan only, LLM smoke prompt carries only renderedText, claimKey, strength, forbidden wording, and the rephrase-only instruction, and Support, Context, Blocked, capped, and refuted rows create no standalone text."
        )
      )
      assert(
        doc.contains(
          "LDH-6 forbids new renderer templates, new LLM behavior, and raw Story, Proof, or EngineCheck passing into renderer or LLM smoke."
        )
      )
      assert(
        doc.contains(
          "LDH-6 opens no new Story family, proof home, Skewer, XRay, broad LineTactic, broad deflection, overloaded defender, pressure, initiative, mate threat, king safety, material win claim, public route `200`, production API, or public/user-facing LLM narration."
        )
      )
      assert(!doc.contains("LDH-6 must verify:"), "summary docs must not duplicate detailed LDH-6 checklist")
      assert(
        !doc.contains("LDH-6 forbidden downstream inputs or changes:"),
        "summary docs must not duplicate detailed LDH-6 checklist"
      )

    assert(
      normalizedModelContract.contains(
        "LDH-6 opens only downstream boundary smoke over selected Lead Verdicts from existing Line/Defender rows."
      )
    )
    assert(
      normalizedModelContract.contains(
        "It verifies ExplanationPlan receives selected Verdict only, Renderer receives ExplanationPlan only, LLM smoke prompt carries only renderedText, claimKey, strength, forbidden wording, and the rephrase-only instruction, and Support, Context, Blocked, capped, and refuted rows create no standalone text."
      )
    )
    assert(
      normalizedModelContract.contains(
        "LDH-6 forbids new renderer templates, new LLM behavior, and raw Story, Proof, or EngineCheck passing into renderer or LLM smoke."
      )
    )
    assert(
      normalizedModelContract.contains(
        "LDH-6 does not open a new Story family, proof home, renderer template, LLM behavior, raw Story, Proof, or EngineCheck downstream path, Skewer, XRay, broad LineTactic, broad deflection, overloaded defender, pressure, initiative, mate threat, king safety, material win claim, public route `200`, production API, or public/user-facing LLM narration."
      )
    )

  test("LDH-7 diagnostics boundary keeps diagnostics out of public meaning"):
    val readme = Files.readString(docsRoot.resolve("README.md"))
    val ssot = Files.readString(docsRoot.resolve("ChessCommentarySSOT.md"))
    val architecture = Files.readString(docsRoot.resolve("ChessModelArchitecture.md"))
    val modelContract = Files.readString(docsRoot.resolve("ChessModelContract.md"))
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))
    val normalizedModelContract = modelContract.replaceAll("\\s+", " ")

    assert(interactionLaw.contains("### LDH-7 Diagnostics Boundary"))
    Vector(
      "LDH-7 opens only diagnostics boundary smoke over existing Line/Defender rows, StoryTable, selected Verdict, ExplanationPlan, renderer, LLM smoke, and test-helper authority.",
      "LDH-7 must verify:",
      "- proofFailures are internal diagnostic only.",
      "- raw proof text does not enter Verdict.values.",
      "- EngineCheck text does not flow directly into ExplanationPlan.",
      "- StoryTable debug relation does not become renderer wording.",
      "- test helpers do not become runtime authority.",
      "LDH-7 opens no new Story family, proof home, renderer wording, LLM behavior, runtime authority helper, raw Story, raw Proof, raw EngineCheck downstream path, Skewer, XRay, broad LineTactic, broad deflection, overloaded defender, pressure, initiative, mate threat, king safety, material win claim, public route `200`, production API, or public/user-facing LLM narration."
    ).foreach: ldhLine =>
      assert(interactionLaw.contains(ldhLine), s"LDH-7 must pin: $ldhLine")

    Vector(readme, ssot, architecture).foreach: doc =>
      assert(
        doc.contains(
          "LDH-7 opens only diagnostics boundary smoke over existing Line/Defender rows, StoryTable, selected Verdict, ExplanationPlan, renderer, LLM smoke, and test-helper authority."
        )
      )
      assert(
        doc.contains(
          "LDH-7 verifies proofFailures stay internal diagnostic only, raw proof text stays out of Verdict.values, EngineCheck text does not flow directly into ExplanationPlan, StoryTable debug relations do not become renderer wording, and test helpers do not become runtime authority."
        )
      )
      assert(
        doc.contains(
          "LDH-7 opens no new Story family, proof home, renderer wording, LLM behavior, runtime authority helper, raw Story, raw Proof, raw EngineCheck downstream path, Skewer, XRay, broad LineTactic, broad deflection, overloaded defender, pressure, initiative, mate threat, king safety, material win claim, public route `200`, production API, or public/user-facing LLM narration."
        )
      )
      assert(!doc.contains("LDH-7 must verify:"), "summary docs must not duplicate detailed LDH-7 checklist")

    assert(
      normalizedModelContract.contains(
        "LDH-7 opens only diagnostics boundary smoke over existing Line/Defender rows, StoryTable, selected Verdict, ExplanationPlan, renderer, LLM smoke, and test-helper authority."
      )
    )
    assert(
      normalizedModelContract.contains(
        "It verifies proofFailures stay internal diagnostic only, raw proof text stays out of Verdict.values, EngineCheck text does not flow directly into ExplanationPlan, StoryTable debug relations do not become renderer wording, and test helpers do not become runtime authority."
      )
    )
    assert(
      normalizedModelContract.contains(
        "LDH-7 does not open a new Story family, proof home, renderer wording, LLM behavior, runtime authority helper, raw Story, raw Proof, raw EngineCheck downstream path, Skewer, XRay, broad LineTactic, broad deflection, overloaded defender, pressure, initiative, mate threat, king safety, material win claim, public route `200`, production API, or public/user-facing LLM narration."
      )
    )

  test("LDH closeout hard cleanup keeps Line Defender authority separated and public surfaces closed"):
    val readme = Files.readString(docsRoot.resolve("README.md"))
    val ssot = Files.readString(docsRoot.resolve("ChessCommentarySSOT.md"))
    val architecture = Files.readString(docsRoot.resolve("ChessModelArchitecture.md"))
    val modelContract = Files.readString(docsRoot.resolve("ChessModelContract.md"))
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))
    val normalizedModelContract = modelContract.replaceAll("\\s+", " ")

    assert(interactionLaw.contains("### LDH Closeout Hard Cleanup Pass"))
    Vector(
      "LDH Closeout opens no chess meaning. It only audits the Line / Defender Interaction Hardening surface.",
      "LDH Closeout audit checklist:",
      "- no new Story family opened.",
      "- no new proof home opened.",
      "- LineFact, LineProof, PinProof, and RemoveGuardProof authority stay separated.",
      "- Tactic.DiscoveredAttack, Tactic.Pin, and Tactic.RemoveGuard do not steal each other's meaning.",
      "- Tactic.DiscoveredAttack, Tactic.Pin, and Tactic.RemoveGuard do not invade Scene.Material, Tactic.Hanging, or Scene.Defense claim homes.",
      "- broad Line, Ray, XRay, Skewer, deflection, overload, pressure, and initiative do not become live authority.",
      "- detailed LDH interaction rules live in StoryInteractionLaw.md only.",
      "- other live docs summarize LDH scope without duplicating rule text.",
      "- public route 200 remains closed.",
      "- production API remains closed.",
      "- public/user-facing LLM narration remains closed.",
      "LDH Closeout ownership map:",
      "- BoardFacts LineFact observes geometry only; it is not a Story or proof home.",
      "- LineProof belongs only to Tactic.DiscoveredAttack for this hardening surface.",
      "- PinProof belongs only to Tactic.Pin.",
      "- RemoveGuardProof belongs only to Tactic.RemoveGuard.",
      "- Scene.Material keeps actual material balance change now.",
      "- Tactic.Hanging keeps capturable target with bounded material gain proof.",
      "- Scene.Defense keeps complete ThreatProof plus DefenseProof preventing immediate material loss.",
      "Completion standard: LDH closes as interaction hardening only, with no new Story family, no new proof home, no mixed LineFact, LineProof, PinProof, or RemoveGuardProof authority, no Line/Defender meaning theft, no Material, Hanging, or Defense claim-home invasion, no broad-term authority, no duplicated live rule authority outside StoryInteractionLaw.md, no public route 200, no production API, and no public/user-facing LLM narration."
    ).foreach: closeoutLine =>
      assert(interactionLaw.contains(closeoutLine), s"LDH closeout must pin: $closeoutLine")

    Vector(readme, ssot, architecture).foreach: doc =>
      assert(
        doc.contains(
          "LDH Closeout opens no chess meaning. It only audits the Line / Defender Interaction Hardening surface."
        )
      )
      assert(
        doc.contains(
          "Completion standard: LDH closes as interaction hardening only, with no new Story family, no new proof home, no mixed LineFact, LineProof, PinProof, or RemoveGuardProof authority, no Line/Defender meaning theft, no Material, Hanging, or Defense claim-home invasion, no broad-term authority, no duplicated live rule authority outside StoryInteractionLaw.md, no public route 200, no production API, and no public/user-facing LLM narration."
        )
      )
      assert(
        !doc.contains("LDH Closeout audit checklist:"),
        "summary docs must not duplicate detailed LDH Closeout checklist"
      )
      assert(
        !doc.contains("LDH Closeout ownership map:"),
        "summary docs must not duplicate detailed LDH Closeout ownership map"
      )

    assert(
      normalizedModelContract.contains(
        "LDH Closeout opens no chess meaning. It only audits the Line / Defender Interaction Hardening surface."
      )
    )
    assert(
      normalizedModelContract.contains(
        "LDH closes as interaction hardening only, with no new Story family, no new proof home, no mixed LineFact, LineProof, PinProof, or RemoveGuardProof authority, no Line/Defender meaning theft, no Material, Hanging, or Defense claim-home invasion, no broad-term authority, no duplicated live rule authority outside StoryInteractionLaw.md, no public route 200, no production API, and no public/user-facing LLM narration."
      )
    )
    assert(
      !modelContract.contains("LDH Closeout audit checklist:"),
      "model contract must summarize detailed LDH Closeout checklist"
    )
    assert(
      !modelContract.contains("LDH Closeout ownership map:"),
      "model contract must summarize detailed LDH Closeout ownership map"
    )

  test("Skewer-0 charter opens only one front and one rear non-king target on a slider line"):
    val readme = Files.readString(docsRoot.resolve("README.md"))
    val ssot = Files.readString(docsRoot.resolve("ChessCommentarySSOT.md"))
    val architecture = Files.readString(docsRoot.resolve("ChessModelArchitecture.md"))
    val modelContract = Files.readString(docsRoot.resolve("ChessModelContract.md"))
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))
    val normalizedModelContract = modelContract.replaceAll("\\s+", " ")

    assert(interactionLaw.contains("## Skewer Slice"))
    assert(interactionLaw.contains("### Skewer-0 Charter"))
    Vector(
      "Current implementation scope is Line / Defender Contact Neighborhood late vertical slice.",
      "Skewer-0 opens only the charter for the fourth narrow line/defender contact vertical slice.",
      "First Skewer positive scope is not a broad skewer tactic.",
      "Skewer first scope: a legal move creates or reveals a slider attack on one front non-king material target, with a second non-king material target behind it on the same line.",
      "LineFact observes geometry.",
      "SkewerProof proves the front-and-back target relation.",
      "Tactic.Skewer may speak only after proof.",
      "Skewer-0 allowed opening:",
      "- narrow `Tactic.Skewer`",
      "- one slider",
      "- one front target",
      "- one rear target",
      "- front/rear target same-line relation",
      "- legal move that creates or reveals the front/rear relation",
      "- bounded skewer wording after selected Verdict only",
      "Skewer-0 forbidden openings:",
      "- broad LineTactic",
      "- XRay public Story",
      "- Pin expansion",
      "- RemoveGuard expansion",
      "- material win claim",
      "- front piece must move",
      "- wins rear piece",
      "- forced line",
      "- best move",
      "- only move",
      "- winning",
      "- decisive",
      "- king safety",
      "- mate threat",
      "- pressure",
      "- initiative",
      "- public route `200`",
      "- production API",
      "- public/user-facing LLM narration",
      "Skewer-0 opens only narrow Tactic.Skewer, one slider, one front target, one rear target, front/rear target same-line relation, a legal move that creates or reveals the front/rear relation, and bounded skewer wording after selected Verdict only.",
      "Skewer-0 opens no broad LineTactic, XRay public Story, Pin expansion, RemoveGuard expansion, material win claim, front piece must move, wins rear piece, forced line, best move, only move, winning, decisive, king safety, mate threat, pressure, initiative, public route `200`, production API, or public/user-facing LLM narration.",
      "Completion standard: Skewer-0 keeps skewer work at charter scope and opens no SkewerProof runtime, no Tactic.Skewer writer, no StoryTable integration, no ExplanationPlan mapping, no renderer wording, no LLM smoke, no public route `200`, and no production API."
    ).foreach: skewerScope =>
      assert(interactionLaw.contains(skewerScope), s"Skewer-0 scope must pin: $skewerScope")

    Vector(readme, ssot, architecture).foreach: doc =>
      assert(doc.contains("Skewer Slice is the current scope."))
      assert(
        doc.contains(
          "Current implementation scope is Line / Defender Contact Neighborhood late vertical slice."
        )
      )
      assert(
        doc.contains(
          "Skewer-0 opens only the charter for the fourth narrow line/defender contact vertical slice."
        )
      )
      assert(doc.contains("First Skewer positive scope is not a broad skewer tactic."))
      assert(
        doc.contains(
          "Skewer first scope: a legal move creates or reveals a slider attack on one front non-king material target, with a second non-king material target behind it on the same line."
        )
      )
      assert(
        doc.contains(
          "LineFact observes geometry. SkewerProof proves the front-and-back target relation. Tactic.Skewer may speak only after proof."
        )
      )
      assert(
        doc.contains(
          "Skewer-0 opens only narrow Tactic.Skewer, one slider, one front target, one rear target, front/rear target same-line relation, a legal move that creates or reveals the front/rear relation, and bounded skewer wording after selected Verdict only."
        )
      )
      assert(
        doc.contains(
          "Skewer-0 opens no broad LineTactic, XRay public Story, Pin expansion, RemoveGuard expansion, material win claim, front piece must move, wins rear piece, forced line, best move, only move, winning, decisive, king safety, mate threat, pressure, initiative, public route `200`, production API, or public/user-facing LLM narration."
        )
      )
      assert(
        doc.contains(
          "Public route `200`, production API, and public/user-facing LLM narration remain closed."
        )
      )
    assert(normalizedModelContract.contains("Skewer-0 is owned by `StoryInteractionLaw.md`."))
    assert(
      normalizedModelContract.contains(
        "Skewer-0 opens only the charter for the fourth narrow line/defender contact vertical slice."
      )
    )
    assert(normalizedModelContract.contains("The first Skewer positive scope is not a broad skewer tactic."))
    assert(
      normalizedModelContract.contains(
        "The Skewer first scope is a legal move that creates or reveals a slider attack on one front non-king material target, with a second non-king material target behind it on the same line."
      )
    )
    assert(
      normalizedModelContract.contains(
        "LineFact observes geometry, SkewerProof proves the front-and-back target relation, and Tactic.Skewer may speak only after proof."
      )
    )
    assert(
      normalizedModelContract.contains(
        "Skewer-0 opens only narrow Tactic.Skewer, one slider, one front target, one rear target, front/rear target same-line relation, a legal move that creates or reveals the front/rear relation, and bounded skewer wording after selected Verdict only."
      )
    )
    assert(
      normalizedModelContract.contains(
        "Skewer-0 opens no broad LineTactic, XRay public Story, Pin expansion, RemoveGuard expansion, material win claim, front piece must move, wins rear piece, forced line, best move, only move, winning, decisive, king safety, mate threat, pressure, initiative, public route `200`, production API, or public/user-facing LLM narration."
      )
    )

  test("Skewer-1 SkewerProof opens only the front and rear target proof home"):
    val readme = Files.readString(docsRoot.resolve("README.md"))
    val ssot = Files.readString(docsRoot.resolve("ChessCommentarySSOT.md"))
    val architecture = Files.readString(docsRoot.resolve("ChessModelArchitecture.md"))
    val modelContract = Files.readString(docsRoot.resolve("ChessModelContract.md"))
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))
    val legacyManifest = Files.readString(docsRoot.resolve("LegacyPruneManifest.md"))
    val normalizedModelContract = modelContract.replaceAll("\\s+", " ")

    assert(interactionLaw.contains("### Skewer-1 SkewerProof"))
    Vector(
      "Skewer-1 opens only `SkewerProof` as a narrow proof home.",
      "SkewerProof proves side creating the skewer, rival side, skewer slider, front target, rear target, legal skewer or revealing move, line kind, same-board proof, front target non-king material piece, rear target non-king material piece, front and rear target same rival side, after move slider attacks front target, rear target behind front target on the same ray, no extra blocker breaks the front-to-rear relation, and before move the skewer relation was absent or blocked.",
      "SkewerProof is not a public Story.",
      "LineFact and LineProof are not public Stories.",
      "SkewerProof says no material gain, front piece must move, or wins rear piece.",
      "SkewerProof proofFailures stay out of renderer/LLM input.",
      "Skewer-1 opens no Tactic.Skewer writer, StoryTable integration, ExplanationPlan mapping, renderer wording, LLM smoke, material gain claim, front piece must move wording, wins rear piece claim, public route `200`, production API, or public/user-facing LLM narration."
    ).foreach: skewerProofScope =>
      assert(interactionLaw.contains(skewerProofScope), s"Skewer-1 scope must pin: $skewerProofScope")

    Vector(readme, ssot, architecture).foreach: doc =>
      assert(doc.contains("Skewer-1 opens only `SkewerProof` as a narrow proof home."))
      assert(
        doc.contains(
          "SkewerProof proves side creating the skewer, rival side, skewer slider, front target, rear target, legal skewer or revealing move, line kind, same-board proof, front target non-king material piece, rear target non-king material piece, front and rear target same rival side, after move slider attacks front target, rear target behind front target on the same ray, no extra blocker breaks the front-to-rear relation, and before move the skewer relation was absent or blocked."
        )
      )
      assert(
        doc.contains("SkewerProof is not a public Story. LineFact and LineProof are not public Stories.")
      )
      assert(
        doc.contains(
          "SkewerProof says no material gain, front piece must move, or wins rear piece; proofFailures stay out of renderer/LLM input."
        )
      )
      assert(
        doc.contains(
          "Skewer-1 opens no Tactic.Skewer writer, StoryTable integration, ExplanationPlan mapping, renderer wording, LLM smoke, material gain claim, front piece must move wording, wins rear piece claim, public route `200`, production API, or public/user-facing LLM narration."
        )
      )
      assert(
        doc.contains(
          "Public route `200`, production API, and public/user-facing LLM narration remain closed."
        )
      )

    assert(normalizedModelContract.contains("Skewer-1 is owned by `StoryInteractionLaw.md`."))
    assert(normalizedModelContract.contains("Skewer-1 opens only `SkewerProof` as a narrow proof home."))
    assert(
      normalizedModelContract.contains(
        "SkewerProof proves side creating the skewer, rival side, skewer slider, front target, rear target, legal skewer or revealing move, line kind, same-board proof, front target non-king material piece, rear target non-king material piece, front and rear target same rival side, after move slider attacks front target, rear target behind front target on the same ray, no extra blocker breaks the front-to-rear relation, and before move the skewer relation was absent or blocked."
      )
    )
    assert(
      normalizedModelContract.contains(
        "SkewerProof is not a public Story, and LineFact and LineProof are not public Stories."
      )
    )
    assert(
      normalizedModelContract.contains(
        "SkewerProof says no material gain, front piece must move, or wins rear piece; proofFailures stay out of renderer/LLM input."
      )
    )
    assert(
      normalizedModelContract.contains(
        "Skewer-1 opens no Tactic.Skewer writer, StoryTable integration, ExplanationPlan mapping, renderer wording, LLM smoke, material gain claim, front piece must move wording, wins rear piece claim, public route `200`, production API, or public/user-facing LLM narration."
      )
    )
    assert(legacyManifest.contains("Skewer-1 SkewerProof authority lives in `StoryInteractionLaw.md`."))
    assert(
      legacyManifest.contains(
        "Legacy broad skewer, XRay, material win, front-piece-must-move, wins-rear-piece, production API, public route, and public/user-facing LLM paths do not return."
      )
    )

  test("Skewer-2 TacticSkewer writer opens only the proof-backed Skewer Story"):
    val readme = Files.readString(docsRoot.resolve("README.md"))
    val ssot = Files.readString(docsRoot.resolve("ChessCommentarySSOT.md"))
    val architecture = Files.readString(docsRoot.resolve("ChessModelArchitecture.md"))
    val modelContract = Files.readString(docsRoot.resolve("ChessModelContract.md"))
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))
    val legacyManifest = Files.readString(docsRoot.resolve("LegacyPruneManifest.md"))
    val normalizedModelContract = modelContract.replaceAll("\\s+", " ")

    assert(interactionLaw.contains("### Skewer-2 TacticSkewer Writer"))
    Vector(
      "Skewer-2 opens only the named `TacticSkewer` writer for one narrow `Tactic.Skewer` Story.",
      "TacticSkewer requires complete StoryProof, complete SkewerProof, same-board legal replay, legal skewer or revealing move, front target, rear target, slider, complete front-and-back line relation, writer identity, and no EngineCheck Refutes status.",
      "Skewer Story identity is tactic Skewer, scene Tactic, skewer-creating side, front/rear target side as rival, front target square, rear target square as secondaryTarget, slider or moved-piece anchor, and skewer/revealing route.",
      "TacticSkewer creates no Scene.Material claim.",
      "TacticSkewer does not replace Tactic.Pin.",
      "Skewer-2 keeps rear-target king positions silent.",
      "Skewer-2 opens no StoryTable Lead admission, ExplanationPlan mapping, renderer wording, LLM smoke, material gain claim, front piece must move wording, wins rear piece claim, Pin replacement, public route `200`, production API, or public/user-facing LLM narration."
    ).foreach: skewerWriterScope =>
      assert(interactionLaw.contains(skewerWriterScope), s"Skewer-2 scope must pin: $skewerWriterScope")

    Vector(readme, ssot, architecture).foreach: doc =>
      assert(
        doc.contains(
          "Skewer-2 opens only the named `TacticSkewer` writer for one narrow `Tactic.Skewer` Story."
        )
      )
      assert(
        doc.contains(
          "TacticSkewer requires complete StoryProof, complete SkewerProof, same-board legal replay, legal skewer or revealing move, front target, rear target, slider, complete front-and-back line relation, writer identity, and no EngineCheck Refutes status."
        )
      )
      assert(
        doc.contains(
          "Skewer Story identity is tactic Skewer, scene Tactic, skewer-creating side, front/rear target side as rival, front target square, rear target square as secondaryTarget, slider or moved-piece anchor, and skewer/revealing route."
        )
      )
      assert(
        doc.contains(
          "TacticSkewer creates no Scene.Material claim, does not replace Tactic.Pin, and keeps rear-target king positions silent."
        )
      )
      assert(
        doc.contains(
          "Skewer-2 opens no StoryTable Lead admission, ExplanationPlan mapping, renderer wording, LLM smoke, material gain claim, front piece must move wording, wins rear piece claim, Pin replacement, public route `200`, production API, or public/user-facing LLM narration."
        )
      )
      assert(
        doc.contains(
          "Public route `200`, production API, and public/user-facing LLM narration remain closed."
        )
      )

    assert(normalizedModelContract.contains("Skewer-2 is owned by `StoryInteractionLaw.md`."))
    assert(
      normalizedModelContract.contains(
        "Skewer-2 opens only the named `TacticSkewer` writer for one narrow `Tactic.Skewer` Story."
      )
    )
    assert(
      normalizedModelContract.contains(
        "TacticSkewer requires complete StoryProof, complete SkewerProof, same-board legal replay, legal skewer or revealing move, front target, rear target, slider, complete front-and-back line relation, writer identity, and no EngineCheck Refutes status."
      )
    )
    assert(
      normalizedModelContract.contains(
        "Skewer Story identity is tactic Skewer, scene Tactic, skewer-creating side, front/rear target side as rival, front target square, rear target square as secondaryTarget, slider or moved-piece anchor, and skewer/revealing route."
      )
    )
    assert(
      normalizedModelContract.contains(
        "TacticSkewer creates no Scene.Material claim, does not replace Tactic.Pin, and keeps rear-target king positions silent."
      )
    )
    assert(
      normalizedModelContract.contains(
        "Skewer-2 opens no StoryTable Lead admission, ExplanationPlan mapping, renderer wording, LLM smoke, material gain claim, front piece must move wording, wins rear piece claim, Pin replacement, public route `200`, production API, or public/user-facing LLM narration."
      )
    )
    assert(legacyManifest.contains("Skewer-2 TacticSkewer authority lives in `StoryInteractionLaw.md`."))
    assert(
      legacyManifest.contains(
        "Legacy broad skewer, Scene.Material-by-skewer, Pin replacement, rear-king skewer, production API, public route, and public/user-facing LLM paths do not return."
      )
    )

  test("Skewer-3 negative corpus keeps skewer-looking false positives silent"):
    val readme = Files.readString(docsRoot.resolve("README.md"))
    val ssot = Files.readString(docsRoot.resolve("ChessCommentarySSOT.md"))
    val architecture = Files.readString(docsRoot.resolve("ChessModelArchitecture.md"))
    val modelContract = Files.readString(docsRoot.resolve("ChessModelContract.md"))
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))
    val legacyManifest = Files.readString(docsRoot.resolve("LegacyPruneManifest.md"))
    val normalizedModelContract = modelContract.replaceAll("\\s+", " ")

    assert(interactionLaw.contains("### Skewer-3 Negative Corpus"))
    Vector(
      "Skewer-3 opens only the negative corpus for the narrow `Tactic.Skewer` slice.",
      "Skewer-3 keeps illegal moves, missing same-board proof, non-sliders, missing front target, missing rear target, front or rear king targets, front/rear targets not on the same rival side, rear targets not behind the front target on the same line, extra blockers between front and rear target, DiscoveredAttack-only lines, Pin-looking positions, front-piece-must-move assumptions, and material-win, forced, or best-move wording silent.",
      "Skewer-3 rule: Two pieces on a line is not enough. Complete front-and-back skewer proof or silence.",
      "Skewer-3 opens no new proof home, new writer, StoryTable Lead admission, ExplanationPlan mapping, renderer wording, LLM smoke, Scene.Material claim, Pin replacement, front piece must move wording, wins rear piece claim, public route `200`, production API, or public/user-facing LLM narration."
    ).foreach: skewerNegativeScope =>
      assert(interactionLaw.contains(skewerNegativeScope), s"Skewer-3 scope must pin: $skewerNegativeScope")

    Vector(readme, ssot, architecture).foreach: doc =>
      assert(doc.contains("Skewer-3 opens only the negative corpus for the narrow `Tactic.Skewer` slice."))
      assert(
        doc.contains(
          "Skewer-3 keeps illegal moves, missing same-board proof, non-sliders, missing front target, missing rear target, front or rear king targets, front/rear targets not on the same rival side, rear targets not behind the front target on the same line, extra blockers between front and rear target, DiscoveredAttack-only lines, Pin-looking positions, front-piece-must-move assumptions, and material-win, forced, or best-move wording silent."
        )
      )
      assert(
        doc.contains(
          "Skewer-3 rule: Two pieces on a line is not enough. Complete front-and-back skewer proof or silence."
        )
      )
      assert(
        doc.contains(
          "Skewer-3 opens no new proof home, new writer, StoryTable Lead admission, ExplanationPlan mapping, renderer wording, LLM smoke, Scene.Material claim, Pin replacement, front piece must move wording, wins rear piece claim, public route `200`, production API, or public/user-facing LLM narration."
        )
      )
      assert(
        doc.contains(
          "Public route `200`, production API, and public/user-facing LLM narration remain closed."
        )
      )

    assert(normalizedModelContract.contains("Skewer-3 is owned by `StoryInteractionLaw.md`."))
    assert(
      normalizedModelContract.contains(
        "Skewer-3 opens only the negative corpus for the narrow `Tactic.Skewer` slice."
      )
    )
    assert(
      normalizedModelContract.contains(
        "Skewer-3 keeps illegal moves, missing same-board proof, non-sliders, missing front target, missing rear target, front or rear king targets, front/rear targets not on the same rival side, rear targets not behind the front target on the same line, extra blockers between front and rear target, DiscoveredAttack-only lines, Pin-looking positions, front-piece-must-move assumptions, and material-win, forced, or best-move wording silent."
      )
    )
    assert(
      normalizedModelContract.contains(
        "Two pieces on a line is not enough. Complete front-and-back skewer proof or silence."
      )
    )
    assert(
      normalizedModelContract.contains(
        "Skewer-3 opens no new proof home, new writer, StoryTable Lead admission, ExplanationPlan mapping, renderer wording, LLM smoke, Scene.Material claim, Pin replacement, front piece must move wording, wins rear piece claim, public route `200`, production API, or public/user-facing LLM narration."
      )
    )
    assert(legacyManifest.contains("Skewer-3 negative corpus authority lives in `StoryInteractionLaw.md`."))
    assert(
      legacyManifest.contains(
        "Legacy broad skewer, front-piece-must-move, material win, forced, best-move, Pin replacement, Scene.Material-by-skewer, production API, public route, and public/user-facing LLM paths do not return."
      )
    )

  test("Skewer-4 EngineCheck reuse keeps engine evidence internal for Skewer"):
    val readme = Files.readString(docsRoot.resolve("README.md"))
    val ssot = Files.readString(docsRoot.resolve("ChessCommentarySSOT.md"))
    val architecture = Files.readString(docsRoot.resolve("ChessModelArchitecture.md"))
    val modelContract = Files.readString(docsRoot.resolve("ChessModelContract.md"))
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))
    val legacyManifest = Files.readString(docsRoot.resolve("LegacyPruneManifest.md"))
    val normalizedModelContract = modelContract.replaceAll("\\s+", " ")

    assert(interactionLaw.contains("### Skewer-4 EngineCheck Reuse"))
    Vector(
      "Skewer-4 opens only existing `EngineCheck` reuse for existing `Tactic.Skewer` Stories.",
      "Skewer-4 EngineCheck rules:",
      "- EngineCheck cannot create Skewer",
      "- Supports creates no new claim",
      "- Caps suppresses standalone claim or weakens expression to bounded strength when downstream speech opens",
      "- Refutes blocks the Skewer Story",
      "- Unknown creates no engine expression",
      "Skewer-4 forbidden openings:",
      "- engine says",
      "- best move",
      "- only move",
      "- forced win",
      "- winning tactic",
      "- raw PV explanation",
      "- eval number public value",
      "- new EngineCheck type",
      "- Skewer from engine evidence",
      "- StoryTable Lead admission",
      "- ExplanationPlan mapping",
      "- renderer wording",
      "- LLM smoke",
      "- public route `200`",
      "- production API",
      "- public/user-facing LLM narration",
      "Completion standard: Existing EngineCheck may only support, cap, or refute an already proof-backed `Tactic.Skewer` Story; it never creates Skewer, never ranks by raw eval or raw PV, and never adds engine wording or stronger tactic wording."
    ).foreach: skewerScope =>
      assert(interactionLaw.contains(skewerScope), s"Skewer-4 scope must pin: $skewerScope")

    Vector(readme, ssot, architecture).foreach: doc =>
      assert(
        doc.contains("Skewer-4 opens only existing `EngineCheck` reuse for existing `Tactic.Skewer` Stories.")
      )
      assert(
        doc.contains(
          "EngineCheck cannot create Skewer; Supports creates no new claim; Caps suppresses standalone claim or keeps downstream speech bounded when opened later; Refutes blocks the Skewer Story; Unknown creates no engine expression."
        )
      )
      assert(
        doc.contains(
          "Skewer-4 opens no engine-says wording, best-move wording, only-move wording, forced-win wording, winning tactic, raw PV explanation, eval number public value, StoryTable Lead admission, ExplanationPlan mapping, renderer wording, LLM smoke, public route `200`, production API, or public/user-facing LLM narration."
        )
      )
      assert(
        doc.contains(
          "Public route `200`, production API, and public/user-facing LLM narration remain closed."
        )
      )
    assert(normalizedModelContract.contains("Skewer-4 is owned by `StoryInteractionLaw.md`."))
    assert(
      normalizedModelContract.contains(
        "Skewer-4 opens only existing `EngineCheck` reuse for existing `Tactic.Skewer` Stories."
      )
    )
    assert(
      normalizedModelContract.contains(
        "EngineCheck cannot create Skewer; Supports creates no new claim; Caps suppresses standalone claim or keeps downstream speech bounded when opened later; Refutes blocks the Skewer Story; Unknown creates no engine expression."
      )
    )
    assert(
      normalizedModelContract.contains(
        "Skewer-4 opens no engine-says wording, best-move wording, only-move wording, forced-win wording, winning tactic, raw PV explanation, eval number public value, StoryTable Lead admission, ExplanationPlan mapping, renderer wording, LLM smoke, public route `200`, production API, or public/user-facing LLM narration."
      )
    )
    assert(legacyManifest.contains("Skewer-4 EngineCheck reuse authority lives in `StoryInteractionLaw.md`."))
    assert(
      legacyManifest.contains(
        "Legacy engine-says, raw PV explanation, eval-number public value, best-move, only-move, forced-win, winning-tactic, StoryTable Lead admission, ExplanationPlan, renderer, LLM, production API, public route, and public/user-facing LLM paths do not return."
      )
    )

  test("Skewer-5 StoryTable integration keeps Skewer claim ownership bounded"):
    val readme = Files.readString(docsRoot.resolve("README.md"))
    val ssot = Files.readString(docsRoot.resolve("ChessCommentarySSOT.md"))
    val architecture = Files.readString(docsRoot.resolve("ChessModelArchitecture.md"))
    val modelContract = Files.readString(docsRoot.resolve("ChessModelContract.md"))
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))
    val legacyManifest = Files.readString(docsRoot.resolve("LegacyPruneManifest.md"))
    val normalizedModelContract = modelContract.replaceAll("\\s+", " ")

    assert(interactionLaw.contains("### Skewer-5 StoryTable Integration"))
    Vector(
      "Skewer-5 opens only StoryTable integration for existing `Tactic.Hanging`, `Tactic.Fork`, `Scene.Material`, `Scene.Defense`, `Tactic.DiscoveredAttack`, `Tactic.Pin`, `Tactic.RemoveGuard`, and `Tactic.Skewer` rows.",
      "Skewer-5 StoryTable checks:",
      "- selected Verdict remains stable when input order changes",
      "- Skewer does not own Material claim",
      "- Skewer does not turn DiscoveredAttack into a duplicate Lead",
      "- Skewer does not own Pin king relation",
      "- Skewer does not own RemoveGuard defender relation",
      "- actual material change now remains owned by Scene.Material",
      "- incomplete front/rear relation leaves DiscoveredAttack or another existing row and keeps Skewer silent",
      "Skewer-5 forbidden openings:",
      "- new Skewer proof home",
      "- new Story family",
      "- broad LineTactic",
      "- broad XRay",
      "- Material claim from Skewer",
      "- DiscoveredAttack duplicate Lead from Skewer",
      "- Pin king relation from Skewer",
      "- RemoveGuard defender relation from Skewer",
      "- ExplanationPlan mapping",
      "- renderer wording",
      "- LLM smoke",
      "- public route `200`",
      "- production API",
      "- public/user-facing LLM narration",
      "Completion standard: StoryTable orders existing open rows with Skewer deterministically, keeps one selected Lead, and keeps Material, DiscoveredAttack, Pin, RemoveGuard, Defense, Fork, Hanging, and Skewer claim homes separate."
    ).foreach: skewerScope =>
      assert(interactionLaw.contains(skewerScope), s"Skewer-5 scope must pin: $skewerScope")

    Vector(readme, ssot, architecture).foreach: doc =>
      assert(
        doc.contains(
          "Skewer-5 opens only StoryTable integration for existing `Tactic.Hanging`, `Tactic.Fork`, `Scene.Material`, `Scene.Defense`, `Tactic.DiscoveredAttack`, `Tactic.Pin`, `Tactic.RemoveGuard`, and `Tactic.Skewer` rows."
        )
      )
      assert(
        doc.contains(
          "Skewer-5 keeps selected Verdict stable across input order, prevents duplicate Lead for DiscoveredAttack and Skewer, keeps Skewer from owning Material, Pin king-relation, or RemoveGuard defender-relation claims, keeps actual material change now in Scene.Material, and keeps incomplete front/rear relations silent so only complete existing rows remain."
        )
      )
      assert(
        doc.contains(
          "Skewer-5 opens no new proof home, new Story family, broad LineTactic, broad XRay, Material claim from Skewer, DiscoveredAttack duplicate Lead from Skewer, Pin king relation from Skewer, RemoveGuard defender relation from Skewer, ExplanationPlan mapping, renderer wording, LLM smoke, public route `200`, production API, or public/user-facing LLM narration."
        )
      )
      assert(
        doc.contains(
          "Public route `200`, production API, and public/user-facing LLM narration remain closed."
        )
      )

    assert(normalizedModelContract.contains("Skewer-5 is owned by `StoryInteractionLaw.md`."))
    assert(
      normalizedModelContract.contains(
        "Skewer-5 opens only StoryTable integration for existing `Tactic.Hanging`, `Tactic.Fork`, `Scene.Material`, `Scene.Defense`, `Tactic.DiscoveredAttack`, `Tactic.Pin`, `Tactic.RemoveGuard`, and `Tactic.Skewer` rows."
      )
    )
    assert(
      normalizedModelContract.contains(
        "Skewer-5 keeps selected Verdict stable across input order, prevents duplicate Lead for DiscoveredAttack and Skewer, keeps Skewer from owning Material, Pin king-relation, or RemoveGuard defender-relation claims, keeps actual material change now in Scene.Material, and keeps incomplete front/rear relations silent so only complete existing rows remain."
      )
    )
    assert(
      normalizedModelContract.contains(
        "Skewer-5 opens no new proof home, new Story family, broad LineTactic, broad XRay, Material claim from Skewer, DiscoveredAttack duplicate Lead from Skewer, Pin king relation from Skewer, RemoveGuard defender relation from Skewer, ExplanationPlan mapping, renderer wording, LLM smoke, public route `200`, production API, or public/user-facing LLM narration."
      )
    )
    assert(
      legacyManifest.contains("Skewer-5 StoryTable integration authority lives in `StoryInteractionLaw.md`.")
    )
    assert(
      legacyManifest.contains(
        "Legacy broad LineTactic, broad XRay, material-by-skewer, pin-by-skewer, remove-guard-by-skewer, duplicate discovered-attack, ExplanationPlan, renderer, LLM, production API, public route, and public/user-facing LLM paths do not return."
      )
    )

  test("Skewer-6 ExplanationPlan opens only bounded Skewer claim mapping"):
    val readme = Files.readString(docsRoot.resolve("README.md"))
    val ssot = Files.readString(docsRoot.resolve("ChessCommentarySSOT.md"))
    val architecture = Files.readString(docsRoot.resolve("ChessModelArchitecture.md"))
    val modelContract = Files.readString(docsRoot.resolve("ChessModelContract.md"))
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))
    val legacyManifest = Files.readString(docsRoot.resolve("LegacyPruneManifest.md"))
    val normalizedModelContract = modelContract.replaceAll("\\s+", " ")

    assert(interactionLaw.contains("### Skewer-6 ExplanationPlan"))
    Vector(
      "Skewer-6 opens only ExplanationPlan mapping for selected uncapped Lead `Tactic.Skewer` Verdicts.",
      "Skewer-6 ExplanationPlan input is selected uncapped Lead Verdict only.",
      "Skewer-6 allowed claim key:",
      "- `skewers_piece_to_piece`",
      "Skewer-6 forbidden claim keys:",
      "- `wins_material`",
      "- `wins_rear_piece`",
      "- `front_piece_must_move`",
      "- `best_move`",
      "- `only_move`",
      "- `forced`",
      "- `decisive`",
      "- `king_unsafe`",
      "- `mate_threat`",
      "- `creates_pressure`",
      "- `takes_initiative`",
      "Support, Context, Blocked, capped, and refuted Skewer rows create no standalone claim.",
      "Skewer-6 forbidden openings:",
      "- renderer wording",
      "- LLM smoke",
      "- public route `200`",
      "- production API",
      "- public/user-facing LLM narration",
      "Completion standard: Support, Context, Blocked, capped, and refuted Skewer rows create no standalone claim; selected uncapped Lead Skewer rows may lower only the bounded `skewers_piece_to_piece` claim key."
    ).foreach: skewerScope =>
      assert(interactionLaw.contains(skewerScope), s"Skewer-6 scope must pin: $skewerScope")

    Vector(readme, ssot, architecture).foreach: doc =>
      assert(
        doc.contains(
          "Skewer-6 opens only ExplanationPlan mapping for selected uncapped Lead `Tactic.Skewer` Verdicts."
        )
      )
      assert(
        doc.contains(
          "Skewer-6 allows only the `skewers_piece_to_piece` claim key and forbids wins_material, wins_rear_piece, front_piece_must_move, best_move, only_move, forced, decisive, king_unsafe, mate_threat, creates_pressure, and takes_initiative."
        )
      )
      assert(
        doc.contains("Support, Context, Blocked, capped, and refuted Skewer rows create no standalone claim.")
      )
      assert(
        doc.contains(
          "Skewer-6 opens no renderer wording, LLM smoke, public route `200`, production API, or public/user-facing LLM narration."
        )
      )
      assert(
        doc.contains(
          "Public route `200`, production API, and public/user-facing LLM narration remain closed."
        )
      )

    assert(normalizedModelContract.contains("Skewer-6 is owned by `StoryInteractionLaw.md`."))
    assert(
      normalizedModelContract.contains(
        "Skewer-6 opens only ExplanationPlan mapping for selected uncapped Lead `Tactic.Skewer` Verdicts."
      )
    )
    assert(
      normalizedModelContract.contains(
        "Skewer-6 allows only the `skewers_piece_to_piece` claim key and forbids wins_material, wins_rear_piece, front_piece_must_move, best_move, only_move, forced, decisive, king_unsafe, mate_threat, creates_pressure, and takes_initiative."
      )
    )
    assert(
      normalizedModelContract.contains(
        "Support, Context, Blocked, capped, and refuted Skewer rows create no standalone claim."
      )
    )
    assert(
      normalizedModelContract.contains(
        "Skewer-6 opens no renderer wording, LLM smoke, public route `200`, production API, or public/user-facing LLM narration."
      )
    )
    assert(legacyManifest.contains("Skewer-6 ExplanationPlan authority lives in `StoryInteractionLaw.md`."))
    assert(
      legacyManifest.contains(
        "Legacy Skewer material-win, wins-rear-piece, front-piece-must-move, best-move, only-move, forced, decisive, king-safety, mate-threat, pressure, initiative, renderer, LLM, production API, public route, and public/user-facing LLM paths do not return."
      )
    )

  test("Skewer-7 deterministic renderer opens only bounded Skewer template"):
    val readme = Files.readString(docsRoot.resolve("README.md"))
    val ssot = Files.readString(docsRoot.resolve("ChessCommentarySSOT.md"))
    val architecture = Files.readString(docsRoot.resolve("ChessModelArchitecture.md"))
    val modelContract = Files.readString(docsRoot.resolve("ChessModelContract.md"))
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))
    val legacyManifest = Files.readString(docsRoot.resolve("LegacyPruneManifest.md"))
    val normalizedModelContract = modelContract.replaceAll("\\s+", " ")

    assert(interactionLaw.contains("### Skewer-7 Deterministic Renderer"))
    Vector(
      "Skewer-7 opens only deterministic renderer text for selected `Tactic.Skewer` ExplanationPlan.",
      "Skewer-7 renderer input is ExplanationPlan only.",
      "Skewer-7 may render `{route} skewers the piece on {target} to the piece on {secondaryTarget}.`",
      "Skewer-7 forbidden wording:",
      "- wins material",
      "- wins the piece behind it",
      "- the front piece must move",
      "- best move",
      "- only move",
      "- forces",
      "- decisive",
      "- king is unsafe",
      "- threatens mate",
      "- creates pressure",
      "Skewer-7 opens no raw Verdict, raw Story, SkewerProof, LineFact, LineProof, BoardFacts, EngineCheck, proofFailures, LLM smoke, public route `200`, production API, or public/user-facing LLM narration.",
      "Completion standard: DeterministicRenderer may speak only from selected Skewer ExplanationPlan and only with bounded `skewers_piece_to_piece` wording."
    ).foreach: skewerScope =>
      assert(interactionLaw.contains(skewerScope), s"Skewer-7 scope must pin: $skewerScope")

    Vector(readme, ssot, architecture).foreach: doc =>
      assert(
        doc.contains(
          "Skewer-7 opens only deterministic renderer text for selected `Tactic.Skewer` ExplanationPlan."
        )
      )
      assert(
        doc.contains(
          "Skewer-7 renderer input is ExplanationPlan only and may render `{route} skewers the piece on {target} to the piece on {secondaryTarget}.`"
        )
      )
      assert(
        doc.contains(
          "Skewer-7 forbids wins material, wins the piece behind it, the front piece must move, best move, only move, forces, decisive, king is unsafe, threatens mate, and creates pressure."
        )
      )
      assert(
        doc.contains(
          "Skewer-7 opens no raw Verdict, raw Story, SkewerProof, LineFact, LineProof, BoardFacts, EngineCheck, proofFailures, LLM smoke, public route `200`, production API, or public/user-facing LLM narration."
        )
      )
      assert(
        doc.contains(
          "Public route `200`, production API, and public/user-facing LLM narration remain closed."
        )
      )

    assert(normalizedModelContract.contains("Skewer-7 is owned by `StoryInteractionLaw.md`."))
    assert(
      normalizedModelContract.contains(
        "Skewer-7 opens only deterministic renderer text for selected `Tactic.Skewer` ExplanationPlan."
      )
    )
    assert(
      normalizedModelContract.contains(
        "Skewer-7 renderer input is ExplanationPlan only and may render `{route} skewers the piece on {target} to the piece on {secondaryTarget}.`"
      )
    )
    assert(
      normalizedModelContract.contains(
        "Skewer-7 forbids wins material, wins the piece behind it, the front piece must move, best move, only move, forces, decisive, king is unsafe, threatens mate, and creates pressure."
      )
    )
    assert(
      normalizedModelContract.contains(
        "Skewer-7 opens no raw Verdict, raw Story, SkewerProof, LineFact, LineProof, BoardFacts, EngineCheck, proofFailures, LLM smoke, public route `200`, production API, or public/user-facing LLM narration."
      )
    )
    assert(
      legacyManifest.contains("Skewer-7 deterministic renderer authority lives in `StoryInteractionLaw.md`.")
    )
    assert(
      legacyManifest.contains(
        "Legacy Skewer material-win, wins-piece-behind, front-piece-must-move, best-move, only-move, forced, decisive, king-safety, mate-threat, pressure, LLM, production API, public route, and public/user-facing LLM paths do not return."
      )
    )

  test("Skewer-8 LLM smoke reuses only the existing 8B prompt boundary"):
    val readme = Files.readString(docsRoot.resolve("README.md"))
    val ssot = Files.readString(docsRoot.resolve("ChessCommentarySSOT.md"))
    val architecture = Files.readString(docsRoot.resolve("ChessModelArchitecture.md"))
    val modelContract = Files.readString(docsRoot.resolve("ChessModelContract.md"))
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))
    val legacyManifest = Files.readString(docsRoot.resolve("LegacyPruneManifest.md"))
    val normalizedModelContract = modelContract.replaceAll("\\s+", " ")

    assert(interactionLaw.contains("### Skewer-8 LLM Smoke"))
    Vector(
      "Skewer-8 opens only LLM smoke for selected Skewer ExplanationPlan and RenderedLine.",
      "Skewer-8 reuses only the existing 8B Codex CLI prompt smoke contract with renderedText, claimKey, strength, forbidden wording, and `Rephrase only. Do not add chess facts.`",
      "Skewer-8 LLM input:",
      "- renderedText",
      "- claimKey",
      "- strength",
      "- forbidden wording",
      "- `Rephrase only. Do not add chess facts.`",
      "Skewer-8 forbidden openings:",
      "- raw Story",
      "- raw SkewerProof",
      "- raw LineProof",
      "- BoardFacts",
      "- EngineCheck",
      "- raw PV",
      "- proofFailures",
      "- new move",
      "- new line",
      "- material win",
      "- forced claim",
      "- pressure claim",
      "- initiative claim",
      "- mate claim",
      "- public/user-facing LLM narration",
      "- public route `200`",
      "- production API",
      "- raw proof repair",
      "- engine explanation",
      "Completion standard: Skewer LLM smoke accepts only rephrases no stronger than renderedText and rejects raw proof, engine, new-move, new-line, material-win, forced, pressure, initiative, and mate additions."
    ).foreach: skewerScope =>
      assert(interactionLaw.contains(skewerScope), s"Skewer-8 scope must pin: $skewerScope")

    Vector(readme, ssot, architecture).foreach: doc =>
      assert(
        doc.contains("Skewer-8 opens only LLM smoke for selected Skewer ExplanationPlan and RenderedLine.")
      )
      assert(
        doc.contains(
          "Skewer-8 reuses only the existing 8B Codex CLI prompt smoke contract with renderedText, claimKey, strength, forbidden wording, and `Rephrase only. Do not add chess facts.`"
        )
      )
      assert(
        doc.contains(
          "Skewer-8 forbids raw Story, raw SkewerProof, raw LineProof, BoardFacts, EngineCheck, raw PV, proofFailures, new move, new line, material-win, forced, pressure, initiative, and mate claims."
        )
      )
      assert(
        doc.contains(
          "Skewer-8 opens no public/user-facing LLM narration, public route `200`, production API, raw proof repair, or engine explanation."
        )
      )
      assert(
        doc.contains(
          "Public route `200`, production API, and public/user-facing LLM narration remain closed."
        )
      )

    assert(normalizedModelContract.contains("Skewer-8 is owned by `StoryInteractionLaw.md`."))
    assert(
      normalizedModelContract.contains(
        "Skewer-8 opens only LLM smoke for selected Skewer ExplanationPlan and RenderedLine."
      )
    )
    assert(
      normalizedModelContract.contains(
        "Skewer-8 reuses only the existing 8B Codex CLI prompt smoke contract with renderedText, claimKey, strength, forbidden wording, and `Rephrase only. Do not add chess facts.`"
      )
    )
    assert(
      normalizedModelContract.contains(
        "Skewer-8 forbids raw Story, raw SkewerProof, raw LineProof, BoardFacts, EngineCheck, raw PV, proofFailures, new move, new line, material-win, forced, pressure, initiative, and mate claims."
      )
    )
    assert(
      normalizedModelContract.contains(
        "Skewer-8 opens no public/user-facing LLM narration, public route `200`, production API, raw proof repair, or engine explanation."
      )
    )
    assert(legacyManifest.contains("Skewer-8 LLM smoke authority lives in `StoryInteractionLaw.md`."))
    assert(
      legacyManifest.contains(
        "Legacy Skewer raw Story, raw SkewerProof, raw LineProof, BoardFacts, EngineCheck, raw PV, proofFailures, new-move, new-line, material-win, forced, pressure, initiative, mate, public/user-facing LLM, production API, public route, raw-proof-repair, and engine-explanation paths do not return."
      )
    )

  test("Skewer Closeout hard cleanup keeps Skewer authority separated and public surfaces closed"):
    val readme = Files.readString(docsRoot.resolve("README.md"))
    val ssot = Files.readString(docsRoot.resolve("ChessCommentarySSOT.md"))
    val architecture = Files.readString(docsRoot.resolve("ChessModelArchitecture.md"))
    val modelContract = Files.readString(docsRoot.resolve("ChessModelContract.md"))
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))
    val legacyManifest = Files.readString(docsRoot.resolve("LegacyPruneManifest.md"))
    val normalizedModelContract = modelContract.replaceAll("\\s+", " ")

    assert(interactionLaw.contains("### Skewer Closeout Hard Cleanup"))
    Vector(
      "Skewer Closeout opens no new chess meaning. It only audits the Skewer hard cleanup surface.",
      "Skewer Closeout must confirm:",
      "- LineFact, LineProof, SkewerProof, Tactic.Skewer, and the speech key do not invade each other's authority.",
      "- Skewer owns no Material, Hanging, Pin, DiscoveredAttack, RemoveGuard, or Defense meaning.",
      "- `front piece must move`, `wins rear piece`, `wins material`, and `forced skewer` are not live authority.",
      "- detailed docs authority lives only in StoryInteractionLaw.md; other live docs summarize it.",
      "- renderer/LLM wording stays no stronger than `skewers_piece_to_piece`.",
      "- test helpers do not become runtime authority.",
      "- public route `200`, production API, and public/user-facing LLM narration remain closed.",
      "Skewer Closeout duplicate checks:",
      "- meaning duplication: no same chess meaning appears under two Story labels or two proof homes.",
      "- authority duplication: BoardFacts, proof home, Story writer, StoryTable, ExplanationPlan, and renderer do not jointly own the same decision.",
      "- terminology duplication: LineTactic, RayTactic, XRay, SkewerFamily, or equivalent broad names do not become live authority.",
      "- document duplication: detailed rules repeat only here; other live docs summarize.",
      "Skewer Closeout opens no broad LineTactic, broad Skewer family, XRay public Story, Material claim, Hanging claim, Pin expansion, DiscoveredAttack expansion, RemoveGuard expansion, Defense claim, front-piece-must-move wording, wins-rear-piece claim, wins-material claim, forced-skewer wording, pressure, initiative, mate threat, king safety, best-move, only-move, forced-line, winning, decisive, new proof home, new Story family, renderer wording beyond Skewer-7, LLM input beyond Skewer-8, public route `200`, production API, or public/user-facing LLM narration.",
      "Completion standard: Skewer closes as a narrow proof-backed front-and-rear target slice, with no sibling meaning ownership, no broad-term authority, no duplicated detailed authority outside StoryInteractionLaw.md, no promoted test helper, and no public route, production API, or public/user-facing LLM narration."
    ).foreach: skewerScope =>
      assert(interactionLaw.contains(skewerScope), s"Skewer Closeout scope must pin: $skewerScope")

    Vector(readme, ssot, architecture).foreach: doc =>
      assert(
        doc.contains(
          "Skewer Closeout opens no new chess meaning; it audits only LineFact, LineProof, SkewerProof, Tactic.Skewer, speech key, renderer/LLM, docs authority, test-helper boundary, and closed public surfaces."
        )
      )
      assert(
        doc.contains(
          "Skewer Closeout confirms Skewer owns no Material, Hanging, Pin, DiscoveredAttack, RemoveGuard, or Defense meaning; front-piece-must-move, wins-rear-piece, wins-material, and forced-skewer terms are not live authority; renderer/LLM wording stays no stronger than `skewers_piece_to_piece`; public route `200`, production API, and public/user-facing LLM narration remain closed."
        )
      )
      assert(
        doc.contains(
          "Skewer Closeout also audits meaning, authority, terminology, and document duplication without duplicating detailed rules outside StoryInteractionLaw.md."
        )
      )
      assert(
        !doc.contains("Skewer Closeout must confirm:"),
        "summary docs must not duplicate detailed Skewer Closeout checklist"
      )
      assert(
        !doc.contains("Skewer Closeout duplicate checks:"),
        "summary docs must not duplicate detailed Skewer duplicate checklist"
      )

    assert(normalizedModelContract.contains("Skewer Closeout opens no new chess meaning."))
    assert(
      normalizedModelContract.contains(
        "It audits only LineFact, LineProof, SkewerProof, Tactic.Skewer, speech key, renderer/LLM, docs authority, test-helper boundary, and closed public surfaces."
      )
    )
    assert(
      normalizedModelContract.contains(
        "It confirms Skewer owns no Material, Hanging, Pin, DiscoveredAttack, RemoveGuard, or Defense meaning; front-piece-must-move, wins-rear-piece, wins-material, and forced-skewer terms are not live authority; renderer/LLM wording stays no stronger than `skewers_piece_to_piece`; public route `200`, production API, and public/user-facing LLM narration remain closed."
      )
    )
    assert(
      normalizedModelContract.contains(
        "It also audits meaning, authority, terminology, and document duplication without duplicating detailed rules outside StoryInteractionLaw.md."
      )
    )
    assert(
      !normalizedModelContract.contains("Skewer Closeout must confirm:"),
      "model contract must summarize, not duplicate detailed Skewer Closeout checklist"
    )
    assert(
      !normalizedModelContract.contains("Skewer Closeout duplicate checks:"),
      "model contract must summarize, not duplicate detailed Skewer duplicate checklist"
    )
    assert(
      legacyManifest.contains("Skewer Closeout hard cleanup authority lives in `StoryInteractionLaw.md`.")
    )
    assert(
      legacyManifest.contains(
        "Legacy Skewer Material, Hanging, Pin, DiscoveredAttack, RemoveGuard, Defense, front-piece-must-move, wins-rear-piece, wins-material, forced-skewer, broad LineTactic, broad Skewer, XRay, renderer-beyond-Skewer-7, LLM-beyond-Skewer-8, test-helper-authority, production API, public route, and public/user-facing LLM paths do not return."
      )
    )

  test("Line Defender Neighborhood Closeout closes four narrow slices without broad meaning"):
    val readme = Files.readString(docsRoot.resolve("README.md"))
    val ssot = Files.readString(docsRoot.resolve("ChessCommentarySSOT.md"))
    val architecture = Files.readString(docsRoot.resolve("ChessModelArchitecture.md"))
    val modelContract = Files.readString(docsRoot.resolve("ChessModelContract.md"))
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))
    val legacyManifest = Files.readString(docsRoot.resolve("LegacyPruneManifest.md"))
    val normalizedModelContract = modelContract.replaceAll("\\s+", " ")

    assert(interactionLaw.contains("## Line / Defender Neighborhood Closeout"))
    assert(interactionLaw.contains("### LNC-0 Closeout Charter"))
    Vector(
      "Line/Defender closes as four narrow proof-backed slices. It opens no broad LineTactic, XRay, pressure, initiative, material-win tactic, or public surface.",
      "LNC-0 opens only Line / Defender neighborhood closeout.",
      "LNC-0 closing targets:",
      "- `Tactic.DiscoveredAttack`",
      "- `Tactic.Pin`",
      "- `Tactic.RemoveGuard`",
      "- `Tactic.Skewer`",
      "LNC-0 related proof homes:",
      "- `LineProof`",
      "- `PinProof`",
      "- `RemoveGuardProof`",
      "- `SkewerProof`",
      "LNC-0 existing collision targets:",
      "- `Tactic.Hanging`",
      "- `Tactic.Fork`",
      "- `Scene.Material`",
      "- `Scene.Defense`",
      "LNC-0 allowed audit work:",
      "- scope audit",
      "- duplication audit",
      "- authority audit",
      "- docs simplification",
      "- downstream no-overclaim audit",
      "- next-neighborhood handoff",
      "LNC-0 must confirm:",
      "- DiscoveredAttack owns only one revealed slider attack on one non-king material target.",
      "- Pin owns only one non-king piece pinned to its own king on a line.",
      "- RemoveGuard owns only one removed defender guard relation from one non-king material target.",
      "- Skewer owns only one front non-king material target and one rear non-king material target on the same line.",
      "- LineFact observes geometry and owns no public Story.",
      "- LineProof, PinProof, RemoveGuardProof, and SkewerProof are proof homes, not public Stories.",
      "- Hanging, Fork, Material, and Defense keep their existing claim homes.",
      "- ExplanationPlan, renderer, and LLM smoke stay downstream of selected Verdict data.",
      "- detailed docs authority lives only in StoryInteractionLaw.md; other live docs summarize it.",
      "- public route `200`, production API, and public/user-facing LLM narration remain closed.",
      "LNC-0 duplicate checks:",
      "- meaning duplication: no Line / Defender chess meaning appears under two Story labels or two proof homes.",
      "- proof duplication: no proof home proves a sibling Story's distinct public claim.",
      "- authority duplication: BoardFacts, proof home, Story writer, StoryTable, ExplanationPlan, renderer, and LLM smoke do not jointly own the same decision.",
      "- terminology duplication: LineTactic, Ray, XRay, broad deflection, overload, pressure, initiative, and material-win tactic names do not become live authority.",
      "- document duplication: detailed closeout rules repeat only here; other live docs summarize.",
      "LNC-0 opens no new Story family, new proof home, new Story writer, new renderer template, new LLM behavior, XRay, broad LineTactic, broad Ray, broad deflection, overload, pressure, initiative, material win by line tactic, forced response, public route `200`, production API, or public/user-facing LLM narration.",
      "Completion standard: Line / Defender Contact Neighborhood closes as four narrow proof-backed slices, with no sibling meaning ownership, no broad-term authority, no collision-home invasion, no duplicated detailed authority outside StoryInteractionLaw.md, no promoted test helper, no new downstream wording or LLM behavior, no public route `200`, no production API, and no public/user-facing LLM narration."
    ).foreach: lineDefenderScope =>
      assert(interactionLaw.contains(lineDefenderScope), s"LNC-0 scope must pin: $lineDefenderScope")

    assertLineDefenderCloseoutSummaryDocs(readme, ssot, architecture, normalizedModelContract, legacyManifest)

  test("LNC-1 scope audit keeps Line Defender positive Stories to four narrow first scopes"):
    val readme = Files.readString(docsRoot.resolve("README.md"))
    val ssot = Files.readString(docsRoot.resolve("ChessCommentarySSOT.md"))
    val architecture = Files.readString(docsRoot.resolve("ChessModelArchitecture.md"))
    val modelContract = Files.readString(docsRoot.resolve("ChessModelContract.md"))
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))
    val legacyManifest = Files.readString(docsRoot.resolve("LegacyPruneManifest.md"))
    val storySource =
      Files.readString(Paths.get("modules/commentary/src/main/scala/lila/commentary/chess/Story.scala"))
    val normalizedModelContract = modelContract.replaceAll("\\s+", " ")

    assert(interactionLaw.contains("### LNC-1 Scope Audit"))
    Vector(
      "LNC-1 opens only Line / Defender Contact Neighborhood scope audit.",
      "LNC-1 confirms the neighborhood has exactly four opened positive Story labels:",
      "- `Tactic.DiscoveredAttack`",
      "- `Tactic.Pin`",
      "- `Tactic.RemoveGuard`",
      "- `Tactic.Skewer`",
      "LNC-1 first-scope audit:",
      "- `Tactic.DiscoveredAttack` speaks only one revealed slider attack on one non-king material target.",
      "- `Tactic.Pin` speaks only one non-king piece pinned to its own king on a line.",
      "- `Tactic.RemoveGuard` speaks only one removed defender guard relation from one non-king material target.",
      "- `Tactic.Skewer` speaks only one front non-king material target and one rear non-king material target on the same line.",
      "LNC-1 closed runtime positives:",
      "- XRay is not an opened positive Story in this neighborhood.",
      "- broad Ray is not an opened positive Story in this neighborhood.",
      "- broad LineTactic is not an opened positive Story in this neighborhood.",
      "- broad deflection is not an opened positive Story in this neighborhood.",
      "- overload is not an opened positive Story in this neighborhood.",
      "Closed names are not backlog inside this neighborhood. They remain closed until a separate charter opens them.",
      "LNC-1 opens no new Story family, proof home, Story writer, renderer wording, LLM behavior, XRay, broad Ray, broad LineTactic, broad deflection, overload, pressure, initiative, material-win tactic, public route `200`, production API, or public/user-facing LLM narration."
    ).foreach: lncScope =>
      assert(interactionLaw.contains(lncScope), s"LNC-1 scope must pin: $lncScope")

    assertLineDefenderCloseoutSummaryDocs(readme, ssot, architecture, normalizedModelContract, legacyManifest)

    Vector(
      "StoryWriter.TacticDiscoveredAttack",
      "StoryWriter.TacticPin",
      "StoryWriter.TacticRemoveGuard",
      "StoryWriter.TacticSkewer"
    ).foreach: writer =>
      assert(storySource.contains(writer), s"Line Defender runtime writer must remain present: $writer")
    Vector(
      "StoryWriter.TacticXray",
      "StoryWriter.TacticXRay",
      "StoryWriter.TacticLineTactic",
      "StoryWriter.TacticDeflect",
      "StoryWriter.TacticOverload"
    ).foreach: writer =>
      assert(!storySource.contains(writer), s"closed Line Defender writer must not open: $writer")

  test("LNC-2 duplication audit keeps one meaning one proof home one Story label one speech key"):
    val readme = Files.readString(docsRoot.resolve("README.md"))
    val ssot = Files.readString(docsRoot.resolve("ChessCommentarySSOT.md"))
    val architecture = Files.readString(docsRoot.resolve("ChessModelArchitecture.md"))
    val modelContract = Files.readString(docsRoot.resolve("ChessModelContract.md"))
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))
    val legacyManifest = Files.readString(docsRoot.resolve("LegacyPruneManifest.md"))
    val explanationPlanSource = Files.readString(
      Paths.get("modules/commentary/src/main/scala/lila/commentary/chess/ExplanationPlan.scala")
    )
    val storySource =
      Files.readString(Paths.get("modules/commentary/src/main/scala/lila/commentary/chess/Story.scala"))
    val normalizedModelContract = modelContract.replaceAll("\\s+", " ")

    assert(interactionLaw.contains("### LNC-2 Duplication Audit"))
    Vector(
      "One chess meaning, one proof home, one Story label, one speech key, one live authority document.",
      "LNC-2 opens only Line / Defender Contact Neighborhood duplication audit.",
      "LNC-2 duplication audit checks:",
      "- no same chess meaning is duplicated under two Story labels.",
      "- no same proof responsibility is duplicated under two proof homes.",
      "- no same speech claim is split across two claim keys.",
      "- no same detailed rule repeats across multiple live documents.",
      "LNC-2 specific ownership checks:",
      "- DiscoveredAttack and Skewer do not duplicate ownership of `line attack`; DiscoveredAttack owns revealed slider attack on one non-king material target, while Skewer owns front-and-rear non-king material targets on one line.",
      "- Pin and Skewer do not duplicate ownership of `front/rear line relation`; Pin owns pinned-to-own-king relation, while Skewer owns front target plus rear target relation.",
      "- RemoveGuard does not grow into Material or Hanging precondition ownership; RemoveGuard owns only removed defender guard relation, Material owns actual material balance change, and Hanging owns capturable target with bounded material gain proof.",
      "- LineProof does not absorb PinProof, RemoveGuardProof, or SkewerProof family-specific relations; LineProof binds only the revealed slider attack line admitted by DiscoveredAttack.",
      "- XRay, Ray, LineTactic, and LineFamily terms are not live authority names for this neighborhood.",
      "LNC-2 speech-key audit:",
      "- `reveals_attack_on_piece` belongs only to `Tactic.DiscoveredAttack`.",
      "- `pins_piece` belongs only to `Tactic.Pin`.",
      "- `removes_defender` belongs only to `Tactic.RemoveGuard`.",
      "- `skewers_piece_to_piece` belongs only to `Tactic.Skewer`.",
      "LNC-2 opens no new Story family, proof home, Story writer, claim key, renderer wording, LLM behavior, XRay, Ray, LineTactic, LineFamily, broad deflection, overload, Material or Hanging precondition path, public route `200`, production API, or public/user-facing LLM narration."
    ).foreach: lncScope =>
      assert(interactionLaw.contains(lncScope), s"LNC-2 scope must pin: $lncScope")

    assertLineDefenderCloseoutSummaryDocs(readme, ssot, architecture, normalizedModelContract, legacyManifest)

    Vector(
      "ExplanationClaim.RevealsAttackOnPiece",
      "ExplanationClaim.PinsPiece",
      "ExplanationClaim.RemovesDefender",
      "ExplanationClaim.SkewersPieceToPiece"
    ).foreach: claim =>
      assert(explanationPlanSource.contains(claim), s"Line Defender speech key must remain present: $claim")
    Vector(
      "StoryWriter.TacticDiscoveredAttack",
      "StoryWriter.TacticPin",
      "StoryWriter.TacticRemoveGuard",
      "StoryWriter.TacticSkewer"
    ).foreach: writer =>
      assert(storySource.contains(writer), s"Line Defender Story writer must remain present: $writer")

  test("LNC-3 authority audit fixes each Line Defender layer to one job"):
    val readme = Files.readString(docsRoot.resolve("README.md"))
    val ssot = Files.readString(docsRoot.resolve("ChessCommentarySSOT.md"))
    val architecture = Files.readString(docsRoot.resolve("ChessModelArchitecture.md"))
    val modelContract = Files.readString(docsRoot.resolve("ChessModelContract.md"))
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))
    val legacyManifest = Files.readString(docsRoot.resolve("LegacyPruneManifest.md"))
    val normalizedModelContract = modelContract.replaceAll("\\s+", " ")

    assert(interactionLaw.contains("### LNC-3 Authority Audit"))
    Vector(
      "LNC-3 opens only Line / Defender Contact Neighborhood authority audit.",
      "LNC-3 layer authority:",
      "- `BoardFacts.LineFact`: geometry observation only.",
      "- `LineProof`: revealed line / attack binding only.",
      "- `PinProof`: pinned relation only.",
      "- `RemoveGuardProof`: guard relation removal only.",
      "- `SkewerProof`: front/rear target relation only.",
      "- Story writers: named proof-backed Story permission only.",
      "- StoryTable: ordering only.",
      "- Verdict: selected result only.",
      "- ExplanationPlan: bounded speech claim only.",
      "- Renderer: phrasing only.",
      "- LLM smoke: rephrase only.",
      "LNC-3 forbidden authority shortcuts:",
      "- proof home must not speak like a Story label.",
      "- Story writer must not own material result.",
      "- StoryTable must not create new chess meaning.",
      "- ExplanationPlan must not read raw proof.",
      "- Renderer and LLM smoke must not repair or upgrade proof.",
      "LNC-3 opens no new Story family, proof home, Story writer, claim key, renderer wording, LLM behavior, raw proof downstream path, material-result ownership by Story writer, StoryTable-created meaning, public route `200`, production API, or public/user-facing LLM narration."
    ).foreach: lncScope =>
      assert(interactionLaw.contains(lncScope), s"LNC-3 scope must pin: $lncScope")

    assertLineDefenderCloseoutSummaryDocs(readme, ssot, architecture, normalizedModelContract, legacyManifest)

  test("LNC-4 collision audit locks Line Defender rows against existing homes"):
    val readme = Files.readString(docsRoot.resolve("README.md"))
    val ssot = Files.readString(docsRoot.resolve("ChessCommentarySSOT.md"))
    val architecture = Files.readString(docsRoot.resolve("ChessModelArchitecture.md"))
    val modelContract = Files.readString(docsRoot.resolve("ChessModelContract.md"))
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))
    val legacyManifest = Files.readString(docsRoot.resolve("LegacyPruneManifest.md"))
    val normalizedModelContract = modelContract.replaceAll("\\s+", " ")

    assert(interactionLaw.contains("### LNC-4 Collision Audit"))
    Vector(
      "LNC-4 opens only Line / Defender Contact Neighborhood collision audit.",
      "LNC-4 collision targets:",
      "- DiscoveredAttack vs Pin.",
      "- DiscoveredAttack vs Skewer.",
      "- Pin vs Skewer.",
      "- RemoveGuard vs Material.",
      "- RemoveGuard vs Hanging.",
      "- Skewer vs Material.",
      "- Line/Defender row vs Defense.",
      "- EngineCheck Caps/Refutes over each line/defender row.",
      "LNC-4 verification criteria:",
      "- input order stable.",
      "- no duplicate Lead.",
      "- incomplete row is not Lead.",
      "- capped and refuted rows create no standalone text.",
      "- actual material change now stays in Scene.Material home.",
      "- material gain proof stays in Hanging or Material home.",
      "- line/defender rows speak only their own relation.",
      "LNC-4 existing runtime coverage:",
      "- `Pin-5 StoryTable prevents duplicate Lead for same-line DiscoveredAttack and Pin` covers DiscoveredAttack vs Pin.",
      "- `Skewer-5 separates DiscoveredAttack collision and keeps incomplete Skewer silent` covers DiscoveredAttack vs Skewer.",
      "- `Skewer-5 keeps Material Pin and RemoveGuard claim homes separate` covers Pin vs Skewer and Skewer vs Material.",
      "- `LDH-1 fixture map covers complex same-board line defender interactions` covers RemoveGuard vs Material, RemoveGuard vs Hanging, and Line/Defender row vs Defense.",
      "- `LDH-1 fixture map covers EngineCheck statuses over existing line defender rows` covers EngineCheck Caps/Refutes over DiscoveredAttack, Pin, RemoveGuard, and Skewer.",
      "LNC-4 opens no new Story family, proof home, Story writer, claim key, renderer wording, LLM behavior, broad LineTactic, XRay, material-win tactic, public route `200`, production API, or public/user-facing LLM narration."
    ).foreach: lncScope =>
      assert(interactionLaw.contains(lncScope), s"LNC-4 scope must pin: $lncScope")

    assertLineDefenderCloseoutSummaryDocs(readme, ssot, architecture, normalizedModelContract, legacyManifest)

  test("LNC-5 downstream boundary audit keeps Line Defender expression bounded"):
    val readme = Files.readString(docsRoot.resolve("README.md"))
    val ssot = Files.readString(docsRoot.resolve("ChessCommentarySSOT.md"))
    val architecture = Files.readString(docsRoot.resolve("ChessModelArchitecture.md"))
    val modelContract = Files.readString(docsRoot.resolve("ChessModelContract.md"))
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))
    val legacyManifest = Files.readString(docsRoot.resolve("LegacyPruneManifest.md"))
    val normalizedModelContract = modelContract.replaceAll("\\s+", " ")

    assert(interactionLaw.contains("### LNC-5 Downstream Boundary Audit"))
    Vector(
      "LNC-5 opens only Line / Defender Contact Neighborhood downstream boundary audit.",
      "LNC-5 downstream authority:",
      "- selected uncapped Lead Verdict only may lower to ExplanationPlan.",
      "- ExplanationPlan input is selected Verdict only.",
      "- Renderer input is ExplanationPlan only.",
      "- LLM smoke input is renderedText, claimKey, strength, forbidden wording, and rephrase-only instruction only.",
      "- Support, Context, Blocked, capped, and refuted rows create no standalone text.",
      "LNC-5 forbidden downstream wording:",
      "- wins material.",
      "- winning / decisive.",
      "- best move / only move.",
      "- forced.",
      "- cannot move.",
      "- no defense.",
      "- front piece must move.",
      "- wins rear piece.",
      "- pressure / initiative.",
      "- mate threat / king unsafe.",
      "LNC-5 existing runtime coverage:",
      "- `LNC-5 Downstream Boundary Audit keeps Line Defender speech bounded` covers DiscoveredAttack, Pin, RemoveGuard, and Skewer downstream handoff and forbidden wording.",
      "- `LDH-6 Downstream Boundary Smoke sends only selected Lead Verdicts to text stages` covers existing Line/Defender selected Lead handoff.",
      "- `Skewer-6 ExplanationPlan gives no standalone claim to non Lead capped refuted or unselected Skewer rows` covers Skewer non-Lead silence.",
      "LNC-5 opens no new Story family, proof home, Story writer, claim key, renderer wording, LLM behavior, broad LineTactic, XRay, public route `200`, production API, or public/user-facing LLM narration."
    ).foreach: lncScope =>
      assert(interactionLaw.contains(lncScope), s"LNC-5 scope must pin: $lncScope")

    assertLineDefenderCloseoutSummaryDocs(readme, ssot, architecture, normalizedModelContract, legacyManifest)

  test("LNC-6 documentation simplification keeps Line Defender closeout detail in one authority"):
    val readme = Files.readString(docsRoot.resolve("README.md"))
    val ssot = Files.readString(docsRoot.resolve("ChessCommentarySSOT.md"))
    val architecture = Files.readString(docsRoot.resolve("ChessModelArchitecture.md"))
    val modelContract = Files.readString(docsRoot.resolve("ChessModelContract.md"))
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))
    val legacyManifest = Files.readString(docsRoot.resolve("LegacyPruneManifest.md"))
    val normalizedModelContract = modelContract.replaceAll("\\s+", " ")

    assert(interactionLaw.contains("### LNC-6 Documentation Simplification"))
    Vector(
      "LNC-6 opens only Line / Defender Contact Neighborhood documentation simplification.",
      "LNC-6 documentation authority:",
      "- detailed closeout authority lives only in `StoryInteractionLaw.md`.",
      "- README, SSOT, Architecture, Contract, AGENTS, and LegacyPruneManifest carry summaries only.",
      "- the same closeout rule must not repeat across live documents.",
      "- closed terms must read as closed summaries, not backlog.",
      "- Line family, Ray family, XRay, and line tactic wording must not read as live authority.",
      "LNC-6 verification:",
      "- docs authority tests enforce summary-only downstream documents.",
      "- detailed closeout checklist appears in exactly one live document.",
      "- live authority lists stay aligned across AGENTS.md, README, LegacyPruneManifest, and docs tests.",
      "LNC-6 opens no new Story family, proof home, Story writer, claim key, renderer wording, LLM behavior, broad LineTactic, XRay, public route `200`, production API, or public/user-facing LLM narration."
    ).foreach: lncScope =>
      assert(interactionLaw.contains(lncScope), s"LNC-6 scope must pin: $lncScope")

    assertLineDefenderCloseoutSummaryDocs(readme, ssot, architecture, normalizedModelContract, legacyManifest)

    val liveDocs =
      Map(
        "AGENTS.md" -> agents,
        "README.md" -> readme,
        "ChessCommentarySSOT.md" -> ssot,
        "ChessModelArchitecture.md" -> architecture,
        "ChessModelContract.md" -> normalizedModelContract,
        "LegacyPruneManifest.md" -> legacyManifest,
        "StoryInteractionLaw.md" -> interactionLaw
      )
    Vector(
      "LNC-0 must confirm:",
      "LNC-0 duplicate checks:",
      "LNC-1 first-scope audit:",
      "LNC-1 closed runtime positives:",
      "LNC-2 duplication audit checks:",
      "LNC-2 specific ownership checks:",
      "LNC-2 speech-key audit:",
      "LNC-3 layer authority:",
      "LNC-3 forbidden authority shortcuts:",
      "LNC-4 collision targets:",
      "LNC-4 verification criteria:",
      "LNC-4 existing runtime coverage:",
      "LNC-5 downstream authority:",
      "LNC-5 forbidden downstream wording:",
      "LNC-5 existing runtime coverage:",
      "LNC-6 documentation authority:",
      "LNC-6 verification:",
      "LNC-7 runtime boundary:",
      "LNC-7 verification:",
      "LNC Closeout final completion standard:",
      "LNC final verification:"
    ).foreach: marker =>
      val owners = liveDocs.collect { case (name, text) if text.contains(marker) => name }
      assertEquals(
        owners.toSet,
        Set("StoryInteractionLaw.md"),
        s"Line / Defender detailed closeout marker must live only in StoryInteractionLaw.md: $marker"
      )

  test("LNC-7 test helper runtime boundary audit keeps helpers below authority"):
    val readme = Files.readString(docsRoot.resolve("README.md"))
    val ssot = Files.readString(docsRoot.resolve("ChessCommentarySSOT.md"))
    val architecture = Files.readString(docsRoot.resolve("ChessModelArchitecture.md"))
    val modelContract = Files.readString(docsRoot.resolve("ChessModelContract.md"))
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))
    val legacyManifest = Files.readString(docsRoot.resolve("LegacyPruneManifest.md"))
    val normalizedModelContract = modelContract.replaceAll("\\s+", " ")

    assert(interactionLaw.contains("### LNC-7 Test Helper / Runtime Boundary Audit"))
    Vector(
      "LNC-7 opens only Line / Defender Contact Neighborhood test helper / runtime boundary audit.",
      "LNC-7 runtime boundary:",
      "- test helpers must not become runtime authority.",
      "- fixture names must not read like new Story families.",
      "- negative corpus helpers must not become production concepts.",
      "- forbidden wording checks must reject public wording without treating snake_case internal field names as public prose.",
      "- runtime source must not contain closeout-only terms.",
      "LNC-7 verification:",
      "- docs authority tests pin this section in StoryInteractionLaw only.",
      "- runtime boundary tests scan production source for closeout-only terminology.",
      "- LLM smoke tests keep forbidden wording matching on public phrases, not internal field keys.",
      "LNC-7 opens no new Story family, proof home, Story writer, claim key, renderer wording, LLM behavior, fixture-derived runtime authority, negative-corpus production concept, broad LineTactic, XRay, public route `200`, production API, or public/user-facing LLM narration."
    ).foreach: lncScope =>
      assert(interactionLaw.contains(lncScope), s"LNC-7 scope must pin: $lncScope")

    assertLineDefenderCloseoutSummaryDocs(readme, ssot, architecture, normalizedModelContract, legacyManifest)
    assert(
      !normalizedModelContract.contains("LNC-7 runtime boundary:"),
      "model contract must summarize, not duplicate detailed LNC-7 checklist"
    )
    Vector(readme, ssot, architecture, legacyManifest).foreach: doc =>
      assert(
        !doc.contains("LNC-7 runtime boundary:"),
        "summary docs must not duplicate detailed LNC-7 checklist"
      )

  test("LNC final completion standard closes Line Defender neighborhood"):
    val readme = Files.readString(docsRoot.resolve("README.md"))
    val ssot = Files.readString(docsRoot.resolve("ChessCommentarySSOT.md"))
    val architecture = Files.readString(docsRoot.resolve("ChessModelArchitecture.md"))
    val modelContract = Files.readString(docsRoot.resolve("ChessModelContract.md"))
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))
    val legacyManifest = Files.readString(docsRoot.resolve("LegacyPruneManifest.md"))
    val normalizedModelContract = modelContract.replaceAll("\\s+", " ")

    assert(interactionLaw.contains("### LNC Closeout Final Completion Standard"))
    Vector(
      "LNC Closeout final completion standard:",
      "- no new Story family.",
      "- no new proof home.",
      "- no new renderer wording.",
      "- no new LLM behavior.",
      "- only `Tactic.DiscoveredAttack`, `Tactic.Pin`, `Tactic.RemoveGuard`, and `Tactic.Skewer` remain as the positive closeout baseline slices.",
      "- no duplicate meaning.",
      "- no duplicate authority.",
      "- no duplicate terminology.",
      "- no duplicate detailed docs.",
      "- public route, production API, and public/user-facing LLM narration remain closed.",
      "LNC final verification:",
      "- docs authority tests pass.",
      "- chess foundation tests pass.",
      "- `git diff --check` is clean.",
      "Final LNC closeout opens no new Story family, proof home, Story writer, claim key, renderer wording, LLM behavior, fixture-derived runtime authority, negative-corpus production concept, broad LineTactic, XRay, public route `200`, production API, or public/user-facing LLM narration."
    ).foreach: finalLine =>
      assert(interactionLaw.contains(finalLine), s"LNC final completion must pin: $finalLine")

    assertLineDefenderCloseoutSummaryDocs(readme, ssot, architecture, normalizedModelContract, legacyManifest)
    Vector(readme, ssot, architecture, normalizedModelContract, legacyManifest).foreach: doc =>
      assert(
        !doc.contains("LNC Closeout final completion standard:"),
        "summary docs must not duplicate detailed LNC final standard"
      )
      assert(
        !doc.contains("LNC final verification:"),
        "summary docs must not duplicate detailed LNC final verification"
      )

  test("PawnAdvance-0 charter opens only bounded passed pawn progress"):
    val readme = Files.readString(docsRoot.resolve("README.md"))
    val ssot = Files.readString(docsRoot.resolve("ChessCommentarySSOT.md"))
    val architecture = Files.readString(docsRoot.resolve("ChessModelArchitecture.md"))
    val contract = Files.readString(docsRoot.resolve("ChessModelContract.md"))
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))
    val manifest = Files.readString(docsRoot.resolve("LegacyPruneManifest.md"))

    Vector(
      "## Pawn / Promotion Neighborhood",
      "### PawnAdvance-0 Charter",
      "PawnAdvance-0 opens only the first narrow Pawn / Promotion Neighborhood vertical slice.",
      "Pawn facts observe structure. PawnAdvanceProof binds this legal advance. Scene.PawnAdvance may speak only bounded pawn progress, not conversion.",
      "`Scene.PawnAdvance`",
      "`PawnAdvanceProof`",
      "`advances_passed_pawn`",
      "`{route} advances the passed pawn.`",
      "PawnAdvance-0 opens no broad PawnTactic, `Tactic.PawnPush`,",
      "Completion standard: PawnAdvance-0 closes as one narrow proof-backed passed-pawn progress slice"
    ).foreach: required =>
      assert(interactionLaw.contains(required), s"PawnAdvance-0 law must pin: $required")

    Vector(readme, ssot, architecture, contract).foreach: doc =>
      val normalizedDoc = doc.replaceAll("\\s+", " ")
      assert(normalizedDoc.contains("PawnAdvance-0 is owned by `StoryInteractionLaw.md`."))
      assert(normalizedDoc.contains("The only bounded claim key is `advances_passed_pawn`"))
      assert(
        !doc.contains("Allowed deterministic wording:"),
        "summary docs must not duplicate detailed PawnAdvance renderer wording"
      )

    assert(manifest.contains("PawnAdvance-0 authority lives in `StoryInteractionLaw.md`."))
    assert(manifest.contains("legacy broad PawnTactic"))

  test("PawnStop-0 charter opens only bounded immediate passed pawn next-square stop"):
    val readme = Files.readString(docsRoot.resolve("README.md"))
    val ssot = Files.readString(docsRoot.resolve("ChessCommentarySSOT.md"))
    val architecture = Files.readString(docsRoot.resolve("ChessModelArchitecture.md"))
    val contract = Files.readString(docsRoot.resolve("ChessModelContract.md"))
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))
    val manifest = Files.readString(docsRoot.resolve("LegacyPruneManifest.md"))

    Vector(
      "### PawnStop-0 Charter",
      "PawnStop-0 opens only the second narrow Pawn / Promotion Neighborhood vertical slice.",
      "First positive scope is not broad pawn defense or endgame hold.",
      "Pawn facts observe structure. PawnStopProof proves the next square is stopped. Scene.PawnStop may speak only bounded immediate stop, not endgame defense.",
      "`Scene.PawnStop`",
      "`PawnStopProof`",
      "`stops_pawn_advance`",
      "`{route} stops the passed pawn from advancing next.`",
      "- an already-passed target pawn.",
      "- the target pawn's next advance square.",
      "- a legal move directly stops that next advance square on the exact after-board.",
      "- promotion stop.",
      "- permanent stop.",
      "- tablebase draw.",
      "- best defense or only move.",
      "- winning or losing endgame.",
      "- conversion stopped.",
      "- pawn race.",
      "- king route or opposition.",
      "- broad pawn strategy.",
      "PawnStop-0 opens no promotion stop, permanent stop, tablebase draw, best",
      "Completion standard: PawnStop-0 closes as one narrow proof-backed passed-pawn"
    ).foreach: required =>
      assert(interactionLaw.contains(required), s"PawnStop-0 law must pin: $required")

    Vector(readme, ssot, architecture, contract).foreach: doc =>
      val normalizedDoc = doc.replaceAll("\\s+", " ")
      assert(
        normalizedDoc.contains("Current implementation scope is Pawn / Promotion Neighborhood closeout.")
      )
      assert(normalizedDoc.contains("PawnStop-0 is owned by `StoryInteractionLaw.md`."))
      assert(normalizedDoc.contains("The only bounded claim key is `stops_pawn_advance`"))
      assert(
        !doc.contains("### PawnStop-0 Charter"),
        "summary docs must not duplicate detailed PawnStop charter"
      )
      assert(
        !doc.contains("Allowed deterministic wording:"),
        "summary docs must not duplicate detailed PawnStop renderer wording"
      )

    assert(manifest.contains("PawnStop-0 authority lives in `StoryInteractionLaw.md`."))
    assert(manifest.contains("legacy broad pawn defense"))

  test("PromotionThreat-0 charter opens only immediate next-move promotion threat"):
    val readme = Files.readString(docsRoot.resolve("README.md"))
    val ssot = Files.readString(docsRoot.resolve("ChessCommentarySSOT.md"))
    val architecture = Files.readString(docsRoot.resolve("ChessModelArchitecture.md"))
    val contract = Files.readString(docsRoot.resolve("ChessModelContract.md"))
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))
    val manifest = Files.readString(docsRoot.resolve("LegacyPruneManifest.md"))

    Vector(
      "### PromotionThreat-0 Charter",
      "PromotionThreat-0 opens only the third narrow Pawn / Promotion Neighborhood vertical slice.",
      "Pawn facts observe structure. PromotionThreatProof proves the next-move",
      "promotion threat. Scene.PromotionThreat may speak only immediate threat, not",
      "`Scene.PromotionThreat`",
      "`PromotionThreatProof`",
      "`threatens_promotion_next`",
      "- legal pawn move",
      "- exact after-board",
      "- next move promotion is legal for the same pawn on that after-board",
      "- promotion square",
      "- promotion route",
      "- actual Promotion Story",
      "- unstoppable pawn",
      "- cannot be stopped",
      "- winning endgame",
      "- conversion",
      "- tablebase claim",
      "- pawn race result",
      "- best move or only move",
      "- forced win",
      "- no counterplay",
      "- `{route} threatens to promote next.`",
      "Renderer input is `ExplanationPlan` only.",
      "It does not open public/user-facing LLM narration.",
      "Completion standard: PromotionThreat-0 closes when a legal pawn move creating a"
    ).foreach: required =>
      assert(interactionLaw.contains(required), s"PromotionThreat-0 law must pin: $required")

    Vector(readme, ssot, architecture, contract).foreach: doc =>
      val normalizedDoc = doc.replaceAll("\\s+", " ")
      assert(
        normalizedDoc.contains("Current implementation scope is Pawn / Promotion Neighborhood closeout.")
      )
      assert(normalizedDoc.contains("PromotionThreat-0 is owned by `StoryInteractionLaw.md`."))
      assert(normalizedDoc.contains("The only bounded claim key is `threatens_promotion_next`"))
      assert(
        !doc.contains("### PromotionThreat-0 Charter"),
        "summary docs must not duplicate detailed PromotionThreat charter"
      )
      assert(
        !doc.contains("PromotionThreat-0 duplicate audit:"),
        "summary docs must not duplicate PromotionThreat duplicate audit"
      )

    assert(manifest.contains("PromotionThreat-0 authority lives in `StoryInteractionLaw.md`."))
    assert(manifest.contains("legacy actual Promotion Story"))

  test("PromotionThreat-1 pins PromotionThreatProof as diagnostic proof home only"):
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))

    Vector(
      "### PromotionThreat-1 PromotionThreatProof",
      "`PromotionThreatProof` is the proof home for the narrow",
      "- threatening side",
      "- rival side",
      "- pawn identity",
      "- pawn move that creates the threat",
      "- exact after-board replay",
      "- next promotion move",
      "- promotion square",
      "- promotion route",
      "- next promotion move is legal on the after-board",
      "- pawn is non-promoted before the creating move",
      "- same-board proof",
      "`PromotionThreatProof` is not a public `Story`.",
      "`PawnAdvanceProof` does not own `PromotionThreat`.",
      "`PawnStopProof` does not own `PromotionThreat`.",
      "- `unstoppable`",
      "- `cannot be stopped`",
      "- `will queen`",
      "- `wins`",
      "- `conversion`",
      "- `forced`",
      "- `tablebase`",
      "proofFailures must not become renderer input or LLM input."
    ).foreach: required =>
      assert(interactionLaw.contains(required), s"PromotionThreat-1 law must pin: $required")

  test("Promotion-0 and Promotion-1 pin actual non-capturing promotion proof without Story output"):
    val readme = Files.readString(docsRoot.resolve("README.md"))
    val ssot = Files.readString(docsRoot.resolve("ChessCommentarySSOT.md"))
    val architecture = Files.readString(docsRoot.resolve("ChessModelArchitecture.md"))
    val contract = Files.readString(docsRoot.resolve("ChessModelContract.md"))
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))
    val manifest = Files.readString(docsRoot.resolve("LegacyPruneManifest.md"))

    Vector(
      "### Promotion-0 Charter",
      "Promotion-0 opens only the fourth narrow Pawn / Promotion Neighborhood vertical slice.",
      "PromotionProof proves the legal promotion event. Scene.Promotion may speak only the promotion event, not conversion or winning.",
      "- narrow `Scene.Promotion`",
      "- legal pawn promotion move",
      "- non-capturing promotion only",
      "- exact board replay",
      "- promotion square",
      "- promoted piece identity",
      "- capture promotion",
      "- promotion material result",
      "- winning endgame",
      "- conversion",
      "- decisive advantage",
      "- best move / only move",
      "- forced win",
      "- tablebase result",
      "- promotion threat",
      "- unstoppable pawn",
      "- public route `200`",
      "- production API",
      "- public/user-facing LLM narration",
      "### Promotion-1 PromotionProof",
      "`PromotionProof` is the proof home for the narrow actual promotion event.",
      "- promoting side",
      "- rival side",
      "- pawn identity",
      "- origin square",
      "- promotion square",
      "- legal promotion move",
      "- move is non-capturing",
      "- promoted piece identity",
      "- exact board replay",
      "- pawn reaches final rank",
      "- same-board proof",
      "`PromotionProof` is not a public `Story`.",
      "`PromotionThreatProof` does not own actual Promotion.",
      "- `wins`",
      "- `decisive`",
      "- `conversion`",
      "- `promotion is enough`",
      "- `best move`",
      "- `tablebase`",
      "- material value claim",
      "proofFailures must not become renderer input or LLM input."
    ).foreach: required =>
      assert(interactionLaw.contains(required), s"Promotion law must pin: $required")

    Vector(readme, ssot, architecture, contract).foreach: doc =>
      val normalizedDoc = doc.replaceAll("\\s+", " ")
      assert(
        normalizedDoc.contains("Current implementation scope is Pawn / Promotion Neighborhood closeout.")
      )
      assert(normalizedDoc.contains("Promotion-1 is owned by `StoryInteractionLaw.md`."))
      assert(normalizedDoc.contains("It adds only `PromotionProof` as a diagnostic proof home"))
      assert(
        !doc.contains("### Promotion-0 Charter"),
        "summary docs must not duplicate detailed Promotion charter"
      )
      assert(
        !doc.contains("### Promotion-1 PromotionProof"),
        "summary docs must not duplicate detailed PromotionProof law"
      )

    assert(manifest.contains("Promotion-1 authority lives in `StoryInteractionLaw.md`."))
    assert(manifest.contains("legacy promotion material result"))

  test("Promotion-2 pins ScenePromotion writer identity without downstream wording"):
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))
    val manifest = Files.readString(docsRoot.resolve("LegacyPruneManifest.md"))

    Vector(
      "### Promotion-2 ScenePromotion Writer",
      "`ScenePromotion` is the named writer for narrow `Scene.Promotion`.",
      "- complete `StoryProof`",
      "- complete `PromotionProof`",
      "- same-board legal replay",
      "- legal non-capturing promotion move",
      "- promotion square complete",
      "- promoted piece identity complete",
      "- writer = `ScenePromotion`",
      "- `EngineCheck` does not `Refute`",
      "- `scene = Promotion`",
      "- `tactic = None`",
      "- `plan = None`",
      "- `side = promoting side`",
      "- `rival = rival side`",
      "- `target = promotion square`",
      "- `anchor = pawn origin square`",
      "- `route = promotion move`",
      "`ScenePromotion` must not own `Scene.PromotionThreat` meaning.",
      "`ScenePromotion` must not create a `Scene.Material` claim.",
      "`ScenePromotion` must not create winning, conversion, or tablebase meaning.",
      "Promotion-2 does not open ExplanationPlan, renderer, LLM narration, public route `200`, or production API."
    ).foreach: required =>
      assert(interactionLaw.contains(required), s"Promotion-2 law must pin: $required")

    assert(manifest.contains("Promotion-2 authority lives in `StoryInteractionLaw.md`."))
    assert(manifest.contains("legacy promotion-threat-by-promotion"))

  test("Promotion-3 pins actual promotion negative corpus as legal event proof or silence"):
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))
    val manifest = Files.readString(docsRoot.resolve("LegacyPruneManifest.md"))

    Vector(
      "### Promotion-3 Negative Corpus",
      "A pawn near promotion is not Promotion. Legal promotion event proof or silence.",
      "- legal move 아님",
      "- same-board proof 없음",
      "- pawn move가 아님",
      "- final rank에 도달하지 않음",
      "- promotion piece identity 없음",
      "- capture promotion",
      "- promotion threat일 뿐 actual promotion 아님",
      "- material result를 promotion claim으로 말하려 함",
      "- winning/conversion/tablebase wording 유입",
      "`ScenePromotion` must stay silent for every Promotion-3 negative unless complete `PromotionProof` proves the legal non-capturing promotion event."
    ).foreach: required =>
      assert(interactionLaw.contains(required), s"Promotion-3 law must pin: $required")

    assert(manifest.contains("Promotion-3 authority lives in `StoryInteractionLaw.md`."))
    assert(manifest.contains("legacy near-promotion-as-promotion"))

  test("Promotion-4 pins EngineCheck reuse without engine-owned Promotion claims"):
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))
    val manifest = Files.readString(docsRoot.resolve("LegacyPruneManifest.md"))

    Vector(
      "### Promotion-4 EngineCheck Reuse",
      "Promotion-4 reuses only the existing `EngineCheck` sidecar.",
      "- `EngineCheck` cannot create Promotion.",
      "- `Supports` creates no new claim.",
      "- `Caps` suppresses standalone Promotion speech or weakens it to bounded relation-only evidence when downstream speech opens.",
      "- `Refutes` blocks the Promotion Story.",
      "- `Unknown` creates no engine expression.",
      "Promotion-4 forbidden engine wording:",
      "- engine says",
      "- eval number",
      "- tablebase result",
      "- best move",
      "- only move",
      "- winning endgame",
      "- forced win",
      "Completion standard: Promotion-4 closes when `EngineCheck` can attach only to an existing same-board `ScenePromotion` Story route, cannot create Promotion, cannot add a new claim under Supports, suppresses or bounds capped rows, blocks refuted rows, keeps Unknown engine-silent, and exposes no engine wording, eval numbers, tablebase, best-move, only-move, winning-endgame, or forced-win claims."
    ).foreach: required =>
      assert(interactionLaw.contains(required), s"Promotion-4 law must pin: $required")

    assert(manifest.contains("Promotion-4 authority lives in `StoryInteractionLaw.md`."))
    assert(manifest.contains("legacy engine-created-promotion"))

  test("Promotion-5 pins StoryTable integration and collision boundaries"):
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))
    val manifest = Files.readString(docsRoot.resolve("LegacyPruneManifest.md"))

    Vector(
      "### Promotion-5 StoryTable Integration",
      "Promotion-5 integrates `Scene.Promotion` into `StoryTable` collision behavior only.",
      "- `Scene.PawnAdvance`",
      "- `Scene.PawnStop`",
      "- `Scene.PromotionThreat`",
      "- `Scene.Promotion`",
      "- existing Material / Defense / Hanging / Line rows",
      "- input order remains stable",
      "- Promotion does not own PromotionThreat meaning",
      "- PromotionThreat does not own actual Promotion meaning",
      "- Promotion does not own Material claim",
      "- actual material/capture result remains in the existing material home",
      "- capped or refuted Promotion creates no standalone text",
      "Completion standard: Promotion-5 closes when `StoryTable` orders actual Promotion deterministically against PawnAdvance, PawnStop, PromotionThreat, Material, Defense, Hanging, and Line rows; Promotion and PromotionThreat cannot borrow each other's meaning; actual material and capture-result claims stay in existing material homes; and capped or refuted Promotion rows do not reach standalone ExplanationPlan, renderer, or LLM wording."
    ).foreach: required =>
      assert(interactionLaw.contains(required), s"Promotion-5 law must pin: $required")

    assert(manifest.contains("Promotion-5 authority lives in `StoryInteractionLaw.md`."))
    assert(manifest.contains("legacy promotion-steals-material"))
    assert(manifest.contains("legacy threat-steals-actual-promotion"))

  test("Promotion-6 pins ExplanationPlan selected uncapped Lead boundary"):
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))
    val manifest = Files.readString(docsRoot.resolve("LegacyPruneManifest.md"))

    Vector(
      "### Promotion-6 ExplanationPlan",
      "Promotion-6 lowers only a selected uncapped `Lead` Verdict for `Scene.Promotion`.",
      "`ExplanationPlan` may admit only the `promotes_pawn` claim key.",
      "- `promotes_pawn`",
      "- `wins_endgame`",
      "- `converts_advantage`",
      "- `decisive`",
      "- `best_move`",
      "- `only_move`",
      "- `forced_win`",
      "- `tablebase_win`",
      "- `unstoppable_pawn`",
      "- `material_gain`",
      "Support, Context, Blocked, capped, and refuted Promotion rows have no standalone claim.",
      "Completion standard: Promotion-6 closes when only selected uncapped Lead Promotion Verdicts lower to ExplanationPlan with `promotes_pawn`; forbidden keys remain closed; Support, Context, Blocked, capped, and refuted Promotion rows produce no standalone ExplanationPlan claim; and renderer, LLM, production API, public route, winning, conversion, material-gain, best-move, only-move, forced-win, tablebase, and unstoppable-pawn speech remain closed."
    ).foreach: required =>
      assert(interactionLaw.contains(required), s"Promotion-6 law must pin: $required")

    assert(manifest.contains("Promotion-6 authority lives in `StoryInteractionLaw.md`."))
    assert(manifest.contains("legacy promotion-explanation-as-win"))

  test("Promotion-7 pins DeterministicRenderer bounded promotion wording only"):
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))
    val manifest = Files.readString(docsRoot.resolve("LegacyPruneManifest.md"))

    Vector(
      "### Promotion-7 Deterministic Renderer",
      "Renderer input is `ExplanationPlan` only.",
      "- `{route} promotes the pawn.`",
      "- `wins`",
      "- `winning endgame`",
      "- `decisive`",
      "- `converts`",
      "- `best move`",
      "- `only move`",
      "- `forces`",
      "- `tablebase win`",
      "- `wins material`",
      "- `cannot be stopped`",
      "Renderer must not read `Story`, `Verdict`, `PromotionProof`, `BoardFacts`, `EngineCheck`, `proofFailures`, or source rows.",
      "Completion standard: Promotion-7 closes when `DeterministicRenderer` accepts only `ExplanationPlan`, phrases selected uncapped Lead Promotion as `{route} promotes the pawn.`, rejects Support, Context, Blocked, capped, refuted, no-claim, and malformed plans, and emits none of the forbidden winning, conversion, best/only, forced, tablebase, material-gain, or unstoppable wording."
    ).foreach: required =>
      assert(interactionLaw.contains(required), s"Promotion-7 law must pin: $required")

    assert(manifest.contains("Promotion-7 authority lives in `StoryInteractionLaw.md`."))
    assert(manifest.contains("legacy promotion-renderer-as-win"))

  test("Promotion-8 pins LLM smoke 8B-only prompt boundary"):
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))
    val manifest = Files.readString(docsRoot.resolve("LegacyPruneManifest.md"))

    Vector(
      "### Promotion-8 LLM Smoke",
      "Promotion-8 reuses only the existing 8B LLM smoke boundary.",
      "Promotion-8 allowed LLM smoke input:",
      "- renderedText",
      "- claimKey",
      "- strength",
      "- forbidden wording",
      "- `Rephrase only. Do not add chess facts.`",
      "Promotion-8 forbidden LLM smoke inputs:",
      "- raw `Story`",
      "- raw `PromotionProof`",
      "- `BoardFacts`",
      "- `EngineCheck`",
      "- raw PV",
      "- `proofFailures`",
      "Promotion-8 LLM smoke must reject output that adds:",
      "- new move",
      "- new line",
      "- winning, conversion, tablebase, or material-gain claim",
      "Completion standard: Promotion-8 closes when LLM smoke receives only rendered text contract fields for Promotion, rejects raw Story, PromotionProof, BoardFacts, EngineCheck, raw PV, proofFailures, new moves, new lines, and winning, conversion, tablebase, or material-gain claims, and reuses `Rephrase only. Do not add chess facts.` without adding new LLM behavior."
    ).foreach: required =>
      assert(interactionLaw.contains(required), s"Promotion-8 law must pin: $required")

    assert(manifest.contains("Promotion-8 authority lives in `StoryInteractionLaw.md`."))
    assert(manifest.contains("legacy promotion-llm-as-win"))

  test("Promotion Closeout hard cleanup keeps Promotion authority narrow"):
    val readme = Files.readString(docsRoot.resolve("README.md"))
    val ssot = Files.readString(docsRoot.resolve("ChessCommentarySSOT.md"))
    val architecture = Files.readString(docsRoot.resolve("ChessModelArchitecture.md"))
    val contract = Files.readString(docsRoot.resolve("ChessModelContract.md"))
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))
    val manifest = Files.readString(docsRoot.resolve("LegacyPruneManifest.md"))

    Vector(
      "### Promotion Closeout Hard Cleanup",
      "`PromotionProof`, `Scene.Promotion`, and `promotes_pawn` remain separate authority homes.",
      "`PromotionProof` owns only proof of the legal non-capturing promotion event.",
      "`Scene.Promotion` owns only the public Story identity for that event.",
      "`promotes_pawn` owns only bounded downstream speech for the selected uncapped Lead Verdict.",
      "`Scene.Promotion` owns no `Scene.PromotionThreat`, `Scene.PawnAdvance`, `Scene.PawnStop`, or PawnBreak meaning.",
      "`Scene.Promotion` owns no Material, Defense, Hanging, or Line / Defender tactic meaning.",
      "`winning`, `conversion`, `decisive`, `tablebase`, and material-gain wording remain forbidden wording only, not live Promotion authority.",
      "Capture promotion remains closed in this slice.",
      "- one chess meaning: legal non-capturing pawn promotion event only",
      "- one proof home: `PromotionProof`",
      "- one Story label: `Scene.Promotion`",
      "- one speech key: `promotes_pawn`",
      "- one detailed live authority document: `StoryInteractionLaw.md`",
      "`README.md`, SSOT, Architecture, Contract, and Manifest may summarize Promotion only.",
      "Renderer and LLM wording must remain no stronger than `promotes_pawn`.",
      "Public route `200`, production API, and public/user-facing LLM narration remain closed.",
      "Completion standard: Promotion Closeout closes when `PromotionProof`, `Scene.Promotion`, and `promotes_pawn` remain separate authority homes, Promotion owns no PromotionThreat, PawnAdvance, PawnStop, PawnBreak, Material, Defense, Hanging, or Line / Defender tactic meaning, winning, conversion, decisive, tablebase, and material-gain wording remains forbidden only, capture promotion remains closed, renderer and LLM smoke stay bounded to `promotes_pawn`, detailed authority stays only in `StoryInteractionLaw.md`, and public route `200`, production API, and public/user-facing LLM narration remain closed."
    ).foreach: required =>
      assert(interactionLaw.contains(required), s"Promotion Closeout law must pin: $required")

    Vector(readme, ssot, architecture, contract).foreach: doc =>
      val normalizedDoc = doc.replaceAll("\\s+", " ")
      assert(
        normalizedDoc.contains("Current implementation scope is Pawn / Promotion Neighborhood closeout.")
      )
      assert(
        !doc.contains("### Promotion Closeout Hard Cleanup"),
        "summary docs must not duplicate detailed Promotion Closeout law"
      )
      assert(
        !doc.contains("Promotion Closeout duplicate checks:"),
        "summary docs must not duplicate detailed Promotion Closeout checklist"
      )

    assert(manifest.contains("Promotion Closeout authority lives in `StoryInteractionLaw.md`."))
    assert(manifest.contains("legacy promotion-closeout-as-win"))

  test("PromotionThreat-2 pins ScenePromotionThreat writer identity and EngineCheck boundary"):
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))

    Vector(
      "### PromotionThreat-2 ScenePromotionThreat Writer",
      "`ScenePromotionThreat` is the named writer for narrow `Scene.PromotionThreat`.",
      "- complete `StoryProof`",
      "- complete `PromotionThreatProof`",
      "- same-board legal replay",
      "- legal creating pawn move",
      "- legal next promotion move on exact after-board",
      "- writer = `ScenePromotionThreat`",
      "- `EngineCheck` does not `Refute`",
      "- `scene = PromotionThreat`",
      "- `tactic = None`",
      "- `plan = None`",
      "- `side = threatening side`",
      "- `rival = rival side`",
      "- `target = promotion square`",
      "- `anchor = pawn origin square`",
      "- `route = creating pawn move`",
      "- `secondaryTarget = None` unless the fixed identity shape requires the promotion destination",
      "`ScenePromotionThreat` must not create actual Promotion.",
      "`ScenePromotionThreat` must not replace `PawnAdvance` or `PawnStop` meaning.",
      "`ScenePromotionThreat` must not create `unstoppable` or `wins` meaning."
    ).foreach: required =>
      assert(interactionLaw.contains(required), s"PromotionThreat-2 law must pin: $required")

  test("PromotionThreat-3 pins negative corpus as proof or silence"):
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))

    Vector(
      "### PromotionThreat-3 Negative Corpus",
      "A pawn near promotion is not a PromotionThreat. Legal next-move promotion proof or silence.",
      "- legal creating move missing",
      "- same-board proof missing",
      "- move is not a pawn move",
      "- creating move itself is promotion",
      "- next promotion move is not legal on the exact after-board",
      "- promotion square cannot be computed",
      "- next move is not promotion because two or more moves are still needed",
      "- rival stoppability check tries to expand into `unstoppable`",
      "- tablebase, winning, or conversion wording enters the path",
      "`ScenePromotionThreat` must return no Story for every negative corpus row.",
      "`PromotionThreatProof` may record internal missing evidence only.",
      "Renderer and LLM smoke must reject `unstoppable`, `wins`, `tablebase`, and `conversion` wording."
    ).foreach: required =>
      assert(interactionLaw.contains(required), s"PromotionThreat-3 law must pin: $required")

  test("PromotionThreat-4 pins EngineCheck reuse without engine-owned claims"):
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))

    Vector(
      "### PromotionThreat-4 EngineCheck Reuse",
      "PromotionThreat-4 reuses only the existing `EngineCheck` sidecar.",
      "`EngineCheck` cannot create `Scene.PromotionThreat`.",
      "- `Supports` does not create a new claim.",
      "- `Caps` suppresses standalone `threatens_promotion_next` speech or weakens it to bounded relation-only evidence.",
      "- `Refutes` blocks the PromotionThreat Story.",
      "- `Unknown` creates no engine expression.",
      "- `engine says`",
      "- eval numbers",
      "- tablebase result",
      "- best move",
      "- only move",
      "- winning endgame",
      "- forced win",
      "Completion standard: PromotionThreat-4 closes when `EngineCheck` can attach only to an existing same-board `ScenePromotionThreat` Story route, cannot create PromotionThreat, cannot add a new claim under Supports, suppresses or bounds capped rows, blocks refuted rows, keeps Unknown engine-silent, and exposes no engine wording, eval numbers, tablebase, best-move, only-move, winning-endgame, or forced-win claims."
    ).foreach: required =>
      assert(interactionLaw.contains(required), s"PromotionThreat-4 law must pin: $required")

  test("PromotionThreat-5 pins StoryTable integration and collision boundaries"):
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))

    Vector(
      "### PromotionThreat-5 StoryTable Integration",
      "PromotionThreat-5 integrates `Scene.PromotionThreat` into `StoryTable` collision behavior only.",
      "- `Scene.PawnAdvance`",
      "- `Scene.PawnStop`",
      "- existing Material / Defense / Hanging / Line rows",
      "- `Scene.PromotionThreat`",
      "- input order remains stable",
      "- PromotionThreat does not own PawnAdvance meaning",
      "- PromotionThreat does not own PawnStop meaning",
      "- PawnStop must not create a `stops promotion` claim",
      "- actual material or tactical claim remains in the existing home",
      "- capped or refuted PromotionThreat creates no standalone text",
      "Completion standard: PromotionThreat-5 closes when `StoryTable` orders PromotionThreat deterministically against PawnAdvance, PawnStop, Material, Defense, Hanging, and Line rows; no row borrows another row's claim meaning; actual material and tactical claims stay in their existing homes; and capped or refuted PromotionThreat rows do not reach standalone ExplanationPlan, renderer, or LLM wording."
    ).foreach: required =>
      assert(interactionLaw.contains(required), s"PromotionThreat-5 law must pin: $required")

  test("PromotionThreat-6 pins ExplanationPlan selected uncapped Lead boundary"):
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))

    Vector(
      "### PromotionThreat-6 ExplanationPlan",
      "PromotionThreat-6 lowers only a selected uncapped `Lead` Verdict for `Scene.PromotionThreat`.",
      "`ExplanationPlan` may admit only the `threatens_promotion_next` claim key.",
      "- `threatens_promotion_next`",
      "- `unstoppable_pawn`",
      "- `will_promote`",
      "- `cannot_be_stopped`",
      "- `wins_endgame`",
      "- `converts_advantage`",
      "- `best_move`",
      "- `only_move`",
      "- `forced`",
      "- `tablebase_win`",
      "- `no_counterplay`",
      "Support, Context, Blocked, capped, and refuted PromotionThreat rows have no standalone claim.",
      "Completion standard: PromotionThreat-6 closes when only selected uncapped Lead PromotionThreat Verdicts"
    ).foreach: required =>
      assert(interactionLaw.contains(required), s"PromotionThreat-6 law must pin: $required")

  test("PromotionThreat-7 pins DeterministicRenderer bounded wording only"):
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))

    Vector(
      "### PromotionThreat-7 Deterministic Renderer",
      "Renderer input is `ExplanationPlan` only.",
      "- `{route} threatens to promote next.`",
      "- `will promote`",
      "- `cannot be stopped`",
      "- `unstoppable`",
      "- `wins`",
      "- `winning endgame`",
      "- `converts`",
      "- `best move`",
      "- `only move`",
      "- `forces`",
      "- `tablebase win`",
      "- `no counterplay`",
      "Renderer must not read `Story`, `Verdict`, `PromotionThreatProof`, `BoardFacts`, `EngineCheck`, `proofFailures`, raw legal continuations, or source rows.",
      "Completion standard: PromotionThreat-7 closes when `DeterministicRenderer` accepts only `ExplanationPlan`"
    ).foreach: required =>
      assert(interactionLaw.contains(required), s"PromotionThreat-7 law must pin: $required")

  test("PromotionThreat-8 pins LLM smoke 8B-only prompt boundary"):
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))

    Vector(
      "### PromotionThreat-8 LLM Smoke",
      "PromotionThreat-8 reuses only the existing 8B LLM smoke boundary.",
      "- renderedText",
      "- claimKey",
      "- strength",
      "- forbidden wording",
      "- `Rephrase only. Do not add chess facts.`",
      "- raw `Story`",
      "- raw `PromotionThreatProof`",
      "- `BoardFacts`",
      "- `EngineCheck`",
      "- raw PV",
      "- `proofFailures`",
      "- new move",
      "- new line",
      "- actual promotion claim",
      "- unstoppable claim",
      "- winning claim",
      "- conversion claim",
      "- tablebase claim",
      "Completion standard: PromotionThreat-8 closes when LLM smoke receives only rendered text contract fields for PromotionThreat"
    ).foreach: required =>
      assert(interactionLaw.contains(required), s"PromotionThreat-8 law must pin: $required")

  test("PromotionThreat Closeout hard cleanup keeps authority separated and public surfaces closed"):
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))
    val readme = Files.readString(docsRoot.resolve("README.md"))
    val ssot = Files.readString(docsRoot.resolve("ChessCommentarySSOT.md"))
    val architecture = Files.readString(docsRoot.resolve("ChessModelArchitecture.md"))
    val contract = Files.readString(docsRoot.resolve("ChessModelContract.md"))
    val manifest = Files.readString(docsRoot.resolve("LegacyPruneManifest.md"))

    Vector(
      "### PromotionThreat Closeout Hard Cleanup",
      "PromotionThreat Closeout opens no new chess meaning. It only audits the PromotionThreat hard cleanup surface.",
      "PromotionThreat Closeout must confirm:",
      "- `PromotionThreatProof` proves only immediate next-move promotion-threat evidence on the exact after-board.",
      "- `Scene.PromotionThreat` is the only Story label for this immediate threat meaning.",
      "- `threatens_promotion_next` is the only speech key for this meaning.",
      "PromotionThreat owns no `Scene.PawnAdvance`, `Scene.PawnStop`, actual Promotion, or PawnBreak meaning.",
      "PromotionThreat owns no `Scene.Material`, `Scene.Defense`, `Tactic.Hanging`, or Line / Defender tactic meaning.",
      "`unstoppable`, `will promote`, `cannot be stopped`, `conversion`, and `tablebase` remain forbidden wording only, not live authority.",
      "PromotionThreat Closeout duplicate checks:",
      "- one chess meaning: immediate next-move promotion threat only",
      "- one proof home: `PromotionThreatProof`",
      "- one Story label: `Scene.PromotionThreat`",
      "- one speech key: `threatens_promotion_next`",
      "- one detailed live authority document: `StoryInteractionLaw.md`",
      "`README.md`, SSOT, Architecture, Contract, and Manifest may summarize PromotionThreat only.",
      "Renderer and LLM wording must remain no stronger than `threatens_promotion_next`.",
      "Public route `200`, production API, and public/user-facing LLM narration remain closed.",
      "Completion standard: PromotionThreat Closeout closes when `PromotionThreatProof`,"
    ).foreach: required =>
      assert(interactionLaw.contains(required), s"PromotionThreat closeout law must pin: $required")

    Vector(readme, ssot, architecture, contract, manifest).foreach: summaryDoc =>
      assert(
        summaryDoc.contains("StoryInteractionLaw.md"),
        "summary docs must point PromotionThreat authority to StoryInteractionLaw.md"
      )
      assert(
        !summaryDoc.contains("### PromotionThreat Closeout Hard Cleanup"),
        "summary docs must not duplicate detailed PromotionThreat closeout law"
      )
      assert(
        !summaryDoc.contains("PromotionThreat Closeout must confirm:"),
        "summary docs must not duplicate PromotionThreat closeout checklist"
      )
      assert(
        !summaryDoc.contains("PromotionThreat Closeout duplicate checks:"),
        "summary docs must not duplicate PromotionThreat duplicate audit"
      )

  test("PawnStop-1 pins PawnStopProof as diagnostic proof home only"):
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))

    Vector(
      "### PawnStop-1 PawnStopProof",
      "`PawnStopProof` is the proof home for narrow `Scene.PawnStop`.",
      "- stopping side",
      "- passed pawn side",
      "- passed pawn identity",
      "- pawn current square",
      "- pawn next advance square",
      "- legal stopping move",
      "- exact after-board replay",
      "- pawn was passed before move",
      "- stopping move directly occupies, attacks, or controls the next advance square",
      "- same-board proof",
      "- `NextSquareOccupied`",
      "- `NextSquareAttacked`",
      "- `NextSquareControlledByPawn`",
      "- long-term blockade",
      "- king opposition",
      "- tablebase draw",
      "- promotion race stop",
      "- tactic sequence stop",
      "- `cannot ever advance` claim",
      "`PawnStopProof` is not a public `Story`.",
      "`PassedPawnObservation` is not a public `Story`.",
      "PawnStopProof may emit internal missing evidence, but those diagnostics do",
      "Completion standard: PawnStop-1 closes when PawnStopProof exposes the named"
    ).foreach: required =>
      assert(interactionLaw.contains(required), s"PawnStop-1 law must pin: $required")

  test("PawnStop-2 pins ScenePawnStop writer identity and no-go meanings"):
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))

    Vector(
      "### PawnStop-2 Scene.PawnStop Writer",
      "`ScenePawnStop` is the named writer for `Scene.PawnStop`.",
      "- complete StoryProof",
      "- complete PawnStopProof",
      "- same-board legal replay",
      "- legal stopping move",
      "- passed pawn exists",
      "- next square stop relation complete",
      "- writer = `ScenePawnStop`",
      "- EngineCheck does not `Refute`",
      "- `scene = PawnStop`",
      "- `tactic = None`",
      "- `plan = None`",
      "- `side = stopping side`",
      "- `rival = passed pawn side`",
      "- `target = pawn next advance square`",
      "- `anchor = stopping move origin square`",
      "- `route = stopping move`",
      "ScenePawnStop must not own `Scene.Defense` meaning.",
      "ScenePawnStop must not say it stopped `PromotionThreat`.",
      "ScenePawnStop must not create endgame-result or tablebase claims."
    ).foreach: required =>
      assert(interactionLaw.contains(required), s"PawnStop-2 law must pin: $required")

  test("PawnStop-3 pins negative corpus and complete proof or silence rule"):
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))

    Vector(
      "### PawnStop-3 Negative Corpus",
      "- legal move is absent",
      "- same-board proof is absent",
      "- target pawn is not a passed pawn",
      "- target pawn next advance square cannot be calculated",
      "- after the stopping move, the next square remains empty and safely advanceable",
      "- stop expands into promotion-threat stop",
      "- king opposition, tablebase, or draw claim enters",
      "- `permanently stopped`, `cannot advance`, or `only move` wording enters",
      "Stopping the next square is not stopping the pawn forever.",
      "Complete PawnStopProof or silence."
    ).foreach: required =>
      assert(interactionLaw.contains(required), s"PawnStop-3 law must pin: $required")

  test("PawnStop-4 pins EngineCheck reuse without engine-owned claims"):
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))

    Vector(
      "### PawnStop-4 EngineCheck Reuse",
      "PawnStop-4 reuses only the existing `EngineCheck` sidecar.",
      "`EngineCheck` cannot create `Scene.PawnStop`.",
      "`Supports` does not create a new claim.",
      "`Caps` suppresses standalone `stops_pawn_advance` speech or weakens it to bounded relation-only evidence.",
      "`Refutes` blocks the PawnStop Story.",
      "`Unknown` creates no engine expression.",
      "- `engine says`",
      "- eval numbers",
      "- best defense",
      "- only move",
      "- tablebase draw",
      "- winning or losing endgame"
    ).foreach: required =>
      assert(interactionLaw.contains(required), s"PawnStop-4 law must pin: $required")

  test("PawnStop-5 pins StoryTable integration and collision boundaries"):
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))

    Vector(
      "### PawnStop-5 StoryTable Integration",
      "PawnStop-5 integrates `Scene.PawnStop` into `StoryTable` as a lower",
      "- `Tactic.Hanging`",
      "- `Tactic.Fork`",
      "- `Scene.Material`",
      "- `Scene.Defense`",
      "- `Tactic.DiscoveredAttack`",
      "- `Tactic.Pin`",
      "- `Tactic.RemoveGuard`",
      "- `Tactic.Skewer`",
      "- `Scene.PawnAdvance`",
      "- `Scene.PawnStop`",
      "- input order must not change selected role shape",
      "- PawnStop must not own `Scene.Defense` claims",
      "- PawnStop must not create `PromotionThreat` claims",
      "- PawnAdvance and PawnStop over the same pawn must not both compete as Lead",
      "- immediate tactic meaning stays in the tactic row home",
      "- actual material change stays in `Scene.Material`",
      "- capped or refuted PawnStop has no standalone text",
      "existing immediate tactic, material, defense, and same-pawn PawnAdvance homes stay",
      "cannot produce renderer or LLM standalone speech."
    ).foreach: required =>
      assert(interactionLaw.contains(required), s"PawnStop-5 law must pin: $required")

  test("PawnStop-6 pins ExplanationPlan selected uncapped Lead boundary"):
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))

    Vector(
      "### PawnStop-6 ExplanationPlan",
      "PawnStop-6 lowers only a selected uncapped `Lead` Verdict for `Scene.PawnStop`.",
      "`ExplanationPlan` may admit only the `stops_pawn_advance` claim key.",
      "- `stops_pawn_advance`",
      "- `stops_promotion`",
      "- `permanently_stops_pawn`",
      "- `draws_endgame`",
      "- `best_defense`",
      "- `only_move`",
      "- `tablebase_draw`",
      "- `wins_endgame`",
      "- `converts_advantage`",
      "- `forced`",
      "Support, Context, Blocked, capped, and refuted PawnStop rows have no standalone claim.",
      "Completion standard: PawnStop-6 closes when only selected uncapped Lead PawnStop Verdicts"
    ).foreach: required =>
      assert(interactionLaw.contains(required), s"PawnStop-6 law must pin: $required")

  test("PawnStop-7 pins DeterministicRenderer bounded wording only"):
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))

    Vector(
      "### PawnStop-7 Deterministic Renderer",
      "Renderer input is `ExplanationPlan` only.",
      "- `{route} stops the passed pawn from advancing next.`",
      "- `stops promotion`",
      "- `stops the pawn for good`",
      "- `draws`",
      "- `holds the endgame`",
      "- `best defense`",
      "- `only move`",
      "- `forces`",
      "- `wins`",
      "- `tablebase`",
      "Renderer must not read `Story`, `Verdict`, `PawnStopProof`, `EngineCheck`, `proofFailures`, or `BoardFacts`.",
      "Completion standard: PawnStop-7 closes when `DeterministicRenderer` accepts only `ExplanationPlan`"
    ).foreach: required =>
      assert(interactionLaw.contains(required), s"PawnStop-7 law must pin: $required")

  test("PawnStop-8 pins LLM smoke 8B-only prompt boundary"):
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))

    Vector(
      "### PawnStop-8 LLM Smoke",
      "PawnStop-8 reuses only the existing 8B LLM smoke boundary.",
      "- renderedText",
      "- claimKey",
      "- strength",
      "- forbidden wording",
      "- `Rephrase only. Do not add chess facts.`",
      "- raw `Story`",
      "- raw `PawnStopProof`",
      "- `BoardFacts`",
      "- `EngineCheck`",
      "- raw PV",
      "- `proofFailures`",
      "- new move",
      "- new line",
      "- promotion claim",
      "- permanent stop claim",
      "- draw claim",
      "- tablebase claim",
      "- winning claim",
      "Completion standard: PawnStop-8 closes when LLM smoke receives only rendered text contract fields"
    ).foreach: required =>
      assert(interactionLaw.contains(required), s"PawnStop-8 law must pin: $required")

  test("PawnStop Closeout hard cleanup keeps authority separated and public surfaces closed"):
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))
    val readme = Files.readString(docsRoot.resolve("README.md"))
    val ssot = Files.readString(docsRoot.resolve("ChessCommentarySSOT.md"))
    val architecture = Files.readString(docsRoot.resolve("ChessModelArchitecture.md"))
    val contract = Files.readString(docsRoot.resolve("ChessModelContract.md"))
    val manifest = Files.readString(docsRoot.resolve("LegacyPruneManifest.md"))

    Vector(
      "### PawnStop Closeout Hard Cleanup",
      "PawnStop Closeout opens no new chess meaning. It only audits the PawnStop hard cleanup surface.",
      "PawnStop Closeout must confirm:",
      "- `PassedPawnObservation` observes same-board passed-pawn structure only.",
      "- `PawnStopProof` proves the legal move directly stops the already-passed pawn's next non-promotion advance square on the exact after-board.",
      "- `Scene.PawnStop` is the only Story label for this bounded immediate next-square stop meaning.",
      "- `stops_pawn_advance` is the only speech key for this meaning.",
      "PawnStop owns no `Scene.PawnAdvance`, PromotionThreat, Promotion, or PawnBreak meaning.",
      "PawnStop owns no `Scene.Defense`, `Scene.Material`, `Tactic.Hanging`, or Line / Defender tactic meaning.",
      "`permanent stop`, `draw`, `tablebase`, `best defense`, and `only move` remain forbidden wording only, not live authority.",
      "PawnStop Closeout duplicate checks:",
      "- one chess meaning: bounded immediate passed-pawn next-square stop only",
      "- one proof home: `PawnStopProof`",
      "- one Story label: `Scene.PawnStop`",
      "- one speech key: `stops_pawn_advance`",
      "- one detailed live authority document: `StoryInteractionLaw.md`",
      "`README.md`, SSOT, Architecture, Contract, and Manifest may summarize PawnStop only.",
      "Renderer and LLM wording must remain no stronger than `stops_pawn_advance`.",
      "Public route `200`, production API, and public/user-facing LLM narration remain closed.",
      "Completion standard: PawnStop Closeout closes when `PassedPawnObservation`,"
    ).foreach: required =>
      assert(interactionLaw.contains(required), s"PawnStop closeout law must pin: $required")

    Vector(readme, ssot, architecture, contract, manifest).foreach: summaryDoc =>
      assert(
        summaryDoc.contains("PawnStop Closeout opens no new chess meaning; it audits only"),
        "summary docs must summarize PawnStop closeout"
      )
      assert(
        summaryDoc.contains("StoryInteractionLaw.md"),
        "summary docs must point PawnStop authority to StoryInteractionLaw.md"
      )
      assert(
        !summaryDoc.contains("### PawnStop Closeout Hard Cleanup"),
        "summary docs must not duplicate detailed PawnStop closeout law"
      )
      assert(
        !summaryDoc.contains("PawnStop Closeout must confirm:"),
        "summary docs must not duplicate detailed PawnStop closeout checklist"
      )
      assert(
        !summaryDoc.contains("PawnStop Closeout duplicate checks:"),
        "summary docs must not duplicate detailed PawnStop duplicate checklist"
      )

  test("PawnAdvance-1 pins PawnAdvanceProof as diagnostic proof home only"):
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))

    Vector(
      "### PawnAdvance-1 PawnAdvanceProof",
      "`PawnAdvanceProof` is the proof home for narrow `Scene.PawnAdvance`.",
      "- advancing side",
      "- pawn identity",
      "- from square",
      "- to square",
      "- legal pawn advance",
      "- move is non-capture",
      "- move is non-promotion",
      "- pawn was passed before move",
      "- pawn remains passed after move",
      "- same-board proof",
      "- exact after-board replay",
      "`PawnAdvanceProof` is not a public `Story`.",
      "`PassedPawnObservation` is not a public `Story`.",
      "- `unstoppable`",
      "- `wins`",
      "- `queens`",
      "- `promotes next`",
      "- `conversion`",
      "- `clear path`",
      "- `cannot be stopped`",
      "proofFailures must not become renderer input or LLM input."
    ).foreach: required =>
      assert(interactionLaw.contains(required), s"PawnAdvance-1 law must pin: $required")

  test("PawnAdvance-2 pins ScenePawnAdvance writer identity only"):
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))

    Vector(
      "### PawnAdvance-2 Scene.PawnAdvance Writer",
      "Named writer: `ScenePawnAdvance`.",
      "- complete `StoryProof`",
      "- complete `PawnAdvanceProof`",
      "- same-board legal replay",
      "- legal non-capturing non-promotion pawn advance",
      "- passed-before and passed-after proof",
      "- writer = `ScenePawnAdvance`",
      "- `EngineCheck` does not `Refute`",
      "- scene = `PawnAdvance`",
      "- tactic = `None`",
      "- plan = `None`",
      "- side = advancing side",
      "- rival = opposite side",
      "- target = destination square",
      "- anchor = pawn origin square",
      "- route = pawn advance",
      "ScenePawnAdvance must not create a PromotionThreat.",
      "ScenePawnAdvance must not create winning, conversion, or material claims.",
      "ScenePawnAdvance must not create pawn-race or unstoppable-pawn claims."
    ).foreach: required =>
      assert(interactionLaw.contains(required), s"PawnAdvance-2 law must pin: $required")

  test("PawnAdvance-3 pins negative corpus and silence rule"):
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))

    Vector(
      "### PawnAdvance-3 Negative Corpus",
      "- legal move is absent",
      "- same-board proof is absent",
      "- moving piece is not a pawn",
      "- move is a capture",
      "- move is a promotion",
      "- pawn was not passed before the move",
      "- moved pawn is not passed after the move",
      "- en passant complexity enters the proof attempt",
      "- proof attempt expands into immediate promotion threat",
      "- `unstoppable`, `winning`, or `conversion` wording enters",
      "A pawn move is not a passed-pawn Story. Complete PawnAdvanceProof or silence."
    ).foreach: required =>
      assert(interactionLaw.contains(required), s"PawnAdvance-3 law must pin: $required")

  test("PawnAdvance-4 pins EngineCheck reuse without engine-owned claims"):
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))

    Vector(
      "### PawnAdvance-4 EngineCheck Reuse",
      "PawnAdvance-4 reuses only the existing `EngineCheck` sidecar.",
      "`EngineCheck` cannot create `Scene.PawnAdvance`.",
      "`Supports` does not create a new claim.",
      "`Caps` suppresses standalone `advances_passed_pawn` speech or weakens it to relation-only bounded evidence.",
      "`Refutes` blocks the PawnAdvance Story.",
      "`Unknown` creates no engine expression.",
      "- `engine says`",
      "- eval numbers",
      "- best move",
      "- only move",
      "- winning endgame",
      "- tablebase-like claim"
    ).foreach: required =>
      assert(interactionLaw.contains(required), s"PawnAdvance-4 law must pin: $required")

  test("PawnAdvance-5 pins StoryTable integration and lower bounded claim"):
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))

    Vector(
      "### PawnAdvance-5 StoryTable Integration",
      "PawnAdvance-5 integrates `Scene.PawnAdvance` into `StoryTable` as a lower",
      "- `Tactic.Hanging`",
      "- `Tactic.Fork`",
      "- `Scene.Material`",
      "- `Scene.Defense`",
      "- `Tactic.DiscoveredAttack`",
      "- `Tactic.Pin`",
      "- `Tactic.RemoveGuard`",
      "- `Tactic.Skewer`",
      "- `Scene.PawnAdvance`",
      "- input order must not change selected role shape",
      "- PawnAdvance must not own tactical or material claims",
      "- actual material change now stays in `Scene.Material`",
      "- immediate tactic meaning stays in the tactic row home",
      "- PawnAdvance remains a lower bounded scene claim only",
      "- capped or refuted PawnAdvance has no standalone text",
      "existing immediate tactic, material, and defense homes stay",
      "cannot produce renderer or LLM standalone speech."
    ).foreach: required =>
      assert(interactionLaw.contains(required), s"PawnAdvance-5 law must pin: $required")

  test("PawnAdvance-6 pins ExplanationPlan bounded claim boundary"):
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))

    Vector(
      "### PawnAdvance-6 ExplanationPlan",
      "PawnAdvance-6 lowers only a selected uncapped `Lead` Verdict for `Scene.PawnAdvance`.",
      "`ExplanationPlan` may admit only the `advances_passed_pawn` claim key.",
      "- `advances_passed_pawn`",
      "- `promotion_threat`",
      "- `unstoppable_pawn`",
      "- `wins_endgame`",
      "- `converts_advantage`",
      "- `best_move`",
      "- `only_move`",
      "- `forced`",
      "- `decisive`",
      "- `creates_pressure`",
      "- `takes_initiative`",
      "Support, Context, Blocked, capped, and refuted PawnAdvance rows have no standalone claim.",
      "Completion standard: PawnAdvance-6 closes when only selected uncapped Lead PawnAdvance Verdicts"
    ).foreach: required =>
      assert(interactionLaw.contains(required), s"PawnAdvance-6 law must pin: $required")

  test("PawnAdvance-7 pins DeterministicRenderer bounded wording only"):
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))

    Vector(
      "### PawnAdvance-7 Deterministic Renderer",
      "Renderer input is `ExplanationPlan` only.",
      "- `{route} advances the passed pawn.`",
      "- `cannot be stopped`",
      "- `will promote`",
      "- `wins`",
      "- `winning endgame`",
      "- `converts`",
      "- `best move`",
      "- `only move`",
      "- `forces`",
      "- `decisive`",
      "- `creates pressure`",
      "Renderer must not read `Story`, `Verdict`, `PawnAdvanceProof`, `EngineCheck`, `proofFailures`, or `BoardFacts`.",
      "Completion standard: PawnAdvance-7 closes when `DeterministicRenderer` accepts only `ExplanationPlan`"
    ).foreach: required =>
      assert(interactionLaw.contains(required), s"PawnAdvance-7 law must pin: $required")

  test("PawnAdvance-8 pins LLM smoke 8B-only prompt boundary"):
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))

    Vector(
      "### PawnAdvance-8 LLM Smoke",
      "PawnAdvance-8 reuses only the existing 8B LLM smoke boundary.",
      "- renderedText",
      "- claimKey",
      "- strength",
      "- forbidden wording",
      "- `Rephrase only. Do not add chess facts.`",
      "- raw `Story`",
      "- raw `PawnAdvanceProof`",
      "- `BoardFacts`",
      "- `EngineCheck`",
      "- raw PV",
      "- `proofFailures`",
      "- new move",
      "- new line",
      "- promotion claim",
      "- unstoppable claim",
      "- winning claim",
      "- conversion claim",
      "Completion standard: PawnAdvance-8 closes when LLM smoke receives only rendered text contract fields"
    ).foreach: required =>
      assert(interactionLaw.contains(required), s"PawnAdvance-8 law must pin: $required")

  test("PawnAdvance Closeout pins hard cleanup ownership boundaries"):
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))
    val readme = Files.readString(docsRoot.resolve("README.md"))
    val ssot = Files.readString(docsRoot.resolve("ChessCommentarySSOT.md"))
    val architecture = Files.readString(docsRoot.resolve("ChessModelArchitecture.md"))
    val contract = Files.readString(docsRoot.resolve("ChessModelContract.md"))
    val manifest = Files.readString(docsRoot.resolve("LegacyPruneManifest.md"))

    Vector(
      "### PawnAdvance Closeout Hard Cleanup",
      "- `PassedPawnObservation` observes same-board pawn structure only.",
      "- `PawnAdvanceProof` binds the legal non-capturing non-promotion advance and exact after-board passed-pawn status.",
      "- `Scene.PawnAdvance` is the only Story label for this bounded pawn-progress meaning.",
      "- `advances_passed_pawn` is the only speech key for this meaning.",
      "PawnAdvance owns no PromotionThreat, Promotion, PawnStop, PawnBreak, Material, Hanging,",
      "- one chess meaning: bounded passed-pawn advance only",
      "- one proof home: `PawnAdvanceProof`",
      "- one Story label: `Scene.PawnAdvance`",
      "- one speech key: `advances_passed_pawn`",
      "- one detailed live authority document: `StoryInteractionLaw.md`",
      "`README.md`, SSOT, Architecture, Contract, and Manifest may summarize PawnAdvance only.",
      "Renderer and LLM wording must remain no stronger than `advances_passed_pawn`.",
      "Public route `200`, production API, and public/user-facing LLM narration remain closed.",
      "Completion standard: PawnAdvance Closeout closes when `PassedPawnObservation`,"
    ).foreach: required =>
      assert(interactionLaw.contains(required), s"PawnAdvance closeout law must pin: $required")

    Vector(readme, ssot, architecture, contract, manifest).foreach: summaryDoc =>
      assert(
        summaryDoc.contains("StoryInteractionLaw.md"),
        "summary docs must point PawnAdvance authority to StoryInteractionLaw.md"
      )
      assert(
        !summaryDoc.contains("### PawnAdvance Closeout Hard Cleanup"),
        "summary docs must not duplicate detailed closeout law"
      )

  test("PIH Closeout hard cleanup keeps pawn interaction authority separated and summaries short"):
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))
    val readme = Files.readString(docsRoot.resolve("README.md"))
    val ssot = Files.readString(docsRoot.resolve("ChessCommentarySSOT.md"))
    val architecture = Files.readString(docsRoot.resolve("ChessModelArchitecture.md"))
    val contract = Files.readString(docsRoot.resolve("ChessModelContract.md"))
    val manifest = Files.readString(docsRoot.resolve("LegacyPruneManifest.md"))

    Vector(
      "### PIH Closeout Hard Cleanup",
      "PIH Closeout opened no new Story family, proof home, Story writer, claim key, renderer wording, LLM behavior, public route `200`, production API, or public/user-facing LLM narration at the PawnAdvance/PawnStop baseline.",
      "PromotionThreat-0 is the third Pawn / Promotion Neighborhood slice.",
      "PIH Closeout must confirm:",
      "- no new Story family was added beyond already-open `Scene.PawnAdvance` and `Scene.PawnStop`.",
      "- no new proof home was added beyond already-open `PawnAdvanceProof` and `PawnStopProof`.",
      "- `PassedPawnObservation`, `PawnAdvanceProof`, and `PawnStopProof` authority remains separated.",
      "- `Scene.PawnAdvance` and `Scene.PawnStop` do not invade each other's meaning.",
      "- PromotionThreat, Promotion, PawnBreak, tablebase, pawn race, king route, and opposition remain closed authority and forbidden wording only.",
      "- Material, Defense, Hanging, and Line / Defender tactic homes are not invaded.",
      "PIH Closeout duplicate audit:",
      "- one chess meaning: bounded passed-pawn advance belongs to `Scene.PawnAdvance`; bounded immediate next-square stop belongs to `Scene.PawnStop`.",
      "- one proof home for each pawn meaning: `PawnAdvanceProof` for advance; `PawnStopProof` for stop.",
      "- one Story label for each pawn meaning: `Scene.PawnAdvance` and `Scene.PawnStop`.",
      "- one speech key for each pawn meaning: `advances_passed_pawn` and `stops_pawn_advance`.",
      "- one detailed live authority document: `StoryInteractionLaw.md`.",
      "`README.md`, SSOT, Architecture, Contract, and Manifest may summarize PIH Closeout only.",
      "Public route `200`, production API, and public/user-facing LLM narration remain closed.",
      "Completion standard: PIH Closeout closes when PawnAdvance and PawnStop interaction hardening has no new pawn meaning, no mixed proof authority, no sibling claim-home invasion, no duplicated detailed authority outside StoryInteractionLaw.md, no promoted test helper, no public route `200`, no production API, and no public/user-facing LLM narration."
    ).foreach: required =>
      assert(interactionLaw.contains(required), s"PIH closeout law must pin: $required")

    Vector(readme, ssot, architecture, contract, manifest).foreach: summaryDoc =>
      assert(summaryDoc.contains("PIH Closeout"), "summary docs must summarize PIH closeout")
      assert(
        summaryDoc.contains("StoryInteractionLaw.md"),
        "summary docs must point PIH closeout authority to StoryInteractionLaw.md"
      )
      assert(
        !summaryDoc.contains("### PIH Closeout Hard Cleanup"),
        "summary docs must not duplicate detailed PIH closeout law"
      )
      assert(
        !summaryDoc.contains("PIH Closeout must confirm:"),
        "summary docs must not duplicate detailed PIH checklist"
      )
      assert(
        !summaryDoc.contains("PIH Closeout duplicate audit:"),
        "summary docs must not duplicate detailed PIH duplicate audit"
      )

  test("PNC-0 closes Pawn / Promotion neighborhood without opening new meaning"):
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))
    val readme = Files.readString(docsRoot.resolve("README.md"))
    val ssot = Files.readString(docsRoot.resolve("ChessCommentarySSOT.md"))
    val architecture = Files.readString(docsRoot.resolve("ChessModelArchitecture.md"))
    val contract = Files.readString(docsRoot.resolve("ChessModelContract.md"))
    val manifest = Files.readString(docsRoot.resolve("LegacyPruneManifest.md"))
    val normalizedModelContract = contract.replaceAll("\\s+", " ")

    Vector(
      "### PNC-0 Pawn / Promotion Neighborhood Closeout",
      "Pawn / Promotion closes as four narrow proof-backed slices. It opens no pawn",
      "strategy, conversion, tablebase, pawn race, or public surface.",
      "PNC-0 opens only closeout work:",
      "- Pawn / Promotion neighborhood closeout",
      "- scope audit",
      "- duplication audit",
      "- authority audit",
      "- collision and downstream audit",
      "- docs simplification",
      "- next-neighborhood handoff",
      "PNC-0 closes only already-open narrow slices:",
      "- `Scene.PawnAdvance`",
      "- `Scene.PawnStop`",
      "- `Scene.PromotionThreat`",
      "- `Scene.Promotion`",
      "- PIH hardening over already-open pawn rows",
      "PNC-0 related proof homes:",
      "- `PawnAdvanceProof`",
      "- `PawnStopProof`",
      "- `PromotionThreatProof`",
      "- `PromotionProof`",
      "PNC-0 opens no:",
      "- new Story family",
      "- new proof home",
      "- new Story writer",
      "- new renderer template",
      "- new LLM behavior",
      "- PawnBreak",
      "- broad PawnTactic",
      "- pawn strategy",
      "- conversion",
      "- winning endgame",
      "- tablebase",
      "- pawn race",
      "- king route or opposition",
      "- public route `200`",
      "- production API",
      "- public/user-facing LLM narration",
      "PNC-0 scope audit:",
      "- PIH remains hardening only and creates no fifth pawn meaning.",
      "PNC-0 duplication audit:",
      "- one chess meaning per closed slice.",
      "- one proof home per closed slice.",
      "- one Story label per closed slice.",
      "- one speech key per closed slice.",
      "- one detailed live authority document: `StoryInteractionLaw.md`.",
      "PNC-0 authority audit:",
      "- Board Facts observes pawn structure only.",
      "- Proof homes bind only their own exact-board proof.",
      "- StoryTable orders existing rows and creates no pawn meaning.",
      "PNC-0 collision and downstream audit:",
      "- PawnAdvance, PawnStop, PromotionThreat, and Promotion must not borrow from or",
      "- Material, Defense, Hanging, and Line / Defender homes keep their own claims.",
      "- `/api/commentary/render` and `/internal/commentary/render-local-probe` remain",
      "PNC-0 docs simplification:",
      "Summary docs must not repeat PNC-0 checklists, slice closeout checklists,",
      "PNC-0 next-neighborhood handoff:",
      "- any next neighborhood must start from a new explicit charter in",
      "Completion standard: PNC-0 closes when PawnAdvance, PawnStop,"
    ).foreach: required =>
      assert(interactionLaw.contains(required), s"PNC-0 law must pin: $required")

    Vector(readme, ssot, architecture, contract).foreach: doc =>
      val normalizedDoc = doc.replaceAll("\\s+", " ")
      assert(
        normalizedDoc.contains("Current implementation scope is Pawn / Promotion Neighborhood closeout.")
      )
      assert(
        !doc.contains("### PNC-0 Pawn / Promotion Neighborhood Closeout"),
        "summary docs must not duplicate detailed PNC closeout law"
      )

    assertPawnPromotionCloseoutSummaryDocs(readme, ssot, architecture, normalizedModelContract, manifest)

  test("PNC-1 scope audit pins the four positive pawn promotion Stories only"):
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))
    val readme = Files.readString(docsRoot.resolve("README.md"))
    val ssot = Files.readString(docsRoot.resolve("ChessCommentarySSOT.md"))
    val architecture = Files.readString(docsRoot.resolve("ChessModelArchitecture.md"))
    val contract = Files.readString(docsRoot.resolve("ChessModelContract.md"))
    val manifest = Files.readString(docsRoot.resolve("LegacyPruneManifest.md"))
    val normalizedModelContract = contract.replaceAll("\\s+", " ")

    Vector(
      "### PNC-1 Scope Audit",
      "PNC-1 confirms:",
      "- the only opened positive pawn/promotion Stories are `Scene.PawnAdvance`,",
      "  `Scene.PawnStop`, `Scene.PromotionThreat`, and `Scene.Promotion`.",
      "- `Scene.PawnAdvance` speaks only its first scope:",
      "- `Scene.PawnStop` speaks only its first scope:",
      "- `Scene.PromotionThreat` speaks only its first scope:",
      "- `Scene.Promotion` speaks only its first scope:",
      "PNC-1 runtime path audit:",
      "- PawnBreak is not open as a positive Story.",
      "- pawn race is not open as a positive Story.",
      "- tablebase is not open as a positive Story.",
      "- king route is not open as a positive Story.",
      "- opposition is not open as a positive Story.",
      "- conversion is not open as a positive pawn/promotion Story.",
      "Closed pawn names are not missing features inside this neighborhood.",
      "They remain closed until a separate charter opens them.",
      "Completion standard: PNC-1 closes when the positive pawn/promotion Story",
      "is exactly four, each Story is bounded to its first scope, and PawnBreak, pawn",
      "race, tablebase, king route, opposition, and conversion"
    ).foreach: required =>
      assert(interactionLaw.contains(required), s"PNC-1 law must pin: $required")

    assertPawnPromotionCloseoutSummaryDocs(readme, ssot, architecture, normalizedModelContract, manifest)

  test("PNC-2 duplication audit keeps pawn promotion meaning homes unique"):
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))
    val readme = Files.readString(docsRoot.resolve("README.md"))
    val ssot = Files.readString(docsRoot.resolve("ChessCommentarySSOT.md"))
    val architecture = Files.readString(docsRoot.resolve("ChessModelArchitecture.md"))
    val contract = Files.readString(docsRoot.resolve("ChessModelContract.md"))
    val manifest = Files.readString(docsRoot.resolve("LegacyPruneManifest.md"))
    val normalizedModelContract = contract.replaceAll("\\s+", " ")

    Vector(
      "### PNC-2 Duplication Audit",
      "PNC-2 confirms:",
      "- no chess meaning is duplicated across two pawn/promotion Story labels.",
      "- no proof responsibility is duplicated across two pawn/promotion proof homes.",
      "- no speech claim is split across two pawn/promotion claim keys.",
      "- no detailed closeout rule is repeated across multiple live documents.",
      "PNC-2 specific duplication checks:",
      "- `Scene.PawnAdvance` and `Scene.PromotionThreat` do not co-own pawn progress.",
      "- `Scene.PawnStop` does not own the opposite meaning of PromotionThreat or Promotion.",
      "- `Scene.PromotionThreat` owns next-move promotion-route threat only.",
      "- `Scene.Promotion` owns actual legal non-capturing promotion only.",
      "- `Scene.Promotion` owns no material gain, conversion, or winning meaning.",
      "- `PawnAdvanceProof`, `PawnStopProof`, `PromotionThreatProof`, and",
      "  `PromotionProof` do not invade each other's proof responsibility.",
      "One chess meaning, one proof home, one Story label, one speech key, one live authority document.",
      "Completion standard: PNC-2 closes when PawnAdvance, PawnStop,",
      "PromotionThreat, and Promotion each have exactly one meaning, one proof",
      "one Story label, and one speech key, and detailed PNC law remains only in"
    ).foreach: required =>
      assert(interactionLaw.contains(required), s"PNC-2 law must pin: $required")

    assertPawnPromotionCloseoutSummaryDocs(readme, ssot, architecture, normalizedModelContract, manifest)

  test("PNC-3 authority audit fixes each pawn promotion layer to one job"):
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))
    val readme = Files.readString(docsRoot.resolve("README.md"))
    val ssot = Files.readString(docsRoot.resolve("ChessCommentarySSOT.md"))
    val architecture = Files.readString(docsRoot.resolve("ChessModelArchitecture.md"))
    val contract = Files.readString(docsRoot.resolve("ChessModelContract.md"))
    val manifest = Files.readString(docsRoot.resolve("LegacyPruneManifest.md"))
    val normalizedModelContract = contract.replaceAll("\\s+", " ")

    Vector(
      "### PNC-3 Authority Audit",
      "PNC-3 layer authority:",
      "- `PassedPawnObservation`: structure observation only.",
      "- `PawnAdvanceProof`: legal passed-pawn advance remains passed.",
      "- `PawnStopProof`: next advance square is directly stopped.",
      "- `PromotionThreatProof`: next-move promotion threat is legal on the exact after-board.",
      "- `PromotionProof`: actual legal promotion event.",
      "- Story writers: named proof-backed Story permission only.",
      "- `StoryTable`: ordering only.",
      "- `Verdict`: selected result only.",
      "- `ExplanationPlan`: bounded speech claim only.",
      "- Renderer: phrasing only.",
      "- LLM smoke: rephrase only.",
      "PNC-3 forbids:",
      "- proof homes speaking as Story labels.",
      "- Story writers owning conversion or result meaning.",
      "- `StoryTable` creating new pawn meaning.",
      "- `ExplanationPlan` reading raw proof.",
      "- Renderer or LLM smoke repairing or upgrading proof.",
      "PNC-3 confirms the authority chain stays:",
      "`observation` -> `proof sidecar` -> `Story` -> `Verdict` -> `ExplanationPlan` -> Renderer -> LLM smoke",
      "Completion standard: PNC-3 closes when each pawn/promotion layer keeps",
      "exactly one authority job, proof homes do not speak as Story labels,"
    ).foreach: required =>
      assert(interactionLaw.contains(required), s"PNC-3 law must pin: $required")

    assertPawnPromotionCloseoutSummaryDocs(readme, ssot, architecture, normalizedModelContract, manifest)

  test("PNC-4 collision audit pins pawn promotion rows against existing homes"):
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))
    val readme = Files.readString(docsRoot.resolve("README.md"))
    val ssot = Files.readString(docsRoot.resolve("ChessCommentarySSOT.md"))
    val architecture = Files.readString(docsRoot.resolve("ChessModelArchitecture.md"))
    val contract = Files.readString(docsRoot.resolve("ChessModelContract.md"))
    val manifest = Files.readString(docsRoot.resolve("LegacyPruneManifest.md"))
    val normalizedModelContract = contract.replaceAll("\\s+", " ")

    Vector(
      "### PNC-4 Collision Audit",
      "PNC-4 collision targets:",
      "- `Scene.PawnAdvance` vs `Scene.PawnStop`.",
      "- `Scene.PawnAdvance` vs `Scene.PromotionThreat`.",
      "- `Scene.PawnStop` vs `Scene.PromotionThreat`.",
      "- `Scene.PromotionThreat` vs `Scene.Promotion`.",
      "- `Scene.Promotion` vs `Scene.Material`.",
      "- pawn rows vs `Scene.Defense`.",
      "- pawn rows vs existing tactic and line rows.",
      "- EngineCheck Caps and Refutes over each pawn row.",
      "PNC-4 runtime checks:",
      "- input order remains stable.",
      "- duplicate Lead is absent.",
      "- incomplete rows are not Lead.",
      "- capped or refuted rows have no standalone text.",
      "- actual promotion stays in `Scene.Promotion`.",
      "- next-move promotion threat stays in `Scene.PromotionThreat`.",
      "- next advance stop stays in `Scene.PawnStop`.",
      "- passed-pawn advance stays in `Scene.PawnAdvance`.",
      "- material and tactical claims stay in their existing homes.",
      "PNC-4 reuses existing runtime tests:",
      "- `PIH-1 fixture map covers pawn interaction hardening categories`.",
      "- `PIH-2 Role Stability keeps pawn rows deterministic without duplicate pawn claims`.",
      "- `PIH-3 Meaning Ownership Boundary keeps pawn collision rows on their own claims`.",
      "- `PIH-4 EngineCheck Interaction reuses existing pawn statuses without engine-owned claims`.",
      "- `PIH-6 Downstream Boundary Smoke sends only selected Lead pawn Verdicts to text stages`.",
      "- `PromotionThreat-5 StoryTable keeps existing rows stable and claim homes separate`.",
      "- `Promotion-5 StoryTable keeps actual Promotion stable and claim homes separate`.",
      "Completion standard: PNC-4 closes when these existing runtime tests"
    ).foreach: required =>
      assert(interactionLaw.contains(required), s"PNC-4 law must pin: $required")

    assertPawnPromotionCloseoutSummaryDocs(readme, ssot, architecture, normalizedModelContract, manifest)

  test("PNC-5 downstream boundary audit keeps pawn promotion speech bounded"):
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))
    val readme = Files.readString(docsRoot.resolve("README.md"))
    val ssot = Files.readString(docsRoot.resolve("ChessCommentarySSOT.md"))
    val architecture = Files.readString(docsRoot.resolve("ChessModelArchitecture.md"))
    val contract = Files.readString(docsRoot.resolve("ChessModelContract.md"))
    val manifest = Files.readString(docsRoot.resolve("LegacyPruneManifest.md"))
    val normalizedModelContract = contract.replaceAll("\\s+", " ")

    Vector(
      "### PNC-5 Downstream Boundary Audit",
      "PNC-5 downstream authority:",
      "- only selected uncapped Lead Verdicts may lower into `ExplanationPlan`.",
      "- `ExplanationPlan` input is selected Verdict only.",
      "- Renderer input is `ExplanationPlan` only.",
      "- LLM smoke input is only renderedText, claimKey, strength, forbidden wording, and the rephrase-only instruction.",
      "PNC-5 non-Lead rows:",
      "- Support rows have no standalone text.",
      "- Context rows have no standalone text.",
      "- Blocked rows have no standalone text.",
      "- capped rows have no standalone text.",
      "- refuted rows have no standalone text.",
      "PNC-5 forbidden downstream wording:",
      "- unstoppable.",
      "- will promote.",
      "- cannot be stopped.",
      "- wins or winning.",
      "- decisive.",
      "- conversion.",
      "- tablebase.",
      "- draw or holds.",
      "- best move or only move.",
      "- forced.",
      "- no counterplay.",
      "- pressure or initiative.",
      "PNC-5 reuses existing runtime tests:",
      "- `Stage 6-5 ExplanationPlan accepts selected Verdict only`.",
      "- `Stage 6-5 ExplanationPlan exposes no raw proof material input`.",
      "- `Stage 7-1 DeterministicRenderer accepts ExplanationPlan only`.",
      "- `Stage 7-1 DeterministicRenderer cannot create text without an ExplanationPlan`.",
      "- `Stage 8B Codex CLI prompt smoke uses only rendered text contract`.",
      "- `Stage 8 narration smoke exposes no raw proof or production API input`.",
      "- `PIH-6 Downstream Boundary Smoke sends only selected Lead pawn Verdicts to text stages`.",
      "- `PawnAdvance-8 LLM smoke reuses 8B prompt boundary without new chess facts`.",
      "- `PawnStop-8 LLM smoke reuses 8B prompt boundary without new chess facts`.",
      "- `PromotionThreat-8 LLM smoke reuses 8B boundary without new chess facts`.",
      "- `Promotion-8 LLM smoke reuses 8B boundary without new chess facts`.",
      "Completion standard: PNC-5 closes when downstream stages accept only selected uncapped Lead pawn/promotion claims"
    ).foreach: required =>
      assert(interactionLaw.contains(required), s"PNC-5 law must pin: $required")

    assertPawnPromotionCloseoutSummaryDocs(readme, ssot, architecture, normalizedModelContract, manifest)

  test("PNC-6 documentation simplification keeps closeout detail in one authority"):
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))
    val readme = Files.readString(docsRoot.resolve("README.md"))
    val ssot = Files.readString(docsRoot.resolve("ChessCommentarySSOT.md"))
    val architecture = Files.readString(docsRoot.resolve("ChessModelArchitecture.md"))
    val contract = Files.readString(docsRoot.resolve("ChessModelContract.md"))
    val manifest = Files.readString(docsRoot.resolve("LegacyPruneManifest.md"))
    val normalizedModelContract = contract.replaceAll("\\s+", " ")

    Vector(
      "### PNC-6 Documentation Simplification",
      "PNC-6 documentation simplification:",
      "- detailed Pawn / Promotion closeout authority lives only in `StoryInteractionLaw.md`.",
      "- `README.md`, SSOT, Architecture, Contract, AGENTS, and LegacyPruneManifest remain summary-only.",
      "- the same closeout rule must not be repeated across live documents.",
      "PNC-6 summary-only documents:",
      "- `README.md`: summary pointer only.",
      "- `ChessCommentarySSOT.md`: summary pointer only.",
      "- `ChessModelArchitecture.md`: summary pointer only.",
      "- `ChessModelContract.md`: summary pointer only.",
      "- `AGENTS.md`: durable operator guardrails only.",
      "- `LegacyPruneManifest.md`: pruning summary only.",
      "PNC-6 closed-summary wording:",
      "- closed pawn terms are closed-summary terms, not work queue entries.",
      "- PawnTactic, pawn strategy, promotion race, conversion, tablebase, king route, and opposition remain closed-summary terms only.",
      "Completion standard: PNC-6 closes when detailed closeout checklists exist only in `StoryInteractionLaw.md`"
    ).foreach: required =>
      assert(interactionLaw.contains(required), s"PNC-6 law must pin: $required")

    assertPawnPromotionCloseoutSummaryDocs(readme, ssot, architecture, normalizedModelContract, manifest)

    val liveDocTextByName = LiveDocs.map(name => name -> Files.readString(docsRoot.resolve(name))).toMap
    liveDocTextByName
      .removed("StoryInteractionLaw.md")
      .foreach: (docName, contents) =>
        assert(!contents.contains("### PNC-"), s"$docName must not repeat detailed PNC headings")
    assert(!agents.contains("### PNC-"), "AGENTS.md must not repeat detailed PNC headings")
    assert(
      !agents.contains("PNC-6 documentation simplification:"),
      "AGENTS.md must not repeat detailed PNC law"
    )
    assert(PawnPromotionCloseoutSummary.contains("surfaces remain closed."))
    assert(PawnPromotionManifestSummary.contains("paths remain closed."))
    assert(PawnBreakSummary.contains("Scene.PawnBreak"))
    assert(PawnCaptureSummary.contains("Scene.PawnCapture"))

  test("PNC-7 test helper runtime boundary keeps helpers below pawn promotion authority"):
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))
    val readme = Files.readString(docsRoot.resolve("README.md"))
    val ssot = Files.readString(docsRoot.resolve("ChessCommentarySSOT.md"))
    val architecture = Files.readString(docsRoot.resolve("ChessModelArchitecture.md"))
    val contract = Files.readString(docsRoot.resolve("ChessModelContract.md"))
    val manifest = Files.readString(docsRoot.resolve("LegacyPruneManifest.md"))
    val normalizedModelContract = contract.replaceAll("\\s+", " ")

    Vector(
      "### PNC-7 Test Helper / Runtime Boundary Audit",
      "PNC-7 test helper runtime boundary:",
      "- test helpers are not runtime authority.",
      "- fixture names are not Story family names.",
      "- negative corpus helpers are not production concepts.",
      "- forbidden wording checks must target public overclaim wording, not arbitrary internal field names.",
      "- runtime source must not contain closeout-only terminology.",
      "PNC-7 forbids helper promotion:",
      "- no test helper may become a proof home, Story label, claim key, renderer input, or LLM input.",
      "- no fixture category may name a new pawn Story family.",
      "- no negative corpus helper may be imported by runtime code.",
      "- no forbidden wording checker may reject safe internal field names by itself.",
      "PNC-7 runtime source guard:",
      "- runtime source must not contain `PNC-`.",
      "- runtime source must not contain `closeout`.",
      "- runtime source must not contain `fixture map`.",
      "- runtime source must not contain `negative corpus`.",
      "- runtime source must not contain `Pawn Interaction Hardening`.",
      "Completion standard: PNC-7 closes when helper names remain test-local"
    ).foreach: required =>
      assert(interactionLaw.contains(required), s"PNC-7 law must pin: $required")

    assertPawnPromotionCloseoutSummaryDocs(readme, ssot, architecture, normalizedModelContract, manifest)

    val runtimeText = runtimeChessSourceText
    Vector(
      "PNC-",
      "closeout",
      "fixture map",
      "negative corpus",
      "Pawn Interaction Hardening",
      "Pawn / Promotion Closeout",
      "PromotionThreat Closeout",
      "Promotion Closeout",
      "PawnAdvance Closeout",
      "PawnStop Closeout"
    ).foreach: closeoutOnlyTerm =>
      assert(
        !runtimeText.contains(closeoutOnlyTerm),
        s"runtime source must not contain closeout-only term: $closeoutOnlyTerm"
      )

  test("PNC final completion standard closes Pawn Promotion neighborhood"):
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))
    val readme = Files.readString(docsRoot.resolve("README.md"))
    val ssot = Files.readString(docsRoot.resolve("ChessCommentarySSOT.md"))
    val architecture = Files.readString(docsRoot.resolve("ChessModelArchitecture.md"))
    val contract = Files.readString(docsRoot.resolve("ChessModelContract.md"))
    val manifest = Files.readString(docsRoot.resolve("LegacyPruneManifest.md"))
    val normalizedModelContract = contract.replaceAll("\\s+", " ")

    assert(interactionLaw.contains("### PNC Closeout Final Completion Standard"))
    Vector(
      "PNC Closeout final completion standard:",
      "- no new Story family.",
      "- no new proof home.",
      "- no new renderer wording.",
      "- no new LLM behavior.",
      "- only `Scene.PawnAdvance`, `Scene.PawnStop`, `Scene.PromotionThreat`, and `Scene.Promotion` remain as the positive closeout baseline slices.",
      "- no duplicate meaning.",
      "- no duplicate authority.",
      "- no duplicate terminology.",
      "- no duplicate detailed docs.",
      "- public route, production API, and public/user-facing LLM narration remain closed.",
      "PNC final verification:",
      "- docs authority tests pass.",
      "- chess foundation tests pass.",
      "- `git diff --check` is clean.",
      "Final PNC closeout opens no new Story family, proof home, Story writer, claim key, renderer wording, LLM behavior, fixture-derived runtime authority, negative-corpus production concept, PawnBreak, broad PawnTactic, pawn strategy, conversion, winning endgame, tablebase, pawn race, king route, opposition, public route `200`, production API, or public/user-facing LLM narration."
    ).foreach: finalLine =>
      assert(interactionLaw.contains(finalLine), s"PNC final completion must pin: $finalLine")

    assertPawnPromotionCloseoutSummaryDocs(readme, ssot, architecture, normalizedModelContract, manifest)
    Vector(readme, ssot, architecture, normalizedModelContract, manifest).foreach: doc =>
      assert(
        !doc.contains("PNC Closeout final completion standard:"),
        "summary docs must not duplicate detailed PNC final standard"
      )
      assert(
        !doc.contains("PNC final verification:"),
        "summary docs must not duplicate detailed PNC final verification"
      )

  test("PawnBreak-0 charter opens only one direct rival pawn lever"):
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))
    val readme = Files.readString(docsRoot.resolve("README.md"))
    val ssot = Files.readString(docsRoot.resolve("ChessCommentarySSOT.md"))
    val architecture = Files.readString(docsRoot.resolve("ChessModelArchitecture.md"))
    val contract = Files.readString(docsRoot.resolve("ChessModelContract.md"))
    val manifest = Files.readString(docsRoot.resolve("LegacyPruneManifest.md"))
    val normalizedModelContract = contract.replaceAll("\\s+", " ")

    Vector(
      "### PawnBreak-0 Charter",
      "PawnBreak-0 opens the first Pawn Structure / Break Neighborhood vertical slice.",
      "Core sentence: Pawn facts observe pawn contact. PawnBreakProof proves one",
      "direct lever. Scene.PawnBreak may speak only bounded pawn contact, not",
      "PawnBreak-0 opens only:",
      "- narrow `Scene.PawnBreak`",
      "- `PawnBreakProof`",
      "- `ScenePawnBreak`",
      "- legal pawn move",
      "- one rival pawn target",
      "- exact-board direct pawn lever/contact after the move",
      "- selected Verdict to bounded pawn-break wording",
      "- `challenges_pawn`",
      "PawnBreak-0 positive scope:",
      "- the moved pawn directly attacks exactly one rival pawn on the exact after-board",
      "- the direct rival-pawn lever was created by the move",
      "- complete `PawnBreakProof`",
      "- named writer = `ScenePawnBreak`",
      "PawnBreak-0 must stay silent for:",
      "- non-pawn moves",
      "- illegal moves",
      "- pawn captures",
      "- zero rival pawn targets",
      "- multiple rival pawn targets",
      "- already-existing pawn contact without a new direct lever",
      "- writerless or contaminated rows",
      "PawnBreak-0 does not open:",
      "- broad PawnTactic",
      "- broad pawn strategy",
      "- opens position",
      "- breaks through",
      "- creates passed pawn",
      "- weakens structure",
      "- wins space",
      "- initiative or pressure",
      "- conversion",
      "- winning or decisive",
      "- best move or only move",
      "- public route `200`",
      "- production API",
      "- public/user-facing LLM narration",
      "- Board Facts owns pawn contact observations.",
      "- `PawnBreakProof` proves only one legal move creating one direct rival-pawn lever.",
      "- `ScenePawnBreak` is the only writer that may create `Scene.PawnBreak`.",
      "- `StoryTable` orders an existing `Scene.PawnBreak`; it does not create it.",
      "- `ExplanationPlan` may lower only selected uncapped Lead `Scene.PawnBreak`",
      "- Renderer and LLM smoke may phrase only bounded direct-pawn-challenge wording.",
      "Completion standard: PawnBreak-0 closes when one legal pawn move creating one",
      "direct rival-pawn lever can become narrow `Scene.PawnBreak`, all zero-target,"
    ).foreach: required =>
      assert(interactionLaw.contains(required), s"PawnBreak-0 charter must pin: $required")

    Vector(readme, ssot, architecture, normalizedModelContract).foreach: doc =>
      assert(
        doc.contains(PawnBreakSummary),
        "summary docs must summarize PawnBreak-0 without detailed charter law"
      )
      assert(
        !doc.contains("### PawnBreak-0 Charter"),
        "summary docs must not duplicate detailed PawnBreak-0 law"
      )
      assert(
        !doc.contains("PawnBreak-0 positive scope:"),
        "summary docs must not duplicate PawnBreak-0 scope checklist"
      )
      assert(
        !doc.contains("PawnBreak-0 must stay silent for:"),
        "summary docs must not duplicate PawnBreak-0 silence checklist"
      )
    assert(manifest.contains(PawnBreakSummary), "LegacyPruneManifest must summarize PawnBreak-0")
    assert(
      !manifest.contains("### PawnBreak-0 Charter"),
      "LegacyPruneManifest must not duplicate detailed PawnBreak-0 law"
    )
    assert(
      !manifest.contains("PawnBreak-0 positive scope:"),
      "LegacyPruneManifest must not duplicate PawnBreak-0 scope checklist"
    )

  test("PawnBreak-1 pins PawnBreakProof as diagnostic direct lever proof home"):
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))
    val readme = Files.readString(docsRoot.resolve("README.md"))
    val ssot = Files.readString(docsRoot.resolve("ChessCommentarySSOT.md"))
    val architecture = Files.readString(docsRoot.resolve("ChessModelArchitecture.md"))
    val contract = Files.readString(docsRoot.resolve("ChessModelContract.md"))
    val manifest = Files.readString(docsRoot.resolve("LegacyPruneManifest.md"))
    val normalizedModelContract = contract.replaceAll("\\s+", " ")

    Vector(
      "### PawnBreak-1 PawnBreakProof",
      "PawnBreak-1 opens `PawnBreakProof` as the proof home for narrow",
      "`Scene.PawnBreak` direct pawn contact.",
      "PawnBreakProof must prove:",
      "- breaking side",
      "- rival side",
      "- pawn identity",
      "- origin square",
      "- destination square",
      "- legal pawn move",
      "- move is non-promotion",
      "- rival pawn target",
      "- after-board pawn lever/contact exists",
      "- same-board proof",
      "- exact after-board replay",
      "First allowed contact kinds:",
      "- `PawnChallengesPawn`",
      "- `PawnLeverCreated`",
      "Closed contact kinds:",
      "- long-term structure weakness",
      "- open-file claim",
      "- passed-pawn creation",
      "- pawn majority plan",
      "- breakthrough sequence",
      "- sacrifice line",
      "- opens the position claim",
      "`PawnBreakProof` is not a public Story.",
      "`BoardFacts.PawnLever` is not a public Story.",
      "Completion standard: PawnBreak-1 closes when `PawnBreakProof` proves only"
    ).foreach: required =>
      assert(interactionLaw.contains(required), s"PawnBreak-1 law must pin: $required")

    Vector(readme, ssot, architecture, normalizedModelContract).foreach: doc =>
      assert(doc.contains(PawnBreakSummary), "summary docs must keep the PawnBreak summary pointer")
      assert(
        !doc.contains("### PawnBreak-1 PawnBreakProof"),
        "summary docs must not duplicate detailed PawnBreak-1 law"
      )
      assert(
        !doc.contains("PawnBreakProof must prove:"),
        "summary docs must not duplicate PawnBreak-1 proof checklist"
      )
      assert(
        !doc.contains("Closed contact kinds:"),
        "summary docs must not duplicate PawnBreak-1 closed contact kinds"
      )
    assert(manifest.contains(PawnBreakSummary), "LegacyPruneManifest must keep the PawnBreak summary pointer")
    assert(
      !manifest.contains("### PawnBreak-1 PawnBreakProof"),
      "LegacyPruneManifest must not duplicate detailed PawnBreak-1 law"
    )
    assert(
      !manifest.contains("PawnBreakProof must prove:"),
      "LegacyPruneManifest must not duplicate PawnBreak-1 proof checklist"
    )

  test("PawnBreak-2 pins ScenePawnBreak as the only bounded PawnBreak writer"):
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))
    val readme = Files.readString(docsRoot.resolve("README.md"))
    val ssot = Files.readString(docsRoot.resolve("ChessCommentarySSOT.md"))
    val architecture = Files.readString(docsRoot.resolve("ChessModelArchitecture.md"))
    val contract = Files.readString(docsRoot.resolve("ChessModelContract.md"))
    val manifest = Files.readString(docsRoot.resolve("LegacyPruneManifest.md"))
    val normalizedModelContract = contract.replaceAll("\\s+", " ")

    Vector(
      "### PawnBreak-2 Scene.PawnBreak Writer",
      "PawnBreak-2 opens `ScenePawnBreak` as the named writer for narrow",
      "`Scene.PawnBreak` only.",
      "ScenePawnBreak writer conditions:",
      "- complete StoryProof",
      "- complete `PawnBreakProof`",
      "- same-board legal replay",
      "- legal non-promotion pawn move",
      "- rival pawn target exists",
      "- pawn lever/contact relation complete",
      "- writer = `ScenePawnBreak`",
      "- EngineCheck does not Refute",
      "Scene.PawnBreak Story identity:",
      "- scene = `PawnBreak`",
      "- tactic = None",
      "- plan = None",
      "- side = breaking side",
      "- rival = rival side",
      "- target = rival pawn square",
      "- anchor = pawn origin square",
      "- route = pawn move",
      "ScenePawnBreak must not create:",
      "- `PassedPawnCreated`",
      "- `Scene.Material` claim",
      "- strategy meaning",
      "- conversion meaning",
      "- initiative meaning",
      "Completion standard: PawnBreak-2 closes when only `ScenePawnBreak` can"
    ).foreach: required =>
      assert(interactionLaw.contains(required), s"PawnBreak-2 law must pin: $required")

    Vector(readme, ssot, architecture, normalizedModelContract).foreach: doc =>
      assert(doc.contains(PawnBreakSummary), "summary docs must keep the PawnBreak summary pointer")
      assert(
        !doc.contains("### PawnBreak-2 Scene.PawnBreak Writer"),
        "summary docs must not duplicate detailed PawnBreak-2 law"
      )
      assert(
        !doc.contains("ScenePawnBreak writer conditions:"),
        "summary docs must not duplicate PawnBreak-2 writer checklist"
      )
      assert(
        !doc.contains("Scene.PawnBreak Story identity:"),
        "summary docs must not duplicate PawnBreak-2 identity checklist"
      )
    assert(manifest.contains(PawnBreakSummary), "LegacyPruneManifest must keep the PawnBreak summary pointer")
    assert(
      !manifest.contains("### PawnBreak-2 Scene.PawnBreak Writer"),
      "LegacyPruneManifest must not duplicate detailed PawnBreak-2 law"
    )
    assert(
      !manifest.contains("ScenePawnBreak writer conditions:"),
      "LegacyPruneManifest must not duplicate PawnBreak-2 writer checklist"
    )

  test("PawnBreak-3 pins negative corpus to direct pawn-contact proof or silence"):
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))
    val readme = Files.readString(docsRoot.resolve("README.md"))
    val ssot = Files.readString(docsRoot.resolve("ChessCommentarySSOT.md"))
    val architecture = Files.readString(docsRoot.resolve("ChessModelArchitecture.md"))
    val contract = Files.readString(docsRoot.resolve("ChessModelContract.md"))
    val manifest = Files.readString(docsRoot.resolve("LegacyPruneManifest.md"))
    val normalizedModelContract = contract.replaceAll("\\s+", " ")

    Vector(
      "### PawnBreak-3 Negative Corpus",
      "PawnBreak-3 closes false positives around narrow `Scene.PawnBreak`.",
      "A pawn move is not a pawn break. Complete direct pawn-contact proof or silence.",
      "PawnBreak-3 must close:",
      "- not a legal move",
      "- no same-board proof",
      "- not a pawn move",
      "- promotion move",
      "- no rival pawn target",
      "- no direct pawn lever/contact on the after-board",
      "- simple space gain being called a pawn break",
      "- passed-pawn creation expansion",
      "- open-file wording",
      "- weak-pawn or weak-structure wording",
      "- initiative wording",
      "- material gain being spoken as a PawnBreak claim",
      "Completion standard: PawnBreak-3 closes when every listed counterexample"
    ).foreach: required =>
      assert(interactionLaw.contains(required), s"PawnBreak-3 law must pin: $required")

    Vector(readme, ssot, architecture, normalizedModelContract).foreach: doc =>
      assert(doc.contains(PawnBreakSummary), "summary docs must keep the PawnBreak summary pointer")
      assert(
        !doc.contains("### PawnBreak-3 Negative Corpus"),
        "summary docs must not duplicate detailed PawnBreak-3 law"
      )
      assert(
        !doc.contains("PawnBreak-3 must close:"),
        "summary docs must not duplicate PawnBreak-3 negative corpus"
      )
    assert(manifest.contains(PawnBreakSummary), "LegacyPruneManifest must keep the PawnBreak summary pointer")
    assert(
      !manifest.contains("### PawnBreak-3 Negative Corpus"),
      "LegacyPruneManifest must not duplicate detailed PawnBreak-3 law"
    )
    assert(
      !manifest.contains("PawnBreak-3 must close:"),
      "LegacyPruneManifest must not duplicate PawnBreak-3 negative corpus"
    )

  test("PawnBreak-4 pins EngineCheck reuse without engine-owned PawnBreak claims"):
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))
    val readme = Files.readString(docsRoot.resolve("README.md"))
    val ssot = Files.readString(docsRoot.resolve("ChessCommentarySSOT.md"))
    val architecture = Files.readString(docsRoot.resolve("ChessModelArchitecture.md"))
    val contract = Files.readString(docsRoot.resolve("ChessModelContract.md"))
    val manifest = Files.readString(docsRoot.resolve("LegacyPruneManifest.md"))
    val normalizedModelContract = contract.replaceAll("\\s+", " ")

    Vector(
      "### PawnBreak-4 EngineCheck Reuse",
      "PawnBreak-4 reuses the existing `EngineCheck` sidecar only.",
      "`EngineCheck` cannot create `Scene.PawnBreak`.",
      "PawnBreak EngineCheck status rules:",
      "- Supports creates no new claim",
      "- Caps suppresses standalone `Scene.PawnBreak` claim output",
      "- Refutes keeps the `Scene.PawnBreak` Story but makes it Blocked",
      "- Unknown creates no engine expression",
      "PawnBreak-4 forbids:",
      "- engine says",
      "- eval numbers",
      "- best move",
      "- only move",
      "- breakthrough works",
      "- winning structure",
      "Completion standard: PawnBreak-4 closes when `EngineCheck` can only"
    ).foreach: required =>
      assert(interactionLaw.contains(required), s"PawnBreak-4 law must pin: $required")

    Vector(readme, ssot, architecture, normalizedModelContract).foreach: doc =>
      assert(doc.contains(PawnBreakSummary), "summary docs must keep the PawnBreak summary pointer")
      assert(
        !doc.contains("### PawnBreak-4 EngineCheck Reuse"),
        "summary docs must not duplicate detailed PawnBreak-4 law"
      )
      assert(
        !doc.contains("PawnBreak EngineCheck status rules:"),
        "summary docs must not duplicate PawnBreak-4 status rules"
      )
    assert(manifest.contains(PawnBreakSummary), "LegacyPruneManifest must keep the PawnBreak summary pointer")
    assert(
      !manifest.contains("### PawnBreak-4 EngineCheck Reuse"),
      "LegacyPruneManifest must not duplicate detailed PawnBreak-4 law"
    )
    assert(
      !manifest.contains("PawnBreak EngineCheck status rules:"),
      "LegacyPruneManifest must not duplicate PawnBreak-4 status rules"
    )

  test("PawnBreak-5 pins StoryTable integration against existing claim homes"):
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))
    val readme = Files.readString(docsRoot.resolve("README.md"))
    val ssot = Files.readString(docsRoot.resolve("ChessCommentarySSOT.md"))
    val architecture = Files.readString(docsRoot.resolve("ChessModelArchitecture.md"))
    val contract = Files.readString(docsRoot.resolve("ChessModelContract.md"))
    val manifest = Files.readString(docsRoot.resolve("LegacyPruneManifest.md"))
    val normalizedModelContract = contract.replaceAll("\\s+", " ")

    Vector(
      "### PawnBreak-5 StoryTable Integration",
      "PawnBreak-5 collides narrow `Scene.PawnBreak` with existing open rows.",
      "PawnBreak-5 collision targets:",
      "- `Scene.PawnAdvance`",
      "- `Scene.PawnStop`",
      "- `Scene.PromotionThreat`",
      "- `Scene.Promotion`",
      "- `Scene.Material`",
      "- `Scene.Defense`",
      "- `Tactic.Hanging`",
      "- Line / Defender tactics",
      "- `Scene.PawnBreak`",
      "PawnBreak-5 must verify:",
      "- input order stability",
      "- `Scene.PawnBreak` owns no Material claim",
      "- `Scene.PawnBreak` creates no PassedPawnCreated claim",
      "- `Scene.PawnBreak` does not replace PawnAdvance or Promotion meaning",
      "- actual material change now remains `Scene.Material`",
      "- promotion/progress claims remain in existing pawn/promotion homes",
      "- capped/refuted `Scene.PawnBreak` has no standalone text",
      "Completion standard: PawnBreak-5 closes when StoryTable collision tests"
    ).foreach: required =>
      assert(interactionLaw.contains(required), s"PawnBreak-5 law must pin: $required")

    Vector(readme, ssot, architecture, normalizedModelContract).foreach: doc =>
      assert(doc.contains(PawnBreakSummary), "summary docs must keep the PawnBreak summary pointer")
      assert(
        !doc.contains("### PawnBreak-5 StoryTable Integration"),
        "summary docs must not duplicate detailed PawnBreak-5 law"
      )
      assert(
        !doc.contains("PawnBreak-5 collision targets:"),
        "summary docs must not duplicate PawnBreak-5 target list"
      )
      assert(
        !doc.contains("PawnBreak-5 must verify:"),
        "summary docs must not duplicate PawnBreak-5 checklist"
      )
    assert(manifest.contains(PawnBreakSummary), "LegacyPruneManifest must keep the PawnBreak summary pointer")
    assert(
      !manifest.contains("### PawnBreak-5 StoryTable Integration"),
      "LegacyPruneManifest must not duplicate detailed PawnBreak-5 law"
    )
    assert(
      !manifest.contains("PawnBreak-5 collision targets:"),
      "LegacyPruneManifest must not duplicate PawnBreak-5 target list"
    )

  test("PawnBreak-6 pins ExplanationPlan to selected uncapped Lead only"):
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))
    val readme = Files.readString(docsRoot.resolve("README.md"))
    val ssot = Files.readString(docsRoot.resolve("ChessCommentarySSOT.md"))
    val architecture = Files.readString(docsRoot.resolve("ChessModelArchitecture.md"))
    val contract = Files.readString(docsRoot.resolve("ChessModelContract.md"))
    val manifest = Files.readString(docsRoot.resolve("LegacyPruneManifest.md"))
    val normalizedModelContract = contract.replaceAll("\\s+", " ")

    Vector(
      "### PawnBreak-6 ExplanationPlan",
      "PawnBreak-6 opens ExplanationPlan only for selected uncapped Lead `Scene.PawnBreak` Verdicts.",
      "PawnBreak-6 allowed claim key:",
      "- `challenges_pawn`",
      "PawnBreak-6 forbidden claim keys:",
      "- `opens_position`",
      "- `breaks_through`",
      "- `creates_passed_pawn`",
      "- `weakens_structure`",
      "- `wins_space`",
      "- `creates_pressure`",
      "- `takes_initiative`",
      "- `converts_advantage`",
      "- `best_move`",
      "- `only_move`",
      "- `forced`",
      "Support, Context, Blocked, capped, and refuted `Scene.PawnBreak` rows have no standalone claim.",
      "Completion standard: PawnBreak-6 closes when only selected uncapped Lead"
    ).foreach: required =>
      assert(interactionLaw.contains(required), s"PawnBreak-6 law must pin: $required")

    Vector(readme, ssot, architecture, normalizedModelContract).foreach: doc =>
      assert(doc.contains(PawnBreakSummary), "summary docs must keep the PawnBreak summary pointer")
      assert(
        !doc.contains("### PawnBreak-6 ExplanationPlan"),
        "summary docs must not duplicate detailed PawnBreak-6 law"
      )
      assert(
        !doc.contains("PawnBreak-6 forbidden claim keys:"),
        "summary docs must not duplicate PawnBreak-6 forbidden keys"
      )
    assert(manifest.contains(PawnBreakSummary), "LegacyPruneManifest must keep the PawnBreak summary pointer")
    assert(
      !manifest.contains("### PawnBreak-6 ExplanationPlan"),
      "LegacyPruneManifest must not duplicate detailed PawnBreak-6 law"
    )
    assert(
      !manifest.contains("PawnBreak-6 forbidden claim keys:"),
      "LegacyPruneManifest must not duplicate PawnBreak-6 forbidden keys"
    )

  test("PawnBreak-7 pins DeterministicRenderer bounded pawn-contact wording only"):
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))
    val readme = Files.readString(docsRoot.resolve("README.md"))
    val ssot = Files.readString(docsRoot.resolve("ChessCommentarySSOT.md"))
    val architecture = Files.readString(docsRoot.resolve("ChessModelArchitecture.md"))
    val contract = Files.readString(docsRoot.resolve("ChessModelContract.md"))
    val manifest = Files.readString(docsRoot.resolve("LegacyPruneManifest.md"))
    val normalizedModelContract = contract.replaceAll("\\s+", " ")

    Vector(
      "### PawnBreak-7 Deterministic Renderer",
      "Renderer input is `ExplanationPlan` only.",
      "- `{route} challenges the pawn on {target}.`",
      "- `opens the position`",
      "- `breaks through`",
      "- `creates a passer`",
      "- `weakens the structure`",
      "- `wins space`",
      "- `takes the initiative`",
      "- `creates pressure`",
      "- `best move`",
      "- `only move`",
      "- `forces`",
      "Renderer must not read `Story`, `Verdict`, `PawnBreakProof`, `BoardFacts`, `EngineCheck`, `proofFailures`, source rows, or LLM smoke.",
      "Completion standard: PawnBreak-7 closes when `DeterministicRenderer` accepts only `ExplanationPlan`, phrases selected uncapped Lead PawnBreak as `{route} challenges the pawn on {target}.`, rejects Support, Context, Blocked, capped, refuted, no-claim, and malformed plans, and emits none of the forbidden strategy, position-opening, passed-pawn, structure, space, initiative, pressure, best/only, or forcing wording."
    ).foreach: required =>
      assert(interactionLaw.contains(required), s"PawnBreak-7 law must pin: $required")

    Vector(readme, ssot, architecture, normalizedModelContract).foreach: doc =>
      assert(doc.contains(PawnBreakSummary), "summary docs must keep the PawnBreak summary pointer")
      assert(
        !doc.contains("### PawnBreak-7 Deterministic Renderer"),
        "summary docs must not duplicate detailed PawnBreak-7 law"
      )
      assert(
        !doc.contains("PawnBreak-7 closes when `DeterministicRenderer`"),
        "summary docs must not duplicate PawnBreak-7 completion law"
      )
    assert(manifest.contains(PawnBreakSummary), "LegacyPruneManifest must keep the PawnBreak summary pointer")
    assert(
      !manifest.contains("### PawnBreak-7 Deterministic Renderer"),
      "LegacyPruneManifest must not duplicate detailed PawnBreak-7 law"
    )
    assert(
      !manifest.contains("PawnBreak-7 closes when `DeterministicRenderer`"),
      "LegacyPruneManifest must not duplicate PawnBreak-7 completion law"
    )

  test("PawnBreak-8 pins LLM smoke to existing 8B boundary only"):
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))
    val readme = Files.readString(docsRoot.resolve("README.md"))
    val ssot = Files.readString(docsRoot.resolve("ChessCommentarySSOT.md"))
    val architecture = Files.readString(docsRoot.resolve("ChessModelArchitecture.md"))
    val contract = Files.readString(docsRoot.resolve("ChessModelContract.md"))
    val manifest = Files.readString(docsRoot.resolve("LegacyPruneManifest.md"))
    val normalizedModelContract = contract.replaceAll("\\s+", " ")

    Vector(
      "### PawnBreak-8 LLM Smoke",
      "PawnBreak-8 reuses only the existing 8B LLM smoke boundary.",
      "PawnBreak-8 LLM input:",
      "- renderedText",
      "- claimKey",
      "- strength",
      "- forbidden wording",
      "- `Rephrase only. Do not add chess facts.`",
      "PawnBreak-8 forbidden LLM input:",
      "- raw `Story`",
      "- raw `PawnBreakProof`",
      "- `BoardFacts`",
      "- `PawnLever` raw data",
      "- `EngineCheck`",
      "- raw PV",
      "- `proofFailures`",
      "PawnBreak-8 forbidden LLM output:",
      "- new move",
      "- new line",
      "- strategy claim",
      "- passed-pawn claim",
      "- open-file claim",
      "- initiative claim",
      "- conversion claim",
      "Completion standard: PawnBreak-8 closes when LLM smoke receives only rendered text contract fields"
    ).foreach: required =>
      assert(interactionLaw.contains(required), s"PawnBreak-8 law must pin: $required")

    Vector(readme, ssot, architecture, normalizedModelContract).foreach: doc =>
      assert(doc.contains(PawnBreakSummary), "summary docs must keep the PawnBreak summary pointer")
      assert(
        !doc.contains("### PawnBreak-8 LLM Smoke"),
        "summary docs must not duplicate detailed PawnBreak-8 law"
      )
      assert(
        !doc.contains("PawnBreak-8 forbidden LLM input:"),
        "summary docs must not duplicate PawnBreak-8 input list"
      )
    assert(manifest.contains(PawnBreakSummary), "LegacyPruneManifest must keep the PawnBreak summary pointer")
    assert(
      !manifest.contains("### PawnBreak-8 LLM Smoke"),
      "LegacyPruneManifest must not duplicate detailed PawnBreak-8 law"
    )
    assert(
      !manifest.contains("PawnBreak-8 forbidden LLM input:"),
      "LegacyPruneManifest must not duplicate PawnBreak-8 input list"
    )

  test("PawnBreak Closeout hard cleanup keeps authority separated and public surfaces closed"):
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))
    val readme = Files.readString(docsRoot.resolve("README.md"))
    val ssot = Files.readString(docsRoot.resolve("ChessCommentarySSOT.md"))
    val architecture = Files.readString(docsRoot.resolve("ChessModelArchitecture.md"))
    val contract = Files.readString(docsRoot.resolve("ChessModelContract.md"))
    val manifest = Files.readString(docsRoot.resolve("LegacyPruneManifest.md"))
    val normalizedModelContract = contract.replaceAll("\\s+", " ")

    Vector(
      "### PawnBreak Closeout Hard Cleanup",
      "`BoardFacts.PawnLever`, `PawnBreakProof`, `Scene.PawnBreak`, and `challenges_pawn` remain separate authority homes.",
      "`BoardFacts.PawnLever` observes pawn contact only.",
      "`PawnBreakProof` proves one direct rival-pawn lever only.",
      "`Scene.PawnBreak` owns only the public Story identity for bounded direct pawn contact.",
      "`challenges_pawn` owns only bounded downstream speech for selected uncapped Lead Verdicts.",
      "`Scene.PawnBreak` owns no PawnAdvance, PawnStop, PromotionThreat, Promotion, or PassedPawnCreated meaning.",
      "`Scene.PawnBreak` owns no Material, Defense, Hanging, DiscoveredAttack, Pin, RemoveGuard, or Skewer meaning.",
      "`opens position`, `breakthrough`, `weakens structure`, `wins space`, `initiative`, and `pressure` remain forbidden wording only, not live PawnBreak authority.",
      "PawnBreak duplicate checks:",
      "- one chess meaning: one legal pawn move creating one direct rival-pawn lever.",
      "- one proof home: `PawnBreakProof`.",
      "- one Story label: `Scene.PawnBreak`.",
      "- one speech key: `challenges_pawn`.",
      "- one detailed live authority document: `StoryInteractionLaw.md`.",
      "README, SSOT, Architecture, Contract, and Manifest may summarize PawnBreak only.",
      "Renderer and LLM smoke wording must remain no stronger than `challenges_pawn`.",
      "Public route `200`, production API, and public/user-facing LLM narration remain closed.",
      "Completion standard: PawnBreak Closeout closes when BoardFacts.PawnLever, PawnBreakProof, Scene.PawnBreak, and challenges_pawn remain separate authority homes"
    ).foreach: required =>
      assert(interactionLaw.contains(required), s"PawnBreak Closeout law must pin: $required")

    Vector(readme, ssot, architecture, normalizedModelContract).foreach: doc =>
      assert(doc.contains(PawnBreakSummary), "summary docs must keep the PawnBreak summary pointer")
      assert(
        !doc.contains("### PawnBreak Closeout Hard Cleanup"),
        "summary docs must not duplicate detailed PawnBreak closeout law"
      )
      assert(
        !doc.contains("PawnBreak duplicate checks:"),
        "summary docs must not duplicate detailed PawnBreak duplicate audit"
      )
      assert(
        !doc.contains(
          "BoardFacts.PawnLever, PawnBreakProof, Scene.PawnBreak, and challenges_pawn remain separate authority homes"
        ),
        "summary docs must not duplicate PawnBreak authority-home detail"
      )
    assert(manifest.contains(PawnBreakSummary), "LegacyPruneManifest must keep the PawnBreak summary pointer")
    assert(
      !manifest.contains("### PawnBreak Closeout Hard Cleanup"),
      "LegacyPruneManifest must not duplicate detailed PawnBreak closeout law"
    )
    assert(
      !manifest.contains("PawnBreak duplicate checks:"),
      "LegacyPruneManifest must not duplicate detailed PawnBreak duplicate audit"
    )
    assert(
      !manifest.contains(
        "BoardFacts.PawnLever, PawnBreakProof, Scene.PawnBreak, and challenges_pawn remain separate authority homes"
      ),
      "LegacyPruneManifest must not duplicate PawnBreak authority-home detail"
    )

  test("PawnCapture-0 charter opens only a bounded pawn captures pawn event"):
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))
    val readme = Files.readString(docsRoot.resolve("README.md"))
    val ssot = Files.readString(docsRoot.resolve("ChessCommentarySSOT.md"))
    val architecture = Files.readString(docsRoot.resolve("ChessModelArchitecture.md"))
    val contract = Files.readString(docsRoot.resolve("ChessModelContract.md"))
    val manifest = Files.readString(docsRoot.resolve("LegacyPruneManifest.md"))
    val normalizedModelContract = contract.replaceAll("\\s+", " ")

    Vector(
      "### PawnCapture-0 Charter",
      "PawnCapture-0 opens the second Pawn Structure / Break Neighborhood vertical slice.",
      "PawnCaptureProof proves the pawn-captures-pawn event. Scene.PawnCapture may speak only that event, not material gain or structure advantage.",
      "PawnCapture-0 opens only:",
      "- narrow `Scene.PawnCapture`",
      "- `PawnCaptureProof`",
      "- `ScenePawnCapture`",
      "- legal pawn move",
      "- captured piece is one rival pawn",
      "- exact-board replay",
      "- selected Verdict to bounded pawn-capture wording",
      "- `captures_rival_pawn`",
      "PawnCapture-0 positive scope:",
      "- a legal pawn move",
      "- the moved pawn captures exactly one rival pawn on the exact board",
      "- complete StoryProof",
      "- complete `PawnCaptureProof`",
      "- named writer = `ScenePawnCapture`",
      "PawnCapture-0 must stay silent for:",
      "- non-pawn moves",
      "- illegal moves",
      "- quiet pawn moves",
      "- captured piece is not a pawn",
      "- missing exact-board proof",
      "- incomplete StoryProof",
      "- missing or incomplete `PawnCaptureProof`",
      "- writerless or contaminated rows",
      "PawnCapture-0 does not open:",
      "- material gain claim",
      "- wins pawn",
      "- creates passed pawn",
      "- opens file",
      "- weakens structure",
      "- breaks through",
      "- pawn majority change",
      "- initiative / pressure",
      "- conversion",
      "- best move / only move",
      "- public route `200`",
      "- production API",
      "- public/user-facing LLM narration",
      "- `PawnCaptureProof` proves only one legal pawn move capturing one rival pawn.",
      "- `ScenePawnCapture` is the only writer that may create `Scene.PawnCapture`.",
      "- `ExplanationPlan` may lower only selected uncapped Lead `Scene.PawnCapture` Verdicts to `captures_rival_pawn`.",
      "Completion standard: PawnCapture-0 closes when one legal pawn move capturing exactly one rival pawn can become narrow `Scene.PawnCapture`"
    ).foreach: required =>
      assert(interactionLaw.contains(required), s"PawnCapture-0 charter must pin: $required")

    Vector(readme, ssot, architecture, normalizedModelContract).foreach: doc =>
      assert(
        doc.contains(PawnCaptureSummary),
        "summary docs must summarize PawnCapture-0 without detailed charter law"
      )
      assert(
        !doc.contains("### PawnCapture-0 Charter"),
        "summary docs must not duplicate detailed PawnCapture-0 law"
      )
      assert(
        !doc.contains("PawnCapture-0 positive scope:"),
        "summary docs must not duplicate PawnCapture-0 scope checklist"
      )
      assert(
        !doc.contains("PawnCapture-0 must stay silent for:"),
        "summary docs must not duplicate PawnCapture-0 silence checklist"
      )
    assert(manifest.contains(PawnCaptureSummary), "LegacyPruneManifest must summarize PawnCapture-0")
    assert(
      !manifest.contains("### PawnCapture-0 Charter"),
      "LegacyPruneManifest must not duplicate detailed PawnCapture-0 law"
    )
    assert(
      !manifest.contains("PawnCapture-0 positive scope:"),
      "LegacyPruneManifest must not duplicate PawnCapture-0 scope checklist"
    )

  test("PassedPawnCreated-0 charter opens only a bounded newly-created passer event"):
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))
    val readme = Files.readString(docsRoot.resolve("README.md"))
    val ssot = Files.readString(docsRoot.resolve("ChessCommentarySSOT.md"))
    val architecture = Files.readString(docsRoot.resolve("ChessModelArchitecture.md"))
    val contract = Files.readString(docsRoot.resolve("ChessModelContract.md"))
    val manifest = Files.readString(docsRoot.resolve("LegacyPruneManifest.md"))
    val normalizedModelContract = contract.replaceAll("\\s+", " ")

    Vector(
      "### PassedPawnCreated-0 Charter",
      "PassedPawnCreated-0 opens the third Pawn Structure / Break Neighborhood vertical slice.",
      "PassedPawnCreatedProof proves the before/after passed-pawn change. Scene.PassedPawnCreated may speak only the newly-created passer, not promotion or conversion.",
      "PassedPawnCreated-0 opens only:",
      "- narrow `Scene.PassedPawnCreated`",
      "- `PassedPawnCreatedProof`",
      "- a legal move",
      "- before-board where the named pawn is not a passed pawn",
      "- exact after-board where the named pawn is a passed pawn",
      "- exactly one newly-created passed pawn",
      "- selected Verdict to bounded created-passer wording",
      "- `creates_passed_pawn`",
      "PassedPawnCreated-0 must stay silent for:",
      "- illegal move",
      "- non-pawn move",
      "- missing same-board proof",
      "- pawn already passed before the move",
      "- exact after-board where the moved pawn is not passed",
      "- more than one newly-created passed pawn",
      "- promotion move",
      "- missing or incomplete `PassedPawnCreatedProof`",
      "PassedPawnCreated-0 does not open:",
      "- unstoppable passer",
      "- promotion threat",
      "- actual promotion",
      "- winning endgame",
      "- conversion",
      "- pawn race",
      "- tablebase",
      "- breakthrough",
      "- initiative / pressure",
      "- best move / only move",
      "- public route `200`",
      "- production API",
      "- public/user-facing LLM narration",
      "Completion standard: PassedPawnCreated-0 closes when one legal non-promotion pawn move that changes exactly one moved pawn from not-passed before to passed on the exact after-board can become narrow `Scene.PassedPawnCreated`"
    ).foreach: required =>
      assert(interactionLaw.contains(required), s"PassedPawnCreated-0 charter must pin: $required")

    Vector(readme, ssot, architecture, normalizedModelContract).foreach: doc =>
      assert(
        doc.contains(PassedPawnCreatedSummary),
        "summary docs must summarize PassedPawnCreated-0 without detailed charter law"
      )
      assert(
        !doc.contains("### PassedPawnCreated-0 Charter"),
        "summary docs must not duplicate detailed PassedPawnCreated-0 law"
      )
      assert(
        !doc.contains("PassedPawnCreated-0 opens only:"),
        "summary docs must not duplicate PassedPawnCreated-0 scope checklist"
      )
      assert(
        !doc.contains("PassedPawnCreated-0 must stay silent for:"),
        "summary docs must not duplicate PassedPawnCreated-0 silence checklist"
      )
    assert(
      manifest.contains(PassedPawnCreatedSummary),
      "LegacyPruneManifest must summarize PassedPawnCreated-0"
    )
    assert(
      !manifest.contains("### PassedPawnCreated-0 Charter"),
      "LegacyPruneManifest must not duplicate detailed PassedPawnCreated-0 law"
    )
    assert(
      !manifest.contains("PassedPawnCreated-0 opens only:"),
      "LegacyPruneManifest must not duplicate PassedPawnCreated-0 scope checklist"
    )

  test("PassedPawnCreated-1 pins PassedPawnCreatedProof as the created-passer proof home"):
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))
    val readme = Files.readString(docsRoot.resolve("README.md"))
    val ssot = Files.readString(docsRoot.resolve("ChessCommentarySSOT.md"))
    val architecture = Files.readString(docsRoot.resolve("ChessModelArchitecture.md"))
    val contract = Files.readString(docsRoot.resolve("ChessModelContract.md"))
    val manifest = Files.readString(docsRoot.resolve("LegacyPruneManifest.md"))
    val normalizedModelContract = contract.replaceAll("\\s+", " ")

    Vector(
      "### PassedPawnCreated-1 PassedPawnCreatedProof",
      "PassedPawnCreated-1 makes `PassedPawnCreatedProof` the proof home for the current",
      "`PassedPawnCreatedProof` proves:",
      "- creating side",
      "- rival side",
      "- created passed pawn identity",
      "- origin square if moved",
      "- after square",
      "- legal creating move",
      "- exact before-board",
      "- exact after-board replay",
      "- pawn was not passed before",
      "- pawn is passed after",
      "- same-board proof",
      "First allowed creation routes:",
      "- ordinary pawn move",
      "- ordinary pawn capture, if the exact after-board creates the passed pawn",
      "Closed creation routes:",
      "- en passant",
      "- promotion",
      "- multi-move sequence",
      "- pawn race result",
      "- tablebase result",
      "- \"unstoppable\" proof",
      "`PassedPawnCreatedProof` is not a public `Story`.",
      "`PassedPawnObservation` is not a public `Story`.",
      "Completion standard: PassedPawnCreated-1 closes when `PassedPawnCreatedProof`"
    ).foreach: required =>
      assert(interactionLaw.contains(required), s"PassedPawnCreated-1 proof law must pin: $required")

    Vector(readme, ssot, architecture, normalizedModelContract).foreach: doc =>
      assert(
        doc.contains(PassedPawnCreatedSummary),
        "summary docs must keep only summary PassedPawnCreated scope"
      )
      assert(
        !doc.contains("### PassedPawnCreated-1 PassedPawnCreatedProof"),
        "summary docs must not duplicate detailed PassedPawnCreated-1 law"
      )
      assert(
        !doc.contains("`PassedPawnCreatedProof` proves:"),
        "summary docs must not duplicate PassedPawnCreated-1 proof checklist"
      )
      assert(
        !doc.contains("Closed creation routes:"),
        "summary docs must not duplicate PassedPawnCreated-1 route checklist"
      )
    assert(
      manifest.contains(PassedPawnCreatedSummary),
      "LegacyPruneManifest must keep PassedPawnCreated summary"
    )
    assert(
      !manifest.contains("### PassedPawnCreated-1 PassedPawnCreatedProof"),
      "LegacyPruneManifest must not duplicate detailed PassedPawnCreated-1 law"
    )
    assert(
      !manifest.contains("`PassedPawnCreatedProof` proves:"),
      "LegacyPruneManifest must not duplicate PassedPawnCreated-1 proof checklist"
    )

  test("PassedPawnCreated-2 pins ScenePassedPawnCreated as the only PassedPawnCreated writer"):
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))
    val readme = Files.readString(docsRoot.resolve("README.md"))
    val ssot = Files.readString(docsRoot.resolve("ChessCommentarySSOT.md"))
    val architecture = Files.readString(docsRoot.resolve("ChessModelArchitecture.md"))
    val contract = Files.readString(docsRoot.resolve("ChessModelContract.md"))
    val manifest = Files.readString(docsRoot.resolve("LegacyPruneManifest.md"))
    val normalizedModelContract = contract.replaceAll("\\s+", " ")

    Vector(
      "### PassedPawnCreated-2 Scene.PassedPawnCreated Writer",
      "Named writer: `ScenePassedPawnCreated`.",
      "PassedPawnCreated-2 writer conditions:",
      "- complete `StoryProof`",
      "- complete `PassedPawnCreatedProof`",
      "- same-board legal replay",
      "- legal creating move",
      "- before-not-passed and after-passed proof",
      "- writer is `ScenePassedPawnCreated`",
      "- `EngineCheck` does not Refute",
      "PassedPawnCreated-2 Story identity:",
      "- `scene = Scene.PassedPawnCreated`",
      "- `tactic = None`",
      "- `plan = None`",
      "- `side = creating side`",
      "- `rival = rival side`",
      "- `target = after square of created passed pawn`",
      "- `anchor = origin square or creating move origin`",
      "- `route = creating move`",
      "PassedPawnCreated-2 forbidden writer ownership:",
      "- `ScenePassedPawnCreated` must not own `Scene.PawnAdvance` meaning.",
      "- `ScenePassedPawnCreated` must not own `Scene.PawnCapture` meaning.",
      "- `ScenePassedPawnCreated` must not create `PromotionThreat`, `Promotion`, or `Conversion` meaning.",
      "Completion standard: PassedPawnCreated-2 closes when `ScenePassedPawnCreated` alone writes narrow `Scene.PassedPawnCreated`"
    ).foreach: required =>
      assert(interactionLaw.contains(required), s"PassedPawnCreated-2 law must pin: $required")

    Vector(readme, ssot, architecture, normalizedModelContract).foreach: doc =>
      assert(
        doc.contains(PassedPawnCreatedSummary),
        "summary docs must keep only summary PassedPawnCreated scope"
      )
      assert(
        !doc.contains("### PassedPawnCreated-2 Scene.PassedPawnCreated Writer"),
        "summary docs must not duplicate detailed PassedPawnCreated-2 law"
      )
      assert(
        !doc.contains("PassedPawnCreated-2 writer conditions:"),
        "summary docs must not duplicate PassedPawnCreated-2 writer checklist"
      )
      assert(
        !doc.contains("PassedPawnCreated-2 Story identity:"),
        "summary docs must not duplicate PassedPawnCreated-2 identity checklist"
      )
    assert(
      manifest.contains(PassedPawnCreatedSummary),
      "LegacyPruneManifest must keep PassedPawnCreated summary"
    )
    assert(
      !manifest.contains("### PassedPawnCreated-2 Scene.PassedPawnCreated Writer"),
      "LegacyPruneManifest must not duplicate detailed PassedPawnCreated-2 law"
    )
    assert(
      !manifest.contains("PassedPawnCreated-2 writer conditions:"),
      "LegacyPruneManifest must not duplicate PassedPawnCreated-2 writer checklist"
    )

  test("PassedPawnCreated-3 pins exact before-after negative corpus"):
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))
    val readme = Files.readString(docsRoot.resolve("README.md"))
    val ssot = Files.readString(docsRoot.resolve("ChessCommentarySSOT.md"))
    val architecture = Files.readString(docsRoot.resolve("ChessModelArchitecture.md"))
    val contract = Files.readString(docsRoot.resolve("ChessModelContract.md"))
    val manifest = Files.readString(docsRoot.resolve("LegacyPruneManifest.md"))
    val normalizedModelContract = contract.replaceAll("\\s+", " ")

    Vector(
      "### PassedPawnCreated-3 Negative Corpus",
      "A pawn move is not passer creation. Exact before/after passed-pawn proof or silence.",
      "PassedPawnCreated-3 must stay silent for:",
      "- legal move missing",
      "- same-board proof missing",
      "- exact after-board where the moved pawn is not passed",
      "- before-board where the pawn was already passed",
      "- promotion move",
      "- en passant",
      "- two-move passer creation",
      "- pawn capture event exaggerated as `Scene.PassedPawnCreated`",
      "- promotion threat wording",
      "- unstoppable wording",
      "- winning wording",
      "Negative corpus ownership:",
      "- `ScenePawnCapture` may own the bounded pawn-capture event when a pawn captures a rival pawn.",
      "- `ScenePassedPawnCreated` may not upgrade that capture into passed-pawn creation without exact before/after passer proof.",
      "- `ScenePassedPawnCreated` may not infer promotion-threat, unstoppable-passer, winning-endgame, conversion, pawn-race, or tablebase meaning.",
      "Completion standard: PassedPawnCreated-3 closes when illegal moves, missing same-board proof, after-board non-passers, already-passed-before pawns, promotion moves, en passant, two-move passer creation, capture-only pawn events, and promotion-threat, unstoppable, or winning wording all produce incomplete `PassedPawnCreatedProof`, no `Scene.PassedPawnCreated`, or rejected downstream text"
    ).foreach: required =>
      assert(interactionLaw.contains(required), s"PassedPawnCreated-3 law must pin: $required")

    Vector(readme, ssot, architecture, normalizedModelContract).foreach: doc =>
      assert(
        doc.contains(PassedPawnCreatedSummary),
        "summary docs must keep only summary PassedPawnCreated scope"
      )
      assert(
        !doc.contains("### PassedPawnCreated-3 Negative Corpus"),
        "summary docs must not duplicate detailed PassedPawnCreated-3 law"
      )
      assert(
        !doc.contains("PassedPawnCreated-3 must stay silent for:"),
        "summary docs must not duplicate PassedPawnCreated-3 negative corpus checklist"
      )
      assert(
        !doc.contains("A pawn move is not passer creation. Exact before/after passed-pawn proof or silence."),
        "summary docs must not duplicate PassedPawnCreated-3 criterion"
      )
    assert(
      manifest.contains(PassedPawnCreatedSummary),
      "LegacyPruneManifest must keep PassedPawnCreated summary"
    )
    assert(
      !manifest.contains("### PassedPawnCreated-3 Negative Corpus"),
      "LegacyPruneManifest must not duplicate detailed PassedPawnCreated-3 law"
    )
    assert(
      !manifest.contains("PassedPawnCreated-3 must stay silent for:"),
      "LegacyPruneManifest must not duplicate PassedPawnCreated-3 negative corpus checklist"
    )

  test("PassedPawnCreated-4 pins EngineCheck reuse only"):
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))
    val readme = Files.readString(docsRoot.resolve("README.md"))
    val ssot = Files.readString(docsRoot.resolve("ChessCommentarySSOT.md"))
    val architecture = Files.readString(docsRoot.resolve("ChessModelArchitecture.md"))
    val contract = Files.readString(docsRoot.resolve("ChessModelContract.md"))
    val manifest = Files.readString(docsRoot.resolve("LegacyPruneManifest.md"))
    val normalizedModelContract = contract.replaceAll("\\s+", " ")

    Vector(
      "### PassedPawnCreated-4 EngineCheck Reuse",
      "PassedPawnCreated-4 reuses only the existing `EngineCheck` sidecar.",
      "EngineCheck rules for `Scene.PassedPawnCreated`:",
      "- `EngineCheck` cannot create `Scene.PassedPawnCreated`.",
      "- `Supports` creates no new claim.",
      "- `Caps` suppresses standalone `Scene.PassedPawnCreated` claim output or weakens it to bounded relation only.",
      "- `Refutes` makes the `Scene.PassedPawnCreated` Story `Blocked`.",
      "- `Unknown` creates no engine expression.",
      "PassedPawnCreated-4 forbidden engine wording:",
      "- `engine says`",
      "- eval number",
      "- best move",
      "- only move",
      "- winning endgame",
      "- tablebase result",
      "- pawn race result",
      "Completion standard: PassedPawnCreated-4 closes when `EngineCheck` can attach only to an already complete `ScenePassedPawnCreated` Story, `Supports` leaves the bounded created-passer claim unchanged, `Caps` suppresses standalone claim output or weakens to bounded relation only, `Refutes` blocks the Story, `Unknown` produces no engine expression, and engine-says, eval-number, best-move, only-move, winning-endgame, tablebase-result, and pawn-race-result wording remain rejected."
    ).foreach: required =>
      assert(interactionLaw.contains(required), s"PassedPawnCreated-4 law must pin: $required")

    Vector(readme, ssot, architecture, normalizedModelContract).foreach: doc =>
      assert(
        doc.contains(PassedPawnCreatedSummary),
        "summary docs must keep only summary PassedPawnCreated scope"
      )
      assert(
        !doc.contains("### PassedPawnCreated-4 EngineCheck Reuse"),
        "summary docs must not duplicate detailed PassedPawnCreated-4 law"
      )
      assert(
        !doc.contains("EngineCheck rules for `Scene.PassedPawnCreated`:"),
        "summary docs must not duplicate PassedPawnCreated-4 EngineCheck checklist"
      )
      assert(
        !doc.contains("PassedPawnCreated-4 forbidden engine wording:"),
        "summary docs must not duplicate PassedPawnCreated-4 forbidden wording checklist"
      )
    assert(
      manifest.contains(PassedPawnCreatedSummary),
      "LegacyPruneManifest must keep PassedPawnCreated summary"
    )
    assert(
      !manifest.contains("### PassedPawnCreated-4 EngineCheck Reuse"),
      "LegacyPruneManifest must not duplicate detailed PassedPawnCreated-4 law"
    )
    assert(
      !manifest.contains("EngineCheck rules for `Scene.PassedPawnCreated`:"),
      "LegacyPruneManifest must not duplicate PassedPawnCreated-4 EngineCheck checklist"
    )

  test("PassedPawnCreated-5 pins StoryTable integration against existing claim homes"):
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))
    val readme = Files.readString(docsRoot.resolve("README.md"))
    val ssot = Files.readString(docsRoot.resolve("ChessCommentarySSOT.md"))
    val architecture = Files.readString(docsRoot.resolve("ChessModelArchitecture.md"))
    val contract = Files.readString(docsRoot.resolve("ChessModelContract.md"))
    val manifest = Files.readString(docsRoot.resolve("LegacyPruneManifest.md"))
    val normalizedModelContract = contract.replaceAll("\\s+", " ")

    Vector(
      "### PassedPawnCreated-5 StoryTable Integration",
      "PassedPawnCreated-5 collides `Scene.PassedPawnCreated` with existing StoryTable rows without opening promotion, conversion, or capture-event ownership.",
      "PassedPawnCreated-5 collision targets:",
      "- `Scene.PawnBreak`",
      "- `Scene.PawnCapture`",
      "- `Scene.PawnAdvance`",
      "- `Scene.PawnStop`",
      "- `Scene.PromotionThreat`",
      "- `Scene.Promotion`",
      "- `Scene.Material`",
      "- `Scene.Defense`",
      "- `Tactic.Hanging`",
      "- Line / Defender tactics",
      "- `Scene.PassedPawnCreated`",
      "PassedPawnCreated-5 verification:",
      "- input order stability",
      "- `Scene.PassedPawnCreated` does not own the `Scene.PawnCapture` event",
      "- `Scene.PassedPawnCreated` does not own `Scene.PawnAdvance` meaning",
      "- `Scene.PassedPawnCreated` does not create `Scene.PromotionThreat` or `Scene.Promotion` meaning",
      "- actual material change now remains in `Scene.Material`",
      "- capped or refuted `Scene.PassedPawnCreated` has no standalone text",
      "Completion standard: PassedPawnCreated-5 closes when StoryTable ordering is input-order stable across PassedPawnCreated collisions, existing claim homes keep lead ownership over PawnBreak, PawnCapture, PawnAdvance, PawnStop, PromotionThreat, Promotion, Material, Defense, Hanging, and line/defender tactics, same-family PassedPawnCreated rows order deterministically, PassedPawnCreated does not own capture-event, advance, promotion-threat, promotion, or material-now meaning, and capped/refuted PassedPawnCreated rows produce no standalone text."
    ).foreach: required =>
      assert(interactionLaw.contains(required), s"PassedPawnCreated-5 law must pin: $required")

    Vector(readme, ssot, architecture, normalizedModelContract).foreach: doc =>
      assert(
        doc.contains(PassedPawnCreatedSummary),
        "summary docs must keep only summary PassedPawnCreated scope"
      )
      assert(
        !doc.contains("### PassedPawnCreated-5 StoryTable Integration"),
        "summary docs must not duplicate detailed PassedPawnCreated-5 law"
      )
      assert(
        !doc.contains("PassedPawnCreated-5 collision targets:"),
        "summary docs must not duplicate PassedPawnCreated-5 target checklist"
      )
      assert(
        !doc.contains("PassedPawnCreated-5 verification:"),
        "summary docs must not duplicate PassedPawnCreated-5 verification checklist"
      )
    assert(
      manifest.contains(PassedPawnCreatedSummary),
      "LegacyPruneManifest must keep PassedPawnCreated summary"
    )
    assert(
      !manifest.contains("### PassedPawnCreated-5 StoryTable Integration"),
      "LegacyPruneManifest must not duplicate detailed PassedPawnCreated-5 law"
    )
    assert(
      !manifest.contains("PassedPawnCreated-5 collision targets:"),
      "LegacyPruneManifest must not duplicate PassedPawnCreated-5 target checklist"
    )

  test("PassedPawnCreated-6 pins ExplanationPlan to created-passer claim only"):
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))
    val readme = Files.readString(docsRoot.resolve("README.md"))
    val ssot = Files.readString(docsRoot.resolve("ChessCommentarySSOT.md"))
    val architecture = Files.readString(docsRoot.resolve("ChessModelArchitecture.md"))
    val contract = Files.readString(docsRoot.resolve("ChessModelContract.md"))
    val manifest = Files.readString(docsRoot.resolve("LegacyPruneManifest.md"))
    val normalizedModelContract = contract.replaceAll("\\s+", " ")

    Vector(
      "### PassedPawnCreated-6 ExplanationPlan",
      "PassedPawnCreated-6 lets `ExplanationPlan` accept only selected uncapped Lead `Scene.PassedPawnCreated` Verdicts.",
      "PassedPawnCreated-6 allowed claim key:",
      "- `creates_passed_pawn`",
      "PassedPawnCreated-6 forbidden claim keys:",
      "- `unstoppable_pawn`",
      "- `promotion_threat`",
      "- `will_promote`",
      "- `wins_endgame`",
      "- `converts_advantage`",
      "- `breaks_through`",
      "- `creates_pressure`",
      "- `takes_initiative`",
      "- `best_move`",
      "- `only_move`",
      "- `forced`",
      "Support, Context, Blocked, capped, and refuted `Scene.PassedPawnCreated` rows create no standalone claim.",
      "Completion standard: PassedPawnCreated-6 closes when only selected uncapped Lead PassedPawnCreated Verdicts lower to `creates_passed_pawn`, every listed forbidden claim key remains unavailable, and Support, Context, Blocked, capped, and refuted PassedPawnCreated rows produce no standalone claim."
    ).foreach: required =>
      assert(interactionLaw.contains(required), s"PassedPawnCreated-6 law must pin: $required")

    Vector(readme, ssot, architecture, normalizedModelContract).foreach: doc =>
      assert(
        doc.contains(PassedPawnCreatedSummary),
        "summary docs must keep only summary PassedPawnCreated scope"
      )
      assert(
        !doc.contains("### PassedPawnCreated-6 ExplanationPlan"),
        "summary docs must not duplicate detailed PassedPawnCreated-6 law"
      )
      assert(
        !doc.contains("PassedPawnCreated-6 forbidden claim keys:"),
        "summary docs must not duplicate PassedPawnCreated-6 forbidden claim checklist"
      )
    assert(
      manifest.contains(PassedPawnCreatedSummary),
      "LegacyPruneManifest must keep PassedPawnCreated summary"
    )
    assert(
      !manifest.contains("### PassedPawnCreated-6 ExplanationPlan"),
      "LegacyPruneManifest must not duplicate detailed PassedPawnCreated-6 law"
    )
    assert(
      !manifest.contains("PassedPawnCreated-6 forbidden claim keys:"),
      "LegacyPruneManifest must not duplicate PassedPawnCreated-6 forbidden claim checklist"
    )

  test("PassedPawnCreated-7 pins deterministic renderer created-passer wording only"):
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))
    val readme = Files.readString(docsRoot.resolve("README.md"))
    val ssot = Files.readString(docsRoot.resolve("ChessCommentarySSOT.md"))
    val architecture = Files.readString(docsRoot.resolve("ChessModelArchitecture.md"))
    val contract = Files.readString(docsRoot.resolve("ChessModelContract.md"))
    val manifest = Files.readString(docsRoot.resolve("LegacyPruneManifest.md"))
    val normalizedModelContract = contract.replaceAll("\\s+", " ")

    Vector(
      "### PassedPawnCreated-7 Deterministic Renderer",
      "Renderer input is `ExplanationPlan` only.",
      "PassedPawnCreated-7 allowed deterministic wording:",
      "- `{route} creates a passed pawn on {target}.`",
      "PassedPawnCreated-7 forbidden renderer wording:",
      "- `unstoppable`",
      "- `will promote`",
      "- `wins`",
      "- `winning endgame`",
      "- `converts`",
      "- `breaks through`",
      "- `takes the initiative`",
      "- `creates pressure`",
      "- `best move`",
      "- `only move`",
      "- `forces`",
      "Completion standard: PassedPawnCreated-7 closes when `DeterministicRenderer` accepts only `ExplanationPlan`, phrases selected uncapped Lead PassedPawnCreated as `{route} creates a passed pawn on {target}.`, rejects Support, Context, Blocked, capped, refuted, no-claim, and malformed plans, and emits none of the forbidden unstoppable, promotion, winning, conversion, breakthrough, pressure, initiative, best-move, only-move, or forced wording."
    ).foreach: required =>
      assert(interactionLaw.contains(required), s"PassedPawnCreated-7 law must pin: $required")

    Vector(readme, ssot, architecture, normalizedModelContract).foreach: doc =>
      assert(
        doc.contains(PassedPawnCreatedSummary),
        "summary docs must keep only summary PassedPawnCreated scope"
      )
      assert(
        !doc.contains("### PassedPawnCreated-7 Deterministic Renderer"),
        "summary docs must not duplicate detailed PassedPawnCreated-7 law"
      )
      assert(
        !doc.contains("PassedPawnCreated-7 forbidden renderer wording:"),
        "summary docs must not duplicate PassedPawnCreated-7 forbidden renderer checklist"
      )
    assert(
      manifest.contains(PassedPawnCreatedSummary),
      "LegacyPruneManifest must keep PassedPawnCreated summary"
    )
    assert(
      !manifest.contains("### PassedPawnCreated-7 Deterministic Renderer"),
      "LegacyPruneManifest must not duplicate detailed PassedPawnCreated-7 law"
    )
    assert(
      !manifest.contains("PassedPawnCreated-7 forbidden renderer wording:"),
      "LegacyPruneManifest must not duplicate PassedPawnCreated-7 forbidden renderer checklist"
    )

  test("PassedPawnCreated-8 pins LLM smoke 8B-only prompt boundary"):
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))
    val readme = Files.readString(docsRoot.resolve("README.md"))
    val ssot = Files.readString(docsRoot.resolve("ChessCommentarySSOT.md"))
    val architecture = Files.readString(docsRoot.resolve("ChessModelArchitecture.md"))
    val contract = Files.readString(docsRoot.resolve("ChessModelContract.md"))
    val manifest = Files.readString(docsRoot.resolve("LegacyPruneManifest.md"))
    val normalizedModelContract = contract.replaceAll("\\s+", " ")

    Vector(
      "### PassedPawnCreated-8 LLM Smoke",
      "PassedPawnCreated-8 reuses only the existing 8B LLM smoke boundary.",
      "PassedPawnCreated-8 LLM smoke input:",
      "- `renderedText`",
      "- `claimKey`",
      "- `strength`",
      "- forbidden wording",
      "- `Rephrase only. Do not add chess facts.`",
      "PassedPawnCreated-8 forbidden LLM smoke inputs:",
      "- raw `Story`",
      "- raw `PassedPawnCreatedProof`",
      "- `PassedPawnObservation`",
      "- `BoardFacts`",
      "- `EngineCheck`",
      "- raw PV",
      "- `proofFailures`",
      "PassedPawnCreated-8 forbidden LLM smoke additions:",
      "- new move",
      "- new line",
      "- promotion claim",
      "- unstoppable claim",
      "- winning claim",
      "- conversion claim",
      "- pressure claim",
      "Completion standard: PassedPawnCreated-8 closes when LLM smoke may receive only renderedText, claimKey, strength, forbidden wording, and `Rephrase only. Do not add chess facts.` for selected PassedPawnCreated RenderedLine; it rejects raw Story, raw PassedPawnCreatedProof, PassedPawnObservation, BoardFacts, EngineCheck, raw PV, proofFailures, new moves, new lines, and promotion, unstoppable, winning, conversion, or pressure claims."
    ).foreach: required =>
      assert(interactionLaw.contains(required), s"PassedPawnCreated-8 law must pin: $required")

    Vector(readme, ssot, architecture, normalizedModelContract).foreach: doc =>
      assert(
        doc.contains(PassedPawnCreatedSummary),
        "summary docs must keep only summary PassedPawnCreated scope"
      )
      assert(
        !doc.contains("### PassedPawnCreated-8 LLM Smoke"),
        "summary docs must not duplicate detailed PassedPawnCreated-8 law"
      )
      assert(
        !doc.contains("PassedPawnCreated-8 forbidden LLM smoke inputs:"),
        "summary docs must not duplicate PassedPawnCreated-8 forbidden input checklist"
      )
    assert(
      manifest.contains(PassedPawnCreatedSummary),
      "LegacyPruneManifest must keep PassedPawnCreated summary"
    )
    assert(
      !manifest.contains("### PassedPawnCreated-8 LLM Smoke"),
      "LegacyPruneManifest must not duplicate detailed PassedPawnCreated-8 law"
    )
    assert(
      !manifest.contains("PassedPawnCreated-8 forbidden LLM smoke inputs:"),
      "LegacyPruneManifest must not duplicate PassedPawnCreated-8 forbidden input checklist"
    )

  test("PassedPawnCreated Closeout hard cleanup keeps authority separated and public surfaces closed"):
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))
    val readme = Files.readString(docsRoot.resolve("README.md"))
    val ssot = Files.readString(docsRoot.resolve("ChessCommentarySSOT.md"))
    val architecture = Files.readString(docsRoot.resolve("ChessModelArchitecture.md"))
    val contract = Files.readString(docsRoot.resolve("ChessModelContract.md"))
    val manifest = Files.readString(docsRoot.resolve("LegacyPruneManifest.md"))
    val normalizedModelContract = contract.replaceAll("\\s+", " ")

    Vector(
      "### PassedPawnCreated Closeout Hard Cleanup",
      "`PassedPawnObservation`, `PassedPawnCreatedProof`, `Scene.PassedPawnCreated`, and `creates_passed_pawn` remain separate authority homes.",
      "`PassedPawnObservation` observes same-board passed-pawn structure only.",
      "`PassedPawnCreatedProof` proves only the before/after passed-pawn change for one legal move on the exact board.",
      "`Scene.PassedPawnCreated` owns only the public Story identity for the bounded newly-created passer event.",
      "`creates_passed_pawn` owns only bounded downstream speech for selected uncapped Lead Verdicts.",
      "`Scene.PassedPawnCreated` owns no PawnBreak, PawnCapture, PawnAdvance, PawnStop, PromotionThreat, or Promotion meaning.",
      "`Scene.PassedPawnCreated` owns no Material, Hanging, Defense, DiscoveredAttack, Pin, RemoveGuard, or Skewer meaning.",
      "`unstoppable`, `promotion threat`, `will promote`, `conversion`, `winning`, `breakthrough`, `initiative`, and `pressure` remain forbidden wording only, not live PassedPawnCreated authority.",
      "PassedPawnCreated duplicate checks:",
      "- one chess meaning: one legal move creates one newly passed pawn on the exact after-board.",
      "- one proof home: `PassedPawnCreatedProof`.",
      "- one Story label: `Scene.PassedPawnCreated`.",
      "- one speech key: `creates_passed_pawn`.",
      "- one detailed live authority document: `StoryInteractionLaw.md`.",
      "README, SSOT, Architecture, Contract, and Manifest may summarize PassedPawnCreated only.",
      "Renderer and LLM smoke wording must remain no stronger than `creates_passed_pawn`.",
      "Public route `200`, production API, and public/user-facing LLM narration remain closed.",
      "Completion standard: PassedPawnCreated Closeout closes when PassedPawnObservation, PassedPawnCreatedProof, Scene.PassedPawnCreated, and creates_passed_pawn remain separate authority homes"
    ).foreach: required =>
      assert(interactionLaw.contains(required), s"PassedPawnCreated Closeout law must pin: $required")

    Vector(readme, ssot, architecture, normalizedModelContract).foreach: doc =>
      assert(
        doc.contains(PassedPawnCreatedSummary),
        "summary docs must keep only summary PassedPawnCreated scope"
      )
      assert(
        !doc.contains("### PassedPawnCreated Closeout Hard Cleanup"),
        "summary docs must not duplicate detailed PassedPawnCreated closeout law"
      )
      assert(
        !doc.contains("PassedPawnCreated duplicate checks:"),
        "summary docs must not duplicate detailed PassedPawnCreated duplicate audit"
      )
      assert(
        !doc.contains(
          "PassedPawnObservation, PassedPawnCreatedProof, Scene.PassedPawnCreated, and creates_passed_pawn remain separate authority homes"
        ),
        "summary docs must not duplicate PassedPawnCreated authority-home detail"
      )
    assert(
      manifest.contains(PassedPawnCreatedSummary),
      "LegacyPruneManifest must keep PassedPawnCreated summary"
    )
    assert(
      !manifest.contains("### PassedPawnCreated Closeout Hard Cleanup"),
      "LegacyPruneManifest must not duplicate detailed PassedPawnCreated closeout law"
    )
    assert(
      !manifest.contains("PassedPawnCreated duplicate checks:"),
      "LegacyPruneManifest must not duplicate detailed PassedPawnCreated duplicate audit"
    )
    assert(
      !manifest.contains(
        "PassedPawnObservation, PassedPawnCreatedProof, Scene.PassedPawnCreated, and creates_passed_pawn remain separate authority homes"
      ),
      "LegacyPruneManifest must not duplicate PassedPawnCreated authority-home detail"
    )

  test("PSBNC-0 closes pawn structure break neighborhood without opening new meaning"):
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))
    val readme = Files.readString(docsRoot.resolve("README.md"))
    val ssot = Files.readString(docsRoot.resolve("ChessCommentarySSOT.md"))
    val architecture = Files.readString(docsRoot.resolve("ChessModelArchitecture.md"))
    val contract = Files.readString(docsRoot.resolve("ChessModelContract.md"))
    val manifest = Files.readString(docsRoot.resolve("LegacyPruneManifest.md"))
    val normalizedModelContract = contract.replaceAll("\\s+", " ")

    Vector(
      "### PSBNC-0 Pawn Structure / Break Neighborhood Closeout Charter",
      "Pawn Structure / Break Neighborhood closes as three narrow proof-backed event slices. PSBNC opens no new chess meaning.",
      "PSBNC-0 closes only already-open narrow meanings:",
      "- `Scene.PawnBreak`",
      "- `Scene.PawnCapture`",
      "- `Scene.PassedPawnCreated`",
      "`BoardFacts.PawnLever`, `PawnBreakProof`, `Scene.PawnBreak`, and `challenges_pawn` remain separate authority homes.",
      "`PawnCaptureProof`, `Scene.PawnCapture`, and `captures_rival_pawn` remain separate authority homes.",
      "`PassedPawnObservation`, `PassedPawnCreatedProof`, `Scene.PassedPawnCreated`, and `creates_passed_pawn` remain separate authority homes.",
      "The three Story labels must not steal each other's claims.",
      "A pawn-captures-rival-pawn event does not automatically own passed-pawn creation.",
      "A newly-created passer event does not own the pawn-capture event.",
      "Pawn contact/challenge stays in the PawnBreak home.",
      "Pawn-captures-rival-pawn event stays in the PawnCapture home.",
      "Newly-created passer event stays in the PassedPawnCreated home.",
      "Completion standard: PSBNC-0 closes when PawnBreakProof / Scene.PawnBreak / challenges_pawn, PawnCaptureProof / Scene.PawnCapture / captures_rival_pawn, and PassedPawnObservation / PassedPawnCreatedProof / Scene.PassedPawnCreated / creates_passed_pawn remain separate"
    ).foreach: required =>
      assert(interactionLaw.contains(required), s"PSBNC-0 law must pin: $required")

    Vector(readme, ssot, architecture, normalizedModelContract).foreach: doc =>
      assert(doc.contains(PawnStructureBreakCloseoutSummary), "summary docs must keep PSBNC-0 summary")
      assert(!doc.contains("### PSBNC-0 Pawn Structure / Break Neighborhood Closeout Charter"))
      assert(!doc.contains("PSBNC-0 closes only already-open narrow meanings:"))
      assert(!doc.contains("The three Story labels must not steal each other's claims."))
    assert(manifest.contains(PawnStructureBreakCloseoutSummary), "Manifest must keep PSBNC-0 summary")
    assert(!manifest.contains("### PSBNC-0 Pawn Structure / Break Neighborhood Closeout Charter"))
    assert(!manifest.contains("PSBNC-0 closes only already-open narrow meanings:"))
    assert(!manifest.contains("The three Story labels must not steal each other's claims."))

  test("PSBNC-1 scope audit keeps only three pawn structure event slices open"):
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))
    val readme = Files.readString(docsRoot.resolve("README.md"))
    val ssot = Files.readString(docsRoot.resolve("ChessCommentarySSOT.md"))
    val architecture = Files.readString(docsRoot.resolve("ChessModelArchitecture.md"))
    val contract = Files.readString(docsRoot.resolve("ChessModelContract.md"))
    val manifest = Files.readString(docsRoot.resolve("LegacyPruneManifest.md"))
    val normalizedModelContract = contract.replaceAll("\\s+", " ")

    Vector(
      "### PSBNC-1 Scope Audit",
      "Three pawn-structure event slices are open. Broad pawn-structure interpretation remains closed.",
      "PSBNC-1 open positive Stories are exactly:",
      "- `Scene.PawnBreak`",
      "- `Scene.PawnCapture`",
      "- `Scene.PassedPawnCreated`",
      "PSBNC-1 does not open:",
      "- broad PawnTactic",
      "- broad pawn structure advantage",
      "- passed pawn strategy",
      "- breakthrough",
      "- open file",
      "- weak square / weakness",
      "- pawn majority change",
      "- wins space",
      "- wins pawn",
      "- wins material",
      "- promotion threat",
      "- unstoppable pawn",
      "- conversion",
      "- initiative",
      "- pressure",
      "- best move / only move / forced",
      "- public route `200`",
      "- production API",
      "- public/user-facing LLM narration",
      "Completion standard: PSBNC-1 closes when the only open Pawn Structure / Break Neighborhood positive Stories are Scene.PawnBreak, Scene.PawnCapture, and Scene.PassedPawnCreated"
    ).foreach: required =>
      assert(interactionLaw.contains(required), s"PSBNC-1 law must pin: $required")

    Vector(readme, ssot, architecture, normalizedModelContract).foreach: doc =>
      assert(doc.contains(PawnStructureBreakCloseoutSummary), "summary docs must keep PSBNC summary")
      assert(!doc.contains("### PSBNC-1 Scope Audit"))
      assert(!doc.contains("PSBNC-1 open positive Stories are exactly:"))
      assert(!doc.contains("PSBNC-1 does not open:"))
    assert(manifest.contains(PawnStructureBreakCloseoutSummary), "Manifest must keep PSBNC summary")
    assert(!manifest.contains("### PSBNC-1 Scope Audit"))
    assert(!manifest.contains("PSBNC-1 open positive Stories are exactly:"))
    assert(!manifest.contains("PSBNC-1 does not open:"))

  test("PSBNC-2 duplication audit keeps one proof home Story label and speech key per meaning"):
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))
    val readme = Files.readString(docsRoot.resolve("README.md"))
    val ssot = Files.readString(docsRoot.resolve("ChessCommentarySSOT.md"))
    val architecture = Files.readString(docsRoot.resolve("ChessModelArchitecture.md"))
    val contract = Files.readString(docsRoot.resolve("ChessModelContract.md"))
    val manifest = Files.readString(docsRoot.resolve("LegacyPruneManifest.md"))
    val normalizedModelContract = contract.replaceAll("\\s+", " ")

    Vector(
      "### PSBNC-2 Duplication Audit",
      "PSBNC-2 duplication audit:",
      "- one chess meaning, one proof home",
      "- one Story label per public chess meaning",
      "- one speech key per allowed public claim",
      "- one detailed live authority document",
      "direct rival-pawn lever/contact:",
      "- proof home = `PawnBreakProof`",
      "- Story label = `Scene.PawnBreak`",
      "- speech key = `challenges_pawn`",
      "pawn captures rival pawn:",
      "- proof home = `PawnCaptureProof`",
      "- Story label = `Scene.PawnCapture`",
      "- speech key = `captures_rival_pawn`",
      "newly-created passed pawn:",
      "- observation = `PassedPawnObservation`",
      "- proof home = `PassedPawnCreatedProof`",
      "- Story label = `Scene.PassedPawnCreated`",
      "- speech key = `creates_passed_pawn`",
      "`PawnBreakProof` must not act as capture proof.",
      "`PawnCaptureProof` must not act as material or passed-pawn proof.",
      "`PassedPawnCreatedProof` must not act as capture or pawn-break proof.",
      "Speech keys must not duplicate.",
      "Completion standard: PSBNC-2 closes when direct rival-pawn lever/contact, pawn captures rival pawn, and newly-created passed pawn each have exactly one proof home, one Story label, and one speech key"
    ).foreach: required =>
      assert(interactionLaw.contains(required), s"PSBNC-2 law must pin: $required")

    Vector(readme, ssot, architecture, normalizedModelContract).foreach: doc =>
      assert(doc.contains(PawnStructureBreakCloseoutSummary), "summary docs must keep PSBNC summary")
      assert(!doc.contains("### PSBNC-2 Duplication Audit"))
      assert(!doc.contains("PSBNC-2 duplication audit:"))
      assert(!doc.contains("direct rival-pawn lever/contact:"))
    assert(manifest.contains(PawnStructureBreakCloseoutSummary), "Manifest must keep PSBNC summary")
    assert(!manifest.contains("### PSBNC-2 Duplication Audit"))
    assert(!manifest.contains("PSBNC-2 duplication audit:"))
    assert(!manifest.contains("direct rival-pawn lever/contact:"))

  test("PSBNC-3 StoryTable interaction audit keeps pawn structure rows in existing claim homes"):
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))
    val readme = Files.readString(docsRoot.resolve("README.md"))
    val ssot = Files.readString(docsRoot.resolve("ChessCommentarySSOT.md"))
    val architecture = Files.readString(docsRoot.resolve("ChessModelArchitecture.md"))
    val contract = Files.readString(docsRoot.resolve("ChessModelContract.md"))
    val manifest = Files.readString(docsRoot.resolve("LegacyPruneManifest.md"))
    val normalizedModelContract = contract.replaceAll("\\s+", " ")

    Vector(
      "### PSBNC-3 StoryTable Interaction Audit",
      "PSBNC-3 collides the three Pawn Structure / Break Neighborhood rows with existing open rows.",
      "PSBNC-3 target rows:",
      "- `Scene.PawnBreak`",
      "- `Scene.PawnCapture`",
      "- `Scene.PassedPawnCreated`",
      "- `Scene.PawnAdvance`",
      "- `Scene.PawnStop`",
      "- `Scene.PromotionThreat`",
      "- `Scene.Promotion`",
      "- `Scene.Material`",
      "- `Tactic.Hanging`",
      "- `Scene.Defense`",
      "- Line / Defender tactics",
      "StoryTable interaction audit rules:",
      "- input order must stay stable",
      "- each claim home must stay in its own home",
      "- `Scene.PawnCapture` must not own the `Scene.PassedPawnCreated` claim",
      "- `Scene.PassedPawnCreated` must not own the `Scene.PawnCapture` event",
      "- `Scene.PawnBreak` must not own capture or passed-pawn creation meaning",
      "- actual material change now remains `Scene.Material` home",
      "- promotion-next meaning remains `Scene.PromotionThreat` home",
      "- actual promotion meaning remains `Scene.Promotion` home",
      "- only the selected uncapped Lead may carry an allowed claim into `ExplanationPlan`",
      "- Support, Context, Blocked, capped, and refuted rows must have no standalone text",
      "Completion standard: PSBNC-3 closes when StoryTable order is stable across input order"
    ).foreach: required =>
      assert(interactionLaw.contains(required), s"PSBNC-3 law must pin: $required")

    Vector(readme, ssot, architecture, normalizedModelContract).foreach: doc =>
      assert(doc.contains(PawnStructureBreakCloseoutSummary), "summary docs must keep PSBNC summary")
      assert(!doc.contains("### PSBNC-3 StoryTable Interaction Audit"))
      assert(!doc.contains("PSBNC-3 target rows:"))
      assert(!doc.contains("StoryTable interaction audit rules:"))
    assert(manifest.contains(PawnStructureBreakCloseoutSummary), "Manifest must keep PSBNC summary")
    assert(!manifest.contains("### PSBNC-3 StoryTable Interaction Audit"))
    assert(!manifest.contains("PSBNC-3 target rows:"))
    assert(!manifest.contains("StoryTable interaction audit rules:"))

  test("PSBNC-4 downstream boundary audit keeps pawn structure speech bounded"):
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))
    val readme = Files.readString(docsRoot.resolve("README.md"))
    val ssot = Files.readString(docsRoot.resolve("ChessCommentarySSOT.md"))
    val architecture = Files.readString(docsRoot.resolve("ChessModelArchitecture.md"))
    val contract = Files.readString(docsRoot.resolve("ChessModelContract.md"))
    val manifest = Files.readString(docsRoot.resolve("LegacyPruneManifest.md"))
    val normalizedModelContract = contract.replaceAll("\\s+", " ")

    Vector(
      "### PSBNC-4 Downstream Boundary Audit",
      "PSBNC-4 audits only the downstream boundaries for the three Pawn Structure / Break Neighborhood slices.",
      "PSBNC-4 downstream targets:",
      "- `ExplanationPlan`",
      "- `DeterministicRenderer`",
      "- LLM smoke",
      "PSBNC-4 boundary rules:",
      "- `ExplanationPlan` input is only the selected uncapped Lead `Verdict`",
      "- `DeterministicRenderer` input is `ExplanationPlan` only",
      "- LLM smoke input reuses only the existing 8B boundary",
      "PSBNC-4 allowed claim keys:",
      "- `challenges_pawn`",
      "- `captures_rival_pawn`",
      "- `creates_passed_pawn`",
      "PSBNC-4 forbidden claim keys:",
      "- `opens_file`",
      "- `weakens_structure`",
      "- `breaks_through`",
      "- `wins_space`",
      "- `wins_pawn`",
      "- `wins_material`",
      "- `promotion_threat`",
      "- `unstoppable_pawn`",
      "- `converts_advantage`",
      "- `creates_pressure`",
      "- `takes_initiative`",
      "- `best_move`",
      "- `only_move`",
      "- `forced`",
      "Renderer and LLM smoke wording must remain no stronger than the selected claim.",
      "Completion standard: PSBNC-4 closes when ExplanationPlan receives only selected uncapped Lead Verdicts"
    ).foreach: required =>
      assert(interactionLaw.contains(required), s"PSBNC-4 law must pin: $required")

    Vector(readme, ssot, architecture, normalizedModelContract).foreach: doc =>
      assert(doc.contains(PawnStructureBreakCloseoutSummary), "summary docs must keep PSBNC summary")
      assert(!doc.contains("### PSBNC-4 Downstream Boundary Audit"))
      assert(!doc.contains("PSBNC-4 downstream targets:"))
      assert(!doc.contains("PSBNC-4 boundary rules:"))
      assert(!doc.contains("PSBNC-4 allowed claim keys:"))
      assert(!doc.contains("PSBNC-4 forbidden claim keys:"))
    assert(manifest.contains(PawnStructureBreakCloseoutSummary), "Manifest must keep PSBNC summary")
    assert(!manifest.contains("### PSBNC-4 Downstream Boundary Audit"))
    assert(!manifest.contains("PSBNC-4 downstream targets:"))
    assert(!manifest.contains("PSBNC-4 boundary rules:"))
    assert(!manifest.contains("PSBNC-4 allowed claim keys:"))
    assert(!manifest.contains("PSBNC-4 forbidden claim keys:"))

  test("PSBNC-5 forbidden wording audit keeps closed pawn structure wording non-authoritative"):
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))
    val readme = Files.readString(docsRoot.resolve("README.md"))
    val ssot = Files.readString(docsRoot.resolve("ChessCommentarySSOT.md"))
    val architecture = Files.readString(docsRoot.resolve("ChessModelArchitecture.md"))
    val contract = Files.readString(docsRoot.resolve("ChessModelContract.md"))
    val manifest = Files.readString(docsRoot.resolve("LegacyPruneManifest.md"))
    val normalizedModelContract = contract.replaceAll("\\s+", " ")

    Vector(
      "### PSBNC-5 Forbidden Wording Audit",
      "PSBNC-5 audits forbidden wording for the three Pawn Structure / Break Neighborhood slices.",
      "PSBNC-5 forbidden live-authority wording:",
      "- opens file",
      "- opens position",
      "- weakens structure",
      "- breakthrough",
      "- breaks through",
      "- wins space",
      "- wins pawn",
      "- wins material",
      "- creates passed pawn, unless selected `Scene.PassedPawnCreated`",
      "- promotion threat, unless selected `Scene.PromotionThreat`",
      "- unstoppable",
      "- conversion",
      "- initiative",
      "- pressure",
      "- best move",
      "- only move",
      "- forced",
      "Forbidden wording may exist only as forbidden wording, negative corpus, or docs saying it remains closed.",
      "It must not become proof home, Story label, claim key, renderer output, LLM-added fact, public value, or public route payload.",
      "Completion standard: PSBNC-5 closes when forbidden wording remains non-authoritative"
    ).foreach: required =>
      assert(interactionLaw.contains(required), s"PSBNC-5 law must pin: $required")

    Vector(readme, ssot, architecture, normalizedModelContract).foreach: doc =>
      assert(doc.contains(PawnStructureBreakCloseoutSummary), "summary docs must keep PSBNC summary")
      assert(!doc.contains("### PSBNC-5 Forbidden Wording Audit"))
      assert(!doc.contains("PSBNC-5 forbidden live-authority wording:"))
      assert(!doc.contains("Forbidden wording may exist only as forbidden wording"))
    assert(manifest.contains(PawnStructureBreakCloseoutSummary), "Manifest must keep PSBNC summary")
    assert(!manifest.contains("### PSBNC-5 Forbidden Wording Audit"))
    assert(!manifest.contains("PSBNC-5 forbidden live-authority wording:"))
    assert(!manifest.contains("Forbidden wording may exist only as forbidden wording"))

  test("PSBNC-6 documentation simplification audit keeps detailed closeout law in StoryInteractionLaw only"):
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))
    val readme = Files.readString(docsRoot.resolve("README.md"))
    val ssot = Files.readString(docsRoot.resolve("ChessCommentarySSOT.md"))
    val architecture = Files.readString(docsRoot.resolve("ChessModelArchitecture.md"))
    val contract = Files.readString(docsRoot.resolve("ChessModelContract.md"))
    val manifest = Files.readString(docsRoot.resolve("LegacyPruneManifest.md"))
    val normalizedModelContract = contract.replaceAll("\\s+", " ")

    Vector(
      "### PSBNC-6 Documentation Simplification Audit",
      "PSBNC-6 documentation simplification audit:",
      "- detailed Pawn Structure / Break Neighborhood closeout authority lives only in `StoryInteractionLaw.md`.",
      "- `README.md`, `ChessCommentarySSOT.md`, `ChessModelArchitecture.md`, `ChessModelContract.md`, and `LegacyPruneManifest.md` remain summary-only.",
      "- `AGENTS.md` keeps durable operator guardrails only.",
      "- retired root docs must not return.",
      "- legacy reset archive documents are historical reference only and not acceptance sources.",
      "PSBNC-6 summary-only documents:",
      "- `README.md`: non-authoritative pointer only.",
      "- `ChessCommentarySSOT.md`: non-authoritative pointer only.",
      "- `ChessModelArchitecture.md`: non-authoritative pointer only.",
      "- `ChessModelContract.md`: non-authoritative pointer only.",
      "- `LegacyPruneManifest.md`: pruning summary only, not detailed law.",
      "PSBNC-6 forbidden documentation duplication:",
      "- no PSBNC detailed checklist in summary docs.",
      "- no closeout stage law in `AGENTS.md`.",
      "- no detailed PSBNC law in `LegacyPruneManifest.md`.",
      "- no retired root docs as live authority.",
      "- no legacy reset archive acceptance source.",
      "Completion standard: PSBNC-6 closes when detailed PSBNC closeout authority appears only in `StoryInteractionLaw.md`"
    ).foreach: required =>
      assert(interactionLaw.contains(required), s"PSBNC-6 law must pin: $required")

    val summaryDocs = Vector(readme, ssot, architecture, normalizedModelContract)
    summaryDocs.foreach: doc =>
      assert(doc.contains(PawnStructureBreakCloseoutSummary), "summary docs must keep PSBNC summary")
      PawnStructureBreakCloseoutDetailedMarkers.foreach: marker =>
        assert(!doc.contains(marker), s"summary docs must not repeat detailed PSBNC law: $marker")

    assert(manifest.contains(PawnStructureBreakCloseoutSummary), "Manifest must keep PSBNC summary")
    PawnStructureBreakCloseoutDetailedMarkers.foreach: marker =>
      assert(!manifest.contains(marker), s"LegacyPruneManifest must not repeat detailed PSBNC law: $marker")

    assert(agents.contains("AGENTS.md is a concise operator guide."))
    assert(agents.contains("Detailed slice authority lives in `StoryInteractionLaw.md`"))
    assert(agents.contains("Documents under"))
    assert(agents.contains("legacy-pre-semantic-reset/"))
    assert(agents.contains("historical reference only"))
    PawnStructureBreakCloseoutDetailedMarkers.foreach: marker =>
      assert(!agents.contains(marker), s"AGENTS.md must not repeat detailed PSBNC law: $marker")

    val liveDocTextByName = LiveDocs.map(name => name -> Files.readString(docsRoot.resolve(name))).toMap
    PawnStructureBreakCloseoutDetailedMarkers.foreach: marker =>
      val owners = liveDocTextByName.collect:
        case (docName, contents) if contents.contains(marker) => docName
      assertEquals(owners.toSet, Set("StoryInteractionLaw.md"), s"detailed PSBNC marker must have one owner: $marker")

    RetiredRootDocs.foreach: fileName =>
      assert(!Files.exists(docsRoot.resolve(fileName)), s"retired root doc must not return: $fileName")

  test("PSBNC-7 runtime boundary audit keeps test helpers below runtime authority"):
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))
    val readme = Files.readString(docsRoot.resolve("README.md"))
    val ssot = Files.readString(docsRoot.resolve("ChessCommentarySSOT.md"))
    val architecture = Files.readString(docsRoot.resolve("ChessModelArchitecture.md"))
    val contract = Files.readString(docsRoot.resolve("ChessModelContract.md"))
    val manifest = Files.readString(docsRoot.resolve("LegacyPruneManifest.md"))
    val normalizedModelContract = contract.replaceAll("\\s+", " ")

    Vector(
      "### PSBNC-7 Runtime Boundary Audit",
      "PSBNC-7 runtime boundary audit:",
      "- test helpers are not runtime authority.",
      "- runtime helpers must not become new authority.",
      "- fixture names are not Story family names.",
      "- negative corpus helpers are not production concepts.",
      "- forbidden wording checks must target public overclaim wording, not arbitrary internal field names.",
      "- runtime source must not contain closeout-only terminology.",
      "PSBNC-7 forbids new runtime authority:",
      "- no new Story family.",
      "- no new proof home.",
      "- no new Story writer.",
      "- no new claim key.",
      "- no new renderer template.",
      "- no new LLM behavior.",
      "- no public route `200`.",
      "- no production API.",
      "- no public/user-facing LLM narration.",
      "PSBNC-7 allowed closeout changes:",
      "- tests may audit the boundary.",
      "- documentation law may record the audit in `StoryInteractionLaw.md`.",
      "- runtime code changes are allowed only if an audit exposes leakage or duplicated authority.",
      "PSBNC-7 runtime source guard:",
      "- runtime source must not contain `PSBNC-`.",
      "- runtime source must not contain `closeout`.",
      "- runtime source must not contain `Runtime Boundary Audit`.",
      "- runtime source must not contain `Pawn Structure / Break Neighborhood Closeout`.",
      "- runtime source must not contain `fixture map`.",
      "- runtime source must not contain `negative corpus`.",
      "Completion standard: PSBNC-7 closes when helper names remain test-local"
    ).foreach: required =>
      assert(interactionLaw.contains(required), s"PSBNC-7 law must pin: $required")

    val detailedMarkers =
      Vector(
        "### PSBNC-7 Runtime Boundary Audit",
        "PSBNC-7 runtime boundary audit:",
        "PSBNC-7 forbids new runtime authority:",
        "PSBNC-7 allowed closeout changes:",
        "PSBNC-7 runtime source guard:"
      )
    Vector(readme, ssot, architecture, normalizedModelContract).foreach: doc =>
      assert(doc.contains(PawnStructureBreakCloseoutSummary), "summary docs must keep PSBNC summary")
      detailedMarkers.foreach: marker =>
        assert(!doc.contains(marker), s"summary docs must not repeat detailed PSBNC-7 law: $marker")
    assert(manifest.contains(PawnStructureBreakCloseoutSummary), "Manifest must keep PSBNC summary")
    detailedMarkers.foreach: marker =>
      assert(!manifest.contains(marker), s"LegacyPruneManifest must not repeat detailed PSBNC-7 law: $marker")
      assert(!agents.contains(marker), s"AGENTS.md must not repeat detailed PSBNC-7 law: $marker")

    val runtimeText = runtimeChessSourceText
    Vector(
      "PSBNC-",
      "closeout",
      "Runtime Boundary Audit",
      "Pawn Structure / Break Neighborhood Closeout",
      "fixture map",
      "negative corpus"
    ).foreach: closeoutOnlyTerm =>
      assert(
        !runtimeText.contains(closeoutOnlyTerm),
        s"runtime source must not contain closeout-only term: $closeoutOnlyTerm"
      )

  test("PSBNC final completion standard closes Pawn Structure Break Neighborhood"):
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))
    val readme = Files.readString(docsRoot.resolve("README.md"))
    val ssot = Files.readString(docsRoot.resolve("ChessCommentarySSOT.md"))
    val architecture = Files.readString(docsRoot.resolve("ChessModelArchitecture.md"))
    val contract = Files.readString(docsRoot.resolve("ChessModelContract.md"))
    val manifest = Files.readString(docsRoot.resolve("LegacyPruneManifest.md"))
    val normalizedModelContract = contract.replaceAll("\\s+", " ")

    assert(interactionLaw.contains("### PSBNC Final Completion Standard"))
    Vector(
      "PSBNC final completion standard:",
      "- `Scene.PawnBreak`, `Scene.PawnCapture`, and `Scene.PassedPawnCreated` remain the only open Pawn Structure / Break Neighborhood positive slices.",
      "- each slice has exactly one proof path, one Story label, and one speech key.",
      "- no slice owns another slice's chess meaning.",
      "- Material, PromotionThreat, Promotion, PawnAdvance, PawnStop, Hanging, Defense, and Line / Defender tactic homes keep their meanings.",
      "- forbidden broad pawn-structure wording remains forbidden only.",
      "- StoryTable ordering is input-order stable across neighborhood collisions.",
      "- non-selected, capped, refuted, Support, Context, and Blocked rows produce no standalone downstream text.",
      "- ExplanationPlan, Renderer, and LLM smoke remain bounded to selected Lead Verdict data.",
      "- detailed authority lives only in `StoryInteractionLaw.md`.",
      "- summary docs remain summaries only.",
      "- public route `200`, production API, and public/user-facing LLM narration remain closed.",
      "PSBNC final verification:",
      "- targeted commentary runtime tests pass.",
      "- docs authority tests pass.",
      "- `git diff --check` passes.",
      "Final PSBNC closeout opens no new Story family, proof home, Story writer, claim key, renderer template, LLM behavior, fixture-derived runtime authority, negative-corpus production concept, broad PawnTactic, broad pawn-structure advantage, passed pawn strategy, breakthrough, open file, weak square, wins space, wins pawn, wins material, promotion threat, unstoppable pawn, conversion, initiative, pressure, best move, only move, forced, public route `200`, production API, or public/user-facing LLM narration."
    ).foreach: finalLine =>
      assert(interactionLaw.contains(finalLine), s"PSBNC final completion must pin: $finalLine")

    Vector(readme, ssot, architecture, normalizedModelContract).foreach: doc =>
      assert(doc.contains(PawnStructureBreakCloseoutSummary), "summary docs must keep PSBNC summary")
      assert(
        !doc.contains("PSBNC final completion standard:"),
        "summary docs must not duplicate detailed PSBNC final standard"
      )
      assert(
        !doc.contains("PSBNC final verification:"),
        "summary docs must not duplicate detailed PSBNC final verification"
      )
    assert(manifest.contains(PawnStructureBreakCloseoutSummary), "Manifest must keep PSBNC summary")
    assert(
      !manifest.contains("PSBNC final completion standard:"),
      "LegacyPruneManifest must not duplicate detailed PSBNC final standard"
    )
    assert(
      !manifest.contains("PSBNC final verification:"),
      "LegacyPruneManifest must not duplicate detailed PSBNC final verification"
    )

  test("PawnCapture-1 pins PawnCaptureProof as ordinary diagonal pawn capture proof home"):
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))
    val readme = Files.readString(docsRoot.resolve("README.md"))
    val ssot = Files.readString(docsRoot.resolve("ChessCommentarySSOT.md"))
    val architecture = Files.readString(docsRoot.resolve("ChessModelArchitecture.md"))
    val contract = Files.readString(docsRoot.resolve("ChessModelContract.md"))
    val manifest = Files.readString(docsRoot.resolve("LegacyPruneManifest.md"))
    val normalizedModelContract = contract.replaceAll("\\s+", " ")

    Vector(
      "### PawnCapture-1 PawnCaptureProof",
      "PawnCapture-1 narrows `PawnCaptureProof` as the proof home for the current `Scene.PawnCapture` slice.",
      "PawnCaptureProof proves:",
      "- capturing side",
      "- rival side",
      "- capturing pawn identity",
      "- origin square",
      "- capture square",
      "- captured rival pawn identity",
      "- legal pawn capture",
      "- exact after-board replay",
      "- same-board proof",
      "PawnCapture-1 first scope:",
      "- ordinary diagonal pawn capture only",
      "PawnCapture-1 closed scope:",
      "- en passant",
      "- promotion capture",
      "- capture that claims material gain",
      "- capture that claims passed pawn creation",
      "- capture that claims file opening",
      "- capture that claims structural weakness",
      "`PawnCaptureProof` is not a public Story.",
      "`CaptureResult` and `Scene.Material` remain the material meaning homes.",
      "`PawnCaptureProof` does not replace `CaptureResult` or `Scene.Material`.",
      "Completion standard: PawnCapture-1 closes when `PawnCaptureProof` explicitly proves ordinary diagonal legal pawn captures"
    ).foreach: required =>
      assert(interactionLaw.contains(required), s"PawnCapture-1 law must pin: $required")

    Vector(readme, ssot, architecture, normalizedModelContract).foreach: doc =>
      assert(doc.contains(PawnCaptureSummary), "summary docs must keep the PawnCapture summary pointer")
      assert(
        !doc.contains("### PawnCapture-1 PawnCaptureProof"),
        "summary docs must not duplicate detailed PawnCapture-1 law"
      )
      assert(
        !doc.contains("PawnCaptureProof proves:"),
        "summary docs must not duplicate PawnCapture-1 proof checklist"
      )
      assert(
        !doc.contains("PawnCapture-1 closed scope:"),
        "summary docs must not duplicate PawnCapture-1 closed scope"
      )
    assert(
      manifest.contains(PawnCaptureSummary),
      "LegacyPruneManifest must keep the PawnCapture summary pointer"
    )
    assert(
      !manifest.contains("### PawnCapture-1 PawnCaptureProof"),
      "LegacyPruneManifest must not duplicate detailed PawnCapture-1 law"
    )
    assert(
      !manifest.contains("PawnCaptureProof proves:"),
      "LegacyPruneManifest must not duplicate PawnCapture-1 proof checklist"
    )

  test("PawnCapture-2 pins ScenePawnCapture as the only PawnCapture writer"):
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))
    val readme = Files.readString(docsRoot.resolve("README.md"))
    val ssot = Files.readString(docsRoot.resolve("ChessCommentarySSOT.md"))
    val architecture = Files.readString(docsRoot.resolve("ChessModelArchitecture.md"))
    val contract = Files.readString(docsRoot.resolve("ChessModelContract.md"))
    val manifest = Files.readString(docsRoot.resolve("LegacyPruneManifest.md"))
    val normalizedModelContract = contract.replaceAll("\\s+", " ")

    Vector(
      "### PawnCapture-2 Scene.PawnCapture Writer",
      "`ScenePawnCapture` is the named writer for narrow `Scene.PawnCapture`.",
      "PawnCapture-2 writer conditions:",
      "- complete `StoryProof`",
      "- complete `PawnCaptureProof`",
      "- same-board legal replay",
      "- legal ordinary pawn capture",
      "- captured piece is rival pawn",
      "- writer = `ScenePawnCapture`",
      "- `EngineCheck` does not `Refute`",
      "PawnCapture-2 Story identity:",
      "- `scene = PawnCapture`",
      "- `tactic = None`",
      "- `plan = None`",
      "- `side = capturing side`",
      "- `rival = rival side`",
      "- `target = capture square`",
      "- `anchor = pawn origin square`",
      "- `route = pawn capture`",
      "PawnCapture-2 forbidden writer output:",
      "- `Scene.Material` claim",
      "- `PassedPawnCreated` claim",
      "- open-file meaning",
      "- weakness meaning",
      "- strategy meaning",
      "Completion standard: PawnCapture-2 closes when `ScenePawnCapture` alone writes narrow `Scene.PawnCapture`"
    ).foreach: required =>
      assert(interactionLaw.contains(required), s"PawnCapture-2 law must pin: $required")

    Vector(readme, ssot, architecture, normalizedModelContract).foreach: doc =>
      assert(doc.contains(PawnCaptureSummary), "summary docs must keep the PawnCapture summary pointer")
      assert(
        !doc.contains("### PawnCapture-2 Scene.PawnCapture Writer"),
        "summary docs must not duplicate detailed PawnCapture-2 law"
      )
      assert(
        !doc.contains("PawnCapture-2 writer conditions:"),
        "summary docs must not duplicate PawnCapture-2 writer checklist"
      )
      assert(
        !doc.contains("PawnCapture-2 Story identity:"),
        "summary docs must not duplicate PawnCapture-2 identity checklist"
      )
    assert(
      manifest.contains(PawnCaptureSummary),
      "LegacyPruneManifest must keep the PawnCapture summary pointer"
    )
    assert(
      !manifest.contains("### PawnCapture-2 Scene.PawnCapture Writer"),
      "LegacyPruneManifest must not duplicate detailed PawnCapture-2 law"
    )
    assert(
      !manifest.contains("PawnCapture-2 writer conditions:"),
      "LegacyPruneManifest must not duplicate PawnCapture-2 writer checklist"
    )

  test("PawnCapture-3 pins negative corpus to complete proof or silence"):
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))
    val readme = Files.readString(docsRoot.resolve("README.md"))
    val ssot = Files.readString(docsRoot.resolve("ChessCommentarySSOT.md"))
    val architecture = Files.readString(docsRoot.resolve("ChessModelArchitecture.md"))
    val contract = Files.readString(docsRoot.resolve("ChessModelContract.md"))
    val manifest = Files.readString(docsRoot.resolve("LegacyPruneManifest.md"))
    val normalizedModelContract = contract.replaceAll("\\s+", " ")

    Vector(
      "### PawnCapture-3 Negative Corpus",
      "A pawn capture is not a material or structure claim. Complete PawnCaptureProof or silence.",
      "PawnCapture-3 must close:",
      "- legal move 아님",
      "- same-board proof 없음",
      "- moving piece가 pawn이 아님",
      "- capture가 아님",
      "- captured piece가 pawn이 아님",
      "- captured piece가 rival side가 아님",
      "- en passant",
      "- promotion capture",
      "- material gain을 PawnCapture claim으로 말하려 함",
      "- passed pawn created / open file / weakness wording 유입",
      "Completion standard: PawnCapture-3 closes when every negative corpus row has incomplete `PawnCaptureProof` or blocked `Scene.PawnCapture` output"
    ).foreach: required =>
      assert(interactionLaw.contains(required), s"PawnCapture-3 law must pin: $required")

    Vector(readme, ssot, architecture, normalizedModelContract).foreach: doc =>
      assert(doc.contains(PawnCaptureSummary), "summary docs must keep the PawnCapture summary pointer")
      assert(
        !doc.contains("### PawnCapture-3 Negative Corpus"),
        "summary docs must not duplicate detailed PawnCapture-3 law"
      )
      assert(
        !doc.contains("PawnCapture-3 must close:"),
        "summary docs must not duplicate PawnCapture-3 corpus checklist"
      )
      assert(
        !doc.contains("Complete PawnCaptureProof or silence."),
        "summary docs must not duplicate PawnCapture-3 standard"
      )
    assert(
      manifest.contains(PawnCaptureSummary),
      "LegacyPruneManifest must keep the PawnCapture summary pointer"
    )
    assert(
      !manifest.contains("### PawnCapture-3 Negative Corpus"),
      "LegacyPruneManifest must not duplicate detailed PawnCapture-3 law"
    )
    assert(
      !manifest.contains("PawnCapture-3 must close:"),
      "LegacyPruneManifest must not duplicate PawnCapture-3 corpus checklist"
    )

  test("PawnCapture-4 pins EngineCheck reuse without engine-owned PawnCapture claims"):
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))
    val readme = Files.readString(docsRoot.resolve("README.md"))
    val ssot = Files.readString(docsRoot.resolve("ChessCommentarySSOT.md"))
    val architecture = Files.readString(docsRoot.resolve("ChessModelArchitecture.md"))
    val contract = Files.readString(docsRoot.resolve("ChessModelContract.md"))
    val manifest = Files.readString(docsRoot.resolve("LegacyPruneManifest.md"))
    val normalizedModelContract = contract.replaceAll("\\s+", " ")

    Vector(
      "### PawnCapture-4 EngineCheck Reuse",
      "PawnCapture-4 reuses the existing `EngineCheck` sidecar only.",
      "PawnCapture-4 rules:",
      "- `EngineCheck` cannot create `Scene.PawnCapture`.",
      "- `Supports` creates no new claim.",
      "- `Caps` suppresses the standalone `captures_rival_pawn` claim or weakens it to bounded relation only.",
      "- `Refutes` blocks the existing `Scene.PawnCapture` Story.",
      "- `Unknown` creates no engine expression.",
      "PawnCapture-4 forbids:",
      "- engine says",
      "- eval number",
      "- best move",
      "- only move",
      "- wins pawn",
      "- structural advantage",
      "Completion standard: PawnCapture-4 closes when `EngineCheck` can attach only to an existing same-board `ScenePawnCapture` Story route, cannot create PawnCapture, cannot add a claim under Supports, suppresses or bounds capped rows, blocks refuted rows, keeps Unknown engine-silent, and exposes no engine-says, eval-number, best-move, only-move, wins-pawn, or structural-advantage wording."
    ).foreach: required =>
      assert(interactionLaw.contains(required), s"PawnCapture-4 law must pin: $required")

    Vector(readme, ssot, architecture, normalizedModelContract).foreach: doc =>
      assert(doc.contains(PawnCaptureSummary), "summary docs must keep the PawnCapture summary pointer")
      assert(
        !doc.contains("### PawnCapture-4 EngineCheck Reuse"),
        "summary docs must not duplicate detailed PawnCapture-4 law"
      )
      assert(
        !doc.contains("PawnCapture-4 rules:"),
        "summary docs must not duplicate PawnCapture-4 rule checklist"
      )
      assert(
        !doc.contains("PawnCapture-4 forbids:"),
        "summary docs must not duplicate PawnCapture-4 forbidden checklist"
      )
    assert(
      manifest.contains(PawnCaptureSummary),
      "LegacyPruneManifest must keep the PawnCapture summary pointer"
    )
    assert(
      !manifest.contains("### PawnCapture-4 EngineCheck Reuse"),
      "LegacyPruneManifest must not duplicate detailed PawnCapture-4 law"
    )
    assert(
      !manifest.contains("PawnCapture-4 rules:"),
      "LegacyPruneManifest must not duplicate PawnCapture-4 rule checklist"
    )

  test("PawnCapture-5 pins StoryTable integration against existing claim homes"):
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))
    val readme = Files.readString(docsRoot.resolve("README.md"))
    val ssot = Files.readString(docsRoot.resolve("ChessCommentarySSOT.md"))
    val architecture = Files.readString(docsRoot.resolve("ChessModelArchitecture.md"))
    val contract = Files.readString(docsRoot.resolve("ChessModelContract.md"))
    val manifest = Files.readString(docsRoot.resolve("LegacyPruneManifest.md"))
    val normalizedModelContract = contract.replaceAll("\\s+", " ")

    Vector(
      "### PawnCapture-5 StoryTable Integration",
      "PawnCapture-5 integrates `Scene.PawnCapture` with existing StoryTable rows without opening material or structure meaning.",
      "PawnCapture-5 collision targets:",
      "- `Scene.PawnBreak`",
      "- `Scene.PawnAdvance`",
      "- `Scene.PawnStop`",
      "- `Scene.PromotionThreat`",
      "- `Scene.Promotion`",
      "- `Scene.Material`",
      "- `Tactic.Hanging`",
      "- `Scene.Defense`",
      "- line tactics",
      "- `Scene.PawnCapture`",
      "PawnCapture-5 verification:",
      "- input order must be stable",
      "- `Scene.PawnCapture` must not own Material claim",
      "- `Scene.PawnCapture` must not create PassedPawnCreated claim",
      "- `Scene.PawnCapture` must not create FileOpens or weakness meaning",
      "- actual material change now stays in `Scene.Material`",
      "- pawn contact/challenge stays in `Scene.PawnBreak`",
      "- capped/refuted `Scene.PawnCapture` has no standalone text",
      "Completion standard: PawnCapture-5 closes when StoryTable ordering is input-order stable across PawnCapture collisions, existing claim homes keep lead ownership over Material, PawnBreak, PawnAdvance, PawnStop, PromotionThreat, Promotion, Hanging, Defense, and line tactics, same-family PawnCapture rows order deterministically, PawnCapture does not own material, passed-pawn, file-opening, or weakness meaning, and capped/refuted PawnCapture rows produce no standalone text."
    ).foreach: required =>
      assert(interactionLaw.contains(required), s"PawnCapture-5 law must pin: $required")

    Vector(readme, ssot, architecture, normalizedModelContract).foreach: doc =>
      assert(doc.contains(PawnCaptureSummary), "summary docs must keep the PawnCapture summary pointer")
      assert(
        !doc.contains("### PawnCapture-5 StoryTable Integration"),
        "summary docs must not duplicate detailed PawnCapture-5 law"
      )
      assert(
        !doc.contains("PawnCapture-5 collision targets:"),
        "summary docs must not duplicate PawnCapture-5 target checklist"
      )
      assert(
        !doc.contains("PawnCapture-5 verification:"),
        "summary docs must not duplicate PawnCapture-5 verification checklist"
      )
    assert(
      manifest.contains(PawnCaptureSummary),
      "LegacyPruneManifest must keep the PawnCapture summary pointer"
    )
    assert(
      !manifest.contains("### PawnCapture-5 StoryTable Integration"),
      "LegacyPruneManifest must not duplicate detailed PawnCapture-5 law"
    )
    assert(
      !manifest.contains("PawnCapture-5 collision targets:"),
      "LegacyPruneManifest must not duplicate PawnCapture-5 target checklist"
    )

  test("PawnCapture-6 pins ExplanationPlan to selected uncapped Lead only"):
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))
    val readme = Files.readString(docsRoot.resolve("README.md"))
    val ssot = Files.readString(docsRoot.resolve("ChessCommentarySSOT.md"))
    val architecture = Files.readString(docsRoot.resolve("ChessModelArchitecture.md"))
    val contract = Files.readString(docsRoot.resolve("ChessModelContract.md"))
    val manifest = Files.readString(docsRoot.resolve("LegacyPruneManifest.md"))
    val normalizedModelContract = contract.replaceAll("\\s+", " ")

    Vector(
      "### PawnCapture-6 ExplanationPlan",
      "PawnCapture-6 lowers only selected uncapped `Lead` Verdicts for `Scene.PawnCapture`.",
      "PawnCapture-6 allowed claim key:",
      "- `captures_rival_pawn`",
      "PawnCapture-6 forbidden claim keys:",
      "- `wins_pawn`",
      "- `wins_material`",
      "- `creates_passed_pawn`",
      "- `opens_file`",
      "- `weakens_structure`",
      "- `breaks_through`",
      "- `creates_pressure`",
      "- `takes_initiative`",
      "- `best_move`",
      "- `only_move`",
      "- `forced`",
      "Support, Context, Blocked, capped, and refuted `Scene.PawnCapture` rows have no standalone claim.",
      "Completion standard: PawnCapture-6 closes when only selected uncapped Lead PawnCapture Verdicts lower to ExplanationPlan with `captures_rival_pawn`; forbidden keys remain closed; Support, Context, Blocked, capped, and refuted PawnCapture rows produce no standalone ExplanationPlan claim; and renderer, LLM, production API, public route, material gain, passed-pawn, file-opening, weakness, breakthrough, pressure, initiative, best-move, only-move, and forced speech remain closed."
    ).foreach: required =>
      assert(interactionLaw.contains(required), s"PawnCapture-6 law must pin: $required")

    Vector(readme, ssot, architecture, normalizedModelContract).foreach: doc =>
      assert(doc.contains(PawnCaptureSummary), "summary docs must keep the PawnCapture summary pointer")
      assert(
        !doc.contains("### PawnCapture-6 ExplanationPlan"),
        "summary docs must not duplicate detailed PawnCapture-6 law"
      )
      assert(
        !doc.contains("PawnCapture-6 allowed claim key:"),
        "summary docs must not duplicate PawnCapture-6 allowed key"
      )
      assert(
        !doc.contains("PawnCapture-6 forbidden claim keys:"),
        "summary docs must not duplicate PawnCapture-6 forbidden checklist"
      )
    assert(
      manifest.contains(PawnCaptureSummary),
      "LegacyPruneManifest must keep the PawnCapture summary pointer"
    )
    assert(
      !manifest.contains("### PawnCapture-6 ExplanationPlan"),
      "LegacyPruneManifest must not duplicate detailed PawnCapture-6 law"
    )
    assert(
      !manifest.contains("PawnCapture-6 forbidden claim keys:"),
      "LegacyPruneManifest must not duplicate PawnCapture-6 forbidden checklist"
    )

  test("PawnCapture-7 pins DeterministicRenderer bounded pawn-capture wording only"):
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))
    val readme = Files.readString(docsRoot.resolve("README.md"))
    val ssot = Files.readString(docsRoot.resolve("ChessCommentarySSOT.md"))
    val architecture = Files.readString(docsRoot.resolve("ChessModelArchitecture.md"))
    val contract = Files.readString(docsRoot.resolve("ChessModelContract.md"))
    val manifest = Files.readString(docsRoot.resolve("LegacyPruneManifest.md"))
    val normalizedModelContract = contract.replaceAll("\\s+", " ")

    Vector(
      "### PawnCapture-7 Deterministic Renderer",
      "Renderer input is `ExplanationPlan` only.",
      "PawnCapture-7 allowed deterministic wording:",
      "- `{route} captures the pawn on {target}.`",
      "PawnCapture-7 forbidden renderer wording:",
      "- `wins a pawn`",
      "- `wins material`",
      "- `creates a passed pawn`",
      "- `opens the file`",
      "- `weakens the structure`",
      "- `breaks through`",
      "- `takes the initiative`",
      "- `best move`",
      "- `only move`",
      "- `forces`",
      "Completion standard: PawnCapture-7 closes when `DeterministicRenderer` accepts only `ExplanationPlan`, phrases selected uncapped Lead PawnCapture as `{route} captures the pawn on {target}.`, rejects Support, Context, Blocked, capped, refuted, no-claim, wrong-claim, and malformed plans, and emits none of the forbidden wins-pawn, wins-material, passed-pawn, file-opening, weakness, breakthrough, initiative, best-move, only-move, or forcing wording."
    ).foreach: required =>
      assert(interactionLaw.contains(required), s"PawnCapture-7 law must pin: $required")

    Vector(readme, ssot, architecture, normalizedModelContract).foreach: doc =>
      assert(doc.contains(PawnCaptureSummary), "summary docs must keep the PawnCapture summary pointer")
      assert(
        !doc.contains("### PawnCapture-7 Deterministic Renderer"),
        "summary docs must not duplicate detailed PawnCapture-7 law"
      )
      assert(
        !doc.contains("PawnCapture-7 allowed deterministic wording:"),
        "summary docs must not duplicate PawnCapture-7 wording law"
      )
      assert(
        !doc.contains("PawnCapture-7 forbidden renderer wording:"),
        "summary docs must not duplicate PawnCapture-7 forbidden wording"
      )
    assert(
      manifest.contains(PawnCaptureSummary),
      "LegacyPruneManifest must keep the PawnCapture summary pointer"
    )
    assert(
      !manifest.contains("### PawnCapture-7 Deterministic Renderer"),
      "LegacyPruneManifest must not duplicate detailed PawnCapture-7 law"
    )
    assert(
      !manifest.contains("PawnCapture-7 allowed deterministic wording:"),
      "LegacyPruneManifest must not duplicate PawnCapture-7 wording law"
    )

  test("PawnCapture-8 pins LLM smoke to existing 8B boundary only"):
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))
    val readme = Files.readString(docsRoot.resolve("README.md"))
    val ssot = Files.readString(docsRoot.resolve("ChessCommentarySSOT.md"))
    val architecture = Files.readString(docsRoot.resolve("ChessModelArchitecture.md"))
    val contract = Files.readString(docsRoot.resolve("ChessModelContract.md"))
    val manifest = Files.readString(docsRoot.resolve("LegacyPruneManifest.md"))
    val normalizedModelContract = contract.replaceAll("\\s+", " ")

    Vector(
      "### PawnCapture-8 LLM Smoke",
      "PawnCapture-8 reuses only the existing 8B LLM smoke boundary.",
      "PawnCapture-8 LLM input:",
      "- renderedText",
      "- claimKey",
      "- strength",
      "- forbidden wording",
      "- `Rephrase only. Do not add chess facts.`",
      "PawnCapture-8 forbidden LLM input:",
      "- raw Story",
      "- raw PawnCaptureProof",
      "- CaptureResult",
      "- BoardFacts",
      "- EngineCheck",
      "- raw PV",
      "- proofFailures",
      "PawnCapture-8 forbidden LLM output:",
      "- new move",
      "- new line",
      "- material claim",
      "- passed-pawn claim",
      "- open-file claim",
      "- weakness claim",
      "- strategy claim",
      "Completion standard: PawnCapture-8 closes when LLM smoke receives only rendered text contract fields, reuses renderedText, claimKey, strength, forbidden wording, and the rephrase-only instruction, rejects raw Story, raw PawnCaptureProof, CaptureResult, BoardFacts, EngineCheck, raw PV, proofFailures, new move, new line, and material, passed-pawn, open-file, weakness, or strategy claims, and adds no new LLM behavior, public LLM narration, production API surface, or public route 200."
    ).foreach: required =>
      assert(interactionLaw.contains(required), s"PawnCapture-8 law must pin: $required")

    Vector(readme, ssot, architecture, normalizedModelContract).foreach: doc =>
      assert(doc.contains(PawnCaptureSummary), "summary docs must keep the PawnCapture summary pointer")
      assert(
        !doc.contains("### PawnCapture-8 LLM Smoke"),
        "summary docs must not duplicate detailed PawnCapture-8 law"
      )
      assert(
        !doc.contains("PawnCapture-8 forbidden LLM input:"),
        "summary docs must not duplicate PawnCapture-8 input law"
      )
      assert(
        !doc.contains("PawnCapture-8 forbidden LLM output:"),
        "summary docs must not duplicate PawnCapture-8 output law"
      )
    assert(
      manifest.contains(PawnCaptureSummary),
      "LegacyPruneManifest must keep the PawnCapture summary pointer"
    )
    assert(
      !manifest.contains("### PawnCapture-8 LLM Smoke"),
      "LegacyPruneManifest must not duplicate detailed PawnCapture-8 law"
    )
    assert(
      !manifest.contains("PawnCapture-8 forbidden LLM input:"),
      "LegacyPruneManifest must not duplicate PawnCapture-8 input law"
    )
    assert(
      !manifest.contains("PawnCapture-8 forbidden LLM output:"),
      "LegacyPruneManifest must not duplicate PawnCapture-8 output law"
    )

  test("PawnCapture Closeout hard cleanup keeps authority separated and public surfaces closed"):
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))
    val readme = Files.readString(docsRoot.resolve("README.md"))
    val ssot = Files.readString(docsRoot.resolve("ChessCommentarySSOT.md"))
    val architecture = Files.readString(docsRoot.resolve("ChessModelArchitecture.md"))
    val contract = Files.readString(docsRoot.resolve("ChessModelContract.md"))
    val manifest = Files.readString(docsRoot.resolve("LegacyPruneManifest.md"))
    val normalizedModelContract = contract.replaceAll("\\s+", " ")

    Vector(
      "### PawnCapture Closeout Hard Cleanup",
      "`PawnCaptureProof`, `Scene.PawnCapture`, and `captures_rival_pawn` remain separate authority homes.",
      "`PawnCaptureProof` proves only a legal ordinary pawn move capturing one rival pawn on the exact board.",
      "`Scene.PawnCapture` owns only the public Story identity for the bounded pawn-captures-rival-pawn event.",
      "`captures_rival_pawn` owns only bounded downstream speech for selected uncapped Lead Verdicts.",
      "`Scene.PawnCapture` owns no PawnBreak, PawnAdvance, PawnStop, PromotionThreat, Promotion, or PassedPawnCreated meaning.",
      "`Scene.PawnCapture` owns no Material, Hanging, Defense, DiscoveredAttack, Pin, RemoveGuard, or Skewer meaning.",
      "`wins pawn`, `creates passed pawn`, `opens file`, `weakens structure`, `initiative`, and `pressure` remain forbidden wording only, not live PawnCapture authority.",
      "PawnCapture duplicate checks:",
      "- one chess meaning: one legal pawn move captures one rival pawn.",
      "- one proof home: `PawnCaptureProof`.",
      "- one Story label: `Scene.PawnCapture`.",
      "- one speech key: `captures_rival_pawn`.",
      "- one detailed live authority document: `StoryInteractionLaw.md`.",
      "README, SSOT, Architecture, Contract, and Manifest may summarize PawnCapture only.",
      "Renderer and LLM smoke wording must remain no stronger than `captures_rival_pawn`.",
      "Public route `200`, production API, and public/user-facing LLM narration remain closed.",
      "Completion standard: PawnCapture Closeout closes when PawnCaptureProof, Scene.PawnCapture, and captures_rival_pawn remain separate authority homes"
    ).foreach: required =>
      assert(interactionLaw.contains(required), s"PawnCapture Closeout law must pin: $required")

    Vector(readme, ssot, architecture, normalizedModelContract).foreach: doc =>
      assert(doc.contains(PawnCaptureSummary), "summary docs must keep the PawnCapture summary pointer")
      assert(
        !doc.contains("### PawnCapture Closeout Hard Cleanup"),
        "summary docs must not duplicate detailed PawnCapture closeout law"
      )
      assert(
        !doc.contains("PawnCapture duplicate checks:"),
        "summary docs must not duplicate detailed PawnCapture duplicate audit"
      )
      assert(
        !doc.contains(
          "PawnCaptureProof, Scene.PawnCapture, and captures_rival_pawn remain separate authority homes"
        ),
        "summary docs must not duplicate PawnCapture authority-home detail"
      )
    assert(
      manifest.contains(PawnCaptureSummary),
      "LegacyPruneManifest must keep the PawnCapture summary pointer"
    )
    assert(
      !manifest.contains("### PawnCapture Closeout Hard Cleanup"),
      "LegacyPruneManifest must not duplicate detailed PawnCapture closeout law"
    )
    assert(
      !manifest.contains("PawnCapture duplicate checks:"),
      "LegacyPruneManifest must not duplicate detailed PawnCapture duplicate audit"
    )
    assert(
      !manifest.contains(
        "PawnCaptureProof, Scene.PawnCapture, and captures_rival_pawn remain separate authority homes"
      ),
      "LegacyPruneManifest must not duplicate PawnCapture authority-home detail"
    )

  test("FPSNC-0 closes file pawn structure neighborhood without opening new meaning"):
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))
    val readme = Files.readString(docsRoot.resolve("README.md"))
    val ssot = Files.readString(docsRoot.resolve("ChessCommentarySSOT.md"))
    val architecture = Files.readString(docsRoot.resolve("ChessModelArchitecture.md"))
    val contract = Files.readString(docsRoot.resolve("ChessModelContract.md"))
    val manifest = Files.readString(docsRoot.resolve("LegacyPruneManifest.md"))
    val normalizedModelContract = contract.replaceAll("\\s+", " ")

    Vector(
      "### FPSNC-0 File / Pawn Structure Neighborhood Closeout Charter",
      "File / Pawn Structure Neighborhood closes as four narrow proof-backed event slices. FPSNC opens no new chess meaning.",
      "FPSNC-0 closes only already-open narrow meanings:",
      "- `Scene.PawnBreak`",
      "- `Scene.PawnCapture`",
      "- `Scene.PassedPawnCreated`",
      "- `Scene.FileOpened`",
      "`PawnBreakProof`, `Scene.PawnBreak`, and `challenges_pawn` remain separate authority homes.",
      "`PawnCaptureProof`, `Scene.PawnCapture`, and `captures_rival_pawn` remain separate authority homes.",
      "`PassedPawnObservation`, `PassedPawnCreatedProof`, `Scene.PassedPawnCreated`, and `creates_passed_pawn` remain separate authority homes.",
      "`FileOpenedProof`, `Scene.FileOpened`, and `opens_file` remain separate authority homes.",
      "The four Story labels must not steal each other's claims.",
      "`Scene.FileOpened` owns no file control, rook activity, pressure, weakness, breakthrough, strategy, or advantage meaning.",
      "`Scene.PawnBreak` does not own FileOpened meaning.",
      "`Scene.PawnCapture` does not automatically own PassedPawnCreated or FileOpened meaning.",
      "`Scene.PassedPawnCreated` does not automatically own PawnCapture or FileOpened events.",
      "Four pawn/file event slices are open. Broad pawn-structure and file interpretation remains closed.",
      "one origin-file-now-open meaning: `FileOpenedProof` -> `Scene.FileOpened` -> `opens_file`.",
      "Completion standard: FPSNC-0 closes when PawnBreakProof / Scene.PawnBreak / challenges_pawn, PawnCaptureProof / Scene.PawnCapture / captures_rival_pawn, PassedPawnObservation / PassedPawnCreatedProof / Scene.PassedPawnCreated / creates_passed_pawn, and FileOpenedProof / Scene.FileOpened / opens_file remain separate"
    ).foreach: required =>
      assert(interactionLaw.contains(required), s"FPSNC-0 law must pin: $required")

    Vector(readme, ssot, architecture, normalizedModelContract).foreach: doc =>
      assert(doc.contains(FileOpenedSummary), "summary docs must keep FileOpened summary")
      assert(doc.contains(FilePawnStructureCloseoutSummary), "summary docs must keep FPSNC-0 summary")
      assert(!doc.contains("### FPSNC-0 File / Pawn Structure Neighborhood Closeout Charter"))
      assert(!doc.contains("FPSNC-0 closes only already-open narrow meanings:"))
      assert(!doc.contains("The four Story labels must not steal each other's claims."))
    assert(manifest.contains(FileOpenedSummary), "Manifest must keep FileOpened summary")
    assert(manifest.contains(FilePawnStructureCloseoutSummary), "Manifest must keep FPSNC-0 summary")
    assert(!manifest.contains("### FPSNC-0 File / Pawn Structure Neighborhood Closeout Charter"))
    assert(!manifest.contains("FPSNC-0 closes only already-open narrow meanings:"))
    assert(!manifest.contains("The four Story labels must not steal each other's claims."))

  test("FPSNC-1 scope audit keeps exactly four event slices open"):
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))
    val readme = Files.readString(docsRoot.resolve("README.md"))
    val ssot = Files.readString(docsRoot.resolve("ChessCommentarySSOT.md"))
    val architecture = Files.readString(docsRoot.resolve("ChessModelArchitecture.md"))
    val contract = Files.readString(docsRoot.resolve("ChessModelContract.md"))
    val manifest = Files.readString(docsRoot.resolve("LegacyPruneManifest.md"))
    val normalizedModelContract = contract.replaceAll("\\s+", " ")

    Vector(
      "### FPSNC-1 Scope Audit",
      "Four exact event slices are open. Broad interpretation remains closed.",
      "FPSNC-1 open positive Stories are exactly:",
      "- `Scene.PawnBreak`",
      "- `Scene.PawnCapture`",
      "- `Scene.PassedPawnCreated`",
      "- `Scene.FileOpened`",
      "FPSNC-1 does not open:",
      "- broad PawnTactic",
      "- broad pawn structure advantage",
      "- file control",
      "- rook activity",
      "- rook lift",
      "- open-file strategy",
      "- passed pawn strategy",
      "- breakthrough",
      "- weak square",
      "- weak pawn",
      "- weakens structure",
      "- pawn majority change",
      "- wins space",
      "- wins pawn",
      "- wins material",
      "- promotion threat",
      "- unstoppable pawn",
      "- conversion",
      "- initiative",
      "- pressure",
      "- attack",
      "- best move / only move / forced",
      "- public route `200`",
      "- production API",
      "- public/user-facing LLM narration",
      "Completion standard: FPSNC-1 closes when the only open File / Pawn Structure Neighborhood positive Stories are Scene.PawnBreak, Scene.PawnCapture, Scene.PassedPawnCreated, and Scene.FileOpened"
    ).foreach: required =>
      assert(interactionLaw.contains(required), s"FPSNC-1 law must pin: $required")

    Vector(readme, ssot, architecture, normalizedModelContract).foreach: doc =>
      assert(doc.contains(FilePawnStructureCloseoutSummary), "summary docs must keep FPSNC summary")
      assert(!doc.contains("### FPSNC-1 Scope Audit"))
      assert(!doc.contains("FPSNC-1 open positive Stories are exactly:"))
      assert(!doc.contains("FPSNC-1 does not open:"))
    assert(manifest.contains(FilePawnStructureCloseoutSummary), "Manifest must keep FPSNC summary")
    assert(!manifest.contains("### FPSNC-1 Scope Audit"))
    assert(!manifest.contains("FPSNC-1 open positive Stories are exactly:"))
    assert(!manifest.contains("FPSNC-1 does not open:"))

  test("FPSNC-2 authority duplication audit keeps proof Story and speech homes unique"):
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))
    val readme = Files.readString(docsRoot.resolve("README.md"))
    val ssot = Files.readString(docsRoot.resolve("ChessCommentarySSOT.md"))
    val architecture = Files.readString(docsRoot.resolve("ChessModelArchitecture.md"))
    val contract = Files.readString(docsRoot.resolve("ChessModelContract.md"))
    val manifest = Files.readString(docsRoot.resolve("LegacyPruneManifest.md"))
    val normalizedModelContract = contract.replaceAll("\\s+", " ")

    Vector(
      "### FPSNC-2 Authority / Duplication Audit",
      "FPSNC-2 duplication audit:",
      "- one chess meaning, one proof home",
      "- one Story label per public chess meaning",
      "- one speech key per allowed public claim",
      "- one detailed live authority document",
      "direct rival-pawn lever/contact:",
      "- proof home = `PawnBreakProof`",
      "- Story label = `Scene.PawnBreak`",
      "- speech key = `challenges_pawn`",
      "pawn captures rival pawn:",
      "- proof home = `PawnCaptureProof`",
      "- Story label = `Scene.PawnCapture`",
      "- speech key = `captures_rival_pawn`",
      "newly-created passed pawn:",
      "- observation = `PassedPawnObservation`",
      "- proof home = `PassedPawnCreatedProof`",
      "- Story label = `Scene.PassedPawnCreated`",
      "- speech key = `creates_passed_pawn`",
      "origin file now has no pawns:",
      "- proof home = `FileOpenedProof`",
      "- Story label = `Scene.FileOpened`",
      "- speech key = `opens_file`",
      "`PawnBreakProof` must not act as capture, passer, or file-open proof.",
      "`PawnCaptureProof` must not act as material, passer, or file-open proof.",
      "`PassedPawnCreatedProof` must not act as capture, pawn-break, or file-open proof.",
      "`FileOpenedProof` must not act as pawn-break, capture, passer, control, rook, or weakness proof.",
      "Speech keys must not duplicate.",
      "Completion standard: FPSNC-2 closes when direct rival-pawn lever/contact, pawn captures rival pawn, newly-created passed pawn, and origin-file-now-has-no-pawns each have exactly one proof home, one Story label, and one speech key"
    ).foreach: required =>
      assert(interactionLaw.contains(required), s"FPSNC-2 law must pin: $required")

    Vector(readme, ssot, architecture, normalizedModelContract).foreach: doc =>
      assert(doc.contains(FilePawnStructureCloseoutSummary), "summary docs must keep FPSNC summary")
      assert(!doc.contains("### FPSNC-2 Authority / Duplication Audit"))
      assert(!doc.contains("FPSNC-2 duplication audit:"))
      assert(!doc.contains("direct rival-pawn lever/contact:"))
      assert(!doc.contains("origin file now has no pawns:"))
    assert(manifest.contains(FilePawnStructureCloseoutSummary), "Manifest must keep FPSNC summary")
    assert(!manifest.contains("### FPSNC-2 Authority / Duplication Audit"))
    assert(!manifest.contains("FPSNC-2 duplication audit:"))
    assert(!manifest.contains("direct rival-pawn lever/contact:"))
    assert(!manifest.contains("origin file now has no pawns:"))

  test("FPSNC-3 cross slice collision fixtures keep each row in its proof home"):
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))
    val readme = Files.readString(docsRoot.resolve("README.md"))
    val ssot = Files.readString(docsRoot.resolve("ChessCommentarySSOT.md"))
    val architecture = Files.readString(docsRoot.resolve("ChessModelArchitecture.md"))
    val contract = Files.readString(docsRoot.resolve("ChessModelContract.md"))
    val manifest = Files.readString(docsRoot.resolve("LegacyPruneManifest.md"))
    val normalizedModelContract = contract.replaceAll("\\s+", " ")

    Vector(
      "### FPSNC-3 Cross-Slice Collision Fixtures",
      "FPSNC-3 adds complex same-board collision fixtures without opening new meaning.",
      "FPSNC-3 required fixture candidates:",
      "- pawn move creates direct pawn contact and opens origin file",
      "- pawn capture opens origin file",
      "- pawn capture also creates a passed pawn",
      "- pawn move creates passed pawn and opens origin file",
      "- pawn move creates direct contact and newly-created passer",
      "- material capture that is also PawnCapture and FileOpened",
      "- FileOpened candidate that is only half-open and must stay silent",
      "- FileOpened candidate with another pawn still on origin file and must stay silent",
      "FPSNC-3 verification:",
      "- each row keeps its own proof home",
      "- each row keeps its own Story label",
      "- each row keeps its own speech key",
      "- no row automatically upgrades into another meaning without its proof",
      "- actual material change now remains Scene.Material home",
      "- FileOpened speaks only exact origin-file-open state",
      "- PassedPawnCreated speaks only newly-created passer state",
      "- PawnCapture speaks only pawn-captures-rival-pawn event",
      "- PawnBreak speaks only direct pawn contact/challenge event",
      "Completion standard: FPSNC-3 closes when the required same-board collision fixtures prove that each open File / Pawn Structure row keeps its own proof home, Story label, and speech key"
    ).foreach: required =>
      assert(interactionLaw.contains(required), s"FPSNC-3 law must pin: $required")

    Vector(readme, ssot, architecture, normalizedModelContract).foreach: doc =>
      assert(doc.contains(FilePawnStructureCloseoutSummary), "summary docs must keep FPSNC summary")
      assert(!doc.contains("### FPSNC-3 Cross-Slice Collision Fixtures"))
      assert(!doc.contains("FPSNC-3 required fixture candidates:"))
      assert(!doc.contains("FPSNC-3 verification:"))
    assert(manifest.contains(FilePawnStructureCloseoutSummary), "Manifest must keep FPSNC summary")
    assert(!manifest.contains("### FPSNC-3 Cross-Slice Collision Fixtures"))
    assert(!manifest.contains("FPSNC-3 required fixture candidates:"))
    assert(!manifest.contains("FPSNC-3 verification:"))

  test("FPSNC-4 StoryTable interaction audit keeps File Pawn Structure rows in existing homes"):
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))
    val readme = Files.readString(docsRoot.resolve("README.md"))
    val ssot = Files.readString(docsRoot.resolve("ChessCommentarySSOT.md"))
    val architecture = Files.readString(docsRoot.resolve("ChessModelArchitecture.md"))
    val contract = Files.readString(docsRoot.resolve("ChessModelContract.md"))
    val manifest = Files.readString(docsRoot.resolve("LegacyPruneManifest.md"))
    val normalizedModelContract = contract.replaceAll("\\s+", " ")

    Vector(
      "### FPSNC-4 StoryTable Interaction Audit",
      "FPSNC-4 collides the four File / Pawn Structure event rows with existing StoryTable rows without opening new meaning.",
      "FPSNC-4 collision targets:",
      "- Scene.PawnBreak",
      "- Scene.PawnCapture",
      "- Scene.PassedPawnCreated",
      "- Scene.FileOpened",
      "- Scene.PawnAdvance",
      "- Scene.PawnStop",
      "- Scene.PromotionThreat",
      "- Scene.Promotion",
      "- Scene.Material",
      "- Tactic.Hanging",
      "- Scene.Defense",
      "- Line / Defender tactics",
      "FPSNC-4 verification:",
      "- input order remains stable",
      "- each claim keeps its existing home",
      "- PawnBreak does not own FileOpened, PawnCapture, or PassedPawnCreated meaning",
      "- PawnCapture does not own FileOpened, PassedPawnCreated, or Material meaning",
      "- PassedPawnCreated does not own FileOpened, PawnCapture, PromotionThreat, or Promotion meaning",
      "- FileOpened does not own PawnBreak, PawnCapture, PassedPawnCreated, or Material meaning",
      "- actual material change now remains Scene.Material home",
      "- promotion-next meaning remains Scene.PromotionThreat home",
      "- actual promotion meaning remains Scene.Promotion home",
      "- capped or refuted rows have no standalone text",
      "- Support, Context, and Blocked rows have no standalone text",
      "Completion standard: FPSNC-4 closes when StoryTable collision fixtures prove that the four File / Pawn Structure rows keep input-order-stable roles against existing pawn, material, hanging, defense, and line/defender rows"
    ).foreach: required =>
      assert(interactionLaw.contains(required), s"FPSNC-4 law must pin: $required")

    Vector(readme, ssot, architecture, normalizedModelContract).foreach: doc =>
      assert(doc.contains(FilePawnStructureCloseoutSummary), "summary docs must keep FPSNC summary")
      assert(!doc.contains("### FPSNC-4 StoryTable Interaction Audit"))
      assert(!doc.contains("FPSNC-4 collision targets:"))
      assert(!doc.contains("FPSNC-4 verification:"))
    assert(manifest.contains(FilePawnStructureCloseoutSummary), "Manifest must keep FPSNC summary")
    assert(!manifest.contains("### FPSNC-4 StoryTable Interaction Audit"))
    assert(!manifest.contains("FPSNC-4 collision targets:"))
    assert(!manifest.contains("FPSNC-4 verification:"))

  test("FPSNC-5 ExplanationPlan boundary audit keeps only selected Lead claim keys"):
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))
    val readme = Files.readString(docsRoot.resolve("README.md"))
    val ssot = Files.readString(docsRoot.resolve("ChessCommentarySSOT.md"))
    val architecture = Files.readString(docsRoot.resolve("ChessModelArchitecture.md"))
    val contract = Files.readString(docsRoot.resolve("ChessModelContract.md"))
    val manifest = Files.readString(docsRoot.resolve("LegacyPruneManifest.md"))
    val normalizedModelContract = contract.replaceAll("\\s+", " ")

    Vector(
      "### FPSNC-5 ExplanationPlan Boundary Audit",
      "FPSNC-5 audits ExplanationPlan lowering for the four File / Pawn Structure event rows without opening new meaning.",
      "ExplanationPlan input is selected uncapped Lead Verdict only.",
      "FPSNC-5 allowed claim keys:",
      "- challenges_pawn",
      "- captures_rival_pawn",
      "- creates_passed_pawn",
      "- opens_file",
      "FPSNC-5 forbidden claim keys:",
      "- controls_file",
      "- uses_open_file",
      "- rook_activity",
      "- creates_pressure",
      "- takes_initiative",
      "- weakens_structure",
      "- creates_weakness",
      "- breaks_through",
      "- wins_space",
      "- wins_pawn",
      "- wins_material",
      "- promotion_threat",
      "- unstoppable_pawn",
      "- converts_advantage",
      "- best_move",
      "- only_move",
      "- forced",
      "FPSNC-5 verification:",
      "- Support, Context, Blocked, capped, and refuted rows have no standalone claim",
      "- non-selected rows have no standalone claim",
      "- selected Lead lowers only to its own claim key",
      "- selected Lead must not lower to a sibling claim key",
      "Completion standard: FPSNC-5 closes when ExplanationPlan accepts only selected uncapped Lead Verdict input for Scene.PawnBreak, Scene.PawnCapture, Scene.PassedPawnCreated, and Scene.FileOpened"
    ).foreach: required =>
      assert(interactionLaw.contains(required), s"FPSNC-5 law must pin: $required")

    Vector(readme, ssot, architecture, normalizedModelContract).foreach: doc =>
      assert(doc.contains(FilePawnStructureCloseoutSummary), "summary docs must keep FPSNC summary")
      assert(!doc.contains("### FPSNC-5 ExplanationPlan Boundary Audit"))
      assert(!doc.contains("FPSNC-5 allowed claim keys:"))
      assert(!doc.contains("FPSNC-5 forbidden claim keys:"))
      assert(!doc.contains("FPSNC-5 verification:"))
    assert(manifest.contains(FilePawnStructureCloseoutSummary), "Manifest must keep FPSNC summary")
    assert(!manifest.contains("### FPSNC-5 ExplanationPlan Boundary Audit"))
    assert(!manifest.contains("FPSNC-5 allowed claim keys:"))
    assert(!manifest.contains("FPSNC-5 forbidden claim keys:"))
    assert(!manifest.contains("FPSNC-5 verification:"))

  test("FPSNC-6 renderer boundary audit keeps only bounded FPSNC templates"):
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))
    val readme = Files.readString(docsRoot.resolve("README.md"))
    val ssot = Files.readString(docsRoot.resolve("ChessCommentarySSOT.md"))
    val architecture = Files.readString(docsRoot.resolve("ChessModelArchitecture.md"))
    val contract = Files.readString(docsRoot.resolve("ChessModelContract.md"))
    val manifest = Files.readString(docsRoot.resolve("LegacyPruneManifest.md"))
    val normalizedModelContract = contract.replaceAll("\\s+", " ")

    Vector(
      "### FPSNC-6 Renderer Boundary Audit",
      "FPSNC-6 audits deterministic renderer wording for the four File / Pawn Structure event rows without opening new meaning.",
      "Renderer input is ExplanationPlan only.",
      "FPSNC-6 allowed bounded templates:",
      "- {route} challenges the pawn on {target}.",
      "- {route} captures the pawn on {target}.",
      "- {route} creates a passed pawn on {target}.",
      "- {route} opens the {file}-file.",
      "FPSNC-6 forbidden renderer wording:",
      "- controls the file",
      "- uses the open file",
      "- activates the rook",
      "- creates pressure",
      "- takes the initiative",
      "- weakens the structure",
      "- creates a weakness",
      "- breaks through",
      "- wins space",
      "- wins a pawn",
      "- wins material",
      "- will promote",
      "- is unstoppable",
      "- best move",
      "- only move",
      "- forces",
      "FPSNC-6 verification:",
      "- renderer does not read raw Story",
      "- renderer does not read raw proof",
      "- renderer does not read BoardFacts, EngineCheck, or proofFailures",
      "- malformed plans produce no text",
      "- capped, refuted, Support, Context, and Blocked plans produce no text",
      "Completion standard: FPSNC-6 closes when DeterministicRenderer accepts only ExplanationPlan input for Scene.PawnBreak, Scene.PawnCapture, Scene.PassedPawnCreated, and Scene.FileOpened"
    ).foreach: required =>
      assert(interactionLaw.contains(required), s"FPSNC-6 law must pin: $required")

    Vector(readme, ssot, architecture, normalizedModelContract).foreach: doc =>
      assert(doc.contains(FilePawnStructureCloseoutSummary), "summary docs must keep FPSNC summary")
      assert(!doc.contains("### FPSNC-6 Renderer Boundary Audit"))
      assert(!doc.contains("FPSNC-6 allowed bounded templates:"))
      assert(!doc.contains("FPSNC-6 forbidden renderer wording:"))
      assert(!doc.contains("FPSNC-6 verification:"))
    assert(manifest.contains(FilePawnStructureCloseoutSummary), "Manifest must keep FPSNC summary")
    assert(!manifest.contains("### FPSNC-6 Renderer Boundary Audit"))
    assert(!manifest.contains("FPSNC-6 allowed bounded templates:"))
    assert(!manifest.contains("FPSNC-6 forbidden renderer wording:"))
    assert(!manifest.contains("FPSNC-6 verification:"))

  test("FPSNC-7 LLM smoke boundary audit reuses only existing 8B contract"):
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))
    val readme = Files.readString(docsRoot.resolve("README.md"))
    val ssot = Files.readString(docsRoot.resolve("ChessCommentarySSOT.md"))
    val architecture = Files.readString(docsRoot.resolve("ChessModelArchitecture.md"))
    val contract = Files.readString(docsRoot.resolve("ChessModelContract.md"))
    val manifest = Files.readString(docsRoot.resolve("LegacyPruneManifest.md"))
    val normalizedModelContract = contract.replaceAll("\\s+", " ")

    Vector(
      "### FPSNC-7 LLM Smoke Boundary Audit",
      "FPSNC-7 reuses only the existing 8B LLM smoke boundary for the four File / Pawn Structure event rows.",
      "LLM may rephrase only the selected bounded event. It must not explain why the event matters.",
      "FPSNC-7 LLM input:",
      "- renderedText",
      "- claimKey",
      "- strength",
      "- forbidden wording",
      "- Rephrase only. Do not add chess facts.",
      "FPSNC-7 forbidden LLM input:",
      "- raw Story",
      "- raw PawnBreakProof",
      "- raw PawnCaptureProof",
      "- raw PassedPawnCreatedProof",
      "- raw FileOpenedProof",
      "- PassedPawnObservation",
      "- BoardFacts",
      "- EngineCheck",
      "- raw PV",
      "- proofFailures",
      "- source rows",
      "FPSNC-7 forbidden LLM output:",
      "- new move",
      "- new line",
      "- file control",
      "- rook activity",
      "- pressure",
      "- initiative",
      "- weakness",
      "- breakthrough",
      "- material",
      "- passed pawn, unless selected claimKey = creates_passed_pawn",
      "- open file, unless selected claimKey = opens_file",
      "- promotion",
      "- unstoppable",
      "- conversion",
      "- best move",
      "- only move",
      "- forced",
      "- strategy claim",
      "Completion standard: FPSNC-7 closes when LLM smoke may rephrase only the selected bounded event and must not explain why the event matters"
    ).foreach: required =>
      assert(interactionLaw.contains(required), s"FPSNC-7 law must pin: $required")

    Vector(readme, ssot, architecture, normalizedModelContract).foreach: doc =>
      assert(doc.contains(FilePawnStructureCloseoutSummary), "summary docs must keep FPSNC summary")
      assert(!doc.contains("### FPSNC-7 LLM Smoke Boundary Audit"))
      assert(!doc.contains("FPSNC-7 LLM input:"))
      assert(!doc.contains("FPSNC-7 forbidden LLM input:"))
      assert(!doc.contains("FPSNC-7 forbidden LLM output:"))
    assert(manifest.contains(FilePawnStructureCloseoutSummary), "Manifest must keep FPSNC summary")
    assert(!manifest.contains("### FPSNC-7 LLM Smoke Boundary Audit"))
    assert(!manifest.contains("FPSNC-7 LLM input:"))
    assert(!manifest.contains("FPSNC-7 forbidden LLM input:"))
    assert(!manifest.contains("FPSNC-7 forbidden LLM output:"))

  test("FPSNC-8 forbidden wording audit keeps closed wording non-authoritative"):
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))
    val readme = Files.readString(docsRoot.resolve("README.md"))
    val ssot = Files.readString(docsRoot.resolve("ChessCommentarySSOT.md"))
    val architecture = Files.readString(docsRoot.resolve("ChessModelArchitecture.md"))
    val contract = Files.readString(docsRoot.resolve("ChessModelContract.md"))
    val manifest = Files.readString(docsRoot.resolve("LegacyPruneManifest.md"))
    val normalizedModelContract = contract.replaceAll("\\s+", " ")

    Vector(
      "### FPSNC-8 Forbidden Wording Audit",
      "FPSNC-8 audits forbidden wording for the four File / Pawn Structure event rows without opening new chess meaning.",
      "Forbidden wording may exist only as forbidden wording, negative corpus, or docs saying it remains closed.",
      "FPSNC-8 forbidden live-authority wording:",
      "- controls file",
      "- file control",
      "- rook activity",
      "- rook lift",
      "- pressure",
      "- initiative",
      "- weak square",
      "- weak pawn",
      "- weakens structure",
      "- breakthrough",
      "- breaks through",
      "- wins space",
      "- wins pawn",
      "- wins material",
      "- promotion threat, unless selected Scene.PromotionThreat",
      "- passed pawn, unless selected Scene.PassedPawnCreated or existing PawnAdvance/PawnStop bounded claim",
      "- open file, unless selected Scene.FileOpened",
      "- unstoppable",
      "- conversion",
      "- best move",
      "- only move",
      "- forced",
      "FPSNC-8 verification:",
      "- forbidden wording does not become proof home",
      "- forbidden wording does not become Story label",
      "- forbidden wording does not become claim key",
      "- forbidden wording does not become renderer output",
      "- forbidden wording does not become LLM-added fact",
      "- forbidden wording does not become public value",
      "- forbidden wording does not become public route payload",
      "Completion standard: FPSNC-8 closes when forbidden wording may exist only as forbidden wording, negative corpus, or docs saying it remains closed"
    ).foreach: required =>
      assert(interactionLaw.contains(required), s"FPSNC-8 law must pin: $required")

    Vector(readme, ssot, architecture, normalizedModelContract).foreach: doc =>
      assert(doc.contains(FilePawnStructureCloseoutSummary), "summary docs must keep FPSNC summary")
      assert(!doc.contains("### FPSNC-8 Forbidden Wording Audit"))
      assert(!doc.contains("FPSNC-8 forbidden live-authority wording:"))
      assert(!doc.contains("FPSNC-8 verification:"))
    assert(manifest.contains(FilePawnStructureCloseoutSummary), "Manifest must keep FPSNC summary")
    assert(!manifest.contains("### FPSNC-8 Forbidden Wording Audit"))
    assert(!manifest.contains("FPSNC-8 forbidden live-authority wording:"))
    assert(!manifest.contains("FPSNC-8 verification:"))

  test("FPSNC-9 documentation simplification audit keeps detailed closeout authority in StoryInteractionLaw only"):
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))
    val readme = Files.readString(docsRoot.resolve("README.md"))
    val ssot = Files.readString(docsRoot.resolve("ChessCommentarySSOT.md"))
    val architecture = Files.readString(docsRoot.resolve("ChessModelArchitecture.md"))
    val contract = Files.readString(docsRoot.resolve("ChessModelContract.md"))
    val manifest = Files.readString(docsRoot.resolve("LegacyPruneManifest.md"))
    val normalizedModelContract = contract.replaceAll("\\s+", " ")

    Vector(
      "### FPSNC-9 Documentation Simplification Audit",
      "FPSNC-9 documentation simplification audit:",
      "- detailed File / Pawn Structure Neighborhood closeout authority lives only in `StoryInteractionLaw.md`.",
      "- `README.md`, `ChessCommentarySSOT.md`, `ChessModelArchitecture.md`, `ChessModelContract.md`, and `LegacyPruneManifest.md` remain summary-only.",
      "- `AGENTS.md` keeps durable operator guardrails only.",
      "- retired root docs must not return.",
      "- legacy reset archive documents are historical reference only and not acceptance sources.",
      "FPSNC-9 summary-only documents:",
      "- `README.md`: non-authoritative pointer only.",
      "- `ChessCommentarySSOT.md`: non-authoritative pointer only.",
      "- `ChessModelArchitecture.md`: non-authoritative pointer only.",
      "- `ChessModelContract.md`: non-authoritative pointer only.",
      "- `LegacyPruneManifest.md`: pruning summary only, not detailed law.",
      "FPSNC-9 forbidden documentation duplication:",
      "- no FPSNC detailed checklist in summary docs.",
      "- no closeout stage law in `AGENTS.md`.",
      "- no detailed FPSNC law in `LegacyPruneManifest.md`.",
      "- no retired root docs as live authority.",
      "- no legacy reset archive acceptance source.",
      "Completion standard: FPSNC-9 closes when detailed FPSNC closeout authority appears only in `StoryInteractionLaw.md`"
    ).foreach: required =>
      assert(interactionLaw.contains(required), s"FPSNC-9 law must pin: $required")

    val summaryDocs = Vector(readme, ssot, architecture, normalizedModelContract)
    summaryDocs.foreach: doc =>
      assert(doc.contains(FilePawnStructureCloseoutSummary), "summary docs must keep FPSNC summary")
      FilePawnStructureCloseoutDetailedMarkers.foreach: marker =>
        assert(!doc.contains(marker), s"summary docs must not repeat detailed FPSNC law: $marker")

    assert(manifest.contains(FilePawnStructureCloseoutSummary), "Manifest must keep FPSNC summary")
    FilePawnStructureCloseoutDetailedMarkers.foreach: marker =>
      assert(!manifest.contains(marker), s"LegacyPruneManifest must not repeat detailed FPSNC law: $marker")

    assert(agents.contains("AGENTS.md is a concise operator guide."))
    assert(agents.contains("Detailed slice authority lives in `StoryInteractionLaw.md`"))
    assert(agents.contains("Documents under"))
    assert(agents.contains("legacy-pre-semantic-reset/"))
    assert(agents.contains("historical reference only"))
    FilePawnStructureCloseoutDetailedMarkers.foreach: marker =>
      assert(!agents.contains(marker), s"AGENTS.md must not repeat detailed FPSNC law: $marker")

    val liveDocTextByName = LiveDocs.map(name => name -> Files.readString(docsRoot.resolve(name))).toMap
    FilePawnStructureCloseoutDetailedMarkers.foreach: marker =>
      val owners = liveDocTextByName.collect:
        case (docName, contents) if contents.contains(marker) => docName
      assertEquals(owners.toSet, Set("StoryInteractionLaw.md"), s"detailed FPSNC marker must have one owner: $marker")

    RetiredRootDocs.foreach: fileName =>
      assert(!Files.exists(docsRoot.resolve(fileName)), s"retired root doc must not return: $fileName")

  test("FPSNC-10 runtime boundary audit keeps helpers out of runtime authority"):
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))
    val readme = Files.readString(docsRoot.resolve("README.md"))
    val ssot = Files.readString(docsRoot.resolve("ChessCommentarySSOT.md"))
    val architecture = Files.readString(docsRoot.resolve("ChessModelArchitecture.md"))
    val contract = Files.readString(docsRoot.resolve("ChessModelContract.md"))
    val manifest = Files.readString(docsRoot.resolve("LegacyPruneManifest.md"))
    val normalizedModelContract = contract.replaceAll("\\s+", " ")

    Vector(
      "### FPSNC-10 Runtime Boundary Audit",
      "FPSNC-10 runtime boundary audit:",
      "- test helpers are not runtime authority.",
      "- runtime helpers must not become new authority.",
      "- fixture names are not Story family names.",
      "- negative corpus helpers are not production concepts.",
      "- forbidden wording checks must target public overclaim wording, not arbitrary internal field names.",
      "- runtime source must not contain closeout-only terminology.",
      "FPSNC-10 forbids new runtime authority:",
      "- no new Story family.",
      "- no new proof home.",
      "- no new Story writer.",
      "- no new claim key.",
      "- no new renderer template.",
      "- no new LLM behavior.",
      "- no public route `200`.",
      "- no production API.",
      "- no public/user-facing LLM narration.",
      "FPSNC-10 allowed closeout changes:",
      "- tests may audit the boundary.",
      "- documentation law may record the audit in `StoryInteractionLaw.md`.",
      "- runtime code changes are allowed only if an audit exposes leakage or duplicated authority.",
      "FPSNC-10 runtime source guard:",
      "- runtime source must not contain `FPSNC-`.",
      "- runtime source must not contain `closeout`.",
      "- runtime source must not contain `Runtime Boundary Audit`.",
      "- runtime source must not contain `File / Pawn Structure Neighborhood Closeout`.",
      "- runtime source must not contain `fixture map`.",
      "- runtime source must not contain `negative corpus`.",
      "Completion standard: FPSNC-10 closes when helper names remain test-local"
    ).foreach: required =>
      assert(interactionLaw.contains(required), s"FPSNC-10 law must pin: $required")

    val detailedMarkers =
      Vector(
        "### FPSNC-10 Runtime Boundary Audit",
        "FPSNC-10 runtime boundary audit:",
        "FPSNC-10 forbids new runtime authority:",
        "FPSNC-10 allowed closeout changes:",
        "FPSNC-10 runtime source guard:"
      )
    Vector(readme, ssot, architecture, normalizedModelContract).foreach: doc =>
      assert(doc.contains(FilePawnStructureCloseoutSummary), "summary docs must keep FPSNC summary")
      detailedMarkers.foreach: marker =>
        assert(!doc.contains(marker), s"summary docs must not repeat detailed FPSNC-10 law: $marker")
    assert(manifest.contains(FilePawnStructureCloseoutSummary), "Manifest must keep FPSNC summary")
    detailedMarkers.foreach: marker =>
      assert(!manifest.contains(marker), s"LegacyPruneManifest must not repeat detailed FPSNC-10 law: $marker")
      assert(!agents.contains(marker), s"AGENTS.md must not repeat detailed FPSNC-10 law: $marker")

    val runtimeText = runtimeChessSourceText
    Vector(
      "FPSNC-",
      "closeout",
      "Runtime Boundary Audit",
      "File / Pawn Structure Neighborhood Closeout",
      "fixture map",
      "negative corpus"
    ).foreach: closeoutOnlyTerm =>
      assert(
        !runtimeText.contains(closeoutOnlyTerm),
        s"runtime source must not contain closeout-only term: $closeoutOnlyTerm"
      )

  test("FPSNC final completion standard closes File Pawn Structure Neighborhood"):
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))
    val readme = Files.readString(docsRoot.resolve("README.md"))
    val ssot = Files.readString(docsRoot.resolve("ChessCommentarySSOT.md"))
    val architecture = Files.readString(docsRoot.resolve("ChessModelArchitecture.md"))
    val contract = Files.readString(docsRoot.resolve("ChessModelContract.md"))
    val manifest = Files.readString(docsRoot.resolve("LegacyPruneManifest.md"))
    val normalizedModelContract = contract.replaceAll("\\s+", " ")

    assert(interactionLaw.contains("### FPSNC Final Completion Standard"))
    Vector(
      "FPSNC final completion standard:",
      "- `Scene.PawnBreak`, `Scene.PawnCapture`, `Scene.PassedPawnCreated`, and `Scene.FileOpened` remain the only open File / Pawn Structure Neighborhood positive slices.",
      "- each slice has exactly one proof path, one Story label, and one speech key.",
      "- no slice owns another slice's chess meaning.",
      "- Material, PromotionThreat, Promotion, PawnAdvance, PawnStop, Hanging, Defense, and Line / Defender tactic homes keep their meanings.",
      "- forbidden broad pawn/file wording remains forbidden only.",
      "- StoryTable ordering is input-order stable across neighborhood collisions.",
      "- non-selected, capped, refuted, Support, Context, and Blocked rows produce no standalone downstream text.",
      "- ExplanationPlan, Renderer, and LLM smoke remain bounded to selected Lead Verdict data.",
      "- detailed authority lives only in `StoryInteractionLaw.md`.",
      "- summary docs remain summaries only.",
      "- public route `200`, production API, and public/user-facing LLM narration remain closed.",
      "FPSNC final verification:",
      "- targeted commentary runtime tests pass.",
      "- docs authority tests pass.",
      "- `git diff --check` passes.",
      "Final FPSNC closeout opens no new Story family, proof home, Story writer, claim key, renderer template, LLM behavior, fixture-derived runtime authority, negative-corpus production concept, broad PawnTactic, broad pawn-structure advantage, file control, rook activity, rook lift, open-file strategy, passed pawn strategy, breakthrough, weak square, weak pawn, weakens structure, pawn majority change, wins space, wins pawn, wins material, promotion threat, unstoppable pawn, conversion, initiative, pressure, attack, best move, only move, forced, public route `200`, production API, or public/user-facing LLM narration."
    ).foreach: finalLine =>
      assert(interactionLaw.contains(finalLine), s"FPSNC final completion must pin: $finalLine")

    Vector(readme, ssot, architecture, normalizedModelContract).foreach: doc =>
      assert(doc.contains(FilePawnStructureCloseoutSummary), "summary docs must keep FPSNC summary")
      assert(
        !doc.contains("FPSNC final completion standard:"),
        "summary docs must not duplicate detailed FPSNC final standard"
      )
      assert(
        !doc.contains("FPSNC final verification:"),
        "summary docs must not duplicate detailed FPSNC final verification"
      )
    assert(manifest.contains(FilePawnStructureCloseoutSummary), "Manifest must keep FPSNC summary")
    assert(
      !manifest.contains("FPSNC final completion standard:"),
      "LegacyPruneManifest must not duplicate detailed FPSNC final standard"
    )
    assert(
      !manifest.contains("FPSNC final verification:"),
      "LegacyPruneManifest must not duplicate detailed FPSNC final verification"
    )
    assert(!agents.contains("FPSNC final completion standard:"))
    assert(!agents.contains("FPSNC final verification:"))

  test("agents and active frontend tests reject retired downstream authority"):
    assert(Files.exists(agentInstructions), "AGENTS.md must be available from the lila worktree")
    assert(
      Files.exists(moveExplanationTest),
      "moveExplanation.test.ts must remain an active frontend no-op test"
    )

    assert(!agents.contains("CommentaryOutline"), "AGENTS.md must not grant CommentaryOutline authority")
    assert(!agents.contains("CommentaryPlan"), "AGENTS.md must not grant CommentaryPlan authority")
    assert(agents.contains("selected `Verdict` data only"))

    val bridgeTest = Files.readString(moveExplanationTest)
    RetiredRootDocs.foreach: docName =>
      assert(
        !bridgeTest.contains(docName),
        s"active moveExplanation.test.ts must not read retired doc $docName"
      )

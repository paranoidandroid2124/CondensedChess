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

  private val commentaryController = Paths.get("app/controllers/Commentary.scala")
  private val commentaryBridgeSource = Paths.get("ui/analyse/src/chesstory/commentaryBridge.ts")

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
    val agents = Files.readString(agentInstructions)
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
      rationale.replaceAll("\\s+", " ").contains(
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
    assert(modelContract.contains("StoryProof is only the minimum evidence form needed before a Story could speak."))
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
    assert(interactionLaw.contains("This diagnostic shape is internal validation, test, and debugging output only."))
    assert(interactionLaw.contains("It is not API/public JSON, renderer input, LLM input, or user commentary."))
    assert(interactionLaw.contains("it does not provide text that may be spoken."))
    assert(modelContract.contains("`Source` and `Opening` never lead over a board-backed story"))
    val chessFoundationTest =
      Files.readString(Paths.get("modules/commentary/src/test/scala/lila/commentary/chess/ChessFoundationTest.scala"))
    assert(chessFoundationTest.contains("Verdict proofFailures are internal diagnostics, not public payload"))
    assert(chessFoundationTest.contains("assertEquals(verdict.values, cleared.values)"))
    assert(chessFoundationTest.contains("Stage 2 ordering does not use proofFailures as public sort input"))
    val controllerSource = Files.readString(commentaryController)
    assert(controllerSource.contains("def renderCommentary"))
    assert(controllerSource.contains("def renderLocalProbeCommentary"))
    assert(controllerSource.contains("ServiceUnavailable(unavailable)"))
    assert(controllerSource.contains("\"noCommentary\" -> true"))
    assert(!controllerSource.contains("Ok("))
    val bridgeSource = Files.readString(commentaryBridgeSource)
    assert(bridgeSource.contains("const PublicRenderRoutesTombstoned = true"))
    assert(bridgeSource.contains("if (PublicRenderRoutesTombstoned) return { kind: 'empty', reason: 'no_commentary' }"))
    assert(readme.contains("Stage order no-go"))
    assert(readme.contains("Current implementation scope is Defense Slice Closeout Pass."))
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
    assert(architecture.contains("Current implementation scope is Defense Slice Closeout Pass."))
    assert(architecture.contains("Stage 4 is named `Engine Check`."))
    assert(architecture.replaceAll("\\s+", " ").contains("Stages 9-11 below"))
    assert(architecture.replaceAll("\\s+", " ").contains("dependency map for product design"))
    assert(
      architecture.replaceAll("\\s+", " ").contains(
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
    assert(interactionLaw.contains("Story comes first. Engine checks, caps, or refutes. Engine never speaks alone."))
    assert(interactionLaw.contains("Stage 4-1 opens only the internal engine evidence shape."))
    assert(interactionLaw.contains("Engine eval, engine line, reply line, and checked move data cannot create a Story."))
    assert(interactionLaw.contains("Renderer, LLM, and public route `200` remain closed."))
    assert(interactionLaw.contains("Stage 4-2 adds same-board and stale engine guards."))
    assert(interactionLaw.contains("Engine evidence must bind to the same board, the same Story route, and the same legal line."))
    assert(
      interactionLaw.contains(
        "Different-FEN engine lines, route-mismatched engine lines, stale engine data, depth-missing engine data, eval-only input without a Story, and PV-only input without a Story are diagnostic only."
      )
    )
    assert(interactionLaw.contains("Stage 4-3 attaches EngineCheck only to `Tactic.Hanging`."))
    assert(interactionLaw.contains("EngineCheck status starts with exactly `Unknown`, `Supports`, `Caps`, and `Refutes`."))
    assert(interactionLaw.contains("`Refutes` blocks the Hanging Story."))
    assert(interactionLaw.contains("`Supports` does not mean winning, best move, decisive, PV explanation, or public truth."))
    assert(interactionLaw.contains("`Caps` leaves the Story available but forbids strong expression."))
    assert(interactionLaw.contains("Stage 4 negative corpus covers local material gain that fails to a larger"))
    assert(interactionLaw.contains("without `CaptureResult`, engine data without complete StoryProof, and engine"))
    assert(interactionLaw.contains("writerless, or proofless engine evidence"))
    assert(interactionLaw.contains("Stage 4 StoryTable integration is conservative."))
    assert(interactionLaw.contains("EngineCheck never creates a Story in StoryTable."))
    assert(interactionLaw.contains("`Caps` records only the internal `engineStrengthLimited`"))
    assert(interactionLaw.contains("Renderer and LLM wording remain"))
    assert(interactionLaw.contains("## Stage 4 Closeout"))
    assert(interactionLaw.contains("One chess meaning, one home. One rule, one live authority."))
    assert(interactionLaw.contains("Stage 4 ends with `Tactic.Hanging` as the only EngineCheck consumer."))
    assert(interactionLaw.contains("`EngineCheck` is not a Story writer and does not create public truth."))
    assert(interactionLaw.contains("`StoryInteractionLaw.md` owns this closeout; other live docs may summarize scope only."))
    assert(interactionLaw.contains("Stage 5 Story Order may receive internal EngineCheck diagnostics from selected"))
    assert(
      interactionLaw.replaceAll("\\s+", " ").contains(
        "Stage 5 must not open renderer, LLM, public route `200`, `Tactic.Fork`, `Scene.Material`, `Scene.Defense`, Plan, Strategy, engine PV commentary, or best-move explanation."
      )
    )
    assert(interactionLaw.contains("## Stage 5 Charter"))
    assert(interactionLaw.contains("Stage 5 name is `Story Order`."))
    assert(interactionLaw.contains("Stage 5 may be described as `StoryTable Arbitration`"))
    assert(interactionLaw.contains("Core sentence: StoryTable orders. It does not invent."))
    assert(interactionLaw.contains("Many Stories may exist. StoryTable chooses roles. No new chess meaning is"))
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
    assert(interactionLaw.contains("`StoryInteractionLaw.md` is the single live authority for the Stage 5 charter."))
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
    assert(interactionLaw.contains("Stage 5-3 goal: resolve close blocker relationships for Hanging Story rows"))
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
    assert(interactionLaw.contains("Stage 5-4 goal: keep StoryTable results from being mistaken for renderer or LLM input."))
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
      "Deterministic renderer, LLM narration, public route `200`, user-facing prose, pedagogy, new Story families, and engine explanation remain closed"
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
    assert(interactionLaw.contains("`allowedClaim` is a structured claim key, not a natural-language sentence."))
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
    assert(interactionLaw.contains("Core sentence: Explanation Plan bounds speech. Renderer only phrases it."))
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
      "`win material` wording is allowed only when `allowedClaim` is `CanWinPiece`",
      "Stage 7-3 completion standard: Renderer automatically refuses forbidden wording"
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
      assert(interactionLaw.contains(authorityAudit), s"Stage 7 closeout must pin authority audit: $authorityAudit")
    Vector(
      "- Support plans produce no text",
      "- Context plans produce no text",
      "- Blocked plans produce no text",
      "- capped plans produce no text",
      "- refuted plans produce no text",
      "- no-claim plans produce no text"
    ).foreach: negativeAudit =>
      assert(interactionLaw.contains(negativeAudit), s"Stage 7 closeout must pin negative audit: $negativeAudit")
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
      assert(interactionLaw.contains(apiCheck), s"Stage 8 production API closed check must be pinned: $apiCheck")
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
      assert(interactionLaw.contains(closeoutStandard), s"Stage 8 closeout standard must include: $closeoutStandard")
    assert(modelContract.contains("positive `CaptureResult`"))
    assert(modelContract.contains("EngineCheck is internal evidence only."))
    assert(modelContract.contains("It records same-board proof, checked move, engine line, reply line, eval before, eval after, depth or freshness, and missing evidence."))
    assert(modelContract.contains("EngineCheck does not create a Story, select a Story, rank a Story, write a Verdict, feed a renderer, or feed an LLM."))
    assert(modelContract.contains("EngineCheck.fromStory binds engine evidence to same-board BoardFacts, an existing Story route, and a same-board legal line."))
    assert(modelContract.contains("EngineCheckStatus has exactly `Unknown`, `Supports`, `Caps`, and `Refutes`."))
    assert(modelContract.contains("Only `Tactic.Hanging`, the narrow `Tactic.Fork` vertical slice, and the narrow `Scene.Material` writer may carry EngineCheck in this checkpoint."))
    assert(modelContract.contains("Eval collapse after capture or fork route may refute an existing EngineCheck only after same-board Story proof"))
    assert(modelContract.contains("Eval collapse cannot create a Story, public eval claim, or engine-authored explanation."))
    assert(modelContract.contains("Verdict carries `engineCheckStatus` and `engineStrengthLimited` as internal diagnostics only."))
    assert(modelContract.contains("`Verdict.values`, renderer, and LLM inputs must not consume EngineCheck diagnostics."))
    assert(modelContract.contains("`StoryInteractionLaw.md` owns the Stage 5 charter."))
    assert(modelContract.contains("Stage 5 Story Order baseline is limited to existing `Tactic.Hanging` Story"))
    assert(modelContract.contains("The current Fork-6 slice adds only deterministic ordering between"))
    assert(modelContract.contains("existing `Tactic.Hanging` rows and existing narrow `Tactic.Fork` rows"))
    assert(modelContract.contains("Material-3 adds only single-row StoryTable admission for proof-backed"))
    assert(modelContract.contains("Stage 5-1 may mark only the selected Lead row as `leadAllowed`"))
    assert(modelContract.contains("Stage 5-2 ordering may use role eligibility"))
    assert(modelContract.contains("It must not use raw engine eval, raw PV text, proofFailures text"))
    assert(modelContract.contains("Stage 5-3 conflict rules may block Hanging-shaped rows"))
    assert(modelContract.contains("Fork-6 role rules block refuted Fork, incomplete Fork, writerless Fork"))
    assert(modelContract.contains("Fork-7 opens only ExplanationPlan mapping for selected narrow `Tactic.Fork`"))
    assert(modelContract.contains("Fork allowed claim keys are `forks_two_targets` and"))
    assert(modelContract.contains("`attacks_two_targets`; the first emitted Fork claim key is"))
    assert(modelContract.contains("`forks_two_targets`. Fork plans may carry secondaryTarget"))
    assert(modelContract.contains("Support, Context, Blocked, capped, and engine-refuted Fork plans create no"))
    assert(
      modelContract
        .replaceAll("\\s+", " ")
        .contains("without opening Plan, Blunder, Defense, extra counterplay, or Strategy relations")
    )
    assert(modelContract.contains("Stage 5-4 Verdict Diagnostic Boundary is also owned by `StoryInteractionLaw.md`"))
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
    assert(agents.contains("`StoryInteractionLaw.md` is the single live authority for the Stage 3 charter."))
    assert(agents.contains("`StoryInteractionLaw.md` is the single live authority for the Stage 4 charter."))
    assert(agents.contains("`StoryInteractionLaw.md` is the single live authority for the Stage 5 charter."))
    assert(agents.contains("`StoryInteractionLaw.md` is the single live authority for the Stage 6 charter."))
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
    assert(agents.contains("Stage 5-1 Hanging role rules also live there"))
    assert(ssot.contains("Stage 5-1 Hanging role rules also live in `StoryInteractionLaw.md`"))
    assert(modelContract.contains("Stage 5-1 Hanging Role Rules are also owned by `StoryInteractionLaw.md`"))
    assert(manifest.contains("Stage 5-1 Hanging Role Rules also live in `StoryInteractionLaw.md`"))
    assert(agents.contains("Stage 5-2 deterministic ordering rules also live there"))
    assert(ssot.contains("Stage 5-2 deterministic ordering rules also live in `StoryInteractionLaw.md`"))
    assert(modelContract.contains("Stage 5-2 Deterministic Ordering is also owned by `StoryInteractionLaw.md`"))
    assert(manifest.contains("Stage 5-2 Deterministic Ordering also lives in `StoryInteractionLaw.md`"))
    assert(agents.contains("Stage 5-3 conflict and block rules also live there"))
    assert(ssot.contains("Stage 5-3 conflict and block rules also live in `StoryInteractionLaw.md`"))
    assert(modelContract.contains("Stage 5-3 Conflict and Block Rules are also owned by `StoryInteractionLaw.md`"))
    assert(manifest.contains("Stage 5-3 Conflict and Block Rules also live in `StoryInteractionLaw.md`"))
    assert(agents.contains("Stage 5-4 Verdict diagnostic boundary also lives there"))
    assert(ssot.contains("Stage 5-4 Verdict diagnostic boundary also lives in `StoryInteractionLaw.md`"))
    assert(modelContract.contains("Stage 5-4 Verdict Diagnostic Boundary is also owned by `StoryInteractionLaw.md`"))
    assert(manifest.contains("Stage 5-4 Verdict Diagnostic Boundary also lives in `StoryInteractionLaw.md`"))
    assert(agents.contains("Stage 5 closeout also lives there"))
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
    Vector(
      "backend Material proof evidence, the named `Tactic.Hanging` writer, the narrow `Tactic.Fork` proof/writer vertical slice, and the narrow `Scene.Material` writer",
      "`Scene.Defense`, Plan, Strategy, Fork LLM narration, public route `200`, and strong wording remain closed there"
    ).foreach: scopeLine =>
      assert(agents.replaceAll("\\s+", " ").contains(scopeLine), s"AGENTS must summarize Stage 3 scope: $scopeLine")
    assert(agents.replaceAll("\\s+", " ").contains("Stage 4 opens only `EngineCheck`, `EngineLine`, and `EngineEval` as internal evidence"))
    assert(agents.replaceAll("\\s+", " ").contains("Stage 4-2 adds same-board and stale engine guards"))
    assert(agents.replaceAll("\\s+", " ").contains("Stage 4-3 attaches EngineCheck first to `Tactic.Hanging`; Fork-5 reuses the same sidecar"))
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
      "StoryTable may order existing `Tactic.Hanging` Story rows, existing narrow `Tactic.Fork` Story rows, and existing proof-backed `Scene.Material` Story rows into roles",
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
    val agents = Files.readString(agentInstructions)
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
    assert(agents.contains("Only the named `Tactic.Hanging` writer, the narrow named `Tactic.Fork`"))
    assert(agents.contains("and the narrow named `Scene.Material` writer are live"))
    assert(agents.contains("Renderer boundary no-go"))
    assert(agents.contains("Forbidden-name no-go"))
    assert(agents.contains("proof-first chess-story kernel"))
    assert(agents.contains("`BoardMood` observes."))
    assert(agents.contains("feature is not a claim"))
    assert(agents.contains("public `Story` requires proof-bearing identity"))
    assert(agents.contains("Authority consolidation is mandatory"))
    assert(agents.contains("`One chess meaning, one home`"))
    assert(agents.contains("`One observation family, one owner`"))
    assert(agents.contains("`One public claim, one proof path`"))
    assert(agents.contains("New names are the last resort"))
    assert(agents.contains("Ask whether a feature can become a `Story` with side, target, anchor, route,"))
    assert(agents.contains("Player-facing move notation defaults to SAN."))
    assert(agents.contains("Renderer, LLM smoke, docs examples, and any downstream public speech surface must"))
    assert(agents.contains("phrase the selected route as SAN"))
    assert(agents.contains("SAN formats already-approved legal moves only."))
    assert(agents.contains("those marks do not create Story, Proof,"))
    assert(
      agents.contains(
        "`observation` -> `proof sidecar` -> `Story` -> `Verdict` -> `Explanation IR` -> Renderer -> LLM narration smoke"
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
    assert(interactionLaw.contains("`Tactic.Hanging` and the narrow `Tactic.Fork` vertical slice are the only live"))
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
    assert(interactionLaw.contains("`MultiTargetProof` is the family-specific proof home for the first Fork slice."))
    assert(interactionLaw.contains("It creates Fork evidence, but it does not directly create a public Story."))
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
    assert(interactionLaw.contains("`TacticFork` is the named positive writer for the first narrow `Tactic.Fork`"))
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
        "reply-map entries showing one reply can save both targets block Fork Lead."
          .toLowerCase
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
    assert(interactionLaw.contains("Fork-6 opens only StoryTable role ordering for existing `Tactic.Hanging` Story"))
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
    assert(interactionLaw.contains("Fork-7 opens only ExplanationPlan mapping for a selected narrow `Tactic.Fork`"))
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
    assert(interactionLaw.contains("Completion standard: Fork ExplanationPlan creates no meaning stronger than the"))
    assert(interactionLaw.contains("## Fork-8 Deterministic Renderer For Fork"))
    assert(interactionLaw.contains("Fork-8 opens only deterministic renderer text for a selected Fork"))
    assert(interactionLaw.contains("- ExplanationPlan only"))
    assert(interactionLaw.contains("`{route} forks the pieces on {targetA} and {targetB}.`"))
    assert(interactionLaw.contains("`targetA` and `targetB` must come from structured `target` and"))
    assert(interactionLaw.contains("- allowed claim is `forks_two_targets`"))
    assert(interactionLaw.contains("Support, Context, Blocked, capped, and engine-refuted Fork plans produce no"))
    assert(interactionLaw.contains("The `attacks_two_targets` claim key remains an internal allowed claim key"))
    assert(interactionLaw.contains("Fork-8 itself does not open Fork LLM smoke"))
    assert(interactionLaw.contains("Completion standard: Fork renderer text is no stronger than the selected Fork"))
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
    assert(interactionLaw.contains("Fork-9 opens only LLM smoke for selected Fork ExplanationPlan and RenderedLine."))
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
    assert(interactionLaw.contains("Completion standard: Fork LLM smoke does not strengthen Fork deterministic"))
    assert(normalizedModelContract.contains("Fork-9 opens only LLM smoke for selected Fork ExplanationPlan and RenderedLine."))
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
    assert(normalizedModelContract.contains("Material-0 and Material-1 are owned by `StoryInteractionLaw.md`."))
    assert(
      normalizedModelContract.contains(
        "Material-1 reuses `CaptureResult` for the first simple capture and immediate bounded recapture scope."
      )
    )
    assert(normalizedModelContract.contains("Material-2 extends `CaptureResult` with captured pieces and bounded exchange sequence proof fields."))
    assert(normalizedModelContract.contains("Material-3 opens `StoryWriter.SceneMaterial` as the named Material writer."))
    assert(normalizedModelContract.contains("Material-4 adds only the Material negative corpus."))
    assert(normalizedModelContract.contains("Material-5 reuses existing `EngineCheck` for `Scene.Material`."))
    assert(
      normalizedModelContract.contains(
        "Material-6 adds only StoryTable integration for existing Hanging, Fork, and Material rows."
      )
    )
    assert(normalizedModelContract.contains("Material-7 opens only ExplanationPlan mapping for selected `Scene.Material` Verdicts."))
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
    assert(normalizedModelContract.contains("Material-8 opens only deterministic renderer text for selected `Scene.Material` ExplanationPlan."))
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
    assert(normalizedModelContract.contains("Material-9 opens only LLM smoke for selected Material ExplanationPlan and RenderedLine."))
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
      assert(interactionLaw.contains(materialCloseoutLine), s"Material closeout must pin: $materialCloseoutLine")
    assert(normalizedModelContract.contains("Material Slice Closeout confirms `Scene.Material` opened no Defense, Conversion, Winning, Plan, Strategy, or Blunder path."))
    assertEquals(widthRows.size, 24)
    assertEquals(mappedTactics, Tactics.sorted)
    widthRows.foreach: row =>
      assertEquals(row.size, 9)
      assert(row(2).nonEmpty, s"width map must name proof shape for ${row(1)}")
      assert(row(3).contains("StoryProof"), s"width map must keep StoryProof reuse visible for ${row(1)}")
      assert(row(5).nonEmpty, s"width map must name false-positive risks for ${row(1)}")
      assert(row(6).contains("yes") || row(6).contains("attached"), s"width map must name EngineCheck need for ${row(1)}")
    Vector(
      "CaptureProof",
      "TargetProof",
      "LineProof",
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
    val agents = Files.readString(agentInstructions)
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

    Vector(readme, ssot, architecture, agents).foreach: doc =>
      assert(doc.contains("Defense-0 opened only the charter for the first narrow `Scene.Defense` slice."))
      assert(doc.contains("Defense requires a threat."))
      assert(doc.contains("ThreatProof proves what must be stopped."))
      assert(doc.contains("DefenseProof proves how the move stops it."))
      assert(doc.contains("Defense is not no-counterplay."))
    assert(normalizedModelContract.contains("Defense-0 is owned by `StoryInteractionLaw.md`."))
    assert(normalizedModelContract.contains("Defense-0 opens only the `Scene.Defense` charter."))
    assert(normalizedModelContract.contains("It opens no writer, proof sidecar, StoryTable integration, ExplanationPlan, renderer, LLM smoke, public route `200`, or production API."))

  test("Defense-1 ThreatProof pins what Defense must stop without creating Story"):
    val readme = Files.readString(docsRoot.resolve("README.md"))
    val ssot = Files.readString(docsRoot.resolve("ChessCommentarySSOT.md"))
    val architecture = Files.readString(docsRoot.resolve("ChessModelArchitecture.md"))
    val modelContract = Files.readString(docsRoot.resolve("ChessModelContract.md"))
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))
    val agents = Files.readString(agentInstructions)
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

    Vector(readme, ssot, architecture, agents).foreach: doc =>
      assert(doc.contains("Defense-1 opens only `ThreatProof`."))
      assert(doc.contains("ThreatProof proves what must be stopped."))
    assert(normalizedModelContract.contains("Defense-1 is owned by `StoryInteractionLaw.md`."))
    assert(normalizedModelContract.contains("Defense-1 opens only `ThreatProof`."))
    assert(normalizedModelContract.contains("ThreatProof proves what must be stopped, but it does not create a Defense Story or public claim."))

  test("Defense-2 DefenseProof pins how a specific move stops a ThreatProof"):
    val readme = Files.readString(docsRoot.resolve("README.md"))
    val ssot = Files.readString(docsRoot.resolve("ChessCommentarySSOT.md"))
    val architecture = Files.readString(docsRoot.resolve("ChessModelArchitecture.md"))
    val modelContract = Files.readString(docsRoot.resolve("ChessModelContract.md"))
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))
    val agents = Files.readString(agentInstructions)
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

    Vector(readme, ssot, architecture, agents).foreach: doc =>
      assert(doc.contains("Defense-2 opens only `DefenseProof`."))
      assert(doc.contains("DefenseProof proves how a specific move stops a specific ThreatProof."))
    assert(normalizedModelContract.contains("Defense-2 is owned by `StoryInteractionLaw.md`."))
    assert(normalizedModelContract.contains("Defense-2 opens only `DefenseProof`."))
    assert(normalizedModelContract.contains("DefenseProof proves whether a specific threat is stopped, but it does not create a Defense Story or public claim."))

  test("Defense-3 SceneDefense writer pins the first narrow Defense Story"):
    val readme = Files.readString(docsRoot.resolve("README.md"))
    val ssot = Files.readString(docsRoot.resolve("ChessCommentarySSOT.md"))
    val architecture = Files.readString(docsRoot.resolve("ChessModelArchitecture.md"))
    val modelContract = Files.readString(docsRoot.resolve("ChessModelContract.md"))
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))
    val agents = Files.readString(agentInstructions)
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

    Vector(readme, ssot, architecture, agents).foreach: doc =>
      assert(doc.contains("Defense-3 opens only the named `SceneDefense` writer for one narrow `Scene.Defense` Story."))
      assert(doc.contains("ThreatProof proves what must be stopped."))
      assert(doc.contains("DefenseProof proves how a specific move stops a specific ThreatProof."))
      assert(doc.contains("Public route `200`, production API, and public/user-facing LLM narration remain closed."))
    assert(normalizedModelContract.contains("Defense-3 is owned by `StoryInteractionLaw.md`."))
    assert(normalizedModelContract.contains("Defense-3 opens only the named `SceneDefense` writer for one narrow `Scene.Defense` Story."))
    assert(normalizedModelContract.contains("A SceneDefense Story requires complete StoryProof, complete ThreatProof, complete DefenseProof, same-board proof, legal defense move, identified protected target, prevented material loss, writer `SceneDefense`, and no refuting EngineCheck."))

  test("Defense-4 negative corpus pins defense-looking false positives silent"):
    val readme = Files.readString(docsRoot.resolve("README.md"))
    val ssot = Files.readString(docsRoot.resolve("ChessCommentarySSOT.md"))
    val architecture = Files.readString(docsRoot.resolve("ChessModelArchitecture.md"))
    val modelContract = Files.readString(docsRoot.resolve("ChessModelContract.md"))
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))
    val agents = Files.readString(agentInstructions)
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

    Vector(readme, ssot, architecture, agents).foreach: doc =>
      assert(doc.contains("Defense-4 opens only the Defense negative corpus."))
      assert(doc.contains("Defense-looking false positives must stay silent without complete ThreatProof and complete DefenseProof."))
      assert(doc.contains("Public route `200`, production API, and public/user-facing LLM narration remain closed."))
    assert(normalizedModelContract.contains("Defense-4 is owned by `StoryInteractionLaw.md`."))
    assert(normalizedModelContract.contains("Defense-4 opens only the Defense negative corpus."))
    assert(normalizedModelContract.contains("Defense-looking rows have no Lead or are Blocked unless ThreatProof and DefenseProof are complete."))

  test("Defense-5 EngineCheck reuse pins Defense engine evidence boundary"):
    val readme = Files.readString(docsRoot.resolve("README.md"))
    val ssot = Files.readString(docsRoot.resolve("ChessCommentarySSOT.md"))
    val architecture = Files.readString(docsRoot.resolve("ChessModelArchitecture.md"))
    val modelContract = Files.readString(docsRoot.resolve("ChessModelContract.md"))
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))
    val agents = Files.readString(agentInstructions)
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

    Vector(readme, ssot, architecture, agents).foreach: doc =>
      assert(doc.contains("Defense-5 opens only existing EngineCheck reuse for existing `Scene.Defense` Stories."))
      assert(doc.contains("EngineCheck may support, cap, or refute an existing Defense Story, but it does not create Defense."))
      assert(doc.contains("Public route `200`, production API, and public/user-facing LLM narration remain closed."))
    assert(normalizedModelContract.contains("Defense-5 is owned by `StoryInteractionLaw.md`."))
    assert(normalizedModelContract.contains("Defense-5 opens only existing EngineCheck reuse for existing `Scene.Defense` Stories."))
    assert(normalizedModelContract.contains("EngineCheck may support, cap, or refute an existing Defense Story, but it does not create Defense."))
    assert(!chessSources.contains("DefenseEngineCheck"))

  test("Defense-6 StoryTable integration pins four-family deterministic ordering"):
    val readme = Files.readString(docsRoot.resolve("README.md"))
    val ssot = Files.readString(docsRoot.resolve("ChessCommentarySSOT.md"))
    val architecture = Files.readString(docsRoot.resolve("ChessModelArchitecture.md"))
    val modelContract = Files.readString(docsRoot.resolve("ChessModelContract.md"))
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))
    val agents = Files.readString(agentInstructions)
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

    Vector(readme, ssot, architecture, agents).foreach: doc =>
      assert(doc.contains("Defense-6 opens only StoryTable integration for existing Hanging, Fork, Material, and Defense rows."))
      assert(doc.contains("StoryTable deterministically orders Hanging, Fork, Material, and Defense without creating new chess meaning."))
      assert(doc.contains("The completed Stage 8, Fork-9, Material Slice Closeout, Defense-0, Defense-1, Defense-2, Defense-3, Defense-4, Defense-5, Defense-6, Defense-7, Defense-8, and Defense-9 scopes remain closed baselines."))
    assert(normalizedModelContract.contains("Defense-6 is owned by `StoryInteractionLaw.md`."))
    assert(normalizedModelContract.contains("Defense-6 opens only StoryTable integration for existing Hanging, Fork, Material, and Defense rows."))
    assert(normalizedModelContract.contains("StoryTable deterministically orders Hanging, Fork, Material, and Defense without creating new chess meaning."))

  test("Defense-7 ExplanationPlan pins selected Defense Verdict speech boundary"):
    val readme = Files.readString(docsRoot.resolve("README.md"))
    val ssot = Files.readString(docsRoot.resolve("ChessCommentarySSOT.md"))
    val architecture = Files.readString(docsRoot.resolve("ChessModelArchitecture.md"))
    val modelContract = Files.readString(docsRoot.resolve("ChessModelContract.md"))
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))
    val agents = Files.readString(agentInstructions)
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

    Vector(readme, ssot, architecture, agents).foreach: doc =>
      assert(doc.contains("Defense-7 opens only ExplanationPlan mapping for selected `Scene.Defense` Verdicts."))
      assert(doc.contains("Defense ExplanationPlan creates no meaning stronger than the selected Verdict."))
      assert(doc.contains("The completed Stage 8, Fork-9, Material Slice Closeout, Defense-0, Defense-1, Defense-2, Defense-3, Defense-4, Defense-5, Defense-6, Defense-7, Defense-8, and Defense-9 scopes remain closed baselines."))
    assert(normalizedModelContract.contains("Defense-7 is owned by `StoryInteractionLaw.md`."))
    assert(normalizedModelContract.contains("Defense-7 opens only ExplanationPlan mapping for selected `Scene.Defense` Verdicts."))
    assert(normalizedModelContract.contains("Defense ExplanationPlan creates no meaning stronger than the selected Verdict."))

  test("Defense-8 deterministic renderer pins Defense text boundary"):
    val readme = Files.readString(docsRoot.resolve("README.md"))
    val ssot = Files.readString(docsRoot.resolve("ChessCommentarySSOT.md"))
    val architecture = Files.readString(docsRoot.resolve("ChessModelArchitecture.md"))
    val modelContract = Files.readString(docsRoot.resolve("ChessModelContract.md"))
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))
    val agents = Files.readString(agentInstructions)
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

    Vector(readme, ssot, architecture, agents).foreach: doc =>
      assert(doc.contains("Defense-8 opens only deterministic renderer text for selected Defense ExplanationPlan."))
      assert(doc.contains("Renderer text is no stronger than the Defense ExplanationPlan."))
      assert(doc.contains("The completed Stage 8, Fork-9, Material Slice Closeout, Defense-0, Defense-1, Defense-2, Defense-3, Defense-4, Defense-5, Defense-6, Defense-7, Defense-8, and Defense-9 scopes remain closed baselines."))
    assert(normalizedModelContract.contains("Defense-8 is owned by `StoryInteractionLaw.md`."))
    assert(normalizedModelContract.contains("Defense-8 opens only deterministic renderer text for selected Defense ExplanationPlan."))
    assert(normalizedModelContract.contains("Renderer text is no stronger than the Defense ExplanationPlan."))

  test("Defense-9 LLM smoke pins Defense rephrase boundary"):
    val readme = Files.readString(docsRoot.resolve("README.md"))
    val ssot = Files.readString(docsRoot.resolve("ChessCommentarySSOT.md"))
    val architecture = Files.readString(docsRoot.resolve("ChessModelArchitecture.md"))
    val modelContract = Files.readString(docsRoot.resolve("ChessModelContract.md"))
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))
    val agents = Files.readString(agentInstructions)
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

    Vector(readme, ssot, architecture, agents).foreach: doc =>
      assert(doc.contains("Defense-9 opens only LLM smoke for selected Defense ExplanationPlan and RenderedLine."))
      assert(doc.contains("LLM smoke does not make Defense text stronger."))
      assert(doc.contains("Public route `200`, production API, and public/user-facing LLM narration remain closed."))
    assert(normalizedModelContract.contains("Defense-9 is owned by `StoryInteractionLaw.md`."))
    assert(normalizedModelContract.contains("Defense-9 opens only LLM smoke for selected Defense ExplanationPlan and RenderedLine."))
    assert(normalizedModelContract.contains("LLM smoke does not make Defense text stronger."))

  test("Defense slice closeout pins audit and next-stage handoff"):
    val readme = Files.readString(docsRoot.resolve("README.md"))
    val ssot = Files.readString(docsRoot.resolve("ChessCommentarySSOT.md"))
    val architecture = Files.readString(docsRoot.resolve("ChessModelArchitecture.md"))
    val modelContract = Files.readString(docsRoot.resolve("ChessModelContract.md"))
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))
    val agents = Files.readString(agentInstructions)
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

    Vector(readme, ssot, architecture, agents).foreach: doc =>
      assert(doc.contains("Current implementation scope is Defense Slice Closeout Pass."))
      assert(doc.contains("Defense Slice Closeout opens no new chess meaning beyond the narrow `Scene.Defense` vertical slice."))
      assert(doc.contains("Defense closes as a narrow proof-backed attacked-piece material-loss defense slice only."))
      assert(doc.contains("Public route `200`, production API, and public/user-facing LLM narration remain closed."))
    assert(normalizedModelContract.contains("Defense Slice Closeout confirms `Scene.Defense` opened no King safety, Mate defense, Plan, Strategy, Counterplay, or Prophylaxis path."))
    assert(normalizedModelContract.contains("`ThreatProof`, `DefenseProof`, `Scene.Defense`, and `defends_piece` each keep one home."))

  test("agents and active frontend tests reject retired downstream authority"):
    assert(Files.exists(agentInstructions), "AGENTS.md must be available from the lila worktree")
    assert(Files.exists(commentaryBridgeTest), "commentaryBridge.test.ts must remain an active frontend test")

    val agents = Files.readString(agentInstructions)
    assert(!agents.contains("CommentaryOutline"), "AGENTS.md must not grant CommentaryOutline authority")
    assert(!agents.contains("CommentaryPlan"), "AGENTS.md must not grant CommentaryPlan authority")
    assert(agents.contains("selected `Verdict` data only"))

    val bridgeTest = Files.readString(commentaryBridgeTest)
    RetiredRootDocs.foreach: docName =>
      assert(
        !bridgeTest.contains(docName),
        s"active commentaryBridge.test.ts must not read retired doc $docName"
      )

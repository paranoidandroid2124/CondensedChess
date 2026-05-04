package lila.commentary.docs

import java.nio.file.{ Files, Paths }

import scala.jdk.CollectionConverters.*

class ChessDocsAuthorityTest extends munit.FunSuite:

  private val docsRoot = Paths.get("modules/commentary/docs")
  private val LiveDocs =
    Vector(
      "ChessCommentarySSOT.md",
      "ChessModelArchitecture.md",
      "ChessModelContract.md",
      "ChessResetRationale.md",
      "LegacyPruneManifest.md",
      "README.md"
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

  test("chess model branch exposes only live reset docs as root authority"):
    val liveDocs = rootDocNames

    assertEquals(liveDocs, LiveDocs)

    val oldLiveDocs =
      Set(
        "CommentaryCoreSSOT.md",
        "SemanticModelArchitecture.md",
        "LegacyArchiveIndex.md"
      )

    oldLiveDocs.foreach: fileName =>
      assert(!liveDocs.contains(fileName), s"$fileName must not be listed as root authority")
      assert(!Files.exists(docsRoot.resolve(fileName)), s"$fileName must not remain live")

  test("live authority is the chess model chain"):
    val readme = Files.readString(docsRoot.resolve("README.md"))
    val ssot = Files.readString(docsRoot.resolve("ChessCommentarySSOT.md"))
    val architecture = Files.readString(docsRoot.resolve("ChessModelArchitecture.md"))
    val modelContract = Files.readString(docsRoot.resolve("ChessModelContract.md"))
    val rationale = Files.readString(docsRoot.resolve("ChessResetRationale.md"))

    assert(readme.contains("`ChessCommentarySSOT.md`"))
    assert(readme.contains("`ChessModelArchitecture.md`"))
    assert(readme.contains("`ChessResetRationale.md`"))
    assert(!readme.contains("archived authority"))
    assert(ssot.contains("`BoardMood` -> `Story` -> `StoryTable` -> `Verdict`"))
    assert(ssot.contains("No other path owns current public chess meaning."))
    assert(!ssot.contains("CommentaryPipelineInput"))
    assert(!ssot.contains("candidate generation"))
    assert(!ssot.contains("semantic selector"))
    assert(architecture.contains("HCE-style deterministic chess scorer"))
    assert(architecture.contains("`BoardMood` -> `Story` -> `StoryTable` -> `Verdict`"))
    assert(architecture.contains("`48` bit slots"))
    assert(!architecture.contains("Historical selector-shaped scaffolds may remain"))
    assert(!architecture.contains("gate bits"))
    assert(modelContract.contains("Forbidden in new core model names"))
    assert(modelContract.contains("The model has exactly `32` long-term plan families"))
    assert(modelContract.contains("B00..B45 are packed `RootStateVector` transport words"))
    assert(modelContract.contains("BoardMood carries proof summaries, not public proof authority by itself"))
    assert(modelContract.contains("`BoardFacts` -> `BoardMood.fromFacts` is the runtime input boundary"))
    assert(modelContract.contains("`BoardMood.fromPieces` is scaffold-only and not runtime authority"))
    assert(modelContract.contains("BoardFacts required fields"))
    assert(modelContract.contains("Nested BoardFacts facts must be marked `known = true`"))
    assert(modelContract.contains("S015 `position_ready` may be `1` only when all nested facts are known and sane"))
    assert(modelContract.contains("B46 and B47 are legal destination summaries, not proof"))
    assert(modelContract.contains("Binding and proof slots remain zero unless later sidecars fill them"))
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

package lila.commentary.source.opening

import play.api.libs.json.Json

class OpeningSourceToolingTest extends munit.FunSuite:

  private val manifests = OpeningSourceCorpus.loadManifests()
  private val lines = OpeningSourceCorpus.loadLines()
  private val positions = OpeningSourceCorpus.loadPositions()
  private val moveStats = OpeningSourceCorpus.loadMoveStats()
  private val rejects = OpeningSourceCorpus.loadRejectFixtures()

  test("opening storage boundary is frozen by current worktree docs and ignore rules"):
    val storageDoc = OpeningSourceCorpus.readWorkspaceFile("modules/commentary/docs/OpeningSourceStorage.md")
    val gitignore = OpeningSourceCorpus.readWorkspaceFile(".gitignore")

    assert(
      storageDoc.contains("Raw source data and ECO-wide generated aggregates are local-only material."),
      "storage doc must freeze local-only bulk output"
    )
    assert(
      storageDoc.contains("Opening move candidates are statistics or references only."),
      "storage doc must freeze candidate authority"
    )
    assert(storageDoc.contains("OpeningIndexBuilder"), "storage doc must name the offline builder boundary")
    assert(gitignore.contains("/tmp/commentary-opening/"), "gitignore must block opening tmp output")
    assert(gitignore.contains("/modules/commentary/.local/opening/"), "gitignore must block opening local cache")
    assert(
      gitignore.contains("/modules/commentary/src/test/resources/commentary-corpus/generated-opening/"),
      "gitignore must block fixture-adjacent generated opening output"
    )

  test("opening source manifest validates license provenance and local-only storage policy"):
    val openingSources = manifests.filter(_.sourceFamily == "opening")
    assertEquals(openingSources.map(_.sourceId).toSet, Set("lichess-openings", "lichess-games"))

    openingSources.foreach: manifest =>
      assertEquals(OpeningContextValidator.validateManifest(manifest), Right(manifest))
      assertEquals(manifest.rawStoragePolicy, "externalOnly")
      assertEquals(manifest.generatedStoragePolicy, "localOnly")

    val missingLicense = reject("missingLicenseProvenance").payload
    val error = OpeningContextValidator.validateManifest(OpeningSourceManifest.fromJson(missingLicense)).left.getOrElse("")
    assert(error.contains("missing licenseName"), s"unexpected error: $error")

  test("opening line parser replays legal UCI move order and rejects illegal lines"):
    val catalan = lines.find(_.lineId == "eco-e04-catalan-open").getOrElse(fail("missing Catalan fixture"))
    val replay = OpeningLineReplay.replay(catalan.moveOrder).fold(fail, identity)

    assertEquals(replay.finalPly, 8)
    assertEquals(replay.finalPositionKey.value, "std:rnbqkb1r/ppp2ppp/4pn2/8/2pP4/6P1/PP2PPBP/RNBQK1NR w KQkq -")
    assertEquals(OpeningContextValidator.validateLine(catalan, manifests), Right(catalan))

    val illegalLine = OpeningLine.fromJson(reject("illegalMoveOrder").payload)
    val error = OpeningContextValidator.validateLine(illegalLine, manifests).left.getOrElse("")
    assert(error.contains("illegal move e2e5"), s"unexpected error: $error")

  test("opening position key normalization matches exact FEN and move-order replay"):
    val catalanPosition = positions.find(_.positionId == "open-pos-catalan-open-tabia").getOrElse(fail("missing position fixture"))
    val normalized = OpeningPositionKey.fromFen(catalanPosition.fen).fold(fail, identity)

    assertEquals(normalized, catalanPosition.positionKey)
    assertEquals(OpeningContextValidator.validatePosition(catalanPosition, lines, manifests), Right(catalanPosition))

    val nameOnly = OpeningPosition.fromJson(reject("missingPositionKey").payload)
    val error = OpeningContextValidator.validatePosition(nameOnly, lines, manifests).left.getOrElse("")
    assert(error.contains("must declare exact positionKey"), s"unexpected error: $error")

  test("opening index is transposition-safe and downgrades ambiguous transpositions"):
    val index = OpeningIndexBuilder.build(lines, positions, moveStats, manifests).fold(fail, identity)
    val key = OpeningPositionKey("std:rnbqkb1r/ppp2ppp/4pn2/8/2pP4/6P1/PP2PPBP/RNBQK1NR w KQkq -")

    assertEquals(
      index.positionsByKey(key).map(_.positionId),
      Vector("open-pos-catalan-open-tabia"),
      "only canonical indexed position should be indexed for the key"
    )
    assert(
      index.downgradedPositions.exists(_.positionId == "open-pos-transposed-downgraded"),
      "ambiguous transposition fixture should be downgraded"
    )
    assert(index.moveStatsByKey(key).map(_.move), Vector("d1a4", "g1f3"))

  test("opening move stats admit candidates only as statistics or references"):
    moveStats.foreach: stat =>
      assertEquals(OpeningContextValidator.validateMoveStat(stat, positions, manifests), Right(stat))
      assert(
        Set("statistical_reference", "context_reference").contains(stat.candidateKind),
        s"candidate role must stay reference-only for ${stat.statId}"
      )

    val bestCandidate = OpeningMoveStat.fromJson(reject("candidateTruthLeak").payload)
    val error = OpeningContextValidator.validateMoveStat(bestCandidate, positions, manifests).left.getOrElse("")
    assert(error.contains("must not claim best/theory/truth authority"), s"unexpected error: $error")

  test("unknown source is rejected before line or stat admission"):
    val unknownLine = OpeningLine.fromJson(reject("unknownSource").payload)
    val error = OpeningContextValidator.validateLine(unknownLine, manifests).left.getOrElse("")
    assert(error.contains("unknown sourceId missing-opening-source"), s"unexpected error: $error")

  test("opening builder refuses committed artifact output paths"):
    assert(
      OpeningIndexBuilder.validateOutputPath("tmp/commentary-opening/generated/lichess-openings/opening-positions.jsonl").isRight,
      "builder should allow ignored tmp generated output"
    )
    assert(
      OpeningIndexBuilder.validateOutputPath("modules/commentary/.local/opening/cache/index.jsonl").isRight,
      "builder should allow ignored local opening cache"
    )
    assert(
      OpeningIndexBuilder
        .validateOutputPath("modules/commentary/src/test/resources/commentary-corpus/generated-opening/sample.jsonl")
        .isRight,
      "builder should allow ignored fixture-adjacent generated output"
    )

    val committed = (reject("committedOutputPath").payload \ "path").as[String]
    val error = OpeningIndexBuilder.validateOutputPath(committed).left.getOrElse("")
    assert(error.contains("outside ignored opening local output roots"), s"unexpected error: $error")

  private def reject(kind: String): OpeningRejectFixture =
    rejects.find(_.rejectKind == kind).getOrElse(fail(s"missing reject fixture $kind"))

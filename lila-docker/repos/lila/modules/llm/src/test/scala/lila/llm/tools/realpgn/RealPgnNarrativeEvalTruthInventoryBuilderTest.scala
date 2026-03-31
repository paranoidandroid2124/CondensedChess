package lila.llm.tools.realpgn

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.time.Instant

import munit.FunSuite
import play.api.libs.json.Json

class RealPgnNarrativeEvalTruthInventoryBuilderTest extends FunSuite:

  private val positiveKey =
    RealPgnNarrativeEvalRunner.PositiveCompensationExemplars.toList.sorted.head
  private val positiveGameId =
    positiveKey.takeWhile(_ != ':')
  private val positivePly =
    positiveKey.dropWhile(_ != ':').drop(1).toInt

  private def sampleGame(id: String, family: String) =
    RealPgnNarrativeEvalRunner.CorpusGame(
      id = id,
      tier = "master_classical",
      family = family,
      label = id,
      notes = Nil,
      expectedThemes = List(family),
      pgn =
        """[Event "Sample"]
          |[Site "?"]
          |[Date "2026.03.25"]
          |[Round "1"]
          |[White "White"]
          |[Black "Black"]
          |[Result "*"]
          |
          |1. e4 e5 2. Nf3 Nc6 *
          |""".stripMargin
    )

  test("buildInventory keeps every qualifying problem move without caps and deduplicates repeated raw keys") {
    val sourceGames = List(
      sampleGame(positiveGameId, "positive"),
      sampleGame("classified_game", "classified"),
      sampleGame("investment_game", "investment"),
      sampleGame("practical_game", "practical"),
      sampleGame("quiet_game", "quiet")
    )
    val rawMoments = List(
      RealPgnNarrativeEvalTruthInventoryBuilder.RawMomentEntry(
        key = positiveKey,
        gameId = positiveGameId,
        ply = positivePly,
        moveNumber = (positivePly + 1) / 2,
        side = if positivePly % 2 == 1 then "white" else "black",
        momentType = "TensionPeak",
        moveClassification = None,
        cpLossVsPlayed = Some(12)
      ),
      RealPgnNarrativeEvalTruthInventoryBuilder.RawMomentEntry(
        key = positiveKey,
        gameId = positiveGameId,
        ply = positivePly,
        moveNumber = (positivePly + 1) / 2,
        side = if positivePly % 2 == 1 then "white" else "black",
        momentType = "TensionPeak",
        moveClassification = None,
        cpLossVsPlayed = Some(12)
      ),
      RealPgnNarrativeEvalTruthInventoryBuilder.RawMomentEntry(
        key = "classified_game:20",
        gameId = "classified_game",
        ply = 20,
        moveNumber = 10,
        side = "black",
        momentType = "AdvantageSwing",
        moveClassification = Some("Blunder"),
        cpLossVsPlayed = Some(22)
      ),
      RealPgnNarrativeEvalTruthInventoryBuilder.RawMomentEntry(
        key = "investment_game:30",
        gameId = "investment_game",
        ply = 30,
        moveNumber = 15,
        side = "black",
        momentType = "InvestmentPivot",
        moveClassification = None,
        cpLossVsPlayed = None
      ),
      RealPgnNarrativeEvalTruthInventoryBuilder.RawMomentEntry(
        key = "practical_game:40",
        gameId = "practical_game",
        ply = 40,
        moveNumber = 20,
        side = "black",
        momentType = "TensionPeak",
        moveClassification = None,
        cpLossVsPlayed = Some(95)
      ),
      RealPgnNarrativeEvalTruthInventoryBuilder.RawMomentEntry(
        key = "quiet_game:12",
        gameId = "quiet_game",
        ply = 12,
        moveNumber = 6,
        side = "black",
        momentType = "TensionPeak",
        moveClassification = None,
        cpLossVsPlayed = Some(18)
      )
    )

    val inventory =
      RealPgnNarrativeEvalTruthInventoryBuilder.buildInventory(
        sourceGames = sourceGames,
        rawMoments = rawMoments,
        generatedAt = Instant.parse("2026-03-25T00:00:00Z")
      )

    assertEquals(inventory.entries.count(_.key == positiveKey), 1)
    assert(inventory.entries.exists(_.key == "classified_game:20"))
    assert(inventory.entries.exists(_.key == "investment_game:30"))
    assert(inventory.entries.exists(_.key == "practical_game:40"))
    assert(!inventory.entries.exists(_.key == "quiet_game:12"))
    assert(
      inventory.entries.exists(entry =>
        entry.key == "practical_game:40" &&
          entry.tags.contains(RealPgnNarrativeEvalTruthInventoryBuilder.InventoryTag.HighCpLossUnclassified)
      ),
      clue(inventory.entries)
    )
    assertEquals(
      inventory.entries.count(_.tags.contains(RealPgnNarrativeEvalTruthInventoryBuilder.InventoryTag.NegativeGuard)),
      RealPgnNarrativeEvalRunner.NegativeGuards.size
    )
  }

  test("buildInventoryFromManifest reads array corpus files and raw game arcs without a separate exemplar corpus") {
    val root = Files.createTempDirectory("truth-inventory-builder")
    val runsRaw = root.resolve("runs").resolve("master_classical_000").resolve("raw")
    Files.createDirectories(runsRaw)
    Files.writeString(
      root.resolve("chronicle_corpus_master_only_140.json"),
      Json.prettyPrint(
        Json.arr(
          Json.obj(
            "version" -> 1,
            "generatedAt" -> "2026-03-25T00:00:00Z",
            "asOfDate" -> "2026-03-25",
            "title" -> "sample",
            "description" -> "sample",
            "games" ->
              Json.arr(
                Json.toJson(sampleGame("classified_game", "classified")),
                Json.toJson(sampleGame(positiveGameId, "positive"))
              )
          )
        )
      ),
      StandardCharsets.UTF_8
    )
    Files.writeString(
      runsRaw.resolve("classified_game.game_arc.json"),
      Json.prettyPrint(
        Json.obj(
          "moments" -> Json.arr(
            Json.obj(
              "ply" -> 20,
              "moveNumber" -> 10,
              "side" -> "black",
              "momentType" -> "AdvantageSwing",
              "moveClassification" -> "Blunder",
              "topEngineMove" -> Json.obj("cpLossVsPlayed" -> 120)
            )
          )
        )
      ),
      StandardCharsets.UTF_8
    )
    Files.writeString(
      runsRaw.resolve(s"$positiveGameId.game_arc.json"),
      Json.prettyPrint(
        Json.obj(
          "moments" -> Json.arr(
            Json.obj(
              "ply" -> positivePly,
              "moveNumber" -> ((positivePly + 1) / 2),
              "side" -> (if positivePly % 2 == 1 then "white" else "black"),
              "momentType" -> "TensionPeak",
              "moveClassification" -> play.api.libs.json.JsNull,
              "topEngineMove" -> Json.obj("cpLossVsPlayed" -> 10)
            )
          )
        )
      ),
      StandardCharsets.UTF_8
    )

    val inventory =
      RealPgnNarrativeEvalTruthInventoryBuilder.buildInventoryFromManifest(
        manifestRoot = root,
        generatedAt = Instant.parse("2026-03-25T00:00:00Z")
      )

    assert(inventory.entries.exists(_.key == positiveKey), clue(inventory.entries))
    assert(inventory.entries.exists(_.key == "classified_game:20"))
    assertEquals(
      inventory.summary.diagnosticTagCounts.getOrElse(
        RealPgnNarrativeEvalTruthInventoryBuilder.InventoryTag.PositiveExemplarGate,
        0
      ),
      1
    )
  }

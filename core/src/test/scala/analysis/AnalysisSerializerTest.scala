package chess
package analysis

import munit.FunSuite
import BookModel.*
import ConceptLabeler.*

import ujson.*

class AnalysisSerializerTest extends FunSuite:
  println("AnalysisSerializerTest instantiated")

  test("renderBook serializes all fields correctly") {
    val section1 = BookSection(
      // ... same
      sectionType = SectionType.OpeningPortrait,
      title = "The Opening",
      diagrams = List(
        BookDiagram(
          id = "d1",
          fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
          roles = List("White"),
          ply = 1,
          tags = TagBundle(
            structure = List(StructureTag.SpaceAdvantageWhite),
            plan = List(),
            tactic = List(),
            mistake = List(),
            endgame = List(),
            transition = List()
          )
        )
      ),
      narrativeHint = "This is a narrative hint.",
      startPly = 1,
      endPly = 10
    )

    val turningPoint = BookTurningPoint(
      ply = 15,
      side = "White",
      playedMove = "e4",
      bestMove = "d4",
      evalBefore = 50,
      evalAfterPlayed = -50,
      evalAfterBest = 60,
      mistakeTags = List(MistakeTag.TacticalMiss)
    )

    val book = Book(
      gameMeta = GameMeta("White", "Black", "1-0", Some("Italian Game")),
      sections = List(section1),
      turningPoints = List(turningPoint),
      tacticalMoments = List.empty,
      checklist = List.empty
    )

    val output = AnalyzePgn.Output(
      pgn = "",
      timeline = Vector.empty,
      summaryText = None,
      root = None,
      studyChapters = Vector.empty,
      opening = None,
      openingStats = None,
      oppositeColorBishops = false,
      openingSummary = None,
      bookExitComment = None,
      openingTrend = None,
      critical = Vector.empty,
      accuracyWhite = None,
      accuracyBlack = None,
      book = Some(book)
    )

    val jsonStr = AnalysisSerializer.render(output)
    
    // Basic String checks
    assert(jsonStr.contains("\"sectionType\":\"OpeningPortrait\""), "Missing sectionType")
    assert(jsonStr.contains("\"narrativeHint\":\"This is a narrative hint.\""), "Missing narrativeHint")
    assert(jsonStr.contains("\"SpaceAdvantageWhite\""), "Missing structure tag")
    assert(jsonStr.contains("\"TacticalMiss\""), "Missing mistake tag")
    assert(jsonStr.contains("\"openingName\":\"Italian Game\""), "Missing openingName")
    
    // Hardening checks
    assert(jsonStr.contains("\"schemaVersion\":3"), "Missing or incorrect schemaVersion")
    assert(jsonStr.contains("\"createdAt\""), "Missing createdAt")
    assert(jsonStr.contains("\"engineInfo\""), "Missing engineInfo")

    // Parsing check
    try
      val parsed = ujson.read(jsonStr)
      val bookObj = parsed("book")
      val sections = bookObj("sections").arr
      assertEquals(sections.length, 1)
      assertEquals(sections(0)("title").str, "The Opening")
      
      val turningPoints = bookObj("turningPoints").arr
      assertEquals(turningPoints.length, 1)
      assertEquals(turningPoints(0)("mistakeTags")(0).str, "TacticalMiss")

    catch
      case e: Throwable => fail(s"JSON parsing failed: ${e.getMessage}")
  }

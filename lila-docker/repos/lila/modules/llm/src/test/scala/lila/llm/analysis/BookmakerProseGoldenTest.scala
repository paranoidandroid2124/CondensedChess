package lila.llm.analysis

import java.nio.charset.StandardCharsets
import java.nio.file.{ Files, Path }

import munit.FunSuite

class BookmakerProseGoldenTest extends FunSuite:

  private val resourceRoot =
    Path.of("C:/Codes/CondensedChess/lila-docker/repos/lila/modules/llm/src/test/resources/bookmaker_thesis_goldens")

  private def normalize(text: String): String =
    text.replace("\r\n", "\n").trim

  private def readResource(name: String): String =
    Files.readString(resourceRoot.resolve(name), StandardCharsets.UTF_8)

  private def renderSlots(fixture: BookmakerProseGoldenFixtures.Fixture): String =
    val outline = BookStyleRenderer.validatedOutline(fixture.ctx)
    val slots = BookmakerPolishSlotsBuilder.build(fixture.ctx, outline, refs = None, strategyPack = fixture.strategyPack).get
    List(
      s"lens=${slots.lens}",
      s"claim=${slots.claim}",
      s"supportPrimary=${slots.supportPrimary.getOrElse("")}",
      s"supportSecondary=${slots.supportSecondary.getOrElse("")}",
      s"tension=${slots.tension.getOrElse("")}",
      s"evidenceHook=${slots.evidenceHook.getOrElse("")}",
      s"coda=${slots.coda.getOrElse("")}",
      s"paragraphPlan=${slots.paragraphPlan.mkString(",")}"
    ).mkString("\n")

  BookmakerProseGoldenFixtures.all.foreach { fixture =>
    test(s"${fixture.id} slots snapshot stays stable") {
      val actual = normalize(renderSlots(fixture))
      val expected = normalize(readResource(s"${fixture.id}.slots.txt"))
      assertEquals(actual, expected)
    }

    test(s"${fixture.id} draft snapshot stays stable") {
      val actual = normalize(BookStyleRenderer.renderDraft(fixture.ctx))
      val expected = normalize(readResource(s"${fixture.id}.draft.txt"))
      assertEquals(actual, expected)
    }

    test(s"${fixture.id} final snapshot stays stable") {
      val actual = normalize(BookStyleRenderer.render(fixture.ctx))
      val expected = normalize(readResource(s"${fixture.id}.final.txt"))
      assertEquals(actual, expected)
    }
  }

package lila.llm.tools

import java.nio.charset.StandardCharsets
import java.nio.file.{ Files, Path }

import lila.llm.analysis.{ BookStyleRenderer, BookmakerPolishSlotsBuilder, BookmakerProseGoldenFixtures }

object BookmakerProseGoldenDump:

  private val outDir =
    Path.of("modules/llm/src/test/resources/bookmaker_thesis_goldens")

  def main(args: Array[String]): Unit =
    Files.createDirectories(outDir)
    BookmakerProseGoldenFixtures.all.foreach { fixture =>
      val outline = BookStyleRenderer.validatedOutline(fixture.ctx)
      val slots = BookmakerPolishSlotsBuilder.build(fixture.ctx, outline, refs = None, strategyPack = fixture.strategyPack).get
      writeText(
        outDir.resolve(s"${fixture.id}.slots.txt"),
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
      )
      writeText(outDir.resolve(s"${fixture.id}.draft.txt"), BookStyleRenderer.renderDraft(fixture.ctx))
      writeText(outDir.resolve(s"${fixture.id}.final.txt"), BookStyleRenderer.render(fixture.ctx))
      println(s"[golden] wrote ${fixture.id}")
    }

  private def writeText(path: Path, text: String): Unit =
    Files.writeString(path, normalize(text) + "\n", StandardCharsets.UTF_8)

  private def normalize(text: String): String =
    text.replace("\r\n", "\n").trim

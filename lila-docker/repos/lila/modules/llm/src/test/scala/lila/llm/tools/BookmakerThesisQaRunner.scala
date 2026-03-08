package lila.llm.tools

import java.nio.charset.StandardCharsets
import java.nio.file.{ Files, Path }

import scala.concurrent.{ Await, ExecutionContext }
import scala.sys.process.*

import akka.actor.ActorSystem
import lila.llm.*
import lila.llm.analysis.*
import lila.llm.analysis.BookmakerProseGoldenFixtures.Fixture
import play.api.libs.ws.ahc.StandaloneAhcWSClient
import play.shaded.ahc.org.asynchttpclient.DefaultAsyncHttpClient

object BookmakerThesisQaRunner:

  private final case class StaticPromptMetrics(
      promptChars: Int,
      estimatedRequestTokens: Int
  )

  private final case class DirectPolishMetrics(
      provider: String,
      model: Option[String],
      parseWarnings: List[String],
      promptTokens: Option[Int],
      cachedTokens: Option[Int],
      completionTokens: Option[Int],
      estimatedCostUsd: Option[Double],
      rawPolished: String,
      softRepaired: String,
      softRepairApplied: Boolean
  )

  private final case class QaResult(
      fixture: Fixture,
      ruleCommentary: String,
      finalCommentary: String,
      evaluation: BookmakerProseContract.Evaluation,
      staticPrompt: StaticPromptMetrics,
      directPolish: Option[DirectPolishMetrics],
      rawEvaluation: Option[BookmakerProseContract.Evaluation]
  )

  private val repoRoot = Path.of(".")

  def main(args: Array[String]): Unit =
    val outPath = args.headOption.getOrElse("modules/llm/docs/BookmakerThesisQaReport.md")
    given Executor = ExecutionContext.global
    given ActorSystem = ActorSystem("bookmaker-thesis-qa")

    val fixtures = BookmakerProseGoldenFixtures.all
    val ws = new StandaloneAhcWSClient(new DefaultAsyncHttpClient())
    try
      val liveProviderAvailable =
        sys.env.get("LLM_PROVIDER").exists(v => v == "openai" || v == "gemini") &&
          (sys.env.get("OPENAI_API_KEY").exists(_.trim.nonEmpty) || sys.env.get("GEMINI_API_KEY").exists(_.trim.nonEmpty))

      val openAi = OpenAiClient(ws, OpenAiConfig.fromEnv)
      val gemini = GeminiClient(ws, GeminiConfig.fromEnv)

      val results = fixtures.map { fixture =>
        val ctx = fixture.ctx
        val outline = BookStyleRenderer.validatedOutline(ctx)
        val slots =
          BookmakerPolishSlotsBuilder.build(ctx, outline, refs = None)
            .getOrElse(sys.error(s"[${fixture.id}] failed to build bookmaker slots"))
        val ruleCommentary = BookStyleRenderer.renderValidatedOutline(outline, ctx)
        val prompt = PolishPrompt.buildPolishPrompt(
          prose = ruleCommentary,
          phase = normalizePhase(ctx.phase.current),
          evalDelta = None,
          concepts = ctx.semantic.map(_.conceptSummary).getOrElse(Nil),
          fen = ctx.fen,
          openingName = ctx.openingData.flatMap(_.name),
          bookmakerSlots = Some(slots)
        )
        val directPolish =
          if liveProviderAvailable then runDirectPolish(openAi, gemini, fixture, ruleCommentary, slots)
          else None
        val finalCommentary =
          directPolish.map(_.softRepaired).getOrElse(BookmakerSoftRepair.repair(ruleCommentary, slots).text)
        QaResult(
          fixture = fixture,
          ruleCommentary = ruleCommentary,
          finalCommentary = finalCommentary,
          evaluation = BookmakerProseContract.evaluate(finalCommentary, slots),
          staticPrompt = StaticPromptMetrics(
            promptChars = prompt.length,
            estimatedRequestTokens = PolishPrompt.estimateRequestTokens(prompt)
          ),
          directPolish = directPolish,
          rawEvaluation = directPolish.map(dp => BookmakerProseContract.evaluate(dp.rawPolished, slots))
        )
      }

      val systemPromptDelta = systemPromptDeltaFromHead()
      val report = renderReport(results, systemPromptDelta, liveProviderAvailable)
      Files.writeString(Path.of(outPath), report, StandardCharsets.UTF_8)
      println(s"[qa] wrote $outPath")
    finally
      ws.close()
      summon[ActorSystem].terminate()

  private def runDirectPolish(
      openAi: OpenAiClient,
      gemini: GeminiClient,
      fixture: Fixture,
      ruleCommentary: String,
      slots: BookmakerPolishSlots
  ): Option[DirectPolishMetrics] =
    val ctx = fixture.ctx
    val phase = normalizePhase(ctx.phase.current)
    val concepts = ctx.semantic.map(_.conceptSummary).getOrElse(Nil)
    sys.env.get("LLM_PROVIDER").map(_.trim.toLowerCase) match
      case Some("openai") =>
        Await.result(
          openAi.polishSync(
            prose = ruleCommentary,
            phase = phase,
            evalDelta = None,
            concepts = concepts,
            fen = ctx.fen,
            openingName = ctx.openingData.flatMap(_.name),
            bookmakerSlots = Some(slots)
          ),
          scala.concurrent.duration.Duration(120, "seconds")
        ).map { result =>
          val repaired = BookmakerSoftRepair.repair(result.commentary, slots)
          DirectPolishMetrics(
            provider = "openai",
            model = Some(result.model),
            parseWarnings = result.parseWarnings,
            promptTokens = result.promptTokens,
            cachedTokens = result.cachedTokens,
            completionTokens = result.completionTokens,
            estimatedCostUsd = result.estimatedCostUsd,
            rawPolished = result.commentary,
            softRepaired = repaired.text,
            softRepairApplied = repaired.applied
          )
        }
      case Some("gemini") =>
        Await.result(
          gemini.polish(
            prose = ruleCommentary,
            phase = phase,
            evalDelta = None,
            concepts = concepts,
            fen = ctx.fen,
            openingName = ctx.openingData.flatMap(_.name),
            bookmakerSlots = Some(slots)
          ),
          scala.concurrent.duration.Duration(120, "seconds")
        ).map { result =>
          val repaired = BookmakerSoftRepair.repair(result, slots)
          DirectPolishMetrics(
            provider = "gemini",
            model = sys.env.get("GEMINI_MODEL").filter(_.trim.nonEmpty),
            parseWarnings = Nil,
            promptTokens = None,
            cachedTokens = None,
            completionTokens = None,
            estimatedCostUsd = None,
            rawPolished = result,
            softRepaired = repaired.text,
            softRepairApplied = repaired.applied
          )
        }
      case _ => None

  private def normalizePhase(phase: String): String =
    Option(phase).map(_.trim.toLowerCase).filter(_.nonEmpty).getOrElse("middlegame")

  private def systemPromptDeltaFromHead(): Option[(Int, Int)] =
    val current = PolishPrompt.systemPrompt.length
    val stdout = new StringBuilder()
    val stderr = new StringBuilder()
    val spec = "HEAD:modules/llm/src/main/scala/lila/llm/PolishPrompt.scala"
    val exit =
      Process(Seq("git", "show", spec), repoRoot.toFile).!(
        ProcessLogger(out => stdout.append(out).append('\n'), err => stderr.append(err).append('\n'))
      )
    if exit != 0 then None
    else
      val pattern = """(?s)val systemPrompt: String =\s*\"\"\"(.*?)\"\"\"\.stripMargin""".r
      pattern.findFirstMatchIn(stdout.toString).map(m => m.group(1).length -> current)

  private def renderReport(
      results: List[QaResult],
      systemPromptDelta: Option[(Int, Int)],
      liveProviderAvailable: Boolean
  ): String =
    val avgPromptChars =
      if results.isEmpty then 0.0 else results.map(_.staticPrompt.promptChars.toDouble).sum / results.size.toDouble
    val avgRequestTokens =
      if results.isEmpty then 0.0 else results.map(_.staticPrompt.estimatedRequestTokens.toDouble).sum / results.size.toDouble
    val claimPass = results.count(_.evaluation.claimLikeFirstParagraph)
    val paragraphPass = results.count(_.evaluation.paragraphBudgetOk)
    val genericPass = results.count(_.evaluation.genericHits.isEmpty)
    val placeholderPass = results.count(_.evaluation.placeholderHits.isEmpty)
    val directPolish = results.flatMap(_.directPolish)
    val softRepairAppliedRate =
      if directPolish.isEmpty then None
      else Some(directPolish.count(_.softRepairApplied).toDouble / directPolish.size.toDouble)
    val acceptanceRatio =
      if !liveProviderAvailable then None
      else Some(directPolish.size.toDouble / results.size.toDouble)
    val fallbackRate =
      if !liveProviderAvailable then None
      else Some((results.size - directPolish.size).toDouble / results.size.toDouble)
    val avgCostUsd =
      if directPolish.flatMap(_.estimatedCostUsd).isEmpty then None
      else Some(directPolish.flatMap(_.estimatedCostUsd).sum / directPolish.flatMap(_.estimatedCostUsd).size.toDouble)

    val sb = new StringBuilder()
    sb.append("# Bookmaker Thesis QA Report\n\n")
    sb.append(s"- Snapshot date: 2026-03-08\n")
    sb.append(s"- Sample count: ${results.size}\n")
    sb.append(s"- Live polish env available: $liveProviderAvailable\n")
    sb.append("- Sample source: Bookmaker thesis motif fixtures\n\n")

    sb.append("## Static Prompt Envelope\n\n")
    sb.append(s"- `PolishPrompt.estimatedSystemTokens`: ${PolishPrompt.estimatedSystemTokens}\n")
    systemPromptDelta match
      case Some((before, after)) => sb.append(s"- System prompt chars: before=$before, after=$after, delta=${after - before}\n")
      case None                  => sb.append("- System prompt chars: HEAD comparison unavailable\n")
    sb.append(f"- Average per-request prompt chars: $avgPromptChars%.1f\n")
    sb.append(f"- Average estimated request tokens: $avgRequestTokens%.1f\n")
    sb.append("- Actual provider-side `prompt_tokens` / `estimatedCostUsd` are recorded when the active provider returns them.\n\n")

    sb.append("## Contract QA\n\n")
    sb.append(s"- Claim-like first paragraph: $claimPass/${results.size}\n")
    sb.append(s"- Paragraph budget (2-4): $paragraphPass/${results.size}\n")
    sb.append(s"- No banned generic phrase hits: $genericPass/${results.size}\n")
    sb.append(s"- No placeholder / authoring leakage hits: $placeholderPass/${results.size}\n")
    softRepairAppliedRate.foreach(rate => sb.append(f"- soft_repair_applied_rate: $rate%.3f\n"))
    sb.append("\n")

    results.foreach { result =>
      sb.append(s"### ${result.fixture.id}: ${result.fixture.title}\n\n")
      sb.append(s"- Motif: ${result.fixture.motif}\n")
      sb.append(s"- Expected lens: ${result.fixture.expectedLens}\n")
      sb.append(s"- Claim-like first paragraph: ${result.evaluation.claimLikeFirstParagraph}\n")
      sb.append(s"- Paragraphs: ${result.evaluation.paragraphs.size}\n")
      sb.append(s"- Generic hits: ${if result.evaluation.genericHits.isEmpty then "none" else result.evaluation.genericHits.mkString(", ")}\n")
      sb.append(s"- Placeholder hits: ${if result.evaluation.placeholderHits.isEmpty then "none" else result.evaluation.placeholderHits.mkString(", ")}\n")
      sb.append(s"- Static prompt chars: ${result.staticPrompt.promptChars}\n")
      sb.append(s"- Estimated request tokens: ${result.staticPrompt.estimatedRequestTokens}\n")
      result.directPolish match
        case Some(dp) =>
          sb.append(s"- Provider: ${dp.provider}\n")
          sb.append(s"- Model: ${dp.model.getOrElse("n/a")}\n")
          sb.append(s"- Parse warnings: ${if dp.parseWarnings.isEmpty then "none" else dp.parseWarnings.mkString(", ")}\n")
          sb.append(s"- Prompt tokens: ${dp.promptTokens.map(_.toString).getOrElse("n/a")}\n")
          sb.append(s"- Cached tokens: ${dp.cachedTokens.map(_.toString).getOrElse("n/a")}\n")
          sb.append(s"- Completion tokens: ${dp.completionTokens.map(_.toString).getOrElse("n/a")}\n")
          sb.append(s"- Estimated cost usd: ${dp.estimatedCostUsd.map(v => f"$v%.6f").getOrElse("n/a")}\n")
          sb.append(s"- Soft repair applied: ${dp.softRepairApplied}\n")
          result.rawEvaluation.foreach { raw =>
            sb.append(s"- Raw claim-like first paragraph: ${raw.claimLikeFirstParagraph}\n")
            sb.append(s"- Raw paragraph count: ${raw.paragraphs.size}\n")
          }
        case None =>
          sb.append("- Direct polish: unavailable\n")
      sb.append("\n#### Rule Draft\n\n```text\n")
      sb.append(result.ruleCommentary.trim)
      sb.append("\n```\n\n")
      result.directPolish.foreach { dp =>
        sb.append("#### Raw Polished Commentary\n\n```text\n")
        sb.append(dp.rawPolished.trim)
        sb.append("\n```\n\n")
      }
      sb.append("#### Final Commentary\n\n```text\n")
      sb.append(result.finalCommentary.trim)
      sb.append("\n```\n\n")
    }

    sb.append("## Live Polish Metrics\n\n")
    if !liveProviderAvailable then
      sb.append("- No live provider configured on this machine.\n")
    else
      sb.append(f"- polish_acceptance_ratio: ${acceptanceRatio.getOrElse(0.0)}%.3f\n")
      sb.append(f"- polish_fallback_rate: ${fallbackRate.getOrElse(0.0)}%.3f\n")
      sb.append(s"- avg_estimated_cost_usd: ${avgCostUsd.map(v => f"$v%.6f").getOrElse("n/a")}\n")
    sb.toString

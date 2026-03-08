package lila.llm.tools

import java.nio.charset.StandardCharsets
import java.nio.file.{ Files, Paths }

import scala.util.control.NonFatal

import play.api.libs.json.*

import lila.llm.analysis.{
  BookStyleRenderer,
  CommentaryEngine,
  NarrativeContextBuilder,
  NarrativeOutlineBuilder,
  NarrativeUtils,
  PlanEvidenceEvaluator,
  TraceRecorder
}
import lila.llm.analysis.ThemeTaxonomy.{ SubplanCatalog, SubplanId, ThemeL1, ThemeResolver }
import lila.llm.model.*
import lila.llm.model.strategic.VariationLine

object PlanHierarchyAuditRunner:

  final case class CorpusCase(
      id: String,
      analysisFen: Option[String],
      startFen: Option[String],
      preMovesUci: Option[List[String]],
      ply: Int,
      playedMove: String,
      opening: Option[String],
      phase: String,
      variations: List[VariationLine],
      probeResults: Option[List[ProbeResult]]
  )
  object CorpusCase:
    given Reads[CorpusCase] = Json.reads[CorpusCase]

  final case class Corpus(
      version: Int,
      cases: List[CorpusCase]
  )
  object Corpus:
    given Reads[Corpus] = Json.reads[Corpus]

  final case class Acc(
      cases: Int = 0,
      hypotheses: Int = 0,
      hypothesesWithSubplan: Int = 0,
      hypothesesUnknownTheme: Int = 0,
      hypothesesDefaultSubplan: Int = 0,
      hypothesesCompatPath: Int = 0,
      hypothesesEnsureIntervention: Int = 0,
      ensureScoreChanged: Int = 0,
      ensureRankChanged: Int = 0,
      mainPlans: Int = 0,
      mainPlansWithSubplan: Int = 0,
      mainPlansDefaultSubplan: Int = 0,
      fallbackEligibleCases: Int = 0,
      fallbackUsedCases: Int = 0,
      subplanSlotCases: Int = 0,
      subplanSlotTotal: Int = 0,
      themePlanValidationReqTotal: Int = 0,
      themePlanValidationReqValid: Int = 0,
      evaluatedTotal: Int = 0,
      l1l2Mismatch: Int = 0,
      subplanSignalsMissing: Int = 0,
      abstractFlankInfrastructureMentions: Int = 0,
      concreteRookPawnMentions: Int = 0,
      concreteHookMentions: Int = 0,
      concreteRookLiftMentions: Int = 0
  )

  def main(args: Array[String]): Unit =
    val corpusPath = args.headOption.getOrElse("modules/llm/docs/BookCommentaryCorpus.json")
    val corpus =
      readCorpus(corpusPath).getOrElse {
        System.err.println(s"[audit] failed to read corpus: $corpusPath")
        sys.exit(1)
      }

    val fallbackEnabled = boolEnv("LLM_LEGACY_STRATEGIC_TEXT_FALLBACK", default = false)
    var acc = Acc()
    val missingSignalsBySubplan = scala.collection.mutable.Map.empty[String, Int].withDefaultValue(0)
    val subplanDistribution = scala.collection.mutable.Map.empty[String, Int].withDefaultValue(0)
    val subplanSlotDistribution = scala.collection.mutable.Map.empty[String, Int].withDefaultValue(0)

    corpus.cases.foreach { c =>
      val fenOpt = c.analysisFen.orElse {
        for
          s <- c.startFen
          pm <- c.preMovesUci
        yield NarrativeUtils.uciListToFen(s, pm)
      }
      fenOpt.foreach { fen =>
        if c.variations.nonEmpty then
          try
            CommentaryEngine.assessExtended(
              fen = fen,
              variations = c.variations,
              playedMove = Some(c.playedMove),
              opening = c.opening,
              phase = Some(c.phase),
              ply = c.ply,
              prevMove = Some(c.playedMove),
              probeResults = c.probeResults.getOrElse(Nil)
            ).foreach { data =>
              val ctx =
                NarrativeContextBuilder.build(
                  data = data,
                  ctx = data.toContext,
                  prevAnalysis = None,
                  probeResults = c.probeResults.getOrElse(Nil),
                  openingRef = None,
                  prevOpeningRef = None,
                  openingBudget = OpeningEventBudget(),
                  afterAnalysis = None,
                  renderMode = NarrativeRenderMode.Bookmaker
                )

              val rec = new TraceRecorder()
              val (outline, _) = NarrativeOutlineBuilder.build(ctx, rec)
              val prose = BookStyleRenderer.render(ctx).toLowerCase
              val slotIds =
                outline.beats
                  .flatMap(_.conceptIds)
                  .collect { case cid if cid.startsWith("subplan_slot:") => cid.stripPrefix("subplan_slot:").trim }
                  .filter(id => id.nonEmpty && id != "none")
                  .distinct

              val validation = PlanEvidenceEvaluator.validateProbeResults(c.probeResults.getOrElse(Nil), ctx.probeRequests)
              val partition =
                PlanEvidenceEvaluator.partition(
                  hypotheses = data.planHypotheses,
                  probeRequests = ctx.probeRequests,
                  validatedProbeResults = validation.validResults,
                  rulePlanIds = data.plans.map(_.plan.id.toString.toLowerCase).toSet,
                  isWhiteToMove = data.isWhiteToMove,
                  droppedProbeCount = validation.droppedCount,
                  droppedProbeReasons = validation.droppedReasons,
                  invalidByRequestId = validation.invalidByRequestId
                )

              val hyps = data.planHypotheses
              val withSubplan = hyps.count(_.subplanId.nonEmpty)
              val unknownTheme = hyps.count(h => normalizeThemeId(h.themeL1) == ThemeL1.Unknown.id)
              val defaultSubplan =
                hyps.count { h =>
                  val theme = resolvedTheme(h)
                  h.subplanId
                    .flatMap(SubplanId.fromId)
                    .map(_.id)
                    .exists(id => ThemeResolver.defaultSubplanForTheme(theme).exists(_.id == id))
                }
              val compatPath =
                hyps.count { h =>
                  val rawThemeUnknown = normalizeThemeId(h.themeL1) == ThemeL1.Unknown.id
                  val rawSubplanMissing = h.subplanId.isEmpty
                  val resolvedThemeKnown = resolvedTheme(h) != ThemeL1.Unknown
                  val resolvedSubplanKnown = resolvedSubplan(h).nonEmpty
                  (rawThemeUnknown || rawSubplanMissing) && resolvedThemeKnown && resolvedSubplanKnown
                }
              val ensureIntervention =
                hyps.count { h =>
                  val themeChanged = normalizeThemeId(h.themeL1) != resolvedTheme(h).id
                  val subplanChanged = normalizeSubplanId(h.subplanId) != resolvedSubplan(h)
                  themeChanged || subplanChanged
                }

              val mainPlans = ctx.mainStrategicPlans
              val mainWithSubplan = mainPlans.count(_.subplanId.nonEmpty)
              val mainDefaultSubplan =
                mainPlans.count { h =>
                  val theme = resolvedTheme(h)
                  h.subplanId
                    .flatMap(SubplanId.fromId)
                    .map(_.id)
                    .exists(id => ThemeResolver.defaultSubplanForTheme(theme).exists(_.id == id))
                }

              val fallbackEligible = ctx.mainStrategicPlans.isEmpty && ctx.plans.top5.nonEmpty
              val fallbackUsed = fallbackEnabled && fallbackEligible

              val themeValidationReqs = ctx.probeRequests.filter(_.purpose.contains("theme_plan_validation"))
              val validIds = validation.validResults.map(_.id).toSet
              val themeValidationValid = themeValidationReqs.count(req => validIds.contains(req.id))

              val evaluated = partition.evaluated
              val l1l2MismatchCount =
                evaluated.count { ep =>
                  ep.subplanId
                    .flatMap(SubplanId.fromId)
                    .exists(_.theme.id != normalizeThemeId(ep.themeL1))
                }

              val subplanSignalsMissingCases =
                evaluated.filter { ep =>
                  val linkedReqs = ctx.probeRequests.filter(req => requestMatches(req, ep.hypothesis))
                  val required =
                    ep.subplanId
                      .flatMap(SubplanId.fromId)
                      .flatMap(SubplanCatalog.specs.get)
                      .map(_.requiredSignals.toSet)
                      .getOrElse(Set.empty[String])
                  required.nonEmpty && !linkedReqs.exists(req => required.subsetOf(req.requiredSignals.toSet))
                }

              subplanSignalsMissingCases.foreach { ep =>
                val sid = ep.subplanId.getOrElse("none")
                val linkedReqs = ctx.probeRequests.filter(req => requestMatches(req, ep.hypothesis))
                val required =
                  ep.subplanId
                    .flatMap(SubplanId.fromId)
                    .flatMap(SubplanCatalog.specs.get)
                    .map(_.requiredSignals.toSet)
                    .getOrElse(Set.empty[String])
                val present = linkedReqs.flatMap(_.requiredSignals).toSet
                (required -- present).foreach { sig =>
                  missingSignalsBySubplan.update(s"$sid::$sig", missingSignalsBySubplan(s"$sid::$sig") + 1)
                }
              }

              hyps.foreach { h =>
                h.subplanId.foreach { sid =>
                  subplanDistribution.update(sid, subplanDistribution(sid) + 1)
                }
              }
              slotIds.foreach { sid =>
                subplanSlotDistribution.update(sid, subplanSlotDistribution(sid) + 1)
              }

              acc = acc.copy(
                cases = acc.cases + 1,
                hypotheses = acc.hypotheses + hyps.size,
                hypothesesWithSubplan = acc.hypothesesWithSubplan + withSubplan,
                hypothesesUnknownTheme = acc.hypothesesUnknownTheme + unknownTheme,
                hypothesesDefaultSubplan = acc.hypothesesDefaultSubplan + defaultSubplan,
                hypothesesCompatPath = acc.hypothesesCompatPath + compatPath,
                hypothesesEnsureIntervention = acc.hypothesesEnsureIntervention + ensureIntervention,
                ensureScoreChanged = acc.ensureScoreChanged,
                ensureRankChanged = acc.ensureRankChanged,
                mainPlans = acc.mainPlans + mainPlans.size,
                mainPlansWithSubplan = acc.mainPlansWithSubplan + mainWithSubplan,
                mainPlansDefaultSubplan = acc.mainPlansDefaultSubplan + mainDefaultSubplan,
                fallbackEligibleCases = acc.fallbackEligibleCases + (if fallbackEligible then 1 else 0),
                fallbackUsedCases = acc.fallbackUsedCases + (if fallbackUsed then 1 else 0),
                subplanSlotCases = acc.subplanSlotCases + (if slotIds.nonEmpty then 1 else 0),
                subplanSlotTotal = acc.subplanSlotTotal + slotIds.size,
                themePlanValidationReqTotal = acc.themePlanValidationReqTotal + themeValidationReqs.size,
                themePlanValidationReqValid = acc.themePlanValidationReqValid + themeValidationValid,
                evaluatedTotal = acc.evaluatedTotal + evaluated.size,
                l1l2Mismatch = acc.l1l2Mismatch + l1l2MismatchCount,
                subplanSignalsMissing = acc.subplanSignalsMissing + subplanSignalsMissingCases.size,
                abstractFlankInfrastructureMentions =
                  acc.abstractFlankInfrastructureMentions + occurrencesOf(prose, "flank infrastructure"),
                concreteRookPawnMentions =
                  acc.concreteRookPawnMentions + occurrencesOf(prose, "rook-pawn march"),
                concreteHookMentions =
                  acc.concreteHookMentions + occurrencesOf(prose, "hook creation"),
                concreteRookLiftMentions =
                  acc.concreteRookLiftMentions + occurrencesOf(prose, "rook-lift scaffold")
              )
            }
          catch
            case NonFatal(_) => ()
      }
    }

    val concreteTotal =
      acc.concreteRookPawnMentions + acc.concreteHookMentions + acc.concreteRookLiftMentions

    println("# Plan Hierarchy Audit")
    println(s"- cases=${
      acc.cases
    }")
    println(
      f"- subplan fill (hypotheses): ${acc.hypothesesWithSubplan}/${acc.hypotheses} (${ratio(acc.hypothesesWithSubplan, acc.hypotheses)}%.3f)"
    )
    println(
      f"- subplan fill (main plans): ${acc.mainPlansWithSubplan}/${acc.mainPlans} (${ratio(acc.mainPlansWithSubplan, acc.mainPlans)}%.3f)"
    )
    println(
      f"- defaultSubplan ratio (hypotheses): ${acc.hypothesesDefaultSubplan}/${acc.hypothesesWithSubplan} (${ratio(acc.hypothesesDefaultSubplan, acc.hypothesesWithSubplan)}%.3f)"
    )
    println(
      f"- defaultSubplan ratio (main plans): ${acc.mainPlansDefaultSubplan}/${acc.mainPlansWithSubplan} (${ratio(acc.mainPlansDefaultSubplan, acc.mainPlansWithSubplan)}%.3f)"
    )
    println(
      f"- unknown theme ratio: ${acc.hypothesesUnknownTheme}/${acc.hypotheses} (${ratio(acc.hypothesesUnknownTheme, acc.hypotheses)}%.3f)"
    )
    println(
      f"- compat path ratio: ${acc.hypothesesCompatPath}/${acc.hypotheses} (${ratio(acc.hypothesesCompatPath, acc.hypotheses)}%.3f)"
    )
    println(
      f"- fallback ratio (used/eligible/all): ${acc.fallbackUsedCases}/${acc.fallbackEligibleCases}/${acc.cases} (${ratio(acc.fallbackUsedCases, acc.cases)}%.3f)"
    )
    println(
      f"- subplan_slot usage (cases): ${acc.subplanSlotCases}/${acc.cases} (${ratio(acc.subplanSlotCases, acc.cases)}%.3f)"
    )
    println(
      f"- subplan_slot avg per case: ${ratio(acc.subplanSlotTotal, acc.cases)}%.3f"
    )
    println(
      f"- phrase concretization (abstract vs concrete): abstract_flank_infrastructure=${acc.abstractFlankInfrastructureMentions}, concrete_total=${concreteTotal}, rook_pawn=${acc.concreteRookPawnMentions}, hook_creation=${acc.concreteHookMentions}, rook_lift_scaffold=${acc.concreteRookLiftMentions}"
    )
    println(
      f"- theme_plan_validation hit-rate: ${acc.themePlanValidationReqValid}/${acc.themePlanValidationReqTotal} (${ratio(acc.themePlanValidationReqValid, acc.themePlanValidationReqTotal)}%.3f)"
    )
    println(
      f"- subplanSignalsMissing ratio: ${acc.subplanSignalsMissing}/${acc.evaluatedTotal} (${ratio(acc.subplanSignalsMissing, acc.evaluatedTotal)}%.3f)"
    )
    println(
      f"- L1/L2 mismatch ratio: ${acc.l1l2Mismatch}/${acc.evaluatedTotal} (${ratio(acc.l1l2Mismatch, acc.evaluatedTotal)}%.3f)"
    )
    println(
      f"- ensureHierarchy intervention ratio: ${acc.hypothesesEnsureIntervention}/${acc.hypotheses} (${ratio(acc.hypothesesEnsureIntervention, acc.hypotheses)}%.3f)"
    )
    println(
      f"- ensureHierarchy score/rank changed: score=${acc.ensureScoreChanged}, rank=${acc.ensureRankChanged}"
    )

    if subplanDistribution.nonEmpty then
      println("- subplan distribution (hypotheses):")
      subplanDistribution.toList.sortBy((_, n) => -n).take(20).foreach { case (sid, n) =>
        println(s"  - $sid=$n")
      }
    if subplanSlotDistribution.nonEmpty then
      println("- subplan_slot distribution (outline):")
      subplanSlotDistribution.toList.sortBy((_, n) => -n).take(20).foreach { case (sid, n) =>
        println(s"  - $sid=$n")
      }
    if missingSignalsBySubplan.nonEmpty then
      println("- requiredSignals missing by subplan:")
      missingSignalsBySubplan.toList.sortBy((_, n) => -n).take(30).foreach { case (k, n) =>
        println(s"  - $k=$n")
      }

  private def readCorpus(path: String): Option[Corpus] =
    try
      val raw = Files.readString(Paths.get(path), StandardCharsets.UTF_8)
      Json.parse(raw).validate[Corpus].asOpt
    catch
      case NonFatal(_) => None

  private def occurrencesOf(text: String, needle: String): Int =
    if needle.isEmpty then 0
    else text.sliding(needle.length).count(_ == needle)

  private def ratio(n: Int, d: Int): Double =
    if d <= 0 then 0.0 else n.toDouble / d.toDouble

  private def normalizeThemeId(raw: String): String =
    ThemeL1.fromId(raw).map(_.id).getOrElse(ThemeL1.Unknown.id)

  private def normalizeSubplanId(raw: Option[String]): Option[String] =
    raw.flatMap(SubplanId.fromId).map(_.id)

  private def resolvedTheme(h: lila.llm.model.authoring.PlanHypothesis): ThemeL1 =
    ThemeL1.fromId(h.themeL1).filter(_ != ThemeL1.Unknown).getOrElse(ThemeResolver.fromHypothesis(h))

  private def resolvedSubplan(h: lila.llm.model.authoring.PlanHypothesis): Option[String] =
    h.subplanId
      .flatMap(SubplanId.fromId)
      .map(_.id)
      .orElse(ThemeResolver.subplanFromHypothesis(h).map(_.id))
      .orElse(ThemeResolver.defaultSubplanForTheme(resolvedTheme(h)).map(_.id))

  private def requestMatches(
      req: ProbeRequest,
      h: lila.llm.model.authoring.PlanHypothesis
  ): Boolean =
    val reqPlanId = req.planId.map(_.trim.toLowerCase)
    val reqPlanName = req.planName.map(normalizeText)
    val reqSeed = req.seedId.map(_.trim.toLowerCase)
    val hypPlanId = h.planId.trim.toLowerCase
    val hypPlanName = normalizeText(h.planName)
    val hypSeed =
      h.evidenceSources
        .collectFirst { case src if src.startsWith("latent_seed:") => src.stripPrefix("latent_seed:").trim.toLowerCase }
    val reqTheme =
      req.planId
        .map(pid => ThemeResolver.fromPlanId(pid).id)
        .filter(_ != ThemeL1.Unknown.id)
        .orElse(req.planName.map(name => ThemeResolver.fromPlanName(name).id).filter(_ != ThemeL1.Unknown.id))
        .getOrElse("")
    val hypTheme = resolvedTheme(h).id
    val reqSubplan =
      req.planName
        .flatMap(explicitSubplanFromPlanName)
        .orElse(req.planName.flatMap(name => ThemeResolver.subplanFromPlanName(name).map(_.id)))
        .orElse(req.seedId.flatMap(seed => ThemeResolver.subplanFromSeedId(seed).map(_.id)))
        .orElse(req.planId.flatMap(pid => ThemeResolver.subplanFromPlanId(pid).map(_.id)))
    val hypSubplan = resolvedSubplan(h)
    val themeAligned = reqTheme.nonEmpty && reqTheme == hypTheme
    val subplanAligned = reqSubplan.forall(rsp => hypSubplan.contains(rsp))
    val contractCompatible = requestContractCompatible(req, hypSubplan)
    reqPlanId.contains(hypPlanId) ||
    reqSeed.exists(seed => hypSeed.contains(seed)) ||
    reqPlanName.exists { pn =>
      pn.contains(hypPlanName) || hypPlanName.contains(pn) || pn.contains(hypPlanId)
    } ||
    (themeAligned && (subplanAligned || contractCompatible))

  private def normalizeText(raw: String): String =
    raw.toLowerCase.filter(_.isLetterOrDigit)

  private def explicitSubplanFromPlanName(raw: String): Option[String] =
    val marker = "subplan:"
    val low = Option(raw).getOrElse("").trim.toLowerCase
    val idx = low.indexOf(marker)
    if idx < 0 then None
    else
      val token =
        low
          .substring(idx + marker.length)
          .takeWhile(ch => ch.isLetterOrDigit || ch == '_' || ch == '-')
          .trim
      SubplanId.fromId(token).map(_.id)

  private def requestContractCompatible(req: ProbeRequest, hypSubplan: Option[String]): Boolean =
    if !req.purpose.contains("theme_plan_validation") then false
    else
      val required =
        hypSubplan
          .flatMap(SubplanId.fromId)
          .flatMap(SubplanCatalog.specs.get)
          .map(_.requiredSignals.toSet)
          .getOrElse(Set.empty[String])
      required.nonEmpty && required.subsetOf(req.requiredSignals.toSet)

  private def boolEnv(name: String, default: Boolean): Boolean =
    sys.env
      .get(name)
      .map(_.trim.toLowerCase)
      .flatMap {
        case "1" | "true" | "yes" | "on"  => Some(true)
        case "0" | "false" | "no" | "off" => Some(false)
        case _                              => None
      }
      .getOrElse(default)

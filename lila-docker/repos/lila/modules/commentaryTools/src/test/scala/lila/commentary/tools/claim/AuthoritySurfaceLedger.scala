package lila.commentary.tools.claim

import lila.commentary.analysis.*
import lila.commentary.analysis.claim.*
import java.nio.charset.StandardCharsets
import java.nio.file.{ Files, Paths }

import lila.commentary.{ DirectionalTargetReadiness, NarrativeSignalDigest, StrategyDirectionalTarget, StrategyPack }
import lila.commentary.model.*
import lila.commentary.model.authoring.{ AuthorQuestion, AuthorQuestionKind, PlanHypothesis, PlanViability, QuestionEvidence }
import lila.commentary.model.strategic.{ EngineEvidence, VariationLine }

object AuthoritySurfaceLedger:

  private[commentary] final case class Sample(
      id: String,
      fixtureId: String,
      reviewGroup: String,
      note: String,
      tacticalContract: Boolean = false,
      softenOwnerPath: Boolean = false,
      taxonomy: String = "-"
  )

  private[commentary] final case class Observation(
      sample: Sample,
      release: String,
      taxonomy: String,
      plannerOwner: String,
      primary: String,
      moveReview: String,
      leak: Boolean,
      rejected: String,
      contractId: String = "-",
      contractStatus: String = "-",
      contractFailures: String = "-"
  ):
    def tsv: String =
      List(
        sample.id,
        sample.reviewGroup,
        release,
        taxonomy,
        plannerOwner,
        clean(primary),
        clean(moveReview),
        leak.toString,
        clean(rejected),
        contractId,
        contractStatus,
        clean(contractFailures),
        clean(sample.note)
      ).mkString("\t")

  private[commentary] final case class OutputPaths(
      matrix: java.nio.file.Path,
      review: java.nio.file.Path
  )

  private val curatedSamples =
    List(
      Sample("B15A-certified-carlsbad", "B15A", "B:fixed_target", "Carlsbad fixed-chain pressure exact control.", taxonomy = "certified_owner_path"),
      Sample("B16B-certified-carlsbad", "B16B", "B:fixed_target", "Trade-resistance pressure exact control.", taxonomy = "certified_owner_path"),
      Sample("K09A-certified-coordination", "K09A", "B:target_coordination", "Preparatory coordination exact control.", taxonomy = "certified_owner_path"),
      Sample("K09D-certified-coordination", "K09D", "B:target_coordination", "Coordination blocker still owns the target-focused probe.", taxonomy = "certified_owner_path"),
      Sample("B21-certified-target-delta", "B21", "B:target_fixation_delta", "Move-local exact target fixation.", taxonomy = "certified_owner_path"),
      Sample("B21A-certified-target-delta", "B21A", "B:target_fixation_delta", "Move-local exact target fixation follow-up.", taxonomy = "certified_owner_path"),
      Sample("K09B-certified-simplification", "K09B", "C:simplification", "Same-task favorable simplification exact control.", taxonomy = "certified_owner_path"),
      Sample("K09F-certified-simplification", "K09F", "C:simplification", "Holdable simplification breadth control.", taxonomy = "certified_owner_path"),
      Sample("K03A-suppressed-carlsbad", "K03A", "B:blocker", "Carlsbad sibling row stays closed.", taxonomy = "position_probe_not_certified"),
      Sample("K08A-suppressed-trade-candidate", "K08A", "C:blocker", "Attacking-piece trade candidate stays closed.", taxonomy = "attacking_piece_trade_unowned"),
      Sample("K08D-suppressed-trade-near-miss", "K08D", "C:blocker", "Root-best near miss stays closed.", taxonomy = "root_best_near_miss"),
      Sample("MI5-suppressed-queen-trade", "MI5", "C:blocker", "Queen-trade relief row stays closed.", taxonomy = "tactic_first_relief")
    )

  private val naturalSamples =
    TaskShiftProvingFixtures.reviewFixtures.map(fixture =>
      Sample(
        id = s"natural-${fixture.id}",
        fixtureId = fixture.id,
        reviewGroup = naturalReviewGroup(fixture),
        note = fixture.note,
        taxonomy = taxonomyForTags(fixture.expectedTags)
      )
    )

  private val sourceSurfaceFixtures =
    List(
      sourceSurfaceSample("source-evans-opsahl-1950", "source:B:carlsbad_fixed_target", "source_carlsbad_fixed_target"),
      sourceSurfaceSample("source-carlsen-anand-2014-g6", "source:C:queen_trade_shield", "source_queen_trade_boundary"),
      sourceSurfaceSample("source-capablanca-golombek-1939-iqp-inducement", "source:C:iqp_inducement", "source_iqp_inducement"),
      sourceSurfaceSample("source-evans-opsahl-1950-iqp-inducement", "source:C:iqp_inducement", "source_iqp_inducement"),
      sourceSurfaceSample("source-alekhine-bogoljubow-1936-iqp-inducement", "source:C:iqp_inducement", "source_iqp_inducement"),
      sourceSurfaceSample("source-najdorf-sergeant-1939-iqp-inducement", "source:C:iqp_inducement", "source_iqp_inducement"),
      sourceSurfaceSample("source-botvinnik-vidmar-1936-iqp-opening-inducement", "source:C:iqp_inducement", "source_iqp_inducement"),
      sourceSurfaceSample("source-lokvenc-czerniak-1952-b6-b5-break-prevention", "source:A:break_prevention", "source_break_prevention"),
      sourceSurfaceSample("source-maderna-palermo-1955-a6-a5-break-prevention", "source:A:break_prevention", "source_break_prevention"),
      sourceSurfaceSample("source-camara-bazan-1960-b7-b5-break-prevention", "source:A:break_prevention", "source_break_prevention"),
      sourceSurfaceSample("source-sliwa-gromek-1960-a6-a5-break-prevention", "source:A:break_prevention", "source_break_prevention"),
      sourceSurfaceSample("source-pfleger-maalouf-1961-a6-a5-break-prevention", "source:A:break_prevention", "source_break_prevention"),
      sourceSurfaceSample("source-polugaevsky-giorgadze-1956-c5-c4-break-prevention", "source:A:break_prevention", "source_break_prevention"),
      sourceSurfaceSample("source-salov-ljubojevic-1992-simplification-window", "source:C:simplification_window", "source_simplification_window"),
      sourceSurfaceSample("source-boleslavsky-nezhmetdinov-1950-static-weakness-fixation", "source:B:static_weakness_fixation", "source_static_weakness_fixation")
    )

  private[commentary] val sourceSurfaceFixtureIds =
    sourceSurfaceFixtures.map(_.id)

  private def sourceSurfaceSample(id: String, reviewGroup: String, taxonomy: String): Sample =
    Sample(
      id,
      id,
      reviewGroup,
      "Fixed source surface fixture copied from prior SourceReview evidence; surface-contract replay only.",
      taxonomy = taxonomy
    )

  private val screenSamples =
    List(
      Sample("screen-K03B", "K03B", "screen:B:fixed_target_candidate", "FEN-only Carlsbad target candidate; no PV owner proof.", taxonomy = "fen_only_owner_path_missing"),
      Sample("screen-K03C", "K03C", "screen:B:fixed_target_candidate", "FEN-only Carlsbad target candidate; no PV owner proof.", taxonomy = "fen_only_owner_path_missing"),
      Sample("screen-K03D", "K03D", "screen:B:fixed_target_candidate", "FEN-only Carlsbad target candidate; no PV owner proof.", taxonomy = "fen_only_owner_path_missing"),
      Sample("screen-K09C", "K09C", "screen:C:simplification_candidate", "FEN-only IQP simplification candidate; no same-branch owner proof.", taxonomy = "fen_only_owner_path_missing"),
      Sample("screen-K09G", "K09G", "screen:C:simplification_candidate", "FEN-only IQP simplification candidate; no same-branch owner proof.", taxonomy = "fen_only_owner_path_missing"),
      Sample("screen-K09H", "K09H", "screen:C:simplification_candidate", "FEN-only IQP simplification candidate; no same-branch owner proof.", taxonomy = "fen_only_owner_path_missing"),
      Sample("screen-K09I", "K09I", "screen:C:simplification_candidate", "FEN-only IQP simplification candidate; no same-branch owner proof.", taxonomy = "fen_only_owner_path_missing"),
      Sample("screen-K08B", "K08B", "screen:C:attacker_trade_candidate", "FEN-only attacking-piece trade candidate; no root PV owner proof.", taxonomy = "fen_only_owner_path_missing"),
      Sample("screen-K08C", "K08C", "screen:C:attacker_trade_candidate", "FEN-only attacking-piece trade candidate; no root PV owner proof.", taxonomy = "fen_only_owner_path_missing"),
      Sample("screen-K08E", "K08E", "screen:C:attacker_trade_candidate", "FEN-only attacking-piece trade candidate; no root PV owner proof.", taxonomy = "fen_only_owner_path_missing"),
      Sample("screen-K08F", "K08F", "screen:C:attacker_trade_candidate", "FEN-only attacking-piece trade candidate; no root PV owner proof.", taxonomy = "fen_only_owner_path_missing"),
      Sample("screen-K08G", "K08G", "screen:C:attacker_trade_candidate", "FEN-only attacking-piece trade candidate; no root PV owner proof.", taxonomy = "fen_only_owner_path_missing")
    )

  private val prioritySamples =
    List(
      Sample("priority-MI1", "priority-MI1", "priority:C:simplification_conversion", "PV-backed material-imbalance simplification candidate from PlanPriority FEN bank.", taxonomy = "same_job_or_conversion_relabel_blocked"),
      Sample("priority-MI4", "priority-MI4", "priority:C:simplification_conversion", "PV-backed bishop-pawn simplification candidate from PlanPriority FEN bank.", taxonomy = "same_job_or_conversion_relabel_blocked"),
      Sample("priority-MR1", "priority-MR1", "priority:C:tactical_first_blocker", "PV-backed Hedgehog shell where a direct tactic leads over strategic background.", taxonomy = "tactical_truth_first"),
      Sample("priority-MR2", "priority-MR2", "priority:C:tactical_first_blocker", "PV-backed open-file fight where a tactic leads over strategic background.", taxonomy = "tactical_truth_first"),
      Sample("priority-MR3", "priority-MR3", "priority:C:tactical_first_blocker", "PV-backed Dragon shell where forcing line leads over strategic background.", taxonomy = "tactical_truth_first"),
      Sample("priority-TO1", "priority-TO1", "priority:C:tactical_override", "PV-backed won-pawn continuation from tactical override bank.", taxonomy = "tactical_truth_first"),
      Sample("priority-SC2", "priority-SC2", "priority:B:compensation_target_pressure", "PV-backed strategic-compensation weakness-fixation candidate.", taxonomy = "compensation_target_pressure_boundary"),
      Sample("priority-SC3", "priority-SC3", "priority:B:compensation_target_pressure", "PV-backed Benko compensation weakness-fixation candidate.", taxonomy = "compensation_target_pressure_boundary"),
      Sample("priority-SC4", "priority-SC4", "priority:B:compensation_target_pressure", "PV-backed Benko bishop-pressure weakness-fixation candidate.", taxonomy = "compensation_target_pressure_boundary"),
      Sample("priority-SC5", "priority-SC5", "priority:B:compensation_target_pressure", "PV-backed Blumenfeld pressure candidate.", taxonomy = "compensation_target_pressure_boundary"),
      Sample("priority-SC6", "priority-SC6", "priority:B:compensation_target_pressure", "PV-backed Blumenfeld initiative candidate.", taxonomy = "compensation_target_pressure_boundary")
    )

  private val controlledSamples =
    List(
      Sample(
        "B15A-supported-local-soft",
        "B15A",
        "B:softened_fixed_target",
        "Same real B15A board/PV with exact owner path deliberately weakened.",
        softenOwnerPath = true,
        taxonomy = "weak_owner_path"
      ),
      Sample(
        "K09B-supported-local-soft",
        "K09B",
        "C:softened_simplification",
        "Same real K09B board/PV with simplification owner path deliberately weakened.",
        softenOwnerPath = true,
        taxonomy = "weak_owner_path"
      ),
      Sample(
        "iqp-supported-local-control",
        "iqp-supported-local-control",
        "C:iqp_inducement_control",
        "Controlled exact board/PV where the sequence leaves an opponent isolated pawn.",
        taxonomy = "iqp_inducement_supported_local"
      ),
      Sample(
        "break-prevention-supported-local-control",
        "break-prevention-supported-local-control",
        "A:break_prevention_control",
        "Controlled exact board/PV where a named key break is locally neutralized, then deliberately weakened to SupportedLocal.",
        softenOwnerPath = true,
        taxonomy = "break_prevention_supported_local"
      ),
      Sample("B15A-tactical-veto", "B15A", "negative:tactical_veto", "Same B15A strategic row under tactical failure.", tacticalContract = true, taxonomy = "tactical_truth_first"),
      Sample("B16B-tactical-veto", "B16B", "negative:tactical_veto", "Same B16B strategic row under tactical failure.", tacticalContract = true, taxonomy = "tactical_truth_first"),
      Sample("K09A-tactical-veto", "K09A", "negative:tactical_veto", "Same K09A coordination row under tactical failure.", tacticalContract = true, taxonomy = "tactical_truth_first"),
      Sample("K09B-tactical-veto", "K09B", "negative:tactical_veto", "Same K09B simplification row under tactical failure.", tacticalContract = true, taxonomy = "tactical_truth_first"),
      Sample("K09F-tactical-veto", "K09F", "negative:tactical_veto", "Same K09F simplification row under tactical failure.", tacticalContract = true, taxonomy = "tactical_truth_first"),
      Sample(
        "break-prevention-tactical-veto",
        "break-prevention-supported-local-control",
        "negative:break_prevention_tactical_veto",
        "Same named-break control under explicit tactical failure.",
        tacticalContract = true,
        softenOwnerPath = true,
        taxonomy = "tactical_truth_first"
      ),
      Sample(
        "break-prevention-missing-witness-control",
        "break-prevention-missing-witness-control",
        "negative:break_prevention_missing_witness",
        "Named-break shell with the best-defense branch witness missing.",
        taxonomy = "owner:break_prevention_witness_missing"
      ),
      Sample(
        "break-prevention-rival-relabel-control",
        "break-prevention-rival-relabel-control",
        "negative:break_prevention_rival_or_relabel",
        "Named-break shell blocked by release-risk rather than promoted as generic counterplay prose.",
        taxonomy = "owner:break_prevention_rival_or_relabel"
      ),
      Sample(
        "prophylaxis-restraint-supported-local-control",
        "prophylaxis-restraint-supported-local-control",
        "A:prophylaxis_restraint_control",
        "Controlled exact board/PV where a named counterplay resource is locally restrained.",
        taxonomy = "prophylaxis_restraint_supported_local"
      ),
      Sample(
        "prophylaxis-restraint-tactical-veto",
        "prophylaxis-restraint-supported-local-control",
        "negative:prophylaxis_restraint_tactical_veto",
        "Same prophylaxis-restraint control under explicit tactical failure.",
        tacticalContract = true,
        taxonomy = "tactical_truth_first"
      ),
      Sample(
        "prophylaxis-restraint-missing-witness-control",
        "prophylaxis-restraint-missing-witness-control",
        "negative:prophylaxis_restraint_missing_witness",
        "Named counterplay-restraint shell missing the best-defense branch witness.",
        taxonomy = "owner:prophylaxis_restraint_witness_missing"
      ),
      Sample(
        "prophylaxis-restraint-rival-relabel-control",
        "prophylaxis-restraint-rival-relabel-control",
        "negative:prophylaxis_restraint_rival_or_relabel",
        "Named counterplay-restraint shell blocked by move-order release risk.",
        taxonomy = "owner:prophylaxis_restraint_rival_or_relabel"
      ),
      Sample("priority-MR1-tactical-veto", "priority-MR1", "negative:tactical_veto", "PlanPriority MR1 under explicit tactical failure.", tacticalContract = true, taxonomy = "tactical_truth_first"),
      Sample("priority-MR2-tactical-veto", "priority-MR2", "negative:tactical_veto", "PlanPriority MR2 under explicit tactical failure.", tacticalContract = true, taxonomy = "tactical_truth_first"),
      Sample("priority-TO1-tactical-veto", "priority-TO1", "negative:tactical_veto", "PlanPriority TO1 under explicit tactical failure.", tacticalContract = true, taxonomy = "tactical_truth_first"),
      Sample(
        "B15A-supported-local-veto",
        "B15A",
        "negative:softened_tactical_veto",
        "Same softened B15A row under tactical failure.",
        tacticalContract = true,
        softenOwnerPath = true,
        taxonomy = "tactical_truth_first"
      )
    )

  private[commentary] val samples =
    curatedSamples ++ naturalSamples ++ sourceSurfaceFixtures ++ screenSamples ++ prioritySamples ++ controlledSamples

  private val header =
    List(
      "id",
      "reviewGroup",
      "release",
      "taxonomy",
      "plannerOwner",
      "primary",
      "moveReview",
      "leak",
      "rejected",
      "contractId",
      "contractStatus",
      "contractFailures",
      "note"
    ).mkString("\t")

  private[commentary] def observations(ids: Set[String] = Set.empty): List[Observation] =
    val selected = samples.filter(sample => ids.isEmpty || ids.contains(sample.id))
    selected.map(observe)

  private[commentary] def outputPaths(ids: Set[String]): OutputPaths =
    val suffix = if ids.isEmpty then "" else "_subset"
    OutputPaths(
      matrix = Paths.get("tmp", s"strategic_claim_authority_surface_ledger$suffix.tsv"),
      review = Paths.get("tmp", s"strategic_claim_authority_surface_review$suffix.md")
    )

  @main def runAuthoritySurfaceLedger(ids: String*): Unit =
    val selectedIds = ids.map(_.trim).filter(_.nonEmpty).toSet
    val observed = observations(selectedIds)
    val lines = header :: observed.map(_.tsv)
    val paths = outputPaths(selectedIds)
    Files.createDirectories(paths.matrix.getParent)
    Files.write(paths.matrix, lines.mkString("\n").getBytes(StandardCharsets.UTF_8))
    Files.write(paths.review, surfaceReviewMarkdown(observed).getBytes(StandardCharsets.UTF_8))

    println(lines.mkString("\n"))
    println()
    println(s"wrote=${paths.matrix.toAbsolutePath}")
    println(s"review=${paths.review.toAbsolutePath}")
    println(summary(observed))

  private[commentary] def surfaceReviewMarkdown(observations: List[Observation]): String =
    val naturalSupported =
      observations.filter(obs =>
        (obs.sample.id.startsWith("natural-") || obs.sample.id.startsWith("source-")) &&
          obs.release == "SupportedLocal"
      )
    val lines =
      List(
        "# Strategic Claim Authority Surface Ledger",
        "",
        summary(observations),
        s"Surface SupportedLocal fixtures: ${if naturalSupported.isEmpty then "none found" else naturalSupported.map(_.sample.id).mkString(", ")}",
        s"Candidate screen rows: ${observations.count(_.sample.id.startsWith("screen-"))}",
        s"Source surface fixtures: ${if sourceSurfaceFixtureIds.isEmpty then "none" else sourceSurfaceFixtureIds.mkString(", ")}",
        "Engine-backed source admission: SourceReview only",
        ""
      ) ++
        List("CertifiedOwner", "SupportedLocal", "Suppressed", "TacticalVeto").flatMap { release =>
          val rows = observations.filter(_.release == release)
          s"## $release" :: rows.take(12).map(reviewLine)
        }
    lines.mkString("\n") + "\n"

  private def reviewLine(obs: Observation): String =
    s"- ${obs.sample.id} (${obs.sample.reviewGroup}) plannerOwner=${clean(obs.plannerOwner)} taxonomy=${obs.taxonomy} contract=${obs.contractId}:${obs.contractStatus}:${obs.contractFailures} primary=${clean(obs.primary)} moveReview=${clean(obs.moveReview)}"

  private def naturalReviewGroup(fixture: TaskShiftProvingFixtures.ReviewFixture): String =
    val tags = fixture.expectedTags.toSet
    if tags.contains("target_fixation_without_handoff") then "natural:B"
    else if tags.contains("positive_control") || tags.contains("holdable_simplification") then "natural:C"
    else if tags.contains("candidate_attack_piece_trade") || tags.contains("queen_trade_relief") then "natural:C-blocker"
    else "natural:blocker"

  private def taxonomyForTags(tags: List[String]): String =
    val tagSet = tags.toSet
    if tagSet.contains("queen_trade_relief") then "tactic_first_relief"
    else if tagSet.contains("near_miss_root_best") then "root_best_near_miss"
    else if tagSet.contains("candidate_attack_piece_trade") then "attacking_piece_trade_unowned"
    else if tagSet.contains("heavy_piece_release_survives") then "rival_release"
    else if tagSet.contains("preparatory_only") then "certified_coordination_not_task_shift"
    else if tagSet.contains("file_entry_contrast") then "file_entry_relabel_boundary"
    else if tagSet.contains("entry_square_candidate") then "prophylaxis_absorption"
    else if tagSet.contains("target_fixation_without_handoff") then "certified_or_fail_closed_target_fixation"
    else if tagSet.contains("positive_control") || tagSet.contains("holdable_simplification") then "certified_owner_path"
    else if tagSet.contains("non_simplification_root_best") then "same_job_or_conversion_relabel_blocked"
    else "-"

  private def observe(sample: Sample): Observation =
    val (fixture, ctx, pack) = scene(sample.fixtureId)
    val planningCtx =
      if sample.softenOwnerPath then ctx.copy(strategicPlanEvidence = PlanEvidenceEvaluator.StrategicPlanEvidenceView.empty)
      else ctx
    val truthContract = Option.when(sample.tacticalContract)(tacticalFailureContract(fixture))
    val inputs =
      if sample.tacticalContract then
        QuestionPlannerInputsBuilder.build(planningCtx, Some(pack), truthContract = None)
      else
        QuestionPlannerInputsBuilder.build(planningCtx, Some(pack), truthContract = truthContract)
    val effectiveInputs =
      if sample.softenOwnerPath then softenMainClaimOwnerPath(inputs)
      else inputs
    val ranked = QuestionFirstCommentaryPlanner.plan(planningCtx, effectiveInputs, truthContract)
    val rawMoveReview =
      moveReviewNarrative(
        MoveReviewCompressionPolicy.buildSlotsOrFallbackFromPlannerRuntime(
          ctx = planningCtx,
          inputs = effectiveInputs,
          rankedPlans = ranked,
          strategyPack = Some(pack),
          truthContract = truthContract
        )
      )
    val rawPrimary = ranked.primary.map(_.claim).getOrElse("-")
    val primary =
      if sample.softenOwnerPath then softenLocalReading(rawPrimary)
      else rawPrimary
    val moveReview =
      if sample.softenOwnerPath && primary != "-" then primary
      else rawMoveReview
    val baselineRelease =
      Option.when(sample.tacticalContract)(strategicBaselineRelease(sample, planningCtx, pack)).flatten
    val mainClaimPacket =
      effectiveInputs.mainBundle.flatMap(_.mainClaim).flatMap(_.packet)
    val proofTrace =
      mainClaimPacket.map(_.proofTrace)
    val ownerDisplaySource =
      ranked.primary.map { plan =>
        if proofTrace.flatMap(_.contractId).exists(id =>
            id == "runtime:neutralize_key_break" || id == "runtime:counterplay_restraint"
          )
        then
          mainClaimPacket.map(_.proofSource).getOrElse(plan.plannerSource)
        else plan.plannerSource
      }
    Observation(
      sample = sample,
      release = releaseLabel(sample, ranked, primary, moveReview, baselineRelease),
      taxonomy = sample.taxonomy,
      plannerOwner = ranked.primary
        .zip(ownerDisplaySource)
        .map { case (plan, source) => s"${plan.questionKind}:${plan.plannerOwnerKind}:$source" }
        .getOrElse("-"),
      primary = primary,
      moveReview = moveReview,
      leak = sample.tacticalContract && strategicLeak(primary, moveReview),
      rejected = ranked.rejected.map(r => s"${r.questionKind}:${r.reasons.mkString("+")}").mkString(" | "),
      contractId = proofTrace.flatMap(_.contractId).getOrElse("-"),
      contractStatus = proofTrace.flatMap(_.contractStatus).getOrElse("-"),
      contractFailures =
        proofTrace
          .map(trace => if trace.failureCodes.isEmpty then "none" else trace.failureCodes.distinct.mkString("+"))
          .getOrElse("-")
    )

  private def moveReviewNarrative(slots: MoveReviewPolishSlots): String =
    val prose = LiveNarrativeCompressionCore.deterministicProse(slots).trim
    if prose.isEmpty then "-" else prose

  private def softenLocalReading(text: String): String =
    val trimmed = Option(text).getOrElse("").trim
    if trimmed.isEmpty || trimmed == "-" then "-"
    else if trimmed.startsWith("A key idea") then trimmed
    else if trimmed.startsWith("A local reading") then
      val stripped = trimmed.stripPrefix("A local reading is that ").stripPrefix("a local reading is that ")
      s"A key idea is that ${stripped.take(1).toLowerCase}${stripped.drop(1)}"
    else s"A key idea is that ${trimmed.take(1).toLowerCase}${trimmed.drop(1)}"

  private def strategicBaselineRelease(
      sample: Sample,
      ctx: NarrativeContext,
      pack: lila.commentary.StrategyPack
  ): Option[String] =
    val baseInputs =
      QuestionPlannerInputsBuilder.build(ctx, Some(pack), truthContract = None)
    val effectiveBaseInputs =
      if sample.softenOwnerPath then softenMainClaimOwnerPath(baseInputs)
      else baseInputs
    val baseRanked =
      QuestionFirstCommentaryPlanner.plan(ctx, effectiveBaseInputs, truthContract = None)
    baseRanked.primary.flatMap(positiveRelease)

  private final case class SceneFixture(
      id: String,
      label: String,
      fen: String,
      phase: String,
      ply: Int,
      scoreCp: Int,
      pvMoves: List[String],
      expectedTags: List[String],
      note: String,
      playedUci: Option[String] = None,
      extraVariations: List[VariationLine] = Nil,
      mainStrategicPlans: List[PlanHypothesis] = Nil,
      strategicPlanExperiments: List[StrategicPlanExperiment] = Nil,
      authorQuestions: List[AuthorQuestion] = Nil,
      authorEvidence: List[QuestionEvidence] = Nil,
      semantic: Option[SemanticSection] = None,
      engineEvidence: Option[EngineEvidence] = None,
      ctxOverride: Option[NarrativeContext] = None,
      strategyPackOverride: Option[StrategyPack] = None
  )

  private def scene(id: String) =
    val fixture: SceneFixture =
      TaskShiftProvingFixtures.reviewFixtures
        .find(_.id == id)
        .map(fixture =>
          SceneFixture(
            id = fixture.id,
            label = fixture.label,
            fen = fixture.fen,
            phase = fixture.phase,
            ply = fixture.ply,
            scoreCp = fixture.scoreCp,
            pvMoves = fixture.pvMoves,
            expectedTags = fixture.expectedTags,
            note = fixture.note
          )
        )
        .orElse(
          priorityScene(id)
        )
        .orElse(
          sourceScene(id)
        )
        .orElse(
          controlledScene(id)
        )
        .orElse(
          StrategicIdeaFenFixtures.all.find(_.id == id).map(fixture =>
            SceneFixture(
              id = fixture.id,
              label = fixture.label,
              fen = fixture.fen,
              phase = fixture.phase,
              ply = 1,
              scoreCp = fixture.stockfishScoreCp.getOrElse(0),
              pvMoves = Nil,
              expectedTags = fixture.forbiddenKinds,
              note = fixture.label
            )
          )
        )
        .getOrElse(sys.error(s"missing review or FEN fixture: $id"))
    val (ctx, pack) =
      fixture.ctxOverride match
        case Some(ctx) =>
          val pack =
            fixture.strategyPackOverride
              .getOrElse(sys.error(s"strategy pack override missing for ${fixture.id}"))
          (ctx, pack)
        case None =>
          val data =
            CommentaryEngine
              .assessExtended(
                fen = fixture.fen,
                variations =
                  VariationLine(fixture.pvMoves, fixture.scoreCp, depth = if fixture.pvMoves.isEmpty then 0 else 16) ::
                    fixture.extraVariations,
                playedMove = fixture.playedUci,
                phase = Some(fixture.phase),
                ply = fixture.ply,
                prevMove = fixture.playedUci
              )
              .getOrElse(sys.error(s"analysis missing for ${fixture.id}"))
          val baseCtx =
            NarrativeContextBuilder
              .build(data, data.toContext, None)
          val selectedMainPlans =
            if fixture.mainStrategicPlans.nonEmpty then fixture.mainStrategicPlans
            else baseCtx.mainStrategicPlans
          val selectedExperiments =
            if fixture.strategicPlanExperiments.nonEmpty then fixture.strategicPlanExperiments
            else baseCtx.strategicPlanExperiments
          val ctx =
            baseCtx.copy(
              authorQuestions =
                if fixture.authorQuestions.nonEmpty then fixture.authorQuestions
                else defaultQuestions,
              authorEvidence =
                if fixture.authorEvidence.nonEmpty then fixture.authorEvidence
                else baseCtx.authorEvidence,
              mainStrategicPlans = selectedMainPlans,
              strategicPlanExperiments = selectedExperiments,
              strategicPlanEvidence =
                if fixture.mainStrategicPlans.nonEmpty || fixture.strategicPlanExperiments.nonEmpty then
                  lila.commentary.analysis.StrategicPlanEvidenceTestSupport.fromExperiments(
                    selectedMainPlans,
                    selectedExperiments
                  )
                else baseCtx.strategicPlanEvidence,
              semantic = fixture.semantic.orElse(baseCtx.semantic),
              engineEvidence = fixture.engineEvidence.orElse(baseCtx.engineEvidence)
            )
          val pack =
            fixture.strategyPackOverride.getOrElse(
              StrategyPackBuilder
                .build(data, ctx)
                .getOrElse(sys.error(s"strategy pack missing for ${fixture.id}"))
            )
          (ctx, pack)
    (fixture, ctx, pack)

  private def priorityScene(id: String): Option[SceneFixture] =
    priorityScenes.get(id)

  private def sourceScene(id: String): Option[SceneFixture] =
    sourceScenes.get(id)

  private def controlledScene(id: String): Option[SceneFixture] =
    controlledScenes.get(id)

  private val breakPreventionFen =
    "2r2rk1/pp3pp1/2n1p2p/3p4/3P1P2/2P1PN1P/PP4P1/2R2RK1 w - - 0 23"

  private val breakPreventionPv =
    List("c1c8", "f8e8", "c8e8")

  private def evidenceBackedPlan(
      planId: String,
      moveOrderSensitive: Boolean
  ): PlanHypothesis =
    PlanHypothesis(
      planId = planId,
      planName = "Clamp the ...c5 break",
      rank = 1,
      score = 0.82,
      preconditions = Nil,
      executionSteps =
        if moveOrderSensitive then List("This candidate is intentionally marked move-order sensitive.")
        else List("Keep the opponent's main counterplay route closed first."),
      failureModes = Nil,
      viability = PlanViability(score = 0.8, label = "high", risk = "test"),
      evidenceSources = List(s"theme:${PlanTaxonomy.PlanTheme.RestrictionProphylaxis.id}"),
      themeL1 = PlanTaxonomy.PlanTheme.RestrictionProphylaxis.id,
      subplanId = Some(PlanTaxonomy.PlanKind.BreakPrevention.id)
    )

  private def evidenceBackedExperiment(
      planId: String,
      moveOrderSensitive: Boolean
  ): StrategicPlanExperiment =
    StrategicPlanExperiment(
      planId = planId,
      themeL1 = PlanTaxonomy.PlanTheme.RestrictionProphylaxis.id,
      subplanId = Some(PlanTaxonomy.PlanKind.BreakPrevention.id),
      evidenceTier = "evidence_backed",
      supportProbeCount = 1,
      refuteProbeCount = 0,
      bestReplyStable = true,
      futureSnapshotAligned = true,
      counterBreakNeutralized = true,
      moveOrderSensitive = moveOrderSensitive,
      experimentConfidence = if moveOrderSensitive then 0.62 else 0.86
    )

  private def breakPreventionSemantic: SemanticSection =
    SemanticSection(
      structuralWeaknesses = Nil,
      pieceActivity = Nil,
      positionalFeatures = Nil,
      compensation = None,
      endgameFeatures = None,
      practicalAssessment = None,
      preventedPlans = List(
        PreventedPlanInfo(
          planId = "deny_counterplay",
          deniedSquares = List("c5"),
          breakNeutralized = Some("...c5"),
          mobilityDelta = -2,
          counterplayScoreDrop = 140,
          preventedThreatType = Some("counterplay"),
          deniedResourceClass = Some("break"),
          citationLine = Some("The ...c5 break never becomes available on the defended branch.")
        )
      ),
      conceptSummary = Nil
    )

  private def breakPreventionPack: StrategyPack =
    StrategyPack(
      sideToMove = "white",
      directionalTargets = List(
        StrategyDirectionalTarget(
          targetId = "target_c5",
          ownerSide = "white",
          piece = "R",
          from = "c1",
          targetSquare = "c5",
          readiness = DirectionalTargetReadiness.Build,
          strategicReasons = List("deny the ...c5 break"),
          evidence = List("probe")
        )
      ),
      signalDigest = Some(NarrativeSignalDigest(decision = Some("deny the ...c5 break")))
    )

  private def breakPreventionEngineEvidence(pv: List[String]): EngineEvidence =
    EngineEvidence(
      depth = 18,
      variations = List(VariationLine(moves = pv, scoreCp = 88, depth = 18))
    )

  private def breakPreventionControlFixture(
      id: String,
      pv: List[String],
      note: String,
      moveOrderSensitive: Boolean = false
  ): SceneFixture =
    val planId = s"${id}_plan"
    SceneFixture(
      id = id,
      label = note,
      fen = breakPreventionFen,
      phase = "middlegame",
      ply = 46,
      scoreCp = 88,
      pvMoves = pv,
      expectedTags = List("break_prevention", "counterplay_axis_suppression"),
      note = note,
      playedUci = Some("c1c8"),
      mainStrategicPlans = List(evidenceBackedPlan(planId, moveOrderSensitive)),
      strategicPlanExperiments = List(evidenceBackedExperiment(planId, moveOrderSensitive)),
      semantic = Some(breakPreventionSemantic),
      engineEvidence = Some(breakPreventionEngineEvidence(pv)),
      strategyPackOverride = Some(breakPreventionPack)
    )

  private val prophylaxisRestraintPv =
    List("a2a3", "b7b5", "a3a4")

  private def prophylaxisRestraintPlan(
      planId: String,
      moveOrderSensitive: Boolean
  ): PlanHypothesis =
    PlanHypothesis(
      planId = planId,
      planName = "Slow queenside counterplay before expanding",
      rank = 1,
      score = 0.82,
      preconditions = Nil,
      executionSteps =
        if moveOrderSensitive then List("This candidate is intentionally marked move-order sensitive.")
        else List("Slow queenside counterplay before expanding."),
      failureModes = List("If the branch opens up, queenside counterplay comes back."),
      viability = PlanViability(score = 0.8, label = "high", risk = "test"),
      evidenceSources = List(s"theme:${PlanTaxonomy.PlanTheme.RestrictionProphylaxis.id}"),
      themeL1 = PlanTaxonomy.PlanTheme.RestrictionProphylaxis.id,
      subplanId = Some(PlanTaxonomy.PlanKind.ProphylaxisRestraint.id)
    )

  private def prophylaxisRestraintExperiment(
      planId: String,
      moveOrderSensitive: Boolean
  ): StrategicPlanExperiment =
    StrategicPlanExperiment(
      planId = planId,
      themeL1 = PlanTaxonomy.PlanTheme.RestrictionProphylaxis.id,
      subplanId = Some(PlanTaxonomy.PlanKind.ProphylaxisRestraint.id),
      evidenceTier = "evidence_backed",
      supportProbeCount = 1,
      refuteProbeCount = 0,
      bestReplyStable = true,
      futureSnapshotAligned = true,
      counterBreakNeutralized = true,
      moveOrderSensitive = moveOrderSensitive,
      experimentConfidence = if moveOrderSensitive then 0.62 else 0.87
    )

  private def prophylaxisRestraintSemantic: SemanticSection =
    SemanticSection(
      structuralWeaknesses = Nil,
      pieceActivity = Nil,
      positionalFeatures = Nil,
      compensation = None,
      endgameFeatures = None,
      practicalAssessment = None,
      preventedPlans = List(
        PreventedPlanInfo(
          planId = "queenside counterplay",
          deniedSquares = Nil,
          breakNeutralized = None,
          mobilityDelta = -2,
          counterplayScoreDrop = 138,
          preventedThreatType = None,
          deniedResourceClass = Some("counterplay_route"),
          citationLine = Some("Queenside counterplay never gets going on the defended branch.")
        )
      ),
      conceptSummary = Nil
    )

  private def prophylaxisRestraintPack: StrategyPack =
    StrategyPack(
      sideToMove = "white",
      directionalTargets = List(
        StrategyDirectionalTarget(
          targetId = "target_b4",
          ownerSide = "white",
          piece = "P",
          from = "a2",
          targetSquare = "b4",
          readiness = DirectionalTargetReadiness.Build,
          strategicReasons = List("queenside counterplay"),
          evidence = List("probe")
        )
      ),
      signalDigest = Some(NarrativeSignalDigest(decision = Some("queenside counterplay")))
    )

  private def prophylaxisBaseCtx: NarrativeContext =
    NarrativeContext(
      fen = "r2q1rk1/pp2bppp/2np1n2/2p1p3/2P1P3/2NP1NP1/PP2QPBP/R1B2RK1 w - - 0 10",
      header = ContextHeader("Middlegame", "Normal", "StyleChoice", "Medium", "ExplainPlan"),
      ply = 20,
      playedMove = Some("e2e3"),
      playedSan = Some("Qe2"),
      summary = NarrativeSummary("Central restraint", None, "StyleChoice", "Maintain", "0.20"),
      threats = ThreatTable(Nil, Nil),
      pawnPlay = PawnPlayTable(false, None, "Low", "Maintain", "Quiet", "Background", None, false, "quiet"),
      plans = PlanTable(Nil, Nil),
      delta = None,
      phase = PhaseContext("Middlegame", "Normal middlegame"),
      candidates = Nil,
      renderMode = NarrativeRenderMode.MoveReview
    )

  private def prophylaxisRestraintControlFixture(
      id: String,
      pv: List[String],
      note: String,
      moveOrderSensitive: Boolean = false
  ): SceneFixture =
    val planId = s"${id}_plan"
    val questions =
      List(
        AuthorQuestion(
          id = "q_b2b_prophylactic_why_this",
          kind = AuthorQuestionKind.WhyThis,
          priority = 100,
          question = "Why is a3 the right prophylactic move here?",
          evidencePurposes = List("reply_multipv")
        )
      )
    val plans = List(prophylaxisRestraintPlan(planId, moveOrderSensitive))
    val experiments = List(prophylaxisRestraintExperiment(planId, moveOrderSensitive))
    val engineEvidence = EngineEvidence(depth = 18, variations = List(VariationLine(pv, scoreCp = 82, depth = 18)))
    SceneFixture(
      id = id,
      label = note,
      fen = breakPreventionFen,
      phase = "middlegame",
      ply = 46,
      scoreCp = 82,
      pvMoves = pv,
      expectedTags = List("prophylaxis_restraint", "prophylactic_move"),
      note = note,
      playedUci = Some("a2a3"),
      mainStrategicPlans = plans,
      strategicPlanExperiments = experiments,
      authorQuestions = questions,
      semantic = Some(prophylaxisRestraintSemantic),
      engineEvidence = Some(engineEvidence),
      ctxOverride = Some(
        prophylaxisBaseCtx.copy(
          fen = breakPreventionFen,
          ply = 23,
          playedMove = Some("a2a3"),
          playedSan = Some("a3"),
          authorQuestions = questions,
          mainStrategicPlans = plans,
          strategicPlanExperiments = experiments,
          semantic = Some(prophylaxisRestraintSemantic),
          engineEvidence = Some(engineEvidence)
        )
      ),
      strategyPackOverride = Some(prophylaxisRestraintPack)
    )

  private val controlledScenes: Map[String, SceneFixture] =
    List(
      breakPreventionControlFixture(
        id = "break-prevention-supported-local-control",
        pv = breakPreventionPv,
        note = "Controlled exact board/PV for named ...c5-break prevention."
      ),
      breakPreventionControlFixture(
        id = "break-prevention-missing-witness-control",
        pv = List("c1c8"),
        note = "Named ...c5-break prevention shell missing the best-defense branch witness."
      ),
      breakPreventionControlFixture(
        id = "break-prevention-rival-relabel-control",
        pv = breakPreventionPv,
        note = "Named ...c5-break prevention shell with move-order release risk.",
        moveOrderSensitive = true
      ),
      prophylaxisRestraintControlFixture(
        id = "prophylaxis-restraint-supported-local-control",
        pv = prophylaxisRestraintPv,
        note = "Controlled exact board/PV for named queenside-counterplay restraint."
      ),
      prophylaxisRestraintControlFixture(
        id = "prophylaxis-restraint-missing-witness-control",
        pv = List("a2a3"),
        note = "Named queenside-counterplay restraint shell missing the best-defense branch witness."
      ),
      prophylaxisRestraintControlFixture(
        id = "prophylaxis-restraint-rival-relabel-control",
        pv = prophylaxisRestraintPv,
        note = "Named queenside-counterplay restraint shell with move-order release risk.",
        moveOrderSensitive = true
      )
    ).map(scene => scene.id -> scene).toMap

  private val sourceScenes: Map[String, SceneFixture] =
    List(
      SceneFixture(
        id = "source-evans-opsahl-1950",
        label = "Evans-Opsahl 1950 exact Carlsbad fixed-target source row",
        fen = "r1b1rnk1/pp2qppp/2p5/3p4/3Pn3/2NBPN2/PPQ2PPP/1R3RK1 w - - 0 13",
        phase = "middlegame",
        ply = 25,
        scoreCp = 20,
        pvMoves =
          List(
            "b2b4",
            "a7a6",
            "a2a4",
            "e4c3",
            "c2c3",
            "f8g6",
            "b4b5",
            "c6b5",
            "a4b5",
            "c8g4",
            "b5a6",
            "b7a6",
            "f3d2",
            "g6h4",
            "c3c5",
            "e7g5"
          ),
        expectedTags = List("source", "carlsbad_fixed_target"),
        note = "Copied from Stockfish-backed source intake after exact replay admission.",
        playedUci = Some("b2b4")
      ),
      SceneFixture(
        id = "source-carlsen-anand-2014-g6",
        label = "Carlsen-Anand 2014 game 6 exact queen_trade_shield source row",
        fen = "r1bqk2r/1p1p1ppp/p1n1pn2/8/1bPNP3/2NQ4/PP3PPP/R1B1KB1R w KQkq - 5 8",
        phase = "opening",
        ply = 15,
        scoreCp = 20,
        pvMoves =
          List(
            "d4c6",
            "d7c6",
            "d3d8",
            "e8d8",
            "e4e5",
            "f6d7",
            "c1f4",
            "b7b5",
            "e1c1",
            "d8c7",
            "c3e4",
            "d7b6",
            "a2a3",
            "b4e7",
            "e4d6",
            "b5c4"
          ),
        expectedTags = List("source", "queen_trade_shield"),
        note = "Copied from Stockfish-backed source intake after exact replay admission.",
        playedUci = Some("d4c6")
      ),
      SceneFixture(
        id = "source-capablanca-golombek-1939-iqp-inducement",
        label = "Capablanca-Golombek 1939 exact IQP inducement source row",
        fen = "r3r1k1/pp3pn1/2pq2pp/3p4/NP1P4/3QP2P/P4PP1/1RR3K1 w - - 0 23",
        phase = "middlegame",
        ply = 45,
        scoreCp = 20,
        pvMoves =
          List(
            "b4b5",
            "e8c8",
            "b5c6",
            "b7b6",
            "d3a6",
            "c8c6",
            "a4b2",
            "c6c7",
            "b2d3",
            "g7e8",
            "c1c7",
            "d6c7",
            "b1c1",
            "c7e7",
            "d3f4"
          ),
        expectedTags = List("source", "iqp_inducement"),
        note = "Copied from Stockfish-backed source window probe after exact top-PV IQP inducement admission.",
        playedUci = Some("b4b5")
      ),
      SceneFixture(
        id = "source-evans-opsahl-1950-iqp-inducement",
        label = "Evans-Opsahl 1950 exact IQP inducement source row",
        fen = "r3rnk1/1p3ppp/p1p5/3p2q1/PP1P2b1/2QBP3/3N1PPP/1R3RK1 w - - 3 17",
        phase = "middlegame",
        ply = 33,
        scoreCp = 20,
        pvMoves =
          List(
            "f1c1",
            "h7h5",
            "b4b5",
            "c6b5",
            "a4b5",
            "a6a5",
            "d3f1",
            "h5h4",
            "h2h3",
            "g4h3",
            "d2f3",
            "g5f6",
            "g2h3",
            "f6f3",
            "f1g2",
            "f3f5"
          ),
        expectedTags = List("source", "iqp_inducement"),
        note = "Copied from Stockfish-backed source window probe after exact top-PV IQP inducement admission.",
        playedUci = Some("f1c1")
      ),
      SceneFixture(
        id = "source-alekhine-bogoljubow-1936-iqp-inducement",
        label = "Alekhine-Bogoljubow 1936 exact IQP inducement source row",
        fen = "rnb1k2r/pp3ppp/4p3/2pqP3/PbpPn3/2N2N2/1PQ1BPPP/R1B2RK1 b kq - 1 10",
        phase = "middlegame",
        ply = 20,
        scoreCp = 20,
        pvMoves =
          List(
            "e4c3",
            "b2c3",
            "c5d4",
            "c3b4",
            "d4d3",
            "c2a2",
            "d3e2",
            "a2e2",
            "d5d3",
            "e2b2",
            "b7b5",
            "a1a3",
            "d3e4",
            "a4b5",
            "c8b7",
            "f1d1",
            "b8d7",
            "h2h3",
            "b7d5"
          ),
        expectedTags = List("source", "iqp_inducement"),
        note = "Copied from Stockfish-backed source window probe after exact top-PV IQP inducement admission.",
        playedUci = Some("e4c3")
      ),
      SceneFixture(
        id = "source-najdorf-sergeant-1939-iqp-inducement",
        label = "Najdorf-Sergeant 1939 exact IQP inducement source row",
        fen = "r1b2rk1/pp2qppp/4p3/2nn4/3N4/2N1P3/PPQ2PPP/3RKB1R w K - 0 12",
        phase = "middlegame",
        ply = 23,
        scoreCp = 20,
        pvMoves =
          List(
            "c3d5",
            "e6d5",
            "f1e2",
            "b7b6",
            "e1g1",
            "c8b7",
            "d1c1",
            "f8c8",
            "h2h3",
            "g7g6",
            "e2f3",
            "c5e6",
            "c2d2"
          ),
        expectedTags = List("source", "iqp_inducement"),
        note = "Copied from Stockfish-backed source window probe after exact top-PV IQP inducement admission.",
        playedUci = Some("c3d5")
      ),
      SceneFixture(
        id = "source-botvinnik-vidmar-1936-iqp-opening-inducement",
        label = "Botvinnik-Vidmar 1936 exact opening IQP inducement source row",
        fen = "r1bq1rk1/pp1nbppp/4pn2/2pp2B1/2PP4/2NBPN2/PP3PPP/R2Q1RK1 b - - 1 8",
        phase = "opening",
        ply = 16,
        scoreCp = -120,
        pvMoves =
          List(
            "c5d4",
            "e3d4",
            "d5c4",
            "d3c4",
            "h7h6",
            "g5h4",
            "d7b6",
            "c4b3",
            "c8d7",
            "f3e5",
            "d7c6",
            "f1e1",
            "b6d5",
            "d1d3",
            "d5f4",
            "d3e3",
            "f4d5"
          ),
        expectedTags = List("source", "iqp_inducement"),
        note = "Copied from Stockfish-backed source window probe after near-top MultiPV IQP inducement admission.",
        playedUci = Some("c5d4"),
        extraVariations =
          List(
            VariationLine(
              List("h7h6", "g5f6", "d7f6", "c4d5", "f6d5", "d3c2", "d5b4", "c2e4", "c5d4", "e3d4", "b4d5"),
              scoreCp = -17,
              depth = 16
            )
          )
      ),
      SceneFixture(
        id = "source-lokvenc-czerniak-1952-b6-b5-break-prevention",
        label = "Lokvenc-Czerniak 1952 exact break-prevention source row",
        fen = "r1bqr1k1/p4pbp/np1p1np1/2pP4/4P3/2N2N2/PPQ1BPPP/R1B1R1K1 w - - 2 12",
        phase = "middlegame",
        ply = 23,
        scoreCp = 37,
        pvMoves =
          List(
            "e2b5",
            "a6b4",
            "c2d1",
            "c8d7",
            "b5f1",
            "b6b5",
            "a2a3",
            "b4a6",
            "c1f4",
            "d8b6",
            "h2h3",
            "c5c4",
            "e4e5",
            "d6e5",
            "f3e5",
            "d7f5"
          ),
        expectedTags = List("source", "break_prevention", "counterplay_axis_suppression"),
        note = "Copied from Stockfish-backed source review after clean route-clamp neutralize_key_break admission.",
        playedUci = Some("e2b5")
      ),
      SceneFixture(
        id = "source-maderna-palermo-1955-a6-a5-break-prevention",
        label = "Maderna-Palermo 1955 exact break-prevention source row",
        fen = "1rbqr1k1/1p1n1pbp/pn1p2p1/2pP4/P3PP2/2N2B2/1P1N2PP/R1BQR1K1 w - - 5 15",
        phase = "middlegame",
        ply = 29,
        scoreCp = 20,
        pvMoves =
          List(
            "a4a5",
            "b6a8",
            "d2c4",
            "d7f8",
            "e4e5",
            "d6e5",
            "f4e5",
            "b7b5",
            "a5b6",
            "a8b6",
            "c4a5",
            "c8d7",
            "c1f4",
            "d8c7",
            "f4g3",
            "g7e5",
            "e1e5",
            "e8e5",
            "a5c6",
            "d7c6",
            "d5c6",
            "f8e6"
          ),
        expectedTags = List("source", "break_prevention", "counterplay_axis_suppression"),
        note = "Copied from Stockfish-backed source review after clean route-clamp neutralize_key_break admission.",
        playedUci = Some("a4a5")
      ),
      SceneFixture(
        id = "source-camara-bazan-1960-b7-b5-break-prevention",
        label = "Camara-Bazan 1960 exact break-prevention source row",
        fen = "1rbqr1k1/pp1n1pbp/3p2p1/2pP4/1n2PP2/2NB3P/PP2N1P1/R1BQ1R1K w - - 3 14",
        phase = "middlegame",
        ply = 27,
        scoreCp = 20,
        pvMoves =
          List(
            "d3b5",
            "b4a6",
            "e4e5",
            "a6c7",
            "e5e6",
            "f7e6",
            "d5e6",
            "c7e6",
            "f4f5",
            "g6f5",
            "e2g3",
            "a7a6",
            "b5d7",
            "c8d7",
            "g3f5",
            "e8f8",
            "f5g7",
            "f8f1",
            "d1f1",
            "e6g7"
          ),
        expectedTags = List("source", "break_prevention", "counterplay_axis_suppression"),
        note = "Copied from Stockfish-backed source triage after clean route-clamp neutralize_key_break admission.",
        playedUci = Some("d3b5")
      ),
      SceneFixture(
        id = "source-sliwa-gromek-1960-a6-a5-break-prevention",
        label = "Sliwa-Gromek 1960 exact break-prevention source row",
        fen = "1r1r3k/1p1q1pbp/pn1p2p1/2pP4/Pn2PP2/NQ4PP/1P3B2/3RRBK1 w - - 2 28",
        phase = "middlegame",
        ply = 55,
        scoreCp = 34,
        pvMoves =
          List(
            "a4a5",
            "d7a4",
            "b3f3",
            "a4a5",
            "e4e5",
            "d6e5",
            "f4e5",
            "b4d5",
            "h3h4",
            "a5b4",
            "h4h5",
            "g6h5"
          ),
        expectedTags = List("source", "break_prevention", "counterplay_axis_suppression"),
        note = "Copied from Stockfish-backed source review after clean route-clamp neutralize_key_break admission.",
        playedUci = Some("a4a5")
      ),
      SceneFixture(
        id = "source-pfleger-maalouf-1961-a6-a5-break-prevention",
        label = "Pfleger-Maalouf 1961 exact break-prevention source row",
        fen = "r2qr1k1/1p3pb1/pn1p1npp/2pP4/P3P3/2NQ1N2/1P1B1PPP/R3R1K1 w - - 0 17",
        phase = "middlegame",
        ply = 33,
        scoreCp = 20,
        pvMoves =
          List(
            "a4a5",
            "b6d7",
            "c3a4",
            "f6g4",
            "d2c3",
            "g4e5",
            "f3e5",
            "g7e5",
            "c3e5",
            "d7e5",
            "d3g3",
            "d8g5",
            "a4b6",
            "g5g3",
            "h2g3",
            "a8d8",
            "f2f4",
            "e5d3",
            "e1e3",
            "d3b4",
            "a1d1",
            "b4c2"
          ),
        expectedTags = List("source", "break_prevention", "counterplay_axis_suppression"),
        note = "Copied from Stockfish-backed source review after clean route-clamp neutralize_key_break admission.",
        playedUci = Some("a4a5")
      ),
      SceneFixture(
        id = "source-polugaevsky-giorgadze-1956-c5-c4-break-prevention",
        label = "Polugaevsky-Giorgadze 1956 exact break-prevention source row",
        fen = "rnbqnrk1/5ppp/pp1p1b2/2pP4/P3P3/2N5/1P1NBPPP/R1BQ1RK1 w - - 0 12",
        phase = "middlegame",
        ply = 23,
        scoreCp = 64,
        pvMoves =
          List(
            "d2c4",
            "b8d7",
            "f2f4",
            "a8b8",
            "c1e3",
            "b6b5",
            "a4b5",
            "a6b5"
          ),
        expectedTags = List("source", "break_prevention", "counterplay_axis_suppression"),
        note = "Copied from Stockfish-backed source review after clean route-clamp neutralize_key_break admission.",
        playedUci = Some("d2c4")
      ),
      SceneFixture(
        id = "source-salov-ljubojevic-1992-simplification-window",
        label = "Salov-Ljubojevic 1992 exact SimplificationWindow source row",
        fen = "7k/p4qp1/8/1Q1pR3/3P1P2/2r3P1/7P/6K1 w - - 0 36",
        phase = "endgame",
        ply = 71,
        scoreCp = 388,
        pvMoves =
          List(
            "b5d5",
            "f7d5",
            "e5d5",
            "h8h7",
            "g1g2",
            "c3c2",
            "g2h3",
            "c2d2",
            "d5d7",
            "a7a5",
            "d7a7",
            "d2d4",
            "a7a5",
            "d4d2",
            "a5h5",
            "h7g8",
            "h3g4",
            "d2b2",
            "h2h4",
            "b2b8",
            "h5d5"
          ),
        expectedTags = List("source", "simplification_window"),
        note = "Copied from Stockfish-backed source window probe after exact top-PV SimplificationWindow admission.",
        playedUci = Some("b5d5")
      ),
      SceneFixture(
        id = "source-boleslavsky-nezhmetdinov-1950-static-weakness-fixation",
        label = "Boleslavsky-Nezhmetdinov 1950 exact StaticWeaknessFixation source row",
        fen = "rnbqr1k1/pp3pbp/3p1np1/2pP4/4P3/2N2N2/PP2BPPP/R1BQ1RK1 w - - 6 10",
        phase = "opening",
        ply = 19,
        scoreCp = 119,
        pvMoves =
          List(
            "f3d2",
            "b8a6",
            "g1h1",
            "a6c7",
            "a2a4",
            "b7b6",
            "f2f3",
            "d8e7",
            "d2c4",
            "c8a6",
            "c1g5",
            "a6c4",
            "e2c4",
            "h7h6",
            "g5h4",
            "a7a6",
            "f3f4",
            "b6b5",
            "a4b5",
            "a6b5",
            "a1a8",
            "e8a8"
          ),
        expectedTags = List("source", "static_weakness_fixation"),
        note = "Copied from Stockfish-backed source window probe after exact top-PV StaticWeaknessFixation admission.",
        playedUci = Some("f3d2")
      ),
      SceneFixture(
        id = "source-aronian-andreikin-2014-defender-trade",
        label = "Aronian-Andreikin 2014 exact DefenderTrade source row",
        fen = "3k1b1r/p2b1ppp/1n3n2/4p3/8/1R4P1/P1QPqPBP/2B2RK1 w - - 0 17",
        phase = "middlegame",
        ply = 33,
        scoreCp = 158,
        pvMoves =
          List(
            "c1a3",
            "f8a3",
            "b3a3",
            "e2c4",
            "c2c4",
            "b6c4",
            "a3a7",
            "h8e8",
            "a2a4",
            "c4d2",
            "f1d1",
            "d2b3",
            "a4a5",
            "b3c5",
            "g3g4",
            "h7h6",
            "h2h4",
            "e8e6"
          ),
        expectedTags = List("source", "defender_trade"),
        note = "Copied from Stockfish-backed source window probe after near-top MultiPV DefenderTrade admission.",
        playedUci = Some("c1a3"),
        extraVariations =
          List(
            VariationLine(
              List("c2b1", "f8c5", "c1a3", "c5d4", "b3d3", "e2g4", "f1c1", "h8e8"),
              scoreCp = 58,
              depth = 16
            )
          )
      )
    ).map(scene => scene.id -> scene).toMap

  private val priorityScenes: Map[String, SceneFixture] =
    List(
      SceneFixture(
        id = "priority-MI1",
        label = "K09 shell minus rook converts by simplification",
        fen = "2bqr1k1/pp2bpp1/2n2n1p/3p4/3N4/2N1B1P1/PP2PPBP/R2Q1RK1 w - - 2 12",
        phase = "middlegame",
        ply = 24,
        scoreCp = 735,
        pvMoves = List("d4c6", "b7c6", "d1a4", "c8d7", "a4a7", "e7d6", "e3d4", "d8e7", "a1d1", "e7e6", "d4f6", "g7f6"),
        expectedTags = List("favorable_exchange", "material_imbalance"),
        note = "PlanPriority MI1."
      ),
      SceneFixture(
        id = "priority-MI4",
        label = "K09 shell minus bishop pawn prefers simplification",
        fen = "r1bqr1k1/pp3pp1/2n2n1p/3p4/3N4/2N1B1P1/PP2PPBP/R2Q1RK1 w - - 2 12",
        phase = "middlegame",
        ply = 24,
        scoreCp = 596,
        pvMoves = List("a1c1", "c8e6", "d4e6", "f7e6", "e3c5", "d8d7", "e2e4", "d5d4", "c3b5"),
        expectedTags = List("favorable_exchange", "material_imbalance"),
        note = "PlanPriority MI4."
      ),
      SceneFixture(
        id = "priority-MR1",
        label = "Hedgehog shell with direct tactical strike",
        fen = "r2qk2r/1b1nbppp/pp1ppn2/8/2PQ4/BPN2NP1/P3PPBP/R2R2K1 w kq - 2 11",
        phase = "middlegame",
        ply = 24,
        scoreCp = 250,
        pvMoves = List("a3d6", "e8g8", "a1c1", "e7d6", "d4d6", "d8c8", "e2e4", "f8d8", "d6f4", "h7h5", "d1d6", "c8c5", "c1d1"),
        expectedTags = List("immediate_tactical_gain", "strategic_background"),
        note = "PlanPriority MR1."
      ),
      SceneFixture(
        id = "priority-MR2",
        label = "open-file fight still routes through tactic first",
        fen = "2r2rk1/pp3pp1/2pq1n1p/3p4/3P4/1QP1PNRP/P4PP1/2R3K1 w - - 0 22",
        phase = "middlegame",
        ply = 24,
        scoreCp = 43,
        pvMoves = List("b3b7", "d6a3", "c1f1", "f6e4", "g3g4", "e4c3", "b7d7", "a3a2", "f3e5"),
        expectedTags = List("immediate_tactical_gain", "strategic_background"),
        note = "PlanPriority MR2."
      ),
      SceneFixture(
        id = "priority-MR3",
        label = "Dragon shell keeps attack background but forcing line leads",
        fen = "2rq1rk1/pp1bppb1/3p1np1/4n2p/3NP2P/1BN1BP2/PPPQ2P1/2KR3R w - - 0 13",
        phase = "middlegame",
        ply = 24,
        scoreCp = 29,
        pvMoves = List("c1b1", "e5c4", "b3c4", "c8c4", "d4e2", "b7b5", "e2d4", "b5b4", "c3e2", "e7e6", "b2b3"),
        expectedTags = List("immediate_tactical_gain", "attacking_piece_trade_blocker"),
        note = "PlanPriority MR3."
      ),
      SceneFixture(
        id = "priority-TO1",
        label = "won-pawn continuation stays tactical-first",
        fen = "r2q1rk1/1b1nbppp/pp1Bpn2/8/2PQ4/1PN2NP1/P3PPBP/R2R2K1 w - - 1 12",
        phase = "middlegame",
        ply = 24,
        scoreCp = 241,
        pvMoves = List("f3e5", "e7d6", "e5d7", "f6d7", "g2b7", "d6e5", "d4e3", "d8f6", "d1d7", "e5c3"),
        expectedTags = List("immediate_tactical_gain"),
        note = "PlanPriority TO1."
      ),
      SceneFixture(
        id = "priority-SC2",
        label = "Open Catalan queenside bind remains strategy-led while the pawn stays down",
        fen = "r1bq1rk1/2p1bppp/p1n1pn2/8/PppP4/2N2NP1/1PQ1PPBP/R1BR2K1 w - - 0 11",
        phase = "opening",
        ply = 24,
        scoreCp = 18,
        pvMoves = List("f3e5", "c6d4", "d1d4", "d8d4", "e5c6", "d4c5", "c6e7", "c5e7", "g2a8", "b4c3", "c2c3", "c8d7"),
        expectedTags = List("strategic_compensation", "weakness_fixation"),
        note = "PlanPriority SC2."
      ),
      SceneFixture(
        id = "priority-SC3",
        label = "Benko accepted keeps black on long-term activity",
        fen = "rn1q1rk1/4ppbp/b2p1np1/2pP4/8/2N2NP1/PP2PPBP/R1BQ1RK1 b - - 5 10",
        phase = "opening",
        ply = 24,
        scoreCp = 102,
        pvMoves = List("b8d7", "d1d2", "f6g4", "f1e1", "g7h6", "d2c2", "h6g7", "h2h3", "g4h6", "g3g4", "d8b6", "b2b3"),
        expectedTags = List("strategic_compensation", "weakness_fixation"),
        note = "PlanPriority SC3."
      ),
      SceneFixture(
        id = "priority-SC4",
        label = "Benko bishop pressure still reads as enduring compensation",
        fen = "rnbq1rk1/4ppbp/P2p1np1/2pP4/4P3/2N2N2/PP2BPPP/R1BQK2R b KQ - 4 9",
        phase = "opening",
        ply = 24,
        scoreCp = 102,
        pvMoves = List("c8a6", "e1g1", "a6e2", "d1e2", "b8d7", "f3d2", "e7e6", "d5e6", "f7e6", "f1d1", "d6d5", "e4d5", "e6d5"),
        expectedTags = List("strategic_compensation", "weakness_fixation"),
        note = "PlanPriority SC4."
      ),
      SceneFixture(
        id = "priority-SC5",
        label = "Blumenfeld compensation stays about pressure",
        fen = "rnbq1rk1/3p1pbp/P3pnp1/2pP4/4P3/2N2N2/PP2BPPP/R1BQK2R b KQ - 2 9",
        phase = "opening",
        ply = 24,
        scoreCp = 118,
        pvMoves = List("b8a6", "d5e6", "d7e6", "e1g1", "c8b7", "d1d8", "f8d8", "e4e5", "b7f3", "g2f3", "f6d5", "c1g5", "d5c3"),
        expectedTags = List("strategic_compensation", "weakness_fixation"),
        note = "PlanPriority SC5."
      ),
      SceneFixture(
        id = "priority-SC6",
        label = "Blumenfeld queenside pawn investment keeps strategic initiative alive",
        fen = "rn1qkb1r/5p1p/b2ppnp1/2pP4/4P3/2N2N1P/PP3PP1/R1BQKB1R b KQkq - 0 9",
        phase = "opening",
        ply = 24,
        scoreCp = 114,
        pvMoves = List("e6d5", "f1a6", "a8a6", "c3d5", "f8g7", "c1g5", "h7h6", "g5f6", "g7f6", "e1g1", "f6b2", "a1b1", "b2g7", "b1b7", "e8g8", "d1e2", "f8e8"),
        expectedTags = List("strategic_compensation", "weakness_fixation"),
        note = "PlanPriority SC6."
      ),
      SceneFixture(
        id = "iqp-supported-local-control",
        label = "Controlled exact IQP inducement",
        fen = "rnbqkbnr/pp3ppp/4p3/2pp4/3P4/4PN2/PP3PPP/RNBQKB1R b KQkq - 0 4",
        phase = "middlegame",
        ply = 8,
        scoreCp = 20,
        pvMoves = List("c5d4", "e3d4", "g8f6", "b1c3", "f8e7", "f1d3"),
        expectedTags = List("iqp_inducement"),
        note = "Controlled exact board/PV for supported-local IQP owner proof."
      )
    ).map(scene => scene.id -> scene).toMap

  private def softenMainClaimOwnerPath(inputs: QuestionPlannerInputs): QuestionPlannerInputs =
    inputs.copy(
      mainBundle =
        inputs.mainBundle.map(bundle =>
          bundle.copy(
            mainClaim =
              bundle.mainClaim.map(claim =>
                claim.copy(
                  packet =
                    claim.packet.map(packet =>
                      ProofContractRules.attachTrace(packet.copy(
                        sameBranchState = PlayerFacingSameBranchState.Ambiguous,
                        persistence = PlayerFacingClaimPersistence.BestDefenseOnly
                      ))
                    )
                )
              )
          )
        )
    )

  private def defaultQuestions =
    List(
      AuthorQuestion("why_this", AuthorQuestionKind.WhyThis, 100, "Why this move?"),
      AuthorQuestion("what_matters_here", AuthorQuestionKind.WhatMattersHere, 90, "What matters here?"),
      AuthorQuestion("what_changed", AuthorQuestionKind.WhatChanged, 80, "What changed?"),
      AuthorQuestion("why_now", AuthorQuestionKind.WhyNow, 60, "Why now?")
    )



  private def tacticalFailureContract(fixture: SceneFixture): DecisiveTruthContract =
    DecisiveTruthContract(
      playedMove = fixture.pvMoves.headOption,
      verifiedBestMove = fixture.pvMoves.headOption,
      truthClass = DecisiveTruthClass.Blunder,
      cpLoss = 280,
      swingSeverity = 280,
      reasonFamily = DecisiveReasonKind.TacticalRefutation,
      allowConcreteBenchmark = false,
      chosenMatchesBest = false,
      compensationAllowed = false,
      truthPhase = None,
      ownershipRole = TruthOwnershipRole.NoneRole,
      visibilityRole = TruthVisibilityRole.PrimaryVisible,
      surfaceMode = TruthSurfaceMode.FailureExplain,
      exemplarRole = TruthExemplarRole.NonExemplar,
      surfacedMoveOwnsTruth = false,
      verifiedPayoffAnchor = None,
      compensationProseAllowed = false,
      benchmarkProseAllowed = false,
      investmentTruthChainKey = None,
      maintenanceExemplarCandidate = false,
      failureMode = FailureInterpretationMode.TacticalRefutation,
      failureIntentConfidence = 0.0,
      failureIntentAnchor = None,
      failureInterpretationAllowed = true
    )

  private def releaseLabel(
      sample: Sample,
      ranked: RankedQuestionPlans,
      primary: String,
      moveReview: String,
      baselineRelease: Option[String]
  ): String =
     ranked.primary match
      case Some(_) if sample.softenOwnerPath && (primary.startsWith("A local reading") || primary.startsWith("A key idea")) =>
        "SupportedLocal"
      case Some(plan) if positiveRelease(plan).nonEmpty =>
        positiveRelease(plan).get
      case Some(plan) =>
        s"Other:${plan.plannerOwnerKind.wireName}"
      case None if sample.tacticalContract &&
          !strategicLeak(primary, moveReview) &&
          (baselineRelease.nonEmpty || ranked.rejected.exists(_.reasons.contains("strategic_claim_tactical_veto"))) =>
        "TacticalVeto"
      case None =>
        "Suppressed"

  private def positiveRelease(plan: QuestionPlan): Option[String] =
    if plan.admissibilityReasons.contains("strategic_claim_supported_local") then Some("SupportedLocal")
    else if plan.admissibilityReasons.contains("certified_position_probe") ||
        plan.admissibilityReasons.contains("exact_target_state_delta") ||
        plan.sourceKinds.exists(_ == PlanTaxonomy.PlanKind.SimplificationWindow.id) ||
        plan.claim.toLowerCase.contains("same local edge")
    then Some("CertifiedOwner")
    else None

  private def strategicLeak(values: String*): Boolean =
    val text = values.mkString(" ").toLowerCase
    List(
      "key strategic fact",
      "fixed target",
      "pressure coordinated",
      "same local edge",
      "simplification",
      "target fixation",
      "local reading",
      "key idea",
      "further probe work",
      "probe work still targets"
    ).exists(text.contains)

  private def clean(raw: String): String =
    raw.replaceAll("\\s+", " ").trim

  private def summary(observations: List[Observation]): String =
    val counts =
      observations.groupBy(_.release).view.mapValues(_.size).toList.sortBy(_._1)
    val leaks = observations.filter(_.leak).map(_.sample.id)
    s"summary=${counts.map { case (k, v) => s"$k=$v" }.mkString(", ")}; leaks=${if leaks.isEmpty then "none" else leaks.mkString(",")}"

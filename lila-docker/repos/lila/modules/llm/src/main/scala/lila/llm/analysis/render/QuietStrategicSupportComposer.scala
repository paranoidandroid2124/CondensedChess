package lila.llm.analysis.render

import lila.llm.{ NarrativeSignalDigest, StrategyPack }
import lila.llm.analysis.*
import lila.llm.model.NarrativeContext

private[llm] object QuietStrategicSupportComposer:

  object Bucket:
    val LongStructuralSqueeze = "long_structural_squeeze"
    val SlowRouteImprovement = "slow_route_improvement"
    val PressureMaintenanceWithoutImmediateTactic =
      "pressure_maintenance_without_immediate_tactic"

  object VerbFamily:
    val KeepsAvailable = "keeps available"
    val MaintainsPressure = "maintains pressure"
    val Limits = "limits"
    val Reinforces = "reinforces"

  final case class QuietStrategicSupportLine(
      text: String,
      bucket: String,
      sourceKinds: List[String],
      verbFamily: String
  )

  final case class QuietStrategicSupportGateTrace(
      sceneType: String,
      selectedOwnerFamily: Option[String],
      selectedOwnerSource: Option[String],
      pvDeltaAvailable: Boolean,
      signalDigestAvailable: Boolean,
      openingRelationClaimPresent: Boolean,
      endgameTransitionClaimPresent: Boolean,
      moveLinkedPvDeltaAnchorAvailable: Boolean,
      rejectReasons: List[String]
  )

  final case class QuietStrategicSupportTrace(
      emitted: Boolean,
      line: Option[QuietStrategicSupportLine],
      rejectReasons: List[String],
      gatePassed: Boolean,
      gate: QuietStrategicSupportGateTrace
  )

  private final case class Candidate(
      priority: Int,
      line: QuietStrategicSupportLine
  )

  private val AllowedSceneTypes =
    Set(SceneType.QuietImprovement, SceneType.TransitionConversion)

  private val ForbiddenVerbPatterns = List(
    """\bprepare\w*\b""".r,
    """\blaunch\w*\b""".r,
    """\bforce\w*\b""".r,
    """\bsecure\w*\b""".r,
    """\bneutraliz\w*\b""".r
  )

  private val SquarePattern = """(?i)\b([a-h][1-8])\b""".r

  def compose(
      ctx: NarrativeContext,
      inputs: QuestionPlannerInputs,
      rankedPlans: RankedQuestionPlans,
      strategyPack: Option[StrategyPack]
  ): Option[QuietStrategicSupportLine] =
    val trace = diagnose(ctx, inputs, rankedPlans, strategyPack)
    Option.when(trace.emitted)(trace.line).flatten

  def diagnose(
      ctx: NarrativeContext,
      inputs: QuestionPlannerInputs,
      rankedPlans: RankedQuestionPlans,
      strategyPack: Option[StrategyPack]
  ): QuietStrategicSupportTrace =
    val digest = strategyPack.flatMap(_.signalDigest)
    val moveLinkedPvDeltaAnchorAvailable = hasMoveLinkedPvDeltaAnchor(rankedPlans)
    val gateReasons =
      List(
        Option.when(inputs.pvDelta.isEmpty)("pv_delta_missing"),
        Option.when(digest.isEmpty)("signal_digest_missing"),
        Option.when(inputs.openingRelationClaim.nonEmpty)("opening_relation_claim_present"),
        Option.when(inputs.endgameTransitionClaim.nonEmpty)("endgame_transition_claim_present"),
        Option.when(!AllowedSceneTypes.contains(rankedPlans.ownerTrace.sceneType))(
          s"scene_type_not_allowed:${rankedPlans.ownerTrace.sceneType.wireName}"
        ),
        rankedPlans.ownerTrace.selectedOwnerFamily.collect {
          case family if family != OwnerFamily.MoveDelta => s"selected_owner_family_not_movedelta:${family.wireName}"
        },
        Option.when(!moveLinkedPvDeltaAnchorAvailable)("move_linked_pv_delta_anchor_missing")
      ).flatten
    val gateTrace =
      QuietStrategicSupportGateTrace(
        sceneType = rankedPlans.ownerTrace.sceneType.wireName,
        selectedOwnerFamily = rankedPlans.ownerTrace.selectedOwnerFamily.map(_.wireName),
        selectedOwnerSource = rankedPlans.ownerTrace.selectedOwnerSource,
        pvDeltaAvailable = inputs.pvDelta.nonEmpty,
        signalDigestAvailable = digest.nonEmpty,
        openingRelationClaimPresent = inputs.openingRelationClaim.nonEmpty,
        endgameTransitionClaimPresent = inputs.endgameTransitionClaim.nonEmpty,
        moveLinkedPvDeltaAnchorAvailable = moveLinkedPvDeltaAnchorAvailable,
        rejectReasons = gateReasons
      )

    if gateReasons.nonEmpty then
      QuietStrategicSupportTrace(
        emitted = false,
        line = None,
        rejectReasons = gateReasons,
        gatePassed = false,
        gate = gateTrace
      )
    else
      diagnoseCandidate(ctx, inputs, digest.get, gateTrace)

  private def diagnoseCandidate(
      ctx: NarrativeContext,
      inputs: QuestionPlannerInputs,
      digest: NarrativeSignalDigest,
      gateTrace: QuietStrategicSupportGateTrace
  ): QuietStrategicSupportTrace =
    val structure = structureCandidate(digest)
    val route = routeCandidate(inputs, digest)
    val pressure = pressureCandidate(inputs, digest)
    val candidates = List(structure, route, pressure).flatten
    if candidates.isEmpty then
      QuietStrategicSupportTrace(
        emitted = false,
        line = None,
        rejectReasons =
          List(
            Option.when(structure.isEmpty)("digest_structure_missing"),
            Option.when(route.isEmpty)("digest_route_missing"),
            Option.when(pressure.isEmpty)("digest_pressure_missing"),
            Some("no_whitelisted_quiet_candidate")
          ).flatten,
        gatePassed = true,
        gate = gateTrace
      )
    else
      val selected = candidates.sortBy(candidate => -candidate.priority).head.line
      BookmakerLiveCompressionPolicy.cleanNarrativeSentence(selected.text, ctx) match
        case None =>
          QuietStrategicSupportTrace(
            emitted = false,
            line = None,
            rejectReasons = List("candidate_cleaned_empty"),
            gatePassed = true,
            gate = gateTrace
          )
        case Some(text) =>
          val cleaned = selected.copy(text = text)
          if hasForbiddenVerbLeakage(text) then
            QuietStrategicSupportTrace(
              emitted = false,
              line = Some(cleaned),
              rejectReasons = List("candidate_forbidden_verb_leakage"),
              gatePassed = true,
              gate = gateTrace
            )
          else
            QuietStrategicSupportTrace(
              emitted = true,
              line = Some(cleaned),
              rejectReasons = Nil,
              gatePassed = true,
              gate = gateTrace
            )

  private def hasMoveLinkedPvDeltaAnchor(
      rankedPlans: RankedQuestionPlans
  ): Boolean =
    rankedPlans.primary.exists(eligibleMoveDeltaPlan) ||
      rankedPlans.secondary.exists(eligibleMoveDeltaPlan) ||
      rankedPlans.ownerTrace.ownerCandidates.exists(eligibleMoveDeltaCandidate)

  private def eligibleMoveDeltaPlan(
      plan: QuestionPlan
  ): Boolean =
    plan.ownerFamily == OwnerFamily.MoveDelta &&
      plan.ownerSource == "pv_delta" &&
      plan.sourceKinds.exists(kind => kind == "pv_delta" || kind == "move_delta")

  private def eligibleMoveDeltaCandidate(
      candidate: OwnerCandidateTrace
  ): Boolean =
    candidate.family == OwnerFamily.MoveDelta &&
      candidate.source == "pv_delta" &&
      candidate.moveLinked &&
      candidate.materiality == OwnerCandidateMateriality.OwnerCandidate &&
      candidate.admissionDecision.exists(decision =>
        decision == AdmissionDecision.PrimaryAllowed || decision == AdmissionDecision.SupportOnly
      )

  private def structureCandidate(
      digest: NarrativeSignalDigest
  ): Option[Candidate] =
    digest.structuralCue.flatMap(nounPhrase).map { cue =>
      val priority = if squeezeCue(cue) then 40 else 25
      Candidate(
        priority = priority,
        line =
          QuietStrategicSupportLine(
            text = s"This reinforces ${withArticle(cue)}.",
            bucket = Bucket.LongStructuralSqueeze,
            sourceKinds = List("MoveDelta.pv_delta", "Digest.structure"),
            verbFamily = VerbFamily.Reinforces
          )
      )
    }

  private def routeCandidate(
      inputs: QuestionPlannerInputs,
      digest: NarrativeSignalDigest
  ): Option[Candidate] =
    Option.when(digest.deploymentRoute.nonEmpty) {
      val routePlan =
        inputs.pvDelta.toList
          .flatMap(_.planAdvancements)
          .flatMap(routePlanPhrase)
          .headOption
      val routeTarget =
        extractSquares(digest.deploymentRoute.mkString(" ")).lastOption

      routePlan
        .map(plan =>
          s"This keeps ${withArticle(plan)} available."
        )
        .orElse(
          routeTarget.map(square => s"This keeps the route toward $square available.")
        )
        .map { sentence =>
          Candidate(
            priority = 30,
            line =
              QuietStrategicSupportLine(
                text = sentence,
                bucket = Bucket.SlowRouteImprovement,
                sourceKinds = List("MoveDelta.pv_delta", "Digest.route"),
                verbFamily = VerbFamily.KeepsAvailable
              )
          )
        }
    }.flatten

  private def pressureCandidate(
      inputs: QuestionPlannerInputs,
      digest: NarrativeSignalDigest
  ): Option[Candidate] =
    digest.practicalVerdict.flatMap(cleanFragment).flatMap { verdict =>
      val pressureSentence =
        pressureTarget(verdict)
          .map(target => s"This maintains pressure on $target.")
          .orElse(
            inputs.pvDelta.toList
              .flatMap(_.newOpportunities)
              .flatMap(extractSquares)
              .headOption
              .map(square => s"This maintains pressure on $square.")
          )
      val restrictionSentence =
        Option.when(verdict.toLowerCase.contains("counterplay")) {
          s"This limits ${counterplayFragment(verdict)}."
        }

      pressureSentence
        .orElse(restrictionSentence)
        .map { sentence =>
          val verbFamily =
            if sentence.toLowerCase.contains("limits ") then VerbFamily.Limits
            else VerbFamily.MaintainsPressure
          Candidate(
            priority = 20,
            line =
              QuietStrategicSupportLine(
                text = sentence,
                bucket = Bucket.PressureMaintenanceWithoutImmediateTactic,
                sourceKinds = List("MoveDelta.pv_delta", "Digest.pressure"),
                verbFamily = verbFamily
              )
          )
        }
    }

  private def routePlanPhrase(
      raw: String
  ): Option[String] =
    cleanFragment(raw)
      .map(_.replaceFirst("(?i)^(met|path|route):\\s*", ""))
      .flatMap(nounPhrase)

  private def nounPhrase(
      raw: String
  ): Option[String] =
    cleanFragment(raw).map(_.replaceAll("""[.;]+$""", "").trim).filter(_.nonEmpty)

  private def cleanFragment(
      raw: String
  ): Option[String] =
    Option(raw)
      .map(_.trim)
      .filter(_.nonEmpty)
      .map(_.replaceAll("""\s+""", " "))
      .map(_.split("[;]").headOption.getOrElse("").trim)
      .filter(_.nonEmpty)

  private def withArticle(
      raw: String
  ): String =
    val trimmed = raw.trim
    val lowered = trimmed.toLowerCase
    if lowered.startsWith("a ") || lowered.startsWith("an ") || lowered.startsWith("the ") then trimmed
    else s"the $trimmed"

  private def pressureTarget(
      verdict: String
  ): Option[String] =
    val lowered = verdict.toLowerCase
    val marker =
      List(
        "pressure remains on ",
        "pressure stays on ",
        "pressure is on ",
        "pressure on "
      ).find(lowered.contains)
    marker.flatMap { prefix =>
      val target = verdict.drop(lowered.indexOf(prefix) + prefix.length).trim
      cleanFragment(target).map(_.stripSuffix("."))
    }

  private def counterplayFragment(
      verdict: String
  ): String =
    val lowered = verdict.toLowerCase
    val idx = lowered.indexOf("counterplay")
    if idx <= 0 then "the counterplay"
    else
      val prefix =
        verdict
          .substring(0, idx)
          .split("""[,:;]""")
          .lastOption
          .getOrElse("")
          .trim
      val candidate = s"$prefix counterplay".trim
      withArticle(candidate.replaceAll("""\s+""", " ").trim)

  private def squeezeCue(
      cue: String
  ): Boolean =
    List("bind", "squeeze", "grip", "clamp", "space", "structure", "center", "file")
      .exists(marker => cue.toLowerCase.contains(marker))

  private def extractSquares(
      raw: String
  ): List[String] =
    SquarePattern.findAllMatchIn(Option(raw).getOrElse("")).map(_.group(1).toLowerCase).toList.distinct

  private def hasForbiddenVerbLeakage(
      text: String
  ): Boolean =
    val lowered = Option(text).getOrElse("").toLowerCase
    ForbiddenVerbPatterns.exists(_.findFirstIn(lowered).nonEmpty)

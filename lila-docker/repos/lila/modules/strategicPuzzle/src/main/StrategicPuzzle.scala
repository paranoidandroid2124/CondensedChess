package lila.strategicPuzzle

import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.UUID

import play.api.libs.json.*
import play.api.libs.functional.syntax.*

import lila.core.userId.UserId

object StrategicPuzzle:

  val PublicSelectionStatus = "pre_polish_top_1000"
  val RuntimeShellSchemaV2 = "chesstory.strategicPuzzle.runtimeShell.v2"
  val RuntimeShellSchema = RuntimeShellSchemaV2
  val AttemptSchema = "chesstory.strategicPuzzleAttempt.v2"

  val StatusFull = "full"
  val StatusPartial = "partial"
  val StatusWrong = "wrong"
  val StatusGiveUp = "giveup"

  final case class SourcePayload(
      seedId: String,
      opening: Option[String],
      eco: Option[String]
  )
  object SourcePayload:
    given OFormat[SourcePayload] = Json.format[SourcePayload]

  final case class PositionPayload(
      fen: String,
      sideToMove: String
  )
  object PositionPayload:
    given OFormat[PositionPayload] = Json.format[PositionPayload]

  final case class DominantFamilySummary(
      key: String,
      dominantIdeaKind: String,
      anchor: String
  )
  object DominantFamilySummary:
    given OFormat[DominantFamilySummary] = Json.format[DominantFamilySummary]

  final case class QualityScore(total: Int)
  object QualityScore:
    given OFormat[QualityScore] = Json.format[QualityScore]

  final case class GenerationMeta(selectionStatus: String)
  object GenerationMeta:
    given OFormat[GenerationMeta] = Json.format[GenerationMeta]

  final case class ShellChoice(
      uci: String,
      san: String,
      credit: String,
      nextNodeId: Option[String],
      terminalId: Option[String],
      afterFen: Option[String],
      familyKey: Option[String],
      label: Option[String],
      feedback: String
  )
  object ShellChoice:
    given OFormat[ShellChoice] = Json.format[ShellChoice]

  final case class PlanStart(
      uci: String,
      san: String,
      credit: String,
      label: Option[String],
      feedback: String,
      afterFen: Option[String],
      terminalId: Option[String]
  )
  object PlanStart:
    given OFormat[PlanStart] = Json.format[PlanStart]

  final case class PuzzlePlan(
      id: String,
      familyKey: Option[String],
      dominantIdeaKind: Option[String],
      anchor: Option[String],
      task: String,
      feedback: String,
      allowedStarts: List[PlanStart],
      featuredTerminalId: String,
      featuredStartUci: Option[String] = None
  )
  object PuzzlePlan:
    given OFormat[PuzzlePlan] = Json.using[Json.WithDefaultValues].format[PuzzlePlan]

  final case class RuntimeProofLayer(
      rootChoices: List[ShellChoice] = Nil,
      nodes: List[PlayerNode] = Nil,
      forcedReplies: List[ForcedReply] = Nil
  )
  object RuntimeProofLayer:
    given OFormat[RuntimeProofLayer] = Json.using[Json.WithDefaultValues].format[RuntimeProofLayer]

  final case class ForcedReply(
      id: String,
      fromNodeId: String,
      uci: String,
      san: String,
      afterFen: String,
      nextNodeId: Option[String]
  )
  object ForcedReply:
    given OFormat[ForcedReply] = Json.format[ForcedReply]

  final case class PlayerNode(
      id: String,
      step: Int,
      fen: String,
      prompt: String,
      badMoveFeedback: String,
      choices: List[ShellChoice]
  )
  object PlayerNode:
    given OFormat[PlayerNode] = Json.format[PlayerNode]

  final case class TerminalReveal(
      id: String,
      outcome: String,
      title: String,
      summary: String,
      commentary: String,
      familyKey: Option[String],
      dominantIdeaKind: Option[String],
      anchor: Option[String],
      lineSan: List[String],
      siblingMoves: List[String],
      opening: Option[String],
      eco: Option[String],
      dominantFamilyKey: Option[String],
      planId: Option[String] = None,
      planTask: Option[String] = None,
      whyPlan: Option[String] = None,
      whyMove: Option[String] = None,
      acceptedStarts: List[String] = Nil,
      featuredStart: Option[String] = None
  )
  object TerminalReveal:
    given OFormat[TerminalReveal] = Json.using[Json.WithDefaultValues].format[TerminalReveal]

  final case class RuntimeShell(
      schema: String,
      startFen: String,
      sideToMove: String,
      prompt: String,
      plans: List[PuzzlePlan] = Nil,
      proof: RuntimeProofLayer,
      terminals: List[TerminalReveal]
  ):
    lazy val proofLayer: RuntimeProofLayer = proof
    lazy val planById: Map[String, PuzzlePlan] = plans.map(plan => plan.id -> plan).toMap
    lazy val nodeById: Map[String, PlayerNode] = proofLayer.nodes.map(node => node.id -> node).toMap
    lazy val replyByNodeId: Map[String, ForcedReply] = proofLayer.forcedReplies.map(reply => reply.fromNodeId -> reply).toMap
    lazy val terminalById: Map[String, TerminalReveal] = terminals.map(terminal => terminal.id -> terminal).toMap
    def materialize(dominantFamily: Option[DominantFamilySummary]): RuntimeShell =
      materializeRuntimeShell(this, dominantFamily)
  object RuntimeShell:
    given Reads[RuntimeShell] =
      (
        (__ \ "schema").read[String] and
          (__ \ "startFen").read[String] and
          (__ \ "sideToMove").read[String] and
          (__ \ "prompt").read[String] and
          (__ \ "plans").readWithDefault[List[PuzzlePlan]](Nil) and
          (__ \ "proof").read[RuntimeProofLayer] and
          (__ \ "terminals").read[List[TerminalReveal]]
      )(RuntimeShell.apply)

    given OWrites[RuntimeShell] = OWrites { shell =>
      Json.obj(
        "schema" -> shell.schema,
        "startFen" -> shell.startFen,
        "sideToMove" -> shell.sideToMove,
        "prompt" -> shell.prompt,
        "plans" -> shell.plans,
        "proof" -> shell.proof,
        "terminals" -> shell.terminals
      )
    }

    given OFormat[RuntimeShell] = OFormat(summon[Reads[RuntimeShell]], summon[OWrites[RuntimeShell]])

  final case class StrategicPuzzleDoc(
      id: String,
      schema: String,
      source: SourcePayload,
      position: PositionPayload,
      dominantFamily: Option[DominantFamilySummary],
      qualityScore: QualityScore,
      generationMeta: GenerationMeta,
      runtimeShell: Option[RuntimeShell]
  )
  object StrategicPuzzleDoc:
    given OFormat[StrategicPuzzleDoc] = Json.format[StrategicPuzzleDoc]

  final case class AttemptSummary(
      puzzleId: String,
      status: String,
      completedAt: String
  )
  object AttemptSummary:
    given OFormat[AttemptSummary] = Json.format[AttemptSummary]

  final case class ProgressPayload(
      authenticated: Boolean,
      currentStreak: Int,
      recentAttempts: List[AttemptSummary]
  )
  object ProgressPayload:
    given OFormat[ProgressPayload] = Json.format[ProgressPayload]

  final case class BootstrapPayload(
      puzzle: StrategicPuzzleDoc,
      runtimeShell: RuntimeShell,
      progress: ProgressPayload
  )
  object BootstrapPayload:
    given OFormat[BootstrapPayload] = Json.format[BootstrapPayload]

  final case class CompleteRequest(
      giveUp: Boolean = false,
      planId: Option[String] = None,
      startUci: Option[String] = None
  )
  object CompleteRequest:
    given OFormat[CompleteRequest] = Json.using[Json.WithDefaultValues].format[CompleteRequest]

  final case class CompleteResponse(
      saved: Boolean,
      currentStreak: Int,
      nextPuzzleId: Option[String],
      nextPuzzleUrl: Option[String]
  )
  object CompleteResponse:
    given OFormat[CompleteResponse] = Json.format[CompleteResponse]

  enum CompleteOutcome:
    case Invalid
    case MissingPuzzle
    case Success(response: CompleteResponse, next: Option[BootstrapPayload])

  final case class AttemptDoc(
      _id: String,
      schema: String,
      userId: String,
      puzzleId: String,
      status: String,
      lineUcis: List[String],
      startUci: Option[String] = None,
      planId: Option[String] = None,
      terminalId: Option[String],
      terminalFamilyKey: Option[String],
      completedAt: String
  )
  object AttemptDoc:
    given OFormat[AttemptDoc] = Json.using[Json.WithDefaultValues].format[AttemptDoc]

  final case class ResolvedCompletion(
      status: String,
      lineUcis: List[String],
      planId: Option[String] = None,
      startUci: Option[String] = None,
      terminalId: Option[String],
      terminalFamilyKey: Option[String]
  ):
    def cleared: Boolean = status == StatusFull

  def resolveCompletion(shell: RuntimeShell, req: CompleteRequest): Option[ResolvedCompletion] =
    if req.giveUp then featuredCompletion(shell, req.planId)
    else
      for
        planId <- req.planId
        startUci <- req.startUci
        resolved <- selectedStartCompletion(shell, planId, startUci)
      yield resolved

  def currentIsoInstant(): String =
    DateTimeFormatter.ISO_INSTANT.format(Instant.now())

  def newAttempt(
      userId: UserId,
      puzzleId: String,
      status: String,
      lineUcis: List[String],
      planId: Option[String],
      startUci: Option[String],
      terminalId: Option[String],
      terminalFamilyKey: Option[String]
  ): AttemptDoc =
    AttemptDoc(
      _id = s"spa_${UUID.randomUUID().toString.replace("-", "")}",
      schema = AttemptSchema,
      userId = userId.value,
      puzzleId = puzzleId,
      status = status,
      lineUcis = lineUcis,
      startUci = startUci.orElse(lineUcis.headOption),
      planId = planId,
      terminalId = terminalId,
      terminalFamilyKey = terminalFamilyKey,
      completedAt = currentIsoInstant()
    )

  def progress(authenticated: Boolean, streak: Int, recent: List[AttemptDoc]): ProgressPayload =
    ProgressPayload(
      authenticated = authenticated,
      currentStreak = streak,
      recentAttempts = recent.map(a => AttemptSummary(a.puzzleId, a.status, a.completedAt))
    )

  def computeStreak(attempts: List[AttemptDoc]): Int =
    latestByPuzzle(attempts).iterator.takeWhile(_.status == StatusFull).length

  private final case class ProofResolution(
      terminal: TerminalReveal,
      lineUcis: List[String],
      lineSans: List[String],
      finalFen: String
  )

  private final case class DerivedPlanStart(
      choice: ShellChoice,
      proof: ProofResolution,
      rootIndex: Int
  )

  private def featuredCompletion(shell: RuntimeShell, requestedPlanId: Option[String]): Option[ResolvedCompletion] =
    requestedPlanId
      .flatMap(shell.planById.get)
      .orElse(featuredPlan(shell))
      .flatMap { plan =>
        plan.featuredStartUci
          .orElse(plan.allowedStarts.headOption.map(_.uci))
          .flatMap(startUci => selectedStartCompletion(shell, plan.id, startUci, Some(StatusGiveUp)))
      }

  private def selectedStartCompletion(
      shell: RuntimeShell,
      planId: String,
      startUci: String,
      statusOverride: Option[String] = None
  ): Option[ResolvedCompletion] =
    shell.planById.get(planId).flatMap { plan =>
      plan.allowedStarts.find(_.uci == startUci).flatMap { start =>
        canonicalProofForStart(shell, startUci).filter(proofBelongsToPlan(plan, _)).map { proof =>
          ResolvedCompletion(
            status = statusOverride.getOrElse(normalizeTerminalOutcome(start.credit)),
            lineUcis = proof.lineUcis,
            planId = Some(plan.id),
            startUci = Some(startUci),
            terminalId = Some(proof.terminal.id),
            terminalFamilyKey = proof.terminal.familyKey.orElse(proof.terminal.dominantFamilyKey)
          )
        }
      }
    }

  private def normalizeTerminalOutcome(outcome: String): String =
    outcome match
      case StatusFull | StatusPartial | StatusWrong | StatusGiveUp => outcome
      case _                                                       => StatusWrong

  private def replyLookup(currentNodeId: Option[String], playerUci: String): String =
    currentNodeId.fold(s"root:$playerUci")(nodeId => s"$nodeId:$playerUci")

  private def materializeRuntimeShell(
      shell: RuntimeShell,
      dominantFamily: Option[DominantFamilySummary]
  ): RuntimeShell =
    val publicPlans = if shell.plans.nonEmpty then shell.plans else derivePlans(shell, dominantFamily)
    val enrichedTerminals = shell.terminals.map(enrichTerminalReveal(_, publicPlans))
    shell.copy(schema = RuntimeShellSchemaV2, plans = publicPlans, terminals = enrichedTerminals)

  private def derivePlans(
      shell: RuntimeShell,
      dominantFamily: Option[DominantFamilySummary]
  ): List[PuzzlePlan] =
    val derivedStarts =
      shell.proofLayer.rootChoices.zipWithIndex.flatMap { case (choice, rootIndex) =>
        canonicalProofForChoice(shell, None, choice).map(proof => DerivedPlanStart(choice, proof, rootIndex))
      }
    val grouped =
      derivedStarts.groupBy(start => planGroupingKey(start.choice, start.proof.terminal, dominantFamily))
    grouped.values.toList
      .flatMap(buildPlan(shell, dominantFamily, _))
      .sortBy(plan => (planPriority(plan, dominantFamily), plan.allowedStarts.headOption.fold(Int.MaxValue)(startPriority), plan.id))

  private def buildPlan(
      shell: RuntimeShell,
      dominantFamily: Option[DominantFamilySummary],
      entries: List[DerivedPlanStart]
  ): Option[PuzzlePlan] =
    val featured =
      entries.sortBy(entry => (startPriority(toPlanStart(entry.choice)), outcomePriority(entry.proof.terminal.outcome), entry.rootIndex)).headOption
    featured.map { picked =>
      val terminal = picked.proof.terminal
      val familyKey =
        terminal.dominantFamilyKey
          .orElse(terminal.familyKey)
          .orElse(picked.choice.familyKey)
          .orElse(dominantFamily.map(_.key))
      val dominantIdeaKind = terminal.dominantIdeaKind.orElse(dominantFamily.map(_.dominantIdeaKind))
      val anchor = terminal.anchor.orElse(dominantFamily.map(_.anchor))
      val task = derivePlanTask(shell.prompt, terminal, familyKey, dominantIdeaKind, anchor)
      val starts =
        entries
          .sortBy(entry => (startPriority(toPlanStart(entry.choice)), entry.rootIndex))
          .map(_.choice)
          .distinctBy(_.uci)
          .map(toPlanStart)
      val feedback = derivePlanFeedback(task, terminal, starts.headOption)
      PuzzlePlan(
        id = stablePlanId(familyKey, terminal, starts.headOption.map(_.uci)),
        familyKey = familyKey,
        dominantIdeaKind = dominantIdeaKind,
        anchor = anchor,
        task = task,
        feedback = feedback,
        allowedStarts = starts,
        featuredTerminalId = terminal.id,
        featuredStartUci = starts.headOption.map(_.uci)
      )
    }

  private def enrichTerminalReveal(
      terminal: TerminalReveal,
      plans: List[PuzzlePlan]
  ): TerminalReveal =
    val matchedPlan =
      terminal.planId.flatMap(planId => plans.find(_.id == planId)).orElse {
        plans.find(plan => plan.featuredTerminalId == terminal.id)
      }.orElse {
        val terminalFamily = terminal.dominantFamilyKey.orElse(terminal.familyKey)
        terminalFamily.flatMap(family => plans.find(_.familyKey.contains(family)))
      }
    val acceptedStarts =
      terminal.acceptedStarts match
        case starts if starts.nonEmpty => starts
        case _                         => matchedPlan.fold(terminal.siblingMoves)(_.allowedStarts.map(_.san))
    val featuredStart =
      terminal.featuredStart.orElse {
        matchedPlan.flatMap(plan =>
          plan.featuredStartUci.flatMap(uci => plan.allowedStarts.find(_.uci == uci).map(_.san))
            .orElse(plan.allowedStarts.headOption.map(_.san))
        )
      }
    terminal.copy(
      planId = terminal.planId.orElse(matchedPlan.map(_.id)),
      planTask = terminal.planTask.orElse(matchedPlan.map(_.task)),
      whyPlan = terminal.whyPlan.orElse(nonBlank(terminal.summary)).orElse(matchedPlan.map(_.feedback)),
      whyMove =
        terminal.whyMove.orElse {
          matchedPlan.flatMap(plan =>
            plan.featuredStartUci.flatMap(uci => plan.allowedStarts.find(_.uci == uci).flatMap(start => nonBlank(start.feedback)))
          )
        }.orElse(firstParagraph(terminal.commentary)),
      acceptedStarts = acceptedStarts,
      featuredStart = featuredStart,
      siblingMoves =
        if terminal.siblingMoves.nonEmpty then terminal.siblingMoves
        else acceptedStarts.filterNot(start => featuredStart.contains(start))
    )

  private def canonicalProofForStart(shell: RuntimeShell, startUci: String): Option[ProofResolution] =
    shell.proofLayer.rootChoices.find(_.uci == startUci).flatMap(choice => canonicalProofForChoice(shell, None, choice))

  private def canonicalProofForChoice(
      shell: RuntimeShell,
      nodeId: Option[String],
      choice: ShellChoice,
      hop: Int = 0,
      lineUcis: Vector[String] = Vector.empty,
      lineSans: Vector[String] = Vector.empty,
      currentFen: Option[String] = None
  ): Option[ProofResolution] =
    if hop >= 5 then None
    else
      val baseFen = currentFen.getOrElse(shell.startFen)
      val afterPlayerFen = choice.afterFen.getOrElse(baseFen)
      val withPlayerUcis = lineUcis :+ choice.uci
      val withPlayerSans = lineSans :+ choice.san
      choice.terminalId.flatMap(shell.terminalById.get).map { terminal =>
        ProofResolution(
          terminal = terminal,
          lineUcis = withPlayerUcis.toList,
          lineSans = withPlayerSans.toList,
          finalFen = afterPlayerFen
        )
      }.orElse {
        shell.replyByNodeId.get(replyLookup(nodeId, choice.uci)).flatMap { reply =>
          val withReplyUcis = withPlayerUcis :+ reply.uci
          val withReplySans = withPlayerSans :+ reply.san
          reply.nextNodeId.flatMap { nextNodeId =>
            shell.nodeById
              .get(nextNodeId)
              .flatMap(node => node.choices.find(_.credit == StatusFull).orElse(node.choices.headOption))
              .flatMap(nextChoice =>
                canonicalProofForChoice(shell, Some(nextNodeId), nextChoice, hop + 1, withReplyUcis, withReplySans, Some(reply.afterFen))
              )
          }
        }
      }

  private def planGroupingKey(
      choice: ShellChoice,
      terminal: TerminalReveal,
      dominantFamily: Option[DominantFamilySummary]
  ): String =
    terminal.planId
      .orElse(terminal.dominantFamilyKey)
      .orElse(terminal.familyKey)
      .orElse(choice.familyKey)
      .orElse(dominantFamily.map(_.key))
      .getOrElse(s"terminal:${terminal.id}")

  private def proofBelongsToPlan(plan: PuzzlePlan, proof: ProofResolution): Boolean =
    proof.terminal.planId.contains(plan.id) ||
      plan.featuredTerminalId == proof.terminal.id ||
      plan.familyKey.exists(family => proof.terminal.dominantFamilyKey.contains(family) || proof.terminal.familyKey.contains(family))

  private def featuredPlan(shell: RuntimeShell): Option[PuzzlePlan] =
    shell.plans.sortBy(plan => (planPriority(plan, none[DominantFamilySummary]), plan.allowedStarts.headOption.fold(Int.MaxValue)(startPriority), plan.id)).headOption

  private def toPlanStart(choice: ShellChoice): PlanStart =
    PlanStart(
      uci = choice.uci,
      san = choice.san,
      credit = choice.credit,
      label = choice.label,
      feedback = choice.feedback,
      afterFen = choice.afterFen,
      terminalId = choice.terminalId
    )

  private def startPriority(start: PlanStart): Int =
    start.credit match
      case StatusFull    => 0
      case StatusPartial => 1
      case _             => 2

  private def outcomePriority(outcome: String): Int =
    outcome match
      case StatusFull    => 0
      case StatusPartial => 1
      case StatusWrong   => 2
      case StatusGiveUp  => 3
      case _             => 4

  private def planPriority(plan: PuzzlePlan, dominantFamily: Option[DominantFamilySummary]): Int =
    val familyBonus = if dominantFamily.exists(df => plan.familyKey.contains(df.key)) then 0 else 1
    val featuredStartBonus = plan.allowedStarts.headOption.fold(1)(startPriority)
    familyBonus * 10 + featuredStartBonus

  private def stablePlanId(
      familyKey: Option[String],
      terminal: TerminalReveal,
      featuredStartUci: Option[String]
  ): String =
    val raw = familyKey.orElse(terminal.planId).orElse(terminal.dominantFamilyKey).orElse(terminal.familyKey).getOrElse(terminal.id)
    val suffix = featuredStartUci.getOrElse(terminal.id)
    s"plan_${slugify(raw)}_${slugify(suffix).take(12)}".take(64)

  private def derivePlanTask(
      shellPrompt: String,
      terminal: TerminalReveal,
      familyKey: Option[String],
      dominantIdeaKind: Option[String],
      anchor: Option[String]
  ): String =
    terminal.planTask.flatMap(nonBlank).orElse {
      dominantIdeaKind match
        case Some("counterplay_suppression") | Some("prophylaxis") =>
          anchor.map(a => s"Shut down the opponent's counterplay around ${anchorPhrase(a)} before starting your own play.")
        case Some("favorable_trade_or_transformation") =>
          anchor.map(a => s"Prepare the favorable transition toward ${anchorPhrase(a)}.")
        case Some("line_occupation") =>
          anchor.map(a => s"Take control of ${anchorPhrase(a)} and keep that grip.")
        case Some("space_gain_or_restriction") =>
          anchor.map(a => s"Tighten the bind around ${anchorPhrase(a)} before the opponent breaks free.")
        case Some("pawn_break") =>
          anchor.map(a => s"Prepare the break around ${anchorPhrase(a)} under the right conditions.")
        case Some("outpost_creation_or_occupation") =>
          anchor.map(a => s"Secure the outpost on ${anchorPhrase(a)} and build around it.")
        case Some("target_fixing") =>
          anchor.map(a => s"Fix the target around ${anchorPhrase(a)} before switching play.")
        case Some("minor_piece_imbalance_exploitation") =>
          anchor.map(a => s"Exploit the minor-piece imbalance around ${anchorPhrase(a)}.")
        case Some("king_attack_build_up") =>
          anchor.map(a => s"Build the attack around ${anchorPhrase(a)} without allowing counterplay first.")
        case _ =>
          firstSentence(terminal.summary)
            .orElse(firstSentence(shellPrompt))
            .orElse(familyKey.map(humanizeKey))
      }.getOrElse("Identify the exact strategic task before choosing the first move.")

  private def derivePlanFeedback(
      task: String,
      terminal: TerminalReveal,
      featuredStart: Option[PlanStart]
  ): String =
    firstSentence(terminal.whyPlan.getOrElse(""))
      .orElse(firstSentence(terminal.summary))
      .orElse(featuredStart.flatMap(start => firstSentence(start.feedback)))
      .filterNot(_ == task)
      .getOrElse(task)

  private def anchorPhrase(raw: String): String =
    val trimmed = Option(raw).getOrElse("").trim
    if trimmed.matches("^[a-h]_file$") then s"the ${trimmed.head}-file"
    else humanizeKey(trimmed)

  private def humanizeKey(raw: String): String =
    Option(raw).getOrElse("").replace('|', ' ').replace('_', ' ').replaceAll("\\s+", " ").trim

  private def firstSentence(raw: String): Option[String] =
    nonBlank(raw).map(_.trim).map { text =>
      text.split("(?<=[.!?])\\s+", 2).headOption.getOrElse(text).trim
    }.filter(_.nonEmpty)

  private def firstParagraph(raw: String): Option[String] =
    nonBlank(raw).map(_.split("\\n\\s*\\n", 2).headOption.getOrElse(raw).trim).filter(_.nonEmpty)

  private def nonBlank(raw: String): Option[String] =
    Option(raw).map(_.trim).filter(_.nonEmpty)

  private def slugify(raw: String): String =
    Option(raw)
      .getOrElse("")
      .trim
      .toLowerCase
      .replaceAll("[^a-z0-9]+", "_")
      .replaceAll("_+", "_")
      .stripPrefix("_")
      .stripSuffix("_")

  private def latestByPuzzle(attempts: List[AttemptDoc]): List[AttemptDoc] =
    attempts
      .foldLeft((Set.empty[String], List.empty[AttemptDoc])) { case ((seen, acc), attempt) =>
        if seen(attempt.puzzleId) then (seen, acc)
        else (seen + attempt.puzzleId, attempt :: acc)
      }
      ._2
      .reverse

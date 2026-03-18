package lila.strategicPuzzle

import scala.annotation.tailrec

import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.UUID

import play.api.libs.json.*

import lila.core.userId.UserId

object StrategicPuzzle:

  val PublicSelectionStatus = "pre_polish_top_1000"
  val RuntimeShellSchema = "chesstory.strategicPuzzle.runtimeShell.v1"
  val AttemptSchema = "chesstory.strategicPuzzleAttempt.v1"

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
      dominantFamilyKey: Option[String]
  )
  object TerminalReveal:
    given OFormat[TerminalReveal] = Json.format[TerminalReveal]

  final case class RuntimeShell(
      schema: String,
      startFen: String,
      sideToMove: String,
      prompt: String,
      rootChoices: List[ShellChoice],
      nodes: List[PlayerNode],
      forcedReplies: List[ForcedReply],
      terminals: List[TerminalReveal]
  ):
    lazy val nodeById: Map[String, PlayerNode] = nodes.map(node => node.id -> node).toMap
    lazy val replyByNodeId: Map[String, ForcedReply] = forcedReplies.map(reply => reply.fromNodeId -> reply).toMap
    lazy val terminalById: Map[String, TerminalReveal] = terminals.map(terminal => terminal.id -> terminal).toMap
  object RuntimeShell:
    given OFormat[RuntimeShell] = Json.format[RuntimeShell]

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
      lineUcis: List[String],
      status: String,
      terminalId: Option[String],
      giveUp: Boolean
  )
  object CompleteRequest:
    given OFormat[CompleteRequest] = Json.format[CompleteRequest]

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
      rootUci: Option[String],
      terminalId: Option[String],
      terminalFamilyKey: Option[String],
      completedAt: String
  )
  object AttemptDoc:
    given OFormat[AttemptDoc] = Json.format[AttemptDoc]

  final case class ResolvedCompletion(
      status: String,
      lineUcis: List[String],
      terminalId: Option[String],
      terminalFamilyKey: Option[String]
  ):
    def cleared: Boolean = status == StatusFull

  def normalizeStatus(status: String, giveUp: Boolean): String =
    if giveUp then StatusGiveUp
    else
      status match
        case StatusFull | StatusPartial | StatusWrong | StatusGiveUp => status
        case _                                                       => StatusWrong

  def resolveCompletion(shell: RuntimeShell, req: CompleteRequest): Option[ResolvedCompletion] =
    if req.giveUp then featuredCompletion(shell)
    else replayedCompletion(shell, req.lineUcis)

  def currentIsoInstant(): String =
    DateTimeFormatter.ISO_INSTANT.format(Instant.now())

  def newAttempt(
      userId: UserId,
      puzzleId: String,
      status: String,
      lineUcis: List[String],
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
      rootUci = lineUcis.headOption,
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

  private def replayedCompletion(shell: RuntimeShell, lineUcis: List[String]): Option[ResolvedCompletion] =
    @tailrec
    def loop(currentNodeId: Option[String], remaining: List[String], validated: Vector[String]): Option[ResolvedCompletion] =
      remaining match
        case Nil => None
        case playerUci :: afterPlayer =>
          currentChoices(shell, currentNodeId).find(_.uci == playerUci) match
            case None => None
            case Some(choice) =>
              val withPlayer = validated :+ playerUci
              choice.terminalId match
                case Some(terminalId) =>
                  Option.when(afterPlayer.isEmpty)(terminalCompletion(shell, terminalId, choice.credit, withPlayer))
                case None =>
                  shell.replyByNodeId.get(replyLookup(currentNodeId, playerUci)) match
                    case Some(reply) =>
                      afterPlayer match
                        case forcedUci :: rest if forcedUci == reply.uci =>
                          reply.nextNodeId match
                            case Some(nextNodeId) =>
                              loop(Some(nextNodeId), rest, withPlayer :+ forcedUci)
                            case None =>
                              None
                        case _ => None
                    case None => None
    loop(none[String], lineUcis, Vector.empty)

  private def featuredCompletion(shell: RuntimeShell): Option[ResolvedCompletion] =
    @tailrec
    def loop(currentNodeId: Option[String], choices: List[ShellChoice], lineUcis: Vector[String], hop: Int): Option[ResolvedCompletion] =
      if hop >= 5 then None
      else
        val choiceOpt = choices.find(_.credit == StatusFull).orElse(choices.headOption)
        choiceOpt match
          case None => None
          case Some(choice) =>
            val withPlayer = lineUcis :+ choice.uci
            choice.terminalId match
              case Some(terminalId) =>
                Some(terminalCompletion(shell, terminalId, choice.credit, withPlayer, Some(StatusGiveUp)))
              case None =>
                shell.replyByNodeId.get(replyLookup(currentNodeId, choice.uci)) match
                  case Some(reply) =>
                    reply.nextNodeId match
                      case Some(nextNodeId) =>
                        val nextChoices = shell.nodeById.get(nextNodeId).fold(Nil)(_.choices)
                        loop(Some(nextNodeId), nextChoices, withPlayer :+ reply.uci, hop + 1)
                      case None =>
                        None
                  case None => None
    loop(none[String], shell.rootChoices, Vector.empty, 0)

  private def terminalCompletion(
      shell: RuntimeShell,
      terminalId: String,
      fallbackStatus: String,
      lineUcis: Vector[String],
      statusOverride: Option[String] = None
  ): ResolvedCompletion =
    val terminalOpt = shell.terminalById.get(terminalId)
    ResolvedCompletion(
      status = statusOverride.getOrElse(terminalOpt.fold(normalizeTerminalOutcome(fallbackStatus))(t => normalizeTerminalOutcome(t.outcome))),
      lineUcis = lineUcis.toList,
      terminalId = Some(terminalId),
      terminalFamilyKey = terminalOpt.flatMap(_.familyKey)
    )

  private def normalizeTerminalOutcome(outcome: String): String =
    outcome match
      case StatusFull | StatusPartial | StatusWrong | StatusGiveUp => outcome
      case _                                                       => StatusWrong

  private def currentChoices(shell: RuntimeShell, currentNodeId: Option[String]): List[ShellChoice] =
    currentNodeId.flatMap(shell.nodeById.get).fold(shell.rootChoices)(_.choices)

  private def replyLookup(currentNodeId: Option[String], playerUci: String): String =
    currentNodeId.fold(s"root:$playerUci")(nodeId => s"$nodeId:$playerUci")

  private def latestByPuzzle(attempts: List[AttemptDoc]): List[AttemptDoc] =
    attempts
      .foldLeft((Set.empty[String], List.empty[AttemptDoc])) { case ((seen, acc), attempt) =>
        if seen(attempt.puzzleId) then (seen, acc)
        else (seen + attempt.puzzleId, attempt :: acc)
      }
      ._2
      .reverse

package lila.llm.analysis

import lila.llm.model.{ PlanMatch, PlanSequenceSummary, TransitionType }
import lila.llm.model.strategic.PlanContinuity
import _root_.chess.Color

/**
 * Tracks strategic continuity per color for the live Bookmaker path.
 *
 * v2 token schema:
 * - primary/secondary continuity per color
 * - last transition summary (type + momentum)
 * - last processed ply for same-ply idempotency
 *
 * Reads are backward-compatible with v1 tokens:
 * history.white/black = PlanContinuity | null
 */
case class PlanStateTracker(
  history: Map[Color, PlanStateTracker.ColorPlanState] = PlanStateTracker.defaultHistory
):

  def update(
      movingColor: Color,
      ply: Int,
      primaryPlan: Option[PlanMatch],
      secondaryPlan: Option[PlanMatch],
      sequence: Option[PlanSequenceSummary] = None
  ): PlanStateTracker =
    val current = colorState(movingColor)
    val samePly = current.lastPly.contains(ply)
    val normalizedSecondary =
      secondaryPlan.filter(sec => primaryPlan.forall(pri => pri.plan.id != sec.plan.id))

    val updatedPrimary =
      updateSlot(
        incomingPlan = primaryPlan,
        previousSlot = current.primary,
        previousOtherSlot = current.secondary,
        ply = ply,
        incrementAllowed = !samePly
      )

    val updatedSecondaryRaw =
      updateSlot(
        incomingPlan = normalizedSecondary,
        previousSlot = current.secondary,
        previousOtherSlot = current.primary,
        ply = ply,
        incrementAllowed = !samePly
      )

    val updatedSecondary = updatedSecondaryRaw.filterNot(sec =>
      updatedPrimary.exists(pri => continuityKey(pri) == continuityKey(sec))
    )

    val updatedColorState = current.copy(
      primary = updatedPrimary,
      secondary = updatedSecondary,
      lastTransition = sequence.map(PlanStateTracker.TransitionSnapshot.fromSummary).orElse(current.lastTransition),
      lastPly = Some(ply)
    )

    PlanStateTracker(history + (movingColor -> updatedColorState))

  def getContinuity(color: Color): Option[PlanContinuity] = colorState(color).primary

  def getSecondaryContinuity(color: Color): Option[PlanContinuity] = colorState(color).secondary

  def getColorState(color: Color): PlanStateTracker.ColorPlanState = colorState(color)

  private def colorState(color: Color): PlanStateTracker.ColorPlanState =
    history.getOrElse(color, PlanStateTracker.ColorPlanState())

  private def continuityKey(c: PlanContinuity): String =
    c.planId.map(_.toLowerCase).getOrElse(c.planName.toLowerCase)

  private def updateSlot(
      incomingPlan: Option[PlanMatch],
      previousSlot: Option[PlanContinuity],
      previousOtherSlot: Option[PlanContinuity],
      ply: Int,
      incrementAllowed: Boolean
  ): Option[PlanContinuity] =
    incomingPlan.map { plan =>
      val matched =
        previousSlot.filter(samePlan(_, plan))
          .orElse(previousOtherSlot.filter(samePlan(_, plan)))

      matched match
        case Some(prev) =>
          prev.copy(
            planName = plan.plan.name,
            planId = Some(plan.plan.id.toString),
            consecutivePlies = if incrementAllowed then prev.consecutivePlies + 1 else prev.consecutivePlies
          )
        case None =>
          PlanContinuity(
            planName = plan.plan.name,
            planId = Some(plan.plan.id.toString),
            consecutivePlies = 1,
            startingPly = ply
          )
    }

  private def samePlan(c: PlanContinuity, plan: PlanMatch): Boolean =
    c.planId.exists(_.equalsIgnoreCase(plan.plan.id.toString)) ||
      c.planName.equalsIgnoreCase(plan.plan.name)

object PlanStateTracker:
  case class TransitionSnapshot(
      transitionType: TransitionType,
      momentum: Double,
      primaryPlanId: Option[String] = None,
      secondaryPlanId: Option[String] = None
  )

  object TransitionSnapshot:
    def fromSummary(s: PlanSequenceSummary): TransitionSnapshot =
      TransitionSnapshot(
        transitionType = s.transitionType,
        momentum = s.momentum,
        primaryPlanId = s.primaryPlanId,
        secondaryPlanId = s.secondaryPlanId
      )

  case class ColorPlanState(
      primary: Option[PlanContinuity] = None,
      secondary: Option[PlanContinuity] = None,
      lastTransition: Option[TransitionSnapshot] = None,
      lastPly: Option[Int] = None
  )

  private[analysis] val defaultHistory: Map[Color, ColorPlanState] =
    Map(Color.White -> ColorPlanState(), Color.Black -> ColorPlanState())
  val empty: PlanStateTracker = PlanStateTracker(defaultHistory)

  import play.api.libs.json.*

  private given transitionTypeReads: Reads[TransitionType] = Reads {
    case JsString(value) =>
      scala.util.Try(TransitionType.valueOf(value)).toOption match
        case Some(t) => JsSuccess(t)
        case None    => JsError(s"invalid transitionType: $value")
    case _ => JsError("transitionType must be a string")
  }
  private given transitionTypeWrites: Writes[TransitionType] = Writes(t => JsString(t.toString))

  given Format[TransitionSnapshot] = Json.format[TransitionSnapshot]
  given Format[ColorPlanState] = Json.format[ColorPlanState]

  private def readColorState(node: JsValue): JsResult[ColorPlanState] =
    node match
      case JsNull => JsSuccess(ColorPlanState())
      case obj: JsObject =>
        val isV2Node =
          (obj \ "primary").toOption.isDefined ||
            (obj \ "secondary").toOption.isDefined ||
            (obj \ "lastTransition").toOption.isDefined ||
            (obj \ "lastPly").toOption.isDefined

        if isV2Node then obj.validate[ColorPlanState]
        else if obj.value.isEmpty then JsSuccess(ColorPlanState())
        else obj.validate[PlanContinuity].map(cont => ColorPlanState(primary = Some(cont)))
      case _ => JsSuccess(ColorPlanState())

  private def readColor(history: JsObject, key: String): JsResult[ColorPlanState] =
    history.value.get(key) match
      case Some(value) => readColorState(value)
      case None        => JsSuccess(ColorPlanState())

  given Format[PlanStateTracker] = Format(
    Reads { js =>
      val historyObj = (js \ "history").asOpt[JsObject].getOrElse(Json.obj())
      for
        white <- readColor(historyObj, "white")
        black <- readColor(historyObj, "black")
      yield PlanStateTracker(defaultHistory ++ Map(Color.White -> white, Color.Black -> black))
    },
    Writes { tracker =>
      Json.obj(
        "version" -> 2,
        "history" -> Json.obj(
          "white" -> Json.toJson(tracker.getColorState(Color.White)),
          "black" -> Json.toJson(tracker.getColorState(Color.Black))
        )
      )
    }
  )

package lila.llm.model.structure

import lila.llm.model.PlanId
import play.api.libs.json.*

enum StructureId:
  case Carlsbad
  case IQPWhite
  case IQPBlack
  case HangingPawnsWhite
  case HangingPawnsBlack
  case FrenchAdvanceChain
  case NajdorfScheveningenCenter
  case BenoniCenter
  case KIDLockedCenter
  case SlavCaroTriangle
  case MaroczyBind
  case Hedgehog
  case FianchettoShell
  case Stonewall
  case OpenCenter
  case LockedCenter
  case FluidCenter
  case SymmetricCenter
  case Unknown

object StructureId:
  val taxonomyV1: List[StructureId] = List(
    StructureId.Carlsbad,
    StructureId.IQPWhite,
    StructureId.IQPBlack,
    StructureId.HangingPawnsWhite,
    StructureId.HangingPawnsBlack,
    StructureId.FrenchAdvanceChain,
    StructureId.NajdorfScheveningenCenter,
    StructureId.BenoniCenter,
    StructureId.KIDLockedCenter,
    StructureId.SlavCaroTriangle,
    StructureId.MaroczyBind,
    StructureId.Hedgehog,
    StructureId.FianchettoShell,
    StructureId.Stonewall,
    StructureId.OpenCenter,
    StructureId.LockedCenter,
    StructureId.FluidCenter,
    StructureId.SymmetricCenter
  )

  given Writes[StructureId] = Writes(id => JsString(id.toString))
  given Reads[StructureId] = Reads {
    case JsString(raw) =>
      StructureId.values.find(_.toString == raw).map(JsSuccess(_)).getOrElse(JsError(s"Unknown StructureId: $raw"))
    case _ => JsError("Expected StructureId string")
  }

enum CenterState:
  case Open
  case Locked
  case Fluid
  case Symmetric

object CenterState:
  given Writes[CenterState] = Writes(v => JsString(v.toString))
  given Reads[CenterState] = Reads {
    case JsString(raw) =>
      CenterState.values.find(_.toString == raw).map(JsSuccess(_)).getOrElse(JsError(s"Unknown CenterState: $raw"))
    case _ => JsError("Expected CenterState string")
  }

enum AlignmentBand:
  case OnBook
  case Playable
  case OffPlan
  case Unknown

object AlignmentBand:
  given Writes[AlignmentBand] = Writes(v => JsString(v.toString))
  given Reads[AlignmentBand] = Reads {
    case JsString(raw) =>
      AlignmentBand.values.find(_.toString == raw).map(JsSuccess(_)).getOrElse(JsError(s"Unknown AlignmentBand: $raw"))
    case _ => JsError("Expected AlignmentBand string")
  }

case class StructureProfile(
    primary: StructureId,
    confidence: Double,
    alternatives: List[StructureId],
    centerState: CenterState,
    evidenceCodes: List[String]
)

object StructureProfile:
  given OFormat[StructureProfile] = Json.format[StructureProfile]

case class StructuralPlaybookEntry(
    structureId: StructureId,
    whitePlans: List[PlanId],
    blackPlans: List[PlanId],
    counterPlans: List[PlanId],
    preconditions: List[String] = Nil,
    narrativeIntent: String = "play around central tension and piece coordination",
    narrativeRisk: String = "balanced"
)

case class PlanAlignment(
    score: Int,
    band: AlignmentBand,
    matchedPlanIds: List[String],
    missingPlanIds: List[String],
    reasonCodes: List[String],
    narrativeIntent: Option[String] = None,
    narrativeRisk: Option[String] = None,
    reasonWeights: Map[String, Double] = Map.empty
)

object PlanAlignment:
  given OFormat[PlanAlignment] = Json.format[PlanAlignment]

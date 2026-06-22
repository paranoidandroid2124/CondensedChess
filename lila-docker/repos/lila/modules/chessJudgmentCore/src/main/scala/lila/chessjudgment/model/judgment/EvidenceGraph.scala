package lila.chessjudgment.model.judgment

import chess.*
import lila.chessjudgment.analysis.evaluation.PerspectiveMath
import lila.chessjudgment.analysis.position.PositionFeatures
import lila.chessjudgment.analysis.singlePosition.{
  PawnPlayAnalysis,
  SinglePositionAssessment,
  Threat,
  ThreatAnalysis,
  ThreatDriver,
  ThreatEvidenceSource,
  ThreatKind,
  ThreatSeverity
}
import lila.chessjudgment.model.{ ActivePlans, Fact, Motif, MotifCategory, PlanScoringResult, PlanSequenceSummary }
import lila.chessjudgment.model.structure.{ PlanAlignment, StructureProfile }

final case class EvidenceSquare(key: String)
final case class EvidenceFile(key: String)
final case class EvidencePieceRole(name: String)

enum EvidenceSemanticAnchorKind:
  case StrategicKind
  case Plan
  case BoardAnchor
  case PawnStructure
  case StructurePlan
  case PawnPlay
  case OpeningAnchor
  case OpeningSupported
  case OpeningObserved
  case CandidateComparison
  case PlanPressure
  case PlanTransition
  case LineEvent
  case LineConsequence
  case StructuralDelta

final case class EvidenceSemanticAnchor(
    kind: EvidenceSemanticAnchorKind,
    values: List[String]
):
  def stableKey: String =
    (kind.toString :: values).mkString(":")

object EvidenceSemanticAnchor:
  def of(kind: EvidenceSemanticAnchorKind, values: String*): EvidenceSemanticAnchor =
    EvidenceSemanticAnchor(kind, values.toList)

enum RelationParticipantRole:
  case Attacker
  case Defender
  case Target
  case Blocker
  case Beneficiary
  case King
  case Mover
  case Bait
  case Lured
  case Other

final case class RelationParticipant(
    square: EvidenceSquare,
    role: Option[EvidencePieceRole],
    participantRole: RelationParticipantRole
)

enum RelationProofAtomRole:
  case Participant
  case LineMove
  case Focus
  case Target

final case class RelationProofAtom(
    role: RelationProofAtomRole,
    square: Option[EvidenceSquare] = None,
    moveUci: Option[String] = None,
    participantRole: Option[RelationParticipantRole] = None,
    pieceRole: Option[EvidencePieceRole] = None,
    label: Option[String] = None
)

enum RelationThreatSignal:
  case MateCheck
  case Check

enum RelationAxisSignal:
  case File
  case Rank
  case Diagonal

final case class RelationWitnessTarget(
    square: EvidenceSquare,
    role: EvidencePieceRole
)

enum RelationWitnessDetail:
  case Empty
  case DefenderTrade(defenderSquare: EvidenceSquare, exchangeSquare: EvidenceSquare, targetSquare: EvidenceSquare)
  case BadPieceLiquidation(badPieceSquare: EvidenceSquare, exchangeSquare: EvidenceSquare)
  case Overload(defenderSquare: EvidenceSquare, targetSquares: List[EvidenceSquare], attackerSquare: EvidenceSquare)
  case Deflection(defenderSquare: EvidenceSquare, targetSquare: EvidenceSquare, attackerSquare: EvidenceSquare)
  case DiscoveredAttack(
      attackerSquare: EvidenceSquare,
      clearedSquare: EvidenceSquare,
      targetSquare: EvidenceSquare,
      attackerRole: EvidencePieceRole
  )
  case DoubleCheck(kingSquare: EvidenceSquare, checkerSquares: List[EvidenceSquare], moverSquare: EvidenceSquare, moverRole: EvidencePieceRole)
  case MatePattern(
      relationKind: String,
      kingSquare: EvidenceSquare,
      checkerSquares: List[EvidenceSquare],
      matingMove: String,
      patternId: Option[String]
  )
  case GreekGift(bishopSquare: EvidenceSquare, targetSquare: EvidenceSquare, entryMove: String, patternId: String)
  case Fork(attackerSquare: EvidenceSquare, attackerRole: EvidencePieceRole, targets: List[RelationWitnessTarget])
  case HangingPiece(
      attackerSquare: EvidenceSquare,
      targetSquare: EvidenceSquare,
      attackerRole: EvidencePieceRole,
      targetRole: EvidencePieceRole
  )
  case TrappedPiece(
      attackerSquare: EvidenceSquare,
      targetSquare: EvidenceSquare,
      attackerRole: EvidencePieceRole,
      targetRole: EvidencePieceRole
  )
  case Domination(
      attackerSquare: EvidenceSquare,
      targetSquare: EvidenceSquare,
      attackerRole: EvidencePieceRole,
      targetRole: EvidencePieceRole,
      controlledEscapeSquares: List[EvidenceSquare]
  )
  case Zwischenzug(
      intermediateMove: String,
      expectedRecaptureSquare: EvidenceSquare,
      checkingPieceSquare: EvidenceSquare,
      checkingPieceRole: EvidencePieceRole,
      checkedKingSquare: EvidenceSquare,
      threatType: RelationThreatSignal
  )
  case Decoy(
      baitFromSquare: EvidenceSquare,
      baitSquare: EvidenceSquare,
      luredFromSquare: EvidenceSquare,
      executionFromSquare: EvidenceSquare,
      executionToSquare: EvidenceSquare,
      baitRole: EvidencePieceRole,
      luredRole: EvidencePieceRole
  )
  case XRay(
      attackerSquare: EvidenceSquare,
      blockerSquare: EvidenceSquare,
      targetSquare: EvidenceSquare,
      attackerRole: EvidencePieceRole,
      blockerRole: EvidencePieceRole,
      targetRole: EvidencePieceRole
  )
  case Clearance(
      beneficiarySquare: EvidenceSquare,
      clearedSquare: EvidenceSquare,
      targetSquare: EvidenceSquare,
      beneficiaryRole: EvidencePieceRole,
      clearingTo: EvidenceSquare
  )
  case Battery(
      frontSquare: EvidenceSquare,
      backSquare: EvidenceSquare,
      targetSquare: EvidenceSquare,
      frontRole: EvidencePieceRole,
      backRole: EvidencePieceRole,
      axis: RelationAxisSignal
  )
  case Interference(
      blockerSquare: EvidenceSquare,
      defenderSquare: EvidenceSquare,
      targetSquare: EvidenceSquare,
      blockerRole: EvidencePieceRole,
      defenderRole: EvidencePieceRole,
      targetRole: EvidencePieceRole
  )
  case Pin(
      attackerSquare: EvidenceSquare,
      pinnedSquare: EvidenceSquare,
      behindSquare: EvidenceSquare,
      targetSquare: EvidenceSquare,
      attackerRole: EvidencePieceRole,
      pinnedRole: EvidencePieceRole,
      behindRole: EvidencePieceRole,
      absolute: Boolean
  )
  case Skewer(
      attackerSquare: EvidenceSquare,
      frontSquare: EvidenceSquare,
      backSquare: EvidenceSquare,
      targetSquare: EvidenceSquare,
      attackerRole: EvidencePieceRole,
      frontRole: EvidencePieceRole,
      backRole: EvidencePieceRole
  )
  case StalemateTrap(stalematedKingSquare: EvidenceSquare, resourceSquare: EvidenceSquare, entryMove: String, terminalMove: String, scoreCp: Option[Int])
  case PerpetualCheck(
      checkedKingSquare: EvidenceSquare,
      checkerSquares: List[EvidenceSquare],
      checkingSide: String,
      entryMove: String,
      cycleStartMove: String,
      cycleReturnMove: String,
      repeatedPositionKey: String,
      scoreCp: Option[Int]
  )

  def detailName: String =
    toString.takeWhile(_ != '(')

final case class RelationWitnessProof(
    sourceKind: String,
    detail: RelationWitnessDetail,
    focusSquares: List[EvidenceSquare],
    targetSquare: Option[EvidenceSquare],
    lineMoves: List[String],
    participants: List[RelationParticipant],
    proofAtoms: List[RelationProofAtom]
):
  def hasTypedDetail: Boolean =
    detail != RelationWitnessDetail.Empty
  def hasLineProof: Boolean =
    proofAtoms.exists(_.role == RelationProofAtomRole.LineMove)
  def detailName: String =
    detail.detailName

object RelationWitnessProof:
  val empty: RelationWitnessProof =
    RelationWitnessProof(
      sourceKind = "unknown",
      detail = RelationWitnessDetail.Empty,
      focusSquares = Nil,
      targetSquare = None,
      lineMoves = Nil,
      participants = Nil,
      proofAtoms = Nil
    )

enum RelationFactKind:
  case DefenderTrade
  case BadPieceLiquidation
  case Overload
  case Deflection
  case DiscoveredAttack
  case DoubleCheck
  case BackRankMate
  case MateNet
  case Fork
  case HangingPiece
  case Decoy
  case Interference
  case Clearance
  case XRay
  case Battery
  case Pin
  case Skewer
  case Zwischenzug
  case Domination
  case TrappedPiece
  case GreekGift
  case StalemateTrap
  case PerpetualCheck

object RelationFactKind:
  private val byId: Map[String, RelationFactKind] =
    Map(
      "defender_trade" -> DefenderTrade,
      "bad_piece_liquidation" -> BadPieceLiquidation,
      "overload" -> Overload,
      "deflection" -> Deflection,
      "discovered_attack" -> DiscoveredAttack,
      "double_check" -> DoubleCheck,
      "back_rank_mate" -> BackRankMate,
      "mate_net" -> MateNet,
      "fork" -> Fork,
      "hanging_piece" -> HangingPiece,
      "decoy" -> Decoy,
      "interference" -> Interference,
      "clearance" -> Clearance,
      "xray" -> XRay,
      "battery" -> Battery,
      "pin" -> Pin,
      "skewer" -> Skewer,
      "zwischenzug" -> Zwischenzug,
      "domination" -> Domination,
      "trapped_piece" -> TrappedPiece,
      "greek_gift" -> GreekGift,
      "stalemate_trap" -> StalemateTrap,
      "perpetual_check" -> PerpetualCheck
    )

  def fromId(raw: String): Option[RelationFactKind] =
    byId.get(Option(raw).getOrElse("").trim.toLowerCase)

enum StrategicFactKind:
  case Outpost
  case FileControl
  case Space
  case CounterplayRestraint
  case TargetFixation
  case Structure
  case Endgame
  case Activity
  case Compensation
  case Practicality
  case PlanPressure

sealed trait EvidencePayload:
  def layer: EvidenceLayer

final case class BoardFactEvidence(
    private val facts: List[Fact],
    private val features: Option[PositionFeatures]
)(private val anchors: List[BoardAnchor] = Nil,
    private val attackDefense: List[BoardAttackDefenseEntry] = Nil,
    private val profile: Option[BoardPositionProfile] = None
) extends EvidencePayload:
  val layer: EvidenceLayer = EvidenceLayer.Board
  def boardAnchors: List[BoardAnchor] =
    anchors
  def boardAnchorCount: Int =
    anchors.size
  def hasBoardAnchors: Boolean =
    anchors.nonEmpty
  def anchorsOf(kind: BoardAnchorKind): List[BoardAnchor] =
    anchors.filter(_.kind == kind)
  def anchorsOfAny(kinds: Set[BoardAnchorKind]): List[BoardAnchor] =
    anchors.filter(anchor => kinds.contains(anchor.kind))
  def anchorsOfAtLeast(kind: BoardAnchorKind, minimumMagnitude: Int): List[BoardAnchor] =
    anchors.filter(anchor => anchor.kind == kind && anchor.magnitude >= minimumMagnitude)
  def semanticGroupingAnchors: List[EvidenceSemanticAnchor] =
    anchors.map(_.semanticGroupingAnchor)
  def proofSignalAnchors: List[BoardAnchor] =
    anchors.filter(BoardFactEvidence.isProofSignalAnchor)
  def proofSignalAnchorKinds: List[BoardAnchorKind] =
    proofSignalAnchors.map(_.kind).distinct
  def hasProofSignalAnchor(kind: BoardAnchorKind): Boolean =
    proofSignalAnchors.exists(_.kind == kind)
  def looseMaterialAnchors: List[BoardAnchor] =
    anchorsOf(BoardAnchorKind.LooseMaterial)
  def outpostAnchors: List[BoardAnchor] =
    anchorsOf(BoardAnchorKind.Outpost)
  def fileControlAnchors: List[BoardAnchor] =
    anchorsOf(BoardAnchorKind.FileControl)
  def spaceAnchors: List[BoardAnchor] =
    anchorsOf(BoardAnchorKind.Space)
  def activityAnchors: List[BoardAnchor] =
    anchorsOfAny(Set(BoardAnchorKind.Activity, BoardAnchorKind.CounterplayRestraint))
  def counterplayRestraintAnchors: List[BoardAnchor] =
    anchorsOfAtLeast(BoardAnchorKind.CounterplayRestraint, minimumMagnitude = 3)
  def endgameTechniqueAnchors: List[BoardAnchor] =
    anchorsOf(BoardAnchorKind.EndgameTechnique)
  def openingContextAnchors: List[BoardAnchor] =
    anchorsOfAny(
      Set(
        BoardAnchorKind.CenterControl,
        BoardAnchorKind.Space,
        BoardAnchorKind.Development,
        BoardAnchorKind.Activity,
        BoardAnchorKind.PawnStructure,
        BoardAnchorKind.KingSafety
      )
    )
  def anchorFocusSquares: List[EvidenceSquare] =
    anchors.flatMap(_.focusSquares).distinct
  def boardProfile: Option[BoardPositionProfile] =
    profile
  def factCount: Int =
    facts.size
  def hasBoardProfile: Boolean =
    profile.nonEmpty
  def hasAttackDefenseEntries: Boolean =
    attackDefense.nonEmpty
  def attackDefenseCount: Int =
    attackDefense.size
  def vulnerableAttackDefense: List[BoardAttackDefenseEntry] =
    attackDefense.filter(entry => entry.isLoose || entry.isUnderdefended)
  def targetHintSquares: List[EvidenceSquare] =
    val anchorSquares =
      anchors.flatMap(_.targetHintSquares)
    val materialSquares =
      vulnerableAttackDefense.map(_.square)
    (anchorSquares ++ materialSquares).distinct

object BoardFactEvidence:
  def apply(facts: List[Fact], features: Option[PositionFeatures]): BoardFactEvidence =
    new BoardFactEvidence(facts, features)()

  private[chessjudgment] def isProofSignalAnchor(anchor: BoardAnchor): Boolean =
    anchor.kind match
      case BoardAnchorKind.LooseMaterial =>
        anchor.signal == BoardAnchorSignal.HangingPiece ||
          anchor.detail.exists(_.defenderSquares.isEmpty)
      case BoardAnchorKind.PinPressure =>
        anchor.detail.flatMap(_.isAbsolute).contains(true) || anchor.magnitude > 1
      case BoardAnchorKind.SkewerPressure | BoardAnchorKind.ForkPressure | BoardAnchorKind.XRayPressure |
          BoardAnchorKind.Outpost =>
        true
      case BoardAnchorKind.BatteryPressure =>
        anchor.detail.flatMap(_.axis).contains(BoardAnchorAxis.Diagonal)
      case BoardAnchorKind.CenterControl | BoardAnchorKind.Space | BoardAnchorKind.Development |
          BoardAnchorKind.FileControl | BoardAnchorKind.Activity | BoardAnchorKind.CounterplayRestraint |
          BoardAnchorKind.KingSafety | BoardAnchorKind.PawnStructure | BoardAnchorKind.WeakSquare |
          BoardAnchorKind.EndgameTechnique =>
        false

final case class BoardAttackDefenseEntry(
    square: EvidenceSquare,
    occupantColor: Color,
    occupantRole: EvidencePieceRole,
    attackerColor: Color,
    attackerSquares: List[EvidenceSquare],
    defenderSquares: List[EvidenceSquare],
    attackCount: Int,
    defenseCount: Int,
    pressureDelta: Int,
    materialValueCp: Int,
    isLoose: Boolean,
    isUnderdefended: Boolean
)

final case class BoardPositionProfile(
    centerLocked: Boolean,
    centerOpen: Boolean,
    pawnTensionCount: Int,
    whiteCenterControl: Int,
    blackCenterControl: Int,
    whiteCentralPawns: Int,
    blackCentralPawns: Int,
    spaceDiff: Int,
    whiteDevelopmentLag: Int,
    blackDevelopmentLag: Int,
    whiteLowMobilityPieces: Int,
    blackLowMobilityPieces: Int,
    whiteKingExposure: Int,
    blackKingExposure: Int,
    whitePawnWeaknesses: Int,
    blackPawnWeaknesses: Int,
    whitePassedPawns: Int,
    blackPassedPawns: Int,
    whiteEntrenchedPieces: Int,
    blackEntrenchedPieces: Int,
    whiteRookPawnMarchReady: Boolean,
    blackRookPawnMarchReady: Boolean,
    whiteHookCreationChance: Boolean,
    blackHookCreationChance: Boolean,
    whiteColorComplexClamp: Boolean,
    blackColorComplexClamp: Boolean,
    hasStrategicSnapshot: Boolean
):
  def centerControlEdgeFor(side: Color): Int =
    if side.white then whiteCenterControl - blackCenterControl
    else blackCenterControl - whiteCenterControl
  def centralPawnsFor(side: Color): Int =
    if side.white then whiteCentralPawns else blackCentralPawns
  def spaceFor(side: Color): Int =
    if side.white then spaceDiff else -spaceDiff
  def developmentLagFor(side: Color): Int =
    if side.white then whiteDevelopmentLag else blackDevelopmentLag
  def lowMobilityFor(side: Color): Int =
    if side.white then whiteLowMobilityPieces else blackLowMobilityPieces
  def kingExposureFor(side: Color): Int =
    if side.white then whiteKingExposure else blackKingExposure
  def opponentPawnWeaknessFor(side: Color): Int =
    if side.white then blackPawnWeaknesses else whitePawnWeaknesses
  def passedPawnsFor(side: Color): Int =
    if side.white then whitePassedPawns else blackPassedPawns
  def opponentPassedPawnsFor(side: Color): Int =
    if side.white then blackPassedPawns else whitePassedPawns
  def entrenchedPiecesFor(side: Color): Int =
    if side.white then whiteEntrenchedPieces else blackEntrenchedPieces
  def rookPawnReadyFor(side: Color): Boolean =
    if side.white then whiteRookPawnMarchReady else blackRookPawnMarchReady
  def hookChanceFor(side: Color): Boolean =
    if side.white then whiteHookCreationChance else blackHookCreationChance
  def colorComplexClampFor(side: Color): Boolean =
    if side.white then whiteColorComplexClamp else blackColorComplexClamp

enum BoardAnchorKind:
  case CenterControl
  case Space
  case Development
  case FileControl
  case Activity
  case CounterplayRestraint
  case KingSafety
  case PawnStructure
  case LooseMaterial
  case PinPressure
  case SkewerPressure
  case ForkPressure
  case XRayPressure
  case BatteryPressure
  case WeakSquare
  case Outpost
  case EndgameTechnique

enum BoardAnchorSignal:
  case CenterControlEdge
  case SpaceEdge
  case DevelopmentLead
  case OpenFileAccess
  case SemiOpenFileAccess
  case RookOnSeventh
  case MobilityEdge
  case OpponentLowMobility
  case KingExposure
  case KingPressure
  case PawnStructureShape
  case HangingPiece
  case AttackedTarget
  case AbsolutePin
  case RelativePin
  case SkewerLine
  case ForkTargets
  case XRayLine
  case BatteryLine
  case WeakSquareHole
  case OutpostSquare
  case EndgameKingActivity
  case EndgameOpposition
  case EndgameRuleOfSquare
  case EndgameTriangulation
  case EndgameRookPattern
  case EndgameOutcomeHint
  case EndgameZugzwang
  case EndgamePromotion
  case EndgameStalemateResource

enum BoardAnchorAxis:
  case File
  case Rank
  case Diagonal

final case class BoardAnchorDetail(
    subjectColor: Option[Color] = None,
    subjectSquare: Option[EvidenceSquare] = None,
    subjectRole: Option[EvidencePieceRole] = None,
    targetSquare: Option[EvidenceSquare] = None,
    targetRole: Option[EvidencePieceRole] = None,
    attackerColor: Option[Color] = None,
    attackerSquare: Option[EvidenceSquare] = None,
    attackerRole: Option[EvidencePieceRole] = None,
    attackerSquares: List[EvidenceSquare] = Nil,
    defenderSquares: List[EvidenceSquare] = Nil,
    relatedSquares: List[EvidenceSquare] = Nil,
    file: Option[EvidenceFile] = None,
    axis: Option[BoardAnchorAxis] = None,
    isAbsolute: Option[Boolean] = None,
    materialLossCp: Option[Int] = None
):
  def focusSquares: List[EvidenceSquare] =
    (
      subjectSquare.toList ++
        targetSquare.toList ++
        attackerSquare.toList ++
        attackerSquares ++
        defenderSquares ++
        relatedSquares
    ).distinct
  def targetHintSquares: List[EvidenceSquare] =
    (
      targetSquare.toList ++
        subjectSquare.toList ++
        relatedSquares
    ).distinct

final case class BoardAnchor(
    kind: BoardAnchorKind,
    side: Color,
    signal: BoardAnchorSignal,
    magnitude: Int,
    confidence: Double,
    detail: Option[BoardAnchorDetail] = None
):
  def focusSquares: List[EvidenceSquare] =
    detail.toList.flatMap(_.focusSquares).distinct
  def targetHintSquares: List[EvidenceSquare] =
    detail.toList.flatMap(_.targetHintSquares).distinct
  def semanticGroupingAnchor: EvidenceSemanticAnchor =
    val sideKey = if side.white then "white" else "black"
    val detailValues =
      detail.toList.flatMap(detail =>
        List(
          detail.subjectColor.map(color => s"subject-color:${colorKey(color)}"),
          detail.attackerColor.map(color => s"attacker-color:${colorKey(color)}"),
          detail.subjectSquare.map(square => s"subject-square:${square.key}"),
          detail.targetSquare.map(square => s"target-square:${square.key}"),
          Option
            .when(detail.relatedSquares.nonEmpty)(s"related:${detail.relatedSquares.map(_.key).sorted.mkString(",")}"),
          detail.file.map(file => s"file:${file.key}"),
          detail.axis.map(axis => s"axis:$axis")
        ).flatten
      )
    EvidenceSemanticAnchor.of(
      EvidenceSemanticAnchorKind.BoardAnchor,
      (List(sideKey, kind.toString, signal.toString) ++ detailValues)*
    )

  private def colorKey(color: Color): String =
    if color.white then "white" else "black"

final case class SinglePositionEvidence(
    assessment: SinglePositionAssessment
) extends EvidencePayload:
  val layer: EvidenceLayer = EvidenceLayer.SinglePosition

final case class PawnStructureFactEvidence(
    profile: StructureProfile,
    alignment: Option[PlanAlignment],
    pawnPlay: Option[PawnPlayAnalysis]
) extends EvidencePayload:
  val layer: EvidenceLayer = EvidenceLayer.PawnStructure

final case class StrategicFactEvidence(
    kind: StrategicFactKind,
    facts: List[Fact],
    relatedPlans: List[lila.chessjudgment.model.PlanId],
    confidence: Double
)(val boardAnchors: List[BoardAnchor] = Nil
) extends EvidencePayload:
  val layer: EvidenceLayer = EvidenceLayer.Strategic
  def hasTypedSupport: Boolean =
    facts.nonEmpty || relatedPlans.nonEmpty || boardAnchors.nonEmpty
  def semanticGroupingAnchors: List[EvidenceSemanticAnchor] =
    boardAnchors.map(_.semanticGroupingAnchor)

enum OpeningFamily:
  case A
  case B
  case C
  case D
  case E

object OpeningFamily:
  def fromEco(raw: String): Option[OpeningFamily] =
    Option(raw)
      .map(_.trim.toUpperCase)
      .flatMap(_.headOption)
      .flatMap(ch => fromRaw(ch.toString))

  def fromOpeningName(raw: String): Option[OpeningFamily] =
    val name = Option(raw).map(_.trim.toLowerCase).getOrElse("")
    Option.when(name.nonEmpty)(name).flatMap { value =>
      if value.contains("sicilian") ||
        value.contains("caro-kann") ||
        value.contains("scandinavian") ||
        value.contains("alekhine") ||
        value.contains("pirc") ||
        value.contains("modern defense") ||
        value.contains("nimzowitsch") ||
        value.contains("owen") ||
        value.contains("english defense")
      then Some(OpeningFamily.B)
      else if value.contains("french") ||
        value.contains("king's pawn") ||
        value.contains("italian") ||
        value.contains("ruy lopez") ||
        value.contains("spanish") ||
        value.contains("four knights") ||
        value.contains("vienna") ||
        value.contains("scotch") ||
        value.contains("petrov") ||
        value.contains("philidor") ||
        value.contains("king's gambit")
      then Some(OpeningFamily.C)
      else if value.contains("queen's gambit") ||
        value.contains("slav") ||
        value.contains("semi-slav") ||
        value.contains("tarrasch") ||
        value.contains("queen's pawn")
      then Some(OpeningFamily.D)
      else if value.contains("indian") ||
        value.contains("nimzo-indian") ||
        value.contains("gruenfeld") ||
        value.contains("grünfeld") ||
        value.contains("catalan") ||
        value.contains("bogo-indian") ||
        value.contains("queen's indian")
      then Some(OpeningFamily.E)
      else if value.contains("english opening") ||
        value.contains("reti") ||
        value.contains("réti") ||
        value.contains("dutch") ||
        value.contains("benoni") ||
        value.contains("benko") ||
        value.contains("bird") ||
        value.contains("polish")
      then Some(OpeningFamily.A)
      else None
    }

  def fromRaw(raw: String): Option[OpeningFamily] =
    Option(raw).map(_.trim.toUpperCase).collect:
      case "A" => OpeningFamily.A
      case "B" => OpeningFamily.B
      case "C" => OpeningFamily.C
      case "D" => OpeningFamily.D
      case "E" => OpeningFamily.E

enum OpeningContextSignal:
  case InputIdentity
  case RecognizedIdentity
  case OpeningPhase
  case ThemePrior

enum OpeningTheme:
  case CenterControl
  case Development
  case PawnStructure
  case GambitInitiative
  case KingSafety
  case PlanPressure

enum FeatureAnchorSignal:
  case CenterControlObserved
  case DevelopmentTempoObserved
  case PawnStructureObserved
  case CompensationObserved
  case KingSafetyObserved
  case PlanPressureObserved
  case StructuralDeltaObserved

final case class FeatureAnchor(
    theme: OpeningTheme,
    signal: FeatureAnchorSignal,
    sourceLayer: EvidenceLayer,
    strength: Double
):
  def isBoardObservation: Boolean =
    sourceLayer == EvidenceLayer.Board
  def canCorroborateOpeningPrior: Boolean =
    !isBoardObservation
  def hasPositiveStrength: Boolean =
    strength > 0.0

enum FeatureApplicability:
  case OpeningRelevant
  case MiddlegameRelevant
  case EndgameRelevant
  case ObservedOnly
  case Contraindicated

enum ApplicabilityStatus:
  case InternalOnly
  case Supported
  case PartiallySupported
  case Unverified
  case Ambiguous
  case Contradicted

final case class ApplicabilityAssessment(
    applicability: FeatureApplicability,
    status: ApplicabilityStatus,
    observedThemes: List[OpeningTheme],
    supportedThemes: List[OpeningTheme],
    unverifiedPriorThemes: List[OpeningTheme],
    observedOnlyThemes: List[OpeningTheme]
):
  def hasInternalAnchorAlignment: Boolean =
    applicability == FeatureApplicability.OpeningRelevant &&
      observedThemes.nonEmpty &&
      supportedThemes.nonEmpty &&
      (status == ApplicabilityStatus.Supported || status == ApplicabilityStatus.PartiallySupported)

  def canCertifyOpeningClaim: Boolean =
    hasInternalAnchorAlignment

final case class OpeningIdentity(
    eco: Option[String],
    name: Option[String],
    family: Option[OpeningFamily]
)

final case class OpeningCandidate(
    identity: OpeningIdentity,
    lineage: Option[String],
    frequency: Int,
    sampleCount: Int,
    confidence: Double
)

enum OpeningRecognitionMatchKind:
  case ExactPrefixAndPosition
  case PositionTransposition

final case class OpeningRecognition(
    movePrefixHash: String,
    positionKey: String,
    matchedBy: OpeningRecognitionMatchKind,
    candidates: List[OpeningCandidate],
    matchedPly: Int,
    frequency: Int,
    sampleCount: Int,
    confidence: Double
):
  def bestCandidate: Option[OpeningCandidate] =
    candidates.headOption

  def bestIdentity: Option[OpeningIdentity] =
    bestCandidate.map(_.identity)

  def lineage: Option[String] =
    bestCandidate.flatMap(_.lineage)

final case class OpeningThemePrior(
    lineage: Option[String],
    family: Option[OpeningFamily],
    themes: List[OpeningTheme],
    typicalPawnStructures: List[String],
    centerBreaks: List[String],
    developmentPriorities: List[String],
    gambitCompensation: Boolean,
    strategicPlanPriors: List[String]
)

final case class OpeningContextEvidence(
    identity: Option[OpeningIdentity],
    signals: List[OpeningContextSignal],
    recognition: Option[OpeningRecognition] = None,
    themePrior: Option[OpeningThemePrior] = None
) extends EvidencePayload:
  val layer: EvidenceLayer = EvidenceLayer.OpeningContext

final case class FeatureAnchorEvidence(
    anchor: FeatureAnchor
) extends EvidencePayload:
  val layer: EvidenceLayer = EvidenceLayer.FeatureAnchor

final case class ApplicabilityAssessmentEvidence(
    assessment: ApplicabilityAssessment
) extends EvidencePayload:
  val layer: EvidenceLayer = EvidenceLayer.ApplicabilityAssessment

final case class ThreatEpisode(
    episodeId: String,
    sourceThreatIndex: Int,
    sideUnderPressure: Color,
    kind: ThreatKind,
    severity: ThreatSeverity,
    driver: ThreatDriver,
    evidenceSource: ThreatEvidenceSource,
    rawLossIfIgnoredCpForDiagnostics: Int,
    lossIfIgnoredWinPercent: Option[Double],
    turnsToImpact: Int,
    attackSquares: List[EvidenceSquare],
    targetPieces: List[EvidencePieceRole],
    motifs: List[Motif],
    bestDefense: Option[String],
    defenseCount: Int
):
  def immediate: Boolean =
    turnsToImpact <= 2
  def strategic: Boolean =
    turnsToImpact >= 3
  def defenseRequired: Boolean =
    severity != ThreatSeverity.Low
  def hasLineValueProof: Boolean =
    evidenceSource == ThreatEvidenceSource.CandidateLineValueDelta ||
      evidenceSource == ThreatEvidenceSource.MotifAndLineValueDelta
  def hasMotifProof: Boolean =
    motifs.nonEmpty
  def hasConcreteThreatProof: Boolean =
    kind == ThreatKind.Mate || hasLineValueProof || hasMotifProof
  def motifKinds: List[String] =
    motifs.map(_.getClass.getSimpleName.stripSuffix("$")).distinct

object ThreatEpisode:
  def fromThreat(sideUnderPressure: Color, threat: Threat, index: Int): ThreatEpisode =
    ThreatEpisode(
      episodeId = s"${sideUnderPressure.name}:threat:$index:${threat.kind}:${threat.turnsToImpact}",
      sourceThreatIndex = index,
      sideUnderPressure = sideUnderPressure,
      kind = threat.kind,
      severity = threat.severity,
      driver = driverFor(threat),
      evidenceSource = threat.evidenceSource,
      rawLossIfIgnoredCpForDiagnostics = threat.lossIfIgnoredCp,
      lossIfIgnoredWinPercent = threat.lossIfIgnoredWinPercent,
      turnsToImpact = threat.turnsToImpact,
      attackSquares = threat.attackSquares.distinct.map(EvidenceSquare(_)),
      targetPieces = threat.targetPieces.distinct.map(EvidencePieceRole(_)),
      motifs = threat.motifs,
      bestDefense = threat.bestDefense.map(EvidenceRef.normalizeMove),
      defenseCount = threat.defenseCount
    )

  def fromAnalysis(sideUnderPressure: Color, analysis: ThreatAnalysis): List[ThreatEpisode] =
    analysis.threats.zipWithIndex.map { case (threat, index) =>
      fromThreat(sideUnderPressure, threat, index)
    }

  private def driverFor(threat: Threat): ThreatDriver =
    threat.kind match
      case ThreatKind.Mate       => ThreatDriver.MateThreat
      case ThreatKind.Material   => ThreatDriver.MaterialThreat
      case ThreatKind.Positional => ThreatDriver.PositionalThreat

final case class ThreatPressureEvidence(
    sideUnderPressure: Color,
    threats: ThreatAnalysis
) extends EvidencePayload:
  val layer: EvidenceLayer = EvidenceLayer.ThreatPressure
  def episodes: List[ThreatEpisode] =
    ThreatEpisode.fromAnalysis(sideUnderPressure, threats)
  def hasProofSignalThreatEpisode: Boolean =
    episodes.exists(_.hasConcreteThreatProof)

final case class ThreatEpisodeEvidence(
    episode: ThreatEpisode,
    summary: ThreatAnalysis
) extends EvidencePayload:
  val layer: EvidenceLayer = EvidenceLayer.ThreatPressure
  def sideUnderPressure: Color =
    episode.sideUnderPressure
  def defenseRequired: Boolean =
    episode.defenseRequired
  def onlyDefense: Option[String] =
    episode.bestDefense.filter(_ => episode.defenseCount == 1)
  def prophylaxisNeeded: Boolean =
    episode.strategic && episode.defenseRequired
  def insufficientData: Boolean =
    !episode.hasConcreteThreatProof
  def maxWinPercentLossIfIgnored: Option[Double] =
    episode.lossIfIgnoredWinPercent
  def isProofSignalDefensivePressure: Boolean =
    !insufficientData &&
      (defenseRequired || prophylaxisNeeded || episode.severity != ThreatSeverity.Low)

final case class ForcedLineThemeEvidence(
    id: String,
    lineMoves: List[String]
)

final case class LineReplayStep(
    ply: Int,
    moveUci: String,
    fenBefore: String,
    fenAfter: String
)

enum LineEventKind:
  case Capture
  case Recapture
  case DefenderMove
  case Threat
  case Castling
  case Check
  case Mate
  case Tempo
  case Stalemate
  case Promotion
  case ForcedTheme

final case class LineMoveEvent(
    kind: LineEventKind,
    moveUci: String,
    plyOffset: Int,
    side: Option[Color] = None,
    pieceRole: Option[EvidencePieceRole] = None,
    targetRole: Option[EvidencePieceRole] = None,
    square: Option[EvidenceSquare] = None
)

enum LineConsequenceKind:
  case ForcedTheme
  case ImmediateReplyCheck
  case Mate
  case DrawResource
  case MaterialGain
  case MaterialLoss
  case RecaptureSequence
  case RecoveryWindow
  case Sacrifice
  case PromotionRace

object LineConsequenceKind:
  def tacticalDriver(kind: LineConsequenceKind): Boolean =
    kind match
      case LineConsequenceKind.MaterialGain | LineConsequenceKind.MaterialLoss |
          LineConsequenceKind.RecaptureSequence | LineConsequenceKind.RecoveryWindow |
          LineConsequenceKind.ImmediateReplyCheck | LineConsequenceKind.Mate |
          LineConsequenceKind.DrawResource | LineConsequenceKind.PromotionRace =>
        true
      case LineConsequenceKind.ForcedTheme | LineConsequenceKind.Sacrifice =>
        false

enum LineMaterialOutcomeSignal:
  case MoverCapture
  case OpponentCapture
  case PromotionGain
  case PromotionLoss
  case UnrecoveredPawnGain
  case UnrecoveredPawnLoss
  case RecoveryWindow

enum LineMaterialOutcomeMagnitude:
  case None
  case Pawn
  case Piece

final case class LineConsequence(
    kind: LineConsequenceKind,
    lineMoves: List[String],
    proofSignal: Boolean,
    eventMove: Option[String] = None
)

final case class LineConsequenceProfile(
    proofSignalKinds: List[LineConsequenceKind],
    hasConcreteProofSignal: Boolean,
    hasConversionConsequence: Boolean,
    hasMaterialResult: Boolean,
    hasRecaptureRecovery: Boolean,
    hasSacrifice: Boolean,
    hasPromotionRace: Boolean,
    hasMate: Boolean,
    hasDrawResource: Boolean
):
  def tacticalDriverKinds: List[LineConsequenceKind] =
    proofSignalKinds.filter(LineConsequenceKind.tacticalDriver)

final case class LineMaterialOutcomeProfile(
    gainSignals: Set[LineMaterialOutcomeSignal],
    lossSignals: Set[LineMaterialOutcomeSignal]
):
  def merge(other: LineMaterialOutcomeProfile): LineMaterialOutcomeProfile =
    LineMaterialOutcomeProfile(
      gainSignals = gainSignals ++ other.gainSignals,
      lossSignals = lossSignals ++ other.lossSignals
    )

  def gainMagnitude: LineMaterialOutcomeMagnitude =
    if gainSignals.exists(signal =>
        signal == LineMaterialOutcomeSignal.MoverCapture ||
          signal == LineMaterialOutcomeSignal.PromotionGain ||
          signal == LineMaterialOutcomeSignal.RecoveryWindow
      )
    then LineMaterialOutcomeMagnitude.Piece
    else if gainSignals.contains(LineMaterialOutcomeSignal.UnrecoveredPawnGain) then LineMaterialOutcomeMagnitude.Pawn
    else LineMaterialOutcomeMagnitude.None

  def lossMagnitude: LineMaterialOutcomeMagnitude =
    if lossSignals.exists(signal =>
        signal == LineMaterialOutcomeSignal.OpponentCapture ||
          signal == LineMaterialOutcomeSignal.PromotionLoss
      )
    then LineMaterialOutcomeMagnitude.Piece
    else if lossSignals.contains(LineMaterialOutcomeSignal.UnrecoveredPawnLoss) then LineMaterialOutcomeMagnitude.Pawn
    else LineMaterialOutcomeMagnitude.None

object LineMaterialOutcomeProfile:
  val empty: LineMaterialOutcomeProfile =
    LineMaterialOutcomeProfile(Set.empty, Set.empty)

final case class LineMaterialCapture(
    moveUci: String,
    plyOffset: Int,
    side: Color,
    attackerRole: EvidencePieceRole,
    capturedRole: EvidencePieceRole,
    square: EvidenceSquare,
    valueCp: Int,
    recapture: Boolean
)

final case class LineMaterialSummary(
    sideToMove: Color,
    captures: List[LineMaterialCapture],
    netCaptureCpForMover: Int,
    maxGainCpForMover: Int,
    maxLossCpForMover: Int,
    hasRecaptureChain: Boolean,
    hasRecoveryWindow: Boolean,
    promotionGainCpForMover: Int,
    materialWindowComplete: Boolean
):
  def hasPromotion: Boolean = promotionGainCpForMover != 0

  def capturesByMover: List[LineMaterialCapture] =
    captures.filter(_.side == sideToMove)

  def capturesByOpponent: List[LineMaterialCapture] =
    captures.filter(_.side != sideToMove)

  def nonPawnCapturesByMover: List[LineMaterialCapture] =
    capturesByMover.filter(capture => proofSignalCapturedRole(capture.capturedRole))

  def nonPawnCapturesByOpponent: List[LineMaterialCapture] =
    capturesByOpponent.filter(capture => proofSignalCapturedRole(capture.capturedRole))

  def pawnCapturesByMover: List[LineMaterialCapture] =
    capturesByMover.filter(capture => pawnCapturedRole(capture.capturedRole))

  def pawnCapturesByOpponent: List[LineMaterialCapture] =
    capturesByOpponent.filter(capture => pawnCapturedRole(capture.capturedRole))

  def hasPromotionGainForMover: Boolean =
    promotionGainCpForMover > 0

  def hasPromotionLossForMover: Boolean =
    promotionGainCpForMover < 0

  def hasResolvedMaterialSequence: Boolean =
    materialWindowComplete && (hasRecaptureChain || hasRecoveryWindow)

  def hasProofSignalMaterialGain: Boolean =
    materialWindowComplete && (nonPawnCapturesByMover.nonEmpty || hasPromotionGainForMover || hasRecoveryWindow)

  def hasProofSignalMaterialLoss: Boolean =
    materialWindowComplete && (nonPawnCapturesByOpponent.nonEmpty || hasPromotionLossForMover)

  def hasUnrecoveredPawnGainForMover: Boolean =
    materialWindowComplete && pawnCapturesByMover.nonEmpty && !hasRecoveryWindow

  def hasUnrecoveredPawnLossForMover: Boolean =
    materialWindowComplete && pawnCapturesByOpponent.nonEmpty && !hasRecoveryWindow

  def hasProofSignalMaterialEvent: Boolean =
    hasProofSignalMaterialGain ||
      hasProofSignalMaterialLoss ||
      hasUnrecoveredPawnGainForMover ||
      hasUnrecoveredPawnLossForMover ||
      hasResolvedMaterialSequence

  def hasSacrificeMaterialEvent: Boolean =
    materialWindowComplete &&
      capturesByOpponent.exists(capture => !capture.recapture) &&
      !hasRecoveryWindow &&
      !hasProofSignalMaterialGain

  private def proofSignalCapturedRole(role: EvidencePieceRole): Boolean =
    val normalized = role.name.trim.toLowerCase
    normalized.nonEmpty && normalized != "pawn" && normalized != "king"

  private def pawnCapturedRole(role: EvidencePieceRole): Boolean =
    role.name.trim.equalsIgnoreCase("pawn")

final case class LineFactEvidence(
    line: LineNodeRef,
    private val firstMove: Option[String],
    private val replyMove: Option[String],
    private val continuationMoves: List[String],
    private val forcedTheme: Option[ForcedLineThemeEvidence] = None,
    private val material: Option[LineMaterialSummary] = None
)(private val replay: List[LineReplayStep] = Nil,
    private val events: List[LineMoveEvent] = Nil,
    private val consequences: List[LineConsequence] = Nil
) extends EvidencePayload:
  val layer: EvidenceLayer = EvidenceLayer.Line
  def rootMove: Option[String] =
    firstMove
  def reply: Option[String] =
    replyMove
  def continuation: List[String] =
    continuationMoves
  def forcedThemeId: Option[String] =
    forcedTheme.map(_.id)
  def lineReplaySteps: List[LineReplayStep] =
    replay
  def lineReplayMoves: List[String] =
    replay.map(_.moveUci)
  def lineReplayContinuationMoves: List[String] =
    lineReplayMoves.drop(1)
  def lineEvents: List[LineMoveEvent] =
    events
  def lineConsequences: List[LineConsequence] =
    consequences
  def lineReplayCount: Int =
    replay.size
  def hasLineReplay: Boolean =
    replay.nonEmpty
  def lineEventKinds: List[LineEventKind] =
    events.map(_.kind)
  def lineEventsOf(kind: LineEventKind): List[LineMoveEvent] =
    events.filter(_.kind == kind)
  def hasLineEvent(kind: LineEventKind): Boolean =
    lineEventsOf(kind).nonEmpty
  def hasLineEventAt(kind: LineEventKind, plyOffset: Int): Boolean =
    lineEventsOf(kind).exists(_.plyOffset == plyOffset)
  def hasTempoEventAt(plyOffset: Int): Boolean =
    hasLineEventAt(LineEventKind.Tempo, plyOffset)
  def lineEventMoves(kind: LineEventKind): List[String] =
    lineEventsOf(kind).map(_.moveUci)
  def hasRootCaptureEvent(rootMoveUci: String): Boolean =
    val normalizedRoot = normalizeUci(rootMoveUci)
    events.exists(event =>
      (event.kind == LineEventKind.Capture || event.kind == LineEventKind.Recapture) &&
        normalizeUci(event.moveUci) == normalizedRoot
    )
  def lineEventCount: Int =
    events.size
  def hasLineEvents: Boolean =
    events.nonEmpty
  def lineConsequenceCount: Int =
    consequences.size
  def hasLineConsequences: Boolean =
    consequences.nonEmpty
  def hasForcedTheme: Boolean =
    forcedTheme.nonEmpty
  def materialNetCaptureCpForMover: Option[Int] =
    material.map(_.netCaptureCpForMover)
  def materialMaxGainCpForMover: Option[Int] =
    material.map(_.maxGainCpForMover)
  def materialPromotionGainCpForMover: Option[Int] =
    material.map(_.promotionGainCpForMover)
  def hasMaterialRecaptureChain: Boolean =
    material.exists(_.hasRecaptureChain)
  def hasMaterialRecoveryWindow: Boolean =
    material.exists(_.hasRecoveryWindow)
  def hasCompleteMaterialWindow: Boolean =
    material.forall(_.materialWindowComplete)
  def hasProofSignalMaterialEvent: Boolean =
    material.exists(_.hasProofSignalMaterialEvent)
  def hasSacrificeMaterialEvent: Boolean =
    material.exists(_.hasSacrificeMaterialEvent)
  def proofSignalConsequences: List[LineConsequence] =
    consequences.filter(_.proofSignal)
  def hasProofSignalConsequence: Boolean =
    proofSignalConsequences.nonEmpty
  def proofSignalConsequenceKinds: List[LineConsequenceKind] =
    proofSignalConsequences.map(_.kind)
  def hasProofSignalConsequence(kind: LineConsequenceKind): Boolean =
    proofSignalConsequenceKinds.contains(kind)
  def consequenceProfile: LineConsequenceProfile =
    val kinds = proofSignalConsequenceKinds
    LineConsequenceProfile(
      proofSignalKinds = kinds,
      hasConcreteProofSignal = kinds.nonEmpty,
      hasConversionConsequence = kinds.exists {
        case LineConsequenceKind.RecaptureSequence | LineConsequenceKind.RecoveryWindow |
            LineConsequenceKind.MaterialGain | LineConsequenceKind.MaterialLoss |
            LineConsequenceKind.Sacrifice =>
          true
        case _ =>
          false
      },
      hasMaterialResult = kinds.exists {
        case LineConsequenceKind.MaterialGain | LineConsequenceKind.MaterialLoss |
            LineConsequenceKind.Sacrifice | LineConsequenceKind.PromotionRace =>
          true
        case _ =>
          false
      },
      hasRecaptureRecovery = kinds.exists(kind =>
        kind == LineConsequenceKind.RecaptureSequence || kind == LineConsequenceKind.RecoveryWindow
      ),
      hasSacrifice = kinds.contains(LineConsequenceKind.Sacrifice),
      hasPromotionRace = kinds.contains(LineConsequenceKind.PromotionRace),
      hasMate = kinds.contains(LineConsequenceKind.Mate),
      hasDrawResource = kinds.contains(LineConsequenceKind.DrawResource)
    )
  def hasConcreteLineConsequence: Boolean =
    consequenceProfile.hasConcreteProofSignal
  def hasConversionConsequence: Boolean =
    consequenceProfile.hasConversionConsequence
  def hasMaterialConsequence: Boolean =
    consequenceProfile.hasMaterialResult
  def hasRecaptureRecoveryConsequence: Boolean =
    consequenceProfile.hasRecaptureRecovery
  def hasSacrificeConsequence: Boolean =
    consequenceProfile.hasSacrifice
  def tacticalLineConsequenceKinds: List[LineConsequenceKind] =
    consequenceProfile.tacticalDriverKinds
  def hasTacticalLineConsequence: Boolean =
    tacticalLineConsequenceKinds.nonEmpty
  def semanticGroupingAnchors: List[EvidenceSemanticAnchor] =
    Option
      .when(hasLineEvent(LineEventKind.Castling))(
        EvidenceSemanticAnchor.of(EvidenceSemanticAnchorKind.LineEvent, LineEventKind.Castling.toString)
      )
      .toList ++
      proofSignalConsequenceKinds.map(kind =>
        EvidenceSemanticAnchor.of(EvidenceSemanticAnchorKind.LineConsequence, kind.toString)
      )

  private def normalizeUci(raw: String): String =
    Option(raw).getOrElse("").trim.toLowerCase
  def materialOutcomeProfile: LineMaterialOutcomeProfile =
    val consequenceGainSignals =
      consequences.collect {
        case LineConsequence(LineConsequenceKind.MaterialGain, _, true, _) =>
          LineMaterialOutcomeSignal.MoverCapture
        case LineConsequence(LineConsequenceKind.MaterialGain, _, false, _) =>
          LineMaterialOutcomeSignal.UnrecoveredPawnGain
        case LineConsequence(LineConsequenceKind.RecoveryWindow, _, true, _) =>
          LineMaterialOutcomeSignal.RecoveryWindow
      }.toSet
    val consequenceLossSignals =
      consequences.collect {
        case LineConsequence(LineConsequenceKind.MaterialLoss, _, true, _) =>
          LineMaterialOutcomeSignal.OpponentCapture
        case LineConsequence(LineConsequenceKind.MaterialLoss, _, false, _) =>
          LineMaterialOutcomeSignal.UnrecoveredPawnLoss
      }.toSet
    val materialGainSignals =
      material
        .map(summary =>
          Set(
            Option.when(summary.nonPawnCapturesByMover.nonEmpty)(LineMaterialOutcomeSignal.MoverCapture),
            Option.when(summary.hasPromotionGainForMover)(LineMaterialOutcomeSignal.PromotionGain),
            Option.when(summary.hasUnrecoveredPawnGainForMover)(LineMaterialOutcomeSignal.UnrecoveredPawnGain),
            Option.when(summary.hasRecoveryWindow)(LineMaterialOutcomeSignal.RecoveryWindow)
          ).flatten
        )
        .getOrElse(Set.empty)
    val materialLossSignals =
      material
        .map(summary =>
          Set(
            Option.when(summary.nonPawnCapturesByOpponent.nonEmpty)(LineMaterialOutcomeSignal.OpponentCapture),
            Option.when(summary.hasPromotionLossForMover)(LineMaterialOutcomeSignal.PromotionLoss),
            Option.when(summary.hasUnrecoveredPawnLossForMover)(LineMaterialOutcomeSignal.UnrecoveredPawnLoss)
          ).flatten
        )
        .getOrElse(Set.empty)
    LineMaterialOutcomeProfile(
      gainSignals = consequenceGainSignals ++ materialGainSignals,
      lossSignals = consequenceLossSignals ++ materialLossSignals
    )
  def hasMaterialOutcomeSignals: Boolean =
    val profile = materialOutcomeProfile
    profile.gainSignals.nonEmpty || profile.lossSignals.nonEmpty

object LineFactEvidence:
  def fromRecords(records: List[EvidenceRecord]): List[LineFactEvidence] =
    records.collect { case EvidenceRecord(_, payload: LineFactEvidence, _) => payload }

  def materialOutcomeProfile(records: List[EvidenceRecord]): LineMaterialOutcomeProfile =
    fromRecords(records).map(_.materialOutcomeProfile).foldLeft(LineMaterialOutcomeProfile.empty)(_.merge(_))

  def maxMaterialNetCaptureCpForMover(records: List[EvidenceRecord]): Int =
    fromRecords(records).flatMap(_.materialNetCaptureCpForMover).maxOption.getOrElse(0)

  def maxMaterialGainCpForMover(records: List[EvidenceRecord]): Int =
    fromRecords(records).flatMap(_.materialMaxGainCpForMover).maxOption.getOrElse(0)

  def maxMaterialPromotionGainCpForMover(records: List[EvidenceRecord]): Int =
    fromRecords(records).flatMap(_.materialPromotionGainCpForMover).maxOption.getOrElse(0)

  def hasMaterialRecaptureChain(records: List[EvidenceRecord]): Boolean =
    fromRecords(records).exists(_.hasMaterialRecaptureChain)

  def hasMaterialRecoveryWindow(records: List[EvidenceRecord]): Boolean =
    fromRecords(records).exists(_.hasMaterialRecoveryWindow)

  def allHaveCompleteMaterialWindow(records: List[EvidenceRecord]): Boolean =
    fromRecords(records).forall(_.hasCompleteMaterialWindow)

  def apply(
      line: LineNodeRef,
      firstMove: Option[String],
      replyMove: Option[String],
      continuationMoves: List[String]
  ): LineFactEvidence =
    new LineFactEvidence(line, firstMove, replyMove, continuationMoves, None, None)()

  def apply(
      line: LineNodeRef,
      firstMove: Option[String],
      replyMove: Option[String],
      continuationMoves: List[String],
      forcedTheme: Option[ForcedLineThemeEvidence]
  ): LineFactEvidence =
    new LineFactEvidence(line, firstMove, replyMove, continuationMoves, forcedTheme, None)()

  def apply(
      line: LineNodeRef,
      firstMove: Option[String],
      replyMove: Option[String],
      continuationMoves: List[String],
      forcedTheme: Option[ForcedLineThemeEvidence],
      material: Option[LineMaterialSummary]
  ): LineFactEvidence =
    new LineFactEvidence(line, firstMove, replyMove, continuationMoves, forcedTheme, material)()

final case class EvalFactEvidence(
    line: LineNodeRef,
    whitePovEvalCp: Int,
    mate: Option[Int],
    depth: Int
) extends EvidencePayload:
  val layer: EvidenceLayer = EvidenceLayer.Eval
  def evalPoint: PerspectiveMath.EvalPoint =
    PerspectiveMath.EvalPoint(whitePovEvalCp, mate)
  def whiteWinPercent: Double =
    PerspectiveMath.winPercentFromWhiteEval(whitePovEvalCp, mate)
  def winPercentFor(mover: Color): Double =
    PerspectiveMath.winPercentForMover(mover, whitePovEvalCp, mate)
  def winPercentAdvantageFor(mover: Color): Double =
    PerspectiveMath.winPercentAdvantageFor(mover, whitePovEvalCp, mate)

final case class MoveMotifProof(
    kind: String,
    category: MotifCategory,
    color: Color,
    subjectSquares: List[EvidenceSquare] = Nil,
    targetSquares: List[EvidenceSquare] = Nil,
    relatedSquares: List[EvidenceSquare] = Nil,
    relatedFiles: List[EvidenceFile] = Nil,
    roles: List[EvidencePieceRole] = Nil
):
  def focusSquares: List[EvidenceSquare] =
    (subjectSquares ++ targetSquares ++ relatedSquares).distinct

final case class MoveMotifEvent(
    rootMove: String,
    eventMove: Option[String],
    plyOffset: Int,
    line: Option[LineNodeRef],
    lineRole: Option[LineNodeRole],
    position: PositionNodeRef,
    scope: EvidenceScope,
    motif: Motif,
    proof: MoveMotifProof
):
  def isRootEvent: Boolean =
    eventMove.exists(move => EvidenceRef.sameMove(move, rootMove)) ||
      (eventMove.isEmpty && plyOffset == 0)
  def kind: String =
    proof.kind
  def category: MotifCategory =
    proof.category

object MoveMotifEvent:
  def fromMotif(
      rootMove: String,
      motif: Motif,
      position: PositionNodeRef,
      line: Option[LineNodeRef],
      scope: EvidenceScope
  ): MoveMotifEvent =
    MoveMotifEvent(
      rootMove = rootMove,
      eventMove = motif.move.map(EvidenceRef.normalizeMove),
      plyOffset = motif.plyIndex,
      line = line,
      lineRole = line.map(_.role),
      position = position,
      scope = scope,
      motif = motif,
      proof = proofFor(motif)
    )

  private def proofFor(motif: Motif): MoveMotifProof =
    val (subjectSquares, targetSquares, relatedSquares, roles) =
      motif match
        case Motif.PawnAdvance(file, fromRank, toRank, _, _, _) =>
          (Nil, Nil, List(squareKey(file, fromRank), squareKey(file, toRank)).flatten, List(Pawn))
        case Motif.PawnBreak(file, targetFile, _, _, _) =>
          (Nil, Nil, Nil, List(Pawn))
        case Motif.PawnPromotion(file, promotedTo, color, _, _) =>
          (Nil, List(squareKey(file, if color.white then 8 else 1)).flatten, Nil, List(Pawn, promotedTo))
        case Motif.PassedPawnPush(file, toRank, _, _, _) =>
          (Nil, Nil, List(squareKey(file, toRank)).flatten, List(Pawn))
        case Motif.RookLift(file, fromRank, toRank, _, _, _) =>
          (Nil, Nil, List(squareKey(file, fromRank), squareKey(file, toRank)).flatten, List(Rook))
        case Motif.Outpost(piece, square, _, _, _) =>
          (List(evidenceSquare(square)), Nil, Nil, List(piece))
        case Motif.Centralization(piece, square, _, _, _) =>
          (List(evidenceSquare(square)), Nil, Nil, List(piece))
        case Motif.Check(piece, targetSquare, _, _, _, _) =>
          (Nil, List(evidenceSquare(targetSquare)), Nil, List(piece, King))
        case Motif.Capture(piece, captured, square, _, _, _, _, _) =>
          (Nil, List(evidenceSquare(square)), Nil, List(piece, captured))
        case Motif.Zwischenzug(_, _, expectedRecaptureSquare, _, _, _) =>
          (Nil, List(evidenceSquare(expectedRecaptureSquare)), Nil, Nil)
        case Motif.Pin(pinningPiece, pinnedPiece, targetBehind, _, _, _, pinningSq, pinnedSq, behindSq) =>
          (
            pinningSq.map(evidenceSquare).toList,
            pinnedSq.map(evidenceSquare).toList,
            behindSq.map(evidenceSquare).toList,
            List(pinningPiece, pinnedPiece, targetBehind)
          )
        case Motif.Fork(attackingPiece, targets, square, targetSquares, _, _, _) =>
          (List(evidenceSquare(square)), targetSquares.map(evidenceSquare), Nil, attackingPiece :: targets)
        case Motif.Domination(dominatingPiece, dominatedPiece, square, _, _, _) =>
          (List(evidenceSquare(square)), Nil, Nil, List(dominatingPiece, dominatedPiece))
        case Motif.Skewer(attackingPiece, frontPiece, backPiece, _, _, _, attackingSq, frontSq, backSq) =>
          (
            attackingSq.map(evidenceSquare).toList,
            frontSq.map(evidenceSquare).toList,
            backSq.map(evidenceSquare).toList,
            List(attackingPiece, frontPiece, backPiece)
          )
        case Motif.DiscoveredAttack(movingPiece, attackingPiece, target, _, _, _, movingSq, attackingSq, targetSq) =>
          (
            movingSq.map(evidenceSquare).toList ++ attackingSq.map(evidenceSquare).toList,
            targetSq.map(evidenceSquare).toList,
            Nil,
            List(movingPiece, attackingPiece, target)
          )
        case Motif.RemovingTheDefender(attacker, victim, protectedTarget, square, _, _, _) =>
          (Nil, List(evidenceSquare(square)), Nil, List(attacker, victim, protectedTarget))
        case Motif.Deflection(piece, fromSquare, _, _, _) =>
          (List(evidenceSquare(fromSquare)), Nil, Nil, List(piece))
        case Motif.Decoy(piece, toSquare, _, _, _) =>
          (Nil, List(evidenceSquare(toSquare)), Nil, List(piece))
        case Motif.XRay(piece, target, square, _, _, _) =>
          (List(evidenceSquare(square)), Nil, Nil, List(piece, target))
        case Motif.Overloading(overloadedPiece, overloadedSquare, duties, _, _, _) =>
          (List(evidenceSquare(overloadedSquare)), duties.map(evidenceSquare), Nil, List(overloadedPiece))
        case Motif.DoubleCheck(movingPiece, revealedPiece, _, _, _) =>
          (Nil, Nil, Nil, List(movingPiece, revealedPiece, King))
        case Motif.BackRankMate(_, attackingPiece, _, _, _) =>
          (Nil, Nil, Nil, List(attackingPiece, King))
        case Motif.TrappedPiece(trappedRole, trappedSquare, _, _, _) =>
          (Nil, List(evidenceSquare(trappedSquare)), Nil, List(trappedRole))
        case Motif.MateNet(kingSquare, attackers, _, _, _) =>
          (Nil, List(evidenceSquare(kingSquare)), Nil, King :: attackers)
        case Motif.Interference(interferingPiece, interferingSquare, blockedPiece1, blockedPiece2, _, _, _) =>
          (List(evidenceSquare(interferingSquare)), Nil, Nil, List(interferingPiece, blockedPiece1, blockedPiece2))
        case Motif.Clearance(clearingPiece, clearingFrom, _, beneficiary, _, _, _) =>
          (List(evidenceSquare(clearingFrom)), Nil, Nil, List(clearingPiece, beneficiary))
        case Motif.DoubledPieces(role, file, _, _, _) =>
          (Nil, Nil, Nil, List(role))
        case Motif.Battery(front, back, _, _, _, _, frontSq, backSq) =>
          (frontSq.map(evidenceSquare).toList, backSq.map(evidenceSquare).toList, Nil, List(front, back))
        case Motif.IsolatedPawn(file, rank, _, _, _) =>
          (Nil, List(squareKey(file, rank)).flatten, Nil, List(Pawn))
        case Motif.BackwardPawn(file, rank, _, _, _) =>
          (Nil, List(squareKey(file, rank)).flatten, Nil, List(Pawn))
        case Motif.PassedPawn(file, rank, _, _, _, _) =>
          (Nil, List(squareKey(file, rank)).flatten, Nil, List(Pawn))
        case Motif.DoubledPawns(file, _, _, _) =>
          (Nil, Nil, Nil, List(Pawn))
        case Motif.PawnChain(baseFile, tipFile, _, _, _) =>
          (Nil, Nil, Nil, List(Pawn))
        case Motif.Opposition(opponentKingSquare, ownKingSquare, _, _, _, _) =>
          (List(evidenceSquare(ownKingSquare)), List(evidenceSquare(opponentKingSquare)), Nil, List(King))
        case Motif.OpenFileControl(file, _, _, _) =>
          (Nil, Nil, Nil, Nil)
        case Motif.SemiOpenFileControl(file, _, _, _) =>
          (Nil, Nil, Nil, Nil)
        case Motif.RookBehindPassedPawn(file, _, _, _) =>
          (Nil, Nil, Nil, List(Rook, Pawn))
        case Motif.KingCutOff(_, coordinate, _, _, _) =>
          (Nil, Nil, Nil, List(King))
        case Motif.Blockade(piece, square, pawnSquare, _, _, _) =>
          (List(evidenceSquare(square)), List(evidenceSquare(pawnSquare)), Nil, List(piece, Pawn))
        case Motif.SmotheredMate(_, kingSquare, _, _) =>
          (Nil, List(evidenceSquare(kingSquare)), Nil, List(Knight, King))
        case _ =>
          (Nil, Nil, Nil, Nil)
    MoveMotifProof(
      kind = motif.getClass.getSimpleName.stripSuffix("$"),
      category = motif.category,
      color = motif.color,
      subjectSquares = subjectSquares.distinct,
      targetSquares = targetSquares.distinct,
      relatedSquares = relatedSquares.distinct,
      relatedFiles = filesFor(motif),
      roles = roles.distinct.map(role => EvidencePieceRole(role.toString))
    )

  private def filesFor(motif: Motif): List[EvidenceFile] =
    val files =
      motif match
        case Motif.PawnAdvance(file, _, _, _, _, _)         => List(file)
        case Motif.PawnBreak(file, targetFile, _, _, _)     => List(file, targetFile)
        case Motif.PawnPromotion(file, _, _, _, _)          => List(file)
        case Motif.PassedPawnPush(file, _, _, _, _)         => List(file)
        case Motif.RookLift(file, _, _, _, _, _)            => List(file)
        case Motif.DoubledPieces(_, file, _, _, _)          => List(file)
        case Motif.IsolatedPawn(file, _, _, _, _)           => List(file)
        case Motif.BackwardPawn(file, _, _, _, _)           => List(file)
        case Motif.PassedPawn(file, _, _, _, _, _)          => List(file)
        case Motif.DoubledPawns(file, _, _, _)              => List(file)
        case Motif.PawnChain(baseFile, tipFile, _, _, _)    => List(baseFile, tipFile)
        case Motif.OpenFileControl(file, _, _, _)           => List(file)
        case Motif.SemiOpenFileControl(file, _, _, _)       => List(file)
        case Motif.RookBehindPassedPawn(file, _, _, _)      => List(file)
        case _                                              => Nil
    files.distinct.map(file => EvidenceFile(file.toString.toLowerCase))

  private def evidenceSquare(square: Square): EvidenceSquare =
    EvidenceSquare(square.key)

  private def squareKey(file: chess.File, rank: Int): Option[EvidenceSquare] =
    Square.fromKey(s"${file.toString.toLowerCase}$rank").map(evidenceSquare)

final case class MoveMotifEvidence(
    event: MoveMotifEvent
) extends EvidencePayload:
  val layer: EvidenceLayer = EvidenceLayer.MoveMotif
  def moveUci: String = event.rootMove
  def rootMove: String = event.rootMove
  def motif: Motif = event.motif
  def proof: MoveMotifProof = event.proof
  def eventMove: Option[String] = event.eventMove
  def plyOffset: Int = event.plyOffset
  def line: Option[LineNodeRef] = event.line
  def lineRole: Option[LineNodeRole] = event.lineRole
  def isRootEvent: Boolean = event.isRootEvent
  def recordLineBound(ref: EvidenceRef): Boolean =
    isRootEvent &&
      line.forall(boundLine => ref.line.contains(boundLine)) &&
      ref.line.forall(lineRef => EvidenceRef.sameMove(lineRef.rootMove, moveUci))
  def sameMotifContext(ref: EvidenceRef, otherRef: EvidenceRef, other: MoveMotifEvidence): Boolean =
    EvidenceRef.sameMove(rootMove, other.rootMove) &&
      (
        (ref.line, otherRef.line) match
          case (Some(left), Some(right)) => left == right
          case (None, None)             => ref.position == otherRef.position && ref.scope == otherRef.scope
          case _                        => false
      )

final case class MoveTransitionEvidence(
    moveUci: String,
    from: PositionNodeRef,
    to: PositionNodeRef
) extends EvidencePayload:
  val layer: EvidenceLayer = EvidenceLayer.MoveTransition

enum StructuralSignalPolarity:
  case Gain
  case Loss
  case Neutral

enum StructuralSignalKind:
  case FileOpened
  case SemiOpenFileCreated
  case FileAccessChanged
  case FileOccupied
  case WeakPawnCreated
  case WeakSquareCreated
  case PawnTensionCreated
  case PawnTensionResolved
  case PawnTensionChanged
  case TargetPressureCreated
  case TargetPressureReleased
  case TargetPressureChanged
  case CenterControlChanged
  case DevelopmentChanged
  case DevelopmentChoice
  case MobilityChanged
  case KingSafetyChanged
  case LineUnlocked
  case PassedPawnCreated
  case PassedPawnAdvanced
  case PromotionPressureChanged
  case OutpostCreated
  case OutpostRemoved
  case RookLiftCreated
  case BatteryCreated
  case KingRingPressureChanged

final case class StructuralSignal(
    kind: StructuralSignalKind,
    polarity: StructuralSignalPolarity,
    magnitude: Int,
    subjects: List[String] = Nil
):
  def anchorKey: String =
    s"$kind:$polarity"

enum TransitionConsequenceKind:
  case OpenFileGain
  case SemiOpenFileGain
  case FileOccupationGain
  case WeakPawnTargetCreated
  case WeakSquareTargetCreated
  case PawnTensionGain
  case PawnTensionResolution
  case TargetPressureGain
  case TargetPressureRelease
  case CenterControlGain
  case CenterControlLoss
  case DevelopmentLagReduced
  case DevelopmentLagIncreased
  case DevelopmentPieceActivated
  case DevelopmentPieceRetreated
  case DevelopmentMobilityGain
  case DevelopmentMobilityLoss
  case DevelopmentCenterControlGain
  case DevelopmentCenterControlLoss
  case DevelopmentSafePlacement
  case DevelopmentUnsafePlacement
  case MobilityGain
  case MobilityLoss
  case LineUnlockGain
  case FileAccessGain
  case FileAccessLoss
  case KingSafetyPressure
  case KingSafetyConcession
  case PassedPawnProgress
  case PassedPawnConcession
  case PromotionPressureGain
  case PromotionPressureConcession
  case OutpostGain
  case OutpostConcession
  case RookLiftActivation
  case BatteryPressureGain
  case KingRingPressureGain
  case KingRingPressureConcession

enum TransitionConsequenceCategory:
  case PawnStructure
  case PawnStructureDelta
  case Development
  case PieceActivity
  case TargetPressure
  case CenterControl
  case StructuralAnchor
  case StrategicMove
  case StrategicSupport
  case PlanAnchor
  case OpeningCenterControl
  case OpeningDevelopment

final case class TransitionConsequence(
    kind: TransitionConsequenceKind,
    polarity: StructuralSignalPolarity,
    strength: Int,
    subjects: List[String] = Nil
):
  def positive: Boolean =
    polarity == StructuralSignalPolarity.Gain
  def negative: Boolean =
    polarity == StructuralSignalPolarity.Loss
  def anchorKey: String =
    s"$kind:$polarity"

final case class StructuralDevelopmentChoice(
    role: String,
    from: String,
    to: String
)

final case class StructuralTransitionBinding(
    moveUci: String,
    role: TransitionEdgeRole,
    from: PositionNodeRef,
    to: PositionNodeRef,
    line: Option[LineNodeRef],
    perspective: Color
)

final case class RelationFactEvidence(
    kind: RelationFactKind,
    focusSquares: List[EvidenceSquare],
    targetSquare: Option[EvidenceSquare],
    lineMoves: List[String],
    participants: List[RelationParticipant]
)(val witnessProof: RelationWitnessProof = RelationWitnessProof.empty
) extends EvidencePayload:
  val layer: EvidenceLayer = EvidenceLayer.Relation
  def detail: RelationWitnessDetail =
    witnessProof.detail
  def hasTypedWitness: Boolean =
    witnessProof.hasTypedDetail
  def proofAtoms: List[RelationProofAtom] =
    witnessProof.proofAtoms
  def hasLineProof: Boolean =
    witnessProof.hasLineProof
  def lineProofCount: Int =
    proofAtoms.count(_.role == RelationProofAtomRole.LineMove)
  def hasParticipantProof: Boolean =
    proofAtoms.exists(_.role == RelationProofAtomRole.Participant)
  def hasConcreteRelationProof: Boolean =
    hasTypedWitness && proofAtoms.nonEmpty
  def mentionsLineMove(moveUci: String): Boolean =
    lineMoves.exists(EvidenceRef.sameMove(_, moveUci))

enum TacticalMechanismKind:
  case KingForcing
  case MaterialGain
  case RecaptureChoice
  case Tempo
  case RelationMechanism
  case Conversion
  case Refutation
  case DrawResource
  case PawnPromotion
  case DefensiveResource

object TacticalMechanismKind:
  def fromMotif(motif: Motif): List[TacticalMechanismKind] =
    motif match
      case m: Motif.Check =>
        List(TacticalMechanismKind.KingForcing) ++
          Option.when(m.checkType == Motif.CheckType.Mate || m.checkType == Motif.CheckType.Smothered)(
            TacticalMechanismKind.Refutation
          ).toList
      case _: Motif.DoubleCheck | _: Motif.BackRankMate | _: Motif.MateNet | _: Motif.SmotheredMate =>
        List(TacticalMechanismKind.KingForcing)
      case m: Motif.Capture =>
        m.captureType match
          case Motif.CaptureType.Recapture =>
            List(TacticalMechanismKind.RecaptureChoice)
          case Motif.CaptureType.Exchange | Motif.CaptureType.ExchangeSacrifice =>
            List(TacticalMechanismKind.MaterialGain, TacticalMechanismKind.Conversion)
          case Motif.CaptureType.Winning | Motif.CaptureType.Sacrifice =>
            List(TacticalMechanismKind.MaterialGain)
          case Motif.CaptureType.Normal =>
            Nil
      case _: Motif.Zwischenzug =>
        List(TacticalMechanismKind.Tempo, TacticalMechanismKind.RecaptureChoice)
      case _: Motif.Fork | _: Motif.Pin | _: Motif.Skewer | _: Motif.DiscoveredAttack |
          _: Motif.RemovingTheDefender | _: Motif.Deflection | _: Motif.Decoy | _: Motif.XRay |
          _: Motif.Overloading | _: Motif.Interference | _: Motif.Clearance | _: Motif.Battery =>
        List(TacticalMechanismKind.RelationMechanism)
      case _: Motif.TrappedPiece | _: Motif.Domination =>
        List(TacticalMechanismKind.MaterialGain)
      case _: Motif.PawnPromotion | _: Motif.PassedPawnPush =>
        List(TacticalMechanismKind.PawnPromotion)
      case _: Motif.StalemateThreat =>
        List(TacticalMechanismKind.DrawResource)
      case _ =>
        Nil

  def fromRelation(kind: RelationFactKind): TacticalMechanismKind =
    kind match
      case RelationFactKind.DoubleCheck | RelationFactKind.BackRankMate | RelationFactKind.MateNet | RelationFactKind.GreekGift =>
        TacticalMechanismKind.KingForcing
      case RelationFactKind.DefenderTrade =>
        TacticalMechanismKind.RecaptureChoice
      case RelationFactKind.HangingPiece | RelationFactKind.TrappedPiece | RelationFactKind.Domination =>
        TacticalMechanismKind.MaterialGain
      case RelationFactKind.Zwischenzug =>
        TacticalMechanismKind.Tempo
      case RelationFactKind.BadPieceLiquidation =>
        TacticalMechanismKind.Conversion
      case RelationFactKind.StalemateTrap | RelationFactKind.PerpetualCheck =>
        TacticalMechanismKind.DrawResource
      case _ =>
        TacticalMechanismKind.RelationMechanism

  def fromLineConsequence(kind: LineConsequenceKind): List[TacticalMechanismKind] =
    kind match
      case LineConsequenceKind.MaterialGain | LineConsequenceKind.MaterialLoss =>
        List(TacticalMechanismKind.MaterialGain)
      case LineConsequenceKind.RecaptureSequence | LineConsequenceKind.RecoveryWindow =>
        List(TacticalMechanismKind.RecaptureChoice)
      case LineConsequenceKind.ImmediateReplyCheck =>
        List(TacticalMechanismKind.Tempo)
      case LineConsequenceKind.Mate =>
        List(TacticalMechanismKind.KingForcing)
      case LineConsequenceKind.DrawResource =>
        List(TacticalMechanismKind.DrawResource)
      case LineConsequenceKind.PromotionRace =>
        List(TacticalMechanismKind.PawnPromotion)
      case LineConsequenceKind.ForcedTheme | LineConsequenceKind.Sacrifice =>
        Nil

  def relativeCauseKind(
      kind: TacticalMechanismKind,
      badLoss: Boolean,
      playedCandidate: Boolean
  ): RelativeCauseKind =
    kind match
      case TacticalMechanismKind.KingForcing =>
        RelativeCauseKind.KingForcing
      case TacticalMechanismKind.RecaptureChoice =>
        if badLoss then RelativeCauseKind.WrongRecapturer else RelativeCauseKind.RecaptureRecoveryWindow
      case TacticalMechanismKind.Tempo =>
        RelativeCauseKind.TempoLoss
      case TacticalMechanismKind.Conversion =>
        if badLoss then RelativeCauseKind.ConversionMiss else RelativeCauseKind.ConversionSecured
      case TacticalMechanismKind.DrawResource =>
        RelativeCauseKind.DrawResource
      case TacticalMechanismKind.DefensiveResource =>
        RelativeCauseKind.DefensiveResource
      case TacticalMechanismKind.MaterialGain | TacticalMechanismKind.RelationMechanism |
          TacticalMechanismKind.Refutation | TacticalMechanismKind.PawnPromotion =>
        if badLoss then
          if playedCandidate then RelativeCauseKind.TacticalRefutationOfPlayed
          else RelativeCauseKind.CandidateTacticalLiability
        else RelativeCauseKind.MissedTacticalResource

enum TacticalMechanismSignalKind:
  case Motif
  case Relation
  case LineConsequence
  case MateBranch
  case ThreatEpisode

final case class TacticalMechanismSignal(
    kind: TacticalMechanismSignalKind,
    label: String,
    sourceLayer: EvidenceLayer
)

final case class TacticalMechanismEvidence(
    kind: TacticalMechanismKind,
    moveUci: Option[String],
    line: Option[LineNodeRef],
    signals: List[TacticalMechanismSignal]
) extends EvidencePayload:
  val layer: EvidenceLayer = EvidenceLayer.TacticalMechanism
  def signalKinds: Set[TacticalMechanismSignalKind] =
    signals.map(_.kind).toSet
  def hasLineProof: Boolean =
    signalKinds.exists(kind =>
      kind == TacticalMechanismSignalKind.LineConsequence ||
        kind == TacticalMechanismSignalKind.Relation ||
        kind == TacticalMechanismSignalKind.MateBranch
    )
  def hasThreatProof: Boolean =
    signalKinds.contains(TacticalMechanismSignalKind.ThreatEpisode)
  def hasConcreteProof: Boolean =
    signals.nonEmpty && (hasLineProof || hasThreatProof)
  def hasEngineOrForcingProof: Boolean =
    signalKinds.exists(kind =>
      kind == TacticalMechanismSignalKind.MateBranch ||
        kind == TacticalMechanismSignalKind.LineConsequence ||
        kind == TacticalMechanismSignalKind.ThreatEpisode
    )
  def tactical: Boolean =
    kind != TacticalMechanismKind.DefensiveResource
  def defensive: Boolean =
    kind == TacticalMechanismKind.DefensiveResource
  def canAnchorTacticalIdea: Boolean =
    tactical && hasConcreteProof
  def canAnchorDefensiveIdea: Boolean =
    defensive && hasThreatProof

final case class StructuralDeltaEvidence(
    transition: StructuralTransitionBinding,
    signals: List[StructuralSignal],
    consequences: List[TransitionConsequence],
    developmentChoices: List[StructuralDevelopmentChoice] = Nil
) extends EvidencePayload:
  val layer: EvidenceLayer = EvidenceLayer.StructuralDelta
  import StructuralSignalKind.*
  import TransitionConsequenceKind.*

  def moveUci: String = transition.moveUci
  def role: TransitionEdgeRole = transition.role
  def from: PositionNodeRef = transition.from
  def to: PositionNodeRef = transition.to
  def line: Option[LineNodeRef] = transition.line
  def perspective: Color = transition.perspective
  def hasSignals: Boolean = signals.nonEmpty
  def hasConsequences: Boolean = consequences.nonEmpty
  def hasTypedOutput: Boolean = hasSignals || hasConsequences
  def consequenceKinds: List[TransitionConsequenceKind] = consequences.map(_.kind).distinct
  def signalAnchors: List[String] = signals.map(_.anchorKey).distinct
  def consequenceAnchors: List[String] = consequences.map(_.anchorKey).distinct
  def signalsOf(kind: StructuralSignalKind): List[StructuralSignal] = signals.filter(_.kind == kind)
  def consequencesOf(kind: TransitionConsequenceKind): List[TransitionConsequence] = consequences.filter(_.kind == kind)
  def hasSignal(kind: StructuralSignalKind): Boolean = signals.exists(_.kind == kind)
  def hasConsequence(kind: TransitionConsequenceKind): Boolean = consequences.exists(_.kind == kind)
  def hasAnyConsequence(kinds: Set[TransitionConsequenceKind]): Boolean =
    consequences.exists(consequence => kinds.contains(consequence.kind))
  def hasConsequenceCategory(category: TransitionConsequenceCategory): Boolean =
    consequences.exists(consequence => StructuralDeltaEvidence.hasConsequenceCategory(consequence.kind, category))
  def positiveConsequences: List[TransitionConsequence] =
    consequences.filter(_.positive)
  def negativeConsequences: List[TransitionConsequence] =
    consequences.filter(_.negative)
  def meaningfulConsequences: List[TransitionConsequence] =
    consequences.filter(consequence =>
      consequence.strength > 0 &&
        consequence.polarity != StructuralSignalPolarity.Neutral
    )
  def hasMeaningfulConsequences: Boolean =
    meaningfulConsequences.nonEmpty
  def hasPawnStructureImprovement: Boolean =
    hasConsequenceCategory(TransitionConsequenceCategory.PawnStructure)
  def hasMeaningfulPawnStructureDelta: Boolean =
    hasPawnStructureImprovement
  def hasTargetPressureGain: Boolean =
    hasConsequence(TargetPressureGain)
  def hasTargetPressureRelease: Boolean =
    hasConsequence(TargetPressureRelease)
  def hasCenterControlGain: Boolean =
    hasConsequence(CenterControlGain)
  def hasDevelopmentActivation: Boolean =
    hasConsequenceCategory(TransitionConsequenceCategory.Development)
  def hasPieceActivityGain: Boolean =
    hasConsequenceCategory(TransitionConsequenceCategory.PieceActivity)
  def hasKingSafetyPressure: Boolean =
    hasConsequence(KingSafetyPressure)
  def hasPassedPawnProgress: Boolean =
    hasConsequence(PassedPawnProgress)
  def hasOutpostGain: Boolean =
    hasConsequence(OutpostGain)
  def hasRookLiftActivation: Boolean =
    hasConsequence(RookLiftActivation)
  def hasBatteryPressureGain: Boolean =
    hasConsequence(BatteryPressureGain)
  def hasKingRingPressureGain: Boolean =
    hasConsequence(KingRingPressureGain)
  def hasStrategicConcession: Boolean =
    strategicConcessions.nonEmpty
  def strategicConcessions: List[TransitionConsequence] =
    negativeConsequences.filter(consequence =>
      StructuralDeltaEvidence.hasConsequenceCategory(consequence.kind, TransitionConsequenceCategory.StrategicSupport)
    )
  def hasPawnStructureDelta: Boolean =
    hasConsequenceCategory(TransitionConsequenceCategory.PawnStructureDelta)
  def hasStructuralAnchor: Boolean =
    hasConsequenceCategory(TransitionConsequenceCategory.StructuralAnchor)
  def hasStrategicMoveDelta: Boolean =
    hasConsequenceCategory(TransitionConsequenceCategory.StrategicMove)
  def hasStrategicSupport: Boolean =
    hasConsequenceCategory(TransitionConsequenceCategory.StrategicSupport)
  def hasPositivePlanAnchor: Boolean =
    positiveConsequences.exists(consequence =>
      StructuralDeltaEvidence.hasConsequenceCategory(consequence.kind, TransitionConsequenceCategory.PlanAnchor)
    )
  def structuralImprovementScore: Int =
    positiveConsequences
      .filterNot(consequence => consequence.kind == KingSafetyPressure)
      .map(_.strength)
      .sum
  def structuralImprovementConsequenceKinds: List[TransitionConsequenceKind] =
    positiveConsequences
      .map(_.kind)
      .filter(StructuralDeltaEvidence.isStructuralAnchorConsequence)
      .distinct
  def createdTargetPressureSubjects: List[String] =
    subjectsOfSignal(TargetPressureCreated)
  def releasedTargetPressureSubjects: List[String] =
    subjectsOfSignal(TargetPressureReleased)

  private def subjectsOfSignal(kind: StructuralSignalKind): List[String] =
    signalsOf(kind).flatMap(_.subjects).distinct

object StructuralDeltaEvidence:
  import TransitionConsequenceKind.*
  import TransitionConsequenceCategory.*

  val pawnStructureImprovementConsequences: Set[TransitionConsequenceKind] =
    consequenceKindsFor(PawnStructure)

  val pawnStructureDeltaConsequences: Set[TransitionConsequenceKind] =
    consequenceKindsFor(PawnStructureDelta)

  val developmentActivationConsequences: Set[TransitionConsequenceKind] =
    consequenceKindsFor(Development)

  val pieceActivityGainConsequences: Set[TransitionConsequenceKind] =
    consequenceKindsFor(PieceActivity)

  def structuralImprovementConsequenceKinds(records: Iterable[EvidenceRecord]): List[TransitionConsequenceKind] =
    records.collect { case EvidenceRecord(_, payload: StructuralDeltaEvidence, _) =>
      payload.structuralImprovementConsequenceKinds
    }.flatten.toList.distinct.sortBy(_.toString)

  def unmatchedStructuralImprovementAxisRecords(
      referenceRecords: List[EvidenceRecord],
      candidateRecords: List[EvidenceRecord]
  ): List[EvidenceRecord] =
    val candidateAxes = structuralImprovementConsequenceKinds(candidateRecords).toSet
    referenceRecords.filter {
      case EvidenceRecord(_, payload: StructuralDeltaEvidence, _) =>
        payload.structuralImprovementConsequenceKinds.exists(axis => !candidateAxes.contains(axis))
      case _ =>
        false
    }

  def hasConsequenceCategory(kind: TransitionConsequenceKind, category: TransitionConsequenceCategory): Boolean =
    consequenceCategories.getOrElse(kind, Set.empty).contains(category)

  def isStructuralAnchorConsequence(kind: TransitionConsequenceKind): Boolean =
    hasConsequenceCategory(kind, StructuralAnchor)

  def isStrategicSupportConsequence(kind: TransitionConsequenceKind): Boolean =
    hasConsequenceCategory(kind, StrategicSupport)

  private def consequenceKindsFor(category: TransitionConsequenceCategory): Set[TransitionConsequenceKind] =
    consequenceCategories.collect { case (kind, categories) if categories.contains(category) => kind }.toSet

  private lazy val consequenceCategories: Map[TransitionConsequenceKind, Set[TransitionConsequenceCategory]] =
    Map(
      OpenFileGain -> Set(PawnStructure, PawnStructureDelta, StructuralAnchor, StrategicMove, StrategicSupport),
      SemiOpenFileGain -> Set(PawnStructure, PawnStructureDelta, StructuralAnchor, StrategicMove, StrategicSupport),
      FileOccupationGain -> Set(PawnStructure, PawnStructureDelta, PieceActivity, StructuralAnchor, StrategicMove, StrategicSupport),
      WeakPawnTargetCreated -> Set(PawnStructure, PawnStructureDelta, StructuralAnchor, StrategicMove, StrategicSupport),
      WeakSquareTargetCreated -> Set(PawnStructure, PawnStructureDelta, StructuralAnchor, StrategicMove, StrategicSupport),
      PawnTensionGain -> Set(PawnStructure, PawnStructureDelta, StructuralAnchor, StrategicMove, StrategicSupport),
      PawnTensionResolution -> Set(PawnStructureDelta),
      TargetPressureGain -> Set(TargetPressure, StructuralAnchor, StrategicMove, StrategicSupport, PlanAnchor),
      TargetPressureRelease -> Set(TargetPressure, StrategicSupport),
      CenterControlGain -> Set(CenterControl, OpeningCenterControl, StructuralAnchor, StrategicMove, StrategicSupport, PlanAnchor),
      CenterControlLoss -> Set(CenterControl, StrategicSupport),
      DevelopmentLagReduced -> Set(Development, StructuralAnchor, StrategicMove, StrategicSupport, PlanAnchor, OpeningDevelopment),
      DevelopmentPieceActivated -> Set(Development, StructuralAnchor, StrategicMove, StrategicSupport, PlanAnchor, OpeningDevelopment),
      DevelopmentMobilityGain -> Set(Development, StructuralAnchor, StrategicMove, StrategicSupport, PlanAnchor, OpeningDevelopment),
      DevelopmentCenterControlGain -> Set(Development, CenterControl, OpeningCenterControl, StructuralAnchor, StrategicMove, StrategicSupport, PlanAnchor),
      DevelopmentSafePlacement -> Set(Development, StructuralAnchor, StrategicMove, StrategicSupport, PlanAnchor, OpeningDevelopment),
      DevelopmentLagIncreased -> Set(Development, StrategicSupport),
      DevelopmentPieceRetreated -> Set(Development, StrategicSupport),
      DevelopmentMobilityLoss -> Set(Development, StrategicSupport),
      DevelopmentCenterControlLoss -> Set(Development, CenterControl, StrategicSupport),
      DevelopmentUnsafePlacement -> Set(Development, StrategicSupport),
      MobilityGain -> Set(PieceActivity, StructuralAnchor, StrategicMove, StrategicSupport, PlanAnchor, OpeningDevelopment),
      MobilityLoss -> Set(PieceActivity, StrategicSupport),
      LineUnlockGain -> Set(PieceActivity, StructuralAnchor, StrategicMove, StrategicSupport, PlanAnchor),
      FileAccessGain -> Set(StructuralAnchor, StrategicMove, StrategicSupport, PlanAnchor),
      FileAccessLoss -> Set(StrategicSupport),
      KingSafetyPressure -> Set(StrategicMove, StrategicSupport, PlanAnchor),
      KingSafetyConcession -> Set(StrategicSupport),
      PassedPawnProgress -> Set(StructuralAnchor, StrategicMove, StrategicSupport),
      PassedPawnConcession -> Set(StrategicSupport),
      PromotionPressureGain -> Set(StructuralAnchor, StrategicMove, StrategicSupport),
      PromotionPressureConcession -> Set(StrategicSupport),
      OutpostGain -> Set(StructuralAnchor, StrategicMove, StrategicSupport),
      OutpostConcession -> Set(StrategicSupport),
      RookLiftActivation -> Set(StructuralAnchor, StrategicMove, StrategicSupport),
      BatteryPressureGain -> Set(StructuralAnchor, StrategicMove, StrategicSupport),
      KingRingPressureGain -> Set(StructuralAnchor, StrategicMove, StrategicSupport),
      KingRingPressureConcession -> Set(StrategicSupport)
    )

final case class PlanTransitionEvidence(
    transition: PlanSequenceSummary
) extends EvidencePayload:
  val layer: EvidenceLayer = EvidenceLayer.PlanTransition

final case class PlanPressureEvidence(
    scoring: PlanScoringResult,
    activePlans: ActivePlans
) extends EvidencePayload:
  val layer: EvidenceLayer = EvidenceLayer.PlanPressure

final case class CandidateComparisonEvidence(
    comparison: CandidateComparisonFact
) extends EvidencePayload:
  val layer: EvidenceLayer = EvidenceLayer.CandidateComparison

final case class CounterfactualFactEvidence(
    referenceLine: LineNodeRef,
    candidateLine: LineNodeRef,
    comparison: EvalComparison
) extends EvidencePayload:
  val layer: EvidenceLayer = EvidenceLayer.Counterfactual

final case class RelativeAssessmentEvidence(
    assessment: RelativeMoveAssessment
) extends EvidencePayload:
  val layer: EvidenceLayer = EvidenceLayer.RelativeAssessment

final case class RelativeCauseFactEvidence(
    cause: RelativeCauseFact
) extends EvidencePayload:
  val layer: EvidenceLayer = EvidenceLayer.RelativeCause

final case class MoveVerdictCertificationEvidence(
    certification: MoveVerdictCertification
) extends EvidencePayload:
  val layer: EvidenceLayer = EvidenceLayer.MoveVerdictCertification

final case class ChessIdeaEvidence(
    idea: ChessIdeaRef
) extends EvidencePayload:
  val layer: EvidenceLayer = EvidenceLayer.ChessIdea

final case class ClaimEvidence(
    claimId: String
) extends EvidencePayload:
  val layer: EvidenceLayer = EvidenceLayer.Claim

final case class EvidenceRecord(
    ref: EvidenceRef,
    payload: EvidencePayload,
    parents: List[EvidenceRef] = Nil
):
  def payloadLineRefs: List[LineNodeRef] =
    payload match
      case lineFact: LineFactEvidence =>
        List(lineFact.line)
      case EvalFactEvidence(payloadLine, _, _, _) =>
        List(payloadLine)
      case CandidateComparisonEvidence(fact) =>
        List(fact.referenceLine, fact.candidateLine)
      case CounterfactualFactEvidence(referenceLine, candidateLine, _) =>
        List(referenceLine, candidateLine)
      case RelativeAssessmentEvidence(assessment) =>
        List(assessment.reference.ref, assessment.candidate.ref)
      case RelativeCauseFactEvidence(cause) =>
        List(cause.eventLine)
      case payload: TacticalMechanismEvidence =>
        payload.line.toList
      case MoveVerdictCertificationEvidence(certification) =>
        certification.causes.map(_.eventLine).distinct
      case _ =>
        Nil
  def referencesLine(line: LineNodeRef): Boolean =
    ref.line.contains(line) || payloadLineRefs.contains(line)
  def carriesLinePayload(line: LineNodeRef, layer: EvidenceLayer): Boolean =
    ref.layer == layer && ref.line.contains(line) && payloadLineRefs.contains(line)
  def hasConcreteLineSignal: Boolean =
    payload match
      case payload: LineFactEvidence =>
        payload.hasConcreteLineConsequence
      case EvalFactEvidence(_, _, mate, _) =>
        mate.nonEmpty
      case payload: RelationFactEvidence =>
        payload.hasLineProof
      case payload: TacticalMechanismEvidence =>
        payload.hasLineProof
      case _ =>
        false
  def hasRootCaptureEvent(rootMove: String): Boolean =
    payload match
      case payload: LineFactEvidence =>
        payload.hasRootCaptureEvent(rootMove)
      case _ =>
        false

object EvidenceRecord:
  def hasConcreteLineSignal(records: List[EvidenceRecord]): Boolean =
    records.exists(_.hasConcreteLineSignal)

  def rootCaptureRecords(records: List[EvidenceRecord], rootMove: String): List[EvidenceRecord] =
    records.filter(_.hasRootCaptureEvent(rootMove))

  def hasRootCaptureEvent(records: List[EvidenceRecord], rootMove: String): Boolean =
    rootCaptureRecords(records, rootMove).nonEmpty

final case class TypedEvidenceGraph(
    records: List[EvidenceRecord]
):
  lazy val byId: Map[String, EvidenceRecord] =
    records.map(record => record.ref.id -> record).toMap

  def refs(layer: EvidenceLayer): List[EvidenceRef] =
    records.collect { case record if record.payload.layer == layer => record.ref }

  def recordsFor(position: PositionNodeRef): List[EvidenceRecord] =
    records.filter(_.ref.position == position)

  def recordsFor(line: LineNodeRef): List[EvidenceRecord] =
    records.filter(_.ref.line.contains(line))

  def add(record: EvidenceRecord): TypedEvidenceGraph =
    copy(records = records.filterNot(_.ref.id == record.ref.id) :+ record)

object TypedEvidenceGraph:
  val empty: TypedEvidenceGraph = TypedEvidenceGraph(Nil)

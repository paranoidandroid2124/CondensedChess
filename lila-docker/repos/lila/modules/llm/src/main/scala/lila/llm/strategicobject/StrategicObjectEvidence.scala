package lila.llm.strategicobject

import chess.{ Color, File, Role, Square }

enum ObjectSector:
  case Queenside
  case Center
  case Kingside
  case Mixed
  case WholeBoard

enum ObjectHorizonClass:
  case Structural
  case Operational
  case Maneuver
  case Promotion

enum StrategicStrengthBand:
  case Latent
  case Emerging
  case Established
  case Dominant

final case class StrategicObjectStateStrength(
    band: StrategicStrengthBand,
    coverage: Int,
    supportBalance: Int,
    pressureBalance: Int
)

final case class StrategicRouteGeometry(
    origin: Square,
    via: List[Square] = Nil,
    target: Square
):
  def normalized: StrategicRouteGeometry =
    copy(via = via.distinct.sortBy(_.key))

  def allSquares: List[Square] =
    (List(origin, target) ++ via).distinct.sortBy(_.key)

final case class StrategicObjectLocus(
    squares: List[Square] = Nil,
    files: List[File] = Nil,
    route: Option[StrategicRouteGeometry] = None
):
  def normalized: StrategicObjectLocus =
    copy(
      squares = squares.distinct.sortBy(_.key),
      files = files.distinct.sortBy(_.char.toString),
      route = route.map(_.normalized)
    )

  def allSquares: List[Square] =
    (squares ++ route.toList.flatMap(_.allSquares)).distinct.sortBy(_.key)

enum StrategicAnchorKind:
  case Square
  case File
  case Piece
  case Route

enum StrategicAnchorRole:
  case Primary
  case Secondary
  case Support
  case Pressure
  case Constraint
  case Entry
  case Exit

final case class StrategicPieceRef(
    owner: Color,
    squares: List[Square] = Nil,
    roles: Set[Role] = Set.empty
):
  def normalized: StrategicPieceRef =
    copy(squares = squares.distinct.sortBy(_.key))

final case class StrategicObjectAnchor(
    kind: StrategicAnchorKind,
    role: StrategicAnchorRole,
    squares: List[Square] = Nil,
    file: Option[File] = None,
    piece: Option[StrategicPieceRef] = None,
    route: Option[StrategicRouteGeometry] = None
):
  def normalized: StrategicObjectAnchor =
    copy(
      squares = squares.distinct.sortBy(_.key),
      piece = piece.map(_.normalized),
      route = route.map(_.normalized)
    )

enum PrimitiveKind:
  case TargetSquare
  case BreakCandidate
  case RouteContestSeed
  case ExchangeSquare
  case AccessRoute
  case DefendedResource
  case PieceRoleIssue
  case CriticalSquare
  case PasserSeed
  case DiagonalLaneSeed
  case LiftCorridorSeed
  case KnightRouteSeed
  case RedeploymentPathSeed
  case LeverContactSeed
  case HookContactSeed
  case CounterplayResourceSeed
  case TensionContactSeed
  case ReleaseCandidate

enum PrimitiveEvidenceFlag:
  case Fixed
  case ProtectedByPawn
  case Maintainable
  case Flank
  case OpenLane

final case class PrimitiveQuantitativeEvidence(
    attackerCount: Option[Int] = None,
    defenderCount: Option[Int] = None,
    supportCount: Option[Int] = None,
    resistanceCount: Option[Int] = None,
    pressureCount: Option[Int] = None,
    relativeRank: Option[Int] = None,
    mobilityGain: Option[Int] = None
)

final case class PrimitiveReference(
    kind: PrimitiveKind,
    owner: Color,
    anchorSquares: List[Square],
    contestedSquares: List[Square] = Nil,
    lane: Option[File] = None,
    roles: Set[Role] = Set.empty,
    targetOwner: Option[Color] = None,
    breakMode: Option[BreakMode] = None,
    issueKind: Option[PieceRoleIssueKind] = None,
    criticalKind: Option[CriticalSquareKind] = None,
    releaseKind: Option[ReleaseCandidateKind] = None,
    route: Option[StrategicRouteGeometry] = None,
    quantitative: PrimitiveQuantitativeEvidence = PrimitiveQuantitativeEvidence(),
    flags: Set[PrimitiveEvidenceFlag] = Set.empty
):
  def normalized: PrimitiveReference =
    copy(
      anchorSquares = anchorSquares.distinct.sortBy(_.key),
      contestedSquares = contestedSquares.distinct.sortBy(_.key),
      route = route.map(_.normalized)
    )

  def allSquares: List[Square] =
    (anchorSquares ++ contestedSquares ++ route.toList.flatMap(_.allSquares)).distinct.sortBy(_.key)

object PrimitiveReference:

  def fromPrimitive(primitive: Primitive): PrimitiveReference =
    primitive match
      case p: TargetSquare =>
        PrimitiveReference(
          kind = PrimitiveKind.TargetSquare,
          owner = p.owner,
          anchorSquares = List(p.square),
          contestedSquares = List(p.square),
          roles = p.occupant.toSet,
          targetOwner = Some(p.targetOwner),
          quantitative =
            PrimitiveQuantitativeEvidence(
              attackerCount = Some(p.attackerCount),
              defenderCount = Some(p.defenderCount)
            ),
          flags = Option.when(p.fixed)(PrimitiveEvidenceFlag.Fixed).toSet
        )
      case p: BreakCandidate =>
        PrimitiveReference(
          kind = PrimitiveKind.BreakCandidate,
          owner = p.owner,
          anchorSquares = p.sourceSquare :: p.targetSquares,
          contestedSquares = List(p.breakSquare),
          lane = Some(p.file),
          roles = Set(chess.Pawn),
          breakMode = Some(p.mode),
          route = Some(StrategicRouteGeometry(origin = p.sourceSquare, target = p.breakSquare)),
          quantitative =
            PrimitiveQuantitativeEvidence(
              supportCount = Some(p.supportCount),
              resistanceCount = Some(p.resistanceCount)
            )
        )
      case p: RouteContestSeed =>
        PrimitiveReference(
          kind = PrimitiveKind.RouteContestSeed,
          owner = p.owner,
          anchorSquares = p.square :: p.carrierSquares,
          contestedSquares = List(p.square),
          lane = Some(p.lane),
          roles = p.supportingRoles,
          quantitative =
            PrimitiveQuantitativeEvidence(
              attackerCount = Some(p.attackerCount),
              defenderCount = Some(p.defenderCount)
            )
        )
      case p: ExchangeSquare =>
        PrimitiveReference(
          kind = PrimitiveKind.ExchangeSquare,
          owner = p.owner,
          anchorSquares = List(p.square),
          contestedSquares = List(p.square),
          roles = Set(p.occupant),
          targetOwner = Some(p.targetOwner),
          quantitative =
            PrimitiveQuantitativeEvidence(
              attackerCount = Some(p.attackerCount),
              defenderCount = Some(p.defenderCount)
            )
        )
      case p: AccessRoute =>
        PrimitiveReference(
          kind = PrimitiveKind.AccessRoute,
          owner = p.owner,
          anchorSquares = p.carrierSquares,
          lane = Some(p.file),
          roles = p.roles,
          flags = Set(PrimitiveEvidenceFlag.OpenLane)
        )
      case p: DefendedResource =>
        PrimitiveReference(
          kind = PrimitiveKind.DefendedResource,
          owner = p.owner,
          anchorSquares = List(p.square),
          contestedSquares = List(p.square),
          roles = Set(p.role),
          quantitative =
            PrimitiveQuantitativeEvidence(
              attackerCount = Some(p.attackerCount),
              defenderCount = Some(p.defenderCount)
            )
        )
      case p: PieceRoleIssue =>
        PrimitiveReference(
          kind = PrimitiveKind.PieceRoleIssue,
          owner = p.owner,
          anchorSquares = List(p.square),
          roles = Set(p.role),
          issueKind = Some(p.issue)
        )
      case p: CriticalSquare =>
        PrimitiveReference(
          kind = PrimitiveKind.CriticalSquare,
          owner = p.owner,
          anchorSquares = List(p.square),
          contestedSquares = List(p.square),
          criticalKind = Some(p.kind),
          quantitative = PrimitiveQuantitativeEvidence(pressureCount = Some(p.pressure))
        )
      case p: PasserSeed =>
        PrimitiveReference(
          kind = PrimitiveKind.PasserSeed,
          owner = p.owner,
          anchorSquares = List(p.square),
          roles = Set(chess.Pawn),
          quantitative = PrimitiveQuantitativeEvidence(relativeRank = Some(p.relativeRank)),
          flags = Option.when(p.protectedByPawn)(PrimitiveEvidenceFlag.ProtectedByPawn).toSet
        )
      case p: DiagonalLaneSeed =>
        PrimitiveReference(
          kind = PrimitiveKind.DiagonalLaneSeed,
          owner = p.owner,
          anchorSquares = List(p.origin, p.target),
          contestedSquares = List(p.target),
          roles = Set(p.role),
          route = Some(StrategicRouteGeometry(origin = p.origin, target = p.target)),
          quantitative =
            PrimitiveQuantitativeEvidence(
              attackerCount = Some(p.attackerCount),
              defenderCount = Some(p.defenderCount)
            )
        )
      case p: LiftCorridorSeed =>
        PrimitiveReference(
          kind = PrimitiveKind.LiftCorridorSeed,
          owner = p.owner,
          anchorSquares = List(p.origin, p.liftSquare, p.target),
          contestedSquares = List(p.target),
          roles = Set(chess.Rook),
          route = Some(StrategicRouteGeometry(origin = p.origin, via = List(p.liftSquare), target = p.target)),
          quantitative =
            PrimitiveQuantitativeEvidence(
              attackerCount = Some(p.attackerCount),
              defenderCount = Some(p.defenderCount)
            )
        )
      case p: KnightRouteSeed =>
        PrimitiveReference(
          kind = PrimitiveKind.KnightRouteSeed,
          owner = p.owner,
          anchorSquares = List(p.origin, p.via, p.target),
          contestedSquares = List(p.target),
          roles = Set(chess.Knight),
          route = Some(StrategicRouteGeometry(origin = p.origin, via = List(p.via), target = p.target)),
          quantitative =
            PrimitiveQuantitativeEvidence(
              attackerCount = Some(p.attackerCount),
              defenderCount = Some(p.defenderCount)
            )
        )
      case p: RedeploymentPathSeed =>
        PrimitiveReference(
          kind = PrimitiveKind.RedeploymentPathSeed,
          owner = p.owner,
          anchorSquares = List(p.origin, p.via, p.target),
          contestedSquares = List(p.target),
          roles = Set(p.role),
          route = Some(StrategicRouteGeometry(origin = p.origin, via = List(p.via), target = p.target)),
          quantitative = PrimitiveQuantitativeEvidence(mobilityGain = Some(p.mobilityGain))
        )
      case p: LeverContactSeed =>
        PrimitiveReference(
          kind = PrimitiveKind.LeverContactSeed,
          owner = p.owner,
          anchorSquares = List(p.from, p.target),
          contestedSquares = List(p.target),
          roles = Set(chess.Pawn),
          route = Some(StrategicRouteGeometry(origin = p.from, target = p.target)),
          quantitative =
            PrimitiveQuantitativeEvidence(
              supportCount = Some(p.supportCount),
              resistanceCount = Some(p.resistanceCount)
            ),
          flags = Option.when(p.flank)(PrimitiveEvidenceFlag.Flank).toSet
        )
      case p: HookContactSeed =>
        PrimitiveReference(
          kind = PrimitiveKind.HookContactSeed,
          owner = p.owner,
          anchorSquares = List(p.from, p.createSquare, p.target),
          contestedSquares = List(p.target),
          roles = Set(chess.Pawn),
          route = Some(StrategicRouteGeometry(origin = p.from, via = List(p.createSquare), target = p.target)),
          quantitative =
            PrimitiveQuantitativeEvidence(
              supportCount = Some(p.supportCount),
              resistanceCount = Some(p.resistanceCount)
            ),
          flags = Set(PrimitiveEvidenceFlag.Flank)
        )
      case p: CounterplayResourceSeed =>
        PrimitiveReference(
          kind = PrimitiveKind.CounterplayResourceSeed,
          owner = p.owner,
          anchorSquares = p.square :: p.pressureSquares,
          contestedSquares = p.pressureSquares,
          roles = Set(p.role),
          quantitative =
            PrimitiveQuantitativeEvidence(
              attackerCount = Some(p.attackerCount),
              supportCount = Some(p.supportCount),
              pressureCount = Some(p.pressureSquares.size)
            )
        )
      case p: TensionContactSeed =>
        PrimitiveReference(
          kind = PrimitiveKind.TensionContactSeed,
          owner = p.owner,
          anchorSquares = List(p.from, p.target),
          contestedSquares = List(p.target),
          roles = Set(chess.Pawn),
          route = Some(StrategicRouteGeometry(origin = p.from, target = p.target)),
          quantitative =
            PrimitiveQuantitativeEvidence(
              supportCount = Some(p.supportCount),
              resistanceCount = Some(p.resistanceCount)
            ),
          flags =
            Option.when(p.maintainable)(PrimitiveEvidenceFlag.Maintainable).toSet ++
              Option.when(p.flank)(PrimitiveEvidenceFlag.Flank).toSet
        )
      case p: ReleaseCandidate =>
        PrimitiveReference(
          kind = PrimitiveKind.ReleaseCandidate,
          owner = p.owner,
          anchorSquares = List(p.from, p.target),
          contestedSquares = List(p.target),
          roles = Set(chess.Pawn),
          route = Some(StrategicRouteGeometry(origin = p.from, target = p.target)),
          releaseKind = Some(p.kind),
          quantitative =
            PrimitiveQuantitativeEvidence(
              supportCount = Some(p.supportCount),
              resistanceCount = Some(p.resistanceCount)
            )
        )

enum RivalReferenceKind:
  case Primitive
  case Piece
  case Object

final case class StrategicRivalReference(
    kind: RivalReferenceKind,
    owner: Color,
    squares: List[Square] = Nil,
    file: Option[File] = None,
    roles: Set[Role] = Set.empty,
    primitiveKind: Option[PrimitiveKind] = None,
    objectId: Option[String] = None,
    objectFamily: Option[StrategicObjectFamily] = None
):
  def normalized: StrategicRivalReference =
    copy(squares = squares.distinct.sortBy(_.key))

enum StrategicRelationOperator:
  case Enables
  case Denies
  case Fixes
  case Preserves
  case TransformsTo
  case RacesWith
  case DependsOn
  case OverloadsOrUndermines

final case class StrategicRelationTarget(
    objectId: String,
    family: StrategicObjectFamily,
    owner: Color
)

final case class StrategicRelation(
    operator: StrategicRelationOperator,
    target: StrategicRelationTarget
)

final case class StrategicObjectEvidenceFootprint(
    primitiveKinds: Set[PrimitiveKind],
    primitiveCount: Int,
    anchorSquares: List[Square],
    contestedSquares: List[Square],
    lanes: List[File],
    supportingPieceCount: Int,
    rivalCount: Int,
    supportBalance: Int,
    pressureBalance: Int,
    mobilityGain: Int,
    tags: Set[StrategicEvidenceTag] = Set.empty
):
  def normalized: StrategicObjectEvidenceFootprint =
    copy(
      anchorSquares = anchorSquares.distinct.sortBy(_.key),
      contestedSquares = contestedSquares.distinct.sortBy(_.key),
      lanes = lanes.distinct.sortBy(_.char.toString)
    )

enum StrategicEvidenceTag:
  case Supported
  case Fixed
  case Protected
  case Central
  case Flank
  case ShellPressure
  case Exposed
  case OpenFile
  case DevelopmentLead
  case DevelopmentLag
  case Trapped
  case Restricted
  case Contested
  case Promotion
  case MobilityGain
  case RouteAccess

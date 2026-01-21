package lila.llm.model.authoring

import chess.{ Color, File, Role, Square }
import lila.llm.model.PlanId

/**
 * Seed Library SSOT: PV/MultiPV 밖의 "저자 관점 아이디어 공간"을 구조화한다.
 *
 * - LatentSeed: 정적 정의(카탈로그 항목)
 * - LatentPlanInfo: 포지션별 후보(AuthorQuestion payload로 사용)
 *
 * NOTE:
 * - 텍스트 근거로는 illegal/null-move를 쓰지 않는다.
 * - "tempo injection"은 내부 랭킹 힌트로만 허용하며, 출력 증거는 합법 branch로만 구성한다.
 */

enum SeedFamily:
  case Pawn, Piece, Structure, Prophylaxis, Exchange, TacticalPrep

enum SideRef:
  case Us, Them

enum Flank:
  case Kingside, Queenside, Center

enum CenterState:
  case Open, Locked, Fluid

enum FileStatus:
  case Open, SemiOpen, Closed

enum StructureType:
  case CarlsbadLike
  case IQP
  case HangingPawns
  case FianchettoShell
  case MaroczyBindLike

enum MovePattern:
  /** Any quiet pawn advance on the given file (1- or 2-step). */
  case PawnAdvance(file: File)
  /** Any pawn capture from file -> file (lever). */
  case PawnLever(from: File, to: File)
  /** A piece move to a specific square (used for outposts, etc). */
  case PieceTo(role: Role, square: Square)
  /** A specific maneuver via intermediate squares. */
  case PieceManeuver(role: Role, target: Square, via: List[Square] = Nil)
  /** Castling (either side). */
  case Castle
  /** Exchange a specific piece type. */
  case Exchange(role: Role, on: Square)
  /** Form a battery (e.g. Alekhine's Gun). */
  case BatteryFormation(front: Role, back: Role, line: LineType)

enum LineType:
  case FileLine(file: File)
  case RankLine(rank: chess.Rank) // Explicitly import or qualify if needed, standard import helps
  case DiagonalLine

enum Precondition:
  case KingPosition(owner: SideRef, flank: Flank)
  case CenterStateIs(state: CenterState)
  case SpaceAdvantage(flank: Flank, min: Int)
  case FileStatusIs(file: File, status: FileStatus)
  case PieceRouteExists(role: Role, from: Square, to: Square)
  case PawnStructureIs(tpe: StructureType)
  case NoImmediateDefensiveTask(maxThreatCp: Int = 150)

case class WeightedPrecondition(
    condition: Precondition,
    required: Boolean = true,
    weight: Double = 1.0
)

case class EvidencePolicy(
    // Internal hint only: allow tempo-injected scoring to rank seeds (never cite lines).
    useTempoInjection: Boolean = true,
    tempoInjectionDepth: Int = 20,
    // Legal proof: require >=N legal quiet branches where the seed shows up quickly as best reply.
    freeTempoVerify: Option[FreeTempoVerify] = Some(FreeTempoVerify()),
    // Safety: is seed playable immediately? (if not, enforce "premature/refuted" wording)
    immediateViability: Option[ImmediateViability] = Some(ImmediateViability()),
    // Explicit refutation check: force the system to look for the main countermeasure lines.
    refutationCheck: Option[RefutationCheck] = Some(RefutationCheck())
)

case class FreeTempoVerify(
    minBranches: Int = 2, // must have >=2 a)/b) legal branches
    maxBranches: Int = 4,
    seedMustAppearWithinPlies: Int = 1,
    deepProbeDepth: Int = 20,
    replyMultiPv: Int = 2
)

case class ImmediateViability(
    maxMoverLossCp: Int = 50,
    deepProbeDepth: Int = 20,
    replyMultiPv: Int = 3
)

case class RefutationCheck(
    counters: List[CounterPattern] = Nil,
    deepProbeDepth: Int = 20,
    replyMultiPv: Int = 3
)

enum CounterPattern:
  case PawnPushBlock(file: File)
  case PieceControl(role: Role, square: Square)
  case CentralStrike
  case Counterplay(flank: Flank)

case class NarrativeTemplate(
    template: String,
    mustBeConditional: Boolean = true
)

case class LatentSeed(
    id: String, // stable: "PawnStorm_Kingside"
    family: SeedFamily,
    mapsToPlan: Option[PlanId] = None,
    // What this seed tries to "start" (used for matching engine replies).
    candidateMoves: List[MovePattern],
    preconditions: List[WeightedPrecondition] = Nil,
    typicalCounters: List[CounterPattern] = Nil,
    evidencePolicy: EvidencePolicy = EvidencePolicy(),
    narrative: NarrativeTemplate
)

case class LatentPlanInfo(
    seedId: String,
    seedFamily: SeedFamily,
    mapsToPlan: Option[PlanId] = None,
    candidateMoves: List[MovePattern] = Nil,
    typicalCounters: List[CounterPattern] = Nil,
    evidencePolicy: EvidencePolicy = EvidencePolicy(),
    narrative: NarrativeTemplate = NarrativeTemplate(template = "")
)


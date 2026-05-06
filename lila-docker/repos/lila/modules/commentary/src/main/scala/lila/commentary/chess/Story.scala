package lila.commentary.chess

enum Scene:
  case Tactic
  case Blunder
  case Material
  case King
  case Defense
  case Opening
  case Pawns
  case Plan
  case Pieces
  case Space
  case Initiative
  case Convert
  case Endgame
  case Counterplay
  case Source
  case Quiet

enum Plan:
  case Minority
  case Majority
  case CenterBreak
  case FlankBreak
  case Storm
  case Expansion
  case Cramp
  case Outpost
  case BadPiece
  case Reroute
  case Bishops
  case Blockade
  case OpenFile
  case Seventh
  case ColorBind
  case WeakSquare
  case Isolani
  case BackwardPawn
  case HangingPawns
  case ChainBase
  case PasserMake
  case PasserBlock
  case Race
  case Trade
  case Simplify
  case KeepPieces
  case Overload
  case Prophy
  case Counterplay
  case Initiative
  case KingConvert
  case Convert

enum Tactic:
  case Loose
  case Hanging
  case AbsPin
  case RelPin
  case Skewer
  case Xray
  case Fork
  case Discover
  case RemoveGuard
  case Overload
  case BackRank
  case MateNet
  case SafeCheck
  case PawnFork
  case PawnPush
  case Trap
  case QueenHit
  case KingOpen
  case Promote
  case InBetween
  case Clear
  case Decoy
  case Deflect
  case Tempo

final case class Proof(
    boardProof: Int,
    lineProof: Int,
    ownerProof: Int,
    anchorProof: Int,
    routeProof: Int,
    persistence: Int,
    immediacy: Int,
    forcing: Int,
    conversionPrize: Int,
    counterplayRisk: Int,
    kingHeat: Int,
    pieceSupport: Int,
    pawnSupport: Int,
    sourceFit: Int,
    novelty: Int,
    clarity: Int
):
  import Proof.inRange

  require(
    Vector(
      boardProof,
      lineProof,
      ownerProof,
      anchorProof,
      routeProof,
      persistence,
      immediacy,
      forcing,
      conversionPrize,
      counterplayRisk,
      kingHeat,
      pieceSupport,
      pawnSupport,
      sourceFit,
      novelty,
      clarity
    )
      .forall(inRange),
    "Proof scores must be 0..100"
  )

  val values: Vector[Int] =
    Vector(
      boardProof,
      lineProof,
      ownerProof,
      anchorProof,
      routeProof,
      persistence,
      immediacy,
      forcing,
      conversionPrize,
      counterplayRisk,
      kingHeat,
      pieceSupport,
      pawnSupport,
      sourceFit,
      novelty,
      clarity
    )

  val truth: Int = List(boardProof, lineProof, ownerProof, anchorProof, routeProof).min

  val tacticHeat: Double =
    0.30 * forcing +
      0.25 * conversionPrize +
      0.20 * kingHeat +
      0.15 * lineProof +
      0.10 * immediacy

  val planHeat: Double =
    0.28 * persistence +
      0.20 * routeProof +
      0.16 * conversionPrize +
      0.14 * pieceSupport +
      0.12 * pawnSupport +
      0.10 * clarity -
      0.22 * counterplayRisk

  val publicStrength: Double = math.min(truth.toDouble, math.max(tacticHeat, planHeat))

object Proof:
  val Size = 16

  object Slots:
    val BoardProof = 0
    val LineProof = 1
    val OwnerProof = 2
    val AnchorProof = 3
    val RouteProof = 4
    val Persistence = 5
    val Immediacy = 6
    val Forcing = 7
    val ConversionPrize = 8
    val CounterplayRisk = 9
    val KingHeat = 10
    val PieceSupport = 11
    val PawnSupport = 12
    val SourceFit = 13
    val Novelty = 14
    val Clarity = 15

  private def inRange(score: Int) = score >= 0 && score <= 100

enum Side:
  case White
  case Black
  case Both
  case None

final class StoryProof private (legalLine: Option[Line], sameBoardProof: Boolean):
  def failures(story: Story): Vector[BoardFacts.MissingEvidence] =
    val missing = Vector(
      Option.when(!StoryProof.playingSide(story.side))("side"),
      Option.when(story.target.isEmpty)("target"),
      Option.when(story.anchor.isEmpty)("anchor"),
      Option.when(story.route.isEmpty)("route"),
      Option.when(!StoryProof.rivalSide(story.side, story.rival))("rival"),
      Option.when(!StoryProof.legalLineBindsRoute(story.route, legalLine))("legal line"),
      Option.when(!sameBoardProof)("same-board proof")
    ).flatten
    if missing.isEmpty then Vector.empty
    else Vector(BoardFacts.MissingEvidence("Story Proof", missing))

object StoryProof:
  val empty: StoryProof = StoryProof(legalLine = None, sameBoardProof = false)

  private[commentary] def fromBoardFacts(facts: BoardFacts, legalLine: Line): StoryProof =
    val sameBoard = BoardFacts.sameBoardReady(facts)
    val legal = facts.sideLegal.lines.contains(legalLine) || facts.rivalLegal.lines.contains(legalLine)
    StoryProof(
      legalLine = Option.when(sameBoard && legal)(legalLine),
      sameBoardProof = sameBoard
    )

  private[commentary] def untrustedLegalLine(legalLine: Line): StoryProof =
    StoryProof(legalLine = Some(legalLine), sameBoardProof = false)

  private def playingSide(side: Side): Boolean =
    side == Side.White || side == Side.Black

  private def rivalSide(side: Side, rival: Side): Boolean =
    playingSide(rival) && playingSide(side) && side != rival

  private def legalLineBindsRoute(route: Option[Line], legalLine: Option[Line]): Boolean =
    route.zip(legalLine).exists((routeLine, legal) => routeLine == legal)

private[commentary] enum StoryWriter:
  case TacticHanging

final case class Story(
    scene: Scene,
    plan: Option[Plan] = None,
    tactic: Option[Tactic] = None,
    proof: Proof,
    side: Side = Side.None,
    target: Option[Square] = None,
    anchor: Option[Square] = None,
    route: Option[Line] = None,
    rival: Side = Side.None,
    storyProof: StoryProof = StoryProof.empty,
    private[commentary] val writer: Option[StoryWriter] = None,
    private[commentary] val captureResult: Option[CaptureResult] = None,
    private[commentary] val engineCheck: Option[EngineCheck] = None
):
  def proofFailures: Vector[BoardFacts.MissingEvidence] =
    storyProof.failures(this)

  def values: Vector[Int] =
    val data = Array.fill(Story.Size)(0)

    data(Story.Slots.Scene + scene.ordinal) = 1
    plan.foreach(p => data(Story.Slots.Plan + p.ordinal) = 1)
    tactic.foreach(t => data(Story.Slots.Tactic + t.ordinal) = 1)

    data(Story.Slots.Pawn + Story.Identity.Side) = side.ordinal
    data(Story.Slots.Pawn + Story.Identity.Rival) = rival.ordinal
    data(Story.Slots.Pawn + Story.Identity.Target) = squareValue(target)
    data(Story.Slots.Pawn + Story.Identity.Anchor) = squareValue(anchor)
    data(Story.Slots.Pawn + Story.Identity.RouteFrom) = route.fold(0)(line => line.from.index + 1)
    data(Story.Slots.Pawn + Story.Identity.RouteTo) = route.fold(0)(line => line.to.index + 1)

    proof.values.zipWithIndex.foreach: (value, index) =>
      data(Story.Slots.Proof + index) = value

    data.toVector

  private def squareValue(square: Option[Square]) = square.fold(0)(_.index + 1)

object Story:
  val Size = 160
  val SceneSlots = 16
  val PlanSlots = 32
  val TacticSlots = 24
  val PawnSlots = 16
  val PieceSlots = 16
  val KingSlots = 16
  val OpeningSlots = 8
  val ProofSlots = 32

  object Slots:
    val Scene = 0
    val Plan = Scene + SceneSlots
    val Tactic = Plan + PlanSlots
    val Pawn = Tactic + TacticSlots
    val Piece = Pawn + PawnSlots
    val King = Piece + PieceSlots
    val Opening = King + KingSlots
    val Proof = Opening + OpeningSlots
    val End = Proof + ProofSlots

  object Identity:
    val Side = 0
    val Rival = 1
    val Target = 2
    val Anchor = 3
    val RouteFrom = 4
    val RouteTo = 5

enum Role:
  case Lead
  case Support
  case Context
  case Blocked

final case class Verdict(
    story: Story,
    rank: Int,
    leadAllowed: Boolean,
    strength: Double,
    role: Role,
    // Internal diagnostics only. Verdict.values, renderer, and LLM inputs must not consume this.
    proofFailures: Vector[BoardFacts.MissingEvidence] = Vector.empty,
    engineCheckStatus: Option[EngineCheckStatus] = None,
    engineStrengthLimited: Boolean = false
):
  def values: Vector[Double] =
    val data = Array.fill(Verdict.Size)(0.0)

    data(Verdict.Slots.Role) = role.ordinal.toDouble
    data(Verdict.Slots.Rank) = rank.toDouble
    data(Verdict.Slots.LeadAllowed) = if leadAllowed then 1.0 else 0.0
    data(Verdict.Slots.Strength) = strength
    data(Verdict.Slots.Side) = story.side.ordinal.toDouble
    data(Verdict.Slots.Rival) = story.rival.ordinal.toDouble
    data(Verdict.Slots.Target) = story.target.fold(0.0)(square => (square.index + 1).toDouble)
    data(Verdict.Slots.Anchor) = story.anchor.fold(0.0)(square => (square.index + 1).toDouble)

    data(Verdict.Slots.Scene + story.scene.ordinal) = 1.0
    story.plan.foreach(plan => data(Verdict.Slots.Plan + plan.ordinal) = 1.0)
    story.tactic.foreach(tactic => data(Verdict.Slots.Tactic + tactic.ordinal) = 1.0)

    story.proof.values.zipWithIndex.foreach: (value, index) =>
      data(Verdict.Slots.Proof + index) = value.toDouble

    data.toVector

object Verdict:
  val Size = 96
  val FinalSlots = 8
  val SceneSlots = 16
  val PlanSlots = 32
  val TacticSlots = 24
  val ProofSlots = 16

  object Slots:
    val Role = 0
    val Rank = 1
    val LeadAllowed = 2
    val Strength = 3
    val Side = 4
    val Rival = 5
    val Target = 6
    val Anchor = 7
    val Scene = FinalSlots
    val Plan = Scene + SceneSlots
    val Tactic = Plan + PlanSlots
    val Proof = Tactic + TacticSlots
    val End = Proof + ProofSlots

object StoryTable:
  val TopK = 8
  // Public Story leads require same-root proof sidecars from named positive Story writers.
  // Stage 3 opens only Tactic.Hanging.
  val PublicStoryLeadsRequireNamedProofWriters = true

  def choose(stories: Vector[Story]): Vector[Verdict] =
    val rows =
      stories.map: story =>
        val leadCandidate = leadByStoryRules(story, stories)
        Row(story, story.proof.publicStrength, leadCandidate, leadCandidate, story.proofFailures)
    rows
      .sortBy(row =>
        (
          leadSortPriority(row),
          -row.strength,
          row.story.scene.ordinal,
          tag(row.story),
          row.story.side.ordinal,
          squareKey(row.story.target),
          squareKey(row.story.anchor),
          routeKey(row.story.route),
          row.story.rival.ordinal
        )
      )
      .take(TopK)
      .zipWithIndex
      .map: (row, index) =>
        Verdict(
          story = row.story,
          rank = index + 1,
          leadAllowed = row.leadAllowed,
          strength = row.strength,
          role = role(row, index),
          proofFailures = row.proofFailures,
          engineCheckStatus = row.story.engineCheck.map(_.status),
          engineStrengthLimited = row.story.engineCheck.exists(_.status == EngineCheckStatus.Caps)
        )

  private case class Row(
      story: Story,
      strength: Double,
      leadAllowed: Boolean,
      leadCandidate: Boolean,
      proofFailures: Vector[BoardFacts.MissingEvidence]
  )

  private def leadSortPriority(row: Row) =
    if row.leadAllowed then 0 else if row.leadCandidate then 1 else 2

  private def leadByStoryRules(story: Story, stories: Vector[Story]) =
    story.proofFailures.isEmpty &&
      positiveWriter(story) &&
      base(story) &&
      identity(story) &&
      fit(story) &&
      quiet(story, stories) &&
      plan(story, stories) &&
      source(story, stories)

  private def base(story: Story) =
    story.proof.publicStrength >= 65 &&
      story.proof.truth >= 70 &&
      story.proof.counterplayRisk <= 70

  private def identity(story: Story) =
    (story.proof.ownerProof < 70 || story.side != Side.None) &&
      (story.proof.anchorProof < 70 || story.anchor.nonEmpty) &&
      (story.proof.routeProof < 70 || story.route.nonEmpty) &&
      (story.scene != Scene.Tactic || story.tactic.nonEmpty) &&
      (story.scene != Scene.Tactic || story.proof.lineProof > 0)

  private def fit(story: Story) =
    story.tactic.isEmpty || story.scene == Scene.Tactic

  private def positiveWriter(story: Story) =
    story.writer.contains(StoryWriter.TacticHanging) &&
      story.scene == Scene.Tactic &&
      story.tactic.contains(Tactic.Hanging) &&
      story.plan.isEmpty &&
      !story.engineCheck.exists(_.status == EngineCheckStatus.Refutes) &&
      story.captureResult.exists: result =>
        result.positiveMaterial &&
          result.sameBoardProof &&
          result.missingEvidence.isEmpty &&
          captureResultBindsStory(story, result) &&
          result.targetPiece.exists(piece => piece.man != Man.Pawn && piece.man != Man.King)

  private def captureResultBindsStory(story: Story, result: CaptureResult) =
    result.side == story.side &&
      story.route.contains(result.captureLine) &&
      result.capturingPiece.exists(piece => story.anchor.contains(piece.square)) &&
      result.targetPiece.exists(piece => story.target.contains(piece.square))

  private def quiet(story: Story, stories: Vector[Story]) =
    story.scene != Scene.Quiet || stories.filterNot(_ == story).forall(_.proof.publicStrength < 55)

  private def plan(story: Story, stories: Vector[Story]) =
    story.scene != Scene.Plan || stories
      .filterNot(_ == story)
      .forall: other =>
        val blocks =
          opposing(story, other) &&
            (other.scene == Scene.Tactic || other.scene == Scene.Blunder) &&
            other.proof.publicStrength >= 70
        val outranks =
          opposing(story, other) &&
            other.scene == Scene.Tactic &&
            other.proof.tacticHeat >= 70 &&
            other.proof.lineProof >= 65 &&
            base(other)
        !blocks && !outranks

  private def source(story: Story, stories: Vector[Story]) =
    story.scene != Scene.Source || stories
      .filterNot(_ == story)
      .forall: other =>
        other.scene == Scene.Source || other.proof.boardProof <= 0 || other.proof.publicStrength < 55

  private def opposing(story: Story, other: Story) =
    (story.side, other.side) match
      case (Side.White, Side.Black) => true
      case (Side.Black, Side.White) => true
      case (Side.White, Side.Both)  => true
      case (Side.Black, Side.Both)  => true
      case (Side.Both, Side.White)  => true
      case (Side.Both, Side.Black)  => true
      case _                        => false

  private def tag(story: Story) =
    story.plan.map(_.ordinal).orElse(story.tactic.map(_.ordinal)).getOrElse(Int.MaxValue)

  private def squareKey(square: Option[Square]) = square.fold(0)(_.index + 1)

  private def routeKey(route: Option[Line]) =
    route.fold(0)(line => (line.from.index + 1) * 65 + line.to.index + 1)

  private def role(row: Row, index: Int) =
    if row.leadAllowed && index == 0 then Role.Lead
    else if row.leadAllowed then Role.Support
    else if base(row.story) then Role.Blocked
    else Role.Context

package lila.commentary.chess

enum Scene:
  case Tactic
  case Blunder
  case Material
  case King
  case Defense
  case Opening
  case Pawns
  case PawnAdvance
  case PawnStop
  case PawnBreak
  case PawnBlock
  case Plan
  case Pieces
  case Space
  case Initiative
  case Convert
  case Endgame
  case Counterplay
  case Source
  case Quiet
  case PromotionThreat
  case Promotion
  case PawnCapture
  case PassedPawnCreated
  case FileOpened
  case CheckGiven
  case CheckEscaped
  case Checkmate
  case Stalemate

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
  case Pin
  case Skewer
  case Xray
  case Fork
  case DiscoveredAttack
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
  case Interference
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
  case TacticFork
  case SceneMaterial
  case SceneDefense
  case TacticDiscoveredAttack
  case TacticPin
  case TacticRemoveGuard
  case TacticOverload
  case TacticDeflect
  case TacticDecoy
  case TacticInterference
  case TacticSkewer
  case TacticQueenHit
  case TacticLoose
  case TacticTrap
  case ScenePawnAdvance
  case ScenePawnStop
  case ScenePawnBreak
  case ScenePawnBlock
  case ScenePromotionThreat
  case ScenePromotion
  case ScenePawnCapture
  case ScenePassedPawnCreated
  case SceneFileOpened
  case SceneCheckGiven
  case SceneCheckEscaped
  case SceneCheckmate
  case SceneStalemate

final case class Story(
    scene: Scene,
    plan: Option[Plan] = None,
    tactic: Option[Tactic] = None,
    proof: Proof,
    side: Side = Side.None,
    target: Option[Square] = None,
    anchor: Option[Square] = None,
    route: Option[Line] = None,
    routeSan: Option[String] = None,
    rival: Side = Side.None,
    storyProof: StoryProof = StoryProof.empty,
    private[commentary] val writer: Option[StoryWriter] = None,
    private[commentary] val captureResult: Option[CaptureResult] = None,
    private[commentary] val engineCheck: Option[EngineCheck] = None,
    secondaryTarget: Option[Square] = None,
    private[commentary] val multiTargetProof: Option[MultiTargetProof] = None,
    private[commentary] val threatProof: Option[ThreatProof] = None,
    private[commentary] val defenseProof: Option[DefenseProof] = None,
    private[commentary] val lineProof: Option[LineProof] = None,
    private[commentary] val pinProof: Option[PinProof] = None,
    private[commentary] val removeGuardProof: Option[RemoveGuardProof] = None,
    private[commentary] val overloadProof: Option[OverloadProof] = None,
    private[commentary] val deflectProof: Option[DeflectProof] = None,
    private[commentary] val decoyProof: Option[DecoyProof] = None,
    private[commentary] val interferenceProof: Option[InterferenceProof] = None,
    private[commentary] val skewerProof: Option[SkewerProof] = None,
    private[commentary] val queenHitProof: Option[QueenHitProof] = None,
    private[commentary] val loosePieceProof: Option[LoosePieceProof] = None,
    private[commentary] val trapProof: Option[TrapProof] = None,
    private[commentary] val pawnAdvanceProof: Option[PawnAdvanceProof] = None,
    private[commentary] val pawnStopProof: Option[PawnStopProof] = None,
    private[commentary] val pawnBreakProof: Option[PawnBreakProof] = None,
    private[commentary] val pawnBlockProof: Option[PawnBlockProof] = None,
    private[commentary] val promotionThreatProof: Option[PromotionThreatProof] = None,
    private[commentary] val promotionProof: Option[PromotionProof] = None,
    private[commentary] val pawnCaptureProof: Option[PawnCaptureProof] = None,
    private[commentary] val passedPawnCreatedProof: Option[PassedPawnCreatedProof] = None,
    private[commentary] val openedFile: Option[Int] = None,
    private[commentary] val fileOpenedProof: Option[FileOpenedProof] = None,
    private[commentary] val checkGivenProof: Option[CheckGivenProof] = None,
    private[commentary] val checkEscapedProof: Option[CheckEscapedProof] = None,
    private[commentary] val checkmateProof: Option[CheckmateProof] = None,
    private[commentary] val stalemateProof: Option[StalemateProof] = None
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
    data(Story.Slots.Piece + Story.PieceIdentity.SecondaryTarget) = squareValue(secondaryTarget)

    proof.values.zipWithIndex.foreach: (value, index) =>
      data(Story.Slots.Proof + index) = value

    data.toVector

  private def squareValue(square: Option[Square]) = square.fold(0)(_.index + 1)

object Story:
  val Size = 175
  val SceneSlots = 29
  val PlanSlots = 32
  val TacticSlots = 26
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

  object PieceIdentity:
    val SecondaryTarget = 0

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
    // Internal diagnostics only. Verdict.values, renderer, and LLM inputs must not consume these.
    proofFailures: Vector[BoardFacts.MissingEvidence] = Vector.empty,
    engineCheckStatus: Option[EngineCheckStatus] = None,
    engineStrengthLimited: Boolean = false,
    selected: Boolean = false
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

  private[commentary] def proofDeficitDiagnostic: Option[ProofDeficitDiagnostic] =
    ProofDeficitDiagnostics.fromVerdict(this)

object Verdict:
  val Size = 111
  val FinalSlots = 8
  val SceneSlots = 29
  val PlanSlots = 32
  val TacticSlots = 26
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
  // Positive writers open one narrow proof-backed tactic at a time.
  val PublicStoryLeadsRequireNamedProofWriters = true

  def choose(stories: Vector[Story]): Vector[Verdict] =
    val admittedStories = stories.filterNot(closedTacticRow)
    val rows =
      admittedStories.map: story =>
        val leadCandidate = leadByStoryRules(story, admittedStories)
        val proofFailures = story.proofFailures
        Row(
          story,
          story.proof.publicStrength,
          leadCandidate,
          blockedByStoryRules(story, proofFailures),
          proofFailures
        )
    rows
      .sortBy(row =>
        (
          roleSortPriority(row),
          interactionPriority(row.story, stories),
          -row.strength,
          familyKey(row.story),
          row.story.side.ordinal,
          squareKey(row.story.target),
          squarePairKey(row.story.secondaryTarget, row.story.anchor),
          routeKey(row.story.route),
          row.story.rival.ordinal
        )
      )
      .take(TopK)
      .zipWithIndex
      .map: (row, index) =>
        val selectedRole = role(row, index)
        Verdict(
          story = row.story,
          rank = index + 1,
          leadAllowed = selectedRole == Role.Lead,
          strength = row.strength,
          role = selectedRole,
          proofFailures = row.proofFailures,
          engineCheckStatus = row.story.engineCheck.map(_.status),
          engineStrengthLimited = row.story.engineCheck.exists(_.status == EngineCheckStatus.Caps),
          selected = selectedRole != Role.Blocked || !stalemateRelated(row.story)
        )

  private val ClosedTactics =
    Set(Tactic.PawnFork, Tactic.Trap, Tactic.Decoy, Tactic.Tempo)

  private def closedTacticRow(story: Story) =
    story.tactic.exists(ClosedTactics.contains) &&
      !story.writer.exists(writer => writer == StoryWriter.TacticTrap || writer == StoryWriter.TacticDecoy)

  private case class Row(
      story: Story,
      strength: Double,
      leadCandidate: Boolean,
      blocked: Boolean,
      proofFailures: Vector[BoardFacts.MissingEvidence]
  )

  private def stalemateRelated(story: Story) =
    story.scene == Scene.Stalemate ||
      story.writer.contains(StoryWriter.SceneStalemate) ||
      story.stalemateProof.nonEmpty

  private def roleSortPriority(row: Row) =
    val roleKey = if row.leadCandidate then 0 else if row.blocked then 2 else 1
    roleKey * 8 + engineCapPriority(row.story) * 4 + engineStatusKey(row.story)

  private def interactionPriority(story: Story, stories: Vector[Story]) =
    if promotionYieldsToOpenedClaimHome(story, stories) then 1
    else if promotionThreatYieldsToOpenedClaimHome(story, stories) then 1
    else if pawnBreakYieldsToOpenedClaimHome(story, stories) then 1
    else if pawnStopYieldsToOpenedClaimHome(story, stories) then 1
    else if pawnStopYieldsToSamePawnAdvance(story, stories) then 1
    else if pawnAdvanceYieldsToOpenedClaimHome(story, stories) then 1
    else if pawnAdvanceYieldsToPromotionThreat(story, stories) then 1
    else if pawnCaptureYieldsToOpenedClaimHome(story, stories) then 1
    else if passedPawnCreatedYieldsToOpenedClaimHome(story, stories) then 1
    else if fileOpenedYieldsToOpenedClaimHome(story, stories) then 1
    else if pawnBlockYieldsToOpenedClaimHome(story, stories) then 1
    else if materialOverlapsHanging(story, stories) then 1
    else if defenseCollidesWithMaterial(story, stories) then 1
    else if defenseOverlapsImmediateMaterialGain(story, stories) then 1
    else if lineDefenderOverlapsOpenedClaimHome(story, stories) then 1
    else if discoveredAttackOverlapsDefenderContact(story, stories) then 1
    else if removeGuardOverlapsMaterial(story, stories) then 1
    else if removeGuardOverlapsHanging(story, stories) then 1
    else if queenHitYieldsToOpenedClaimHome(story, stories) then 1
    else if looseYieldsToOpenedClaimHome(story, stories) then 1
    else 0

  private def looseYieldsToOpenedClaimHome(story: Story, stories: Vector[Story]) =
    positiveLooseWriter(story) &&
      stories.exists: other =>
        other != story &&
          (
            positiveMaterialWriter(other) ||
              positiveHangingWriter(other) ||
              positiveQueenHitWriter(other) ||
              positiveForkWriter(other) ||
              positiveSkewerWriter(other) ||
              positivePinWriter(other) ||
              positiveRemoveGuardWriter(other) ||
              positiveOverloadWriter(other) ||
              positiveDiscoveredAttackWriter(other) ||
              positiveDefenseWriter(other)
          )

  private def queenHitYieldsToOpenedClaimHome(story: Story, stories: Vector[Story]) =
    positiveQueenHitWriter(story) &&
      stories.exists: other =>
        other != story &&
          (
            positiveMaterialWriter(other) ||
              positiveHangingWriter(other) ||
              positiveForkWriter(other) ||
              positiveSkewerWriter(other) ||
              positivePinWriter(other) ||
              positiveRemoveGuardWriter(other) ||
              positiveOverloadWriter(other) ||
              positiveDiscoveredAttackWriter(other) ||
              positiveDefenseWriter(other)
          )

  private def promotionYieldsToOpenedClaimHome(story: Story, stories: Vector[Story]) =
    positivePromotionWriter(story) &&
      stories.exists: other =>
        other != story &&
          (
            positiveHangingWriter(other) ||
              positiveForkWriter(other) ||
              positiveMaterialWriter(other) ||
              positiveDefenseWriter(other) ||
              positiveLineDefenderWriter(other)
          )

  private def engineCapPriority(story: Story) =
    if story.engineCheck.exists(_.status == EngineCheckStatus.Caps) then 1 else 0

  private def engineStatusKey(story: Story) =
    story.engineCheck.fold(0)(_.status.ordinal + 1)

  private def pawnAdvanceYieldsToOpenedClaimHome(story: Story, stories: Vector[Story]) =
    positivePawnAdvanceWriter(story) &&
      stories.exists: other =>
        other != story &&
          (
            positiveHangingWriter(other) ||
              positiveForkWriter(other) ||
              positiveMaterialWriter(other) ||
              positiveDefenseWriter(other) ||
              positiveLineDefenderWriter(other)
          )

  private def pawnBreakYieldsToOpenedClaimHome(story: Story, stories: Vector[Story]) =
    positivePawnBreakWriter(story) &&
      stories.exists: other =>
        other != story &&
          (
            positiveHangingWriter(other) ||
              positiveForkWriter(other) ||
              positiveMaterialWriter(other) ||
              positiveDefenseWriter(other) ||
              positiveLineDefenderWriter(other) ||
              positivePawnAdvanceWriter(other) ||
              positivePawnStopWriter(other) ||
              positivePromotionThreatWriter(other) ||
              positivePromotionWriter(other)
          )

  private def pawnCaptureYieldsToOpenedClaimHome(story: Story, stories: Vector[Story]) =
    positivePawnCaptureWriter(story) &&
      stories.exists: other =>
        other != story &&
          (
            positiveHangingWriter(other) ||
              positiveForkWriter(other) ||
              positiveMaterialWriter(other) ||
              positiveDefenseWriter(other) ||
              positiveLineDefenderWriter(other) ||
              positivePawnAdvanceWriter(other) ||
              positivePawnStopWriter(other) ||
              positivePawnBreakWriter(other) ||
              positivePromotionThreatWriter(other) ||
              positivePromotionWriter(other)
          )

  private def passedPawnCreatedYieldsToOpenedClaimHome(story: Story, stories: Vector[Story]) =
    positivePassedPawnCreatedWriter(story) &&
      stories.exists: other =>
        other != story &&
          (
            positiveHangingWriter(other) ||
              positiveForkWriter(other) ||
              positiveMaterialWriter(other) ||
              positiveDefenseWriter(other) ||
              positiveLineDefenderWriter(other) ||
              positivePawnAdvanceWriter(other) ||
              positivePawnStopWriter(other) ||
              positivePawnBreakWriter(other) ||
              positivePawnCaptureWriter(other) ||
              positivePromotionThreatWriter(other) ||
              positivePromotionWriter(other)
          )

  private def fileOpenedYieldsToOpenedClaimHome(story: Story, stories: Vector[Story]) =
    positiveFileOpenedWriter(story) &&
      stories.exists: other =>
        other != story &&
          (
            positiveHangingWriter(other) ||
              positiveForkWriter(other) ||
              positiveMaterialWriter(other) ||
              positiveDefenseWriter(other) ||
              positiveLineDefenderWriter(other) ||
              positivePawnAdvanceWriter(other) ||
              positivePawnStopWriter(other) ||
              positivePawnBreakWriter(other) ||
              positivePawnCaptureWriter(other) ||
              positivePassedPawnCreatedWriter(other) ||
              positivePromotionThreatWriter(other) ||
              positivePromotionWriter(other)
          )

  private def pawnBlockYieldsToOpenedClaimHome(story: Story, stories: Vector[Story]) =
    positivePawnBlockWriter(story) &&
      stories.exists: other =>
        other != story &&
          (
            positiveHangingWriter(other) ||
              positiveForkWriter(other) ||
              positiveMaterialWriter(other) ||
              positiveDefenseWriter(other) ||
              positiveLineDefenderWriter(other) ||
              positivePawnAdvanceWriter(other) ||
              positivePawnStopWriter(other) ||
              positivePawnBreakWriter(other) ||
              positivePawnCaptureWriter(other) ||
              positivePassedPawnCreatedWriter(other) ||
              positiveFileOpenedWriter(other) ||
              positivePromotionThreatWriter(other) ||
              positivePromotionWriter(other)
          )

  private def pawnAdvanceYieldsToPromotionThreat(story: Story, stories: Vector[Story]) =
    positivePawnAdvanceWriter(story) &&
      stories.exists: other =>
        other != story &&
          positivePromotionThreatWriter(other) &&
          sameSideRoute(story, other)

  private def promotionThreatYieldsToOpenedClaimHome(story: Story, stories: Vector[Story]) =
    positivePromotionThreatWriter(story) &&
      stories.exists: other =>
        other != story &&
          (
            positiveHangingWriter(other) ||
              positiveForkWriter(other) ||
              positiveMaterialWriter(other) ||
              positiveDefenseWriter(other) ||
              positiveLineDefenderWriter(other)
          )

  private def pawnStopYieldsToOpenedClaimHome(story: Story, stories: Vector[Story]) =
    positivePawnStopWriter(story) &&
      stories.exists: other =>
        other != story &&
          (
            positiveHangingWriter(other) ||
              positiveForkWriter(other) ||
              positiveMaterialWriter(other) ||
              positiveDefenseWriter(other) ||
              positiveLineDefenderWriter(other)
          )

  private def pawnStopYieldsToSamePawnAdvance(story: Story, stories: Vector[Story]) =
    positivePawnStopWriter(story) &&
      stories.exists: other =>
        other != story &&
          positivePawnAdvanceWriter(other) &&
          samePassedPawnAdvanceSquare(story, other)

  private def samePassedPawnAdvanceSquare(stop: Story, advance: Story) =
    (
      for
        stopProof <- stop.pawnStopProof
        advanceProof <- advance.pawnAdvanceProof
        stoppedPawn <- stopProof.targetPawn
        advancedPawnBefore <- advanceProof.pawnBefore
        advancedPawnAfter <- advanceProof.pawnAfter
        stoppedNextSquare <- stopProof.nextAdvanceSquare
      yield stopProof.side == advance.rival &&
        stopProof.rivalSide == advance.side &&
        stoppedPawn.square == advancedPawnBefore.square &&
        stoppedNextSquare == advancedPawnAfter.square
    ).getOrElse(false)

  private def defenseCollidesWithMaterial(story: Story, stories: Vector[Story]) =
    positiveDefenseWriter(story) &&
      stories.exists(other => other != story && positiveMaterialWriter(other))

  private def materialOverlapsHanging(story: Story, stories: Vector[Story]) =
    positiveMaterialWriter(story) &&
      stories.exists: other =>
        other != story &&
          positiveHangingWriter(other) &&
          sameCaptureMaterialResult(story, other)

  private def removeGuardOverlapsMaterial(story: Story, stories: Vector[Story]) =
    positiveRemoveGuardWriter(story) &&
      stories.exists: other =>
        other != story &&
          positiveMaterialWriter(other) &&
          sameSideRoute(story, other)

  private def removeGuardOverlapsHanging(story: Story, stories: Vector[Story]) =
    positiveRemoveGuardWriter(story) &&
      stories.exists: other =>
        other != story &&
          positiveHangingWriter(other) &&
          sameSideRoute(story, other)

  private def lineDefenderOverlapsOpenedClaimHome(story: Story, stories: Vector[Story]) =
    positiveLineDefenderWriter(story) &&
      stories.exists: other =>
        other != story &&
          sameSideRoute(story, other) &&
          (
            positiveHangingWriter(other) ||
              positiveForkWriter(other) ||
              positiveMaterialWriter(other) ||
              positiveDefenseWriter(other)
          )

  private def discoveredAttackOverlapsDefenderContact(story: Story, stories: Vector[Story]) =
    positiveDiscoveredAttackWriter(story) &&
      stories.exists: other =>
        other != story &&
          sameSideRoute(story, other) &&
          story.target == other.target &&
          (positivePinWriter(other) || positiveRemoveGuardWriter(other))

  private def positiveLineDefenderWriter(story: Story) =
    positiveDiscoveredAttackWriter(story) ||
      positivePinWriter(story) ||
      positiveRemoveGuardWriter(story) ||
      positiveDeflectWriter(story) ||
      positiveInterferenceWriter(story) ||
      positiveSkewerWriter(story)

  private def sameCaptureMaterialResult(story: Story, other: Story) =
    story.side == other.side &&
      story.route == other.route &&
      story.target == other.target &&
      story.captureResult.flatMap(_.materialResult) == other.captureResult.flatMap(_.materialResult)

  private def sameSideRoute(story: Story, other: Story) =
    story.side == other.side &&
      story.route.nonEmpty &&
      story.route == other.route

  private def defenseOverlapsImmediateMaterialGain(story: Story, stories: Vector[Story]) =
    positiveDefenseWriter(story) &&
      story.defenseProof.flatMap(_.afterDefenseTargetStatus).contains(DefenseTargetStatus.AttackerCaptured) &&
      stories.exists: other =>
        other != story &&
          positiveMaterialWriter(other) &&
          sameRouteCapturesDefenseAttacker(story, other)

  private def sameRouteCapturesDefenseAttacker(defense: Story, material: Story) =
    defense.side == material.side &&
      defense.route == material.route &&
      defense.threatProof.flatMap(_.attackingPiece).map(_.square) ==
      material.captureResult.flatMap(_.targetPiece).map(_.square)

  private def leadByStoryRules(story: Story, stories: Vector[Story]) =
    story.proofFailures.isEmpty &&
      !interferenceProofContaminatesOtherRow(story) &&
      positiveWriter(story) &&
      base(story) &&
      identity(story) &&
      fit(story) &&
      quiet(story, stories) &&
      plan(story, stories) &&
      source(story, stories)

  private def blockedByStoryRules(story: Story, proofFailures: Vector[BoardFacts.MissingEvidence]) =
    proofFailures.nonEmpty ||
      story.engineCheck.exists(_.status == EngineCheckStatus.Refutes) ||
      hangingWithoutWriter(story) ||
      hangingWriterWithoutCapture(story) ||
      forkWithoutWriter(story) ||
      pinWithoutWriter(story) ||
      removeGuardWithoutWriter(story) ||
      overloadWithoutWriter(story) ||
      deflectWithoutWriter(story) ||
      interferenceWithoutWriter(story) ||
      skewerWithoutWriter(story) ||
      queenHitWithoutWriter(story) ||
      looseWithoutWriter(story) ||
      trapWithoutWriter(story) ||
      discoveredAttackWithoutWriter(story) ||
      invalidForkWriter(story) ||
      invalidMaterialWriter(story) ||
      invalidDefenseWriter(story) ||
      invalidDiscoveredAttackWriter(story) ||
      invalidPinWriter(story) ||
      invalidRemoveGuardWriter(story) ||
      invalidOverloadWriter(story) ||
      invalidDeflectWriter(story) ||
      invalidDecoyWriter(story) ||
      invalidInterferenceWriter(story) ||
      invalidSkewerWriter(story) ||
      invalidQueenHitWriter(story) ||
      invalidLooseWriter(story) ||
      invalidTrapWriter(story) ||
      invalidPawnAdvanceWriter(story) ||
      invalidPawnStopWriter(story) ||
      invalidPawnBreakWriter(story) ||
      invalidPawnBlockWriter(story) ||
      invalidPawnCaptureWriter(story) ||
      invalidPassedPawnCreatedWriter(story) ||
      invalidFileOpenedWriter(story) ||
      invalidPromotionThreatWriter(story) ||
      invalidPromotionWriter(story) ||
      invalidCheckGivenWriter(story) ||
      invalidCheckEscapedWriter(story) ||
      invalidCheckmateWriter(story) ||
      invalidStalemateWriter(story) ||
      checkGivenProofContaminatesOtherRow(story) ||
      checkEscapedProofContaminatesOtherRow(story) ||
      checkmateProofContaminatesOtherRow(story) ||
      stalemateProofContaminatesOtherRow(story) ||
      queenHitProofContaminatesOtherRow(story) ||
      loosePieceProofContaminatesOtherRow(story) ||
      removeGuardProofContaminatesOtherRow(story) ||
      trapProofContaminatesOtherRow(story) ||
      overloadProofContaminatesOtherRow(story) ||
      deflectProofContaminatesOtherRow(story) ||
      decoyProofContaminatesOtherRow(story) ||
      interferenceProofContaminatesOtherRow(story) ||
      pawnAdvanceWithoutWriter(story) ||
      pawnStopWithoutWriter(story) ||
      pawnBreakWithoutWriter(story) ||
      pawnBlockWithoutWriter(story) ||
      pawnCaptureWithoutWriter(story) ||
      passedPawnCreatedWithoutWriter(story) ||
      fileOpenedWithoutWriter(story) ||
      promotionThreatWithoutWriter(story) ||
      promotionWithoutWriter(story) ||
      checkGivenWithoutWriter(story) ||
      checkEscapedWithoutWriter(story) ||
      checkmateWithoutWriter(story) ||
      stalemateWithoutWriter(story) ||
      (!leadByStoryRules(story, Vector(story)) && base(story))

  private def hangingWithoutWriter(story: Story) =
    story.writer.isEmpty &&
      story.scene == Scene.Tactic &&
      story.tactic.contains(Tactic.Hanging)

  private def hangingWriterWithoutCapture(story: Story) =
    story.writer.contains(StoryWriter.TacticHanging) &&
      story.scene == Scene.Tactic &&
      story.tactic.contains(Tactic.Hanging) &&
      story.captureResult.isEmpty

  private def forkWithoutWriter(story: Story) =
    story.writer.isEmpty &&
      story.scene == Scene.Tactic &&
      story.tactic.contains(Tactic.Fork)

  private def discoveredAttackWithoutWriter(story: Story) =
    story.writer.isEmpty &&
      story.scene == Scene.Tactic &&
      story.tactic.contains(Tactic.DiscoveredAttack)

  private def pinWithoutWriter(story: Story) =
    story.writer.isEmpty &&
      story.scene == Scene.Tactic &&
      story.tactic.contains(Tactic.Pin)

  private def removeGuardWithoutWriter(story: Story) =
    story.writer.isEmpty &&
      story.scene == Scene.Tactic &&
      story.tactic.contains(Tactic.RemoveGuard)

  private def overloadWithoutWriter(story: Story) =
    story.writer.isEmpty &&
      story.scene == Scene.Tactic &&
      story.tactic.contains(Tactic.Overload)

  private def deflectWithoutWriter(story: Story) =
    story.writer.isEmpty &&
      story.scene == Scene.Tactic &&
      story.tactic.contains(Tactic.Deflect)

  private def interferenceWithoutWriter(story: Story) =
    story.writer.isEmpty &&
      story.scene == Scene.Tactic &&
      story.tactic.contains(Tactic.Interference)

  private def skewerWithoutWriter(story: Story) =
    story.writer.isEmpty &&
      story.scene == Scene.Tactic &&
      story.tactic.contains(Tactic.Skewer)

  private def queenHitWithoutWriter(story: Story) =
    story.writer.isEmpty &&
      story.scene == Scene.Tactic &&
      story.tactic.contains(Tactic.QueenHit)

  private def looseWithoutWriter(story: Story) =
    story.writer.isEmpty &&
      story.scene == Scene.Tactic &&
      story.tactic.contains(Tactic.Loose)

  private def trapWithoutWriter(story: Story) =
    story.writer.isEmpty &&
      story.scene == Scene.Tactic &&
      story.tactic.contains(Tactic.Trap)

  private def pawnAdvanceWithoutWriter(story: Story) =
    story.writer.isEmpty &&
      story.scene == Scene.PawnAdvance

  private def pawnStopWithoutWriter(story: Story) =
    story.writer.isEmpty &&
      story.scene == Scene.PawnStop

  private def pawnBreakWithoutWriter(story: Story) =
    story.writer.isEmpty &&
      story.scene == Scene.PawnBreak

  private def pawnBlockWithoutWriter(story: Story) =
    story.writer.isEmpty &&
      story.scene == Scene.PawnBlock

  private def pawnCaptureWithoutWriter(story: Story) =
    story.writer.isEmpty &&
      story.scene == Scene.PawnCapture

  private def passedPawnCreatedWithoutWriter(story: Story) =
    story.writer.isEmpty &&
      story.scene == Scene.PassedPawnCreated

  private def fileOpenedWithoutWriter(story: Story) =
    story.writer.isEmpty &&
      story.scene == Scene.FileOpened

  private def promotionThreatWithoutWriter(story: Story) =
    story.writer.isEmpty &&
      story.scene == Scene.PromotionThreat

  private def promotionWithoutWriter(story: Story) =
    story.writer.isEmpty &&
      story.scene == Scene.Promotion

  private def checkGivenWithoutWriter(story: Story) =
    story.writer.isEmpty &&
      story.scene == Scene.CheckGiven

  private def checkEscapedWithoutWriter(story: Story) =
    story.writer.isEmpty &&
      story.scene == Scene.CheckEscaped

  private def checkmateWithoutWriter(story: Story) =
    story.writer.isEmpty &&
      story.scene == Scene.Checkmate

  private def stalemateWithoutWriter(story: Story) =
    story.writer.isEmpty &&
      story.scene == Scene.Stalemate

  private def invalidForkWriter(story: Story) =
    story.writer.contains(StoryWriter.TacticFork) &&
      !positiveForkWriter(story)

  private def invalidMaterialWriter(story: Story) =
    story.writer.contains(StoryWriter.SceneMaterial) &&
      !positiveMaterialWriter(story)

  private def invalidDefenseWriter(story: Story) =
    story.writer.contains(StoryWriter.SceneDefense) &&
      !positiveDefenseWriter(story)

  private def invalidDiscoveredAttackWriter(story: Story) =
    story.writer.contains(StoryWriter.TacticDiscoveredAttack) &&
      !positiveDiscoveredAttackWriter(story)

  private def invalidPinWriter(story: Story) =
    story.writer.contains(StoryWriter.TacticPin) &&
      !positivePinWriter(story)

  private def invalidRemoveGuardWriter(story: Story) =
    story.writer.contains(StoryWriter.TacticRemoveGuard) &&
      !positiveRemoveGuardWriter(story)

  private def invalidOverloadWriter(story: Story) =
    story.writer.contains(StoryWriter.TacticOverload) &&
      !positiveOverloadWriter(story)

  private def invalidDeflectWriter(story: Story) =
    story.writer.contains(StoryWriter.TacticDeflect) &&
      !positiveDeflectWriter(story)

  private def invalidDecoyWriter(story: Story) =
    story.writer.contains(StoryWriter.TacticDecoy) &&
      !positiveDecoyWriter(story)

  private def invalidInterferenceWriter(story: Story) =
    story.writer.contains(StoryWriter.TacticInterference) &&
      !positiveInterferenceWriter(story)

  private def invalidSkewerWriter(story: Story) =
    story.writer.contains(StoryWriter.TacticSkewer) &&
      !skewerWriterShape(story)

  private def invalidQueenHitWriter(story: Story) =
    story.writer.contains(StoryWriter.TacticQueenHit) &&
      !queenHitWriterShape(story)

  private def invalidLooseWriter(story: Story) =
    story.writer.contains(StoryWriter.TacticLoose) &&
      !looseWriterShape(story)

  private def invalidTrapWriter(story: Story) =
    story.writer.contains(StoryWriter.TacticTrap) &&
      !positiveTrapWriter(story)

  private def invalidPawnAdvanceWriter(story: Story) =
    story.writer.contains(StoryWriter.ScenePawnAdvance) &&
      !positivePawnAdvanceWriter(story)

  private def invalidPawnStopWriter(story: Story) =
    story.writer.contains(StoryWriter.ScenePawnStop) &&
      !positivePawnStopWriter(story)

  private def invalidPawnBreakWriter(story: Story) =
    story.writer.contains(StoryWriter.ScenePawnBreak) &&
      !positivePawnBreakWriter(story)

  private def invalidPawnBlockWriter(story: Story) =
    story.writer.contains(StoryWriter.ScenePawnBlock) &&
      !positivePawnBlockWriter(story)

  private def invalidPawnCaptureWriter(story: Story) =
    story.writer.contains(StoryWriter.ScenePawnCapture) &&
      !positivePawnCaptureWriter(story)

  private def invalidPassedPawnCreatedWriter(story: Story) =
    story.writer.contains(StoryWriter.ScenePassedPawnCreated) &&
      !positivePassedPawnCreatedWriter(story)

  private def invalidFileOpenedWriter(story: Story) =
    story.writer.contains(StoryWriter.SceneFileOpened) &&
      !positiveFileOpenedWriter(story)

  private def invalidPromotionThreatWriter(story: Story) =
    story.writer.contains(StoryWriter.ScenePromotionThreat) &&
      !positivePromotionThreatWriter(story)

  private def invalidPromotionWriter(story: Story) =
    story.writer.contains(StoryWriter.ScenePromotion) &&
      !positivePromotionWriter(story)

  private def invalidCheckGivenWriter(story: Story) =
    story.writer.contains(StoryWriter.SceneCheckGiven) &&
      !positiveCheckGivenWriter(story)

  private def invalidCheckEscapedWriter(story: Story) =
    story.writer.contains(StoryWriter.SceneCheckEscaped) &&
      !positiveCheckEscapedWriter(story)

  private def invalidCheckmateWriter(story: Story) =
    story.writer.contains(StoryWriter.SceneCheckmate) &&
      !positiveCheckmateWriter(story)

  private def invalidStalemateWriter(story: Story) =
    story.writer.contains(StoryWriter.SceneStalemate) &&
      !positiveStalemateWriter(story)

  private def checkGivenProofContaminatesOtherRow(story: Story) =
    story.checkGivenProof.nonEmpty &&
      !story.writer.contains(StoryWriter.SceneCheckGiven)

  private def checkEscapedProofContaminatesOtherRow(story: Story) =
    story.checkEscapedProof.nonEmpty &&
      !story.writer.contains(StoryWriter.SceneCheckEscaped)

  private def checkmateProofContaminatesOtherRow(story: Story) =
    story.checkmateProof.nonEmpty &&
      !story.writer.contains(StoryWriter.SceneCheckmate)

  private def stalemateProofContaminatesOtherRow(story: Story) =
    story.stalemateProof.nonEmpty &&
      !story.writer.contains(StoryWriter.SceneStalemate)

  private def queenHitProofContaminatesOtherRow(story: Story) =
    story.queenHitProof.nonEmpty &&
      !story.writer.contains(StoryWriter.TacticQueenHit)

  private def loosePieceProofContaminatesOtherRow(story: Story) =
    story.loosePieceProof.nonEmpty &&
      !story.writer.contains(StoryWriter.TacticLoose)

  private def removeGuardProofContaminatesOtherRow(story: Story) =
    story.removeGuardProof.nonEmpty &&
      !story.writer.contains(StoryWriter.TacticRemoveGuard)

  private def trapProofContaminatesOtherRow(story: Story) =
    story.trapProof.nonEmpty &&
      !story.writer.contains(StoryWriter.TacticTrap)

  private def overloadProofContaminatesOtherRow(story: Story) =
    story.overloadProof.nonEmpty &&
      !story.writer.contains(StoryWriter.TacticOverload)

  private def deflectProofContaminatesOtherRow(story: Story) =
    story.deflectProof.nonEmpty &&
      !story.writer.contains(StoryWriter.TacticDeflect)

  private def decoyProofContaminatesOtherRow(story: Story) =
    story.decoyProof.nonEmpty &&
      !story.writer.contains(StoryWriter.TacticDecoy)

  private def interferenceProofContaminatesOtherRow(story: Story) =
    story.interferenceProof.nonEmpty &&
      !story.writer.contains(StoryWriter.TacticInterference)

  private def base(story: Story) =
    story.proof.publicStrength >= 65 &&
      story.proof.truth >= 70 &&
      story.proof.counterplayRisk <= 70

  private def identity(story: Story) =
    (story.proof.ownerProof < 70 || story.side != Side.None) &&
      (story.proof.anchorProof < 70 || story.anchor.nonEmpty) &&
      (story.proof.routeProof < 70 || story.route.nonEmpty) &&
      (story.proof.routeProof < 70 || story.routeSan.nonEmpty) &&
      (story.scene != Scene.Tactic || story.tactic.nonEmpty) &&
      (story.scene != Scene.Tactic || story.proof.lineProof > 0)

  private def fit(story: Story) =
    story.tactic.isEmpty || story.scene == Scene.Tactic

  private def positiveWriter(story: Story) =
    story.writer match
      case Some(StoryWriter.TacticHanging) => positiveHangingWriter(story)
      case Some(StoryWriter.TacticFork) => positiveForkWriter(story)
      case Some(StoryWriter.SceneMaterial) => positiveMaterialWriter(story)
      case Some(StoryWriter.SceneDefense) => positiveDefenseWriter(story)
      case Some(StoryWriter.TacticDiscoveredAttack) => positiveDiscoveredAttackWriter(story)
      case Some(StoryWriter.TacticPin) => positivePinWriter(story)
      case Some(StoryWriter.TacticRemoveGuard) => positiveRemoveGuardWriter(story)
      case Some(StoryWriter.TacticOverload) => positiveOverloadWriter(story)
      case Some(StoryWriter.TacticDeflect) => positiveDeflectWriter(story)
      case Some(StoryWriter.TacticDecoy) => positiveDecoyWriter(story)
      case Some(StoryWriter.TacticInterference) => positiveInterferenceWriter(story)
      case Some(StoryWriter.TacticSkewer) => positiveSkewerWriter(story)
      case Some(StoryWriter.TacticQueenHit) => positiveQueenHitWriter(story)
      case Some(StoryWriter.TacticLoose) => positiveLooseWriter(story)
      case Some(StoryWriter.TacticTrap) => positiveTrapWriter(story)
      case Some(StoryWriter.ScenePawnAdvance) => positivePawnAdvanceWriter(story)
      case Some(StoryWriter.ScenePawnStop) => positivePawnStopWriter(story)
      case Some(StoryWriter.ScenePawnBreak) => positivePawnBreakWriter(story)
      case Some(StoryWriter.ScenePawnBlock) => positivePawnBlockWriter(story)
      case Some(StoryWriter.ScenePromotionThreat) => positivePromotionThreatWriter(story)
      case Some(StoryWriter.ScenePromotion) => positivePromotionWriter(story)
      case Some(StoryWriter.ScenePawnCapture) => positivePawnCaptureWriter(story)
      case Some(StoryWriter.ScenePassedPawnCreated) => positivePassedPawnCreatedWriter(story)
      case Some(StoryWriter.SceneFileOpened) => positiveFileOpenedWriter(story)
      case Some(StoryWriter.SceneCheckGiven) => positiveCheckGivenWriter(story)
      case Some(StoryWriter.SceneCheckEscaped) => positiveCheckEscapedWriter(story)
      case Some(StoryWriter.SceneCheckmate) => positiveCheckmateWriter(story)
      case Some(StoryWriter.SceneStalemate) => positiveStalemateWriter(story)
      case _ => false

  private def positiveHangingWriter(story: Story) =
      story.scene == Scene.Tactic &&
      story.tactic.contains(Tactic.Hanging) &&
      story.plan.isEmpty &&
      story.fileOpenedProof.isEmpty &&
      !story.engineCheck.exists(_.status == EngineCheckStatus.Refutes) &&
      story.captureResult.exists: result =>
        result.positiveMaterial &&
          result.sameBoardProof &&
          result.missingEvidence.isEmpty &&
          captureResultBindsStory(story, result) &&
          result.targetPiece.exists(piece => piece.man != Man.Pawn && piece.man != Man.King)

  private def positiveForkWriter(story: Story) =
      story.scene == Scene.Tactic &&
      story.tactic.contains(Tactic.Fork) &&
      story.plan.isEmpty &&
      story.fileOpenedProof.isEmpty &&
      !story.engineCheck.exists(_.status == EngineCheckStatus.Refutes) &&
      story.multiTargetProof.exists: proof =>
        proof.complete &&
          proof.sameBoardProof &&
          multiTargetProofBindsStory(story, proof) &&
          multiTargetRelationProven(proof)

  private def positiveMaterialWriter(story: Story) =
      story.scene == Scene.Material &&
      story.tactic.isEmpty &&
      story.plan.isEmpty &&
      story.fileOpenedProof.isEmpty &&
      !story.engineCheck.exists(_.status == EngineCheckStatus.Refutes) &&
      story.captureResult.exists: result =>
        result.positiveMaterial &&
          result.sameBoardProof &&
          result.missingEvidence.isEmpty &&
          result.materialResult.nonEmpty &&
          result.boundedExchangeSequence.nonEmpty &&
          captureResultBindsStory(story, result)

  private def positiveDefenseWriter(story: Story) =
      story.scene == Scene.Defense &&
      story.tactic.isEmpty &&
      story.plan.isEmpty &&
      story.fileOpenedProof.isEmpty &&
      !story.engineCheck.exists(_.status == EngineCheckStatus.Refutes) &&
      story.threatProof.exists(proof => proof.complete && proof.sameBoardProof) &&
      story.defenseProof.exists: proof =>
        proof.complete &&
          proof.sameBoardProof &&
          proof.defenseMove.nonEmpty &&
          proof.defendedTarget.nonEmpty &&
          proof.materialLossPrevented.exists(_ > 0) &&
          defenseProofBindsStory(story, proof)

  private def positiveDiscoveredAttackWriter(story: Story) =
      story.scene == Scene.Tactic &&
      story.tactic.contains(Tactic.DiscoveredAttack) &&
      story.plan.isEmpty &&
      story.fileOpenedProof.isEmpty &&
      !story.engineCheck.exists(_.status == EngineCheckStatus.Refutes) &&
      story.lineProof.exists: proof =>
        proof.complete &&
          proof.sameBoardProof &&
          proof.afterSliderAttacksTarget &&
          proof.targetNonKingMaterial &&
          lineProofBindsStory(story, proof)

  private def positivePinWriter(story: Story) =
    story.scene == Scene.Tactic &&
      story.tactic.contains(Tactic.Pin) &&
      story.plan.isEmpty &&
      story.captureResult.isEmpty &&
      story.threatProof.isEmpty &&
      story.defenseProof.isEmpty &&
      story.fileOpenedProof.isEmpty &&
      !story.engineCheck.exists(_.status == EngineCheckStatus.Refutes) &&
      story.pinProof.exists: proof =>
        proof.complete &&
          proof.sameBoardProof &&
          proof.afterPinRelation &&
          proof.targetNonKing &&
          proof.targetAndKingSameSide &&
          proof.sliderAttacksThroughTargetTowardKingAfterMove &&
          pinProofBindsStory(story, proof)

  private def positiveRemoveGuardWriter(story: Story) =
    story.scene == Scene.Tactic &&
      story.tactic.contains(Tactic.RemoveGuard) &&
      story.plan.isEmpty &&
      story.captureResult.isEmpty &&
      story.threatProof.isEmpty &&
      story.defenseProof.isEmpty &&
      story.multiTargetProof.isEmpty &&
      story.lineProof.isEmpty &&
      story.pinProof.isEmpty &&
      story.overloadProof.isEmpty &&
      story.deflectProof.isEmpty &&
      story.fileOpenedProof.isEmpty &&
      story.trapProof.isEmpty &&
      !story.engineCheck.exists(_.status == EngineCheckStatus.Refutes) &&
      story.removeGuardProof.exists: proof =>
        proof.complete &&
          proof.sameBoardProof &&
          proof.exactBoardAfterMoveRelation &&
          proof.targetNonKingMaterial &&
          proof.defenderGuardedTargetBeforeMove &&
          proof.afterMoveDefenderNoLongerGuardsTarget &&
          proof.removeGuardMove.nonEmpty &&
          proof.guardedTarget.nonEmpty &&
          proof.removedDefender.nonEmpty &&
          removeGuardProofBindsStory(story, proof)

  private def positiveOverloadWriter(story: Story) =
    story.scene == Scene.Tactic &&
      story.tactic.contains(Tactic.Overload) &&
      story.plan.isEmpty &&
      story.captureResult.isEmpty &&
      story.threatProof.isEmpty &&
      story.defenseProof.isEmpty &&
      story.multiTargetProof.isEmpty &&
      story.lineProof.isEmpty &&
      story.pinProof.isEmpty &&
      story.removeGuardProof.isEmpty &&
      story.deflectProof.isEmpty &&
      story.skewerProof.isEmpty &&
      story.queenHitProof.isEmpty &&
      story.loosePieceProof.isEmpty &&
      story.trapProof.isEmpty &&
      story.fileOpenedProof.isEmpty &&
      !story.engineCheck.exists(_.status == EngineCheckStatus.Refutes) &&
      story.overloadProof.exists: proof =>
        proof.complete &&
          proof.proofComplete &&
          proof.sameBoardProof &&
          proof.legalMove &&
          proof.sameBoardLegalReplay &&
          proof.dualDutyDefender &&
          proof.dutyTargetsNonKingMaterial &&
          proof.testMoveAttacksDutyTargetAfter &&
          proof.noReplyPreservesBothDutyTargets &&
          proof.defenderDuty.exists(_.complete) &&
          proof.dualDefenderDuty.exists(_.complete) &&
          proof.overloadTest.exists(_.complete) &&
          proof.cannotSatisfyBoth.exists(_.complete) &&
          proof.overloadMove.nonEmpty &&
          proof.defender.nonEmpty &&
          proof.testedDutyTarget.nonEmpty &&
          proof.dutyTargets.size == 2 &&
          overloadProofBindsStory(story, proof)

  private def positiveDeflectWriter(story: Story) =
    story.scene == Scene.Tactic &&
      story.tactic.contains(Tactic.Deflect) &&
      story.plan.isEmpty &&
      story.captureResult.isEmpty &&
      story.threatProof.isEmpty &&
      story.defenseProof.isEmpty &&
      story.multiTargetProof.isEmpty &&
      story.lineProof.isEmpty &&
      story.pinProof.isEmpty &&
      story.removeGuardProof.isEmpty &&
      story.overloadProof.isEmpty &&
      story.skewerProof.isEmpty &&
      story.queenHitProof.isEmpty &&
      story.loosePieceProof.isEmpty &&
      story.trapProof.isEmpty &&
      story.fileOpenedProof.isEmpty &&
      story.secondaryTarget.isEmpty &&
      !story.engineCheck.exists(_.status == EngineCheckStatus.Refutes) &&
      story.deflectProof.exists: proof =>
        proof.complete &&
          proof.sameBoardProof &&
          proof.legalSideMove &&
          proof.legalRivalReply &&
          proof.replyByNamedDefender &&
          proof.defenderGuardedTargetBeforeReply &&
          proof.defenderNoLongerGuardsTargetAfterReply &&
          proof.targetRemainsAfterReply &&
          proof.defenderRemainsAfterReply &&
          proof.sideMoveDoesNotCaptureDefender &&
          proof.completeStoryProof &&
          proof.noEngineEvidenceUsed &&
          deflectProofBindsStory(story, proof)

  private def positiveDecoyWriter(story: Story) =
    story.scene == Scene.Tactic &&
      story.tactic.contains(Tactic.Decoy) &&
      story.plan.isEmpty &&
      story.captureResult.isEmpty &&
      story.threatProof.isEmpty &&
      story.defenseProof.isEmpty &&
      story.multiTargetProof.isEmpty &&
      story.lineProof.isEmpty &&
      story.pinProof.isEmpty &&
      story.removeGuardProof.isEmpty &&
      story.overloadProof.isEmpty &&
      story.deflectProof.isEmpty &&
      story.skewerProof.isEmpty &&
      story.queenHitProof.isEmpty &&
      story.loosePieceProof.isEmpty &&
      story.trapProof.isEmpty &&
      story.fileOpenedProof.isEmpty &&
      story.secondaryTarget.isEmpty &&
      !story.engineCheck.exists(_.status == EngineCheckStatus.Refutes) &&
      story.decoyProof.exists: proof =>
        proof.complete &&
          proof.sameBoardProof &&
          proof.legalSideMove &&
          proof.legalRivalReply &&
          proof.replyByNamedPiece &&
          proof.rivalPieceRemainsAfterReply &&
          proof.replyLandsOnDecoySquare &&
          proof.decoySquareEqualsLandingSquare &&
          proof.completeTrapFollowUpProof &&
          proof.trapFollowUpBindsSamePieceAndSquare &&
          proof.completeStoryProof &&
          proof.noEngineEvidenceUsed &&
          decoyProofBindsStory(story, proof)

  private def positiveInterferenceWriter(story: Story) =
    story.scene == Scene.Tactic &&
      story.tactic.contains(Tactic.Interference) &&
      story.plan.isEmpty &&
      story.captureResult.isEmpty &&
      story.threatProof.isEmpty &&
      story.defenseProof.isEmpty &&
      story.multiTargetProof.isEmpty &&
      story.lineProof.isEmpty &&
      story.pinProof.isEmpty &&
      story.removeGuardProof.isEmpty &&
      story.overloadProof.isEmpty &&
      story.deflectProof.isEmpty &&
      story.decoyProof.isEmpty &&
      story.skewerProof.isEmpty &&
      story.queenHitProof.isEmpty &&
      story.loosePieceProof.isEmpty &&
      story.trapProof.isEmpty &&
      story.fileOpenedProof.isEmpty &&
      story.secondaryTarget.isEmpty &&
      !story.engineCheck.exists(_.status == EngineCheckStatus.Refutes) &&
      story.interferenceProof.exists: proof =>
        proof.complete &&
          proof.sameBoardProof &&
          proof.legalSideMove &&
          proof.completeStoryProof &&
          proof.moveIsNonCapture &&
          proof.movedPieceLandsOnBlockingSquare &&
          proof.lineDefenderIsSlider &&
          proof.targetBound &&
          proof.defenderBlockingTargetCollinearOnSliderRay &&
          proof.blockingSquareStrictlyBetweenDefenderAndTarget &&
          proof.defenderLineContactBeforeMove &&
          proof.afterMoveBlockingSquareOccupiedByMovedPiece &&
          proof.defenderLineContactRemovedByBlocker &&
          proof.noEngineEvidenceUsed &&
          interferenceProofBindsStory(story, proof)

  private def skewerWriterShape(story: Story) =
    story.scene == Scene.Tactic &&
      story.tactic.contains(Tactic.Skewer) &&
      story.plan.isEmpty &&
      story.captureResult.isEmpty &&
      story.threatProof.isEmpty &&
      story.defenseProof.isEmpty &&
      story.multiTargetProof.isEmpty &&
      story.lineProof.isEmpty &&
      story.pinProof.isEmpty &&
      story.removeGuardProof.isEmpty &&
      story.fileOpenedProof.isEmpty &&
      !story.engineCheck.exists(_.status == EngineCheckStatus.Refutes) &&
      story.skewerProof.exists: proof =>
        proof.complete &&
          proof.sameBoardProof &&
          proof.frontTargetNonKingMaterial &&
          proof.rearTargetNonKingMaterial &&
          proof.frontAndRearSameRivalSide &&
          proof.afterMoveSliderAttacksFrontTarget &&
          proof.rearTargetBehindFrontTargetOnSameRay &&
          proof.noExtraBlockerBreaksFrontToRearRelation &&
          proof.beforeSkewerRelationAbsentOrBlocked &&
          proof.skewerMove.nonEmpty &&
          proof.skewerSlider.nonEmpty &&
          proof.frontTarget.nonEmpty &&
          proof.rearTarget.nonEmpty &&
          skewerProofBindsStory(story, proof)

  private def positiveSkewerWriter(story: Story) =
    skewerWriterShape(story)

  private def positiveQueenHitWriter(story: Story) =
    queenHitWriterShape(story)

  private def positiveLooseWriter(story: Story) =
    looseWriterShape(story)

  private def positiveTrapWriter(story: Story) =
    story.scene == Scene.Tactic &&
      story.tactic.contains(Tactic.Trap) &&
      story.plan.isEmpty &&
      story.captureResult.isEmpty &&
      story.threatProof.isEmpty &&
      story.defenseProof.isEmpty &&
      story.multiTargetProof.isEmpty &&
      story.lineProof.isEmpty &&
      story.pinProof.isEmpty &&
      story.removeGuardProof.isEmpty &&
      story.overloadProof.isEmpty &&
      story.deflectProof.isEmpty &&
      story.skewerProof.isEmpty &&
      story.queenHitProof.isEmpty &&
      story.loosePieceProof.isEmpty &&
      story.fileOpenedProof.isEmpty &&
      story.secondaryTarget.isEmpty &&
      !story.engineCheck.exists(_.status == EngineCheckStatus.Refutes) &&
      story.trapProof.exists: proof =>
        proof.complete &&
          proof.sameBoardProof &&
          proof.legalMove &&
          proof.nonCapturingMove &&
          proof.exactAfterBoardReplay &&
          proof.completeStoryProof &&
          proof.targetRivalMinor &&
          proof.targetDefendedByRivalSide &&
          proof.afterBoardTargetAttackedByMovingSide &&
          proof.targetHasLegalMoves &&
          proof.everyTargetMoveUnsafe &&
          proof.routeDoesNotGiveCheck &&
          proof.routeDoesNotGiveMate &&
          proof.routeDoesNotPromote &&
          proof.noEngineEvidenceUsed &&
          trapProofBindsStory(story, proof)

  private def looseWriterShape(story: Story) =
    story.scene == Scene.Tactic &&
      story.tactic.contains(Tactic.Loose) &&
      story.plan.isEmpty &&
      story.captureResult.isEmpty &&
      story.threatProof.isEmpty &&
      story.defenseProof.isEmpty &&
      story.multiTargetProof.isEmpty &&
      story.lineProof.isEmpty &&
      story.pinProof.isEmpty &&
      story.removeGuardProof.isEmpty &&
      story.skewerProof.isEmpty &&
      story.queenHitProof.isEmpty &&
      story.fileOpenedProof.isEmpty &&
      story.secondaryTarget.isEmpty &&
      !story.engineCheck.exists(_.status == EngineCheckStatus.Refutes) &&
      story.loosePieceProof.exists: proof =>
        proof.complete &&
          proof.sameBoardProof &&
          proof.legalMove &&
          proof.exactAfterBoardReplay &&
          proof.targetRivalOwned &&
          proof.targetNonKing &&
          proof.afterBoardTargetAttackedByMovingSide &&
          proof.rivalLegalDefendersAfter.isEmpty &&
          proof.rivalSideHasNoLegalDefenderOfTarget &&
          proof.looseAttackProducedOrRevealedByLegalMove &&
          loosePieceProofBindsStory(story, proof)

  private def queenHitWriterShape(story: Story) =
    story.scene == Scene.Tactic &&
      story.tactic.contains(Tactic.QueenHit) &&
      story.plan.isEmpty &&
      story.captureResult.isEmpty &&
      story.threatProof.isEmpty &&
      story.defenseProof.isEmpty &&
      story.multiTargetProof.isEmpty &&
      story.lineProof.isEmpty &&
      story.pinProof.isEmpty &&
      story.removeGuardProof.isEmpty &&
      story.skewerProof.isEmpty &&
      story.loosePieceProof.isEmpty &&
      story.fileOpenedProof.isEmpty &&
      story.secondaryTarget.isEmpty &&
      !story.engineCheck.exists(_.status == EngineCheckStatus.Refutes) &&
      story.queenHitProof.exists: proof =>
        proof.complete &&
          proof.sameBoardProof &&
          proof.legalMove &&
          proof.exactAfterBoardReplay &&
          proof.rivalQueenExistsAfter &&
          proof.afterBoardQueenAttackedByMovingSide &&
          proof.queenHitProducedOrRevealedByLegalMove &&
          queenHitProofBindsStory(story, proof)

  private def positivePawnAdvanceWriter(story: Story) =
    story.scene == Scene.PawnAdvance &&
      story.tactic.isEmpty &&
      story.plan.isEmpty &&
      story.captureResult.isEmpty &&
      story.threatProof.isEmpty &&
      story.defenseProof.isEmpty &&
      story.multiTargetProof.isEmpty &&
      story.lineProof.isEmpty &&
      story.pinProof.isEmpty &&
      story.removeGuardProof.isEmpty &&
      story.skewerProof.isEmpty &&
      story.queenHitProof.isEmpty &&
      story.loosePieceProof.isEmpty &&
      story.pawnStopProof.isEmpty &&
      story.pawnBreakProof.isEmpty &&
      story.pawnBlockProof.isEmpty &&
      story.fileOpenedProof.isEmpty &&
      story.promotionThreatProof.isEmpty &&
      story.promotionProof.isEmpty &&
      !story.engineCheck.exists(_.status == EngineCheckStatus.Refutes) &&
      story.pawnAdvanceProof.exists: proof =>
        proof.complete &&
          proof.sameBoardProof &&
          proof.legalOneStepNonCaptureNonPromotion &&
          proof.alreadyPassedBefore &&
          proof.exactAfterBoardReplay &&
          proof.afterBoardPassedPawn &&
          pawnAdvanceProofBindsStory(story, proof)

  private def positivePawnStopWriter(story: Story) =
    story.scene == Scene.PawnStop &&
      story.tactic.isEmpty &&
      story.plan.isEmpty &&
      story.captureResult.isEmpty &&
      story.threatProof.isEmpty &&
      story.defenseProof.isEmpty &&
      story.multiTargetProof.isEmpty &&
      story.lineProof.isEmpty &&
      story.pinProof.isEmpty &&
      story.removeGuardProof.isEmpty &&
      story.skewerProof.isEmpty &&
      story.pawnAdvanceProof.isEmpty &&
      story.pawnBreakProof.isEmpty &&
      story.pawnBlockProof.isEmpty &&
      story.fileOpenedProof.isEmpty &&
      story.promotionThreatProof.isEmpty &&
      story.promotionProof.isEmpty &&
      !story.engineCheck.exists(_.status == EngineCheckStatus.Refutes) &&
      story.pawnStopProof.exists: proof =>
        proof.complete &&
          proof.sameBoardProof &&
          proof.legalStopMove &&
          proof.targetPawn.nonEmpty &&
          proof.targetPawnAlreadyPassed &&
          proof.nextAdvanceSquareNonPromotion &&
          proof.nextAdvanceSquareEmptyBefore &&
          proof.stopKind.exists(PawnStopKind.values.contains) &&
          (
            proof.nextAdvanceSquareOccupiedAfter ||
              proof.nextAdvanceSquareAttackedAfter ||
              proof.nextAdvanceSquareControlledByPawnAfter
          ) &&
          proof.exactAfterBoardReplay &&
          proof.targetPawnStillPresentAfter &&
          proof.nextAdvanceSquareStoppedAfter &&
          pawnStopProofBindsStory(story, proof)

  private def positivePawnBreakWriter(story: Story) =
    story.scene == Scene.PawnBreak &&
      story.tactic.isEmpty &&
      story.plan.isEmpty &&
      story.captureResult.isEmpty &&
      story.threatProof.isEmpty &&
      story.defenseProof.isEmpty &&
      story.multiTargetProof.isEmpty &&
      story.lineProof.isEmpty &&
      story.pinProof.isEmpty &&
      story.removeGuardProof.isEmpty &&
      story.skewerProof.isEmpty &&
      story.pawnAdvanceProof.isEmpty &&
      story.pawnStopProof.isEmpty &&
      story.pawnCaptureProof.isEmpty &&
      story.passedPawnCreatedProof.isEmpty &&
      story.fileOpenedProof.isEmpty &&
      story.pawnBlockProof.isEmpty &&
      story.promotionThreatProof.isEmpty &&
      story.promotionProof.isEmpty &&
      !story.engineCheck.exists(_.status == EngineCheckStatus.Refutes) &&
      story.pawnBreakProof.exists: proof =>
        proof.complete &&
          proof.sameBoardProof &&
          proof.legalPawnMove &&
          proof.nonPromotionMove &&
          proof.nonCapturingMove &&
          proof.exactAfterBoardReplay &&
          proof.directPawnLeverAfterMove &&
          proof.leverCreatedByMove &&
          proof.singleRivalPawnTarget &&
          proof.contactKinds == Vector(
            PawnBreakContactKind.PawnChallengesPawn,
            PawnBreakContactKind.PawnLeverCreated
          ) &&
          pawnBreakProofBindsStory(story, proof)

  private def positivePawnBlockWriter(story: Story) =
    story.scene == Scene.PawnBlock &&
      story.tactic.isEmpty &&
      story.plan.isEmpty &&
      story.captureResult.isEmpty &&
      story.threatProof.isEmpty &&
      story.defenseProof.isEmpty &&
      story.multiTargetProof.isEmpty &&
      story.lineProof.isEmpty &&
      story.pinProof.isEmpty &&
      story.removeGuardProof.isEmpty &&
      story.skewerProof.isEmpty &&
      story.pawnAdvanceProof.isEmpty &&
      story.pawnStopProof.isEmpty &&
      story.pawnBreakProof.isEmpty &&
      story.pawnCaptureProof.isEmpty &&
      story.passedPawnCreatedProof.isEmpty &&
      story.fileOpenedProof.isEmpty &&
      story.promotionThreatProof.isEmpty &&
      story.promotionProof.isEmpty &&
      !story.engineCheck.exists(_.status == EngineCheckStatus.Refutes) &&
      story.pawnBlockProof.exists: proof =>
        proof.complete &&
          proof.sameBoardProof &&
          proof.legalMove &&
          proof.exactAfterBoardReplay &&
          proof.blockCreatedByMove &&
          proof.nextAdvanceSquareOccupiedAfter &&
          proof.occupyingPieceBelongsToBlockingSide &&
          proof.ordinaryDirectOneSquarePawnBlock &&
          proof.blockedPawn.exists(_.man == Man.Pawn) &&
          proof.blockedPawn.exists(_.side == proof.rivalSide) &&
          proof.blockedPawnNextAdvanceSquare == story.target &&
          pawnBlockProofBindsStory(story, proof)

  private def positivePromotionThreatWriter(story: Story) =
    story.scene == Scene.PromotionThreat &&
      story.tactic.isEmpty &&
      story.plan.isEmpty &&
      story.captureResult.isEmpty &&
      story.threatProof.isEmpty &&
      story.defenseProof.isEmpty &&
      story.multiTargetProof.isEmpty &&
      story.lineProof.isEmpty &&
      story.pinProof.isEmpty &&
      story.removeGuardProof.isEmpty &&
      story.skewerProof.isEmpty &&
      story.pawnAdvanceProof.isEmpty &&
      story.pawnStopProof.isEmpty &&
      story.pawnBreakProof.isEmpty &&
      story.pawnBlockProof.isEmpty &&
      story.pawnCaptureProof.isEmpty &&
      story.passedPawnCreatedProof.isEmpty &&
      story.fileOpenedProof.isEmpty &&
      story.promotionProof.isEmpty &&
      !story.engineCheck.exists(_.status == EngineCheckStatus.Refutes) &&
      story.promotionThreatProof.exists: proof =>
        proof.complete &&
          proof.sameBoardProof &&
          proof.legalPawnMove &&
          proof.nonPromotionCreatingMove &&
          proof.exactAfterBoardReplay &&
          proof.pawnOnPenultimateRankAfter &&
          proof.nextMovePromotionLegal &&
          promotionThreatProofBindsStory(story, proof)

  private def positivePawnCaptureWriter(story: Story) =
    story.scene == Scene.PawnCapture &&
      story.tactic.isEmpty &&
      story.plan.isEmpty &&
      story.captureResult.isEmpty &&
      story.threatProof.isEmpty &&
      story.defenseProof.isEmpty &&
      story.multiTargetProof.isEmpty &&
      story.lineProof.isEmpty &&
      story.pinProof.isEmpty &&
      story.removeGuardProof.isEmpty &&
      story.skewerProof.isEmpty &&
      story.pawnAdvanceProof.isEmpty &&
      story.pawnStopProof.isEmpty &&
      story.pawnBreakProof.isEmpty &&
      story.pawnBlockProof.isEmpty &&
      story.passedPawnCreatedProof.isEmpty &&
      story.fileOpenedProof.isEmpty &&
      story.promotionThreatProof.isEmpty &&
      story.promotionProof.isEmpty &&
      !story.engineCheck.exists(_.status == EngineCheckStatus.Refutes) &&
      story.pawnCaptureProof.exists: proof =>
        proof.complete &&
          proof.sameBoardProof &&
          proof.legalPawnMove &&
          proof.legalPawnCapture &&
          proof.nonPromotionMove &&
          proof.ordinaryDiagonalPawnCapture &&
          proof.pawnCapturesPawn &&
          proof.exactAfterBoardReplay &&
          proof.singleRivalPawnCaptured &&
          pawnCaptureProofBindsStory(story, proof)

  private def positivePassedPawnCreatedWriter(story: Story) =
    story.scene == Scene.PassedPawnCreated &&
      story.tactic.isEmpty &&
      story.plan.isEmpty &&
      story.captureResult.isEmpty &&
      story.threatProof.isEmpty &&
      story.defenseProof.isEmpty &&
      story.multiTargetProof.isEmpty &&
      story.lineProof.isEmpty &&
      story.pinProof.isEmpty &&
      story.removeGuardProof.isEmpty &&
      story.skewerProof.isEmpty &&
      story.pawnAdvanceProof.isEmpty &&
      story.pawnStopProof.isEmpty &&
      story.pawnBreakProof.isEmpty &&
      story.pawnBlockProof.isEmpty &&
      story.pawnCaptureProof.isEmpty &&
      story.fileOpenedProof.isEmpty &&
      story.promotionThreatProof.isEmpty &&
      story.promotionProof.isEmpty &&
      !story.engineCheck.exists(_.status == EngineCheckStatus.Refutes) &&
      story.passedPawnCreatedProof.exists: proof =>
        proof.complete &&
          proof.sameBoardProof &&
          proof.exactBeforeBoard &&
          proof.legalPawnMove &&
          proof.ordinaryPawnMoveOrCapture &&
          proof.nonPromotionMove &&
          !proof.passedBefore &&
          proof.exactAfterBoardReplay &&
          proof.passedAfter &&
          proof.exactlyOneNewPassedPawn &&
          passedPawnCreatedProofBindsStory(story, proof)

  private def positiveFileOpenedWriter(story: Story) =
    story.scene == Scene.FileOpened &&
      story.tactic.isEmpty &&
      story.plan.isEmpty &&
      story.captureResult.isEmpty &&
      story.threatProof.isEmpty &&
      story.defenseProof.isEmpty &&
      story.multiTargetProof.isEmpty &&
      story.lineProof.isEmpty &&
      story.pinProof.isEmpty &&
      story.removeGuardProof.isEmpty &&
      story.skewerProof.isEmpty &&
      story.pawnAdvanceProof.isEmpty &&
      story.pawnStopProof.isEmpty &&
      story.pawnBreakProof.isEmpty &&
      story.pawnBlockProof.isEmpty &&
      story.pawnCaptureProof.isEmpty &&
      story.passedPawnCreatedProof.isEmpty &&
      story.promotionThreatProof.isEmpty &&
      story.promotionProof.isEmpty &&
      story.openedFile.nonEmpty &&
      !story.engineCheck.exists(_.status == EngineCheckStatus.Refutes) &&
      story.fileOpenedProof.exists: proof =>
        proof.complete &&
          proof.sameBoardProof &&
          proof.legalPawnMove &&
          proof.nonPromotionMove &&
          proof.ordinaryPawnMoveOrCapture &&
          !proof.enPassantMove &&
          proof.leavesOriginFile &&
          proof.originFileOccupiedBeforeByMovingPawn &&
          !proof.originFileOpenBefore &&
          proof.exactAfterBoardReplay &&
          proof.afterBoardHasNoWhitePawnOnOriginFile &&
          proof.afterBoardHasNoBlackPawnOnOriginFile &&
          proof.originFileOpenAfter &&
          proof.openedFileIsOriginFile &&
          fileOpenedProofBindsStory(story, proof)

  private def positivePromotionWriter(story: Story) =
    story.scene == Scene.Promotion &&
      story.tactic.isEmpty &&
      story.plan.isEmpty &&
      story.captureResult.isEmpty &&
      story.threatProof.isEmpty &&
      story.defenseProof.isEmpty &&
      story.multiTargetProof.isEmpty &&
      story.lineProof.isEmpty &&
      story.pinProof.isEmpty &&
      story.removeGuardProof.isEmpty &&
      story.skewerProof.isEmpty &&
      story.pawnAdvanceProof.isEmpty &&
      story.pawnStopProof.isEmpty &&
      story.pawnBreakProof.isEmpty &&
      story.pawnBlockProof.isEmpty &&
      story.pawnCaptureProof.isEmpty &&
      story.passedPawnCreatedProof.isEmpty &&
      story.fileOpenedProof.isEmpty &&
      story.promotionThreatProof.isEmpty &&
      story.proof.conversionPrize == 0 &&
      !story.engineCheck.exists(_.status == EngineCheckStatus.Refutes) &&
      story.promotionProof.exists: proof =>
        proof.complete &&
          proof.sameBoardProof &&
          proof.legalPromotionMove &&
          proof.nonCapturing &&
          proof.exactBoardReplay &&
          proof.pawnReachesFinalRank &&
          proof.promotedPiece.nonEmpty &&
          promotionProofBindsStory(story, proof)

  private def positiveCheckGivenWriter(story: Story) =
    story.scene == Scene.CheckGiven &&
      story.tactic.isEmpty &&
      story.plan.isEmpty &&
      story.captureResult.isEmpty &&
      story.secondaryTarget.isEmpty &&
      story.multiTargetProof.isEmpty &&
      story.threatProof.isEmpty &&
      story.defenseProof.isEmpty &&
      story.lineProof.isEmpty &&
      story.pinProof.isEmpty &&
      story.removeGuardProof.isEmpty &&
      story.skewerProof.isEmpty &&
      story.pawnAdvanceProof.isEmpty &&
      story.pawnStopProof.isEmpty &&
      story.pawnBreakProof.isEmpty &&
      story.pawnBlockProof.isEmpty &&
      story.promotionThreatProof.isEmpty &&
      story.promotionProof.isEmpty &&
      story.pawnCaptureProof.isEmpty &&
      story.passedPawnCreatedProof.isEmpty &&
      story.fileOpenedProof.isEmpty &&
      story.openedFile.isEmpty &&
      story.checkEscapedProof.isEmpty &&
      story.checkmateProof.isEmpty &&
      !story.engineCheck.exists(_.status == EngineCheckStatus.Refutes) &&
      story.checkGivenProof.exists: proof =>
        proof.complete &&
          proof.sameBoardProof &&
          proof.legalMove &&
          proof.exactAfterBoardReplay &&
          proof.afterBoardRivalKingInCheck &&
          proof.checkProducedByLegalMove &&
          proof.rivalKingSquareAfter == story.target &&
          checkGivenProofBindsStory(story, proof)

  private def positiveCheckEscapedWriter(story: Story) =
    story.scene == Scene.CheckEscaped &&
      story.tactic.isEmpty &&
      story.plan.isEmpty &&
      story.captureResult.isEmpty &&
      story.secondaryTarget.isEmpty &&
      story.multiTargetProof.isEmpty &&
      story.threatProof.isEmpty &&
      story.defenseProof.isEmpty &&
      story.lineProof.isEmpty &&
      story.pinProof.isEmpty &&
      story.removeGuardProof.isEmpty &&
      story.skewerProof.isEmpty &&
      story.pawnAdvanceProof.isEmpty &&
      story.pawnStopProof.isEmpty &&
      story.pawnBreakProof.isEmpty &&
      story.pawnBlockProof.isEmpty &&
      story.promotionThreatProof.isEmpty &&
      story.promotionProof.isEmpty &&
      story.pawnCaptureProof.isEmpty &&
      story.passedPawnCreatedProof.isEmpty &&
      story.fileOpenedProof.isEmpty &&
      story.openedFile.isEmpty &&
      story.checkGivenProof.isEmpty &&
      story.checkmateProof.isEmpty &&
      !story.engineCheck.exists(_.status == EngineCheckStatus.Refutes) &&
      story.checkEscapedProof.exists: proof =>
        proof.complete &&
          proof.sameBoardProof &&
          proof.legalMove &&
          proof.exactBeforeBoardState &&
          proof.exactAfterBoardReplay &&
          proof.beforeBoardSideKingInCheck &&
          proof.afterBoardSideKingNotInCheck &&
          proof.checkEscapedByLegalMove &&
          proof.beforeKingSquare.nonEmpty &&
          proof.originSquare == story.anchor &&
          proof.afterKingSquare == story.target &&
          checkEscapedProofBindsStory(story, proof)

  private def positiveCheckmateWriter(story: Story) =
    story.scene == Scene.Checkmate &&
      story.tactic.isEmpty &&
      story.plan.isEmpty &&
      story.captureResult.isEmpty &&
      story.secondaryTarget.isEmpty &&
      story.multiTargetProof.isEmpty &&
      story.threatProof.isEmpty &&
      story.defenseProof.isEmpty &&
      story.lineProof.isEmpty &&
      story.pinProof.isEmpty &&
      story.removeGuardProof.isEmpty &&
      story.skewerProof.isEmpty &&
      story.pawnAdvanceProof.isEmpty &&
      story.pawnStopProof.isEmpty &&
      story.pawnBreakProof.isEmpty &&
      story.pawnBlockProof.isEmpty &&
      story.promotionThreatProof.isEmpty &&
      story.promotionProof.isEmpty &&
      story.pawnCaptureProof.isEmpty &&
      story.passedPawnCreatedProof.isEmpty &&
      story.fileOpenedProof.isEmpty &&
      story.openedFile.isEmpty &&
      story.checkGivenProof.isEmpty &&
      story.checkEscapedProof.isEmpty &&
      !story.engineCheck.exists(_.status == EngineCheckStatus.Refutes) &&
      story.checkmateProof.exists: proof =>
        proof.complete &&
          proof.sameBoardProof &&
          proof.legalMove &&
          proof.exactAfterBoardReplay &&
          proof.afterBoardRivalKingInCheck &&
          proof.afterBoardRivalSideHasNoLegalEscape &&
          proof.checkmateProducedByLegalMove &&
          proof.rivalKingSquareAfter == story.target &&
          checkmateProofBindsStory(story, proof)

  private def positiveStalemateWriter(story: Story) =
    story.scene == Scene.Stalemate &&
      story.tactic.isEmpty &&
      story.plan.isEmpty &&
      story.captureResult.isEmpty &&
      story.secondaryTarget.isEmpty &&
      story.multiTargetProof.isEmpty &&
      story.threatProof.isEmpty &&
      story.defenseProof.isEmpty &&
      story.lineProof.isEmpty &&
      story.pinProof.isEmpty &&
      story.removeGuardProof.isEmpty &&
      story.skewerProof.isEmpty &&
      story.pawnAdvanceProof.isEmpty &&
      story.pawnStopProof.isEmpty &&
      story.pawnBreakProof.isEmpty &&
      story.pawnBlockProof.isEmpty &&
      story.promotionThreatProof.isEmpty &&
      story.promotionProof.isEmpty &&
      story.pawnCaptureProof.isEmpty &&
      story.passedPawnCreatedProof.isEmpty &&
      story.fileOpenedProof.isEmpty &&
      story.openedFile.isEmpty &&
      story.checkGivenProof.isEmpty &&
      story.checkEscapedProof.isEmpty &&
      story.checkmateProof.isEmpty &&
      !story.engineCheck.exists(_.status == EngineCheckStatus.Refutes) &&
      story.stalemateProof.exists: proof =>
        proof.complete &&
          proof.sameBoardProof &&
          proof.legalMove &&
          proof.exactAfterBoardReplay &&
          proof.afterBoardRivalSideNotInCheck &&
          proof.afterBoardRivalSideHasNoLegalMoves &&
          proof.stalemateProducedByLegalMove &&
          proof.rivalKingSquareAfter == story.target &&
          stalemateProofBindsStory(story, proof)

  private def captureResultBindsStory(story: Story, result: CaptureResult) =
    result.side == story.side &&
      story.route.contains(result.captureLine) &&
      result.capturingPiece.exists(piece => story.anchor.contains(piece.square)) &&
      result.targetPiece.exists(piece => story.target.contains(piece.square))

  private def multiTargetProofBindsStory(story: Story, proof: MultiTargetProof) =
    proof.side == story.side &&
      proof.forkMove.exists(move => story.route.contains(move)) &&
      proof.attackerAfterMove.exists(piece => story.anchor.contains(piece.square)) &&
      proof.targetA.exists(piece => story.rival == piece.side) &&
      story.target.exists(target => proof.targetSquares.headOption.contains(target)) &&
      story.secondaryTarget.exists(target => proof.targetSquares.lift(1).contains(target))

  private def defenseProofBindsStory(story: Story, proof: DefenseProof) =
    proof.defendingSide == story.side &&
      story.rival == proof.originalThreat.rivalSide &&
      proof.defenseMove.exists(move => story.route.contains(move)) &&
      proof.defendedTarget.exists(piece => story.target.contains(piece.square)) &&
      proof.defenseMove.exists(move => story.anchor.contains(move.from))

  private def lineProofBindsStory(story: Story, proof: LineProof) =
    proof.side == story.side &&
      proof.revealingMove.exists(move => story.route.contains(move)) &&
      proof.revealedTarget.exists(piece => story.target.contains(piece.square)) &&
      proof.movedPiece.orElse(proof.slider).exists(piece => story.anchor.contains(piece.square))

  private def pinProofBindsStory(story: Story, proof: PinProof) =
    proof.side == story.side &&
      proof.pinningMove.exists(move => story.route.contains(move)) &&
      proof.pinnedTarget.exists(piece => story.target.contains(piece.square) && story.rival == piece.side) &&
      proof.pinningSlider.exists(piece => story.anchor.contains(piece.square)) &&
      proof.kingBehindTarget.exists(king => proof.pinnedTarget.exists(_.side == king.side))

  private def removeGuardProofBindsStory(story: Story, proof: RemoveGuardProof) =
    proof.side == story.side &&
      proof.rivalSide == story.rival &&
      proof.removeGuardMove.exists(move => story.route.contains(move)) &&
      proof.guardedTarget.exists(piece => story.target.contains(piece.square) && story.rival == piece.side) &&
      proof.removedDefender.exists(piece => story.rival == piece.side) &&
      (
        proof.removedDefender.exists(piece => story.anchor.contains(piece.square)) ||
          proof.removeGuardMove.exists(move => story.anchor.contains(move.from))
      )

  private def overloadProofBindsStory(story: Story, proof: OverloadProof) =
    proof.side == story.side &&
      proof.rivalSide == story.rival &&
      proof.overloadMove.exists(move => story.route.contains(move)) &&
      proof.defender.exists(piece => story.anchor.contains(piece.square) && story.rival == piece.side) &&
      proof.testedDutyTarget.exists(piece => story.target.contains(piece.square) && story.rival == piece.side) &&
      story.secondaryTarget.exists(square => proof.dutyTargets.exists(target => target.square == square)) &&
      proof.otherDutyTarget.exists(piece => story.secondaryTarget.contains(piece.square) && story.rival == piece.side)

  private def deflectProofBindsStory(story: Story, proof: DeflectProof) =
    proof.side == story.side &&
      proof.rivalSide == story.rival &&
      proof.sideMove.exists(move => story.route.contains(move)) &&
      proof.targetBeforeReply.exists(piece => story.target.contains(piece.square) && story.rival == piece.side) &&
      proof.targetAfterReply.exists(piece => story.target.contains(piece.square) && story.rival == piece.side) &&
      proof.defenderAfterReply.exists(piece => story.anchor.contains(piece.square) && story.rival == piece.side)

  private def decoyProofBindsStory(story: Story, proof: DecoyProof) =
    proof.side == story.side &&
      proof.rivalSide == story.rival &&
      proof.sideMove.exists(move => story.route.contains(move)) &&
      proof.decoySquare.exists(square => story.target.contains(square)) &&
      proof.landingSquare.exists(square => story.target.contains(square)) &&
      proof.namedPieceAfterReply.exists(piece => story.rival == piece.side && story.target.contains(piece.square)) &&
      proof.sideMove.exists: move =>
        proof.afterSideMoveBoard.exists(piece => story.side == piece.side && story.anchor.contains(piece.square) && piece.square == move.to)

  private def interferenceProofBindsStory(story: Story, proof: InterferenceProof) =
    proof.side == story.side &&
      proof.rivalSide == story.rival &&
      proof.sideMove.exists(move => story.route.contains(move)) &&
      proof.targetSquare.exists(square => story.target.contains(square)) &&
      proof.blockingSquare.exists(square => story.anchor.contains(square)) &&
      proof.movedPieceAfter.exists(piece => story.side == piece.side && story.anchor.contains(piece.square)) &&
      proof.lineDefenderBefore.exists(piece => story.rival == piece.side) &&
      proof.lineDefenderAfter.exists(piece => story.rival == piece.side)

  private def skewerProofBindsStory(story: Story, proof: SkewerProof) =
    proof.side == story.side &&
      proof.rivalSide == story.rival &&
      proof.skewerMove.exists(move => story.route.contains(move)) &&
      proof.frontTarget.exists(piece => story.target.contains(piece.square) && story.rival == piece.side) &&
      proof.rearTarget.exists(piece =>
        story.secondaryTarget.contains(piece.square) &&
          story.rival == piece.side &&
          piece.man != Man.King
      ) &&
      (
        proof.skewerSlider.exists(piece => story.anchor.contains(piece.square)) ||
          proof.skewerMove.exists(move => story.anchor.contains(move.from))
      )

  private def queenHitProofBindsStory(story: Story, proof: QueenHitProof) =
    proof.attackingSide == story.side &&
      proof.rivalSide == story.rival &&
      proof.attackMove.exists(move => story.route.contains(move)) &&
      proof.rivalQueenSquareAfter.exists(square => story.target.contains(square)) &&
      proof.attackingPieceSquareAfter.exists(square => story.anchor.contains(square))

  private def loosePieceProofBindsStory(story: Story, proof: LoosePieceProof) =
    proof.attackingSide == story.side &&
      proof.rivalSide == story.rival &&
      proof.attackMove.exists(move => story.route.contains(move)) &&
      proof.targetPieceSquareAfter.exists(square => story.target.contains(square)) &&
      proof.attackingPieceSquareAfter.exists(square => story.anchor.contains(square)) &&
      proof.targetPieceAfter.exists(piece => story.rival == piece.side && piece.man != Man.King)

  private def trapProofBindsStory(story: Story, proof: TrapProof) =
    proof.attackingSide == story.side &&
      proof.rivalSide == story.rival &&
      proof.trapMove.exists(move => story.route.contains(move)) &&
      proof.targetPieceSquareAfter.exists(square => story.target.contains(square)) &&
      proof.anchorSquareAfter.exists(square => story.anchor.contains(square)) &&
      proof.targetPieceAfter.exists(piece =>
        story.rival == piece.side &&
          (piece.man == Man.Knight || piece.man == Man.Bishop) &&
          proof.targetPieceSquareAfter.contains(piece.square)
      ) &&
      proof.movingPieceAfter.exists(piece =>
        story.side == piece.side &&
          proof.anchorSquareAfter.contains(piece.square) &&
          story.anchor.contains(piece.square)
      )

  private def pawnAdvanceProofBindsStory(story: Story, proof: PawnAdvanceProof) =
    proof.side == story.side &&
      proof.rivalSide == story.rival &&
      proof.advanceMove.exists(move => story.route.contains(move)) &&
      proof.pawnBefore.exists(piece => story.anchor.contains(piece.square)) &&
      proof.pawnAfter.exists(piece => story.target.contains(piece.square))

  private def pawnStopProofBindsStory(story: Story, proof: PawnStopProof) =
    proof.side == story.side &&
      proof.rivalSide == story.rival &&
      proof.stopMove.exists(move => story.route.contains(move)) &&
      proof.stopMove.exists(move => story.anchor.contains(move.from)) &&
      proof.nextAdvanceSquare.exists(square => story.target.contains(square))

  private def pawnBreakProofBindsStory(story: Story, proof: PawnBreakProof) =
    proof.side == story.side &&
      proof.rivalSide == story.rival &&
      proof.breakMove.exists(move => story.route.contains(move)) &&
      proof.pawnBefore.exists(piece => story.anchor.contains(piece.square)) &&
      proof.targetPawn.exists(piece => story.target.contains(piece.square))

  private def pawnBlockProofBindsStory(story: Story, proof: PawnBlockProof) =
    proof.blockingSide == story.side &&
      proof.rivalSide == story.rival &&
      proof.blockMove.exists(move => story.route.contains(move)) &&
      proof.originSquare.exists(square => story.anchor.contains(square)) &&
      proof.blockedPawnNextAdvanceSquare.exists(square => story.target.contains(square)) &&
      proof.blockedPawn.exists(piece => proof.blockedPawnSquare.contains(piece.square))

  private def pawnCaptureProofBindsStory(story: Story, proof: PawnCaptureProof) =
    proof.side == story.side &&
      proof.rivalSide == story.rival &&
      proof.captureMove.exists(move => story.route.contains(move)) &&
      proof.pawnBefore.exists(piece => story.anchor.contains(piece.square)) &&
      proof.capturedPawn.exists(piece => story.target.contains(piece.square))

  private def passedPawnCreatedProofBindsStory(story: Story, proof: PassedPawnCreatedProof) =
    proof.side == story.side &&
      proof.rivalSide == story.rival &&
      proof.creatingMove.exists(move => story.route.contains(move)) &&
      proof.originSquare.exists(square => story.anchor.contains(square)) &&
      proof.createdPassedPawn.exists(piece => story.target.contains(piece.square))

  private def fileOpenedProofBindsStory(story: Story, proof: FileOpenedProof) =
    proof.side == story.side &&
      proof.rivalSide == story.rival &&
      proof.openingMove.exists(move => story.route.contains(move)) &&
      proof.originSquare.exists(square => story.anchor.contains(square)) &&
      proof.destinationSquare.exists(square => story.target.contains(square)) &&
      proof.openedFileIsOriginFile &&
      proof.openedFile == story.openedFile &&
      proof.openedFile == proof.originFile

  private def promotionThreatProofBindsStory(story: Story, proof: PromotionThreatProof) =
    proof.side == story.side &&
      proof.rivalSide == story.rival &&
      proof.creatingMove.exists(move => story.route.contains(move)) &&
      proof.pawnBefore.exists(piece => story.anchor.contains(piece.square)) &&
      proof.promotionSquare.exists(square => story.target.contains(square))

  private def promotionProofBindsStory(story: Story, proof: PromotionProof) =
    proof.side == story.side &&
      proof.rivalSide == story.rival &&
      proof.promotionMove.exists(move => story.route.contains(move)) &&
      proof.originSquare.exists(square => story.anchor.contains(square)) &&
      proof.promotionSquare.exists(square => story.target.contains(square)) &&
      proof.promotedPiece.exists(piece => story.target.contains(piece.square))

  private def checkGivenProofBindsStory(story: Story, proof: CheckGivenProof) =
    proof.checkingSide == story.side &&
      proof.rivalSide == story.rival &&
      proof.checkMove.exists(move => story.route.contains(move)) &&
      proof.originSquare.exists(square => story.anchor.contains(square)) &&
      proof.rivalKingSquareAfter.exists(square => story.target.contains(square))

  private def checkEscapedProofBindsStory(story: Story, proof: CheckEscapedProof) =
    proof.escapingSide == story.side &&
      proof.rivalSide == story.rival &&
      proof.escapeMove.exists(move => story.route.contains(move)) &&
      proof.beforeKingSquare.nonEmpty &&
      proof.originSquare.exists(square => story.anchor.contains(square)) &&
      proof.afterKingSquare.exists(square => story.target.contains(square))

  private def checkmateProofBindsStory(story: Story, proof: CheckmateProof) =
    proof.matingSide == story.side &&
      proof.rivalSide == story.rival &&
      proof.mateMove.exists(move => story.route.contains(move)) &&
      proof.originSquare.exists(square => story.anchor.contains(square)) &&
      proof.rivalKingSquareAfter.exists(square => story.target.contains(square))

  private def stalemateProofBindsStory(story: Story, proof: StalemateProof) =
    proof.stalematingSide == story.side &&
      proof.rivalSide == story.rival &&
      proof.stalemateMove.exists(move => story.route.contains(move)) &&
      proof.originSquare.exists(square => story.anchor.contains(square)) &&
      proof.rivalKingSquareAfter.exists(square => story.target.contains(square))

  private def multiTargetRelationProven(proof: MultiTargetProof) =
    proof.targetSquares.size == 2 &&
      proof.targetValues.size == 2 &&
      proof.attackedTargetSquaresAfterMove == proof.targetSquares &&
      proof.replyMap.map(_.target) == proof.targets &&
      proof.replyMap.forall(!_.savedByOneReply)

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
      case (Side.White, Side.Both) => true
      case (Side.Black, Side.Both) => true
      case (Side.Both, Side.White) => true
      case (Side.Both, Side.Black) => true
      case _ => false

  private def tag(story: Story) =
    story.plan.map(_.ordinal).orElse(story.tactic.map(_.ordinal)).getOrElse(Int.MaxValue)

  private def familyKey(story: Story) =
    story.scene.ordinal * 100 + tag(story)

  private def squareKey(square: Option[Square]) = square.fold(0)(_.index + 1)

  private def squarePairKey(first: Option[Square], second: Option[Square]) =
    squareKey(first) * 65 + squareKey(second)

  private def routeKey(route: Option[Line]) =
    route.fold(0)(line => (line.from.index + 1) * 65 + line.to.index + 1)

  private def role(row: Row, index: Int) =
    if row.leadCandidate && index == 0 then Role.Lead
    else if row.leadCandidate then Role.Support
    else if row.blocked then Role.Blocked
    else Role.Context

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
  case TacticSkewer

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
    private[commentary] val skewerProof: Option[SkewerProof] = None
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
  val Size = 161
  val SceneSlots = 16
  val PlanSlots = 32
  val TacticSlots = 25
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

object Verdict:
  val Size = 97
  val FinalSlots = 8
  val SceneSlots = 16
  val PlanSlots = 32
  val TacticSlots = 25
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
    val rows =
      stories.map: story =>
        val leadCandidate = leadByStoryRules(story, stories)
        val proofFailures = story.proofFailures
        Row(story, story.proof.publicStrength, leadCandidate, blockedByStoryRules(story, proofFailures), proofFailures)
    rows
      .sortBy(row =>
        (
          roleSortPriority(row) * 2 + engineCapPriority(row.story),
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
          selected = true
        )

  private case class Row(
      story: Story,
      strength: Double,
      leadCandidate: Boolean,
      blocked: Boolean,
      proofFailures: Vector[BoardFacts.MissingEvidence]
  )

  private def roleSortPriority(row: Row) =
    if row.leadCandidate then 0 else if row.blocked then 2 else 1

  private def interactionPriority(story: Story, stories: Vector[Story]) =
    if materialOverlapsHanging(story, stories) then 1
    else if defenseCollidesWithMaterial(story, stories) then 1
    else if defenseOverlapsImmediateMaterialGain(story, stories) then 1
    else if lineDefenderOverlapsOpenedClaimHome(story, stories) then 1
    else if discoveredAttackOverlapsDefenderContact(story, stories) then 1
    else if removeGuardOverlapsMaterial(story, stories) then 1
    else if removeGuardOverlapsHanging(story, stories) then 1
    else 0

  private def engineCapPriority(story: Story) =
    if story.engineCheck.exists(_.status == EngineCheckStatus.Caps) then 1 else 0

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
      skewerWithoutWriter(story) ||
      discoveredAttackWithoutWriter(story) ||
      invalidForkWriter(story) ||
      invalidMaterialWriter(story) ||
      invalidDefenseWriter(story) ||
      invalidDiscoveredAttackWriter(story) ||
      invalidPinWriter(story) ||
      invalidRemoveGuardWriter(story) ||
      invalidSkewerWriter(story) ||
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

  private def skewerWithoutWriter(story: Story) =
    story.writer.isEmpty &&
      story.scene == Scene.Tactic &&
      story.tactic.contains(Tactic.Skewer)

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

  private def invalidSkewerWriter(story: Story) =
    story.writer.contains(StoryWriter.TacticSkewer) &&
      !skewerWriterShape(story)

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
      case Some(StoryWriter.TacticFork)    => positiveForkWriter(story)
      case Some(StoryWriter.SceneMaterial) => positiveMaterialWriter(story)
      case Some(StoryWriter.SceneDefense)  => positiveDefenseWriter(story)
      case Some(StoryWriter.TacticDiscoveredAttack) => positiveDiscoveredAttackWriter(story)
      case Some(StoryWriter.TacticPin)     => positivePinWriter(story)
      case Some(StoryWriter.TacticRemoveGuard) => positiveRemoveGuardWriter(story)
      case Some(StoryWriter.TacticSkewer)  => positiveSkewerWriter(story)
      case _                               => false

  private def positiveHangingWriter(story: Story) =
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

  private def positiveForkWriter(story: Story) =
    story.scene == Scene.Tactic &&
    story.tactic.contains(Tactic.Fork) &&
    story.plan.isEmpty &&
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

  private def captureResultBindsStory(story: Story, result: CaptureResult) =
    result.side == story.side &&
      story.route.contains(result.captureLine) &&
      result.capturingPiece.exists(piece => story.anchor.contains(piece.square)) &&
      result.targetPiece.exists(piece => story.target.contains(piece.square))

  private def multiTargetProofBindsStory(story: Story, proof: MultiTargetProof) =
    proof.side == story.side &&
      proof.forkMove.exists(move => story.route.contains(move)) &&
      proof.attacker.exists(piece => story.anchor.contains(piece.square)) &&
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
      case (Side.White, Side.Both)  => true
      case (Side.Black, Side.Both)  => true
      case (Side.Both, Side.White)  => true
      case (Side.Both, Side.Black)  => true
      case _                        => false

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

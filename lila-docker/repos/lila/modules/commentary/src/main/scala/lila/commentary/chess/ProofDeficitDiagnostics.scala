package lila.commentary.chess

private[commentary] final case class ProofCoordinates(
    root: Option[String],
    side: Option[String],
    target: Option[String],
    anchor: Option[String],
    route: Option[String],
    rival: Option[String],
    requiredLegalLine: Option[String],
    sameRootProofSidecar: Option[String]
)

private[commentary] final case class ProofDeficitDiagnostic(
    storyIdentityLabel: String,
    leadAllowed: Boolean,
    roleReason: Option[String],
    blockedBy: Vector[String],
    boardFactsPresent: Vector[String],
    proofCoordinates: ProofCoordinates,
    missingSidecar: Vector[String],
    reason: String
):
  def story: String = storyIdentityLabel

private[commentary] object ProofDeficitDiagnostics:
  def fromVerdict(verdict: Verdict): Option[ProofDeficitDiagnostic] =
    Option.when(nonSpeakingRow(verdict)):
      val story = verdict.story
      val missing = missingEvidence(story)
      ProofDeficitDiagnostic(
        storyIdentityLabel = storyName(story),
        leadAllowed = verdict.leadAllowed,
        roleReason = roleReason(verdict),
        blockedBy = blockedBy(verdict, missing),
        boardFactsPresent = boardFactsPresent(story),
        proofCoordinates = proofCoordinates(story, missing),
        missingSidecar = missingSidecar(story, missing),
        reason =
          "Internal proof-deficit diagnostic: this row cannot produce standalone text while missing proof coordinates, row authority, or strength limits apply; diagnostics do not repair or upgrade the Story."
      )

  private def nonSpeakingRow(verdict: Verdict): Boolean =
    verdict.role != Role.Lead ||
      !verdict.leadAllowed ||
      verdict.engineStrengthLimited ||
      verdict.engineCheckStatus.contains(EngineCheckStatus.Refutes)

  private def missingEvidence(story: Story): Vector[String] =
    story.proofFailures.flatMap(_.missing).distinct

  private def roleReason(verdict: Verdict): Option[String] =
    verdict.role match
      case Role.Lead if verdict.engineStrengthLimited => Some("engine_strength_limited")
      case Role.Lead if verdict.leadAllowed => None
      case Role.Blocked => Some("blocked")
      case role => Some(s"non_lead_role:$role")

  private def blockedBy(verdict: Verdict, missing: Vector[String]): Vector[String] =
    val story = verdict.story
    val blocks =
      Vector(
        Option.when(verdict.role != Role.Lead)("non_lead_role"),
        Option.when(!verdict.leadAllowed)("lead_not_allowed"),
        Option.when(verdict.engineStrengthLimited)("engine_strength_limited"),
        Option.when(story.writer.isEmpty)("writer"),
        verdict.engineCheckStatus.collect { case EngineCheckStatus.Refutes => "engine_refutes" }
      ).flatten ++ missing.map(normalizeBlocker) ++ missingFamilySidecar(story).map(normalizeBlocker)
    blocks.distinct

  private def boardFactsPresent(story: Story): Vector[String] =
    Vector(
      Option.when(identityPresent(story))("story_identity"),
      Option.when(story.route.nonEmpty)("route"),
      Option.when(story.storyProof.failures(story).isEmpty)("story_proof"),
      Option.when(story.captureResult.nonEmpty)("capture_result"),
      Option.when(story.engineCheck.nonEmpty)("engine_check"),
      Option.when(story.multiTargetProof.nonEmpty)("multi_target_proof"),
      Option.when(story.threatProof.nonEmpty)("threat_proof"),
      Option.when(story.defenseProof.nonEmpty)("defense_proof"),
      Option.when(story.lineProof.nonEmpty)("line_proof"),
      Option.when(story.pinProof.nonEmpty)("pin_proof"),
      Option.when(story.removeGuardProof.nonEmpty)("remove_guard_proof"),
      Option.when(story.overloadProof.nonEmpty)("overload_proof"),
      Option.when(story.skewerProof.nonEmpty)("skewer_proof"),
      Option.when(story.interferenceProof.nonEmpty)("interference_proof"),
      Option.when(story.queenHitProof.nonEmpty)("queen_hit_proof"),
      Option.when(story.loosePieceProof.nonEmpty)("loose_piece_proof"),
      Option.when(story.pawnAdvanceProof.nonEmpty)("pawn_advance_proof"),
      Option.when(story.pawnStopProof.nonEmpty)("pawn_stop_proof"),
      Option.when(story.pawnBreakProof.nonEmpty)("pawn_break_proof"),
      Option.when(story.pawnBlockProof.nonEmpty)("pawn_block_proof"),
      Option.when(story.pawnCaptureProof.nonEmpty)("pawn_capture_proof"),
      Option.when(story.passedPawnCreatedProof.nonEmpty)("passed_pawn_created_proof"),
      Option.when(story.fileOpenedProof.nonEmpty)("file_opened_proof"),
      Option.when(story.promotionThreatProof.nonEmpty)("promotion_threat_proof"),
      Option.when(story.promotionProof.nonEmpty)("promotion_proof"),
      Option.when(story.checkGivenProof.nonEmpty)("check_given_proof"),
      Option.when(story.checkEscapedProof.nonEmpty)("check_escaped_proof"),
      Option.when(story.checkmateProof.nonEmpty)("checkmate_proof"),
      Option.when(story.stalemateProof.nonEmpty)("stalemate_proof")
    ).flatten

  private def proofCoordinates(story: Story, missing: Vector[String]): ProofCoordinates =
    ProofCoordinates(
      root = Option.when(!missing.contains("same-board proof"))("same-board story proof"),
      side = Option.when(story.side == Side.White || story.side == Side.Black)(sideText(story.side)),
      target = story.target.map(squareText),
      anchor = story.anchor.map(squareText),
      route = story.route.map(lineText),
      rival = Option.when(story.rival == Side.White || story.rival == Side.Black)(sideText(story.rival)),
      requiredLegalLine = if missing.contains("legal line") then None else story.route.map(lineText),
      sameRootProofSidecar =
        Option.when(!missing.contains("same-board proof") && !missing.contains("legal line"))("StoryProof")
    )

  private def missingSidecar(story: Story, missing: Vector[String]): Vector[String] =
    val storyProofMissing = missing.map:
      case "legal line" => "StoryProof legal line"
      case "same-board proof" => "StoryProof same-board proof"
      case other => s"Story identity $other"
    val writerMissing = Option.when(story.writer.isEmpty)("Story writer").toVector
    (storyProofMissing ++ writerMissing ++ missingFamilySidecar(story)).distinct

  private def missingFamilySidecar(story: Story): Vector[String] =
    story.writer.flatMap:
      case StoryWriter.TacticHanging => Option.when(story.captureResult.isEmpty)("CaptureResult")
      case StoryWriter.TacticFork => Option.when(story.multiTargetProof.isEmpty)("MultiTargetProof")
      case StoryWriter.SceneMaterial => Option.when(story.captureResult.isEmpty)("CaptureResult")
      case StoryWriter.SceneDefense => Option.when(story.defenseProof.isEmpty)("DefenseProof")
      case StoryWriter.TacticDiscoveredAttack => Option.when(story.lineProof.isEmpty)("LineProof")
      case StoryWriter.TacticPin => Option.when(story.pinProof.isEmpty)("PinProof")
      case StoryWriter.TacticRemoveGuard => Option.when(story.removeGuardProof.isEmpty)("RemoveGuardProof")
      case StoryWriter.TacticOverload => Option.when(story.overloadProof.isEmpty)("OverloadProof")
      case StoryWriter.TacticDeflect => Option.when(story.deflectProof.isEmpty)("DeflectProof")
      case StoryWriter.TacticDecoy => Option.when(story.decoyProof.isEmpty)("DecoyProof")
      case StoryWriter.TacticInterference => Option.when(story.interferenceProof.isEmpty)("InterferenceProof")
      case StoryWriter.TacticSkewer => Option.when(story.skewerProof.isEmpty)("SkewerProof")
      case StoryWriter.TacticQueenHit => Option.when(story.queenHitProof.isEmpty)("QueenHitProof")
      case StoryWriter.TacticLoose => Option.when(story.loosePieceProof.isEmpty)("LoosePieceProof")
      case StoryWriter.TacticTrap => Option.when(story.trapProof.isEmpty)("TrapProof")
      case StoryWriter.ScenePawnAdvance => Option.when(story.pawnAdvanceProof.isEmpty)("PawnAdvanceProof")
      case StoryWriter.ScenePawnStop => Option.when(story.pawnStopProof.isEmpty)("PawnStopProof")
      case StoryWriter.ScenePawnBreak => Option.when(story.pawnBreakProof.isEmpty)("PawnBreakProof")
      case StoryWriter.ScenePawnBlock => Option.when(story.pawnBlockProof.isEmpty)("PawnBlockProof")
      case StoryWriter.ScenePromotionThreat => Option.when(story.promotionThreatProof.isEmpty)("PromotionThreatProof")
      case StoryWriter.ScenePromotion => Option.when(story.promotionProof.isEmpty)("PromotionProof")
      case StoryWriter.ScenePawnCapture => Option.when(story.pawnCaptureProof.isEmpty)("PawnCaptureProof")
      case StoryWriter.ScenePassedPawnCreated =>
        Option.when(story.passedPawnCreatedProof.isEmpty)("PassedPawnCreatedProof")
      case StoryWriter.SceneFileOpened => Option.when(story.fileOpenedProof.isEmpty)("FileOpenedProof")
      case StoryWriter.SceneCheckGiven => Option.when(story.checkGivenProof.isEmpty)("CheckGivenProof")
      case StoryWriter.SceneCheckEscaped => Option.when(story.checkEscapedProof.isEmpty)("CheckEscapedProof")
      case StoryWriter.SceneCheckmate => Option.when(story.checkmateProof.isEmpty)("CheckmateProof")
      case StoryWriter.SceneStalemate => Option.when(story.stalemateProof.isEmpty)("StalemateProof")
    .toVector

  private def identityPresent(story: Story): Boolean =
    (story.side == Side.White || story.side == Side.Black) &&
      story.target.nonEmpty &&
      story.anchor.nonEmpty &&
      story.route.nonEmpty &&
      (story.rival == Side.White || story.rival == Side.Black)

  private def storyName(story: Story): String =
    story.tactic.map(tactic => s"${story.scene}.${tactic}").orElse(story.plan.map(plan => s"${story.scene}.${plan}")).getOrElse(story.scene.toString)

  private def normalizeBlocker(value: String): String =
    value.toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9]+", "_").stripPrefix("_").stripSuffix("_")

  private def sideText(side: Side): String =
    side match
      case Side.White => "White"
      case Side.Black => "Black"
      case Side.Both => "Both"
      case Side.None => "None"

  private def squareText(square: Square): String =
    s"${('a' + square.file).toChar}${square.rank + 1}"

  private def lineText(line: Line): String =
    s"${squareText(line.from)}-${squareText(line.to)}"

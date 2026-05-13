package lila.commentary.chess

import java.util.Locale

private[commentary] final case class NarrationSmokeCheck(
    accepted: Boolean,
    violations: Vector[String]
)

private[commentary] object LlmNarrationSmoke:
  def mockNarrate(plan: ExplanationPlan, rendered: Option[RenderedLine]): Option[String] =
    rendered.flatMap(mockNarrate(plan, _))

  def mockNarrate(plan: ExplanationPlan, rendered: RenderedLine): Option[String] =
    Option.when(inputMatches(plan, rendered) && check(plan, rendered, rendered.text).accepted)(rendered.text)

  def codexCliPrompt(plan: ExplanationPlan, rendered: RenderedLine): Option[String] =
    Option.when(inputMatches(plan, rendered)):
      s"""You are a chess narration smoke-test model.
         |instruction: Rephrase only. Do not add chess facts.
         |Use only the supplied fields. Keep the output no stronger than renderedText.
         |Do not add a move, line, tactic, plan, cause, or stronger claim.
         |
         |renderedText: ${rendered.text}
         |claimKey: ${rendered.claimKey}
         |strength: ${rendered.strength}
         |forbiddenWording: ${promptForbiddenLabels(plan).mkString(", ")}
         |
         |Return JSON only: {"text":"..."}""".stripMargin

  def check(plan: ExplanationPlan, rendered: RenderedLine, output: String): NarrationSmokeCheck =
    val violations =
      Vector(
        Option.when(!inputMatches(plan, rendered))("input_mismatch"),
        Option.when(mentionsRawInput(output))("raw_input"),
        Option.when(violatesForbiddenWording(output, plan))("forbidden_wording"),
        Option.when(usesCoordinateMoveText(output))("non_san_move_text"),
        Option.when(addsNewMoveOrLine(output, plan, rendered))("new_move_or_line"),
        Option.when(addsNewTacticOrPlan(output, plan))("new_tactic_or_plan"),
        Option.when(addsNewCauseOrEvaluation(output))("new_cause_or_evaluation"),
        Option.when(addsOpenFileReason(output, plan))("new_cause_or_evaluation"),
        Option.when(mentionsEngine(output) || mentionsEngineEvalValue(output))("engine_mention"),
        Option.when(strongerThanRendered(output, plan, rendered))("stronger_claim")
      ).flatten.distinct
    NarrationSmokeCheck(violations.isEmpty, violations)

  private def inputMatches(plan: ExplanationPlan, rendered: RenderedLine): Boolean =
    plan.role == Role.Lead &&
      !plan.debugOnly &&
      claimMatchesPlan(plan) &&
      plan.allowedClaim.exists(_.key == rendered.claimKey) &&
      rendered.strength == plan.strength.key &&
      rendered.forbiddenCheckPassed &&
      plan.evidenceLine.nonEmpty &&
      plan.route.nonEmpty &&
      plan.routeSan.nonEmpty &&
      plan.evidenceLine == plan.route

  private def claimMatchesPlan(plan: ExplanationPlan): Boolean =
    plan.scene match
      case Scene.Tactic =>
        plan.tactic.exists(tactic => claimMatchesTactic(plan, tactic))
      case Scene.Material =>
        plan.tactic.isEmpty && plan.allowedClaim.contains(ExplanationClaim.MaterialBalanceChanges)
      case Scene.Defense =>
        plan.tactic.isEmpty && plan.allowedClaim.exists(ExplanationClaim.DefenseAllowed.contains)
      case Scene.PawnAdvance =>
        plan.tactic.isEmpty && plan.allowedClaim.contains(ExplanationClaim.AdvancesPassedPawn)
      case Scene.PawnStop =>
        plan.tactic.isEmpty && plan.allowedClaim.contains(ExplanationClaim.StopsPassedPawnNextAdvance)
      case Scene.PawnBreak =>
        plan.tactic.isEmpty && plan.allowedClaim.contains(ExplanationClaim.ChallengesPawnDirectly)
      case Scene.PawnCapture =>
        plan.tactic.isEmpty && plan.allowedClaim.contains(ExplanationClaim.CapturesPawn)
      case Scene.PassedPawnCreated =>
        plan.tactic.isEmpty && plan.allowedClaim.contains(ExplanationClaim.CreatesPassedPawn)
      case Scene.FileOpened =>
        plan.tactic.isEmpty && plan.allowedClaim.contains(ExplanationClaim.OpensFile)
      case Scene.PawnBlock =>
        plan.tactic.isEmpty && plan.allowedClaim.contains(ExplanationClaim.BlocksPawn)
      case Scene.CheckGiven =>
        plan.tactic.isEmpty && plan.allowedClaim.contains(ExplanationClaim.GivesCheck)
      case Scene.CheckEscaped =>
        plan.tactic.isEmpty && plan.allowedClaim.contains(ExplanationClaim.EscapesCheck)
      case Scene.Checkmate =>
        plan.tactic.isEmpty && plan.allowedClaim.contains(ExplanationClaim.Checkmates)
      case Scene.Stalemate =>
        plan.tactic.isEmpty && plan.allowedClaim.contains(ExplanationClaim.Stalemates)
      case Scene.PromotionThreat =>
        plan.tactic.isEmpty && plan.allowedClaim.contains(ExplanationClaim.CreatesPromotionThreat)
      case Scene.Promotion =>
        plan.tactic.isEmpty && plan.allowedClaim.contains(ExplanationClaim.PromotesPawn)
      case _ =>
        false

  private def promptForbiddenLabels(plan: ExplanationPlan): Vector[String] =
    val forbidden =
      if plan.scene == Scene.Tactic &&
        plan.tactic.contains(Tactic.Overload) &&
        plan.allowedClaim.contains(ExplanationClaim.OverloadsDefender)
      then
        plan.forbiddenWording.filterNot: forbidden =>
          forbidden == ForbiddenWording.EngineSays ||
          forbidden == ForbiddenWording.EvalNumber ||
            forbidden == ForbiddenWording.RawPv
      else plan.forbiddenWording
    val labels = forbidden.map(forbiddenLabel)
    if isOverloadPlan(plan) then (labels ++ OverloadExtraForbiddenPhrases).distinct else labels

  private def claimMatchesTactic(plan: ExplanationPlan, tactic: Tactic): Boolean =
    tactic match
      case Tactic.Hanging => plan.allowedClaim.contains(ExplanationClaim.CanWinPiece)
      case Tactic.Fork => plan.allowedClaim.contains(ExplanationClaim.ForksTwoTargets)
      case Tactic.DiscoveredAttack => plan.allowedClaim.contains(ExplanationClaim.RevealsAttackOnPiece)
      case Tactic.Pin => plan.allowedClaim.contains(ExplanationClaim.PinsPiece)
      case Tactic.RemoveGuard => plan.allowedClaim.contains(ExplanationClaim.RemovesDefender)
      case Tactic.Overload => plan.allowedClaim.contains(ExplanationClaim.OverloadsDefender)
      case Tactic.Skewer => plan.allowedClaim.contains(ExplanationClaim.SkewersPieceToPiece)
      case Tactic.QueenHit => plan.allowedClaim.contains(ExplanationClaim.AttacksQueen)
      case Tactic.Loose => plan.allowedClaim.contains(ExplanationClaim.AttacksLoosePiece)
      case Tactic.Trap => plan.allowedClaim.contains(ExplanationClaim.TrapsPiece)
      case Tactic.Decoy => plan.allowedClaim.contains(ExplanationClaim.DecoysPiece)
      case Tactic.Deflect => plan.allowedClaim.contains(ExplanationClaim.DeflectsDefender)
      case _ => false

  private def violatesForbiddenWording(text: String, plan: ExplanationPlan): Boolean =
    val normalized = normalize(text)
    val forbiddenPhrases = plan.forbiddenWording.flatMap(forbiddenMeaning)
    forbiddenPhrases.exists(phrase => containsPhrase(normalized, phrase)) ||
    publicForbiddenPhrases.exists(phrase => containsPhrase(normalized, phrase)) ||
    stage4TempoForbiddenPhrases.exists(phrase => containsPhrase(normalized, phrase)) ||
    defenderManipulationForbiddenPhrases(normalized, plan) ||
    deflectForbiddenPhrases(normalized, plan) ||
    overloadForbiddenPhrases(normalized, plan) ||
    looseForbiddenPhrases(normalized, plan) ||
    !materialWinAllowed(normalized, plan)

  private def strongerThanRendered(text: String, plan: ExplanationPlan, rendered: RenderedLine): Boolean =
    val normalized = normalize(text)
    val renderedNormalized = normalize(rendered.text)
    val strongPhrases =
      Vector(
        "free piece",
        "blunder",
        "winning",
        "winning position",
        "decisive",
        "decisively",
        "forced",
        "best move",
        "only move",
        "engine says",
        "hanging",
        "hanging piece",
        "target is hanging",
        "engine approved",
        "no counterplay",
        "best defense",
        "refutes attack",
        "refutes the attack",
        "stops counterplay",
        "stops all counterplay",
        "king is safe",
        "king safety",
        "mate defense",
        "mate is stopped",
        "gains tempo",
        "gain tempo",
        "wins tempo",
        "tempo"
      )
    strongPhrases.exists(phrase => containsPhrase(normalized, phrase)) ||
    (!plan.allowedClaim.contains(ExplanationClaim.CanWinPiece) && materialWinPhrases.exists(
      containsPhrase(normalized, _)
    )) ||
    (!plan.allowedClaim.contains(ExplanationClaim.CanWinPiece) && pieceWinPhrases.exists(
      containsPhrase(normalized, _)
    )) ||
    (containsPhrase(normalized, "wins material") && !containsPhrase(renderedNormalized, "wins material")) ||
    addsPieceIdentityAbsentFromRendered(normalized, renderedNormalized, plan)

  private def addsNewMoveOrLine(text: String, plan: ExplanationPlan, rendered: RenderedLine): Boolean =
    val normalized = normalize(text)
    val mentionsNewVariation =
      Vector("new variation", "variation").exists(phrase => containsPhrase(normalized, phrase))
    val allowedMoves =
      moveTokens(rendered.text).toSet ++
        plan.routeSan.map(_.toLowerCase(Locale.ROOT)).toSet
    val allowedSquares =
      squareTokens(rendered.text).toSet ++
        plan.target.map(squareText).toSet ++
        plan.anchor.map(squareText).toSet ++
        plan.route.toVector.flatMap(line => Vector(squareText(line.from), squareText(line.to))) ++
        plan.evidenceLine.toVector.flatMap(line => Vector(squareText(line.from), squareText(line.to)))
    mentionsNewVariation ||
    moveTokens(text).exists(token => !allowedMoves.contains(token)) ||
    squareTokens(text).exists(token => !allowedSquares.contains(token))

  private def addsNewTacticOrPlan(text: String, plan: ExplanationPlan): Boolean =
    val normalized = normalize(text)
    val allowed =
      plan.tactic match
        case Some(Tactic.Fork) =>
          Set("fork", "forks")
        case Some(Tactic.DiscoveredAttack) =>
          Set("attack", "reveals attack", "reveals an attack", "discovered attack")
        case Some(Tactic.Pin) =>
          Set("pin", "pins")
        case Some(Tactic.RemoveGuard) =>
          Set("removes defender", "removes the defender", "remove guard", "removes guard", "guard removal")
        case Some(Tactic.Overload) =>
          Set("overload", "overloads", "overloaded", "overloading")
        case Some(Tactic.Skewer) =>
          Set("skewer", "skewers", "skewered")
        case Some(Tactic.QueenHit) =>
          Set("attack", "attacks", "attacks queen", "attacks the queen")
        case Some(Tactic.Loose) =>
          Set("attack", "attacks", "attacks undefended piece", "attacks the undefended piece")
        case Some(Tactic.Trap) =>
          Set("trap", "traps", "traps piece", "traps the piece")
        case Some(Tactic.Decoy) =>
          Set("decoy", "decoys", "decoyed", "decoys piece", "decoys the piece")
        case Some(Tactic.Deflect) =>
          Set("deflect", "deflects", "deflected", "deflects defender", "deflects the defender")
        case _ =>
          if plan.scene == Scene.PawnAdvance then
            Set("passed pawn", "advances passed pawn", "advances the passed pawn")
          else if plan.scene == Scene.PawnStop then
            Set(
              "passed pawn",
              "stops passed pawn",
              "stops the passed pawn",
              "next advance",
              "stops the passed pawn next advance"
            )
          else if plan.scene == Scene.PromotionThreat then
            Set(
              "promotion",
              "promote",
              "promotion threat",
              "next move promotion threat",
              "next-move promotion threat",
              "threatens to promote next",
              "creates promotion threat",
              "creates a promotion threat",
              "creates a next move promotion threat",
              "creates a next-move promotion threat"
            )
          else if plan.scene == Scene.PawnBreak then
            Set(
              "directly challenges pawn",
              "directly challenges the pawn",
              "challenges pawn",
              "challenges the pawn"
            )
          else if plan.scene == Scene.PawnCapture then
            Set(
              "captures pawn",
              "captures the pawn",
              "pawn captures pawn",
              "takes pawn",
              "takes the pawn"
            )
          else if plan.scene == Scene.PassedPawnCreated then
            Set(
              "passed pawn",
              "creates passed pawn",
              "creates a passed pawn",
              "makes passed pawn",
              "makes a passed pawn"
            )
          else if plan.scene == Scene.FileOpened then
            Set(
              "open file",
              "opens file",
              "opens the file",
              "opens e file",
              "opens the e file",
              "leaves file open",
              "leaves the file open",
              "leaves the a file open",
              "leaves the b file open",
              "leaves the c file open",
              "leaves the d file open",
              "leaves the e file open",
              "leaves the f file open",
              "leaves the g file open",
              "leaves the h file open"
            )
          else if plan.scene == Scene.Promotion then
            Set(
              "promotes",
              "promotes pawn",
              "promotes the pawn",
              "pawn promotes"
            )
          else if plan.scene == Scene.Defense then
            Set("defend", "defends", "defended", "defends piece", "defends the piece")
          else if plan.scene == Scene.Checkmate then
            Set("checkmate", "checkmates")
          else if plan.scene == Scene.Stalemate then
            Set("stalemate", "stalemates")
          else Set.empty[String]
    val tacticOrPlanPhrases = Vector(
      "fork",
      "forks",
      "reveals attack",
      "reveals an attack",
      "discovered attack",
      "pin",
      "pins",
      "removes defender",
      "removes the defender",
      "remove guard",
      "removes guard",
      "guard removal",
      "line tactic",
      "line tactics",
      "creates pressure",
      "pressure",
      "takes initiative",
      "initiative",
      "deflection",
      "deflect",
      "deflects",
      "deflected",
      "decoy",
      "decoys",
      "decoyed",
      "overload",
      "overloads",
      "overloaded",
      "overloading",
      "skewer",
      "skewers",
      "skewered",
      "x ray",
      "xray",
      "trap",
      "traps",
      "trapped",
      "attack",
      "attacks",
      "starts attack",
      "starts an attack",
      "mate",
      "checkmate",
      "checkmates",
      "mate net",
      "plan",
      "strategy",
      "strategic",
      "king safety",
      "counterplay",
      "convert",
      "converts",
      "conversion",
      "promote",
      "promotion",
      "promotes",
      "will promote",
      "queens",
      "promotes next",
      "promotion stop",
      "permanent stop",
      "stops passed pawn",
      "stops the passed pawn",
      "stops pawn advance",
      "stops the pawn advance",
      "next advance",
      "draw",
      "draws",
      "drawn endgame",
      "draws endgame",
      "draws the endgame",
      "wins endgame",
      "wins the endgame",
      "tablebase",
      "clear path",
      "cannot be stopped",
      "can't be stopped",
      "can not be stopped",
      "pawn break",
      "breaks pawn",
      "breaks the pawn",
      "pawn race",
      "race",
      "king route",
      "opposition",
      "outpost",
      "file control",
      "open file",
      "opens file",
      "opens the file",
      "opens e file",
      "opens the e file",
      "creates open file",
      "creates an open file",
      "defense",
      "defend",
      "defends",
      "defended",
      "defends piece",
      "defends the piece"
    )
    tacticOrPlanPhrases.exists: phrase =>
      !allowed.contains(phrase) && containsPhrase(normalized, phrase)

  private def addsNewCauseOrEvaluation(text: String): Boolean =
    val normalized = normalize(text)
    Vector(
      "because",
      "therefore",
      "so that",
      "which means",
      "advantage",
      "better",
      "worse",
      "evaluation",
      "eval",
      "score"
    ).exists(phrase => containsPhrase(normalized, phrase))

  private def addsOpenFileReason(text: String, plan: ExplanationPlan): Boolean =
    plan.scene == Scene.FileOpened &&
      {
        val normalized = normalize(text)
        Vector(
          "for later play",
          "for future play",
          "later play",
          "future play",
          "improves position",
          "improves the position",
          "improve position",
          "improve the position",
          "file matters",
          "the file matters",
          "why the file matters",
          "matters for"
        ).exists(phrase => containsPhrase(normalized, phrase))
      }

  private def mentionsEngine(text: String): Boolean =
    val normalized = normalize(text)
    Vector(
      "engine",
      "stockfish",
      "eval",
      "evaluation",
      "pv",
      "principal variation",
      "raw pv",
      "centipawn",
      "centipawns"
    ).exists(phrase => containsPhrase(normalized, phrase))

  private def mentionsEngineEvalValue(text: String): Boolean =
    EvalNumberPattern.findFirstIn(text).nonEmpty ||
      CentipawnPattern.findFirstIn(text).nonEmpty

  private val EvalNumberPattern =
    """(?<![A-Za-z0-9])[+-](?:\d+(?:\.\d+)?|\.\d+)(?![A-Za-z0-9])""".r

  private val CentipawnPattern =
    """(?i)\b\d+(?:\.\d+)?\s*(?:cp|centipawns?)\b""".r

  private val materialWinPhrases =
    Vector(
      "wins material",
      "win material",
      "winning material",
      "material win",
      "gains material",
      "gain material",
      "gaining material",
      "material gain",
      "material gains"
    )

  private val pieceWinPhrases =
    Vector(
      "wins piece",
      "wins a piece",
      "wins the piece",
      "win piece",
      "win a piece",
      "win the piece"
    )

  private val publicForbiddenPhrases =
    Vector("best move", "only move", "forced", "forces", "forced line", "by force")

  private val stage4TempoForbiddenPhrases =
    Vector(
      "tempo",
      "gains tempo",
      "wins tempo",
      "with tempo",
      "gains time",
      "free move",
      "queen must move",
      "forces the queen",
      "forced response",
      "initiative",
      "pressure",
      "keeps the move",
      "keeps control"
    )

  private def defenderManipulationForbiddenPhrases(normalized: String, plan: ExplanationPlan): Boolean =
    isOpenedDefenderSpeechPlan(plan) &&
      (
        defenderClosedPhrases.exists(phrase => containsPhrase(normalized, phrase)) ||
          (!isRemoveGuardPlan(plan) && removeGuardPhrases.exists(phrase => containsPhrase(normalized, phrase))) ||
          (!isOverloadPlan(plan) && overloadPhrases.exists(phrase => containsPhrase(normalized, phrase))) ||
          (!isDeflectPlan(plan) && deflectPhrases.exists(phrase => containsPhrase(normalized, phrase))) ||
          (!isTrapPlan(plan) && trapPhrases.exists(phrase => containsPhrase(normalized, phrase)))
      )

  private def isOpenedDefenderSpeechPlan(plan: ExplanationPlan): Boolean =
    isRemoveGuardPlan(plan) || isOverloadPlan(plan) || isDeflectPlan(plan) || isTrapPlan(plan)

  private def isRemoveGuardPlan(plan: ExplanationPlan): Boolean =
    plan.scene == Scene.Tactic &&
      plan.tactic.contains(Tactic.RemoveGuard) &&
      plan.allowedClaim.contains(ExplanationClaim.RemovesDefender)

  private def isTrapPlan(plan: ExplanationPlan): Boolean =
    plan.scene == Scene.Tactic &&
      plan.tactic.contains(Tactic.Trap) &&
      plan.allowedClaim.contains(ExplanationClaim.TrapsPiece)

  private val defenderClosedPhrases =
    Vector(
      "decoy",
      "decoys",
      "decoyed",
      "lure",
      "lures",
      "lured",
      "attract",
      "attracts",
      "attracted",
      "interference",
      "interferes",
      "interfered",
      "blocks line",
      "blocks the line",
      "blocked line",
      "blocked the line"
    )

  private val removeGuardPhrases =
    Vector(
      "remove defender",
      "remove the defender",
      "removes defender",
      "removes the defender",
      "removed defender",
      "removed the defender",
      "defender is removed",
      "defender was removed",
      "the defender is removed",
      "the defender was removed"
    )

  private val overloadPhrases =
    Vector(
      "overload",
      "overloads",
      "overloaded",
      "overloading",
      "overloads defender",
      "overloads the defender",
      "overloaded defender",
      "overloaded the defender"
    )

  private val deflectPhrases =
    Vector(
      "deflect",
      "deflects",
      "deflected",
      "deflecting",
      "deflects defender",
      "deflects the defender",
      "deflected defender",
      "deflected the defender"
    )

  private val trapPhrases =
    Vector(
      "trap",
      "traps",
      "trapped",
      "trapping",
      "traps piece",
      "traps the piece",
      "trapped piece",
      "trapped the piece"
    )

  private val OverloadExtraForbiddenPhrases =
    Vector(
      "wins material",
      "wins",
      "win",
      "wins the game",
      "win the game",
      "wins by overload",
      "win by overload",
      "wins a piece",
      "wins the queen",
      "forced",
      "forcing",
      "forced choice",
      "only move",
      "only",
      "only defensive move",
      "best move",
      "best",
      "best defensive move",
      "decisive",
      "winning",
      "no counterplay",
      "deflects",
      "decoys",
      "removes the defender",
      "loose",
      "loose target",
      "target loose",
      "target is loose",
      "makes the target loose",
      "cannot defend everything",
      "cannot save both",
      "can't save both",
      "can not save both",
      "cannot satisfy both",
      "has to choose",
      "defender has to choose",
      "must choose"
    )

  private def overloadForbiddenPhrases(normalized: String, plan: ExplanationPlan): Boolean =
    isOverloadPlan(plan) &&
      OverloadExtraForbiddenPhrases.exists(phrase => containsPhrase(normalized, phrase))

  private def isOverloadPlan(plan: ExplanationPlan): Boolean =
    plan.scene == Scene.Tactic &&
      plan.tactic.contains(Tactic.Overload) &&
      plan.allowedClaim.contains(ExplanationClaim.OverloadsDefender)

  private def deflectForbiddenPhrases(normalized: String, plan: ExplanationPlan): Boolean =
    isDeflectPlan(plan) &&
      DeflectExtraForbiddenPhrases.exists(phrase => containsPhrase(normalized, phrase))

  private def isDeflectPlan(plan: ExplanationPlan): Boolean =
    plan.scene == Scene.Tactic &&
      plan.tactic.contains(Tactic.Deflect) &&
      plan.allowedClaim.contains(ExplanationClaim.DeflectsDefender)

  private val DeflectExtraForbiddenPhrases =
    Vector(
      "wins material",
      "wins piece",
      "wins a piece",
      "wins the piece",
      "forced",
      "forcing",
      "forced reply",
      "forces reply",
      "only",
      "only move",
      "best",
      "best move",
      "no defense",
      "no defence",
      "no counterplay",
      "removes defender",
      "removes the defender",
      "overloads defender",
      "overloads the defender",
      "traps defender",
      "traps the defender",
      "decoys",
      "decoys the defender",
      "engine line",
      "reply line",
      "rival reply",
      "raw reply",
      "reply moves",
      "reply moved",
      "reply move",
      "moves the bishop",
      "bishop moves",
      "moves away from",
      "moves off",
      "after e6g4",
      "e6g4",
      "e6 g4",
      "e6-g4"
    )

  private val pieceIdentityPhrases =
    Vector("queen", "rook", "bishop", "knight", "pawn", "king")

  private def addsPieceIdentityAbsentFromRendered(
      normalized: String,
      renderedNormalized: String,
      plan: ExplanationPlan
  ): Boolean =
    val allowedByClaim =
      if plan.scene == Scene.CheckGiven && plan.allowedClaim.contains(ExplanationClaim.GivesCheck) then Set("king")
      else if plan.scene == Scene.Checkmate && plan.allowedClaim.contains(ExplanationClaim.Checkmates) then Set("king")
      else if plan.scene == Scene.Stalemate && plan.allowedClaim.contains(ExplanationClaim.Stalemates) then Set("king")
      else Set.empty[String]
    pieceIdentityPhrases.filterNot(allowedByClaim.contains).exists: phrase =>
      containsPhrase(normalized, phrase) && !containsPhrase(renderedNormalized, phrase)

  private def materialWinAllowed(normalized: String, plan: ExplanationPlan): Boolean =
    !materialWinPhrases.exists(containsPhrase(normalized, _)) ||
      plan.allowedClaim.contains(ExplanationClaim.CanWinPiece)

  private def mentionsRawInput(text: String): Boolean =
    val normalized = normalize(text)
    Vector(
      "raw story",
      "story row",
      "proof deficit",
      "proof deficit diagnostic",
      "proofdeficitdiagnostic",
      "diagnostic reason",
      "use diagnostic reason",
      "explain why blocked",
      "tell user missing proof",
      "missing proof",
      "blockedby",
      "blocked by",
      "boardfactspresent",
      "board facts present",
      "proofcoordinates",
      "proof coordinates",
      "missingsidecar",
      "missing sidecar",
      "raw multitargetproof",
      "multitargetproof",
      "raw multi target proof",
      "multi target proof",
      "pawnadvanceproof",
      "pawn advance proof",
      "pawnstopproof",
      "pawn stop proof",
      "pawnbreakproof",
      "pawn break proof",
      "pawncaptureproof",
      "pawn capture proof",
      "passedpawncreatedproof",
      "passed pawn created proof",
      "fileopenedproof",
      "file opened proof",
      "pawnblockproof",
      "pawn block proof",
      "trapproof",
      "trap proof",
      "queenhitproof",
      "queen hit proof",
      "loosepieceproof",
      "loose piece proof",
      "checkmateproof",
      "checkmate proof",
      "stalemateproof",
      "stalemate proof",
      "checkgivenproof",
      "check given proof",
      "checkescapedproof",
      "check escaped proof",
      "passedpawnobservation",
      "passed pawn observation",
      "captureresult",
      "capture result",
      "promotionthreatproof",
      "promotion threat proof",
      "promotionproof",
      "promotion proof",
      "boardfacts",
      "board facts",
      "pawnlever",
      "pawn lever",
      "pawnlever raw data",
      "pawn lever raw data",
      "enginecheck",
      "engine check",
      "engine proof failure",
      "engine proof failure wording",
      "engineeval",
      "engine eval",
      "engineline",
      "engine line",
      "raw pv",
      "prooffailures",
      "proof failures",
      "source row",
      "source rows",
      "result notation",
      "tablebase diagnostics",
      "san diagnostics",
      "san # diagnostics",
      "san mark",
      "san marks",
      "escape method",
      "escape method diagnostics",
      "escape diagnostics",
      "escape square",
      "escape-square",
      "escape square map",
      "escape-square map",
      "target move map",
      "king move",
      "checking piece capture",
      "missing evidence",
      "same-board proof",
      "same board proof",
      "exact after-board replay",
      "exact after board replay",
      "storytable",
      "story table",
      "debug relation",
      "blocked_by_engine_refute",
      "blocked by engine refute",
      "capped_same_story",
      "capped same story",
      "same_family_lower_rank",
      "same family lower rank"
    ).exists(phrase => containsPhrase(normalized, phrase))

  private def looseForbiddenPhrases(normalized: String, plan: ExplanationPlan): Boolean =
    plan.scene == Scene.Tactic &&
      plan.tactic.contains(Tactic.Loose) &&
      plan.allowedClaim.contains(ExplanationClaim.AttacksLoosePiece) &&
      Vector(
        "wins piece",
        "wins the piece",
        "win piece",
        "win the piece",
        "hanging",
        "free piece",
        "en prise",
        "underdefended",
        "overloaded",
        "overloaded defender",
        "pressure",
        "initiative",
        "tempo"
      ).exists(phrase => containsPhrase(normalized, phrase))

  private def forbiddenMeaning(forbidden: ForbiddenWording): Vector[String] =
    forbidden match
      case ForbiddenWording.FreePiece =>
        Vector("free piece")
      case ForbiddenWording.Blunder =>
        Vector("blunder")
      case ForbiddenWording.Winning =>
        Vector("winning", "winning position")
      case ForbiddenWording.Decisive =>
        Vector("decisive", "decisively")
      case ForbiddenWording.Forced =>
        Vector("forced", "forces")
      case ForbiddenWording.ForcedMove =>
        Vector("forced move")
      case ForbiddenWording.BestMove =>
        Vector("best move")
      case ForbiddenWording.OnlyMove =>
        Vector("only move")
      case ForbiddenWording.NoEscape =>
        Vector("no escape")
      case ForbiddenWording.CannotBeSaved =>
        Vector("cannot be saved")
      case ForbiddenWording.CannotRefuse =>
        Vector("cannot refuse", "can not refuse", "can't refuse")
      case ForbiddenWording.QueenTrap =>
        Vector("queen trap", "queen is trapped", "queen gets trapped", "trapped queen")
      case ForbiddenWording.EngineSays =>
        Vector(
          "engine says",
          "engine approved",
          "engine",
          "stockfish",
          "eval",
          "evaluation",
          "pv",
          "principal variation",
          "raw pv",
          "centipawn",
          "centipawns"
        )
      case ForbiddenWording.EvalNumber =>
        Vector("eval number", "evaluation number", "eval", "evaluation", "centipawn", "centipawns")
      case ForbiddenWording.RawPv =>
        Vector("raw pv", "pv", "principal variation", "engine line")
      case ForbiddenWording.PawnForkWins =>
        Vector("pawn fork wins", "pawn fork wins material", "pawn fork wins a pawn")
      case ForbiddenWording.NoCounterplay =>
        Vector("no counterplay")
      case ForbiddenWording.KingUnsafe =>
        Vector("king unsafe", "king is unsafe", "the king is unsafe", "unsafe king", "king safety")
      case ForbiddenWording.Checkmate =>
        Vector("checkmate", "mates")
      case ForbiddenWording.KingSafety =>
        Vector("king safety")
      case ForbiddenWording.Attack =>
        Vector("attack", "attacks")
      case ForbiddenWording.AttacksKing =>
        Vector("attacks king", "attacks the king", "king attack")
      case ForbiddenWording.CreatesAttack =>
        Vector("creates attack", "creates an attack")
      case ForbiddenWording.AttacksQueen =>
        Vector("attacks queen", "attacks the queen", "queen attack")
      case ForbiddenWording.GivesCheck =>
        Vector("gives check", "checks the king")
      case ForbiddenWording.EscapesCheck =>
        Vector("escapes check", "escapes the check")
      case ForbiddenWording.ForcesReply =>
        Vector("forces reply", "forces a reply", "forced reply")
      case ForbiddenWording.MateInOne =>
        Vector("mate in one", "mate-in-one")
      case ForbiddenWording.MateInN =>
        Vector("mate in", "mate-in")
      case ForbiddenWording.ForcedMate =>
        Vector("forced mate", "forces mate")
      case ForbiddenWording.EngineSaysMate =>
        Vector("engine says mate", "engine mate", "mate score")
      case ForbiddenWording.DrawsGame =>
        Vector("draws the game", "draws game", "draws", "draw")
      case ForbiddenWording.SavesGame =>
        Vector("saves the game", "saves game", "saves")
      case ForbiddenWording.ThrowsWin =>
        Vector("throws away the win", "throws win", "throws away a win", "thrown win")
      case ForbiddenWording.EngineSaysDraw =>
        Vector("engine says draw", "engine draw", "engine says")
      case ForbiddenWording.Losing =>
        Vector("losing", "losing position")
      case ForbiddenWording.KingMovedOutOfCheck =>
        Vector(
          "moves king out of check",
          "moves the king out of check",
          "moved the king out of check",
          "king moved out of check",
          "king escapes check",
          "king gets out of check"
        )
      case ForbiddenWording.CheckBlocked =>
        Vector("blocks check", "blocks the check", "blocked check", "blocked the check", "interposes", "interposition")
      case ForbiddenWording.CheckingPieceCaptured =>
        Vector(
          "captures checker",
          "captures the checker",
          "captured checker",
          "captured the checker",
          "captures the checking piece",
          "captured the checking piece",
          "takes the checking piece"
        )
      case ForbiddenWording.AvoidsMate =>
        Vector("avoids mate", "avoids checkmate", "stops mate", "prevents mate", "prevents checkmate")
      case ForbiddenWording.SafeKing =>
        Vector("safe king", "king is safe", "makes the king safe")
      case ForbiddenWording.DefenseSuccess =>
        Vector("successful defense", "defense succeeds", "defense success", "defends successfully", "defends everything", "best defense")
      case ForbiddenWording.FileControl =>
        Vector(
          "file control",
          "controls file",
          "controls the file",
          "open file",
          "half open file",
          "half-open file",
          "file half open",
          "e file half open",
          "half open e-file",
          "half-open e-file",
          "opens file",
          "opens the file",
          "opens the e-file",
          "creates an open file",
          "creates open file"
        )
      case ForbiddenWording.RookActivity =>
        Vector(
          "rook activity",
          "rook becomes active",
          "activates the rook",
          "rook lift",
          "rook uses the file",
          "rook on the file",
          "opens a route for the rook",
          "route for the rook"
        )
      case ForbiddenWording.Outpost =>
        Vector("outpost")
      case ForbiddenWording.StrategicKey =>
        Vector("strategic key", "strategic")
      case ForbiddenWording.Conversion =>
        Vector("conversion", "convert", "converts")
      case ForbiddenWording.MateNet =>
        Vector("mate net", "mating net")
      case ForbiddenWording.StrongWording =>
        Vector(
          "wins material",
          "win material",
          "winning material",
          "material win",
          "come out ahead materially",
          "winning",
          "decisive",
          "forced",
          "best move",
          "only move",
          "no counterplay"
        )
      case ForbiddenWording.WinsPawn =>
        Vector("wins pawn", "wins a pawn", "win pawn", "win a pawn")
      case ForbiddenWording.WinsMaterialByFork =>
        Vector("wins material by fork", "win material by fork", "material by fork")
      case ForbiddenWording.WinsQueen =>
        Vector("wins queen", "wins the queen", "win the queen")
      case ForbiddenWording.TrapsQueen =>
        Vector("traps queen", "traps the queen", "queen trap", "queen is trapped")
      case ForbiddenWording.QueenIsLost =>
        Vector("queen is lost", "the queen is lost", "lost queen")
      case ForbiddenWording.GainsTempo =>
        Vector("gains tempo", "gain tempo", "wins tempo", "tempo")
      case ForbiddenWording.DecisiveFork =>
        Vector("decisive fork")
      case ForbiddenWording.ForcedWin =>
        Vector("forced win", "forces a win")
      case ForbiddenWording.BestDefense =>
        Vector("best defense")
      case ForbiddenWording.RefutesAttack =>
        Vector("refutes attack", "refutes the attack")
      case ForbiddenWording.StopsCounterplay =>
        Vector("stops counterplay", "stops all counterplay")
      case ForbiddenWording.SolvesPosition =>
        Vector("solves the position", "solves position")
      case ForbiddenWording.KingSafe =>
        Vector("king safe", "king is safe")
      case ForbiddenWording.MateDefense =>
        Vector("mate defense", "stops mate", "mate is stopped")
      case ForbiddenWording.WinsMaterial =>
        Vector(
          "wins material",
          "win material",
          "winning material",
          "material win",
          "gains material",
          "gain material",
          "gaining material",
          "material gain",
          "material gains"
        )
      case ForbiddenWording.PinsPiece =>
        Vector("pins piece", "pins the piece", "pin")
      case ForbiddenWording.SkewersPiece =>
        Vector("skewers piece", "skewers the piece", "skewer")
      case ForbiddenWording.WinsRearPiece =>
        Vector(
          "wins rear piece",
          "wins the rear piece",
          "win rear piece",
          "win the rear piece",
          "wins the piece behind it",
          "win the piece behind it",
          "wins piece behind it",
          "win piece behind it"
        )
      case ForbiddenWording.FrontPieceMustMove =>
        Vector(
          "front piece must move",
          "front target must move",
          "front piece has to move",
          "front target has to move",
          "must move the front piece",
          "must move the front target"
        )
      case ForbiddenWording.CreatesPressure =>
        Vector("creates pressure", "pressure")
      case ForbiddenWording.TakesInitiative =>
        Vector("takes initiative", "initiative")
      case ForbiddenWording.MateThreat =>
        Vector(
          "mate threat",
          "mating threat",
          "creates a mating threat",
          "threatens mate",
          "mate",
          "no legal escape",
          "has no legal escape",
          "no escape"
        )
      case ForbiddenWording.CannotMove =>
        Vector("cannot move", "can't move", "can not move")
      case ForbiddenWording.TargetIsHanging =>
        Vector("target is hanging", "target hangs", "hanging target")
      case ForbiddenWording.HangingPiece =>
        Vector("hanging piece", "piece is hanging")
      case ForbiddenWording.WinsPiece =>
        Vector("wins piece", "wins a piece", "win piece", "win a piece")
      case ForbiddenWording.NoDefense =>
        Vector("no defense", "no defence")
      case ForbiddenWording.RefutesDefense =>
        Vector("refutes defense", "refutes the defense", "refutes defence", "refutes the defence")
      case ForbiddenWording.LeavesUndefended =>
        Vector("leaves it undefended", "leaves the piece undefended", "leaves target undefended")
      case ForbiddenWording.NoDefenderRemains =>
        Vector(
          "no defender remains",
          "no defenders remain",
          "no defender left",
          "no defender",
          "no defenders"
        )
      case ForbiddenWording.RemovesDefender =>
        Vector("removes defender", "removes the defender")
      case ForbiddenWording.LineTacticIdentity =>
        Vector("line tactic", "line tactics")
      case ForbiddenWording.PromotionThreat =>
        Vector(
          "promotion threat",
          "threatens promotion",
          "will promote",
          "promotes",
          "promotion",
          "queens",
          "promotes next"
        )
      case ForbiddenWording.ActualPromotion =>
        Vector(
          "actual promotion",
          "will promote",
          "promotes next",
          "queens",
          "queens next",
          "is queening",
          "guarantees promotion",
          "promotion is guaranteed"
        )
      case ForbiddenWording.UnstoppablePawn =>
        Vector(
          "unstoppable pawn",
          "unstoppable",
          "cannot be stopped",
          "can't be stopped",
          "can not be stopped"
        )
      case ForbiddenWording.WinningEndgame =>
        Vector("winning endgame", "losing endgame", "won endgame", "lost endgame", "wins", "loses")
      case ForbiddenWording.ConvertsAdvantage =>
        Vector("converts advantage", "conversion", "convert", "converts")
      case ForbiddenWording.TablebaseWin =>
        Vector("tablebase win", "wins the tablebase", "tablebase", "won endgame", "wins")
      case ForbiddenWording.MaterialGain =>
        Vector("material gain", "material gains", "gains material", "gain material")
      case ForbiddenWording.PromotionStop =>
        Vector(
          "promotion stop",
          "stops promotion",
          "stops the promotion",
          "prevents promotion",
          "prevents the pawn from queening",
          "stops the pawn from queening",
          "prevents queening"
        )
      case ForbiddenWording.PermanentStop =>
        Vector(
          "permanent stop",
          "permanently stops",
          "stops permanently",
          "stops the pawn for good",
          "cannot advance",
          "can not advance",
          "can't advance"
        )
      case ForbiddenWording.DrawsEndgame =>
        Vector(
          "draws endgame",
          "draws the endgame",
          "drawn endgame",
          "draws the position",
          "draw",
          "holds the endgame"
        )
      case ForbiddenWording.TablebaseDraw =>
        Vector(
          "tablebase draw",
          "draws the tablebase",
          "tablebase",
          "drawn endgame",
          "draw",
          "draws",
          "draws the position"
        )
      case ForbiddenWording.ConversionStopped =>
        Vector("conversion stopped", "stops conversion", "stops the conversion", "prevents conversion")
      case ForbiddenWording.KingRoute =>
        Vector("king route", "king path", "king walk")
      case ForbiddenWording.Opposition =>
        Vector("opposition", "takes the opposition", "has the opposition")
      case ForbiddenWording.PawnRace =>
        Vector("pawn race", "race")
      case ForbiddenWording.PassedPawnStrategy =>
        Vector("passed pawn strategy", "strategy", "strategic", "clear path")
      case ForbiddenWording.AdvancesPassedPawn =>
        Vector(
          "advances the passed pawn",
          "advances a passed pawn",
          "advances passed pawn",
          "pushes the passed pawn"
        )
      case ForbiddenWording.OpensPosition =>
        Vector("opens position", "opens the position", "open position", "opens lines")
      case ForbiddenWording.OpensFile =>
        Vector(
          "opens file",
          "opens the file",
          "opens the e-file",
          "creates an open file",
          "creates open file"
        )
      case ForbiddenWording.ControlsFile =>
        Vector(
          "file control",
          "takes control of the file",
          "controls file",
          "controls the file",
          "open file",
          "half open file",
          "half-open file",
          "file half open",
          "e file half open",
          "half open e-file",
          "half-open e-file",
          "controls open file",
          "controls the open file",
          "controls the e-file",
          "controls e-file"
        )
      case ForbiddenWording.UsesOpenFile =>
        Vector(
          "uses open file",
          "uses the open file",
          "uses the file",
          "uses e-file",
          "uses the e-file"
        )
      case ForbiddenWording.BreaksThrough =>
        Vector("breaks through", "breakthrough", "breaks the position")
      case ForbiddenWording.CreatesPassedPawn =>
        Vector("creates passed pawn", "creates a passed pawn", "makes a passed pawn")
      case ForbiddenWording.PawnCaptureEvent =>
        Vector("captures pawn", "captures the pawn", "pawn captures pawn", "takes pawn", "takes the pawn")
      case ForbiddenWording.StopsPassedPawn =>
        Vector("stops passed pawn", "stops the passed pawn", "passed pawn stop")
      case ForbiddenWording.StopsPawnAdvance =>
        Vector("stops pawn advance", "stops the pawn advance", "stops the pawn from advancing")
      case ForbiddenWording.ChallengesPawn =>
        Vector("challenges pawn", "challenges the pawn", "pawn challenge")
      case ForbiddenWording.FixesPawn =>
        Vector("fixes pawn", "fixes the pawn", "fixed pawn")
      case ForbiddenWording.CreatesBlockade =>
        Vector("creates blockade", "creates a blockade", "blockade")
      case ForbiddenWording.RestrictsOpponent =>
        Vector("restricts opponent", "restricts the opponent", "restricts black", "restricts white", "restriction")
      case ForbiddenWording.WeakensStructure =>
        Vector(
          "weakens structure",
          "weakens the structure",
          "weakens black structure",
          "weakens black s structure",
          "weakens white structure",
          "weakens white s structure",
          "creates a weakness",
          "creates weakness",
          "weakness",
          "structural weakness",
          "weakens the pawn",
          "weakens pawn",
          "weak pawn",
          "weak square"
        )
      case ForbiddenWording.CreatesWeakness =>
        Vector(
          "creates weakness",
          "creates a weakness",
          "creates structural weakness",
          "creates a structural weakness",
          "creates a weak square",
          "weak square"
        )
      case ForbiddenWording.WinsSpace =>
        Vector("wins space", "gains space", "space advantage")
      case ForbiddenWording.Decoy =>
        Vector("decoy", "decoys")
      case ForbiddenWording.DeflectsDefender =>
        Vector("deflects defender", "deflects the defender", "deflect", "deflected")
      case ForbiddenWording.OverloadsDefender =>
        Vector("overloads defender", "overloads the defender", "overload", "overloaded")
      case ForbiddenWording.TrapsPiece =>
        Vector("traps piece", "traps the piece", "trap", "trapped")
      case ForbiddenWording.RawReplyLine =>
        Vector("reply line", "raw reply", "reply is", "reply move")
      case ForbiddenWording.WhyItMatters =>
        Vector("why it matters", "this matters because")

  private def forbiddenLabel(forbidden: ForbiddenWording): String =
    forbiddenMeaning(forbidden).head

  private def moveTokens(text: String): Vector[String] =
    """(?i)\b(?:O-O-O|O-O|[a-h][1-8][x-]?[a-h][1-8]|[KQRBN]?[a-h]?[1-8]?x?[a-h][1-8](?:=[QRBN])?[+#]?)\b""".r
      .findAllIn(text)
      .map(_.toLowerCase(Locale.ROOT))
      .toVector

  private def usesCoordinateMoveText(text: String): Boolean =
    """(?i)\b[a-h][1-8][a-h][1-8][qrbn]?\b""".r.findFirstIn(text).nonEmpty ||
      """(?i)\b[a-h][1-8][x-][a-h][1-8]\b""".r.findFirstIn(text).nonEmpty ||
      """(?i)\bfrom\s+[a-h][1-8]\s+to\s+[a-h][1-8]\b""".r.findFirstIn(text).nonEmpty

  private def squareTokens(text: String): Vector[String] =
    """(?i)\b[a-h][1-8]\b""".r
      .findAllIn(text)
      .map(_.toLowerCase(Locale.ROOT))
      .toVector

  private def containsPhrase(normalizedText: String, phrase: String): Boolean =
    val normalizedPhrase = normalize(phrase)
    s" $normalizedText ".contains(s" $normalizedPhrase ")

  private def normalize(text: String): String =
    text
      .toLowerCase(Locale.ROOT)
      .replaceAll("[^a-z0-9_]+", " ")
      .replaceAll("\\s+", " ")
      .trim

  private def squareText(square: Square): String =
    s"${('a' + square.file).toChar}${square.rank + 1}"

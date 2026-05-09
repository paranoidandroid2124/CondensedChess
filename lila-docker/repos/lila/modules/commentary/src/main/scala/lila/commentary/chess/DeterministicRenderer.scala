package lila.commentary.chess

import java.util.Locale

private[commentary] final case class RenderedLine(
    text: String,
    claimKey: String,
    strength: String,
    forbiddenCheckPassed: Boolean
)

private[commentary] object DeterministicRenderer:
  def fromPlan(plan: ExplanationPlan): Option[RenderedLine] =
    textFromPlan(plan).flatMap: text =>
      val forbiddenCheckPassed = respectsForbiddenWording(text, plan)
      Option.when(forbiddenCheckPassed):
        RenderedLine(
          text = text,
          claimKey = plan.allowedClaim.get.key,
          strength = plan.strength.key,
          forbiddenCheckPassed = true
        )

  private def textFromPlan(plan: ExplanationPlan): Option[String] =
    if canPhraseHanging(plan) then
      Some(s"${plan.routeSan.get} wins material against the piece on ${squareText(plan.target.get)}.")
    else if canPhraseFork(plan) then
      Some(
        s"${plan.routeSan.get} forks the pieces on ${squareText(plan.target.get)} and ${squareText(plan.secondaryTarget.get)}."
      )
    else if canPhraseDiscoveredAttack(plan) then
      Some(s"${plan.routeSan.get} reveals an attack on the piece on ${squareText(plan.target.get)}.")
    else if canPhrasePin(plan) then
      Some(s"${plan.routeSan.get} pins the piece on ${squareText(plan.target.get)}.")
    else if canPhraseRemoveGuard(plan) then
      Some(s"${plan.routeSan.get} removes the defender of the piece on ${squareText(plan.target.get)}.")
    else if canPhraseSkewer(plan) then
      Some(
        s"${plan.routeSan.get} skewers the piece on ${squareText(plan.target.get)} to the piece on ${squareText(plan.secondaryTarget.get)}."
      )
    else if canPhrasePawnAdvance(plan) then Some(s"${plan.routeSan.get} advances the passed pawn.")
    else if canPhrasePawnStop(plan) then
      Some(s"${plan.routeSan.get} stops the passed pawn from advancing next.")
    else if canPhrasePawnBreak(plan) then
      Some(s"${plan.routeSan.get} challenges the pawn on ${squareText(plan.target.get)}.")
    else if canPhrasePawnCapture(plan) then
      Some(s"${plan.routeSan.get} captures the pawn on ${squareText(plan.target.get)}.")
    else if canPhrasePassedPawnCreated(plan) then
      Some(s"${plan.routeSan.get} creates a passed pawn on ${squareText(plan.target.get)}.")
    else if canPhraseFileOpened(plan) then
      Some(s"${plan.routeSan.get} opens the ${fileText(plan.anchor.get.file)}-file.")
    else if canPhrasePromotionThreat(plan) then Some(s"${plan.routeSan.get} threatens to promote next.")
    else if canPhrasePromotion(plan) then Some(s"${plan.routeSan.get} promotes the pawn.")
    else if canPhraseMaterial(plan) then
      Some(s"After ${plan.routeSan.get}, ${sideText(plan.side)} comes out ahead in material.")
    else if canPhraseDefense(plan) then
      plan.allowedClaim match
        case Some(ExplanationClaim.PreventsMaterialLoss) =>
          Some(
            s"${plan.routeSan.get} prevents the piece on ${squareText(plan.target.get)} from being lost immediately."
          )
        case Some(ExplanationClaim.DefendsPiece | ExplanationClaim.ProtectsTarget) =>
          Some(s"${plan.routeSan.get} defends the piece on ${squareText(plan.target.get)}.")
        case _ => None
    else None

  private def canPhraseHanging(plan: ExplanationPlan): Boolean =
    plan.role == Role.Lead &&
      !plan.debugOnly &&
      plan.scene == Scene.Tactic &&
      plan.tactic.contains(Tactic.Hanging) &&
      plan.allowedClaim.contains(ExplanationClaim.CanWinPiece) &&
      plan.strength == ExplanationStrength.Bounded &&
      plan.target.nonEmpty &&
      plan.route.nonEmpty &&
      plan.routeSan.nonEmpty &&
      plan.evidenceLine.contains(plan.route.get) &&
      plan.forbiddenWording.nonEmpty

  private def canPhraseFork(plan: ExplanationPlan): Boolean =
    plan.role == Role.Lead &&
      !plan.debugOnly &&
      plan.scene == Scene.Tactic &&
      plan.tactic.contains(Tactic.Fork) &&
      plan.allowedClaim.contains(ExplanationClaim.ForksTwoTargets) &&
      plan.strength == ExplanationStrength.Bounded &&
      plan.target.nonEmpty &&
      plan.secondaryTarget.nonEmpty &&
      plan.route.nonEmpty &&
      plan.routeSan.nonEmpty &&
      plan.evidenceLine.contains(plan.route.get) &&
      plan.forbiddenWording.nonEmpty

  private def canPhraseDiscoveredAttack(plan: ExplanationPlan): Boolean =
    plan.role == Role.Lead &&
      !plan.debugOnly &&
      plan.scene == Scene.Tactic &&
      plan.tactic.contains(Tactic.DiscoveredAttack) &&
      plan.allowedClaim.contains(ExplanationClaim.RevealsAttackOnPiece) &&
      plan.strength == ExplanationStrength.Bounded &&
      plan.target.nonEmpty &&
      plan.secondaryTarget.isEmpty &&
      plan.route.nonEmpty &&
      plan.routeSan.nonEmpty &&
      plan.evidenceLine.contains(plan.route.get) &&
      plan.forbiddenWording.nonEmpty

  private def canPhrasePin(plan: ExplanationPlan): Boolean =
    plan.role == Role.Lead &&
      !plan.debugOnly &&
      plan.scene == Scene.Tactic &&
      plan.tactic.contains(Tactic.Pin) &&
      plan.allowedClaim.contains(ExplanationClaim.PinsPiece) &&
      plan.strength == ExplanationStrength.Bounded &&
      plan.target.nonEmpty &&
      plan.secondaryTarget.isEmpty &&
      plan.route.nonEmpty &&
      plan.routeSan.nonEmpty &&
      plan.evidenceLine.contains(plan.route.get) &&
      plan.forbiddenWording.nonEmpty

  private def canPhraseRemoveGuard(plan: ExplanationPlan): Boolean =
    plan.role == Role.Lead &&
      !plan.debugOnly &&
      plan.scene == Scene.Tactic &&
      plan.tactic.contains(Tactic.RemoveGuard) &&
      plan.allowedClaim.contains(ExplanationClaim.RemovesDefender) &&
      plan.strength == ExplanationStrength.Bounded &&
      plan.target.nonEmpty &&
      plan.secondaryTarget.isEmpty &&
      plan.route.nonEmpty &&
      plan.routeSan.nonEmpty &&
      plan.evidenceLine.contains(plan.route.get) &&
      plan.forbiddenWording.nonEmpty

  private def canPhraseSkewer(plan: ExplanationPlan): Boolean =
    plan.role == Role.Lead &&
      !plan.debugOnly &&
      plan.scene == Scene.Tactic &&
      plan.tactic.contains(Tactic.Skewer) &&
      plan.allowedClaim.contains(ExplanationClaim.SkewersPieceToPiece) &&
      plan.strength == ExplanationStrength.Bounded &&
      plan.target.nonEmpty &&
      plan.secondaryTarget.nonEmpty &&
      plan.route.nonEmpty &&
      plan.routeSan.nonEmpty &&
      plan.evidenceLine.contains(plan.route.get) &&
      plan.forbiddenWording.nonEmpty

  private def canPhrasePawnAdvance(plan: ExplanationPlan): Boolean =
    plan.role == Role.Lead &&
      !plan.debugOnly &&
      plan.scene == Scene.PawnAdvance &&
      plan.tactic.isEmpty &&
      plan.allowedClaim.contains(ExplanationClaim.AdvancesPassedPawn) &&
      plan.strength == ExplanationStrength.Bounded &&
      plan.target.nonEmpty &&
      plan.anchor.nonEmpty &&
      plan.secondaryTarget.isEmpty &&
      plan.route.nonEmpty &&
      plan.routeSan.nonEmpty &&
      plan.evidenceLine.contains(plan.route.get) &&
      plan.forbiddenWording.nonEmpty

  private def canPhrasePawnStop(plan: ExplanationPlan): Boolean =
    plan.role == Role.Lead &&
      !plan.debugOnly &&
      plan.scene == Scene.PawnStop &&
      plan.tactic.isEmpty &&
      plan.allowedClaim.contains(ExplanationClaim.StopsPassedPawnNextAdvance) &&
      plan.strength == ExplanationStrength.Bounded &&
      plan.target.nonEmpty &&
      plan.anchor.nonEmpty &&
      plan.secondaryTarget.isEmpty &&
      plan.route.nonEmpty &&
      plan.routeSan.nonEmpty &&
      plan.evidenceLine.contains(plan.route.get) &&
      plan.forbiddenWording.nonEmpty

  private def canPhrasePawnBreak(plan: ExplanationPlan): Boolean =
    plan.role == Role.Lead &&
      !plan.debugOnly &&
      plan.scene == Scene.PawnBreak &&
      plan.tactic.isEmpty &&
      plan.allowedClaim.contains(ExplanationClaim.ChallengesPawnDirectly) &&
      plan.strength == ExplanationStrength.Bounded &&
      plan.target.nonEmpty &&
      plan.anchor.nonEmpty &&
      plan.secondaryTarget.isEmpty &&
      plan.route.nonEmpty &&
      plan.routeSan.nonEmpty &&
      plan.evidenceLine.contains(plan.route.get) &&
      plan.forbiddenWording.nonEmpty

  private def canPhrasePawnCapture(plan: ExplanationPlan): Boolean =
    plan.role == Role.Lead &&
      !plan.debugOnly &&
      plan.scene == Scene.PawnCapture &&
      plan.tactic.isEmpty &&
      plan.allowedClaim.contains(ExplanationClaim.CapturesPawn) &&
      plan.strength == ExplanationStrength.Bounded &&
      plan.target.nonEmpty &&
      plan.anchor.nonEmpty &&
      plan.secondaryTarget.isEmpty &&
      plan.route.nonEmpty &&
      plan.routeSan.nonEmpty &&
      plan.evidenceLine.contains(plan.route.get) &&
      plan.forbiddenWording.nonEmpty

  private def canPhrasePassedPawnCreated(plan: ExplanationPlan): Boolean =
    plan.role == Role.Lead &&
      !plan.debugOnly &&
      plan.scene == Scene.PassedPawnCreated &&
      plan.tactic.isEmpty &&
      plan.allowedClaim.contains(ExplanationClaim.CreatesPassedPawn) &&
      plan.strength == ExplanationStrength.Bounded &&
      plan.target.nonEmpty &&
      plan.anchor.nonEmpty &&
      plan.secondaryTarget.isEmpty &&
      plan.route.nonEmpty &&
      plan.routeSan.nonEmpty &&
      plan.evidenceLine.contains(plan.route.get) &&
      plan.forbiddenWording.nonEmpty

  private def canPhraseFileOpened(plan: ExplanationPlan): Boolean =
    plan.role == Role.Lead &&
      !plan.debugOnly &&
      plan.scene == Scene.FileOpened &&
      plan.tactic.isEmpty &&
      plan.allowedClaim.contains(ExplanationClaim.OpensFile) &&
      plan.strength == ExplanationStrength.Bounded &&
      plan.target.nonEmpty &&
      plan.anchor.nonEmpty &&
      plan.secondaryTarget.isEmpty &&
      plan.route.nonEmpty &&
      plan.routeSan.nonEmpty &&
      plan.evidenceLine.contains(plan.route.get) &&
      plan.forbiddenWording.nonEmpty

  private def canPhrasePromotionThreat(plan: ExplanationPlan): Boolean =
    plan.role == Role.Lead &&
      !plan.debugOnly &&
      plan.scene == Scene.PromotionThreat &&
      plan.tactic.isEmpty &&
      plan.allowedClaim.contains(ExplanationClaim.CreatesPromotionThreat) &&
      plan.strength == ExplanationStrength.Bounded &&
      plan.target.nonEmpty &&
      plan.anchor.nonEmpty &&
      plan.secondaryTarget.isEmpty &&
      plan.route.nonEmpty &&
      plan.routeSan.nonEmpty &&
      plan.evidenceLine.contains(plan.route.get) &&
      plan.forbiddenWording.nonEmpty

  private def canPhrasePromotion(plan: ExplanationPlan): Boolean =
    plan.role == Role.Lead &&
      !plan.debugOnly &&
      plan.scene == Scene.Promotion &&
      plan.tactic.isEmpty &&
      plan.allowedClaim.contains(ExplanationClaim.PromotesPawn) &&
      plan.strength == ExplanationStrength.Bounded &&
      plan.target.nonEmpty &&
      plan.anchor.nonEmpty &&
      plan.secondaryTarget.isEmpty &&
      plan.route.nonEmpty &&
      plan.routeSan.nonEmpty &&
      plan.evidenceLine.contains(plan.route.get) &&
      plan.forbiddenWording.nonEmpty

  private def canPhraseMaterial(plan: ExplanationPlan): Boolean =
    plan.role == Role.Lead &&
      !plan.debugOnly &&
      plan.scene == Scene.Material &&
      plan.tactic.isEmpty &&
      plan.allowedClaim.contains(ExplanationClaim.MaterialBalanceChanges) &&
      plan.strength == ExplanationStrength.Bounded &&
      (plan.side == Side.White || plan.side == Side.Black) &&
      plan.target.nonEmpty &&
      plan.anchor.nonEmpty &&
      plan.secondaryTarget.isEmpty &&
      plan.route.nonEmpty &&
      plan.routeSan.nonEmpty &&
      plan.evidenceLine.contains(plan.route.get) &&
      plan.forbiddenWording.nonEmpty

  private def canPhraseDefense(plan: ExplanationPlan): Boolean =
    plan.role == Role.Lead &&
      !plan.debugOnly &&
      plan.scene == Scene.Defense &&
      plan.tactic.isEmpty &&
      plan.allowedClaim.exists(ExplanationClaim.DefenseAllowed.contains) &&
      plan.strength == ExplanationStrength.Bounded &&
      (plan.side == Side.White || plan.side == Side.Black) &&
      plan.target.nonEmpty &&
      plan.anchor.nonEmpty &&
      plan.secondaryTarget.isEmpty &&
      plan.route.nonEmpty &&
      plan.routeSan.nonEmpty &&
      plan.evidenceLine.contains(plan.route.get) &&
      plan.forbiddenWording.nonEmpty

  private def respectsForbiddenWording(text: String, plan: ExplanationPlan): Boolean =
    val normalized = normalize(text)
    val forbiddenPhrases = plan.forbiddenWording.flatMap(forbiddenMeaning)
    forbiddenPhrases.forall(phrase => !containsPhrase(normalized, phrase)) &&
    publicForbiddenPhrases.forall(phrase => !containsPhrase(normalized, phrase)) &&
    noEnginePublicExpression(text, normalized) &&
    materialWinAllowed(normalized, plan)

  private def forbiddenMeaning(forbidden: ForbiddenWording): Vector[String] =
    forbidden match
      case ForbiddenWording.FreePiece =>
        Vector("free piece")
      case ForbiddenWording.Blunder =>
        Vector("blunder")
      case ForbiddenWording.Winning =>
        Vector("winning", "winning position")
      case ForbiddenWording.Decisive =>
        Vector("decisive")
      case ForbiddenWording.Forced =>
        Vector("forced", "forces")
      case ForbiddenWording.BestMove =>
        Vector("best move")
      case ForbiddenWording.OnlyMove =>
        Vector("only move")
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
      case ForbiddenWording.NoCounterplay =>
        Vector("no counterplay")
      case ForbiddenWording.KingUnsafe =>
        Vector("king unsafe", "king is unsafe", "the king is unsafe", "unsafe king", "king safety")
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
        Vector("mate threat", "mating threat", "creates a mating threat", "threatens mate", "mate")
      case ForbiddenWording.CannotMove =>
        Vector("cannot move", "can't move", "can not move")
      case ForbiddenWording.TargetIsHanging =>
        Vector("target is hanging", "target hangs", "hanging target")
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

  private def materialWinAllowed(normalized: String, plan: ExplanationPlan): Boolean =
    val materialWinPhrases =
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
    !materialWinPhrases.exists(containsPhrase(normalized, _)) ||
    plan.allowedClaim.contains(ExplanationClaim.CanWinPiece)

  private def noEnginePublicExpression(text: String, normalized: String): Boolean =
    val enginePhrases =
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
      )
    !enginePhrases.exists(containsPhrase(normalized, _)) &&
    EvalNumberPattern.findFirstIn(text).isEmpty &&
    CentipawnPattern.findFirstIn(text).isEmpty

  private val EvalNumberPattern =
    """(?<![A-Za-z0-9])[+-](?:\d+(?:\.\d+)?|\.\d+)(?![A-Za-z0-9])""".r

  private val CentipawnPattern =
    """(?i)\b\d+(?:\.\d+)?\s*(?:cp|centipawns?)\b""".r

  private val publicForbiddenPhrases =
    Vector("best move", "only move", "forced", "forces", "forced line")

  private def containsPhrase(normalizedText: String, phrase: String): Boolean =
    val normalizedPhrase = normalize(phrase)
    s" $normalizedText ".contains(s" $normalizedPhrase ")

  private def normalize(text: String): String =
    text
      .toLowerCase(Locale.ROOT)
      .replaceAll("[^a-z0-9]+", " ")
      .replaceAll("\\s+", " ")
      .trim

  private def squareText(square: Square): String =
    s"${('a' + square.file).toChar}${square.rank + 1}"

  private def fileText(file: Int): String =
    s"${('a' + file).toChar}"

  private def sideText(side: Side): String =
    side match
      case Side.White => "White"
      case Side.Black => "Black"
      case Side.Both => "Both sides"
      case Side.None => "The side"

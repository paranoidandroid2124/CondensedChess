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
    else if canPhraseMaterial(plan) then
      Some(s"After ${plan.routeSan.get}, ${sideText(plan.side)} comes out ahead in material.")
    else if canPhraseDefense(plan) then
      plan.allowedClaim match
        case Some(ExplanationClaim.PreventsMaterialLoss) =>
          Some(s"${plan.routeSan.get} prevents the piece on ${squareText(plan.target.get)} from being lost immediately.")
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
        Vector("file control")
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
        Vector("no defender remains", "no defenders remain", "no defender left", "no defender", "no defenders")
      case ForbiddenWording.RemovesDefender =>
        Vector("removes defender", "removes the defender")
      case ForbiddenWording.LineTacticIdentity =>
        Vector("line tactic", "line tactics")

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

  private def sideText(side: Side): String =
    side match
      case Side.White => "White"
      case Side.Black => "Black"
      case Side.Both  => "Both sides"
      case Side.None  => "The side"

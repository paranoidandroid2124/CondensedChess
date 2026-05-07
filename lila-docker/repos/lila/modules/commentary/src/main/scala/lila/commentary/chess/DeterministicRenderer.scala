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
      Some(s"${captureRouteText(plan.evidenceLine.get)} wins material against the piece on ${squareText(plan.target.get)}.")
    else if canPhraseFork(plan) then
      Some(
        s"${moveRouteText(plan.evidenceLine.get)} forks the pieces on ${squareText(plan.target.get)} and ${squareText(plan.secondaryTarget.get)}."
      )
    else if canPhraseMaterial(plan) then
      Some(s"After ${captureRouteText(plan.evidenceLine.get)}, ${sideText(plan.side)} comes out ahead in material.")
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
      plan.evidenceLine.contains(plan.route.get) &&
      plan.forbiddenWording.nonEmpty

  private def respectsForbiddenWording(text: String, plan: ExplanationPlan): Boolean =
    val normalized = normalize(text)
    val forbiddenPhrases = plan.forbiddenWording.flatMap(forbiddenMeaning)
    forbiddenPhrases.forall(phrase => !containsPhrase(normalized, phrase)) &&
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
        Vector("forced")
      case ForbiddenWording.BestMove =>
        Vector("best move")
      case ForbiddenWording.OnlyMove =>
        Vector("only move")
      case ForbiddenWording.EngineSays =>
        Vector("engine says", "engine approved")
      case ForbiddenWording.NoCounterplay =>
        Vector("no counterplay")
      case ForbiddenWording.KingUnsafe =>
        Vector("king unsafe", "unsafe king", "king safety")
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

  private def materialWinAllowed(normalized: String, plan: ExplanationPlan): Boolean =
    val materialWinPhrases =
      Vector("wins material", "win material", "winning material", "material win")
    !materialWinPhrases.exists(containsPhrase(normalized, _)) ||
      plan.allowedClaim.contains(ExplanationClaim.CanWinPiece)

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

  private def captureRouteText(line: Line): String =
    s"${squareText(line.from)}x${squareText(line.to)}"

  private def moveRouteText(line: Line): String =
    s"${squareText(line.from)}-${squareText(line.to)}"

  private def sideText(side: Side): String =
    side match
      case Side.White => "White"
      case Side.Black => "Black"
      case Side.Both  => "Both sides"
      case Side.None  => "The side"

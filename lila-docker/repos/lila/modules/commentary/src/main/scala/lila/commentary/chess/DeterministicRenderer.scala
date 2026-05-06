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
    if canPhrase(plan) then
      val text =
        s"${routeText(plan.evidenceLine.get)} wins material against the piece on ${squareText(plan.target.get)}."
      val forbiddenCheckPassed = respectsForbiddenWording(text, plan)
      Option.when(forbiddenCheckPassed):
        RenderedLine(
          text = text,
          claimKey = plan.allowedClaim.get.key,
          strength = plan.strength.key,
          forbiddenCheckPassed = true
        )
    else None

  private def canPhrase(plan: ExplanationPlan): Boolean =
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

  private def routeText(line: Line): String =
    s"${squareText(line.from)}x${squareText(line.to)}"

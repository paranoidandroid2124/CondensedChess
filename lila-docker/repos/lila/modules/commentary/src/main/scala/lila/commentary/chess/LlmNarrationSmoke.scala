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
         |Do not add a move, line, tactic, plan, cause, evaluation, engine mention, or stronger claim.
         |
         |renderedText: ${rendered.text}
         |claimKey: ${rendered.claimKey}
         |strength: ${rendered.strength}
         |forbiddenWording: ${plan.forbiddenWording.map(forbiddenLabel).mkString(", ")}
         |
         |Return JSON only: {"text":"..."}""".stripMargin

  def check(plan: ExplanationPlan, rendered: RenderedLine, output: String): NarrationSmokeCheck =
    val violations =
      Vector(
        Option.when(!inputMatches(plan, rendered))("input_mismatch"),
        Option.when(violatesForbiddenWording(output, plan))("forbidden_wording"),
        Option.when(addsNewMoveOrLine(output, plan, rendered))("new_move_or_line"),
        Option.when(addsNewTacticOrPlan(output))("new_tactic_or_plan"),
        Option.when(addsNewCauseOrEvaluation(output))("new_cause_or_evaluation"),
        Option.when(mentionsEngine(output))("engine_mention"),
        Option.when(strongerThanRendered(output, plan, rendered))("stronger_claim")
      ).flatten.distinct
    NarrationSmokeCheck(violations.isEmpty, violations)

  private def inputMatches(plan: ExplanationPlan, rendered: RenderedLine): Boolean =
    plan.role == Role.Lead &&
      !plan.debugOnly &&
      plan.allowedClaim.exists(_.key == rendered.claimKey) &&
      rendered.strength == plan.strength.key &&
      rendered.forbiddenCheckPassed &&
      plan.evidenceLine.nonEmpty &&
      plan.route.nonEmpty &&
      plan.evidenceLine == plan.route

  private def violatesForbiddenWording(text: String, plan: ExplanationPlan): Boolean =
    val normalized = normalize(text)
    val forbiddenPhrases = plan.forbiddenWording.flatMap(forbiddenMeaning)
    forbiddenPhrases.exists(phrase => containsPhrase(normalized, phrase)) ||
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
        "forced",
        "best move",
        "only move",
        "engine says",
        "engine approved",
        "no counterplay"
      )
    strongPhrases.exists(phrase => containsPhrase(normalized, phrase)) ||
      (!plan.allowedClaim.contains(ExplanationClaim.CanWinPiece) && materialWinPhrases.exists(containsPhrase(normalized, _))) ||
      (containsPhrase(normalized, "wins material") && !containsPhrase(renderedNormalized, "wins material"))

  private def addsNewMoveOrLine(text: String, plan: ExplanationPlan, rendered: RenderedLine): Boolean =
    val allowedMoves =
      moveTokens(rendered.text).toSet ++
        plan.route.map(routeText).toSet ++
        plan.evidenceLine.map(routeText).toSet
    val allowedSquares =
      squareTokens(rendered.text).toSet ++
        plan.target.map(squareText).toSet ++
        plan.anchor.map(squareText).toSet ++
        plan.route.toVector.flatMap(line => Vector(squareText(line.from), squareText(line.to))) ++
        plan.evidenceLine.toVector.flatMap(line => Vector(squareText(line.from), squareText(line.to)))
    moveTokens(text).exists(token => !allowedMoves.contains(token)) ||
      squareTokens(text).exists(token => !allowedSquares.contains(token))

  private def addsNewTacticOrPlan(text: String): Boolean =
    val normalized = normalize(text)
    Vector(
      "fork",
      "pin",
      "skewer",
      "x ray",
      "xray",
      "mate",
      "mate net",
      "plan",
      "strategy",
      "strategic",
      "king safety",
      "counterplay",
      "conversion",
      "outpost",
      "file control",
      "defense"
    ).exists(phrase => containsPhrase(normalized, phrase))

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

  private def mentionsEngine(text: String): Boolean =
    val normalized = normalize(text)
    Vector("engine", "stockfish", "eval", "pv").exists(phrase => containsPhrase(normalized, phrase))

  private val materialWinPhrases =
    Vector("wins material", "win material", "winning material", "material win")

  private def materialWinAllowed(normalized: String, plan: ExplanationPlan): Boolean =
    !materialWinPhrases.exists(containsPhrase(normalized, _)) ||
      plan.allowedClaim.contains(ExplanationClaim.CanWinPiece)

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

  private def forbiddenLabel(forbidden: ForbiddenWording): String =
    forbiddenMeaning(forbidden).head

  private def moveTokens(text: String): Vector[String] =
    """(?i)\b(?:[a-h][1-8][x-]?[a-h][1-8]|[KQRBN]?[a-h]?[1-8]?x?[a-h][1-8](?:=[QRBN])?)\b""".r
      .findAllIn(text)
      .map(_.toLowerCase(Locale.ROOT))
      .toVector

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
      .replaceAll("[^a-z0-9]+", " ")
      .replaceAll("\\s+", " ")
      .trim

  private def squareText(square: Square): String =
    s"${('a' + square.file).toChar}${square.rank + 1}"

  private def routeText(line: Line): String =
    s"${squareText(line.from)}x${squareText(line.to)}"

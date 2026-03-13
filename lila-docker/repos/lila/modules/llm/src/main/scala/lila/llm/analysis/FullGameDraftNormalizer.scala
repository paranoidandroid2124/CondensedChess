package lila.llm.analysis

import lila.llm.analysis.ThemeTaxonomy.{ SubplanCatalog, SubplanId, ThemeL1, ThemeResolver }

private[llm] object FullGameDraftNormalizer:

  private val MetaLeakTokens = List(
    "Idea:",
    "Primary route is",
    "Ranked stack:",
    "Preconditions:",
    "Evidence:",
    "Signals:",
    "Refutation/Hold:"
  )

  private val MetaRewrites: List[(String, String)] = List(
    """(?i)\bIdea:\s*""" -> "",
    """(?i)\bPrimary route is\s+""" -> "The leading route is ",
    """(?i)\bRanked stack:\s*""" -> "Related candidates still cluster around ",
    """(?i)\bPreconditions:\s*""" -> "This works best when ",
    """(?i)\bEvidence:\s*""" -> "",
    """(?i)\bSignals:\s*""" -> "The clearest signs are ",
    """(?i)\bRefutation/Hold:\s*""" -> ""
  )

  def renderLatentPlanText(
      template: String,
      fen: String,
      seedId: String,
      fallbackPlanName: Option[String] = None
  ): String =
    val us = if sideToMoveIsWhite(fen) then "White" else "Black"
    val them = if us == "White" then "Black" else "White"
    val seedReadable = renderSeedPhrase(seedId)
    val fallback =
      fallbackPlanName
        .map(_.trim)
        .filter(_.nonEmpty)
        .map(plan => s"If $them is slow, $us can work toward $plan.")
        .getOrElse(s"If $them is slow, a long-term plan becomes available for $us.")
    val base = Option(template).map(_.trim).filter(_.nonEmpty).getOrElse(fallback)
    normalize(
      base
        .replace("{us}", us)
        .replace("{them}", them)
        .replace("{seed}", Option.when(seedReadable.nonEmpty)(seedReadable).getOrElse("the key setup move"))
    )

  def normalize(raw: String): String =
    val sanitized = UserFacingSignalSanitizer.sanitize(raw)
    val proseified = MetaRewrites.foldLeft(sanitized) { case (acc, (pattern, replacement)) =>
      acc.replaceAll(pattern, replacement)
    }
    UserFacingSignalSanitizer.sanitize(cleanup(proseified))

  def placeholderHits(raw: String): List[String] =
    UserFacingSignalSanitizer.placeholderHits(raw)

  def metaLeakHits(raw: String): List[String] =
    val low = Option(raw).getOrElse("").toLowerCase
    MetaLeakTokens.filter(token => low.contains(token.toLowerCase))

  def leakHits(raw: String): List[String] =
    (placeholderHits(raw) ++ metaLeakHits(raw)).distinct

  def humanizeConstraint(raw: String): String =
    Option(raw)
      .map(_.trim)
      .getOrElse("")
      .replaceAll("""(?i)^if\s+""", "")
      .replaceAll("""(?i)^requires\s+""", "")
      .replaceAll("""(?i)^needs\s+""", "")
      .replaceAll("""(?i)\bthem king\b""", "the enemy king")
      .replaceAll("""(?i)\bopponent blocks with\.\s*""", "the opponent blocks with ")
      .replaceAll("""\s+\.""", ".")
      .replaceAll("""\s{2,}""", " ")
      .stripSuffix(".")

  def humanizeEvidenceSource(raw: String): Option[String] =
    val trimmed = Option(raw).map(_.trim).filter(_.nonEmpty).getOrElse("")
    if trimmed.isEmpty then None
    else
      ThemeResolver
        .subplanFromEvidenceSource(trimmed)
        .flatMap(SubplanCatalog.specs.get)
        .map(_.objective)
        .orElse {
          ThemeResolver.fromEvidenceSource(trimmed) match
            case ThemeL1.OpeningPrinciples       => Some("development and king safety")
            case ThemeL1.RestrictionProphylaxis  => Some("restriction and prophylaxis")
            case ThemeL1.PieceRedeployment       => Some("piece improvement")
            case ThemeL1.SpaceClamp              => Some("space gains and restriction")
            case ThemeL1.WeaknessFixation        => Some("persistent structural targets")
            case ThemeL1.PawnBreakPreparation    => Some("break timing")
            case ThemeL1.FavorableExchange       => Some("favorable exchanges")
            case ThemeL1.FlankInfrastructure     => Some("flank attacking infrastructure")
            case ThemeL1.AdvantageTransformation => Some("durable advantage conversion")
            case ThemeL1.ImmediateTacticalGain   => Some("forcing tactical pressure")
            case ThemeL1.Unknown                 => None
        }
        .orElse {
          val lowered = trimmed.toLowerCase
          val stripped =
            lowered
              .replaceFirst("""^(theme|subplan|support|proposal|seed|latent_seed|structural_state):""", "")
              .replaceAll("""^probe:""", "")
              .replace('_', ' ')
              .replace('-', ' ')
              .replaceAll("""\s+""", " ")
              .trim
          Option.when(stripped.nonEmpty && !stripped.contains(":"))(stripped)
        }

  private def cleanup(text: String): String =
    Option(text)
      .getOrElse("")
      .replace("No explicit refutation was detected.", "Nothing concrete refutes the plan yet.")
      .replaceAll("""(?i)\bthem king\b""", "the enemy king")
      .replaceAll("""(?i)\bopponent blocks with\.\s*""", "the opponent blocks with ")
      .replaceAll("""(?i)\bThis works best when\s+if\b""", "This works best when ")
      .replaceAll("""(?i)\bThis works best when\s+(requires|needs)\b""", "This works best when ")
      .replaceAll("""\s+\.""", ".")
      .replaceAll("""\.\s*\.""", ". ")
      .replaceAll("""\s{2,}""", " ")
      .replaceAll("""(?i)\bThe leading route is\s+The leading route is\b""", "The leading route is")
      .trim

  private def renderSeedPhrase(seedId: String): String =
    val raw = Option(seedId).getOrElse("").trim
    if raw.isEmpty then ""
    else
      val normalized = raw.replaceAll("([a-z])([A-Z])", "$1_$2").replace('-', '_').toLowerCase
      val tokens = normalized.split("[^a-z0-9]+").filter(_.nonEmpty).toSet

      def has(token: String) = tokens.contains(token)

      ThemeResolver
        .subplanFromSeedId(normalized)
        .flatMap {
          case SubplanId.RookPawnMarch if has("kingside")  => Some("a kingside pawn storm")
          case SubplanId.RookPawnMarch if has("queenside") => Some("a queenside pawn storm")
          case SubplanId.RookPawnMarch                     => Some("a pawn storm")
          case SubplanId.CentralBreakTiming if has("e")    => Some("an e-break")
          case SubplanId.CentralBreakTiming if has("d")    => Some("a d-break")
          case sid =>
            SubplanCatalog.specs
              .get(sid)
              .flatMap(_.aliases.headOption)
              .map(withIndefiniteArticle)
        }
        .orElse {
          val pawnStorm = has("pawnstorm") || (has("pawn") && has("storm"))
          if pawnStorm && has("kingside") then Some("a kingside pawn storm")
          else if pawnStorm && has("queenside") then Some("a queenside pawn storm")
          else if pawnStorm then Some("a pawn storm")
          else None
        }
        .getOrElse {
          val plain = normalized.replace('_', ' ').trim
          if plain.isEmpty then ""
          else withIndefiniteArticle(plain)
        }

  private def withIndefiniteArticle(phrase: String): String =
    val cleaned = Option(phrase).getOrElse("").trim
    if cleaned.isEmpty then cleaned
    else
      val low = cleaned.toLowerCase
      if low.startsWith("a ") || low.startsWith("an ") || low.startsWith("the ") then cleaned
      else
        val article =
          cleaned.headOption.map(_.toLower) match
            case Some('a' | 'e' | 'i' | 'o' | 'u') => "an"
            case _                                  => "a"
        s"$article $cleaned"

  private def sideToMoveIsWhite(fen: String): Boolean =
    Option(fen).getOrElse("").trim.split("\\s+").drop(1).headOption.contains("w")

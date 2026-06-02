package lila.commentary.analysis

import lila.commentary.analysis.PlanTaxonomy.{ PlanKind, PlanTheme, SubplanCatalog, ThemeResolver }

private[commentary] object FullGameDraftNormalizer:

  private val MetaLeakTokens = List(
    "Idea:",
    "Primary route is",
    "Ranked stack:",
    "Preconditions:",
    "Evidence:",
    "Signals:",
    "Refutation/Hold:",
    "Strategic focus:",
    "Strategic priority:",
    "The strategic stack still points first to",
    "The strategic stack still favors",
    "The backup strategic stack is"
  )

  private val MetaRewrites: List[(String, String)] = List(
    """(?i)\bIdea:\s*""" -> "",
    """(?i)\bPrimary route is\s+""" -> "The leading route is ",
    """(?i)\bRanked stack:\s*""" -> "Related candidates still cluster around ",
    """(?i)\bPreconditions:\s*""" -> "This works best when ",
    """(?i)\bEvidence:\s*""" -> "",
    """(?i)\bSignals:\s*""" -> "The clearest signs are ",
    """(?i)\bRefutation/Hold:\s*""" -> "",
    """(?i)\bStrategic focus:\s*""" -> "Key theme: ",
    """(?i)\bStrategic priority:\s*""" -> "Key theme: ",
    """(?i)\bStrategic focus centers on\s+""" -> "The position revolves around ",
    """(?i)\bStrategic focus remains on\s+""" -> "The position still turns on ",
    """(?i)\bStrategic focus is sharpening along\s+""" -> "Pressure is sharpening along ",
    """(?i)\bKey theme centers on\s+""" -> "The position revolves around ",
    """(?i)\bKey theme remains on\s+""" -> "The position still turns on ",
    """(?i)\bKey theme is sharpening along\s+""" -> "Pressure is sharpening along ",
    """(?i)\bThe strategic stack still points first to\s+""" -> "The main plan remains ",
    """(?i)\bThe strategic stack still favors\s+""" -> "The main plan remains ",
    """(?i)\bThe backup strategic stack is\s+""" -> "Secondary ideas still include "
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
        .subplanIdFromSupport(trimmed)
        .flatMap(PlanKind.fromId)
        .flatMap(SubplanCatalog.specs.get)
        .map(_.objective)
        .orElse {
          ThemeResolver.themeIdFromSupport(trimmed).flatMap(PlanTheme.fromId).getOrElse(PlanTheme.Unknown) match
            case PlanTheme.OpeningPrinciples       => Some("development and king safety")
            case PlanTheme.RestrictionProphylaxis  => Some("restriction and prophylaxis")
            case PlanTheme.PieceRedeployment       => Some("piece improvement")
            case PlanTheme.SpaceClamp              => Some("space gains and restriction")
            case PlanTheme.WeaknessFixation        => Some("persistent structural targets")
            case PlanTheme.PawnBreakPreparation    => Some("break timing")
            case PlanTheme.FavorableExchange       => Some("favorable exchanges")
            case PlanTheme.FlankInfrastructure     => Some("flank attacking infrastructure")
            case PlanTheme.AdvantageTransformation => Some("durable advantage conversion")
            case PlanTheme.ImmediateTacticalGain   => Some("forcing tactical pressure")
            case PlanTheme.Unknown                 => None
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
      .replaceAll("""(?i)\bStrategic focus remains on ([^.]+)\.""", "$1 remains the practical priority.")
      .replaceAll("""(?i)\bThe strategic burden is still ([^.]+)\.""", "$1 remains the practical priority.")
      .replaceAll("""(?i)\bStrategic priority remains ([^.]+)\.""", "$1 remains the practical priority.")
      .replaceAll("""\s+\.""", ".")
      .replaceAll("""\.\s*\.""", ". ")
      .replaceAll("""\s{2,}""", " ")
      .replaceAll("""(?i)\bKey theme:\s+Key theme:\s+""", "Key theme: ")
      .replaceAll("""(?i)\bThe leading route is\s+The leading route is\b""", "The leading route is")
      .replaceAll("""(?i)\bThe main plan remains\s+The main plan remains\b""", "The main plan remains")
      .trim

  private def semanticText(raw: String): String =
    Option(raw)
      .getOrElse("")
      .toLowerCase
      .replaceAll("""\b(a|an|the)\b""", " ")
      .replaceAll("""[^\p{L}\p{N}\s]""", " ")
      .replaceAll("""\s+""", " ")
      .trim

package lila.commentary.analysis

import lila.commentary.analysis.PlanTaxonomy.{ SubplanCatalog, PlanTheme, ThemeResolver }

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

  private val MetaRewrites: List[(scala.util.matching.Regex, String)] = List(
    """(?i)\bIdea:\s*""".r -> "",
    """(?i)\bPrimary route is\s+""".r -> "The leading route is ",
    """(?i)\bRanked stack:\s*""".r -> "Related candidates still cluster around ",
    """(?i)\bPreconditions:\s*""".r -> "This works best when ",
    """(?i)\bEvidence:\s*""".r -> "",
    """(?i)\bSignals:\s*""".r -> "The clearest signs are ",
    """(?i)\bRefutation/Hold:\s*""".r -> "",
    """(?i)\bStrategic focus:\s*""".r -> "Key theme: ",
    """(?i)\bStrategic priority:\s*""".r -> "Key theme: ",
    """(?i)\bStrategic focus centers on\s+""".r -> "The position revolves around ",
    """(?i)\bStrategic focus remains on\s+""".r -> "The position still turns on ",
    """(?i)\bStrategic focus is sharpening along\s+""".r -> "Pressure is sharpening along ",
    """(?i)\bKey theme centers on\s+""".r -> "The position revolves around ",
    """(?i)\bKey theme remains on\s+""".r -> "The position still turns on ",
    """(?i)\bKey theme is sharpening along\s+""".r -> "Pressure is sharpening along ",
    """(?i)\bThe strategic stack still points first to\s+""".r -> "The main plan remains ",
    """(?i)\bThe strategic stack still favors\s+""".r -> "The main plan remains ",
    """(?i)\bThe backup strategic stack is\s+""".r -> "Secondary ideas still include "
  )

  private val IfPrefixPattern = """(?i)^if\s+""".r
  private val RequiresPrefixPattern = """(?i)^requires\s+""".r
  private val NeedsPrefixPattern = """(?i)^needs\s+""".r
  private val ThemKingPattern = """(?i)\bthem king\b""".r
  private val OpponentBlocksPattern = """(?i)\bopponent blocks with\.\s*""".r
  private val SpacePeriodPattern = """\s+\.""".r
  private val DoubleWhitespacePattern = """\s{2,}""".r
  private val ThemePrefixPattern = """^(theme|subplan|support|proposal|seed|latent_seed|structural_state):""".r
  private val ProbePrefixPattern = """^probe:""".r
  private val WhitespacePattern = """\s+""".r
  private val ThisWorksBestWhenIfPattern = """(?i)\bThis works best when\s+if\b""".r
  private val ThisWorksBestWhenReqPattern = """(?i)\bThis works best when\s+(requires|needs)\b""".r
  private val StrategicFocusRemainsPattern = """(?i)\bStrategic focus remains on ([^.]+)\.""".r
  private val StrategicBurdenPattern = """(?i)\bThe strategic burden is still ([^.]+)\.""".r
  private val StrategicPriorityPattern = """(?i)\bStrategic priority remains ([^.]+)\.""".r
  private val DoublePeriodPattern = """\.\s*\.""".r
  private val DoubleKeyThemePattern = """(?i)\bKey theme:\s+Key theme:\s+""".r
  private val DoubleLeadingRoutePattern = """(?i)\bThe leading route is\s+The leading route is\b""".r
  private val DoubleMainPlanPattern = """(?i)\bThe main plan remains\s+The main plan remains\b""".r
  private val ArticlesPattern = """\b(a|an|the)\b""".r
  private val NonAlphaNumSpacePattern = """[^\p{L}\p{N}\s]""".r

  def normalize(raw: String): String =
    val sanitized = UserFacingSignalSanitizer.sanitize(raw)
    val proseified = MetaRewrites.foldLeft(sanitized) { case (acc, (pattern, replacement)) =>
      pattern.replaceAllIn(acc, replacement)
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
    val s1 = Option(raw).map(_.trim).getOrElse("")
    val s2 = IfPrefixPattern.replaceAllIn(s1, "")
    val s3 = RequiresPrefixPattern.replaceAllIn(s2, "")
    val s4 = NeedsPrefixPattern.replaceAllIn(s3, "")
    val s5 = ThemKingPattern.replaceAllIn(s4, "the enemy king")
    val s6 = OpponentBlocksPattern.replaceAllIn(s5, "the opponent blocks with ")
    val s7 = SpacePeriodPattern.replaceAllIn(s6, ".")
    DoubleWhitespacePattern.replaceAllIn(s7, " ").stripSuffix(".")

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
          val s1 = ThemePrefixPattern.replaceFirstIn(lowered, "")
          val s2 = ProbePrefixPattern.replaceAllIn(s1, "")
          val s3 = s2.replace('_', ' ').replace('-', ' ')
          val stripped = WhitespacePattern.replaceAllIn(s3, " ").trim
          Option.when(stripped.nonEmpty && !stripped.contains(":"))(stripped)
        }

  private def cleanup(text: String): String =
    val s1 = Option(text).getOrElse("")
      .replace("No explicit refutation was detected.", "Nothing concrete refutes the plan yet.")
    val s2 = ThemKingPattern.replaceAllIn(s1, "the enemy king")
    val s3 = OpponentBlocksPattern.replaceAllIn(s2, "the opponent blocks with ")
    val s4 = ThisWorksBestWhenIfPattern.replaceAllIn(s3, "This works best when ")
    val s5 = ThisWorksBestWhenReqPattern.replaceAllIn(s4, "This works best when ")
    val s6 = StrategicFocusRemainsPattern.replaceAllIn(s5, "$1 remains the practical priority.")
    val s7 = StrategicBurdenPattern.replaceAllIn(s6, "$1 remains the practical priority.")
    val s8 = StrategicPriorityPattern.replaceAllIn(s7, "$1 remains the practical priority.")
    val s9 = SpacePeriodPattern.replaceAllIn(s8, ".")
    val s10 = DoublePeriodPattern.replaceAllIn(s9, ". ")
    val s11 = DoubleWhitespacePattern.replaceAllIn(s10, " ")
    val s12 = DoubleKeyThemePattern.replaceAllIn(s11, "Key theme: ")
    val s13 = DoubleLeadingRoutePattern.replaceAllIn(s12, "The leading route is")
    val s14 = DoubleMainPlanPattern.replaceAllIn(s13, "The main plan remains")
    s14.trim

  private def semanticText(raw: String): String =
    val s1 = Option(raw).getOrElse("").toLowerCase
    val s2 = ArticlesPattern.replaceAllIn(s1, " ")
    val s3 = NonAlphaNumSpacePattern.replaceAllIn(s2, " ")
    WhitespacePattern.replaceAllIn(s3, " ").trim

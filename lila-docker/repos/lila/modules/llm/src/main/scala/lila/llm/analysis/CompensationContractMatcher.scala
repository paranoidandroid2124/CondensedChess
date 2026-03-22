package lila.llm.analysis

private[llm] object CompensationContractMatcher:

  import StrategyPackSurface.*

  private val CompensationKeywords =
    List(
      "compensation",
      "initiative",
      "material can wait",
      "recover the material",
      "recover material",
      "winning the material back",
      "win the material back",
      "return vector"
    )

  def mentionsCompensationContract(text: String, surface: Snapshot): Boolean =
    val normalized = normalize(text)
    if normalized.isEmpty then false
    else
      mentionsCompensationKeyword(normalized) ||
        canonicalSubtype(surface).exists(supportsSubtype(normalized, _))

  def supportsSubtype(text: String, subtype: CompensationSubtype): Boolean =
    val normalized = normalize(text)
    if normalized.isEmpty then false
    else
      val theaterOk =
        subtype.pressureTheater match
          case "queenside" => normalized.contains("queenside")
          case "center"    => normalized.contains("center") || normalized.contains("central")
          case "kingside"  => normalized.contains("kingside")
          case _           => true
      val modeOk =
        subtype.pressureMode match
          case "line_occupation" =>
            containsAny(
              normalized,
              List("line pressure", "file pressure", "open file", "open files", "along the files", "queenside files", "central files")
            )
          case "target_fixing" =>
            containsAny(
              normalized,
              List("fixed target", "fixed targets", "targets tied down", "fixed pawn", "weak pawn", "tied down")
            )
          case "counterplay_denial" =>
            containsAny(normalized, List("counterplay", "deny", "denying", "restrict", "restricting", "stop the counterplay"))
          case "defender_tied_down" =>
            containsAny(normalized, List("tied down", "defenders tied down", "defender tied down"))
          case "conversion_window" =>
            containsAny(normalized, List("convert", "conversion", "cash out", "transition"))
          case "break_preparation" =>
            containsAny(normalized, List("break", "pawn break"))
          case _ => mentionsCompensationKeyword(normalized)
      val recoveryOk =
        subtype.recoveryPolicy match
          case "immediate" => true
          case _ =>
            containsAny(
              normalized,
              List(
                "material can wait",
                "winning the material back",
                "recover the material",
                "recover material",
                "before taking the material"
              )
            ) || mentionsCompensationKeyword(normalized)
      val stabilityOk =
        subtype.stabilityClass match
          case "transition_only" => containsAny(normalized, List("convert", "conversion", "cash out", "transition"))
          case "durable_pressure" => true
          case _                  => true
      theaterOk && modeOk && recoveryOk && stabilityOk

  def canonicalSubtype(surface: Snapshot): Option[CompensationSubtype] =
    surface.strictCompensationSubtype
      .orElse(surface.displayCompensationSubtype)
      .orElse(surface.effectiveCompensationSubtype)

  private def mentionsCompensationKeyword(text: String): Boolean =
    containsAny(text, CompensationKeywords)

  private def normalize(text: String): String =
    Option(text).getOrElse("").trim.toLowerCase

  private def containsAny(text: String, needles: List[String]): Boolean =
    needles.exists(text.contains)

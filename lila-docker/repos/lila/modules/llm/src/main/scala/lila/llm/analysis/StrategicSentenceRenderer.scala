package lila.llm.analysis

private[analysis] object StrategicSentenceRenderer:

  private val PieceHeadForPattern = """(?i)^the ([a-z]+) can head for ([a-h][1-8])$""".r
  private val PieceTowardPattern = """(?i)^([a-z]+) toward ([a-h][1-8])(?:\s+(?:for|to)\s+.+)?$""".r
  private val PieceViaPathPattern = """(?i)^([a-z]+) via ([a-h][1-8](?:-[a-h][1-8])+)(?:\s+(?:for|to)\s+.+)?$""".r

  def pieceHeadFor(raw: String): Option[(String, String)] =
    normalizeText(raw) match
      case PieceHeadForPattern(piece, square) => Some(piece -> square)
      case _                                  => None

  def pieceToward(raw: String): Option[(String, String)] =
    normalizeText(raw) match
      case PieceTowardPattern(piece, square) => Some(piece -> square)
      case _                                 => None

  def pieceViaPath(raw: String): Option[(String, String)] =
    normalizeText(raw) match
      case PieceViaPathPattern(piece, path) => Some(piece -> path)
      case _                                => None

  def renderExecutionSupport(execution: String): String =
    LiveNarrativeCompressionCore.rewritePlayerLanguage(s"A likely follow-up is $execution.")

  def renderObjectiveSupport(objective: String): String =
    pieceHeadFor(objective)
      .map { case (piece, square) => s"A concrete target is $square for the $piece." }
      .getOrElse(LiveNarrativeCompressionCore.rewritePlayerLanguage(s"A concrete target is ${normalizeText(objective)}."))

  def renderObjectiveClaim(objective: String): String =
    pieceHeadFor(objective)
      .map { case (piece, square) => s"The move is mainly about bringing the $piece to $square." }
      .getOrElse {
        val normalized = normalizeText(objective)
        if normalized.toLowerCase.startsWith("pressure ") then s"The move is mainly about $normalized."
        else s"The move is mainly about $normalized."
      }

  def renderCompensationClaimFromObjective(objective: String): String =
    pieceHeadFor(objective)
      .map { case (piece, square) =>
        s"The point is to keep the $piece headed for $square before winning the material back."
      }
      .getOrElse {
        val normalized = normalizeText(objective)
        if normalized.toLowerCase.startsWith("pressure ") then
          s"The point is to keep $normalized before winning the material back."
        else s"The point is to keep $normalized in view before winning the material back."
      }

  def renderCompensationClaimFromExecution(execution: String): String =
    pieceToward(execution)
      .map { case (piece, square) =>
        s"The move gives up material to keep the $piece headed for $square rather than winning it back right away."
      }
      .orElse {
        pieceViaPath(execution).map { case (piece, path) =>
          val square = path.split("-").lastOption.getOrElse("")
          if square.nonEmpty then
            s"The move gives up material to keep the $piece headed for $square rather than winning it back right away."
          else
            s"The move gives up material to keep ${normalizeText(execution)} in play rather than winning it back right away."
        }
      }
      .getOrElse(s"The move gives up material to keep ${normalizeText(execution)} in play rather than winning it back right away.")

  def renderCompensationSupportFromObjective(objective: String): String =
    pieceHeadFor(objective)
      .map { case (piece, square) => s"Bringing the $piece to $square matters more than grabbing the material back right away." }
      .getOrElse {
        val normalized = normalizeText(objective)
        if normalized.toLowerCase.startsWith("pressure ") then
          s"This works only while $normalized is still there."
        else s"The point is to keep $normalized in play before winning the material back."
      }

  def renderCompensationFollowUpFromExecution(execution: String): Option[String] =
    pieceToward(execution)
      .map { case (piece, square) => s"A likely follow-up is bringing the $piece to $square." }
      .orElse {
        pieceViaPath(execution).flatMap { case (piece, path) =>
          path.split("-").lastOption.filter(_.nonEmpty).map(square => s"A likely follow-up is bringing the $piece to $square.")
        }
      }
      .orElse {
        val normalized = normalizeText(execution)
        Option.when(LiveNarrativeCompressionCore.hasConcreteAnchor(normalized)) {
          LiveNarrativeCompressionCore.rewritePlayerLanguage(s"A likely follow-up is $normalized.")
        }
      }

  private def normalizeText(raw: String): String =
    Option(raw).getOrElse("").replaceAll("""[_\-]+""", " ").replaceAll("\\s+", " ").trim

package lila.llm.analysis

/**
 * TraceRecorder records which context fields were used/dropped during rendering.
 *
 * This is primarily a developer/debugging tool used by `BookStyleRenderer.renderTrace`.
 */
final case class TraceEntry(field: String, status: String, value: String, reason: String)

/**
 * Active Trace Recorder to prevent drift and manage global state (mentions).
 */
final class TraceRecorder:
  private val entries = List.newBuilder[TraceEntry]
  private val mentionedTokens = scala.collection.mutable.Set[String]()

  def use(field: String, value: Any, reason: String): Unit =
    entries += TraceEntry(field, "USED", value.toString, reason)

  def drop(field: String, value: Any, reason: String): Unit =
    entries += TraceEntry(field, "DROPPED", value.toString, reason)

  def unused(field: String, value: Any, reason: String): Unit =
    entries += TraceEntry(field, "UNUSED", value.toString, reason)

  def empty(field: String, reason: String): Unit =
    entries += TraceEntry(field, "EMPTY", "-", reason)

  /** Checks if a text contains already mentioned tokens. */
  def isRedundant(text: String): Boolean =
    val tokens = extractTokens(text)
    tokens.nonEmpty && tokens.forall(mentionedTokens.contains)

  /** Checks if a field has been logged as USED. */
  def isUsed(field: String): Boolean =
    entries.result().exists(e => e.field == field && e.status == "USED")

  /** Registers tokens from text as mentioned. */
  def logMentions(text: String): Unit =
    mentionedTokens ++= extractTokens(text)

  private def extractTokens(text: String): Set[String] =
    // Tokenize squares (e4), files (c-file), and key concepts
    val squares = "[a-h][1-8]".r.findAllIn(text).toSet
    val files = "[a-h]-file".r.findAllIn(text).toSet
    val plans = List("kingside", "queenside", "center", "attack", "defense").filter(text.contains).toSet
    squares ++ files ++ plans

  def renderTable: String =
    val header = "| Field | Status | Value | Reason |\n|-------|--------|-------|--------|"
    val rows = entries.result().map(e => s"| ${e.field} | ${e.status} | ${e.value.take(100)} | ${e.reason} |")
    (header :: rows).mkString("\n")


package lila.llm.analysis

/**
 * TraceRecorder records which context fields were used/dropped during rendering.
 */
final case class TraceEntry(field: String, status: String, value: String, reason: String)

/**
 * Active Trace Recorder to prevent drift.
 */
final class TraceRecorder:
  private val entries = List.newBuilder[TraceEntry]

  def use(field: String, value: Any, reason: String): Unit =
    entries += TraceEntry(field, "USED", value.toString, reason)

  def drop(field: String, value: Any, reason: String): Unit =
    entries += TraceEntry(field, "DROPPED", value.toString, reason)

  def unused(field: String, value: Any, reason: String): Unit =
    entries += TraceEntry(field, "UNUSED", value.toString, reason)

  def empty(field: String, reason: String): Unit =
    entries += TraceEntry(field, "EMPTY", "-", reason)

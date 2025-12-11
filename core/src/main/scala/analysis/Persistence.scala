package chess
package analysis

import java.io._
import java.nio.file._
import scala.concurrent.{Future, ExecutionContext}
import scala.util.Try

object Persistence:

  private val ANALYSIS_DIR = "analysis"
  
  // --- Retry Helper ---
  private def withRetry[T](op: => T, retries: Int = 3, delay: Long = 100): T =
    try op
    catch
      case e: IOException if retries > 0 =>
        System.err.println(s"[Persistence] I/O error, retrying in ${delay}ms: ${e.getMessage}")
        Thread.sleep(delay)
        withRetry(op, retries - 1, delay * 2)
      case e: Throwable => throw e
  
  def init(): Unit =
    withRetry(Files.createDirectories(Paths.get(ANALYSIS_DIR)))

  // --- Analysis Results ---
  def saveAnalysisJson(id: String, json: String): Unit =
    try
      withRetry {
        Files.createDirectories(Paths.get(ANALYSIS_DIR))
        Files.writeString(Paths.get(ANALYSIS_DIR, s"$id.json"), json)
      }
    catch
      case e: Throwable => System.err.println(s"Failed to save analysis $id: ${e.getMessage}")

  /** Async version of saveAnalysisJson - non-blocking */
  def saveAnalysisJsonAsync(id: String, json: String)(using ec: ExecutionContext): Future[Unit] =
    Future(saveAnalysisJson(id, json))

  def loadAnalysisJson(id: String): Option[String] =
    try
      val path = Paths.get(ANALYSIS_DIR, s"$id.json")
      if Files.exists(path) then Some(withRetry(Files.readString(path))) else None
    catch
      case _: Throwable => None

  /** Safe JSON loading with parsing - returns Either for error handling */
  def loadAnalysisJsonSafe(id: String): Either[String, ujson.Value] =
    loadAnalysisJson(id) match
      case Some(str) => 
        Try(ujson.read(str)).toEither.left.map(e => s"Parse error: ${e.getMessage}")
      case None => 
        Left(s"Analysis $id not found")

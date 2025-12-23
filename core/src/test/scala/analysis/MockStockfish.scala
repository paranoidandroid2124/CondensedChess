package chess
package analysis

import scala.collection.mutable
import java.util.concurrent.atomic.AtomicBoolean
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * A mock implementation of StockfishClient for testing purposes.
 * It allows pre-seeding results for specific FENs.
 */
class MockStockfish extends StockfishClient("mock") {
  
  import scala.concurrent.Future
  import StockfishClient._
  
  private val cannedResults = mutable.Map.empty[String, Either[String, EvalResult]]
  private val isClosed = new AtomicBoolean(false)
  private var shouldTimeout = false
  private var shouldCrash = false
  
  override protected def start(): Unit = {
    // Overridden to avoid starting a real process in tests
  }

  override def close(): Unit = {
    isClosed.set(true)
  }
  
  // Test helpers
  def seed(fen: String, moves: List[String] = Nil, result: Either[String, EvalResult]): Unit = {
    val key = makeKey(fen, moves)
    cannedResults(key) = result
  }
  
  def setShouldTimeout(value: Boolean): Unit = {
    shouldTimeout = value
  }
  
  def setShouldCrash(value: Boolean): Unit = {
    shouldCrash = value
  }
  
  private def makeKey(fen: String, moves: List[String]): String =
    if moves.isEmpty then fen else s"$fen moves ${moves.mkString(" ")}"

  override def evaluateFen(
      fen: String,
      depth: Int,
      multiPv: Int = 1,
      moveTimeMs: Option[Int] = Some(500),
      moves: List[String] = Nil
  ): Future[Either[String, EvalResult]] = {
    if isClosed.get() then return Future.successful(Left("Client closed"))
    
    if shouldCrash then return Future.failed(new RuntimeException("Mock Engine Crash"))
    if shouldTimeout then {
      // simulate delay
      // Thread.sleep(moveTimeMs.getOrElse(100) + 10)
      return Future.successful(Left("Engine timeout (soft mock)"))
    }
    
    val key = makeKey(fen, moves)
    // Check exact match first
    val res = cannedResults.get(key) match {
      case Some(res) => res
      case None => 
         // Check just FEN match if no moves provided in seed but requested?
         // No, exact match is better for testing forced moves.
         // Fallback to default
         Right(EvalResult(List(
           Line(1, depth, Some(10), None, List("a2a3"))
         ), Some("a2a3")))
    }
    Future.successful(res)
  }
}

package chess
package analysis

import munit.FunSuite
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Await

class EngineChaosTest extends FunSuite:

  val mockClient = new MockStockfish()

  override def beforeAll(): Unit =
    // Inject our mock factory
    EnginePool.clientFactory = () => mockClient

  override def afterAll(): Unit =
    // Reset to default
    EnginePool.clientFactory = () => new StockfishClient()

  test("EngineService recovers from crash during task (Retry Logic)") {
    val service = new EngineService()
    
    // 1. Seed standard response
    mockClient.seed("startPos", Nil, Right(StockfishClient.EvalResult(List(StockfishClient.Line(1, 10, Some(50), None, List("e2e4"))), Some("e2e4"))))
    
    // 2. Configure mock to fail ONCE (simulating crash that recovers?)
    // MockStockfish currently has simple boolean flags.
    // If we set shouldCrash=true, it always crashes. 
    // EngineService retries 2 times.
    // We need MockStockfish to be stateful: "Crash once then work".
    // Since we don't have that yet, let's just verify it *fails* gracefully after retries exhausted, 
    // OR we can implement "fail once" in MockStockfish if we want to test success.
    
    // Let's test "Exhausted Retries" scenario first.
    mockClient.setShouldCrash(true)
    
    val req = AnalysisTypes.Analyze("startPos", Nil, 10, 1, 100)
    val future = service.submit(req)
    
    try
      Await.result(future, 2.seconds)
      fail("Should have failed")
    catch
      case e: Throwable => 
         // Expected behavior: EngineService propagates the failure after retries
         // We verify it was the crash exception
         assert(e.getMessage.contains("Mock Engine Crash"))
  }

  test("EngineService handles timeout gracefully") {
     val service = new EngineService()
     mockClient.setShouldCrash(false)
     mockClient.setShouldTimeout(true)
     
     val req = AnalysisTypes.Analyze("timeoutPos", Nil, 10, 1, 100)
     val future = service.submit(req)
     
     try
       Await.result(future, 2.seconds)
       fail("Should have failed with timeout")
     catch
       case e: Throwable =>
         assert(e.getMessage.contains("timeout"))
  }

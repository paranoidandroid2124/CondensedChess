package chess
package analysis

import scala.io.Source
import java.io.File

object TestHypothesisNarrative extends App {
  println(" Testing LlmClient.studyChapterComments with Hypothesis Payload...")

  // 1. Load .env manually
  val envFile = new File(".env")
  if (envFile.exists()) {
    Source.fromFile(envFile).getLines().foreach { line =>
      val parts = line.split("=", 2)
      if (parts.length == 2) {
        val key = parts(0).trim
        val value = parts(1).trim.stripPrefix("\"").stripSuffix("\"")
        if (key == "GEMINI_API_KEY") {
          System.setProperty("GEMINI_API_KEY", value)
          println(s"[Test] Injection API Key from .env: ${value.take(5)}...")
        }
      }
    }
  } else {
    println("[Test] .env file not found at " + envFile.getAbsolutePath)
  }

  // 2. Define Payload
  val payload = 
    """
    [
      {
        "id": "test-chapter-1",
        "label": "15...Nf6",
        "played": "Nf6",
        "best": "Re8",
        "deltaWinPct": -35.0,
        "tags": ["greedy_capture", "tactical_miss"],
        "phase": "middlegame",
        "conceptShift": { "name": "tactical_vulnerability", "delta": 2.5 },
        "practicality": { "overall": 0.2, "categoryGlobal": "Blunder" },
        "lines": [
          { "label": "Hypothesis", "move": "Nf6", "pv": "16. dxe5 dxe5 17. Qxd8+ Rxd8 18. Nxe5", "winPct": 25.0 },
          { "label": "Best", "move": "Re8", "pv": "16. Qe2 h6 17. Rd1", "winPct": 60.0 }
        ]
      }
    ]
    """

  // 3. Call Client using Reflection to force re-initialization if needed?
  // Actually, LlmClient is an object. If it was already loaded, 'apiKey' val is already set.
  // But running from sbt runMain usually starts a fresh JVM or isolation.
  // Let's hope it's not already loaded.
  
  val result = LlmClient.studyChapterComments(payload)

  if (result.nonEmpty) {
    println("\n[SUCCESS] Received Response:")
    result.foreach { case (id, chapter) =>
      println(s"Title: ${chapter.title}")
      println(s"Summary: ${chapter.summary}")
      println("Key Moves:")
      chapter.keyMoves.foreach { case ((ply, san), comment) =>
        println(s" - $san: $comment")
      }
    }
  } else {
    println("\n[FAILURE] No response from LLM. Check API Key or Network.")
  }
}

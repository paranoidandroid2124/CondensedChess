package lila.llm

import lila.llm.analysis.BookStyleRenderer

object DiversityDemonstrator {
  def main(args: Array[String]): Unit = {
    println("================================================================================")
    println("CRITICAL VERIFICATION: OUTPUT GENERATION")
    println("================================================================================")

    try {
      // 1. Tactical
      val tactical = DiversityTestHelper.tacticalCrisis
      println("\n[SCENARIO 1: TACTICAL]")
      println(s"Context Phase: ${tactical.phase.current}")
      println(s"Threats: ${tactical.threats.toUs.map(_.kind).mkString}")
      println("--- GENERATED TEXT ---")
      println(BookStyleRenderer.render(tactical))
      println("-----------------------")

      // 2. Strategic
      val strategic = DiversityTestHelper.strategicQuiet
      println("\n[SCENARIO 2: STRATEGIC]")
      println(s"Context Phase: ${strategic.phase.current}")
      println(s"Top Plan: ${strategic.plans.top5.headOption.map(_.name).getOrElse("None")}")
      println("--- GENERATED TEXT ---")
      println(BookStyleRenderer.render(strategic))
      println("-----------------------")

      // 3. Endgame
      val endgame = DiversityTestHelper.endgame
      println("\n[SCENARIO 3: ENDGAME]")
      println(s"Context Phase: ${endgame.phase.current}")
      println(s"Top Plan: ${endgame.plans.top5.headOption.map(_.name).getOrElse("None")}")
      println("--- GENERATED TEXT ---")
      println(BookStyleRenderer.render(endgame))
      println("-----------------------")

    } catch {
      case e: Throwable => 
        e.printStackTrace()
    }
    println("================================================================================")
  }
}

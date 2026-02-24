package lila.llm.analysis

import munit.FunSuite

class EndgamePatternLabelTest extends FunSuite:

  test("pattern detector should match v2 goldset labels") {
    val errors = scala.collection.mutable.ListBuffer.empty[String]
    EndgamePatternGoldsetSupport.rows.foreach { row =>
      row.cases.foreach { c =>
        EndgamePatternGoldsetSupport.evaluateCase(c) match
          case Left(err) => errors += err
          case Right(eval) =>
            val predictedLabel = eval.finalFeature.primaryPattern.getOrElse("None")
            if predictedLabel != c.expectedLabel then
              errors +=
                s"${c.id} (${row.pattern}/${c.kind}) expectedLabel=${c.expectedLabel} predicted=$predictedLabel coreHint=${eval.core.theoreticalOutcomeHint} coreConf=${eval.core.confidence} finalConf=${eval.finalFeature.confidence}"
      }
    }
    if errors.nonEmpty then fail(errors.mkString("\n"))
  }


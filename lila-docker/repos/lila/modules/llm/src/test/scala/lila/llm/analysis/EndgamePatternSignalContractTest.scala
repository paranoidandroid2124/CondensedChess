package lila.llm.analysis

import munit.FunSuite
import play.api.libs.json.JsObject

class EndgamePatternSignalContractTest extends FunSuite:

  private val deterministicSignalKeysByPattern: Map[String, Set[String]] = Map(
    "Lucena" -> Set("rookEndgamePattern", "theoreticalOutcomeHint"),
    "PhilidorDefense" -> Set("rookEndgamePattern", "theoreticalOutcomeHint"),
    "VancuraDefense" -> Set("rookEndgamePattern", "theoreticalOutcomeHint"),
    "WrongRookPawnWrongBishopFortress" -> Set("theoreticalOutcomeHint"),
    "OutsidePasserDecoy" -> Set("theoreticalOutcomeHint"),
    "ConnectedPassers" -> Set("theoreticalOutcomeHint"),
    "KeySquaresOppositionBreakthrough" -> Set("zugzwangLikely", "theoreticalOutcomeHint"),
    "TriangulationZugzwang" -> Set("triangulationAvailable", "zugzwangLikely"),
    "BreakthroughSacrifice" -> Set("zugzwangLikely", "theoreticalOutcomeHint"),
    "Shouldering" -> Set("zugzwangLikely"),
    "ShortSideDefense" -> Set("rookEndgamePattern", "theoreticalOutcomeHint"),
    "OppositeColoredBishopsDraw" -> Set("theoreticalOutcomeHint"),
    "GoodBishopRookPawnConversion" -> Set("theoreticalOutcomeHint"),
    "KnightBlockadeRookPawnDraw" -> Set("theoreticalOutcomeHint")
  )

  test("pattern detector should satisfy deterministic signal contracts") {
    val errors = scala.collection.mutable.ListBuffer.empty[String]
    EndgamePatternGoldsetSupport.rows.foreach { row =>
      row.cases.foreach { c =>
        EndgamePatternGoldsetSupport.evaluateCase(c) match
          case Left(err) => errors += err
          case Right(eval) =>
            val expectedLabel = c.expectedLabel
            if expectedLabel == "None" then
              if eval.finalFeature != eval.core then
                errors += s"${c.id} (${row.pattern}/${c.kind}) expected no override but final feature changed"
            else
              val deterministicSignals = selectDeterministicSignals(row, c)
              if deterministicSignals.value.nonEmpty then
                val (checks, matches, mismatches) =
                  EndgamePatternGoldsetSupport.compareSignals(deterministicSignals, eval.finalFeature)
                if matches != checks then
                  errors +=
                    s"${c.id} (${row.pattern}/${c.kind}) signalMismatch=$matches/$checks label=${eval.finalFeature.primaryPattern.getOrElse("None")} details=${mismatches.mkString(" | ")}"
      }
    }
    if errors.nonEmpty then fail(errors.mkString("\n"))
  }

  private def selectDeterministicSignals(
      row: EndgamePatternGoldsetSupport.PatternRow,
      c: EndgamePatternGoldsetSupport.PatternCase
  ): JsObject =
    val merged = EndgamePatternGoldsetSupport.expectedSignals(row, c)
    val allowed = deterministicSignalKeysByPattern.getOrElse(row.pattern, Set.empty)
    JsObject(merged.value.filter((key, _) => allowed.contains(key)))


package lila.llm.analysis

import munit.FunSuite
import lila.llm.model.authoring.*

class NarrativeOutlineValidatorTest extends FunSuite:

  test("validate strips shared lesson sentences and helper-label prefixes from released beats") {
    val outline = NarrativeOutline(
      List(
        OutlineBeat(
          kind = OutlineBeatKind.OpeningTheory,
          text = "Reference paths still matter. Shared lesson: central control decides everything."
        ),
        OutlineBeat(
          kind = OutlineBeatKind.Context,
          text = "alignment intent: pressure on e6."
        )
      )
    )

    val validated = NarrativeOutlineValidator.validate(outline, new TraceRecorder())
    val opening = validated.beats.find(_.kind == OutlineBeatKind.OpeningTheory).getOrElse(fail("missing opening beat"))
    val context = validated.beats.find(_.kind == OutlineBeatKind.Context).getOrElse(fail("missing context beat"))

    assertEquals(opening.text, "Reference paths still matter.")
    assertEquals(context.text, "pressure on e6.")
  }

  test("validate drops beats that collapse after authority leak cleanup") {
    val outline = NarrativeOutline(
      List(
        OutlineBeat(
          kind = OutlineBeatKind.TeachingPoint,
          text = "Shared lesson: central control decides everything."
        )
      )
    )

    val validated = NarrativeOutlineValidator.validate(outline, new TraceRecorder())

    assertEquals(validated.beats, Nil)
  }

  test("validate keeps admissible opening-theory generalization but drops the same pattern when unanchored in context") {
    val outline = NarrativeOutline(
      List(
        OutlineBeat(
          kind = OutlineBeatKind.OpeningTheory,
          text = "Across these branches, results changed by which side better handled central control."
        ),
        OutlineBeat(
          kind = OutlineBeatKind.Context,
          text = "Across these branches, results changed by which side better handled central control."
        )
      )
    )

    val validated = NarrativeOutlineValidator.validate(outline, new TraceRecorder())
    val opening = validated.beats.find(_.kind == OutlineBeatKind.OpeningTheory).getOrElse(fail("missing opening beat"))

    assertEquals(opening.text, "Across these branches, results changed by which side better handled central control.")
    assert(!validated.beats.exists(_.kind == OutlineBeatKind.Context), clue(validated.beats))
  }

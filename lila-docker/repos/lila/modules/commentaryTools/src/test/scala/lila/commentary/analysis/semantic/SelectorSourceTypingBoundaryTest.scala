package lila.commentary.analysis.semantic

import java.nio.charset.StandardCharsets
import java.nio.file.{ Files, Paths }

import munit.FunSuite

class SelectorSourceTypingBoundaryTest extends FunSuite:

  private def selectorSource: String =
    Files.readString(
      Paths.get("modules/commentaryCore/src/main/scala/lila/commentary/analysis/StrategicIdeaSelector.scala"),
      StandardCharsets.UTF_8
    )

  private def read(path: String): String =
    Files.readString(Paths.get(path), StandardCharsets.UTF_8)

  test("StrategicIdeaSelector does not mint or inspect selector sources as raw strings") {
    val text = selectorSource
    val banned =
      List(
        "source = \"" -> "use EvidenceSourceId.* in evidence(...) calls",
        "candidateHasSource(candidate, \"" -> "use typed candidateHasSource(candidate, EvidenceSourceId.*)",
        "ideaRefs.contains(\"source:" -> "use EvidenceRef.Source(EvidenceSourceId.*).wireKey"
      )

    val hits =
      banned.collect {
        case (needle, reason) if text.contains(needle) => s"$needle : $reason"
      }

    assertEquals(hits, Nil)
  }

  test("StrategicIdeaSelector no longer owns slow-structural detectors") {
    val text = selectorSource
    val banned =
      List(
        "def collectSpaceEvidence" -> "space evidence belongs in the typed evidence producer pipeline",
        "def collectLineOccupationEvidence" -> "line evidence belongs in the typed evidence producer pipeline",
        "def collectOutpostEvidence" -> "outpost evidence belongs in the typed evidence producer pipeline",
        "def collectMinorPieceImbalanceEvidence" -> "minor-piece evidence belongs in the typed evidence producer pipeline"
      )

    val hits =
      banned.collect {
        case (needle, reason) if text.contains(needle) => s"$needle : $reason"
      }

    assertEquals(hits, Nil)
  }

  test("StrategicIdeaSelector no longer owns theme detectors") {
    val text = selectorSource
    val banned =
      List(
        "def collectProphylaxisEvidence" -> "prophylaxis evidence belongs in the typed evidence producer pipeline",
        "def collectKingAttackEvidence" -> "king-attack evidence belongs in the typed evidence producer pipeline",
        "def collectFavorableTradeEvidence" -> "transformation evidence belongs in the typed evidence producer pipeline",
        "def collectCounterplaySuppressionEvidence" -> "counterplay evidence belongs in the typed evidence producer pipeline"
      )

    val hits =
      banned.collect {
        case (needle, reason) if text.contains(needle) => s"$needle : $reason"
      }

    assertEquals(hits, Nil)
  }

  test("selector evidence producers are split by role under semantic evidence package") {
    val root = Paths.get("")
    val expected =
      List(
        "modules/commentaryCore/src/main/scala/lila/commentary/analysis/semantic/evidence/StructuralSpaceEvidenceProducer.scala",
        "modules/commentaryCore/src/main/scala/lila/commentary/analysis/semantic/evidence/LineOccupationEvidenceProducer.scala",
        "modules/commentaryCore/src/main/scala/lila/commentary/analysis/semantic/evidence/OutpostEvidenceProducer.scala",
        "modules/commentaryCore/src/main/scala/lila/commentary/analysis/semantic/evidence/MinorPieceImbalanceEvidenceProducer.scala",
        "modules/commentaryCore/src/main/scala/lila/commentary/analysis/semantic/evidence/ProphylaxisEvidenceProducer.scala",
        "modules/commentaryCore/src/main/scala/lila/commentary/analysis/semantic/evidence/KingAttackEvidenceProducer.scala",
        "modules/commentaryCore/src/main/scala/lila/commentary/analysis/semantic/evidence/TransformationEvidenceProducer.scala",
        "modules/commentaryCore/src/main/scala/lila/commentary/analysis/semantic/evidence/CounterplayEvidenceProducer.scala",
        "modules/commentaryCore/src/main/scala/lila/commentary/analysis/semantic/evidence/StrategicIdeaEvidenceSupport.scala"
      )
    val missing = expected.filterNot(path => Files.exists(root.resolve(path)))
    val obsolete =
      List(
        "modules/commentaryCore/src/main/scala/lila/commentary/analysis/semantic/SlowStructuralEvidenceProducer.scala",
        "modules/commentaryCore/src/main/scala/lila/commentary/analysis/semantic/StrategicIdeaEvidenceProducers.scala"
      ).filter(path => Files.exists(root.resolve(path)))

    assertEquals(missing, Nil)
    assertEquals(obsolete, Nil)
  }

  test("typed selector evidence boundary uses stable role names") {
    val runtimeAndDocs =
      List(
        "modules/commentaryCore/src/main/scala/lila/commentary/analysis/semantic/StrategicIdeaEvidencePipeline.scala",
        "modules/commentaryCore/src/main/scala/lila/commentary/analysis/semantic/evidence/StructuralSpaceEvidenceProducer.scala",
        "modules/commentaryCore/src/main/scala/lila/commentary/analysis/semantic/evidence/LineOccupationEvidenceProducer.scala",
        "modules/commentaryCore/src/main/scala/lila/commentary/analysis/semantic/evidence/OutpostEvidenceProducer.scala",
        "modules/commentaryCore/src/main/scala/lila/commentary/analysis/semantic/evidence/MinorPieceImbalanceEvidenceProducer.scala",
        "modules/commentaryCore/src/main/scala/lila/commentary/analysis/semantic/evidence/ProphylaxisEvidenceProducer.scala",
        "modules/commentaryCore/src/main/scala/lila/commentary/analysis/semantic/evidence/KingAttackEvidenceProducer.scala",
        "modules/commentaryCore/src/main/scala/lila/commentary/analysis/semantic/evidence/TransformationEvidenceProducer.scala",
        "modules/commentaryCore/src/main/scala/lila/commentary/analysis/semantic/evidence/CounterplayEvidenceProducer.scala",
        "modules/commentaryCore/src/main/scala/lila/commentary/analysis/semantic/evidence/StrategicIdeaEvidenceSupport.scala",
        "modules/commentary/docs/CommentaryPipelineSSOT.md",
        "modules/commentary/docs/CommentaryTrustBoundary.md"
      ).map(path => path -> read(path))

    val banned =
      List(
        "StrategicIdeaObservationPipeline" -> "selector ranking carriers should use evidence-pipeline naming",
        "StrategicIdeaObservationProducer" -> "selector ranking carriers should use evidence-producer naming",
        "StrategicIdeaThemeObservationProducers" -> "source file names should describe evidence production, not observation rollout",
        "migrated slow structural" -> "docs should name the current role, not the migration step",
        "migrated selector themes" -> "docs should name the current role, not the migration step",
        "pilot" -> "authority docs should name the admitted lane or diagnostic lane directly",
        "Phase 2" -> "authority docs should name the runtime contract instead of roadmap phase",
        "pre-v2" -> "authority docs should describe legacy flat docs without version-rollout labels",
        " v2" -> "authority docs should describe the current nested proof contract without version-rollout labels"
      )

    val hits =
      for
        (path, text) <- runtimeAndDocs
        (needle, reason) <- banned
        if text.contains(needle)
      yield s"$path contains $needle : $reason"

    assertEquals(hits, Nil)
  }

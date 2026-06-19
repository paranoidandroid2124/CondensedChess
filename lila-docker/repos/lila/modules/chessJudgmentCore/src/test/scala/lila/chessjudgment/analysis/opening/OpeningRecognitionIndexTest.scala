package lila.chessjudgment.analysis.opening

import lila.chessjudgment.analysis.assembly.{ MoveReviewInputNormalizer, MoveReviewJudgmentOrchestrator, RawMoveReviewInput }
import lila.chessjudgment.model.judgment.{
  ApplicabilityAssessmentEvidence,
  FeatureAnchorEvidence,
  FeatureApplicability,
  EvidenceRecord,
  OpeningContextEvidence,
  OpeningContextSignal,
  OpeningFamily,
  OpeningRecognitionMatchKind,
  OpeningTheme
}
import lila.chessjudgment.model.strategic.VariationLine

class OpeningRecognitionIndexTest extends munit.FunSuite:

  private val italianPrefix = List("e2e4", "e7e5", "g1f3", "b8c6", "f1c4")
  private val italianFen = "r1bqkbnr/pppp1ppp/2n5/4p3/2B1P3/5N2/PPPP1PPP/RNBQK2R b KQkq - 3 3"

  test("recognizes an opening from move prefix hash and position key without PGN metadata"):
    val index = OpeningRecognitionIndex.fromTsvLines(
      List(
        OpeningRecognitionIndex.TsvHeader,
        List(
          OpeningIndexKeys.movePrefixHash(italianPrefix),
          OpeningIndexKeys.positionKey(italianFen),
          italianPrefix.size.toString,
          "C50",
          "Italian Game",
          "C",
          "open_games/italian",
          "42",
          "50",
          "0.84"
        ).mkString("\t")
      )
    )

    val recognition = index.recognize(italianPrefix, italianFen, italianPrefix.size).get

    assertEquals(recognition.movePrefixHash, OpeningIndexKeys.movePrefixHash(italianPrefix))
    assertEquals(recognition.positionKey, OpeningIndexKeys.positionKey(italianFen))
    assertEquals(recognition.matchedBy, OpeningRecognitionMatchKind.ExactPrefixAndPosition)
    assertEquals(recognition.matchedPly, italianPrefix.size)
    assertEquals(recognition.frequency, 42)
    assertEquals(recognition.sampleCount, 50)
    assertEquals(recognition.confidence, 0.84)
    assertEquals(recognition.bestIdentity.flatMap(_.eco), Some("C50"))
    assertEquals(recognition.bestIdentity.flatMap(_.name), Some("Italian Game"))
    assertEquals(recognition.bestIdentity.flatMap(_.family), Some(OpeningFamily.C))

    val transposedRecognition = index.recognize(List("d2d4", "g8f6"), italianFen, italianPrefix.size).get
    assertEquals(transposedRecognition.matchedBy, OpeningRecognitionMatchKind.PositionTransposition)
    assertEquals(transposedRecognition.bestIdentity.flatMap(_.name), Some("Italian Game"))

  test("loads curated theme priors by lineage before falling back to ECO family"):
    val themeIndex = OpeningThemePriorIndex.fromTsvLines(
      List(
        OpeningThemePriorIndex.TsvHeader,
        "open_games/italian\tC\tCenterControl|Development|KingSafety\topen_center|e4_e5\tc2_c3|d2_d4\tknights_before_bishops|castle_kingside\tfalse\tcentral_break|kingside_pressure",
        "eco_c\tC\tCenterControl|Development\topen_games\td2_d4\tminor_piece_development\tfalse\tcentral_play"
      )
    )

    val prior = themeIndex.priorFor(Some("open_games/italian"), Some(OpeningFamily.C)).get

    assertEquals(prior.lineage, Some("open_games/italian"))
    assertEquals(prior.family, Some(OpeningFamily.C))
    assertEquals(prior.gambitCompensation, false)
    assert(prior.themes.contains(OpeningTheme.CenterControl))
    assert(prior.themes.contains(OpeningTheme.Development))
    assert(prior.centerBreaks.contains("c2_c3"))
    assert(prior.strategicPlanPriors.contains("central_break"))

  test("normalizer derives opening identity and theme prior from prefix when metadata is absent"):
    val recognitionIndex = OpeningRecognitionIndex.fromTsvLines(
      List(
        OpeningRecognitionIndex.TsvHeader,
        List(
          OpeningIndexKeys.movePrefixHash(italianPrefix),
          OpeningIndexKeys.positionKey(italianFen),
          italianPrefix.size.toString,
          "C50",
          "Italian Game",
          "C",
          "open_games/italian",
          "42",
          "50",
          "0.84"
        ).mkString("\t")
      )
    )
    val themeIndex = OpeningThemePriorIndex.fromTsvLines(
      List(
        OpeningThemePriorIndex.TsvHeader,
        "open_games/italian\tC\tCenterControl|Development|KingSafety\topen_center|e4_e5\tc2_c3|d2_d4\tknights_before_bishops|castle_kingside\tfalse\tcentral_break|kingside_pressure"
      )
    )
    val raw = RawMoveReviewInput(
      fen = italianFen,
      playedMoveUci = "g8f6",
      variations = List(VariationLine(moves = List("g8f6"), scoreCp = 20, depth = 12)),
      currentEvalCp = Some(20),
      ply = Some(italianPrefix.size),
      movePrefixUci = italianPrefix
    )

    val normalized = MoveReviewInputNormalizer.normalize(raw, recognitionIndex, themeIndex).get

    assertEquals(normalized.opening.flatMap(_.name), Some("Italian Game"))
    assertEquals(normalized.openingRecognition.flatMap(_.bestIdentity.flatMap(_.eco)), Some("C50"))
    assertEquals(normalized.openingThemePrior.map(_.lineage), Some(Some("open_games/italian")))
    assert(normalized.openingSignals.contains(OpeningContextSignal.RecognizedIdentity))
    assert(normalized.openingSignals.contains(OpeningContextSignal.ThemePrior))

  test("judgment build emits OpeningContextEvidence from the compact default index"):
    val raw = RawMoveReviewInput(
      fen = italianFen,
      playedMoveUci = "g8f6",
      variations = List(VariationLine(moves = List("g8f6"), scoreCp = 20, depth = 12)),
      currentEvalCp = Some(20),
      ply = Some(italianPrefix.size),
      movePrefixUci = italianPrefix
    )

    val graph =
      MoveReviewJudgmentOrchestrator
        .build(raw)
        .get
        .context
        .evidenceGraph
        .records

    val openingContext =
      graph
        .collectFirst { case EvidenceRecord(_, payload: OpeningContextEvidence, _) => payload }
        .get
    val featureAnchors =
      graph.collect { case EvidenceRecord(_, FeatureAnchorEvidence(anchor), _) => anchor }
    val applicability =
      graph.collectFirst { case EvidenceRecord(_, ApplicabilityAssessmentEvidence(assessment), _) => assessment }.get

    assertEquals(openingContext.identity.flatMap(_.name), Some("Italian Game"))
    assertEquals(openingContext.recognition.flatMap(_.bestIdentity.flatMap(_.eco)), Some("C50"))
    assertEquals(openingContext.recognition.map(_.matchedBy), Some(OpeningRecognitionMatchKind.ExactPrefixAndPosition))
    assertEquals(openingContext.themePrior.flatMap(_.lineage), Some("open_games/italian"))
    assert(openingContext.signals.contains(OpeningContextSignal.RecognizedIdentity))
    assert(openingContext.signals.contains(OpeningContextSignal.ThemePrior))
    assert(featureAnchors.exists(_.theme == OpeningTheme.CenterControl))
    assertEquals(applicability.applicability, FeatureApplicability.OpeningRelevant)
    assert(applicability.observedThemes.contains(OpeningTheme.CenterControl))
    assert(openingContext.themePrior.exists(_.themes.contains(OpeningTheme.CenterControl)))

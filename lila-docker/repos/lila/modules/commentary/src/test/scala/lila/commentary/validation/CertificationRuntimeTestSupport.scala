package lila.commentary.validation

import chess.Color
import chess.format.Fen

import lila.commentary.certification.*
import lila.commentary.strategic.{ StrategicObjectExtraction, StrategicObjectExtractor }

object CertificationRuntimeTestSupport:

  private[validation] val rowsById: Map[String, CertificationExpectationCorpus.Row] =
    CertificationExpectationCorpus.loadAll().map(row => row.id -> row).toMap

  private def rowFor(rowId: String): CertificationExpectationCorpus.Row =
    rowsById.getOrElse(rowId, throw IllegalArgumentException(s"Missing certification row $rowId"))

  private[validation] def rowsForFamilies(
      families: Set[String]
  ): Vector[CertificationExpectationCorpus.Row] =
    CertificationExpectationCorpus.loadAll().filter(row => families.contains(row.family))

  private[validation] def ownerColor(row: CertificationExpectationCorpus.Row): Color =
    row.owner match
      case "white" => Color.White
      case "black" => Color.Black
      case other => throw IllegalArgumentException(s"Unsupported owner $other for ${row.id}")

  private[validation] def expectedVerdict(
      row: CertificationExpectationCorpus.Row
  ): CertificationVerdict =
    row.expectation match
      case "certified" => CertificationVerdict.Certified
      case "support_only" => CertificationVerdict.SupportOnly
      case "deferred" => CertificationVerdict.Deferred
      case "rejected" => CertificationVerdict.Rejected
      case other => throw IllegalArgumentException(s"Unsupported expectation $other for ${row.id}")

  private[validation] def objectExtraction(
      row: CertificationExpectationCorpus.Row
  ): StrategicObjectExtraction =
    StrategicObjectExtractor.fromFen(row.normalizedFen).fold(
      message => throw IllegalStateException(s"${row.id} object extraction failed: $message"),
      identity
    )

  private[validation] def evidenceBundleFor(
      row: CertificationExpectationCorpus.Row
  ): CertificationEvidenceBundle =
    val current = objectExtraction(row)
    val strength =
      if row.caseType == "best_defense_breaks_claim" then CertificationEvidenceStrength.Insufficient
      else CertificationEvidenceStrength.Satisfied
    val purposes =
      row.validatedEnginePurposes.map: purposeKey =>
        CertificationEvidencePurpose
          .fromKey(purposeKey)
          .map(_ -> strength)
          .getOrElse(throw IllegalArgumentException(s"Unsupported evidence purpose $purposeKey for ${row.id}"))

    // Test scaffold only: corpus metadata synthesizes explicit evidence strengths.
    CertificationEvidenceBundle.forObjectExtraction(
      current,
      Vector(
        CertificationEvidence(
          familyId = CertificationId(row.family),
          color = ownerColor(row),
          purposeStrengths = purposes.toMap
        )
      )
    )

  private[validation] def extractionFor(
      row: CertificationExpectationCorpus.Row
  ): CertificationExtraction =
    CertificationExtractor
      .fromObjectExtraction(objectExtraction(row), evidenceBundleFor(row))
      .fold(message => throw IllegalStateException(s"${row.id} certification extraction failed: $message"), identity)

  private[validation] def familyClaims(
      extraction: CertificationExtraction,
      row: CertificationExpectationCorpus.Row
  ): Vector[Certification] =
    extraction.claims.forFamilyId(row.family)

  private[validation] def ownerClaim(
      extraction: CertificationExtraction,
      row: CertificationExpectationCorpus.Row
  ): Certification =
    familyClaims(extraction, row).find(_.owner.contains(ownerColor(row))).getOrElse(
      throw IllegalStateException(s"Missing ${row.family} owner-side certification for ${row.id}")
    )

  private[validation] def rivalClaim(
      extraction: CertificationExtraction,
      row: CertificationExpectationCorpus.Row
  ): Certification =
    familyClaims(extraction, row).find(_.owner.contains(!ownerColor(row))).getOrElse(
      throw IllegalStateException(s"Missing ${row.family} rival-side certification for ${row.id}")
    )

  def objectExtractionForRowId(rowId: String): StrategicObjectExtraction =
    objectExtraction(rowFor(rowId))

  def objectExtractionFromFen(fen: String): StrategicObjectExtraction =
    StrategicObjectExtractor.fromFen(Fen.Full.clean(fen)).fold(
      message => throw IllegalStateException(s"object extraction failed for $fen: $message"),
      identity
    )

  def evidenceBundleForRowId(rowId: String): CertificationEvidenceBundle =
    evidenceBundleFor(rowFor(rowId))

  def evidenceBundleForFamily(
      current: StrategicObjectExtraction,
      familyId: String,
      color: Color,
      purposeStrengths: Map[CertificationEvidencePurpose, CertificationEvidenceStrength]
  ): CertificationEvidenceBundle =
    CertificationEvidenceBundle.forObjectExtraction(
      current,
      Vector(
        CertificationEvidence(
          familyId = CertificationId(familyId),
          color = color,
          purposeStrengths = purposeStrengths
        )
      )
    )

  def extractionForRowId(rowId: String): CertificationExtraction =
    extractionFor(rowFor(rowId))

  def extractionFromFen(
      fen: String,
      evidenceBundle: CertificationEvidenceBundle
  ): CertificationExtraction =
    CertificationExtractor
      .fromObjectExtraction(objectExtractionFromFen(fen), evidenceBundle)
      .fold(message => throw IllegalStateException(s"certification extraction failed for $fen: $message"), identity)

  def familyForRowId(rowId: String): String =
    rowFor(rowId).family

  def ownerColorForRowId(rowId: String): Color =
    ownerColor(rowFor(rowId))

  def expectedVerdictForRowId(rowId: String): CertificationVerdict =
    expectedVerdict(rowFor(rowId))

  def scopeForRowId(rowId: String): String =
    rowFor(rowId).scope

  def burdenTagForRowId(rowId: String): String =
    rowFor(rowId).burdenTag

  def helperTagsForRowId(rowId: String): Vector[String] =
    rowFor(rowId).validatedHelpers

  def evidencePurposesForRowId(rowId: String): Vector[String] =
    rowFor(rowId).validatedEnginePurposes

  def supportFamiliesForRowId(rowId: String): Vector[String] =
    rowFor(rowId).validatedRequiredSupportFamilies

  def familyClaimsForRowId(
      extraction: CertificationExtraction,
      rowId: String
  ): Vector[Certification] =
    familyClaims(extraction, rowFor(rowId))

  def ownerClaimForRowId(
      extraction: CertificationExtraction,
      rowId: String
  ): Certification =
    ownerClaim(extraction, rowFor(rowId))

  def rivalClaimForRowId(
      extraction: CertificationExtraction,
      rowId: String
  ): Certification =
    rivalClaim(extraction, rowFor(rowId))

  def ownerClaimForFamily(
      extraction: CertificationExtraction,
      familyId: String,
      color: Color
  ): Certification =
    extraction.claims
      .forFamilyId(familyId)
      .find(_.owner.contains(color))
      .getOrElse(throw IllegalStateException(s"Missing $familyId owner-side claim for ${if color.white then "white" else "black"}"))

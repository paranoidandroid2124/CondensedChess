package lila.commentary.analysis.claim

import lila.commentary.analysis.OpeningNameLookup
import lila.commentary.analysis.OpeningFamilyCatalog

private[commentary] object OpeningFamilyClaimResolver:

  final case class OpeningFamilyMatchProof(
      opening: Option[String],
      phase: String,
      ply: Int,
      fen: Option[String]
  )

  enum OpeningFamilyId(val wireKey: String):
    case OpenGames extends OpeningFamilyId("open_games")
    case Sicilian extends OpeningFamilyId("sicilian")
    case French extends OpeningFamilyId("french")
    case CaroKann extends OpeningFamilyId("caro_kann")
    case Scandinavian extends OpeningFamilyId("scandinavian")
    case NimzoIndian extends OpeningFamilyId("nimzo_indian")
    case KingsIndian extends OpeningFamilyId("kings_indian")
    case Benoni extends OpeningFamilyId("benoni")
    case Catalan extends OpeningFamilyId("catalan")
    case QueensGambit extends OpeningFamilyId("queens_gambit")
    case London extends OpeningFamilyId("london")
    case English extends OpeningFamilyId("english")
    case Austrian extends OpeningFamilyId("austrian")

    def displayName: String =
      OpeningFamilyCatalog.default.family(wireKey).map(_.displayName).getOrElse(wireKey)

    def structureLabel: String =
      OpeningFamilyCatalog.default.family(wireKey).map(_.structureLabel).getOrElse(displayName)

  object OpeningFamilyId:
    def fromWireKey(key: String): Option[OpeningFamilyId] =
      OpeningFamilyId.values.find(_.wireKey == Option(key).getOrElse("").trim.toLowerCase)

  final case class OpeningFamilyClaim private (wireKey: String)

  object OpeningFamilyClaim:
    def apply(wireKey: String): OpeningFamilyClaim =
      new OpeningFamilyClaim(normalizeFamilyKey(wireKey))

    def apply(family: OpeningFamilyId): OpeningFamilyClaim =
      apply(family.wireKey)

  private val OpeningStageMaxPly = 24

  def decideOpeningFamilyClaim(
      claim: OpeningFamilyClaim,
      proof: OpeningFamilyMatchProof
  ): Option[ClaimAuthorityDecision] =
    val openingLower = normalizeOpening(proof.opening)
    if !isOpeningStage(proof.phase, proof.ply) then None
    else if openingProofMatchesFamily(openingLower, proof.fen, claim.wireKey)
    then Some(ClaimAuthorityDecision(ClaimAuthorityTier.SupportedLocal))
    else
      Some(
        ClaimAuthorityDecision(
          ClaimAuthorityTier.Suppressed,
          List(
            "opening_family_label_mismatch",
            "opening_family_structure_mismatch",
            s"opening_family_mismatch:${claim.wireKey}"
          )
        )
      )

  private def normalizeFamilyKey(raw: String): String =
    Option(raw).getOrElse("").trim.toLowerCase

  private def normalizeOpening(opening: Option[String]): String =
    opening.getOrElse("").trim.toLowerCase

  private def isOpeningStage(phase: String, ply: Int): Boolean =
    phase.equalsIgnoreCase("opening") ||
      (phase.equalsIgnoreCase("middlegame") && ply > 0 && ply <= OpeningStageMaxPly)

  private def openingMatchesFamily(openingLower: String, familyKey: String): Boolean =
    OpeningFamilyCatalog.default.openingMatchesFamily(openingLower, familyKey)

  private def openingProofMatchesFamily(
      openingLower: String,
      fen: Option[String],
      familyKey: String
  ): Boolean =
    openingMatchesFamily(openingLower, familyKey) &&
      bookFenMatchesFamily(fen, familyKey)

  private def bookFenMatchesFamily(fen: Option[String], familyKey: String): Boolean =
    fen.flatMap(OpeningNameLookup.default.lookup)
      .flatMap(_.name)
      .exists(name => openingMatchesFamily(normalizeOpening(Some(name)), familyKey))

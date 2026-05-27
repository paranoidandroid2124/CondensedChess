package lila.commentary.analysis.claim

import lila.commentary.analysis.OpeningNameLookup

private[commentary] object OpeningFamilyClaimResolver:

  final case class OpeningFamilyMatchProof(
      opening: Option[String],
      phase: String,
      ply: Int,
      fen: Option[String]
  )

  enum OpeningFamilyId(val wireKey: String, val displayName: String, val structureLabel: String):
    case OpenGames extends OpeningFamilyId("open_games", "Open Games", "Open Games")
    case Sicilian extends OpeningFamilyId("sicilian", "Sicilian", "Sicilian")
    case French extends OpeningFamilyId("french", "French", "French")
    case CaroKann extends OpeningFamilyId("caro_kann", "Caro-Kann", "Caro-Kann")
    case Scandinavian extends OpeningFamilyId("scandinavian", "Scandinavian", "Scandinavian")
    case NimzoIndian extends OpeningFamilyId("nimzo_indian", "Nimzo-Indian", "Nimzo-Indian")
    case KingsIndian extends OpeningFamilyId("kings_indian", "King's Indian", "King's Indian")
    case Benoni extends OpeningFamilyId("benoni", "Benoni", "Benoni")
    case Catalan extends OpeningFamilyId("catalan", "Catalan", "Catalan")
    case QueensGambit extends OpeningFamilyId("queens_gambit", "Queen's Gambit", "Queen's Gambit")
    case London extends OpeningFamilyId("london", "London", "London")
    case English extends OpeningFamilyId("english", "English", "English")
    case Austrian extends OpeningFamilyId("austrian", "Austrian Attack", "Austrian/Pirc/Modern")

  object OpeningFamilyId:
    def fromWireKey(key: String): Option[OpeningFamilyId] =
      OpeningFamilyId.values.find(_.wireKey == Option(key).getOrElse("").trim.toLowerCase)

  final case class OpeningFamilyClaim(family: OpeningFamilyId)

  private case class OpeningFamily(
      id: OpeningFamilyId,
      aliases: List[String]
  )

  private val OpeningStageMaxPly = 24

  private val openingFamilies = List(
    OpeningFamily(
      id = OpeningFamilyId.OpenGames,
      aliases = List(
        "open game",
        "open games",
        "king's pawn",
        "kings pawn",
        "italian game",
        "italian",
        "ruy lopez",
        "spanish",
        "scotch game",
        "scotch",
        "four knights",
        "four knights game",
        "petrov's defense",
        "petrov defense",
        "petrov",
        "philidor defense",
        "philidor",
        "vienna game",
        "vienna",
        "bishop's opening",
        "bishops opening",
        "ponziani opening",
        "ponziani"
      )
    ),
    OpeningFamily(
      id = OpeningFamilyId.Sicilian,
      aliases = List(
        "sicilian",
        "sicilian defense",
        "najdorf",
        "najdorf variation",
        "dragon",
        "dragon variation",
        "scheveningen",
        "sveshnikov",
        "taimanov",
        "kan",
        "rossolimo",
        "alapin",
        "smith-morra",
        "closed sicilian",
        "accelerated dragon",
        "pelikan"
      )
    ),
    OpeningFamily(
      id = OpeningFamilyId.French,
      aliases = List(
        "french",
        "french defense",
        "winawer",
        "winawer variation",
        "tarrasch",
        "tarrasch variation",
        "rubinstein",
        "rubinstein variation",
        "maccutcheon",
        "steinitz"
      )
    ),
    OpeningFamily(
      id = OpeningFamilyId.CaroKann,
      aliases = List("caro-kann", "caro kann", "caro-kann defense", "caro kann defense")
    ),
    OpeningFamily(
      id = OpeningFamilyId.Scandinavian,
      aliases = List("scandinavian", "scandinavian defense", "center counter")
    ),
    OpeningFamily(
      id = OpeningFamilyId.NimzoIndian,
      aliases = List("nimzo-indian", "nimzo indian", "nimzo-indian defense", "nimzo indian defense")
    ),
    OpeningFamily(
      id = OpeningFamilyId.KingsIndian,
      aliases = List("king's indian", "kings indian", "king's indian defense", "kings indian defense", "k.i.d", "kid")
    ),
    OpeningFamily(
      id = OpeningFamilyId.Benoni,
      aliases = List("benoni", "benoni defense", "modern benoni", "modern benoni defense")
    ),
    OpeningFamily(
      id = OpeningFamilyId.Catalan,
      aliases = List("catalan", "catalan opening")
    ),
    OpeningFamily(
      id = OpeningFamilyId.QueensGambit,
      aliases = List("queen's gambit", "queens gambit", "queen's gambit declined", "queens gambit declined", "queen's gambit accepted", "queens gambit accepted", "qgd", "qga")
    ),
    OpeningFamily(
      id = OpeningFamilyId.London,
      aliases = List("london", "london system")
    ),
    OpeningFamily(
      id = OpeningFamilyId.English,
      aliases = List("english", "english opening")
    ),
    OpeningFamily(
      id = OpeningFamilyId.Austrian,
      aliases = List("austrian attack", "pirc", "pirc defense", "modern defense")
    )
  )

  private val familiesById = openingFamilies.map(f => f.id -> f).toMap

  def decideOpeningFamilyClaim(
      claim: OpeningFamilyClaim,
      proof: OpeningFamilyMatchProof
  ): Option[ClaimAuthorityDecision] =
    val openingLower = normalizeOpening(proof.opening)
    if !isOpeningStage(proof.phase, proof.ply) then None
    else if openingProofMatchesFamily(openingLower, proof.fen, claim.family)
    then Some(ClaimAuthorityDecision(ClaimAuthorityTier.SupportedLocal))
    else
      Some(
        ClaimAuthorityDecision(
          ClaimAuthorityTier.Suppressed,
          List(
            "opening_family_label_mismatch",
            "opening_family_structure_mismatch",
            s"opening_family_mismatch:${claim.family.wireKey}"
          )
        )
      )

  private def normalizeOpening(opening: Option[String]): String =
    opening.getOrElse("").trim.toLowerCase

  private def isOpeningStage(phase: String, ply: Int): Boolean =
    phase.equalsIgnoreCase("opening") ||
      (phase.equalsIgnoreCase("middlegame") && ply > 0 && ply <= OpeningStageMaxPly)

  private def phraseMatchesAlias(normalizedPhrase: String, alias: String): Boolean =
    val phraseWords = normalizedWords(normalizedPhrase)
    val aliasWords = normalizedWords(alias)
    aliasWords.nonEmpty && phraseWords == aliasWords

  private def openingMatchesFamily(openingLower: String, familyKey: OpeningFamilyId): Boolean =
    familiesById
      .get(familyKey)
      .exists(_.aliases.exists(alias => phraseMatchesAlias(openingLower, alias)))

  private def openingProofMatchesFamily(
      openingLower: String,
      fen: Option[String],
      familyKey: OpeningFamilyId
  ): Boolean =
    openingMatchesFamily(openingLower, familyKey) &&
      bookFenMatchesFamily(fen, familyKey)

  private def normalizedWords(raw: String): List[String] =
    Option(raw).getOrElse("").toLowerCase.replaceAll("""[^a-z0-9]+""", " ").trim
      .split("\\s+")
      .toList
      .filter(_.nonEmpty)

  private def bookFenMatchesFamily(fen: Option[String], familyKey: OpeningFamilyId): Boolean =
    fen.flatMap(OpeningNameLookup.default.lookup)
      .flatMap(_.name)
      .exists(name => openingMatchesFamily(normalizeOpening(Some(name)), familyKey))

package lila.commentary.analysis.claim

import _root_.chess.{ Bishop, Knight, Pawn, Queen, Rook, Square }
import _root_.chess.format.Fen
import _root_.chess.variant.Standard

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
      aliases: List[String],
      markers: List[String]
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
        "italian",
        "ruy lopez",
        "spanish",
        "scotch",
        "four knights",
        "petrov",
        "philidor",
        "vienna",
        "bishop's opening",
        "bishops opening",
        "ponziani"
      ),
      markers = List(
        "open games (1.e4 e5)",
        "open games",
        "central e4-e5 structure",
        "e4-e5 structure"
      )
    ),
    OpeningFamily(
      id = OpeningFamilyId.Sicilian,
      aliases = List(
        "sicilian",
        "najdorf",
        "dragon",
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
      ),
      markers = List("sicilian")
    ),
    OpeningFamily(
      id = OpeningFamilyId.French,
      aliases = List(
        "french",
        "winawer",
        "tarrasch",
        "rubinstein",
        "maccutcheon",
        "steinitz"
      ),
      markers = List("french")
    ),
    OpeningFamily(
      id = OpeningFamilyId.CaroKann,
      aliases = List("caro-kann", "caro kann"),
      markers = List("caro-kann", "caro kann")
    ),
    OpeningFamily(
      id = OpeningFamilyId.Scandinavian,
      aliases = List("scandinavian", "center counter"),
      markers = List("scandinavian")
    ),
    OpeningFamily(
      id = OpeningFamilyId.NimzoIndian,
      aliases = List("nimzo", "nimzo-indian", "nimzo indian"),
      markers = List("nimzo")
    ),
    OpeningFamily(
      id = OpeningFamilyId.KingsIndian,
      aliases = List("king's indian", "kings indian", "k.i.d", "kid"),
      markers = List("king's indian", "kings indian")
    ),
    OpeningFamily(
      id = OpeningFamilyId.Benoni,
      aliases = List("benoni", "modern benoni"),
      markers = List("benoni")
    ),
    OpeningFamily(
      id = OpeningFamilyId.Catalan,
      aliases = List("catalan"),
      markers = List("catalan")
    ),
    OpeningFamily(
      id = OpeningFamilyId.QueensGambit,
      aliases = List("queen's gambit", "queens gambit", "qgd", "qga"),
      markers = List("queen's gambit", "queens gambit")
    ),
    OpeningFamily(
      id = OpeningFamilyId.London,
      aliases = List("london"),
      markers = List("london")
    ),
    OpeningFamily(
      id = OpeningFamilyId.English,
      aliases = List("english"),
      markers = List("english")
    ),
    OpeningFamily(
      id = OpeningFamilyId.Austrian,
      aliases = List("austrian attack", "pirc", "modern defense"),
      markers = List("austrian attack", "pirc", "modern defense")
    )
  )

  private val familiesById = openingFamilies.map(f => f.id -> f).toMap
  private val requiresStructureRegex =
    """(?i)\brequires?\s+an?\s+([^,.;:!?]{2,60}?)\s+structure\b""".r
  private val typicalInRegex =
    """(?i)\btypical in\s+([^,.;:!?]{2,60})""".r

  def decideOpeningFamilyClaim(
      claim: OpeningFamilyClaim,
      proof: OpeningFamilyMatchProof
  ): Option[ClaimAuthorityDecision] =
    val openingLower = normalizeOpening(proof.opening)
    if !isOpeningStage(proof.phase, proof.ply) then None
    else if openingMatchesFamily(openingLower, claim.family) ||
        structureMatchesFamily(proof.fen, claim.family)
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

  def decideOpeningFamilyClaim(
      sentence: String,
      proof: OpeningFamilyMatchProof
  ): Option[ClaimAuthorityDecision] =
    val openingLower = normalizeOpening(proof.opening)
    if !isOpeningStage(proof.phase, proof.ply) then None
    else
      val mentionedFamilies = detectMentionedFamilyIds(sentence)
      if mentionedFamilies.isEmpty then None
      else
        val unsupportedFamilies =
          mentionedFamilies.filterNot(familyKey =>
            openingMatchesFamily(openingLower, familyKey) ||
              structureMatchesFamily(proof.fen, familyKey)
          )
        if unsupportedFamilies.nonEmpty then
          val familyCodes =
            unsupportedFamilies.toList.sortBy(_.wireKey).map(familyKey => s"opening_family_mismatch:${familyKey.wireKey}")
          Some(
            ClaimAuthorityDecision(
              ClaimAuthorityTier.Suppressed,
              List("opening_family_label_mismatch", "opening_family_structure_mismatch") ++ familyCodes
            )
          )
        else None

  def suppressesUnsupportedFamilyClaim(
      sentence: String,
      proof: OpeningFamilyMatchProof
  ): Boolean =
    decideOpeningFamilyClaim(sentence, proof).exists(_.tier == ClaimAuthorityTier.Suppressed)

  private def normalizeOpening(opening: Option[String]): String =
    opening.getOrElse("").trim.toLowerCase

  private def isOpeningStage(phase: String, ply: Int): Boolean =
    phase.equalsIgnoreCase("opening") ||
      (phase.equalsIgnoreCase("middlegame") && ply > 0 && ply <= OpeningStageMaxPly)

  private def familyKeyFromPhrase(phrase: String): Option[OpeningFamilyId] =
    val normalized = phrase.trim.toLowerCase
    openingFamilies
      .find { family =>
        family.aliases.exists(alias => phraseMatchesAlias(normalized, alias))
      }
      .map(_.id)

  private def phraseMatchesAlias(normalizedPhrase: String, alias: String): Boolean =
    val phraseWords = normalizedWords(normalizedPhrase)
    val aliasWords = normalizedWords(alias)
    aliasWords.nonEmpty &&
      (
        phraseWords == aliasWords ||
          containsWordSlice(phraseWords, aliasWords)
      )

  private def detectMentionedFamilyIds(sentence: String): Set[OpeningFamilyId] =
    val lower = sentence.toLowerCase
    val direct = openingFamilies.collect {
      case family if family.markers.exists(marker => phraseMatchesAlias(lower, marker)) => family.id
    }
    val fromPatterns =
      (requiresStructureRegex.findAllMatchIn(lower).map(_.group(1)).toList ++
        typicalInRegex.findAllMatchIn(lower).map(_.group(1)).toList)
        .flatMap(familyKeyFromPhrase)
    (direct ++ fromPatterns).toSet

  private def openingMatchesFamily(openingLower: String, familyKey: OpeningFamilyId): Boolean =
    familiesById
      .get(familyKey)
      .exists(_.aliases.exists(alias => phraseMatchesAlias(openingLower, alias)))

  private def normalizedWords(raw: String): List[String] =
    Option(raw).getOrElse("").toLowerCase.replaceAll("""[^a-z0-9]+""", " ").trim
      .split("\\s+")
      .toList
      .filter(_.nonEmpty)

  private def containsWordSlice(words: List[String], slice: List[String]): Boolean =
    slice.nonEmpty &&
      words.sliding(slice.length).exists(_ == slice)

  private def parseFenBoard(fen: String): Option[_root_.chess.Board] =
    Fen.read(Standard, Fen.Full(fen)).map(_.board)

  private def hasPiece(board: _root_.chess.Board, square: String, piece: Char): Boolean =
    val roleOpt =
      piece.toLower match
        case 'p' => Some(Pawn)
        case 'n' => Some(Knight)
        case 'b' => Some(Bishop)
        case 'r' => Some(Rook)
        case 'q' => Some(Queen)
        case _   => None
    roleOpt.exists { role =>
      Square.all
        .find(_.key == square)
        .flatMap(board.pieceAt)
        .exists(actual => actual.role == role && actual.color.white == piece.isUpper)
    }

  private def structureMatchesFamily(fen: Option[String], familyKey: OpeningFamilyId): Boolean =
    val boardOpt = fen.flatMap(parseFenBoard)
    boardOpt.exists { board =>
      familyKey match
        case OpeningFamilyId.OpenGames =>
          hasPiece(board, "e4", 'P') && hasPiece(board, "e5", 'p')
        case OpeningFamilyId.Sicilian =>
          hasPiece(board, "c5", 'p')
        case OpeningFamilyId.French =>
          hasPiece(board, "e6", 'p') && hasPiece(board, "d5", 'p')
        case OpeningFamilyId.CaroKann =>
          hasPiece(board, "c6", 'p') && hasPiece(board, "d5", 'p')
        case OpeningFamilyId.Scandinavian =>
          hasPiece(board, "e4", 'P') && hasPiece(board, "d5", 'p')
        case OpeningFamilyId.Catalan =>
          hasPiece(board, "d4", 'P') && hasPiece(board, "c4", 'P') && hasPiece(board, "g2", 'B')
        case OpeningFamilyId.QueensGambit =>
          hasPiece(board, "d4", 'P') && hasPiece(board, "c4", 'P')
        case OpeningFamilyId.London =>
          hasPiece(board, "d4", 'P') && hasPiece(board, "e3", 'P') && hasPiece(board, "f4", 'B')
        case OpeningFamilyId.English =>
          hasPiece(board, "c4", 'P')
        case OpeningFamilyId.KingsIndian =>
          hasPiece(board, "f6", 'n') && hasPiece(board, "g7", 'b') && hasPiece(board, "g6", 'p')
        case OpeningFamilyId.NimzoIndian =>
          hasPiece(board, "f6", 'n') && hasPiece(board, "b4", 'b')
        case OpeningFamilyId.Benoni =>
          hasPiece(board, "c5", 'p') && hasPiece(board, "d6", 'p') && hasPiece(board, "d5", 'P')
        case OpeningFamilyId.Austrian =>
          hasPiece(board, "e4", 'P') && hasPiece(board, "f4", 'P')
    }

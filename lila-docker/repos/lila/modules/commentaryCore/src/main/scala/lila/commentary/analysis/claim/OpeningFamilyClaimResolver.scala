package lila.commentary.analysis.claim

private[commentary] object OpeningFamilyClaimResolver:

  final case class OpeningFamilyMatchProof(
      opening: Option[String],
      phase: String,
      ply: Int,
      fen: Option[String]
  )

  private case class OpeningFamily(
      id: String,
      aliases: List[String],
      markers: List[String]
  )

  private val OpeningStageMaxPly = 24

  private val openingFamilies = List(
    OpeningFamily(
      id = "open_games",
      aliases = List(
        "open game",
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
      id = "sicilian",
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
      id = "french",
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
      id = "caro_kann",
      aliases = List("caro-kann", "caro kann"),
      markers = List("caro-kann", "caro kann")
    ),
    OpeningFamily(
      id = "scandinavian",
      aliases = List("scandinavian", "center counter"),
      markers = List("scandinavian")
    ),
    OpeningFamily(
      id = "nimzo_indian",
      aliases = List("nimzo", "nimzo-indian", "nimzo indian"),
      markers = List("nimzo")
    ),
    OpeningFamily(
      id = "kings_indian",
      aliases = List("king's indian", "kings indian", "k.i.d", "kid"),
      markers = List("king's indian", "kings indian")
    ),
    OpeningFamily(
      id = "benoni",
      aliases = List("benoni", "modern benoni"),
      markers = List("benoni")
    ),
    OpeningFamily(
      id = "catalan",
      aliases = List("catalan"),
      markers = List("catalan")
    ),
    OpeningFamily(
      id = "queens_gambit",
      aliases = List("queen's gambit", "queens gambit", "qgd", "qga"),
      markers = List("queen's gambit", "queens gambit")
    ),
    OpeningFamily(
      id = "london",
      aliases = List("london"),
      markers = List("london")
    ),
    OpeningFamily(
      id = "english",
      aliases = List("english"),
      markers = List("english")
    ),
    OpeningFamily(
      id = "austrian",
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
          val familyCodes = unsupportedFamilies.toList.sorted.map(familyKey => s"opening_family_mismatch:$familyKey")
          Some(
            ClaimAuthorityDecision(
              ClaimAuthorityTier.Suppressed,
              List("opening_family_label_mismatch", "opening_family_structure_mismatch") ++ familyCodes
            )
          )
        else
          Some(
            ClaimAuthorityDecision(
              ClaimAuthorityTier.SupportedLocal
            )
          )

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

  private def familyKeyFromPhrase(phrase: String): Option[String] =
    val normalized = phrase.trim.toLowerCase
    openingFamilies
      .find { family =>
        family.aliases.exists(alias => phraseMatchesAlias(normalized, alias))
      }
      .map(_.id)

  private def phraseMatchesAlias(normalizedPhrase: String, alias: String): Boolean =
    val normalizedAlias = alias.trim.toLowerCase
    normalizedPhrase == normalizedAlias ||
      (normalizedAlias.length >= 4 && normalizedPhrase.contains(normalizedAlias)) ||
      (normalizedPhrase.length >= 4 && normalizedAlias.contains(normalizedPhrase))

  private def detectMentionedFamilyIds(sentence: String): Set[String] =
    val lower = sentence.toLowerCase
    val direct = openingFamilies.collect {
      case family if family.markers.exists(lower.contains) => family.id
    }
    val fromPatterns =
      (requiresStructureRegex.findAllMatchIn(lower).map(_.group(1)).toList ++
        typicalInRegex.findAllMatchIn(lower).map(_.group(1)).toList)
        .flatMap(familyKeyFromPhrase)
    (direct ++ fromPatterns).toSet

  private def openingMatchesFamily(openingLower: String, familyKey: String): Boolean =
    familiesById
      .get(familyKey)
      .exists(_.aliases.exists(alias => phraseMatchesAlias(openingLower, alias)))

  private def parseFenPieces(fen: String): Option[Map[String, Char]] =
    val boardPart = Option(fen).map(_.trim).filter(_.nonEmpty).flatMap(_.split(" ").headOption).getOrElse("")
    val ranks = boardPart.split("/")
    if ranks.length != 8 then None
    else
      val pieces = scala.collection.mutable.Map.empty[String, Char]
      var valid = true
      ranks.zipWithIndex.foreach { case (rankStr, rankIdx) =>
        if valid then
          var file = 0
          rankStr.foreach { ch =>
            if ch.isDigit then file += ch.asDigit
            else
              if file >= 0 && file < 8 then
                val sq = s"${('a' + file).toChar}${8 - rankIdx}"
                pieces.update(sq, ch)
              else valid = false
              file += 1
          }
          if file != 8 then valid = false
      }
      if valid then Some(pieces.toMap) else None

  private def hasPiece(board: Map[String, Char], square: String, piece: Char): Boolean =
    board.get(square).contains(piece)

  private def structureMatchesFamily(fen: Option[String], familyKey: String): Boolean =
    val boardOpt = fen.flatMap(parseFenPieces)
    boardOpt.exists { board =>
      familyKey match
        case "open_games" =>
          hasPiece(board, "e4", 'P') && hasPiece(board, "e5", 'p')
        case "sicilian" =>
          hasPiece(board, "c5", 'p')
        case "french" =>
          hasPiece(board, "e6", 'p') && hasPiece(board, "d5", 'p')
        case "caro_kann" =>
          hasPiece(board, "c6", 'p') && hasPiece(board, "d5", 'p')
        case "scandinavian" =>
          hasPiece(board, "e4", 'P') && hasPiece(board, "d5", 'p')
        case "catalan" =>
          hasPiece(board, "d4", 'P') && hasPiece(board, "c4", 'P') && hasPiece(board, "g2", 'B')
        case "queens_gambit" =>
          hasPiece(board, "d4", 'P') && hasPiece(board, "c4", 'P')
        case "london" =>
          hasPiece(board, "d4", 'P') && hasPiece(board, "e3", 'P') && hasPiece(board, "f4", 'B')
        case "english" =>
          hasPiece(board, "c4", 'P')
        case "kings_indian" =>
          hasPiece(board, "f6", 'n') && hasPiece(board, "g7", 'b') && hasPiece(board, "g6", 'p')
        case "nimzo_indian" =>
          hasPiece(board, "f6", 'n') && hasPiece(board, "b4", 'b')
        case "benoni" =>
          hasPiece(board, "c5", 'p') && hasPiece(board, "d6", 'p') && hasPiece(board, "d5", 'P')
        case "austrian" =>
          hasPiece(board, "e4", 'P') && hasPiece(board, "f4", 'P')
        case _ => true
    }

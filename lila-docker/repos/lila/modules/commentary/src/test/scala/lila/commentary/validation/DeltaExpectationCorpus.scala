package lila.commentary.validation

import chess.{ Square }
import chess.format.{ Fen, Uci }

import lila.commentary.witness.WitnessAnchor

import play.api.libs.functional.syntax.*
import play.api.libs.json.*

private[validation] object DeltaExpectationCorpus:

  val resourcePath = "/commentary-corpus/delta-expectations.jsonl"

  val requiredFamilies: Vector[String] = Vector(
    "TradeCompressionCorridor",
    "TradeInvariant"
  )

  val requiredCaseTypes: Set[String] =
    Set("exact", "near_miss", "nasty_negative", "move_local_false_witness")
  val requiredExpectations: Set[String] = Set("present", "absent")
  val requiredOwners: Set[String] = Set("white", "black")
  val requiredScopesByFamily: Map[String, Set[String]] = Map(
    "TradeCompressionCorridor" -> Set("move_local"),
    "TradeInvariant" -> Set("move_local")
  )
  val requiredDeltaTagsByFamily: Map[String, Set[String]] = Map(
    "TradeCompressionCorridor" -> Set("transition_compression"),
    "TradeInvariant" -> Set("bounded_favorable_simplification")
  )
  val requiredPressureTargetsByFamily: Map[String, Set[String]] = Map(
    "TradeCompressionCorridor" -> Set(
      "capture_creates_compressed_exchange_corridor",
      "liquidation_without_shared_corridor",
      "shared_corridor_without_compressed_window",
      "shared_corridor_without_material_compression",
      "shared_corridor_without_unique_pair",
      "corridor_picture_without_trade_reduction",
      "corridor_already_true_before_trade"
    ),
    "TradeInvariant" -> Set(
      "bounded_capture_preserves_endgame_race",
      "trade_without_persistent_carrier",
      "winning_look_without_invariant_carrier",
      "trade_switches_persistent_carrier",
      "pawn_capture_preserves_task_without_bounded_reduction",
      "quiet_move_preserves_task_without_trade",
      "trade_creates_after_only_carrier"
    )
  )
  val requiredHelpersByFamily: Map[String, Set[String]] = Map(
    "TradeCompressionCorridor" -> Set(
      "reciprocal_exchange_corridor",
      "compressed_trade_window",
      "trade_compression_transition"
    ),
    "TradeInvariant" -> Set(
      "bounded_material_reduction",
      "persistent_object_carrier",
      "trade_invariant_transition"
    )
  )
  val requiredPersistentCarrierFamiliesByFamily: Map[String, String] = Map(
    "TradeInvariant" -> "EndgameRaceScaffold"
  )
  val requiredForbiddenRivalFamiliesByFamily: Map[String, Set[String]] = Map(
    "TradeCompressionCorridor" -> Set("TradeInvariant"),
    "TradeInvariant" -> Set("TradeCompressionCorridor")
  )

  final case class Row(
      id: String,
      caseType: String,
      fenBefore: String,
      playedMove: String,
      fenAfter: String,
      expectation: String,
      family: String,
      owner: String,
      scope: String,
      deltaTag: String,
      anchor: String,
      pressureTarget: String,
      helpers: List[String],
      canonicalCorridorPairAfter: Option[List[String]],
      persistentCarrierFamily: Option[String],
      persistentCarrierAnchor: Option[String],
      forbiddenRivalFamilies: List[String],
      notes: Option[String]
  ):
    def normalizedFenBefore: Fen.Full = Fen.Full.clean(fenBefore)
    def normalizedFenAfter: Fen.Full = Fen.Full.clean(fenAfter)

    def validatedCaseType: String =
      require(
        requiredCaseTypes.contains(caseType),
        s"Row $id uses unsupported caseType $caseType"
      )
      caseType

    def validatedExpectation: String =
      require(
        requiredExpectations.contains(expectation),
        s"Row $id uses unsupported expectation $expectation"
      )
      expectation

    def validatedFamily: String =
      require(
        requiredFamilies.contains(family),
        s"Row $id uses unsupported family $family"
      )
      family

    def validatedOwner: String =
      require(
        requiredOwners.contains(owner),
        s"Row $id uses unsupported owner $owner"
      )
      owner

    def validatedScope: String =
      val allowedScopes =
        requiredScopesByFamily.getOrElse(
          validatedFamily,
          throw IllegalArgumentException(s"Row $id has no frozen scope contract for $family")
        )
      require(
        allowedScopes.contains(scope),
        s"Row $id scope mismatch for $family: expected one of ${allowedScopes.toVector.sorted.mkString(", ")} but found $scope"
      )
      scope

    def validatedDeltaTag: String =
      val allowedDeltaTags =
        requiredDeltaTagsByFamily.getOrElse(
          validatedFamily,
          throw IllegalArgumentException(s"Row $id has no frozen delta-tag contract for $family")
        )
      require(
        allowedDeltaTags.contains(deltaTag),
        s"Row $id deltaTag mismatch for $family: expected one of ${allowedDeltaTags.toVector.sorted.mkString(", ")} but found $deltaTag"
      )
      deltaTag

    def expectedAnchor: WitnessAnchor =
      require(anchor == "board", s"Row $id uses unsupported anchor $anchor")
      WitnessAnchor.BoardAnchor

    def validatedPressureTarget: String =
      require(
        pressureTarget.matches("^[a-z][a-z0-9_]*$"),
        s"Row $id uses invalid pressureTarget $pressureTarget"
      )
      val allowedTargets =
        requiredPressureTargetsByFamily.getOrElse(
          validatedFamily,
          throw IllegalArgumentException(
            s"Row $id has no frozen pressure-target contract for $family"
          )
        )
      require(
        allowedTargets.contains(pressureTarget),
        s"Row $id pressureTarget mismatch for $family: expected one of ${allowedTargets.toVector.sorted.mkString(", ")} but found $pressureTarget"
      )
      pressureTarget

    def validatedHelpers: Vector[String] =
      val normalized = helpers.map(_.trim).filter(_.nonEmpty).toVector
      require(normalized.nonEmpty, s"Row $id must declare at least one helper")
      require(
        normalized.distinct == normalized,
        s"Row $id declares duplicate helpers: ${normalized.mkString(", ")}"
      )
      require(
        normalized.forall(_.matches("^[a-z][a-z0-9_]*$")),
        s"Row $id uses invalid helper ids: ${normalized.mkString(", ")}"
      )
      val expectedHelpers =
        requiredHelpersByFamily.getOrElse(
          validatedFamily,
          throw IllegalArgumentException(s"Row $id has no frozen helper contract for $family")
        )
      require(
        normalized.toSet == expectedHelpers,
        s"Row $id helper contract mismatch for $family: expected ${expectedHelpers.toVector.sorted.mkString(", ")} but found ${normalized.sorted.mkString(", ")}"
      )
      normalized

    def validatedCanonicalCorridorPairAfter: Option[(Square, Square)] =
      validatedFamily match
        case "TradeCompressionCorridor" =>
          val validatedTarget = validatedPressureTarget
          validatedCaseType match
            case "near_miss" =>
              require(
                canonicalCorridorPairAfter.isEmpty,
                s"Row $id must not declare canonicalCorridorPairAfter for a near_miss corridor row"
              )
              None
            case _ =>
              if validatedTarget == "shared_corridor_without_unique_pair" then
                require(
                  canonicalCorridorPairAfter.isEmpty,
                  s"Row $id must not declare canonicalCorridorPairAfter when the after-board pair is ambiguous"
                )
                None
              else
                val declaredPair =
                  canonicalCorridorPairAfter.getOrElse(
                    throw IllegalArgumentException(
                      s"Row $id must declare canonicalCorridorPairAfter for $family $caseType"
                    )
                  )
                val normalized = declaredPair.map(_.trim).filter(_.nonEmpty).toVector
                require(normalized.size == 2, s"Row $id must declare exactly two canonical corridor squares")
                require(
                  normalized.distinct.size == 2,
                  s"Row $id canonicalCorridorPairAfter must contain two distinct squares"
                )
                val firstSquare: Square =
                  Square
                    .fromKey(normalized.head)
                    .getOrElse(
                      throw IllegalArgumentException(
                        s"Row $id uses invalid corridor square ${normalized.head}"
                      )
                    )
                val secondSquare: Square =
                  Square
                    .fromKey(normalized(1))
                    .getOrElse(
                      throw IllegalArgumentException(
                        s"Row $id uses invalid corridor square ${normalized(1)}"
                      )
                    )
                require(
                  firstSquare.file == secondSquare.file || firstSquare.onSameDiagonal(secondSquare),
                  s"Row $id canonical corridor squares must share a file or diagonal"
                )
                Some((firstSquare, secondSquare))
        case _ =>
          require(
            canonicalCorridorPairAfter.isEmpty,
            s"Row $id must not declare canonicalCorridorPairAfter outside TradeCompressionCorridor"
          )
          None

    def validatedPersistentCarrierFamily: Option[String] =
      validatedFamily match
        case "TradeInvariant" =>
          val declaredFamily =
            persistentCarrierFamily.getOrElse(
              throw IllegalArgumentException(
                s"Row $id must declare persistentCarrierFamily for TradeInvariant"
              )
            )
          val requiredFamily =
            requiredPersistentCarrierFamiliesByFamily.getOrElse(
              validatedFamily,
              throw IllegalArgumentException(s"Row $id has no frozen persistent carrier contract")
            )
          require(
            declaredFamily == requiredFamily,
            s"Row $id persistentCarrierFamily mismatch: expected $requiredFamily but found $declaredFamily"
          )
          Some(declaredFamily)
        case _ =>
          require(
            persistentCarrierFamily.isEmpty,
            s"Row $id must not declare persistentCarrierFamily outside TradeInvariant"
          )
          None

    def validatedPersistentCarrierAnchor: Option[WitnessAnchor] =
      validatedFamily match
        case "TradeInvariant" =>
          val declaredAnchor =
            persistentCarrierAnchor.getOrElse(
              throw IllegalArgumentException(
                s"Row $id must declare persistentCarrierAnchor for TradeInvariant"
              )
            )
          require(
            declaredAnchor == "board",
            s"Row $id persistentCarrierAnchor mismatch: expected board but found $declaredAnchor"
          )
          Some(WitnessAnchor.BoardAnchor)
        case _ =>
          require(
            persistentCarrierAnchor.isEmpty,
            s"Row $id must not declare persistentCarrierAnchor outside TradeInvariant"
          )
          None

    def validatedForbiddenRivalFamilies: Vector[String] =
      val normalized = forbiddenRivalFamilies.map(_.trim).filter(_.nonEmpty).toVector
      require(normalized.nonEmpty, s"Row $id must declare at least one forbidden rival family")
      require(
        normalized.distinct == normalized,
        s"Row $id declares duplicate forbidden rival families: ${normalized.mkString(", ")}"
      )
      require(
        normalized.forall(requiredFamilies.contains),
        s"Row $id uses unsupported forbidden rival families: ${normalized.mkString(", ")}"
      )
      require(
        !normalized.contains(validatedFamily),
        s"Row $id must not list its own family as a forbidden rival"
      )
      val expectedRivals =
        requiredForbiddenRivalFamiliesByFamily.getOrElse(
          validatedFamily,
          throw IllegalArgumentException(s"Row $id has no frozen rival-family contract for $family")
        )
      require(
        normalized.toSet == expectedRivals,
        s"Row $id forbidden rival family mismatch for $family: expected ${expectedRivals.toVector.sorted.mkString(", ")} but found ${normalized.sorted.mkString(", ")}"
      )
      normalized

    def validatedMove: Uci.Move =
      Uci(playedMove) match
        case Some(move: Uci.Move) => move
        case Some(_) => throw IllegalArgumentException(s"Row $id uses unsupported non-move UCI $playedMove")
        case None => throw IllegalArgumentException(s"Row $id uses invalid UCI $playedMove")

  private given Reads[Row] =
    (
      (__ \ "id").read[String] and
        (__ \ "caseType").read[String] and
        (__ \ "fenBefore").read[String] and
        (__ \ "playedMove").read[String] and
        (__ \ "fenAfter").read[String] and
        (__ \ "expectation").read[String] and
        (__ \ "family").read[String] and
        (__ \ "owner").read[String] and
        (__ \ "scope").read[String] and
        (__ \ "deltaTag").read[String] and
        (__ \ "anchor").read[String] and
        (__ \ "pressureTarget").read[String] and
        (__ \ "helpers").read[List[String]] and
        (__ \ "canonicalCorridorPairAfter").readNullable[List[String]] and
        (__ \ "persistentCarrierFamily").readNullable[String] and
        (__ \ "persistentCarrierAnchor").readNullable[String] and
        (__ \ "forbiddenRivalFamilies").read[List[String]] and
        (__ \ "notes").readNullable[String]
    )(Row.apply)

  def loadAll(): Vector[Row] =
    val source =
      scala.io.Source.fromInputStream(
        Option(getClass.getResourceAsStream(resourcePath))
          .getOrElse(throw IllegalStateException(s"Missing test resource $resourcePath"))
      )
    try
      source
        .getLines()
        .filter(_.nonEmpty)
        .zipWithIndex
        .map: (line, index) =>
          Json.parse(line).validate[Row].fold(
            errors => throw IllegalArgumentException(s"Invalid delta row ${index + 1}: $errors"),
            identity
          )
        .toVector
    finally source.close()

package lila.commentary.validation

import chess.{ Color, Square }
import chess.format.Fen

import lila.commentary.witness.WitnessSector
import lila.commentary.witness.WitnessAnchor

import play.api.libs.functional.syntax.*
import play.api.libs.json.*

private[validation] object ObjectExpectationCorpus:

  val resourcePath = "/commentary-corpus/object-expectations.jsonl"

  val requiredFamilies: Vector[String] = Vector(
    "OpeningDevelopmentRegime",
    "DistributedContactRegime",
    "EndgameRaceScaffold",
    "AttackScaffold",
    "FortressHoldingShell",
    "KingSafetyShell",
    "CentralContactFront"
  )

  val requiredCaseTypes: Set[String] = Set("exact", "near_miss", "nasty_negative")
  val requiredExpectations: Set[String] = Set("present", "absent")
  val requiredOwners: Set[String] = Set("white", "black", "neutral")
  val allowedOwnersByFamily: Map[String, Set[String]] = Map(
    "OpeningDevelopmentRegime" -> Set("neutral"),
    "DistributedContactRegime" -> Set("neutral"),
    "EndgameRaceScaffold" -> Set("neutral"),
    "AttackScaffold" -> Set("white", "black"),
    "FortressHoldingShell" -> Set("white", "black"),
    "KingSafetyShell" -> Set("white", "black"),
    "CentralContactFront" -> Set("neutral")
  )
  val requiredAnchorKindByFamily: Map[String, String] = Map(
    "OpeningDevelopmentRegime" -> "board",
    "DistributedContactRegime" -> "board",
    "EndgameRaceScaffold" -> "board",
    "AttackScaffold" -> "square",
    "FortressHoldingShell" -> "square",
    "KingSafetyShell" -> "square",
    "CentralContactFront" -> "sector"
  )
  val requiredPressureTargetsByFamily: Map[String, Set[String]] = Map(
    "OpeningDevelopmentRegime" -> Set(
      "positive_window",
      "no_longer_development",
      "rival_object_preempts_opening"
    ),
    "DistributedContactRegime" -> Set(
      "multi_sector_contact",
      "central_only_contact",
      "multi_sector_contested_without_occupied_contact"
    ),
    "EndgameRaceScaffold" -> Set(
      "dual_unblocked_run",
      "one_sided_run",
      "direct_blockade"
    ),
    "AttackScaffold" -> Set(
      "carrier_plus_support_same_king",
      "off_theater_pressure",
      "carrier_only_pressure"
    ),
    "FortressHoldingShell" -> Set(
      "sealed_shell_no_entry",
      "insufficient_shell_occupancy",
      "direct_file_entry"
    ),
    "KingSafetyShell" -> Set(
      "adjacent_double_hole",
      "single_hole_only",
      "non_adjacent_holes_only"
    ),
    "CentralContactFront" -> Set(
      "connected_central_front",
      "single_square_touch",
      "connected_center_without_occupied_contact"
    )
  )
  val requiredHelpersByFamily: Map[String, Set[String]] = Map(
    "OpeningDevelopmentRegime" -> Set("opening_development_window"),
    "DistributedContactRegime" -> Set(
      "sector_mask",
      "contact_square",
      "front_connectivity",
      "distributed_contact_spread"
    ),
    "EndgameRaceScaffold" -> Set("dual_run_endgame_trigger"),
    "AttackScaffold" -> Set("king_theater_link", "attack_host_core"),
    "FortressHoldingShell" -> Set("fortress_entry_denial_shell"),
    "KingSafetyShell" -> Set("home_shelter_shell"),
    "CentralContactFront" -> Set(
      "central_sector_mask",
      "contact_square",
      "front_connectivity",
      "central_contact_front_state"
    )
  )
  val minimumCorpusFiles: Vector[String] = Vector(
    "root-expectations.jsonl",
    "witness-expectations.jsonl",
    "object-expectations.jsonl",
    "delta-expectations.jsonl",
    "certification-expectations.jsonl",
    "projection-expectations.jsonl",
    "planner-expectations.jsonl",
    "surface-expectations.jsonl",
    "engine-probe-expectations.jsonl"
  )

  final case class Row(
      id: String,
      caseType: String,
      fen: String,
      expectation: String,
      family: String,
      owner: String,
      anchor: String,
      pressureTarget: String,
      helpers: List[String],
      notes: Option[String]
  ):
    def normalizedFen: Fen.Full = Fen.Full.clean(fen)

    def expectedAnchor: WitnessAnchor =
      val resolved = resolvedAnchor
      val expectedKind =
        requiredAnchorKindByFamily.getOrElse(
          validatedFamily,
          throw IllegalArgumentException(s"Row $id has no frozen anchor contract for $family")
        )
      require(
        resolved.kind.key == expectedKind,
        s"Row $id anchor mismatch for $family: expected $expectedKind but found ${resolved.kind.key} ($anchor)"
      )
      resolved

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
      val allowedOwners =
        allowedOwnersByFamily.getOrElse(
          validatedFamily,
          throw IllegalArgumentException(s"Row $id has no frozen owner contract for $family")
        )
      require(
        allowedOwners.contains(owner),
        s"Row $id owner mismatch for $family: expected one of ${allowedOwners.toVector.sorted.mkString(", ")} but found $owner"
      )
      owner

    def anchorKind: String =
      expectedAnchor.kind.key

    def expectedColor: Option[Color] =
      validatedOwner match
        case "white" => Some(Color.White)
        case "black" => Some(Color.Black)
        case "neutral" => None

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

    private def resolvedAnchor: WitnessAnchor =
      if anchor == "board" then WitnessAnchor.BoardAnchor
      else
        WitnessSector.values
          .find(_.key == anchor)
          .map(sector => WitnessAnchor.SectorAnchor(sector))
          .orElse(Square.fromKey(anchor).map(square => WitnessAnchor.SquareAnchor(square)))
          .getOrElse(throw IllegalArgumentException(s"Row $id uses unsupported anchor $anchor"))

  private given Reads[Row] =
    (
      (__ \ "id").read[String] and
        (__ \ "caseType").read[String] and
        (__ \ "fen").read[String] and
        (__ \ "expectation").read[String] and
        (__ \ "family").read[String] and
        (__ \ "owner").read[String] and
        (__ \ "anchor").read[String] and
        (__ \ "pressureTarget").read[String] and
        (__ \ "helpers").read[List[String]] and
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
          Json.parse(line).validate[Row] match
            case JsSuccess(row, _) => row
            case JsError(errors) =>
              throw IllegalArgumentException(
                s"Failed to parse object expectation row ${index + 1}: ${JsError.toJson(errors)}"
              )
        .toVector
    finally source.close()

  def resourceExists(fileName: String): Boolean =
    Option(getClass.getResourceAsStream(s"/commentary-corpus/$fileName")).exists: stream =>
      stream.close()
      true

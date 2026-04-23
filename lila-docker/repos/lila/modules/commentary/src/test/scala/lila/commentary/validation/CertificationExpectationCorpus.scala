package lila.commentary.validation

import chess.format.Fen

import lila.commentary.witness.WitnessAnchor

import play.api.libs.functional.syntax.*
import play.api.libs.json.*

private[validation] object CertificationExpectationCorpus:

  val resourcePath = "/commentary-corpus/certification-expectations.jsonl"

  val requiredFamilies: Vector[String] = Vector(
    "DevelopmentComparison",
    "InitiativeWindow",
    "MobilityComparison",
    "ComparativeKingFragility",
    "CertifiedKingSafetyEdge",
    "MateNetCertification",
    "MaterialHarvest",
    "WinningEndgame",
    "FortressDrawCertification",
    "PerpetualCheckHolding",
    "PromotionRace",
    "SpaceBindRestrictionCertification"
  )

  val requiredCaseTypes: Set[String] =
    Set("exact", "near_miss", "nasty_negative", "best_defense_breaks_claim")
  val requiredExpectations: Set[String] =
    Set("certified", "support_only", "deferred", "rejected")
  val requiredOwners: Set[String] = Set("white", "black")
  val requiredScopesByFamily: Map[String, Set[String]] = Map(
    "DevelopmentComparison" -> Set("comparative"),
    "InitiativeWindow" -> Set("comparative"),
    "MobilityComparison" -> Set("comparative"),
    "ComparativeKingFragility" -> Set("comparative"),
    "CertifiedKingSafetyEdge" -> Set("comparative"),
    "MateNetCertification" -> Set("current_position"),
    "MaterialHarvest" -> Set("current_position"),
    "WinningEndgame" -> Set("current_position"),
    "FortressDrawCertification" -> Set("current_position"),
    "PerpetualCheckHolding" -> Set("current_position"),
    "PromotionRace" -> Set("current_position"),
    "SpaceBindRestrictionCertification" -> Set("current_position")
  )
  val requiredBurdenTagsByFamily: Map[String, String] = Map(
    "DevelopmentComparison" -> "development_superiority",
    "InitiativeWindow" -> "counterplay_denial_window",
    "MobilityComparison" -> "mobility_superiority",
    "ComparativeKingFragility" -> "king_fragility_asymmetry",
    "CertifiedKingSafetyEdge" -> "king_safety_edge_certification",
    "MateNetCertification" -> "forcing_mate_net",
    "MaterialHarvest" -> "realized_material_conversion",
    "WinningEndgame" -> "conversion_result",
    "FortressDrawCertification" -> "fortress_hold_certification",
    "PerpetualCheckHolding" -> "perpetual_check_hold",
    "PromotionRace" -> "promotion_route_survival",
    "SpaceBindRestrictionCertification" -> "space_bind_persistence"
  )
  val requiredHelpersByFamily: Map[String, Set[String]] = Map(
    "DevelopmentComparison" -> Set("development_balance_count", "development_gap_floor"),
    "InitiativeWindow" -> Set(
      "initiative_window_contract",
      "rival_counterplay_source",
      "move_order_relevance_gate"
    ),
    "MobilityComparison" -> Set(
      "mobility_balance_count",
      "mobility_gap_floor",
      "restriction_support_gate"
    ),
    "ComparativeKingFragility" -> Set(
      "king_theater_fragility_bundle",
      "king_fragility_asymmetry"
    ),
    "CertifiedKingSafetyEdge" -> Set(
      "attack_host_viability",
      "attacker_budget_present",
      "move_order_relevance_gate",
      "best_defense_survival",
      "major_piece_presence"
    ),
    "MateNetCertification" -> Set("mate_net_forcing_window", "best_defense_survival"),
    "MaterialHarvest" -> Set("material_conversion_realization", "best_defense_survival"),
    "WinningEndgame" -> Set("winning_endgame_conversion", "best_defense_survival"),
    "FortressDrawCertification" -> Set("fortress_draw_burden", "best_defense_survival"),
    "PerpetualCheckHolding" -> Set("perpetual_check_loop", "best_defense_survival"),
    "PromotionRace" -> Set("promotion_route_survival", "best_defense_survival"),
    "SpaceBindRestrictionCertification" -> Set(
      "structural_space_host",
      "same_anchor_restriction_route",
      "best_defense_survival"
    )
  )
  val requiredSupportFamiliesByFamily: Map[String, Set[String]] = Map(
    "DevelopmentComparison" -> Set("OpeningDevelopmentRegime"),
    "InitiativeWindow" -> Set("DevelopmentComparison"),
    "CertifiedKingSafetyEdge" -> Set("AttackScaffold", "ComparativeKingFragility"),
    "FortressDrawCertification" -> Set("FortressHoldingShell"),
    "PromotionRace" -> Set("EndgameRaceScaffold")
  )
  val requiredEngineRequirementByFamily: Map[String, String] =
    requiredFamilies.map(_ -> "required").toMap
  val requiredEnginePurposesByFamily: Map[String, Set[String]] = Map(
    "DevelopmentComparison" -> Set("comparative_superiority"),
    "InitiativeWindow" -> Set("counterplay_denial", "best_defense_survival"),
    "MobilityComparison" -> Set("comparative_superiority"),
    "ComparativeKingFragility" -> Set("comparative_superiority"),
    "CertifiedKingSafetyEdge" -> Set("comparative_superiority", "best_defense_survival"),
    "MateNetCertification" -> Set("best_defense_survival", "tactical_release_detection"),
    "MaterialHarvest" -> Set("best_defense_survival", "tactical_release_detection"),
    "WinningEndgame" -> Set("best_defense_survival", "conversion_route_survival"),
    "FortressDrawCertification" -> Set("best_defense_survival"),
    "PerpetualCheckHolding" -> Set("best_defense_survival"),
    "PromotionRace" -> Set("best_defense_survival", "conversion_route_survival"),
    "SpaceBindRestrictionCertification" -> Set("best_defense_survival", "tactical_release_detection")
  )
  val requiredForbiddenShortcutsByFamily: Map[String, Set[String]] = Map(
    "DevelopmentComparison" -> Set(
      "opening_regime_alone",
      "phase_gate",
      "development_wording"
    ),
    "InitiativeWindow" -> Set(
      "development_comparison_alone",
      "attack_scaffold_alone",
      "counterplay_wording"
    ),
    "MobilityComparison" -> Set(
      "restriction_geometry_alone",
      "space_clamp_wording",
      "bad_piece_wording"
    ),
    "ComparativeKingFragility" -> Set(
      "king_safety_shell_alone",
      "hole_count_only",
      "attack_wording"
    ),
    "CertifiedKingSafetyEdge" -> Set(
      "attack_scaffold_alone",
      "comparative_fragility_alone",
      "phase_proxy_only"
    ),
    "MateNetCertification" -> Set(
      "attack_scaffold_alone",
      "king_safety_edge_alone",
      "mate_threat_wording"
    ),
    "MaterialHarvest" -> Set(
      "material_gain_shell",
      "tactical_smell",
      "winning_endgame_wording"
    ),
    "WinningEndgame" -> Set(
      "trade_invariant_alone",
      "fortress_holding_shell_alone",
      "material_edge_only"
    ),
    "FortressDrawCertification" -> Set(
      "fortress_holding_shell_alone",
      "trade_invariant_alone",
      "draw_wording"
    ),
    "PerpetualCheckHolding" -> Set(
      "checking_sequence_wording",
      "attack_scaffold_alone",
      "draw_wording"
    ),
    "PromotionRace" -> Set(
      "endgame_race_scaffold_alone",
      "passer_complex_wording",
      "conversion_funnel_wording"
    ),
    "SpaceBindRestrictionCertification" -> Set(
      "structural_space_claim_alone",
      "mobility_comparison_only",
      "center_release_rival_shortcut",
      "outpost_without_space_host"
    )
  )
  val requiredExpectationCoverageByFamily: Map[String, Set[String]] = Map(
    "DevelopmentComparison" -> Set("certified", "rejected", "support_only"),
    "InitiativeWindow" -> Set("certified", "rejected", "deferred"),
    "MobilityComparison" -> Set("certified", "rejected", "support_only"),
    "ComparativeKingFragility" -> Set("certified", "rejected", "support_only"),
    "CertifiedKingSafetyEdge" -> Set("certified", "rejected", "deferred"),
    "MateNetCertification" -> Set("certified", "rejected", "deferred"),
    "MaterialHarvest" -> Set("certified", "rejected", "support_only"),
    "WinningEndgame" -> Set("certified", "rejected", "deferred"),
    "FortressDrawCertification" -> Set("certified", "rejected", "support_only"),
    "PerpetualCheckHolding" -> Set("certified", "rejected", "deferred"),
    "PromotionRace" -> Set("certified", "rejected", "deferred"),
    "SpaceBindRestrictionCertification" -> Set("certified", "rejected", "support_only")
  )

  final case class Row(
      id: String,
      caseType: String,
      fen: String,
      expectation: String,
      family: String,
      owner: String,
      scope: String,
      anchor: String,
      burdenTag: String,
      helpers: List[String],
      requiredSupportFamilies: List[String],
      engineRequirement: String,
      enginePurposes: List[String],
      forbiddenShortcuts: List[String],
      notes: Option[String]
  ):
    def normalizedFen: Fen.Full = Fen.Full.clean(fen)

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

    def expectedAnchor: WitnessAnchor =
      require(anchor == "board", s"Row $id uses unsupported anchor $anchor")
      WitnessAnchor.BoardAnchor

    def validatedBurdenTag: String =
      val requiredTag =
        requiredBurdenTagsByFamily.getOrElse(
          validatedFamily,
          throw IllegalArgumentException(s"Row $id has no frozen burden tag for $family")
        )
      require(
        burdenTag == requiredTag,
        s"Row $id burdenTag mismatch for $family: expected $requiredTag but found $burdenTag"
      )
      burdenTag

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

    def validatedRequiredSupportFamilies: Vector[String] =
      val normalized = requiredSupportFamilies.map(_.trim).filter(_.nonEmpty).toVector
      require(
        normalized.distinct == normalized,
        s"Row $id declares duplicate required support families: ${normalized.mkString(", ")}"
      )
      require(
        normalized.forall(_.matches("^[A-Z][A-Za-z0-9]*$")),
        s"Row $id uses invalid required support families: ${normalized.mkString(", ")}"
      )
      val expectedSupportFamilies =
        requiredSupportFamiliesByFamily.getOrElse(validatedFamily, Set.empty)
      require(
        normalized.toSet == expectedSupportFamilies,
        s"Row $id support-family contract mismatch for $family: expected ${expectedSupportFamilies.toVector.sorted.mkString(", ")} but found ${normalized.sorted.mkString(", ")}"
      )
      normalized

    def validatedEngineRequirement: String =
      val requiredValue =
        requiredEngineRequirementByFamily.getOrElse(
          validatedFamily,
          throw IllegalArgumentException(
            s"Row $id has no frozen engine-requirement contract for $family"
          )
        )
      require(
        engineRequirement == requiredValue,
        s"Row $id engineRequirement mismatch for $family: expected $requiredValue but found $engineRequirement"
      )
      engineRequirement

    def validatedEnginePurposes: Vector[String] =
      val normalized = enginePurposes.map(_.trim).filter(_.nonEmpty).toVector
      require(normalized.nonEmpty, s"Row $id must declare at least one engine purpose")
      require(
        normalized.distinct == normalized,
        s"Row $id declares duplicate engine purposes: ${normalized.mkString(", ")}"
      )
      require(
        normalized.forall(_.matches("^[a-z][a-z0-9_]*$")),
        s"Row $id uses invalid engine purposes: ${normalized.mkString(", ")}"
      )
      val expectedPurposes =
        requiredEnginePurposesByFamily.getOrElse(
          validatedFamily,
          throw IllegalArgumentException(
            s"Row $id has no frozen engine-purpose contract for $family"
          )
        )
      require(
        normalized.toSet == expectedPurposes,
        s"Row $id engine purposes mismatch for $family: expected ${expectedPurposes.toVector.sorted.mkString(", ")} but found ${normalized.sorted.mkString(", ")}"
      )
      normalized

    def validatedForbiddenShortcuts: Vector[String] =
      val normalized = forbiddenShortcuts.map(_.trim).filter(_.nonEmpty).toVector
      require(normalized.nonEmpty, s"Row $id must declare at least one forbidden shortcut")
      require(
        normalized.distinct == normalized,
        s"Row $id declares duplicate forbidden shortcuts: ${normalized.mkString(", ")}"
      )
      require(
        normalized.forall(_.matches("^[a-z][a-z0-9_]*$")),
        s"Row $id uses invalid forbidden shortcuts: ${normalized.mkString(", ")}"
      )
      val expectedShortcuts =
        requiredForbiddenShortcutsByFamily.getOrElse(
          validatedFamily,
          throw IllegalArgumentException(
            s"Row $id has no frozen forbidden-shortcut contract for $family"
          )
        )
      require(
        normalized.toSet == expectedShortcuts,
        s"Row $id forbidden shortcuts mismatch for $family: expected ${expectedShortcuts.toVector.sorted.mkString(", ")} but found ${normalized.sorted.mkString(", ")}"
      )
      normalized

  private given Reads[Row] =
    (
      (__ \ "id").read[String] and
        (__ \ "caseType").read[String] and
        (__ \ "fen").read[String] and
        (__ \ "expectation").read[String] and
        (__ \ "family").read[String] and
        (__ \ "owner").read[String] and
        (__ \ "scope").read[String] and
        (__ \ "anchor").read[String] and
        (__ \ "burdenTag").read[String] and
        (__ \ "helpers").read[List[String]] and
        (__ \ "requiredSupportFamilies").readNullable[List[String]].map(_.getOrElse(Nil)) and
        (__ \ "engineRequirement").read[String] and
        (__ \ "enginePurposes").read[List[String]] and
        (__ \ "forbiddenShortcuts").read[List[String]] and
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
            errors =>
              throw IllegalArgumentException(
                s"Invalid certification row ${index + 1}: $errors"
              ),
            identity
          )
        .toVector
    finally source.close()

package lila.commentary.tools.claim

import lila.commentary.analysis.*
import lila.commentary.analysis.claim.*
import java.nio.charset.StandardCharsets
import java.nio.file.{ Files, Paths }

private[commentary] object AdmissionUnitCatalog:

  enum NegativeControlKind:
    case TacticalFirst
    case RivalOrGenericRelabel
    case MissingWitness

  final case class SourceCandidateTarget(min: Int, max: Int):
    require(min > 0, "minimum source candidates must be positive")
    require(max >= min, "maximum source candidates must be at least the minimum")

    override def toString: String = s"$min-$max"

  final case class ControlledPositiveRequirement(
      count: Int,
      expectedPacket: String,
      expectedSurface: String
  )

  final case class NegativeControlRequirement(
      kind: NegativeControlKind,
      expectedBlocker: String
  )

  final case class DocumentationTargets(
      corpusLedger: String,
      trustDoc: String,
      surfaceReview: String
  )

  final case class AdmissionUnitSpec(
      planKindId: String,
      proofFamily: String,
      proofSource: String,
      acceptedScope: PlayerFacingPacketScope,
      defaultAuthorityTier: String,
      requiredWitnesses: Set[ProofWitness],
      controlledPositive: ControlledPositiveRequirement,
      negativeControls: List[NegativeControlRequirement],
      sourceCandidateTarget: SourceCandidateTarget,
      maxAuthorityRows: Int,
      documentationTargets: DocumentationTargets
  ):
    def contract: Option[ProofContract] =
      ProofContractRules.contractForProofFamily(proofFamily)

    def tsv: String =
      List(
        planKindId,
        proofFamily,
        proofSource,
        acceptedScope.toString,
        defaultAuthorityTier,
        requiredWitnesses.map(_.toString).toList.sorted.mkString("+"),
        sourceCandidateTarget.toString,
        maxAuthorityRows.toString,
        negativeControls.map(control => s"${control.kind}:${control.expectedBlocker}").mkString("+"),
        contract.map(_.id).getOrElse("-"),
        contract.map(_.status.toString).getOrElse("-")
      ).mkString("\t")

  val sourceCandidateTarget: SourceCandidateTarget =
    SourceCandidateTarget(2, 5)

  val maxAuthorityRowsPerPass: Int = 2

  val requiredNegativeKinds: List[NegativeControlKind] =
    List(
      NegativeControlKind.TacticalFirst,
      NegativeControlKind.RivalOrGenericRelabel,
      NegativeControlKind.MissingWitness
    )

  private val docs =
    DocumentationTargets(
      corpusLedger = "tmp/strategic_claim_source_witnesses.md",
      trustDoc = "modules/commentary/docs/CommentaryTrustBoundary.md",
      surfaceReview = "tmp/strategic_claim_authority_surface_review.md"
    )

  private def supportedLocalPositive(proofSource: String): ControlledPositiveRequirement =
    ControlledPositiveRequirement(
      count = 1,
      expectedPacket = s"proofSource=$proofSource;scope=MoveLocal;authority=SupportedLocal",
      expectedSurface = "A local reading is that ..."
    )

  private def defaultNegatives(planKindId: String): List[NegativeControlRequirement] =
    List(
      NegativeControlRequirement(NegativeControlKind.TacticalFirst, "tactical:first"),
      NegativeControlRequirement(NegativeControlKind.RivalOrGenericRelabel, s"owner:${planKindId}_rival_or_relabel"),
      NegativeControlRequirement(NegativeControlKind.MissingWitness, s"owner:${planKindId}_witness_missing")
    )

  private def unit(
      planKindId: String,
      proofFamily: String,
      proofSource: String,
      acceptedScope: PlayerFacingPacketScope,
      requiredWitnesses: Set[ProofWitness],
      defaultAuthorityTier: String = "SupportedLocal"
  ): AdmissionUnitSpec =
    AdmissionUnitSpec(
      planKindId = planKindId,
      proofFamily = proofFamily,
      proofSource = proofSource,
      acceptedScope = acceptedScope,
      defaultAuthorityTier = defaultAuthorityTier,
      requiredWitnesses = requiredWitnesses,
      controlledPositive = supportedLocalPositive(proofSource),
      negativeControls = defaultNegatives(planKindId),
      sourceCandidateTarget = sourceCandidateTarget,
      maxAuthorityRows = maxAuthorityRowsPerPass,
      documentationTargets = docs
    )

  private val defaultWitnesses =
    Set(
      ProofWitness.OwnerSeed,
      ProofWitness.Continuation,
      ProofWitness.NoRivalRelease,
      ProofWitness.NoTacticalVeto,
      ProofWitness.ClaimOnlySurface
    )

  val admissionUnits: List[AdmissionUnitSpec] =
    List(
      unit(
        planKindId = PlanTaxonomy.PlanKind.BreakPrevention.id,
        proofFamily = "neutralize_key_break",
        proofSource = "counterplay_axis_suppression",
        acceptedScope = PlayerFacingPacketScope.MoveLocal,
        requiredWitnesses = defaultWitnesses + ProofWitness.ExactSlice
      ),
      unit(
        planKindId = PlanTaxonomy.PlanKind.ProphylaxisRestraint.id,
        proofFamily = "counterplay_restraint",
        proofSource = "prophylactic_move",
        acceptedScope = PlayerFacingPacketScope.MoveLocal,
        requiredWitnesses = defaultWitnesses + ProofWitness.ExactSlice
      ),
      unit(
        planKindId = PlanTaxonomy.PlanKind.OpenFilePressure.id,
        proofFamily = "half_open_file_pressure",
        proofSource = "local_file_entry_bind",
        acceptedScope = PlayerFacingPacketScope.MoveLocal,
        requiredWitnesses = defaultWitnesses + ProofWitness.ExactSlice
      ),
      unit(
        planKindId = PlanTaxonomy.PlanKind.BadPieceLiquidation.id,
        proofFamily = PlanTaxonomy.PlanKind.BadPieceLiquidation.id,
        proofSource = "bad_piece_liquidation",
        acceptedScope = PlayerFacingPacketScope.MoveLocal,
        requiredWitnesses = defaultWitnesses
      ),
      unit(
        planKindId = PlanTaxonomy.PlanKind.CentralBreakTiming.id,
        proofFamily = PlanTaxonomy.PlanKind.CentralBreakTiming.id,
        proofSource = PlanTaxonomy.PlanKind.CentralBreakTiming.id,
        acceptedScope = PlayerFacingPacketScope.LineScoped,
        defaultAuthorityTier = "Deferred",
        requiredWitnesses =
          Set(
            ProofWitness.OwnerSeed,
            ProofWitness.NoTacticalVeto
          )
      )
    )

  private val header =
    List(
      "planKindId",
      "proofFamily",
      "proofSource",
      "acceptedScope",
      "defaultAuthorityTier",
      "requiredWitnesses",
      "sourceCandidateTarget",
      "maxAuthorityRows",
      "negativeControls",
      "contractId",
      "contractStatus"
    ).mkString("\t")

  def tsv: String =
    (header :: admissionUnits.map(_.tsv)).mkString("\n") + "\n"

  def markdown: String =
    val rows =
      admissionUnits
        .map { unit =>
          s"| `${unit.planKindId}` | `${unit.proofSource}` | `${unit.proofFamily}` | `${unit.defaultAuthorityTier}` | `${unit.sourceCandidateTarget}` | `${unit.maxAuthorityRows}` | `${unit.contract.map(_.status.toString).getOrElse("-")}` |"
        }
        .mkString("\n")

    List(
      "# Strategic Admission Unit Catalog",
      "",
      "This is the repeatable proof shape for future strategic admission-unit work.",
      "Each pass is 1 plan kind, 1 proof source, 1 controlled positive, 3 negative",
      "controls, 2-5 natural source candidates, and 0-2 authority rows.",
      "",
      "Authority defaults:",
      "",
      "- CertifiedOwner requires PV1 source-move agreement.",
      "- SupportedLocal may use near-top MultiPV only when exact proof packet,",
      "  planner authority, tactical veto, and claim-only surfaces all pass.",
      "- Tactical-first remains absolute over strategic prose.",
      "- Runtime contracts must not contain source witness ids.",
      "",
      "| plan kind | proof source | proof family | default authority | source candidates | max authority rows | contract status |",
      "| --- | --- | --- | --- | --- | --- | --- |",
      rows,
      "",
      "Documentation targets:",
      "",
      s"- `${docs.corpusLedger}`",
      s"- `${docs.trustDoc}`",
      s"- `${docs.surfaceReview}`",
      ""
    ).mkString("\n")

object runAdmissionUnitCatalog:
  def main(args: Array[String]): Unit =
    val root = Paths.get("tmp")
    Files.createDirectories(root)
    val tsvPath = root.resolve("strategic_admission_unit_catalog.tsv")
    val mdPath = root.resolve("strategic_admission_unit_catalog.md")
    Files.write(tsvPath, AdmissionUnitCatalog.tsv.getBytes(StandardCharsets.UTF_8))
    Files.write(mdPath, AdmissionUnitCatalog.markdown.getBytes(StandardCharsets.UTF_8))
    println(s"wrote=$tsvPath")
    println(s"review=$mdPath")

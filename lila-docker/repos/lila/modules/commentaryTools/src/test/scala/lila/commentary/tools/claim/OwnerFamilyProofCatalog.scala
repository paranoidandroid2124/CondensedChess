package lila.commentary.tools.claim

import lila.commentary.analysis.*
import lila.commentary.analysis.claim.*
import java.nio.charset.StandardCharsets
import java.nio.file.{ Files, Paths }

private[commentary] object OwnerFamilyProofCatalog:

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

  final case class OwnerFamilyProofContract(
      familyId: String,
      ownerFamily: String,
      ownerSource: String,
      acceptedScope: PlayerFacingPacketScope,
      defaultAuthorityTier: String,
      requiredWitnesses: Set[OwnerProofWitness],
      controlledPositive: ControlledPositiveRequirement,
      negativeControls: List[NegativeControlRequirement],
      sourceCandidateTarget: SourceCandidateTarget,
      maxAuthorityRows: Int,
      documentationTargets: DocumentationTargets
  ):
    def contract: Option[OwnerProofContract] =
      OwnerProofRules.contractForFamily(ownerFamily)

    def tsv: String =
      List(
        familyId,
        ownerFamily,
        ownerSource,
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

  private def supportedLocalPositive(ownerSource: String): ControlledPositiveRequirement =
    ControlledPositiveRequirement(
      count = 1,
      expectedPacket = s"ownerSource=$ownerSource;scope=MoveLocal;authority=SupportedLocal",
      expectedSurface = "A local reading is that ..."
    )

  private def defaultNegatives(family: String): List[NegativeControlRequirement] =
    List(
      NegativeControlRequirement(NegativeControlKind.TacticalFirst, "tactical:first"),
      NegativeControlRequirement(NegativeControlKind.RivalOrGenericRelabel, s"owner:${family}_rival_or_relabel"),
      NegativeControlRequirement(NegativeControlKind.MissingWitness, s"owner:${family}_witness_missing")
    )

  private def unit(
      familyId: String,
      ownerFamily: String,
      ownerSource: String,
      acceptedScope: PlayerFacingPacketScope,
      requiredWitnesses: Set[OwnerProofWitness],
      defaultAuthorityTier: String = "SupportedLocal"
  ): OwnerFamilyProofContract =
    OwnerFamilyProofContract(
      familyId = familyId,
      ownerFamily = ownerFamily,
      ownerSource = ownerSource,
      acceptedScope = acceptedScope,
      defaultAuthorityTier = defaultAuthorityTier,
      requiredWitnesses = requiredWitnesses,
      controlledPositive = supportedLocalPositive(ownerSource),
      negativeControls = defaultNegatives(familyId),
      sourceCandidateTarget = sourceCandidateTarget,
      maxAuthorityRows = maxAuthorityRowsPerPass,
      documentationTargets = docs
    )

  private val defaultWitnesses =
    Set(
      OwnerProofWitness.OwnerSeed,
      OwnerProofWitness.Continuation,
      OwnerProofWitness.NoRivalRelease,
      OwnerProofWitness.NoTacticalVeto,
      OwnerProofWitness.ClaimOnlySurface
    )

  val ownerFamilyProofCatalog: List[OwnerFamilyProofContract] =
    List(
      unit(
        familyId = ThemeTaxonomy.SubplanId.BreakPrevention.id,
        ownerFamily = "neutralize_key_break",
        ownerSource = "counterplay_axis_suppression",
        acceptedScope = PlayerFacingPacketScope.MoveLocal,
        requiredWitnesses = defaultWitnesses + OwnerProofWitness.ExactSlice
      ),
      unit(
        familyId = ThemeTaxonomy.SubplanId.ProphylaxisRestraint.id,
        ownerFamily = "counterplay_restraint",
        ownerSource = "prophylactic_move",
        acceptedScope = PlayerFacingPacketScope.MoveLocal,
        requiredWitnesses = defaultWitnesses + OwnerProofWitness.ExactSlice
      ),
      unit(
        familyId = ThemeTaxonomy.SubplanId.OpenFilePressure.id,
        ownerFamily = "half_open_file_pressure",
        ownerSource = "local_file_entry_bind",
        acceptedScope = PlayerFacingPacketScope.MoveLocal,
        requiredWitnesses = defaultWitnesses + OwnerProofWitness.ExactSlice
      ),
      unit(
        familyId = ThemeTaxonomy.SubplanId.BadPieceLiquidation.id,
        ownerFamily = ThemeTaxonomy.SubplanId.BadPieceLiquidation.id,
        ownerSource = "bad_piece_liquidation",
        acceptedScope = PlayerFacingPacketScope.MoveLocal,
        requiredWitnesses = defaultWitnesses
      ),
      unit(
        familyId = ThemeTaxonomy.SubplanId.CentralBreakTiming.id,
        ownerFamily = ThemeTaxonomy.SubplanId.CentralBreakTiming.id,
        ownerSource = ThemeTaxonomy.SubplanId.CentralBreakTiming.id,
        acceptedScope = PlayerFacingPacketScope.LineScoped,
        defaultAuthorityTier = "Deferred",
        requiredWitnesses =
          Set(
            OwnerProofWitness.OwnerSeed,
            OwnerProofWitness.NoTacticalVeto
          )
      )
    )

  private val header =
    List(
      "familyId",
      "ownerFamily",
      "ownerSource",
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
    (header :: ownerFamilyProofCatalog.map(_.tsv)).mkString("\n") + "\n"

  def markdown: String =
    val rows =
      ownerFamilyProofCatalog
        .map { unit =>
          s"| `${unit.familyId}` | `${unit.ownerSource}` | `${unit.ownerFamily}` | `${unit.defaultAuthorityTier}` | `${unit.sourceCandidateTarget}` | `${unit.maxAuthorityRows}` | `${unit.contract.map(_.status.toString).getOrElse("-")}` |"
        }
        .mkString("\n")

    List(
      "# Strategic Owner Family Proof Catalog",
      "",
      "This is the repeatable proof shape for future strategic family work.",
      "Each pass is 1 family, 1 owner source, 1 controlled positive, 3 negative",
      "controls, 2-5 natural source candidates, and 0-2 authority rows.",
      "",
      "Authority defaults:",
      "",
      "- CertifiedOwner requires PV1 source-move agreement.",
      "- SupportedLocal may use near-top MultiPV only when exact owner packet,",
      "  planner authority, tactical veto, and claim-only surfaces all pass.",
      "- Tactical-first remains absolute over strategic prose.",
      "- Runtime contracts must not contain source witness ids.",
      "",
      "| family | owner source | owner family | default authority | source candidates | max authority rows | contract status |",
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

object runOwnerFamilyProofCatalog:
  def main(args: Array[String]): Unit =
    val root = Paths.get("tmp")
    Files.createDirectories(root)
    val tsvPath = root.resolve("strategic_owner_family_proof_catalog.tsv")
    val mdPath = root.resolve("strategic_owner_family_proof_catalog.md")
    Files.write(tsvPath, OwnerFamilyProofCatalog.tsv.getBytes(StandardCharsets.UTF_8))
    Files.write(mdPath, OwnerFamilyProofCatalog.markdown.getBytes(StandardCharsets.UTF_8))
    println(s"wrote=$tsvPath")
    println(s"review=$mdPath")

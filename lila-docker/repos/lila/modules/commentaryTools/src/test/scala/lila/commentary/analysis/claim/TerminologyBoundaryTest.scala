package lila.commentary.analysis.claim

import java.nio.charset.StandardCharsets
import java.nio.file.{ Files, Path, Paths }
import scala.jdk.CollectionConverters.*

import munit.FunSuite

class TerminologyBoundaryTest extends FunSuite:

  private val forbidden =
    List(
      "OwnerFamilyProofCatalog",
      "FamilyCoverageAdmission",
      "ownerFamily",
      "ownerSource",
      "familyId",
      "owner_family",
      "owner_source",
      "mainClaimSource",
      "sourceAdmittedAuthorityRowIds",
      "sourceAuthorityRowIds",
      "Source admitted authority rows",
      "Natural SupportedLocal search",
      "Maderna-Palermo, Camara-Bazan, and Pfleger-Maalouf are current natural SupportedLocal rows"
    )

  private val legacyCommentarySymbols =
    List(
      "Commentary" + "FactSurface",
      "MoveReview" + "IdeaSurface",
      "MoveReviewPv" + "ChainValidator",
      "MoveReviewPv" + "Facts",
      "truth" + "MotifId"
    )

  test("source and tracked snapshots do not reintroduce ambiguous layer names") {
    val files =
      scalaFiles(Paths.get("modules/commentaryCore/src/main/scala")) ++
        scalaFiles(Paths.get("modules/commentaryTools/src/test/scala")) ++
        snapshotFiles(Paths.get("tmp"))

    val offenders =
      files
        .filterNot(path => path.endsWith(Paths.get("TerminologyBoundaryTest.scala")))
        .flatMap { path =>
          val text = Files.readString(path, StandardCharsets.UTF_8)
          forbidden.filter(text.contains).map(term => s"${path.toString}:$term")
        }

    assertEquals(offenders, Nil)
  }

  test("commentary surface cleanup does not reintroduce legacy boundary symbols") {
    val files =
      scalaFiles(Paths.get("modules/commentaryCore/src/main/scala")) ++
        scalaFiles(Paths.get("modules/commentaryTools/src/test/scala")) ++
        markdownFiles(Paths.get("modules/commentary/docs"))

    val offenders =
      files
        .filterNot(path => path.endsWith(Paths.get("TerminologyBoundaryTest.scala")))
        .flatMap { path =>
          val text = Files.readString(path, StandardCharsets.UTF_8)
          legacyCommentarySymbols.filter(text.contains).map(term => s"${path.toString}:$term")
        }

    assertEquals(offenders, Nil)
  }

  private def scalaFiles(root: Path): List[Path] =
    if !Files.exists(root) then Nil
    else
      Files
        .walk(root)
        .iterator()
        .asScala
        .filter(path => Files.isRegularFile(path) && path.toString.endsWith(".scala"))
        .toList

  private def snapshotFiles(root: Path): List[Path] =
    if !Files.exists(root) then Nil
    else
      val names =
        Set(
          "strategic_admission_unit_catalog.md",
          "strategic_admission_unit_catalog.tsv",
          "strategic_admission_unit_review.md",
          "strategic_admission_unit_review.tsv",
          "strategic_plan_kind_admission_matrix.md",
          "strategic_plan_kind_admission_matrix.tsv",
          "strategic_claim_source_review.md",
          "strategic_claim_source_review.tsv",
          "strategic_claim_source_window_review.md",
          "strategic_claim_source_window_review.tsv",
          "strategic_claim_authority_surface_review.md",
          "strategic_claim_authority_surface_ledger.tsv"
        )
      Files
        .walk(root)
        .iterator()
        .asScala
        .filter(path => Files.isRegularFile(path) && names.contains(path.getFileName.toString))
        .toList

  private def markdownFiles(root: Path): List[Path] =
    if !Files.exists(root) then Nil
    else
      Files
        .walk(root)
        .iterator()
        .asScala
        .filter(path => Files.isRegularFile(path) && path.toString.endsWith(".md"))
        .toList

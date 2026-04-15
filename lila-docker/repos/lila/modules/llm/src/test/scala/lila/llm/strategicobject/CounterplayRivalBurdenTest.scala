package lila.llm.strategicobject

import scala.concurrent.duration.*
import scala.io.Source
import scala.util.Using

import munit.FunSuite
import play.api.libs.json.*

class CounterplayRivalBurdenTest extends FunSuite:
  import CounterplayRivalBurdenTest.*

  override val munitTimeout = 150.seconds

  test("claim certification stamps the live CounterplayAxis rival burden metadata without widening other families") {
    counterplayRows.foreach { row =>
      val runtime = runtimeFor(row)
      val counterplayClaim =
        runtime.claims.find(claim =>
          claim.objectId == row.objectId &&
            claim.deltaScope == StrategicDeltaScope.MoveLocal &&
            claim.status == ClaimStatus.SupportOnly
        ).getOrElse(
          fail(s"expected support-only CounterplayAxis move-local claim for ${row.rowId}")
        )
      val burden =
        counterplayClaim.plannerMetadata.counterplayRivalBurden.getOrElse(
          fail(s"expected counterplay rival burden metadata for ${row.rowId}")
        )
      val expectedUnmatched =
        row.expectedAdmittedRivalIds.filterNot(row.expectedMatchedRivalIds.contains)

      assertEquals(burden.allMatchedRivalLegsMustCertify, true, clue(counterplayClaim))
      assertEquals(
        burden.coEqualRivalLegs.map(_.objectId).sorted,
        row.expectedMatchedRivalIds.sorted,
        clue(burden)
      )
      assertEquals(
        burden.admittedButUnmatchedRivalLegs.map(_.objectId).sorted,
        expectedUnmatched.sorted,
        clue(burden)
      )

      if row.rowId == "CounterplayAxis@L2482" then
        assertEquals(burden.coEqualRivalLegs.size, 2, clue(burden))
        assertEquals(burden.admittedButUnmatchedRivalLegs.size, 0, clue(burden))
      else
        assertEquals(burden.coEqualRivalLegs.size, 1, clue(burden))
        assertEquals(burden.admittedButUnmatchedRivalLegs.size, 1, clue(burden))
    }
  }

  test("planner keeps live CounterplayAxis support attached only while all matched rival legs certify and reads metadata instead of raw rival truth") {
    counterplayRows.foreach { row =>
      val runtime = runtimeFor(row)
      val counterplayClaim =
        runtime.claims.find(claim =>
          claim.objectId == row.objectId &&
            claim.deltaScope == StrategicDeltaScope.MoveLocal &&
            claim.status == ClaimStatus.SupportOnly
        ).getOrElse(
          fail(s"expected support-only CounterplayAxis move-local claim for ${row.rowId}")
        )
      val burden =
        counterplayClaim.plannerMetadata.counterplayRivalBurden.getOrElse(
          fail(s"expected counterplay rival burden metadata for ${row.rowId}")
        )
      val planned = CanonicalQuestionPlanner.plan(runtime.contract, runtime.claims)
      val metadataOnlyClaims =
        runtime.claims.map(claim =>
          if claim.id == counterplayClaim.id then stripCounterplayRawRivalEvidence(claim)
          else claim
        )
      val missingMatchedRivalClaims =
        runtime.claims.filterNot(claim =>
          burden.coEqualRivalLegs.exists(_.objectId == claim.objectId)
        )
      val metadataOnlyPlanned = CanonicalQuestionPlanner.plan(runtime.contract, metadataOnlyClaims)
      val missingMatchedRivalPlanned =
        CanonicalQuestionPlanner.plan(runtime.contract, missingMatchedRivalClaims)

      assert(
        planned.supportClaimIds.contains(counterplayClaim.id),
        clue(s"${row.rowId}: expected planner_support claim ${counterplayClaim.id}")
      )
      assert(
        !planned.claimIds.contains(counterplayClaim.id),
        clue(s"${row.rowId}: CounterplayAxis support slice must not reopen primary")
      )
      assert(
        metadataOnlyPlanned.supportClaimIds.contains(counterplayClaim.id),
        clue(s"${row.rowId}: planner should consume certified metadata, not raw rival evidence")
      )
      assert(
        !missingMatchedRivalPlanned.supportClaimIds.contains(counterplayClaim.id),
        clue(s"${row.rowId}: owner-only salvage must stay closed when a matched rival leg is removed")
      )
    }
  }

  test("RestrictionShell and TradeInvariant live guard rows keep their existing planner semantics and never inherit counterplay burden metadata") {
    guardRows.foreach { row =>
      val runtime = runtimeFor(row)
      val rowClaims = runtime.claims.filter(_.objectId == row.objectId)

      assert(rowClaims.nonEmpty, clue(row))
      assert(
        rowClaims.forall(_.plannerMetadata.counterplayRivalBurden.isEmpty),
        clue(s"${row.rowId}: unexpected counterplay burden on ${row.objectId}")
      )

      row.familyCluster match
        case "RestrictionShell" =>
          assert(
            rowClaims.exists(_.plannerMetadata.currentPositionProbeKind.map(_.toString) == row.expectedCurrentPositionProbeKind),
            clue(s"${row.rowId}: expected current-position probe kind ${row.expectedCurrentPositionProbeKind}")
          )
          row.expectedSharedTargetContinuity.foreach { expected =>
            if expected then
              assert(rowClaims.exists(_.plannerMetadata.sharedTargetContinuity), clue(rowClaims))
            else
              assert(rowClaims.forall(claim => !claim.plannerMetadata.sharedTargetContinuity), clue(rowClaims))
          }
        case "TradeInvariant" =>
          assert(
            rowClaims.exists(_.plannerMetadata.tradeInvariantPrimaryClass.map(_.toString) == row.expectedPrimaryReason),
            clue(s"${row.rowId}: expected trade primary reason ${row.expectedPrimaryReason}")
          )
        case other =>
          fail(s"unexpected guard family cluster: $other")
    }
  }

  private def runtimeFor(
      row: LiveUniverseRow
  ): RuntimeContext =
    val truth = PrimitiveExtractionTest.moveTransitionVisibleTruthFrameFor(row.playedUci)
    val contract = PrimitiveExtractionTest.moveTransitionVisibleContractFor(row.playedUci)
    val objects = StrategicObjectSynthesizerTest.objectsForFen(row.fen, truth)
    val deltas = CanonicalStrategicObjectDeltaProjector.project(contract, truth, objects)
    val claims = CanonicalClaimCertification.certify(contract, objects, deltas)

    RuntimeContext(
      contract = contract,
      claims = claims
    )

  private def stripCounterplayRawRivalEvidence(
      claim: CertifiedClaim
  ): CertifiedClaim =
    claim.copy(
      delta =
        claim.delta.map(delta =>
          delta.copy(
            projection =
              delta.projection match
                case StrategicDeltaProjection.MoveLocal(change, witness) =>
                  StrategicDeltaProjection.MoveLocal(
                    change,
                    witness.copy(counterplayRivalEvidence = None)
                  )
                case other =>
                  other
          )
        )
    )

object CounterplayRivalBurdenTest:

  private final case class LiveUniverseRow(
      rowId: String,
      familyCluster: String,
      sampleId: String,
      fen: String,
      playedUci: String,
      objectId: String,
      expectedMatchedRivalIds: List[String],
      expectedAdmittedRivalIds: List[String],
      expectedCurrentPositionProbeKind: Option[String],
      expectedSharedTargetContinuity: Option[Boolean],
      expectedPrimaryReason: Option[String]
  )

  private final case class RuntimeContext(
      contract: lila.llm.analysis.DecisiveTruthContract,
      claims: List[CertifiedClaim]
  )

  private given Reads[LiveUniverseRow] = Json.reads[LiveUniverseRow]

  private val rows: List[LiveUniverseRow] =
    Using.resource(
      Option(
        getClass.getClassLoader.getResourceAsStream(
          "strategic-object-corpus/counterplay-rival-burden-live-universe.jsonl"
        )
      ).getOrElse(
        throw new IllegalStateException(
          "missing resource: strategic-object-corpus/counterplay-rival-burden-live-universe.jsonl"
        )
      )
    ) { stream =>
      Source
        .fromInputStream(stream, "UTF-8")
        .getLines()
        .toList
        .filter(_.trim.nonEmpty)
        .map(line =>
          Json.parse(line).validate[LiveUniverseRow].asEither match
            case Right(row) => row
            case Left(error) =>
              throw new IllegalArgumentException(s"invalid live-universe row: $line\n$error")
        )
    }

  private val counterplayRows: List[LiveUniverseRow] =
    rows.filter(_.familyCluster == "CounterplayAxis")

  private val guardRows: List[LiveUniverseRow] =
    rows.filter(row =>
      row.familyCluster == "RestrictionShell" || row.familyCluster == "TradeInvariant"
    )

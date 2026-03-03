package lila.llm

import lila.db.dsl.*
import lila.llm.model.CollapseAnalysis
import reactivemongo.api.bson.*
import reactivemongo.api.indexes.{ Index, IndexType }

final class CcaHistoryRepo(val coll: Coll)(using Executor):

  import CcaHistoryRepo.given

  // Ensure indexes on construction (idempotent — no-op if already exist)
  ensureIndexes()

  private val MaxPerUser = 50

  /** Insert one or more CCA records for a user, then trim to cap. */
  def insert(uid: String, records: List[CollapseAnalysis]): Funit =
    records.nonEmpty.so:
      val docs = records.map: r =>
        $doc(
          "uid"      -> uid,
          "interval" -> r.interval,
          "rootCause" -> r.rootCause,
          "epp"      -> r.earliestPreventablePly,
          "patch"    -> r.patchLineUci,
          "recov"    -> r.recoverabilityPlies,
          "at"       -> java.time.Instant.now()
        )
      coll.insert(ordered = false).many(docs).void.andDo(trimExcess(uid))

  /** Delete oldest records beyond per-user cap. */
  private def trimExcess(uid: String): Unit =
    // Find the `at` of the Nth-newest record; delete everything older
    coll
      .find($doc("uid" -> uid), $doc("at" -> true).some)
      .sort($doc("at" -> -1))
      .skip(MaxPerUser)
      .one[Bdoc]
      .foreach:
        case Some(doc) =>
          doc.getAsOpt[java.time.Instant]("at").foreach: cutoff =>
            coll.delete.one($doc("uid" -> uid, "at" -> $doc("$lte" -> cutoff)))
        case None => () // within cap, nothing to trim

  /** Retrieve the most recent CCA records for a user. */
  def recent(uid: String, limit: Int = 30): Fu[List[CollapseAnalysis]] =
    coll
      .find($doc("uid" -> uid), none[Bdoc])
      .sort($doc("at" -> -1))
      .cursor[Bdoc]()
      .list(limit)
      .map(_.map(readDoc))

  private def readDoc(doc: Bdoc): CollapseAnalysis =
    CollapseAnalysis(
      interval = doc.getAsOpt[String]("interval").getOrElse(""),
      rootCause = doc.getAsOpt[String]("rootCause").getOrElse(""),
      earliestPreventablePly = doc.getAsOpt[Int]("epp").getOrElse(0),
      patchLineUci = doc.getAsOpt[List[String]]("patch").getOrElse(Nil),
      recoverabilityPlies = doc.getAsOpt[Int]("recov").getOrElse(0)
    )

  private def ensureIndexes(): Unit =
    import scala.concurrent.duration.*
    // {uid: 1, at: -1} — per-user recent-first query
    coll.indexesManager.ensure(
      Index(
        key = Seq("uid" -> IndexType.Ascending, "at" -> IndexType.Descending),
        name = Some("uid_at"),
        unique = false
      )
    )
    // {at: 1} with TTL 30 days — auto-expire old records
    coll.indexesManager.ensure(
      Index(
        key = Seq("at" -> IndexType.Ascending),
        name = Some("at_ttl"),
        unique = false,
        options = $doc("expireAfterSeconds" -> 2592000)
      )
    )

object CcaHistoryRepo:
  given BSONDocumentWriter[CollapseAnalysis] = BSONDocumentWriter[CollapseAnalysis]: r =>
    $doc(
      "interval"  -> r.interval,
      "rootCause" -> r.rootCause,
      "epp"       -> r.earliestPreventablePly,
      "patch"     -> r.patchLineUci,
      "recov"     -> r.recoverabilityPlies
    )


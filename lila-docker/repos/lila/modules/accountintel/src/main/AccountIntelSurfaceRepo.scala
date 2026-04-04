package lila.accountintel

import java.time.Instant
import reactivemongo.api.indexes.{ Index, IndexType }

import lila.accountintel.AccountIntel.AccountIntelSurfaceSnapshot
import lila.db.dsl.{ *, given }

final class AccountIntelSurfaceRepo(coll: Coll)(using Executor) extends AccountIntel.AccountIntelSurfaceStore:

  import BSONHandlers.given

  ensureIndexes()

  private object F:
    val dedupeKey = "dedupeKey"
    val sourceFingerprint = "sourceFingerprint"
    val updatedAt = "updatedAt"

  def byId(id: String): Fu[Option[AccountIntelSurfaceSnapshot]] =
    coll.one[AccountIntelSurfaceSnapshot]($id(id))

  def latestByDedupeKey(dedupeKey: String): Fu[Option[AccountIntelSurfaceSnapshot]] =
    coll
      .find($doc(F.dedupeKey -> dedupeKey))
      .sort($sort.desc(F.updatedAt))
      .one[AccountIntelSurfaceSnapshot]

  def latestFreshByDedupeKey(
      dedupeKey: String,
      freshSince: Instant
  ): Fu[Option[AccountIntelSurfaceSnapshot]] =
    coll
      .find($doc(F.dedupeKey -> dedupeKey, F.updatedAt.$gte(freshSince)))
      .sort($sort.desc(F.updatedAt))
      .one[AccountIntelSurfaceSnapshot]

  def byDedupeKeyAndFingerprint(
      dedupeKey: String,
      sourceFingerprint: String
  ): Fu[Option[AccountIntelSurfaceSnapshot]] =
    coll.one[AccountIntelSurfaceSnapshot]($doc(F.dedupeKey -> dedupeKey, F.sourceFingerprint -> sourceFingerprint))

  def insert(snapshot: AccountIntelSurfaceSnapshot): Funit =
    coll.insert.one(snapshot).void

  private def ensureIndexes(): Unit =
    coll.indexesManager.ensure(
      Index(
        key = Seq(F.dedupeKey -> IndexType.Ascending, F.updatedAt -> IndexType.Descending),
        name = Some("surface_dedupe_updated")
      )
    )
    coll.indexesManager.ensure(
      Index(
        key = Seq(F.dedupeKey -> IndexType.Ascending, F.sourceFingerprint -> IndexType.Ascending),
        name = Some("surface_dedupe_fingerprint"),
        unique = true
      )
    )

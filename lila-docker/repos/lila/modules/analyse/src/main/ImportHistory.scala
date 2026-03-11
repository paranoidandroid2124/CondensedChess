package lila.analyse

import chess.format.pgn.PgnStr
import com.roundeights.hasher.Algo
import reactivemongo.api.bson.*
import reactivemongo.api.indexes.{ Index, IndexType }
import scalalib.ThreadLocalRandom

import java.time.Instant

import lila.db.dsl.{ *, given }
import lila.tree.ParseImport

object ImportHistory:

  val providerLichess  = "lichess"
  val providerChessCom = "chesscom"
  val providers        = Set(providerLichess, providerChessCom)

  val sourceManual  = "manual"
  val sourceAccount = "account"

  val maxAccountsPerUser  = 24
  val maxAnalysesPerUser  = 120
  val recentAccountLimit  = 6
  val recentAnalysisLimit = 8

  private[analyse] val externalUsernameRegex = "^[A-Za-z0-9][A-Za-z0-9_-]{1,29}$".r

  case class Account(
      _id: String,
      userId: UserId,
      provider: String,
      username: String,
      usernameKey: String,
      activityAt: Instant,
      lastSearchedAt: Instant,
      lastAnalysedAt: Option[Instant],
      searchCount: Int,
      analysisCount: Int,
      createdAt: Instant
  )

  case class Analysis(
      _id: String,
      userId: UserId,
      sourceType: String,
      provider: Option[String],
      username: Option[String],
      externalGameId: Option[String],
      sourceUrl: Option[String],
      white: Option[String],
      black: Option[String],
      result: Option[String],
      speed: Option[String],
      playedAtLabel: Option[String],
      event: Option[String],
      opening: Option[String],
      variant: Option[String],
      title: String,
      normalizedPgn: String,
      pgnHash: String,
      lastOpenedAt: Instant,
      createdAt: Instant
  )

  case class AnalysisSource(
      sourceType: String = sourceManual,
      provider: Option[String] = None,
      username: Option[String] = None,
      externalGameId: Option[String] = None,
      sourceUrl: Option[String] = None,
      white: Option[String] = None,
      black: Option[String] = None,
      result: Option[String] = None,
      speed: Option[String] = None,
      playedAtLabel: Option[String] = None
  ):
    def normalized: AnalysisSource =
      val providerNorm = clean(provider, 32).map(_.toLowerCase).filter(providers)
      val usernameNorm = clean(username, 30).filter(externalUsernameRegex.matches)
      copy(
        sourceType = if providerNorm.isDefined then sourceAccount else sourceManual,
        provider = providerNorm,
        username = usernameNorm,
        externalGameId = providerNorm.flatMap(_ => clean(externalGameId, 80)),
        sourceUrl = clean(sourceUrl, 1024),
        white = clean(white, 80),
        black = clean(black, 80),
        result = clean(result, 16),
        speed = clean(speed, 32),
        playedAtLabel = clean(playedAtLabel, 64)
      )

  case class RecentSummary(accounts: List[Account], analyses: List[Analysis])
  object RecentSummary:
    val empty = RecentSummary(Nil, Nil)

  private[analyse] case class DerivedMetadata(
      white: Option[String],
      black: Option[String],
      result: Option[String],
      playedAtLabel: Option[String],
      event: Option[String],
      opening: Option[String],
      variant: Option[String],
      title: String
  )

  private[analyse] def deriveMetadata(normalizedPgn: String): DerivedMetadata =
    ParseImport.full(PgnStr(normalizedPgn)).toOption.fold(
      DerivedMetadata(
        white = None,
        black = None,
        result = None,
        playedAtLabel = None,
        event = None,
        opening = None,
        variant = None,
        title = "Imported PGN"
      )
    ) { imported =>
      val tags = imported.parsed.tags
      val white = clean(tags.names.white.map(_.value), 80)
      val black = clean(tags.names.black.map(_.value), 80)
      val result = clean(tags("Result"), 16)
      val event = clean(tags("Event"), 120)
      val opening = clean(tags("Opening"), 120)
      val variant = clean(Some(imported.game.variant.name), 40)
      val date = clean(tags("UTCDate").orElse(tags("Date")), 24)
      val time = clean(tags("UTCTime"), 24)
      val playedAtLabel = (date, time) match
        case (Some(d), Some(t)) => s"$d $t".some
        case (Some(d), None)    => Some(d)
        case _                  => None
      val title =
        white
          .zip(black)
          .map: (w, b) =>
            s"$w vs $b"
          .orElse(event)
          .orElse(opening)
          .getOrElse("Imported PGN")
      DerivedMetadata(
        white = white,
        black = black,
        result = result,
        playedAtLabel = playedAtLabel,
        event = event,
        opening = opening,
        variant = variant,
        title = title
      )
    }

  private[analyse] def clean(value: Option[String], max: Int): Option[String] =
    value.map(_.trim).filter(_.nonEmpty).map: v =>
      if v.length <= max then v else v.take(max)

final class ImportHistoryApi(
    accountColl: Coll,
    analysisColl: Coll
)(using Executor):

  import ImportHistory.*
  import ImportHistoryApi.given

  ensureIndexes()

  def recentSummary(
      userId: UserId,
      accountLimit: Int = recentAccountLimit,
      analysisLimit: Int = recentAnalysisLimit
  ): Fu[RecentSummary] =
    recentAccounts(userId, accountLimit).zip(recentAnalyses(userId, analysisLimit)).map(RecentSummary.apply)

  def recentAccounts(userId: UserId, limit: Int = recentAccountLimit): Fu[List[Account]] =
    accountColl
      .find($doc("userId" -> userId), none[Bdoc])
      .sort($doc("activityAt" -> -1))
      .cursor[Account]()
      .list(limit)

  def recentAnalyses(userId: UserId, limit: Int = recentAnalysisLimit): Fu[List[Analysis]] =
    analysisColl
      .find($doc("userId" -> userId), none[Bdoc])
      .sort($doc("lastOpenedAt" -> -1))
      .cursor[Analysis]()
      .list(limit)

  def byIdForUser(id: String, userId: UserId): Fu[Option[Analysis]] =
    analysisColl.find($id(id) ++ $doc("userId" -> userId), none[Bdoc]).one[Analysis]

  def openAnalysis(id: String, userId: UserId): Fu[Option[Analysis]] =
    byIdForUser(id, userId).flatMap:
      case Some(existing) =>
        val updated = existing.copy(lastOpenedAt = nowInstant)
        analysisColl.update.one($id(existing._id), updated).void
          .flatMap: _ =>
            updated.provider.zip(updated.username).so:
              case (provider, username) =>
                recordAccountAnalysis(userId, provider, username, updated.lastOpenedAt, incrementCount = false)
            .inject(updated.some)
      case None => fuccess(none)

  def recordAccountSearch(userId: UserId, provider: String, username: String): Funit =
    normalizedAccount(provider, username).so:
      case (providerNorm, usernameDisplay, usernameKey) =>
        val now = nowInstant
        val id = accountId(userId, providerNorm, usernameKey)
        accountColl.byId[Account](id).flatMap: existing =>
          val entry = existing.fold(
            Account(
              _id = id,
              userId = userId,
              provider = providerNorm,
              username = usernameDisplay,
              usernameKey = usernameKey,
              activityAt = now,
              lastSearchedAt = now,
              lastAnalysedAt = None,
              searchCount = 1,
              analysisCount = 0,
              createdAt = now
            )
          ) { current =>
            current.copy(
              username = usernameDisplay,
              activityAt = now,
              lastSearchedAt = now,
              searchCount = current.searchCount + 1
            )
          }
          accountColl.update.one($id(id), entry, upsert = true).void.andDo(trimAccounts(userId))

  def recordAnalysis(
      userId: UserId,
      normalizedPgn: String,
      source: AnalysisSource = AnalysisSource()
  ): Fu[Analysis] =
    val normalizedSource = source.normalized
    val derived = deriveMetadata(normalizedPgn)
    val pgnHash = Algo.sha256(normalizedPgn).hex
    val now = nowInstant

    findExistingAnalysis(userId, normalizedSource, pgnHash).flatMap: existing =>
      val provider = normalizedSource.provider.orElse(existing.flatMap(_.provider))
      val username = normalizedSource.username.orElse(existing.flatMap(_.username))
      val externalGameId = normalizedSource.externalGameId.orElse(existing.flatMap(_.externalGameId))
      val sourceUrl = normalizedSource.sourceUrl.orElse(existing.flatMap(_.sourceUrl))
      val white = normalizedSource.white.orElse(derived.white).orElse(existing.flatMap(_.white))
      val black = normalizedSource.black.orElse(derived.black).orElse(existing.flatMap(_.black))
      val result = normalizedSource.result.orElse(derived.result).orElse(existing.flatMap(_.result))
      val speed = normalizedSource.speed.orElse(existing.flatMap(_.speed))
      val playedAtLabel = normalizedSource.playedAtLabel.orElse(derived.playedAtLabel).orElse(existing.flatMap(_.playedAtLabel))
      val event = derived.event.orElse(existing.flatMap(_.event))
      val opening = derived.opening.orElse(existing.flatMap(_.opening))
      val variant = derived.variant.orElse(existing.flatMap(_.variant))
      val title = clean(Some(derived.title), 140).getOrElse(existing.fold("Imported PGN")(_.title))
      val linkedAccountChanged = provider.zip(username).exists { case (p, u) =>
        existing.forall(current => current.provider != p.some || current.username != u.some)
      }
      val entry = Analysis(
        _id = existing.fold(s"aih_${ThreadLocalRandom.nextString(12)}")(_._id),
        userId = userId,
        sourceType = if provider.isDefined then sourceAccount else sourceManual,
        provider = provider,
        username = username,
        externalGameId = externalGameId,
        sourceUrl = sourceUrl,
        white = white,
        black = black,
        result = result,
        speed = speed,
        playedAtLabel = playedAtLabel,
        event = event,
        opening = opening,
        variant = variant,
        title = title,
        normalizedPgn = normalizedPgn,
        pgnHash = pgnHash,
        lastOpenedAt = now,
        createdAt = existing.fold(now)(_.createdAt)
      )

      analysisColl.update.one($id(entry._id), entry, upsert = true).void
        .andDo(trimAnalyses(userId))
        .flatMap: _ =>
          provider.zip(username).so:
            case (p, u) => recordAccountAnalysis(userId, p, u, now, incrementCount = existing.isEmpty || linkedAccountChanged)
          .inject(entry)

  private def findExistingAnalysis(
      userId: UserId,
      source: AnalysisSource,
      pgnHash: String
  ): Fu[Option[Analysis]] =
    source.provider.zip(source.externalGameId) match
      case Some((provider, externalGameId)) =>
        analysisColl
          .find($doc("userId" -> userId, "provider" -> provider, "externalGameId" -> externalGameId), none[Bdoc])
          .one[Analysis]
          .flatMap:
            case some @ Some(_) => fuccess(some)
            case None =>
              analysisColl.find($doc("userId" -> userId, "pgnHash" -> pgnHash), none[Bdoc]).one[Analysis]
      case None =>
        analysisColl.find($doc("userId" -> userId, "pgnHash" -> pgnHash), none[Bdoc]).one[Analysis]

  private def recordAccountAnalysis(
      userId: UserId,
      provider: String,
      username: String,
      now: Instant,
      incrementCount: Boolean
  ): Funit =
    normalizedAccount(provider, username).so:
      case (providerNorm, usernameDisplay, usernameKey) =>
        val id = accountId(userId, providerNorm, usernameKey)
        accountColl.byId[Account](id).flatMap: existing =>
          val entry = existing.fold(
            Account(
              _id = id,
              userId = userId,
              provider = providerNorm,
              username = usernameDisplay,
              usernameKey = usernameKey,
              activityAt = now,
              lastSearchedAt = now,
              lastAnalysedAt = now.some,
              searchCount = 1,
              analysisCount = 1,
              createdAt = now
            )
          ) { current =>
            current.copy(
              username = usernameDisplay,
              activityAt = now,
              lastAnalysedAt = now.some,
              analysisCount = current.analysisCount + (if incrementCount then 1 else 0)
            )
          }
          accountColl.update.one($id(id), entry, upsert = true).void.andDo(trimAccounts(userId))

  private def normalizedAccount(provider: String, username: String): Option[(String, String, String)] =
    val providerNorm = clean(Some(provider), 32).map(_.toLowerCase).filter(providers)
    val usernameDisplay = clean(Some(username), 30).filter(externalUsernameRegex.matches)
    for
      p <- providerNorm
      display <- usernameDisplay
    yield (p, display, display.toLowerCase)

  private def accountId(userId: UserId, provider: String, usernameKey: String) =
    s"${userId.value}:$provider:$usernameKey"

  private def trimAccounts(userId: UserId): Funit =
    trimCollection(accountColl, $doc("userId" -> userId), "activityAt", maxAccountsPerUser)

  private def trimAnalyses(userId: UserId): Funit =
    trimCollection(analysisColl, $doc("userId" -> userId), "lastOpenedAt", maxAnalysesPerUser)

  private def trimCollection(coll: Coll, selector: Bdoc, sortField: String, keep: Int): Funit =
    coll
      .find(selector, $doc("_id" -> true).some)
      .sort($doc(sortField -> -1))
      .skip(keep)
      .cursor[Bdoc]()
      .list(keep * 4)
      .flatMap: docs =>
        val ids = docs.flatMap(_.getAsOpt[String]("_id"))
        ids.nonEmpty.so(coll.delete.one($inIds(ids)).void)

  private def ensureIndexes(): Unit =
    accountColl.indexesManager.ensure(
      Index(
        key = Seq("userId" -> IndexType.Ascending, "provider" -> IndexType.Ascending, "usernameKey" -> IndexType.Ascending),
        name = Some("user_provider_username"),
        unique = true
      )
    )
    accountColl.indexesManager.ensure(
      Index(
        key = Seq("userId" -> IndexType.Ascending, "activityAt" -> IndexType.Descending),
        name = Some("user_activity")
      )
    )
    analysisColl.indexesManager.ensure(
      Index(
        key = Seq("userId" -> IndexType.Ascending, "pgnHash" -> IndexType.Ascending),
        name = Some("user_pgn_hash"),
        unique = true
      )
    )
    analysisColl.indexesManager.ensure(
      Index(
        key = Seq("userId" -> IndexType.Ascending, "provider" -> IndexType.Ascending, "externalGameId" -> IndexType.Ascending),
        name = Some("user_provider_external_game"),
        unique = true,
        options = $doc(
          "partialFilterExpression" -> $doc(
            "provider" -> $doc("$exists" -> true),
            "externalGameId" -> $doc("$exists" -> true)
          )
        )
      )
    )
    analysisColl.indexesManager.ensure(
      Index(
        key = Seq("userId" -> IndexType.Ascending, "lastOpenedAt" -> IndexType.Descending),
        name = Some("user_last_opened")
      )
    )

  lila.common.Bus.sub[lila.core.user.UserDelete]: del =>
    accountColl.delete.one($doc("userId" -> del.id)).void >>
      analysisColl.delete.one($doc("userId" -> del.id)).void

object ImportHistoryApi:
  given BSONDocumentHandler[ImportHistory.Account] = Macros.handler
  given BSONDocumentHandler[ImportHistory.Analysis] = Macros.handler

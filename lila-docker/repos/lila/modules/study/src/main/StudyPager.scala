package lila.study

import scalalib.paginator.Paginator

import lila.core.study.StudyOrder
import lila.core.user.Me
import lila.core.user.Me.*
import lila.db.dsl.{ *, given }
import lila.db.paginator.{ Adapter, CachedAdapter }

final class StudyPager(
    studyRepo: StudyRepo,
    chapterRepo: ChapterRepo
)(using Executor):

  val maxPerPage = MaxPerPage(16)
  val defaultNbChaptersPerStudy = 4

  import BSONHandlers.given
  import studyRepo.{
    selectLiker,
    selectMemberId,
    selectOwnerId,
    selectPrivateOrUnlisted,
    selectPublic,
    selectTopic
  }

  def all(order: StudyOrder, page: Int)(using me: Option[Me]) =
    paginator(
      noRelaySelect ++ accessSelect,
      order,
      page,
      fuccess(9999).some
    )(using me)

  def byOwner(owner: User, order: StudyOrder, page: Int)(using me: Option[Me]) =
    paginator(
      selectOwnerId(owner.id) ++ accessSelect,
      order,
      page
    )(using me)

  def mine(order: StudyOrder, page: Int)(using me: Me) =
    paginator(
      selectOwnerId(me.userId),
      order,
      page
    )(using Some(me))

  def minePublic(order: StudyOrder, page: Int)(using me: Me) =
    paginator(
      selectOwnerId(me) ++ selectPublic,
      order,
      page
    )(using Some(me))

  def minePrivate(order: StudyOrder, page: Int)(using me: Me) =
    paginator(
      selectOwnerId(me) ++ selectPrivateOrUnlisted,
      order,
      page
    )(using Some(me))

  def mineMember(order: StudyOrder, page: Int)(using me: Me) =
    paginator(
      selectMemberId(me) ++ $doc("ownerId".$ne(me.userId)),
      order,
      page
    )(using Some(me))

  def mineLikes(order: StudyOrder, page: Int)(using me: Me) =
    paginator(
      selectLiker(me) ++ accessSelect(using Some(me)) ++ $doc("ownerId".$ne(me.userId)),
      order,
      page
    )(using Some(me))

  def byTopic(topic: StudyTopic, order: StudyOrder, page: Int)(using me: Option[Me]) =
    val onlyMine = me.ifTrue(order == StudyOrder.mine)
    paginator(
      selectTopic(topic) ++ onlyMine.fold(accessSelect)(selectMemberId(_)),
      order,
      page,
      hint = onlyMine.isDefined.option($doc("uids" -> 1, "rank" -> -1))
    )(using me)

  private def accessSelect(using me: Option[Me]) =
    me.fold(selectPublic): u =>
      $or(selectPublic, selectMemberId(u))

  private val noRelaySelect = $doc("from".$ne("relay"))

  private def paginator(
      selector: Bdoc,
      order: StudyOrder,
      page: Int,
      nbResults: Option[Fu[Int]] = none,
      hint: Option[Bdoc] = none
  )(using me: Option[Me]): Fu[Paginator[Study.WithChaptersAndLiked]] = studyRepo.coll: coll =>
    val adapter = Adapter[Study](
      collection = coll,
      selector = selector,
      projection = studyRepo.projection.some,
      sort = order match
        case StudyOrder.hot => $sort.desc("rank")
        case StudyOrder.newest => $sort.desc("createdAt")
        case StudyOrder.oldest => $sort.asc("createdAt")
        case StudyOrder.updated => $sort.desc("updatedAt")
        case StudyOrder.popular => $sort.desc("likes")
        case StudyOrder.alphabetical => $sort.asc("name")
        // mine filter for topic view
        case StudyOrder.mine => $sort.desc("rank")
        // relevant not used here
        case StudyOrder.relevant => $sort.desc("rank")
      ,
      hint = hint
    ).mapFutureList(studies => withChaptersAndLiking()(studies)(using me))
    Paginator(
      adapter = nbResults.fold(adapter): nb =>
        CachedAdapter(adapter, nb),
      currentPage = page,
      maxPerPage = maxPerPage
    )

  def withChaptersAndLiking(
      nbChaptersPerStudy: Int = defaultNbChaptersPerStudy
  )(studies: Seq[Study])(using me: Option[Me]): Fu[Seq[Study.WithChaptersAndLiked]] =
    withChapters(studies, nbChaptersPerStudy).flatMap(withLiking)

  private def withChapters(
      studies: Seq[Study],
      nbChaptersPerStudy: Int
  ): Fu[Seq[Study.WithChapters]] =
    for chapters <- chapterRepo.idNamesByStudyIds(studies.map(_.id), nbChaptersPerStudy)
    yield studies.map: study =>
      Study.WithChapters(study, chapters.get(study.id).so(_.map(_.name)))

  private def withLiking(
      studies: Seq[Study.WithChapters]
  )(using me: Option[Me]): Fu[Seq[Study.WithChaptersAndLiked]] =
    me.so: u =>
      studyRepo.filterLiked(u, studies.map(_.study.id))
    .map: liked =>
        studies.map { case Study.WithChapters(study, chapters) =>
          Study.WithChaptersAndLiked(study, chapters, liked(study.id))
        }

object Orders:
  import lila.core.study.StudyOrder
  val default = StudyOrder.hot
  val list = StudyOrder.all.filter(_ != StudyOrder.relevant)
  val withoutMine = list.filterNot(_ == StudyOrder.mine)
  val withoutSelector = withoutMine.filter(o => o != StudyOrder.oldest && o != StudyOrder.alphabetical)
  val search =
    List(StudyOrder.hot, StudyOrder.newest, StudyOrder.popular, StudyOrder.alphabetical, StudyOrder.relevant)
  private val byKey = list.mapBy(_.toString)
  def apply(key: String): StudyOrder = byKey.getOrElse(key, default)
  val name: StudyOrder => String =
    case StudyOrder.hot => "Hot"
    case StudyOrder.newest => "Newest"
    case StudyOrder.oldest => "Oldest"
    case StudyOrder.updated => "Recently updated"
    case StudyOrder.popular => "Most popular"
    case StudyOrder.alphabetical => "Alphabetical"
    case StudyOrder.mine => "My studies"
    case StudyOrder.relevant => "Relevant"

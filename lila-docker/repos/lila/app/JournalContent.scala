package lila.app

import java.nio.charset.StandardCharsets
import java.nio.file.{ Files, Path }
import java.time.{ LocalDate, ZoneId }
import java.time.format.DateTimeFormatter
import java.util.Locale
import scala.jdk.CollectionConverters.*
import scala.util.Using

import lila.common.config.GetRelativeFile
import lila.core.data.{ Html, Markdown }
import lila.memo.{ MarkdownCache, MarkdownOptions }

final class JournalContent(
    getFile: GetRelativeFile,
    markdownCache: MarkdownCache
):

  import JournalContent.*

  private val logger = lila.log("journal")
  private val contentDir = getFile.exec("content/journal").toPath

  @volatile private var cache = Cached(Nil, Int.MinValue)

  def all: List[Post] =
    val stamp = directoryStamp()
    val snapshot = cache
    if snapshot.stamp == stamp then snapshot.posts
    else synchronized:
      val refreshedStamp = directoryStamp()
      if cache.stamp != refreshedStamp then cache = Cached(loadPosts(), refreshedStamp)
      cache.posts

  def latestPost: Option[Post] = all.headOption

  def bySlug(slug: String): Option[Post] =
    all.find(_.slug == slug)

  private def loadPosts(): List[Post] =
    markdownFiles().flatMap(loadPost).sortBy(_.publishedAt)(Ordering[LocalDate].reverse)

  private def loadPost(path: Path): Option[Post] =
    try
      val raw = Files.readString(path, StandardCharsets.UTF_8).replace("\uFEFF", "")
      val (frontMatter, bodyMarkdown) = parseDocument(raw)
      if frontMatter.boolean("draft").contains(true) then None
      else
        val slug = frontMatter.value("slug").map(slugify).filter(_.nonEmpty).getOrElse(slugify(stripExtension(path.getFileName.toString)))
        val publishedAt = frontMatter
          .value("date")
          .flatMap(parseDate)
          .getOrElse(lastModifiedDate(path))
        val body = bodyMarkdown.trim
        val title = frontMatter.value("title").filter(_.nonEmpty).getOrElse(titleFromSlug(slug))
        val summary = frontMatter.value("summary").filter(_.nonEmpty).getOrElse(deriveSummary(body))
        val readTime = frontMatter.value("read_time").filter(_.nonEmpty).getOrElse(estimateReadTime(body))
        val tags = frontMatter.list("tags").filter(_.nonEmpty).orElse(frontMatter.value("tags").map(parseInlineList)).getOrElse(Nil)

        Some(Post(
          slug = slug,
          kicker = frontMatter.value("kicker").filter(_.nonEmpty).getOrElse("Journal"),
          title = title,
          summary = summary,
          publishedAt = publishedAt,
          publishedLabel = publishedAt.format(DateLabelFormatter),
          readTime = readTime,
          tags = tags,
          body = markdownCache.toHtmlSyncWithoutPgnEmbeds(
            s"journal:${path.getFileName.toString}",
            Markdown(body),
            MarkdownOptions.all
          )
        ))
    catch
      case e: Throwable =>
        logger.warn(s"Failed to load journal post ${path.toAbsolutePath}", e)
        None

  private def directoryStamp(): Int =
    if !Files.isDirectory(contentDir) then Int.MinValue
    else
      markdownFiles()
        .map: path =>
          val modified = Files.getLastModifiedTime(path).toMillis
          s"${path.getFileName}:${modified}"
        .mkString("|")
        .hashCode

  private def markdownFiles(): List[Path] =
    if !Files.isDirectory(contentDir) then Nil
    else
      Using.resource(Files.list(contentDir)) { stream =>
        stream.iterator().asScala
          .filter(path => Files.isRegularFile(path) && path.getFileName.toString.toLowerCase(Locale.ENGLISH).endsWith(".md"))
          .toList
      }

  private def parseDocument(raw: String): (FrontMatter, String) =
    val normalized = raw.replace("\r\n", "\n").replace('\r', '\n')
    val marker = "---\n"
    if !normalized.startsWith(marker) then (FrontMatter.empty, normalized)
    else
      val end = normalized.indexOf(s"\n$marker", marker.length)
      if end < 0 then (FrontMatter.empty, normalized)
      else
        val frontMatterBlock = normalized.slice(marker.length, end)
        val body = normalized.drop(end + marker.length + 1)
        (FrontMatter.parse(frontMatterBlock), body)

  private def parseDate(raw: String): Option[LocalDate] =
    scala.util.Try(LocalDate.parse(raw.trim, DateInputFormatter)).toOption

  private def lastModifiedDate(path: Path): LocalDate =
    LocalDate.ofInstant(Files.getLastModifiedTime(path).toInstant, ZoneId.systemDefault())

  private def deriveSummary(body: String): String =
    body.linesIterator
      .map(_.trim)
      .find(line =>
        line.nonEmpty &&
          !line.startsWith("#") &&
          !line.startsWith("- ") &&
          !line.startsWith("* ") &&
          !line.startsWith(">") &&
          !line.startsWith("```")
      )
      .map(_.replaceAll("""\[(.*?)\]\((.*?)\)""", "$1"))
      .getOrElse("Notes from the Chesstory journal.")

  private def estimateReadTime(body: String): String =
    val wordCount = body.split("\\s+").count(_.nonEmpty)
    val minutes = math.max(1, math.ceil(wordCount / 220d).toInt)
    s"$minutes min read"

  private def parseInlineList(raw: String): List[String] =
    raw.split(",").iterator.map(_.trim).filter(_.nonEmpty).toList

  private def slugify(raw: String): String =
    raw.toLowerCase(Locale.ENGLISH)
      .replaceAll("[^a-z0-9]+", "-")
      .replaceAll("(^-|-$)", "")

  private def stripExtension(fileName: String): String =
    fileName.lastIndexOf('.') match
      case -1 => fileName
      case n  => fileName.take(n)

  private def titleFromSlug(slug: String): String =
    slug
      .split('-')
      .iterator
      .filter(_.nonEmpty)
      .map(word => word.head.toUpper + word.drop(1))
      .mkString(" ")

object JournalContent:

  private val DateInputFormatter = DateTimeFormatter.ISO_LOCAL_DATE
  private val DateLabelFormatter = DateTimeFormatter.ofPattern("MMMM d, uuuu", Locale.ENGLISH)

  case class Post(
      slug: String,
      kicker: String,
      title: String,
      summary: String,
      publishedAt: LocalDate,
      publishedLabel: String,
      readTime: String,
      tags: List[String],
      body: Html
  )

  private case class Cached(posts: List[Post], stamp: Int)

  private case class FrontMatter(values: Map[String, String], lists: Map[String, List[String]]):
    def value(key: String): Option[String] = values.get(key)
    def list(key: String): Option[List[String]] = lists.get(key)
    def boolean(key: String): Option[Boolean] =
      value(key).map(_.trim.toLowerCase(Locale.ENGLISH)).collect:
        case "true"  => true
        case "false" => false

  private object FrontMatter:
    val empty = FrontMatter(Map.empty, Map.empty)

    def parse(raw: String): FrontMatter =
      val lines = raw.linesIterator.toList
      var values = Map.empty[String, String]
      var lists = Map.empty[String, List[String]]
      var index = 0

      while index < lines.length do
        val line = lines(index).trim
        if line.nonEmpty then
          val separator = line.indexOf(':')
          if separator > 0 then
            val key = line.take(separator).trim
            val rest = line.drop(separator + 1).trim
            if rest.nonEmpty then values = values.updated(key, rest)
            else
              val items = scala.collection.mutable.ListBuffer.empty[String]
              var next = index + 1
              while next < lines.length && lines(next).trim.startsWith("- ") do
                items += lines(next).trim.drop(2).trim
                next += 1
              if items.nonEmpty then
                lists = lists.updated(key, items.toList)
                index = next - 1
        index += 1

      FrontMatter(values, lists)

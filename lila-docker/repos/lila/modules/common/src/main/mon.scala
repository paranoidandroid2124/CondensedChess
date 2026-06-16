package lila

import com.github.benmanes.caffeine.cache.Cache as CaffeineCache
import kamon.metric.Timer
import kamon.tag.TagSet
import lila.core.userId.UserId

object mon:

  import kamon.Kamon.{ timer, gauge, counter, histogram }

  private def tags(elems: (String, Any)*): Map[String, Any] = Map.from(elems)

  object syncache:
    def miss(name: String) = counter("syncache.miss").withTag("name", name)
    def timeout(name: String) = counter("syncache.timeout").withTag("name", name)
    def compute(name: String) = timer("syncache.compute").withTag("name", name)
    def wait(name: String) = timer("syncache.wait").withTag("name", name)
  def caffeineStats(cache: CaffeineCache[?, ?], name: String): Unit =
    val stats = cache.stats
    gauge("caffeine.request").withTags(tags("name" -> name, "hit" -> true)).update(stats.hitCount.toDouble)
    gauge("caffeine.request").withTags(tags("name" -> name, "hit" -> false)).update(stats.missCount.toDouble)
    histogram("caffeine.hit.rate").withTag("name", name).record((stats.hitRate * 100000).toLong)
    if stats.totalLoadTime > 0 then
      gauge("caffeine.load.count")
        .withTags(tags("name" -> name, "success" -> "success"))
        .update(stats.loadSuccessCount.toDouble)
      gauge("caffeine.load.count")
        .withTags(tags("name" -> name, "success" -> "failure"))
        .update(stats.loadFailureCount.toDouble)
      gauge("caffeine.loadTime.cumulated")
        .withTag("name", name)
        .update(stats.totalLoadTime / 1000000d) // in millis; too much nanos for Kamon to handle)
      timer("caffeine.loadTime.penalty").withTag("name", name).record(stats.averageLoadPenalty.toLong)
    gauge("caffeine.eviction.count").withTag("name", name).update(stats.evictionCount.toDouble)
    gauge("caffeine.entry.count").withTag("name", name).update(cache.estimatedSize.toDouble)
  object mongoCache:
    def request(name: String, hit: Boolean) =
      counter("mongocache.request").withTags:
        tags(
          "name" -> name,
          "hit" -> hit
        )
    def compute(name: String) = timer("mongocache.compute").withTag("name", name)
  object evalCache:
    private val r = counter("evalCache.request")
    def request(ply: Int, isHit: Boolean) =
      r.withTags(tags("ply" -> (if ply < 15 then ply.toString else "15+"), "hit" -> isHit))
  object asyncActor:
    def overflow(name: String) = counter("asyncActor.overflow").withTag("name", name)
    def queueSize(name: String) = histogram("asyncActor.queueSize").withTag("name", name)
  object email:
    object send:
      val welcome = counter("email.send").withTag("type", "welcome")
      val time = future("email.send.time")
  object security:
    def rateLimit(key: String) = counter("security.rateLimit.count").withTag("key", key)
    def concurrencyLimit(key: String) = counter("security.concurrencyLimit.count").withTag("key", key)
  object study:
    object tree:
      val read = timer("study.tree.read").withoutTags()
      val write = timer("study.tree.write").withoutTags()
  object blocking:
    def time(name: String) = timer("blocking.time").withTag("name", name)
    def timeout(name: String) = counter("blocking.timeout").withTag("name", name)
  object markdown:
    val time = timer("markdown.time").withoutTags()
    def pgnsFromText = future("markdown.pgnsFromText")
  object picfit:
    def uploadTime(user: UserId) = future("picfit.upload.time", tags("user" -> user))
    def uploadSize(user: UserId) = histogram("picfit.upload.size").withTag("user", user)
  object commentary:
    object commentary:
      def repair(path: String, scope: "any" | "material") =
        counter("commentary.commentary.repair").withTags(tags("path" -> path, "scope" -> scope))
      def compare(path: String, consistent: Boolean) =
        counter("commentary.commentary.compare").withTags(tags("path" -> path, "consistent" -> consistent))
      def thesisAgreement(path: String, agreed: Boolean) =
        counter("commentary.commentary.thesisAgreement").withTags(tags("path" -> path, "agreed" -> agreed))
      def sample(kind: String) =
        counter("commentary.commentary.sample").withTag("kind", kind)
      def metric(name: String, path: String) =
        gauge("commentary.commentary.metric").withTags(tags("name" -> name, "path" -> path))

  type TimerPath = lila.mon.type => Timer

  private def future(name: String) = (success: Boolean) => timer(name).withTag("success", successTag(success))
  private def future(name: String, tags: Map[String, Any]) = (success: Boolean) =>
    timer(name).withTags(tags + ("success" -> successTag(success)))

  private def successTag(success: Boolean) = if success then "success" else "failure"

  import scala.language.implicitConversions
  private given Conversion[UserId, String] = _.value
  private given Conversion[Map[String, Any], TagSet] = TagSet.from

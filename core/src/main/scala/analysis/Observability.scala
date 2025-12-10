package chess
package analysis

import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicInteger

/**
 * Central metrics collector for the application.
 * Used to power the /status internal dashboard.
 */
object Observability:
  // Analysis Metrics
  val activeJobs = new AtomicInteger(0)
  val completedJobs = new AtomicLong(0)
  val failedJobs = new AtomicLong(0)

  // Cache Metrics
  val cacheHits = new AtomicLong(0)
  val cacheMisses = new AtomicLong(0)
  val diskWrites = new AtomicLong(0)
  
  // Engine Pool Metrics (Dynamically fetched from EnginePool if possible, or tracked here)
  // For now, simple counters
  
  def statusJson: String =
    s"""
    {
      "jobs": {
        "active": ${activeJobs.get()},
        "completed": ${completedJobs.get()},
        "failed": ${failedJobs.get()}
      },
      "cache": {
        "hits": ${cacheHits.get()},
        "misses": ${cacheMisses.get()},
        "disk_writes": ${diskWrites.get()},
        "hit_ratio": ${
           val h = cacheHits.get().toDouble
           val total = h + cacheMisses.get()
           if total == 0 then 0.0 else (h / total * 100).toInt / 100.0
        }
      },
      "pool": {
         "max": ${sys.env.getOrElse("MAX_ENGINES", "4")}
      }
    }
    """

package chess
package analysis

import scala.concurrent.{Future, ExecutionContext}
import scala.collection.concurrent.TrieMap
import chess.analysis.AnalysisTypes._
import chess.analysis.MoveGenerator
import chess.format.Fen


import chess.analysis.AnalysisModel.EngineEval

class ExperimentRunner(
    engineService: EngineService
)(using ec: ExecutionContext):

  // Cache: Key -> Future[ExperimentResult] to handle Thundering Herd
  // Key: Hash String
  private val memCache = TrieMap.empty[String, Future[ExperimentResult]]

  private def computeHash(key: (ExperimentType, String, Option[String])): String =
    val (etype, fen, move) = key
    val raw = s"${etype.toString}|$fen|${move.getOrElse("")}"
    java.security.MessageDigest.getInstance("MD5")
      .digest(raw.getBytes)
      .map("%02x".format(_))
      .mkString

  /**
   * Run a specific experiment with caching.
   */
  def run(
      expType: ExperimentType,
      fen: String,
      move: Option[String],
      depth: Int = 10,
      multiPv: Int = 1,
      forcedMoves: List[String] = Nil 
  ): Future[ExperimentResult] =
    val fullKey = (expType, fen, move)
    val hash = computeHash(fullKey)
    
    // 1. Check Memory Cache (In-flight or Hot)
    memCache.get(hash) match
      case Some(future) => future
      case None =>
        // 2. Check Disk Cache (Persistent)
        // We do this sync for simplicity, or future?
        // Let's do it inside the Future to avoid blocking calling thread
        val promise = scala.concurrent.Promise[ExperimentResult]()
        memCache.put(hash, promise.future)
        
        Future {
           Persistence.getCachedExperiment(hash)
        }.flatMap {
           case Some(json) =>
             deserializeEval(json) match
               case Some(eval) =>
                 Future.successful(ExperimentResult(
                   expType = expType,
                   fen = fen,
                   move = move,
                   eval = eval,
                   metadata = Map("cache" -> "disk")
                 ))
               case None => 
                 // Parse failed, treat as miss
                 Future.failed(new RuntimeException("Cache Parse Failed"))
           case None => Future.failed(new RuntimeException("Cache Miss"))
        }.recoverWith { case _ =>
            // 3. Compute
            val request = Analyze(
                fen = fen, 
                moves = forcedMoves,
                depth = depth, 
                multiPv = multiPv,
                timeoutMs = depth * 150 
            )
            engineService.submit(request).map { res =>
                ExperimentResult(
                    expType = expType,
                    fen = fen,
                    move = move,
                    eval = res.eval
                )
            }
        }.map { result =>
             // 4. Save to Disk (Async)
             if !result.metadata.contains("cache") then
               val json = serializeEval(result.eval)
               Persistence.saveCachedExperiment(hash, fen, depth, json)
             result
        }.onComplete { r =>
             promise.complete(r)
        }
        
        promise.future

  // Tiny JSON serializer for EngineEval
  private def serializeEval(e: EngineEval): String =
    val lines = e.lines.map { l =>
       s"""{"move":"${l.move}","winPct":${l.winPct},"cp":${l.cp.getOrElse("null")},"mate":${l.mate.getOrElse("null")},"pv":[${l.pv.map(s => s""""$s"""").mkString(",")}]}"""
    }.mkString(",")
    s"""{"depth":${e.depth},"lines":[$lines]}"""

  // Tiny JSON deserializer for EngineEval
  private def deserializeEval(json: String): Option[EngineEval] =
    try
      val depthRegex = """"depth":(\d+)""".r
      val depth = depthRegex.findFirstMatchIn(json).map(_.group(1).toInt).getOrElse(0)
      
      // Extract lines array content: "lines":[ ... ]
      val linesContentStart = json.indexOf("\"lines\":[")
      if linesContentStart == -1 then return None
      
      val content = json.substring(linesContentStart + 9, json.lastIndexOf("]"))
      if content.trim.isEmpty then return Some(EngineEval(depth, Nil))
      
      // Split by object close/start "},"
      // Simple split might break if nested objects exist, but Line structure is flat except PV array
      // PV array is [ ... ], so we must be careful.
      // Actually we can iterate. 
      // Or use a simpler approach: Extract all object blocks via Regex?
      // Regex for objects: \{.*?\} might be greedy.
      // Let's assume standard formatting from serializeEval: no nested objects inside Line except pv list.
      
      val lineObjs = content.split("\\},\\{").map(_.stripPrefix("{").stripSuffix("}"))
      
      val lines = lineObjs.map { obj =>
        val move = """"move":"([^"]+)"""".r.findFirstMatchIn(obj).map(_.group(1)).getOrElse("")
        val winPct = """"winPct":([\d\.]+)""".r.findFirstMatchIn(obj).map(_.group(1).toDouble).getOrElse(50.0)
        val cp = """"cp":(-?\d+)""".r.findFirstMatchIn(obj).map(_.group(1).toInt)
        val mate = """"mate":(-?\d+)""".r.findFirstMatchIn(obj).map(_.group(1).toInt)
        
        // PV is tricky: "pv":["a1","a2"]
        val pvStr = """"pv":\[(.*?)\]""".r.findFirstMatchIn(obj).map(_.group(1)).getOrElse("")
        val pv = pvStr.split(",").map(_.stripPrefix("\"").stripSuffix("\"")).toList.filter(_.nonEmpty)
        
        AnalysisModel.EngineLine(move, winPct, cp, mate, pv)
      }.toList
      
      Some(EngineEval(depth, lines))
    catch
      case _: Throwable => None

  /**
   * Validate if a potential tactical move is sound.
   */
  def verifyTactics(fen: String, move: String): Future[ExperimentResult] =
    run(
      expType = ExperimentType.TacticalCheck,
      fen = fen,
      move = Some(move),
      forcedMoves = List(move),
      depth = 12,
      multiPv = 1
    )

  /**
   * Check opening stats or shallow eval.
   */
  def checkOpening(fen: String): Future[ExperimentResult] =
    run(
      expType = ExperimentType.OpeningStats,
      fen = fen,
      move = None,
      depth = 8,
      multiPv = 3
    )
    
  /**
   * Detect turning points by deeper check.
   */
  def detectTurningPoint(fen: String): Future[ExperimentResult] =
    run(
      expType = ExperimentType.TurningPointVerification,
      fen = fen,
      move = None,
      depth = 14,
      multiPv = 1
    )

  /**
   * Main entry point for hypothesis generation and testing (Phase 5).
   * Generates candidate moves using heuristics, then runs experiments in parallel.
   */
  def findHypotheses(
      position: chess.Position,
      features: FeatureExtractor.PositionFeatures,
      maxExperiments: Int = 5
  ): Future[List[ExperimentResult]] =
    // 1. Generate Candidates using Heuristics (The "Coach"'s Intuition)
    val allCandidates = MoveGenerator.generateCandidates(position, features)
    
    // 2. Select Top Candidates (Prioritization)
    val selected = allCandidates
      .distinctBy(_.uci)
      .sortBy(-_.priority)
      .take(maxExperiments)
      
    // 3. Run Experiments (Verification)
    Future.sequence(
      selected.map { candidate =>
        val expType = candidate.candidateType match
          case MoveGenerator.CandidateType.TacticalCheck => ExperimentType.TacticalCheck
          case _ => ExperimentType.StructureAnalysis 
          
        run(
           expType = expType,
           fen = Fen.write(position, chess.FullMoveNumber(1)).value, // Use clean FEN for cache consistency
           move = Some(candidate.uci),
           depth = 10,
           multiPv = 1,
           forcedMoves = List(candidate.uci)
        ).map(res => res.copy(metadata = res.metadata + ("candidateType" -> candidate.candidateType.toString)))
      }
    )

  def clearCache(): Unit =
    memCache.clear()


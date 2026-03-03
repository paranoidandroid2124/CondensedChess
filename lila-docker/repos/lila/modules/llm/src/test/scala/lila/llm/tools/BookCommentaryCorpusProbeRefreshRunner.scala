package lila.llm.tools

import java.nio.charset.StandardCharsets
import java.nio.file.{ Files, Path, Paths }

import scala.sys.process.*
import scala.util.control.NonFatal

import play.api.libs.json.*

import lila.llm.analysis.{
  CommentaryEngine,
  NarrativeContextBuilder,
  NarrativeUtils
}
import lila.llm.model.*
import lila.llm.model.strategic.VariationLine

/**
 * Refreshes BookCommentaryCorpus probeResults in two passes.
 *
 * Pass 1: build probeRequests from current server pipeline (without probeResults).
 * Pass 2: run stockfish.wasm helper and synthesize ProbeResult payloads per request.
 *
 * Usage (from `repos/lila`):
 *   sbt "llm/Test/runMain lila.llm.tools.BookCommentaryCorpusProbeRefreshRunner"
 *   sbt "llm/Test/runMain lila.llm.tools.BookCommentaryCorpusProbeRefreshRunner modules/llm/docs/BookCommentaryCorpus.json modules/llm/docs/BookCommentaryCorpus_refreshed.json"
 */
object BookCommentaryCorpusProbeRefreshRunner:

  private val JsonStartMarker = "---JSON_START---"
  private val JsonEndMarker = "---JSON_END---"
  private val DefaultDepth = 20
  private val DefaultMultiPv = 3

  final case class CorpusCaseInput(
      id: String,
      analysisFen: Option[String],
      startFen: Option[String],
      preMovesUci: Option[List[String]],
      ply: Int,
      playedMove: String,
      opening: Option[String],
      phase: String,
      variations: List[VariationLine]
  )
  object CorpusCaseInput:
    given Reads[CorpusCaseInput] = Json.reads[CorpusCaseInput]

  private final case class CaseWork(
      id: String,
      caseObj: JsObject,
      fen: String,
      requests: List[ProbeRequest]
  )

  def main(args: Array[String]): Unit =
    val positional = positionalArgs(args.toList)
    val inPath = positional.headOption.getOrElse("modules/llm/docs/BookCommentaryCorpus.json")
    val outPath =
      positional
        .lift(1)
        .getOrElse("modules/llm/docs/BookCommentaryCorpus_refreshed.json")
    val depth = optionInt(args.toList, "--depth").getOrElse(DefaultDepth).max(8)
    val multiPv = optionInt(args.toList, "--multi-pv").getOrElse(DefaultMultiPv).max(1)

    val inputPath = Paths.get(inPath)
    val outputPath = Paths.get(outPath)
    val root =
      try Json.parse(Files.readString(inputPath, StandardCharsets.UTF_8)).as[JsObject]
      catch
        case NonFatal(e) =>
          System.err.println(s"[probe-refresh] failed to parse input json `$inPath`: ${e.getMessage}")
          sys.exit(1)

    val caseObjs = (root \ "cases").asOpt[List[JsObject]].getOrElse(Nil)
    if caseObjs.isEmpty then
      System.err.println(s"[probe-refresh] no `cases` found in `$inPath`")
      sys.exit(1)

    val work = caseObjs.flatMap(buildCaseWork)
    println(s"[probe-refresh] pass1 complete: cases_with_fen=${work.size}/${caseObjs.size}")

    val requestsByCase = work.map(w => w.id -> w.requests).toMap
    val allRequests = work.flatMap(_.requests)
    if allRequests.isEmpty then
      System.err.println("[probe-refresh] no probeRequests produced in pass1; aborting")
      sys.exit(1)

    val movePoolByFen =
      allRequests
        .groupBy(_.fen)
        .view
        .mapValues(_.flatMap(_.moves).map(_.trim.toLowerCase).filter(_.nonEmpty).distinct)
        .toMap
    val fenPool = allRequests.map(_.fen).distinct
    val maxDepth = allRequests.flatMap(req => Option(req.depth).map(_.max(1))).maxOption.getOrElse(depth).max(depth)
    val maxMultiPv = allRequests.flatMap(_.multiPv).maxOption.getOrElse(multiPv).max(multiPv)

    val variationsByFen =
      runStockfishBatch(
        fens = fenPool,
        playedMovesByFen = movePoolByFen,
        depth = maxDepth,
        multiPv = maxMultiPv
      )
    println(
      s"[probe-refresh] pass2 engine batch complete: fen=${variationsByFen.size}/${fenPool.size}, depth=$maxDepth, multiPv=$maxMultiPv"
    )

    val baselineByFen =
      variationsByFen.view.mapValues(_.headOption.map(_.scoreCp).getOrElse(0)).toMap

    val refreshedCases =
      work.map { w =>
        val probeResults =
          synthesizeProbeResults(
            requests = requestsByCase.getOrElse(w.id, Nil),
            variationsByFen = variationsByFen,
            baselineByFen = baselineByFen
          )
        w.id -> (w.caseObj + ("probeResults" -> Json.toJson(probeResults)))
      }.toMap

    val updatedCaseObjs =
      caseObjs.map { js =>
        val id = (js \ "id").asOpt[String].getOrElse("")
        refreshedCases.getOrElse(id, js + ("probeResults" -> Json.arr()))
      }
    val outRoot = root + ("cases" -> JsArray(updatedCaseObjs))
    Files.writeString(outputPath, Json.prettyPrint(outRoot), StandardCharsets.UTF_8)

    val totalProbeResults = refreshedCases.values.map(js => (js \ "probeResults").asOpt[List[JsValue]].getOrElse(Nil).size).sum
    println(
      s"[probe-refresh] wrote `$outPath` (cases=${updatedCaseObjs.size}, probeResults=$totalProbeResults)"
    )

  private def buildCaseWork(caseObj: JsObject): Option[CaseWork] =
    caseObj.validate[CorpusCaseInput].asOpt.flatMap { c =>
      resolveFen(c).flatMap { fen =>
        val requests =
          buildProbeRequests(
            fen = fen,
            c = c
          )
        Option.when(requests.nonEmpty)(CaseWork(c.id, caseObj, fen, requests))
      }
    }

  private def resolveFen(c: CorpusCaseInput): Option[String] =
    c.analysisFen.orElse {
      for
        s <- c.startFen
        pre <- c.preMovesUci
      yield NarrativeUtils.uciListToFen(s, pre)
    }

  private def buildProbeRequests(
      fen: String,
      c: CorpusCaseInput
  ): List[ProbeRequest] =
    if c.variations.isEmpty then Nil
    else
      CommentaryEngine
        .assessExtended(
          fen = fen,
          variations = c.variations,
          playedMove = Some(c.playedMove),
          opening = c.opening,
          phase = Some(c.phase),
          ply = c.ply,
          prevMove = Some(c.playedMove),
          probeResults = Nil
        )
        .toList
        .flatMap { data =>
          val ctx =
            NarrativeContextBuilder.build(
              data = data,
              ctx = data.toContext,
              prevAnalysis = None,
              probeResults = Nil,
              openingRef = None,
              prevOpeningRef = None,
              openingBudget = OpeningEventBudget(),
              afterAnalysis = None
            )
          ctx.probeRequests
        }

  private def runStockfishBatch(
      fens: List[String],
      playedMovesByFen: Map[String, List[String]],
      depth: Int,
      multiPv: Int
  ): Map[String, List[VariationLine]] =
    val acc = scala.collection.mutable.Map.empty[String, List[VariationLine]]
    val chunkSize = 8
    fens.distinct.grouped(chunkSize).zipWithIndex.foreach { case (chunk, idx) =>
      runStockfishChunk(
        fens = chunk,
        playedMovesByFen = playedMovesByFen.view.filterKeys(chunk.toSet).toMap,
        depth = depth,
        multiPv = multiPv
      ) match
        case Right(found) =>
          acc ++= found
          println(s"[probe-refresh] stockfish chunk ${idx + 1}: ok (${found.size}/${chunk.size})")
        case Left(err) =>
          println(s"[probe-refresh] stockfish chunk ${idx + 1}: failed, retry per-fen (${err.take(220)})")
          chunk.foreach { fen =>
            val perFenMoves = playedMovesByFen.get(fen).toList
            val firstTry =
              runStockfishChunk(
                fens = List(fen),
                playedMovesByFen = Map(fen -> perFenMoves.flatten),
                depth = depth,
                multiPv = multiPv
              )
            val recovered = firstTry.orElse {
              runStockfishChunk(
                fens = List(fen),
                playedMovesByFen = Map(fen -> perFenMoves.flatten),
                depth = depth.min(14),
                multiPv = multiPv.min(2)
              )
            }
            recovered match
              case Right(found) if found.nonEmpty =>
                acc ++= found
              case _ =>
                println(s"[probe-refresh] stockfish fen failed, leaving empty: ${fen.take(48)}")
          }
    }
    acc.toMap

  private def runStockfishChunk(
      fens: List[String],
      playedMovesByFen: Map[String, List[String]],
      depth: Int,
      multiPv: Int
  ): Either[String, Map[String, List[VariationLine]]] =
    val scriptPath = Path.of("run_stockfish_wasm.js")
    if !Files.exists(scriptPath) then
      return Left(s"missing helper script: $scriptPath")

    val payload = Json.obj(
      "fens" -> fens,
      "playedMovesByFen" -> playedMovesByFen,
      "depth" -> depth,
      "multiPv" -> multiPv
    )
    val inputPath = Files.createTempFile("book-corpus-probe-refresh-", ".json")
    val stdout = new StringBuilder()
    val stderr = new StringBuilder()
    try
      Files.writeString(inputPath, Json.stringify(payload), StandardCharsets.UTF_8)
      val command = Seq("node", "run_stockfish_wasm.js", "--input", inputPath.toString)
      val exit =
        Process(command, Path.of(".").toFile).!(
          ProcessLogger(
            out => stdout.append(out).append('\n'),
            err => stderr.append(err).append('\n')
          )
        )
      if exit != 0 then
        Left(
          s"stockfish helper failed (exit=$exit): ${stderr.toString.take(1600)}"
        )
      else
        val jsonPayload = extractJsonPayload(stdout.toString)
        val root = Json.parse(jsonPayload).as[JsObject]
        Right(
          root.fields.map { (fen, js) =>
            fen -> parseVariationList(js)
          }.toMap
        )
    catch
      case NonFatal(e) =>
        Left(e.getMessage)
    finally
      Files.deleteIfExists(inputPath)

  private def synthesizeProbeResults(
      requests: List[ProbeRequest],
      variationsByFen: Map[String, List[VariationLine]],
      baselineByFen: Map[String, Int]
  ): List[ProbeResult] =
    requests.flatMap { req =>
      val fenLines = variationsByFen.getOrElse(req.fen, Nil)
      val baseCp = req.baselineEvalCp.getOrElse(baselineByFen.getOrElse(req.fen, 0))
      if req.moves.nonEmpty then
        req.moves.flatMap { move =>
          fenLines.find(_.moves.headOption.exists(NarrativeUtils.uciEquivalent(_, move))).map { line =>
            val pv = normalizeMoveProbePv(line.moves, move)
            mkProbeResult(
              req = req,
              evalCp = line.scoreCp,
              mate = line.mate,
              depth = line.depth,
              replyPvs = List(pv),
              baseCp = baseCp,
              probedMove = Some(move)
            )
          }
        }
      else
        fenLines.headOption.map { head =>
          val pvs = fenLines.take(4).map(_.moves).filter(_.nonEmpty)
          val normalized = if pvs.nonEmpty then pvs else List(head.moves)
          mkProbeResult(
            req = req,
            evalCp = head.scoreCp,
            mate = head.mate,
            depth = head.depth,
            replyPvs = normalized,
            baseCp = baseCp,
            probedMove = None
          )
        }.toList
    }

  private def mkProbeResult(
      req: ProbeRequest,
      evalCp: Int,
      mate: Option[Int],
      depth: Int,
      replyPvs: List[List[String]],
      baseCp: Int,
      probedMove: Option[String]
  ): ProbeResult =
    val normalizedPvs = replyPvs.map(_.map(_.trim.toLowerCase).filter(_.nonEmpty)).filter(_.nonEmpty)
    val best = normalizedPvs.headOption.getOrElse(Nil)
    val delta = evalCp - baseCp
    val motifs = inferKeyMotifs(req.purpose, probedMove, delta)
    val required = req.requiredSignals.toSet
    val l1Delta =
      if required.contains("l1Delta") || req.purpose.exists(_.contains("theme_plan_validation")) then
        Some(buildL1Delta(delta, motifs))
      else None
    val future =
      if required.contains("futureSnapshot") || req.purpose.exists(p =>
          p.contains("latent") || p.contains("theme_plan_validation")
        ) then
        Some(buildFutureSnapshot(delta))
      else None
    ProbeResult(
      id = req.id,
      fen = Some(req.fen),
      evalCp = evalCp,
      bestReplyPv = best,
      replyPvs = Option.when(normalizedPvs.nonEmpty)(normalizedPvs),
      deltaVsBaseline = delta,
      keyMotifs = motifs,
      purpose = req.purpose,
      questionId = req.questionId,
      questionKind = req.questionKind,
      probedMove = probedMove,
      mate = mate,
      depth = Option.when(depth > 0)(depth),
      l1Delta = l1Delta,
      futureSnapshot = future,
      objective = req.objective,
      seedId = req.seedId
    )

  private def normalizeMoveProbePv(pv: List[String], move: String): List[String] =
    val normalized = pv.map(_.trim.toLowerCase).filter(_.nonEmpty)
    normalized match
      case head :: tail if NarrativeUtils.uciEquivalent(head, move) =>
        if tail.nonEmpty then tail else List(head)
      case xs if xs.nonEmpty => xs
      case _                 => List(move.trim.toLowerCase)

  private def inferKeyMotifs(
      purpose: Option[String],
      probedMove: Option[String],
      deltaVsBaseline: Int
  ): List[String] =
    val base =
      purpose.map(_.trim).filter(_.nonEmpty) match
        case Some("theme_plan_validation") => List("plan_validation")
        case Some("NullMoveThreat")        => List("prophylaxis_check")
        case Some("played_move_counterfactual") => List("counterfactual_probe")
        case Some("recapture_branches")    => List("recapture_branch")
        case Some("keep_tension_branches") => List("tension_branch")
        case Some("reply_multipv") | Some("defense_reply_multipv") => List("reply_multipv")
        case Some("convert_reply_multipv") => List("conversion_probe")
        case Some("latent_plan_refutation") => List("refutation_probe")
        case Some("latent_plan_immediate") => List("immediate_plan_probe")
        case Some("free_tempo_branches")   => List("free_tempo_probe")
        case _                             => List("engine_probe")
    val moveTag = probedMove.map(m => s"move:${m.take(5)}")
    val swingTag =
      if deltaVsBaseline >= 80 then Some("eval_swing_positive")
      else if deltaVsBaseline <= -80 then Some("eval_swing_negative")
      else None
    (base ++ moveTag ++ swingTag).distinct

  private def buildL1Delta(deltaVsBaseline: Int, motifs: List[String]): L1DeltaSnapshot =
    val collapse =
      if deltaVsBaseline <= -120 then Some("probe indicates structural or tactical concession")
      else if deltaVsBaseline >= 120 then Some("probe preserves initiative without structural loss")
      else motifs.headOption.map(m => s"probe motif: $m")
    L1DeltaSnapshot(
      materialDelta = 0,
      kingSafetyDelta = if deltaVsBaseline < 0 then -1 else if deltaVsBaseline > 0 then 1 else 0,
      centerControlDelta = 0,
      openFilesDelta = 0,
      mobilityDelta = if deltaVsBaseline < 0 then -1 else if deltaVsBaseline > 0 then 1 else 0,
      collapseReason = collapse
    )

  private def buildFutureSnapshot(deltaVsBaseline: Int): FutureSnapshot =
    FutureSnapshot(
      resolvedThreatKinds = if deltaVsBaseline >= 50 then List("Material", "Initiative") else Nil,
      newThreatKinds = if deltaVsBaseline <= -50 then List("Tactical") else Nil,
      targetsDelta = TargetsDelta(
        tacticalAdded = Nil,
        tacticalRemoved = Nil,
        strategicAdded = if deltaVsBaseline >= 0 then List("plan_anchor") else Nil,
        strategicRemoved = if deltaVsBaseline < 0 then List("plan_anchor") else Nil
      ),
      planBlockersRemoved = if deltaVsBaseline >= 20 then List("reply_pressure") else Nil,
      planPrereqsMet = if deltaVsBaseline >= -20 then List("viability_stable") else Nil
    )

  private def extractJsonPayload(output: String): String =
    val start = output.indexOf(JsonStartMarker)
    val end = output.indexOf(JsonEndMarker)
    if start < 0 || end < 0 || end <= start then
      throw new RuntimeException(
        s"cannot parse stockfish JSON payload from output:\n${output.take(2000)}"
      )
    output.substring(start + JsonStartMarker.length, end).trim

  private def parseVariationList(js: JsValue): List[VariationLine] =
    js.asOpt[List[JsValue]].getOrElse(Nil).flatMap { row =>
      val moves = (row \ "moves").asOpt[List[String]].getOrElse(Nil).map(_.trim.toLowerCase).filter(_.nonEmpty)
      Option.when(moves.nonEmpty) {
        VariationLine(
          moves = moves,
          scoreCp = (row \ "scoreCp").asOpt[Int].getOrElse(0),
          mate = (row \ "mate").asOpt[Int],
          depth = (row \ "depth").asOpt[Int].getOrElse(0)
        )
      }
    }

  private def optionInt(args: List[String], flag: String): Option[Int] =
    args
      .sliding(2)
      .collectFirst {
        case List(k, v) if k == flag => v.toIntOption
      }
      .flatten

  private def positionalArgs(args: List[String]): List[String] =
    val takesValue = Set("--depth", "--multi-pv")
    val out = scala.collection.mutable.ListBuffer.empty[String]
    var i = 0
    while i < args.size do
      val token = args(i)
      if takesValue.contains(token) then i = i + 2
      else if token.startsWith("--") then i = i + 1
      else
        out += token
        i = i + 1
    out.toList

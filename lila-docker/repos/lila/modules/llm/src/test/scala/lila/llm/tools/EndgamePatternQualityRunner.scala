package lila.llm.tools

import java.nio.charset.StandardCharsets
import java.nio.file.{ Files, Path, Paths }

import chess.Color
import play.api.libs.json.*
import lila.llm.analysis.strategic.{ EndgameAnalyzerImpl, EndgamePatternOracle }

object EndgamePatternQualityRunner:

  private final case class V1Expect(
      oppositionType: Option[String],
      ruleOfSquare: Option[String],
      triangulationAvailable: Option[Boolean],
      rookEndgamePattern: Option[String],
      zugzwangLikely: Option[Boolean]
  )
  private object V1Expect:
    given Reads[V1Expect] = Json.reads[V1Expect]

  private final case class V1Row(
      fen: String,
      color: String,
      expect: V1Expect
  )
  private object V1Row:
    given Reads[V1Row] = Json.reads[V1Row]

  private final case class V2Case(
      id: String,
      kind: String,
      fen: String,
      color: String,
      expectedLabel: String,
      signalsOverride: Option[JsObject]
  )
  private object V2Case:
    given Reads[V2Case] = Reads { js =>
      for
        id <- (js \ "id").validate[String]
        kind <- (js \ "kind").validate[String]
        fen <- (js \ "fen").validate[String]
        color <- (js \ "color").validate[String]
        expectedLabel <- (js \ "expectedLabel").validate[String]
        signalsOverride <- (js \ "signalsOverride").validateOpt[JsObject]
      yield V2Case(id, kind, fen, color, expectedLabel, signalsOverride)
    }

  private final case class V2PatternRow(
      pattern: String,
      defaultSignals: JsObject,
      cases: List[V2Case]
  )
  private object V2PatternRow:
    given Reads[V2PatternRow] = Reads { js =>
      for
        pattern <- (js \ "pattern").validate[String]
        defaultSignals <- (js \ "defaultSignals").validate[JsObject]
        cases <- (js \ "cases").validate[List[V2Case]]
      yield V2PatternRow(pattern, defaultSignals, cases)
    }

  private final case class Config(
      v1Input: Path,
      v2Input: Path,
      out: Path,
      strict: Boolean,
      gateV1F1: Double,
      gateV2Precision: Double,
      gateV2F1: Double,
      gateSignalMatch: Double,
      gateLatencyP95Ms: Double
  )

  private final case class V1Metrics(
      rows: Int,
      checks: Int,
      matched: Int,
      f1: Double,
      latencyP95Ms: Double
  )

  private final case class PatternMetrics(
      tp: Int,
      fp: Int,
      fn: Int,
      precision: Double,
      recall: Double,
      f1: Double
  )

  private final case class V2Metrics(
      totalCases: Int,
      accuracy: Double,
      precision: Double,
      recall: Double,
      f1: Double,
      macroF1: Double,
      signalChecks: Int,
      signalMatches: Int,
      signalMatchRate: Double,
      perPattern: Map[String, PatternMetrics],
      latencyP95Ms: Double
  )

  private final case class GateVerdict(
      passV1: Boolean,
      passV2Precision: Boolean,
      passV2F1: Boolean,
      passSignalMatch: Boolean,
      passLatency: Boolean
  ):
    def overall: Boolean = passV1 && passV2Precision && passV2F1 && passSignalMatch && passLatency

  private val defaults = Config(
    v1Input = Paths.get("modules/llm/src/test/resources/endgame_goldset_v1.jsonl"),
    v2Input = Paths.get("modules/llm/src/test/resources/endgame_goldset_v2_patterns.jsonl"),
    out = Paths.get("modules/llm/docs/EndgamePatternQualityReport.md"),
    strict = false,
    gateV1F1 = 0.85,
    gateV2Precision = 0.88,
    gateV2F1 = 0.88,
    gateSignalMatch = 0.55,
    gateLatencyP95Ms = 40.0
  )

  def main(args: Array[String]): Unit =
    parseArgs(args.toList) match
      case Left(err) =>
        System.err.println(s"[endgame-quality] $err")
        sys.exit(2)
      case Right(cfg) =>
        val analyzer = new EndgameAnalyzerImpl()
        (evaluateV1(cfg.v1Input, analyzer), evaluateV2(cfg.v2Input, analyzer)) match
          case (Right(v1), Right(v2)) =>
            val combinedLatencyP95 = math.max(v1.latencyP95Ms, v2.latencyP95Ms)
            val gate = GateVerdict(
              passV1 = v1.f1 >= cfg.gateV1F1,
              passV2Precision = v2.precision >= cfg.gateV2Precision,
              passV2F1 = v2.f1 >= cfg.gateV2F1,
              passSignalMatch = v2.signalMatchRate >= cfg.gateSignalMatch,
              passLatency = combinedLatencyP95 <= cfg.gateLatencyP95Ms
            )
            writeReport(cfg.out, renderReport(cfg, v1, v2, combinedLatencyP95, gate))
            if gate.overall then
              println(
                f"[endgame-quality] ✅ pass=true v1F1=${v1.f1}%.4f v2Precision=${v2.precision}%.4f v2F1=${v2.f1}%.4f signalMatch=${v2.signalMatchRate}%.4f latencyP95Ms=$combinedLatencyP95%.3f"
              )
            else
              System.err.println(
                f"[endgame-quality] ❌ pass=false v1F1=${v1.f1}%.4f v2Precision=${v2.precision}%.4f v2F1=${v2.f1}%.4f signalMatch=${v2.signalMatchRate}%.4f latencyP95Ms=$combinedLatencyP95%.3f"
              )
              if cfg.strict then sys.exit(1)
          case (Left(err), _) =>
            System.err.println(s"[endgame-quality] $err")
            sys.exit(2)
          case (_, Left(err)) =>
            System.err.println(s"[endgame-quality] $err")
            sys.exit(2)

  private def parseArgs(args: List[String]): Either[String, Config] =
    args.foldLeft[Either[String, Config]](Right(defaults)) {
      case (Left(err), _) => Left(err)
      case (Right(cfg), arg) if arg == "--strict" => Right(cfg.copy(strict = true))
      case (Right(cfg), arg) if arg.startsWith("--v1-input=") =>
        Right(cfg.copy(v1Input = Paths.get(arg.stripPrefix("--v1-input="))))
      case (Right(cfg), arg) if arg.startsWith("--v2-input=") =>
        Right(cfg.copy(v2Input = Paths.get(arg.stripPrefix("--v2-input="))))
      case (Right(cfg), arg) if arg.startsWith("--out=") =>
        Right(cfg.copy(out = Paths.get(arg.stripPrefix("--out="))))
      case (Right(cfg), arg) if arg.startsWith("--gate-v1-f1=") =>
        parseGate(arg, "--gate-v1-f1=").map(v => cfg.copy(gateV1F1 = v))
      case (Right(cfg), arg) if arg.startsWith("--gate-v2-precision=") =>
        parseGate(arg, "--gate-v2-precision=").map(v => cfg.copy(gateV2Precision = v))
      case (Right(cfg), arg) if arg.startsWith("--gate-v2-f1=") =>
        parseGate(arg, "--gate-v2-f1=").map(v => cfg.copy(gateV2F1 = v))
      case (Right(cfg), arg) if arg.startsWith("--gate-signal-match=") =>
        parseGate(arg, "--gate-signal-match=").map(v => cfg.copy(gateSignalMatch = v))
      case (Right(cfg), arg) if arg.startsWith("--gate-latency-p95-ms=") =>
        arg.stripPrefix("--gate-latency-p95-ms=").toDoubleOption
          .filter(_ >= 0.0)
          .toRight(s"invalid --gate-latency-p95-ms: $arg")
          .map(v => cfg.copy(gateLatencyP95Ms = v))
      case (Right(_), arg) =>
        Left(s"unknown argument: $arg")
    }

  private def parseGate(arg: String, prefix: String): Either[String, Double] =
    arg.stripPrefix(prefix).toDoubleOption
      .filter(v => v >= 0.0 && v <= 1.0)
      .toRight(s"invalid $prefix$arg")

  private def evaluateV1(path: Path, analyzer: EndgameAnalyzerImpl): Either[String, V1Metrics] =
    readLines(path).flatMap { lines =>
      val rows = lines.zipWithIndex.map { case (line, idx) =>
        Json.parse(line).validate[V1Row].asEither.left.map(err => s"invalid v1 row at line ${idx + 1}: $err")
      }
      val rowsEither = rows.foldLeft[Either[String, List[V1Row]]](Right(Nil)) {
        case (Left(err), _) => Left(err)
        case (Right(_), Left(err)) => Left(err)
        case (Right(acc), Right(row)) => Right(row :: acc)
      }.map(_.reverse)

      rowsEither.map { allRows =>
        var checks = 0
        var matched = 0
        val latencies = scala.collection.mutable.ListBuffer.empty[Long]
        def compare(maybe: Option[Boolean]): Unit =
          maybe.foreach { ok =>
            checks += 1
            if ok then matched += 1
          }

        allRows.foreach { row =>
          parseColor(row.color).foreach { color =>
            chess.format.Fen.read(chess.variant.Standard, chess.format.Fen.Full(row.fen)).map(_.board).foreach { board =>
              val started = System.nanoTime()
              val featureOpt = analyzer.analyze(board, color)
              latencies += elapsedMs(started)
              compare(row.expect.oppositionType.map(expected => featureOpt.exists(_.oppositionType.toString.equalsIgnoreCase(expected))))
              compare(row.expect.ruleOfSquare.map(expected => featureOpt.exists(_.ruleOfSquare.toString.equalsIgnoreCase(expected))))
              compare(row.expect.triangulationAvailable.map(expected => featureOpt.exists(_.triangulationAvailable == expected)))
              compare(row.expect.rookEndgamePattern.map(expected => featureOpt.exists(_.rookEndgamePattern.toString.equalsIgnoreCase(expected))))
              compare(row.expect.zugzwangLikely.map { expected =>
                featureOpt.exists(f => (f.isZugzwang || f.zugzwangLikelihood >= 0.65) == expected)
              })
            }
          }
        }

        V1Metrics(
          rows = allRows.size,
          checks = checks,
          matched = matched,
          f1 = if checks == 0 then 0.0 else matched.toDouble / checks.toDouble,
          latencyP95Ms = percentile(latencies.toList, 0.95)
        )
      }
    }

  private def evaluateV2(path: Path, analyzer: EndgameAnalyzerImpl): Either[String, V2Metrics] =
    readLines(path).flatMap { lines =>
      val rowsEither = lines.zipWithIndex.foldLeft[Either[String, List[V2PatternRow]]](Right(Nil)) {
        case (Left(err), _) => Left(err)
        case (Right(acc), (line, idx)) =>
          Json.parse(line).validate[V2PatternRow].asEither match
            case Left(err) => Left(s"invalid v2 row at line ${idx + 1}: $err")
            case Right(row) => Right(row :: acc)
      }.map(_.reverse)

      rowsEither.map { rows =>
        val patterns = rows.map(_.pattern).toSet
        var correct = 0
        var total = 0
        var predictedPositive = 0
        var actualPositive = 0
        var truePositive = 0
        var signalChecks = 0
        var signalMatches = 0
        val perPatternStats =
          scala.collection.mutable.Map.empty[String, (Int, Int, Int)].withDefaultValue((0, 0, 0)) // tp, fp, fn
        val latencies = scala.collection.mutable.ListBuffer.empty[Long]

        rows.foreach { row =>
          row.cases.foreach { c =>
            val colorOpt = parseColor(c.color)
            val boardOpt = chess.format.Fen.read(chess.variant.Standard, chess.format.Fen.Full(c.fen)).map(_.board)
            (colorOpt, boardOpt) match
              case (Some(color), Some(board)) =>
                val started = System.nanoTime()
                val coreOpt = analyzer.analyze(board, color)
                val finalFeatureOpt = coreOpt.map { core =>
                  EndgamePatternOracle.detect(board, color, core)
                    .map(EndgamePatternOracle.applyPattern(core, _))
                    .getOrElse(core)
                }
                latencies += elapsedMs(started)
                val predicted = finalFeatureOpt.flatMap(_.primaryPattern).getOrElse("None")
                val expected = c.expectedLabel

                total += 1
                if predicted == expected then correct += 1
                if predicted != "None" then predictedPositive += 1
                if expected != "None" then actualPositive += 1
                if predicted == expected && expected != "None" then truePositive += 1

                patterns.foreach { p =>
                  val (tp, fp, fn) = perPatternStats(p)
                  if expected == p && predicted == p then perPatternStats.update(p, (tp + 1, fp, fn))
                  else if expected != p && predicted == p then perPatternStats.update(p, (tp, fp + 1, fn))
                  else if expected == p && predicted != p then perPatternStats.update(p, (tp, fp, fn + 1))
                }

                finalFeatureOpt.foreach { feature =>
                  val expectedSignals =
                    if c.expectedLabel == row.pattern then mergeSignals(row.defaultSignals, c.signalsOverride.getOrElse(Json.obj()))
                    else c.signalsOverride.getOrElse(Json.obj())
                  val (checked, matched) = compareSignals(expectedSignals, feature)
                  signalChecks += checked
                  signalMatches += matched
                }
              case _ => ()
          }
        }

        val perPattern = patterns.toList.sorted.map { p =>
          val (tp, fp, fn) = perPatternStats(p)
          val precision = if tp + fp == 0 then 1.0 else tp.toDouble / (tp + fp).toDouble
          val recall = if tp + fn == 0 then 1.0 else tp.toDouble / (tp + fn).toDouble
          val f1 = if precision + recall == 0.0 then 0.0 else 2.0 * precision * recall / (precision + recall)
          p -> PatternMetrics(tp, fp, fn, precision, recall, f1)
        }.toMap

        val precision = if predictedPositive == 0 then 1.0 else truePositive.toDouble / predictedPositive.toDouble
        val recall = if actualPositive == 0 then 1.0 else truePositive.toDouble / actualPositive.toDouble
        val f1 = if precision + recall == 0.0 then 0.0 else 2.0 * precision * recall / (precision + recall)
        val macroF1 = if perPattern.isEmpty then 0.0 else perPattern.values.map(_.f1).sum / perPattern.size.toDouble
        val signalMatchRate = if signalChecks == 0 then 1.0 else signalMatches.toDouble / signalChecks.toDouble

        V2Metrics(
          totalCases = total,
          accuracy = if total == 0 then 0.0 else correct.toDouble / total.toDouble,
          precision = precision,
          recall = recall,
          f1 = f1,
          macroF1 = macroF1,
          signalChecks = signalChecks,
          signalMatches = signalMatches,
          signalMatchRate = signalMatchRate,
          perPattern = perPattern,
          latencyP95Ms = percentile(latencies.toList, 0.95)
        )
      }
    }

  private def compareSignals(
      expected: JsObject,
      feature: lila.llm.model.strategic.EndgameFeature
  ): (Int, Int) =
    var checks = 0
    var matches = 0

    def record(ok: Boolean): Unit =
      checks += 1
      if ok then matches += 1

    expected.value.foreach { (key, js) =>
      key match
        case "oppositionType" =>
          js.asOpt[String].foreach(expectedToken => record(matchesToken(feature.oppositionType.toString, expectedToken)))
        case "ruleOfSquare" =>
          js.asOpt[String].foreach(expectedToken => record(matchesToken(feature.ruleOfSquare.toString, expectedToken)))
        case "triangulationAvailable" =>
          js.asOpt[Boolean].foreach(v => record(feature.triangulationAvailable == v))
        case "rookEndgamePattern" =>
          js.asOpt[String].foreach(expectedToken => record(matchesToken(feature.rookEndgamePattern.toString, expectedToken)))
        case "zugzwangLikely" =>
          js.asOpt[Boolean].foreach(v => record((feature.isZugzwang || feature.zugzwangLikelihood >= 0.65) == v))
        case "theoreticalOutcomeHint" =>
          js.asOpt[String].foreach(expectedToken => record(matchesToken(feature.theoreticalOutcomeHint.toString, expectedToken)))
        case "confidenceMin" =>
          js.asOpt[Double].foreach(v => record(feature.confidence >= v))
        case "confidenceMax" =>
          js.asOpt[Double].foreach(v => record(feature.confidence <= v))
        case "kingActivityDeltaSign" =>
          js.asOpt[String].foreach {
            case "positive" => record(feature.kingActivityDelta > 0)
            case "neutral_or_positive" => record(feature.kingActivityDelta >= 0)
            case "neutral_or_negative" => record(feature.kingActivityDelta <= 0)
            case _ => ()
          }
        case _ => ()
    }
    (checks, matches)

  private def mergeSignals(base: JsObject, overrides: JsObject): JsObject =
    JsObject(base.value ++ overrides.value)

  private def matchesToken(actual: String, expectedToken: String): Boolean =
    expectedToken.split("_or_").exists(token => actual.equalsIgnoreCase(token))

  private def parseColor(raw: String): Option[Color] =
    if raw.equalsIgnoreCase("white") then Some(Color.White)
    else if raw.equalsIgnoreCase("black") then Some(Color.Black)
    else None

  private def readLines(path: Path): Either[String, List[String]] =
    if !Files.exists(path) then Left(s"file does not exist: $path")
    else
      try
        Right(
          Files
            .readAllLines(path, StandardCharsets.UTF_8)
            .toArray
            .toList
            .map(_.toString.stripPrefix("\uFEFF").trim)
            .filter(line => line.nonEmpty && !line.startsWith("#"))
        )
      catch case e: Exception => Left(e.getMessage)

  private def elapsedMs(startNs: Long): Long =
    ((System.nanoTime() - startNs) / 1000000L).max(0L)

  private def percentile(values: List[Long], p: Double): Double =
    if values.isEmpty then 0.0
    else
      val sorted = values.sorted
      val idx = math.ceil(p * sorted.size.toDouble).toInt - 1
      sorted(idx.max(0).min(sorted.size - 1)).toDouble

  private def writeReport(path: Path, text: String): Unit =
    val parent = path.getParent
    if parent != null then Files.createDirectories(parent)
    Files.writeString(path, text, StandardCharsets.UTF_8)

  private def renderReport(
      cfg: Config,
      v1: V1Metrics,
      v2: V2Metrics,
      combinedLatencyP95: Double,
      gate: GateVerdict
  ): String =
    val sb = new StringBuilder()
    sb.append("# Endgame Pattern Quality Report\n\n")
    sb.append(s"- V1 input: `${cfg.v1Input}`\n")
    sb.append(s"- V2 input: `${cfg.v2Input}`\n")
    sb.append(s"- Overall gate: ${if gate.overall then "PASS" else "FAIL"}\n\n")

    sb.append("## V1 Baseline\n\n")
    sb.append(s"- Rows: ${v1.rows}\n")
    sb.append(s"- Checks: ${v1.checks}\n")
    sb.append(s"- Matched: ${v1.matched}\n")
    sb.append(f"- Concept F1: ${v1.f1}%.4f (gate >= ${cfg.gateV1F1}%.2f)\n")
    sb.append(f"- Latency p95: ${v1.latencyP95Ms}%.3f ms\n\n")

    sb.append("## V2 Pattern Metrics\n\n")
    sb.append(s"- Cases: ${v2.totalCases}\n")
    sb.append(f"- Accuracy: ${v2.accuracy}%.4f\n")
    sb.append(f"- Precision: ${v2.precision}%.4f (gate >= ${cfg.gateV2Precision}%.2f)\n")
    sb.append(f"- Recall: ${v2.recall}%.4f\n")
    sb.append(f"- F1: ${v2.f1}%.4f (gate >= ${cfg.gateV2F1}%.2f)\n")
    sb.append(f"- Macro F1: ${v2.macroF1}%.4f\n")
    sb.append(f"- Signal match: ${v2.signalMatchRate}%.4f (checks=${v2.signalChecks}, matched=${v2.signalMatches}, gate >= ${cfg.gateSignalMatch}%.2f)\n")
    sb.append(f"- Latency p95: ${v2.latencyP95Ms}%.3f ms\n\n")

    sb.append("## Combined Latency Gate\n\n")
    sb.append(f"- max(v1, v2) p95: ${combinedLatencyP95}%.3f ms (gate <= ${cfg.gateLatencyP95Ms}%.2f)\n\n")

    sb.append("## Per-Pattern\n\n")
    sb.append("| Pattern | TP | FP | FN | Precision | Recall | F1 |\n")
    sb.append("|---|---:|---:|---:|---:|---:|---:|\n")
    v2.perPattern.toList.sortBy(_._1).foreach { case (pattern, m) =>
      sb.append(f"| $pattern | ${m.tp} | ${m.fp} | ${m.fn} | ${m.precision}%.4f | ${m.recall}%.4f | ${m.f1}%.4f |\n")
    }
    sb.append("\n")

    sb.append("## Gate Verdict\n\n")
    sb.append(s"- v1_f1: ${if gate.passV1 then "PASS" else "FAIL"}\n")
    sb.append(s"- v2_precision: ${if gate.passV2Precision then "PASS" else "FAIL"}\n")
    sb.append(s"- v2_f1: ${if gate.passV2F1 then "PASS" else "FAIL"}\n")
    sb.append(s"- signal_match: ${if gate.passSignalMatch then "PASS" else "FAIL"}\n")
    sb.append(s"- latency_p95: ${if gate.passLatency then "PASS" else "FAIL"}\n")
    sb.toString()

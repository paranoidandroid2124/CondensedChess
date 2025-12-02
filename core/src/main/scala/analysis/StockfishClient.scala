package chess
package analysis

import java.util.concurrent.atomic.{ AtomicBoolean, AtomicReference }

import scala.collection.mutable
import scala.sys.process.{ Process, ProcessIO }

/** 간단한 UCI Stockfish 래퍼.
  *
  * - depth 지정, MultiPV 지원
  * - info line에서 cp/mate/pv/depth/multipv 파싱
  * - bestmove까지 blocking
  */
final class StockfishClient(command: String = sys.env.getOrElse("STOCKFISH_BIN", "stockfish")):

  case class Line(
      multiPv: Int,
      depth: Int,
      cp: Option[Int],
      mate: Option[Int],
      pv: List[String]
  ):
    lazy val cpOrMate: Int = cp.getOrElse {
      // mate in N → 큰 절댓값으로 취급(부호는 side-to-move 기준)
      mate.fold(0)(m => if m > 0 then 32000 - m else -32000 - m)
    }
    lazy val winPercent: Double = StockfishClient.cpToWinPercent(cpOrMate)

  case class EvalResult(lines: List[Line], bestmove: Option[String])

  def evaluateFen(
      fen: String,
      depth: Int,
      multiPv: Int = 1,
      moveTimeMs: Option[Int] = Some(500)
  ): Either[String, EvalResult] =
    runEngine(fen, depth, multiPv, moveTimeMs)

  private def runEngine(fen: String, depth: Int, multiPv: Int, moveTimeMs: Option[Int]): Either[String, EvalResult] =
    val infoByPv = mutable.Map.empty[Int, Line]
    var best: Option[String] = None
    val errBuffer = new StringBuilder
    val stop = new AtomicBoolean(false)
    val writerRef = new AtomicReference[Option[java.io.PrintWriter]](None)

    val processIO = new ProcessIO(
      in => // writer
        val writer = new java.io.PrintWriter(in)
        writerRef.set(Some(writer))
        writer.println("uci")
        writer.println(s"setoption name MultiPV value $multiPv")
        writer.println("isready")
        writer.flush()
        writer.println(s"position fen $fen")
        val goCmd = moveTimeMs.fold(s"go depth $depth")(mt => s"go movetime $mt")
        writer.println(goCmd)
        writer.flush()
      ,
      out => // reader
        val source = scala.io.Source.fromInputStream(out)
        val iter = source.getLines()
        try
          while !stop.get() && iter.hasNext do
            val line = iter.next()
            if line.startsWith("bestmove") then
              best = line.split("\\s+").lift(1)
              stop.set(true)
              writerRef.get().foreach { w =>
                w.println("quit")
                w.flush()
                w.close()
              }
            else if line.startsWith("info") then
              parseInfo(line).foreach { l => infoByPv.update(l.multiPv, l) }
        finally source.close()
      ,
      err => // stderr collector
        val source = scala.io.Source.fromInputStream(err)
        try errBuffer.appendAll(source.mkString)
        finally source.close()
    )

    val process = Process(command).run(processIO)
    val exit = process.exitValue() // waits until quit

    val lines = infoByPv.toList.sortBy(_._1).map(_._2)
    if exit != 0 && lines.isEmpty then Left(s"Stockfish exited $exit: ${errBuffer.result()}")
    else Right(EvalResult(lines, best))

  private def parseInfo(line: String): Option[Line] =
    // info depth 18 seldepth 30 multipv 1 score cp 23 nodes ... pv e2e4 e7e5 ...
    val tokens = line.split("\\s+").toList
    def idx(key: String) = tokens.indexOf(key)
    val depthIdx = idx("depth")
    val multipvIdx = idx("multipv")
    val scoreIdx = idx("score")
    val pvIdx = idx("pv")
    if depthIdx == -1 || scoreIdx == -1 || pvIdx == -1 then None
    else
      val depth = tokens.lift(depthIdx + 1).flatMap(s => s.toIntOption).getOrElse(0)
      val multiPv = tokens.lift(multipvIdx + 1).flatMap(_.toIntOption).getOrElse(1)
      val (cp, mate) =
        tokens.lift(scoreIdx + 1) match
          case Some("cp") =>
            val cpVal = tokens.lift(scoreIdx + 2).flatMap(_.toIntOption)
            cpVal -> None
          case Some("mate") =>
            val mateVal = tokens.lift(scoreIdx + 2).flatMap(_.toIntOption)
            None -> mateVal
          case _ => None -> None
      val pv = tokens.drop(pvIdx + 1)
      Some(Line(multiPv = multiPv, depth = depth, cp = cp, mate = mate, pv = pv))

object StockfishClient:
  def cpToWinPercent(cp: Int): Double =
    100.0 / (1.0 + math.exp(-0.004 * cp))

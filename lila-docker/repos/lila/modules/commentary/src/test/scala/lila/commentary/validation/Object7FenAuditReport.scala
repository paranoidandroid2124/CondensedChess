package lila.commentary.validation

import chess.format.Fen

import scala.util.Try

object Object7FenAuditReport:

  final case class AuditRow(
      id: String,
      source: String,
      family: String,
      fen: String
  )

  private sealed trait AuditStatus:
    def key: String
    def sortWeight: Int

  private object AuditStatus:
    case object ParseFailure extends AuditStatus:
      val key = "parse_failure"
      val sortWeight = 0

    case object EngineRejected extends AuditStatus:
      val key = "engine_rejected"
      val sortWeight = 1

    case object Ok extends AuditStatus:
      val key = "ok"
      val sortWeight = 2

  private final case class AuditResult(
      row: AuditRow,
      status: AuditStatus,
      cp: Option[Int],
      mate: Option[Int],
      bestMove: Option[String],
      materialDelta: Int,
      error: Option[String]
  ):
    def severity: Int =
      mate.map(math.abs).map(_ + 10_000).orElse(cp.map(math.abs)).getOrElse(0)

    def scoreText: String =
      mate.map(m => s"mate=$m").orElse(cp.map(v => s"cp=$v")).getOrElse("score=none")

  private[validation] val corpusRows =
    ObjectExpectationCorpus.loadAll().map(row =>
      AuditRow(
        id = row.id,
        source = s"object-corpus/${row.caseType}",
        family = row.family,
        fen = row.fen
      )
    )

  private[validation] val ruleTestRows = Vector(
    AuditRow(
      id = "opening-exact-rule",
      source = "rule-test/StrategicObject7RuleTest",
      family = "OpeningDevelopmentRegime",
      fen = "r1bqkbnr/pppp1ppp/2n5/4p3/3PP3/5N2/PPP2PPP/RNBQKB1R w KQkq - 2 3"
    ),
    AuditRow(
      id = "opening-near-miss-rule",
      source = "rule-test/StrategicObject7RuleTest",
      family = "OpeningDevelopmentRegime",
      fen = "r1bq1rk1/ppp1bppp/2np1n2/4p3/3PP3/2N1BN2/PPP1BPPP/R2QK2R w KQ - 6 7"
    ),
    AuditRow(
      id = "opening-sparse-late-rule",
      source = "rule-test/StrategicObject7RuleTest",
      family = "OpeningDevelopmentRegime",
      fen = "1n3b1k/3pp3/8/8/8/8/3PP3/2B3NK w - - 0 1"
    ),
    AuditRow(
      id = "distributed-contact-exact-rule",
      source = "rule-test/StrategicObject7RuleTest",
      family = "DistributedContactRegime",
      fen = "6k1/3n2pp/8/1pppp3/1PPPP3/5N2/6PP/6K1 w - - 0 1"
    ),
    AuditRow(
      id = "distributed-contact-near-miss-rule",
      source = "rule-test/StrategicObject7RuleTest",
      family = "DistributedContactRegime",
      fen = "6k1/3n2pp/8/3pp3/3PP3/5N2/6PP/6K1 w - - 0 1"
    ),
    AuditRow(
      id = "endgame-race-exact-rule",
      source = "rule-test/StrategicObject7RuleTest",
      family = "EndgameRaceScaffold",
      fen = "4k3/2p5/3P4/8/6p1/8/4K3/8 w - - 0 1"
    ),
    AuditRow(
      id = "endgame-race-near-miss-rule",
      source = "rule-test/StrategicObject7RuleTest",
      family = "EndgameRaceScaffold",
      fen = "4k3/8/3P4/8/8/8/4K3/8 w - - 0 1"
    ),
    AuditRow(
      id = "attack-exact-rule",
      source = "rule-test/AttackScaffoldRuleTest",
      family = "AttackScaffold",
      fen = "6k1/6pp/8/8/3B4/8/6R1/6K1 w - - 0 1"
    ),
    AuditRow(
      id = "attack-off-theater-rule",
      source = "rule-test/AttackScaffoldRuleTest",
      family = "AttackScaffold",
      fen = "1k6/6pp/8/8/3B4/8/6R1/6K1 w - - 0 1"
    ),
    AuditRow(
      id = "attack-carrier-only-rule",
      source = "rule-test/AttackScaffoldRuleTest",
      family = "AttackScaffold",
      fen = "8/6k1/8/8/8/8/6R1/6K1 b - - 0 1"
    ),
    AuditRow(
      id = "attack-lone-open-file-rook-rule",
      source = "rule-test/AttackScaffoldRuleTest",
      family = "AttackScaffold",
      fen = "6k1/8/8/8/8/8/6R1/K7 b - - 0 1"
    ),
    AuditRow(
      id = "attack-stacked-heavy-same-file-rule",
      source = "rule-test/AttackScaffoldRuleTest",
      family = "AttackScaffold",
      fen = "6k1/8/8/8/8/8/6R1/6QK b - - 0 1"
    ),
    AuditRow(
      id = "fortress-exact-rule",
      source = "rule-test/FortressHoldingShellRuleTest",
      family = "FortressHoldingShell",
      fen = "r6k/6pp/8/8/4K3/8/8/1R6 w - - 0 1"
    ),
    AuditRow(
      id = "fortress-insufficient-shell-rule",
      source = "rule-test/FortressHoldingShellRuleTest",
      family = "FortressHoldingShell",
      fen = "r6k/7p/4b3/8/4K3/8/8/1R6 w - - 0 1"
    ),
    AuditRow(
      id = "fortress-direct-file-entry-rule",
      source = "rule-test/FortressHoldingShellRuleTest",
      family = "FortressHoldingShell",
      fen = "r6k/6pp/8/8/4K3/8/8/7R w - - 0 1"
    ),
    AuditRow(
      id = "fortress-diagonal-entry-rule",
      source = "rule-test/FortressHoldingShellRuleTest",
      family = "FortressHoldingShell",
      fen = "7k/6pp/8/8/4B3/4K3/8/8 w - - 0 1"
    ),
    AuditRow(
      id = "fortress-blocked-neighbor-file-major-rule",
      source = "rule-test/FortressHoldingShellRuleTest",
      family = "FortressHoldingShell",
      fen = "7k/6pp/8/6K1/6B1/8/8/6R1 w - - 0 1"
    ),
    AuditRow(
      id = "fortress-same-file-passer-rule",
      source = "rule-test/FortressHoldingShellRuleTest",
      family = "FortressHoldingShell",
      fen = "6k1/5b1r/8/6P1/8/8/8/R5K1 w - - 0 1"
    ),
    AuditRow(
      id = "king-safety-exact-rule",
      source = "rule-test/KingSafetyShellRuleTest",
      family = "KingSafetyShell",
      fen = "6k1/8/6p1/4B3/8/8/7R/6K1 w - - 0 1"
    ),
    AuditRow(
      id = "king-safety-single-hole-rule",
      source = "rule-test/KingSafetyShellRuleTest",
      family = "KingSafetyShell",
      fen = "6k1/5pp1/8/8/8/8/7R/6K1 w - - 0 1"
    ),
    AuditRow(
      id = "king-safety-non-adjacent-rule",
      source = "rule-test/KingSafetyShellRuleTest",
      family = "KingSafetyShell",
      fen = "6k1/6p1/6p1/4N3/8/8/7R/6K1 w - - 0 1"
    ),
    AuditRow(
      id = "king-safety-central-home-rank-rule",
      source = "rule-test/KingSafetyShellRuleTest",
      family = "KingSafetyShell",
      fen = "4k3/8/8/8/8/8/8/3RR1K1 b - - 0 1"
    ),
    AuditRow(
      id = "central-contact-canonical-component-rule",
      source = "rule-test/CentralContactFrontRuleTest",
      family = "CentralContactFront",
      fen = "4k3/8/2pp4/1pPP3n/4p3/5N2/1N2N3/4K3 w - - 0 1"
    ),
    AuditRow(
      id = "central-contact-exact-rule",
      source = "rule-test/CentralContactFrontRuleTest",
      family = "CentralContactFront",
      fen = "6k1/6pp/5n2/3pp3/3PP3/5N2/6PP/6K1 w - - 0 1"
    ),
    AuditRow(
      id = "central-contact-single-touch-rule",
      source = "rule-test/CentralContactFrontRuleTest",
      family = "CentralContactFront",
      fen = "4k3/8/8/8/8/8/3N1n2/4K3 w - - 0 1"
    ),
    AuditRow(
      id = "central-contact-empty-board-rule",
      source = "rule-test/CentralContactFrontRuleTest",
      family = "CentralContactFront",
      fen = "4k3/8/8/8/8/8/8/4K3 w - - 0 1"
    )
  )

  def main(args: Array[String]): Unit =
    val results =
      uniqueRows
        .map(audit)
        .sortBy(result =>
          (
            result.status.sortWeight,
            -result.severity,
            -math.abs(result.materialDelta),
            result.row.family,
            result.row.id
          )
        )

    println(s"Object7 FEN audit: ${results.size} unique positions")
    println(
      s"parse_failures=${results.count(_.status == AuditStatus.ParseFailure)} engine_rejections=${results.count(_.status == AuditStatus.EngineRejected)}"
    )
    results.foreach: result =>
      val bestMove = result.bestMove.getOrElse("-")
      val detail = result.error.map(error => s" | error=$error").getOrElse("")
      println(
        s"${result.status.key} | ${result.row.family} | ${result.row.id} | ${result.row.source} | ${result.scoreText} | mat=${result.materialDelta} | best=$bestMove | ${result.row.fen}$detail"
      )

  private[validation] val uniqueRows =
    (corpusRows ++ ruleTestRows).groupBy(_.fen).values.map(_.head).toVector

  private def audit(row: AuditRow): AuditResult =
    if Fen.read(Fen.Full.clean(row.fen)).isEmpty then
      AuditResult(
        row = row,
        status = AuditStatus.ParseFailure,
        cp = None,
        mate = None,
        bestMove = None,
        materialDelta = materialDelta(row.fen),
        error = Some("Fen.read failed")
      )
    else
      Try(StockfishProbe.probeFen(row.fen)).toEither match
        case Left(error) =>
          AuditResult(
            row = row,
            status = AuditStatus.EngineRejected,
            cp = None,
            mate = None,
            bestMove = None,
            materialDelta = materialDelta(row.fen),
            error = Some(error.getMessage)
          )
        case Right(probe) =>
          AuditResult(
            row = row,
            status = AuditStatus.Ok,
            cp = probe.cp,
            mate = probe.mate,
            bestMove = Some(probe.bestMove),
            materialDelta = materialDelta(row.fen),
            error = None
          )

  private def materialDelta(fen: String): Int =
    fen
      .takeWhile(_ != ' ')
      .foldLeft(0):
        case (acc, piece) =>
          acc + (piece match
            case 'P' => 1
            case 'N' => 3
            case 'B' => 3
            case 'R' => 5
            case 'Q' => 9
            case 'p' => -1
            case 'n' => -3
            case 'b' => -3
            case 'r' => -5
            case 'q' => -9
            case _ => 0
          )

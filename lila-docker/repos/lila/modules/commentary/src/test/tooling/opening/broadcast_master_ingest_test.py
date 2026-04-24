import json
import tempfile
import unittest
from pathlib import Path

from broadcast_master_ingest import run_ingest_from_pgn_text, validate_output_root


SAMPLE_PGN = """
[Event "Pilot Accepted"]
[Site "https://lichess.org/broadcast"]
[Date "2026.03.01"]
[Round "1"]
[White "Alpha, A."]
[Black "Beta, B."]
[Result "1-0"]
[WhiteElo "2450"]
[BlackElo "2380"]
[WhiteTitle "GM"]
[TimeControl "5400+30"]
[Variant "Standard"]
[ECO "C50"]
[Opening "Italian Game"]
[BroadcastName "Pilot Event"]
[BroadcastURL "https://lichess.org/broadcast/pilot/round-1/abc"]
[GameURL "https://lichess.org/broadcast/pilot/round-1/abc/game-1"]

1. e4 { [%eval 0.12] } e5 2. Nf3?! { Inaccuracy. Nc3 was best. } Nc6 3. Bc4 Bc5 1-0

[Event "Pilot Duplicate"]
[Site "https://lichess.org/broadcast"]
[Date "2026.03.01"]
[Round "1"]
[White "Alpha, A."]
[Black "Beta, B."]
[Result "1-0"]
[WhiteElo "2450"]
[BlackElo "2380"]
[WhiteTitle "GM"]
[TimeControl "5400+30"]
[Variant "Standard"]
[ECO "C50"]
[Opening "Italian Game"]
[BroadcastName "Pilot Event"]
[BroadcastURL "https://lichess.org/broadcast/pilot/round-1/abc"]
[GameURL "https://lichess.org/broadcast/pilot/round-1/abc/game-1"]

1. e4 e5 2. Nf3 Nc6 3. Bc4 Bc5 1-0

[Event "Pilot Natural Time Control"]
[Site "https://lichess.org/broadcast"]
[Date "2026.03.01"]
[Round "1"]
[White "Eta, E."]
[Black "Theta, T."]
[Result "1/2-1/2"]
[WhiteElo "2250"]
[BlackElo "2210"]
[BlackTitle "FM"]
[TimeControl "90Minutes +30 Seconds Increment from move one"]
[Variant "Standard"]
[ECO "D30"]
[Opening "Queen's Gambit Declined"]
[BroadcastName "Pilot Event"]
[BroadcastURL "https://lichess.org/broadcast/pilot/round-1/abc"]
[GameURL "https://lichess.org/broadcast/pilot/round-1/abc/game-natural-time"]

1. d4 d5 2. c4 e6 1/2-1/2

[Event "Pilot Multi Period Time Control"]
[Site "https://lichess.org/broadcast"]
[Date "2026.03.01"]
[Round "1"]
[White "Iota, I."]
[Black "Kappa, K."]
[Result "0-1"]
[WhiteElo "2300"]
[BlackElo "2330"]
[WhiteTitle "IM"]
[TimeControl "40/5400+30:1800+30"]
[Variant "Standard"]
[ECO "E60"]
[Opening "King's Indian Defense"]
[BroadcastName "Pilot Event"]
[BroadcastURL "https://lichess.org/broadcast/pilot/round-1/abc"]
[GameURL "https://lichess.org/broadcast/pilot/round-1/abc/game-multi-period"]

1. d4 Nf6 2. c4 g6 0-1

[Event "Pilot Blitz"]
[Site "https://lichess.org/broadcast"]
[Date "2026.03.01"]
[Round "2"]
[White "Gamma, G."]
[Black "Delta, D."]
[Result "0-1"]
[WhiteElo "2500"]
[BlackElo "2490"]
[BlackTitle "IM"]
[TimeControl "180+2"]
[Variant "Standard"]
[ECO "B20"]
[Opening "Sicilian Defense"]
[BroadcastName "Pilot Event"]
[BroadcastURL "https://lichess.org/broadcast/pilot/round-2/def"]
[GameURL "https://lichess.org/broadcast/pilot/round-2/def/game-2"]

1. e4 c5 0-1

[Event "Pilot Bullet"]
[Site "https://lichess.org/broadcast"]
[Date "2026.03.01"]
[Round "2"]
[White "Bullet, B."]
[Black "Fast, F."]
[Result "1-0"]
[WhiteElo "2500"]
[BlackElo "2490"]
[WhiteTitle "GM"]
[TimeControl "60+0"]
[Variant "Standard"]
[ECO "A00"]
[Opening "Van't Kruijs Opening"]
[BroadcastName "Pilot Event"]
[BroadcastURL "https://lichess.org/broadcast/pilot/round-2/def"]
[GameURL "https://lichess.org/broadcast/pilot/round-2/def/game-bullet"]

1. e3 e5 1-0

[Event "Pilot Missing Broadcast"]
[Site "https://lichess.org/broadcast"]
[Date "2026.03.01"]
[Round "2"]
[White "Broadcast, Missing"]
[Black "Meta, Good"]
[Result "1-0"]
[WhiteElo "2500"]
[BlackElo "2490"]
[WhiteTitle "GM"]
[TimeControl "3600+30"]
[Variant "Standard"]
[ECO "C20"]
[Opening "King's Pawn Game"]
[GameURL "https://lichess.org/broadcast/pilot/round-2/def/game-no-broadcast"]

1. e4 e5 1-0

[Event "Pilot Non Standard"]
[Site "https://lichess.org/broadcast"]
[Date "2026.03.01"]
[Round "2"]
[White "Variant, V."]
[Black "Standard, S."]
[Result "1-0"]
[WhiteElo "2500"]
[BlackElo "2490"]
[WhiteTitle "GM"]
[TimeControl "3600+30"]
[Variant "Chess960"]
[ECO "A00"]
[Opening "Start Position"]
[BroadcastName "Pilot Event"]
[BroadcastURL "https://lichess.org/broadcast/pilot/round-2/def"]
[GameURL "https://lichess.org/broadcast/pilot/round-2/def/game-variant"]

1. e4 e5 1-0

[Event "Pilot Unsupported Result"]
[Site "https://lichess.org/broadcast"]
[Date "2026.03.01"]
[Round "2"]
[White "Result, Unknown"]
[Black "Meta, Good"]
[Result "*"]
[WhiteElo "2500"]
[BlackElo "2490"]
[WhiteTitle "GM"]
[TimeControl "3600+30"]
[Variant "Standard"]
[ECO "C20"]
[Opening "King's Pawn Game"]
[BroadcastName "Pilot Event"]
[BroadcastURL "https://lichess.org/broadcast/pilot/round-2/def"]
[GameURL "https://lichess.org/broadcast/pilot/round-2/def/game-unsupported-result"]

1. e4 e5 *

[Event "Pilot Missing Time Control"]
[Site "https://lichess.org/broadcast"]
[Date "2026.03.01"]
[Round "2"]
[White "Clock, Missing"]
[Black "Meta, Good"]
[Result "1-0"]
[WhiteElo "2500"]
[BlackElo "2490"]
[WhiteTitle "GM"]
[Variant "Standard"]
[ECO "C20"]
[Opening "King's Pawn Game"]
[BroadcastName "Pilot Event"]
[BroadcastURL "https://lichess.org/broadcast/pilot/round-2/def"]
[GameURL "https://lichess.org/broadcast/pilot/round-2/def/game-missing-time-control"]

1. e4 e5 1-0

[Event "Pilot Missing Player Metadata"]
[Site "https://lichess.org/broadcast"]
[Date "2026.03.01"]
[Round "3"]
[White "No Rating"]
[Black "No Title"]
[Result "1/2-1/2"]
[TimeControl "3600+30"]
[Variant "Standard"]
[ECO "D00"]
[Opening "Queen's Pawn Game"]
[BroadcastName "Pilot Event"]
[BroadcastURL "https://lichess.org/broadcast/pilot/round-3/ghi"]
[GameURL "https://lichess.org/broadcast/pilot/round-3/ghi/game-3"]

1. d4 d5 1/2-1/2
"""


class BroadcastMasterIngestTest(unittest.TestCase):
    def local_root(self) -> Path:
        base = Path.cwd() / "tmp" / "commentary-opening" / "unit-tests"
        base.mkdir(parents=True, exist_ok=True)
        return base

    def test_filters_and_writes_master_reference_pilot_artifacts(self):
        with tempfile.TemporaryDirectory(dir=self.local_root()) as tmp:
            root = Path(tmp)
            result = run_ingest_from_pgn_text(
                SAMPLE_PGN,
                root=root,
                source_id="lichess-broadcast-master-reference-2026-03-pilot-test",
                source_url="https://database.lichess.org/broadcast/lichess_db_broadcast_2026-03.pgn.zst",
                source_checksum="fixture-checksum",
                year_month="2026-03",
                max_read_games=20,
                max_accepted_games=10,
                per_position_sample_size_threshold=2,
            )

            self.assertEqual(result.summary["readGames"], 11)
            self.assertEqual(result.summary["acceptedGames"], 3)
            self.assertEqual(result.summary["dedupedGames"], 1)
            self.assertEqual(result.summary["rejectedByReason"]["fast_time_control"], 2)
            self.assertEqual(result.summary["rejectedByReason"]["missing_player_level_metadata"], 1)
            self.assertEqual(result.summary["rejectedByReason"]["missing_broadcast_provenance"], 1)
            self.assertEqual(result.summary["rejectedByReason"]["non_standard_variant"], 1)
            self.assertEqual(result.summary["rejectedByReason"]["unsupported_result"], 1)
            self.assertEqual(result.summary["rejectedByReason"]["missing_time_control"], 1)
            self.assertEqual(result.summary["annotationReport"]["gamesWithEngineEvalComments"], 1)
            self.assertEqual(result.summary["annotationReport"]["gamesWithTextualAdviceComments"], 1)
            self.assertEqual(result.summary["annotationReport"]["gamesWithNags"], 1)

            manifest = json.loads(result.manifest_path.read_text(encoding="utf-8"))
            self.assertEqual(manifest["sourceUse"], "master_reference")
            self.assertEqual(manifest["aggregateUse"], "master_reference_stat")
            self.assertEqual(manifest["dedupePolicy"], "stable_game_id_or_normalized_pgn_hash")
            self.assertEqual(manifest["annotationPolicy"], "strip_comments_report_engine_eval")
            self.assertEqual(manifest["rawStoragePolicy"], "localOnly")
            self.assertEqual(manifest["generatedStoragePolicy"], "localOnly")
            self.assertEqual(manifest["sourceScope"], "full_month")
            self.assertIn("CC BY-SA 4.0", manifest["licenseNotice"])
            self.assertTrue(str(result.manifest_path).replace("\\", "/").endswith("/source-manifest.json"))
            self.assertTrue(result.summary["processedFullInput"])
            self.assertIn("positionSampleDistribution", result.summary)
            self.assertIn("topOpeningFamilies", result.summary)
            self.assertGreater(result.summary["lowSampleRows"], 0)
            self.assertTrue(result.position_sample_distribution_path.exists())
            self.assertTrue(result.family_coverage_path.exists())
            self.assertTrue(result.low_sample_rows_path.exists())

            rows = [
                json.loads(line)
                for line in result.move_stats_path.read_text(encoding="utf-8").splitlines()
                if line.strip()
            ]
            self.assertGreater(len(rows), 0)
            self.assertTrue(all(row["aggregateUse"] == "master_reference_stat" for row in rows))
            self.assertTrue(all(row["candidateKind"] == "statistical_reference" for row in rows))
            self.assertTrue(all(row["currentPositionTruth"] is False for row in rows))
            self.assertTrue(all(row["sampleConfidence"] in {"below_threshold", "meets_threshold"} for row in rows))
            forbidden = {"bestMove", "theoryTruth", "forcedLine", "objectiveResult", "engineVerdict"}
            self.assertTrue(all(forbidden.isdisjoint(row.keys()) for row in rows))

    def test_capped_run_is_streamed_sample_not_full_month(self):
        with tempfile.TemporaryDirectory(dir=self.local_root()) as tmp:
            root = Path(tmp)
            result = run_ingest_from_pgn_text(
                SAMPLE_PGN,
                root=root,
                source_id="lichess-broadcast-master-reference-2026-03-sample-test",
                source_url="https://database.lichess.org/broadcast/lichess_db_broadcast_2026-03.pgn.zst",
                source_checksum="fixture-checksum",
                year_month="2026-03",
                max_read_games=2,
                max_accepted_games=10,
                per_position_sample_size_threshold=2,
            )

            manifest = json.loads(result.manifest_path.read_text(encoding="utf-8"))
            self.assertEqual(manifest["sourceScope"], "streamed_sample")
            self.assertFalse(result.summary["processedFullInput"])
            self.assertEqual(result.summary["stopReason"], "max_read_games")

    def test_output_root_must_stay_under_ignored_opening_roots(self):
        with tempfile.TemporaryDirectory() as tmp:
            bad_root = Path(tmp) / "repo-visible-output"
            with self.assertRaisesRegex(ValueError, "ignored opening local output roots"):
                validate_output_root(bad_root)

        good_root = self.local_root() / "allowed-root-test"
        self.assertEqual(validate_output_root(good_root), good_root.resolve())


if __name__ == "__main__":
    unittest.main()

import json
import tempfile
import unittest
from pathlib import Path

from compare_opening_source_stats import compare_sources


def write_json(path: Path, value: dict) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, sort_keys=True) + "\n", encoding="utf-8")


def write_jsonl(path: Path, rows: list[dict]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(
        "".join(json.dumps(row, sort_keys=True, separators=(",", ":")) + "\n" for row in rows),
        encoding="utf-8",
    )


class CompareOpeningSourceStatsTest(unittest.TestCase):
    def test_compares_sources_without_merging_rankings(self):
        report_parent = Path.cwd() / "tmp" / "commentary-opening" / "reports"
        report_parent.mkdir(parents=True, exist_ok=True)
        with tempfile.TemporaryDirectory(dir=report_parent) as tmp:
            root = Path(tmp)
            contract = root / "fixtures" / "opening-sources.jsonl"
            master_manifest = root / "generated" / "master" / "source-manifest.json"
            online_manifest = root / "generated" / "online" / "source-manifest.json"
            master_rows = root / "generated" / "master" / "opening-move-stats.jsonl"
            online_rows = root / "generated" / "online" / "opening-move-stats.jsonl"
            report_dir = root / "reports" / "comparison"

            write_jsonl(
                contract,
                [
                    {
                        "sourceId": "online",
                        "sourceFamily": "opening",
                        "sourceUse": "online_trend",
                        "aggregateUse": "online_trend_stat",
                    }
                ],
            )
            write_json(
                master_manifest,
                {
                    "sourceId": "master",
                    "sourceUse": "master_reference",
                    "aggregateUse": "master_reference_stat",
                    "sourceChecksum": "master-checksum",
                    "attributionText": "Source attribution",
                    "licenseNotice": "Share-alike notice",
                },
            )
            write_json(
                online_manifest,
                {
                    "sourceId": "online",
                    "aggregateUse": "trend_stat",
                    "sourceChecksum": "online-checksum",
                },
            )
            write_jsonl(
                master_rows,
                [
                    {
                        "id": "m-a",
                        "sourceId": "master",
                        "sourceUse": "master_reference",
                        "aggregateUse": "master_reference_stat",
                        "positionKey": "std:common a",
                        "move": "e2e4",
                        "sampleSize": 9,
                        "frequency": 0.6,
                        "candidateKind": "statistical_reference",
                        "currentPositionTruth": False,
                        "positionSampleSize": 15,
                        "sampleConfidence": "meets_threshold",
                    },
                    {
                        "id": "m-b",
                        "sourceId": "master",
                        "sourceUse": "master_reference",
                        "aggregateUse": "master_reference_stat",
                        "positionKey": "std:common a",
                        "move": "d2d4",
                        "sampleSize": 6,
                        "frequency": 0.4,
                        "candidateKind": "statistical_reference",
                        "currentPositionTruth": False,
                        "positionSampleSize": 15,
                        "sampleConfidence": "meets_threshold",
                    },
                    {
                        "id": "m-only",
                        "sourceId": "master",
                        "sourceUse": "master_reference",
                        "aggregateUse": "master_reference_stat",
                        "positionKey": "std:master only",
                        "move": "g1f3",
                        "sampleSize": 1,
                        "frequency": 1.0,
                        "candidateKind": "statistical_reference",
                        "currentPositionTruth": False,
                        "positionSampleSize": 1,
                        "sampleConfidence": "below_threshold",
                    },
                    {
                        "id": "m-low-common",
                        "sourceId": "master",
                        "sourceUse": "master_reference",
                        "aggregateUse": "master_reference_stat",
                        "positionKey": "std:low common",
                        "move": "b1c3",
                        "sampleSize": 1,
                        "frequency": 1.0,
                        "candidateKind": "statistical_reference",
                        "currentPositionTruth": False,
                        "positionSampleSize": 1,
                        "sampleConfidence": "below_threshold",
                    },
                ],
            )
            write_jsonl(
                online_rows,
                [
                    {
                        "id": "o-a",
                        "sourceId": "online",
                        "positionKey": "std:common a",
                        "move": "d2d4",
                        "sampleSize": 8,
                        "frequency": 0.8,
                        "candidateKind": "statistical_reference",
                    },
                    {
                        "id": "o-b",
                        "sourceId": "online",
                        "positionKey": "std:common a",
                        "move": "e2e4",
                        "sampleSize": 1,
                        "frequency": 0.1,
                        "candidateKind": "statistical_reference",
                    },
                    {
                        "id": "o-only",
                        "sourceId": "online",
                        "positionKey": "std:online only",
                        "move": "c2c4",
                        "sampleSize": 1,
                        "frequency": 1.0,
                        "candidateKind": "statistical_reference",
                    },
                    {
                        "id": "o-low-common",
                        "sourceId": "online",
                        "positionKey": "std:low common",
                        "move": "g1f3",
                        "sampleSize": 1,
                        "frequency": 1.0,
                        "candidateKind": "statistical_reference",
                    },
                ],
            )

            result = compare_sources(
                master_manifest_path=master_manifest,
                master_rows_path=master_rows,
                online_manifest_path=online_manifest,
                online_rows_path=online_rows,
                contract_manifest_path=contract,
                output_dir=report_dir,
            )

            self.assertEqual(result["summary"]["commonPositionCount"], 2)
            self.assertEqual(result["summary"]["masterOnlyPositionCount"], 1)
            self.assertEqual(result["summary"]["onlineOnlyPositionCount"], 1)
            self.assertEqual(result["summary"]["frequencyDisagreementTotalCount"], 2)
            self.assertEqual(result["summary"]["highConfidenceFrequencyDisagreementCount"], 1)
            self.assertEqual(result["summary"]["frequencyDisagreementExampleCount"], 1)
            self.assertEqual(result["sourceBoundary"]["master"]["aggregateUse"], "master_reference_stat")
            self.assertEqual(result["sourceBoundary"]["online"]["aggregateUse"], "online_trend_stat")
            self.assertEqual(result["sourceBoundary"]["rankingPolicy"], "do_not_merge_rankings")
            self.assertTrue(result["manifestWarnings"])
            self.assertEqual(result["frequencyDisagreementExamples"][0]["masterMostFrequentMove"], "e2e4")
            self.assertEqual(result["frequencyDisagreementExamples"][0]["onlineMostFrequentMove"], "d2d4")
            self.assertEqual(result["frequencyDisagreementExamples"][0]["onlinePositionSampleSize"], 10)
            self.assertNotIn("topDisagreements", result)
            self.assertNotIn("topMove", json.dumps(result))
            self.assertNotIn("disagreementScore", json.dumps(result))
            self.assertGreater(result["lowConfidence"]["masterRows"], 0)
            self.assertGreater(result["lowConfidence"]["onlineRows"], 0)
            self.assertIn("sampleCapped", result["lowConfidence"])
            self.assertIn("sampleLimitPerSource", result["lowConfidence"])
            self.assertEqual(result["recommendedSourcePolicy"]["defaultEducationalSource"], "master_reference")
            self.assertTrue((report_dir / "comparison-summary.json").exists())
            self.assertTrue((report_dir / "frequency-disagreements.jsonl").exists())
            self.assertTrue((report_dir / "low-confidence-row-sample.jsonl").exists())

    def test_rejects_truth_leaking_fields_and_escaped_output_path(self):
        report_parent = Path.cwd() / "tmp" / "commentary-opening" / "reports"
        report_parent.mkdir(parents=True, exist_ok=True)
        with tempfile.TemporaryDirectory(dir=report_parent) as tmp:
            root = Path(tmp)
            contract = root / "fixtures" / "opening-sources.jsonl"
            master_manifest = root / "generated" / "master" / "source-manifest.json"
            online_manifest = root / "generated" / "online" / "source-manifest.json"
            master_rows = root / "generated" / "master" / "opening-move-stats.jsonl"
            online_rows = root / "generated" / "online" / "opening-move-stats.jsonl"
            write_jsonl(contract, [{"sourceId": "online", "sourceUse": "online_trend", "aggregateUse": "online_trend_stat"}])
            write_json(
                master_manifest,
                {
                    "sourceId": "master",
                    "sourceUse": "master_reference",
                    "aggregateUse": "master_reference_stat",
                    "sourceChecksum": "master-checksum",
                    "attributionText": "Source attribution",
                    "licenseNotice": "Share-alike notice",
                },
            )
            write_json(online_manifest, {"sourceId": "online", "sourceChecksum": "online-checksum"})
            write_jsonl(
                master_rows,
                [
                    {
                        "id": "truth-leak",
                        "sourceId": "master",
                        "sourceUse": "master_reference",
                        "aggregateUse": "master_reference_stat",
                        "positionKey": "std:x",
                        "move": "e2e4",
                        "sampleSize": 1,
                        "frequency": 1.0,
                        "candidateKind": "statistical_reference",
                        "oracleVerdict": "forbidden",
                    }
                ],
            )
            write_jsonl(
                online_rows,
                [
                    {
                        "id": "online",
                        "sourceId": "online",
                        "positionKey": "std:x",
                        "move": "d2d4",
                        "sampleSize": 1,
                        "frequency": 1.0,
                        "candidateKind": "statistical_reference",
                    }
                ],
            )
            with self.assertRaises(ValueError):
                compare_sources(
                    master_manifest_path=master_manifest,
                    master_rows_path=master_rows,
                    online_manifest_path=online_manifest,
                    online_rows_path=online_rows,
                    contract_manifest_path=contract,
                    output_dir=root / ".." / ".." / "generated",
                )
            with self.assertRaises(ValueError):
                compare_sources(
                    master_manifest_path=master_manifest,
                    master_rows_path=master_rows,
                    online_manifest_path=online_manifest,
                    online_rows_path=online_rows,
                    contract_manifest_path=contract,
                    output_dir=Path("modules/commentary/tmp/commentary-opening/reports/bad"),
                )
            with self.assertRaises(ValueError):
                compare_sources(
                    master_manifest_path=master_manifest,
                    master_rows_path=master_rows,
                    online_manifest_path=online_manifest,
                    online_rows_path=online_rows,
                    contract_manifest_path=contract,
                    output_dir=root / "reports" / "comparison",
                )


if __name__ == "__main__":
    unittest.main()

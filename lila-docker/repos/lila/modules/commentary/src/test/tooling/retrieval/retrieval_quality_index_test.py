import json
import sys
import tempfile
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

from retrieval_quality_index import build_retrieval_quality_index, position_key_from_fen, side_to_move_from_fen, validate_output_root


def read_json(path: Path) -> dict:
    return json.loads(path.read_text(encoding="utf-8"))


def read_jsonl(path: Path) -> list[dict]:
    return [json.loads(line) for line in path.read_text(encoding="utf-8").splitlines() if line.strip()]


class RetrievalQualityIndexTest(unittest.TestCase):
    def local_root(self) -> Path:
        base = Path.cwd() / "tmp" / "commentary-opening" / "retrieval" / "unit-tests"
        base.mkdir(parents=True, exist_ok=True)
        return base

    def test_builds_local_only_index_summary_and_fail_closed_rejects(self):
        exact_fen = "rnbqkb1r/ppp2ppp/4pn2/8/2pP4/6P1/PP2PPBP/RNBQK1NR w KQkq - 0 5"
        exact_key = "std:rnbqkb1r/ppp2ppp/4pn2/8/2pP4/6P1/PP2PPBP/RNBQK1NR w KQkq -"
        candidates = [
            {
                "retrievalId": "quality-exact-catalan",
                "sourceId": "example-index",
                "sourceRef": "retrieval-example:quality-exact-catalan",
                "exampleKind": "curated_reference",
                "fen": exact_fen,
                "similarityKind": "exact_position",
                "similarityScore": 0.98,
                "similarityKey": {
                    "positionKey": exact_key,
                    "openingFamily": "Catalan Opening",
                    "openingAlias": "Open Defense",
                    "pawnStructure": ["queenside_tension"],
                },
                "gameMetadata": {
                    "players": ["White Player", "Black Player"],
                    "event": "Quality citation fixture",
                    "date": "2026-03",
                    "round": "1",
                    "result": "1/2-1/2",
                    "url": "local-curation:quality-exact-catalan",
                },
                "licenseNotice": "Local curated fixture metadata.",
                "attributionText": "Local curated retrieval fixture.",
            },
            {
                "retrievalId": "quality-motif-fork",
                "sourceId": "example-index",
                "sourceRef": "retrieval-example:quality-motif-fork",
                "exampleKind": "puzzle_reference",
                "fen": "4k3/8/3r1r2/8/4N3/8/8/4K3 w - - 0 1",
                "similarityKind": "motif_context",
                "similarityScore": 0.82,
                "similarityKey": {
                    "motifTags": ["fork"],
                    "motifCarriers": ["motif-detector-carrier:motif-fork-example"],
                },
                "licenseNotice": "Local curated fixture metadata.",
                "attributionText": "Local curated retrieval fixture.",
            },
            {
                "retrievalId": "quality-endgame-lucena",
                "sourceId": "example-index",
                "sourceRef": "retrieval-example:quality-endgame-lucena",
                "exampleKind": "study_reference",
                "fen": "6k1/3PK3/8/8/8/8/4R3/4r3 w - - 0 1",
                "similarityKind": "endgame_study_context",
                "similarityScore": 0.88,
                "similarityKey": {"endgameStudy": ["lucena_rook_pawn"], "applicabilityRefs": ["endgame-study-applicability:study-lucena-context"]},
                "licenseNotice": "Local curated fixture metadata.",
                "attributionText": "Local curated retrieval fixture.",
            },
            {
                "retrievalId": "quality-missing-source-ref",
                "sourceId": "example-index",
                "exampleKind": "curated_reference",
                "fen": "4k3/8/8/8/8/8/4N3/4K3 w - - 0 1",
                "similarityKind": "mixed_feature_context",
                "similarityScore": 0.72,
            },
            {
                "retrievalId": "quality-citation-missing-attribution",
                "sourceId": "example-index",
                "sourceRef": "retrieval-example:quality-citation-missing-attribution",
                "exampleKind": "broadcast_game_reference",
                "fen": "4k3/8/8/8/8/8/4N3/4K3 w - - 0 1",
                "similarityKind": "mixed_feature_context",
                "similarityScore": 0.72,
                "gameMetadata": {"players": ["White Player", "Black Player"], "result": "1-0"},
            },
            {
                "retrievalId": "quality-duplicate-source-ref",
                "sourceId": "example-index",
                "sourceRef": "retrieval-example:quality-exact-catalan",
                "exampleKind": "curated_reference",
                "fen": "4k3/8/8/8/8/8/4N3/4K3 w - - 0 1",
                "similarityKind": "mixed_feature_context",
                "similarityScore": 0.72,
                "licenseNotice": "Local curated fixture metadata.",
                "attributionText": "Local curated retrieval fixture.",
            },
            {
                "retrievalId": "quality-duplicate-similarity-key",
                "sourceId": "example-index",
                "sourceRef": "retrieval-example:quality-duplicate-similarity-key",
                "exampleKind": "curated_reference",
                "fen": exact_fen,
                "positionKey": exact_key,
                "sideToMove": "white",
                "similarityKind": "exact_position",
                "similarityScore": 0.99,
                "similarityKey": {"positionKey": exact_key},
                "licenseNotice": "Local curated fixture metadata.",
                "attributionText": "Local curated retrieval fixture.",
            },
            {
                "retrievalId": "quality-low-similarity",
                "sourceId": "example-index",
                "sourceRef": "retrieval-example:quality-low-similarity",
                "exampleKind": "curated_reference",
                "fen": "4k3/8/8/8/8/8/4N3/4K3 w - - 0 1",
                "similarityKind": "mixed_feature_context",
                "similarityScore": 0.4,
                "licenseNotice": "Local curated fixture metadata.",
                "attributionText": "Local curated retrieval fixture.",
            },
            {
                "retrievalId": "quality-truth-snippet",
                "sourceId": "example-index",
                "sourceRef": "retrieval-example:quality-truth-snippet",
                "exampleKind": "curated_reference",
                "fen": "4k3/8/8/8/8/8/4N3/4K3 w - - 0 1",
                "similarityKind": "mixed_feature_context",
                "similarityScore": 0.72,
                "snippetText": "This proves the best forced result.",
                "licenseNotice": "Local curated fixture metadata.",
                "attributionText": "Local curated retrieval fixture.",
            },
            {
                "retrievalId": "quality-live-board-flag",
                "sourceId": "example-index",
                "sourceRef": "retrieval-example:quality-live-board-flag",
                "exampleKind": "curated_reference",
                "fen": "4k3/8/8/8/8/8/4N3/4K3 w - - 0 1",
                "similarityKind": "mixed_feature_context",
                "similarityScore": 0.72,
                "currentPositionClaim": True,
                "licenseNotice": "Local curated fixture metadata.",
                "attributionText": "Local curated retrieval fixture.",
            },
            {
                "retrievalId": "quality-opening-wrong-side",
                "sourceId": "example-index",
                "sourceRef": "retrieval-example:quality-opening-wrong-side",
                "exampleKind": "curated_reference",
                "fen": "rnbqkb1r/ppp2ppp/4pn2/8/2pP4/6P1/PP2PPBP/RNBQK1NR w KQkq - 0 5",
                "similarityKind": "opening_context",
                "similarityScore": 0.72,
                "similarityKey": {
                    "openingFamily": "Catalan Opening",
                    "sideToMove": "black",
                    "materialSignature": "king_pawn_vs_king",
                },
                "licenseNotice": "Local curated fixture metadata.",
                "attributionText": "Local curated retrieval fixture.",
            },
            {
                "retrievalId": "quality-best-forced-result-s21-current-position-proof",
                "sourceId": "example-index",
                "sourceRef": "retrieval-example:quiet-reference",
                "exampleKind": "curated_reference",
                "fen": "4k3/8/8/8/8/8/4N3/4K3 w - - 0 1",
                "positionKey": "std:4k3/8/8/8/8/8/4N3/4K3 w - -",
                "sideToMove": "white",
                "similarityKind": "mixed_feature_context",
                "similarityScore": 0.72,
                "similarityKey": {"materialClass": "minor_piece", "phaseContext": ["endgame"]},
                "licenseNotice": "Local curated fixture metadata.",
                "attributionText": "Local curated retrieval fixture.",
            },
            {
                "retrievalId": "quality-quiet-id",
                "sourceId": "example-index",
                "sourceRef": "retrieval-example:best-forced-result-s21-current-position-proof",
                "exampleKind": "curated_reference",
                "fen": "4k3/8/8/8/8/8/4N3/4K3 w - - 0 1",
                "positionKey": "std:4k3/8/8/8/8/8/4N3/4K3 w - -",
                "sideToMove": "white",
                "similarityKind": "mixed_feature_context",
                "similarityScore": 0.72,
                "similarityKey": {"materialClass": "minor_piece", "phaseContext": ["endgame"]},
                "licenseNotice": "Local curated fixture metadata.",
                "attributionText": "Local curated retrieval fixture.",
            },
            {
                "retrievalId": "quality-exact-missing-key",
                "sourceId": "example-index",
                "sourceRef": "retrieval-example:quality-exact-missing-key",
                "exampleKind": "curated_reference",
                "fen": "rnbqkb1r/ppp2ppp/4pn2/8/2pP4/6P1/PP2PPBP/RNBQK1NR w KQkq - 0 5",
                "positionKey": "std:rnbqkb1r/ppp2ppp/4pn2/8/2pP4/6P1/PP2PPBP/RNBQK1NR w KQkq -",
                "sideToMove": "white",
                "similarityKind": "exact_position",
                "similarityScore": 0.98,
                "similarityKey": {"openingFamily": "Catalan Opening", "pawnStructure": ["queenside_tension"]},
                "licenseNotice": "Local curated fixture metadata.",
                "attributionText": "Local curated retrieval fixture.",
            },
            {
                "retrievalId": "quality-mismatched-key-side",
                "sourceId": "example-index",
                "sourceRef": "retrieval-example:quality-mismatched-key-side",
                "exampleKind": "curated_reference",
                "fen": "4k3/8/8/8/8/8/4N3/4K3 w - - 0 1",
                "positionKey": "std:4k3/8/8/8/8/8/4N3/4K3 w - -",
                "sideToMove": "white",
                "similarityKind": "mixed_feature_context",
                "similarityScore": 0.72,
                "similarityKey": {"materialClass": "minor_piece", "phaseContext": ["endgame"], "sideToMove": "black"},
                "licenseNotice": "Local curated fixture metadata.",
                "attributionText": "Local curated retrieval fixture.",
            },
            {
                "retrievalId": "quality-wrong-opening-family",
                "sourceId": "example-index",
                "sourceRef": "retrieval-example:quality-wrong-opening-family",
                "exampleKind": "curated_reference",
                "fen": "rnbqkb1r/ppp2ppp/4pn2/8/2pP4/6P1/PP2PPBP/RNBQK1NR w KQkq - 0 5",
                "positionKey": "std:rnbqkb1r/ppp2ppp/4pn2/8/2pP4/6P1/PP2PPBP/RNBQK1NR w KQkq -",
                "sideToMove": "white",
                "similarityKind": "opening_context",
                "similarityScore": 0.72,
                "similarityKey": {"openingFamily": "Sicilian Defense", "pawnStructure": ["queenside_tension"]},
                "licenseNotice": "Local curated fixture metadata.",
                "attributionText": "Local curated retrieval fixture.",
            },
            {
                "retrievalId": "quality-wrong-material-class",
                "sourceId": "example-index",
                "sourceRef": "retrieval-example:quality-wrong-material-class",
                "exampleKind": "curated_reference",
                "fen": "4k3/8/8/8/8/8/4N3/4K3 w - - 0 1",
                "positionKey": "std:4k3/8/8/8/8/8/4N3/4K3 w - -",
                "sideToMove": "white",
                "similarityKind": "mixed_feature_context",
                "similarityScore": 0.72,
                "similarityKey": {"materialClass": "rook_pawn_vs_rook", "phaseContext": ["endgame"]},
                "licenseNotice": "Local curated fixture metadata.",
                "attributionText": "Local curated retrieval fixture.",
            },
            {
                "retrievalId": "quality-motif-without-carrier",
                "sourceId": "example-index",
                "sourceRef": "retrieval-example:quality-motif-without-carrier",
                "exampleKind": "puzzle_reference",
                "fen": "4k3/8/3r1r2/8/4N3/8/8/4K3 w - - 0 1",
                "positionKey": "std:4k3/8/3r1r2/8/4N3/8/8/4K3 w - -",
                "sideToMove": "white",
                "similarityKind": "motif_context",
                "similarityScore": 0.82,
                "similarityKey": {"motifTags": ["fork"]},
                "licenseNotice": "Local curated fixture metadata.",
                "attributionText": "Local curated retrieval fixture.",
            },
            {
                "retrievalId": "quality-coded-result-feature",
                "sourceId": "example-index",
                "sourceRef": "retrieval-example:quality-coded-result-feature",
                "exampleKind": "curated_reference",
                "fen": "4k3/8/8/8/8/8/4N3/4K3 w - - 0 1",
                "positionKey": "std:4k3/8/8/8/8/8/4N3/4K3 w - -",
                "sideToMove": "white",
                "similarityKind": "mixed_feature_context",
                "similarityScore": 0.72,
                "similarityKey": {"materialClass": "minor_piece", "phaseContext": ["endgame"]},
                "matchedFeatures": ["score:1-0"],
                "licenseNotice": "Local curated fixture metadata.",
                "attributionText": "Local curated retrieval fixture.",
            },
            {
                "retrievalId": "quality-current-position-feature",
                "sourceId": "example-index",
                "sourceRef": "retrieval-example:quality-current-position-feature",
                "exampleKind": "curated_reference",
                "fen": "4k3/8/8/8/8/8/4N3/4K3 w - - 0 1",
                "sideToMove": "white",
                "similarityKind": "mixed_feature_context",
                "similarityScore": 0.72,
                "similarityKey": {"materialClass": "minor_piece", "phaseContext": ["endgame"]},
                "matchedFeatures": ["current-position:reference"],
                "licenseNotice": "Local curated fixture metadata.",
                "attributionText": "Local curated retrieval fixture.",
            },
        ]
        for candidate in candidates:
            candidate.setdefault("positionKey", position_key_from_fen(candidate["fen"]))
            candidate.setdefault("sideToMove", side_to_move_from_fen(candidate["fen"]))
            candidate.setdefault("similarityKey", {"materialClass": "minor_piece", "phaseContext": ["endgame"]})

        with tempfile.TemporaryDirectory(dir=self.local_root()) as tmp:
            result = build_retrieval_quality_index(candidates, Path(tmp))

            index_rows = read_jsonl(result.index_path)
            reject_rows = read_jsonl(result.rejects_path)
            summary = read_json(result.summary_path)

            self.assertEqual(len(index_rows), 3)
            self.assertEqual(len(reject_rows), 17)
            self.assertEqual(summary["totalExamples"], 20)
            self.assertEqual(summary["acceptedExamples"], 3)
            self.assertEqual(summary["rejectedExamples"], 17)
            self.assertEqual(summary["duplicateSourceRefCount"], 1)
            self.assertEqual(summary["duplicateSimilarityKeyCount"], 1)
            self.assertEqual(summary["lowSimilarityCount"], 1)
            self.assertEqual(summary["qualityReport"]["qualityDecision"], "context_quality_pass")
            self.assertEqual(summary["qualityReport"]["highQualityExampleCount"], 3)
            self.assertEqual(summary["qualityReport"]["lowConfidenceSuppressedCount"], 17)
            self.assertEqual(summary["qualityReport"]["displayCandidateCount"], 0)
            self.assertEqual(summary["qualityReport"]["nastyNegativeResults"]["sameOpeningWrongStructureRejected"], 2)
            self.assertEqual(summary["qualityReport"]["nastyNegativeResults"]["motifWithoutExactCarrierRejected"], 1)
            self.assertEqual(summary["qualityReport"]["consumptionPolicy"]["retrievalOutranksMasterReference"], False)
            self.assertEqual(summary["qualityReport"]["validationRejectedByReason"]["motif_carrier_missing"], 1)
            self.assertEqual(summary["similarityKindSplit"]["exact_position"], 1)
            self.assertEqual(summary["similarityKindSplit"]["feature_context"], 2)
            self.assertEqual(summary["citationMetadataCompleteness"]["complete"], 1)
            self.assertEqual(summary["citationMetadataCompleteness"]["missing"], 1)
            self.assertEqual(
                summary["rejectedByReason"],
                {
                    "current_position_claim": 1,
                    "duplicate_similarity_key": 1,
                    "duplicate_source_ref": 1,
                    "low_similarity": 1,
                    "exact_position_key_missing": 1,
                    "missing_attribution_or_license": 1,
                    "missing_source_ref": 1,
                    "motif_carrier_missing": 1,
                    "opening_family_mismatch": 1,
                    "opening_side_or_material_mismatch": 1,
                    "similarity_material_mismatch": 1,
                    "similarity_side_to_move_mismatch": 1,
                    "truth_wording": 5,
                },
            )
            self.assertTrue(str(result.index_path).replace("\\", "/").endswith("/retrieval-index.jsonl"))
            self.assertTrue(str(result.rejects_path).replace("\\", "/").endswith("/retrieval-rejects.jsonl"))
            self.assertTrue(str(result.summary_path).replace("\\", "/").endswith("/retrieval-summary.json"))
            for row in index_rows:
                self.assertEqual(row["authority"], "retrieval_example")
                self.assertFalse(row["currentPositionClaim"])
                self.assertIn("licenseNotice", row)
                self.assertIn("attributionText", row)
                self.assertIn("positionKey", row["similarityKey"])
                self.assertIn("sideToMove", row["similarityKey"])
                self.assertIn("materialSignature", row["similarityKey"])
                self.assertNotIn("snippetText", row)
            exact_row = next(row for row in index_rows if row["retrievalId"] == "quality-exact-catalan")
            self.assertEqual(exact_row["similarityKey"]["materialSignature"], "opening_full_material")

    def test_rejects_output_outside_ignored_retrieval_root(self):
        with self.assertRaisesRegex(ValueError, "outside ignored retrieval local output root"):
            validate_output_root(Path.cwd() / "modules" / "commentary" / "src" / "test" / "resources")

    def test_committed_retrieval_reject_ledger_fails_closed(self):
        corpus = Path.cwd() / "modules" / "commentary" / "src" / "test" / "resources" / "commentary-corpus"
        accepted_examples = read_jsonl(corpus / "retrieval-examples.jsonl")
        reject_fixtures = read_jsonl(corpus / "retrieval-reject-fixtures.jsonl")

        with tempfile.TemporaryDirectory(dir=self.local_root()) as tmp:
            result = build_retrieval_quality_index(accepted_examples + reject_fixtures, Path(tmp))
            rejects = read_jsonl(result.rejects_path)
            summary = read_json(result.summary_path)

            self.assertEqual(summary["acceptedExamples"], len(accepted_examples))
            self.assertEqual(summary["rejectedExamples"], len(reject_fixtures))
            self.assertEqual(summary["totalExamples"], len(accepted_examples) + len(reject_fixtures))
            self.assertEqual(summary["qualityReport"]["displayCandidateCount"], 0)
            rejected_ids = {row["retrievalId"] for row in rejects}
            self.assertEqual(rejected_ids, {row["retrievalId"] for row in reject_fixtures})
            fixtures_by_id = {row["retrievalId"]: row["rejectReason"] for row in reject_fixtures}
            for row in rejects:
                self.assertEqual(row["fixtureRejectReason"], fixtures_by_id[row["retrievalId"]])
                self.assertEqual(row["reason"], fixtures_by_id[row["retrievalId"]])
                self.assertIn("validatorReason", row)


if __name__ == "__main__":
    unittest.main()

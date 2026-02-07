# Book Commentary Corpus Delta

- Baseline: `BookCommentaryCorpusReport.before.md`
- Current: `BookCommentaryCorpusReport.md`

## Summary Metrics

| Metric | Before | After |
|---|---:|---:|
| Avg quality score | 97.2 / 100 | 100.0 / 100 |
| Avg lexical diversity | 0.771 | 0.780 |
| Avg variation-anchor coverage | 1.000 | 1.000 |
| Low-quality cases | 0 | 0 |
| Advisory findings (non-blocking) | 15 across 13 cases | 0 across 0 cases |

## Cross-Case Repetition (Top 12)

### Before
- [7 cases] "the resulting position is fairly technical and calm"
- [6 cases] "it often simplifies into a manageable structure"
- [6 cases] "this tends to lead to a simpler position"
- [6 cases] "white is just a touch better"
- [5 cases] "the opening battle continues"
- [5 cases] "the position offers counterplay for both players"
- [4 cases] "both sides have their chances"
- [4 cases] "in practical terms white is more comfortable here"
- [4 cases] "the position is about level"
- [4 cases] "the position simplifies into an endgame"
- [4 cases] "this line reduces immediate tactical volatility"
- [3 cases] "keep an eye on the pawn on c4 1 attacker no defenders"

### After
- [5 cases] "from a practical standpoint white has the easier roadmap"
- [5 cases] "the position is about level"
- [4 cases] "white can press with a comparatively straightforward conversion scheme"
- [4 cases] "neither side has stabilized a lasting edge"
- [4 cases] "counterplay exists for both sides"
- [4 cases] "we are firmly in endgame territory"
- [4 cases] "white has the easier side to press with"
- [4 cases] "white is just a touch better"
- [4 cases] "precision is required in this endgame"
- [3 cases] "it s close to equal with play for both sides"
- [3 cases] "it drives the initiative"
- [3 cases] "plans and tactics start to bite"

## Notes
- This delta tracks non-blocking quality advisories in addition to pass/fail expectations.
- Run strict mode to fail the corpus when advisories remain: `--strict-quality`.

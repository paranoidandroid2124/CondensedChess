# Book Commentary Corpus Delta

- Baseline: `BookCommentaryCorpusReport.before.md`
- Current: `BookCommentaryCorpusReport.md`

## Summary Metrics

| Metric | Before | After |
|---|---:|---:|
| Results (passed cases) | 40 / 40 | 40 / 40 |
| Avg quality score | 97.2 / 100 | 99.3 / 100 |
| Avg lexical diversity | 0.771 | 0.763 |
| Avg variation-anchor coverage | 1.000 | 0.983 |
| Low-quality cases | 0 | 0 |
| Advisory findings (non-blocking) | 15 across 13 cases | 6 across 6 cases |

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
- [5 cases] "it drives the initiative"
- [5 cases] "the opening is already testing tactical accuracy"
- [4 cases] "piece activity outweighs broad strategic plans"
- [4 cases] "opening development is still in progress"
- [4 cases] "this is a concrete middlegame fight"
- [4 cases] "white has the easier game to play"
- [4 cases] "the evaluation is close enough that accuracy still matters most"
- [4 cases] "it improves the piece s scope"
- [4 cases] "a3 favors structural clarity and methodical handling over complications"
- [4 cases] "this is a technical endgame phase"
- [4 cases] "king activity and tempi become decisive"
- [4 cases] "there is no room for autopilot decisions"

## Notes
- This delta tracks non-blocking quality advisories in addition to pass/fail expectations.
- Current run resolves all corpus expectation failures (40/40 pass).
- Run strict mode to fail the corpus when advisories remain: `--strict-quality`.

# External Consumption And Audit Evidence

This file records the current-worktree evidence that the live `U-primary 18`,
the one live `U-attached` contract, and the live `Delta 2` pair do not stop at
internal witness tests only.

It does **not** claim planner, outline, renderer, API, or frontend integration.

## Public Consumption Boundary

The canonical external consumption boundary for the current worktree is:

- [CommentaryCore.scala](/C:/Codes/CondensedChess/lila-docker/repos/lila/modules/commentary/src/main/scala/lila/commentary/CommentaryCore.scala)

The public facade currently exposes:

- `activeUPrimaryDescriptorIds`
- `activeUAttachedDescriptorIds`
- `activeObjectFamilyIds`
- `extractUWitnesses(rootState)`
- `extractUWitnesses(fen)`
- `extractUWitnessesFromFen(String)`
- `extractUWitnessesFailClosed(fen)`
- `extractUWitnessesFromFenFailClosed(String)`
- `extractUAttachedWitnesses(rootState)`
- `extractUAttachedWitnesses(fen)`
- `extractUAttachedWitnessesFromFen(String)`
- `extractUAttachedWitnessesFailClosed(fen)`
- `extractUAttachedWitnessesFromFenFailClosed(String)`
- `extractStrategicObjects(rootState)`
- `extractStrategicObjects(fen)`
- `extractStrategicObjectsFromFen(String)`
- `extractStrategicObjectsFailClosed(fen)`
- `extractStrategicObjectsFromFenFailClosed(String)`
- `activeDeltaFamilyIds`
- `extractStrategicDeltas(beforeExtraction, afterExtraction, playedMove)`
- `extractStrategicDeltas(fenBefore, playedMove, fenAfter)`
- `extractStrategicDeltasFromFens(String, String, String)`
- `extractStrategicDeltasFailClosed(fenBefore, playedMove, fenAfter)`
- `extractStrategicDeltasFromFensFailClosed(String, String, String)`

This is the smallest honest public boundary above `root`, `witness/u`,
`object`, and `delta`.

The strategic-object overloads currently return a bundled
`StrategicObjectExtraction` value carrying `rootState`, the witness snapshots
used for objectization, and the extracted `objects`.

The strategic-delta overloads currently return a bundled
`StrategicDeltaExtraction` value carrying the before/after strategic-object
extractions, the played move, and the extracted `deltas`.

## External Consumer Proof

Durable external-consumption evidence is now carried by a tracked public-boundary
contract test:

- [CommentaryCoreBoundaryTest.scala](/C:/Codes/CondensedChess/lila-docker/repos/lila/modules/commentary/src/test/scala/lila/commentary/CommentaryCoreBoundaryTest.scala)

That artifact proves:

- a caller that only touches `lila.commentary.CommentaryCore` can consume the
  public `U` and strategic-object extraction facades without reaching into
  package-private runtime internals
- the tracked boundary test exercises the `RootStateVector`, `Fen.Full`,
  `String`, and fail-closed public overloads for all four public extraction
  facades
- a seeded exact position yields live `file_lane_state`,
  `rook_on_open_file_state`, and `diagonal_lane_only`
- closed-center and fixed-chain exact positions yield live
  `structural_space_claim`
- the public facade exports the frozen active `U-primary 18`, the one live
  `U-attached` descriptor id, the seven live strategic-object family ids, and
  the two live strategic-delta family ids
- a seeded exact position yields live `OpeningDevelopmentRegime` object
- a seeded exact move transition yields live `TradeCompressionCorridor` and
  `TradeInvariant` delta extraction
- the shell-only attached `10` rows remain outside standalone public/runtime
  descriptor registration and extraction, even though host vocabulary can still
  appear inside `structural_space_claim` payload
- fail-closed input discipline survives all public extraction facades

## Validation Commands

The following command was rerun for the attached-boundary extension:

```powershell
sbt "-Dsbt.server.autostart=false" "commentary/testOnly lila.commentary.CommentaryCoreBoundaryTest"
```

These commands are current-worktree evidence only. They are not a statement
about any other branch snapshot.

## Scope Guardrail

This evidence authorizes the following wording and no more:

- `U-primary 18` plus the one live `U-attached` contract are externally
  consumable through a stable commentary-module facade and are verified by a
  tracked public-boundary contract test
- the seven live strategic-object families are externally consumable through
  the same stable commentary-module facade and are verified by the same tracked
  public-boundary contract test
- the two live strategic-delta families are externally consumable through the
  same stable commentary-module facade and are verified by the same tracked
  public-boundary contract test
- the shell-only attached `10` rows remain shell-only and are not standalone
  public extracted truths

This evidence does **not** authorize:

- planner ownership claims
- strategy projection claims
- outline / renderer integration claims
- controller / API / frontend integration claims
- upper-layer completion claims

## Subagent Audit Record

Parallel audit was used for the following questions:

- smallest honest external consumption path beyond internal witness tests
- whether any existing non-test consumer already exists outside
  `modules/commentary`
- what persisted artifact shape best captures the audit conclusions

Their conclusions are recorded here only after local re-check against code and
docs.

Accepted conclusions:

- no live non-test controller/API/frontend consumer of commentary witness
  extraction was found
- the smallest honest external path is library-style JVM consumption of the
  public extractor boundary, not a pretend HTTP/UI surface
- persisted audit evidence should live in tracked repository documentation plus
  a tracked runnable artifact, not only in ephemeral chat output

Local corroboration for those audit conclusions lives at:

- [CommentaryCore.scala](/C:/Codes/CondensedChess/lila-docker/repos/lila/modules/commentary/src/main/scala/lila/commentary/CommentaryCore.scala)
- [CommentaryCoreBoundaryTest.scala](/C:/Codes/CondensedChess/lila-docker/repos/lila/modules/commentary/src/test/scala/lila/commentary/CommentaryCoreBoundaryTest.scala)
- [routes](/C:/Codes/CondensedChess/lila-docker/repos/lila/conf/routes:46)
- [Main.scala](/C:/Codes/CondensedChess/lila-docker/repos/lila/app/controllers/Main.scala:165)
- [Ops.scala](/C:/Codes/CondensedChess/lila-docker/repos/lila/app/controllers/Ops.scala:173)
- Removed legacy commentary controller path.
- [build.sbt](/C:/Codes/CondensedChess/lila-docker/repos/lila/build.sbt:163)

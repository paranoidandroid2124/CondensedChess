# Testing Guide 🧪

## Golden Master Tests (Regression)

We use "Golden Master" testing to ensure the chess analysis engine's output remains stable across refactors.

### 1. How to Run
Run the specific test suite via sbt:

```bash
sbt "testOnly chess.analysis.GoldenAnalysisTest"
```

### 2. First Run (Initialization)
If the golden JSON files do not exist (e.g. fresh checkout or new PGN added), the test will **automatically create them** and pass.
- **Check**: Look at `core/src/test/resources/golden/*.json`. These are now the "Single Source of Truth".

### 3. Subsequent Runs (Verification)
The test compares the engine's current output with the saved JSONs.
- **Pass**: Output matches exactly.
- **Fail**: Output differs. The error message will show equality failure.

### 4. Handling Changes (Approving New Output)
If you improved the engine (e.g. better critical moment detection) and the output *should* change:

1. Run with the update flag:
   ```bash
   # Windows (Powershell)
   $env:UPDATE_GOLDEN="true"; sbt "testOnly chess.analysis.GoldenAnalysisTest"
   
   # Mac/Linux
   UPDATE_GOLDEN=true sbt "testOnly chess.analysis.GoldenAnalysisTest"
   ```
2. This allows the test to **overwrite** the `.json` files with the new output.
3. **Diff the JSONs**: Use Git Diff to verify that the changes in `golden/*.json` are what you expected.
4. Commit the new golden files.

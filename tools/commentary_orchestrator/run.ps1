param(
  [Parameter(ValueFromRemainingArguments = $true)]
  [string[]]$Args
)

$script:PythonCandidates = [System.Collections.Generic.List[string]]::new()

$pythonCommand = Get-Command python -ErrorAction SilentlyContinue
if ($pythonCommand) {
  $script:PythonCandidates.Add($pythonCommand.Source)
}

$anacondaPackageRoots = @(
  "C:\Anaconda\pkgs"
)

foreach ($root in $anacondaPackageRoots) {
  if (-not (Test-Path $root)) {
    continue
  }
  Get-ChildItem $root -Directory -Filter "python-*" -ErrorAction SilentlyContinue |
    Sort-Object Name -Descending |
    ForEach-Object {
      $candidate = Join-Path $_.FullName "python.exe"
      if (Test-Path $candidate) {
        $script:PythonCandidates.Add($candidate)
      }
    }
}

$pythonExe = $null
foreach ($candidate in ($script:PythonCandidates | Select-Object -Unique)) {
  $probe = & $candidate -c "import json; print('ok')" 2>$null
  if ($LASTEXITCODE -eq 0 -and $probe -eq "ok") {
    $pythonExe = $candidate
    break
  }
}

if (-not $pythonExe) {
  throw "No working python.exe found for commentary_orchestrator."
}

$orchestrator = Join-Path $PSScriptRoot "orchestrator.py"
& $pythonExe $orchestrator @Args
exit $LASTEXITCODE

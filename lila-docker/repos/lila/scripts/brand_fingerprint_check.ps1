[CmdletBinding()]
param(
  [string]$RepoRoot = "."
)

$ErrorActionPreference = "Stop"

$resolvedRoot = Resolve-Path $RepoRoot
Set-Location $resolvedRoot

$targets = @(
  "app/views",
  "modules/web/src/main",
  "ui"
)

$commonGlobs = @(
  "--glob", "!**/test/**",
  "--glob", "!**/tests/**",
  "--glob", "!**/target/**",
  "--glob", "!**/dist/**",
  "--glob", "!**/docs/**",
  "--glob", "!**/COPYING.md"
)

$forbidden = @(
  @{ label = "header#top"; regex = 'header\s*#top|id\s*:=\s*"top"|id="top"' },
  @{ label = ".site-title-nav"; regex = 'site-title-nav' },
  @{ label = ".topnav-toggle"; regex = 'topnav-toggle' },
  @{ label = ".hbg"; regex = '(^|[^a-zA-Z0-9_-])hbg([^a-zA-Z0-9_-]|$)' },
  @{ label = "window.lichess"; regex = 'window\.lichess|globals\.lichess' },
  @{ label = "application/web.lichess+json"; regex = 'application/web\.lichess\+json' },
  @{ label = "explorer.lichess.ovh"; regex = 'explorer\.lichess\.ovh' },
  @{ label = "LichessDotOrg"; regex = 'LichessDotOrg' }
)

$totalHits = 0

Write-Host "== Brand fingerprint check =="
Write-Host "Root: $resolvedRoot"
Write-Host "Targets: $($targets -join ', ')"
Write-Host ""

foreach ($entry in $forbidden) {
  $label = $entry.label
  $regex = $entry.regex

  $args = @("-n", "--color", "never") + $commonGlobs + @($regex) + $targets
  $hits = @(& rg @args 2>$null)

  if ($LASTEXITCODE -eq 0 -and $hits.Count -gt 0) {
    $totalHits += $hits.Count
    Write-Host "[FAIL] $label ($($hits.Count))"
    $hits | ForEach-Object { Write-Host "  $_" }
  } else {
    Write-Host "[OK]   $label"
  }
}

Write-Host ""
if ($totalHits -gt 0) {
  Write-Host "Brand fingerprint check failed. Total hits: $totalHits"
  exit 1
}

Write-Host "Brand fingerprint check passed."
exit 0

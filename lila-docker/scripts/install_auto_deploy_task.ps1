[CmdletBinding()]
param(
    [string]$TaskName = "ChesstoryAutoDeploy",
    [int]$IntervalMinutes = 10,
    [string]$Branch = "master",
    [string]$HealthUrl = "http://localhost:8080/",
    [switch]$RunAsSystem,
    [switch]$SkipTests
)

$ErrorActionPreference = "Stop"

if ($IntervalMinutes -lt 1) {
    throw "IntervalMinutes must be >= 1"
}

$scriptPath = (Resolve-Path (Join-Path $PSScriptRoot "ops_auto_deploy.ps1")).Path
$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path

$taskCommand = "powershell.exe -NoProfile -ExecutionPolicy Bypass -File `"$scriptPath`" -RepoRoot `"$repoRoot`" -Branch `"$Branch`" -HealthUrl `"$HealthUrl`""
if ($SkipTests) {
    $taskCommand += " -SkipTests"
}

$createArgs = @(
    "/Create",
    "/TN", $TaskName,
    "/SC", "MINUTE",
    "/MO", $IntervalMinutes,
    "/TR", $taskCommand,
    "/F"
)

if ($RunAsSystem) {
    $createArgs += @("/RU", "SYSTEM")
}

Write-Host "Registering task with schtasks..."
& schtasks.exe @createArgs | Out-Host
if ($LASTEXITCODE -ne 0) {
    throw "Failed to register scheduled task: $TaskName"
}

Write-Host "Installed scheduled task: $TaskName"
Write-Host "Interval: every $IntervalMinutes minute(s)"
Write-Host "Execute once now: schtasks /Run /TN `"$TaskName`""

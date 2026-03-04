[CmdletBinding()]
param(
    [string]$TaskName = "ChesstoryAutoDeploy"
)

$ErrorActionPreference = "Stop"

& schtasks.exe /Query /TN $TaskName | Out-Null
if ($LASTEXITCODE -eq 0) {
    & schtasks.exe /Delete /TN $TaskName /F | Out-Host
    if ($LASTEXITCODE -ne 0) {
        throw "Failed to remove task: $TaskName"
    }
    Write-Host "Removed scheduled task: $TaskName"
} else {
    Write-Host "Task not found: $TaskName"
}

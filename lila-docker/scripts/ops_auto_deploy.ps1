[CmdletBinding()]
param(
    [string]$RepoRoot = "",
    [string]$SettingsEnvPath = "",
    [string]$Branch = "master",
    [string]$HealthUrl = "http://localhost:8080/",
    [int]$HealthTimeoutSec = 180,
    [switch]$SkipTests
)

$ErrorActionPreference = "Stop"

if ([string]::IsNullOrWhiteSpace($RepoRoot)) {
    $scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
    $RepoRoot = (Resolve-Path (Join-Path $scriptDir "..\..")).Path
}

if ([string]::IsNullOrWhiteSpace($SettingsEnvPath)) {
    $SettingsEnvPath = Join-Path $RepoRoot "lila-docker\settings.env"
}

function Invoke-LoggedCommand {
    param(
        [Parameter(Mandatory = $true)][string]$FilePath,
        [Parameter(Mandatory = $true)][string[]]$Arguments,
        [string]$WorkingDirectory = $RepoRoot
    )

    Write-Host ">> $FilePath $($Arguments -join ' ')"
    & $FilePath @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "Command failed ($LASTEXITCODE): $FilePath $($Arguments -join ' ')"
    }
}

function Wait-ForHealthyService {
    param(
        [Parameter(Mandatory = $true)][string]$Url,
        [Parameter(Mandatory = $true)][int]$TimeoutSec
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSec)
    while ((Get-Date) -lt $deadline) {
        try {
            $res = Invoke-WebRequest -Uri $Url -TimeoutSec 5 -UseBasicParsing
            if ($res.StatusCode -ge 200 -and $res.StatusCode -lt 500) {
                Write-Host "Healthcheck OK: $($res.StatusCode) $Url"
                return
            }
        } catch {
            # retry
        }
        Start-Sleep -Seconds 5
    }
    throw "Healthcheck timeout after ${TimeoutSec}s: $Url"
}

function Import-EnvFile {
    param([Parameter(Mandatory = $true)][string]$Path)

    if (-not (Test-Path $Path)) {
        Write-Warning "Env file not found: $Path"
        return
    }

    Write-Host "Loading env file: $Path"
    foreach ($rawLine in Get-Content $Path) {
        $line = $rawLine.Trim()
        if ([string]::IsNullOrWhiteSpace($line) -or $line.StartsWith("#")) {
            continue
        }

        $m = [regex]::Match($line, "^\s*([A-Za-z_][A-Za-z0-9_]*)\s*=\s*(.*)\s*$")
        if (-not $m.Success) {
            continue
        }

        $name = $m.Groups[1].Value
        $value = $m.Groups[2].Value
        if (($value.StartsWith('"') -and $value.EndsWith('"')) -or ($value.StartsWith("'") -and $value.EndsWith("'"))) {
            if ($value.Length -ge 2) {
                $value = $value.Substring(1, $value.Length - 2)
            }
        }
        [System.Environment]::SetEnvironmentVariable($name, $value, "Process")
    }
}

$dockerRoot = Join-Path $RepoRoot "lila-docker"
$lilaRoot = Join-Path $dockerRoot "repos\lila"
$composeEnvPath = Join-Path $dockerRoot ".env"
$opsLogDir = Join-Path $dockerRoot "logs\ops"
$lockPath = Join-Path $opsLogDir "autodeploy.lock"
$resultCode = 0

New-Item -ItemType Directory -Force -Path $opsLogDir | Out-Null
$logName = "autodeploy-{0}.log" -f (Get-Date -Format "yyyyMMdd-HHmmss")
$logPath = Join-Path $opsLogDir $logName

$lockAcquired = $false
try {
    Start-Transcript -Path $logPath | Out-Null

    if (Test-Path $lockPath) {
        Write-Warning "Lock file exists ($lockPath). Another deploy is in progress. Skipping."
        return
    }

    Set-Content -Path $lockPath -Value "$PID $(Get-Date -Format o)"
    $lockAcquired = $true

    Push-Location $RepoRoot

    # Keep docker compose interpolation consistent with existing local setup.
    Import-EnvFile -Path $composeEnvPath
    Import-EnvFile -Path $SettingsEnvPath

    $dirty = git status --porcelain
    if ($LASTEXITCODE -ne 0) {
        throw "Failed to check git status at $RepoRoot"
    }
    if ($dirty) {
        Write-Warning "Working tree is dirty. Auto deploy skipped to avoid conflicts."
        return
    }

    $before = (git rev-parse HEAD).Trim()
    if ($LASTEXITCODE -ne 0) {
        throw "Failed to read current commit SHA."
    }

    Invoke-LoggedCommand -FilePath "git" -Arguments @("fetch", "origin", $Branch, "--prune")
    Invoke-LoggedCommand -FilePath "git" -Arguments @("pull", "--ff-only", "origin", $Branch)

    $after = (git rev-parse HEAD).Trim()
    if ($LASTEXITCODE -ne 0) {
        throw "Failed to read updated commit SHA."
    }

    if ($before -eq $after) {
        Write-Host "No upstream change detected ($after)."
        return
    }

    Write-Host "Updated: $before -> $after"
    Write-Host "Changed files:"
    Invoke-LoggedCommand -FilePath "git" -Arguments @("diff", "--name-only", $before, $after)

    if (-not $SkipTests) {
        Write-Host "Running pre-deploy validation: sbt test:compile"
        Invoke-LoggedCommand -FilePath "sbt" -Arguments @("test:compile") -WorkingDirectory $lilaRoot
    } else {
        Write-Host "SkipTests enabled. Validation step skipped."
    }

    Write-Host "Rebuilding and restarting services with docker compose profile=base"
    Invoke-LoggedCommand -FilePath "docker" -Arguments @("compose", "--profile", "base", "up", "-d", "--build") -WorkingDirectory $dockerRoot

    Wait-ForHealthyService -Url $HealthUrl -TimeoutSec $HealthTimeoutSec
    Write-Host "Auto deploy completed successfully."
}
catch {
    Write-Error $_
    $resultCode = 1
}
finally {
    if (Get-Location) {
        Pop-Location -ErrorAction SilentlyContinue
    }
    if ($lockAcquired -and (Test-Path $lockPath)) {
        Remove-Item -Path $lockPath -Force -ErrorAction SilentlyContinue
    }
    Stop-Transcript | Out-Null
}

if ($resultCode -ne 0) {
    exit $resultCode
}

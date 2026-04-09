# =====================================================
#  HostelHub - Run a single backend service via PowerShell
#  Usage: .\run-service.ps1 user-service
#         .\run-service.ps1 room-service
#         .\run-service.ps1 booking-service
#         .\run-service.ps1 notification-service
#         .\run-service.ps1 api-gateway
# =====================================================

param(
    [Parameter(Mandatory=$true)]
    [ValidateSet("user-service","room-service","booking-service","notification-service","api-gateway")]
    [string]$ServiceName
)

$env:JAVA_HOME = "C:\Program Files\Java\jdk-18.0.2"
$MAVEN_HOME = Join-Path $PSScriptRoot "..\tools\apache-maven-3.9.6"
$env:PATH = "$MAVEN_HOME\bin;$($env:JAVA_HOME)\bin;$($env:PATH)"

$mvnCmd = Join-Path $MAVEN_HOME "bin\mvn.cmd"

if (-not (Test-Path $mvnCmd)) {
    Write-Host "ERROR: Maven not found at $mvnCmd" -ForegroundColor Red
    Write-Host "Run the Maven download first." -ForegroundColor Yellow
    exit 1
}

$serviceDir = Join-Path $PSScriptRoot $ServiceName

if (-not (Test-Path $serviceDir)) {
    Write-Host "ERROR: Service directory not found: $serviceDir" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "============================================" -ForegroundColor Cyan
Write-Host "  Starting $ServiceName" -ForegroundColor Cyan
Write-Host "  JAVA_HOME = $($env:JAVA_HOME)" -ForegroundColor Gray
Write-Host "  MAVEN     = $MAVEN_HOME" -ForegroundColor Gray
Write-Host "============================================" -ForegroundColor Cyan
Write-Host ""

Push-Location $serviceDir
& $mvnCmd spring-boot:run
Pop-Location

<#
    Bring up the full Game Item Trading Platform with Docker.
    Requires only Docker Desktop (no local Maven/JDK/Node needed — every
    service builds inside its container).

    Usage:
        ./scripts/run-stack.ps1            # build + start everything
        ./scripts/run-stack.ps1 -Seed      # also load 100K+ SKU seed data
#>
param(
    [switch]$Seed
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    Write-Error "Docker is not installed. Install Docker Desktop: https://www.docker.com/products/docker-desktop/"
    exit 1
}

Write-Host "Building and starting the stack (first run downloads images and compiles services)..." -ForegroundColor Cyan
docker compose up -d --build

Write-Host "Waiting for the gateway to become healthy..." -ForegroundColor Cyan
$ok = $false
for ($i = 0; $i -lt 60; $i++) {
    try {
        $r = Invoke-RestMethod -Uri "http://localhost:8080/actuator/health" -TimeoutSec 3
        if ($r.status -eq "UP") { $ok = $true; break }
    } catch { Start-Sleep -Seconds 5 }
}
if (-not $ok) { Write-Warning "Gateway did not report healthy yet; services may still be starting." }

if ($Seed) {
    Write-Host "Generating and loading 100K+ SKU seed data..." -ForegroundColor Cyan
    python scripts/seed/generate_seed.py --skus 100000
    Get-Content scripts/db/seed-items.sql | docker compose exec -T mysql mysql -uroot -proot
}

Write-Host "`nStack is up:" -ForegroundColor Green
Write-Host "  Frontend    http://localhost:3001"
Write-Host "  Gateway API http://localhost:8080"
Write-Host "  AI assistant http://localhost:8087"
Write-Host "  Prometheus  http://localhost:9090"
Write-Host "  Grafana     http://localhost:3000  (admin/admin)"
Write-Host "  RabbitMQ UI http://localhost:15672 (guest/guest)"
Write-Host "  Nacos       http://localhost:8848/nacos"
Write-Host "`nStop with: ./scripts/stop-stack.ps1"

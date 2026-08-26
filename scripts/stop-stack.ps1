<#
    Stop the stack. Add -Volumes to also delete database/volume data.
        ./scripts/stop-stack.ps1
        ./scripts/stop-stack.ps1 -Volumes
#>
param(
    [switch]$Volumes
)

$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

if ($Volumes) {
    docker compose down -v
} else {
    docker compose down
}

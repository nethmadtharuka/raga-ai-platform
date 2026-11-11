<#
.SYNOPSIS
  Runs the RAGA Spring Boot backend locally with the settings from the root .env file.

.DESCRIPTION
  Spring Boot does not read .env files. This script loads .env into this process only,
  then applies the local-development overrides:
    - CHROMA_URL  -> http://localhost:8000 (.env holds the Docker-network address http://chromadb:8000)
    - SERVER_PORT -> 8090 (8080 is often taken by other local servers)
    - SPRING_PROFILES_ACTIVE is cleared (the "docker" profile is for the container)
  Key values are never printed.

.EXAMPLE
  powershell -ExecutionPolicy Bypass -File .\scripts\dev-backend.ps1
  powershell -ExecutionPolicy Bypass -File .\scripts\dev-backend.ps1 -Port 8091
#>
param(
    [int]$Port = 8090,
    [string]$ChromaUrl = 'http://localhost:8000'
)
$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$envFile = Join-Path $root '.env'
if (-not (Test-Path $envFile)) { throw "Missing $envFile. Copy .env.example to .env and fill in the values." }

foreach ($line in Get-Content $envFile) {
    if ($line -match '^\s*([A-Za-z_][A-Za-z0-9_]*)\s*=\s*(.*?)\s*$') {
        $value = $Matches[2] -replace '^["'']|["'']$', ''
        Set-Item -Path "env:$($Matches[1])" -Value $value
    }
}

$env:CHROMA_URL = $ChromaUrl
$env:SERVER_PORT = "$Port"
Remove-Item env:SPRING_PROFILES_ACTIVE -ErrorAction SilentlyContinue

foreach ($name in 'GEMINI_API_KEY', 'APP_API_KEY') {
    if (-not [Environment]::GetEnvironmentVariable($name)) { throw "$name is empty in .env" }
}
if (Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue) {
    throw "Port $Port is already in use. Pick another one with -Port and set RAGA_API_TARGET for the frontend."
}

Write-Host "RAGA backend -> http://localhost:$Port  (ChromaDB: $ChromaUrl, model: $(if ($env:GEMINI_MODEL) { $env:GEMINI_MODEL } else { 'default' }))"
Set-Location $root
& .\mvnw.cmd -B spring-boot:run

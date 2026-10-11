$ErrorActionPreference='Stop'
$projectRoot=Split-Path -Parent $PSScriptRoot
Set-Location -LiteralPath $projectRoot
New-Item -ItemType Directory -Path '.runtime\jdk' -Force | Out-Null
$jdk=Get-ChildItem -LiteralPath '.runtime\jdk' -Directory | Where-Object { Test-Path -LiteralPath (Join-Path $_.FullName 'bin\java.exe') } | Select-Object -First 1
if (!$jdk) {
 Write-Output 'Downloading portable Microsoft OpenJDK 17...'
 Invoke-WebRequest -Uri 'https://aka.ms/download-jdk/microsoft-jdk-17-windows-x64.zip' -OutFile '.runtime\jdk17.zip' -TimeoutSec 300
 Expand-Archive -LiteralPath '.runtime\jdk17.zip' -DestinationPath '.runtime\jdk' -Force
}
Write-Output 'Portable JDK is ready. Run scripts/build.ps1, then scripts/start.ps1.'

param([switch]$Offline,[switch]$Clean)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
Set-Location -LiteralPath $projectRoot
$jdk = Get-ChildItem -LiteralPath "$projectRoot\.runtime\jdk" -Directory -ErrorAction SilentlyContinue | Where-Object { Test-Path -LiteralPath (Join-Path $_.FullName 'bin\java.exe') } | Select-Object -First 1
if ($jdk) { $env:JAVA_HOME = $jdk.FullName; $env:PATH = "$env:JAVA_HOME\bin;$env:PATH" }
$env:JAVA_TOOL_OPTIONS = '-Dfile.encoding=UTF-8'
$mavenArgs = @('-Dfile.encoding=UTF-8','-DskipTests','package')
if ($Clean) { $mavenArgs = @('clean') + $mavenArgs }
if ($Offline) { $mavenArgs = @('-o') + $mavenArgs }
if (Test-Path -LiteralPath "$projectRoot\.runtime\settings.xml") { $mavenArgs = @('-gs',"$projectRoot\.runtime\settings.xml",'-s',"$projectRoot\.runtime\settings.xml") + $mavenArgs }
& mvn @mavenArgs
if ($LASTEXITCODE -ne 0) { throw 'Maven build failed.' }

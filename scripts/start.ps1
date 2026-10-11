param([switch]$Build)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
Set-Location -LiteralPath $projectRoot
if ($Build) { & "$PSScriptRoot\build.ps1" }
New-Item -ItemType Directory -Path "$projectRoot\.runtime","$projectRoot\.data" -Force | Out-Null
if (Test-Path -LiteralPath "$projectRoot\.runtime\processes.json") { throw 'A launch record already exists. Run scripts/stop.ps1 before starting again.' }
$jdk = Get-ChildItem -LiteralPath "$projectRoot\.runtime\jdk" -Directory -ErrorAction SilentlyContinue | Where-Object { Test-Path -LiteralPath (Join-Path $_.FullName 'bin\java.exe') } | Select-Object -First 1
$javaExe = if ($jdk) { Join-Path $jdk.FullName 'bin\java.exe' } elseif ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME 'bin\java.exe' } else { (Get-Command java).Source }
$services = @(@{Name='user-service';Port=8081},@{Name='item-service';Port=8082},@{Name='cart-service';Port=8083},@{Name='trade-service';Port=8084},@{Name='pay-service';Port=8085},@{Name='hm-gateway';Port=8080})
$records = @()
try {
 foreach ($service in $services) {
  $listener = [System.Net.Sockets.TcpClient]::new()
  try { $listener.Connect('127.0.0.1',$service.Port); throw "Port $($service.Port) is occupied." } catch [System.Net.Sockets.SocketException] { } finally { $listener.Dispose() }
  $jar = Join-Path $projectRoot "$($service.Name)\target\$($service.Name)-1.0.0.jar"
  if (!(Test-Path -LiteralPath $jar)) { throw "Missing $jar. Run scripts/build.ps1 first." }
  $process = Start-Process -FilePath $javaExe -ArgumentList @('-Xms48m','-Xmx192m','-Dfile.encoding=UTF-8','-jar',"`"$jar`"",'--spring.config.name=outdoor') -WorkingDirectory $projectRoot -WindowStyle Hidden -PassThru -RedirectStandardOutput "$projectRoot\.runtime\$($service.Name).out.log" -RedirectStandardError "$projectRoot\.runtime\$($service.Name).err.log"
  $records += @{Name=$service.Name;Pid=$process.Id;StartedAt=$process.StartTime.ToUniversalTime().ToString('o')}
 }
 $records | ConvertTo-Json | Set-Content -LiteralPath "$projectRoot\.runtime\processes.json" -Encoding utf8
 $deadline = [DateTime]::UtcNow.AddSeconds(90)
 foreach ($service in $services) {
  $ready = $false
  while ([DateTime]::UtcNow -lt $deadline) {
   try { $health = Invoke-RestMethod -Uri "http://127.0.0.1:$($service.Port)/health" -TimeoutSec 2; if ($health.status -eq 'UP') { $ready=$true; break } } catch { }
   Start-Sleep -Milliseconds 700
  }
  if (!$ready) { throw "$($service.Name) did not become ready. Check .runtime logs." }
  Write-Output "$($service.Name): ready"
 }
 Write-Output 'User: http://127.0.0.1:8080/  Admin: http://127.0.0.1:8080/admin/'
} catch {
 foreach ($record in $records) { $p = Get-Process -Id $record.Pid -ErrorAction SilentlyContinue; if ($p) { Stop-Process -Id $p.Id } }
 if (Test-Path -LiteralPath "$projectRoot\.runtime\processes.json") { Remove-Item -LiteralPath "$projectRoot\.runtime\processes.json" }
 throw
}

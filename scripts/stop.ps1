$ErrorActionPreference='Stop'
$projectRoot=Split-Path -Parent $PSScriptRoot
$recordPath=Join-Path $projectRoot '.runtime\processes.json'
if (!(Test-Path -LiteralPath $recordPath)) { Write-Output 'No launch record.'; return }
foreach ($record in (Get-Content -LiteralPath $recordPath -Raw | ConvertFrom-Json)) {
 $process=Get-Process -Id $record.Pid -ErrorAction SilentlyContinue
 if ($process -and $process.ProcessName -eq 'java' -and [Math]::Abs(($process.StartTime.ToUniversalTime() - ([DateTime]$record.StartedAt).ToUniversalTime()).TotalSeconds) -lt 1) { Stop-Process -Id $process.Id }
}
Remove-Item -LiteralPath $recordPath
Write-Output 'Project processes stopped; database files preserved.'

param([Parameter(Mandatory=$true)][string]$NginxRoot)
$ErrorActionPreference='Stop'
$projectRoot=Split-Path -Parent $PSScriptRoot
$destinationRoot=(Resolve-Path -LiteralPath $NginxRoot).Path
if (!(Test-Path -LiteralPath (Join-Path $destinationRoot 'conf\nginx.conf'))) { throw 'The destination is not the expected Nginx project.' }
$portal=Join-Path $destinationRoot 'html\hmall-portal'
$admin=Join-Path $destinationRoot 'html\hmall-admin'
New-Item -ItemType Directory -Path "$portal\assets","$admin\assets" -Force | Out-Null
Copy-Item -LiteralPath "$projectRoot\frontend\index.html" -Destination "$portal\index.html" -Force
Copy-Item -LiteralPath "$projectRoot\frontend\admin\index.html" -Destination "$admin\index.html" -Force
foreach ($asset in (Get-ChildItem -LiteralPath "$projectRoot\frontend\assets" -File)) {
 Copy-Item -LiteralPath $asset.FullName -Destination (Join-Path "$portal\assets" $asset.Name) -Force
 Copy-Item -LiteralPath $asset.FullName -Destination (Join-Path "$admin\assets" $asset.Name) -Force
}
$configPath=Join-Path $destinationRoot 'conf\nginx.conf'
$config=[System.IO.File]::ReadAllText($configPath)
$config=$config.Replace('root html/hm-refresh-admin;','root html/hmall-admin;')
[System.IO.File]::WriteAllText($configPath,$config,(New-Object System.Text.UTF8Encoding($false)))
Write-Output 'New storefront, admin and local assets synchronized. Existing assets preserved.'

# verify_pack.ps1 - install the freshly built jar into the modpack and verify it on a real launch.
#
# Why this exists: some bugs only show up in the modpack.  A mixin that targets a jar-in-jar
# "gamelibrary" (Registrate arrives inside CMM's jar) is applied in dev but NOT in the pack, so a
# dev self-test can pass while the pack destroys belts.  This script therefore launches the pack
# and reads its log instead of trusting the dev run.
#
# Usage: powershell -NoProfile -ExecutionPolicy Bypass -File tools\verify_pack.ps1
param(
    [string]$Jar     = 'C:\Users\George\Documents\cmm-plus\build\libs\CMMPlus-1.21.1-1.0.0.jar',
    [string]$PackMods = 'D:\PCL2\.minecraft\versions\重度手搓症状\mods',
    [string]$LogPath = 'D:\PCL2\.minecraft\versions\重度手搓症状\logs\latest.log',
    [string]$Launcher = 'D:\PCL2\PCL\LatestLaunch.bat',
    [int]$TimeoutSeconds = 300
)
$ErrorActionPreference = 'Continue'

$target = Join-Path $PackMods (Split-Path $Jar -Leaf)
$before = if (Test-Path $LogPath) { (Get-Item $LogPath).LastWriteTime } else { [datetime]::MinValue }

# 1. install, refusing to touch a jar the game still holds
try {
    $fs = [System.IO.File]::Open($target, 'Open', 'ReadWrite', 'None'); $fs.Close()
} catch {
    Write-Host "the game is holding $target - close it and rerun"; exit 2
}
if (Test-Path $target) { Remove-Item $target -Force }
Copy-Item $Jar $target -Force
$installed = Get-Item $target
$hash = (Get-FileHash $target -Algorithm SHA256).Hash
Write-Host ("installed {0}  {1} bytes  sha256 {2}" -f $installed.Name, $installed.Length, $hash)
Write-Host ("matches build: {0}" -f ($hash -eq (Get-FileHash $Jar -Algorithm SHA256).Hash))

# 2. launch the pack
Start-Process -FilePath $Launcher -WorkingDirectory (Split-Path $Launcher) -WindowStyle Minimized
Write-Host "launched; waiting up to $TimeoutSeconds s for the log to move past $before"

$deadline = (Get-Date).AddSeconds($TimeoutSeconds)
while ((Get-Date) -lt $deadline) {
    Start-Sleep -Seconds 10
    if (Test-Path $LogPath) {
        $now = (Get-Item $LogPath).LastWriteTime
        if ($now -gt $before -and (Get-Content $LogPath -Tail 1) -match 'Setting user|Loaded|Reloading ResourceManager') { break }
    }
}
if (-not (Test-Path $LogPath) -or (Get-Item $LogPath).LastWriteTime -le $before) {
    Write-Host "log never advanced - the launch did not reach the game; nothing can be concluded"
    exit 3
}

# 3. read what the launch actually did
$log = Get-Content $LogPath
Write-Host ''
Write-Host "--- log grew to $($log.Count) lines ---"
foreach ($pattern in @(
        'CMMPlus',
        'cmmplus\.mixins|BeltBlockMixin|BeltSlicerMixin|BeltBlockEntityMixin',
        'Mixin apply failed|InvalidInjectionException|CRITICAL|Unable to load mixin',
        'cmmplus:block/belt|missing model|Unable to load model',
        'Exception|Caused by')) {
    $hits = $log | Select-String -Pattern $pattern | Select-Object -First 6
    Write-Host ''
    Write-Host "== $pattern  ($($hits.Count) shown)"
    foreach ($h in $hits) { Write-Host ('   ' + $h.Line.Trim()) }
}

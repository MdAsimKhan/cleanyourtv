# Windows: build Bing Wallpaper add-on and install it on the TV.
#
# Usage: addons\bing-wallpaper\build-and-install.ps1 -TvHost 192.168.1.33
#        addons\bing-wallpaper\build-and-install.ps1 -TvHost 192.168.1.33 -Uninstall
param(
    [string]$TvHost = $env:TV_HOST,
    [switch]$Uninstall
)
$ErrorActionPreference = 'Stop'
if (-not $TvHost) { throw 'pass -TvHost <tv ip> or set $env:TV_HOST' }
$env:TV_HOST = $TvHost
$here = $PSScriptRoot
$root = Split-Path (Split-Path $here -Parent) -Parent
$tools = Join-Path $root 'tools'
$py = Join-Path $tools 'venv\Scripts\python.exe'
if (-not (Test-Path $py)) { throw 'run: python -m venv tools\venv; tools\venv\Scripts\python.exe -m pip install -r tools\requirements.txt' }
$pkg = 'com.gathin.bingwallpaper'
function Tv([string]$cmd) { & $py -I (Join-Path $tools 'tv.py') $cmd; if ($LASTEXITCODE) { throw "tv.py failed: $cmd" } }

if ($Uninstall) {
    Tv "pm uninstall $pkg"
    Write-Host "Uninstalled $pkg"
    exit 0
}

if (-not $env:JAVA_HOME -or -not (Test-Path "$env:JAVA_HOME\bin\java.exe")) {
    $jbr = Get-ChildItem 'C:\Program Files\Android' -Directory -ErrorAction SilentlyContinue |
        ForEach-Object { Join-Path $_.FullName 'jbr' } | Where-Object { Test-Path "$_\bin\java.exe" } | Select-Object -First 1
    if ($jbr) { $env:JAVA_HOME = $jbr }
}
if (-not $env:ANDROID_HOME) { $env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk" }

Push-Location $here
try {
    & .\gradlew.bat assembleRelease -q
    if ($LASTEXITCODE) { throw 'build failed (JDK 17 and Android SDK 34 needed)' }
} finally { Pop-Location }

$apk = Join-Path $here 'app\build\outputs\apk\release\app-release.apk'
& $py -I (Join-Path $tools 'b64push.py') $apk /data/local/tmp/bing-wallpaper.apk | Out-Null
if ($LASTEXITCODE) { throw 'sending the APK failed' }
Tv 'pm install -r /data/local/tmp/bing-wallpaper.apk; rm -f /data/local/tmp/bing-wallpaper.apk'
Tv "pm grant $pkg android.permission.WRITE_SECURE_SETTINGS 2>/dev/null; true" | Out-Null
Tv "appops set $pkg AUTO_START allow 2>/dev/null; appops set $pkg APP_AUTO_START allow 2>/dev/null; appops write-settings 2>/dev/null; true" | Out-Null
Write-Host "Installed $pkg and granted permissions."

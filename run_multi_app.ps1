$ErrorActionPreference = "Stop"

# Use local.properties to find SDK
$LocalProps = ".\local.properties"
$SdkDir = "C:\Users\admin\AppData\Local\Android\Sdk"
if (Test-Path $LocalProps) {
    Get-Content $LocalProps | ForEach-Object {
        if ($_ -match "sdk\.dir=(.*)") {
            $SdkDir = $matches[1].Replace("\:", ":").Replace("\\", "\")
        }
    }
}
$AdbExe = Join-Path $SdkDir "platform-tools\adb.exe"

Write-Host "Building APK..."
.\gradlew.bat assembleDebug

if ($LASTEXITCODE -ne 0) {
    Write-Error "Build Failed!"
    exit 1
}

$ApkPath = "app\build\outputs\apk\debug\app-debug.apk"
if (-not (Test-Path $ApkPath)) {
    Write-Error "APK not found at $ApkPath"
    exit 1
}

Write-Host "Detecting devices..."
$Devices = & $AdbExe devices | Select-String -Pattern "device$" | ForEach-Object { $_.ToString().Split()[0] }

if ($Devices.Count -eq 0) {
    Write-Warning "No devices found."
    exit
}

foreach ($Device in $Devices) {
    if ($Device -match "emulator") {
        Write-Host "Installing to $Device..."
        & $AdbExe -s $Device install -r $ApkPath
        
        Write-Host "Launching on $Device..."
        & $AdbExe -s $Device shell am start -n com.example.roomiesplit/.MainActivity
    }
}

Write-Host "Done!"

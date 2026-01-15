# Read sdk.dir from local.properties
$LocalProps = ".\local.properties"
$SdkDir = "C:\Users\admin\AppData\Local\Android\Sdk" # Default fallback

if (Test-Path $LocalProps) {
    Get-Content $LocalProps | ForEach-Object {
        if ($_ -match "sdk\.dir=(.*)") {
            $SdkDir = $matches[1].Replace("\:", ":").Replace("\\", "\")
        }
    }
}
Write-Host "Using Android SDK: $SdkDir"
$AdbExe = Join-Path $SdkDir "platform-tools\adb.exe"

# Add platform-tools to PATH locally for this session
$Env:Path += ";$SdkDir\platform-tools"

if (-not (Test-Path $AdbExe)) {
    Write-Warning "ADB not found at $AdbExe. Installation might fail."
    exit 1
}

Write-Host "Checking for connected devices..."
$DevicesOutput = & $AdbExe devices
$Devices = $DevicesOutput | Where-Object { $_ -match "\tdevice$" } | ForEach-Object { $_.Split("`t")[0] }

if (-not $Devices) {
    Write-Error "No devices connected or emulators running."
    exit 1
}

Write-Host "Found devices: $($Devices -join ', ')"

Write-Host "Building APK..."
if (Test-Path ".\gradlew.bat") {
    .\gradlew.bat assembleDebug
    if ($LASTEXITCODE -ne 0) {
        Write-Error "Build Failed!"
        exit 1
    }
}
else {
    Write-Error "gradlew.bat not found."
    exit 1
}

$ApkPath = ".\app\build\outputs\apk\debug\app-debug.apk"
if (-not (Test-Path $ApkPath)) {
    Write-Error "APK not found at $ApkPath"
    exit 1
}

foreach ($Device in $Devices) {
    Write-Host "Installing on $Device..."
    & $AdbExe -s $Device install -r $ApkPath
    
    Write-Host "Launching on $Device..."
    & $AdbExe -s $Device shell am start -n com.example.roomiesplit/.MainActivity
}

Write-Host "Deployment complete for all devices."

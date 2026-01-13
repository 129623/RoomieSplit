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

# Add platform-tools to PATH so gradle and other commands can find adb
$Env:Path += ";$SdkDir\platform-tools"

if (-not (Test-Path $AdbExe)) {
    Write-Warning "ADB not found at $AdbExe. Installation might fail."
}

Write-Host "Checking for connected devices..."
& $AdbExe devices

# Wait for device to be ready
Write-Host "Waiting for device to connect... (Please ensure Emulator is running)"
& $AdbExe wait-for-device
Write-Host "Device connected!"

Write-Host "Building and Installing Debug APK..."

if (Test-Path ".\gradlew.bat") {
    .\gradlew.bat installDebug
    if ($LASTEXITCODE -eq 0) {
        Write-Host "Launch App..."
        & $AdbExe shell am start -n com.example.roomiesplit/.MainActivity
    }
    else {
        Write-Error "Build Failed!"
    }
}
else {
    Write-Error "gradlew.bat not found. Please run setup_wrapper.ps1 first."
}

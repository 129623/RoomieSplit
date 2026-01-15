$LocalProps = ".\local.properties"
$SdkDir = "C:\Users\admin\AppData\Local\Android\Sdk" # Default fallback

if (Test-Path $LocalProps) {
    Get-Content $LocalProps | ForEach-Object {
        if ($_ -match "sdk\.dir=(.*)") {
            $SdkDir = $matches[1].Replace("\:", ":").Replace("\\", "\")
        }
    }
}
$EmulatorExe = Join-Path $SdkDir "emulator\emulator.exe"
$AvdName = "Medium_Phone_API_36.1"

if (-not (Test-Path $EmulatorExe)) {
    Write-Error "Emulator executable not found at $EmulatorExe"
    exit 1
}

Write-Host "Launching Emulator: $AvdName in a new window..."
# Use Start-Process to run in a separate window so it doesn't block this terminal
Start-Process -FilePath $EmulatorExe -ArgumentList "-avd $AvdName -netdelay none -netspeed full"

Write-Host "Emulator launching... Please wait for it to fully boot before running run_app.ps1"

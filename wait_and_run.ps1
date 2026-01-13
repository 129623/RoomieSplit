. "$PSScriptRoot\setup_env.ps1"

Write-Host "Waiting for device to be ready..."
$deviceReady = $false
$timeout = 120 # Seconds
$timer = 0

while (-not $deviceReady -and $timer -lt $timeout) {
    $devices = adb devices | Select-String "device$"
    if ($devices) {
        $deviceReady = $true
        Write-Host "Device found!"
        break
    }
    Start-Sleep -Seconds 5
    $timer += 5
    Write-Host "Waiting... ($timer/$timeout)"
}

if ($deviceReady) {
    Write-Host "Device ready. Running App..."
    . "$PSScriptRoot\run_app.ps1"
}
else {
    Write-Error "Timeout waiting for emulator/device."
}

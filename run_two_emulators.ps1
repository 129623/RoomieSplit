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

if (-not (Test-Path $EmulatorExe)) {
    Write-Error "Emulator executable not found at $EmulatorExe"
    exit 1
}

$Avd1 = "Medium_Phone_API_36.1"
$Avd2 = "Medium_Phone2"

Write-Host "Launching Emulator 1: $Avd1..."
Start-Process -FilePath $EmulatorExe -ArgumentList "-avd `"$Avd1`" -netdelay none -netspeed full"

Write-Host "Launching Emulator 2: $Avd2..."
Start-Process -FilePath $EmulatorExe -ArgumentList "-avd `"$Avd2`" -netdelay none -netspeed full"

Write-Host "Both emulators are launching..."

$path = "D:\Android\.android\avd\Medium_Phone_API_36.1.ini"
if (Test-Path $path) {
    (Get-Content $path) -replace [regex]::Escape('path=C:\Users\1296233\.android\avd\Medium_Phone.avd'), 'path=D:\Android\.android\avd\Medium_Phone.avd' | Set-Content $path
    Write-Host "AVD configuration updated."
} else {
    Write-Error "AVD INI file not found at $path"
}

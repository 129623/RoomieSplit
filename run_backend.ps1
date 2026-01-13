$ErrorActionPreference = "Stop"

Write-Host "Starting Backend Service..."

if (Test-Path ".\gradlew.bat") {
    # Check for application.properties
    $propFile = "backend\src\main\resources\application.properties"
    if (Test-Path $propFile) {
        Write-Host "Found configuration: $propFile"
        Write-Host "Ensure your MySQL is running on localhost:3306 (user: root, pass: 123456)"
    }
    else {
        Write-Warning "application.properties not found!"
    }

    # Run the backend
    .\gradlew.bat :backend:bootRun
}
else {
    Write-Error "gradlew.bat not found. Please run setup_wrapper.ps1 first."
}

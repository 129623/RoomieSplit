. "$PSScriptRoot\setup_env.ps1"
$ProgressPreference = 'SilentlyContinue'
$gradleVersion = "8.2"
$gradleDistributionUrl = "https://services.gradle.org/distributions/gradle-$gradleVersion-bin.zip"
$zipFile = "gradle-$gradleVersion-bin.zip"
$extractPath = "gradle_temp"

Write-Host "Downloading Gradle $gradleVersion..."
Invoke-WebRequest -Uri $gradleDistributionUrl -OutFile $zipFile

Write-Host "Extracting..."
Expand-Archive -Path $zipFile -DestinationPath $extractPath -Force

$gradleBin = "$PWD\$extractPath\gradle-$gradleVersion\bin\gradle.bat"
Write-Host "Running gradle wrapper..."
& $gradleBin wrapper

Write-Host "Cleanup..."
Remove-Item $zipFile
Remove-Item $extractPath -Recurse -Force

Write-Host "Gradle Wrapper installed."

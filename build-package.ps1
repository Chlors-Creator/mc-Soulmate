$toolchainHome = Join-Path $PSScriptRoot '.toolchain\jdk-25.0.1'
$env:JAVA_HOME = $toolchainHome
$env:PATH = (Join-Path $toolchainHome 'bin') + ';' + $env:PATH
$env:GRADLE_USER_HOME = Join-Path $PSScriptRoot '.gradle-user-home'
$tempDirectory = Join-Path $env:GRADLE_USER_HOME 'tmp'
New-Item -ItemType Directory -Path $tempDirectory -Force | Out-Null
$env:TEMP = $tempDirectory
$env:TMP = $tempDirectory
$gradleWrapper = Join-Path $PSScriptRoot 'gradlew.bat'

& $gradleWrapper --no-configuration-cache --console plain jar
exit $LASTEXITCODE

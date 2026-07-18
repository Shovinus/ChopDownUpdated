param(
    [string[]] $MinecraftVersion
)

$ErrorActionPreference = 'Stop'

$targets = @(
    @{ Minecraft = '1.20.1'; Forge = '47.4.20'; Java = '17'; Pack = '15' },
    @{ Minecraft = '1.20.2'; Forge = '48.1.0';  Java = '17'; Pack = '18' },
    @{ Minecraft = '1.20.3'; Forge = '49.0.2';  Java = '17'; Pack = '22' },
    @{ Minecraft = '1.20.4'; Forge = '49.2.8';  Java = '17'; Pack = '22' },
    @{ Minecraft = '1.20.6'; Forge = '50.2.9';  Java = '21'; Pack = '32' }
)

if ($MinecraftVersion) {
    $targets = @($targets | Where-Object { $_.Minecraft -in $MinecraftVersion })
    if ($targets.Count -ne $MinecraftVersion.Count) {
        throw "One or more requested Minecraft versions are not supported: $($MinecraftVersion -join ', ')"
    }
}

$java17 = 'C:\Program Files\Eclipse Adoptium\jdk-17.0.17.10-hotspot'
if (-not (Test-Path -LiteralPath "$java17\bin\java.exe")) {
    throw "Java 17 was not found at $java17"
}

$originalJavaHome = $env:JAVA_HOME
$env:JAVA_HOME = $java17

try {
    foreach ($target in $targets) {
        $buildDirectory = "build/versions/$($target.Minecraft)"

        Write-Host "Building Minecraft $($target.Minecraft) with Forge $($target.Forge)..."
        & "$PSScriptRoot\..\gradlew.bat" --no-daemon clean build `
            "-Pminecraft_version=$($target.Minecraft)" `
            '-Pminecraft_version_range=[1.20,1.21)' `
            "-Pforge_version=$($target.Forge)" `
            '-Pforge_version_range=[46,)' `
            '-Ploader_version_range=[46,)' `
            "-Pjava_toolchain_version=$($target.Java)" `
            "-Ppack_format=$($target.Pack)" `
            "-Pbuild_directory=$buildDirectory"

        if ($LASTEXITCODE -ne 0) {
            throw "Minecraft $($target.Minecraft) build failed with exit code $LASTEXITCODE"
        }
    }
} finally {
    $env:JAVA_HOME = $originalJavaHome
}

Write-Host 'All supported Minecraft 1.20.x builds completed successfully.'

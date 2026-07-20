param(
    [Parameter(Mandatory = $true)]
    [ValidateSet('1.21.2', '1.21.3', '1.21.4', '1.21.5', '1.21.6', '1.21.7', '1.21.8', '1.21.9', '1.21.10')]
    [string] $MinecraftVersion,

    [ValidateSet('Client', 'Server')]
    [string] $Side = 'Client',

    [ValidateSet('Run', 'Debug', 'Build')]
    [string] $Action = 'Run'
)

$ErrorActionPreference = 'Stop'

$forgeVersions = @{
    '1.21.3'  = '53.1.11'
    '1.21.4'  = '54.1.17'
    '1.21.5'  = '55.1.11'
    '1.21.6'  = '56.0.9'
    '1.21.7'  = '57.0.3'
    '1.21.8'  = '58.1.19'
    '1.21.9'  = '59.0.5'
    '1.21.10' = '60.1.11'
}

if (-not $forgeVersions.ContainsKey($MinecraftVersion)) {
    throw "Minecraft $MinecraftVersion cannot be tested: Forge did not publish a build for this version."
}

$forgeVersion = $forgeVersions[$MinecraftVersion]
$forgeMajor = [int]($forgeVersion.Split('.')[0])
$gradle = Join-Path $PSScriptRoot '..\gradlew.bat'
$buildDirectory = "build/versions/$MinecraftVersion"
$versionArgs = @(
    "-Pminecraft_version=$MinecraftVersion"
    "-Pforge_version=$forgeVersion"
    "-Pminecraft_version_range=[$MinecraftVersion,1.21.11)"
    "-Pforge_version_range=[$forgeMajor,$($forgeMajor + 1))"
    "-Ploader_version_range=[$forgeMajor,$($forgeMajor + 1))"
    "-Pbuild_directory=$buildDirectory"
    '--console=plain'
)

if ($Action -eq 'Build') {
    & $gradle @versionArgs clean compileJava
} else {
    $runTask = if ($Side -eq 'Client') { 'runClient' } else { 'runServer' }
    $runArgs = @($versionArgs) + $runTask
    if ($Action -eq 'Debug') {
        $runArgs += '--debug-jvm'
    }
    & $gradle @runArgs
}

exit $LASTEXITCODE

param(
    [Parameter(Mandatory = $true)]
    [ValidateSet('1.21', '1.21.1')]
    [string] $MinecraftVersion,

    [ValidateSet('Client', 'Server')]
    [string] $Side = 'Client',

    [ValidateSet('Run', 'Debug', 'Build')]
    [string] $Action = 'Run'
)

$ErrorActionPreference = 'Stop'

$fabricApiVersions = @{
    '1.21'    = '0.102.0+1.21'
    '1.21.1'  = '0.116.14+1.21.1'
}

$gradle = Join-Path $PSScriptRoot '..\gradlew.bat'
$buildDirectory = "build/versions/$MinecraftVersion"
$versionArgs = @(
    "-Pminecraft_version=$MinecraftVersion"
    "-Pfabric_api_version=$($fabricApiVersions[$MinecraftVersion])"
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

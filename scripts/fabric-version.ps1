param(
    [Parameter(Mandatory = $true)]
    [ValidateSet('1.21', '1.21.1', '1.21.2', '1.21.3', '1.21.4', '1.21.5', '1.21.6', '1.21.7', '1.21.8', '1.21.9', '1.21.10', '1.21.11')]
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
    '1.21.2'  = '0.106.1+1.21.2'
    '1.21.3'  = '0.114.1+1.21.3'
    '1.21.4'  = '0.119.4+1.21.4'
    '1.21.5'  = '0.128.2+1.21.5'
    '1.21.6'  = '0.128.2+1.21.6'
    '1.21.7'  = '0.129.0+1.21.7'
    '1.21.8'  = '0.136.1+1.21.8'
    '1.21.9'  = '0.134.1+1.21.9'
    '1.21.10' = '0.138.4+1.21.10'
    '1.21.11' = '0.141.5+1.21.11'
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

$ErrorActionPreference = 'Continue'

$versions = @('1.21', '1.21.1', '1.21.2', '1.21.3', '1.21.4', '1.21.5', '1.21.6', '1.21.7', '1.21.8', '1.21.9', '1.21.10')
$results = @()

foreach ($version in $versions) {
    Write-Host "Testing Fabric on Minecraft $version..."
    & "$PSScriptRoot\fabric-version.ps1" -MinecraftVersion $version -Action Build
    $results += [pscustomobject]@{
        Minecraft = $version
        Compatible = $LASTEXITCODE -eq 0
    }
}

$results | Format-Table -AutoSize
if ($results.Compatible -contains $false) {
    exit 1
}

$ErrorActionPreference = 'Continue'

$versions = @('1.21', '1.21.1')
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

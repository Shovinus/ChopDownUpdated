$ErrorActionPreference = 'Continue'

$versions = @('1.21.2', '1.21.3', '1.21.4', '1.21.5', '1.21.6', '1.21.7', '1.21.8', '1.21.9', '1.21.10')
$results = @()

foreach ($version in $versions) {
    Write-Host "Testing Forge on Minecraft $version..."
    if ($version -eq '1.21.2') {
        $results += [pscustomobject]@{
            Minecraft = $version
            Compatible = 'N/A'
            Note = 'No Forge release exists'
        }
        continue
    }

    & powershell.exe -NoProfile -ExecutionPolicy Bypass -File "$PSScriptRoot\forge-version.ps1" -MinecraftVersion $version -Action Build
    $results += [pscustomobject]@{
        Minecraft = $version
        Compatible = if ($LASTEXITCODE -eq 0) { 'Yes' } else { 'No' }
        Note = ''
    }
}

$results | Format-Table -AutoSize
if ($results.Compatible -contains 'No') {
    exit 1
}

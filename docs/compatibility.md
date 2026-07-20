# Compatibility

This branch targets Fabric on Minecraft 1.21.1.

| Component | Version |
| --- | --- |
| Minecraft | 1.21.1 |
| Fabric Loader | 0.19.3 or newer |
| Fabric API | 0.116.14+1.21.1 |
| Java | 21 or newer |

The Fabric API mod must be installed alongside Chop Down Updated.

Build the remapped production jar with:

```powershell
$env:JAVA_HOME='C:\path\to\jdk-21'
.\gradlew.bat build
```

The server config is written to `config/chopdownupdated.json` on first launch.

## Cross-version test matrix

Use the `Fabric: Debug Client (select version)` or `Fabric: Debug Server (select version)` entry in
VS Code's Run and Debug panel to select Minecraft 1.21 or 1.21.1. The matching Fabric API
is selected automatically.

Compilation and dedicated-server startup are verified on both Minecraft 1.21 and 1.21.1.

Run the complete compile matrix from PowerShell with:

```powershell
.\scripts\test-fabric-versions.ps1
```

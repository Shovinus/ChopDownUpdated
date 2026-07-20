# Compatibility

This branch targets Fabric on Minecraft 1.21.10.

| Component | Version |
| --- | --- |
| Minecraft | 1.21.10 |
| Fabric Loader | 0.19.3 or newer |
| Fabric API | 0.138.4+1.21.10 |
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
VS Code's Run and Debug panel to select any release from 1.21.2 through 1.21.10. The matching Fabric API
is selected automatically.

Gameplay testing confirms that this source version works from Minecraft 1.21.2 through 1.21.10.
Minecraft 1.21 and 1.21.1 are maintained on the separate `fabric-1.21.1` branch.

Run the complete compile matrix from PowerShell with:

```powershell
.\scripts\test-fabric-versions.ps1
```

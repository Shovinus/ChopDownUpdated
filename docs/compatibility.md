# Compatibility

This branch targets Fabric on Minecraft 1.21.11.

| Component | Version |
| --- | --- |
| Minecraft | 1.21.11 |
| Fabric Loader | 0.19.3 or newer |
| Fabric API | 0.141.5+1.21.11 |
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
VS Code's Run and Debug panel to select any release from 1.21 through 1.21.11. The matching Fabric API
is selected automatically.

The unchanged 1.21.11 source currently has this compile compatibility:

| Minecraft versions | Result |
| --- | --- |
| 1.21 through 1.21.10 | Incompatible |
| 1.21.11 | Compatible |

Older releases use the pre-1.21.11 resource identifier and command permission APIs. Run the complete
compile matrix from PowerShell with:

```powershell
.\scripts\test-fabric-versions.ps1
```

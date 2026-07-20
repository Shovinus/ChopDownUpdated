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

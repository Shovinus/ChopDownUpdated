# Compatibility

This build is currently scoped to the Minecraft 1.20 line.

| Minecraft | Forge | Status |
| --- | --- | --- |
| 1.20.1 | 47.4.20 | Supported build target |
| 1.20.6 | 50.2.8 | Compatibility probe |

The jar metadata declares:

| Dependency | Range |
| --- | --- |
| Minecraft | `[1.20,1.20.7)` |
| Forge / JavaFML | `[46,51)` |

That deliberately excludes confirmed failing targets such as 1.19.4 and 1.21.11.

Build the normal jar locally with:

```powershell
$env:JAVA_HOME='C:\Program Files\Eclipse Adoptium\jdk-17.0.17.10-hotspot'
.\gradlew.bat build
```

The 1.20.6 compatibility build task needs Java 21 installed.

# Compatibility

This build targets Minecraft 1.20.1.

| Minecraft | Forge | Status |
| --- | --- | --- |
| 1.20.1 | 47.4.20 | Supported build target |
| 1.20.2 | 48.1.0 | Supported build target |
| 1.20.3 | 49.0.2 | Supported build target |
| 1.20.4 | 49.2.8 | Supported build target |
| 1.20.5 | — | Forge did not publish a release |
| 1.20.6 | 50.2.9 | Supported build target |

The jar metadata declares:

| Dependency | Range |
| --- | --- |
| Minecraft | `[1.20,1.21)` |
| Forge / JavaFML | `[46,)` |

The build dependencies target exact versions for compilation, while the jar metadata allows Forge
to attempt loading on any Minecraft 1.20.x and Forge 46-or-newer installation.

Build the normal jar locally with:

```powershell
$env:JAVA_HOME='C:\Program Files\Eclipse Adoptium\jdk-17.0.17.10-hotspot'
.\gradlew.bat build
```

Build every supported 1.20.x artifact with:

```powershell
.\scripts\build-all-versions.ps1
```

To build or resume selected targets, pass one or more versions:

```powershell
.\scripts\build-all-versions.ps1 -MinecraftVersion 1.20.2,1.20.4
```

Minecraft 1.20.1 through 1.20.4 require Java 17. Minecraft 1.20.6 requires Java 21;
Gradle provisions that compiler through the configured Foojay toolchain resolver.

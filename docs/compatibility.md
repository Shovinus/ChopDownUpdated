# Compatibility

This branch targets Minecraft 1.21.11.

| Minecraft | Forge | Status |
| --- | --- | --- |
| 1.21.11 | 61.1.9 | Supported build target |

The jar metadata declares:

| Dependency | Range |
| --- | --- |
| Minecraft | `[1.21.11,1.22)` |
| Forge / JavaFML | `[61,)` |

The project uses Java 21, ForgeGradle 7, and Gradle 9.5, matching the Forge 1.21.11 MDK.

Build the jar locally with:

```powershell
$env:JAVA_HOME='C:\path\to\jdk-21'
.\gradlew.bat build
```

If Gradle itself is launched with an older supported JDK, the configured Foojay resolver provisions
the Java 21 compilation toolchain automatically.

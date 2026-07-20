# Compatibility

This branch tests the same code against the available Forge releases from Minecraft 1.21.2
through 1.21.10. Forge did not publish a 1.21.2 build, so the runnable matrix starts at 1.21.3.

| Minecraft | Forge | Status |
| --- | --- | --- |
| 1.21.2 | N/A | Unavailable (no Forge release) |
| 1.21.3 | 53.1.11 | Test profile |
| 1.21.4 | 54.1.17 | Test profile |
| 1.21.5 | 55.1.11 | Test profile |
| 1.21.6 | 56.0.9 | Test profile |
| 1.21.7 | 57.0.3 | Test profile |
| 1.21.8 | 58.1.19 | Test profile |
| 1.21.9 | 59.0.5 | Test profile |
| 1.21.10 | 60.1.11 | Default build target |

The jar metadata declares:

| Dependency | Range |
| --- | --- |
| Minecraft | `[1.21.10,1.21.11)` |
| Forge / JavaFML | `[60,61)` |

The project uses Java 21, ForgeGradle 7, and Gradle 9.5.

Build the jar locally with:

```powershell
$env:JAVA_HOME='C:\path\to\jdk-21'
.\gradlew.bat build
```

To compile against every profile, run:

```powershell
.\scripts\test-forge-versions.ps1
```

VS Code's Run and Debug view also provides selectable client and server profiles. Selecting
1.21.2 reports the missing Forge release instead of attempting an invalid dependency download.

If Gradle itself is launched with an older supported JDK, the configured Foojay resolver provisions
the Java 21 compilation toolchain automatically.

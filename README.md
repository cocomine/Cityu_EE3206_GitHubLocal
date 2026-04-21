# Local Git GUI (Year 3 OOD Project)

## Project Overview
This project is a simplified local-only Git GUI application for Java SE 21.
It executes real local `git` commands through `ProcessBuilder`, and adds local GitHub-like issue/comment features stored as JSON under each repository's `.gitgui/` folder.

Key points:
- Local repository operations only
- No remote/cloud/GitHub server integration
- Strong OOD focus: Command Pattern, Template Method, Strategy, Facade, Repository abstraction

## Architecture Summary
The project uses four layers:
- `ui`: JavaFX app and controllers
- `application`: facade, services, command orchestration
- `domain`: entities, value objects, enums, domain models
- `infrastructure`: git process execution and JSON persistence

### OOD Patterns Used
- Command Pattern: each Git action is its own command class
- Template Method: `GitCommand.execute()` defines common command workflow
- Strategy Pattern: `GitOutputParser` for parsing command output
- Facade Pattern: `AppFacade` is the single UI entry point
- Repository/Store abstraction: `IssueStore` with `JsonIssueStore`
- Dependency Inversion: use of interfaces (`GitClient`, `IssueStore`, `GitOutputParser`)

## Package Structure
- `ui`
- `ui.controller`
- `application`
- `application.command`
- `application.command.parser`
- `domain`
- `infrastructure.git`
- `infrastructure.store`

## Prerequisites
- JDK 21 (includes `jpackage`)
- Git available on `PATH`

## How to Run (Development)

### IntelliJ IDEA
1. Open this project folder in IntelliJ IDEA.
2. Set project SDK to JDK 21.
3. Import as a Gradle project.
4. Run `ui.Launcher`.

### Gradle Wrapper
```bash
./gradlew run
```

### Run Tests
```bash
./gradlew test
```

## Build Single Runnable JAR (Fat JAR)

### Build for Current Machine Platform
```bash
./gradlew singleJar
```

Output:
- `build/libs/gitgui-local-1.0.0-<platform>-all.jar`

Examples:
- `gitgui-local-1.0.0-mac-aarch64-all.jar`
- `gitgui-local-1.0.0-win-all.jar`

### Build for a Specific Target Platform
```bash
./gradlew singleJar -PtargetPlatform=win
./gradlew singleJar -PtargetPlatform=mac-aarch64
./gradlew singleJar -PtargetPlatform=mac
./gradlew singleJar -PtargetPlatform=linux
```

Supported `targetPlatform` values:
- `mac-aarch64`
- `mac`
- `win`
- `linux`

Run the generated JAR with a matching platform:
```bash
java -jar build/libs/gitgui-local-1.0.0-<platform>-all.jar
```

## Build Installer with Bundled Java Runtime

### Build App Image (Current OS)
```bash
./gradlew packageAppImage
```

### Build Installer (Current OS)
```bash
./gradlew packageInstaller
```

Installer output folder:
- `build/jpackage/`

Installer type by OS:
- macOS: `.dmg`
- Windows: `.exe`
- Linux: `.deb`

Important:
- Installer packaging is OS-specific. Build Windows installer on Windows, macOS installer on macOS, Linux installer on Linux.

## CI: Build Installers for macOS/Windows/Linux

GitHub Actions workflow:
- `.github/workflows/build-installers.yml`

Triggers:
- Manual run (`workflow_dispatch`)
- Push tag matching `v*` (for example `v1.0.0`)

Workflow behavior:
- Builds `packageInstaller` and `singleJar` on:
  - `ubuntu-latest`
  - `windows-latest`
  - `macos-latest`
- Uploads artifacts for each platform:
  - Installer (`.deb` / `.exe` / `.dmg`)
  - Single JAR (`*-all.jar`)

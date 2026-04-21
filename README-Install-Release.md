# Installation and Launch Guide (with Release Edition Notes)

This document is for end users. It explains how to install and launch the app, and how to choose between different release editions (Release Assets).
It is recommended to download the correct platform asset from GitHub Releases first, then follow the steps below.

## 1. Choose the Right Release Edition

### A. Installer Edition (Recommended for Most Users)
- macOS: `*.dmg`
- Windows: `*.exe`
- Linux: `*.deb`

macOS mapping (CI / Release artifact names):
- Intel Mac (x64): `gitgui-mac-x64-installer`
- Apple Silicon Mac (arm64): `gitgui-mac-arm64-installer`

Purpose:
- One-click installation experience
- Java Runtime is bundled (no separate JDK/JRE installation required)
- Best choice for general end users

### B. Single JAR Edition (Developer/Technical Use)
- Filename examples: `gitgui-local-1.0.0-win-all.jar`, `gitgui-local-1.0.0-mac-all.jar`, `gitgui-local-1.0.0-mac-aarch64-all.jar`

Purpose:
- Start directly with `java -jar`
- Suitable for development, debugging, and CI validation
- Must match your OS/platform (for example, a Windows JAR cannot run directly on macOS)

## 2. Installation and Launch

### 2.1 Installer Edition (Recommended)

### macOS (`.dmg`)
1. Choose the correct CPU architecture artifact first (`mac-x64` or `mac-arm64`).
2. Download the `.dmg` file inside that artifact.
3. Open it and drag the app to `Applications`.
4. Launch from Launchpad or Applications.

### Windows (`.exe`)
1. Download the `.exe`.
2. Run the installer.
3. Launch from the Start menu or desktop shortcut after installation.

### Linux (`.deb`)
1. Download the `.deb`.
2. Install:
```bash
sudo dpkg -i gitgui-local-<version>.deb
sudo apt-get -f install -y
```
3. Launch from the application menu (or the installed launcher command).

### 2.2 Single JAR Edition

Prerequisite:
- Java 21 (or a compatible version) installed

Launch:
```bash
java -jar gitgui-local-<version>-<platform>-all.jar
```

Notes:
- Download the platform-matching JAR (`win` / `mac` / `mac-aarch64` / `linux`).
- Intel Mac (x64) should use `mac`.
- Apple Silicon (M1/M2/M3) should use `mac-aarch64`.

## 3. Recommended Usage by Edition

- General end users: use the `Installer` edition.
- Development, testing, automation scripts: use the `Single JAR` edition.

## 4. FAQ

### Q1. Should I choose `mac` or `mac-aarch64`?
Single JAR:
- Intel Mac: choose `mac`
- Apple Silicon (M1/M2/M3): choose `mac-aarch64`

Installer artifact:
- Intel Mac: choose `mac-x64`
- Apple Silicon (M1/M2/M3): choose `mac-arm64`

### Q2. Why is there no single universal file for all platforms?
- JavaFX includes platform-specific native libraries, so builds must be packaged by OS/CPU.

### Q3. Can installer packages be built cross-platform?
- No. Windows installers must be built on Windows; macOS and Linux follow the same rule.

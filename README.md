# NilLoader Installer

A small cross-platform installer for [NilLoader](https://git.sleeping.town/Nil/NilLoader).

> **This is an unofficial build** It is not affiliated with or endorsed by the original NilLoader project.

The goal is to turn the current manual Java-agent setup into a normal launcher-oriented install flow.

## Supported launchers

- Minecraft Launcher (official / Mojang, including the Microsoft Store profile database): detects existing installations, clones the selected installation, downloads NilLoader as `NilLoader.jar`, and adds the upstream-compatible `-javaagent:NilLoader.jar` argument only to the cloned profile.
- Prism Launcher
- PolyMC
- MultiMC-compatible instances: writes the NilLoader component patch directly into the instance `patches/` directory.

## Safety behavior

- The official launcher profile database is backed up before editing.
- Existing non-NilLoader official installations are not modified; a separate `+ NilLoader` installation is created. Existing NilLoader profiles can be repaired in place.
- Existing Prism/PolyMC NilLoader patches are backed up before replacement.
- Downloaded NilLoader agents are validated as JARs and checked for a valid `Premain-Class` before launcher configuration is changed.
- Nilmods can be placed in the generated `nilmods/` directory.

## Requirements

- Java 17 or newer to run the installer.
- Close the Minecraft/Prism/PolyMC launcher before installing so it does not overwrite configuration files while they are being changed.

## Build

With Gradle:

```bash
gradle clean jar
java -jar build/libs/NilLoaderInstaller-0.1.2+unofficial.jar
```

No third-party runtime dependencies are used.

You can also build without Gradle:

Linux/macOS:

```bash
./build.sh
```

Windows:

```bat
build.bat
```

The standalone jar is written to `dist/NilLoaderInstaller-0.1.1+unofficial.jar`.

## NilLoader version

The installer currently targets NilLoader `1.3.6`, matching the current official Prism component metadata documented by NilLoader.

## Project layout

- `LauncherDetector` — discovers official and MultiMC-family installations/instances.
- `OfficialLauncherInstaller` — clones or repairs an official launcher profile and injects the canonical relative Java agent argument.
- `PrismInstaller` — installs the NilLoader component patch.
- `Downloader` — downloads the NilLoader agent for the official launcher flow.
- `Json` — tiny dependency-free JSON reader/writer used for launcher configuration.

## License

MIT. NilLoader itself is a separate project and remains under its own license.
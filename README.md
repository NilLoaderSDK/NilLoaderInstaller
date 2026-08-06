# NilLoader Installer

A small cross-platform installer for [NilLoader](https://git.sleeping.town/Nil/NilLoader).

The goal is to turn the current manual Java-agent setup into a normal launcher-oriented install flow.

## Supported launchers

- Minecraft Launcher (official / Mojang, including the Microsoft Store profile database): detects existing installations, clones the selected installation, downloads NilLoader, and adds the required `-javaagent` argument only to the cloned profile.
- Prism Launcher
- PolyMC
- MultiMC-compatible instances: writes the NilLoader component patch directly into the instance `patches/` directory.

## Safety behavior

- The official launcher profile database is backed up before editing.
- Existing official installations are not modified; a separate `+ NilLoader` installation is created.
- Existing Prism/PolyMC NilLoader patches are backed up before replacement.
- Nilmods can be placed in the generated `nilmods/` directory.

## Requirements

- Java 17 or newer to run the installer.
- Close the Minecraft/Prism/PolyMC launcher before installing so it does not overwrite configuration files while they are being changed.

## Build

With Gradle:

```bash
gradle clean jar
java -jar build/libs/NilLoaderInstaller.jar
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

The standalone jar is written to `dist/NilLoaderInstaller.jar`.

## NilLoader version

The installer currently targets NilLoader `1.3.6`, matching the current official Prism component metadata documented by NilLoader.

## Project layout

- `LauncherDetector` — discovers official and MultiMC-family installations/instances.
- `OfficialLauncherInstaller` — clones an official launcher profile and injects the Java agent argument.
- `PrismInstaller` — installs the NilLoader component patch.
- `Downloader` — downloads the NilLoader agent for the official launcher flow.
- `Json` — tiny dependency-free JSON reader/writer used for launcher configuration.

## License

MIT. NilLoader itself is a separate project and remains under its own license.

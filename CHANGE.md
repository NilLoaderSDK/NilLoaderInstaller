# Changelog

## v0.1.2
- Mark this build as unofficial: jar output is now named `NilLoaderInstaller-<version>+unofficial.jar` to distinguish it from official releases.
- Add a CI verification step that fails the build if the expected `+unofficial` jar is missing.
- Note the unofficial status of this fork/build directly in the README.

## v0.1.1
- Fix official Minecraft Launcher installs that could exit immediately before game logging started.
- Follow NilLoader's documented manual layout for the official launcher: place the agent at `<gameDir>/NilLoader.jar` and use the exact JVM argument `-javaagent:NilLoader.jar`.
- Repair old NilLoader JVM arguments from v0.1.0 instead of preserving a potentially broken absolute/quoted `-javaagent` path.
- Selecting an already-installed official profile now repairs it in place instead of creating another cloned profile.
- Validate downloaded JARs before modifying launcher configuration: the file must be a valid JAR with a `Premain-Class` whose class entry exists.
- Backup files now keep the actual launcher profile database filename, including the Microsoft Store variant.

## v0.1.0
- Initial release of NilLoaderInstaller.
- Builds a runnable Java jar for the installer application.

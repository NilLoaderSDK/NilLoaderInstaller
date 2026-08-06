package me.tamkungz.nilloaderinstaller.model;

import java.nio.file.Path;

public record InstallTarget(
        LauncherType launcherType,
        String displayName,
        String minecraftVersion,
        Path launcherRoot,
        Path instanceRoot,
        Path gameDir,
        String profileId,
        boolean installed
) {
    @Override
    public String toString() {
        String version = minecraftVersion == null || minecraftVersion.isBlank() ? "unknown" : minecraftVersion;
        return displayName + " — " + launcherType.displayName() + " — Minecraft " + version;
    }
}

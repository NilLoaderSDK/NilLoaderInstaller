package me.tamkungz.nilloaderinstaller.model;

public enum LauncherType {
    OFFICIAL("Minecraft Launcher"),
    PRISM("Prism Launcher"),
    POLYMC("PolyMC"),
    MULTIMC("MultiMC-compatible");

    private final String displayName;

    LauncherType(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}

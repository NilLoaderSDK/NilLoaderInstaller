package me.tamkungz.nilloaderinstaller.util;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class Os {
    private Os() {}

    public static String name() {
        return System.getProperty("os.name", "unknown").toLowerCase(Locale.ROOT);
    }

    public static Path home() {
        return Paths.get(System.getProperty("user.home"));
    }

    public static boolean isWindows() { return name().contains("win"); }
    public static boolean isMac() { return name().contains("mac"); }
    public static boolean isLinux() { return name().contains("linux"); }

    public static List<Path> defaultOfficialRoots() {
        List<Path> out = new ArrayList<>();
        if (isWindows()) {
            String appData = System.getenv("APPDATA");
            if (appData != null) out.add(Path.of(appData, ".minecraft"));
        } else if (isMac()) {
            out.add(home().resolve("Library/Application Support/minecraft"));
        } else {
            out.add(home().resolve(".minecraft"));
        }
        return out;
    }

    public static List<Path> defaultMultiMcRoots() {
        List<Path> out = new ArrayList<>();
        if (isWindows()) {
            String appData = System.getenv("APPDATA");
            if (appData != null) {
                out.add(Path.of(appData, "PrismLauncher"));
                out.add(Path.of(appData, "PolyMC"));
                out.add(Path.of(appData, "MultiMC"));
            }
        } else if (isMac()) {
            Path appSupport = home().resolve("Library/Application Support");
            out.add(appSupport.resolve("PrismLauncher"));
            out.add(appSupport.resolve("PolyMC"));
            out.add(appSupport.resolve("MultiMC"));
        } else {
            out.add(home().resolve(".local/share/PrismLauncher"));
            out.add(home().resolve(".local/share/PolyMC"));
            out.add(home().resolve(".local/share/multimc"));
            out.add(home().resolve(".local/share/MultiMC"));
            out.add(home().resolve(".var/app/org.prismlauncher.PrismLauncher/data/PrismLauncher"));
            out.add(home().resolve(".var/app/org.polymc.PolyMC/data/PolyMC"));
        }
        return out;
    }
}

package me.tamkungz.nilloaderinstaller;

import me.tamkungz.nilloaderinstaller.model.InstallTarget;
import me.tamkungz.nilloaderinstaller.model.LauncherType;
import me.tamkungz.nilloaderinstaller.util.Json;
import me.tamkungz.nilloaderinstaller.util.Os;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class LauncherDetector {
    private LauncherDetector() {}

    public static List<InstallTarget> detectAll() {
        Set<String> seen = new LinkedHashSet<>();
        List<InstallTarget> out = new ArrayList<>();
        for (Path root : Os.defaultOfficialRoots()) addUnique(out, seen, detectPath(root));
        for (Path root : Os.defaultMultiMcRoots()) addUnique(out, seen, detectPath(root));
        out.sort(Comparator.comparing((InstallTarget t) -> t.launcherType().displayName())
                .thenComparing(InstallTarget::displayName, String.CASE_INSENSITIVE_ORDER));
        return out;
    }

    public static List<InstallTarget> detectPath(Path raw) {
        List<InstallTarget> out = new ArrayList<>();
        if (raw == null) return out;
        Path path = raw.toAbsolutePath().normalize();
        if (!Files.exists(path)) return out;

        // Official launcher root (classic and Microsoft Store launcher variants)
        if (officialProfilesFile(path) != null) {
            out.addAll(detectOfficial(path));
            return out;
        }

        // Direct Prism/MultiMC instance
        if (Files.isRegularFile(path.resolve("instance.cfg")) || Files.isRegularFile(path.resolve("mmc-pack.json"))) {
            InstallTarget target = detectInstance(path, guessLauncherType(path.getParent()));
            if (target != null) out.add(target);
            return out;
        }

        // Prism/MultiMC root
        Path instances = path.resolve("instances");
        if (Files.isDirectory(instances)) {
            LauncherType type = guessLauncherType(path);
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(instances)) {
                for (Path instance : stream) {
                    if (!Files.isDirectory(instance)) continue;
                    InstallTarget target = detectInstance(instance, type);
                    if (target != null) out.add(target);
                }
            } catch (IOException ignored) {}
            out.sort(Comparator.comparing(InstallTarget::displayName, String.CASE_INSENSITIVE_ORDER));
            return out;
        }

        return out;
    }

    public static Path officialProfilesFile(Path root) {
        Path classic = root.resolve("launcher_profiles.json");
        Path store = root.resolve("launcher_profiles_microsoft_store.json");
        if (Files.isRegularFile(classic)) return classic;
        if (Files.isRegularFile(store)) return store;
        return null;
    }

    private static List<InstallTarget> detectOfficial(Path root) {
        List<InstallTarget> out = new ArrayList<>();
        Path profilesFile = officialProfilesFile(root);
        if (profilesFile == null) return out;
        try {
            Map<String, Object> json = Json.object(Json.read(profilesFile));
            Object profilesValue = json.get("profiles");
            if (!(profilesValue instanceof Map<?, ?> profiles)) return out;
            for (Map.Entry<?, ?> entry : profiles.entrySet()) {
                if (!(entry.getValue() instanceof Map<?, ?> p)) continue;
                String id = String.valueOf(entry.getKey());
                String name = value(p.get("name"), id);
                String version = value(p.get("lastVersionId"), "unknown");
                String javaArgs = value(p.get("javaArgs"), "");
                String gameDirRaw = value(p.get("gameDir"), "");
                Path gameDir = gameDirRaw.isBlank() ? root : Path.of(gameDirRaw).toAbsolutePath().normalize();
                boolean installed = javaArgs.toLowerCase(Locale.ROOT).contains("-javaagent:")
                        && javaArgs.toLowerCase(Locale.ROOT).contains("nilloader");
                out.add(new InstallTarget(
                        LauncherType.OFFICIAL,
                        name,
                        version,
                        root,
                        null,
                        gameDir,
                        id,
                        installed
                ));
            }
        } catch (Exception ignored) {}
        return out;
    }

    private static InstallTarget detectInstance(Path instance, LauncherType type) {
        try {
            String name = instance.getFileName().toString();
            Path cfg = instance.resolve("instance.cfg");
            if (Files.isRegularFile(cfg)) {
                for (String line : Files.readAllLines(cfg, StandardCharsets.UTF_8)) {
                    int eq = line.indexOf('=');
                    if (eq <= 0) continue;
                    String key = line.substring(0, eq).trim();
                    if (key.equalsIgnoreCase("name")) {
                        String v = line.substring(eq + 1).trim();
                        if (!v.isBlank()) name = v;
                    }
                }
            }

            String mcVersion = "unknown";
            Path pack = instance.resolve("mmc-pack.json");
            if (Files.isRegularFile(pack)) {
                Object root = Json.read(pack);
                if (root instanceof Map<?, ?> rootMap) {
                    Object comps = rootMap.get("components");
                    if (comps instanceof List<?> list) {
                        for (Object item : list) {
                            if (!(item instanceof Map<?, ?> comp)) continue;
                            if ("net.minecraft".equals(value(comp.get("uid"), ""))) {
                                mcVersion = value(comp.get("version"), "unknown");
                                break;
                            }
                        }
                    }
                }
            }

            Path gameDir = instance.resolve(".minecraft");
            Path patch = instance.resolve("patches/com.unascribed.nilloader.json");
            boolean installed = Files.isRegularFile(patch);
            return new InstallTarget(type, name, mcVersion,
                    instance.getParent() != null ? instance.getParent().getParent() : null,
                    instance, gameDir, null, installed);
        } catch (Exception e) {
            return null;
        }
    }

    private static LauncherType guessLauncherType(Path root) {
        String text = root == null ? "" : root.toString().toLowerCase(Locale.ROOT);
        if (text.contains("prism")) return LauncherType.PRISM;
        if (text.contains("polymc")) return LauncherType.POLYMC;
        return LauncherType.MULTIMC;
    }

    private static String value(Object v, String fallback) {
        return v instanceof String s ? s : fallback;
    }

    private static void addUnique(List<InstallTarget> out, Set<String> seen, List<InstallTarget> candidates) {
        for (InstallTarget t : candidates) {
            String key = t.launcherType() + "|" + t.profileId() + "|" + t.instanceRoot() + "|" + t.displayName();
            if (seen.add(key)) out.add(t);
        }
    }
}

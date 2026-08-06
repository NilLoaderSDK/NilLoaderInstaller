package me.tamkungz.nilloaderinstaller.install;

import me.tamkungz.nilloaderinstaller.AppInfo;
import me.tamkungz.nilloaderinstaller.LauncherDetector;
import me.tamkungz.nilloaderinstaller.model.InstallResult;
import me.tamkungz.nilloaderinstaller.model.InstallTarget;
import me.tamkungz.nilloaderinstaller.util.Json;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class OfficialLauncherInstaller {
    private OfficialLauncherInstaller() {}

    public static InstallResult install(InstallTarget target) throws IOException, InterruptedException {
        if (target.launcherRoot() == null || target.profileId() == null) {
            throw new IOException("Missing official launcher profile information");
        }

        Path profilesFile = LauncherDetector.officialProfilesFile(target.launcherRoot());
        if (profilesFile == null) {
            throw new IOException("Minecraft launcher profile database was not found");
        }

        Map<String, Object> root = Json.object(Json.read(profilesFile));
        Object profilesValue = root.get("profiles");
        if (!(profilesValue instanceof Map<?, ?> rawProfiles)) {
            throw new IOException("launcher_profiles.json does not contain profiles");
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> profiles = (Map<String, Object>) rawProfiles;
        Object sourceValue = profiles.get(target.profileId());
        if (!(sourceValue instanceof Map<?, ?>)) {
            throw new IOException("The selected launcher profile no longer exists. Refresh and try again.");
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> source = (Map<String, Object>) Json.deepCopy(sourceValue);

        Path gameDir = target.gameDir() != null ? target.gameDir() : target.launcherRoot();
        Files.createDirectories(gameDir);
        Path nilLoaderDir = gameDir.resolve("nilloader");
        Files.createDirectories(nilLoaderDir);
        Path jar = nilLoaderDir.resolve("nilloader-" + AppInfo.NILLOADER_VERSION + ".jar");
        Downloader.downloadNilLoader(jar);
        Files.createDirectories(gameDir.resolve("nilmods"));

        String oldArgs = Json.string(source.get("javaArgs"), "").trim();
        String agentArg = "-javaagent:\"" + jar.toAbsolutePath().normalize() + "\"";
        String newArgs;
        if (containsNilLoaderAgent(oldArgs)) {
            newArgs = oldArgs;
        } else {
            newArgs = oldArgs.isBlank() ? agentArg : oldArgs + " " + agentArg;
        }

        String originalName = Json.string(source.get("name"), target.displayName());
        source.put("name", originalName.endsWith(" + NilLoader") ? originalName : originalName + " + NilLoader");
        source.put("type", "custom");
        source.put("javaArgs", newArgs);
        source.put("created", java.time.Instant.now().toString());
        source.put("lastUsed", java.time.Instant.now().toString());

        String newId = "nilloader-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        while (profiles.containsKey(newId)) {
            newId = "nilloader-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        }
        profiles.put(newId, source);

        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        Path backup = profilesFile.resolveSibling("launcher_profiles.json.nilloader-backup-" + ts);
        Files.copy(profilesFile, backup, StandardCopyOption.REPLACE_EXISTING);

        Path temp = profilesFile.resolveSibling("launcher_profiles.json.nilloader.tmp");
        Json.write(temp, root);
        try {
            Files.move(temp, profilesFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            Files.move(temp, profilesFile, StandardCopyOption.REPLACE_EXISTING);
        }

        return new InstallResult(
                "Created a new Minecraft Launcher installation named ‘" + source.get("name") + "’ and installed NilLoader "
                        + AppInfo.NILLOADER_VERSION + ".",
                gameDir.resolve("nilmods")
        );
    }

    private static boolean containsNilLoaderAgent(String args) {
        String lower = args.toLowerCase(Locale.ROOT);
        return lower.contains("-javaagent:") && lower.contains("nilloader");
    }
}

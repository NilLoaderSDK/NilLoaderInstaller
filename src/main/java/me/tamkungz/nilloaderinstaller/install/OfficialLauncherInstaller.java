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
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

public final class OfficialLauncherInstaller {
    private static final String CANONICAL_AGENT_ARG = "-javaagent:NilLoader.jar";

    // Handles the v0.1.0 absolute/quoted form as well as the canonical relative form.
    // Only NilLoader java agents are removed; unrelated -javaagent options are preserved.
    private static final Pattern NILLOADER_AGENT_ARG = Pattern.compile(
            "(?i)(?<!\\S)-javaagent:(?:\\\"[^\\\"]*nilloader[^\\\"]*\\\"|'[^']*nilloader[^']*'|\\S*nilloader\\S*)"
    );

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
            throw new IOException(profilesFile.getFileName() + " does not contain profiles");
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

        // NilLoader's documented manual setup expects the agent in the game working
        // directory and uses the exact relative JVM argument -javaagent:NilLoader.jar.
        // This avoids launcher-specific quoting/path parsing issues seen with v0.1.0.
        Path jar = gameDir.resolve("NilLoader.jar");
        Downloader.downloadNilLoader(jar);
        Files.createDirectories(gameDir.resolve("nilmods"));

        String oldArgs = Json.string(source.get("javaArgs"), "");
        String newArgs = withCanonicalNilLoaderAgent(oldArgs);
        source.put("javaArgs", newArgs);
        source.put("type", "custom");
        source.put("lastUsed", java.time.Instant.now().toString());

        boolean repair = target.installed();
        String profileId;
        if (repair) {
            profileId = target.profileId();
            profiles.put(profileId, source);
        } else {
            String originalName = Json.string(source.get("name"), target.displayName());
            source.put("name", originalName.endsWith(" + NilLoader") ? originalName : originalName + " + NilLoader");
            source.put("created", java.time.Instant.now().toString());

            profileId = "nilloader-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
            while (profiles.containsKey(profileId)) {
                profileId = "nilloader-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
            }
            profiles.put(profileId, source);
        }

        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        String originalFileName = profilesFile.getFileName().toString();
        Path backup = profilesFile.resolveSibling(originalFileName + ".nilloader-backup-" + ts);
        Files.copy(profilesFile, backup, StandardCopyOption.REPLACE_EXISTING);

        Path temp = profilesFile.resolveSibling(originalFileName + ".nilloader.tmp");
        Json.write(temp, root);
        try {
            Files.move(temp, profilesFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            Files.move(temp, profilesFile, StandardCopyOption.REPLACE_EXISTING);
        }

        String action = repair ? "Repaired" : "Created";
        return new InstallResult(
                action + " Minecraft Launcher installation ‘" + source.get("name") + "’ with NilLoader "
                        + AppInfo.NILLOADER_VERSION + ".\nJVM argument: " + CANONICAL_AGENT_ARG,
                gameDir.resolve("nilmods")
        );
    }

    static String withCanonicalNilLoaderAgent(String args) {
        String current = args == null ? "" : args;
        current = NILLOADER_AGENT_ARG.matcher(current).replaceAll(" ").trim();
        current = current.replaceAll("[ \\t]{2,}", " ");
        return current.isBlank() ? CANONICAL_AGENT_ARG : current + " " + CANONICAL_AGENT_ARG;
    }
}

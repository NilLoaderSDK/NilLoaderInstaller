package me.tamkungz.nilloaderinstaller.install;

import me.tamkungz.nilloaderinstaller.AppInfo;
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
import java.util.List;
import java.util.Map;

public final class PrismInstaller {
    private PrismInstaller() {}

    public static InstallResult install(InstallTarget target) throws IOException {
        if (target.instanceRoot() == null) throw new IOException("Missing instance root");
        Path patches = target.instanceRoot().resolve("patches");
        Files.createDirectories(patches);
        Path patch = patches.resolve("com.unascribed.nilloader.json");
        if (Files.exists(patch)) {
            String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
            Files.copy(patch, patch.resolveSibling(patch.getFileName() + ".backup-" + ts),
                    StandardCopyOption.REPLACE_EXISTING);
        }

        Map<String, Object> agent = new LinkedHashMap<>();
        agent.put("name", AppInfo.NILLOADER_COORDINATE);
        agent.put("url", AppInfo.NILLOADER_MAVEN_BASE);

        Map<String, Object> root = new LinkedHashMap<>();
        root.put("formatVersion", 1);
        root.put("name", "NilLoader");
        root.put("uid", "com.unascribed.nilloader");
        root.put("version", AppInfo.NILLOADER_VERSION);
        root.put("+agents", List.of(agent));
        Json.write(patch, root);

        Path nilmods = target.gameDir().resolve("nilmods");
        Files.createDirectories(nilmods);
        return new InstallResult(
                "NilLoader " + AppInfo.NILLOADER_VERSION + " component installed into " + target.displayName()
                        + ". The launcher will download the agent when the instance starts.",
                nilmods
        );
    }
}

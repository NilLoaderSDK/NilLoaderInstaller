package me.tamkungz.nilloaderinstaller.install;

import me.tamkungz.nilloaderinstaller.AppInfo;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;

public final class Downloader {
    private Downloader() {}

    public static void downloadNilLoader(Path destination) throws IOException, InterruptedException {
        Files.createDirectories(destination.toAbsolutePath().getParent());
        Path temp = destination.resolveSibling(destination.getFileName() + ".part");

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        String jarUrl = System.getProperty("nilloader.installer.jarUrl", AppInfo.NILLOADER_JAR_URL);
        HttpRequest request = HttpRequest.newBuilder(URI.create(jarUrl))
                .timeout(Duration.ofSeconds(60))
                .header("User-Agent", AppInfo.APP_NAME + "/" + AppInfo.APP_VERSION)
                .GET()
                .build();
        HttpResponse<Path> response = client.send(request, HttpResponse.BodyHandlers.ofFile(temp));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            Files.deleteIfExists(temp);
            throw new IOException("NilLoader download failed: HTTP " + response.statusCode());
        }
        if (Files.size(temp) < 1024) {
            Files.deleteIfExists(temp);
            throw new IOException("Downloaded NilLoader file is unexpectedly small");
        }
        try {
            Files.move(temp, destination, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException atomicNotSupported) {
            Files.move(temp, destination, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}

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
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

public final class Downloader {
    private Downloader() {}

    public static void downloadNilLoader(Path destination) throws IOException, InterruptedException {
        Files.createDirectories(destination.toAbsolutePath().getParent());
        Path temp = destination.resolveSibling(destination.getFileName() + ".part");
        Files.deleteIfExists(temp);

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

        try {
            validateJavaAgentJar(temp);
        } catch (IOException e) {
            Files.deleteIfExists(temp);
            throw e;
        }

        try {
            Files.move(temp, destination, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException atomicNotSupported) {
            Files.move(temp, destination, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    static void validateJavaAgentJar(Path jar) throws IOException {
        if (!Files.isRegularFile(jar) || Files.size(jar) < 1024) {
            throw new IOException("Downloaded NilLoader file is unexpectedly small or missing");
        }

        try (JarFile jf = new JarFile(jar.toFile())) {
            Manifest manifest = jf.getManifest();
            if (manifest == null) {
                throw new IOException("Downloaded NilLoader JAR has no manifest");
            }
            Attributes attrs = manifest.getMainAttributes();
            String premain = attrs.getValue("Premain-Class");
            if (premain == null || premain.isBlank()) {
                throw new IOException("Downloaded file is not a Java agent (Premain-Class is missing)");
            }
            String classEntry = premain.replace('.', '/') + ".class";
            if (jf.getJarEntry(classEntry) == null) {
                throw new IOException("Downloaded Java agent is incomplete: " + classEntry + " is missing");
            }
        } catch (java.util.zip.ZipException e) {
            throw new IOException("Downloaded NilLoader file is not a valid JAR", e);
        }
    }
}

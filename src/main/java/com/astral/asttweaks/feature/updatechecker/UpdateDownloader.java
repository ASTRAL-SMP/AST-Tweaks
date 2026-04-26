package com.astral.asttweaks.feature.updatechecker;

import com.astral.asttweaks.ASTTweaks;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.DoubleConsumer;

/**
 * Downloads a release asset from GitHub into the mods folder, then deletes
 * the currently-running jar so Fabric only sees the new one on next launch.
 */
public class UpdateDownloader {

    private static final String USER_AGENT;
    private static final int BUFFER_SIZE = 16 * 1024;
    private static final int CONNECT_TIMEOUT_MS = 15_000;
    private static final int READ_TIMEOUT_MS = 60_000;

    static {
        String version = FabricLoader.getInstance()
                .getModContainer(ASTTweaks.MOD_ID)
                .map(c -> c.getMetadata().getVersion().getFriendlyString())
                .orElse("unknown");
        USER_AGENT = String.format("AST-Tweaks/%s (Fabric Minecraft Mod)", version);
    }

    public static class Result {
        public final boolean success;
        public final String message;
        public final Path downloadedFile;

        private Result(boolean success, String message, Path downloadedFile) {
            this.success = success;
            this.message = message;
            this.downloadedFile = downloadedFile;
        }

        public static Result ok(Path file, String message) {
            return new Result(true, message, file);
        }

        public static Result fail(String message) {
            return new Result(false, message, null);
        }
    }

    public static CompletableFuture<Result> downloadAsync(GitHubRelease release, DoubleConsumer progress) {
        return CompletableFuture.supplyAsync(() -> download(release, progress));
    }

    public static Result download(GitHubRelease release, DoubleConsumer progress) {
        Optional<GitHubRelease.Asset> assetOpt = pickJarAsset(release);
        if (assetOpt.isEmpty()) {
            return Result.fail("No .jar asset found in the latest release");
        }
        GitHubRelease.Asset asset = assetOpt.get();

        Path modsDir = FabricLoader.getInstance().getGameDir().resolve("mods");
        try {
            Files.createDirectories(modsDir);
        } catch (IOException e) {
            return Result.fail("Failed to access mods directory: " + e.getMessage());
        }

        Path target = modsDir.resolve(asset.getName());
        Path tmp = modsDir.resolve(asset.getName() + ".part");

        try {
            downloadToFile(asset.getBrowserDownloadUrl(), tmp, asset.getSize(), progress);
        } catch (IOException e) {
            safeDelete(tmp);
            return Result.fail("Download failed: " + e.getMessage());
        }

        try {
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            safeDelete(tmp);
            return Result.fail("Failed to move downloaded file into place: " + e.getMessage());
        }

        deleteRunningJar();

        return Result.ok(target, "Downloaded " + asset.getName());
    }

    private static Optional<GitHubRelease.Asset> pickJarAsset(GitHubRelease release) {
        List<GitHubRelease.Asset> assets = release.getAssets();
        if (assets == null || assets.isEmpty()) {
            return Optional.empty();
        }
        // Prefer the .jar matching the mod's archives_base_name, but fall back
        // to any .jar (excluding -sources/-dev/-javadoc).
        return assets.stream()
                .filter(a -> a.getName() != null && a.getName().toLowerCase().endsWith(".jar"))
                .filter(a -> {
                    String n = a.getName().toLowerCase();
                    return !n.contains("-sources") && !n.contains("-dev") && !n.contains("-javadoc");
                })
                .findFirst();
    }

    private static void downloadToFile(String url, Path target, long expectedSize, DoubleConsumer progress) throws IOException {
        URL parsed = URI.create(url).toURL();
        URLConnection conn = parsed.openConnection();
        conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
        conn.setReadTimeout(READ_TIMEOUT_MS);
        conn.setRequestProperty("User-Agent", USER_AGENT);
        conn.setRequestProperty("Accept", "application/octet-stream");

        long total = expectedSize > 0 ? expectedSize : conn.getContentLengthLong();
        long downloaded = 0;

        try (InputStream in = conn.getInputStream();
             var out = Files.newOutputStream(target)) {
            byte[] buf = new byte[BUFFER_SIZE];
            int read;
            while ((read = in.read(buf)) != -1) {
                out.write(buf, 0, read);
                downloaded += read;
                if (progress != null && total > 0) {
                    progress.accept(Math.min(1.0, (double) downloaded / total));
                }
            }
        }

        if (progress != null) {
            progress.accept(1.0);
        }
    }

    /**
     * Try to delete the currently-running mod jar. POSIX systems unlink
     * the file immediately while keeping the open handle valid. On Windows
     * the delete may fail because the JVM holds the file open; we fall
     * back to {@link java.io.File#deleteOnExit()} so it goes away when
     * Minecraft shuts down.
     */
    private static void deleteRunningJar() {
        Optional<Path> sourceOpt = FabricLoader.getInstance()
                .getModContainer(ASTTweaks.MOD_ID)
                .flatMap(c -> c.getOrigin().getPaths().stream().findFirst());
        if (sourceOpt.isEmpty()) {
            return;
        }
        Path source = sourceOpt.get();
        if (!Files.isRegularFile(source) || !source.getFileName().toString().endsWith(".jar")) {
            return;
        }
        try {
            Files.delete(source);
            ASTTweaks.LOGGER.info("UpdateDownloader: Deleted previous jar {}", source.getFileName());
        } catch (IOException e) {
            try {
                source.toFile().deleteOnExit();
                ASTTweaks.LOGGER.warn("UpdateDownloader: Could not delete running jar now ({}); scheduled for deletion on JVM exit.", e.getMessage());
            } catch (Exception ignored) {
                ASTTweaks.LOGGER.warn("UpdateDownloader: Could not delete running jar; please remove {} manually after restart.", source.getFileName());
            }
        }
    }

    private static void safeDelete(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
        }
    }
}

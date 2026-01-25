package com.astral.asttweaks.feature.updatechecker;

import com.astral.asttweaks.ASTTweaks;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * HTTP client for interacting with the Modrinth API.
 */
public class ModrinthApiClient {
    private static final String API_BASE = "https://api.modrinth.com/v2";
    private static final String USER_AGENT;
    private static final Gson GSON = new GsonBuilder().create();
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(15);

    private final HttpClient httpClient;

    static {
        String version = FabricLoader.getInstance()
                .getModContainer(ASTTweaks.MOD_ID)
                .map(container -> container.getMetadata().getVersion().getFriendlyString())
                .orElse("unknown");
        USER_AGENT = String.format("AST-Tweaks/%s (Fabric Minecraft Mod)", version);
    }

    public ModrinthApiClient() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .build();
    }

    /**
     * Fetch versions for a project asynchronously.
     *
     * @param projectId The Modrinth project ID or slug
     * @param loader The mod loader (e.g., "fabric")
     * @param gameVersion The Minecraft version (e.g., "1.19.4")
     * @return CompletableFuture containing list of versions, or empty list on error
     */
    public CompletableFuture<List<ModrinthVersion>> getVersionsAsync(
            String projectId,
            String loader,
            String gameVersion
    ) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return getVersions(projectId, loader, gameVersion);
            } catch (Exception e) {
                ASTTweaks.LOGGER.warn("Failed to fetch versions from Modrinth: {}", e.getMessage());
                return List.of();
            }
        });
    }

    /**
     * Fetch versions for a project synchronously.
     *
     * @param projectId The Modrinth project ID or slug
     * @param loader The mod loader (e.g., "fabric")
     * @param gameVersion The Minecraft version (e.g., "1.19.4")
     * @return List of versions
     * @throws IOException If the request fails
     * @throws InterruptedException If the request is interrupted
     */
    public List<ModrinthVersion> getVersions(
            String projectId,
            String loader,
            String gameVersion
    ) throws IOException, InterruptedException {
        String loadersParam = URLEncoder.encode("[\"" + loader + "\"]", StandardCharsets.UTF_8);
        String versionsParam = URLEncoder.encode("[\"" + gameVersion + "\"]", StandardCharsets.UTF_8);

        String url = String.format(
                "%s/project/%s/version?loaders=%s&game_versions=%s",
                API_BASE,
                URLEncoder.encode(projectId, StandardCharsets.UTF_8),
                loadersParam,
                versionsParam
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", USER_AGENT)
                .header("Accept", "application/json")
                .timeout(REQUEST_TIMEOUT)
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException("Modrinth API returned status " + response.statusCode());
        }

        Type listType = new TypeToken<List<ModrinthVersion>>() {}.getType();
        return GSON.fromJson(response.body(), listType);
    }

    /**
     * Get the latest version for a project.
     *
     * @param projectId The Modrinth project ID or slug
     * @param loader The mod loader
     * @param gameVersion The Minecraft version
     * @return Optional containing the latest version, or empty if none found
     */
    public CompletableFuture<Optional<ModrinthVersion>> getLatestVersionAsync(
            String projectId,
            String loader,
            String gameVersion
    ) {
        return getVersionsAsync(projectId, loader, gameVersion)
                .thenApply(versions -> versions.isEmpty() ? Optional.empty() : Optional.of(versions.get(0)));
    }
}

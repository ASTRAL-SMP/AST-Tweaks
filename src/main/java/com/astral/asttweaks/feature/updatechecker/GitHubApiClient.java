package com.astral.asttweaks.feature.updatechecker;

import com.astral.asttweaks.ASTTweaks;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * HTTP client for the GitHub REST API (releases endpoint).
 */
public class GitHubApiClient {
    private static final String API_BASE = "https://api.github.com";
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

    public GitHubApiClient() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    /**
     * Fetch the latest published (non-draft, non-prerelease) release for a repository.
     *
     * @param ownerRepo a string of the form "owner/repo"
     */
    public CompletableFuture<Optional<GitHubRelease>> getLatestReleaseAsync(String ownerRepo) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return Optional.ofNullable(getLatestRelease(ownerRepo));
            } catch (Exception e) {
                ASTTweaks.LOGGER.warn("UpdateChecker: GitHub request failed: {}", e.getMessage());
                return Optional.empty();
            }
        });
    }

    public GitHubRelease getLatestRelease(String ownerRepo) throws IOException, InterruptedException {
        if (ownerRepo == null || ownerRepo.isBlank() || !ownerRepo.contains("/")) {
            throw new IOException("Invalid GitHub repo format (expected owner/repo): " + ownerRepo);
        }
        String url = API_BASE + "/repos/" + ownerRepo.trim() + "/releases/latest";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", USER_AGENT)
                .header("Accept", "application/vnd.github+json")
                .header("X-GitHub-Api-Version", "2022-11-28")
                .timeout(REQUEST_TIMEOUT)
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        int status = response.statusCode();
        if (status == 404) {
            return null;
        }
        if (status != 200) {
            throw new IOException("GitHub API returned status " + status);
        }

        return GSON.fromJson(response.body(), GitHubRelease.class);
    }
}

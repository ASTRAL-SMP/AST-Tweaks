package com.astral.asttweaks.feature.updatechecker;

import com.astral.asttweaks.ASTTweaks;
import com.astral.asttweaks.feature.Feature;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screen.TitleScreen;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Feature that checks for mod updates via GitHub releases.
 */
public class UpdateCheckerFeature implements Feature {

    private final UpdateCheckerConfig config = new UpdateCheckerConfig();
    private final GitHubApiClient apiClient = new GitHubApiClient();
    private final AtomicBoolean checkInProgress = new AtomicBoolean(false);
    private final AtomicBoolean hasCheckedThisSession = new AtomicBoolean(false);
    private final AtomicBoolean pendingNotification = new AtomicBoolean(false);
    private final AtomicBoolean titlePopupShown = new AtomicBoolean(false);

    private String cachedLatestVersion = null;
    private GitHubRelease cachedLatestRelease = null;
    private String currentVersion = null;

    @Override
    public String getId() {
        return "updatechecker";
    }

    @Override
    public String getName() {
        return "Update Checker";
    }

    @Override
    public void init() {
        currentVersion = FabricLoader.getInstance()
                .getModContainer(ASTTweaks.MOD_ID)
                .map(container -> container.getMetadata().getVersion().getFriendlyString())
                .orElse("0.0.0");

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            UpdateNotification.showCurrentVersion(currentVersion);

            if (pendingNotification.get() && cachedLatestVersion != null) {
                UpdateNotification.show(cachedLatestVersion, currentVersion);
                pendingNotification.set(false);
            }
        });

        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (!(screen instanceof TitleScreen)) {
                return;
            }
            if (!shouldShowTitlePopup()) {
                return;
            }
            if (!titlePopupShown.compareAndSet(false, true)) {
                return;
            }
            GitHubRelease release = cachedLatestRelease;
            String version = currentVersion;
            client.execute(() -> client.setScreen(
                    new UpdateAvailableScreen(screen, this, release, version)
            ));
        });

        if (config.shouldCheck()) {
            performUpdateCheck();
        }

        ASTTweaks.LOGGER.info("UpdateChecker feature initialized (current version: {})", currentVersion);
    }

    @Override
    public void tick() {
    }

    @Override
    public boolean isEnabled() {
        return config.isEnabled();
    }

    @Override
    public void setEnabled(boolean enabled) {
        config.setEnabled(enabled);
    }

    private boolean shouldShowTitlePopup() {
        if (!config.isEnabled() || !config.isShowOnTitleScreen()) {
            return false;
        }
        if (cachedLatestRelease == null || cachedLatestVersion == null) {
            return false;
        }
        if (!hasUpdateAvailable()) {
            return false;
        }
        String skipped = config.getSkippedVersion();
        return !cachedLatestVersion.equals(skipped);
    }

    public void skipVersion(String version) {
        if (version == null || version.isBlank()) {
            return;
        }
        config.setSkippedVersion(version);
        ASTTweaks.LOGGER.info("UpdateChecker: User skipped version {}", version);
    }

    public void markUpdateInstalled(String version) {
        pendingNotification.set(false);
        if (version != null && !version.isBlank()) {
            config.setSkippedVersion(version);
        }
    }

    public void performUpdateCheck() {
        if (!config.isEnabled()) {
            return;
        }

        String repo = config.getGithubRepo();
        if (repo == null || repo.isBlank()) {
            ASTTweaks.LOGGER.debug("UpdateChecker: No GitHub repo configured, skipping check");
            return;
        }

        if (!checkInProgress.compareAndSet(false, true)) {
            ASTTweaks.LOGGER.debug("UpdateChecker: Check already in progress");
            return;
        }

        ASTTweaks.LOGGER.info("UpdateChecker: Checking for updates (repo: {})", repo);

        apiClient.getLatestReleaseAsync(repo)
                .thenAccept(this::handleReleaseResponse)
                .exceptionally(throwable -> {
                    ASTTweaks.LOGGER.warn("UpdateChecker: Failed to check for updates", throwable);
                    checkInProgress.set(false);
                    return null;
                });
    }

    private void handleReleaseResponse(Optional<GitHubRelease> releaseOpt) {
        try {
            config.setLastCheck(System.currentTimeMillis());
            hasCheckedThisSession.set(true);

            if (releaseOpt.isEmpty()) {
                ASTTweaks.LOGGER.info("UpdateChecker: No release found for the configured repo");
                return;
            }

            GitHubRelease release = releaseOpt.get();
            if (release.isDraft() || release.isPrerelease()) {
                ASTTweaks.LOGGER.info("UpdateChecker: Latest release is a draft/prerelease, ignoring");
                return;
            }

            String latestVersionNumber = release.getVersionNumber();
            cachedLatestVersion = latestVersionNumber;
            cachedLatestRelease = release;

            ASTTweaks.LOGGER.info("UpdateChecker: Latest version is {}, current version is {}",
                    latestVersionNumber, currentVersion);

            if (isNewerVersion(latestVersionNumber, currentVersion)) {
                ASTTweaks.LOGGER.info("UpdateChecker: Update available! {} -> {}",
                        currentVersion, latestVersionNumber);

                if (config.isShowNotification()) {
                    pendingNotification.set(true);
                }
            } else {
                ASTTweaks.LOGGER.info("UpdateChecker: You are running the latest version");
            }
        } finally {
            checkInProgress.set(false);
        }
    }

    private boolean isNewerVersion(String newVersion, String currentVersion) {
        if (newVersion == null || currentVersion == null) {
            return false;
        }
        String cleanNew = cleanVersionString(newVersion);
        String cleanCurrent = cleanVersionString(currentVersion);

        if (cleanNew.equals(cleanCurrent)) {
            return false;
        }
        try {
            String[] newParts = cleanNew.split("[.\\-+]");
            String[] currentParts = cleanCurrent.split("[.\\-+]");
            int maxLength = Math.max(newParts.length, currentParts.length);
            for (int i = 0; i < maxLength; i++) {
                int newNum = i < newParts.length ? parseVersionPart(newParts[i]) : 0;
                int currentNum = i < currentParts.length ? parseVersionPart(currentParts[i]) : 0;
                if (newNum > currentNum) return true;
                else if (newNum < currentNum) return false;
            }
        } catch (Exception e) {
            ASTTweaks.LOGGER.warn("UpdateChecker: Failed to parse version strings: {} vs {}",
                    newVersion, currentVersion);
        }
        return false;
    }

    private String cleanVersionString(String version) {
        return version.replaceFirst("^[vV]", "").trim();
    }

    private int parseVersionPart(String part) {
        try {
            StringBuilder digits = new StringBuilder();
            for (char c : part.toCharArray()) {
                if (Character.isDigit(c)) digits.append(c);
                else break;
            }
            return digits.length() > 0 ? Integer.parseInt(digits.toString()) : 0;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public void forceCheck() {
        if (!config.isEnabled()) {
            return;
        }
        String repo = config.getGithubRepo();
        if (repo == null || repo.isBlank()) {
            UpdateNotification.showError();
            return;
        }
        if (!checkInProgress.compareAndSet(false, true)) {
            return;
        }
        apiClient.getLatestReleaseAsync(repo)
                .thenAccept(releaseOpt -> {
                    try {
                        config.setLastCheck(System.currentTimeMillis());
                        if (releaseOpt.isEmpty()) {
                            UpdateNotification.showError();
                            return;
                        }
                        GitHubRelease release = releaseOpt.get();
                        String latestVersionNumber = release.getVersionNumber();
                        cachedLatestVersion = latestVersionNumber;
                        cachedLatestRelease = release;
                        if (isNewerVersion(latestVersionNumber, currentVersion)) {
                            UpdateNotification.show(latestVersionNumber, currentVersion);
                        } else {
                            UpdateNotification.showUpToDate();
                        }
                    } finally {
                        checkInProgress.set(false);
                    }
                })
                .exceptionally(throwable -> {
                    UpdateNotification.showError();
                    checkInProgress.set(false);
                    return null;
                });
    }

    public UpdateCheckerConfig getConfig() {
        return config;
    }

    public String getCachedLatestVersion() {
        return cachedLatestVersion;
    }

    public String getCurrentVersion() {
        return currentVersion;
    }

    public boolean hasUpdateAvailable() {
        return cachedLatestVersion != null && isNewerVersion(cachedLatestVersion, currentVersion);
    }
}

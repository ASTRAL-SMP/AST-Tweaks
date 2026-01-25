package com.astral.asttweaks.feature.updatechecker;

import com.astral.asttweaks.ASTTweaks;
import com.astral.asttweaks.feature.Feature;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Feature that checks for mod updates via the Modrinth API.
 */
public class UpdateCheckerFeature implements Feature {
    private static final String LOADER = "fabric";
    private static final String GAME_VERSION = "1.19.4";

    private final UpdateCheckerConfig config = new UpdateCheckerConfig();
    private final ModrinthApiClient apiClient = new ModrinthApiClient();
    private final AtomicBoolean checkInProgress = new AtomicBoolean(false);
    private final AtomicBoolean hasCheckedThisSession = new AtomicBoolean(false);
    private final AtomicBoolean pendingNotification = new AtomicBoolean(false);

    private String cachedLatestVersion = null;
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
        // Get current mod version
        currentVersion = FabricLoader.getInstance()
                .getModContainer(ASTTweaks.MOD_ID)
                .map(container -> container.getMetadata().getVersion().getFriendlyString())
                .orElse("0.0.0");

        // Register event to show notification when joining a world
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            // 常に現在バージョンを表示
            UpdateNotification.showCurrentVersion(currentVersion);

            // 新バージョンがあれば続けて表示
            if (pendingNotification.get() && cachedLatestVersion != null) {
                UpdateNotification.show(cachedLatestVersion, currentVersion);
                pendingNotification.set(false);
            }
        });

        // Perform initial check if conditions are met
        if (config.shouldCheck()) {
            performUpdateCheck();
        }

        ASTTweaks.LOGGER.info("UpdateChecker feature initialized (current version: {})", currentVersion);
    }

    @Override
    public void tick() {
        // No per-tick operations needed
    }

    @Override
    public boolean isEnabled() {
        return config.isEnabled();
    }

    @Override
    public void setEnabled(boolean enabled) {
        config.setEnabled(enabled);
    }

    /**
     * Perform an update check asynchronously.
     */
    public void performUpdateCheck() {
        if (!config.isEnabled()) {
            return;
        }

        String projectId = config.getProjectId();
        if (projectId == null || projectId.isBlank()) {
            ASTTweaks.LOGGER.debug("UpdateChecker: No project ID configured, skipping check");
            return;
        }

        // Prevent concurrent checks
        if (!checkInProgress.compareAndSet(false, true)) {
            ASTTweaks.LOGGER.debug("UpdateChecker: Check already in progress");
            return;
        }

        ASTTweaks.LOGGER.info("UpdateChecker: Checking for updates (project: {})", projectId);

        apiClient.getLatestVersionAsync(projectId, LOADER, GAME_VERSION)
                .thenAccept(this::handleVersionResponse)
                .exceptionally(throwable -> {
                    ASTTweaks.LOGGER.warn("UpdateChecker: Failed to check for updates", throwable);
                    checkInProgress.set(false);
                    return null;
                });
    }

    private void handleVersionResponse(Optional<ModrinthVersion> latestVersionOpt) {
        try {
            config.setLastCheck(System.currentTimeMillis());
            hasCheckedThisSession.set(true);

            if (latestVersionOpt.isEmpty()) {
                ASTTweaks.LOGGER.info("UpdateChecker: No versions found for the configured project");
                return;
            }

            ModrinthVersion latestVersion = latestVersionOpt.get();
            String latestVersionNumber = latestVersion.getVersionNumber();
            cachedLatestVersion = latestVersionNumber;

            ASTTweaks.LOGGER.info("UpdateChecker: Latest version is {}, current version is {}",
                    latestVersionNumber, currentVersion);

            if (isNewerVersion(latestVersionNumber, currentVersion)) {
                ASTTweaks.LOGGER.info("UpdateChecker: Update available! {} -> {}",
                        currentVersion, latestVersionNumber);

                if (config.isShowNotification()) {
                    // Set pending notification to show when joining a world
                    pendingNotification.set(true);
                }
            } else {
                ASTTweaks.LOGGER.info("UpdateChecker: You are running the latest version");
            }
        } finally {
            checkInProgress.set(false);
        }
    }

    /**
     * Compare two version strings to determine if the new version is newer.
     * Handles semantic versioning (e.g., 1.2.3).
     */
    private boolean isNewerVersion(String newVersion, String currentVersion) {
        if (newVersion == null || currentVersion == null) {
            return false;
        }

        // Clean up version strings (remove 'v' prefix, etc.)
        String cleanNew = cleanVersionString(newVersion);
        String cleanCurrent = cleanVersionString(currentVersion);

        // Simple string comparison for exact match
        if (cleanNew.equals(cleanCurrent)) {
            return false;
        }

        // Parse and compare version numbers
        try {
            String[] newParts = cleanNew.split("[.\\-+]");
            String[] currentParts = cleanCurrent.split("[.\\-+]");

            int maxLength = Math.max(newParts.length, currentParts.length);

            for (int i = 0; i < maxLength; i++) {
                int newNum = i < newParts.length ? parseVersionPart(newParts[i]) : 0;
                int currentNum = i < currentParts.length ? parseVersionPart(currentParts[i]) : 0;

                if (newNum > currentNum) {
                    return true;
                } else if (newNum < currentNum) {
                    return false;
                }
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
            // Extract leading digits only
            StringBuilder digits = new StringBuilder();
            for (char c : part.toCharArray()) {
                if (Character.isDigit(c)) {
                    digits.append(c);
                } else {
                    break;
                }
            }
            return digits.length() > 0 ? Integer.parseInt(digits.toString()) : 0;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * Force a manual update check, showing a notification regardless of result.
     */
    public void forceCheck() {
        if (!config.isEnabled()) {
            return;
        }

        String projectId = config.getProjectId();
        if (projectId == null || projectId.isBlank()) {
            UpdateNotification.showError();
            return;
        }

        if (!checkInProgress.compareAndSet(false, true)) {
            return;
        }

        apiClient.getLatestVersionAsync(projectId, LOADER, GAME_VERSION)
                .thenAccept(latestVersionOpt -> {
                    try {
                        config.setLastCheck(System.currentTimeMillis());

                        if (latestVersionOpt.isEmpty()) {
                            UpdateNotification.showError();
                            return;
                        }

                        ModrinthVersion latestVersion = latestVersionOpt.get();
                        String latestVersionNumber = latestVersion.getVersionNumber();
                        cachedLatestVersion = latestVersionNumber;

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

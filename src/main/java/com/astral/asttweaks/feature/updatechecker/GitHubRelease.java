package com.astral.asttweaks.feature.updatechecker;

import net.fabricmc.loader.api.FabricLoader;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * POJO matching the relevant subset of a GitHub release returned by
 * GET /repos/{owner}/{repo}/releases.
 */
public class GitHubRelease {
    /**
     * Releases for different Minecraft versions share one repository, so a release
     * only counts for this build if it ships a jar whose file name contains this
     * marker (e.g. "mc1.19" for ast-tweaks-1.1.30+mc1.19.4.jar).
     */
    public static final String GAME_VERSION_MARKER = computeGameVersionMarker();
    // "mc1.19" must not match "mc1.190"
    private static final Pattern MARKER_PATTERN = Pattern.compile(Pattern.quote(GAME_VERSION_MARKER) + "(?![0-9])");

    private String tag_name;
    private String name;
    private String body;
    private String html_url;
    private boolean prerelease;
    private boolean draft;
    private List<Asset> assets;

    public String getTagName() {
        return tag_name;
    }

    public String getName() {
        return name;
    }

    public String getBody() {
        return body;
    }

    public String getHtmlUrl() {
        return html_url;
    }

    public boolean isPrerelease() {
        return prerelease;
    }

    public boolean isDraft() {
        return draft;
    }

    public List<Asset> getAssets() {
        return assets;
    }

    public Optional<Asset> findJarAsset() {
        if (assets == null) {
            return Optional.empty();
        }
        return assets.stream()
                .filter(a -> a.getName() != null)
                .filter(a -> {
                    String n = a.getName().toLowerCase(Locale.ROOT);
                    return n.endsWith(".jar")
                            && MARKER_PATTERN.matcher(n).find()
                            && !n.contains("-sources") && !n.contains("-dev") && !n.contains("-javadoc");
                })
                .findFirst();
    }

    private static String computeGameVersionMarker() {
        String mcVersion = FabricLoader.getInstance()
                .getModContainer("minecraft")
                .map(c -> c.getMetadata().getVersion().getFriendlyString())
                .orElse("1.19.4");
        String[] parts = mcVersion.split("\\.");
        String majorMinor = parts.length >= 2 ? parts[0] + "." + parts[1] : mcVersion;
        return "mc" + majorMinor.toLowerCase(Locale.ROOT);
    }

    /**
     * Strip a leading 'v' or 'V' from the tag, returning the version number portion.
     */
    public String getVersionNumber() {
        if (tag_name == null) return null;
        return tag_name.replaceFirst("^[vV]", "").trim();
    }

    public static class Asset {
        private String name;
        private long size;
        private String browser_download_url;
        private String content_type;

        public String getName() {
            return name;
        }

        public long getSize() {
            return size;
        }

        public String getBrowserDownloadUrl() {
            return browser_download_url;
        }

        public String getContentType() {
            return content_type;
        }
    }
}

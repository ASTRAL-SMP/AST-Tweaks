package com.astral.asttweaks.feature.updatechecker;

import java.util.List;

/**
 * POJO matching the relevant subset of a GitHub release returned by
 * GET /repos/{owner}/{repo}/releases/latest.
 */
public class GitHubRelease {
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

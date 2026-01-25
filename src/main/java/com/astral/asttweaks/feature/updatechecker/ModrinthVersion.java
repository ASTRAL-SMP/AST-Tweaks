package com.astral.asttweaks.feature.updatechecker;

import java.util.List;

/**
 * POJO representing a version from the Modrinth API response.
 */
public class ModrinthVersion {
    private String id;
    private String project_id;
    private String name;
    private String version_number;
    private String changelog;
    private String date_published;
    private List<String> game_versions;
    private List<String> loaders;
    private String version_type;  // release, beta, alpha
    private List<ModrinthFile> files;

    public String getId() {
        return id;
    }

    public String getProjectId() {
        return project_id;
    }

    public String getName() {
        return name;
    }

    public String getVersionNumber() {
        return version_number;
    }

    public String getChangelog() {
        return changelog;
    }

    public String getDatePublished() {
        return date_published;
    }

    public List<String> getGameVersions() {
        return game_versions;
    }

    public List<String> getLoaders() {
        return loaders;
    }

    public String getVersionType() {
        return version_type;
    }

    public List<ModrinthFile> getFiles() {
        return files;
    }

    /**
     * POJO representing a file in a Modrinth version.
     */
    public static class ModrinthFile {
        private String url;
        private String filename;
        private boolean primary;
        private long size;

        public String getUrl() {
            return url;
        }

        public String getFilename() {
            return filename;
        }

        public boolean isPrimary() {
            return primary;
        }

        public long getSize() {
            return size;
        }
    }
}

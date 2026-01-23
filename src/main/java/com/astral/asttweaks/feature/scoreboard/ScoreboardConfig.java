package com.astral.asttweaks.feature.scoreboard;

import com.astral.asttweaks.config.ModConfig;

/**
 * Configuration wrapper for scoreboard feature.
 * Delegates to main ModConfig for persistence.
 */
public class ScoreboardConfig {
    private boolean visible = true;
    private int currentPage = 0;

    public boolean isEnabled() {
        return ModConfig.getInstance().scoreboardEnabled;
    }

    public void setEnabled(boolean enabled) {
        ModConfig.getInstance().scoreboardEnabled = enabled;
        ModConfig.getInstance().save();
    }

    public boolean isPagingEnabled() {
        return ModConfig.getInstance().scoreboardPagingEnabled;
    }

    public int getMaxLines() {
        return ModConfig.getInstance().scoreboardMaxLines;
    }

    public int getPageSize() {
        return ModConfig.getInstance().scoreboardPageSize;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(int page) {
        this.currentPage = Math.max(0, page);
    }

    public void nextPage(int totalEntries) {
        int maxPage = getMaxPage(totalEntries);
        if (currentPage < maxPage) {
            currentPage++;
        }
    }

    public void previousPage() {
        if (currentPage > 0) {
            currentPage--;
        }
    }

    public int getMaxPage(int totalEntries) {
        if (!isPagingEnabled() || totalEntries <= getPageSize()) {
            return 0;
        }
        return (totalEntries - 1) / getPageSize();
    }

    // Position getters/setters
    public int getPositionX() {
        return ModConfig.getInstance().scoreboardPositionX;
    }

    public void setPositionX(int x) {
        ModConfig.getInstance().scoreboardPositionX = Math.max(0, Math.min(100, x));
        ModConfig.getInstance().save();
    }

    public int getPositionY() {
        return ModConfig.getInstance().scoreboardPositionY;
    }

    public void setPositionY(int y) {
        ModConfig.getInstance().scoreboardPositionY = Math.max(0, Math.min(100, y));
        ModConfig.getInstance().save();
    }

    // Scale getter/setter
    public float getScale() {
        return ModConfig.getInstance().scoreboardScale;
    }

    public void setScale(float scale) {
        ModConfig.getInstance().scoreboardScale = Math.max(0.5f, Math.min(2.0f, scale));
        ModConfig.getInstance().save();
    }

    // Color getters/setters
    public int getHeaderColor() {
        return ModConfig.getInstance().scoreboardHeaderColor;
    }

    public void setHeaderColor(int color) {
        ModConfig.getInstance().scoreboardHeaderColor = color;
        ModConfig.getInstance().save();
    }

    public int getBodyColor() {
        return ModConfig.getInstance().scoreboardBodyColor;
    }

    public void setBodyColor(int color) {
        ModConfig.getInstance().scoreboardBodyColor = color;
        ModConfig.getInstance().save();
    }

    public int getTextColor() {
        return ModConfig.getInstance().scoreboardTextColor;
    }

    public void setTextColor(int color) {
        ModConfig.getInstance().scoreboardTextColor = color;
        ModConfig.getInstance().save();
    }

    // Rank display
    public boolean isShowRankEnabled() {
        return ModConfig.getInstance().scoreboardShowRank;
    }
}

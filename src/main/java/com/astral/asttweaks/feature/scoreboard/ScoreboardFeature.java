package com.astral.asttweaks.feature.scoreboard;

import com.astral.asttweaks.ASTTweaks;
import com.astral.asttweaks.feature.Feature;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.scoreboard.ScoreboardObjective;

/**
 * Scoreboard enhancement feature with paging support.
 */
public class ScoreboardFeature implements Feature {
    private final ScoreboardConfig config;
    private final ScoreboardRenderer renderer;

    public ScoreboardFeature() {
        this.config = new ScoreboardConfig();
        this.renderer = new ScoreboardRenderer(config);
    }

    @Override
    public String getId() {
        return "scoreboard";
    }

    @Override
    public String getName() {
        return "Scoreboard Helper";
    }

    @Override
    public void init() {
        ASTTweaks.LOGGER.info("Scoreboard feature initialized");
    }

    @Override
    public void tick() {
        // No tick logic needed for scoreboard
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
     * Toggle scoreboard visibility.
     */
    public void toggleVisibility() {
        config.setVisible(!config.isVisible());
        ASTTweaks.LOGGER.debug("Scoreboard visibility: {}", config.isVisible());
    }

    /**
     * Go to next page.
     */
    public void pageDown() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) return;

        ScoreboardObjective objective = getSidebarObjective();
        if (objective != null) {
            int totalEntries = renderer.getTotalEntries(objective);
            config.nextPage(totalEntries);
            ASTTweaks.LOGGER.debug("Scoreboard page: {}", config.getCurrentPage());
        }
    }

    /**
     * Go to previous page.
     */
    public void pageUp() {
        config.previousPage();
        ASTTweaks.LOGGER.debug("Scoreboard page: {}", config.getCurrentPage());
    }

    /**
     * Render the custom scoreboard.
     * Returns true if vanilla rendering should be cancelled.
     */
    public boolean render(MatrixStack matrices, ScoreboardObjective objective) {
        return renderer.render(matrices, objective);
    }

    /**
     * Check if the scoreboard should be visible.
     */
    public boolean shouldRender() {
        return config.isEnabled() && config.isVisible();
    }

    /**
     * Get the current sidebar objective.
     */
    private ScoreboardObjective getSidebarObjective() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) return null;
        // In 1.19.4, sidebar slot is 1
        return client.world.getScoreboard().getObjectiveForSlot(1);
    }

    public ScoreboardConfig getConfig() {
        return config;
    }
}

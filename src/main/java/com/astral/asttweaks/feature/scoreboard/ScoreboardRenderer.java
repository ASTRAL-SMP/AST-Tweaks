package com.astral.asttweaks.feature.scoreboard;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.ScoreboardPlayerScore;
import net.minecraft.scoreboard.Team;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Custom scoreboard renderer with paging support.
 */
public class ScoreboardRenderer extends DrawableHelper {
    private final ScoreboardConfig config;

    public ScoreboardRenderer(ScoreboardConfig config) {
        this.config = config;
    }

    /**
     * Render the scoreboard with paging support.
     * Returns true if rendering was handled, false to use vanilla rendering.
     */
    public boolean render(MatrixStack matrices, ScoreboardObjective objective) {
        if (!config.isEnabled() || !config.isVisible()) {
            return true; // Skip rendering
        }

        // F3デバッグ画面が表示中なら非表示
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.options.debugEnabled) {
            return true; // F3画面表示中はスコアボードを非表示
        }
        Scoreboard scoreboard = objective.getScoreboard();

        // Get all scores sorted by score value
        Collection<ScoreboardPlayerScore> allScores = scoreboard.getAllPlayerScores(objective);
        List<ScoreboardPlayerScore> sortedScores = allScores.stream()
                .filter(score -> score.getPlayerName() != null && !score.getPlayerName().startsWith("#"))
                .collect(Collectors.toList());

        // Sort by score descending
        sortedScores.sort((a, b) -> {
            if (a.getScore() != b.getScore()) {
                return Integer.compare(b.getScore(), a.getScore());
            }
            return a.getPlayerName().compareToIgnoreCase(b.getPlayerName());
        });

        int totalEntries = sortedScores.size();

        // Apply paging if enabled
        List<ScoreboardPlayerScore> displayScores;
        if (config.isPagingEnabled() && totalEntries > config.getPageSize()) {
            int startIndex = config.getCurrentPage() * config.getPageSize();
            int endIndex = Math.min(startIndex + config.getPageSize(), totalEntries);

            // Ensure page is valid
            if (startIndex >= totalEntries) {
                config.setCurrentPage(0);
                startIndex = 0;
                endIndex = Math.min(config.getPageSize(), totalEntries);
            }

            displayScores = sortedScores.subList(startIndex, endIndex);
        } else {
            // Limit to max lines without paging
            displayScores = sortedScores.subList(0, Math.min(config.getMaxLines(), totalEntries));
        }

        // Render the scoreboard
        TextRenderer textRenderer = client.textRenderer;
        int scaledWidth = client.getWindow().getScaledWidth();
        int scaledHeight = client.getWindow().getScaledHeight();

        // Get custom settings
        float scale = config.getScale();
        int positionX = config.getPositionX();
        int positionY = config.getPositionY();
        int headerColor = config.getHeaderColor();
        int bodyColor = config.getBodyColor();
        int textColor = config.getTextColor();

        Text title = objective.getDisplayName();
        int titleWidth = textRenderer.getWidth(title);
        int maxWidth = titleWidth;

        // Calculate max width for all entries
        for (int i = 0; i < displayScores.size(); i++) {
            ScoreboardPlayerScore score = displayScores.get(i);
            Team team = scoreboard.getPlayerTeam(score.getPlayerName());
            Text playerText = Team.decorateName(team, Text.literal(score.getPlayerName()));
            String scoreValue = Formatting.RED + "" + score.getScore();
            int entryWidth = textRenderer.getWidth(playerText) + textRenderer.getWidth(scoreValue) + 4;

            // 順位表示が有効な場合、順位番号の幅を追加
            if (config.isShowRankEnabled()) {
                int rankIndex = config.isPagingEnabled() && sortedScores.size() > config.getPageSize()
                    ? config.getCurrentPage() * config.getPageSize() + i
                    : i;
                String rankText = rankIndex + " ";
                entryWidth += textRenderer.getWidth(rankText);
            }

            maxWidth = Math.max(maxWidth, entryWidth);
        }

        int displayCount = displayScores.size();
        int lineHeight = 9;
        int boxHeight = displayCount * lineHeight;

        // Apply scale transformation
        matrices.push();

        // Calculate position based on percentage (0-100)
        int scaledBoxWidth = (int)((maxWidth + 4) * scale);
        int scaledBoxHeight = (int)((boxHeight + lineHeight) * scale);

        // Position calculation: percentage to pixels
        int anchorX = (int)(scaledWidth * positionX / 100.0);
        int anchorY = (int)(scaledHeight * positionY / 100.0);

        // Calculate the top-left corner for scaling pivot
        int xEnd = anchorX - 3;
        int xStart = xEnd - maxWidth - 2;
        int yStart = anchorY + boxHeight / 3;

        // Apply scaling around the anchor point
        matrices.translate(anchorX, anchorY, 0);
        matrices.scale(scale, scale, 1.0f);
        matrices.translate(-anchorX / scale, -anchorY / scale, 0);

        // Recalculate positions after scale
        xEnd = (int)(anchorX / scale) - 3;
        xStart = xEnd - maxWidth - 2;
        yStart = (int)(anchorY / scale) + boxHeight / 3;

        // Draw title background
        int titleY = yStart - displayCount * lineHeight - lineHeight;
        fill(matrices, xStart - 2, titleY - 1, xEnd, titleY + lineHeight - 1, headerColor);
        textRenderer.draw(matrices, title, (float)(xStart + (maxWidth - titleWidth) / 2), (float)titleY, textColor);

        // Draw entries
        for (int i = 0; i < displayCount; i++) {
            ScoreboardPlayerScore score = displayScores.get(i);
            int y = yStart - (displayCount - i) * lineHeight;

            Team team = scoreboard.getPlayerTeam(score.getPlayerName());
            Text playerText = Team.decorateName(team, Text.literal(score.getPlayerName()));
            String scoreValue = Formatting.RED + "" + score.getScore();

            fill(matrices, xStart - 2, y, xEnd, y + lineHeight, bodyColor);

            int textStartX = xStart;
            if (config.isShowRankEnabled()) {
                int rankIndex = config.isPagingEnabled() && sortedScores.size() > config.getPageSize()
                    ? config.getCurrentPage() * config.getPageSize() + i
                    : i;
                String rankText = rankIndex + " ";
                textRenderer.draw(matrices, rankText, (float)textStartX, (float)y, 0xAAAAAA);
                textStartX += textRenderer.getWidth(rankText);
            }

            textRenderer.draw(matrices, playerText, (float)textStartX, (float)y, textColor);
            textRenderer.draw(matrices, scoreValue, (float)(xEnd - textRenderer.getWidth(scoreValue)), (float)y, textColor);
        }

        // Draw page indicator if paging is active
        if (config.isPagingEnabled() && totalEntries > config.getPageSize()) {
            int currentPage = config.getCurrentPage() + 1;
            int maxPage = config.getMaxPage(totalEntries) + 1;
            String pageText = "Page " + currentPage + "/" + maxPage;
            int pageY = yStart + 2;
            textRenderer.draw(matrices, pageText, (float)(xEnd - textRenderer.getWidth(pageText)), (float)pageY, 0xAAAAAA);
        }

        matrices.pop();

        return true;
    }

    public int getTotalEntries(ScoreboardObjective objective) {
        if (objective == null) return 0;
        return (int) objective.getScoreboard().getAllPlayerScores(objective).stream()
                .filter(score -> score.getPlayerName() != null && !score.getPlayerName().startsWith("#"))
                .count();
    }
}

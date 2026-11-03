package com.astral.asttweaks.feature.scoreboard;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import com.astral.asttweaks.util.gui.Gfx;
import net.minecraft.network.chat.Component;
//? if >=1.20.3 {
import net.minecraft.network.chat.numbers.NumberFormat;
import net.minecraft.network.chat.numbers.StyledFormat;
import net.minecraft.world.scores.PlayerScoreEntry;
//?} else {
/*import net.minecraft.ChatFormatting;
import net.minecraft.world.scores.Score;
*///?}
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

/**
 * Custom scoreboard renderer with paging support.
 */
public class ScoreboardRenderer {
    private final ScoreboardConfig config;

    public ScoreboardRenderer(ScoreboardConfig config) {
        this.config = config;
    }

    /**
     * Render the scoreboard with paging support.
     * Returns true if rendering was handled, false to use vanilla rendering.
     */
    public boolean render(Gfx gfx, Objective objective) {
        if (!config.isEnabled() || !config.isVisible()) {
            return true; // Skip rendering
        }

        // F3デバッグ画面が表示中なら非表示
        Minecraft client = Minecraft.getInstance();
        //? if >=26.1 {
        if (client.debugEntries.isOverlayVisible()) {
        //?} else {
        /*if (client.options.renderDebug) {
        *///?}
            return true; // F3画面表示中はスコアボードを非表示
        }
        Scoreboard scoreboard = objective.getScoreboard();

        // Get all scores sorted by score value
        List<ScoreEntry> sortedScores = collectScores(scoreboard, objective);

        int totalEntries = sortedScores.size();

        // Apply paging if enabled
        List<ScoreEntry> displayScores;
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
        Font textRenderer = client.font;
        int scaledWidth = client.getWindow().getGuiScaledWidth();
        int scaledHeight = client.getWindow().getGuiScaledHeight();

        // Get custom settings
        float scale = config.getScale();
        int positionX = config.getPositionX();
        int positionY = config.getPositionY();
        int headerColor = config.getHeaderColor();
        int bodyColor = config.getBodyColor();
        int textColor = withLegacyAlpha(config.getTextColor());

        Component title = objective.getDisplayName();
        int titleWidth = textRenderer.width(title);
        int maxWidth = titleWidth;

        // Calculate max width for all entries
        for (int i = 0; i < displayScores.size(); i++) {
            ScoreEntry score = displayScores.get(i);
            PlayerTeam team = scoreboard.getPlayersTeam(score.owner());
            Component playerText = PlayerTeam.formatNameForTeam(team, score.ownerName());
            Component scoreValue = score.formattedValue();
            int entryWidth = textRenderer.width(playerText) + textRenderer.width(scoreValue) + 4;

            // 順位表示が有効な場合、順位番号の幅を追加
            if (config.isShowRankEnabled()) {
                int rankIndex = config.isPagingEnabled() && sortedScores.size() > config.getPageSize()
                    ? config.getCurrentPage() * config.getPageSize() + i
                    : i;
                String rankText = rankIndex + " ";
                entryWidth += textRenderer.width(rankText);
            }

            maxWidth = Math.max(maxWidth, entryWidth);
        }

        int displayCount = displayScores.size();
        int lineHeight = 9;
        int boxHeight = displayCount * lineHeight;

        // Apply scale transformation
        gfx.pushPose();

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
        gfx.translate(anchorX, anchorY);
        gfx.scale(scale);
        gfx.translate(-anchorX / scale, -anchorY / scale);

        // Recalculate positions after scale
        xEnd = (int)(anchorX / scale) - 3;
        xStart = xEnd - maxWidth - 2;
        yStart = (int)(anchorY / scale) + boxHeight / 3;

        // Draw title background
        int titleY = yStart - displayCount * lineHeight - lineHeight;
        gfx.fill(xStart - 2, titleY - 1, xEnd, titleY + lineHeight - 1, headerColor);
        gfx.text(textRenderer, title, xStart + (maxWidth - titleWidth) / 2, titleY, textColor, false);

        // Draw entries
        for (int i = 0; i < displayCount; i++) {
            ScoreEntry score = displayScores.get(i);
            int y = yStart - (displayCount - i) * lineHeight;

            PlayerTeam team = scoreboard.getPlayersTeam(score.owner());
            Component playerText = PlayerTeam.formatNameForTeam(team, score.ownerName());
            Component scoreValue = score.formattedValue();

            gfx.fill(xStart - 2, y, xEnd, y + lineHeight, bodyColor);

            int textStartX = xStart;
            if (config.isShowRankEnabled()) {
                int rankIndex = config.isPagingEnabled() && sortedScores.size() > config.getPageSize()
                    ? config.getCurrentPage() * config.getPageSize() + i
                    : i;
                String rankText = rankIndex + " ";
                gfx.text(textRenderer, rankText, textStartX, y, 0xFFAAAAAA, false);
                textStartX += textRenderer.width(rankText);
            }

            gfx.text(textRenderer, playerText, textStartX, y, textColor, false);
            gfx.text(textRenderer, scoreValue, xEnd - textRenderer.width(scoreValue), y, textColor, false);
        }

        // Draw page indicator if paging is active
        if (config.isPagingEnabled() && totalEntries > config.getPageSize()) {
            int currentPage = config.getCurrentPage() + 1;
            int maxPage = config.getMaxPage(totalEntries) + 1;
            String pageText = "Page " + currentPage + "/" + maxPage;
            int pageY = yStart + 2;
            gfx.text(textRenderer, pageText, xEnd - textRenderer.width(pageText), pageY, 0xFFAAAAAA, false);
        }

        gfx.popPose();

        return true;
    }

    // Font.draw in 1.19.4 treated a near-zero alpha as opaque; saved configs may rely on that.
    private static int withLegacyAlpha(int color) {
        return (color & 0xFC000000) == 0 ? color | 0xFF000000 : color;
    }

    public int getTotalEntries(Objective objective) {
        if (objective == null) return 0;
        return (int) collectScores(objective.getScoreboard(), objective).stream()
                .count();
    }

    /** スコア API の世代差（1.20.3 で PlayerScoreEntry になった）を吸収した 1 行分 */
    public record ScoreEntry(String owner, Component ownerName, int value, Component formattedValue) {}

    /** 表示対象のスコアを値の降順（同値は名前順）で返す */
    private static List<ScoreEntry> collectScores(Scoreboard scoreboard, Objective objective) {
        List<ScoreEntry> entries = new java.util.ArrayList<>();
        //? if >=1.20.3 {
        NumberFormat numberFormat = objective.numberFormatOrDefault(StyledFormat.SIDEBAR_DEFAULT);
        for (PlayerScoreEntry score : scoreboard.listPlayerScores(objective)) {
            if (score.isHidden()) continue;
            entries.add(new ScoreEntry(score.owner(), score.ownerName(), score.value(), score.formatValue(numberFormat)));
        }
        //?} else {
        /*for (Score score : scoreboard.getPlayerScores(objective)) {
            if (score.getOwner() == null || score.getOwner().startsWith("#")) continue;
            entries.add(new ScoreEntry(score.getOwner(), Component.literal(score.getOwner()), score.getScore(),
                    Component.literal(String.valueOf(score.getScore())).withStyle(ChatFormatting.RED)));
        }
        *///?}
        entries.sort((a, b) -> {
            if (a.value() != b.value()) {
                return Integer.compare(b.value(), a.value());
            }
            return a.owner().compareToIgnoreCase(b.owner());
        });
        return entries;
    }
}

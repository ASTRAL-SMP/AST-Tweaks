package com.astral.asttweaks.feature.updatechecker;

import com.astral.asttweaks.ASTTweaks;
import com.astral.asttweaks.util.McClient;
import com.astral.asttweaks.util.gui.CompatScreen;
import com.astral.asttweaks.util.gui.Gfx;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
//? if >=26.1 {
import net.minecraft.util.Util;
//?} else {
/*import net.minecraft.Util;
*///?}

/**
 * Xaero-style popup screen shown over the title screen when a new mod
 * version is available. Offers to download the new jar in-place, open the
 * GitHub release page in a browser, dismiss for the session, or skip the version.
 */
public class UpdateAvailableScreen extends CompatScreen {

    private static final int BUTTON_WIDTH = 160;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_SPACING = 6;
    private static final int PANEL_WIDTH = 360;
    private static final int PANEL_PADDING = 14;

    private final Screen parent;
    private final UpdateCheckerFeature feature;
    private final GitHubRelease latestRelease;
    private final String latestVersionNumber;
    private final String currentVersion;

    private Button updateButton;
    private Button openPageButton;
    private Button laterButton;
    private Button skipButton;

    private enum State { PROMPT, DOWNLOADING, SUCCESS, FAILED }

    private volatile State state = State.PROMPT;
    private volatile int progressPercent = 0;
    private volatile String errorMessage = "";

    public UpdateAvailableScreen(Screen parent,
                                 UpdateCheckerFeature feature,
                                 GitHubRelease latestRelease,
                                 String currentVersion) {
        super(Component.translatable("screen." + ASTTweaks.MOD_ID + ".updatechecker.title"));
        this.parent = parent;
        this.feature = feature;
        this.latestRelease = latestRelease;
        this.latestVersionNumber = latestRelease.getVersionNumber();
        this.currentVersion = currentVersion;
    }

    @Override
    protected void init() {
        super.init();

        int totalButtonsHeight = BUTTON_HEIGHT * 4 + BUTTON_SPACING * 3;
        int firstButtonY = this.height / 2 + 10;
        int buttonX = (this.width - BUTTON_WIDTH) / 2;

        this.updateButton = this.addRenderableWidget(Button.builder(
                Component.translatable("screen." + ASTTweaks.MOD_ID + ".updatechecker.button.update"),
                b -> startDownload()
        ).bounds(buttonX, firstButtonY, BUTTON_WIDTH, BUTTON_HEIGHT).build());

        this.openPageButton = this.addRenderableWidget(Button.builder(
                Component.translatable("screen." + ASTTweaks.MOD_ID + ".updatechecker.button.openPage"),
                b -> openProjectPage()
        ).bounds(buttonX, firstButtonY + (BUTTON_HEIGHT + BUTTON_SPACING), BUTTON_WIDTH, BUTTON_HEIGHT).build());

        this.laterButton = this.addRenderableWidget(Button.builder(
                Component.translatable("screen." + ASTTweaks.MOD_ID + ".updatechecker.button.later"),
                b -> onClose()
        ).bounds(buttonX, firstButtonY + (BUTTON_HEIGHT + BUTTON_SPACING) * 2, BUTTON_WIDTH, BUTTON_HEIGHT).build());

        this.skipButton = this.addRenderableWidget(Button.builder(
                Component.translatable("screen." + ASTTweaks.MOD_ID + ".updatechecker.button.skip"),
                b -> skipThisVersion()
        ).bounds(buttonX, firstButtonY + (BUTTON_HEIGHT + BUTTON_SPACING) * 3, BUTTON_WIDTH, BUTTON_HEIGHT).build());

        applyStateToButtons();
    }

    private void applyStateToButtons() {
        boolean prompt = state == State.PROMPT;
        boolean finished = state == State.SUCCESS || state == State.FAILED;
        boolean downloading = state == State.DOWNLOADING;

        if (updateButton != null) {
            updateButton.active = prompt;
            updateButton.visible = !downloading;
        }
        if (openPageButton != null) {
            openPageButton.active = !downloading;
            openPageButton.visible = !downloading;
        }
        if (skipButton != null) {
            skipButton.active = prompt;
            skipButton.visible = prompt;
        }
        if (laterButton != null) {
            laterButton.active = !downloading;
            if (finished) {
                laterButton.setMessage(Component.translatable("screen." + ASTTweaks.MOD_ID + ".updatechecker.button.close"));
            }
        }
    }

    private void startDownload() {
        state = State.DOWNLOADING;
        progressPercent = 0;
        applyStateToButtons();

        UpdateDownloader.downloadAsync(latestRelease, p -> progressPercent = (int) Math.round(p * 100))
                .thenAccept(result -> {
                    Minecraft.getInstance().execute(() -> {
                        if (result.success) {
                            state = State.SUCCESS;
                            feature.markUpdateInstalled(latestVersionNumber);
                        } else {
                            state = State.FAILED;
                            errorMessage = result.message == null ? "" : result.message;
                        }
                        applyStateToButtons();
                    });
                })
                .exceptionally(t -> {
                    Minecraft.getInstance().execute(() -> {
                        state = State.FAILED;
                        errorMessage = t.getMessage() == null ? t.getClass().getSimpleName() : t.getMessage();
                        applyStateToButtons();
                    });
                    return null;
                });
    }

    private void openProjectPage() {
        String url = latestRelease.getHtmlUrl();
        if (url == null || url.isBlank()) {
            String repo = feature.getConfig().getGithubRepo();
            if (repo == null || repo.isBlank()) return;
            url = "https://github.com/" + repo + "/releases";
        }
        if (this.minecraft == null) {
            return;
        }
        final String finalUrl = url;
        //? if >=26.3 {
        /*// 26.3 で URL を開く処理が Blaze3D に移り、ConfirmLinkScreen も URI を受け取るようになった
        java.net.URI uri = java.net.URI.create(finalUrl);
        McClient.setScreen(new ConfirmLinkScreen(confirmed -> {
            if (confirmed) {
                com.mojang.blaze3d.Blaze3D.openUri(uri);
            }
            McClient.setScreen(this);
        }, uri, true));
        *///?} else {
        McClient.setScreen(new ConfirmLinkScreen(confirmed -> {
            if (confirmed) {
                Util.getPlatform().openUri(finalUrl);
            }
            McClient.setScreen(this);
        }, finalUrl, true));
        //?}
    }

    private void skipThisVersion() {
        feature.skipVersion(latestVersionNumber);
        onClose();
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            McClient.setScreen(parent);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void draw(Gfx gfx, int mouseX, int mouseY, float delta) {
        int panelWidth = Math.min(PANEL_WIDTH, this.width - 20);
        int panelX = (this.width - panelWidth) / 2;
        int panelTop = this.height / 2 - 110;
        int panelBottom = this.height / 2 + 130;

        gfx.fill(panelX, panelTop, panelX + panelWidth, panelBottom, 0xC0101010);
        gfx.hLine(panelX, panelX + panelWidth - 1, panelTop, 0xFF404040);
        gfx.hLine(panelX, panelX + panelWidth - 1, panelBottom - 1, 0xFF404040);
        gfx.vLine(panelX, panelTop, panelBottom - 1, 0xFF404040);
        gfx.vLine(panelX + panelWidth - 1, panelTop, panelBottom - 1, 0xFF404040);

        gfx.centeredText(this.font, this.title, this.width / 2, panelTop + PANEL_PADDING, 0xFFFFFFFF);
        gfx.centeredText(
                this.font,
                Component.translatable("screen." + ASTTweaks.MOD_ID + ".updatechecker.heading").withStyle(ChatFormatting.YELLOW),
                this.width / 2,
                panelTop + PANEL_PADDING + 14,
                0xFFFFFFFF
        );
        gfx.centeredText(
                this.font,
                Component.translatable("screen." + ASTTweaks.MOD_ID + ".updatechecker.versions", currentVersion, latestVersionNumber),
                this.width / 2,
                panelTop + PANEL_PADDING + 30,
                0xFFFFFFFF
        );

        renderWrappedDescription(gfx, panelX, panelTop, panelWidth);
        renderStatusLine(gfx, panelX, panelWidth);

        renderWidgets(gfx, mouseX, mouseY, delta);
    }

    private void renderWrappedDescription(Gfx gfx, int panelX, int panelTop, int panelWidth) {
        Component description = Component.translatable("screen." + ASTTweaks.MOD_ID + ".updatechecker.description");
        List<FormattedCharSequence> lines = this.font.split(description, panelWidth - PANEL_PADDING * 2);
        int y = panelTop + PANEL_PADDING + 50;
        for (FormattedCharSequence line : lines) {
            int x = (this.width - this.font.width(line)) / 2;
            gfx.text(this.font, line, x, y, 0xFFCCCCCC);
            y += 11;
        }
    }

    private void renderStatusLine(Gfx gfx, int panelX, int panelWidth) {
        Component status = null;
        int color = 0xFFFFFFFF;
        switch (state) {
            case DOWNLOADING -> {
                status = Component.translatable("screen." + ASTTweaks.MOD_ID + ".updatechecker.status.downloading", progressPercent);
                color = 0xFFFFFF55;
            }
            case SUCCESS -> {
                status = Component.translatable("screen." + ASTTweaks.MOD_ID + ".updatechecker.status.success");
                color = 0xFF55FF55;
            }
            case FAILED -> {
                status = Component.translatable("screen." + ASTTweaks.MOD_ID + ".updatechecker.status.failed", errorMessage);
                color = 0xFFFF5555;
            }
            case PROMPT -> {
            }
        }
        if (status != null) {
            int y = this.height / 2 + 4;
            gfx.centeredText(this.font, status, this.width / 2, y, color);
        }
    }
}

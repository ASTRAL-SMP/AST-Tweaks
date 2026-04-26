package com.astral.asttweaks.feature.updatechecker;

import com.astral.asttweaks.ASTTweaks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ConfirmLinkScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import net.minecraft.util.Formatting;

import java.util.List;

/**
 * Xaero-style popup screen shown over the title screen when a new mod
 * version is available. Offers to download the new jar in-place, open the
 * GitHub release page in a browser, dismiss for the session, or skip the version.
 */
public class UpdateAvailableScreen extends Screen {

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

    private ButtonWidget updateButton;
    private ButtonWidget openPageButton;
    private ButtonWidget laterButton;
    private ButtonWidget skipButton;

    private enum State { PROMPT, DOWNLOADING, SUCCESS, FAILED }

    private volatile State state = State.PROMPT;
    private volatile int progressPercent = 0;
    private volatile String errorMessage = "";

    public UpdateAvailableScreen(Screen parent,
                                 UpdateCheckerFeature feature,
                                 GitHubRelease latestRelease,
                                 String currentVersion) {
        super(Text.translatable("screen." + ASTTweaks.MOD_ID + ".updatechecker.title"));
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

        this.updateButton = this.addDrawableChild(ButtonWidget.builder(
                Text.translatable("screen." + ASTTweaks.MOD_ID + ".updatechecker.button.update"),
                b -> startDownload()
        ).dimensions(buttonX, firstButtonY, BUTTON_WIDTH, BUTTON_HEIGHT).build());

        this.openPageButton = this.addDrawableChild(ButtonWidget.builder(
                Text.translatable("screen." + ASTTweaks.MOD_ID + ".updatechecker.button.openPage"),
                b -> openProjectPage()
        ).dimensions(buttonX, firstButtonY + (BUTTON_HEIGHT + BUTTON_SPACING), BUTTON_WIDTH, BUTTON_HEIGHT).build());

        this.laterButton = this.addDrawableChild(ButtonWidget.builder(
                Text.translatable("screen." + ASTTweaks.MOD_ID + ".updatechecker.button.later"),
                b -> close()
        ).dimensions(buttonX, firstButtonY + (BUTTON_HEIGHT + BUTTON_SPACING) * 2, BUTTON_WIDTH, BUTTON_HEIGHT).build());

        this.skipButton = this.addDrawableChild(ButtonWidget.builder(
                Text.translatable("screen." + ASTTweaks.MOD_ID + ".updatechecker.button.skip"),
                b -> skipThisVersion()
        ).dimensions(buttonX, firstButtonY + (BUTTON_HEIGHT + BUTTON_SPACING) * 3, BUTTON_WIDTH, BUTTON_HEIGHT).build());

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
                laterButton.setMessage(Text.translatable("screen." + ASTTweaks.MOD_ID + ".updatechecker.button.close"));
            }
        }
    }

    private void startDownload() {
        state = State.DOWNLOADING;
        progressPercent = 0;
        applyStateToButtons();

        UpdateDownloader.downloadAsync(latestRelease, p -> progressPercent = (int) Math.round(p * 100))
                .thenAccept(result -> {
                    MinecraftClient.getInstance().execute(() -> {
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
                    MinecraftClient.getInstance().execute(() -> {
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
            url = "https://github.com/" + repo + "/releases/latest";
        }
        if (this.client == null) {
            return;
        }
        final String finalUrl = url;
        this.client.setScreen(new ConfirmLinkScreen(confirmed -> {
            if (confirmed) {
                Util.getOperatingSystem().open(finalUrl);
            }
            this.client.setScreen(this);
        }, finalUrl, true));
    }

    private void skipThisVersion() {
        feature.skipVersion(latestVersionNumber);
        close();
    }

    @Override
    public void close() {
        if (this.client != null) {
            this.client.setScreen(parent);
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        renderBackground(matrices);

        int panelWidth = Math.min(PANEL_WIDTH, this.width - 20);
        int panelX = (this.width - panelWidth) / 2;
        int panelTop = this.height / 2 - 110;
        int panelBottom = this.height / 2 + 130;

        fill(matrices, panelX, panelTop, panelX + panelWidth, panelBottom, 0xC0101010);
        drawHorizontalLine(matrices, panelX, panelX + panelWidth - 1, panelTop, 0xFF404040);
        drawHorizontalLine(matrices, panelX, panelX + panelWidth - 1, panelBottom - 1, 0xFF404040);
        drawVerticalLine(matrices, panelX, panelTop, panelBottom - 1, 0xFF404040);
        drawVerticalLine(matrices, panelX + panelWidth - 1, panelTop, panelBottom - 1, 0xFF404040);

        drawCenteredTextWithShadow(matrices, this.textRenderer, this.title, this.width / 2, panelTop + PANEL_PADDING, 0xFFFFFF);
        drawCenteredTextWithShadow(
                matrices,
                this.textRenderer,
                Text.translatable("screen." + ASTTweaks.MOD_ID + ".updatechecker.heading").formatted(Formatting.YELLOW),
                this.width / 2,
                panelTop + PANEL_PADDING + 14,
                0xFFFFFF
        );
        drawCenteredTextWithShadow(
                matrices,
                this.textRenderer,
                Text.translatable("screen." + ASTTweaks.MOD_ID + ".updatechecker.versions", currentVersion, latestVersionNumber),
                this.width / 2,
                panelTop + PANEL_PADDING + 30,
                0xFFFFFF
        );

        renderWrappedDescription(matrices, panelX, panelTop, panelWidth);
        renderStatusLine(matrices, panelX, panelWidth);

        super.render(matrices, mouseX, mouseY, delta);
    }

    private void renderWrappedDescription(MatrixStack matrices, int panelX, int panelTop, int panelWidth) {
        Text description = Text.translatable("screen." + ASTTweaks.MOD_ID + ".updatechecker.description");
        List<OrderedText> lines = this.textRenderer.wrapLines(description, panelWidth - PANEL_PADDING * 2);
        int y = panelTop + PANEL_PADDING + 50;
        for (OrderedText line : lines) {
            int x = (this.width - this.textRenderer.getWidth(line)) / 2;
            this.textRenderer.drawWithShadow(matrices, line, x, y, 0xCCCCCC);
            y += 11;
        }
    }

    private void renderStatusLine(MatrixStack matrices, int panelX, int panelWidth) {
        Text status = null;
        int color = 0xFFFFFF;
        switch (state) {
            case DOWNLOADING -> {
                status = Text.translatable("screen." + ASTTweaks.MOD_ID + ".updatechecker.status.downloading", progressPercent);
                color = 0xFFFF55;
            }
            case SUCCESS -> {
                status = Text.translatable("screen." + ASTTweaks.MOD_ID + ".updatechecker.status.success");
                color = 0x55FF55;
            }
            case FAILED -> {
                status = Text.translatable("screen." + ASTTweaks.MOD_ID + ".updatechecker.status.failed", errorMessage);
                color = 0xFF5555;
            }
            case PROMPT -> {
            }
        }
        if (status != null) {
            int y = this.height / 2 + 4;
            drawCenteredTextWithShadow(matrices, this.textRenderer, status, this.width / 2, y, color);
        }
    }
}

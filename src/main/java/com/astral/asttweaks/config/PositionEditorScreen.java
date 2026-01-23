package com.astral.asttweaks.config;

import com.astral.asttweaks.ASTTweaks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

/**
 * Screen for adjusting scoreboard position via drag and drop.
 */
public class PositionEditorScreen extends Screen {
    private final Screen parent;
    private final ModConfig config;

    // Temporary values (not saved until confirmed)
    private int tempPositionX;
    private int tempPositionY;
    private float tempScale;

    // Dragging state
    private boolean isDragging = false;
    private int dragOffsetX = 0;
    private int dragOffsetY = 0;

    // Preview scoreboard dimensions
    private int previewWidth = 120;
    private int previewHeight = 100;

    // Scale slider
    private ScaleSlider scaleSlider;

    public PositionEditorScreen(Screen parent) {
        super(Text.translatable("config." + ASTTweaks.MOD_ID + ".scoreboard.positionEditor.title"));
        this.parent = parent;
        this.config = ModConfig.getInstance();
        this.tempPositionX = config.scoreboardPositionX;
        this.tempPositionY = config.scoreboardPositionY;
        this.tempScale = config.scoreboardScale;
    }

    @Override
    protected void init() {
        super.init();

        // Scale slider
        scaleSlider = new ScaleSlider(
                width / 2 - 100, height - 70,
                200, 20,
                Text.translatable("config." + ASTTweaks.MOD_ID + ".scoreboard.scale"),
                (tempScale - 0.5f) / 1.5f  // Convert scale (0.5-2.0) to slider (0-1)
        );
        addDrawableChild(scaleSlider);

        // Confirm button
        addDrawableChild(ButtonWidget.builder(
                Text.translatable("config." + ASTTweaks.MOD_ID + ".scoreboard.positionEditor.confirm"),
                button -> {
                    // Save values
                    config.scoreboardPositionX = tempPositionX;
                    config.scoreboardPositionY = tempPositionY;
                    config.scoreboardScale = tempScale;
                    config.save();
                    close();
                }
        ).dimensions(width / 2 - 105, height - 40, 100, 20).build());

        // Cancel button
        addDrawableChild(ButtonWidget.builder(
                Text.translatable("config." + ASTTweaks.MOD_ID + ".scoreboard.positionEditor.cancel"),
                button -> close()
        ).dimensions(width / 2 + 5, height - 40, 100, 20).build());
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        renderBackground(matrices);

        // Title
        drawCenteredTextWithShadow(matrices, textRenderer, title, width / 2, 20, 0xFFFFFF);

        // Instructions
        Text instructions = Text.translatable("config." + ASTTweaks.MOD_ID + ".scoreboard.positionEditor.instructions");
        drawCenteredTextWithShadow(matrices, textRenderer, instructions, width / 2, 40, 0xAAAAAA);

        // Render preview scoreboard
        renderPreviewScoreboard(matrices, mouseX, mouseY);

        // Scale display
        String scaleText = String.format("%.1fx", tempScale);
        drawTextWithShadow(matrices, textRenderer, Text.literal(scaleText), width / 2 + 110, height - 65, 0xFFFFFF);

        super.render(matrices, mouseX, mouseY, delta);
    }

    private void renderPreviewScoreboard(MatrixStack matrices, int mouseX, int mouseY) {
        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;

        // Calculate actual position from percentage
        int scaledPreviewWidth = (int)(previewWidth * tempScale);
        int scaledPreviewHeight = (int)(previewHeight * tempScale);

        int x = (int)(width * tempPositionX / 100.0) - scaledPreviewWidth;
        int y = (int)(height * tempPositionY / 100.0) - scaledPreviewHeight / 2;

        // Apply scale transform
        matrices.push();
        matrices.translate(x + scaledPreviewWidth, y + scaledPreviewHeight / 2.0, 0);
        matrices.scale(tempScale, tempScale, 1.0f);
        matrices.translate(-(x + scaledPreviewWidth) / tempScale, -(y + scaledPreviewHeight / 2.0) / tempScale, 0);

        int baseX = (int)((x + scaledPreviewWidth) / tempScale) - previewWidth;
        int baseY = (int)((y + scaledPreviewHeight / 2.0) / tempScale) - previewHeight / 2;

        // Header background
        int headerColor = config.scoreboardHeaderColor;
        DrawableHelper.fill(matrices, baseX, baseY, baseX + previewWidth, baseY + 12, headerColor);

        // Header text
        String headerText = "Scoreboard";
        int headerTextX = baseX + (previewWidth - textRenderer.getWidth(headerText)) / 2;
        textRenderer.draw(matrices, headerText, headerTextX, baseY + 2, config.scoreboardTextColor);

        // Body background
        int bodyColor = config.scoreboardBodyColor;
        DrawableHelper.fill(matrices, baseX, baseY + 12, baseX + previewWidth, baseY + previewHeight, bodyColor);

        // Dummy entries
        String[] dummyEntries = {"Player1: 100", "Player2: 85", "Player3: 72", "Player4: 60", "Player5: 45"};
        for (int i = 0; i < dummyEntries.length; i++) {
            int entryY = baseY + 14 + i * 11;
            textRenderer.draw(matrices, dummyEntries[i], baseX + 4, entryY, config.scoreboardTextColor);
        }

        // Border when hovering or dragging
        if (isDragging || isMouseOverPreview(mouseX, mouseY)) {
            // Draw border
            int borderColor = 0xFFFFFF00; // Yellow
            DrawableHelper.fill(matrices, baseX - 1, baseY - 1, baseX + previewWidth + 1, baseY, borderColor);
            DrawableHelper.fill(matrices, baseX - 1, baseY + previewHeight, baseX + previewWidth + 1, baseY + previewHeight + 1, borderColor);
            DrawableHelper.fill(matrices, baseX - 1, baseY, baseX, baseY + previewHeight, borderColor);
            DrawableHelper.fill(matrices, baseX + previewWidth, baseY, baseX + previewWidth + 1, baseY + previewHeight, borderColor);
        }

        matrices.pop();
    }

    private boolean isMouseOverPreview(int mouseX, int mouseY) {
        int scaledPreviewWidth = (int)(previewWidth * tempScale);
        int scaledPreviewHeight = (int)(previewHeight * tempScale);

        int x = (int)(width * tempPositionX / 100.0) - scaledPreviewWidth;
        int y = (int)(height * tempPositionY / 100.0) - scaledPreviewHeight / 2;

        return mouseX >= x && mouseX <= x + scaledPreviewWidth &&
               mouseY >= y && mouseY <= y + scaledPreviewHeight;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && isMouseOverPreview((int)mouseX, (int)mouseY)) {
            isDragging = true;

            int scaledPreviewWidth = (int)(previewWidth * tempScale);
            int scaledPreviewHeight = (int)(previewHeight * tempScale);
            int x = (int)(width * tempPositionX / 100.0) - scaledPreviewWidth;
            int y = (int)(height * tempPositionY / 100.0) - scaledPreviewHeight / 2;

            dragOffsetX = (int)mouseX - x;
            dragOffsetY = (int)mouseY - y;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            isDragging = false;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (isDragging && button == 0) {
            int scaledPreviewWidth = (int)(previewWidth * tempScale);
            int scaledPreviewHeight = (int)(previewHeight * tempScale);

            // Calculate new position
            int newX = (int)mouseX - dragOffsetX + scaledPreviewWidth;
            int newY = (int)mouseY - dragOffsetY + scaledPreviewHeight / 2;

            // Convert to percentage
            tempPositionX = Math.max(0, Math.min(100, (int)(newX * 100.0 / width)));
            tempPositionY = Math.max(0, Math.min(100, (int)(newY * 100.0 / height)));

            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public void close() {
        if (client != null) {
            client.setScreen(parent);
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    /**
     * Custom slider for scale adjustment.
     */
    private class ScaleSlider extends SliderWidget {
        public ScaleSlider(int x, int y, int width, int height, Text text, double value) {
            super(x, y, width, height, text, value);
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            setMessage(Text.translatable("config." + ASTTweaks.MOD_ID + ".scoreboard.scale")
                    .append(": " + String.format("%.1f", tempScale) + "x"));
        }

        @Override
        protected void applyValue() {
            // Convert slider (0-1) to scale (0.5-2.0)
            tempScale = (float)(0.5 + value * 1.5);
            updateMessage();
        }
    }
}

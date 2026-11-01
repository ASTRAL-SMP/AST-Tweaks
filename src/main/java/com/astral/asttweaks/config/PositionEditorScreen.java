package com.astral.asttweaks.config;

import com.astral.asttweaks.ASTTweaks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

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
        super(Component.translatable("config." + ASTTweaks.MOD_ID + ".scoreboard.positionEditor.title"));
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
                Component.translatable("config." + ASTTweaks.MOD_ID + ".scoreboard.scale"),
                (tempScale - 0.5f) / 1.5f  // Convert scale (0.5-2.0) to slider (0-1)
        );
        addRenderableWidget(scaleSlider);

        // Confirm button
        addRenderableWidget(Button.builder(
                Component.translatable("config." + ASTTweaks.MOD_ID + ".scoreboard.positionEditor.confirm"),
                button -> {
                    // Save values
                    config.scoreboardPositionX = tempPositionX;
                    config.scoreboardPositionY = tempPositionY;
                    config.scoreboardScale = tempScale;
                    config.save();
                    onClose();
                }
        ).bounds(width / 2 - 105, height - 40, 100, 20).build());

        // Cancel button
        addRenderableWidget(Button.builder(
                Component.translatable("config." + ASTTweaks.MOD_ID + ".scoreboard.positionEditor.cancel"),
                button -> onClose()
        ).bounds(width / 2 + 5, height - 40, 100, 20).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        // Title
        graphics.centeredText(font, title, width / 2, 20, 0xFFFFFFFF);

        // Instructions
        Component instructions = Component.translatable("config." + ASTTweaks.MOD_ID + ".scoreboard.positionEditor.instructions");
        graphics.centeredText(font, instructions, width / 2, 40, 0xFFAAAAAA);

        // Render preview scoreboard
        renderPreviewScoreboard(graphics, mouseX, mouseY);

        // Scale display
        String scaleText = String.format("%.1fx", tempScale);
        graphics.text(font, Component.literal(scaleText), width / 2 + 110, height - 65, 0xFFFFFFFF);

        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    private void renderPreviewScoreboard(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        Font textRenderer = Minecraft.getInstance().font;

        // Calculate actual position from percentage
        int scaledPreviewWidth = (int)(previewWidth * tempScale);
        int scaledPreviewHeight = (int)(previewHeight * tempScale);

        int x = (int)(width * tempPositionX / 100.0) - scaledPreviewWidth;
        int y = (int)(height * tempPositionY / 100.0) - scaledPreviewHeight / 2;

        // Apply scale transform
        graphics.pose().pushMatrix();
        graphics.pose().translate(x + scaledPreviewWidth, (float) (y + scaledPreviewHeight / 2.0));
        graphics.pose().scale(tempScale, tempScale);
        graphics.pose().translate(-(x + scaledPreviewWidth) / tempScale, (float) (-(y + scaledPreviewHeight / 2.0) / tempScale));

        int baseX = (int)((x + scaledPreviewWidth) / tempScale) - previewWidth;
        int baseY = (int)((y + scaledPreviewHeight / 2.0) / tempScale) - previewHeight / 2;

        // Header background
        int headerColor = config.scoreboardHeaderColor;
        graphics.fill(baseX, baseY, baseX + previewWidth, baseY + 12, headerColor);

        // Header text
        String headerText = "Scoreboard";
        int headerTextX = baseX + (previewWidth - textRenderer.width(headerText)) / 2;
        // Font used to treat an alpha below 4 as opaque; GuiGraphicsExtractor skips alpha-0 text instead
        int textColor = (config.scoreboardTextColor & 0xFC000000) == 0 ? config.scoreboardTextColor | 0xFF000000 : config.scoreboardTextColor;
        graphics.text(textRenderer, headerText, headerTextX, baseY + 2, textColor, false);

        // Body background
        int bodyColor = config.scoreboardBodyColor;
        graphics.fill(baseX, baseY + 12, baseX + previewWidth, baseY + previewHeight, bodyColor);

        // Dummy entries
        String[] dummyEntries = {"Player1: 100", "Player2: 85", "Player3: 72", "Player4: 60", "Player5: 45"};
        for (int i = 0; i < dummyEntries.length; i++) {
            int entryY = baseY + 14 + i * 11;
            graphics.text(textRenderer, dummyEntries[i], baseX + 4, entryY, textColor, false);
        }

        // Border when hovering or dragging
        if (isDragging || isMouseOverPreview(mouseX, mouseY)) {
            // Draw border
            int borderColor = 0xFFFFFF00; // Yellow
            graphics.fill(baseX - 1, baseY - 1, baseX + previewWidth + 1, baseY, borderColor);
            graphics.fill(baseX - 1, baseY + previewHeight, baseX + previewWidth + 1, baseY + previewHeight + 1, borderColor);
            graphics.fill(baseX - 1, baseY, baseX, baseY + previewHeight, borderColor);
            graphics.fill(baseX + previewWidth, baseY, baseX + previewWidth + 1, baseY + previewHeight, borderColor);
        }

        graphics.pose().popMatrix();
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
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        if (event.button() == 0 && isMouseOverPreview((int)mouseX, (int)mouseY)) {
            isDragging = true;

            int scaledPreviewWidth = (int)(previewWidth * tempScale);
            int scaledPreviewHeight = (int)(previewHeight * tempScale);
            int x = (int)(width * tempPositionX / 100.0) - scaledPreviewWidth;
            int y = (int)(height * tempPositionY / 100.0) - scaledPreviewHeight / 2;

            dragOffsetX = (int)mouseX - x;
            dragOffsetY = (int)mouseY - y;
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() == 0) {
            isDragging = false;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
        double mouseX = event.x();
        double mouseY = event.y();
        if (isDragging && event.button() == 0) {
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
        return super.mouseDragged(event, deltaX, deltaY);
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /**
     * Custom slider for scale adjustment.
     */
    private class ScaleSlider extends AbstractSliderButton {
        public ScaleSlider(int x, int y, int width, int height, Component text, double value) {
            super(x, y, width, height, text, value);
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.translatable("config." + ASTTweaks.MOD_ID + ".scoreboard.scale")
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

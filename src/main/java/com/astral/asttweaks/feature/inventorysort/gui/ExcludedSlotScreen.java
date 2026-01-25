package com.astral.asttweaks.feature.inventorysort.gui;

import com.astral.asttweaks.ASTTweaks;
import com.astral.asttweaks.feature.FeatureManager;
import com.astral.asttweaks.feature.inventorysort.InventorySortConfig;
import com.astral.asttweaks.feature.inventorysort.InventorySortFeature;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

/**
 * Screen for configuring excluded inventory slots from sorting.
 * Displays a 36-slot grid representing the player inventory.
 */
public class ExcludedSlotScreen extends Screen {
    private static final int SLOT_SIZE = 20;
    private static final int SLOT_SPACING = 2;
    private static final int GRID_WIDTH = 9;
    private static final int HOTBAR_ROW = 0;
    private static final int MAIN_ROWS = 3;

    private final Screen parent;
    private final InventorySortConfig config;

    private int gridX;
    private int gridY;

    public ExcludedSlotScreen(Screen parent) {
        super(Text.translatable("config." + ASTTweaks.MOD_ID + ".inventorysort.excludedslots.title"));
        this.parent = parent;

        InventorySortFeature feature = (InventorySortFeature) FeatureManager.getInstance().getFeature("inventorysort");
        this.config = feature != null ? feature.getConfig() : null;
    }

    @Override
    protected void init() {
        super.init();

        int totalWidth = GRID_WIDTH * (SLOT_SIZE + SLOT_SPACING) - SLOT_SPACING;
        int totalHeight = 4 * (SLOT_SIZE + SLOT_SPACING) + 10; // 4 rows + gap

        gridX = (this.width - totalWidth) / 2;
        gridY = (this.height - totalHeight) / 2 - 20;

        // Done button
        this.addDrawableChild(ButtonWidget.builder(
                        Text.translatable("gui.done"),
                        button -> this.client.setScreen(parent))
                .dimensions(this.width / 2 - 100, this.height - 30, 200, 20)
                .build());
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        this.renderBackground(matrices);

        // Title
        int titleWidth = this.textRenderer.getWidth(this.title);
        this.textRenderer.drawWithShadow(matrices, this.title, this.width / 2 - titleWidth / 2, 15, 0xFFFFFF);

        // Help text
        Text helpText = Text.translatable("config." + ASTTweaks.MOD_ID + ".inventorysort.excludedslots.help");
        int helpWidth = this.textRenderer.getWidth(helpText);
        this.textRenderer.drawWithShadow(matrices, helpText, this.width / 2 - helpWidth / 2, 30, 0xAAAAAA);

        // Draw main inventory slots (rows 1-3, slots 9-35)
        int mainY = gridY;
        for (int row = 0; row < MAIN_ROWS; row++) {
            for (int col = 0; col < GRID_WIDTH; col++) {
                int slot = 9 + row * GRID_WIDTH + col;
                int x = gridX + col * (SLOT_SIZE + SLOT_SPACING);
                int y = mainY + row * (SLOT_SIZE + SLOT_SPACING);
                drawSlot(matrices, x, y, slot, mouseX, mouseY);
            }
        }

        // Gap between main inventory and hotbar
        int hotbarY = mainY + MAIN_ROWS * (SLOT_SIZE + SLOT_SPACING) + 10;

        // Draw hotbar slots (0-8)
        for (int col = 0; col < GRID_WIDTH; col++) {
            int slot = col;
            int x = gridX + col * (SLOT_SIZE + SLOT_SPACING);
            int y = hotbarY;
            drawSlot(matrices, x, y, slot, mouseX, mouseY);
        }

        // Row labels
        Text mainLabel = Text.literal("Main (9-35)");
        this.textRenderer.drawWithShadow(matrices, mainLabel, gridX - 5 - this.textRenderer.getWidth(mainLabel), mainY + SLOT_SIZE / 2 - 4, 0x888888);

        Text hotbarLabel = Text.literal("Hotbar (0-8)");
        this.textRenderer.drawWithShadow(matrices, hotbarLabel, gridX - 5 - this.textRenderer.getWidth(hotbarLabel), hotbarY + SLOT_SIZE / 2 - 4, 0x888888);

        super.render(matrices, mouseX, mouseY, delta);
    }

    private void drawSlot(MatrixStack matrices, int x, int y, int slot, int mouseX, int mouseY) {
        boolean excluded = config != null && config.isSlotExcluded(slot);
        boolean hovered = mouseX >= x && mouseX < x + SLOT_SIZE && mouseY >= y && mouseY < y + SLOT_SIZE;

        // Background color
        int bgColor;
        if (excluded) {
            bgColor = hovered ? 0xFFCC4444 : 0xFFAA3333; // Red for excluded
        } else {
            bgColor = hovered ? 0xFF666666 : 0xFF444444; // Gray for included
        }

        // Draw background
        fill(matrices, x, y, x + SLOT_SIZE, y + SLOT_SIZE, bgColor);

        // Draw border
        int borderColor = hovered ? 0xFFFFFFFF : 0xFF888888;
        // Top
        fill(matrices, x, y, x + SLOT_SIZE, y + 1, borderColor);
        // Bottom
        fill(matrices, x, y + SLOT_SIZE - 1, x + SLOT_SIZE, y + SLOT_SIZE, borderColor);
        // Left
        fill(matrices, x, y, x + 1, y + SLOT_SIZE, borderColor);
        // Right
        fill(matrices, x + SLOT_SIZE - 1, y, x + SLOT_SIZE, y + SLOT_SIZE, borderColor);

        // Draw slot number
        String slotText = String.valueOf(slot);
        int textWidth = this.textRenderer.getWidth(slotText);
        int textColor = excluded ? 0xFFFFFF : 0xCCCCCC;
        this.textRenderer.drawWithShadow(matrices, slotText,
                x + (SLOT_SIZE - textWidth) / 2,
                y + (SLOT_SIZE - 8) / 2,
                textColor);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && config != null) {
            // Check main inventory slots
            int mainY = gridY;
            for (int row = 0; row < MAIN_ROWS; row++) {
                for (int col = 0; col < GRID_WIDTH; col++) {
                    int slot = 9 + row * GRID_WIDTH + col;
                    int x = gridX + col * (SLOT_SIZE + SLOT_SPACING);
                    int y = mainY + row * (SLOT_SIZE + SLOT_SPACING);

                    if (mouseX >= x && mouseX < x + SLOT_SIZE && mouseY >= y && mouseY < y + SLOT_SIZE) {
                        config.toggleSlotExclusion(slot);
                        return true;
                    }
                }
            }

            // Check hotbar slots
            int hotbarY = mainY + MAIN_ROWS * (SLOT_SIZE + SLOT_SPACING) + 10;
            for (int col = 0; col < GRID_WIDTH; col++) {
                int slot = col;
                int x = gridX + col * (SLOT_SIZE + SLOT_SPACING);
                int y = hotbarY;

                if (mouseX >= x && mouseX < x + SLOT_SIZE && mouseY >= y && mouseY < y + SLOT_SIZE) {
                    config.toggleSlotExclusion(slot);
                    return true;
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void close() {
        this.client.setScreen(parent);
    }
}

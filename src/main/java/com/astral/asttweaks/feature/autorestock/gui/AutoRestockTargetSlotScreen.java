package com.astral.asttweaks.feature.autorestock.gui;

import com.astral.asttweaks.ASTTweaks;
import com.astral.asttweaks.feature.FeatureManager;
import com.astral.asttweaks.feature.autorestock.AutoRestockConfig;
import com.astral.asttweaks.feature.autorestock.AutoRestockFeature;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

/**
 * Slot picker for a single auto restock entry.
 */
public class AutoRestockTargetSlotScreen extends Screen {
    private static final int SLOT_SIZE = 20;
    private static final int SLOT_SPACING = 2;
    private static final int GRID_WIDTH = 9;
    private static final int MAIN_ROWS = 3;
    private static final int[] ARMOR_SLOTS = {39, 38, 37, 36};

    private final Screen parent;
    private final AutoRestockConfig config;
    private final ItemStack stack;

    private int gridX;
    private int gridY;

    public AutoRestockTargetSlotScreen(Screen parent, ItemStack stack) {
        super(Text.translatable("config." + ASTTweaks.MOD_ID + ".autorestock.slotselect.title"));
        this.parent = parent;
        this.stack = stack.copyWithCount(1);
        AutoRestockFeature feature = FeatureManager.getInstance().getAutoRestockFeature();
        this.config = feature != null ? feature.getConfig() : null;
    }

    @Override
    protected void init() {
        super.init();

        int totalWidth = GRID_WIDTH * (SLOT_SIZE + SLOT_SPACING) - SLOT_SPACING;
        int totalHeight = 5 * (SLOT_SIZE + SLOT_SPACING) + 15;
        this.gridX = (this.width - totalWidth) / 2;
        this.gridY = (this.height - totalHeight) / 2 - 10;

        this.addDrawableChild(ButtonWidget.builder(
                        Text.translatable("gui.done"),
                        b -> this.client.setScreen(this.parent))
                .dimensions(this.width / 2 - 100, this.height - 30, 200, 20)
                .build());
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        this.renderBackground(matrices);

        int titleWidth = this.textRenderer.getWidth(this.title);
        this.textRenderer.drawWithShadow(matrices, this.title, this.width / 2 - titleWidth / 2, 15, 0xFFFFFF);

        Text help = Text.translatable("config." + ASTTweaks.MOD_ID + ".autorestock.slotselect.help");
        int helpWidth = this.textRenderer.getWidth(help);
        this.textRenderer.drawWithShadow(matrices, help, this.width / 2 - helpWidth / 2, 30, 0xAAAAAA);

        this.client.getItemRenderer().renderInGuiWithOverrides(matrices, this.stack, this.width / 2 - 48, 42);
        this.textRenderer.drawWithShadow(matrices, this.stack.getName(), this.width / 2 - 28, 47, 0xFFFFFF);

        int armorY = gridY;
        for (int i = 0; i < ARMOR_SLOTS.length; i++) {
            int x = gridX + i * (SLOT_SIZE + SLOT_SPACING);
            drawDisabledSlot(matrices, x, armorY, "A");
        }
        int offhandX = gridX + (GRID_WIDTH - 1) * (SLOT_SIZE + SLOT_SPACING);
        drawSelectableSlot(matrices, offhandX, armorY, AutoRestockConfig.OFFHAND_SLOT, mouseX, mouseY, "Off");

        int mainStartY = armorY + SLOT_SIZE + SLOT_SPACING + 5;
        for (int row = 0; row < MAIN_ROWS; row++) {
            for (int col = 0; col < GRID_WIDTH; col++) {
                int slot = 9 + row * GRID_WIDTH + col;
                int x = gridX + col * (SLOT_SIZE + SLOT_SPACING);
                int y = mainStartY + row * (SLOT_SIZE + SLOT_SPACING);
                drawSelectableSlot(matrices, x, y, slot, mouseX, mouseY, null);
            }
        }

        int hotbarY = mainStartY + MAIN_ROWS * (SLOT_SIZE + SLOT_SPACING) + 10;
        for (int col = 0; col < GRID_WIDTH; col++) {
            int x = gridX + col * (SLOT_SIZE + SLOT_SPACING);
            drawSelectableSlot(matrices, x, hotbarY, col, mouseX, mouseY, null);
        }

        drawLabel(matrices, Text.translatable("gui.asttweaks.inventory.armor"), armorY);
        drawLabel(matrices, Text.translatable("gui.asttweaks.inventory.main"),
                mainStartY + (MAIN_ROWS * (SLOT_SIZE + SLOT_SPACING)) / 2 - SLOT_SIZE / 2);
        drawLabel(matrices, Text.translatable("gui.asttweaks.inventory.hotbar"), hotbarY);

        super.render(matrices, mouseX, mouseY, delta);
    }

    private void drawLabel(MatrixStack matrices, Text text, int y) {
        this.textRenderer.drawWithShadow(matrices, text,
                this.gridX - 5 - this.textRenderer.getWidth(text),
                y + SLOT_SIZE / 2 - 4, 0x888888);
    }

    private void drawDisabledSlot(MatrixStack matrices, int x, int y, String label) {
        fill(matrices, x, y, x + SLOT_SIZE, y + SLOT_SIZE, 0xFF303030);
        drawBorder(matrices, x, y, 0xFF666666);
        int textWidth = this.textRenderer.getWidth(label);
        this.textRenderer.drawWithShadow(matrices, label, x + (SLOT_SIZE - textWidth) / 2, y + 6, 0xAAAAAA);
    }

    private void drawSelectableSlot(MatrixStack matrices, int x, int y, int slot, int mouseX, int mouseY, String overrideLabel) {
        boolean selected = this.config != null && this.config.isTargetSlotSelected(this.stack, slot);
        boolean hovered = mouseX >= x && mouseX < x + SLOT_SIZE && mouseY >= y && mouseY < y + SLOT_SIZE;

        int bgColor;
        if (selected) {
            bgColor = hovered ? 0xFF44AA44 : 0xFF338833;
        } else {
            bgColor = hovered ? 0xFF666666 : 0xFF444444;
        }

        fill(matrices, x, y, x + SLOT_SIZE, y + SLOT_SIZE, bgColor);
        drawBorder(matrices, x, y, hovered ? 0xFFFFFFFF : 0xFF888888);

        String label = overrideLabel != null ? overrideLabel : String.valueOf(slot);
        int textWidth = this.textRenderer.getWidth(label);
        this.textRenderer.drawWithShadow(matrices, label, x + (SLOT_SIZE - textWidth) / 2, y + 6, 0xFFFFFF);
    }

    private void drawBorder(MatrixStack matrices, int x, int y, int color) {
        fill(matrices, x, y, x + SLOT_SIZE, y + 1, color);
        fill(matrices, x, y + SLOT_SIZE - 1, x + SLOT_SIZE, y + SLOT_SIZE, color);
        fill(matrices, x, y, x + 1, y + SLOT_SIZE, color);
        fill(matrices, x + SLOT_SIZE - 1, y, x + SLOT_SIZE, y + SLOT_SIZE, color);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && this.config != null) {
            int armorY = gridY;
            int mainStartY = armorY + SLOT_SIZE + SLOT_SPACING + 5;
            int hotbarY = mainStartY + MAIN_ROWS * (SLOT_SIZE + SLOT_SPACING) + 10;

            int offhandX = gridX + (GRID_WIDTH - 1) * (SLOT_SIZE + SLOT_SPACING);
            if (isIn(mouseX, mouseY, offhandX, armorY)) {
                this.config.toggleTargetSlot(this.stack, AutoRestockConfig.OFFHAND_SLOT);
                return true;
            }

            for (int row = 0; row < MAIN_ROWS; row++) {
                for (int col = 0; col < GRID_WIDTH; col++) {
                    int slot = 9 + row * GRID_WIDTH + col;
                    int x = gridX + col * (SLOT_SIZE + SLOT_SPACING);
                    int y = mainStartY + row * (SLOT_SIZE + SLOT_SPACING);
                    if (isIn(mouseX, mouseY, x, y)) {
                        this.config.toggleTargetSlot(this.stack, slot);
                        return true;
                    }
                }
            }

            for (int col = 0; col < GRID_WIDTH; col++) {
                int x = gridX + col * (SLOT_SIZE + SLOT_SPACING);
                if (isIn(mouseX, mouseY, x, hotbarY)) {
                    this.config.toggleTargetSlot(this.stack, col);
                    return true;
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean isIn(double mouseX, double mouseY, int x, int y) {
        return mouseX >= x && mouseX < x + SLOT_SIZE && mouseY >= y && mouseY < y + SLOT_SIZE;
    }

    @Override
    public void close() {
        this.client.setScreen(this.parent);
    }
}

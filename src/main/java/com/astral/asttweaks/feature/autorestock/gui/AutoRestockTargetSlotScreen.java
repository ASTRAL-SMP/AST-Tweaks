package com.astral.asttweaks.feature.autorestock.gui;

import com.astral.asttweaks.ASTTweaks;
import com.astral.asttweaks.feature.FeatureManager;
import com.astral.asttweaks.feature.autorestock.AutoRestockConfig;
import com.astral.asttweaks.feature.autorestock.AutoRestockFeature;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

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
        super(Component.translatable("config." + ASTTweaks.MOD_ID + ".autorestock.slotselect.title"));
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

        this.addRenderableWidget(Button.builder(
                        Component.translatable("gui.done"),
                        b -> this.minecraft.setScreen(this.parent))
                .bounds(this.width / 2 - 100, this.height - 30, 200, 20)
                .build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        int titleWidth = this.font.width(this.title);
        graphics.text(this.font, this.title, this.width / 2 - titleWidth / 2, 15, 0xFFFFFFFF);

        Component help = Component.translatable("config." + ASTTweaks.MOD_ID + ".autorestock.slotselect.help");
        int helpWidth = this.font.width(help);
        graphics.text(this.font, help, this.width / 2 - helpWidth / 2, 30, 0xFFAAAAAA);

        graphics.item(this.stack, this.width / 2 - 48, 42);
        graphics.text(this.font, this.stack.getHoverName(), this.width / 2 - 28, 47, 0xFFFFFFFF);

        int armorY = gridY;
        for (int i = 0; i < ARMOR_SLOTS.length; i++) {
            int x = gridX + i * (SLOT_SIZE + SLOT_SPACING);
            drawDisabledSlot(graphics, x, armorY, "A");
        }
        int offhandX = gridX + (GRID_WIDTH - 1) * (SLOT_SIZE + SLOT_SPACING);
        drawSelectableSlot(graphics, offhandX, armorY, AutoRestockConfig.OFFHAND_SLOT, mouseX, mouseY, "Off");

        int mainStartY = armorY + SLOT_SIZE + SLOT_SPACING + 5;
        for (int row = 0; row < MAIN_ROWS; row++) {
            for (int col = 0; col < GRID_WIDTH; col++) {
                int slot = 9 + row * GRID_WIDTH + col;
                int x = gridX + col * (SLOT_SIZE + SLOT_SPACING);
                int y = mainStartY + row * (SLOT_SIZE + SLOT_SPACING);
                drawSelectableSlot(graphics, x, y, slot, mouseX, mouseY, null);
            }
        }

        int hotbarY = mainStartY + MAIN_ROWS * (SLOT_SIZE + SLOT_SPACING) + 10;
        for (int col = 0; col < GRID_WIDTH; col++) {
            int x = gridX + col * (SLOT_SIZE + SLOT_SPACING);
            drawSelectableSlot(graphics, x, hotbarY, col, mouseX, mouseY, null);
        }

        drawLabel(graphics, Component.translatable("gui.asttweaks.inventory.armor"), armorY);
        drawLabel(graphics, Component.translatable("gui.asttweaks.inventory.main"),
                mainStartY + (MAIN_ROWS * (SLOT_SIZE + SLOT_SPACING)) / 2 - SLOT_SIZE / 2);
        drawLabel(graphics, Component.translatable("gui.asttweaks.inventory.hotbar"), hotbarY);

        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    private void drawLabel(GuiGraphicsExtractor graphics, Component text, int y) {
        graphics.text(this.font, text,
                this.gridX - 5 - this.font.width(text),
                y + SLOT_SIZE / 2 - 4, 0xFF888888);
    }

    private void drawDisabledSlot(GuiGraphicsExtractor graphics, int x, int y, String label) {
        graphics.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, 0xFF303030);
        drawBorder(graphics, x, y, 0xFF666666);
        int textWidth = this.font.width(label);
        graphics.text(this.font, label, x + (SLOT_SIZE - textWidth) / 2, y + 6, 0xFFAAAAAA);
    }

    private void drawSelectableSlot(GuiGraphicsExtractor graphics, int x, int y, int slot, int mouseX, int mouseY, String overrideLabel) {
        boolean selected = this.config != null && this.config.isTargetSlotSelected(this.stack, slot);
        boolean hovered = mouseX >= x && mouseX < x + SLOT_SIZE && mouseY >= y && mouseY < y + SLOT_SIZE;

        int bgColor;
        if (selected) {
            bgColor = hovered ? 0xFF44AA44 : 0xFF338833;
        } else {
            bgColor = hovered ? 0xFF666666 : 0xFF444444;
        }

        graphics.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, bgColor);
        drawBorder(graphics, x, y, hovered ? 0xFFFFFFFF : 0xFF888888);

        String label = overrideLabel != null ? overrideLabel : String.valueOf(slot);
        int textWidth = this.font.width(label);
        graphics.text(this.font, label, x + (SLOT_SIZE - textWidth) / 2, y + 6, 0xFFFFFFFF);
    }

    private void drawBorder(GuiGraphicsExtractor graphics, int x, int y, int color) {
        graphics.fill(x, y, x + SLOT_SIZE, y + 1, color);
        graphics.fill(x, y + SLOT_SIZE - 1, x + SLOT_SIZE, y + SLOT_SIZE, color);
        graphics.fill(x, y, x + 1, y + SLOT_SIZE, color);
        graphics.fill(x + SLOT_SIZE - 1, y, x + SLOT_SIZE, y + SLOT_SIZE, color);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();
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

        return super.mouseClicked(event, doubleClick);
    }

    private boolean isIn(double mouseX, double mouseY, int x, int y) {
        return mouseX >= x && mouseX < x + SLOT_SIZE && mouseY >= y && mouseY < y + SLOT_SIZE;
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.parent);
    }
}

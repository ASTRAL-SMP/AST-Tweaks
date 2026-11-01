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

/**
 * Screen for configuring Auto Restock protected slots.
 * Protected player-inventory slots are never used as a restock source.
 * Layout mirrors {@code AutoDropProtectedSlotScreen}: armor slots are always
 * protected (non-clickable); all other slots toggle.
 */
public class AutoRestockProtectedSlotScreen extends Screen {
    private static final int SLOT_SIZE = 20;
    private static final int SLOT_SPACING = 2;
    private static final int GRID_WIDTH = 9;
    private static final int MAIN_ROWS = 3;

    private static final int[] ARMOR_SLOTS = {39, 38, 37, 36}; // head, chest, legs, feet
    private static final int OFFHAND_SLOT = AutoRestockConfig.OFFHAND_SLOT;

    private final Screen parent;
    private final AutoRestockConfig config;

    private int gridX;
    private int gridY;

    public AutoRestockProtectedSlotScreen(Screen parent) {
        super(Component.translatable("config." + ASTTweaks.MOD_ID + ".autorestock.protectedslots.title"));
        this.parent = parent;
        AutoRestockFeature feature = FeatureManager.getInstance().getAutoRestockFeature();
        this.config = feature != null ? feature.getConfig() : null;
    }

    @Override
    protected void init() {
        super.init();

        int totalWidth = GRID_WIDTH * (SLOT_SIZE + SLOT_SPACING) - SLOT_SPACING;
        int totalHeight = 5 * (SLOT_SIZE + SLOT_SPACING) + 15;
        gridX = (this.width - totalWidth) / 2;
        gridY = (this.height - totalHeight) / 2 - 10;

        this.addRenderableWidget(Button.builder(
                        Component.translatable("gui.done"),
                        b -> this.minecraft.setScreen(parent))
                .bounds(this.width / 2 - 100, this.height - 30, 200, 20)
                .build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        int titleWidth = this.font.width(this.title);
        graphics.text(this.font, this.title, this.width / 2 - titleWidth / 2, 15, 0xFFFFFFFF);

        Component help = Component.translatable("config." + ASTTweaks.MOD_ID + ".autorestock.protectedslots.help");
        int helpWidth = this.font.width(help);
        graphics.text(this.font, help, this.width / 2 - helpWidth / 2, 30, 0xFFAAAAAA);

        int armorY = gridY;
        for (int i = 0; i < ARMOR_SLOTS.length; i++) {
            int x = gridX + i * (SLOT_SIZE + SLOT_SPACING);
            drawArmorSlot(graphics, x, armorY);
        }
        int offhandX = gridX + (GRID_WIDTH - 1) * (SLOT_SIZE + SLOT_SPACING);
        drawSlot(graphics, offhandX, armorY, OFFHAND_SLOT, mouseX, mouseY, "Off");

        int mainStartY = armorY + SLOT_SIZE + SLOT_SPACING + 5;
        for (int row = 0; row < MAIN_ROWS; row++) {
            for (int col = 0; col < GRID_WIDTH; col++) {
                int slot = 9 + row * GRID_WIDTH + col;
                int x = gridX + col * (SLOT_SIZE + SLOT_SPACING);
                int y = mainStartY + row * (SLOT_SIZE + SLOT_SPACING);
                drawSlot(graphics, x, y, slot, mouseX, mouseY, null);
            }
        }

        int hotbarY = mainStartY + MAIN_ROWS * (SLOT_SIZE + SLOT_SPACING) + 10;
        for (int col = 0; col < GRID_WIDTH; col++) {
            int x = gridX + col * (SLOT_SIZE + SLOT_SPACING);
            drawSlot(graphics, x, hotbarY, col, mouseX, mouseY, null);
        }

        drawLabel(graphics, Component.translatable("gui.asttweaks.inventory.armor"), armorY);
        drawLabel(graphics, Component.translatable("gui.asttweaks.inventory.main"),
                mainStartY + (MAIN_ROWS * (SLOT_SIZE + SLOT_SPACING)) / 2 - SLOT_SIZE / 2);
        drawLabel(graphics, Component.translatable("gui.asttweaks.inventory.hotbar"), hotbarY);

        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    private void drawLabel(GuiGraphicsExtractor graphics, Component text, int y) {
        graphics.text(this.font, text,
                gridX - 5 - this.font.width(text),
                y + SLOT_SIZE / 2 - 4, 0xFF888888);
    }

    private void drawArmorSlot(GuiGraphicsExtractor graphics, int x, int y) {
        graphics.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, 0xFF303030);
        drawBorder(graphics, x, y, 0xFF666666);

        String text = "A";
        int textWidth = this.font.width(text);
        graphics.text(this.font, text,
                x + (SLOT_SIZE - textWidth) / 2,
                y + (SLOT_SIZE - 8) / 2, 0xFFAAAAAA);
    }

    private void drawSlot(GuiGraphicsExtractor graphics, int x, int y, int slot, int mouseX, int mouseY, String overrideLabel) {
        boolean protectedSlot = config != null && config.isSlotProtected(slot);
        boolean hovered = mouseX >= x && mouseX < x + SLOT_SIZE && mouseY >= y && mouseY < y + SLOT_SIZE;

        int bgColor;
        if (protectedSlot) {
            bgColor = hovered ? 0xFFCC4444 : 0xFFAA3333;
        } else {
            bgColor = hovered ? 0xFF666666 : 0xFF444444;
        }
        graphics.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, bgColor);
        drawBorder(graphics, x, y, hovered ? 0xFFFFFFFF : 0xFF888888);

        String text = overrideLabel != null ? overrideLabel : String.valueOf(slot);
        int textWidth = this.font.width(text);
        int textColor = protectedSlot ? 0xFFFFFFFF : 0xFFCCCCCC;
        graphics.text(this.font, text,
                x + (SLOT_SIZE - textWidth) / 2,
                y + (SLOT_SIZE - 8) / 2, textColor);
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
        if (button == 0 && config != null) {
            int armorY = gridY;
            int mainStartY = armorY + SLOT_SIZE + SLOT_SPACING + 5;
            int hotbarY = mainStartY + MAIN_ROWS * (SLOT_SIZE + SLOT_SPACING) + 10;

            int offhandX = gridX + (GRID_WIDTH - 1) * (SLOT_SIZE + SLOT_SPACING);
            if (isIn(mouseX, mouseY, offhandX, armorY)) {
                config.toggleSlotProtection(OFFHAND_SLOT);
                return true;
            }

            for (int row = 0; row < MAIN_ROWS; row++) {
                for (int col = 0; col < GRID_WIDTH; col++) {
                    int slot = 9 + row * GRID_WIDTH + col;
                    int x = gridX + col * (SLOT_SIZE + SLOT_SPACING);
                    int y = mainStartY + row * (SLOT_SIZE + SLOT_SPACING);
                    if (isIn(mouseX, mouseY, x, y)) {
                        config.toggleSlotProtection(slot);
                        return true;
                    }
                }
            }

            for (int col = 0; col < GRID_WIDTH; col++) {
                int x = gridX + col * (SLOT_SIZE + SLOT_SPACING);
                if (isIn(mouseX, mouseY, x, hotbarY)) {
                    config.toggleSlotProtection(col);
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    private boolean isIn(double mx, double my, int x, int y) {
        return mx >= x && mx < x + SLOT_SIZE && my >= y && my < y + SLOT_SIZE;
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }
}

package com.astral.asttweaks.feature.autodrop.gui;

import com.astral.asttweaks.ASTTweaks;
import com.astral.asttweaks.feature.FeatureManager;
import com.astral.asttweaks.feature.autodrop.AutoDropConfig;
import com.astral.asttweaks.feature.autodrop.AutoDropFeature;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

/**
 * Screen for configuring Auto Drop protected slots.
 * Layout (PlayerInventory slot indices shown):
 *   [Armor 39][38][37][36]                                   [Offhand 40]
 *   [Main 9][10][11][12][13][14][15][16][17]
 *   [18][19][20][21][22][23][24][25][26]
 *   [27][28][29][30][31][32][33][34][35]
 *   <gap>
 *   [Hotbar 0][1][2][3][4][5][6][7][8]
 * Armor is always protected (non-clickable); all other slots toggle.
 */
public class AutoDropProtectedSlotScreen extends Screen {
    private static final int SLOT_SIZE = 20;
    private static final int SLOT_SPACING = 2;
    private static final int GRID_WIDTH = 9;
    private static final int MAIN_ROWS = 3;

    private static final int[] ARMOR_SLOTS = {39, 38, 37, 36}; // head, chest, legs, feet
    private static final int OFFHAND_SLOT = 40;

    private final Screen parent;
    private final AutoDropConfig config;

    private int gridX;
    private int gridY;

    public AutoDropProtectedSlotScreen(Screen parent) {
        super(Text.translatable("config." + ASTTweaks.MOD_ID + ".autodrop.protectedslots.title"));
        this.parent = parent;
        AutoDropFeature feature = (AutoDropFeature) FeatureManager.getInstance().getFeature("autodrop");
        this.config = feature != null ? feature.getConfig() : null;
    }

    @Override
    protected void init() {
        super.init();

        int totalWidth = GRID_WIDTH * (SLOT_SIZE + SLOT_SPACING) - SLOT_SPACING;
        int totalHeight = 5 * (SLOT_SIZE + SLOT_SPACING) + 15;
        gridX = (this.width - totalWidth) / 2;
        gridY = (this.height - totalHeight) / 2 - 10;

        this.addDrawableChild(ButtonWidget.builder(
                        Text.translatable("gui.done"),
                        b -> this.client.setScreen(parent))
                .dimensions(this.width / 2 - 100, this.height - 30, 200, 20)
                .build());
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        this.renderBackground(matrices);

        int titleWidth = this.textRenderer.getWidth(this.title);
        this.textRenderer.drawWithShadow(matrices, this.title, this.width / 2 - titleWidth / 2, 15, 0xFFFFFF);

        Text help = Text.translatable("config." + ASTTweaks.MOD_ID + ".autodrop.protectedslots.help");
        int helpWidth = this.textRenderer.getWidth(help);
        this.textRenderer.drawWithShadow(matrices, help, this.width / 2 - helpWidth / 2, 30, 0xAAAAAA);

        int armorY = gridY;
        for (int i = 0; i < 4; i++) {
            int x = gridX + i * (SLOT_SIZE + SLOT_SPACING);
            drawArmorSlot(matrices, x, armorY, ARMOR_SLOTS[i]);
        }
        int offhandX = gridX + (GRID_WIDTH - 1) * (SLOT_SIZE + SLOT_SPACING);
        drawSlot(matrices, offhandX, armorY, OFFHAND_SLOT, mouseX, mouseY, "Off");

        int mainStartY = armorY + SLOT_SIZE + SLOT_SPACING + 5;
        for (int row = 0; row < MAIN_ROWS; row++) {
            for (int col = 0; col < GRID_WIDTH; col++) {
                int slot = 9 + row * GRID_WIDTH + col;
                int x = gridX + col * (SLOT_SIZE + SLOT_SPACING);
                int y = mainStartY + row * (SLOT_SIZE + SLOT_SPACING);
                drawSlot(matrices, x, y, slot, mouseX, mouseY, null);
            }
        }

        int hotbarY = mainStartY + MAIN_ROWS * (SLOT_SIZE + SLOT_SPACING) + 10;
        for (int col = 0; col < GRID_WIDTH; col++) {
            int x = gridX + col * (SLOT_SIZE + SLOT_SPACING);
            drawSlot(matrices, x, hotbarY, col, mouseX, mouseY, null);
        }

        drawLabel(matrices, "Armor", armorY);
        drawLabel(matrices, "Main", mainStartY + (MAIN_ROWS * (SLOT_SIZE + SLOT_SPACING)) / 2 - SLOT_SIZE / 2);
        drawLabel(matrices, "Hotbar", hotbarY);

        super.render(matrices, mouseX, mouseY, delta);
    }

    private void drawLabel(MatrixStack matrices, String text, int y) {
        Text label = Text.literal(text);
        this.textRenderer.drawWithShadow(matrices, label,
                gridX - 5 - this.textRenderer.getWidth(label),
                y + SLOT_SIZE / 2 - 4, 0x888888);
    }

    private void drawArmorSlot(MatrixStack matrices, int x, int y, int slot) {
        int bgColor = 0xFF5A2020;
        fill(matrices, x, y, x + SLOT_SIZE, y + SLOT_SIZE, bgColor);
        drawBorder(matrices, x, y, 0xFF888888);

        String text = "A";
        int textWidth = this.textRenderer.getWidth(text);
        this.textRenderer.drawWithShadow(matrices, text,
                x + (SLOT_SIZE - textWidth) / 2,
                y + (SLOT_SIZE - 8) / 2, 0xFFFFFF);
    }

    private void drawSlot(MatrixStack matrices, int x, int y, int slot, int mouseX, int mouseY, String overrideLabel) {
        boolean protectedSlot = config != null && config.isSlotProtected(slot);
        boolean hovered = mouseX >= x && mouseX < x + SLOT_SIZE && mouseY >= y && mouseY < y + SLOT_SIZE;

        int bgColor;
        if (protectedSlot) {
            bgColor = hovered ? 0xFFCC4444 : 0xFFAA3333;
        } else {
            bgColor = hovered ? 0xFF666666 : 0xFF444444;
        }
        fill(matrices, x, y, x + SLOT_SIZE, y + SLOT_SIZE, bgColor);
        drawBorder(matrices, x, y, hovered ? 0xFFFFFFFF : 0xFF888888);

        String text = overrideLabel != null ? overrideLabel : String.valueOf(slot);
        int textWidth = this.textRenderer.getWidth(text);
        int textColor = protectedSlot ? 0xFFFFFF : 0xCCCCCC;
        this.textRenderer.drawWithShadow(matrices, text,
                x + (SLOT_SIZE - textWidth) / 2,
                y + (SLOT_SIZE - 8) / 2, textColor);
    }

    private void drawBorder(MatrixStack matrices, int x, int y, int color) {
        fill(matrices, x, y, x + SLOT_SIZE, y + 1, color);
        fill(matrices, x, y + SLOT_SIZE - 1, x + SLOT_SIZE, y + SLOT_SIZE, color);
        fill(matrices, x, y, x + 1, y + SLOT_SIZE, color);
        fill(matrices, x + SLOT_SIZE - 1, y, x + SLOT_SIZE, y + SLOT_SIZE, color);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
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
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean isIn(double mx, double my, int x, int y) {
        return mx >= x && mx < x + SLOT_SIZE && my >= y && my < y + SLOT_SIZE;
    }

    @Override
    public void close() {
        this.client.setScreen(parent);
    }
}

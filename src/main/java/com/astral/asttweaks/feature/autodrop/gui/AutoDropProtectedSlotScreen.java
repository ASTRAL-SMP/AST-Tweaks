package com.astral.asttweaks.feature.autodrop.gui;

import com.astral.asttweaks.ASTTweaks;
import com.astral.asttweaks.feature.FeatureManager;
import com.astral.asttweaks.feature.autodrop.AutoDropConfig;
import com.astral.asttweaks.feature.autodrop.AutoDropFeature;
import com.astral.asttweaks.util.McClient;
import com.astral.asttweaks.util.gui.CompatScreen;
import com.astral.asttweaks.util.gui.Gfx;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

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
public class AutoDropProtectedSlotScreen extends CompatScreen {
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
        super(Component.translatable("config." + ASTTweaks.MOD_ID + ".autodrop.protectedslots.title"));
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

        this.addRenderableWidget(Button.builder(
                        Component.translatable("gui.done"),
                        b -> McClient.setScreen(parent))
                .bounds(this.width / 2 - 100, this.height - 30, 200, 20)
                .build());
    }

    @Override
    protected void draw(Gfx gfx, int mouseX, int mouseY, float delta) {
        int titleWidth = this.font.width(this.title);
        gfx.text(this.font, this.title, this.width / 2 - titleWidth / 2, 15, 0xFFFFFFFF);

        Component help = Component.translatable("config." + ASTTweaks.MOD_ID + ".autodrop.protectedslots.help");
        int helpWidth = this.font.width(help);
        gfx.text(this.font, help, this.width / 2 - helpWidth / 2, 30, 0xFFAAAAAA);

        int armorY = gridY;
        for (int i = 0; i < 4; i++) {
            int x = gridX + i * (SLOT_SIZE + SLOT_SPACING);
            drawArmorSlot(gfx, x, armorY, ARMOR_SLOTS[i]);
        }
        int offhandX = gridX + (GRID_WIDTH - 1) * (SLOT_SIZE + SLOT_SPACING);
        drawSlot(gfx, offhandX, armorY, OFFHAND_SLOT, mouseX, mouseY, "Off");

        int mainStartY = armorY + SLOT_SIZE + SLOT_SPACING + 5;
        for (int row = 0; row < MAIN_ROWS; row++) {
            for (int col = 0; col < GRID_WIDTH; col++) {
                int slot = 9 + row * GRID_WIDTH + col;
                int x = gridX + col * (SLOT_SIZE + SLOT_SPACING);
                int y = mainStartY + row * (SLOT_SIZE + SLOT_SPACING);
                drawSlot(gfx, x, y, slot, mouseX, mouseY, null);
            }
        }

        int hotbarY = mainStartY + MAIN_ROWS * (SLOT_SIZE + SLOT_SPACING) + 10;
        for (int col = 0; col < GRID_WIDTH; col++) {
            int x = gridX + col * (SLOT_SIZE + SLOT_SPACING);
            drawSlot(gfx, x, hotbarY, col, mouseX, mouseY, null);
        }

        drawLabel(gfx, "Armor", armorY);
        drawLabel(gfx, "Main", mainStartY + (MAIN_ROWS * (SLOT_SIZE + SLOT_SPACING)) / 2 - SLOT_SIZE / 2);
        drawLabel(gfx, "Hotbar", hotbarY);

        renderWidgets(gfx, mouseX, mouseY, delta);
    }

    private void drawLabel(Gfx gfx, String text, int y) {
        Component label = Component.literal(text);
        gfx.text(this.font, label,
                gridX - 5 - this.font.width(label),
                y + SLOT_SIZE / 2 - 4, 0xFF888888);
    }

    private void drawArmorSlot(Gfx gfx, int x, int y, int slot) {
        int bgColor = 0xFF5A2020;
        gfx.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, bgColor);
        drawBorder(gfx, x, y, 0xFF888888);

        String text = "A";
        int textWidth = this.font.width(text);
        gfx.text(this.font, text,
                x + (SLOT_SIZE - textWidth) / 2,
                y + (SLOT_SIZE - 8) / 2, 0xFFFFFFFF);
    }

    private void drawSlot(Gfx gfx, int x, int y, int slot, int mouseX, int mouseY, String overrideLabel) {
        boolean protectedSlot = config != null && config.isSlotProtected(slot);
        boolean hovered = mouseX >= x && mouseX < x + SLOT_SIZE && mouseY >= y && mouseY < y + SLOT_SIZE;

        int bgColor;
        if (protectedSlot) {
            bgColor = hovered ? 0xFFCC4444 : 0xFFAA3333;
        } else {
            bgColor = hovered ? 0xFF666666 : 0xFF444444;
        }
        gfx.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, bgColor);
        drawBorder(gfx, x, y, hovered ? 0xFFFFFFFF : 0xFF888888);

        String text = overrideLabel != null ? overrideLabel : String.valueOf(slot);
        int textWidth = this.font.width(text);
        int textColor = protectedSlot ? 0xFFFFFFFF : 0xFFCCCCCC;
        gfx.text(this.font, text,
                x + (SLOT_SIZE - textWidth) / 2,
                y + (SLOT_SIZE - 8) / 2, textColor);
    }

    private void drawBorder(Gfx gfx, int x, int y, int color) {
        gfx.fill(x, y, x + SLOT_SIZE, y + 1, color);
        gfx.fill(x, y + SLOT_SIZE - 1, x + SLOT_SIZE, y + SLOT_SIZE, color);
        gfx.fill(x, y, x + 1, y + SLOT_SIZE, color);
        gfx.fill(x + SLOT_SIZE - 1, y, x + SLOT_SIZE, y + SLOT_SIZE, color);
    }

    @Override
    protected boolean onMouseClicked(double mouseX, double mouseY, int button) {
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
        return superMouseClicked(mouseX, mouseY, button);
    }

    private boolean isIn(double mx, double my, int x, int y) {
        return mx >= x && mx < x + SLOT_SIZE && my >= y && my < y + SLOT_SIZE;
    }

    @Override
    public void onClose() {
        McClient.setScreen(parent);
    }
}

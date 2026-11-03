package com.astral.asttweaks.feature.pickprotect.gui;

import com.astral.asttweaks.ASTTweaks;
import com.astral.asttweaks.feature.FeatureManager;
import com.astral.asttweaks.feature.pickprotect.PickProtectConfig;
import com.astral.asttweaks.feature.pickprotect.PickProtectFeature;
import com.astral.asttweaks.util.McClient;
import com.astral.asttweaks.util.gui.CompatScreen;
import com.astral.asttweaks.util.gui.Gfx;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * Hotbar-only whitelist screen for the Pick Protect feature.
 * Shows the current hotbar (slots 0-8) and lets the user toggle protection per slot.
 */
public class PickProtectSlotScreen extends CompatScreen {
    private static final int SLOT_SIZE = 24;
    private static final int SLOT_SPACING = 4;
    private static final int HOTBAR_SIZE = 9;

    private final Screen parent;
    private final PickProtectConfig config;

    private int gridX;
    private int gridY;

    public PickProtectSlotScreen(Screen parent) {
        super(Component.translatable("config." + ASTTweaks.MOD_ID + ".pickprotect.slots.title"));
        this.parent = parent;

        PickProtectFeature feature = (PickProtectFeature) FeatureManager.getInstance().getFeature("pickprotect");
        this.config = feature != null ? feature.getConfig() : null;
    }

    @Override
    protected void init() {
        super.init();

        int totalWidth = HOTBAR_SIZE * (SLOT_SIZE + SLOT_SPACING) - SLOT_SPACING;
        gridX = (this.width - totalWidth) / 2;
        gridY = this.height / 2 - SLOT_SIZE / 2;

        this.addRenderableWidget(Button.builder(
                        Component.translatable("gui.done"),
                        button -> McClient.setScreen(parent))
                .bounds(this.width / 2 - 100, this.height - 30, 200, 20)
                .build());
    }

    @Override
    protected void draw(Gfx gfx, int mouseX, int mouseY, float delta) {
        int titleWidth = this.font.width(this.title);
        gfx.text(this.font, this.title,
                this.width / 2 - titleWidth / 2, 20, 0xFFFFFFFF);

        Component helpText = Component.translatable("config." + ASTTweaks.MOD_ID + ".pickprotect.slots.help");
        int helpWidth = this.font.width(helpText);
        gfx.text(this.font, helpText,
                this.width / 2 - helpWidth / 2, 36, 0xFFAAAAAA);

        ItemStack hoveredStack = null;
        for (int slot = 0; slot < HOTBAR_SIZE; slot++) {
            int x = gridX + slot * (SLOT_SIZE + SLOT_SPACING);
            boolean hovered = mouseX >= x && mouseX < x + SLOT_SIZE
                    && mouseY >= gridY && mouseY < gridY + SLOT_SIZE;
            ItemStack stack = drawSlot(gfx, x, gridY, slot, hovered);
            if (hovered && stack != null && !stack.isEmpty()) {
                hoveredStack = stack;
            }
        }

        // Slot index labels (1-9, matching the in-game hotbar keys)
        for (int slot = 0; slot < HOTBAR_SIZE; slot++) {
            int x = gridX + slot * (SLOT_SIZE + SLOT_SPACING);
            String label = String.valueOf(slot + 1);
            int lw = this.font.width(label);
            gfx.text(this.font, label,
                    x + (SLOT_SIZE - lw) / 2,
                    gridY + SLOT_SIZE + 4, 0xFFCCCCCC);
        }

        renderWidgets(gfx, mouseX, mouseY, delta);

        if (hoveredStack != null) {
            gfx.tooltip(this, this.font, hoveredStack, mouseX, mouseY);
        }
    }

    private ItemStack drawSlot(Gfx gfx, int x, int y, int slot, boolean hovered) {
        boolean protectedSlot = config != null && config.isSlotProtected(slot);

        int bgColor;
        if (protectedSlot) {
            bgColor = hovered ? 0xFFCC4444 : 0xFFAA3333;
        } else {
            bgColor = hovered ? 0xFF666666 : 0xFF444444;
        }
        gfx.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, bgColor);

        int borderColor = hovered ? 0xFFFFFFFF : 0xFF888888;
        gfx.fill(x, y, x + SLOT_SIZE, y + 1, borderColor);
        gfx.fill(x, y + SLOT_SIZE - 1, x + SLOT_SIZE, y + SLOT_SIZE, borderColor);
        gfx.fill(x, y, x + 1, y + SLOT_SIZE, borderColor);
        gfx.fill(x + SLOT_SIZE - 1, y, x + SLOT_SIZE, y + SLOT_SIZE, borderColor);

        ItemStack stack = ItemStack.EMPTY;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            Inventory inv = mc.player.getInventory();
            stack = inv.getItem(slot);
        }

        if (!stack.isEmpty()) {
            int itemX = x + (SLOT_SIZE - 16) / 2;
            int itemY = y + (SLOT_SIZE - 16) / 2;
            gfx.fakeItem(stack, itemX, itemY);
            gfx.itemDecorations(this.font, stack, itemX, itemY);
        }

        return stack;
    }

    @Override
    protected boolean onMouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && config != null) {
            for (int slot = 0; slot < HOTBAR_SIZE; slot++) {
                int x = gridX + slot * (SLOT_SIZE + SLOT_SPACING);
                if (mouseX >= x && mouseX < x + SLOT_SIZE
                        && mouseY >= gridY && mouseY < gridY + SLOT_SIZE) {
                    config.toggleSlot(slot);
                    return true;
                }
            }
        }
        return superMouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void onClose() {
        McClient.setScreen(parent);
    }
}

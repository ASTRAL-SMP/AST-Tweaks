package com.astral.asttweaks.feature.pickprotect.gui;

import com.astral.asttweaks.ASTTweaks;
import com.astral.asttweaks.feature.FeatureManager;
import com.astral.asttweaks.feature.pickprotect.PickProtectConfig;
import com.astral.asttweaks.feature.pickprotect.PickProtectFeature;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

/**
 * Hotbar-only whitelist screen for the Pick Protect feature.
 * Shows the current hotbar (slots 0-8) and lets the user toggle protection per slot.
 */
public class PickProtectSlotScreen extends Screen {
    private static final int SLOT_SIZE = 24;
    private static final int SLOT_SPACING = 4;
    private static final int HOTBAR_SIZE = 9;

    private final Screen parent;
    private final PickProtectConfig config;

    private int gridX;
    private int gridY;

    public PickProtectSlotScreen(Screen parent) {
        super(Text.translatable("config." + ASTTweaks.MOD_ID + ".pickprotect.slots.title"));
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

        this.addDrawableChild(ButtonWidget.builder(
                        Text.translatable("gui.done"),
                        button -> this.client.setScreen(parent))
                .dimensions(this.width / 2 - 100, this.height - 30, 200, 20)
                .build());
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        this.renderBackground(matrices);

        int titleWidth = this.textRenderer.getWidth(this.title);
        this.textRenderer.drawWithShadow(matrices, this.title,
                this.width / 2 - titleWidth / 2, 20, 0xFFFFFF);

        Text helpText = Text.translatable("config." + ASTTweaks.MOD_ID + ".pickprotect.slots.help");
        int helpWidth = this.textRenderer.getWidth(helpText);
        this.textRenderer.drawWithShadow(matrices, helpText,
                this.width / 2 - helpWidth / 2, 36, 0xAAAAAA);

        ItemStack hoveredStack = null;
        for (int slot = 0; slot < HOTBAR_SIZE; slot++) {
            int x = gridX + slot * (SLOT_SIZE + SLOT_SPACING);
            boolean hovered = mouseX >= x && mouseX < x + SLOT_SIZE
                    && mouseY >= gridY && mouseY < gridY + SLOT_SIZE;
            ItemStack stack = drawSlot(matrices, x, gridY, slot, hovered);
            if (hovered && stack != null && !stack.isEmpty()) {
                hoveredStack = stack;
            }
        }

        // Slot index labels (1-9, matching the in-game hotbar keys)
        for (int slot = 0; slot < HOTBAR_SIZE; slot++) {
            int x = gridX + slot * (SLOT_SIZE + SLOT_SPACING);
            String label = String.valueOf(slot + 1);
            int lw = this.textRenderer.getWidth(label);
            this.textRenderer.drawWithShadow(matrices, label,
                    x + (SLOT_SIZE - lw) / 2,
                    gridY + SLOT_SIZE + 4, 0xCCCCCC);
        }

        super.render(matrices, mouseX, mouseY, delta);

        if (hoveredStack != null) {
            this.renderTooltip(matrices, hoveredStack, mouseX, mouseY);
        }
    }

    private ItemStack drawSlot(MatrixStack matrices, int x, int y, int slot, boolean hovered) {
        boolean protectedSlot = config != null && config.isSlotProtected(slot);

        int bgColor;
        if (protectedSlot) {
            bgColor = hovered ? 0xFFCC4444 : 0xFFAA3333;
        } else {
            bgColor = hovered ? 0xFF666666 : 0xFF444444;
        }
        fill(matrices, x, y, x + SLOT_SIZE, y + SLOT_SIZE, bgColor);

        int borderColor = hovered ? 0xFFFFFFFF : 0xFF888888;
        fill(matrices, x, y, x + SLOT_SIZE, y + 1, borderColor);
        fill(matrices, x, y + SLOT_SIZE - 1, x + SLOT_SIZE, y + SLOT_SIZE, borderColor);
        fill(matrices, x, y, x + 1, y + SLOT_SIZE, borderColor);
        fill(matrices, x + SLOT_SIZE - 1, y, x + SLOT_SIZE, y + SLOT_SIZE, borderColor);

        ItemStack stack = ItemStack.EMPTY;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) {
            PlayerInventory inv = mc.player.getInventory();
            stack = inv.getStack(slot);
        }

        if (!stack.isEmpty()) {
            int itemX = x + (SLOT_SIZE - 16) / 2;
            int itemY = y + (SLOT_SIZE - 16) / 2;
            RenderSystem.enableDepthTest();
            this.itemRenderer.renderInGui(matrices, stack, itemX, itemY);
            this.itemRenderer.renderGuiItemOverlay(matrices, this.textRenderer, stack, itemX, itemY);
        }

        return stack;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
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
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void close() {
        this.client.setScreen(parent);
    }
}

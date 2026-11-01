package com.astral.asttweaks.config;

import com.astral.asttweaks.ASTTweaks;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Main configuration menu screen with navigation buttons.
 */
public class MainConfigScreen extends Screen {
    private final Screen parent;

    public MainConfigScreen(Screen parent) {
        super(Component.translatable("config." + ASTTweaks.MOD_ID + ".title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();

        int buttonWidth = 200;
        int buttonHeight = 20;
        int centerX = width / 2 - buttonWidth / 2;
        int startY = height / 2 - 40;

        // Settings button (opens Cloth Config screen)
        addRenderableWidget(Button.builder(
                Component.translatable("config." + ASTTweaks.MOD_ID + ".menu.settings"),
                button -> minecraft.setScreen(ConfigScreen.createClothConfigScreen(this))
        ).bounds(centerX, startY, buttonWidth, buttonHeight).build());

        // Position editor button
        addRenderableWidget(Button.builder(
                Component.translatable("config." + ASTTweaks.MOD_ID + ".menu.positionEditor"),
                button -> minecraft.setScreen(new PositionEditorScreen(this))
        ).bounds(centerX, startY + 30, buttonWidth, buttonHeight).build());

        // Done button
        addRenderableWidget(Button.builder(
                Component.translatable("gui.done"),
                button -> onClose()
        ).bounds(centerX, startY + 70, buttonWidth, buttonHeight).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        graphics.centeredText(font, title, width / 2, 20, 0xFFFFFFFF);
        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }
}

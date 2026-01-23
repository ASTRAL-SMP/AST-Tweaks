package com.astral.asttweaks.config;

import com.astral.asttweaks.ASTTweaks;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

/**
 * Main configuration menu screen with navigation buttons.
 */
public class MainConfigScreen extends Screen {
    private final Screen parent;

    public MainConfigScreen(Screen parent) {
        super(Text.translatable("config." + ASTTweaks.MOD_ID + ".title"));
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
        addDrawableChild(ButtonWidget.builder(
                Text.translatable("config." + ASTTweaks.MOD_ID + ".menu.settings"),
                button -> client.setScreen(ConfigScreen.createClothConfigScreen(this))
        ).dimensions(centerX, startY, buttonWidth, buttonHeight).build());

        // Position editor button
        addDrawableChild(ButtonWidget.builder(
                Text.translatable("config." + ASTTweaks.MOD_ID + ".menu.positionEditor"),
                button -> client.setScreen(new PositionEditorScreen(this))
        ).dimensions(centerX, startY + 30, buttonWidth, buttonHeight).build());

        // Done button
        addDrawableChild(ButtonWidget.builder(
                Text.translatable("gui.done"),
                button -> close()
        ).dimensions(centerX, startY + 70, buttonWidth, buttonHeight).build());
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        renderBackground(matrices);
        drawCenteredTextWithShadow(matrices, textRenderer, title, width / 2, 20, 0xFFFFFF);
        super.render(matrices, mouseX, mouseY, delta);
    }

    @Override
    public void close() {
        client.setScreen(parent);
    }
}

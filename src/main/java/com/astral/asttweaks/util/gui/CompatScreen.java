package com.astral.asttweaks.util.gui;

//? if >=26.1 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
//?} else if >=1.20 {
/*import net.minecraft.client.gui.GuiGraphics;
*///?} else {
/*import com.mojang.blaze3d.vertex.PoseStack;
*///?}
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * 描画・入力メソッドのシグネチャ差を吸収した Screen。
 * サブクラスは draw / onMouseClicked などのフックを実装し、親の処理が必要なら renderWidgets / super* を呼ぶ。
 */
public abstract class CompatScreen extends Screen {
    protected CompatScreen(Component title) {
        super(title);
    }

    /** 画面全体の描画。登録済みウィジェットは renderWidgets で描く */
    protected abstract void draw(Gfx gfx, int mouseX, int mouseY, float delta);

    protected boolean onMouseClicked(double mouseX, double mouseY, int button) {
        return superMouseClicked(mouseX, mouseY, button);
    }

    protected boolean onMouseReleased(double mouseX, double mouseY, int button) {
        return superMouseReleased(mouseX, mouseY, button);
    }

    protected boolean onMouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        return superMouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    /** amount は縦スクロール量 */
    protected boolean onMouseScrolled(double mouseX, double mouseY, double amount) {
        return superMouseScrolled(mouseX, mouseY, amount);
    }

    protected boolean onKeyPressed(int keyCode, int scancode, int modifiers) {
        return superKeyPressed(keyCode, scancode, modifiers);
    }

    protected boolean onKeyReleased(int keyCode, int scancode, int modifiers) {
        return superKeyReleased(keyCode, scancode, modifiers);
    }

    protected boolean onCharTyped(int codepoint, int modifiers) {
        return superCharTyped(codepoint, modifiers);
    }

    //? if >=26.1 {
    @Override
    public final void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        draw(new Gfx(graphics), mouseX, mouseY, delta);
    }

    protected void renderWidgets(Gfx gfx, int mouseX, int mouseY, float delta) {
        super.extractRenderState(gfx.g, mouseX, mouseY, delta);
    }

    @Override
    public final boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        return GuiEvents.withMouse(event, doubleClick, () -> onMouseClicked(event.x(), event.y(), event.button()));
    }

    protected boolean superMouseClicked(double mouseX, double mouseY, int button) {
        return super.mouseClicked(GuiEvents.mouseEvent(mouseX, mouseY, button), GuiEvents.doubleClick());
    }

    @Override
    public final boolean mouseReleased(MouseButtonEvent event) {
        return GuiEvents.withMouse(event, false, () -> onMouseReleased(event.x(), event.y(), event.button()));
    }

    protected boolean superMouseReleased(double mouseX, double mouseY, int button) {
        return super.mouseReleased(GuiEvents.mouseEvent(mouseX, mouseY, button));
    }

    @Override
    public final boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
        return GuiEvents.withMouse(event, false, () -> onMouseDragged(event.x(), event.y(), event.button(), deltaX, deltaY));
    }

    protected boolean superMouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        return super.mouseDragged(GuiEvents.mouseEvent(mouseX, mouseY, button), deltaX, deltaY);
    }

    @Override
    public final boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        return onMouseScrolled(mouseX, mouseY, scrollY);
    }

    protected boolean superMouseScrolled(double mouseX, double mouseY, double amount) {
        return super.mouseScrolled(mouseX, mouseY, 0, amount);
    }

    @Override
    public final boolean keyPressed(KeyEvent event) {
        return GuiEvents.withKey(event, () -> onKeyPressed(event.key(), GuiEvents.scancode(event), event.modifiers()));
    }

    protected boolean superKeyPressed(int keyCode, int scancode, int modifiers) {
        return super.keyPressed(GuiEvents.keyEvent(keyCode, scancode, modifiers));
    }

    @Override
    public final boolean keyReleased(KeyEvent event) {
        return GuiEvents.withKey(event, () -> onKeyReleased(event.key(), GuiEvents.scancode(event), event.modifiers()));
    }

    protected boolean superKeyReleased(int keyCode, int scancode, int modifiers) {
        return super.keyReleased(GuiEvents.keyEvent(keyCode, scancode, modifiers));
    }

    @Override
    public final boolean charTyped(CharacterEvent event) {
        return onCharTyped(event.codepoint(), 0);
    }

    protected boolean superCharTyped(int codepoint, int modifiers) {
        return super.charTyped(new CharacterEvent(codepoint));
    }
    //?} else {
    /*@Override
    //? if >=1.20 {
    public final void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
    //?} else {
    /^public final void render(PoseStack graphics, int mouseX, int mouseY, float delta) {
    ^///?}
        Gfx gfx = new Gfx(graphics);
        gfx.background(this);
        draw(gfx, mouseX, mouseY, delta);
    }

    protected void renderWidgets(Gfx gfx, int mouseX, int mouseY, float delta) {
        super.render(gfx.g, mouseX, mouseY, delta);
    }

    @Override
    public final boolean mouseClicked(double mouseX, double mouseY, int button) {
        return onMouseClicked(mouseX, mouseY, button);
    }

    protected boolean superMouseClicked(double mouseX, double mouseY, int button) {
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public final boolean mouseReleased(double mouseX, double mouseY, int button) {
        return onMouseReleased(mouseX, mouseY, button);
    }

    protected boolean superMouseReleased(double mouseX, double mouseY, int button) {
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public final boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        return onMouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    protected boolean superMouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public final boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        return onMouseScrolled(mouseX, mouseY, amount);
    }

    protected boolean superMouseScrolled(double mouseX, double mouseY, double amount) {
        return super.mouseScrolled(mouseX, mouseY, amount);
    }

    @Override
    public final boolean keyPressed(int keyCode, int scancode, int modifiers) {
        return onKeyPressed(keyCode, scancode, modifiers);
    }

    protected boolean superKeyPressed(int keyCode, int scancode, int modifiers) {
        return super.keyPressed(keyCode, scancode, modifiers);
    }

    @Override
    public final boolean keyReleased(int keyCode, int scancode, int modifiers) {
        return onKeyReleased(keyCode, scancode, modifiers);
    }

    protected boolean superKeyReleased(int keyCode, int scancode, int modifiers) {
        return super.keyReleased(keyCode, scancode, modifiers);
    }

    @Override
    public final boolean charTyped(char chr, int modifiers) {
        return onCharTyped(chr, modifiers);
    }

    protected boolean superCharTyped(int codepoint, int modifiers) {
        return super.charTyped((char) codepoint, modifiers);
    }
    *///?}
}

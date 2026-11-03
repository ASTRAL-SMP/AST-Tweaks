package com.astral.asttweaks.util.gui;

//? if >=26.1 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
//?} else if >=1.20 {
/*import net.minecraft.client.gui.GuiGraphics;
*///?} else {
/*import com.mojang.blaze3d.vertex.PoseStack;
*///?}
import net.minecraft.client.gui.components.ObjectSelectionList;

/**
 * ObjectSelectionList.Entry の描画・クリックのシグネチャ差を吸収する。
 */
public abstract class CompatListEntry<E extends CompatListEntry<E>> extends ObjectSelectionList.Entry<E> {

    /** x, y, entryWidth, entryHeight は行の内容領域 */
    protected abstract void drawEntry(Gfx gfx, int x, int y, int entryWidth, int entryHeight,
                                      int mouseX, int mouseY, boolean hovered, float delta);

    protected boolean onMouseClicked(double mouseX, double mouseY, int button) {
        return false;
    }

    //? if >=26.1 {
    @Override
    public final void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float delta) {
        drawEntry(new Gfx(graphics), getContentX(), getContentY(), getContentWidth(), getContentHeight(),
                mouseX, mouseY, hovered, delta);
    }

    @Override
    public final boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        return GuiEvents.withMouse(event, doubleClick, () -> onMouseClicked(event.x(), event.y(), event.button()));
    }
    //?} else {
    /*@Override
    //? if >=1.20 {
    public final void render(GuiGraphics graphics, int index, int y, int x, int entryWidth, int entryHeight,
    //?} else {
    /^public final void render(PoseStack graphics, int index, int y, int x, int entryWidth, int entryHeight,
    ^///?}
                             int mouseX, int mouseY, boolean hovered, float delta) {
        drawEntry(new Gfx(graphics), x, y, entryWidth, entryHeight, mouseX, mouseY, hovered, delta);
    }

    @Override
    public final boolean mouseClicked(double mouseX, double mouseY, int button) {
        return onMouseClicked(mouseX, mouseY, button);
    }
    *///?}
}

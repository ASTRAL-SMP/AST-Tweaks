package com.astral.asttweaks.util.gui;

//? if >=26.1 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
//?} else if >=1.20 {
/*import net.minecraft.client.gui.GuiGraphics;
*///?} else {
/*import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
*///?}
import net.minecraft.resources.Identifier;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

/**
 * GUI 描画 API の世代差（PoseStack → GuiGraphics → GuiGraphicsExtractor）を吸収する。
 * 色は常に ARGB で渡す。
 */
public final class Gfx {
    //? if >=26.1 {
    public final GuiGraphicsExtractor g;
    public Gfx(GuiGraphicsExtractor g) { this.g = g; }
    //?} else if >=1.20 {
    /*public final GuiGraphics g;
    public Gfx(GuiGraphics g) { this.g = g; }
    *///?} else {
    /*public final PoseStack g;
    public Gfx(PoseStack g) { this.g = g; }
    *///?}

    public void fill(int x1, int y1, int x2, int y2, int color) {
        //? if >=1.20 {
        g.fill(x1, y1, x2, y2, color);
        //?} else {
        /*GuiComponent.fill(g, x1, y1, x2, y2, color);
        *///?}
    }

    /** minX..maxX（両端含む）の横線 */
    public void hLine(int minX, int maxX, int y, int color) {
        //? if >=26.1 {
        g.horizontalLine(minX, maxX, y, color);
        //?} else if >=1.20 {
        /*g.hLine(minX, maxX, y, color);
        *///?} else {
        /*GuiComponent.fill(g, Math.min(minX, maxX), y, Math.max(minX, maxX) + 1, y + 1, color);
        *///?}
    }

    /** minY..maxY（両端含まない）の縦線 */
    public void vLine(int x, int minY, int maxY, int color) {
        //? if >=26.1 {
        g.verticalLine(x, minY, maxY, color);
        //?} else if >=1.20 {
        /*g.vLine(x, minY, maxY, color);
        *///?} else {
        /*GuiComponent.fill(g, x, Math.min(minY, maxY) + 1, x + 1, Math.max(minY, maxY), color);
        *///?}
    }

    public void text(Font font, Component text, int x, int y, int color) {
        text(font, text, x, y, color, true);
    }

    public void text(Font font, Component text, int x, int y, int color, boolean shadow) {
        //? if >=26.1 {
        g.text(font, text, x, y, color, shadow);
        //?} else if >=1.20 {
        /*g.drawString(font, text, x, y, color, shadow);
        *///?} else {
        /*if (shadow) font.drawShadow(g, text, x, y, color); else font.draw(g, text, x, y, color);
        *///?}
    }

    public void text(Font font, String text, int x, int y, int color) {
        text(font, text, x, y, color, true);
    }

    public void text(Font font, String text, int x, int y, int color, boolean shadow) {
        //? if >=26.1 {
        g.text(font, text, x, y, color, shadow);
        //?} else if >=1.20 {
        /*g.drawString(font, text, x, y, color, shadow);
        *///?} else {
        /*if (shadow) font.drawShadow(g, text, x, y, color); else font.draw(g, text, x, y, color);
        *///?}
    }

    public void text(Font font, FormattedCharSequence text, int x, int y, int color) {
        text(font, text, x, y, color, true);
    }

    public void text(Font font, FormattedCharSequence text, int x, int y, int color, boolean shadow) {
        //? if >=26.1 {
        g.text(font, text, x, y, color, shadow);
        //?} else if >=1.20 {
        /*g.drawString(font, text, x, y, color, shadow);
        *///?} else {
        /*if (shadow) font.drawShadow(g, text, x, y, color); else font.draw(g, text, x, y, color);
        *///?}
    }

    public void centeredText(Font font, Component text, int centerX, int y, int color) {
        //? if >=26.1 {
        g.centeredText(font, text, centerX, y, color);
        //?} else if >=1.20 {
        /*g.drawCenteredString(font, text, centerX, y, color);
        *///?} else {
        /*GuiComponent.drawCenteredString(g, font, text, centerX, y, color);
        *///?}
    }

    public void centeredText(Font font, String text, int centerX, int y, int color) {
        //? if >=26.1 {
        g.centeredText(font, text, centerX, y, color);
        //?} else if >=1.20 {
        /*g.drawCenteredString(font, text, centerX, y, color);
        *///?} else {
        /*GuiComponent.drawCenteredString(g, font, text, centerX, y, color);
        *///?}
    }

    public void item(ItemStack stack, int x, int y) {
        //? if >=26.1 {
        g.item(stack, x, y);
        //?} else if >=1.20 {
        /*g.renderItem(stack, x, y);
        *///?} else {
        /*Minecraft.getInstance().getItemRenderer().renderAndDecorateItem(g, stack, x, y);
        *///?}
    }

    public void fakeItem(ItemStack stack, int x, int y) {
        //? if >=26.1 {
        g.fakeItem(stack, x, y);
        //?} else if >=1.20 {
        /*g.renderFakeItem(stack, x, y);
        *///?} else {
        /*Minecraft.getInstance().getItemRenderer().renderAndDecorateFakeItem(g, stack, x, y);
        *///?}
    }

    public void itemDecorations(Font font, ItemStack stack, int x, int y) {
        //? if >=26.1 {
        g.itemDecorations(font, stack, x, y);
        //?} else if >=1.20 {
        /*g.renderItemDecorations(font, stack, x, y);
        *///?} else {
        /*Minecraft.getInstance().getItemRenderer().renderGuiItemDecorations(g, font, stack, x, y);
        *///?}
    }

    public void tooltip(Screen screen, Font font, Component text, int mouseX, int mouseY) {
        //? if >=26.1 {
        g.setTooltipForNextFrame(font, text, mouseX, mouseY);
        //?} else if >=1.20 {
        /*g.renderTooltip(font, text, mouseX, mouseY);
        *///?} else {
        /*screen.renderTooltip(g, text, mouseX, mouseY);
        *///?}
    }

    public void tooltip(Screen screen, Font font, List<Component> lines, int mouseX, int mouseY) {
        //? if >=26.1 {
        g.setTooltipForNextFrame(font, lines, Optional.empty(), mouseX, mouseY);
        //?} else if >=1.20 {
        /*g.renderComponentTooltip(font, lines, mouseX, mouseY);
        *///?} else {
        /*screen.renderComponentTooltip(g, lines, mouseX, mouseY);
        *///?}
    }

    public void tooltip(Screen screen, Font font, ItemStack stack, int mouseX, int mouseY) {
        //? if >=26.1 {
        g.setTooltipForNextFrame(font, stack, mouseX, mouseY);
        //?} else if >=1.20 {
        /*g.renderTooltip(font, stack, mouseX, mouseY);
        *///?} else {
        /*screen.renderComponentTooltip(g, screen.getTooltipFromItem(stack), mouseX, mouseY);
        *///?}
    }

    /** 256x256 のテクスチャの一部を描く */
    public void texture(Identifier texture, int x, int y, int u, int v, int width, int height) {
        //? if >=26.1 {
        g.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, u, v, width, height, 256, 256);
        //?} else if >=1.20 {
        /*g.blit(texture, x, y, u, v, width, height);
        *///?} else {
        /*RenderSystem.setShaderTexture(0, texture);
        GuiComponent.blit(g, x, y, u, v, width, height, 256, 256);
        *///?}
    }

    /** ウィジェットやリストを描く */
    public void widget(Renderable widget, int mouseX, int mouseY, float delta) {
        //? if >=26.1 {
        widget.extractRenderState(g, mouseX, mouseY, delta);
        //?} else {
        /*widget.render(g, mouseX, mouseY, delta);
        *///?}
    }

    /** 画面の背景（暗転やダート）。26.1+ は Screen 側が描くので何もしない */
    public void background(Screen screen) {
        //? if <26.1 {
        /*screen.renderBackground(g);
        *///?}
    }

    public void pushPose() {
        //? if >=26.1 {
        g.pose().pushMatrix();
        //?} else if >=1.20 {
        /*g.pose().pushPose();
        *///?} else {
        /*g.pushPose();
        *///?}
    }

    public void popPose() {
        //? if >=26.1 {
        g.pose().popMatrix();
        //?} else if >=1.20 {
        /*g.pose().popPose();
        *///?} else {
        /*g.popPose();
        *///?}
    }

    public void translate(float x, float y) {
        //? if >=26.1 {
        g.pose().translate(x, y);
        //?} else if >=1.20 {
        /*g.pose().translate(x, y, 0);
        *///?} else {
        /*g.translate(x, y, 0);
        *///?}
    }

    public void scale(float scale) {
        //? if >=26.1 {
        g.pose().scale(scale, scale);
        //?} else if >=1.20 {
        /*g.pose().scale(scale, scale, 1);
        *///?} else {
        /*g.scale(scale, scale, 1);
        *///?}
    }

    public int guiWidth() {
        //? if >=1.20 {
        return g.guiWidth();
        //?} else {
        /*return Minecraft.getInstance().getWindow().getGuiScaledWidth();
        *///?}
    }

    public int guiHeight() {
        //? if >=1.20 {
        return g.guiHeight();
        //?} else {
        /*return Minecraft.getInstance().getWindow().getGuiScaledHeight();
        *///?}
    }
}

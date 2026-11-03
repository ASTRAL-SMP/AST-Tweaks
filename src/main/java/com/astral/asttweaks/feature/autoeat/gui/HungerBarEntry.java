package com.astral.asttweaks.feature.autoeat.gui;

import me.shedaniel.clothconfig2.gui.entries.TooltipListEntry;
//? if >=26.1 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
//?} else if >=1.20 {
/*import net.minecraft.client.gui.GuiGraphics;
*///?} else {
/*import com.mojang.blaze3d.vertex.PoseStack;
*///?}
import com.astral.asttweaks.util.Ids;
import com.astral.asttweaks.util.gui.Gfx;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * A Cloth Config entry that displays a hunger bar visualization.
 */
public class HungerBarEntry extends TooltipListEntry<Object> {
    //? if >=26.1 {
    private static final Identifier FOOD_EMPTY_SPRITE = Ids.mc("hud/food_empty");
    private static final Identifier FOOD_HALF_SPRITE = Ids.mc("hud/food_half");
    private static final Identifier FOOD_FULL_SPRITE = Ids.mc("hud/food_full");
    //?} else {
    /*// 1.20.2 より前は HUD アイコンが 1 枚のテクスチャにまとまっている
    private static final Identifier ICONS = Ids.mc("textures/gui/icons.png");
    *///?}
    private final Supplier<Integer> hungerThreshold;

    public HungerBarEntry(Supplier<Integer> hungerThreshold) {
        super(Component.literal(""), null);
        this.hungerThreshold = hungerThreshold;
    }

    //? if >=26.1 {
    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean isSelected, float delta) {
        super.extractRenderState(graphics, index, y, x, entryWidth, entryHeight, mouseX, mouseY, isSelected, delta);
    //?} else if >=1.20 {
    /*@Override
    public void render(GuiGraphics graphics, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean isSelected, float delta) {
        super.render(graphics, index, y, x, entryWidth, entryHeight, mouseX, mouseY, isSelected, delta);
    *///?} else {
    /*@Override
    public void render(PoseStack graphics, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean isSelected, float delta) {
        super.render(graphics, index, y, x, entryWidth, entryHeight, mouseX, mouseY, isSelected, delta);
    *///?}
        Gfx gfx = new Gfx(graphics);

        int threshold = this.hungerThreshold.get();
        int fullShanks = threshold / 2;
        boolean hasHalfShank = threshold % 2 == 1;

        int barX = x + entryWidth / 2 - 45;
        for (int i = 0; i < 10; i++) {
            //? if >=26.1 {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, FOOD_EMPTY_SPRITE, barX + i * 9, y + 4, 9, 9);
            if (i < fullShanks) {
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, FOOD_FULL_SPRITE, barX + i * 9, y + 4, 9, 9);
            } else if (i == fullShanks && hasHalfShank) {
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, FOOD_HALF_SPRITE, barX + i * 9, y + 4, 9, 9);
            }
            //?} else {
            /*gfx.texture(ICONS, barX + i * 9, y + 4, 16, 27, 9, 9);
            if (i < fullShanks) {
                gfx.texture(ICONS, barX + i * 9, y + 4, 52, 27, 9, 9);
            } else if (i == fullShanks && hasHalfShank) {
                gfx.texture(ICONS, barX + i * 9, y + 4, 61, 27, 9, 9);
            }
            *///?}
        }
    }

    @Override
    public List<? extends GuiEventListener> children() {
        return Collections.emptyList();
    }

    @Override
    public List<AbstractWidget> narratables() {
        return Collections.emptyList();
    }

    @Override
    public Object getValue() {
        return null;
    }

    @Override
    public Optional<Object> getDefaultValue() {
        return Optional.empty();
    }
}

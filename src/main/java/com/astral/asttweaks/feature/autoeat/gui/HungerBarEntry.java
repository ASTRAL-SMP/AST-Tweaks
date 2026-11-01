package com.astral.asttweaks.feature.autoeat.gui;

import me.shedaniel.clothconfig2.gui.entries.TooltipListEntry;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.renderer.RenderPipelines;
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
    private static final Identifier FOOD_EMPTY_SPRITE = Identifier.withDefaultNamespace("hud/food_empty");
    private static final Identifier FOOD_HALF_SPRITE = Identifier.withDefaultNamespace("hud/food_half");
    private static final Identifier FOOD_FULL_SPRITE = Identifier.withDefaultNamespace("hud/food_full");
    private final Supplier<Integer> hungerThreshold;

    public HungerBarEntry(Supplier<Integer> hungerThreshold) {
        super(Component.literal(""), null);
        this.hungerThreshold = hungerThreshold;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean isSelected, float delta) {
        super.extractRenderState(graphics, index, y, x, entryWidth, entryHeight, mouseX, mouseY, isSelected, delta);

        int threshold = this.hungerThreshold.get();
        int fullShanks = threshold / 2;
        boolean hasHalfShank = threshold % 2 == 1;

        int barX = x + entryWidth / 2 - 45;
        for (int i = 0; i < 10; i++) {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, FOOD_EMPTY_SPRITE, barX + i * 9, y + 4, 9, 9);
            if (i < fullShanks) {
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, FOOD_FULL_SPRITE, barX + i * 9, y + 4, 9, 9);
            } else if (i == fullShanks && hasHalfShank) {
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, FOOD_HALF_SPRITE, barX + i * 9, y + 4, 9, 9);
            }
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

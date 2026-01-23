package com.astral.asttweaks.feature.autoeat.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import me.shedaniel.clothconfig2.gui.entries.TooltipListEntry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * A Cloth Config entry that displays a hunger bar visualization.
 */
public class HungerBarEntry extends TooltipListEntry<Object> {
    private static final Identifier ICONS = new Identifier("minecraft", "textures/gui/icons.png");
    private final Supplier<Integer> hungerThreshold;

    public HungerBarEntry(Supplier<Integer> hungerThreshold) {
        super(Text.literal(""), null);
        this.hungerThreshold = hungerThreshold;
    }

    @Override
    public void render(MatrixStack matrices, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean isSelected, float delta) {
        super.render(matrices, index, y, x, entryWidth, entryHeight, mouseX, mouseY, isSelected, delta);
        MinecraftClient client = MinecraftClient.getInstance();
        RenderSystem.setShaderTexture(0, ICONS);

        int threshold = this.hungerThreshold.get();
        int fullShanks = threshold / 2;
        boolean hasHalfShank = threshold % 2 == 1;

        int barX = x + entryWidth / 2 - 45;
        for (int i = 0; i < 10; i++) {
            DrawableHelper.drawTexture(matrices, barX + i * 9, y + 4, 16, 27, 9, 9, 256, 256);
            if (i < fullShanks) {
                DrawableHelper.drawTexture(matrices, barX + i * 9, y + 4, 52, 27, 9, 9, 256, 256);
            } else if (i == fullShanks && hasHalfShank) {
                DrawableHelper.drawTexture(matrices, barX + i * 9, y + 4, 61, 27, 9, 9, 256, 256);
            }
        }
    }

    @Override
    public List<? extends Element> children() {
        return Collections.emptyList();
    }

    @Override
    public List<ClickableWidget> narratables() {
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

package com.astral.asttweaks.feature.autoeat.gui;

import me.shedaniel.clothconfig2.gui.entries.TooltipListEntry;
//? if >=26.1 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
//?} else if >=1.20 {
/*import net.minecraft.client.gui.GuiGraphics;
*///?} else {
/*import com.mojang.blaze3d.vertex.PoseStack;
*///?}
import com.astral.asttweaks.util.gui.Gfx;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.Component;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * A Cloth Config entry that displays a button.
 */
public class ButtonEntry extends TooltipListEntry<Void> {
    private final Button button;

    public ButtonEntry(Component buttonText, Button.OnPress onPress) {
        super(Component.literal(""), null);
        this.button = Button.builder(buttonText, onPress).build();
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
        this.button.setX(x + (entryWidth - 150) / 2);
        this.button.setY(y);
        this.button.setWidth(150);
        gfx.widget(this.button, mouseX, mouseY, delta);
    }

    @Override
    public List<? extends GuiEventListener> children() {
        return Collections.singletonList(this.button);
    }

    @Override
    public List<AbstractWidget> narratables() {
        return Collections.singletonList(this.button);
    }

    @Override
    public Void getValue() {
        return null;
    }

    @Override
    public Optional<Void> getDefaultValue() {
        return Optional.empty();
    }
}

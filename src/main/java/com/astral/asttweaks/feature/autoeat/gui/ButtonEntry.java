package com.astral.asttweaks.feature.autoeat.gui;

import me.shedaniel.clothconfig2.gui.entries.TooltipListEntry;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * A Cloth Config entry that displays a button.
 */
public class ButtonEntry extends TooltipListEntry<Void> {
    private final ButtonWidget button;

    public ButtonEntry(Text buttonText, ButtonWidget.PressAction onPress) {
        super(Text.literal(""), null);
        this.button = ButtonWidget.builder(buttonText, onPress).build();
    }

    @Override
    public void render(MatrixStack matrices, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean isSelected, float delta) {
        super.render(matrices, index, y, x, entryWidth, entryHeight, mouseX, mouseY, isSelected, delta);
        this.button.setX(x + (entryWidth - 150) / 2);
        this.button.setY(y);
        this.button.setWidth(150);
        this.button.render(matrices, mouseX, mouseY, delta);
    }

    @Override
    public List<? extends Element> children() {
        return Collections.singletonList(this.button);
    }

    @Override
    public List<ClickableWidget> narratables() {
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

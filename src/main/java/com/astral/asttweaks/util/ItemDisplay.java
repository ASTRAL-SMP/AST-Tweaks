package com.astral.asttweaks.util;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Item components are only bound once a world's registries are loaded, so
 * {@code new ItemStack(item)} and {@code item.components()} throw on the title
 * screen. Config screens can be opened from there via ModMenu.
 */
public final class ItemDisplay {
    public static final Component NEEDS_WORLD = Component.translatable("config.asttweaks.itemlist.needsWorld");

    private ItemDisplay() {}

    public static boolean itemDataLoaded() {
        return componentsBound(Items.STONE);
    }

    public static boolean componentsBound(Item item) {
        return item.builtInRegistryHolder().areComponentsBound();
    }

    public static ItemStack stackOrEmpty(Item item) {
        return componentsBound(item) ? new ItemStack(item) : ItemStack.EMPTY;
    }

    public static Component name(Item item) {
        return Component.translatable(item.getDescriptionId());
    }
}

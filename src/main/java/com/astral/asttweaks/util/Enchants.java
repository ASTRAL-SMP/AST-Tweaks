package com.astral.asttweaks.util;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
//? if >=26.1 {
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.EnchantmentTags;
//?}

/**
 * エンチャント判定のバージョン差分を吸収する。
 * 1.21 からエンチャントはレジストリキー（Holder）で扱う。
 */
public final class Enchants {
    private Enchants() {}

    //? if >=26.1 {
    public static boolean has(ItemStack stack, ResourceKey<Enchantment> enchantment) {
        for (Holder<Enchantment> holder : stack.getEnchantments().keySet()) {
            if (holder.is(enchantment)) return true;
        }
        return false;
    }
    //?} else {
    /*public static boolean has(ItemStack stack, Enchantment enchantment) {
        return EnchantmentHelper.getItemEnchantmentLevel(enchantment, stack) > 0;
    }
    *///?}

    /** 砥石で外せる（呪い以外の）エンチャントがあるか */
    public static boolean hasRemovable(ItemStack stack) {
        //? if >=26.1 {
        for (Holder<Enchantment> holder : EnchantmentHelper.getEnchantmentsForCrafting(stack).keySet()) {
            if (!holder.is(EnchantmentTags.CURSE)) return true;
        }
        return false;
        //?} else {
        /*for (Enchantment enchantment : EnchantmentHelper.getEnchantments(stack).keySet()) {
            if (!enchantment.isCurse()) return true;
        }
        return false;
        *///?}
    }
}

package com.astral.asttweaks.util;

import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
//? if >=26.1 {
import net.minecraft.core.component.DataComponents;
//?}

/**
 * 食料情報のバージョン差分を吸収する。1.20.5 から食料はデータコンポーネントになった。
 */
public final class Foods {
    private Foods() {}

    public static FoodProperties of(ItemStack stack) {
        //? if >=26.1 {
        return stack.get(DataComponents.FOOD);
        //?} else {
        /*return stack.getItem().getFoodProperties();
        *///?}
    }

    public static FoodProperties of(Item item) {
        //? if >=26.1 {
        return ItemDisplay.componentsBound(item) ? item.components().get(DataComponents.FOOD) : null;
        //?} else {
        /*return item.getFoodProperties();
        *///?}
    }

    public static int nutrition(FoodProperties food) {
        //? if >=26.1 {
        return food.nutrition();
        //?} else {
        /*return food.getNutrition();
        *///?}
    }

    /** 回復する満腹度（隠し満腹度）。旧版は nutrition * modifier * 2 で求める */
    public static float saturation(FoodProperties food) {
        //? if >=26.1 {
        return food.saturation();
        //?} else {
        /*return food.getNutrition() * food.getSaturationModifier() * 2.0f;
        *///?}
    }
}

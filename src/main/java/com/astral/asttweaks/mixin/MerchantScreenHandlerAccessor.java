package com.astral.asttweaks.mixin;

import net.minecraft.screen.MerchantScreenHandler;
import net.minecraft.village.Merchant;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * 取引画面の元の取引一覧（サーバーと同じ並び）を得るためのAccessor。
 * ItemScroller 等が getRecipes() を並べ替えた一覧に差し替えても、merchant 側は元の並びのまま。
 */
@Mixin(MerchantScreenHandler.class)
public interface MerchantScreenHandlerAccessor {
    @Accessor("merchant")
    Merchant getMerchant();
}

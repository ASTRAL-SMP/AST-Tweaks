package com.astral.asttweaks.mixin;

import net.minecraft.client.gui.screen.ingame.MerchantScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Void Trade が取引を選んだとき、取引画面側のハイライトも合わせるためのAccessor。
 */
@Mixin(MerchantScreen.class)
public interface MerchantScreenAccessor {
    @Accessor("selectedIndex")
    void setSelectedIndex(int selectedIndex);
}

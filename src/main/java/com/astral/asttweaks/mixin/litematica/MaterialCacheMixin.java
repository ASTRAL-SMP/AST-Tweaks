package com.astral.asttweaks.mixin.litematica;

import com.astral.asttweaks.compat.LitematicaCoralCompat;
import com.astral.asttweaks.config.ModConfig;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Litematica の MaterialCache が返す必要アイテムを差し替えるフック。
 * getRequiredBuildItemForState は Easy Place・設置制限・ピックブロック・
 * マテリアルリストの全てで使われるため、ここ一点で死サンゴ→生サンゴの
 * 代用が効く（メソッド名のみで両オーバーロードに適用）。
 */
@Mixin(targets = "fi.dy.masa.litematica.materials.MaterialCache", remap = false)
public abstract class MaterialCacheMixin {

    @Inject(method = "getRequiredBuildItemForState", at = @At("RETURN"), cancellable = true)
    private void asttweaks$substituteDeadCoral(CallbackInfoReturnable<ItemStack> cir) {
        if (!ModConfig.getInstance().litematicaCoralSubstituteEnabled) {
            return;
        }
        ItemStack substituted = LitematicaCoralCompat.substituteDeadCoral(cir.getReturnValue());
        if (substituted != null) {
            cir.setReturnValue(substituted);
        }
    }
}

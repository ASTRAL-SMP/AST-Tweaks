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
 *
 * メソッド名のみの指定により getRequiredBuildItemForState の両オーバーロードへ注入される:
 * - (BlockState) 版: Easy Place（WorldUtils.doEasyPlaceAction）、設置制限
 *   （placementRestrictionInEffect）、マテリアルリスト（MaterialListUtils）が使用
 * - (BlockState, World, BlockPos) 版: スケマティックへのピックブロック
 *   （WorldUtils.doSchematicWorldPickBlock）が使用
 *
 * このため Easy Place・設置制限・ピックブロック・マテリアルリストの全てで
 * 死サンゴ→生サンゴの代用が効く。
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

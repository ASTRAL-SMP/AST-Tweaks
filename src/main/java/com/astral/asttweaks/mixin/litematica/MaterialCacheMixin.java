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
 * getRequiredBuildItemForState には 2 つのオーバーロードがあり、両方に注入する必要がある:
 * - (BlockState) 版: Easy Place（WorldUtils.doEasyPlaceAction）、設置制限
 *   （placementRestrictionInEffect）、マテリアルリスト（MaterialListUtils）が使用
 * - (BlockState, World, BlockPos) 版: スケマティックへのピックブロック
 *   （WorldUtils.doSchematicWorldPickBlock）が使用
 *
 * セレクタ名のみだと既定数量子（max 1）で最初の 1 引数版にしか注入されないため、
 * 末尾に "*"（Quantifier.ANY）を付けて両オーバーロードへ注入する。
 * これで Easy Place・設置制限・ピックブロック・マテリアルリストの全てで
 * 死サンゴ→生サンゴの代用が効く。
 */
@Mixin(targets = "fi.dy.masa.litematica.materials.MaterialCache", remap = false)
public abstract class MaterialCacheMixin {

    @Inject(method = "getRequiredBuildItemForState*", at = @At("RETURN"), cancellable = true)
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

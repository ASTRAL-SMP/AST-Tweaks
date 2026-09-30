package com.astral.asttweaks.mixin.nvidium;

import com.astral.asttweaks.feature.FeatureManager;
import com.astral.asttweaks.feature.nvidiumchunkfix.NvidiumChunkFixFeature;
import com.astral.asttweaks.feature.nvidiumchunkfix.RegionCountFix;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * リージョンのメタデータに詰めるセクション数を、シェーダーの読み方に合わせて「個数 - 1」にする
 * （{@link RegionCountFix}）。あわせて全スロットが埋まったリージョンを数える（F3 表示・ログ用）。
 */
@Mixin(targets = "me.cortex.nvidium.managers.RegionManager$Region", remap = false)
public abstract class RegionMixin {
    @Shadow
    @Final
    private int rx;

    @Shadow
    @Final
    private int ry;

    @Shadow
    @Final
    private int rz;

    @Shadow
    @Final
    private int id;

    @ModifyArg(
        method = "getPackedData",
        at = @At(value = "INVOKE", target = "Lme/cortex/nvidium/managers/RegionManager;packRegion(IIIIIII)J"),
        index = 0
    )
    private int asttweaks$encodeSectionCount(int count) {
        NvidiumChunkFixFeature feature = FeatureManager.getInstance().getNvidiumChunkFixFeature();
        boolean enabled = feature != null && feature.isEnabled();
        return RegionCountFix.encodeCount(this.id, count, this.rx, this.ry, this.rz, enabled);
    }
}

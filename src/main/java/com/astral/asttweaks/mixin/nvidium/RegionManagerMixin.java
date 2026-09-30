package com.astral.asttweaks.mixin.nvidium;

import com.astral.asttweaks.feature.FeatureManager;
import com.astral.asttweaks.feature.nvidiumchunkfix.NvidiumChunkFixFeature;
import com.astral.asttweaks.feature.nvidiumchunkfix.NvidiumRegionManagerExt;
import com.astral.asttweaks.feature.nvidiumchunkfix.RegionCountFix;
import me.cortex.nvidium.managers.RegionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.function.IntConsumer;

/**
 * Nvidium のリージョン ID 解放を RenderPipeline に通知する（可視性統計のリセット用）。
 *
 * RegionManager はリージョンが空になると ID を IdProvider に返し、次に作られるリージョンがその ID を
 * 再利用する。可視性統計（RegionVisibilityTracker）は VRAM 上限での破棄時にしかリセットされないため、
 * 読み込まれたばかりのリージョンが前の持ち主の「長く見えていない」統計を引き継ぎ、最優先で破棄されてしまう。
 */
@Mixin(value = RegionManager.class, remap = false)
public abstract class RegionManagerMixin implements NvidiumRegionManagerExt {
    @Unique
    private IntConsumer asttweaks$releaseListener;

    @Override
    public void asttweaks$setReleaseListener(IntConsumer listener) {
        this.asttweaks$releaseListener = listener;
    }

    @ModifyArg(
        method = "removeSectionIndex",
        at = @At(value = "INVOKE", target = "Lme/cortex/nvidium/util/IdProvider;release(I)V")
    )
    private int asttweaks$onRegionIdReleased(int regionId) {
        RegionCountFix.onRegionReleased(regionId);
        if (asttweaks$releaseListener != null) {
            NvidiumChunkFixFeature feature = FeatureManager.getInstance().getNvidiumChunkFixFeature();
            if (feature != null && feature.isEnabled()) {
                asttweaks$releaseListener.accept(regionId);
            }
        }
        return regionId;
    }
}

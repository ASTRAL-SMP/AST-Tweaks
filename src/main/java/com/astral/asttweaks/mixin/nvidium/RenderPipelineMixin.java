package com.astral.asttweaks.mixin.nvidium;

import com.astral.asttweaks.feature.FeatureManager;
import com.astral.asttweaks.feature.nvidiumchunkfix.EvictedSections;
import com.astral.asttweaks.feature.nvidiumchunkfix.NvidiumChunkFixFeature;
import com.astral.asttweaks.feature.nvidiumchunkfix.NvidiumRegionManagerExt;
import me.cortex.nvidium.RenderPipeline;
import me.cortex.nvidium.managers.RegionManager;
import me.cortex.nvidium.managers.RegionVisibilityTracker;
import me.cortex.nvidium.managers.SectionManager;
import net.minecraft.util.math.ChunkSectionPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Nvidium がリージョンを GPU から破棄したとき（VRAM 上限・region_keep_distance 超過）に、
 * そのリージョンにあったセクションを記録して Sodium 側で再ビルドさせる。
 * 実際の再ビルド予約は {@link SodiumRenderSectionManagerMixin} で行う。
 */
@Mixin(value = RenderPipeline.class, remap = false)
public abstract class RenderPipelineMixin {
    @Shadow
    @Final
    public SectionManager sectionManager;

    @Shadow
    @Final
    public RegionVisibilityTracker regionVisibilityTracking;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void asttweaks$hookRegionRelease(CallbackInfo ci) {
        RegionVisibilityTracker tracker = this.regionVisibilityTracking;
        ((NvidiumRegionManagerExt) this.sectionManager.getRegionManager())
                .asttweaks$setReleaseListener(tracker::resetRegion);
    }

    @Inject(method = "removeRegion", at = @At("HEAD"), cancellable = true)
    private void asttweaks$onRemoveRegion(int id, CallbackInfo ci) {
        if (!asttweaks$isFixEnabled()) {
            return;
        }
        if (id < 0) {
            // VRAM 上限を超えたが破棄候補が無い（どのリージョンも観測期間 200 フレーム未満）。
            // そのまま進むと regions[-1] で配列外参照になりクラッシュする
            ci.cancel();
            return;
        }

        RegionManager regions = this.sectionManager.getRegionManager();
        if (!regions.regionExists(id)) {
            return;
        }

        // リージョンは 8x4x8 セクション（SectionManager.removeRegionById と同じ展開）
        long regionKey = regions.regionIdToKey(id);
        int baseX = ChunkSectionPos.unpackX(regionKey) << 3;
        int baseY = ChunkSectionPos.unpackY(regionKey) << 2;
        int baseZ = ChunkSectionPos.unpackZ(regionKey) << 3;
        for (int x = baseX; x < baseX + 8; x++) {
            for (int y = baseY; y < baseY + 4; y++) {
                for (int z = baseZ; z < baseZ + 8; z++) {
                    if (this.sectionManager.getSectionRegionIndex(x, y, z) != -1) {
                        EvictedSections.add(ChunkSectionPos.asLong(x, y, z));
                    }
                }
            }
        }
        EvictedSections.onRegionEvicted();
    }

    @Inject(method = "addDebugInfo", at = @At("TAIL"))
    private void asttweaks$addDebugInfo(List<String> info, CallbackInfo ci) {
        if (!asttweaks$isFixEnabled()) {
            return;
        }
        info.add("AST Chunk Fix: evicted " + EvictedSections.getEvictedRegions()
                + " regions, restored " + EvictedSections.getRestoredSections()
                + " sections, pending " + EvictedSections.size());
    }

    @Unique
    private static boolean asttweaks$isFixEnabled() {
        NvidiumChunkFixFeature feature = FeatureManager.getInstance().getNvidiumChunkFixFeature();
        return feature != null && feature.isEnabled();
    }
}

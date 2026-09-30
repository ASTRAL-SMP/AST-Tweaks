package com.astral.asttweaks.mixin.nvidium;

import com.astral.asttweaks.feature.FeatureManager;
import com.astral.asttweaks.feature.nvidiumchunkfix.EvictedSections;
import com.astral.asttweaks.feature.nvidiumchunkfix.NvidiumChunkFixFeature;
import it.unimi.dsi.fastutil.longs.Long2ReferenceMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import me.cortex.nvidium.Nvidium;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkUpdateType;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSection;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSectionManager;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Nvidium に破棄されたセクションを Sodium 側で再ビルド予約する。
 *
 * 予約だけしておけば、Sodium は視界内（BFS で到達したもの）から順にビルドし、Nvidium へアップロードされる。
 * updateChunks() の先頭で処理すると、同じフレームの BFS（needsUpdate 時に実行）でキューに積まれる。
 */
@Mixin(value = RenderSectionManager.class, remap = false)
public abstract class SodiumRenderSectionManagerMixin {
    @Unique
    private static final long RESYNC_INTERVAL_NANOS = 50_000_000L;

    // Nvidium の region_keep_distance 判定の境界で、再ビルド→即破棄を繰り返さないための余裕（チャンク）
    @Unique
    private static final int KEEP_DISTANCE_MARGIN = 2;

    @Shadow
    @Final
    private Long2ReferenceMap<RenderSection> sections;

    @Shadow
    private boolean needsUpdate;

    @Shadow
    private int centerChunkX;

    @Shadow
    private int centerChunkZ;

    @Shadow
    private float cameraY;

    @Unique
    private long asttweaks$lastResyncNanos;

    @Inject(method = "updateChunks()V", at = @At("HEAD"))
    private void asttweaks$rebuildEvictedSections(CallbackInfo ci) {
        if (EvictedSections.isEmpty()) {
            return;
        }
        NvidiumChunkFixFeature feature = FeatureManager.getInstance().getNvidiumChunkFixFeature();
        if (feature == null || !feature.isEnabled()) {
            EvictedSections.reset();
            return;
        }
        long now = System.nanoTime();
        if (now - asttweaks$lastResyncNanos < RESYNC_INTERVAL_NANOS) {
            return;
        }
        asttweaks$lastResyncNanos = now;

        // region_keep_distance が 32（Vanilla）/256（Keep All）以外のとき、Nvidium はリージョン中心が
        // カメラから keep+4 チャンクを超えたリージョンを毎フレーム破棄する。その外で再ビルドしても
        // すぐ捨てられるだけなので、内側に戻ってくるまで保留する
        int keepDistance = Nvidium.config.region_keep_distance;
        boolean keepLimited = keepDistance != 32 && keepDistance != 256;
        int keepLimit = keepDistance + 4 - KEEP_DISTANCE_MARGIN;
        int cameraChunkY = MathHelper.floor(cameraY) >> 4;

        boolean scheduled = false;
        LongIterator it = EvictedSections.iterator();
        while (it.hasNext()) {
            long key = it.nextLong();
            RenderSection section = sections.get(key);
            if (section == null || section.isDisposed()) {
                // アンロード済み。再ロード時は Sodium が最初からビルドする
                it.remove();
                continue;
            }
            if (keepLimited && !asttweaks$isWithinKeepDistance(key, keepLimit, cameraChunkY)) {
                continue;
            }
            // 未ビルドなら INITIAL_BUILD が予約済みなので触らない
            if (section.isBuilt()) {
                section.markForUpdate(ChunkUpdateType.REBUILD);
                EvictedSections.onSectionRestored();
                scheduled = true;
            }
            it.remove();
        }

        if (scheduled) {
            // BFS を走らせて視界内のものを再ビルドキューに積ませる
            needsUpdate = true;
        }
    }

    @Inject(method = "destroy", at = @At("HEAD"))
    private void asttweaks$clearEvictedSections(CallbackInfo ci) {
        // レンダラー再ロード後は全セクションが最初からビルドされる
        EvictedSections.reset();
    }

    /**
     * Nvidium の RegionManager.withinSquare と同じく、セクションが属するリージョンの中心で判定する。
     */
    @Unique
    private boolean asttweaks$isWithinKeepDistance(long sectionKey, int limit, int cameraChunkY) {
        int regionCenterX = (ChunkSectionPos.unpackX(sectionKey) & ~7) + 4;
        int regionCenterY = (ChunkSectionPos.unpackY(sectionKey) & ~3) + 2;
        int regionCenterZ = (ChunkSectionPos.unpackZ(sectionKey) & ~7) + 4;
        return Math.abs(regionCenterX - centerChunkX) <= limit
                && Math.abs(regionCenterY - cameraChunkY) <= limit
                && Math.abs(regionCenterZ - centerChunkZ) <= limit;
    }
}

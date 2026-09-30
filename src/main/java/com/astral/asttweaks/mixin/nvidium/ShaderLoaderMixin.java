package com.astral.asttweaks.mixin.nvidium;

import com.astral.asttweaks.feature.FeatureManager;
import com.astral.asttweaks.feature.nvidiumchunkfix.NvidiumChunkFixFeature;
import com.astral.asttweaks.feature.nvidiumchunkfix.RegionCountFix;
import me.cortex.nvidium.sodiumCompat.ShaderLoader;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * section_raster/task.glsl のリージョンのセクション数の読み方を書き換える（{@link RegionCountFix}）。
 * シェーダーは RenderPipeline の生成時にここを通ってコンパイルされる。
 */
@Mixin(value = ShaderLoader.class, remap = false)
public abstract class ShaderLoaderMixin {

    @Inject(method = "parse", at = @At("RETURN"), cancellable = true)
    private static void asttweaks$patchRegionCount(Identifier path, CallbackInfoReturnable<String> cir) {
        if (!"nvidium".equals(path.getNamespace()) || !RegionCountFix.SECTION_RASTER_TASK.equals(path.getPath())) {
            return;
        }
        NvidiumChunkFixFeature feature = FeatureManager.getInstance().getNvidiumChunkFixFeature();
        boolean enabled = feature != null && feature.isEnabled();
        cir.setReturnValue(RegionCountFix.patchSectionRasterTask(cir.getReturnValue(), enabled));
    }
}

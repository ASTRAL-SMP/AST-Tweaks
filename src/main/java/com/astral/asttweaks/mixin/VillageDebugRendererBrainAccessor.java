//? if <26.1 {
/*package com.astral.asttweaks.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Set;
import java.util.UUID;
import net.minecraft.client.renderer.debug.BrainDebugRenderer;
import net.minecraft.core.BlockPos;

@Mixin(BrainDebugRenderer.BrainDump.class)
public interface VillageDebugRendererBrainAccessor {

    @Accessor("uuid")
    UUID getUuid();

    @Accessor("pois")
    Set<BlockPos> getPointsOfInterest();
}
*///?}

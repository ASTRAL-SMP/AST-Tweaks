package com.astral.asttweaks.mixin;

import net.minecraft.client.render.debug.VillageDebugRenderer;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Set;
import java.util.UUID;

@Mixin(VillageDebugRenderer.Brain.class)
public interface VillageDebugRendererBrainAccessor {

    @Accessor("uuid")
    UUID getUuid();

    @Accessor("pointsOfInterest")
    Set<BlockPos> getPointsOfInterest();
}

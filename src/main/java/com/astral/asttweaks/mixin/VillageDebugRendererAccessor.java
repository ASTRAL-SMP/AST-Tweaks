//? if <26.1 {
/*package com.astral.asttweaks.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;
import java.util.UUID;
import net.minecraft.client.renderer.debug.BrainDebugRenderer;

@Mixin(BrainDebugRenderer.class)
public interface VillageDebugRendererAccessor {

    @Accessor("brainDumpsPerEntity")
    Map<UUID, BrainDebugRenderer.BrainDump> getBrains();
}
*///?}

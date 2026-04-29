package com.astral.asttweaks.mixin;

import net.minecraft.client.render.debug.VillageDebugRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;
import java.util.UUID;

@Mixin(VillageDebugRenderer.class)
public interface VillageDebugRendererAccessor {

    @Accessor("brains")
    Map<UUID, VillageDebugRenderer.Brain> getBrains();
}

package com.astral.asttweaks.mixin;

import io.netty.buffer.Unpooled;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.CustomPayloadS2CPacket;
import net.minecraft.server.network.DebugInfoSender;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.WeakHashMap;

/**
 * Vanilla's {@code DebugInfoSender.sendBrainDebugData} is an empty stub —
 * the server never broadcasts villager brain debug payloads, so the client's
 * {@code VillageDebugRenderer.brains} map stays empty in unmodified play.
 *
 * Approach borrowed from adrian154/debug-renderers: inject the actual packet
 * send at HEAD, mirroring the private {@code writeBrain} helper that vanilla
 * already implements but never invokes for transmission.
 *
 * Filtered to {@link VillagerEntity} since the Villager Link feature is the
 * only consumer; other brain-bearing entities (golems, allays, frogs, etc.)
 * don't need their data serialized every tick.
 *
 * Effective scope: integrated server (singleplayer / LAN host) only — the
 * mod isn't loaded on dedicated servers.
 */
@Mixin(DebugInfoSender.class)
public abstract class DebugInfoSenderMixin {

    /**
     * Per-entity last-send tick. Brain.tick runs on the server tick thread for
     * a given world, so this map is touched single-threaded in practice. Weak
     * keys prevent the map from pinning despawned entities.
     */
    private static final WeakHashMap<LivingEntity, Long> asttweaks$lastSent = new WeakHashMap<>();
    private static final long ASTTWEAKS_THROTTLE_TICKS = 20L;

    @Invoker("sendToAll")
    private static void asttweaks$sendToAll(ServerWorld world, PacketByteBuf buf, Identifier channel) {
        throw new AssertionError();
    }

    @Invoker("writeBrain")
    private static void asttweaks$writeBrain(LivingEntity entity, PacketByteBuf buf) {
        throw new AssertionError();
    }

    @Inject(method = "sendBrainDebugData(Lnet/minecraft/entity/LivingEntity;)V", at = @At("HEAD"))
    private static void asttweaks$sendBrainDebugData(LivingEntity entity, CallbackInfo ci) {
        if (entity.getWorld().isClient()) return;
        if (!(entity instanceof VillagerEntity)) return;

        long now = entity.getWorld().getTime();
        Long last = asttweaks$lastSent.get(entity);
        if (last != null && now - last < ASTTWEAKS_THROTTLE_TICKS) return;
        asttweaks$lastSent.put(entity, now);

        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeDouble(entity.getX());
        buf.writeDouble(entity.getY());
        buf.writeDouble(entity.getZ());
        buf.writeUuid(entity.getUuid());
        buf.writeInt(entity.getId());
        buf.writeString(entity.getEntityName());
        VillagerEntity villager = (VillagerEntity) entity;
        buf.writeString(villager.getVillagerData().getProfession().toString());
        buf.writeInt(villager.getExperience());
        buf.writeFloat(entity.getHealth());
        buf.writeFloat(entity.getMaxHealth());
        asttweaks$writeBrain(entity, buf);
        asttweaks$sendToAll((ServerWorld) entity.getWorld(), buf, CustomPayloadS2CPacket.DEBUG_BRAIN);
    }
}

//? if <26.1 {
/*package com.astral.asttweaks.mixin;

import io.netty.buffer.Unpooled;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.WeakHashMap;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.game.DebugPackets;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.villager.Villager;

/^*
 * Vanilla's {@code DebugInfoSender.sendBrainDebugData} is an empty stub —
 * the server never broadcasts villager brain debug payloads, so the client's
 * {@code VillageDebugRenderer.brains} map stays empty in unmodified play.
 *
 * Approach borrowed from adrian154/debug-renderers: inject the actual packet
 * send at HEAD, mirroring the private {@code writeBrain} helper that vanilla
 * already implements but never invokes for transmission.
 *
 * Filtered to {@link Villager} since the Villager Link feature is the
 * only consumer; other brain-bearing entities (golems, allays, frogs, etc.)
 * don't need their data serialized every tick.
 *
 * Effective scope: integrated server (singleplayer / LAN host) only — the
 * mod isn't loaded on dedicated servers.
 ^/
@Mixin(DebugPackets.class)
public abstract class DebugInfoSenderMixin {

    /^*
     * Per-entity last-send tick. Brain.tick runs on the server tick thread for
     * a given world, so this map is touched single-threaded in practice. Weak
     * keys prevent the map from pinning despawned entities.
     ^/
    private static final WeakHashMap<LivingEntity, Long> asttweaks$lastSent = new WeakHashMap<>();
    private static final long ASTTWEAKS_THROTTLE_TICKS = 20L;

    @Invoker("sendPacketToAllPlayers")
    private static void asttweaks$sendToAll(ServerLevel world, FriendlyByteBuf buf, Identifier channel) {
        throw new AssertionError();
    }

    @Invoker("writeBrain")
    private static void asttweaks$writeBrain(LivingEntity entity, FriendlyByteBuf buf) {
        throw new AssertionError();
    }

    @Inject(method = "sendEntityBrain(Lnet/minecraft/world/entity/LivingEntity;)V", at = @At("HEAD"))
    private static void asttweaks$sendBrainDebugData(LivingEntity entity, CallbackInfo ci) {
        if (entity.level().isClientSide()) return;
        if (!(entity instanceof Villager)) return;

        long now = entity.level().getGameTime();
        Long last = asttweaks$lastSent.get(entity);
        if (last != null && now - last < ASTTWEAKS_THROTTLE_TICKS) return;
        asttweaks$lastSent.put(entity, now);

        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeDouble(entity.getX());
        buf.writeDouble(entity.getY());
        buf.writeDouble(entity.getZ());
        buf.writeUUID(entity.getUUID());
        buf.writeInt(entity.getId());
        buf.writeUtf(entity.getScoreboardName());
        Villager villager = (Villager) entity;
        buf.writeUtf(villager.getVillagerData().getProfession().toString());
        buf.writeInt(villager.getVillagerXp());
        buf.writeFloat(entity.getHealth());
        buf.writeFloat(entity.getMaxHealth());
        asttweaks$writeBrain(entity, buf);
        asttweaks$sendToAll((ServerLevel) entity.level(), buf, ClientboundCustomPayloadPacket.DEBUG_BRAIN);
    }
}
*///?}

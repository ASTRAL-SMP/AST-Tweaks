package com.astral.asttweaks.feature.syncertune;

import com.astral.asttweaks.ASTTweaks;
import com.astral.asttweaks.compat.TweakerooCompat;
import com.astral.asttweaks.feature.Feature;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.WorldChunk;

/**
 * Block entity が密集した場所では TweakerMore の ServerDataSyncer の
 * serverDataSyncerQueryInterval / serverDataSyncerQueryLimit を緩めの値に切り替え、
 * 通常密度に戻ったら元の値に戻す機能。
 *
 * 切り替え値（high / normal）はすべて AST-Tweaks 側の設定で持つ。
 * TweakerMore が未導入 or 該当設定がリフレクションで取得できない場合は no-op。
 */
public class SyncerTuneFeature implements Feature {
    private final SyncerTuneConfig config = new SyncerTuneConfig();

    private enum Mode { UNINITIALIZED, NORMAL, HIGH }

    private Mode currentMode = Mode.UNINITIALIZED;
    private int tickCounter = 0;

    @Override
    public String getId() {
        return "syncertune";
    }

    @Override
    public String getName() {
        return "Syncer Tune";
    }

    @Override
    public void init() {
        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
        ASTTweaks.LOGGER.info("Syncer Tune feature initialized");
    }

    @Override
    public void tick() {
        // tick 処理は onClientTick 側で
    }

    @Override
    public boolean isEnabled() {
        return config.isEnabled();
    }

    @Override
    public void setEnabled(boolean enabled) {
        config.setEnabled(enabled);
        if (!enabled) {
            // 無効化時に TweakerMore 設定を normal 値へ戻して制御権を解放する
            applyNormal();
            currentMode = Mode.UNINITIALIZED;
            tickCounter = 0;
        }
    }

    public SyncerTuneConfig getConfig() {
        return config;
    }

    private void onClientTick(MinecraftClient client) {
        if (!config.isEnabled()) {
            return;
        }
        if (!TweakerooCompat.isServerDataSyncerConfigAvailable()) {
            return;
        }
        ClientPlayerEntity player = client.player;
        ClientWorld world = client.world;
        if (player == null || world == null) {
            return;
        }

        // ワールド入退場での状態リセット
        if (currentMode == Mode.UNINITIALIZED) {
            applyNormal();
        }

        tickCounter++;
        if (tickCounter < config.getDetectionIntervalTicks()) {
            return;
        }
        tickCounter = 0;

        int count = countNearbyBlockEntities(world, player.getBlockPos(), config.getDetectionChunkRadius());
        boolean shouldBeHigh = count >= config.getHighDensityThreshold();

        if (shouldBeHigh && currentMode != Mode.HIGH) {
            applyHigh();
            ASTTweaks.LOGGER.info("SyncerTune: switched to HIGH (block entity count: {}, threshold: {})",
                    count, config.getHighDensityThreshold());
        } else if (!shouldBeHigh && currentMode != Mode.NORMAL) {
            applyNormal();
            ASTTweaks.LOGGER.info("SyncerTune: switched to NORMAL (block entity count: {}, threshold: {})",
                    count, config.getHighDensityThreshold());
        }
    }

    private void applyHigh() {
        TweakerooCompat.setServerDataSyncerQueryInterval(config.getHighInterval());
        TweakerooCompat.setServerDataSyncerQueryLimit(config.getHighLimit());
        currentMode = Mode.HIGH;
    }

    private void applyNormal() {
        TweakerooCompat.setServerDataSyncerQueryInterval(config.getNormalInterval());
        TweakerooCompat.setServerDataSyncerQueryLimit(config.getNormalLimit());
        currentMode = Mode.NORMAL;
    }

    private int countNearbyBlockEntities(ClientWorld world, BlockPos playerPos, int chunkRadius) {
        int count = 0;
        int playerChunkX = playerPos.getX() >> 4;
        int playerChunkZ = playerPos.getZ() >> 4;
        for (int dx = -chunkRadius; dx <= chunkRadius; dx++) {
            for (int dz = -chunkRadius; dz <= chunkRadius; dz++) {
                WorldChunk chunk = world.getChunkManager().getWorldChunk(playerChunkX + dx, playerChunkZ + dz);
                if (chunk != null) {
                    count += chunk.getBlockEntityPositions().size();
                }
            }
        }
        return count;
    }
}

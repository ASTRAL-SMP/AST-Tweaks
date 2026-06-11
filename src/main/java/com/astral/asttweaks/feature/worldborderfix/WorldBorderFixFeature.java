package com.astral.asttweaks.feature.worldborderfix;

import com.astral.asttweaks.ASTTweaks;
import com.astral.asttweaks.compat.NvidiumCompat;
import com.astral.asttweaks.feature.Feature;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.text.Text;
import net.minecraft.world.border.WorldBorder;

/**
 * ワールドボーダー付近で発生する描画バグへの対策。
 *
 * 1. ボーダー付近で壁が透明になり X-ray のように見えるバグ
 * 2. 遠距離座標（ボーダーが遠い座標にある場合）で地形が正しく
 *    読み込まれない・描画が乱れるバグ
 *
 * どちらも Nvidium のメッシュシェーダーカリングがボーダー付近・遠距離座標で
 * 破綻することが原因のため、対象エリアにいる間だけ Nvidium を一時無効化して
 * Sodium 標準の描画パスに切り替え、離れたら自動で元に戻す。
 * Nvidium 未導入時は no-op。
 */
public class WorldBorderFixFeature implements Feature {
    private static final int CHECK_INTERVAL_TICKS = 10;
    // 境界をまたぐたびにレンダラー再ロードが連発しないようにするヒステリシス幅
    private static final double BORDER_EXIT_MARGIN = 32.0;
    private static final double COORD_EXIT_MARGIN = 256.0;

    private final WorldBorderFixConfig config = new WorldBorderFixConfig();
    private int tickCounter = 0;

    @Override
    public String getId() {
        return "worldborderfix";
    }

    @Override
    public String getName() {
        return "World Border Fix";
    }

    @Override
    public void init() {
        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
        ASTTweaks.LOGGER.info("World Border Fix feature initialized");
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
            releaseSuppression(MinecraftClient.getInstance());
            tickCounter = 0;
        }
    }

    public WorldBorderFixConfig getConfig() {
        return config;
    }

    private void onClientTick(MinecraftClient client) {
        if (!config.isEnabled()) {
            // 設定画面から直接無効化された場合もここで確実に復元する
            releaseSuppression(client);
            return;
        }
        if (!NvidiumCompat.isAvailable()) {
            return;
        }

        ClientPlayerEntity player = client.player;
        ClientWorld world = client.world;
        if (player == null || world == null) {
            // ワールド退出時はレンダラー再ロード不要、フラグだけ元に戻す
            NvidiumCompat.restore();
            return;
        }

        tickCounter++;
        if (tickCounter < CHECK_INTERVAL_TICKS) {
            return;
        }
        tickCounter = 0;

        boolean suppressed = NvidiumCompat.isSuppressed();
        boolean shouldSuppress = shouldSuppress(world.getWorldBorder(), player, suppressed);

        if (shouldSuppress && !suppressed) {
            if (NvidiumCompat.suppress()) {
                client.worldRenderer.reload();
                player.sendMessage(Text.translatable(
                        "message." + ASTTweaks.MOD_ID + ".worldborderfix.suppressed"), true);
                ASTTweaks.LOGGER.info("WorldBorderFix: Nvidium temporarily disabled (near world border / far coordinates)");
            }
        } else if (!shouldSuppress && suppressed) {
            if (NvidiumCompat.restore()) {
                client.worldRenderer.reload();
                player.sendMessage(Text.translatable(
                        "message." + ASTTweaks.MOD_ID + ".worldborderfix.restored"), true);
                ASTTweaks.LOGGER.info("WorldBorderFix: Nvidium re-enabled");
            }
        }
    }

    private boolean shouldSuppress(WorldBorder border, ClientPlayerEntity player, boolean currentlySuppressed) {
        if (config.isXrayFixEnabled()) {
            // ボーダーの外側（負値）も含めてしきい値未満なら発動
            double distance = border.getDistanceInsideBorder(player);
            double threshold = config.getBorderDistance() + (currentlySuppressed ? BORDER_EXIT_MARGIN : 0);
            if (distance < threshold) {
                return true;
            }
        }
        if (config.isFarCoordFixEnabled()) {
            double coord = Math.max(Math.abs(player.getX()), Math.abs(player.getZ()));
            double threshold = config.getCoordThreshold() - (currentlySuppressed ? COORD_EXIT_MARGIN : 0);
            if (coord >= threshold) {
                return true;
            }
        }
        return false;
    }

    private void releaseSuppression(MinecraftClient client) {
        if (!NvidiumCompat.isSuppressed()) {
            return;
        }
        if (NvidiumCompat.restore() && client != null && client.world != null) {
            client.worldRenderer.reload();
        }
    }
}

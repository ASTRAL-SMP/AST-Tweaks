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
 * Sodium 標準の描画パスに切り替える。Nvidium 未導入時は no-op。
 *
 * Nvidium 0.1.x はランタイム切替を公式サポートしておらず、レンダラー再ロードを
 * 伴う切替（特に再有効化方向）は一部環境でネイティブクラッシュを起こすため、
 * 再ロードは必要最小限に抑える:
 * - 抑制時のみ再ロード（それも Nvidium が実際に動作中の場合のみ）
 * - 復帰は既定でフラグのみ戻し、次の自然な再ロード（ディメンション移動・
 *   F3+A・再ログイン等）で Nvidium が復帰する。即時復帰はオプション
 * - 判定が連続して安定するまで切替えない（サーバー間移動時、ボーダー同期前の
 *   デフォルト境界による誤判定での切替連発を防ぐ）
 * - 再ロード間に最低クールダウンを設ける
 */
public class WorldBorderFixFeature implements Feature {
    private static final int CHECK_INTERVAL_TICKS = 10;
    // 状態切替に必要な連続安定判定回数（CHECK_INTERVAL_TICKS × この回数だけ継続したら切替）
    private static final int STABLE_CHECKS_REQUIRED = 3;
    // この機能起因のレンダラー再ロード同士の最小間隔
    private static final int RELOAD_COOLDOWN_TICKS = 200;
    // 境界をまたぐたびに切替が連発しないようにするヒステリシス幅
    private static final double BORDER_EXIT_MARGIN = 32.0;
    private static final double COORD_EXIT_MARGIN = 256.0;

    private final WorldBorderFixConfig config = new WorldBorderFixConfig();
    private int tickCounter = 0;
    private int stableCount = 0;
    private boolean lastDesired = false;
    private int reloadCooldown = 0;

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
            releaseSuppression();
            tickCounter = 0;
            stableCount = 0;
        }
    }

    public WorldBorderFixConfig getConfig() {
        return config;
    }

    private void onClientTick(MinecraftClient client) {
        if (reloadCooldown > 0) {
            reloadCooldown--;
        }
        if (!config.isEnabled()) {
            // 設定画面から直接無効化された場合もここで確実に復元する
            releaseSuppression();
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
            stableCount = 0;
            return;
        }

        tickCounter++;
        if (tickCounter < CHECK_INTERVAL_TICKS) {
            return;
        }
        tickCounter = 0;

        boolean suppressed = NvidiumCompat.isSuppressed();
        boolean desired = shouldSuppress(world.getWorldBorder(), player, suppressed);

        if (desired == suppressed) {
            stableCount = 0;
            lastDesired = desired;
            return;
        }
        // 切替が必要な状態が連続して安定するまで待つ（サーバー間移動直後の
        // ボーダー未同期による誤判定での切替連発を防ぐ）
        if (desired != lastDesired) {
            lastDesired = desired;
            stableCount = 1;
            return;
        }
        stableCount++;
        if (stableCount < STABLE_CHECKS_REQUIRED) {
            return;
        }

        // 再ロードが必要なのは「Nvidium が動作中に抑制する」場合と
        // 「即時復帰オプションが有効な復帰」の場合のみ
        boolean needsReload = desired ? NvidiumCompat.isCurrentlyEnabled() : config.isAutoReenable();
        if (needsReload && reloadCooldown > 0) {
            return; // クールダウン中は次の判定で再試行
        }

        boolean changed = desired ? NvidiumCompat.suppress() : NvidiumCompat.restore();
        stableCount = 0;
        if (!changed) {
            return;
        }

        if (needsReload) {
            client.worldRenderer.reload();
            reloadCooldown = RELOAD_COOLDOWN_TICKS;
        }
        if (desired) {
            if (needsReload) {
                player.sendMessage(Text.translatable(
                        "message." + ASTTweaks.MOD_ID + ".worldborderfix.suppressed"), true);
            }
            ASTTweaks.LOGGER.info("WorldBorderFix: Nvidium temporarily disabled (near world border / far coordinates, reload={})", needsReload);
        } else {
            player.sendMessage(Text.translatable(
                    "message." + ASTTweaks.MOD_ID + (needsReload
                            ? ".worldborderfix.restored"
                            : ".worldborderfix.restoredDeferred")), true);
            ASTTweaks.LOGGER.info("WorldBorderFix: Nvidium restore (immediate reload={})", needsReload);
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

    /**
     * 機能の無効化時にフラグを元へ戻す。クラッシュ回避のためここでは再ロードせず、
     * 次の自然なレンダラー再ロードで Nvidium が復帰する。
     */
    private void releaseSuppression() {
        if (NvidiumCompat.restore()) {
            ASTTweaks.LOGGER.info("WorldBorderFix: feature disabled, Nvidium will return on the next renderer reload");
        }
    }
}

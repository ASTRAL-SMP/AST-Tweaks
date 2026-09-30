package com.astral.asttweaks.feature.nvidiumchunkfix;

import com.astral.asttweaks.ASTTweaks;
import com.astral.asttweaks.compat.NvidiumCompat;
import com.astral.asttweaks.feature.Feature;

/**
 * Nvidium（特に Bobby 併用時）で地形がリージョン単位（8x4x8 セクション = 128x64x128 ブロック）で
 * 抜け落ち、ブロック更新か F3+A まで戻らない問題の修正。
 *
 * 原因は Nvidium 0.1.12〜0.1.15 のリージョン破棄処理:
 * 1. 地形メモリが上限（既定は空き VRAM - 1GB）に達すると、最も長く見えていないリージョンの
 *    ジオメトリを GPU から破棄する（RenderPipeline.removeRegion）。Sodium には通知しないため、
 *    Sodium はそのセクションをビルド済みとみなして再ビルドせず、穴が残り続ける。
 *    Bobby は描画距離を大きく広げるので上限に達しやすく、併用時に顕在化する。
 * 2. 解放したリージョン ID の可視性統計をリセットしないため、同じ ID を再利用した新しい
 *    （プレイヤーの近くで読み込まれたばかりの）リージョンが古い統計を引き継ぎ、真っ先に破棄される。
 * 3. 破棄候補が無いと removeRegion(-1) で配列外参照になりクラッシュする。
 *
 * 対策は mixin.nvidium パッケージ:
 * - 破棄されたセクションを記録し、Sodium に再ビルドを予約する。Sodium は視界内のセクションから
 *   ビルドするので、見える位置に来た時点で自動的に復元される
 * - リージョン ID の解放時に可視性統計をリセットする
 * - 破棄候補が無いときの removeRegion(-1) を無視する
 *
 * 対象外バージョンの Nvidium・未導入時は mixin 自体が適用されず no-op。
 */
public class NvidiumChunkFixFeature implements Feature {
    private final NvidiumChunkFixConfig config = new NvidiumChunkFixConfig();

    @Override
    public String getId() {
        return "nvidiumchunkfix";
    }

    @Override
    public String getName() {
        return "Nvidium Chunk Fix";
    }

    @Override
    public void init() {
        if (NvidiumCompat.isRegionPatchSupported()) {
            ASTTweaks.LOGGER.info("Nvidium Chunk Fix feature initialized");
        } else {
            ASTTweaks.LOGGER.info("Nvidium 0.1.12-0.1.x not found - Nvidium Chunk Fix will be a no-op");
        }
    }

    @Override
    public void tick() {
        // 描画スレッド側（mixin）で処理する
    }

    @Override
    public boolean isEnabled() {
        return config.isEnabled();
    }

    @Override
    public void setEnabled(boolean enabled) {
        config.setEnabled(enabled);
        if (!enabled) {
            EvictedSections.reset();
        }
    }

    public NvidiumChunkFixConfig getConfig() {
        return config;
    }
}

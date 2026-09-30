package com.astral.asttweaks.feature.nvidiumchunkfix;

import com.astral.asttweaks.ASTTweaks;
import com.astral.asttweaks.compat.NvidiumCompat;
import com.astral.asttweaks.feature.Feature;

/**
 * Nvidium（特に Bobby 併用時）で地形がリージョン単位（8x4x8 セクション = 128x64x128 ブロック）で
 * 抜け落ち、ブロック更新か F3+A まで戻らない問題の修正。
 *
 * 原因はいずれも Nvidium 0.1.12〜0.1.15 側:
 * 1. リージョンの 256 セクションすべてが埋まると、シェーダーがセクション数を 0 と読んで
 *    リージョンごと描画しない（{@link RegionCountFix}）。Bobby で描画距離を広げて長く保持すると
 *    ネザー天井の層などで埋まりきって発生する。主因
 * 2. 地形メモリが上限（既定は空き VRAM - 1GB）に達すると、最も長く見えていないリージョンの
 *    ジオメトリを GPU から破棄する（RenderPipeline.removeRegion）。Sodium には通知しないため、
 *    Sodium はそのセクションをビルド済みとみなして再ビルドせず、穴が残り続ける
 * 3. 解放したリージョン ID の可視性統計をリセットしないため、同じ ID を再利用した新しい
 *    （プレイヤーの近くで読み込まれたばかりの）リージョンが古い統計を引き継ぎ、真っ先に破棄される
 * 4. 破棄候補が無いと removeRegion(-1) で配列外参照になりクラッシュする
 *
 * 対策は mixin.nvidium パッケージ:
 * - セクション数を「個数 - 1」で詰め、シェーダー側で + 1 して読む（上流 0.2 系と同じ修正）
 * - 破棄されたセクションを記録し、Sodium に再ビルドを予約する。Sodium は視界内のセクションから
 *   ビルドするので、見える位置に来た時点で自動的に復元される
 * - リージョン ID の解放時に可視性統計をリセットする
 * - 破棄候補が無いときの removeRegion(-1) を無視する
 *
 * シェーダーはパイプライン生成時にコンパイルされるため、有効/無効の切り替えは次のレンダラー
 * 再ロード（F3+A 等）から反映される。対象外バージョンの Nvidium・未導入時は mixin 自体が適用されず no-op。
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

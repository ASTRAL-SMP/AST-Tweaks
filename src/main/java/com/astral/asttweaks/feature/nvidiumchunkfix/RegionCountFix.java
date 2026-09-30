package com.astral.asttweaks.feature.nvidiumchunkfix;

import com.astral.asttweaks.ASTTweaks;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;

/**
 * Nvidium 0.1.x が 256 セクションすべてが埋まったリージョンを描画しない不具合の修正。
 *
 * RegionManager はリージョンのメタデータにセクション数（使用中の最大スロット + 1、最大 256）を
 * 48 ビット目から詰めるが、section_raster/task.glsl は {@code (data>>48)&0xFF} の 8 ビットで読む。
 * 256 は 0 になるため、そのリージョンのセクションが 1 つもディスパッチされず丸ごと消える
 * （さらに 256 の 9 ビット目が sizeZ のビットに食い込む）。ネザー天井の層のように全セクションに
 * 面があるリージョンで、Bobby で描画距離を広げて長く保持していると埋まりきって発生する。
 *
 * 上流では 0.2 系の 1ed0dc5「Fix chunks not rendering at far render distances」で
 * 「個数 - 1 を詰めてシェーダー側で + 1」に変えて直っているので、同じ形にする。
 * シェーダーはパイプライン生成時にコンパイルされるため、書き換えたかどうかをパイプライン単位で
 * 保持し、Java 側の詰め方をそれに合わせる（途中で機能を切り替えても両者は食い違わない）。
 *
 * Nvidium / Sodium のクラスを参照しないこと。
 */
public final class RegionCountFix {
    public static final String SECTION_RASTER_TASK = "occlusion/section_raster/task.glsl";

    private static final String COUNT_DECODE_ORIGINAL = "uint8_t count = (uint8_t)((data>>48)&0xFF);";
    private static final String COUNT_DECODE_PATCHED = "int count = int((data>>48)&0xFF)+1;";
    private static final String COUNT_STORE_ORIGINAL = "_count = count;";
    private static final String COUNT_STORE_PATCHED = "_count = uint8_t(count);";

    private static final int SECTIONS_PER_REGION = 8 * 4 * 8;

    // 現在のパイプラインのシェーダーが「個数 - 1」形式で読むか
    private static boolean active = false;

    // 全スロットが埋まっている（パッチ無しだと描画されない）リージョン ID
    private static final IntOpenHashSet FULL_REGIONS = new IntOpenHashSet();
    private static boolean fullRegionLogged = false;

    private RegionCountFix() {
    }

    /**
     * section_raster/task.glsl のセクション数の読み方を「個数 - 1 + 1」に書き換える。
     * パイプライン生成時（シェーダーのコンパイル時）に呼ばれ、以降の Java 側の詰め方もこれで決まる。
     */
    public static String patchSectionRasterTask(String source, boolean enabled) {
        active = false;
        if (!enabled) {
            return source;
        }
        if (!source.contains(COUNT_DECODE_ORIGINAL) || !source.contains(COUNT_STORE_ORIGINAL)) {
            ASTTweaks.LOGGER.warn("[Nvidium Chunk Fix] Unexpected Nvidium section raster shader, 256-section region fix not applied");
            return source;
        }
        active = true;
        ASTTweaks.LOGGER.info("[Nvidium Chunk Fix] Patched Nvidium section raster shader to draw regions holding all 256 sections");
        return source
                .replace(COUNT_DECODE_ORIGINAL, COUNT_DECODE_PATCHED)
                .replace(COUNT_STORE_ORIGINAL, COUNT_STORE_PATCHED);
    }

    public static boolean isActive() {
        return active;
    }

    /**
     * リージョンのメタデータに詰めるセクション数を返す。count は使用中の最大スロット + 1（1〜256）。
     */
    public static int encodeCount(int regionId, int count, int regionX, int regionY, int regionZ, boolean enabled) {
        if (count >= SECTIONS_PER_REGION) {
            if (FULL_REGIONS.add(regionId) && enabled && !fullRegionLogged) {
                fullRegionLogged = true;
                ASTTweaks.LOGGER.info("[Nvidium Chunk Fix] Nvidium region at block ({}, {}, {}) holds all 256 sections - {}",
                        regionX << 7, regionY << 6, regionZ << 7,
                        active ? "drawn thanks to the fix"
                                : "Nvidium 0.1.x cannot draw it; the fix applies after the next renderer reload (F3+A)");
            }
        } else {
            FULL_REGIONS.remove(regionId);
        }
        return active ? count - 1 : count;
    }

    public static void onRegionReleased(int regionId) {
        FULL_REGIONS.remove(regionId);
    }

    public static int getFullRegionCount() {
        return FULL_REGIONS.size();
    }

    /**
     * 新しいパイプラインの生成時に呼ぶ（リージョン ID はパイプラインごとに振り直される）。
     */
    public static void resetRegions() {
        FULL_REGIONS.clear();
        fullRegionLogged = false;
    }
}

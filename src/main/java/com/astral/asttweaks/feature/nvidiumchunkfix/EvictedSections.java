package com.astral.asttweaks.feature.nvidiumchunkfix;

import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

/**
 * Nvidium が GPU から破棄したセクションのうち、まだ Sodium に再ビルドを予約していないものを保持する。
 * キーは {@code ChunkSectionPos.asLong} 形式。描画スレッドからのみ触る。
 *
 * Nvidium / Sodium のクラスを参照しないこと（未導入環境でも Feature 経由でロードされるため）。
 */
public final class EvictedSections {
    private static final LongOpenHashSet PENDING = new LongOpenHashSet();

    // F3 表示用の累計（レンダラー再ロードでリセット）
    private static int evictedRegions = 0;
    private static int restoredSections = 0;

    private EvictedSections() {
    }

    public static void add(long sectionKey) {
        PENDING.add(sectionKey);
    }

    public static boolean isEmpty() {
        return PENDING.isEmpty();
    }

    public static int size() {
        return PENDING.size();
    }

    /**
     * 保留中のセクションを走査する。処理済みのものは {@link LongIterator#remove()} で取り除く。
     */
    public static LongIterator iterator() {
        return PENDING.iterator();
    }

    public static void onRegionEvicted() {
        evictedRegions++;
    }

    public static void onSectionRestored() {
        restoredSections++;
    }

    public static int getEvictedRegions() {
        return evictedRegions;
    }

    public static int getRestoredSections() {
        return restoredSections;
    }

    /**
     * レンダラー破棄時・機能無効化時に呼ぶ。再ロード後は Sodium が全セクションを最初からビルドするので保留分は不要。
     */
    public static void reset() {
        PENDING.clear();
        evictedRegions = 0;
        restoredSections = 0;
    }
}

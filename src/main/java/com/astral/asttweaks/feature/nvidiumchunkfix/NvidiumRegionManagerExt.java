package com.astral.asttweaks.feature.nvidiumchunkfix;

import java.util.function.IntConsumer;

/**
 * Nvidium の RegionManager に mixin で追加するインターフェース。
 * リージョン ID が解放されたときに呼ばれるリスナーを登録する。
 */
public interface NvidiumRegionManagerExt {
    void asttweaks$setReleaseListener(IntConsumer listener);
}

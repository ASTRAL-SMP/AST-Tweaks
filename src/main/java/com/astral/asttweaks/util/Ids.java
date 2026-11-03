package com.astral.asttweaks.util;

import net.minecraft.resources.Identifier;

/**
 * Identifier（1.21.11 以前は Identifier）の生成。1.21 からコンストラクタが非公開になった。
 */
public final class Ids {
    private Ids() {}

    public static Identifier of(String namespace, String path) {
        //? if >=1.21 {
        return Identifier.fromNamespaceAndPath(namespace, path);
        //?} else {
        /*return new Identifier(namespace, path);
        *///?}
    }

    public static Identifier mc(String path) {
        //? if >=1.21 {
        return Identifier.withDefaultNamespace(path);
        //?} else {
        /*return new Identifier("minecraft", path);
        *///?}
    }
}

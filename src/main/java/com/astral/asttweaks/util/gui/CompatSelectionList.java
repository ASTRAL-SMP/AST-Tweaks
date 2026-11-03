package com.astral.asttweaks.util.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ObjectSelectionList;

/**
 * ObjectSelectionList のコンストラクタ差を吸収する。
 * 26.1 はリストの高さを、それ以前は画面の高さと下端を受け取る。
 */
public abstract class CompatSelectionList<E extends CompatListEntry<E>> extends ObjectSelectionList<E> {

    /** top / bottom は画面座標 */
    protected CompatSelectionList(Minecraft minecraft, int width, int screenHeight, int top, int bottom, int itemHeight) {
        //? if >=26.1 {
        super(minecraft, width, bottom - top, top, itemHeight);
        //?} else {
        /*super(minecraft, width, screenHeight, top, bottom, itemHeight);
        *///?}
    }

    public void clearAll() {
        this.clearEntries();
    }

    @Override
    public int addEntry(E entry) {
        return super.addEntry(entry);
    }

    /** 行を広げた分だけスクロールバーを右にずらす（26.1+ は不要） */
    protected int scrollbarOffset() {
        return 0;
    }

    //? if <26.1 {
    /*@Override
    protected int getScrollbarPosition() {
        return super.getScrollbarPosition() + scrollbarOffset();
    }
    *///?}
}

package com.astral.asttweaks.feature.autorestock;

import java.util.ArrayList;
import java.util.List;

/**
 * Per-item auto restock rule.
 */
public class AutoRestockEntry {
    public String itemId;
    public String nbt;
    public Integer targetSlot;
    public List<Integer> targetSlots = new ArrayList<>();
    public int desiredCount;

    public AutoRestockEntry() {
    }

    public AutoRestockEntry(String itemId, String nbt, List<Integer> targetSlots, int desiredCount) {
        this.itemId = itemId;
        this.nbt = nbt;
        this.targetSlots = new ArrayList<>(targetSlots);
        this.desiredCount = desiredCount;
    }
}

package com.astral.asttweaks.compat;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import com.astral.asttweaks.ASTTweaks;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Litematica で死んだサンゴ系ブロックを設置する際、手持ちに死サンゴが無くても
 * 対応する生きたサンゴで代用できるようにするための置換ロジック。
 * 生サンゴは水の外に置くとすぐ枯れて死サンゴになるため、スケマティック通りに揃う。
 * MaterialCacheMixin から呼ばれる。
 */
public class LitematicaCoralCompat {
    // dead item -> live item（サンゴ系でないアイテムは null を記録して再判定を省く）
    private static final Map<Item, Item> DEAD_TO_LIVE_CACHE = new IdentityHashMap<>();
    // 動作確認用ログをアイテム種別ごとに 1 回だけ出すための記録
    private static final Set<Item> LOGGED_ITEMS = Collections.newSetFromMap(new IdentityHashMap<>());

    /**
     * Litematica が要求するアイテムが死サンゴ系で、プレイヤーが死サンゴを持っておらず
     * 対応する生サンゴを持っている場合のみ、生サンゴの ItemStack を返す。
     * 置換しない場合は null。
     */
    public static ItemStack substituteDeadCoral(ItemStack required) {
        if (required == null || required.isEmpty()) {
            return null;
        }
        Item dead = required.getItem();
        Item live = lookupLiveCoral(dead);
        if (live == null) {
            return null;
        }
        // 死サンゴを持っているなら本来のアイテムを優先する
        if (playerHasItem(dead)) {
            return null;
        }
        // 生サンゴも持っていなければ置換せず、元の不足表示を維持する
        if (!playerHasItem(live)) {
            return null;
        }
        if (LOGGED_ITEMS.add(dead)) {
            ASTTweaks.LOGGER.info("LitematicaCoral: substituting {} -> {}",
                    Registries.ITEM.getId(dead), Registries.ITEM.getId(live));
        }
        return new ItemStack(live, required.getCount());
    }

    /** dead_ プレフィックスを外した同名サンゴアイテムが存在すればそれを返す */
    private static Item lookupLiveCoral(Item dead) {
        if (DEAD_TO_LIVE_CACHE.containsKey(dead)) {
            return DEAD_TO_LIVE_CACHE.get(dead);
        }
        Item live = null;
        Identifier id = Registries.ITEM.getId(dead);
        if (id.getNamespace().equals("minecraft")
                && id.getPath().startsWith("dead_")
                && id.getPath().contains("coral")) {
            Identifier liveId = new Identifier(id.getNamespace(), id.getPath().substring("dead_".length()));
            if (Registries.ITEM.containsId(liveId)) {
                live = Registries.ITEM.get(liveId);
            }
        }
        DEAD_TO_LIVE_CACHE.put(dead, live);
        return live;
    }

    private static boolean playerHasItem(Item item) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) {
            return false;
        }
        PlayerInventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.size(); slot++) {
            ItemStack stack = inventory.getStack(slot);
            if (!stack.isEmpty() && stack.getItem() == item) {
                return true;
            }
        }
        return false;
    }
}

package com.astral.asttweaks.compat;

import com.astral.asttweaks.ASTTweaks;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.screen.MerchantScreenHandler;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * ItemScroller（masa）の村人取引お気に入りとの連携。リフレクションで呼ぶので ItemScroller は任意依存。
 *
 * ItemScroller は村人を手で右クリックしたときだけ「最後に取引した村人」を記録し、その村人のお気に入りを
 * 取引一覧の先頭に並べ替える。Void Trade はパケットで右クリックするため、記録は自前で渡す。
 */
public final class ItemScrollerCompat {
    private static final String STORAGE_CLASS = "fi.dy.masa.itemscroller.villager.VillagerDataStorage";

    private static boolean initialized = false;
    private static Object storage;
    private static Method setLastInteractedUUID;
    private static Method getFavoritesForCurrentVillager;
    private static Field favoritesField;

    private ItemScrollerCompat() {}

    public static boolean isLoaded() {
        return FabricLoader.getInstance().isModLoaded("itemscroller");
    }

    private static boolean init() {
        if (initialized) {
            return storage != null;
        }
        initialized = true;
        if (!isLoaded()) {
            return false;
        }
        try {
            Class<?> storageClass = Class.forName(STORAGE_CLASS);
            Object instance = storageClass.getMethod("getInstance").invoke(null);
            Method setUuid = storageClass.getMethod("setLastInteractedUUID", UUID.class);
            // 引数型（MerchantScreenHandler）は実行環境のマッピング名になるので、名前と代入可能性で探す
            Method getFavorites = null;
            for (Method method : storageClass.getMethods()) {
                if (method.getName().equals("getFavoritesForCurrentVillager")
                        && method.getParameterCount() == 1
                        && method.getParameterTypes()[0].isAssignableFrom(MerchantScreenHandler.class)) {
                    getFavorites = method;
                }
            }
            if (getFavorites == null) {
                throw new NoSuchMethodException("getFavoritesForCurrentVillager(MerchantScreenHandler)");
            }
            favoritesField = getFavorites.getReturnType().getField("favorites");
            setLastInteractedUUID = setUuid;
            getFavoritesForCurrentVillager = getFavorites;
            storage = instance;
            ASTTweaks.LOGGER.info("ItemScroller villager favorites integration enabled");
            return true;
        } catch (ReflectiveOperationException | RuntimeException e) {
            ASTTweaks.LOGGER.warn("ItemScroller found but villager favorites API is unavailable: {}", e.toString());
            return false;
        }
    }

    /**
     * これから取引する村人を ItemScroller に伝える（取引一覧の並べ替えとお気に入りの対象がこの村人になる）。
     */
    public static void setLastInteractedVillager(UUID uuid) {
        if (!init()) {
            return;
        }
        try {
            setLastInteractedUUID.invoke(storage, uuid);
        } catch (ReflectiveOperationException | RuntimeException e) {
            ASTTweaks.LOGGER.warn("Failed to pass villager to ItemScroller: {}", e.toString());
        }
    }

    /**
     * 開いている取引画面の村人のお気に入り（元の取引一覧での番号、0 始まり）。ItemScroller が無いか未設定なら空。
     */
    public static List<Integer> getFavoriteTradeIndices(MerchantScreenHandler handler) {
        List<Integer> result = new ArrayList<>();
        if (!init()) {
            return result;
        }
        try {
            Object data = getFavoritesForCurrentVillager.invoke(storage, handler);
            if (data != null && favoritesField.get(data) instanceof List<?> favorites) {
                for (Object index : favorites) {
                    if (index instanceof Number number) {
                        result.add(number.intValue());
                    }
                }
            }
        } catch (ReflectiveOperationException | RuntimeException e) {
            ASTTweaks.LOGGER.warn("Failed to read ItemScroller favorites: {}", e.toString());
        }
        return result;
    }
}

package com.astral.asttweaks.compat;

import com.astral.asttweaks.ASTTweaks;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import org.apache.commons.lang3.tuple.Pair;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Reads server-side entity NBT through the data syncers of other client mods,
 * so features can see data the server doesn't send to the client normally.
 *
 * - Tweakeroo {@code entityDataSync}: Servux, or the vanilla NBT query when
 *   {@code entityDataSyncBackup} is on and the player is an operator. Requests are
 *   only sent while those Tweakeroo options are enabled.
 * - TweakerMore {@code serverDataSyncer}: the vanilla NBT query (op level 2 or
 *   Carpet TIS Addition). Used only while that TweakerMore option is enabled.
 *
 * Uses reflection to avoid a hard dependency on either mod.
 */
public final class EntityDataSyncCompat {
    private static final long TWEAKERMORE_REFRESH_MILLIS = 10_000L;

    private static boolean initialized = false;

    private static Object tweakerooManager = null;
    private static Method tweakerooRequestEntityNbt = null;
    private static Method tweakerooGetBestWorld = null;

    private static Object tweakerMoreSyncer = null;
    private static Method tweakerMoreFetchEntity = null;
    private static Object tweakerMoreSyncerOption = null;
    private static Method tweakerMoreGetBooleanValue = null;

    // Entity ids are per level, so the cache is dropped whenever the level changes
    private static final Map<Integer, CachedNbt> tweakerMoreCache = new ConcurrentHashMap<>();
    private static net.minecraft.world.level.Level cacheLevel = null;

    private record CachedNbt(CompoundTag nbt, long time, boolean pending) {}

    private EntityDataSyncCompat() {}

    /**
     * Returns the most recent server NBT known for the entity, or null. May schedule a
     * refresh in the background; call again later to pick up the result.
     */
    public static CompoundTag getEntityNbt(Entity entity) {
        init();
        if (entity.level() != cacheLevel) {
            cacheLevel = entity.level();
            tweakerMoreCache.clear();
        }
        CompoundTag nbt = fromTweakeroo(entity);
        if (nbt != null) {
            return nbt;
        }
        return fromTweakerMore(entity);
    }

    private static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        if (FabricLoader.getInstance().isModLoaded("tweakeroo")) {
            try {
                Class<?> managerClass = Class.forName("fi.dy.masa.tweakeroo.data.EntityDataManager");
                tweakerooManager = managerClass.getMethod("getInstance").invoke(null);
                tweakerooRequestEntityNbt = managerClass.getMethod("requestEntityNbt",
                        net.minecraft.world.level.Level.class, int.class);
                tweakerooGetBestWorld = managerClass.getMethod("getBestWorld");
                ASTTweaks.LOGGER.info("Tweakeroo entity data sync available");
            } catch (ReflectiveOperationException | LinkageError e) {
                tweakerooManager = null;
                ASTTweaks.LOGGER.warn("Tweakeroo entity data sync API changed - not used: {}", e.toString());
            }
        }

        if (FabricLoader.getInstance().isModLoaded("tweakermore")) {
            try {
                Class<?> syncerClass = Class.forName(
                        "me.fallenbreath.tweakermore.impl.mod_tweaks.serverDataSyncer.ServerDataSyncer");
                tweakerMoreSyncer = syncerClass.getMethod("getInstance").invoke(null);
                tweakerMoreFetchEntity = syncerClass.getMethod("fetchEntity", Entity.class);
                Class<?> configsClass = Class.forName("me.fallenbreath.tweakermore.config.TweakerMoreConfigs");
                tweakerMoreSyncerOption = configsClass.getField("SERVER_DATA_SYNCER").get(null);
                tweakerMoreGetBooleanValue = tweakerMoreSyncerOption.getClass().getMethod("getBooleanValue");
                ASTTweaks.LOGGER.info("TweakerMore server data syncer available");
            } catch (ReflectiveOperationException | LinkageError e) {
                tweakerMoreSyncer = null;
                ASTTweaks.LOGGER.warn("TweakerMore server data syncer API changed - not used: {}", e.toString());
            }
        }
    }

    private static CompoundTag fromTweakeroo(Entity entity) {
        if (tweakerooManager == null) {
            return null;
        }
        try {
            // The integrated server's level in singleplayer; the client level's entity has no brain memories
            Object world = tweakerooGetBestWorld.invoke(tweakerooManager);
            if (world == null) {
                return null;
            }
            Object result = tweakerooRequestEntityNbt.invoke(tweakerooManager, world, entity.getId());
            if (result instanceof Pair<?, ?> pair && pair.getRight() instanceof CompoundTag nbt && !nbt.isEmpty()) {
                return nbt;
            }
        } catch (ReflectiveOperationException | RuntimeException e) {
            ASTTweaks.LOGGER.warn("Tweakeroo entity data sync failed - disabling: {}", e.toString());
            tweakerooManager = null;
        }
        return null;
    }

    private static CompoundTag fromTweakerMore(Entity entity) {
        if (tweakerMoreSyncer == null) {
            return null;
        }
        CachedNbt cached = tweakerMoreCache.get(entity.getId());
        long now = System.currentTimeMillis();
        try {
            boolean enabled = (boolean) tweakerMoreGetBooleanValue.invoke(tweakerMoreSyncerOption);
            boolean stale = cached == null || now - cached.time() > TWEAKERMORE_REFRESH_MILLIS;
            if (enabled && stale && (cached == null || !cached.pending())) {
                @SuppressWarnings("unchecked")
                Optional<CompletableFuture<CompoundTag>> future =
                        (Optional<CompletableFuture<CompoundTag>>) tweakerMoreFetchEntity.invoke(tweakerMoreSyncer, entity);
                if (future.isPresent()) {
                    int id = entity.getId();
                    CompoundTag previous = cached != null ? cached.nbt() : null;
                    tweakerMoreCache.put(id, new CachedNbt(previous, now, true));
                    future.get().whenComplete((nbt, error) ->
                            tweakerMoreCache.put(id, new CachedNbt(nbt != null ? nbt : previous, System.currentTimeMillis(), false)));
                }
            }
        } catch (ReflectiveOperationException | RuntimeException e) {
            ASTTweaks.LOGGER.warn("TweakerMore server data syncer failed - disabling: {}", e.toString());
            tweakerMoreSyncer = null;
        }
        return cached != null ? cached.nbt() : null;
    }
}

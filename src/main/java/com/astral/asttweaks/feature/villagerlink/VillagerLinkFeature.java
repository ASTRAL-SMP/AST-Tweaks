package com.astral.asttweaks.feature.villagerlink;

import com.astral.asttweaks.ASTTweaks;
import com.astral.asttweaks.compat.EntityDataSyncCompat;
import com.astral.asttweaks.feature.Feature;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.gizmos.GizmoProperties;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.debug.DebugBrainDump;
import net.minecraft.util.debug.DebugSubscriptions;
import net.minecraft.util.debug.DebugValueAccess;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Visualizes the relationship between villagers and their job site blocks
 * by drawing lines between matched pairs.
 *
 * Primary source: vanilla brain debug values ({@link DebugSubscriptions#BRAINS}),
 * which the client subscribes to while this feature is enabled. This is
 * authoritative — each brain dump carries the actual claimed POI set. The
 * server only sends them to players with debug permission (op, or the
 * singleplayer owner; see {@code ServerDebugSubscribersMixin}).
 *
 * Second source: the villager's {@code job_site} memory from server NBT synced by
 * Tweakeroo's entityDataSync (Servux) or TweakerMore's serverDataSyncer, when
 * either is installed and enabled.
 *
 * Fallback: when neither is available, the original heuristic kicks in — we
 * scan a small radius around the villager and pick the nearest POI block
 * matching the profession's workstation predicate.
 */
public class VillagerLinkFeature implements Feature {

    private static final int BLOCK_SEARCH_RADIUS = 8;
    private static final int UPDATE_INTERVAL_TICKS = 20;

    private final VillagerLinkConfig config;
    private final Map<Villager, BlockPos> cachedLinks = new HashMap<>();

    private int tickCounter = 0;

    public VillagerLinkFeature() {
        this.config = new VillagerLinkConfig();
    }

    @Override
    public String getId() {
        return "villagerlink";
    }

    @Override
    public String getName() {
        return "Villager Link";
    }

    @Override
    public void init() {
        LevelRenderEvents.END_EXTRACTION.register(this::renderLinks);
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(ASTTweaks.MOD_ID, "villagerlink_jobsite"),
                this::renderJobSiteOverlay);
        ASTTweaks.LOGGER.info("VillagerLink feature initialized");
    }

    @Override
    public void tick() {
        tickCounter++;
        if (tickCounter < UPDATE_INTERVAL_TICKS) {
            return;
        }
        tickCounter = 0;
        updateLinks();
    }

    @Override
    public boolean isEnabled() {
        return config.isEnabled();
    }

    @Override
    public void setEnabled(boolean enabled) {
        config.setEnabled(enabled);
        if (!enabled) {
            cachedLinks.clear();
        }
    }

    public VillagerLinkConfig getConfig() {
        return config;
    }

    private void updateLinks() {
        cachedLinks.clear();

        Minecraft client = Minecraft.getInstance();
        ClientLevel world = client.level;
        if (world == null || client.player == null) {
            return;
        }

        int range = Math.max(1, config.getRange());
        AABB searchBox = new AABB(client.player.position(), client.player.position()).inflate(range);
        List<Villager> villagers = world.getEntitiesOfClass(Villager.class, searchBox, e -> !e.isRemoved());

        DebugValueAccess brains = getDebugBrains(client);

        for (Villager villager : villagers) {
            Holder<VillagerProfession> profession = villager.getVillagerData().profession();
            if (profession.is(VillagerProfession.NONE) || profession.is(VillagerProfession.NITWIT)) {
                continue;
            }

            Predicate<Holder<PoiType>> predicate = profession.value().heldJobSite();
            if (predicate == null) {
                continue;
            }

            BlockPos jobSite = findJobSiteFromDebugData(brains, villager, world, predicate);
            if (jobSite == null) {
                jobSite = findJobSiteFromEntityData(villager, world, predicate);
            }
            if (jobSite == null) {
                jobSite = findClosestJobSite(world, villager.blockPosition(), predicate);
            }
            if (jobSite != null) {
                cachedLinks.put(villager, jobSite);
            }
        }
    }

    private DebugValueAccess getDebugBrains(Minecraft client) {
        ClientPacketListener connection = client.getConnection();
        return connection != null ? connection.createDebugValueAccess() : null;
    }

    private BlockPos findJobSiteFromDebugData(DebugValueAccess brains, Villager villager,
                                               ClientLevel world, Predicate<Holder<PoiType>> predicate) {
        if (brains == null) {
            return null;
        }
        DebugBrainDump brain = brains.getEntityValue(DebugSubscriptions.BRAINS, villager);
        if (brain == null) {
            return null;
        }
        Set<BlockPos> claimed = brain.pois();
        if (claimed == null || claimed.isEmpty()) {
            return null;
        }
        for (BlockPos pos : claimed) {
            if (isJobSiteBlock(world, pos, predicate)) {
                return pos.immutable();
            }
        }
        return null;
    }

    /**
     * Reads the villager's {@code job_site} brain memory from server NBT synced by
     * Tweakeroo (entityDataSync / Servux) or TweakerMore (serverDataSyncer).
     */
    private BlockPos findJobSiteFromEntityData(Villager villager, ClientLevel world, Predicate<Holder<PoiType>> predicate) {
        CompoundTag nbt = EntityDataSyncCompat.getEntityNbt(villager);
        if (nbt == null) {
            return null;
        }
        CompoundTag jobSiteMemory = nbt.getCompoundOrEmpty("Brain")
                .getCompoundOrEmpty("memories")
                .getCompoundOrEmpty("minecraft:job_site")
                .getCompoundOrEmpty("value");
        Optional<GlobalPos> jobSite = GlobalPos.CODEC.parse(NbtOps.INSTANCE, jobSiteMemory).result();
        if (jobSite.isEmpty() || !jobSite.get().dimension().equals(world.dimension())) {
            return null;
        }
        BlockPos pos = jobSite.get().pos();
        return isJobSiteBlock(world, pos, predicate) ? pos : null;
    }

    private static boolean isJobSiteBlock(ClientLevel world, BlockPos pos, Predicate<Holder<PoiType>> predicate) {
        BlockState state = world.getBlockState(pos);
        if (state.isAir()) {
            return false;
        }
        Optional<Holder<PoiType>> poi = PoiTypes.forState(state);
        return poi.isPresent() && predicate.test(poi.get());
    }

    private BlockPos findClosestJobSite(ClientLevel world, BlockPos origin, Predicate<Holder<PoiType>> predicate) {
        BlockPos closest = null;
        double closestDistSq = Double.MAX_VALUE;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        int r = BLOCK_SEARCH_RADIUS;
        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    cursor.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
                    BlockState state = world.getBlockState(cursor);
                    if (state.isAir()) continue;

                    Optional<Holder<PoiType>> poi = PoiTypes.forState(state);
                    if (poi.isEmpty()) continue;
                    if (!predicate.test(poi.get())) continue;

                    double distSq = origin.distSqr(cursor);
                    if (distSq < closestDistSq) {
                        closestDistSq = distSq;
                        closest = cursor.immutable();
                    }
                }
            }
        }
        return closest;
    }

    private void renderLinks(LevelExtractionContext context) {
        if (!isEnabled() || cachedLinks.isEmpty()) {
            return;
        }

        float tickDelta = context.deltaTracker().getGameTimeDeltaPartialTick(false);

        int color = config.getLineColor();
        if ((color >>> 24) == 0) {
            color |= 0xFF000000;
        }

        boolean seeThrough = config.isSeeThrough();

        for (Map.Entry<Villager, BlockPos> entry : cachedLinks.entrySet()) {
            Villager villager = entry.getKey();
            if (villager.isRemoved()) continue;

            Vec3 villagerPos = villager.getPosition(tickDelta).add(0.0, villager.getBbHeight() * 0.5, 0.0);
            Vec3 blockPos = Vec3.atCenterOf(entry.getValue());

            GizmoProperties line = Gizmos.line(villagerPos, blockPos, color, 2.0f);
            if (seeThrough) {
                line.setAlwaysOnTop();
            }
        }
    }

    private void renderJobSiteOverlay(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        if (!isEnabled()) return;

        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) return;
        if (client.options.hideGui) return;
        if (client.screen != null) return;

        HitResult target = client.hitResult;
        if (target == null || target.getType() != HitResult.Type.ENTITY) return;
        Entity entity = ((EntityHitResult) target).getEntity();
        if (!(entity instanceof Villager villager)) return;

        BlockPos linked = cachedLinks.get(villager);
        if (linked == null) return;

        Font textRenderer = client.font;
        Component text = Component.translatable(
                "hud." + ASTTweaks.MOD_ID + ".villagerlink.jobsite",
                linked.getX(), linked.getY(), linked.getZ());
        int textWidth = textRenderer.width(text);

        int screenWidth = graphics.guiWidth();
        int screenHeight = graphics.guiHeight();

        // ホットバー上部に配置（ホットバー本体: 22px + HP/満腹度バー: 約10px + 余白）
        int x = (screenWidth - textWidth) / 2;
        int y = screenHeight - 59;

        graphics.fill(x - 4, y - 2, x + textWidth + 4, y + textRenderer.lineHeight + 2, 0x80000000);
        graphics.text(textRenderer, text, x, y, 0xFFFFFFFF);
    }
}

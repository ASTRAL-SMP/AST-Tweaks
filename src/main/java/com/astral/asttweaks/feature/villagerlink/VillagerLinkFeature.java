package com.astral.asttweaks.feature.villagerlink;

import com.astral.asttweaks.ASTTweaks;
import com.astral.asttweaks.compat.EntityDataSyncCompat;
import com.astral.asttweaks.feature.Feature;
import com.astral.asttweaks.util.McClient;
import com.astral.asttweaks.util.gui.Gfx;
//? if >=26.1 {
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.gizmos.GizmoProperties;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.debug.DebugBrainDump;
import net.minecraft.util.debug.DebugSubscriptions;
import net.minecraft.util.debug.DebugValueAccess;
//?} else {
/*import com.astral.asttweaks.mixin.VillageDebugRendererAccessor;
import com.astral.asttweaks.mixin.VillageDebugRendererBrainAccessor;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.debug.BrainDebugRenderer;
import net.minecraft.util.Mth;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import java.util.UUID;
*///?}
//? if >=1.20 && <26.1 {
/*import net.minecraft.client.gui.GuiGraphics;
*///?}
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
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
        //? if >=26.1 {
        LevelRenderEvents.END_EXTRACTION.register(this::renderLinks);
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(ASTTweaks.MOD_ID, "villagerlink_jobsite"),
                this::renderJobSiteOverlay);
        //?} else {
        /*WorldRenderEvents.AFTER_TRANSLUCENT.register(this::renderLinks);
        HudRenderCallback.EVENT.register(this::renderJobSiteOverlay);
        *///?}
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

        //? if >=26.1 {
        DebugValueAccess brains = getDebugBrains(client);
        //?} else {
        /*Map<UUID, BrainDebugRenderer.BrainDump> brains = getDebugBrains(client);
        *///?}

        for (Villager villager : villagers) {
            //? if >=26.1 {
            Holder<VillagerProfession> profession = villager.getVillagerData().profession();
            if (profession.is(VillagerProfession.NONE) || profession.is(VillagerProfession.NITWIT)) {
                continue;
            }

            Predicate<Holder<PoiType>> predicate = profession.value().heldJobSite();
            //?} else {
            /*VillagerProfession profession = villager.getVillagerData().getProfession();
            if (profession == VillagerProfession.NONE || profession == VillagerProfession.NITWIT) {
                continue;
            }

            Predicate<Holder<PoiType>> predicate = profession.heldJobSite();
            *///?}
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

    //? if >=26.1 {
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
    //?} else {
    /*private Map<UUID, BrainDebugRenderer.BrainDump> getDebugBrains(Minecraft client) {
        if (client.debugRenderer == null || client.debugRenderer.brainDebugRenderer == null) {
            return null;
        }
        return ((VillageDebugRendererAccessor) client.debugRenderer.brainDebugRenderer).getBrains();
    }

    private BlockPos findJobSiteFromDebugData(Map<UUID, BrainDebugRenderer.BrainDump> brains, Villager villager,
                                               ClientLevel world, Predicate<Holder<PoiType>> predicate) {
        if (brains == null || brains.isEmpty()) {
            return null;
        }
        BrainDebugRenderer.BrainDump brain = brains.get(villager.getUUID());
        if (brain == null) {
            return null;
        }
        Set<BlockPos> claimed = ((VillageDebugRendererBrainAccessor) brain).getPointsOfInterest();
    *///?}
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
        //? if >=26.1 {
        CompoundTag jobSiteMemory = nbt.getCompoundOrEmpty("Brain")
                .getCompoundOrEmpty("memories")
                .getCompoundOrEmpty("minecraft:job_site")
                .getCompoundOrEmpty("value");
        //?} else {
        /*CompoundTag jobSiteMemory = nbt.getCompound("Brain")
                .getCompound("memories")
                .getCompound("minecraft:job_site")
                .getCompound("value");
        *///?}
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

    //? if >=26.1 {
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
    //?} else {
    /*private void renderLinks(WorldRenderContext context) {
        if (!isEnabled() || cachedLinks.isEmpty()) {
            return;
        }

        Minecraft client = Minecraft.getInstance();
        if (client.level == null) {
            return;
        }

        Camera camera = context.camera();
        Vec3 camPos = camera.getPosition();
        float tickDelta = context.tickDelta();

        int color = config.getLineColor();
        float a = ((color >> 24) & 0xFF) / 255f;
        if (a <= 0f) a = 1.0f;
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;

        boolean seeThrough = config.isSeeThrough();

        PoseStack matrices = context.matrixStack();
        matrices.pushPose();
        matrices.translate(-camPos.x, -camPos.y, -camPos.z);

        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.lineWidth(2.0f);
        if (seeThrough) {
            RenderSystem.disableDepthTest();
        }
        RenderSystem.setShader(GameRenderer::getRendertypeLinesShader);

        Tesselator tessellator = Tesselator.getInstance();
        BufferBuilder buffer = tessellator.getBuilder();
        buffer.begin(VertexFormat.Mode.LINES, DefaultVertexFormat.POSITION_COLOR_NORMAL);

        Matrix4f positionMatrix = matrices.last().pose();
        Matrix3f normalMatrix = matrices.last().normal();

        for (Map.Entry<Villager, BlockPos> entry : cachedLinks.entrySet()) {
            Villager villager = entry.getKey();
            if (villager.isRemoved()) continue;

            double vx = Mth.lerp(tickDelta, villager.xo, villager.getX());
            double vy = Mth.lerp(tickDelta, villager.yo, villager.getY()) + villager.getBbHeight() * 0.5;
            double vz = Mth.lerp(tickDelta, villager.zo, villager.getZ());

            BlockPos blockPos = entry.getValue();
            double bx = blockPos.getX() + 0.5;
            double by = blockPos.getY() + 0.5;
            double bz = blockPos.getZ() + 0.5;

            float dx = (float) (bx - vx);
            float dy = (float) (by - vy);
            float dz = (float) (bz - vz);
            float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (len > 0.0001f) {
                dx /= len;
                dy /= len;
                dz /= len;
            } else {
                dx = 0f; dy = 1f; dz = 0f;
            }

            buffer.vertex(positionMatrix, (float) vx, (float) vy, (float) vz)
                    .color(r, g, b, a)
                    .normal(normalMatrix, dx, dy, dz)
                    .endVertex();
            buffer.vertex(positionMatrix, (float) bx, (float) by, (float) bz)
                    .color(r, g, b, a)
                    .normal(normalMatrix, dx, dy, dz)
                    .endVertex();
        }

        tessellator.end();

        if (seeThrough) {
            RenderSystem.enableDepthTest();
        }
        RenderSystem.disableBlend();
        RenderSystem.enableCull();
        RenderSystem.lineWidth(1.0f);

        matrices.popPose();
    }
    *///?}

    //? if >=26.1 {
    private void renderJobSiteOverlay(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
    //?} else if >=1.20 {
    /*private void renderJobSiteOverlay(GuiGraphics graphics, float tickDelta) {
    *///?} else {
    /*private void renderJobSiteOverlay(PoseStack graphics, float tickDelta) {
    *///?}
        if (!isEnabled()) return;

        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) return;
        if (McClient.isHudHidden()) return;
        if (McClient.screen() != null) return;

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

        Gfx gfx = new Gfx(graphics);
        int screenWidth = gfx.guiWidth();
        int screenHeight = gfx.guiHeight();

        // ホットバー上部に配置（ホットバー本体: 22px + HP/満腹度バー: 約10px + 余白）
        int x = (screenWidth - textWidth) / 2;
        int y = screenHeight - 59;

        gfx.fill(x - 4, y - 2, x + textWidth + 4, y + textRenderer.lineHeight + 2, 0x80000000);
        gfx.text(textRenderer, text, x, y, 0xFFFFFFFF);
    }
}

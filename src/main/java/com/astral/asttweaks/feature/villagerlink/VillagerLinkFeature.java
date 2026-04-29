package com.astral.asttweaks.feature.villagerlink;

import com.astral.asttweaks.ASTTweaks;
import com.astral.asttweaks.feature.Feature;
import com.astral.asttweaks.mixin.VillageDebugRendererAccessor;
import com.astral.asttweaks.mixin.VillageDebugRendererBrainAccessor;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.debug.VillageDebugRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.village.VillagerProfession;
import net.minecraft.world.poi.PointOfInterestType;
import net.minecraft.world.poi.PointOfInterestTypes;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Visualizes the relationship between villagers and their job site blocks
 * by drawing lines between matched pairs.
 *
 * Primary source: vanilla {@code VillageDebugRenderer} brain debug data,
 * which the server broadcasts via {@code DebugInfoSender.sendBrainDebugData}.
 * This is authoritative — each Brain carries the actual claimed POI set.
 *
 * Fallback: when the server doesn't broadcast brain debug packets (some
 * server software filters them), the original heuristic kicks in — we
 * scan a small radius around the villager and pick the nearest POI block
 * matching the profession's workstation predicate.
 */
public class VillagerLinkFeature implements Feature {

    private static final int BLOCK_SEARCH_RADIUS = 8;
    private static final int UPDATE_INTERVAL_TICKS = 20;

    private final VillagerLinkConfig config;
    private final Map<VillagerEntity, BlockPos> cachedLinks = new HashMap<>();

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
        WorldRenderEvents.AFTER_TRANSLUCENT.register(this::renderLinks);
        HudRenderCallback.EVENT.register(this::renderJobSiteOverlay);
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

        MinecraftClient client = MinecraftClient.getInstance();
        ClientWorld world = client.world;
        if (world == null || client.player == null) {
            return;
        }

        int range = Math.max(1, config.getRange());
        Box searchBox = new Box(client.player.getPos(), client.player.getPos()).expand(range);
        List<VillagerEntity> villagers = world.getEntitiesByClass(VillagerEntity.class, searchBox, e -> !e.isRemoved());

        Map<UUID, VillageDebugRenderer.Brain> brains = getDebugBrains(client);

        for (VillagerEntity villager : villagers) {
            VillagerProfession profession = villager.getVillagerData().getProfession();
            if (profession == VillagerProfession.NONE || profession == VillagerProfession.NITWIT) {
                continue;
            }

            Predicate<RegistryEntry<PointOfInterestType>> predicate = profession.heldWorkstation();
            if (predicate == null) {
                continue;
            }

            BlockPos jobSite = findJobSiteFromDebugData(brains, villager, world, predicate);
            if (jobSite == null) {
                jobSite = findClosestJobSite(world, villager.getBlockPos(), predicate);
            }
            if (jobSite != null) {
                cachedLinks.put(villager, jobSite);
            }
        }
    }

    private Map<UUID, VillageDebugRenderer.Brain> getDebugBrains(MinecraftClient client) {
        if (client.debugRenderer == null || client.debugRenderer.villageDebugRenderer == null) {
            return null;
        }
        return ((VillageDebugRendererAccessor) client.debugRenderer.villageDebugRenderer).getBrains();
    }

    private BlockPos findJobSiteFromDebugData(Map<UUID, VillageDebugRenderer.Brain> brains, VillagerEntity villager,
                                               ClientWorld world, Predicate<RegistryEntry<PointOfInterestType>> predicate) {
        if (brains == null || brains.isEmpty()) {
            return null;
        }
        VillageDebugRenderer.Brain brain = brains.get(villager.getUuid());
        if (brain == null) {
            return null;
        }
        Set<BlockPos> claimed = ((VillageDebugRendererBrainAccessor) brain).getPointsOfInterest();
        if (claimed == null || claimed.isEmpty()) {
            return null;
        }
        for (BlockPos pos : claimed) {
            BlockState state = world.getBlockState(pos);
            if (state.isAir()) continue;
            Optional<RegistryEntry<PointOfInterestType>> poi = PointOfInterestTypes.getTypeForState(state);
            if (poi.isEmpty()) continue;
            if (predicate.test(poi.get())) {
                return pos.toImmutable();
            }
        }
        return null;
    }

    private BlockPos findClosestJobSite(ClientWorld world, BlockPos origin, Predicate<RegistryEntry<PointOfInterestType>> predicate) {
        BlockPos closest = null;
        double closestDistSq = Double.MAX_VALUE;
        BlockPos.Mutable cursor = new BlockPos.Mutable();

        int r = BLOCK_SEARCH_RADIUS;
        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    cursor.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
                    BlockState state = world.getBlockState(cursor);
                    if (state.isAir()) continue;

                    Optional<RegistryEntry<PointOfInterestType>> poi = PointOfInterestTypes.getTypeForState(state);
                    if (poi.isEmpty()) continue;
                    if (!predicate.test(poi.get())) continue;

                    double distSq = origin.getSquaredDistance(cursor);
                    if (distSq < closestDistSq) {
                        closestDistSq = distSq;
                        closest = cursor.toImmutable();
                    }
                }
            }
        }
        return closest;
    }

    private void renderLinks(WorldRenderContext context) {
        if (!isEnabled() || cachedLinks.isEmpty()) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) {
            return;
        }

        Camera camera = context.camera();
        Vec3d camPos = camera.getPos();
        float tickDelta = context.tickDelta();

        int color = config.getLineColor();
        float a = ((color >> 24) & 0xFF) / 255f;
        if (a <= 0f) a = 1.0f;
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;

        boolean seeThrough = config.isSeeThrough();

        MatrixStack matrices = context.matrixStack();
        matrices.push();
        matrices.translate(-camPos.x, -camPos.y, -camPos.z);

        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.lineWidth(2.0f);
        if (seeThrough) {
            RenderSystem.disableDepthTest();
        }
        RenderSystem.setShader(GameRenderer::getRenderTypeLinesProgram);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(VertexFormat.DrawMode.LINES, VertexFormats.LINES);

        Matrix4f positionMatrix = matrices.peek().getPositionMatrix();
        Matrix3f normalMatrix = matrices.peek().getNormalMatrix();

        for (Map.Entry<VillagerEntity, BlockPos> entry : cachedLinks.entrySet()) {
            VillagerEntity villager = entry.getKey();
            if (villager.isRemoved()) continue;

            double vx = MathHelper.lerp(tickDelta, villager.prevX, villager.getX());
            double vy = MathHelper.lerp(tickDelta, villager.prevY, villager.getY()) + villager.getHeight() * 0.5;
            double vz = MathHelper.lerp(tickDelta, villager.prevZ, villager.getZ());

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
                    .next();
            buffer.vertex(positionMatrix, (float) bx, (float) by, (float) bz)
                    .color(r, g, b, a)
                    .normal(normalMatrix, dx, dy, dz)
                    .next();
        }

        tessellator.draw();

        if (seeThrough) {
            RenderSystem.enableDepthTest();
        }
        RenderSystem.disableBlend();
        RenderSystem.enableCull();
        RenderSystem.lineWidth(1.0f);

        matrices.pop();
    }

    private void renderJobSiteOverlay(MatrixStack matrices, float tickDelta) {
        if (!isEnabled()) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;
        if (client.options.hudHidden) return;
        if (client.currentScreen != null) return;

        HitResult target = client.crosshairTarget;
        if (target == null || target.getType() != HitResult.Type.ENTITY) return;
        Entity entity = ((EntityHitResult) target).getEntity();
        if (!(entity instanceof VillagerEntity villager)) return;

        BlockPos linked = cachedLinks.get(villager);
        if (linked == null) return;

        TextRenderer textRenderer = client.textRenderer;
        Text text = Text.translatable(
                "hud." + ASTTweaks.MOD_ID + ".villagerlink.jobsite",
                linked.getX(), linked.getY(), linked.getZ());
        int textWidth = textRenderer.getWidth(text);

        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();

        // ホットバー上部に配置（ホットバー本体: 22px + HP/満腹度バー: 約10px + 余白）
        int x = (screenWidth - textWidth) / 2;
        int y = screenHeight - 59;

        DrawableHelper.fill(matrices, x - 4, y - 2, x + textWidth + 4, y + textRenderer.fontHeight + 2, 0x80000000);
        textRenderer.drawWithShadow(matrices, text, x, y, 0xFFFFFFFF);
    }
}

package com.astral.asttweaks.feature.portalprotect;

import com.astral.asttweaks.ASTTweaks;
import com.astral.asttweaks.feature.Feature;
import com.astral.asttweaks.util.McClient;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.NetherPortalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Prevents block placement at positions that would trigger a nether portal's
 * neighbor-update validity check, which is what destroys "sliced" portals.
 *
 * NetherPortalBlock.getStateForNeighborUpdate skips the validity check when the
 * neighbor change comes from the thin horizontal axis (perpendicular to the
 * portal plane), so placements in that direction are safe. We only cancel
 * placements on the dangerous sides: in-plane horizontal (same axis as portal)
 * and vertical. That lets rails / suppression blocks on the front and back of
 * a horizontal portal go through without any whitelist.
 */
public class PortalProtectFeature implements Feature {
    private final PortalProtectConfig config = new PortalProtectConfig();

    @Override
    public String getId() { return "portalprotect"; }

    @Override
    public String getName() { return "Portal Protect"; }

    @Override
    public void init() {
        UseBlockCallback.EVENT.register(this::onUseBlock);
        ASTTweaks.LOGGER.info("Portal Protect feature initialized");
    }

    @Override
    public void tick() {}

    @Override
    public boolean isEnabled() { return config.isEnabled(); }

    @Override
    public void setEnabled(boolean enabled) { config.setEnabled(enabled); }

    public PortalProtectConfig getConfig() { return config; }

    private InteractionResult onUseBlock(Player player, Level world, InteractionHand hand, BlockHitResult hit) {
        if (!world.isClientSide()) return InteractionResult.PASS;
        if (!config.isEnabled()) return InteractionResult.PASS;

        ItemStack stack = player.getItemInHand(hand);
        if (!(stack.getItem() instanceof BlockItem)) return InteractionResult.PASS;

        BlockPlaceContext ctx = new BlockPlaceContext(
                new UseOnContext(player, hand, hit));
        if (!ctx.canPlace()) return InteractionResult.PASS;

        BlockPos placePos = ctx.getClickedPos();

        // Defensive: replacing the portal block itself.
        BlockState placeState = world.getBlockState(placePos);
        if (placeState.getBlock() instanceof NetherPortalBlock) {
            notify(player);
            return InteractionResult.FAIL;
        }

        // Only cancel if the placement would change a neighbor on a side that
        // triggers the portal's validity check (in-plane horizontal or vertical).
        for (Direction d : Direction.values()) {
            BlockState neighborState = world.getBlockState(placePos.relative(d));
            if (!(neighborState.getBlock() instanceof NetherPortalBlock)) continue;

            Direction.Axis portalAxis = neighborState.getValue(BlockStateProperties.HORIZONTAL_AXIS);
            Direction.Axis placementAxis = d.getAxis();

            // Mirrors NetherPortalBlock.getStateForNeighborUpdate:
            //   axisMismatch = placementAxis != portalAxis && placementAxis.isHorizontal()
            // axisMismatch == true means vanilla skips the validity check, so we can pass.
            boolean safe = placementAxis != portalAxis && placementAxis.isHorizontal();
            if (!safe) {
                notify(player);
                return InteractionResult.FAIL;
            }
        }
        return InteractionResult.PASS;
    }

    private static void notify(Player player) {
        McClient.overlay(player, Component.translatable(
                "message." + ASTTweaks.MOD_ID + ".portalprotect.blocked"));
    }
}

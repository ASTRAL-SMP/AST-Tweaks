package com.astral.asttweaks.feature.portalprotect;

import com.astral.asttweaks.ASTTweaks;
import com.astral.asttweaks.feature.Feature;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.BlockState;
import net.minecraft.block.NetherPortalBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

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

    private ActionResult onUseBlock(PlayerEntity player, World world, Hand hand, BlockHitResult hit) {
        if (!world.isClient()) return ActionResult.PASS;
        if (!config.isEnabled()) return ActionResult.PASS;

        ItemStack stack = player.getStackInHand(hand);
        if (!(stack.getItem() instanceof BlockItem)) return ActionResult.PASS;

        ItemPlacementContext ctx = new ItemPlacementContext(
                new ItemUsageContext(player, hand, hit));
        if (!ctx.canPlace()) return ActionResult.PASS;

        BlockPos placePos = ctx.getBlockPos();

        // Defensive: replacing the portal block itself.
        BlockState placeState = world.getBlockState(placePos);
        if (placeState.getBlock() instanceof NetherPortalBlock) {
            notify(player);
            return ActionResult.FAIL;
        }

        // Only cancel if the placement would change a neighbor on a side that
        // triggers the portal's validity check (in-plane horizontal or vertical).
        for (Direction d : Direction.values()) {
            BlockState neighborState = world.getBlockState(placePos.offset(d));
            if (!(neighborState.getBlock() instanceof NetherPortalBlock)) continue;

            Direction.Axis portalAxis = neighborState.get(Properties.HORIZONTAL_AXIS);
            Direction.Axis placementAxis = d.getAxis();

            // Mirrors NetherPortalBlock.getStateForNeighborUpdate:
            //   axisMismatch = placementAxis != portalAxis && placementAxis.isHorizontal()
            // axisMismatch == true means vanilla skips the validity check, so we can pass.
            boolean safe = placementAxis != portalAxis && placementAxis.isHorizontal();
            if (!safe) {
                notify(player);
                return ActionResult.FAIL;
            }
        }
        return ActionResult.PASS;
    }

    private static void notify(PlayerEntity player) {
        player.sendMessage(Text.translatable(
                "message." + ASTTweaks.MOD_ID + ".portalprotect.blocked"), true);
    }
}

package com.astral.asttweaks.feature.portalprotect;

import com.astral.asttweaks.ASTTweaks;
import com.astral.asttweaks.feature.Feature;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.NetherPortalBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

/**
 * Prevents block placement at or directly adjacent to nether portal blocks so
 * that "sliced" portal decorations (portal blocks left exposed in a wall) are
 * not accidentally overwritten by misplaced building blocks.
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
        if (isPortal(world, placePos)) {
            notify(player);
            return ActionResult.FAIL;
        }
        for (Direction d : Direction.values()) {
            if (isPortal(world, placePos.offset(d))) {
                notify(player);
                return ActionResult.FAIL;
            }
        }
        return ActionResult.PASS;
    }

    private static boolean isPortal(World world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        Block block = state.getBlock();
        return block instanceof NetherPortalBlock;
    }

    private static void notify(PlayerEntity player) {
        player.sendMessage(Text.translatable(
                "message." + ASTTweaks.MOD_ID + ".portalprotect.blocked"), true);
    }
}

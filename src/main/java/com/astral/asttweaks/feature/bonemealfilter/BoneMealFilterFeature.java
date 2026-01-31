package com.astral.asttweaks.feature.bonemealfilter;

import com.astral.asttweaks.ASTTweaks;
import com.astral.asttweaks.feature.Feature;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.Block;
import net.minecraft.item.BoneMealItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;

/**
 * Bone meal filter feature that restricts bone meal usage to whitelisted blocks only.
 */
public class BoneMealFilterFeature implements Feature {
    private final BoneMealFilterConfig config;

    public BoneMealFilterFeature() {
        this.config = new BoneMealFilterConfig();
    }

    @Override
    public String getId() {
        return "bonemealfilter";
    }

    @Override
    public String getName() {
        return "Bone Meal Filter";
    }

    @Override
    public void init() {
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (!world.isClient()) return ActionResult.PASS;

            ItemStack stack = player.getStackInHand(hand);
            if (!(stack.getItem() instanceof BoneMealItem)) return ActionResult.PASS;

            if (!config.isEnabled()) return ActionResult.PASS;

            Block block = world.getBlockState(hitResult.getBlockPos()).getBlock();
            if (config.isWhitelisted(block)) return ActionResult.PASS;

            return ActionResult.FAIL;
        });
        ASTTweaks.LOGGER.info("BoneMealFilter feature initialized");
    }

    @Override
    public void tick() {
        // No tick processing needed
    }

    @Override
    public boolean isEnabled() {
        return config.isEnabled();
    }

    @Override
    public void setEnabled(boolean enabled) {
        config.setEnabled(enabled);
    }

    public BoneMealFilterConfig getConfig() {
        return config;
    }
}

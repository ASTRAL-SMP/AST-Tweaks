package com.astral.asttweaks.feature.bonemealfilter;

import com.astral.asttweaks.ASTTweaks;
import com.astral.asttweaks.feature.Feature;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

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
            if (!world.isClientSide()) return InteractionResult.PASS;

            ItemStack stack = player.getItemInHand(hand);
            if (!(stack.getItem() instanceof BoneMealItem)) return InteractionResult.PASS;

            if (!config.isEnabled()) return InteractionResult.PASS;

            Block block = world.getBlockState(hitResult.getBlockPos()).getBlock();
            if (config.isWhitelisted(block)) return InteractionResult.PASS;

            return InteractionResult.FAIL;
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

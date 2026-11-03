package com.astral.asttweaks.feature.silktouchswitch;

import com.astral.asttweaks.ASTTweaks;
import com.astral.asttweaks.compat.TweakerooCompat;
import com.astral.asttweaks.feature.Feature;
import com.astral.asttweaks.util.Enchants;
import com.astral.asttweaks.util.McClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;

/**
 * シルクタッチスイッチ機能。
 * Tweakeroo の toolSwitch が選択したツールを上書きし、
 * 指定ブロックに対してシルクタッチ付きツールを優先する。
 */
public class SilkTouchSwitchFeature implements Feature {
    private final SilkTouchSwitchConfig config;

    public SilkTouchSwitchFeature() {
        this.config = new SilkTouchSwitchConfig();
    }

    @Override
    public String getId() {
        return "silktouchswitch";
    }

    @Override
    public String getName() {
        return "Silk Touch Switch";
    }

    @Override
    public void init() {
        ASTTweaks.LOGGER.info("SilkTouchSwitch feature initialized");
    }

    @Override
    public void tick() {
        // ロジックは Mixin 経由のイベント駆動
    }

    @Override
    public boolean isEnabled() {
        return config.isEnabled();
    }

    @Override
    public void setEnabled(boolean enabled) {
        config.setEnabled(enabled);
    }

    public SilkTouchSwitchConfig getConfig() {
        return config;
    }

    /**
     * Mixin から呼ばれるメインロジック。
     * ブロックがリストに含まれる場合、シルクタッチ付きツールに切り替える。
     */
    public void onAttackBlock(BlockPos pos) {
        if (!config.isEnabled()) return;
        if (!TweakerooCompat.isToolSwitchEnabled()) return;

        Minecraft client = Minecraft.getInstance();
        if (client.level == null || client.player == null) return;

        Block block = client.level.getBlockState(pos).getBlock();
        if (!config.isInSilkTouchList(block)) return;

        int slot = findSilkTouchToolSlot(client.player);
        if (slot >= 0) {
            McClient.setSelectedSlot(client.player.getInventory(), slot);
        }
    }

    /**
     * ホットバー (0-8) からシルクタッチ付きツールを探す。
     */
    private int findSilkTouchToolSlot(LocalPlayer player) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && hasSilkTouch(stack)) {
                return i;
            }
        }
        return -1;
    }

    private boolean hasSilkTouch(ItemStack stack) {
        return Enchants.has(stack, Enchantments.SILK_TOUCH);
    }
}

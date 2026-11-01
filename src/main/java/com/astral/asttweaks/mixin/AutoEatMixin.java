package com.astral.asttweaks.mixin;

import com.astral.asttweaks.feature.FeatureManager;
import com.astral.asttweaks.feature.autoeat.AutoEatFeature;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin to prevent using blacklisted food items.
 */
@Mixin(Item.class)
public class AutoEatMixin {
    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void onUse(Level world, Player user, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        ItemStack stack = user.getItemInHand(hand);
        AutoEatFeature feature = FeatureManager.getInstance().getAutoEatFeature();

        if (feature != null && feature.getConfig().isBlacklisted(stack.getItem())) {
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }
}

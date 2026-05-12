package com.astral.asttweaks.mixin;

import com.astral.asttweaks.ASTTweaks;
import com.astral.asttweaks.feature.FeatureManager;
import com.astral.asttweaks.feature.pickprotect.PickProtectConfig;
import com.astral.asttweaks.feature.pickprotect.PickProtectFeature;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Reroutes the vanilla Pick Block action so it lands in a non-protected hotbar
 * slot whenever vanilla's swappable slot would be protected. If no non-protected
 * destination is reachable (e.g. the only empty hotbar slot is protected), the
 * action is cancelled with a HUD message.
 */
@Mixin(MinecraftClient.class)
public abstract class PickBlockMixin {
    private static final int HOTBAR_SIZE = 9;

    @Inject(method = "doItemPick", at = @At("HEAD"), cancellable = true)
    private void asttweaks$onDoItemPick(CallbackInfo ci) {
        PickProtectFeature feature = (PickProtectFeature) FeatureManager.getInstance().getFeature("pickprotect");
        if (feature == null || !feature.isEnabled()) return;

        PickProtectConfig cfg = feature.getConfig();
        if (cfg.getProtectedSlots().isEmpty()) return;

        MinecraftClient client = (MinecraftClient) (Object) this;
        ClientPlayerEntity player = client.player;
        World world = client.world;
        if (player == null || world == null) return;

        HitResult hit = client.crosshairTarget;
        if (hit == null || hit.getType() == HitResult.Type.MISS) return;

        boolean creative = player.getAbilities().creativeMode;
        ItemStack pickStack = resolvePickStack(hit, world, creative);
        if (pickStack == null || pickStack.isEmpty()) return;

        // Creative Ctrl-pick on a block-with-entity enriches the stack with the
        // BlockEntity NBT before vanilla looks it up, so the "already in hotbar"
        // shortcut based on the bare stack would be unsafe — skip it.
        boolean blockEntityPick = creative && Screen.hasControlDown()
                && hit.getType() == HitResult.Type.BLOCK
                && world.getBlockState(((BlockHitResult) hit).getBlockPos()).hasBlockEntity();

        PlayerInventory inv = player.getInventory();

        if (!blockEntityPick) {
            int existing = inv.getSlotWithStack(pickStack);
            // Already on the hotbar — vanilla just re-selects it, no overwrite.
            if (PlayerInventory.isValidHotbarIndex(existing)) return;
            // Survival mode with no matching item — vanilla does nothing.
            if (!creative && existing == -1) return;
        }

        int vanillaDest = inv.getSwappableHotbarSlot();
        if (!cfg.isSlotProtected(vanillaDest)) return;

        int redirect = findRedirectSlot(inv, cfg);
        if (redirect < 0) {
            ci.cancel();
            player.sendMessage(Text.translatable(
                    "message." + ASTTweaks.MOD_ID + ".pickprotect.blocked"), true);
            return;
        }

        // Set selectedSlot to the slot we want the pick to land in. Vanilla's
        // getSwappableHotbarSlot() starting from this slot will return the same
        // slot (it always prefers selectedSlot when the slot itself qualifies),
        // so both client and server end up agreeing on the destination.
        if (inv.selectedSlot != redirect) {
            inv.selectedSlot = redirect;
            // Sync immediately so the server uses the new selected slot when it
            // handles the upcoming PickFromInventory packet (survival path).
            player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(redirect));
        }
        // Let vanilla doItemPick proceed.
    }

    private static ItemStack resolvePickStack(HitResult hit, World world, boolean creative) {
        HitResult.Type type = hit.getType();
        if (type == HitResult.Type.BLOCK) {
            BlockPos pos = ((BlockHitResult) hit).getBlockPos();
            BlockState state = world.getBlockState(pos);
            if (state.isAir()) return ItemStack.EMPTY;
            return state.getBlock().getPickStack(world, pos, state);
        }
        if (type == HitResult.Type.ENTITY && creative) {
            Entity entity = ((EntityHitResult) hit).getEntity();
            ItemStack stack = entity.getPickBlockStack();
            return stack != null ? stack : ItemStack.EMPTY;
        }
        return ItemStack.EMPTY;
    }

    /**
     * Pick the hotbar slot the item should land in. We choose a slot such that
     * vanilla's {@code getSwappableHotbarSlot()} with selectedSlot set to that
     * slot would return the same slot — guaranteeing client and server agree.
     * Returns -1 if no non-protected destination is reachable.
     */
    private static int findRedirectSlot(PlayerInventory inv, PickProtectConfig cfg) {
        int origin = inv.selectedSlot;

        // Phase 1: if any hotbar slot is empty, vanilla's first pass will pick
        // the first empty slot in the cycle — we can only redirect to a
        // non-protected empty slot.
        boolean anyEmpty = false;
        for (int j = 0; j < HOTBAR_SIZE; j++) {
            if (inv.getStack(j).isEmpty()) { anyEmpty = true; break; }
        }
        if (anyEmpty) {
            for (int i = 0; i < HOTBAR_SIZE; i++) {
                int s = (origin + i) % HOTBAR_SIZE;
                if (cfg.isSlotProtected(s)) continue;
                if (inv.getStack(s).isEmpty()) return s;
            }
            return -1;
        }

        // Phase 2: no empty slots — vanilla goes to its non-enchanted pass.
        boolean anyNonEnchanted = false;
        for (int j = 0; j < HOTBAR_SIZE; j++) {
            if (!inv.getStack(j).hasEnchantments()) { anyNonEnchanted = true; break; }
        }
        if (anyNonEnchanted) {
            for (int i = 0; i < HOTBAR_SIZE; i++) {
                int s = (origin + i) % HOTBAR_SIZE;
                if (cfg.isSlotProtected(s)) continue;
                if (!inv.getStack(s).hasEnchantments()) return s;
            }
            return -1;
        }

        // Phase 3: every hotbar slot is filled with an enchanted item. Vanilla
        // falls back to selectedSlot itself — any non-protected slot is valid.
        for (int i = 0; i < HOTBAR_SIZE; i++) {
            int s = (origin + i) % HOTBAR_SIZE;
            if (!cfg.isSlotProtected(s)) return s;
        }
        return -1;
    }
}

package com.astral.asttweaks.mixin;

import com.astral.asttweaks.ASTTweaks;
import com.astral.asttweaks.feature.FeatureManager;
import com.astral.asttweaks.feature.pickprotect.PickProtectConfig;
import com.astral.asttweaks.feature.pickprotect.PickProtectFeature;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
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
@Mixin(Minecraft.class)
public abstract class PickBlockMixin {
    private static final int HOTBAR_SIZE = 9;

    @Inject(method = "pickBlockOrEntity", at = @At("HEAD"), cancellable = true)
    private void asttweaks$onPickBlockOrEntity(CallbackInfo ci) {
        PickProtectFeature feature = (PickProtectFeature) FeatureManager.getInstance().getFeature("pickprotect");
        if (feature == null || !feature.isEnabled()) return;

        PickProtectConfig cfg = feature.getConfig();
        if (cfg.getProtectedSlots().isEmpty()) return;

        Minecraft client = (Minecraft) (Object) this;
        LocalPlayer player = client.player;
        Level world = client.level;
        if (player == null || world == null) return;

        HitResult hit = client.hitResult;
        if (hit == null || hit.getType() == HitResult.Type.MISS) return;

        // Mirrors ServerGamePacketListenerImpl#handlePickItemFromBlock / #tryPickItem,
        // which decide the destination slot on the server.
        boolean creative = player.hasInfiniteMaterials();
        boolean includeData = creative && client.hasControlDown();
        ItemStack pickStack = resolvePickStack(hit, world, includeData);
        if (pickStack == null || pickStack.isEmpty()) return;

        // Creative Ctrl-pick on a block-with-entity enriches the stack with the
        // BlockEntity data before the server looks it up, so the "already in hotbar"
        // shortcut based on the bare stack would be unsafe — skip it.
        boolean blockEntityPick = includeData
                && hit.getType() == HitResult.Type.BLOCK
                && world.getBlockState(((BlockHitResult) hit).getBlockPos()).hasBlockEntity();

        Inventory inv = player.getInventory();

        if (!blockEntityPick) {
            int existing = inv.findSlotMatchingItem(pickStack);
            // Already on the hotbar — the server just re-selects it, no overwrite.
            if (Inventory.isHotbarSlot(existing)) return;
            // Survival mode with no matching item — the server does nothing.
            if (!creative && existing == -1) return;
        }

        int vanillaDest = inv.getSuitableHotbarSlot();
        if (!cfg.isSlotProtected(vanillaDest)) return;

        int redirect = findRedirectSlot(inv, cfg);
        if (redirect < 0) {
            ci.cancel();
            player.sendOverlayMessage(Component.translatable(
                    "message." + ASTTweaks.MOD_ID + ".pickprotect.blocked"));
            return;
        }

        // Set selectedSlot to the slot we want the pick to land in. Vanilla's
        // getSuitableHotbarSlot() starting from this slot will return the same
        // slot (it always prefers selectedSlot when the slot itself qualifies),
        // so both client and server end up agreeing on the destination.
        if (inv.getSelectedSlot() != redirect) {
            inv.setSelectedSlot(redirect);
            // The pick packet does not sync the carried item first, so sync it
            // here before the server handles the upcoming PickItemFromBlock/Entity packet.
            player.connection.send(new ServerboundSetCarriedItemPacket(redirect));
        }
        // Let vanilla pickBlockOrEntity proceed.
    }

    private static ItemStack resolvePickStack(HitResult hit, Level world, boolean includeData) {
        HitResult.Type type = hit.getType();
        if (type == HitResult.Type.BLOCK) {
            BlockPos pos = ((BlockHitResult) hit).getBlockPos();
            BlockState state = world.getBlockState(pos);
            if (state.isAir()) return ItemStack.EMPTY;
            return state.getCloneItemStack(world, pos, includeData);
        }
        if (type == HitResult.Type.ENTITY) {
            Entity entity = ((EntityHitResult) hit).getEntity();
            ItemStack stack = entity.getPickResult();
            return stack != null ? stack : ItemStack.EMPTY;
        }
        return ItemStack.EMPTY;
    }

    /**
     * Pick the hotbar slot the item should land in. We choose a slot such that
     * vanilla's {@code getSuitableHotbarSlot()} with selectedSlot set to that
     * slot would return the same slot — guaranteeing client and server agree.
     * Returns -1 if no non-protected destination is reachable.
     */
    private static int findRedirectSlot(Inventory inv, PickProtectConfig cfg) {
        int origin = inv.getSelectedSlot();

        // Phase 1: if any hotbar slot is empty, vanilla's first pass will pick
        // the first empty slot in the cycle — we can only redirect to a
        // non-protected empty slot.
        boolean anyEmpty = false;
        for (int j = 0; j < HOTBAR_SIZE; j++) {
            if (inv.getItem(j).isEmpty()) { anyEmpty = true; break; }
        }
        if (anyEmpty) {
            for (int i = 0; i < HOTBAR_SIZE; i++) {
                int s = (origin + i) % HOTBAR_SIZE;
                if (cfg.isSlotProtected(s)) continue;
                if (inv.getItem(s).isEmpty()) return s;
            }
            return -1;
        }

        // Phase 2: no empty slots — vanilla goes to its non-enchanted pass.
        boolean anyNonEnchanted = false;
        for (int j = 0; j < HOTBAR_SIZE; j++) {
            if (!inv.getItem(j).isEnchanted()) { anyNonEnchanted = true; break; }
        }
        if (anyNonEnchanted) {
            for (int i = 0; i < HOTBAR_SIZE; i++) {
                int s = (origin + i) % HOTBAR_SIZE;
                if (cfg.isSlotProtected(s)) continue;
                if (!inv.getItem(s).isEnchanted()) return s;
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

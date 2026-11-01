package com.astral.asttweaks.mixin;

import com.astral.asttweaks.config.ModConfig;
import com.astral.asttweaks.feature.FeatureManager;
import com.astral.asttweaks.feature.inventorysort.InventorySortFeature;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractContainerScreen.class)
public abstract class HandledScreenMixin<T extends AbstractContainerMenu> extends Screen {
    @Shadow protected int leftPos;
    @Shadow protected int topPos;
    @Shadow @Final protected int imageWidth;
    @Shadow @Final protected int imageHeight;
    @Shadow @Final protected T menu;

    @Unique private static final int SIZE = 9;

    protected HandledScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void astTweaks$render(GuiGraphicsExtractor m, int mx, int my, float d, CallbackInfo ci) {
        ModConfig cfg = ModConfig.getInstance();
        if (!cfg.inventorySortEnabled || !cfg.inventorySortShowButton) return;

        InventorySortFeature f = FeatureManager.getInstance().getInventorySortFeature();
        if (f == null) return;

        boolean hasC = hasExternalContainer();

        // Player button
        int px = leftPos + imageWidth - SIZE - 7;
        int py = hasC ? topPos + imageHeight - 94 : topPos + 4;
        boolean hp = mx >= px && mx < px + SIZE && my >= py && my < py + SIZE;
        drawBtn(m, px, py, hp);
        if (hp) m.setTooltipForNextFrame(this.font, Component.translatable("gui.asttweaks.inventorysort.player"), mx, my);

        // Container button
        if (hasC) {
            int cx = leftPos + imageWidth - SIZE - 7;
            int cy = topPos + 4;
            boolean hc = mx >= cx && mx < cx + SIZE && my >= cy && my < cy + SIZE;
            drawBtn(m, cx, cy, hc);
            if (hc) m.setTooltipForNextFrame(this.font, Component.translatable("gui.asttweaks.inventorysort.container"), mx, my);
        }
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void astTweaks$click(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
        if (event.button() != 0) return;
        double mx = event.x();
        double my = event.y();

        ModConfig cfg = ModConfig.getInstance();
        if (!cfg.inventorySortEnabled || !cfg.inventorySortShowButton) return;

        InventorySortFeature f = FeatureManager.getInstance().getInventorySortFeature();
        if (f == null) return;

        boolean hasC = hasExternalContainer();

        int px = leftPos + imageWidth - SIZE - 7;
        int py = hasC ? topPos + imageHeight - 94 : topPos + 4;

        if (mx >= px && mx < px + SIZE && my >= py && my < py + SIZE) {
            f.performPlayerSort();
            cir.setReturnValue(true);
            return;
        }

        if (hasC) {
            int cx = leftPos + imageWidth - SIZE - 7;
            int cy = topPos + 4;
            if (mx >= cx && mx < cx + SIZE && my >= cy && my < cy + SIZE) {
                f.performContainerSort();
                cir.setReturnValue(true);
            }
        }
    }

    /**
     * Check if the current screen has an external container (chest, shulker, etc.)
     * Returns false for player inventory screen.
     */
    @Unique
    private boolean hasExternalContainer() {
        // PlayerScreenHandler is the player's own inventory screen (E key)
        if (menu instanceof InventoryMenu) {
            return false;
        }
        // For other screens, check if slot 0 is NOT a PlayerInventory slot
        if (!menu.slots.isEmpty()) {
            Slot firstSlot = menu.getSlot(0);
            return !(firstSlot.container instanceof Inventory);
        }
        return false;
    }

    @Unique
    private void drawBtn(GuiGraphicsExtractor m, int x, int y, boolean hover) {
        int bg = hover ? 0xFFAAAAAA : 0xFF8B8B8B;
        int light = hover ? 0xFFFFFFFF : 0xFFCCCCCC;
        int dark = 0xFF373737;
        int icon = 0xFF3F3F3F;

        // Background
        m.fill(x, y, x + SIZE, y + SIZE, bg);

        // 3D borders
        m.fill(x, y, x + SIZE - 1, y + 1, light);
        m.fill(x, y, x + 1, y + SIZE - 1, light);
        m.fill(x + 1, y + SIZE - 1, x + SIZE, y + SIZE, dark);
        m.fill(x + SIZE - 1, y + 1, x + SIZE, y + SIZE, dark);

        // Sort icon (3 lines)
        int cx = x + SIZE / 2;
        m.fill(cx - 3, y + 2, cx + 3, y + 3, icon);
        m.fill(cx - 2, y + 4, cx + 2, y + 5, icon);
        m.fill(cx - 1, y + 6, cx + 1, y + 7, icon);
    }
}

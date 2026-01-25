package com.astral.asttweaks.mixin;

import com.astral.asttweaks.config.ModConfig;
import com.astral.asttweaks.feature.FeatureManager;
import com.astral.asttweaks.feature.inventorysort.InventorySortFeature;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin<T extends ScreenHandler> extends Screen {
    @Shadow protected int x;
    @Shadow protected int y;
    @Shadow protected int backgroundWidth;
    @Shadow protected int backgroundHeight;
    @Shadow @Final protected T handler;

    @Unique private static final int SIZE = 9;

    protected HandledScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void astTweaks$render(MatrixStack m, int mx, int my, float d, CallbackInfo ci) {
        ModConfig cfg = ModConfig.getInstance();
        if (!cfg.inventorySortEnabled || !cfg.inventorySortShowButton) return;

        InventorySortFeature f = FeatureManager.getInstance().getInventorySortFeature();
        if (f == null) return;

        boolean hasC = hasExternalContainer();

        // Player button
        int px = x + backgroundWidth - SIZE - 7;
        int py = hasC ? y + backgroundHeight - 94 : y + 4;
        boolean hp = mx >= px && mx < px + SIZE && my >= py && my < py + SIZE;
        drawBtn(m, px, py, hp);
        if (hp) renderTooltip(m, Text.translatable("gui.asttweaks.inventorysort.player"), mx, my);

        // Container button
        if (hasC) {
            int cx = x + backgroundWidth - SIZE - 7;
            int cy = y + 4;
            boolean hc = mx >= cx && mx < cx + SIZE && my >= cy && my < cy + SIZE;
            drawBtn(m, cx, cy, hc);
            if (hc) renderTooltip(m, Text.translatable("gui.asttweaks.inventorysort.container"), mx, my);
        }
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void astTweaks$click(double mx, double my, int btn, CallbackInfoReturnable<Boolean> cir) {
        if (btn != 0) return;

        ModConfig cfg = ModConfig.getInstance();
        if (!cfg.inventorySortEnabled || !cfg.inventorySortShowButton) return;

        InventorySortFeature f = FeatureManager.getInstance().getInventorySortFeature();
        if (f == null) return;

        boolean hasC = hasExternalContainer();

        int px = x + backgroundWidth - SIZE - 7;
        int py = hasC ? y + backgroundHeight - 94 : y + 4;

        if (mx >= px && mx < px + SIZE && my >= py && my < py + SIZE) {
            f.performPlayerSort();
            cir.setReturnValue(true);
            return;
        }

        if (hasC) {
            int cx = x + backgroundWidth - SIZE - 7;
            int cy = y + 4;
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
        if (handler instanceof PlayerScreenHandler) {
            return false;
        }
        // For other screens, check if slot 0 is NOT a PlayerInventory slot
        if (!handler.slots.isEmpty()) {
            Slot firstSlot = handler.getSlot(0);
            return !(firstSlot.inventory instanceof PlayerInventory);
        }
        return false;
    }

    @Unique
    private void drawBtn(MatrixStack m, int x, int y, boolean hover) {
        int bg = hover ? 0xFFAAAAAA : 0xFF8B8B8B;
        int light = hover ? 0xFFFFFFFF : 0xFFCCCCCC;
        int dark = 0xFF373737;
        int icon = 0xFF3F3F3F;

        // Background
        DrawableHelper.fill(m, x, y, x + SIZE, y + SIZE, bg);

        // 3D borders
        DrawableHelper.fill(m, x, y, x + SIZE - 1, y + 1, light);
        DrawableHelper.fill(m, x, y, x + 1, y + SIZE - 1, light);
        DrawableHelper.fill(m, x + 1, y + SIZE - 1, x + SIZE, y + SIZE, dark);
        DrawableHelper.fill(m, x + SIZE - 1, y + 1, x + SIZE, y + SIZE, dark);

        // Sort icon (3 lines)
        int cx = x + SIZE / 2;
        DrawableHelper.fill(m, cx - 3, y + 2, cx + 3, y + 3, icon);
        DrawableHelper.fill(m, cx - 2, y + 4, cx + 2, y + 5, icon);
        DrawableHelper.fill(m, cx - 1, y + 6, cx + 1, y + 7, icon);
    }
}

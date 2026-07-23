package com.astral.asttweaks.mixin.litematica;

import fi.dy.masa.malilib.gui.widgets.WidgetListBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Exposes malilib's protected list widget getter. Declaring it here rather than
 * as a {@code @Shadow} on the browser screen keeps the target lookup on the
 * class that actually declares the method.
 */
@Mixin(targets = "fi.dy.masa.malilib.gui.GuiListBase", remap = false)
public interface GuiListBaseAccessor {
    @Invoker("getListWidget")
    WidgetListBase<?, ?> asttweaks$getListWidget();
}

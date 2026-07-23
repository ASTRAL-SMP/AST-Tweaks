package com.astral.asttweaks.mixin.litematica;

import com.astral.asttweaks.compat.LitematicaDropCompat;
import com.astral.asttweaks.config.ModConfig;
import fi.dy.masa.malilib.gui.widgets.WidgetFileBrowserBase;
import fi.dy.masa.malilib.gui.widgets.WidgetListBase;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

/**
 * Handles schematic files dropped onto any Litematica schematic browser screen
 * (load / manage / save), importing them into the folder currently open in the
 * browser instead of the schematics root.
 */
@Mixin(targets = "fi.dy.masa.litematica.gui.GuiSchematicBrowserBase", remap = false)
public abstract class GuiSchematicBrowserBaseMixin extends Screen {
    protected GuiSchematicBrowserBaseMixin(Text title) {
        super(title);
    }

    @Shadow
    public abstract String getBrowserContext();

    @Shadow
    public abstract File getDefaultDirectory();

    @Override
    public void filesDragged(List<Path> paths) {
        if (!ModConfig.getInstance().litematicaSchematicDropEnabled) {
            super.filesDragged(paths);
            return;
        }

        WidgetListBase<?, ?> listWidget = ((GuiListBaseAccessor) this).asttweaks$getListWidget();
        WidgetFileBrowserBase browser = listWidget instanceof WidgetFileBrowserBase fileBrowser
                ? fileBrowser
                : null;

        File targetDirectory = LitematicaDropCompat.resolveBrowserDirectory(
                browser != null ? browser.getCurrentDirectory() : null,
                this.getBrowserContext(),
                this.getDefaultDirectory());

        LitematicaDropCompat.handleDroppedFiles(paths, targetDirectory);

        if (browser != null) {
            // Reload the listing so the freshly imported files appear.
            browser.refreshEntries();
        }
    }
}

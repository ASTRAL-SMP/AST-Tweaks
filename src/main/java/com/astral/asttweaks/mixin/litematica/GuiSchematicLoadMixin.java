package com.astral.asttweaks.mixin.litematica;

import com.astral.asttweaks.compat.LitematicaDropCompat;
import com.astral.asttweaks.config.ModConfig;
import fi.dy.masa.litematica.gui.widgets.WidgetSchematicBrowser;
import fi.dy.masa.malilib.gui.widgets.WidgetListBase;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

@Mixin(targets = "fi.dy.masa.litematica.gui.GuiSchematicLoad", remap = false)
public abstract class GuiSchematicLoadMixin extends Screen {
    protected GuiSchematicLoadMixin(Text title) {
        super(title);
    }

    @Shadow
    protected abstract WidgetListBase<?, ?> getListWidget();

    @Override
    public void filesDragged(List<Path> paths) {
        if (ModConfig.getInstance().litematicaSchematicDropEnabled) {
            // Import into the folder the browser is currently showing, so drops
            // land in the sub-directory the user is looking at instead of root.
            WidgetListBase<?, ?> listWidget = this.getListWidget();
            WidgetSchematicBrowser browser = listWidget instanceof WidgetSchematicBrowser
                    ? (WidgetSchematicBrowser) listWidget
                    : null;
            File targetDirectory = browser != null ? browser.getCurrentDirectory() : null;

            LitematicaDropCompat.handleDroppedFiles(paths, targetDirectory);

            if (browser != null) {
                // Reload the listing so the freshly imported files appear.
                browser.refreshEntries();
            }
        } else {
            super.filesDragged(paths);
        }
    }
}

package com.astral.asttweaks.mixin.litematica;

import com.astral.asttweaks.compat.LitematicaDropCompat;
import com.astral.asttweaks.config.ModConfig;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;

import java.nio.file.Path;
import java.util.List;

@Mixin(targets = "fi.dy.masa.litematica.gui.GuiSchematicLoad", remap = false)
public abstract class GuiSchematicLoadMixin extends Screen {
    protected GuiSchematicLoadMixin(Text title) {
        super(title);
    }

    @Override
    public void filesDragged(List<Path> paths) {
        if (ModConfig.getInstance().litematicaSchematicDropEnabled) {
            LitematicaDropCompat.handleDroppedFiles(paths);
        } else {
            super.filesDragged(paths);
        }
    }
}

package com.astral.asttweaks.feature.notepad;

import com.astral.asttweaks.ASTTweaks;
import com.astral.asttweaks.feature.Feature;
import com.astral.asttweaks.util.McClient;
import net.minecraft.client.Minecraft;

/**
 * Notepad feature that allows players to write and save notes.
 */
public class NotepadFeature implements Feature {
    private final NotepadConfig config;
    private final NotepadStorage storage;

    public NotepadFeature() {
        this.config = new NotepadConfig();
        this.storage = new NotepadStorage();
    }

    @Override
    public String getId() {
        return "notepad";
    }

    @Override
    public String getName() {
        return "Notepad";
    }

    @Override
    public void init() {
        ASTTweaks.LOGGER.info("Notepad feature initialized");
    }

    @Override
    public void tick() {
        // No tick processing needed for notepad
    }

    @Override
    public boolean isEnabled() {
        return config.isEnabled();
    }

    @Override
    public void setEnabled(boolean enabled) {
        config.setEnabled(enabled);
    }

    public NotepadConfig getConfig() {
        return config;
    }

    public NotepadStorage getStorage() {
        return storage;
    }

    /**
     * Open the notepad screen.
     */
    public void openNotepad() {
        if (!isEnabled()) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        if (client != null && client.player != null) {
            McClient.setScreen(new NotepadScreen(storage));
        }
    }
}

package com.astral.asttweaks.feature.notepad;

import com.astral.asttweaks.ASTTweaks;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Handles text persistence for the notepad feature.
 * Saves and loads plain text content from config directory.
 */
public class NotepadStorage {
    private static final Path NOTEPAD_PATH = FabricLoader.getInstance()
            .getConfigDir().resolve(ASTTweaks.MOD_ID + "_notepad.txt");

    private String content = "";

    public NotepadStorage() {
        load();
    }

    /**
     * Load notepad content from file.
     */
    public void load() {
        if (Files.exists(NOTEPAD_PATH)) {
            try {
                content = Files.readString(NOTEPAD_PATH);
                ASTTweaks.LOGGER.info("Notepad content loaded from {}", NOTEPAD_PATH);
            } catch (IOException e) {
                ASTTweaks.LOGGER.error("Failed to load notepad content", e);
                content = "";
            }
        } else {
            content = "";
        }
    }

    /**
     * Save notepad content to file.
     */
    public void save() {
        try {
            Files.createDirectories(NOTEPAD_PATH.getParent());
            Files.writeString(NOTEPAD_PATH, content);
            ASTTweaks.LOGGER.info("Notepad content saved to {}", NOTEPAD_PATH);
        } catch (IOException e) {
            ASTTweaks.LOGGER.error("Failed to save notepad content", e);
        }
    }

    /**
     * Get the current notepad content.
     */
    public String getContent() {
        return content;
    }

    /**
     * Set the notepad content.
     */
    public void setContent(String content) {
        this.content = content != null ? content : "";
    }
}

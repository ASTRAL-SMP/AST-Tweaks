package com.astral.asttweaks.config;

import com.astral.asttweaks.ASTTweaks;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

/**
 * Main configuration manager for AST-Tweaks.
 */
public class ModConfig {
    private static ModConfig instance;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance()
            .getConfigDir().resolve(ASTTweaks.MOD_ID + ".json");

    // Scoreboard settings
    public boolean scoreboardEnabled = true;
    public boolean scoreboardPagingEnabled = true;
    public int scoreboardMaxLines = 15;
    public int scoreboardPageSize = 10;

    // Scoreboard position (percentage: 0-100)
    public int scoreboardPositionX = 100;  // 100 = right edge (default)
    public int scoreboardPositionY = 50;   // 50 = center

    // Scoreboard scale
    public float scoreboardScale = 1.0f;

    // Scoreboard colors (ARGB format)
    public int scoreboardHeaderColor = 0x66000000;  // Semi-transparent black
    public int scoreboardBodyColor = 0x4D000000;    // More transparent black
    public int scoreboardTextColor = 0xFFFFFFFF;    // White

    // Rank display
    public boolean scoreboardShowRank = false;

    // Auto-eat settings
    public boolean autoEatEnabled = true;
    public int autoEatHungerThreshold = 6;
    public boolean autoEatWhileAction = false;
    public Set<String> autoEatBlacklist = new HashSet<>();

    private ModConfig() {
        // Default blacklist items
        autoEatBlacklist.add("minecraft:rotten_flesh");
        autoEatBlacklist.add("minecraft:spider_eye");
        autoEatBlacklist.add("minecraft:poisonous_potato");
        autoEatBlacklist.add("minecraft:pufferfish");
    }

    public static ModConfig getInstance() {
        if (instance == null) {
            instance = new ModConfig();
        }
        return instance;
    }

    /**
     * Load configuration from file.
     */
    public void load() {
        if (Files.exists(CONFIG_PATH)) {
            try {
                String json = Files.readString(CONFIG_PATH);
                ModConfig loaded = GSON.fromJson(json, ModConfig.class);
                if (loaded != null) {
                    this.scoreboardEnabled = loaded.scoreboardEnabled;
                    this.scoreboardPagingEnabled = loaded.scoreboardPagingEnabled;
                    this.scoreboardMaxLines = loaded.scoreboardMaxLines;
                    this.scoreboardPageSize = loaded.scoreboardPageSize;
                    this.scoreboardPositionX = loaded.scoreboardPositionX;
                    this.scoreboardPositionY = loaded.scoreboardPositionY;
                    this.scoreboardScale = loaded.scoreboardScale;
                    this.scoreboardHeaderColor = loaded.scoreboardHeaderColor;
                    this.scoreboardBodyColor = loaded.scoreboardBodyColor;
                    this.scoreboardTextColor = loaded.scoreboardTextColor;
                    this.scoreboardShowRank = loaded.scoreboardShowRank;
                    this.autoEatEnabled = loaded.autoEatEnabled;
                    this.autoEatHungerThreshold = loaded.autoEatHungerThreshold;
                    this.autoEatWhileAction = loaded.autoEatWhileAction;
                    if (loaded.autoEatBlacklist != null) {
                        this.autoEatBlacklist = new HashSet<>(loaded.autoEatBlacklist);
                    }
                }
                ASTTweaks.LOGGER.info("Configuration loaded from {}", CONFIG_PATH);
            } catch (IOException e) {
                ASTTweaks.LOGGER.error("Failed to load configuration", e);
            }
        } else {
            save();
        }
    }

    /**
     * Save configuration to file.
     */
    public void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            Files.writeString(CONFIG_PATH, GSON.toJson(this));
            ASTTweaks.LOGGER.info("Configuration saved to {}", CONFIG_PATH);
        } catch (IOException e) {
            ASTTweaks.LOGGER.error("Failed to save configuration", e);
        }
    }
}

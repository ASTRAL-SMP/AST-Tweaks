package com.astral.asttweaks.config;

import com.astral.asttweaks.ASTTweaks;
import com.astral.asttweaks.feature.automove.MoveDirection;
import com.astral.asttweaks.feature.updatechecker.CheckFrequency;
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

    // Auto-move settings
    public boolean autoMoveEnabled = true;
    public MoveDirection autoMoveDirection = MoveDirection.FORWARD;

    // Entity culling settings
    public boolean entityCullingEnabled = true;
    public boolean disableAllEntityRendering = false;
    public boolean disableArmorStandRendering = false;
    public boolean disableFallingBlockRendering = false;
    public boolean disableDeadMobRendering = false;
    public int itemRenderLimit = -1;  // -1 = unlimited
    public int xpOrbRenderLimit = -1; // -1 = unlimited
    public Set<String> entityBlacklist = new HashSet<>();
    public Set<String> itemEntityBlacklist = new HashSet<>();  // アイテム種類ごとのブラックリスト

    // Lava highlight settings
    public boolean lavaHighlightEnabled = false;
    public boolean lavaHighlightSource = true;
    public boolean lavaHighlightFlowing = false;
    public int lavaSourceColor = 0x8000FF00;      // Semi-transparent green
    public int lavaFlowingColor = 0x80FF0000;     // Semi-transparent red

    // Notepad settings
    public boolean notepadEnabled = true;

    // Auto totem settings
    public boolean autoTotemEnabled = true;

    // Auto repair settings (Fast repair mode - Tweakeroo style)
    public boolean autoRepairEnabled = true;
    public int autoRepairClicksPerTick = 10;          // Number of clicks per tick (fast use)
    public boolean autoRepairWhitelistMode = false;   // false = blacklist mode
    public Set<String> autoRepairItemList = new HashSet<>();
    public int autoRepairTargetSlot = 0;              // Hotbar slot to use for repairing items (0-8)

    // Mass grindstone settings
    public boolean massGrindstoneEnabled = true;
    public boolean massGrindstoneWhitelistMode = true;  // Default to whitelist for SMP safety
    public Set<String> massGrindstoneItemList = new HashSet<>();
    public int massGrindstoneOperationsPerTick = 10;    // Number of operations per tick
    public boolean massGrindstoneDropResults = true;    // Drop results for maximum efficiency

    // Update checker settings
    public boolean updateCheckerEnabled = true;
    public String updateCheckerProjectId = "";
    public CheckFrequency updateCheckerFrequency = CheckFrequency.STARTUP_ONLY;
    public long updateCheckerLastCheck = 0;
    public boolean updateCheckerShowNotification = true;

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
                    this.autoMoveEnabled = loaded.autoMoveEnabled;
                    if (loaded.autoMoveDirection != null) {
                        this.autoMoveDirection = loaded.autoMoveDirection;
                    }
                    this.entityCullingEnabled = loaded.entityCullingEnabled;
                    this.disableAllEntityRendering = loaded.disableAllEntityRendering;
                    this.disableArmorStandRendering = loaded.disableArmorStandRendering;
                    this.disableFallingBlockRendering = loaded.disableFallingBlockRendering;
                    this.disableDeadMobRendering = loaded.disableDeadMobRendering;
                    this.itemRenderLimit = loaded.itemRenderLimit;
                    this.xpOrbRenderLimit = loaded.xpOrbRenderLimit;
                    if (loaded.entityBlacklist != null) {
                        this.entityBlacklist = new HashSet<>(loaded.entityBlacklist);
                    }
                    if (loaded.itemEntityBlacklist != null) {
                        this.itemEntityBlacklist = new HashSet<>(loaded.itemEntityBlacklist);
                    }
                    this.lavaHighlightEnabled = loaded.lavaHighlightEnabled;
                    this.lavaHighlightSource = loaded.lavaHighlightSource;
                    this.lavaHighlightFlowing = loaded.lavaHighlightFlowing;
                    this.lavaSourceColor = loaded.lavaSourceColor;
                    this.lavaFlowingColor = loaded.lavaFlowingColor;
                    this.notepadEnabled = loaded.notepadEnabled;
                    this.autoTotemEnabled = loaded.autoTotemEnabled;
                    this.autoRepairEnabled = loaded.autoRepairEnabled;
                    this.autoRepairClicksPerTick = loaded.autoRepairClicksPerTick;
                    this.autoRepairWhitelistMode = loaded.autoRepairWhitelistMode;
                    if (loaded.autoRepairItemList != null) {
                        this.autoRepairItemList = new HashSet<>(loaded.autoRepairItemList);
                    }
                    this.autoRepairTargetSlot = loaded.autoRepairTargetSlot;
                    this.massGrindstoneEnabled = loaded.massGrindstoneEnabled;
                    this.massGrindstoneWhitelistMode = loaded.massGrindstoneWhitelistMode;
                    if (loaded.massGrindstoneItemList != null) {
                        this.massGrindstoneItemList = new HashSet<>(loaded.massGrindstoneItemList);
                    }
                    this.massGrindstoneOperationsPerTick = loaded.massGrindstoneOperationsPerTick;
                    this.massGrindstoneDropResults = loaded.massGrindstoneDropResults;
                    this.updateCheckerEnabled = loaded.updateCheckerEnabled;
                    if (loaded.updateCheckerProjectId != null) {
                        this.updateCheckerProjectId = loaded.updateCheckerProjectId;
                    }
                    if (loaded.updateCheckerFrequency != null) {
                        this.updateCheckerFrequency = loaded.updateCheckerFrequency;
                    }
                    this.updateCheckerLastCheck = loaded.updateCheckerLastCheck;
                    this.updateCheckerShowNotification = loaded.updateCheckerShowNotification;
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

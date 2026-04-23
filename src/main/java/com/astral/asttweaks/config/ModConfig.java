package com.astral.asttweaks.config;

import com.astral.asttweaks.ASTTweaks;
import com.astral.asttweaks.feature.autorestock.AutoRestockEntry;
import com.astral.asttweaks.feature.autodrop.AutoDropMode;
import com.astral.asttweaks.feature.automove.MoveDirection;
import com.astral.asttweaks.feature.inventorysort.SortMode;
import com.astral.asttweaks.feature.inventorysort.SortTarget;
import com.astral.asttweaks.feature.updatechecker.CheckFrequency;
import com.astral.asttweaks.util.KeyCombo;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.lwjgl.glfw.GLFW;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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

    // Bone meal filter settings
    public boolean boneMealFilterEnabled = false;
    public Set<String> boneMealFilterWhitelist = new HashSet<>();

    // Mouse sensitivity settings
    public boolean mouseSensitivityEnabled = true;
    public int mouseSensitivityTargetValue = 10;  // 0-200（%表示）

    // Silk touch switch settings
    public boolean silkTouchSwitchEnabled = true;
    public Set<String> silkTouchSwitchBlockList = new HashSet<>();

    // Auto drop settings
    public boolean autoDropEnabled = false;
    public AutoDropMode autoDropMode = AutoDropMode.EXECUTE_KEY;
    public int autoDropOperationsPerTick = 8;
    public Set<Integer> autoDropProtectedSlots = new HashSet<>();  // PlayerInventory indices (0-8 hotbar, 9-35 main, 40 offhand)
    public Set<String> autoDropExcludedItems = new HashSet<>();

    // Auto restock settings
    public boolean autoRestockEnabled = false;
    public boolean autoRestockFromInventory = true;
    public boolean autoRestockFromShulker = true;
    public boolean autoRestockPreferOverExternalAutoCollect = false;
    public int autoRestockOperationsPerTick = 8;
    public List<AutoRestockEntry> autoRestockEntries = new ArrayList<>();

    // キーコンボ設定（全キーバインド）
    public KeyCombo scoreboardToggleKey = new KeyCombo(GLFW.GLFW_KEY_O, -1);
    public KeyCombo scoreboardPageUpKey = new KeyCombo(GLFW.GLFW_KEY_UP, -1);
    public KeyCombo scoreboardPageDownKey = new KeyCombo(GLFW.GLFW_KEY_DOWN, -1);
    public KeyCombo autoEatToggleKey = new KeyCombo(-1, -1);
    public KeyCombo autoMoveToggleKey = new KeyCombo(-1, -1);
    public KeyCombo autoMoveForwardKey = new KeyCombo(-1, -1);
    public KeyCombo autoMoveBackwardKey = new KeyCombo(-1, -1);
    public KeyCombo autoMoveLeftKey = new KeyCombo(-1, -1);
    public KeyCombo autoMoveRightKey = new KeyCombo(-1, -1);
    public KeyCombo autoMoveForwardLeftKey = new KeyCombo(-1, -1);
    public KeyCombo autoMoveForwardRightKey = new KeyCombo(-1, -1);
    public KeyCombo autoMoveBackwardLeftKey = new KeyCombo(-1, -1);
    public KeyCombo autoMoveBackwardRightKey = new KeyCombo(-1, -1);
    public KeyCombo autoTotemToggleKey = new KeyCombo(-1, -1);
    public KeyCombo autoRepairToggleKey = new KeyCombo(-1, -1);
    public KeyCombo boneMealFilterToggleKey = new KeyCombo(-1, -1);
    public KeyCombo silkTouchSwitchToggleKey = new KeyCombo(-1, -1);
    public KeyCombo mouseSensitivityToggleKey = new KeyCombo(-1, -1);
    public KeyCombo notepadOpenKey = new KeyCombo(-1, -1);
    public KeyCombo massGrindstoneExecuteKey = new KeyCombo(-1, -1);
    public KeyCombo inventorySortExecuteKey = new KeyCombo(GLFW.GLFW_KEY_R, -1);
    public KeyCombo inventorySortContainerExecuteKey = new KeyCombo(-1, -1);
    public KeyCombo openGeneralScreenKey = new KeyCombo(GLFW.GLFW_KEY_L, GLFW.GLFW_KEY_K);
    public KeyCombo autoDropToggleKey = new KeyCombo(-1, -1);
    public KeyCombo autoDropExecuteKey = new KeyCombo(-1, -1);
    public KeyCombo autoRestockToggleKey = new KeyCombo(-1, -1);
    public KeyCombo autoRestockInventoryToggleKey = new KeyCombo(-1, -1);
    public KeyCombo autoRestockShulkerToggleKey = new KeyCombo(-1, -1);

    // Inventory sort settings
    public boolean inventorySortEnabled = true;
    public SortMode inventorySortMode = SortMode.ITEM_ID;
    public Set<Integer> inventorySortExcludedSlots = new HashSet<>();
    public SortTarget inventorySortTarget = SortTarget.PLAYER_ONLY;
    public boolean inventorySortShowButton = true;

    // 16色の染料色名
    private static final String[] DYE_COLORS = {
        "white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray",
        "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black"
    };

    private ModConfig() {
        // Default blacklist items
        autoEatBlacklist.add("minecraft:rotten_flesh");
        autoEatBlacklist.add("minecraft:spider_eye");
        autoEatBlacklist.add("minecraft:poisonous_potato");
        autoEatBlacklist.add("minecraft:pufferfish");

        // デフォルトのシルクタッチ対象ブロック（ガラス系）
        silkTouchSwitchBlockList.add("minecraft:glass");
        silkTouchSwitchBlockList.add("minecraft:glass_pane");
        for (String color : DYE_COLORS) {
            silkTouchSwitchBlockList.add("minecraft:" + color + "_stained_glass");
            silkTouchSwitchBlockList.add("minecraft:" + color + "_stained_glass_pane");
        }

        // Auto Drop default protection: hotbar (0-8) + offhand (40)
        for (int i = 0; i <= 8; i++) {
            autoDropProtectedSlots.add(i);
        }
        autoDropProtectedSlots.add(40);
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
                    this.boneMealFilterEnabled = loaded.boneMealFilterEnabled;
                    if (loaded.boneMealFilterWhitelist != null) {
                        this.boneMealFilterWhitelist = new HashSet<>(loaded.boneMealFilterWhitelist);
                    }
                    this.inventorySortEnabled = loaded.inventorySortEnabled;
                    if (loaded.inventorySortMode != null) {
                        this.inventorySortMode = loaded.inventorySortMode;
                    }
                    if (loaded.inventorySortExcludedSlots != null) {
                        this.inventorySortExcludedSlots = new HashSet<>(loaded.inventorySortExcludedSlots);
                    }
                    if (loaded.inventorySortTarget != null) {
                        this.inventorySortTarget = loaded.inventorySortTarget;
                    }
                    this.inventorySortShowButton = loaded.inventorySortShowButton;
                    this.mouseSensitivityEnabled = loaded.mouseSensitivityEnabled;
                    this.mouseSensitivityTargetValue = loaded.mouseSensitivityTargetValue;
                    this.silkTouchSwitchEnabled = loaded.silkTouchSwitchEnabled;
                    if (loaded.silkTouchSwitchBlockList != null) {
                        this.silkTouchSwitchBlockList = new HashSet<>(loaded.silkTouchSwitchBlockList);
                    }
                    this.autoDropEnabled = loaded.autoDropEnabled;
                    if (loaded.autoDropMode != null) {
                        this.autoDropMode = loaded.autoDropMode;
                    }
                    this.autoDropOperationsPerTick = loaded.autoDropOperationsPerTick;
                    if (loaded.autoDropProtectedSlots != null) {
                        this.autoDropProtectedSlots = new HashSet<>(loaded.autoDropProtectedSlots);
                    }
                    if (loaded.autoDropExcludedItems != null) {
                        this.autoDropExcludedItems = new HashSet<>(loaded.autoDropExcludedItems);
                    }
                    this.autoRestockEnabled = loaded.autoRestockEnabled;
                    boolean hasAutoRestockConfig = loaded.autoRestockOperationsPerTick > 0;
                    this.autoRestockFromInventory = hasAutoRestockConfig ? loaded.autoRestockFromInventory : true;
                    this.autoRestockFromShulker = hasAutoRestockConfig ? loaded.autoRestockFromShulker : true;
                    this.autoRestockPreferOverExternalAutoCollect = loaded.autoRestockPreferOverExternalAutoCollect;
                    this.autoRestockOperationsPerTick = hasAutoRestockConfig ? loaded.autoRestockOperationsPerTick : 8;
                    if (loaded.autoRestockEntries != null) {
                        this.autoRestockEntries = new ArrayList<>(loaded.autoRestockEntries);
                    }
                    // キーコンボ設定の読み込み
                    if (loaded.scoreboardToggleKey != null) {
                        this.scoreboardToggleKey.copyFrom(loaded.scoreboardToggleKey);
                    }
                    if (loaded.scoreboardPageUpKey != null) {
                        this.scoreboardPageUpKey.copyFrom(loaded.scoreboardPageUpKey);
                    }
                    if (loaded.scoreboardPageDownKey != null) {
                        this.scoreboardPageDownKey.copyFrom(loaded.scoreboardPageDownKey);
                    }
                    if (loaded.autoEatToggleKey != null) {
                        this.autoEatToggleKey.copyFrom(loaded.autoEatToggleKey);
                    }
                    if (loaded.autoMoveToggleKey != null) {
                        this.autoMoveToggleKey.copyFrom(loaded.autoMoveToggleKey);
                    }
                    if (loaded.autoMoveForwardKey != null) {
                        this.autoMoveForwardKey.copyFrom(loaded.autoMoveForwardKey);
                    }
                    if (loaded.autoMoveBackwardKey != null) {
                        this.autoMoveBackwardKey.copyFrom(loaded.autoMoveBackwardKey);
                    }
                    if (loaded.autoMoveLeftKey != null) {
                        this.autoMoveLeftKey.copyFrom(loaded.autoMoveLeftKey);
                    }
                    if (loaded.autoMoveRightKey != null) {
                        this.autoMoveRightKey.copyFrom(loaded.autoMoveRightKey);
                    }
                    if (loaded.autoMoveForwardLeftKey != null) {
                        this.autoMoveForwardLeftKey.copyFrom(loaded.autoMoveForwardLeftKey);
                    }
                    if (loaded.autoMoveForwardRightKey != null) {
                        this.autoMoveForwardRightKey.copyFrom(loaded.autoMoveForwardRightKey);
                    }
                    if (loaded.autoMoveBackwardLeftKey != null) {
                        this.autoMoveBackwardLeftKey.copyFrom(loaded.autoMoveBackwardLeftKey);
                    }
                    if (loaded.autoMoveBackwardRightKey != null) {
                        this.autoMoveBackwardRightKey.copyFrom(loaded.autoMoveBackwardRightKey);
                    }
                    if (loaded.autoTotemToggleKey != null) {
                        this.autoTotemToggleKey.copyFrom(loaded.autoTotemToggleKey);
                    }
                    if (loaded.autoRepairToggleKey != null) {
                        this.autoRepairToggleKey.copyFrom(loaded.autoRepairToggleKey);
                    }
                    if (loaded.boneMealFilterToggleKey != null) {
                        this.boneMealFilterToggleKey.copyFrom(loaded.boneMealFilterToggleKey);
                    }
                    if (loaded.silkTouchSwitchToggleKey != null) {
                        this.silkTouchSwitchToggleKey.copyFrom(loaded.silkTouchSwitchToggleKey);
                    }
                    if (loaded.mouseSensitivityToggleKey != null) {
                        this.mouseSensitivityToggleKey.copyFrom(loaded.mouseSensitivityToggleKey);
                    }
                    if (loaded.notepadOpenKey != null) {
                        this.notepadOpenKey.copyFrom(loaded.notepadOpenKey);
                    }
                    if (loaded.massGrindstoneExecuteKey != null) {
                        this.massGrindstoneExecuteKey.copyFrom(loaded.massGrindstoneExecuteKey);
                    }
                    if (loaded.inventorySortExecuteKey != null) {
                        this.inventorySortExecuteKey.copyFrom(loaded.inventorySortExecuteKey);
                    }
                    if (loaded.inventorySortContainerExecuteKey != null) {
                        this.inventorySortContainerExecuteKey.copyFrom(loaded.inventorySortContainerExecuteKey);
                    }
                    if (loaded.openGeneralScreenKey != null) {
                        this.openGeneralScreenKey.copyFrom(loaded.openGeneralScreenKey);
                    }
                    if (loaded.autoDropToggleKey != null) {
                        this.autoDropToggleKey.copyFrom(loaded.autoDropToggleKey);
                    }
                    if (loaded.autoDropExecuteKey != null) {
                        this.autoDropExecuteKey.copyFrom(loaded.autoDropExecuteKey);
                    }
                    if (loaded.autoRestockToggleKey != null) {
                        this.autoRestockToggleKey.copyFrom(loaded.autoRestockToggleKey);
                    }
                    if (loaded.autoRestockInventoryToggleKey != null) {
                        this.autoRestockInventoryToggleKey.copyFrom(loaded.autoRestockInventoryToggleKey);
                    }
                    if (loaded.autoRestockShulkerToggleKey != null) {
                        this.autoRestockShulkerToggleKey.copyFrom(loaded.autoRestockShulkerToggleKey);
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

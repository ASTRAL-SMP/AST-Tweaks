package com.astral.asttweaks.config;

import com.astral.asttweaks.ASTTweaks;
import com.astral.asttweaks.feature.autodrop.AutoDropMode;
import com.astral.asttweaks.feature.autodrop.gui.AutoDropItemListScreen;
import com.astral.asttweaks.feature.autodrop.gui.AutoDropProtectedSlotScreen;
import com.astral.asttweaks.feature.autorestock.gui.AutoRestockItemListScreen;
import com.astral.asttweaks.feature.autoeat.gui.ButtonEntry;
import com.astral.asttweaks.feature.autoeat.gui.FoodListScreen;
import com.astral.asttweaks.feature.autoeat.gui.HungerBarEntry;
import com.astral.asttweaks.feature.automove.MoveDirection;
import com.astral.asttweaks.feature.autorepair.gui.RepairItemListScreen;
import com.astral.asttweaks.feature.bonemealfilter.gui.BlockListScreen;
import com.astral.asttweaks.feature.silktouchswitch.gui.SilkTouchBlockListScreen;
import com.astral.asttweaks.feature.entityculling.gui.EntityListScreen;
import com.astral.asttweaks.feature.entityculling.gui.ItemEntityListScreen;
import com.astral.asttweaks.feature.inventorysort.SortMode;
import com.astral.asttweaks.feature.inventorysort.SortTarget;
import com.astral.asttweaks.feature.inventorysort.gui.ExcludedSlotScreen;
import com.astral.asttweaks.feature.massgrindstone.gui.GrindstoneItemListScreen;
import com.astral.asttweaks.feature.updatechecker.CheckFrequency;
import com.astral.asttweaks.util.KeyCombo;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.gui.entries.IntegerSliderEntry;
import me.shedaniel.clothconfig2.impl.builders.SubCategoryBuilder;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

/**
 * ModMenu integration and configuration screen using Cloth Config.
 */
public class ConfigScreen implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return MainConfigScreen::new;
    }

    /**
     * Creates the Cloth Config settings screen.
     */
    public static Screen createClothConfigScreen(Screen parent) {
        ModConfig config = ModConfig.getInstance();

        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Text.translatable("config." + ASTTweaks.MOD_ID + ".title"));

        ConfigEntryBuilder entryBuilder = builder.entryBuilder();

        // ============================
        // 一般カテゴリ（最初のタブ）
        // ============================
        ConfigCategory general = builder.getOrCreateCategory(
                Text.translatable("config." + ASTTweaks.MOD_ID + ".category.general"));

        // --- Auto Totem サブカテゴリ ---
        SubCategoryBuilder autoTotemSub = entryBuilder.startSubCategory(
                Text.translatable("config." + ASTTweaks.MOD_ID + ".category.autototem"));

        autoTotemSub.add(entryBuilder
                .startBooleanToggle(
                        Text.translatable("config." + ASTTweaks.MOD_ID + ".autototem.enabled"),
                        config.autoTotemEnabled)
                .setDefaultValue(true)
                .setTooltip(Text.translatable("config." + ASTTweaks.MOD_ID + ".autototem.enabled.tooltip"))
                .setSaveConsumer(value -> config.autoTotemEnabled = value)
                .build());

        autoTotemSub.add(new KeyComboEntry(
                Text.translatable("config." + ASTTweaks.MOD_ID + ".keybind.autoTotemToggle"),
                config.autoTotemToggleKey,
                new KeyCombo(-1, -1),
                combo -> config.autoTotemToggleKey.copyFrom(combo)));

        general.addEntry(autoTotemSub.build());

        // --- Notepad サブカテゴリ ---
        SubCategoryBuilder notepadSub = entryBuilder.startSubCategory(
                Text.translatable("config." + ASTTweaks.MOD_ID + ".category.notepad"));

        notepadSub.add(entryBuilder
                .startBooleanToggle(
                        Text.translatable("config." + ASTTweaks.MOD_ID + ".notepad.enabled"),
                        config.notepadEnabled)
                .setDefaultValue(true)
                .setTooltip(Text.translatable("config." + ASTTweaks.MOD_ID + ".notepad.enabled.tooltip"))
                .setSaveConsumer(value -> config.notepadEnabled = value)
                .build());

        notepadSub.add(new KeyComboEntry(
                Text.translatable("config." + ASTTweaks.MOD_ID + ".keybind.notepadOpen"),
                config.notepadOpenKey,
                new KeyCombo(-1, -1),
                combo -> config.notepadOpenKey.copyFrom(combo)));

        general.addEntry(notepadSub.build());

        // --- Auto Move サブカテゴリ ---
        SubCategoryBuilder autoMoveSub = entryBuilder.startSubCategory(
                Text.translatable("config." + ASTTweaks.MOD_ID + ".category.automove"));

        autoMoveSub.add(entryBuilder
                .startBooleanToggle(
                        Text.translatable("config." + ASTTweaks.MOD_ID + ".automove.enabled"),
                        config.autoMoveEnabled)
                .setDefaultValue(true)
                .setTooltip(Text.translatable("config." + ASTTweaks.MOD_ID + ".automove.enabled.tooltip"))
                .setSaveConsumer(value -> config.autoMoveEnabled = value)
                .build());

        autoMoveSub.add(entryBuilder
                .startEnumSelector(
                        Text.translatable("config." + ASTTweaks.MOD_ID + ".automove.direction"),
                        MoveDirection.class,
                        config.autoMoveDirection)
                .setDefaultValue(MoveDirection.FORWARD)
                .setTooltip(Text.translatable("config." + ASTTweaks.MOD_ID + ".automove.direction.tooltip"))
                .setEnumNameProvider(dir -> Text.translatable("config." + ASTTweaks.MOD_ID + ".automove.direction." + ((MoveDirection)dir).getId()))
                .setSaveConsumer(value -> config.autoMoveDirection = value)
                .build());

        autoMoveSub.add(new KeyComboEntry(
                Text.translatable("config." + ASTTweaks.MOD_ID + ".keybind.autoMoveToggle"),
                config.autoMoveToggleKey,
                new KeyCombo(-1, -1),
                combo -> config.autoMoveToggleKey.copyFrom(combo)));

        autoMoveSub.add(new KeyComboEntry(
                Text.translatable("config." + ASTTweaks.MOD_ID + ".keybind.autoMoveForward"),
                config.autoMoveForwardKey,
                new KeyCombo(-1, -1),
                combo -> config.autoMoveForwardKey.copyFrom(combo)));

        autoMoveSub.add(new KeyComboEntry(
                Text.translatable("config." + ASTTweaks.MOD_ID + ".keybind.autoMoveBackward"),
                config.autoMoveBackwardKey,
                new KeyCombo(-1, -1),
                combo -> config.autoMoveBackwardKey.copyFrom(combo)));

        autoMoveSub.add(new KeyComboEntry(
                Text.translatable("config." + ASTTweaks.MOD_ID + ".keybind.autoMoveLeft"),
                config.autoMoveLeftKey,
                new KeyCombo(-1, -1),
                combo -> config.autoMoveLeftKey.copyFrom(combo)));

        autoMoveSub.add(new KeyComboEntry(
                Text.translatable("config." + ASTTweaks.MOD_ID + ".keybind.autoMoveRight"),
                config.autoMoveRightKey,
                new KeyCombo(-1, -1),
                combo -> config.autoMoveRightKey.copyFrom(combo)));

        autoMoveSub.add(new KeyComboEntry(
                Text.translatable("config." + ASTTweaks.MOD_ID + ".keybind.autoMoveForwardLeft"),
                config.autoMoveForwardLeftKey,
                new KeyCombo(-1, -1),
                combo -> config.autoMoveForwardLeftKey.copyFrom(combo)));

        autoMoveSub.add(new KeyComboEntry(
                Text.translatable("config." + ASTTweaks.MOD_ID + ".keybind.autoMoveForwardRight"),
                config.autoMoveForwardRightKey,
                new KeyCombo(-1, -1),
                combo -> config.autoMoveForwardRightKey.copyFrom(combo)));

        autoMoveSub.add(new KeyComboEntry(
                Text.translatable("config." + ASTTweaks.MOD_ID + ".keybind.autoMoveBackwardLeft"),
                config.autoMoveBackwardLeftKey,
                new KeyCombo(-1, -1),
                combo -> config.autoMoveBackwardLeftKey.copyFrom(combo)));

        autoMoveSub.add(new KeyComboEntry(
                Text.translatable("config." + ASTTweaks.MOD_ID + ".keybind.autoMoveBackwardRight"),
                config.autoMoveBackwardRightKey,
                new KeyCombo(-1, -1),
                combo -> config.autoMoveBackwardRightKey.copyFrom(combo)));

        general.addEntry(autoMoveSub.build());

        // --- Mouse Sensitivity サブカテゴリ ---
        SubCategoryBuilder mouseSensSub = entryBuilder.startSubCategory(
                Text.translatable("config." + ASTTweaks.MOD_ID + ".category.mousesensitivity"));

        mouseSensSub.add(entryBuilder
                .startBooleanToggle(
                        Text.translatable("config." + ASTTweaks.MOD_ID + ".mousesensitivity.enabled"),
                        config.mouseSensitivityEnabled)
                .setDefaultValue(true)
                .setTooltip(Text.translatable("config." + ASTTweaks.MOD_ID + ".mousesensitivity.enabled.tooltip"))
                .setSaveConsumer(value -> config.mouseSensitivityEnabled = value)
                .build());

        mouseSensSub.add(entryBuilder
                .startIntSlider(
                        Text.translatable("config." + ASTTweaks.MOD_ID + ".mousesensitivity.targetValue"),
                        config.mouseSensitivityTargetValue, 0, 200)
                .setDefaultValue(10)
                .setTooltip(Text.translatable("config." + ASTTweaks.MOD_ID + ".mousesensitivity.targetValue.tooltip"))
                .setTextGetter(value -> Text.literal(value + "%"))
                .setSaveConsumer(value -> config.mouseSensitivityTargetValue = value)
                .build());

        mouseSensSub.add(new KeyComboEntry(
                Text.translatable("config." + ASTTweaks.MOD_ID + ".keybind.mouseSensitivityToggle"),
                config.mouseSensitivityToggleKey,
                new KeyCombo(-1, -1),
                combo -> config.mouseSensitivityToggleKey.copyFrom(combo)));

        general.addEntry(mouseSensSub.build());

        // --- Update Checker サブカテゴリ ---
        SubCategoryBuilder updateCheckerSub = entryBuilder.startSubCategory(
                Text.translatable("config." + ASTTweaks.MOD_ID + ".category.updatechecker"));

        updateCheckerSub.add(entryBuilder
                .startBooleanToggle(
                        Text.translatable("config." + ASTTweaks.MOD_ID + ".updatechecker.enabled"),
                        config.updateCheckerEnabled)
                .setDefaultValue(true)
                .setTooltip(Text.translatable("config." + ASTTweaks.MOD_ID + ".updatechecker.enabled.tooltip"))
                .setSaveConsumer(value -> config.updateCheckerEnabled = value)
                .build());

        updateCheckerSub.add(entryBuilder
                .startStrField(
                        Text.translatable("config." + ASTTweaks.MOD_ID + ".updatechecker.projectId"),
                        config.updateCheckerProjectId)
                .setDefaultValue("")
                .setTooltip(Text.translatable("config." + ASTTweaks.MOD_ID + ".updatechecker.projectId.tooltip"))
                .setSaveConsumer(value -> config.updateCheckerProjectId = value)
                .build());

        updateCheckerSub.add(entryBuilder
                .startEnumSelector(
                        Text.translatable("config." + ASTTweaks.MOD_ID + ".updatechecker.frequency"),
                        CheckFrequency.class,
                        config.updateCheckerFrequency)
                .setDefaultValue(CheckFrequency.STARTUP_ONLY)
                .setTooltip(Text.translatable("config." + ASTTweaks.MOD_ID + ".updatechecker.frequency.tooltip"))
                .setEnumNameProvider(freq -> Text.translatable("config." + ASTTweaks.MOD_ID + ".updatechecker.frequency." + ((CheckFrequency)freq).getId()))
                .setSaveConsumer(value -> config.updateCheckerFrequency = value)
                .build());

        updateCheckerSub.add(entryBuilder
                .startBooleanToggle(
                        Text.translatable("config." + ASTTweaks.MOD_ID + ".updatechecker.showNotification"),
                        config.updateCheckerShowNotification)
                .setDefaultValue(true)
                .setTooltip(Text.translatable("config." + ASTTweaks.MOD_ID + ".updatechecker.showNotification.tooltip"))
                .setSaveConsumer(value -> config.updateCheckerShowNotification = value)
                .build());

        general.addEntry(updateCheckerSub.build());

        // --- キーバインド サブカテゴリ（一般のみ） ---
        SubCategoryBuilder keyBindSub = entryBuilder.startSubCategory(
                Text.translatable("config." + ASTTweaks.MOD_ID + ".category.keybinds"));

        keyBindSub.add(new KeyComboEntry(
                Text.translatable("config." + ASTTweaks.MOD_ID + ".keybind.openGeneralScreen"),
                config.openGeneralScreenKey,
                new KeyCombo(GLFW.GLFW_KEY_L, GLFW.GLFW_KEY_K),
                combo -> config.openGeneralScreenKey.copyFrom(combo)));

        general.addEntry(keyBindSub.build());

        // ============================
        // Scoreboard category
        // ============================
        ConfigCategory scoreboard = builder.getOrCreateCategory(
                Text.translatable("config." + ASTTweaks.MOD_ID + ".category.scoreboard"));

        scoreboard.addEntry(entryBuilder
                .startBooleanToggle(
                        Text.translatable("config." + ASTTweaks.MOD_ID + ".scoreboard.enabled"),
                        config.scoreboardEnabled)
                .setDefaultValue(true)
                .setTooltip(Text.translatable("config." + ASTTweaks.MOD_ID + ".scoreboard.enabled.tooltip"))
                .setSaveConsumer(value -> config.scoreboardEnabled = value)
                .build());

        scoreboard.addEntry(entryBuilder
                .startBooleanToggle(
                        Text.translatable("config." + ASTTweaks.MOD_ID + ".scoreboard.paging.enabled"),
                        config.scoreboardPagingEnabled)
                .setDefaultValue(true)
                .setTooltip(Text.translatable("config." + ASTTweaks.MOD_ID + ".scoreboard.paging.enabled.tooltip"))
                .setSaveConsumer(value -> config.scoreboardPagingEnabled = value)
                .build());

        scoreboard.addEntry(entryBuilder
                .startBooleanToggle(
                        Text.translatable("config." + ASTTweaks.MOD_ID + ".scoreboard.showRank"),
                        config.scoreboardShowRank)
                .setDefaultValue(false)
                .setTooltip(Text.translatable("config." + ASTTweaks.MOD_ID + ".scoreboard.showRank.tooltip"))
                .setSaveConsumer(value -> config.scoreboardShowRank = value)
                .build());

        scoreboard.addEntry(entryBuilder
                .startIntSlider(
                        Text.translatable("config." + ASTTweaks.MOD_ID + ".scoreboard.maxLines"),
                        config.scoreboardMaxLines, 5, 30)
                .setDefaultValue(15)
                .setTooltip(Text.translatable("config." + ASTTweaks.MOD_ID + ".scoreboard.maxLines.tooltip"))
                .setSaveConsumer(value -> config.scoreboardMaxLines = value)
                .build());

        scoreboard.addEntry(entryBuilder
                .startIntSlider(
                        Text.translatable("config." + ASTTweaks.MOD_ID + ".scoreboard.pageSize"),
                        config.scoreboardPageSize, 3, 20)
                .setDefaultValue(10)
                .setTooltip(Text.translatable("config." + ASTTweaks.MOD_ID + ".scoreboard.pageSize.tooltip"))
                .setSaveConsumer(value -> config.scoreboardPageSize = value)
                .build());

        // Position X slider (0-100%)
        scoreboard.addEntry(entryBuilder
                .startIntSlider(
                        Text.translatable("config." + ASTTweaks.MOD_ID + ".scoreboard.positionX"),
                        config.scoreboardPositionX, 0, 100)
                .setDefaultValue(100)
                .setTooltip(Text.translatable("config." + ASTTweaks.MOD_ID + ".scoreboard.positionX.tooltip"))
                .setTextGetter(value -> Text.literal(value + "%"))
                .setSaveConsumer(value -> config.scoreboardPositionX = value)
                .build());

        // Position Y slider (0-100%)
        scoreboard.addEntry(entryBuilder
                .startIntSlider(
                        Text.translatable("config." + ASTTweaks.MOD_ID + ".scoreboard.positionY"),
                        config.scoreboardPositionY, 0, 100)
                .setDefaultValue(50)
                .setTooltip(Text.translatable("config." + ASTTweaks.MOD_ID + ".scoreboard.positionY.tooltip"))
                .setTextGetter(value -> Text.literal(value + "%"))
                .setSaveConsumer(value -> config.scoreboardPositionY = value)
                .build());

        // Scale slider (percentage display)
        scoreboard.addEntry(entryBuilder
                .startIntSlider(
                        Text.translatable("config." + ASTTweaks.MOD_ID + ".scoreboard.scale"),
                        (int)(config.scoreboardScale * 100), 50, 200)
                .setDefaultValue(100)
                .setTooltip(Text.translatable("config." + ASTTweaks.MOD_ID + ".scoreboard.scale.tooltip"))
                .setTextGetter(value -> Text.literal(value + "%"))
                .setSaveConsumer(value -> config.scoreboardScale = value / 100.0f)
                .build());

        // Header color
        scoreboard.addEntry(entryBuilder
                .startAlphaColorField(
                        Text.translatable("config." + ASTTweaks.MOD_ID + ".scoreboard.headerColor"),
                        config.scoreboardHeaderColor)
                .setDefaultValue(0x66000000)
                .setTooltip(Text.translatable("config." + ASTTweaks.MOD_ID + ".scoreboard.headerColor.tooltip"))
                .setSaveConsumer(value -> config.scoreboardHeaderColor = value)
                .build());

        // Body color
        scoreboard.addEntry(entryBuilder
                .startAlphaColorField(
                        Text.translatable("config." + ASTTweaks.MOD_ID + ".scoreboard.bodyColor"),
                        config.scoreboardBodyColor)
                .setDefaultValue(0x4D000000)
                .setTooltip(Text.translatable("config." + ASTTweaks.MOD_ID + ".scoreboard.bodyColor.tooltip"))
                .setSaveConsumer(value -> config.scoreboardBodyColor = value)
                .build());

        // Text color
        scoreboard.addEntry(entryBuilder
                .startAlphaColorField(
                        Text.translatable("config." + ASTTweaks.MOD_ID + ".scoreboard.textColor"),
                        config.scoreboardTextColor)
                .setDefaultValue(0xFFFFFFFF)
                .setTooltip(Text.translatable("config." + ASTTweaks.MOD_ID + ".scoreboard.textColor.tooltip"))
                .setSaveConsumer(value -> config.scoreboardTextColor = value)
                .build());

        // スコアボードキーバインド
        SubCategoryBuilder scoreboardKeySub = entryBuilder.startSubCategory(
                Text.translatable("config." + ASTTweaks.MOD_ID + ".category.keybinds"));

        scoreboardKeySub.add(new KeyComboEntry(
                Text.translatable("config." + ASTTweaks.MOD_ID + ".keybind.scoreboardToggle"),
                config.scoreboardToggleKey,
                new KeyCombo(GLFW.GLFW_KEY_O, -1),
                combo -> config.scoreboardToggleKey.copyFrom(combo)));

        scoreboardKeySub.add(new KeyComboEntry(
                Text.translatable("config." + ASTTweaks.MOD_ID + ".keybind.scoreboardPageUp"),
                config.scoreboardPageUpKey,
                new KeyCombo(GLFW.GLFW_KEY_UP, -1),
                combo -> config.scoreboardPageUpKey.copyFrom(combo)));

        scoreboardKeySub.add(new KeyComboEntry(
                Text.translatable("config." + ASTTweaks.MOD_ID + ".keybind.scoreboardPageDown"),
                config.scoreboardPageDownKey,
                new KeyCombo(GLFW.GLFW_KEY_DOWN, -1),
                combo -> config.scoreboardPageDownKey.copyFrom(combo)));

        scoreboard.addEntry(scoreboardKeySub.build());

        // ============================
        // Auto-eat category
        // ============================
        ConfigCategory autoeat = builder.getOrCreateCategory(
                Text.translatable("config." + ASTTweaks.MOD_ID + ".category.autoeat"));

        autoeat.addEntry(entryBuilder
                .startBooleanToggle(
                        Text.translatable("config." + ASTTweaks.MOD_ID + ".autoeat.enabled"),
                        config.autoEatEnabled)
                .setDefaultValue(true)
                .setTooltip(Text.translatable("config." + ASTTweaks.MOD_ID + ".autoeat.enabled.tooltip"))
                .setSaveConsumer(value -> config.autoEatEnabled = value)
                .build());

        autoeat.addEntry(entryBuilder
                .startBooleanToggle(
                        Text.translatable("config." + ASTTweaks.MOD_ID + ".autoeat.eatWhileAction"),
                        config.autoEatWhileAction)
                .setDefaultValue(false)
                .setTooltip(Text.translatable("config." + ASTTweaks.MOD_ID + ".autoeat.eatWhileAction.tooltip"))
                .setSaveConsumer(value -> config.autoEatWhileAction = value)
                .build());

        IntegerSliderEntry hungerSlider = entryBuilder
                .startIntSlider(
                        Text.translatable("config." + ASTTweaks.MOD_ID + ".autoeat.hungerThreshold"),
                        config.autoEatHungerThreshold, 0, 20)
                .setDefaultValue(6)
                .setTooltip(Text.translatable("config." + ASTTweaks.MOD_ID + ".autoeat.hungerThreshold.tooltip"))
                .setSaveConsumer(value -> config.autoEatHungerThreshold = value)
                .build();

        autoeat.addEntry(new HungerBarEntry(hungerSlider::getValue));
        autoeat.addEntry(hungerSlider);

        autoeat.addEntry(new ButtonEntry(
                Text.translatable("config." + ASTTweaks.MOD_ID + ".autoeat.blacklist.button"),
                button -> MinecraftClient.getInstance().setScreen(new FoodListScreen(MinecraftClient.getInstance().currentScreen))
        ));

        autoeat.addEntry(new KeyComboEntry(
                Text.translatable("config." + ASTTweaks.MOD_ID + ".keybind.autoEatToggle"),
                config.autoEatToggleKey,
                new KeyCombo(-1, -1),
                combo -> config.autoEatToggleKey.copyFrom(combo)));

        // ============================
        // Entity culling category
        // ============================
        ConfigCategory entityCulling = builder.getOrCreateCategory(
                Text.translatable("config." + ASTTweaks.MOD_ID + ".category.entityculling"));

        entityCulling.addEntry(entryBuilder
                .startBooleanToggle(
                        Text.translatable("config." + ASTTweaks.MOD_ID + ".entityculling.enabled"),
                        config.entityCullingEnabled)
                .setDefaultValue(true)
                .setTooltip(Text.translatable("config." + ASTTweaks.MOD_ID + ".entityculling.enabled.tooltip"))
                .setSaveConsumer(value -> config.entityCullingEnabled = value)
                .build());

        entityCulling.addEntry(entryBuilder
                .startBooleanToggle(
                        Text.translatable("config." + ASTTweaks.MOD_ID + ".entityculling.disableAll"),
                        config.disableAllEntityRendering)
                .setDefaultValue(false)
                .setTooltip(Text.translatable("config." + ASTTweaks.MOD_ID + ".entityculling.disableAll.tooltip"))
                .setSaveConsumer(value -> config.disableAllEntityRendering = value)
                .build());

        entityCulling.addEntry(entryBuilder
                .startBooleanToggle(
                        Text.translatable("config." + ASTTweaks.MOD_ID + ".entityculling.disableArmorStand"),
                        config.disableArmorStandRendering)
                .setDefaultValue(false)
                .setTooltip(Text.translatable("config." + ASTTweaks.MOD_ID + ".entityculling.disableArmorStand.tooltip"))
                .setSaveConsumer(value -> config.disableArmorStandRendering = value)
                .build());

        entityCulling.addEntry(entryBuilder
                .startBooleanToggle(
                        Text.translatable("config." + ASTTweaks.MOD_ID + ".entityculling.disableFallingBlock"),
                        config.disableFallingBlockRendering)
                .setDefaultValue(false)
                .setTooltip(Text.translatable("config." + ASTTweaks.MOD_ID + ".entityculling.disableFallingBlock.tooltip"))
                .setSaveConsumer(value -> config.disableFallingBlockRendering = value)
                .build());

        entityCulling.addEntry(entryBuilder
                .startBooleanToggle(
                        Text.translatable("config." + ASTTweaks.MOD_ID + ".entityculling.disableDeadMob"),
                        config.disableDeadMobRendering)
                .setDefaultValue(false)
                .setTooltip(Text.translatable("config." + ASTTweaks.MOD_ID + ".entityculling.disableDeadMob.tooltip"))
                .setSaveConsumer(value -> config.disableDeadMobRendering = value)
                .build());

        entityCulling.addEntry(entryBuilder
                .startIntSlider(
                        Text.translatable("config." + ASTTweaks.MOD_ID + ".entityculling.itemLimit"),
                        config.itemRenderLimit, -1, 100)
                .setDefaultValue(-1)
                .setTooltip(Text.translatable("config." + ASTTweaks.MOD_ID + ".entityculling.itemLimit.tooltip"))
                .setTextGetter(value -> value < 0 ? Text.translatable("config." + ASTTweaks.MOD_ID + ".entityculling.unlimited") : Text.literal(String.valueOf(value)))
                .setSaveConsumer(value -> config.itemRenderLimit = value)
                .build());

        entityCulling.addEntry(entryBuilder
                .startIntSlider(
                        Text.translatable("config." + ASTTweaks.MOD_ID + ".entityculling.xpOrbLimit"),
                        config.xpOrbRenderLimit, -1, 100)
                .setDefaultValue(-1)
                .setTooltip(Text.translatable("config." + ASTTweaks.MOD_ID + ".entityculling.xpOrbLimit.tooltip"))
                .setTextGetter(value -> value < 0 ? Text.translatable("config." + ASTTweaks.MOD_ID + ".entityculling.unlimited") : Text.literal(String.valueOf(value)))
                .setSaveConsumer(value -> config.xpOrbRenderLimit = value)
                .build());

        entityCulling.addEntry(new ButtonEntry(
                Text.translatable("config." + ASTTweaks.MOD_ID + ".entityculling.blacklist.button"),
                button -> MinecraftClient.getInstance().setScreen(new EntityListScreen(MinecraftClient.getInstance().currentScreen))
        ));

        entityCulling.addEntry(new ButtonEntry(
                Text.translatable("config." + ASTTweaks.MOD_ID + ".entityculling.itemblacklist.button"),
                button -> MinecraftClient.getInstance().setScreen(new ItemEntityListScreen(MinecraftClient.getInstance().currentScreen))
        ));

        // ============================
        // Lava highlight category
        // ============================
        ConfigCategory lavaHighlight = builder.getOrCreateCategory(
                Text.translatable("config." + ASTTweaks.MOD_ID + ".category.lavahighlight"));

        lavaHighlight.addEntry(entryBuilder
                .startBooleanToggle(
                        Text.translatable("config." + ASTTweaks.MOD_ID + ".lavahighlight.enabled"),
                        config.lavaHighlightEnabled)
                .setDefaultValue(false)
                .setTooltip(Text.translatable("config." + ASTTweaks.MOD_ID + ".lavahighlight.enabled.tooltip"))
                .setSaveConsumer(value -> config.lavaHighlightEnabled = value)
                .build());

        lavaHighlight.addEntry(entryBuilder
                .startBooleanToggle(
                        Text.translatable("config." + ASTTweaks.MOD_ID + ".lavahighlight.source"),
                        config.lavaHighlightSource)
                .setDefaultValue(true)
                .setTooltip(Text.translatable("config." + ASTTweaks.MOD_ID + ".lavahighlight.source.tooltip"))
                .setSaveConsumer(value -> config.lavaHighlightSource = value)
                .build());

        lavaHighlight.addEntry(entryBuilder
                .startBooleanToggle(
                        Text.translatable("config." + ASTTweaks.MOD_ID + ".lavahighlight.flowing"),
                        config.lavaHighlightFlowing)
                .setDefaultValue(false)
                .setTooltip(Text.translatable("config." + ASTTweaks.MOD_ID + ".lavahighlight.flowing.tooltip"))
                .setSaveConsumer(value -> config.lavaHighlightFlowing = value)
                .build());

        lavaHighlight.addEntry(entryBuilder
                .startAlphaColorField(
                        Text.translatable("config." + ASTTweaks.MOD_ID + ".lavahighlight.sourceColor"),
                        config.lavaSourceColor)
                .setDefaultValue(0x8000FF00)
                .setTooltip(Text.translatable("config." + ASTTweaks.MOD_ID + ".lavahighlight.sourceColor.tooltip"))
                .setSaveConsumer(value -> config.lavaSourceColor = value)
                .build());

        lavaHighlight.addEntry(entryBuilder
                .startAlphaColorField(
                        Text.translatable("config." + ASTTweaks.MOD_ID + ".lavahighlight.flowingColor"),
                        config.lavaFlowingColor)
                .setDefaultValue(0x80FF0000)
                .setTooltip(Text.translatable("config." + ASTTweaks.MOD_ID + ".lavahighlight.flowingColor.tooltip"))
                .setSaveConsumer(value -> config.lavaFlowingColor = value)
                .build());

        // ============================
        // Auto repair category
        // ============================
        ConfigCategory autoRepair = builder.getOrCreateCategory(
                Text.translatable("config." + ASTTweaks.MOD_ID + ".category.autorepair"));

        autoRepair.addEntry(entryBuilder
                .startBooleanToggle(
                        Text.translatable("config." + ASTTweaks.MOD_ID + ".autorepair.enabled"),
                        config.autoRepairEnabled)
                .setDefaultValue(true)
                .setTooltip(Text.translatable("config." + ASTTweaks.MOD_ID + ".autorepair.enabled.tooltip"))
                .setSaveConsumer(value -> config.autoRepairEnabled = value)
                .build());

        autoRepair.addEntry(entryBuilder
                .startIntSlider(
                        Text.translatable("config." + ASTTweaks.MOD_ID + ".autorepair.clicksPerTick"),
                        config.autoRepairClicksPerTick, 1, 20)
                .setDefaultValue(10)
                .setTooltip(Text.translatable("config." + ASTTweaks.MOD_ID + ".autorepair.clicksPerTick.tooltip"))
                .setTextGetter(value -> Text.literal(value + " clicks"))
                .setSaveConsumer(value -> config.autoRepairClicksPerTick = value)
                .build());

        autoRepair.addEntry(entryBuilder
                .startBooleanToggle(
                        Text.translatable("config." + ASTTweaks.MOD_ID + ".autorepair.whitelistMode"),
                        config.autoRepairWhitelistMode)
                .setDefaultValue(false)
                .setTooltip(Text.translatable("config." + ASTTweaks.MOD_ID + ".autorepair.whitelistMode.tooltip"))
                .setSaveConsumer(value -> config.autoRepairWhitelistMode = value)
                .build());

        autoRepair.addEntry(entryBuilder
                .startIntSlider(
                        Text.translatable("config." + ASTTweaks.MOD_ID + ".autorepair.targetSlot"),
                        config.autoRepairTargetSlot, 0, 8)
                .setDefaultValue(0)
                .setTooltip(Text.translatable("config." + ASTTweaks.MOD_ID + ".autorepair.targetSlot.tooltip"))
                .setTextGetter(value -> Text.literal("Slot " + (value + 1)))
                .setSaveConsumer(value -> config.autoRepairTargetSlot = value)
                .build());

        autoRepair.addEntry(new ButtonEntry(
                Text.translatable("config." + ASTTweaks.MOD_ID + ".autorepair.itemlist.button"),
                button -> MinecraftClient.getInstance().setScreen(new RepairItemListScreen(MinecraftClient.getInstance().currentScreen))
        ));

        autoRepair.addEntry(new KeyComboEntry(
                Text.translatable("config." + ASTTweaks.MOD_ID + ".keybind.autoRepairToggle"),
                config.autoRepairToggleKey,
                new KeyCombo(-1, -1),
                combo -> config.autoRepairToggleKey.copyFrom(combo)));

        // ============================
        // Mass grindstone category
        // ============================
        ConfigCategory massGrindstone = builder.getOrCreateCategory(
                Text.translatable("config." + ASTTweaks.MOD_ID + ".category.massgrindstone"));

        massGrindstone.addEntry(entryBuilder
                .startBooleanToggle(
                        Text.translatable("config." + ASTTweaks.MOD_ID + ".massgrindstone.enabled"),
                        config.massGrindstoneEnabled)
                .setDefaultValue(true)
                .setTooltip(Text.translatable("config." + ASTTweaks.MOD_ID + ".massgrindstone.enabled.tooltip"))
                .setSaveConsumer(value -> config.massGrindstoneEnabled = value)
                .build());

        massGrindstone.addEntry(entryBuilder
                .startBooleanToggle(
                        Text.translatable("config." + ASTTweaks.MOD_ID + ".massgrindstone.whitelistMode"),
                        config.massGrindstoneWhitelistMode)
                .setDefaultValue(true)
                .setTooltip(Text.translatable("config." + ASTTweaks.MOD_ID + ".massgrindstone.whitelistMode.tooltip"))
                .setSaveConsumer(value -> config.massGrindstoneWhitelistMode = value)
                .build());

        massGrindstone.addEntry(entryBuilder
                .startIntSlider(
                        Text.translatable("config." + ASTTweaks.MOD_ID + ".massgrindstone.operationsPerTick"),
                        config.massGrindstoneOperationsPerTick, 1, 20)
                .setDefaultValue(10)
                .setTooltip(Text.translatable("config." + ASTTweaks.MOD_ID + ".massgrindstone.operationsPerTick.tooltip"))
                .setTextGetter(value -> Text.literal(value + " ops"))
                .setSaveConsumer(value -> config.massGrindstoneOperationsPerTick = value)
                .build());

        massGrindstone.addEntry(entryBuilder
                .startBooleanToggle(
                        Text.translatable("config." + ASTTweaks.MOD_ID + ".massgrindstone.dropResults"),
                        config.massGrindstoneDropResults)
                .setDefaultValue(true)
                .setTooltip(Text.translatable("config." + ASTTweaks.MOD_ID + ".massgrindstone.dropResults.tooltip"))
                .setSaveConsumer(value -> config.massGrindstoneDropResults = value)
                .build());

        massGrindstone.addEntry(new ButtonEntry(
                Text.translatable("config." + ASTTweaks.MOD_ID + ".massgrindstone.itemlist.button"),
                button -> MinecraftClient.getInstance().setScreen(new GrindstoneItemListScreen(MinecraftClient.getInstance().currentScreen))
        ));

        massGrindstone.addEntry(new KeyComboEntry(
                Text.translatable("config." + ASTTweaks.MOD_ID + ".keybind.massGrindstoneExecute"),
                config.massGrindstoneExecuteKey,
                new KeyCombo(-1, -1),
                combo -> config.massGrindstoneExecuteKey.copyFrom(combo)));

        // ============================
        // Inventory sort category
        // ============================
        ConfigCategory inventorySort = builder.getOrCreateCategory(
                Text.translatable("config." + ASTTweaks.MOD_ID + ".category.inventorysort"));

        inventorySort.addEntry(entryBuilder
                .startBooleanToggle(
                        Text.translatable("config." + ASTTweaks.MOD_ID + ".inventorysort.enabled"),
                        config.inventorySortEnabled)
                .setDefaultValue(true)
                .setTooltip(Text.translatable("config." + ASTTweaks.MOD_ID + ".inventorysort.enabled.tooltip"))
                .setSaveConsumer(value -> config.inventorySortEnabled = value)
                .build());

        inventorySort.addEntry(entryBuilder
                .startEnumSelector(
                        Text.translatable("config." + ASTTweaks.MOD_ID + ".inventorysort.mode"),
                        SortMode.class,
                        config.inventorySortMode)
                .setDefaultValue(SortMode.ITEM_ID)
                .setTooltip(Text.translatable("config." + ASTTweaks.MOD_ID + ".inventorysort.mode.tooltip"))
                .setEnumNameProvider(mode -> Text.translatable("config." + ASTTweaks.MOD_ID + ".inventorysort.mode." + ((SortMode)mode).getId()))
                .setSaveConsumer(value -> config.inventorySortMode = value)
                .build());

        inventorySort.addEntry(entryBuilder
                .startEnumSelector(
                        Text.translatable("config." + ASTTweaks.MOD_ID + ".inventorysort.target"),
                        SortTarget.class,
                        config.inventorySortTarget)
                .setDefaultValue(SortTarget.PLAYER_ONLY)
                .setTooltip(Text.translatable("config." + ASTTweaks.MOD_ID + ".inventorysort.target.tooltip"))
                .setEnumNameProvider(target -> Text.translatable("config." + ASTTweaks.MOD_ID + ".inventorysort.target." + ((SortTarget)target).getId()))
                .setSaveConsumer(value -> config.inventorySortTarget = value)
                .build());

        inventorySort.addEntry(entryBuilder
                .startBooleanToggle(
                        Text.translatable("config." + ASTTweaks.MOD_ID + ".inventorysort.showButton"),
                        config.inventorySortShowButton)
                .setDefaultValue(true)
                .setTooltip(Text.translatable("config." + ASTTweaks.MOD_ID + ".inventorysort.showButton.tooltip"))
                .setSaveConsumer(value -> config.inventorySortShowButton = value)
                .build());

        inventorySort.addEntry(new ButtonEntry(
                Text.translatable("config." + ASTTweaks.MOD_ID + ".inventorysort.excludedslots.button"),
                button -> MinecraftClient.getInstance().setScreen(new ExcludedSlotScreen(MinecraftClient.getInstance().currentScreen))
        ));

        inventorySort.addEntry(new KeyComboEntry(
                Text.translatable("config." + ASTTweaks.MOD_ID + ".keybind.inventorySortExecute"),
                config.inventorySortExecuteKey,
                new KeyCombo(GLFW.GLFW_KEY_R, -1),
                combo -> config.inventorySortExecuteKey.copyFrom(combo)));

        inventorySort.addEntry(new KeyComboEntry(
                Text.translatable("config." + ASTTweaks.MOD_ID + ".keybind.inventorySortContainerExecute"),
                config.inventorySortContainerExecuteKey,
                new KeyCombo(-1, -1),
                combo -> config.inventorySortContainerExecuteKey.copyFrom(combo)));

        // ============================
        // Bone meal filter category
        // ============================
        ConfigCategory boneMealFilter = builder.getOrCreateCategory(
                Text.translatable("config." + ASTTweaks.MOD_ID + ".category.bonemealfilter"));

        boneMealFilter.addEntry(entryBuilder
                .startBooleanToggle(
                        Text.translatable("config." + ASTTweaks.MOD_ID + ".bonemealfilter.enabled"),
                        config.boneMealFilterEnabled)
                .setDefaultValue(false)
                .setTooltip(Text.translatable("config." + ASTTweaks.MOD_ID + ".bonemealfilter.enabled.tooltip"))
                .setSaveConsumer(value -> config.boneMealFilterEnabled = value)
                .build());

        boneMealFilter.addEntry(new ButtonEntry(
                Text.translatable("config." + ASTTweaks.MOD_ID + ".bonemealfilter.whitelist.button"),
                button -> MinecraftClient.getInstance().setScreen(new BlockListScreen(MinecraftClient.getInstance().currentScreen))
        ));

        boneMealFilter.addEntry(new KeyComboEntry(
                Text.translatable("config." + ASTTweaks.MOD_ID + ".keybind.boneMealFilterToggle"),
                config.boneMealFilterToggleKey,
                new KeyCombo(-1, -1),
                combo -> config.boneMealFilterToggleKey.copyFrom(combo)));

        // ============================
        // Silk touch switch category
        // ============================
        ConfigCategory silkTouchSwitch = builder.getOrCreateCategory(
                Text.translatable("config." + ASTTweaks.MOD_ID + ".category.silktouchswitch"));

        silkTouchSwitch.addEntry(entryBuilder
                .startBooleanToggle(
                        Text.translatable("config." + ASTTweaks.MOD_ID + ".silktouchswitch.enabled"),
                        config.silkTouchSwitchEnabled)
                .setDefaultValue(true)
                .setTooltip(Text.translatable("config." + ASTTweaks.MOD_ID + ".silktouchswitch.enabled.tooltip"))
                .setSaveConsumer(value -> config.silkTouchSwitchEnabled = value)
                .build());

        silkTouchSwitch.addEntry(new ButtonEntry(
                Text.translatable("config." + ASTTweaks.MOD_ID + ".silktouchswitch.blocklist.button"),
                button -> MinecraftClient.getInstance().setScreen(new SilkTouchBlockListScreen(MinecraftClient.getInstance().currentScreen))
        ));

        silkTouchSwitch.addEntry(new KeyComboEntry(
                Text.translatable("config." + ASTTweaks.MOD_ID + ".keybind.silkTouchSwitchToggle"),
                config.silkTouchSwitchToggleKey,
                new KeyCombo(-1, -1),
                combo -> config.silkTouchSwitchToggleKey.copyFrom(combo)));

        // ============================
        // Auto drop category
        // ============================
        ConfigCategory autoDrop = builder.getOrCreateCategory(
                Text.translatable("config." + ASTTweaks.MOD_ID + ".category.autodrop"));

        autoDrop.addEntry(entryBuilder
                .startBooleanToggle(
                        Text.translatable("config." + ASTTweaks.MOD_ID + ".autodrop.enabled"),
                        config.autoDropEnabled)
                .setDefaultValue(false)
                .setTooltip(Text.translatable("config." + ASTTweaks.MOD_ID + ".autodrop.enabled.tooltip"))
                .setSaveConsumer(value -> config.autoDropEnabled = value)
                .build());

        autoDrop.addEntry(entryBuilder
                .startEnumSelector(
                        Text.translatable("config." + ASTTweaks.MOD_ID + ".autodrop.mode"),
                        AutoDropMode.class,
                        config.autoDropMode)
                .setDefaultValue(AutoDropMode.EXECUTE_KEY)
                .setTooltip(Text.translatable("config." + ASTTweaks.MOD_ID + ".autodrop.mode.tooltip"))
                .setEnumNameProvider(mode -> Text.translatable("config." + ASTTweaks.MOD_ID + ".autodrop.mode." + ((AutoDropMode)mode).getId()))
                .setSaveConsumer(value -> config.autoDropMode = value)
                .build());

        autoDrop.addEntry(entryBuilder
                .startIntSlider(
                        Text.translatable("config." + ASTTweaks.MOD_ID + ".autodrop.operationsPerTick"),
                        config.autoDropOperationsPerTick, 1, 64)
                .setDefaultValue(8)
                .setTooltip(Text.translatable("config." + ASTTweaks.MOD_ID + ".autodrop.operationsPerTick.tooltip"))
                .setTextGetter(value -> Text.literal(value + " ops"))
                .setSaveConsumer(value -> config.autoDropOperationsPerTick = value)
                .build());

        autoDrop.addEntry(new ButtonEntry(
                Text.translatable("config." + ASTTweaks.MOD_ID + ".autodrop.protectedslots.button"),
                button -> MinecraftClient.getInstance().setScreen(new AutoDropProtectedSlotScreen(MinecraftClient.getInstance().currentScreen))
        ));

        autoDrop.addEntry(new ButtonEntry(
                Text.translatable("config." + ASTTweaks.MOD_ID + ".autodrop.itemlist.button"),
                button -> MinecraftClient.getInstance().setScreen(new AutoDropItemListScreen(MinecraftClient.getInstance().currentScreen))
        ));

        autoDrop.addEntry(new KeyComboEntry(
                Text.translatable("config." + ASTTweaks.MOD_ID + ".keybind.autoDropToggle"),
                config.autoDropToggleKey,
                new KeyCombo(-1, -1),
                combo -> config.autoDropToggleKey.copyFrom(combo)));

        autoDrop.addEntry(new KeyComboEntry(
                Text.translatable("config." + ASTTweaks.MOD_ID + ".keybind.autoDropExecute"),
                config.autoDropExecuteKey,
                new KeyCombo(-1, -1),
                combo -> config.autoDropExecuteKey.copyFrom(combo)));

        // ============================
        // Auto restock category
        // ============================
        ConfigCategory autoRestock = builder.getOrCreateCategory(
                Text.translatable("config." + ASTTweaks.MOD_ID + ".category.autorestock"));

        autoRestock.addEntry(entryBuilder
                .startBooleanToggle(
                        Text.translatable("config." + ASTTweaks.MOD_ID + ".autorestock.enabled"),
                        config.autoRestockEnabled)
                .setDefaultValue(false)
                .setTooltip(Text.translatable("config." + ASTTweaks.MOD_ID + ".autorestock.enabled.tooltip"))
                .setSaveConsumer(value -> config.autoRestockEnabled = value)
                .build());

        autoRestock.addEntry(entryBuilder
                .startBooleanToggle(
                        Text.translatable("config." + ASTTweaks.MOD_ID + ".autorestock.inventory.enabled"),
                        config.autoRestockFromInventory)
                .setDefaultValue(true)
                .setTooltip(Text.translatable("config." + ASTTweaks.MOD_ID + ".autorestock.inventory.enabled.tooltip"))
                .setSaveConsumer(value -> config.autoRestockFromInventory = value)
                .build());

        autoRestock.addEntry(entryBuilder
                .startBooleanToggle(
                        Text.translatable("config." + ASTTweaks.MOD_ID + ".autorestock.shulker.enabled"),
                        config.autoRestockFromShulker)
                .setDefaultValue(true)
                .setTooltip(Text.translatable("config." + ASTTweaks.MOD_ID + ".autorestock.shulker.enabled.tooltip"))
                .setSaveConsumer(value -> config.autoRestockFromShulker = value)
                .build());

        autoRestock.addEntry(entryBuilder
                .startBooleanToggle(
                        Text.translatable("config." + ASTTweaks.MOD_ID + ".autorestock.priorityExternalAutoCollect.enabled"),
                        config.autoRestockPreferOverExternalAutoCollect)
                .setDefaultValue(false)
                .setTooltip(Text.translatable("config." + ASTTweaks.MOD_ID + ".autorestock.priorityExternalAutoCollect.enabled.tooltip"))
                .setSaveConsumer(value -> config.autoRestockPreferOverExternalAutoCollect = value)
                .build());

        autoRestock.addEntry(entryBuilder
                .startIntSlider(
                        Text.translatable("config." + ASTTweaks.MOD_ID + ".autorestock.operationsPerTick"),
                        config.autoRestockOperationsPerTick, 1, 32)
                .setDefaultValue(8)
                .setTooltip(Text.translatable("config." + ASTTweaks.MOD_ID + ".autorestock.operationsPerTick.tooltip"))
                .setTextGetter(value -> Text.literal(value + " ops"))
                .setSaveConsumer(value -> config.autoRestockOperationsPerTick = value)
                .build());

        autoRestock.addEntry(new ButtonEntry(
                Text.translatable("config." + ASTTweaks.MOD_ID + ".autorestock.itemlist.button"),
                button -> MinecraftClient.getInstance().setScreen(new AutoRestockItemListScreen(MinecraftClient.getInstance().currentScreen))
        ));

        autoRestock.addEntry(new KeyComboEntry(
                Text.translatable("config." + ASTTweaks.MOD_ID + ".keybind.autoRestockToggle"),
                config.autoRestockToggleKey,
                new KeyCombo(-1, -1),
                combo -> config.autoRestockToggleKey.copyFrom(combo)));

        autoRestock.addEntry(new KeyComboEntry(
                Text.translatable("config." + ASTTweaks.MOD_ID + ".keybind.autoRestockInventoryToggle"),
                config.autoRestockInventoryToggleKey,
                new KeyCombo(-1, -1),
                combo -> config.autoRestockInventoryToggleKey.copyFrom(combo)));

        autoRestock.addEntry(new KeyComboEntry(
                Text.translatable("config." + ASTTweaks.MOD_ID + ".keybind.autoRestockShulkerToggle"),
                config.autoRestockShulkerToggleKey,
                new KeyCombo(-1, -1),
                combo -> config.autoRestockShulkerToggleKey.copyFrom(combo)));

        // ============================
        // Villager link category
        // ============================
        ConfigCategory villagerLink = builder.getOrCreateCategory(
                Text.translatable("config." + ASTTweaks.MOD_ID + ".category.villagerlink"));

        villagerLink.addEntry(entryBuilder
                .startBooleanToggle(
                        Text.translatable("config." + ASTTweaks.MOD_ID + ".villagerlink.enabled"),
                        config.villagerLinkEnabled)
                .setDefaultValue(false)
                .setTooltip(Text.translatable("config." + ASTTweaks.MOD_ID + ".villagerlink.enabled.tooltip"))
                .setSaveConsumer(value -> config.villagerLinkEnabled = value)
                .build());

        villagerLink.addEntry(entryBuilder
                .startIntSlider(
                        Text.translatable("config." + ASTTweaks.MOD_ID + ".villagerlink.range"),
                        config.villagerLinkRange, 4, 128)
                .setDefaultValue(32)
                .setTooltip(Text.translatable("config." + ASTTweaks.MOD_ID + ".villagerlink.range.tooltip"))
                .setTextGetter(value -> Text.literal(value + " blocks"))
                .setSaveConsumer(value -> config.villagerLinkRange = value)
                .build());

        villagerLink.addEntry(entryBuilder
                .startAlphaColorField(
                        Text.translatable("config." + ASTTweaks.MOD_ID + ".villagerlink.lineColor"),
                        config.villagerLinkLineColor)
                .setDefaultValue(0xFF00FFFF)
                .setTooltip(Text.translatable("config." + ASTTweaks.MOD_ID + ".villagerlink.lineColor.tooltip"))
                .setSaveConsumer(value -> config.villagerLinkLineColor = value)
                .build());

        villagerLink.addEntry(entryBuilder
                .startBooleanToggle(
                        Text.translatable("config." + ASTTweaks.MOD_ID + ".villagerlink.seeThrough"),
                        config.villagerLinkSeeThrough)
                .setDefaultValue(true)
                .setTooltip(Text.translatable("config." + ASTTweaks.MOD_ID + ".villagerlink.seeThrough.tooltip"))
                .setSaveConsumer(value -> config.villagerLinkSeeThrough = value)
                .build());

        villagerLink.addEntry(new KeyComboEntry(
                Text.translatable("config." + ASTTweaks.MOD_ID + ".keybind.villagerLinkToggle"),
                config.villagerLinkToggleKey,
                new KeyCombo(-1, -1),
                combo -> config.villagerLinkToggleKey.copyFrom(combo)));

        builder.setSavingRunnable(config::save);

        return builder.build();
    }
}

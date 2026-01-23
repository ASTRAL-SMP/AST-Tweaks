package com.astral.asttweaks.config;

import com.astral.asttweaks.ASTTweaks;
import com.astral.asttweaks.feature.autoeat.gui.ButtonEntry;
import com.astral.asttweaks.feature.autoeat.gui.FoodListScreen;
import com.astral.asttweaks.feature.autoeat.gui.HungerBarEntry;
import com.astral.asttweaks.feature.automove.MoveDirection;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.gui.entries.IntegerSliderEntry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

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

        // Scoreboard category
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

        // Auto-eat category
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
                button -> MinecraftClient.getInstance().setScreen(new FoodListScreen(builder.build()))
        ));

        // Auto-move category
        ConfigCategory automove = builder.getOrCreateCategory(
                Text.translatable("config." + ASTTweaks.MOD_ID + ".category.automove"));

        automove.addEntry(entryBuilder
                .startBooleanToggle(
                        Text.translatable("config." + ASTTweaks.MOD_ID + ".automove.enabled"),
                        config.autoMoveEnabled)
                .setDefaultValue(true)
                .setTooltip(Text.translatable("config." + ASTTweaks.MOD_ID + ".automove.enabled.tooltip"))
                .setSaveConsumer(value -> config.autoMoveEnabled = value)
                .build());

        automove.addEntry(entryBuilder
                .startEnumSelector(
                        Text.translatable("config." + ASTTweaks.MOD_ID + ".automove.direction"),
                        MoveDirection.class,
                        config.autoMoveDirection)
                .setDefaultValue(MoveDirection.FORWARD)
                .setTooltip(Text.translatable("config." + ASTTweaks.MOD_ID + ".automove.direction.tooltip"))
                .setEnumNameProvider(dir -> Text.translatable("config." + ASTTweaks.MOD_ID + ".automove.direction." + ((MoveDirection)dir).getId()))
                .setSaveConsumer(value -> config.autoMoveDirection = value)
                .build());

        builder.setSavingRunnable(config::save);

        return builder.build();
    }
}

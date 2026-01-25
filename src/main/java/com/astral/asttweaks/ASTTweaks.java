package com.astral.asttweaks;

import com.astral.asttweaks.compat.TweakerooCompat;
import com.astral.asttweaks.config.ModConfig;
import com.astral.asttweaks.feature.FeatureManager;
import com.astral.asttweaks.util.KeyBindings;
import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ASTTweaks implements ClientModInitializer {
    public static final String MOD_ID = "asttweaks";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static ASTTweaks instance;

    @Override
    public void onInitializeClient() {
        instance = this;
        LOGGER.info("Initializing AST-Tweaks...");

        // Load configuration
        ModConfig.getInstance().load();

        // Register keybindings
        KeyBindings.register();

        // Initialize mod compatibility layers
        TweakerooCompat.init();

        // Initialize feature manager
        FeatureManager.getInstance().init();

        LOGGER.info("AST-Tweaks initialized successfully!");
    }

    public static ASTTweaks getInstance() {
        return instance;
    }
}

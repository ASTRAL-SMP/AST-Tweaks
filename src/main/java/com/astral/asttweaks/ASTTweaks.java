package com.astral.asttweaks;

import com.astral.asttweaks.compat.NvidiumCompat;
import com.astral.asttweaks.compat.TweakerooCompat;
import com.astral.asttweaks.config.ModConfig;
import com.astral.asttweaks.feature.FeatureManager;
import com.astral.asttweaks.feature.updatechecker.ResidualCleanup;
import com.astral.asttweaks.util.KeyBindings;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
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

        ResidualCleanup.run();

        // Load configuration
        ModConfig.getInstance().load();

        // Register keybindings
        KeyBindings.register();

        // Initialize mod compatibility layers
        TweakerooCompat.init();
        NvidiumCompat.init();

        // Initialize feature manager
        FeatureManager.getInstance().init();

        LOGGER.info("AST-Tweaks initialized successfully!");

        // 開発用: -Dasttweaks.mixinAudit=true（./gradlew runClient -PmixinAudit）で全 Mixin を強制適用して検証する
        if (Boolean.getBoolean("asttweaks.mixinAudit")) {
            ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
                LOGGER.info("Running Mixin audit...");
                org.spongepowered.asm.mixin.MixinEnvironment.getCurrentEnvironment().audit();
                LOGGER.info("Mixin audit finished");
            });
        }
    }

    public static ASTTweaks getInstance() {
        return instance;
    }
}

package com.astral.asttweaks.util;

import com.astral.asttweaks.ASTTweaks;
import com.astral.asttweaks.feature.FeatureManager;
import com.astral.asttweaks.feature.autoeat.AutoEatFeature;
import com.astral.asttweaks.feature.automove.AutoMoveFeature;
import com.astral.asttweaks.feature.scoreboard.ScoreboardFeature;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

/**
 * Manages keybindings for AST-Tweaks.
 */
public class KeyBindings {
    private static final String CATEGORY = "key.categories." + ASTTweaks.MOD_ID;

    public static KeyBinding scoreboardToggle;
    public static KeyBinding scoreboardPageUp;
    public static KeyBinding scoreboardPageDown;
    public static KeyBinding autoEatToggle;
    public static KeyBinding autoMoveToggle;

    public static void register() {
        // Toggle scoreboard visibility
        scoreboardToggle = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key." + ASTTweaks.MOD_ID + ".scoreboard.toggle",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_O,
                CATEGORY
        ));

        // Page up
        scoreboardPageUp = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key." + ASTTweaks.MOD_ID + ".scoreboard.pageUp",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_UP,
                CATEGORY
        ));

        // Page down
        scoreboardPageDown = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key." + ASTTweaks.MOD_ID + ".scoreboard.pageDown",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_DOWN,
                CATEGORY
        ));

        // Toggle auto-eat
        autoEatToggle = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key." + ASTTweaks.MOD_ID + ".autoeat.toggle",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN,
                CATEGORY
        ));

        // Toggle auto-move
        autoMoveToggle = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key." + ASTTweaks.MOD_ID + ".automove.toggle",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN,
                CATEGORY
        ));

        // Register tick handler for keybindings
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            ScoreboardFeature scoreboard = FeatureManager.getInstance().getScoreboardFeature();
            if (scoreboard == null) return;

            while (scoreboardToggle.wasPressed()) {
                scoreboard.toggleVisibility();
            }

            while (scoreboardPageUp.wasPressed()) {
                scoreboard.pageUp();
            }

            while (scoreboardPageDown.wasPressed()) {
                scoreboard.pageDown();
            }

            // Auto-eat toggle
            AutoEatFeature autoEat = FeatureManager.getInstance().getAutoEatFeature();
            if (autoEat != null) {
                while (autoEatToggle.wasPressed()) {
                    boolean newState = !autoEat.isEnabled();
                    autoEat.setEnabled(newState);
                    if (client.player != null) {
                        client.player.sendMessage(
                            net.minecraft.text.Text.translatable(
                                "message." + ASTTweaks.MOD_ID + ".autoeat." + (newState ? "enabled" : "disabled")
                            ),
                            true
                        );
                    }
                }
            }

            // Auto-move toggle
            AutoMoveFeature autoMove = FeatureManager.getInstance().getAutoMoveFeature();
            if (autoMove != null) {
                while (autoMoveToggle.wasPressed()) {
                    autoMove.toggle();
                    if (client.player != null) {
                        client.player.sendMessage(
                            net.minecraft.text.Text.translatable(
                                "message." + ASTTweaks.MOD_ID + ".automove." +
                                (autoMove.isMoving() ? "enabled" : "disabled")
                            ),
                            true
                        );
                    }
                }
            }
        });
    }
}

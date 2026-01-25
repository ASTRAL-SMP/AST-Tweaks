package com.astral.asttweaks.util;

import com.astral.asttweaks.ASTTweaks;
import com.astral.asttweaks.feature.FeatureManager;
import com.astral.asttweaks.feature.autoeat.AutoEatFeature;
import com.astral.asttweaks.feature.automove.AutoMoveFeature;
import com.astral.asttweaks.feature.autorepair.AutoRepairFeature;
import com.astral.asttweaks.feature.autototem.AutoTotemFeature;
import com.astral.asttweaks.feature.massgrindstone.MassGrindstoneFeature;
import com.astral.asttweaks.feature.notepad.NotepadFeature;
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
    public static KeyBinding autoTotemToggle;
    public static KeyBinding autoRepairToggle;
    public static KeyBinding notepadOpen;
    public static KeyBinding massGrindstoneExecute;

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

        // Toggle auto-totem
        autoTotemToggle = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key." + ASTTweaks.MOD_ID + ".autototem.toggle",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN,
                CATEGORY
        ));

        // Toggle auto-repair
        autoRepairToggle = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key." + ASTTweaks.MOD_ID + ".autorepair.toggle",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN,
                CATEGORY
        ));

        // Open notepad
        notepadOpen = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key." + ASTTweaks.MOD_ID + ".notepad.open",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN,
                CATEGORY
        ));

        // Mass grindstone execute
        massGrindstoneExecute = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key." + ASTTweaks.MOD_ID + ".massgrindstone.execute",
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

            // Auto-totem toggle
            AutoTotemFeature autoTotem = FeatureManager.getInstance().getAutoTotemFeature();
            if (autoTotem != null) {
                while (autoTotemToggle.wasPressed()) {
                    boolean newState = !autoTotem.isEnabled();
                    autoTotem.setEnabled(newState);
                    if (client.player != null) {
                        client.player.sendMessage(
                            net.minecraft.text.Text.translatable(
                                "message." + ASTTweaks.MOD_ID + ".autototem." + (newState ? "enabled" : "disabled")
                            ),
                            true
                        );
                    }
                }
            }

            // Auto-repair toggle
            AutoRepairFeature autoRepair = FeatureManager.getInstance().getAutoRepairFeature();
            if (autoRepair != null) {
                while (autoRepairToggle.wasPressed()) {
                    boolean newState = !autoRepair.isEnabled();
                    autoRepair.setEnabled(newState);
                    if (client.player != null) {
                        client.player.sendMessage(
                            net.minecraft.text.Text.translatable(
                                "message." + ASTTweaks.MOD_ID + ".autorepair." + (newState ? "enabled" : "disabled")
                            ),
                            true
                        );
                    }
                }
            }

            // Notepad open
            NotepadFeature notepad = FeatureManager.getInstance().getNotepadFeature();
            if (notepad != null) {
                while (notepadOpen.wasPressed()) {
                    notepad.openNotepad();
                }
            }

            // Note: Mass grindstone key handling is done in MassGrindstoneFeature
            // because wasPressed() doesn't work while GUI screens are open.
            // We use InputUtil.isKeyPressed() directly in the feature class.
        });
    }
}

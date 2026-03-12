package com.astral.asttweaks.util;

import com.astral.asttweaks.ASTTweaks;
import com.astral.asttweaks.config.ConfigScreen;
import com.astral.asttweaks.config.ModConfig;
import com.astral.asttweaks.feature.FeatureManager;
import com.astral.asttweaks.feature.autoeat.AutoEatFeature;
import com.astral.asttweaks.feature.automove.AutoMoveFeature;
import com.astral.asttweaks.feature.autorepair.AutoRepairFeature;
import com.astral.asttweaks.feature.autototem.AutoTotemFeature;
import com.astral.asttweaks.feature.bonemealfilter.BoneMealFilterFeature;
import com.astral.asttweaks.feature.notepad.NotepadFeature;
import com.astral.asttweaks.feature.scoreboard.ScoreboardFeature;
import com.astral.asttweaks.feature.silktouchswitch.SilkTouchSwitchFeature;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

/**
 * キーバインド管理。
 * 全キーバインドをmod menu内のカスタムKeyComboで管理。
 * MC側のキー設定には一切登録しない。
 */
public class KeyBindings {

    // エッジ検出用の前フレーム状態
    private static boolean wasScoreboardToggleDown = false;
    private static boolean wasScoreboardPageUpDown = false;
    private static boolean wasScoreboardPageDownDown = false;
    private static boolean wasAutoEatToggleDown = false;
    private static boolean wasAutoMoveToggleDown = false;
    private static boolean wasAutoTotemToggleDown = false;
    private static boolean wasAutoRepairToggleDown = false;
    private static boolean wasBoneMealFilterToggleDown = false;
    private static boolean wasSilkTouchSwitchToggleDown = false;
    private static boolean wasNotepadOpenDown = false;
    private static boolean wasOpenGeneralScreenDown = false;

    public static void register() {
        // ティックハンドラ登録（全キーバインドをKeyCombo+エッジ検出で処理）
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;
            long window = client.getWindow().getHandle();
            ModConfig config = ModConfig.getInstance();

            // GUI画面が開いているときはカスタムキーコンボを処理しない
            // （massGrindstone, inventorySort は各Featureクラス内で直接処理）
            if (client.currentScreen != null) return;

            // スコアボードトグル
            boolean scoreboardToggleDown = config.scoreboardToggleKey.isPressed(window);
            if (scoreboardToggleDown && !wasScoreboardToggleDown) {
                ScoreboardFeature scoreboard = FeatureManager.getInstance().getScoreboardFeature();
                if (scoreboard != null) {
                    scoreboard.toggleVisibility();
                }
            }
            wasScoreboardToggleDown = scoreboardToggleDown;

            // スコアボード上スクロール
            boolean pageUpDown = config.scoreboardPageUpKey.isPressed(window);
            if (pageUpDown && !wasScoreboardPageUpDown) {
                ScoreboardFeature scoreboard = FeatureManager.getInstance().getScoreboardFeature();
                if (scoreboard != null) {
                    scoreboard.pageUp();
                }
            }
            wasScoreboardPageUpDown = pageUpDown;

            // スコアボード下スクロール
            boolean pageDownDown = config.scoreboardPageDownKey.isPressed(window);
            if (pageDownDown && !wasScoreboardPageDownDown) {
                ScoreboardFeature scoreboard = FeatureManager.getInstance().getScoreboardFeature();
                if (scoreboard != null) {
                    scoreboard.pageDown();
                }
            }
            wasScoreboardPageDownDown = pageDownDown;

            // 自動食事トグル
            boolean autoEatDown = config.autoEatToggleKey.isPressed(window);
            if (autoEatDown && !wasAutoEatToggleDown) {
                AutoEatFeature autoEat = FeatureManager.getInstance().getAutoEatFeature();
                if (autoEat != null) {
                    boolean newState = !autoEat.isEnabled();
                    autoEat.setEnabled(newState);
                    client.player.sendMessage(
                            Text.translatable("message." + ASTTweaks.MOD_ID + ".autoeat." + (newState ? "enabled" : "disabled")),
                            true);
                }
            }
            wasAutoEatToggleDown = autoEatDown;

            // 自動移動トグル
            boolean autoMoveDown = config.autoMoveToggleKey.isPressed(window);
            if (autoMoveDown && !wasAutoMoveToggleDown) {
                AutoMoveFeature autoMove = FeatureManager.getInstance().getAutoMoveFeature();
                if (autoMove != null) {
                    autoMove.toggle();
                    client.player.sendMessage(
                            Text.translatable("message." + ASTTweaks.MOD_ID + ".automove." + (autoMove.isMoving() ? "enabled" : "disabled")),
                            true);
                }
            }
            wasAutoMoveToggleDown = autoMoveDown;

            // 自動トーテムトグル
            boolean autoTotemDown = config.autoTotemToggleKey.isPressed(window);
            if (autoTotemDown && !wasAutoTotemToggleDown) {
                AutoTotemFeature autoTotem = FeatureManager.getInstance().getAutoTotemFeature();
                if (autoTotem != null) {
                    boolean newState = !autoTotem.isEnabled();
                    autoTotem.setEnabled(newState);
                    client.player.sendMessage(
                            Text.translatable("message." + ASTTweaks.MOD_ID + ".autototem." + (newState ? "enabled" : "disabled")),
                            true);
                }
            }
            wasAutoTotemToggleDown = autoTotemDown;

            // 自動修繕トグル
            boolean autoRepairDown = config.autoRepairToggleKey.isPressed(window);
            if (autoRepairDown && !wasAutoRepairToggleDown) {
                AutoRepairFeature autoRepair = FeatureManager.getInstance().getAutoRepairFeature();
                if (autoRepair != null) {
                    boolean newState = !autoRepair.isEnabled();
                    autoRepair.setEnabled(newState);
                    client.player.sendMessage(
                            Text.translatable("message." + ASTTweaks.MOD_ID + ".autorepair." + (newState ? "enabled" : "disabled")),
                            true);
                }
            }
            wasAutoRepairToggleDown = autoRepairDown;

            // 骨粉フィルタートグル
            boolean boneMealDown = config.boneMealFilterToggleKey.isPressed(window);
            if (boneMealDown && !wasBoneMealFilterToggleDown) {
                BoneMealFilterFeature boneMealFilter = FeatureManager.getInstance().getBoneMealFilterFeature();
                if (boneMealFilter != null) {
                    boolean newState = !boneMealFilter.isEnabled();
                    boneMealFilter.setEnabled(newState);
                    client.player.sendMessage(
                            Text.translatable("message." + ASTTweaks.MOD_ID + ".bonemealfilter." + (newState ? "enabled" : "disabled")),
                            true);
                }
            }
            wasBoneMealFilterToggleDown = boneMealDown;

            // シルクタッチスイッチトグル
            boolean silkTouchDown = config.silkTouchSwitchToggleKey.isPressed(window);
            if (silkTouchDown && !wasSilkTouchSwitchToggleDown) {
                SilkTouchSwitchFeature silkTouchSwitch = FeatureManager.getInstance().getSilkTouchSwitchFeature();
                if (silkTouchSwitch != null) {
                    boolean newState = !silkTouchSwitch.isEnabled();
                    silkTouchSwitch.setEnabled(newState);
                    client.player.sendMessage(
                            Text.translatable("message." + ASTTweaks.MOD_ID + ".silktouchswitch." + (newState ? "enabled" : "disabled")),
                            true);
                }
            }
            wasSilkTouchSwitchToggleDown = silkTouchDown;

            // メモ帳を開く
            boolean notepadDown = config.notepadOpenKey.isPressed(window);
            if (notepadDown && !wasNotepadOpenDown) {
                NotepadFeature notepad = FeatureManager.getInstance().getNotepadFeature();
                if (notepad != null) {
                    notepad.openNotepad();
                }
            }
            wasNotepadOpenDown = notepadDown;

            // 一般設定画面を開く（K+L デフォルト）
            boolean openGeneralDown = config.openGeneralScreenKey.isPressed(window);
            if (openGeneralDown && !wasOpenGeneralScreenDown) {
                client.setScreen(ConfigScreen.createClothConfigScreen(null));
            }
            wasOpenGeneralScreenDown = openGeneralDown;

            // Note: Mass grindstone key handling is done in MassGrindstoneFeature
            // because it needs to work while GUI screens are open.

            // Note: Inventory sort key handling is done in InventorySortFeature
            // because it needs to work while GUI screens are open.
        });
    }
}

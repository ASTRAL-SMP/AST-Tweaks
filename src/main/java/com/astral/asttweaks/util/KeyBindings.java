package com.astral.asttweaks.util;

import com.astral.asttweaks.ASTTweaks;
import com.astral.asttweaks.config.ConfigScreen;
import com.astral.asttweaks.config.ModConfig;
import com.astral.asttweaks.feature.FeatureManager;
import com.astral.asttweaks.feature.autoeat.AutoEatFeature;
import com.astral.asttweaks.feature.automove.AutoMoveFeature;
import com.astral.asttweaks.feature.automove.MoveDirection;
import com.astral.asttweaks.feature.autodrop.AutoDropFeature;
import com.astral.asttweaks.feature.autorepair.AutoRepairFeature;
import com.astral.asttweaks.feature.autorestock.AutoRestockFeature;
import com.astral.asttweaks.feature.autototem.AutoTotemFeature;
import com.astral.asttweaks.feature.bonemealfilter.BoneMealFilterFeature;
import com.astral.asttweaks.feature.notepad.NotepadFeature;
import com.astral.asttweaks.feature.scoreboard.ScoreboardFeature;
import com.astral.asttweaks.feature.mousesensitivity.MouseSensitivityFeature;
import com.astral.asttweaks.feature.silktouchswitch.SilkTouchSwitchFeature;
import com.astral.asttweaks.feature.villagerlink.VillagerLinkFeature;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

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
    private static boolean wasAutoMoveForwardDown = false;
    private static boolean wasAutoMoveBackwardDown = false;
    private static boolean wasAutoMoveLeftDown = false;
    private static boolean wasAutoMoveRightDown = false;
    private static boolean wasAutoMoveForwardLeftDown = false;
    private static boolean wasAutoMoveForwardRightDown = false;
    private static boolean wasAutoMoveBackwardLeftDown = false;
    private static boolean wasAutoMoveBackwardRightDown = false;
    private static boolean wasAutoTotemToggleDown = false;
    private static boolean wasAutoRepairToggleDown = false;
    private static boolean wasBoneMealFilterToggleDown = false;
    private static boolean wasSilkTouchSwitchToggleDown = false;
    private static boolean wasNotepadOpenDown = false;
    private static boolean wasMouseSensitivityToggleDown = false;
    private static boolean wasOpenGeneralScreenDown = false;
    private static boolean wasAutoDropToggleDown = false;
    private static boolean wasAutoRestockToggleDown = false;
    private static boolean wasAutoRestockInventoryToggleDown = false;
    private static boolean wasAutoRestockShulkerToggleDown = false;
    private static boolean wasVillagerLinkToggleDown = false;

    public static void register() {
        // ティックハンドラ登録（全キーバインドをKeyCombo+エッジ検出で処理）
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;
            ModConfig config = ModConfig.getInstance();

            // GUI画面が開いているときはカスタムキーコンボを処理しない
            // （massGrindstone, inventorySort は各Featureクラス内で直接処理）
            if (McClient.screen() != null) return;

            // スコアボードトグル
            boolean scoreboardToggleDown = config.scoreboardToggleKey.isPressed();
            if (scoreboardToggleDown && !wasScoreboardToggleDown) {
                ScoreboardFeature scoreboard = FeatureManager.getInstance().getScoreboardFeature();
                if (scoreboard != null) {
                    scoreboard.toggleVisibility();
                }
            }
            wasScoreboardToggleDown = scoreboardToggleDown;

            // スコアボード上スクロール
            boolean pageUpDown = config.scoreboardPageUpKey.isPressed();
            if (pageUpDown && !wasScoreboardPageUpDown) {
                ScoreboardFeature scoreboard = FeatureManager.getInstance().getScoreboardFeature();
                if (scoreboard != null) {
                    scoreboard.pageUp();
                }
            }
            wasScoreboardPageUpDown = pageUpDown;

            // スコアボード下スクロール
            boolean pageDownDown = config.scoreboardPageDownKey.isPressed();
            if (pageDownDown && !wasScoreboardPageDownDown) {
                ScoreboardFeature scoreboard = FeatureManager.getInstance().getScoreboardFeature();
                if (scoreboard != null) {
                    scoreboard.pageDown();
                }
            }
            wasScoreboardPageDownDown = pageDownDown;

            // 自動食事トグル
            boolean autoEatDown = config.autoEatToggleKey.isPressed();
            if (autoEatDown && !wasAutoEatToggleDown) {
                AutoEatFeature autoEat = FeatureManager.getInstance().getAutoEatFeature();
                if (autoEat != null) {
                    boolean newState = !autoEat.isEnabled();
                    autoEat.setEnabled(newState);
                    McClient.overlay(client.player, 
                            Component.translatable("message." + ASTTweaks.MOD_ID + ".autoeat." + (newState ? "enabled" : "disabled")));
                }
            }
            wasAutoEatToggleDown = autoEatDown;

            // 自動移動トグル
            boolean autoMoveDown = config.autoMoveToggleKey.isPressed();
            if (autoMoveDown && !wasAutoMoveToggleDown) {
                AutoMoveFeature autoMove = FeatureManager.getInstance().getAutoMoveFeature();
                if (autoMove != null) {
                    autoMove.toggle();
                    McClient.overlay(client.player, 
                            Component.translatable("message." + ASTTweaks.MOD_ID + ".automove." + (autoMove.isMoving() ? "enabled" : "disabled")));
                }
            }
            wasAutoMoveToggleDown = autoMoveDown;

            // 自動移動 方向別トグル
            handleAutoMoveDirection(client, config, MoveDirection.FORWARD,
                    config.autoMoveForwardKey, wasAutoMoveForwardDown);
            wasAutoMoveForwardDown = config.autoMoveForwardKey.isPressed();

            handleAutoMoveDirection(client, config, MoveDirection.BACKWARD,
                    config.autoMoveBackwardKey, wasAutoMoveBackwardDown);
            wasAutoMoveBackwardDown = config.autoMoveBackwardKey.isPressed();

            handleAutoMoveDirection(client, config, MoveDirection.LEFT,
                    config.autoMoveLeftKey, wasAutoMoveLeftDown);
            wasAutoMoveLeftDown = config.autoMoveLeftKey.isPressed();

            handleAutoMoveDirection(client, config, MoveDirection.RIGHT,
                    config.autoMoveRightKey, wasAutoMoveRightDown);
            wasAutoMoveRightDown = config.autoMoveRightKey.isPressed();

            handleAutoMoveDirection(client, config, MoveDirection.FORWARD_LEFT,
                    config.autoMoveForwardLeftKey, wasAutoMoveForwardLeftDown);
            wasAutoMoveForwardLeftDown = config.autoMoveForwardLeftKey.isPressed();

            handleAutoMoveDirection(client, config, MoveDirection.FORWARD_RIGHT,
                    config.autoMoveForwardRightKey, wasAutoMoveForwardRightDown);
            wasAutoMoveForwardRightDown = config.autoMoveForwardRightKey.isPressed();

            handleAutoMoveDirection(client, config, MoveDirection.BACKWARD_LEFT,
                    config.autoMoveBackwardLeftKey, wasAutoMoveBackwardLeftDown);
            wasAutoMoveBackwardLeftDown = config.autoMoveBackwardLeftKey.isPressed();

            handleAutoMoveDirection(client, config, MoveDirection.BACKWARD_RIGHT,
                    config.autoMoveBackwardRightKey, wasAutoMoveBackwardRightDown);
            wasAutoMoveBackwardRightDown = config.autoMoveBackwardRightKey.isPressed();

            // 自動トーテムトグル
            boolean autoTotemDown = config.autoTotemToggleKey.isPressed();
            if (autoTotemDown && !wasAutoTotemToggleDown) {
                AutoTotemFeature autoTotem = FeatureManager.getInstance().getAutoTotemFeature();
                if (autoTotem != null) {
                    boolean newState = !autoTotem.isEnabled();
                    autoTotem.setEnabled(newState);
                    McClient.overlay(client.player, 
                            Component.translatable("message." + ASTTweaks.MOD_ID + ".autototem." + (newState ? "enabled" : "disabled")));
                }
            }
            wasAutoTotemToggleDown = autoTotemDown;

            // 自動修繕トグル
            boolean autoRepairDown = config.autoRepairToggleKey.isPressed();
            if (autoRepairDown && !wasAutoRepairToggleDown) {
                AutoRepairFeature autoRepair = FeatureManager.getInstance().getAutoRepairFeature();
                if (autoRepair != null) {
                    boolean newState = !autoRepair.isEnabled();
                    autoRepair.setEnabled(newState);
                    McClient.overlay(client.player, 
                            Component.translatable("message." + ASTTweaks.MOD_ID + ".autorepair." + (newState ? "enabled" : "disabled")));
                }
            }
            wasAutoRepairToggleDown = autoRepairDown;

            // 骨粉フィルタートグル
            boolean boneMealDown = config.boneMealFilterToggleKey.isPressed();
            if (boneMealDown && !wasBoneMealFilterToggleDown) {
                BoneMealFilterFeature boneMealFilter = FeatureManager.getInstance().getBoneMealFilterFeature();
                if (boneMealFilter != null) {
                    boolean newState = !boneMealFilter.isEnabled();
                    boneMealFilter.setEnabled(newState);
                    McClient.overlay(client.player, 
                            Component.translatable("message." + ASTTweaks.MOD_ID + ".bonemealfilter." + (newState ? "enabled" : "disabled")));
                }
            }
            wasBoneMealFilterToggleDown = boneMealDown;

            // シルクタッチスイッチトグル
            boolean silkTouchDown = config.silkTouchSwitchToggleKey.isPressed();
            if (silkTouchDown && !wasSilkTouchSwitchToggleDown) {
                SilkTouchSwitchFeature silkTouchSwitch = FeatureManager.getInstance().getSilkTouchSwitchFeature();
                if (silkTouchSwitch != null) {
                    boolean newState = !silkTouchSwitch.isEnabled();
                    silkTouchSwitch.setEnabled(newState);
                    McClient.overlay(client.player, 
                            Component.translatable("message." + ASTTweaks.MOD_ID + ".silktouchswitch." + (newState ? "enabled" : "disabled")));
                }
            }
            wasSilkTouchSwitchToggleDown = silkTouchDown;

            // メモ帳を開く
            boolean notepadDown = config.notepadOpenKey.isPressed();
            if (notepadDown && !wasNotepadOpenDown) {
                NotepadFeature notepad = FeatureManager.getInstance().getNotepadFeature();
                if (notepad != null) {
                    notepad.openNotepad();
                }
            }
            wasNotepadOpenDown = notepadDown;

            // マウス感度トグル
            boolean mouseSensDown = config.mouseSensitivityToggleKey.isPressed();
            if (mouseSensDown && !wasMouseSensitivityToggleDown) {
                MouseSensitivityFeature mouseSens = FeatureManager.getInstance().getMouseSensitivityFeature();
                if (mouseSens != null && mouseSens.isEnabled()) {
                    int sensitivity = mouseSens.toggle();
                    if (mouseSens.isToggled()) {
                        McClient.overlay(client.player, 
                                Component.translatable("message." + ASTTweaks.MOD_ID + ".mousesensitivity.activated", sensitivity));
                    } else {
                        McClient.overlay(client.player, 
                                Component.translatable("message." + ASTTweaks.MOD_ID + ".mousesensitivity.deactivated", sensitivity));
                    }
                }
            }
            wasMouseSensitivityToggleDown = mouseSensDown;

            // 自動ドロップトグル
            boolean autoDropDown = config.autoDropToggleKey.isPressed();
            if (autoDropDown && !wasAutoDropToggleDown) {
                AutoDropFeature autoDrop = FeatureManager.getInstance().getAutoDropFeature();
                if (autoDrop != null) {
                    boolean newState = !autoDrop.isEnabled();
                    autoDrop.setEnabled(newState);
                    McClient.overlay(client.player, 
                            Component.translatable("message." + ASTTweaks.MOD_ID + ".autodrop." + (newState ? "enabled" : "disabled")));
                }
            }
            wasAutoDropToggleDown = autoDropDown;

            // 自動補充トグル
            boolean autoRestockDown = config.autoRestockToggleKey.isPressed();
            if (autoRestockDown && !wasAutoRestockToggleDown) {
                AutoRestockFeature autoRestock = FeatureManager.getInstance().getAutoRestockFeature();
                if (autoRestock != null) {
                    boolean newState = !autoRestock.isEnabled();
                    autoRestock.setEnabled(newState);
                    McClient.overlay(client.player, 
                            Component.translatable("message." + ASTTweaks.MOD_ID + ".autorestock." + (newState ? "enabled" : "disabled")));
                }
            }
            wasAutoRestockToggleDown = autoRestockDown;

            // インベントリからの自動補充トグル
            boolean autoRestockInventoryDown = config.autoRestockInventoryToggleKey.isPressed();
            if (autoRestockInventoryDown && !wasAutoRestockInventoryToggleDown) {
                AutoRestockFeature autoRestock = FeatureManager.getInstance().getAutoRestockFeature();
                if (autoRestock != null) {
                    boolean newState = !autoRestock.getConfig().isInventoryRestockEnabled();
                    autoRestock.getConfig().setInventoryRestockEnabled(newState);
                    McClient.overlay(client.player, 
                            Component.translatable("message." + ASTTweaks.MOD_ID + ".autorestock.inventory." + (newState ? "enabled" : "disabled")));
                }
            }
            wasAutoRestockInventoryToggleDown = autoRestockInventoryDown;

            // 開いているシュルカーからの自動補充トグル
            boolean autoRestockShulkerDown = config.autoRestockShulkerToggleKey.isPressed();
            if (autoRestockShulkerDown && !wasAutoRestockShulkerToggleDown) {
                AutoRestockFeature autoRestock = FeatureManager.getInstance().getAutoRestockFeature();
                if (autoRestock != null) {
                    boolean newState = !autoRestock.getConfig().isShulkerRestockEnabled();
                    autoRestock.getConfig().setShulkerRestockEnabled(newState);
                    McClient.overlay(client.player, 
                            Component.translatable("message." + ASTTweaks.MOD_ID + ".autorestock.shulker." + (newState ? "enabled" : "disabled")));
                }
            }
            wasAutoRestockShulkerToggleDown = autoRestockShulkerDown;

            // 村人リンクトグル
            boolean villagerLinkDown = config.villagerLinkToggleKey.isPressed();
            if (villagerLinkDown && !wasVillagerLinkToggleDown) {
                VillagerLinkFeature villagerLink = FeatureManager.getInstance().getVillagerLinkFeature();
                if (villagerLink != null) {
                    boolean newState = !villagerLink.isEnabled();
                    villagerLink.setEnabled(newState);
                    McClient.overlay(client.player, 
                            Component.translatable("message." + ASTTweaks.MOD_ID + ".villagerlink." + (newState ? "enabled" : "disabled")));
                }
            }
            wasVillagerLinkToggleDown = villagerLinkDown;

            // 一般設定画面を開く（K+L デフォルト）
            boolean openGeneralDown = config.openGeneralScreenKey.isPressed();
            if (openGeneralDown && !wasOpenGeneralScreenDown) {
                McClient.setScreen(ConfigScreen.createClothConfigScreen(null));
            }
            wasOpenGeneralScreenDown = openGeneralDown;

            // Note: Mass grindstone key handling is done in MassGrindstoneFeature
            // because it needs to work while GUI screens are open.

            // Note: Inventory sort key handling is done in InventorySortFeature
            // because it needs to work while GUI screens are open.
        });
    }

    private static void handleAutoMoveDirection(Minecraft client, ModConfig config,
            MoveDirection direction, KeyCombo keyCombo, boolean wasDown) {
        boolean isDown = keyCombo.isPressed();
        if (isDown && !wasDown) {
            AutoMoveFeature autoMove = FeatureManager.getInstance().getAutoMoveFeature();
            if (autoMove != null) {
                autoMove.toggleWithDirection(direction);
                String dirKey = "config." + ASTTweaks.MOD_ID + ".automove.direction." + direction.getId();
                if (autoMove.isMoving()) {
                    McClient.overlay(client.player, 
                            Component.translatable("message." + ASTTweaks.MOD_ID + ".automove.enabled.direction",
                                    Component.translatable(dirKey)));
                } else {
                    McClient.overlay(client.player, 
                            Component.translatable("message." + ASTTweaks.MOD_ID + ".automove.disabled"));
                }
            }
        }
    }
}

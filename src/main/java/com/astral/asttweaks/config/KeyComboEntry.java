package com.astral.asttweaks.config;

import com.astral.asttweaks.util.KeyCombo;
import com.mojang.blaze3d.platform.InputConstants;
import me.shedaniel.clothconfig2.gui.entries.TooltipListEntry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
//? if >=26.1 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
//?} else if >=1.20 {
/*import net.minecraft.client.gui.GuiGraphics;
*///?} else {
/*import com.mojang.blaze3d.vertex.PoseStack;
*///?}
import com.astral.asttweaks.util.gui.Gfx;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Cloth Config のカスタムウィジェット。
 * 任意の2キーコンボ（例: K+L）をキャプチャできる。
 *
 * 操作フロー:
 *   クリック → リスニングモード
 *   1キー目を押して離す → "K + ?" 表示、2キー目待ち
 *   2キー目を押す → コンボ確定（K + L）
 *   Enter を押す → 単キー確定
 *   ESC → 未割り当て (None) に設定 (MC vanilla の controls 画面互換)
 */
public class KeyComboEntry extends TooltipListEntry<KeyCombo> {
    private final KeyCombo value;
    private final KeyCombo defaultValue;
    private final KeyCombo originalValue; // 画面オープン時の値（isEdited比較用）
    private final Consumer<KeyCombo> saveConsumer;
    private final Button bindButton;
    private final Button resetButton;
    private boolean listening = false;
    private int firstKey = -1;               // 1キー目（押して離した後に記録）
    private String firstKeyType = KeyCombo.TYPE_KEY; // 1キー目の種別 ("key" or "mouse")
    private boolean waitingRelease = false;  // 1キー目の離しを待っている状態

    // リスニング中のエントリ数。0 でない間は Screen.shouldCloseOnEsc を false に倒し、
    // ESC を画面クローズではなく当エントリの keyPressed まで届かせる (ScreenMixin が参照)。
    private static int activeListenerCount = 0;

    public static boolean isAnyListening() {
        return activeListenerCount > 0;
    }

    public KeyComboEntry(Component fieldName, KeyCombo currentValue, KeyCombo defaultValue, Consumer<KeyCombo> saveConsumer) {
        super(fieldName, null);
        this.value = currentValue.copy();
        this.defaultValue = defaultValue.copy();
        this.originalValue = currentValue.copy();
        this.saveConsumer = saveConsumer;

        this.bindButton = Button.builder(Component.literal(value.getDisplayName()), button -> {
            if (!listening) {
                activeListenerCount++;
            }
            listening = true;
            firstKey = -1;
            firstKeyType = KeyCombo.TYPE_KEY;
            waitingRelease = false;
            updateButtonText();
        }).build();
        // ホバー時に操作説明（特に ESC=未割り当て）が見えるようにする
        this.bindButton.setTooltip(Tooltip.create(Component.translatable("config.asttweaks.keybind.bindButton.tooltip")));

        this.resetButton = Button.builder(Component.translatable("controls.reset"), button -> {
            value.copyFrom(defaultValue);
            // リスニング中にリセットされた場合はカウンタも巻き戻す
            if (listening) {
                activeListenerCount = Math.max(0, activeListenerCount - 1);
            }
            listening = false;
            firstKey = -1;
            firstKeyType = KeyCombo.TYPE_KEY;
            waitingRelease = false;
            updateButtonText();
        }).build();

        updateButtonText();
    }

    private void updateButtonText() {
        if (listening) {
            if (firstKey != -1) {
                // 1キー目確定済み — 2キー目 or Enter 待ち
                this.bindButton.setMessage(
                        Component.literal(KeyCombo.getInputName(firstKey, firstKeyType) + " + ?")
                                .withStyle(ChatFormatting.YELLOW));
            } else {
                // ESC で未割り当てにできる旨を、リスニングモードのプロンプト上で常時掲示する（機能の発見性向上）
                this.bindButton.setMessage(
                        Component.translatable("config.asttweaks.keybind.listening_prompt").withStyle(ChatFormatting.YELLOW));
            }
        } else if (value.mainKey == -1) {
            // 未割り当て状態を視認しやすく
            this.bindButton.setMessage(
                    Component.translatable("config.asttweaks.keybind.unbound").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        } else {
            this.bindButton.setMessage(Component.literal(value.getDisplayName()));
        }
    }

    //? if >=26.1 {
    @Override
    public boolean keyPressed(KeyEvent event) {
        return onKeyPressed(event.key());
    }
    //?} else {
    /*@Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return onKeyPressed(keyCode);
    }
    *///?}

    private boolean onKeyPressed(int keyCode) {
        if (!listening) return false;

        if (keyCode == InputConstants.KEY_ESCAPE) {
            // ESC で未割り当て (None) に設定
            value.mainKey = -1;
            value.mainKeyType = KeyCombo.TYPE_KEY;
            value.modifierKey = -1;
            value.modifierKeyType = KeyCombo.TYPE_KEY;
            finishListening();
            return true;
        }

        if (firstKey == -1) {
            // 1キー目を押した — まだ離していないので離しを待つ
            firstKey = keyCode;
            firstKeyType = KeyCombo.TYPE_KEY;
            waitingRelease = true;
            updateButtonText();
            return true;
        } else {
            if (keyCode == InputConstants.KEY_RETURN || keyCode == InputConstants.KEY_NUMPADENTER) {
                // Enter → 単キーバインドとして確定
                value.mainKey = firstKey;
                value.mainKeyType = firstKeyType;
                value.modifierKey = -1;
                value.modifierKeyType = KeyCombo.TYPE_KEY;
            } else {
                // 2キー目 → コンボ確定
                value.modifierKey = firstKey;
                value.modifierKeyType = firstKeyType;
                value.mainKey = keyCode;
                value.mainKeyType = KeyCombo.TYPE_KEY;
            }
            finishListening();
            return true;
        }
    }

    //? if >=26.1 {
    @Override
    public boolean keyReleased(KeyEvent event) {
        return onKeyReleased(event.key());
    }
    //?} else {
    /*@Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        return onKeyReleased(keyCode);
    }
    *///?}

    private boolean onKeyReleased(int keyCode) {
        if (!listening) return false;

        // 1キー目の離しを検知 — waitingRelease を解除するだけ
        // （2キー目の入力を引き続き待つ）
        if (waitingRelease && firstKey != -1 && KeyCombo.TYPE_KEY.equals(firstKeyType) && keyCode == firstKey) {
            waitingRelease = false;
            return true;
        }
        return false;
    }

    //? if >=26.1 {
    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (listening) return onMouseClicked(event.button());
        return super.mouseClicked(event, doubleClick);
    }
    //?} else {
    /*@Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (listening) return onMouseClicked(button);
        return super.mouseClicked(mouseX, mouseY, button);
    }
    *///?}

    private boolean onMouseClicked(int button) {
        if (firstKey == -1) {
            // 1キー目をマウスボタンとして記録
            firstKey = button;
            firstKeyType = KeyCombo.TYPE_MOUSE;
            waitingRelease = true;
            updateButtonText();
        } else {
            // 2キー目としてマウスボタンを記録
            value.modifierKey = firstKey;
            value.modifierKeyType = firstKeyType;
            value.mainKey = button;
            value.mainKeyType = KeyCombo.TYPE_MOUSE;
            finishListening();
        }
        return true;
    }

    //? if >=26.1 {
    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (listening) return onMouseReleased(event.button());
        return super.mouseReleased(event);
    }
    //?} else {
    /*@Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (listening) return onMouseReleased(button);
        return super.mouseReleased(mouseX, mouseY, button);
    }
    *///?}

    private boolean onMouseReleased(int button) {
        // 1キー目がマウスだった場合の離し検知
        if (waitingRelease && firstKey != -1 && KeyCombo.TYPE_MOUSE.equals(firstKeyType) && button == firstKey) {
            waitingRelease = false;
            return true;
        }
        return false;
    }

    private void finishListening() {
        if (listening) {
            activeListenerCount = Math.max(0, activeListenerCount - 1);
        }
        listening = false;
        firstKey = -1;
        firstKeyType = KeyCombo.TYPE_KEY;
        waitingRelease = false;
        updateButtonText();
    }

    //? if >=26.1 {
    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean isSelected, float delta) {
        super.extractRenderState(graphics, index, y, x, entryWidth, entryHeight, mouseX, mouseY, isSelected, delta);
    //?} else if >=1.20 {
    /*@Override
    public void render(GuiGraphics graphics, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean isSelected, float delta) {
        super.render(graphics, index, y, x, entryWidth, entryHeight, mouseX, mouseY, isSelected, delta);
    *///?} else {
    /*@Override
    public void render(PoseStack graphics, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean isSelected, float delta) {
        super.render(graphics, index, y, x, entryWidth, entryHeight, mouseX, mouseY, isSelected, delta);
    *///?}
        Gfx gfx = new Gfx(graphics);

        Minecraft client = Minecraft.getInstance();
        int right = x + entryWidth;

        // リセットボタン幅をテキストに合わせて動的計算（標準 Cloth Config と同じ）
        int resetWidth = client.font.width(this.resetButton.getMessage()) + 6;
        this.resetButton.setWidth(resetWidth);
        this.resetButton.setX(right - resetWidth);
        this.resetButton.setY(y);
        gfx.widget(this.resetButton, mouseX, mouseY, delta);

        // バインドボタン（150px枠内でリセットボタンの残り領域、標準と同じ配置）
        this.bindButton.setWidth(150 - resetWidth - 2);
        this.bindButton.setX(right - 150);
        this.bindButton.setY(y);
        gfx.widget(this.bindButton, mouseX, mouseY, delta);

        // フィールド名を左側に描画
        gfx.text(client.font, this.getFieldName(), x, y + 6, 0xFFFFFFFF);
    }

    @Override
    public List<? extends GuiEventListener> children() {
        return List.of(this.bindButton, this.resetButton);
    }

    @Override
    public List<AbstractWidget> narratables() {
        return List.of(this.bindButton, this.resetButton);
    }

    @Override
    public boolean isEdited() {
        return value.mainKey != originalValue.mainKey
                || value.modifierKey != originalValue.modifierKey;
    }

    @Override
    public KeyCombo getValue() {
        return value;
    }

    @Override
    public Optional<KeyCombo> getDefaultValue() {
        return Optional.of(defaultValue);
    }

    @Override
    public void save() {
        // "Save & Quit" 経由で閉じられた場合の counter leak を防ぐ
        if (listening) {
            finishListening();
        }
        if (saveConsumer != null) {
            saveConsumer.accept(value);
        }
    }
}

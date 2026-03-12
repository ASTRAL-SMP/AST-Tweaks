package com.astral.asttweaks.config;

import com.astral.asttweaks.util.KeyCombo;
import me.shedaniel.clothconfig2.gui.entries.TooltipListEntry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

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
 *   ESC → キャンセル
 */
public class KeyComboEntry extends TooltipListEntry<KeyCombo> {
    private final KeyCombo value;
    private final KeyCombo defaultValue;
    private final KeyCombo originalValue; // 画面オープン時の値（isEdited比較用）
    private final Consumer<KeyCombo> saveConsumer;
    private final ButtonWidget bindButton;
    private final ButtonWidget resetButton;
    private boolean listening = false;
    private int firstKey = -1;          // 1キー目（押して離した後に記録）
    private boolean waitingRelease = false; // 1キー目の離しを待っている状態

    public KeyComboEntry(Text fieldName, KeyCombo currentValue, KeyCombo defaultValue, Consumer<KeyCombo> saveConsumer) {
        super(fieldName, null);
        this.value = currentValue.copy();
        this.defaultValue = defaultValue.copy();
        this.originalValue = currentValue.copy();
        this.saveConsumer = saveConsumer;

        this.bindButton = ButtonWidget.builder(Text.literal(value.getDisplayName()), button -> {
            listening = true;
            firstKey = -1;
            waitingRelease = false;
            updateButtonText();
        }).build();

        this.resetButton = ButtonWidget.builder(Text.translatable("controls.reset"), button -> {
            value.copyFrom(defaultValue);
            listening = false;
            firstKey = -1;
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
                        Text.literal(KeyCombo.getKeyName(firstKey) + " + ?")
                                .formatted(Formatting.YELLOW));
            } else {
                this.bindButton.setMessage(
                        Text.literal("> ??? <").formatted(Formatting.YELLOW));
            }
        } else {
            this.bindButton.setMessage(Text.literal(value.getDisplayName()));
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!listening) return false;

        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            // ESCでキャンセル
            listening = false;
            firstKey = -1;
            waitingRelease = false;
            updateButtonText();
            return true;
        }

        if (firstKey == -1) {
            // 1キー目を押した — まだ離していないので離しを待つ
            firstKey = keyCode;
            waitingRelease = true;
            updateButtonText();
            return true;
        } else {
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                // Enter → 単キーバインドとして確定
                value.mainKey = firstKey;
                value.modifierKey = -1;
            } else {
                // 2キー目 → コンボ確定
                value.modifierKey = firstKey;
                value.mainKey = keyCode;
            }
            listening = false;
            firstKey = -1;
            waitingRelease = false;
            updateButtonText();
            return true;
        }
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (!listening) return false;

        // 1キー目の離しを検知 — waitingRelease を解除するだけ
        // （2キー目の入力を引き続き待つ）
        if (waitingRelease && firstKey != -1 && keyCode == firstKey) {
            waitingRelease = false;
            return true;
        }
        return false;
    }

    @Override
    public void render(MatrixStack matrices, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean isSelected, float delta) {
        super.render(matrices, index, y, x, entryWidth, entryHeight, mouseX, mouseY, isSelected, delta);

        MinecraftClient client = MinecraftClient.getInstance();
        int right = x + entryWidth;

        // リセットボタン幅をテキストに合わせて動的計算（標準 Cloth Config と同じ）
        int resetWidth = client.textRenderer.getWidth(this.resetButton.getMessage()) + 6;
        this.resetButton.setWidth(resetWidth);
        this.resetButton.setX(right - resetWidth);
        this.resetButton.setY(y);
        this.resetButton.render(matrices, mouseX, mouseY, delta);

        // バインドボタン（150px枠内でリセットボタンの残り領域、標準と同じ配置）
        this.bindButton.setWidth(150 - resetWidth - 2);
        this.bindButton.setX(right - 150);
        this.bindButton.setY(y);
        this.bindButton.render(matrices, mouseX, mouseY, delta);

        // フィールド名を左側に描画
        client.textRenderer.drawWithShadow(matrices, this.getFieldName(), x, y + 6, 16777215);
    }

    @Override
    public List<? extends Element> children() {
        return List.of(this.bindButton, this.resetButton);
    }

    @Override
    public List<ClickableWidget> narratables() {
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
        if (saveConsumer != null) {
            saveConsumer.accept(value);
        }
    }
}

package com.astral.asttweaks.feature.mousesensitivity;

import com.astral.asttweaks.ASTTweaks;
import com.astral.asttweaks.feature.Feature;
import net.minecraft.client.MinecraftClient;

/**
 * マウス感度をキーバインドでトグル切替する機能。
 * キーを押すと事前設定した感度に切り替わり、もう一度押すと元の感度に戻る。
 */
public class MouseSensitivityFeature implements Feature {
    private final MouseSensitivityConfig config;
    private boolean toggled = false;
    private double savedSensitivity = 0.5;
    private int savedSensitivityPercent = 100;

    public MouseSensitivityFeature() {
        this.config = new MouseSensitivityConfig();
    }

    @Override
    public String getId() {
        return "mousesensitivity";
    }

    @Override
    public String getName() {
        return "Mouse Sensitivity";
    }

    @Override
    public void init() {
        ASTTweaks.LOGGER.info("MouseSensitivity feature initialized");
    }

    @Override
    public void tick() {
        // no-op
    }

    @Override
    public boolean isEnabled() {
        return config.isEnabled();
    }

    @Override
    public void setEnabled(boolean enabled) {
        config.setEnabled(enabled);
    }

    public MouseSensitivityConfig getConfig() {
        return config;
    }

    /**
     * 感度をトグルする。
     * トグルOFF→ON: 現在の感度を保存して目標感度に変更
     * トグルON→OFF: 保存した感度に復元
     *
     * @return トグル後の感度（GUI表示の%値: 0-200）
     */
    public int toggle() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.options == null) return 0;

        if (!toggled) {
            // 現在の感度を保存して目標感度に変更
            savedSensitivity = client.options.getMouseSensitivity().getValue();
            // Minecraft表示と一致させるため(int)キャスト（切り捨て）で%を保存
            savedSensitivityPercent = (int)(savedSensitivity * 200.0);
            double targetInternal = config.getTargetSensitivity() / 200.0;
            client.options.getMouseSensitivity().setValue(targetInternal);
            toggled = true;
            ASTTweaks.LOGGER.debug("Mouse sensitivity toggled ON: {}% -> {}%",
                    savedSensitivityPercent, config.getTargetSensitivity());
            return config.getTargetSensitivity();
        } else {
            // 元の感度に復元
            client.options.getMouseSensitivity().setValue(savedSensitivity);
            toggled = false;
            ASTTweaks.LOGGER.debug("Mouse sensitivity toggled OFF: restored to {}%", savedSensitivityPercent);
            return savedSensitivityPercent;
        }
    }

    /**
     * 現在トグル状態かどうか。
     */
    public boolean isToggled() {
        return toggled;
    }
}

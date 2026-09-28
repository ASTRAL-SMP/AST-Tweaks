package com.astral.asttweaks.feature.voidtrade.gui;

import com.astral.asttweaks.ASTTweaks;
import com.astral.asttweaks.feature.FeatureManager;
import com.astral.asttweaks.feature.voidtrade.VoidTradeConfig;
import com.astral.asttweaks.feature.voidtrade.VoidTradePreset;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.MutableText;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

/**
 * Void Trade のプリセット画面。組み込みの手順（簡易ボイトレ・テンプレート）と、
 * 名前を付けて保存したワークフローを一覧し、読み込み・保存・削除する。
 */
public class VoidTradePresetScreen extends Screen {
    private static final String KEY = "config." + ASTTweaks.MOD_ID + ".voidtrade.presets.";
    private static final String WORKFLOW_KEY = "config." + ASTTweaks.MOD_ID + ".voidtrade.workflow.";
    private static final int TOP = 34;
    private static final int NAME_MAX_LENGTH = 48;

    private enum Builtin { SIMPLE, TEMPLATE }

    private final VoidTradeWorkflowScreen editor;
    private final VoidTradeConfig config;
    private PresetListWidget presetList;
    private TextFieldWidget nameField;
    private ButtonWidget loadButton;
    private ButtonWidget deleteButton;
    private ButtonWidget saveButton;
    private int listLeft;
    private int listWidth;
    private Text notice;
    private int noticeTicks;

    public VoidTradePresetScreen(VoidTradeWorkflowScreen editor) {
        super(Text.translatable(KEY + "title"));
        this.editor = editor;
        this.config = FeatureManager.getInstance().getVoidTradeFeature().getConfig();
    }

    @Override
    protected void init() {
        super.init();

        this.listWidth = Math.min(320, this.width - 40);
        this.listLeft = (this.width - this.listWidth) / 2;
        this.presetList = new PresetListWidget(this.client, this.listWidth, this.height, TOP, this.height - 84, 14);
        this.presetList.setLeftPos(this.listLeft);
        this.presetList.setRenderBackground(false);
        this.presetList.setRenderHorizontalShadows(false);
        this.addSelectableChild(this.presetList);

        int saveWidth = 110;
        this.nameField = this.addDrawableChild(new TextFieldWidget(this.textRenderer, this.listLeft, this.height - 75,
                this.listWidth - saveWidth - 6, 18, Text.translatable(KEY + "name")));
        this.nameField.setMaxLength(NAME_MAX_LENGTH);
        this.nameField.setPlaceholder(Text.translatable(KEY + "name"));
        this.saveButton = this.addDrawableChild(ButtonWidget.builder(Text.translatable(KEY + "save"), b -> saveCurrent())
                .dimensions(this.listLeft + this.listWidth - saveWidth, this.height - 76, saveWidth, 20)
                .tooltip(Tooltip.of(Text.translatable(KEY + "save.tooltip")))
                .build());

        int gap = 6;
        int buttonWidth = Math.min(100, (this.width - 20 - gap * 2) / 3);
        int left = this.width / 2 - (buttonWidth * 3 + gap * 2) / 2;
        int bottomY = this.height - 28;
        this.loadButton = this.addDrawableChild(ButtonWidget.builder(Text.translatable(KEY + "load"), b -> loadSelected())
                .dimensions(left, bottomY, buttonWidth, 20)
                .build());
        this.deleteButton = this.addDrawableChild(ButtonWidget.builder(Text.translatable(KEY + "delete"), b -> deleteSelected())
                .dimensions(left + buttonWidth + gap, bottomY, buttonWidth, 20)
                .build());
        this.addDrawableChild(ButtonWidget.builder(Text.translatable("gui.back"), b -> close())
                .dimensions(left + (buttonWidth + gap) * 2, bottomY, buttonWidth, 20)
                .build());

        this.presetList.rebuild();
        updateButtons();
    }

    private PresetEntry getSelected() {
        return this.presetList.getSelectedOrNull();
    }

    private void updateButtons() {
        PresetEntry selected = getSelected();
        this.loadButton.active = selected != null;
        this.deleteButton.active = selected != null && selected.preset != null;
        this.saveButton.active = !this.editor.getSteps().isEmpty();
    }

    private void showNotice(Text text) {
        this.notice = text;
        this.noticeTicks = 200;
    }

    private void loadSelected() {
        PresetEntry selected = getSelected();
        if (selected == null) {
            return;
        }
        // 読み込み（と既存ステップの置き換え確認）は編集画面側で行う
        this.client.setScreen(this.editor);
        if (selected.builtin == Builtin.SIMPLE) {
            this.editor.applySimplePreset();
        } else if (selected.builtin == Builtin.TEMPLATE) {
            this.editor.applyTemplate();
        } else {
            this.editor.loadPreset(selected.preset);
        }
    }

    private void saveCurrent() {
        String name = this.nameField.getText().trim();
        if (name.isEmpty()) {
            showNotice(Text.translatable(KEY + "save.noName"));
            return;
        }
        List<VoidTradePreset> presets = this.config.getPresets();
        VoidTradePreset existing = presets.stream().filter(preset -> preset.name.equals(name)).findFirst().orElse(null);
        if (existing == null) {
            presets.add(new VoidTradePreset(name, this.editor.getSteps()));
            this.config.save();
            showNotice(Text.translatable(KEY + "save.done", name));
            this.presetList.rebuild();
            return;
        }
        this.client.setScreen(new ConfirmScreen(confirmed -> {
            if (confirmed) {
                existing.steps = VoidTradePreset.copySteps(this.editor.getSteps());
                this.config.save();
                showNotice(Text.translatable(KEY + "save.overwritten", name));
            }
            this.client.setScreen(this);
        }, Text.translatable(KEY + "overwrite.title"), Text.translatable(KEY + "overwrite.message", name)));
    }

    private void deleteSelected() {
        PresetEntry selected = getSelected();
        if (selected == null || selected.preset == null) {
            return;
        }
        VoidTradePreset preset = selected.preset;
        this.client.setScreen(new ConfirmScreen(confirmed -> {
            if (confirmed) {
                this.config.getPresets().remove(preset);
                this.config.save();
                showNotice(Text.translatable(KEY + "delete.done", preset.name));
            }
            this.client.setScreen(this);
        }, Text.translatable(KEY + "delete.title"), Text.translatable(KEY + "delete.message", preset.name)));
    }

    @Override
    public void tick() {
        super.tick();
        this.nameField.tick();
        if (this.noticeTicks > 0) {
            this.noticeTicks--;
        }
        updateButtons();
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        this.renderBackground(matrices);
        fill(matrices, this.listLeft, TOP, this.listLeft + this.listWidth, this.height - 84, 0x80000000);
        this.presetList.render(matrices, mouseX, mouseY, delta);

        drawCenteredTextWithShadow(matrices, this.textRenderer, this.title, this.width / 2, 8, 0xFFFFFF);
        if (this.noticeTicks > 0 && this.notice != null) {
            drawCenteredTextWithShadow(matrices, this.textRenderer, this.notice, this.width / 2, 20, 0xFFFF55);
        } else {
            drawCenteredTextWithShadow(matrices, this.textRenderer, Text.translatable(KEY + "help"), this.width / 2, 20, 0xA0A0A0);
        }

        // 選択中のプリセットの説明（組み込みは中身の説明、保存したものはステップ数）
        PresetEntry selected = getSelected();
        if (selected != null) {
            Text description = selected.builtin == Builtin.SIMPLE ? Text.translatable(WORKFLOW_KEY + "simple.tooltip")
                    : selected.builtin == Builtin.TEMPLATE ? Text.translatable(WORKFLOW_KEY + "template.tooltip")
                    : Text.translatable(KEY + "stepCount", selected.preset.steps.size());
            List<OrderedText> lines = this.textRenderer.wrapLines(description, this.listWidth);
            int y = this.height - 52;
            for (int i = 0; i < Math.min(2, lines.size()); i++) {
                drawTextWithShadow(matrices, this.textRenderer, lines.get(i), this.listLeft, y, 0xA0A0A0);
                y += 10;
            }
        }

        super.render(matrices, mouseX, mouseY, delta);
    }

    @Override
    public void close() {
        this.client.setScreen(this.editor);
    }

    class PresetListWidget extends AlwaysSelectedEntryListWidget<PresetEntry> {
        PresetListWidget(MinecraftClient client, int width, int height, int top, int bottom, int itemHeight) {
            super(client, width, height, top, bottom, itemHeight);
        }

        void rebuild() {
            PresetEntry previous = this.getSelectedOrNull();
            this.clearEntries();
            this.addEntry(new PresetEntry(Builtin.SIMPLE, null));
            this.addEntry(new PresetEntry(Builtin.TEMPLATE, null));
            for (VoidTradePreset preset : VoidTradePresetScreen.this.config.getPresets()) {
                this.addEntry(new PresetEntry(null, preset));
            }
            PresetEntry reselect = null;
            if (previous != null) {
                for (PresetEntry entry : this.children()) {
                    if (entry.builtin == previous.builtin && entry.preset == previous.preset) {
                        reselect = entry;
                    }
                }
            }
            super.setSelected(reselect);
        }

        @Override
        public void setSelected(PresetEntry entry) {
            super.setSelected(entry);
            if (entry != null && entry.preset != null) {
                VoidTradePresetScreen.this.nameField.setText(entry.preset.name);
            }
        }

        @Override
        public int getRowWidth() {
            return this.width - 20;
        }

        @Override
        protected int getScrollbarPositionX() {
            return this.left + this.width - 6;
        }
    }

    class PresetEntry extends AlwaysSelectedEntryListWidget.Entry<PresetEntry> {
        private final Builtin builtin;
        private final VoidTradePreset preset;

        PresetEntry(Builtin builtin, VoidTradePreset preset) {
            this.builtin = builtin;
            this.preset = preset;
        }

        private MutableText label() {
            if (this.builtin != null) {
                return Text.translatable(WORKFLOW_KEY + (this.builtin == Builtin.SIMPLE ? "simple" : "template"))
                        .append(Text.translatable(KEY + "builtin").formatted(Formatting.GRAY));
            }
            return Text.literal(this.preset.name)
                    .append(Text.translatable(KEY + "steps", this.preset.steps.size()).formatted(Formatting.GRAY));
        }

        @Override
        public void render(MatrixStack matrices, int index, int y, int x, int entryWidth, int entryHeight,
                           int mouseX, int mouseY, boolean hovered, float tickDelta) {
            List<OrderedText> lines = VoidTradePresetScreen.this.textRenderer.wrapLines(label(), entryWidth - 4);
            if (!lines.isEmpty()) {
                drawTextWithShadow(matrices, VoidTradePresetScreen.this.textRenderer, lines.get(0), x + 2, y + 2,
                        this.builtin != null ? 0x55FFFF : 0xFFFFFF);
            }
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            return button == 0;
        }

        @Override
        public Text getNarration() {
            return label();
        }
    }
}

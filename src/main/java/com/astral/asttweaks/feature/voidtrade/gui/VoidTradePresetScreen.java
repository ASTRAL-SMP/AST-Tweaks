//? if <26.1 {
/*package com.astral.asttweaks.feature.voidtrade.gui;

import com.astral.asttweaks.ASTTweaks;
import com.astral.asttweaks.feature.FeatureManager;
import com.astral.asttweaks.feature.voidtrade.VoidTradeConfig;
import com.astral.asttweaks.feature.voidtrade.VoidTradePreset;
import com.astral.asttweaks.util.McClient;
import com.astral.asttweaks.util.gui.CompatListEntry;
import com.astral.asttweaks.util.gui.CompatScreen;
import com.astral.asttweaks.util.gui.CompatSelectionList;
import com.astral.asttweaks.util.gui.Gfx;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;

/^*
 * Void Trade のプリセット画面。組み込みの手順（簡易ボイトレ・テンプレート）と、
 * 名前を付けて保存したワークフローを一覧し、読み込み・保存・削除する。
 ^/
public class VoidTradePresetScreen extends CompatScreen {
    private static final String KEY = "config." + ASTTweaks.MOD_ID + ".voidtrade.presets.";
    private static final String WORKFLOW_KEY = "config." + ASTTweaks.MOD_ID + ".voidtrade.workflow.";
    private static final int TOP = 34;
    private static final int NAME_MAX_LENGTH = 48;

    private enum Builtin { SIMPLE, TEMPLATE }

    private final VoidTradeWorkflowScreen editor;
    private final VoidTradeConfig config;
    private PresetListWidget presetList;
    private EditBox nameField;
    private Button loadButton;
    private Button deleteButton;
    private Button saveButton;
    private int listLeft;
    private int listWidth;
    private Component notice;
    private int noticeTicks;

    public VoidTradePresetScreen(VoidTradeWorkflowScreen editor) {
        super(Component.translatable(KEY + "title"));
        this.editor = editor;
        this.config = FeatureManager.getInstance().getVoidTradeFeature().getConfig();
    }

    @Override
    protected void init() {
        super.init();

        this.listWidth = Math.min(320, this.width - 40);
        this.listLeft = (this.width - this.listWidth) / 2;
        this.presetList = new PresetListWidget(this.minecraft, this.listWidth, this.height, TOP, this.height - 84, 14);
        this.presetList.setLeftPos(this.listLeft);
        this.presetList.setRenderBackground(false);
        this.presetList.setRenderTopAndBottom(false);
        this.addWidget(this.presetList);

        int saveWidth = 110;
        this.nameField = this.addRenderableWidget(new EditBox(this.font, this.listLeft, this.height - 75,
                this.listWidth - saveWidth - 6, 18, Component.translatable(KEY + "name")));
        this.nameField.setMaxLength(NAME_MAX_LENGTH);
        this.nameField.setHint(Component.translatable(KEY + "name"));
        this.saveButton = this.addRenderableWidget(Button.builder(Component.translatable(KEY + "save"), b -> saveCurrent())
                .bounds(this.listLeft + this.listWidth - saveWidth, this.height - 76, saveWidth, 20)
                .tooltip(Tooltip.create(Component.translatable(KEY + "save.tooltip")))
                .build());

        int gap = 6;
        int buttonWidth = Math.min(100, (this.width - 20 - gap * 2) / 3);
        int left = this.width / 2 - (buttonWidth * 3 + gap * 2) / 2;
        int bottomY = this.height - 28;
        this.loadButton = this.addRenderableWidget(Button.builder(Component.translatable(KEY + "load"), b -> loadSelected())
                .bounds(left, bottomY, buttonWidth, 20)
                .build());
        this.deleteButton = this.addRenderableWidget(Button.builder(Component.translatable(KEY + "delete"), b -> deleteSelected())
                .bounds(left + buttonWidth + gap, bottomY, buttonWidth, 20)
                .build());
        this.addRenderableWidget(Button.builder(Component.translatable("gui.back"), b -> onClose())
                .bounds(left + (buttonWidth + gap) * 2, bottomY, buttonWidth, 20)
                .build());

        this.presetList.rebuild();
        updateButtons();
    }

    private PresetEntry getSelected() {
        return this.presetList.getSelected();
    }

    private void updateButtons() {
        PresetEntry selected = getSelected();
        this.loadButton.active = selected != null;
        this.deleteButton.active = selected != null && selected.preset != null;
        this.saveButton.active = !this.editor.getSteps().isEmpty();
    }

    private void showNotice(Component text) {
        this.notice = text;
        this.noticeTicks = 200;
    }

    private void loadSelected() {
        PresetEntry selected = getSelected();
        if (selected == null) {
            return;
        }
        // 読み込み（と既存ステップの置き換え確認）は編集画面側で行う
        McClient.setScreen(this.editor);
        if (selected.builtin == Builtin.SIMPLE) {
            this.editor.applySimplePreset();
        } else if (selected.builtin == Builtin.TEMPLATE) {
            this.editor.applyTemplate();
        } else {
            this.editor.loadPreset(selected.preset);
        }
    }

    private void saveCurrent() {
        String name = this.nameField.getValue().trim();
        if (name.isEmpty()) {
            showNotice(Component.translatable(KEY + "save.noName"));
            return;
        }
        List<VoidTradePreset> presets = this.config.getPresets();
        VoidTradePreset existing = presets.stream().filter(preset -> preset.name.equals(name)).findFirst().orElse(null);
        if (existing == null) {
            presets.add(new VoidTradePreset(name, this.editor.getSteps()));
            this.config.save();
            showNotice(Component.translatable(KEY + "save.done", name));
            this.presetList.rebuild();
            return;
        }
        McClient.setScreen(new ConfirmScreen(confirmed -> {
            if (confirmed) {
                existing.steps = VoidTradePreset.copySteps(this.editor.getSteps());
                this.config.save();
                showNotice(Component.translatable(KEY + "save.overwritten", name));
            }
            McClient.setScreen(this);
        }, Component.translatable(KEY + "overwrite.title"), Component.translatable(KEY + "overwrite.message", name)));
    }

    private void deleteSelected() {
        PresetEntry selected = getSelected();
        if (selected == null || selected.preset == null) {
            return;
        }
        VoidTradePreset preset = selected.preset;
        McClient.setScreen(new ConfirmScreen(confirmed -> {
            if (confirmed) {
                this.config.getPresets().remove(preset);
                this.config.save();
                showNotice(Component.translatable(KEY + "delete.done", preset.name));
            }
            McClient.setScreen(this);
        }, Component.translatable(KEY + "delete.title"), Component.translatable(KEY + "delete.message", preset.name)));
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
    protected void draw(Gfx gfx, int mouseX, int mouseY, float delta) {
        gfx.fill(this.listLeft, TOP, this.listLeft + this.listWidth, this.height - 84, 0x80000000);
        gfx.widget(this.presetList, mouseX, mouseY, delta);

        gfx.centeredText(this.font, this.title, this.width / 2, 8, 0xFFFFFF);
        if (this.noticeTicks > 0 && this.notice != null) {
            gfx.centeredText(this.font, this.notice, this.width / 2, 20, 0xFFFF55);
        } else {
            gfx.centeredText(this.font, Component.translatable(KEY + "help"), this.width / 2, 20, 0xA0A0A0);
        }

        // 選択中のプリセットの説明（組み込みは中身の説明、保存したものはステップ数）
        PresetEntry selected = getSelected();
        if (selected != null) {
            Component description = selected.builtin == Builtin.SIMPLE ? Component.translatable(WORKFLOW_KEY + "simple.tooltip")
                    : selected.builtin == Builtin.TEMPLATE ? Component.translatable(WORKFLOW_KEY + "template.tooltip")
                    : Component.translatable(KEY + "stepCount", selected.preset.steps.size());
            List<FormattedCharSequence> lines = this.font.split(description, this.listWidth);
            int y = this.height - 52;
            for (int i = 0; i < Math.min(2, lines.size()); i++) {
                gfx.text(this.font, lines.get(i), this.listLeft, y, 0xA0A0A0);
                y += 10;
            }
        }

        renderWidgets(gfx, mouseX, mouseY, delta);
    }

    @Override
    public void onClose() {
        McClient.setScreen(this.editor);
    }

    class PresetListWidget extends CompatSelectionList<PresetEntry> {
        PresetListWidget(Minecraft client, int width, int height, int top, int bottom, int itemHeight) {
            super(client, width, height, top, bottom, itemHeight);
        }

        void rebuild() {
            PresetEntry previous = this.getSelected();
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
                VoidTradePresetScreen.this.nameField.setValue(entry.preset.name);
            }
        }

        @Override
        public int getRowWidth() {
            return this.width - 20;
        }

        @Override
        protected int getScrollbarPosition() {
            return this.x0 + this.width - 6;
        }
    }

    class PresetEntry extends CompatListEntry<PresetEntry> {
        private final Builtin builtin;
        private final VoidTradePreset preset;

        PresetEntry(Builtin builtin, VoidTradePreset preset) {
            this.builtin = builtin;
            this.preset = preset;
        }

        private MutableComponent label() {
            if (this.builtin != null) {
                return Component.translatable(WORKFLOW_KEY + (this.builtin == Builtin.SIMPLE ? "simple" : "template"))
                        .append(Component.translatable(KEY + "builtin").withStyle(ChatFormatting.GRAY));
            }
            return Component.literal(this.preset.name)
                    .append(Component.translatable(KEY + "steps", this.preset.steps.size()).withStyle(ChatFormatting.GRAY));
        }

        @Override
        protected void drawEntry(Gfx gfx, int x, int y, int entryWidth, int entryHeight,
                                 int mouseX, int mouseY, boolean hovered, float tickDelta) {
            List<FormattedCharSequence> lines = VoidTradePresetScreen.this.font.split(label(), entryWidth - 4);
            if (!lines.isEmpty()) {
                gfx.text(VoidTradePresetScreen.this.font, lines.get(0), x + 2, y + 2,
                        this.builtin != null ? 0x55FFFF : 0xFFFFFF);
            }
        }

        @Override
        protected boolean onMouseClicked(double mouseX, double mouseY, int button) {
            return button == 0;
        }

        @Override
        public Component getNarration() {
            return label();
        }
    }
}
*///?}

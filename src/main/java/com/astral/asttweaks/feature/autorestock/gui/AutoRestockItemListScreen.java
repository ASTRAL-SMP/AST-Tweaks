package com.astral.asttweaks.feature.autorestock.gui;

import com.astral.asttweaks.feature.FeatureManager;
import com.astral.asttweaks.feature.autorestock.AutoRestockConfig;
import com.astral.asttweaks.feature.autorestock.AutoRestockFeature;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.Registries;
import net.minecraft.resource.featuretoggle.FeatureSet;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

/**
 * Screen for configuring per-stack auto restock rules.
 */
public class AutoRestockItemListScreen extends Screen {
    private static final int COUNT_FIELD_WIDTH = 58;
    private static final int COUNT_FIELD_MAX_LENGTH = 10;

    private final Screen parent;
    private final AutoRestockConfig config;
    private ItemListWidget itemListWidget;
    private TextFieldWidget searchField;
    private String searchText = "";
    private List<ItemStack> allStacks = List.of();
    private ItemEntry activeCountEntry;
    private TextFieldWidget activeCountField;

    public AutoRestockItemListScreen(Screen parent) {
        super(Text.translatable("config.asttweaks.autorestock.itemlist.title"));
        this.parent = parent;
        AutoRestockFeature feature = FeatureManager.getInstance().getAutoRestockFeature();
        this.config = feature != null ? feature.getConfig() : null;
    }

    @Override
    protected void init() {
        super.init();

        this.itemListWidget = new ItemListWidget(this.client, this.width, this.height, 54, this.height - 32, 36);
        this.addSelectableChild(this.itemListWidget);

        this.allStacks = buildAvailableStacks();

        this.searchField = new TextFieldWidget(this.textRenderer, this.width / 2 - 100, 32, 200, 18,
                Text.translatable("config.asttweaks.search.placeholder"));
        this.searchField.setPlaceholder(Text.translatable("config.asttweaks.search.placeholder"));
        this.searchField.setChangedListener(text -> {
            this.searchText = text;
            rebuildList();
        });
        this.searchField.setText(this.searchText);
        this.addSelectableChild(this.searchField);

        rebuildList();

        this.addDrawableChild(ButtonWidget.builder(
                        Text.translatable("gui.done"),
                        b -> this.client.setScreen(parent))
                .dimensions(this.width / 2 - 100, this.height - 27, 200, 20)
                .build());
    }

    @Override
    public void tick() {
        super.tick();
        this.searchField.tick();
        if (this.activeCountField != null) {
            this.activeCountField.tick();
        }
    }

    private List<ItemStack> buildAvailableStacks() {
        FeatureSet enabledFeatures = AutoRestockConfig.getDefaultEnabledFeatures();
        DynamicRegistryManager lookup = AutoRestockConfig.getDefaultRegistryLookup();

        if (this.client != null && this.client.getNetworkHandler() != null) {
            enabledFeatures = this.client.getNetworkHandler().getEnabledFeatures();
            lookup = this.client.getNetworkHandler().getRegistryManager();
        } else if (this.client != null && this.client.world != null) {
            enabledFeatures = this.client.world.getEnabledFeatures();
        }

        ItemGroups.updateDisplayContext(enabledFeatures, false, lookup);

        LinkedHashMap<String, ItemStack> uniqueStacks = new LinkedHashMap<>();
        for (ItemStack stack : ItemGroups.getSearchGroup().getSearchTabStacks()) {
            if (stack.isEmpty() || !stack.isItemEnabled(enabledFeatures)) {
                continue;
            }

            uniqueStacks.putIfAbsent(AutoRestockConfig.getStackKey(stack), stack.copyWithCount(1));
        }

        return new ArrayList<>(uniqueStacks.values());
    }

    private void rebuildList() {
        clearActiveCountField(true);
        this.itemListWidget.children().clear();
        String lowerSearch = searchText.toLowerCase(Locale.ROOT);

        allStacks.stream()
                .filter(stack -> searchText.isEmpty()
                        || stack.getName().getString().toLowerCase(Locale.ROOT).contains(lowerSearch)
                        || Registries.ITEM.getId(stack.getItem()).toString().toLowerCase(Locale.ROOT).contains(lowerSearch))
                .sorted((a, b) -> {
                    boolean aConfigured = config != null && config.isStackConfigured(a);
                    boolean bConfigured = config != null && config.isStackConfigured(b);
                    if (aConfigured != bConfigured) {
                        return aConfigured ? -1 : 1;
                    }

                    int byName = a.getName().getString().compareToIgnoreCase(b.getName().getString());
                    if (byName != 0) {
                        return byName;
                    }

                    return AutoRestockConfig.getStackKey(a).compareTo(AutoRestockConfig.getStackKey(b));
                })
                .forEach(stack -> itemListWidget.addEntry(new ItemEntry(stack.copyWithCount(1))));
    }

    private void setActiveCountField(ItemEntry entry) {
        if (this.activeCountEntry != null && this.activeCountEntry != entry) {
            this.activeCountEntry.normalizeCountFieldText();
            this.activeCountEntry.countField.setFocused(false);
        }

        this.activeCountEntry = entry;
        this.activeCountField = entry.countField;
        this.searchField.setFocused(false);
        this.activeCountField.setFocused(true);
    }

    private void clearActiveCountField(boolean normalize) {
        if (this.activeCountEntry != null) {
            if (normalize) {
                this.activeCountEntry.normalizeCountFieldText();
            }
            this.activeCountEntry.countField.setFocused(false);
        }

        this.activeCountEntry = null;
        this.activeCountField = null;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && this.activeCountField != null && !this.activeCountField.isMouseOver(mouseX, mouseY)) {
            clearActiveCountField(true);
        }

        boolean handled = super.mouseClicked(mouseX, mouseY, button);
        if (this.searchField.isFocused()) {
            clearActiveCountField(false);
        }
        return handled;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.activeCountField != null && this.activeCountField.isFocused()) {
            if (keyCode == 257 || keyCode == 335) {
                clearActiveCountField(true);
                return true;
            }
            if (this.activeCountField.keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            }
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (this.activeCountField != null && this.activeCountField.isFocused() &&
                this.activeCountField.charTyped(chr, modifiers)) {
            return true;
        }

        return super.charTyped(chr, modifiers);
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        this.renderBackground(matrices);
        this.itemListWidget.render(matrices, mouseX, mouseY, delta);

        int titleWidth = this.textRenderer.getWidth(this.title);
        this.textRenderer.drawWithShadow(matrices, this.title, this.width / 2 - titleWidth / 2, 10, 0xFFFFFF);

        Text help = Text.translatable("config.asttweaks.autorestock.itemlist.help");
        int helpWidth = this.textRenderer.getWidth(help);
        this.textRenderer.drawWithShadow(matrices, help, this.width / 2 - helpWidth / 2, 20, 0xA0A0A0);

        this.searchField.render(matrices, mouseX, mouseY, delta);
        super.render(matrices, mouseX, mouseY, delta);
    }

    class ItemListWidget extends AlwaysSelectedEntryListWidget<ItemEntry> {
        public ItemListWidget(MinecraftClient client, int width, int height, int top, int bottom, int itemHeight) {
            super(client, width, height, top, bottom, itemHeight);
        }

        @Override
        protected int getScrollbarPositionX() {
            return super.getScrollbarPositionX() + 60;
        }

        @Override
        public int getRowWidth() {
            return super.getRowWidth() + 150;
        }

        @Override
        public int addEntry(ItemEntry entry) {
            return super.addEntry(entry);
        }
    }

    class ItemEntry extends AlwaysSelectedEntryListWidget.Entry<ItemEntry> {
        private final ItemStack stack;
        private final ButtonWidget addRemoveButton;
        private final ButtonWidget positionButton;
        private final TextFieldWidget countField;
        private boolean updatingCountField;

        public ItemEntry(ItemStack stack) {
            this.stack = stack;
            this.addRemoveButton = ButtonWidget.builder(Text.literal(""), b -> {
                        if (config == null) {
                            return;
                        }

                        if (config.isStackConfigured(this.stack)) {
                            AutoRestockItemListScreen.this.clearActiveCountField(false);
                            config.removeStack(this.stack);
                        } else {
                            config.addStack(this.stack);
                        }
                        AutoRestockItemListScreen.this.rebuildList();
                    })
                    .dimensions(0, 0, 52, 20)
                    .build();
            this.positionButton = ButtonWidget.builder(
                            Text.translatable("config.asttweaks.autorestock.itemlist.position"),
                            b -> AutoRestockItemListScreen.this.client.setScreen(
                                    new AutoRestockTargetSlotScreen(AutoRestockItemListScreen.this, this.stack.copyWithCount(1))))
                    .dimensions(0, 0, 88, 20)
                    .build();
            this.countField = new TextFieldWidget(
                    AutoRestockItemListScreen.this.textRenderer, 0, 0, COUNT_FIELD_WIDTH, 18,
                    Text.translatable("config.asttweaks.autorestock.itemlist.count"));
            this.countField.setMaxLength(COUNT_FIELD_MAX_LENGTH);
            this.countField.setTextPredicate(text -> text.isEmpty() || text.matches("\\d{0," + COUNT_FIELD_MAX_LENGTH + "}"));
            this.countField.setPlaceholder(Text.translatable("config.asttweaks.autorestock.itemlist.count"));
            this.countField.setChangedListener(text -> {
                if (this.updatingCountField || config == null || !config.isStackConfigured(this.stack) || text.isEmpty()) {
                    return;
                }

                try {
                    int parsedValue = parseDesiredCount(text);
                    int savedValue = config.setDesiredCount(this.stack, parsedValue);
                    if (savedValue != parsedValue) {
                        setCountFieldText(String.valueOf(savedValue));
                    }
                } catch (NumberFormatException ignored) {
                }
            });
            syncCountFieldText();
            updateAddRemoveButton();
        }

        private void updateAddRemoveButton() {
            boolean configured = config != null && config.isStackConfigured(this.stack);
            this.addRemoveButton.setMessage(Text.translatable(
                    configured ? "config.asttweaks.autorestock.itemlist.remove"
                            : "config.asttweaks.autorestock.itemlist.add"));
        }

        private void setCountFieldText(String text) {
            this.updatingCountField = true;
            this.countField.setText(text);
            this.updatingCountField = false;
        }

        private void syncCountFieldText() {
            if (config != null && config.isStackConfigured(this.stack) && !this.countField.isFocused()) {
                setCountFieldText(String.valueOf(config.getDesiredCount(this.stack)));
            }
        }

        private void normalizeCountFieldText() {
            if (config == null || !config.isStackConfigured(this.stack)) {
                setCountFieldText("");
                return;
            }

            String text = this.countField.getText();
            if (text.isEmpty()) {
                setCountFieldText(String.valueOf(config.getDesiredCount(this.stack)));
                return;
            }

            try {
                int savedValue = config.setDesiredCount(this.stack, parseDesiredCount(text));
                setCountFieldText(String.valueOf(savedValue));
            } catch (NumberFormatException ignored) {
                setCountFieldText(String.valueOf(config.getDesiredCount(this.stack)));
            }
        }

        private int parseDesiredCount(String text) {
            long parsed = Long.parseLong(text);
            return (int) Math.min(Integer.MAX_VALUE, Math.max(1L, parsed));
        }

        @Override
        public void render(MatrixStack matrices, int index, int y, int x, int width, int height,
                           int mouseX, int mouseY, boolean hovered, float delta) {
            MinecraftClient client = AutoRestockItemListScreen.this.client;
            boolean configured = config != null && config.isStackConfigured(this.stack);
            updateAddRemoveButton();
            syncCountFieldText();

            client.getItemRenderer().renderInGuiWithOverrides(matrices, this.stack, x + 5, y + 8);
            client.getItemRenderer().renderGuiItemOverlay(matrices, client.textRenderer, this.stack, x + 5, y + 8);

            client.textRenderer.draw(matrices, this.stack.getName(), x + 30, y + 5, 0xFFFFFF);
            Text itemId = Text.literal(Registries.ITEM.getId(this.stack.getItem()).toString()).formatted(Formatting.GRAY);
            client.textRenderer.draw(matrices, itemId, x + 30, y + 16, 0xA0A0A0);

            int addRemoveX = x + width - 55;
            this.addRemoveButton.setPosition(addRemoveX, y + 7);
            this.addRemoveButton.render(matrices, mouseX, mouseY, delta);

            if (configured) {
                int positionButtonX = addRemoveX - 154;
                this.positionButton.setPosition(positionButtonX, y + 7);
                this.positionButton.render(matrices, mouseX, mouseY, delta);

                this.countField.setPosition(positionButtonX + 94, y + 8);
                this.countField.render(matrices, mouseX, mouseY, delta);
            }
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            boolean configured = config != null && config.isStackConfigured(this.stack);
            if (configured && this.positionButton.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
            if (configured && this.countField.mouseClicked(mouseX, mouseY, button)) {
                AutoRestockItemListScreen.this.setActiveCountField(this);
                return true;
            }
            return this.addRemoveButton.mouseClicked(mouseX, mouseY, button);
        }

        @Override
        public Text getNarration() {
            return this.stack.getName();
        }
    }
}

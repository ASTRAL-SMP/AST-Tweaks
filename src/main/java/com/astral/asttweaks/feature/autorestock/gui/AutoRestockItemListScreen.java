package com.astral.asttweaks.feature.autorestock.gui;

import com.astral.asttweaks.feature.FeatureManager;
import com.astral.asttweaks.feature.autorestock.AutoRestockConfig;
import com.astral.asttweaks.feature.autorestock.AutoRestockFeature;
import com.astral.asttweaks.util.ItemDisplay;
import com.astral.asttweaks.util.McClient;
import com.astral.asttweaks.util.gui.CompatListEntry;
import com.astral.asttweaks.util.gui.CompatScreen;
import com.astral.asttweaks.util.gui.CompatSelectionList;
import com.astral.asttweaks.util.gui.Gfx;
import com.astral.asttweaks.util.gui.GuiEvents;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Screen for configuring per-stack auto restock rules.
 */
public class AutoRestockItemListScreen extends CompatScreen {
    private static final int COUNT_FIELD_WIDTH = 58;
    private static final int COUNT_FIELD_MAX_LENGTH = 10;

    private final Screen parent;
    private final AutoRestockConfig config;
    private ItemListWidget itemListWidget;
    private EditBox searchField;
    private String searchText = "";
    private List<ItemStack> allStacks = List.of();
    private ItemEntry activeCountEntry;
    private EditBox activeCountField;

    public AutoRestockItemListScreen(Screen parent) {
        super(Component.translatable("config.asttweaks.autorestock.itemlist.title"));
        this.parent = parent;
        AutoRestockFeature feature = FeatureManager.getInstance().getAutoRestockFeature();
        this.config = feature != null ? feature.getConfig() : null;
    }

    @Override
    protected void init() {
        super.init();

        this.itemListWidget = new ItemListWidget(this.minecraft, this.width, this.height, 54, this.height - 32, 36);
        this.addWidget(this.itemListWidget);

        this.allStacks = buildAvailableStacks();

        this.searchField = new EditBox(this.font, this.width / 2 - 100, 32, 200, 18,
                Component.translatable("config.asttweaks.search.placeholder"));
        this.searchField.setHint(Component.translatable("config.asttweaks.search.placeholder"));
        this.searchField.setResponder(text -> {
            this.searchText = text;
            rebuildList();
        });
        this.searchField.setValue(this.searchText);
        this.addWidget(this.searchField);

        rebuildList();

        this.addRenderableWidget(Button.builder(
                        Component.translatable("gui.done"),
                        b -> McClient.setScreen(parent))
                .bounds(this.width / 2 - 100, this.height - 27, 200, 20)
                .build());
    }

    private List<ItemStack> buildAvailableStacks() {
        if (!ItemDisplay.componentsBound(Items.STONE)) {
            return new ArrayList<>();
        }

        FeatureFlagSet enabledFeatures = AutoRestockConfig.getDefaultEnabledFeatures();
        RegistryAccess lookup = AutoRestockConfig.getDefaultRegistryLookup();

        if (this.minecraft != null && this.minecraft.getConnection() != null) {
            enabledFeatures = this.minecraft.getConnection().enabledFeatures();
            lookup = this.minecraft.getConnection().registryAccess();
        } else if (this.minecraft != null && this.minecraft.level != null) {
            enabledFeatures = this.minecraft.level.enabledFeatures();
        }
        if (lookup == RegistryAccess.EMPTY) {
            // Creative tab generators look up dynamic registries (enchantments, paintings...)
            return new ArrayList<>();
        }

        CreativeModeTabs.tryRebuildTabContents(enabledFeatures, false, lookup);

        LinkedHashMap<String, ItemStack> uniqueStacks = new LinkedHashMap<>();
        for (ItemStack stack : CreativeModeTabs.searchTab().getSearchTabDisplayItems()) {
            if (stack.isEmpty() || !stack.isItemEnabled(enabledFeatures)) {
                continue;
            }

            uniqueStacks.putIfAbsent(AutoRestockConfig.getStackKey(stack), stack.copyWithCount(1));
        }

        return new ArrayList<>(uniqueStacks.values());
    }

    private void rebuildList() {
        clearActiveCountField(true);
        this.itemListWidget.clearAll();
        String lowerSearch = searchText.toLowerCase(Locale.ROOT);

        allStacks.stream()
                .filter(stack -> searchText.isEmpty()
                        || stack.getHoverName().getString().toLowerCase(Locale.ROOT).contains(lowerSearch)
                        || BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().toLowerCase(Locale.ROOT).contains(lowerSearch))
                .sorted((a, b) -> {
                    boolean aConfigured = config != null && config.isStackConfigured(a);
                    boolean bConfigured = config != null && config.isStackConfigured(b);
                    if (aConfigured != bConfigured) {
                        return aConfigured ? -1 : 1;
                    }

                    int byName = a.getHoverName().getString().compareToIgnoreCase(b.getHoverName().getString());
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
    protected boolean onMouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && this.activeCountField != null && !this.activeCountField.isMouseOver(mouseX, mouseY)) {
            clearActiveCountField(true);
        }

        boolean handled = superMouseClicked(mouseX, mouseY, button);
        if (this.searchField.isFocused()) {
            clearActiveCountField(false);
        }
        return handled;
    }

    @Override
    protected boolean onKeyPressed(int keyCode, int scancode, int modifiers) {
        if (this.activeCountField != null && this.activeCountField.isFocused()) {
            if (keyCode == InputConstants.KEY_RETURN || keyCode == InputConstants.KEY_NUMPADENTER) {
                clearActiveCountField(true);
                return true;
            }
            if (GuiEvents.keyPress(this.activeCountField, keyCode, scancode, modifiers)) {
                return true;
            }
        }

        return superKeyPressed(keyCode, scancode, modifiers);
    }

    @Override
    protected boolean onCharTyped(int codepoint, int modifiers) {
        if (this.activeCountField != null && this.activeCountField.isFocused() &&
                GuiEvents.charType(this.activeCountField, codepoint, modifiers)) {
            return true;
        }

        return superCharTyped(codepoint, modifiers);
    }

    @Override
    protected void draw(Gfx gfx, int mouseX, int mouseY, float delta) {
        gfx.widget(this.itemListWidget, mouseX, mouseY, delta);

        if (!ItemDisplay.itemDataLoaded()) {
            gfx.centeredText(this.font, ItemDisplay.NEEDS_WORLD, this.width / 2, this.height / 2, 0xFFFFAA00);
        }

        int titleWidth = this.font.width(this.title);
        gfx.text(this.font, this.title, this.width / 2 - titleWidth / 2, 10, 0xFFFFFFFF);

        Component help = Component.translatable("config.asttweaks.autorestock.itemlist.help");
        int helpWidth = this.font.width(help);
        gfx.text(this.font, help, this.width / 2 - helpWidth / 2, 20, 0xFFA0A0A0);

        gfx.widget(this.searchField, mouseX, mouseY, delta);
        renderWidgets(gfx, mouseX, mouseY, delta);
    }

    class ItemListWidget extends CompatSelectionList<ItemEntry> {
        public ItemListWidget(Minecraft client, int width, int height, int top, int bottom, int itemHeight) {
            super(client, width, height, top, bottom, itemHeight);
        }


        @Override
        public int getRowWidth() {
            return super.getRowWidth() + 150;
        }

    }

    class ItemEntry extends CompatListEntry<ItemEntry> {
        private final ItemStack stack;
        private final Button addRemoveButton;
        private final Button positionButton;
        private final EditBox countField;
        private boolean updatingCountField;

        public ItemEntry(ItemStack stack) {
            this.stack = stack;
            this.addRemoveButton = Button.builder(Component.literal(""), b -> {
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
                    .bounds(0, 0, 52, 20)
                    .build();
            this.positionButton = Button.builder(
                            Component.translatable("config.asttweaks.autorestock.itemlist.position"),
                            b -> McClient.setScreen(
                                    new AutoRestockTargetSlotScreen(AutoRestockItemListScreen.this, this.stack.copyWithCount(1))))
                    .bounds(0, 0, 88, 20)
                    .build();
            this.countField = new EditBox(
                    AutoRestockItemListScreen.this.font, 0, 0, COUNT_FIELD_WIDTH, 18,
                    Component.translatable("config.asttweaks.autorestock.itemlist.count")) {
                @Override
                public void insertText(String input) {
                    super.insertText(input.replaceAll("\\D", ""));
                }
            };
            this.countField.setMaxLength(COUNT_FIELD_MAX_LENGTH);
            this.countField.setHint(Component.translatable("config.asttweaks.autorestock.itemlist.count"));
            this.countField.setResponder(text -> {
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
            this.addRemoveButton.setMessage(Component.translatable(
                    configured ? "config.asttweaks.autorestock.itemlist.remove"
                            : "config.asttweaks.autorestock.itemlist.add"));
        }

        private void setCountFieldText(String text) {
            this.updatingCountField = true;
            this.countField.setValue(text);
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

            String text = this.countField.getValue();
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
        protected void drawEntry(Gfx gfx, int x, int y, int entryWidth, int entryHeight,
                                 int mouseX, int mouseY, boolean hovered, float delta) {
            int width = entryWidth;
            Minecraft client = AutoRestockItemListScreen.this.minecraft;
            boolean configured = config != null && config.isStackConfigured(this.stack);
            updateAddRemoveButton();
            syncCountFieldText();

            gfx.item(this.stack, x + 5, y + 8);
            gfx.itemDecorations(client.font, this.stack, x + 5, y + 8);

            gfx.text(client.font, this.stack.getHoverName(), x + 30, y + 5, 0xFFFFFFFF, false);
            Component itemId = Component.literal(BuiltInRegistries.ITEM.getKey(this.stack.getItem()).toString()).withStyle(ChatFormatting.GRAY);
            gfx.text(client.font, itemId, x + 30, y + 16, 0xFFA0A0A0, false);

            int addRemoveX = x + width - 55;
            this.addRemoveButton.setPosition(addRemoveX, y + 7);
            gfx.widget(this.addRemoveButton, mouseX, mouseY, delta);

            if (configured) {
                int positionButtonX = addRemoveX - 154;
                this.positionButton.setPosition(positionButtonX, y + 7);
                gfx.widget(this.positionButton, mouseX, mouseY, delta);

                this.countField.setPosition(positionButtonX + 94, y + 8);
                gfx.widget(this.countField, mouseX, mouseY, delta);
            }
        }

        @Override
        protected boolean onMouseClicked(double mouseX, double mouseY, int button) {
            boolean configured = config != null && config.isStackConfigured(this.stack);
            if (configured && GuiEvents.click(this.positionButton, mouseX, mouseY, button)) {
                return true;
            }
            if (configured && GuiEvents.click(this.countField, mouseX, mouseY, button)) {
                AutoRestockItemListScreen.this.setActiveCountField(this);
                return true;
            }
            return GuiEvents.click(this.addRemoveButton, mouseX, mouseY, button);
        }

        @Override
        public Component getNarration() {
            return this.stack.getHoverName();
        }
    }
}

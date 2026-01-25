package com.astral.asttweaks.feature.entityculling.gui;

import com.astral.asttweaks.ASTTweaks;
import com.astral.asttweaks.feature.FeatureManager;
import com.astral.asttweaks.feature.entityculling.EntityCullingConfig;
import com.astral.asttweaks.feature.entityculling.EntityCullingFeature;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Screen for configuring item entity visibility blacklist.
 */
public class ItemEntityListScreen extends Screen {
    private final Screen parent;
    private ItemListWidget itemListWidget;
    private final EntityCullingConfig config;
    private TextFieldWidget searchField;
    private String searchText = "";
    private List<Item> allItems;

    public ItemEntityListScreen(Screen parent) {
        super(Text.translatable("config.asttweaks.entityculling.itemblacklist.title"));
        this.parent = parent;
        EntityCullingFeature feature = FeatureManager.getInstance().getEntityCullingFeature();
        this.config = feature != null ? feature.getConfig() : null;
    }

    @Override
    protected void init() {
        super.init();

        // Initialize list widget first (before search field, as setText triggers listener)
        this.itemListWidget = new ItemListWidget(this.client, this.width, this.height, 54, this.height - 32, 24);
        this.addSelectableChild(this.itemListWidget);

        // Cache all items
        this.allItems = new ArrayList<>(Registries.ITEM.stream().collect(Collectors.toList()));

        // Search field
        this.searchField = new TextFieldWidget(this.textRenderer, this.width / 2 - 100, 32, 200, 18,
                Text.translatable("config." + ASTTweaks.MOD_ID + ".search.placeholder"));
        this.searchField.setPlaceholder(Text.translatable("config." + ASTTweaks.MOD_ID + ".search.placeholder"));
        this.searchField.setChangedListener(text -> {
            this.searchText = text;
            rebuildList();
        });
        this.searchField.setText(this.searchText);
        this.addSelectableChild(this.searchField);

        rebuildList();

        // "Hide All" button
        this.addDrawableChild(ButtonWidget.builder(
                        Text.translatable("config.asttweaks.entityculling.hideAll"),
                        button -> {
                            if (config != null) {
                                config.hideAllItems(allItems);
                                rebuildList();
                            }
                        })
                .dimensions(this.width / 2 - 110, this.height - 27, 70, 20)
                .build());

        // "Show All" button
        this.addDrawableChild(ButtonWidget.builder(
                        Text.translatable("config.asttweaks.entityculling.showAll"),
                        button -> {
                            if (config != null) {
                                config.showAllItems();
                                rebuildList();
                            }
                        })
                .dimensions(this.width / 2 - 35, this.height - 27, 70, 20)
                .build());

        // "Done" button
        this.addDrawableChild(ButtonWidget.builder(
                        Text.translatable("gui.done"),
                        button -> this.client.setScreen(parent))
                .dimensions(this.width / 2 + 40, this.height - 27, 70, 20)
                .build());
    }

    private void rebuildList() {
        this.itemListWidget.children().clear();
        String lowerSearch = searchText.toLowerCase();

        allItems.stream()
                .filter(i -> searchText.isEmpty() ||
                        i.getName().getString().toLowerCase().contains(lowerSearch))
                .sorted((a, b) -> {
                    boolean aBlacklisted = config != null && config.isItemBlacklisted(a);
                    boolean bBlacklisted = config != null && config.isItemBlacklisted(b);
                    if (aBlacklisted != bBlacklisted) {
                        return aBlacklisted ? -1 : 1;
                    }
                    return a.getName().getString().compareTo(b.getName().getString());
                })
                .forEach(i -> itemListWidget.addEntry(new ItemEntry(i)));
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        this.renderBackground(matrices);
        this.itemListWidget.render(matrices, mouseX, mouseY, delta);

        int titleWidth = this.textRenderer.getWidth(this.title);
        this.textRenderer.drawWithShadow(matrices, this.title, this.width / 2 - titleWidth / 2, 10, 16777215);

        Text helpText = Text.translatable("config.asttweaks.entityculling.itemblacklist.help");
        int helpWidth = this.textRenderer.getWidth(helpText);
        this.textRenderer.drawWithShadow(matrices, helpText, this.width / 2 - helpWidth / 2, 20, 10526880);

        this.searchField.render(matrices, mouseX, mouseY, delta);

        super.render(matrices, mouseX, mouseY, delta);
    }

    class ItemListWidget extends AlwaysSelectedEntryListWidget<ItemEntry> {
        public ItemListWidget(MinecraftClient client, int width, int height, int top, int bottom, int itemHeight) {
            super(client, width, height, top, bottom, itemHeight);
        }

        @Override
        protected int getScrollbarPositionX() {
            return super.getScrollbarPositionX() + 20;
        }

        @Override
        public int getRowWidth() {
            return super.getRowWidth() + 50;
        }

        @Override
        public int addEntry(ItemEntry entry) {
            return super.addEntry(entry);
        }
    }

    class ItemEntry extends AlwaysSelectedEntryListWidget.Entry<ItemEntry> {
        private final Item item;
        private final ButtonWidget toggleButton;

        public ItemEntry(Item item) {
            this.item = item;

            this.toggleButton = ButtonWidget.builder(
                            Text.literal(""),
                            button -> {
                                if (config != null) {
                                    config.toggleItemBlacklist(item);
                                    updateButtonText();
                                }
                            })
                    .dimensions(0, 0, 60, 20)
                    .build();

            updateButtonText();
        }

        private void updateButtonText() {
            boolean blacklisted = config != null && config.isItemBlacklisted(item);
            this.toggleButton.setMessage(
                    Text.translatable(blacklisted ?
                            "config.asttweaks.entityculling.hidden" :
                            "config.asttweaks.entityculling.visible")
                            .formatted(blacklisted ? Formatting.RED : Formatting.GREEN)
            );
        }

        @Override
        public void render(MatrixStack matrices, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            MinecraftClient client = ItemEntityListScreen.this.client;

            // Item icon
            ItemStack stack = new ItemStack(item);
            client.getItemRenderer().renderInGuiWithOverrides(matrices, stack, x + 2, y + 2);

            // Item name
            Text itemName = item.getName();
            client.textRenderer.draw(matrices, itemName, x + 24, y + 6, 16777215);

            // Toggle button
            this.toggleButton.setPosition(x + entryWidth - 65, y + 2);
            this.toggleButton.render(matrices, mouseX, mouseY, tickDelta);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            return this.toggleButton.mouseClicked(mouseX, mouseY, button);
        }

        @Override
        public Text getNarration() {
            return item.getName();
        }
    }
}

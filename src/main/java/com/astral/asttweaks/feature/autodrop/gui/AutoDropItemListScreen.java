package com.astral.asttweaks.feature.autodrop.gui;

import com.astral.asttweaks.feature.FeatureManager;
import com.astral.asttweaks.feature.autodrop.AutoDropConfig;
import com.astral.asttweaks.feature.autodrop.AutoDropFeature;
import com.astral.asttweaks.util.ItemDisplay;
import com.astral.asttweaks.util.McClient;
import com.astral.asttweaks.util.gui.CompatListEntry;
import com.astral.asttweaks.util.gui.CompatScreen;
import com.astral.asttweaks.util.gui.CompatSelectionList;
import com.astral.asttweaks.util.gui.Gfx;
import com.astral.asttweaks.util.gui.GuiEvents;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Screen for configuring Auto Drop item exclusion list.
 * Items in this list will NOT be dropped.
 */
public class AutoDropItemListScreen extends CompatScreen {
    private final Screen parent;
    private ItemListWidget itemListWidget;
    private final AutoDropConfig config;
    private EditBox searchField;
    private String searchText = "";
    private List<Item> allItems;

    public AutoDropItemListScreen(Screen parent) {
        super(Component.translatable("config.asttweaks.autodrop.itemlist.title"));
        this.parent = parent;
        AutoDropFeature feature = (AutoDropFeature) FeatureManager.getInstance().getFeature("autodrop");
        this.config = feature != null ? feature.getConfig() : null;
    }

    @Override
    protected void init() {
        super.init();

        this.itemListWidget = new ItemListWidget(this.minecraft, this.width, this.height, 54, this.height - 32, 36);
        this.addWidget(this.itemListWidget);

        this.allItems = new ArrayList<>(BuiltInRegistries.ITEM.stream()
                .filter(item -> item != Items.AIR)
                .collect(Collectors.toList()));

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

    private void rebuildList() {
        this.itemListWidget.clearAll();
        String lowerSearch = searchText.toLowerCase();

        allItems.stream()
                .filter(item -> searchText.isEmpty() ||
                        ItemDisplay.name(item).getString().toLowerCase().contains(lowerSearch) ||
                        BuiltInRegistries.ITEM.getKey(item).toString().toLowerCase().contains(lowerSearch))
                .sorted((a, b) -> {
                    boolean aEx = config != null && config.isItemExcluded(a);
                    boolean bEx = config != null && config.isItemExcluded(b);
                    if (aEx != bEx) return aEx ? -1 : 1;
                    return ItemDisplay.name(a).getString().compareTo(ItemDisplay.name(b).getString());
                })
                .forEach(item -> itemListWidget.addEntry(new ItemEntry(item)));
    }

    @Override
    protected void draw(Gfx gfx, int mouseX, int mouseY, float delta) {
        gfx.widget(this.itemListWidget, mouseX, mouseY, delta);

        int titleWidth = this.font.width(this.title);
        gfx.text(this.font, this.title, this.width / 2 - titleWidth / 2, 10, 0xFFFFFFFF);

        Component help = Component.translatable("config.asttweaks.autodrop.itemlist.help");
        int helpWidth = this.font.width(help);
        gfx.text(this.font, help, this.width / 2 - helpWidth / 2, 20, 0xFFA0A0A0);

        gfx.widget(this.searchField, mouseX, mouseY, delta);
        renderWidgets(gfx, mouseX, mouseY, delta);
    }

    class ItemListWidget extends CompatSelectionList<ItemEntry> {
        public ItemListWidget(Minecraft c, int w, int h, int t, int b, int ih) {
            super(c, w, h, t, b, ih);
        }
        @Override public int getRowWidth() { return super.getRowWidth() + 50; }
        @Override public int addEntry(ItemEntry e) { return super.addEntry(e); }
    }

    class ItemEntry extends CompatListEntry<ItemEntry> {
        private final Item item;
        private final ItemStack stack;
        private final Button toggleButton;

        public ItemEntry(Item item) {
            this.item = item;
            this.stack = ItemDisplay.stackOrEmpty(item);
            this.toggleButton = Button.builder(
                            Component.literal(""),
                            b -> {
                                if (config != null) {
                                    config.toggleItemExclusion(item);
                                    updateButtonText();
                                }
                            })
                    .bounds(0, 0, 80, 20)
                    .build();
            updateButtonText();
        }

        private void updateButtonText() {
            boolean excluded = config != null && config.isItemExcluded(item);
            this.toggleButton.setMessage(
                    Component.translatable(excluded ? "config.asttweaks.autodrop.protected" : "config.asttweaks.autodrop.willdrop")
                            .withStyle(excluded ? ChatFormatting.GREEN : ChatFormatting.RED)
            );
        }

        @Override
        protected void drawEntry(Gfx gfx, int x, int y, int entryWidth, int entryHeight,
                                 int mx, int my, boolean hovered, float d) {
            int w = entryWidth;
            Minecraft client = AutoDropItemListScreen.this.minecraft;
            gfx.item(stack, x + 5, y + 8);
            gfx.itemDecorations(client.font, stack, x + 5, y + 8);

            gfx.text(client.font, ItemDisplay.name(item), x + 30, y + 5, 0xFFFFFFFF, false);
            Component itemId = Component.literal(BuiltInRegistries.ITEM.getKey(item).toString()).withStyle(ChatFormatting.GRAY);
            gfx.text(client.font, itemId, x + 30, y + 16, 0xFFA0A0A0, false);

            this.toggleButton.setPosition(x + w - 85, y + 7);
            gfx.widget(this.toggleButton, mx, my, d);
        }

        @Override
        protected boolean onMouseClicked(double mouseX, double mouseY, int button) {
            return GuiEvents.click(this.toggleButton, mouseX, mouseY, button);
        }

        @Override
        public Component getNarration() {
            return ItemDisplay.name(item);
        }
    }
}

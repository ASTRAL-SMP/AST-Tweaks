package com.astral.asttweaks.feature.autodrop.gui;

import com.astral.asttweaks.feature.FeatureManager;
import com.astral.asttweaks.feature.autodrop.AutoDropConfig;
import com.astral.asttweaks.feature.autodrop.AutoDropFeature;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Screen for configuring Auto Drop item exclusion list.
 * Items in this list will NOT be dropped.
 */
public class AutoDropItemListScreen extends Screen {
    private final Screen parent;
    private ItemListWidget itemListWidget;
    private final AutoDropConfig config;
    private TextFieldWidget searchField;
    private String searchText = "";
    private List<Item> allItems;

    public AutoDropItemListScreen(Screen parent) {
        super(Text.translatable("config.asttweaks.autodrop.itemlist.title"));
        this.parent = parent;
        AutoDropFeature feature = (AutoDropFeature) FeatureManager.getInstance().getFeature("autodrop");
        this.config = feature != null ? feature.getConfig() : null;
    }

    @Override
    protected void init() {
        super.init();

        this.itemListWidget = new ItemListWidget(this.client, this.width, this.height, 54, this.height - 32, 36);
        this.addSelectableChild(this.itemListWidget);

        this.allItems = new ArrayList<>(Registries.ITEM.stream()
                .filter(item -> item != Items.AIR)
                .collect(Collectors.toList()));

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

    private void rebuildList() {
        this.itemListWidget.children().clear();
        String lowerSearch = searchText.toLowerCase();

        allItems.stream()
                .filter(item -> searchText.isEmpty() ||
                        item.getName().getString().toLowerCase().contains(lowerSearch) ||
                        Registries.ITEM.getId(item).toString().toLowerCase().contains(lowerSearch))
                .sorted((a, b) -> {
                    boolean aEx = config != null && config.isItemExcluded(a);
                    boolean bEx = config != null && config.isItemExcluded(b);
                    if (aEx != bEx) return aEx ? -1 : 1;
                    return a.getName().getString().compareTo(b.getName().getString());
                })
                .forEach(item -> itemListWidget.addEntry(new ItemEntry(item)));
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        this.renderBackground(matrices);
        this.itemListWidget.render(matrices, mouseX, mouseY, delta);

        int titleWidth = this.textRenderer.getWidth(this.title);
        this.textRenderer.drawWithShadow(matrices, this.title, this.width / 2 - titleWidth / 2, 10, 0xFFFFFF);

        Text help = Text.translatable("config.asttweaks.autodrop.itemlist.help");
        int helpWidth = this.textRenderer.getWidth(help);
        this.textRenderer.drawWithShadow(matrices, help, this.width / 2 - helpWidth / 2, 20, 0xA0A0A0);

        this.searchField.render(matrices, mouseX, mouseY, delta);
        super.render(matrices, mouseX, mouseY, delta);
    }

    class ItemListWidget extends AlwaysSelectedEntryListWidget<ItemEntry> {
        public ItemListWidget(MinecraftClient c, int w, int h, int t, int b, int ih) {
            super(c, w, h, t, b, ih);
        }
        @Override protected int getScrollbarPositionX() { return super.getScrollbarPositionX() + 20; }
        @Override public int getRowWidth() { return super.getRowWidth() + 50; }
        @Override public int addEntry(ItemEntry e) { return super.addEntry(e); }
    }

    class ItemEntry extends AlwaysSelectedEntryListWidget.Entry<ItemEntry> {
        private final Item item;
        private final ItemStack stack;
        private final ButtonWidget toggleButton;

        public ItemEntry(Item item) {
            this.item = item;
            this.stack = new ItemStack(item);
            this.toggleButton = ButtonWidget.builder(
                            Text.literal(""),
                            b -> {
                                if (config != null) {
                                    config.toggleItemExclusion(item);
                                    updateButtonText();
                                }
                            })
                    .dimensions(0, 0, 80, 20)
                    .build();
            updateButtonText();
        }

        private void updateButtonText() {
            boolean excluded = config != null && config.isItemExcluded(item);
            this.toggleButton.setMessage(
                    Text.translatable(excluded ? "config.asttweaks.autodrop.protected" : "config.asttweaks.autodrop.willdrop")
                            .formatted(excluded ? Formatting.GREEN : Formatting.RED)
            );
        }

        @Override
        public void render(MatrixStack m, int i, int y, int x, int w, int h, int mx, int my, boolean hovered, float d) {
            MinecraftClient client = AutoDropItemListScreen.this.client;
            client.getItemRenderer().renderInGuiWithOverrides(m, stack, x + 5, y + 8);
            client.getItemRenderer().renderGuiItemOverlay(m, client.textRenderer, stack, x + 5, y + 8);

            client.textRenderer.draw(m, item.getName(), x + 30, y + 5, 0xFFFFFF);
            Text itemId = Text.literal(Registries.ITEM.getId(item).toString()).formatted(Formatting.GRAY);
            client.textRenderer.draw(m, itemId, x + 30, y + 16, 0xA0A0A0);

            this.toggleButton.setPosition(x + w - 85, y + 7);
            this.toggleButton.render(m, mx, my, d);
        }

        @Override
        public boolean mouseClicked(double mx, double my, int b) {
            return this.toggleButton.mouseClicked(mx, my, b);
        }

        @Override
        public Text getNarration() {
            return Text.literal(item.getName().getString());
        }
    }
}

package com.astral.asttweaks.feature.massgrindstone.gui;

import com.astral.asttweaks.feature.FeatureManager;
import com.astral.asttweaks.feature.massgrindstone.MassGrindstoneConfig;
import com.astral.asttweaks.feature.massgrindstone.MassGrindstoneFeature;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Screen for configuring mass grindstone item whitelist/blacklist.
 */
public class GrindstoneItemListScreen extends Screen {
    private final Screen parent;
    private GrindstoneItemListWidget itemListWidget;
    private final MassGrindstoneConfig config;

    public GrindstoneItemListScreen(Screen parent) {
        super(Text.translatable("config.asttweaks.massgrindstone.itemlist.title"));
        this.parent = parent;
        MassGrindstoneFeature feature = (MassGrindstoneFeature) FeatureManager.getInstance().getFeature("massgrindstone");
        this.config = feature != null ? feature.getConfig() : null;
    }

    @Override
    protected void init() {
        super.init();

        this.itemListWidget = new GrindstoneItemListWidget(this.client, this.width, this.height, 32, this.height - 32, 36);
        this.addSelectableChild(this.itemListWidget);

        // Get all enchantable items (items that can hold enchantments)
        List<Item> enchantableItems = Registries.ITEM.stream()
                .filter(item -> {
                    ItemStack stack = new ItemStack(item);
                    // Include items that can be enchanted or are enchanted books
                    return stack.isEnchantable() || item == Items.ENCHANTED_BOOK;
                })
                .sorted((a, b) -> {
                    boolean aInList = config != null && config.isInList(a);
                    boolean bInList = config != null && config.isInList(b);
                    if (aInList != bInList) {
                        return aInList ? -1 : 1;
                    }
                    return a.getName().getString().compareTo(b.getName().getString());
                })
                .collect(Collectors.toList());

        for (Item item : enchantableItems) {
            this.itemListWidget.addEntry(new GrindstoneItemEntry(item));
        }

        this.addDrawableChild(ButtonWidget.builder(
                        Text.translatable("gui.done"),
                        button -> this.client.setScreen(parent))
                .dimensions(this.width / 2 - 100, this.height - 27, 200, 20)
                .build());
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        this.renderBackground(matrices);
        this.itemListWidget.render(matrices, mouseX, mouseY, delta);

        int titleWidth = this.textRenderer.getWidth(this.title);
        this.textRenderer.drawWithShadow(matrices, this.title, this.width / 2 - titleWidth / 2, 10, 16777215);

        // Show mode indicator
        Text modeText;
        if (config != null && config.isWhitelistMode()) {
            modeText = Text.translatable("config.asttweaks.massgrindstone.itemlist.help.whitelist");
        } else {
            modeText = Text.translatable("config.asttweaks.massgrindstone.itemlist.help.blacklist");
        }
        int helpWidth = this.textRenderer.getWidth(modeText);
        this.textRenderer.drawWithShadow(matrices, modeText, this.width / 2 - helpWidth / 2, 20, 10526880);

        super.render(matrices, mouseX, mouseY, delta);
    }

    class GrindstoneItemListWidget extends AlwaysSelectedEntryListWidget<GrindstoneItemEntry> {
        public GrindstoneItemListWidget(MinecraftClient client, int width, int height, int top, int bottom, int itemHeight) {
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
        public int addEntry(GrindstoneItemEntry entry) {
            return super.addEntry(entry);
        }
    }

    class GrindstoneItemEntry extends AlwaysSelectedEntryListWidget.Entry<GrindstoneItemEntry> {
        private final Item item;
        private final ItemStack stack;
        private final ButtonWidget toggleButton;

        public GrindstoneItemEntry(Item item) {
            this.item = item;
            this.stack = new ItemStack(item);

            this.toggleButton = ButtonWidget.builder(
                            Text.literal(""),
                            button -> {
                                if (config != null) {
                                    config.toggleItem(item);
                                    updateButtonText();
                                }
                            })
                    .dimensions(0, 0, 80, 20)
                    .build();

            updateButtonText();
        }

        private void updateButtonText() {
            boolean inList = config != null && config.isInList(item);
            boolean whitelistMode = config != null && config.isWhitelistMode();

            if (whitelistMode) {
                // Whitelist mode: items in list are processed
                this.toggleButton.setMessage(
                        Text.translatable(inList ? "config.asttweaks.massgrindstone.willprocess" : "config.asttweaks.massgrindstone.wontprocess")
                                .formatted(inList ? Formatting.GREEN : Formatting.GRAY)
                );
            } else {
                // Blacklist mode: items in list are NOT processed
                this.toggleButton.setMessage(
                        Text.translatable(inList ? "config.asttweaks.massgrindstone.wontprocess" : "config.asttweaks.massgrindstone.willprocess")
                                .formatted(inList ? Formatting.RED : Formatting.GREEN)
                );
            }
        }

        @Override
        public void render(MatrixStack matrices, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            MinecraftClient client = GrindstoneItemListScreen.this.client;

            client.getItemRenderer().renderInGuiWithOverrides(matrices, stack, x + 5, y + 8);
            client.getItemRenderer().renderGuiItemOverlay(matrices, client.textRenderer, stack, x + 5, y + 8);

            Text itemName = item.getName();
            client.textRenderer.draw(matrices, itemName, x + 30, y + 5, 16777215);

            // Show enchantability info
            Text enchantInfo;
            if (item == Items.ENCHANTED_BOOK) {
                enchantInfo = Text.translatable("config.asttweaks.massgrindstone.enchantedbook")
                        .formatted(Formatting.LIGHT_PURPLE);
            } else {
                int enchantability = stack.getItem().getEnchantability();
                enchantInfo = Text.literal(String.format("Enchantability: %d", enchantability))
                        .formatted(Formatting.GRAY);
            }
            client.textRenderer.draw(matrices, enchantInfo, x + 30, y + 16, 10526880);

            this.toggleButton.setPosition(x + entryWidth - 85, y + 7);
            this.toggleButton.render(matrices, mouseX, mouseY, tickDelta);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            return this.toggleButton.mouseClicked(mouseX, mouseY, button);
        }

        @Override
        public Text getNarration() {
            return Text.literal(item.getName().getString());
        }
    }
}

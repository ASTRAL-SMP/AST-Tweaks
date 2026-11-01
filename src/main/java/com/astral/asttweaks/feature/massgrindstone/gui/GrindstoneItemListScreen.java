package com.astral.asttweaks.feature.massgrindstone.gui;

import com.astral.asttweaks.feature.FeatureManager;
import com.astral.asttweaks.feature.massgrindstone.MassGrindstoneConfig;
import com.astral.asttweaks.feature.massgrindstone.MassGrindstoneFeature;
import com.astral.asttweaks.util.ItemDisplay;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantable;

/**
 * Screen for configuring mass grindstone item whitelist/blacklist.
 */
public class GrindstoneItemListScreen extends Screen {
    private final Screen parent;
    private GrindstoneItemListWidget itemListWidget;
    private final MassGrindstoneConfig config;

    public GrindstoneItemListScreen(Screen parent) {
        super(Component.translatable("config.asttweaks.massgrindstone.itemlist.title"));
        this.parent = parent;
        MassGrindstoneFeature feature = (MassGrindstoneFeature) FeatureManager.getInstance().getFeature("massgrindstone");
        this.config = feature != null ? feature.getConfig() : null;
    }

    @Override
    protected void init() {
        super.init();

        this.itemListWidget = new GrindstoneItemListWidget(this.minecraft, this.width, this.height - 32 - 32, 32, 36);
        this.addWidget(this.itemListWidget);

        // Get all enchantable items (items that can hold enchantments)
        List<Item> enchantableItems = BuiltInRegistries.ITEM.stream()
                .filter(item -> {
                    ItemStack stack = ItemDisplay.stackOrEmpty(item);
                    // Include items that can be enchanted or are enchanted books
                    return stack.isEnchantable() || item == Items.ENCHANTED_BOOK;
                })
                .sorted((a, b) -> {
                    boolean aInList = config != null && config.isInList(a);
                    boolean bInList = config != null && config.isInList(b);
                    if (aInList != bInList) {
                        return aInList ? -1 : 1;
                    }
                    return ItemDisplay.name(a).getString().compareTo(ItemDisplay.name(b).getString());
                })
                .collect(Collectors.toList());

        for (Item item : enchantableItems) {
            this.itemListWidget.addEntry(new GrindstoneItemEntry(item));
        }

        this.addRenderableWidget(Button.builder(
                        Component.translatable("gui.done"),
                        button -> this.minecraft.setScreen(parent))
                .bounds(this.width / 2 - 100, this.height - 27, 200, 20)
                .build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        this.itemListWidget.extractRenderState(graphics, mouseX, mouseY, delta);

        if (!ItemDisplay.itemDataLoaded()) {
            graphics.centeredText(this.font, ItemDisplay.NEEDS_WORLD, this.width / 2, this.height / 2, 0xFFFFAA00);
        }

        int titleWidth = this.font.width(this.title);
        graphics.text(this.font, this.title, this.width / 2 - titleWidth / 2, 10, 0xFFFFFFFF);

        // Show mode indicator
        Component modeText;
        if (config != null && config.isWhitelistMode()) {
            modeText = Component.translatable("config.asttweaks.massgrindstone.itemlist.help.whitelist");
        } else {
            modeText = Component.translatable("config.asttweaks.massgrindstone.itemlist.help.blacklist");
        }
        int helpWidth = this.font.width(modeText);
        graphics.text(this.font, modeText, this.width / 2 - helpWidth / 2, 20, 0xFFA0A0A0);

        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    class GrindstoneItemListWidget extends ObjectSelectionList<GrindstoneItemEntry> {
        public GrindstoneItemListWidget(Minecraft client, int width, int height, int top, int itemHeight) {
            super(client, width, height, top, itemHeight);
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

    class GrindstoneItemEntry extends ObjectSelectionList.Entry<GrindstoneItemEntry> {
        private final Item item;
        private final ItemStack stack;
        private final Button toggleButton;

        public GrindstoneItemEntry(Item item) {
            this.item = item;
            this.stack = ItemDisplay.stackOrEmpty(item);

            this.toggleButton = Button.builder(
                            Component.literal(""),
                            button -> {
                                if (config != null) {
                                    config.toggleItem(item);
                                    updateButtonText();
                                }
                            })
                    .bounds(0, 0, 80, 20)
                    .build();

            updateButtonText();
        }

        private void updateButtonText() {
            boolean inList = config != null && config.isInList(item);
            boolean whitelistMode = config != null && config.isWhitelistMode();

            if (whitelistMode) {
                // Whitelist mode: items in list are processed
                this.toggleButton.setMessage(
                        Component.translatable(inList ? "config.asttweaks.massgrindstone.willprocess" : "config.asttweaks.massgrindstone.wontprocess")
                                .withStyle(inList ? ChatFormatting.GREEN : ChatFormatting.GRAY)
                );
            } else {
                // Blacklist mode: items in list are NOT processed
                this.toggleButton.setMessage(
                        Component.translatable(inList ? "config.asttweaks.massgrindstone.wontprocess" : "config.asttweaks.massgrindstone.willprocess")
                                .withStyle(inList ? ChatFormatting.RED : ChatFormatting.GREEN)
                );
            }
        }

        @Override
        public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            int x = this.getContentX();
            int y = this.getContentY();
            int entryWidth = this.getContentWidth();
            Minecraft client = GrindstoneItemListScreen.this.minecraft;

            graphics.item(stack, x + 5, y + 8);
            graphics.itemDecorations(client.font, stack, x + 5, y + 8);

            Component itemName = ItemDisplay.name(item);
            graphics.text(client.font, itemName, x + 30, y + 5, 0xFFFFFFFF, false);

            // Show enchantability info
            Component enchantInfo;
            if (item == Items.ENCHANTED_BOOK) {
                enchantInfo = Component.translatable("config.asttweaks.massgrindstone.enchantedbook")
                        .withStyle(ChatFormatting.LIGHT_PURPLE);
            } else {
                Enchantable enchantable = stack.get(DataComponents.ENCHANTABLE);
                int enchantability = enchantable != null ? enchantable.value() : 0;
                enchantInfo = Component.literal(String.format("Enchantability: %d", enchantability))
                        .withStyle(ChatFormatting.GRAY);
            }
            graphics.text(client.font, enchantInfo, x + 30, y + 16, 0xFFA0A0A0, false);

            this.toggleButton.setPosition(x + entryWidth - 85, y + 7);
            this.toggleButton.extractRenderState(graphics, mouseX, mouseY, tickDelta);
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
            return this.toggleButton.mouseClicked(event, doubleClick);
        }

        @Override
        public Component getNarration() {
            return ItemDisplay.name(item);
        }
    }
}

package com.astral.asttweaks.feature.massgrindstone.gui;

import com.astral.asttweaks.feature.FeatureManager;
import com.astral.asttweaks.feature.massgrindstone.MassGrindstoneConfig;
import com.astral.asttweaks.feature.massgrindstone.MassGrindstoneFeature;
import com.astral.asttweaks.util.ItemDisplay;
import com.astral.asttweaks.util.McClient;
import com.astral.asttweaks.util.gui.CompatListEntry;
import com.astral.asttweaks.util.gui.CompatScreen;
import com.astral.asttweaks.util.gui.CompatSelectionList;
import com.astral.asttweaks.util.gui.Gfx;
import com.astral.asttweaks.util.gui.GuiEvents;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
//? if >=26.1 {
import net.minecraft.core.component.DataComponents;
//?}
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
//? if >=26.1 {
import net.minecraft.world.item.enchantment.Enchantable;
//?}

/**
 * Screen for configuring mass grindstone item whitelist/blacklist.
 */
public class GrindstoneItemListScreen extends CompatScreen {
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

        this.itemListWidget = new GrindstoneItemListWidget(this.minecraft, this.width, this.height, 32, this.height - 32, 36);
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
                        button -> McClient.setScreen(parent))
                .bounds(this.width / 2 - 100, this.height - 27, 200, 20)
                .build());
    }

    @Override
    protected void draw(Gfx gfx, int mouseX, int mouseY, float delta) {
        gfx.widget(this.itemListWidget, mouseX, mouseY, delta);

        if (!ItemDisplay.itemDataLoaded()) {
            gfx.centeredText(this.font, ItemDisplay.NEEDS_WORLD, this.width / 2, this.height / 2, 0xFFFFAA00);
        }

        int titleWidth = this.font.width(this.title);
        gfx.text(this.font, this.title, this.width / 2 - titleWidth / 2, 10, 0xFFFFFFFF);

        // Show mode indicator
        Component modeText;
        if (config != null && config.isWhitelistMode()) {
            modeText = Component.translatable("config.asttweaks.massgrindstone.itemlist.help.whitelist");
        } else {
            modeText = Component.translatable("config.asttweaks.massgrindstone.itemlist.help.blacklist");
        }
        int helpWidth = this.font.width(modeText);
        gfx.text(this.font, modeText, this.width / 2 - helpWidth / 2, 20, 0xFFA0A0A0);

        renderWidgets(gfx, mouseX, mouseY, delta);
    }

    class GrindstoneItemListWidget extends CompatSelectionList<GrindstoneItemEntry> {
        public GrindstoneItemListWidget(Minecraft client, int width, int height, int top, int bottom, int itemHeight) {
            super(client, width, height, top, bottom, itemHeight);
        }

        @Override
        public int getRowWidth() {
            return super.getRowWidth() + 50;
        }

    }

    class GrindstoneItemEntry extends CompatListEntry<GrindstoneItemEntry> {
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
        protected void drawEntry(Gfx gfx, int x, int y, int entryWidth, int entryHeight,
                                 int mouseX, int mouseY, boolean hovered, float tickDelta) {
            Minecraft client = GrindstoneItemListScreen.this.minecraft;

            gfx.item(stack, x + 5, y + 8);
            gfx.itemDecorations(client.font, stack, x + 5, y + 8);

            Component itemName = ItemDisplay.name(item);
            gfx.text(client.font, itemName, x + 30, y + 5, 0xFFFFFFFF, false);

            // Show enchantability info
            Component enchantInfo;
            if (item == Items.ENCHANTED_BOOK) {
                enchantInfo = Component.translatable("config.asttweaks.massgrindstone.enchantedbook")
                        .withStyle(ChatFormatting.LIGHT_PURPLE);
            } else {
                //? if >=26.1 {
                Enchantable enchantable = stack.get(DataComponents.ENCHANTABLE);
                int enchantability = enchantable != null ? enchantable.value() : 0;
                //?} else {
                /*int enchantability = item.getEnchantmentValue();
                *///?}
                enchantInfo = Component.literal(String.format("Enchantability: %d", enchantability))
                        .withStyle(ChatFormatting.GRAY);
            }
            gfx.text(client.font, enchantInfo, x + 30, y + 16, 0xFFA0A0A0, false);

            this.toggleButton.setPosition(x + entryWidth - 85, y + 7);
            gfx.widget(this.toggleButton, mouseX, mouseY, tickDelta);
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

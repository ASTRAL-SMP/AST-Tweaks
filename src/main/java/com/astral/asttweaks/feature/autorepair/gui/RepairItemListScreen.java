package com.astral.asttweaks.feature.autorepair.gui;

import com.astral.asttweaks.feature.FeatureManager;
import com.astral.asttweaks.feature.autorepair.AutoRepairConfig;
import com.astral.asttweaks.feature.autorepair.AutoRepairFeature;
import com.astral.asttweaks.util.ItemDisplay;
import java.util.List;
import java.util.stream.Collectors;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Screen for configuring repair item whitelist/blacklist.
 */
public class RepairItemListScreen extends Screen {
    private final Screen parent;
    private RepairItemListWidget itemListWidget;
    private final AutoRepairConfig config;

    public RepairItemListScreen(Screen parent) {
        super(Component.translatable("config.asttweaks.autorepair.itemlist.title"));
        this.parent = parent;
        AutoRepairFeature feature = (AutoRepairFeature) FeatureManager.getInstance().getFeature("autorepair");
        this.config = feature != null ? feature.getConfig() : null;
    }

    @Override
    protected void init() {
        super.init();

        this.itemListWidget = new RepairItemListWidget(this.minecraft, this.width, this.height - 32 - 32, 32, 36);
        this.addWidget(this.itemListWidget);

        // Get all damageable items (tools, armor, weapons)
        List<Item> repairableItems = BuiltInRegistries.ITEM.stream()
                .filter(item -> {
                    ItemStack stack = ItemDisplay.stackOrEmpty(item);
                    return stack.isDamageableItem();
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

        for (Item item : repairableItems) {
            this.itemListWidget.addEntry(new RepairItemEntry(item));
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
            modeText = Component.translatable("config.asttweaks.autorepair.itemlist.help.whitelist");
        } else {
            modeText = Component.translatable("config.asttweaks.autorepair.itemlist.help.blacklist");
        }
        int helpWidth = this.font.width(modeText);
        graphics.text(this.font, modeText, this.width / 2 - helpWidth / 2, 20, 0xFFA0A0A0);

        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    class RepairItemListWidget extends ObjectSelectionList<RepairItemEntry> {
        public RepairItemListWidget(Minecraft client, int width, int height, int top, int itemHeight) {
            super(client, width, height, top, itemHeight);
        }

        @Override
        public int getRowWidth() {
            return super.getRowWidth() + 50;
        }

        @Override
        public int addEntry(RepairItemEntry entry) {
            return super.addEntry(entry);
        }
    }

    class RepairItemEntry extends ObjectSelectionList.Entry<RepairItemEntry> {
        private final Item item;
        private final ItemStack stack;
        private final Button toggleButton;

        public RepairItemEntry(Item item) {
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
                // Whitelist mode: items in list are repaired
                this.toggleButton.setMessage(
                        Component.translatable(inList ? "config.asttweaks.autorepair.willrepair" : "config.asttweaks.autorepair.wontrepair")
                                .withStyle(inList ? ChatFormatting.GREEN : ChatFormatting.GRAY)
                );
            } else {
                // Blacklist mode: items in list are NOT repaired
                this.toggleButton.setMessage(
                        Component.translatable(inList ? "config.asttweaks.autorepair.wontrepair" : "config.asttweaks.autorepair.willrepair")
                                .withStyle(inList ? ChatFormatting.RED : ChatFormatting.GREEN)
                );
            }
        }

        @Override
        public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            int x = this.getContentX();
            int y = this.getContentY();
            int entryWidth = this.getContentWidth();
            Minecraft client = RepairItemListScreen.this.minecraft;

            graphics.item(stack, x + 5, y + 8);
            graphics.itemDecorations(client.font, stack, x + 5, y + 8);

            Component itemName = ItemDisplay.name(item);
            graphics.text(client.font, itemName, x + 30, y + 5, 0xFFFFFFFF, false);

            // Show max durability
            if (stack.isDamageableItem()) {
                Component durabilityInfo = Component.literal(String.format("Max Durability: %d", stack.getMaxDamage()))
                        .withStyle(ChatFormatting.GRAY);
                graphics.text(client.font, durabilityInfo, x + 30, y + 16, 0xFFA0A0A0, false);
            }

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

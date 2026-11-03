package com.astral.asttweaks.feature.autorepair.gui;

import com.astral.asttweaks.feature.FeatureManager;
import com.astral.asttweaks.feature.autorepair.AutoRepairConfig;
import com.astral.asttweaks.feature.autorepair.AutoRepairFeature;
import com.astral.asttweaks.util.ItemDisplay;
import com.astral.asttweaks.util.McClient;
import com.astral.asttweaks.util.gui.CompatListEntry;
import com.astral.asttweaks.util.gui.CompatScreen;
import com.astral.asttweaks.util.gui.CompatSelectionList;
import com.astral.asttweaks.util.gui.Gfx;
import com.astral.asttweaks.util.gui.GuiEvents;
import java.util.List;
import java.util.stream.Collectors;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Screen for configuring repair item whitelist/blacklist.
 */
public class RepairItemListScreen extends CompatScreen {
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

        this.itemListWidget = new RepairItemListWidget(this.minecraft, this.width, this.height, 32, this.height - 32, 36);
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
            modeText = Component.translatable("config.asttweaks.autorepair.itemlist.help.whitelist");
        } else {
            modeText = Component.translatable("config.asttweaks.autorepair.itemlist.help.blacklist");
        }
        int helpWidth = this.font.width(modeText);
        gfx.text(this.font, modeText, this.width / 2 - helpWidth / 2, 20, 0xFFA0A0A0);

        renderWidgets(gfx, mouseX, mouseY, delta);
    }

    class RepairItemListWidget extends CompatSelectionList<RepairItemEntry> {
        public RepairItemListWidget(Minecraft client, int width, int height, int top, int bottom, int itemHeight) {
            super(client, width, height, top, bottom, itemHeight);
        }

        @Override
        public int getRowWidth() {
            return super.getRowWidth() + 50;
        }

    }

    class RepairItemEntry extends CompatListEntry<RepairItemEntry> {
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
        protected void drawEntry(Gfx gfx, int x, int y, int entryWidth, int entryHeight,
                                 int mouseX, int mouseY, boolean hovered, float tickDelta) {
            Minecraft client = RepairItemListScreen.this.minecraft;

            gfx.item(stack, x + 5, y + 8);
            gfx.itemDecorations(client.font, stack, x + 5, y + 8);

            Component itemName = ItemDisplay.name(item);
            gfx.text(client.font, itemName, x + 30, y + 5, 0xFFFFFFFF, false);

            // Show max durability
            if (stack.isDamageableItem()) {
                Component durabilityInfo = Component.literal(String.format("Max Durability: %d", stack.getMaxDamage()))
                        .withStyle(ChatFormatting.GRAY);
                gfx.text(client.font, durabilityInfo, x + 30, y + 16, 0xFFA0A0A0, false);
            }

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

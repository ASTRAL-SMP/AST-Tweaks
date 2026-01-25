package com.astral.asttweaks.feature.autorepair.gui;

import com.astral.asttweaks.feature.FeatureManager;
import com.astral.asttweaks.feature.autorepair.AutoRepairConfig;
import com.astral.asttweaks.feature.autorepair.AutoRepairFeature;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Screen for configuring repair item whitelist/blacklist.
 */
public class RepairItemListScreen extends Screen {
    private final Screen parent;
    private RepairItemListWidget itemListWidget;
    private final AutoRepairConfig config;

    public RepairItemListScreen(Screen parent) {
        super(Text.translatable("config.asttweaks.autorepair.itemlist.title"));
        this.parent = parent;
        AutoRepairFeature feature = (AutoRepairFeature) FeatureManager.getInstance().getFeature("autorepair");
        this.config = feature != null ? feature.getConfig() : null;
    }

    @Override
    protected void init() {
        super.init();

        this.itemListWidget = new RepairItemListWidget(this.client, this.width, this.height, 32, this.height - 32, 36);
        this.addSelectableChild(this.itemListWidget);

        // Get all damageable items (tools, armor, weapons)
        List<Item> repairableItems = Registries.ITEM.stream()
                .filter(item -> {
                    ItemStack stack = new ItemStack(item);
                    return stack.isDamageable();
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

        for (Item item : repairableItems) {
            this.itemListWidget.addEntry(new RepairItemEntry(item));
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
            modeText = Text.translatable("config.asttweaks.autorepair.itemlist.help.whitelist");
        } else {
            modeText = Text.translatable("config.asttweaks.autorepair.itemlist.help.blacklist");
        }
        int helpWidth = this.textRenderer.getWidth(modeText);
        this.textRenderer.drawWithShadow(matrices, modeText, this.width / 2 - helpWidth / 2, 20, 10526880);

        super.render(matrices, mouseX, mouseY, delta);
    }

    class RepairItemListWidget extends AlwaysSelectedEntryListWidget<RepairItemEntry> {
        public RepairItemListWidget(MinecraftClient client, int width, int height, int top, int bottom, int itemHeight) {
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
        public int addEntry(RepairItemEntry entry) {
            return super.addEntry(entry);
        }
    }

    class RepairItemEntry extends AlwaysSelectedEntryListWidget.Entry<RepairItemEntry> {
        private final Item item;
        private final ItemStack stack;
        private final ButtonWidget toggleButton;

        public RepairItemEntry(Item item) {
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
                // Whitelist mode: items in list are repaired
                this.toggleButton.setMessage(
                        Text.translatable(inList ? "config.asttweaks.autorepair.willrepair" : "config.asttweaks.autorepair.wontrepair")
                                .formatted(inList ? Formatting.GREEN : Formatting.GRAY)
                );
            } else {
                // Blacklist mode: items in list are NOT repaired
                this.toggleButton.setMessage(
                        Text.translatable(inList ? "config.asttweaks.autorepair.wontrepair" : "config.asttweaks.autorepair.willrepair")
                                .formatted(inList ? Formatting.RED : Formatting.GREEN)
                );
            }
        }

        @Override
        public void render(MatrixStack matrices, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            MinecraftClient client = RepairItemListScreen.this.client;

            client.getItemRenderer().renderInGuiWithOverrides(matrices, stack, x + 5, y + 8);
            client.getItemRenderer().renderGuiItemOverlay(matrices, client.textRenderer, stack, x + 5, y + 8);

            Text itemName = item.getName();
            client.textRenderer.draw(matrices, itemName, x + 30, y + 5, 16777215);

            // Show max durability
            if (stack.isDamageable()) {
                Text durabilityInfo = Text.literal(String.format("Max Durability: %d", stack.getMaxDamage()))
                        .formatted(Formatting.GRAY);
                client.textRenderer.draw(matrices, durabilityInfo, x + 30, y + 16, 10526880);
            }

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

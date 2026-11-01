package com.astral.asttweaks.feature.autoeat.gui;

import com.astral.asttweaks.ASTTweaks;
import com.astral.asttweaks.feature.FeatureManager;
import com.astral.asttweaks.feature.autoeat.AutoEatConfig;
import com.astral.asttweaks.feature.autoeat.AutoEatFeature;
import com.astral.asttweaks.util.ItemDisplay;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Screen for configuring food blacklist.
 */
public class FoodListScreen extends Screen {
    private final Screen parent;
    private FoodListWidget foodListWidget;
    private final AutoEatConfig config;
    private EditBox searchField;
    private String searchText = "";
    private List<Item> allFoodItems;

    public FoodListScreen(Screen parent) {
        super(Component.translatable("config.asttweaks.autoeat.blacklist.title"));
        this.parent = parent;
        AutoEatFeature feature = (AutoEatFeature) FeatureManager.getInstance().getFeature("autoeat");
        this.config = feature != null ? feature.getConfig() : null;
    }

    @Override
    protected void init() {
        super.init();

        // Initialize list widget first (before search field, as setText triggers listener)
        this.foodListWidget = new FoodListWidget(this.minecraft, this.width, this.height - 32 - 54, 54, 36);
        this.addWidget(this.foodListWidget);

        // Cache all food items
        this.allFoodItems = new ArrayList<>(BuiltInRegistries.ITEM.stream()
                .filter(item -> food(item) != null)
                .collect(Collectors.toList()));

        // Search field
        this.searchField = new EditBox(this.font, this.width / 2 - 100, 32, 200, 18,
                Component.translatable("config." + ASTTweaks.MOD_ID + ".search.placeholder"));
        this.searchField.setHint(Component.translatable("config." + ASTTweaks.MOD_ID + ".search.placeholder"));
        this.searchField.setResponder(text -> {
            this.searchText = text;
            rebuildList();
        });
        this.searchField.setValue(this.searchText);
        this.addWidget(this.searchField);

        rebuildList();

        this.addRenderableWidget(Button.builder(
                        Component.translatable("gui.done"),
                        button -> this.minecraft.setScreen(parent))
                .bounds(this.width / 2 - 100, this.height - 27, 200, 20)
                .build());
    }

    private static FoodProperties food(Item item) {
        return ItemDisplay.componentsBound(item) ? item.components().get(DataComponents.FOOD) : null;
    }

    private void rebuildList() {
        this.foodListWidget.clear();
        String lowerSearch = searchText.toLowerCase();

        allFoodItems.stream()
                .filter(item -> searchText.isEmpty() ||
                        ItemDisplay.name(item).getString().toLowerCase().contains(lowerSearch))
                .sorted((a, b) -> {
                    boolean aBlacklisted = config != null && config.isBlacklisted(a);
                    boolean bBlacklisted = config != null && config.isBlacklisted(b);
                    if (aBlacklisted != bBlacklisted) {
                        return aBlacklisted ? -1 : 1;
                    }
                    return ItemDisplay.name(a).getString().compareTo(ItemDisplay.name(b).getString());
                })
                .forEach(item -> foodListWidget.addEntry(new FoodEntry(item)));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        this.foodListWidget.extractRenderState(graphics, mouseX, mouseY, delta);

        if (!ItemDisplay.itemDataLoaded()) {
            graphics.centeredText(this.font, ItemDisplay.NEEDS_WORLD, this.width / 2, this.height / 2, 0xFFFFAA00);
        }

        graphics.centeredText(this.font, this.title, this.width / 2, 10, 0xFFFFFFFF);

        Component helpText = Component.translatable("config.asttweaks.autoeat.blacklist.help");
        graphics.centeredText(this.font, helpText, this.width / 2, 20, 0xFFA0A0A0);

        this.searchField.extractRenderState(graphics, mouseX, mouseY, delta);

        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    class FoodListWidget extends ObjectSelectionList<FoodEntry> {
        public FoodListWidget(Minecraft client, int width, int height, int y, int itemHeight) {
            super(client, width, height, y, itemHeight);
        }

        void clear() {
            this.clearEntries();
        }

        @Override
        public int getRowWidth() {
            return super.getRowWidth() + 50;
        }

        @Override
        public int addEntry(FoodEntry entry) {
            return super.addEntry(entry);
        }
    }

    class FoodEntry extends ObjectSelectionList.Entry<FoodEntry> {
        private final Item item;
        private final ItemStack stack;
        private final Button toggleButton;

        public FoodEntry(Item item) {
            this.item = item;
            this.stack = ItemDisplay.stackOrEmpty(item);

            this.toggleButton = Button.builder(
                            Component.literal(""),
                            button -> {
                                if (config != null) {
                                    config.toggleBlacklist(item);
                                    updateButtonText();
                                }
                            })
                    .bounds(0, 0, 60, 20)
                    .build();

            updateButtonText();
        }

        private void updateButtonText() {
            boolean blacklisted = config != null && config.isBlacklisted(item);
            this.toggleButton.setMessage(
                    Component.translatable(blacklisted ? "config.asttweaks.autoeat.blacklisted" : "config.asttweaks.autoeat.allowed")
                            .withStyle(blacklisted ? ChatFormatting.RED : ChatFormatting.GREEN)
            );
        }

        @Override
        public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            Minecraft client = FoodListScreen.this.minecraft;
            int x = this.getContentX();
            int y = this.getContentY();

            graphics.item(stack, x + 5, y + 8);
            graphics.itemDecorations(client.font, stack, x + 5, y + 8);

            Component itemName = ItemDisplay.name(item);
            graphics.text(client.font, itemName, x + 30, y + 5, 0xFFFFFFFF, false);

            FoodProperties food = food(item);
            if (food != null) {
                Component foodInfo = Component.literal(String.format("Hunger: %d  Saturation: %.1f",
                                food.nutrition(),
                                food.saturation()))
                        .withStyle(ChatFormatting.GRAY);
                graphics.text(client.font, foodInfo, x + 30, y + 16, 0xFFA0A0A0, false);
            }

            this.toggleButton.setPosition(x + this.getContentWidth() - 65, y + 7);
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

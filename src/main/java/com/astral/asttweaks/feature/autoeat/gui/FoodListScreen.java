package com.astral.asttweaks.feature.autoeat.gui;

import com.astral.asttweaks.ASTTweaks;
import com.astral.asttweaks.feature.FeatureManager;
import com.astral.asttweaks.feature.autoeat.AutoEatConfig;
import com.astral.asttweaks.feature.autoeat.AutoEatFeature;
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
 * Screen for configuring food blacklist.
 */
public class FoodListScreen extends Screen {
    private final Screen parent;
    private FoodListWidget foodListWidget;
    private final AutoEatConfig config;
    private TextFieldWidget searchField;
    private String searchText = "";
    private List<Item> allFoodItems;

    public FoodListScreen(Screen parent) {
        super(Text.translatable("config.asttweaks.autoeat.blacklist.title"));
        this.parent = parent;
        AutoEatFeature feature = (AutoEatFeature) FeatureManager.getInstance().getFeature("autoeat");
        this.config = feature != null ? feature.getConfig() : null;
    }

    @Override
    protected void init() {
        super.init();

        // Initialize list widget first (before search field, as setText triggers listener)
        this.foodListWidget = new FoodListWidget(this.client, this.width, this.height, 54, this.height - 32, 36);
        this.addSelectableChild(this.foodListWidget);

        // Cache all food items
        this.allFoodItems = new ArrayList<>(Registries.ITEM.stream()
                .filter(item -> item.getFoodComponent() != null)
                .collect(Collectors.toList()));

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

        this.addDrawableChild(ButtonWidget.builder(
                        Text.translatable("gui.done"),
                        button -> this.client.setScreen(parent))
                .dimensions(this.width / 2 - 100, this.height - 27, 200, 20)
                .build());
    }

    private void rebuildList() {
        this.foodListWidget.children().clear();
        String lowerSearch = searchText.toLowerCase();

        allFoodItems.stream()
                .filter(item -> searchText.isEmpty() ||
                        item.getName().getString().toLowerCase().contains(lowerSearch))
                .sorted((a, b) -> {
                    boolean aBlacklisted = config != null && config.isBlacklisted(a);
                    boolean bBlacklisted = config != null && config.isBlacklisted(b);
                    if (aBlacklisted != bBlacklisted) {
                        return aBlacklisted ? -1 : 1;
                    }
                    return a.getName().getString().compareTo(b.getName().getString());
                })
                .forEach(item -> foodListWidget.addEntry(new FoodEntry(item)));
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        this.renderBackground(matrices);
        this.foodListWidget.render(matrices, mouseX, mouseY, delta);

        int titleWidth = this.textRenderer.getWidth(this.title);
        this.textRenderer.drawWithShadow(matrices, this.title, this.width / 2 - titleWidth / 2, 10, 16777215);

        Text helpText = Text.translatable("config.asttweaks.autoeat.blacklist.help");
        int helpWidth = this.textRenderer.getWidth(helpText);
        this.textRenderer.drawWithShadow(matrices, helpText, this.width / 2 - helpWidth / 2, 20, 10526880);

        this.searchField.render(matrices, mouseX, mouseY, delta);

        super.render(matrices, mouseX, mouseY, delta);
    }

    class FoodListWidget extends AlwaysSelectedEntryListWidget<FoodEntry> {
        public FoodListWidget(MinecraftClient client, int width, int height, int top, int bottom, int itemHeight) {
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
        public int addEntry(FoodEntry entry) {
            return super.addEntry(entry);
        }
    }

    class FoodEntry extends AlwaysSelectedEntryListWidget.Entry<FoodEntry> {
        private final Item item;
        private final ItemStack stack;
        private final ButtonWidget toggleButton;

        public FoodEntry(Item item) {
            this.item = item;
            this.stack = new ItemStack(item);

            this.toggleButton = ButtonWidget.builder(
                            Text.literal(""),
                            button -> {
                                if (config != null) {
                                    config.toggleBlacklist(item);
                                    updateButtonText();
                                }
                            })
                    .dimensions(0, 0, 60, 20)
                    .build();

            updateButtonText();
        }

        private void updateButtonText() {
            boolean blacklisted = config != null && config.isBlacklisted(item);
            this.toggleButton.setMessage(
                    Text.translatable(blacklisted ? "config.asttweaks.autoeat.blacklisted" : "config.asttweaks.autoeat.allowed")
                            .formatted(blacklisted ? Formatting.RED : Formatting.GREEN)
            );
        }

        @Override
        public void render(MatrixStack matrices, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            MinecraftClient client = FoodListScreen.this.client;

            client.getItemRenderer().renderInGuiWithOverrides(matrices, stack, x + 5, y + 8);
            client.getItemRenderer().renderGuiItemOverlay(matrices, client.textRenderer, stack, x + 5, y + 8);

            Text itemName = item.getName();
            client.textRenderer.draw(matrices, itemName, x + 30, y + 5, 16777215);

            if (item.getFoodComponent() != null) {
                Text foodInfo = Text.literal(String.format("Hunger: %d  Saturation: %.1f",
                                item.getFoodComponent().getHunger(),
                                item.getFoodComponent().getSaturationModifier()))
                        .formatted(Formatting.GRAY);
                client.textRenderer.draw(matrices, foodInfo, x + 30, y + 16, 10526880);
            }

            this.toggleButton.setPosition(x + entryWidth - 65, y + 7);
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

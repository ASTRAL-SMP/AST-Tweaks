package com.astral.asttweaks.feature.bonemealfilter.gui;

import com.astral.asttweaks.ASTTweaks;
import com.astral.asttweaks.feature.FeatureManager;
import com.astral.asttweaks.feature.bonemealfilter.BoneMealFilterConfig;
import com.astral.asttweaks.feature.bonemealfilter.BoneMealFilterFeature;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;

/**
 * Screen for configuring bone meal whitelist.
 */
public class BlockListScreen extends CompatScreen {
    private final Screen parent;
    private BlockListWidget blockListWidget;
    private final BoneMealFilterConfig config;
    private EditBox searchField;
    private String searchText = "";
    private List<Block> allFertilizableBlocks;

    public BlockListScreen(Screen parent) {
        super(Component.translatable("config.asttweaks.bonemealfilter.whitelist.title"));
        this.parent = parent;
        BoneMealFilterFeature feature = FeatureManager.getInstance().getBoneMealFilterFeature();
        this.config = feature != null ? feature.getConfig() : null;
    }

    @Override
    protected void init() {
        super.init();

        // Initialize list widget first (before search field, as setText triggers listener)
        this.blockListWidget = new BlockListWidget(this.minecraft, this.width, this.height, 54, this.height - 32, 36);
        this.addWidget(this.blockListWidget);

        // Cache all fertilizable blocks
        this.allFertilizableBlocks = new ArrayList<>(BuiltInRegistries.BLOCK.stream()
                .filter(block -> block instanceof BonemealableBlock)
                .filter(block -> !block.asItem().equals(net.minecraft.world.item.Items.AIR))
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
                        button -> McClient.setScreen(parent))
                .bounds(this.width / 2 - 100, this.height - 27, 200, 20)
                .build());
    }

    private void rebuildList() {
        this.blockListWidget.clearAll();
        String lowerSearch = searchText.toLowerCase();

        allFertilizableBlocks.stream()
                .filter(block -> searchText.isEmpty() ||
                        block.getName().getString().toLowerCase().contains(lowerSearch))
                .sorted((a, b) -> {
                    boolean aWhitelisted = config != null && config.isWhitelisted(a);
                    boolean bWhitelisted = config != null && config.isWhitelisted(b);
                    if (aWhitelisted != bWhitelisted) {
                        return aWhitelisted ? -1 : 1;
                    }
                    return a.getName().getString().compareTo(b.getName().getString());
                })
                .forEach(block -> blockListWidget.addEntry(new BlockEntry(block)));
    }

    @Override
    protected void draw(Gfx gfx, int mouseX, int mouseY, float delta) {
        gfx.widget(this.blockListWidget, mouseX, mouseY, delta);

        gfx.centeredText(this.font, this.title, this.width / 2, 10, 0xFFFFFFFF);

        Component helpText = Component.translatable("config.asttweaks.bonemealfilter.whitelist.help");
        gfx.centeredText(this.font, helpText, this.width / 2, 20, 0xFFA0A0A0);

        gfx.widget(this.searchField, mouseX, mouseY, delta);

        renderWidgets(gfx, mouseX, mouseY, delta);
    }

    class BlockListWidget extends CompatSelectionList<BlockEntry> {
        public BlockListWidget(Minecraft client, int width, int height, int top, int bottom, int itemHeight) {
            super(client, width, height, top, bottom, itemHeight);
        }

        @Override
        public int getRowWidth() {
            return super.getRowWidth() + 50;
        }

        @Override
        protected int scrollbarOffset() {
            return 20;
        }
    }

    class BlockEntry extends CompatListEntry<BlockEntry> {
        private final Block block;
        private final ItemStack stack;
        private final Button toggleButton;

        public BlockEntry(Block block) {
            this.block = block;
            this.stack = ItemDisplay.stackOrEmpty(block.asItem());

            this.toggleButton = Button.builder(
                            Component.literal(""),
                            button -> {
                                if (config != null) {
                                    config.toggleWhitelist(block);
                                    updateButtonText();
                                }
                            })
                    .bounds(0, 0, 80, 20)
                    .build();

            updateButtonText();
        }

        private void updateButtonText() {
            boolean whitelisted = config != null && config.isWhitelisted(block);
            this.toggleButton.setMessage(
                    Component.translatable(whitelisted ? "config.asttweaks.bonemealfilter.allowed" : "config.asttweaks.bonemealfilter.notallowed")
                            .withStyle(whitelisted ? ChatFormatting.GREEN : ChatFormatting.GRAY)
            );
        }

        @Override
        protected void drawEntry(Gfx gfx, int x, int y, int entryWidth, int entryHeight,
                                 int mouseX, int mouseY, boolean hovered, float tickDelta) {
            Minecraft client = BlockListScreen.this.minecraft;

            gfx.item(stack, x + 5, y + 8);
            gfx.itemDecorations(client.font, stack, x + 5, y + 8);

            Component blockName = block.getName();
            gfx.text(client.font, blockName, x + 30, y + 5, 0xFFFFFFFF, false);

            Component blockId = Component.literal(BuiltInRegistries.BLOCK.getKey(block).toString())
                    .withStyle(ChatFormatting.GRAY);
            gfx.text(client.font, blockId, x + 30, y + 16, 0xFFA0A0A0, false);

            this.toggleButton.setPosition(x + entryWidth - 85, y + 7);
            gfx.widget(this.toggleButton, mouseX, mouseY, tickDelta);
        }

        @Override
        protected boolean onMouseClicked(double mouseX, double mouseY, int button) {
            return GuiEvents.click(this.toggleButton, mouseX, mouseY, button);
        }

        @Override
        public Component getNarration() {
            return Component.literal(block.getName().getString());
        }
    }
}

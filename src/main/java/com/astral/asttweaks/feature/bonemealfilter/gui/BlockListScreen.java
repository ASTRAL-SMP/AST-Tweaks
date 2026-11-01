package com.astral.asttweaks.feature.bonemealfilter.gui;

import com.astral.asttweaks.ASTTweaks;
import com.astral.asttweaks.feature.FeatureManager;
import com.astral.asttweaks.feature.bonemealfilter.BoneMealFilterConfig;
import com.astral.asttweaks.feature.bonemealfilter.BoneMealFilterFeature;
import com.astral.asttweaks.util.ItemDisplay;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;

/**
 * Screen for configuring bone meal whitelist.
 */
public class BlockListScreen extends Screen {
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
        this.blockListWidget = new BlockListWidget(this.minecraft, this.width, this.height - 32 - 54, 54, 36);
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
                        button -> this.minecraft.setScreen(parent))
                .bounds(this.width / 2 - 100, this.height - 27, 200, 20)
                .build());
    }

    private void rebuildList() {
        this.blockListWidget.clear();
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
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        this.blockListWidget.extractRenderState(graphics, mouseX, mouseY, delta);

        int titleWidth = this.font.width(this.title);
        graphics.text(this.font, this.title, this.width / 2 - titleWidth / 2, 10, 0xFFFFFFFF);

        Component helpText = Component.translatable("config.asttweaks.bonemealfilter.whitelist.help");
        int helpWidth = this.font.width(helpText);
        graphics.text(this.font, helpText, this.width / 2 - helpWidth / 2, 20, 0xFFA0A0A0);

        this.searchField.extractRenderState(graphics, mouseX, mouseY, delta);

        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    class BlockListWidget extends ObjectSelectionList<BlockEntry> {
        public BlockListWidget(Minecraft client, int width, int height, int top, int itemHeight) {
            super(client, width, height, top, itemHeight);
        }

        void clear() {
            this.clearEntries();
        }

        @Override
        public int getRowWidth() {
            return super.getRowWidth() + 50;
        }

        @Override
        public int addEntry(BlockEntry entry) {
            return super.addEntry(entry);
        }
    }

    class BlockEntry extends ObjectSelectionList.Entry<BlockEntry> {
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
        public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            int x = this.getContentX();
            int y = this.getContentY();
            int entryWidth = this.getContentWidth();
            Minecraft client = BlockListScreen.this.minecraft;

            graphics.item(stack, x + 5, y + 8);
            graphics.itemDecorations(client.font, stack, x + 5, y + 8);

            Component blockName = block.getName();
            graphics.text(client.font, blockName, x + 30, y + 5, 0xFFFFFFFF, false);

            Component blockId = Component.literal(BuiltInRegistries.BLOCK.getKey(block).toString())
                    .withStyle(ChatFormatting.GRAY);
            graphics.text(client.font, blockId, x + 30, y + 16, 0xFFA0A0A0, false);

            this.toggleButton.setPosition(x + entryWidth - 85, y + 7);
            this.toggleButton.extractRenderState(graphics, mouseX, mouseY, tickDelta);
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
            return this.toggleButton.mouseClicked(event, doubleClick);
        }

        @Override
        public Component getNarration() {
            return Component.literal(block.getName().getString());
        }
    }
}

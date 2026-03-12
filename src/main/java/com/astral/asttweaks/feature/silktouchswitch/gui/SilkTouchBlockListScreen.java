package com.astral.asttweaks.feature.silktouchswitch.gui;

import com.astral.asttweaks.ASTTweaks;
import com.astral.asttweaks.feature.FeatureManager;
import com.astral.asttweaks.feature.silktouchswitch.SilkTouchSwitchConfig;
import com.astral.asttweaks.feature.silktouchswitch.SilkTouchSwitchFeature;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * シルクタッチスイッチ対象ブロックの設定画面。
 */
public class SilkTouchBlockListScreen extends Screen {
    // シルクタッチで挙動が変わるブロックのキュレーションリスト（Minecraft 1.19.4）
    private static final Set<String> SILK_TOUCH_RELEVANT_BLOCKS = Set.of(
            // ガラス系 (34)
            "minecraft:glass", "minecraft:glass_pane",
            "minecraft:white_stained_glass", "minecraft:orange_stained_glass",
            "minecraft:magenta_stained_glass", "minecraft:light_blue_stained_glass",
            "minecraft:yellow_stained_glass", "minecraft:lime_stained_glass",
            "minecraft:pink_stained_glass", "minecraft:gray_stained_glass",
            "minecraft:light_gray_stained_glass", "minecraft:cyan_stained_glass",
            "minecraft:purple_stained_glass", "minecraft:blue_stained_glass",
            "minecraft:brown_stained_glass", "minecraft:green_stained_glass",
            "minecraft:red_stained_glass", "minecraft:black_stained_glass",
            "minecraft:white_stained_glass_pane", "minecraft:orange_stained_glass_pane",
            "minecraft:magenta_stained_glass_pane", "minecraft:light_blue_stained_glass_pane",
            "minecraft:yellow_stained_glass_pane", "minecraft:lime_stained_glass_pane",
            "minecraft:pink_stained_glass_pane", "minecraft:gray_stained_glass_pane",
            "minecraft:light_gray_stained_glass_pane", "minecraft:cyan_stained_glass_pane",
            "minecraft:purple_stained_glass_pane", "minecraft:blue_stained_glass_pane",
            "minecraft:brown_stained_glass_pane", "minecraft:green_stained_glass_pane",
            "minecraft:red_stained_glass_pane", "minecraft:black_stained_glass_pane",
            // 鉱石系 (18)
            "minecraft:coal_ore", "minecraft:deepslate_coal_ore",
            "minecraft:diamond_ore", "minecraft:deepslate_diamond_ore",
            "minecraft:emerald_ore", "minecraft:deepslate_emerald_ore",
            "minecraft:lapis_ore", "minecraft:deepslate_lapis_ore",
            "minecraft:redstone_ore", "minecraft:deepslate_redstone_ore",
            "minecraft:nether_quartz_ore", "minecraft:nether_gold_ore",
            "minecraft:copper_ore", "minecraft:deepslate_copper_ore",
            "minecraft:iron_ore", "minecraft:deepslate_iron_ore",
            "minecraft:gold_ore", "minecraft:deepslate_gold_ore",
            // 自然ブロック
            "minecraft:grass_block", "minecraft:mycelium", "minecraft:podzol",
            "minecraft:ice", "minecraft:packed_ice", "minecraft:blue_ice",
            "minecraft:snow_block", "minecraft:clay",
            "minecraft:glowstone", "minecraft:sea_lantern",
            "minecraft:melon",
            "minecraft:campfire", "minecraft:soul_campfire",
            "minecraft:turtle_egg",
            "minecraft:amethyst_cluster", "minecraft:large_amethyst_bud",
            "minecraft:medium_amethyst_bud", "minecraft:small_amethyst_bud",
            // 構造物系
            "minecraft:bookshelf", "minecraft:ender_chest",
            "minecraft:bee_nest", "minecraft:beehive",
            // サンゴ系 (5)
            "minecraft:brain_coral_block", "minecraft:bubble_coral_block",
            "minecraft:fire_coral_block", "minecraft:horn_coral_block",
            "minecraft:tube_coral_block",
            // 感染ブロック (7)
            "minecraft:infested_stone", "minecraft:infested_cobblestone",
            "minecraft:infested_stone_bricks", "minecraft:infested_mossy_stone_bricks",
            "minecraft:infested_cracked_stone_bricks", "minecraft:infested_chiseled_stone_bricks",
            "minecraft:infested_deepslate"
    );

    private final Screen parent;
    private BlockListWidget blockListWidget;
    private final SilkTouchSwitchConfig config;
    private TextFieldWidget searchField;
    private String searchText = "";
    private List<Block> allBlocks;

    public SilkTouchBlockListScreen(Screen parent) {
        super(Text.translatable("config.asttweaks.silktouchswitch.blocklist.title"));
        this.parent = parent;
        SilkTouchSwitchFeature feature = FeatureManager.getInstance().getSilkTouchSwitchFeature();
        this.config = feature != null ? feature.getConfig() : null;
    }

    @Override
    protected void init() {
        super.init();

        // リストウィジェットを先に初期化
        this.blockListWidget = new BlockListWidget(this.client, this.width, this.height, 54, this.height - 32, 36);
        this.addSelectableChild(this.blockListWidget);

        // シルクタッチ関連ブロックのみをキャッシュ
        this.allBlocks = new ArrayList<>(Registries.BLOCK.stream()
                .filter(block -> SILK_TOUCH_RELEVANT_BLOCKS.contains(
                        Registries.BLOCK.getId(block).toString()))
                .collect(Collectors.toList()));

        // 検索フィールド
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
        this.blockListWidget.children().clear();
        String lowerSearch = searchText.toLowerCase();

        allBlocks.stream()
                .filter(block -> searchText.isEmpty() ||
                        block.getName().getString().toLowerCase().contains(lowerSearch))
                .sorted((a, b) -> {
                    // 対象ブロックを上位にソート
                    boolean aInList = config != null && config.isInSilkTouchList(a);
                    boolean bInList = config != null && config.isInSilkTouchList(b);
                    if (aInList != bInList) {
                        return aInList ? -1 : 1;
                    }
                    return a.getName().getString().compareTo(b.getName().getString());
                })
                .forEach(block -> blockListWidget.addEntry(new BlockEntry(block)));
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        this.renderBackground(matrices);
        this.blockListWidget.render(matrices, mouseX, mouseY, delta);

        int titleWidth = this.textRenderer.getWidth(this.title);
        this.textRenderer.drawWithShadow(matrices, this.title, this.width / 2 - titleWidth / 2, 10, 16777215);

        Text helpText = Text.translatable("config.asttweaks.silktouchswitch.blocklist.help");
        int helpWidth = this.textRenderer.getWidth(helpText);
        this.textRenderer.drawWithShadow(matrices, helpText, this.width / 2 - helpWidth / 2, 20, 10526880);

        this.searchField.render(matrices, mouseX, mouseY, delta);

        super.render(matrices, mouseX, mouseY, delta);
    }

    class BlockListWidget extends AlwaysSelectedEntryListWidget<BlockEntry> {
        public BlockListWidget(MinecraftClient client, int width, int height, int top, int bottom, int itemHeight) {
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
        public int addEntry(BlockEntry entry) {
            return super.addEntry(entry);
        }
    }

    class BlockEntry extends AlwaysSelectedEntryListWidget.Entry<BlockEntry> {
        private final Block block;
        private final ItemStack stack;
        private final ButtonWidget toggleButton;

        public BlockEntry(Block block) {
            this.block = block;
            this.stack = new ItemStack(block.asItem());

            this.toggleButton = ButtonWidget.builder(
                            Text.literal(""),
                            button -> {
                                if (config != null) {
                                    config.toggleList(block);
                                    updateButtonText();
                                }
                            })
                    .dimensions(0, 0, 80, 20)
                    .build();

            updateButtonText();
        }

        private void updateButtonText() {
            boolean inList = config != null && config.isInSilkTouchList(block);
            this.toggleButton.setMessage(
                    Text.translatable(inList ? "config.asttweaks.silktouchswitch.target" : "config.asttweaks.silktouchswitch.nottarget")
                            .formatted(inList ? Formatting.GREEN : Formatting.GRAY)
            );
        }

        @Override
        public void render(MatrixStack matrices, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            MinecraftClient client = SilkTouchBlockListScreen.this.client;

            client.getItemRenderer().renderInGuiWithOverrides(matrices, stack, x + 5, y + 8);
            client.getItemRenderer().renderGuiItemOverlay(matrices, client.textRenderer, stack, x + 5, y + 8);

            Text blockName = block.getName();
            client.textRenderer.draw(matrices, blockName, x + 30, y + 5, 16777215);

            Text blockId = Text.literal(Registries.BLOCK.getId(block).toString())
                    .formatted(Formatting.GRAY);
            client.textRenderer.draw(matrices, blockId, x + 30, y + 16, 10526880);

            this.toggleButton.setPosition(x + entryWidth - 85, y + 7);
            this.toggleButton.render(matrices, mouseX, mouseY, tickDelta);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            return this.toggleButton.mouseClicked(mouseX, mouseY, button);
        }

        @Override
        public Text getNarration() {
            return Text.literal(block.getName().getString());
        }
    }
}

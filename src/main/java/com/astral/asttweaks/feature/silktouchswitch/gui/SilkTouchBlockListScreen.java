package com.astral.asttweaks.feature.silktouchswitch.gui;

import com.astral.asttweaks.ASTTweaks;
import com.astral.asttweaks.feature.FeatureManager;
import com.astral.asttweaks.feature.silktouchswitch.SilkTouchSwitchConfig;
import com.astral.asttweaks.feature.silktouchswitch.SilkTouchSwitchFeature;
import com.astral.asttweaks.util.ItemDisplay;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
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
            // サンゴ系 (35) — live は block/plant/fan/wall_fan、dead は plant/fan/wall_fan（dead block はツルハシで普通にドロップ）
            "minecraft:brain_coral_block", "minecraft:bubble_coral_block",
            "minecraft:fire_coral_block", "minecraft:horn_coral_block",
            "minecraft:tube_coral_block",
            "minecraft:brain_coral", "minecraft:bubble_coral",
            "minecraft:fire_coral", "minecraft:horn_coral",
            "minecraft:tube_coral",
            "minecraft:brain_coral_fan", "minecraft:bubble_coral_fan",
            "minecraft:fire_coral_fan", "minecraft:horn_coral_fan",
            "minecraft:tube_coral_fan",
            "minecraft:brain_coral_wall_fan", "minecraft:bubble_coral_wall_fan",
            "minecraft:fire_coral_wall_fan", "minecraft:horn_coral_wall_fan",
            "minecraft:tube_coral_wall_fan",
            "minecraft:dead_brain_coral", "minecraft:dead_bubble_coral",
            "minecraft:dead_fire_coral", "minecraft:dead_horn_coral",
            "minecraft:dead_tube_coral",
            "minecraft:dead_brain_coral_fan", "minecraft:dead_bubble_coral_fan",
            "minecraft:dead_fire_coral_fan", "minecraft:dead_horn_coral_fan",
            "minecraft:dead_tube_coral_fan",
            "minecraft:dead_brain_coral_wall_fan", "minecraft:dead_bubble_coral_wall_fan",
            "minecraft:dead_fire_coral_wall_fan", "minecraft:dead_horn_coral_wall_fan",
            "minecraft:dead_tube_coral_wall_fan",
            // 感染ブロック (7)
            "minecraft:infested_stone", "minecraft:infested_cobblestone",
            "minecraft:infested_stone_bricks", "minecraft:infested_mossy_stone_bricks",
            "minecraft:infested_cracked_stone_bricks", "minecraft:infested_chiseled_stone_bricks",
            "minecraft:infested_deepslate"
    );

    private final Screen parent;
    private BlockListWidget blockListWidget;
    private final SilkTouchSwitchConfig config;
    private EditBox searchField;
    private String searchText = "";
    private List<Block> allBlocks;

    public SilkTouchBlockListScreen(Screen parent) {
        super(Component.translatable("config.asttweaks.silktouchswitch.blocklist.title"));
        this.parent = parent;
        SilkTouchSwitchFeature feature = FeatureManager.getInstance().getSilkTouchSwitchFeature();
        this.config = feature != null ? feature.getConfig() : null;
    }

    @Override
    protected void init() {
        super.init();

        // リストウィジェットを先に初期化
        this.blockListWidget = new BlockListWidget(this.minecraft, this.width, this.height - 32 - 54, 54, 36);
        this.addWidget(this.blockListWidget);

        // シルクタッチ関連ブロックのみをキャッシュ
        this.allBlocks = new ArrayList<>(BuiltInRegistries.BLOCK.stream()
                .filter(block -> SILK_TOUCH_RELEVANT_BLOCKS.contains(
                        BuiltInRegistries.BLOCK.getKey(block).toString()))
                .collect(Collectors.toList()));

        // 検索フィールド
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
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        this.blockListWidget.extractRenderState(graphics, mouseX, mouseY, delta);

        int titleWidth = this.font.width(this.title);
        graphics.text(this.font, this.title, this.width / 2 - titleWidth / 2, 10, 0xFFFFFFFF);

        Component helpText = Component.translatable("config.asttweaks.silktouchswitch.blocklist.help");
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
                                    config.toggleList(block);
                                    updateButtonText();
                                }
                            })
                    .bounds(0, 0, 80, 20)
                    .build();

            updateButtonText();
        }

        private void updateButtonText() {
            boolean inList = config != null && config.isInSilkTouchList(block);
            this.toggleButton.setMessage(
                    Component.translatable(inList ? "config.asttweaks.silktouchswitch.target" : "config.asttweaks.silktouchswitch.nottarget")
                            .withStyle(inList ? ChatFormatting.GREEN : ChatFormatting.GRAY)
            );
        }

        @Override
        public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            int x = this.getContentX();
            int y = this.getContentY();
            int entryWidth = this.getContentWidth();
            Minecraft client = SilkTouchBlockListScreen.this.minecraft;

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

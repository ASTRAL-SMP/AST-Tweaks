package com.astral.asttweaks.feature.entityculling.gui;

import com.astral.asttweaks.ASTTweaks;
import com.astral.asttweaks.feature.FeatureManager;
import com.astral.asttweaks.feature.entityculling.EntityCullingConfig;
import com.astral.asttweaks.feature.entityculling.EntityCullingFeature;
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
import net.minecraft.world.entity.EntityType;

/**
 * Screen for configuring entity visibility blacklist.
 */
public class EntityListScreen extends CompatScreen {
    private final Screen parent;
    private EntityListWidget entityListWidget;
    private final EntityCullingConfig config;
    private EditBox searchField;
    private String searchText = "";
    private List<EntityType<?>> allEntityTypes;

    public EntityListScreen(Screen parent) {
        super(Component.translatable("config.asttweaks.entityculling.blacklist.title"));
        this.parent = parent;
        EntityCullingFeature feature = FeatureManager.getInstance().getEntityCullingFeature();
        this.config = feature != null ? feature.getConfig() : null;
    }

    @Override
    protected void init() {
        super.init();

        // Initialize list widget first (before search field, as setText triggers listener)
        this.entityListWidget = new EntityListWidget(this.minecraft, this.width, this.height, 54, this.height - 32, 24);
        this.addWidget(this.entityListWidget);

        // Cache all entity types
        this.allEntityTypes = new ArrayList<>(BuiltInRegistries.ENTITY_TYPE.stream().collect(Collectors.toList()));

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

        // "Hide All" button
        this.addRenderableWidget(Button.builder(
                        Component.translatable("config.asttweaks.entityculling.hideAll"),
                        button -> {
                            if (config != null) {
                                config.hideAllEntities(allEntityTypes);
                                rebuildList();
                            }
                        })
                .bounds(this.width / 2 - 110, this.height - 27, 70, 20)
                .build());

        // "Show All" button
        this.addRenderableWidget(Button.builder(
                        Component.translatable("config.asttweaks.entityculling.showAll"),
                        button -> {
                            if (config != null) {
                                config.showAllEntities();
                                rebuildList();
                            }
                        })
                .bounds(this.width / 2 - 35, this.height - 27, 70, 20)
                .build());

        // "Done" button
        this.addRenderableWidget(Button.builder(
                        Component.translatable("gui.done"),
                        button -> McClient.setScreen(parent))
                .bounds(this.width / 2 + 40, this.height - 27, 70, 20)
                .build());
    }

    private void rebuildList() {
        this.entityListWidget.clearAll();
        String lowerSearch = searchText.toLowerCase();

        allEntityTypes.stream()
                .filter(e -> searchText.isEmpty() ||
                        e.getDescription().getString().toLowerCase().contains(lowerSearch))
                .sorted((a, b) -> {
                    boolean aBlacklisted = config != null && config.isEntityBlacklisted(a);
                    boolean bBlacklisted = config != null && config.isEntityBlacklisted(b);
                    if (aBlacklisted != bBlacklisted) {
                        return aBlacklisted ? -1 : 1;
                    }
                    return a.getDescription().getString().compareTo(b.getDescription().getString());
                })
                .forEach(e -> entityListWidget.addEntry(new EntityEntry(e)));
    }

    @Override
    protected void draw(Gfx gfx, int mouseX, int mouseY, float delta) {
        gfx.widget(this.entityListWidget, mouseX, mouseY, delta);

        int titleWidth = this.font.width(this.title);
        gfx.text(this.font, this.title, this.width / 2 - titleWidth / 2, 10, 0xFFFFFFFF);

        Component helpText = Component.translatable("config.asttweaks.entityculling.blacklist.help");
        int helpWidth = this.font.width(helpText);
        gfx.text(this.font, helpText, this.width / 2 - helpWidth / 2, 20, 0xFFA0A0A0);

        gfx.widget(this.searchField, mouseX, mouseY, delta);

        renderWidgets(gfx, mouseX, mouseY, delta);
    }

    class EntityListWidget extends CompatSelectionList<EntityEntry> {
        public EntityListWidget(Minecraft client, int width, int height, int top, int bottom, int itemHeight) {
            super(client, width, height, top, bottom, itemHeight);
        }


        @Override
        public int getRowWidth() {
            return super.getRowWidth() + 50;
        }

    }

    class EntityEntry extends CompatListEntry<EntityEntry> {
        private final EntityType<?> entityType;
        private final Button toggleButton;

        public EntityEntry(EntityType<?> entityType) {
            this.entityType = entityType;

            this.toggleButton = Button.builder(
                            Component.literal(""),
                            button -> {
                                if (config != null) {
                                    config.toggleEntityBlacklist(entityType);
                                    updateButtonText();
                                }
                            })
                    .bounds(0, 0, 60, 20)
                    .build();

            updateButtonText();
        }

        private void updateButtonText() {
            boolean blacklisted = config != null && config.isEntityBlacklisted(entityType);
            this.toggleButton.setMessage(
                    Component.translatable(blacklisted ?
                            "config.asttweaks.entityculling.hidden" :
                            "config.asttweaks.entityculling.visible")
                            .withStyle(blacklisted ? ChatFormatting.RED : ChatFormatting.GREEN)
            );
        }

        @Override
        protected void drawEntry(Gfx gfx, int x, int y, int entryWidth, int entryHeight,
                                 int mouseX, int mouseY, boolean hovered, float tickDelta) {
            Minecraft client = EntityListScreen.this.minecraft;

            // Entity name
            Component entityName = entityType.getDescription();
            gfx.text(client.font, entityName, x + 5, y + 6, 0xFFFFFFFF, false);

            // Toggle button
            this.toggleButton.setPosition(x + entryWidth - 65, y + 2);
            gfx.widget(this.toggleButton, mouseX, mouseY, tickDelta);
        }

        @Override
        protected boolean onMouseClicked(double mouseX, double mouseY, int button) {
            return GuiEvents.click(this.toggleButton, mouseX, mouseY, button);
        }

        @Override
        public Component getNarration() {
            return entityType.getDescription();
        }
    }
}

package com.astral.asttweaks.feature.entityculling.gui;

import com.astral.asttweaks.feature.FeatureManager;
import com.astral.asttweaks.feature.entityculling.EntityCullingConfig;
import com.astral.asttweaks.feature.entityculling.EntityCullingFeature;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EntityType;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Screen for configuring entity visibility blacklist.
 */
public class EntityListScreen extends Screen {
    private final Screen parent;
    private EntityListWidget entityListWidget;
    private final EntityCullingConfig config;

    public EntityListScreen(Screen parent) {
        super(Text.translatable("config.asttweaks.entityculling.blacklist.title"));
        this.parent = parent;
        EntityCullingFeature feature = FeatureManager.getInstance().getEntityCullingFeature();
        this.config = feature != null ? feature.getConfig() : null;
    }

    @Override
    protected void init() {
        super.init();

        this.entityListWidget = new EntityListWidget(this.client, this.width, this.height, 32, this.height - 32, 24);
        this.addSelectableChild(this.entityListWidget);

        // Get all entity types, sorted by blacklist status then name
        List<EntityType<?>> entityTypes = Registries.ENTITY_TYPE.stream()
                .sorted((a, b) -> {
                    boolean aBlacklisted = config != null && config.isEntityBlacklisted(a);
                    boolean bBlacklisted = config != null && config.isEntityBlacklisted(b);
                    if (aBlacklisted != bBlacklisted) {
                        return aBlacklisted ? -1 : 1;
                    }
                    return a.getName().getString().compareTo(b.getName().getString());
                })
                .collect(Collectors.toList());

        for (EntityType<?> entityType : entityTypes) {
            this.entityListWidget.addEntry(new EntityEntry(entityType));
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
        this.entityListWidget.render(matrices, mouseX, mouseY, delta);

        int titleWidth = this.textRenderer.getWidth(this.title);
        this.textRenderer.drawWithShadow(matrices, this.title, this.width / 2 - titleWidth / 2, 10, 16777215);

        Text helpText = Text.translatable("config.asttweaks.entityculling.blacklist.help");
        int helpWidth = this.textRenderer.getWidth(helpText);
        this.textRenderer.drawWithShadow(matrices, helpText, this.width / 2 - helpWidth / 2, 20, 10526880);

        super.render(matrices, mouseX, mouseY, delta);
    }

    class EntityListWidget extends AlwaysSelectedEntryListWidget<EntityEntry> {
        public EntityListWidget(MinecraftClient client, int width, int height, int top, int bottom, int itemHeight) {
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
        public int addEntry(EntityEntry entry) {
            return super.addEntry(entry);
        }
    }

    class EntityEntry extends AlwaysSelectedEntryListWidget.Entry<EntityEntry> {
        private final EntityType<?> entityType;
        private final ButtonWidget toggleButton;

        public EntityEntry(EntityType<?> entityType) {
            this.entityType = entityType;

            this.toggleButton = ButtonWidget.builder(
                            Text.literal(""),
                            button -> {
                                if (config != null) {
                                    config.toggleEntityBlacklist(entityType);
                                    updateButtonText();
                                }
                            })
                    .dimensions(0, 0, 60, 20)
                    .build();

            updateButtonText();
        }

        private void updateButtonText() {
            boolean blacklisted = config != null && config.isEntityBlacklisted(entityType);
            this.toggleButton.setMessage(
                    Text.translatable(blacklisted ?
                            "config.asttweaks.entityculling.hidden" :
                            "config.asttweaks.entityculling.visible")
                            .formatted(blacklisted ? Formatting.RED : Formatting.GREEN)
            );
        }

        @Override
        public void render(MatrixStack matrices, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            MinecraftClient client = EntityListScreen.this.client;

            // Entity name
            Text entityName = entityType.getName();
            client.textRenderer.draw(matrices, entityName, x + 5, y + 6, 16777215);

            // Toggle button
            this.toggleButton.setPosition(x + entryWidth - 65, y + 2);
            this.toggleButton.render(matrices, mouseX, mouseY, tickDelta);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            return this.toggleButton.mouseClicked(mouseX, mouseY, button);
        }

        @Override
        public Text getNarration() {
            return entityType.getName();
        }
    }
}

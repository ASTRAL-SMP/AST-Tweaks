//? if <26.1 {
/*package com.astral.asttweaks.feature.voidtrade.gui;

import com.astral.asttweaks.ASTTweaks;
import com.astral.asttweaks.compat.ItemScrollerCompat;
import com.astral.asttweaks.feature.automove.MoveDirection;
import com.astral.asttweaks.feature.FeatureManager;
import com.astral.asttweaks.feature.voidtrade.VoidTradeFeature;
import com.astral.asttweaks.feature.voidtrade.VoidTradePreset;
import com.astral.asttweaks.feature.voidtrade.VoidTradeShare;
import com.astral.asttweaks.feature.voidtrade.VoidTradeStep;
import com.astral.asttweaks.feature.voidtrade.VoidTradeStepType;
import com.astral.asttweaks.util.McClient;
import com.astral.asttweaks.util.gui.CompatListEntry;
import com.astral.asttweaks.util.gui.CompatScreen;
import com.astral.asttweaks.util.gui.CompatSelectionList;
import com.astral.asttweaks.util.gui.Gfx;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.ObjIntConsumer;
import java.util.stream.Collectors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/^*
 * Void Trade のワークフロー（ステップ列）編集画面。
 * 左にステップ一覧、右に選択中ステップの詳細を表示する。編集内容は設定のリストへ直接反映し、閉じるときに保存する。
 ^/
public class VoidTradeWorkflowScreen extends CompatScreen {
    private static final String KEY = "config." + ASTTweaks.MOD_ID + ".voidtrade.workflow.";
    private static final String TYPE_KEY = "config." + ASTTweaks.MOD_ID + ".voidtrade.steptype.";
    private static final int TOP = 34;
    private static final int ROW_HEIGHT = 24;
    private static final int LIST_ITEM_HEIGHT = 14;
    private static final int COORD_LABEL_WIDTH = 10;

    private final Screen parent;
    private final VoidTradeFeature feature;
    private final List<VoidTradeStep> steps;

    private StepListWidget stepList;
    private int selectedIndex;
    private boolean updatingFields;

    private int listLeft;
    private int listWidth;
    private int editorLeft;
    private int editorWidth;
    private int paramLabelWidth;
    private int descriptionY;
    private final List<Label> labels = new ArrayList<>();

    private CycleButton<VoidTradeStepType> typeButton;
    private EditBox xField;
    private EditBox yField;
    private EditBox zField;
    private Button herePosButton;
    private Button lookPosButton;
    private CycleButton<Boolean> keepFacingButton;
    private CycleButton<Boolean> sprintButton;
    private CycleButton<MoveDirection> nudgeDirectionButton;
    private EditBox nudgeTicksField;
    private EditBox radiusField;
    private EditBox ticksField;
    private CycleButton<Boolean> favoritesButton;
    private EditBox offerField;
    private CycleButton<Boolean> dropOutputButton;
    private EditBox commandField;
    private CycleButton<Boolean> firstLoopOnlyButton;

    private Button duplicateButton;
    private Button removeButton;
    private Button upButton;
    private Button downButton;
    private Button copyButton;
    private Button startStopButton;

    private Component notice;
    private int noticeTicks;

    private record Label(Component text, int x, int y) {}

    public VoidTradeWorkflowScreen(Screen parent) {
        super(Component.translatable(KEY + "title"));
        this.parent = parent;
        this.feature = FeatureManager.getInstance().getVoidTradeFeature();
        this.steps = this.feature.getConfig().getSteps();
        this.selectedIndex = this.steps.isEmpty() ? -1 : 0;
    }

    @Override
    protected void init() {
        super.init();

        this.listLeft = 10;
        this.listWidth = Math.min(240, (this.width - 30) / 2);
        this.editorLeft = this.listLeft + this.listWidth + 10;
        this.editorWidth = this.width - this.editorLeft - 10;
        this.paramLabelWidth = this.editorWidth * 2 / 5;

        this.stepList = new StepListWidget(this.minecraft, this.listWidth, this.height, TOP, this.height - 58, LIST_ITEM_HEIGHT);
        this.stepList.setLeftPos(this.listLeft);
        this.stepList.setRenderBackground(false);
        this.stepList.setRenderTopAndBottom(false);
        this.addWidget(this.stepList);

        // 一覧の操作ボタン
        int opY = this.height - 54;
        int gap = 4;
        int opWidth = (this.listWidth - gap * 4) / 5;
        this.addRenderableWidget(Button.builder(Component.translatable(KEY + "add"), b -> addStep())
                .bounds(this.listLeft, opY, opWidth, 20)
                .tooltip(Tooltip.create(Component.translatable(KEY + "add.tooltip")))
                .build());
        this.duplicateButton = this.addRenderableWidget(Button.builder(Component.translatable(KEY + "duplicate"), b -> duplicateStep())
                .bounds(this.listLeft + (opWidth + gap), opY, opWidth, 20)
                .build());
        this.removeButton = this.addRenderableWidget(Button.builder(Component.translatable(KEY + "remove"), b -> removeStep())
                .bounds(this.listLeft + (opWidth + gap) * 2, opY, opWidth, 20)
                .build());
        this.upButton = this.addRenderableWidget(Button.builder(Component.literal("↑"), b -> moveStep(-1))
                .bounds(this.listLeft + (opWidth + gap) * 3, opY, opWidth, 20)
                .build());
        this.downButton = this.addRenderableWidget(Button.builder(Component.literal("↓"), b -> moveStep(1))
                .bounds(this.listLeft + (opWidth + gap) * 4, opY, opWidth, 20)
                .build());

        // 下段
        int bottomY = this.height - 28;
        int bottomGap = 6;
        int bottomWidth = Math.min(100, (this.width - 20 - bottomGap * 4) / 5);
        int bottomLeft = this.width / 2 - (bottomWidth * 5 + bottomGap * 4) / 2;
        this.addRenderableWidget(Button.builder(Component.translatable(KEY + "presets"), b -> McClient.setScreen(new VoidTradePresetScreen(this)))
                .bounds(bottomLeft, bottomY, bottomWidth, 20)
                .tooltip(Tooltip.create(Component.translatable(KEY + "presets.tooltip")))
                .build());
        this.copyButton = this.addRenderableWidget(Button.builder(Component.translatable(KEY + "copy"), b -> copyWorkflow())
                .bounds(bottomLeft + (bottomWidth + bottomGap), bottomY, bottomWidth, 20)
                .tooltip(Tooltip.create(Component.translatable(KEY + "copy.tooltip")))
                .build());
        this.addRenderableWidget(Button.builder(Component.translatable(KEY + "paste"), b -> pasteWorkflow())
                .bounds(bottomLeft + (bottomWidth + bottomGap) * 2, bottomY, bottomWidth, 20)
                .tooltip(Tooltip.create(Component.translatable(KEY + "paste.tooltip")))
                .build());
        this.startStopButton = this.addRenderableWidget(Button.builder(Component.translatable(KEY + "start"), b -> toggleRunning())
                .bounds(bottomLeft + (bottomWidth + bottomGap) * 3, bottomY, bottomWidth, 20)
                .build());
        this.addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(bottomLeft + (bottomWidth + bottomGap) * 4, bottomY, bottomWidth, 20)
                .build());

        // 詳細エディタ（種別に応じて refreshEditor で表示・配置を切り替える）
        this.typeButton = this.addRenderableWidget(CycleButton
                .<VoidTradeStepType>builder(type -> Component.translatable(TYPE_KEY + type.getId()))
                .withValues(VoidTradeStepType.values())
                .withInitialValue(VoidTradeStepType.MOVE_TO)
                .create(this.editorLeft, TOP, this.editorWidth, 20, Component.translatable(KEY + "type"), (button, type) -> {
                    VoidTradeStep step = getSelectedStep();
                    if (step != null) {
                        // ItemScroller があれば、新しく作る取引ステップはお気に入り全部を対象にする（AHK と同じ）
                        if (type == VoidTradeStepType.TRADE && step.getType() != VoidTradeStepType.TRADE && ItemScrollerCompat.isLoaded()) {
                            step.useFavorites = true;
                        }
                        step.type = type;
                        refreshEditor();
                    }
                }));

        int coordWidth = (this.editorWidth - COORD_LABEL_WIDTH * 3 - 8) / 3;
        this.xField = addIntField(coordWidth, true, (step, value) -> step.x = value);
        this.yField = addIntField(coordWidth, true, (step, value) -> step.y = value);
        this.zField = addIntField(coordWidth, true, (step, value) -> step.z = value);

        int halfWidth = (this.editorWidth - 4) / 2;
        this.herePosButton = this.addRenderableWidget(Button.builder(Component.translatable(KEY + "here"), b -> setPosition(currentPlayerPos()))
                .bounds(0, 0, halfWidth, 20)
                .tooltip(Tooltip.create(Component.translatable(KEY + "here.tooltip")))
                .build());
        this.lookPosButton = this.addRenderableWidget(Button.builder(Component.translatable(KEY + "look"), b -> setPosition(crosshairPos(false)))
                .bounds(0, 0, halfWidth, 20)
                .tooltip(Tooltip.create(Component.translatable(KEY + "look.tooltip")))
                .build());

        int paramWidth = this.editorWidth - this.paramLabelWidth;
        this.keepFacingButton = this.addRenderableWidget(CycleButton
                .<Boolean>builder(value -> Component.translatable(KEY + (value ? "facing.keep" : "facing.target")))
                .withValues(Boolean.FALSE, Boolean.TRUE)
                .withInitialValue(Boolean.FALSE)
                .withTooltip(value -> Tooltip.create(Component.translatable(KEY + "facing.tooltip")))
                .create(0, 0, this.editorWidth, 20, Component.translatable(KEY + "facing"), (button, value) -> {
                    VoidTradeStep step = getSelectedStep();
                    if (step != null) {
                        step.keepFacing = value;
                        refreshEditor();
                    }
                }));
        this.sprintButton = this.addRenderableWidget(CycleButton.onOffBuilder(true)
                .create(0, 0, this.editorWidth, 20, Component.translatable(KEY + "sprint"), (button, value) -> {
                    VoidTradeStep step = getSelectedStep();
                    if (step != null) {
                        step.sprint = value;
                    }
                }));
        this.radiusField = this.addRenderableWidget(new EditBox(this.font, 0, 0, paramWidth, 18, Component.translatable(KEY + "radius")));
        this.radiusField.setMaxLength(6);
        this.radiusField.setFilter(text -> text.matches("\\d{0,2}(\\.\\d{0,2})?"));
        this.radiusField.setResponder(text -> {
            VoidTradeStep step = getSelectedStep();
            if (this.updatingFields || step == null) {
                return;
            }
            try {
                step.radius = Math.max(0.5, Math.min(16.0, Double.parseDouble(text)));
            } catch (NumberFormatException ignored) {
            }
        });
        this.nudgeDirectionButton = this.addRenderableWidget(CycleButton
                .<MoveDirection>builder(direction -> Component.translatable("config." + ASTTweaks.MOD_ID + ".automove.direction." + direction.getId()))
                .withValues(MoveDirection.values())
                .withInitialValue(MoveDirection.LEFT)
                .create(0, 0, this.editorWidth, 20, Component.translatable(KEY + "nudgeDirection"), (button, value) -> {
                    VoidTradeStep step = getSelectedStep();
                    if (step != null) {
                        step.nudgeDirection = value;
                    }
                }));
        this.nudgeTicksField = addIntField(paramWidth, false, (step, value) -> step.nudgeTicks = Math.max(1, Math.min(200, value)));
        this.ticksField = addIntField(paramWidth, false, (step, value) -> step.ticks = Math.max(1, value));
        this.favoritesButton = this.addRenderableWidget(CycleButton
                .<Boolean>builder(value -> Component.translatable(KEY + (value ? "target.favorites" : "target.number")))
                .withValues(Boolean.FALSE, Boolean.TRUE)
                .withInitialValue(Boolean.FALSE)
                .withTooltip(value -> Tooltip.create(Component.translatable(KEY + "target.tooltip")))
                .create(0, 0, this.editorWidth, 20, Component.translatable(KEY + "target"), (button, value) -> {
                    VoidTradeStep step = getSelectedStep();
                    if (step != null) {
                        step.useFavorites = value;
                        refreshEditor();
                    }
                }));
        this.offerField = addIntField(paramWidth, false, (step, value) -> step.offerIndex = Math.max(1, value));
        this.dropOutputButton = this.addRenderableWidget(CycleButton.onOffBuilder(false)
                .create(0, 0, this.editorWidth, 20, Component.translatable(KEY + "dropOutput"), (button, value) -> {
                    VoidTradeStep step = getSelectedStep();
                    if (step != null) {
                        step.dropOutput = value;
                    }
                }));
        this.commandField = this.addRenderableWidget(new EditBox(this.font, 0, 0, paramWidth, 18, Component.translatable(KEY + "command")));
        this.commandField.setMaxLength(256);
        this.commandField.setResponder(text -> {
            VoidTradeStep step = getSelectedStep();
            if (!this.updatingFields && step != null) {
                step.command = text;
            }
        });
        this.firstLoopOnlyButton = this.addRenderableWidget(CycleButton.onOffBuilder(false)
                .withTooltip(value -> Tooltip.create(Component.translatable(KEY + "firstLoopOnly.tooltip")))
                .create(0, 0, this.editorWidth, 20, Component.translatable(KEY + "firstLoopOnly"), (button, value) -> {
                    VoidTradeStep step = getSelectedStep();
                    if (step != null) {
                        step.firstLoopOnly = value;
                    }
                }));

        rebuildList();
    }

    private EditBox addIntField(int width, boolean allowNegative, ObjIntConsumer<VoidTradeStep> setter) {
        EditBox field = new EditBox(this.font, 0, 0, width, 18, Component.empty());
        field.setMaxLength(allowNegative ? 9 : 8);
        String pattern = allowNegative ? "-?\\d{0,8}" : "\\d{0,8}";
        field.setFilter(text -> text.matches(pattern));
        field.setResponder(text -> {
            VoidTradeStep step = getSelectedStep();
            if (this.updatingFields || step == null) {
                return;
            }
            try {
                setter.accept(step, Integer.parseInt(text));
            } catch (NumberFormatException ignored) {
            }
        });
        return this.addRenderableWidget(field);
    }

    private VoidTradeStep getSelectedStep() {
        return this.selectedIndex >= 0 && this.selectedIndex < this.steps.size() ? this.steps.get(this.selectedIndex) : null;
    }

    private void selectStep(int index) {
        if (index != this.selectedIndex) {
            this.selectedIndex = index;
            refreshEditor();
        }
    }

    private void rebuildList() {
        if (this.selectedIndex >= this.steps.size()) {
            this.selectedIndex = this.steps.size() - 1;
        }
        this.stepList.rebuild(this.steps.size(), this.selectedIndex);
        refreshEditor();
    }

    /^*
     * 選択中ステップの種別に合わせて入力欄の表示・配置・値を切り替える。
     ^/
    private void refreshEditor() {
        VoidTradeStep step = getSelectedStep();
        VoidTradeStepType type = step != null ? step.getType() : null;
        this.labels.clear();
        this.setFocused(null);
        this.updatingFields = true;

        this.typeButton.visible = step != null;
        if (step != null) {
            this.typeButton.setValue(type);
        }

        int y = TOP + ROW_HEIGHT;
        boolean usesPosition = type != null && type.usesPosition();
        this.xField.setVisible(usesPosition);
        this.yField.setVisible(usesPosition);
        this.zField.setVisible(usesPosition);
        this.herePosButton.visible = usesPosition;
        this.lookPosButton.visible = usesPosition;
        if (usesPosition) {
            int x = this.editorLeft;
            x = placeCoordField(this.xField, "X", step.x, x, y);
            x = placeCoordField(this.yField, "Y", step.y, x, y);
            placeCoordField(this.zField, "Z", step.z, x, y);
            y += ROW_HEIGHT;
            this.herePosButton.setPosition(this.editorLeft, y);
            this.lookPosButton.setPosition(this.editorLeft + this.herePosButton.getWidth() + 4, y);
            y += ROW_HEIGHT;
        }

        boolean trade = type == VoidTradeStepType.TRADE;
        this.keepFacingButton.visible = type == VoidTradeStepType.MOVE_TO;
        this.sprintButton.visible = type == VoidTradeStepType.MOVE_TO && !step.keepFacing;
        this.nudgeDirectionButton.visible = type == VoidTradeStepType.NUDGE;
        this.nudgeTicksField.setVisible(type == VoidTradeStepType.NUDGE);
        this.radiusField.setVisible(type == VoidTradeStepType.INTERACT_ENTITY);
        this.ticksField.setVisible(type == VoidTradeStepType.WAIT);
        this.favoritesButton.visible = trade;
        this.offerField.setVisible(trade && !step.useFavorites);
        this.dropOutputButton.visible = trade;
        this.commandField.setVisible(type == VoidTradeStepType.COMMAND);
        this.firstLoopOnlyButton.visible = step != null;
        if (type != null) {
            switch (type) {
                case MOVE_TO -> {
                    y = placeButton(this.keepFacingButton, step.keepFacing, y);
                    if (!step.keepFacing) {
                        y = placeButton(this.sprintButton, step.sprint, y);
                    }
                }
                case NUDGE -> {
                    y = placeButton(this.nudgeDirectionButton, step.getNudgeDirection(), y);
                    y = placeParamField(this.nudgeTicksField, KEY + "nudgeTicks", String.valueOf(step.nudgeTicks), y);
                }
                case INTERACT_ENTITY -> y = placeParamField(this.radiusField, KEY + "radius", formatRadius(step.radius), y);
                case WAIT -> y = placeParamField(this.ticksField, KEY + "ticks", String.valueOf(step.ticks), y);
                case TRADE -> {
                    y = placeButton(this.favoritesButton, step.useFavorites, y);
                    if (!step.useFavorites) {
                        y = placeParamField(this.offerField, KEY + "offer", String.valueOf(step.offerIndex), y);
                    }
                    y = placeButton(this.dropOutputButton, step.dropOutput, y);
                }
                case COMMAND -> y = placeParamField(this.commandField, KEY + "command", step.command != null ? step.command : "", y);
                default -> {
                }
            }
            y = placeButton(this.firstLoopOnlyButton, step.firstLoopOnly, y);
        }
        this.descriptionY = y + 4;

        this.updatingFields = false;
        updateButtons();
    }

    private int placeCoordField(EditBox field, String label, int value, int x, int y) {
        this.labels.add(new Label(Component.literal(label), x, y + 6));
        field.setPosition(x + COORD_LABEL_WIDTH, y + 1);
        field.setValue(String.valueOf(value));
        return x + COORD_LABEL_WIDTH + field.getWidth() + 4;
    }

    private <T> int placeButton(CycleButton<T> button, T value, int y) {
        button.setPosition(this.editorLeft, y);
        button.setValue(value);
        return y + ROW_HEIGHT;
    }

    private int placeParamField(EditBox field, String labelKey, String value, int y) {
        this.labels.add(new Label(Component.translatable(labelKey), this.editorLeft, y + 6));
        field.setPosition(this.editorLeft + this.paramLabelWidth, y + 1);
        field.setValue(value);
        return y + ROW_HEIGHT;
    }

    private static String formatRadius(double radius) {
        return radius == Math.floor(radius) ? String.valueOf((int) radius) : String.format(Locale.ROOT, "%.2f", radius);
    }

    private void updateButtons() {
        boolean hasStep = getSelectedStep() != null;
        boolean inWorld = this.minecraft != null && this.minecraft.player != null;
        this.copyButton.active = !this.steps.isEmpty();
        this.duplicateButton.active = hasStep;
        this.removeButton.active = hasStep;
        this.upButton.active = hasStep && this.selectedIndex > 0;
        this.downButton.active = hasStep && this.selectedIndex < this.steps.size() - 1;
        this.herePosButton.active = inWorld;
        this.lookPosButton.active = inWorld;
        this.startStopButton.setMessage(Component.translatable(KEY + (this.feature.isRunning() ? "stop" : "start")));
        this.startStopButton.active = inWorld && (this.feature.isRunning() || !this.steps.isEmpty());
    }

    // ------------------------------------------------------------
    // 座標の取り込み
    // ------------------------------------------------------------

    private BlockPos currentPlayerPos() {
        return this.minecraft != null && this.minecraft.player != null ? this.minecraft.player.blockPosition() : null;
    }

    /^*
     * 画面を開く直前に見ていたブロック／エンティティの座標。merchantOnly なら村人（行商人含む）のときだけ返す。
     ^/
    private BlockPos crosshairPos(boolean merchantOnly) {
        if (this.minecraft == null) {
            return null;
        }
        HitResult hit = this.minecraft.hitResult;
        if (hit instanceof EntityHitResult entityHit) {
            return !merchantOnly || entityHit.getEntity() instanceof AbstractVillager ? entityHit.getEntity().blockPosition() : null;
        }
        if (!merchantOnly && hit instanceof BlockHitResult blockHit && hit.getType() == HitResult.Type.BLOCK) {
            return blockHit.getBlockPos();
        }
        return null;
    }

    /^*
     * プレイヤーの手が届く範囲で一番近い村人（行商人含む）の座標。
     ^/
    private BlockPos nearestMerchantPos() {
        if (this.minecraft == null || this.minecraft.player == null || this.minecraft.level == null) {
            return null;
        }
        AABB area = this.minecraft.player.getBoundingBox().inflate(6.0);
        return this.minecraft.level.getEntitiesOfClass(AbstractVillager.class, area, entity -> entity.isAlive() && !entity.isBaby())
                .stream()
                .min(Comparator.comparingDouble(entity -> entity.distanceToSqr(this.minecraft.player)))
                .map(Entity::blockPosition)
                .orElse(null);
    }

    private void setPosition(BlockPos pos) {
        VoidTradeStep step = getSelectedStep();
        if (step == null || pos == null) {
            return;
        }
        step.x = pos.getX();
        step.y = pos.getY();
        step.z = pos.getZ();
        refreshEditor();
    }

    // ------------------------------------------------------------
    // 一覧操作
    // ------------------------------------------------------------

    private void insertSteps(List<VoidTradeStep> newSteps) {
        int index = this.selectedIndex >= 0 ? this.selectedIndex + 1 : this.steps.size();
        this.steps.addAll(index, newSteps);
        this.selectedIndex = index;
        rebuildList();
    }

    private void addStep() {
        VoidTradeStep step = new VoidTradeStep();
        BlockPos pos = currentPlayerPos();
        VoidTradeStep base = getSelectedStep();
        if (pos != null) {
            step.x = pos.getX();
            step.y = pos.getY();
            step.z = pos.getZ();
        } else if (base != null) {
            step.x = base.x;
            step.y = base.y;
            step.z = base.z;
        }
        insertSteps(List.of(step));
    }

    private void duplicateStep() {
        VoidTradeStep step = getSelectedStep();
        if (step != null) {
            insertSteps(List.of(step.copy()));
        }
    }

    private void removeStep() {
        if (getSelectedStep() == null) {
            return;
        }
        this.steps.remove(this.selectedIndex);
        rebuildList();
    }

    private void moveStep(int delta) {
        int target = this.selectedIndex + delta;
        if (getSelectedStep() == null || target < 0 || target >= this.steps.size()) {
            return;
        }
        Collections.swap(this.steps, this.selectedIndex, target);
        this.selectedIndex = target;
        rebuildList();
    }

    /^*
     * 歩いて離れる典型的なボイドトレードの手順を作る。座標は現在地（村人を見ていればその位置）で仮置きする。
     ^/
    void applyTemplate() {
        BlockPos here = currentPlayerPos();
        if (here == null) {
            here = BlockPos.ZERO;
        }
        BlockPos merchantPos = crosshairPos(true);
        if (merchantPos == null) {
            merchantPos = here;
        }

        VoidTradeStep wait = new VoidTradeStep(VoidTradeStepType.WAIT, 0, 0, 0);
        wait.ticks = 40;
        replaceWorkflow(List.of(
                new VoidTradeStep(VoidTradeStepType.MOVE_TO, here.getX(), here.getY(), here.getZ()),
                new VoidTradeStep(VoidTradeStepType.INTERACT_ENTITY, merchantPos.getX(), merchantPos.getY(), merchantPos.getZ()),
                new VoidTradeStep(VoidTradeStepType.MOVE_TO, here.getX(), here.getY(), here.getZ()),
                new VoidTradeStep(VoidTradeStepType.WAIT_UNLOAD, 0, 0, 0),
                wait,
                new VoidTradeStep(VoidTradeStepType.TRADE, 0, 0, 0),
                new VoidTradeStep(VoidTradeStepType.CLOSE_SCREEN, 0, 0, 0)),
                Component.translatable(KEY + "template.created"));
    }

    /^*
     * 簡易ボイトレ（AST VT.ahk）と同じ手順を作る。
     * 左に少し歩いて（ゲートウェイで村人の所へ飛ぶ）→ 右クリックで開く → 待つ（元の場所へ戻され村人がアンロードされる）
     * → 取引 → 閉じる → 補充を待つ を繰り返す。
     * 村人は視線の先（なければ手の届く範囲で一番近い村人）。取引は ItemScroller があればお気に入りを
     * AsutanTweaks の MassTrade と同じく結果を捨てながら取引する。
     ^/
    void applySimplePreset() {
        BlockPos merchantPos = crosshairPos(true);
        if (merchantPos == null) {
            merchantPos = nearestMerchantPos();
        }
        boolean favorites = ItemScrollerCompat.isLoaded();
        Component doneNotice = Component.translatable(KEY + (merchantPos == null ? "simple.noVillager"
                : favorites ? "simple.createdFavorites" : "simple.created"));
        if (merchantPos == null) {
            merchantPos = currentPlayerPos() != null ? currentPlayerPos() : BlockPos.ZERO;
        }

        // AHK の {a down}{a up}（100ms）相当。元の場所へ戻されるたびにゲートウェイへ入り直すので毎周行う
        VoidTradeStep nudge = new VoidTradeStep(VoidTradeStepType.NUDGE, 0, 0, 0);
        nudge.nudgeDirection = MoveDirection.LEFT;
        nudge.nudgeTicks = 2;
        VoidTradeStep beforeTrade = new VoidTradeStep(VoidTradeStepType.WAIT, 0, 0, 0);
        beforeTrade.ticks = 90;     // AHK: 開いた後 2.5 秒 + 次周の 2 秒
        VoidTradeStep trade = new VoidTradeStep(VoidTradeStepType.TRADE, 0, 0, 0);
        trade.useFavorites = favorites;
        trade.dropOutput = true;    // AsutanTweaks の MassTrade（結果を Ctrl+Q で捨てる）と同じ
        VoidTradeStep refill = new VoidTradeStep(VoidTradeStepType.WAIT, 0, 0, 0);
        refill.ticks = 60;          // AHK: 閉じた後の補充待ち 3 秒
        replaceWorkflow(List.of(
                nudge,
                new VoidTradeStep(VoidTradeStepType.INTERACT_ENTITY, merchantPos.getX(), merchantPos.getY(), merchantPos.getZ()),
                beforeTrade,
                trade,
                new VoidTradeStep(VoidTradeStepType.CLOSE_SCREEN, 0, 0, 0),
                refill),
                doneNotice);
    }

    /^*
     * ワークフロー全体を差し替える。既存のステップがあれば確認を挟む。
     ^/
    private void replaceWorkflow(List<VoidTradeStep> newSteps, Component doneNotice) {
        if (this.steps.isEmpty()) {
            applyWorkflow(newSteps, doneNotice);
            return;
        }
        confirmThen(Component.translatable(KEY + "replace.title"), Component.translatable(KEY + "replace.message", this.steps.size()),
                () -> applyWorkflow(newSteps, doneNotice));
    }

    private void confirmThen(Component title, Component message, Runnable action) {
        McClient.setScreen(new ConfirmScreen(confirmed -> {
            if (confirmed) {
                action.run();
            }
            McClient.setScreen(this);
        }, title, message));
    }

    /^*
     * 保存したプリセットを今のワークフローとして読み込む（プリセット画面から呼ぶ）。
     ^/
    void loadPreset(VoidTradePreset preset) {
        replaceWorkflow(VoidTradePreset.copySteps(preset.steps), Component.translatable(KEY + "presets.loaded", preset.name));
    }

    List<VoidTradeStep> getSteps() {
        return this.steps;
    }

    private void copyWorkflow() {
        if (this.steps.isEmpty()) {
            return;
        }
        this.minecraft.keyboardHandler.setClipboard(VoidTradeShare.export(this.steps));
        showNotice(Component.translatable(KEY + "copy.done", this.steps.size()));
    }

    private void pasteWorkflow() {
        List<VoidTradeStep> pasted = VoidTradeShare.parse(this.minecraft.keyboardHandler.getClipboard());
        if (pasted == null) {
            showNotice(Component.translatable(KEY + "paste.invalid"));
            return;
        }
        Component doneNotice = Component.translatable(KEY + "paste.done", pasted.size());
        List<String> commands = pasted.stream()
                .filter(step -> step.getType() == VoidTradeStepType.COMMAND)
                .map(step -> "/" + step.command.trim().replaceFirst("¹^/", ""))
                .collect(Collectors.toList());
        if (!commands.isEmpty()) {
            // 貼り付けたワークフローが知らないうちにコマンドを送らないよう、中身を見せて確認する
            confirmThen(Component.translatable(KEY + "paste.commandTitle"),
                    Component.translatable(KEY + "paste.commandMessage", String.join(", ", commands)),
                    () -> applyWorkflow(pasted, doneNotice));
            return;
        }
        replaceWorkflow(pasted, doneNotice);
    }

    void showNotice(Component text) {
        this.notice = text;
        this.noticeTicks = 200;
    }

    private void applyWorkflow(List<VoidTradeStep> newSteps, Component doneNotice) {
        this.feature.stop();
        this.steps.clear();
        this.steps.addAll(newSteps);
        this.selectedIndex = 0;
        rebuildList();
        showNotice(doneNotice);
    }

    private void toggleRunning() {
        if (this.feature.isRunning()) {
            this.feature.stop();
            updateButtons();
            return;
        }
        // 編集画面から開始する場合は、機能が無効でも有効化してから始める
        if (!this.feature.isEnabled()) {
            this.feature.setEnabled(true);
        }
        McClient.setScreen(null);
        this.feature.start();
    }

    // ------------------------------------------------------------
    // Screen
    // ------------------------------------------------------------

    @Override
    public void tick() {
        super.tick();
        this.xField.tick();
        this.yField.tick();
        this.zField.tick();
        this.radiusField.tick();
        this.nudgeTicksField.tick();
        this.ticksField.tick();
        this.offerField.tick();
        this.commandField.tick();
        if (this.noticeTicks > 0) {
            this.noticeTicks--;
        }
        updateButtons();
    }

    @Override
    protected void draw(Gfx gfx, int mouseX, int mouseY, float delta) {
        gfx.fill(this.listLeft, TOP, this.listLeft + this.listWidth, this.height - 58, 0x80000000);
        gfx.widget(this.stepList, mouseX, mouseY, delta);

        gfx.centeredText(this.font, this.title, this.width / 2, 8, 0xFFFFFF);
        if (this.noticeTicks > 0 && this.notice != null) {
            gfx.centeredText(this.font, this.notice, this.width / 2, 20, 0xFFFF55);
        } else {
            gfx.centeredText(this.font, Component.translatable(KEY + "help"), this.width / 2, 20, 0xA0A0A0);
        }

        if (this.steps.isEmpty()) {
            int y = TOP + 8;
            for (FormattedCharSequence line : this.font.split(Component.translatable(KEY + "empty"), this.listWidth - 12)) {
                gfx.text(this.font, line, this.listLeft + 6, y, 0xA0A0A0);
                y += 10;
            }
        }

        for (Label label : this.labels) {
            gfx.text(this.font, label.text(), label.x(), label.y(), 0xE0E0E0);
        }

        VoidTradeStep step = getSelectedStep();
        if (step != null) {
            int y = this.descriptionY;
            Component description = Component.translatable(TYPE_KEY + step.getType().getId() + ".desc");
            for (FormattedCharSequence line : this.font.split(description, this.editorWidth)) {
                if (y + 9 > this.height - 32) {
                    break;
                }
                gfx.text(this.font, line, this.editorLeft, y, 0xA0A0A0);
                y += 10;
            }
        }

        renderWidgets(gfx, mouseX, mouseY, delta);
    }

    @Override
    public void onClose() {
        McClient.setScreen(this.parent);
    }

    @Override
    public void removed() {
        this.feature.getConfig().save();
        super.removed();
    }

    class StepListWidget extends CompatSelectionList<StepEntry> {
        StepListWidget(Minecraft client, int width, int height, int top, int bottom, int itemHeight) {
            super(client, width, height, top, bottom, itemHeight);
        }

        void rebuild(int count, int selected) {
            this.clearEntries();
            for (int i = 0; i < count; i++) {
                this.addEntry(new StepEntry(i));
            }
            StepEntry entry = selected >= 0 && selected < count ? this.children().get(selected) : null;
            super.setSelected(entry);
            if (entry != null) {
                this.ensureVisible(entry);
            }
        }

        @Override
        public void setSelected(StepEntry entry) {
            super.setSelected(entry);
            if (entry != null) {
                VoidTradeWorkflowScreen.this.selectStep(entry.index);
            }
        }

        @Override
        public int getRowWidth() {
            return this.width - 20;
        }

        @Override
        protected int getScrollbarPosition() {
            return this.x0 + this.width - 6;
        }
    }

    class StepEntry extends CompatListEntry<StepEntry> {
        private final int index;

        StepEntry(int index) {
            this.index = index;
        }

        @Override
        protected void drawEntry(Gfx gfx, int x, int y, int entryWidth, int entryHeight,
                                 int mouseX, int mouseY, boolean hovered, float tickDelta) {
            if (this.index >= VoidTradeWorkflowScreen.this.steps.size()) {
                return;
            }
            boolean current = VoidTradeWorkflowScreen.this.feature.getCurrentStepIndex() == this.index;
            MutableComponent text = Component.literal((current ? "▶ " : "") + (this.index + 1) + ". ")
                    .append(VoidTradeWorkflowScreen.this.steps.get(this.index).describe());
            List<FormattedCharSequence> lines = VoidTradeWorkflowScreen.this.font.split(text, entryWidth - 4);
            if (!lines.isEmpty()) {
                gfx.text(VoidTradeWorkflowScreen.this.font, lines.get(0), x + 2, y + 2,
                        current ? 0xFFFF55 : 0xFFFFFF);
            }
        }

        @Override
        protected boolean onMouseClicked(double mouseX, double mouseY, int button) {
            return button == 0;
        }

        @Override
        public Component getNarration() {
            return this.index < VoidTradeWorkflowScreen.this.steps.size()
                    ? VoidTradeWorkflowScreen.this.steps.get(this.index).describe()
                    : Component.empty();
        }
    }
}
*///?}

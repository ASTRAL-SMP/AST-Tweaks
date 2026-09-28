package com.astral.asttweaks.feature.voidtrade.gui;

import com.astral.asttweaks.ASTTweaks;
import com.astral.asttweaks.compat.ItemScrollerCompat;
import com.astral.asttweaks.feature.automove.MoveDirection;
import com.astral.asttweaks.feature.FeatureManager;
import com.astral.asttweaks.feature.voidtrade.VoidTradeFeature;
import com.astral.asttweaks.feature.voidtrade.VoidTradePreset;
import com.astral.asttweaks.feature.voidtrade.VoidTradeShare;
import com.astral.asttweaks.feature.voidtrade.VoidTradeStep;
import com.astral.asttweaks.feature.voidtrade.VoidTradeStepType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.MerchantEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.ObjIntConsumer;
import java.util.stream.Collectors;

/**
 * Void Trade のワークフロー（ステップ列）編集画面。
 * 左にステップ一覧、右に選択中ステップの詳細を表示する。編集内容は設定のリストへ直接反映し、閉じるときに保存する。
 */
public class VoidTradeWorkflowScreen extends Screen {
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

    private CyclingButtonWidget<VoidTradeStepType> typeButton;
    private TextFieldWidget xField;
    private TextFieldWidget yField;
    private TextFieldWidget zField;
    private ButtonWidget herePosButton;
    private ButtonWidget lookPosButton;
    private CyclingButtonWidget<Boolean> keepFacingButton;
    private CyclingButtonWidget<Boolean> sprintButton;
    private CyclingButtonWidget<MoveDirection> nudgeDirectionButton;
    private TextFieldWidget nudgeTicksField;
    private TextFieldWidget radiusField;
    private TextFieldWidget ticksField;
    private CyclingButtonWidget<Boolean> favoritesButton;
    private TextFieldWidget offerField;
    private CyclingButtonWidget<Boolean> dropOutputButton;
    private TextFieldWidget commandField;
    private CyclingButtonWidget<Boolean> firstLoopOnlyButton;

    private ButtonWidget duplicateButton;
    private ButtonWidget removeButton;
    private ButtonWidget upButton;
    private ButtonWidget downButton;
    private ButtonWidget copyButton;
    private ButtonWidget startStopButton;

    private Text notice;
    private int noticeTicks;

    private record Label(Text text, int x, int y) {}

    public VoidTradeWorkflowScreen(Screen parent) {
        super(Text.translatable(KEY + "title"));
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

        this.stepList = new StepListWidget(this.client, this.listWidth, this.height, TOP, this.height - 58, LIST_ITEM_HEIGHT);
        this.stepList.setLeftPos(this.listLeft);
        this.stepList.setRenderBackground(false);
        this.stepList.setRenderHorizontalShadows(false);
        this.addSelectableChild(this.stepList);

        // 一覧の操作ボタン
        int opY = this.height - 54;
        int gap = 4;
        int opWidth = (this.listWidth - gap * 4) / 5;
        this.addDrawableChild(ButtonWidget.builder(Text.translatable(KEY + "add"), b -> addStep())
                .dimensions(this.listLeft, opY, opWidth, 20)
                .tooltip(Tooltip.of(Text.translatable(KEY + "add.tooltip")))
                .build());
        this.duplicateButton = this.addDrawableChild(ButtonWidget.builder(Text.translatable(KEY + "duplicate"), b -> duplicateStep())
                .dimensions(this.listLeft + (opWidth + gap), opY, opWidth, 20)
                .build());
        this.removeButton = this.addDrawableChild(ButtonWidget.builder(Text.translatable(KEY + "remove"), b -> removeStep())
                .dimensions(this.listLeft + (opWidth + gap) * 2, opY, opWidth, 20)
                .build());
        this.upButton = this.addDrawableChild(ButtonWidget.builder(Text.literal("↑"), b -> moveStep(-1))
                .dimensions(this.listLeft + (opWidth + gap) * 3, opY, opWidth, 20)
                .build());
        this.downButton = this.addDrawableChild(ButtonWidget.builder(Text.literal("↓"), b -> moveStep(1))
                .dimensions(this.listLeft + (opWidth + gap) * 4, opY, opWidth, 20)
                .build());

        // 下段
        int bottomY = this.height - 28;
        int bottomGap = 6;
        int bottomWidth = Math.min(100, (this.width - 20 - bottomGap * 4) / 5);
        int bottomLeft = this.width / 2 - (bottomWidth * 5 + bottomGap * 4) / 2;
        this.addDrawableChild(ButtonWidget.builder(Text.translatable(KEY + "presets"), b -> this.client.setScreen(new VoidTradePresetScreen(this)))
                .dimensions(bottomLeft, bottomY, bottomWidth, 20)
                .tooltip(Tooltip.of(Text.translatable(KEY + "presets.tooltip")))
                .build());
        this.copyButton = this.addDrawableChild(ButtonWidget.builder(Text.translatable(KEY + "copy"), b -> copyWorkflow())
                .dimensions(bottomLeft + (bottomWidth + bottomGap), bottomY, bottomWidth, 20)
                .tooltip(Tooltip.of(Text.translatable(KEY + "copy.tooltip")))
                .build());
        this.addDrawableChild(ButtonWidget.builder(Text.translatable(KEY + "paste"), b -> pasteWorkflow())
                .dimensions(bottomLeft + (bottomWidth + bottomGap) * 2, bottomY, bottomWidth, 20)
                .tooltip(Tooltip.of(Text.translatable(KEY + "paste.tooltip")))
                .build());
        this.startStopButton = this.addDrawableChild(ButtonWidget.builder(Text.translatable(KEY + "start"), b -> toggleRunning())
                .dimensions(bottomLeft + (bottomWidth + bottomGap) * 3, bottomY, bottomWidth, 20)
                .build());
        this.addDrawableChild(ButtonWidget.builder(Text.translatable("gui.done"), b -> close())
                .dimensions(bottomLeft + (bottomWidth + bottomGap) * 4, bottomY, bottomWidth, 20)
                .build());

        // 詳細エディタ（種別に応じて refreshEditor で表示・配置を切り替える）
        this.typeButton = this.addDrawableChild(CyclingButtonWidget
                .<VoidTradeStepType>builder(type -> Text.translatable(TYPE_KEY + type.getId()))
                .values(VoidTradeStepType.values())
                .initially(VoidTradeStepType.MOVE_TO)
                .build(this.editorLeft, TOP, this.editorWidth, 20, Text.translatable(KEY + "type"), (button, type) -> {
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
        this.herePosButton = this.addDrawableChild(ButtonWidget.builder(Text.translatable(KEY + "here"), b -> setPosition(currentPlayerPos()))
                .dimensions(0, 0, halfWidth, 20)
                .tooltip(Tooltip.of(Text.translatable(KEY + "here.tooltip")))
                .build());
        this.lookPosButton = this.addDrawableChild(ButtonWidget.builder(Text.translatable(KEY + "look"), b -> setPosition(crosshairPos(false)))
                .dimensions(0, 0, halfWidth, 20)
                .tooltip(Tooltip.of(Text.translatable(KEY + "look.tooltip")))
                .build());

        int paramWidth = this.editorWidth - this.paramLabelWidth;
        this.keepFacingButton = this.addDrawableChild(CyclingButtonWidget
                .<Boolean>builder(value -> Text.translatable(KEY + (value ? "facing.keep" : "facing.target")))
                .values(Boolean.FALSE, Boolean.TRUE)
                .initially(Boolean.FALSE)
                .tooltip(value -> Tooltip.of(Text.translatable(KEY + "facing.tooltip")))
                .build(0, 0, this.editorWidth, 20, Text.translatable(KEY + "facing"), (button, value) -> {
                    VoidTradeStep step = getSelectedStep();
                    if (step != null) {
                        step.keepFacing = value;
                        refreshEditor();
                    }
                }));
        this.sprintButton = this.addDrawableChild(CyclingButtonWidget.onOffBuilder(true)
                .build(0, 0, this.editorWidth, 20, Text.translatable(KEY + "sprint"), (button, value) -> {
                    VoidTradeStep step = getSelectedStep();
                    if (step != null) {
                        step.sprint = value;
                    }
                }));
        this.radiusField = this.addDrawableChild(new TextFieldWidget(this.textRenderer, 0, 0, paramWidth, 18, Text.translatable(KEY + "radius")));
        this.radiusField.setMaxLength(6);
        this.radiusField.setTextPredicate(text -> text.matches("\\d{0,2}(\\.\\d{0,2})?"));
        this.radiusField.setChangedListener(text -> {
            VoidTradeStep step = getSelectedStep();
            if (this.updatingFields || step == null) {
                return;
            }
            try {
                step.radius = Math.max(0.5, Math.min(16.0, Double.parseDouble(text)));
            } catch (NumberFormatException ignored) {
            }
        });
        this.nudgeDirectionButton = this.addDrawableChild(CyclingButtonWidget
                .<MoveDirection>builder(direction -> Text.translatable("config." + ASTTweaks.MOD_ID + ".automove.direction." + direction.getId()))
                .values(MoveDirection.values())
                .initially(MoveDirection.LEFT)
                .build(0, 0, this.editorWidth, 20, Text.translatable(KEY + "nudgeDirection"), (button, value) -> {
                    VoidTradeStep step = getSelectedStep();
                    if (step != null) {
                        step.nudgeDirection = value;
                    }
                }));
        this.nudgeTicksField = addIntField(paramWidth, false, (step, value) -> step.nudgeTicks = Math.max(1, Math.min(200, value)));
        this.ticksField = addIntField(paramWidth, false, (step, value) -> step.ticks = Math.max(1, value));
        this.favoritesButton = this.addDrawableChild(CyclingButtonWidget
                .<Boolean>builder(value -> Text.translatable(KEY + (value ? "target.favorites" : "target.number")))
                .values(Boolean.FALSE, Boolean.TRUE)
                .initially(Boolean.FALSE)
                .tooltip(value -> Tooltip.of(Text.translatable(KEY + "target.tooltip")))
                .build(0, 0, this.editorWidth, 20, Text.translatable(KEY + "target"), (button, value) -> {
                    VoidTradeStep step = getSelectedStep();
                    if (step != null) {
                        step.useFavorites = value;
                        refreshEditor();
                    }
                }));
        this.offerField = addIntField(paramWidth, false, (step, value) -> step.offerIndex = Math.max(1, value));
        this.dropOutputButton = this.addDrawableChild(CyclingButtonWidget.onOffBuilder(false)
                .build(0, 0, this.editorWidth, 20, Text.translatable(KEY + "dropOutput"), (button, value) -> {
                    VoidTradeStep step = getSelectedStep();
                    if (step != null) {
                        step.dropOutput = value;
                    }
                }));
        this.commandField = this.addDrawableChild(new TextFieldWidget(this.textRenderer, 0, 0, paramWidth, 18, Text.translatable(KEY + "command")));
        this.commandField.setMaxLength(256);
        this.commandField.setChangedListener(text -> {
            VoidTradeStep step = getSelectedStep();
            if (!this.updatingFields && step != null) {
                step.command = text;
            }
        });
        this.firstLoopOnlyButton = this.addDrawableChild(CyclingButtonWidget.onOffBuilder(false)
                .tooltip(value -> Tooltip.of(Text.translatable(KEY + "firstLoopOnly.tooltip")))
                .build(0, 0, this.editorWidth, 20, Text.translatable(KEY + "firstLoopOnly"), (button, value) -> {
                    VoidTradeStep step = getSelectedStep();
                    if (step != null) {
                        step.firstLoopOnly = value;
                    }
                }));

        rebuildList();
    }

    private TextFieldWidget addIntField(int width, boolean allowNegative, ObjIntConsumer<VoidTradeStep> setter) {
        TextFieldWidget field = new TextFieldWidget(this.textRenderer, 0, 0, width, 18, Text.empty());
        field.setMaxLength(allowNegative ? 9 : 8);
        String pattern = allowNegative ? "-?\\d{0,8}" : "\\d{0,8}";
        field.setTextPredicate(text -> text.matches(pattern));
        field.setChangedListener(text -> {
            VoidTradeStep step = getSelectedStep();
            if (this.updatingFields || step == null) {
                return;
            }
            try {
                setter.accept(step, Integer.parseInt(text));
            } catch (NumberFormatException ignored) {
            }
        });
        return this.addDrawableChild(field);
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

    /**
     * 選択中ステップの種別に合わせて入力欄の表示・配置・値を切り替える。
     */
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

    private int placeCoordField(TextFieldWidget field, String label, int value, int x, int y) {
        this.labels.add(new Label(Text.literal(label), x, y + 6));
        field.setPosition(x + COORD_LABEL_WIDTH, y + 1);
        field.setText(String.valueOf(value));
        return x + COORD_LABEL_WIDTH + field.getWidth() + 4;
    }

    private <T> int placeButton(CyclingButtonWidget<T> button, T value, int y) {
        button.setPosition(this.editorLeft, y);
        button.setValue(value);
        return y + ROW_HEIGHT;
    }

    private int placeParamField(TextFieldWidget field, String labelKey, String value, int y) {
        this.labels.add(new Label(Text.translatable(labelKey), this.editorLeft, y + 6));
        field.setPosition(this.editorLeft + this.paramLabelWidth, y + 1);
        field.setText(value);
        return y + ROW_HEIGHT;
    }

    private static String formatRadius(double radius) {
        return radius == Math.floor(radius) ? String.valueOf((int) radius) : String.format(Locale.ROOT, "%.2f", radius);
    }

    private void updateButtons() {
        boolean hasStep = getSelectedStep() != null;
        boolean inWorld = this.client != null && this.client.player != null;
        this.copyButton.active = !this.steps.isEmpty();
        this.duplicateButton.active = hasStep;
        this.removeButton.active = hasStep;
        this.upButton.active = hasStep && this.selectedIndex > 0;
        this.downButton.active = hasStep && this.selectedIndex < this.steps.size() - 1;
        this.herePosButton.active = inWorld;
        this.lookPosButton.active = inWorld;
        this.startStopButton.setMessage(Text.translatable(KEY + (this.feature.isRunning() ? "stop" : "start")));
        this.startStopButton.active = inWorld && (this.feature.isRunning() || !this.steps.isEmpty());
    }

    // ------------------------------------------------------------
    // 座標の取り込み
    // ------------------------------------------------------------

    private BlockPos currentPlayerPos() {
        return this.client != null && this.client.player != null ? this.client.player.getBlockPos() : null;
    }

    /**
     * 画面を開く直前に見ていたブロック／エンティティの座標。merchantOnly なら村人（行商人含む）のときだけ返す。
     */
    private BlockPos crosshairPos(boolean merchantOnly) {
        if (this.client == null) {
            return null;
        }
        HitResult hit = this.client.crosshairTarget;
        if (hit instanceof EntityHitResult entityHit) {
            return !merchantOnly || entityHit.getEntity() instanceof MerchantEntity ? entityHit.getEntity().getBlockPos() : null;
        }
        if (!merchantOnly && hit instanceof BlockHitResult blockHit && hit.getType() == HitResult.Type.BLOCK) {
            return blockHit.getBlockPos();
        }
        return null;
    }

    /**
     * プレイヤーの手が届く範囲で一番近い村人（行商人含む）の座標。
     */
    private BlockPos nearestMerchantPos() {
        if (this.client == null || this.client.player == null || this.client.world == null) {
            return null;
        }
        Box area = this.client.player.getBoundingBox().expand(6.0);
        return this.client.world.getEntitiesByClass(MerchantEntity.class, area, entity -> entity.isAlive() && !entity.isBaby())
                .stream()
                .min(Comparator.comparingDouble(entity -> entity.squaredDistanceTo(this.client.player)))
                .map(Entity::getBlockPos)
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

    /**
     * 歩いて離れる典型的なボイドトレードの手順を作る。座標は現在地（村人を見ていればその位置）で仮置きする。
     */
    void applyTemplate() {
        BlockPos here = currentPlayerPos();
        if (here == null) {
            here = BlockPos.ORIGIN;
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
                Text.translatable(KEY + "template.created"));
    }

    /**
     * 簡易ボイトレ（AST VT.ahk）と同じ手順を作る。
     * 左に少し歩いて（ゲートウェイで村人の所へ飛ぶ）→ 右クリックで開く → 待つ（元の場所へ戻され村人がアンロードされる）
     * → 取引 → 閉じる → 補充を待つ を繰り返す。
     * 村人は視線の先（なければ手の届く範囲で一番近い村人）。取引は ItemScroller があればお気に入りを
     * AsutanTweaks の MassTrade と同じく結果を捨てながら取引する。
     */
    void applySimplePreset() {
        BlockPos merchantPos = crosshairPos(true);
        if (merchantPos == null) {
            merchantPos = nearestMerchantPos();
        }
        boolean favorites = ItemScrollerCompat.isLoaded();
        Text doneNotice = Text.translatable(KEY + (merchantPos == null ? "simple.noVillager"
                : favorites ? "simple.createdFavorites" : "simple.created"));
        if (merchantPos == null) {
            merchantPos = currentPlayerPos() != null ? currentPlayerPos() : BlockPos.ORIGIN;
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

    /**
     * ワークフロー全体を差し替える。既存のステップがあれば確認を挟む。
     */
    private void replaceWorkflow(List<VoidTradeStep> newSteps, Text doneNotice) {
        if (this.steps.isEmpty()) {
            applyWorkflow(newSteps, doneNotice);
            return;
        }
        confirmThen(Text.translatable(KEY + "replace.title"), Text.translatable(KEY + "replace.message", this.steps.size()),
                () -> applyWorkflow(newSteps, doneNotice));
    }

    private void confirmThen(Text title, Text message, Runnable action) {
        this.client.setScreen(new ConfirmScreen(confirmed -> {
            if (confirmed) {
                action.run();
            }
            this.client.setScreen(this);
        }, title, message));
    }

    /**
     * 保存したプリセットを今のワークフローとして読み込む（プリセット画面から呼ぶ）。
     */
    void loadPreset(VoidTradePreset preset) {
        replaceWorkflow(VoidTradePreset.copySteps(preset.steps), Text.translatable(KEY + "presets.loaded", preset.name));
    }

    List<VoidTradeStep> getSteps() {
        return this.steps;
    }

    private void copyWorkflow() {
        if (this.steps.isEmpty()) {
            return;
        }
        this.client.keyboard.setClipboard(VoidTradeShare.export(this.steps));
        showNotice(Text.translatable(KEY + "copy.done", this.steps.size()));
    }

    private void pasteWorkflow() {
        List<VoidTradeStep> pasted = VoidTradeShare.parse(this.client.keyboard.getClipboard());
        if (pasted == null) {
            showNotice(Text.translatable(KEY + "paste.invalid"));
            return;
        }
        Text doneNotice = Text.translatable(KEY + "paste.done", pasted.size());
        List<String> commands = pasted.stream()
                .filter(step -> step.getType() == VoidTradeStepType.COMMAND)
                .map(step -> "/" + step.command.trim().replaceFirst("^/", ""))
                .collect(Collectors.toList());
        if (!commands.isEmpty()) {
            // 貼り付けたワークフローが知らないうちにコマンドを送らないよう、中身を見せて確認する
            confirmThen(Text.translatable(KEY + "paste.commandTitle"),
                    Text.translatable(KEY + "paste.commandMessage", String.join(", ", commands)),
                    () -> applyWorkflow(pasted, doneNotice));
            return;
        }
        replaceWorkflow(pasted, doneNotice);
    }

    void showNotice(Text text) {
        this.notice = text;
        this.noticeTicks = 200;
    }

    private void applyWorkflow(List<VoidTradeStep> newSteps, Text doneNotice) {
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
        this.client.setScreen(null);
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
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        this.renderBackground(matrices);
        fill(matrices, this.listLeft, TOP, this.listLeft + this.listWidth, this.height - 58, 0x80000000);
        this.stepList.render(matrices, mouseX, mouseY, delta);

        drawCenteredTextWithShadow(matrices, this.textRenderer, this.title, this.width / 2, 8, 0xFFFFFF);
        if (this.noticeTicks > 0 && this.notice != null) {
            drawCenteredTextWithShadow(matrices, this.textRenderer, this.notice, this.width / 2, 20, 0xFFFF55);
        } else {
            drawCenteredTextWithShadow(matrices, this.textRenderer, Text.translatable(KEY + "help"), this.width / 2, 20, 0xA0A0A0);
        }

        if (this.steps.isEmpty()) {
            int y = TOP + 8;
            for (OrderedText line : this.textRenderer.wrapLines(Text.translatable(KEY + "empty"), this.listWidth - 12)) {
                drawTextWithShadow(matrices, this.textRenderer, line, this.listLeft + 6, y, 0xA0A0A0);
                y += 10;
            }
        }

        for (Label label : this.labels) {
            drawTextWithShadow(matrices, this.textRenderer, label.text(), label.x(), label.y(), 0xE0E0E0);
        }

        VoidTradeStep step = getSelectedStep();
        if (step != null) {
            int y = this.descriptionY;
            Text description = Text.translatable(TYPE_KEY + step.getType().getId() + ".desc");
            for (OrderedText line : this.textRenderer.wrapLines(description, this.editorWidth)) {
                if (y + 9 > this.height - 32) {
                    break;
                }
                drawTextWithShadow(matrices, this.textRenderer, line, this.editorLeft, y, 0xA0A0A0);
                y += 10;
            }
        }

        super.render(matrices, mouseX, mouseY, delta);
    }

    @Override
    public void close() {
        this.client.setScreen(this.parent);
    }

    @Override
    public void removed() {
        this.feature.getConfig().save();
        super.removed();
    }

    class StepListWidget extends AlwaysSelectedEntryListWidget<StepEntry> {
        StepListWidget(MinecraftClient client, int width, int height, int top, int bottom, int itemHeight) {
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
        protected int getScrollbarPositionX() {
            return this.left + this.width - 6;
        }
    }

    class StepEntry extends AlwaysSelectedEntryListWidget.Entry<StepEntry> {
        private final int index;

        StepEntry(int index) {
            this.index = index;
        }

        @Override
        public void render(MatrixStack matrices, int index, int y, int x, int entryWidth, int entryHeight,
                           int mouseX, int mouseY, boolean hovered, float tickDelta) {
            if (this.index >= VoidTradeWorkflowScreen.this.steps.size()) {
                return;
            }
            boolean current = VoidTradeWorkflowScreen.this.feature.getCurrentStepIndex() == this.index;
            MutableText text = Text.literal((current ? "▶ " : "") + (this.index + 1) + ". ")
                    .append(VoidTradeWorkflowScreen.this.steps.get(this.index).describe());
            List<OrderedText> lines = VoidTradeWorkflowScreen.this.textRenderer.wrapLines(text, entryWidth - 4);
            if (!lines.isEmpty()) {
                drawTextWithShadow(matrices, VoidTradeWorkflowScreen.this.textRenderer, lines.get(0), x + 2, y + 2,
                        current ? 0xFFFF55 : 0xFFFFFF);
            }
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            return button == 0;
        }

        @Override
        public Text getNarration() {
            return this.index < VoidTradeWorkflowScreen.this.steps.size()
                    ? VoidTradeWorkflowScreen.this.steps.get(this.index).describe()
                    : Text.empty();
        }
    }
}

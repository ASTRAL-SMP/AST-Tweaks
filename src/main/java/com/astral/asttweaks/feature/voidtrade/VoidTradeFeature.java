//? if <26.1 {
/*package com.astral.asttweaks.feature.voidtrade;

import com.astral.asttweaks.ASTTweaks;
import com.astral.asttweaks.compat.ItemScrollerCompat;
import com.astral.asttweaks.config.ModConfig;
import com.astral.asttweaks.feature.Feature;
import com.astral.asttweaks.feature.automove.MoveDirection;
import com.astral.asttweaks.mixin.MerchantScreenAccessor;
import com.astral.asttweaks.mixin.MerchantScreenHandlerAccessor;
import com.astral.asttweaks.util.McClient;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ServerboundSelectTradePacket;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/^*
 * Void Trade（ボイドトレード）自動化。
 *
 * 1.19.4 のサーバーは取引相手が離れても取引画面を閉じない（MerchantScreenHandler#canUse は
 * customer の一致しか見ず、FollowCustomerTask も customer を外さない）。そのため取引画面を
 * 開いたまま村人のチャンクがアンロードされる距離まで離れると、以後の取引は保存済みで破棄された
 * 村人の実体に対して行われ、取引回数や需要による値上がりが村人に残らない。
 *
 * 座標付きのステップ列（移動 → 取引を開く → 離れる → アンロード待ち → 取引 → 閉じる → 戻る …）を
 * 1 tick に 1 ステップずつ進め、末尾まで来たら先頭に戻ってループする。
 ^/
public class VoidTradeFeature implements Feature {
    private static final double ARRIVE_DISTANCE = 0.35;
    private static final double SPRINT_STOP_DISTANCE = 1.5;
    private static final double TELEPORT_DISTANCE_SQUARED = 8.0 * 8.0;   // 1 tick でこれ以上動いたらテレポートとみなす
    private static final double MAX_REACH_SQUARED = 36.0;       // サーバー側の MAX_BREAK_SQUARED_DISTANCE と同じ
    private static final int INTERACT_RETRY_TICKS = 5;           // 右クリック連打相当。開いた後の余分なクリックはサーバーが無視する
    private static final int OUT_OF_REACH_GRACE_TICKS = 40;
    private static final int TRADE_IDLE_LIMIT = 5;
    private static final int MAX_THROWS_PER_TICK = 16;
    private static final int MIN_LOOP_TICKS = 20;               // コマンドだけのワークフロー等で毎 tick ループしないように
    private static final int STATUS_REFRESH_TICKS = 20;
    private static final int OUTPUT_SLOT = 2;

    private enum StepResult { CONTINUE, DONE, FAILED }

    private final VoidTradeConfig config;
    private boolean wasToggleKeyDown = false;

    private boolean running = false;
    private int stepIndex;
    private int loopsCompleted;
    private int loopTicks;
    private int cooldownTicks;
    private boolean stepStarted;
    private int stepTicks;
    private String waitReason;          // タイムアウト時に表示する理由（翻訳キー末尾）

    // 直前に取引を開いた村人（アンロード検知用）
    private int merchantEntityId = -1;
    private ClientLevel merchantWorld;
    private int lastInteractTick;

    // 移動制御（VoidTradeInputMixin から参照）
    private Vec3 moveTarget;
    private boolean moveSprint;
    private boolean moveKeepFacing;
    private Vec3 lastTickPos;              // テレポート検知用（前 tick の位置）
    private ClientLevel lastTickWorld;
    private MoveDirection nudgeDirection;   // 「少し歩く」ステップ中の方向（null なら無効）

    // 取引ステップの状態
    private List<MerchantOffer> tradeQueue;    // このステップで取引する取引（お気に入りなら複数）
    private int tradeQueuePos;
    private boolean offerSelected;
    private int tradedCount;
    private int tradeIdleTicks;
    private boolean inventoryFull;
    private String firstFinishReason;
    private int currentOfferTraded;
    private final List<MutableComponent> skippedOffers = new ArrayList<>();   // 1 回も取引できなかったお気に入り
    private final Set<Integer> warnedSkipSteps = new HashSet<>();         // 取引できなかったお気に入りを知らせたステップ
    // 取引ステップ・取引ごとの、最初に画面を開いたときの取引回数（ボイド不成立の検出用）
    private final Map<String, Integer> baselineUses = new HashMap<>();

    public VoidTradeFeature() {
        this.config = new VoidTradeConfig();
    }

    @Override
    public String getId() {
        return "voidtrade";
    }

    @Override
    public String getName() {
        return "Void Trade";
    }

    @Override
    public void init() {
        // 無効化されたときに実行中のワークフローを止める必要があるため、自前で毎 tick 呼ぶ
        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
        ASTTweaks.LOGGER.info("VoidTrade feature initialized");
    }

    @Override
    public void tick() {
        // tick処理はClientTickEventsで行う
    }

    @Override
    public boolean isEnabled() {
        return config.isEnabled();
    }

    @Override
    public void setEnabled(boolean enabled) {
        config.setEnabled(enabled);
        if (!enabled) {
            stop();
        }
    }

    public VoidTradeConfig getConfig() {
        return config;
    }

    public boolean isRunning() {
        return running;
    }

    /^*
     * 実行中のステップ番号（0 始まり）。停止中は -1。
     ^/
    public int getCurrentStepIndex() {
        return running ? stepIndex : -1;
    }

    public void toggle() {
        if (running) {
            stop();
        } else {
            start();
        }
    }

    public void start() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || running) {
            return;
        }
        if (!isEnabled()) {
            client.player.displayClientMessage(Component.translatable("message." + ASTTweaks.MOD_ID + ".voidtrade.disabled"), true);
            return;
        }
        if (config.getSteps().isEmpty()) {
            client.player.displayClientMessage(Component.translatable("message." + ASTTweaks.MOD_ID + ".voidtrade.empty"), true);
            return;
        }

        running = true;
        stepIndex = 0;
        loopsCompleted = 0;
        loopTicks = 0;
        cooldownTicks = 0;
        stepStarted = false;
        merchantEntityId = -1;
        merchantWorld = null;
        baselineUses.clear();
        warnedSkipSteps.clear();
        client.player.displayClientMessage(Component.translatable("message." + ASTTweaks.MOD_ID + ".voidtrade.started"), true);
        ASTTweaks.LOGGER.info("VoidTrade: started ({} steps)", config.getSteps().size());
    }

    public void stop() {
        if (!running) {
            return;
        }
        reset();
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            client.player.displayClientMessage(Component.translatable("message." + ASTTweaks.MOD_ID + ".voidtrade.stopped", loopsCompleted), true);
        }
        ASTTweaks.LOGGER.info("VoidTrade: stopped after {} loops", loopsCompleted);
    }

    private void reset() {
        running = false;
        stepStarted = false;
        moveTarget = null;
        nudgeDirection = null;
    }

    private void onClientTick(Minecraft client) {
        if (client.player == null || client.level == null) {
            // 切断時は黙って止める
            reset();
            wasToggleKeyDown = false;
            return;
        }

        if (!isEnabled()) {
            stop();
            wasToggleKeyDown = false;
            return;
        }

        // 取引画面を開いたまま動かすので、画面表示中もキーを拾う（テキスト入力のある画面は除外）
        boolean keyDown = ModConfig.getInstance().voidTradeToggleKey.isPressed();
        if (keyDown && !wasToggleKeyDown
                && (McClient.screen() == null || McClient.screen() instanceof MerchantScreen)) {
            toggle();
        }
        wasToggleKeyDown = keyDown;

        // シングルプレイのポーズ中はワールドが止まるので、待機やタイムアウトの tick も進めない
        if (running && !client.isPaused()) {
            runTick(client);
        }
    }

    private void runTick(Minecraft client) {
        List<VoidTradeStep> steps = config.getSteps();
        if (steps.isEmpty()) {
            stop();
            return;
        }
        if (client.player.isDeadOrDying()) {
            failWith(Component.translatable("message." + ASTTweaks.MOD_ID + ".voidtrade.reason.dead"));
            return;
        }
        if (cooldownTicks > 0) {
            cooldownTicks--;
            return;
        }
        if (stepIndex >= steps.size()) {
            // 実行中にステップが削除された
            completeLoop();
            return;
        }

        VoidTradeStep step = steps.get(stepIndex);
        if (!stepStarted && step.firstLoopOnly && loopsCompleted > 0) {
            // 「初回のみ」のステップは 2 周目以降は飛ばす
            stepIndex++;
            if (stepIndex >= steps.size()) {
                completeLoop();
            }
            return;
        }
        if (!stepStarted) {
            beginStep(client, step);
        }
        stepTicks++;
        loopTicks++;

        StepResult result = executeStep(client, step);
        if (result == StepResult.FAILED || !running) {
            return;
        }
        if (result == StepResult.DONE) {
            endStep(client, step);
            stepIndex++;
            if (stepIndex >= steps.size()) {
                completeLoop();
            }
            return;
        }

        if (step.getType().hasTimeout() && stepTicks >= config.getStepTimeoutTicks()) {
            fail(waitReason != null ? waitReason : "timeout");
            return;
        }
        if (stepTicks % STATUS_REFRESH_TICKS == 0) {
            showStatus(client, step);
        }
    }

    private void beginStep(Minecraft client, VoidTradeStep step) {
        stepStarted = true;
        stepTicks = 0;
        waitReason = null;
        moveTarget = null;
        lastTickPos = client.player.position();
        lastTickWorld = client.level;
        nudgeDirection = null;
        lastInteractTick = -1;
        tradeQueue = null;
        tradeQueuePos = 0;
        offerSelected = false;
        tradedCount = 0;
        tradeIdleTicks = 0;
        inventoryFull = false;
        firstFinishReason = null;
        currentOfferTraded = 0;
        skippedOffers.clear();
        showStatus(client, step);
    }

    private void endStep(Minecraft client, VoidTradeStep step) {
        stepStarted = false;
        moveTarget = null;
        nudgeDirection = null;
        if (step.getType() == VoidTradeStepType.TRADE && inventoryFull && client.player != null) {
            client.player.displayClientMessage(
                    Component.translatable("message." + ASTTweaks.MOD_ID + ".voidtrade.inventoryFull", tradedCount)
                            .withStyle(ChatFormatting.YELLOW),
                    false);
        }
    }

    private void completeLoop() {
        loopsCompleted++;
        stepIndex = 0;
        stepStarted = false;

        int limit = config.getLoopCount();
        if (limit > 0 && loopsCompleted >= limit) {
            reset();
            Minecraft client = Minecraft.getInstance();
            if (client.player != null) {
                client.player.displayClientMessage(
                        Component.translatable("message." + ASTTweaks.MOD_ID + ".voidtrade.completed", loopsCompleted)
                                .withStyle(ChatFormatting.GREEN),
                        false);
            }
            ASTTweaks.LOGGER.info("VoidTrade: completed {} loops", loopsCompleted);
            return;
        }

        if (loopTicks < MIN_LOOP_TICKS) {
            cooldownTicks = MIN_LOOP_TICKS - loopTicks;
        }
        loopTicks = 0;
    }

    private StepResult fail(String reasonKey) {
        failWith(Component.translatable("message." + ASTTweaks.MOD_ID + ".voidtrade.reason." + reasonKey));
        return StepResult.FAILED;
    }

    private void failWith(Component reason) {
        Minecraft client = Minecraft.getInstance();
        List<VoidTradeStep> steps = config.getSteps();
        Component stepText = stepIndex < steps.size() ? steps.get(stepIndex).describe() : Component.literal("-");
        if (client.player != null) {
            client.player.displayClientMessage(
                    Component.translatable("message." + ASTTweaks.MOD_ID + ".voidtrade.failed", stepIndex + 1, stepText, reason)
                            .withStyle(ChatFormatting.RED),
                    false);
        }
        ASTTweaks.LOGGER.warn("VoidTrade: stopped at step {} ({}): {}", stepIndex + 1, stepText.getString(), reason.getString());
        reset();
    }

    private void showStatus(Minecraft client, VoidTradeStep step) {
        if (client.player == null) {
            return;
        }
        int limit = config.getLoopCount();
        String loop = (loopsCompleted + 1) + "/" + (limit > 0 ? String.valueOf(limit) : "∞");
        MutableComponent detail = step.describe();
        if (moveTarget != null) {
            double distance = Math.sqrt(squaredHorizontalDistance(client.player, moveTarget));
            detail.append(Component.translatable("message." + ASTTweaks.MOD_ID + ".voidtrade.progress.distance",
                    String.format("%.1f", distance)));
        } else if (step.getType() == VoidTradeStepType.TRADE && tradedCount > 0) {
            detail.append(Component.translatable("message." + ASTTweaks.MOD_ID + ".voidtrade.progress.trades", tradedCount));
        }
        client.player.displayClientMessage(
                Component.translatable("message." + ASTTweaks.MOD_ID + ".voidtrade.status",
                        loop, stepIndex + 1, config.getSteps().size(), detail),
                true);
    }

    private StepResult executeStep(Minecraft client, VoidTradeStep step) {
        return switch (step.getType()) {
            case MOVE_TO -> tickMoveTo(client, step);
            case NUDGE -> tickNudge(client, step);
            case INTERACT_ENTITY -> tickInteractEntity(client, step);
            case INTERACT_BLOCK -> tickInteractBlock(client, step);
            case WAIT_UNLOAD -> tickWaitUnload(client);
            case TRADE -> tickTrade(client, step);
            case CLOSE_SCREEN -> tickCloseScreen(client);
            case WAIT -> stepTicks >= Math.max(1, step.ticks) + config.getExtraWaitTicks() ? StepResult.DONE : StepResult.CONTINUE;
            case COMMAND -> tickCommand(client, step);
        };
    }

    // ------------------------------------------------------------
    // 移動
    // ------------------------------------------------------------

    private StepResult tickMoveTo(Minecraft client, VoidTradeStep step) {
        if (teleportedSinceLastTick(client)) {
            // ゲートウェイやポータルに入って飛ばされた = 目的地（の入口）に着いた。飛ばされた先で歩き続けない
            moveTarget = null;
            return StepResult.DONE;
        }

        Vec3 target = new Vec3(step.x + 0.5, step.y, step.z + 0.5);
        double distanceSq = squaredHorizontalDistance(client.player, target);
        if (distanceSq <= ARRIVE_DISTANCE * ARRIVE_DISTANCE) {
            moveTarget = null;
            return StepResult.DONE;
        }

        moveTarget = target;
        moveKeepFacing = step.keepFacing;
        moveSprint = step.sprint && !step.keepFacing && distanceSq > SPRINT_STOP_DISTANCE * SPRINT_STOP_DISTANCE;
        waitReason = "moveTimeout";
        return StepResult.CONTINUE;
    }

    /^*
     * 移動キーを数 tick だけ押したのと同じ動き（AHK の {a down}{a up} 相当）。向きは変えない。
     ^/
    private StepResult tickNudge(Minecraft client, VoidTradeStep step) {
        if (stepTicks > Math.max(1, step.nudgeTicks) || teleportedSinceLastTick(client)) {
            nudgeDirection = null;
            return StepResult.DONE;
        }
        nudgeDirection = step.getNudgeDirection();
        return StepResult.CONTINUE;
    }

    /^*
     * VoidTradeInputMixin から毎 tick（Input#tick 直後）呼ばれる。
     * 移動ステップ中なら向きを目的地に合わせて前進入力で上書きする。取引画面を開いたままでも効く。
     ^/
    public void applyMovementInput(LocalPlayer player, Input input) {
        if (!running || player != Minecraft.getInstance().player) {
            return;
        }
        if (nudgeDirection != null) {
            int forward = nudgeDirection.getForwardAxis();
            int sideways = nudgeDirection.getSidewaysAxis();
            input.up = forward > 0;
            input.down = forward < 0;
            input.left = sideways > 0;
            input.right = sideways < 0;
            input.forwardImpulse = forward;
            input.leftImpulse = sideways;
            input.shiftKeyDown = false;
            return;
        }
        if (moveTarget == null) {
            return;
        }

        double dx = moveTarget.x - player.getX();
        double dz = moveTarget.z - player.getZ();
        if (moveKeepFacing) {
            // 向きはそのまま、目的地の方向を前後・左右の入力に分解する（Entity#movementInputToVelocity の逆変換）
            double yaw = Math.toRadians(player.getYRot());
            double sin = Math.sin(yaw);
            double cos = Math.cos(yaw);
            double forward = -dx * sin + dz * cos;
            double sideways = dx * cos + dz * sin;
            double length = Math.sqrt(forward * forward + sideways * sideways);
            if (length > 1.0E-4) {
                forward /= length;
                sideways /= length;
            }
            input.up = forward > 0.3;
            input.down = forward < -0.3;
            input.left = sideways > 0.3;
            input.right = sideways < -0.3;
            input.forwardImpulse = (float) forward;
            input.leftImpulse = (float) sideways;
            input.shiftKeyDown = false;
            input.jumping = player.horizontalCollision && player.onGround();
            player.setSprinting(false);
            return;
        }
        float targetYaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0f;
        player.setYRot(player.getYRot() + Mth.wrapDegrees(targetYaw - player.getYRot()));

        input.up = true;
        input.down = false;
        input.left = false;
        input.right = false;
        input.forwardImpulse = 1.0f;
        input.leftImpulse = 0.0f;
        input.shiftKeyDown = false;
        // 1 ブロックの段差は跳んで越える
        input.jumping = player.horizontalCollision && player.onGround();
        player.setSprinting(moveSprint);
    }

    /^*
     * 前 tick から大きく位置が飛んだ（ワールドが変わった）か。移動系ステップ中に毎 tick 呼び、基準位置を更新する。
     ^/
    private boolean teleportedSinceLastTick(Minecraft client) {
        Vec3 pos = client.player.position();
        boolean teleported = client.level != lastTickWorld
                || (lastTickPos != null && lastTickPos.distanceToSqr(pos) > TELEPORT_DISTANCE_SQUARED);
        lastTickPos = pos;
        lastTickWorld = client.level;
        return teleported;
    }

    private static double squaredHorizontalDistance(LocalPlayer player, Vec3 target) {
        double dx = target.x - player.getX();
        double dz = target.z - player.getZ();
        return dx * dx + dz * dz;
    }

    // ------------------------------------------------------------
    // 村人・ブロックへの右クリック
    // ------------------------------------------------------------

    private StepResult tickInteractEntity(Minecraft client, VoidTradeStep step) {
        LocalPlayer player = client.player;
        if (player.containerMenu instanceof MerchantMenu) {
            return StepResult.DONE;
        }

        AbstractVillager merchant = findMerchant(client.level, step);
        if (merchant == null) {
            waitReason = "merchantNotFound";
            return StepResult.CONTINUE;
        }
        if (merchant.getBoundingBox().distanceToSqr(player.getEyePosition()) >= MAX_REACH_SQUARED) {
            waitReason = "merchantTooFar";
            return stepTicks > OUT_OF_REACH_GRACE_TICKS ? fail("merchantTooFar") : StepResult.CONTINUE;
        }

        Vec3 aim = merchant.getBoundingBox().getCenter();
        lookAt(player, aim);
        waitReason = "screenNotOpened";

        // 先に視線（回転）をサーバーへ送ってから次 tick 以降に右クリックする。開かなければ一定間隔で再試行
        if (stepTicks < 2 || (lastInteractTick >= 0 && stepTicks - lastInteractTick < INTERACT_RETRY_TICKS)) {
            return StepResult.CONTINUE;
        }
        lastInteractTick = stepTicks;
        merchantEntityId = merchant.getId();
        merchantWorld = client.level;
        // ItemScroller は手で右クリックした村人しか記録しないので、お気に入りと一覧の並びをこの村人に合わせる
        ItemScrollerCompat.setLastInteractedVillager(merchant.getUUID());

        // バニラの右クリック（MinecraftClient#doItemUse）と同じ順で送る
        InteractionResult result = client.gameMode.interactAt(
                player, merchant, new EntityHitResult(merchant, aim), InteractionHand.MAIN_HAND);
        if (!result.consumesAction()) {
            result = client.gameMode.interact(player, merchant, InteractionHand.MAIN_HAND);
        }
        if (result.consumesAction() && result.shouldSwing()) {
            player.swing(InteractionHand.MAIN_HAND);
        }
        return StepResult.CONTINUE;
    }

    private static AbstractVillager findMerchant(ClientLevel world, VoidTradeStep step) {
        Vec3 center = new Vec3(step.x + 0.5, step.y, step.z + 0.5);
        AABB area = new AABB(center, center).inflate(Math.max(0.5, step.radius));

        AbstractVillager nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (AbstractVillager merchant : world.getEntitiesOfClass(AbstractVillager.class, area,
                entity -> entity.isAlive() && !entity.isBaby())) {
            double distance = merchant.distanceToSqr(center);
            if (distance < nearestDistance) {
                nearest = merchant;
                nearestDistance = distance;
            }
        }
        return nearest;
    }

    private StepResult tickInteractBlock(Minecraft client, VoidTradeStep step) {
        LocalPlayer player = client.player;
        BlockPos pos = new BlockPos(step.x, step.y, step.z);
        Vec3 center = Vec3.atCenterOf(pos);
        Vec3 eye = player.getEyePosition();
        if (eye.distanceToSqr(center) > MAX_REACH_SQUARED) {
            return fail("blockTooFar");
        }

        // プレイヤー側を向いた面の中央を狙う
        Direction side = Direction.getNearest(eye.x - center.x, eye.y - center.y, eye.z - center.z);
        Vec3 hitPos = center.add(side.getStepX() * 0.5, side.getStepY() * 0.5, side.getStepZ() * 0.5);
        lookAt(player, hitPos);
        if (stepTicks < 2) {
            return StepResult.CONTINUE;
        }

        InteractionResult result = client.gameMode.useItemOn(
                player, InteractionHand.MAIN_HAND, new BlockHitResult(hitPos, side, pos, false));
        if (result.consumesAction() && result.shouldSwing()) {
            player.swing(InteractionHand.MAIN_HAND);
        }
        return StepResult.DONE;
    }

    private static void lookAt(LocalPlayer player, Vec3 target) {
        Vec3 eye = player.getEyePosition();
        double dx = target.x - eye.x;
        double dy = target.y - eye.y;
        double dz = target.z - eye.z;
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0f;
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, horizontal));
        player.setYRot(player.getYRot() + Mth.wrapDegrees(yaw - player.getYRot()));
        player.setXRot(Mth.clamp(pitch, -90.0f, 90.0f));
    }

    // ------------------------------------------------------------
    // アンロード待ち・取引・画面操作
    // ------------------------------------------------------------

    private StepResult tickWaitUnload(Minecraft client) {
        if (!(client.player.containerMenu instanceof MerchantMenu)) {
            return fail("screenClosed");
        }
        if (merchantEntityId < 0 || merchantWorld == null) {
            return fail("noMerchant");
        }

        waitReason = "notUnloaded";
        if (client.level != merchantWorld) {
            return StepResult.DONE;
        }
        // サーバーが村人の追跡をやめる（= 遠ざかった／アンロードされた）とクライアントからも消える
        Entity merchant = client.level.getEntity(merchantEntityId);
        return merchant == null || merchant.isRemoved() ? StepResult.DONE : StepResult.CONTINUE;
    }

    private StepResult tickTrade(Minecraft client, VoidTradeStep step) {
        LocalPlayer player = client.player;
        if (!(player.containerMenu instanceof MerchantMenu handler)) {
            return fail("screenClosed");
        }
        if (handler.getOffers().isEmpty()) {
            waitReason = "offersNotReceived";
            return StepResult.CONTINUE;
        }

        if (tradeQueue == null) {
            tradeQueue = resolveTradeTargets(player, handler, step);
            if (tradeQueue == null) {
                return StepResult.FAILED;
            }
            // 前の周の取引が村人に残っている = アンロードされないまま取引していた。
            // 気づかずに回し続けると村人がロック・値上がりするので止める
            for (MerchantOffer offer : tradeQueue) {
                String key = stepIndex + ":" + originalOffers(handler).indexOf(offer);
                Integer baseline = baselineUses.putIfAbsent(key, offer.getUses());
                if (baseline != null && offer.getUses() > baseline) {
                    return fail("notVoided");
                }
            }
        }
        if (tradeQueuePos >= tradeQueue.size()) {
            // 1 回も取引できなかった場合は、次の周も空回りするだけなので理由を出して止める
            if (tradedCount > 0) {
                warnSkippedOffers(player);
                return StepResult.DONE;
            }
            StepResult result = fail(firstFinishReason != null ? firstFinishReason : "offerLocked");
            if ("noMaterials".equals(firstFinishReason)) {
                sendOfferList(player, handler);
            }
            return result;
        }

        MerchantOffer offer = tradeQueue.get(tradeQueuePos);
        if (offer.isOutOfStock()) {
            return nextOffer("offerLocked");
        }

        // 最初は必ず選び直す（前の取引の材料が残っていると別の取引が出力されるため）。
        // 以降は出力が空になったら選び直して、サーバー側でインベントリから材料を補充させる
        if (!offerSelected || handler.getSlot(OUTPUT_SLOT).getItem().isEmpty()) {
            selectOffer(client, handler, offer);
            offerSelected = true;
        }
        if (handler.getSlot(OUTPUT_SLOT).getItem().isEmpty()) {
            // 選んでも出力が出ない = 材料切れ
            return ++tradeIdleTicks >= TRADE_IDLE_LIMIT ? nextOffer("noMaterials") : StepResult.CONTINUE;
        }

        int usesBefore = offer.getUses();
        if (step.dropOutput) {
            for (int i = 0; i < MAX_THROWS_PER_TICK
                    && !offer.isOutOfStock()
                    && !handler.getSlot(OUTPUT_SLOT).getItem().isEmpty(); i++) {
                client.gameMode.handleContainerInput(handler.containerId, OUTPUT_SLOT, 1, ContainerInput.THROW, player);
            }
        } else {
            // シフトクリックは入っている材料の分だけ連続で取引される
            client.gameMode.handleContainerInput(handler.containerId, OUTPUT_SLOT, 0, ContainerInput.QUICK_MOVE, player);
        }

        int traded = offer.getUses() - usesBefore;
        if (traded > 0) {
            tradedCount += traded;
            currentOfferTraded += traded;
            tradeIdleTicks = 0;
            return StepResult.CONTINUE;
        }
        // 出力があるのに取引が進まない = インベントリに空きがない
        inventoryFull = true;
        return ++tradeIdleTicks >= TRADE_IDLE_LIMIT ? nextOffer("inventoryFull") : StepResult.CONTINUE;
    }

    /^*
     * 今の取引を終えて次の取引へ進む。
     ^/
    private StepResult nextOffer(String finishReason) {
        if (firstFinishReason == null) {
            firstFinishReason = finishReason;
        }
        if (currentOfferTraded == 0 && tradeQueue.size() > 1) {
            skippedOffers.add(describeOffer(tradeQueue.get(tradeQueuePos))
                    .append(Component.translatable("message." + ASTTweaks.MOD_ID + ".voidtrade.skipped." + finishReason)));
        }
        currentOfferTraded = 0;
        tradeQueuePos++;
        offerSelected = false;
        tradeIdleTicks = 0;
        return StepResult.CONTINUE;
    }

    /^*
     * お気に入りのうち 1 回も取引できなかったものを知らせる。毎周出るとうるさいので、ステップごとに 1 回だけ。
     ^/
    private void warnSkippedOffers(LocalPlayer player) {
        if (skippedOffers.isEmpty() || !warnedSkipSteps.add(stepIndex)) {
            return;
        }
        MutableComponent list = Component.empty();
        for (int i = 0; i < skippedOffers.size(); i++) {
            if (i > 0) {
                list.append(", ");
            }
            list.append(skippedOffers.get(i));
        }
        player.displayClientMessage(Component.translatable("message." + ASTTweaks.MOD_ID + ".voidtrade.skippedFavorites",
                skippedOffers.size(), tradeQueue.size(), list).withStyle(ChatFormatting.YELLOW), false);
    }

    /^*
     * 取引する取引を決める。取引番号は画面に表示されている並び（ItemScroller が並べ替えていればその並び）で数える。
     * 決められなければ理由を出して止め、null を返す。
     ^/
    private List<MerchantOffer> resolveTradeTargets(LocalPlayer player, MerchantMenu handler, VoidTradeStep step) {
        List<MerchantOffer> targets = new ArrayList<>();
        if (step.useFavorites) {
            MerchantOffers original = originalOffers(handler);
            for (int index : ItemScrollerCompat.getFavoriteTradeIndices(handler)) {
                if (index >= 0 && index < original.size()) {
                    targets.add(original.get(index));
                }
            }
            if (targets.isEmpty()) {
                fail(ItemScrollerCompat.isLoaded() ? "noFavorites" : "noItemScroller");
                sendOfferList(player, handler);
                return null;
            }
            return targets;
        }

        MerchantOffers offers = handler.getOffers();
        int index = step.offerIndex - 1;
        if (index < 0 || index >= offers.size()) {
            fail("offerOutOfRange");
            sendOfferList(player, handler);
            return null;
        }
        targets.add(offers.get(index));
        return targets;
    }

    /^*
     * サーバーと同じ並びの取引一覧。ItemScroller 等が getRecipes() を並べ替えた一覧に差し替えていても変わらない。
     ^/
    private static MerchantOffers originalOffers(MerchantMenu handler) {
        return ((MerchantScreenHandlerAccessor) handler).getMerchant().getOffers();
    }

    /^*
     * MerchantScreen で取引を選んだときと同じ処理（MerchantScreen#syncRecipeIndex）。
     * 表示上の並びとサーバー側の並びが違う場合（ItemScroller のお気に入りの並べ替え）に備えて番号を変換する。
     * 材料の自動補充は表示側の一覧、サーバーへ送る番号と MerchantInventory の番号は元の一覧で数える。
     ^/
    private static void selectOffer(Minecraft client, MerchantMenu handler, MerchantOffer offer) {
        int visibleIndex = handler.getOffers().indexOf(offer);
        int realIndex = originalOffers(handler).indexOf(offer);
        if (visibleIndex < 0 || realIndex < 0) {
            return;
        }
        handler.setSelectionHint(realIndex);
        handler.tryMoveItems(visibleIndex);
        client.getConnection().send(new ServerboundSelectTradePacket(realIndex));
        if (McClient.screen() instanceof MerchantScreen screen) {
            ((MerchantScreenAccessor) screen).setSelectedIndex(visibleIndex);
        }
    }

    /^*
     * 取引番号を決めやすいよう、表示順の取引一覧（必要な材料 → 結果）をチャットに出す。
     ^/
    private static void sendOfferList(LocalPlayer player, MerchantMenu handler) {
        MerchantOffers offers = handler.getOffers();
        player.displayClientMessage(Component.translatable("message." + ASTTweaks.MOD_ID + ".voidtrade.offerList").withStyle(ChatFormatting.GRAY), false);
        for (int i = 0; i < offers.size(); i++) {
            MerchantOffer offer = offers.get(i);
            MutableComponent line = Component.literal(" " + (i + 1) + ": ").append(describeOffer(offer));
            if (offer.isOutOfStock()) {
                line.append(Component.translatable("message." + ASTTweaks.MOD_ID + ".voidtrade.offerList.locked"));
            }
            player.displayClientMessage(line.withStyle(ChatFormatting.GRAY), false);
        }
    }

    /^*
     * 「材料 → 結果」の 1 行表記。
     ^/
    private static MutableComponent describeOffer(MerchantOffer offer) {
        MutableComponent text = describeStack(offer.getCostA());
        if (!offer.getCostB().isEmpty()) {
            text.append(" + ").append(describeStack(offer.getCostB()));
        }
        return text.append(" → ").append(describeStack(offer.getResult()));
    }

    private static MutableComponent describeStack(ItemStack stack) {
        return Component.empty().append(stack.getHoverName()).append(" x" + stack.getCount());
    }

    private StepResult tickCloseScreen(Minecraft client) {
        LocalPlayer player = client.player;
        if (player.containerMenu != player.inventoryMenu) {
            player.closeContainer();
        }
        return StepResult.DONE;
    }

    private StepResult tickCommand(Minecraft client, VoidTradeStep step) {
        String command = step.command != null ? step.command.trim() : "";
        if (command.startsWith("/")) {
            command = command.substring(1);
        }
        if (!command.isEmpty()) {
            client.player.connection.sendCommand(command);
        }
        return StepResult.DONE;
    }
}
*///?}

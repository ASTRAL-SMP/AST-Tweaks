package com.astral.asttweaks.feature.voidtrade;

import com.astral.asttweaks.ASTTweaks;
import com.astral.asttweaks.compat.ItemScrollerCompat;
import com.astral.asttweaks.config.ModConfig;
import com.astral.asttweaks.feature.Feature;
import com.astral.asttweaks.feature.automove.MoveDirection;
import com.astral.asttweaks.mixin.MerchantScreenAccessor;
import com.astral.asttweaks.mixin.MerchantScreenHandlerAccessor;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.MerchantScreen;
import net.minecraft.client.input.Input;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.MerchantEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.SelectMerchantTradeC2SPacket;
import net.minecraft.screen.MerchantScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOfferList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Void Trade（ボイドトレード）自動化。
 *
 * 1.19.4 のサーバーは取引相手が離れても取引画面を閉じない（MerchantScreenHandler#canUse は
 * customer の一致しか見ず、FollowCustomerTask も customer を外さない）。そのため取引画面を
 * 開いたまま村人のチャンクがアンロードされる距離まで離れると、以後の取引は保存済みで破棄された
 * 村人の実体に対して行われ、取引回数や需要による値上がりが村人に残らない。
 *
 * 座標付きのステップ列（移動 → 取引を開く → 離れる → アンロード待ち → 取引 → 閉じる → 戻る …）を
 * 1 tick に 1 ステップずつ進め、末尾まで来たら先頭に戻ってループする。
 */
public class VoidTradeFeature implements Feature {
    private static final double ARRIVE_DISTANCE = 0.35;
    private static final double SPRINT_STOP_DISTANCE = 1.5;
    private static final double TELEPORT_DISTANCE_SQUARED = 8.0 * 8.0;   // 1 tick でこれ以上動いたらテレポートとみなす
    private static final double MAX_REACH_SQUARED = 36.0;       // サーバー側の MAX_BREAK_SQUARED_DISTANCE と同じ
    private static final int INTERACT_RETRY_TICKS = 5;           // 右クリック連打相当。開いた後の余分なクリックはサーバーが無視する
    private static final int OUT_OF_REACH_GRACE_TICKS = 40;
    private static final int TRADE_IDLE_LIMIT = 20;            // 出力が出ないまま諦めるまでの tick 数（サーバーの応答待ちを含む）
    private static final int TRADE_RESELECT_TICKS = 5;         // 出力が出ないときに取引を選び直す間隔
    private static final int MIN_LOOP_TICKS = 20;               // コマンドだけのワークフロー等で毎 tick ループしないように
    private static final int STATUS_REFRESH_TICKS = 20;
    private static final int OUTPUT_SLOT = 2;

    private enum StepResult { CONTINUE, DONE, FAILED }

    /**
     * 取引ステップで取引する 1 件。TradeOffer の実体ではなく、サーバーと同じ並び（元の一覧）での番号で持つ。
     * 画面を開いている間に取引一覧が差し替えられても（ItemScroller の並べ替えの作り直しやサーバーからの再送）
     * 同じ取引を追い続けられるよう、TradeOffer は毎 tick 元の一覧から引き直す。
     */
    private record TradeTarget(int index, MutableText description) {}

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
    private ClientWorld merchantWorld;
    private int lastInteractTick;

    // 移動制御（VoidTradeInputMixin から参照）
    private Vec3d moveTarget;
    private boolean moveSprint;
    private boolean moveKeepFacing;
    private Vec3d lastTickPos;              // テレポート検知用（前 tick の位置）
    private ClientWorld lastTickWorld;
    private MoveDirection nudgeDirection;   // 「少し歩く」ステップ中の方向（null なら無効）

    // 取引ステップの状態
    private List<TradeTarget> tradeQueue;   // このステップで取引する取引（お気に入りなら複数）
    private int tradeQueuePos;
    private boolean offerSelected;
    private int tradedCount;
    private int tradeIdleTicks;
    private boolean inventoryFull;
    private String firstFinishReason;
    private int currentOfferTraded;
    private final List<MutableText> skippedOffers = new ArrayList<>();   // 1 回も取引できなかったお気に入り
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

    /**
     * 実行中のステップ番号（0 始まり）。停止中は -1。
     */
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
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || running) {
            return;
        }
        if (!isEnabled()) {
            client.player.sendMessage(Text.translatable("message." + ASTTweaks.MOD_ID + ".voidtrade.disabled"), true);
            return;
        }
        if (config.getSteps().isEmpty()) {
            client.player.sendMessage(Text.translatable("message." + ASTTweaks.MOD_ID + ".voidtrade.empty"), true);
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
        client.player.sendMessage(Text.translatable("message." + ASTTweaks.MOD_ID + ".voidtrade.started"), true);
        ASTTweaks.LOGGER.info("VoidTrade: started ({} steps)", config.getSteps().size());
    }

    public void stop() {
        if (!running) {
            return;
        }
        reset();
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            client.player.sendMessage(Text.translatable("message." + ASTTweaks.MOD_ID + ".voidtrade.stopped", loopsCompleted), true);
        }
        ASTTweaks.LOGGER.info("VoidTrade: stopped after {} loops", loopsCompleted);
    }

    private void reset() {
        running = false;
        stepStarted = false;
        moveTarget = null;
        nudgeDirection = null;
    }

    private void onClientTick(MinecraftClient client) {
        if (client.player == null || client.world == null) {
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
        boolean keyDown = ModConfig.getInstance().voidTradeToggleKey.isPressed(client.getWindow().getHandle());
        if (keyDown && !wasToggleKeyDown
                && (client.currentScreen == null || client.currentScreen instanceof MerchantScreen)) {
            toggle();
        }
        wasToggleKeyDown = keyDown;

        // シングルプレイのポーズ中はワールドが止まるので、待機やタイムアウトの tick も進めない
        if (running && !client.isPaused()) {
            runTick(client);
        }
    }

    private void runTick(MinecraftClient client) {
        List<VoidTradeStep> steps = config.getSteps();
        if (steps.isEmpty()) {
            stop();
            return;
        }
        if (client.player.isDead()) {
            failWith(Text.translatable("message." + ASTTweaks.MOD_ID + ".voidtrade.reason.dead"));
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

    private void beginStep(MinecraftClient client, VoidTradeStep step) {
        stepStarted = true;
        stepTicks = 0;
        waitReason = null;
        moveTarget = null;
        lastTickPos = client.player.getPos();
        lastTickWorld = client.world;
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

    private void endStep(MinecraftClient client, VoidTradeStep step) {
        stepStarted = false;
        moveTarget = null;
        nudgeDirection = null;
        if (step.getType() == VoidTradeStepType.TRADE && inventoryFull && client.player != null) {
            client.player.sendMessage(
                    Text.translatable("message." + ASTTweaks.MOD_ID + ".voidtrade.inventoryFull", tradedCount)
                            .formatted(Formatting.YELLOW),
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
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player != null) {
                client.player.sendMessage(
                        Text.translatable("message." + ASTTweaks.MOD_ID + ".voidtrade.completed", loopsCompleted)
                                .formatted(Formatting.GREEN),
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
        failWith(Text.translatable("message." + ASTTweaks.MOD_ID + ".voidtrade.reason." + reasonKey));
        return StepResult.FAILED;
    }

    private void failWith(Text reason) {
        MinecraftClient client = MinecraftClient.getInstance();
        List<VoidTradeStep> steps = config.getSteps();
        Text stepText = stepIndex < steps.size() ? steps.get(stepIndex).describe() : Text.literal("-");
        if (client.player != null) {
            client.player.sendMessage(
                    Text.translatable("message." + ASTTweaks.MOD_ID + ".voidtrade.failed", stepIndex + 1, stepText, reason)
                            .formatted(Formatting.RED),
                    false);
        }
        ASTTweaks.LOGGER.warn("VoidTrade: stopped at step {} ({}): {}", stepIndex + 1, stepText.getString(), reason.getString());
        reset();
    }

    private void showStatus(MinecraftClient client, VoidTradeStep step) {
        if (client.player == null) {
            return;
        }
        int limit = config.getLoopCount();
        String loop = (loopsCompleted + 1) + "/" + (limit > 0 ? String.valueOf(limit) : "∞");
        MutableText detail = step.describe();
        if (moveTarget != null) {
            double distance = Math.sqrt(squaredHorizontalDistance(client.player, moveTarget));
            detail.append(Text.translatable("message." + ASTTweaks.MOD_ID + ".voidtrade.progress.distance",
                    String.format("%.1f", distance)));
        } else if (step.getType() == VoidTradeStepType.TRADE && tradedCount > 0) {
            detail.append(Text.translatable("message." + ASTTweaks.MOD_ID + ".voidtrade.progress.trades", tradedCount));
        }
        client.player.sendMessage(
                Text.translatable("message." + ASTTweaks.MOD_ID + ".voidtrade.status",
                        loop, stepIndex + 1, config.getSteps().size(), detail),
                true);
    }

    private StepResult executeStep(MinecraftClient client, VoidTradeStep step) {
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

    private StepResult tickMoveTo(MinecraftClient client, VoidTradeStep step) {
        if (teleportedSinceLastTick(client)) {
            // ゲートウェイやポータルに入って飛ばされた = 目的地（の入口）に着いた。飛ばされた先で歩き続けない
            moveTarget = null;
            return StepResult.DONE;
        }

        Vec3d target = new Vec3d(step.x + 0.5, step.y, step.z + 0.5);
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

    /**
     * 移動キーを数 tick だけ押したのと同じ動き（AHK の {a down}{a up} 相当）。向きは変えない。
     */
    private StepResult tickNudge(MinecraftClient client, VoidTradeStep step) {
        if (stepTicks > Math.max(1, step.nudgeTicks) || teleportedSinceLastTick(client)) {
            nudgeDirection = null;
            return StepResult.DONE;
        }
        nudgeDirection = step.getNudgeDirection();
        return StepResult.CONTINUE;
    }

    /**
     * VoidTradeInputMixin から毎 tick（Input#tick 直後）呼ばれる。
     * 移動ステップ中なら向きを目的地に合わせて前進入力で上書きする。取引画面を開いたままでも効く。
     */
    public void applyMovementInput(ClientPlayerEntity player, Input input) {
        if (!running || player != MinecraftClient.getInstance().player) {
            return;
        }
        if (nudgeDirection != null) {
            int forward = nudgeDirection.getForwardAxis();
            int sideways = nudgeDirection.getSidewaysAxis();
            input.pressingForward = forward > 0;
            input.pressingBack = forward < 0;
            input.pressingLeft = sideways > 0;
            input.pressingRight = sideways < 0;
            input.movementForward = forward;
            input.movementSideways = sideways;
            input.sneaking = false;
            return;
        }
        if (moveTarget == null) {
            return;
        }

        double dx = moveTarget.x - player.getX();
        double dz = moveTarget.z - player.getZ();
        if (moveKeepFacing) {
            // 向きはそのまま、目的地の方向を前後・左右の入力に分解する（Entity#movementInputToVelocity の逆変換）
            double yaw = Math.toRadians(player.getYaw());
            double sin = Math.sin(yaw);
            double cos = Math.cos(yaw);
            double forward = -dx * sin + dz * cos;
            double sideways = dx * cos + dz * sin;
            double length = Math.sqrt(forward * forward + sideways * sideways);
            if (length > 1.0E-4) {
                forward /= length;
                sideways /= length;
            }
            input.pressingForward = forward > 0.3;
            input.pressingBack = forward < -0.3;
            input.pressingLeft = sideways > 0.3;
            input.pressingRight = sideways < -0.3;
            input.movementForward = (float) forward;
            input.movementSideways = (float) sideways;
            input.sneaking = false;
            input.jumping = player.horizontalCollision && player.isOnGround();
            player.setSprinting(false);
            return;
        }
        float targetYaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0f;
        player.setYaw(player.getYaw() + MathHelper.wrapDegrees(targetYaw - player.getYaw()));

        input.pressingForward = true;
        input.pressingBack = false;
        input.pressingLeft = false;
        input.pressingRight = false;
        input.movementForward = 1.0f;
        input.movementSideways = 0.0f;
        input.sneaking = false;
        // 1 ブロックの段差は跳んで越える
        input.jumping = player.horizontalCollision && player.isOnGround();
        player.setSprinting(moveSprint);
    }

    /**
     * 前 tick から大きく位置が飛んだ（ワールドが変わった）か。移動系ステップ中に毎 tick 呼び、基準位置を更新する。
     */
    private boolean teleportedSinceLastTick(MinecraftClient client) {
        Vec3d pos = client.player.getPos();
        boolean teleported = client.world != lastTickWorld
                || (lastTickPos != null && lastTickPos.squaredDistanceTo(pos) > TELEPORT_DISTANCE_SQUARED);
        lastTickPos = pos;
        lastTickWorld = client.world;
        return teleported;
    }

    private static double squaredHorizontalDistance(ClientPlayerEntity player, Vec3d target) {
        double dx = target.x - player.getX();
        double dz = target.z - player.getZ();
        return dx * dx + dz * dz;
    }

    // ------------------------------------------------------------
    // 村人・ブロックへの右クリック
    // ------------------------------------------------------------

    private StepResult tickInteractEntity(MinecraftClient client, VoidTradeStep step) {
        ClientPlayerEntity player = client.player;
        if (player.currentScreenHandler instanceof MerchantScreenHandler) {
            return StepResult.DONE;
        }

        MerchantEntity merchant = findMerchant(client.world, step);
        if (merchant == null) {
            waitReason = "merchantNotFound";
            return StepResult.CONTINUE;
        }
        if (merchant.getBoundingBox().squaredMagnitude(player.getEyePos()) >= MAX_REACH_SQUARED) {
            waitReason = "merchantTooFar";
            return stepTicks > OUT_OF_REACH_GRACE_TICKS ? fail("merchantTooFar") : StepResult.CONTINUE;
        }

        Vec3d aim = merchant.getBoundingBox().getCenter();
        lookAt(player, aim);
        waitReason = "screenNotOpened";

        // 先に視線（回転）をサーバーへ送ってから次 tick 以降に右クリックする。開かなければ一定間隔で再試行
        if (stepTicks < 2 || (lastInteractTick >= 0 && stepTicks - lastInteractTick < INTERACT_RETRY_TICKS)) {
            return StepResult.CONTINUE;
        }
        lastInteractTick = stepTicks;
        merchantEntityId = merchant.getId();
        merchantWorld = client.world;
        // ItemScroller は手で右クリックした村人しか記録しないので、お気に入りと一覧の並びをこの村人に合わせる
        ItemScrollerCompat.setLastInteractedVillager(merchant.getUuid());

        // バニラの右クリック（MinecraftClient#doItemUse）と同じ順で送る
        ActionResult result = client.interactionManager.interactEntityAtLocation(
                player, merchant, new EntityHitResult(merchant, aim), Hand.MAIN_HAND);
        if (!result.isAccepted()) {
            result = client.interactionManager.interactEntity(player, merchant, Hand.MAIN_HAND);
        }
        if (result.isAccepted() && result.shouldSwingHand()) {
            player.swingHand(Hand.MAIN_HAND);
        }
        return StepResult.CONTINUE;
    }

    private static MerchantEntity findMerchant(ClientWorld world, VoidTradeStep step) {
        Vec3d center = new Vec3d(step.x + 0.5, step.y, step.z + 0.5);
        Box area = new Box(center, center).expand(Math.max(0.5, step.radius));

        MerchantEntity nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (MerchantEntity merchant : world.getEntitiesByClass(MerchantEntity.class, area,
                entity -> entity.isAlive() && !entity.isBaby())) {
            double distance = merchant.squaredDistanceTo(center);
            if (distance < nearestDistance) {
                nearest = merchant;
                nearestDistance = distance;
            }
        }
        return nearest;
    }

    private StepResult tickInteractBlock(MinecraftClient client, VoidTradeStep step) {
        ClientPlayerEntity player = client.player;
        BlockPos pos = new BlockPos(step.x, step.y, step.z);
        Vec3d center = Vec3d.ofCenter(pos);
        Vec3d eye = player.getEyePos();
        if (eye.squaredDistanceTo(center) > MAX_REACH_SQUARED) {
            return fail("blockTooFar");
        }

        // プレイヤー側を向いた面の中央を狙う
        Direction side = Direction.getFacing(eye.x - center.x, eye.y - center.y, eye.z - center.z);
        Vec3d hitPos = center.add(side.getOffsetX() * 0.5, side.getOffsetY() * 0.5, side.getOffsetZ() * 0.5);
        lookAt(player, hitPos);
        if (stepTicks < 2) {
            return StepResult.CONTINUE;
        }

        ActionResult result = client.interactionManager.interactBlock(
                player, Hand.MAIN_HAND, new BlockHitResult(hitPos, side, pos, false));
        if (result.isAccepted() && result.shouldSwingHand()) {
            player.swingHand(Hand.MAIN_HAND);
        }
        return StepResult.DONE;
    }

    private static void lookAt(ClientPlayerEntity player, Vec3d target) {
        Vec3d eye = player.getEyePos();
        double dx = target.x - eye.x;
        double dy = target.y - eye.y;
        double dz = target.z - eye.z;
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0f;
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, horizontal));
        player.setYaw(player.getYaw() + MathHelper.wrapDegrees(yaw - player.getYaw()));
        player.setPitch(MathHelper.clamp(pitch, -90.0f, 90.0f));
    }

    // ------------------------------------------------------------
    // アンロード待ち・取引・画面操作
    // ------------------------------------------------------------

    private StepResult tickWaitUnload(MinecraftClient client) {
        if (!(client.player.currentScreenHandler instanceof MerchantScreenHandler)) {
            return fail("screenClosed");
        }
        if (merchantEntityId < 0 || merchantWorld == null) {
            return fail("noMerchant");
        }

        waitReason = "notUnloaded";
        if (client.world != merchantWorld) {
            return StepResult.DONE;
        }
        // サーバーが村人の追跡をやめる（= 遠ざかった／アンロードされた）とクライアントからも消える
        Entity merchant = client.world.getEntityById(merchantEntityId);
        return merchant == null || merchant.isRemoved() ? StepResult.DONE : StepResult.CONTINUE;
    }

    private StepResult tickTrade(MinecraftClient client, VoidTradeStep step) {
        ClientPlayerEntity player = client.player;
        if (!(player.currentScreenHandler instanceof MerchantScreenHandler handler)) {
            return fail("screenClosed");
        }
        TradeOfferList original = originalOffers(handler);
        if (original.isEmpty()) {
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
            for (TradeTarget target : tradeQueue) {
                String key = stepIndex + ":" + target.index();
                int uses = original.get(target.index()).getUses();
                Integer baseline = baselineUses.putIfAbsent(key, uses);
                if (baseline != null && uses > baseline) {
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

        TradeTarget target = tradeQueue.get(tradeQueuePos);
        if (target.index() >= original.size()) {
            // 画面を開いている間に取引一覧が短くなった（通常は起きない）
            return nextOffer("offerLocked");
        }
        TradeOffer offer = original.get(target.index());
        if (offer.isDisabled()) {
            return nextOffer("offerLocked");
        }

        // 最初は必ず選び直す（前の取引の材料が残っていると別の取引が出力されるため）。
        // 以降は出力が空になったら選び直して、サーバー側でインベントリから材料を補充させる。
        // 出力が空のままのときはサーバーの応答を待つ間を空けて、一定間隔で選び直す
        if (!offerSelected
                || (handler.getSlot(OUTPUT_SLOT).getStack().isEmpty() && tradeIdleTicks % TRADE_RESELECT_TICKS == 0)) {
            selectOffer(client, handler, target.index());
            offerSelected = true;
        }
        if (handler.getSlot(OUTPUT_SLOT).getStack().isEmpty()) {
            if (++tradeIdleTicks < TRADE_IDLE_LIMIT) {
                return StepResult.CONTINUE;
            }
            // 選んでも出力が出ない = 材料切れ。前の取引の材料が入力欄に残ったまま戻せないならインベントリ満杯
            boolean inputsLeft = !handler.getSlot(0).getStack().isEmpty() || !handler.getSlot(1).getStack().isEmpty();
            if (inputsLeft && player.getInventory().getEmptySlot() < 0) {
                inventoryFull = true;
                return nextOffer("inventoryFull");
            }
            return nextOffer("noMaterials");
        }

        int usesBefore = offer.getUses();
        if (step.dropOutput) {
            // 1 tick に投げる回数は設定で抑える（1 回ごとにクリックと腕振りの 2 パケットを送るため）
            for (int i = 0; i < config.getTradesPerTick()
                    && !offer.isDisabled()
                    && !handler.getSlot(OUTPUT_SLOT).getStack().isEmpty(); i++) {
                client.interactionManager.clickSlot(handler.syncId, OUTPUT_SLOT, 1, SlotActionType.THROW, player);
            }
        } else {
            // シフトクリックは入っている材料の分だけ連続で取引される
            client.interactionManager.clickSlot(handler.syncId, OUTPUT_SLOT, 0, SlotActionType.QUICK_MOVE, player);
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

    /**
     * 今の取引を終えて次の取引へ進む。
     */
    private StepResult nextOffer(String finishReason) {
        TradeTarget target = tradeQueue.get(tradeQueuePos);
        ASTTweaks.LOGGER.info("VoidTrade: trade #{} ({}/{}) finished: {} ({} trades)",
                target.index() + 1, tradeQueuePos + 1, tradeQueue.size(), finishReason, currentOfferTraded);
        if (firstFinishReason == null) {
            firstFinishReason = finishReason;
        }
        if (currentOfferTraded == 0 && tradeQueue.size() > 1) {
            skippedOffers.add(target.description().copy()
                    .append(Text.translatable("message." + ASTTweaks.MOD_ID + ".voidtrade.skipped." + finishReason)));
        }
        currentOfferTraded = 0;
        tradeQueuePos++;
        offerSelected = false;
        tradeIdleTicks = 0;
        return StepResult.CONTINUE;
    }

    /**
     * お気に入りのうち 1 回も取引できなかったものを知らせる。毎周出るとうるさいので、ステップごとに 1 回だけ。
     */
    private void warnSkippedOffers(ClientPlayerEntity player) {
        if (skippedOffers.isEmpty() || !warnedSkipSteps.add(stepIndex)) {
            return;
        }
        MutableText list = Text.empty();
        for (int i = 0; i < skippedOffers.size(); i++) {
            if (i > 0) {
                list.append(", ");
            }
            list.append(skippedOffers.get(i));
        }
        player.sendMessage(Text.translatable("message." + ASTTweaks.MOD_ID + ".voidtrade.skippedFavorites",
                skippedOffers.size(), tradeQueue.size(), list).formatted(Formatting.YELLOW), false);
    }

    /**
     * 取引する取引を決める。お気に入りは ItemScroller が持つ元の一覧（サーバーと同じ並び）での番号をそのまま使う。
     * 番号指定は画面に表示されている並び（ItemScroller が並べ替えていればその並び）で数え、元の一覧での番号に直す。
     * 決められなければ理由を出して止め、null を返す。
     */
    private List<TradeTarget> resolveTradeTargets(ClientPlayerEntity player, MerchantScreenHandler handler, VoidTradeStep step) {
        List<TradeTarget> targets = new ArrayList<>();
        TradeOfferList original = originalOffers(handler);
        if (step.useFavorites) {
            for (int index : ItemScrollerCompat.getFavoriteTradeIndices(handler)) {
                if (index >= 0 && index < original.size() && targets.stream().noneMatch(t -> t.index() == index)) {
                    targets.add(new TradeTarget(index, describeOffer(original.get(index))));
                }
            }
            if (targets.isEmpty()) {
                fail(ItemScrollerCompat.isLoaded() ? "noFavorites" : "noItemScroller");
                sendOfferList(player, handler);
                return null;
            }
            ASTTweaks.LOGGER.info("VoidTrade: trading {} favorite(s): {}", targets.size(),
                    targets.stream().map(t -> "#" + (t.index() + 1)).collect(Collectors.joining(", ")));
            return targets;
        }

        TradeOfferList offers = handler.getRecipes();
        int visibleIndex = step.offerIndex - 1;
        if (visibleIndex < 0 || visibleIndex >= offers.size()) {
            fail("offerOutOfRange");
            sendOfferList(player, handler);
            return null;
        }
        TradeOffer offer = offers.get(visibleIndex);
        int realIndex = original.indexOf(offer);
        targets.add(new TradeTarget(realIndex >= 0 ? realIndex : visibleIndex, describeOffer(offer)));
        return targets;
    }

    /**
     * サーバーと同じ並びの取引一覧。ItemScroller 等が getRecipes() を並べ替えた一覧に差し替えていても変わらない。
     */
    private static TradeOfferList originalOffers(MerchantScreenHandler handler) {
        return ((MerchantScreenHandlerAccessor) handler).getMerchant().getOffers();
    }

    /**
     * MerchantScreen で取引を選んだときと同じ処理（MerchantScreen#syncRecipeIndex）。
     * サーバーへ送る番号と MerchantInventory の番号は元の一覧、材料の自動補充（switchTo）は表示側の一覧で数える
     * （ItemScroller のお気に入りの並べ替えに備えて番号を変換する）。表示側の一覧に同じ取引が見つからなければ
     * クライアント側の補充は行わず、サーバーが補充した結果を待つ。
     */
    private static void selectOffer(MinecraftClient client, MerchantScreenHandler handler, int realIndex) {
        TradeOfferList original = originalOffers(handler);
        if (realIndex < 0 || realIndex >= original.size()) {
            return;
        }
        int visibleIndex = handler.getRecipes().indexOf(original.get(realIndex));
        handler.setRecipeIndex(realIndex);
        if (visibleIndex >= 0) {
            handler.switchTo(visibleIndex);
        }
        client.getNetworkHandler().sendPacket(new SelectMerchantTradeC2SPacket(realIndex));
        if (visibleIndex >= 0 && client.currentScreen instanceof MerchantScreen screen) {
            ((MerchantScreenAccessor) screen).setSelectedIndex(visibleIndex);
        }
    }

    /**
     * 取引番号を決めやすいよう、表示順の取引一覧（必要な材料 → 結果）をチャットに出す。
     */
    private static void sendOfferList(ClientPlayerEntity player, MerchantScreenHandler handler) {
        TradeOfferList offers = handler.getRecipes();
        player.sendMessage(Text.translatable("message." + ASTTweaks.MOD_ID + ".voidtrade.offerList").formatted(Formatting.GRAY), false);
        for (int i = 0; i < offers.size(); i++) {
            TradeOffer offer = offers.get(i);
            MutableText line = Text.literal(" " + (i + 1) + ": ").append(describeOffer(offer));
            if (offer.isDisabled()) {
                line.append(Text.translatable("message." + ASTTweaks.MOD_ID + ".voidtrade.offerList.locked"));
            }
            player.sendMessage(line.formatted(Formatting.GRAY), false);
        }
    }

    /**
     * 「材料 → 結果」の 1 行表記。
     */
    private static MutableText describeOffer(TradeOffer offer) {
        MutableText text = describeStack(offer.getAdjustedFirstBuyItem());
        if (!offer.getSecondBuyItem().isEmpty()) {
            text.append(" + ").append(describeStack(offer.getSecondBuyItem()));
        }
        return text.append(" → ").append(describeStack(offer.getSellItem()));
    }

    private static MutableText describeStack(ItemStack stack) {
        return Text.empty().append(stack.getName()).append(" x" + stack.getCount());
    }

    private StepResult tickCloseScreen(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        if (player.currentScreenHandler != player.playerScreenHandler) {
            player.closeHandledScreen();
        }
        return StepResult.DONE;
    }

    private StepResult tickCommand(MinecraftClient client, VoidTradeStep step) {
        String command = step.command != null ? step.command.trim() : "";
        if (command.startsWith("/")) {
            command = command.substring(1);
        }
        if (!command.isEmpty()) {
            client.player.networkHandler.sendChatCommand(command);
        }
        return StepResult.DONE;
    }
}

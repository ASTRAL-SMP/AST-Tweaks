package com.astral.asttweaks.feature.autorepair;

import com.astral.asttweaks.ASTTweaks;
import com.astral.asttweaks.compat.TweakerooCompat;
import com.astral.asttweaks.feature.Feature;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;

import java.util.HashSet;
import java.util.Set;

/**
 * Fast auto repair feature (Tweakeroo style).
 * Uses experience bottles from offhand to repair items in mainhand at high speed.
 */
public class AutoRepairFeature implements Feature {
    private final AutoRepairConfig config;

    // State machine states
    private enum State {
        IDLE,               // Waiting for activation / checking conditions
        SETUP_OFFHAND,      // Moving experience bottle to offhand
        SETUP_MAINHAND,     // Moving repair item to mainhand
        REPAIRING,          // Fast using experience bottles
        SWAP_NEXT_ITEM      // Swapping to next repair item
    }

    private State currentState = State.IDLE;
    private int delayTicks = 0;
    private int currentRepairSlot = -1;      // Original slot of item being repaired
    private int previousMainhandSlot = -1;   // Player's original selected slot before repair
    private int originalOffhandDisplacedSlot = -1;  // オフハンドアイテムの退避先スロット
    private boolean hadOffhandItem = false;          // 修繕開始時にオフハンドにアイテムがあったか
    private int armorDisplacedFromTargetSlot = -1;  // armor 修繕時、target スロットの元アイテムを退避した先（PlayerInventory index）
    private Set<Integer> repairedSlots = new HashSet<>();  // Slots that have been repaired this session
    private static final int SYNC_DELAY_TICKS = 1;  // Minimal delay for fast swapping

    // PlayerInventory slot ranges
    private static final int ARMOR_SLOT_FIRST = 36;
    private static final int ARMOR_SLOT_LAST = 39;

    // In-flight throttle state: 投擲済みボトルの推定リペア量で過剰投擲を抑制する
    private int bottlesInFlight = 0;        // 投擲後 damage 更新待ちのボトル本数（推定）
    private int lastSeenDamage = -1;        // 直近に観測した damage 値
    private int ticksWithoutDamageChange = 0; // damage が更新されないまま経過した tick 数
    // 1 本の経験瓶あたりの平均修繕量（vanilla: 3-11 XP × 2 durability/XP ≒ 14）
    private static final int AVG_DURABILITY_PER_BOTTLE = 14;
    // damage 更新が来ない場合に in-flight 推定をリセットするまでの待機 tick 数
    private static final int IN_FLIGHT_RESET_TICKS = 20;

    public AutoRepairFeature() {
        this.config = new AutoRepairConfig();
    }

    @Override
    public String getId() {
        return "autorepair";
    }

    @Override
    public String getName() {
        return "Auto Repair";
    }

    @Override
    public void init() {
        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
        ASTTweaks.LOGGER.info("AutoRepair feature initialized (Fast mode)");
    }

    @Override
    public void tick() {
        // Tick processing handled in onClientTick
    }

    @Override
    public boolean isEnabled() {
        return config.isEnabled();
    }

    @Override
    public void setEnabled(boolean enabled) {
        config.setEnabled(enabled);
        if (!enabled) {
            TweakerooCompat.ensureRestored();
            resetState();
        }
    }

    public AutoRepairConfig getConfig() {
        return config;
    }

    /**
     * Get the target hotbar slot from config.
     */
    private int getTargetSlot() {
        return config.getTargetSlot();
    }

    private void onClientTick(MinecraftClient client) {
        if (!config.isEnabled()) {
            return;
        }

        if (client.player == null || client.world == null || client.isPaused()) {
            resetState();
            return;
        }

        // Don't process if in a GUI screen (except chat)
        if (client.currentScreen != null) {
            return;
        }

        processStateMachine(client);
    }

    private void processStateMachine(MinecraftClient client) {
        PlayerEntity player = client.player;

        switch (currentState) {
            case IDLE:
                // Check if we have experience bottles and items needing repair
                if (hasExperienceBottle(player) && findNextRepairItem(player) != -1) {
                    previousMainhandSlot = player.getInventory().selectedSlot;
                    // Disable Tweakeroo's AlmostBrokenTools to prevent interference
                    TweakerooCompat.disableAlmostBrokenTools();
                    currentState = State.SETUP_OFFHAND;
                    ASTTweaks.LOGGER.info("Starting fast repair sequence, previousSlot: {}", previousMainhandSlot);
                }
                break;

            case SETUP_OFFHAND:
                // Ensure experience bottle is in offhand
                if (!hasExperienceBottleInOffhand(player)) {
                    if (!moveExperienceBottleToOffhand(client, player)) {
                        // No bottle available, stop
                        resetState();
                        return;
                    }
                    delayTicks = SYNC_DELAY_TICKS;
                } else {
                    delayTicks = 0;
                }
                currentState = State.SETUP_MAINHAND;
                break;

            case SETUP_MAINHAND:
                if (delayTicks > 0) {
                    delayTicks--;
                    return;
                }

                // Find next item to repair and move to target slot
                int repairSlot = findNextRepairItem(player);
                if (repairSlot == -1) {
                    // All items repaired
                    finishRepair(client, player);
                    return;
                }

                int targetSlot = getTargetSlot();
                ASTTweaks.LOGGER.info("State: SETUP_MAINHAND, repairSlot: {}, targetSlot: {}", repairSlot, targetSlot);

                if (isArmorSlot(repairSlot)) {
                    // 防具スロットは ArmorSlot.canInsert で非装備品が弾かれるため、
                    // 先に target を空にしてから PICKUP ベースで防具を取り込む
                    if (!setupArmorRepair(client, player, repairSlot)) {
                        // 退避先確保失敗 → 今回はスキップ
                        repairedSlots.add(repairSlot);
                        return;
                    }
                    delayTicks = SYNC_DELAY_TICKS;
                } else if (repairSlot != targetSlot) {
                    moveItemToMainhand(client, player, repairSlot);
                    delayTicks = SYNC_DELAY_TICKS;
                }

                currentRepairSlot = repairSlot;
                // Select target slot
                player.getInventory().selectedSlot = targetSlot;
                // 新規アイテムにつき in-flight 推定をリセット
                bottlesInFlight = 0;
                lastSeenDamage = -1;
                ticksWithoutDamageChange = 0;
                currentState = State.REPAIRING;
                break;

            case REPAIRING:
                if (delayTicks > 0) {
                    delayTicks--;
                    return;
                }

                // Check if current item is fully repaired
                if (isCurrentItemFullyRepaired(player)) {
                    // damage=0 を確認した瞬間に swap-back と次アイテムへの swap-in を同一 tick で行い、
                    // Mending 対象が mainhand に居ない時間を最小化する（Clumps の大型オーブ取りこぼし対策）
                    swapToNextItemImmediate(client, player);
                    return;
                }

                // Check if we still have experience bottles
                if (!hasExperienceBottleInOffhand(player)) {
                    // Try to refill offhand
                    if (!moveExperienceBottleToOffhand(client, player)) {
                        // No more bottles, finish
                        finishRepair(client, player);
                        return;
                    }
                    delayTicks = SYNC_DELAY_TICKS;
                    return;
                }

                // Fast use experience bottles from offhand (with in-flight throttle)
                fastUseOffhand(client, player);
                break;

            case SWAP_NEXT_ITEM:
                // 旧フローの後方互換（resetState から直に到達しない限り通常は未使用）
                currentState = State.SETUP_MAINHAND;
                break;
        }
    }

    /**
     * Fast use experience bottles from offhand (multiple times per tick).
     * 投擲済み（in-flight）ボトル本数の推定で残りダメージをカバーしきれていれば投擲を見送る。
     * これにより Clumps 環境で起こる「最後の大型オーブが repaired 済みアイテムに当たって XP バーへ漏れる」損失を抑制する。
     */
    private void fastUseOffhand(MinecraftClient client, PlayerEntity player) {
        if (client.interactionManager == null) return;

        ItemStack targetStack = currentRepairSlot != -1 && isArmorSlot(currentRepairSlot)
                ? player.getInventory().getStack(currentRepairSlot)
                : player.getMainHandStack();
        int currentDamage = targetStack.getDamage();

        // damage 更新を検知したら in-flight カウンタを巻き戻す
        if (lastSeenDamage == -1 || currentDamage < lastSeenDamage) {
            int repaired = lastSeenDamage == -1 ? 0 : (lastSeenDamage - currentDamage);
            int consumedEstimate = Math.max(1, (repaired + AVG_DURABILITY_PER_BOTTLE - 1) / AVG_DURABILITY_PER_BOTTLE);
            bottlesInFlight = Math.max(0, bottlesInFlight - consumedEstimate);
            lastSeenDamage = currentDamage;
            ticksWithoutDamageChange = 0;
        } else {
            ticksWithoutDamageChange++;
            // damage 更新が長期間来なければ取りこぼしと判断してカウンタをリセットし投擲再開
            if (ticksWithoutDamageChange >= IN_FLIGHT_RESET_TICKS) {
                bottlesInFlight = 0;
                ticksWithoutDamageChange = 0;
            }
        }

        // 推定残ダメージ（保守的）
        int estimatedRemainingDamage = currentDamage - bottlesInFlight * AVG_DURABILITY_PER_BOTTLE;
        if (estimatedRemainingDamage <= 0) {
            // 投擲済みで十分。今ティックは見送ってオーブ到着を待つ
            return;
        }

        // 残ダメージに合わせて投擲数を抑える
        int bottlesNeeded = (estimatedRemainingDamage + AVG_DURABILITY_PER_BOTTLE - 1) / AVG_DURABILITY_PER_BOTTLE;
        int clicks = Math.min(config.getClicksPerTick(), bottlesNeeded);
        if (clicks <= 0) return;

        int actuallyThrown = 0;
        for (int i = 0; i < clicks; i++) {
            if (!hasExperienceBottleInOffhand(player)) {
                break;
            }
            client.interactionManager.interactItem(player, Hand.OFF_HAND);
            actuallyThrown++;
        }
        bottlesInFlight += actuallyThrown;
    }

    /**
     * damage=0 を観測した直後に同一 tick で swap-back と swap-in を実行する。
     * Mending 対象が mainhand に居ない時間を最小化することで、in-flight オーブの取りこぼしを減らす。
     */
    private void swapToNextItemImmediate(MinecraftClient client, PlayerEntity player) {
        int targetSlot = getTargetSlot();

        // 現アイテムを repaired として記録
        if (currentRepairSlot != -1) {
            repairedSlots.add(currentRepairSlot);
        }

        // 現アイテムを元のスロットへ戻す（swap-back）
        if (currentRepairSlot != -1 && currentRepairSlot != targetSlot) {
            if (isArmorSlot(currentRepairSlot)) {
                teardownArmorRepair(client, player, currentRepairSlot);
            } else {
                swapSlots(client, player, targetSlot, currentRepairSlot);
            }
        }

        // 次の修繕対象を探す
        int nextSlot = findNextRepairItem(player);
        if (nextSlot == -1) {
            // 全完了
            currentRepairSlot = -1;
            finishRepair(client, player);
            return;
        }

        // 次アイテムを target slot に持ってくる（swap-in、同一 tick で連続実行）
        if (isArmorSlot(nextSlot)) {
            if (!setupArmorRepair(client, player, nextSlot)) {
                // 退避先確保失敗 → スキップして次回検索
                repairedSlots.add(nextSlot);
                currentRepairSlot = -1;
                delayTicks = SYNC_DELAY_TICKS;
                currentState = State.SETUP_MAINHAND;
                return;
            }
        } else if (nextSlot != targetSlot) {
            swapSlots(client, player, nextSlot, targetSlot);
        }
        player.getInventory().selectedSlot = targetSlot;
        currentRepairSlot = nextSlot;

        // in-flight 推定をリセット（新アイテム用）
        bottlesInFlight = 0;
        lastSeenDamage = -1;
        ticksWithoutDamageChange = 0;

        // server 側へ slot 更新が伝わるのを待つ
        delayTicks = SYNC_DELAY_TICKS;
        currentState = State.REPAIRING;
    }

    private static boolean isArmorSlot(int invSlot) {
        return invSlot >= ARMOR_SLOT_FIRST && invSlot <= ARMOR_SLOT_LAST;
    }

    /**
     * PlayerInventory の armor slot (36-39) を PlayerScreenHandler のスクリーンスロット (5-8) に変換。
     */
    private static int armorInvToScreenSlot(int invSlot) {
        return 44 - invSlot;
    }

    /**
     * 防具は装備したまま修繕する（Mending は装備中の armor にも適用されるため）。
     * スロット移動は行わないので常に成功。
     */
    private boolean setupArmorRepair(MinecraftClient client, PlayerEntity player, int armorSlot) {
        armorDisplacedFromTargetSlot = -1;
        ASTTweaks.LOGGER.info("Set up armor repair (in-place): armorSlot={}", armorSlot);
        return true;
    }

    /**
     * 装備のまま修繕したので戻す必要なし。
     */
    private void teardownArmorRepair(MinecraftClient client, PlayerEntity player, int armorSlot) {
        // no-op: armor は装備したままなのでスロット移動は不要
    }

    /**
     * main inventory (9-35) から空きスロットを探す。なければ -1。
     * hotbar や offhand は除外（既に何か入っている前提）。
     */
    private int findEmptyMainInvSlot(PlayerEntity player) {
        PlayerInventory inv = player.getInventory();
        for (int i = 9; i < 36; i++) {
            if (inv.getStack(i).isEmpty()) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Check if player has experience bottle anywhere in inventory.
     */
    private boolean hasExperienceBottle(PlayerEntity player) {
        PlayerInventory inv = player.getInventory();

        // Check offhand first
        if (inv.offHand.get(0).getItem() == Items.EXPERIENCE_BOTTLE) {
            return true;
        }

        // Check main inventory
        for (int i = 0; i < 36; i++) {
            if (inv.getStack(i).getItem() == Items.EXPERIENCE_BOTTLE) {
                return true;
            }
        }

        return false;
    }

    /**
     * Check if offhand contains experience bottle.
     */
    private boolean hasExperienceBottleInOffhand(PlayerEntity player) {
        return player.getOffHandStack().getItem() == Items.EXPERIENCE_BOTTLE;
    }

    /**
     * Move experience bottle to offhand.
     */
    private boolean moveExperienceBottleToOffhand(MinecraftClient client, PlayerEntity player) {
        if (client.interactionManager == null) return false;

        PlayerInventory inv = player.getInventory();

        // Find experience bottle in inventory
        int bottleSlot = -1;
        for (int i = 0; i < 36; i++) {
            if (inv.getStack(i).getItem() == Items.EXPERIENCE_BOTTLE) {
                bottleSlot = i;
                break;
            }
        }

        if (bottleSlot == -1) {
            return false;
        }

        // 最初の呼び出し時、オフハンドにアイテムがあれば退避先を記録
        if (!hadOffhandItem) {
            ItemStack offhandStack = inv.offHand.get(0);
            if (!offhandStack.isEmpty() && offhandStack.getItem() != Items.EXPERIENCE_BOTTLE) {
                hadOffhandItem = true;
                originalOffhandDisplacedSlot = bottleSlot;
                ASTTweaks.LOGGER.info("Recording offhand item displaced to slot {}", bottleSlot);
            }
        }

        // Convert inventory slot to screen slot
        // Hotbar slots 0-8 -> screen slots 36-44
        // Main inventory 9-35 -> screen slots 9-35
        int screenSlot = bottleSlot < 9 ? bottleSlot + 36 : bottleSlot;

        // Offhand slot is 45 in player inventory screen
        int offhandScreenSlot = 45;

        // Swap bottle with offhand using SWAP action
        // First pick up the bottle
        client.interactionManager.clickSlot(
                player.currentScreenHandler.syncId,
                screenSlot,
                0,
                SlotActionType.PICKUP,
                player
        );

        // Then put it in offhand
        client.interactionManager.clickSlot(
                player.currentScreenHandler.syncId,
                offhandScreenSlot,
                0,
                SlotActionType.PICKUP,
                player
        );

        // If there was something in offhand, put it back
        client.interactionManager.clickSlot(
                player.currentScreenHandler.syncId,
                screenSlot,
                0,
                SlotActionType.PICKUP,
                player
        );

        ASTTweaks.LOGGER.info("Moved experience bottle to offhand from slot {}", bottleSlot);
        return true;
    }

    /**
     * Find next item that needs repair in inventory.
     * Returns the inventory slot, or -1 if none found.
     * Excludes slots that have already been repaired this session.
     */
    private int findNextRepairItem(PlayerEntity player) {
        PlayerInventory inv = player.getInventory();

        // Check hotbar first (slots 0-8)
        for (int i = 0; i < 9; i++) {
            if (!repairedSlots.contains(i) && needsRepair(inv.getStack(i))) {
                return i;
            }
        }

        // Then check main inventory (slots 9-35)
        for (int i = 9; i < 36; i++) {
            if (!repairedSlots.contains(i) && needsRepair(inv.getStack(i))) {
                return i;
            }
        }

        // 最後に装備中の防具 (slots 36-39)
        // 設定でオフの場合はスキップ
        if (config.shouldRepairArmor()) {
            for (int i = ARMOR_SLOT_FIRST; i <= ARMOR_SLOT_LAST; i++) {
                if (!repairedSlots.contains(i) && needsRepair(inv.getStack(i))) {
                    return i;
                }
            }
        }

        return -1;
    }

    /**
     * Check if an item needs repair.
     */
    private boolean needsRepair(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        // Check if item is damageable
        if (!stack.isDamageable()) {
            return false;
        }

        // Check if item has Mending
        if (!hasMending(stack)) {
            return false;
        }

        // Check if item is in the repair list (whitelist/blacklist)
        if (!config.shouldRepairItem(stack.getItem())) {
            return false;
        }

        // Check if item has any damage
        return stack.getDamage() > 0;
    }

    /**
     * Check if an item has the Mending enchantment.
     */
    private boolean hasMending(ItemStack stack) {
        return EnchantmentHelper.getLevel(Enchantments.MENDING, stack) > 0;
    }

    /**
     * 現在の修繕対象が damage=0 まで戻ったかチェック。
     * armor (装備のまま修繕) の場合は対象 armor スロットを、それ以外は mainhand を見る。
     */
    private boolean isCurrentItemFullyRepaired(PlayerEntity player) {
        ItemStack target = currentRepairSlot != -1 && isArmorSlot(currentRepairSlot)
                ? player.getInventory().getStack(currentRepairSlot)
                : player.getMainHandStack();
        if (target.isEmpty() || !target.isDamageable()) {
            return true;
        }
        return target.getDamage() == 0;
    }

    /**
     * Move item from given slot to target slot.
     */
    private void moveItemToMainhand(MinecraftClient client, PlayerEntity player, int fromSlot) {
        int targetSlot = getTargetSlot();
        swapSlots(client, player, fromSlot, targetSlot);
        ASTTweaks.LOGGER.info("Moving item from slot {} to target slot {}", fromSlot, targetSlot);
    }

    /**
     * Swap two inventory slots.
     */
    private void swapSlots(MinecraftClient client, PlayerEntity player, int slot1, int slot2) {
        if (client.interactionManager == null) return;

        // Use number key swap for hotbar slots
        if (slot2 < 9) {
            // Convert slot1 to screen slot
            int screenSlot1 = slot1 < 9 ? slot1 + 36 : slot1;

            // SWAP action with hotbar key
            client.interactionManager.clickSlot(
                    player.currentScreenHandler.syncId,
                    screenSlot1,
                    slot2,
                    SlotActionType.SWAP,
                    player
            );
        } else if (slot1 < 9) {
            // Swap other way
            int screenSlot2 = slot2 < 9 ? slot2 + 36 : slot2;

            client.interactionManager.clickSlot(
                    player.currentScreenHandler.syncId,
                    screenSlot2,
                    slot1,
                    SlotActionType.SWAP,
                    player
            );
        }
    }

    /**
     * Finish repair and restore player's original state.
     */
    private void finishRepair(MinecraftClient client, PlayerEntity player) {
        // Restore original selected slot
        if (previousMainhandSlot != -1 && previousMainhandSlot != player.getInventory().selectedSlot) {
            player.getInventory().selectedSlot = previousMainhandSlot;
        }

        // オフハンドに退避されたアイテムを復元
        if (hadOffhandItem && originalOffhandDisplacedSlot != -1 && client.interactionManager != null) {
            int screenSlot = originalOffhandDisplacedSlot < 9
                    ? originalOffhandDisplacedSlot + 36
                    : originalOffhandDisplacedSlot;
            int offhandScreenSlot = 45;

            // 退避先スロットのアイテムを拾う
            client.interactionManager.clickSlot(
                    player.currentScreenHandler.syncId,
                    screenSlot,
                    0,
                    SlotActionType.PICKUP,
                    player
            );
            // オフハンドに置く
            client.interactionManager.clickSlot(
                    player.currentScreenHandler.syncId,
                    offhandScreenSlot,
                    0,
                    SlotActionType.PICKUP,
                    player
            );
            // オフハンドにあったもの（経験値ボトル等）を元のスロットに戻す
            client.interactionManager.clickSlot(
                    player.currentScreenHandler.syncId,
                    screenSlot,
                    0,
                    SlotActionType.PICKUP,
                    player
            );

            ASTTweaks.LOGGER.info("Restored offhand item from slot {}", originalOffhandDisplacedSlot);
        }

        // Restore Tweakeroo AlmostBrokenTools if it was disabled
        TweakerooCompat.restoreAlmostBrokenTools();
        ASTTweaks.LOGGER.info("Fast repair completed, repaired {} slots", repairedSlots.size());
        resetState();
    }

    private void resetState() {
        // Ensure Tweakeroo setting is restored if we're resetting due to interruption
        TweakerooCompat.ensureRestored();
        currentState = State.IDLE;
        delayTicks = 0;
        currentRepairSlot = -1;
        previousMainhandSlot = -1;
        originalOffhandDisplacedSlot = -1;
        hadOffhandItem = false;
        armorDisplacedFromTargetSlot = -1;
        repairedSlots.clear();
        bottlesInFlight = 0;
        lastSeenDamage = -1;
        ticksWithoutDamageChange = 0;
    }
}

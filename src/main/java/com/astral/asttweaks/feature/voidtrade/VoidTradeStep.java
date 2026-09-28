package com.astral.asttweaks.feature.voidtrade;

import com.astral.asttweaks.ASTTweaks;
import com.astral.asttweaks.feature.automove.MoveDirection;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

/**
 * Void Trade ワークフローの 1 ステップ。Gson でそのまま保存される。
 * 種別ごとに使うフィールドだけが意味を持つ（他は無視）。
 */
public class VoidTradeStep {
    public VoidTradeStepType type = VoidTradeStepType.MOVE_TO;
    public int x;
    public int y;
    public int z;
    public boolean firstLoopOnly = false;                    // 全種別: 最初の周だけ実行する
    public boolean sprint = true;                            // MOVE_TO: ダッシュで移動（目的地を向いて歩くときのみ）
    public boolean keepFacing = false;                       // MOVE_TO: 向きを変えずに横移動で向かう
    public MoveDirection nudgeDirection = MoveDirection.LEFT;  // NUDGE: 向いている方向から見た移動方向
    public int nudgeTicks = 2;                               // NUDGE: 移動キーを押す tick 数
    public double radius = 2.0;                              // INTERACT_ENTITY: 指定座標からの村人検索半径
    public int ticks = 20;                                   // WAIT: 待機 tick 数
    public boolean useFavorites = false;                     // TRADE: ItemScroller のお気に入りをすべて取引する
    public int offerIndex = 1;                               // TRADE: 取引番号（画面の上から 1 始まり）
    public boolean dropOutput = false;                       // TRADE: 結果をインベントリに入れず捨てる
    public String command = "";                              // COMMAND: 送信するコマンド

    public VoidTradeStep() {
    }

    public VoidTradeStep(VoidTradeStepType type, int x, int y, int z) {
        this.type = type;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public VoidTradeStepType getType() {
        return type != null ? type : VoidTradeStepType.MOVE_TO;
    }

    public MoveDirection getNudgeDirection() {
        return nudgeDirection != null ? nudgeDirection : MoveDirection.LEFT;
    }

    /**
     * 設定ファイルや共有テキストから読んだデータの欠けを補う。種別が不明（新しい版の種別など）なら false。
     */
    public boolean sanitize() {
        if (type == null) {
            return false;
        }
        if (command == null) {
            command = "";
        }
        if (nudgeDirection == null) {
            nudgeDirection = MoveDirection.LEFT;
        }
        return true;
    }

    public VoidTradeStep copy() {
        VoidTradeStep copy = new VoidTradeStep(getType(), x, y, z);
        copy.firstLoopOnly = firstLoopOnly;
        copy.sprint = sprint;
        copy.keepFacing = keepFacing;
        copy.nudgeDirection = getNudgeDirection();
        copy.nudgeTicks = nudgeTicks;
        copy.radius = radius;
        copy.ticks = ticks;
        copy.useFavorites = useFavorites;
        copy.offerIndex = offerIndex;
        copy.dropOutput = dropOutput;
        copy.command = command;
        return copy;
    }

    /**
     * 一覧・ステータス表示用の 1 行説明。
     */
    public MutableText describe() {
        VoidTradeStepType stepType = getType();
        String prefix = "message." + ASTTweaks.MOD_ID + ".voidtrade.step.";
        String key = prefix + stepType.getId();
        MutableText text = switch (stepType) {
            case MOVE_TO -> {
                MutableText move = Text.translatable(key, x, y, z);
                if (keepFacing) {
                    yield move.append(Text.translatable(prefix + "keepFacingSuffix"));
                }
                yield sprint ? move.append(Text.translatable(prefix + "sprintSuffix")) : move;
            }
            case NUDGE -> Text.translatable(key,
                    Text.translatable("config." + ASTTweaks.MOD_ID + ".automove.direction." + getNudgeDirection().getId()), nudgeTicks);
            case INTERACT_ENTITY, INTERACT_BLOCK -> Text.translatable(key, x, y, z);
            case WAIT -> Text.translatable(key, ticks);
            case TRADE -> {
                MutableText trade = useFavorites ? Text.translatable(prefix + "tradeFavorites") : Text.translatable(key, offerIndex);
                yield dropOutput ? trade.append(Text.translatable(prefix + "dropSuffix")) : trade;
            }
            case COMMAND -> Text.translatable(key, command != null ? command : "");
            default -> Text.translatable(key);
        };
        return firstLoopOnly ? Text.translatable(prefix + "firstLoopPrefix").append(text) : text;
    }
}

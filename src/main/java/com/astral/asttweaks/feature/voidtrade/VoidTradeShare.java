package com.astral.asttweaks.feature.voidtrade;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.List;

/**
 * ワークフローをクリップボード経由で共有するためのテキスト形式（1 行の JSON）。
 */
public final class VoidTradeShare {
    private static final Gson GSON = new Gson();
    private static final String FORMAT = "asttweaks-voidtrade";
    private static final int VERSION = 1;
    private static final JsonObject DEFAULTS = GSON.toJsonTree(new VoidTradeStep()).getAsJsonObject();

    private VoidTradeShare() {}

    public static String export(List<VoidTradeStep> steps) {
        JsonObject root = new JsonObject();
        root.addProperty("format", FORMAT);
        root.addProperty("version", VERSION);
        JsonArray array = new JsonArray();
        for (VoidTradeStep step : steps) {
            JsonObject json = GSON.toJsonTree(step).getAsJsonObject();
            // Discord 等に貼りやすいよう、初期値のままの項目は省く（読み込み時に初期値で補われる）
            for (String key : DEFAULTS.keySet()) {
                if (!key.equals("type") && DEFAULTS.get(key).equals(json.get(key))) {
                    json.remove(key);
                }
            }
            array.add(json);
        }
        root.add("steps", array);
        return GSON.toJson(root);
    }

    /**
     * 共有テキストを読み取る。ステップの配列だけ（設定ファイルの voidTradeSteps をそのまま写したもの）も受け付ける。
     * 読み取れなければ null。
     */
    public static List<VoidTradeStep> parse(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            JsonElement root = JsonParser.parseString(text.trim());
            JsonElement stepsJson = root.isJsonArray() ? root
                    : root.isJsonObject() ? root.getAsJsonObject().get("steps") : null;
            if (stepsJson == null || !stepsJson.isJsonArray()) {
                return null;
            }
            List<VoidTradeStep> steps = new ArrayList<>();
            for (JsonElement element : stepsJson.getAsJsonArray()) {
                VoidTradeStep step = element.isJsonObject() ? GSON.fromJson(element, VoidTradeStep.class) : null;
                if (step != null && step.sanitize()) {
                    steps.add(step);
                }
            }
            return steps.isEmpty() ? null : steps;
        } catch (JsonParseException | IllegalStateException e) {
            return null;
        }
    }
}

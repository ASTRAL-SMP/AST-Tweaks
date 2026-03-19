package com.astral.asttweaks.feature;

import com.astral.asttweaks.feature.autoeat.AutoEatFeature;
import com.astral.asttweaks.feature.bonemealfilter.BoneMealFilterFeature;
import com.astral.asttweaks.feature.automove.AutoMoveFeature;
import com.astral.asttweaks.feature.autorepair.AutoRepairFeature;
import com.astral.asttweaks.feature.autototem.AutoTotemFeature;
import com.astral.asttweaks.feature.massgrindstone.MassGrindstoneFeature;
import com.astral.asttweaks.feature.entityculling.EntityCullingFeature;
import com.astral.asttweaks.feature.inventorysort.InventorySortFeature;
import com.astral.asttweaks.feature.lavahighlight.LavaHighlightFeature;
import com.astral.asttweaks.feature.mousesensitivity.MouseSensitivityFeature;
import com.astral.asttweaks.feature.notepad.NotepadFeature;
import com.astral.asttweaks.feature.scoreboard.ScoreboardFeature;
import com.astral.asttweaks.feature.silktouchswitch.SilkTouchSwitchFeature;
import com.astral.asttweaks.feature.updatechecker.UpdateCheckerFeature;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

import java.util.HashMap;
import java.util.Map;

/**
 * Manages all features in AST-Tweaks.
 * Handles registration, initialization, and tick updates.
 */
public class FeatureManager {
    private static FeatureManager instance;
    private final Map<String, Feature> features = new HashMap<>();

    private FeatureManager() {}

    public static FeatureManager getInstance() {
        if (instance == null) {
            instance = new FeatureManager();
        }
        return instance;
    }

    /**
     * Initialize the feature manager and register all features.
     */
    public void init() {
        // Register built-in features
        registerFeature(new ScoreboardFeature());
        registerFeature(new AutoEatFeature());
        registerFeature(new AutoMoveFeature());
        registerFeature(new EntityCullingFeature());
        registerFeature(new LavaHighlightFeature());
        registerFeature(new NotepadFeature());
        registerFeature(new AutoTotemFeature());
        registerFeature(new AutoRepairFeature());
        registerFeature(new MassGrindstoneFeature());
        registerFeature(new UpdateCheckerFeature());
        registerFeature(new InventorySortFeature());
        registerFeature(new BoneMealFilterFeature());
        registerFeature(new SilkTouchSwitchFeature());
        registerFeature(new MouseSensitivityFeature());

        // Initialize all features
        for (Feature feature : features.values()) {
            feature.init();
        }

        // Register tick event
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            for (Feature feature : features.values()) {
                if (feature.isEnabled()) {
                    feature.tick();
                }
            }
        });
    }

    /**
     * Register a new feature.
     */
    public void registerFeature(Feature feature) {
        features.put(feature.getId(), feature);
    }

    /**
     * Get a feature by its ID.
     */
    public Feature getFeature(String id) {
        return features.get(id);
    }

    /**
     * Get all registered features.
     */
    public Map<String, Feature> getFeatures() {
        return features;
    }

    /**
     * Get the scoreboard feature.
     */
    public ScoreboardFeature getScoreboardFeature() {
        return (ScoreboardFeature) getFeature("scoreboard");
    }

    /**
     * Get the auto-eat feature.
     */
    public AutoEatFeature getAutoEatFeature() {
        return (AutoEatFeature) getFeature("autoeat");
    }

    /**
     * Get the auto-move feature.
     */
    public AutoMoveFeature getAutoMoveFeature() {
        return (AutoMoveFeature) getFeature("automove");
    }

    /**
     * Get the entity culling feature.
     */
    public EntityCullingFeature getEntityCullingFeature() {
        return (EntityCullingFeature) getFeature("entityculling");
    }

    /**
     * Get the lava highlight feature.
     */
    public LavaHighlightFeature getLavaHighlightFeature() {
        return (LavaHighlightFeature) getFeature("lavahighlight");
    }

    /**
     * Get the notepad feature.
     */
    public NotepadFeature getNotepadFeature() {
        return (NotepadFeature) getFeature("notepad");
    }

    /**
     * Get the auto totem feature.
     */
    public AutoTotemFeature getAutoTotemFeature() {
        return (AutoTotemFeature) getFeature("autototem");
    }

    /**
     * Get the auto repair feature.
     */
    public AutoRepairFeature getAutoRepairFeature() {
        return (AutoRepairFeature) getFeature("autorepair");
    }

    /**
     * Get the update checker feature.
     */
    public UpdateCheckerFeature getUpdateCheckerFeature() {
        return (UpdateCheckerFeature) getFeature("updatechecker");
    }

    /**
     * Get the mass grindstone feature.
     */
    public MassGrindstoneFeature getMassGrindstoneFeature() {
        return (MassGrindstoneFeature) getFeature("massgrindstone");
    }

    /**
     * Get the inventory sort feature.
     */
    public InventorySortFeature getInventorySortFeature() {
        return (InventorySortFeature) getFeature("inventorysort");
    }

    /**
     * Get the bone meal filter feature.
     */
    public BoneMealFilterFeature getBoneMealFilterFeature() {
        return (BoneMealFilterFeature) getFeature("bonemealfilter");
    }

    /**
     * Get the silk touch switch feature.
     */
    public SilkTouchSwitchFeature getSilkTouchSwitchFeature() {
        return (SilkTouchSwitchFeature) getFeature("silktouchswitch");
    }

    /**
     * Get the mouse sensitivity feature.
     */
    public MouseSensitivityFeature getMouseSensitivityFeature() {
        return (MouseSensitivityFeature) getFeature("mousesensitivity");
    }
}

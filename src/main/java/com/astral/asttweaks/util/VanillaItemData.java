package com.astral.asttweaks.util;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.RegistryAccess;
import java.util.function.Function;
//? if >=26.1 {
import com.astral.asttweaks.ASTTweaks;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.GenericMessageScreen;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.WorldLoader;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.ServerPacksSource;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.util.Util;
import net.minecraft.world.level.WorldDataConfiguration;
import java.util.concurrent.CompletableFuture;
//?}

/**
 * Item components and static tags are bound only when a world's data is loaded, so on
 * the title screen item lists would be empty. Before opening such a screen there, this
 * loads the vanilla data pack the same way vanilla's "Create New World" screen does
 * (which also binds them as a side effect).
 * 1.20.1 以前はアイテムデータが常に利用できるので、単に画面を開くだけ。
 */
public final class VanillaItemData {
    //? if >=26.1 {
    private static RegistryAccess.Frozen registries;
    private static boolean joinListenerRegistered = false;
    //?}

    private VanillaItemData() {}

    /**
     * Opens the screen built by {@code factory} (given the current screen as parent),
     * loading vanilla item data first if no world has been loaded yet.
     */
    public static void open(Function<Screen, Screen> factory) {
        open(factory, false);
    }

    /**
     * Like {@link #open(Function)}; with {@code needsRegistries} the vanilla registries are
     * also loaded when outside a world (e.g. after leaving one), for {@link #registries()}.
     */
    public static void open(Function<Screen, Screen> factory, boolean needsRegistries) {
        Screen parent = McClient.screen();
        //? if >=26.1 {
        Minecraft minecraft = Minecraft.getInstance();
        if (!ItemDisplay.itemDataLoaded() || (needsRegistries && minecraft.level == null && registries == null)) {
            load(minecraft);
        }
        //?}
        McClient.setScreen(factory.apply(parent));
    }

    /**
     * Registries loaded by {@link #open} while outside a world; null once a world is joined
     * (and always null before 26.1).
     */
    public static RegistryAccess registries() {
        //? if >=26.1 {
        return registries;
        //?} else {
        /*return null;
        *///?}
    }

    //? if >=26.1 {
    private static void load(Minecraft minecraft) {
        if (!joinListenerRegistered) {
            joinListenerRegistered = true;
            ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> registries = null);
        }
        minecraft.setScreenAndShow(new GenericMessageScreen(Component.translatable("config.asttweaks.itemlist.loading")));
        try {
            PackRepository packs = new PackRepository(new ServerPacksSource(minecraft.directoryValidator()));
            WorldLoader.InitConfig config = new WorldLoader.InitConfig(
                    new WorldLoader.PackConfig(packs, WorldDataConfiguration.DEFAULT, false, true),
                    Commands.CommandSelection.INTEGRATED,
                    LevelBasedPermissionSet.GAMEMASTER);
            CompletableFuture<RegistryAccess.Frozen> result = WorldLoader.<Void, RegistryAccess.Frozen>load(
                    config,
                    context -> new WorldLoader.DataLoadOutput<>(null, context.datapackDimensions()),
                    (resources, managers, layers, cookie) -> {
                        resources.close();
                        return layers.compositeAccess();
                    },
                    Util.backgroundExecutor(),
                    minecraft);
            minecraft.managedBlock(result::isDone);
            registries = result.join();
        } catch (RuntimeException e) {
            ASTTweaks.LOGGER.warn("Failed to load vanilla item data for the title screen", e);
        }
    }
    //?}
}

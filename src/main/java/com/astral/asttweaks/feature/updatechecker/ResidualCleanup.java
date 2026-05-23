package com.astral.asttweaks.feature.updatechecker;

import com.astral.asttweaks.ASTTweaks;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Removes leftover AST-Tweaks artifacts in the mods directory at startup.
 * Fabric Loader picks the newest matching jar, so older jars left behind by
 * a prior in-game update are never loaded and have no file lock — they can
 * be deleted immediately even on Windows.
 */
public final class ResidualCleanup {

    private ResidualCleanup() {}

    public static void run() {
        Path modsDir = FabricLoader.getInstance().getGameDir().resolve("mods");
        if (!Files.isDirectory(modsDir)) {
            return;
        }

        Path currentJar = FabricLoader.getInstance()
                .getModContainer(ASTTweaks.MOD_ID)
                .flatMap(c -> c.getOrigin().getPaths().stream().findFirst())
                .orElse(null);

        List<Path> targets = new ArrayList<>();
        try (Stream<Path> stream = Files.list(modsDir)) {
            stream.filter(ResidualCleanup::isAstTweaksArtifact)
                    .filter(p -> !isCurrentJar(p, currentJar))
                    .forEach(targets::add);
        } catch (IOException e) {
            ASTTweaks.LOGGER.warn("ResidualCleanup: failed to scan mods directory: {}", e.getMessage());
            return;
        }

        if (targets.isEmpty()) {
            return;
        }

        for (Path target : targets) {
            try {
                Files.delete(target);
                ASTTweaks.LOGGER.info("ResidualCleanup: removed leftover artifact {}", target.getFileName());
            } catch (IOException e) {
                try {
                    target.toFile().deleteOnExit();
                    ASTTweaks.LOGGER.warn("ResidualCleanup: could not delete {} now ({}); scheduled for JVM exit",
                            target.getFileName(), e.getMessage());
                } catch (Exception ignored) {
                    ASTTweaks.LOGGER.warn("ResidualCleanup: could not remove leftover artifact {}", target.getFileName());
                }
            }
        }
    }

    private static boolean isAstTweaksArtifact(Path path) {
        if (!Files.isRegularFile(path)) {
            return false;
        }
        String name = path.getFileName().toString().toLowerCase();
        if (!name.startsWith("ast-tweaks-")) {
            return false;
        }
        return name.endsWith(".jar")
                || name.endsWith(".jar.part")
                || name.endsWith(".jar.bak")
                || name.endsWith(".jar.disabled");
    }

    private static boolean isCurrentJar(Path candidate, Path currentJar) {
        if (currentJar == null) {
            return false;
        }
        try {
            return Files.isSameFile(candidate, currentJar);
        } catch (IOException e) {
            return candidate.toAbsolutePath().normalize().equals(currentJar.toAbsolutePath().normalize());
        }
    }
}

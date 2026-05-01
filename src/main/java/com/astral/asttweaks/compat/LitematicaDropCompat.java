package com.astral.asttweaks.compat;

import com.astral.asttweaks.ASTTweaks;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.data.SchematicHolder;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.util.FileType;
import fi.dy.masa.litematica.util.WorldUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Optional Litematica integration. This class is only referenced from the
 * Litematica-gated mixin, so AST-Tweaks remains usable without Litematica.
 */
public final class LitematicaDropCompat {
    private LitematicaDropCompat() {
    }

    public static void handleDroppedFiles(List<Path> paths) {
        int loaded = 0;
        int imported = 0;
        int skipped = 0;
        int failed = 0;

        for (Path path : paths) {
            File source = path.toFile();
            FileType sourceType = FileType.fromFile(source);

            if (!isSupported(sourceType)) {
                skipped++;
                continue;
            }

            try {
                File fileToLoad = importIfNeeded(source);
                FileType loadType = FileType.fromFile(fileToLoad);
                LitematicaSchematic schematic = loadSchematic(fileToLoad, loadType);

                if (schematic == null) {
                    failed++;
                    continue;
                }

                SchematicHolder.getInstance().addSchematic(schematic, true);

                if (!source.getCanonicalFile().equals(fileToLoad.getCanonicalFile())) {
                    imported++;
                }

                loaded++;
            } catch (Exception e) {
                failed++;
                ASTTweaks.LOGGER.warn("Failed to import dropped Litematica schematic '{}'", source.getAbsolutePath(), e);
            }
        }

        sendSummary(loaded, imported, skipped, failed);
    }

    private static boolean isSupported(FileType fileType) {
        return fileType == FileType.LITEMATICA_SCHEMATIC
                || fileType == FileType.SCHEMATICA_SCHEMATIC
                || fileType == FileType.SPONGE_SCHEMATIC
                || fileType == FileType.VANILLA_STRUCTURE;
    }

    private static File importIfNeeded(File source) throws IOException {
        File baseDir = DataManager.getSchematicsBaseDirectory();
        Files.createDirectories(baseDir.toPath());

        if (isInsideDirectory(source.toPath(), baseDir.toPath())) {
            return source.getCanonicalFile();
        }

        File target = resolveUniqueTarget(baseDir.toPath(), source.getName()).toFile();
        Files.copy(source.toPath(), target.toPath());
        return target.getCanonicalFile();
    }

    private static boolean isInsideDirectory(Path file, Path directory) throws IOException {
        Path realFile = file.toRealPath();
        Path realDirectory = directory.toRealPath();
        return realFile.startsWith(realDirectory);
    }

    private static Path resolveUniqueTarget(Path directory, String fileName) {
        Path candidate = directory.resolve(fileName);

        if (!Files.exists(candidate)) {
            return candidate;
        }

        String baseName = fileName;
        String extension = "";
        int extensionIndex = fileName.lastIndexOf('.');

        if (extensionIndex > 0) {
            baseName = fileName.substring(0, extensionIndex);
            extension = fileName.substring(extensionIndex);
        }

        int index = 1;
        do {
            candidate = directory.resolve(baseName + " (" + index + ")" + extension);
            index++;
        } while (Files.exists(candidate));

        return candidate;
    }

    private static LitematicaSchematic loadSchematic(File file, FileType fileType) {
        if (fileType == FileType.LITEMATICA_SCHEMATIC) {
            return LitematicaSchematic.createFromFile(file.getParentFile(), file.getName());
        }

        if (fileType == FileType.SCHEMATICA_SCHEMATIC) {
            return WorldUtils.convertSchematicaSchematicToLitematicaSchematic(
                    file.getParentFile(),
                    file.getName(),
                    false,
                    message -> sendMessage(Text.translatable(message))
            );
        }

        if (fileType == FileType.SPONGE_SCHEMATIC) {
            return WorldUtils.convertSpongeSchematicToLitematicaSchematic(file.getParentFile(), file.getName());
        }

        if (fileType == FileType.VANILLA_STRUCTURE) {
            return WorldUtils.convertStructureToLitematicaSchematic(file.getParentFile(), file.getName());
        }

        return null;
    }

    private static void sendSummary(int loaded, int imported, int skipped, int failed) {
        if (loaded > 0) {
            sendMessage(Text.translatable("message." + ASTTweaks.MOD_ID + ".litematicaDrop.loaded", loaded, imported));
        }

        if (skipped > 0) {
            sendMessage(Text.translatable("message." + ASTTweaks.MOD_ID + ".litematicaDrop.skipped", skipped));
        }

        if (failed > 0) {
            sendMessage(Text.translatable("message." + ASTTweaks.MOD_ID + ".litematicaDrop.failed", failed));
        }
    }

    private static void sendMessage(Text message) {
        MinecraftClient client = MinecraftClient.getInstance();

        if (client.player != null) {
            client.player.sendMessage(message, false);
        } else if (client.inGameHud != null) {
            client.inGameHud.getChatHud().addMessage(message);
        }
    }
}

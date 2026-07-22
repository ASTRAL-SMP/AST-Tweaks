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
        handleDroppedFiles(paths, null);
    }

    /**
     * Imports the dropped files into {@code targetDirectory}, which should be the
     * folder currently open in the schematic browser. When it is {@code null} or
     * not part of the schematics tree, the schematics base directory is used.
     */
    public static void handleDroppedFiles(List<Path> paths, File targetDirectory) {
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
                File fileToLoad = importIfNeeded(source, targetDirectory);
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

    private static File importIfNeeded(File source, File targetDirectory) throws IOException {
        File baseDir = DataManager.getSchematicsBaseDirectory();
        Files.createDirectories(baseDir.toPath());

        // A file already living somewhere inside the schematics tree is loaded in
        // place rather than copied around.
        if (isInsideDirectory(source.toPath(), baseDir.toPath())) {
            return source.getCanonicalFile();
        }

        File destinationDir = resolveDestinationDirectory(baseDir, targetDirectory);
        Files.createDirectories(destinationDir.toPath());

        File target = resolveUniqueTarget(destinationDir.toPath(), source.getName()).toFile();
        Files.copy(source.toPath(), target.toPath());
        return target.getCanonicalFile();
    }

    /**
     * Resolves the folder a dropped file should be copied into. Only directories
     * that live inside the schematics base directory are honoured; anything else
     * falls back to the base directory.
     */
    private static File resolveDestinationDirectory(File baseDir, File targetDirectory) throws IOException {
        if (targetDirectory != null
                && targetDirectory.isDirectory()
                && isInsideDirectory(targetDirectory.toPath(), baseDir.toPath())) {
            return targetDirectory.getCanonicalFile();
        }

        return baseDir;
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

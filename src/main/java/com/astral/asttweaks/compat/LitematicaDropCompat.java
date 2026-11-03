package com.astral.asttweaks.compat;

import com.astral.asttweaks.ASTTweaks;
import com.astral.asttweaks.util.McClient;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.data.SchematicHolder;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.util.FileType;
import fi.dy.masa.litematica.util.WorldUtils;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

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
     * Determines which folder a drop should land in. The widget's current
     * directory is authoritative, but the browser state cached by Litematica is
     * used as a fallback so a drop still follows the visible hierarchy even when
     * the list widget cannot be reached.
     */
    //? if >=26.1 {
    public static File resolveBrowserDirectory(Path currentDirectoryPath, String browserContext, Path defaultDirectoryPath) {
    //?} else {
    /*public static File resolveBrowserDirectory(File currentDirectoryPath, String browserContext, File defaultDirectoryPath) {
    *///?}
        File currentDirectory = toFile(currentDirectoryPath);
        File defaultDirectory = toFile(defaultDirectoryPath);

        if (isUsableDirectory(currentDirectory)) {
            return currentDirectory;
        }

        if (browserContext != null) {
            File cached = toFile(DataManager.getDirectoryCache().getCurrentDirectoryForContext(browserContext));

            if (isUsableDirectory(cached)) {
                ASTTweaks.LOGGER.debug("Litematica drop: falling back to the cached browser directory for '{}'", browserContext);
                return cached;
            }
        }

        return isUsableDirectory(defaultDirectory) ? defaultDirectory : null;
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

        File destinationDir;

        try {
            destinationDir = resolveDestinationDirectory(targetDirectory);
            Files.createDirectories(destinationDir.toPath());
        } catch (IOException e) {
            ASTTweaks.LOGGER.warn("Failed to prepare the Litematica drop target directory", e);
            sendSummary(0, 0, 0, paths.size(), null);
            return;
        }

        ASTTweaks.LOGGER.info("Litematica drop: importing {} dropped file(s) into '{}' (browser directory: {})",
                paths.size(), destinationDir.getAbsolutePath(),
                targetDirectory != null ? targetDirectory.getAbsolutePath() : "<unknown>");

        for (Path path : paths) {
            File source = path.toFile();
            FileType sourceType = fileType(source);

            if (!isSupported(sourceType)) {
                skipped++;
                continue;
            }

            try {
                File fileToLoad = importIfNeeded(source, destinationDir);
                FileType loadType = fileType(fileToLoad);
                LitematicaSchematic schematic = loadSchematic(fileToLoad, loadType);

                if (schematic == null) {
                    failed++;
                    continue;
                }

                SchematicHolder.getInstance().addSchematic(schematic, true);

                if (!canonicalize(source).equals(fileToLoad)) {
                    imported++;
                }

                loaded++;
            } catch (Exception e) {
                failed++;
                ASTTweaks.LOGGER.warn("Failed to import dropped Litematica schematic '{}'", source.getAbsolutePath(), e);
            }
        }

        sendSummary(loaded, imported, skipped, failed, imported > 0 ? destinationDir : null);
    }

    private static boolean isSupported(FileType fileType) {
        return fileType == FileType.LITEMATICA_SCHEMATIC
                || fileType == FileType.SCHEMATICA_SCHEMATIC
                || fileType == FileType.SPONGE_SCHEMATIC
                || fileType == FileType.VANILLA_STRUCTURE;
    }

    private static File importIfNeeded(File source, File destinationDir) throws IOException {
        File canonicalSource = canonicalize(source);
        File sourceParent = canonicalSource.getParentFile();

        // Already sitting in the folder the browser is showing: load it in place.
        if (sourceParent != null && sourceParent.equals(destinationDir)) {
            return canonicalSource;
        }

        File target = resolveUniqueTarget(destinationDir.toPath(), source.getName()).toFile();
        Files.copy(source.toPath(), target.toPath());
        return canonicalize(target);
    }

    /**
     * Resolves the folder a dropped file should be copied into. Only directories
     * that live inside the schematics base directory are honoured; anything else
     * falls back to the base directory.
     */
    private static File resolveDestinationDirectory(File targetDirectory) {
        File baseDir = canonicalize(schematicsBaseDirectory());

        if (targetDirectory == null) {
            return baseDir;
        }

        File canonicalTarget = canonicalize(targetDirectory);

        if (!isInsideDirectory(canonicalTarget, baseDir)) {
            ASTTweaks.LOGGER.warn("Litematica drop: browser directory '{}' is outside the schematics base directory '{}', importing into the base directory instead",
                    canonicalTarget.getAbsolutePath(), baseDir.getAbsolutePath());
            return baseDir;
        }

        return canonicalTarget;
    }

    private static File toFile(Path path) {
        return path != null ? path.toFile() : null;
    }

    private static File toFile(File file) {
        return file;
    }

    // Litematica は 0.2x（1.21 以降）でファイル API が File から Path に変わった

    private static FileType fileType(File file) {
        //? if >=26.1 {
        return FileType.fromFile(file.toPath());
        //?} else {
        /*return FileType.fromFile(file);
        *///?}
    }

    private static File schematicsBaseDirectory() {
        //? if >=26.1 {
        return DataManager.getSchematicsBaseDirectory().toFile();
        //?} else {
        /*return DataManager.getSchematicsBaseDirectory();
        *///?}
    }

    //? if >=26.1 {
    private static Path schematicDir(File file) {
        return file.getParentFile().toPath();
    }
    //?} else {
    /*private static File schematicDir(File file) {
        return file.getParentFile();
    }
    *///?}

    private static boolean isUsableDirectory(File directory) {
        return directory != null && directory.isDirectory();
    }

    private static File canonicalize(File file) {
        try {
            return file.getCanonicalFile();
        } catch (IOException e) {
            return file.getAbsoluteFile();
        }
    }

    private static boolean isInsideDirectory(File file, File directory) {
        return file.toPath().startsWith(directory.toPath());
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
            return LitematicaSchematic.createFromFile(schematicDir(file), file.getName());
        }

        if (fileType == FileType.SCHEMATICA_SCHEMATIC) {
            return WorldUtils.convertSchematicaSchematicToLitematicaSchematic(
                    schematicDir(file),
                    file.getName(),
                    false,
                    message -> sendMessage(Component.translatable(message))
            );
        }

        if (fileType == FileType.SPONGE_SCHEMATIC) {
            return WorldUtils.convertSpongeSchematicToLitematicaSchematic(schematicDir(file), file.getName());
        }

        if (fileType == FileType.VANILLA_STRUCTURE) {
            return WorldUtils.convertStructureToLitematicaSchematic(schematicDir(file), file.getName());
        }

        return null;
    }

    private static void sendSummary(int loaded, int imported, int skipped, int failed, File destinationDir) {
        if (loaded > 0) {
            sendMessage(Component.translatable("message." + ASTTweaks.MOD_ID + ".litematicaDrop.loaded", loaded, imported));
        }

        if (destinationDir != null) {
            sendMessage(Component.translatable("message." + ASTTweaks.MOD_ID + ".litematicaDrop.importedInto",
                    describeDestination(destinationDir)));
        }

        if (skipped > 0) {
            sendMessage(Component.translatable("message." + ASTTweaks.MOD_ID + ".litematicaDrop.skipped", skipped));
        }

        if (failed > 0) {
            sendMessage(Component.translatable("message." + ASTTweaks.MOD_ID + ".litematicaDrop.failed", failed));
        }
    }

    /**
     * Renders the destination as a path relative to the schematics base directory
     * so the chat message stays short and matches what the browser shows.
     */
    private static String describeDestination(File destinationDir) {
        File baseDir = canonicalize(schematicsBaseDirectory());

        if (destinationDir.equals(baseDir)) {
            return "/";
        }

        if (isInsideDirectory(destinationDir, baseDir)) {
            return baseDir.toPath().relativize(destinationDir.toPath()).toString();
        }

        return destinationDir.getAbsolutePath();
    }

    private static void sendMessage(Component message) {
        Minecraft client = Minecraft.getInstance();

        if (client.player != null) {
            client.player.sendSystemMessage(message);
        } else if (client.gui != null) {
            //? if >=26.1 {
            McClient.chat().addClientSystemMessage(message);
            //?} else {
            /*McClient.chat().addMessage(message);
            *///?}
        }
    }
}

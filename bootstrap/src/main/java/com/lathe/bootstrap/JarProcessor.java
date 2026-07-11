package com.lathe.bootstrap;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap; // Added import for HashMap
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class JarProcessor {

    /**
     * Creates a directory if it does not already exist.
     *
     * @param directory The path to the directory to create.
     * @throws IOException if the directory cannot be created.
     */
    public static void createDirectory(Path directory) throws IOException {
        if (!Files.exists(directory)) {
            Files.createDirectories(directory);
            System.out.println("Created directory: " + directory);
        } else {
            System.out.println("Directory already exists: " + directory);
        }
    }

    /**
     * Copies the original Minecraft JAR to the temporary runtime directory.
     *
     * @param originalMinecraftJar The path to the original Minecraft JAR.
     * @param tempRuntimeDir The path to the temporary runtime directory.
     * @return The path to the copied Minecraft JAR in the temporary directory.
     * @throws IOException if the file cannot be copied.
     */
    public static Path copyMinecraftJarToTemp(Path originalMinecraftJar, Path tempRuntimeDir) throws IOException {
        Path tempMinecraftJar = tempRuntimeDir.resolve(originalMinecraftJar.getFileName().toString());
        Files.copy(originalMinecraftJar, tempMinecraftJar, StandardCopyOption.REPLACE_EXISTING);
        System.out.println("Copied original Minecraft JAR to: " + tempMinecraftJar);
        return tempMinecraftJar;
    }

    /**
     * Removes the META-INF directory from a JAR file.
     *
     * @param jarPath The path to the JAR file.
     * @throws IOException if an I/O error occurs.
     */
    public static void removeMetaInf(Path jarPath) throws IOException {
        // Use an explicitly typed empty map for Java 8 compatibility
        Map<String, Object> env = new HashMap<>();
        try (FileSystem fs = FileSystems.newFileSystem(jarPath, env)) {
            Path metaInfPath = fs.getPath("META-INF");
            if (Files.exists(metaInfPath)) {
                System.out.println("Removing META-INF from JAR: " + jarPath);
                try (Stream<Path> walk = Files.walk(metaInfPath)) {
                    walk.sorted(Comparator.reverseOrder())
                        .forEach(path -> {
                            try {
                                Files.delete(path);
                            } catch (IOException e) {
                                System.err.println("Failed to delete " + path + ": " + e.getMessage());
                            }
                        });
                }
                System.out.println("Successfully removed META-INF directory.");
            } else {
                System.out.println("META-INF directory not found in JAR, skipping removal.");
            }
        }
    }

    /**
     * Extracts the embedded lathe-mod.jar from the bootstrap JAR's resources.
     *
     * @param targetDir The directory where the embedded JAR should be extracted.
     * @return The path to the extracted lathe-mod.jar.
     * @throws IOException if the extraction fails.
     */
    public static Path extractEmbeddedLatheMod(Path targetDir) throws IOException {
        String embeddedJarName = "embedded/lathe-mod.jar";
        Path extractedPath = targetDir.resolve("lathe-mod.jar");

        try (InputStream is = JarProcessor.class.getClassLoader().getResourceAsStream(embeddedJarName)) {
            if (is == null) {
                throw new IOException("Embedded resource '" + embeddedJarName + "' not found.");
            }
            Files.copy(is, extractedPath, StandardCopyOption.REPLACE_EXISTING);
            System.out.println("Extracted embedded lathe-mod.jar to: " + extractedPath);
        }
        return extractedPath;
    }

    /**
     * Injects a source JAR into a target JAR. This typically means copying
     * all entries from the source JAR into the target JAR.
     *
     * @param targetJarPath The path to the target JAR file (e.g., temporary Minecraft JAR).
     * @param sourceJarPath The path to the source JAR file (e.g., extracted lathe-mod.jar).
     * @throws IOException if an I/O error occurs during injection.
     */
    public static void injectJar(Path targetJarPath, Path sourceJarPath) throws IOException {
        System.out.println("Injecting " + sourceJarPath.getFileName() + " into " + targetJarPath.getFileName());

        // Use explicitly typed empty maps for Java 8 compatibility
        Map<String, Object> targetEnv = new HashMap<>();
        Map<String, Object> sourceEnv = new HashMap<>();

        try (FileSystem targetFs = FileSystems.newFileSystem(targetJarPath, targetEnv);
             FileSystem sourceFs = FileSystems.newFileSystem(sourceJarPath, sourceEnv)) {

            Path sourceRoot = sourceFs.getPath("/");
            Path targetRoot = targetFs.getPath("/");

            try (Stream<Path> walk = Files.walk(sourceRoot)) {
                for (Path sourceEntry : (Iterable<Path>) walk::iterator) {
                    if (Files.isRegularFile(sourceEntry)) {
                        Path relativePath = sourceRoot.relativize(sourceEntry);
                        Path targetEntry = targetRoot.resolve(relativePath.toString());

                        // Ensure parent directories exist in the target JAR
                        if (targetEntry.getParent() != null) {
                            Files.createDirectories(targetEntry.getParent());
                        }

                        // Copy the file, replacing if it already exists
                        Files.copy(sourceEntry, targetEntry, StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            }
            System.out.println("Successfully injected " + sourceJarPath.getFileName() + ".");
        }
    }

    /**
     * Applies a list of legacy JAR mods to the temporary Minecraft JAR.
     * This involves injecting each legacy mod JAR into the temporary Minecraft JAR.
     *
     * @param context The bootstrap context.
     * @param legacyMods A list of LegacyModInfo objects representing the mods to apply.
     * @throws IOException if an I/O error occurs during application.
     */
    public static void applyLegacyMods(BootstrapContext context, List<LegacyModInfo> legacyMods) throws IOException {
        System.out.println("Applying legacy JAR mods to " + context.getTempMinecraftJar().getFileName());
        for (LegacyModInfo modInfo : legacyMods) {
            System.out.println("  - Applying: " + modInfo.getArchivePath().getFileName());
            injectModFiles(context.getTempMinecraftJar(), modInfo);
        }
        System.out.println("Finished applying legacy JAR mods.");
    }

    /**
     * Injects specific files from a mod archive into a target JAR.
     *
     * @param targetJarPath The path to the target JAR file (e.g., temporary Minecraft JAR).
     * @param modInfo The LegacyModInfo object containing details about the mod and files to inject.
     * @throws IOException if an I/O error occurs during injection.
     */
    private static void injectModFiles(Path targetJarPath, LegacyModInfo modInfo) throws IOException {
        // Use an explicitly typed empty map for Java 8 compatibility
        Map<String, Object> targetEnv = new HashMap<>();
        try (FileSystem targetFs = FileSystems.newFileSystem(targetJarPath, targetEnv);
             ZipFile sourceZipFile = new ZipFile(modInfo.getArchivePath().toFile())) {

            Path targetRoot = targetFs.getPath("/");

            for (String fileToInject : modInfo.getFilesToInject()) {
                ZipEntry entry = sourceZipFile.getEntry(modInfo.getInstallationRootInArchive() + fileToInject);
                if (entry == null) {
                    System.err.println("Warning: File '" + fileToInject + "' not found in mod archive '" + modInfo.getArchivePath().getFileName() + "' at expected path '" + modInfo.getInstallationRootInArchive() + fileToInject + "'. Skipping.");
                    continue;
                }

                Path targetEntryPath = targetRoot.resolve(fileToInject);

                // Ensure parent directories exist in the target JAR
                if (targetEntryPath.getParent() != null) {
                    Files.createDirectories(targetEntryPath.getParent());
                }

                try (InputStream is = sourceZipFile.getInputStream(entry)) {
                    Files.copy(is, targetEntryPath, StandardCopyOption.REPLACE_EXISTING);
                }
            }
            System.out.println("Successfully injected files from " + modInfo.getArchivePath().getFileName() + ".");
        }
    }
}

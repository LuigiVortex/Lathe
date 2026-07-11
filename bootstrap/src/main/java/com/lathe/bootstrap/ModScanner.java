package com.lathe.bootstrap;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ModScanner {

    // Known package roots to help identify installation roots in nested ZIPs
    private static final String[] KNOWN_PACKAGE_ROOTS = {"net/", "com/", "paulscode/", "org/"};

    /**
     * Scans for legacy JAR/ZIP mods in the mods directory specified in the context.
     * Analyzes each archive to determine its type and installation root.
     *
     * @param context The bootstrap context containing necessary paths and information, including the mods directory.
     * @return A list of processed LegacyModInfo objects.
     * @throws IOException if an I/O error occurs during scanning or archive analysis.
     */
    public static List<LegacyModInfo> scanForLegacyMods(BootstrapContext context) throws IOException {
        System.out.println("Scanning for legacy JAR/ZIP mods in: " + context.getModsDirectory());
        Path modsDirectory = context.getModsDirectory();
        List<LegacyModInfo> legacyMods = new ArrayList<>();

        if (Files.exists(modsDirectory) && Files.isDirectory(modsDirectory)) {
            try (Stream<Path> paths = Files.list(modsDirectory)) {
                for (Path modPath : paths.filter(Files::isRegularFile).collect(Collectors.toList())) {
                    String fileName = modPath.getFileName().toString().toLowerCase();
                    if (fileName.endsWith(".jar")) {
                        System.out.println("  - Processing JAR mod: " + modPath.getFileName());
                        legacyMods.add(processJarArchive(modPath));
                    } else if (fileName.endsWith(".zip")) {
                        System.out.println("  - Processing ZIP mod: " + modPath.getFileName());
                        legacyMods.add(processZipArchive(modPath));
                    } else {
                        System.out.println("  - Skipping unknown file type: " + modPath.getFileName());
                    }
                }
            }
            System.out.println("Found and processed " + legacyMods.size() + " legacy mods.");
        } else {
            System.out.println("Mods directory does not exist or is not a directory: " + modsDirectory);
        }
        return legacyMods;
    }

    /**
     * Processes a JAR archive, treating all its contents as files to be injected.
     *
     * @param jarPath The path to the JAR file.
     * @return A LegacyModInfo object representing the JAR mod.
     * @throws IOException if an I/O error occurs during processing.
     */
    private static LegacyModInfo processJarArchive(Path jarPath) throws IOException {
        LegacyModInfo modInfo = new LegacyModInfo(jarPath, LegacyModInfo.ModType.JAR, "");
        try (ZipFile zipFile = new ZipFile(jarPath.toFile())) {
            zipFile.stream()
                   .filter(entry -> !entry.isDirectory())
                   .forEach(entry -> modInfo.addFileToInject(entry.getName()));
        }
        return modInfo;
    }

    /**
     * Processes a ZIP archive, determining if it's a root layout or nested layout.
     *
     * @param zipPath The path to the ZIP file.
     * @return A LegacyModInfo object representing the ZIP mod.
     * @throws IOException if an I/O error occurs during processing.
     */
    private static LegacyModInfo processZipArchive(Path zipPath) throws IOException {
        String installationRoot = "";
        LegacyModInfo.ModType modType = LegacyModInfo.ModType.ZIP_ROOT_LAYOUT; // Assume root layout initially
        boolean foundClassFile = false;

        try (ZipFile zipFile = new ZipFile(zipPath.toFile())) {
            List<String> classFiles = new ArrayList<>();
            for (ZipEntry entry : zipFile.stream().filter(entry -> !entry.isDirectory()).collect(Collectors.toList())) {
                String entryName = entry.getName();
                if (entryName.toLowerCase().endsWith(".class")) {
                    classFiles.add(entryName);
                    foundClassFile = true;
                }
            }

            if (!foundClassFile) {
                // If no class files, assume root layout and inject everything (e.g., resource packs)
                // Or, we might want to skip it if it's not a mod. For now, treat as root.
                System.out.println("    - No class files found in ZIP, treating as root layout for all files.");
                LegacyModInfo modInfo = new LegacyModInfo(zipPath, LegacyModInfo.ModType.ZIP_ROOT_LAYOUT, "");
                zipFile.stream()
                       .filter(entry -> !entry.isDirectory())
                       .forEach(entry -> modInfo.addFileToInject(entry.getName()));
                return modInfo;
            }

            // Check for root layout (class files directly in root or known package roots)
            boolean isRootLayout = false;
            for (String classFile : classFiles) {
                if (!classFile.contains("/")) { // Class file directly in root
                    isRootLayout = true;
                    break;
                }
                for (String packageRoot : KNOWN_PACKAGE_ROOTS) {
                    if (classFile.startsWith(packageRoot)) { // Class file in known package root
                        isRootLayout = true;
                        break;
                    }
                }
                if (isRootLayout) break;
            }

            if (isRootLayout) {
                modType = LegacyModInfo.ModType.ZIP_ROOT_LAYOUT;
                System.out.println("    - Detected ZIP Root Layout.");
            } else {
                // Nested layout: find the common installation root
                modType = LegacyModInfo.ModType.ZIP_NESTED_LAYOUT;
                System.out.println("    - Detected ZIP Nested Layout, determining installation root.");

                // Find the longest common prefix that is NOT part of a package structure
                String firstClassFile = classFiles.get(0); // Use the first class file found
                int lastSlash = firstClassFile.lastIndexOf('/');
                if (lastSlash != -1) {
                    String potentialRoot = firstClassFile.substring(0, lastSlash + 1); // e.g., "bin/net/minecraft/client/"
                    for (String packageRoot : KNOWN_PACKAGE_ROOTS) {
                        int packageRootIndex = potentialRoot.indexOf(packageRoot);
                        if (packageRootIndex != -1) {
                            installationRoot = potentialRoot.substring(0, packageRootIndex);
                            break;
                        }
                    }
                    if (installationRoot.isEmpty() && !potentialRoot.isEmpty()) {
                        // If no known package root found, but it's nested, assume the first segment is the root
                        // This is a heuristic and might need refinement.
                        installationRoot = potentialRoot.substring(0, potentialRoot.indexOf('/') + 1);
                    }
                }
                System.out.println("    - Determined installation root: '" + installationRoot + "'");
            }

            LegacyModInfo modInfo = new LegacyModInfo(zipPath, modType, installationRoot);
            for (ZipEntry entry : zipFile.stream().filter(entry -> !entry.isDirectory()).collect(Collectors.toList())) {
                String entryName = entry.getName();
                if (entryName.startsWith(installationRoot)) {
                    modInfo.addFileToInject(entryName.substring(installationRoot.length()));
                }
            }
            return modInfo;

        }
    }

    /**
     * Processes Lathe API mods, which might involve loading their metadata,
     * resolving dependencies, and preparing them for injection.
     * This is a placeholder.
     *
     * @param context The bootstrap context.
     * @throws IOException if an I/O error occurs during processing.
     */
    public static void processApiMods(BootstrapContext context) throws IOException {
        System.out.println("Processing Lathe API mods (placeholder)...");
        // TODO: Implement logic to process Lathe API mods.
        // This will likely involve reading metadata, checking compatibility, etc.
    }
}

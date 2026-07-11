package com.lathe.bootstrap;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class MinecraftLocator {

    /**
     * Determines the home directory for Lathe, which is the directory containing the executing lathe.jar.
     * This is typically .minecraft/lathe/
     *
     * @return The Path to the Lathe home directory.
     */
    public static Path getLatheHomeDirectory() {
        // Get the path of the currently executing JAR.
        // For a JAR launched with `java -jar lathe.jar`, System.getProperty("user.dir")
        // should point to the directory containing lathe.jar.
        Path currentDir = Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize();
        System.out.println("Lathe home directory detected: " + currentDir);
        return currentDir;
    }

    /**
     * Locates the selected Minecraft version JAR file within the Lathe home directory.
     * This method assumes the version to load is passed as an argument or determined otherwise.
     * For now, it will look for "b1.7.3.jar" as a placeholder.
     *
     * @param latheHomeDir The Path to the Lathe home directory.
     * @param minecraftVersionJarName The filename of the Minecraft version JAR (e.g., "b1.7.3.jar").
     * @return The Path to the original Minecraft version JAR.
     * @throws RuntimeException if the Minecraft JAR cannot be located.
     */
    public static Path locateMinecraftVersionJar(Path latheHomeDir, String minecraftVersionJarName) {
        Path minecraftJarPath = latheHomeDir.resolve(minecraftVersionJarName);

        if (Files.exists(minecraftJarPath) && Files.isRegularFile(minecraftJarPath)) {
            System.out.println("Original Minecraft version JAR located: " + minecraftJarPath);
            return minecraftJarPath;
        } else {
            throw new RuntimeException("Minecraft version JAR '" + minecraftVersionJarName + "' not found in: " + latheHomeDir);
        }
    }

    // The locateModsDirectory method is no longer needed here as it's handled by BootstrapContext
    // and the path is derived from latheHomeDir.
}

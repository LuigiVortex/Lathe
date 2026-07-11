package com.lathe.bootstrap;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MinecraftLauncher {

    /**
     * Launches the modified Minecraft JAR with the correct classpath and native library path.
     *
     * @param context The bootstrap context containing paths to libraries, natives, and the modified Minecraft JAR.
     * @param minecraftVersionJarName The filename of the Minecraft version JAR (e.g., "b1.7.3.jar").
     * @throws IOException If an I/O error occurs during process creation or classpath building.
     * @throws InterruptedException If the current thread is interrupted while waiting for the process.
     */
    public static void launch(BootstrapContext context, String minecraftVersionJarName) throws IOException, InterruptedException {
        Path modifiedMinecraftJar = context.getTempMinecraftJar();
        Path librariesDir = context.getLibrariesDirectory();
        Path nativesDir = context.getNativesDirectory();

        System.out.println("Attempting to launch modified Minecraft JAR: " + modifiedMinecraftJar);
        System.out.println("Using libraries from: " + librariesDir);
        System.out.println("Using natives from: " + nativesDir);

        // 1. Build the classpath
        List<Path> classpathEntries = new ArrayList<>();
        classpathEntries.add(modifiedMinecraftJar); // The modified Minecraft JAR itself

        if (Files.exists(librariesDir) && Files.isDirectory(librariesDir)) {
            try (Stream<Path> paths = Files.walk(librariesDir)) {
                paths.filter(Files::isRegularFile)
                     .filter(path -> path.toString().toLowerCase().endsWith(".jar"))
                     .forEach(classpathEntries::add);
            }
        } else {
            System.out.println("Libraries directory not found or is not a directory: " + librariesDir);
        }

        String classpath = classpathEntries.stream()
                                           .map(Path::toAbsolutePath)
                                           .map(Path::toString)
                                           .collect(Collectors.joining(System.getProperty("path.separator")));

        // 2. Construct the launch command
        List<String> command = new ArrayList<>();
        command.add(System.getProperty("java.home") + "/bin/java"); // Path to Java executable

        // Add JVM arguments for native library path
        if (Files.exists(nativesDir) && Files.isDirectory(nativesDir)) {
            command.add("-Djava.library.path=" + nativesDir.toAbsolutePath().toString());
        } else {
            System.out.println("Natives directory not found or is not a directory: " + nativesDir);
        }

        // Add classpath
        command.add("-cp");
        command.add(classpath);

        // Specify the main class for Minecraft Beta 1.7.3
        // TODO: This main class might vary by Minecraft version and should be determined dynamically.
        command.add("net.minecraft.client.Minecraft");

        // TODO: Pass any additional arguments from the initial launch command to Minecraft.
        // For example, if the user launched with `java -Xmx2G -jar lathe.jar --username Player`,
        // we'd need to parse and forward `--username Player` to Minecraft.

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.inheritIO(); // Redirects subprocess's stdout/stderr to parent's
        System.out.println("Executing command: " + String.join(" ", command));

        Process process = processBuilder.start();
        int exitCode = process.waitFor(); // Wait for Minecraft process to exit
        System.out.println("Minecraft process exited with code: " + exitCode);

        if (exitCode != 0) {
            System.err.println("Minecraft launch failed or exited abnormally.");
        }
    }
}

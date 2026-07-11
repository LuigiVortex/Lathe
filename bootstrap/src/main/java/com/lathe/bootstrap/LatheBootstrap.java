package com.lathe.bootstrap;

import java.nio.file.Path;
import java.util.List;

public class LatheBootstrap {

    public static void main(String[] args) {
        System.out.println("Lathe Bootstrap starting...");

        try {
            // 1. Determine Lathe Home Directory
            Path latheHomeDir = MinecraftLocator.getLatheHomeDirectory();

            // 2. Initialize Bootstrap Context with Lathe Home Directory
            BootstrapContext context = new BootstrapContext(latheHomeDir);

            // 3. Locate selected Minecraft version JAR (e.g., b1.7.3.jar)
            // TODO: The actual version name should come from launch arguments or a config file.
            // For now, hardcode "b1.7.3.jar" for testing.
            String minecraftVersionJarName = "b1.7.3.jar";
            Path originalMinecraftJar = MinecraftLocator.locateMinecraftVersionJar(latheHomeDir, minecraftVersionJarName);
            context.setOriginalMinecraftJar(originalMinecraftJar);
            System.out.println("Original Minecraft JAR located: " + originalMinecraftJar);

            // 4. Create temporary runtime location (latheHomeDir/temp) and copy Minecraft JAR
            // Ensure the temp directory exists
            JarProcessor.createDirectory(context.getTempRuntimeDirectory());
            Path tempMinecraftJar = JarProcessor.copyMinecraftJarToTemp(originalMinecraftJar, context.getTempRuntimeDirectory());
            context.setTempMinecraftJar(tempMinecraftJar);
            System.out.println("Temporary Minecraft JAR created: " + tempMinecraftJar);

            // 5. Remove META-INF from the temporary copy
            JarProcessor.removeMetaInf(tempMinecraftJar);
            System.out.println("META-INF removed from temporary JAR.");

            // 6. Extract embedded lathe-mod.jar from the bootstrap into the temp directory
            Path embeddedLatheModJar = JarProcessor.extractEmbeddedLatheMod(context.getTempRuntimeDirectory());
            context.setEmbeddedLatheModJar(embeddedLatheModJar);
            System.out.println("Embedded Lathe Mod extracted: " + embeddedLatheModJar);

            // 7. Inject Lathe Mod into the temporary Minecraft JAR
            JarProcessor.injectJar(tempMinecraftJar, embeddedLatheModJar);
            System.out.println("Lathe Mod injected into temporary Minecraft JAR.");

            // 8. Scan ./mods/ for legacy jar mods and Lathe API mods
            List<LegacyModInfo> legacyMods = ModScanner.scanForLegacyMods(context); // Changed type to List<LegacyModInfo>
            JarProcessor.applyLegacyMods(context, legacyMods); // Pass the list of LegacyModInfo
            System.out.println("Legacy JAR mods applied.");

            // 9. Process Lathe API mods
            ModScanner.processApiMods(context);
            System.out.println("Lathe API mods processed.");

            // 10. Perform compatibility checks
            CompatibilityChecker.performChecks(context);
            System.out.println("Compatibility checks completed.");

            // 11. Launch the temporary modified Minecraft JAR
            // TODO: Pass launch arguments (JVM args, game args) from the initial 'java -Xmx2G -jar lathe.jar' command
            MinecraftLauncher.launch(context, minecraftVersionJarName); // Pass context and version name for classpath building
            System.out.println("Modified Minecraft JAR launched.");

            // TODO: Add a shutdown hook to clean up temporary files when Minecraft exits.

        } catch (Exception e) {
            System.err.println("Lathe Bootstrap failed: " + e.getMessage());
            e.printStackTrace();
            // TODO: Display a user-friendly error interface
        }
    }
}

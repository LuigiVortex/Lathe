package com.lathe.bootstrap;

import java.nio.file.Path;

public class BootstrapContext {
    private Path latheHomeDir;
    private Path originalMinecraftJar; // This will be e.g., b1.7.3.jar inside latheHomeDir
    private Path tempMinecraftJar;     // This will be in latheHomeDir/temp
    private Path embeddedLatheModJar;
    private Path modsDirectory;        // This will be latheHomeDir/mods
    private Path librariesDirectory;   // This will be latheHomeDir/libraries
    private Path nativesDirectory;     // This will be latheHomeDir/natives
    private Path tempRuntimeDirectory; // This will be latheHomeDir/temp

    // Constructor to initialize with the base latheHomeDir
    public BootstrapContext(Path latheHomeDir) {
        this.latheHomeDir = latheHomeDir;
        this.modsDirectory = latheHomeDir.resolve("mods");
        this.librariesDirectory = latheHomeDir.resolve("libraries");
        this.nativesDirectory = latheHomeDir.resolve("natives");
        this.tempRuntimeDirectory = latheHomeDir.resolve("temp");
    }

    public Path getLatheHomeDir() {
        return latheHomeDir;
    }

    public Path getOriginalMinecraftJar() {
        return originalMinecraftJar;
    }

    public void setOriginalMinecraftJar(Path originalMinecraftJar) {
        this.originalMinecraftJar = originalMinecraftJar;
    }

    public Path getTempMinecraftJar() {
        return tempMinecraftJar;
    }

    public void setTempMinecraftJar(Path tempMinecraftJar) {
        this.tempMinecraftJar = tempMinecraftJar;
    }

    public Path getEmbeddedLatheModJar() {
        return embeddedLatheModJar;
    }

    public void setEmbeddedLatheModJar(Path embeddedLatheModJar) {
        this.embeddedLatheModJar = embeddedLatheModJar;
    }

    public Path getModsDirectory() {
        return modsDirectory;
    }

    // No setter for modsDirectory as it's derived from latheHomeDir

    public Path getLibrariesDirectory() {
        return librariesDirectory;
    }

    // No setter for librariesDirectory as it's derived from latheHomeDir

    public Path getNativesDirectory() {
        return nativesDirectory;
    }

    // No setter for nativesDirectory as it's derived from latheHomeDir

    public Path getTempRuntimeDirectory() {
        return tempRuntimeDirectory;
    }

    // No setter for tempRuntimeDirectory as it's derived from latheHomeDir
}

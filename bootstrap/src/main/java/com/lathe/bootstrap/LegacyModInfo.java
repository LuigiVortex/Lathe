package com.lathe.bootstrap;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

public class LegacyModInfo {

    public enum ModType {
        JAR,
        ZIP_ROOT_LAYOUT,
        ZIP_NESTED_LAYOUT
    }

    private final Path archivePath;
    private final ModType type;
    private final String installationRootInArchive; // e.g., "bin/", "minecraft/", or "" for root layout
    private final Set<String> filesToInject; // Relative paths within the archive that should be injected
    private final Set<String> classesToReplace; // Full class names (e.g., "net/minecraft/client/Minecraft.class")

    public LegacyModInfo(Path archivePath, ModType type, String installationRootInArchive) {
        this.archivePath = archivePath;
        this.type = type;
        this.installationRootInArchive = installationRootInArchive;
        this.filesToInject = new HashSet<>();
        this.classesToReplace = new HashSet<>();
    }

    public Path getArchivePath() {
        return archivePath;
    }

    public ModType getType() {
        return type;
    }

    public String getInstallationRootInArchive() {
        return installationRootInArchive;
    }

    public Set<String> getFilesToInject() {
        return filesToInject;
    }

    public Set<String> getClassesToReplace() {
        return classesToReplace;
    }

    public void addFileToInject(String relativePath) {
        this.filesToInject.add(relativePath);
        if (relativePath.endsWith(".class")) {
            this.classesToReplace.add(relativePath);
        }
    }

    @Override
    public String toString() {
        return "LegacyModInfo{" +
               "archivePath=" + archivePath.getFileName() +
               ", type=" + type +
               ", installationRoot='" + installationRootInArchive + '\'' +
               ", filesToInjectCount=" + filesToInject.size() +
               ", classesToReplaceCount=" + classesToReplace.size() +
               '}';
    }
}

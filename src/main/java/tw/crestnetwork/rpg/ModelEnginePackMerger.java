package tw.crestnetwork.rpg;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.stream.Stream;

final class ModelEnginePackMerger {
    boolean merge(Path modelEnginePack, Path oraxenPack, String minecraftVersion) throws IOException {
        Path targetAssets = oraxenPack.resolve("assets");
        boolean changed = copyTree(modelEnginePack.resolve("assets"), targetAssets);

        String overlayName = "modelengine_" + minecraftVersion.replace('.', '_').replace('-', '_');
        Path overlayAssets = modelEnginePack.resolve(overlayName).resolve("assets");
        return copyTree(overlayAssets, targetAssets) || changed;
    }

    private boolean copyTree(Path sourceRoot, Path targetRoot) throws IOException {
        if (!Files.isDirectory(sourceRoot)) return false;

        boolean changed = false;
        try (Stream<Path> paths = Files.walk(sourceRoot)) {
            for (Path source : paths.filter(Files::isRegularFile).toList()) {
                Path relative = sourceRoot.relativize(source);
                Path target = targetRoot.resolve(relative).normalize();
                if (!target.startsWith(targetRoot.normalize())) throw new IOException("資源包路徑超出目標目錄");
                if (sameContent(source, target)) continue;

                Files.createDirectories(target.getParent());
                Path temporary = target.resolveSibling(target.getFileName() + ".crest-rpg.tmp");
                Files.copy(source, temporary, StandardCopyOption.REPLACE_EXISTING);
                try {
                    Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                } catch (java.nio.file.AtomicMoveNotSupportedException ignored) {
                    Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
                }
                changed = true;
            }
        }
        return changed;
    }

    private boolean sameContent(Path source, Path target) throws IOException {
        if (!Files.isRegularFile(target) || Files.size(source) != Files.size(target)) return false;
        return Files.mismatch(source, target) == -1;
    }
}

package com.nodetracker;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Files kept under the mod's old id, "nodetracker", before it became "nodeatlas". */
public final class LegacyFiles {
    public static final String OLD = "nodetracker";

    private LegacyFiles() {
    }

    /** Copies {@code old} to {@code now} when only the old one exists. True when it copied; the old file stays. */
    public static boolean adopt(Path old, Path now) throws IOException {
        if (Files.exists(now) || !Files.isRegularFile(old)) {
            return false;
        }
        if (now.getParent() != null) {
            Files.createDirectories(now.getParent());
        }
        Files.copy(old, now);
        return true;
    }

    /** {@link #adopt} that logs instead of throwing, for use at load and login. */
    static void adoptQuietly(Path old, Path now) {
        try {
            if (adopt(old, now)) {
                NodeTracker.LOG.info("Copied {} to {}", old, now);
            }
        } catch (IOException e) {
            NodeTracker.LOG.warn("Could not copy {} to {}", old, now, e);
        }
    }
}

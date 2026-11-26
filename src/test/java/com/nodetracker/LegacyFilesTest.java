package com.nodetracker;

import java.nio.file.Files;
import java.nio.file.Path;

/** Plain check of the old-folder migration: java -cp build/classes/java/main src/test/java/com/nodetracker/LegacyFilesTest.java */
public final class LegacyFilesTest {
    public static void main(String[] args) throws Exception {
        Path game = Files.createTempDirectory("nodeatlas-test");
        Path old = game.resolve("nodetracker").resolve("My World").resolve("nodes.json");
        Path now = game.resolve("nodeatlas").resolve("My World").resolve("nodes.json");
        Files.createDirectories(old.getParent());
        Files.writeString(old, "[{\"x\":1,\"y\":64,\"z\":2,\"type\":\"pure\"}]");
        check("copies when only the old file exists", LegacyFiles.adopt(old, now));
        check("new file has the old contents", Files.readString(now).equals(Files.readString(old)));
        check("old file is kept", Files.exists(old));
        Files.writeString(now, "[]");
        check("never overwrites an existing new file", !LegacyFiles.adopt(old, now) && Files.readString(now).equals("[]"));
        check("nothing to do without an old file", !LegacyFiles.adopt(game.resolve("missing.json"), game.resolve("other.json"))
                && !Files.exists(game.resolve("other.json")));
        Path oldCfg = game.resolve("nodetracker-client.toml");
        Path newCfg = game.resolve("nodeatlas-client.toml");
        Files.writeString(oldCfg, "arrowSize = 3\n");
        check("config copied", LegacyFiles.adopt(oldCfg, newCfg) && Files.readString(newCfg).equals("arrowSize = 3\n"));
        System.out.println("ALL PASS");
    }

    private static void check(String name, boolean ok) {
        System.out.println((ok ? "PASS " : "FAIL ") + name);
        if (!ok) {
            System.exit(1);
        }
    }
}

package com.nodetracker;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;

/**
 * The scanned nodes of the world or server the player is in, kept in {@code nodeatlas/<world>/nodes.json} under the
 * game directory (the original's per-world folder), and the node the arrow points to.
 */
public final class NodeLog {
    private static final List<TrackedNode> NODES = new ArrayList<>();
    private static Path file;
    private static TrackedNode marked;
    private static int version;

    private NodeLog() {
    }

    public static List<TrackedNode> nodes() {
        return NODES;
    }

    public static TrackedNode marked() {
        return marked;
    }

    /** Goes up on every change to the log or the mark, so map markers know when to redraw. */
    public static int version() {
        return version;
    }

    public static void mark(TrackedNode node) {
        marked = node;
        version++;
    }

    static void open(Path path) {
        close();
        file = path;
        version++;
        if (!Files.exists(path)) {
            return;
        }
        try {
            var json = JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8));
            TrackedNode.CODEC.listOf().parse(JsonOps.INSTANCE, json)
                    .resultOrPartial(e -> NodeTracker.LOG.warn("Could not read {}: {}", path, e))
                    .ifPresent(NODES::addAll);
        } catch (IOException | RuntimeException e) {
            NodeTracker.LOG.error("Could not read {}", path, e);
        }
    }

    static void close() {
        NODES.clear();
        marked = null;
        file = null;
        version++;
    }

    /** Adds a node, or updates the one already logged at that spot. */
    static void record(TrackedNode node) {
        NODES.removeIf(n -> n.sameSpot(node.dimension(), node.pos()));
        NODES.add(node);
        if (marked != null && marked.sameSpot(node.dimension(), node.pos())) {
            marked = node;
        }
        save();
    }

    public static void remove(TrackedNode node) {
        NODES.removeIf(n -> n.sameSpot(node.dimension(), node.pos()));
        if (marked != null && marked.sameSpot(node.dimension(), node.pos())) {
            marked = null;
        }
        save();
    }

    public static boolean has(Identifier dim, BlockPos pos) {
        return NODES.stream().anyMatch(n -> n.sameSpot(dim, pos));
    }

    private static void save() {
        version++;
        if (file == null) {
            return;
        }
        TrackedNode.CODEC.listOf().encodeStart(JsonOps.INSTANCE, NODES).resultOrPartial(e -> NodeTracker.LOG.warn("Could not save nodes: {}", e))
                .ifPresent(json -> {
                    try {
                        Files.createDirectories(file.getParent());
                        Files.writeString(file, new GsonBuilder().setPrettyPrinting().create().toJson(json), StandardCharsets.UTF_8);
                    } catch (IOException e) {
                        NodeTracker.LOG.error("Could not write {}", file, e);
                    }
                });
    }
}

package com.nodetracker.map;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nodetracker.NodeLog;
import com.nodetracker.NodeTracker;
import com.nodetracker.TrackedNode;
import java.io.Reader;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.resources.ResourceManager;

/**
 * How logged nodes look on map mods, read from {@code assets/nodeatlas/map_markers.json} so resource packs can change
 * colours, shapes and sizes per node type. Markers are drawn from these numbers, with no texture.
 */
public final class MarkerStyle {
    public record Style(int color, int outline, String shape, int size, int xaeroColor) {
        /** ARGB pixels, row by row, size x size; 0 is clear. */
        public int[] pixels() {
            int[] out = new int[size * size];
            double c = (size - 1) / 2.0;
            for (int y = 0; y < size; y++) {
                for (int x = 0; x < size; x++) {
                    double dx = Math.abs(x - c);
                    double dy = Math.abs(y - c);
                    double d = switch (shape) {
                        case "circle" -> Math.sqrt(dx * dx + dy * dy);
                        case "square" -> Math.max(dx, dy);
                        default -> dx + dy;
                    };
                    if (d <= c + 0.5) {
                        out[y * size + x] = d > c - 0.6 ? outline : color;
                    }
                }
            }
            return out;
        }
    }

    private static final Style FALLBACK = new Style(0xFF9A6CFF, 0xFF1A0B2E, "diamond", 9, 13);
    private static Style base = FALLBACK;
    private static Style marked = new Style(0xFF55FFFF, 0xFF0B2E2E, "diamond", 11, 11);
    private static final Map<String, Style> TYPES = new HashMap<>();
    private static int version;

    private MarkerStyle() {
    }

    public static int version() {
        return version;
    }

    public static boolean isMarked(TrackedNode node) {
        TrackedNode m = NodeLog.marked();
        return m != null && m.sameSpot(node.dimension(), node.pos());
    }

    public static Style of(TrackedNode node) {
        return isMarked(node) ? marked : TYPES.getOrDefault(node.type(), base);
    }

    public static Component label(TrackedNode node) {
        Component type = Component.translatable("nodeatlas.type." + node.type());
        if (node.modifier().isPresent()) {
            type = Component.translatable("nodeatlas.type_modified", Component.translatable("nodeatlas.modifier." + node.modifier().get()), type);
        }
        return Component.translatable("nodeatlas.map.node", type);
    }

    public static String coords(TrackedNode node) {
        return node.pos().getX() + " " + node.pos().getY() + " " + node.pos().getZ();
    }

    static void reload(ResourceManager resources) {
        Style newBase = FALLBACK;
        Style newMarked = marked;
        Map<String, Style> types = new HashMap<>();
        var found = resources.getResource(NodeTracker.id("map_markers.json"));
        if (found.isPresent()) {
            try (Reader reader = found.get().openAsReader()) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                newBase = read(root.getAsJsonObject("default"), FALLBACK);
                newMarked = read(root.getAsJsonObject("marked"), newBase);
                JsonObject t = root.getAsJsonObject("types");
                if (t != null) {
                    for (var e : t.entrySet()) {
                        types.put(e.getKey(), read(e.getValue().getAsJsonObject(), newBase));
                    }
                }
            } catch (Exception e) {
                NodeTracker.LOG.warn("Could not read nodeatlas map_markers.json, using the built-in look", e);
            }
        }
        base = newBase;
        marked = newMarked;
        TYPES.clear();
        TYPES.putAll(types);
        version++;
    }

    private static Style read(JsonObject o, Style parent) {
        if (o == null) {
            return parent;
        }
        int size = o.has("size") ? Math.max(3, Math.min(32, o.get("size").getAsInt())) : parent.size();
        String shape = o.has("shape") ? o.get("shape").getAsString().toLowerCase(Locale.ROOT) : parent.shape();
        int xaero = o.has("xaero_color") ? Math.max(0, o.get("xaero_color").getAsInt()) : parent.xaeroColor();
        return new Style(colour(o.get("color"), parent.color()), colour(o.get("outline"), parent.outline()), shape, size, xaero);
    }

    private static int colour(JsonElement e, int fallback) {
        if (e == null) {
            return fallback;
        }
        String s = e.getAsString().trim();
        if (s.startsWith("#")) {
            s = s.substring(1);
        }
        long v = Long.parseLong(s, 16);
        return s.length() <= 6 ? (int) (0xFF000000L | v) : (int) v;
    }
}

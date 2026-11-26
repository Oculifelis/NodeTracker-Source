package com.nodetracker;

import net.neoforged.neoforge.common.ModConfigSpec;

/** The original's config (arrow offset from top centre, size, plain red arrow) and which map mods show logged nodes. */
public final class NodeConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.IntValue ARROW_X;
    public static final ModConfigSpec.IntValue ARROW_Y;
    public static final ModConfigSpec.DoubleValue ARROW_SIZE;
    public static final ModConfigSpec.BooleanValue ALT_ARROW;
    public static final ModConfigSpec.BooleanValue JOURNEYMAP;
    public static final ModConfigSpec.BooleanValue XAERO;
    public static final ModConfigSpec.BooleanValue FTB_CHUNKS;
    public static final ModConfigSpec.BooleanValue MAP_LABELS;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        ARROW_X = b.comment("Arrow offset from the top centre of the screen, in GUI pixels").defineInRange("arrowX", 0, -2000, 2000);
        ARROW_Y = b.defineInRange("arrowY", 0, -2000, 2000);
        ARROW_SIZE = b.comment("Arrow size, 1 = 64 GUI pixels").defineInRange("arrowSize", 1.0, 0.1, 2.0);
        ALT_ARROW = b.comment("Use the plain red arrow").define("altArrow", false);
        JOURNEYMAP = b.comment("Show logged nodes on JourneyMap").define("journeymap", true);
        XAERO = b.comment("Show logged nodes as temporary waypoints on Xaero's Minimap and World Map").define("xaero", true);
        FTB_CHUNKS = b.comment("Show logged nodes on the FTB Chunks map and minimap").define("ftbChunks", true);
        MAP_LABELS = b.comment("Write the node type under each JourneyMap marker").define("mapLabels", false);
        SPEC = b.build();
    }

    private NodeConfig() {
    }
}

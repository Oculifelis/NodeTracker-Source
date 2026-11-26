package com.nodetracker.map;

import com.nodetracker.NodeTracker;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;

/**
 * Optional map mod support. Each integration class touches its map mod's classes, so it is only loaded when that mod
 * is present. JourneyMap finds {@link JourneyMapMarkers} by its plugin annotation by itself.
 */
public final class MapMarkers {
    static volatile String journeyMap = "not loaded";
    static volatile String xaero = "not loaded";
    static volatile String ftbChunks = "not loaded";

    private MapMarkers() {
    }

    /** One log line with what each map was last given, for the dev check. */
    public static String report() {
        return "journeymap=" + journeyMap + ", xaero=" + xaero + ", ftbchunks=" + ftbChunks;
    }

    public static void register(IEventBus modBus) {
        modBus.addListener((AddClientReloadListenersEvent e) ->
                e.addListener(NodeTracker.id("map_markers"), (ResourceManagerReloadListener) MarkerStyle::reload));
        ModList mods = ModList.get();
        if (mods.isLoaded("xaerominimap") || mods.isLoaded("xaerobetterpvp")) {
            XaeroMarkers.register();
        }
        if (mods.isLoaded("ftbchunks")) {
            FtbChunksMarkers.register();
        }
    }
}

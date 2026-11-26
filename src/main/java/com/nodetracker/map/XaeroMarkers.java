package com.nodetracker.map;

import com.nodetracker.NodeConfig;
import com.nodetracker.NodeLog;
import com.nodetracker.TrackedNode;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import xaero.common.minimap.waypoints.Waypoint;
import xaero.hud.minimap.BuiltInHudModules;
import xaero.hud.minimap.module.MinimapSession;
import xaero.hud.minimap.waypoint.WaypointColor;
import xaero.hud.minimap.waypoint.set.WaypointSet;
import xaero.hud.minimap.world.MinimapWorld;

/**
 * Logged nodes of the current dimension as temporary waypoints in Xaero's Minimap's current waypoint set. Temporary
 * waypoints are never saved, so nothing is left behind in Xaero's files. Xaero's World Map shows them too.
 */
final class XaeroMarkers {
    private static final List<Waypoint> OURS = new ArrayList<>();
    private static WaypointSet set;
    private static Object shown;

    private XaeroMarkers() {
    }

    static void register() {
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> tick());
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> forget());
    }

    private static void forget() {
        OURS.clear();
        set = null;
        shown = null;
    }

    private static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            forget();
            return;
        }
        MinimapSession session = BuiltInHudModules.MINIMAP.getCurrentSession();
        if (session == null) {
            return;
        }
        MinimapWorld world = session.getWorldManager().getCurrentWorld();
        WaypointSet current = world == null ? null : world.getCurrentWaypointSet();
        if (current == null) {
            return;
        }
        var dim = mc.level.dimension().location();
        Object key = List.of(NodeLog.version(), MarkerStyle.version(), NodeConfig.XAERO.get(), dim);
        if (current == set && key.equals(shown)) {
            return;
        }
        if (set != null) {
            set.removeAll(OURS);
        }
        OURS.clear();
        set = current;
        shown = key;
        if (!NodeConfig.XAERO.get()) {
            return;
        }
        int colours = WaypointColor.values().length;
        for (TrackedNode node : NodeLog.nodes()) {
            if (!node.dimension().equals(dim)) {
                continue;
            }
            MarkerStyle.Style style = MarkerStyle.of(node);
            String name = MarkerStyle.label(node).getString();
            String initials = name.isBlank() ? "N" : name.strip().substring(0, 1);
            Waypoint wp = new Waypoint(node.pos().getX(), node.pos().getY(), node.pos().getZ(), name, initials,
                    Math.floorMod(style.xaeroColor(), colours), 0, true);
            current.add(wp);
            OURS.add(wp);
        }
        MapMarkers.xaero = OURS.size() + " waypoints";
    }
}

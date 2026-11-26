package com.nodetracker.map;

import com.mojang.blaze3d.platform.NativeImage;
import com.nodetracker.NodeConfig;
import com.nodetracker.NodeLog;
import com.nodetracker.NodeTracker;
import com.nodetracker.TrackedNode;
import java.util.List;
import journeymap.api.v2.client.IClientAPI;
import journeymap.api.v2.client.IClientPlugin;
import journeymap.api.v2.common.JourneyMapPlugin;
import journeymap.api.v2.client.display.Context;
import journeymap.api.v2.client.display.MarkerOverlay;
import journeymap.api.v2.client.model.MapImage;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

/** Logged nodes as JourneyMap markers on the fullscreen map, the minimap and the web map, in every dimension. */
@JourneyMapPlugin(apiVersion = "2.0.0")
public final class JourneyMapMarkers implements IClientPlugin {
    private static IClientAPI api;
    private static Object shown;
    private static boolean failed;

    @Override
    public String getModId() {
        return NodeTracker.MODID;
    }

    @Override
    public void initialize(IClientAPI clientApi) {
        api = clientApi;
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> tick());
    }

    private static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (api == null || mc.level == null) {
            shown = null;
            return;
        }
        Object key = List.of(NodeLog.version(), MarkerStyle.version(), NodeConfig.JOURNEYMAP.get(), NodeConfig.MAP_LABELS.get(),
                System.identityHashCode(mc.level));
        if (key.equals(shown)) {
            return;
        }
        shown = key;
        api.removeAll(NodeTracker.MODID);
        if (!NodeConfig.JOURNEYMAP.get()) {
            return;
        }
        boolean labels = NodeConfig.MAP_LABELS.get();
        int count = 0;
        for (TrackedNode node : NodeLog.nodes()) {
            MarkerStyle.Style style = MarkerStyle.of(node);
            MarkerOverlay marker = new MarkerOverlay(NodeTracker.MODID, node.pos(), image(style));
            String name = MarkerStyle.label(node).getString();
            marker.setDimension(ResourceKey.create(Registries.DIMENSION, node.dimension()));
            marker.setTitle(name + "\n" + MarkerStyle.coords(node));
            if (labels) {
                marker.setLabel(name);
            }
            marker.setOverlayGroupName("Aura Nodes");
            marker.setDisplayOrder(MarkerStyle.isMarked(node) ? 101 : 100);
            marker.setActiveUIs(Context.UI.Fullscreen, Context.UI.Minimap, Context.UI.Webmap);
            try {
                api.show(marker);
                count++;
            } catch (Exception e) {
                if (!failed) {
                    failed = true;
                    NodeTracker.LOG.warn("JourneyMap refused a node marker", e);
                }
            }
        }
        MapMarkers.journeyMap = count + " markers";
    }

    private static MapImage image(MarkerStyle.Style style) {
        int n = style.size();
        int[] px = style.pixels();
        NativeImage img = new NativeImage(n, n, true);
        for (int y = 0; y < n; y++) {
            for (int x = 0; x < n; x++) {
                int c = px[y * n + x];
                img.setPixelRGBA(x, y, (c & 0xFF00FF00) | ((c >> 16) & 0xFF) | ((c & 0xFF) << 16));
            }
        }
        return new MapImage(img, 0, 0, n, n, 0xFFFFFF, 1f).centerAnchors().setDisplayWidth(n * 1.5).setDisplayHeight(n * 1.5);
    }
}

package com.nodetracker.map;

import com.nodetracker.NodeConfig;
import com.nodetracker.NodeLog;
import com.nodetracker.TrackedNode;
import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import dev.ftb.mods.ftbchunks.api.client.event.AddMapIconEvent;
import dev.ftb.mods.ftbchunks.api.client.icon.MapIcon;
import dev.ftb.mods.ftbchunks.api.client.icon.MapType;
import dev.ftb.mods.ftbchunks.api.neoforge.FTBChunksClientEvent;
import dev.ftb.mods.ftblibrary.client.gui.input.Key;
import dev.ftb.mods.ftblibrary.client.gui.input.MouseButton;
import dev.ftb.mods.ftblibrary.client.gui.widget.BaseScreen;
import dev.ftb.mods.ftblibrary.util.TooltipList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

/** Logged nodes as icons on the FTB Chunks large map and minimap. */
final class FtbChunksMarkers {
    private static Object shown;

    private FtbChunksMarkers() {
    }

    static void register() {
        NeoForge.EVENT_BUS.addListener((FTBChunksClientEvent.MapIcon e) -> add(e.getEventData()));
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> tick());
    }

    private static void add(AddMapIconEvent.Data data) {
        if (!NodeConfig.FTB_CHUNKS.get() || data.mapType().isWorldIcon()) {
            return;
        }
        int count = 0;
        for (TrackedNode node : NodeLog.nodes()) {
            if (node.dimension().equals(data.dimension().identifier())) {
                data.add(new NodeIcon(node, MarkerStyle.of(node)));
                count++;
            }
        }
        MapMarkers.ftbChunks = count + " icons";
    }

    private static void tick() {
        if (Minecraft.getInstance().level == null) {
            shown = null;
            return;
        }
        Object key = List.of(NodeLog.version(), MarkerStyle.version(), NodeConfig.FTB_CHUNKS.get());
        if (!key.equals(shown)) {
            shown = key;
            try {
                FTBChunksAPI.clientApi().requestMinimapIconRefresh();
            } catch (RuntimeException ignored) {
                // FTB Chunks is not ready yet; its own first refresh picks the nodes up.
            }
        }
    }

    private record NodeIcon(TrackedNode node, MarkerStyle.Style style) implements MapIcon {
        @Override
        public Vec3 getPos(float partialTick) {
            return Vec3.atCenterOf(node.pos());
        }

        @Override
        public void addTooltip(TooltipList list) {
            list.add(MarkerStyle.label(node));
            list.string(MarkerStyle.coords(node));
        }

        @Override
        public boolean onMousePressed(BaseScreen screen, MouseButton button) {
            return false;
        }

        @Override
        public boolean onKeyPressed(BaseScreen screen, Key key) {
            return false;
        }

        @Override
        public void draw(MapType mapType, GuiGraphicsExtractor g, int x, int y, int w, int h, boolean outsideVisibleArea, int iconAlpha) {
            int n = style.size();
            int[] px = style.pixels();
            for (int j = 0; j < n; j++) {
                for (int i = 0; i < n; i++) {
                    int c = px[j * n + i];
                    if (c != 0) {
                        int a = ((c >>> 24) * iconAlpha) / 255;
                        g.fill(x + i * w / n, y + j * h / n, x + (i + 1) * w / n, y + (j + 1) * h / n, (a << 24) | (c & 0xFFFFFF));
                    }
                }
            }
        }
    }
}

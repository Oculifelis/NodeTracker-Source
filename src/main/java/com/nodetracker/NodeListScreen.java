package com.nodetracker;

import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;

/**
 * The original's GuiMain: the scanned nodes of one dimension, ten to a page. The six primal icons along the top filter
 * the list to nodes holding that aspect; the column heads sort by distance or by a primal (most first). Each row can be
 * marked for the arrow or deleted. The bottom row cycles dimensions, clears the arrow and opens the config.
 */
final class NodeListScreen extends Screen {
    private static final List<String> PRIMALS = List.of("aer", "aqua", "ignis", "ordo", "perditio", "terra");
    private static final int WIDE = 416;
    private static final int ROWS = 10;
    private static final int ROW_H = 14;
    private static final int TOP = 56;

    private final List<TrackedNode> shown = new ArrayList<>();
    private ResourceLocation dimension;
    private String filter = "all";
    private String sort = "distance";
    private int scroll;
    private int left;

    NodeListScreen() {
        super(Component.translatable("nodeatlas.screen.title"));
        var level = Minecraft.getInstance().level;
        dimension = level == null ? ResourceLocation.withDefaultNamespace("overworld") : level.dimension().location();
    }

    @Override
    protected void init() {
        left = (width - WIDE) / 2;
        refresh();
        // Filters: all, then one per primal (icon drawn over the button).
        addRenderableWidget(Button.builder(Component.translatable("nodeatlas.screen.all"), b -> filter("all"))
                .bounds(width / 2 - 4 * 24, 8, 22, 22).build());
        for (int i = 0; i < PRIMALS.size(); i++) {
            String aspect = aspectId(PRIMALS.get(i));
            addRenderableWidget(Button.builder(Component.empty(), b -> filter(aspect)).bounds(width / 2 - 3 * 24 + i * 24, 8, 22, 22).build());
        }
        // Column heads that sort.
        addRenderableWidget(Button.builder(Component.translatable("nodeatlas.screen.distance"), b -> sort("distance"))
                .bounds(left, TOP - 16, 26, 12).build());
        for (int i = 0; i < PRIMALS.size(); i++) {
            String aspect = aspectId(PRIMALS.get(i));
            addRenderableWidget(Button.builder(Component.empty(), b -> sort(aspect)).bounds(primalX(i) - 2, TOP - 16, 22, 12).build());
        }
        // Rows.
        for (int r = 0; r < ROWS && scroll + r < shown.size(); r++) {
            TrackedNode node = shown.get(scroll + r);
            int y = TOP + r * ROW_H;
            addRenderableWidget(Button.builder(Component.translatable("nodeatlas.screen.mark"), b -> {
                NodeLog.mark(node);
                onClose();
            }).bounds(left + 356, y, 32, 12).build());
            addRenderableWidget(Button.builder(Component.translatable("nodeatlas.screen.delete"), b -> {
                NodeLog.remove(node);
                rebuildWidgets();
            }).bounds(left + 390, y, 26, 12).build());
        }
        // Bottom: dimension, paging, clear arrow, config.
        int by = TOP + ROWS * ROW_H + 8;
        addRenderableWidget(Button.builder(Component.literal("<"), b -> cycleDimension(-1)).bounds(left, by, 16, 18).build());
        addRenderableWidget(Button.builder(Component.literal(">"), b -> cycleDimension(1)).bounds(left + 124, by, 16, 18).build());
        addRenderableWidget(Button.builder(Component.literal("▲"), b -> page(-1)).bounds(width / 2 - 20, by, 18, 18).build());
        addRenderableWidget(Button.builder(Component.literal("▼"), b -> page(1)).bounds(width / 2 + 2, by, 18, 18).build());
        addRenderableWidget(Button.builder(Component.translatable("nodeatlas.screen.clear_arrow"), b -> {
            NodeLog.mark(null);
            onClose();
        }).bounds(left + WIDE - 150, by, 74, 18).build());
        addRenderableWidget(Button.builder(Component.translatable("nodeatlas.screen.config"), b ->
                minecraft.setScreen(new ConfigurationScreen(NodeTracker.container, this))).bounds(left + WIDE - 72, by, 72, 18).build());
    }

    private void refresh() {
        shown.clear();
        var player = Minecraft.getInstance().player;
        for (TrackedNode n : NodeLog.nodes()) {
            if (n.dimension().equals(dimension) && (filter.equals("all") || n.amount(filter) > 0)) {
                shown.add(n);
            }
        }
        Comparator<TrackedNode> byDistance = Comparator.comparingDouble(n -> player == null ? 0 : flatDistance(n));
        shown.sort(sort.equals("distance") ? byDistance : Comparator.<TrackedNode>comparingInt(n -> -n.amount(sort)).thenComparing(byDistance));
        scroll = Math.max(0, Math.min(scroll, shown.size() - ROWS));
    }

    private void filter(String aspect) {
        filter = aspect;
        sort = "distance";
        scroll = 0;
        rebuildWidgets();
    }

    private void sort(String key) {
        sort = key;
        scroll = 0;
        rebuildWidgets();
    }

    private void page(int dir) {
        scroll = Math.max(0, Math.min(scroll + dir, shown.size() - ROWS));
        rebuildWidgets();
    }

    private void cycleDimension(int dir) {
        LinkedHashSet<ResourceLocation> dims = new LinkedHashSet<>();
        var level = Minecraft.getInstance().level;
        if (level != null) {
            dims.add(level.dimension().location());
        }
        NodeLog.nodes().forEach(n -> dims.add(n.dimension()));
        List<ResourceLocation> list = new ArrayList<>(dims);
        list.sort(Comparator.comparing(ResourceLocation::toString));
        int i = Math.max(0, list.indexOf(dimension));
        dimension = list.get(Math.floorMod(i + dir, list.size()));
        scroll = 0;
        rebuildWidgets();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY != 0) {
            page(scrollY > 0 ? -1 : 1);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (NodeTracker.NODE_LIST.matches(keyCode, scanCode)) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        for (int i = 0; i < PRIMALS.size(); i++) {
            String aspect = aspectId(PRIMALS.get(i));
            icon(g, aspect, width / 2 - 3 * 24 + i * 24 + 3, 11, 16);
            icon(g, aspect, primalX(i) + 4, TOP - 16, 12);
            if (aspect.equals(filter)) {
                g.fill(width / 2 - 3 * 24 + i * 24, 31, width / 2 - 3 * 24 + i * 24 + 22, 32, 0xFFFFFFFF);
            }
        }
        if (filter.equals("all")) {
            g.fill(width / 2 - 4 * 24, 31, width / 2 - 4 * 24 + 22, 32, 0xFFFFFFFF);
        }
        int head = 0xFFAAAAAA;
        g.drawString(font, Component.translatable("nodeatlas.screen.position"), left + 28, TOP - 14, head, false);
        g.drawString(font, Component.translatable("nodeatlas.screen.type"), left + 100, TOP - 14, head, false);
        g.drawString(font, Component.translatable("nodeatlas.screen.other"), left + 318, TOP - 14, head, false);

        TrackedNode marked = NodeLog.marked();
        for (int r = 0; r < ROWS && scroll + r < shown.size(); r++) {
            TrackedNode n = shown.get(scroll + r);
            int y = TOP + r * ROW_H + 2;
            boolean isMarked = marked != null && marked.sameSpot(n.dimension(), n.pos());
            int colour = isMarked ? 0xFF55FFFF : 0xFFFFFFFF;
            g.drawString(font, String.valueOf((int) Math.round(flatDistance(n))), left + 2, y, colour, false);
            g.drawString(font, fit(n.pos().getX() + " " + n.pos().getY() + " " + n.pos().getZ(), 70), left + 28, y, colour, false);
            Component type = Component.translatable("nodeatlas.type." + n.type());
            if (n.modifier().isPresent()) {
                type = Component.translatable("nodeatlas.type_modified", Component.translatable("nodeatlas.modifier." + n.modifier().get()), type);
            }
            g.drawString(font, fit(type.getString(), 74), left + 100, y, n.drained() ? 0xFFFF5555 : n.tools().isEmpty() ? colour : 0xFFFFAA00, false);
            for (int i = 0; i < PRIMALS.size(); i++) {
                int amount = n.amount(aspectId(PRIMALS.get(i)));
                if (amount > 0) {
                    g.drawString(font, String.valueOf(amount), primalX(i) + 4, y, colour, false);
                }
            }
            int ox = left + 318;
            for (Map.Entry<String, Integer> e : n.aspects().entrySet()) {
                if (PRIMALS.stream().noneMatch(p -> aspectId(p).equals(e.getKey())) && ox < left + 346) {
                    icon(g, e.getKey(), ox, y - 1, 8);
                    ox += 9;
                }
            }
        }
        if (shown.isEmpty()) {
            Component hint = Component.translatable("nodeatlas.screen.empty");
            g.drawString(font, hint, (width - font.width(hint)) / 2, TOP + 20, 0xFFAAAAAA, false);
        }
        int by = TOP + ROWS * ROW_H + 8;
        String dim = dimension.getNamespace().equals("minecraft") ? dimension.getPath() : dimension.toString();
        g.drawString(font, dim, left + 70 - font.width(dim) / 2, by + 5, 0xFFFFFFFF, false);
        String count = shown.isEmpty() ? "0" : (scroll + 1) + "-" + Math.min(shown.size(), scroll + ROWS) + " / " + shown.size();
        g.drawString(font, count, width / 2 - font.width(count) / 2, by + 22, 0xFFAAAAAA, false);
    }

    private int primalX(int i) {
        return left + 176 + i * 23;
    }

    private static String aspectId(String primal) {
        return "thaumaturge:" + primal;
    }

    private double flatDistance(TrackedNode n) {
        var p = Minecraft.getInstance().player;
        if (p == null) {
            return 0;
        }
        double dx = n.pos().getX() + 0.5 - p.getX();
        double dz = n.pos().getZ() + 0.5 - p.getZ();
        return Math.sqrt(dx * dx + dz * dz);
    }

    /** Draws an aspect's own Thaumaturge icon (32x32 texture) at the given size, if the aspect is known here. */
    private static void icon(GuiGraphics g, String aspect, int x, int y, int size) {
        var level = Minecraft.getInstance().level;
        ResourceLocation id = ResourceLocation.tryParse(aspect);
        if (level == null || id == null) {
            return;
        }
        level.registryAccess().registry(IAspect.REGISTRY_KEY).flatMap(r -> r.getOptional(id)).ifPresent(a ->
                g.blit(a.texture(), x, y, size, size, 0f, 0f, 32, 32, 32, 32));
    }

    /** Cuts text that would run into the next column, marking the cut with a dot. */
    private String fit(String text, int width) {
        return font.width(text) <= width ? text : font.plainSubstrByWidth(text, width - font.width(".")) + ".";
    }
}

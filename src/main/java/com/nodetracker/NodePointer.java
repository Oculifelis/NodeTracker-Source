package com.nodetracker;

import com.leclowndu93150.thaumaturge.api.items.GogglesAccess;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import com.mojang.math.Axis;
import net.minecraft.client.gui.LayeredDraw;

/**
 * The original's GuiPointer: while revealing goggles are worn, an arrow at the top of the screen turns toward the marked
 * node, with its flat distance and whether it lies above, below or level. The arrow is drawn in code (the original's
 * texture is not shipped): a Thaumaturge-violet head, or plain red with the alt-arrow option.
 */
final class NodePointer implements LayeredDraw.Layer {
    private static final int SIZE = 64;

    @Override
    public void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        var player = mc.player;
        TrackedNode node = NodeLog.marked();
        if (player == null || mc.level == null || node == null || mc.options.hideGui
                || !node.dimension().equals(mc.level.dimension().location()) || !GogglesAccess.wearsRevealingGear(player)) {
            return;
        }
        double dx = node.pos().getX() + 0.5 - player.getX();
        double dz = node.pos().getZ() + 0.5 - player.getZ();
        double direction = Math.toDegrees(Math.atan2(dx, dz)) + player.getYRot();
        float scale = NodeConfig.ARROW_SIZE.get().floatValue();
        float cx = g.guiWidth() / 2.0F + NodeConfig.ARROW_X.get();
        float cy = SIZE * scale / 2.0F + 5 + NodeConfig.ARROW_Y.get();

        g.pose().pushPose();
        g.pose().translate(cx, cy, 0);
        g.pose().mulPose(Axis.ZP.rotation((float) Math.toRadians(-direction)));
        g.pose().scale(scale, scale, 1);
        if (NodeConfig.ALT_ARROW.get()) {
            triangle(g, 20, 0xFFFF0000);
        } else {
            triangle(g, 24, 0xFF1A0B2E);
            triangle(g, 20, 0xFF9A6CFF);
            triangle(g, 12, 0xFFD9C6FF);
        }
        g.pose().popPose();

        int distance = (int) Math.round(Math.sqrt(dx * dx + dz * dz));
        int py = Mth.floor(player.getY());
        String where = py > node.pos().getY() ? "below" : py == node.pos().getY() ? "level" : "above";
        int colour = where.equals("below") ? 0xFFFF5555 : where.equals("level") ? 0xFFFFFFFF : 0xFF55FF55;
        Component text = Component.translatable("nodeatlas.pointer." + where, distance);
        int ty = (int) (SIZE * scale + 5 * scale + NodeConfig.ARROW_Y.get());
        g.drawString(mc.font, text, (int) cx - mc.font.width(text) / 2, ty, colour, true);
    }

    /** An upward-pointing triangle centred on the origin, half-height {@code r}, filled one row at a time. */
    private static void triangle(GuiGraphics g, int r, int colour) {
        for (int y = -r; y < r; y++) {
            int half = Math.round((y + r) * 0.55F);
            g.fill(-half, y, half + 1, y + 1, colour);
        }
    }
}

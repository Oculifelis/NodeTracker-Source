package com.nodetracker;

import com.leclowndu93150.thaumaturge.api.items.GogglesAccess;
import com.leclowndu93150.thaumaturge.content.aura.node.BlockEntityNode;
import com.leclowndu93150.thaumaturge.registry.TTItems;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * The original's RightClickEvent: using the Thaumometer, or right-clicking while wearing revealing goggles (the
 * original's Lens of Order and Entropy), logs the aura node looked at. Nodes have no hitbox, so the look ray is walked
 * block by block up to {@link #REACH}, stopping at the first solid block.
 */
final class NodeScanner {
    static final double REACH = 16.0;
    /** How far from a scanned node the blocks tagged as node tools are looked for. */
    static final int TOOL_RADIUS = 8;
    static final TagKey<Block> NODE_TOOLS = TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath("nodeatlas", "node_tools"));
    static final TagKey<Block> NODE_DRAINERS = TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath("nodeatlas", "node_drainers"));

    private NodeScanner() {
    }

    static void onUse(InteractionHand hand) {
        Minecraft mc = Minecraft.getInstance();
        var player = mc.player;
        if (player == null || mc.level == null
                || !player.getItemInHand(hand).is(TTItems.THAUMOMETER.get()) && !GogglesAccess.wearsRevealingGear(player)) {
            return;
        }
        Vec3 eye = player.getEyePosition();
        Vec3 step = player.getViewVector(1.0F).scale(0.2);
        BlockPos last = null;
        for (int i = 0; i * 0.2 <= REACH; i++) {
            BlockPos pos = BlockPos.containing(eye.add(step.scale(i)));
            if (pos.equals(last)) {
                continue;
            }
            last = pos;
            if (mc.level.getBlockEntity(pos) instanceof BlockEntityNode node) {
                log(node, pos);
                return;
            }
            if (mc.level.getBlockState(pos).isSolidRender()) {
                return;
            }
        }
    }

    private static void log(BlockEntityNode node, BlockPos pos) {
        Map<String, Integer> aspects = new LinkedHashMap<>();
        for (var entry : node.getAspects().sortedByAmount()) {
            entry.aspect().unwrapKey().ifPresent(k -> aspects.merge(k.identifier().toString(), entry.amount(), Integer::sum));
        }
        if (aspects.isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        var dim = mc.level.dimension().identifier();
        boolean known = NodeLog.has(dim, pos);
        List<String> tools = new ArrayList<>();
        Component firstTool = null;
        boolean drained = false;
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-TOOL_RADIUS, -TOOL_RADIUS, -TOOL_RADIUS),
                pos.offset(TOOL_RADIUS, TOOL_RADIUS, TOOL_RADIUS))) {
            BlockState state = mc.level.getBlockState(p);
            boolean drainer = state.is(NODE_DRAINERS);
            if (!drainer && !state.is(NODE_TOOLS)) {
                continue;
            }
            drained |= drainer;
            String id = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
            if (!tools.contains(id)) {
                tools.add(id);
                if (firstTool == null) {
                    firstTool = state.getBlock().getName();
                }
            }
        }
        NodeLog.record(new TrackedNode(dim, pos.immutable(), node.kind().getSerializedName(),
                Optional.ofNullable(node.trait()).map(m -> m.getSerializedName()), aspects, System.currentTimeMillis(), List.copyOf(tools), drained));
        mc.player.sendOverlayMessage(drained ? Component.translatable("nodeatlas.message.drained", pos.getX(), pos.getY(), pos.getZ())
                : firstTool != null ? Component.translatable("nodeatlas.message.near_tool", pos.getX(), pos.getY(), pos.getZ(), firstTool)
                : Component.translatable(known ? "nodeatlas.message.updated" : "nodeatlas.message.logged",
                pos.getX(), pos.getY(), pos.getZ()));
    }
}

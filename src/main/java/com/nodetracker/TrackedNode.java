package com.nodetracker;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

/**
 * One scanned aura node, as the original's NodeList: where it is, its type and modifier, its aspects (aspect id to
 * amount), when it was last scanned (epoch milliseconds), the node tools found near it (block ids, from the
 * {@code nodeatlas:node_tools} block tag) and whether one of them drains it ({@code nodeatlas:node_drainers}).
 */
public record TrackedNode(ResourceLocation dimension, BlockPos pos, String type, Optional<String> modifier,
                          Map<String, Integer> aspects, long seen, List<String> tools, boolean drained) {
    public static final Codec<TrackedNode> CODEC = RecordCodecBuilder.create(i -> i.group(
            ResourceLocation.CODEC.fieldOf("dimension").forGetter(TrackedNode::dimension),
            BlockPos.CODEC.fieldOf("pos").forGetter(TrackedNode::pos),
            Codec.STRING.fieldOf("type").forGetter(TrackedNode::type),
            Codec.STRING.optionalFieldOf("modifier").forGetter(TrackedNode::modifier),
            Codec.unboundedMap(Codec.STRING, Codec.INT).fieldOf("aspects").forGetter(TrackedNode::aspects),
            Codec.LONG.fieldOf("seen").forGetter(TrackedNode::seen),
            Codec.STRING.listOf().optionalFieldOf("tools", List.of()).forGetter(TrackedNode::tools),
            Codec.BOOL.optionalFieldOf("drained", false).forGetter(TrackedNode::drained)).apply(i, TrackedNode::new));

    public int amount(String aspect) {
        return aspects.getOrDefault(aspect, 0);
    }

    public boolean sameSpot(ResourceLocation dim, BlockPos at) {
        return dimension.equals(dim) && pos.equals(at);
    }
}

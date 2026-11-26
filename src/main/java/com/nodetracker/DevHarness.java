package com.nodetracker;

import com.leclowndu93150.thaumaturge.api.aspect.AspectInstance;
import com.leclowndu93150.thaumaturge.api.aspect.AspectList;
import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import com.leclowndu93150.thaumaturge.api.nodes.NodeModifier;
import com.leclowndu93150.thaumaturge.api.nodes.NodeType;
import com.leclowndu93150.thaumaturge.content.aura.node.BlockEntityNode;
import com.leclowndu93150.thaumaturge.registry.TTBlocks;
import com.leclowndu93150.thaumaturge.registry.TTItems;
import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

/**
 * Development-only check, inert unless the JVM runs with -Dnodetracker.autotest (only the Gradle dev run passes it, see
 * build.gradle). It makes a creative world, places an aura node in cleared air in front of the player, puts revealing
 * goggles on, scans the node, screenshots the list, marks the node, turns away and screenshots the arrow, then quits.
 * "NTTEST" lines in the log report each step. After Tenebrae's DevHarness.
 */
final class DevHarness {
    private static final Logger LOG = LogUtils.getLogger();
    private static final List<Runnable> STEPS = new ArrayList<>();
    private static boolean started;
    private static boolean built;
    private static int idx;
    private static int wait = 100;
    private static BlockPos node;

    private DevHarness() {
    }

    static void register() {
        if (System.getProperty("nodeatlas.autotest") != null) {
            NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> tick());
        }
    }

    private static void tick() {
        Minecraft mc = Minecraft.getInstance();
        // A test client never holds the cursor, even off-screen.
        mc.mouseHandler.releaseMouse();
        if (mc.level == null || mc.player == null) {
            if (!started && mc.screen instanceof TitleScreen title) {
                started = true;
                mc.options.pauseOnLostFocus = false;
                mc.options.renderDistance().set(4);
                String name = "nttest" + System.currentTimeMillis() / 1000;
                mc.createWorldOpenFlows().createFreshLevel(name, new LevelSettings(name, GameType.CREATIVE,
                                LevelSettings.DifficultySettings.DEFAULT, true, WorldDataConfiguration.DEFAULT),
                        new WorldOptions(20261101L, false, false), WorldPresets::createNormalWorldDimensions, title);
                LOG.info("NTTEST creating world");
            }
            return;
        }
        if (mc.screen instanceof net.minecraft.client.gui.screens.PauseScreen) {
            mc.setScreen(null);
        }
        if (!built) {
            built = true;
            build();
        }
        if (wait > 0) {
            wait--;
            return;
        }
        if (idx < STEPS.size()) {
            try {
                STEPS.get(idx++).run();
            } catch (Throwable t) {
                LOG.error("NTTEST step {} failed", idx, t);
            }
            wait = 30;
        } else {
            LOG.info("NTTEST DONE");
            mc.stop();
        }
    }

    private static void build() {
        STEPS.add(() -> {
            Minecraft mc = Minecraft.getInstance();
            var p = mc.player;
            p.setXRot(0);
            p.setYRot(0);
            node = BlockPos.containing(p.getEyePosition().add(0, 0, 4));
            var server = mc.getSingleplayerServer();
            server.execute(() -> {
                ServerLevel level = server.overworld();
                for (BlockPos at : BlockPos.betweenClosed(node.offset(-3, -2, -4), node.offset(3, 3, 2))) {
                    level.setBlockAndUpdate(at, Blocks.AIR.defaultBlockState());
                }
                level.setBlockAndUpdate(node, TTBlocks.NODE.get().defaultBlockState());
                if (level.getBlockEntity(node) instanceof BlockEntityNode be) {
                    var aspects = level.registryAccess().lookupOrThrow(IAspect.REGISTRY_KEY);
                    AspectList list = AspectList.EMPTY;
                    for (String[] a : new String[][] {{"aer", "20"}, {"ignis", "14"}, {"ordo", "9"}, {"permutatio", "6"}}) {
                        list = list.add(new AspectInstance(aspects.getOrThrow(ResourceKey.create(IAspect.REGISTRY_KEY,
                                Identifier.fromNamespaceAndPath("thaumaturge", a[0]))), Integer.parseInt(a[1])));
                    }
                    be.reclassify(NodeType.DARK);
                    be.assignTrait(NodeModifier.BRIGHT);
                    be.setAspects(list);
                    be.setChangedAndSync();
                }
                server.getPlayerList().getPlayer(p.getUUID()).setItemSlot(EquipmentSlot.HEAD, new ItemStack(TTItems.GOGGLES_REVEALING.get()));
            });
            LOG.info("NTTEST placed node at {}", node);
        });
        STEPS.add(() -> {
            Minecraft mc = Minecraft.getInstance();
            // Clearing the area can drop the player a block, so aim at the node's centre rather than straight ahead.
            var to = net.minecraft.world.phys.Vec3.atCenterOf(node).subtract(mc.player.getEyePosition());
            mc.player.setYRot((float) Math.toDegrees(Math.atan2(-to.x, to.z)));
            mc.player.setXRot((float) -Math.toDegrees(Math.atan2(to.y, Math.sqrt(to.x * to.x + to.z * to.z))));
            var be = mc.level.getBlockEntity(node);
            LOG.info("NTTEST before scan: goggles={} eye={} look={} block={} be={} aspects={}",
                    com.leclowndu93150.thaumaturge.api.items.GogglesAccess.wearsRevealingGear(mc.player),
                    mc.player.getEyePosition(), mc.player.getViewVector(1.0F), mc.level.getBlockState(node), be,
                    be instanceof BlockEntityNode n ? n.getAspects().sortedByAmount() : null);
            NodeScanner.onUse(InteractionHand.MAIN_HAND);
            LOG.info("NTTEST scanned: {} node(s) logged {}", NodeLog.nodes().size(), NodeLog.nodes());
        });
        STEPS.add(() -> Minecraft.getInstance().setScreen(new NodeListScreen()));
        STEPS.add(() -> shot("list"));
        STEPS.add(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (!NodeLog.nodes().isEmpty()) {
                NodeLog.mark(NodeLog.nodes().getFirst());
            }
            mc.setScreen(null);
            mc.player.setYRot(90);
            LOG.info("NTTEST marked {}", NodeLog.marked());
        });
        STEPS.add(() -> shot("arrow"));
        STEPS.add(() -> {
            // FTB Chunks only asks for icons while its minimap or large map draws; open the large map if it is loaded.
            try {
                Class.forName("dev.ftb.mods.ftbchunks.client.gui.map.LargeMapScreen").getMethod("openMap").invoke(null);
            } catch (ReflectiveOperationException | LinkageError ignored) {
                // FTB Chunks is not installed.
            }
        });
        STEPS.add(() -> shot("ftbmap"));
        STEPS.add(() -> LOG.info("NTTEST maps: {}", com.nodetracker.map.MapMarkers.report()));
    }

    private static void shot(String name) {
        Minecraft mc = Minecraft.getInstance();
        Screenshot.grab(mc.gameDirectory, NodeTracker.MODID + "_" + name + ".png", mc.getMainRenderTarget(), 1, c -> {});
        LOG.info("NTTEST shot {}", name);
    }
}

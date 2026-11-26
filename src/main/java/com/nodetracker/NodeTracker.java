package com.nodetracker;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.logging.LogUtils;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

/**
 * Thaumcraft Node Tracker (Dyonovan, GTNewHorizons) for Thaumaturge. Client side only: scanned aura nodes are logged per
 * world or server, listed on a key (I, as the original), and the marked one gets an arrow while goggles are worn.
 */
@Mod(value = NodeTracker.MODID, dist = Dist.CLIENT)
public final class NodeTracker {
    public static final String MODID = "nodeatlas";
    public static final Logger LOG = LogUtils.getLogger();
    static final KeyMapping.Category CATEGORY = new KeyMapping.Category(id("keys"));
    static final KeyMapping NODE_LIST = new KeyMapping("key.nodeatlas.node_list", InputConstants.KEY_I, CATEGORY);
    static ModContainer container;

    public NodeTracker(IEventBus modBus, ModContainer container) {
        NodeTracker.container = container;
        Path config = FMLPaths.CONFIGDIR.get();
        LegacyFiles.adoptQuietly(config.resolve(LegacyFiles.OLD + "-client.toml"), config.resolve(MODID + "-client.toml"));
        container.registerConfig(ModConfig.Type.CLIENT, NodeConfig.SPEC);
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        modBus.addListener((RegisterKeyMappingsEvent e) -> {
            e.registerCategory(CATEGORY);
            e.register(NODE_LIST);
        });
        modBus.addListener((RegisterGuiLayersEvent e) -> e.registerAbove(VanillaGuiLayers.BOSS_OVERLAY, id("pointer"), new NodePointer()));
        NeoForge.EVENT_BUS.addListener((InputEvent.InteractionKeyMappingTriggered e) -> {
            if (e.isUseItem()) {
                NodeScanner.onUse(e.getHand());
            }
        });
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> {
            Minecraft mc = Minecraft.getInstance();
            while (NODE_LIST.consumeClick()) {
                if (mc.player != null && mc.screen == null) {
                    mc.setScreen(new NodeListScreen());
                }
            }
        });
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingIn e) -> {
            Path game = Minecraft.getInstance().gameDirectory.toPath();
            String folder = worldFolder(e);
            Path nodes = game.resolve(MODID).resolve(folder).resolve("nodes.json");
            LegacyFiles.adoptQuietly(game.resolve(LegacyFiles.OLD).resolve(folder).resolve("nodes.json"), nodes);
            NodeLog.open(nodes);
        });
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> NodeLog.close());
        DevHarness.register();
        com.nodetracker.map.MapMarkers.register(modBus);
    }

    /** The save folder's name in single player, else host_port of the server (the original's naming). */
    private static String worldFolder(ClientPlayerNetworkEvent.LoggingIn e) {
        var server = Minecraft.getInstance().getSingleplayerServer();
        String name;
        if (server != null) {
            var root = server.getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize().getFileName();
            name = root == null ? "sp_world" : root.toString();
        } else if (e.getConnection().getRemoteAddress() instanceof InetSocketAddress address) {
            name = address.getHostString() + "_" + address.getPort();
        } else {
            name = "server";
        }
        return name.replaceAll("[^A-Za-z0-9._ -]", "_");
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MODID, path);
    }
}

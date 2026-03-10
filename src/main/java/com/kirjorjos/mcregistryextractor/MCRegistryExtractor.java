package com.kirjorjos.mcregistryextractor;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLEnvironment;

import com.google.gson.JsonObject;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(MCRegistryExtractor.MOD_ID)
public class MCRegistryExtractor
{
    public static final String MOD_ID = "mc_registry_extractor";
    private static final Logger LOGGER = LogUtils.getLogger();
    public static boolean isDev = false;
    private static final String LAST_WORLD_FILE = "last_world.txt";

    public MCRegistryExtractor()
    {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        LOGGER.info("MC Registry Extractor Initializing...");

        // Check if we are in a dev environment (IDE)
        if (!FMLEnvironment.production) {
            isDev = true;
            LOGGER.info("MC Registry Extractor detected DEV environment (IDE)");
        }

        // Check manifest for isDev attribute
        ModList.get().getModContainerById(MOD_ID).ifPresent(container -> {
            try {
                Path manifestPath = container.getModInfo().getOwningFile().getFile().findResource("META-INF/MANIFEST.MF");
                if (Files.exists(manifestPath)) {
                    try (java.io.InputStream is = Files.newInputStream(manifestPath)) {
                        java.util.jar.Manifest manifest = new java.util.jar.Manifest(is);
                        String val = manifest.getMainAttributes().getValue("isDev");
                        if ("true".equalsIgnoreCase(val)) {
                            isDev = true;
                            LOGGER.info("MC Registry Extractor detected DEV mode from JAR manifest");
                        }
                    }
                }
            } catch (Exception e) {
                // Fallback to mod properties if manifest fails
                Object val = container.getModInfo().getModProperties().get("isDev");
                if (val instanceof String && ((String)val).equalsIgnoreCase("true")) {
                    isDev = true;
                    LOGGER.info("MC Registry Extractor detected DEV mode from mod properties");
                }
            }
        });
        
        // Fallback for testing: always enable if on client and we want automation
        // Since we are using this specifically for dev work, we can enable it by default on client
        // or check for a system property
        if (System.getProperty("mcreg.dev") != null) {
            isDev = true;
            LOGGER.info("MC Registry Extractor detected DEV mode from system property");
        }

        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);
        
        if (isDev) {
            LOGGER.info("Registering DevHandler for automation...");
            MinecraftForge.EVENT_BUS.register(new DevHandler());
        }
    }

    private void commonSetup(final FMLCommonSetupEvent event)
    {
        // Some common setup code
        LOGGER.info("HELLO FROM COMMON SETUP");
        LOGGER.info("DIRT BLOCK >> {}", ForgeRegistries.BLOCKS.getKey(Blocks.DIRT));
    }

    public static class DevHandler {
        private int ticks = 0;
        private boolean startedJoin = false;
        private boolean extracted = false;
        private boolean worldSaved = false;

        public DevHandler() {
            LOGGER.info("DevHandler: Initialized and registered");
        }

        @SubscribeEvent
        public void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            
            Minecraft mc = Minecraft.getInstance();
            ticks++;
            
            if (ticks % 100 == 0) {
                String screenName = mc.screen != null ? mc.screen.getClass().getName() : "null";
                String playerName = mc.player != null ? mc.player.getName().getString() : "null";
                LOGGER.info("DevHandler State - Ticks: " + ticks + " | Screen: " + screenName + " | Player: " + playerName);
            }

            // Auto-join logic
            if (ticks > 40 && !startedJoin && mc.screen instanceof TitleScreen) {
                startedJoin = true;
                tryAutoJoin(mc);
            }

            // Save last world logic
            if (mc.player != null) {
                if (!worldSaved && mc.getSingleplayerServer() != null) {
                    String folderName = mc.getSingleplayerServer().getWorldData().getLevelName();
                    try {
                        saveLastWorld(folderName);
                        worldSaved = true;
                    } catch (Exception e) {
                        LOGGER.error("Failed to save last world", e);
                    }
                }
            } else {
                worldSaved = false;
            }

            if (mc.player != null && !extracted) {
                extracted = true;
                LOGGER.info("DevHandler: Player detected, starting extraction...");
                
                try {
                    JsonObject root = new JsonObject();
                    JsonObject metadata = new JsonObject();
                    metadata.addProperty("schemaVersion", "1");
                    metadata.addProperty("gameVersion", "1.19.2");
                    metadata.addProperty("description", "Auto-generated game registries");
                    root.add("metadata", metadata);

                    root.add("items", ExtractItems.extractItems(mc.player));
                    root.add("blocks", ExtractBlocks.extractBlocks(mc.player));
                    root.add("fluids", ExtractFluids.extractFluids());
                    root.add("entities", ExtractEntities.extractEntities(mc.player));

                    String home = System.getProperty("user.home");
                    String path = home + "/registry_output.json";
                    JSONWriter.write(path, root);
                    LOGGER.info("DevHandler: Successfully extracted to " + path);
                    
                    LOGGER.info("DevHandler: Extraction complete, closing game...");
                    mc.stop();
                } catch (Exception e) {
                    LOGGER.error("DevHandler: Extraction failed!", e);
                    mc.stop();
                }
            }
        }

        private void tryAutoJoin(Minecraft mc) {
            Path path = Paths.get(LAST_WORLD_FILE);
            if (Files.exists(path)) {
                try {
                    String folderName = Files.readString(path).trim();
                    if (!folderName.isEmpty()) {
                        LOGGER.info("DevHandler: Attempting to auto-join world: " + folderName);
                        mc.createWorldOpenFlows().loadLevel(mc.screen, folderName);
                    }
                } catch (IOException e) {
                    LOGGER.error("DevHandler: Failed to read " + LAST_WORLD_FILE, e);
                }
            } else {
                LOGGER.info("DevHandler: No " + LAST_WORLD_FILE + " found, skipping auto-join.");
            }
        }

        private void saveLastWorld(String folderName) {
            try {
                Files.writeString(Paths.get(LAST_WORLD_FILE), folderName);
                LOGGER.info("DevHandler: Saved last world folder: " + folderName);
            } catch (IOException e) {
                LOGGER.error("DevHandler: Failed to save " + LAST_WORLD_FILE, e);
            }
        }
    }

    // You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ClientModEvents
    {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event)
        {
            // Some client setup code
            LOGGER.info("HELLO FROM CLIENT SETUP");
            LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
        }
    }

    public static Logger getLogger() {
        return LOGGER;
    }
}

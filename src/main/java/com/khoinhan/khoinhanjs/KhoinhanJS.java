package com.khoinhan.khoinhanjs;

import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import net.minecraft.client.server.IntegratedServer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dedicated.DedicatedServer;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class KhoinhanJS {
    public static final String MODID = "khoinhanjs";
    public static final Logger LOGGER = LogUtils.getLogger();
    
    private static boolean registryEnabled = false;
    private static boolean singleplayerWarningShown = false;
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public static void init() {
        LOGGER.info("KhoinhanJS initialized - welcome to java");
        // Don't check singleplayer here - wait for server to start
    }
    
    public static boolean isRegistryEnabled() {
        return registryEnabled;
    }
    
    public static void enableRegistry() {
        if (!registryEnabled) {
            registryEnabled = true;
            LOGGER.info("KhoinhanJS registry enabled");
            sendWarningToChat("&a[KhoinhanJS] Registry enabled! Reloading KubeJS server-scripts...");
            reloadKubeJSScripts();
        }
    }
    
    public static void disableRegistry() {
        registryEnabled = false;
        LOGGER.info("KhoinhanJS registry disabled");
    }
    
    public static void forceReloadBindings() {
        if (registryEnabled) {
            LOGGER.info("Force reloading KhoinhanJS bindings...");
            sendWarningToChat("&e[KhoinhanJS] Reloading bindings...");
            // The bindings will be re-registered on next KubeJS reload
        } else {
            LOGGER.warn("Cannot reload bindings - registry is disabled");
        }
    }
    
    public static void checkSingleplayerAndWarn() {
        try {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                    // Singleplayer mode - keep registry disabled and show warning
                    singleplayerWarningShown = true;
                    String warning = "&c[KhoinhanJS] &eWARNING: Running in singleplayer mode! " +
                                   "Network features like fetch, Discord, and server bindings are disabled by default for your safety, " +
                                   "as these scripts can execute potentially unsafe network operations on your computer. " +
                                   "Use &f/khoinhanjs enable &eto enable them if you understand the risks.";
                    
                    LOGGER.warn("KhoinhanJS detected singleplayer mode - registry disabled by default");
                    
                    // Since Dedicated servers doenst get this, and singleplayer only loads if you join a world this is actually working
                    scheduler.schedule(() -> {
                        sendWarningToChat(warning);
                    }, 3, TimeUnit.SECONDS);
            } else if (server instanceof DedicatedServer && !registryEnabled) {
                    // Dedicated server - enable registry automatically
                    registryEnabled = true;
                    LOGGER.info("KhoinhanJS detected dedicated server - registry enabled automatically");
            }
        } catch (Exception e) {
            LOGGER.error("Failed to check singleplayer status: " + e.getMessage());
        }
    }
    
    private static void sendWarningToChat(String message) {
        try {
            // Always log to console first
            LOGGER.info("KhoinhanJS: {}", message.replaceAll("&[0-9a-fk-or]", ""));
            
            // Try to send to players using system message (bypasses registry check)
            ServerBinding.sendSystemMessage(message);
        } catch (Exception e) {
            LOGGER.error("Failed to send warning to chat: " + e.getMessage());
        }
    }
    
    private static void reloadKubeJSScripts() {
        try {
            // Send a message to inform users to reload KubeJS scripts manually
            sendWarningToChat("&e[KhoinhanJS] Please run /kubejs reload server to reload server-scripts with the new bindings!");
            LOGGER.info("KubeJS server-scripts reload requested - user should run /kubejs reload server");
        } catch (Exception e) {
            LOGGER.error("Failed to request KubeJS scripts reload: " + e.getMessage());
        }
    }
}
package com.khoinhan.khoinhanjs;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

@Mod(KhoinhanJS.MODID)
public class KhoinhanJSNeoForge {
    public KhoinhanJSNeoForge(IEventBus modEventBus, ModContainer modContainer) {
        KhoinhanJS.init();
        
        // Register command
        NeoForge.EVENT_BUS.addListener(this::onRegisterCommands);
        
        // Handle server lifecycle
        NeoForge.EVENT_BUS.addListener(this::onServerStarting);
        NeoForge.EVENT_BUS.addListener(this::onServerStopping);
    }
    
    private void onRegisterCommands(RegisterCommandsEvent event) {
        KhoinhanJSCommand.register(event.getDispatcher());
    }
    
    private void onServerStarting(ServerStartingEvent event) {
        KhoinhanJS.LOGGER.info("Server starting - KhoinhanJS ready");
        // Check for singleplayer and show warning after server is started
        KhoinhanJS.checkSingleplayerAndWarn();
    }
    
    private void onServerStopping(ServerStoppingEvent event) {
        KhoinhanJS.disableRegistry();
        KhoinhanJS.LOGGER.info("Server stopping - KhoinhanJS registry disabled");
    }
}

package com.khoinhan.khoinhanjs;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class KhoinhanJSCommand {
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("khoinhanjs")
            .requires(source -> source.hasPermission(2)) // Requires op level 2
            .then(Commands.literal("enable")
                .executes(context -> {
                    CommandSourceStack source = context.getSource();
                    if (KhoinhanJS.isRegistryEnabled()) {
                        source.sendFailure(Component.literal("KhoinhanJS registry is already enabled!"));
                        return 0;
                    }
                    
                    KhoinhanJS.enableRegistry();
                    source.sendSuccess(() -> Component.literal("KhoinhanJS registry enabled successfully!"), true);
                    return 1;
                }))
            .then(Commands.literal("disable")
                .executes(context -> {
                    CommandSourceStack source = context.getSource();
                    if (!KhoinhanJS.isRegistryEnabled()) {
                        source.sendFailure(Component.literal("KhoinhanJS registry is already disabled!"));
                        return 0;
                    }
                    
                    KhoinhanJS.disableRegistry();
                    source.sendSuccess(() -> Component.literal("KhoinhanJS registry disabled!"), true);
                    return 1;
                }))
            .then(Commands.literal("reload")
                .executes(context -> {
                    CommandSourceStack source = context.getSource();
                    if (!KhoinhanJS.isRegistryEnabled()) {
                        source.sendFailure(Component.literal("KhoinhanJS registry is disabled! Enable it first with /khoinhanjs enable"));
                        return 0;
                    }
                    
                    KhoinhanJS.forceReloadBindings();
                    source.sendSuccess(() -> Component.literal("KhoinhanJS bindings reload requested!"), true);
                    return 1;
                }))
            .then(Commands.literal("status")
                .executes(context -> {
                    CommandSourceStack source = context.getSource();
                    boolean enabled = KhoinhanJS.isRegistryEnabled();
                    String status = enabled ? "enabled" : "disabled";
                    String color = enabled ? "&a" : "&c";
                    
                    source.sendSuccess(() -> Component.literal(color + "[KhoinhanJS] Registry is currently " + status), false);
                    return 1;
                }))
        );
    }
}

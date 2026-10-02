package com.darkspirit69.jadebridge.command;

import com.darkspirit69.jadebridge.JadeBridge;
import com.darkspirit69.jadebridge.JadeProtocol;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * /jadebridge status | providers | reload | debug [on|off], permission jadebridge.admin.
 */
public final class JadeBridgeCommand {

    private JadeBridgeCommand() {
    }

    public static LiteralCommandNode<CommandSourceStack> build() {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("jadebridge")
                .requires(source -> source.getSender().hasPermission("jadebridge.admin"))
                .then(Commands.literal("status").executes(context -> {
                    status(context.getSource());
                    return 1;
                }))
                .then(Commands.literal("providers").executes(context -> {
                    providers(context.getSource());
                    return 1;
                }))
                .then(Commands.literal("reload").executes(context -> {
                    reload(context.getSource());
                    return 1;
                }))
                .then(Commands.literal("debug")
                        .executes(context -> {
                            JadeBridge plugin = plugin();
                            context.getSource().getSender().sendMessage(Component.text(
                                    "Debug logging is " + (plugin.settings().isDebug() ? "on" : "off"),
                                    NamedTextColor.YELLOW));
                            return 1;
                        })
                        .then(Commands.literal("on").executes(context -> {
                            plugin().settings().setDebug(true);
                            context.getSource().getSender().sendMessage(Component.text("Debug logging enabled", NamedTextColor.GREEN));
                            return 1;
                        }))
                        .then(Commands.literal("off").executes(context -> {
                            plugin().settings().setDebug(false);
                            context.getSource().getSender().sendMessage(Component.text("Debug logging disabled", NamedTextColor.YELLOW));
                            return 1;
                        })));
        return root.build();
    }

    private static void status(CommandSourceStack source) {
        JadeBridge plugin = plugin();
        var sessions = plugin.sessions();
        source.getSender().sendMessage(Component.text("JadeBridge " + plugin.getPluginMeta().getVersion(), NamedTextColor.GOLD));
        send(source, "Minecraft server: " + plugin.getServer().getMinecraftVersion());
        send(source, "Jade protocol: " + JadeProtocol.PROTOCOL_VERSION);
        send(source, "Debug: " + (plugin.settings().isDebug() ? "on" : "off"));
        send(source, "Active Jade sessions: " + sessions.activeSessions());
        send(source, "Block data providers: " + plugin.providers().blockProviderCount());
        send(source, "Entity data providers: " + plugin.providers().entityProviderCount());
        send(source, "Requests served: " + sessions.totalBlockRequests() + " block, "
                + sessions.totalEntityRequests() + " entity");
        send(source, "Rejected requests: " + sessions.rejectedRequests());
    }

    private static void providers(CommandSourceStack source) {
        JadeBridge plugin = plugin();
        send(source, "Block data providers:");
        for (var id : plugin.providers().blockProviderIds()) {
            send(source, "  " + id);
        }
        send(source, "Entity data providers:");
        for (var id : plugin.providers().entityProviderIds()) {
            send(source, "  " + id);
        }
    }

    private static void reload(CommandSourceStack source) {
        JadeBridge plugin = plugin();
        plugin.settings().load(plugin);
        plugin.sessions().clearCounters();
        send(source, "Configuration reloaded. Rate limits apply to new sessions; " +
                "shearable blocks refresh on the next datapack load.");
    }

    private static void send(CommandSourceStack source, String line) {
        source.getSender().sendMessage(Component.text(line, NamedTextColor.GRAY));
    }

    private static JadeBridge plugin() {
        return JavaPlugin.getPlugin(JadeBridge.class);
    }
}

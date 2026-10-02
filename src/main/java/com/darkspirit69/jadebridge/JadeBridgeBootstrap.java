package com.darkspirit69.jadebridge;

import com.darkspirit69.jadebridge.command.JadeBridgeCommand;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;

/**
 * Paper plugins register their commands through the Brigadier lifecycle instead of
 * a command section in the descriptor; this bootstrap does that for /jadebridge.
 */
public final class JadeBridgeBootstrap implements PluginBootstrap {

    @Override
    public void bootstrap(BootstrapContext context) {
        context.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            event.registrar().register(JadeBridgeCommand.build());
        });
    }
}

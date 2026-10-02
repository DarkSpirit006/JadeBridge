package com.darkspirit69.jadebridge;

import com.darkspirit69.jadebridge.protocol.BlockRequestHandler;
import com.darkspirit69.jadebridge.protocol.EntityRequestHandler;
import com.darkspirit69.jadebridge.protocol.HandshakeHandler;
import com.darkspirit69.jadebridge.protocol.JadeMessageListener;
import com.darkspirit69.jadebridge.provider.ProviderRegistry;
import com.darkspirit69.jadebridge.provider.VanillaProviders;
import com.darkspirit69.jadebridge.session.SessionManager;
import com.darkspirit69.jadebridge.util.EffectTimes;
import com.darkspirit69.jadebridge.util.Nms;
import com.darkspirit69.jadebridge.util.ReachCheck;
import com.darkspirit69.jadebridge.util.ShearableBlocks;
import io.papermc.paper.event.server.ServerResourcesReloadedEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class JadeBridge extends JavaPlugin implements Listener {

    private Settings settings;
    private ProviderRegistry providers;
    private SessionManager sessions;

    @Override
    public void onLoad() {
        Nms.init();
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();
        settings = new Settings();
        settings.load(this);

        providers = VanillaProviders.create();
        sessions = new SessionManager();
        ReachCheck reach = new ReachCheck(settings.positionDeviation());

        HandshakeHandler handshake = new HandshakeHandler(this, settings, providers, sessions);
        BlockRequestHandler blockRequests = new BlockRequestHandler(this, settings, providers, sessions, reach);
        EntityRequestHandler entityRequests = new EntityRequestHandler(this, settings, providers, sessions, reach);
        JadeMessageListener listener = new JadeMessageListener(this, settings, handshake, blockRequests, entityRequests);

        var messenger = getServer().getMessenger();
        messenger.registerIncomingPluginChannel(this, JadeProtocol.CHANNEL_CLIENT_HANDSHAKE, listener);
        messenger.registerIncomingPluginChannel(this, JadeProtocol.CHANNEL_REQUEST_BLOCK, listener);
        messenger.registerIncomingPluginChannel(this, JadeProtocol.CHANNEL_REQUEST_ENTITY, listener);
        messenger.registerOutgoingPluginChannel(this, JadeProtocol.CHANNEL_SERVER_HANDSHAKE);
        messenger.registerOutgoingPluginChannel(this, JadeProtocol.CHANNEL_RECEIVE_DATA);

        getServer().getPluginManager().registerEvents(sessions, this);
        getServer().getPluginManager().registerEvents(listener, this);
        getServer().getPluginManager().registerEvents(this, this);
        EffectTimes effectTimes = new EffectTimes();
        EffectTimes.activate(effectTimes);
        getServer().getPluginManager().registerEvents(effectTimes, this);

        getSLF4JLogger().info("JadeBridge enabled: protocol "
                + JadeProtocol.PROTOCOL_VERSION + ", "
                + providers.blockProviderCount() + " block and "
                + providers.entityProviderCount() + " entity data providers");
    }

    @Override
    public void onDisable() {
        ShearableBlocks.invalidate();
    }

    @EventHandler
    public void onServerLoad(ServerLoadEvent event) {
        recomputeShearableBlocks();
    }

    @EventHandler
    public void onResourcesReloaded(ServerResourcesReloadedEvent event) {
        ShearableBlocks.invalidate();
        recomputeShearableBlocks();
    }

    private void recomputeShearableBlocks() {
        if (!ShearableBlocks.get().isEmpty()) {
            return;
        }
        try {
            ShearableBlocks.compute(net.minecraft.server.MinecraftServer.getServer());
            getSLF4JLogger().info("Collected " + ShearableBlocks.get().size() + " shearable blocks for the Jade handshake");
        } catch (Exception e) {
            getSLF4JLogger().warn("Could not collect shearable blocks; the handshake will carry an empty list", e);
        }
    }

    public Settings settings() {
        return settings;
    }

    public ProviderRegistry providers() {
        return providers;
    }

    public SessionManager sessions() {
        return sessions;
    }
}

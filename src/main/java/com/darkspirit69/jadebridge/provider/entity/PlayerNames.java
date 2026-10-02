package com.darkspirit69.jadebridge.provider.entity;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;

/**
 * Offline profile name resolution with the same caching behaviour as Jade's
 * PlayerNameLookup: known names come back immediately, unknown ones trigger one
 * background fetch per uuid, and unresolvable profiles get the dummy name "???".
 */
public final class PlayerNames {

    public static final String DUMMY_NAME = "???";

    private static final Set<UUID> FETCHING = ConcurrentHashMap.newKeySet();
    private static final Map<UUID, String> FETCHED = new ConcurrentHashMap<>();

    private PlayerNames() {
    }

    @Nullable
    public static String lookup(MinecraftServer server, UUID uuid) {
        String name = FETCHED.get(uuid);
        if (name != null) {
            return name;
        }
        name = server.services().nameToIdCache().get(uuid).map(NameAndId::name).orElse(null);
        if (name != null) {
            FETCHED.put(uuid, name);
            return name;
        }
        if (FETCHING.add(uuid)) {
            CompletableFuture.runAsync(
                    () -> server.services().profileResolver().fetchById(uuid).ifPresentOrElse(
                            profile -> {
                                FETCHED.put(uuid, profile.name());
                                FETCHING.remove(uuid);
                            },
                            () -> FETCHED.put(uuid, DUMMY_NAME)),
                    Util.backgroundExecutor());
        }
        return null;
    }

    public static void clear() {
        FETCHING.clear();
        FETCHED.clear();
    }
}

package com.darkspirit69.jadebridge.provider.entity;

import com.darkspirit69.jadebridge.provider.EntityContext;
import com.darkspirit69.jadebridge.provider.EntityDataProvider;
import net.minecraft.resources.Identifier;
import net.minecraft.server.permissions.Permissions;

/**
 * {@code jade:loot_table} (debug) — the loot table of entities that define one.
 * Only served to players with the gamemaster permission.
 */
public final class EntityLootTableProvider implements EntityDataProvider {

    public static final EntityLootTableProvider INSTANCE = new EntityLootTableProvider();
    public static final Identifier ID = Identifier.fromNamespaceAndPath("jade", "loot_table");

    private EntityLootTableProvider() {
    }

    @Override
    public Identifier id() {
        return ID;
    }

    @Override
    public void appendServerData(EntityContext context) {
        if (!context.player().permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) {
            return;
        }
        context.entity().getLootTable().ifPresent(key -> context.put(ID, Identifier.STREAM_CODEC, key.identifier()));
    }
}

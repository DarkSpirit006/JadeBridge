package com.darkspirit69.jadebridge.provider.entity;

import com.darkspirit69.jadebridge.provider.EntityContext;
import com.darkspirit69.jadebridge.provider.EntityDataProvider;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import org.jspecify.annotations.Nullable;

/**
 * {@code minecraft:animal_owner} — resolves the owner of pets. Online owners are
 * named directly; offline names go through the server's profile caches, with an
 * async fetch filling in unknown profiles (mirrors Jade's PlayerNameLookup).
 */
public final class AnimalOwnerProvider implements EntityDataProvider {

    public static final AnimalOwnerProvider INSTANCE = new AnimalOwnerProvider();
    public static final Identifier ID = Identifier.withDefaultNamespace("animal_owner");

    private AnimalOwnerProvider() {
    }

    @Override
    public Identifier id() {
        return ID;
    }

    @Override
    public void appendServerData(EntityContext context) {
        UUID uuid = ownerUuidOf(context.entity());
        if (uuid == null) {
            return;
        }
        Component name = nameOf(context.level(), uuid);
        if (name != null) {
            context.put(ID, ComponentSerialization.STREAM_CODEC, name);
        }
    }

    @Nullable
    public static UUID ownerUuidOf(Entity entity) {
        if (entity instanceof OwnableEntity ownable) {
            EntityReference<LivingEntity> reference = ownable.getOwnerReference();
            if (reference != null) {
                return reference.getUUID();
            }
        }
        return null;
    }

    @Nullable
    private static Component nameOf(ServerLevel level, UUID uuid) {
        Entity entity = level.getEntity(uuid);
        if (entity != null) {
            return entityName(entity);
        }
        String name = PlayerNames.lookup(level.getServer(), uuid);
        return name == null ? null : Component.literal(name);
    }

    /**
     * Same name preference as {@code ObjectNameProvider.getEntityName(entity, false)}:
     * custom name first, then the display name for players and the type name for mobs.
     */
    private static Component entityName(Entity entity) {
        Component customName = entity.getCustomName();
        if (customName != null) {
            return customName;
        }
        if (entity instanceof net.minecraft.world.entity.player.Player) {
            return entity.getDisplayName();
        }
        return entity.getName();
    }
}

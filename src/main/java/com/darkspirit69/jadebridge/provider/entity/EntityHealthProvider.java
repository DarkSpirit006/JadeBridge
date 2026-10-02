package com.darkspirit69.jadebridge.provider.entity;

import com.darkspirit69.jadebridge.provider.EntityContext;
import com.darkspirit69.jadebridge.provider.EntityDataProvider;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;

/**
 * {@code minecraft:entity_health} — the absorption amount. Current health, max
 * health and armor are already synced to clients through vanilla entity data,
 * which is why Jade only asks the server for absorption.
 */
public final class EntityHealthProvider implements EntityDataProvider {

    public static final EntityHealthProvider INSTANCE = new EntityHealthProvider();
    public static final Identifier ID = Identifier.withDefaultNamespace("entity_health");

    private EntityHealthProvider() {
    }

    @Override
    public Identifier id() {
        return ID;
    }

    @Override
    public int priority() {
        return -8000;
    }

    @Override
    public void appendServerData(EntityContext context) {
        float absorption = context.entity(LivingEntity.class).getAbsorptionAmount();
        if (absorption > 0) {
            context.put(ID, ByteBufCodecs.FLOAT, absorption);
        }
    }
}

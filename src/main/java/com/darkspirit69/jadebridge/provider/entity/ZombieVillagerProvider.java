package com.darkspirit69.jadebridge.provider.entity;

import com.darkspirit69.jadebridge.provider.EntityContext;
import com.darkspirit69.jadebridge.provider.EntityDataProvider;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.monster.zombie.ZombieVillager;

/** {@code minecraft:zombie_villager} — remaining conversion time to villager. */
public final class ZombieVillagerProvider implements EntityDataProvider {

    public static final ZombieVillagerProvider INSTANCE = new ZombieVillagerProvider();
    public static final Identifier ID = Identifier.withDefaultNamespace("zombie_villager");

    private ZombieVillagerProvider() {
    }

    @Override
    public Identifier id() {
        return ID;
    }

    @Override
    public void appendServerData(EntityContext context) {
        ZombieVillager zombieVillager = context.entity(ZombieVillager.class);
        int time = zombieVillager.villagerConversionTime;
        if (time > 0) {
            context.put(ID, ByteBufCodecs.VAR_INT, time);
        }
    }
}

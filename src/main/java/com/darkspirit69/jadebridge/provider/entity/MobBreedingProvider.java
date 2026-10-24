package com.darkspirit69.jadebridge.provider.entity;

import com.darkspirit69.jadebridge.provider.EntityContext;
import com.darkspirit69.jadebridge.provider.EntityDataProvider;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.entity.npc.villager.Villager;

/**
 * {@code minecraft:mob_breeding} — breeding cooldown of animals and villagers, the
 * duplication cooldown of allays. {@code -1} means "in love right now".
 */
public final class MobBreedingProvider implements EntityDataProvider {

    public static final int IN_LOVE = -1;

    public static final MobBreedingProvider INSTANCE = new MobBreedingProvider();
    public static final Identifier ID = Identifier.withDefaultNamespace("mob_breeding");

    private MobBreedingProvider() {
    }

    @Override
    public Identifier id() {
        return ID;
    }

    @Override
    public void appendServerData(EntityContext context) {
        Integer time = cooldownOf(context);
        if (time != null) {
            context.put(ID, ByteBufCodecs.VAR_INT, time);
        }
    }

    private static Integer cooldownOf(EntityContext context) {
        int time;
        if (context.entity() instanceof Allay allay) {
            if (allay.duplicationCooldown > 0 && allay.duplicationCooldown < Integer.MAX_VALUE) {
                time = (int) allay.duplicationCooldown;
            } else {
                return null;
            }
        } else if (context.entity() instanceof Villager villager) {
            time = villager.getAge();
        } else if (context.entity() instanceof Animal animal) {
            if (animal.isInLove()) {
                return IN_LOVE;
            }
            time = animal.getAge();
        } else {
            return null;
        }
        return time > 0 ? time : null;
    }
}

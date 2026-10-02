package com.darkspirit69.jadebridge.provider.entity;

import com.darkspirit69.jadebridge.provider.EntityContext;
import com.darkspirit69.jadebridge.provider.EntityDataProvider;
import com.darkspirit69.jadebridge.util.Nms;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.animal.frog.Tadpole;

/**
 * {@code minecraft:mob_growth} — ticks until a baby animal or tadpole grows up.
 * The client only asks for babies that are not age-locked.
 */
public final class MobGrowthProvider implements EntityDataProvider {

    public static final MobGrowthProvider INSTANCE = new MobGrowthProvider();
    public static final Identifier ID = Identifier.withDefaultNamespace("mob_growth");

    private MobGrowthProvider() {
    }

    @Override
    public Identifier id() {
        return ID;
    }

    @Override
    public void appendServerData(EntityContext context) {
        int time = growthTime(context);
        if (time > 0) {
            context.put(ID, ByteBufCodecs.VAR_INT, time);
        }
    }

    private static int growthTime(EntityContext context) {
        if (context.entity() instanceof AgeableMob ageable) {
            return -ageable.getAge();
        }
        if (context.entity() instanceof Tadpole tadpole) {
            return Nms.tadpoleTicksLeft(tadpole);
        }
        return -1;
    }
}

package com.darkspirit69.jadebridge.provider.entity;

import com.darkspirit69.jadebridge.provider.EntityContext;
import com.darkspirit69.jadebridge.provider.EntityDataProvider;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.animal.golem.CopperGolem;

/**
 * {@code minecraft:waxed} — present (as {@link Unit#INSTANCE}) when a copper golem
 * is waxed and will not weather further. The block-side waxed check reads synced
 * block entity data on the client.
 */
public final class WaxedProvider implements EntityDataProvider {

    public static final WaxedProvider INSTANCE = new WaxedProvider();
    public static final Identifier ID = Identifier.withDefaultNamespace("waxed");

    private WaxedProvider() {
    }

    @Override
    public Identifier id() {
        return ID;
    }

    @Override
    public int priority() {
        return 10;
    }

    @Override
    public void appendServerData(EntityContext context) {
        CopperGolem golem = context.entity(CopperGolem.class);
        if (golem.nextWeatheringTick == CopperGolem.IGNORE_WEATHERING_TICK) {
            context.put(ID, Unit.STREAM_CODEC, Unit.INSTANCE);
        }
    }
}

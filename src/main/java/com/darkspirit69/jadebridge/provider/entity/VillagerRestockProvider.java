package com.darkspirit69.jadebridge.provider.entity;

import com.darkspirit69.jadebridge.provider.EntityContext;
import com.darkspirit69.jadebridge.provider.EntityDataProvider;
import com.darkspirit69.jadebridge.util.Nms;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.trading.MerchantOffer;

/**
 * {@code minecraft:villager_restock} — restocks used today and ticks until the next
 * one, sent only for villagers that used a trade at least once. Added in Jade 26.3.
 */
public final class VillagerRestockProvider implements EntityDataProvider {

    private static final int MAX_RESTOCKS_PER_DAY = 2;
    private static final long RESTOCK_COOLDOWN = 2400L;
    private static final long NEW_DAY_INTERVAL = 12000L;

    public static final VillagerRestockProvider INSTANCE = new VillagerRestockProvider();
    public static final Identifier ID = Identifier.withDefaultNamespace("villager_restock");

    private record Data(int restocksToday, int ticksUntilRestock) {
        static final StreamCodec<ByteBuf, Data> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT,
                Data::restocksToday,
                ByteBufCodecs.VAR_INT,
                Data::ticksUntilRestock,
                Data::new);
    }

    private VillagerRestockProvider() {
    }

    @Override
    public Identifier id() {
        return ID;
    }

    @Override
    public void appendServerData(EntityContext context) {
        Villager villager = context.entity(Villager.class);
        boolean traded = false;
        for (MerchantOffer offer : villager.getOffers()) {
            if (offer.getUses() > 0) {
                traded = true;
                break;
            }
        }
        if (!traded) {
            return;
        }
        int restocksToday = villager.numberOfRestocksToday;
        long remaining = 0;
        if (restocksToday > 0) {
            long lastRestock = Nms.villagerLastRestock(villager);
            if (lastRestock < 0) {
                return;
            }
            long interval = restocksToday < MAX_RESTOCKS_PER_DAY ? RESTOCK_COOLDOWN : NEW_DAY_INTERVAL;
            remaining = Math.max(0L, lastRestock + interval - villager.level().getGameTime());
        }
        context.put(ID, Data.CODEC, new Data(restocksToday, (int) remaining));
    }
}

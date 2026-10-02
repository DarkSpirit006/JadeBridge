package com.darkspirit69.jadebridge.provider.block;

import com.darkspirit69.jadebridge.provider.BlockContext;
import com.darkspirit69.jadebridge.provider.BlockDataProvider;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;

/**
 * {@code minecraft:beehive} — bee count as a signed byte: negative when the hive
 * is not full, positive when it is. The honey level itself is block state data the
 * client already has.
 */
public final class BeehiveProvider implements BlockDataProvider {

    public static final BeehiveProvider INSTANCE = new BeehiveProvider();
    public static final Identifier ID = Identifier.withDefaultNamespace("beehive");

    private BeehiveProvider() {
    }

    @Override
    public Identifier id() {
        return ID;
    }

    @Override
    public void appendServerData(BlockContext context) {
        BeehiveBlockEntity beehive = context.blockEntity(BeehiveBlockEntity.class);
        int bees = beehive.getOccupantCount();
        byte value = (byte) (beehive.isFull() ? bees : -bees);
        context.put(ID, ByteBufCodecs.BYTE, value);
    }
}

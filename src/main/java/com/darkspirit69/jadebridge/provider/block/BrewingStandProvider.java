package com.darkspirit69.jadebridge.provider.block;

import com.darkspirit69.jadebridge.provider.BlockContext;
import com.darkspirit69.jadebridge.provider.BlockDataProvider;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;

/** {@code minecraft:brewing_stand} — fuel and remaining brew time. */
public final class BrewingStandProvider implements BlockDataProvider {

    public static final BrewingStandProvider INSTANCE = new BrewingStandProvider();
    public static final Identifier ID = Identifier.withDefaultNamespace("brewing_stand");

    private record Data(int fuel, int time) {
        static final StreamCodec<ByteBuf, Data> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT,
                Data::fuel,
                ByteBufCodecs.VAR_INT,
                Data::time,
                Data::new);
    }

    private BrewingStandProvider() {
    }

    @Override
    public Identifier id() {
        return ID;
    }

    @Override
    public void appendServerData(BlockContext context) {
        BrewingStandBlockEntity brewingStand = context.blockEntity(BrewingStandBlockEntity.class);
        context.put(ID, Data.CODEC, new Data(brewingStand.fuel, brewingStand.brewTime));
    }
}

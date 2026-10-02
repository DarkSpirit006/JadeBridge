package com.darkspirit69.jadebridge.provider.block;

import com.darkspirit69.jadebridge.provider.BlockContext;
import com.darkspirit69.jadebridge.provider.BlockDataProvider;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;

/** {@code minecraft:furnace} — cooking progress plus the three furnace slots. */
public final class FurnaceProvider implements BlockDataProvider {

    public static final FurnaceProvider INSTANCE = new FurnaceProvider();
    public static final Identifier ID = Identifier.withDefaultNamespace("furnace");

    private record Data(int progress, int total, List<ItemStack> inventory) {
        static final StreamCodec<RegistryFriendlyByteBuf, Data> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT,
                Data::progress,
                ByteBufCodecs.VAR_INT,
                Data::total,
                ItemStack.OPTIONAL_LIST_STREAM_CODEC,
                Data::inventory,
                Data::new);
    }

    private FurnaceProvider() {
    }

    @Override
    public Identifier id() {
        return ID;
    }

    @Override
    public void appendServerData(BlockContext context) {
        AbstractFurnaceBlockEntity furnace = context.blockEntity(AbstractFurnaceBlockEntity.class);
        Data data = new Data(
                furnace.cookingTimer,
                furnace.cookingTotalTime,
                List.of(furnace.getItem(0), furnace.getItem(1), furnace.getItem(2)));
        context.put(ID, Data.CODEC, data);
    }
}

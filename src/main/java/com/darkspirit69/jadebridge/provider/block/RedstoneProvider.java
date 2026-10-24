package com.darkspirit69.jadebridge.provider.block;

import com.darkspirit69.jadebridge.provider.BlockContext;
import com.darkspirit69.jadebridge.provider.BlockDataProvider;
import net.minecraft.core.Direction;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.CalibratedSculkSensorBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.CalibratedSculkSensorBlockEntity;
import net.minecraft.world.level.block.entity.ComparatorBlockEntity;
import org.jspecify.annotations.Nullable;

/**
 * {@code minecraft:redstone} — the comparator's stored output signal, or the input
 * signal behind a calibrated sculk sensor. Levers, repeaters and plain power levels
 * are block state data the client reads on its own.
 */
public final class RedstoneProvider implements BlockDataProvider {

    public static final RedstoneProvider INSTANCE = new RedstoneProvider();
    public static final Identifier ID = Identifier.withDefaultNamespace("redstone");

    private RedstoneProvider() {
    }

    @Override
    public Identifier id() {
        return ID;
    }

    @Override
    public void appendServerData(BlockContext context) {
        Integer signal = signalOf(context);
        if (signal != null) {
            context.put(ID, ByteBufCodecs.VAR_INT, signal);
        }
    }

    @Nullable
    private static Integer signalOf(BlockContext context) {
        BlockEntity blockEntity = context.blockEntity();
        if (blockEntity instanceof ComparatorBlockEntity comparator) {
            return comparator.getOutputSignal();
        }
        if (blockEntity instanceof CalibratedSculkSensorBlockEntity) {
            Direction direction = context.state().getValue(CalibratedSculkSensorBlock.FACING).getOpposite();
            return context.level().getSignal(context.pos().relative(direction), direction);
        }
        return null;
    }
}

package com.darkspirit69.jadebridge.provider.block;

import com.darkspirit69.jadebridge.provider.BlockContext;
import com.darkspirit69.jadebridge.provider.BlockDataProvider;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/** {@code minecraft:hopper_lock} — whether the hopper is disabled by redstone. */
public final class HopperLockProvider implements BlockDataProvider {

    public static final HopperLockProvider INSTANCE = new HopperLockProvider();
    public static final Identifier ID = Identifier.withDefaultNamespace("hopper_lock");

    private HopperLockProvider() {
    }

    @Override
    public Identifier id() {
        return ID;
    }

    @Override
    public void appendServerData(BlockContext context) {
        boolean locked = !context.state().getValue(BlockStateProperties.ENABLED);
        context.put(ID, ByteBufCodecs.BOOL, locked);
    }
}

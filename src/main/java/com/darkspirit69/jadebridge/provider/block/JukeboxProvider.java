package com.darkspirit69.jadebridge.provider.block;

import com.darkspirit69.jadebridge.provider.BlockContext;
import com.darkspirit69.jadebridge.provider.BlockDataProvider;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;

/** {@code minecraft:jukebox} — the disc currently in the jukebox. */
public final class JukeboxProvider implements BlockDataProvider {

    public static final JukeboxProvider INSTANCE = new JukeboxProvider();
    public static final Identifier ID = Identifier.withDefaultNamespace("jukebox");

    private JukeboxProvider() {
    }

    @Override
    public Identifier id() {
        return ID;
    }

    @Override
    public void appendServerData(BlockContext context) {
        ItemStack disc = context.blockEntity(JukeboxBlockEntity.class).getTheItem();
        context.put(ID, ItemStack.OPTIONAL_STREAM_CODEC, disc);
    }
}

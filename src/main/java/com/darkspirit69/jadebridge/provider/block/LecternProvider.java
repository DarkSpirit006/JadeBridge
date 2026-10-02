package com.darkspirit69.jadebridge.provider.block;

import com.darkspirit69.jadebridge.provider.BlockContext;
import com.darkspirit69.jadebridge.provider.BlockDataProvider;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.LecternBlockEntity;

/** {@code minecraft:lectern} — the book on the lectern. */
public final class LecternProvider implements BlockDataProvider {

    public static final LecternProvider INSTANCE = new LecternProvider();
    public static final Identifier ID = Identifier.withDefaultNamespace("lectern");

    private LecternProvider() {
    }

    @Override
    public Identifier id() {
        return ID;
    }

    @Override
    public void appendServerData(BlockContext context) {
        ItemStack book = context.blockEntity(LecternBlockEntity.class).getBook();
        context.put(ID, ItemStack.OPTIONAL_STREAM_CODEC, book);
    }
}

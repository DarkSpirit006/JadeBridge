package com.darkspirit69.jadebridge.provider.block;

import com.darkspirit69.jadebridge.provider.BlockContext;
import com.darkspirit69.jadebridge.provider.BlockDataProvider;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.SelectableSlotContainer;
import net.minecraft.world.level.block.entity.ListBackedContainer;

/**
 * {@code minecraft:shelf} — the item in the bookshelf slot the player is looking at.
 * Chiseled bookshelves and shelves both expose their hit slot through
 * {@link SelectableSlotContainer}.
 */
public final class ShelfProvider implements BlockDataProvider {

    public static final ShelfProvider INSTANCE = new ShelfProvider();
    public static final Identifier ID = Identifier.withDefaultNamespace("shelf");

    private ShelfProvider() {
    }

    @Override
    public Identifier id() {
        return ID;
    }

    @Override
    public void appendServerData(BlockContext context) {
        if (!(context.state().getBlock() instanceof SelectableSlotContainer container)) {
            return;
        }
        int slot = container.getHitSlot(context.hit(), context.hit().getDirection()).orElse(-1);
        if (slot == -1) {
            return;
        }
        ItemStack item = ((ListBackedContainer) context.blockEntity()).getItem(slot);
        context.put(ID, ItemStack.OPTIONAL_STREAM_CODEC, item);
    }
}

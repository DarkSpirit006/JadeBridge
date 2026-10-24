package com.darkspirit69.jadebridge.provider.block;

import com.darkspirit69.jadebridge.provider.BlockContext;
import com.darkspirit69.jadebridge.provider.BlockDataProvider;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.Nameable;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.properties.ChestType;
import org.jspecify.annotations.Nullable;

/**
 * {@code jade:object_name} — the display name of named block entities (renamed
 * chests, furnaces, ...). Port of {@code ObjectNameProvider.BlockData}.
 */
public final class ObjectNameProvider implements BlockDataProvider {

    public static final ObjectNameProvider INSTANCE = new ObjectNameProvider();
    public static final Identifier ID = Identifier.fromNamespaceAndPath("jade", "object_name");

    private ObjectNameProvider() {
    }

    @Override
    public Identifier id() {
        return ID;
    }

    @Override
    public void appendServerData(BlockContext context) {
        BlockEntity blockEntity = context.blockEntity();
        if (!(blockEntity instanceof Nameable nameable)) {
            return;
        }
        Component name = nameOf(context, nameable);
        if (name != null) {
            context.put(ID, ComponentSerialization.STREAM_CODEC, name);
        }
    }

    @Nullable
    private static Component nameOf(BlockContext context, Nameable nameable) {
        if (nameable instanceof ChestBlockEntity
                && context.state().getBlock() instanceof ChestBlock
                && context.state().getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
            MenuProvider menuProvider = context.state().getMenuProvider(context.level(), context.pos());
            if (menuProvider != null) {
                Component name = menuProvider.getDisplayName();
                if (!(name.getContents() instanceof TranslatableContents contents)
                        || !"container.chestDouble".equals(contents.getKey())) {
                    return name;
                }
            }
        }
        if (nameable.hasCustomName()) {
            return nameable.getDisplayName();
        }
        return context.blockEntity().components().get(DataComponents.ITEM_NAME);
    }
}

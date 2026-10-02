package com.darkspirit69.jadebridge.provider;

import java.util.List;
import java.util.Map;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Container;
import net.minecraft.world.LockCode;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.ContainerEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.WorldlyContainerHolder;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.EnderChestBlockEntity;
import org.jspecify.annotations.Nullable;

/**
 * {@code minecraft:item_storage} — inventories of containers, chests, minecarts,
 * horses and the viewer's own ender chest. Registered on both the block and the
 * entity side, like Jade's {@code ItemStorageProvider.BLOCK/.ENTITY}.
 */
public final class ItemStorageProvider implements BlockDataProvider, EntityDataProvider {

    public static final ItemStorageProvider BLOCK = new ItemStorageProvider();
    public static final ItemStorageProvider ENTITY = new ItemStorageProvider();

    public static final Identifier ID = Identifier.withDefaultNamespace("item_storage");
    private static final Identifier DEFAULT_EXTENSION_ID = Identifier.withDefaultNamespace("item_storage.default");

    /**
     * ItemStack codec with Jade's {@code __JadeCount} convention: stacks larger than
     * 99 travel as a 99-count stack carrying the real count in custom data.
     */
    static final StreamCodec<RegistryFriendlyByteBuf, ItemStack> ITEM_CODEC = ItemStack.OPTIONAL_STREAM_CODEC.map(
            stack -> {
                if (stack.count() < 99 || !stack.has(DataComponents.CUSTOM_DATA)) {
                    return stack;
                }
                int count = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getIntOr("__JadeCount", 99);
                stack.setCount(count);
                return stack;
            },
            stack -> {
                int count = stack.count();
                if (count <= 99) {
                    return stack;
                }
                stack = stack.copyWithCount(99);
                CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                        .update(tag -> tag.putInt("__JadeCount", count));
                stack.set(DataComponents.CUSTOM_DATA, data);
                return stack;
            });

    static final StreamCodec<RegistryFriendlyByteBuf, Map.Entry<Identifier, List<ViewGroup<ItemStack>>>> CODEC =
            ViewGroup.listCodec(ITEM_CODEC);

    private ItemStorageProvider() {
    }

    @Override
    public Identifier id() {
        return ID;
    }

    @Override
    public int priority() {
        return 1000;
    }

    @Override
    public void appendServerData(BlockContext context) {
        if (context.blockEntity() instanceof AbstractFurnaceBlockEntity) {
            return;
        }
        Container container = containerFor(context);
        if (container == null) {
            return;
        }
        boolean sorted = context.data().getBooleanOr("SortItems", false);
        List<ViewGroup<ItemStack>> groups = List.of(new ViewGroup<>(ItemCollector.collect(container, sorted)));
        context.put(ID, CODEC, Map.entry(DEFAULT_EXTENSION_ID, groups));
    }

    @Override
    public void appendServerData(EntityContext context) {
        Container container = containerFor(context);
        if (container == null) {
            return;
        }
        boolean sorted = context.data().getBooleanOr("SortItems", false);
        List<ViewGroup<ItemStack>> groups = List.of(new ViewGroup<>(ItemCollector.collect(container, sorted)));
        context.put(ID, CODEC, Map.entry(DEFAULT_EXTENSION_ID, groups));
    }

    @Nullable
    private static Container containerFor(BlockContext context) {
        BlockEntity blockEntity = context.blockEntity();
        if (blockEntity instanceof RandomizableContainer container && container.getLootTable() != null) {
            context.data().putBoolean("Loot", true);
            return null;
        }
        Player player = context.player();
        if (!player.isCreative() && !player.isSpectator()
                && blockEntity instanceof BaseContainerBlockEntity base && base.lockKey != LockCode.NO_LOCK) {
            context.data().putBoolean("Locked", true);
            return null;
        }
        if (blockEntity instanceof EnderChestBlockEntity) {
            return player.getEnderChestInventory();
        }
        if (blockEntity instanceof ChestBlockEntity chest && chest.getBlockState().getBlock() instanceof ChestBlock chestBlock) {
            Container compound = ChestBlock.getContainer(
                    chestBlock,
                    chest.getBlockState(),
                    context.level(),
                    chest.getBlockPos(),
                    true);
            if (compound != null) {
                return compound;
            }
        }
        if (blockEntity instanceof Container container) {
            return container;
        }
        if (blockEntity == null && context.state().getBlock() instanceof WorldlyContainerHolder holder) {
            return holder.getContainer(context.state(), context.level(), context.pos());
        }
        return null;
    }

    @Nullable
    private static Container containerFor(EntityContext context) {
        Entity entity = context.entity();
        if (entity instanceof Player || entity instanceof ArmorStand) {
            return null;
        }
        if (entity instanceof AbstractHorse horse) {
            return horse.inventory;
        }
        if (entity instanceof ContainerEntity containerEntity && containerEntity.getContainerLootTable() != null) {
            context.data().putBoolean("Loot", true);
            return null;
        }
        if (entity instanceof Container container) {
            return container;
        }
        return null;
    }
}

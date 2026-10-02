package com.darkspirit69.jadebridge.provider;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;

/**
 * Registry of server data providers plus the id mapping used on the wire.
 * Ids are assigned in registration order; the client adopts this order during the
 * handshake (Jade's {@code remapIds}), so only the ids themselves have to match Jade's.
 * Lookups walk the target class hierarchy base class first and then stable-sort by
 * priority, the same ordering Jade's HierarchyLookup produces.
 */
public final class ProviderRegistry {

    private record Entry(Class<?> targetClass, JadeProvider provider) {
    }

    private final List<Entry> blockEntries = new ArrayList<>();
    private final List<Entry> entityEntries = new ArrayList<>();
    private final List<BlockDataProvider> blockById = new ArrayList<>();
    private final List<EntityDataProvider> entityById = new ArrayList<>();

    public void registerBlock(Class<?> targetClass, BlockDataProvider provider) {
        blockEntries.add(new Entry(targetClass, provider));
        blockById.add(provider);
    }

    public void registerEntity(Class<?> targetClass, EntityDataProvider provider) {
        entityEntries.add(new Entry(targetClass, provider));
        entityById.add(provider);
    }

    @Nullable
    public BlockDataProvider blockProvider(int id) {
        return id >= 0 && id < blockById.size() ? blockById.get(id) : null;
    }

    @Nullable
    public EntityDataProvider entityProvider(int id) {
        return id >= 0 && id < entityById.size() ? entityById.get(id) : null;
    }

    public List<Identifier> blockProviderIds() {
        return blockById.stream().map(JadeProvider::id).toList();
    }

    public List<Identifier> entityProviderIds() {
        return entityById.stream().map(JadeProvider::id).toList();
    }

    /**
     * Providers applicable to a block target: registered against any class in the
     * block's or its block entity's hierarchy. Block-side registrations run before
     * block-entity-side ones, mirroring Jade's PairHierarchyLookup merge.
     */
    public List<BlockDataProvider> providersFor(Class<? extends Block> blockClass, @Nullable Class<? extends BlockEntity> blockEntityClass) {
        List<BlockDataProvider> result = new ArrayList<>();
        collect(blockEntries, blockClass, result);
        if (blockEntityClass != null) {
            collect(blockEntries, blockEntityClass, result);
        }
        result.sort(Comparator.comparingInt(JadeProvider::priority));
        return result;
    }

    public List<EntityDataProvider> providersFor(Class<? extends Entity> entityClass) {
        List<EntityDataProvider> result = new ArrayList<>();
        collect(entityEntries, entityClass, result);
        result.sort(Comparator.comparingInt(JadeProvider::priority));
        return result;
    }

    @SuppressWarnings("unchecked")
    private static <P extends JadeProvider> void collect(List<Entry> entries, Class<?> targetClass, List<P> result) {
        if (targetClass != null && targetClass != Object.class) {
            collect(entries, targetClass.getSuperclass(), result);
        }
        for (Entry entry : entries) {
            if (entry.targetClass() == targetClass) {
                result.add((P) entry.provider());
            }
        }
    }

    public int blockProviderCount() {
        return blockById.size();
    }

    public int entityProviderCount() {
        return entityById.size();
    }
}

package com.darkspirit69.jadebridge.util;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.advancements.predicates.ItemPredicate;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ReloadableServerRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.AlternativesEntry;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.minecraft.world.level.storage.loot.entries.NestedLootTable;
import net.minecraft.world.level.storage.loot.predicates.AnyOfCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.MatchTool;
import org.jspecify.annotations.Nullable;

/**
 * Collects every block whose loot table drops something when broken with shears,
 * the list Jade's {@code LootTableMineableCollector} builds for the harvest tool
 * feature. Runs once per server load and after datapack reloads; the loot
 * structure fields are private, so this class owns the reflection for them.
 */
public final class ShearableBlocks {

    private static volatile List<Block> blocks = List.of();

    private static final Field POOLS = field(LootTable.class, "pools");
    private static final Field ENTRIES = field(LootPool.class, "entries");
    private static final Field POOL_CONDITION = field(LootPool.class, "condition");
    private static final Field ENTRY_CONDITION = field(LootPoolEntryContainer.class, "condition");
    private static final Field CHILDREN = field(AlternativesEntry.class.getSuperclass(), "children");
    private static final Field TERMS = field(AnyOfCondition.class.getSuperclass(), "terms");
    private static final Field NESTED_TABLES = field(NestedLootTable.class, "value");

    private ShearableBlocks() {
    }

    public static List<Block> get() {
        return blocks;
    }

    public static void invalidate() {
        blocks = List.of();
    }

    public static void compute(MinecraftServer server) {
        ItemStack shears = Items.SHEARS.getDefaultInstance();
        ReloadableServerRegistries.Holder lootRegistries = server.reloadableRegistries();
        List<Block> result = new ArrayList<>();
        for (Block block : BuiltInRegistries.BLOCK) {
            Optional<ResourceKey<LootTable>> key = block.getLootTable();
            if (key.isPresent() && matches(lootRegistries.getLootTable(key.get()), lootRegistries, shears)) {
                result.add(block);
            }
        }
        result.add(Blocks.TRIPWIRE);
        blocks = List.copyOf(result);
    }

    private static boolean matches(
            @Nullable LootTable lootTable,
            ReloadableServerRegistries.Holder lootRegistries,
            ItemStack tool) {
        if (lootTable == null || lootTable == LootTable.EMPTY) {
            return false;
        }
        for (LootPool pool : poolsOf(lootTable)) {
            if (matchesPool(pool, lootRegistries, tool)) {
                return true;
            }
        }
        return false;
    }

    private static boolean matchesPool(
            LootPool pool,
            ReloadableServerRegistries.Holder lootRegistries,
            ItemStack tool) {
        List<LootPoolEntryContainer> entries = entriesOf(pool);
        if (entries.isEmpty()) {
            return false;
        }
        LootItemCondition poolCondition = conditionOf(POOL_CONDITION, pool);
        if (poolCondition != null && isCorrectConditions(poolCondition, tool)) {
            return true;
        }
        for (LootPoolEntryContainer entry : entries) {
            if (matchesEntry(entry, lootRegistries, tool)) {
                return true;
            }
        }
        return false;
    }

    private static boolean matchesEntry(
            LootPoolEntryContainer entry,
            ReloadableServerRegistries.Holder lootRegistries,
            ItemStack tool) {
        if (entry instanceof AlternativesEntry) {
            for (LootPoolEntryContainer child : childrenOf(entry)) {
                if (matchesEntry(child, lootRegistries, tool)) {
                    return true;
                }
            }
            return false;
        }
        if (entry instanceof NestedLootTable nested) {
            HolderSet<LootTable> tables = value(NESTED_TABLES, nested);
            if (tables != null) {
                for (Holder<LootTable> table : tables) {
                    if (matches(table.value(), lootRegistries, tool)) {
                        return true;
                    }
                }
            }
            return false;
        }
        LootItemCondition condition = conditionOf(ENTRY_CONDITION, entry);
        return condition != null && isCorrectConditions(condition, tool);
    }

    /**
     * A block counts as shearable when the entry's single condition is a match-tool
     * test that the shears pass, possibly nested inside an any-of condition.
     */
    private static boolean isCorrectConditions(LootItemCondition condition, ItemStack tool) {
        if (condition instanceof MatchTool matchTool) {
            ItemPredicate predicate = matchTool.predicate().orElse(null);
            return predicate != null && predicate.test(tool);
        }
        if (condition instanceof AnyOfCondition) {
            HolderSet<LootItemCondition> terms = value(TERMS, condition);
            if (terms != null) {
                for (Holder<LootItemCondition> child : terms) {
                    if (isCorrectConditions(child.value(), tool)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Nullable
    private static LootItemCondition conditionOf(@Nullable Field field, Object owner) {
        Optional<Holder<LootItemCondition>> condition = value(field, owner);
        return condition == null ? null : condition.map(Holder::value).orElse(null);
    }

    private static List<LootPool> poolsOf(LootTable table) {
        List<LootPool> pools = value(POOLS, table);
        return pools == null ? List.of() : pools;
    }

    private static List<LootPoolEntryContainer> entriesOf(LootPool pool) {
        List<LootPoolEntryContainer> entries = value(ENTRIES, pool);
        return entries == null ? List.of() : entries;
    }

    private static List<LootPoolEntryContainer> childrenOf(LootPoolEntryContainer entry) {
        List<LootPoolEntryContainer> children = value(CHILDREN, entry);
        return children == null ? List.of() : children;
    }

    @Nullable
    @SuppressWarnings("unchecked")
    private static <T> T value(@Nullable Field field, Object owner) {
        if (field == null) {
            return null;
        }
        try {
            return (T) field.get(owner);
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    @Nullable
    private static Field field(Class<?> owner, String name) {
        try {
            Field field = owner.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }
}

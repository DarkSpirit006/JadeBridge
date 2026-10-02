package com.darkspirit69.jadebridge.provider;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TooltipDisplay;

/**
 * Reads a container the way Jade's {@code ItemCollector} does: filter hidden stacks,
 * merge identical item definitions, cap at 54 distinct stacks and optionally sort by
 * count. Vanilla containers never exceed the 108-slot scan budget, so a single pass
 * is always complete.
 */
public final class ItemCollector {

    public static final int MAX_SIZE = 54;
    public static final int SCAN_LIMIT = MAX_SIZE * 2;

    private static final CompoundTag CLEAR_MARKER = new CompoundTag();

    static {
        CLEAR_MARKER.putBoolean("__JadeClear", true);
    }

    private record ItemDefinition(Item item, DataComponentPatch components) {

        ItemDefinition(ItemStack stack) {
            this(stack.getItem(), stack.getComponentsPatch());
        }

        ItemStack toStack(int count) {
            ItemStack stack = new ItemStack(item, count);
            stack.applyComponents(components);
            return stack;
        }
    }

    private ItemCollector() {
    }

    public static List<ItemStack> collect(Container container, boolean sorted) {
        Map<ItemDefinition, Integer> counts = new LinkedHashMap<>();
        int scanned = 0;
        int slots = container.getContainerSize();
        for (int slot = 0; slot < slots && scanned < SCAN_LIMIT; slot++) {
            ItemStack stack = container.getItem(slot);
            scanned++;
            if (!isShown(stack)) {
                continue;
            }
            counts.merge(new ItemDefinition(stack), stack.count(), Integer::sum);
        }

        List<ItemDefinition> definitions = new ArrayList<>(counts.keySet());
        if (sorted) {
            definitions.sort((a, b) -> -Integer.compare(counts.get(a), counts.get(b)));
        }
        List<ItemStack> views = new ArrayList<>(Math.min(definitions.size(), MAX_SIZE));
        for (ItemDefinition definition : definitions) {
            if (views.size() >= MAX_SIZE) {
                break;
            }
            views.add(definition.toStack(counts.get(definition)));
        }
        return views;
    }

    private static boolean isShown(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (stack.getOrDefault(DataComponents.TOOLTIP_DISPLAY, TooltipDisplay.DEFAULT).hideTooltip()) {
            return false;
        }
        if (stack.hasNonDefault(DataComponents.CUSTOM_MODEL_DATA) || stack.hasNonDefault(DataComponents.ITEM_MODEL)) {
            CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
            return !customData.matchedBy(CLEAR_MARKER);
        }
        return true;
    }
}

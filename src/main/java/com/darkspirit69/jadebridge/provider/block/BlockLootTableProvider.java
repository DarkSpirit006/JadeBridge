package com.darkspirit69.jadebridge.provider.block;

import com.darkspirit69.jadebridge.provider.BlockContext;
import com.darkspirit69.jadebridge.provider.BlockDataProvider;
import com.mojang.datafixers.util.Pair;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.RandomizableContainer;

/**
 * {@code jade:loot_table} (debug) — the loot table of unopened containers.
 * Only served to players with the gamemaster permission, like Jade's provider.
 */
public final class BlockLootTableProvider implements BlockDataProvider {

    public static final BlockLootTableProvider INSTANCE = new BlockLootTableProvider();
    public static final Identifier ID = Identifier.fromNamespaceAndPath("jade", "loot_table");

    private static final StreamCodec<RegistryFriendlyByteBuf, Pair<Identifier, Long>> CODEC = StreamCodec.composite(
            Identifier.STREAM_CODEC,
            Pair::getFirst,
            ByteBufCodecs.LONG,
            Pair::getSecond,
            Pair::new);

    private BlockLootTableProvider() {
    }

    @Override
    public Identifier id() {
        return ID;
    }

    @Override
    public void appendServerData(BlockContext context) {
        if (!context.player().permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) {
            return;
        }
        if (context.blockEntity() instanceof RandomizableContainer container && container.getLootTable() != null) {
            context.put(ID, CODEC, Pair.of(container.getLootTable().identifier(), container.getLootTableSeed()));
        }
    }
}

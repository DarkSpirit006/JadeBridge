package com.darkspirit69.jadebridge.provider.block;

import com.darkspirit69.jadebridge.provider.BlockContext;
import com.darkspirit69.jadebridge.provider.BlockDataProvider;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.CommandBlockEntity;

/**
 * {@code minecraft:command_block} — the command, only for operators
 * ({@code canUseGameMasterBlocks}), truncated like the Jade client displays it.
 */
public final class CommandBlockProvider implements BlockDataProvider {

    public static final CommandBlockProvider INSTANCE = new CommandBlockProvider();
    public static final Identifier ID = Identifier.withDefaultNamespace("command_block");

    private CommandBlockProvider() {
    }

    @Override
    public Identifier id() {
        return ID;
    }

    @Override
    public void appendServerData(BlockContext context) {
        if (!context.player().canUseGameMasterBlocks()) {
            return;
        }
        String command = context.blockEntity(CommandBlockEntity.class).getCommandBlock().getCommand();
        if (command.length() > 40) {
            command = command.substring(0, 37) + "...";
        }
        context.put(ID, ByteBufCodecs.STRING_UTF8, command);
    }
}

package com.darkspirit69.jadebridge.provider.block;

import com.darkspirit69.jadebridge.provider.BlockContext;
import com.darkspirit69.jadebridge.provider.BlockDataProvider;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.TrialSpawnerBlockEntity;
import net.minecraft.world.level.block.entity.trialspawner.TrialSpawnerState;

/**
 * {@code minecraft:mob_spawner.cooldown} — remaining cooldown of a trial spawner,
 * only sent while the spawner is actually cooling down. Regular spawner data is
 * synced to clients natively and needs no server provider.
 */
public final class TrialSpawnerProvider implements BlockDataProvider {

    public static final TrialSpawnerProvider INSTANCE = new TrialSpawnerProvider();
    public static final Identifier ID = Identifier.withDefaultNamespace("mob_spawner.cooldown");

    private TrialSpawnerProvider() {
    }

    @Override
    public Identifier id() {
        return ID;
    }

    @Override
    public void appendServerData(BlockContext context) {
        TrialSpawnerBlockEntity spawner = context.blockEntity(TrialSpawnerBlockEntity.class);
        if (spawner.getState() != TrialSpawnerState.COOLDOWN) {
            return;
        }
        var stateData = spawner.getTrialSpawner().getStateData();
        long cooldownEndsAt = stateData.cooldownEndsAt;
        ServerLevel level = context.level();
        if (spawner.getTrialSpawner().canSpawnInLevel(level) && level.getGameTime() < cooldownEndsAt) {
            context.put(ID, ByteBufCodecs.VAR_INT, (int) (cooldownEndsAt - level.getGameTime()));
        }
    }
}

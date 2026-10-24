package com.darkspirit69.jadebridge.util;

import java.util.Objects;
import java.util.function.IntSupplier;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;

/**
 * Jade's server-side reach check: squared distance against the interaction range
 * plus a position-deviation slack (default 21).
 *
 * Jade's own server registers the {@code jade:max_position_deviation} gamerule for
 * this slack. That is not possible from a Paper plugin: the minecraft:game_rule
 * registry is already frozen when plugin lifecycle starts (verified against Paper
 * 26.2 — registration throws "Registry is already frozen"). The deviation is
 * therefore configurable through JadeBridge's config instead.
 */
public final class ReachCheck {

    private final IntSupplier deviation;

    public ReachCheck(IntSupplier deviation) {
        this.deviation = Objects.requireNonNull(deviation, "deviation");
    }

    public boolean isOutOfReach(ServerPlayer player, BlockPos pos, double baseReach) {
        double maxDistance = Mth.square(baseReach + Math.max(0, deviation.getAsInt()));
        return pos.distSqr(player.blockPosition()) > maxDistance;
    }
}

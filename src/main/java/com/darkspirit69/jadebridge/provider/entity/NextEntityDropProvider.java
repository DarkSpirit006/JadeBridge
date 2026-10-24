package com.darkspirit69.jadebridge.provider.entity;

import com.darkspirit69.jadebridge.provider.EntityContext;
import com.darkspirit69.jadebridge.provider.EntityDataProvider;
import com.darkspirit69.jadebridge.util.Nms;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.animal.armadillo.Armadillo;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.animal.sniffer.Sniffer;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

/**
 * {@code minecraft:next_entity_drop} — raw tag ints for the next egg, scute or
 * sniff, written straight into the response like Jade does.
 */
public final class NextEntityDropProvider implements EntityDataProvider {

    public static final NextEntityDropProvider INSTANCE = new NextEntityDropProvider();
    public static final Identifier ID = Identifier.withDefaultNamespace("next_entity_drop");

    private static final int MAX_TRACKED_TICKS = 24000 * 2;

    private NextEntityDropProvider() {
    }

    @Override
    public Identifier id() {
        return ID;
    }

    @Override
    public void appendServerData(EntityContext context) {
        if (context.entity() instanceof Chicken chicken) {
            if (!chicken.isBaby() && chicken.eggTime < MAX_TRACKED_TICKS) {
                context.data().putInt("NextEggIn", chicken.eggTime);
            }
        } else if (context.entity() instanceof Armadillo armadillo) {
            int scuteTime = Nms.armadilloScuteTime(armadillo);
            if (!armadillo.isBaby() && scuteTime >= 0 && scuteTime < MAX_TRACKED_TICKS) {
                context.data().putInt("NextScuteIn", scuteTime);
            }
        } else if (context.entity() instanceof Sniffer sniffer) {
            long time = sniffer.getBrain().getTimeUntilExpiry(MemoryModuleType.SNIFF_COOLDOWN);
            if (time > 0 && time < MAX_TRACKED_TICKS) {
                context.data().putInt("NextSniffIn", (int) time);
            }
        }
    }
}

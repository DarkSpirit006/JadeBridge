package com.darkspirit69.jadebridge.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import net.minecraft.world.entity.animal.armadillo.Armadillo;
import net.minecraft.world.entity.animal.frog.Tadpole;
import net.minecraft.world.entity.npc.villager.Villager;
import org.jspecify.annotations.Nullable;

/**
 * The only reflective spot in the plugin: members Jade reaches through its
 * classtweaker but that are private on a Paper server. Lookups happen once and
 * fail soft (the feature is skipped, never crashes a request).
 */
public final class Nms {

    private static Method tadpoleAge;
    private static Field armadilloScuteTime;
    private static Field villagerLastRestock;
    private static boolean initialized;

    private Nms() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        tadpoleAge = method("net.minecraft.world.entity.animal.frog.Tadpole", "getTicksLeftUntilAdult");
        armadilloScuteTime = field("net.minecraft.world.entity.animal.armadillo.Armadillo", "scuteTime");
        villagerLastRestock = field("net.minecraft.world.entity.npc.villager.Villager", "lastRestockGameTime");
    }

    @Nullable
    private static Method method(String owner, String name) {
        try {
            Method method = Class.forName(owner).getDeclaredMethod(name);
            method.setAccessible(true);
            return method;
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    @Nullable
    private static Field field(String owner, String name) {
        try {
            Field field = Class.forName(owner).getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    /** Ticks until a tadpole grows up, or -1 when unavailable. */
    public static int tadpoleTicksLeft(Tadpole tadpole) {
        if (tadpoleAge == null) {
            return -1;
        }
        try {
            return (int) tadpoleAge.invoke(tadpole);
        } catch (ReflectiveOperationException e) {
            return -1;
        }
    }

    /** Ticks until an armadillo drops its next scute, or -1 when unavailable. */
    public static int armadilloScuteTime(Armadillo armadillo) {
        if (armadilloScuteTime == null) {
            return -1;
        }
        try {
            return armadilloScuteTime.getInt(armadillo);
        } catch (ReflectiveOperationException e) {
            return -1;
        }
    }

    /** Game time of a villager's last restock, or -1 when unavailable. */
    public static long villagerLastRestock(Villager villager) {
        if (villagerLastRestock == null) {
            return -1;
        }
        try {
            return villagerLastRestock.getLong(villager);
        } catch (ReflectiveOperationException e) {
            return -1;
        }
    }
}

package com.darkspirit69.jadebridge.util;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.potion.PotionEffectType;
import org.jspecify.annotations.Nullable;

/**
 * Tracks when potion effects were added or last changed on living entities, the
 * data Jade's MobEffectInstance mixin usually provides. Entries die with the
 * entity (weak keys); effects nobody observed report time 0, which matches the
 * mixin default for effects loaded from NBT.
 */
public final class EffectTimes implements Listener {

    private static volatile EffectTimes active;

    /** long[]{addTime, updateTime}, wall clock millis. */
    private final Map<LivingEntity, Map<Holder<MobEffect>, long[]>> times =
            Collections.synchronizedMap(new WeakHashMap<>());

    /**
     * Called when the tracker is registered with the server; providers read through
     * the static accessors below.
     */
    public static void activate(EffectTimes tracker) {
        active = tracker;
    }

    public static long addTime(LivingEntity entity, Holder<MobEffect> holder) {
        long[] value = find(entity, holder);
        return value == null ? 0 : value[0];
    }

    public static long updateTime(LivingEntity entity, Holder<MobEffect> holder) {
        long[] value = find(entity, holder);
        return value == null ? 0 : value[1];
    }

    @EventHandler
    public void onPotionEffect(EntityPotionEffectEvent event) {
        if (!(event.getEntity() instanceof LivingEntity living)) {
            return;
        }
        Holder<MobEffect> holder = holderOf(event.getModifiedType());
        if (holder == null) {
            return;
        }
        long now = System.currentTimeMillis();
        switch (event.getAction()) {
            case ADDED -> put(living, holder, now, now);
            case CHANGED -> put(living, holder, addTime(living, holder), now);
            case REMOVED, CLEARED -> remove(living, holder);
            default -> {
            }
        }
    }

    @Nullable
    private static long[] find(LivingEntity entity, Holder<MobEffect> holder) {
        EffectTimes tracker = active;
        if (tracker == null) {
            return null;
        }
        Map<Holder<MobEffect>, long[]> entityTimes = tracker.times.get(entity);
        return entityTimes == null ? null : entityTimes.get(holder);
    }

    private static void put(LivingEntity entity, Holder<MobEffect> holder, long addTime, long updateTime) {
        EffectTimes tracker = active;
        if (tracker == null) {
            return;
        }
        tracker.times
                .computeIfAbsent(entity, $ -> Collections.synchronizedMap(new WeakHashMap<>()))
                .put(holder, new long[]{addTime, updateTime});
    }

    private static void remove(LivingEntity entity, Holder<MobEffect> holder) {
        EffectTimes tracker = active;
        if (tracker == null) {
            return;
        }
        Map<Holder<MobEffect>, long[]> entityTimes = tracker.times.get(entity);
        if (entityTimes != null) {
            entityTimes.remove(holder);
        }
    }

    @Nullable
    private static Holder<MobEffect> holderOf(PotionEffectType type) {
        if (type == null) {
            return null;
        }
        try {
            return org.bukkit.craftbukkit.potion.CraftPotionEffectType.bukkitToMinecraftHolder(type);
        } catch (Exception e) {
            return null;
        }
    }
}

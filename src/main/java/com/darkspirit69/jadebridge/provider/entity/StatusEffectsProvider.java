package com.darkspirit69.jadebridge.provider.entity;

import com.darkspirit69.jadebridge.provider.EntityContext;
import com.darkspirit69.jadebridge.provider.EntityDataProvider;
import com.darkspirit69.jadebridge.util.EffectTimes;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import org.jspecify.annotations.NonNull;

/**
 * {@code minecraft:potion_effects} — the visible active effects, each with the wall
 * clock times Jade's client needs for its fade-in animation. The times come from
 * {@link EffectTimes}, which observes Bukkit's EntityPotionEffectEvent instead of
 * Jade's MobEffectInstance mixin.
 */
public final class StatusEffectsProvider implements EntityDataProvider {

    public static final StatusEffectsProvider INSTANCE = new StatusEffectsProvider();
    public static final Identifier ID = Identifier.withDefaultNamespace("potion_effects");

    private record Effect(MobEffectInstance effect, long updateTime, long addTime) {
        static final StreamCodec<RegistryFriendlyByteBuf, Effect> CODEC = StreamCodec.composite(
                MobEffectInstance.STREAM_CODEC,
                (@NonNull Effect effect) -> effect.effect(),
                ByteBufCodecs.LONG,
                (@NonNull Effect effect) -> effect.updateTime(),
                ByteBufCodecs.LONG,
                (@NonNull Effect effect) -> effect.addTime(),
                (MobEffectInstance effect, @NonNull Long updateTime, @NonNull Long addTime) ->
                        new Effect(effect, updateTime, addTime));
    }

    private static final StreamCodec<RegistryFriendlyByteBuf, List<Effect>> CODEC =
            ByteBufCodecs.<RegistryFriendlyByteBuf, Effect>list().apply(Effect.CODEC);

    private StatusEffectsProvider() {
    }

    @Override
    public Identifier id() {
        return ID;
    }

    @Override
    public void appendServerData(EntityContext context) {
        if (!(context.entity() instanceof LivingEntity living)) {
            return;
        }
        List<Effect> effects = living.getActiveEffects().stream()
                .filter((@NonNull MobEffectInstance effect) -> effect.isVisible())
                .map(instance -> toEffect(living, instance))
                .toList();
        if (effects.isEmpty()) {
            return;
        }
        context.put(ID, CODEC, effects);
    }

    private static Effect toEffect(LivingEntity entity, MobEffectInstance instance) {
        Holder<MobEffect> holder = instance.getEffect();
        long addTime = EffectTimes.addTime(entity, holder);
        long updateTime = EffectTimes.updateTime(entity, holder);
        return new Effect(instance, updateTime, addTime);
    }
}

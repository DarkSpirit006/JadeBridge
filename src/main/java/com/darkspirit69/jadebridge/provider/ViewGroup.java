package com.darkspirit69.jadebridge.provider;

import io.netty.buffer.ByteBuf;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

/**
 * Mirror of Jade's {@code snownee.jade.api.view.ViewGroup} wire format: a list of
 * views, an optional group id and an optional extra-data compound.
 */
public record ViewGroup<T>(List<T> views, @Nullable String id, @Nullable CompoundTag extraData) {

    public ViewGroup(List<T> views) {
        this(views, (String) null, (CompoundTag) null);
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public ViewGroup(List<T> views, Optional<String> id, Optional<CompoundTag> extraData) {
        this(views, id.orElse(null), extraData.orElse(null));
    }

    public static <B extends ByteBuf, T> StreamCodec<B, ViewGroup<T>> codec(StreamCodec<B, T> viewCodec) {
        return StreamCodec.composite(
                ByteBufCodecs.<B, T>list().apply(viewCodec),
                ViewGroup::views,
                ByteBufCodecs.optional(ByteBufCodecs.STRING_UTF8),
                $ -> Optional.ofNullable($.id()),
                ByteBufCodecs.optional(ByteBufCodecs.COMPOUND_TAG),
                $ -> Optional.ofNullable($.extraData()),
                ViewGroup::new);
    }

    public static <B extends ByteBuf, T> StreamCodec<B, Map.Entry<Identifier, List<ViewGroup<T>>>> listCodec(StreamCodec<B, T> viewCodec) {
        return StreamCodec.composite(
                Identifier.STREAM_CODEC,
                Map.Entry::getKey,
                ByteBufCodecs.<B, ViewGroup<T>>list().apply(codec(viewCodec)),
                Map.Entry::getValue,
                Map::entry);
    }
}

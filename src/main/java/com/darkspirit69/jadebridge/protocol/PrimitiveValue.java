package com.darkspirit69.jadebridge.protocol;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Mirror of Jade's {@code JadeCodecs.PRIMITIVE_STREAM_CODEC}, the value codec used for
 * server config overrides inside the handshake packet. The type tag on the wire is:
 * 0 = false, 1 = true, 2 = VarInt, 3 = float, 4 = UTF string, values above 20 encode
 * small non-negative ints as {@code value + 20} in a single byte.
 */
public final class PrimitiveValue {

    public static final StreamCodec<ByteBuf, Object> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public Object decode(ByteBuf buf) {
            byte tag = buf.readByte();
            return switch (tag) {
                case 0 -> Boolean.FALSE;
                case 1 -> Boolean.TRUE;
                case 2 -> ByteBufCodecs.VAR_INT.decode(buf);
                case 3 -> ByteBufCodecs.FLOAT.decode(buf);
                case 4 -> ByteBufCodecs.STRING_UTF8.decode(buf);
                default -> {
                    if (tag > 20) {
                        yield tag - 20;
                    }
                    throw new IllegalArgumentException("Unknown primitive type: " + tag);
                }
            };
        }

        @Override
        public void encode(ByteBuf buf, Object value) {
            switch (value) {
                case Boolean b -> buf.writeByte(b ? 1 : 0);
                case Number n -> {
                    float f = n.floatValue();
                    if (f != (int) f) {
                        buf.writeByte(3);
                        ByteBufCodecs.FLOAT.encode(buf, f);
                        break;
                    }
                    int i = n.intValue();
                    if (i >= 0 && i <= Byte.MAX_VALUE - 20) {
                        buf.writeByte(i + 20);
                    } else {
                        buf.writeByte(2);
                        ByteBufCodecs.VAR_INT.encode(buf, i);
                    }
                }
                case String s -> {
                    buf.writeByte(4);
                    ByteBufCodecs.STRING_UTF8.encode(buf, s);
                }
                case null, default -> throw new IllegalArgumentException("Not a primitive value: " + value);
            }
        }
    };

    private PrimitiveValue() {
    }
}

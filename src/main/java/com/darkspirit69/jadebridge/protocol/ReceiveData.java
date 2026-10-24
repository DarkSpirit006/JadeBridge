package com.darkspirit69.jadebridge.protocol;

import com.darkspirit69.jadebridge.JadeProtocol;
import io.netty.buffer.Unpooled;
import java.util.Objects;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import org.bukkit.entity.Player;

/**
 * Body of {@code jade:receive_data} plus Jade's 16 KiB payload trimming. The trim
 * logic is a direct port of {@code ReceiveDataPacket.send}/{@code removeLargest}:
 * repeatedly drop the largest child tag (descending one level into compounds) so the
 * client keeps as much data as possible instead of losing the whole packet.
 */
public final class ReceiveData {

    public static void send(Player player, org.bukkit.plugin.Plugin plugin, CompoundTag tag) {
        trim(tag);
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        try {
            ByteBufCodecs.COMPOUND_TAG.encode(buf, tag);
            byte[] bytes = new byte[buf.readableBytes()];
            buf.readBytes(bytes);
            player.sendPluginMessage(plugin, JadeProtocol.CHANNEL_RECEIVE_DATA, bytes);
        } finally {
            buf.release();
        }
    }

    static void trim(CompoundTag tag) {
        if (tag.sizeInBytes() <= JadeProtocol.MAX_PAYLOAD_SIZE) {
            return;
        }
        int rounds = 0;
        do {
            if (++rounds > 10) {
                return;
            }
            removeLargest(tag, 0, 1);
        } while (tag.sizeInBytes() > JadeProtocol.MAX_PAYLOAD_SIZE);
    }

    private static boolean removeLargest(CompoundTag tag, int depth, int maxDepth) {
        int largestSize = 0;
        String largestKey = null;
        Tag largestValue = null;
        for (String key : tag.keySet()) {
            Tag child = Objects.requireNonNull(tag.get(key));
            int size = child.sizeInBytes();
            if (size > largestSize) {
                largestSize = size;
                largestKey = key;
                largestValue = child;
            }
        }
        if (largestKey == null) {
            return false;
        }
        if (depth < maxDepth && largestValue instanceof CompoundTag compound) {
            if (!removeLargest(compound, depth + 1, maxDepth)) {
                tag.remove(largestKey);
            }
        } else {
            tag.remove(largestKey);
        }
        return true;
    }

    private ReceiveData() {
    }
}

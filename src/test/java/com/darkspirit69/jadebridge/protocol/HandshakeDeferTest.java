package com.darkspirit69.jadebridge.protocol;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.darkspirit69.jadebridge.JadeProtocol;
import com.darkspirit69.jadebridge.Settings;
import com.darkspirit69.jadebridge.TestEnv;
import com.darkspirit69.jadebridge.provider.ProviderRegistry;
import com.darkspirit69.jadebridge.session.SessionManager;
import io.netty.buffer.Unpooled;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * The real Jade client can send its handshake before its minecraft:register, and
 * Bukkit drops plugin messages to unregistered channels, so the reply must be
 * held back and flushed when jade:server_handshake shows up.
 */
class HandshakeDeferTest {

    private record Sent(String channel, byte[] body) {
    }

    @BeforeAll
    static void boot() {
        TestEnv.boot();
    }

    @Test
    void replyIsHeldUntilTheChannelIsRegistered() {
        Set<String> channels = new HashSet<>();
        List<Sent> sent = new ArrayList<>();
        Player player = player(channels, sent);

        HandshakeHandler handler = handler();
        handler.handle(player, utf8("9"));
        assertTrue(sent.isEmpty(), "reply must not be sent while the channel is unregistered");

        channels.add(JadeProtocol.CHANNEL_SERVER_HANDSHAKE);
        handler.flushPendingReply(player);

        assertEquals(1, sent.size());
        assertEquals(JadeProtocol.CHANNEL_SERVER_HANDSHAKE, sent.get(0).channel());
        assertTrue(sent.get(0).body().length > 0);
    }

    @Test
    void replyGoesOutImmediatelyWhenTheChannelIsAlreadyRegistered() {
        Set<String> channels = new HashSet<>(Set.of(JadeProtocol.CHANNEL_SERVER_HANDSHAKE));
        List<Sent> sent = new ArrayList<>();
        Player player = player(channels, sent);

        HandshakeHandler handler = handler();
        handler.handle(player, utf8("9"));

        assertEquals(1, sent.size());
        handler.flushPendingReply(player);
        assertEquals(1, sent.size(), "flush must not send a second reply");
    }

    @Test
    void heldReplyIsDiscardedWhenThePlayerLeaves() {
        Set<String> channels = new HashSet<>();
        List<Sent> sent = new ArrayList<>();
        Player player = player(channels, sent);
        UUID id = player.getUniqueId();

        HandshakeHandler handler = handler();
        handler.handle(player, utf8("9"));
        handler.discardPendingReply(id);

        channels.add(JadeProtocol.CHANNEL_SERVER_HANDSHAKE);
        handler.flushPendingReply(player);
        assertTrue(sent.isEmpty(), "a reply held for a player who left must never be sent");
    }

    private static HandshakeHandler handler() {
        return new HandshakeHandler(plugin(), new Settings(), new ProviderRegistry(), new SessionManager());
    }

    static byte[] utf8(String value) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        try {
            ByteBufCodecs.STRING_UTF8.encode(buf, value);
            byte[] bytes = new byte[buf.readableBytes()];
            buf.readBytes(bytes);
            return bytes;
        } finally {
            buf.release();
        }
    }

    /**
     * A Player stand-in backed by a proxy; only the handful of methods the
     * handshake path calls is answered, everything else gets a default value.
     */
    private static Player player(Set<String> channels, List<Sent> sent) {
        UUID id = UUID.randomUUID();
        return (Player) proxy(Player.class, (method, args) -> {
            if ("getUniqueId".equals(method)) {
                return id;
            }
            if ("getListeningPluginChannels".equals(method)) {
                return channels;
            }
            if ("sendPluginMessage".equals(method)) {
                sent.add(new Sent((String) args[1], (byte[]) args[2]));
                return null;
            }
            return null;
        });
    }

    private static Plugin plugin() {
        return (Plugin) proxy(Plugin.class, (method, args) -> null);
    }

    private static Object proxy(Class<?> face, java.util.function.BiFunction<String, Object[], Object> behaviour) {
        InvocationHandler handler = (proxy, method, args) -> {
            if ("toString".equals(method.getName())) {
                return "fake " + face.getSimpleName();
            }
            if ("hashCode".equals(method.getName())) {
                return System.identityHashCode(proxy);
            }
            if ("equals".equals(method.getName())) {
                return proxy == args[0];
            }
            Class<?> returnType = method.getReturnType();
            if (returnType != void.class && returnType != boolean.class && returnType.isPrimitive()) {
                return primitiveZero(returnType);
            }
            Object result = behaviour.apply(method.getName(), args);
            return returnType == boolean.class && result == null ? false : result;
        };
        return Proxy.newProxyInstance(HandshakeDeferTest.class.getClassLoader(), new Class<?>[]{face}, handler);
    }

    private static Object primitiveZero(Class<?> type) {
        if (type == char.class) {
            return '\0';
        }
        if (type == byte.class) {
            return (byte) 0;
        }
        if (type == short.class) {
            return (short) 0;
        }
        if (type == int.class) {
            return 0;
        }
        if (type == long.class) {
            return 0L;
        }
        if (type == float.class) {
            return 0f;
        }
        if (type == double.class) {
            return 0d;
        }
        return null;
    }
}

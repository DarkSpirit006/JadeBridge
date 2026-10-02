package com.darkspirit69.jadebridge;

/**
 * Wire identifiers of the Jade protocol. Values must never drift from the ones
 * declared in {@code snownee.jade.api.JadeIds} / {@code snownee.jade.Jade#PROTOCOL_VERSION};
 * the client uses them to find our packets and to validate the handshake.
 */
public final class JadeProtocol {

    public static final String PROTOCOL_VERSION = "9";

    public static final String CHANNEL_CLIENT_HANDSHAKE = "jade:client_handshake";
    public static final String CHANNEL_SERVER_HANDSHAKE = "jade:server_handshake";
    public static final String CHANNEL_REQUEST_BLOCK = "jade:request_block";
    public static final String CHANNEL_REQUEST_ENTITY = "jade:request_entity";
    public static final String CHANNEL_RECEIVE_DATA = "jade:receive_data";

    /** Jade trims server payloads down to this size before sending (ReceiveDataPacket.MAX_SIZE). */
    public static final int MAX_PAYLOAD_SIZE = 16 * 1024;

    /** Sanity cap for anything we decode; vanilla custom payloads cap at 32 KiB anyway. */
    public static final int MAX_INCOMING_SIZE = 32 * 1024;

    private JadeProtocol() {
    }
}

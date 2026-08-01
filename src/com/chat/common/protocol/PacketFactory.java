package com.chat.common.protocol;

public class PacketFactory {

    public static Packet createSuccessPacket(PacketType type, Object payload) {
        Packet packet = new Packet(type, payload);
        packet.setSuccess(true);
        return packet;
    }

    public static Packet createSuccessPacket(PacketType type, String sessionToken, Object payload) {
        Packet packet = new Packet(type, sessionToken, payload);
        packet.setSuccess(true);
        return packet;
    }

    public static Packet createErrorPacket(String errorMessage) {
        Packet packet = new Packet();
        packet.setType(PacketType.ERROR_RESPONSE);
        packet.setSuccess(false);
        packet.setMessage(errorMessage);
        return packet;
    }

    public static Packet createErrorPacket(PacketType type, String errorMessage) {
        Packet packet = new Packet();
        packet.setType(type);
        packet.setSuccess(false);
        packet.setMessage(errorMessage);
        return packet;
    }
}

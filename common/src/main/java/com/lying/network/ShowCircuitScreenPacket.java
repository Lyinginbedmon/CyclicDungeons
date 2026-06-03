package com.lying.network;

import dev.architectury.networking.NetworkManager;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public class ShowCircuitScreenPacket
{
	private static final Identifier PACKET_ID = CDPacketHandler.SHOW_CIRCUIT_SCREEN_ID;
	public static final CustomPayload.Id<Payload> PACKET_TYPE	= new CustomPayload.Id<>(PACKET_ID);
	public static final PacketCodec<RegistryByteBuf, Payload> PACKET_CODEC	= CustomPayload.codecOf(Payload::write, Payload::read);
	
	public static void sendTo(ServerPlayerEntity player)
	{
		NetworkManager.sendToPlayer(player, new Payload());
	}
	
	public static class Payload implements CustomPayload
	{
		protected Payload() { }
		
		public static Payload read(RegistryByteBuf buffer) { return new Payload(); }
		
		public void write(RegistryByteBuf buffer) { }
		
		public Id<? extends CustomPayload> getId() { return PACKET_TYPE; }
	}
}

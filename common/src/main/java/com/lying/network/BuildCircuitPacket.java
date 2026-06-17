package com.lying.network;

import java.util.List;

import com.google.common.collect.Lists;
import com.lying.item.component.CircuitComponent.CircuitPart;

import dev.architectury.networking.NetworkManager;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public class BuildCircuitPacket
{
	private static final Identifier PACKET_ID = CDPacketHandler.BUILD_CIRCUIT_ID;
	public static final CustomPayload.Id<Payload> PACKET_TYPE	= new CustomPayload.Id<>(PACKET_ID);
	public static final PacketCodec<RegistryByteBuf, Payload> PACKET_CODEC	= CustomPayload.codecOf(Payload::write, Payload::read);
	
	public static void sendToServer(List<CircuitPart> circuit)
	{
		NetworkManager.sendToServer(new Payload(circuit));
	}
	
	public static class Payload implements CustomPayload
	{
		private List<CircuitPart> circuit = Lists.newArrayList();
		
		public Payload(List<CircuitPart> list)
		{
			circuit.addAll(list);
		}
		
		public static Payload read(RegistryByteBuf buffer)
		{
			return new Payload(CircuitPart.LIST_PACKET_CODEC.decode(buffer));
		}
		
		public void write(RegistryByteBuf buffer)
		{
			CircuitPart.LIST_PACKET_CODEC.encode(buffer, circuit);
		}
		
		public Id<? extends CustomPayload> getId() { return PACKET_TYPE; }
		
		public List<CircuitPart> circuit() { return circuit; }
	}
}

package com.lying.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public class CyclePortPacket
{
	private static final Identifier PACKET_ID = CDPacketHandler.CYCLE_PORT_ID;
	public static final CustomPayload.Id<Payload> PACKET_TYPE	= new CustomPayload.Id<>(PACKET_ID);
	public static final PacketCodec<RegistryByteBuf, Payload> PACKET_CODEC	= CustomPayload.codecOf(Payload::write, Payload::read);
	
	public static class Payload implements CustomPayload
	{
		private int delta;
		private BlockPos pos;
		private boolean isOrigin;
		
		public Payload(BlockPos position, boolean originIn, int amount)
		{
			delta = amount;
			isOrigin = originIn;
			pos = position;
		}
		
		public static Payload read(RegistryByteBuf buffer)
		{
			return new Payload(
					new BlockPos(buffer.readInt(), buffer.readInt(), buffer.readInt()), 
					buffer.readBoolean(),
					buffer.readInt());
		}
		
		public void write(RegistryByteBuf buffer)
		{
			buffer.writeInt(pos.getX());
			buffer.writeInt(pos.getY());
			buffer.writeInt(pos.getZ());
			buffer.writeBoolean(isOrigin);
			buffer.writeInt(delta);
		}
		
		public Id<? extends CustomPayload> getId() { return PACKET_TYPE; }
		
		public BlockPos pos() { return pos; }
		public boolean isOrigin() { return isOrigin; }
		public int delta() { return delta; }
	}
}

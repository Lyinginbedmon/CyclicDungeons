package com.lying.network;

import org.jetbrains.annotations.NotNull;

import com.lying.grammar.GrammarPhrase;

import dev.architectury.networking.NetworkManager;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtSizeTracker;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class ShowDungeonLayoutPacket
{
	private static final Identifier PACKET_ID = CDPacketHandler.SHOW_DUNGEON_LAYOUT_ID;
	public static final CustomPayload.Id<Payload> PACKET_TYPE	= new CustomPayload.Id<>(PACKET_ID);
	public static final PacketCodec<RegistryByteBuf, Payload> PACKET_CODEC	= CustomPayload.codecOf(Payload::write, Payload::read);
	
	public static void sendTo(ServerPlayerEntity player, GrammarPhrase graph, boolean isGenerated)
	{
		NetworkManager.sendToPlayer(player, new Payload(graph, isGenerated));
	}
	
	public static class Payload implements CustomPayload
	{
		private final GrammarPhrase graph;
		private final boolean isGenerated;
		
		protected Payload(@NotNull GrammarPhrase graphIn, boolean isGenerated)
		{
			this.graph = graphIn;
			this.isGenerated = isGenerated; 
		}
		
		public static Payload read(RegistryByteBuf buffer)
		{
			NbtElement ele = RegistryByteBuf.readNbt(buffer, NbtSizeTracker.of(2097152L));
			GrammarPhrase graph = GrammarPhrase.fromNbt(ele);
			return new Payload(graph, buffer.readBoolean());
		}
		
		public void write(RegistryByteBuf buffer)
		{
			buffer.writeNbt(graph.toNbt());
			buffer.writeBoolean(isGenerated);
		}
		
		public GrammarPhrase graph() { return graph; }
		
		public Text title()
		{
			return
					isGenerated ? 
						Text.literal("Generated dungeon") :
						Text.literal("Parsed dungeon");
		}
		
		public Id<? extends CustomPayload> getId() { return PACKET_TYPE; }
	}
}

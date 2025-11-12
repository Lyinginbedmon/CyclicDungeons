package com.lying.item.component;

import java.util.Optional;

import com.lying.block.IWireableBlock.WireRecipient;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.util.math.BlockPos;

public record WiringComponent(Optional<BlockPos> pos, Optional<WireRecipient> type)
{
	public static final Codec<WiringComponent> CODEC	= RecordCodecBuilder.create(instance -> instance.group(
			BlockPos.CODEC.optionalFieldOf("target").forGetter(WiringComponent::pos),
			WireRecipient.CODEC.optionalFieldOf("type").forGetter(WiringComponent::type)
			)
			.apply(instance, WiringComponent::new));
	
	public static final PacketCodec<ByteBuf, WiringComponent> PACKET_CODEC = PacketCodec.tuple(
			PacketCodecs.optional(BlockPos.PACKET_CODEC), WiringComponent::pos, 
			PacketCodecs.optional(WireRecipient.PACKET_CODEC), WiringComponent::type, 
			WiringComponent::new);
	
	public boolean isWiring() { return pos.isPresent(); }
	
	public static WiringComponent of(BlockPos pos, WireRecipient type) { return new WiringComponent(Optional.of(pos), Optional.of(type)); }
	
	public static WiringComponent empty() { return new WiringComponent(Optional.empty(), Optional.empty()); }

}

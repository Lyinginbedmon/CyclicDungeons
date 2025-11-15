package com.lying.item.component;

import java.util.Optional;
import java.util.function.Consumer;

import com.lying.block.IWireableBlock.WireRecipient;
import com.lying.reference.Reference;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.netty.buffer.ByteBuf;
import net.minecraft.item.Item.TooltipContext;
import net.minecraft.item.tooltip.TooltipAppender;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

public record WiringComponent(Optional<BlockPos> pos, Optional<WireRecipient> type) implements TooltipAppender
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
	
	public void appendTooltip(TooltipContext context, Consumer<Text> tooltip, TooltipType type)
	{
		pos.ifPresent(p -> tooltip.accept(Reference.ModInfo.translate("gui", "wire_gun.target", p.toShortString())));
	}

}

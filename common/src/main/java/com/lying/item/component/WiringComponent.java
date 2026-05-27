package com.lying.item.component;

import java.util.Optional;
import java.util.function.Consumer;

import com.lying.CyclicDungeons;
import com.lying.block.IWireableBlock;
import com.lying.block.IWireableBlock.WireRecipient;
import com.lying.init.CDLogicGates;
import com.lying.item.WiringGunItem.WireMode;
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
import net.minecraft.world.World;

public record WiringComponent(
		Optional<BlockPos> pos, 
		Optional<String> portA, 
		Optional<String> portB
		) implements TooltipAppender
{
	public static final Codec<WiringComponent> CODEC	= RecordCodecBuilder.create(instance -> instance.group(
			BlockPos.CODEC.optionalFieldOf("target").forGetter(WiringComponent::pos),
			Codec.STRING.optionalFieldOf("start_port").forGetter(WiringComponent::portA),
			Codec.STRING.optionalFieldOf("end_port").forGetter(WiringComponent::portB)
			)
			.apply(instance, WiringComponent::new));
	
	public static final PacketCodec<ByteBuf, WiringComponent> PACKET_CODEC = PacketCodec.tuple(
			PacketCodecs.optional(BlockPos.PACKET_CODEC), WiringComponent::pos, 
			PacketCodecs.optional(PacketCodecs.STRING), WiringComponent::portA,
			PacketCodecs.optional(PacketCodecs.STRING), WiringComponent::portB,
			WiringComponent::new);
	
	public boolean isWiring() { return pos.isPresent(); }
	
	public static WiringComponent of(BlockPos pos, WireRecipient type) { return new WiringComponent(Optional.of(pos), Optional.of(CDLogicGates.OUTPUT), Optional.empty()); }
	
	public WiringComponent startingAt(String port)
	{
		return new WiringComponent(pos, Optional.of(port), portB);
	}
	
	public static WiringComponent empty() { return new WiringComponent(Optional.empty(), Optional.empty(), Optional.empty()); }
	
	public void appendTooltip(TooltipContext context, Consumer<Text> tooltip, TooltipType type)
	{
		pos.ifPresent(p -> tooltip.accept(Reference.ModInfo.translate("gui", "wiring_gun.target", portA.orElse(CDLogicGates.OUTPUT), p.toShortString())));
	}
	
	public boolean tryApplyTo(BlockPos pos, World world, WireMode mode)
	{
		if(!isWiring())
			return false;
		
		BlockPos origin = pos().get();
		IWireableBlock provider = IWireableBlock.getWireable(origin, world);
		IWireableBlock receiver = IWireableBlock.getWireable(pos, world);
		if(receiver == null || provider == null)
		{
			CyclicDungeons.LOGGER.error("One or both wiring targets not viable");
			return false;
		}
		
		String input = portB.orElse(CDLogicGates.INPUT);
		String output = portA.orElse(CDLogicGates.OUTPUT);
		if(!receiver.inputPorts(pos, world).contains(input) || !provider.outputPorts(origin, world).contains(output))
		{
			CyclicDungeons.LOGGER.error("Ports not acceptable to wiring target(s)");
			return false;
		}
		
		if(!receiver.acceptWireFrom(input, pos, mode, origin, output, world))
		{
			CyclicDungeons.LOGGER.error("Receiver refused wire");
			return false;
		}
		else
			return provider.acceptWireTo(output, origin, mode, pos, input, world);
	}
}

package com.lying.item.component;

import java.util.Optional;
import java.util.function.Consumer;

import com.lying.CyclicDungeons;
import com.lying.block.IWireableBlock;
import com.lying.block.entity.logic.WiringManifest.ManifestEntry.PortEntry;
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
		Optional<PortEntry> output, 
		Optional<String> portB
		) implements TooltipAppender
{
	public static final Codec<WiringComponent> CODEC	= RecordCodecBuilder.create(instance -> instance.group(
			PortEntry.CODEC.optionalFieldOf("target_port").forGetter(WiringComponent::output),
			Codec.STRING.optionalFieldOf("end_port").forGetter(WiringComponent::portB)
			)
			.apply(instance, WiringComponent::new));
	
	public static final PacketCodec<ByteBuf, WiringComponent> PACKET_CODEC = PacketCodec.tuple(
			PacketCodecs.optional(PortEntry.PACKET_CODEC), WiringComponent::output, 
			PacketCodecs.optional(PacketCodecs.STRING), WiringComponent::portB,
			WiringComponent::new);
	
	public boolean isWiring() { return output.isPresent(); }
	
	public static WiringComponent of(BlockPos pos) { return new WiringComponent(Optional.of(new PortEntry(pos, CDLogicGates.OUTPUT)), Optional.empty()); }
	
	public WiringComponent startingAt(String port)
	{
		return new WiringComponent(Optional.of(new PortEntry(output.get().pos(), port)), portB);
	}
	
	public static WiringComponent empty() { return new WiringComponent(Optional.empty(), Optional.empty()); }
	
	public void appendTooltip(TooltipContext context, Consumer<Text> tooltip, TooltipType type)
	{
		output.ifPresent(p -> tooltip.accept(Reference.ModInfo.translate("gui", "wiring_gun.target", output.get().displayName())));
	}
	
	public boolean tryApplyTo(BlockPos pos, World world, WireMode mode)
	{
		if(!isWiring())
			return false;
		
		BlockPos origin = output().get().pos();
		IWireableBlock provider = IWireableBlock.getWireable(origin, world);
		IWireableBlock receiver = IWireableBlock.getWireable(pos, world);
		if(receiver == null || provider == null)
		{
			CyclicDungeons.LOGGER.error("One or both wiring targets not viable");
			return false;
		}
		
		String input = portB.orElse(CDLogicGates.INPUT);
		String output = output().get().port();
		if(!receiver.inputPorts(pos, world).contains(input) || !provider.outputPorts(origin, world).contains(output))
		{
			CyclicDungeons.LOGGER.error("Ports not acceptable to wiring target(s)");
			return false;
		}
		
		PortEntry portIn = new PortEntry(pos, input);
		PortEntry portOut = output().get();
		if(!receiver.acceptWireFrom(input, pos, mode, portOut, world))
		{
			CyclicDungeons.LOGGER.error("Receiver refused wire");
			return false;
		}
		else
			return provider.acceptWireTo(output, origin, mode, portIn, world);
	}
}

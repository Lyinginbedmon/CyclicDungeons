package com.lying.item.component;

import static com.lying.reference.Reference.ModInfo.translate;

import java.util.Optional;
import java.util.function.Consumer;

import com.lying.CyclicDungeons;
import com.lying.block.IWireableBlock;
import com.lying.block.Port;
import com.lying.block.entity.logic.PortEntry;
import com.lying.item.WiringGunItem.WireMode;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.netty.buffer.ByteBuf;
import net.minecraft.item.Item.TooltipContext;
import net.minecraft.item.tooltip.TooltipAppender;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TextCodecs;
import net.minecraft.text.HoverEvent.Action;
import net.minecraft.world.World;

public record WiringComponent(
		Optional<PortEntry> output,
		Optional<Text> block
		) implements TooltipAppender
{
	public static final Codec<WiringComponent> CODEC	= RecordCodecBuilder.create(instance -> instance.group(
			PortEntry.CODEC.optionalFieldOf("start_port").forGetter(WiringComponent::output),
			TextCodecs.CODEC.optionalFieldOf("name").forGetter(WiringComponent::block)
			)
			.apply(instance, WiringComponent::new));
	
	public static final PacketCodec<ByteBuf, WiringComponent> PACKET_CODEC = PacketCodec.tuple(
			PacketCodecs.optional(PortEntry.PACKET_CODEC), WiringComponent::output, 
			PacketCodecs.optional(TextCodecs.PACKET_CODEC), WiringComponent::block,
			WiringComponent::new);
	
	public static WiringComponent of(PortEntry port, MutableText name) { return new WiringComponent(Optional.of(port), Optional.of(name)); }
	
	public static WiringComponent empty() { return new WiringComponent(Optional.empty(), Optional.empty()); }
	
	public boolean isWiring() { return output.isPresent(); }
	
	public void appendTooltip(TooltipContext context, Consumer<Text> tooltip, TooltipType type)
	{
		output.ifPresent(p -> tooltip.accept(translate("gui", "wiring_gun.target", output.get().displayName())));
	}
	
	public MutableText startName()
	{
		return output.isEmpty() ? Text.empty() : ((MutableText)block.get()).styled(s -> s.withHoverEvent(new HoverEvent(Action.SHOW_TEXT, Text.literal(output.get().pos().toShortString()))));
	}
	
	/** Attempts to wire the two blocks together between the given ports */
	public static boolean tryWire(PortEntry start, PortEntry end, World world, WireMode mode)
	{
		Optional<IWireableBlock> provider = IWireableBlock.getWireable(start.pos(), world);
		Optional<IWireableBlock> receiver = IWireableBlock.getWireable(end.pos(), world);
		if(receiver.isEmpty() || provider.isEmpty())
		{
			CyclicDungeons.LOGGER.error("One or both wiring targets not viable");
			return false;
		}
		
		Port output = start.port();
		Port input = end.port();
		if(!receiver.get().inputPorts(end.pos(), world).contains(input) || !provider.get().outputPorts(start.pos(), world).contains(output))
		{
			CyclicDungeons.LOGGER.error("Ports not acceptable to wiring target(s)");
			return false;
		}
		
		if(!receiver.get().acceptWireFrom(input, end.pos(), mode, start, world))
		{
			CyclicDungeons.LOGGER.error("Receiver refused wire");
			return false;
		}
		else if(!provider.get().acceptWireTo(output, start.pos(), mode, end, world))
		{
			CyclicDungeons.LOGGER.error("Provider refused wire");
			return false;
		}
		else
			return true;
	}
}

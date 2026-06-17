package com.lying.item.component;

import static com.lying.reference.Reference.ModInfo.translate;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import org.joml.Vector2i;

import com.google.common.collect.Lists;
import com.lying.block.entity.logic.LogicModule;
import com.lying.utility.CDUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.netty.buffer.ByteBuf;
import net.minecraft.item.Item.TooltipContext;
import net.minecraft.item.tooltip.TooltipAppender;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.text.Text;
import net.minecraft.util.Pair;

public record CircuitComponent(
		Optional<List<CircuitPart>> modules
		) implements TooltipAppender
{
	public static final Codec<CircuitComponent> CODEC	= RecordCodecBuilder.create(instance -> instance.group(
			CircuitPart.CODEC.listOf().optionalFieldOf("circuit").forGetter(CircuitComponent::modules)
			)
			.apply(instance, CircuitComponent::new));
	
	public static final PacketCodec<ByteBuf, CircuitComponent> PACKET_CODEC = PacketCodec.tuple(
			PacketCodecs.optional(CircuitPart.LIST_PACKET_CODEC), CircuitComponent::modules, 
			CircuitComponent::new);
	
	public static CircuitComponent of(List<CircuitPart> circuitIn)
	{
		return new CircuitComponent(circuitIn.isEmpty() ? Optional.empty() : Optional.of(circuitIn));
	}
	
	public static CircuitComponent empty() { return new CircuitComponent(Optional.empty()); }
	
	public List<LogicModule> circuit()
	{
		return modules.isEmpty() ? List.of() : modules.get().stream().map(CircuitPart::module).toList();
	}
	
	public void appendTooltip(TooltipContext context, Consumer<Text> tooltip, TooltipType type)
	{
		modules.ifPresentOrElse(
				s -> 
				{
					tooltip.accept(translate("gui", "logic_card.circuit", s.size()));
					s.forEach(m -> tooltip.accept(Text.literal(" * ").append(m.module().displayName())));
				}, 
				() -> tooltip.accept(translate("gui", "logic_card.no_circuit")));
	}
	
	public static record CircuitPart(LogicModule module, Vector2i pos)
	{
		public static final Codec<CircuitPart> CODEC	= RecordCodecBuilder.create(instance -> instance.group(
				LogicModule.CODEC.fieldOf("gate").forGetter(CircuitPart::module), 
				CDUtils.VEC2I_CODEC.fieldOf("grid").forGetter(CircuitPart::pos))
				.apply(instance, CircuitPart::new));
		public static final Codec<List<CircuitPart>> LIST_CODEC	= CODEC.listOf();
		public static final PacketCodec<ByteBuf, CircuitPart> PACKET_CODEC	= PacketCodec.tuple(
				LogicModule.PACKET_CODEC, CircuitPart::module, 
				CDUtils.VEC2I_PACKET_CODEC, CircuitPart::pos,
				CircuitPart::new);
		public static final PacketCodec<ByteBuf, List<CircuitPart>> LIST_PACKET_CODEC	= PacketCodec.of((val,buf) -> 
		{
			buf.writeInt(val.size());
			val.forEach(v -> PACKET_CODEC.encode(buf, v));
		}, buf -> 
		{
			List<CircuitPart> set = Lists.newArrayList();
			int size = buf.readInt();
			while(size-- > 0)
				set.add(PACKET_CODEC.decode(buf));
			return set;
		});
		
		public static CircuitPart fromPair(Pair<LogicModule,Vector2i> pair) { return new CircuitPart(pair.getLeft(), pair.getRight()); }
	}
}

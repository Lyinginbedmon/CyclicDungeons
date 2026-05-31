package com.lying.block.entity.logic;

import java.util.Optional;

import com.lying.block.IWireableBlock;
import com.lying.block.Port;
import com.lying.reference.Reference;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.text.MutableText;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Holder object containing a BlockPos and a port of the block at that position
 */
public record PortEntry(BlockPos pos, Port port)
{
	public static final Codec<PortEntry> CODEC	= RecordCodecBuilder.create(instance -> instance.group(
			BlockPos.CODEC.fieldOf("pos").forGetter(PortEntry::pos),
			Port.CODEC.fieldOf("port").forGetter(PortEntry::port)
			).apply(instance, PortEntry::new));
	public static final PacketCodec<ByteBuf, PortEntry> PACKET_CODEC	= PacketCodec.tuple(
			BlockPos.PACKET_CODEC, PortEntry::pos, 
			Port.PACKET_CODEC, PortEntry::port, 
			PortEntry::new);
	
	public MutableText displayName() { return Reference.ModInfo.translate("gui", "port_name", port.name(), pos.toShortString()); }
	
	public PortEntry relativeTo(BlockPos offset)
	{
		return new PortEntry(pos.subtract(offset), port);
	}
	
	public boolean equals(Object obj)
	{
		if(!(obj instanceof PortEntry))
			return false;
		
		PortEntry other = (PortEntry)obj;
		return pos.getManhattanDistance(other.pos()) == 0 && port.equals(other.port());
	}
	
	public boolean isActive(World world, BlockPos origin)
	{
		BlockPos target = pos.add(origin);
		Optional<IWireableBlock> wireable = IWireableBlock.getWireable(target, world);
		return wireable.isPresent() && wireable.get().isPortActive(port, target, world);
	}
}
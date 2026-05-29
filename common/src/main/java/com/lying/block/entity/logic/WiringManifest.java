package com.lying.block.entity.logic;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.common.collect.Lists;
import com.lying.block.IWireableBlock;
import com.lying.block.IWireableBlock.WireRecipient;
import com.lying.block.Port;
import com.lying.init.CDLogicGates;
import com.lying.reference.Reference;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.netty.buffer.ByteBuf;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.text.MutableText;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class WiringManifest
{
	public static final Codec<WiringManifest> CODEC	= RecordCodecBuilder.create(instance -> instance.group(
			ManifestEntry.LIST_CODEC.fieldOf("inputs").forGetter(m -> Lists.newArrayList(m.inputs.values())),
			ManifestEntry.LIST_CODEC.fieldOf("outputs").forGetter(m -> Lists.newArrayList(m.outputs.values())))
			.apply(instance, (i,o) -> 
			{
				WiringManifest manifest = new WiringManifest();
				i.forEach(e -> manifest.inputs.put(e.port(), e));
				o.forEach(e -> manifest.outputs.put(e.port(), e));
				return manifest;
			}));
	
	private Map<Port, ManifestEntry> inputs = new HashMap<>();
	private Map<Port, ManifestEntry> outputs = new HashMap<>();
	
	public WiringManifest() { }
	
	public boolean isEmpty() { return inputs.isEmpty() && outputs.isEmpty(); }
	
	public int size()
	{
		return totalInputs() + totalOutputs();
	}
	
	public int totalInputs()
	{
		return tallyWires(inputs.values());
	}
	
	public int totalOutputs()
	{
		return tallyWires(outputs.values());
	}
	
	protected int tallyWires(Collection<ManifestEntry> entries)
	{
		int tally = 0;
		for(ManifestEntry input : entries)
			tally += input.positions.size();
		return tally;
	}
	
	public Optional<ManifestEntry> getPort(Port name, boolean isInput)
	{
		ManifestEntry port = (isInput ? inputs : outputs).getOrDefault(name, null);
		if(port == null)
		{
			port = new ManifestEntry(name);
			(isInput ? inputs : outputs).put(name, port);
		}
		return port == null ? Optional.empty() : Optional.of(port);
	}
	
	/** Returns all blocks being listened to by an input port */
	public List<BlockPos> getInputListeners(Port name, BlockPos offset)
	{
		ManifestEntry port = inputs.getOrDefault(name, null);
		return port == null ? List.of() : port.connections().stream().map(PortEntry::pos).map(p -> p.add(offset)).toList();
	}
	
	/** Returns all blocks listening to the given output port */
	public List<BlockPos> getOutputListeners(Port name, BlockPos offset)
	{
		ManifestEntry port = outputs.getOrDefault(name, null);
		return port == null ? List.of() : port.connections().stream().map(PortEntry::pos).map(p -> p.add(offset)).toList();
	}
	
	public void clear()
	{
		inputs.clear();
		outputs.clear();
	}
	
	public boolean cleanInputsGlobal(World world)
	{
		boolean result = false;
		for(ManifestEntry entry : inputs.values())
			result = entry.cleanGlobal(world, WireRecipient.SENSOR) || result;
		return result;
	}
	
	public boolean cleanInputsLocal(World world, BlockPos offset)
	{
		boolean result = false;
		for(ManifestEntry entry : inputs.values())
			result = entry.cleanLocal(world, offset, WireRecipient.SENSOR) || result;
		return result;
	}
	
	public boolean cleanOutputsGlobal(World world)
	{
		boolean result = false;
		for(ManifestEntry entry : outputs.values())
			result = entry.cleanGlobal(world, WireRecipient.ACTOR) || result;
		return result;
	}
	
	public boolean cleanOutputsLocal(World world, BlockPos offset)
	{
		boolean result = false;
		for(ManifestEntry entry : outputs.values())
			result = entry.cleanLocal(world, offset, WireRecipient.ACTOR) || result;
		return result;
	}
	
	public boolean cleanLocal(BlockPos origin, World world)
	{
		boolean result = false;
		if(cleanInputsLocal(world, origin))
			result = true;
		if(cleanOutputsLocal(world, origin))
			result = true;
		return result;
	}
	
	/**
	 * Holder object containing an input port name and a list of paired BlockPos positions and output port names
	 */
	public static class ManifestEntry
	{
		public static final Codec<List<ManifestEntry>> LIST_CODEC	= Codec.of(ManifestEntry::encodeSet, ManifestEntry::decodeSet);
		
		private final Port port;
		private final List<PortEntry> positions = Lists.newArrayList();
		
		public ManifestEntry(Port name)
		{
			port = name;
		}
		
		public ManifestEntry attach(PortEntry entry)
		{
			if(!positions.contains(entry))
				positions.add(entry);
			return this;
		}
		
		public ManifestEntry attach(BlockPos pos, Port port)
		{
			return attach(new PortEntry(pos, port));
		}
		
		public ManifestEntry attach(BlockPos pos)
		{
			return attach(pos, CDLogicGates.OUTPUT);
		}
		
		public ManifestEntry attachAllBlocks(List<BlockPos> pos)
		{
			pos.forEach(this::attach);
			return this;
		}
		
		public ManifestEntry attachAll(List<PortEntry> pos)
		{
			pos.forEach(this::attach);
			return this;
		}
		
		public Port port() { return port; }
		
		public List<PortEntry> connections() { return positions; }
		
		public boolean isEmpty() { return positions.isEmpty(); }
		
		public boolean cleanGlobal(World world, final WireRecipient type)
		{
			return cleanLocal(world, BlockPos.ORIGIN, type);
		}
		
		public boolean cleanLocal(World world, BlockPos offset, final WireRecipient typeIn)
		{
			return positions.removeIf(p -> 
			{
				BlockPos pos = p.pos().add(offset);
				BlockState state = world.getBlockState(pos);
				Block block = state.getBlock();
				if(!(block instanceof IWireableBlock))
					return true;
				
				WireRecipient type = ((IWireableBlock)block).type();
				return type != WireRecipient.LOGIC && type != typeIn;
			});
		}
		
		public boolean status(World world, BlockPos offset)
		{
			return positions.stream().anyMatch(p -> p.isActive(world, offset));
		}
		
		private static <T extends Object> DataResult<T> encodeSet(final List<ManifestEntry> manifest, final DynamicOps<T> ops, final T prefix)
		{
			RecordBuilder<T> map = ops.mapBuilder();
			for(ManifestEntry entry : manifest)
			{
				T key = ops.createString(entry.port().name());
				List<PortEntry> values = entry.connections();
				if(values == null || values.isEmpty())
					continue;
				else if(values.size() == 1)
					map.add(key, PortEntry.CODEC.encodeStart(ops, values.getFirst()).getOrThrow());
				else
					map.add(key, PortEntry.CODEC.listOf().encodeStart(ops, values).getOrThrow());
			}
			return map.build(prefix);
		}
		
		private static <T> DataResult<Pair<List<ManifestEntry>, T>> decodeSet(final DynamicOps<T> ops, final T input)
		{
			List<ManifestEntry> wires = Lists.newArrayList();
			MapLike<T> map = ops.getMap(input).result().get();
			map.entries().forEach(entry -> 
			{
				String key = ops.getStringValue(entry.getFirst()).getOrThrow();
				T value = entry.getSecond();
				DataResult<PortEntry> single = PortEntry.CODEC.parse(ops, value);
				if(single.isSuccess())
					wires.add(new ManifestEntry(Port.of(key)).attach(single.getOrThrow()));
				else
					wires.add(new ManifestEntry(Port.of(key)).attachAll(PortEntry.CODEC.listOf().parse(ops, value).getOrThrow()));
			});
			return DataResult.success(Pair.of(wires, input));
		}
	}
	
	/**
	 * Holder object containing a BlockPos and a port of the block at that position
	 */
	public static record PortEntry(BlockPos pos, Port port)
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
			IWireableBlock wireable = IWireableBlock.getWireable(target, world);
			return wireable.isPortActive(port, target, world);
		}
	}
}
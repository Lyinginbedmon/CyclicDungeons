package com.lying.block.entity.logic;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.common.collect.Lists;
import com.lying.block.Port;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;

/** Map of ports to the named wires attached to them */
public class PortSet
{
	public static final Codec<PortSet> CODEC	= Codec.of(PortSet::encode, PortSet::decode);
	public static final PacketCodec<ByteBuf, PortSet> PACKET_CODEC	= PacketCodec.of(PortSet::write, PortSet::read);
	private static final Codec<String> CODEC_STRING	= Codec.STRING;
	private static final Codec<List<String>> CODEC_LIST	= CODEC_STRING.listOf();
	
	private final Map<Port, List<String>> values = new HashMap<>();
	
	private void write(ByteBuf buf)
	{
		buf.writeInt(values.size());
		values.entrySet().forEach(entry -> 
		{
			Port.PACKET_CODEC.encode(buf, entry.getKey());
			buf.writeInt(entry.getValue().size());
			entry.getValue().forEach(s -> PacketCodecs.STRING.encode(buf, s));
		});
	}
	
	private static PortSet read(ByteBuf buf)
	{
		PortSet state = new PortSet();
		int size = buf.readInt();
		while(size-- > 0)
		{
			Port port = Port.PACKET_CODEC.decode(buf);
			List<String> set = Lists.newArrayList();
			int scale = buf.readInt();
			while(scale-- > 0)
				set.add(PacketCodecs.STRING.decode(buf));
			
			state.values.put(port, set);
		}
		return state;
	}
	
	public void put(Port key, String... value)
	{
		List<String> set = values.getOrDefault(key, Lists.newArrayList());
		for(String wire : value)
			if(!set.contains(wire))
				set.add(wire);
		values.put(key, set);
	}
	
	public List<String> get(Port key)
	{
		return values.getOrDefault(key, List.of());
	}
	
	public Collection<Port> ports()
	{
		return values.keySet();
	}
	
	public boolean hasWire(String wireName)
	{
		for(List<String> value : values.values())
			if(value.contains(wireName))
				return true;
		return false;
	}
	
	/** Returns a list of all wires that this set contacts */
	public List<String> wires()
	{
		List<String> wires = Lists.newArrayList();
		for(List<String> value : values.values())
			for(String wire : value)
				if(!wires.contains(wire))
					wires.add(wire);
		return wires;
	}
	
	public boolean clear(String wireName)
	{
		boolean result = false;
		for(Port port : values.keySet())
		{
			List<String> set = Lists.newArrayList(values.get(port));
			if(!set.contains(wireName))
				continue;
			
			set.removeIf(wireName::equals);
			values.put(port, set);
		}
		return result;
	}
	
	public boolean isEmpty() { return values.isEmpty(); }
	
	public void clear() { values.clear(); }
	
	private static <T extends Object> DataResult<T> encode(final PortSet wireSet, final DynamicOps<T> ops, final T prefix)
	{
		RecordBuilder<T> map = ops.mapBuilder();
		for(Entry<Port, List<String>> entry : wireSet.values.entrySet())
		{
			T key = ops.createString(entry.getKey().name());
			List<String> values = entry.getValue();
			if(values == null || values.isEmpty())
				continue;
			else if(values.size() == 1)
				map.add(key, CODEC_STRING.encodeStart(ops, values.getFirst()).getOrThrow());
			else
				map.add(key, CODEC_LIST.encodeStart(ops, values).getOrThrow());
		}
		return map.build(prefix);
	}
	
	private static <T> DataResult<Pair<PortSet, T>> decode(final DynamicOps<T> ops, final T input)
	{
		PortSet wires = new PortSet();
		MapLike<T> map = ops.getMap(input).result().get();
		map.entries().forEach(entry -> 
		{
			String key = ops.getStringValue(entry.getFirst()).getOrThrow();
			T value = entry.getSecond();
			DataResult<String> single = CODEC_STRING.parse(ops, value);
			if(single.isSuccess())
				wires.put(Port.of(key), single.getOrThrow());
			else
				wires.put(Port.of(key), CODEC_LIST.parse(ops, value).getOrThrow().toArray(new String[0]));
		});
		return DataResult.success(Pair.of(wires, input));
	}
}
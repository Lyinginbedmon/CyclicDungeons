package com.lying.block.entity.logic;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;

public class WireSet
{
	public static final Codec<WireSet> CODEC	= Codec.of(WireSet::encode, WireSet::decode);
	private static final Codec<String> CODEC_STRING	= Codec.STRING;
	private static final Codec<List<String>> CODEC_LIST	= CODEC_STRING.listOf();
	
	private final Map<String, List<String>> values = new HashMap<>();
	
	public void put(String key, String... value)
	{
		List<String> set = values.getOrDefault(key, Lists.newArrayList());
		for(String wire : value)
			if(!set.contains(wire))
				set.add(wire);
		values.put(key, set);
	}
	
	public List<String> get(String key)
	{
		return values.getOrDefault(key, List.of());
	}
	
	public Collection<String> ports()
	{
		return values.keySet();
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
	
	public boolean isEmpty() { return values.isEmpty(); }
	
	public void clear() { values.clear(); }
	
	private static <T extends Object> DataResult<T> encode(final WireSet wireSet, final DynamicOps<T> ops, final T prefix)
	{
		RecordBuilder<T> map = ops.mapBuilder();
		for(Entry<String, List<String>> entry : wireSet.values.entrySet())
		{
			T key = ops.createString(entry.getKey());
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
	
	private static <T> DataResult<Pair<WireSet, T>> decode(final DynamicOps<T> ops, final T input)
	{
		WireSet wires = new WireSet();
		MapLike<T> map = ops.getMap(input).result().get();
		map.entries().forEach(entry -> 
		{
			String key = ops.getStringValue(entry.getFirst()).getOrThrow();
			T value = entry.getSecond();
			DataResult<String> single = CODEC_STRING.parse(ops, value);
			if(single.isSuccess())
				wires.put(key, single.getOrThrow());
			else
				wires.put(key, CODEC_LIST.parse(ops, value).getOrThrow().toArray(new String[0]));
		});
		return DataResult.success(Pair.of(wires, input));
	}
}
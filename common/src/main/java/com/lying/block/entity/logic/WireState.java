package com.lying.block.entity.logic;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;

public class WireState
{
	public static final Codec<WireState> CODEC	= Codec.of(WireState::encode, WireState::decode);
	
	private final Map<String, Boolean> values = new HashMap<>();
	
	public void copy(WireState state)
	{
		values.clear();
		for(Entry<String, Boolean> entry : state.values.entrySet())
			values.put(entry.getKey(), entry.getValue());
	}
	
	public void setLive(String key)
	{
		put(key, true);
	}
	
	public void put(String key, boolean value)
	{
		values.put(key, value);
	}
	
	public boolean get(String key)
	{
		return values.getOrDefault(key, false);
	}
	
	public boolean isEmpty() { return values.isEmpty(); }
	
	public boolean isInert()
	{
		for(Boolean var : values.values())
			if(var)
				return false;
		return true;
	}
	
	public void clear() { values.clear(); }
	
	private static <T extends Object> DataResult<T> encode(final WireState wireSet, final DynamicOps<T> ops, final T prefix)
	{
		RecordBuilder<T> map = ops.mapBuilder();
		for(Entry<String, Boolean> entry : wireSet.values.entrySet())
		{
			T key = ops.createString(entry.getKey());
			boolean values = entry.getValue();
			if(!values)
				continue;
			else
				map.add(key, Codec.BOOL.encodeStart(ops, values).getOrThrow());
		}
		return map.build(prefix);
	}
	
	private static <T> DataResult<Pair<WireState, T>> decode(final DynamicOps<T> ops, final T input)
	{
		WireState wires = new WireState();
		MapLike<T> map = ops.getMap(input).result().get();
		map.entries().forEach(entry -> 
		{
			String key = ops.getStringValue(entry.getFirst()).getOrThrow();
			T value = entry.getSecond();
			DataResult<Boolean> single = Codec.BOOL.parse(ops, value);
			if(single.isSuccess())
				wires.put(key, single.getOrThrow());
		});
		return DataResult.success(Pair.of(wires, input));
	}
}
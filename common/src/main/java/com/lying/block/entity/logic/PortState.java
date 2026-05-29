package com.lying.block.entity.logic;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;

import com.lying.block.Port;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;

/** Map of wire names to their states */
public class PortState
{
	public static final Codec<PortState> CODEC	= Codec.of(PortState::encode, PortState::decode);
	
	private final Map<Port, Boolean> values = new HashMap<>();
	
	public void copy(PortState state)
	{
		values.clear();
		for(Entry<Port, Boolean> entry : state.values.entrySet())
			values.put(entry.getKey(), entry.getValue());
	}
	
	public void setLive(Port key)
	{
		put(key, true);
	}
	
	public void put(Port key, boolean value)
	{
		values.put(key, value);
	}
	
	public boolean get(Port key)
	{
		return values.getOrDefault(key, false);
	}
	
	public boolean isEmpty() { return values.isEmpty(); }
	
	/** Returns TRUE if all recorded values are FALSE */
	public boolean isInert()
	{
		return values.values().stream().allMatch(v -> !v);
	}
	
	public final boolean isWorthStoring() { return !isEmpty() && !isInert(); }
	
	public Collection<Port> keys() { return values.keySet(); }
	
	public Collection<Boolean> values() { return values.values(); }
	
	public void clear() { values.clear(); }
	
	public void print(Consumer<String> printFunc)
	{
		values.entrySet().forEach(e -> printFunc.accept(e.getKey()+": "+e.getValue()));
	}
	
	private static <T extends Object> DataResult<T> encode(final PortState wireSet, final DynamicOps<T> ops, final T prefix)
	{
		RecordBuilder<T> map = ops.mapBuilder();
		for(Entry<Port, Boolean> entry : wireSet.values.entrySet())
		{
			T key = ops.createString(entry.getKey().name());
			boolean values = entry.getValue();
			if(values)
				map.add(key, Codec.BOOL.encodeStart(ops, values).getOrThrow());
		}
		return map.build(prefix);
	}
	
	private static <T> DataResult<Pair<PortState, T>> decode(final DynamicOps<T> ops, final T input)
	{
		PortState wires = new PortState();
		MapLike<T> map = ops.getMap(input).result().get();
		map.entries().forEach(entry -> 
		{
			String key = ops.getStringValue(entry.getFirst()).getOrThrow();
			T value = entry.getSecond();
			DataResult<Boolean> single = Codec.BOOL.parse(ops, value);
			if(single.isSuccess())
				wires.put(Port.of(key), single.getOrThrow());
		});
		return DataResult.success(Pair.of(wires, input));
	}
}
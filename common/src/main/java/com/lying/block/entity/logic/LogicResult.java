package com.lying.block.entity.logic;

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

public class LogicResult
{
	public static final Codec<LogicResult> CODEC	= Codec.of(LogicResult::encode, LogicResult::decode);
	
	private final Map<Port, Boolean> values = new HashMap<>();
	
	public void copy(LogicResult state)
	{
		values.clear();
		for(Entry<Port, Boolean> entry : state.values.entrySet())
			values.put(entry.getKey(), entry.getValue());
	}
	
	public static LogicResult create() { return new LogicResult(); }
	
	public LogicResult put(Port port, boolean value)
	{
		values.put(port, value);
		return this;
	}
	
	/** Returns the status of the given port */
	public boolean get(Port port)
	{
		return values.getOrDefault(port, false);
	}
	
	public boolean isEmpty() { return values.isEmpty(); }
	
	/** Returns TRUE if all ports are FALSE */
	public boolean isInert()
	{
		return values.values().stream().allMatch(v -> !v);
	}
	
	public final boolean isWorthStoring() { return !isEmpty() && !isInert(); }
	
	public void clear() { values.clear(); }
	
	public void print(Consumer<String> printFunc)
	{
		values.entrySet().forEach(e -> printFunc.accept(e.getKey()+": "+e.getValue()));
	}
	
	private static <T extends Object> DataResult<T> encode(final LogicResult result, final DynamicOps<T> ops, final T prefix)
	{
		RecordBuilder<T> map = ops.mapBuilder();
		for(Entry<Port, Boolean> entry : result.values.entrySet())
		{
			T key = ops.createString(entry.getKey().name());
			boolean values = entry.getValue();
			if(!values)
				continue;
			else
				map.add(key, Codec.BOOL.encodeStart(ops, values).getOrThrow());
		}
		return map.build(prefix);
	}
	
	private static <T> DataResult<Pair<LogicResult, T>> decode(final DynamicOps<T> ops, final T input)
	{
		LogicResult wires = new LogicResult();
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
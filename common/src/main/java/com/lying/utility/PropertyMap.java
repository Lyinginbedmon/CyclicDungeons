package com.lying.utility;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import com.google.gson.JsonElement;
import com.lying.CyclicDungeons;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.Property;

/** Serializable hashmap for storing/testing/applying sets of blockstate property values */
@SuppressWarnings("serial")
public class PropertyMap extends HashMap<String, String>
{
	public static final Codec<PropertyMap> CODEC	= Codec.of(PropertyMap::encode, PropertyMap::decode);
	
	private static <T> DataResult<T> encode(final PropertyMap map, final DynamicOps<T> ops, final T prefix)
	{
		Map<T,T> list = new HashMap<>();
		map.entrySet().forEach(entry -> list.put(ops.createString(entry.getKey()), ops.createString(entry.getValue())));
		return DataResult.success(ops.createMap(list));
	}
	
	private static <T> DataResult<Pair<PropertyMap, T>> decode(final DynamicOps<T> ops, final T input)
	{
		PropertyMap map = new PropertyMap();
		ops.getMap(input).result().ifPresent(m -> 
			m.entries().forEach(entry -> 
				map.put(ops.getStringValue(entry.getFirst()).getOrThrow(), ops.getStringValue(entry.getSecond()).getOrThrow())));
		return DataResult.success(Pair.of(map, input));
	}
	
	public static PropertyMap create() { return new PropertyMap(); }
	
	public <T extends Comparable<T>> PropertyMap add(Property<T> property, T value)
	{
		put(property.getName(), property.name(value));
		return this;
	}
	
	public JsonElement toJson()
	{
		return CODEC.encodeStart(JsonOps.INSTANCE, this).getOrThrow();
	}
	
	public static PropertyMap fromJson(JsonElement obj)
	{
		return CODEC.parse(JsonOps.INSTANCE, obj).resultOrPartial(CyclicDungeons.LOGGER::error).orElseThrow();
	}
	
	@Nullable
	public <T extends Comparable<T>> String put(Property<T> property, T value)
	{
		return put(property.getName(), property.name(value));
	}
	
	/** Returns true if the values in the property map of the given state match all the values of this map */
	public boolean matches(BlockState state)
	{
		StateManager<Block, BlockState> manager = state.getBlock().getStateManager();
		return entrySet().stream().allMatch(entry ->
		{
			Property<?> property = manager.getProperty(entry.getKey());
			if(property == null)
				return false;
			
			Optional<?> value = property.parse(entry.getValue());
			return value.isPresent() && state.get(property).equals(value.get());
		});
	}
	
	/** Returns the given state modified to have all applicable values of this map */
	public BlockState applyTo(BlockState state)
	{
		StateManager<Block, BlockState> manager = state.getBlock().getStateManager();
		BlockState newState = state;
		for(Entry<String,String> entry : entrySet())
		{
			Property<?> property = manager.getProperty(entry.getKey());
			if(property != null)
				newState = setValue(property, entry.getValue(), newState);
		}
		return newState;
	}
	
	/** Attempts to set the value of the given property in the given state, provided the given string refers to a valid value */
	protected static <T extends Comparable<T>> BlockState setValue(Property<T> property, String string, BlockState state)
	{
		Optional<T> value = property.parse(string);
		return value.isPresent() ? state.with(property, value.get()) : state;
	}
	
	public boolean equals(Object b)
	{
		return b instanceof PropertyMap && equals(this, (PropertyMap)b);
	}
	
	public static boolean equals(PropertyMap a, PropertyMap b)
	{
		if(a.size() != b.size())
			return false;
		
		for(Entry<String, String> entry : a.entrySet())
			if(!b.containsKey(entry.getKey()) || !b.get(entry.getKey()).equals(entry.getValue()))
				return false;
		
		return true;
	}
}
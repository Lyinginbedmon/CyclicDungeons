package com.lying.init;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

public class CDLogicGates
{
	private static final Map<String, Supplier<LogicGate>> MODULES	= new HashMap<>();
	
	public static final String INPUT	= "input";
	
	// Always TRUE
	public static final Supplier<LogicGate> TRUE	= register("true", s -> new LogicGate(s)
	{
		public boolean getOutput(Map<String, List<String>> inputs, Map<String, Boolean> wireStates) { return true; }
	});
	
	// Always FALSE
	public static final Supplier<LogicGate> FALSE	= register("false", s -> new LogicGate(s)
	{
		public boolean getOutput(Map<String, List<String>> inputs, Map<String, Boolean> wireStates) { return false; }
	});
	
	// TRUE only if all input values are FALSE
	public static final Supplier<LogicGate> NOT	= register("not", s -> new LogicGate(s)
	{
		public boolean getOutput(Map<String, List<String>> inputs, Map<String, Boolean> wireStates)
		{
			return inputs.getOrDefault(INPUT, List.of()).stream().noneMatch(wireStates::get);
		}
	});
	// TRUE only if all input values are TRUE
	public static final Supplier<LogicGate> AND	= register("and", s -> new LogicGate(s)
	{
		public boolean getOutput(Map<String, List<String>> inputs, Map<String, Boolean> wireStates)
		{
			return inputs.getOrDefault(INPUT, List.of()).stream().allMatch(wireStates::get);
		}
	});
	// TRUE if any input value is TRUE
	public static final Supplier<LogicGate> OR	= register("or", s -> new LogicGate(s)
	{
		public boolean getOutput(Map<String, List<String>> inputs, Map<String, Boolean> wireStates)
		{
			return inputs.getOrDefault(INPUT, List.of()).stream().anyMatch(wireStates::get);
		}
	});
	// TRUE if only one input value is TRUE
	public static final Supplier<LogicGate> XOR	= register("xor", s -> new LogicGate(s)
	{
		public boolean getOutput(Map<String, List<String>> inputs, Map<String, Boolean> wireStates)
		{
			boolean result = false;
			for(String input : inputs.getOrDefault(INPUT, List.of()))
				if(wireStates.get(input))
					if(!result)
						result = true;
					else
						return false;
			return result;
		}
	});
	
	public static Supplier<LogicGate> register(String nameIn, Function<String, LogicGate> factory)
	{
		final Supplier<LogicGate> supplier = () -> factory.apply(nameIn);
		MODULES.put(nameIn, supplier);
		return supplier;
	}
	
	public static abstract class LogicGate
	{
		public static final Codec<LogicGate> CODEC	= Codec.STRING.comapFlatMap(s -> 
		{
			LogicGate gate = CDLogicGates.MODULES.getOrDefault(s,() -> null).get();
			return gate == null ? DataResult.error(() -> "Logic gate name not recognised") : DataResult.success(gate);
		}, LogicGate::registryName);
		
		private final String registryName;
		
		protected LogicGate(String nameIn)
		{
			registryName = nameIn;
		}
		
		public String registryName() { return registryName; }
		
		public abstract boolean getOutput(Map<String, List<String>> inputs, Map<String, Boolean> wireStates);
	}
}

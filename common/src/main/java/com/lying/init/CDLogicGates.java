package com.lying.init;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import com.lying.block.entity.logic.WireSet;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

public class CDLogicGates
{
	private static final Map<String, Supplier<LogicGate>> MODULES	= new HashMap<>();
	
	public static final String INPUT	= "input";
	public static final String OUTPUT	= "result";
	
	// Always TRUE
	public static final Supplier<LogicGate> TRUE	= register("true", s -> new LogicGate(s, (inputs, wires) -> true));
	
	// Always FALSE
	public static final Supplier<LogicGate> FALSE	= register("false", s -> new LogicGate(s, (inputs, wires) -> false));
	
	// TRUE only if all input values are uniformly TRUE
	public static final Supplier<LogicGate> AND		= register("and", s -> new LogicGate(s, (inputs, wires) -> inputs.get(INPUT).stream().allMatch(wires::get)));
	// TRUE only if all input values are not uniformly TRUE
	public static final Supplier<LogicGate> NAND	= register("nand", s -> new LogicGate(s, (inputs, wires) -> !inputs.get(INPUT).stream().allMatch(wires::get)));
	// TRUE if any input value is TRUE
	public static final Supplier<LogicGate> OR		= register("or", s -> new LogicGate(s, (inputs, wires) -> inputs.get(INPUT).stream().anyMatch(wires::get)));
	// TRUE while all input values are FALSE, functionally a multi-input NOT
	public static final Supplier<LogicGate> NOR		= register("nor", s -> new LogicGate(s, (inputs, wires) -> !inputs.get(INPUT).stream().anyMatch(wires::get)));
	// TRUE if only one input value is TRUE
	public static final Supplier<LogicGate> XOR		= register("xor", s -> new LogicGate(s, (inputs, wires) -> 
		{
			boolean result = false;
			for(String input : inputs.get(INPUT))
				if(wires.get(input))
					if(!result)
						result = true;
					else
						return false;
			return result;
		}
	));
	// TRUE if all input values share the same value
	public static final Supplier<LogicGate> XNOR	= register("xnor", s -> new LogicGate(s, (inputs, wires) -> 
		{
			List<String> lines = inputs.get(INPUT);
			if(lines.isEmpty())
				return true;
			
			final boolean goal = wires.get(lines.removeFirst());
			while(!lines.isEmpty())
				if(wires.get(lines.removeFirst()) != goal)
					return false;
			return true;
		}));
	
	// TODO Implement RNG, memory cells
	
	public static Supplier<LogicGate> register(String nameIn, Function<String, LogicGate> factory)
	{
		final Supplier<LogicGate> supplier = () -> factory.apply(nameIn);
		MODULES.put(nameIn, supplier);
		return supplier;
	}
	
	public static class LogicGate
	{
		public static final Codec<LogicGate> CODEC	= Codec.STRING.comapFlatMap(s -> 
		{
			LogicGate gate = CDLogicGates.MODULES.getOrDefault(s,() -> null).get();
			return gate == null ? DataResult.error(() -> "Logic gate name not recognised") : DataResult.success(gate);
		}, LogicGate::registryName);
		
		private final String registryName;
		private final GateLogic logic;
		
		public LogicGate(String nameIn, GateLogic logicIn)
		{
			registryName = nameIn;
			logic = logicIn;
		}
		
		public final String registryName() { return registryName; }
		
		public final boolean getOutput(WireSet inputs, Map<String, Boolean> wireStates) { return logic.result(inputs, wireStates); }
	}
	
	@FunctionalInterface
	public interface GateLogic
	{
		public boolean result(WireSet inputs, Map<String, Boolean> wireStates);
	}
}

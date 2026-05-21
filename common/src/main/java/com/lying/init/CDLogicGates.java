package com.lying.init;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import com.lying.block.entity.logic.WireSet;
import com.lying.block.entity.logic.WireState;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

public class CDLogicGates
{
	private static final Map<String, Supplier<LogicGate>> MODULES	= new HashMap<>();
	
	public static final String INPUT	= "input";
	public static final String OUTPUT	= "result";
	public static final String RESET	= "reset";
	
	// Always TRUE
	public static final Supplier<LogicGate> TRUE	= register("true", s -> new LogicGate(s, (inputs, wires, pWires, pOut) -> true));
	
	// Always FALSE
	public static final Supplier<LogicGate> FALSE	= register("false", s -> new LogicGate(s, (inputs, wires, pWires, pOut) -> false));
	
	// TRUE only if all input values are uniformly TRUE
	public static final Supplier<LogicGate> AND		= register("and", s -> new LogicGate(s, (inputs, wires, pWires, pOut) -> inputs.get(INPUT).stream().allMatch(wires::get)));
	// TRUE only if all input values are not uniformly TRUE
	public static final Supplier<LogicGate> NAND	= register("nand", s -> new LogicGate(s, (inputs, wires, pWires, pOut) -> !inputs.get(INPUT).stream().allMatch(wires::get)));
	// TRUE if any input value is TRUE
	public static final Supplier<LogicGate> OR		= register("or", s -> new LogicGate(s, (inputs, wires, pWires, pOut) -> isWireLive(INPUT, inputs, wires)));
	// TRUE while all input values are FALSE, functionally a multi-input NOT
	public static final Supplier<LogicGate> NOR		= register("nor", s -> new LogicGate(s, (inputs, wires, pWires, pOut) -> !isWireLive(INPUT, inputs, wires)));
	// TRUE if only one input value is TRUE
	public static final Supplier<LogicGate> XOR		= register("xor", s -> new LogicGate(s, (inputs, wires, pWires, pOut) -> 
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
	public static final Supplier<LogicGate> XNOR	= register("xnor", s -> new LogicGate(s, (inputs, wires, pWires, pOut) -> 
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
	
	// TRUE if any input became TRUE in this frame
	public static final Supplier<LogicGate> EDGE_R	= register("rising_edge", s -> new LogicGate(s, (inputs, wires, pWires, pOut) -> isRisingEdge(INPUT, inputs, wires, pWires)));
	// TRUE if all inputs became FALSE in this frame
	public static final Supplier<LogicGate> EDGE_F	= register("falling_edge", s -> new LogicGate(s, (inputs, wires, pWires, pOut) -> isWireLive(INPUT, inputs, pWires) && !isWireLive(INPUT, inputs, wires)));
	
	// TODO Implement RNG, memory cells
	
	// Toggles its output on the rising edge of its input
	public static final Supplier<LogicGate> TOGGLE	= register("toggle", s -> new LogicGate(s, (inputs, wires, pWires, pOut) -> 
	{
		final boolean risingEdge = isRisingEdge(INPUT, inputs, wires, pWires);
		final boolean resetOn = isWireLive(RESET, inputs, wires);
		return resetOn ? false : (risingEdge ? !pOut : pOut);
	}));
	
	public static boolean isWireLive(String port, WireSet inputs, WireState wires)
	{
		return inputs.get(port).stream().anyMatch(wires::get);
	}
	
	public static boolean isRisingEdge(String port, WireSet inputs, WireState wires, WireState pWires)
	{
		return !isWireLive(port, inputs, pWires) && isWireLive(port, inputs, wires);
	}
	
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
		
		public final boolean getResult(WireSet inputs, WireState wireStates, WireState prevWires, boolean prevResult)
		{
			return logic.result(inputs, wireStates, prevWires, prevResult);
		}
	}
	
	@FunctionalInterface
	public interface GateLogic
	{
		/**
		 * @param inputs		The nominated input wires to this gate, by input port
		 * @param wireStates	The state of all wires in the circuit currently
		 * @param prevWires		The state of all wires in the circuit in the previous frame
		 * @param prevResult	The result of this gate in the previous frame
		 * @return
		 */
		public boolean result(WireSet inputs, WireState wireStates, WireState prevWires, boolean prevResult);
	}
}

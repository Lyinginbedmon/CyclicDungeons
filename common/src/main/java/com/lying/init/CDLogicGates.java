package com.lying.init;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import com.google.common.collect.Lists;
import com.lying.block.entity.ModularLogicBlockEntity;
import com.lying.block.entity.logic.LogicResult;
import com.lying.block.entity.logic.WireState;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.random.Random;

public class CDLogicGates
{
	private static final Map<String, Supplier<LogicGate>> MODULES	= new HashMap<>();
	
	public static final String INPUT	= "input";
	public static final String OUTPUT	= "output";
	public static final String RESET	= "reset";
	
	/** Always TRUE */
	public static final Supplier<LogicGate> TRUE	= register("true", s -> new LogicGate(s, (ports, pPorts, pOut, tile) -> LogicResult.create().put(OUTPUT, true)));
	
	/** Always FALSE */
	public static final Supplier<LogicGate> FALSE	= register("false", s -> new LogicGate(s, (ports, pPorts, pOut, tile) -> LogicResult.create().put(OUTPUT, false)));
	
	/**  TRUE only if all input values are uniformly TRUE */
	public static final Supplier<LogicGate> AND		= register("and", s -> new LogicGate(s, (ports, pPorts, pOut, tile) -> LogicResult.create().put(OUTPUT, ports.values().stream().allMatch(v -> v))));
	/** TRUE only if all input values are not uniformly TRUE */
	public static final Supplier<LogicGate> NAND	= register("nand", s -> new LogicGate(s, (ports, pPorts, pOut, tile) -> LogicResult.create().put(OUTPUT, !ports.values().stream().allMatch(v -> v))));
	/** TRUE if any input value is TRUE */
	public static final Supplier<LogicGate> OR		= register("or", s -> new LogicGate(s, (ports, pPorts, pOut, tile) -> LogicResult.create().put(OUTPUT, ports.values().stream().anyMatch(v -> v))));
	/** TRUE while all input values are FALSE, functionally a multi-input NOT */
	public static final Supplier<LogicGate> NOR		= register("nor", s -> new LogicGate(s, (ports, pPorts, pOut, tile) -> LogicResult.create().put(OUTPUT, ports.values().stream().noneMatch(v -> v))));
	/** TRUE if only one input value is TRUE */
	public static final Supplier<LogicGate> XOR		= register("xor", s -> new LogicGate(s, (ports, pPorts, pOut, tile) -> 
		{
			boolean result = false;
			for(Boolean var : ports.values())
				if(var)
					if(!result)
						result = true;
					else
						return LogicResult.create().put(OUTPUT, false);
			return LogicResult.create().put(OUTPUT, result);
		}
	));
	/** TRUE if all input values share the same value */
	public static final Supplier<LogicGate> XNOR	= register("xnor", s -> new LogicGate(s, (ports, pPorts, pOut, tile) -> 
		{
			List<Boolean> lines = Lists.newArrayList(ports.values());
			if(lines.isEmpty())
				return LogicResult.create().put(OUTPUT, true);
			
			final boolean goal = lines.removeFirst();
			while(!lines.isEmpty())
				if(lines.removeFirst() != goal)
					return LogicResult.create().put(OUTPUT, false);
			return LogicResult.create().put(OUTPUT, true);
		}));
	
	/** TRUE if any input became TRUE in this frame */
	public static final Supplier<LogicGate> EDGE_R	= register("rising_edge", s -> new LogicGate(s, (ports, pPorts, pOut, tile) -> LogicResult.create().put(OUTPUT, isRisingEdge(INPUT, ports, pPorts))));
	/** TRUE if all inputs became FALSE in this frame */
	public static final Supplier<LogicGate> EDGE_F	= register("falling_edge", s -> new LogicGate(s, (ports, pPorts, pOut, tile) -> LogicResult.create().put(OUTPUT, pPorts.get(INPUT) && !ports.get(INPUT))));
	
	/** Outputs a random value when it receives an input */
	public static final Supplier<LogicGate> RAND	= register("random", s -> new LogicGate(s, (ports, pPorts, pOut, tile) -> LogicResult.create().put(OUTPUT, 
		isRisingEdge(INPUT, ports, pPorts) ? 
			Random.create().nextBoolean() : 
			pOut.get(OUTPUT))));
	
	/** Toggles its output on the rising edge of its input */
	public static final Supplier<LogicGate> TOGGLE	= register("toggle", s -> new LogicGate(s, (ports, pPorts, pOut, tile) -> LogicResult.create().put(OUTPUT, 
		ports.get(RESET) ? false :
			isRisingEdge(INPUT, ports, pPorts) ? !pOut.get(OUTPUT) : pOut.get(OUTPUT))));
	/** Turns TRUE when it receives an input, until it receives a reset input */
	public static final Supplier<LogicGate> RSNOR	= register("rs_nor", s -> new LogicGate(s, (ports, pPorts, pOut, tile) -> LogicResult.create().put(OUTPUT,  
		ports.get(RESET) ? false :
			ports.get(INPUT) || pOut.get(OUTPUT))));
	
	/** Combines the values of two 1-bit input values */
	public static final Supplier<LogicGate> ADDER_2B = register("2bit_adder", s -> new LogicGate(s, (ports, pPorts, pOut, tile) -> 
	{
		boolean bit0 = ports.get("bit_a");
		boolean bit1 = ports.get("bit_b");
		return LogicResult.create()
			.put(OUTPUT, bit0 != bit1)
			.put("carry", bit0 && bit1);
	}));
	
	/** Emits a noise from the logic block when triggered */
	public static final Supplier<LogicGate> BEEP	= register("beep", s -> new LogicGate(s, (ports, pPorts, pOut, tile) -> 
	{
		if(isRisingEdge(INPUT, ports, pPorts))
		{
			ServerWorld world = (ServerWorld)tile.getWorld();
			world.playSound(null, tile.getPos(), SoundEvents.BLOCK_STONE_BUTTON_CLICK_ON, SoundCategory.BLOCKS);
		}
		return LogicResult.create();
	}));
	
	public static boolean isRisingEdge(String port, WireState ports, WireState pPorts)
	{
		return !pPorts.get(port) && ports.get(port);
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
		
		public final LogicResult getResult(WireState portState, WireState portCache, LogicResult prevResult, ModularLogicBlockEntity tile)
		{
			return logic.result(portState, portCache, prevResult, tile);
		}
	}
	
	@FunctionalInterface
	public interface GateLogic
	{
		/**
		 * @param portState	The state of all ports on the module
		 * @param portCache		The state of all ports on the module in the previous update
		 * @param prevResult	The result of this gate in the previous update
		 * @return
		 */
		public LogicResult result(WireState portState, WireState portCache, LogicResult prevResult, ModularLogicBlockEntity tile);
	}
}

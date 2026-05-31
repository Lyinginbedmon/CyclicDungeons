package com.lying.init;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import com.google.common.collect.Lists;
import com.lying.block.ModularLogicBlock;
import com.lying.block.Port;
import com.lying.block.entity.ModularLogicBlockEntity;
import com.lying.block.entity.logic.LogicModule;
import com.lying.block.entity.logic.LogicResult;
import com.lying.block.entity.logic.PortState;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.random.Random;

public class CDLogicGates
{
	private static final Map<String, Supplier<LogicGate>> MODULES	= new HashMap<>();
	
	public static final Port INPUT	= Port.of("input");
	public static final Port OUTPUT	= Port.of("output");
	public static final Port SET	= Port.of("set");
	public static final Port RESET	= Port.of("reset");
	public static final Port CARRY	= Port.of("carry");
	public static final Port BIT_1	= Port.of("bit1");
	public static final Port BIT_2	= Port.of("bit2");
	public static final Port BIT_4	= Port.of("bit4");
	public static final Port BIT_8	= Port.of("bit8");
	
	/** Always TRUE */
	public static final Supplier<LogicGate> TRUE	= register("true", s -> new LogicGate(s, (ports, pPorts, pOut, tile, module) -> LogicResult.create().put(OUTPUT, true)));
	
	/** Always FALSE */
	public static final Supplier<LogicGate> FALSE	= register("false", s -> new LogicGate(s, (ports, pPorts, pOut, tile, module) -> LogicResult.create().put(OUTPUT, false)));
	
	/**  TRUE only if all input values are uniformly TRUE */
	public static final Supplier<LogicGate> AND		= register("and", s -> new LogicGate(s, (ports, pPorts, pOut, tile, module) -> LogicResult.create().put(OUTPUT, ports.values().stream().allMatch(v -> v))));
	/** TRUE only if all input values are not uniformly TRUE */
	public static final Supplier<LogicGate> NAND	= register("nand", s -> new LogicGate(s, (ports, pPorts, pOut, tile, module) -> LogicResult.create().put(OUTPUT, !ports.values().stream().allMatch(v -> v))));
	/** TRUE if any input value is TRUE */
	public static final Supplier<LogicGate> OR		= register("or", s -> new LogicGate(s, (ports, pPorts, pOut, tile, module) -> LogicResult.create().put(OUTPUT, ports.values().stream().anyMatch(v -> v))));
	/** TRUE while all input values are FALSE, functionally a multi-input NOT */
	public static final Supplier<LogicGate> NOR		= register("nor", s -> new LogicGate(s, (ports, pPorts, pOut, tile, module) -> LogicResult.create().put(OUTPUT, ports.values().stream().noneMatch(v -> v))));
	/** TRUE if only one input value is TRUE */
	public static final Supplier<LogicGate> XOR		= register("xor", s -> new LogicGate(s, (ports, pPorts, pOut, tile, module) -> 
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
	public static final Supplier<LogicGate> XNOR	= register("xnor", s -> new LogicGate(s, (ports, pPorts, pOut, tile, module) -> 
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
	
	/** TRUE if the input became TRUE in this frame */
	public static final Supplier<LogicGate> EDGE_R	= register("rising_edge", s -> new LogicGate(s, (ports, pPorts, pOut, tile, module) -> LogicResult.create().put(OUTPUT, isRisingEdge(INPUT, ports, pPorts))));
	/** TRUE if the input became FALSE in this frame */
	public static final Supplier<LogicGate> EDGE_F	= register("falling_edge", s -> new LogicGate(s, (ports, pPorts, pOut, tile, module) -> LogicResult.create().put(OUTPUT, pPorts.get(INPUT) && !ports.get(INPUT))));
	/** TRUE if the input changed in this frame */
	public static final Supplier<LogicGate> DELTA	= register("delta", s -> new LogicGate(s, (ports, pPorts, pOut, tile, module) -> LogicResult.create().put(OUTPUT, ports.get(INPUT) != pPorts.get(INPUT))));
	
	/** Outputs a random value when triggered */
	public static final Supplier<LogicGate> RAND	= register("random", s -> new LogicGate(s, (ports, pPorts, pOut, tile, module) -> LogicResult.create().put(OUTPUT, 
		isRisingEdge(INPUT, ports, pPorts) ? 
			Random.create().nextBoolean() : 
			pOut.get(OUTPUT))));
	
	/** Toggles its output on the rising edge of its input */
	public static final Supplier<LogicGate> TOGGLE	= register("toggle", s -> new LogicGate(s, (ports, pPorts, pOut, tile, module) -> LogicResult.create().put(OUTPUT, 
		ports.get(RESET) ? false :
			isRisingEdge(INPUT, ports, pPorts) ? !pOut.get(OUTPUT) : pOut.get(OUTPUT))));
	/** Turns TRUE when it receives an input, until it receives a reset input */
	public static final Supplier<LogicGate> RSNOR	= register("rs_nor", s -> new LogicGate(s, (ports, pPorts, pOut, tile, module) -> LogicResult.create().put(OUTPUT,  
		ports.get(RESET) ? false :
			ports.get(INPUT) || pOut.get(OUTPUT))));
	/** Stores the input value when it receives a set signal, resets when it receives a reset signal */
	public static final Supplier<LogicGate> SRLATCH	= register("sr_latch", s -> new LogicGate(s, (ports, pPorts, pOut, tile, module) -> 
		ports.get(RESET) ?
			LogicResult.create().put(OUTPUT, false) :
		ports.get(SET) ? 
			LogicResult.create().put(OUTPUT, ports.get(INPUT)) : pOut));
	
	/** Combines the values of two 1-bit input values */
	public static final Supplier<LogicGate> ADDER_2B = register("2bit_adder", s -> new LogicGate(s, (ports, pPorts, pOut, tile, module) -> 
	{
		boolean bit0 = ports.get(BIT_1);
		boolean bit1 = ports.get(BIT_2);
		return LogicResult.create()
			.put(OUTPUT, bit0 != bit1)
			.put(CARRY, bit0 && bit1);
	}));
	/** Converts a 4-bit number into a value between 0 and 15 (inclusive) */
	public static final Supplier<LogicGate> CONVERTER_4B	= register("b2d_converter", s -> new LogicGate(s, (ports, pPorts, pOut, tile, module) -> 
	{
		return LogicResult.create()
				.put(Port.of("output_"+binaryToDecimal(gatherBits(4, ports))), true);
	}));
	/** Holds a 4-bit number and increments it when it receives a rising-edge pulse */
	public static final Supplier<LogicGate> COUNTER_4B	= register("4bit_counter", s -> new LogicGate(s, (ports, pPorts, pOut, tile, module) -> 
	{
		if(isRisingEdge(RESET, ports, pPorts))
			return LogicResult.create();
		
		if(isRisingEdge(Port.of("inc"), ports, pPorts))
		{
			boolean[] data = gatherBits(4, ports);
			// Toggle values from bit 1 to 4 until we encounter a false, set that to true and break
			for(int i=0; i<data.length; i++)
			{
				if(!data[i])
				{
					data[i] = true;
					break;
				}
				else
					data[i] = false;
			}
			return LogicResult.create()
					.put(BIT_1, data[0])
					.put(BIT_2, data[1])
					.put(BIT_4, data[2])
					.put(BIT_8, data[3]);
		}
		// TODO Add decrement function?
		return pOut;
	}));
	
	/** Emits a noise from the logic block when triggered */
	public static final Supplier<LogicGate> BEEP	= register("beep", s -> new LogicGate(s, (ports, pPorts, pOut, tile, module) -> 
	{
		if(isRisingEdge(INPUT, ports, pPorts))
		{
			ServerWorld world = (ServerWorld)tile.getWorld();
			world.playSound(null, tile.getPos(), SoundEvents.BLOCK_STONE_BUTTON_CLICK_ON, SoundCategory.BLOCKS);
		}
		return LogicResult.create();
	}));
	/** Controls the light emission value of the logic block with a 4-bit value */
	public static final Supplier<LogicGate> LIGHT	= register("light", s -> new LogicGate(s, (ports, pPorts, pOut, tile, module) -> 
	{
		ServerWorld world = (ServerWorld)tile.getWorld();
		world.setBlockState(tile.getPos(), tile.getCachedState().with(ModularLogicBlock.LIGHT, binaryToDecimal(gatherBits(4, ports))));
		return LogicResult.create();
	}));
	
	/** When named, used by the modular logic system to interact with the trap system */
	public static final Supplier<LogicGate> ENTRY	= register("input", s -> new LogicGate(s, (ports, pPorts, pOut, tile, module) -> LogicResult.create().put(
			OUTPUT, 
			module.toPort().isPresent() && tile.getInput(module.toPort().get()))));
	/** When named, used by the modular logic system to interact with the trap system */
	public static final Supplier<LogicGate> EXIT	= register("output", s -> new LogicGate(s, (ports, pPorts, pOut, tile, module) -> LogicResult.create().put(
			OUTPUT, 
			ports.get(INPUT))));
	
	public static boolean isRisingEdge(Port port, PortState ports, PortState pPorts)
	{
		return !pPorts.get(port) && ports.get(port);
	}
	
	public static boolean[] gatherBits(int count, PortState ports)
	{
		boolean[] bits = new boolean[count];
		for(int i=0; i<bits.length; i++)
			bits[i] = ports.get(Port.of("bit_"+(int)Math.pow(2, i)));
		return bits;
	}
	
	public static int binaryToDecimal(boolean... bits)
	{
		int result = 0;
		for(int i=0; i<bits.length; i++)
			result += (int)Math.pow(2, i) * (bits[i] ? 1 : 0);
		return result;
	}
	
	public static boolean[] decimalToBinary(int val)
	{
		if(val <= 1)
			return new boolean[] { val == 1 };
		int bits = (int)Math.floor(Math.sqrt(val)) + 1;
		boolean[] result = new boolean[bits];
		for(int i=bits; i>=0; i--)
		{
			int d = (int)Math.pow(2, i);
			
			if(val <= 0)
				result[i-1] = false;
			else
			{
				result[i - 1] = val >= d;
				val -= d;
			}
		}
		return result;
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
		
		public final LogicResult getResult(PortState portState, PortState portCache, LogicResult prevResult, ModularLogicBlockEntity tile, LogicModule module)
		{
			return logic.result(portState, portCache, prevResult, tile, module);
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
		public LogicResult result(PortState portState, PortState portCache, LogicResult prevResult, ModularLogicBlockEntity tile, LogicModule module);
	}
}

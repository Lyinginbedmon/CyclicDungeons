package com.lying.init;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import com.google.common.collect.Lists;
import com.ibm.icu.text.Collator;
import com.lying.block.ModularLogicBlock;
import com.lying.block.Port;
import com.lying.block.entity.ModularLogicBlockEntity;
import com.lying.block.entity.logic.LogicModule;
import com.lying.block.entity.logic.LogicResult;
import com.lying.block.entity.logic.PortState;
import com.lying.reference.Reference;
import com.lying.utility.CDUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.random.Random;

public class CDLogicGates
{
	private static final Map<String, Supplier<LogicGate>> MODULES	= new HashMap<>();
	private static final Map<LogicCategory, List<String>> BY_CATEGORY	= new HashMap<>();
	
	// FIXME Implement in-game circuit builder screen
	
	public static final Port INPUT	= Port.of("input");
	public static final Port OUTPUT	= Port.of("output");
	public static final Port SET	= Port.of("set");
	public static final Port RESET	= Port.of("reset");
	public static final Port INC	= Port.of("inc");
	public static final Port CARRY	= Port.of("carry");
	public static final Port BIT_1	= Port.of("bit1");
	public static final Port BIT_2	= Port.of("bit2");
	public static final Port BIT_4	= Port.of("bit4");
	public static final Port BIT_8	= Port.of("bit8");
	
	/** Always TRUE */
	public static final Supplier<LogicGate> TRUE	= register("true", s -> LogicGate.Builder
			.create((ports, pPorts, pOut, tile, module) -> LogicResult.create().put(OUTPUT, true))
			.category(LogicCategory.IO)
			.addOutput(OUTPUT)
			.build(s));
	
	/** Always FALSE */
	public static final Supplier<LogicGate> FALSE	= register("false", s -> LogicGate.Builder
			.create((ports, pPorts, pOut, tile, module) -> LogicResult.create().put(OUTPUT, false))
			.category(LogicCategory.IO)
			.addOutput(OUTPUT)
			.build(s));
	
	/**  TRUE only if all input values are uniformly TRUE */
	public static final Supplier<LogicGate> AND		= register("and", s -> LogicGate.Builder
			.create((ports, pPorts, pOut, tile, module) -> LogicResult.create().put(OUTPUT, ports.values().stream().allMatch(v -> v)))
			.category(LogicCategory.BASIC)
			.addInputCollector(CDLogicGates::freeInputs)
			.addOutput(OUTPUT)
			.build(s));
	/** TRUE only if all input values are not uniformly TRUE */
	public static final Supplier<LogicGate> NAND	= register("nand", s -> LogicGate.Builder
			.create((ports, pPorts, pOut, tile, module) -> LogicResult.create().put(OUTPUT, !ports.values().stream().allMatch(v -> v)))
			.category(LogicCategory.BASIC)
			.addInputCollector(CDLogicGates::freeInputs)
			.addOutput(OUTPUT)
			.build(s));
	/** TRUE if any input value is TRUE */
	public static final Supplier<LogicGate> OR		= register("or", s -> LogicGate.Builder
			.create((ports, pPorts, pOut, tile, module) -> LogicResult.create().put(OUTPUT, ports.values().stream().anyMatch(v -> v)))
			.category(LogicCategory.BASIC)
			.addInputCollector(CDLogicGates::freeInputs)
			.addOutput(OUTPUT)
			.build(s));
	/** TRUE while all input values are FALSE, functionally a multi-input NOT */
	public static final Supplier<LogicGate> NOR		= register("nor", s -> LogicGate.Builder
			.create((ports, pPorts, pOut, tile, module) -> LogicResult.create().put(OUTPUT, ports.values().stream().noneMatch(v -> v)))
			.category(LogicCategory.BASIC)
			.addInputCollector(CDLogicGates::freeInputs)
			.addOutput(OUTPUT)
			.build(s));
	/** TRUE if only one input value is TRUE */
	public static final Supplier<LogicGate> XOR		= register("xor", s -> LogicGate.Builder
			.create((ports, pPorts, pOut, tile, module) -> 
			{
				boolean result = false;
				for(Boolean var : ports.values())
					if(var)
						if(!result)
							result = true;
						else
							return LogicResult.create().put(OUTPUT, false);
				return LogicResult.create().put(OUTPUT, result);
			})
			.category(LogicCategory.BASIC)
			.addInputCollector(CDLogicGates::freeInputs)
			.addOutput(OUTPUT)
			.build(s));
	/** TRUE if all input values share the same value */
	public static final Supplier<LogicGate> XNOR	= register("xnor", s -> LogicGate.Builder
			.create((ports, pPorts, pOut, tile, module) -> 
			{
				List<Boolean> lines = Lists.newArrayList(ports.values());
				if(lines.isEmpty())
					return LogicResult.create().put(OUTPUT, true);
				
				final boolean goal = lines.removeFirst();
				while(!lines.isEmpty())
					if(lines.removeFirst() != goal)
						return LogicResult.create().put(OUTPUT, false);
				return LogicResult.create().put(OUTPUT, true);
			})
			.category(LogicCategory.BASIC)
			.addInputCollector(CDLogicGates::freeInputs)
			.addOutput(OUTPUT)
			.build(s));
	
	/** TRUE if the input became TRUE in this frame */
	public static final Supplier<LogicGate> EDGE_R	= register("rising_edge", s -> LogicGate.Builder
			.create((ports, pPorts, pOut, tile, module) -> LogicResult.create().put(OUTPUT, isRisingEdge(INPUT, ports, pPorts)))
			.category(LogicCategory.UTILITY)
			.addInput(INPUT).addOutput(OUTPUT)
			.build(s));
	/** TRUE if the input became FALSE in this frame */
	public static final Supplier<LogicGate> EDGE_F	= register("falling_edge", s -> LogicGate.Builder
			.create((ports, pPorts, pOut, tile, module) -> LogicResult.create().put(OUTPUT, pPorts.get(INPUT) && !ports.get(INPUT)))
			.category(LogicCategory.UTILITY)
			.addInput(INPUT).addOutput(OUTPUT)
			.build(s));
	/** TRUE if the input changed in this frame */
	public static final Supplier<LogicGate> DELTA	= register("delta", s -> LogicGate.Builder
			.create((ports, pPorts, pOut, tile, module) -> LogicResult.create().put(OUTPUT, ports.get(INPUT) != pPorts.get(INPUT)))
			.category(LogicCategory.UTILITY)
			.addInput(INPUT).addOutput(OUTPUT)
			.build(s));
	
	/** Outputs a random value when triggered */
	public static final Supplier<LogicGate> RAND	= register("random", s -> LogicGate.Builder
			.create((ports, pPorts, pOut, tile, module) -> LogicResult.create().put(OUTPUT, 
				isRisingEdge(INPUT, ports, pPorts) ? 
					Random.create().nextBoolean() : 
					pOut.get(OUTPUT)))
			.category(LogicCategory.MATH)
			.addInput(INPUT).addOutput(OUTPUT)
			.build(s));
	
	/** Toggles its output on the rising edge of its input */
	public static final Supplier<LogicGate> TOGGLE	= register("toggle", s -> LogicGate.Builder
			.create((ports, pPorts, pOut, tile, module) -> LogicResult.create().put(OUTPUT, 
				ports.get(RESET) ? false :
					isRisingEdge(INPUT, ports, pPorts) ? !pOut.get(OUTPUT) : pOut.get(OUTPUT)))
			.category(LogicCategory.MEMORY)
			.addInput(INPUT, RESET).addOutput(OUTPUT)
			.build(s));
	/** Turns TRUE when it receives an input, until it receives a reset input */
	public static final Supplier<LogicGate> RSNOR	= register("rs_nor", s -> LogicGate.Builder
			.create((ports, pPorts, pOut, tile, module) -> LogicResult.create().put(OUTPUT,  
				ports.get(RESET) ? false :
					ports.get(INPUT) || pOut.get(OUTPUT)))
			.category(LogicCategory.MEMORY)
			.addInput(INPUT, RESET).addOutput(OUTPUT)
			.build(s));
	/** Stores the input value when it receives a set signal, resets when it receives a reset signal */
	public static final Supplier<LogicGate> SRLATCH	= register("sr_latch", s -> LogicGate.Builder
			.create((ports, pPorts, pOut, tile, module) -> 
				ports.get(RESET) ?
					LogicResult.create().put(OUTPUT, false) :
				ports.get(SET) ? 
					LogicResult.create().put(OUTPUT, ports.get(INPUT)) : pOut)
			.category(LogicCategory.MEMORY)
			.addInput(INPUT, RESET, SET).addOutput(OUTPUT)
			.build(s));
	
	/** Combines the values of two 1-bit input values */
	public static final Supplier<LogicGate> ADDER_2B = register("2bit_adder", s -> LogicGate.Builder
			.create((ports, pPorts, pOut, tile, module) -> 
			{
				boolean bit0 = ports.get(BIT_1);
				boolean bit1 = ports.get(BIT_2);
				return LogicResult.create()
					.put(OUTPUT, bit0 != bit1)
					.put(CARRY, bit0 && bit1);
			})
			.category(LogicCategory.MATH)
			.addInput(BIT_1, BIT_2)
			.addOutput(OUTPUT, CARRY)
			.build(s));
	/** Converts a 4-bit number into an output value between 0 and 15 (inclusive) */
	public static final Supplier<LogicGate> CONVERTER_4B	= register("b2d_converter", s -> LogicGate.Builder
			.create((ports, pPorts, pOut, tile, module) -> 
			{
				return LogicResult.create()
						.put(Port.of("output_"+CDUtils.binaryToDecimal(gatherBits(4, ports))), true);
			})
			.category(LogicCategory.MATH)
			.addInput(BIT_1, BIT_2, BIT_4, BIT_8)
			.addOutputCollector(m -> 
			{
				List<Port> ports = Lists.newArrayList();
				for(int i=0; i<16; i++) ports.add(Port.of("output_"+i));
				return ports;
			})
			.build(s));
	public static final Supplier<LogicGate> CONVERTER_D	= register("d2b_converter", s -> LogicGate.Builder
			.create((ports, pPorts, pOut, tile, module) -> 
			{
				// Identify highest active input port
				int val = 0;
				for(int i=15; i>=0; i--)
					if(ports.get(Port.of("input_"+i)))
					{
						val = i;
						break;
					}
				
				// Convert value to 4bit binary
				final boolean[] result = CDUtils.decimalToBinary(val);
				return LogicResult.create()
						.put(BIT_1, result[0])
						.put(BIT_2, result[1])
						.put(BIT_4, result[2])
						.put(BIT_8, result[3]);
			})
			.category(LogicCategory.MATH)
			.addInputCollector(m -> 
			{
				List<Port> ports = Lists.newArrayList();
				for(int i=0; i<16; i++) ports.add(Port.of("input_"+i));
				return ports;
			})
			.addOutput(BIT_1, BIT_2, BIT_4, BIT_8)
			.build(s));
	/** Holds a 4-bit number and increments it when it receives a rising-edge pulse */
	public static final Supplier<LogicGate> COUNTER_4B	= register("4bit_counter", s -> LogicGate.Builder
			.create((ports, pPorts, pOut, tile, module) -> 
			{
				if(isRisingEdge(RESET, ports, pPorts))
					return LogicResult.create();
				
				if(isRisingEdge(INC, ports, pPorts))
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
			})
			.category(LogicCategory.MATH)
			.addInput(INC, RESET).addOutput(BIT_1, BIT_2, BIT_4, BIT_8)
			.build(s));
	
	/** Emits a noise from the logic block when triggered */
	public static final Supplier<LogicGate> BEEP	= register("beep", s -> LogicGate.Builder
			.create((ports, pPorts, pOut, tile, module) -> 
			{
				if(isRisingEdge(INPUT, ports, pPorts))
				{
					ServerWorld world = (ServerWorld)tile.getWorld();
					world.playSound(null, tile.getPos(), SoundEvents.BLOCK_STONE_BUTTON_CLICK_ON, SoundCategory.BLOCKS);
				}
				return LogicResult.create();
			})
			.category(LogicCategory.UTILITY)
			.addInput(INPUT)
			.build(s));
	/** Controls the light emission value of the logic block with a 4-bit value */
	public static final Supplier<LogicGate> LIGHT	= register("light", s -> LogicGate.Builder
			.create((ports, pPorts, pOut, tile, module) -> 
			{
				ServerWorld world = (ServerWorld)tile.getWorld();
				world.setBlockState(tile.getPos(), tile.getCachedState().with(ModularLogicBlock.LIGHT, CDUtils.binaryToDecimal(gatherBits(4, ports))));
				return LogicResult.create();
			})
			.category(LogicCategory.UTILITY)
			.addInput(BIT_1, BIT_2, BIT_4, BIT_8)
			.build(s));
	
	/** When named, used by the modular logic system to interact with the trap system */
	public static final Supplier<LogicGate> ENTRY	= register("input", s -> LogicGate.Builder
			.create((ports, pPorts, pOut, tile, module) -> LogicResult.create().put(
				OUTPUT, 
				module.toPort().isPresent() && tile.getInput(module.toPort().get())))
			.category(LogicCategory.IO)
			.addOutput(OUTPUT)
			.build(s));
	/** When named, used by the modular logic system to interact with the trap system */
	public static final Supplier<LogicGate> EXIT	= register("output", s -> LogicGate.Builder
			.create((ports, pPorts, pOut, tile, module) -> LogicResult.create().put(
				OUTPUT, 
				ports.get(INPUT)))
			.category(LogicCategory.IO)
			.addInput(INPUT).addOutput(OUTPUT)
			.build(s));
	
	protected static boolean isRisingEdge(Port port, PortState ports, PortState pPorts)
	{
		return !pPorts.get(port) && ports.get(port);
	}
	
	protected static boolean[] gatherBits(int count, PortState ports)
	{
		boolean[] bits = new boolean[count];
		for(int i=0; i<bits.length; i++)
			bits[i] = ports.get(Port.of("bit_"+(int)Math.pow(2, i)));
		return bits;
	}
	
	/** Returns a list of ports one greater than the inport ports currently in use by the module */
	protected static List<Port> freeInputs(LogicModule module)
	{
		List<Port> ports = Lists.newArrayList();
		module.collectInputs().orElse(List.of()).stream().map(Port::of).forEach(ports::add);
		ports.add(Port.of("input_"+ports.size()));
		return ports;
	}
	
	/** Returns a list of ports one greater than the output ports currently in use by the module */
	protected static List<Port> freeOutputs(LogicModule module)
	{
		List<Port> ports = Lists.newArrayList();
		module.collectOutputs().orElse(List.of()).stream().map(Port::of).forEach(ports::add);
		ports.add(Port.of("input_"+ports.size()));
		return ports;
	}
	
	private static Supplier<LogicGate> register(String nameIn, Function<String, LogicGate> factory)
	{
		final Supplier<LogicGate> supplier = () -> factory.apply(nameIn);
		MODULES.put(nameIn, supplier);
		
		LogicGate gate = supplier.get();
		List<String> set = BY_CATEGORY.getOrDefault(gate.category(), Lists.newArrayList());
		set.add(nameIn);
		BY_CATEGORY.put(gate.category(), set);
		
		return supplier;
	}
	
	/** Returns a name-sorted list of logic gates in the given category */
	public static List<LogicGate> byCategory(LogicCategory category)
	{
		return BY_CATEGORY.getOrDefault(category, List.of()).stream()
				.map(MODULES::get)
				.map(Supplier::get)
				.sorted(LogicGate.SORT)
				.toList();
	}
	
	/** Returns a list of all registered logic gates */
	public static List<LogicGate> getAllGates() { return MODULES.values().stream().map(Supplier::get).toList(); }
	
	public static abstract class LogicGate
	{
		public static final Codec<LogicGate> CODEC	= Codec.STRING.comapFlatMap(s -> 
		{
			LogicGate gate = CDLogicGates.MODULES.getOrDefault(s,() -> null).get();
			return gate == null ? DataResult.error(() -> "Logic gate name not recognised") : DataResult.success(gate);
		}, LogicGate::registryName);
		public static final Comparator<LogicGate> SORT	= (a,b) -> Collator.getInstance().compare(a.registryName.toLowerCase(), b.registryName.toLowerCase());
		
		private final String registryName;
		private final GateLogic logic;
		private final LogicCategory category;
		
		protected LogicGate(String nameIn, GateLogic logicIn, LogicCategory categoryIn)
		{
			registryName = nameIn;
			logic = logicIn;
			category = categoryIn;
		}
		
		public final String registryName() { return registryName; }
		
		public Text displayName() { return Reference.ModInfo.translate("logic", registryName); }
		
		public final LogicCategory category() { return category; }
		
		public final LogicResult getResult(PortState portState, PortState portCache, LogicResult prevResult, ModularLogicBlockEntity tile, LogicModule module)
		{
			return logic.result(portState, portCache, prevResult, tile, module);
		}
		
		public abstract List<Port> inputPorts(LogicModule module);
		
		public abstract List<Port> outputPorts(LogicModule module);
		
		public LogicModule create() { return LogicModule.of(this); }
		
		public static class Builder
		{
			private final GateLogic logic;
			private LogicCategory category = LogicCategory.BASIC;
			private List<Port> inputs = Lists.newArrayList(), outputs = Lists.newArrayList();
			private Optional<Function<LogicModule,List<Port>>> inputCollector = Optional.empty();
			private Optional<Function<LogicModule,List<Port>>> outputCollector = Optional.empty();
			
			protected Builder(GateLogic logicIn)
			{
				logic = logicIn;
			}
			
			public static Builder create(GateLogic logicIn)
			{
				return new Builder(logicIn);
			}
			
			public Builder category(LogicCategory cat)
			{
				category = cat;
				return this;
			}
			
			public Builder addInput(Port... ports)
			{
				for(Port port : ports)
					if(!inputs.contains(port))
						inputs.add(port);
				return this;
			}
			
			public Builder addInputCollector(Function<LogicModule,List<Port>> collector)
			{
				inputCollector = Optional.of(collector);
				return this;
			}
			
			public Builder addOutput(Port... ports)
			{
				for(Port port : ports)
					if(!outputs.contains(port))
						outputs.add(port);
				return this;
			}
			
			public Builder addOutputCollector(Function<LogicModule,List<Port>> collector)
			{
				outputCollector = Optional.of(collector);
				return this;
			}
			
			public LogicGate build(String registryName)
			{
				return new LogicGate(registryName, logic, category)
						{
							public List<Port> inputPorts(LogicModule module) { return inputCollector.isPresent() ? inputCollector.get().apply(module) : inputs; }
							public List<Port> outputPorts(LogicModule module) { return outputCollector.isPresent() ? outputCollector.get().apply(module) : outputs; }
						};
			}
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
	
	public static enum LogicCategory
	{
		BASIC,
		IO,
		MATH,
		MEMORY,
		UTILITY;
	}
}

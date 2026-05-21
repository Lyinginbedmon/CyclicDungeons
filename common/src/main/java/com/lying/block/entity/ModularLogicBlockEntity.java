package com.lying.block.entity;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import org.slf4j.Logger;

import com.google.common.collect.Lists;
import com.lying.CyclicDungeons;
import com.lying.block.IWireableBlock;
import com.lying.block.entity.logic.WireSet;
import com.lying.block.entity.logic.WireState;
import com.lying.init.CDBlockEntityTypes;
import com.lying.init.CDLogicGates;
import com.lying.init.CDLogicGates.LogicGate;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtOps;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class ModularLogicBlockEntity extends BlockEntity
{
	public static final Logger LOGGER	= CyclicDungeons.LOGGER;
	private static final List<LogicModule> TEST_LOGIC	= Lists.newArrayList(
		LogicModule.of(CDLogicGates.TRUE.get())
			.name("input_a")
			.addOutput("a"),
		LogicModule.of(CDLogicGates.FALSE.get())
			.name("input_b")
			.addOutput("not_1"),
		LogicModule.of(CDLogicGates.AND.get())
			.name("result_gate")
			.addInput(CDLogicGates.INPUT, "a", "b", "c"),
		LogicModule.of(CDLogicGates.NOR.get())
			.addInput(CDLogicGates.INPUT, "not_1")
			.addOutput("b"),
		LogicModule.of(CDLogicGates.NOR.get())
			.addInput(CDLogicGates.INPUT, "not_2")
			.addOutput("c"),
		LogicModule.of(CDLogicGates.FALSE.get())
			.name("input_c")
			.addOutput("not_2")
		);
	
	private List<LogicModule> modules = Lists.newArrayList(TEST_LOGIC);
	
	public ModularLogicBlockEntity(BlockPos pos, BlockState state)
	{
		super(CDBlockEntityTypes.MODULAR_LOGIC.get(), pos, state);
	}
	
	protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup)
	{
		super.writeNbt(nbt, registryLookup);
		if(!modules.isEmpty())
			nbt.put("circuit", LogicModule.LIST_CODEC.encodeStart(NbtOps.INSTANCE, modules).getOrThrow());
	}
	
	protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup)
	{
		super.readNbt(nbt, registryLookup);
		modules.clear();
		if(nbt.contains("circuit"))
			modules.addAll(LogicModule.LIST_CODEC.parse(NbtOps.INSTANCE, nbt.get("circuit")).getOrThrow());
	}
	
	public static <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type)
	{
		return type != CDBlockEntityTypes.MODULAR_LOGIC.get() ? 
				null : 
				IWireableBlock.validateTicker(type, CDBlockEntityTypes.MODULAR_LOGIC.get(), 
					world.isClient() ? 
						ModularLogicBlockEntity::tickClient : 
						ModularLogicBlockEntity::tickServer);
	}
	
	public static <T extends BlockEntity> void tickClient(World world, BlockPos pos, BlockState state, ModularLogicBlockEntity tile) { }
	
	public static <T extends BlockEntity> void tickServer(World world, BlockPos pos, BlockState state, ModularLogicBlockEntity tile)
	{
		tile.processLogic();
	}
	
	public boolean processLogic()
	{
		// Account for all input/output wires in this circuit
		final WireState wireMap = new WireState();
		final Consumer<String> wireRegistry = w -> wireMap.put(w, false);
		modules.forEach(m -> m.registerWires(wireRegistry));
		
		// Iteratively process all modules according to their inputs (if any) until all are accounted
		List<String> calculatedWires = Lists.newArrayList();
		List<LogicModule> calculateStack = Lists.newArrayList(modules);
		while(!calculateStack.isEmpty())
		{
			// Cache the modules that can be calculated in this iteration step
			// This prevents modules being calculated before others that might affect their own wires
			List<LogicModule> calculated = calculateStack.stream()
					.filter(m -> calculatedWires.containsAll(m.prerequisites()))
					.toList();
			for(LogicModule module : calculated)
			{
				module.process(wireMap);
				if(!module.outputWires.isEmpty())
					calculatedWires.addAll(module.outputWires.wires());
			}
			calculateStack.removeAll(calculated);
		}
		
		return false;
	}
	
	public static class LogicModule
	{
		public static final Codec<LogicModule> CODEC	= RecordCodecBuilder.create(instance -> instance.group(
				Codec.STRING.optionalFieldOf("name").forGetter(m -> m.name),
				LogicGate.CODEC.fieldOf("logic").forGetter(m -> m.handler),
				WireSet.CODEC.optionalFieldOf("input_wires").forGetter(m -> m.inputWires.isEmpty() ? Optional.empty() : Optional.of(m.inputWires)),
				WireSet.CODEC.optionalFieldOf("output_wires").forGetter(m -> m.outputWires.isEmpty() ? Optional.empty() : Optional.of(m.outputWires)),
				WireState.CODEC.optionalFieldOf("prev_inputs").forGetter(m -> m.prevWires.isInert() ? Optional.of(m.prevWires) : Optional.empty()),
				Codec.BOOL.optionalFieldOf("prev_output").forGetter(m -> m.prevResult ? Optional.of(true) : Optional.empty())
				).apply(instance, (name,handler,inputs,outputs,prevWires,prevOut) -> 
				{
					LogicModule module = new LogicModule(handler);
					name.ifPresent(module::name);
					inputs.ifPresent(module::addInput);
					outputs.ifPresent(module::addOutput);
					prevWires.ifPresent(module.prevWires::copy);
					module.prevResult = prevOut.orElse(false);
					return module;
				}));
		public static final Codec<List<LogicModule>> LIST_CODEC	= CODEC.listOf();
		
		private Optional<String> name = Optional.empty();
		private final LogicGate handler;
		private final WireSet inputWires = new WireSet();
		private final WireSet outputWires = new WireSet();
		
		// State of input wires in previous frame
		private WireState prevWires = new WireState();
		// Module output in previous frame
		private boolean prevResult = false;
		
		protected LogicModule(LogicGate handlerIn)
		{
			handler = handlerIn;
		}
		
		public static LogicModule of(LogicGate handlerIn)
		{
			return new LogicModule(handlerIn);
		}
		
		public LogicModule name(String nameIn)
		{
			name = Optional.of(nameIn);
			return this;
		}
		
		public String displayName() { return name.orElse(handler.registryName()); }
		
		/** Attaches a wire to the default result port of this module */
		public LogicModule addOutput(String wire)
		{
			return addOutput(CDLogicGates.OUTPUT, wire);
		}
		
		public LogicModule addOutput(WireSet wires)
		{
			for(String port : wires.ports())
				addOutput(port, wires.get(port).toArray(new String[0]));
			return this;
		}
		
		public LogicModule addOutput(String input, String... wires)
		{
			outputWires.put(input, wires);
			return this;
		}
		
		public LogicModule addInput(WireSet wires)
		{
			for(String port : wires.ports())
				addInput(port, wires.get(port).toArray(new String[0]));
			return this;
		}
		
		public LogicModule addInput(String input, String... wires)
		{
			inputWires.put(input, wires);
			return this;
		}
		
		protected Optional<List<String>> collectInputs()
		{
			List<String> set = inputWires.wires();
			return set.isEmpty() ? Optional.empty() : Optional.of(set);
		}
		
		/** Adds all wires used by this module to the wire map */
		public void registerWires(Consumer<String> register)
		{
			outputWires.wires().forEach(register::accept);
			inputWires.wires().forEach(register::accept);
		}
		
		/** Returns a list of all input wires that must be calculated before this module */
		public List<String> prerequisites()
		{
			List<String> wires = Lists.newArrayList();
			inputWires.wires().forEach(wires::add);
			return wires.stream().distinct().toList();
		}
		
		/** Returns the result of this module, updating its output wire if necessary */
		public boolean process(WireState wireStates)
		{
			boolean result = handler.getResult(inputWires, wireStates, prevWires, prevResult);
			
			// Update record of input state
			prevWires.clear();
			for(String wire : inputWires.wires())
				prevWires.put(wire, wireStates.get(wire));
			
			// Update output wire(s)
			if(!outputWires.isEmpty() && result)
				for(String wire : outputWires.get(CDLogicGates.OUTPUT))
					wireStates.put(wire, true);
			
			prevResult = result;
			return result;
		}
	}
	
}

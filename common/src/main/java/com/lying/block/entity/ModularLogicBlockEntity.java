package com.lying.block.entity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import org.slf4j.Logger;

import com.google.common.collect.Lists;
import com.lying.CyclicDungeons;
import com.lying.block.IWireableBlock;
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
			.setOutput("a"),
		LogicModule.of(CDLogicGates.FALSE.get())
			.name("input_b")
			.setOutput("not_1"),
		LogicModule.of(CDLogicGates.AND.get())
			.name("result_gate")
			.addInput(CDLogicGates.INPUT, "a", "b", "c"),
		LogicModule.of(CDLogicGates.NOT.get())
			.addInput(CDLogicGates.INPUT, "not_1")
			.setOutput("b"),
		LogicModule.of(CDLogicGates.NOT.get())
			.addInput(CDLogicGates.INPUT, "not_2")
			.setOutput("c"),
		LogicModule.of(CDLogicGates.FALSE.get())
			.name("input_c")
			.setOutput("not_2")
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
		final Map<String, Boolean> wireMap = new HashMap<>();
		final Consumer<String> wireRegistry = w -> wireMap.put(w, false);
		modules.forEach(m -> m.registerWires(wireRegistry));
		
		// Iteratively process all modules according to their inputs (if any) until all are accounted
		List<String> calculatedWires = Lists.newArrayList();
		List<LogicModule> calculateStack = Lists.newArrayList(modules);
		while(!calculateStack.isEmpty())
		{
			List<LogicModule> calculated = calculateStack.stream()
					.filter(m -> calculatedWires.containsAll(m.prerequisites()))
					.toList();
			for(LogicModule module : calculated)
			{
				module.process(wireMap);
				module.outputWire.ifPresent(calculatedWires::add);
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
				InputList.CODEC.listOf().optionalFieldOf("input").forGetter(LogicModule::collectInputs),
				Codec.STRING.optionalFieldOf("output").forGetter(m -> m.outputWire)
				).apply(instance, (n,h,i,o) -> 
				{
					LogicModule module = new LogicModule(h);
					n.ifPresent(module::name);
					o.ifPresent(module::setOutput);
					return module;
				}));
		public static final Codec<List<LogicModule>> LIST_CODEC	= CODEC.listOf();
		
		private Optional<String> name = Optional.empty();
		private final LogicGate handler;
		private final Map<String, List<String>> inputSet = new HashMap<>();
		private Optional<String> outputWire = Optional.empty();
		
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
		
		public LogicModule setOutput(String wire)
		{
			outputWire = Optional.of(wire);
			return this;
		}
		
		public LogicModule addInput(String input, String... wires)
		{
			List<String> set = inputSet.getOrDefault(input, Lists.newArrayList());
			for(String wire : wires)
				if(!set.contains(wire))
					set.add(wire);
			inputSet.put(input, set);
			return this;
		}
		
		protected Optional<List<InputList>> collectInputs()
		{
			List<InputList> set = inputSet.entrySet().stream().map(e -> new InputList(e.getKey(), e.getValue())).toList();
			return set.isEmpty() ? Optional.empty() : Optional.of(set);
		}
		
		/** Adds all wires used by this module to the wire map */
		public void registerWires(Consumer<String> register)
		{
			outputWire.ifPresent(register::accept);
			inputSet.values().forEach(l -> l.forEach(register::accept));
		}
		
		/** Returns a list of all input wires that must be calculated before this module */
		public List<String> prerequisites()
		{
			List<String> wires = Lists.newArrayList();
			inputSet.values().forEach(wires::addAll);
			return wires.stream().distinct().toList();
		}
		
		/** Returns the result of this module, updating its output wire if necessary */
		public boolean process(Map<String, Boolean> wireStates)
		{
			boolean result = handler.getOutput(inputSet, wireStates);
			
			if(outputWire.isPresent() && result)
				wireStates.put(outputWire.get(), true);
			
			return result;
		}
		
		private static record InputList(String name, List<String> values)
		{
			public static final Codec<InputList> CODEC	= RecordCodecBuilder.create(instance -> instance.group(
					Codec.STRING.fieldOf("var").forGetter(InputList::name), 
					Codec.STRING.listOf().fieldOf("wires").forGetter(InputList::values)
					).apply(instance, InputList::new));
		}
	}
	
}

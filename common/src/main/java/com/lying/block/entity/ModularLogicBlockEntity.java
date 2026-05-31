package com.lying.block.entity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;

import org.slf4j.Logger;

import com.google.common.collect.Lists;
import com.lying.CyclicDungeons;
import com.lying.block.IWireableBlock;
import com.lying.block.Port;
import com.lying.block.entity.logic.LogicModule;
import com.lying.block.entity.logic.LogicWire;
import com.lying.block.entity.logic.PortState;
import com.lying.init.CDBlockEntityTypes;
import com.lying.init.CDLogicGates;
import com.lying.reference.Reference;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtOps;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class ModularLogicBlockEntity extends TrapLogicBlockEntity
{
	public static final Logger LOGGER = CyclicDungeons.LOGGER;
	public static final long UPDATE_FREQUENCY = Reference.Values.TICKS_PER_SECOND / 2;
	private List<LogicModule> modules = Lists.newArrayList(
			LogicModule.of(CDLogicGates.ENTRY.get())
				.name("input_1")
				.addOutput(CDLogicGates.OUTPUT, "wire1"),
			LogicModule.of(CDLogicGates.ENTRY.get())
				.name("input_2")
				.addOutput(CDLogicGates.OUTPUT, "wire2"),
			LogicModule.of(CDLogicGates.ENTRY.get())
				.name("input_3")
				.addOutput(CDLogicGates.OUTPUT, "wire3"),
			LogicModule.of(CDLogicGates.EXIT.get())
				.name("output_1")
				.addInput(CDLogicGates.INPUT, "wire1"),
			LogicModule.of(CDLogicGates.EXIT.get())
				.name("output_2")
				.addInput(CDLogicGates.INPUT, "wire2"),
			LogicModule.of(CDLogicGates.EXIT.get())
				.name("output_3")
				.addInput(CDLogicGates.INPUT, "wire3")
			);
	private Map<String, LogicWire> wires = new HashMap<>();
	private int ticks = 0;
	
	private List<Port> inputModules = Lists.newArrayList();
	private Map<Port, LogicModule> outputModules = new HashMap<>();
	private Map<Port, Boolean> outputMap = new HashMap<>();
	
	public ModularLogicBlockEntity(BlockPos pos, BlockState state)
	{
		super(CDBlockEntityTypes.MODULAR_LOGIC.get(), pos, state);
	}
	
	protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup)
	{
		super.writeNbt(nbt, registryLookup);
		nbt.remove("Logic");
		if(!modules.isEmpty())
		{
			NbtCompound c = new NbtCompound();
			c.put("Logic", LogicModule.LIST_CODEC.encodeStart(NbtOps.INSTANCE, modules).getOrThrow());
			
			if(!wires.isEmpty())
			{
				PortState set = new PortState();
				wires.values().forEach(w -> set.put(Port.of(w.name()), w.isOn()));
				if(!set.isInert())
					c.put("Wires", PortState.CODEC.encodeStart(NbtOps.INSTANCE, set).getOrThrow());
			}
			
			nbt.put("Circuit", c);
		}
	}
	
	protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup)
	{
		super.readNbt(nbt, registryLookup);
		modules.clear();
		wires.clear();
		if(nbt.contains("Circuit"))
		{
			NbtCompound c = nbt.getCompound("Circuit");
			modules.addAll(LogicModule.LIST_CODEC.parse(NbtOps.INSTANCE, c.get("Logic")).getOrThrow());
			
			if(c.contains("Wires"))
			{
				PortState set = PortState.CODEC.parse(NbtOps.INSTANCE, c.get("Wires")).getOrThrow();
				for(Port wire : set.keys())
					wires.put(wire.name(), new LogicWire(wire.name()).setState(set.get(wire)));
			}
		}
		
		logPorts();
	}
	
	public static <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type)
	{
		return type != CDBlockEntityTypes.MODULAR_LOGIC.get() ? 
				null : 
				IWireableBlock.validateTicker(type, CDBlockEntityTypes.MODULAR_LOGIC.get(), 
					world.isClient() ? 
						TrapLogicBlockEntity::tickClient : 
						ModularLogicBlockEntity::tickServer);
	}
	
	public static <T extends BlockEntity> void tickServer(World world, BlockPos pos, BlockState state, ModularLogicBlockEntity tile)
	{
		if(tile.ticks++ % UPDATE_FREQUENCY == 0)
			tile.respondToPorts();
	}
	
	public void logPorts()
	{
		inputModules.clear();
		outputModules.clear();
		if(modules.isEmpty())
			return;
		
		modules.forEach(m -> 
		{
			if(m.isInput())
				m.toPort().ifPresent(inputModules::add);
			if(m.isOutput())
				m.toPort().ifPresent(p -> outputModules.put(p, m));
		});
	}
	
	public void respondToPorts()
	{
		cleanAllOf(false);
		cleanAllOf(true);
		if(modules.isEmpty())
			return;
		
		// Make sure all wires exist within the wire map
		final Consumer<String> wireRegistry = w -> 
		{
			if(!wires.containsKey(w))
			{
				LogicWire wire = new LogicWire(w);
				// Update the wire on instantiation to ensure initial state is accurate
				wire.update(modules);
				wires.put(w, wire);
			}
		};
		modules.forEach(m -> m.registerWires(wireRegistry));
		
		// Update all modules
		modules.forEach(m -> m.update(wires, this));
		
		// Update all wires
		wires.values().forEach(w -> w.update(modules));
		
		// Update output port status map
		outputMap.clear();
		for(Entry<Port, LogicModule> entry : outputModules.entrySet())
			outputMap.put(entry.getKey(), entry.getValue().getOutputStatus(CDLogicGates.OUTPUT));
	}
	
	public List<Port> inputPorts() { return Lists.newArrayList(inputModules); }
	public List<Port> outputPorts() { return Lists.newArrayList(outputModules.keySet()); }
	
	protected boolean getPortStatus(Port port, boolean isInput)
	{
		if(isInput)
			return super.getPortStatus(port, isInput);
		else
			return outputMap.getOrDefault(port, false);
	}
}

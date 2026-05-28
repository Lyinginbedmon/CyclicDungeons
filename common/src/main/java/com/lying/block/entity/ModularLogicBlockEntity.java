package com.lying.block.entity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import com.google.common.collect.Lists;
import com.lying.block.IWireableBlock;
import com.lying.block.IWireableBlock.Port;
import com.lying.block.entity.logic.LogicModule;
import com.lying.block.entity.logic.LogicWire;
import com.lying.block.entity.logic.PortState;
import com.lying.init.CDBlockEntityTypes;
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

public class ModularLogicBlockEntity extends BlockEntity
{
	public static final long UPDATE_FREQUENCY = Reference.Values.TICKS_PER_SECOND / 2;
	private List<LogicModule> modules = Lists.newArrayList();
	private Map<String, LogicWire> wires = new HashMap<>();
	private int ticks = 0;
	
	private Map<String, LogicModule> outputModules = new HashMap<>();
	
	public ModularLogicBlockEntity(BlockPos pos, BlockState state)
	{
		super(CDBlockEntityTypes.MODULAR_LOGIC.get(), pos, state);
	}
	
	protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup)
	{
		super.writeNbt(nbt, registryLookup);
		if(!modules.isEmpty())
		{
			nbt.put("circuit", LogicModule.LIST_CODEC.encodeStart(NbtOps.INSTANCE, modules).getOrThrow());
			
			if(!wires.isEmpty())
			{
				PortState set = new PortState();
				wires.values().forEach(w -> set.put(new Port(w.name()), w.isOn()));
				if(!set.isInert())
					nbt.put("wires", PortState.CODEC.encodeStart(NbtOps.INSTANCE, set).getOrThrow());
			}
		}
	}
	
	protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup)
	{
		super.readNbt(nbt, registryLookup);
		modules.clear();
		wires.clear();
		if(nbt.contains("circuit"))
		{
			modules.addAll(LogicModule.LIST_CODEC.parse(NbtOps.INSTANCE, nbt.get("circuit")).getOrThrow());
			
			if(nbt.contains("wires"))
			{
				PortState set = PortState.CODEC.parse(NbtOps.INSTANCE, nbt.get("wires")).getOrThrow();
				for(Port wire : set.keys())
					wires.put(wire.name(), new LogicWire(wire.name()).setState(set.get(wire)));
			}
		}
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
		if(tile.ticks++ % UPDATE_FREQUENCY == 0)
			tile.processLogic();
	}
	
	public boolean processLogic()
	{
		if(modules.isEmpty())
			return false;
		
		outputModules.clear();
		
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
		modules.forEach(m -> 
		{
			m.registerWires(wireRegistry);
			if(m.isOutput())
				outputModules.put(m.displayName(), m);
		});
		
		// Update all modules
		modules.forEach(m -> m.update(wires, this));
		
		// Update all wires
		wires.values().forEach(w -> w.update(modules));
		
		return true;
	}
	
	public List<Port> outputPorts() { return Lists.newArrayList(outputModules.keySet().stream().map(Port::new).toList()); }
	
	public boolean getPortStatus(Port output)
	{
		return outputModules.containsKey(output.name()) ? outputModules.get(output.name()).getOutputStatus(output) : false;
	}
}

package com.lying.block.entity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import com.google.common.collect.Lists;
import com.lying.block.IWireableBlock;
import com.lying.block.entity.logic.LogicModule;
import com.lying.block.entity.logic.LogicWire;
import com.lying.block.entity.logic.WireState;
import com.lying.init.CDBlockEntityTypes;
import com.lying.init.CDLogicGates;

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
	@SuppressWarnings("unused")
	private static final List<LogicModule> TEST_LOGIC	= Lists.newArrayList(
		LogicModule.of(CDLogicGates.TRUE.get())
			.name("input_a")
			.addOutput("a"),
		LogicModule.of(CDLogicGates.FALSE.get())
			.name("input_b")
			.addOutput("not_1"),
		LogicModule.of(CDLogicGates.AND.get())
			.name("result_gate")
			.addInput("input_0", "a")
			.addInput("input_1", "b")
			.addInput("input_2", "c"),
		LogicModule.of(CDLogicGates.NOR.get())
			.addInput("not_1")
			.addOutput("b"),
		LogicModule.of(CDLogicGates.NOR.get())
			.addInput("not_2")
			.addOutput("c"),
		LogicModule.of(CDLogicGates.FALSE.get())
			.name("input_c")
			.addOutput("not_2")
		);
	
	private List<LogicModule> modules = Lists.newArrayList();
	private Map<String, LogicWire> wires = new HashMap<>();
	
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
				WireState set = new WireState();
				wires.values().forEach(w -> set.put(w.name(), w.isOn()));
				if(!set.isInert())
					nbt.put("wires", WireState.CODEC.encodeStart(NbtOps.INSTANCE, set).getOrThrow());
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
				WireState set = WireState.CODEC.parse(NbtOps.INSTANCE, nbt.get("wires")).getOrThrow();
				for(String wire : set.keys())
					wires.put(wire, new LogicWire(wire).setState(set.get(wire)));
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
		tile.processLogic(world.getTime());
	}
	
	public boolean processLogic(long worldTime)
	{
		if(modules.isEmpty())
			return false;
		
		// Make sure all wires exist within the wire map
		final Consumer<String> wireRegistry = w -> 
		{
			if(!wires.containsKey(w))
			{
				LogicWire wire = new LogicWire(w);
				// Update the wire on instantiation to ensure initial state is accurate
				wire.update(modules, worldTime);
				wires.put(w, wire);
			}
		};
		modules.forEach(m -> m.registerWires(wireRegistry));
		
		// Update all modules
		modules.forEach(m -> m.update(wires, worldTime, this));
		
		// Update all wires
		wires.values().forEach(w -> w.update(modules, worldTime));
		
		return true;
	}
}

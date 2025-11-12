package com.lying.block.entity;

import java.util.List;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.lying.block.IWireableBlock;
import com.lying.block.IWireableBlock.WireRecipient;
import com.lying.block.TrapLogicBlock;
import com.lying.init.CDBlockEntityTypes;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class TrapLogicBlockEntity extends BlockEntity
{
	private static final Function<int[], BlockPos> intArrayToBlockPos = v -> new BlockPos(v[0], v[1], v[2]);
	
	private List<BlockPos> actors = Lists.newArrayList();
	private List<BlockPos> sensors = Lists.newArrayList();
	
	public TrapLogicBlockEntity(BlockPos pos, BlockState state)
	{
		super(CDBlockEntityTypes.TRAP_LOGIC.get(), pos, state);
	}
	
	protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup)
	{
		super.writeNbt(nbt, registryLookup);
		if(!sensors.isEmpty())
		{
			NbtList set = new NbtList();
			sensors.forEach(p -> set.add(NbtHelper.fromBlockPos(p)));
			nbt.put("Sensors", set);
		}
		
		if(!actors.isEmpty())
		{
			NbtList set = new NbtList();
			actors.forEach(p -> set.add(NbtHelper.fromBlockPos(p)));
			nbt.put("Actors", set);
		}
	}
	
	protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup)
	{
		super.readNbt(nbt, registryLookup);
		
		sensors.clear();
		if(nbt.contains("Sensors"))
		{
			NbtList set = nbt.getList("Sensors", NbtElement.INT_ARRAY_TYPE);
			for(int i=0; i<set.size(); i++)
				sensors.add(intArrayToBlockPos.apply(set.getIntArray(i)));
		}
		
		actors.clear();
		if(nbt.contains("Actors"))
		{
			NbtList set = nbt.getList("Actors", NbtElement.INT_ARRAY_TYPE);
			for(int i=0; i<set.size(); i++)
				actors.add(intArrayToBlockPos.apply(set.getIntArray(i)));
		}
	}
	
	public static <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type)
	{
		return type != CDBlockEntityTypes.TRAP_LOGIC.get() ? 
				null : 
				TrapLogicBlock.validateTicker(type, CDBlockEntityTypes.TRAP_LOGIC.get(), 
					world.isClient() ? 
						TrapLogicBlockEntity::tickClient : 
						TrapLogicBlockEntity::tickServer);
	}
	
	public static <T extends BlockEntity> void tickClient(World world, BlockPos pos, BlockState state, TrapLogicBlockEntity tile)
	{
		
	}
	
	public static <T extends BlockEntity> void tickServer(World world, BlockPos pos, BlockState state, TrapLogicBlockEntity tile)
	{
		tile.cleanActors();
		if(!tile.hasActors())
			return;
		
		if(!tile.hasSensors())
		{
			// Deactivate actors
			tile.actors.forEach(p -> IWireableBlock.getWireable(pos, world).deactivate(pos, world));
		}
		else
		{
			// FIXME Convert logic handling to registry object instead of hard-coding
			
			// Copy aggregate sensor state to actors
			boolean status = tile.sensorInputState();
			tile.actors.forEach(p -> 
			{
				if(status)
					IWireableBlock.getWireable(p, world).activate(p, world);
				else
					IWireableBlock.getWireable(p, world).deactivate(p, world);
			});
		}
	}
	
	public boolean hasSensors() { return !sensors.isEmpty(); }
	
	public boolean hasActors() { return !actors.isEmpty(); }
	
	public boolean sensorInputState()
	{
		sensors.removeIf(pos -> 
		{
			BlockState sensorState = world.getBlockState(pos);
			return !(sensorState.getBlock() instanceof IWireableBlock) || IWireableBlock.getWireable(pos, world).type() != WireRecipient.SENSOR;
		});
		
		return !sensors.isEmpty() && sensors.stream().anyMatch(p -> IWireableBlock.getWireable(p, world).isActive(p, world));
	}
	
	private void cleanActors()
	{
		actors.removeIf(pos -> 
		{
			BlockState sensorState = world.getBlockState(pos);
			return !(sensorState.getBlock() instanceof IWireableBlock) || IWireableBlock.getWireable(pos, world).type() != WireRecipient.ACTOR;
		});
	}
	
	public boolean processWireConnection(BlockPos pos, WireRecipient type)
	{
		if(type == WireRecipient.ACTOR)
			actors.add(pos);
		else
			sensors.add(pos);
		
		return true;
	}
	
	public void reset()
	{
		actors.forEach(p -> IWireableBlock.getWireable(p, world).deactivate(p, world));
		
		sensors.clear();
		actors.clear();
	}
}

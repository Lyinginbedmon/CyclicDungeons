package com.lying.block.entity;

import java.util.List;

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

public class TrapActorBlockEntity extends BlockEntity
{
	private List<BlockPos> sensors = Lists.newArrayList();
	
	public TrapActorBlockEntity(BlockPos pos, BlockState state)
	{
		super(CDBlockEntityTypes.TRAP_ACTOR.get(), pos, state);
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
	}
	
	protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup)
	{
		super.readNbt(nbt, registryLookup);
		sensors.clear();
		if(nbt.contains("Sensors"))
		{
			NbtList set = nbt.getList("Sensors", NbtElement.INT_ARRAY_TYPE);
			for(int i=0; i<set.size(); i++)
			{
				int[] val = set.getIntArray(i);
				sensors.add(new BlockPos(val[0], val[1], val[2]));
			}
		}
	}
	
	public static <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type)
	{
		return type != CDBlockEntityTypes.TRAP_ACTOR.get() ? 
				null : 
				TrapLogicBlock.validateTicker(type, CDBlockEntityTypes.TRAP_ACTOR.get(), 
					world.isClient() ? 
						TrapActorBlockEntity::tickClient : 
						TrapActorBlockEntity::tickServer);
	}
	
	public static <T extends BlockEntity> void tickClient(World world, BlockPos pos, BlockState state, TrapActorBlockEntity tile)
	{
		
	}
	
	public static <T extends BlockEntity> void tickServer(World world, BlockPos pos, BlockState state, TrapActorBlockEntity tile)
	{
		if(tile.hasSensors())
		{
			// Copy sensor state to actor
			IWireableBlock actor = IWireableBlock.getWireable(pos, world);
			if(tile.sensorInputState())
				actor.activate(pos, world);
			else
				actor.deactivate(pos, world);
		}
	}
	
	public boolean hasSensors() { return !sensors.isEmpty(); }
	
	public boolean sensorInputState()
	{
		sensors.removeIf(pos -> 
		{
			BlockState sensorState = world.getBlockState(pos);
			return !(sensorState.getBlock() instanceof IWireableBlock) || IWireableBlock.getWireable(pos, world).type() != WireRecipient.SENSOR;
		});
		
		return !sensors.isEmpty() && sensors.stream().anyMatch(p -> IWireableBlock.getWireable(p, world).isActive(p, world));
	}
	
	public void reset()
	{
		sensors.clear();
		IWireableBlock.getWireable(getPos(), getWorld()).deactivate(getPos(), getWorld());
	}
	
	public boolean processWire(BlockPos pos)
	{
		sensors.add(pos);
		return true;
	}
}

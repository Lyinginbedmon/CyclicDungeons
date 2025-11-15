package com.lying.block.entity;

import com.lying.block.IWireableBlock;
import com.lying.block.IWireableBlock.WireRecipient;
import com.lying.block.TrapLogicBlock;
import com.lying.init.CDBlockEntityTypes;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class TrapActorBlockEntity extends AbstractWireableBlockEntity
{
	public TrapActorBlockEntity(BlockPos pos, BlockState state)
	{
		super(CDBlockEntityTypes.TRAP_ACTOR.get(), pos, state);
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
	
	public static <T extends BlockEntity> void tickClient(World world, BlockPos pos, BlockState state, TrapActorBlockEntity tile) { }
	
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
	
	public boolean sensorInputState()
	{
		cleanSensors();
		return hasSensors() && getSensors().stream().anyMatch(p -> IWireableBlock.getWireable(p, world).isActive(p, world));
	}
	
	public boolean processWireConnection(BlockPos pos, WireRecipient type)
	{
		if(type != WireRecipient.SENSOR)
			return false;
		
		addWire(pos, type);
		return true;
	}
}

package com.lying.block.entity;

import java.util.List;

import com.lying.block.ITrapActor;
import com.lying.block.IWireableBlock;
import com.lying.init.CDBlockEntityTypes;
import com.lying.init.CDLogicGates;
import com.lying.item.WiringGunItem.WireMode;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class TrapActorBlockEntity<T extends ITrapActor> extends AbstractWireableBlockEntity
{
	protected <K extends TrapActorBlockEntity<?>> TrapActorBlockEntity(BlockEntityType<K> type, BlockPos pos, BlockState state)
	{
		super(type, pos, state);
	}
	
	public TrapActorBlockEntity(BlockPos pos, BlockState state)
	{
		this(CDBlockEntityTypes.TRAP_ACTOR.get(), pos, state);
	}
	
	public List<String> outputPorts() { return List.of(); }
	
	public static <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type)
	{
		return type != CDBlockEntityTypes.TRAP_ACTOR.get() ? 
				null : 
				IWireableBlock.validateTicker(type, CDBlockEntityTypes.TRAP_ACTOR.get(), 
					world.isClient() ? 
						TrapActorBlockEntity::tickClient : 
						TrapActorBlockEntity::tickServer);
	}
	
	public static <T extends BlockEntity> void tickClient(World world, BlockPos pos, BlockState state, TrapActorBlockEntity<?> tile) { }
	
	public static <T extends BlockEntity> void tickServer(World world, BlockPos pos, BlockState state, TrapActorBlockEntity<?> tile)
	{
		tile.respondToPorts();
	}
	
	public void respondToPorts()
	{
		if(hasInputs())
		{
			// Copy sensor state to actor
			ITrapActor actor = (ITrapActor)world.getBlockState(pos).getBlock();
			if(sensorInputState())
				actor.activate(pos, world);
			else
				actor.deactivate(pos, world);
		}
	}
	
	protected void resetBlock()
	{
		ITrapActor actor = (ITrapActor)world.getBlockState(pos).getBlock();
		if(actor.isActive(pos, world))
			actor.deactivate(pos, world);
	}
	
	public boolean sensorInputState()
	{
		cleanSensors();
		return hasInputs() && getInput(CDLogicGates.INPUT);
	}
	
	public boolean processInputConnection(String input, BlockPos pos, String port, WireMode space)
	{
		addInputWire(input, pos, port, space);
		return true;
	}
	
	public boolean processOutputConnection(String output, BlockPos pos, String input, WireMode space)
	{
		return false;
	}
}

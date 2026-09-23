package com.lying.block.puzzle.entity;

import com.lying.block.puzzle.OneWayDoorBlock;
import com.lying.block.puzzle.PuzzleDoorBlock;
import com.lying.init.CDBlockEntityTypes;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class OneWayDoorBlockEntity extends PuzzleBreakBlockEntity
{
	public OneWayDoorBlockEntity(BlockPos pos, BlockState state)
	{
		super(CDBlockEntityTypes.BOSS_DOOR.get(), pos, state);
	}
	
	public static <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type)
	{
		return type != CDBlockEntityTypes.BOSS_DOOR.get() ? 
				null : 
				PuzzleDoorBlock.validateTicker(type, CDBlockEntityTypes.BOSS_DOOR.get(), 
					world.isClient() ? 
						OneWayDoorBlockEntity::tickClient : 
						PuzzleBreakBlockEntity::tickServer);
	}
	
	public static <T extends BlockEntity> void tickClient(World world, BlockPos pos, BlockState state, OneWayDoorBlockEntity tile)
	{
		
	}
	
	protected void onTrigger()
	{
		world.setBlockState(getPos(), getCachedState().with(OneWayDoorBlock.OPEN, true), 3);
		world.setBlockState(getPos().up(), world.getBlockState(getPos().up()).with(OneWayDoorBlock.OPEN, true), 3);
	}
}
